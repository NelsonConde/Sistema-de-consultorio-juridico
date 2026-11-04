"use client";

import { useEffect, useState } from "react";

export function useDebouncedValue(value, delay = 350) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => window.clearTimeout(timeoutId);
  }, [value, delay]);

  return debouncedValue;
}


export function useDebouncedPageSearch(value, setPage, delay = 350) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    if (value === debouncedValue) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setPage(1);
      setDebouncedValue(value);
    }, delay);

    return () => window.clearTimeout(timeoutId);
  }, [value, debouncedValue, delay, setPage]);

  return debouncedValue;
}


export default useDebouncedValue;
