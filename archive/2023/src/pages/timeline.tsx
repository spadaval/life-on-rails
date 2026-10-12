import {
  TbSquareRoundedLetterG,
  TbSquareRoundedLetterH,
  TbSquareRoundedLetterM,
} from "react-icons/tb";
import { api } from "../utils/api";
import { Loader } from "../components/ui/loader";
import { differenceInCalendarDays, format } from "date-fns";
import type {
  GoalAddedEvent,
  GoalArchivedEvent,
  HabitAddedEvent,
  HabitArchivedEvent,
  TimelineEvent,
} from "../server/api/types";
import { TimelineEventType } from "../server/api/types";
import { HabitIcon, MetricIcon } from "../components/ui/icons";

const GoalArchived = ({ event }: { event: GoalArchivedEvent }) => (
  <div>
    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
      You archived{" "}
      <div className="inline-block">
        <TbSquareRoundedLetterG className="mb-1 inline text-2xl text-yellow-500"></TbSquareRoundedLetterG>
        <h3 className="inline text-yellow-800">{event.goal.name}</h3>
      </div>
      .
    </h3>
    <p className="mb text-sm font-normal text-gray-500 dark:text-gray-400">
      Created on {event.date.toUTCString()} (active for{" "}
      {differenceInCalendarDays(event.date, event.created)} days)
    </p>
    {/* {JSON.stringify(event.habits)} */}
    <div className="flex flex-col text-sm font-normal text-gray-500 dark:text-gray-400">
      <div className="flex flex-col">
        <div className="flex flex-row p-2">
          <TbSquareRoundedLetterH className="text-xl text-blue-500"></TbSquareRoundedLetterH>
          <p className="">This is a habit</p>
        </div>
        <div className="flex flex-row p-2">
          <TbSquareRoundedLetterM className="ml-4 text-xl text-purple-500"></TbSquareRoundedLetterM>
          <p className="">This is a linked Metric</p>
        </div>
      </div>
      <div className="flex p-2">
        <TbSquareRoundedLetterH className="text-xl text-blue-500"></TbSquareRoundedLetterH>
        <p className="">This is a habit</p>
      </div>
    </div>
  </div>
);

const GoalAdded = ({ event }: { event: GoalAddedEvent }) => (
  <div>
    <time className="mb text-sm font-normal text-gray-500 dark:text-gray-400">
      {format(event.date, "h:mm:ss a")}
    </time>
    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
      You created{" "}
      <div className="inline-block">
        <TbSquareRoundedLetterG className="mb-1 inline text-2xl text-yellow-500"></TbSquareRoundedLetterG>
        <h3 className="inline text-yellow-800">{event.goal.name}</h3>
      </div>
      .
    </h3>
    {/* <div className="flex flex-col text-sm font-normal text-gray-500 dark:text-gray-400">
      <div className="flex flex-col">
        <div className="flex flex-row p-2">
          <TbSquareRoundedLetterH className="text-xl text-blue-500"></TbSquareRoundedLetterH>
          <p className="">This is a habit</p>
        </div>
        <div className="flex flex-row p-2">
          <TbSquareRoundedLetterM className="ml-4 text-xl text-purple-500"></TbSquareRoundedLetterM>
          <p className="">This is a linked Metric</p>
        </div>
      </div> */}
    {event.habits.map((event, i) => {
      return (
        <div key={i} className="flex flex-row p-2">
          <HabitIcon />
          <p className="">{event.description}</p>
        </div>
      );
    })}
    {event.metrics.map((event, i) => {
      return (
        <div key={i} className="flex p-2">
          <MetricIcon />
          <p className="">{event.prompt}</p>
        </div>
      );
    })}
  </div>
  // </div>
);

const HabitAdded = ({ event }: { event: HabitAddedEvent }) => (
  <div>
    <time className="mb text-sm font-normal text-gray-500 dark:text-gray-400">
      {format(event.date, "h:mm:ss a")}
    </time>

    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
      You created{" "}
      <div className="inline-block">
        <HabitIcon />
        <h3 className="inline">{event.habit.description}</h3>
      </div>
      .
    </h3>
    <p className="text-sm font-normal text-gray-500 dark:text-gray-400">
      You completed this habit {(Math.random() * 30).toFixed(0)} times.
    </p>
  </div>
);

const HabitArchived = ({ event }: { event: HabitArchivedEvent }) => (
  <div>
    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
      You archived{" "}
      <div className="inline-block">
        <HabitIcon />
        <h3 className="inline">{event.habit.description}</h3>
      </div>
      .
    </h3>
    <p className="text-sm font-normal text-gray-500 dark:text-gray-400">
      Created {event.date.toUTCString()} (active for{" "}
      {differenceInCalendarDays(event.date, event.created)} days)
    </p>
  </div>
);

function Group({ date, events }: { date: Date; events: TimelineEvent[] }) {
  return (
    <li className="mb-10 ml-4">
      <div className="absolute -left-1.5 mt-1.5 h-3 w-3 rounded-full border border-white bg-gray-200 dark:border-gray-900 dark:bg-gray-700"></div>
      <time className="mb-1 text-sm font-normal leading-none text-gray-400 dark:text-gray-500">
        {date.toDateString()}
      </time>
      <div className="space-y-4">
        {events.map((event, i) => {
          if (event.eventType === TimelineEventType.GOAL_ARCHIVED)
            return <GoalArchived key={i} event={event}></GoalArchived>;
          if (event.eventType === TimelineEventType.GOAL_ADDED)
            return <GoalAdded key={i} event={event}></GoalAdded>;
          if (event.eventType === TimelineEventType.HABIT_ADDED)
            return <HabitAdded key={i} event={event}></HabitAdded>;
          if (event.eventType === TimelineEventType.HABIT_ARCHIVED)
            return <HabitArchived key={i} event={event}></HabitArchived>;
        })}
      </div>
    </li>
  );
}

function Timeline() {
  const timelineQuery = api.timeline.getTimeline.useQuery();

  if (!timelineQuery.data) {
    return <Loader></Loader>;
  }
  const timeline = timelineQuery.data;

  return (
    <>
      <section className="bg-white dark:bg-gray-900">
        <div className="mx-auto px-6 py-10">
          <h1 className="mb-3 text-2xl font-semibold text-gray-800 dark:text-white lg:text-3xl">
            Personal Timeline
          </h1>
          <p>See what you've achieved to date!</p>
          <hr className="my-6 border-gray-200 dark:border-gray-700" />
          <ol className="relative border-l border-gray-200 dark:border-gray-700">
            {timeline.map(([date, events], i) => {
              return (
                <Group key={i} date={new Date(date)} events={events}></Group>
              );
            })}
          </ol>
        </div>
      </section>
    </>
  );
}

export default Timeline;
