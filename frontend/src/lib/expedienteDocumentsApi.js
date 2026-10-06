import { apiClient } from "@/lib/apiClient";
import {
    ApiError,
    getApiErrorTitle,
    getResponseCorrelationId,
    readResponseBody,
} from "@/lib/api";

async function buildApiError(response, fallback) {
    const payload = await readResponseBody(response);
    const correlationId = getResponseCorrelationId(response, payload);

    return new ApiError(getApiErrorTitle(payload, fallback), {
        status: response.status,
        payload,
        response,
        correlationId,
    });
}

function buildQuery(filters = {}) {
    const params = new URLSearchParams();

    const values = {
        tipoDocumental: filters.tipoDocumental,
        resourceType: filters.resourceType,
        origen: filters.origen,
        autor: filters.autor,
        fechaDesde: filters.fechaDesde,
        fechaHasta: filters.fechaHasta,
    };

    Object.entries(values).forEach(([key, value]) => {
        if (value !== undefined && value !== null && String(value).trim() !== "") {
            params.set(key, String(value).trim());
        }
    });

    const query = params.toString();

    return query ? `?${query}` : "";
}

export async function listExpedienteDocuments(consultaId, filters = {}) {
    if (!consultaId) {
        throw new Error("La consulta es obligatoria");
    }

    const response = await apiClient.get(
        `/consultas/${consultaId}/expediente/archivos${buildQuery(filters)}`,
    );

    if (!response.ok) {
        throw await buildApiError(
            response,
            "No se pudieron consultar los documentos del expediente",
        );
    }

    const data = await response.json();

    return Array.isArray(data) ? data : [];
}

export const expedienteDocumentsApi = {
    list: listExpedienteDocuments,
};