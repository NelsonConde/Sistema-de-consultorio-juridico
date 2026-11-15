"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Download, FileClock, FilePlus2, Trash2, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { ConfirmActionDialog } from "@/components/ui/ConfirmActionDialog";
import { FormFileUpload } from "@/components/forms/parts/FormFileUpload";
import { useFileResource } from "@/hooks/useFileResource";
import { fileApi } from "@/lib/fileApi";

const DOWNLOADABLE_STATUSES = new Set(["VIGENTE", "READY", "ACTIVE"]);

function formatSize(size) {
  if (!Number.isFinite(Number(size))) return "—";
  if (Number(size) < 1024) return `${size} B`;
  return `${(Number(size) / 1024).toFixed(1)} KB`;
}

function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("es-CO");
}

function errorMessage(error, fallback) {
  if (error?.status === 401) return "Tu sesión ya no es válida. Inicia sesión nuevamente.";
  if (error?.status === 403) return "No tienes autorización para realizar esta operación documental.";
  if (error?.status === 404) return "El documento o proceso ya no está disponible.";
  if (error?.status === 409) return error.message || "La operación documental presenta un conflicto.";
  return error?.message || fallback;
}

function sameFile(first, second) {
  return first.name === second.name &&
    first.size === second.size &&
    first.type === second.type;
}

function hasPendingSession(file, uploads) {
  return uploads.some((uploadState) =>
    uploadState.retryableCompletion &&
    uploadState.uploadId &&
    sameFile(file, uploadState.file),
  );
}

function UploadStatus({ uploadState, onRetry, onCancel }) {
  const { file, state, progress, error, retryableCompletion } = uploadState;
  const active = ["initiating", "uploading", "confirming"].includes(state);
  const pendingConfirmation = state === "confirmation_failed" && retryableCompletion;
  const canCancel = ["initiating", "uploading"].includes(state) ||
    (pendingConfirmation && uploadState.uploadId);

  return (
    <div className="rounded-lg border bg-muted/20 p-3 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="font-medium">{file.name}</span>
        <span className="text-xs text-muted-foreground">
          {state === "initiating" && "Iniciando carga…"}
          {state === "uploading" && `Transfiriendo… ${progress ?? 0}%`}
          {state === "confirming" && "Confirmando…"}
          {state === "completed" && "Carga confirmada"}
          {state === "confirmation_failed" && "Confirmación pendiente"}
          {state === "transfer_failed" && "Transferencia fallida"}
          {state === "initiation_failed" && "No se pudo iniciar"}
          {state === "cancelled" && "Carga cancelada"}
        </span>
      </div>

      {active && (
        <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-muted">
          <div
            className="h-full rounded-full bg-primary transition-all"
            style={{ width: `${progress ?? 0}%` }}
          />
        </div>
      )}

      {error && <p className="mt-2 text-xs text-destructive">{error.message}</p>}

      {(active || pendingConfirmation) && (
        <div className="mt-2 flex justify-end gap-2">
          {pendingConfirmation && (
            <Button type="button" size="sm" variant="outline" onClick={onRetry}>
              Reintentar confirmación
            </Button>
          )}
          {canCancel && (
            <Button type="button" size="sm" variant="outline" onClick={onCancel}>
              Cancelar carga
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

export function ProcesoDocumentosDialog({ proceso, puedeGestionar, onClose }) {
  const resource = useMemo(
    () => ({ type: "proceso", id: proceso?.id }),
    [proceso?.id],
  );
  const {
    files,
    loading,
    uploads,
    isUploading,
    refresh,
    upload,
    retryConfirmation,
    cancelUpload,
    download,
    remove,
  } = useFileResource(resource, { load: false });

  const [selectedFiles, setSelectedFiles] = useState([]);
  const [versionTarget, setVersionTarget] = useState(null);
  const [error, setError] = useState("");
  const [listError, setListError] = useState("");
  const [history, setHistory] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [closeBlocked, setCloseBlocked] = useState(false);
  const submittingRef = useRef(false);
  const hasPendingConfirmation = uploads.some(
    (uploadState) => uploadState.retryableCompletion && uploadState.uploadId,
  );

  const refreshDocuments = useCallback(async () => {
    setListError("");
    try {
      await refresh();
      return true;
    } catch (loadError) {
      setListError(errorMessage(loadError, "No se pudieron cargar los documentos del proceso."));
      return false;
    }
  }, [refresh]);

  useEffect(() => {
    setError("");
    setListError("");
    setSelectedFiles([]);
    setVersionTarget(null);
    setHistory(null);

    refreshDocuments();
  }, [refreshDocuments]);

  if (!proceso) return null;

  async function handleUpload() {
    if (!puedeGestionar || selectedFiles.length === 0 || submittingRef.current) return;

    const filesToUpload = selectedFiles.filter((file) => !hasPendingSession(file, uploads));
    const hasSkippedFiles = filesToUpload.length !== selectedFiles.length;
    if (filesToUpload.length === 0) {
      setError("Este archivo tiene una confirmación pendiente. Reintenta la confirmación o cancela la carga antes de volver a cargarlo.");
      return;
    }

    submittingRef.current = true;
    setError(hasSkippedFiles
      ? "Se omitió un archivo con confirmación pendiente. Reintenta la confirmación o cancela esa carga antes de volver a cargarlo."
      : "");
    try {
      const results = await upload(filesToUpload, null, {
        documentoLogico: versionTarget?.documentoLogico || undefined,
        tipoDocumental: versionTarget?.tipoDocumental || "PROCESO_DOCUMENTO",
      });
      const successful = results.filter((result) => result.ok);
      const failed = results.filter((result) => !result.ok);
      setSelectedFiles(failed
        .filter((result) => !result.error?.retryableCompletion)
        .map((result) => result.file));

      if (successful.length > 0) {
        await refreshDocuments();
        setVersionTarget(null);
        setHistory(null);
        toast.success(
          successful.length === 1
            ? "Documento cargado correctamente"
            : `${successful.length} documentos cargados correctamente`,
        );
      }

      if (failed.length > 0) {
        setError(errorMessage(failed[0].error, "No fue posible completar la carga documental."));
      }
    } finally {
      submittingRef.current = false;
    }
  }

  async function handleRetryConfirmation(uploadState) {
    setError("");
    try {
      await retryConfirmation(uploadState.file);
      await refreshDocuments();
      setSelectedFiles((current) => current.filter((file) => file !== uploadState.file));
      setCloseBlocked(false);
      setVersionTarget(null);
      setHistory(null);
      toast.success("Carga confirmada correctamente");
    } catch (retryError) {
      setError(errorMessage(retryError, "No fue posible confirmar la carga."));
    }
  }

  async function handleCancelUpload(file) {
    try {
      await cancelUpload(file);
      setCloseBlocked(false);
    } catch (cancelError) {
      setError(errorMessage(cancelError, "No fue posible cancelar la carga."));
    }
  }

  function handleClose() {
    if (hasPendingConfirmation) {
      setCloseBlocked(true);
      return;
    }

    onClose();
  }

  function handleFileSelection(_name, value) {
    const selected = Array.isArray(value) ? value : value ? [value] : [];
    const eligible = selected.filter((file) => !hasPendingSession(file, uploads));
    const skipped = eligible.length !== selected.length;

    setSelectedFiles(eligible);
    setError(skipped
      ? "Este archivo tiene una confirmación pendiente. Reintenta la confirmación o cancela la carga antes de volver a cargarlo."
      : "");
  }

  async function handleDownload(file) {
    if (downloadingId) return;
    setDownloadingId(file.id);
    setError("");
    try {
      await download(file, resource);
    } catch (downloadError) {
      setError(errorMessage(downloadError, "No fue posible descargar el documento."));
    } finally {
      setDownloadingId(null);
    }
  }

  async function toggleHistory(file) {
    if (history?.documentoLogico === file.documentoLogico) {
      setHistory(null);
      return;
    }

    setHistory({ documentoLogico: file.documentoLogico, loading: true, files: [], error: "" });
    try {
      const versions = await fileApi.listVersions(file.documentoLogico, resource);
      setHistory({ documentoLogico: file.documentoLogico, loading: false, files: versions, error: "" });
    } catch (historyError) {
      setHistory({
        documentoLogico: file.documentoLogico,
        loading: false,
        files: [],
        error: errorMessage(historyError, "No fue posible consultar el historial documental."),
      });
    }
  }

  async function confirmDelete() {
    if (!deleteTarget || deleting) return;
    setDeleting(true);
    setError("");
    try {
      await remove(deleteTarget, resource);
      setHistory(null);
      toast.success("Documento eliminado correctamente");
      setDeleteTarget(null);
      await refreshDocuments();
    } catch (deleteError) {
      setError(errorMessage(deleteError, "No fue posible eliminar el documento."));
    } finally {
      setDeleting(false);
    }
  }

  function startNewVersion(file) {
    setVersionTarget(file);
    setSelectedFiles([]);
    setError("");
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="process-documents-title"
        className="max-h-[92vh] w-full max-w-5xl overflow-y-auto rounded-xl border bg-background p-6 shadow-xl"
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3 id="process-documents-title" className="text-lg font-semibold">
              Documentos del proceso #{proceso.id}
            </h3>
            <p className="text-sm text-muted-foreground">
              Consulta archivos vigentes, versiones y operaciones documentales autorizadas.
            </p>
          </div>
          <Button type="button" size="icon" variant="ghost" onClick={handleClose} disabled={isUploading}>
            <X className="size-4" />
            <span className="sr-only">Cerrar documentos</span>
          </Button>
        </div>

        {puedeGestionar && (
          <section className="mt-5 space-y-3 rounded-xl border p-4" aria-label="Carga documental">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h4 className="font-medium">
                  {versionTarget ? `Nueva versión de ${versionTarget.fileName}` : "Cargar documentos"}
                </h4>
                <p className="text-xs text-muted-foreground">
                  {versionTarget
                    ? `Se conservará el documento lógico y el historial. Versión actual: ${versionTarget.version}.`
                    : "Los archivos se almacenan de forma privada y se confirman antes de aparecer en el listado."}
                </p>
              </div>
              {versionTarget && (
                <Button type="button" size="sm" variant="outline" onClick={() => setVersionTarget(null)}>
                  Cancelar nueva versión
                </Button>
              )}
            </div>

            <FormFileUpload
              name="processDocuments"
              label={versionTarget ? "Archivo de la nueva versión" : "Archivos del proceso"}
              multiple={!versionTarget}
              value={selectedFiles}
              setValue={handleFileSelection}
              disabled={isUploading}
            />

            <div className="flex justify-end">
              <Button
                type="button"
                onClick={handleUpload}
                disabled={isUploading || selectedFiles.length === 0}
              >
                <FilePlus2 className="mr-2 size-4" />
                {isUploading ? "Cargando…" : versionTarget ? "Cargar nueva versión" : "Cargar archivos"}
              </Button>
            </div>

            {uploads.length > 0 && (
              <div className="space-y-2">
                {uploads.map((uploadState) => (
                  <UploadStatus
                    key={`${uploadState.uploadId || "sin-sesion"}-${uploadState.file.name}-${uploadState.file.size}-${uploadState.file.lastModified}`}
                    uploadState={uploadState}
                    onRetry={() => handleRetryConfirmation(uploadState)}
                    onCancel={() => handleCancelUpload(uploadState.file)}
                  />
                ))}
              </div>
            )}
          </section>
        )}

        {closeBlocked && hasPendingConfirmation && (
          <output className="mt-3 block text-sm text-muted-foreground">
            No puedes cerrar mientras haya una carga pendiente de confirmación. Reintenta la confirmación o cancela la carga.
          </output>
        )}

        {error && (
          <div role="alert" className="mt-4 rounded-lg border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <section className="mt-5" aria-label="Documentos vigentes">
          <h4 className="mb-3 font-medium">Documentos vigentes</h4>
          {loading ? (
            <p className="rounded-lg border p-5 text-center text-sm text-muted-foreground">Cargando documentos…</p>
          ) : listError ? (
            <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 p-5 text-sm text-destructive">
              {listError}
            </p>
          ) : files.length === 0 ? (
            <p className="rounded-lg border p-5 text-center text-sm text-muted-foreground">
              No hay documentos vigentes para este proceso.
            </p>
          ) : (
            <div className="space-y-3">
              {files.map((file) => (
                <article key={file.id} className="rounded-lg border p-4">
                  <div className="flex flex-col justify-between gap-3 lg:flex-row lg:items-center">
                    <div className="min-w-0">
                      <p className="truncate font-medium" title={file.fileName}>{file.fileName}</p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {file.tipoDocumental || "PROCESO_DOCUMENTO"} · Versión {file.version ?? 1} · {file.status || "VIGENTE"} · {formatSize(file.size)} · {formatDate(file.createdAt)}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Button type="button" size="sm" variant="outline" onClick={() => handleDownload(file)} disabled={downloadingId === file.id}>
                        <Download className="mr-1 size-4" />
                        {downloadingId === file.id ? "Descargando…" : "Descargar"}
                      </Button>
                      <Button type="button" size="sm" variant="outline" onClick={() => toggleHistory(file)}>
                        <FileClock className="mr-1 size-4" />
                        Historial
                      </Button>
                      {puedeGestionar && (
                        <>
                          <Button type="button" size="sm" variant="outline" onClick={() => startNewVersion(file)}>
                            Nueva versión
                          </Button>
                          <Button type="button" size="sm" variant="destructive" onClick={() => setDeleteTarget(file)}>
                            <Trash2 className="mr-1 size-4" />
                            Eliminar archivo
                          </Button>
                        </>
                      )}
                    </div>
                  </div>

                  {history?.documentoLogico === file.documentoLogico && (
                    <div className="mt-4 border-t pt-3">
                      <h5 className="text-sm font-medium">Historial documental</h5>
                      {history.loading ? (
                        <p className="mt-2 text-sm text-muted-foreground">Consultando versiones…</p>
                      ) : history.error ? (
                        <p role="alert" className="mt-2 text-sm text-destructive">{history.error}</p>
                      ) : (
                        <ul className="mt-2 space-y-2">
                          {history.files.map((version) => (
                            <li key={version.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md bg-muted/30 px-3 py-2 text-sm">
                              <span>
                                Versión {version.version} · {version.fileName} · {version.status} · {formatDate(version.createdAt)}
                              </span>
                              <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                disabled={!DOWNLOADABLE_STATUSES.has(version.status) || downloadingId === version.id}
                                onClick={() => handleDownload(version)}
                              >
                                Descargar
                              </Button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>
      </div>

      <ConfirmActionDialog
        open={Boolean(deleteTarget)}
        title="Eliminar archivo"
        description={deleteTarget
          ? `¿Seguro que deseas eliminar el archivo “${deleteTarget.fileName}”? El historial documental se conservará según las reglas del backend.`
          : "¿Eliminar este archivo?"}
        confirmText="Eliminar archivo"
        loading={deleting}
        onConfirm={confirmDelete}
        onClose={() => !deleting && setDeleteTarget(null)}
      />
    </div>
  );
}
