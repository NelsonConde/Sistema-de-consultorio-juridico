"use client"

/**
 * Formulario de gestión de asesores y monitores.
 *
 * Lista los perfiles activos con opciones de búsqueda y paginación.
 * Requiere permiso `ACCEDER_ASESORES_MONITORES`.
 *
 * @module components/forms/usuarios/AsesoresYMonitoresForm
 */
;

import React, { useEffect, useState } from "react";
import { Input } from "@/components/ui/input";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { API_URL_BASE } from "@/lib/config";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { PERMISOS } from "@/lib/permission";
import { tienePermiso } from "@/lib/authz";
import { getTotalPages, paginateItems, sortByIdAsc } from "@/lib/list-utils";

/**
 * Formulario para gestionar asesores y monitores.
 * @returns {JSX.Element} Componente de administración de asesores y monitores.
 */
export function AsesoresYMonitoresForm() {
  const router = useRouter();

  const [asesores, setAsesores] = useState([]);
  const [monitores, setMonitores] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [cargando, setCargando] = useState(true);

  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);
  const REGISTROS_POR_PAGINA_OPTIONS = [5, 10, 20, 50];

  const [confirmDialog, setConfirmDialog] = useState(null);
  const [confirmLoading, setConfirmLoading] = useState(false);
  const [puedeGestionar, setPuedeGestionar] = useState(false);

  useEffect(() => {
    const verificarYcargar = async () => {
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

        const usuario = await res.json();

        const puedeEntrar =
          tienePermiso(usuario, PERMISOS.ACCEDER_ASESORES_MONITORES) &&
          tienePermiso(usuario, PERMISOS.VER_ASESORES_MONITORES);

        if (!puedeEntrar) {
          router.replace("/inicio");
          return;
        }

        setPuedeGestionar(
          tienePermiso(usuario, PERMISOS.GESTIONAR_ASESORES_MONITORES)
        );

        const [asesoresRes, monitoresRes] = await Promise.all([
          fetch(`${API_URL_BASE}/asesores/activos`, {
            credentials: "include",
          }),
          fetch(`${API_URL_BASE}/monitores/activos`, {
            credentials: "include",
          }),
        ]);

        if (asesoresRes.status === 401 || monitoresRes.status === 401) {
          router.replace("/");
          return;
        }

        if (asesoresRes.status === 403 || monitoresRes.status === 403) {
          router.replace("/inicio");
          return;
        }

        const asesoresData = await asesoresRes.json();
        const monitoresData = await monitoresRes.json();

        setAsesores(sortByIdAsc(Array.isArray(asesoresData) ? asesoresData : []));
        setMonitores(sortByIdAsc(Array.isArray(monitoresData) ? monitoresData : []));
      } catch (err) {
        console.error(err);
        toast.error("Error cargando usuarios");
      } finally {
        setCargando(false);
      }
    };

    verificarYcargar();
  }, [router]);

  function abrirConfirmacionDesactivar(usuario) {
    if (!puedeGestionar) {
      router.replace("/inicio");
      return;
    }

    setConfirmDialog(usuario);
  }

  async function confirmarDesactivar() {
    if (!confirmDialog?.id || !confirmDialog?.rol) return;

    try {
      setConfirmLoading(true);

      const endpoint =
        confirmDialog.rol === "Asesor" ? "asesores" : "monitores";

      const res = await fetch(
        `${API_URL_BASE}/${endpoint}/${confirmDialog.id}/activo?activo=false`,
        {
          method: "PATCH",
          credentials: "include",
        }
      );

      if (res.ok) {
        toast.success(`${confirmDialog.rol} desactivado`);

        if (confirmDialog.rol === "Asesor") {
          setAsesores((prev) =>
            sortByIdAsc(
              prev.map((a) =>
                a.id === confirmDialog.id ? { ...a, activo: false } : a
              )
            )
          );
        } else {
          setMonitores((prev) =>
            sortByIdAsc(
              prev.map((m) =>
                m.id === confirmDialog.id ? { ...m, activo: false } : m
              )
            )
          );
        }

        setConfirmDialog(null);
      } else {
        toast.error("Error al desactivar");
      }
    } catch (error) {
      console.error(error);
      toast.error("Error de conexión");
    } finally {
      setConfirmLoading(false);
    }
  }

  const usuarios = sortByIdAsc([
    ...asesores.map((a) => ({ ...a, rol: "Asesor" })),
    ...monitores.map((m) => ({ ...m, rol: "Monitor" })),
  ]);

  const filtrados = sortByIdAsc(
    usuarios.filter((u) =>
      `${u.nombre || ""} ${u.documento || ""} ${u.email || ""}`
        .toLowerCase()
        .includes(busqueda.toLowerCase())
    )
  );
  const totalPaginas = getTotalPages(filtrados.length, registrosPorPagina);
  const usuariosPaginados = paginateItems(filtrados, paginaActual, registrosPorPagina);

  useEffect(() => {
    setPaginaActual(1);
  }, [busqueda]);

  useEffect(() => {
    if (paginaActual > totalPaginas) {
      setPaginaActual(totalPaginas);
    }
  }, [paginaActual, totalPaginas]);

  if (cargando) {
    return <div className="text-center mt-10">Cargando...</div>;
  }

  return (
    <div className="space-y-6">
      <Input
        placeholder="Buscar por nombre, documento o email..."
        value={busqueda}
        onChange={(e) => setBusqueda(e.target.value)}
      />

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
            {usuariosPaginados.map((u) => (
              <tr
                key={`${u.rol}-${u.id}`}
                className="border-t hover:bg-muted/30 transition"
              >
                <td className="p-3">{u.id}</td>
                <td className="p-3 font-medium">{u.nombre}</td>
                <td className="p-3">{u.documento}</td>
                <td className="p-3">{u.email}</td>
                <td className="p-3">{u.telefono}</td>
                <td className="p-3">{u.codigo}</td>

                <td className="p-3">
                  <span className="px-2 py-1 text-xs rounded bg-primary/10 text-primary">
                    {u.rol}
                  </span>
                </td>

                <td className="p-3">
                  <span
                    className={`text-xs px-2 py-1 rounded ${u.activo
                        ? "bg-blue-100 text-blue-700"
                        : "bg-red-100 text-red-700"
                      }`}
                  >
                    {u.activo ? "Activo" : "Inactivo"}
                  </span>
                </td>

                <td className="p-3">
                  {puedeGestionar && (
                    <button
                      type="button"
                      onClick={() => abrirConfirmacionDesactivar(u)}
                      disabled={!u.activo}
                      className={`text-xs px-3 py-1 rounded ${u.activo
                          ? "bg-red-100 text-red-700 hover:bg-red-200"
                          : "bg-gray-100 text-gray-400 cursor-not-allowed"
                        }`}
                    >
                      {u.activo ? "Desactivar" : "Inactivo"}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Pagination
        currentPage={paginaActual}
        totalPages={totalPaginas}
        onPageChange={(p) => setPaginaActual(p)}
        pageSize={registrosPorPagina}
        onPageSizeChange={(v) => { setRegistrosPorPagina(v); setPaginaActual(1); }}
        pageSizeOptions={REGISTROS_POR_PAGINA_OPTIONS}
        totalItems={filtrados.length}
      />

      {filtrados.length === 0 && (
        <p className="text-center text-muted-foreground">
          No se encontraron resultados
        </p>
      )}

      <ConfirmActionDialog
        open={Boolean(confirmDialog)}
        title={`Desactivar ${confirmDialog?.rol?.toLowerCase() || "usuario"}`}
        description={`¿Deseas desactivar a "${confirmDialog?.nombre || "este usuario"
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