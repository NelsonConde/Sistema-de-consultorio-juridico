/**
 * Form handling.
 *
 * State handling.
 * Error handling.
 * Handles expired-session responses consistently.
 *
 * @module hooks/useApiForm
 */

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  apiResponse,
  getApiErrorDescription,
  getApiErrorTitle,
} from "@/lib/api";

/**
 * @typedef {Object} UseApiFormOptions
 * @property {string} endpoint - Parameter description.
 * @property {"GET"|"POST"|"PUT"|"PATCH"|"DELETE"} [method="POST"] - Implementation detail.
 * @property {string} [successMessage="Registro exitoso"] - Parameter description.
 */

/**
 * @typedef {Object} UseApiFormResult
 * @property {function(object): Promise<{success: boolean, data?: unknown, error?: unknown}>} submit
 * Implementation detail.
 * @property {boolean} isSubmitting - True while the request is in progress.
 */

/**
 * Error handling.
 *
 * Supports any HTTP method through the `method` parameter.
 * Implementation detail.
 *
 * @param {UseApiFormOptions} options - Parameter description.
 * @returns {UseApiFormResult} Result value.
 *
 * @example
 * const { submit, isSubmitting } = useApiForm({
 *   endpoint: `${API_URL_BASE}/areas`,
 *   method: "POST",
 * Implementation detail.
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
   * Implementation detail.
   *
   * @param {object} data - Parameter description.
   * @returns {Promise<{success: boolean, data?: unknown, error?: unknown}>}
   */
  const submit = async (data) => {
    setIsSubmitting(true);

    try {
      const { response, data: payload } = await apiResponse(endpoint, {
        method,
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      });

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
