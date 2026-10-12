/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable @typescript-eslint/no-misused-promises */
import type { ScoringFormat } from "@prisma/client";
import { SlidersHorizontal } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { CreateMenu } from "../components/createMenu";
import { ArchivedItems } from "../components/overview/archived-items";
import type { Filters } from "../components/overview/configure";
import {
  ConfigureOverview,
  filteredData,
} from "../components/overview/configure";
import { GoalCard } from "../components/overview/goals";
import { HabitCard } from "../components/overview/habits";
import { LinkedMetric } from "../components/overview/metrics";
import { Loader } from "../components/ui/loader";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "../components/ui/popover";
import { TemplatesList } from "../pages/templates";
import type { RouterOutputs } from "../utils/api";
import { api } from "../utils/api";
import { HelpIcon } from "../components/ui/help-icon";

function Header({
  filters,
  setFilters,
}: {
  filters: Filters | undefined;
  setFilters: (filters: Filters | undefined) => void;
}) {
  return (
    <>
      <div className="mb-2 flex w-full items-center justify-between">
        <div>
          <span className="flex flex-row items-center gap-2">
            <h1 className="ml-2 text-xl font-semibold uppercase text-gray-800">
              Overview
            </h1>
            <HelpIcon>
              <p className="mb-2 px-2 text-xs italic text-slate-500">
                All scores are based off the preference in your{" "}
                <Link href="/profile" className="underline">
                  user settings.
                </Link>
              </p>
            </HelpIcon>
          </span>
        </div>
        <div className="flex">
          <CreateMenu></CreateMenu>
          <button className="rounded px-2 py-2 hover:bg-gray-200">
            <Popover>
              <PopoverTrigger asChild>
                <SlidersHorizontal className="fill-gray-500 stroke-gray-500" />
              </PopoverTrigger>
              <PopoverContent className="w-80">
                <ConfigureOverview
                  filters={filters}
                  setFilters={setFilters}
                ></ConfigureOverview>
              </PopoverContent>
            </Popover>
          </button>
        </div>
      </div>
    </>
  );
}

function isEmpty(
  data: Omit<RouterOutputs["goals"]["getAllGoals"], "goalsMap">
) {
  return (
    data.goals.length == 0 &&
    data.habits.length == 0 &&
    data.metrics.length == 0
  );
}

function OverviewContent(
  props: { scoringUnit: ScoringFormat } & Omit<
    RouterOutputs["goals"]["getAllGoals"],
    "goalsMap"
  >
) {
  const hasUnlinkedItems = props.habits.length > 0 || props.metrics.length > 0;
  return (
    <div className="mt-3 max-sm:max-w-screen-sm md:mx-auto">
      <h1 className="under col-span-full my-10 ml-2 text-lg font-semibold uppercase text-slate-600">
        Linked Items
        <hr />
      </h1>

      {props.goals.map((goal) => (
        <GoalCard
          {...goal.goal}
          habits={goal.habits}
          metrics={goal.metrics}
          key={goal.goal.id}
          scoringUnit={props.scoringUnit}
        ></GoalCard>
      ))}
      {hasUnlinkedItems ? (
        <h1 className="under col-span-full my-10 ml-2 text-lg font-semibold uppercase text-slate-600">
          Unlinked Items
          <hr />
        </h1>
      ) : (
        <span className="my-10 text-gray-400">No unlinked items</span>
      )}
      {props.habits.map((habit) => (
        <HabitCard
          {...habit}
          weight={0.5}
          key={habit.id}
          scoringUnit={props.scoringUnit}
        ></HabitCard>
      ))}
      {props.metrics.map((metric) => {
        return (
          <LinkedMetric
            {...metric}
            weight={0.5}
            key={metric.id}
            offset={0}
            scoringUnit={props.scoringUnit}
          ></LinkedMetric>
        );
      })}
    </div>
  );
}

function OverviewPage() {
  const [filters, setFilters] = useState<Filters>();
  const goalsQuery = api.goals.getAllGoals.useQuery();
  const profileQuery = api.profile.getProfile.useQuery();
  if (goalsQuery.isLoading || profileQuery.isLoading) return <Loader></Loader>;
  if (goalsQuery.isError || profileQuery.isError) return <p>Query error</p>;

  const data = filteredData(goalsQuery.data, filters);
  const user = profileQuery.data;

  return (
    <div className="">
      <div className="mb-10 scrollbar-none">
        <Header filters={filters} setFilters={setFilters}></Header>
        {isEmpty(data) ? (
          <>
            <span className="text-md w-full text-center">
              You have no items! If you're overwhelmed, try starting with one of
              these templates.
            </span>
            <TemplatesList />
          </>
        ) : (
          <OverviewContent
            goals={data.goals}
            metrics={data.metrics}
            habits={data.habits}
            scoringUnit={user.scoringUnit}
          ></OverviewContent>
        )}
      </div>
      <div className="w-full">
        <ArchivedItems></ArchivedItems>
        <HelpIcon>
          Archive items you no longer want to track. Archived items will no
          longer affect any scores or show up on the dashboard. All of your
          archived items will show up here. You can unarchive them at any time.
        </HelpIcon>
      </div>
    </div>
  );
}
export default OverviewPage;
