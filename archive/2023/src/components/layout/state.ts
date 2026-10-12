import { create } from "zustand";

type SidebarStore = {
  open: boolean;
  setOpen: (open: boolean) => void;
};

export const useSidebarStore = create<SidebarStore>()((set) => ({
  open: true,
  setOpen(open) {
    set(() => ({
      open,
    }));
  },
}));
