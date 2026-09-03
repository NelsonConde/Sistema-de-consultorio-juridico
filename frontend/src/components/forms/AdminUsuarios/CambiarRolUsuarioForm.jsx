"use client"

import { apiClient } from "@/lib/apiClient";
import { apiResponse, readResponseBody } from "@/lib/api";
/**
 * Form for changing a system user's role/profile.
 *
 * Requires the `ASIGNAR_ROL_USUARIOS` permission.
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
import { tieneAlgunPermiso, tienePermiso } from "@/lib/authz";
import { digitsOnlyRule, maxLengthRule } from "@/lib/form-validation";
import Pagination from "@/components/ui/Pagination";
import { sortByIdAsc } from "@/lib/list-utils";

import { PERMISO_GESTIONAR_USUARIOS, TIPOS_PERFIL, VALORES_INICIALES } from "./cambiar-rol.constants";
import {
  buscarPerfil,
  extraerLista,
  filtrarActivos,
  mapOption,
  normalizarTexto,
  rolCoincideConPerfil,
  toNumberOrNull,
  usuarioActivo,
} from "./cambiar-rol.utils";

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
  const rolIdDestino = watch("rolIdDestino");

  const usuarioSeleccionado = useMemo(() => {
    return usuarios.find((usuario) => String(usuario.id) === String(usuarioSistemaId));
  }, [usuarios, usuarioSistemaId]);

  const perfilDestino = useMemo(() => buscarPerfil(destino), [destino]);

  const rolesCompatibles = useMemo(() => {
    if (!perfilDestino) return [];

    return roles.filter(
      (rol) => usuarioActivo(rol) && rolCoincideConPerfil(rol, perfilDestino)
    );
  }, [roles, perfilDestino]);

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
      setValue("rolIdDestino", "");
      return;
    }

    limpiarCamposDestino();
    setValue(
      "rolIdDestino",
      rolesCompatibles.length === 1 ? String(rolesCompatibles[0].id) : ""
    );
    setAvisoPerfil({
      tipo: "info",
      mensaje:
        "Se usarán los datos comunes cargados desde el perfil actual. Si el usuario ya tuvo el perfil destino, el backend reutilizará o reactivará ese registro al guardar.",
    });
  }, [perfilDestino, rolesCompatibles, setValue]);

  async function leerRespuesta(response) {
    const data = await readResponseBody(response);
    return typeof data === "string" ? { mensaje: data } : data;
  }

  async function fetchJson(url) {
    const { response, data } = await apiResponse(url, { method: "GET" });

    if (response.status === 401) {
      router.replace("/");
      return [];
    }

    if (!response.ok) {
      return [];
    }

    return extraerLista(data);
  }

  async function cargarDatosIniciales() {
    try {
      setLoading(true);

      const meRes = await apiClient.request(`${API_URL_BASE}/auth/me`, {
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
      const res = await apiClient.request(
        `${API_URL_BASE}/${perfilActual.endpointActual}/${usuario.perfilId}`,
        { credentials: "include" }
      );

      if (!res.ok) {
        setValue("usuario", usuario.username || "");
        return;
      }

      const datos = await res.json();
      precargarDatosPerfil(datos);
    } catch {

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
    if (!perfilDestino || !rolIdDestino) return null;

    const rolSeleccionado = rolesCompatibles.find(
      (rol) => String(rol.id) === String(rolIdDestino)
    );

    return rolSeleccionado?.id ?? null;
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

    if (rolesCompatibles.length === 0) {
      toast.error("No hay roles activos compatibles con el perfil seleccionado");
      return;
    }

    const rolIdDestinoValidado = obtenerRolIdDestino();

    if (!rolIdDestinoValidado) {
      toast.error("Selecciona un rol destino válido");
      return;
    }

    try {
      setGuardando(true);

      const response = await apiClient.request(
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
          maxLength={255}
          register={register}
          errors={errors}
          rules={{ required: REQUIRED, ...maxLengthRule(255) }}
        />

        <FormInput
          name="nombre"
          label="Nombre completo"
          maxLength={120}
          register={register}
          errors={errors}
          rules={{ required: REQUIRED, ...maxLengthRule(120) }}
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
          maxLength={30}
          register={register}
          errors={errors}
          rules={{
            required:
              destino === "ESTUDIANTE" ||
              destino === "ASESOR" ||
              destino === "CONCILIADOR"
                ? REQUIRED
                : false,
            ...maxLengthRule(30),
          }}
        />

        <FormInput
          name="telefono"
          label="Teléfono"
          digitsOnly
          maxLength={30}
          register={register}
          errors={errors}
          rules={{ ...digitsOnlyRule({ required: true, maxLength: 30 }) }}
        />

        <FormInput
          name="usuario"
          label="Usuario"
          maxLength={100}
          register={register}
          errors={errors}
          rules={{ required: REQUIRED, ...maxLengthRule(100) }}
        />

        <FormInput
          name="codigo"
          label="Código"
          maxLength={30}
          register={register}
          errors={errors}
          rules={{ required: REQUIRED, ...maxLengthRule(30) }}
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

          <div className="flex flex-col gap-4">
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

              {perfilesDisponibles.map((perfil) => (
                <option key={perfil.value} value={perfil.value}>
                  {perfil.label}
                </option>
              ))}
            </select>

            {errors?.destino && (
              <p className="text-xs text-red-500">
                {errors.destino.message}
              </p>
            )}
            </div>

            {perfilDestino && (
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium">Rol destino</label>

                <select
                  {...register("rolIdDestino", {
                    required:
                      rolesCompatibles.length > 0
                        ? "Selecciona un rol destino"
                        : false,
                  })}
                  disabled={rolesCompatibles.length === 0}
                  className={`flex h-9 w-full rounded-lg border bg-background px-3 py-2 text-sm ${
                    errors?.rolIdDestino ? "border-red-500" : ""
                  }`}
                >
                  <option value="">
                    {rolesCompatibles.length === 0
                      ? "No hay roles compatibles"
                      : "Seleccione un rol destino"}
                  </option>

                  {rolesCompatibles.map((rol) => (
                    <option key={rol.id} value={rol.id}>
                      {rol.nombre || `Rol #${rol.id}`}
                    </option>
                  ))}
                </select>

                {errors?.rolIdDestino && (
                  <p className="text-xs text-red-500">
                    {errors.rolIdDestino.message}
                  </p>
                )}

                {rolesCompatibles.length === 0 && (
                  <p className="text-xs text-red-500">
                    No existe un rol activo compatible con este perfil.
                  </p>
                )}
              </div>
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
