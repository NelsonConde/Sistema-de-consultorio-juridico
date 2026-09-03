"use client"

import * as React from "react"
import { useRouter } from "next/navigation"
import { RefreshCw, LayoutDashboard, FileText, ClipboardList, MessageSquare, ChevronRight, AlertCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import { API_URL_BASE } from "@/lib/config"
import { apiResponse, withErrorReference } from "@/lib/api"
import { PERMISOS } from "@/lib/permission"
import {
  tienePermiso, esAdministrativo, esAsesor, esMonitor, esEstudiante,
} from "@/lib/authz"


/**
 * Date handling.
 * Semestre 1: enero-mayo (meses 0-5), Semestre 2: junio-diciembre (meses 6-11)
 *
 * @returns {Object} Result value.
 */
function calcularSemestreActual() {
  const hoy = new Date()
  return { año: hoy.getFullYear(), semestre: hoy.getMonth() >= 6 ? 2 : 1 }
}

/**
 * State handling.
 *
 * @param {string} nombre - Parameter description.
 * @returns {string} Result value.
 */
function etiquetaEstado(nombre) {
  const mapa = {
    PENDIENTE: "Pendiente", ACTIVO: "Activo", EN_PROCESO: "En proceso",
    URGENTE: "Urgente", CERRADO: "Cerrado", ARCHIVADO: "Archivado",
  }
  return mapa[String(nombre || "").toUpperCase()] || nombre || "Sin estado"
}

/**
 * Date handling.
 *
 * @param {string|Date} str - Value to format.
 * @returns {string} Result value.
 */
function formatFecha(str) {
  if (!str) return "—"
  try {
    return new Date(str).toLocaleDateString("es-CO", { day: "2-digit", month: "short", year: "numeric" })
  } catch { return str }
}

/**
 * Implementation detail.
 * Implementation detail.
 *
 * @param {*} payload - Parameter description.
 * @returns {Array} Result value.
 */
function obtenerArrayDesdeRespuesta(payload) {
  if (Array.isArray(payload)) return payload
  if (!payload || typeof payload !== "object") return []
  for (const k of ["content", "data", "items", "rows", "consultas", "resultado", "result", "payload"]) {
    if (Array.isArray(payload[k])) return payload[k]
  }
  return []
}

const CHART_COLORS = [
  "var(--chart-1)", "var(--chart-2)", "var(--chart-3)", "var(--chart-4)", "var(--chart-5)",
]

const METRIC_CARDS = [
  { key: "totalConsultas",         label: "Total consultas",    cls: "bg-primary text-primary-foreground" },
  { key: "consultasFinalizadas",   label: "Finalizadas",        cls: "bg-chart-1 text-primary-foreground" },
  { key: "consultasPendientes",    label: "Pendientes",         cls: "bg-chart-2 text-primary-foreground" },
  { key: "totalPersonasAtendidas", label: "Personas atendidas", cls: "bg-secondary text-secondary-foreground" },
]


/**
 * Implementation detail.
 * Implementation detail.
 *
 * @component
 * @param {Object} props - Component properties.
 * @param {string} props.label - Descriptive label.
 * @param {number|string} props.value - Parameter description.
 * @param {string} props.cls - Parameter description.
 * @returns {JSX.Element} Result value.
 */
function MetricCard({ label, value, cls }) {
  return (
    <div className={`${cls} rounded-lg px-4 py-3 flex flex-col gap-0.5 min-w-0`}>
      <span className="text-[10px] font-semibold uppercase tracking-wider opacity-75">{label}</span>
      <span className="text-3xl font-extrabold leading-tight">{value ?? "—"}</span>
    </div>
  )
}

/**
 * Consultation flow detail.
 * Implementation detail.
 *
 * @component
 * @param {Object} props - Component properties.
 * @param {number} props.finalizadas - Number of completed consultations.
 * @param {number} props.pendientes - Number of pending consultations.
 * @returns {JSX.Element} Result value.
 */
function DonutChart({ finalizadas, pendientes }) {
  const total = finalizadas + pendientes
  if (total === 0)
    return <p className="text-xs text-muted-foreground text-center py-4">Sin consultas</p>

  const pctFin = Math.round((finalizadas / total) * 100)
  const r = 52, cx = 66, cy = 66
  const circ = 2 * Math.PI * r
  const dash = (finalizadas / total) * circ

  return (
    <div className="flex items-center gap-4">
      <svg width={132} height={132} viewBox="0 0 132 132" className="shrink-0">
        <circle cx={cx} cy={cy} r={r} fill="none" stroke="var(--accent)" strokeWidth={20} />
        {finalizadas > 0 && (
          <circle cx={cx} cy={cy} r={r} fill="none"
            stroke="var(--primary)" strokeWidth={20}
            strokeDasharray={`${dash} ${circ}`}
            transform={`rotate(-90 ${cx} ${cy})`} />
        )}
        <text x={cx} y={cy - 6} textAnchor="middle"
          style={{ fontSize: 20, fontWeight: 800, fill: "var(--primary)" }}>
          {pctFin}%
        </text>
        <text x={cx} y={cy + 12} textAnchor="middle"
          style={{ fontSize: 9, fill: "var(--muted-foreground)" }}>
          finalizadas
        </text>
      </svg>
      <div className="flex flex-col gap-2 text-xs">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-primary shrink-0" />
          <span>Finalizadas</span>
          <strong className="ml-auto pl-3">{finalizadas}</strong>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-accent border border-border shrink-0" />
          <span>Pendientes</span>
          <strong className="ml-auto pl-3">{pendientes}</strong>
        </div>
      </div>
    </div>
  )
}

/**
 * Consultation flow detail.
 * Implementation detail.
 *
 * @component
 * @param {Object} props - Component properties.
 * @param {Array<{nombre: string, cantidad: number}>} props.areas - Parameter description.
 * @returns {JSX.Element} Result value.
 */
function BarChartAreas({ areas }) {
  if (!areas || areas.length === 0)
    return <p className="text-xs text-muted-foreground text-center py-4">Sin datos de áreas</p>

  const W = 480, H = 150
  const pad = { top: 14, right: 12, bottom: 32, left: 28 }
  const chartW = W - pad.left - pad.right
  const chartH = H - pad.top - pad.bottom
  const maxVal = Math.max(...areas.map((a) => a.cantidad), 1)
  const barW = Math.min(38, (chartW / areas.length) - 10)

  return (
    <svg width="100%" viewBox={`0 0 ${W} ${H}`} className="overflow-visible">
      {[0, 0.5, 1].map((f) => {
        const y = pad.top + chartH * (1 - f)
        return (
          <React.Fragment key={f}>
            <line x1={pad.left} x2={W - pad.right} y1={y} y2={y}
              stroke="var(--border)" strokeWidth={1} strokeDasharray="4 3" />
            <text x={pad.left - 4} y={y + 3} textAnchor="end"
              style={{ fontSize: 8, fill: "var(--muted-foreground)" }}>
              {Math.round(maxVal * f)}
            </text>
          </React.Fragment>
        )
      })}
      {areas.map((area, i) => {
        const slotW = chartW / areas.length
        const x = pad.left + slotW * i + (slotW - barW) / 2
        const bH = Math.max((area.cantidad / maxVal) * chartH, 2)
        const y = pad.top + chartH - bH
        const color = CHART_COLORS[i % CHART_COLORS.length]
        return (
          <React.Fragment key={area.nombre}>
            <rect x={x} y={y} width={barW} height={bH} fill={color} rx={3} />
            <text x={x + barW / 2} y={y - 4} textAnchor="middle"
              style={{ fontSize: 9, fontWeight: 700, fill: color }}>
              {area.cantidad}
            </text>
            <text x={x + barW / 2} y={H - pad.bottom + 12} textAnchor="middle"
              style={{ fontSize: 9, fill: "var(--muted-foreground)" }}>
              {area.nombre.length > 9 ? area.nombre.slice(0, 8) + "…" : area.nombre}
            </text>
          </React.Fragment>
        )
      })}
      <line x1={pad.left} x2={W - pad.right} y1={pad.top + chartH} y2={pad.top + chartH}
        stroke="var(--border)" strokeWidth={1} />
    </svg>
  )
}

/**
 * Implementation detail.
 * Implementation detail.
 *
 * @component
 * @param {Object} props - Component properties.
 * @param {string} props.title - Panel title.
 * @param {Function} props.icon - Parameter description.
 * @param {number} [props.count] - Parameter description.
 * @param {JSX.Element} props.children - Child content.
 * @param {string} [props.className] - Additional Tailwind classes.
 * @returns {JSX.Element} Result value.
 */
function Panel({ title, icon: Icon, count, children, className = "" }) {
  return (
    <div className={`bg-card border border-border rounded-xl overflow-hidden flex flex-col ${className}`}>
      <div className="flex items-center justify-between px-4 py-2.5 border-b border-border shrink-0">
        <div className="flex items-center gap-2">
          {Icon && <Icon className="w-3.5 h-3.5 text-primary" />}
          <span className="font-bold text-xs text-card-foreground uppercase tracking-wide">{title}</span>
        </div>
        {count !== undefined && (
          <span className="bg-primary text-primary-foreground text-[10px] font-bold px-2 py-0.5 rounded">
            {count}
          </span>
        )}
      </div>
      <div className="p-4 flex-1 overflow-auto">{children}</div>
    </div>
  )
}

/**
 * Data loading behavior.
 * User flow detail.
 *
 * @component
 * @param {Object} props - Component properties.
 * @param {string} [props.className] - Additional CSS classes.
 * @returns {JSX.Element} Result value.
 */
function Skeleton({ className = "h-20" }) {
  return <div className={`rounded-xl bg-muted animate-pulse ${className}`} />
}


/**
 * List and table handling.
 * State handling.
 *
 * @component
 * @param {Object} props - Component properties.
 * @param {Array<Object>} props.items - Item to process.
 * @param {boolean} props.cargando - Whether data is loading.
 * @returns {JSX.Element} Result value.
 */
function ConsultasPendientesLista({ items, cargando }) {
  if (cargando) return <div className="space-y-1.5">{[1,2,3].map(i => <Skeleton key={i} className="h-10" />)}</div>
  if (!items || items.length === 0)
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground py-2">
        <AlertCircle className="w-3.5 h-3.5 shrink-0" />
        Sin consultas pendientes
      </div>
    )

  return (
    <div className="space-y-1">
      {items.map((c) => {
        const id = c.id || c.consultaId
        const descripcion = c.consulta || c.descripcion || c.hechos || `Consulta #${id}`
        const persona = [c.nombre, c.apellido].filter(Boolean).join(" ") ||
          [c.personaNombre, c.personaApellido].filter(Boolean).join(" ") || "—"
        const fecha = formatFecha(c.fecha || c.createdAt)
        const estado = etiquetaEstado(c.estado)

        return (
          <div key={id}
            className="flex items-start justify-between gap-2 rounded-lg px-2.5 py-2 hover:bg-muted/50 transition-colors group">
            <div className="flex items-start gap-2 min-w-0">
              <span className="text-[10px] font-bold text-muted-foreground mt-0.5 shrink-0">#{id}</span>
              <div className="min-w-0">
                <p className="text-xs font-medium text-foreground truncate leading-tight">{descripcion}</p>
                <p className="text-[10px] text-muted-foreground truncate">{persona} · {fecha}</p>
              </div>
            </div>
            <span className="text-[10px] bg-primary/10 text-primary font-semibold px-1.5 py-0.5 rounded shrink-0">
              {estado}
            </span>
          </div>
        )
      })}
    </div>
  )
}

/**
 * List and table handling.
 * State handling.
 *
 * @component
 * @param {Object} props - Component properties.
 * @param {Array<Object>} props.items - Item to process.
 * @param {boolean} props.cargando - Whether data is loading.
 * @returns {JSX.Element} Result value.
 */
function TareasPendientesLista({ items, cargando }) {
  if (cargando) return <div className="space-y-1.5">{[1,2,3].map(i => <Skeleton key={i} className="h-10" />)}</div>
  if (!items || items.length === 0)
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground py-2">
        <AlertCircle className="w-3.5 h-3.5 shrink-0" />
        Sin tareas pendientes
      </div>
    )

  return (
    <div className="space-y-1">
      {items.map((t, i) => {
        const id = t.id || t.seguimientoId
        const descripcion = t.descripcion || t.titulo || `Tarea #${id}`
        const consultaId = t.consultaId
        const limite = formatFecha(t.fechaLimite || t.fechaEntrega)

        return (
          <div key={id || i}
            className="flex items-start justify-between gap-2 rounded-lg px-2.5 py-2 hover:bg-muted/50 transition-colors">
            <div className="flex items-start gap-2 min-w-0">
              <ChevronRight className="w-3 h-3 text-muted-foreground shrink-0 mt-0.5" />
              <div className="min-w-0">
                <p className="text-xs font-medium text-foreground truncate leading-tight">{descripcion}</p>
                <p className="text-[10px] text-muted-foreground">
                  {consultaId ? `Consulta #${consultaId}` : "—"}
                  {limite !== "—" ? ` · Límite: ${limite}` : ""}
                </p>
              </div>
            </div>
            <span className="text-[10px] bg-primary/10 text-primary font-semibold px-1.5 py-0.5 rounded shrink-0">
              Pendiente
            </span>
          </div>
        )
      })}
    </div>
  )
}

/**
 * List and table handling.
 * Implementation detail.
 *
 * @component
 * @param {Object} props - Component properties.
 * @param {Array<Object>} props.items - Item to process.
 * @param {boolean} props.cargando - Whether data is loading.
 * @returns {JSX.Element} Result value.
 */
function RespuestasPendientesLista({ items, cargando }) {
  if (cargando) return <div className="space-y-1.5">{[1,2,3].map(i => <Skeleton key={i} className="h-10" />)}</div>
  if (!items || items.length === 0)
    return (
      <div className="flex items-center gap-2 text-xs text-muted-foreground py-2">
        <AlertCircle className="w-3.5 h-3.5 shrink-0" />
        Sin respuestas por calificar
      </div>
    )

  return (
    <div className="space-y-1">
      {items.map((r) => {
        const contenido = r.contenido || `Respuesta #${r.id}`
        const estudiante = r.estudianteNombre || "Estudiante"

        return (
          <div key={r.id}
            className="flex items-start justify-between gap-2 rounded-lg px-2.5 py-2 hover:bg-muted/50 transition-colors">
            <div className="flex items-start gap-2 min-w-0">
              <MessageSquare className="w-3 h-3 text-muted-foreground shrink-0 mt-0.5" />
              <div className="min-w-0">
                <p className="text-xs font-medium text-foreground truncate leading-tight">{contenido}</p>
                <p className="text-[10px] text-muted-foreground">
                  {estudiante}
                  {r.consultaId ? ` · Consulta #${r.consultaId}` : ""}
                  {r.fueraPlazo ? " · ⚠ Fuera de plazo" : ""}
                </p>
              </div>
            </div>
            <span className="text-[10px] bg-chart-2/20 text-chart-1 font-semibold px-1.5 py-0.5 rounded shrink-0">
              Por calificar
            </span>
          </div>
        )
      })}
    </div>
  )
}


/**
 * Implementation detail.
 * Consultation flow detail.
 * Role handling.
 *
 * @component
 * @returns {JSX.Element} Result value.
 *
 * @description
 * Role handling.
 * Consultation flow detail.
 * Consultation flow detail.
 * List and table handling.
 * Data loading behavior.
 */
export function InicioForm() {
  const router = useRouter()
  const [user, setUser] = React.useState(null)
  const [stats, setStats] = React.useState(null)
  const [cargandoStats, setCargandoStats] = React.useState(true)

  // listas
  const [consultasPendientes, setConsultasPendientes] = React.useState([])
  const [tareasPendientes, setTareasPendientes] = React.useState([])
  const [respuestasPendientes, setRespuestasPendientes] = React.useState([])
  const [cargandoConsultas, setCargandoConsultas] = React.useState(false)
  const [cargandoTareas, setCargandoTareas] = React.useState(false)
  const [cargandoRespuestas, setCargandoRespuestas] = React.useState(false)

  const [error, setError] = React.useState("")
  const { año, semestre } = calcularSemestreActual()

  const cargarStats = React.useCallback(async (u) => {
    setCargandoStats(true)
    setError("")

    try {
      let url = null

      if (tienePermiso(u, PERMISOS.VER_REPORTES) || esAdministrativo(u)) {
        url = `${API_URL_BASE}/estadisticas/${año}/semestre/${semestre}`
      } else if (
          tienePermiso(u, PERMISOS.VER_CONSULTAS) &&
          (esAsesor(u) || esMonitor(u) || esEstudiante(u))
      ) {
        url = `${API_URL_BASE}/estadisticas/${año}/semestre/${semestre}/me`
      }

      if (!url) {
        setStats(null)
        return
      }

      const { response: res, data, correlationId } = await apiResponse(url, {
        method: "GET",
      })

      if (!res.ok) {
        setStats(null)
        setError(
            withErrorReference(
                "No se pudieron cargar las estadísticas.",
                correlationId
            )
        )
        return
      }

      setStats(data)
    } catch {
      setStats(null)
      setError("No se pudieron cargar las estadísticas.")
    } finally {
      setCargandoStats(false)
    }
  }, [año, semestre])


  const cargarConsultasPendientes = React.useCallback(async () => {
    setCargandoConsultas(true)
    try {
      const { response: res, data: payload } = await apiResponse(`${API_URL_BASE}/consultas`, { method: "GET" })
      if (!res.ok) return
      const lista = obtenerArrayDesdeRespuesta(payload)
      const pendientes = lista
        .filter((c) => {
          const estado = String(c.estado || "").toUpperCase()
          return estado === "PENDIENTE" || estado === "URGENTE" || estado === "EN_PROCESO" || estado === "ACTIVO"
        })
        .slice(0, 10)
      setConsultasPendientes(pendientes)
    } catch {
      setConsultasPendientes([])
    } finally {
      setCargandoConsultas(false)
    }
  }, [])


  const cargarTareasPendientes = React.useCallback(async () => {
    setCargandoTareas(true)
    try {
      const { response: resConsultas, data: payload } = await apiResponse(`${API_URL_BASE}/consultas`, { method: "GET" })
      if (!resConsultas.ok) { setTareasPendientes([]); return }

      const consultas = obtenerArrayDesdeRespuesta(payload).slice(0, 5)

      const resultados = await Promise.allSettled(
        consultas.map((c) => {
          const cid = c.id || c.consultaId
          return apiResponse(
            `${API_URL_BASE}/seguimientos/consulta/${cid}/visibles-estudiante`,
            { method: "GET" }
          ).then(({ response, data }) => response.ok ? data : [])
        })
      )

      const todasLasTareas = resultados.flatMap((r) =>
        r.status === "fulfilled" ? obtenerArrayDesdeRespuesta(r.value) : []
      )

      const pendientes = todasLasTareas
        .filter((t) => String(t.estado || "PENDIENTE").toUpperCase() === "PENDIENTE")
        .slice(0, 10)

      setTareasPendientes(pendientes)
    } catch {
      setTareasPendientes([])
    } finally {
      setCargandoTareas(false)
    }
  }, [])


  const cargarRespuestasPendientes = React.useCallback(async () => {
    setCargandoRespuestas(true)
    try {
      const { response: res, data: payload } = await apiResponse(`${API_URL_BASE}/seguimientos/respuestas/pendientes`, { method: "GET" })
      if (!res.ok) return
      const lista = obtenerArrayDesdeRespuesta(payload)
      setRespuestasPendientes(lista.slice(0, 10))
    } catch {
      setRespuestasPendientes([])
    } finally {
      setCargandoRespuestas(false)
    }
  }, [])


  React.useEffect(() => {
    async function init() {
      try {
        const { response: res, data: u } = await apiResponse(`${API_URL_BASE}/auth/me`, { method: "GET" })
        if (res.status === 401) { router.replace("/"); return }
        if (!res.ok) { setCargandoStats(false); return }
        setUser(u)

        await Promise.allSettled([
          cargarStats(u),
          cargarConsultasPendientes(),
          // Follow-up workflow detail.
          esEstudiante(u) ? cargarTareasPendientes() : Promise.resolve(),
          // Implementation detail.
          (esAsesor(u) || esMonitor(u) || esAdministrativo(u) || tienePermiso(u, PERMISOS.VER_REPORTES))
            ? cargarRespuestasPendientes()
            : Promise.resolve(),
        ])
      } catch {
        setCargandoStats(false)
        setError("No se pudo cargar la información de inicio.")
      }
    }
    init()
  }, [router, cargarStats, cargarConsultasPendientes, cargarTareasPendientes, cargarRespuestasPendientes])

  const recargarTodo = React.useCallback(() => {
    if (!user) return
    cargarStats(user)
    cargarConsultasPendientes()
    if (esEstudiante(user)) cargarTareasPendientes()
    if (esAsesor(user) || esMonitor(user) || esAdministrativo(user) || tienePermiso(user, PERMISOS.VER_REPORTES))
      cargarRespuestasPendientes()
  }, [user, cargarStats, cargarConsultasPendientes, cargarTareasPendientes, cargarRespuestasPendientes])


  const nombreRol = user?.tipoPerfil
    ? user.tipoPerfil.charAt(0) + user.tipoPerfil.slice(1).toLowerCase()
    : null

  const esAdmin = user ? tienePermiso(user, PERMISOS.VER_REPORTES) || esAdministrativo(user) : false
  const mostrarTareas = user ? esEstudiante(user) : false
  const mostrarRespuestas = user
    ? esAsesor(user) || esMonitor(user) || esAdministrativo(user) || tienePermiso(user, PERMISOS.VER_REPORTES)
    : false

  const cargandoAlgo = cargandoStats

  return (
    <div className="p-4 lg:p-6 flex flex-col gap-4 bg-background">

      {/* ── Encabezado ── */}
      <div className="flex flex-wrap justify-between items-center gap-3">
        <div>
          <div className="flex items-center gap-2">
            <LayoutDashboard className="w-4 h-4 text-primary" />
            <h1 className="text-lg font-extrabold text-foreground leading-none">
              Bienvenido{user?.nombre ? `, ${user.nombre}` : ""}
            </h1>
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">
            {nombreRol ? `${nombreRol} · ` : ""}Semestre {año}-{semestre}
          </p>
        </div>
        <Button variant="outline" size="sm" className="h-8 gap-1.5 text-xs"
          onClick={recargarTodo} disabled={cargandoAlgo}>
          <RefreshCw className={`w-3.5 h-3.5 ${cargandoAlgo ? "animate-spin" : ""}`} />
          Actualizar
        </Button>
      </div>

      {/* Error */}
      {error && (
        <div className="rounded-lg border border-primary/30 bg-primary/10 px-3 py-2 text-primary text-xs">
          {error}
        </div>
      )}

      {/* Implementation detail.*/}
      {cargandoStats ? (
        <div className="grid grid-cols-2 xl:grid-cols-4 gap-3">
          {[1,2,3,4].map((i) => <Skeleton key={i} className="h-16" />)}
        </div>
      ) : stats ? (
        <div className="grid grid-cols-2 xl:grid-cols-4 gap-3">
          {METRIC_CARDS.map(({ key, label, cls }) => (
            <MetricCard key={key} label={label} value={stats[key]} cls={cls} />
          ))}
        </div>
      ) : null}

      {/* Implementation detail.*/}
      {!cargandoStats && stats && (
        esAdmin && stats.consultasPorArea && stats.consultasPorArea.length > 0 ? (
          <div className="grid lg:grid-cols-3 gap-3">
            <Panel title="Consultas por área"
              count={`Total ${stats.totalConsultas}`}
              className="lg:col-span-2">
              <BarChartAreas areas={stats.consultasPorArea} />
            </Panel>
            <Panel title="Estado de consultas"
              count={`${Math.round((stats.consultasFinalizadas / (stats.totalConsultas || 1)) * 100)}% completado`}>
              <DonutChart finalizadas={stats.consultasFinalizadas} pendientes={stats.consultasPendientes} />
            </Panel>
          </div>
        ) : (
          <div className="grid lg:grid-cols-2 gap-3">
            <Panel title="Estado de consultas"
              count={`${Math.round((stats.consultasFinalizadas / (stats.totalConsultas || 1)) * 100)}% completado`}>
              <DonutChart finalizadas={stats.consultasFinalizadas} pendientes={stats.consultasPendientes} />
            </Panel>
            {/* State handling.*/}
            {stats.procesosPorEstado && stats.procesosPorEstado.length > 0 && (
              <Panel title="Procesos por estado">
                <div className="space-y-2.5">
                  {stats.procesosPorEstado.map((p, i) => {
                    const max = Math.max(...stats.procesosPorEstado.map((x) => x.cantidad), 1)
                    const pct = Math.round((p.cantidad / max) * 100)
                    const color = CHART_COLORS[i % CHART_COLORS.length]
                    return (
                      <div key={p.nombre}>
                        <div className="flex justify-between text-xs mb-1">
                          <span className="font-medium text-foreground">{etiquetaEstado(p.nombre)}</span>
                          <span className="font-bold" style={{ color }}>{p.cantidad}</span>
                        </div>
                        <div className="w-full h-1.5 rounded-full bg-accent">
                          <div className="h-1.5 rounded-full transition-all duration-500"
                            style={{ width: `${pct}%`, backgroundColor: color }} />
                        </div>
                      </div>
                    )
                  })}
                </div>
              </Panel>
            )}
          </div>
        )
      )}

      {/* ── Fila 3: listas operativas ── */}
      <div className={`grid gap-3 ${mostrarTareas || mostrarRespuestas ? "lg:grid-cols-2" : "grid-cols-1"}`}>

        {/* Consultation flow detail.*/}
        <Panel
          title="Consultas pendientes"
          icon={FileText}
          count={consultasPendientes.length > 0 ? consultasPendientes.length : undefined}
        >
          <ConsultasPendientesLista items={consultasPendientes} cargando={cargandoConsultas} />
        </Panel>

        {/* Follow-up workflow detail.*/}
        {mostrarTareas && (
          <Panel
            title="Mis tareas pendientes"
            icon={ClipboardList}
            count={tareasPendientes.length > 0 ? tareasPendientes.length : undefined}
          >
            <TareasPendientesLista items={tareasPendientes} cargando={cargandoTareas} />
          </Panel>
        )}

        {/* Implementation detail.*/}
        {mostrarRespuestas && (
          <Panel
            title="Respuestas por calificar"
            icon={MessageSquare}
            count={respuestasPendientes.length > 0 ? respuestasPendientes.length : undefined}
          >
            <RespuestasPendientesLista items={respuestasPendientes} cargando={cargandoRespuestas} />
          </Panel>
        )}
      </div>

    </div>
  )
}
