"use client"

import React, { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";

/**
 * Modal reutilizable de selección simple para consultas.
 * Extraído de los formularios para mantener la vista separada de la lógica del módulo.
 */
export function ModalSimple({
  abierto,
  titulo,
  items,
  busqueda,
  setBusqueda,
  onSeleccionar,
  onCerrar,
  seleccionado,
  renderItem,
}) {
  if (!abierto) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-background rounded-xl border shadow-lg w-full max-w-md mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">{titulo}</h3>
          <button
            type="button"
            onClick={onCerrar}
            className="text-muted-foreground hover:text-foreground text-xl"
          >
            ✕
          </button>
        </div>

        <input
          autoFocus
          type="text"
          placeholder="Buscar..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          className="w-full rounded-lg border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />

        <div className="max-h-72 overflow-y-auto space-y-1">
          <button
            type="button"
            onClick={() => onSeleccionar(null)}
            className={`w-full text-left px-3 py-2 rounded-lg text-sm hover:bg-muted transition-colors ${!seleccionado ? "bg-primary/10 text-primary font-medium" : ""}`}
          >
            Sin asignar
          </button>

          {items.length === 0 ? (
            <p className="text-center text-sm text-muted-foreground py-4">
              Sin resultados
            </p>
          ) : (
            items.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => onSeleccionar(item)}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm hover:bg-muted transition-colors ${seleccionado?.id === item.id ? "bg-primary/10 text-primary font-medium" : ""}`}
              >
                {renderItem(item)}
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

/** Modal reutilizable de selección múltiple. */
export function ModalMultiple({
  abierto,
  titulo,
  items,
  busqueda,
  setBusqueda,
  onConfirmar,
  onCerrar,
  seleccionados,
  renderItem,
}) {
  const [temp, setTemp] = useState([]);

  useEffect(() => {
    if (abierto) setTemp(seleccionados || []);
  }, [abierto, seleccionados]);

  if (!abierto) return null;

  function toggleItem(id) {
    const numId = Number(id);
    setTemp((prev) =>
      prev.includes(numId)
        ? prev.filter((item) => item !== numId)
        : [...prev, numId]
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-background rounded-xl border shadow-lg w-full max-w-md mx-4 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">{titulo}</h3>
          <button
            type="button"
            onClick={onCerrar}
            className="text-muted-foreground hover:text-foreground text-xl"
          >
            ✕
          </button>
        </div>

        <input
          autoFocus
          type="text"
          placeholder="Buscar..."
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          className="w-full rounded-lg border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />

        <div className="max-h-64 overflow-y-auto space-y-1">
          {items.length === 0 ? (
            <p className="text-center text-sm text-muted-foreground py-4">
              Sin resultados
            </p>
          ) : (
            items.map((item) => {
              const numId = Number(item.id);
              const marcado = temp.includes(numId);
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => toggleItem(item.id)}
                  className={`w-full text-left px-3 py-2 rounded-lg text-sm hover:bg-muted transition-colors flex items-center gap-3 ${marcado ? "bg-primary/10" : ""}`}
                >
                  <span
                    className={`w-4 h-4 rounded border flex items-center justify-center flex-shrink-0 ${marcado ? "bg-primary border-primary" : "border-gray-400"}`}
                  >
                    {marcado && <span className="text-white text-xs">✓</span>}
                  </span>
                  <span>{renderItem(item)}</span>
                </button>
              );
            })
          )}
        </div>

        <div className="flex justify-between items-center pt-2">
          <span className="text-xs text-muted-foreground">
            {temp.length} seleccionado(s)
          </span>
          <div className="flex gap-2">
            <Button type="button" variant="outline" size="sm" onClick={() => setTemp([])}>
              Limpiar
            </Button>
            <Button type="button" size="sm" onClick={() => onConfirmar(temp)}>
              Confirmar
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
