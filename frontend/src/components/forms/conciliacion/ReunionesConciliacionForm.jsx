"use client";

import { apiClient } from "@/lib/apiClient";
import { useEffect, useRef, useState } from "react";
import { useDebouncedPageSearch } from "@/hooks/useDebouncedValue";
import { fetchPaged, isAbortError } from "@/lib/pagedApi";
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
import {
  isConcurrencyConflict,
  requireResourceVersion,
  withErrorReference,
} from "@/lib/api";
import { PERMISOS } from "@/lib/permission";
import { esEstudiante, tienePermiso } from "@/lib/authz";
import { requestConciliacion } from "./conciliaciones.service";

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];
const ESTADOS_FINALES = ["COMPLETO_CONCILIADO", "COMPLETO_NO_CONCILIADO"];
const ESTADOS_REUNIONES = [
  { value: "REUNION_PROGRAMADA", label: "Reunión programada" },
  { value: "COMPLETO_CONCILIADO", label: "Completo conciliado" },
  { value: "COMPLETO_NO_CONCILIADO", label: "Completo no conciliado" },
];

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
  const savingRef = useRef(false);
  const preserveFormOnDetailRefreshRef = useRef(false);

  const [usuario, setUsuario] = useState(null);
  const [conciliaciones, setConciliaciones] = useState([]);
  const [conciliacionesPendientes, setConciliacionesPendientes] = useState([]);
  const [sedes, setSedes] = useState([]);
  const [detalle, setDetalle] = useState(null);

  const [loading, setLoading] = useState(true);
  const [loadingDetalle, setLoadingDetalle] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const searchAplicado = useDebouncedPageSearch(search, setCurrentPage, 350).trim();
  const [estadoFiltro, setEstadoFiltro] = useState("");
  const [fechaDesde, setFechaDesde] = useState("");
  const [fechaHasta, setFechaHasta] = useState("");
  const [sortBy, setSortBy] = useState("fechaReunion");
  const [direction, setDirection] = useState("desc");
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loadingLista, setLoadingLista] = useState(false);
  const [errorLista, setErrorLista] = useState("");
  const [searchPendientes, setSearchPendientes] = useState("");
  const [currentPagePendientes, setCurrentPagePendientes] = useState(1);
  const searchPendientesAplicado = useDebouncedPageSearch(
    searchPendientes,
    setCurrentPagePendientes,
    350
  ).trim();
  const [sortByPendientes, setSortByPendientes] = useState("id");
  const [directionPendientes, setDirectionPendientes] = useState("desc");
  const [pageSizePendientes, setPageSizePendientes] = useState(10);
  const [totalElementsPendientes, setTotalElementsPendientes] = useState(0);
  const [totalPagesPendientes, setTotalPagesPendientes] = useState(0);
  const [loadingPendientes, setLoadingPendientes] = useState(false);
  const [errorPendientes, setErrorPendientes] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [form, setForm] = useState(FORM_INICIAL);
  const [versionBase, setVersionBase] = useState({ conciliacion: null, reunion: null });

  const puedeVer = usuario && tienePermiso(usuario, PERMISOS.VER_CONCILIACIONES);
  const puedeProgramar = usuario && tienePermiso(usuario, PERMISOS.PROGRAMAR_REUNIONES_CONCILIACION) && !esEstudiante(usuario);
  const puedeReprogramar = usuario && tienePermiso(usuario, PERMISOS.REPROGRAMAR_REUNIONES_CONCILIACION) && !esEstudiante(usuario);

  const reunionActual = detalle?.reunion || null;
  const conciliacionActiva = detalle ? detalle.activo !== false : false;
  const conciliacionFinalizada = estadoFinalizado(detalle?.estadoCodigo);
  const primeraReunionElegible = Boolean(
    !reunionActual &&
      normalizarTexto(detalle?.estadoCodigo) === "ESPERANDO_REUNION" &&
      detalle?.estudianteId &&
      detalle?.conciliadorId
  );

  const puedeGuardarReunion = Boolean(
    detalle?.id &&
      conciliacionActiva &&
      !conciliacionFinalizada &&
      ((!reunionActual && primeraReunionElegible && puedeProgramar) ||
        (reunionActual && puedeReprogramar))
  );

  useEffect(() => {
    inicializar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // biome-ignore lint/correctness/useExhaustiveDependencies: reloadKey refreshes this independent remote list after mutations.
  useEffect(() => {
    if (!usuario || !puedeVer) return undefined;

    const controller = new AbortController();

    async function cargarPagina() {
      setLoadingLista(true);
      setErrorLista("");
      try {
        const page = await fetchPaged("/conciliaciones/reuniones", {
          search: searchAplicado,
          page: currentPage,
          size: pageSize,
          sortBy,
          direction,
          filters: { estado: estadoFiltro, fechaDesde, fechaHasta },
          signal: controller.signal,
          resourceName: "reuniones de conciliación",
        });

        const items = page.content.map((item) => ({
          ...item,
          id: item.conciliacionId,
          reunion: {
            version: item.version,
            fechaReunion: item.fechaReunion,
            sedeId: item.sedeId,
            sedeNombre: item.sedeNombre,
            observaciones: item.observaciones,
            fechaCreacion: item.fechaCreacion,
            fechaActualizacion: item.fechaActualizacion,
          },
        }));

        setConciliaciones(items);
        setTotalElements(page.totalElements);
        setTotalPages(page.totalPages);
        if (page.totalPages > 0 && page.page > page.totalPages) {
          setCurrentPage(page.totalPages);
        } else if (page.page !== currentPage) {
          setCurrentPage(page.page);
        }
      } catch (err) {
        if (isAbortError(err)) return;
        if (err?.status === 401) {
          router.replace("/");
          return;
        }
        setConciliaciones([]);
        setTotalElements(0);
        setTotalPages(0);
        setErrorLista(
          withErrorReference(
            err?.status === 403
              ? "No tienes permiso para consultar reuniones de conciliación."
              : err?.message || "No se pudieron cargar las reuniones de conciliación",
            err?.correlationId || null
          )
        );
      } finally {
        if (!controller.signal.aborted) setLoadingLista(false);
      }
    }

    cargarPagina();
    return () => controller.abort();
  }, [
    usuario,
    puedeVer,
    searchAplicado,
    currentPage,
    pageSize,
    sortBy,
    direction,
    estadoFiltro,
    fechaDesde,
    fechaHasta,
    reloadKey,
    router,
  ]);

  // biome-ignore lint/correctness/useExhaustiveDependencies: reloadKey refreshes this independent remote list after mutations.
  useEffect(() => {
    if (!usuario || !puedeVer) return undefined;

    const controller = new AbortController();

    async function cargarPendientes() {
      setLoadingPendientes(true);
      setErrorPendientes("");
      try {
        const page = await fetchPaged("/conciliaciones", {
          search: searchPendientesAplicado,
          page: currentPagePendientes,
          size: pageSizePendientes,
          sortBy: sortByPendientes,
          direction: directionPendientes,
          filters: { estado: "ESPERANDO_REUNION" },
          signal: controller.signal,
          resourceName: "conciliaciones pendientes de primera reunión",
        });

        setConciliacionesPendientes(page.content);
        setTotalElementsPendientes(page.totalElements);
        setTotalPagesPendientes(page.totalPages);
        if (page.totalPages > 0 && page.page > page.totalPages) {
          setCurrentPagePendientes(page.totalPages);
        } else if (page.page !== currentPagePendientes) {
          setCurrentPagePendientes(page.page);
        }
      } catch (err) {
        if (isAbortError(err)) return;
        if (err?.status === 401) {
          router.replace("/");
          return;
        }
        setConciliacionesPendientes([]);
        setTotalElementsPendientes(0);
        setTotalPagesPendientes(0);
        setErrorPendientes(
          withErrorReference(
            err?.status === 403
              ? "No tienes permiso para consultar conciliaciones pendientes."
              : err?.message || "No se pudieron cargar las conciliaciones pendientes",
            err?.correlationId || null
          )
        );
      } finally {
        if (!controller.signal.aborted) setLoadingPendientes(false);
      }
    }

    cargarPendientes();
    return () => controller.abort();
  }, [
    usuario,
    puedeVer,
    searchPendientesAplicado,
    currentPagePendientes,
    pageSizePendientes,
    sortByPendientes,
    directionPendientes,
    reloadKey,
    router,
  ]);

  useEffect(() => {
    if (!detalle) {
      setForm(FORM_INICIAL);
      return;
    }

    if (preserveFormOnDetailRefreshRef.current) {
      preserveFormOnDetailRefreshRef.current = false;
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
    try {
      return await requestConciliacion(path, options, fallback);
    } catch (error) {
      if (error?.status === 401) {
        router.replace("/");
      }
      throw error;
    }
  }

  async function inicializar() {
    try {
      setLoading(true);
      setError("");
      setMensaje("");

      const meResponse = await apiClient.request(`${API_URL_BASE}/auth/me`, {
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
      await cargarSedes();
    } catch (err) {
      setError(
        withErrorReference(
          err?.message || "No se pudo cargar la información de reuniones",
          err?.correlationId || null
        )
      );
    } finally {
      setLoading(false);
    }
  }

  async function cargarSedes() {
    try {
      const data = await apiFetch("/sedes", { method: "GET" }, "No se pudieron cargar las sedes");
      setSedes(ordenarPorIdAsc(extraerLista(data)));
    } catch {
      setSedes([]);
    }
  }

  async function refrescar(mensajeOk = "Información actualizada") {
    setReloadKey((value) => value + 1);
    if (detalle?.id) {
      await cargarDetalle(detalle.id, { silencioso: true });
    }
    setMensaje(mensajeOk);
    toast.success(mensajeOk);
  }

  function aplicarDetalle(data) {
    setDetalle(data);
    setConciliaciones((items) =>
      items.map((item) =>
        String(item.id) === String(data?.id)
          ? {
              ...item,
              version: data.version,
              estadoCodigo: data.estadoCodigo,
              estadoNombre: data.estadoNombre,
              activo: data.activo,
              reunion: data.reunion || item.reunion,
            }
          : item
      )
    );
    setConciliacionesPendientes((items) =>
      normalizarTexto(data?.estadoCodigo) === "ESPERANDO_REUNION" && !data?.reunion
        ? items.map((item) =>
            String(item.id) === String(data?.id)
              ? {
                  ...item,
                  version: data.version,
                  estadoCodigo: data.estadoCodigo,
                  estadoNombre: data.estadoNombre,
                  activo: data.activo,
                }
              : item
          )
        : items.filter((item) => String(item.id) !== String(data?.id))
    );
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

      aplicarDetalle(data);
      setVersionBase({
        conciliacion: data?.version ?? null,
        reunion: data?.reunion?.version ?? null,
      });
    } catch (err) {
      setError(
        withErrorReference(
          err?.message || "No se pudo cargar el detalle de la conciliación",
          err?.correlationId || null
        )
      );
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
    if (!detalle?.id || !puedeGuardarReunion || savingRef.current) return;

    const mensajeValidacion = validarFormulario();
    if (mensajeValidacion) {
      setError(mensajeValidacion);
      return;
    }

    savingRef.current = true;
    try {
      setSaving(true);
      setError("");
      setMensaje("");

      const payload = {
        fechaReunion: normalizarFechaRequest(form.fechaReunion),
        sedeId: Number(form.sedeId),
        observaciones: form.observaciones?.trim() || null,
        conciliacionVersion:
          versionBase.conciliacion ?? requireResourceVersion(detalle, "conciliación"),
        ...(reunionActual
          ? {
              version:
                versionBase.reunion ??
                requireResourceVersion(reunionActual, "reunión de conciliación"),
            }
          : {}),
      };

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
      if (isConcurrencyConflict(err) && detalle?.id) {
        setError(
          withErrorReference(
            "La conciliación o su reunión cambió mientras editabas. El formulario se conserva.",
            err?.correlationId || null
          )
        );
        toast.error("La reunión cambió mientras la estabas editando", {
          description: withErrorReference(
            "No hubo reintento automático. Se cargará la versión actual sin borrar tus campos.",
            err?.correlationId || null
          ),
        });
        try {
          const latest = await apiFetch(
            `/conciliaciones/${detalle.id}`,
            { method: "GET" },
            "No se pudo cargar la versión actual de la conciliación"
          );
          setVersionBase({
            conciliacion: latest?.version ?? versionBase.conciliacion,
            reunion: latest?.reunion?.version ?? versionBase.reunion,
          });
          preserveFormOnDetailRefreshRef.current = true;
          aplicarDetalle(latest);
          setReloadKey((value) => value + 1);
          setError(
            `La versión actual del servidor se cargó sin reemplazar tus campos. ` +
              `Reunión actual: ${formatearFecha(latest?.reunion?.fechaReunion)}.`
          );
        } catch {
          // Keep the form untouched when the current server version cannot be refreshed.
        }
        return;
      }

      const message = withErrorReference(
        err?.message || "No se pudo guardar la reunión",
        err?.correlationId || null
      );
      setError(message);
      toast.error("No se pudo guardar la reunión", {
        description: message,
      });
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  }

  function handlePageSizeChange(nextSize) {
    setPageSize(nextSize);
    setCurrentPage(1);
  }

  function handlePendingPageSizeChange(nextSize) {
    setPageSizePendientes(nextSize);
    setCurrentPagePendientes(1);
  }

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
            <h3 className="font-semibold">Primeras reuniones pendientes</h3>
            <p className="mt-1 text-sm text-muted-foreground">
              Conciliaciones con estudiante y conciliador asignados que aún no tienen reunión.
            </p>
          </div>

          <div className="relative w-full lg:max-w-md">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <input
              value={searchPendientes}
              onChange={(event) => setSearchPendientes(event.target.value)}
              placeholder="Buscar conciliaciones pendientes"
              className="h-10 w-full rounded-md border bg-background pl-9 pr-3 text-sm"
            />
          </div>
        </div>

        <div className="mb-3 flex flex-wrap items-center gap-2 text-sm">
          <span className="text-muted-foreground">Orden:</span>
          <select
            aria-label="Ordenar conciliaciones pendientes por"
            value={sortByPendientes}
            onChange={(event) => {
              setSortByPendientes(event.target.value);
              setCurrentPagePendientes(1);
            }}
            className="h-8 rounded-md border bg-background px-2"
          >
            <option value="id">ID</option>
            <option value="fechaCreacion">Fecha de creación</option>
            <option value="fechaConciliacion">Fecha de conciliación</option>
            <option value="estado">Estado</option>
          </select>
          <select
            aria-label="Dirección de conciliaciones pendientes"
            value={directionPendientes}
            onChange={(event) => {
              setDirectionPendientes(event.target.value);
              setCurrentPagePendientes(1);
            }}
            className="h-8 rounded-md border bg-background px-2"
          >
            <option value="desc">Descendente</option>
            <option value="asc">Ascendente</option>
          </select>
        </div>

        {errorPendientes && (
          <div className="mb-4 rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {errorPendientes}
          </div>
        )}

        <div className="overflow-x-auto rounded-xl border">
          <table className="min-w-full divide-y text-sm">
            <thead className="bg-muted/50 text-left text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">Consulta</th>
                <th className="px-4 py-3">Estado</th>
                <th className="px-4 py-3">Estudiante</th>
                <th className="px-4 py-3">Conciliador</th>
                <th className="px-4 py-3 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {conciliacionesPendientes.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                    {loadingPendientes
                      ? "Cargando conciliaciones pendientes..."
                      : "No hay primeras reuniones pendientes para mostrar."}
                  </td>
                </tr>
              ) : (
                conciliacionesPendientes.map((item) => {
                  const seleccionada = String(detalle?.id || "") === String(item.id);
                  const elegible = Boolean(
                    normalizarTexto(item.estadoCodigo) === "ESPERANDO_REUNION" &&
                      item.estudianteId &&
                      item.conciliadorId
                  );

                  return (
                    <tr key={item.id} className={seleccionada ? "bg-primary/5" : "hover:bg-muted/30"}>
                      <td className="px-4 py-3 font-medium">#{item.id}</td>
                      <td className="px-4 py-3">#{item.consultaId || "-"}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${badgeEstadoClass(item.estadoCodigo)}`}>
                          {etiquetaEstado(item.estadoCodigo, item.estadoNombre)}
                        </span>
                      </td>
                      <td className="px-4 py-3">{item.estudianteNombre || "Sin estudiante"}</td>
                      <td className="px-4 py-3">{item.conciliadorNombre || "Sin conciliador"}</td>
                      <td className="px-4 py-3 text-right">
                        <Button
                          type="button"
                          size="sm"
                          disabled={!elegible}
                          onClick={() => cargarDetalle(item.id)}
                        >
                          <Eye className="mr-2 h-4 w-4" />
                          {elegible ? "Ver y programar" : "No elegible"}
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
          currentPage={currentPagePendientes}
          totalPages={totalPagesPendientes}
          pageSize={pageSizePendientes}
          onPageChange={setCurrentPagePendientes}
          onPageSizeChange={handlePendingPageSizeChange}
          pageSizeOptions={PAGE_SIZE_OPTIONS}
          totalItems={totalElementsPendientes}
        />
      </section>

      <section className="rounded-2xl border bg-card p-5 shadow-sm">
        <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h3 className="font-semibold">Reuniones existentes</h3>
            <p className="mt-1 text-sm text-muted-foreground">
              Consulta o reprograma reuniones ya registradas.
            </p>
          </div>

          <div className="grid w-full gap-2 lg:max-w-4xl lg:grid-cols-5">
            <div className="relative lg:col-span-2">
              <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
              <input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Buscar reuniones existentes"
                className="h-10 w-full rounded-md border bg-background pl-9 pr-3 text-sm"
              />
            </div>
            <select
              aria-label="Estado de reuniones existentes"
              value={estadoFiltro}
              onChange={(event) => {
                setEstadoFiltro(event.target.value);
                setCurrentPage(1);
              }}
              className="h-10 rounded-md border bg-background px-2 text-sm"
            >
              <option value="">Todos los estados</option>
              {ESTADOS_REUNIONES.map((estado) => (
                <option key={estado.value} value={estado.value}>{estado.label}</option>
              ))}
            </select>
            <input
              type="date"
              value={fechaDesde}
              onChange={(event) => {
                setFechaDesde(event.target.value);
                setCurrentPage(1);
              }}
              className="h-10 rounded-md border bg-background px-2 text-sm"
              aria-label="Fecha de reunión desde"
            />
            <input
              type="date"
              value={fechaHasta}
              onChange={(event) => {
                setFechaHasta(event.target.value);
                setCurrentPage(1);
              }}
              className="h-10 rounded-md border bg-background px-2 text-sm"
              aria-label="Fecha de reunión hasta"
            />
          </div>
        </div>

        <div className="mb-3 flex flex-wrap items-center gap-2 text-sm">
          <span className="text-muted-foreground">Orden:</span>
          <select
            aria-label="Ordenar reuniones existentes por"
            value={sortBy}
            onChange={(event) => {
              setSortBy(event.target.value);
              setCurrentPage(1);
            }}
            className="h-8 rounded-md border bg-background px-2"
          >
            <option value="fechaReunion">Fecha de reunión</option>
            <option value="id">ID</option>
            <option value="fechaCreacion">Fecha de creación</option>
            <option value="estado">Estado</option>
            <option value="sede">Sede</option>
          </select>
          <select
            aria-label="Dirección de reuniones existentes"
            value={direction}
            onChange={(event) => {
              setDirection(event.target.value);
              setCurrentPage(1);
            }}
            className="h-8 rounded-md border bg-background px-2"
          >
            <option value="desc">Descendente</option>
            <option value="asc">Ascendente</option>
          </select>
        </div>

        {errorLista && (
          <div className="mb-4 rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {errorLista}
          </div>
        )}

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
              {conciliaciones.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">
                    {loadingLista ? "Cargando reuniones..." : "No hay reuniones para mostrar."}
                  </td>
                </tr>
              ) : (
                conciliaciones.map((item) => {
                  const seleccionada = String(detalle?.id || "") === String(item.id);
                  const reunion = item.reunion;

                  return (
                    <tr key={item.id} className={seleccionada ? "bg-primary/5" : "hover:bg-muted/30"}>
                      <td className="px-4 py-3 font-medium">#{item.id}</td>
                      <td className="px-4 py-3">#{item.consultaId || "-"}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${badgeEstadoClass(item.estadoCodigo)}`}>
                          {etiquetaEstado(item.estadoCodigo, item.estadoNombre)}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        {formatearFecha(reunion?.fechaReunion)}
                      </td>
                      <td className="px-4 py-3">
                        {reunion?.sedeNombre || "No registra"}
                      </td>
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
          totalItems={totalElements}
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
                ) : !reunionActual && !primeraReunionElegible ? (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
                    Esta conciliación todavía no reúne el estado y los responsables requeridos para programar su primera reunión.
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
