/**
 * File handling.
 *
 * File handling.
 * List and table handling.
 *
 * @module components/forms/parts/FormFileUpload
 */

import { File as FileIcon, UploadCloud, X } from "lucide-react";
import React from "react";
import { toast } from "sonner";

/** File handling.*/
const MAX_TAMANO_BYTES = 10 * 1024 * 1024;

/**
 * File handling.
 *
 * @param {File} file - Parameter description.
 * @param {number} maxBytes - Implementation detail.
 * @returns {{ valido: boolean, error?: string } Result value.
 */
function validarArchivo(file, maxBytes) {
  if (!String(file.name || "").trim()) {
    return { valido: false, error: "El archivo debe tener un nombre." };
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
 * @property {string} name - Parameter description.
 * @property {string} [label] - Parameter description.
 * @property {boolean} [multiple=false] - Parameter description.
 * @property {function(string, File|File[]): void} setValue - Parameter description.
 * @property {File|File[]|null} value - Parameter description.
 * @property {object} [errors] - Parameter description.
 * @property {number} [maxTamanoByte] - Parameter description.
 */

/**
 * File handling.
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
  maxTamanoByte = MAX_TAMANO_BYTES,
  ...props
}) {
  const selectedFiles = Array.isArray(value) ? value : value ? [value] : [];

  /**
   * File handling.
   *
   * @param {React.ChangeEvent<HTMLInputElement>} event
   */
  function handleFileChange(event) {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) return;

    const validos = [];
    files.forEach((file) => {
      const { valido, error } = validarArchivo(file, maxTamanoByte);
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

    const nuevos = multiple ? [...selectedFiles, ...validos] : [validos[0]];

    if (setValue) {
      setValue(name, multiple ? nuevos : nuevos[0], {
        shouldValidate: true,
        shouldDirty: true,
      });
    }

    event.target.value = "";
  }

  /**
   * List and table handling.
   *
   * @param {string} nombreArchivo - Parameter description.
   */
  function removeFile(nombreArchivo) {
    const restantes = selectedFiles.filter((f) => f.name !== nombreArchivo);
    if (setValue) {
      setValue(name, multiple ? restantes : (restantes[0] ?? null), {
        shouldValidate: true,
        shouldDirty: true,
      });
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
          ${
            errors?.[name]
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
            <span className="font-semibold">Haz clic para seleccionar</span> o
            arrastra y suelta
          </p>
          <p className="text-xs text-center text-muted-foreground/70">
            Cualquier formato · máx. {mb} MB por archivo
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
