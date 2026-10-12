/* eslint-disable @typescript-eslint/no-misused-promises */
import classNames from "classnames";
import type { ReactNode } from "react";
import { useForm } from "react-hook-form";
import { api } from "../utils/api";

import { State, useAppState } from "../components/layout/appState";
import { Card, CardHeader } from "./ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "./ui/dialog";

function Modal({
  close,
  children,
}: {
  close: () => void;
  children: ReactNode;
}) {
  return (
    <div
      className={classNames(
        "fixed inset-0 z-50 flex h-screen w-screen items-center justify-center overflow-y-auto overflow-x-hidden backdrop-blur-sm backdrop-brightness-50"
      )}
      onKeyDown={(it) => {
        console.log(it);
        if (it.key == "Escape") close();
      }}
    >
      {children}
    </div>
  );
}

export function CreateGoalModal({ close }: { close: () => void }) {
  interface GoalCreate {
    name: string;
  }
  //const reset = useOverviewStore((store) => store.reset);

  const context = api.useContext();
  const mutation = api.goals.createGoal.useMutation({
    onSuccess() {
      void context.goals.getAllGoals.invalidate();
    },
  });

  const { register, handleSubmit } = useForm<GoalCreate>();
  const onSubmit = (data: GoalCreate) => {
    close();
    console.log(data);
    mutation.mutate({
      name: data.name,
    });
  };

  return (
    <Modal close={close}>
      <div className="relative max-h-full w-full max-w-md">
        <div className="relative rounded-lg bg-white shadow dark:bg-gray-700">
          <button
            type="button"
            className="absolute right-2.5 top-3 ml-auto inline-flex items-center rounded-lg bg-transparent p-1.5 text-sm text-gray-400 hover:bg-gray-200 hover:text-gray-900 dark:hover:bg-gray-800 dark:hover:text-white"
            onClick={close}
          >
            <svg
              className="h-5 w-5"
              fill="currentColor"
              viewBox="0 0 20 20"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fillRule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
          </button>
          <div className="px-6 py-6 lg:px-8">
            <h3 className="mb-4 text-xl font-medium text-gray-900 dark:text-white">
              Create a new Goal
            </h3>
            <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
              <div>
                <input
                  type="text"
                  className="block w-full rounded-lg border border-gray-300 bg-gray-50 p-2.5 text-sm text-gray-900 focus:border-blue-500 focus:ring-blue-500 dark:border-gray-500 dark:bg-gray-600 dark:text-white dark:placeholder-gray-400"
                  placeholder="Goal Name"
                  required
                  {...register("name")}
                />
              </div>
              <button
                type="submit"
                className="w-full rounded-lg bg-blue-700 px-5 py-2.5 text-center text-sm font-medium text-white hover:bg-blue-800 focus:outline-none focus:ring-4 focus:ring-blue-300 dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800"
              >
                Create Goal
              </button>
            </form>
          </div>
        </div>
      </div>
    </Modal>
  );
}

function CreateCard({
  onClick,
  color,
  heroText,
  description,
}: {
  onClick: () => void;
  color: string;
  heroText: string;
  description: string;
}) {
  return (
    <Card
      className="h-[156px] w-full cursor-pointer hover:shadow-md flex flex-row"
      onClick={onClick}
    >
        <div
          className={`${color} flex h-[156px] w-[156px] flex-col items-center justify-center text-white flex-none`}
          style={{width:"156px", height: "156px"}}
        >
          {heroText}
        </div>
        <CardHeader className="space-y-3">
          <p className="text-md text-gray-500">{description}</p>
        </CardHeader>
    </Card>
  );
}

export function CreateModal({ close }: { close: () => void }) {
  const store = useAppState();
  const openCreateGoalModal = useAppState((store) => store.openCreateGoalModal);
  const openCreateHabitModal = useAppState(
    (store) => store.openCreateHabitModal
  );
  const openCreateMetricModal = useAppState(
    (store) => store.openCreateMetricModal
  );

  return (
    <Dialog open={store.modal?.state === State.Create} onOpenChange={close}>
      <DialogHeader>
        <DialogTitle>Create an item</DialogTitle>
      </DialogHeader>
      <DialogContent className="w-fit pt-10">
        <CreateCard
          onClick={openCreateGoalModal}
          color="bg-green-200"
          heroText="Goal"
          description="Connect items together towards one shared goal"
        ></CreateCard>
        <CreateCard
          onClick={openCreateHabitModal}
          color="bg-blue-200"
          heroText="Habit"
          description="Build a habit"
        ></CreateCard>
        <CreateCard
          onClick={openCreateMetricModal}
          color="bg-red-200"
          heroText="Metric"
          description="Define custom tracking and scoring for your habits and goals"
        ></CreateCard>
      </DialogContent>
    </Dialog>
  );
}
export function CreateHabitModal({ close }: { close: () => void }) {
  interface HabitCreate {
    description: string;
  }

  const context = api.useContext();
  const mutation = api.habits.createHabit.useMutation({
    onSuccess() {
      void context.goals.getAllGoals.invalidate();
    },
  });

  const { register, handleSubmit } = useForm<HabitCreate>();
  const onSubmit = (data: HabitCreate) => {
    close();
    console.log(data);
    mutation.mutate({
      description: data.description,
    });
  };

  return (
    <Modal close={close}>
      <div className="relative max-h-full w-full max-w-md">
        <div className="relative rounded-lg bg-white shadow dark:bg-gray-700">
          <button
            type="button"
            className="absolute right-2.5 top-3 ml-auto inline-flex items-center rounded-lg bg-transparent p-1.5 text-sm text-gray-400 hover:bg-gray-200 hover:text-gray-900 dark:hover:bg-gray-800 dark:hover:text-white"
            onClick={close}
          >
            <svg
              className="h-5 w-5"
              fill="currentColor"
              viewBox="0 0 20 20"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fillRule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
          </button>
          <div className="px-6 py-6 lg:px-8">
            <h3 className="mb-4 text-xl font-medium text-gray-900 dark:text-white">
              Create a new Habit
            </h3>
            <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
              <div>
                <input
                  type="text"
                  className="block w-full rounded-lg border border-gray-300 bg-gray-50 p-2.5 text-sm text-gray-900 focus:border-blue-500 focus:ring-blue-500 dark:border-gray-500 dark:bg-gray-600 dark:text-white dark:placeholder-gray-400"
                  placeholder="Habit Name"
                  required
                  {...register("description")}
                />
              </div>
              <button
                type="submit"
                className="w-full rounded-lg bg-blue-700 px-5 py-2.5 text-center text-sm font-medium text-white hover:bg-blue-800 focus:outline-none focus:ring-4 focus:ring-blue-300 dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800"
              >
                Create Habit
              </button>
            </form>
          </div>
        </div>
      </div>
    </Modal>
  );
}
export function CreateMetricModal({ close }: { close: () => void }) {
  interface MetricCreate {
    prompt: string;
  }

  const context = api.useContext();
  const mutation = api.metrics.createMetric.useMutation({
    onSuccess() {
      void context.goals.getAllGoals.invalidate();
    },
  });

  const { register, handleSubmit } = useForm<MetricCreate>();
  const onSubmit = (data: MetricCreate) => {
    close();
    console.log(data);
    mutation.mutate({
      prompt: data.prompt,
    });
  };

  return (
    <Modal close={close}>
      <div className="relative max-h-full w-full max-w-md">
        <div className="relative rounded-lg bg-white shadow dark:bg-gray-700">
          <button
            type="button"
            className="absolute right-2.5 top-3 ml-auto inline-flex items-center rounded-lg bg-transparent p-1.5 text-sm text-gray-400 hover:bg-gray-200 hover:text-gray-900 dark:hover:bg-gray-800 dark:hover:text-white"
            onClick={close}
          >
            <svg
              className="h-5 w-5"
              fill="currentColor"
              viewBox="0 0 20 20"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fillRule="evenodd"
                d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                clipRule="evenodd"
              />
            </svg>
          </button>
          <div className="px-6 py-6 lg:px-8">
            <h3 className="mb-4 text-xl font-medium text-gray-900 dark:text-white">
              Create a new Metric
            </h3>
            <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
              <div>
                <input
                  type="text"
                  className="block w-full rounded-lg border border-gray-300 bg-gray-50 p-2.5 text-sm text-gray-900 focus:border-blue-500 focus:ring-blue-500 dark:border-gray-500 dark:bg-gray-600 dark:text-white dark:placeholder-gray-400"
                  placeholder="Metric Name"
                  required
                  {...register("prompt")}
                />
              </div>
              <button
                type="submit"
                className="w-full rounded-lg bg-blue-700 px-5 py-2.5 text-center text-sm font-medium text-white hover:bg-blue-800 focus:outline-none focus:ring-4 focus:ring-blue-300 dark:bg-blue-600 dark:hover:bg-blue-700 dark:focus:ring-blue-800"
              >
                Create Metric
              </button>
            </form>
          </div>
        </div>
      </div>
    </Modal>
  );
}

export const Modals = () => {
  const store = useAppState();
  const reset = store.reset;
  return (
    <>
      {store.modal?.state === State.Create && (
        <CreateModal close={reset}></CreateModal>
      )}
      {store.modal?.state === State.CreateGoal && (
        <CreateGoalModal close={reset}></CreateGoalModal>
      )}
      {store.modal?.state === State.CreateHabit && (
        <CreateHabitModal close={reset}></CreateHabitModal>
      )}
      {store.modal?.state === State.CreateMetric && (
        <CreateMetricModal close={reset}></CreateMetricModal>
      )}
    </>
  );
};
