"use client";

import React, { useRef, useState } from "react";
import { Upload, Download, FileSpreadsheet, CheckCircle, XCircle, AlertCircle, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { API_URL_BASE } from "@/lib/config";

// ─── helpers ──────────────────────────────────────────────────────────────────

function descargarPlantilla() {
  // Genera y descarga la plantilla desde el servidor si existe,
  // o la construye en el cliente con los encabezados correctos.
  // Como el backend no provee el archivo, lo generamos con una librería
  // ya disponible en el navegador (Blob + URL object).

  // Contenido CSV mínimo con los encabezados + fila de ejemplo,
  // compatible con Excel cuando se abre como .csv UTF-8
  const encabezados = [
    "nombre", "tipoDocumentoId", "documento", "email",
    "telefono", "usuario", "sedeId", "codigo", "asesorId", "activo", "conciliacion",
  ];
  const ejemplo = [
    "Juan Pérez", "1", "12345678", "juan.perez@correo.com",
    "3001234567", "juanperez", "1", "20241001", "1", "TRUE", "FALSE",
  ];

  const csv = [encabezados.join(","), ejemplo.join(",")].join("\n");
  const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "plantilla_importacion_estudiantes.csv";
  a.click();
  URL.revokeObjectURL(url);
}

// ─── Componente ───────────────────────────────────────────────────────────────

/**
 * Formulario para importar estudiantes desde un archivo Excel.
 * @param {{puedeImportar:boolean}} props - Props del componente.
 * @returns {JSX.Element} Componente de importación de estudiantes.
 */
export function ImportarEstudiantesForm({ puedeImportar }) {
  const inputRef = useRef(null);

  const [archivoSeleccionado, setArchivoSeleccionado] = useState(null);
  const [importando, setImportando] = useState(false);
  const [resultado, setResultado] = useState(null); // { exitosos, fallidos, totalFilas, errores }
  const [errorFormato, setErrorFormato] = useState("");

  function seleccionarArchivo(file) {
    setResultado(null);
    setErrorFormato("");

    if (!file) return;

    const esXlsx = file.name.endsWith(".xlsx") || file.name.endsWith(".xls");
    if (!esXlsx) {
      setErrorFormato("Solo se aceptan archivos Excel (.xlsx). Descarga la plantilla para ver el formato correcto.");
      setArchivoSeleccionado(null);
      return;
    }

    setArchivoSeleccionado(file);
  }

  function onInputChange(e) {
    seleccionarArchivo(e.target.files?.[0] ?? null);
    // reset input so re-selecting same file still fires onChange
    e.target.value = "";
  }

  function onDrop(e) {
    e.preventDefault();
    seleccionarArchivo(e.dataTransfer.files?.[0] ?? null);
  }

  function limpiar() {
    setArchivoSeleccionado(null);
    setResultado(null);
    setErrorFormato("");
  }

  async function importar() {
    if (!archivoSeleccionado) return;

    setImportando(true);
    setResultado(null);
    setErrorFormato("");

    try {
      const form = new FormData();
      form.append("archivo", archivoSeleccionado);

      const res = await fetch(`${API_URL_BASE}/estudiantes/importar`, {
        method: "POST",
        credentials: "include",
        body: form,
      });

      // Error de formato (texto plano del backend)
      if (res.status === 400) {
        const text = await res.text();
        setErrorFormato(text || "El archivo no tiene el formato correcto.");
        return;
      }

      if (res.status === 403) {
        toast.error("No tienes permiso para importar estudiantes.");
        return;
      }

      if (!res.ok) {
        const text = await res.text();
        try {
          const json = JSON.parse(text);
          setErrorFormato(json.mensaje || "Error interno del servidor.");
        } catch {
          setErrorFormato("Error interno del servidor.");
        }
        return;
      }

      const data = await res.json();
      setResultado(data);

      if (data.fallidos === 0) {
        toast.success(`${data.exitosos} estudiante(s) importado(s) correctamente.`);
      } else if (data.exitosos > 0) {
        toast.warning(`Importación parcial: ${data.exitosos} exitoso(s), ${data.fallidos} fallido(s).`);
      } else {
        toast.error(`Todos los registros fallaron (${data.fallidos} error(es)).`);
      }

      // Limpiar archivo tras importar
      setArchivoSeleccionado(null);
    } catch (err) {
      console.error(err);
      toast.error("Error de conexión al importar.");
    } finally {
      setImportando(false);
    }
  }

  if (!puedeImportar) return null;

  return (
    <div className="space-y-4">

      {/* Encabezado de sección */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <FileSpreadsheet className="w-4 h-4 text-primary" />
          <span className="text-sm font-semibold text-foreground">Importación masiva desde Excel</span>
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="gap-1.5 text-xs h-8"
          onClick={descargarPlantilla}
        >
          <Download className="w-3.5 h-3.5" />
          Descargar plantilla
        </Button>
      </div>

      {/* Zona de drop */}
      {!archivoSeleccionado && !resultado && (
        <div
          onDragOver={(e) => e.preventDefault()}
          onDrop={onDrop}
          onClick={() => inputRef.current?.click()}
          className="border-2 border-dashed border-border rounded-xl px-6 py-8 flex flex-col items-center gap-2 cursor-pointer hover:border-primary hover:bg-primary/5 transition-colors"
        >
          <Upload className="w-8 h-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">
            Arrastra el archivo aquí o haz clic para seleccionar
          </p>
          <p className="text-xs text-muted-foreground">Solo archivos .xlsx</p>
          <input
            ref={inputRef}
            type="file"
            accept=".xlsx,.xls"
            className="hidden"
            onChange={onInputChange}
          />
        </div>
      )}

      {/* Archivo seleccionado */}
      {archivoSeleccionado && !resultado && (
        <div className="flex items-center justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3">
          <div className="flex items-center gap-3 min-w-0">
            <FileSpreadsheet className="w-5 h-5 text-primary shrink-0" />
            <div className="min-w-0">
              <p className="text-sm font-medium text-foreground truncate">{archivoSeleccionado.name}</p>
              <p className="text-xs text-muted-foreground">
                {(archivoSeleccionado.size / 1024).toFixed(1)} KB
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Button
              type="button"
              size="sm"
              className="h-8 gap-1.5 text-xs"
              onClick={importar}
              disabled={importando}
            >
              <Upload className="w-3.5 h-3.5" />
              {importando ? "Importando…" : "Importar"}
            </Button>
            <button
              type="button"
              onClick={limpiar}
              disabled={importando}
              className="p-1.5 rounded-lg hover:bg-muted transition-colors text-muted-foreground"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* Error de formato */}
      {errorFormato && (
        <div className="flex gap-2 rounded-xl border border-primary/30 bg-primary/10 px-4 py-3 text-xs text-primary">
          <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold mb-0.5">Error de formato</p>
            <p>{errorFormato}</p>
          </div>
        </div>
      )}

      {/* Resultado */}
      {resultado && (
        <div className="space-y-3">
          {/* Resumen */}
          <div className="grid grid-cols-3 gap-3">
            <div className="rounded-xl border border-border bg-card px-4 py-3 text-center">
              <p className="text-2xl font-extrabold text-foreground">{resultado.totalFilas}</p>
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground font-semibold mt-0.5">Total filas</p>
            </div>
            <div className="rounded-xl border border-border bg-card px-4 py-3 text-center">
              <p className="text-2xl font-extrabold text-primary">{resultado.exitosos}</p>
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground font-semibold mt-0.5">Exitosos</p>
            </div>
            <div className="rounded-xl border border-border bg-card px-4 py-3 text-center">
              <p className={`text-2xl font-extrabold ${resultado.fallidos > 0 ? "text-chart-1" : "text-muted-foreground"}`}>
                {resultado.fallidos}
              </p>
              <p className="text-[10px] uppercase tracking-wide text-muted-foreground font-semibold mt-0.5">Fallidos</p>
            </div>
          </div>

          {/* Banner de resultado global */}
          {resultado.fallidos === 0 ? (
            <div className="flex items-center gap-2 rounded-xl bg-primary/10 border border-primary/30 px-4 py-2.5 text-sm text-primary">
              <CheckCircle className="w-4 h-4 shrink-0" />
              <span>Todos los estudiantes fueron importados correctamente.</span>
            </div>
          ) : resultado.exitosos === 0 ? (
            <div className="flex items-center gap-2 rounded-xl bg-card border border-border px-4 py-2.5 text-sm text-foreground">
              <XCircle className="w-4 h-4 shrink-0 text-chart-1" />
              <span>Ningún registro pudo ser importado. Revisa los errores abajo.</span>
            </div>
          ) : (
            <div className="flex items-center gap-2 rounded-xl bg-card border border-border px-4 py-2.5 text-sm text-foreground">
              <AlertCircle className="w-4 h-4 shrink-0 text-chart-2" />
              <span>Importación parcial: algunos registros fallaron.</span>
            </div>
          )}

          {/* Lista de errores por fila */}
          {resultado.errores && resultado.errores.length > 0 && (
            <div className="rounded-xl border border-border bg-card overflow-hidden">
              <div className="px-4 py-2.5 border-b border-border bg-muted/30 flex items-center gap-2">
                <XCircle className="w-3.5 h-3.5 text-chart-1" />
                <span className="text-xs font-semibold uppercase tracking-wide text-foreground">
                  Errores ({resultado.errores.length})
                </span>
              </div>
              <div className="max-h-48 overflow-y-auto divide-y divide-border">
                {resultado.errores.map((err, i) => (
                  <div key={i} className="px-4 py-2 text-xs text-foreground flex items-start gap-2">
                    <span className="text-chart-1 font-mono font-bold shrink-0">·</span>
                    <span>{err}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Botón para nueva importación */}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="gap-1.5 text-xs h-8"
            onClick={limpiar}
          >
            <Upload className="w-3.5 h-3.5" />
            Importar otro archivo
          </Button>
        </div>
      )}
    </div>
  );
}
