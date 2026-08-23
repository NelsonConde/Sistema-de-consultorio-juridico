import { API_URL_BASE } from "@/lib/config";

export async function fetchCatalogo(path) {
  const res = await fetch(`${API_URL_BASE}${path}`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    return [];
  }

  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

/**
 * Formulario para crear o editar una persona.
 * @param {{onSubmit:function, initialValues:Object}} props - Props del componente.
 * @returns {JSX.Element} Componente de formulario.
 */
