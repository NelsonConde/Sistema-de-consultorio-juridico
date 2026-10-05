"use client";

import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { fileApi } from "@/lib/fileApi";

function initialUploadState(file) {
  return {
    file,
    state: "idle",
    progress: 0,
    loaded: 0,
    total: file?.size ?? null,
    uploadId: null,
    expiresAt: null,
    error: null,
    data: null,
    retryableCompletion: false,
  };
}

/**
 * File handling for a domain resource.
 *
 * Besides listing and downloading files, this hook keeps the UI state for
 * each upload so consumers can display progress, cancel transfers and retry a
 * failed backend confirmation without uploading the file bytes again.
 */
export function useFileResource(resource, { load = true } = {}) {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploadStates, setUploadStates] = useState(() => new Map());

  const controllersRef = useRef(new Map());

  const normalizedResource = useMemo(
      () => ({
        type: resource?.type,
        id: resource?.id,
        parentId: resource?.parentId,
      }),
      [resource?.type, resource?.id, resource?.parentId],
  );

  const updateUploadState = useCallback((file, patch) => {
    if (!file) {
      return;
    }

    setUploadStates((previous) => {
      const next = new Map(previous);
      const current = next.get(file) ?? initialUploadState(file);

      next.set(file, {
        ...current,
        ...patch,
        file,
      });

      return next;
    });
  }, []);

  const clearUploadState = useCallback((file) => {
    if (!file) {
      return;
    }

    setUploadStates((previous) => {
      if (!previous.has(file)) {
        return previous;
      }

      const next = new Map(previous);
      next.delete(file);
      return next;
    });
  }, []);

  const refresh = useCallback(async () => {
    if (!normalizedResource.type || !normalizedResource.id) {
      setFiles([]);
      return [];
    }

    setLoading(true);

    try {
      const result = await fileApi.list(normalizedResource);
      setFiles(result);
      return result;
    } finally {
      setLoading(false);
    }
  }, [normalizedResource]);

  useEffect(() => {
    if (load) {
      refresh().catch(() => setFiles([]));
    }
  }, [load, refresh]);

  useEffect(() => {
    const controllers = controllersRef.current;

    return () => {
      for (const controller of controllers.values()) {
        if (!controller.signal.aborted) {
          controller.abort();
        }
      }

      controllers.clear();
    };
  }, []);

  const upload = useCallback(
      async (selectedFiles, resourceOverride = null) => {
        const filesToUpload = Array.from(selectedFiles || []);

        const targetResource = resourceOverride
            ? {
              type: resourceOverride.type,
              id: resourceOverride.id,
              parentId: resourceOverride.parentId,
            }
            : normalizedResource;

        if (filesToUpload.length === 0) {
          return [];
        }

        const operations = filesToUpload.map(async (file) => {
          let controller = controllersRef.current.get(file);

          /*
           * Reuse the controller if the same File object is submitted again while
           * the first request is still active. fileApi also deduplicates the
           * underlying upload operation.
           */
          if (!controller || controller.signal.aborted) {
            controller = new AbortController();
            controllersRef.current.set(file, controller);
          }

          updateUploadState(file, {
            state: "initiating",
            progress: 0,
            loaded: 0,
            total: file.size,
            uploadId: null,
            expiresAt: null,
            error: null,
            data: null,
            retryableCompletion: false,
          });

          try {
            const data = await fileApi.upload(targetResource, file, {
              signal: controller.signal,
              keepPendingOnConfirmationFailure: true,

              onSession: (session) => {
                updateUploadState(file, {
                  uploadId: session.uploadId,
                  expiresAt: session.expiresAt ?? null,
                });
              },

              onProgress: (progress) => {
                updateUploadState(file, {
                  progress: progress.percent,
                  loaded: progress.loaded,
                  total: progress.total,
                });
              },

              onStateChange: (event) => {
                const patch = {
                  state: event.state,
                };

                if (event.uploadId) {
                  patch.uploadId = event.uploadId;
                }

                if (event.error) {
                  patch.error = event.error;
                } else if (
                    event.state === "initiating" ||
                    event.state === "uploading" ||
                    event.state === "confirming" ||
                    event.state === "completed"
                ) {
                  patch.error = null;
                }

                if (event.data) {
                  patch.data = event.data;
                }

                if (event.state === "completed") {
                  patch.progress = 100;
                  patch.loaded = file.size;
                  patch.total = file.size;
                  patch.retryableCompletion = false;
                }

                if (event.state === "confirmation_failed") {
                  patch.retryableCompletion = true;
                }

                if (
                    event.state === "transfer_failed" ||
                    event.state === "initiation_failed" ||
                    event.state === "cancelled"
                ) {
                  patch.retryableCompletion = false;
                }

                updateUploadState(file, patch);
              },
            });

            return {
              file,
              ok: true,
              data,
              error: null,
            };
          } catch (error) {
            updateUploadState(file, {
              error,
              retryableCompletion:
                  error?.phase === "confirmation" &&
                  error?.retryableCompletion === true,
            });

            return {
              file,
              ok: false,
              data: null,
              error,
            };
          } finally {
            const currentController = controllersRef.current.get(file);

            if (currentController === controller) {
              controllersRef.current.delete(file);
            }
          }
        });

        const results = await Promise.all(operations);

        if (load && results.some((result) => result.ok)) {
          await refresh();
        }

        return results;
      },
      [load, normalizedResource, refresh, updateUploadState],
  );

  const cancelUpload = useCallback(
      async (file) => {
        if (!file) {
          return;
        }

        const controller = controllersRef.current.get(file);

        /*
         * During the byte transfer, AbortController causes fileApi to stop the
         * XMLHttpRequest and compensate the pending backend session.
         */
        if (controller && !controller.signal.aborted) {
          controller.abort();
          return;
        }

        /*
         * After a confirmation failure the bytes are already in storage and the
         * session is intentionally preserved for retry. The user may still
         * choose to discard that pending upload explicitly.
         */
        const current = uploadStates.get(file);

        if (
            current?.state === "confirmation_failed" &&
            current?.uploadId
        ) {
          await fileApi.abortUpload(current.uploadId);

          updateUploadState(file, {
            state: "cancelled",
            error: null,
            retryableCompletion: false,
          });
        }
      },
      [uploadStates, updateUploadState],
  );

  const retryConfirmation = useCallback(
      async (file) => {
        const current = uploadStates.get(file);

        if (!current?.uploadId) {
          throw new Error(
              "No existe una sesión de carga disponible para reintentar",
          );
        }

        if (
            current.state !== "confirmation_failed" ||
            !current.retryableCompletion
        ) {
          throw new Error(
              "La carga no se encuentra pendiente de confirmación",
          );
        }

        updateUploadState(file, {
          state: "confirming",
          error: null,
        });

        try {
          const data = await fileApi.retryComplete(
              normalizedResource,
              current.uploadId,
              {
                onStateChange: (event) => {
                  updateUploadState(file, {
                    state: event.state,
                    error: event.error ?? null,
                    data: event.data ?? current.data,
                    retryableCompletion:
                        event.state === "confirmation_failed",
                    progress:
                        event.state === "completed"
                            ? 100
                            : current.progress,
                  });
                },
              },
          );

          updateUploadState(file, {
            state: "completed",
            progress: 100,
            loaded: file.size,
            total: file.size,
            error: null,
            data,
            retryableCompletion: false,
          });

          if (load) {
            await refresh();
          }

          return data;
        } catch (error) {
          updateUploadState(file, {
            state: "confirmation_failed",
            error,
            retryableCompletion: true,
          });

          throw error;
        }
      },
      [
        load,
        normalizedResource,
        refresh,
        updateUploadState,
        uploadStates,
      ],
  );

  const uploads = useMemo(
      () => Array.from(uploadStates.values()),
      [uploadStates],
  );

  const isUploading = useMemo(
      () =>
          uploads.some((uploadState) =>
              ["initiating", "uploading", "confirming"].includes(
                  uploadState.state,
              ),
          ),
      [uploads],
  );

  const getUploadState = useCallback(
      (file) => uploadStates.get(file) ?? initialUploadState(file),
      [uploadStates],
  );

  return {
    files,
    loading,

    uploads,
    isUploading,
    getUploadState,

    refresh,
    upload,
    cancelUpload,
    retryConfirmation,
    clearUploadState,

    download: fileApi.download,
    remove: fileApi.remove,
  };
}