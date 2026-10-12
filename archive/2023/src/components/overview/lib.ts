import { useEffect, useState } from "react";

export const textcolor = (score: number | undefined) => {
  if (typeof score === "undefined") return "text-black";
  return score < 0.25
    ? "text-red-500"
    : score < 0.7
    ? "text-yellow-500"
    : "text-green-400";
};

export const bgcolor = (score: number | undefined) => {
  if (!score) return "text-black";
  return score < 0.25
    ? "bg-red-500"
    : score < 0.7
    ? "bg-yellow-500"
    : "bg-green-400";
};

export function useDebouncedState<T>(
  initialState: T,
  delay: number,
  sync: (state: T, syncedState: T) => void
) {
  const [currentState, setCurrentState] = useState<T>(initialState);
  const [syncedState, setSyncedState] = useState<T>(initialState);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (currentState != syncedState) {
        sync(currentState, syncedState);
        setSyncedState(currentState);
      }
    }, delay);

    return () => {
      clearTimeout(timer);
    };
  }, [currentState, delay, sync, syncedState]);

  return {state: currentState, setState: setCurrentState};
}
