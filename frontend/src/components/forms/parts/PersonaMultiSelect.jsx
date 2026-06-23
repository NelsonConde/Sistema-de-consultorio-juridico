"use client";

import React, { useState, useRef, useEffect } from "react";

/**
 * Selector de personas con modo single o multi.
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} [props.personas=[]] - Lista de personas disponibles.
 * @param {Array<number>|string} [props.selectedIds=[]] - IDs seleccionados.
 * @param {function} props.onChange - Callback cuando cambian las selecciones.
 * @param {string} [props.placeholder] - Texto de búsqueda.
 * @param {boolean} [props.disabled=false] - Indica si el selector está deshabilitado.
 * @param {boolean} [props.single=false] - Activa modo de selección única.
 * @param {boolean} [props.required=false] - Indica si el campo es obligatorio.
 * @returns {JSX.Element} Selector de personas con búsqueda.
 */
export function PersonaMultiSelect({
  personas = [],
  selectedIds = [],
  onChange,
  placeholder = "Buscar persona...",
  disabled = false,
  single = false,
  required = false,
}) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const containerRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
        setSearch("");
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Modo single
  const singleId = single ? (selectedIds ? Number(selectedIds) : null) : null;
  const singlePersona = single
    ? personas.find((p) => Number(p.id) === singleId) || null
    : null;

  // Modo multi
  const normalizedIds = single ? [] : (selectedIds || []).map(Number);
  const selectedPersonas = single
    ? []
    : personas.filter((p) => normalizedIds.includes(Number(p.id)));

  const filtered = personas.filter((p) => {
    const q = search.toLowerCase();
    return (
      p.nombres?.toLowerCase().includes(q) ||
      p.apellidos?.toLowerCase().includes(q) ||
      p.numeroDocumento?.toLowerCase().includes(q)
    );
  });

  function handleSelect(id) {
    const numId = Number(id);
    if (single) {
      onChange(numId);
      setOpen(false);
      setSearch("");
    } else {
      if (normalizedIds.includes(numId)) {
        onChange(normalizedIds.filter((x) => x !== numId));
      } else {
        onChange([...normalizedIds, numId]);
      }
    }
  }

  function handleClearSingle() {
    onChange("");
    setSearch("");
  }

  function removeMulti(id) {
    onChange(normalizedIds.filter((x) => x !== Number(id)));
  }

  function inputPlaceholder() {
    if (single) {
      return singlePersona
        ? `${singlePersona.nombres} ${singlePersona.apellidos} — ${singlePersona.numeroDocumento}`
        : placeholder;
    }
    return selectedPersonas.length > 0
      ? `${selectedPersonas.length} seleccionada(s) — buscar más...`
      : placeholder;
  }

  const ic =
    "w-full rounded-md border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50";

  return (
    <div ref={containerRef} className="relative">
      {/* Tags modo multi */}
      {!single && selectedPersonas.length > 0 && (
        <div className="flex flex-wrap gap-1 mb-2">
          {selectedPersonas.map((p) => (
            <span
              key={p.id}
              className="inline-flex items-center gap-1 rounded-full bg-primary/10 border border-primary/20 px-2 py-0.5 text-xs font-medium text-primary"
            >
              {p.nombres} {p.apellidos}
              {!disabled && (
                <button
                  type="button"
                  onClick={() => removeMulti(p.id)}
                  className="ml-0.5 text-primary/60 hover:text-primary font-bold leading-none"
                  aria-label={`Quitar ${p.nombres}`}
                >
                  ×
                </button>
              )}
            </span>
          ))}
        </div>
      )}

      {/* Input trigger */}
      <div className="relative">
        <input
          type="text"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          placeholder={inputPlaceholder()}
          disabled={disabled}
          className={`${ic} ${single && singlePersona && !open ? "caret-transparent" : ""}`}
          autoComplete="off"
        />

        {/* Texto de persona seleccionada en modo single (cuando no está editando) */}
        {single && singlePersona && !open && (
          <span className="absolute inset-y-0 left-3 right-10 flex items-center text-sm pointer-events-none truncate">
            {singlePersona.nombres} {singlePersona.apellidos}
            <span className="text-muted-foreground ml-1">
              — {singlePersona.numeroDocumento}
            </span>
          </span>
        )}

        {/* Limpiar en modo single */}
        {single && singlePersona && !disabled && (
          <button
            type="button"
            onClick={handleClearSingle}
            className="absolute right-7 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground text-lg leading-none"
            aria-label="Limpiar selección"
          >
            ×
          </button>
        )}

        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none text-xs">
          ▾
        </span>
      </div>

      {/* Input oculto para validación required en modo single */}
      {single && required && (
        <input
          type="text"
          required
          readOnly
          value={singleId || ""}
          className="sr-only"
          tabIndex={-1}
          aria-hidden="true"
        />
      )}

      {/* Dropdown */}
      {open && !disabled && (
        <div className="absolute z-50 mt-1 w-full rounded-md border bg-popover shadow-md max-h-52 overflow-y-auto">
          {filtered.length === 0 ? (
            <p className="px-3 py-2 text-sm text-muted-foreground">Sin resultados</p>
          ) : (
            filtered.map((p) => {
              const isSelected = single
                ? Number(p.id) === singleId
                : normalizedIds.includes(Number(p.id));
              return (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => handleSelect(p.id)}
                  className={`w-full text-left flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted transition-colors ${
                    isSelected ? "bg-primary/5 font-medium" : ""
                  }`}
                >
                  {/* Checkbox (multi) o radio (single) */}
                  <span
                    className={`w-4 h-4 flex-shrink-0 border flex items-center justify-center ${
                      single ? "rounded-full" : "rounded"
                    } ${isSelected ? "bg-primary border-primary text-primary-foreground" : "border-input"}`}
                  >
                    {isSelected && !single && (
                      <svg viewBox="0 0 10 8" fill="none" stroke="currentColor" strokeWidth="2" className="w-2.5 h-2.5">
                        <path d="M1 4l2.5 2.5L9 1" />
                      </svg>
                    )}
                    {isSelected && single && (
                      <span className="w-2 h-2 rounded-full bg-white block" />
                    )}
                  </span>
                  <span className="truncate">
                    {p.nombres} {p.apellidos}
                    <span className="text-muted-foreground ml-1 font-normal">— {p.numeroDocumento}</span>
                  </span>
                </button>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
