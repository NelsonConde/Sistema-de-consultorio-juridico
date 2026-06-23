import React from "react";
import { useForm } from "react-hook-form";
import { useApiForm } from "@/hooks/useApiForm";
import { FormInput } from "../parts/FormInput";
import { FormSelect } from "../parts/FormSelect";
import { FormCheckbox } from "../parts/FormCheckbox";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { API_URL_BASE } from "@/lib/config";

/**
 * Formulario para crear o editar conciliadores.
 * @param {{onSuccess:function, initialValues:Object}} props - Props del componente.
 * @returns {JSX.Element} Componente de formulario de conciliador.
 */
export function ConciliadorForm({ onSuccess, initialValues = {} }) {
  const defaultFormValues = {
    tipoUsuario: "conciliador",
    sabeLeerEscribir: true,
    necesitaAjustePcd: false,
    ingresosAdicionales: false,
    energiaElectrica: false,
    acueducto: false,
    alcantarillado: false,
    ...initialValues,
  };

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({ defaultValues: defaultFormValues });

  const { submit, isSubmitting } = useApiForm({
    endpoint: `${API_URL_BASE}/personas`,
  });

  const REQUIRED_MESSAGE = "El campo es obligatorio";

  // ── Opciones de selects ──────────────────────────────────────────────────

  const tipoDocumentoOptions = [
    { value: "CC", label: "Cédula de Ciudadanía" },
    { value: "TI", label: "Tarjeta de Identidad" },
    { value: "CE", label: "Cédula de Extranjería" },
    { value: "PA", label: "Pasaporte" },
    { value: "RC", label: "Registro Civil" },
  ];

  const pronombreOptions = [
    { value: "el", label: "Él" },
    { value: "ella", label: "Ella" },
    { value: "elle", label: "Elle" },
    { value: "otro", label: "Otro" },
  ];

  const sexoOptions = [
    { value: "hombre", label: "Hombre" },
    { value: "mujer", label: "Mujer" },
    { value: "intersexual", label: "Intersexual" },
  ];

  const generoOptions = [
    { value: "masculino", label: "Masculino" },
    { value: "femenino", label: "Femenino" },
    { value: "no_binario", label: "No Binario" },
    { value: "transgenero", label: "Transgénero" },
    { value: "otro", label: "Otro" },
  ];

  const orientacionSexualOptions = [
    { value: "heterosexual", label: "Heterosexual" },
    { value: "homosexual", label: "Homosexual" },
    { value: "bisexual", label: "Bisexual" },
    { value: "pansexual", label: "Pansexual" },
    { value: "asexual", label: "Asexual" },
    { value: "otro", label: "Otro" },
  ];

  const estadoCivilOptions = [
    { value: "soltero", label: "Soltero/a" },
    { value: "casado", label: "Casado/a" },
    { value: "union_libre", label: "Unión Libre" },
    { value: "divorciado", label: "Divorciado/a" },
    { value: "viudo", label: "Viudo/a" },
  ];

  const escolaridadOptions = [
    { value: "ninguna", label: "Ninguna" },
    { value: "primaria", label: "Primaria" },
    { value: "secundaria", label: "Secundaria" },
    { value: "tecnico", label: "Técnico" },
    { value: "tecnologo", label: "Tecnólogo" },
    { value: "universitario", label: "Universitario" },
    { value: "postgrado", label: "Postgrado" },
  ];

  const grupoEtnicoOptions = [
    { value: "ninguno", label: "Ninguno" },
    { value: "indigena", label: "Indígena" },
    { value: "afrodescendiente", label: "Afrodescendiente" },
    { value: "raizal", label: "Raizal" },
    { value: "palenquero", label: "Palenquero" },
    { value: "rom", label: "Rom / Gitano" },
    { value: "otro", label: "Otro" },
  ];

  const condicionActualOptions = [
    { value: "ninguna", label: "Ninguna" },
    { value: "victima", label: "Víctima del conflicto" },
    { value: "desplazado", label: "Desplazado" },
    { value: "discapacitado", label: "Persona con discapacidad" },
    { value: "habitante_calle", label: "Habitante de calle" },
    { value: "adulto_mayor", label: "Adulto mayor" },
    { value: "otro", label: "Otro" },
  ];

  const discapacidadOptions = [
    { value: "ninguna", label: "Ninguna" },
    { value: "visual", label: "Visual" },
    { value: "auditiva", label: "Auditiva" },
    { value: "motriz", label: "Motriz" },
    { value: "cognitiva", label: "Cognitiva" },
    { value: "multiple", label: "Múltiple" },
    { value: "otra", label: "Otra" },
  ];

  const zonaOptions = [
    { value: "urbana", label: "Urbana" },
    { value: "rural", label: "Rural" },
  ];

  const tipoViviendaOptions = [
    { value: "propia", label: "Propia" },
    { value: "arrendada", label: "Arrendada" },
    { value: "familiar", label: "Familiar" },
    { value: "otro", label: "Otro" },
  ];

  const tenenciaOptions = [
    { value: "propietario", label: "Propietario" },
    { value: "arrendatario", label: "Arrendatario" },
    { value: "familiar", label: "Familiar / Cedida" },
    { value: "otro", label: "Otro" },
  ];

  const relacionUniversidadOptions = [
    { value: "estudiante", label: "Estudiante" },
    { value: "docente", label: "Docente" },
    { value: "administrativo", label: "Administrativo" },
    { value: "egresado", label: "Egresado" },
    { value: "externo", label: "Externo" },
  ];

  const comoSeEnteroOptions = [
    { value: "redes_sociales", label: "Redes Sociales" },
    { value: "universidad", label: "Universidad" },
    { value: "recomendacion", label: "Recomendación" },
    { value: "pagina_web", label: "Página Web" },
    { value: "otro", label: "Otro" },
  ];

  // ── Submit ───────────────────────────────────────────────────────────────

  const handleSubmitForm = async (data) => {
    const result = await submit(data);
    if (result?.success && onSuccess) {
      onSuccess(result.data);
    }
  };

  // ── Render ───────────────────────────────────────────────────────────────

  return (
  <div className="space-y-8">

    {/* HEADER VISUAL */}
    <div className="relative overflow-hidden rounded-2xl border bg-gradient-to-r from-blue-600/10 via-indigo-500/10 to-purple-500/10 p-6">
      <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-600 via-indigo-500 to-purple-600" />

      <h2 className="text-2xl font-bold tracking-tight">
        Registro de Conciliador
      </h2>

      <p className="text-muted-foreground text-sm mt-1 max-w-2xl">
        Complete la información del conciliador. Los campos marcados con{" "}
        <span className="text-red-500">*</span> son obligatorios.
      </p>
    </div>

    {/* CONTENEDOR PRINCIPAL */}
    <div className="space-y-6">

      {/* ───────────── SECCIÓN ───────────── */}
      <section className="rounded-xl border bg-card p-6 shadow-sm space-y-6">
        <h3 className="text-lg font-semibold flex items-center gap-2">
          Información Básica
          <span className="h-1 w-6 bg-blue-600 rounded-full" />
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <input type="hidden" {...register("tipoUsuario")} />

          <FormSelect
            name="tipoDocumento"
            label="Tipo de Documento *"
            options={tipoDocumentoOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="numeroDocumento"
            label="Número de Documento *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="fechaExpedicion"
            label="Fecha de Expedición *"
            type="date"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="ciudadExpedicion"
            label="Ciudad de Expedición *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="nombres"
            label="Nombres *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="apellidos"
            label="Apellidos *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="nombreIdentitario"
            label="Nombre Identitario *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="pronombre"
            label="Pronombre *"
            options={pronombreOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="sexo"
            label="Sexo *"
            options={sexoOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="genero"
            label="Género *"
            options={generoOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="orientacionSexual"
            label="Orientación Sexual *"
            options={orientacionSexualOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="fechaNacimiento"
            label="Fecha de Nacimiento *"
            type="date"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="telefono"
            label="Teléfono / Celular *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="correo"
            label="Correo Electrónico *"
            type="email"
            register={register}
            errors={errors}
            rules={{
              required: REQUIRED_MESSAGE,
              pattern: {
                value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                message: "Correo electrónico no válido",
              },
            }}
          />
          <FormInput
            name="nacionalidad"
            label="Nacionalidad *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="estadoCivil"
            label="Estado Civil *"
            options={estadoCivilOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="escolaridad"
            label="Escolaridad *"
            options={escolaridadOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="grupoEtnico"
            label="Grupo Étnico *"
            options={grupoEtnicoOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="condicionActual"
            label="Condición Actual *"
            options={condicionActualOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="discapacidad"
            label="Discapacidad *"
            options={discapacidadOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="caracterizacionPcd"
            label="Caracterización P.C.D"
            placeholder="No informa"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
        </div>

        <div className="flex flex-wrap gap-6 mt-2 p-4 rounded-lg bg-muted/20">
          <FormCheckbox
            name="sabeLeerEscribir"
            label="¿Sabe leer y escribir?"
            register={register}
            errors={errors}
          />
          <FormCheckbox
            name="necesitaAjustePcd"
            label="¿Necesita ajuste razonable (PCD)?"
            register={register}
            errors={errors}
          />
        </div>
      </section>

      <Separator />

      {/* ── Sección 2: Información de Vivienda ── */}
      <section className="space-y-4">
        <h3 className="text-lg font-semibold border-b pb-2">
          Información de Vivienda
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <FormInput
            name="departamento"
            label="Departamento *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="municipio"
            label="Municipio *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="barrio"
            label="Barrio *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="direccion"
            label="Dirección *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="comuna"
            label="Comuna *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="localidad"
            label="Localidad"
            placeholder="No informa"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="estrato"
            label="Estrato *"
            type="number"
            min={1}
            max={6}
            register={register}
            errors={errors}
            rules={{
              required: REQUIRED_MESSAGE,
              min: { value: 1, message: "Mínimo estrato 1" },
              max: { value: 6, message: "Máximo estrato 6" },
            }}
          />
          <FormSelect
            name="tipoVivienda"
            label="Tipo de Vivienda *"
            options={tipoViviendaOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="zona"
            label="Zona *"
            options={zonaOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="tenencia"
            label="Tenencia *"
            options={tenenciaOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="numeroPersonasACargo"
            label="N.° Personas a Cargo *"
            type="number"
            min={0}
            max={20}
            register={register}
            errors={errors}
            rules={{
              required: REQUIRED_MESSAGE,
              min: { value: 0, message: "No puede ser negativo" },
            }}
          />
        </div>

        <div className="flex flex-wrap gap-6 mt-2 p-4 rounded-lg bg-muted/20">
          <FormCheckbox
            name="ingresosAdicionales"
            label="¿Tiene ingresos adicionales?"
            register={register}
            errors={errors}
          />
          <span className="text-sm font-medium self-center text-muted-foreground">
            Su vivienda cuenta con:
          </span>
          <FormCheckbox
            name="energiaElectrica"
            label="Energía eléctrica"
            register={register}
            errors={errors}
          />
          <FormCheckbox
            name="acueducto"
            label="Acueducto"
            register={register}
            errors={errors}
          />
          <FormCheckbox
            name="alcantarillado"
            label="Alcantarillado"
            register={register}
            errors={errors}
          />
        </div>
      </section>

      <Separator />

      {/* ── Sección 3: Aspectos Económicos ── */}
      <section className="space-y-4">
        <h3 className="text-lg font-semibold border-b pb-2">
          Aspectos Económicos
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <FormInput
            name="ocupacion"
            label="Ocupación *"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="empresa"
            label="Empresa"
            placeholder="No informa"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="salario"
            label="Salario *"
            type="number"
            min={0}
            step={10000}
            register={register}
            errors={errors}
            rules={{
              required: REQUIRED_MESSAGE,
              min: { value: 0, message: "No puede ser negativo" },
            }}
          />
          <FormInput
            name="cargo"
            label="Cargo"
            placeholder="No informa"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="direccionEmpresa"
            label="Dirección de la Empresa"
            placeholder="No informa"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormInput
            name="telefonoEmpresa"
            label="Teléfono de la Empresa"
            placeholder="No informa"
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
        </div>
      </section>

      <Separator />

      {/* ── Sección 4: Datos del Acudiente ── */}
      <section className="space-y-4">
        <h3 className="text-lg font-semibold border-b pb-2">
          Datos del Acudiente u Otro Contacto
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <FormInput
            name="nombreCompletoAcudiente"
            label="Nombres y Apellidos"
            placeholder="No informa"
            register={register}
            errors={errors}
          />
          <FormInput
            name="relacionAcudiente"
            label="Parentesco"
            placeholder="No informa"
            register={register}
            errors={errors}
          />
          <FormInput
            name="telefonoAcudiente"
            label="Teléfono / Celular"
            placeholder="No informa"
            register={register}
            errors={errors}
          />
          <FormInput
            name="correoAcudiente"
            label="Correo Electrónico"
            type="email"
            placeholder="No informa"
            register={register}
            errors={errors}
          />
          <FormInput
            name="direccionAcudiente"
            label="Dirección del Acudiente"
            placeholder="No informa"
            register={register}
            errors={errors}
          />
        </div>
      </section>

      <Separator />

      {/* ── Sección 5: Información del Servicio ── */}
      <section className="space-y-4">
        <h3 className="text-lg font-semibold border-b pb-2">
          Información del Servicio
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <FormSelect
            name="comoSeEntero"
            label="¿Cómo se enteró del servicio? *"
            options={comoSeEnteroOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
          <FormSelect
            name="relacionConUniversidad"
            label="Relación con la Universidad *"
            options={relacionUniversidadOptions}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED_MESSAGE }}
          />
        </div>
      </section>

      {/* ── Acciones ── */}
      <div className="flex justify-end gap-3 pt-2">
        <Button
          type="button"
          variant="outline"
          onClick={() => reset(defaultFormValues)}
          disabled={isSubmitting}
        >
          Limpiar
        </Button>
        <Button
          type="button"
          onClick={handleSubmit(handleSubmitForm)}
          disabled={isSubmitting}
        >
          {isSubmitting ? "Guardando..." : "Guardar Conciliador"}
        </Button>
      </div>
    </div>
  </div>);
}
