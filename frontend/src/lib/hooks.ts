"use client";

import { useEffect } from "react";

/**
 * Runs an async loader once on mount (and again when `load` identity changes).
 * The call is deferred to a microtask so state updates happen in a callback, not in the effect body.
 */
export function useLoadOnMount(load: () => Promise<unknown>) {
  useEffect(() => {
    let alive = true;
    Promise.resolve().then(() => { if (alive) void load(); });
    return () => { alive = false; };
  }, [load]);
}
