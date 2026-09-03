"use client";

import { apiClient } from "@/lib/apiClient";
import { fileApi } from "@/lib/fileApi";
import { useEffect, useMemo, useState } from "react";
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
import { API_URL_BASE } from "@/lib/config";
import {
  isConcurrencyConflict,
  requireResourceVersion,
  withErrorReference,
} from "@/lib/api";
import { PERMISOS } from "@/lib/permission";
import {
  esConciliador,
  esEstudiante,
  tieneAlgunPermiso,
  tienePermiso,
} from "@/lib/authz";

import { ESTADOS_FINALES, ESTADOS_NO_FINALES, PAGE_SIZE_OPTIONS } from "./conciliaciones.constants";
import {
  archivoEsPdf,
  badgeEstadoClass,
  etiquetaEstado,
  extraerLista,
  formatearFecha,
  leerRespuesta,
  idConsulta,
  nombreConsulta,
  nombrePersona,
  normalizarTexto,
  ordenarPorIdAsc,
} from "./conciliaciones.utils";
import { esRolAdministrador } from "./conciliaciones.permissions";
import { ActionCard, CampoConsulta, InfoCard, PersonasCard } from "./ConciliacionesFormParts";
import { requestConciliacion } from "./conciliaciones.service";

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

  const conciliacionesActivasNoFinalizadasPorConsulta = useMemo(() => {
    const ids = new Set();

    conciliaciones.forEach((item) => {
      const estado = normalizarTexto(item?.estadoCodigo || item?.estadoNombre);
      const activa = item?.activo !== false;
      const finalizada = estado === "COMPLETO_CONCILIADO" || estado === "COMPLETO_NO_CONCILIADO";

      if (activa && !finalizada && item?.consultaId) {
        ids.add(String(item.consultaId));
      }
    });

    return ids;
  }, [conciliaciones]);

  const consultasDisponiblesParaConciliacion = useMemo(() => {
    return consultas.filter((consulta) => {
      const estado = normalizarTexto(consulta?.estado || consulta?.estadoConsulta || consulta?.estadoNombre);
      const consultaId = String(idConsulta(consulta) || "");
      const cerradaOArchivada = ["CERRADO", "CERRADA", "ARCHIVADO", "ARCHIVADA"].includes(estado);

      return Boolean(consultaId)
        && !cerradaOArchivada
        && !conciliacionesActivasNoFinalizadasPorConsulta.has(consultaId);
    });
  }, [consultas, conciliacionesActivasNoFinalizadasPorConsulta]);

  const detalleFinalizado = useMemo(() => {
    const estado = normalizarTexto(detalle?.estadoCodigo || detalle?.estadoNombre);
    return estado === "COMPLETO_CONCILIADO" || estado === "COMPLETO_NO_CONCILIADO";
  }, [detalle]);

  useEffect(() => {
    cargarInicial();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [search, conciliaciones.length, pageSize]);

  async function apiFetch(path, options = {}, fallback = "No se pudo completar la operación") {
    try {
      return await requestConciliacion(path, options, fallback);
    } catch (error) {
      if (error?.status === 401) {
        router.replace("/");
      }
      throw error;
    }
  }

  async function cargarInicial() {
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
      await Promise.all([cargarConciliaciones(), cargarAuxiliares(me)]);
    } catch (err) {
      setError(
        withErrorReference(
          err?.message || "Error cargando conciliaciones",
          err?.correlationId || null
        )
      );
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
      setError(
        withErrorReference(
          err?.message || "Error cargando detalle",
          err?.correlationId || null
        )
      );
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

    const consultaSeleccionada = consultas.find(
      (consulta) => String(idConsulta(consulta)) === String(crearConsultaId)
    );
    const estadoConsulta = normalizarTexto(
      consultaSeleccionada?.estado || consultaSeleccionada?.estadoConsulta || consultaSeleccionada?.estadoNombre
    );

    if (["CERRADO", "CERRADA", "ARCHIVADO", "ARCHIVADA"].includes(estadoConsulta)) {
      setError("No se puede crear una conciliación sobre una consulta cerrada o archivada.");
      return;
    }

    if (conciliacionesActivasNoFinalizadasPorConsulta.has(String(crearConsultaId))) {
      setError("La consulta ya tiene una conciliación activa no finalizada.");
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
      setError(
        withErrorReference(
          err?.message || "Error creando conciliación",
          err?.correlationId || null
        )
      );
    } finally {
      setSaving(false);
    }
  }

  async function asignarEstudiante() {
    if (!detalle?.id) return;
    if (detalleFinalizado) {
      setError("No se puede modificar una conciliación finalizada.");
      return;
    }
    if (!estudianteId) {
      setError("Selecciona un estudiante habilitado para conciliación.");
      return;
    }

    await ejecutarAccion(async () => {
      await apiFetch(
        `/conciliaciones/${detalle.id}/estudiante?estudianteId=${encodeURIComponent(estudianteId)}&version=${encodeURIComponent(String(requireResourceVersion(detalle, "conciliación")))}`,
        { method: "PATCH" },
        "No se pudo asignar el estudiante"
      );
      await refrescar("Estudiante asignado correctamente");
    });
  }

  async function asignarConciliador() {
    if (!detalle?.id) return;
    if (detalleFinalizado) {
      setError("No se puede modificar una conciliación finalizada.");
      return;
    }
    if (!conciliadorId) {
      setError("Selecciona un conciliador activo.");
      return;
    }

    await ejecutarAccion(async () => {
      await apiFetch(
        `/conciliaciones/${detalle.id}/conciliador?conciliadorId=${encodeURIComponent(conciliadorId)}&version=${encodeURIComponent(String(requireResourceVersion(detalle, "conciliación")))}`,
        { method: "PATCH" },
        "No se pudo asignar el conciliador"
      );
      await refrescar("Conciliador asignado correctamente");
    });
  }

  async function cambiarEstado() {
    if (!detalle?.id) return;
    if (detalleFinalizado) {
      setError("No se puede modificar una conciliación finalizada.");
      return;
    }
    if (!estadoNoFinal) {
      setError("Selecciona un estado válido.");
      return;
    }

    if (estadoNoFinal === "ESPERANDO_REUNION" && (!detalle.estudianteId || !detalle.conciliadorId)) {
      setError("Para pasar a esperando reunión debe asignar estudiante y conciliador.");
      return;
    }

    await ejecutarAccion(async () => {
      await apiFetch(
        `/conciliaciones/${detalle.id}/estado?estado=${encodeURIComponent(estadoNoFinal)}&version=${encodeURIComponent(String(requireResourceVersion(detalle, "conciliación")))}`,
        { method: "PATCH" },
        "No se pudo cambiar el estado"
      );
      await refrescar("Estado actualizado correctamente");
    });
  }

  async function finalizarConciliacion(event) {
    event.preventDefault();
    if (!detalle?.id) return;

    if (detalleFinalizado) {
      setError("La conciliación ya se encuentra finalizada.");
      return;
    }

    if (!detalle.estudianteId || !detalle.conciliadorId) {
      setError("Para finalizar la conciliación debe asignar estudiante y conciliador.");
      return;
    }

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
      formData.append("acta", archivoActa);

      await apiFetch(
        `/conciliaciones/${detalle.id}/finalizar?estado=${encodeURIComponent(estadoFinal)}&version=${encodeURIComponent(String(requireResourceVersion(detalle, "conciliación")))}`,
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
        `/conciliaciones/${detalle.id}/solicitud?version=${encodeURIComponent(String(requireResourceVersion(detalle, "conciliación")))}`,
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
        `/conciliaciones/${detalle.id}?version=${encodeURIComponent(String(requireResourceVersion(detalle, "conciliación")))}`,
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
      if (isConcurrencyConflict(err) && detalle?.id) {
        const conflictMessage = withErrorReference(
          "La conciliación cambió mientras realizabas la operación. Tus selecciones y archivos se conservaron.",
          err?.correlationId || null
        );
        setError(conflictMessage);
        toast.error("Este registro cambió mientras realizabas la operación", {
          description: withErrorReference(
            "No hubo reintento automático. Se cargará la versión actual como nueva base.",
            err?.correlationId || null
          ),
        });

        try {
          const selectedStudent = estudianteId;
          const selectedConciliator = conciliadorId;
          const latest = await apiFetch(
            `/conciliaciones/${detalle.id}`,
            { method: "GET" },
            "No se pudo cargar la versión actual de la conciliación"
          );
          setDetalle(latest);
          setEstudianteId(selectedStudent);
          setConciliadorId(selectedConciliator);
        } catch {
          // The draft remains intact even if the refresh cannot be completed.
        }
        return;
      }

      const message = withErrorReference(
        err?.message || "No se pudo completar la acción",
        err?.correlationId || null
      );
      setError(message);
      toast.error("No se pudo completar la acción", {
        description: message,
      });
    } finally {
      setSaving(false);
    }
  }

  async function descargarDocumento(fileId, conciliacionId) {
    if (!fileId || !conciliacionId) return;

    try {
      const files = await fileApi.list({ type: "conciliacion", id: conciliacionId });
      const file = files.find((item) => Number(item.id) === Number(fileId));
      if (!file) throw new Error("El documento ya no está disponible");
      await fileApi.download(file, { type: "conciliacion", id: conciliacionId });
    } catch (err) {
      setError(
        withErrorReference(
          err?.message || "No se pudo descargar el documento",
          err?.correlationId || null
        )
      );
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
              consultas={consultasDisponiblesParaConciliacion}
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
                          disabled={!item.documentoSolicitudFileId}
                          onClick={() => descargarDocumento(item.documentoSolicitudFileId, item.id)}
                        >
                          Solicitud
                        </Button>
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          disabled={!item.actaFileId}
                          onClick={() => descargarDocumento(item.actaFileId, item.id)}
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
                    disabled={!detalle.documentoSolicitudFileId}
                    onClick={() => descargarDocumento(detalle.documentoSolicitudFileId, detalle.id)}
                  >
                    <Download className="mr-2 h-4 w-4" />
                    Descargar solicitud
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    disabled={!detalle.actaFileId}
                    onClick={() => descargarDocumento(detalle.actaFileId, detalle.id)}
                  >
                    <Download className="mr-2 h-4 w-4" />
                    Descargar acta
                  </Button>
                </div>
              </div>

              {mostrarPanelGestion ? (
                <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                  {puedeAsignarEstudiante && !detalleFinalizado && (
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

                  {puedeAsignarConciliador && !detalleFinalizado && (
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

                  {puedeCambiarEstado && !detalleFinalizado && (
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

                  {puedeFinalizar && !detalleFinalizado && (
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

                  {puedeReemplazarSolicitud && !detalleFinalizado && (
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

                  {puedeDesactivar && !detalleFinalizado && (
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

export default ConciliacionesForm;
