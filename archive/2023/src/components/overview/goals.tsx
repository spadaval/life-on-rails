import { Cog6ToothIcon } from "@heroicons/react/24/outline";
import type { Metric } from "@prisma/client";
import { ScoringFormat } from "@prisma/client";
import classNames from "classnames";
import { EditableField } from "../../components/inlineEdit";
import { HabitCard } from "../../components/overview/habits";
import type {
  ExpandedGoal,
  ExpandedHabit,
  ExpandedMetric,
} from "../../server/queries";
import { api } from "../../utils/api";
import { GoalIcon } from "../ui/icons";
import { GoalSheet } from "./goal-panel";
import { textcolor } from "./lib";
import { LinkedMetric } from "./metrics";

function min(a: number, b: number) {
  return a < b ? a : b;
}

export function GoalCard({
  id,
  name,
  score,
  habits,
  metrics,
  scoringUnit,
}: ExpandedGoal & {
  score: number;
  habits: (ExpandedHabit & {
    completions: number;
    score: number;
    metrics: ExpandedMetric[];
  })[];
  metrics: (Metric & { score: number })[];
  scoringUnit: ScoringFormat;
}) {
  const context = api.useContext();
  const editGoal = api.goals.editGoal.useMutation({
    onSettled: () => {
      void context.goals.getAllGoals.invalidate();
    },
  });
  return (
    <div className="py-4 hover:bg-gray-100">
      <div className="flex w-full justify-between gap-2 content-baseline flex-grow">
        <div className="flex w-auto flex-row items-center gap-2 overflow-x-hidden flex-grow">
          <GoalIcon />
          <EditableField
            initialText={name}
            commit={(name) => editGoal.mutate({ goalId: id, name })}
            className="text-yellow-800"
          ></EditableField>
        </div>
        <div className="flex flex-shrink-0 flex-row items-center space-x-2 justify-self-end whitespace-nowrap">
          <span
            className={classNames(
              "h-fit w-fit rounded-lg bg-gray-100 p-2 text-xl font-semibold",
              textcolor(score)
            )}
          >
            {scoringUnit == ScoringFormat.Normalized
              ? min(1, score).toFixed(2)
              : (min(1, score) * 100).toFixed(2) + "%"}
          </span>
          <GoalSheet goalId={id} score={score} scoringUnit={scoringUnit}>
            <Cog6ToothIcon className="h-6 w-6 cursor-pointer opacity-40"></Cog6ToothIcon>
          </GoalSheet>
        </div>
      </div>
      {habits.map((habit) => (
        <HabitCard
          {...habit}
          weight={0.4}
          linkedGoal={id}
          key={habit.id}
          scoringUnit={scoringUnit}
        ></HabitCard>
      ))}
      {metrics.map((m) => (
        <LinkedMetric
          {...m}
          weight={0.4}
          key={m.id}
          offset={1}
          scoringUnit={scoringUnit}
        ></LinkedMetric>
      ))}
    </div>
  );
}
