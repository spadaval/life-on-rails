import { create } from "zustand";
import { ExpandedHabit } from "../server/queries";

export enum State {
  None,
  CreateLinkedHabit,
  CreateGoal,
  CreateHabit,
  CreateMetric,
  HabitPanel,
}

interface CreateGoal {
  state: State.CreateGoal;
}
interface CreateHabit {
  state: State.CreateHabit;
}
interface CreateMetric {
  state: State.CreateMetric;
}
interface HabitPanel {
  state: State.HabitPanel;
  habitId: string;
}

interface OverviewStore {
  modal: CreateGoal | CreateHabit | CreateMetric | HabitPanel | undefined;
  openCreateGoalModal: () => void;
  openCreateHabitModal: () => void;
  openCreateMetricModal: () => void;
  openHabitPanel: (habitId: string) => void;
  reset: () => void;
}

export const useOverviewStore = create<OverviewStore>()((set) => ({
  modal: undefined,
  openCreateGoalModal() {
    set(() => ({
      modal: { state: State.CreateGoal },
    }));
  },
  openCreateHabitModal() {
    set(() => ({
      modal: { state: State.CreateHabit },
    }));
  },
  openCreateMetricModal() {
    set(() => ({
      modal: { state: State.CreateMetric },
    }));
  },
  openHabitPanel(habitId) {
    set(() => ({
      modal: { state: State.HabitPanel, habitId },
    }));
  },
  reset: () => {
    set((_) => ({ modal: undefined }));
  },
}));
