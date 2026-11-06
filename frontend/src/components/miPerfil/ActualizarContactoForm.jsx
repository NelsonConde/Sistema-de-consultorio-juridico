"use client";

import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { FormInput } from "@/components/forms/parts/FormInput";
import { Button } from "@/components/ui/button";
import { useApiForm } from "@/hooks/useApiForm";
import { getApiErrorDescription, withErrorReference } from "@/lib/api";
import { API_URL_BASE } from "@/lib/config";
import { requiredEmailRule } from "@/lib/form-validation";

/**
 * Formulario de contacto (correo/teléfono) de Mi Perfil.
 *
 * Solo expone los dos campos editables — nunca nombre, rol, sede o código,
 * que son de solo lectura y viven en MiPerfilView.
 *
 * @param {{ perfil: object, onUpdated: (perfil: object) => void }} props
 */
export function ActualizarContactoForm({ perfil, onUpdated }) {
  const [feedback, setFeedback] = useState(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      email: perfil?.email || "",
      telefono: perfil?.telefono || "",
    },
  });

  // Mantiene los cambios locales al refrescar el perfil. Solo un guardado
  // exitoso reemplaza explícitamente el borrador por los valores confirmados.
  useEffect(() => {
    reset(
      {
        email: perfil?.email || "",
        telefono: perfil?.telefono || "",
      },
      { keepDirtyValues: true },
    );
  }, [perfil, reset]);

  const { submit, isSubmitting } = useApiForm({
    endpoint: `${API_URL_BASE}/mi-perfil/contacto`,
    method: "PATCH",
    successMessage: "Datos de contacto actualizados",
  });

  const submittingRef = useRef(false);

  const onSubmit = async (data) => {
    if (submittingRef.current) return;
    submittingRef.current = true;
    setFeedback(null);

    try {
      const result = await submit({
        email: data.email,
        telefono: data.telefono,
      });

      if (result.success) {
        const updatedProfile =
          result.data && typeof result.data === "object"
            ? result.data
            : { ...perfil, email: data.email, telefono: data.telefono };

        reset({
          email: updatedProfile.email ?? data.email,
          telefono: updatedProfile.telefono ?? data.telefono,
        });
        onUpdated?.(updatedProfile);
        setFeedback({
          type: "success",
          message: "Los datos de contacto se guardaron correctamente.",
        });
      } else {
        setFeedback({
          type: "error",
          message: withErrorReference(
            getApiErrorDescription(
              result.error,
              "No fue posible guardar los datos de contacto.",
            ),
            result.correlationId,
          ),
        });
      }
    } finally {
      submittingRef.current = false;
    }
  };

  return (
    <form
      noValidate
      onSubmit={handleSubmit(onSubmit)}
      className="min-w-0 space-y-5"
      aria-busy={isSubmitting}
    >
      <h2 className="text-base font-semibold">Datos de contacto</h2>

      <FormInput
        name="email"
        label="Correo electrónico"
        type="email"
        register={register}
        errors={errors}
        rules={requiredEmailRule()}
        autoComplete="email"
        disabled={isSubmitting}
        onChange={() => setFeedback(null)}
      />

      <FormInput
        name="telefono"
        label="Teléfono"
        type="tel"
        register={register}
        errors={errors}
        rules={{
          required: "El teléfono es obligatorio",
          pattern: {
            value: /^[0-9+\-\s]{7,30}$/,
            message: "Ingresa un teléfono válido",
          },
        }}
        autoComplete="tel"
        disabled={isSubmitting}
        onChange={() => setFeedback(null)}
      />

      {feedback && (
        <p
          role={feedback.type === "error" ? "alert" : "status"}
          className={`whitespace-pre-wrap break-words rounded-lg border px-3 py-2 text-sm ${
            feedback.type === "error"
              ? "border-destructive/30 bg-destructive/5 text-destructive"
              : "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300"
          }`}
        >
          {feedback.message}
        </p>
      )}

      <Button type="submit" disabled={isSubmitting} className="w-full sm:w-48">
        {isSubmitting ? "Guardando..." : "Guardar contacto"}
      </Button>
    </form>
  );
}
