"use client";

import {
  ArrowUpDown,
  ChevronDown,
  ChevronUp,
  Info,
  Search,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import Pagination from "@/components/ui/Pagination";
import { apiResponse, getApiErrorDescription } from "@/lib/api";
import { API_URL_BASE } from "@/lib/config";

const OUTCOME_LABELS = {
  SUCCESS: "Éxito",
  FAILURE: "Fallo",
  DENIED: "Denegado",
};

const OUTCOME_STYLES = {
  SUCCESS: "border-emerald-300 bg-emerald-50 text-emerald-700",
  FAILURE: "border-red-300 bg-red-50 text-red-700",
  DENIED: "border-amber-300 bg-amber-50 text-amber-700",
};

function DetailMap({ title, values }) {
  const entries = Object.entries(values || {}).filter(
    ([, value]) => value !== null && value !== undefined && value !== "",
  );
  if (entries.length === 0) return null;

  return (
    <section className="space-y-1">
      <h4 className="font-medium">{title}</h4>
      <dl className="grid grid-cols-[minmax(8rem,auto)_1fr] gap-x-3 gap-y-1 rounded-md border p-3">
        {entries.map(([key, value]) => (
          <div key={key} className="contents">
            <dt className="text-muted-foreground">{key}</dt>
            <dd className="wrap-break-word">{String(value)}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

export function AuditLogsTable() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [search, setSearch] = useState("");
  const [searchInputValue, setSearchInputValue] = useState("");
  const [sortBy, setSortBy] = useState("occurredAt");
  const [sortDir, setSortDir] = useState("desc");

  const fetchLogs = useCallback(async () => {
    try {
      setLoading(true);
      const url = new URL(`${API_URL_BASE}/audit`);
      url.searchParams.set("page", String(page - 1));
      url.searchParams.set("size", String(size));
      url.searchParams.set("sortBy", sortBy);
      url.searchParams.set("sortDir", sortDir);
      if (search.trim()) url.searchParams.set("username", search.trim());

      const { response, data } = await apiResponse(url.toString(), {
        method: "GET",
      });
      if (!response.ok) {
        throw new Error(
          getApiErrorDescription(data, "Error cargando la auditoría"),
        );
      }
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalItems(data.totalElements || 0);
    } catch (error) {
      toast.error(error.message);
      setLogs([]);
    } finally {
      setLoading(false);
    }
  }, [page, size, search, sortBy, sortDir]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleSort = (column) => {
    setSortDir(sortBy === column && sortDir === "desc" ? "asc" : "desc");
    setSortBy(column);
    setPage(1);
  };

  const sortIcon = (column) => {
    if (sortBy !== column)
      return <ArrowUpDown className="h-4 w-4 opacity-50" />;
    return sortDir === "asc" ? (
      <ChevronUp className="h-4 w-4" />
    ) : (
      <ChevronDown className="h-4 w-4" />
    );
  };

  const formatDate = (value) =>
    value
      ? new Intl.DateTimeFormat("es-CO", {
          dateStyle: "short",
          timeStyle: "medium",
          timeZone: "America/Bogota",
        }).format(new Date(value))
      : "N/A";

  return (
    <div className="space-y-4">
      <form
        onSubmit={(event) => {
          event.preventDefault();
          setSearch(searchInputValue);
          setPage(1);
        }}
        className="flex w-full max-w-md items-center gap-2"
      >
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Buscar por usuario actor..."
            className="pl-9"
            value={searchInputValue}
            onChange={(event) => setSearchInputValue(event.target.value)}
          />
        </div>
        <Button type="submit" variant="secondary" disabled={loading}>
          Buscar
        </Button>
        {search && (
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              setSearchInputValue("");
              setSearch("");
              setPage(1);
            }}
          >
            Limpiar
          </Button>
        )}
      </form>

      <div className="overflow-hidden rounded-xl border bg-card shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-muted/50 text-muted-foreground">
              <tr>
                {[
                  ["occurredAt", "Instante"],
                  ["actorUsername", "Actor"],
                ].map(([field, label]) => (
                  <th key={field} className="px-4 py-3 font-medium">
                    <button
                      type="button"
                      className="flex items-center gap-1"
                      onClick={() => handleSort(field)}
                    >
                      {label}
                      {sortIcon(field)}
                    </button>
                  </th>
                ))}
                <th className="px-4 py-3 font-medium">Acción</th>
                <th className="px-4 py-3 font-medium">Resultado</th>
                <th className="px-4 py-3 font-medium">Entidad</th>
                <th className="px-4 py-3 font-medium">ID</th>
                <th className="px-4 py-3 font-medium">Evidencia</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {loading || logs.length === 0 ? (
                <tr>
                  <td
                    colSpan="7"
                    className="h-32 text-center text-muted-foreground"
                  >
                    {loading
                      ? "Cargando registros..."
                      : "No se encontraron registros de auditoría."}
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-muted/30">
                    <td className="px-4 py-3 whitespace-nowrap">
                      {formatDate(log.occurredAt)}
                    </td>
                    <td className="px-4 py-3 font-medium">
                      {log.actorUsername}
                    </td>
                    <td className="px-4 py-3">{log.action}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${OUTCOME_STYLES[log.outcome] || ""}`}
                      >
                        {OUTCOME_LABELS[log.outcome] || log.outcome}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {log.entityName}
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">
                      {log.entityId || "—"}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <Dialog>
                        <DialogTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            aria-label={`Ver evidencia ${log.correlationId}`}
                          >
                            <Info className="h-4 w-4" />
                          </Button>
                        </DialogTrigger>
                        <DialogContent className="sm:max-w-2xl">
                          <DialogHeader>
                            <DialogTitle>Evidencia de auditoría</DialogTitle>
                            <DialogDescription>
                              {log.action} · {formatDate(log.occurredAt)}
                            </DialogDescription>
                          </DialogHeader>
                          <div className="max-h-[65vh] space-y-4 overflow-auto text-sm">
                            <DetailMap
                              title="Contexto"
                              values={{
                                correlacion: log.correlationId,
                                origen: log.source,
                                ip: log.ipAddress,
                                agente: log.userAgent,
                                codigoMotivo: log.reasonCode,
                                motivo: log.reason,
                              }}
                            />
                            <DetailMap
                              title="Valores anteriores"
                              values={log.beforeState}
                            />
                            <DetailMap
                              title="Valores nuevos"
                              values={log.afterState}
                            />
                            <DetailMap
                              title="Metadatos permitidos"
                              values={log.metadata}
                            />
                          </div>
                        </DialogContent>
                      </Dialog>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <Pagination
        currentPage={page}
        totalPages={totalPages}
        totalItems={totalItems}
        pageSize={size}
        onPageChange={setPage}
        onPageSizeChange={(newSize) => {
          setSize(Math.min(newSize, 100));
          setPage(1);
        }}
        pageSizeOptions={[10, 20, 50, 100]}
      />
    </div>
  );
}
