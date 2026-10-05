"use client";

import { useCallback, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { expedienteDocumentsApi } from "@/lib/expedienteDocumentsApi";
import { fileApi } from "@/lib/fileApi";

const FILTROS_INICIALES = {
    tipoDocumental: "",
    resourceType: "",
    origen: "",
    autor: "",
    fechaDesde: "",
    fechaHasta: "",
};

function formatearTexto(value) {
    if (!value) {
        return "No disponible";
    }

    return String(value)
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatearFecha(value) {
    if (!value) {
        return "No disponible";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "No disponible";
    }

    return new Intl.DateTimeFormat("es-CO", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(date);
}

function formatearTamano(bytes) {
    const size = Number(bytes);

    if (!Number.isFinite(size) || size < 0) {
        return "No disponible";
    }

    if (size < 1024) {
        return `${size} B`;
    }

    if (size < 1024 * 1024) {
        return `${(size / 1024).toFixed(1)} KB`;
    }

    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

export function DocumentosExpedienteTab({ consultaId }) {
    const [documentos, setDocumentos] = useState([]);
    const [filtros, setFiltros] = useState(FILTROS_INICIALES);
    const [cargando, setCargando] = useState(false);
    const [error, setError] = useState("");
    const [errorDescarga, setErrorDescarga] = useState("");
    const [descargandoId, setDescargandoId] = useState(null);

    const cargarDocumentos = useCallback(async (filters) => {
        if (!consultaId) {
            setDocumentos([]);
            return;
        }

        try {
            setCargando(true);
            setError("");
            setErrorDescarga("");

            const data = await expedienteDocumentsApi.list(
                consultaId,
                filters,
            );

            setDocumentos(data);
        } catch (err) {
            if (err?.status === 403) {
                setDocumentos([]);
                setError(
                    "No tienes autorización para consultar los documentos de este expediente.",
                );
                return;
            }

            if (err?.status === 404) {
                setDocumentos([]);
                setError("No se encontró el expediente solicitado.");
                return;
            }

            setDocumentos([]);
            setError(
                "No fue posible cargar los documentos del expediente.",
            );
        } finally {
            setCargando(false);
        }
    }, [consultaId]);

    useEffect(() => {
        cargarDocumentos(FILTROS_INICIALES);
    }, [cargarDocumentos]);

    function handleFiltroChange(event) {
        const { name, value } = event.target;

        setFiltros((prev) => ({
            ...prev,
            [name]: value,
        }));
    }

    function aplicarFiltros(event) {
        event.preventDefault();
        cargarDocumentos(filtros);
    }

    function limpiarFiltros() {
        setFiltros(FILTROS_INICIALES);
        cargarDocumentos(FILTROS_INICIALES);
    }

    async function descargarDocumento(documento) {
        try {
            setDescargandoId(documento.id);
            setErrorDescarga("");

            await fileApi.download(documento);
        } catch (err) {
            if (err?.status === 403) {
                setDocumentos([]);
                setError(
                    "No tienes autorización para consultar o descargar los documentos de este expediente.",
                );
                return;
            }

            if (err?.status === 404) {
                setErrorDescarga(
                    "El documento solicitado ya no está disponible.",
                );
                return;
            }

            if (err?.status === 409) {
                setErrorDescarga(
                    err?.message ||
                    "El documento no está disponible para descarga en este momento.",
                );
                return;
            }

            setErrorDescarga(
                err?.message ||
                "No fue posible descargar el documento. Intenta nuevamente.",
            );
        } finally {
            setDescargandoId(null);
        }
    }

    return (
        <div className="space-y-4">
            <form
                onSubmit={aplicarFiltros}
                className="rounded-lg border bg-muted/20 p-4 space-y-4"
            >
                <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
                    <select
                        name="resourceType"
                        value={filtros.resourceType}
                        onChange={handleFiltroChange}
                        className="rounded-md border bg-background px-3 py-2 text-sm"
                    >
                        <option value="">Todos los orígenes</option>
                        <option value="CONSULTA">Consulta</option>
                        <option value="SEGUIMIENTO">Seguimiento</option>
                        <option value="RESPUESTA">Respuesta</option>
                        <option value="PROCESO">Proceso</option>
                        <option value="CONCILIACION">Conciliación</option>
                    </select>

                    <input
                        name="tipoDocumental"
                        value={filtros.tipoDocumental}
                        onChange={handleFiltroChange}
                        placeholder="Tipo documental"
                        className="rounded-md border bg-background px-3 py-2 text-sm"
                    />

                    <input
                        name="autor"
                        value={filtros.autor}
                        onChange={handleFiltroChange}
                        placeholder="Autor"
                        className="rounded-md border bg-background px-3 py-2 text-sm"
                    />

                    <input
                        type="date"
                        name="fechaDesde"
                        value={filtros.fechaDesde}
                        onChange={handleFiltroChange}
                        className="rounded-md border bg-background px-3 py-2 text-sm"
                    />

                    <input
                        type="date"
                        name="fechaHasta"
                        value={filtros.fechaHasta}
                        onChange={handleFiltroChange}
                        className="rounded-md border bg-background px-3 py-2 text-sm"
                    />
                </div>

                <div className="flex flex-wrap gap-2">
                    <Button type="submit" disabled={cargando}>
                        {cargando ? "Buscando..." : "Aplicar filtros"}
                    </Button>

                    <Button
                        type="button"
                        variant="outline"
                        onClick={limpiarFiltros}
                        disabled={cargando}
                    >
                        Limpiar
                    </Button>
                </div>
            </form>

            {errorDescarga && !error && (
                <div
                    role="alert"
                    className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm"
                >
                    {errorDescarga}
                </div>
            )}

            {cargando ? (
                <div className="rounded-lg border p-6 text-center text-sm text-muted-foreground">
                    Cargando documentos...
                </div>
            ) : error ? (
                <div
                    role="alert"
                    className="rounded-lg border p-6 text-sm"
                >
                    {error}
                </div>
            ) : documentos.length === 0 ? (
                <div className="rounded-lg border p-6 text-center text-sm text-muted-foreground">
                    No hay documentos para mostrar.
                </div>
            ) : (
                <div className="space-y-3">
                    <div className="hidden overflow-x-auto rounded-lg border md:block">
                        <table className="w-full text-sm">
                            <thead className="bg-muted">
                            <tr>
                                <th className="px-3 py-3 text-left font-medium">
                                    Documento
                                </th>
                                <th className="px-3 py-3 text-left font-medium">
                                    Tipo
                                </th>
                                <th className="px-3 py-3 text-left font-medium">
                                    Origen
                                </th>
                                <th className="px-3 py-3 text-left font-medium">
                                    Autor
                                </th>
                                <th className="px-3 py-3 text-left font-medium">
                                    Fecha
                                </th>
                                <th className="px-3 py-3 text-left font-medium">
                                    Versión
                                </th>
                                <th className="px-3 py-3 text-left font-medium">
                                    Tamaño
                                </th>
                                <th className="px-3 py-3 text-left font-medium">
                                    Estado
                                </th>
                                <th className="px-3 py-3 text-right font-medium">
                                    Acción
                                </th>
                            </tr>
                            </thead>

                            <tbody>
                            {documentos.map((documento) => (
                                <tr
                                    key={documento.id}
                                    className="border-t"
                                >
                                    <td className="max-w-[240px] px-3 py-3">
                                            <span
                                                className="block truncate font-medium"
                                                title={documento.fileName}
                                            >
                                                {documento.fileName || "Documento"}
                                            </span>
                                    </td>

                                    <td className="px-3 py-3">
                                        {formatearTexto(
                                            documento.tipoDocumental,
                                        )}
                                    </td>

                                    <td className="px-3 py-3">
                                        {formatearTexto(
                                            documento.resourceType,
                                        )}
                                    </td>

                                    <td className="px-3 py-3">
                                        {documento.autorUsername ||
                                            "No disponible"}
                                    </td>

                                    <td className="whitespace-nowrap px-3 py-3">
                                        {formatearFecha(
                                            documento.createdAt,
                                        )}
                                    </td>

                                    <td className="px-3 py-3">
                                        {documento.version ?? "—"}
                                    </td>

                                    <td className="whitespace-nowrap px-3 py-3">
                                        {formatearTamano(
                                            documento.size,
                                        )}
                                    </td>

                                    <td className="px-3 py-3">
                                        {formatearTexto(
                                            documento.status,
                                        )}
                                    </td>

                                    <td className="px-3 py-3 text-right">
                                        <Button
                                            type="button"
                                            variant="outline"
                                            size="sm"
                                            onClick={() =>
                                                descargarDocumento(
                                                    documento,
                                                )
                                            }
                                            disabled={
                                                descargandoId ===
                                                documento.id
                                            }
                                        >
                                            {descargandoId === documento.id
                                                ? "Descargando..."
                                                : "Descargar"}
                                        </Button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>

                    <div className="space-y-3 md:hidden">
                        {documentos.map((documento) => (
                            <article
                                key={documento.id}
                                className="rounded-lg border bg-background p-4"
                            >
                                <p className="break-words font-medium">
                                    {documento.fileName || "Documento"}
                                </p>

                                <dl className="mt-3 grid grid-cols-2 gap-x-3 gap-y-2 text-sm">
                                    <dt className="text-muted-foreground">
                                        Tipo
                                    </dt>
                                    <dd>
                                        {formatearTexto(
                                            documento.tipoDocumental,
                                        )}
                                    </dd>

                                    <dt className="text-muted-foreground">
                                        Origen
                                    </dt>
                                    <dd>
                                        {formatearTexto(
                                            documento.resourceType,
                                        )}
                                    </dd>

                                    <dt className="text-muted-foreground">
                                        Autor
                                    </dt>
                                    <dd>
                                        {documento.autorUsername ||
                                            "No disponible"}
                                    </dd>

                                    <dt className="text-muted-foreground">
                                        Fecha
                                    </dt>
                                    <dd>
                                        {formatearFecha(
                                            documento.createdAt,
                                        )}
                                    </dd>

                                    <dt className="text-muted-foreground">
                                        Versión
                                    </dt>
                                    <dd>
                                        {documento.version ?? "—"}
                                    </dd>

                                    <dt className="text-muted-foreground">
                                        Tamaño
                                    </dt>
                                    <dd>
                                        {formatearTamano(
                                            documento.size,
                                        )}
                                    </dd>

                                    <dt className="text-muted-foreground">
                                        Estado
                                    </dt>
                                    <dd>
                                        {formatearTexto(
                                            documento.status,
                                        )}
                                    </dd>
                                </dl>

                                <div className="mt-4 flex justify-end">
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        onClick={() =>
                                            descargarDocumento(
                                                documento,
                                            )
                                        }
                                        disabled={
                                            descargandoId ===
                                            documento.id
                                        }
                                    >
                                        {descargandoId === documento.id
                                            ? "Descargando..."
                                            : "Descargar"}
                                    </Button>
                                </div>
                            </article>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}