"use client"

/**
 * Formulario de creación de usuarios del sistema jurídico.
 *
 * Permite crear asesores, monitores, estudiantes, conciliadores y administrativos.
 * Cuando el tipo seleccionado es "Estudiante", muestra un Tabs con dos modos:
 * creación individual y cargue masivo desde Excel.
 *
 * Requiere permiso `CREAR_USUARIOS` o `GESTIONAR_ADMINISTRADORES`.
 *
 * @module components/forms/AdminUsuarios/UsuarioSistemaForm
 */
;

import React, { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { FormInput } from "../parts/FormInput";
import { FormSelect } from "../parts/FormSelect";
import { FormCheckbox } from "../parts/FormCheckbox";
import { Button } from "@/components/ui/button";
import { API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import { tieneAlgunPermiso, tienePermiso } from "@/lib/authz";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ImportarEstudiantesForm } from "../usuarios/ImportarEstudiantesForm";

const REQUIRED = "Campo obligatorio";

const PERMISO_GESTIONAR_USUARIOS_LEGACY = "Gestionar usuarios";

function mapOption(item) {
  return {
    value: item.id,
    label:
      item.displayName ||
      item.nombre ||
      item.descripcion ||
      item.codigo ||
      String(item.id),
  };
}

function extraerLista(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.rows)) return data.rows;
  return [];
}

function normalizarTexto(value) {
  const text = String(value || "").trim();
  return text === "" ? null : text;
}

function puedeCrearUsuariosUsuario(usuario) {
  return tieneAlgunPermiso(usuario, [
    PERMISOS.CREAR_USUARIOS,
    PERMISO_GESTIONAR_USUARIOS_LEGACY,
  ]);
}

function puedeGestionarAdministradoresUsuario(usuario) {
  return tienePermiso(usuario, PERMISOS.GESTIONAR_ADMINISTRADORES);
}

function puedeVerCatalogosUsuario(usuario) {
  return tieneAlgunPermiso(usuario, [
    PERMISOS.VER_CATALOGOS,
    PERMISOS.GESTIONAR_CATALOGOS,
  ]);
}

/**
 * Formulario para gestionar usuarios del sistema.
 * @returns {JSX.Element} Componente de administración de usuarios.
 */
export function UsuarioSistemaForm() {
  const router = useRouter();

  const [checking, setChecking] = useState(true);
  const [user, setUser] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [tabEstudiante, setTabEstudiante] = useState("individual");
  const [tiposDocumento, setTiposDocumento] = useState([]);
  const [sedes, setSedes] = useState([]);
  const [areas, setAreas] = useState([]);
  const [asesores, setAsesores] = useState([]);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      rol: "",
      nombre: "",
      tipoDocumentoId: "",
      documento: "",
      email: "",
      telefono: "",
      usuario: "",
      codigo: "",
      sedeId: "",
      areaId: "",
      asesorId: "",
      conciliacion: false,
      directora: false,
      tipoConciliador: "",
    },
  });

  const rol = watch("rol") || "";

  const puedeCrearUsuarios = puedeCrearUsuariosUsuario(user);
  const puedeGestionarAdministradores =
    puedeGestionarAdministradoresUsuario(user);

  const roles = useMemo(() => {
    const base = [
      { value: "asesores", label: "Asesor" },
      { value: "conciliadores", label: "Conciliador" },
      { value: "estudiantes", label: "Estudiante" },
      { value: "monitores", label: "Monitor" },
    ];

    return puedeGestionarAdministradores
      ? [{ value: "administrativos", label: "Administrativo" }, ...base]
      : base;
  }, [puedeGestionarAdministradores]);

  useEffect(() => {
    verificarYCargar();
  }, []);

  async function leerRespuesta(response) {
    const text = await response.text();

    if (!text) return null;

    try {
      return JSON.parse(text);
    } catch {
      return { mensaje: text };
    }
  }

  async function fetchLista(url, mensaje403) {
    const res = await fetch(url, { credentials: "include" });

    if (res.status === 401) {
      router.push("/");
      return [];
    }

    if (res.status === 403) {
      toast.error(mensaje403);
      return [];
    }

    if (!res.ok) {
      return [];
    }

    const data = await res.json();
    return extraerLista(data);
  }

  async function verificarYCargar() {
    try {
      const res = await fetch(`${API_URL_BASE}/auth/me`, {
        credentials: "include",
      });

      if (res.status === 401) {
        router.push("/");
        return;
      }

      if (!res.ok) {
        toast.error("No se pudo validar la sesión");
        router.push("/");
        return;
      }

      const usuarioActual = await res.json();
      setUser(usuarioActual);

      const autorizado =
        puedeCrearUsuariosUsuario(usuarioActual) ||
        puedeGestionarAdministradoresUsuario(usuarioActual);

      if (!autorizado) {
        toast.error("No tienes permisos para crear usuarios");
        router.push("/inicio");
        return;
      }

      if (!puedeVerCatalogosUsuario(usuarioActual)) {
        toast.error("No tienes permiso para cargar catálogos");
        return;
      }

      const [tiposData, sedesData, areasData, asesoresData] =
        await Promise.all([
          fetchLista(
            `${API_URL_BASE}/tipos-documento/activos`,
            "No tienes permiso para consultar tipos de documento"
          ),
          fetchLista(
            `${API_URL_BASE}/sedes`,
            "No tienes permiso para consultar sedes"
          ),
          fetchLista(
            `${API_URL_BASE}/areas`,
            "No tienes permiso para consultar áreas"
          ),
          fetchLista(
            `${API_URL_BASE}/asesores/activos`,
            "No tienes permiso para consultar asesores"
          ),
        ]);

      setTiposDocumento(tiposData.map(mapOption));
      setSedes(sedesData.map(mapOption));
      setAreas(areasData.map(mapOption));
      setAsesores(
        asesoresData.map((asesor) => ({
          value: asesor.id,
          label: asesor.documento
            ? `${asesor.nombre} - ${asesor.documento}`
            : asesor.nombre || String(asesor.id),
        }))
      );
    } catch (error) {
      console.error(error);
      toast.error("Error cargando el formulario");
      router.push("/");
    } finally {
      setChecking(false);
    }
  }

  function normalizarPayload(data) {
    const {
      rol: _rol,
      tipoDocumentoId,
      sedeId,
      areaId,
      asesorId,
      conciliacion,
      directora,
      tipoConciliador,
      email,
      ...rest
    } = data;

    const payload = {
      ...rest,
      email: normalizarTexto(email),
      activo: true,
      tipoDocumentoId: tipoDocumentoId ? Number(tipoDocumentoId) : null,
      sedeId: sedeId ? Number(sedeId) : null,
    };

    if (areaId) payload.areaId = Number(areaId);
    if (asesorId) payload.asesorId = Number(asesorId);
    if (rol === "estudiantes") payload.conciliacion = Boolean(conciliacion);
    if (rol === "administrativos") payload.directora = Boolean(directora);
    if (rol === "conciliadores") payload.tipoConciliador = tipoConciliador;

    return payload;
  }

  async function onSubmit(data) {
    if (!rol) {
      toast.error("Selecciona un tipo de usuario");
      return;
    }

    if (rol === "administrativos" && !puedeGestionarAdministradores) {
      toast.error("No tienes permiso para gestionar administradores");
      return;
    }

    if (rol !== "administrativos" && !puedeCrearUsuarios) {
      toast.error("No tienes permiso para crear usuarios");
      return;
    }

    if (
      !tiposDocumento.some(
        (tipo) => Number(tipo.value) === Number(data.tipoDocumentoId)
      )
    ) {
      toast.error("Seleccione un tipo de documento válido");
      return;
    }

    try {
      setGuardando(true);

      const res = await fetch(`${API_URL_BASE}/${rol}`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(normalizarPayload(data)),
      });

      const result = await leerRespuesta(res);

      if (res.status === 401) {
        router.push("/");
        return;
      }

      if (res.status === 403) {
        toast.error("No tienes permiso para crear este tipo de usuario");
        return;
      }

      if (!res.ok) {
        throw new Error(
          result?.mensaje || result?.message || "Error al crear usuario"
        );
      }

      toast.success("Usuario creado correctamente");
      reset();
    } catch (error) {
      console.error(error);
      toast.error(error.message || "Error al crear usuario");
    } finally {
      setGuardando(false);
    }
  }

  function renderTipoDocumento() {
    if (tiposDocumento.length === 0) {
      return <Aviso>No se cargaron tipos de documento.</Aviso>;
    }

    return (
      <FormSelect
        name="tipoDocumentoId"
        label="Tipo de Documento"
        options={tiposDocumento}
        register={register}
        errors={errors}
        rules={{ required: REQUIRED, valueAsNumber: true }}
      />
    );
  }

  function renderCamposRol() {
    if (rol === "asesores") {
      return areas.length > 0 ? (
        <FormSelect
          name="areaId"
          label="Área"
          options={areas}
          register={register}
          errors={errors}
          rules={{ required: REQUIRED, valueAsNumber: true }}
        />
      ) : (
        <Aviso>No se cargaron áreas.</Aviso>
      );
    }

    if (rol === "estudiantes") {
      return (
        <>
          {asesores.length > 0 ? (
            <FormSelect
              name="asesorId"
              label="Asesor"
              options={asesores}
              register={register}
              errors={errors}
              rules={{ required: REQUIRED, valueAsNumber: true }}
            />
          ) : (
            <Aviso>No se cargaron asesores.</Aviso>
          )}

          <FormCheckbox
            name="conciliacion"
            label="¿Conciliación?"
            register={register}
          />
        </>
      );
    }

    if (rol === "administrativos") {
      return (
        <FormCheckbox
          name="directora"
          label="¿Directora?"
          register={register}
        />
      );
    }

    if (rol === "conciliadores") {
      return (
        <FormSelect
          name="tipoConciliador"
          label="Tipo"
          options={[
            { value: "INTERNO", label: "Interno" },
            { value: "EXTERNO", label: "Externo" },
          ]}
          register={register}
          errors={errors}
          rules={{ required: REQUIRED }}
        />
      );
    }

    return null;
  }

  if (checking) {
    return <div className="text-center mt-10">Cargando...</div>;
  }

  return (
    <div className="rounded-xl border bg-card p-6 shadow">
      <h2 className="mb-4 text-xl font-bold">Crear Usuario</h2>

      <div className="space-y-5">
        <FormSelect
          name="rol"
          label="Tipo de Usuario"
          options={roles}
          register={register}
          errors={errors}
          rules={{ required: REQUIRED }}
        />

        {rol && (
          rol === "estudiantes" ? (
            <Tabs value={tabEstudiante} onValueChange={setTabEstudiante}>
              <TabsList className="mb-4">
                <TabsTrigger key="individual" value="individual">Crear uno</TabsTrigger>
                <TabsTrigger key="masivo" value="masivo">Cargue masivo</TabsTrigger>
              </TabsList>

              <TabsContent key="individual" value="individual">
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <FormInput
                    name="nombre"
                    label="Nombre"
                    register={register}
                    errors={errors}
                    rules={{ required: REQUIRED }}
                  />

                  {renderTipoDocumento()}

                  <FormInput
                    name="documento"
                    label="Documento"
                    register={register}
                    errors={errors}
                    rules={{ required: REQUIRED }}
                  />

                  <FormInput
                    name="email"
                    label="Email"
                    register={register}
                    errors={errors}
                    rules={{ required: REQUIRED }}
                  />

                  <FormInput
                    name="telefono"
                    label="Teléfono"
                    register={register}
                    errors={errors}
                    rules={{ required: REQUIRED }}
                  />

                  <FormInput
                    name="usuario"
                    label="Usuario"
                    register={register}
                    errors={errors}
                    rules={{ required: REQUIRED }}
                  />

                  <FormInput
                    name="codigo"
                    label="Código"
                    register={register}
                    errors={errors}
                    rules={{ required: REQUIRED }}
                  />

                  {sedes.length > 0 ? (
                    <FormSelect
                      name="sedeId"
                      label="Sede"
                      options={sedes}
                      register={register}
                      errors={errors}
                      rules={{ required: REQUIRED, valueAsNumber: true }}
                    />
                  ) : (
                    <Aviso>No se cargaron sedes.</Aviso>
                  )}

                  {renderCamposRol()}
                </div>

                <div className="flex justify-end gap-4 mt-4">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => reset()}
                    disabled={guardando}
                  >
                    Limpiar
                  </Button>

                  <Button
                    type="button"
                    onClick={handleSubmit(onSubmit)}
                    disabled={guardando}
                  >
                    {guardando ? "Creando..." : "Crear Usuario"}
                  </Button>
                </div>
              </TabsContent>

              <TabsContent key="masivo" value="masivo">
                <ImportarEstudiantesForm puedeImportar={puedeCrearUsuarios} />
              </TabsContent>
            </Tabs>
          ) : (
            <>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <FormInput
                  name="nombre"
                  label="Nombre"
                  register={register}
                  errors={errors}
                  rules={{ required: REQUIRED }}
                />

                {renderTipoDocumento()}

                <FormInput
                  name="documento"
                  label="Documento"
                  register={register}
                  errors={errors}
                  rules={{ required: REQUIRED }}
                />

                <FormInput
                  name="email"
                  label="Email"
                  register={register}
                  errors={errors}
                  rules={{ required: REQUIRED }}
                />

                <FormInput
                  name="telefono"
                  label="Teléfono"
                  register={register}
                  errors={errors}
                  rules={{ required: REQUIRED }}
                />

                <FormInput
                  name="usuario"
                  label="Usuario"
                  register={register}
                  errors={errors}
                  rules={{ required: REQUIRED }}
                />

                <FormInput
                  name="codigo"
                  label="Código"
                  register={register}
                  errors={errors}
                  rules={{ required: REQUIRED }}
                />

                {sedes.length > 0 ? (
                  <FormSelect
                    name="sedeId"
                    label="Sede"
                    options={sedes}
                    register={register}
                    errors={errors}
                    rules={{ required: REQUIRED, valueAsNumber: true }}
                  />
                ) : (
                  <Aviso>No se cargaron sedes.</Aviso>
                )}

                {renderCamposRol()}
              </div>

              <div className="flex justify-end gap-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => reset()}
                  disabled={guardando}
                >
                  Limpiar
                </Button>

                <Button
                  type="button"
                  onClick={handleSubmit(onSubmit)}
                  disabled={guardando}
                >
                  {guardando ? "Creando..." : "Crear Usuario"}
                </Button>
              </div>
            </>
          )
        )}
      </div>
    </div>
  );
}

function Aviso({ children }) {
  return (
    <div className="rounded-lg border bg-muted/30 p-3 text-sm text-muted-foreground">
      {children}
    </div>
  );
}
