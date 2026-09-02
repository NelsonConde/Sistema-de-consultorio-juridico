"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import Pagination from "@/components/ui/Pagination";
import { FormInput } from "@/components/forms/parts/FormInput";
import { FormSelect } from "@/components/forms/parts/FormSelect";
import { apiResponse, getApiErrorDescription, getApiErrorTitle } from "@/lib/api";
import {
  DEFAULT_PAGE_SIZE_OPTIONS,
  getTotalPages,
  paginateItems,
  sortByIdAsc,
} from "@/lib/list-utils";
import { requiredSelectRule } from "@/lib/form-validation";

function normalizeName(value) {
  return String(value ?? "")
    .trim()
    .replace(/\s+/g, " ");
}

function comparableName(value) {
  return normalizeName(value).toLocaleLowerCase("es");
}

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

/**
 * State handling.
 * Implementation detail.
 *
 * Implementation detail.
 * Validation rule.
 * Implementation detail.
 */
export function CatalogCrudForm({ config }) {
  const router = useRouter();
  const [records, setRecords] = useState([]);
  const [parentActive, setParentActive] = useState([]);
  const [parentAll, setParentAll] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editId, setEditId] = useState(null);
  const [stateDialog, setStateDialog] = useState(null);
  const [stateLoading, setStateLoading] = useState(false);
  const [paginaActual, setPaginaActual] = useState(1);
  const [registrosPorPagina, setRegistrosPorPagina] = useState(10);

  const defaultValues = useMemo(
    () => ({
      nombre: "",
      ...(config.parent ? { [config.parent.field]: "" } : {}),
    }),
    [config]
  );

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({ defaultValues });

  useEffect(() => {
    reset(defaultValues);
  }, [defaultValues, reset]);

  useEffect(() => {
    let mounted = true;

    async function load() {
      setLoading(true);
      try {
        const requests = [apiResponse(config.listEndpoint)];

        if (config.parent) {
          requests.push(apiResponse(config.parent.activeEndpoint));
          requests.push(apiResponse(config.parent.adminEndpoint));
        }

        const results = await Promise.all(requests);
        const recordsResult = results[0];

        if (recordsResult.response.status === 401) {
          router.replace("/");
          return;
        }

        if (recordsResult.response.status === 403) {
          toast.error("No tienes permiso para gestionar este catálogo");
          router.replace("/inicio");
          return;
        }

        if (!recordsResult.response.ok) {
          toast.error("Error al cargar el catálogo", {
            description: getApiErrorDescription(recordsResult.data),
          });
          return;
        }

        if (!mounted) return;
        setRecords(sortByIdAsc(asArray(recordsResult.data)));

        if (config.parent) {
          const activeResult = results[1];
          const allResult = results[2];

          if (activeResult.response.ok) {
            setParentActive(sortByIdAsc(asArray(activeResult.data)));
          } else {
            setParentActive([]);
          }

          if (allResult.response.ok) {
            setParentAll(sortByIdAsc(asArray(allResult.data)));
          } else {
            // If `/todos` is unavailable by contract, at least the
            // Implementation detail.
            setParentAll(sortByIdAsc(asArray(activeResult.data)));
          }
        }
      } catch (error) {

        toast.error("Error de conexión al cargar el catálogo");
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, [config, router]);

  async function reload() {
    try {
      const { response, data } = await apiResponse(config.listEndpoint);
      if (response.ok) {
        setRecords(sortByIdAsc(asArray(data)));
      }
    } catch {
      // Keep the current catalog state when a background refresh fails.
    }
  }

  function parentLabel(parentId) {
    if (!config.parent) return "";
    return (
      parentAll.find((item) => Number(item.id) === Number(parentId))?.nombre ??
      parentId ??
      "—"
    );
  }

  const parentOptions = useMemo(
    () =>
      parentActive.map((item) => ({
        value: String(item.id),
        label: item.nombre,
      })),
    [parentActive]
  );

  function openEdit(item) {
    setEditId(item.id);
    reset({
      nombre: item.nombre ?? "",
      ...(config.parent
        ? { [config.parent.field]: String(item[config.parent.field] ?? "") }
        : {}),
    });
  }

  function cancelEdit() {
    setEditId(null);
    reset(defaultValues);
  }

  function duplicateExists(nombre, parentId) {
    const comparable = comparableName(nombre);

    return records.some((item) => {
      if (Number(item.id) === Number(editId)) return false;
      if (comparableName(item.nombre) !== comparable) return false;

      if (!config.parent) return true;

      return Number(item[config.parent.field]) === Number(parentId);
    });
  }

  function noChanges(nombre, parentId) {
    if (!editId) return false;
    const current = records.find((item) => Number(item.id) === Number(editId));
    if (!current) return false;

    const sameName = comparableName(current.nombre) === comparableName(nombre);
    const sameParent = !config.parent ||
      Number(current[config.parent.field]) === Number(parentId);

    return sameName && sameParent;
  }

  async function onSubmit(values) {
    const nombre = normalizeName(values.nombre);
    const parentId = config.parent ? Number(values[config.parent.field]) : null;

    if (!nombre) {
      toast.error(`El nombre de ${config.singular.toLowerCase()} es obligatorio`);
      return;
    }

    if (nombre.length > config.maxLength) {
      toast.error(`El nombre no puede superar los ${config.maxLength} caracteres`);
      return;
    }

    if (config.parent && (!parentId || Number.isNaN(parentId))) {
      toast.error(`Debe seleccionar ${config.parent.label.toLowerCase()}`);
      return;
    }

    if (duplicateExists(nombre, parentId)) {
      toast.error(
        config.parent
          ? `Ya existe ${config.singular.toLowerCase()} con ese nombre en ${config.parent.label.toLowerCase()}`
          : `Ya existe ${config.singular.toLowerCase()} con ese nombre`
      );
      return;
    }

    if (noChanges(nombre, parentId)) {
      toast.info("No hay cambios para actualizar");
      return;
    }

    const payload = {
      nombre,
      ...(config.parent ? { [config.parent.field]: parentId } : {}),
    };

    const endpoint = editId ? `${config.endpoint}/${editId}` : config.endpoint;
    const method = editId ? "PUT" : "POST";

    setSaving(true);
    try {
      const { response, data } = await apiResponse(endpoint, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (response.status === 401) {
        router.replace("/");
        return;
      }

      if (response.status === 403) {
        toast.error("No tienes permiso para realizar esta acción");
        return;
      }

      if (!response.ok) {
        toast.error(getApiErrorTitle(data, "Error al guardar"), {
          description: getApiErrorDescription(data),
        });
        return;
      }

      toast.success(editId ? "Registro actualizado" : "Registro creado");
      setEditId(null);
      reset(defaultValues);
      await reload();
    } catch (error) {

      toast.error("Error de conexión al guardar");
    } finally {
      setSaving(false);
    }
  }

  async function confirmStateChange() {
    if (!stateDialog?.id) return;
    const nextState = !Boolean(stateDialog.activo);

    setStateLoading(true);
    try {
      const { response, data } = await apiResponse(
        `${config.endpoint}/${stateDialog.id}/activo?activo=${nextState}`,
        { method: "PATCH" }
      );

      if (response.status === 401) {
        router.replace("/");
        return;
      }

      if (response.status === 403) {
        toast.error("No tienes permiso para realizar esta acción");
        return;
      }

      if (!response.ok) {
        toast.error(
          getApiErrorTitle(
            data,
            `Error al ${nextState ? "activar" : "desactivar"}`
          ),
          { description: getApiErrorDescription(data) }
        );
        return;
      }

      toast.success(nextState ? "Registro activado" : "Registro desactivado");
      setStateDialog(null);
      await reload();
    } catch (error) {

      toast.error("Error de conexión");
    } finally {
      setStateLoading(false);
    }
  }

  const ordered = useMemo(() => sortByIdAsc(records), [records]);
  const totalPaginas = getTotalPages(ordered.length, registrosPorPagina);
  const paged = useMemo(
    () => paginateItems(ordered, paginaActual, registrosPorPagina),
    [ordered, paginaActual, registrosPorPagina]
  );

  useEffect(() => {
    setPaginaActual(1);
  }, [registrosPorPagina, config.id]);

  useEffect(() => {
    if (paginaActual > totalPaginas) setPaginaActual(totalPaginas);
  }, [paginaActual, totalPaginas]);

  if (loading) {
    return <div className="text-center py-10 text-muted-foreground">Cargando...</div>;
  }

  const stateWillActivate = stateDialog ? !Boolean(stateDialog.activo) : false;

  return (
    <div className="space-y-6">
      <div className="space-y-6 p-6 bg-card rounded-xl border">
        <div>
          <h2 className="text-2xl font-bold">
            {editId ? `Editar ${config.singular}` : `Registro de ${config.singular}`}
          </h2>
          <p className="text-muted-foreground">
            Complete la siguiente información
          </p>
        </div>

        <FormInput
          name="nombre"
          label={`Nombre de ${config.singular.toLowerCase()}`}
          register={register}
          errors={errors}
          maxLength={config.maxLength}
          rules={{
            required: "El nombre es obligatorio",
            maxLength: {
              value: config.maxLength,
              message: `El nombre no puede superar los ${config.maxLength} caracteres`,
            },
            validate: (value) =>
              normalizeName(value).length > 0 || "El nombre es obligatorio",
          }}
        />

        {config.parent && (
          <FormSelect
            name={config.parent.field}
            label={config.parent.label}
            options={parentOptions}
            register={register}
            errors={errors}
            rules={requiredSelectRule(
              `Debe seleccionar ${config.parent.label.toLowerCase()}`
            )}
          />
        )}

        <div className="flex gap-3">
          <Button onClick={handleSubmit(onSubmit)} disabled={saving}>
            {saving
              ? "Guardando..."
              : editId
              ? `Actualizar ${config.singular.toLowerCase()}`
              : `Guardar ${config.singular.toLowerCase()}`}
          </Button>

          {editId && (
            <Button variant="outline" type="button" onClick={cancelEdit}>
              Cancelar
            </Button>
          )}
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border bg-card">
        <table className="min-w-full">
          <thead className="bg-muted">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium">ID</th>
              <th className="px-4 py-3 text-left text-xs font-medium">Nombre</th>
              {config.parent && (
                <th className="px-4 py-3 text-left text-xs font-medium">
                  {config.parent.label}
                </th>
              )}
              <th className="px-4 py-3 text-left text-xs font-medium">Estado</th>
              <th className="px-4 py-3 text-left text-xs font-medium">Acciones</th>
            </tr>
          </thead>

          <tbody>
            {ordered.length === 0 ? (
              <tr>
                <td
                  colSpan={config.parent ? 5 : 4}
                  className="text-center py-8 text-sm text-muted-foreground"
                >
                  Sin registros de {config.plural}.
                </td>
              </tr>
            ) : (
              paged.map((item) => (
                <tr key={item.id} className="border-t hover:bg-muted/50">
                  <td className="px-4 py-3 text-sm">{item.id}</td>
                  <td className="px-4 py-3 text-sm">{item.nombre}</td>
                  {config.parent && (
                    <td className="px-4 py-3 text-sm">
                      {parentLabel(item[config.parent.field])}
                    </td>
                  )}
                  <td className="px-4 py-3 text-sm">
                    <span
                      className={`px-2 py-1 rounded-full text-xs font-medium ${
                        item.activo
                          ? "bg-green-100 text-green-600"
                          : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {item.activo ? "Activo" : "Inactivo"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => openEdit(item)}
                      >
                        Editar
                      </Button>

                      <Button
                        size="sm"
                        variant={item.activo ? "destructive" : "outline"}
                        onClick={() => setStateDialog(item)}
                      >
                        {item.activo ? "Desactivar" : "Activar"}
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
        totalItems={ordered.length}
      />

      <ConfirmActionDialog
        open={Boolean(stateDialog)}
        title={`${stateWillActivate ? "Activar" : "Desactivar"} ${config.singular.toLowerCase()}`}
        description={`¿Deseas ${stateWillActivate ? "activar" : "desactivar"} ${config.singular.toLowerCase()} \"${stateDialog?.nombre || "seleccionado"}\"?`}
        confirmText={stateWillActivate ? "Activar" : "Desactivar"}
        cancelText="Cancelar"
        loading={stateLoading}
        onClose={() => !stateLoading && setStateDialog(null)}
        onConfirm={confirmStateChange}
      />
    </div>
  );
}
