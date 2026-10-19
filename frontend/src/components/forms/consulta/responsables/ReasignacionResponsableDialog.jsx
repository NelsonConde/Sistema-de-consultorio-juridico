"use client";

import { AlertTriangle, ArrowRight, LoaderCircle } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { RemotePagedSelect } from "@/components/forms/parts/RemotePagedSelect";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  getApiErrorCorrelationId,
  getApiErrorStatus,
  requireResourceVersion,
  withErrorReference,
} from "@/lib/api";
import {
  reasignarResponsableConsulta,
  TIPOS_RESPONSABLE_CONSULTA,
} from "@/lib/responsablesConsultaApi";
import {
  idNormalizado,
  obtenerAreaIdAsesor,
  obtenerAsesorIdEstudiante,
} from "../consultas-juridicas.utils";

const CONFIGURACION_ROLES = {
  [TIPOS_RESPONSABLE_CONSULTA.ESTUDIANTE]: {
    etiqueta: "Estudiante",
    campoId: "estudianteId",
    endpoint: (consulta) =>
      `/estudiantes/activos/asesor/${encodeURIComponent(String(consulta?.asesorId || ""))}`,
    recurso: "estudiantes activos",
    placeholder: "Buscar estudiante...",
  },
  [TIPOS_RESPONSABLE_CONSULTA.ASESOR]: {
    etiqueta: "Asesor",
    campoId: "asesorId",
    endpoint: () => "/asesores/activos",
    recurso: "asesores activos",
    placeholder: "Buscar asesor...",
  },
  [TIPOS_RESPONSABLE_CONSULTA.MONITOR]: {
    etiqueta: "Monitor",
    campoId: "monitorId",
    endpoint: () => "/monitores/activos",
    recurso: "monitores activos",
    placeholder: "Buscar monitor...",
  },
};

function etiquetaPersona(item, fallbackId = null) {
  const nombre = item?.nombre || item?.nombreCompleto;
  if (nombre) return nombre;
  return fallbackId ? "Nombre no disponible" : "Sin responsable asignado";
}

export function ReasignacionResponsableDialog({
  open,
  onOpenChange,
  consulta,
  responsables,
  onSuccess,
  onConflict,
  onSessionExpired,
}) {
  const [tipoResponsable, setTipoResponsable] = useState("");
  const [nuevoResponsableId, setNuevoResponsableId] = useState("");
  const [nuevoResponsable, setNuevoResponsable] = useState(null);
  const [motivo, setMotivo] = useState("");
  const [paso, setPaso] = useState("datos");
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (!open) return;
    setTipoResponsable("");
    setNuevoResponsableId("");
    setNuevoResponsable(null);
    setMotivo("");
    setPaso("datos");
    setError("");
    setEnviando(false);
  }, [open]);

  const configuracion = CONFIGURACION_ROLES[tipoResponsable] || null;
  const responsableActual = useMemo(() => {
    if (!configuracion) return { id: null, item: null, etiqueta: "" };

    const id = consulta?.[configuracion.campoId] ?? null;
    const item = responsables?.[tipoResponsable] || null;
    return { id, item, etiqueta: etiquetaPersona(item, id) };
  }, [configuracion, consulta, responsables, tipoResponsable]);

  const filtrarResponsable = useMemo(() => {
    if (!configuracion) return null;

    const actualId = idNormalizado(responsableActual.id);
    const areaId = idNormalizado(consulta?.areaId);
    const asesorEstudianteId =
      obtenerAsesorIdEstudiante(responsables?.ESTUDIANTE) ||
      (consulta?.estudianteId ? idNormalizado(consulta?.asesorId) : "");

    return (item) => {
      if (idNormalizado(item?.id) === actualId) return false;

      if (tipoResponsable === TIPOS_RESPONSABLE_CONSULTA.ASESOR) {
        const candidatoId = idNormalizado(item?.id);
        return (
          obtenerAreaIdAsesor(item) === areaId &&
          (!asesorEstudianteId || candidatoId === asesorEstudianteId)
        );
      }

      if (tipoResponsable === TIPOS_RESPONSABLE_CONSULTA.ESTUDIANTE) {
        return (
          obtenerAsesorIdEstudiante(item) === idNormalizado(consulta?.asesorId)
        );
      }

      return true;
    };
  }, [
    configuracion,
    consulta?.areaId,
    consulta?.asesorId,
    consulta?.estudianteId,
    responsableActual.id,
    responsables?.ESTUDIANTE,
    tipoResponsable,
  ]);

  function cambiarRol(event) {
    setTipoResponsable(event.target.value);
    setNuevoResponsableId("");
    setNuevoResponsable(null);
    setPaso("datos");
    setError("");
  }

  function seleccionarResponsable(id, item) {
    if (
      tipoResponsable === TIPOS_RESPONSABLE_CONSULTA.ASESOR &&
      item &&
      obtenerAreaIdAsesor(item) !== idNormalizado(consulta?.areaId)
    ) {
      setError("El asesor seleccionado no pertenece al área de la consulta.");
      return false;
    }

    if (
      tipoResponsable === TIPOS_RESPONSABLE_CONSULTA.ESTUDIANTE &&
      item &&
      obtenerAsesorIdEstudiante(item) !== idNormalizado(consulta?.asesorId)
    ) {
      setError(
        "El estudiante seleccionado no pertenece al asesor actual de la consulta.",
      );
      return false;
    }

    setNuevoResponsableId(id ? String(id) : "");
    setNuevoResponsable(item || null);
    setError("");
    return true;
  }

  function validar() {
    if (!configuracion) return "Selecciona el rol que deseas reasignar.";
    if (!nuevoResponsableId) return "Selecciona el nuevo responsable.";
    if (
      idNormalizado(nuevoResponsableId) === idNormalizado(responsableActual.id)
    ) {
      return "El nuevo responsable debe ser diferente del responsable actual.";
    }
    if (!motivo.trim()) return "Escribe el motivo de la reasignación.";

    try {
      requireResourceVersion(consulta, "consulta");
    } catch (versionError) {
      return versionError.message;
    }

    return "";
  }

  function continuarAConfirmacion() {
    const mensaje = validar();
    if (mensaje) {
      setError(mensaje);
      return;
    }

    setError("");
    setPaso("confirmacion");
  }

  async function confirmarReasignacion() {
    const mensaje = validar();
    if (mensaje) {
      setError(mensaje);
      setPaso("datos");
      return;
    }

    setEnviando(true);
    setError("");

    try {
      const consultaActualizada = await reasignarResponsableConsulta({
        consultaId: consulta.id,
        tipoResponsable,
        nuevoResponsableId,
        motivo,
        version: requireResourceVersion(consulta, "consulta"),
      });

      await onSuccess?.(consultaActualizada, {
        tipoResponsable,
        nuevoResponsable,
        nuevoResponsableId: Number(nuevoResponsableId),
      });
      onOpenChange(false);
    } catch (requestError) {
      const status = getApiErrorStatus(requestError);
      const referencia = getApiErrorCorrelationId(requestError);

      if (status === 401) {
        onSessionExpired?.();
        return;
      }

      if (status === 409) {
        try {
          await onConflict?.();
          setPaso("datos");
          setError(
            withErrorReference(
              "La consulta cambió mientras confirmabas. Se actualizó su versión; revisa los datos y confirma nuevamente.",
              referencia,
            ),
          );
        } catch {
          setError(
            withErrorReference(
              "La consulta cambió y no fue posible cargar su versión actual. Cierra el diálogo y vuelve a abrir el detalle.",
              referencia,
            ),
          );
        }
        return;
      }

      const fallback =
        status === 403
          ? "No tienes permiso o alcance sobre esta consulta."
          : status === 400
            ? "La reasignación no cumple las reglas de la consulta."
            : "No fue posible completar la reasignación. Intenta nuevamente.";
      const descripcion =
        status === 0
          ? "No se pudo conectar con el servidor. Conservamos los datos para que puedas reintentar."
          : requestError?.message || fallback;

      setError(withErrorReference(descripcion, referencia));
    } finally {
      setEnviando(false);
    }
  }

  function cambiarApertura(nextOpen) {
    if (!nextOpen && enviando) return;
    onOpenChange(nextOpen);
  }

  const etiquetaNueva = nuevoResponsableId
    ? etiquetaPersona(nuevoResponsable, nuevoResponsableId)
    : "";

  return (
    <Dialog open={open} onOpenChange={cambiarApertura}>
      <DialogContent
        className="sm:max-w-2xl"
        showCloseButton={!enviando}
        aria-describedby={undefined}
      >
        <DialogHeader>
          <DialogTitle>Reasignar responsable</DialogTitle>
        </DialogHeader>

        {paso === "datos" ? (
          <div className="space-y-5 py-1">
            <div className="space-y-1.5">
              <label htmlFor="tipo-responsable" className="text-sm font-medium">
                Rol a reasignar <span className="text-destructive">*</span>
              </label>
              <select
                id="tipo-responsable"
                value={tipoResponsable}
                onChange={cambiarRol}
                disabled={enviando}
                className="h-9 w-full rounded-md border bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              >
                <option value="">Selecciona un rol</option>
                <option value={TIPOS_RESPONSABLE_CONSULTA.ESTUDIANTE}>
                  Estudiante
                </option>
                <option value={TIPOS_RESPONSABLE_CONSULTA.ASESOR}>
                  Asesor
                </option>
                <option value={TIPOS_RESPONSABLE_CONSULTA.MONITOR}>
                  Monitor
                </option>
              </select>
            </div>

            {configuracion && (
              <>
                <div className="rounded-lg border bg-muted/30 p-3">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                    Responsable actual
                  </p>
                  <p className="mt-1 text-sm font-medium">
                    {responsableActual.etiqueta}
                  </p>
                </div>

                <RemotePagedSelect
                  key={tipoResponsable}
                  label={`Nuevo ${configuracion.etiqueta.toLowerCase()}`}
                  value={nuevoResponsableId}
                  selectedLabel={etiquetaNueva}
                  onChange={seleccionarResponsable}
                  endpoint={configuracion.endpoint(consulta)}
                  legacyEndpoint={configuracion.endpoint(consulta)}
                  resourceName={configuracion.recurso}
                  sortBy="nombre"
                  direction="asc"
                  placeholder={`Selecciona un ${configuracion.etiqueta.toLowerCase()}`}
                  searchPlaceholder={configuracion.placeholder}
                  getOptionLabel={(item) => etiquetaPersona(item, item?.id)}
                  filterItem={filtrarResponsable}
                  disabled={
                    enviando ||
                    (tipoResponsable ===
                      TIPOS_RESPONSABLE_CONSULTA.ESTUDIANTE &&
                      !consulta?.asesorId)
                  }
                  emptyMessage="No hay responsables disponibles para esta consulta."
                  required
                />
              </>
            )}

            <div className="space-y-1.5">
              <label
                htmlFor="motivo-reasignacion"
                className="text-sm font-medium"
              >
                Motivo <span className="text-destructive">*</span>
              </label>
              <textarea
                id="motivo-reasignacion"
                value={motivo}
                onChange={(event) => {
                  setMotivo(event.target.value);
                  setError("");
                }}
                rows={4}
                disabled={enviando}
                placeholder="Explica por qué se realiza el cambio de responsable"
                className="w-full resize-y rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
              />
            </div>
          </div>
        ) : (
          <div className="space-y-4 py-1">
            <div className="rounded-lg border p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                {configuracion?.etiqueta}
              </p>
              <div className="mt-3 grid gap-3 sm:grid-cols-[1fr_auto_1fr] sm:items-center">
                <div>
                  <p className="text-xs text-muted-foreground">
                    Responsable actual
                  </p>
                  <p className="font-medium">{responsableActual.etiqueta}</p>
                </div>
                <ArrowRight
                  className="h-5 w-5 text-muted-foreground"
                  aria-hidden="true"
                />
                <div>
                  <p className="text-xs text-muted-foreground">
                    Nuevo responsable
                  </p>
                  <p className="font-medium">{etiquetaNueva}</p>
                </div>
              </div>
            </div>

            <div className="rounded-lg border p-4">
              <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                Motivo
              </p>
              <p className="mt-2 whitespace-pre-wrap text-sm">
                {motivo.trim()}
              </p>
            </div>

            <div className="flex gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-100">
              <AlertTriangle
                className="mt-0.5 h-4 w-4 shrink-0"
                aria-hidden="true"
              />
              <p>
                Verifica el cambio antes de confirmar. El historial no puede
                editarse ni eliminarse.
              </p>
            </div>
          </div>
        )}

        {error && (
          <div
            role="alert"
            className="whitespace-pre-line rounded-lg border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive"
          >
            {error}
          </div>
        )}

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={enviando}
            onClick={() =>
              paso === "confirmacion" ? setPaso("datos") : onOpenChange(false)
            }
          >
            {paso === "confirmacion" ? "Volver" : "Cancelar"}
          </Button>
          {paso === "datos" ? (
            <Button
              type="button"
              onClick={continuarAConfirmacion}
              disabled={enviando}
            >
              Revisar cambio
            </Button>
          ) : (
            <Button
              type="button"
              onClick={confirmarReasignacion}
              disabled={enviando}
            >
              {enviando && (
                <LoaderCircle
                  className="h-4 w-4 animate-spin"
                  aria-hidden="true"
                />
              )}
              {enviando ? "Reasignando..." : "Confirmar reasignación"}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
