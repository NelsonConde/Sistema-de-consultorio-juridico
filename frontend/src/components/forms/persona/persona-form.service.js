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
 * Form handling.
 * @param {{onSubmit:function, initialValues:Object}} props - Parameter description.
 * @returns {JSX.Element} Result value.
 */
