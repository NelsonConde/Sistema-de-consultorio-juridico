import { apiRequestData } from "@/lib/api";
import { API_URL_BASE } from "@/lib/config";

function resolveConciliacionUrl(path) {
  return path.startsWith("http") ? path : `${API_URL_BASE}${path}`;
}

/**
 * Conciliation workflow detail.
 * Error handling.
 */
export async function requestConciliacion(
  path,
  options = {},
  fallback = "Ocurrió un error"
) {
  const data = await apiRequestData(
    resolveConciliacionUrl(path),
    options,
    {
      fallback,
      statusMessages: {
        401: "Sesión expirada. Inicia sesión nuevamente.",
      },
    }
  );

  // Implementation detail.
  return typeof data === "string" ? { mensaje: data } : data;
}
