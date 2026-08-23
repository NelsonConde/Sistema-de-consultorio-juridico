import { apiClient } from "@/lib/apiClient";
import { API_URL_BASE } from "@/lib/config";

export async function leerRespuesta(res) {
  const text = await res.text();

  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { mensaje: text };
  }
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
