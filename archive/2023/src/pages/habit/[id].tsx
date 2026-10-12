import { useState } from "react";
import Calendar from "react-calendar";

import { EllipsisVerticalIcon } from "@heroicons/react/24/outline";
import type { HabitCompletion } from "@prisma/client";
import { ScoringFormat } from "@prisma/client";

import classNames from "classnames";
import {
  differenceInCalendarDays,
  endOfDay,
  format,
  startOfDay,
} from "date-fns";
import { useRouter } from "next/router";
import type { Value } from "react-calendar/dist/cjs/shared/types";
import { AiOutlineFire, AiTwotoneFire } from "react-icons/ai";
import { GiBackwardTime } from "react-icons/gi";
import { MdScoreboard } from "react-icons/md";
import { RxRocket } from "react-icons/rx";
import { Dropdown } from "../../components/createMenu";
import {
  DropDown,
  EditableField,
  EditableNumberField,
} from "../../components/inlineEdit";
import { textcolor } from "../../components/overview/lib";
import { LinkedMetric } from "../../components/overview/metrics";
import { CreateTag } from "../../components/overview/tags";
import { api } from "../../utils/api";

function isSameDay(a: Date, b: Date) {
  return differenceInCalendarDays(a, b) === 0;
}

function HabitsPage() {
  const router = useRouter();
  const id = router.query.id;
  console.log(router.query.id);

  if (typeof id != "string") return <p>Error</p>;
  else return <_HabitsPage id={id}></_HabitsPage>;
}

function ScorePill({
  scoringUnit,
  score,
}: {
  scoringUnit: ScoringFormat;
  score: number;
}) {
  return (
    <span
      className={classNames(
        "rounded-lg bg-gray-100 p-2 text-xl",
        textcolor(0.5)
      )}
    >
      {scoringUnit === ScoringFormat.Normalized
        ? score.toFixed(2)
        : (score * 100).toFixed(2) + "%"}
    </span>
  );
}

function min(a: number, b: number) {
  return a < b ? a : b;
}

function ActivityTable({
  date,
  completions,
}: {
  date: Date;
  completions: HabitCompletion[];
}) {
  const todaysCompletions = completions.filter(
    (it) => it.date > startOfDay(date) && it.date < endOfDay(date)
  );

  return (
    <div className="mt-6 text-center">
      <p className="font-semibold">Activity Log</p>
      {todaysCompletions.length == 0 && (
        <div className="items-center justify-center rounded-lg bg-white  p-4 text-center sm:flex">
          <div className="text-sm font-normal text-gray-500 dark:text-gray-300">
            No activity on {date.toLocaleDateString()}.
          </div>
        </div>
      )}

      {/* filter for date and show only those activities  */}
      <ol className="relative pt-1 dark:border-gray-700">
        {todaysCompletions.map((it) =>
          it.memo == null ? (
            <li className="mb-2" key={it.id}>
              <div className="items-center justify-between rounded-lg border border-gray-300 bg-white p-2 shadow-sm dark:border-gray-600 dark:bg-gray-700 sm:flex">
                <time className="mb-1 text-xs font-normal text-gray-400 sm:order-last sm:mb-0">
                  {it.date.toLocaleString()}
                </time>
                <div className="text-sm font-normal text-gray-500 dark:text-gray-300">
                  You completed this habit.
                </div>
              </div>
            </li>
          ) : (
            <li className="mb-2" key={it.id}>
              <div className="rounded-lg border border-gray-300 bg-white p-2 shadow-sm dark:border-gray-600 dark:bg-gray-700">
                <div className="mb-3 items-center justify-between sm:flex">
                  <time className="mb-1 text-xs font-normal text-gray-400 sm:order-last sm:mb-0">
                    {it.date?.toLocaleString()}
                  </time>
                  <div className="text-sm font-normal text-gray-500 dark:text-gray-300">
                    You completed this habit with a memo.
                  </div>
                </div>
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-3 text-left text-xs font-normal italic text-gray-500 dark:border-gray-500 dark:bg-gray-600 dark:text-gray-300">
                  {it.memo}
                </div>
              </div>
            </li>
          )
        )}
      </ol>
    </div>
  );
}

function _HabitsPage({ id }: { id: string }) {
  const context = api.useContext();
  const habitData = api.habits.getHabit.useQuery({ habitId: id });
  // highlight dates on calendar, 100 days
  const completionsData = api.habits.getCompletions.useQuery({
    habitId: id,
    timeHorizon: 100,
  });
  const goalQuery = api.habits.getGoals.useQuery({ habitId: id });
  const profileQuery = api.profile.getProfile.useQuery();
  const allItemQuery = api.goals.getAllGoals.useQuery();

  function tileClassName({ date, view }: { date: Date; view: string }) {
    if (
      view === "month" &&
      calendarData?.find((dDate) => isSameDay(dDate, date))
    ) {
      return "highlight";
    }
  }
  const editFrequency = api.habits.editFrequency.useMutation({
    onSuccess: () => {
      void context.habits.getHabit.invalidate();
    },
  });
  const editFrequencyHorizon = api.habits.editFrequencyHorizon.useMutation({
    onSuccess: () => {
      void context.habits.getHabit.invalidate();
    },
  });
  const [date, setDate] = useState(new Date());
  const mutation = api.habits.editHabit.useMutation({
    onSuccess: () => {
      void context.habits.getHabits.invalidate();
    },
  });
  const linkHabit = api.tags.linkHabit.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });
  const unlinkHabit = api.tags.unlinkHabit.useMutation({
    onSuccess() {
      void context.invalidate();
    },
  });

  if (
    !completionsData.data ||
    !goalQuery.data ||
    !habitData.data ||
    !profileQuery.data ||
    !allItemQuery.data
  ) {
    return (
      <div className="flex h-screen items-center justify-center">
        <RxRocket className="animate-spin text-2xl"></RxRocket>
      </div>
    );
  }

  const calendarData = completionsData.data.map((item) => item.date);
  const maxDate = calendarData.reduce(function (a, b) {
    return a > b ? a : b;
  });
  const goalsMap = allItemQuery.data.goalsMap;

  return (
    <>
      <div className="mx-auto max-w-screen-md px-4 py-8">
        {/* <p>{JSON.stringify(habitData)}</p> */}
        {/* <p>{JSON.stringify(completionsData)}</p> */}
        <h1 className="mb-4 flex flex-row justify-center text-center text-2xl font-bold">
          <EditableField
            initialText={habitData.data?.description}
            commit={(text) => {
              mutation.mutate({ habitId: id, description: text });
            }}
            className="ml-2 font-semibold"
          ></EditableField>
        </h1>
        <h2 className="mb-4 flex flex-row justify-center text-center text-lg">
          <EditableNumberField
            initial={habitData.data?.frequency}
            commit={(number) =>
              editFrequency.mutate({ habitId: id, frequency: number })
            }
            className="mr-1"
          ></EditableNumberField>
          <span className="text-md"> times per</span>
          <DropDown
            frequencyHorizon={habitData.data?.frequencyHorizon}
            commit={(freq) =>
              editFrequencyHorizon.mutate({
                habitId: id,
                frequencyHorizon: freq,
              })
            }
            className="lowercase"
          ></DropDown>
        </h2>

        <div className="mt-4 flex flex-wrap">
          <div className="mb-2 mt-2 flex w-full flex-auto justify-start gap-x-3 px-3 lg:w-6/12">
            {/* <p>Completion Weight: {habitData.data?.completionWeight}</p>
          <p>Metrics: {habitData.data?.metrics.length}</p> */}
            {habitData.data?.tags.map((it) => (
              <span
                key={it}
                className="mr-1 inline-block rounded bg-slate-200 px-2 py-1 text-xs font-semibold uppercase text-slate-600 last:mr-0"
              >
                {it}
              </span>
            ))}
            <span className="mr-1 inline-block rounded bg-slate-200 px-2 py-1 text-xs font-semibold uppercase text-slate-600 last:mr-0">
              <CreateTag
                commit={(name) =>
                  linkHabit.mutate({ habitId: id, tagName: name })
                }
              ></CreateTag>
            </span>
          </div>
        </div>

        <div className="mt-4 flex flex-wrap">
          <div className="mb-4 mt-2 w-full px-3 lg:w-6/12">
            <div className="relative mb-3 flex min-w-0 flex-col break-words rounded bg-white shadow-md">
              <div className="flex-auto p-4">
                <div className="flex flex-wrap">
                  <div className="relative w-full max-w-full flex-1 flex-grow pr-4">
                    <h5 className="text-blueGray-400 text-xs font-bold uppercase">
                      {" "}
                      Current Streak
                    </h5>
                    <span className="text-blueGray-700 text-xl font-semibold">
                      5
                    </span>
                  </div>
                  <div className="relative w-auto flex-initial pl-4">
                    <AiOutlineFire className="text-2xl"></AiOutlineFire>
                  </div>
                </div>
                <p className="text-blueGray-400 mt-4 text-sm">
                  <span className="whitespace-nowrap"> May 3</span>
                </p>
              </div>
            </div>
          </div>
          <div className="mb-4 mt-2 w-full px-3 lg:w-6/12">
            <div className="relative mb-3 flex min-w-0 flex-col break-words rounded bg-white shadow-md">
              <div className="flex-auto p-4">
                <div className="flex flex-wrap">
                  <div className="relative w-full max-w-full flex-1 flex-grow pr-4">
                    <h5 className="text-blueGray-400 text-xs font-bold uppercase">
                      {" "}
                      Longest Streak
                    </h5>
                    <span className="text-blueGray-700 text-xl font-semibold">
                      15
                    </span>
                  </div>
                  <div className="relative w-auto flex-initial pl-4">
                    <AiTwotoneFire className="text-2xl"></AiTwotoneFire>
                  </div>
                </div>
                <p className="text-blueGray-400 mt-4 text-sm">
                  <span className="whitespace-nowrap"> Apr 26 - May 3</span>
                </p>
              </div>
            </div>
          </div>
          <div className="mb-4 mt-2 w-full px-3 lg:w-6/12">
            <div className="relative mb-3 flex min-w-0 flex-col break-words rounded bg-white shadow-md">
              <div className="flex-auto p-4">
                <div className="flex flex-wrap">
                  <div className="relative w-full max-w-full flex-1 flex-grow pr-4">
                    <h5 className="text-blueGray-400 text-xs font-bold uppercase">
                      {" "}
                      Last Completed
                    </h5>
                    <span className="text-blueGray-700 text-xl font-semibold">
                      {maxDate?.toDateString()}
                    </span>
                  </div>
                  <div className="relative w-auto flex-initial pl-4">
                    <GiBackwardTime className="text-2xl"></GiBackwardTime>
                  </div>
                </div>
                <p className="text-blueGray-400 mt-4 text-sm">
                  <span className="whitespace-nowrap">
                    {" "}
                    {differenceInCalendarDays(new Date(), maxDate)} day ago
                  </span>
                </p>
              </div>
            </div>
          </div>
          <div className="mt-2 w-full px-3 lg:w-6/12">
            <div className="relative mb-6 flex min-w-0 flex-col break-words rounded bg-white shadow-lg">
              <div className="flex-auto p-4">
                <div className="flex flex-wrap">
                  <div className="relative w-full max-w-full flex-1 flex-grow pr-4">
                    <h5 className="text-blueGray-400 text-xs font-bold uppercase">
                      Current Score
                    </h5>
                    <span className="text-blueGray-700 text-xl font-semibold">
                      {min(100, (habitData.data?.score * 100)).toFixed(2)}%
                    </span>
                  </div>
                  <div className="relative w-auto flex-initial pl-4">
                    {/* <div className="text-white p-3 text-center inline-flex items-center justify-center w-12 h-12 shadow-lg rounded-full  bg-yellow-500">
              
            </div> */}
                    <MdScoreboard className="text-2xl"></MdScoreboard>
                  </div>
                </div>
                <p className="text-blueGray-400 mt-4 text-sm">
                  <span>
                    <RxRocket className="inline"></RxRocket>
                  </span>
                  <span className="whitespace-nowrap"> Since last period </span>
                  {/* <span className="mr-2 text-emerald-500">
                    <MdArrowUpward className="inline"></MdArrowUpward> 12%{" "}
                  </span>
                  <span className="whitespace-nowrap"> Since last period </span> */}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="flex flex-col items-center justify-center">
          <p className="mb-2 text-sm font-bold">{date.toDateString()}</p>
          <Calendar
            className="w-full md:w-auto"
            onChange={(date: Value) => {
              if (date instanceof Date) setDate(date);
            }}
            value={date}
            tileClassName={tileClassName}
            calendarType="US"
            maxDate={new Date()}
            minDate={habitData.data?.createdAt}
          />
        </div>
        <ActivityTable
          date={date}
          completions={completionsData.data}
        ></ActivityTable>

        <div className="mt-5 text-center">
          <h3 className="font-semibold">Linked Metrics</h3>
          {habitData.data.metrics.length == 0 && (
            <div className="items-center justify-center rounded-lg bg-white  p-4 text-center sm:flex">
              <div className="text-sm font-normal text-gray-500 dark:text-gray-300">
                No linked metrics.
              </div>
            </div>
          )}
          {habitData.data.metrics.map((metric) => {
            return (
              <div
                className="mt-2 flex w-full flex-row justify-between rounded-lg bg-gray-50 px-3 py-2"
                key={metric.id}
              >
                <LinkedMetric
                  {...metric}
                  weight={0.5}
                  key={metric.id}
                  offset={0}
                  scoringUnit={profileQuery.data.scoringUnit}
                ></LinkedMetric>
              </div>
            );
          })}
        </div>

        <div className="mt-5 text-center">
          <h3 className="font-semibold">Linked Goals</h3>
          {goalQuery.data.length == 0 && (
            <div className="items-center justify-center rounded-lg bg-white  p-4 text-center sm:flex">
              <div className="text-sm font-normal text-gray-500 dark:text-gray-300">
                No linked goals.
              </div>
            </div>
          )}
          {goalQuery.data.map((goal) => (
            <div
              className="mt-2 flex w-full flex-row justify-between rounded-lg bg-gray-50 px-3 py-2"
              key={goal.id}
            >
              <div className={"col-span-2 flex flex-row items-baseline gap-2"}>
                <span className="inline-block rounded-full bg-yellow-500 px-2 py-1 text-xs text-white">
                  Goal
                </span>
                <div className="">
                  <EditableField
                    initialText={goal.name}
                    commit={(text) => {
                      console.log(text);
                    }}
                    className="ml-2 font-semibold"
                  ></EditableField>
                </div>
              </div>
              <div className="flex flex-row items-center space-x-2 justify-self-end whitespace-nowrap">
                <ScorePill
                  scoringUnit={profileQuery.data.scoringUnit}
                  score={goalsMap.get(goal.id)?.goal.score ?? 0}
                ></ScorePill>
                <Dropdown
                  options={[
                    {
                      name: "Delete",
                      onClick: () => console.log("delete"),
                    },
                  ]}
                  trigger={
                    <EllipsisVerticalIcon className="h-6 w-6 fill-black stroke-black opacity-40"></EllipsisVerticalIcon>
                  }
                ></Dropdown>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-8 text-sm italic text-gray-500 ">
          <p>
            Date Created:{" "}
            {format(habitData.data?.createdAt ?? new Date(), "MM-dd-yyyy p")}
          </p>
          <p>
            Last Updated:{" "}
            {format(habitData.data?.updatedAt ?? new Date(), "MM-dd-yyyy p")}
          </p>
        </div>
        {/* <div className="mt-10 text-center">
          <button className=" text-red-500 text-md">Delete this Habit</button>
        </div> */}
      </div>
    </>
  );
}

export default HabitsPage;
