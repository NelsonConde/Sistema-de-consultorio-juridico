"use client";

import { LockKeyhole, UserRoundCog } from "lucide-react";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { normalizarEstadoConsulta } from "../consultas-juridicas.utils";
import { ReasignacionResponsableDialog } from "./ReasignacionResponsableDialog";

const ESTADOS_OPERATIVOS = new Set([
  "PENDIENTE",
  "ACTIVO",
  "EN_PROCESO",
  "URGENTE",
]);

function etiquetaResponsable(item, id) {
  const nombre = item?.nombre || item?.nombreCompleto;

  if (nombre) return nombre;
  return id ? "Nombre no disponible" : "Sin asignar";
}

export function ResponsablesConsultaPanel({
  consulta,
  responsables,
  puedeReasignar,
  onSuccess,
  onConflict,
  onSessionExpired,
}) {
  const [dialogoAbierto, setDialogoAbierto] = useState(false);
  const estado = normalizarEstadoConsulta(consulta?.estado);
  const estadoOperativo = ESTADOS_OPERATIVOS.has(estado);
  const accionDisponible = puedeReasignar && estadoOperativo;

  const items = [
    {
      tipo: "ASESOR",
      etiqueta: "Asesor",
      id: consulta?.asesorId,
      item: responsables?.ASESOR,
    },
    {
      tipo: "MONITOR",
      etiqueta: "Monitor",
      id: consulta?.monitorId,
      item: responsables?.MONITOR,
    },
    {
      tipo: "ESTUDIANTE",
      etiqueta: "Estudiante",
      id: consulta?.estudianteId,
      item: responsables?.ESTUDIANTE,
    },
  ];

  return (
    <section
      className="md:col-span-2 lg:col-span-3 rounded-lg border bg-muted/20 p-4"
      aria-labelledby="responsables-consulta-titulo"
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <UserRoundCog
              className="h-5 w-5 text-muted-foreground"
              aria-hidden="true"
            />
            <h3 id="responsables-consulta-titulo" className="font-medium">
              Responsables de la consulta
            </h3>
          </div>
        </div>

        {puedeReasignar && (
          <Button
            type="button"
            variant="outline"
            disabled={!accionDisponible}
            onClick={() => setDialogoAbierto(true)}
          >
            <UserRoundCog className="h-4 w-4" aria-hidden="true" />
            Reasignar responsable
          </Button>
        )}
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        {items.map(({ tipo, etiqueta, id, item }) => (
          <div key={tipo} className="rounded-lg border bg-background p-3">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
              {etiqueta}
            </p>
            <p className="mt-1 text-sm font-medium">
              {etiquetaResponsable(item, id)}
            </p>
          </div>
        ))}
      </div>

      {!puedeReasignar && (
        <div className="mt-3 flex items-center gap-2 text-sm text-muted-foreground">
          <LockKeyhole className="h-4 w-4" aria-hidden="true" />
          No tienes permiso para reasignar responsables.
        </div>
      )}

      {puedeReasignar && !estadoOperativo && (
        <div className="mt-3 flex items-center gap-2 text-sm text-muted-foreground">
          <LockKeyhole className="h-4 w-4" aria-hidden="true" />
          <span>
            La reasignación no está disponible para una consulta en estado
          </span>
          <Badge variant="secondary">{estado || "no definido"}</Badge>
        </div>
      )}

      <ReasignacionResponsableDialog
        open={dialogoAbierto}
        onOpenChange={setDialogoAbierto}
        consulta={consulta}
        responsables={responsables}
        onSuccess={onSuccess}
        onConflict={onConflict}
        onSessionExpired={onSessionExpired}
      />
    </section>
  );
}
