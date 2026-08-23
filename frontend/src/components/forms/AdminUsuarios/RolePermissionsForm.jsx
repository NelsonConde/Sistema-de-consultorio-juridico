"use client"

/**
 * Formulario de gestión de permisos por página para roles del sistema.
 *
 * Permite seleccionar qué páginas puede ver un rol, asignando y revocando
 * automáticamente los permisos necesarios para acceder a cada página.
 *
 * Protecciones implementadas:
 * - No permite quitarle a tu propio rol el acceso a Administración.
 * - Las acciones sin permiso muestran un toast en lugar de redirigir.
 * - El algoritmo de diff solo revoca permisos gestionados por este form,
 *   preservando permisos asignados por otros medios.
 *
 * @module components/forms/AdminUsuarios/RolePermissionsForm
 */

import { apiClient } from "@/lib/apiClient";
import React, { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { API_URL_BASE } from "@/lib/config";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";

import { PAGINAS } from "./role-permissions.config";
import {
  buscarPermiso,
  esErrorDuplicadoPermiso,
  extraerLista,
  idPermiso,
  leerRespuesta,
  nombrePermiso,
  nombreRol,
  normalizar,
  paginaMarcada,
  permisosAsignarPagina,
  permisosGestionadosPagina,
} from "./role-permissions.utils";

export function RolePermissionsForm() {
  const router = useRouter();

  const [me, setMe] = useState(null);
  const [roles, setRoles] = useState([]);
  const [permisos, setPermisos] = useState([]);
  const [rolId, setRolId] = useState("");
  const [permisosActualesRol, setPermisosActualesRol] = useState([]);
  const [paginasSeleccionadas, setPaginasSeleccionadas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingRol, setLoadingRol] = useState(false);
  const [saving, setSaving] = useState(false);
  const [mensaje, setMensaje] = useState("");
  const [error, setError] = useState("");

  const puedeAsignarPermisos = tienePermiso(
    me,
    PERMISOS.ASIGNAR_PERMISOS_ROLES
  );

  const paginasConfigurables = useMemo(() => PAGINAS, []);

  const paginaAdministracion = "/admin";

  const rolSeleccionado = useMemo(
    () => roles.find((rol) => String(rol.id) === String(rolId)) || null,
    [roles, rolId]
  );

  const rolSeleccionadoEsPropio = useMemo(() => {
    if (!me || !rolId) return false;

    const roleById = rolSeleccionado;
    if (roleById && me?.rolId != null) {
      return String(me.rolId) === String(roleById.id);
    }

    if (roleById && me?.rolNombre) {
      return normalizar(nombreRol(roleById)) === normalizar(me.rolNombre);
    }

    if (me?.rolNombre) {
      return normalizar(nombreRol({ nombre: me.rolNombre })) ===
        normalizar(me.rolNombre);
    }

    return false;
  }, [me, rolId, rolSeleccionado]);

  useEffect(() => {
    cargarDatosIniciales();
  }, []);

  useEffect(() => {
    if (rolId) {
      cargarRol(rolId);
    } else {
      setPermisosActualesRol([]);
      setPaginasSeleccionadas([]);
    }
  }, [rolId, permisos]);

  async function cargarDatosIniciales() {
    try {
      setLoading(true);
      setError("");
      setMensaje("");

      const meRes = await apiClient.request(`${API_URL_BASE}/auth/me`, {
        method: "GET",
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
        tienePermiso(meData, PERMISOS.ACCEDER_ROLES) &&
        tienePermiso(meData, PERMISOS.ASIGNAR_PERMISOS_ROLES);

      if (!puedeEntrar) {
        router.replace("/inicio");
        return;
      }

      const [rolesData, permisosData] = await Promise.all([
        cargarRolesActivos(),
        cargarPermisosActivos(),
      ]);

      setMe(meData);
      setRoles(rolesData);
      setPermisos(permisosData);
    } catch (err) {
      console.error(err);
      setError(err.message || "Error cargando datos");
    } finally {
      setLoading(false);
    }
  }

  async function cargarRolesActivos() {
    const res = await apiClient.request(`${API_URL_BASE}/roles/activos`, {
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
      throw new Error("No se pudieron cargar los roles");
    }

    const data = await res.json();
    return extraerLista(data);
  }

  async function cargarPermisosActivos() {
    const res = await apiClient.request(`${API_URL_BASE}/permisos/activos`, {
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
      throw new Error("No se pudieron cargar los permisos");
    }

    const data = await res.json();
    const lista = extraerLista(data);

    if (lista.length > 0) return lista;

    try {
      const resTodos = await apiClient.request(`${API_URL_BASE}/permisos`, {
        credentials: "include",
      });

      if (!resTodos.ok) return lista;

      const dataTodos = await resTodos.json();
      return extraerLista(dataTodos);
    } catch {
      return lista;
    }
  }

  async function crearPermiso(nombre) {
    const res = await apiClient.request(`${API_URL_BASE}/permisos`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        nombre,
        activo: true,
      }),
    });

    const data = await leerRespuesta(res);

    if (res.status === 401) {
      router.replace("/");
      return null;
    }

    if (res.status === 403) {
      router.replace("/inicio");
      return null;
    }

    if (!res.ok) {
      if (res.status === 400 && esErrorDuplicadoPermiso(data)) {
        return {
          duplicado: true,
          nombre,
        };
      }

      throw new Error(
        data?.mensaje ||
          data?.message ||
          `No se pudo crear el permiso "${nombre}"`
      );
    }

    return data;
  }

  async function asegurarPermisos(nombres) {
    let lista = [...permisos];

    for (const nombre of nombres.filter(Boolean)) {
      if (buscarPermiso(lista, nombre)) continue;

      const creado = await crearPermiso(nombre);

      if (creado?.duplicado) {
        lista = await cargarPermisosActivos();

        if (buscarPermiso(lista, nombre)) {
          continue;
        }

        throw new Error(
          `El permiso "${nombre}" ya existe en la base de datos, pero no aparece como activo. Actívalo o revisa el endpoint /permisos/activos.`
        );
      }

      if (creado && idPermiso(creado)) {
        lista = [...lista, creado];
      } else {
        lista = await cargarPermisosActivos();
      }

      if (!buscarPermiso(lista, nombre)) {
        lista = await cargarPermisosActivos();
      }

      if (!buscarPermiso(lista, nombre)) {
        throw new Error(`El permiso "${nombre}" no existe y no se pudo crear`);
      }
    }

    setPermisos(lista);
    return lista;
  }

  async function cargarRol(id) {
    try {
      setLoadingRol(true);
      setError("");
      setMensaje("");

      const res = await apiClient.request(`${API_URL_BASE}/roles/${id}`, {
        credentials: "include",
      });

      if (res.status === 401) {
        router.replace("/");
        return;
      }

      if (res.status === 403) {
        router.replace("/inicio");
        return;
      }

      if (!res.ok) {
        throw new Error("No se pudo cargar el rol");
      }

      const rol = await res.json();
      const permisosRol = Array.isArray(rol?.permisos) ? rol.permisos : [];

      const paginas = paginasConfigurables
        .filter((page) => paginaMarcada(page, permisosRol))
        .map((page) => page.path);

      setPermisosActualesRol(permisosRol);
      setPaginasSeleccionadas(paginas);
    } catch (err) {
      console.error(err);
      setPermisosActualesRol([]);
      setPaginasSeleccionadas([]);
      setError(err.message || "Error cargando rol");
    } finally {
      setLoadingRol(false);
    }
  }

  /**
   * Alterna la selección de una página en el formulario de permisos.
   * Muestra toast si el usuario no tiene permiso para asignar permisos.
   * Muestra error en UI si intenta quitarse acceso a Administración.
   *
   * @param {string} path - Ruta de la página a togglear.
   */
  function togglePagina(path) {
    if (!puedeAsignarPermisos) {
      toast.error("Sin permiso", {
        description: "No tienes permiso para asignar permisos a roles.",
      });
      return;
    }

    if (
      path === paginaAdministracion &&
      paginasSeleccionadas.includes(path) &&
      rolSeleccionadoEsPropio
    ) {
      setError(
        "No puedes quitar el acceso a Administración de tu propio rol."
      );
      return;
    }

    setError("");
    setMensaje("");

    setPaginasSeleccionadas((prev) =>
      prev.includes(path)
        ? prev.filter((item) => item !== path)
        : [...prev, path]
    );
  }

  function nombresPermisosObjetivo() {
    const paths = new Set(paginasSeleccionadas);
    const nombres = new Set();

    paginasConfigurables.forEach((page) => {
      if (!paths.has(page.path)) return;

      permisosAsignarPagina(page, rolSeleccionado)
        .filter(Boolean)
        .forEach((permiso) => {
          nombres.add(permiso);
        });
    });

    return [...nombres];
  }

  function nombresPermisosGestionados() {
    const nombres = new Set();

    paginasConfigurables.forEach((page) => {
      permisosGestionadosPagina(page).forEach((permiso) => {
        nombres.add(permiso);
      });
    });

    return [...nombres];
  }
  function idsDesdeNombres(listaPermisos, nombres) {
    return nombres
      .filter(Boolean)
      .map((nombre) => buscarPermiso(listaPermisos, nombre))
      .map(idPermiso)
      .filter((id) => id !== null && id !== undefined);
  }

  async function cambiarPermisoRol(permisoId, method) {
    const res = await apiClient.request(
      `${API_URL_BASE}/roles/${rolId}/permisos/${permisoId}`,
      {
        method,
        credentials: "include",
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
      const data = await leerRespuesta(res);

      throw new Error(
        data?.mensaje ||
          data?.message ||
          (method === "PATCH"
            ? "No se pudo agregar un permiso"
            : "No se pudo quitar un permiso")
      );
    }
  }

  async function guardar() {
  if (!puedeAsignarPermisos) {
    router.replace("/inicio");
    return;
  }

  if (!rolId) {
    setError("Selecciona un rol");
    return;
  }

  try {
    setSaving(true);
    setError("");
    setMensaje("");

    const nombresObjetivo = nombresPermisosObjetivo();
    const nombresGestionados = nombresPermisosGestionados();

    const permisosActualizados = await asegurarPermisos(nombresObjetivo);

    const actuales = new Set(
      permisosActualesRol
        .map(idPermiso)
        .filter((id) => id !== null && id !== undefined)
    );

    const objetivo = new Set(
      idsDesdeNombres(permisosActualizados, nombresObjetivo)
    );

    const gestionados = new Set(
      idsDesdeNombres(permisosActualizados, nombresGestionados)
    );

    const agregar = [...objetivo].filter((id) => !actuales.has(id));

    const quitar = [...actuales].filter(
      (id) => gestionados.has(id) && !objetivo.has(id)
    );

    for (const permisoId of agregar) {
      await cambiarPermisoRol(permisoId, "PATCH");
    }

    for (const permisoId of quitar) {
      await cambiarPermisoRol(permisoId, "DELETE");
    }

    setMensaje("Permisos actualizados correctamente");
    await cargarRol(rolId);
  } catch (err) {
    console.error(err);
    setError(err.message || "Error guardando permisos");
  } finally {
    setSaving(false);
  }
}

  if (loading) {
    return <div className="py-10 text-center">Cargando permisos...</div>;
  }

  return (
    <div className="space-y-6 rounded-xl border bg-card p-6">
      <div>
        <h2 className="text-2xl font-bold">Permisos por página</h2>
        <p className="text-sm text-muted-foreground">
          Selecciona un rol y marca las páginas que podrá ver.
        </p>
      </div>

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

      <div className="flex flex-col gap-1.5">
        <label className="text-sm font-medium">Rol</label>
        <select
          value={rolId}
          onChange={(event) => setRolId(event.target.value)}
          className="h-10 rounded-md border bg-background px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <option value="">Selecciona un rol</option>

          {roles.map((rol) => (
            <option key={rol.id} value={rol.id}>
              {nombreRol(rol)}
            </option>
          ))}
        </select>
      </div>

      {rolId && (
        <div className="space-y-4">
          {loadingRol ? (
            <div className="rounded-lg border bg-muted/30 p-6 text-center text-sm text-muted-foreground">
              Cargando páginas del rol...
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {paginasConfigurables.map((page) => {
                const selected = paginasSeleccionadas.includes(page.path);

                return (
                  <React.Fragment key={page.path}>
                    <button
                      type="button"
                      onClick={() => togglePagina(page.path)}
                      className={`rounded-lg border px-4 py-3 text-sm font-medium transition ${
                        selected
                          ? "border-primary bg-primary text-primary-foreground"
                          : "bg-background hover:bg-muted"
                      }`}
                    >
                      {page.title}
                    </button>
                  </React.Fragment>
                );
              })}
            </div>
          )}

          <div className="flex justify-end">
            <Button
              type="button"
              onClick={guardar}
              disabled={saving || loadingRol}
            >
              {saving ? "Guardando..." : "Guardar"}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
