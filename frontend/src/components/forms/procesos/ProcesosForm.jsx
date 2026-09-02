"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { API_URL_BASE } from "@/lib/config";
import { isConcurrencyConflict, requireResourceVersion } from "@/lib/api";
import { getTotalPages, paginateItems, sortByIdAsc } from "@/lib/list-utils";

import { ESTADOS_PROCESO, FORM_INICIAL } from "./procesos.constants";
import {
  crearMapa,
  estadoProcesoEsFinal,
  extraerLista,
  labelCatalogo,
  labelConsulta,
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
  puedeCargarConsultas,
  puedeGestionarProcesos,
  puedeVerProcesos,
} from "./procesos.permissions";
import { Aviso, ModalCambioEstado, ModalEdicion } from "./ProcesosFormParts";

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
    if (!form.organoControlId) return [];
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
      const version = requireResourceVersion(procesoCambioEstado, "proceso");
      await apiEnviar(
        `${API_URL_BASE}/procesos/${procesoCambioEstado.id}/estado?estado=${encodeURIComponent(estadoSeleccionado)}&version=${encodeURIComponent(String(version))}`,
        { method: "PATCH" }
      );
      toast.success("Estado del proceso actualizado correctamente");
      cerrarCambioEstado();
      await cargarDatos();
    } catch (error) {

      if (error.status === 401) {
        router.push("/");
        return;
      }
      if (isConcurrencyConflict(error)) {
        toast.error("El proceso cambió antes de actualizar su estado", {
          description: "La lista se actualizará y la acción deberá confirmarse nuevamente.",
        });
        await cargarDatos();
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
          body: JSON.stringify(
            normalizarPayload(
              { ...form, version: requireResourceVersion(form, "proceso") },
              { includeVersion: true }
            )
          ),
        });
        toast.success("Proceso actualizado correctamente");
        cerrarEdicion();
        await cargarDatos();
      } catch (error) {

        if (error.status === 401) { router.push("/"); return; }
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
            // Keep the current draft when the fresh version cannot be loaded.
          }
          return;
        }
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
        const version = requireResourceVersion(proceso, "proceso");
        await apiEnviar(`${API_URL_BASE}/procesos/${proceso.id}?version=${encodeURIComponent(String(version))}`, { method: "DELETE" });
        toast.success("Proceso eliminado correctamente");
        await cargarDatos();
      } catch (error) {

        if (error.status === 401) { router.push("/"); return; }
        if (isConcurrencyConflict(error)) {
          toast.error("El proceso cambió antes de eliminarlo", {
            description: "La lista se actualizará. Confirma nuevamente la eliminación sobre la versión actual.",
          });
          await cargarDatos();
          return;
        }
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
