"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import { tieneAlgunPermiso } from "@/lib/authz";
import { getTotalPages, paginateItems, sortByIdAsc } from "@/lib/list-utils";

const PERMISOS_PROCESOS = {
  VER_PROCESOS: PERMISOS.VER_PROCESOS || "Ver procesos",
  GESTIONAR_PROCESOS: PERMISOS.GESTIONAR_PROCESOS || "Gestionar procesos",
  GESTIONAR_CONSULTAS_LEGACY: "Gestionar consultas",
};

const FORM_INICIAL = {
  id: null,
  numeroRadicado: "",
  departamentoId: "",
  consultaId: "",
  organoControlId: "",
  especialidadId: "",
  estado: "",
};

const ESTADOS_PROCESO = [
  { value: "PENDIENTE", label: "Pendiente" },
  { value: "SENTENCIA_FAVORABLE", label: "Sentencia favorable" },
  { value: "SENTENCIA_DESFAVORABLE", label: "Sentencia desfavorable" },
  { value: "DESISTIMIENTO", label: "Desistimiento" },
  { value: "RECHAZO", label: "Rechazo" },
  { value: "PRESCRIPCION", label: "Prescripción" },
];

/**
 * Convierte un estado de proceso a su etiqueta en español.
 * 
 * @param {string} estado - Código de estado (ej: "PENDIENTE", "SENTENCIA_FAVORABLE")
 * @returns {string} Etiqueta legible del estado
 */
function labelEstadoProceso(estado) {
  return ESTADOS_PROCESO.find((item) => item.value === estado)?.label || estado || "Sin estado";
}

/**
 * Determina si un estado de proceso es final (no puede cambiar).
 * 
 * @param {string} estado - Estado a verificar
 * @returns {boolean} True si el estado es final, false si es PENDIENTE
 */
function estadoProcesoEsFinal(estado) {
  return estado && estado !== "PENDIENTE";
}

/**
 * Extrae un array de una estructura de datos anidada, buscando en claves comunes.
 * Útil para normalizar diferentes formatos de respuesta del backend.
 * 
 * @param {*} data - Datos a procesar (array u objeto)
 * @returns {Array} Array encontrado o array vacío
 */
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

/**
 * Lee la respuesta de una solicitud fetch y la parsea como JSON.
 * Maneja respuestas vacías y errores de parseo.
 * 
 * @async
 * @param {Response} response - Objeto Response de fetch
 * @returns {Promise<*>} Datos parseados o null si está vacío
 */
async function leerRespuesta(response) {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try { return JSON.parse(text); } catch { return { mensaje: text }; }
}

/**
 * Extrae un mensaje de error de un payload de respuesta.
 * Busca en propiedades comunes (mensaje, message, error).
 * 
 * @param {*} payload - Objeto de respuesta del backend
 * @param {string} defecto - Mensaje por defecto si no se encuentra uno
 * @returns {string} Mensaje de error encontrado o mensaje por defecto
 */
function mensajeError(payload, defecto) {
  if (!payload) return defecto;
  if (typeof payload === "string") return payload || defecto;
  return payload.mensaje || payload.message || payload.error || defecto;
}

/**
 * Realiza una solicitud GET al API con manejo de errores y autenticación.
 * 
 * @async
 * @param {string} url - URL del endpoint
 * @returns {Promise<*>} Datos de respuesta
 * @throws {Error} Si falla la autenticación, autorización o la solicitud
 */
async function apiGet(url) {
  const response = await fetch(url, { credentials: "include" });
  const payload = await leerRespuesta(response);
  if (response.status === 401) { const e = new Error("Sesión vencida. Inicia sesión nuevamente."); e.status = 401; throw e; }
  if (response.status === 403) { const e = new Error("No tienes permisos para consultar esta información."); e.status = 403; throw e; }
  if (!response.ok) throw new Error(mensajeError(payload, "No se pudo consultar la información."));
  return payload;
}

/**
 * Realiza una solicitud POST/PUT/DELETE al API con manejo de errores y autenticación.
 * Automáticamente agrega header Content-Type: application/json.
 * 
 * @async
 * @param {string} url - URL del endpoint
 * @param {Object} options - Opciones de fetch (method, body, headers, etc)
 * @returns {Promise<*>} Datos de respuesta
 * @throws {Error} Si falla la autenticación, autorización o la solicitud
 */
async function apiEnviar(url, options) {
  const response = await fetch(url, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(options?.headers || {}) },
    ...options,
  });
  const payload = await leerRespuesta(response);
  if (response.status === 401) { const e = new Error("Sesión vencida. Inicia sesión nuevamente."); e.status = 401; throw e; }
  if (response.status === 403) { const e = new Error("No tienes permisos para realizar esta acción."); e.status = 403; throw e; }
  if (!response.ok) throw new Error(mensajeError(payload, "No se pudo procesar la solicitud."));
  return payload;
}

/**
 * Verifica si el usuario puede acceder a la sección de procesos.
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene alguno de los permisos necesarios
 */
function puedeAccederProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS.ACCEDER_PROCESOS, PERMISOS_PROCESOS.VER_PROCESOS, PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * Verifica si el usuario puede ver (listar) procesos.
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene permiso VER_PROCESOS o GESTIONAR_PROCESOS
 */
function puedeVerProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS_PROCESOS.VER_PROCESOS, PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * Verifica si el usuario puede crear/editar/eliminar procesos.
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene permiso GESTIONAR_PROCESOS
 */
function puedeGestionarProcesos(user) {
  return tieneAlgunPermiso(user, [PERMISOS_PROCESOS.GESTIONAR_PROCESOS]);
}

/**
 * Verifica si el usuario puede cargar catálogos (departamentos, órganos, etc).
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene permiso VER_CATALOGOS o GESTIONAR_CATALOGOS
 */
function puedeCargarCatalogos(user) {
  return tieneAlgunPermiso(user, [PERMISOS.VER_CATALOGOS, PERMISOS.GESTIONAR_CATALOGOS]);
}

/**
 * Verifica si el usuario puede cargar consultas para asociar a procesos.
 * @param {Object} user - Objeto del usuario
 * @returns {boolean} True si tiene permiso VER_CONSULTAS o GESTIONAR_CONSULTAS_LEGACY
 */
function puedeCargarConsultas(user) {
  return tieneAlgunPermiso(user, [PERMISOS.VER_CONSULTAS, PERMISOS_PROCESOS.GESTIONAR_CONSULTAS_LEGACY]);
}

/**
 * Ordena una lista de items, colocando los activos primero y luego los inactivos.
 * Dentro de cada grupo, ordena alfabéticamente por nombre o descripción.
 * 
 * @param {Array<Object>} lista - Lista a ordenar
 * @returns {Array<Object>} Nueva lista ordenada
 */
function ordenarActivosPrimero(lista) {
  return [...lista].sort((a, b) => {
    const activoA = a.activo === false ? 1 : 0;
    const activoB = b.activo === false ? 1 : 0;
    if (activoA !== activoB) return activoA - activoB;
    return String(a.nombre || a.descripcion || "").localeCompare(String(b.nombre || b.descripcion || ""), "es");
  });
}

/**
 * Crea un Map indexado por id numérico desde una lista de items.
 * Facilita búsquedas rápidas por id.
 * 
 * @param {Array<Object>} lista - Lista de items con propiedad 'id'
 * @returns {Map<number, Object>} Mapa id => item
 */
function crearMapa(lista) {
  return new Map(lista.map((item) => [Number(item.id), item]));
}

/**
 * Obtiene el nombre de un item del catálogo usando un Map.
 * 
 * @param {Map<number, Object>} mapa - Mapa de items indexado por id
 * @param {number} id - ID a buscar
 * @param {string} [fallback] - Valor por defecto si no se encuentra
 * @returns {string} Nombre/descripción del item o fallback o "#id" si no existe
 */
function nombreCatalogo(mapa, id, fallback) {
  const item = mapa.get(Number(id));
  return item?.nombre || item?.descripcion || fallback || (id ? `#${id}` : "Sin asignar");
}

/**
 * Genera una etiqueta legible para un item de catálogo.
 * Agrega "(Inactivo)" al nombre si el item no está activo.
 * 
 * @param {Object} item - Item de catálogo con propiedades {nombre, descripcion, codigo, activo, id}
 * @returns {string} Etiqueta formateada
 */
function labelCatalogo(item) {
  const nombre = item.nombre || item.descripcion || item.codigo || `#${item.id}`;
  return item.activo === false ? `${nombre} (Inactivo)` : nombre;
}

/**
 * Genera una etiqueta detallada para una consulta.
 * Incluye ID, descripción, nombre de la persona y documento.
 * 
 * @param {Object} consulta - Objeto de consulta
 * @returns {string} Etiqueta en formato: "#id - descripción - persona - documento"
 */
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

/**
 * Normaliza un formulario de proceso antes de enviarlo al backend.
 * Convierte IDs a números y valores vacíos a null.
 * 
 * @param {Object} form - Objeto de formulario
 * @returns {Object} Objeto normalizado listo para enviar al API
 */
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

/**
 * Convierte un objeto de proceso a formato de formulario.
 * Asegura que todos los campos tengan valor por defecto (strings vacíos).
 * 
 * @param {Object} proceso - Objeto de proceso del backend
 * @returns {Object} Objeto en formato de formulario
 */
function procesoAForm(proceso) {
  return {
    id: proceso.id,
    numeroRadicado: proceso.numeroRadicado || "",
    departamentoId: proceso.departamentoId || "",
    consultaId: proceso.consultaId || "",
    organoControlId: proceso.organoControlId || "",
    especialidadId: proceso.especialidadId || "",
    estado: proceso.estado || "PENDIENTE",
  };
}

// ─── Modal de búsqueda de consulta ───────────────────────────────────────────
function ModalBuscarConsulta({ abierto, consultas, busqueda, setBusqueda, onSeleccionar, onCerrar, consultaIdSeleccionada }) {
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
function CampoConsulta({ label, consultaId, consultas, onSeleccionar, required, disabled = false }) {
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

// ─── Modal de edición ─────────────────────────────────────────────────────────
function ModalEdicion({
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
            disabled={especialidadesFiltradas.length === 0}
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
function ModalCambioEstado({ proceso, estadoSeleccionado, setEstadoSeleccionado, onCerrar, onGuardar, guardando }) {
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
export function ProcesosForm() {
  const router = useRouter();

  const [checking, setChecking] = useState(true);
  const [cargando, setCargando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [user, setUser] = useState(null);

  const [procesos, setProcesos] = useState([]);
  const [departamentos, setDepartamentos] = useState([]);
  const [organosControl, setOrganosControl] = useState([]);
  const [especialidades, setEspecialidades] = useState([]);
  const [consultas, setConsultas] = useState([]);

  const [busqueda, setBusqueda] = useState("");
  const [editando, setEditando] = useState(false);
  const [procesoCambioEstado, setProcesoCambioEstado] = useState(null);
  const [estadoSeleccionado, setEstadoSeleccionado] = useState("");
  const [form, setForm] = useState(FORM_INICIAL);
  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);
  const REGISTROS_POR_PAGINA_OPTIONS = [5, 10, 20, 50];
  const [confirmEliminar, setConfirmEliminar] = useState({ abierto: false, proceso: null, loading: false });

  const puedeVer = puedeVerProcesos(user);
  const puedeGestionar = puedeGestionarProcesos(user);

  const mapaDepartamentos = useMemo(() => crearMapa(departamentos), [departamentos]);
  const mapaOrganos = useMemo(() => crearMapa(organosControl), [organosControl]);
  const mapaEspecialidades = useMemo(() => crearMapa(especialidades), [especialidades]);
  const mapaConsultas = useMemo(() => crearMapa(consultas), [consultas]);

  const especialidadesFiltradas = useMemo(() => {
    if (!form.organoControlId) return especialidades;
    return especialidades.filter((e) => Number(e.organoControlId) === Number(form.organoControlId));
  }, [especialidades, form.organoControlId]);

  const procesosFiltrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase();
    const filtrados = !texto ? procesos : procesos.filter((proceso) => {
      const consulta = mapaConsultas.get(Number(proceso.consultaId));
      const valores = [
        proceso.id, proceso.numeroRadicado,
        labelEstadoProceso(proceso.estado),
        nombreCatalogo(mapaDepartamentos, proceso.departamentoId),
        nombreCatalogo(mapaOrganos, proceso.organoControlId),
        nombreCatalogo(mapaEspecialidades, proceso.especialidadId),
        consulta ? labelConsulta(consulta) : proceso.consultaId,
        proceso.activo === false ? "inactivo" : "activo",
      ];
      return valores.some((v) => String(v || "").toLowerCase().includes(texto));
    });

    return sortByIdAsc(filtrados);
  }, [busqueda, procesos, mapaConsultas, mapaDepartamentos, mapaOrganos, mapaEspecialidades]);

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
      await cargarDatos(usuarioActual);
    } catch (error) {
      console.error(error);
      if (error.status === 401) { router.push("/"); return; }
      toast.error(error.message || "No se pudo cargar procesos");
      router.push("/inicio");
    } finally {
      setChecking(false);
    }
  }

  async function cargarDatos(usuarioActual = user) {
    try {
      setCargando(true);
      const verProcesos = puedeVerProcesos(usuarioActual);
      const catalogosPermitidos = puedeCargarCatalogos(usuarioActual);
      const consultasPermitidas = puedeCargarConsultas(usuarioActual);

      if (!verProcesos) { toast.error("No tienes permiso para ver procesos"); return; }

      const [procesosRes, departamentosRes, organosRes, especialidadesRes, consultasRes] =
        await Promise.allSettled([
          apiGet(`${API_URL_BASE}/procesos`),
          catalogosPermitidos ? apiGet(`${API_URL_BASE}/departamentos`) : Promise.resolve([]),
          catalogosPermitidos ? apiGet(`${API_URL_BASE}/organos-control`) : Promise.resolve([]),
          catalogosPermitidos ? apiGet(`${API_URL_BASE}/especialidades`) : Promise.resolve([]),
          consultasPermitidas ? apiGet(`${API_URL_BASE}/consultas`) : Promise.resolve([]),
        ]);

      if (procesosRes.status === "fulfilled") setProcesos(sortByIdAsc(extraerLista(procesosRes.value)));
      else throw procesosRes.reason;

      if (departamentosRes.status === "fulfilled") setDepartamentos(ordenarActivosPrimero(extraerLista(departamentosRes.value)));
      if (organosRes.status === "fulfilled") setOrganosControl(ordenarActivosPrimero(extraerLista(organosRes.value)));
      if (especialidadesRes.status === "fulfilled") setEspecialidades(ordenarActivosPrimero(extraerLista(especialidadesRes.value)));
      if (consultasRes.status === "fulfilled") setConsultas(sortByIdAsc(extraerLista(consultasRes.value)));

      const erroresAuxiliares = [departamentosRes, organosRes, especialidadesRes, consultasRes]
        .filter((r) => r.status === "rejected").map((r) => r.reason?.message).filter(Boolean);
      if (erroresAuxiliares.length > 0) toast.error(erroresAuxiliares[0]);
    } finally {
      setCargando(false);
    }
  }

  function validarAntesDeGuardar() {
    const numeroRadicado = String(form.numeroRadicado || "").trim();

    if (estadoProcesoEsFinal(form.estado) && !numeroRadicado) {
      toast.error("Un proceso finalizado debe conservar número de radicado.");
      return false;
    }

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

  function abrirEdicion(proceso) {
    if (!puedeGestionar) { toast.error("No tienes permiso para editar procesos"); return; }
    setForm(procesoAForm(proceso));
    setEditando(true);
  }

  function cerrarEdicion() {
    setEditando(false);
    setForm(FORM_INICIAL);
  }

  function abrirCambioEstado(proceso) {
    if (!puedeGestionar) { toast.error("No tienes permiso para cambiar el estado del proceso"); return; }
    setProcesoCambioEstado(proceso);
    setEstadoSeleccionado(proceso.estado || "");
  }

  function cerrarCambioEstado() {
    setProcesoCambioEstado(null);
    setEstadoSeleccionado("");
  }

  async function cambiarEstadoProceso(event) {
    event?.preventDefault?.();

    if (!puedeGestionar) {
      toast.error("No tienes permiso para cambiar el estado del proceso");
      return;
    }

    if (!procesoCambioEstado?.id) {
      toast.error("Selecciona un proceso");
      return;
    }

    if (!estadoSeleccionado) {
      toast.error("Selecciona el nuevo estado");
      return;
    }

    if (estadoSeleccionado === procesoCambioEstado.estado) {
      toast.error("El proceso ya tiene ese estado");
      return;
    }

    if (estadoProcesoEsFinal(estadoSeleccionado)) {
      const numeroRadicado = String(procesoCambioEstado.numeroRadicado || "").trim();

      if (!numeroRadicado) {
        toast.error("Antes de finalizar el proceso debes registrar y guardar un número de radicado.");
        return;
      }

      if (numeroRadicado.length !== 23) {
        toast.error("El número de radicado debe tener exactamente 23 caracteres");
        return;
      }
    }

    try {
      setGuardando(true);
      await apiEnviar(`${API_URL_BASE}/procesos/${procesoCambioEstado.id}/estado?estado=${encodeURIComponent(estadoSeleccionado)}`, {
        method: "PATCH",
      });
      toast.success("Estado del proceso actualizado correctamente");
      cerrarCambioEstado();
      await cargarDatos();
    } catch (error) {
      console.error(error);
      if (error.status === 401) {
        router.push("/");
        return;
      }
      toast.error(error.message || "No se pudo cambiar el estado del proceso");
    } finally {
      setGuardando(false);
    }
  }

    async function guardarEdicion(event) {
      event.preventDefault();
      if (!puedeGestionar) { toast.error("No tienes permiso para editar procesos"); return; }
      if (!validarAntesDeGuardar()) return;
      try {
        setGuardando(true);
        await apiEnviar(`${API_URL_BASE}/procesos/${form.id}`, {
          method: "PUT",
          body: JSON.stringify(normalizarPayload(form)),
        });
        toast.success("Proceso actualizado correctamente");
        cerrarEdicion();
        await cargarDatos();
      } catch (error) {
        console.error(error);
        if (error.status === 401) { router.push("/"); return; }
        toast.error(error.message || "No se pudo actualizar el proceso");
      } finally {
        setGuardando(false);
      }
    }

    async function eliminarProceso(proceso) {
      if (!puedeGestionar) { toast.error("No tienes permiso para eliminar procesos"); return; }
      setConfirmEliminar({ abierto: true, proceso, loading: false });
    }

    async function ejecutarEliminarProceso() {
      const { proceso } = confirmEliminar;
      if (!proceso) return;

      setConfirmEliminar((s) => ({ ...s, loading: true }));

      try {
        await apiEnviar(`${API_URL_BASE}/procesos/${proceso.id}`, { method: "DELETE" });
        toast.success("Proceso eliminado correctamente");
        await cargarDatos();
      } catch (error) {
        console.error(error);
        if (error.status === 401) { router.push("/"); return; }
        toast.error(error.message || "No se pudo eliminar el proceso");
      } finally {
        setConfirmEliminar({ abierto: false, proceso: null, loading: false });
      }
    }

    const totalPaginas = getTotalPages(procesosFiltrados.length, registrosPorPagina);
    const procesosPaginados = useMemo(
      () => paginateItems(procesosFiltrados, paginaActual, registrosPorPagina),
      [procesosFiltrados, paginaActual, registrosPorPagina]
    );

    useEffect(() => {
      setPaginaActual(1);
    }, [busqueda, registrosPorPagina]);

    useEffect(() => {
      if (paginaActual > totalPaginas) {
        setPaginaActual(totalPaginas);
      }
    }, [paginaActual, totalPaginas]);

    if (checking) return <div className="text-center mt-10">Cargando...</div>;

    return (
      <div className="rounded-xl border bg-card p-6 shadow space-y-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-xl font-bold">Procesos</h2>
            <p className="text-sm text-muted-foreground">Consulta y gestiona los procesos registrados.</p>
          </div>
          {puedeGestionar && (
            <Button type="button" onClick={() => router.push("/nuevoproceso")}>
              Nuevo proceso
            </Button>
          )}
        </div>

        {!puedeVer ? (
          <Aviso>No tienes permiso para ver procesos.</Aviso>
        ) : (
          <>
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <input
                value={busqueda}
                onChange={(e) => setBusqueda(e.target.value)}
                placeholder="Buscar por radicado, departamento, órgano o consulta..."
                className="h-9 w-full rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring sm:max-w-md"
              />
              <Button type="button" variant="outline" onClick={() => cargarDatos()} disabled={cargando}>
                {cargando ? "Actualizando..." : "Actualizar"}
              </Button>
            </div>

            <div className="overflow-x-auto rounded-lg border">
              <table className="w-full text-sm">
                <thead className="bg-muted/50 text-left">
                  <tr>
                    <th className="px-3 py-2 font-medium">ID</th>
                    <th className="px-3 py-2 font-medium">Radicado</th>
                    <th className="px-3 py-2 font-medium">Departamento</th>
                    <th className="px-3 py-2 font-medium">Consulta</th>
                    <th className="px-3 py-2 font-medium">Órgano</th>
                    <th className="px-3 py-2 font-medium">Especialidad</th>
                    <th className="px-3 py-2 font-medium">Estado</th>
                    {puedeGestionar && <th className="px-3 py-2 font-medium">Acciones</th>}
                  </tr>
                </thead>
                <tbody>
                  {procesosFiltrados.length === 0 ? (
                    <tr>
                      <td colSpan={puedeGestionar ? 8 : 7} className="px-3 py-8 text-center text-muted-foreground">
                        No hay procesos para mostrar.
                      </td>
                    </tr>
                  ) : (
                    procesosPaginados.map((proceso) => {
                      const consulta = mapaConsultas.get(Number(proceso.consultaId));
                      return (
                        <tr key={proceso.id} className="border-t align-top">
                          <td className="px-3 py-2">#{proceso.id}</td>
                          <td className="px-3 py-2">{proceso.numeroRadicado || "Sin radicado"}</td>
                          <td className="px-3 py-2">{nombreCatalogo(mapaDepartamentos, proceso.departamentoId)}</td>
                          <td className="px-3 py-2 max-w-xs">
                            {consulta ? labelConsulta(consulta) : `Consulta #${proceso.consultaId}`}
                          </td>
                          <td className="px-3 py-2">
                            {proceso.organoControlId ? nombreCatalogo(mapaOrganos, proceso.organoControlId) : "Sin órgano"}
                          </td>
                          <td className="px-3 py-2">
                            {proceso.especialidadId ? nombreCatalogo(mapaEspecialidades, proceso.especialidadId) : "Sin especialidad"}
                          </td>
                          <td className="px-3 py-2">
                            <span className={`rounded-full border px-2 py-0.5 text-xs ${estadoProcesoEsFinal(proceso.estado) ? "bg-green-50 text-green-700 border-green-200" : ""}`}>
                              {labelEstadoProceso(proceso.estado)}
                            </span>
                          </td>
                          {puedeGestionar && (
                            <td className="px-3 py-2">
                              <div className="flex flex-wrap gap-2">
                                <Button type="button" size="sm" variant="outline" onClick={() => abrirEdicion(proceso)}>
                                  Editar
                                </Button>
                                <Button type="button" size="sm" variant="outline" onClick={() => abrirCambioEstado(proceso)}>
                                  Cambiar estado
                                </Button>
                                <Button type="button" size="sm" variant="destructive" onClick={() => eliminarProceso(proceso)}>
                                  Eliminar
                                </Button>
                              </div>
                            </td>
                          )}
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            <Pagination
              currentPage={paginaActual}
              totalPages={totalPaginas}
              onPageChange={(p) => setPaginaActual(p)}
              pageSize={registrosPorPagina}
              onPageSizeChange={(v) => { setRegistrosPorPagina(v); setPaginaActual(1); }}
              pageSizeOptions={REGISTROS_POR_PAGINA_OPTIONS}
              totalItems={procesosFiltrados.length}
            />
          </>
        )}

        {editando && (
          <ModalEdicion
            form={form}
            actualizarCampo={actualizarCampo}
            departamentos={departamentos}
            consultas={consultas}
            organosControl={organosControl}
            especialidadesFiltradas={especialidadesFiltradas}
            onCerrar={cerrarEdicion}
            onGuardar={guardarEdicion}
            guardando={guardando}
          />
        )}

        {procesoCambioEstado && (
          <ModalCambioEstado
            proceso={procesoCambioEstado}
            estadoSeleccionado={estadoSeleccionado}
            setEstadoSeleccionado={setEstadoSeleccionado}
            onCerrar={cerrarCambioEstado}
            onGuardar={cambiarEstadoProceso}
            guardando={guardando}
          />
        )}

        <ConfirmActionDialog
          open={confirmEliminar.abierto}
          title="Eliminar proceso"
          description={confirmEliminar.proceso ? `¿Seguro que deseas eliminar el proceso #${confirmEliminar.proceso.id}?` : "¿Eliminar este proceso?"}
          confirmText="Eliminar"
          cancelText="Cancelar"
          loading={confirmEliminar.loading}
          variant="destructive"
          onConfirm={ejecutarEliminarProceso}
          onClose={() => setConfirmEliminar({ abierto: false, proceso: null, loading: false })}
        />
      </div>
    );
  }