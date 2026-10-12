import type { Habit } from "@prisma/client";
import { ScoringFormat } from "@prisma/client";
import { ArrowDownIcon, ArrowUpIcon } from "lucide-react";
import type { ReactNode } from "react";
import { useState } from "react";
import type { ExpandedHabit, ExpandedMetric } from "../../server/queries";
import { api } from "../../utils/api";
import {
  CreateLinkedHabitInline,
  CreateLinkedMetricInline,
  EditableField,
} from "../inlineEdit";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "../ui/accordion";
import { ActionButton, Button } from "../ui/button";
import { HelpIcon } from "../ui/help-icon";
import { HabitIcon, MetricIcon } from "../ui/icons";
import { Label } from "../ui/label";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "../ui/sheet";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../ui/tooltip";
import { useDebouncedState } from "./lib";
import { GoalTagList } from "./tags";

function min(a: number, b: number) {
  return a < b ? a : b;
}

export function CreateMetricLinkedToGoal({ goalId }: { goalId: string }) {
  const context = api.useContext();

  const createLinkedMetric = api.metrics.createMetric.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });

  const [active, setActive] = useState(false);

  return active ? (
    <CreateLinkedMetricInline
      createMetric={(prompt) => createLinkedMetric.mutate({ prompt, goalId })}
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

function MetricsSection({ goalId }: { goalId: string }) {
  const context = api.useContext();
  const deleteMetric = api.metrics.deleteMetric.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });
  const metricsQuery = api.goals.getMetrics.useQuery({ goalId: goalId });

  if (metricsQuery.isError) {
    return <p>ERROR</p>;
  }
  if (metricsQuery.isLoading) {
    return <p>LOADING</p>;
  }
  console.log(metricsQuery.data);
  return (
    <>
      <div className="mb-4 grid w-full grid-cols-2 items-baseline justify-between gap-2">
        {metricsQuery.data.length == 0 && (
          <div className="col-span-full w-full bg-gray-100 px-4 py-2">
            No linked metrics
          </div>
        )}
        {metricsQuery.data.map((metric) => {
          return (
            <>
              <div>
                <p className="text-lg">{metric.prompt}</p>
                <p className="text-sm text-gray-300">
                  {/* TODO remove when superjson works again */}
                  Created: {new Date(metric.createdAt).toLocaleDateString()}
                </p>
              </div>
              <div className="flex-shrink space-x-2 text-right">
                <Button
                  onClick={() =>
                    deleteMetric.mutate({
                      metricId: metric.id,
                    })
                  }
                  variant="outline"
                >
                  Delete
                </Button>
              </div>
            </>
          );
        })}
      </div>
      <CreateMetricLinkedToGoal goalId={goalId}></CreateMetricLinkedToGoal>
    </>
  );
}

/* export const InlineEdit = ({
  placeholder,
  initialText,
  commit,
}: {
  placeholder: string;
  initialText: string;
  commit: (text: string) => void;
}) => {
  const [isActive, setActive] = useState<boolean>(false);
  const [text, setText] = useState<string>(initialText);
  if (!isActive) {
    return (
      <div className="bg-slate-100">
        <span onClick={() => setActive(true)}>
          <PlusIcon className="h-4 w-4"></PlusIcon>
          {placeholder}
        </span>
      </div>
    );
  } else
    return (
      <div className="bg-slate-100">
        <input
          autoFocus
          type="text"
          value={text}
          onBlur={() => setActive(false)}
          onChange={(event) => {
            setText(event.target.value);
          }}
          onKeyDown={(event) => {
            if (event.key == "Enter") {
              console.log(text);
              commit(text);
              setActive(false);
            } else if (event.key == "Escape") {
              setText("");
              setActive(false);
            }
          }}
        ></input>
      </div>
    );
};

function InlineCreateHabit({ goalId }: { goalId: string }) {
  const context = api.useContext();
  const addHabit = api.create.createLinkedHabit.useMutation({
    onSuccess() {
      void context.invalidate();
      void context.journal.getHabits.invalidate();
    },
  });

  return (
    <InlineEdit
      placeholder="New habit"
      initialText=""
      commit={(text: string) =>
        addHabit.mutate({ description: text, goalId: goalId })
      }
    />
  );
} */

function CreateHabitLinkedToGoal({ goalId }: { goalId: string }) {
  const context = api.useContext();

  const createHabit = api.habits.createHabit.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });

  const [active, setActive] = useState(false);

  return active ? (
    <CreateLinkedHabitInline
      createHabit={(description) =>
        createHabit.mutate({ description: description, goalId })
      }
      closeEdit={() => setActive(false)}
    ></CreateLinkedHabitInline>
  ) : (
    <button
      className="mx-auto mt-2 block w-full rounded bg-gray-100 p-2 text-sm  text-gray-600 hover:bg-gray-200"
      onClick={() => setActive(true)}
    >
      + Create a new Linked Habit
    </button>
  );
}

function HabitsSection({
  goalId,
  habits,
}: {
  goalId: string;
  habits: Habit[];
}) {
  const context = api.useContext();
  const deleteHabit = api.habits.deleteHabit.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });

  return (
    <>
      <div className="mb-4 grid w-full grid-cols-2 items-baseline justify-between gap-2">
        {habits.length == 0 && (
          <div className="col-span-full w-full bg-gray-100 px-4 py-2">
            No linked habits
          </div>
        )}
        {habits.map((habit) => {
          return (
            <>
              <div>
                <p className="text-lg">{habit.description}</p>
                <p className="text-sm text-gray-300">
                  Created: {new Date(habit.createdAt).toUTCString()}
                </p>
              </div>
              <div className="flex-shrink space-x-2 text-right">
                <Button
                  onClick={() =>
                    deleteHabit.mutate({
                      habitId: habit.id,
                    })
                  }
                  variant="outline"
                >
                  Delete
                </Button>
              </div>
            </>
          );
        })}
      </div>
      <CreateHabitLinkedToGoal goalId={goalId}></CreateHabitLinkedToGoal>
    </>
  );
}

function WeightBar({
  children,
  weight,
  totalWeight,
  color,
  updateWeight,
}: {
  children: ReactNode;
  weight: number;
  totalWeight: number;
  color: number;
  updateWeight: (weight: number) => void;
}) {
  return (
    <Tooltip delayDuration={0}>
      <div
        className="inline-flex h-full flex-col items-center gap-3 text-center"
        style={{
          width: `${(weight / totalWeight) * 100}%`,
        }}
      >
        <span className="flex flex-row items-center justify-center gap-2 overflow-x-hidden whitespace-nowrap">
          {children}
        </span>
        <TooltipTrigger asChild>
          <div
            className="h-5 w-full animate-[scale] animate-in animate-out hover:scale-110 hover:scale-y-150"
            style={{
              backgroundColor: `hsl(0deg 0% ${40 + ((color * 15) % 60)}%)`,
            }}
          ></div>
        </TooltipTrigger>
        <p> Weight: {weight}</p>
        <span>
          <Button
            variant="ghost"
            disabled={weight <= 1}
            onClick={() => updateWeight(weight - 1)}
          >
            <ArrowDownIcon></ArrowDownIcon>
          </Button>
          <Button variant="ghost" onClick={() => updateWeight(weight + 1)}>
            <ArrowUpIcon></ArrowUpIcon>
          </Button>
        </span>
      </div>
      <TooltipContent>
        {((weight / totalWeight) * 100).toFixed(2)}%
      </TooltipContent>
    </Tooltip>
  );
}

function ScoringSection({
  goalId,
  habits,
  metrics,
}: {
  goalId: string;
  habits: (ExpandedHabit & { weight: number })[];
  metrics: (ExpandedMetric & { weight: number })[];
}) {
  const context = api.useContext();
  const updateHabitWeight = api.goals.updateHabitWeight.useMutation({
    onSuccess() {
      void context.goals.getWeights.invalidate();
    },
  });
  const updateMetricWeight = api.goals.updateMetricWeight.useMutation({
    onSuccess() {
      void context.goals.getWeights.invalidate();
    },
  });

  const initialHabitWeights = habits.map((it) => it.weight);
  const initialMetricWeights = metrics.map((it) => it.weight);

  const syncHabitWeights = (weights: number[], oldWeights: number[]) => {
    weights.forEach((w, i) => {
      if (w != oldWeights[i])
        updateHabitWeight.mutate({
          goalId: goalId,
          habitId: habits[i]!.id,
          weight: w,
        });
    });
  };
  const syncMetricWeights = (weights: number[], oldWeights: number[]) => {
    weights.forEach((w, i) => {
      if (w != oldWeights[i])
        updateMetricWeight.mutate({
          goalId: goalId,
          metricId: metrics[i]!.id,
          weight: w,
        });
    });
  };

  const { state: habitWeights, setState: setHabitWeights } = useDebouncedState(
    initialHabitWeights,
    500,
    syncHabitWeights
  );

  const { state: metricWeights, setState: setMetricWeights } =
    useDebouncedState(initialMetricWeights, 500, syncMetricWeights);

  const totalWeight =
    habitWeights.reduce((acc, cur) => acc + cur, 0) +
    metricWeights.reduce((acc, cur) => acc + cur, 0);

  if (totalWeight == 0) {
    return (
      <div className="h-10 w-full bg-gray-100">
        <p>
          No linked habits or metrics! Goals need linked items to calculate
          scores.
        </p>
      </div>
    );
  }
  return (
    <TooltipProvider>
      <div className="relative w-full">
        {habits.map((habit, i) => (
          <WeightBar
            key={habit.id}
            weight={habitWeights[i]!}
            totalWeight={totalWeight}
            color={i}
            updateWeight={(weight) => {
              habitWeights[i] = weight;
              setHabitWeights([...habitWeights]);
            }}
          >
            <HabitIcon />
            {habit.description}
          </WeightBar>
        ))}
        {metrics.map((metric, i) => (
          <WeightBar
            key={metric.id}
            weight={metric.weight}
            totalWeight={totalWeight}
            color={i + 10}
            updateWeight={(weight) => {
              metricWeights[i] = weight;
              setMetricWeights([...metricWeights]);
            }}
          >
            <MetricIcon></MetricIcon>
            {metric.prompt}
          </WeightBar>
        ))}
      </div>
    </TooltipProvider>
  );
}

function GoalPanel({
  goalId,
  score,
  scoringUnit,
}: {
  goalId: string;
  score: number;
  scoringUnit: ScoringFormat;
}) {
  const context = api.useContext();

  const deleteGoal = api.goals.deleteGoal.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });
  const editGoal = api.goals.editGoal.useMutation({
    onSettled: () => {
      void context.goals.getAllGoals.invalidate();
    },
  });

  const archiveGoal = api.goals.archive.useMutation({
    onSettled: () => {
      void context.goals.getAllGoals.invalidate();
    },
  });

  const goalQuery = api.goals.getGoal.useQuery({ id: goalId });
  const goalWeightsQuery = api.goals.getWeights.useQuery({ goalId: goalId });

  const data = goalQuery.data;
  if (goalQuery.isError || goalWeightsQuery.isError) {
    return <p>Error</p>;
  }
  if (
    goalQuery.isLoading ||
    !data ||
    !goalWeightsQuery.data ||
    goalWeightsQuery.isLoading
  ) {
    return <p>LOADING</p>;
  }
  const habitWeights = new Map(
    goalWeightsQuery.data.habits.map((habit) => [habit.habitId, habit.weight])
  );
  const metricWeights = new Map(
    goalWeightsQuery.data.metrics.map((metric) => [
      metric.metricId,
      metric.weight,
    ])
  );

  return (
    <div className="relative h-full">
      <div className="flex h-full flex-col gap-2">
        <SheetHeader>
          <SheetTitle>
            <div className="mb-1 flex flex-row justify-between">
              <div className="flex items-center justify-between text-xl">
                <span className="inline-block h-fit w-fit rounded-full bg-yellow-500 px-2 py-1 text-xs text-white">
                  Goal
                </span>
                <EditableField
                  initialText={data.name}
                  commit={(name) => editGoal.mutate({ goalId: data.id, name })}
                ></EditableField>
              </div>
              <div className="flex flex-row items-center space-x-2 whitespace-nowrap">
                <span className="h-fit w-fit rounded-lg bg-gray-100 p-2 text-xl text-yellow-500">
                  {scoringUnit == ScoringFormat.Normalized
                    ? min(1, score).toFixed(2)
                    : (min(1, score) * 100).toFixed(2) + "%"}
                </span>
              </div>
            </div>

            <GoalTagList goalId={goalId}></GoalTagList>
          </SheetTitle>
        </SheetHeader>
        <Accordion
          className="flex-grow overflow-scroll scrollbar-none"
          type="multiple"
        >
          <AccordionItem value="scoring">
            <AccordionTrigger>
              <span className="flex flex-row items-center justify-between gap-2">
                Scoring
                <HelpIcon>
                  Goal scores are calculated from a weighted average of the
                  scores of linked metrics and habits. Weights are unitless.
                </HelpIcon>
              </span>
            </AccordionTrigger>
            <AccordionContent>
              <ScoringSection
                goalId={goalId}
                habits={goalQuery.data.habits.map((it) => ({
                  ...it,
                  weight: habitWeights?.get(it.id) ?? -1,
                }))}
                metrics={goalQuery.data.metrics.map((it) => ({
                  ...it,
                  weight: metricWeights?.get(it.id) ?? -1,
                }))}
              ></ScoringSection>
            </AccordionContent>
          </AccordionItem>
          {/* <AccordionItem value="history">
                <AccordionTrigger>History</AccordionTrigger>
                <AccordionContent>
                  <HistorySection habitId={habitId}></HistorySection>
                </AccordionContent>
              </AccordionItem> */}
          <AccordionItem value="habits">
            <AccordionTrigger>Habits</AccordionTrigger>
            <AccordionContent>
              <HabitsSection
                habits={data.habits}
                goalId={goalId}
              ></HabitsSection>
            </AccordionContent>
          </AccordionItem>
          <AccordionItem value="metrics">
            <AccordionTrigger>Metrics</AccordionTrigger>
            <AccordionContent>
              <MetricsSection goalId={goalId}></MetricsSection>
            </AccordionContent>
          </AccordionItem>
        </Accordion>
        <div className="w-full space-x-2 bg-gray-100 px-4 py-2 text-right">
          <Button
            className="cursor-pointer"
            variant="default"
            onClick={() => archiveGoal.mutate({ goalId })}
          >
            <Label>Archive</Label>
          </Button>

          <ActionButton
            variant="destructive"
            idle="Delete"
            action={() => deleteGoal.mutateAsync({ id: goalId })}
          ></ActionButton>
        </div>
      </div>
    </div>
  );
}

export function GoalSheet({
  goalId,
  children,
  score,
  scoringUnit,
}: {
  goalId: string;
  children: ReactNode;
  score: number;
  scoringUnit: ScoringFormat;
}) {
  return (
    <Sheet>
      <SheetTrigger>{children}</SheetTrigger>
      <SheetContent
        position="right"
        size="lg"
        className="overflow-scroll max-md:w-full"
      >
        <GoalPanel
          goalId={goalId}
          score={score}
          scoringUnit={scoringUnit}
        ></GoalPanel>
      </SheetContent>
    </Sheet>
  );
}
