/**
 * File handling.
 *
 * Data loading behavior.
 * Data loading behavior.
 * File handling.
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

const DOWNLOAD_ERROR_MESSAGES = {
  403: "No tienes permisos para descargar este archivo. La sesión local fue limpiada por seguridad.",
  404: "El archivo ya no está disponible.",
  409: "El archivo cambió durante la operación. Actualiza la información e intenta nuevamente.",
  STORAGE_UNAVAILABLE: "El almacenamiento de archivos no está disponible en este momento.",
  EXPIRED_LINK: "El enlace de descarga venció. Se solicitó uno nuevo automáticamente.",
  DEFAULT: "No se pudo descargar el archivo. Intenta nuevamente.",
};

const STORAGE_UNAVAILABLE_STATUSES = new Set([500, 502, 503, 504]);

function resourcePath(resource) {
  const builder = RESOURCE_PATHS[String(resource?.type || "").toLowerCase()];
  if (!builder || !resource?.id) {
    throw new Error("El recurso del archivo no es válido");
  }
  return builder(resource);
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

function clearLocalSessionData() {
  if (typeof window === "undefined") return;

  try {
    window.localStorage.clear();
  } catch {
    // Browser storage may be unavailable.
  }

  try {
    window.sessionStorage.clear();
  } catch {
    // Browser storage may be unavailable.
  }
}

function buildDownloadError(message, { status = 0, correlationId = null } = {}) {
  const error = new Error(message);
  error.name = "DownloadError";
  error.status = status;
  error.correlationId = correlationId;
  return error;
}

function toDownloadApiError(error) {
  const status = Number(error?.status || error?.response?.status || 0);

  if (status === 403) {
    clearLocalSessionData();
    return buildDownloadError(DOWNLOAD_ERROR_MESSAGES[403], {
      status,
      correlationId: error?.correlationId || null,
    });
  }

  if (status === 404) {
    return buildDownloadError(DOWNLOAD_ERROR_MESSAGES[404], {
      status,
      correlationId: error?.correlationId || null,
    });
  }

  if (status === 409) {
    return buildDownloadError(DOWNLOAD_ERROR_MESSAGES[409], {
      status,
      correlationId: error?.correlationId || null,
    });
  }

  if (STORAGE_UNAVAILABLE_STATUSES.has(status)) {
    return buildDownloadError(DOWNLOAD_ERROR_MESSAGES.STORAGE_UNAVAILABLE, {
      status,
      correlationId: error?.correlationId || null,
    });
  }

  return buildDownloadError(DOWNLOAD_ERROR_MESSAGES.DEFAULT, {
    status,
    correlationId: error?.correlationId || null,
  });
}

function isExpiredDownloadResponse(response) {
  if (!response) return false;
  if ([401, 403, 404, 408, 410].includes(response.status)) return true;

  const errorCode = response.headers?.get?.("x-amz-error-code") || "";
  return /expired|signature|token/i.test(errorCode);
}

function isStorageUnavailableResponse(response) {
  return STORAGE_UNAVAILABLE_STATUSES.has(Number(response?.status || 0));
}

function safeDownloadFileName(...candidates) {
  const rawName = candidates.find(
    (candidate) => typeof candidate === "string" && candidate.trim()
  );

  const fallback = "archivo";
  if (!rawName) return fallback;

  const sanitized = rawName
    .trim()
    .replace(/[/\\?%*:|"<>]/g, "_")
    .replace(/[\u0000-\u001f\u007f]+/g, "")
    .replace(/^\.+/, "")
    .slice(0, 180);

  return sanitized || fallback;
}

async function requestDownloadDescriptor(fileId, resource) {
  const parentId = resource?.type === "respuesta" ? resource.parentId : null;
  const query = parentId ? `?parentId=${encodeURIComponent(parentId)}` : "";
  const response = await apiClient.get(`/archivos/${fileId}/download${query}`);

  if (!response.ok) {
    throw await buildApiError(
      response,
      "No se pudo preparar la descarga"
    );
  }

  const descriptor = await response.json();
  if (!descriptor?.downloadUrl) {
    throw buildDownloadError(DOWNLOAD_ERROR_MESSAGES.STORAGE_UNAVAILABLE);
  }

  return descriptor;
}

async function fetchSignedDownloadUrl(downloadUrl) {
  const response = await fetch(downloadUrl, {
    method: "GET",
    cache: "no-store",
  });

  if (!response.ok) {
    if (isExpiredDownloadResponse(response)) {
      throw buildDownloadError(DOWNLOAD_ERROR_MESSAGES.EXPIRED_LINK, {
        status: response.status,
        expiredLink: true,
      });
    }

    if (isStorageUnavailableResponse(response)) {
      throw buildDownloadError(DOWNLOAD_ERROR_MESSAGES.STORAGE_UNAVAILABLE, {
        status: response.status,
      });
    }

    throw buildDownloadError(DOWNLOAD_ERROR_MESSAGES.DEFAULT, {
      status: response.status,
    });
  }

  return response.blob();
}

function triggerBrowserDownload(blob, fileName) {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

async function initiate(resource, file) {
  const response = await apiClient.post(`${resourcePath(resource)}/uploads`, {
    fileName: file.name,
    size: file.size,
    contentType: file.type || "application/octet-stream",
  });

  if (!response.ok) {
    throw await buildApiError(
      response,
      "No se pudo iniciar la carga del archivo"
    );
  }

  return response.json();
}

async function uploadToStorage(upload, file) {
  // Presigned storage URLs are external to the backend API and must not receive
  // cookies, CSRF headers, or backend correlation headers from apiClient.
  const response = await fetch(upload.uploadUrl, {
    method: "PUT",
    headers: {
      "Content-Type": file.type || "application/octet-stream",
    },
    body: file,
  });

  if (!response.ok) {
    throw new Error("No se pudo transferir el archivo al almacenamiento");
  }
}

export async function upload(resource, file) {
  if (!file) throw new Error("El archivo es obligatorio");

  const uploadSession = await initiate(resource, file);
  try {
    await uploadToStorage(uploadSession, file);

    const completion = await apiClient.post(
      `/file-uploads/${uploadSession.uploadId}/complete`,
      resource.type === "respuesta" ? { parentId: resource.parentId } : {},
    );

    if (!completion.ok) {
      throw await buildApiError(
        completion,
        "No se pudo confirmar la carga del archivo"
      );
    }

    return completion.json();
  } catch (error) {
    try {
      await apiClient.delete(`/file-uploads/${uploadSession.uploadId}`);
    } catch {
      // Data loading behavior.
    }
    throw error;
  }
}

export async function uploadMany(resource, files) {
  const selectedFiles = Array.from(files || []);
  const results = await Promise.allSettled(
    selectedFiles.map((file) => upload(resource, file)),
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
      "No se pudieron listar los archivos"
    );
  }
  const data = await response.json();
  return Array.isArray(data) ? data : [];
}

export async function downloadById(fileId, resource = null, options = {}) {
  if (!fileId) throw buildDownloadError("El archivo no es válido");

  try {
    let descriptor = await requestDownloadDescriptor(fileId, resource);

    try {
      const blob = await fetchSignedDownloadUrl(descriptor.downloadUrl);
      triggerBrowserDownload(
        blob,
        safeDownloadFileName(descriptor.fileName, options.fileName)
      );
      return;
    } catch (error) {
      if (!error?.expiredLink) throw error;
    }

    descriptor = await requestDownloadDescriptor(fileId, resource);
    const blob = await fetchSignedDownloadUrl(descriptor.downloadUrl);
    triggerBrowserDownload(
      blob,
      safeDownloadFileName(descriptor.fileName, options.fileName)
    );
  } catch (error) {
    throw toDownloadApiError(error);
  }
}

export async function download(file, resource = null) {
  return downloadById(file?.id, resource, {
    fileName: file?.fileName || file?.nombre,
  });
}

export async function remove(file, resource = null) {
  const parentId = resource?.type === "respuesta" ? resource.parentId : null;
  const query = parentId ? `?parentId=${encodeURIComponent(parentId)}` : "";
  const response = await apiClient.delete(`/archivos/${file.id}${query}`);
  if (!response.ok) {
    throw await buildApiError(
      response,
      "No se pudo eliminar el archivo"
    );
  }
}

export const fileApi = {
  upload,
  uploadMany,
  list,
  downloadById,
  download,
  remove,
};
