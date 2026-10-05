import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormFileUpload } from "./FormFileUpload";

const STATE_LABELS = {
    initiating: "Preparando carga...",
    uploading: "Transfiriendo archivo...",
    confirming: "Confirmando carga...",
    confirmation_failed: "No se pudo confirmar la carga",
    transfer_failed: "No se pudo transferir el archivo",
    initiation_failed: "No se pudo iniciar la carga",
    cancelled: "Carga cancelada",
    completed: "Carga completada",
};

function stateLabel(state) {
    return STATE_LABELS[state] ?? "";
}

/**
 * File selection and upload state for consulta documents.
 *
 * @param {Object} props
 * @param {Array<File>} props.archivos - Selected files.
 * @param {function} props.onChange - Updates selected files.
 * @param {function} props.getUploadState - Returns upload state for a file.
 * @param {boolean} props.isUploading - Indicates an active upload operation.
 * @param {function} props.onCancel - Cancels an active upload.
 * @param {function} props.onRetryConfirmation - Retries backend confirmation.
 * @param {function} props.onDiscardPending - Discards a pending confirmation.
 * @returns {JSX.Element}
 */
export default function ArchivosConsultaForm({
                                                 archivos,
                                                 onChange,
                                                 getUploadState,
                                                 isUploading = false,
                                                 onCancel,
                                                 onRetryConfirmation,
                                                 onDiscardPending,
                                             }) {
    const selectedFiles = Array.from(archivos || []);

    return (
        <div className="space-y-4">
            <div>
                <h3 className="text-lg font-medium">
                    Documentos Adicionales
                </h3>

                <p className="text-sm text-muted-foreground">
                    Selecciona los documentos relacionados. Estos se subirán por
                    separado.
                </p>
            </div>

            <FormFileUpload
                name="archivos"
                label="Documentos a subir"
                multiple
                setValue={(_name, value) => onChange(value)}
                value={selectedFiles}
                errors={{}}
                disabled={isUploading}
                canRemoveFile={(file) =>
                    getUploadState?.(file)?.state !== "confirmation_failed"
                }
            />

            {selectedFiles.length > 0 && (
                <section
                    className="space-y-2"
                    aria-live="polite"
                    aria-label="Estado de carga de documentos"
                >
                    {selectedFiles.map((file) => {
                        const uploadState = getUploadState?.(file);

                        if (!uploadState || uploadState.state === "idle") {
                            return null;
                        }

                        const progress = Number.isFinite(
                            uploadState.progress,
                        )
                            ? uploadState.progress
                            : 0;

                        const activeTransfer = [
                            "initiating",
                            "uploading",
                        ].includes(uploadState.state);

                        const pendingConfirmation =
                            uploadState.state === "confirmation_failed" &&
                            uploadState.retryableCompletion &&
                            uploadState.uploadId;

                        const retryableTransfer = [
                            "transfer_failed",
                            "initiation_failed",
                            "cancelled",
                        ].includes(uploadState.state);

                        return (
                            <div
                                key={`${file.name}-${file.size}-${file.lastModified}`}
                                className="rounded-md border p-3"
                            >
                                <div className="flex items-start justify-between gap-3">
                                    <div className="min-w-0 flex-1">
                                        <p className="truncate text-sm font-medium">
                                            {file.name}
                                        </p>

                                        <p className="mt-1 text-xs text-muted-foreground">
                                            {stateLabel(uploadState.state)}
                                        </p>
                                    </div>

                                    {["initiating", "confirming"].includes(
                                        uploadState.state,
                                    ) && (
                                        <Loader2 className="h-4 w-4 shrink-0 animate-spin" />
                                    )}

                                    {uploadState.state === "uploading" && (
                                        <span className="text-xs font-medium">
                      {progress}%
                    </span>
                                    )}
                                </div>

                                {uploadState.state === "uploading" && (
                                    <div className="mt-3">
                                        <div
                                            className="h-2 w-full overflow-hidden rounded-full bg-muted"
                                            role="progressbar"
                                            aria-label={`Progreso de ${file.name}`}
                                            aria-valuemin={0}
                                            aria-valuemax={100}
                                            aria-valuenow={progress}
                                        >
                                            <div
                                                className="h-full bg-primary transition-[width]"
                                                style={{
                                                    width: `${progress}%`,
                                                }}
                                            />
                                        </div>
                                    </div>
                                )}

                                {uploadState.error?.message && (
                                    <p className="mt-2 text-xs text-destructive">
                                        {uploadState.error.message}
                                    </p>
                                )}

                                {retryableTransfer && (
                                    <p className="mt-2 text-xs text-muted-foreground">
                                        Puedes volver a intentar la carga del archivo.
                                    </p>
                                )}

                                {activeTransfer && (
                                    <div className="mt-3">
                                        <Button
                                            type="button"
                                            variant="outline"
                                            size="sm"
                                            onClick={() => onCancel?.(file)}
                                        >
                                            Cancelar
                                        </Button>
                                    </div>
                                )}

                                {pendingConfirmation && (
                                    <div className="mt-3 flex flex-wrap gap-2">
                                        <Button
                                            type="button"
                                            size="sm"
                                            onClick={() =>
                                                onRetryConfirmation?.(file)
                                            }
                                        >
                                            Reintentar confirmación
                                        </Button>

                                        <Button
                                            type="button"
                                            variant="outline"
                                            size="sm"
                                            onClick={() =>
                                                onDiscardPending?.(file)
                                            }
                                        >
                                            Descartar
                                        </Button>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </section>
            )}
        </div>
    );
}