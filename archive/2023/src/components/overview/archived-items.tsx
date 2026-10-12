import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@radix-ui/react-accordion";
import { Loader } from "lucide-react";
import { TbSquareRoundedLetterM } from "react-icons/tb";
import { api } from "../../utils/api";
import { Button } from "../ui/button";
import { GoalIcon, HabitIcon } from "../ui/icons";

export function ArchivedItems() {
  const archivedItemsQuery = api.overview.getArchivedItems.useQuery();
  const context = api.useContext();
  const unarchiveGoal = api.goals.unarchive.useMutation({
    onSettled() {
      void context.goals.invalidate();
      void context.overview.getArchivedItems.invalidate();
    },
  });
  const unarchiveHabit = api.habits.unarchive.useMutation({
    onSettled() {
      void context.goals.invalidate();
      void context.habits.invalidate();
      void context.overview.getArchivedItems.invalidate();
    },
  });
  const unarchiveMetric = api.metrics.unarchive.useMutation({
    onSettled() {
      void context.goals.invalidate();
      void context.metrics.invalidate();
      void context.overview.getArchivedItems.invalidate();
    },
  });

  if (!archivedItemsQuery.data)
    return (
      <Accordion type="single" collapsible>
        <AccordionItem value="archived">
          <AccordionContent>
            <Loader></Loader>
          </AccordionContent>
        </AccordionItem>
      </Accordion>
    );
  const { goals, habits, metrics } = archivedItemsQuery.data;

  if (goals.length === 0 && habits.length === 0 && metrics.length === 0) {
    return <span className="text-gray-400">No archived items</span>;
  }

  return (
    <Accordion type="single" collapsible>
      <AccordionItem value="archived">
        <AccordionTrigger>Archived Items</AccordionTrigger>
        <AccordionContent>
          {goals.map((goal) => (
            <div
              className="flex w-full flex-row items-center justify-between gap-2"
              key={goal.id}
            >
              <div className="col-span-1 flex w-auto flex-row items-center gap-2">
                <GoalIcon />
                {goal.name}
              </div>
              <div className="flex flex-row items-center space-x-2 justify-self-end whitespace-nowrap">
                <Button
                  variant="secondary"
                  onClick={() => unarchiveGoal.mutate({ goalId: goal.id })}
                >
                  Unarchive
                </Button>
              </div>
            </div>
          ))}
          {habits.map((habit) => (
            <div
              className="flex w-full flex-row items-center justify-between gap-2"
              key={habit.id}
            >
              <div className="col-span-1 flex w-auto flex-row items-center gap-2">
                <HabitIcon />
                {habit.description}
              </div>
              <div className="flex flex-row items-center space-x-2 justify-self-end whitespace-nowrap">
                <Button
                  variant="secondary"
                  onClick={() => unarchiveHabit.mutate({ habitId: habit.id })}
                >
                  Unarchive
                </Button>
              </div>
            </div>
          ))}
          {metrics.map((metric) => (
            <div
              className="flex w-full flex-row items-center justify-between gap-2"
              key={metric.id}
            >
              <div className="col-span-1 flex w-auto flex-row items-center gap-2">
                <TbSquareRoundedLetterM className="text-2xl text-yellow-500"></TbSquareRoundedLetterM>
                {metric.prompt}
              </div>
              <div className="flex flex-row items-center space-x-2 justify-self-end whitespace-nowrap">
                <Button
                  variant="secondary"
                  onClick={() =>
                    unarchiveMetric.mutate({ metricId: metric.id })
                  }
                >
                  Unarchive
                </Button>
              </div>
            </div>
          ))}
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
}
