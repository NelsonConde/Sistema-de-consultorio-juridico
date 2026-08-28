"use client";

import React, { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { ESTADOS_PROCESO } from "./procesos.constants";
import { estadoProcesoEsFinal, labelCatalogo, labelConsulta, labelEstadoProceso } from "./procesos.utils";

export function ModalBuscarConsulta({ abierto, consultas, busqueda, setBusqueda, onSeleccionar, onCerrar, consultaIdSeleccionada }) {
  if (!abierto) return null;

  const consultasFiltradas = busqueda.trim()
    ? consultas.filter((c) => labelConsulta(c).toLowerCase().includes(busqueda.trim().toLowerCase()))
    : consultas;

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50">
      <div className="bg-background rounded-xl border shadow-lg w-full max-w-lg mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">Seleccionar Consulta</h3>
          <button
            type="button"
            onClick={onCerrar}
            className="text-muted-foreground hover:text-foreground text-xl"
          >
            ✕
          </button>
        </div>

        <input
          autoFocus
          type="text"
          placeholder="Buscar por #id, descripción, persona o cédula..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          className="w-full rounded-lg border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />

        <div className="max-h-72 overflow-y-auto space-y-1">
          {consultasFiltradas.length === 0 ? (
            <p className="text-center text-sm text-muted-foreground py-4">Sin resultados</p>
          ) : (
            consultasFiltradas.map((consulta) => {
              const id = consulta.id || consulta.consultaId;
              const marcado = String(consultaIdSeleccionada) === String(id);
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => onSeleccionar(consulta)}
                  className={`w-full text-left px-3 py-2 rounded-lg text-sm hover:bg-muted transition-colors ${marcado ? "bg-primary/10 text-primary font-medium" : ""
                    }`}
                >
                  <div className="font-medium">
                    #{id} — {consulta.consulta || consulta.descripcion || consulta.hechos || "Sin descripción"}
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {[consulta.nombre, consulta.apellido].filter(Boolean).join(" ")}
                    {consulta.cedula ? ` · ${consulta.cedula}` : ""}
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

// ─── Campo de consulta con botón + modal ─────────────────────────────────────
export function CampoConsulta({ label, consultaId, consultas, onSeleccionar, required, disabled = false }) {
  const [modalAbierto, setModalAbierto] = useState(false);
  const [busqueda, setBusqueda] = useState("");

  const consultaSeleccionada = useMemo(
    () => consultas.find((c) => String(c.id || c.consultaId) === String(consultaId)) || null,
    [consultas, consultaId]
  );

  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium">
        {label}{required && " *"}
      </label>

      <button
        type="button"
        onClick={() => {
          if (disabled) return;
          setBusqueda("");
          setModalAbierto(true);
        }}
        disabled={disabled}
        className={`flex h-9 w-full items-center justify-between rounded-lg border bg-background px-3 text-sm text-left hover:bg-muted/50 transition-colors disabled:cursor-not-allowed disabled:opacity-70 ${!consultaSeleccionada ? "text-muted-foreground" : ""
          }`}
      >
        <span className="truncate">
          {consultaSeleccionada
            ? `#${consultaSeleccionada.id || consultaSeleccionada.consultaId} — ${consultaSeleccionada.consulta ||
            consultaSeleccionada.descripcion ||
            consultaSeleccionada.hechos ||
            "Sin descripción"
            }`
            : "Buscar consulta..."}
        </span>
        <span className="text-muted-foreground ml-2 flex-shrink-0">▼</span>
      </button>

      <ModalBuscarConsulta
        abierto={modalAbierto}
        consultas={consultas}
        busqueda={busqueda}
        setBusqueda={setBusqueda}
        consultaIdSeleccionada={consultaId}
        onSeleccionar={(c) => {
          onSeleccionar(String(c.id || c.consultaId));
          setModalAbierto(false);
          setBusqueda("");
        }}
        onCerrar={() => { setModalAbierto(false); setBusqueda(""); }}
      />
    </div>
  );
}

// ─── Componentes auxiliares ───────────────────────────────────────────────────
export function CampoTexto({ label, value, onChange, placeholder, maxLength }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium">{label}</label>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        maxLength={maxLength}
        className="h-9 rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
      />
    </div>
  );
}

export function CampoSelect({ label, value, onChange, children, required, disabled }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium">{label}</label>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        disabled={disabled}
        className="h-9 rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring disabled:cursor-not-allowed disabled:opacity-60"
      >
        {children}
      </select>
    </div>
  );
}

export function Aviso({ children }) {
  return (
    <div className="rounded-lg border bg-muted/30 p-3 text-sm text-muted-foreground">
      {children}
    </div>
  );
}

// ─── Modal de edición ─────────────────────────────────────────────────────────
export function ModalEdicion({
  form, actualizarCampo, departamentos, consultas,
  organosControl, especialidadesFiltradas, onCerrar, onGuardar, guardando,
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <form
        onSubmit={onGuardar}
        className="w-full max-w-3xl rounded-xl border bg-background p-6 shadow-lg space-y-5"
      >
        <div className="flex items-center justify-between gap-3">
          <div>
            <h3 className="text-lg font-semibold">Editar proceso #{form.id}</h3>
            <p className="text-sm text-muted-foreground">
              Actualiza la información del proceso seleccionado.
            </p>
          </div>
          <button
            type="button"
            onClick={onCerrar}
            className="text-xl text-muted-foreground hover:text-foreground"
          >
            ×
          </button>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="space-y-1">
            <CampoTexto
              label={estadoProcesoEsFinal(form.estado) ? "Número de radicado *" : "Número de radicado"}
              value={form.numeroRadicado}
              onChange={(v) => actualizarCampo("numeroRadicado", v)}
              placeholder={
                estadoProcesoEsFinal(form.estado)
                  ? "Obligatorio para procesos finalizados"
                  : "Opcional mientras el proceso esté pendiente"
              }
              maxLength={23}
            />
            <p className="text-xs text-muted-foreground">
              {estadoProcesoEsFinal(form.estado)
                ? "Este proceso ya tiene resultado final, por eso debe conservar un radicado válido."
                : "Puede quedar vacío mientras el proceso esté pendiente. Será obligatorio antes de registrar un resultado final."}
            </p>
          </div>

          <CampoSelect
            label="Departamento"
            value={form.departamentoId}
            onChange={(v) => actualizarCampo("departamentoId", v)}
            required
          >
            <option value="">Seleccione un departamento</option>
            {departamentos.map((d) => (
              <option key={d.id} value={d.id}>{labelCatalogo(d)}</option>
            ))}
          </CampoSelect>

          {/* Consulta — modal de búsqueda en lugar de select nativo */}
          <div className="space-y-1">
            <CampoConsulta
              label="Consulta"
              consultaId={form.consultaId}
              consultas={consultas}
              onSeleccionar={(v) => actualizarCampo("consultaId", v)}
              required
              disabled
            />
            <p className="text-xs text-muted-foreground">
              La consulta asociada no se cambia desde la edición del proceso.
            </p>
          </div>

          <CampoSelect
            label="Órgano de control"
            value={form.organoControlId}
            onChange={(v) => actualizarCampo("organoControlId", v)}
          >
            <option value="">Sin órgano de control</option>
            {organosControl.map((o) => (
              <option key={o.id} value={o.id}>{labelCatalogo(o)}</option>
            ))}
          </CampoSelect>

          <CampoSelect
            label="Especialidad"
            value={form.especialidadId}
            onChange={(v) => actualizarCampo("especialidadId", v)}
            disabled={!form.organoControlId || especialidadesFiltradas.length === 0}
          >
            <option value="">Sin especialidad</option>
            {especialidadesFiltradas.map((e) => (
              <option key={e.id} value={e.id}>{labelCatalogo(e)}</option>
            ))}
          </CampoSelect>

        </div>

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Button>
          <Button type="submit" disabled={guardando}>
            {guardando ? "Guardando..." : "Guardar cambios"}
          </Button>
        </div>
      </form>
    </div>
  );
}


// ─── Modal de cambio de estado funcional ─────────────────────────────────────
export function ModalCambioEstado({ proceso, estadoSeleccionado, setEstadoSeleccionado, onCerrar, onGuardar, guardando }) {
  if (!proceso) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <form
        onSubmit={onGuardar}
        className="w-full max-w-md rounded-xl border bg-background p-6 shadow-lg space-y-5"
      >
        <div className="flex items-center justify-between gap-3">
          <div>
            <h3 className="text-lg font-semibold">Cambiar estado del proceso #{proceso.id}</h3>
            <p className="text-sm text-muted-foreground">
              Usa este flujo para registrar el resultado funcional del proceso.
            </p>
          </div>
          <button
            type="button"
            onClick={onCerrar}
            className="text-xl text-muted-foreground hover:text-foreground"
          >
            ×
          </button>
        </div>

        <div className="rounded-lg border bg-muted/30 p-3 text-sm text-muted-foreground">
          Estado actual: <strong>{labelEstadoProceso(proceso.estado)}</strong>
        </div>

        <CampoSelect
          label="Nuevo estado"
          value={estadoSeleccionado}
          onChange={setEstadoSeleccionado}
          required
        >
          <option value="">Seleccione un estado</option>
          {ESTADOS_PROCESO.map((estado) => (
            <option key={estado.value} value={estado.value}>{estado.label}</option>
          ))}
        </CampoSelect>
        
        {estadoProcesoEsFinal(estadoSeleccionado) && !String(proceso.numeroRadicado || "").trim() && (
          <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
            Para registrar un resultado final debes editar el proceso, guardar un número de radicado y luego cambiar el estado.
          </div>
        )}

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Button>
          <Button type="submit" disabled={guardando}>
            {guardando ? "Guardando..." : "Cambiar estado"}
          </Button>
        </div>
      </form>
    </div>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────
