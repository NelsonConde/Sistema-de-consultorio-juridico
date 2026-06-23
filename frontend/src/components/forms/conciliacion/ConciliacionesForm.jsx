"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  Download,
  Eye,
  FileCheck2,
  FileText,
  RefreshCw,
  Search,
  ShieldAlert,
  Trash2,
  Upload,
  UserCheck,
  Users,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import Pagination from "@/components/ui/Pagination";
import { API_URL_BASE, FILE_STORAGE_API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import {
  esAdministrativo,
  esConciliador,
  esEstudiante,
  tieneAlgunPermiso,
  tienePermiso,
  tieneRol,
} from "@/lib/authz";

const ESTADOS_NO_FINALES = [
  { value: "ESPERANDO_REUNION", label: "Esperando reunión" },
];

const ESTADOS_FINALES = [
  { value: "COMPLETO_CONCILIADO", label: "Completo - conciliado" },
  { value: "COMPLETO_NO_CONCILIADO", label: "Completo - no conciliado" },
];

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

function normalizarTexto(value) {
  return String(value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}

function extraerLista(data) {
  if (Array.isArray(data)) return data;
  if (!data || typeof data !== "object") return [];

  const claves = [
    "content",
    "data",
    "items",
    "rows",
    "consultas",
    "conciliaciones",
    "estudiantes",
    "conciliadores",
    "resultado",
    "payload",
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

  try {
    return JSON.parse(text);
  } catch {
    return { mensaje: text };
  }
}

function obtenerMensajeError(data, fallback = "Ocurrió un error") {
  if (!data) return fallback;
  if (typeof data === "string") return data || fallback;

  if (data.detalles && typeof data.detalles === "object") {
    const detalle = Object.values(data.detalles).filter(Boolean).join(". ");
    if (detalle) return detalle;
  }

  if (Array.isArray(data.detalles)) {
    const detalle = data.detalles.filter(Boolean).join(". ");
    if (detalle) return detalle;
  }

  return data.mensaje || data.message || data.error || fallback;
}

function encodePath(path) {
  return String(path || "")
    .split("/")
    .map((segment) => encodeURIComponent(segment))
    .join("/");
}

function archivoEsPdf(file) {
  if (!file) return false;
  const nombre = String(file.name || "").toLowerCase();
  return file.type === "application/pdf" || nombre.endsWith(".pdf");
}

function valor(...opciones) {
  return opciones.find((item) => item !== undefined && item !== null && item !== "") ?? "";
}

function nombrePersona(item) {
  const directo = valor(
    item?.nombre,
    item?.nombreCompleto,
    item?.nombre_completo,
    item?.personaNombre,
    item?.displayName
  );

  if (directo) return directo;

  const nombres = valor(item?.nombres, item?.primerNombre);
  const apellidos = valor(item?.apellidos, item?.apellido);
  const compuesto = `${nombres} ${apellidos}`.trim();

  return compuesto || "Sin nombre";
}

function nombreConsulta(item) {
  const id = valor(item?.id, item?.consultaId, item?.idConsulta);
  return valor(
    item?.descripcion,
    item?.descripcionConsulta,
    item?.consulta,
    item?.hechos,
    item?.titulo,
    id ? `Consulta #${id}` : "Consulta"
  );
}

function idConsulta(item) {
  return valor(item?.id, item?.consultaId, item?.idConsulta);
}

function ordenarPorIdAsc(items) {
  return [...items].sort((a, b) => {
    const idA = Number(a?.id ?? Number.MAX_SAFE_INTEGER);
    const idB = Number(b?.id ?? Number.MAX_SAFE_INTEGER);
    return idA - idB;
  });
}

function formatearFecha(value) {
  if (!value) return "No registra";
  const fecha = new Date(value);
  if (Number.isNaN(fecha.getTime())) return String(value);

  return new Intl.DateTimeFormat("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(fecha);
}

function etiquetaEstado(codigo, nombre) {
  if (nombre) return nombre;

  const estado = normalizarTexto(codigo).replace(/_/g, " ");
  if (!estado) return "Sin estado";

  return estado.charAt(0) + estado.slice(1).toLowerCase();
}

function badgeEstadoClass(codigo) {
  const estado = normalizarTexto(codigo);

  if (estado.includes("COMPLETO")) {
    return "border-emerald-200 bg-emerald-50 text-emerald-700";
  }

  if (estado === "EN_ESPERA") {
    return "border-amber-200 bg-amber-50 text-amber-700";
  }

  if (estado === "REUNION_PROGRAMADA") {
    return "border-blue-200 bg-blue-50 text-blue-700";
  }

  return "border-slate-200 bg-slate-50 text-slate-700";
}

function personaResumen(persona) {
  if (!persona) return "No registra";
  const nombre = nombrePersona(persona);
  const documento = valor(persona?.numeroDocumento, persona?.documento, persona?.cedula);
  return documento ? `${nombre} - ${documento}` : nombre;
}

function esRolAdministrador(usuario) {
  return (
    esAdministrativo(usuario) ||
    tieneRol(usuario, "Administrador") ||
    tieneRol(usuario, "Administrativo") ||
    tieneRol(usuario, "Director")
  );
}


function obtenerDetalleConsulta(item) {
  const persona = item?.persona || item?.consultante || item?.partePrincipal || {};
  const nombreParte = valor(
    item?.personaNombre,
    item?.consultanteNombre,
    item?.nombrePersona,
    item?.nombre,
    persona?.nombre,
    persona?.nombreCompleto,
    persona?.nombre_completo
  );
  const apellidoParte = valor(item?.personaApellido, item?.apellido, persona?.apellido, persona?.apellidos);
  const documentoParte = valor(
    item?.cedula,
    item?.documento,
    item?.numeroDocumento,
    persona?.documento,
    persona?.numeroDocumento
  );
  const responsable = valor(item?.estudianteNombre, item?.asesorNombre, item?.monitorNombre);

  return {
    id: idConsulta(item),
    titulo: nombreConsulta(item),
    parte: [nombreParte, apellidoParte].filter(Boolean).join(" "),
    documentoParte,
    estado: valor(item?.estado, item?.estadoNombre, item?.estadoConsulta),
    area: valor(item?.areaNombre, item?.area, item?.nombreArea),
    tema: valor(item?.temaNombre, item?.tema, item?.nombreTema),
    tipo: valor(item?.tipoNombre, item?.tipo, item?.nombreTipo),
    responsable,
  };
}

function labelConsultaBusqueda(item) {
  const detalle = obtenerDetalleConsulta(item);
  return [
    detalle.id,
    detalle.titulo,
    detalle.parte,
    detalle.documentoParte,
    detalle.estado,
    detalle.area,
    detalle.tema,
    detalle.tipo,
    detalle.responsable,
  ]
    .map(normalizarTexto)
    .join(" ");
}

function ModalBuscarConsulta({ abierto, consultas, busqueda, setBusqueda, onSeleccionar, onCerrar, consultaIdSeleccionada }) {
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

function CampoConsulta({ label, consultaId, consultas, onSeleccionar, required = false }) {
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

export function ConciliacionesForm() {
  const router = useRouter();

  const [usuario, setUsuario] = useState(null);
  const [conciliaciones, setConciliaciones] = useState([]);
  const [consultas, setConsultas] = useState([]);
  const [estudiantes, setEstudiantes] = useState([]);
  const [conciliadores, setConciliadores] = useState([]);
  const [detalle, setDetalle] = useState(null);

  const [loading, setLoading] = useState(true);
  const [loadingDetalle, setLoadingDetalle] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [crearConsultaId, setCrearConsultaId] = useState("");
  const [archivoSolicitud, setArchivoSolicitud] = useState(null);
  const [estadoNoFinal, setEstadoNoFinal] = useState("ESPERANDO_REUNION");
  const [estadoFinal, setEstadoFinal] = useState("COMPLETO_CONCILIADO");
  const [archivoActa, setArchivoActa] = useState(null);
  const [archivoSolicitudReemplazo, setArchivoSolicitudReemplazo] = useState(null);
  const [estudianteId, setEstudianteId] = useState("");
  const [conciliadorId, setConciliadorId] = useState("");

  const puedeVer = usuario && tienePermiso(usuario, PERMISOS.VER_CONCILIACIONES);
  const puedeGestionar = usuario && tienePermiso(usuario, PERMISOS.GESTIONAR_CONCILIACIONES);
  const puedeConcluir = usuario && tienePermiso(usuario, PERMISOS.CONCLUIR_CONCILIACIONES);
  const puedeCrear = Boolean(
    puedeGestionar && !esEstudiante(usuario) && !esConciliador(usuario)
  );
  const puedeOperar = Boolean(
    puedeGestionar || (puedeConcluir && !esEstudiante(usuario))
  );
  const esAdmin = esRolAdministrador(usuario);
  const puedeAsignarEstudiante = Boolean(puedeOperar && !esEstudiante(usuario));
  const puedeAsignarConciliador = Boolean(puedeGestionar && esAdmin);
  const puedeCambiarEstado = Boolean(puedeOperar && !esEstudiante(usuario));
  const puedeFinalizar = Boolean(puedeOperar && !esEstudiante(usuario));
  const puedeReemplazarSolicitud = Boolean(puedeGestionar && esAdmin);
  const puedeDesactivar = Boolean(puedeGestionar && esAdmin);

  const mostrarPanelGestion =
    puedeCrear ||
    puedeAsignarEstudiante ||
    puedeAsignarConciliador ||
    puedeCambiarEstado ||
    puedeFinalizar ||
    puedeReemplazarSolicitud ||
    puedeDesactivar;

  useEffect(() => {
    cargarInicial();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [search, conciliaciones.length, pageSize]);

  async function apiFetch(path, options = {}, fallback = "No se pudo completar la operación") {
    const response = await fetch(`${API_URL_BASE}${path}`, {
      credentials: "include",
      ...options,
      headers: options.body instanceof FormData
        ? options.headers
        : {
            ...(options.headers || {}),
          },
    });

    const data = await leerRespuesta(response);

    if (response.status === 401) {
      router.replace("/");
      throw new Error("Sesión vencida. Inicia sesión nuevamente.");
    }

    if (!response.ok) {
      throw new Error(obtenerMensajeError(data, fallback));
    }

    return data;
  }

  async function cargarInicial() {
    try {
      setLoading(true);
      setError("");
      setMensaje("");

      const meResponse = await fetch(`${API_URL_BASE}/auth/me`, {
        method: "GET",
        credentials: "include",
      });

      if (meResponse.status === 401) {
        router.replace("/");
        return;
      }

      if (!meResponse.ok) {
        router.replace("/inicio");
        return;
      }

      const me = await meResponse.json();
      const puedeEntrar =
        tienePermiso(me, PERMISOS.ACCEDER_CONCILIACIONES) &&
        tienePermiso(me, PERMISOS.VER_CONCILIACIONES);

      if (!puedeEntrar) {
        router.replace("/inicio");
        return;
      }

      setUsuario(me);
      await Promise.all([cargarConciliaciones(), cargarAuxiliares(me)]);
    } catch (err) {
      console.error(err);
      setError(err.message || "Error cargando conciliaciones");
    } finally {
      setLoading(false);
    }
  }

  async function cargarConciliaciones() {
    const data = await apiFetch(
      "/conciliaciones",
      { method: "GET" },
      "No se pudieron cargar las conciliaciones"
    );

    setConciliaciones(ordenarPorIdAsc(extraerLista(data)));
  }

  async function cargarAuxiliares(me) {
    const puedeCargarConsultas = tienePermiso(me, PERMISOS.VER_CONSULTAS);
    const puedeCargarEstudiantes = tieneAlgunPermiso(me, [
      PERMISOS.VER_ESTUDIANTES,
      PERMISOS.VER_PERFILES_AUXILIARES,
      PERMISOS.GESTIONAR_CONCILIACIONES,
      PERMISOS.CONCLUIR_CONCILIACIONES,
    ]);
    const puedeCargarConciliadores = tieneAlgunPermiso(me, [
      PERMISOS.VER_CONCILIADORES,
      PERMISOS.VER_PERFILES_AUXILIARES,
      PERMISOS.GESTIONAR_CONCILIACIONES,
    ]);

    const tareas = [];

    if (puedeCargarConsultas) {
      tareas.push(
        apiFetch("/consultas", { method: "GET" }, "No se pudieron cargar las consultas")
          .then((data) => setConsultas(ordenarPorIdAsc(extraerLista(data))))
          .catch(() => setConsultas([]))
      );
    }

    if (puedeCargarEstudiantes) {
      tareas.push(
        apiFetch(
          "/estudiantes/conciliacion",
          { method: "GET" },
          "No se pudieron cargar los estudiantes habilitados para conciliación"
        )
          .then((data) => setEstudiantes(ordenarPorIdAsc(extraerLista(data))))
          .catch(() => setEstudiantes([]))
      );
    }

    if (puedeCargarConciliadores) {
      tareas.push(
        apiFetch(
          "/conciliadores/activos",
          { method: "GET" },
          "No se pudieron cargar los conciliadores"
        )
          .then((data) => setConciliadores(ordenarPorIdAsc(extraerLista(data))))
          .catch(() => setConciliadores([]))
      );
    }

    await Promise.allSettled(tareas);
  }

  async function refrescar(mensajeOk = "Información actualizada") {
    await cargarConciliaciones();
    if (detalle?.id) {
      await cargarDetalle(detalle.id, { silencioso: true });
    }
    setMensaje(mensajeOk);
    toast.success(mensajeOk);
  }

  async function cargarDetalle(id, opciones = {}) {
    try {
      setLoadingDetalle(true);
      if (!opciones.silencioso) {
        setError("");
        setMensaje("");
      }

      const data = await apiFetch(
        `/conciliaciones/${id}`,
        { method: "GET" },
        "No se pudo obtener el detalle de la conciliación"
      );

      setDetalle(data);
      setEstudianteId(String(data?.estudianteId || ""));
      setConciliadorId(String(data?.conciliadorId || ""));
    } catch (err) {
      console.error(err);
      setError(err.message || "Error cargando detalle");
    } finally {
      setLoadingDetalle(false);
    }
  }

  async function crearConciliacion(event) {
    event.preventDefault();

    if (!crearConsultaId) {
      setError("Selecciona la consulta asociada.");
      return;
    }

    if (!archivoEsPdf(archivoSolicitud)) {
      setError("La solicitud es obligatoria y debe ser un archivo PDF.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setMensaje("");

      const formData = new FormData();
      formData.append("solicitud", archivoSolicitud);

      const creada = await apiFetch(
        `/conciliaciones/consulta/${crearConsultaId}`,
        { method: "POST", body: formData },
        "No se pudo crear la conciliación"
      );

      setCrearConsultaId("");
      setArchivoSolicitud(null);
      const input = document.getElementById("solicitud-conciliacion");
      if (input) input.value = "";
      await refrescar("Conciliación creada correctamente");

      if (creada?.id) {
        await cargarDetalle(creada.id);
      }
    } catch (err) {
      console.error(err);
      setError(err.message || "Error creando conciliación");
    } finally {
      setSaving(false);
    }
  }

  async function asignarEstudiante() {
    if (!detalle?.id) return;
    if (!estudianteId) {
      setError("Selecciona un estudiante habilitado para conciliación.");
      return;
    }

    await ejecutarAccion(async () => {
      await apiFetch(
        `/conciliaciones/${detalle.id}/estudiante?estudianteId=${encodeURIComponent(estudianteId)}`,
        { method: "PATCH" },
        "No se pudo asignar el estudiante"
      );
      await refrescar("Estudiante asignado correctamente");
    });
  }

  async function asignarConciliador() {
    if (!detalle?.id) return;
    if (!conciliadorId) {
      setError("Selecciona un conciliador activo.");
      return;
    }

    await ejecutarAccion(async () => {
      await apiFetch(
        `/conciliaciones/${detalle.id}/conciliador?conciliadorId=${encodeURIComponent(conciliadorId)}`,
        { method: "PATCH" },
        "No se pudo asignar el conciliador"
      );
      await refrescar("Conciliador asignado correctamente");
    });
  }

  async function cambiarEstado() {
    if (!detalle?.id) return;
    if (!estadoNoFinal) {
      setError("Selecciona un estado válido.");
      return;
    }

    await ejecutarAccion(async () => {
      await apiFetch(
        `/conciliaciones/${detalle.id}/estado?estado=${encodeURIComponent(estadoNoFinal)}`,
        { method: "PATCH" },
        "No se pudo cambiar el estado"
      );
      await refrescar("Estado actualizado correctamente");
    });
  }

  async function finalizarConciliacion(event) {
    event.preventDefault();
    if (!detalle?.id) return;

    if (!estadoFinal) {
      setError("Selecciona el estado final.");
      return;
    }

    if (!archivoEsPdf(archivoActa)) {
      setError("El acta es obligatoria y debe ser un archivo PDF.");
      return;
    }

    await ejecutarAccion(async () => {
      const formData = new FormData();
      formData.append("estado", estadoFinal);
      formData.append("acta", archivoActa);

      await apiFetch(
        `/conciliaciones/${detalle.id}/finalizar`,
        { method: "POST", body: formData },
        "No se pudo finalizar la conciliación"
      );

      setArchivoActa(null);
      const input = document.getElementById("acta-conciliacion");
      if (input) input.value = "";
      await refrescar("Conciliación finalizada correctamente");
    });
  }

  async function reemplazarSolicitud(event) {
    event.preventDefault();
    if (!detalle?.id) return;

    if (!archivoEsPdf(archivoSolicitudReemplazo)) {
      setError("La nueva solicitud debe ser un archivo PDF.");
      return;
    }

    await ejecutarAccion(async () => {
      const formData = new FormData();
      formData.append("solicitud", archivoSolicitudReemplazo);

      await apiFetch(
        `/conciliaciones/${detalle.id}/solicitud`,
        { method: "POST", body: formData },
        "No se pudo reemplazar la solicitud"
      );

      setArchivoSolicitudReemplazo(null);
      const input = document.getElementById("solicitud-reemplazo-conciliacion");
      if (input) input.value = "";
      await refrescar("Solicitud reemplazada correctamente");
    });
  }

  async function desactivarConciliacion() {
    if (!detalle?.id) return;

    const confirmar = window.confirm(
      `¿Seguro que deseas desactivar la conciliación #${detalle.id}? Esta acción no equivale a finalizarla.`
    );

    if (!confirmar) return;

    await ejecutarAccion(async () => {
      await apiFetch(
        `/conciliaciones/${detalle.id}`,
        { method: "DELETE" },
        "No se pudo desactivar la conciliación"
      );

      setDetalle(null);
      await refrescar("Conciliación desactivada correctamente");
    });
  }

  async function ejecutarAccion(action) {
    try {
      setSaving(true);
      setError("");
      setMensaje("");
      await action();
    } catch (err) {
      console.error(err);
      setError(err.message || "No se pudo completar la acción");
      toast.error(err.message || "No se pudo completar la acción");
    } finally {
      setSaving(false);
    }
  }

  async function descargarDocumento(path) {
    if (!path) return;

    try {
      const response = await fetch(
        `${FILE_STORAGE_API_URL_BASE}/files/download/${encodePath(path)}`,
        { method: "GET", credentials: "include" }
      );

      if (response.status === 401) {
        router.replace("/");
        return;
      }

      if (!response.ok) {
        throw new Error("No se pudo descargar el documento");
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = String(path).split("/").pop() || "documento.pdf";
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      setError(err.message || "No se pudo descargar el documento");
    }
  }

  function handlePageSizeChange(nextSize) {
    setPageSize(nextSize);
    setCurrentPage(1);
  }

  const conciliacionesFiltradas = useMemo(() => {
    const texto = normalizarTexto(search);
    const ordenadas = ordenarPorIdAsc(conciliaciones);

    if (!texto) return ordenadas;

    return ordenadas.filter((item) => {
      const contenido = [
        item?.id,
        item?.consultaId,
        item?.estudianteNombre,
        item?.conciliadorNombre,
        item?.estadoCodigo,
        item?.estadoNombre,
        item?.solicitadoPorUsername,
      ]
        .map(normalizarTexto)
        .join(" ");

      return contenido.includes(texto);
    });
  }, [conciliaciones, search]);

  const totalPages = Math.max(1, Math.ceil(conciliacionesFiltradas.length / pageSize));

  const conciliacionesPagina = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return conciliacionesFiltradas.slice(start, start + pageSize);
  }, [conciliacionesFiltradas, currentPage, pageSize]);

  if (loading) {
    return <div className="py-10 text-center text-sm text-muted-foreground">Cargando conciliaciones...</div>;
  }

  if (!puedeVer) {
    return (
      <div className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-amber-800">
        <div className="flex items-center gap-2 font-semibold">
          <ShieldAlert className="h-5 w-5" />
          No tienes permisos para ver conciliaciones.
        </div>
        <p className="mt-2 text-sm">
          Solicita el permiso Ver conciliaciones y el acceso a la página de Conciliaciones.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <section className="rounded-2xl border bg-card p-5 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-2xl font-bold tracking-tight">Conciliaciones</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Gestión y consulta de conciliaciones asociadas a consultas jurídicas visibles para tu usuario.
            </p>
          </div>
          <Button type="button" variant="outline" onClick={() => refrescar()} disabled={saving}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Actualizar
          </Button>
        </div>

        {error && (
          <div className="mt-4 rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        {mensaje && (
          <div className="mt-4 rounded-lg border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-primary">
            {mensaje}
          </div>
        )}
      </section>

      {puedeCrear && (
        <section className="rounded-2xl border bg-card p-5 shadow-sm">
          <div className="mb-4 flex items-center gap-2">
            <Upload className="h-5 w-5 text-primary" />
            <div>
              <h3 className="font-semibold">Crear conciliación desde consulta</h3>
              <p className="text-sm text-muted-foreground">
                La solicitud PDF es obligatoria.
              </p>
            </div>
          </div>

          <form onSubmit={crearConciliacion} className="grid grid-cols-1 gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end">
            <CampoConsulta
              label="Consulta"
              consultaId={crearConsultaId}
              consultas={consultas}
              onSeleccionar={setCrearConsultaId}
              required
            />

            <div className="space-y-1.5">
              <label className="text-sm font-medium">Solicitud PDF *</label>
              <input
                id="solicitud-conciliacion"
                type="file"
                accept="application/pdf,.pdf"
                onChange={(event) => setArchivoSolicitud(event.target.files?.[0] || null)}
                className="block h-10 w-full rounded-md border bg-background px-3 py-2 text-sm"
              />
            </div>

            <Button type="submit" disabled={saving}>
              Crear conciliación
            </Button>
          </form>
        </section>
      )}

      <section className="rounded-2xl border bg-card p-5 shadow-sm">
        <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h3 className="font-semibold">Listado operativo</h3>
          </div>

          <div className="relative w-full lg:w-80">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Buscar por id, consulta, estado o responsable"
              className="h-10 w-full rounded-md border bg-background pl-9 pr-3 text-sm"
            />
          </div>
        </div>

        <div className="overflow-x-auto rounded-xl border">
          <table className="min-w-full divide-y text-sm">
            <thead className="bg-muted/50 text-left text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">Consulta</th>
                <th className="px-4 py-3">Estado</th>
                <th className="px-4 py-3">Estudiante</th>
                <th className="px-4 py-3">Conciliador</th>
                <th className="px-4 py-3">Documentos</th>
                <th className="px-4 py-3 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {conciliacionesPagina.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-muted-foreground">
                    No hay conciliaciones para mostrar.
                  </td>
                </tr>
              ) : (
                conciliacionesPagina.map((item) => (
                  <tr key={item.id} className="hover:bg-muted/30">
                    <td className="px-4 py-3 font-medium">#{item.id}</td>
                    <td className="px-4 py-3">#{item.consultaId || "-"}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${badgeEstadoClass(item.estadoCodigo)}`}>
                        {etiquetaEstado(item.estadoCodigo, item.estadoNombre)}
                      </span>
                    </td>
                    <td className="px-4 py-3">{item.estudianteNombre || "Sin asignar"}</td>
                    <td className="px-4 py-3">{item.conciliadorNombre || "Sin asignar"}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          disabled={!item.documentoSolicitudPath}
                          onClick={() => descargarDocumento(item.documentoSolicitudPath)}
                        >
                          Solicitud
                        </Button>
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          disabled={!item.actaPath}
                          onClick={() => descargarDocumento(item.actaPath)}
                        >
                          Acta
                        </Button>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Button type="button" size="sm" onClick={() => cargarDetalle(item.id)}>
                        <Eye className="mr-2 h-4 w-4" />
                        Ver
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          pageSize={pageSize}
          onPageChange={setCurrentPage}
          onPageSizeChange={handlePageSizeChange}
          pageSizeOptions={PAGE_SIZE_OPTIONS}
          totalItems={conciliacionesFiltradas.length}
        />
      </section>

      {detalle && (
        <section className="rounded-2xl border bg-card p-5 shadow-sm">
          <div className="mb-5 flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <h3 className="text-xl font-semibold">Detalle de conciliación #{detalle.id}</h3>
              <p className="mt-1 text-sm text-muted-foreground">
                Consulta #{detalle.consultaId} · Creada por {detalle.solicitadoPorUsername || "No registra"}
              </p>
            </div>
            <span className={`inline-flex w-fit rounded-full border px-3 py-1 text-sm font-medium ${badgeEstadoClass(detalle.estadoCodigo)}`}>
              {etiquetaEstado(detalle.estadoCodigo, detalle.estadoNombre)}
            </span>
          </div>

          {loadingDetalle ? (
            <div className="rounded-xl border bg-muted/30 p-6 text-center text-sm text-muted-foreground">
              Cargando detalle...
            </div>
          ) : (
            <div className="space-y-6">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
                <InfoCard title="Estudiante" value={detalle.estudianteNombre || "Sin asignar"} icon={<Users className="h-4 w-4" />} />
                <InfoCard title="Conciliador" value={detalle.conciliadorNombre || "Sin asignar"} icon={<UserCheck className="h-4 w-4" />} />
                <InfoCard title="Fecha conciliación" value={formatearFecha(detalle.fechaConciliacion)} icon={<FileText className="h-4 w-4" />} />
                <InfoCard title="Finalización" value={formatearFecha(detalle.fechaFinalizacion)} icon={<FileCheck2 className="h-4 w-4" />} />
              </div>

              {detalle.reunion && (
                <div className="rounded-xl border bg-muted/20 p-4">
                  <h4 className="mb-2 font-semibold">Reunión registrada</h4>
                  <div className="grid grid-cols-1 gap-2 text-sm md:grid-cols-3">
                    <p><span className="font-medium">Fecha:</span> {formatearFecha(detalle.reunion.fechaReunion)}</p>
                    <p><span className="font-medium">Sede:</span> {detalle.reunion.sedeNombre || detalle.reunion.sedeId || "No registra"}</p>
                    <p><span className="font-medium">Observaciones:</span> {detalle.reunion.observaciones || "No registra"}</p>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
                <PersonasCard title="Consultante" items={detalle.consultante ? [detalle.consultante] : []} />
                <PersonasCard title="Partes" items={detalle.partes || []} />
                <PersonasCard title="Contrapartes" items={detalle.contrapartes || []} />
              </div>

              <div className="rounded-xl border p-4">
                <h4 className="mb-3 font-semibold">Documentos</h4>
                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    disabled={!detalle.documentoSolicitudPath}
                    onClick={() => descargarDocumento(detalle.documentoSolicitudPath)}
                  >
                    <Download className="mr-2 h-4 w-4" />
                    Descargar solicitud
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    disabled={!detalle.actaPath}
                    onClick={() => descargarDocumento(detalle.actaPath)}
                  >
                    <Download className="mr-2 h-4 w-4" />
                    Descargar acta
                  </Button>
                </div>
              </div>

              {mostrarPanelGestion ? (
                <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                  {puedeAsignarEstudiante && (
                    <ActionCard title="Asignar estudiante">
                      <div className="flex flex-col gap-2 sm:flex-row">
                        <select
                          value={estudianteId}
                          onChange={(event) => setEstudianteId(event.target.value)}
                          className="h-10 flex-1 rounded-md border bg-background px-3 text-sm"
                        >
                          <option value="">Selecciona estudiante</option>
                          {estudiantes.map((item) => (
                            <option key={item.id} value={item.id}>
                              #{item.id} - {nombrePersona(item)}
                            </option>
                          ))}
                        </select>
                        <Button type="button" onClick={asignarEstudiante} disabled={saving}>
                          Guardar
                        </Button>
                      </div>
                    </ActionCard>
                  )}

                  {puedeAsignarConciliador && (
                    <ActionCard title="Asignar conciliador">
                      <div className="flex flex-col gap-2 sm:flex-row">
                        <select
                          value={conciliadorId}
                          onChange={(event) => setConciliadorId(event.target.value)}
                          className="h-10 flex-1 rounded-md border bg-background px-3 text-sm"
                        >
                          <option value="">Selecciona conciliador</option>
                          {conciliadores.map((item) => (
                            <option key={item.id} value={item.id}>
                              #{item.id} - {nombrePersona(item)}
                            </option>
                          ))}
                        </select>
                        <Button type="button" onClick={asignarConciliador} disabled={saving}>
                          Guardar
                        </Button>
                      </div>
                    </ActionCard>
                  )}

                  {puedeCambiarEstado && (
                    <ActionCard title="Cambiar estado no final">
                      <div className="flex flex-col gap-2 sm:flex-row">
                        <select
                          value={estadoNoFinal}
                          onChange={(event) => setEstadoNoFinal(event.target.value)}
                          className="h-10 flex-1 rounded-md border bg-background px-3 text-sm"
                        >
                          {ESTADOS_NO_FINALES.map((item) => (
                            <option key={item.value} value={item.value}>
                              {item.label}
                            </option>
                          ))}
                        </select>
                        <Button type="button" onClick={cambiarEstado} disabled={saving}>
                          Cambiar
                        </Button>
                      </div>
                    </ActionCard>
                  )}

                  {puedeFinalizar && (
                    <ActionCard title="Finalizar con acta">
                      <form onSubmit={finalizarConciliacion} className="space-y-3">
                        <select
                          value={estadoFinal}
                          onChange={(event) => setEstadoFinal(event.target.value)}
                          className="h-10 w-full rounded-md border bg-background px-3 text-sm"
                        >
                          {ESTADOS_FINALES.map((item) => (
                            <option key={item.value} value={item.value}>
                              {item.label}
                            </option>
                          ))}
                        </select>
                        <input
                          id="acta-conciliacion"
                          type="file"
                          accept="application/pdf,.pdf"
                          onChange={(event) => setArchivoActa(event.target.files?.[0] || null)}
                          className="block h-10 w-full rounded-md border bg-background px-3 py-2 text-sm"
                        />
                        <Button type="submit" disabled={saving}>
                          Finalizar conciliación
                        </Button>
                      </form>
                    </ActionCard>
                  )}

                  {puedeReemplazarSolicitud && (
                    <ActionCard title="Reemplazar solicitud">
                      <form onSubmit={reemplazarSolicitud} className="space-y-3">
                        <input
                          id="solicitud-reemplazo-conciliacion"
                          type="file"
                          accept="application/pdf,.pdf"
                          onChange={(event) => setArchivoSolicitudReemplazo(event.target.files?.[0] || null)}
                          className="block h-10 w-full rounded-md border bg-background px-3 py-2 text-sm"
                        />
                        <Button type="submit" disabled={saving} variant="outline">
                          Reemplazar solicitud
                        </Button>
                      </form>
                    </ActionCard>
                  )}

                  {puedeDesactivar && (
                    <ActionCard title="Desactivar conciliación">
                      <Button type="button" variant="destructive" onClick={desactivarConciliacion} disabled={saving}>
                        <Trash2 className="mr-2 h-4 w-4" />
                        Desactivar
                      </Button>
                    </ActionCard>
                  )}
                </div>
              ) : (
                <div className="rounded-xl border bg-muted/20 p-4 text-sm text-muted-foreground">
                  Este rol solo tiene visualización. No se muestran acciones de gestión para esta conciliación.
                </div>
              )}
            </div>
          )}
        </section>
      )}
    </div>
  );
}

function InfoCard({ title, value, icon }) {
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

function PersonasCard({ title, items }) {
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

function ActionCard({ title, description, children }) {
  return (
    <div className="rounded-xl border bg-background p-4">
      <h4 className="font-semibold">{title}</h4>
      <p className="mb-3 mt-1 text-sm text-muted-foreground">{description}</p>
      {children}
    </div>
  );
}

export default ConciliacionesForm;
