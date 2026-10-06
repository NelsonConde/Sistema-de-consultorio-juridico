"use client"

/**
 * Form handling.
 *
 * Validation rule.
 * Validation rule.
 * People workflow detail.
 * Implementation detail.
 *
 * File handling.
 * Implementation detail.
 *
 * @module components/forms/consulta/NuevaConsultaForm
 */

import { apiClient } from "@/lib/apiClient";
import { useFileResource } from "@/hooks/useFileResource";
import { useEffect, useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useRouter } from "next/navigation";

import { API_URL_BASE } from "@/lib/config";
import ArchivosConsultaForm from "../parts/ArchivosConsultaForm";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";
import {
  apiResponse,
  getApiErrorDescription,
  getApiErrorTitle,
  withErrorReference,
} from "@/lib/api";
import { buscarPersonasActivas } from "@/lib/personasApi";

import { VACIOS } from "./nueva-consulta.constants";
import { leerRespuesta, numberArray, numberOrNull, textOrNull } from "./nueva-consulta.utils";
import {
  idNormalizado,
  obtenerAreaIdAsesor,
  obtenerAsesorIdEstudiante,
  validarCoherenciaConsultaFrontend,
} from "./consultas-juridicas.utils";
import { ModalMultiple, ModalSimple } from "./ConsultaSelectionModals";
import { RemotePagedSelect } from "../parts/RemotePagedSelect";

export function NuevaConsultaForm() {
  const router = useRouter();

  const [form, setForm] = useState(VACIOS);
  const [archivos, setArchivos] = useState([]);
  const [consultaCreadaId, setConsultaCreadaId] = useState(null);

  const {
    upload: uploadArchivos,
    isUploading: archivosSubiendo,
    getUploadState,
    clearUploadState,
    cancelUpload,
    retryConfirmation,
  } = useFileResource(
      {
        type: "consulta",
        id: consultaCreadaId,
      },
      { load: false },
  );
  const [guardando, setGuardando] = useState(false);
  const submitLockRef = useRef(false);
  const [checking, setChecking] = useState(true);
  const [puedeAsignarResponsables, setPuedeAsignarResponsables] =
    useState(false);

  const [personasResultado, setPersonasResultado] = useState([]);
  const [personasBusquedaAplicada, setPersonasBusquedaAplicada] = useState("");
  const [personasPagina, setPersonasPagina] = useState(1);
  const [personasTamano, setPersonasTamano] = useState(10);
  const [personasTotalPaginas, setPersonasTotalPaginas] = useState(0);
  const [personasTotalElementos, setPersonasTotalElementos] = useState(0);
  const [personasLoading, setPersonasLoading] = useState(false);
  const [personasCache, setPersonasCache] = useState({});
  const [sedes, setSedes] = useState([]);
  const [areas, setAreas] = useState([]);
  const [temas, setTemas] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [asesores, setAsesores] = useState([]);
  const [monitores, setMonitores] = useState([]);
  const [estudiantes, setEstudiantes] = useState([]);

  const [modalParte, setModalParte] = useState({
    abierto: false,
    busqueda: "",
  });

  const [modalPartesAdicionales, setModalPartesAdicionales] = useState({
    abierto: false,
    busqueda: "",
  });

  const [modalContrapartes, setModalContrapartes] = useState({
    abierto: false,
    busqueda: "",
  });

  const asesorSeleccionado = useMemo(
    () => asesores.find((a) => String(a.id) === String(form.asesorId)) || null,
    [asesores, form.asesorId]
  );

  const monitorSeleccionado = useMemo(
    () =>
      monitores.find((m) => String(m.id) === String(form.monitorId)) || null,
    [monitores, form.monitorId]
  );

  const estudianteSeleccionado = useMemo(
    () =>
      estudiantes.find((e) => String(e.id) === String(form.estudianteId)) ||
      null,
    [estudiantes, form.estudianteId]
  );

  const parteSeleccionada = useMemo(
    () =>
      form.personaId
        ? personasCache[String(form.personaId)] || null
        : null,
    [personasCache, form.personaId]
  );

  const partesAdicionalesSeleccionadas = useMemo(
    () =>
      (form.partesIds || [])
        .map((id) => personasCache[String(id)] || null)
        .filter(Boolean),
    [personasCache, form.partesIds]
  );

  const contrapartesSeleccionadas = useMemo(
    () =>
      (form.contrapartesIds || [])
        .map((id) => personasCache[String(id)] || null)
        .filter(Boolean),
    [personasCache, form.contrapartesIds]
  );

  const personasParaPrincipal = useMemo(
    () =>
      personasResultado.filter((p) => {
        const id = Number(p.id);
        return (
          !(form.partesIds || []).includes(id) &&
          !(form.contrapartesIds || []).includes(id)
        );
      }),
    [personasResultado, form.partesIds, form.contrapartesIds]
  );

  const personasParaAdicionales = useMemo(
    () =>
      personasResultado.filter((p) => {
        const id = Number(p.id);
        return (
          id !== Number(form.personaId) &&
          !(form.contrapartesIds || []).includes(id)
        );
      }),
    [personasResultado, form.personaId, form.contrapartesIds]
  );

  const personasParaContrapartes = useMemo(
    () =>
      personasResultado.filter((p) => {
        const id = Number(p.id);
        return (
          id !== Number(form.personaId) &&
          !(form.partesIds || []).includes(id)
        );
      }),
    [personasResultado, form.personaId, form.partesIds]
  );

  const parteFiltrada = personasParaPrincipal;
  const partesAdicionalesFiltradas = personasParaAdicionales;
  const contrapartesFiltradas = personasParaContrapartes;

  const modalPersonasAbierto =
    modalParte.abierto ||
    modalPartesAdicionales.abierto ||
    modalContrapartes.abierto;

  const busquedaPersonasActual = modalParte.abierto
    ? modalParte.busqueda
    : modalPartesAdicionales.abierto
      ? modalPartesAdicionales.busqueda
      : modalContrapartes.abierto
        ? modalContrapartes.busqueda
        : "";

  useEffect(() => {
    if (!modalPersonasAbierto) return;

    const timeoutId = window.setTimeout(() => {
      setPersonasPagina(1);
      setPersonasBusquedaAplicada(busquedaPersonasActual.trim());
    }, 350);

    return () => window.clearTimeout(timeoutId);
  }, [modalPersonasAbierto, busquedaPersonasActual]);

  useEffect(() => {
    if (!modalPersonasAbierto) return;

    const controller = new AbortController();

    async function cargarPaginaPersonas() {
      try {
        setPersonasLoading(true);

        const resultado = await buscarPersonasActivas({
          search: personasBusquedaAplicada,
          page: personasPagina,
          size: personasTamano,
          signal: controller.signal,
        });

        setPersonasResultado(resultado.content);
        setPersonasTotalPaginas(resultado.totalPages);
        setPersonasTotalElementos(resultado.totalElements);
        setPersonasCache((prev) => {
          const next = { ...prev };
          for (const persona of resultado.content) {
            if (persona?.id != null) {
              next[String(persona.id)] = persona;
            }
          }
          return next;
        });
      } catch (error) {
        if (error.name === "AbortError") return;

        if (error.status === 401) {
          router.replace("/");
          return;
        }

        if (error.status === 403) {
          router.replace("/inicio");
          return;
        }

        setPersonasResultado([]);
        setPersonasTotalPaginas(0);
        setPersonasTotalElementos(0);
        toast.error(error.message || "No se pudieron buscar personas");
      } finally {
        setPersonasLoading(false);
      }
    }

    cargarPaginaPersonas();
    return () => controller.abort();
  }, [
    modalPersonasAbierto,
    personasBusquedaAplicada,
    personasPagina,
    personasTamano,
    router,
  ]);

  function abrirModalPersona(setModal) {
    setPersonasPagina(1);
    setPersonasBusquedaAplicada("");
    setModal({ abierto: true, busqueda: "" });
  }

  useEffect(() => {
    async function verificar() {
      try {
        const res = await apiClient.request(`${API_URL_BASE}/auth/me`, {
          method: "GET",
          credentials: "include",
        });

        if (res.status === 401) {
          router.replace("/");
          return;
        }

        if (!res.ok) {
          router.replace("/");
          return;
        }

        const user = await res.json();

        const puedeEntrar =
          tienePermiso(user, PERMISOS.ACCEDER_NUEVA_CONSULTA) &&
          tienePermiso(user, PERMISOS.CREAR_CONSULTAS);

        if (!puedeEntrar) {
          router.replace("/inicio");
          return;
        }

        const puedeAsignar = tienePermiso(
          user,
          PERMISOS.ASIGNAR_RESPONSABLES_CONSULTA
        );

        setPuedeAsignarResponsables(puedeAsignar);

        await cargarCatalogos();
      } catch {
        router.replace("/");
      } finally {
        setChecking(false);
      }
    }

    verificar();
  }, [router]);

  useEffect(() => {
    if (form.areaId) {
      apiClient.request(`${API_URL_BASE}/temas/area/${form.areaId}`, {
        credentials: "include",
      })
        .then((r) => {
          if (r.status === 401) {
            router.replace("/");
            return [];
          }

          if (r.status === 403) {
            router.replace("/inicio");
            return [];
          }

          if (!r.ok) {
            return [];
          }

          return r.json();
        })
        .then((d) => setTemas(Array.isArray(d) ? d : []))
        .catch(() => setTemas([]));
    } else {
      setTemas([]);
    }
  }, [form.areaId, router]);

  useEffect(() => {
    if (form.temaId) {
      apiClient.request(`${API_URL_BASE}/tipos/tema/${form.temaId}`, {
        credentials: "include",
      })
        .then((r) => {
          if (r.status === 401) {
            router.replace("/");
            return [];
          }

          if (r.status === 403) {
            router.replace("/inicio");
            return [];
          }

          if (!r.ok) {
            return [];
          }

          return r.json();
        })
        .then((d) => setTipos(Array.isArray(d) ? d : []))
        .catch(() => setTipos([]));
    } else {
      setTipos([]);
    }
  }, [form.temaId, router]);

  async function cargarCatalogos() {
    try {
      async function fetchLista(url) {
        const res = await apiClient.request(url, {
          credentials: "include",
        });

        if (res.status === 401) {
          router.replace("/");
          return [];
        }

        if (res.status === 403) {
          router.replace("/inicio");
          return [];
        }

        if (!res.ok) {
          return [];
        }

        const data = await res.json();
        return Array.isArray(data) ? data : [];
      }

      const [sR, aR] = await Promise.all([
        fetchLista(`${API_URL_BASE}/sedes`),
        fetchLista(`${API_URL_BASE}/areas`),
      ]);

      setSedes(sR);
      setAreas(aR);
    } catch {
      toast.error("Error cargando datos");
    }
  }

  function handleChange(e) {
    const { name, value } = e.target;

    if (name === "areaId") {
      setAsesores([]);
      setEstudiantes([]);
    }

    setForm((prev) => {
      const next = {
        ...prev,
        [name]: value,
      };

      if (name === "areaId") {
        next.temaId = "";
        next.tipoId = "";
        next.asesorId = "";
        next.estudianteId = "";
      }

      if (name === "temaId") {
        next.tipoId = "";
      }

      return next;
    });
  }

  async function subirArchivosConsulta(consultaId) {
    const seleccionados = Array.from(archivos || []);

    if (seleccionados.length === 0) {
      return true;
    }

    /*
     * A confirmation failure means the bytes already reached storage.
     * Those files must never start a second upload session.
     */
    const confirmacionesPendientes = seleccionados.filter(
        (file) =>
            getUploadState(file).state === "confirmation_failed",
    );

    const archivosParaTransferir = seleccionados.filter(
        (file) =>
            getUploadState(file).state !== "confirmation_failed",
    );

    if (archivosParaTransferir.length === 0) {
      toast.warning("Hay archivos pendientes de confirmación", {
        description:
            "Usa la opción de reintentar confirmación. No vuelvas a cargar el archivo.",
      });
      return false;
    }

    try {
      const results = await uploadArchivos(
          archivosParaTransferir,
          {
            type: "consulta",
            id: consultaId,
          },
      );

      const failed = results.filter((result) => !result.ok);
      const succeeded = results.filter((result) => result.ok);

      for (const result of succeeded) {
        clearUploadState(result.file);
      }

      const archivosRestantes = [
        ...confirmacionesPendientes,
        ...failed.map((result) => result.file),
      ];

      setArchivos(archivosRestantes);

      if (archivosRestantes.length === 0) {
        toast.success("Archivos subidos correctamente");
        return true;
      }

      const confirmationFailures = failed.filter(
          (result) =>
              result.error?.phase === "confirmation" &&
              result.error?.retryableCompletion === true,
      );

      const correlationId =
          failed.find((result) => result.error?.correlationId)
              ?.error?.correlationId || null;

      const totalConfirmacionesPendientes =
          confirmacionesPendientes.length +
          confirmationFailures.length;

      if (totalConfirmacionesPendientes > 0) {
        toast.warning(
            "La consulta se creó, pero hay archivos pendientes de confirmación",
            {
              description: withErrorReference(
                  "La transferencia ya terminó. Reintenta únicamente la confirmación.",
                  correlationId,
              ),
            },
        );

        return false;
      }

      const warning =
          failed.length === archivosParaTransferir.length
              ? "La consulta se creó, pero no se pudieron subir los archivos"
              : `La consulta se creó; ${failed.length} archivo(s) no pudieron completar la carga`;

      toast.warning(warning, {
        description: withErrorReference(
            "Revisa los archivos e intenta nuevamente.",
            correlationId,
        ),
      });

      return false;
    } catch (error) {
      toast.warning(
          "La consulta se creó, pero falló la carga de archivos",
          {
            description: withErrorReference(
                error?.message ||
                "Revisa los archivos e intenta nuevamente.",
                error?.correlationId || null,
            ),
          },
      );

      return false;
    }
  }

  async function cancelarCargaArchivo(file) {
    try {
      await cancelUpload(file);
    } catch (error) {
      toast.error("No se pudo cancelar la carga", {
        description:
            error?.message || "Intenta nuevamente.",
      });
    }
  }

  async function reintentarConfirmacionArchivo(file) {
    try {
      await retryConfirmation(file);

      const restantes = archivos.filter(
          (selectedFile) => selectedFile !== file,
      );

      setArchivos(restantes);
      clearUploadState(file);

      toast.success("Archivo confirmado correctamente");

      if (
          consultaCreadaId &&
          restantes.length === 0
      ) {
        router.push(
            `/consultasjuridicas?refresh=${Date.now()}`,
        );
      }
    } catch (error) {
      toast.error("No se pudo confirmar el archivo", {
        description:
            error?.message ||
            "La confirmación sigue pendiente. Puedes volver a intentarlo.",
      });
    }
  }

  async function descartarCargaPendiente(file) {
    try {
      await cancelUpload(file);

      const restantes = archivos.filter(
          (selectedFile) => selectedFile !== file,
      );

      setArchivos(restantes);
      clearUploadState(file);

      toast.success("Carga pendiente descartada");

      if (
          consultaCreadaId &&
          restantes.length === 0
      ) {
        router.push(
            `/consultasjuridicas?refresh=${Date.now()}`,
        );
      }
    } catch (error) {
      toast.error("No se pudo descartar la carga pendiente", {
        description:
            error?.message || "Intenta nuevamente.",
      });
    }
  }

  function validarFormularioConsulta() {
    const faltantes = [];

    if (!form.fecha) faltantes.push("fecha");
    if (!form.tramite?.trim()) faltantes.push("trámite");
    if (!form.sedeId) faltantes.push("sede");
    if (!form.areaId) faltantes.push("área");
    if (!form.temaId) faltantes.push("tema");
    if (!form.tipoId) faltantes.push("tipo");
    if (!form.personaId) faltantes.push("parte principal");
    if (!form.descripcion?.trim()) faltantes.push("descripción");
    if (!form.hechos?.trim()) faltantes.push("hechos");
    if (!form.pretensiones?.trim()) faltantes.push("pretensiones");
    if (!form.conceptoJuridico?.trim()) faltantes.push("concepto jurídico");

    if (faltantes.length === 0) {
      const errorCoherencia = validarCoherenciaConsultaFrontend({
        form,
        temas,
        tipos,
        asesores,
        monitores,
        estudiantes,
      });

      if (errorCoherencia) {
        toast.error("Asignación o relación inválida", {
          description: errorCoherencia,
        });
        return false;
      }

      if (String(form.descripcion || "").trim().length > 500) {
        toast.error("La descripción no puede superar los 500 caracteres");
        return false;
      }

      if (String(form.tramite || "").trim().length > 100) {
        toast.error("El trámite no puede superar los 100 caracteres");
        return false;
      }

      if (String(form.tipoViolencia || "").trim().length > 100) {
        toast.error("El tipo de violencia no puede superar los 100 caracteres");
        return false;
      }

      if (String(form.resultado || "").trim().length > 100) {
        toast.error("El resultado no puede superar los 100 caracteres");
        return false;
      }

      return true;
    }

    toast.error("Faltan datos obligatorios", {
      description: `Complete o seleccione: ${faltantes.join(", ")}.`,
    });

    return false;
  }

  async function handleGuardar(e) {
    e.preventDefault();

    if (submitLockRef.current) {
      return;
    }

    /*
     * La consulta ya existe. A partir de aquí solo se pueden
     * reintentar operaciones documentales.
     */
    if (consultaCreadaId) {
      if (guardando || archivosSubiendo) {
        return;
      }

      submitLockRef.current = true;
      setGuardando(true);

      try {
        const archivosCompletos =
            await subirArchivosConsulta(consultaCreadaId);

        if (archivosCompletos) {
          router.push(
              `/consultasjuridicas?refresh=${Date.now()}`,
          );
        }
      } finally {
        submitLockRef.current = false;
        setGuardando(false);
      }

      return;
    }

    if (!validarFormularioConsulta()) {
      return;
    }

    submitLockRef.current = true;
    setGuardando(true);

    const payload = {
      fecha: form.fecha,
      descripcion: form.descripcion,
      hechos: form.hechos,
      pretensiones: form.pretensiones,
      conceptoJuridico: form.conceptoJuridico,
      tramite: form.tramite,
      observaciones: form.observaciones || "",
      tipoViolencia: textOrNull(form.tipoViolencia),
      resultado: textOrNull(form.resultado),
      personaId: numberOrNull(form.personaId),
      sedeId: numberOrNull(form.sedeId),
      areaId: numberOrNull(form.areaId),
      temaId: numberOrNull(form.temaId),
      tipoId: numberOrNull(form.tipoId),
      asesorId:
        puedeAsignarResponsables && form.asesorId
          ? numberOrNull(form.asesorId)
          : null,
      monitorId:
        puedeAsignarResponsables && form.monitorId
          ? numberOrNull(form.monitorId)
          : null,
      estudianteId:
        puedeAsignarResponsables && form.estudianteId
          ? numberOrNull(form.estudianteId)
          : null,
      partesIds: numberArray(form.partesIds),
      contrapartesIds: numberArray(form.contrapartesIds),
    };

    try {
      const { response: res, data, correlationId } = await apiResponse(
        `${API_URL_BASE}/consultas`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        }
      );

      if (res.status === 401) {
        router.replace("/");
        return;
      }

      if (res.status === 403) {
        router.replace("/inicio");
        return;
      }

      if (!res.ok) {
        toast.error(getApiErrorTitle(data, "Error al crear"), {
          description: withErrorReference(
            getApiErrorDescription(data),
            correlationId
          ),
        });
        return;
      }

      const consultaId = data?.id;

      if (!consultaId) {
        toast.error("No se pudo completar la creación", {
          description: withErrorReference(
            "El servidor no devolvió el identificador de la consulta creada.",
            correlationId
          ),
        });
        return;
      }

      setConsultaCreadaId(consultaId);

      toast.success("Consulta creada");

      const archivosCompletos =
          await subirArchivosConsulta(consultaId);

      if (archivosCompletos) {
        router.push(
            `/consultasjuridicas?refresh=${Date.now()}`,
        );
      }
    } catch {
      toast.error("Error de conexión", {
        description: "No se pudo completar la creación de la consulta. Verifica la conexión e intenta nuevamente.",
      });
    } finally {
      submitLockRef.current = false;
      setGuardando(false);
    }
  }

  function renderPersona(p) {
    return (
      <>
        <div className="font-medium">
          {p.nombres} {p.apellidos}
        </div>
        <div className="text-xs text-muted-foreground">
          {p.tipoDocumento} {p.numeroDocumentoEnmascarado || ""}
        </div>
      </>
    );
  }

  if (checking) {
    return <p className="p-6">Verificando permisos...</p>;
  }

  return (
    <>
      <form onSubmit={handleGuardar} className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <C label="Fecha *">
            <input
              type="date"
              name="fecha"
              value={form.fecha}
              onChange={handleChange}
              required
              className={ic}
            />
          </C>

          <C label="Trámite *">
            <input
              name="tramite"
              value={form.tramite}
              onChange={handleChange}
              required
              placeholder="Ej: Conciliación"
              maxLength={100}
              className={ic}
            />
          </C>

          <C label="Sede *">
            <select
              name="sedeId"
              value={form.sedeId}
              onChange={handleChange}
              required
              className={ic}
            >
              <option value="">Seleccione</option>
              {sedes.map((sede) => (
                <option key={sede.id} value={sede.id}>
                  {sede.nombre}
                </option>
              ))}
            </select>
          </C>

          <C label="Área *">
            <select
              name="areaId"
              value={form.areaId}
              onChange={handleChange}
              required
              className={ic}
            >
              <option value="">Seleccione</option>
              {areas.map((area) => (
                <option key={area.id} value={area.id}>
                  {area.nombre}
                </option>
              ))}
            </select>
          </C>

          <C label="Tema *">
            <select
              name="temaId"
              value={form.temaId}
              onChange={handleChange}
              required
              className={ic}
              disabled={!form.areaId}
            >
              <option value="">
                {form.areaId ? "Seleccione" : "Seleccione área primero"}
              </option>
              {temas.map((tema) => (
                <option key={tema.id} value={tema.id}>
                  {tema.nombre}
                </option>
              ))}
            </select>
          </C>

          <C label="Tipo *">
            <select
              name="tipoId"
              value={form.tipoId}
              onChange={handleChange}
              required
              className={ic}
              disabled={!form.temaId}
            >
              <option value="">
                {form.temaId ? "Seleccione" : "Seleccione tema primero"}
              </option>
              {tipos.map((tipo) => (
                <option key={tipo.id} value={tipo.id}>
                  {tipo.nombre}
                </option>
              ))}
            </select>
          </C>

          <C label="Tipo de violencia">
            <input
              name="tipoViolencia"
              value={form.tipoViolencia}
              onChange={handleChange}
              placeholder="Opcional"
              maxLength={100}
              className={ic}
            />
          </C>

          <C label="Resultado">
            <input
              name="resultado"
              value={form.resultado}
              onChange={handleChange}
              placeholder="Opcional"
              maxLength={100}
              className={ic}
            />
          </C>

          {puedeAsignarResponsables && (
            <>
              <C label="Asesor">
                <RemotePagedSelect
                  value={form.asesorId}
                  selectedLabel={asesorSeleccionado
                    ? `${asesorSeleccionado.nombre}${asesorSeleccionado.documento ? ` - ${asesorSeleccionado.documento}` : ""}`
                    : ""}
                  onChange={(id, item) => {
                    if (item && obtenerAreaIdAsesor(item) !== idNormalizado(form.areaId)) {
                      toast.error("El asesor seleccionado no pertenece al área de la consulta.");
                      return false;
                    }
                    setAsesores(item ? [item] : []);
                    setEstudiantes([]);
                    setForm((prev) => ({
                      ...prev,
                      asesorId: id ? String(id) : "",
                      estudianteId: "",
                    }));
                  }}
                  endpoint="/asesores/activos/paginados"
                  legacyEndpoint="/asesores/activos"
                  resourceName="asesores activos"
                  sortBy="nombre"
                  direction="asc"
                  placeholder={form.areaId ? "Sin asignar" : "Seleccione área primero"}
                  searchPlaceholder="Buscar asesor..."
                  getOptionLabel={(item) =>
                    `${item.nombre}${item.documento ? ` - ${item.documento}` : ""}`
                  }
                  disabled={!form.areaId}
                />
              </C>

              <C label="Monitor">
                <RemotePagedSelect
                  value={form.monitorId}
                  selectedLabel={monitorSeleccionado
                    ? `${monitorSeleccionado.nombre}${monitorSeleccionado.documento ? ` - ${monitorSeleccionado.documento}` : ""}`
                    : ""}
                  onChange={(id, item) => {
                    setMonitores(item ? [item] : []);
                    setForm((prev) => ({ ...prev, monitorId: id ? String(id) : "" }));
                  }}
                  endpoint="/monitores/activos/paginados"
                  legacyEndpoint="/monitores/activos"
                  resourceName="monitores activos"
                  sortBy="nombre"
                  direction="asc"
                  placeholder="Sin asignar"
                  searchPlaceholder="Buscar monitor..."
                  getOptionLabel={(item) =>
                    `${item.nombre}${item.documento ? ` - ${item.documento}` : ""}`
                  }
                />
              </C>

              <C label="Estudiante">
                <RemotePagedSelect
                  value={form.estudianteId}
                  selectedLabel={estudianteSeleccionado
                    ? `${estudianteSeleccionado.nombre}${estudianteSeleccionado.codigo ? ` - ${estudianteSeleccionado.codigo}` : ""}`
                    : ""}
                  onChange={(id, item) => {
                    if (item && obtenerAsesorIdEstudiante(item) !== idNormalizado(form.asesorId)) {
                      toast.error("El estudiante seleccionado no pertenece al asesor asignado.");
                      return false;
                    }
                    setEstudiantes(item ? [item] : []);
                    setForm((prev) => ({ ...prev, estudianteId: id ? String(id) : "" }));
                  }}
                  endpoint="/estudiantes/activos/paginados"
                  legacyEndpoint="/estudiantes/activos"
                  resourceName="estudiantes activos"
                  sortBy="nombre"
                  direction="asc"
                  placeholder={form.asesorId ? "Sin asignar" : "Seleccione asesor primero"}
                  searchPlaceholder="Buscar estudiante..."
                  getOptionLabel={(item) =>
                    `${item.nombre}${item.codigo ? ` - ${item.codigo}` : ""}`
                  }
                  disabled={!form.asesorId}
                />
              </C>
            </>
          )}
        </div>

        <C label="Parte principal *">
          <button
            type="button"
            onClick={() =>
              abrirModalPersona(setModalParte)
            }
            className="flex h-9 w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm text-left hover:bg-muted/50 transition-colors"
          >
            <span
              className={
                parteSeleccionada ? "text-foreground" : "text-muted-foreground"
              }
            >
              {parteSeleccionada
                ? `${parteSeleccionada.nombres} ${parteSeleccionada.apellidos} - ${parteSeleccionada.numeroDocumentoEnmascarado || ""}`
                : "Buscar parte principal..."}
            </span>
            <span className="text-muted-foreground">▼</span>
          </button>
        </C>

        <C label="Partes adicionales">
          <button
            type="button"
            onClick={() =>
              abrirModalPersona(setModalPartesAdicionales)
            }
            className="flex min-h-9 w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm text-left hover:bg-muted/50 transition-colors"
          >
            <span
              className={
                partesAdicionalesSeleccionadas.length > 0
                  ? "text-foreground"
                  : "text-muted-foreground"
              }
            >
              {partesAdicionalesSeleccionadas.length > 0
                ? partesAdicionalesSeleccionadas
                  .map((p) => `${p.nombres} ${p.apellidos}`)
                  .join(", ")
                : "Buscar y agregar partes..."}
            </span>
            <span className="text-muted-foreground">▼</span>
          </button>
        </C>

        <C label="Contrapartes">
          <button
            type="button"
            onClick={() =>
              abrirModalPersona(setModalContrapartes)
            }
            className="flex min-h-9 w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm text-left hover:bg-muted/50 transition-colors"
          >
            <span
              className={
                contrapartesSeleccionadas.length > 0
                  ? "text-foreground"
                  : "text-muted-foreground"
              }
            >
              {contrapartesSeleccionadas.length > 0
                ? contrapartesSeleccionadas
                  .map((p) => `${p.nombres} ${p.apellidos}`)
                  .join(", ")
                : "Buscar y agregar contrapartes..."}
            </span>
            <span className="text-muted-foreground">▼</span>
          </button>
        </C>

        <C label="Descripción *">
          <textarea
            name="descripcion"
            value={form.descripcion}
            onChange={handleChange}
            maxLength={500}
            required
            rows={3}
            placeholder="Resumen de la consulta"
            className={ic}
          />
        </C>

        <C label="Hechos *">
          <textarea
            name="hechos"
            value={form.hechos}
            onChange={handleChange}
            maxLength={2000}
            required
            rows={3}
            placeholder="Descripción de los hechos"
            className={ic}
          />
        </C>

        <C label="Pretensiones *">
          <textarea
            name="pretensiones"
            value={form.pretensiones}
            onChange={handleChange}
            maxLength={2000}
            required
            rows={3}
            placeholder="Qué solicita el consultante"
            className={ic}
          />
        </C>

        <C label="Concepto jurídico *">
          <textarea
            name="conceptoJuridico"
            value={form.conceptoJuridico}
            onChange={handleChange}
            maxLength={2000}
            required
            rows={3}
            placeholder="Fundamento legal aplicable"
            className={ic}
          />
        </C>

        <C label="Observaciones">
          <textarea
            name="observaciones"
            value={form.observaciones}
            onChange={handleChange}
            maxLength={500}
            rows={2}
            placeholder="Opcional"
            className={ic}
          />
        </C>

        <ArchivosConsultaForm
            archivos={archivos}
            onChange={setArchivos}
            getUploadState={getUploadState}
            isUploading={archivosSubiendo}
            onCancel={cancelarCargaArchivo}
            onRetryConfirmation={reintentarConfirmacionArchivo}
            onDiscardPending={descartarCargaPendiente}
        />

        <div className="flex justify-end gap-3 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push("/consultasjuridicas")}
            disabled={guardando || archivosSubiendo}
          >
            Cancelar
          </Button>

          <Button
              type="submit"
              disabled={guardando || archivosSubiendo}
          >
            {guardando || archivosSubiendo
                ? consultaCreadaId
                    ? "Procesando archivos..."
                    : "Guardando..."
                : consultaCreadaId
                    ? "Reintentar archivos"
                    : "Crear consulta"}
          </Button>
        </div>
      </form>

      <ModalSimple
        abierto={modalParte.abierto}
        titulo="Seleccionar Parte Principal"
        items={parteFiltrada}
        busqueda={modalParte.busqueda}
        setBusqueda={(value) =>
          setModalParte((prev) => ({
            ...prev,
            busqueda: value,
          }))
        }
        onSeleccionar={(item) => {
          setForm((prev) => ({
            ...prev,
            personaId: item ? String(item.id) : "",
          }));
          setModalParte({ abierto: false, busqueda: "" });
        }}
        onCerrar={() => setModalParte({ abierto: false, busqueda: "" })}
        loading={personasLoading}
        pagination={{
          currentPage: personasPagina,
          totalPages: personasTotalPaginas,
          onPageChange: setPersonasPagina,
          pageSize: personasTamano,
          onPageSizeChange: (size) => {
            setPersonasTamano(size);
            setPersonasPagina(1);
          },
          pageSizeOptions: [10, 20, 50],
          totalItems: personasTotalElementos,
        }}
        seleccionado={parteSeleccionada}
        renderItem={renderPersona}
      />

      <ModalMultiple
        abierto={modalPartesAdicionales.abierto}
        titulo="Seleccionar Partes Adicionales"
        items={partesAdicionalesFiltradas}
        busqueda={modalPartesAdicionales.busqueda}
        setBusqueda={(value) =>
          setModalPartesAdicionales((prev) => ({
            ...prev,
            busqueda: value,
          }))
        }
        onConfirmar={(ids) => {
          setForm((prev) => ({
            ...prev,
            partesIds: ids,
          }));
          setModalPartesAdicionales({ abierto: false, busqueda: "" });
        }}
        onCerrar={() =>
          setModalPartesAdicionales({ abierto: false, busqueda: "" })
        }
        loading={personasLoading}
        pagination={{
          currentPage: personasPagina,
          totalPages: personasTotalPaginas,
          onPageChange: setPersonasPagina,
          pageSize: personasTamano,
          onPageSizeChange: (size) => {
            setPersonasTamano(size);
            setPersonasPagina(1);
          },
          pageSizeOptions: [10, 20, 50],
          totalItems: personasTotalElementos,
        }}
        seleccionados={form.partesIds}
        renderItem={renderPersona}
      />

      <ModalMultiple
        abierto={modalContrapartes.abierto}
        titulo="Seleccionar Contrapartes"
        items={contrapartesFiltradas}
        busqueda={modalContrapartes.busqueda}
        setBusqueda={(value) =>
          setModalContrapartes((prev) => ({
            ...prev,
            busqueda: value,
          }))
        }
        onConfirmar={(ids) => {
          setForm((prev) => ({
            ...prev,
            contrapartesIds: ids,
          }));
          setModalContrapartes({ abierto: false, busqueda: "" });
        }}
        onCerrar={() => setModalContrapartes({ abierto: false, busqueda: "" })}
        loading={personasLoading}
        pagination={{
          currentPage: personasPagina,
          totalPages: personasTotalPaginas,
          onPageChange: setPersonasPagina,
          pageSize: personasTamano,
          onPageSizeChange: (size) => {
            setPersonasTamano(size);
            setPersonasPagina(1);
          },
          pageSizeOptions: [10, 20, 50],
          totalItems: personasTotalElementos,
        }}
        seleccionados={form.contrapartesIds}
        renderItem={renderPersona}
      />
    </>
  );
}

function C({ label, children }) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-sm font-medium">{label}</label>
      {children}
    </div>
  );
}

const ic =
  "w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50";
