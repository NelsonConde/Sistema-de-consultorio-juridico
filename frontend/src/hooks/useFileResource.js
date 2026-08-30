"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { fileApi } from "@/lib/fileApi";

/** File handling.*/
export function useFileResource(resource, { load = true } = {}) {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const normalizedResource = useMemo(
    () => ({
      type: resource?.type,
      id: resource?.id,
      parentId: resource?.parentId,
    }),
    [resource?.type, resource?.id, resource?.parentId],
  );

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
    if (load) refresh().catch(() => setFiles([]));
  }, [load, refresh]);

  const upload = useCallback(
    async (selectedFiles) => {
      const result = await fileApi.uploadMany(
        normalizedResource,
        selectedFiles,
      );
      await refresh();
      return result;
    },
    [normalizedResource, refresh],
  );

  return {
    files,
    loading,
    refresh,
    upload,
    download: fileApi.download,
    remove: fileApi.remove,
  };
}
