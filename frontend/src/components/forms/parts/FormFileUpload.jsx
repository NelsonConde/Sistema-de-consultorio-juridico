/**
 * Componente de carga de archivos con drag & drop y validación.
 *
 * Valida tipo MIME y tamaño máximo antes de agregar cada archivo.
 * Muestra la lista de archivos seleccionados con opción de eliminar individualmente.
 *
 * @module components/forms/parts/FormFileUpload
 */

import React from "react";
import { UploadCloud, X, File as FileIcon } from "lucide-react";
import { toast } from "sonner";

/** Tipos MIME permitidos por defecto. */
const TIPOS_PERMITIDOS = [
  "application/pdf",
  "image/jpeg",
  "image/png",
  "image/webp",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.ms-excel",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
];

/** Tamaño máximo por archivo en bytes (10 MB). */
const MAX_TAMANO_BYTES = 10 * 1024 * 1024;

/**
 * Valida un archivo según tipo MIME y tamaño máximo.
 *
 * @param {File} file - Archivo a validar.
 * @param {string[]} tiposPermitidos - Array de tipos MIME aceptados.
 * @param {number} maxBytes - Tamaño máximo en bytes.
 * @returns {{ valido: boolean, error?: string }} Resultado de la validación.
 */
function validarArchivo(file, tiposPermitidos, maxBytes) {
  if (!tiposPermitidos.includes(file.type)) {
    return {
      valido: false,
      error: `"${file.name}" tiene un formato no permitido. Use PDF, imágenes o documentos Office.`,
    };
  }

  if (file.size > maxBytes) {
    const mb = (maxBytes / 1024 / 1024).toFixed(0);
    return {
      valido: false,
      error: `"${file.name}" supera el tamaño máximo de ${mb} MB.`,
    };
  }

  return { valido: true };
}

/**
 * @typedef {Object} FormFileUploadProps
 * @property {string} name - Nombre del campo, usado como `id` del input.
 * @property {string} [label] - Etiqueta visible sobre el área de carga.
 * @property {boolean} [multiple=false] - Si se permiten múltiples archivos.
 * @property {function(string, File|File[]): void} setValue - Función para actualizar el valor en el form.
 * @property {File|File[]|null} value - Valor actual del campo.
 * @property {object} [errors] - Objeto de errores de react-hook-form.
 * @property {string[]} [tiposPermitidos] - Tipos MIME aceptados. Por defecto: PDF, imágenes, Office.
 * @property {number} [maxTamanoByte] - Tamaño máximo en bytes. Por defecto: 10 MB.
 */

/**
 * Campo de carga de archivos con validación de tipo y tamaño.
 *
 * @param {FormFileUploadProps} props
 * @returns {JSX.Element}
 */
export function FormFileUpload({
  name,
  label,
  multiple = false,
  setValue,
  value,
  errors,
  tiposPermitidos = TIPOS_PERMITIDOS,
  maxTamanoByte = MAX_TAMANO_BYTES,
  ...props
}) {
  const selectedFiles = Array.isArray(value) ? value : value ? [value] : [];

  /**
   * Maneja la selección de archivos, validando cada uno antes de agregarlo.
   *
   * @param {React.ChangeEvent<HTMLInputElement>} event
   */
  function handleFileChange(event) {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) return;

    const validos = [];
    files.forEach((file) => {
      const { valido, error } = validarArchivo(file, tiposPermitidos, maxTamanoByte);
      if (valido) {
        validos.push(file);
      } else {
        toast.error("Archivo no permitido", { description: error });
      }
    });

    if (validos.length === 0) {
      event.target.value = "";
      return;
    }

    const nuevos = multiple
      ? [...selectedFiles, ...validos]
      : [validos[0]];

    if (setValue) {
      setValue(name, multiple ? nuevos : nuevos[0], {
        shouldValidate: true,
        shouldDirty: true,
      });
    }

    event.target.value = "";
  }

  /**
   * Elimina un archivo de la lista por nombre.
   *
   * @param {string} nombreArchivo - Nombre del archivo a eliminar.
   */
  function removeFile(nombreArchivo) {
    const restantes = selectedFiles.filter((f) => f.name !== nombreArchivo);
    if (setValue) {
      setValue(
        name,
        multiple ? restantes : restantes[0] ?? null,
        { shouldValidate: true, shouldDirty: true }
      );
    }
  }

  const mb = (maxTamanoByte / 1024 / 1024).toFixed(0);

  return (
    <div className="flex flex-col gap-1.5 w-full">
      {label && (
        <label htmlFor={name} className="text-sm font-medium leading-none">
          {label}
        </label>
      )}

      <div
        className={`
          relative flex flex-col items-center justify-center w-full h-32
          border-2 border-dashed rounded-lg cursor-pointer
          transition-colors hover:bg-muted/50
          ${errors?.[name]
            ? "border-destructive bg-destructive/5"
            : "border-muted-foreground/25 bg-background"
          }
        `}
      >
        <input
          id={name}
          type="file"
          multiple={multiple}
          onChange={handleFileChange}
          className="absolute inset-0 w-full h-full opacity-0 cursor-pointer disabled:cursor-not-allowed"
          {...props}
        />

        <div className="flex flex-col items-center justify-center pt-5 pb-6 text-muted-foreground pointer-events-none">
          <UploadCloud className="w-8 h-8 mb-2" />
          <p className="mb-1 text-sm text-center">
            <span className="font-semibold">Haz clic para seleccionar</span> o arrastra y suelta
          </p>
          <p className="text-xs text-center text-muted-foreground/70">
            PDF, imágenes o documentos Office · máx. {mb} MB por archivo
          </p>
        </div>
      </div>

      {errors?.[name] && (
        <p className="text-xs text-destructive">{errors[name]?.message}</p>
      )}

      {selectedFiles.length > 0 && (
        <div className="flex flex-col gap-2 mt-2">
          {selectedFiles.map((file) => (
            <div
              key={file.name}
              className="flex items-center justify-between p-2 text-sm border rounded-md bg-muted/30"
            >
              <div className="flex items-center gap-2 truncate">
                <FileIcon className="w-4 h-4 shrink-0 text-muted-foreground" />
                <span className="truncate">{file.name}</span>
                <span className="text-xs text-muted-foreground shrink-0">
                  ({(file.size / 1024).toFixed(0)} KB)
                </span>
              </div>
              <button
                type="button"
                onClick={() => removeFile(file.name)}
                className="p-1 transition-colors rounded hover:bg-destructive/10 hover:text-destructive"
                title={`Quitar ${file.name}`}
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
