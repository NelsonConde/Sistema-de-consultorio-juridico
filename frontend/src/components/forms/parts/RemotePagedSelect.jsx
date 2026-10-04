"use client";

import { useEffect, useMemo, useState } from "react";
import { Search, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import Pagination from "@/components/ui/Pagination";
import { useDebouncedPageSearch } from "@/hooks/useDebouncedValue";
import { fetchPaged, isAbortError } from "@/lib/pagedApi";

export function RemotePagedSelect({
  label = "",
  value,
  selectedLabel = "",
  onChange,
  endpoint,
  resourceName = "opciones",
  getOptionLabel = (item) => item?.nombre || item?.username || String(item?.id ?? ""),
  searchPlaceholder = "Buscar...",
  placeholder = "Seleccione una opción",
  pageSize = 10,
  sortBy = "id",
  direction = "asc",
  filters = {},
  disabled = false,
  required = false,
  error = "",
  emptyMessage = "No se encontraron resultados.",
}) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const debouncedSearch = useDebouncedPageSearch(search, setPage, 300);
  const [items, setItems] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [selectedItem, setSelectedItem] = useState(null);

  const filtersKey = useMemo(() => JSON.stringify(filters || {}), [filters]);
  const stableFilters = useMemo(() => JSON.parse(filtersKey), [filtersKey]);

  useEffect(() => {
    setPage(1);
  }, [filtersKey]);

  useEffect(() => {
    if (!value) {
      setSelectedItem(null);
    }
  }, [value]);

  useEffect(() => {
    if (!open || disabled) return undefined;

    const controller = new AbortController();

    async function load() {
      setLoading(true);
      setLoadError("");

      try {
        const result = await fetchPaged(endpoint, {
          search: debouncedSearch,
          page,
          size: pageSize,
          sortBy,
          direction,
          filters: stableFilters,
          signal: controller.signal,
          resourceName,
        });

        setItems(result.content);
        setTotalElements(result.totalElements);
        setTotalPages(result.totalPages);

        if (result.totalPages > 0 && result.page > result.totalPages) {
          setPage(result.totalPages);
        } else if (result.page !== page) {
          setPage(result.page);
        }
      } catch (requestError) {
        if (isAbortError(requestError)) return;
        setItems([]);
        setTotalElements(0);
        setTotalPages(0);
        setLoadError(requestError?.message || `No fue posible cargar ${resourceName}.`);
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    load();
    return () => controller.abort();
  }, [
    open,
    disabled,
    endpoint,
    debouncedSearch,
    page,
    pageSize,
    sortBy,
    direction,
    stableFilters,
    resourceName,
  ]);

  const currentLabel = selectedItem
    ? getOptionLabel(selectedItem)
    : selectedLabel || (value ? `Seleccionado #${value}` : placeholder);

  function selectItem(item) {
    setSelectedItem(item);
    onChange?.(item?.id ?? "", item);
    setOpen(false);
  }

  function clearSelection(event) {
    event.stopPropagation();
    setSelectedItem(null);
    onChange?.("", null);
  }

  return (
    <div className="flex flex-col gap-1.5 w-full">
      {label && (
        <label className="text-sm font-medium leading-none">
          {label}
          {required && <span className="ml-1 text-red-500">*</span>}
        </label>
      )}

      <div className="relative">
        <button
          type="button"
          disabled={disabled}
          onClick={() => setOpen((current) => !current)}
          className={`flex h-8 w-full items-center justify-between rounded-lg border bg-background px-2.5 py-1 text-left text-sm ${
            error ? "border-red-500" : ""
          } ${disabled ? "cursor-not-allowed opacity-60" : ""}`}
        >
          <span className={value ? "truncate" : "truncate text-muted-foreground"}>
            {currentLabel}
          </span>
          {value && !disabled ? (
            <span
              role="button"
              tabIndex={0}
              aria-label="Limpiar selección"
              onClick={clearSelection}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") clearSelection(event);
              }}
              className="ml-2 rounded p-0.5 hover:bg-muted"
            >
              <X className="h-3.5 w-3.5" />
            </span>
          ) : (
            <Search className="ml-2 h-3.5 w-3.5 text-muted-foreground" />
          )}
        </button>

        {open && !disabled && (
          <div className="absolute z-50 mt-1 w-full min-w-[320px] rounded-lg border bg-background p-3 shadow-lg">
            <div className="relative mb-3">
              <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
              <input
                autoFocus
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder={searchPlaceholder}
                className="h-9 w-full rounded-md border bg-background pl-8 pr-3 text-sm"
              />
            </div>

            {loading ? (
              <p className="py-4 text-center text-sm text-muted-foreground">Cargando...</p>
            ) : loadError ? (
              <p className="py-3 text-sm text-red-600">{loadError}</p>
            ) : items.length === 0 ? (
              <p className="py-4 text-center text-sm text-muted-foreground">{emptyMessage}</p>
            ) : (
              <div className="max-h-60 space-y-1 overflow-auto">
                {items.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => selectItem(item)}
                    className={`w-full rounded-md px-3 py-2 text-left text-sm hover:bg-muted ${
                      Number(value) === Number(item.id) ? "bg-muted font-medium" : ""
                    }`}
                  >
                    {getOptionLabel(item)}
                  </button>
                ))}
              </div>
            )}

            <Pagination
              currentPage={page}
              totalPages={totalPages}
              onPageChange={setPage}
              pageSize={pageSize}
              onPageSizeChange={() => {}}
              pageSizeOptions={[pageSize]}
              totalItems={totalElements}
            />

            <div className="mt-2 flex justify-end">
              <Button type="button" size="sm" variant="outline" onClick={() => setOpen(false)}>
                Cerrar
              </Button>
            </div>
          </div>
        )}
      </div>

      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}

export default RemotePagedSelect;
