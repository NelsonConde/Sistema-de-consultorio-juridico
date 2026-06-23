/**
 * Formulario de creación de consultas jurídicas.
 *
 * Valida que todos los campos obligatorios estén completos antes de enviar,
 * incluyendo la parte principal (validación explícita antes del fetch).
 * Los IDs de personas y catálogos se convierten de forma segura con `numberOrNull()`
 * para evitar enviar `NaN` al backend.
 *
 * Soporta subida de archivos adjuntos después de crear la consulta.
 * Los campos de texto libre tienen límite de caracteres (maxLength).
 *
 * @module components/forms/consulta/NuevaConsultaForm
 */
"use client";

import React, { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useRouter } from "next/navigation";

import { API_URL_BASE, FILE_STORAGE_API_URL_BASE } from "@/lib/config";
import ArchivosConsultaForm from "../parts/ArchivosConsultaForm";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";
import { getApiErrorDescription, getApiErrorTitle, readResponseBody } from "@/lib/api";

const VACIOS = {
  fecha: "",
  descripcion: "",
  hechos: "",
  pretensiones: "",
  conceptoJuridico: "",
  tramite: "",
  observaciones: "",
  tipoViolencia: "",
  estado: "",
  resultado: "",
  personaId: "",
  sedeId: "",
  areaId: "",
  temaId: "",
  tipoId: "",
  asesorId: "",
  monitorId: "",
  estudianteId: "",
  partesIds: [],
  contrapartesIds: [],
};

function ModalSimple({
  abierto,
  titulo,
  items,
  busqueda,
  setBusqueda,
  onSeleccionar,
  onCerrar,
  seleccionado,
  renderItem,
}) {
  if (!abierto) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-background rounded-xl border shadow-lg w-full max-w-md mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">{titulo}</h3>
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
          placeholder="Buscar..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          className="w-full rounded-lg border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />

        <div className="max-h-72 overflow-y-auto space-y-1">
          <button
            type="button"
            onClick={() => onSeleccionar(null)}
            className={`w-full text-left px-3 py-2 rounded-lg text-sm hover:bg-muted transition-colors ${!seleccionado ? "bg-primary/10 text-primary font-medium" : ""
              }`}
          >
            Sin asignar
          </button>

          {items.length === 0 ? (
            <p className="text-center text-sm text-muted-foreground py-4">
              Sin resultados
            </p>
          ) : (
            items.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => onSeleccionar(item)}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm hover:bg-muted transition-colors ${seleccionado?.id === item.id
                    ? "bg-primary/10 text-primary font-medium"
                    : ""
                  }`}
              >
                {renderItem(item)}
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

function ModalMultiple({
  abierto,
  titulo,
  items,
  busqueda,
  setBusqueda,
  onConfirmar,
  onCerrar,
  seleccionados,
  renderItem,
}) {
  const [temp, setTemp] = useState([]);

  useEffect(() => {
    if (abierto) setTemp(seleccionados || []);
  }, [abierto, seleccionados]);

  if (!abierto) return null;

  function toggleItem(id) {
    const numId = Number(id);

    setTemp((prev) =>
      prev.includes(numId)
        ? prev.filter((item) => item !== numId)
        : [...prev, numId]
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-background rounded-xl border shadow-lg w-full max-w-md mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">{titulo}</h3>
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
          placeholder="Buscar..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          className="w-full rounded-lg border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />

        <div className="max-h-64 overflow-y-auto space-y-1">
          {items.length === 0 ? (
            <p className="text-center text-sm text-muted-foreground py-4">
              Sin resultados
            </p>
          ) : (
            items.map((item) => {
              const numId = Number(item.id);
              const marcado = temp.includes(numId);

              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => toggleItem(item.id)}
                  className={`w-full text-left px-3 py-2 rounded-lg text-sm hover:bg-muted transition-colors flex items-center gap-3 ${marcado ? "bg-primary/10" : ""
                    }`}
                >
                  <span
                    className={`w-4 h-4 rounded border flex items-center justify-center flex-shrink-0 ${marcado ? "bg-primary border-primary" : "border-gray-400"
                      }`}
                  >
                    {marcado && <span className="text-white text-xs">✓</span>}
                  </span>

                  <span>{renderItem(item)}</span>
                </button>
              );
            })
          )}
        </div>

        <div className="flex justify-between items-center pt-2">
          <span className="text-xs text-muted-foreground">
            {temp.length} seleccionado(s)
          </span>

          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setTemp([])}
            >
              Limpiar
            </Button>

            <Button type="button" size="sm" onClick={() => onConfirmar(temp)}>
              Confirmar
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

async function leerRespuesta(res) {
  const text = await res.text();

  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { mensaje: text };
  }
}

function textOrNull(value) {
  const text = String(value ?? "").trim();
  return text === "" ? null : text;
}

function numberOrNull(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);
  return Number.isNaN(number) ? null : number;
}

function numberArray(values) {
  if (!Array.isArray(values)) {
    return [];
  }

  return values
    .map((value) => Number(value))
    .filter((value) => !Number.isNaN(value));
}

export function NuevaConsultaForm() {
  const router = useRouter();

  const [form, setForm] = useState(VACIOS);
  const [archivos, setArchivos] = useState([]);
  const [guardando, setGuardando] = useState(false);
  const [checking, setChecking] = useState(true);
  const [puedeAsignarResponsables, setPuedeAsignarResponsables] =
    useState(false);

  const [personas, setPersonas] = useState([]);
  const [sedes, setSedes] = useState([]);
  const [areas, setAreas] = useState([]);
  const [temas, setTemas] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [asesores, setAsesores] = useState([]);
  const [monitores, setMonitores] = useState([]);
  const [estudiantes, setEstudiantes] = useState([]);

  const [modalAsesor, setModalAsesor] = useState({
    abierto: false,
    busqueda: "",
  });

  const [modalMonitor, setModalMonitor] = useState({
    abierto: false,
    busqueda: "",
  });

  const [modalEstudiante, setModalEstudiante] = useState({
    abierto: false,
    busqueda: "",
  });

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
    () => personas.find((p) => Number(p.id) === Number(form.personaId)) || null,
    [personas, form.personaId]
  );

  const partesAdicionalesSeleccionadas = useMemo(
    () =>
      personas.filter((p) => (form.partesIds || []).includes(Number(p.id))),
    [personas, form.partesIds]
  );

  const contrapartesSeleccionadas = useMemo(
    () =>
      personas.filter((p) =>
        (form.contrapartesIds || []).includes(Number(p.id))
      ),
    [personas, form.contrapartesIds]
  );

  const personasParaPrincipal = useMemo(
    () =>
      personas.filter((p) => {
        const id = Number(p.id);

        return (
          !(form.partesIds || []).includes(id) &&
          !(form.contrapartesIds || []).includes(id)
        );
      }),
    [personas, form.partesIds, form.contrapartesIds]
  );

  const personasParaAdicionales = useMemo(
    () =>
      personas.filter((p) => {
        const id = Number(p.id);

        return (
          id !== Number(form.personaId) &&
          !(form.contrapartesIds || []).includes(id)
        );
      }),
    [personas, form.personaId, form.contrapartesIds]
  );

  const personasParaContrapartes = useMemo(
    () =>
      personas.filter((p) => {
        const id = Number(p.id);

        return (
          id !== Number(form.personaId) &&
          !(form.partesIds || []).includes(id)
        );
      }),
    [personas, form.personaId, form.partesIds]
  );

  const asesoresFiltrados = useMemo(() => {
    const t = modalAsesor.busqueda.toLowerCase();

    return t
      ? asesores.filter((a) =>
        `${a.nombre} ${a.documento}`.toLowerCase().includes(t)
      )
      : asesores;
  }, [asesores, modalAsesor.busqueda]);

  const monitoresFiltrados = useMemo(() => {
    const t = modalMonitor.busqueda.toLowerCase();

    return t
      ? monitores.filter((m) =>
        `${m.nombre} ${m.documento}`.toLowerCase().includes(t)
      )
      : monitores;
  }, [monitores, modalMonitor.busqueda]);

  const estudiantesFiltrados = useMemo(() => {
    const t = modalEstudiante.busqueda.toLowerCase();

    return t
      ? estudiantes.filter((e) =>
        `${e.nombre} ${e.documento} ${e.codigo}`.toLowerCase().includes(t)
      )
      : estudiantes;
  }, [estudiantes, modalEstudiante.busqueda]);

  const parteFiltrada = useMemo(() => {
    const t = modalParte.busqueda.toLowerCase();

    return t
      ? personasParaPrincipal.filter((p) =>
        `${p.nombres} ${p.apellidos} ${p.numeroDocumento}`
          .toLowerCase()
          .includes(t)
      )
      : personasParaPrincipal;
  }, [personasParaPrincipal, modalParte.busqueda]);

  const partesAdicionalesFiltradas = useMemo(() => {
    const t = modalPartesAdicionales.busqueda.toLowerCase();

    return t
      ? personasParaAdicionales.filter((p) =>
        `${p.nombres} ${p.apellidos} ${p.numeroDocumento}`
          .toLowerCase()
          .includes(t)
      )
      : personasParaAdicionales;
  }, [personasParaAdicionales, modalPartesAdicionales.busqueda]);

  const contrapartesFiltradas = useMemo(() => {
    const t = modalContrapartes.busqueda.toLowerCase();

    return t
      ? personasParaContrapartes.filter((p) =>
        `${p.nombres} ${p.apellidos} ${p.numeroDocumento}`
          .toLowerCase()
          .includes(t)
      )
      : personasParaContrapartes;
  }, [personasParaContrapartes, modalContrapartes.busqueda]);

  useEffect(() => {
    async function verificar() {
      try {
        const res = await fetch(`${API_URL_BASE}/auth/me`, {
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

        await cargarCatalogos(puedeAsignar);
      } catch (error) {
        console.error("Error verificando permisos:", error);
        router.replace("/");
      } finally {
        setChecking(false);
      }
    }

    verificar();
  }, [router]);

  useEffect(() => {
    if (form.areaId) {
      fetch(`${API_URL_BASE}/temas/area/${form.areaId}`, {
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
      fetch(`${API_URL_BASE}/tipos/tema/${form.temaId}`, {
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

  async function cargarCatalogos(puedeAsignar = false) {
    try {
      async function fetchLista(url) {
        const res = await fetch(url, {
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

      const [pR, sR, aR] = await Promise.all([
        fetchLista(`${API_URL_BASE}/personas/activos`),
        fetchLista(`${API_URL_BASE}/sedes`),
        fetchLista(`${API_URL_BASE}/areas`),
      ]);

      setPersonas(pR);
      setSedes(sR);
      setAreas(aR);

      if (puedeAsignar) {
        const [asR, moR, esR] = await Promise.all([
          fetchLista(`${API_URL_BASE}/asesores/activos`),
          fetchLista(`${API_URL_BASE}/monitores/activos`),
          fetchLista(`${API_URL_BASE}/estudiantes/activos`),
        ]);

        setAsesores(asR);
        setMonitores(moR);
        setEstudiantes(esR);
      } else {
        setAsesores([]);
        setMonitores([]);
        setEstudiantes([]);
      }
    } catch (e) {
      console.error("Error catálogos:", e);
      toast.error("Error cargando datos");
    }
  }

  function handleChange(e) {
    const { name, value } = e.target;

    setForm((prev) => {
      const next = {
        ...prev,
        [name]: value,
      };

      if (name === "areaId") {
        next.temaId = "";
        next.tipoId = "";
      }

      if (name === "temaId") {
        next.tipoId = "";
      }

      return next;
    });
  }

  async function subirArchivosConsulta(consultaId) {
    if (!archivos || archivos.length === 0) {
      return true;
    }

    const formData = new FormData();

    archivos.forEach((file) => {
      formData.append("files", file);
    });

    formData.append("path", String(consultaId));

    try {
      const uploadRes = await fetch(
        `${FILE_STORAGE_API_URL_BASE}/files/upload-multiple`,
        {
          method: "POST",
          credentials: "include",
          body: formData,
        }
      );

      if (uploadRes.ok) {
        toast.success("Archivos subidos correctamente");
        return true;
      }

      const errorText = await uploadRes.text();
      console.error("Error subiendo archivos:", errorText);
      toast.warning("La consulta se creó, pero no se pudieron subir los archivos");
      return false;
    } catch (error) {
      console.error("Error subiendo archivos:", error);
      toast.warning("La consulta se creó, pero falló la conexión al subir archivos");
      return false;
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
      return true;
    }

    toast.error("Faltan datos obligatorios", {
      description: `Complete o seleccione: ${faltantes.join(", ")}.`,
    });

    return false;
  }

  async function handleGuardar(e) {
    e.preventDefault();

    if (!validarFormularioConsulta()) {
      return;
    }

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
      const res = await fetch(`${API_URL_BASE}/consultas`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (res.status === 401) {
        router.replace("/");
        return;
      }

      if (res.status === 403) {
        router.replace("/inicio");
        return;
      }

      const data = await readResponseBody(res);

      if (!res.ok) {
        toast.error(getApiErrorTitle(data, "Error al crear"), {
          description: getApiErrorDescription(data),
        });
        return;
      }

      const consultaId = data?.id;

      if (!consultaId) {
        throw new Error("No se recibió el id de la consulta");
      }

      toast.success("Consulta creada");

      await subirArchivosConsulta(consultaId);

      router.push(`/consultasjuridicas?refresh=${Date.now()}`);
    } catch (error) {
      console.error(error);
      toast.error(error.message || "Error de conexión");
    } finally {
      setGuardando(false);
    }
  }

  function renderPersona(p) {
    return (
      <>
        <div className="font-medium">
          {p.nombres} {p.apellidos}
        </div>
        <div className="text-xs text-muted-foreground">{p.numeroDocumento}</div>
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
              className={ic}
            />
          </C>

          <C label="Resultado">
            <input
              name="resultado"
              value={form.resultado}
              onChange={handleChange}
              placeholder="Opcional"
              className={ic}
            />
          </C>

          {puedeAsignarResponsables && (
            <>
              <C label="Asesor">
                <button
                  type="button"
                  onClick={() =>
                    setModalAsesor((prev) => ({
                      ...prev,
                      abierto: true,
                    }))
                  }
                  className="flex h-9 w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm text-left hover:bg-muted/50 transition-colors"
                >
                  <span
                    className={
                      asesorSeleccionado
                        ? "text-foreground"
                        : "text-muted-foreground"
                    }
                  >
                    {asesorSeleccionado
                      ? `${asesorSeleccionado.nombre}${asesorSeleccionado.documento
                        ? ` - ${asesorSeleccionado.documento}`
                        : ""
                      }`
                      : "Sin asignar"}
                  </span>
                  <span className="text-muted-foreground">▼</span>
                </button>
              </C>

              <C label="Monitor">
                <button
                  type="button"
                  onClick={() =>
                    setModalMonitor((prev) => ({
                      ...prev,
                      abierto: true,
                    }))
                  }
                  className="flex h-9 w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm text-left hover:bg-muted/50 transition-colors"
                >
                  <span
                    className={
                      monitorSeleccionado
                        ? "text-foreground"
                        : "text-muted-foreground"
                    }
                  >
                    {monitorSeleccionado
                      ? `${monitorSeleccionado.nombre}${monitorSeleccionado.documento
                        ? ` - ${monitorSeleccionado.documento}`
                        : ""
                      }`
                      : "Sin asignar"}
                  </span>
                  <span className="text-muted-foreground">▼</span>
                </button>
              </C>

              <C label="Estudiante">
                <button
                  type="button"
                  onClick={() =>
                    setModalEstudiante((prev) => ({
                      ...prev,
                      abierto: true,
                    }))
                  }
                  className="flex h-9 w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm text-left hover:bg-muted/50 transition-colors"
                >
                  <span
                    className={
                      estudianteSeleccionado
                        ? "text-foreground"
                        : "text-muted-foreground"
                    }
                  >
                    {estudianteSeleccionado
                      ? `${estudianteSeleccionado.nombre}${estudianteSeleccionado.codigo
                        ? ` - ${estudianteSeleccionado.codigo}`
                        : ""
                      }`
                      : "Sin asignar"}
                  </span>
                  <span className="text-muted-foreground">▼</span>
                </button>
              </C>
            </>
          )}
        </div>

        <C label="Parte principal *">
          <button
            type="button"
            onClick={() =>
              setModalParte((prev) => ({
                ...prev,
                abierto: true,
              }))
            }
            className="flex h-9 w-full items-center justify-between rounded-md border bg-background px-3 py-2 text-sm text-left hover:bg-muted/50 transition-colors"
          >
            <span
              className={
                parteSeleccionada ? "text-foreground" : "text-muted-foreground"
              }
            >
              {parteSeleccionada
                ? `${parteSeleccionada.nombres} ${parteSeleccionada.apellidos} - ${parteSeleccionada.numeroDocumento}`
                : "Buscar parte principal..."}
            </span>
            <span className="text-muted-foreground">▼</span>
          </button>
        </C>

        <C label="Partes adicionales">
          <button
            type="button"
            onClick={() =>
              setModalPartesAdicionales((prev) => ({
                ...prev,
                abierto: true,
              }))
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
              setModalContrapartes((prev) => ({
                ...prev,
                abierto: true,
              }))
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
            maxLength={2000}
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

        <ArchivosConsultaForm archivos={archivos} onChange={setArchivos} />

        <div className="flex justify-end gap-3 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push("/consultasjuridicas")}
            disabled={guardando}
          >
            Cancelar
          </Button>

          <Button type="submit" disabled={guardando}>
            {guardando ? "Guardando..." : "Crear consulta"}
          </Button>
        </div>
      </form>

      {puedeAsignarResponsables && (
        <>
          <ModalSimple
            abierto={modalAsesor.abierto}
            titulo="Seleccionar Asesor"
            items={asesoresFiltrados}
            busqueda={modalAsesor.busqueda}
            setBusqueda={(value) =>
              setModalAsesor((prev) => ({
                ...prev,
                busqueda: value,
              }))
            }
            onSeleccionar={(item) => {
              setForm((prev) => ({
                ...prev,
                asesorId: item ? String(item.id) : "",
              }));
              setModalAsesor({ abierto: false, busqueda: "" });
            }}
            onCerrar={() => setModalAsesor({ abierto: false, busqueda: "" })}
            seleccionado={asesorSeleccionado}
            renderItem={(asesor) => (
              <>
                <div className="font-medium">{asesor.nombre}</div>
                <div className="text-xs text-muted-foreground">
                  {asesor.documento}
                </div>
              </>
            )}
          />

          <ModalSimple
            abierto={modalMonitor.abierto}
            titulo="Seleccionar Monitor"
            items={monitoresFiltrados}
            busqueda={modalMonitor.busqueda}
            setBusqueda={(value) =>
              setModalMonitor((prev) => ({
                ...prev,
                busqueda: value,
              }))
            }
            onSeleccionar={(item) => {
              setForm((prev) => ({
                ...prev,
                monitorId: item ? String(item.id) : "",
              }));
              setModalMonitor({ abierto: false, busqueda: "" });
            }}
            onCerrar={() => setModalMonitor({ abierto: false, busqueda: "" })}
            seleccionado={monitorSeleccionado}
            renderItem={(monitor) => (
              <>
                <div className="font-medium">{monitor.nombre}</div>
                <div className="text-xs text-muted-foreground">
                  {monitor.documento}
                </div>
              </>
            )}
          />

          <ModalSimple
            abierto={modalEstudiante.abierto}
            titulo="Seleccionar Estudiante"
            items={estudiantesFiltrados}
            busqueda={modalEstudiante.busqueda}
            setBusqueda={(value) =>
              setModalEstudiante((prev) => ({
                ...prev,
                busqueda: value,
              }))
            }
            onSeleccionar={(item) => {
              setForm((prev) => ({
                ...prev,
                estudianteId: item ? String(item.id) : "",
              }));
              setModalEstudiante({ abierto: false, busqueda: "" });
            }}
            onCerrar={() =>
              setModalEstudiante({ abierto: false, busqueda: "" })
            }
            seleccionado={estudianteSeleccionado}
            renderItem={(estudiante) => (
              <>
                <div className="font-medium">{estudiante.nombre}</div>
                <div className="text-xs text-muted-foreground">
                  {estudiante.codigo} — {estudiante.documento}
                </div>
              </>
            )}
          />
        </>
      )}

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
