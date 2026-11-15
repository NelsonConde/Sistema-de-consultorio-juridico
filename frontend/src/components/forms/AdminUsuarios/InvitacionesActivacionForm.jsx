"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import Pagination from "@/components/ui/Pagination";
import { useDebouncedPageSearch } from "@/hooks/useDebouncedValue";
import { getApiErrorDescription, readResponseBody } from "@/lib/api";
import { apiClient } from "@/lib/apiClient";
import { fetchPaged, isAbortError } from "@/lib/pagedApi";

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

export function InvitacionesActivacionForm({ reloadKey = 0 }) {
  const router = useRouter();
  const inFlightRef = useRef(new Set());
  const [usuarios, setUsuarios] = useState([]);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [sendingId, setSendingId] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const appliedSearch = useDebouncedPageSearch(search, setPage, 300).trim();
  const reloadSignal = `${refreshKey}:${reloadKey}`;

  useEffect(() => {
    void reloadSignal;
    const controller = new AbortController();

    async function loadPendingAccounts() {
      setLoading(true);
      setError("");

      try {
        const result = await fetchPaged("/usuarios-sistema", {
          search: appliedSearch,
          page,
          size: pageSize,
          sortBy: "username",
          direction: "asc",
          filters: {
            activo: true,
            estadoActivacion: "PENDIENTE",
          },
          signal: controller.signal,
          resourceName: "cuentas pendientes de activación",
        });

        setUsuarios(result.content);
        setTotalElements(result.totalElements);
        setTotalPages(result.totalPages);

        if (result.totalPages > 0 && result.page > result.totalPages) {
          setPage(result.totalPages);
        }
      } catch (requestError) {
        if (isAbortError(requestError)) return;
        if (requestError?.status === 401) {
          router.replace("/");
          return;
        }

        setUsuarios([]);
        setTotalElements(0);
        setTotalPages(0);
        setError(
          requestError?.status === 403
            ? "No tienes permiso para consultar cuentas pendientes."
            : requestError?.message ||
                "No fue posible cargar las cuentas pendientes.",
        );
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    loadPendingAccounts();
    return () => controller.abort();
  }, [appliedSearch, page, pageSize, reloadSignal, router]);

  async function resendInvitation(usuario) {
    if (!usuario?.id || inFlightRef.current.has(usuario.id)) return;

    inFlightRef.current.add(usuario.id);
    setSendingId(usuario.id);
    setError("");

    try {
      const response = await apiClient.post(
        `/usuarios-sistema/${usuario.id}/invitacion-activacion/reenviar`,
      );
      const body = await readResponseBody(response);

      if (response.status === 401) {
        router.replace("/");
        return;
      }

      if (response.status === 403) {
        setError("No tienes permiso para reenviar invitaciones.");
        return;
      }

      if (!response.ok) {
        setError(
          getApiErrorDescription(
            body,
            "No fue posible procesar el reenvío de la invitación.",
          ),
        );
        return;
      }

      toast.success("Solicitud de reenvío procesada", {
        description:
          "Si la cuenta continúa pendiente, el sistema intentará despachar una invitación. La entrega depende del proveedor de correo.",
      });
      setRefreshKey((current) => current + 1);
    } catch {
      setError("No fue posible conectar con el servicio de invitaciones.");
    } finally {
      inFlightRef.current.delete(usuario.id);
      setSendingId(null);
    }
  }

  return (
    <section className="mt-6 space-y-4 rounded-xl border bg-card p-6 shadow">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-xl font-bold">
            Cuentas pendientes de activación
          </h2>
          <p className="text-sm text-muted-foreground">
            Consulta cuentas sin contraseña y solicita un nuevo intento de
            invitación.
          </p>
        </div>
        <input
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Buscar por correo o rol"
          aria-label="Buscar cuentas pendientes"
          className="h-10 w-full rounded-md border bg-background px-3 text-sm sm:max-w-xs"
        />
      </div>

      {error && (
        <div
          role="alert"
          className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive"
        >
          <p>{error}</p>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="mt-2"
            onClick={() => setRefreshKey((current) => current + 1)}
          >
            Reintentar carga
          </Button>
        </div>
      )}

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead className="bg-muted/60">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Cuenta</th>
              <th className="px-4 py-3 text-left font-medium">Perfil</th>
              <th className="px-4 py-3 text-left font-medium">Estado</th>
              <th className="px-4 py-3 text-right font-medium">Acción</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  Cargando cuentas pendientes...
                </td>
              </tr>
            ) : usuarios.length === 0 ? (
              <tr>
                <td
                  colSpan={4}
                  className="px-4 py-8 text-center text-muted-foreground"
                >
                  No hay cuentas pendientes de activación.
                </td>
              </tr>
            ) : (
              usuarios.map((usuario) => (
                <tr key={usuario.id} className="border-t">
                  <td className="px-4 py-3 font-medium">{usuario.username}</td>
                  <td className="px-4 py-3">
                    {usuario.rolNombre || usuario.tipoPerfil || "Sin perfil"}
                  </td>
                  <td className="px-4 py-3">
                    <span className="rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-700">
                      Pendiente
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Button
                      type="button"
                      size="sm"
                      onClick={() => resendInvitation(usuario)}
                      disabled={sendingId === usuario.id}
                    >
                      {sendingId === usuario.id
                        ? "Procesando..."
                        : "Reenviar invitación"}
                    </Button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <Pagination
        currentPage={page}
        totalPages={totalPages}
        onPageChange={setPage}
        pageSize={pageSize}
        onPageSizeChange={(value) => {
          setPageSize(value);
          setPage(1);
        }}
        pageSizeOptions={PAGE_SIZE_OPTIONS}
        totalItems={totalElements}
      />
    </section>
  );
}

export default InvitacionesActivacionForm;
