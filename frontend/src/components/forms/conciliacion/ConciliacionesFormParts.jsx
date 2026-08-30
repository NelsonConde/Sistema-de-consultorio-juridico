"use client";

import React, { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { idConsulta, labelConsultaBusqueda, obtenerDetalleConsulta, normalizarTexto, personaResumen } from "./conciliaciones.utils";

export function ModalBuscarConsulta({ abierto, consultas, busqueda, setBusqueda, onSeleccionar, onCerrar, consultaIdSeleccionada }) {
  if (!abierto) return null;

  const texto = normalizarTexto(busqueda);
  const consultasFiltradas = texto
    ? consultas.filter((consulta) => labelConsultaBusqueda(consulta).includes(texto))
    : consultas;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
      <div className="w-full max-w-2xl rounded-xl border bg-background p-6 shadow-lg">
        <div className="mb-4 flex items-center justify-between gap-3">
          <div>
            <h3 className="text-lg font-semibold">Seleccionar consulta</h3>
            <p className="text-sm text-muted-foreground">
              Busca por ID, descripción, parte, documento, estado, área, tema o tipo.
            </p>
          </div>
          <button
            type="button"
            onClick={onCerrar}
            className="rounded-full px-2 py-1 text-xl text-muted-foreground hover:bg-muted hover:text-foreground"
            aria-label="Cerrar selector de consultas"
          >
            ✕
          </button>
        </div>

        <div className="relative mb-4">
          <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <input
            autoFocus
            type="text"
            value={busqueda}
            onChange={(event) => setBusqueda(event.target.value)}
            placeholder="Buscar consulta..."
            className="h-10 w-full rounded-lg border bg-background pl-9 pr-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
        </div>

        <div className="max-h-80 space-y-2 overflow-y-auto pr-1">
          {consultasFiltradas.length === 0 ? (
            <div className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">
              No se encontraron consultas con ese criterio.
            </div>
          ) : (
            consultasFiltradas.map((consulta) => {
              const detalle = obtenerDetalleConsulta(consulta);
              const seleccionado = String(consultaIdSeleccionada) === String(detalle.id);

              return (
                <button
                  key={detalle.id}
                  type="button"
                  onClick={() => onSeleccionar(consulta)}
                  className={`w-full rounded-lg border px-4 py-3 text-left text-sm transition-colors hover:bg-muted/60 ${
                    seleccionado ? "border-primary bg-primary/10 text-primary" : "bg-background"
                  }`}
                >
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0">
                      <div className="font-medium">
                        #{detalle.id} — {detalle.titulo}
                      </div>
                      <div className="mt-1 text-xs text-muted-foreground">
                        {detalle.parte || "Parte sin registrar"}
                        {detalle.documentoParte ? ` · ${detalle.documentoParte}` : ""}
                      </div>
                    </div>

                    {detalle.estado && (
                      <span className="w-fit rounded-full border bg-muted/50 px-2 py-0.5 text-xs text-muted-foreground">
                        {detalle.estado}
                      </span>
                    )}
                  </div>

                  <div className="mt-2 flex flex-wrap gap-2 text-xs text-muted-foreground">
                    {detalle.area && <span>Área: {detalle.area}</span>}
                    {detalle.tema && <span>Tema: {detalle.tema}</span>}
                    {detalle.tipo && <span>Tipo: {detalle.tipo}</span>}
                    {detalle.responsable && <span>Responsable: {detalle.responsable}</span>}
                  </div>
                </button>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}

export function CampoConsulta({ label, consultaId, consultas, onSeleccionar, required = false }) {
  const [modalAbierto, setModalAbierto] = useState(false);
  const [busqueda, setBusqueda] = useState("");

  const consultaSeleccionada = useMemo(
    () => consultas.find((consulta) => String(idConsulta(consulta)) === String(consultaId)) || null,
    [consultas, consultaId]
  );

  const detalle = consultaSeleccionada ? obtenerDetalleConsulta(consultaSeleccionada) : null;

  function cerrarModal() {
    setModalAbierto(false);
    setBusqueda("");
  }

  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium">
        {label}{required ? " *" : ""}
      </label>

      <button
        type="button"
        onClick={() => setModalAbierto(true)}
        className={`min-h-10 w-full rounded-md border bg-background px-3 py-2 text-left text-sm transition-colors hover:bg-muted/50 ${
          !detalle ? "text-muted-foreground" : ""
        }`}
      >
        {detalle ? (
          <span className="block">
            <span className="block truncate font-medium text-foreground">
              #{detalle.id} — {detalle.titulo}
            </span>
            <span className="block truncate text-xs text-muted-foreground">
              {[detalle.parte, detalle.documentoParte].filter(Boolean).join(" · ") || "Consulta seleccionada"}
            </span>
          </span>
        ) : (
          <span className="flex items-center justify-between gap-2">
            Buscar y seleccionar consulta...
            <Search className="h-4 w-4" />
          </span>
        )}
      </button>

      <ModalBuscarConsulta
        abierto={modalAbierto}
        consultas={consultas}
        busqueda={busqueda}
        setBusqueda={setBusqueda}
        consultaIdSeleccionada={consultaId}
        onSeleccionar={(consulta) => {
          onSeleccionar(String(idConsulta(consulta)));
          cerrarModal();
        }}
        onCerrar={cerrarModal}
      />
    </div>
  );
}

export function InfoCard({ title, value, icon }) {
  return (
    <div className="rounded-xl border bg-background p-4">
      <div className="mb-2 flex items-center gap-2 text-sm font-medium text-muted-foreground">
        {icon}
        {title}
      </div>
      <p className="font-semibold">{value}</p>
    </div>
  );
}

export function PersonasCard({ title, items }) {
  return (
    <div className="rounded-xl border bg-background p-4">
      <h4 className="mb-3 font-semibold">{title}</h4>
      {items.length === 0 ? (
        <p className="text-sm text-muted-foreground">No registra.</p>
      ) : (
        <ul className="space-y-2 text-sm">
          {items.map((item, index) => (
            <li key={item?.id || index} className="rounded-lg bg-muted/30 px-3 py-2">
              {personaResumen(item)}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function ActionCard({ title, description, children }) {
  return (
    <div className="rounded-xl border bg-background p-4">
      <h4 className="font-semibold">{title}</h4>
      <p className="mb-3 mt-1 text-sm text-muted-foreground">{description}</p>
      {children}
    </div>
  );
}
