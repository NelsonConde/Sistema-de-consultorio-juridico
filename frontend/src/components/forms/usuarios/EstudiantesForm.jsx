/**
 * Formulario de gestión de estudiantes.
 *
 * Lista los estudiantes registrados con paginación, permite filtrar por búsqueda
 * y desactivar estudiantes mediante un diálogo de confirmación.
 *
 * Las acciones sin permiso muestran un toast en lugar de redirigir,
 * para que el usuario no pierda el contexto de la pantalla.
 *
 * @module components/forms/usuarios/EstudiantesForm
 */
"use client";

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
 * Formulario para listar y administrar estudiantes.
 * @returns {JSX.Element} Componente de lista de estudiantes.
 */
export function EstudiantesForm() {
  const router = useRouter();

  const [estudiantes, setEstudiantes] = useState([]);
  const [busqueda, setBusqueda] = useState("");
  const [cargando, setCargando] = useState(true);
  const [puedeCambiarEstado, setPuedeCambiarEstado] = useState(false);

  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);
  const REGISTROS_POR_PAGINA_OPTIONS = [5, 10, 20, 50];

  const [confirmDialog, setConfirmDialog] = useState(null);
  const [confirmLoading, setConfirmLoading] = useState(false);

  useEffect(() => {
    const verificarYCargar = async () => {
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
          tienePermiso(usuario, PERMISOS.ACCEDER_ESTUDIANTES) &&
          tienePermiso(usuario, PERMISOS.VER_ESTUDIANTES);

        if (!puedeEntrar) {
          router.replace("/inicio");
          return;
        }

        setPuedeCambiarEstado(
          tienePermiso(usuario, PERMISOS.CAMBIAR_ESTADO_ESTUDIANTES)
        );

        let url = `${API_URL_BASE}/estudiantes/activos`;

        if (usuario.tipoPerfil === "ASESOR" && usuario.perfilId) {
          url = `${API_URL_BASE}/estudiantes/activos/asesor/${usuario.perfilId}`;
        }

        const estudiantesRes = await fetch(url, {
          credentials: "include",
        });

        if (estudiantesRes.status === 401) {
          router.replace("/");
          return;
        }

        if (estudiantesRes.status === 403) {
          router.replace("/inicio");
          return;
        }

        if (!estudiantesRes.ok) {
          toast.error("Error cargando estudiantes");
          setEstudiantes([]);
          return;
        }

        const data = await estudiantesRes.json();

        if (Array.isArray(data)) {
          setEstudiantes(sortByIdAsc(data));
        } else if (Array.isArray(data.content)) {
          setEstudiantes(sortByIdAsc(data.content));
        } else if (Array.isArray(data.data)) {
          setEstudiantes(sortByIdAsc(data.data));
        } else {
          setEstudiantes([]);
          toast.error("La API no devolvió una lista");
        }
      } catch (e) {
        console.error(e);
        toast.error("Error cargando estudiantes");
      } finally {
        setCargando(false);
      }
    };

    verificarYCargar();
  }, [router]);

  /**
   * Abre el diálogo de confirmación para desactivar un estudiante.
   * Muestra un toast de error si el usuario no tiene el permiso requerido,
   * sin redirigirlo para que no pierda el contexto.
   *
   * @param {object} estudiante - El estudiante a desactivar.
   */
  function abrirConfirmacionDesactivar(estudiante) {
    if (!puedeCambiarEstado) {
      toast.error("Sin permiso", {
        description: "No tienes permiso para cambiar el estado de estudiantes.",
      });
      return;
    }

    setConfirmDialog(estudiante);
  }

  async function confirmarDesactivar() {
    if (!confirmDialog?.id) return;

    if (!puedeCambiarEstado) {
      router.replace("/inicio");
      return;
    }

    try {
      setConfirmLoading(true);

      const res = await fetch(
        `${API_URL_BASE}/estudiantes/${confirmDialog.id}/activo?activo=false`,
        {
          method: "PATCH",
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

      if (res.ok) {
        toast.success("Estudiante desactivado");

        setEstudiantes((prev) =>
          sortByIdAsc(
            prev.map((e) =>
              e.id === confirmDialog.id ? { ...e, activo: false } : e
            )
          )
        );

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

  const filtrados = sortByIdAsc(
    estudiantes.filter((e) =>
      `${e.nombre} ${e.documento} ${e.email} ${e.codigo}`
        .toLowerCase()
        .includes(busqueda.toLowerCase())
    )
  );
  const totalPaginas = getTotalPages(filtrados.length, registrosPorPagina);
  const estudiantesPaginados = paginateItems(filtrados, paginaActual, registrosPorPagina);

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
        placeholder="Buscar por nombre, documento, email o código..."
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
              <th className="p-3">Conciliación</th>
              <th className="p-3">Estado</th>
              <th className="p-3">Acciones</th>
            </tr>
          </thead>

          <tbody>
            {estudiantesPaginados.map((e) => (
              <tr
                key={e.id}
                className="border-t hover:bg-muted/30 transition"
              >
                <td className="p-3">{e.id}</td>
                <td className="p-3 font-medium">{e.nombre}</td>
                <td className="p-3">{e.documento}</td>
                <td className="p-3">{e.email}</td>
                <td className="p-3">{e.telefono}</td>
                <td className="p-3">{e.codigo}</td>

                <td className="p-3">
                  <span
                    className={`text-xs px-2 py-1 rounded ${
                      e.conciliacion
                        ? "bg-green-100 text-green-700"
                        : "bg-gray-100 text-gray-600"
                    }`}
                  >
                    {e.conciliacion ? "Sí" : "No"}
                  </span>
                </td>

                <td className="p-3">
                  <span
                    className={`text-xs px-2 py-1 rounded ${
                      e.activo
                        ? "bg-blue-100 text-blue-700"
                        : "bg-red-100 text-red-700"
                    }`}
                  >
                    {e.activo ? "Activo" : "Inactivo"}
                  </span>
                </td>

                <td className="p-3">
                  {puedeCambiarEstado ? (
                    <button
                      type="button"
                      onClick={() => abrirConfirmacionDesactivar(e)}
                      disabled={!e.activo}
                      className={`text-xs px-3 py-1 rounded ${
                        e.activo
                          ? "bg-red-100 text-red-700 hover:bg-red-200"
                          : "bg-gray-100 text-gray-400 cursor-not-allowed"
                      }`}
                    >
                      {e.activo ? "Desactivar" : "Inactivo"}
                    </button>
                  ) : (
                    <span className="text-xs text-muted-foreground">—</span>
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
        title="Desactivar estudiante"
        description={`¿Deseas desactivar a "${
          confirmDialog?.nombre || "este estudiante"
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