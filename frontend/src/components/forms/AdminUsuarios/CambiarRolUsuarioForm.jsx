"use client"

/**
 * Formulario para cambiar el rol de un usuario del sistema.
 *
 * Requiere permiso `ASIGNAR_ROL_USUARIOS`.
 *
 * @module components/forms/AdminUsuarios/CambiarRolUsuarioForm
 */
;

import React, { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { API_URL_BASE } from "@/lib/config";
import { FormInput } from "../parts/FormInput";
import { FormSelect } from "../parts/FormSelect";
import { FormCheckbox } from "../parts/FormCheckbox";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import { PERMISOS } from "@/lib/permission";
import { normalizar, tieneAlgunPermiso, tienePermiso } from "@/lib/authz";
import Pagination from "@/components/ui/Pagination";
import { sortByIdAsc } from "@/lib/list-utils";

const PERMISO_GESTIONAR_USUARIOS = "Gestionar usuarios";

const TIPOS_PERFIL = [
  {
    value: "ADMINISTRATIVO",
    label: "Administrativo",
    endpoint: "administrativo",
    endpointActual: "administrativos",
    rolIdFallback: 1,
    nombresRol: ["Administrador", "Administrativo"],
  },
  {
    value: "ASESOR",
    label: "Asesor",
    endpoint: "asesor",
    endpointActual: "asesores",
    rolIdFallback: 2,
    nombresRol: ["Asesor"],
  },
  {
    value: "ESTUDIANTE",
    label: "Estudiante",
    endpoint: "estudiante",
    endpointActual: "estudiantes",
    rolIdFallback: 3,
    nombresRol: ["Estudiante"],
  },
  {
    value: "MONITOR",
    label: "Monitor",
    endpoint: "monitor",
    endpointActual: "monitores",
    rolIdFallback: 4,
    nombresRol: ["Monitor"],
  },
  {
    value: "CONCILIADOR",
    label: "Conciliador",
    endpoint: "conciliador",
    endpointActual: "conciliadores",
    rolIdFallback: 5,
    nombresRol: ["Conciliador"],
  },
];

const VALORES_INICIALES = {
  usuarioSistemaId: "",
  destino: "",
  motivo: "",
  nombre: "",
  tipoDocumentoId: "",
  documento: "",
  telefono: "",
  usuario: "",
  codigo: "",
  sedeId: "",
  asesorId: "",
  areaId: "",
  conciliacion: false,
  directora: false,
  tipoConciliador: "",
};

function usuarioActivo(usuario) {
  return (
    usuario?.activo !== false &&
    String(usuario?.estado || "").toUpperCase() !== "INACTIVO"
  );
}

function filtrarActivos(lista) {
  return Array.isArray(lista) ? lista.filter(usuarioActivo) : [];
}

function extraerLista(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.rows)) return data.rows;
  return [];
}

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

function normalizarTexto(value) {
  const text = String(value || "").trim();
  return text === "" ? null : text;
}

function toNumberOrNull(value) {
  if (value === null || value === undefined || value === "") return null;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function buscarPerfil(value) {
  return TIPOS_PERFIL.find((perfil) => perfil.value === value);
}

function coincideNombreRol(rol, perfil) {
  const nombre = normalizar(rol?.nombre);
  return perfil.nombresRol.some((nombreRol) => normalizar(nombreRol) === nombre);
}

/**
 * Formulario para cambiar el rol de un usuario del sistema.
 * @returns {JSX.Element} Componente de cambio de rol.
 */
export function CambiarRolUsuarioForm() {
  const router = useRouter();

  const [user, setUser] = useState(null);
  const [paginaActualModal, setPaginaActualModal] = useState(1);
  const [registrosPorPaginaModal, setRegistrosPorPaginaModal] = useState(10);
  const REGISTROS_POR_PAGINA_OPTIONS_MODAL = [5, 10, 20, 50];
  const [usuarios, setUsuarios] = useState([]);
  const [roles, setRoles] = useState([]);
  const [tiposDocumento, setTiposDocumento] = useState([]);
  const [sedes, setSedes] = useState([]);
  const [asesores, setAsesores] = useState([]);
  const [areas, setAreas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [avisoPerfil, setAvisoPerfil] = useState(null);
  const [modalAbierto, setModalAbierto] = useState(false);
  const [busquedaModal, setBusquedaModal] = useState("");

  const {
    register,
    handleSubmit,
    watch,
    reset,
    setValue,
    formState: { errors },
  } = useForm({ defaultValues: VALORES_INICIALES });

  const REQUIRED = "Campo obligatorio";
  const usuarioSistemaId = watch("usuarioSistemaId");
  const destino = watch("destino");

  const usuarioSeleccionado = useMemo(() => {
    return usuarios.find((usuario) => String(usuario.id) === String(usuarioSistemaId));
  }, [usuarios, usuarioSistemaId]);

  const perfilDestino = useMemo(() => buscarPerfil(destino), [destino]);

  const rolesPorPerfil = useMemo(() => {
    const mapa = {};

    TIPOS_PERFIL.forEach((perfil) => {
      const rol = roles.find((item) => coincideNombreRol(item, perfil));
      if (rol?.id) mapa[perfil.value] = rol;
    });

    return mapa;
  }, [roles]);

  const puedeAsignarRol = tieneAlgunPermiso(user, [
    PERMISOS.ASIGNAR_ROL_USUARIOS,
    PERMISO_GESTIONAR_USUARIOS,
  ]);

  const puedeGestionarAdministradores = tienePermiso(
    user,
    PERMISOS.GESTIONAR_ADMINISTRADORES
  );

  const perfilesDisponibles = useMemo(() => {
    let perfiles = TIPOS_PERFIL;

    if (!puedeGestionarAdministradores) {
      perfiles = perfiles.filter((perfil) => perfil.value !== "ADMINISTRATIVO");
    }

    if (!usuarioSeleccionado?.tipoPerfil) return perfiles;

    return perfiles.filter((perfil) => perfil.value !== usuarioSeleccionado.tipoPerfil);
  }, [usuarioSeleccionado, puedeGestionarAdministradores]);

  const usuariosFiltrados = useMemo(() => {
    const activos = filtrarActivos(usuarios);
    const q = busquedaModal.trim().toLowerCase();

    const filtrados = !q ? activos : activos.filter((usuario) =>
      `${usuario.username || ""} ${usuario.rolNombre || ""} ${usuario.tipoPerfil || ""}`
        .toLowerCase()
        .includes(q)
    );

    return sortByIdAsc(filtrados);
  }, [usuarios, busquedaModal]);

  const totalPaginasModal = Math.max(1, Math.ceil(usuariosFiltrados.length / registrosPorPaginaModal));

  useEffect(() => {
    setPaginaActualModal(1);
  }, [busquedaModal, registrosPorPaginaModal]);

  useEffect(() => {
    if (paginaActualModal > totalPaginasModal) {
      setPaginaActualModal(totalPaginasModal);
    }
  }, [paginaActualModal, totalPaginasModal]);

  useEffect(() => {
    cargarDatosIniciales();
  }, []);

  useEffect(() => {
    if (!usuarioSeleccionado) return;

    setValue("destino", "");
    setAvisoPerfil(null);
    limpiarCamposDestino();
    cargarDatosActualesUsuario(usuarioSeleccionado);
  }, [usuarioSeleccionado, setValue]);

  useEffect(() => {
    if (!perfilDestino) {
      setAvisoPerfil(null);
      return;
    }

    limpiarCamposDestino();
    setAvisoPerfil({
      tipo: "info",
      mensaje:
        "Se usarán los datos comunes cargados desde el perfil actual. Si el usuario ya tuvo el perfil destino, el backend reutilizará o reactivará ese registro al guardar.",
    });
  }, [perfilDestino]);

  async function leerRespuesta(response) {
    const text = await response.text();

    if (!text) return null;

    try {
      return JSON.parse(text);
    } catch {
      return { mensaje: text };
    }
  }

  async function fetchJson(url) {
    const response = await fetch(url, { credentials: "include" });

    if (response.status === 401) {
      router.replace("/");
      return [];
    }

    if (!response.ok) {
      return [];
    }

    const data = await response.json();
    return extraerLista(data);
  }

  async function cargarDatosIniciales() {
    try {
      setLoading(true);

      const meRes = await fetch(`${API_URL_BASE}/auth/me`, {
        method: "GET",
        credentials: "include",
      });

      if (meRes.status === 401 || !meRes.ok) {
        router.replace("/");
        return;
      }

      const meData = await meRes.json();

      if (
        !tieneAlgunPermiso(meData, [
          PERMISOS.ASIGNAR_ROL_USUARIOS,
          PERMISO_GESTIONAR_USUARIOS,
        ])
      ) {
        router.replace("/inicio");
        return;
      }

      setUser(meData);

      const [usuariosData, rolesData, tiposData, sedesData, asesoresData, areasData] =
        await Promise.all([
          fetchJson(`${API_URL_BASE}/usuarios-sistema/activos`),
          fetchJson(`${API_URL_BASE}/roles/activos`),
          fetchJson(`${API_URL_BASE}/tipos-documento/activos`),
          fetchJson(`${API_URL_BASE}/sedes`),
          fetchJson(`${API_URL_BASE}/asesores/activos`),
          fetchJson(`${API_URL_BASE}/areas`),
        ]);

      setUsuarios(sortByIdAsc(filtrarActivos(usuariosData)));
      setRoles(filtrarActivos(rolesData));
      setTiposDocumento(tiposData.map(mapOption));
      setSedes(sedesData.map(mapOption));
      setAreas(areasData.map(mapOption));
      setAsesores(
        filtrarActivos(asesoresData).map((asesor) => ({
          value: asesor.id,
          label: asesor.documento
            ? `${asesor.nombre} - ${asesor.documento}`
            : asesor.nombre || String(asesor.id),
        }))
      );
    } catch (error) {
      console.error(error);
      toast.error("Error cargando datos");
    } finally {
      setLoading(false);
    }
  }

  async function cargarDatosActualesUsuario(usuario) {
    const perfilActual = buscarPerfil(usuario.tipoPerfil);

    if (!perfilActual || !usuario.perfilId) {
      setValue("usuario", usuario.username || "");
      return;
    }

    try {
      const res = await fetch(
        `${API_URL_BASE}/${perfilActual.endpointActual}/${usuario.perfilId}`,
        { credentials: "include" }
      );

      if (!res.ok) {
        setValue("usuario", usuario.username || "");
        return;
      }

      const datos = await res.json();
      precargarDatosPerfil(datos);
    } catch (error) {
      console.error("Error cargando datos actuales:", error);
      setValue("usuario", usuario.username || "");
    }
  }

  function precargarDatosComunes(datos) {
    setValue("nombre", datos.nombre || "");
    setValue("tipoDocumentoId", datos.tipoDocumentoId || "");
    setValue("documento", datos.documento || "");
    setValue("telefono", datos.telefono || "");
    setValue("usuario", datos.usuario || datos.username || "");
    setValue("codigo", datos.codigo || "");
    setValue("sedeId", datos.sedeId || "");
  }

  function limpiarCamposDestino() {
    setValue("asesorId", "");
    setValue("areaId", "");
    setValue("conciliacion", false);
    setValue("directora", false);
    setValue("tipoConciliador", "");
  }

  function precargarDatosPerfil(datos) {
    precargarDatosComunes(datos);
    setValue("asesorId", datos.asesorId || "");
    setValue("areaId", datos.areaId || "");
    setValue("conciliacion", Boolean(datos.conciliacion));
    setValue("directora", Boolean(datos.directora));
    setValue("tipoConciliador", datos.tipoConciliador || "");
  }

  function obtenerRolIdDestino() {
    if (!perfilDestino) return null;

    return rolesPorPerfil[perfilDestino.value]?.id || perfilDestino.rolIdFallback;
  }

  function construirPayload(data) {
    const rolId = obtenerRolIdDestino();

    const payload = {
      rolId,
      motivo: normalizarTexto(data.motivo),
      nombre: normalizarTexto(data.nombre),
      telefono: normalizarTexto(data.telefono),
      usuario: normalizarTexto(data.usuario),
      codigo: normalizarTexto(data.codigo),
    };

    const tipoDocumentoId = toNumberOrNull(data.tipoDocumentoId);
    const sedeId = toNumberOrNull(data.sedeId);

    if (tipoDocumentoId !== null) payload.tipoDocumentoId = tipoDocumentoId;
    if (normalizarTexto(data.documento)) payload.documento = normalizarTexto(data.documento);
    if (sedeId !== null) payload.sedeId = sedeId;

    if (destino === "ESTUDIANTE") {
      payload.asesorId = Number(data.asesorId);
      payload.conciliacion = Boolean(data.conciliacion);
    }

    if (destino === "ASESOR") {
      payload.areaId = Number(data.areaId);
    }

    if (destino === "ADMINISTRATIVO") {
      payload.directora = Boolean(data.directora);
    }

    if (destino === "CONCILIADOR") {
      payload.tipoConciliador = data.tipoConciliador;
    }

    return payload;
  }

  async function onSubmit(data) {
    if (!puedeAsignarRol) {
      router.replace("/inicio");
      return;
    }

    if (!usuarioSeleccionado) {
      toast.error("Selecciona un usuario");
      return;
    }

    if (!usuarioActivo(usuarioSeleccionado)) {
      toast.error("No puedes cambiar el rol de un usuario inactivo");
      return;
    }

    if (!perfilDestino) {
      toast.error("Selecciona el perfil destino");
      return;
    }

    if (destino === "ADMINISTRATIVO" && !puedeGestionarAdministradores) {
      toast.error("No tienes permiso para gestionar administradores");
      return;
    }

    const rolIdDestino = obtenerRolIdDestino();

    if (!rolIdDestino) {
      toast.error("No se pudo resolver el rol destino");
      return;
    }

    try {
      setGuardando(true);

      const response = await fetch(
        `${API_URL_BASE}/usuarios-sistema/${usuarioSeleccionado.id}/perfil/${perfilDestino.endpoint}`,
        {
          method: "PATCH",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(construirPayload(data)),
        }
      );

      const result = await leerRespuesta(response);

      if (response.status === 401) {
        router.replace("/");
        return;
      }

      if (response.status === 403) {
        router.replace("/inicio");
        return;
      }

      if (!response.ok) {
        throw new Error(
          result?.mensaje ||
            result?.message ||
            "No se pudo cambiar el perfil del usuario"
        );
      }

      toast.success("Perfil y rol actualizados correctamente");

      setUsuarios((current) =>
        sortByIdAsc(
          filtrarActivos(
            current.map((usuario) => (usuario.id === result.id ? result : usuario))
          )
        )
      );

      reset({
        ...VALORES_INICIALES,
        usuarioSistemaId: String(result.id),
      });

      setAvisoPerfil(null);
    } catch (error) {
      console.error(error);
      toast.error(error.message || "Error cambiando perfil");
    } finally {
      setGuardando(false);
    }
  }

  function seleccionarUsuario(usuario) {
    if (!usuarioActivo(usuario)) return;

    reset({
      ...VALORES_INICIALES,
      usuarioSistemaId: String(usuario.id),
    });

    setAvisoPerfil(null);
    setModalAbierto(false);
    setBusquedaModal("");
  }

  function limpiarSeleccion() {
    reset(VALORES_INICIALES);
    setAvisoPerfil(null);
    setBusquedaModal("");
  }

  function renderCamposComunes() {
    return (
      <>
        <FormInput
          name="motivo"
          label="Motivo del cambio"
          register={register}
          errors={errors}
          rules={{ required: REQUIRED }}
        />

        <FormInput
          name="nombre"
          label="Nombre completo"
          register={register}
          errors={errors}
          rules={{ required: REQUIRED }}
        />

        {tiposDocumento.length > 0 && (
          <FormSelect
            name="tipoDocumentoId"
            label="Tipo de documento"
            options={tiposDocumento}
            register={register}
            errors={errors}
            rules={{
              required:
                destino === "ESTUDIANTE" || destino === "ASESOR"
                  ? REQUIRED
                  : false,
              valueAsNumber: true,
            }}
          />
        )}

        <FormInput
          name="documento"
          label="Documento"
          register={register}
          errors={errors}
          rules={{
            required:
              destino === "ESTUDIANTE" ||
              destino === "ASESOR" ||
              destino === "CONCILIADOR"
                ? REQUIRED
                : false,
          }}
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

        {sedes.length > 0 && (
          <FormSelect
            name="sedeId"
            label="Sede"
            options={sedes}
            register={register}
            errors={errors}
            rules={{
              required:
                destino === "ESTUDIANTE" || destino === "ASESOR"
                  ? REQUIRED
                  : false,
              valueAsNumber: true,
            }}
          />
        )}
      </>
    );
  }

  function renderCamposDestino() {
    switch (destino) {
      case "ESTUDIANTE":
        return (
          <>
            <FormSelect
              name="asesorId"
              label="Asesor"
              options={asesores}
              register={register}
              errors={errors}
              rules={{ required: REQUIRED, valueAsNumber: true }}
            />

            <FormCheckbox
              name="conciliacion"
              label="¿Conciliación?"
              register={register}
            />
          </>
        );

      case "ASESOR":
        return (
          <FormSelect
            name="areaId"
            label="Área"
            options={areas}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED, valueAsNumber: true }}
          />
        );

      case "ADMINISTRATIVO":
        return (
          <FormCheckbox
            name="directora"
            label="¿Directora?"
            register={register}
          />
        );

      case "CONCILIADOR":
        return (
          <FormSelect
            name="tipoConciliador"
            label="Tipo de conciliador"
            options={[
              { value: "INTERNO", label: "Interno" },
              { value: "EXTERNO", label: "Externo" },
            ]}
            register={register}
            errors={errors}
            rules={{ required: REQUIRED }}
          />
        );

      case "MONITOR":
      default:
        return null;
    }
  }

  if (loading) {
    return <div className="text-center mt-10">Cargando usuarios...</div>;
  }

  return (
    <div className="space-y-6 p-6 bg-card rounded-xl border">
      <div>
        <h2 className="text-2xl font-bold">Cambiar rol y perfil de usuario</h2>
        <p className="text-muted-foreground">
          Selecciona un usuario, elige el nuevo perfil y guarda el cambio.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <input
          type="hidden"
          {...register("usuarioSistemaId", { required: REQUIRED })}
        />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium">Usuario</label>

            <Button
              type="button"
              variant="outline"
              onClick={() => setModalAbierto(true)}
              className="w-full justify-start"
            >
              {usuarioSeleccionado
                ? `${usuarioSeleccionado.username} - ${
                    usuarioSeleccionado.rolNombre || "Sin rol"
                  } - ${usuarioSeleccionado.tipoPerfil || "Sin perfil"}`
                : "Buscar usuario"}
            </Button>

            {errors?.usuarioSistemaId && (
              <p className="text-xs text-red-500">
                {errors.usuarioSistemaId.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-medium">Nuevo perfil / rol</label>

            <select
              {...register("destino", { required: REQUIRED })}
              disabled={!usuarioSeleccionado}
              className={`flex h-9 w-full rounded-lg border bg-background px-3 py-2 text-sm ${
                errors?.destino ? "border-red-500" : ""
              }`}
            >
              <option value="">Seleccione un perfil destino</option>

              {perfilesDisponibles.map((perfil) => {
                const rolAsignado = rolesPorPerfil[perfil.value];
                return (
                  <option key={perfil.value} value={perfil.value}>
                    {perfil.label}
                    {rolAsignado?.nombre ? ` - Rol: ${rolAsignado.nombre}` : ""}
                  </option>
                );
              })}
            </select>

            {errors?.destino && (
              <p className="text-xs text-red-500">
                {errors.destino.message}
              </p>
            )}
          </div>
        </div>

        {usuarioSeleccionado && (
          <div className="rounded-lg border bg-muted/30 p-4 text-sm">
            <p>
              <span className="font-medium">Usuario actual:</span>{" "}
              {usuarioSeleccionado.username}
            </p>
            <p>
              <span className="font-medium">Rol actual:</span>{" "}
              {usuarioSeleccionado.rolNombre || "Sin rol"}
            </p>
            <p>
              <span className="font-medium">Perfil actual:</span>{" "}
              {usuarioSeleccionado.tipoPerfil || "Sin perfil"}
            </p>
          </div>
        )}

        {avisoPerfil && (
          <div className="rounded-lg border bg-background p-4 text-sm text-muted-foreground">
            <p>{avisoPerfil.mensaje}</p>
          </div>
        )}

        {destino && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {renderCamposComunes()}
              {renderCamposDestino()}
            </div>

            <div className="flex justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                disabled={guardando}
                onClick={limpiarSeleccion}
              >
                Limpiar
              </Button>

              <Button type="submit" disabled={guardando}>
                {guardando ? "Guardando..." : "Cambiar rol y perfil"}
              </Button>
            </div>
          </>
        )}
      </form>

      {modalAbierto && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/70 px-4 backdrop-blur-sm">
          <div className="w-full max-w-3xl rounded-xl border bg-card shadow-lg">
            <div className="flex items-center justify-between border-b p-4">
              <h3 className="text-lg font-semibold">Seleccionar usuario activo</h3>

              <Button
                type="button"
                variant="outline"
                onClick={() => setModalAbierto(false)}
              >
                Cerrar
              </Button>
            </div>

            <div className="space-y-4 p-4">
              <input
                value={busquedaModal}
                onChange={(event) => setBusquedaModal(event.target.value)}
                placeholder="Buscar por usuario, rol o perfil..."
                className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />

              <div className="max-h-[420px] overflow-auto rounded-lg border">
                <table className="w-full text-sm">
                  <thead className="bg-muted">
                    <tr>
                      <th className="px-4 py-3 text-left font-medium">ID</th>
                      <th className="px-4 py-3 text-left font-medium">Usuario</th>
                      <th className="px-4 py-3 text-left font-medium">Rol</th>
                      <th className="px-4 py-3 text-left font-medium">Perfil</th>
                      <th className="px-4 py-3 text-right font-medium">Acción</th>
                    </tr>
                  </thead>

                  <tbody>
                    {usuariosFiltrados.length === 0 ? (
                      <tr>
                        <td
                          colSpan={5}
                          className="px-4 py-8 text-center text-muted-foreground"
                        >
                          No hay usuarios activos para mostrar.
                        </td>
                      </tr>
                    ) : (
                      usuariosFiltrados.slice((paginaActualModal - 1) * registrosPorPaginaModal, (paginaActualModal - 1) * registrosPorPaginaModal + registrosPorPaginaModal).map((usuario) => (
                        <tr key={usuario.id} className="border-t hover:bg-muted/50">
                          <td className="px-4 py-3">{usuario.id}</td>
                          <td className="px-4 py-3">{usuario.username}</td>
                          <td className="px-4 py-3">
                            {usuario.rolNombre || "Sin rol"}
                          </td>
                          <td className="px-4 py-3">
                            {usuario.tipoPerfil || "Sin perfil"}
                          </td>
                          <td className="px-4 py-3 text-right">
                            <Button
                              type="button"
                              size="sm"
                              onClick={() => seleccionarUsuario(usuario)}
                            >
                              Seleccionar
                            </Button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              <Pagination
                currentPage={paginaActualModal}
                totalPages={totalPaginasModal}
                onPageChange={(p) => setPaginaActualModal(p)}
                pageSize={registrosPorPaginaModal}
                onPageSizeChange={(v) => { setRegistrosPorPaginaModal(v); setPaginaActualModal(1); }}
                pageSizeOptions={REGISTROS_POR_PAGINA_OPTIONS_MODAL}
                totalItems={usuariosFiltrados.length}
              />

            </div>
          </div>
        </div>
      )}
    </div>
  );
}
