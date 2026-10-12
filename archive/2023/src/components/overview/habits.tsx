/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable @typescript-eslint/no-misused-promises */
import { Cog6ToothIcon } from "@heroicons/react/24/outline";
import type { FrequencyHorizon, Goal } from "@prisma/client";
import { ScoringFormat } from "@prisma/client";
import { cva } from "class-variance-authority";
import classNames from "classnames";
import { useCombobox } from "downshift";
import { CornerDownRight } from "lucide-react";
import { useState } from "react";
import type { ExpandedHabit } from "../../server/queries";
import { api } from "../../utils/api";
import {
  CreateLinkedMetricInline,
  DropDown,
  EditableField,
  EditableNumberField,
} from "../inlineEdit";
import { useAppState } from "../layout/appState";
import { HabitIcon } from "../ui/icons";
import { HabitSheet } from "./habit-panel";
import { textcolor } from "./lib";
import { LinkedMetric } from "./metrics";
import { TagList } from "./tags";

function min(a: number, b: number) {
  return a < b ? a : b;
}

function getGoalsFilter(
  inputValue: string | undefined
): (_goal: Goal) => boolean {
  if (inputValue === undefined) {
    return (_goal: Goal) => true;
  }
  const lowerCasedInputValue = inputValue.toLowerCase();

  return function goalsFilter(goal: Goal) {
    return (
      !inputValue || goal.name.toLowerCase().includes(lowerCasedInputValue)
    );
  };
}

export function HabitHeaderLine({
  id,
  weight,
  description,
  frequency,
  frequencyHorizon,
  score,
  completions,
  scoringUnit,
}: {
  id: string;
  weight: number | undefined;
  description: string;
  frequency: number;
  frequencyHorizon: FrequencyHorizon;
  score: number;
  completions: number;
  scoringUnit: ScoringFormat;
}) {
  const context = api.useContext();
  const mutation = api.habits.editHabit.useMutation({
    onSuccess: () => {
      void context.habits.getHabit.invalidate();
      void context.goals.getAllGoals.invalidate();
    },
  });
  const editFrequency = api.habits.editFrequency.useMutation({
    onSuccess: () => {
      void context.habits.getHabit.invalidate();
      void context.goals.getAllGoals.invalidate();
    },
  });
  const editFrequencyHorizon = api.habits.editFrequencyHorizon.useMutation({
    onSuccess: () => {
      void context.goals.getAllGoals.invalidate();
      void context.habits.getHabit.invalidate();
    },
  });
  //const openHabitPanel = useOverviewStore((store) => store.openHabitPanel);

  return (
    <>
      <div className="contents">
        <div className="flex w-full flex-row items-center justify-between">
          <div className="flex flex-row items-baseline gap-2">
            <span className="inline-block rounded-full bg-blue-500 px-2 py-1 text-xs text-white">
              Habit
            </span>
            <EditableField
              initialText={description}
              commit={(text) =>
                mutation.mutate({ habitId: id, description: text })
              }
              className="font-semibold"
            ></EditableField>
          </div>
          <div className="flex flex-row items-center space-x-2 whitespace-nowrap">
            <span
              className={classNames(
                "rounded-lg bg-gray-100 p-2 text-xl",
                textcolor(score)
              )}
            >
              {scoringUnit == ScoringFormat.Normalized
                ? min(1, score).toFixed(2)
                : (min(1, score) * 100).toFixed(2) + "%"}
            </span>
            <HabitSheet habitId={id} scoringUnit={scoringUnit}>
              {/* <Cog6ToothIcon className="h-6 w-6 cursor-pointer opacity-40"></Cog6ToothIcon> */}
            </HabitSheet>
          </div>
        </div>
        <div className="flex flex-row">
          <span className="text-sm lowercase text-gray-500">
            <span className="space-x-1 text-sm">
              <span className="text-md ">Completed</span>
              <span className="text-md ">{completions}</span>
              <span className="">/</span>
              <EditableNumberField
                initial={frequency}
                commit={(number) =>
                  editFrequency.mutate({ habitId: id, frequency: number })
                }
                className="font-semibold"
              ></EditableNumberField>
              <span className="text-md ">times this</span>
              <DropDown
                frequencyHorizon={frequencyHorizon}
                commit={(freq) =>
                  editFrequencyHorizon.mutate({
                    habitId: id,
                    frequencyHorizon: freq,
                  })
                }
                className="font-semibold"
              ></DropDown>
            </span>
          </span>
        </div>
      </div>
    </>
  );
}

function LinkHabitBox({ id, closeBox }: { id: string; closeBox: () => void }) {
  const context = api.useContext();

  const goalsData = api.goals.getAllGoals.useQuery();
  const linkHabit = api.habits.linkHabit.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });

  const goals = goalsData.data?.goals.map((it) => it.goal) ?? [];

  const [items, setItems] = useState<Goal[]>(goals);

  const {
    isOpen,
    getMenuProps,
    getInputProps,
    highlightedIndex,
    getItemProps,
    selectedItem,
  } = useCombobox({
    initialIsOpen: true,
    onInputValueChange({ inputValue }) {
      setItems(goals.filter(getGoalsFilter(inputValue)));
    },
    items,
    itemToString(item) {
      return item ? item.name : "";
    },
  });
  return (
    <div>
      <div className="flex w-72 flex-row">
        <div className="flex gap-0.5 bg-white shadow-sm">
          <input
            placeholder="Choose a goal"
            className="w-full p-1.5"
            {...getInputProps()}
          />
          {/* <button
            aria-label="toggle menu"
            className="px-2"
            type="button"
            {...getToggleButtonProps()}
          >
            {isOpen ? <>&#8593;</> : <>&#8595;</>}
          </button> */}
        </div>
        <div className="flex flex-row gap-2">
          {selectedItem ? (
            <button
              className="rounded-md bg-blue-500 px-4 py-2 text-white hover:bg-blue-700"
              onClick={() => {
                //alert(`Linked habit ${id} to goal ${selectedItem.name}`);
                linkHabit.mutate({ habitId: id, goalId: selectedItem.id });
                closeBox();
              }}
            >
              Link
            </button>
          ) : (
            <button className="cursor-not-allowed rounded bg-blue-500 px-4 py-2  text-white opacity-50">
              Link
            </button>
          )}
          <button
            className="rounded-md bg-red-300 px-2  text-white hover:bg-red-500"
            onClick={closeBox}
          >
            Cancel
          </button>
        </div>
      </div>
      <ul
        className={`absolute mt-1 max-h-80 w-72 overflow-scroll bg-white p-0 shadow-md ${
          isOpen && items.length ? "" : "hidden"
        }`}
        {...getMenuProps()}
      >
        {isOpen &&
          items.map((item, index) => (
            <li
              className={classNames(
                {
                  "bg-blue-300": highlightedIndex === index,
                  "": selectedItem === item,
                },
                "flex flex-col px-3 py-2 shadow-sm"
              )}
              key={`${item.name}${index}`}
              {...getItemProps({ item, index })}
            >
              <span>{item.name}</span>
              <span className="text-sm text-gray-700">
                {item.createdAt?.toISOString() ?? "Loading..."}
              </span>
            </li>
          ))}
      </ul>
    </div>
  );
}

export function LinkHabit({ id }: { id: string }) {
  const [active, setActive] = useState<boolean>(false);
  if (!active) {
    return (
      <button
        className="text-blue-500 hover:underline"
        onClick={() => setActive(true)}
      >
        Link to Goal
      </button>
    );
  } else {
    return (
      <LinkHabitBox id={id} closeBox={() => setActive(false)}></LinkHabitBox>
    );
  }
}

export function HabitFooter({
  id,
  tags,
  linkedGoal,
  linkHabit,
  unlinkHabit,
}: {
  id: string;
  tags: string[];
  linkedGoal: string | undefined;
  linkHabit: (args: { habitId: string; tagName: string }) => void;
  unlinkHabit: (args: { habitId: string; tagName: string }) => void;
}) {
  const context = api.useContext();
  const unlinkHabitFromGoal = api.habits.unlinkHabit.useMutation({
    onSuccess() {
      void context.goals.getAllGoals.invalidate();
    },
  });

  const deleteHabit = api.habits.deleteHabit.useMutation({
    onSuccess() {
      void context.goals.getAllGoals.invalidate();
    },
  });

  const openHabitPanel = useAppState((store) => store.openHabitPanel);

  return (
    <div className="mt-6 border-t-2 border-gray-200/50 pt-4">
      <div className="flex justify-between">
        <TagList
          tags={tags}
          link={(tag) => linkHabit({ habitId: id, tagName: tag })}
          unlink={(tag) => unlinkHabit({ habitId: id, tagName: tag })}
        ></TagList>
        <div className="flex items-center space-x-4">
          {linkedGoal ? (
            <button
              className=" text-blue-500 hover:underline"
              onClick={() =>
                unlinkHabitFromGoal.mutate({ habitId: id, goalId: linkedGoal })
              }
            >
              Unlink
            </button>
          ) : (
            <LinkHabit id={id}></LinkHabit>
          )}
          <button
            className=" text-gray-500 hover:text-red-300"
            onClick={() => deleteHabit.mutate({ habitId: id })}
          >
            Delete
          </button>
          <button
            className=" text-gray-500 hover:text-red-300"
            onClick={() => openHabitPanel(id)}
          >
            Configure
          </button>
        </div>
      </div>
    </div>
  );
}

export function HabitCard({
  id,
  linkedGoal,
  score,
  description,
  metrics,
  scoringUnit,
}: ExpandedHabit & {
  weight: number | undefined;
  linkedGoal?: string | undefined;
  scoringUnit: ScoringFormat;
}) {
  const v = cva("rounded-lg p-6 shadow-md space-y-2", {
    variants: {
      variant: {
        freestanding: "bg-white",
        linked: "bg-white",
      },
    },
  });

  const linked = linkedGoal != undefined;
  const context = api.useContext();
  const editHabit = api.habits.editHabit.useMutation({
    onSuccess: () => {
      void context.goals.getAllGoals.invalidate();
    },
  });

  return (
    <div className="ml-2 hover:bg-gray-200">
      <div className="flex w-full flex-row justify-between">
        <div className="flex flex-grow flex-row items-center overflow-x-scroll scrollbar-none">
          {linked && (
            <CornerDownRight className="h-4 w-4 flex-shrink-0 stroke-black opacity-40"></CornerDownRight>
          )}
          <HabitIcon />
          <EditableField
            initialText={description}
            commit={(text) =>
              editHabit.mutate({ habitId: id, description: text })
            }
          ></EditableField>
        </div>

        <div className="flex flex-shrink-0 flex-row items-center space-x-2 justify-self-end whitespace-nowrap">
          <span
            className={classNames(
              "rounded-lg bg-gray-100 p-2 text-xl",
              textcolor(score)
            )}
          >
            {scoringUnit == ScoringFormat.Normalized
              ? min(1, score).toFixed(2)
              : (min(1, score) * 100).toFixed(2) + "%"}
          </span>
          <HabitSheet habitId={id} scoringUnit={scoringUnit}>
            <Cog6ToothIcon className="h-6 w-6 cursor-pointer opacity-40"></Cog6ToothIcon>
          </HabitSheet>
        </div>
      </div>
      {metrics.map((m) => (
        <LinkedMetric
          {...m}
          weight={0.5}
          key={m.id}
          offset={linkedGoal ? 2 : 1}
          scoringUnit={scoringUnit}
        ></LinkedMetric>
      ))}
    </div>
  );
}

export function CreateMetricLinkedToHabit({ habitId }: { habitId: string }) {
  const context = api.useContext();

  const createLinkedMetric = api.metrics.createMetric.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });

  const [active, setActive] = useState(false);

  return active ? (
    <CreateLinkedMetricInline
      createMetric={(prompt) => createLinkedMetric.mutate({ prompt, habitId })}
      closeEdit={() => setActive(false)}
    ></CreateLinkedMetricInline>
  ) : (
    <button
      className="mx-auto mt-2 block w-full rounded bg-gray-100 p-2 text-sm  text-gray-600 hover:bg-gray-200"
      onClick={() => setActive(true)}
    >
      + Create a new Linked Metric
    </button>
  );
}
