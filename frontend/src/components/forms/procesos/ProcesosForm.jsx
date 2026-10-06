"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { useDebouncedPageSearch } from "@/hooks/useDebouncedValue";
import { API_URL_BASE } from "@/lib/config";
import { isConcurrencyConflict, requireResourceVersion } from "@/lib/api";
import { fetchPaged, isAbortError } from "@/lib/pagedApi";

import { ESTADOS_PROCESO, FORM_INICIAL } from "./procesos.constants";
import {
  crearMapa,
  estadoProcesoEsFinal,
  extraerLista,
  labelEstadoProceso,
  nombreCatalogo,
  normalizarPayload,
  ordenarActivosPrimero,
  procesoAForm,
} from "./procesos.utils";
import { apiEnviar, apiGet } from "./procesos.service";
import {
  puedeAccederProcesos,
  puedeCargarCatalogos,
  puedeGestionarProcesos,
  puedeVerProcesos,
} from "./procesos.permissions";
import { Aviso, ModalCambioEstado, ModalEdicion } from "./ProcesosFormParts";

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

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
  const [paginaActual, setPaginaActual] = useState(1);
  const busquedaAplicada = useDebouncedPageSearch(busqueda, setPaginaActual, 350).trim();
  const [estadoFiltro, setEstadoFiltro] = useState("");
  const [fechaDesde, setFechaDesde] = useState("");
  const [fechaHasta, setFechaHasta] = useState("");
  const [sortBy, setSortBy] = useState("id");
  const [direction, setDirection] = useState("desc");
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);
  const [totalRegistros, setTotalRegistros] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [errorLista, setErrorLista] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  const [editando, setEditando] = useState(false);
  const [procesoCambioEstado, setProcesoCambioEstado] = useState(null);
  const [estadoSeleccionado, setEstadoSeleccionado] = useState("");
  const [form, setForm] = useState(FORM_INICIAL);
  const [confirmEliminar, setConfirmEliminar] = useState({
    abierto: false,
    proceso: null,
    loading: false,
  });

  const puedeVer = puedeVerProcesos(user);
  const puedeGestionar = puedeGestionarProcesos(user);

  const mapaDepartamentos = useMemo(() => crearMapa(departamentos), [departamentos]);
  const mapaOrganos = useMemo(() => crearMapa(organosControl), [organosControl]);
  const mapaEspecialidades = useMemo(() => crearMapa(especialidades), [especialidades]);

  const especialidadesFiltradas = useMemo(() => {
    if (!form.organoControlId) return [];
    return especialidades.filter(
      (especialidad) => Number(especialidad.organoControlId) === Number(form.organoControlId)
    );
  }, [especialidades, form.organoControlId]);

  useEffect(() => {
    verificarYCargar();
  }, []);

  useEffect(() => {
    if (!user || !puedeVer) return undefined;

    const controller = new AbortController();

    async function cargarPagina() {
      setCargando(true);
      setErrorLista("");

      try {
        const page = await fetchPaged("/procesos", {
          search: busquedaAplicada,
          page: paginaActual,
          size: registrosPorPagina,
          sortBy,
          direction,
          filters: {
            estado: estadoFiltro,
            fechaDesde,
            fechaHasta,
          },
          signal: controller.signal,
          resourceName: "procesos",
        });

        setProcesos(page.content);
        setTotalRegistros(page.totalElements);
        setTotalPaginas(page.totalPages);

        if (page.totalPages > 0 && page.page > page.totalPages) {
          setPaginaActual(page.totalPages);
        } else if (page.page !== paginaActual) {
          setPaginaActual(page.page);
        }
      } catch (error) {
        if (isAbortError(error)) return;

        if (error?.status === 401) {
          router.push("/");
          return;
        }

        setProcesos([]);
        setTotalRegistros(0);
        setTotalPaginas(0);
        setErrorLista(
          error?.status === 403
            ? "No tienes permiso para consultar procesos."
            : error?.message || "No fue posible cargar los procesos."
        );
      } finally {
        if (!controller.signal.aborted) setCargando(false);
      }
    }

    cargarPagina();
    return () => controller.abort();
  }, [
    user,
    puedeVer,
    busquedaAplicada,
    paginaActual,
    registrosPorPagina,
    sortBy,
    direction,
    estadoFiltro,
    fechaDesde,
    fechaHasta,
    reloadKey,
    router,
  ]);

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

      if (puedeCargarCatalogos(usuarioActual)) {
        const [departamentosRes, organosRes, especialidadesRes] = await Promise.allSettled([
          apiGet(`${API_URL_BASE}/departamentos`),
          apiGet(`${API_URL_BASE}/organos-control`),
          apiGet(`${API_URL_BASE}/especialidades`),
        ]);

        if (departamentosRes.status === "fulfilled") {
          setDepartamentos(ordenarActivosPrimero(extraerLista(departamentosRes.value)));
        }
        if (organosRes.status === "fulfilled") {
          setOrganosControl(ordenarActivosPrimero(extraerLista(organosRes.value)));
        }
        if (especialidadesRes.status === "fulfilled") {
          setEspecialidades(ordenarActivosPrimero(extraerLista(especialidadesRes.value)));
        }

        const errorCatalogo = [departamentosRes, organosRes, especialidadesRes]
          .find((result) => result.status === "rejected");
        if (errorCatalogo?.reason?.message) toast.error(errorCatalogo.reason.message);
      }
    } catch (error) {
      if (error?.status === 401) {
        router.push("/");
        return;
      }
      toast.error(error?.message || "No se pudo cargar procesos");
      router.push("/inicio");
    } finally {
      setChecking(false);
    }
  }

  function recargarPagina() {
    setReloadKey((value) => value + 1);
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
    if (form.especialidadId && form.organoControlId) {
      const especialidad = especialidades.find(
        (item) => Number(item.id) === Number(form.especialidadId)
      );
      if (!especialidad || Number(especialidad.organoControlId) !== Number(form.organoControlId)) {
        toast.error("La especialidad no pertenece al órgano de control seleccionado");
        return false;
      }
    }
    return true;
  }

  async function abrirEdicion(proceso) {
    if (!puedeGestionar) {
      toast.error("No tienes permiso para editar procesos");
      return;
    }

    try {
      const detalle = await apiGet(`${API_URL_BASE}/procesos/${proceso.id}`);
      setForm(procesoAForm(detalle));
      setConsultas([
        {
          id: detalle.consultaId,
          consultaId: detalle.consultaId,
          consulta: proceso.consulta || detalle.consulta || `Consulta #${detalle.consultaId}`,
        },
      ]);
      setEditando(true);
    } catch (error) {
      if (error?.status === 401) {
        router.push("/");
        return;
      }
      toast.error(
        error?.status === 404
          ? "El proceso no está disponible para consulta."
          : error?.message || "No fue posible cargar el detalle del proceso."
      );
    }
  }

  function cerrarEdicion() {
    setEditando(false);
    setForm(FORM_INICIAL);
    setConsultas([]);
  }

  function abrirCambioEstado(proceso) {
    if (!puedeGestionar) {
      toast.error("No tienes permiso para cambiar el estado del proceso");
      return;
    }
    setProcesoCambioEstado(proceso);
    setEstadoSeleccionado(proceso.estado || "");
  }

  function cerrarCambioEstado() {
    setProcesoCambioEstado(null);
    setEstadoSeleccionado("");
  }

  async function cambiarEstadoProceso(event) {
    event?.preventDefault?.();

    if (!puedeGestionar || !procesoCambioEstado?.id || !estadoSeleccionado) {
      toast.error("Selecciona un proceso y un estado válido");
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
      const version = requireResourceVersion(procesoCambioEstado, "proceso");
      await apiEnviar(
        `${API_URL_BASE}/procesos/${procesoCambioEstado.id}/estado?estado=${encodeURIComponent(
          estadoSeleccionado
        )}&version=${encodeURIComponent(String(version))}`,
        { method: "PATCH" }
      );
      toast.success("Estado del proceso actualizado correctamente");
      cerrarCambioEstado();
      recargarPagina();
    } catch (error) {
      if (error?.status === 401) {
        router.push("/");
        return;
      }
      if (isConcurrencyConflict(error)) {
        toast.error("El proceso cambió antes de actualizar su estado", {
          description: "La lista se actualizará y la acción deberá confirmarse nuevamente.",
        });
        recargarPagina();
        return;
      }
      toast.error(error?.message || "No se pudo cambiar el estado del proceso");
    } finally {
      setGuardando(false);
    }
  }

  async function guardarEdicion(event) {
    event.preventDefault();
    if (!puedeGestionar) {
      toast.error("No tienes permiso para editar procesos");
      return;
    }
    if (!validarAntesDeGuardar()) return;

    try {
      setGuardando(true);
      await apiEnviar(`${API_URL_BASE}/procesos/${form.id}`, {
        method: "PUT",
        body: JSON.stringify(
          normalizarPayload(
            { ...form, version: requireResourceVersion(form, "proceso") },
            { includeVersion: true }
          )
        ),
      });
      toast.success("Proceso actualizado correctamente");
      cerrarEdicion();
      recargarPagina();
    } catch (error) {
      if (error?.status === 401) {
        router.push("/");
        return;
      }
      if (isConcurrencyConflict(error)) {
        toast.error("Este proceso cambió mientras lo estabas editando", {
          description: "Tus cambios se conservaron. Se cargará la versión actual como nueva base.",
        });
        try {
          const latest = await apiGet(`${API_URL_BASE}/procesos/${form.id}`);
          if (latest?.version != null) {
            setForm((prev) => ({ ...prev, version: latest.version }));
          }
        } catch {
          // El borrador local se conserva si no puede recuperarse la versión actual.
        }
        return;
      }
      toast.error(error?.message || "No se pudo actualizar el proceso");
    } finally {
      setGuardando(false);
    }
  }

  function eliminarProceso(proceso) {
    if (!puedeGestionar) {
      toast.error("No tienes permiso para eliminar procesos");
      return;
    }
    setConfirmEliminar({ abierto: true, proceso, loading: false });
  }

  async function ejecutarEliminarProceso() {
    const { proceso } = confirmEliminar;
    if (!proceso) return;

    setConfirmEliminar((state) => ({ ...state, loading: true }));

    try {
      const version = requireResourceVersion(proceso, "proceso");
      await apiEnviar(
        `${API_URL_BASE}/procesos/${proceso.id}?version=${encodeURIComponent(String(version))}`,
        { method: "DELETE" }
      );
      toast.success("Proceso eliminado correctamente");
      if (procesos.length === 1 && paginaActual > 1) {
        setPaginaActual((page) => page - 1);
      } else {
        recargarPagina();
      }
    } catch (error) {
      if (error?.status === 401) {
        router.push("/");
        return;
      }
      if (isConcurrencyConflict(error)) {
        toast.error("El proceso cambió antes de eliminarlo", {
          description: "La lista se actualizará. Confirma nuevamente la eliminación sobre la versión actual.",
        });
        recargarPagina();
        return;
      }
      toast.error(error?.message || "No se pudo eliminar el proceso");
    } finally {
      setConfirmEliminar({ abierto: false, proceso: null, loading: false });
    }
  }

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
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-6">
            <input
              value={busqueda}
              onChange={(event) => setBusqueda(event.target.value)}
              placeholder="Buscar por radicado, departamento, órgano o consulta..."
              className="h-9 rounded-lg border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring md:col-span-2"
            />
            <select
              value={estadoFiltro}
              onChange={(event) => {
                setEstadoFiltro(event.target.value);
                setPaginaActual(1);
              }}
              className="h-9 rounded-lg border bg-background px-2 text-sm"
            >
              <option value="">Todos los estados</option>
              {ESTADOS_PROCESO.map((estado) => (
                <option key={estado.value} value={estado.value}>{estado.label}</option>
              ))}
            </select>
            <input
              type="date"
              value={fechaDesde}
              onChange={(event) => {
                setFechaDesde(event.target.value);
                setPaginaActual(1);
              }}
              className="h-9 rounded-lg border bg-background px-2 text-sm"
              aria-label="Fecha desde"
            />
            <input
              type="date"
              value={fechaHasta}
              onChange={(event) => {
                setFechaHasta(event.target.value);
                setPaginaActual(1);
              }}
              className="h-9 rounded-lg border bg-background px-2 text-sm"
              aria-label="Fecha hasta"
            />
            <Button type="button" variant="outline" onClick={recargarPagina} disabled={cargando}>
              {cargando ? "Actualizando..." : "Actualizar"}
            </Button>
          </div>

          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="text-muted-foreground">Orden:</span>
            <select
              value={sortBy}
              onChange={(event) => {
                setSortBy(event.target.value);
                setPaginaActual(1);
              }}
              className="h-8 rounded-md border bg-background px-2"
            >
              <option value="id">ID</option>
              <option value="fechaCreacion">Fecha de creación</option>
              <option value="numeroRadicado">Radicado</option>
              <option value="estado">Estado</option>
            </select>
            <select
              value={direction}
              onChange={(event) => {
                setDirection(event.target.value);
                setPaginaActual(1);
              }}
              className="h-8 rounded-md border bg-background px-2"
            >
              <option value="desc">Descendente</option>
              <option value="asc">Ascendente</option>
            </select>
          </div>

          {errorLista && <Aviso>{errorLista}</Aviso>}

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
                {cargando && procesos.length === 0 ? (
                  <tr>
                    <td colSpan={puedeGestionar ? 8 : 7} className="px-3 py-8 text-center text-muted-foreground">
                      Cargando procesos...
                    </td>
                  </tr>
                ) : procesos.length === 0 ? (
                  <tr>
                    <td colSpan={puedeGestionar ? 8 : 7} className="px-3 py-8 text-center text-muted-foreground">
                      No se encontraron procesos con los filtros seleccionados.
                    </td>
                  </tr>
                ) : (
                  procesos.map((proceso) => (
                    <tr key={proceso.id} className="border-t align-top">
                      <td className="px-3 py-2">#{proceso.id}</td>
                      <td className="px-3 py-2">{proceso.numeroRadicado || "Sin radicado"}</td>
                      <td className="px-3 py-2">
                        {proceso.departamentoNombre || nombreCatalogo(mapaDepartamentos, proceso.departamentoId)}
                      </td>
                      <td className="px-3 py-2 max-w-xs">
                        {proceso.consulta || `Consulta #${proceso.consultaId}`}
                      </td>
                      <td className="px-3 py-2">
                        {proceso.organoControlNombre ||
                          (proceso.organoControlId
                            ? nombreCatalogo(mapaOrganos, proceso.organoControlId)
                            : "Sin órgano")}
                      </td>
                      <td className="px-3 py-2">
                        {proceso.especialidadNombre ||
                          (proceso.especialidadId
                            ? nombreCatalogo(mapaEspecialidades, proceso.especialidadId)
                            : "Sin especialidad")}
                      </td>
                      <td className="px-3 py-2">
                        <span
                          className={`rounded-full border px-2 py-0.5 text-xs ${
                            estadoProcesoEsFinal(proceso.estado)
                              ? "bg-green-50 text-green-700 border-green-200"
                              : ""
                          }`}
                        >
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
                  ))
                )}
              </tbody>
            </table>
          </div>

          <Pagination
            currentPage={paginaActual}
            totalPages={totalPaginas}
            onPageChange={setPaginaActual}
            pageSize={registrosPorPagina}
            onPageSizeChange={(value) => {
              setRegistrosPorPagina(value);
              setPaginaActual(1);
            }}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            totalItems={totalRegistros}
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
        description={
          confirmEliminar.proceso
            ? `¿Seguro que deseas eliminar el proceso #${confirmEliminar.proceso.id}?`
            : "¿Eliminar este proceso?"
        }
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
