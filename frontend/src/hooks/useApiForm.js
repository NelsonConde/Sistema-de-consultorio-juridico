/**
 * Hook para manejar envíos de formularios al backend.
 *
 * Centraliza la lógica común de todos los formularios: estado de carga,
 * manejo de errores HTTP, toasts de éxito/error, y redirección en caso
 * de sesión expirada (401).
 *
 * @module hooks/useApiForm
 */

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  getApiErrorDescription,
  getApiErrorTitle,
  readResponseBody,
} from "@/lib/api";

/**
 * @typedef {Object} UseApiFormOptions
 * @property {string} endpoint - URL completa del endpoint del backend.
 * @property {"GET"|"POST"|"PUT"|"PATCH"|"DELETE"} [method="POST"] - Método HTTP.
 * @property {string} [successMessage="Registro exitoso"] - Mensaje del toast de éxito.
 */

/**
 * @typedef {Object} UseApiFormResult
 * @property {function(object): Promise<{success: boolean, data?: unknown, error?: unknown}>} submit
 *   Función que ejecuta la petición. Recibe el payload y devuelve el resultado.
 * @property {boolean} isSubmitting - `true` mientras la petición está en curso.
 */

/**
 * Hook para enviar formularios al backend con manejo automático de errores y toasts.
 *
 * Soporta cualquier método HTTP a través del parámetro `method`.
 * Ante un 401 redirige automáticamente al login (`/`).
 *
 * @param {UseApiFormOptions} options - Configuración del hook.
 * @returns {UseApiFormResult} Función `submit` y estado `isSubmitting`.
 *
 * @example
 * const { submit, isSubmitting } = useApiForm({
 *   endpoint: `${API_URL_BASE}/areas`,
 *   method: "POST",
 *   successMessage: "Área creada correctamente",
 * });
 *
 * const handleSubmit = async (data) => {
 *   const result = await submit(data);
 *   if (result.success) reset();
 * };
 */
export function useApiForm({ endpoint, method = "POST", successMessage = "Registro exitoso" }) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const router = useRouter();

  /**
   * Ejecuta la petición HTTP al backend con el payload recibido.
   *
   * @param {object} data - Datos del formulario a enviar como JSON.
   * @returns {Promise<{success: boolean, data?: unknown, error?: unknown}>}
   */
  const submit = async (data) => {
    setIsSubmitting(true);

    try {
      const response = await fetch(endpoint, {
        method,
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });

      const payload = await readResponseBody(response);

      if (response.status === 401) {
        toast.error("Sesión expirada", {
          description: "Debe iniciar sesión nuevamente",
        });

        router.replace("/");
        return { success: false, error: payload };
      }

      if (response.status === 403) {
        toast.error("No autorizado", {
          description: "No tiene permisos para esta acción",
        });

        return { success: false, error: payload };
      }

      if (response.ok) {
        toast.success(successMessage);
        return { success: true, data: payload };
      }

      toast.error(getApiErrorTitle(payload, "Error en la operación"), {
        description: getApiErrorDescription(payload),
      });

      return { success: false, error: payload };
    } catch (error) {
      console.error("Error de red:", error);

      toast.error("Error de conexión", {
        description: "Verifique que el backend esté disponible",
      });

      return { success: false, error };
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    submit,
    isSubmitting,
  };
}
