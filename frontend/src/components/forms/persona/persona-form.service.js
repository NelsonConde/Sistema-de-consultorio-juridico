import { API_URL_BASE } from "@/lib/config";
import { apiResponse } from "@/lib/api";

export async function fetchCatalogo(path) {
  const { response: res, data } = await apiResponse(`${API_URL_BASE}${path}`, {
    method: "GET",
  });

  if (!res.ok) {
    return [];
  }

  return Array.isArray(data) ? data : [];
}

/**
 * Formulario para crear o editar una persona.
 * @param {{onSubmit:function, initialValues:Object}} props - Props del componente.
 * @returns {JSX.Element} Componente de formulario.
 */
