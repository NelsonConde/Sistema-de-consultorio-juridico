import { apiClient } from "@/lib/apiClient";
import { API_URL_BASE } from "@/lib/config";
import { readResponseBody } from "@/lib/api";

export async function leerRespuesta(res) {
  const data = await readResponseBody(res);
  return typeof data === "string" ? { mensaje: data } : data;
}

export async function fetchCatalogo(path) {
  const res = await apiClient.request(`${API_URL_BASE}${path}`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    return [];
  }

  const data = await res.json();
  return Array.isArray(data) ? data : [];
}
