"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import { tieneAlgunPermiso } from "@/lib/authz";

const PERMISOS_PROCESOS = {
  VER_PROCESOS: PERMISOS.VER_PROCESOS || "Ver procesos",
  GESTIONAR_PROCESOS: PERMISOS.GESTIONAR_PROCESOS || "Gestionar procesos",
  GESTIONAR_CONSULTAS_LEGACY: "Gestionar consultas",
};

const FORM_INICIAL = {
  numeroRadicado: "",
  departamentoId: "",
  consultaId: "",
  organoControlId: "",
  especialidadId: "",
};

function extraerLista(data) {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];

  const claves = [
    "content", "data", "items", "rows", "departamentos", "organos",
    "organosControl", "especialidades", "consultas", "procesos", "resultado", "payload",
  ];

  for (const clave of claves) {
    const valor = data[clave];
    if (Array.isArray(valor)) return valor;
    if (valor && typeof valor === "object") {
      const interno = extraerLista(valor);
      if (interno.length > 0) return interno;
    }
  }

  return [];
}

async function leerRespuesta(response) {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try { return JSON.parse(text); } catch { return { mensaje: text }; }
}

function mensajeError(payload, defecto) {
  if (!payload) return defecto;
  if (typeof payload === "string") return payload || defecto;
  return payload.mensaje || payload.message || payload.error || defecto;
}

async function apiGet(url) {
  const response = await fetch(url, { credentials: "include" });
  const payload = await leerRespuesta(response);
  if (response.status === 401) { const e = new Error("Sesión vencida. Inicia sesión nuevamente."); e.status = 401; throw e; }
  if (response.status === 403) { const e = new Error("No tienes permisos para consultar esta información."); e.status = 403; throw e; }
  if (!response.ok) throw new Error(mensajeError(payload, "No se pudo consultar la información."));
  return payload;
}

async function apiEnviar(url, options) {
  const response = await fetch(url, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options?.headers || {}) },
    ...options,
  });
  const payload = await leerRespuesta(response);
  if (response.status === 401) { const e = new Error("Sesión vencida. Inicia sesión nuevamente."); e.status = 401; throw e; }
  if (response.status === 403) { const e = new Error("No tienes permisos para realizar esta acción."); e.status = 403; throw e; }
  if (!response.ok) throw new Error(mensajeError(payload, "No se pudo guardar el proceso."));
  return payload;
}

function puedeAccederProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS.ACCEDER_PROCESOS, PERMISOS_PROCESOS.VER_PROCESOS, PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}
function puedeGestionarProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}
function puedeCargarCatalogos(user) {
  return tieneAlgunPermiso(user, [PERMISOS.VER_CATALOGOS, PERMISOS.GESTIONAR_CATALOGOS]);
}
function puedeCargarConsultas(user) {
  return tieneAlgunPermiso(user, [PERMISOS.VER_CONSULTAS, PERMISOS_PROCESOS.GESTIONAR_CONSULTAS_LEGACY]);
}

function ordenarActivosPrimero(lista) {
  return [...lista].sort((a, b) => {
    const activoA = a.activo === false ? 1 : 0;
    const activoB = b.activo === false ? 1 : 0;
    if (activoA !== activoB) return activoA - activoB;
    return String(a.nombre || a.descripcion || "").localeCompare(String(b.nombre || b.descripcion || ""), "es");
  });
}

function labelCatalogo(item) {
  const nombre = item.nombre || item.descripcion || item.codigo || `#${item.id}`;
  return item.activo === false ? `${nombre} (Inactivo)` : nombre;
}

function labelConsulta(consulta) {
  const persona = consulta.persona || consulta.consultante || {};
  const nombrePersona = [
    consulta.nombre || consulta.personaNombre || persona.nombre,
    consulta.apellido || consulta.personaApellido || persona.apellido,
  ].filter(Boolean).join(" ");

  return [
    `#${consulta.id || consulta.consultaId}`,
    consulta.consulta || consulta.descripcion || consulta.hechos || consulta.asunto,
    nombrePersona,
    consulta.cedula || consulta.documento || persona.documento,
  ].filter(Boolean).join(" - ");
}

function normalizarPayload(form) {
  const numeroRadicado = String(form.numeroRadicado || "").trim();
  return {
    numeroRadicado: numeroRadicado || null,
    departamentoId: form.departamentoId ? Number(form.departamentoId) : null,
    consultaId: form.consultaId ? Number(form.consultaId) : null,
    organoControlId: form.organoControlId ? Number(form.organoControlId) : null,
    especialidadId: form.especialidadId ? Number(form.especialidadId) : null,
  };
}

// ─── Modal de búsqueda de consulta ───────────────────────────────────────────
function ModalBuscarConsulta({ abierto, consultas, busqueda, setBusqueda, onSeleccionar, onCerrar, consultaIdSeleccionada }) {
  if (!abierto) return null;

  const consultasFiltradas = busqueda.trim()
    ? consultas.filter((c) => labelConsulta(c).toLowerCase().includes(busqueda.trim().toLowerCase()))
    : consultas;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
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
function CampoConsulta({ label, consultaId, consultas, onSeleccionar, required }) {
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
        onClick={() => { setBusqueda(""); setModalAbierto(true); }}
        className={`flex h-9 w-full items-center justify-between rounded-lg border bg-background px-3 text-sm text-left hover:bg-muted/50 transition-colors ${!consultaSeleccionada ? "text-muted-foreground" : ""
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
function CampoTexto({ label, value, onChange, placeholder, maxLength }) {
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

function CampoSelect({ label, value, onChange, children, required, disabled }) {
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

function Aviso({ children }) {
  return (
    <div className="rounded-lg border bg-muted/30 p-3 text-sm text-muted-foreground">
      {children}
    </div>
  );
}

// ─── Componente principal ─────────────────────────────────────────────────────
export function NuevoProcesoForm() {
  const router = useRouter();

  const [checking, setChecking] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [user, setUser] = useState(null);
  const [form, setForm] = useState(FORM_INICIAL);

  const [departamentos, setDepartamentos] = useState([]);
  const [organosControl, setOrganosControl] = useState([]);
  const [especialidades, setEspecialidades] = useState([]);
  const [consultas, setConsultas] = useState([]);

  const puedeGestionar = puedeGestionarProcesos(user);

  const especialidadesFiltradas = useMemo(() => {
    if (!form.organoControlId) return especialidades;
    return especialidades.filter(
      (e) => Number(e.organoControlId) === Number(form.organoControlId)
    );
  }, [especialidades, form.organoControlId]);

  useEffect(() => { verificarYCargar(); }, []);

  function actualizarCampo(name, value) {
    setForm((prev) => ({
      ...prev,
      [name]: value,
      ...(name === "organoControlId" ? { especialidadId: "" } : {}),
    }));
  }

  async function verificarYCargar() {
    try {
      const usuarioActual = await apiGet(`${API_URL_BASE}/auth/me`);
      setUser(usuarioActual);

      if (!puedeAccederProcesos(usuarioActual)) {
        toast.error("No tienes permiso para acceder a procesos");
        router.push("/inicio");
        return;
      }

      if (!puedeGestionarProcesos(usuarioActual)) {
        toast.error("No tienes permiso para crear procesos");
        router.push("/procesos");
        return;
      }

      const consultasPermitidas = puedeCargarConsultas(usuarioActual);
      const catalogosPermitidos = puedeCargarCatalogos(usuarioActual);

      const [departamentosRes, organosRes, especialidadesRes, consultasRes] =
        await Promise.allSettled([
          catalogosPermitidos ? apiGet(`${API_URL_BASE}/departamentos`) : Promise.resolve([]),
          catalogosPermitidos ? apiGet(`${API_URL_BASE}/organos-control`) : Promise.resolve([]),
          catalogosPermitidos ? apiGet(`${API_URL_BASE}/especialidades`) : Promise.resolve([]),
          consultasPermitidas ? apiGet(`${API_URL_BASE}/consultas`) : Promise.resolve([]),
        ]);

      if (departamentosRes.status === "fulfilled") setDepartamentos(ordenarActivosPrimero(extraerLista(departamentosRes.value)));
      if (organosRes.status === "fulfilled") setOrganosControl(ordenarActivosPrimero(extraerLista(organosRes.value)));
      if (especialidadesRes.status === "fulfilled") setEspecialidades(ordenarActivosPrimero(extraerLista(especialidadesRes.value)));
      if (consultasRes.status === "fulfilled") setConsultas(extraerLista(consultasRes.value));

      const errores = [departamentosRes, organosRes, especialidadesRes, consultasRes]
        .filter((r) => r.status === "rejected").map((r) => r.reason?.message).filter(Boolean);
      if (errores.length > 0) toast.error(errores[0]);
    } catch (error) {
      console.error(error);
      if (error.status === 401) { router.push("/"); return; }
      toast.error(error.message || "No se pudo cargar el formulario");
      router.push("/inicio");
    } finally {
      setChecking(false);
    }
  }

  function validarAntesDeGuardar() {
    const numeroRadicado = String(form.numeroRadicado || "").trim();

    if (numeroRadicado && numeroRadicado.length !== 23) {
      toast.error("El número de radicado debe tener exactamente 23 caracteres");
      return false;
    }

    if (!form.departamentoId) {
      toast.error("Selecciona un departamento");
      return false;
    }

    if (!form.consultaId) {
      toast.error("Selecciona una consulta");
      return false;
    }

    if (form.especialidadId && !form.organoControlId) {
      toast.error("Selecciona primero un órgano de control");
      return false;
    }

    return true;
  }

  async function guardarProceso(event) {
    event.preventDefault();
    if (!puedeGestionar) { toast.error("No tienes permiso para crear procesos"); return; }
    if (!validarAntesDeGuardar()) return;
    try {
      setGuardando(true);
      await apiEnviar(`${API_URL_BASE}/procesos`, {
        method: "POST",
        body: JSON.stringify(normalizarPayload(form)),
      });
      toast.success("Proceso creado correctamente");
      router.push("/procesos");
    } catch (error) {
      console.error(error);
      if (error.status === 401) { router.push("/"); return; }
      toast.error(error.message || "No se pudo crear el proceso");
    } finally {
      setGuardando(false);
    }
  }

  if (checking) return <div className="text-center mt-10">Cargando...</div>;

  return (
    <div className="rounded-xl border bg-card p-6 shadow space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-xl font-bold">Nuevo proceso</h2>
          <p className="text-sm text-muted-foreground">
            Registra un proceso asociado a una consulta jurídica.
          </p>
        </div>
        <Button type="button" variant="outline" onClick={() => router.push("/procesos")}>
          Ver procesos
        </Button>
      </div>

      {!puedeGestionar ? (
        <Aviso>No tienes permiso para crear procesos.</Aviso>
      ) : (
        <form onSubmit={guardarProceso} className="space-y-5">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="space-y-1">
              <CampoTexto
                label="Número de radicado"
                value={form.numeroRadicado}
                onChange={(v) => actualizarCampo("numeroRadicado", v)}
                placeholder="Opcional mientras el proceso esté pendiente"
                maxLength={23}
              />
              <p className="text-xs text-muted-foreground">
                Puede quedar vacío mientras el proceso esté pendiente. Será obligatorio antes de registrar un resultado final.
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
            <CampoConsulta
              label="Consulta"
              consultaId={form.consultaId}
              consultas={consultas}
              onSeleccionar={(v) => actualizarCampo("consultaId", v)}
              required
            />

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
              disabled={especialidadesFiltradas.length === 0}
            >
              <option value="">Sin especialidad</option>
              {especialidadesFiltradas.map((e) => (
                <option key={e.id} value={e.id}>{labelCatalogo(e)}</option>
              ))}
            </CampoSelect>


          </div>

          <div className="flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => setForm(FORM_INICIAL)}
              disabled={guardando}
            >
              Limpiar
            </Button>
            <Button type="submit" disabled={guardando}>
              {guardando ? "Guardando..." : "Crear proceso"}
            </Button>
          </div>
        </form>
      )}
    </div>
  );
}