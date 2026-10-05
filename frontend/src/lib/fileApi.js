/**
 * File handling for domain resources.
 *
 * Upload flow:
 * 1. Initiate an upload session in the backend.
 * 2. Transfer bytes directly to the presigned storage URL.
 * 3. Confirm the upload in the backend.
 *
 * Callers with a confirmation-retry UI may preserve a failed upload session
 * and retry confirmation without uploading the file bytes again.
 */

import { apiClient } from "@/lib/apiClient";
import {
  ApiError,
  getApiErrorTitle,
  getResponseCorrelationId,
  readResponseBody,
} from "@/lib/api";

const RESOURCE_PATHS = {
  consulta: (resource) => `/consultas/${resource.id}/archivos`,
  seguimiento: (resource) => `/seguimientos/${resource.id}/archivos`,
  respuesta: (resource) =>
      `/seguimientos/${resource.parentId}/respuestas/${resource.id}/archivos`,
  conciliacion: (resource) => `/conciliaciones/${resource.id}/archivos`,
};

/**
 * Prevents two concurrent calls with the same File object from creating
 * duplicate upload sessions for the same resource.
 *
 * WeakMap allows entries to disappear when the File object is no longer used.
 */
const inFlightUploads = new WeakMap();

function resourcePath(resource) {
  const builder = RESOURCE_PATHS[String(resource?.type || "").toLowerCase()];

  if (!builder || !resource?.id) {
    throw new Error("El recurso del archivo no es válido");
  }

  return builder(resource);
}

function resourceKey(resource) {
  return [
    String(resource?.type || "").toLowerCase(),
    resource?.id ?? "",
    resource?.parentId ?? "",
  ].join(":");
}

async function buildApiError(response, fallback) {
  const payload = await readResponseBody(response);
  const correlationId = getResponseCorrelationId(response, payload);

  return new ApiError(getApiErrorTitle(payload, fallback), {
    status: response.status,
    payload,
    response,
    correlationId,
  });
}

function createAbortError() {
  const error = new Error("La carga fue cancelada");
  error.name = "AbortError";
  return error;
}

function enrichUploadError(error, metadata) {
  if (error && typeof error === "object") {
    Object.assign(error, metadata);
  }

  return error;
}

function notifyState(options, state, extra = {}) {
  options?.onStateChange?.({
    state,
    ...extra,
  });
}

/**
 * Starts the backend upload session.
 *
 * checksum is deliberately optional. SCRUM-281 must not invent or force the
 * checksum contract until the backend requires it.
 */
export async function initiate(resource, file, { checksum } = {}) {
  if (!file) {
    throw new Error("El archivo es obligatorio");
  }

  const body = {
    fileName: file.name,
    size: file.size,
    contentType: file.type || "application/octet-stream",
  };

  if (checksum) {
    body.checksum = checksum;
  }

  const response = await apiClient.post(
      `${resourcePath(resource)}/uploads`,
      body,
  );

  if (!response.ok) {
    throw await buildApiError(
        response,
        "No se pudo iniciar la carga del archivo",
    );
  }

  return response.json();
}

/**
 * Transfers the file directly to the presigned storage URL.
 *
 * XMLHttpRequest is intentionally used instead of fetch because the browser
 * Fetch API does not expose upload progress events.
 */
export function uploadToStorage(
    uploadSession,
    file,
    { signal, onProgress } = {},
) {
  if (!uploadSession?.uploadUrl) {
    return Promise.reject(
        new Error("La sesión de carga no contiene una URL válida"),
    );
  }

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    let settled = false;

    function cleanup() {
      if (signal) {
        signal.removeEventListener("abort", handleSignalAbort);
      }
    }

    function finish(callback) {
      if (settled) {
        return;
      }

      settled = true;
      cleanup();
      callback();
    }

    function handleSignalAbort() {
      xhr.abort();
    }

    if (signal?.aborted) {
      reject(createAbortError());
      return;
    }

    xhr.open("PUT", uploadSession.uploadUrl, true);
    xhr.setRequestHeader(
        "Content-Type",
        file.type || "application/octet-stream",
    );

    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable) {
        onProgress?.({
          loaded: event.loaded,
          total: null,
          percent: null,
        });
        return;
      }

      const percent =
          event.total > 0
              ? Math.min(100, Math.round((event.loaded / event.total) * 100))
              : 0;

      onProgress?.({
        loaded: event.loaded,
        total: event.total,
        percent,
      });
    };

    xhr.onload = () => {
      finish(() => {
        if (xhr.status >= 200 && xhr.status < 300) {
          onProgress?.({
            loaded: file.size,
            total: file.size,
            percent: 100,
          });
          resolve();
          return;
        }

        reject(
            new Error(
                `No se pudo transferir el archivo al almacenamiento (${xhr.status})`,
            ),
        );
      });
    };

    xhr.onerror = () => {
      finish(() => {
        reject(
            new Error("No se pudo transferir el archivo al almacenamiento"),
        );
      });
    };

    xhr.onabort = () => {
      finish(() => {
        reject(createAbortError());
      });
    };

    if (signal) {
      signal.addEventListener("abort", handleSignalAbort, { once: true });
    }

    xhr.send(file);
  });
}

function completionBody(resource) {
  return resource?.type === "respuesta"
      ? { parentId: resource.parentId }
      : {};
}

/**
 * Confirms an upload that has already been transferred to storage.
 *
 * This function can safely be invoked again when the previous confirmation
 * request failed. It does not upload the file again.
 */
export async function completeUpload(resource, uploadId) {
  if (!uploadId) {
    throw new Error("El identificador de carga es obligatorio");
  }

  const response = await apiClient.post(
      `/file-uploads/${uploadId}/complete`,
      completionBody(resource),
  );

  if (!response.ok) {
    const error = await buildApiError(
        response,
        "No se pudo confirmar la carga del archivo",
    );

    throw enrichUploadError(error, {
      phase: "confirmation",
      uploadId,
      retryableCompletion: true,
    });
  }

  return response.json();
}

/**
 * Retries only backend confirmation.
 *
 * Bytes are not transferred again.
 */
export async function retryComplete(resource, uploadId, options = {}) {
  notifyState(options, "confirming", { uploadId, retry: true });

  try {
    const result = await completeUpload(resource, uploadId);

    notifyState(options, "completed", {
      uploadId,
      retry: true,
      data: result,
    });

    return result;
  } catch (error) {
    notifyState(options, "confirmation_failed", {
      uploadId,
      retry: true,
      error,
    });

    throw error;
  }
}

/**
 * Cancels an upload session in the backend.
 *
 * For an active browser transfer, callers should also abort the AbortController
 * associated with that transfer.
 */
export async function abortUpload(uploadId) {
  if (!uploadId) {
    throw new Error("El identificador de carga es obligatorio");
  }

  const response = await apiClient.delete(`/file-uploads/${uploadId}`);

  if (!response.ok) {
    throw await buildApiError(
        response,
        "No se pudo cancelar la carga del archivo",
    );
  }
}

async function abortUploadSilently(uploadId) {
  if (!uploadId) {
    return;
  }

  try {
    await abortUpload(uploadId);
  } catch {
    // Best-effort compensation. The backend reconciliation process remains
    // responsible for stale or incomplete upload sessions.
  }
}

async function performUpload(resource, file, options = {}) {
  if (!file) {
    throw new Error("El archivo es obligatorio");
  }

  notifyState(options, "initiating", { file });

  let uploadSession;

  try {
    uploadSession = await initiate(resource, file, {
      checksum: options.checksum,
    });
  } catch (error) {
    notifyState(options, "initiation_failed", {
      file,
      error,
    });

    throw enrichUploadError(error, {
      phase: "initiation",
      retryableCompletion: false,
    });
  }

  const uploadId = uploadSession.uploadId;

  options.onSession?.({
    uploadId,
    uploadUrl: uploadSession.uploadUrl,
    expiresAt: uploadSession.expiresAt,
  });

  notifyState(options, "uploading", {
    file,
    uploadId,
  });

  try {
    await uploadToStorage(uploadSession, file, {
      signal: options.signal,
      onProgress: options.onProgress,
    });
  } catch (error) {
    await abortUploadSilently(uploadId);

    const cancelled = error?.name === "AbortError";

    notifyState(options, cancelled ? "cancelled" : "transfer_failed", {
      file,
      uploadId,
      error,
    });

    throw enrichUploadError(error, {
      phase: "transfer",
      uploadId,
      retryableCompletion: false,
      cancelled,
    });
  }

  notifyState(options, "confirming", {
    file,
    uploadId,
  });

  try {
    const result = await completeUpload(resource, uploadId);

    notifyState(options, "completed", {
      file,
      uploadId,
      data: result,
    });

    return result;
  } catch (error) {
    const keepPendingConfirmation =
        options.keepPendingOnConfirmationFailure === true;

    /*
     * Existing callers do not have a confirmation-retry UI. Preserve the
     * previous compensation behavior for them so a failed confirmation does
     * not leave an abandoned upload session.
     *
     * SCRUM-281 callers explicitly opt in to preserving the session so they can
     * retry only /complete without transferring the file again.
     */
    if (!keepPendingConfirmation) {
      await abortUploadSilently(uploadId);
    }

    notifyState(options, "confirmation_failed", {
      file,
      uploadId,
      error,
    });

    throw enrichUploadError(error, {
      phase: "confirmation",
      uploadId,
      retryableCompletion: keepPendingConfirmation,
    });
  }
}

function getInFlightUpload(file, key) {
  return inFlightUploads.get(file)?.get(key) ?? null;
}

function setInFlightUpload(file, key, promise) {
  let resourceUploads = inFlightUploads.get(file);

  if (!resourceUploads) {
    resourceUploads = new Map();
    inFlightUploads.set(file, resourceUploads);
  }

  resourceUploads.set(key, promise);
}

function clearInFlightUpload(file, key, promise) {
  const resourceUploads = inFlightUploads.get(file);

  if (!resourceUploads) {
    return;
  }

  if (resourceUploads.get(key) === promise) {
    resourceUploads.delete(key);
  }

  if (resourceUploads.size === 0) {
    inFlightUploads.delete(file);
  }
}

/**
 * Uploads one file.
 *
 * Calling this method concurrently with the same File object and resource
 * returns the same in-flight operation instead of starting a second backend
 * upload session.
 */
export function upload(resource, file, options = {}) {
  if (!file) {
    return Promise.reject(new Error("El archivo es obligatorio"));
  }

  const key = resourceKey(resource);
  const existing = getInFlightUpload(file, key);

  if (existing) {
    return existing;
  }

  const promise = performUpload(resource, file, options).finally(() => {
    clearInFlightUpload(file, key, promise);
  });

  setInFlightUpload(file, key, promise);

  return promise;
}

/**
 * Uploads multiple files independently.
 *
 * Callbacks receive the corresponding file as the first argument so the UI can
 * maintain progress and state per item.
 */
export async function uploadMany(resource, files, options = {}) {
  const selectedFiles = Array.from(files || []);

  const results = await Promise.allSettled(
      selectedFiles.map((file) =>
          upload(resource, file, {
            ...options,
            onProgress: options.onProgress
                ? (progress) => options.onProgress(file, progress)
                : undefined,
            onStateChange: options.onStateChange
                ? (state) => options.onStateChange(file, state)
                : undefined,
            onSession: options.onSession
                ? (session) => options.onSession(file, session)
                : undefined,
          }),
      ),
  );

  return results.map((result, index) => ({
    file: selectedFiles[index],
    ok: result.status === "fulfilled",
    data: result.status === "fulfilled" ? result.value : null,
    error: result.status === "rejected" ? result.reason : null,
  }));
}

export async function list(resource) {
  const response = await apiClient.get(resourcePath(resource));

  if (!response.ok) {
    throw await buildApiError(
        response,
        "No se pudieron listar los archivos",
    );
  }

  const data = await response.json();
  return Array.isArray(data) ? data : [];
}

export async function download(file, resource = null) {
  const parentId =
      resource?.type === "respuesta" ? resource.parentId : null;

  const query = parentId
      ? `?parentId=${encodeURIComponent(parentId)}`
      : "";

  const response = await apiClient.get(
      `/archivos/${file.id}/download${query}`,
  );

  if (!response.ok) {
    throw await buildApiError(
        response,
        "No se pudo preparar la descarga",
    );
  }

  const descriptor = await response.json();

  // Presigned download URLs are external to the backend API.
  const fileResponse = await fetch(descriptor.downloadUrl);

  if (!fileResponse.ok) {
    throw new Error("No se pudo descargar el archivo");
  }

  const blob = await fileResponse.blob();
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");

  anchor.href = url;
  anchor.download =
      descriptor.fileName || file.fileName || "archivo";

  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();

  window.URL.revokeObjectURL(url);
}

export async function remove(file, resource = null) {
  const parentId =
      resource?.type === "respuesta" ? resource.parentId : null;

  const query = parentId
      ? `?parentId=${encodeURIComponent(parentId)}`
      : "";

  const response = await apiClient.delete(
      `/archivos/${file.id}${query}`,
  );

  if (!response.ok) {
    throw await buildApiError(
        response,
        "No se pudo eliminar el archivo",
    );
  }
}

export const fileApi = {
  initiate,
  uploadToStorage,
  completeUpload,
  retryComplete,
  abortUpload,
  upload,
  uploadMany,
  list,
  download,
  remove,
};