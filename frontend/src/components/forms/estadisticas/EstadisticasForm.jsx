"use client";

import React, { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";
import {
  RefreshCw, Download, FileBarChart2, Calendar,
  ChevronRight, X, Users, FileText, Scale, ClipboardList, GraduationCap,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";

// ─── helpers ──────────────────────────────────────────────────────────────────

/**
 * Calcula el semestre actual basado en la fecha del sistema.
 * Semestre 1: enero-mayo (meses 0-5), Semestre 2: junio-diciembre (meses 6-11)
 * 
 * @returns {Object} Objeto con propiedades {año: number, semestre: 1|2}
 */
function calcularSemestreActual() {
  const hoy = new Date();
  return { año: hoy.getFullYear(), semestre: hoy.getMonth() >= 6 ? 2 : 1 };
}

/**
 * Convierte un nombre de estado a su etiqueta en español.
 * 
 * @param {string} nombre - Nombre del estado (ej: "PENDIENTE", "ACTIVO")
 * @returns {string} Etiqueta en español o el nombre original si no coincide
 */
function etiquetaEstado(nombre) {
  const mapa = {
    PENDIENTE: "Pendiente", ACTIVO: "Activo", ARCHIVADO: "Archivado",
    SENTENCIA_FAVORABLE: "Sentencia favorable",
    SENTENCIA_DESFAVORABLE: "Sentencia desfavorable",
    CONCILIADO: "Conciliado", EN_TRAMITE: "En trámite", DESISTIDO: "Desistido",
  };
  return mapa[String(nombre || "")] || nombre;
}

/**
 * Retorna la fecha de hoy en formato ISO (yyyy-MM-dd).
 * Útil para valores por defecto en seleccionadores de fechas.
 * 
 * @returns {string} Fecha hoy en formato ISO
 */
function hoyStr() { return new Date().toISOString().slice(0, 10); }


const CHART_COLORS = [
  "var(--chart-1)", "var(--chart-2)", "var(--chart-3)",
  "var(--chart-4)", "var(--chart-5)",
];

function BarChartAreas({ areas }) {
  if (!areas || areas.length === 0)
    return <p className="text-xs text-muted-foreground text-center py-4">Sin datos de áreas</p>;

  const W = 480, H = 150;
  const pad = { top: 14, right: 12, bottom: 32, left: 28 };
  const chartW = W - pad.left - pad.right;
  const chartH = H - pad.top - pad.bottom;
  const maxVal = Math.max(...areas.map((a) => a.cantidad), 1);
  const barW   = Math.min(38, (chartW / areas.length) - 10);

  return (
    <svg width="100%" viewBox={`0 0 ${W} ${H}`} className="overflow-visible">
      {[0, 0.5, 1].map((f) => {
        const y = pad.top + chartH * (1 - f);
        return (
          <React.Fragment key={f}>
            <line x1={pad.left} x2={W - pad.right} y1={y} y2={y}
              stroke="var(--border)" strokeWidth={1} strokeDasharray="4 3" />
            <text x={pad.left - 4} y={y + 3} textAnchor="end"
              style={{ fontSize: 8, fill: "var(--muted-foreground)" }}>
              {Math.round(maxVal * f)}
            </text>
          </React.Fragment>
        );
      })}
      {areas.map((area, i) => {
        const slotW = chartW / areas.length;
        const x     = pad.left + slotW * i + (slotW - barW) / 2;
        const bH    = Math.max((area.cantidad / maxVal) * chartH, 2);
        const y     = pad.top + chartH - bH;
        const color = CHART_COLORS[i % CHART_COLORS.length];
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
        );
      })}
      <line x1={pad.left} x2={W - pad.right}
        y1={pad.top + chartH} y2={pad.top + chartH}
        stroke="var(--border)" strokeWidth={1} />
    </svg>
  );
}

function DonutChart({ finalizadas, pendientes }) {
  const total = finalizadas + pendientes;
  if (total === 0)
    return <p className="text-xs text-muted-foreground text-center py-4">Sin consultas</p>;
  const pctFin = Math.round((finalizadas / total) * 100);
  const r = 52, cx = 66, cy = 66;
  const circ = 2 * Math.PI * r;
  const dash  = (finalizadas / total) * circ;
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
          style={{ fontSize: 20, fontWeight: 800, fill: "var(--primary)" }}>{pctFin}%</text>
        <text x={cx} y={cy + 12} textAnchor="middle"
          style={{ fontSize: 9, fill: "var(--muted-foreground)" }}>finalizadas</text>
      </svg>
      <div className="flex flex-col gap-2 text-xs">
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-primary shrink-0" />
          <span>Finalizadas</span><strong className="ml-auto pl-3">{finalizadas}</strong>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-2.5 h-2.5 rounded-full bg-accent border border-border shrink-0" />
          <span>Pendientes</span><strong className="ml-auto pl-3">{pendientes}</strong>
        </div>
      </div>
    </div>
  );
}

function BarrasHorizontales({ items }) {
  if (!items || items.length === 0)
    return <p className="text-xs text-muted-foreground py-1">Sin datos</p>;
  const maxVal = Math.max(...items.map((p) => p.cantidad), 1);
  return (
    <div className="space-y-2">
      {items.map((p, i) => {
        const pct   = Math.round((p.cantidad / maxVal) * 100);
        const color = CHART_COLORS[i % CHART_COLORS.length];
        return (
          <div key={p.nombre}>
            <div className="flex justify-between text-xs mb-0.5">
              <span className="font-medium text-foreground truncate max-w-[72%]">
                {etiquetaEstado(p.nombre)}
              </span>
              <span className="font-bold shrink-0 ml-2" style={{ color }}>{p.cantidad}</span>
            </div>
            <div className="w-full h-1.5 rounded-full bg-accent">
              <div className="h-1.5 rounded-full transition-all duration-500"
                style={{ width: `${pct}%`, backgroundColor: color }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

function Panel({ title, badge, children, className = "" }) {
  return (
    <div className={`bg-card border border-border rounded-xl overflow-hidden ${className}`}>
      {title && (
        <div className="flex justify-between items-center px-4 py-2.5 border-b border-border">
          <span className="font-bold text-xs text-card-foreground uppercase tracking-wide">{title}</span>
          {badge && (
            <span className="bg-primary text-primary-foreground text-[10px] font-bold px-2 py-0.5 rounded">
              {badge}
            </span>
          )}
        </div>
      )}
      <div className="p-4">{children}</div>
    </div>
  );
}

function Skeleton({ className = "h-20" }) {
  return <div className={`rounded-xl bg-muted animate-pulse ${className}`} />;
}

function MetricCard({ label, value, cls, icon: Icon, activa, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        "rounded-xl px-4 py-3 flex flex-col gap-0.5 min-w-0 text-left transition-all w-full",
        "border-2",
        cls,
        activa
          ? "border-primary-foreground/60 ring-2 ring-primary-foreground/40 scale-[1.02]"
          : "border-transparent hover:scale-[1.01] hover:brightness-110",
      ].join(" ")}
    >
      <div className="flex items-center justify-between">
        <span className="text-[10px] font-semibold uppercase tracking-wider opacity-75">{label}</span>
        {Icon && <Icon className="w-3.5 h-3.5 opacity-60" />}
      </div>
      <span className="text-3xl font-extrabold leading-tight">{value ?? "—"}</span>
      <span className="text-[10px] opacity-60 flex items-center gap-0.5 mt-0.5">
        {activa ? "Cerrar detalles" : "Ver detalles"}
        <ChevronRight className={`w-3 h-3 transition-transform ${activa ? "rotate-90" : ""}`} />
      </span>
    </button>
  );
}


function DetalleConsultas({ stats }) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
      <Panel title="Estado de consultas"
        badge={`${Math.round((stats.consultasFinalizadas / (stats.totalConsultas || 1)) * 100)}% completado`}>
        <DonutChart finalizadas={stats.consultasFinalizadas} pendientes={stats.consultasPendientes} />
      </Panel>
      <Panel title="Consultas por estado">
        <BarrasHorizontales items={stats.consultasPorEstado} />
      </Panel>
      <Panel title="Tipo de violencia">
        <BarrasHorizontales items={stats.consultasPorTipoViolencia} />
      </Panel>
      <Panel title="Consultas por área jurídica" badge={`Total ${stats.totalConsultas}`} className="lg:col-span-3">
        <BarChartAreas areas={stats.consultasPorArea} />
      </Panel>
    </div>
  );
}

function DetallePersonas({ stats }) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
      <Panel title="Por género"><BarrasHorizontales items={stats.personasPorGenero} /></Panel>
      <Panel title="Por estrato"><BarrasHorizontales items={stats.personasPorEstrato} /></Panel>
      <Panel title="Por zona"><BarrasHorizontales items={stats.personasPorZona} /></Panel>
      <Panel title="Grupo étnico"><BarrasHorizontales items={stats.personasPorGrupoEtnico} /></Panel>
      <Panel title="Por condición"><BarrasHorizontales items={stats.personasPorCondicion} /></Panel>
      <Panel title="Por municipio"><BarrasHorizontales items={stats.personasPorMunicipio} /></Panel>
    </div>
  );
}

function DetalleConciliaciones({ stats }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <Panel title="Conciliaciones por estado">
        <BarrasHorizontales items={stats.conciliacionesPorEstado} />
      </Panel>
      <Panel title="Total conciliaciones">
        <p className="text-4xl font-extrabold text-primary">{stats.totalConciliaciones ?? "—"}</p>
      </Panel>
    </div>
  );
}

function DetalleSeguimientos({ stats }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
      <Panel title="Seguimientos por estado">
        <BarrasHorizontales items={stats.seguimientosPorEstado} />
      </Panel>
      <Panel title="Procesos por estado"
        badge="Sin filtro semestral">
        <BarrasHorizontales items={stats.procesosPorEstado} />
      </Panel>
    </div>
  );
}

function DetalleEstudiantes({ stats }) {
  return (
    <div className="grid grid-cols-2 gap-3">
      <Panel title="Estudiantes activos">
        <p className="text-4xl font-extrabold text-primary">{stats.totalEstudiantesActivos ?? "—"}</p>
        <p className="text-xs text-muted-foreground mt-1">En el semestre</p>
      </Panel>
      <Panel title="Habilitados conciliación">
        <p className="text-4xl font-extrabold text-primary">
          {stats.totalEstudiantesHabilitadosConciliacion ?? "—"}
        </p>
        <p className="text-xs text-muted-foreground mt-1">Con conciliación activa</p>
      </Panel>
    </div>
  );
}

// Mapa de categorías
const CATEGORIAS = [
  {
    id: "consultas",
    label: "Consultas",
    icon: FileText,
    key: "totalConsultas",
    cls: "bg-primary text-primary-foreground",
    Detalle: DetalleConsultas,
  },
  {
    id: "personas",
    label: "Personas atendidas",
    icon: Users,
    key: "totalPersonasAtendidas",
    cls: "bg-chart-1 text-primary-foreground",
    Detalle: DetallePersonas,
  },
  {
    id: "conciliaciones",
    label: "Conciliaciones",
    icon: Scale,
    key: "totalConciliaciones",
    cls: "bg-chart-2 text-primary-foreground",
    Detalle: DetalleConciliaciones,
  },
  {
    id: "seguimientos",
    label: "Seguimientos",
    icon: ClipboardList,
    key: "totalSeguimientos",
    cls: "bg-secondary text-secondary-foreground",
    Detalle: DetalleSeguimientos,
  },
  {
    id: "estudiantes",
    label: "Estudiantes activos",
    icon: GraduationCap,
    key: "totalEstudiantesActivos",
    cls: "bg-chart-3 text-primary-foreground",
    Detalle: DetalleEstudiantes,
  },
];


function EstadisticasContenido({ stats }) {
  const [abierta, setAbierta] = useState(null);

  function toggle(id) {
    setAbierta((prev) => (prev === id ? null : id));
  }

  const catActiva = CATEGORIAS.find((c) => c.id === abierta);

  return (
    <div className="flex flex-col gap-4">
      {/* Tarjetas generales */}
      <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-5 gap-3">
        {CATEGORIAS.map(({ id, label, icon, key, cls }) => (
          <MetricCard
            key={id}
            label={label}
            value={stats[key]}
            cls={cls}
            icon={icon}
            activa={abierta === id}
            onClick={() => toggle(id)}
          />
        ))}
      </div>

      {/* Panel de detalle — aparece bajo las tarjetas */}
      {catActiva && (
        <div className="bg-card border border-border rounded-xl overflow-hidden">
          {/* Header del panel de detalle */}
          <div className="flex items-center justify-between px-4 py-2.5 border-b border-border">
            <div className="flex items-center gap-2">
              <catActiva.icon className="w-3.5 h-3.5 text-primary" />
              <span className="font-bold text-xs text-card-foreground uppercase tracking-wide">
                Detalle — {catActiva.label}
              </span>
            </div>
            <button
              type="button"
              onClick={() => setAbierta(null)}
              className="p-1 rounded hover:bg-muted transition-colors text-muted-foreground"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
          <div className="p-4">
            <catActiva.Detalle stats={stats} />
          </div>
        </div>
      )}
    </div>
  );
}


export function EstadisticasForm() {
  const router = useRouter();

  const [checking, setChecking]       = useState(true);
  const [semestres, setSemestres]     = useState([]);
  const [semSel, setSemSel]           = useState(null);
  const [stats, setStats]             = useState(null);
  const [cargando, setCargando]       = useState(false);
  const [descargando, setDescargando] = useState(false);
  const [error, setError]             = useState("");

  // rango libre
  const [modoRango, setModoRango]     = useState(false);
  const [fechaInicio, setFechaInicio] = useState("2024-01-01");
  const [fechaFin, setFechaFin]       = useState(hoyStr());

  useEffect(() => {
    async function init() {
      try {
        const res = await fetch(`${API_URL_BASE}/auth/me`, { credentials: "include" });
        if (res.status === 401) { router.replace("/"); return; }
        const user = await res.json();
        if (!tienePermiso(user, PERMISOS.VER_REPORTES)) { router.replace("/inicio"); return; }

        const resSem = await fetch(`${API_URL_BASE}/estadisticas/semestres`, { credentials: "include" });
        if (resSem.ok) {
          const lista = await resSem.json();
          setSemestres(lista);
          const actual = calcularSemestreActual();
          const found  = lista.find((s) => s.año === actual.año && s.semestre === actual.semestre);
          setSemSel(found ? found.etiqueta : lista[lista.length - 1]?.etiqueta ?? null);
        }
      } catch (err) {
        console.error(err);
        setError("Error al cargar la página.");
      } finally {
        setChecking(false);
      }
    }
    init();
  }, [router]);

  
  const cargarPorSemestre = useCallback(async () => {
    if (!semSel || modoRango) return;
    const sem = semestres.find((s) => s.etiqueta === semSel);
    if (!sem) return;
    setCargando(true); setError("");
    try {
      const res = await fetch(
        `${API_URL_BASE}/estadisticas/${sem.año}/semestre/${sem.semestre}`,
        { credentials: "include" }
      );
      if (res.status === 403) { setError("Sin permiso."); setStats(null); return; }
      if (!res.ok) { setError("Error al cargar estadísticas."); setStats(null); return; }
      setStats(await res.json());
    } catch { setError("Error de conexión."); }
    finally { setCargando(false); }
  }, [semSel, semestres, modoRango]);

  
  const cargarPorRango = useCallback(async () => {
    if (!modoRango) return;
    if (!fechaInicio || !fechaFin) { setError("Selecciona fechas de inicio y fin."); return; }
    if (fechaInicio > fechaFin)    { setError("La fecha de inicio no puede ser posterior a la de fin."); return; }
    setCargando(true); setError("");
    try {
      const res = await fetch(
        `${API_URL_BASE}/estadisticas/reporte?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`,
        { credentials: "include" }
      );
      if (res.status === 403) { setError("Sin permiso."); setStats(null); return; }
      if (!res.ok) {
        const txt = await res.text();
        setError(txt || "Error al cargar estadísticas."); setStats(null); return;
      }
      setStats(await res.json());
    } catch { setError("Error de conexión."); }
    finally { setCargando(false); }
  }, [modoRango, fechaInicio, fechaFin]);

  useEffect(() => { if (!modoRango) cargarPorSemestre(); }, [cargarPorSemestre, modoRango]);

  
  async function descargarPDF() {
    setDescargando(true);
    try {
      let url, filename;
      if (modoRango) {
        url      = `${API_URL_BASE}/estadisticas/reporte/pdf?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`;
        filename = `reporte-${fechaInicio}-${fechaFin}.pdf`;
      } else {
        const sem = semestres.find((s) => s.etiqueta === semSel);
        if (!sem) return;
        url      = `${API_URL_BASE}/estadisticas/${sem.año}/semestre/${sem.semestre}/pdf`;
        filename = `estadisticas-${sem.año}-s${sem.semestre}.pdf`;
      }
      const res = await fetch(url, { credentials: "include" });
      if (!res.ok) { alert("El PDF no está disponible aún."); return; }
      const blob = await res.blob();
      const a    = document.createElement("a");
      a.href     = URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      URL.revokeObjectURL(a.href);
    } catch { alert("Error al descargar el PDF."); }
    finally { setDescargando(false); }
  }

  
  if (checking)
    return <div className="p-8 text-muted-foreground animate-pulse text-sm">Cargando...</div>;

  const semActual = semestres.find((s) => s.etiqueta === semSel);
  const puedeDescargar = !descargando && !!stats;

  return (
    <div className="p-4 lg:p-6 flex flex-col gap-4 bg-background">

      {/* Encabezado */}
      <div className="flex flex-wrap justify-between items-center gap-3">
        <div>
          <div className="flex items-center gap-2">
            <FileBarChart2 className="w-4 h-4 text-primary" />
            <h1 className="text-lg font-extrabold text-foreground leading-none">Estadísticas</h1>
          </div>
          <p className="text-[11px] text-muted-foreground mt-0.5">
            {modoRango
              ? (fechaInicio && fechaFin ? `${fechaInicio} → ${fechaFin}` : "Rango personalizado")
              : (semActual ? `${semActual.periodoInicio} → ${semActual.periodoFin}` : "")}
          </p>
        </div>
        <Button onClick={descargarPDF} disabled={!puedeDescargar} className="h-8 gap-1.5 text-xs px-3">
          <Download className="w-3.5 h-3.5" />
          {descargando ? "Descargando…" : "Exportar PDF"}
        </Button>
      </div>

      {/* Selector de modo */}
      <Tabs
        value={modoRango ? "rango" : "semestre"}
        onValueChange={(v) => { setModoRango(v === "rango"); setStats(null); setError(""); }}
      >
        <TabsList className="h-8">
          <TabsTrigger key="semestre" value="semestre" className="text-xs h-7">Por semestre</TabsTrigger>
          <TabsTrigger key="rango" value="rango" className="text-xs h-7">
            <Calendar className="w-3 h-3 mr-1" />Rango personalizado
          </TabsTrigger>
        </TabsList>

        <TabsContent key="semestre" value="semestre" className="mt-3">
          <div className="flex items-center gap-2">
            <Select value={semSel || ""} onValueChange={(v) => { setSemSel(v); setStats(null); }}
              disabled={cargando || semestres.length === 0}>
              <SelectTrigger className="w-36 h-8 text-xs">
                <SelectValue placeholder="Semestre" />
              </SelectTrigger>
              <SelectContent>
                {semestres.map((s) => (
                  <SelectItem key={s.etiqueta} value={s.etiqueta} className="text-xs">{s.etiqueta}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button variant="outline" size="icon" className="h-8 w-8"
              onClick={cargarPorSemestre} disabled={cargando} title="Actualizar">
              <RefreshCw className={`w-3.5 h-3.5 ${cargando ? "animate-spin" : ""}`} />
            </Button>
          </div>
        </TabsContent>

        <TabsContent key="rango" value="rango" className="mt-3">
          <div className="flex items-center gap-2 flex-wrap">
            <div className="flex items-center gap-1.5">
              <label className="text-xs text-muted-foreground shrink-0">Desde</label>
              <input type="date" value={fechaInicio} min="2024-01-01" max={hoyStr()}
                onChange={(e) => { setFechaInicio(e.target.value); setStats(null); }}
                className="h-8 rounded-md border border-input bg-background px-2 text-xs outline-none focus:ring-1 focus:ring-ring" />
            </div>
            <div className="flex items-center gap-1.5">
              <label className="text-xs text-muted-foreground shrink-0">Hasta</label>
              <input type="date" value={fechaFin} min="2024-01-01" max={hoyStr()}
                onChange={(e) => { setFechaFin(e.target.value); setStats(null); }}
                className="h-8 rounded-md border border-input bg-background px-2 text-xs outline-none focus:ring-1 focus:ring-ring" />
            </div>
            <Button className="h-8 text-xs px-3" onClick={cargarPorRango} disabled={cargando}>
              {cargando ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : "Consultar"}
            </Button>
          </div>
        </TabsContent>
      </Tabs>

      {/* Error */}
      {error && (
        <div className="rounded-lg border border-primary/30 bg-primary/10 px-3 py-2 text-primary text-xs">
          {error}
        </div>
      )}

      {/* Skeleton */}
      {cargando && (
        <div className="flex flex-col gap-3">
          <div className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-5 gap-3">
            {[1,2,3,4,5].map((i) => <Skeleton key={i} className="h-20" />)}
          </div>
          <Skeleton className="h-40" />
        </div>
      )}

      {/* Contenido */}
      {!cargando && stats && <EstadisticasContenido stats={stats} />}

      {/* Sin datos */}
      {!cargando && !stats && !error && (
        <div className="text-center py-12 text-muted-foreground">
          <FileBarChart2 className="w-10 h-10 mx-auto mb-2 opacity-25" />
          <p className="text-xs">
            {modoRango
              ? "Selecciona un rango de fechas y haz clic en Consultar."
              : "Selecciona un semestre para ver las estadísticas."}
          </p>
        </div>
      )}
    </div>
  );
}
