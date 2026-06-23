"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  CalendarClock,
  CheckCircle2,
  Eye,
  RefreshCw,
  Search,
  ShieldAlert,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import Pagination from "@/components/ui/Pagination";
import { API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import { esEstudiante, tienePermiso } from "@/lib/authz";

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];
const ESTADOS_FINALES = ["COMPLETO_CONCILIADO", "COMPLETO_NO_CONCILIADO"];

const FORM_INICIAL = {
  fechaReunion: "",
  sedeId: "",
  observaciones: "",
};

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

  const claves = ["content", "data", "items", "rows", "sedes", "conciliaciones", "resultado", "payload"];

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

function ordenarPorIdAsc(items) {
  return [...items].sort((a, b) => {
    const idA = Number(a?.id ?? a?.conciliacionId ?? Number.MAX_SAFE_INTEGER);
    const idB = Number(b?.id ?? b?.conciliacionId ?? Number.MAX_SAFE_INTEGER);
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

function estadoFinalizado(estadoCodigo) {
  return ESTADOS_FINALES.includes(normalizarTexto(estadoCodigo));
}

function badgeEstadoClass(codigo) {
  const estado = normalizarTexto(codigo);

  if (estado.includes("COMPLETO")) return "border-emerald-200 bg-emerald-50 text-emerald-700";
  if (estado === "EN_ESPERA") return "border-amber-200 bg-amber-50 text-amber-700";
  if (estado === "REUNION_PROGRAMADA") return "border-blue-200 bg-blue-50 text-blue-700";

  return "border-slate-200 bg-slate-50 text-slate-700";
}

function etiquetaEstado(codigo, nombre) {
  if (nombre) return nombre;

  const estado = normalizarTexto(codigo).replace(/_/g, " ");
  if (!estado) return "Sin estado";

  return estado.charAt(0) + estado.slice(1).toLowerCase();
}

function toDatetimeLocal(value) {
  if (!value) return "";

  const texto = String(value);
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(texto)) {
    return texto.slice(0, 16);
  }

  const fecha = new Date(value);
  if (Number.isNaN(fecha.getTime())) return "";

  const pad = (n) => String(n).padStart(2, "0");
  return `${fecha.getFullYear()}-${pad(fecha.getMonth() + 1)}-${pad(fecha.getDate())}T${pad(fecha.getHours())}:${pad(fecha.getMinutes())}`;
}

function normalizarFechaRequest(value) {
  if (!value) return "";
  return String(value).length === 16 ? `${value}:00` : String(value);
}

function fechaEsFutura(value) {
  if (!value) return false;
  const fecha = new Date(value);
  return !Number.isNaN(fecha.getTime()) && fecha.getTime() > Date.now();
}

function nombreSede(sede) {
  return sede?.nombre || sede?.displayName || sede?.descripcion || `Sede #${sede?.id}`;
}

export function ReunionesConciliacionForm() {
  const router = useRouter();

  const [usuario, setUsuario] = useState(null);
  const [conciliaciones, setConciliaciones] = useState([]);
  const [sedes, setSedes] = useState([]);
  const [detalle, setDetalle] = useState(null);

  const [loading, setLoading] = useState(true);
  const [loadingDetalle, setLoadingDetalle] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [form, setForm] = useState(FORM_INICIAL);

  const puedeVer = usuario && tienePermiso(usuario, PERMISOS.VER_CONCILIACIONES);
  const puedeProgramar = usuario && tienePermiso(usuario, PERMISOS.PROGRAMAR_REUNIONES_CONCILIACION) && !esEstudiante(usuario);
  const puedeReprogramar = usuario && tienePermiso(usuario, PERMISOS.REPROGRAMAR_REUNIONES_CONCILIACION) && !esEstudiante(usuario);

  const reunionActual = detalle?.reunion || null;
  const conciliacionActiva = detalle ? detalle.activo !== false : false;
  const conciliacionFinalizada = estadoFinalizado(detalle?.estadoCodigo);

  const puedeGuardarReunion = Boolean(
    detalle?.id &&
      conciliacionActiva &&
      !conciliacionFinalizada &&
      ((!reunionActual && puedeProgramar) || (reunionActual && puedeReprogramar))
  );

  useEffect(() => {
    inicializar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!detalle) {
      setForm(FORM_INICIAL);
      return;
    }

    if (detalle.reunion) {
      setForm({
        fechaReunion: toDatetimeLocal(detalle.reunion.fechaReunion),
        sedeId: detalle.reunion.sedeId ? String(detalle.reunion.sedeId) : "",
        observaciones: detalle.reunion.observaciones || "",
      });
      return;
    }

    setForm(FORM_INICIAL);
  }, [detalle]);

  async function apiFetch(path, options = {}, fallback = "Ocurrió un error") {
    const response = await fetch(`${API_URL_BASE}${path}`, {
      credentials: "include",
      ...options,
      headers: {
        ...(options.headers || {}),
      },
    });

    const data = await leerRespuesta(response);

    if (response.status === 401) {
      router.replace("/");
      throw new Error("Sesión expirada. Inicia sesión nuevamente.");
    }

    if (!response.ok) {
      throw new Error(obtenerMensajeError(data, fallback));
    }

    return data;
  }

  async function inicializar() {
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
      await Promise.all([cargarConciliaciones(), cargarSedes()]);
    } catch (err) {
      console.error(err);
      setError(err.message || "No se pudo cargar la información de reuniones");
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

  async function cargarSedes() {
    try {
      const data = await apiFetch("/sedes", { method: "GET" }, "No se pudieron cargar las sedes");
      setSedes(ordenarPorIdAsc(extraerLista(data)));
    } catch (err) {
      console.warn(err);
      setSedes([]);
    }
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
    } catch (err) {
      console.error(err);
      setError(err.message || "No se pudo cargar el detalle de la conciliación");
    } finally {
      setLoadingDetalle(false);
    }
  }

  function handleFormChange(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function validarFormulario() {
    if (!form.fechaReunion) return "Selecciona la fecha y hora de la reunión.";
    if (!fechaEsFutura(form.fechaReunion)) return "La fecha de la reunión debe ser posterior al momento actual.";
    if (!form.sedeId) return "Selecciona la sede de la reunión.";
    if ((form.observaciones || "").trim().length > 300) return "Las observaciones no pueden superar 300 caracteres.";
    return "";
  }

  async function guardarReunion(event) {
    event.preventDefault();
    if (!detalle?.id || !puedeGuardarReunion) return;

    const mensajeValidacion = validarFormulario();
    if (mensajeValidacion) {
      setError(mensajeValidacion);
      return;
    }

    const payload = {
      fechaReunion: normalizarFechaRequest(form.fechaReunion),
      sedeId: Number(form.sedeId),
      observaciones: form.observaciones?.trim() || null,
    };

    try {
      setSaving(true);
      setError("");
      setMensaje("");

      await apiFetch(
        `/conciliaciones/${detalle.id}/reunion`,
        {
          method: reunionActual ? "PUT" : "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        },
        reunionActual ? "No fue posible reprogramar la reunión." : "No fue posible programar la reunión."
      );

      await refrescar(reunionActual ? "La reunión fue reprogramada correctamente." : "La reunión fue programada correctamente.");
    } catch (err) {
      console.error(err);
      setError(err.message || "No se pudo guardar la reunión");
      toast.error(err.message || "No se pudo guardar la reunión");
    } finally {
      setSaving(false);
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
        item?.estadoCodigo,
        item?.estadoNombre,
        item?.estudianteNombre,
        item?.conciliadorNombre,
        item?.reunion?.fechaReunion,
        item?.reunion?.sedeNombre,
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
    return <div className="py-10 text-center text-sm text-muted-foreground">Cargando reuniones de conciliación...</div>;
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
            <h2 className="text-2xl font-bold tracking-tight">Reuniones de conciliación</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Programa o reprograma la reunión desde el detalle de una conciliación visible para tu usuario.
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

      <section className="rounded-2xl border bg-card p-5 shadow-sm">
        <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h3 className="font-semibold">Seleccionar conciliación</h3>
          </div>

          <div className="relative w-full lg:w-80">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setCurrentPage(1);
              }}
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
                <th className="px-4 py-3">Reunión</th>
                <th className="px-4 py-3">Sede</th>
                <th className="px-4 py-3 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {conciliacionesPagina.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                    No hay conciliaciones para mostrar.
                  </td>
                </tr>
              ) : (
                conciliacionesPagina.map((item) => {
                  const seleccionada = String(detalle?.id || "") === String(item.id);
                  const reunion = item.reunion || null;

                  return (
                    <tr key={item.id} className={seleccionada ? "bg-primary/5" : "hover:bg-muted/30"}>
                      <td className="px-4 py-3 font-medium">#{item.id}</td>
                      <td className="px-4 py-3">#{item.consultaId || "-"}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${badgeEstadoClass(item.estadoCodigo)}`}>
                          {etiquetaEstado(item.estadoCodigo, item.estadoNombre)}
                        </span>
                      </td>
                      <td className="px-4 py-3">{formatearFecha(reunion?.fechaReunion)}</td>
                      <td className="px-4 py-3">{reunion?.sedeNombre || "No registra"}</td>
                      <td className="px-4 py-3 text-right">
                        <Button type="button" size="sm" onClick={() => cargarDetalle(item.id)}>
                          <Eye className="mr-2 h-4 w-4" />
                          Ver reunión
                        </Button>
                      </td>
                    </tr>
                  );
                })
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
              <h3 className="text-xl font-semibold">Reunión de conciliación #{detalle.id}</h3>
              <p className="mt-1 text-sm text-muted-foreground">
                Consulta #{detalle.consultaId} · {detalle.estudianteNombre || "Sin estudiante"} · {detalle.conciliadorNombre || "Sin conciliador"}
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
            <div className="grid grid-cols-1 gap-5 xl:grid-cols-[1fr_1fr]">
              <div className="space-y-4">
                <div className="rounded-xl border bg-background p-4">
                  <div className="mb-3 flex items-center gap-2">
                    <CalendarClock className="h-5 w-5 text-primary" />
                    <h4 className="font-semibold">Reunión vigente</h4>
                  </div>

                  {reunionActual ? (
                    <div className="grid grid-cols-1 gap-3 text-sm md:grid-cols-2">
                      <InfoItem label="Fecha y hora" value={formatearFecha(reunionActual.fechaReunion)} />
                      <InfoItem label="Sede" value={reunionActual.sedeNombre || reunionActual.sedeId || "No registra"} />
                      <InfoItem label="Creación" value={formatearFecha(reunionActual.fechaCreacion)} />
                      <InfoItem label="Última actualización" value={formatearFecha(reunionActual.fechaActualizacion)} />
                      <div className="md:col-span-2">
                        <InfoItem label="Observaciones" value={reunionActual.observaciones || "No registra"} />
                      </div>
                    </div>
                  ) : (
                    <div className="rounded-lg border border-dashed p-5 text-sm text-muted-foreground">
                      Esta conciliación aún no tiene reunión programada.
                    </div>
                  )}
                </div>

              </div>

              <div className="rounded-xl border bg-background p-4">
                <div className="mb-3">
                  <h4 className="font-semibold">{reunionActual ? "Reprogramar reunión" : "Programar reunión"}</h4>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Fecha futura, sede activa y observaciones opcionales de máximo 300 caracteres.
                  </p>
                </div>

                {conciliacionFinalizada || !conciliacionActiva ? (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                    No se pueden programar reuniones sobre conciliaciones finalizadas o inactivas.
                  </div>
                ) : puedeGuardarReunion ? (
                  <form onSubmit={guardarReunion} className="space-y-4">
                    <div className="space-y-1.5">
                      <label className="text-sm font-medium">Fecha y hora de reunión *</label>
                      <input
                        type="datetime-local"
                        name="fechaReunion"
                        value={form.fechaReunion}
                        onChange={handleFormChange}
                        className="h-10 w-full rounded-md border bg-background px-3 text-sm"
                        required
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-sm font-medium">Sede *</label>
                      <select
                        name="sedeId"
                        value={form.sedeId}
                        onChange={handleFormChange}
                        className="h-10 w-full rounded-md border bg-background px-3 text-sm"
                        required
                      >
                        <option value="">Selecciona una sede</option>
                        {sedes.map((sede) => (
                          <option key={sede.id} value={sede.id}>
                            {nombreSede(sede)}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div className="space-y-1.5">
                      <div className="flex items-center justify-between gap-3">
                        <label className="text-sm font-medium">Observaciones</label>
                        <span className={`text-xs ${(form.observaciones || "").length > 300 ? "text-destructive" : "text-muted-foreground"}`}>
                          {(form.observaciones || "").length}/300
                        </span>
                      </div>
                      <textarea
                        name="observaciones"
                        value={form.observaciones}
                        onChange={handleFormChange}
                        maxLength={300}
                        rows={4}
                        className="w-full rounded-md border bg-background px-3 py-2 text-sm"
                        placeholder="Observaciones opcionales para la reunión..."
                      />
                    </div>

                    <Button type="submit" disabled={saving || sedes.length === 0}>
                      <CheckCircle2 className="mr-2 h-4 w-4" />
                      {saving ? "Guardando..." : reunionActual ? "Reprogramar reunión" : "Programar reunión"}
                    </Button>

                    {sedes.length === 0 && (
                      <p className="text-sm text-muted-foreground">
                        No se cargaron sedes activas. Revisa permisos de catálogos o el endpoint de sedes.
                      </p>
                    )}
                  </form>
                ) : (
                  <div className="rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground">
                    No tienes permisos para {reunionActual ? "reprogramar" : "programar"} esta reunión. Si el botón aparece para otro rol, el backend igualmente valida alcance.
                  </div>
                )}
              </div>
            </div>
          )}
        </section>
      )}
    </div>
  );
}

function InfoItem({ label, value }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase text-muted-foreground">{label}</p>
      <p className="mt-1 font-medium">{value}</p>
    </div>
  );
}

export default ReunionesConciliacionForm;
