"use client";

import { ArrowRight, History, RefreshCw, ShieldX } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  getApiErrorCorrelationId,
  getApiErrorStatus,
  withErrorReference,
} from "@/lib/api";
import {
  obtenerHistorialResponsables,
  obtenerNombreResponsableConsulta,
} from "@/lib/responsablesConsultaApi";

const ETIQUETAS_ROL = {
  ESTUDIANTE: "Estudiante",
  ASESOR: "Asesor",
  MONITOR: "Monitor",
};

function etiquetaRol(tipo) {
  return (
    ETIQUETAS_ROL[String(tipo || "").toUpperCase()] || tipo || "Responsable"
  );
}

function formatearFecha(valor) {
  if (!valor) return "Fecha no disponible";
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) return "Fecha no disponible";

  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(fecha);
}

function ResponsableHistorico({ nombre, vacio = "Sin responsable asignado" }) {
  if (!nombre) return <span className="text-muted-foreground">{vacio}</span>;

  return <span className="font-medium">{nombre}</span>;
}

function CargandoHistorial() {
  return (
    <output className="block space-y-3" aria-label="Cargando historial">
      {["a", "b", "c"].map((key) => (
        <div key={key} className="rounded-lg border p-4">
          <div className="flex justify-between gap-4">
            <Skeleton className="h-5 w-24" />
            <Skeleton className="h-4 w-36" />
          </div>
          <Skeleton className="mt-4 h-4 w-4/5" />
          <Skeleton className="mt-2 h-4 w-2/3" />
        </div>
      ))}
    </output>
  );
}

async function completarNombresResponsables(eventos, signal) {
  const solicitudes = new Map();

  for (const evento of eventos) {
    const referencias = [
      [evento.responsableAnteriorId, evento.responsableAnteriorNombre],
      [evento.responsableNuevoId, evento.responsableNuevoNombre],
    ];

    for (const [id, nombre] of referencias) {
      if (!id || nombre) continue;
      const clave = `${evento.tipoResponsable}:${id}`;

      if (!solicitudes.has(clave)) {
        solicitudes.set(
          clave,
          obtenerNombreResponsableConsulta(evento.tipoResponsable, id, {
            signal,
          }).catch(() => null),
        );
      }
    }
  }

  const nombres = new Map(
    await Promise.all(
      [...solicitudes.entries()].map(async ([clave, solicitud]) => [
        clave,
        await solicitud,
      ]),
    ),
  );

  return eventos.map((evento) => ({
    ...evento,
    responsableAnteriorNombre:
      evento.responsableAnteriorNombre ||
      nombres.get(
        `${evento.tipoResponsable}:${evento.responsableAnteriorId}`,
      ) ||
      null,
    responsableNuevoNombre:
      evento.responsableNuevoNombre ||
      nombres.get(`${evento.tipoResponsable}:${evento.responsableNuevoId}`) ||
      null,
  }));
}

export function HistorialResponsablesTab({ consultaId, refreshKey = 0 }) {
  const router = useRouter();
  const [eventos, setEventos] = useState([]);
  const [estado, setEstado] = useState("loading");
  const [mensaje, setMensaje] = useState("");
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const revisionSolicitud = `${refreshKey}:${intento}`;

    async function cargarHistorial() {
      if (!consultaId) return;

      setEstado("loading");
      setMensaje("");

      try {
        const historial = await obtenerHistorialResponsables(consultaId, {
          signal: controller.signal,
        });
        const historialConNombres = await completarNombresResponsables(
          historial,
          controller.signal,
        );
        setEventos(historialConNombres);
        setEstado(historial.length > 0 ? "success" : "empty");
      } catch (requestError) {
        if (requestError?.name === "AbortError") return;

        const status = getApiErrorStatus(requestError);
        const referencia = getApiErrorCorrelationId(requestError);

        if (status === 401) {
          router.replace("/");
          return;
        }

        setEventos([]);
        if (status === 403) {
          setEstado("forbidden");
          setMensaje(
            withErrorReference(
              "No tienes permiso o alcance vigente para consultar este historial.",
              referencia,
            ),
          );
          return;
        }

        if (status === 404) {
          setEstado("error");
          setMensaje(
            withErrorReference(
              "El servicio de historial todavía no está disponible para esta consulta.",
              referencia,
            ),
          );
          return;
        }

        setEstado("error");
        const descripcion =
          status === 0
            ? "No se pudo conectar con el servidor. Verifica la conexión e intenta nuevamente."
            : requestError?.message ||
              "No fue posible cargar el historial de responsables.";
        setMensaje(withErrorReference(descripcion, referencia));
      }
    }

    cargarHistorial();
    return () => controller.abort(revisionSolicitud);
  }, [consultaId, refreshKey, intento, router]);

  if (estado === "loading") return <CargandoHistorial />;

  if (estado === "forbidden") {
    return (
      <div className="rounded-lg border border-amber-200 bg-amber-50 p-5 text-amber-950 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-100">
        <div className="flex items-start gap-3">
          <ShieldX className="mt-0.5 h-5 w-5 shrink-0" aria-hidden="true" />
          <div>
            <h3 className="font-medium">Historial no autorizado</h3>
            <p className="mt-1 whitespace-pre-line text-sm">{mensaje}</p>
          </div>
        </div>
      </div>
    );
  }

  if (estado === "error") {
    return (
      <div className="rounded-lg border p-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 className="font-medium">No se pudo mostrar el historial</h3>
            <p className="mt-1 whitespace-pre-line text-sm text-muted-foreground">
              {mensaje}
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            onClick={() => setIntento((valor) => valor + 1)}
          >
            <RefreshCw className="h-4 w-4" aria-hidden="true" />
            Reintentar
          </Button>
        </div>
      </div>
    );
  }

  if (estado === "empty") {
    return (
      <div className="rounded-lg border border-dashed p-8 text-center">
        <History
          className="mx-auto h-8 w-8 text-muted-foreground"
          aria-hidden="true"
        />
        <h3 className="mt-3 font-medium">Sin reasignaciones registradas</h3>
      </div>
    );
  }

  return (
    <section
      aria-labelledby="historial-responsables-titulo"
      className="space-y-4"
    >
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 id="historial-responsables-titulo" className="font-medium">
            Historial de responsables
          </h3>
        </div>
        <Button
          type="button"
          size="sm"
          variant="ghost"
          onClick={() => setIntento((valor) => valor + 1)}
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Actualizar
        </Button>
      </div>

      <ol className="space-y-3">
        {eventos.map((evento) => (
          <li
            key={
              evento.id ??
              `${evento.ocurridoEn}-${evento.tipoResponsable}-${evento.responsableNuevoId}`
            }
            className="rounded-lg border bg-card p-4"
          >
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <Badge variant="secondary">
                {etiquetaRol(evento.tipoResponsable)}
              </Badge>
              <time
                className="text-xs text-muted-foreground"
                dateTime={evento.ocurridoEn || undefined}
              >
                {formatearFecha(evento.ocurridoEn)}
              </time>
            </div>

            <div className="mt-4 grid gap-3 sm:grid-cols-[1fr_auto_1fr] sm:items-center">
              <div>
                <p className="text-xs text-muted-foreground">
                  Responsable anterior
                </p>
                <ResponsableHistorico
                  nombre={evento.responsableAnteriorNombre}
                />
              </div>
              <ArrowRight
                className="h-5 w-5 text-muted-foreground"
                aria-hidden="true"
              />
              <div>
                <p className="text-xs text-muted-foreground">
                  Nuevo responsable
                </p>
                <ResponsableHistorico
                  nombre={evento.responsableNuevoNombre}
                  vacio="Nombre no disponible"
                />
              </div>
            </div>

            <div className="mt-4 rounded-md bg-muted/40 p-3">
              <p className="text-xs font-medium text-muted-foreground">
                Motivo
              </p>
              <p className="mt-1 whitespace-pre-wrap text-sm">
                {evento.motivo}
              </p>
            </div>

            <p className="mt-3 text-xs text-muted-foreground">
              Registrado por{" "}
              <ResponsableHistorico
                nombre={evento.actorNombre}
                vacio="Nombre no disponible"
              />
            </p>
          </li>
        ))}
      </ol>
    </section>
  );
}
