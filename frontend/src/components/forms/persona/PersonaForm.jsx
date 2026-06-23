/**
 * Formulario de registro y edición de personas en el sistema jurídico.
 *
 * Implementa un formulario por pasos (wizard) con 6-7 secciones según si la
 * persona es menor de edad (agrega paso "Acudiente"). Valida campos obligatorios
 * por paso antes de avanzar usando `trigger()` de react-hook-form.
 *
 * Validaciones destacadas:
 * - Al avanzar desde "Identificación" valida los campos del paso actual.
 * - Al guardar verifica que exista al menos teléfono o correo (validación cruzada).
 * - Campos numéricos (estrato, personas a cargo) validados en react-hook-form y en HTML.
 *
 * @module components/forms/persona/PersonaForm
 */
"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useApiForm } from "@/hooks/useApiForm";
import { FormInput } from "../parts/FormInput";
import { FormCheckbox } from "../parts/FormCheckbox";
import { Button } from "@/components/ui/button";
import { API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";
import { toast } from "sonner";
import { maxNumberRule, nonNegativeNumberRule, optionalEmailRule } from "@/lib/form-validation";

const REQUIRED_MESSAGE = "El campo es obligatorio";
const MAX_ESTRATO = 7;
const MAX_PERSONAS_A_CARGO = 10;

const OPCIONES_VACIAS = [];

const FORM_DEFAULTS = {
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
  sabeLeerEscribir: true,
  discapacidad: "",
  caracterizacionPcd: "",
  necesitaAjustePcd: false,
  departamentoId: "",
  municipioId: "",
  barrioId: "",
  direccion: "",
  comuna: "",
  localidad: "",
  estrato: "",
  tipoVivienda: "",
  zona: "",
  tenencia: "",
  numeroPersonasACargo: "",
  ingresosAdicionales: false,
  energiaElectrica: false,
  acueducto: false,
  alcantarillado: false,
  ocupacionId: "",
  empresaId: "",
  salario: "",
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

const fallbackTipoDocumentoOptions = [
  { value: "CC", label: "Cédula de Ciudadanía" },
  { value: "TI", label: "Tarjeta de Identidad" },
  { value: "CE", label: "Cédula de Extranjería" },
  { value: "PA", label: "Pasaporte" },
];

const pronombreOptions = [
  { value: "Él", label: "Él" },
  { value: "Ella", label: "Ella" },
  { value: "Elle", label: "Elle" },
  { value: "Otro", label: "Otro" },
];

const sexoOptions = [
  { value: "Hombre", label: "Hombre" },
  { value: "Mujer", label: "Mujer" },
  { value: "Intersexual", label: "Intersexual" },
];

const generoOptions = [
  { value: "Masculino", label: "Masculino" },
  { value: "Femenino", label: "Femenino" },
  { value: "No binario", label: "No binario" },
  { value: "Transgénero", label: "Transgénero" },
  { value: "Otro", label: "Otro" },
];

const orientacionSexualOptions = [
  { value: "Heterosexual", label: "Heterosexual" },
  { value: "Homosexual", label: "Homosexual" },
  { value: "Bisexual", label: "Bisexual" },
  { value: "Pansexual", label: "Pansexual" },
  { value: "Asexual", label: "Asexual" },
  { value: "Otro", label: "Otro" },
];

const estadoCivilOptions = [
  { value: "Soltero/a", label: "Soltero/a" },
  { value: "Casado/a", label: "Casado/a" },
  { value: "Unión libre", label: "Unión libre" },
  { value: "Divorciado/a", label: "Divorciado/a" },
  { value: "Viudo/a", label: "Viudo/a" },
];

const escolaridadOptions = [
  { value: "Ninguna", label: "Ninguna" },
  { value: "Primaria", label: "Primaria" },
  { value: "Secundaria", label: "Secundaria" },
  { value: "Técnico", label: "Técnico" },
  { value: "Tecnólogo", label: "Tecnólogo" },
  { value: "Universitario", label: "Universitario" },
  { value: "Postgrado", label: "Postgrado" },
];

const zonaOptions = [
  { value: "Urbana", label: "Urbana" },
  { value: "Rural", label: "Rural" },
];

function requiredLabel(label) {
  return (
    <span className="inline-flex flex-wrap items-center gap-1">
      <span>{label}</span>
      <span className="text-red-500">*</span>
    </span>
  );
}

function optionalLabel(label) {
  return <span>{label}</span>;
}

/**
 * Convierte un registro de catálogo en una opción para select.
 * @param {Object} item - Registro del catálogo.
 * @returns {{value:string, label:string}} Opción para un campo select.
 */
function toOption(item) {
  return {
    value: String(item.id),
    label: item.nombre || item.descripcion || `Registro ${item.id}`,
  };
}

/**
 * Convierte datos de documento en una opción amigable para el select.
 * @param {Object} item - Datos del documento.
 * @returns {{value:string, label:string}} Opción de documento.
 */
function toDocumentoOption(item) {
  const value = item.codigo || item.abreviatura || item.nombre || item.descripcion || "";
  const label = item.nombre || item.descripcion || item.codigo || item.abreviatura || value;

  return { value, label };
}

/**
 * Normaliza los valores iniciales antes de usarlos en el formulario.
 * @param {Object} values - Valores iniciales recibidos.
 * @returns {Object} Valores preparados para el formulario.
 */
function toFormValues(values = {}) {
  return {
    ...FORM_DEFAULTS,
    ...values,
    tipoPersonaId: values.tipoPersonaId != null ? String(values.tipoPersonaId) : "",
    nacionalidadId: values.nacionalidadId != null ? String(values.nacionalidadId) : "",
    condicionActualId:
      values.condicionActualId != null ? String(values.condicionActualId) : "",
    departamentoId: values.departamentoId != null ? String(values.departamentoId) : "",
    municipioId: values.municipioId != null ? String(values.municipioId) : "",
    barrioId: values.barrioId != null ? String(values.barrioId) : "",
    ocupacionId: values.ocupacionId != null ? String(values.ocupacionId) : "",
    empresaId: values.empresaId != null ? String(values.empresaId) : "",
    sabeLeerEscribir: values.sabeLeerEscribir ?? true,
    necesitaAjustePcd: Boolean(values.necesitaAjustePcd),
    ingresosAdicionales: Boolean(values.ingresosAdicionales),
    energiaElectrica: Boolean(values.energiaElectrica),
    acueducto: Boolean(values.acueducto),
    alcantarillado: Boolean(values.alcantarillado),
    estrato: values.estrato ?? "",
    numeroPersonasACargo: values.numeroPersonasACargo ?? "",
    salario: values.salario ?? "",
  };
}

/**
 * Convierte un valor numérico vacío en null.
 * @param {unknown} value - Valor de entrada.
 * @returns {number|null} Número o null.
 */
function numberOrNull(value) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = Number(value);
  return Number.isNaN(number) ? null : number;
}

/**
 * Convierte texto vacío en null y elimina espacios innecesarios.
 * @param {unknown} value - Valor de texto recibido.
 * @returns {string|null} Texto limpio o null.
 */
function textOrNull(value) {
  const text = String(value ?? "").trim();
  return text === "" ? null : text;
}

/**
 * Construye un payload con tipos normalizados para el backend.
 * @param {Object} data - Datos del formulario.
 * @returns {Object} Payload normalizado.
 */
function construirPayload(data) {
  return {
    ...data,
    id: data.id ?? undefined,
    tipoPersonaId: numberOrNull(data.tipoPersonaId),
    nacionalidadId: numberOrNull(data.nacionalidadId),
    condicionActualId: numberOrNull(data.condicionActualId),
    departamentoId: numberOrNull(data.departamentoId),
    municipioId: numberOrNull(data.municipioId),
    barrioId: numberOrNull(data.barrioId),
    ocupacionId: numberOrNull(data.ocupacionId),
    empresaId: numberOrNull(data.empresaId),
    estrato: Number(data.estrato || 0),
    numeroPersonasACargo: Number(data.numeroPersonasACargo || 0),
    salario: Number(data.salario || 0),
    correo: textOrNull(data.correo),
    correoAcudiente: textOrNull(data.correoAcudiente),
    nombreCompletoAcudiente: textOrNull(data.nombreCompletoAcudiente),
    relacionAcudiente: textOrNull(data.relacionAcudiente),
    telefonoAcudiente: textOrNull(data.telefonoAcudiente),
    direccionAcudiente: textOrNull(data.direccionAcudiente),
  };
}

/**
 * Carga un catálogo desde la API.
 * @param {string} path - Ruta del catálogo relativa.
 * @returns {Promise<Array<any>>} Lista de registros.
 */
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

/**
 * Formulario para crear o editar una persona.
 * @param {{onSubmit:function, initialValues:Object}} props - Props del componente.
 * @returns {JSX.Element} Componente de formulario.
 */
export function PersonaForm({ onSubmit, initialValues }) {
  const router = useRouter();

  const initialValuesKey = useMemo(
    () => JSON.stringify(initialValues ?? {}),
    [initialValues]
  );

  const defaultFormValues = useMemo(
    () => toFormValues(initialValues ?? {}),
    [initialValuesKey]
  );

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    trigger,
    formState: { errors },
  } = useForm({
    defaultValues: defaultFormValues,
  });

  const { submit, isSubmitting } = useApiForm({
    endpoint: `${API_URL_BASE}/personas`,
  });

  const [checking, setChecking] = useState(true);
  const [pasoActual, setPasoActual] = useState(0);
  const [catalogoMensaje, setCatalogoMensaje] = useState("");
  const [tipoDocumentoOptions, setTipoDocumentoOptions] = useState(
    fallbackTipoDocumentoOptions
  );
  const [tipoPersonaOptions, setTipoPersonaOptions] = useState(OPCIONES_VACIAS);
  const [nacionalidadOptions, setNacionalidadOptions] = useState(OPCIONES_VACIAS);
  const [condicionOptions, setCondicionOptions] = useState(OPCIONES_VACIAS);
  const [ocupacionOptions, setOcupacionOptions] = useState(OPCIONES_VACIAS);
  const [empresaOptions, setEmpresaOptions] = useState(OPCIONES_VACIAS);
  const [departamentoOptions, setDepartamentoOptions] = useState(OPCIONES_VACIAS);
  const [municipioOptions, setMunicipioOptions] = useState(OPCIONES_VACIAS);
  const [barrioOptions, setBarrioOptions] = useState(OPCIONES_VACIAS);

  const formValues = watch();
  const departamentoId = watch("departamentoId");
  const municipioId = watch("municipioId");

  const esMenorEdad = useMemo(() => {
    if (!formValues.fechaNacimiento) return false;

    const nacimiento = new Date(formValues.fechaNacimiento);
    const hoy = new Date();

    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mes = hoy.getMonth() - nacimiento.getMonth();

    if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
      edad -= 1;
    }

    return edad < 18;
  }, [formValues.fechaNacimiento]);

  const pasos = useMemo(() => {
    const secciones = [
      "Identificación",
      "Identidad",
      "Contacto",
      "Vivienda",
      "Economía",
    ];

    if (esMenorEdad) {
      secciones.push("Acudiente");
    }

    secciones.push("Servicio");
    return secciones;
  }, [esMenorEdad]);

  const pasoActualNombre = pasos[pasoActual] || pasos[0];

  useEffect(() => {
    reset(defaultFormValues);
  }, [defaultFormValues, reset]);

  useEffect(() => {
    if (pasoActual >= pasos.length) {
      setPasoActual(Math.max(pasos.length - 1, 0));
    }
  }, [pasoActual, pasos.length]);

  useEffect(() => {
    if (!esMenorEdad) {
      setValue("nombreCompletoAcudiente", "");
      setValue("relacionAcudiente", "");
      setValue("telefonoAcudiente", "");
      setValue("correoAcudiente", "");
      setValue("direccionAcudiente", "");
    }
  }, [esMenorEdad, setValue]);

  useEffect(() => {
    async function iniciar() {
      await verificarSesion();
    }

    iniciar();
  }, []);

  useEffect(() => {
    async function cargarMunicipiosPorDepartamento() {
      if (!departamentoId) {
        setMunicipioOptions([]);
        setBarrioOptions([]);
        setValue("municipioId", "");
        setValue("barrioId", "");
        return;
      }

      const municipios = await fetchCatalogo(
        `/municipios/departamento/${departamentoId}`
      );

      setMunicipioOptions(municipios.map(toOption));

      if (!municipios.some((municipio) => String(municipio.id) === String(municipioId))) {
        setValue("municipioId", "");
        setValue("barrioId", "");
        setBarrioOptions([]);
      }
    }

    if (!checking) {
      cargarMunicipiosPorDepartamento();
    }
  }, [departamentoId]);

  useEffect(() => {
    async function cargarBarriosPorMunicipio() {
      if (!municipioId) {
        setBarrioOptions([]);
        setValue("barrioId", "");
        return;
      }

      const barrios = await fetchCatalogo(`/barrios/municipio/${municipioId}`);

      setBarrioOptions(barrios.map(toOption));

      if (!barrios.some((barrio) => String(barrio.id) === String(formValues.barrioId))) {
        setValue("barrioId", "");
      }
    }

    if (!checking) {
      cargarBarriosPorMunicipio();
    }
  }, [municipioId]);

  async function verificarSesion() {
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

      const puedeEntrarRecepcion = tienePermiso(
        user,
        PERMISOS.ACCEDER_RECEPCION
      );

      const puedeCrearPersonas = tienePermiso(user, PERMISOS.CREAR_PERSONAS);

      if (!puedeEntrarRecepcion || !puedeCrearPersonas) {
        router.replace("/inicio");
        return;
      }

      await cargarCatalogos();
    } catch (error) {
      console.error("Error verificando sesión", error);
      router.replace("/");
    } finally {
      setChecking(false);
    }
  }

  async function cargarCatalogos() {
    try {
      const [
        tiposDocumento,
        tiposPersona,
        nacionalidades,
        condiciones,
        ocupaciones,
        empresas,
        departamentos,
      ] = await Promise.all([
        fetchCatalogo("/tipos-documento/activos"),
        fetchCatalogo("/tipos-persona"),
        fetchCatalogo("/nacionalidades"),
        fetchCatalogo("/condiciones"),
        fetchCatalogo("/ocupaciones"),
        fetchCatalogo("/empresas"),
        fetchCatalogo("/departamentos"),
      ]);

      const opcionesDocumento = tiposDocumento
        .map(toDocumentoOption)
        .filter((item) => item.value && item.label);

      setTipoDocumentoOptions(
        opcionesDocumento.length > 0
          ? opcionesDocumento
          : fallbackTipoDocumentoOptions
      );
      setTipoPersonaOptions(tiposPersona.map(toOption));
      setNacionalidadOptions(nacionalidades.map(toOption));
      setCondicionOptions(condiciones.map(toOption));
      setOcupacionOptions(ocupaciones.map(toOption));
      setEmpresaOptions(empresas.map(toOption));
      setDepartamentoOptions(departamentos.map(toOption));

      if (
        tiposPersona.length === 0 ||
        nacionalidades.length === 0 ||
        condiciones.length === 0 ||
        ocupaciones.length === 0 ||
        empresas.length === 0 ||
        departamentos.length === 0
      ) {
        setCatalogoMensaje(
          "Algunos catálogos no se pudieron cargar. Verifica permisos o disponibilidad del backend antes de guardar."
        );
      }
    } catch (error) {
      console.error("Error cargando catálogos", error);
      setCatalogoMensaje(
        "No se pudieron cargar todos los catálogos necesarios para persona."
      );
      setTipoDocumentoOptions(fallbackTipoDocumentoOptions);
    }
  }

  /**
   * Normaliza los datos del formulario, valida la regla cruzada de contacto
   * (debe haber al menos teléfono o correo) y envía al backend.
   * Solo ejecuta el callback `onSubmit` si el backend respondió con éxito.
   *
   * @param {object} data - Datos del formulario validados por react-hook-form.
   * @returns {Promise<void>}
   */
  async function handleSubmitForm(data) {
    // Validación cruzada: debe existir al menos teléfono o correo
    const telefono = String(data.telefono ?? "").trim();
    const correo   = String(data.correo ?? "").trim();

    if (!telefono && !correo) {
      toast.error("Contacto requerido", {
        description: "Debe ingresar al menos un teléfono o un correo electrónico.",
      });
      setPasoActual(pasos.indexOf("Contacto"));
      return;
    }

    const datosNormalizados = esMenorEdad
      ? data
      : {
          ...data,
          nombreCompletoAcudiente: "",
          relacionAcudiente: "",
          telefonoAcudiente: "",
          correoAcudiente: "",
          direccionAcudiente: "",
        };

    const payload = construirPayload(datosNormalizados);

    const result = await submit(payload);

    if (result?.success) {
      limpiarFormulario();

      if (typeof onSubmit === "function") {
        onSubmit(result.data || payload);
      }
    }
  }

  function irAnterior() {
    setPasoActual((actual) => Math.max(actual - 1, 0));
  }

  /**
   * Valida los campos del paso actual con react-hook-form `trigger()`
   * y avanza al siguiente paso solo si todos son válidos.
   *
   * @returns {Promise<void>}
   */
  async function irSiguiente() {
    // Campos obligatorios agrupados por paso
    const camposPorPaso = {
      "Identificación": [
        "tipoPersonaId", "tipoDocumento", "numeroDocumento",
        "fechaExpedicion", "ciudadExpedicion", "nombres",
        "apellidos", "fechaNacimiento",
      ],
      "Identidad": [],
      "Contacto": [],   // validación cruzada en handleSubmitForm
      "Vivienda": [],
      "Economía": [],
      "Acudiente": [],
      "Servicio": [],
    };

    const campos = camposPorPaso[pasoActualNombre] ?? [];
    const esValido = campos.length === 0 || await trigger(campos);

    if (!esValido) return;

    setPasoActual((actual) => Math.min(actual + 1, pasos.length - 1));
  }

  function limpiarFormulario() {
    reset(FORM_DEFAULTS);
    setPasoActual(0);
    setMunicipioOptions([]);
    setBarrioOptions([]);
  }

  if (checking) {
    return <div className="text-center mt-10">Cargando...</div>;
  }

  return (
    <div className="space-y-6 p-6 bg-card rounded-xl shadow-sm border border-border">
      <div>
        <h3 className="text-xl font-bold">Registro de persona</h3>
        <p className="text-sm text-muted-foreground">
          Completa la información por grupos. Puedes cambiar de sección en cualquier momento.
        </p>
      </div>

      {catalogoMensaje && (
        <div className="rounded-lg border border-yellow-500/30 bg-yellow-500/10 px-4 py-3 text-sm text-yellow-700 dark:text-yellow-200">
          {catalogoMensaje}
        </div>
      )}

      <div className="grid grid-cols-2 gap-2 md:grid-cols-4 lg:grid-cols-8">
        {pasos.map((paso, index) => (
          <button
            key={paso}
            type="button"
            onClick={() => setPasoActual(index)}
            className={`rounded-lg border px-3 py-2 text-sm font-medium transition ${
              pasoActual === index
                ? "border-primary bg-primary text-primary-foreground"
                : "bg-background hover:bg-muted"
            }`}
          >
            {paso}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit(handleSubmitForm)} className="space-y-6">
        <div className="rounded-xl border bg-background p-5">
          {pasoActualNombre === "Identificación" && (
            <Seccion
              titulo="Identificación"
              descripcion="Datos principales para reconocer a la persona."
            >
              <FormSelectField
                name="tipoPersonaId"
                label={requiredLabel("Tipo de persona")}
                options={tipoPersonaOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="tipoDocumento"
                label={requiredLabel("Tipo de documento")}
                options={tipoDocumentoOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="numeroDocumento"
                label={requiredLabel("Número de documento")}
                placeholder="Ej: 1090123456"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="fechaExpedicion"
                label={requiredLabel("Fecha de expedición")}
                placeholder="Ej: 2020-01-15"
                type="date"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="ciudadExpedicion"
                label={requiredLabel("Ciudad de expedición")}
                placeholder="Ej: Cúcuta"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="nombres"
                label={requiredLabel("Nombres")}
                placeholder="Ej: María Fernanda"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="apellidos"
                label={requiredLabel("Apellidos")}
                placeholder="Ej: Pérez Gómez"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="fechaNacimiento"
                label={requiredLabel("Fecha de nacimiento")}
                placeholder="Ej: 1998-05-20"
                type="date"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />
            </Seccion>
          )}

          {pasoActualNombre === "Identidad" && (
            <Seccion
              titulo="Identidad y caracterización"
              descripcion="Información de identidad, características personales y condiciones diferenciales."
            >
              <FormInput
                name="nombreIdentitario"
                label={requiredLabel("Nombre identitario")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="pronombre"
                label={requiredLabel("Pronombre")}
                options={pronombreOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="sexo"
                label={requiredLabel("Sexo")}
                options={sexoOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="genero"
                label={requiredLabel("Género")}
                options={generoOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="orientacionSexual"
                label={requiredLabel("Orientación sexual")}
                options={orientacionSexualOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="nacionalidadId"
                label={requiredLabel("Nacionalidad")}
                options={nacionalidadOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="estadoCivil"
                label={requiredLabel("Estado civil")}
                options={estadoCivilOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="escolaridad"
                label={requiredLabel("Escolaridad")}
                options={escolaridadOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="grupoEtnico"
                label={requiredLabel("Grupo étnico")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="condicionActualId"
                label={requiredLabel("Condición actual")}
                options={condicionOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="discapacidad"
                label={requiredLabel("Discapacidad")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="caracterizacionPcd"
                label={requiredLabel("Caracterización PCD")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <div className="rounded-lg border bg-muted/20 p-3">
                <FormCheckbox
                  name="sabeLeerEscribir"
                  label="¿Sabe leer y escribir?"
                  register={register}
                  errors={errors}
                />
              </div>

              <div className="rounded-lg border bg-muted/20 p-3">
                <FormCheckbox
                  name="necesitaAjustePcd"
                  label="¿Necesita ajuste razonable (PCD)?"
                  register={register}
                  errors={errors}
                />
              </div>
            </Seccion>
          )}

          {pasoActualNombre === "Contacto" && (
            <Seccion
              titulo="Contacto"
              descripcion="Datos de comunicación. Si no hay correo, déjalo vacío para enviar null."
            >
              <FormInput
                name="telefono"
                label={optionalLabel("Teléfono")}
                placeholder="Ej: 3001234567"
                register={register}
                errors={errors}
                rules={{
                  validate: (value) =>
                    String(value || "").trim() ||
                    String(formValues.correo || "").trim() ||
                    "Debe registrar al menos un teléfono o un correo electrónico",
                }}
              />

              <FormInput
                name="correo"
                label={optionalLabel("Correo electrónico")}
                placeholder="Ej: persona@example.com"
                type="email"
                register={register}
                errors={errors}
                rules={{
                  ...optionalEmailRule(),
                  validate: {
                    formato: (value) =>
                      optionalEmailRule().validate(value),
                    contacto: (value) =>
                      String(value || "").trim() ||
                      String(formValues.telefono || "").trim() ||
                      "Debe registrar al menos un teléfono o un correo electrónico",
                  },
                }}
              />
            </Seccion>
          )}

          {pasoActualNombre === "Vivienda" && (
            <Seccion
              titulo="Información de vivienda"
              descripcion="Selecciona departamento, municipio y barrio desde catálogos activos."
            >
              <FormSelectField
                name="departamentoId"
                label={requiredLabel("Departamento")}
                options={departamentoOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="municipioId"
                label={requiredLabel("Municipio")}
                options={municipioOptions}
                register={register}
                errors={errors}
                disabled={!departamentoId}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="barrioId"
                label={requiredLabel("Barrio")}
                options={barrioOptions}
                register={register}
                errors={errors}
                disabled={!municipioId}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="direccion"
                label={requiredLabel("Dirección")}
                placeholder="Ej: Calle 10 # 5-20"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="comuna"
                label={requiredLabel("Comuna")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="localidad"
                label={requiredLabel("Localidad")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="estrato"
                label={requiredLabel("Estrato")}
                placeholder="Ej: 2"
                type="number"
                max={MAX_ESTRATO}
                min={0}
                register={register}
                errors={errors}
                rules={{
                  required: REQUIRED_MESSAGE,
                  ...nonNegativeNumberRule(),
                  ...maxNumberRule(MAX_ESTRATO),
                }}
              />

              <FormInput
                name="tipoVivienda"
                label={requiredLabel("Tipo de vivienda")}
                placeholder="Ej: Casa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="zona"
                label={requiredLabel("Zona")}
                options={zonaOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="tenencia"
                label={requiredLabel("Tenencia de la vivienda")}
                placeholder="Ej: Arriendo"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="numeroPersonasACargo"
                label={requiredLabel("Número de personas a cargo")}
                placeholder="Ej: 1"
                type="number"
                min={0}
                max={MAX_PERSONAS_A_CARGO}
                register={register}
                errors={errors}
                rules={{
                  required: REQUIRED_MESSAGE,
                  ...nonNegativeNumberRule(),
                  ...maxNumberRule(MAX_PERSONAS_A_CARGO),
                }}
              />

              <div className="rounded-lg border bg-muted/20 p-3">
                <FormCheckbox
                  name="ingresosAdicionales"
                  label="¿Recibe ingresos adicionales?"
                  register={register}
                  errors={errors}
                />
              </div>

              <div className="rounded-lg border bg-muted/20 p-3">
                <FormCheckbox
                  name="energiaElectrica"
                  label="¿Cuenta con energía eléctrica?"
                  register={register}
                  errors={errors}
                />
              </div>

              <div className="rounded-lg border bg-muted/20 p-3">
                <FormCheckbox
                  name="acueducto"
                  label="¿Cuenta con acueducto?"
                  register={register}
                  errors={errors}
                />
              </div>

              <div className="rounded-lg border bg-muted/20 p-3">
                <FormCheckbox
                  name="alcantarillado"
                  label="¿Cuenta con alcantarillado?"
                  register={register}
                  errors={errors}
                />
              </div>
            </Seccion>
          )}

          {pasoActualNombre === "Economía" && (
            <Seccion
              titulo="Aspectos económicos"
              descripcion="Datos laborales y económicos de la persona."
            >
              <FormSelectField
                name="ocupacionId"
                label={requiredLabel("Ocupación")}
                options={ocupacionOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormSelectField
                name="empresaId"
                label={requiredLabel("Empresa donde labora")}
                options={empresaOptions}
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="salario"
                label={requiredLabel("Salario")}
                placeholder="Ej: 0"
                type="number"
                step={10000}
                min={0}
                register={register}
                errors={errors}
                rules={{
                  required: REQUIRED_MESSAGE,
                  ...nonNegativeNumberRule(),
                }}
              />

              <FormInput
                name="cargo"
                label={requiredLabel("Cargo")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="direccionEmpresa"
                label={requiredLabel("Dirección de la empresa")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="telefonoEmpresa"
                label={requiredLabel("Teléfono de la empresa")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />
            </Seccion>
          )}

          {pasoActualNombre === "Acudiente" && (
            <Seccion
              titulo="Datos del acudiente"
              descripcion={
                esMenorEdad
                  ? "La persona es menor de edad. Los datos del acudiente son necesarios."
                  : "Estos datos aplican principalmente cuando la persona es menor de edad."
              }
            >
              <FormInput
                name="nombreCompletoAcudiente"
                label={optionalLabel("Nombre completo")}
                placeholder="Ej: Ana Pérez"
                register={register}
                errors={errors}
              />

              <FormInput
                name="relacionAcudiente"
                label={optionalLabel("Relación / parentesco")}
                placeholder="Ej: Madre"
                register={register}
                errors={errors}
              />

              <FormInput
                name="telefonoAcudiente"
                label={optionalLabel("Teléfono")}
                placeholder="Ej: 3001234567"
                register={register}
                errors={errors}
              />

              <FormInput
                name="correoAcudiente"
                label={optionalLabel("Correo electrónico")}
                placeholder="Ej: persona@example.com"
                type="email"
                register={register}
                errors={errors}
                rules={optionalEmailRule()}
              />

              <FormInput
                name="direccionAcudiente"
                label={optionalLabel("Dirección")}
                placeholder="Ej: Calle 10 # 5-20"
                register={register}
                errors={errors}
              />
            </Seccion>
          )}

          {pasoActualNombre === "Servicio" && (
            <Seccion
              titulo="Información del servicio"
              descripcion="Información sobre cómo llegó la persona al servicio."
            >
              <FormInput
                name="comoSeEntero"
                label={requiredLabel("¿Cómo se enteró de nuestros servicios?")}
                placeholder="Ej: Universidad"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />

              <FormInput
                name="relacionConUniversidad"
                label={requiredLabel("Relación con la Universidad")}
                placeholder="Ej: No informa"
                register={register}
                errors={errors}
                rules={{ required: REQUIRED_MESSAGE }}
              />
            </Seccion>
          )}
        </div>

        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={irAnterior}
              disabled={pasoActualNombre === "Identificación" || isSubmitting}
            >
              <ChevronLeft className="mr-1 size-4" />
              Anterior
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={irSiguiente}
              disabled={pasoActual === pasos.length - 1 || isSubmitting}
            >
              Siguiente
              <ChevronRight className="ml-1 size-4" />
            </Button>
          </div>

          <div className="flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={limpiarFormulario}
              disabled={isSubmitting}
            >
              Limpiar formulario
            </Button>

            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Guardando..." : "Guardar Persona"}
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
}

function Seccion({ titulo, descripcion, children }) {
  return (
    <section className="space-y-4">
      <div>
        <h3 className="text-lg font-semibold border-b pb-2">{titulo}</h3>
        {descripcion && (
          <p className="mt-2 text-sm text-muted-foreground">{descripcion}</p>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {children}
      </div>
    </section>
  );
}

function FormSelectField({
  name,
  label,
  options,
  register,
  errors,
  rules,
  disabled = false,
}) {
  return (
    <div className="flex flex-col gap-1.5 w-full">
      <label htmlFor={name} className="text-sm font-medium leading-none">
        {label}
      </label>

      <select
        id={name}
        defaultValue=""
        disabled={disabled}
        {...register(name, rules)}
        className={`flex h-9 w-full rounded-lg border bg-background px-2.5 py-1 text-sm disabled:cursor-not-allowed disabled:opacity-60 ${
          errors?.[name] ? "border-red-500" : ""
        }`}
      >
        <option value="">Seleccione una opción</option>

        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>

      {errors?.[name] && (
        <p className="text-xs text-red-500">{errors[name]?.message}</p>
      )}
    </div>
  );
}
