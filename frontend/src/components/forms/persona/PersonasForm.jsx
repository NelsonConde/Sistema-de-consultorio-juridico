"use client"

/**
 * Formulario de listado de personas registradas en el sistema.
 *
 * Muestra la lista de personas con búsqueda y paginación.
 * Permite abrir el formulario de registro/edición de persona.
 *
 * @module components/forms/persona/PersonasForm
 */
;

import React, { useEffect, useMemo, useState } from "react";
import { Search, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { API_URL_BASE } from "@/lib/config";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { useRouter } from "next/navigation";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";
import { getApiErrorDescription, getApiErrorTitle } from "@/lib/api";
import { EMAIL_PATTERN } from "@/lib/form-validation";

const FORM_INICIAL = {
  tipoPersonaId: "",
  tipoDocumento: "",
  numeroDocumento: "",
  fechaExpedicion: "",
  ciudadExpedicion: "",
  nombres: "",
  apellidos: "",
  nombreIdentitario: "",
  pronombre: "",
  sexo: "",
  genero: "",
  orientacionSexual: "",
  fechaNacimiento: "",
  telefono: "",
  correo: "",
  nacionalidadId: "",
  estadoCivil: "",
  escolaridad: "",
  grupoEtnico: "",
  condicionActualId: "",
  sabeLeerEscribir: false,
  discapacidad: "",
  caracterizacionPcd: "",
  necesitaAjustePcd: false,
  departamentoId: "",
  municipioId: "",
  barrioId: "",
  direccion: "",
  comuna: "",
  localidad: "",
  estrato: 0,
  tipoVivienda: "",
  zona: "",
  tenencia: "",
  numeroPersonasACargo: 0,
  ingresosAdicionales: false,
  energiaElectrica: false,
  acueducto: false,
  alcantarillado: false,
  ocupacionId: "",
  empresaId: "",
  salario: 0,
  cargo: "",
  direccionEmpresa: "",
  telefonoEmpresa: "",
  nombreCompletoAcudiente: "",
  relacionAcudiente: "",
  telefonoAcudiente: "",
  correoAcudiente: "",
  direccionAcudiente: "",
  comoSeEntero: "",
  relacionConUniversidad: "",
};

const FALLBACK_TIPO_DOCUMENTO_OPTIONS = [
  { value: "CC", label: "Cédula de Ciudadanía" },
  { value: "TI", label: "Tarjeta de Identidad" },
  { value: "CE", label: "Cédula de Extranjería" },
  { value: "PA", label: "Pasaporte" },
];

const PRONOMBRE_OPTIONS = [
  { value: "Él", label: "Él" },
  { value: "Ella", label: "Ella" },
  { value: "Elle", label: "Elle" },
  { value: "Otro", label: "Otro" },
];

const SEXO_OPTIONS = [
  { value: "Hombre", label: "Hombre" },
  { value: "Mujer", label: "Mujer" },
  { value: "Intersexual", label: "Intersexual" },
];

const GENERO_OPTIONS = [
  { value: "Masculino", label: "Masculino" },
  { value: "Femenino", label: "Femenino" },
  { value: "No binario", label: "No binario" },
  { value: "Transgénero", label: "Transgénero" },
  { value: "Otro", label: "Otro" },
];

const ORIENTACION_OPTIONS = [
  { value: "Heterosexual", label: "Heterosexual" },
  { value: "Homosexual", label: "Homosexual" },
  { value: "Bisexual", label: "Bisexual" },
  { value: "Pansexual", label: "Pansexual" },
  { value: "Asexual", label: "Asexual" },
  { value: "Otro", label: "Otro" },
];

const ESTADO_CIVIL_OPTIONS = [
  { value: "Soltero/a", label: "Soltero/a" },
  { value: "Casado/a", label: "Casado/a" },
  { value: "Unión libre", label: "Unión libre" },
  { value: "Divorciado/a", label: "Divorciado/a" },
  { value: "Viudo/a", label: "Viudo/a" },
];

const ESCOLARIDAD_OPTIONS = [
  { value: "Ninguna", label: "Ninguna" },
  { value: "Primaria", label: "Primaria" },
  { value: "Secundaria", label: "Secundaria" },
  { value: "Técnico", label: "Técnico" },
  { value: "Tecnólogo", label: "Tecnólogo" },
  { value: "Universitario", label: "Universitario" },
  { value: "Postgrado", label: "Postgrado" },
];

const ZONA_OPTIONS = [
  { value: "Urbana", label: "Urbana" },
  { value: "Rural", label: "Rural" },
];

const REGISTROS_POR_PAGINA_OPTIONS = [5, 10, 20, 50];

async function leerRespuesta(res) {
  const text = await res.text();

  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { mensaje: text };
  }
}

async function fetchCatalogo(path) {
  const res = await fetch(`${API_URL_BASE}${path}`, {
    method: "GET",
    credentials: "include",
  });

  if (!res.ok) {
    return [];
  }

  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

function toOption(item) {
  return {
    value: String(item.id),
    label: item.nombre || item.descripcion || `Registro ${item.id}`,
  };
}

function toDocumentoOption(item) {
  const value = item.codigo || item.abreviatura || item.nombre || item.descripcion || "";
  const label = item.nombre || item.descripcion || item.codigo || item.abreviatura || value;

  return { value, label };
}

function optionsMap(options) {
  return new Map(options.map((option) => [String(option.value), option.label]));
}

function labelFromMap(map, value, fallback = "N/A") {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  return map.get(String(value)) || fallback;
}

function nombreCompleto(persona) {
  return [persona?.nombres, persona?.apellidos].filter(Boolean).join(" ");
}

function valorTexto(value) {
  return value || "N/A";
}

function numberOrNull(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);
  return Number.isNaN(number) ? null : number;
}

function textOrNull(value) {
  const text = String(value ?? "").trim();
  return text === "" ? null : text;
}

function calcularEsMenorEdad(fechaNacimiento) {
  if (!fechaNacimiento) return false;

  const nacimiento = new Date(fechaNacimiento);
  if (Number.isNaN(nacimiento.getTime())) return false;

  const hoy = new Date();
  let edad = hoy.getFullYear() - nacimiento.getFullYear();
  const mes = hoy.getMonth() - nacimiento.getMonth();

  if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
    edad -= 1;
  }

  return edad < 18;
}

function ordenarPorIdAscendente(items) {
  return [...items].sort((a, b) => {
    const idA = Number(a?.id ?? Number.MAX_SAFE_INTEGER);
    const idB = Number(b?.id ?? Number.MAX_SAFE_INTEGER);

    return idA - idB;
  });
}

function obtenerPaginasVisibles(paginaActual, totalPaginas) {
  const paginas = new Set([
    1,
    totalPaginas,
    paginaActual - 1,
    paginaActual,
    paginaActual + 1,
  ]);

  return Array.from(paginas)
    .filter((pagina) => pagina >= 1 && pagina <= totalPaginas)
    .sort((a, b) => a - b);
}

function convertirPersonaAForm(persona) {
  return {
    ...FORM_INICIAL,
    ...persona,
    tipoPersonaId: persona.tipoPersonaId != null ? String(persona.tipoPersonaId) : "",
    nacionalidadId: persona.nacionalidadId != null ? String(persona.nacionalidadId) : "",
    condicionActualId:
      persona.condicionActualId != null ? String(persona.condicionActualId) : "",
    departamentoId: persona.departamentoId != null ? String(persona.departamentoId) : "",
    municipioId: persona.municipioId != null ? String(persona.municipioId) : "",
    barrioId: persona.barrioId != null ? String(persona.barrioId) : "",
    ocupacionId: persona.ocupacionId != null ? String(persona.ocupacionId) : "",
    empresaId: persona.empresaId != null ? String(persona.empresaId) : "",
    sabeLeerEscribir: Boolean(persona.sabeLeerEscribir),
    necesitaAjustePcd: Boolean(persona.necesitaAjustePcd),
    ingresosAdicionales: Boolean(persona.ingresosAdicionales),
    energiaElectrica: Boolean(persona.energiaElectrica),
    acueducto: Boolean(persona.acueducto),
    alcantarillado: Boolean(persona.alcantarillado),
    estrato: persona.estrato ?? 0,
    numeroPersonasACargo: persona.numeroPersonasACargo ?? 0,
    salario: persona.salario ?? 0,
  };
}

function construirPayload(form, id) {
  const esMenorEdad = calcularEsMenorEdad(form.fechaNacimiento);

  return {
    ...form,
    id,
    tipoPersonaId: numberOrNull(form.tipoPersonaId),
    nacionalidadId: numberOrNull(form.nacionalidadId),
    condicionActualId: numberOrNull(form.condicionActualId),
    departamentoId: numberOrNull(form.departamentoId),
    municipioId: numberOrNull(form.municipioId),
    barrioId: numberOrNull(form.barrioId),
    ocupacionId: numberOrNull(form.ocupacionId),
    empresaId: numberOrNull(form.empresaId),
    estrato: Number(form.estrato || 0),
    numeroPersonasACargo: Number(form.numeroPersonasACargo || 0),
    salario: Number(form.salario || 0),
    correo: textOrNull(form.correo),
    correoAcudiente: esMenorEdad ? textOrNull(form.correoAcudiente) : null,
    nombreCompletoAcudiente: esMenorEdad ? textOrNull(form.nombreCompletoAcudiente) : null,
    relacionAcudiente: esMenorEdad ? textOrNull(form.relacionAcudiente) : null,
    telefonoAcudiente: esMenorEdad ? textOrNull(form.telefonoAcudiente) : null,
    direccionAcudiente: esMenorEdad ? textOrNull(form.direccionAcudiente) : null,
  };
}

export function PersonasForm() {
  const [user, setUser] = useState(null);
  const [personas, setPersonas] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);
  const [personaEditando, setPersonaEditando] = useState(null);
  const [personaADesactivar, setPersonaADesactivar] = useState(null);
  const [form, setForm] = useState(FORM_INICIAL);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [desactivando, setDesactivando] = useState(false);
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState("");

  const [tipoDocumentoOptions, setTipoDocumentoOptions] = useState(
    FALLBACK_TIPO_DOCUMENTO_OPTIONS
  );
  const [tipoPersonaOptions, setTipoPersonaOptions] = useState([]);
  const [nacionalidadOptions, setNacionalidadOptions] = useState([]);
  const [condicionOptions, setCondicionOptions] = useState([]);
  const [ocupacionOptions, setOcupacionOptions] = useState([]);
  const [empresaOptions, setEmpresaOptions] = useState([]);
  const [departamentoOptions, setDepartamentoOptions] = useState([]);
  const [municipioOptions, setMunicipioOptions] = useState([]);
  const [barrioOptions, setBarrioOptions] = useState([]);
  const [municipioCatalogoOptions, setMunicipioCatalogoOptions] = useState([]);
  const [barrioCatalogoOptions, setBarrioCatalogoOptions] = useState([]);

  const router = useRouter();

  const esMenorEdadFormulario = useMemo(
    () => calcularEsMenorEdad(form.fechaNacimiento),
    [form.fechaNacimiento]
  );

  const puedeEditar = tienePermiso(user, PERMISOS.EDITAR_PERSONAS);
  const puedeDesactivar = tienePermiso(
    user,
    PERMISOS.CAMBIAR_ESTADO_PERSONAS
  );

  const tipoPersonaMap = useMemo(
    () => optionsMap(tipoPersonaOptions),
    [tipoPersonaOptions]
  );
  const municipioMap = useMemo(
    () => optionsMap(municipioCatalogoOptions),
    [municipioCatalogoOptions]
  );
  const barrioMap = useMemo(
    () => optionsMap(barrioCatalogoOptions),
    [barrioCatalogoOptions]
  );

  const personasFiltradas = useMemo(() => {
    const q = busqueda.trim().toLowerCase();

    const base = !q
      ? personas
      : personas.filter((persona) =>
          [
            persona.id,
            persona.numeroDocumento,
            persona.nombres,
            persona.apellidos,
            persona.telefono,
            persona.correo,
            persona.direccion,
            labelFromMap(tipoPersonaMap, persona.tipoPersonaId, ""),
            labelFromMap(municipioMap, persona.municipioId, ""),
            labelFromMap(barrioMap, persona.barrioId, ""),
          ]
            .filter(Boolean)
            .join(" ")
            .toLowerCase()
            .includes(q)
        );

    return ordenarPorIdAscendente(base);
  }, [personas, busqueda, tipoPersonaMap, municipioMap, barrioMap]);

  const totalRegistros = personasFiltradas.length;
  const totalPaginas = Math.max(1, Math.ceil(totalRegistros / registrosPorPagina));
  const indiceInicial = (paginaActual - 1) * registrosPorPagina;
  const indiceFinal = Math.min(indiceInicial + registrosPorPagina, totalRegistros);
  const paginasVisibles = obtenerPaginasVisibles(paginaActual, totalPaginas);

  const personasPaginadas = useMemo(
    () => personasFiltradas.slice(indiceInicial, indiceInicial + registrosPorPagina),
    [personasFiltradas, indiceInicial, registrosPorPagina]
  );

  useEffect(() => {
    cargarInicial();
  }, [router]);

  useEffect(() => {
    setPaginaActual(1);
  }, [busqueda, registrosPorPagina]);

  useEffect(() => {
    if (paginaActual > totalPaginas) {
      setPaginaActual(totalPaginas);
    }
  }, [paginaActual, totalPaginas]);

  useEffect(() => {
    async function cargarMunicipios() {
      if (!form.departamentoId) {
        setMunicipioOptions([]);
        setBarrioOptions([]);
        return;
      }

      const municipios = await fetchCatalogo(
        `/municipios/departamento/${form.departamentoId}`
      );

      setMunicipioOptions(municipios.map(toOption));

      if (!municipios.some((municipio) => String(municipio.id) === String(form.municipioId))) {
        setForm((prev) => ({ ...prev, municipioId: "", barrioId: "" }));
        setBarrioOptions([]);
      }
    }

    if (personaEditando) {
      cargarMunicipios();
    }
  }, [form.departamentoId, personaEditando]);

  useEffect(() => {
    async function cargarBarrios() {
      if (!form.municipioId) {
        setBarrioOptions([]);
        return;
      }

      const barrios = await fetchCatalogo(`/barrios/municipio/${form.municipioId}`);

      setBarrioOptions(barrios.map(toOption));

      if (!barrios.some((barrio) => String(barrio.id) === String(form.barrioId))) {
        setForm((prev) => ({ ...prev, barrioId: "" }));
      }
    }

    if (personaEditando) {
      cargarBarrios();
    }
  }, [form.municipioId, personaEditando]);

  async function cargarInicial() {
    try {
      setLoading(true);
      setError("");
      setMensaje("");

      const meRes = await fetch(`${API_URL_BASE}/auth/me`, {
        credentials: "include",
      });

      if (meRes.status === 401) {
        router.replace("/");
        return;
      }

      if (!meRes.ok) {
        router.replace("/");
        return;
      }

      const meData = await meRes.json();

      const puedeEntrar =
        tienePermiso(meData, PERMISOS.ACCEDER_PERSONAS) &&
        tienePermiso(meData, PERMISOS.VER_PERSONAS);

      if (!puedeEntrar) {
        router.replace("/inicio");
        return;
      }

      setUser(meData);

      await cargarCatalogosBase();
      await cargarPersonas();
    } catch (err) {
      console.error(err);
      setError(err.message || "Error cargando personas");
    } finally {
      setLoading(false);
    }
  }

  async function cargarCatalogosBase() {
    const [
      tiposDocumento,
      tiposPersona,
      nacionalidades,
      condiciones,
      ocupaciones,
      empresas,
      departamentos,
      municipios,
      barrios,
    ] = await Promise.all([
      fetchCatalogo("/tipos-documento/activos"),
      fetchCatalogo("/tipos-persona"),
      fetchCatalogo("/nacionalidades"),
      fetchCatalogo("/condiciones"),
      fetchCatalogo("/ocupaciones"),
      fetchCatalogo("/empresas"),
      fetchCatalogo("/departamentos"),
      fetchCatalogo("/municipios"),
      fetchCatalogo("/barrios"),
    ]);

    const opcionesDocumento = tiposDocumento
      .map(toDocumentoOption)
      .filter((item) => item.value && item.label);

    setTipoDocumentoOptions(
      opcionesDocumento.length > 0
        ? opcionesDocumento
        : FALLBACK_TIPO_DOCUMENTO_OPTIONS
    );
    setTipoPersonaOptions(tiposPersona.map(toOption));
    setNacionalidadOptions(nacionalidades.map(toOption));
    setCondicionOptions(condiciones.map(toOption));
    setOcupacionOptions(ocupaciones.map(toOption));
    setEmpresaOptions(empresas.map(toOption));
    setDepartamentoOptions(departamentos.map(toOption));
    setMunicipioCatalogoOptions(municipios.map(toOption));
    setBarrioCatalogoOptions(barrios.map(toOption));
  }

  async function cargarPersonas() {
    const personasRes = await fetch(`${API_URL_BASE}/personas/activos`, {
      credentials: "include",
    });

    if (personasRes.status === 401) {
      router.replace("/");
      return;
    }

    if (personasRes.status === 403) {
      router.replace("/inicio");
      return;
    }

    if (!personasRes.ok) {
      throw new Error("No se pudieron cargar las personas");
    }

    const personasData = await personasRes.json();
    const personasOrdenadas = ordenarPorIdAscendente(
      Array.isArray(personasData) ? personasData : []
    );

    setPersonas(personasOrdenadas);
  }

  async function abrirEdicion(persona) {
    if (!puedeEditar) {
      setError("No tienes permisos para editar personas");
      return;
    }

    setMensaje("");
    setError("");
    setPersonaEditando(persona);
    setForm(convertirPersonaAForm(persona));

    if (persona.departamentoId) {
      const municipios = await fetchCatalogo(
        `/municipios/departamento/${persona.departamentoId}`
      );
      setMunicipioOptions(municipios.map(toOption));
    } else {
      setMunicipioOptions([]);
    }

    if (persona.municipioId) {
      const barrios = await fetchCatalogo(`/barrios/municipio/${persona.municipioId}`);
      setBarrioOptions(barrios.map(toOption));
    } else {
      setBarrioOptions([]);
    }
  }

  function cerrarEdicion() {
    setPersonaEditando(null);
    setForm(FORM_INICIAL);
    setMunicipioOptions([]);
    setBarrioOptions([]);
    setSaving(false);
  }

  function abrirConfirmacionDesactivar(persona) {
    setMensaje("");
    setError("");
    setPersonaADesactivar(persona);
  }

  function cerrarConfirmacionDesactivar() {
    if (desactivando) return;
    setPersonaADesactivar(null);
  }

  function handleBusquedaChange(event) {
    setBusqueda(event.target.value);
    setPaginaActual(1);
  }

  function handleRegistrosPorPaginaChange(event) {
    setRegistrosPorPagina(Number(event.target.value));
    setPaginaActual(1);
  }

  function handleChange(event) {
    const { name, value, type, checked } = event.target;

    setForm((prev) => {
      const next = {
        ...prev,
        [name]: type === "checkbox" ? checked : value,
      };

      if (name === "departamentoId") {
        next.municipioId = "";
        next.barrioId = "";
      }

      if (name === "municipioId") {
        next.barrioId = "";
      }

      return next;
    });
  }

  function handleNumberChange(event) {
    const { name, value } = event.target;
    const numericValue = value === "" ? "" : Math.max(0, Number(value));

    setForm((prev) => ({
      ...prev,
      [name]: Number.isNaN(numericValue) ? "" : numericValue,
    }));
  }

  function validarEdicionPersona() {
    const telefono = String(form.telefono || "").trim();
    const correo = String(form.correo || "").trim();
    const correoAcudiente = String(form.correoAcudiente || "").trim();

    if (!telefono && !correo) {
      return "Debe registrar al menos un teléfono o un correo electrónico.";
    }

    if (correo && !EMAIL_PATTERN.test(correo)) {
      return "Ingrese un correo electrónico válido.";
    }

    if (correoAcudiente && !EMAIL_PATTERN.test(correoAcudiente)) {
      return "Ingrese un correo electrónico válido para el acudiente.";
    }

    const camposNumericos = [
      ["estrato", "estrato"],
      ["numeroPersonasACargo", "personas a cargo"],
      ["salario", "salario"],
    ];

    const campoNegativo = camposNumericos.find(([name]) => Number(form[name]) < 0);

    if (campoNegativo) {
      return `El campo ${campoNegativo[1]} no puede ser negativo.`;
    }

    return "";
  }

  async function guardarEdicion(event) {
    event.preventDefault();

    if (!personaEditando?.id) {
      setError("No hay una persona seleccionada");
      return;
    }

    if (!puedeEditar) {
      setError("No tienes permisos para editar personas");
      return;
    }

    const errorValidacion = validarEdicionPersona();

    if (errorValidacion) {
      setError(errorValidacion);
      return;
    }

    try {
      setSaving(true);
      setError("");
      setMensaje("");

      const payload = construirPayload(form, personaEditando.id);

      const res = await fetch(`${API_URL_BASE}/personas/${personaEditando.id}`, {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await leerRespuesta(res);

      if (res.status === 401) {
        router.replace("/");
        return;
      }

      if (res.status === 403) {
        router.replace("/inicio");
        return;
      }

      if (!res.ok) {
        throw new Error(
          getApiErrorDescription(
            data,
            getApiErrorTitle(data, "No se pudo actualizar la persona")
          )
        );
      }

      setMensaje("Persona actualizada correctamente");
      cerrarEdicion();
      await cargarPersonas();
    } catch (err) {
      console.error(err);
      setError(err.message || "Error actualizando persona");
    } finally {
      setSaving(false);
    }
  }

  async function confirmarDesactivarPersona() {
    if (!personaADesactivar?.id) return;

    if (!puedeDesactivar) {
      setError("No tienes permisos para desactivar personas");
      setPersonaADesactivar(null);
      return;
    }

    try {
      setDesactivando(true);
      setError("");
      setMensaje("");

      const res = await fetch(
        `${API_URL_BASE}/personas/${personaADesactivar.id}/desactivar`,
        {
          method: "PATCH",
          credentials: "include",
        }
      );

      const data = await leerRespuesta(res);

      if (res.status === 401) {
        router.replace("/");
        return;
      }

      if (res.status === 403) {
        router.replace("/inicio");
        return;
      }

      if (!res.ok) {
        throw new Error(
          getApiErrorDescription(
            data,
            getApiErrorTitle(data, "No se pudo desactivar la persona")
          )
        );
      }

      setMensaje("Persona desactivada correctamente");
      setPersonaADesactivar(null);
      await cargarPersonas();
    } catch (err) {
      console.error(err);
      setError(err.message || "Error desactivando persona");
    } finally {
      setDesactivando(false);
    }
  }

  if (loading) {
    return (
      <div className="rounded-xl border bg-card p-8 text-center text-muted-foreground">
        Cargando personas...
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {error && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/10 px-4 py-3 text-sm text-destructive">
          {error}
        </div>
      )}

      {mensaje && (
        <div className="rounded-lg border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-primary">
          {mensaje}
        </div>
      )}

      <div className="rounded-xl border bg-card p-5 space-y-4">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h3 className="text-xl font-bold">Personas registradas</h3>
            <p className="text-sm text-muted-foreground">
              Listado general de personas creadas desde recepción.
            </p>
          </div>

          <Button type="button" variant="outline" onClick={cargarInicial}>
            Actualizar
          </Button>
        </div>

        <div className="relative">
          <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
          <input
            value={busqueda}
            onChange={handleBusquedaChange}
            placeholder="Buscar por documento, nombre, teléfono, correo o dirección..."
            className="h-10 w-full rounded-md border bg-background pl-9 pr-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          />
        </div>

        <div className="flex flex-col gap-3 rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm md:flex-row md:items-center md:justify-between">
          <div className="text-muted-foreground">
            {totalRegistros === 0 ? (
              "No hay registros para mostrar."
            ) : (
              <>
                Mostrando{" "}
                <span className="font-semibold text-foreground">
                  {indiceInicial + 1}
                </span>{" "}
                a{" "}
                <span className="font-semibold text-foreground">
                  {indiceFinal}
                </span>{" "}
                de{" "}
                <span className="font-semibold text-foreground">
                  {totalRegistros}
                </span>{" "}
                personas, ordenadas por ID de menor a mayor.
              </>
            )}
          </div>

          <label className="flex items-center gap-2">
            <span className="text-muted-foreground">Registros por página:</span>
            <select
              value={registrosPorPagina}
              onChange={handleRegistrosPorPaginaChange}
              className="h-9 rounded-md border bg-background px-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {REGISTROS_POR_PAGINA_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="overflow-hidden rounded-lg border border-primary/30">
          <div className="max-h-[560px] overflow-auto">
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-primary/20">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">ID</th>
                  <th className="px-4 py-3 text-left font-medium">Persona</th>
                  <th className="px-4 py-3 text-left font-medium">Documento</th>
                  <th className="px-4 py-3 text-left font-medium">Contacto</th>
                  <th className="px-4 py-3 text-left font-medium">Ubicación</th>
                  <th className="px-4 py-3 text-left font-medium">Tipo</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>

              <tbody>
                {personasPaginadas.length === 0 ? (
                  <tr>
                    <td
                      colSpan={7}
                      className="px-4 py-8 text-center text-muted-foreground"
                    >
                      No hay personas para mostrar.
                    </td>
                  </tr>
                ) : (
                  personasPaginadas.map((persona) => (
                    <tr
                      key={persona.id}
                      className="border-t border-primary/20 transition hover:bg-primary/10"
                    >
                      <td className="px-4 py-3">{persona.id}</td>

                      <td className="px-4 py-3">
                        <div className="font-medium">
                          {nombreCompleto(persona) || "Sin nombre"}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          Nacimiento: {valorTexto(persona.fechaNacimiento)}
                        </div>
                      </td>

                      <td className="px-4 py-3">
                        <div>{valorTexto(persona.tipoDocumento)}</div>
                        <div className="text-xs text-muted-foreground">
                          {valorTexto(persona.numeroDocumento)}
                        </div>
                      </td>

                      <td className="px-4 py-3">
                        <div>{valorTexto(persona.telefono)}</div>
                        <div className="text-xs text-muted-foreground">
                          {valorTexto(persona.correo)}
                        </div>
                      </td>

                      <td className="px-4 py-3">
                        <div>
                          {labelFromMap(
                            municipioMap,
                            persona.municipioId,
                            persona.municipio || "N/A"
                          )}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {valorTexto(persona.direccion)}
                        </div>
                      </td>

                      <td className="px-4 py-3">
                        <span className="rounded-full border border-primary/30 bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">
                          {labelFromMap(
                            tipoPersonaMap,
                            persona.tipoPersonaId,
                            persona.tipoUsuario || "N/A"
                          )}
                        </span>
                      </td>

                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-2">
                          {puedeEditar && (
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              onClick={() => abrirEdicion(persona)}
                              className="border-primary/30 bg-primary/10 hover:bg-primary/20"
                            >
                              Editar
                            </Button>
                          )}

                          {puedeDesactivar && (
                            <Button
                              type="button"
                              variant="destructive"
                              size="sm"
                              disabled={persona.activo === false}
                              onClick={() => abrirConfirmacionDesactivar(persona)}
                              className="bg-destructive/20 text-destructive hover:bg-destructive/30 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                              {persona.activo === false ? "Inactivo" : "Desactivar"}
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <Pagination
          currentPage={paginaActual}
          totalPages={totalPaginas}
          onPageChange={setPaginaActual}
          pageSize={registrosPorPagina}
          onPageSizeChange={setRegistrosPorPagina}
          pageSizeOptions={REGISTROS_POR_PAGINA_OPTIONS}
          totalItems={totalRegistros}
        />
      </div>

      {personaEditando && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/70 px-4 backdrop-blur-sm">
          <div className="flex max-h-[92vh] w-full max-w-6xl flex-col overflow-hidden rounded-2xl border bg-card shadow-2xl">
            <div className="flex items-center justify-between border-b bg-primary/10 px-6 py-5">
              <div>
                <h3 className="text-xl font-bold">Editar persona</h3>
                <p className="text-sm text-muted-foreground">
                  Actualiza la información general de {nombreCompleto(personaEditando)}
                </p>
              </div>

              <button
                type="button"
                onClick={cerrarEdicion}
                className="rounded-full border bg-background p-2 text-muted-foreground transition hover:bg-muted hover:text-foreground"
              >
                <X className="size-5" />
              </button>
            </div>

            <form onSubmit={guardarEdicion} className="flex min-h-0 flex-1 flex-col">
              <div className="flex-1 overflow-auto p-6">
                <div className="space-y-6">
                  <Seccion titulo="Información básica">
                    <Select label="Tipo persona" name="tipoPersonaId" value={form.tipoPersonaId} onChange={handleChange} options={tipoPersonaOptions} />
                    <Select label="Tipo documento" name="tipoDocumento" value={form.tipoDocumento} onChange={handleChange} options={tipoDocumentoOptions} />
                    <Input label="Número documento" name="numeroDocumento" value={form.numeroDocumento} onChange={handleChange} />
                    <Input label="Fecha expedición" name="fechaExpedicion" type="date" value={form.fechaExpedicion} onChange={handleChange} />
                    <Input label="Ciudad expedición" name="ciudadExpedicion" value={form.ciudadExpedicion} onChange={handleChange} />
                    <Input label="Nombres" name="nombres" value={form.nombres} onChange={handleChange} />
                    <Input label="Apellidos" name="apellidos" value={form.apellidos} onChange={handleChange} />
                    <Input label="Nombre identitario" name="nombreIdentitario" value={form.nombreIdentitario} onChange={handleChange} />
                    <Select label="Pronombre" name="pronombre" value={form.pronombre} onChange={handleChange} options={PRONOMBRE_OPTIONS} />
                    <Select label="Sexo" name="sexo" value={form.sexo} onChange={handleChange} options={SEXO_OPTIONS} />
                    <Select label="Género" name="genero" value={form.genero} onChange={handleChange} options={GENERO_OPTIONS} />
                    <Select label="Orientación sexual" name="orientacionSexual" value={form.orientacionSexual} onChange={handleChange} options={ORIENTACION_OPTIONS} />
                    <Input label="Fecha nacimiento" name="fechaNacimiento" type="date" value={form.fechaNacimiento} onChange={handleChange} />
                  </Seccion>

                  <Seccion titulo="Contacto e identidad social">
                    <Input label="Teléfono" name="telefono" value={form.telefono} onChange={handleChange} />
                    <Input label="Correo" name="correo" type="email" value={form.correo} onChange={handleChange} />
                    <Select label="Nacionalidad" name="nacionalidadId" value={form.nacionalidadId} onChange={handleChange} options={nacionalidadOptions} />
                    <Select label="Estado civil" name="estadoCivil" value={form.estadoCivil} onChange={handleChange} options={ESTADO_CIVIL_OPTIONS} />
                    <Select label="Escolaridad" name="escolaridad" value={form.escolaridad} onChange={handleChange} options={ESCOLARIDAD_OPTIONS} />
                    <Input label="Grupo étnico" name="grupoEtnico" value={form.grupoEtnico} onChange={handleChange} />
                    <Select label="Condición actual" name="condicionActualId" value={form.condicionActualId} onChange={handleChange} options={condicionOptions} />
                    <Input label="Discapacidad" name="discapacidad" value={form.discapacidad} onChange={handleChange} />
                    <Input label="Caracterización PCD" name="caracterizacionPcd" value={form.caracterizacionPcd} onChange={handleChange} />
                  </Seccion>

                  <Seccion titulo="Ubicación y vivienda">
                    <Select label="Departamento" name="departamentoId" value={form.departamentoId} onChange={handleChange} options={departamentoOptions} />
                    <Select label="Municipio" name="municipioId" value={form.municipioId} onChange={handleChange} options={municipioOptions} disabled={!form.departamentoId} />
                    <Select label="Barrio" name="barrioId" value={form.barrioId} onChange={handleChange} options={barrioOptions} disabled={!form.municipioId} />
                    <Input label="Dirección" name="direccion" value={form.direccion} onChange={handleChange} />
                    <Input label="Comuna" name="comuna" value={form.comuna} onChange={handleChange} />
                    <Input label="Localidad" name="localidad" value={form.localidad} onChange={handleChange} />
                    <Input label="Estrato" name="estrato" type="number" min={0} value={form.estrato} onChange={handleNumberChange} />
                    <Input label="Tipo vivienda" name="tipoVivienda" value={form.tipoVivienda} onChange={handleChange} />
                    <Select label="Zona" name="zona" value={form.zona} onChange={handleChange} options={ZONA_OPTIONS} />
                    <Input label="Tenencia" name="tenencia" value={form.tenencia} onChange={handleChange} />
                    <Input label="Personas a cargo" name="numeroPersonasACargo" type="number" min={0} value={form.numeroPersonasACargo} onChange={handleNumberChange} />
                  </Seccion>

                  <Seccion titulo="Economía">
                    <Select label="Ocupación" name="ocupacionId" value={form.ocupacionId} onChange={handleChange} options={ocupacionOptions} />
                    <Select label="Empresa" name="empresaId" value={form.empresaId} onChange={handleChange} options={empresaOptions} />
                    <Input label="Salario" name="salario" type="number" min={0} value={form.salario} onChange={handleNumberChange} />
                    <Input label="Cargo" name="cargo" value={form.cargo} onChange={handleChange} />
                    <Input label="Dirección empresa" name="direccionEmpresa" value={form.direccionEmpresa} onChange={handleChange} />
                    <Input label="Teléfono empresa" name="telefonoEmpresa" value={form.telefonoEmpresa} onChange={handleChange} />
                  </Seccion>

                  {esMenorEdadFormulario && (
                    <Seccion titulo="Acudiente">
                      <Input label="Nombre acudiente" name="nombreCompletoAcudiente" value={form.nombreCompletoAcudiente} onChange={handleChange} />
                      <Input label="Relación acudiente" name="relacionAcudiente" value={form.relacionAcudiente} onChange={handleChange} />
                      <Input label="Teléfono acudiente" name="telefonoAcudiente" value={form.telefonoAcudiente} onChange={handleChange} />
                      <Input label="Correo acudiente" name="correoAcudiente" type="email" value={form.correoAcudiente} onChange={handleChange} />
                      <Input label="Dirección acudiente" name="direccionAcudiente" value={form.direccionAcudiente} onChange={handleChange} />
                    </Seccion>
                  )}

                  <Seccion titulo="Servicio">
                    <Input label="Cómo se enteró" name="comoSeEntero" value={form.comoSeEntero} onChange={handleChange} />
                    <Input label="Relación con universidad" name="relacionConUniversidad" value={form.relacionConUniversidad} onChange={handleChange} />
                  </Seccion>

                  <div className="rounded-xl border bg-muted/20 p-4">
                    <h4 className="mb-3 font-semibold">Condiciones adicionales</h4>

                    <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                      <Checkbox label="Sabe leer y escribir" name="sabeLeerEscribir" checked={form.sabeLeerEscribir} onChange={handleChange} />
                      <Checkbox label="Necesita ajuste PCD" name="necesitaAjustePcd" checked={form.necesitaAjustePcd} onChange={handleChange} />
                      <Checkbox label="Ingresos adicionales" name="ingresosAdicionales" checked={form.ingresosAdicionales} onChange={handleChange} />
                      <Checkbox label="Energía eléctrica" name="energiaElectrica" checked={form.energiaElectrica} onChange={handleChange} />
                      <Checkbox label="Acueducto" name="acueducto" checked={form.acueducto} onChange={handleChange} />
                      <Checkbox label="Alcantarillado" name="alcantarillado" checked={form.alcantarillado} onChange={handleChange} />
                    </div>
                  </div>
                </div>
              </div>

              <div className="flex justify-end gap-3 border-t bg-card px-6 py-4">
                <Button type="button" variant="outline" onClick={cerrarEdicion} disabled={saving}>
                  Cancelar
                </Button>

                <Button type="submit" disabled={saving}>
                  {saving ? "Guardando..." : "Guardar cambios"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      <ConfirmActionDialog
        open={Boolean(personaADesactivar)}
        title="Desactivar persona"
        description={`¿Deseas desactivar a "${
          nombreCompleto(personaADesactivar) || "esta persona"
        }"? Podrás reactivarla después desde la página de eliminación.`}
        confirmText="Desactivar"
        cancelText="Cancelar"
        loading={desactivando}
        onClose={cerrarConfirmacionDesactivar}
        onConfirm={confirmarDesactivarPersona}
      />
    </div>
  );
}

function Seccion({ titulo, children }) {
  return (
    <section className="rounded-xl border bg-background p-4">
      <h4 className="mb-4 border-b pb-2 font-semibold">{titulo}</h4>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
        {children}
      </div>
    </section>
  );
}

function Input({ label, name, value, onChange, type = "text", min }) {
  return (
    <label className="flex flex-col gap-1.5 text-sm">
      <span className="font-medium">{label}</span>
      <input
        type={type}
        name={name}
        value={value ?? ""}
        min={min}
        onChange={onChange}
        className="h-10 rounded-md border bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
      />
    </label>
  );
}

function Select({ label, name, value, onChange, options, disabled = false }) {
  return (
    <label className="flex flex-col gap-1.5 text-sm">
      <span className="font-medium">{label}</span>
      <select
        name={name}
        value={value ?? ""}
        onChange={onChange}
        disabled={disabled}
        className="h-10 rounded-md border bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60"
      >
        <option value="">Seleccione una opción</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function Checkbox({ label, name, checked, onChange }) {
  return (
    <label className="flex items-center gap-2 rounded-md border bg-background px-3 py-2 text-sm">
      <input
        type="checkbox"
        name={name}
        checked={Boolean(checked)}
        onChange={onChange}
        className="h-4 w-4"
      />
      <span>{label}</span>
    </label>
  );
}