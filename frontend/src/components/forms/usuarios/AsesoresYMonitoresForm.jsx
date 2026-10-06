"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { useDebouncedPageSearch } from "@/hooks/useDebouncedValue";
import { apiClient } from "@/lib/apiClient";
import { API_URL_BASE } from "@/lib/config";
import { fetchPaged, isAbortError } from "@/lib/pagedApi";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

const TABS = {
  asesores: { label: "Asesores", endpoint: "/asesores", rol: "Asesor" },
  monitores: { label: "Monitores", endpoint: "/monitores", rol: "Monitor" },
};

export function AsesoresYMonitoresForm() {
  const router = useRouter();

  const [autorizado, setAutorizado] = useState(false);
  const [tipoActivo, setTipoActivo] = useState("asesores");
  const [usuarios, setUsuarios] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [paginaActual, setPaginaActual] = useState(1);
  const busquedaAplicada = useDebouncedPageSearch(busqueda, setPaginaActual, 300).trim();
  const [activo, setActivo] = useState("");
  const [sortBy, setSortBy] = useState("id");
  const [direction, setDirection] = useState("desc");
  const [cargando, setCargando] = useState(true);
  const [errorLista, setErrorLista] = useState("");
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);
  const [totalRegistros, setTotalRegistros] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [confirmDialog, setConfirmDialog] = useState(null);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [puedeGestionar, setPuedeGestionar] = useState(false);

  const tab = TABS[tipoActivo];

  useEffect(() => {
    let mounted = true;

    async function verificar() {
      try {
        const res = await apiClient.get(`${API_URL_BASE}/auth/me`);
        if (res.status === 401) {
          router.replace("/");
          return;
        }
        if (!res.ok) {
          router.replace("/");
          return;
        }

        const usuario = await res.json();
        const puedeEntrar =
          tienePermiso(usuario, PERMISOS.ACCEDER_ASESORES_MONITORES) &&
          tienePermiso(usuario, PERMISOS.VER_ASESORES_MONITORES);

        if (!puedeEntrar) {
          router.replace("/inicio");
          return;
        }

        if (!mounted) return;
        setPuedeGestionar(
          tienePermiso(usuario, PERMISOS.GESTIONAR_ASESORES_MONITORES)
        );
        setAutorizado(true);
      } catch {
        router.replace("/");
      }
    }

    verificar();
    return () => {
      mounted = false;
    };
  }, [router]);

  useEffect(() => {
    if (!autorizado) return undefined;

    const controller = new AbortController();

    async function cargar() {
      setCargando(true);
      setErrorLista("");

      try {
        const page = await fetchPaged(tab.endpoint, {
          search: busquedaAplicada,
          page: paginaActual,
          size: registrosPorPagina,
          sortBy,
          direction,
          filters: { activo },
          signal: controller.signal,
          resourceName: tab.label.toLowerCase(),
        });

        setUsuarios(page.content.map((item) => ({ ...item, rol: tab.rol })));
        setTotalRegistros(page.totalElements);
        setTotalPaginas(page.totalPages);

        if (page.totalPages > 0 && page.page > page.totalPages) {
          setPaginaActual(page.totalPages);
        } else if (page.page !== paginaActual) {
          setPaginaActual(page.page);
        }
      } catch (error) {
        if (isAbortError(error)) return;
        if (error?.status === 401) {
          router.replace("/");
          return;
        }

        setUsuarios([]);
        setTotalRegistros(0);
        setTotalPaginas(0);
        setErrorLista(
          error?.status === 403
            ? `No tienes permiso para consultar ${tab.label.toLowerCase()}.`
            : error?.message || `Error cargando ${tab.label.toLowerCase()}.`
        );
      } finally {
        if (!controller.signal.aborted) setCargando(false);
      }
    }

    cargar();
    return () => controller.abort();
  }, [
    autorizado,
    tab.endpoint,
    tab.label,
    tab.rol,
    busquedaAplicada,
    paginaActual,
    registrosPorPagina,
    activo,
    sortBy,
    direction,
    reloadKey,
    router,
  ]);

  function abrirConfirmacionDesactivar(usuario) {
    if (!puedeGestionar) {
      toast.error("No tienes permiso para gestionar asesores y monitores.");
      return;
    }
    setConfirmDialog(usuario);
  }

  async function confirmarDesactivar() {
    if (!confirmDialog?.id || !confirmDialog?.rol) return;

    try {
      setConfirmLoading(true);
      const endpoint = confirmDialog.rol === "Asesor" ? "asesores" : "monitores";
      const res = await apiClient.patch(
        `${API_URL_BASE}/${endpoint}/${confirmDialog.id}/activo?activo=false`
      );

      if (res.status === 401) {
        router.replace("/");
        return;
      }
      if (res.status === 403) {
        setUsuarios([]);
        setTotalRegistros(0);
        setTotalPaginas(0);
        setErrorLista("No tienes permiso para cambiar el estado de este perfil.");
        return;
      }
      if (!res.ok) {
        toast.error("Error al desactivar");
        return;
      }

      toast.success(`${confirmDialog.rol} desactivado`);
      setConfirmDialog(null);
      if (usuarios.length === 1 && paginaActual > 1) {
        setPaginaActual((page) => page - 1);
      } else {
        setReloadKey((value) => value + 1);
      }
    } catch {
      toast.error("Error de conexión");
    } finally {
      setConfirmLoading(false);
    }
  }

  if (!autorizado && cargando) {
    return <div className="text-center mt-10">Cargando...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex gap-2 border-b pb-2">
        {Object.entries(TABS).map(([key, item]) => (
          <Button
            key={key}
            type="button"
            size="sm"
            variant={tipoActivo === key ? "default" : "outline"}
            onClick={() => {
              setTipoActivo(key);
              setPaginaActual(1);
            }}
          >
            {item.label}
          </Button>
        ))}
      </div>

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <Input
          placeholder="Buscar por nombre, documento o email..."
          value={busqueda}
          onChange={(event) => setBusqueda(event.target.value)}
          className="md:col-span-2"
        />
        <select
          value={activo}
          onChange={(event) => {
            setActivo(event.target.value);
            setPaginaActual(1);
          }}
          className="h-9 rounded-md border bg-background px-3 text-sm"
        >
          <option value="">Todos</option>
          <option value="true">Activos</option>
          <option value="false">Inactivos</option>
        </select>
        <div className="flex gap-2">
          <select
            value={sortBy}
            onChange={(event) => {
              setSortBy(event.target.value);
              setPaginaActual(1);
            }}
            className="h-9 min-w-0 flex-1 rounded-md border bg-background px-2 text-sm"
          >
            <option value="id">ID</option>
            <option value="nombre">Nombre</option>
            <option value="documento">Documento</option>
            <option value="email">Email</option>
            <option value="codigo">Código</option>
            <option value="activo">Estado</option>
          </select>
          <select
            value={direction}
            onChange={(event) => {
              setDirection(event.target.value);
              setPaginaActual(1);
            }}
            className="h-9 rounded-md border bg-background px-2 text-sm"
            aria-label="Dirección de orden"
          >
            <option value="asc">Asc</option>
            <option value="desc">Desc</option>
          </select>
        </div>
      </div>

      {errorLista && (
        <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {errorLista}
        </div>
      )}

      <div className="overflow-x-auto border rounded-xl">
        <table className="w-full text-sm">
          <thead className="bg-muted/50">
            <tr className="text-left">
              <th className="p-3">ID</th>
              <th className="p-3">Nombre</th>
              <th className="p-3">Documento</th>
              <th className="p-3">Email</th>
              <th className="p-3">Teléfono</th>
              <th className="p-3">Código</th>
              <th className="p-3">Rol</th>
              <th className="p-3">Estado</th>
              <th className="p-3">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {cargando && usuarios.length === 0 ? (
              <tr>
                <td colSpan={9} className="p-6 text-center text-muted-foreground">
                  Cargando {tab.label.toLowerCase()}...
                </td>
              </tr>
            ) : usuarios.length === 0 ? (
              <tr>
                <td colSpan={9} className="p-6 text-center text-muted-foreground">
                  No se encontraron resultados.
                </td>
              </tr>
            ) : (
              usuarios.map((usuario) => (
                <tr key={`${usuario.rol}-${usuario.id}`} className="border-t hover:bg-muted/30 transition">
                  <td className="p-3">{usuario.id}</td>
                  <td className="p-3 font-medium">{usuario.nombre}</td>
                  <td className="p-3">{usuario.documento || "—"}</td>
                  <td className="p-3">{usuario.email || "—"}</td>
                  <td className="p-3">—</td>
                  <td className="p-3">{usuario.codigo || "—"}</td>
                  <td className="p-3">
                    <span className="px-2 py-1 text-xs rounded bg-primary/10 text-primary">
                      {usuario.rol}
                    </span>
                  </td>
                  <td className="p-3">
                    <span
                      className={`text-xs px-2 py-1 rounded ${
                        usuario.activo
                          ? "bg-blue-100 text-blue-700"
                          : "bg-red-100 text-red-700"
                      }`}
                    >
                      {usuario.activo ? "Activo" : "Inactivo"}
                    </span>
                  </td>
                  <td className="p-3">
                    {puedeGestionar ? (
                      <button
                        type="button"
                        onClick={() => abrirConfirmacionDesactivar(usuario)}
                        disabled={!usuario.activo}
                        className={`text-xs px-3 py-1 rounded ${
                          usuario.activo
                            ? "bg-red-100 text-red-700 hover:bg-red-200"
                            : "bg-gray-100 text-gray-400 cursor-not-allowed"
                        }`}
                      >
                        {usuario.activo ? "Desactivar" : "Inactivo"}
                      </button>
                    ) : (
                      <span className="text-xs text-muted-foreground">—</span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <Pagination
        currentPage={paginaActual}
        totalPages={totalPaginas}
        onPageChange={setPaginaActual}
        pageSize={registrosPorPagina}
        onPageSizeChange={(value) => {
          setRegistrosPorPagina(value);
          setPaginaActual(1);
        }}
        pageSizeOptions={PAGE_SIZE_OPTIONS}
        totalItems={totalRegistros}
      />

      <ConfirmActionDialog
        open={Boolean(confirmDialog)}
        title={`Desactivar ${confirmDialog?.rol?.toLowerCase() || "usuario"}`}
        description={`¿Deseas desactivar a "${
          confirmDialog?.nombre || "este usuario"
        }"? Podrás reactivarlo después desde la página de eliminación.`}
        confirmText="Desactivar"
        cancelText="Cancelar"
        loading={confirmLoading}
        onClose={() => setConfirmDialog(null)}
        onConfirm={confirmarDesactivar}
      />
    </div>
  );
}
