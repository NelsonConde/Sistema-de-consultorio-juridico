import { apiRequestData } from "@/lib/api";

const CONSULTA_STATUS_MESSAGES = {
  401: "Sesión vencida. Inicia sesión nuevamente.",
  403: "No tienes permisos para consultar esta información.",
};

const MUTACION_STATUS_MESSAGES = {
  401: "Sesión vencida. Inicia sesión nuevamente.",
  403: "No tienes permisos para realizar esta acción.",
};

export async function apiGet(url, fallback = "No se pudo consultar la información.") {
  const data = await apiRequestData(
    url,
    { method: "GET" },
    {
      fallback,
      statusMessages: CONSULTA_STATUS_MESSAGES,
    }
  );

  return typeof data === "string" ? { mensaje: data } : data;
}

/**
 * Realiza una solicitud mutable del módulo de procesos usando el cliente HTTP
 * centralizado. Conserva los mensajes funcionales que ya utiliza la interfaz.
 */
export async function apiEnviar(
  url,
  options = {},
  fallback = "No se pudo procesar la solicitud."
) {
  const data = await apiRequestData(
    url,
    {
      headers: {
        "Content-Type": "application/json",
        ...(options?.headers || {}),
      },
      ...options,
    },
    {
      fallback,
      statusMessages: MUTACION_STATUS_MESSAGES,
    }
  );

  return typeof data === "string" ? { mensaje: data } : data;
}
