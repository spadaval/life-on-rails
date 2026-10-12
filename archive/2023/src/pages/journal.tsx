import { useState } from "react";
import TimePicker from "../components/time-picker";

import type { ScoringFormat } from "@prisma/client";

import { api } from "../utils/api";

// fixes zoomed in icons
import { CreateMenu } from "../components/createMenu";
import { JournalTable } from "../components/journal/table";
import { Loader } from "../components/ui/loader";
import type { ExpandedHabit, ExpandedMetric } from "../server/queries";

interface JournalProps {
  habits: ExpandedHabit[];
  date: Date;
  setDate: (date: Date) => void;
  metrics: ExpandedMetric[];
  scoringUnit?: ScoringFormat;
}

// https://tailwindcomponents.com/component/free-tailwind-css-advance-table-component
// function dropdownFunction(element) {
//     var dropdowns = document.getElementsByClassName("dropdown-content");
//     var i;
//     let list = element.parentElement.parentElement.getElementsByClassName("dropdown-content")[0];
//     list.classList.add("target");
//     for (i = 0; i < dropdowns.length; i++) {
//         if (!dropdowns[i].classList.contains("target")) {
//             dropdowns[i].classList.add("hidden");
//         }
//     }
//     list.classList.toggle("hidden");
// }

function Journal({
  date,
  setDate,
  habits,
  metrics,
  scoringUnit,
}: JournalProps) {
  return (
    <>
      <div className="flex max-w-5xl justify-center">
        <div className="w-full">
          <div className="px-4 py-4 md:px-10 md:py-7">
            <div className="flex items-center justify-between">
              <p
                tabIndex={0}
                className="text-base font-bold leading-normal text-gray-800 focus:outline-none sm:text-lg md:text-xl lg:text-2xl"
              >
                Daily Journal
              </p>
              <CreateMenu className="mt-4 inline-flex items-start justify-start rounded bg-gray-200 px-6 py-2 hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2 sm:mt-0"></CreateMenu>
            </div>
            <div className="flex w-auto items-center justify-between">
              <TimePicker date={date} setDate={setDate}></TimePicker>
            </div>
          </div>
          <div className="bg-white px-4 py-3 md:px-6 md:py-4 xl:px-10">
            <div className="items-center justify-between sm:flex">
              Pending Tasks
            </div>
            <div className="mt-7">
              <JournalTable
                habits={habits}
                metrics={metrics}
                date={date}
                scoringUnit={scoringUnit ?? "Normalized"}
              ></JournalTable>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

const today = new Date();

const JournalPage = () => {
  const [date, setDate] = useState(today);

  const habits = api.habits.getHabits.useQuery({ date });
  const metrics = api.metrics.getMetrics.useQuery({ date });
  const profile = api.profile.getProfile.useQuery();

  if (habits.isLoading || metrics.isLoading) return <Loader></Loader>;
  if (habits.isError || metrics.isError) return <p>Query error</p>;
  const habitsData = habits.data;
  const metricsData = metrics.data; //query.data.subjectives.map((subjective) => ({ editable: true, ...subjective }));
  const user = profile.data;
  return (
    <Journal
      habits={habitsData}
      date={date}
      setDate={setDate}
      metrics={metricsData.filter((it) => it.linkedHabits.length == 0)}
      scoringUnit={user?.scoringUnit}
    ></Journal>
  );
};

export default JournalPage;
