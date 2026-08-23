import { apiRequestData } from "@/lib/api";
import { API_URL_BASE } from "@/lib/config";

function resolveConciliacionUrl(path) {
  return path.startsWith("http") ? path : `${API_URL_BASE}${path}`;
}

/**
 * Cliente de datos del módulo de conciliaciones.
 * Centraliza transporte, lectura de respuesta y construcción del error HTTP.
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

  // Conserva la forma previa de las respuestas de texto plano del módulo.
  return typeof data === "string" ? { mensaje: data } : data;
}
