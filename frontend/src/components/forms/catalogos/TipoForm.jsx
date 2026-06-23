"use client"

/**
 * Formulario de gestión de tipos de consulta jurídica.
 *
 * Permite crear, editar y desactivar tipos agrupados por tema.
 * Requiere permiso `GESTIONAR_CATALOGOS`.
 *
 * @module components/forms/catalogos/TipoForm
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
import { tienePermiso } from "@/lib/authz";
import { getApiErrorDescription, getApiErrorTitle, readResponseBody } from "@/lib/api";
import { requiredSelectRule } from "@/lib/form-validation";
import Pagination from "@/components/ui/Pagination";
import { DEFAULT_PAGE_SIZE_OPTIONS, getTotalPages, paginateItems, sortByIdAsc } from "@/lib/list-utils";

/**
 * Formulario para gestionar tipos en el catálogo.
 * @returns {JSX.Element} Componente de formulario de tipos.
 */
export function TipoForm() {
  const router = useRouter();
  const [checking, setChecking] = useState(true);
  const [temas, setTemas] = useState([]);
  const [tipos, setTipos] = useState([]);
  const [editandoId, setEditandoId] = useState(null);

  const [tipoADesactivar, setTipoADesactivar] = useState(null);
  const [desactivando, setDesactivando] = useState(false);
  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: { nombre: "", temaId: "" },
  });

  const { submit, isSubmitting } = useApiForm({
    endpoint: `${API_URL_BASE}/tipos`,
  });

  useEffect(() => {
    const verificarYCargar = async () => {
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

        if (!tienePermiso(user, PERMISOS.GESTIONAR_CATALOGOS)) {
          router.push("/inicio");
          return;
        }

        const response = await fetch(`${API_URL_BASE}/temas`, {
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
          const temasData = await response.json();

          setTemas(
            Array.isArray(temasData)
              ? sortByIdAsc(temasData).map((tema) => ({
                  value: tema.id,
                  label: tema.nombre,
                }))
              : []
          );
        }

        await cargarTipos();
      } catch (error) {
        console.error("Error:", error);
        router.push("/");
      } finally {
        setChecking(false);
      }
    };

    verificarYCargar();
  }, [router]);

  async function cargarTipos() {
    const res = await fetch(`${API_URL_BASE}/tipos`, {
      credentials: "include",
    });

    const data = await res.json();
    setTipos(sortByIdAsc(Array.isArray(data) ? data : []));
  }

  function abrirEditar(tipo) {
    setEditandoId(tipo.id);
    reset({
      nombre: tipo.nombre,
      temaId: tipo.temaId?.toString(),
    });
  }

  function cancelarEdicion() {
    setEditandoId(null);
    reset({ nombre: "", temaId: "" });
  }

  function abrirConfirmacionDesactivar(tipo) {
    setTipoADesactivar(tipo);
  }

  function cerrarConfirmacionDesactivar() {
    if (desactivando) return;
    setTipoADesactivar(null);
  }

  async function confirmarDesactivarTipo() {
    if (!tipoADesactivar?.id) return;

    try {
      setDesactivando(true);

      const res = await fetch(
        `${API_URL_BASE}/tipos/${tipoADesactivar.id}/activo?activo=false`,
        {
          method: "PATCH",
          credentials: "include",
        }
      );

      if (res.ok || res.status === 204) {
        toast.success("Tipo desactivado");
        setTipoADesactivar(null);
        cargarTipos();
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
    if (editandoId) {
      const res = await fetch(`${API_URL_BASE}/tipos/${editandoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ ...data, temaId: Number(data.temaId) }),
      });

      const payload = await readResponseBody(res);

      if (res.ok) {
        toast.success("Tipo actualizado");
        setEditandoId(null);
        reset({ nombre: "", temaId: "" });
        cargarTipos();
      } else {
        toast.error(getApiErrorTitle(payload, "Error al actualizar"), {
          description: getApiErrorDescription(payload),
        });
      }
    } else {
      const result = await submit({ ...data, temaId: Number(data.temaId) });

      if (result?.success) {
        reset({ nombre: "", temaId: "" });
        cargarTipos();
      }
    }
  };

  const tiposOrdenados = useMemo(() => sortByIdAsc(tipos), [tipos]);
  const totalPaginas = getTotalPages(tiposOrdenados.length, registrosPorPagina);
  const tiposPaginados = useMemo(
    () => paginateItems(tiposOrdenados, paginaActual, registrosPorPagina),
    [tiposOrdenados, paginaActual, registrosPorPagina]
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
      <div className="space-y-6 p-6 bg-card rounded-xl border">
        <div>
          <h2 className="text-2xl font-bold">
            {editandoId ? "Editar Tipo" : "Registro de Tipo"}
          </h2>
          <p className="text-muted-foreground">
            Complete la siguiente información
          </p>
        </div>

        <FormInput
          name="nombre"
          label="Nombre del tipo"
          register={register}
          errors={errors}
          rules={{ required: "El nombre es obligatorio" }}
        />

        <FormSelect
          name="temaId"
          label="Tema"
          options={temas}
          register={register}
          errors={errors}
          rules={requiredSelectRule("Debe seleccionar un tema")}
        />

        <div className="flex gap-3">
          <Button onClick={handleSubmit(onSubmit)} disabled={isSubmitting}>
            {isSubmitting
              ? "Guardando..."
              : editandoId
              ? "Actualizar tipo"
              : "Guardar tipo"}
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
              {["ID", "Nombre", "Tema", "Estado", "Acciones"].map((h) => (
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
            {tiposOrdenados.length === 0 ? (
              <tr>
                <td
                  colSpan={5}
                  className="text-center py-8 text-sm text-muted-foreground"
                >
                  Sin tipos registrados.
                </td>
              </tr>
            ) : (
              tiposPaginados.map((tipo) => (
                <tr key={tipo.id} className="border-t hover:bg-muted/50">
                  <td className="px-4 py-3 text-sm">{tipo.id}</td>

                  <td className="px-4 py-3 text-sm">{tipo.nombre}</td>

                  <td className="px-4 py-3 text-sm">
                    {temas.find(
                      (t) => Number(t.value) === Number(tipo.temaId)
                    )?.label ?? tipo.temaId}
                  </td>

                  <td className="px-4 py-3 text-sm">
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                        tipo.activo
                          ? "bg-green-100 text-green-600"
                          : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {tipo.activo ? "Activo" : "Inactivo"}
                    </span>
                  </td>

                  <td className="px-4 py-3 text-sm">
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => abrirEditar(tipo)}
                      >
                        Editar
                      </Button>

                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => abrirConfirmacionDesactivar(tipo)}
                        disabled={!tipo.activo}
                      >
                        {tipo.activo ? "Desactivar" : "Inactivo"}
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
        totalItems={tiposOrdenados.length}
      />

      <ConfirmActionDialog
        open={Boolean(tipoADesactivar)}
        title="Desactivar tipo"
        description={`¿Deseas desactivar el tipo "${
          tipoADesactivar?.nombre || "seleccionado"
        }"? Podrás reactivarlo después si es necesario.`}
        confirmText="Desactivar"
        cancelText="Cancelar"
        loading={desactivando}
        onClose={cerrarConfirmacionDesactivar}
        onConfirm={confirmarDesactivarTipo}
      />
    </div>
  );
}