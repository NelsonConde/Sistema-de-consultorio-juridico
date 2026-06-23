/**
 * Hook para detectar si el viewport es móvil.
 *
 * Usa la API `matchMedia` del navegador para escuchar cambios de tamaño
 * en tiempo real. El breakpoint coincide con el de Tailwind CSS (`md`).
 *
 * @module hooks/use-mobile
 */

import * as React from "react";

/**
 * Ancho máximo en píxeles que se considera viewport móvil.
 * Coincide con el breakpoint `md` de Tailwind CSS (768 px).
 *
 * @type {number}
 */
const MOBILE_BREAKPOINT = 768;

/**
 * Devuelve `true` si el ancho del viewport es menor que `MOBILE_BREAKPOINT`.
 *
 * Se actualiza automáticamente cuando el usuario redimensiona la ventana.
 * Devuelve `false` durante el SSR (antes del montaje del componente).
 *
 * @returns {boolean} `true` si el viewport es móvil, `false` si es escritorio.
 *
 * @example
 * function MiComponente() {
 *   const isMobile = useIsMobile();
 *   return <div>{isMobile ? "Móvil" : "Escritorio"}</div>;
 * }
 */
export function useIsMobile() {
  const [isMobile, setIsMobile] = React.useState(undefined);

  React.useEffect(() => {
    const mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`);

    const onChange = () => {
      setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);
    };

    mql.addEventListener("change", onChange);
    setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);

    return () => mql.removeEventListener("change", onChange);
  }, []);

  return !!isMobile;
}
