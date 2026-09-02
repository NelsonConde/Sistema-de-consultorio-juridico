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

export async function download(file, resource = null) {
  const parentId = resource?.type === "respuesta" ? resource.parentId : null;
  const query = parentId ? `?parentId=${encodeURIComponent(parentId)}` : "";
  const response = await apiClient.get(`/archivos/${file.id}/download${query}`);

  if (!response.ok) {
    throw await buildApiError(
      response,
      "No se pudo preparar la descarga"
    );
  }

  const descriptor = await response.json();
  // The actual file download uses the external presigned storage URL.
  const fileResponse = await fetch(descriptor.downloadUrl);
  if (!fileResponse.ok) {
    throw new Error("No se pudo descargar el archivo");
  }

  const blob = await fileResponse.blob();
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = descriptor.fileName || file.fileName || "archivo";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
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
  download,
  remove,
};
