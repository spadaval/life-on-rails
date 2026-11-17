// The two states that are links in this direction (state=stale, state=crowded), for every screen.
import { createContext, useContext } from "react";

export interface Mode {
  /** LoR cannot be reached: the last data it sent, and when. Changes are off until it answers. */
  staleSince: number | null;
  /** Twelve things waiting on him. */
  crowded: boolean;
}

export const ModeCtx = createContext<Mode>({ staleSince: null, crowded: false });
export const useMode = () => useContext(ModeCtx);
