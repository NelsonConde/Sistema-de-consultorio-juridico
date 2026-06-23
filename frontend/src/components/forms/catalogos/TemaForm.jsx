"use client"

/**
 * Formulario de gestión de temas jurídicos.
 *
 * Permite crear, editar y desactivar temas agrupados por área jurídica.
 * Requiere permiso `GESTIONAR_CATALOGOS`.
 *
 * @module components/forms/catalogos/TemaForm
 */
;

import React, { useState, useEffect, useMemo } from "react";
import { useApiForm } from "@/hooks/useApiForm";
import { useForm } from "react-hook-form";
import { FormInput } from "../parts/FormInput";
import { FormSelect } from "../parts/FormSelect";
import { Button } from "@/components/ui/button";
import { useRouter } from "next/navigation";
import { API_URL_BASE } from "@/lib/config";
import { toast } from "sonner";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import { PERMISOS } from "@/lib/permission";
import { tieneAlgunPermiso, tienePermiso } from "@/lib/authz";
import { getApiErrorDescription, getApiErrorTitle, readResponseBody } from "@/lib/api";
import { requiredSelectRule } from "@/lib/form-validation";
import Pagination from "@/components/ui/Pagination";
import { DEFAULT_PAGE_SIZE_OPTIONS, getTotalPages, paginateItems, sortByIdAsc } from "@/lib/list-utils";

/**
 * Formulario para gestionar temas de catálogo.
 * @returns {JSX.Element} Componente de formulario de temas.
 */
export function TemaForm() {
  const router = useRouter();
  const [checking, setChecking] = useState(true);
  const [temas, setTemas] = useState([]);
  const [areas, setAreas] = useState([]);
  const [editandoId, setEditandoId] = useState(null);

  const [temaADesactivar, setTemaADesactivar] = useState(null);
  const [desactivando, setDesactivando] = useState(false);
  const [puedeGestionarCatalogos, setPuedeGestionarCatalogos] = useState(false);
  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: { nombre: "", areaId: "" },
  });

  const { submit, isSubmitting } = useApiForm({
    endpoint: `${API_URL_BASE}/temas`,
  });

  useEffect(() => {
  const verificar = async () => {
    try {
      const res = await fetch(`${API_URL_BASE}/auth/me`, {
        method: "GET",
        credentials: "include",
      });

      if (res.status === 401) {
        router.push("/");
        return;
      }

      if (!res.ok) {
        router.push("/");
        return;
      }

      const user = await res.json();

      const puedeGestionar = tienePermiso(
        user,
        PERMISOS.GESTIONAR_CATALOGOS
      );

      setPuedeGestionarCatalogos(puedeGestionar);

      if (!puedeGestionar) {
        router.push("/inicio");
        return;
      }

      const response = await fetch(`${API_URL_BASE}/areas`, {
        credentials: "include",
      });

      if (response.status === 401) {
        router.push("/");
        return;
      }

      if (response.status === 403) {
        router.push("/inicio");
        return;
      }

      if (response.ok) {
        const areasData = await response.json();

        setAreas(
          sortByIdAsc(areasData).map((area) => ({
            value: area.id.toString(),
            label: area.nombre,
          }))
        );
      }

      await cargarTemas();
    } catch (error) {
      console.error(error);
      router.push("/");
    } finally {
      setChecking(false);
    }
  };

  verificar();
}, [router]);

  async function cargarTemas() {
    const res = await fetch(`${API_URL_BASE}/temas`, {
      credentials: "include",
    });

    const data = await res.json();
    setTemas(sortByIdAsc(Array.isArray(data) ? data : []));
  }

  function abrirEditar(tema) {
    if (!puedeGestionarCatalogos) {
      toast.error("No tienes permiso para editar catálogos");
      return;
    }

    setEditandoId(tema.id);
    reset({
      nombre: tema.nombre,
      areaId: tema.areaId?.toString(),
    });
  }

  function cancelarEdicion() {
    setEditandoId(null);
    reset({ nombre: "", areaId: "" });
  }

  function abrirConfirmacionDesactivar(tema) {
    if (!puedeGestionarCatalogos) {
      toast.error("No tienes permiso para desactivar catálogos");
      return;
    }

    setTemaADesactivar(tema);
  }

  function cerrarConfirmacionDesactivar() {
    if (desactivando) return;
    setTemaADesactivar(null);
  }

  async function confirmarDesactivarTema() {
    if (!temaADesactivar?.id) return;

    if (!puedeGestionarCatalogos) {
      toast.error("No tienes permiso para desactivar catálogos");
      setTemaADesactivar(null);
      return;
    }

    try {
      setDesactivando(true);

      const res = await fetch(
        `${API_URL_BASE}/temas/${temaADesactivar.id}/activo?activo=false`,
        {
          method: "PATCH",
          credentials: "include",
        }
      );

      if (res.ok || res.status === 204) {
        toast.success("Tema desactivado");
        setTemaADesactivar(null);
        cargarTemas();
      } else {
        const payload = await readResponseBody(res);
        toast.error(getApiErrorTitle(payload, "Error al desactivar"), {
          description: getApiErrorDescription(payload),
        });
      }
    } catch (error) {
      console.error(error);
      toast.error("Error de conexión");
    } finally {
      setDesactivando(false);
    }
  }

  const onSubmit = async (data) => {
    if (!puedeGestionarCatalogos) {
      toast.error("No tienes permiso para gestionar catálogos");
      return;
    }

    if (editandoId) {
      const res = await fetch(`${API_URL_BASE}/temas/${editandoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(data),
      });

      const payload = await readResponseBody(res);

      if (res.ok) {
        toast.success("Tema actualizado");
        setEditandoId(null);
        reset({ nombre: "", areaId: "" });
        cargarTemas();
      } else {
        toast.error(getApiErrorTitle(payload, "Error al actualizar"), {
          description: getApiErrorDescription(payload),
        });
      }
    } else {
      const result = await submit(data);

      if (result?.success) {
        reset({ nombre: "", areaId: "" });
        cargarTemas();
      }
    }
  };

  const temasOrdenados = useMemo(() => sortByIdAsc(temas), [temas]);
  const totalPaginas = getTotalPages(temasOrdenados.length, registrosPorPagina);
  const temasPaginados = useMemo(
    () => paginateItems(temasOrdenados, paginaActual, registrosPorPagina),
    [temasOrdenados, paginaActual, registrosPorPagina]
  );

  useEffect(() => {
    setPaginaActual(1);
  }, [registrosPorPagina]);

  useEffect(() => {
    if (paginaActual > totalPaginas) {
      setPaginaActual(totalPaginas);
    }
  }, [paginaActual, totalPaginas]);

  if (checking) {
    return <div className="text-center mt-10">Cargando...</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <FormInput
          name="nombre"
          label="Nombre"
          register={register}
          errors={errors}
          rules={{ required: "El nombre es obligatorio" }}
        />

        <FormSelect
          name="areaId"
          label="Área"
          options={areas}
          register={register}
          errors={errors}
          rules={requiredSelectRule("Debe seleccionar un área")}
        />

        <div className="flex gap-3 mt-2">
          <Button
            onClick={handleSubmit(onSubmit)}
            disabled={isSubmitting || !puedeGestionarCatalogos}
          >
            {isSubmitting
              ? "Guardando..."
              : editandoId
              ? "Actualizar tema"
              : "Guardar tema"}
          </Button>

          {editandoId && (
            <Button variant="outline" type="button" onClick={cancelarEdicion}>
              Cancelar
            </Button>
          )}
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border bg-card">
        <table className="min-w-full">
          <thead className="bg-muted">
            <tr>
              {["ID", "Nombre", "Área", "Estado", "Acciones"].map((h) => (
                <th
                  key={h}
                  className="px-4 py-3 text-left text-xs font-medium"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>

          <tbody>
            {temasOrdenados.length === 0 ? (
              <tr>
                <td
                  colSpan={5}
                  className="text-center py-8 text-sm text-muted-foreground"
                >
                  Sin temas registrados.
                </td>
              </tr>
            ) : (
              temasPaginados.map((tema) => (
                <tr key={tema.id} className="border-t hover:bg-muted/50">
                  <td className="px-4 py-3 text-sm">{tema.id}</td>

                  <td className="px-4 py-3 text-sm">{tema.nombre}</td>

                  <td className="px-4 py-3 text-sm">
                    {areas.find((a) => a.value === tema.areaId?.toString())
                      ?.label ?? tema.areaId}
                  </td>

                  <td className="px-4 py-3 text-sm">
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                        tema.activo
                          ? "bg-green-100 text-green-600"
                          : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {tema.activo ? "Activo" : "Inactivo"}
                    </span>
                  </td>

                  <td className="px-4 py-3 text-sm">
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => abrirEditar(tema)}
                        disabled={!puedeGestionarCatalogos}
                      >
                        Editar
                      </Button>

                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => abrirConfirmacionDesactivar(tema)}
                        disabled={!tema.activo || !puedeGestionarCatalogos}
                      >
                        {tema.activo ? "Desactivar" : "Inactivo"}
                      </Button>
                    </div>
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
        pageSizeOptions={DEFAULT_PAGE_SIZE_OPTIONS}
        totalItems={temasOrdenados.length}
      />

      <ConfirmActionDialog
        open={Boolean(temaADesactivar)}
        title="Desactivar tema"
        description={`¿Deseas desactivar el tema "${
          temaADesactivar?.nombre || "seleccionado"
        }"? Podrás reactivarlo después si es necesario.`}
        confirmText="Desactivar"
        cancelText="Cancelar"
        loading={desactivando}
        onClose={cerrarConfirmacionDesactivar}
        onConfirm={confirmarDesactivarTema}
      />
    </div>
  );
}