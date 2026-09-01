"use client"

import { apiClient } from "@/lib/apiClient";
/**
 * List and table handling.
 *
 * Handles list pagination consistently.
 * Form handling.
 *
 * @module components/forms/persona/PersonasForm
 */
;

import React, { useEffect, useMemo, useState } from "react";
import { Search, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { API_URL_BASE } from "@/lib/config";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { useRouter } from "next/navigation";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";
import { getApiErrorDescription, getApiErrorTitle, requireResourceVersion } from "@/lib/api";
import { buscarPersonasActivas, obtenerPersonaDetalle } from "@/lib/personasApi";
import { DIGITS_PATTERN, EMAIL_PATTERN } from "@/lib/form-validation";

import {
  ESCOLARIDAD_OPTIONS,
  ESTADO_CIVIL_OPTIONS,
  FALLBACK_TIPO_DOCUMENTO_OPTIONS,
  FORM_INICIAL,
  GENERO_OPTIONS,
  ORIENTACION_OPTIONS,
  PRONOMBRE_OPTIONS,
  REGISTROS_POR_PAGINA_OPTIONS,
  SEXO_OPTIONS,
  ZONA_OPTIONS,
} from "./personas.constants";
import {
  calcularEsMenorEdad,
  construirPayload,
  convertirPersonaAForm,
  nombreCompleto,
  textOrNull,
  toDocumentoOption,
  toOption,
} from "./personas.utils";
import { Checkbox, Input, Seccion, Select } from "./PersonasFormParts";
import { fetchCatalogo, leerRespuesta } from "./personas.service";

export function PersonasForm() {
  const [user, setUser] = useState(null);
  const [personas, setPersonas] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [busquedaAplicada, setBusquedaAplicada] = useState("");
  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);
  const [totalRegistros, setTotalRegistros] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [cargandoLista, setCargandoLista] = useState(false);
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

  const indiceInicial =
    totalRegistros === 0 ? 0 : (paginaActual - 1) * registrosPorPagina;
  const indiceFinal =
    totalRegistros === 0
      ? 0
      : Math.min(paginaActual * registrosPorPagina, totalRegistros);

  useEffect(() => {
    cargarInicial();
  }, [router]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setPaginaActual(1);
      setBusquedaAplicada(busqueda.trim());
    }, 350);

    return () => window.clearTimeout(timeoutId);
  }, [busqueda]);

  useEffect(() => {
    if (!user) return;

    const controller = new AbortController();
    cargarPersonas({ signal: controller.signal });

    return () => controller.abort();
  }, [user, busquedaAplicada, paginaActual, registrosPorPagina]);

  useEffect(() => {
    if (totalPaginas > 0 && paginaActual > totalPaginas) {
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

      const meRes = await apiClient.request(`${API_URL_BASE}/auth/me`, {
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
        : FALLBACK_TIPO_DOCUMENTO_OPTIONS
    );
    setTipoPersonaOptions(tiposPersona.map(toOption));
    setNacionalidadOptions(nacionalidades.map(toOption));
    setCondicionOptions(condiciones.map(toOption));
    setOcupacionOptions(ocupaciones.map(toOption));
    setEmpresaOptions(empresas.map(toOption));
    setDepartamentoOptions(departamentos.map(toOption));
  }

  async function cargarPersonas({ signal } = {}) {
    try {
      setCargandoLista(true);
      setError("");

      const resultado = await buscarPersonasActivas({
        search: busquedaAplicada,
        page: paginaActual,
        size: registrosPorPagina,
        signal,
      });

      if (
        resultado.totalPages > 0 &&
        paginaActual > resultado.totalPages
      ) {
        setPaginaActual(resultado.totalPages);
        return;
      }

      setPersonas(resultado.content);
      setTotalRegistros(resultado.totalElements);
      setTotalPaginas(resultado.totalPages);
    } catch (err) {
      if (err.name === "AbortError") return;

      if (err.status === 401) {
        router.replace("/");
        return;
      }

      if (err.status === 403) {
        router.replace("/inicio");
        return;
      }

      setPersonas([]);
      setTotalRegistros(0);
      setTotalPaginas(0);
      setError(err.message || "No se pudieron cargar las personas");
    } finally {
      setCargandoLista(false);
    }
  }

  async function abrirEdicion(personaResumen) {
    if (!puedeEditar) {
      setError("No tienes permisos para editar personas");
      return;
    }

    try {
      setMensaje("");
      setError("");

      const persona = await obtenerPersonaDetalle(personaResumen.id);

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
        const barrios = await fetchCatalogo(
          `/barrios/municipio/${persona.municipioId}`
        );
        setBarrioOptions(barrios.map(toOption));
      } else {
        setBarrioOptions([]);
      }
    } catch (err) {
      if (err.status === 401) {
        router.replace("/");
        return;
      }

      if (err.status === 403) {
        setError("No tienes permisos para consultar esta persona");
        return;
      }

      if (err.status === 404) {
        setError("La persona no está disponible para consulta o edición");
        return;
      }

      setError(err.message || "No se pudo abrir la persona");
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
    const requerido = [
      ["tipoPersonaId", "tipo de persona"],
      ["tipoDocumento", "tipo de documento"],
      ["numeroDocumento", "número de documento"],
      ["fechaExpedicion", "fecha de expedición"],
      ["ciudadExpedicion", "ciudad de expedición"],
      ["nombres", "nombres"],
      ["apellidos", "apellidos"],
      ["nombreIdentitario", "nombre identitario"],
      ["pronombre", "pronombre"],
      ["sexo", "sexo"],
      ["genero", "género"],
      ["orientacionSexual", "orientación sexual"],
      ["fechaNacimiento", "fecha de nacimiento"],
      ["nacionalidadId", "nacionalidad"],
      ["estadoCivil", "estado civil"],
      ["escolaridad", "escolaridad"],
      ["grupoEtnico", "grupo étnico"],
      ["condicionActualId", "condición actual"],
      ["discapacidad", "discapacidad"],
      ["caracterizacionPcd", "caracterización PCD"],
      ["departamentoId", "departamento"],
      ["municipioId", "municipio"],
      ["barrioId", "barrio"],
      ["direccion", "dirección"],
      ["comuna", "comuna"],
      ["localidad", "localidad"],
      ["estrato", "estrato"],
      ["tipoVivienda", "tipo de vivienda"],
      ["zona", "zona"],
      ["tenencia", "tenencia"],
      ["numeroPersonasACargo", "personas a cargo"],
      ["ocupacionId", "ocupación"],
      ["empresaId", "empresa"],
      ["salario", "salario"],
      ["cargo", "cargo"],
      ["direccionEmpresa", "dirección de la empresa"],
      ["telefonoEmpresa", "teléfono de la empresa"],
      ["comoSeEntero", "cómo se enteró del servicio"],
      ["relacionConUniversidad", "relación con la universidad"],
    ];

    const faltante = requerido.find(([name]) => String(form[name] ?? "").trim() === "");
    if (faltante) {
      return `El campo ${faltante[1]} es obligatorio.`;
    }

    const telefono = String(form.telefono || "").trim();
    const correo = String(form.correo || "").trim();
    const telefonoEmpresa = String(form.telefonoEmpresa || "").trim();
    const telefonoAcudiente = String(form.telefonoAcudiente || "").trim();
    const correoAcudiente = String(form.correoAcudiente || "").trim();

    if (!telefono && !correo) {
      return "Debe registrar al menos un teléfono o un correo electrónico.";
    }

    if (telefono && !DIGITS_PATTERN.test(telefono)) {
      return "El teléfono solo puede contener números.";
    }

    if (!DIGITS_PATTERN.test(telefonoEmpresa)) {
      return "El teléfono de la empresa solo puede contener números.";
    }

    if (telefonoAcudiente && !DIGITS_PATTERN.test(telefonoAcudiente)) {
      return "El teléfono del acudiente solo puede contener números.";
    }

    if (correo && !EMAIL_PATTERN.test(correo)) {
      return "Ingrese un correo electrónico válido.";
    }

    if (correoAcudiente && !EMAIL_PATTERN.test(correoAcudiente)) {
      return "Ingrese un correo electrónico válido para el acudiente.";
    }

    const limites = {
      tipoDocumento: 10,
      numeroDocumento: 30,
      ciudadExpedicion: 100,
      nombres: 100,
      apellidos: 100,
      nombreIdentitario: 100,
      pronombre: 50,
      sexo: 20,
      genero: 20,
      orientacionSexual: 50,
      telefono: 30,
      correo: 120,
      estadoCivil: 30,
      escolaridad: 100,
      grupoEtnico: 100,
      discapacidad: 100,
      caracterizacionPcd: 150,
      direccion: 150,
      comuna: 100,
      localidad: 100,
      tipoVivienda: 100,
      zona: 50,
      tenencia: 100,
      cargo: 100,
      direccionEmpresa: 150,
      telefonoEmpresa: 30,
      nombreCompletoAcudiente: 150,
      relacionAcudiente: 100,
      telefonoAcudiente: 30,
      correoAcudiente: 120,
      direccionAcudiente: 150,
      comoSeEntero: 150,
      relacionConUniversidad: 150,
    };

    const excedido = Object.entries(limites).find(
      ([name, max]) => String(form[name] ?? "").length > max
    );

    if (excedido) {
      return `El campo ${excedido[0]} no puede superar ${excedido[1]} caracteres.`;
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

    if (
      form.municipioId &&
      municipioOptions.length > 0 &&
      !municipioOptions.some((item) => String(item.value) === String(form.municipioId))
    ) {
      return "El municipio seleccionado no pertenece al departamento seleccionado.";
    }

    if (
      form.barrioId &&
      barrioOptions.length > 0 &&
      !barrioOptions.some((item) => String(item.value) === String(form.barrioId))
    ) {
      return "El barrio seleccionado no pertenece al municipio seleccionado.";
    }

    if (esMenorEdadFormulario) {
      if (!String(form.nombreCompletoAcudiente || "").trim()) {
        return "Si la persona es menor de edad, el nombre del acudiente es obligatorio.";
      }

      if (!String(form.relacionAcudiente || "").trim()) {
        return "Si la persona es menor de edad, la relación del acudiente es obligatoria.";
      }

      if (!telefonoAcudiente && !correoAcudiente) {
        return "Si la persona es menor de edad, debe informar teléfono o correo del acudiente.";
      }
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

      const payload = construirPayload(
        form,
        personaEditando.id,
        requireResourceVersion(personaEditando, "registro de persona")
      );

      const res = await apiClient.request(`${API_URL_BASE}/personas/${personaEditando.id}`, {
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

      if (res.status === 409) {
        const message = getApiErrorDescription(
          data,
          "La persona fue modificada por otro usuario."
        );

        try {
          const latest = await obtenerPersonaDetalle(personaEditando.id);
          setPersonaEditando((prev) => ({ ...prev, version: latest.version }));
        } catch (refreshError) {
          console.error("Could not refresh the person after a concurrency conflict", refreshError);
        }

        setError(`${message} Tus cambios siguen en el formulario. Revisa la información y vuelve a intentar.`);
        toast.error("Este registro cambió mientras lo estabas editando", {
          description: "Tus cambios se conservaron. Se cargó la versión actual como nueva base para un reintento manual.",
        });
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

      const version = requireResourceVersion(personaADesactivar, "registro de persona");
      const res = await apiClient.request(
        `${API_URL_BASE}/personas/${personaADesactivar.id}/desactivar?version=${encodeURIComponent(String(version))}`,
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

      if (res.status === 409) {
        const message = getApiErrorDescription(
          data,
          "La persona fue modificada por otro usuario."
        );
        setError(`${message} La lista se actualizará para que confirmes nuevamente la acción.`);
        toast.error("No se pudo desactivar porque el registro cambió", {
          description: "La acción no se reintentó automáticamente.",
        });
        await cargarPersonas();
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

          <Button
            type="button"
            variant="outline"
            onClick={() => cargarPersonas()}
            disabled={cargandoLista}
          >
            {cargandoLista ? "Actualizando..." : "Actualizar"}
          </Button>
        </div>

        <div className="relative">
          <Search className="absolute left-3 top-2.5 size-4 text-muted-foreground" />
          <input
            value={busqueda}
            onChange={handleBusquedaChange}
            placeholder="Buscar por nombre, apellido o documento..."
            maxLength={100}
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
                personas.
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
                  <th className="px-4 py-3 text-left font-medium">Tipo</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>

              <tbody>
                {personas.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="px-4 py-8 text-center text-muted-foreground"
                    >
                      No hay personas para mostrar.
                    </td>
                  </tr>
                ) : (
                  personas.map((persona) => (
                    <tr
                      key={persona.id}
                      className="border-t border-primary/20 transition hover:bg-primary/10"
                    >
                      <td className="px-4 py-3">{persona.id}</td>

                      <td className="px-4 py-3">
                        <div className="font-medium">
                          {nombreCompleto(persona) || "Sin nombre"}
                        </div>
                      </td>

                      <td className="px-4 py-3">
                        <div>{persona.tipoDocumento || "N/A"}</div>
                        <div className="text-xs text-muted-foreground">
                          {persona.numeroDocumentoEnmascarado || "N/A"}
                        </div>
                      </td>

                      <td className="px-4 py-3">
                        <span className="rounded-full border border-primary/30 bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">
                          {persona.tipoPersona || "N/A"}
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
          onPageSizeChange={(size) => {
            setRegistrosPorPagina(size);
            setPaginaActual(1);
          }}
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
                    <Input label="Número documento" name="numeroDocumento" value={form.numeroDocumento} onChange={handleChange} maxLength={30} />
                    <Input label="Fecha expedición" name="fechaExpedicion" type="date" value={form.fechaExpedicion} onChange={handleChange} />
                    <Input label="Ciudad expedición" name="ciudadExpedicion" value={form.ciudadExpedicion} onChange={handleChange} maxLength={100} />
                    <Input label="Nombres" name="nombres" value={form.nombres} onChange={handleChange} maxLength={100} />
                    <Input label="Apellidos" name="apellidos" value={form.apellidos} onChange={handleChange} maxLength={100} />
                    <Input label="Nombre identitario" name="nombreIdentitario" value={form.nombreIdentitario} onChange={handleChange} maxLength={100} />
                    <Select label="Pronombre" name="pronombre" value={form.pronombre} onChange={handleChange} options={PRONOMBRE_OPTIONS} />
                    <Select label="Sexo" name="sexo" value={form.sexo} onChange={handleChange} options={SEXO_OPTIONS} />
                    <Select label="Género" name="genero" value={form.genero} onChange={handleChange} options={GENERO_OPTIONS} />
                    <Select label="Orientación sexual" name="orientacionSexual" value={form.orientacionSexual} onChange={handleChange} options={ORIENTACION_OPTIONS} />
                    <Input label="Fecha nacimiento" name="fechaNacimiento" type="date" value={form.fechaNacimiento} onChange={handleChange} />
                  </Seccion>

                  <Seccion titulo="Contacto e identidad social">
                    <Input label="Teléfono" name="telefono" value={form.telefono} onChange={handleChange} digitsOnly maxLength={30} />
                    <Input label="Correo" name="correo" type="email" value={form.correo} onChange={handleChange} maxLength={120} />
                    <Select label="Nacionalidad" name="nacionalidadId" value={form.nacionalidadId} onChange={handleChange} options={nacionalidadOptions} />
                    <Select label="Estado civil" name="estadoCivil" value={form.estadoCivil} onChange={handleChange} options={ESTADO_CIVIL_OPTIONS} />
                    <Select label="Escolaridad" name="escolaridad" value={form.escolaridad} onChange={handleChange} options={ESCOLARIDAD_OPTIONS} />
                    <Input label="Grupo étnico" name="grupoEtnico" value={form.grupoEtnico} onChange={handleChange} maxLength={100} />
                    <Select label="Condición actual" name="condicionActualId" value={form.condicionActualId} onChange={handleChange} options={condicionOptions} />
                    <Input label="Discapacidad" name="discapacidad" value={form.discapacidad} onChange={handleChange} maxLength={100} />
                    <Input label="Caracterización PCD" name="caracterizacionPcd" value={form.caracterizacionPcd} onChange={handleChange} maxLength={150} />
                  </Seccion>

                  <Seccion titulo="Ubicación y vivienda">
                    <Select label="Departamento" name="departamentoId" value={form.departamentoId} onChange={handleChange} options={departamentoOptions} />
                    <Select label="Municipio" name="municipioId" value={form.municipioId} onChange={handleChange} options={municipioOptions} disabled={!form.departamentoId} />
                    <Select label="Barrio" name="barrioId" value={form.barrioId} onChange={handleChange} options={barrioOptions} disabled={!form.municipioId} />
                    <Input label="Dirección" name="direccion" value={form.direccion} onChange={handleChange} maxLength={150} />
                    <Input label="Comuna" name="comuna" value={form.comuna} onChange={handleChange} maxLength={100} />
                    <Input label="Localidad" name="localidad" value={form.localidad} onChange={handleChange} maxLength={100} />
                    <Input label="Estrato" name="estrato" type="number" min={0} value={form.estrato} onChange={handleNumberChange} />
                    <Input label="Tipo vivienda" name="tipoVivienda" value={form.tipoVivienda} onChange={handleChange} maxLength={100} />
                    <Select label="Zona" name="zona" value={form.zona} onChange={handleChange} options={ZONA_OPTIONS} />
                    <Input label="Tenencia" name="tenencia" value={form.tenencia} onChange={handleChange} maxLength={100} />
                    <Input label="Personas a cargo" name="numeroPersonasACargo" type="number" min={0} value={form.numeroPersonasACargo} onChange={handleNumberChange} />
                  </Seccion>

                  <Seccion titulo="Economía">
                    <Select label="Ocupación" name="ocupacionId" value={form.ocupacionId} onChange={handleChange} options={ocupacionOptions} />
                    <Select label="Empresa" name="empresaId" value={form.empresaId} onChange={handleChange} options={empresaOptions} />
                    <Input label="Salario" name="salario" type="number" min={0} value={form.salario} onChange={handleNumberChange} />
                    <Input label="Cargo" name="cargo" value={form.cargo} onChange={handleChange} maxLength={100} />
                    <Input label="Dirección empresa" name="direccionEmpresa" value={form.direccionEmpresa} onChange={handleChange} maxLength={150} />
                    <Input label="Teléfono empresa" name="telefonoEmpresa" value={form.telefonoEmpresa} onChange={handleChange} digitsOnly maxLength={30} />
                  </Seccion>

                  {esMenorEdadFormulario && (
                    <Seccion titulo="Acudiente">
                      <Input label="Nombre acudiente" name="nombreCompletoAcudiente" value={form.nombreCompletoAcudiente} onChange={handleChange} maxLength={150} />
                      <Input label="Relación acudiente" name="relacionAcudiente" value={form.relacionAcudiente} onChange={handleChange} maxLength={100} />
                      <Input label="Teléfono acudiente" name="telefonoAcudiente" value={form.telefonoAcudiente} onChange={handleChange} digitsOnly maxLength={30} />
                      <Input label="Correo acudiente" name="correoAcudiente" type="email" value={form.correoAcudiente} onChange={handleChange} maxLength={120} />
                      <Input label="Dirección acudiente" name="direccionAcudiente" value={form.direccionAcudiente} onChange={handleChange} maxLength={150} />
                    </Seccion>
                  )}

                  <Seccion titulo="Servicio">
                    <Input label="Cómo se enteró" name="comoSeEntero" value={form.comoSeEntero} onChange={handleChange} maxLength={150} />
                    <Input label="Relación con universidad" name="relacionConUniversidad" value={form.relacionConUniversidad} onChange={handleChange} maxLength={150} />
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
