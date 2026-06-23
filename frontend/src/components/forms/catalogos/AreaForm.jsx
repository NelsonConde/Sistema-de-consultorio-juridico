/**
 * Formulario de gestión de áreas jurídicas.
 *
 * Permite crear, editar y desactivar áreas. Incluye tabla paginada
 * con las áreas registradas y diálogo de confirmación para desactivar.
 *
 * Requiere permiso `GESTIONAR_CATALOGOS`. Si el usuario no tiene sesión
 * válida redirige a `/`; si no tiene permiso suficiente redirige a `/inicio`.
 *
 * @module components/forms/catalogos/AreaForm
 */
"use client"

import React, { useEffect, useMemo, useState } from "react"
import { useForm } from "react-hook-form"
import { FormInput } from "../parts/FormInput"
import { Button } from "@/components/ui/button"
import { useRouter } from "next/navigation"
import { API_URL_BASE } from "@/lib/config"
import { toast } from "sonner"
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog"
import { PERMISOS } from "@/lib/permission"
import { tienePermiso } from "@/lib/authz"
import { getApiErrorDescription, getApiErrorTitle, readResponseBody } from "@/lib/api"
import Pagination from "@/components/ui/Pagination"
import { DEFAULT_PAGE_SIZE_OPTIONS, getTotalPages, paginateItems, sortByIdAsc } from "@/lib/list-utils"

/**
 * Formulario para gestionar áreas de catálogo.
 * @returns {JSX.Element} Componente de formulario de áreas.
 */
export function AreaForm() {
  const router = useRouter()
  const [checking, setChecking] = useState(true)
  const [areas, setAreas] = useState([])
  const [editandoId, setEditandoId] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [confirmDialog, setConfirmDialog] = useState(null)
  const [confirmLoading, setConfirmLoading] = useState(false)
  const [paginaActual, setPaginaActual] = useState(1)
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: { nombre: "" },
  })

  useEffect(() => {
    async function verificar() {
      try {
        const res = await fetch(`${API_URL_BASE}/auth/me`, {
          credentials: "include",
        })

        if (res.status === 401) {
          router.push("/")
          return
        }

        if (!res.ok) {
          router.push("/")
          return
        }

        const user = await res.json()

        if (!tienePermiso(user, PERMISOS.GESTIONAR_CATALOGOS)) {
          router.push("/inicio")
          return
        }

        await cargarAreas()
      } catch (error) {
        console.error(error)
        router.push("/")
      } finally {
        setChecking(false)
      }
    }

    verificar()
  }, [router])

  /**
   * Carga la lista de áreas desde el backend y actualiza el estado.
   * Redirige a raíz si la sesión expiró (401) o muestra toast si hay error de red.
   */
  async function cargarAreas() {
    try {
      const res = await fetch(`${API_URL_BASE}/areas`, {
        credentials: "include",
      })

      if (res.status === 401) {
        router.push("/")
        return
      }

      if (res.status === 403) {
        toast.error("Sin permiso para ver áreas")
        return
      }

      if (!res.ok) {
        toast.error("Error al cargar áreas")
        return
      }

      const data = await res.json()
      setAreas(sortByIdAsc(Array.isArray(data) ? data : []))
    } catch (error) {
      console.error("Error de red al cargar áreas:", error)
      toast.error("Error de conexión al cargar áreas")
    }
  }

  function abrirEditar(area) {
    setEditandoId(area.id)
    reset({ nombre: area.nombre })
  }

  function cancelarEdicion() {
    setEditandoId(null)
    reset({ nombre: "" })
  }

  function abrirConfirmacionDesactivar(area) {
    setConfirmDialog(area)
  }

  async function confirmarDesactivarArea() {
    if (!confirmDialog?.id) return

    try {
      setConfirmLoading(true)

      const res = await fetch(
        `${API_URL_BASE}/areas/${confirmDialog.id}/activo?activo=false`,
        {
          method: "PATCH",
          credentials: "include",
        }
      )

      if (res.ok || res.status === 204) {
        toast.success("Área desactivada")
        setConfirmDialog(null)
        cargarAreas()
      } else {
        const payload = await readResponseBody(res)
        toast.error(getApiErrorTitle(payload, "Error al desactivar"), {
          description: getApiErrorDescription(payload),
        })
      }
    } catch (error) {
      console.error(error)
      toast.error("Error de conexión")
    } finally {
      setConfirmLoading(false)
    }
  }

  /**
   * Envía el formulario al backend (POST para crear, PUT para editar).
   * Muestra toast de éxito o error según el resultado.
   * Recarga la lista de áreas si la operación fue exitosa.
   *
   * @param {{ nombre: string }} data - Datos del formulario validados por react-hook-form.
   */
  const onSubmit = async (data) => {
    setIsSubmitting(true)

    try {
      const url = editandoId
        ? `${API_URL_BASE}/areas/${editandoId}`
        : `${API_URL_BASE}/areas`

      const method = editandoId ? "PUT" : "POST"

      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(data),
      })

      if (res.status === 401) {
        router.push("/")
        return
      }

      if (res.status === 403) {
        toast.error("No tienes permiso para realizar esta acción")
        return
      }

      const payload = await readResponseBody(res)

      if (res.ok) {
        toast.success(editandoId ? "Área actualizada" : "Área creada")
        setEditandoId(null)
        reset({ nombre: "" })
        cargarAreas()
      } else {
        toast.error(getApiErrorTitle(payload, "Error al guardar"), {
          description: getApiErrorDescription(payload),
        })
      }
    } catch (error) {
      console.error("Error de red al guardar área:", error)
      toast.error("Error de conexión al guardar")
    } finally {
      setIsSubmitting(false)
    }
  }

  const areasOrdenadas = useMemo(() => sortByIdAsc(areas), [areas])
  const totalPaginas = getTotalPages(areasOrdenadas.length, registrosPorPagina)
  const areasPaginadas = useMemo(
    () => paginateItems(areasOrdenadas, paginaActual, registrosPorPagina),
    [areasOrdenadas, paginaActual, registrosPorPagina]
  )

  useEffect(() => {
    setPaginaActual(1)
  }, [registrosPorPagina])

  useEffect(() => {
    if (paginaActual > totalPaginas) {
      setPaginaActual(totalPaginas)
    }
  }, [paginaActual, totalPaginas])

  if (checking) return <div className="text-center mt-10">Cargando...</div>

  return (
    <div className="space-y-6">
      <div className="space-y-6 p-6 bg-card rounded-xl border">
        <div>
          <h2 className="text-2xl font-bold">
            {editandoId ? "Editar Área" : "Registro de Área"}
          </h2>
          <p className="text-muted-foreground">
            Complete la siguiente información
          </p>
        </div>

        <FormInput
          name="nombre"
          label="Nombre del área"
          register={register}
          errors={errors}
          rules={{ required: "El nombre es obligatorio" }}
        />

        <div className="flex gap-3">
          <Button onClick={handleSubmit(onSubmit)} disabled={isSubmitting}>
            {isSubmitting
              ? "Guardando..."
              : editandoId
              ? "Actualizar área"
              : "Guardar área"}
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
              {["ID", "Nombre", "Estado", "Acciones"].map((h) => (
                <th key={h} className="px-4 py-3 text-left text-xs font-medium">
                  {h}
                </th>
              ))}
            </tr>
          </thead>

          <tbody>
            {areasOrdenadas.length === 0 ? (
              <tr>
                <td
                  colSpan={4}
                  className="text-center py-8 text-sm text-muted-foreground"
                >
                  Sin áreas registradas.
                </td>
              </tr>
            ) : (
              areasPaginadas.map((area) => (
                <tr key={area.id} className="border-t hover:bg-muted/50">
                  <td className="px-4 py-3 text-sm">{area.id}</td>
                  <td className="px-4 py-3 text-sm">{area.nombre}</td>
                  <td className="px-4 py-3 text-sm">
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                        area.activo
                          ? "bg-green-100 text-green-600"
                          : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {area.activo ? "Activo" : "Inactivo"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => abrirEditar(area)}
                      >
                        Editar
                      </Button>

                      <Button
                        size="sm"
                        variant="destructive"
                        onClick={() => abrirConfirmacionDesactivar(area)}
                        disabled={!area.activo}
                      >
                        {area.activo ? "Desactivar" : "Inactivo"}
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
          setRegistrosPorPagina(value)
          setPaginaActual(1)
        }}
        pageSizeOptions={DEFAULT_PAGE_SIZE_OPTIONS}
        totalItems={areasOrdenadas.length}
      />

      <ConfirmActionDialog
        open={Boolean(confirmDialog)}
        title="Desactivar área"
        description={`¿Deseas desactivar el área "${
          confirmDialog?.nombre || "seleccionada"
        }"? Podrás reactivarla después si es necesario.`}
        confirmText="Desactivar"
        cancelText="Cancelar"
        loading={confirmLoading}
        onClose={() => setConfirmDialog(null)}
        onConfirm={confirmarDesactivarArea}
      />
    </div>
  )
}