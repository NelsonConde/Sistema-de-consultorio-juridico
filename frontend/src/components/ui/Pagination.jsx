"use client";

import React from "react";
import { Button } from "@/components/ui/button";

/**
 * Componente de paginación para listas de datos.
 * @param {{currentPage:number, totalPages:number, onPageChange:function, pageSize:number, onPageSizeChange:function, pageSizeOptions?:Array<number>, totalItems?:number}} props - Propiedades de paginación.
 * @returns {JSX.Element|null} Control de paginación.
 */
export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  pageSize,
  onPageSizeChange,
  pageSizeOptions = [5, 10, 20, 50],
  totalItems,
}) {
  if (!totalPages || totalPages < 1) return null;

  function changePage(p) {
    const next = Math.max(1, Math.min(totalPages, p));
    if (next !== currentPage) onPageChange(next);
  }

  const visiblePages = new Set([1, totalPages, currentPage - 1, currentPage, currentPage + 1]);
  const pages = Array.from(visiblePages).filter((p) => p >= 1 && p <= totalPages).sort((a, b) => a - b);

  return (
    <div className="flex items-center justify-between gap-4 py-3">
      <div className="flex items-center gap-2">
        <Button type="button" size="sm" variant="outline" onClick={() => changePage(1)} disabled={currentPage === 1}>
          «
        </Button>
        <Button type="button" size="sm" variant="outline" onClick={() => changePage(currentPage - 1)} disabled={currentPage === 1}>
          ‹
        </Button>

        {pages.map((p) => (
          <Button
            key={p}
            type="button"
            size="sm"
            variant={p === currentPage ? "default" : "outline"}
            onClick={() => changePage(p)}
          >
            {p}
          </Button>
        ))}

        <Button type="button" size="sm" variant="outline" onClick={() => changePage(currentPage + 1)} disabled={currentPage === totalPages}>
          ›
        </Button>
        <Button type="button" size="sm" variant="outline" onClick={() => changePage(totalPages)} disabled={currentPage === totalPages}>
          »
        </Button>
      </div>

      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        {typeof totalItems === "number" && (
          <div>{totalItems} registro(s)</div>
        )}

        <div className="flex items-center gap-2">
          <label className="text-sm">Mostrar</label>
          <select
            value={pageSize}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
            className="rounded-md border bg-background px-2 py-1 text-sm"
          >
            {pageSizeOptions.map((opt) => (
              <option key={opt} value={opt}>{opt}</option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
}

export default Pagination;
