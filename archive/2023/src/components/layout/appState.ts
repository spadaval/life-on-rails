import { create } from "zustand";

export enum State {
  None,
  CreateLinkedHabit,
  Create,
  CreateGoal,
  CreateHabit,
  CreateMetric,
  HabitPanel,
}

interface Create {
  state: State.Create;
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

interface AppState {
  modal:
    | Create
    | CreateGoal
    | CreateHabit
    | CreateMetric
    | HabitPanel
    | undefined;
  openCreateModal: () => void;
  openCreateGoalModal: () => void;
  openCreateHabitModal: () => void;
  openCreateMetricModal: () => void;
  openHabitPanel: (habitId: string) => void;
  reset: () => void;
}

export const useAppState = create<AppState>()((set) => ({
  modal: undefined,
  openCreateModal() {
    set(() => ({
      modal: { state: State.Create },
    }));
  },
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
