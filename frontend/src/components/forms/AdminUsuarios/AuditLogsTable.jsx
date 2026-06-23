"use client"

/**
 * Tabla de logs de auditoría del sistema.
 *
 * Muestra el historial de acciones realizadas por los usuarios.
 * Solo visible para administradores con acceso a la sección de administración.
 *
 * @module components/forms/AdminUsuarios/AuditLogsTable
 */
;

import { useCallback, useEffect, useState } from "react";
import { ArrowUpDown, ChevronDown, ChevronUp, Info, Search } from "lucide-react";
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
import { API_URL_BASE } from "@/lib/config";
import { getApiErrorDescription, readResponseBody } from "@/lib/api";
import { toast } from "sonner";

/**
 * Tabla que muestra los registros de auditoría.
 * @returns {JSX.Element} Componente de registros de auditoría.
 */
export function AuditLogsTable() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [totalItems, setTotalItems] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  
  // Params
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [search, setSearch] = useState("")
  const [searchInputValue, setSearchInputValue] = useState("")
  const [sortBy, setSortBy] = useState("timestamp")
  const [sortDir, setSortDir] = useState("desc")

  const fetchLogs = useCallback(async () => {
    try {
      setLoading(true)
      const url = new URL(`${API_URL_BASE}/audit`)
      url.searchParams.append("page", (page - 1).toString())
      url.searchParams.append("size", size.toString())
      url.searchParams.append("sortBy", sortBy)
      url.searchParams.append("sortDir", sortDir)
      
      if (search.trim() !== "") {
        url.searchParams.append("username", search.trim())
      }

      const response = await fetch(url.toString(), {
        credentials: "include"
      })

      const data = await readResponseBody(response)

      if (!response.ok) {
        throw new Error(getApiErrorDescription(data, "Error cargando los registros de auditoría"))
      }

      setLogs(data.content || [])
      setTotalPages(data.totalPages || 0)
      setTotalItems(data.totalElements || 0)
    } catch (error) {
      toast.error(error.message)
      setLogs([])
    } finally {
      setLoading(false)
    }
  }, [page, size, search, sortBy, sortDir])

  useEffect(() => {
    fetchLogs()
  }, [fetchLogs])

  const handleSearch = (e) => {
    e.preventDefault()
    setSearch(searchInputValue)
    setPage(1) // Reset a página 1 al buscar
  }

  const handleClearSearch = () => {
    setSearchInputValue("")
    setSearch("")
    setPage(1)
  }

  const handleSort = (column) => {
    if (sortBy === column) {
      setSortDir(sortDir === "asc" ? "desc" : "asc")
    } else {
      setSortBy(column)
      setSortDir("desc")
    }
    setPage(1)
  }

  const formatDate = (dateString) => {
    if (!dateString) return "N/A"
    const date = new Date(dateString)
    return new Intl.DateTimeFormat('es-CO', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: true
    }).format(date)
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <form onSubmit={handleSearch} className="flex w-full sm:max-w-sm items-center space-x-2">
          <div className="relative flex-1">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Buscar por nombre de usuario..."
              className="pl-9"
              value={searchInputValue}
              onChange={(e) => setSearchInputValue(e.target.value)}
            />
          </div>
          <Button type="submit" variant="secondary" disabled={loading}>
            Buscar
          </Button>
          {search && (
            <Button type="button" variant="ghost" onClick={handleClearSearch}>
              Limpiar
            </Button>
          )}
        </form>
      </div>

      <div className="rounded-xl border bg-card text-card-foreground shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-muted/50 text-muted-foreground">
              <tr>
                <th 
                  className="px-4 py-3 font-medium cursor-pointer hover:bg-muted/80 transition-colors"
                  onClick={() => handleSort("timestamp")}
                >
                  <div className="flex items-center gap-1">
                    Fecha
                    {sortBy === "timestamp" ? (
                      sortDir === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                    ) : (
                      <ArrowUpDown className="h-4 w-4 opacity-50" />
                    )}
                  </div>
                </th>
                <th 
                  className="px-4 py-3 font-medium cursor-pointer hover:bg-muted/80 transition-colors"
                  onClick={() => handleSort("username")}
                >
                  <div className="flex items-center gap-1">
                    Usuario
                    {sortBy === "username" ? (
                      sortDir === "asc" ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />
                    ) : (
                      <ArrowUpDown className="h-4 w-4 opacity-50" />
                    )}
                  </div>
                </th>
                <th className="px-4 py-3 font-medium">Acción</th>
                <th className="px-4 py-3 font-medium">Tipo de Entidad</th>
                <th className="px-4 py-3 font-medium">ID Entidad</th>
                <th className="px-4 py-3 font-medium">Detalles</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {loading ? (
                <tr>
                  <td colSpan="6" className="h-32 text-center text-muted-foreground">
                    <div className="flex items-center justify-center gap-2">
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-primary border-r-transparent" />
                      Cargando registros...
                    </div>
                  </td>
                </tr>
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan="6" className="h-32 text-center text-muted-foreground">
                    No se encontraron registros de auditoría.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-muted/30 transition-colors">
                    <td className="px-4 py-3 whitespace-nowrap">{formatDate(log.timestamp)}</td>
                    <td className="px-4 py-3 font-medium">{log.username}</td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold bg-primary/10 text-primary border-primary/20">
                        {log.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-muted-foreground">{log.entityName}</td>
                    <td className="px-4 py-3 text-muted-foreground">{log.entityId || "-"}</td>
                    <td className="px-4 py-3 text-center" title={log.details ? "Ver detalles" : "Sin detalles disponibles"}>
                      {log.details ? (
                        <Dialog>
                          <DialogTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              aria-label={`Ver detalles del registro de ${log.username}`}
                              className="h-8 w-8"
                            >
                              <Info className="h-4 w-4" />
                            </Button>
                          </DialogTrigger>
                          <DialogContent className="sm:max-w-lg">
                            <DialogHeader>
                              <DialogTitle>Detalles del registro</DialogTitle>
                              <DialogDescription>
                                {log.action} · {log.entityName || "Entidad"} · {formatDate(log.timestamp)}
                              </DialogDescription>
                            </DialogHeader>
                            <div className="max-h-[60vh] overflow-auto rounded-md border bg-muted/30 p-3 text-sm whitespace-pre-wrap wrap-break-word">
                              {log.details}
                            </div>
                          </DialogContent>
                        </Dialog>
                      ) : (
                        <span className="text-xs text-muted-foreground">—</span>
                      )}
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
          setSize(newSize)
          setPage(1)
        }}
        pageSizeOptions={[10, 20, 50, 100]}
      />
    </div>
  )
}
