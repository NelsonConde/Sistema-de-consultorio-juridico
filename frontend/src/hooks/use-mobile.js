/**
 * Implementation detail.
 *
 * Implementation detail.
 * Uses the Tailwind CSS `md` breakpoint (768 px).
 *
 * @module hooks/use-mobile
 */

import * as React from "react";

/**
 * Implementation detail.
 * Uses the Tailwind CSS `md` breakpoint (768 px).
 *
 * @type {number}
 */
const MOBILE_BREAKPOINT = 768;

/**
 * Uses the Tailwind CSS `md` breakpoint (768 px).
 *
 * User flow detail.
 * Component implementation detail.
 *
 * @returns {boolean} Result value.
 *
 * @example
 * function MiComponente() {
 *   const isMobile = useIsMobile();
 * Implementation detail.
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
