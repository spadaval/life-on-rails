import { startOfDay } from "date-fns";
import type { ExpandedHabit } from "../server/queries";
import { api } from "../utils/api";

function HabitTableRow({
  description,
  frequency,
  frequencyHorizon,
  score,
  completions,
  metrics,
}: ExpandedHabit) {
  return (
    <tr>
      <td className="border-blue-gray-50  px-5 py-3">
        <div className="flex items-center gap-4">
          <p className="text-blue-gray-900 block text-sm leading-normal antialiased">
            {description}
          </p>
        </div>
      </td>
      <td className="border-blue-gray-50 px-5 py-3">
        <div className="w-10/12">
          <p className="text-blue-gray-600  mb-1 block text-center font-sans text-xs font-medium antialiased">
            {(score * 100).toFixed(1)}
            {/* */}%
          </p>
          <div className="flex-start bg-blue-gray-50 flex h-1 w-full overflow-hidden rounded-sm bg-gray-200 font-sans text-xs font-medium">
            <div
              className="flex h-full items-baseline justify-center overflow-hidden break-all bg-gradient-to-tr from-red-600 to-red-400 text-white"
              style={{
                width: `${score * 100}%`,
              }}
            />
          </div>
        </div>
      </td>
      <td className="border-blue-gray-50 px-1 py-3 text-center font-sans text-sm">
        <p className="text-blue-gray-900 block text-xs leading-normal antialiased">
          {metrics.length}
        </p>
      </td>
      <td className="border-blue-gray-50 px-1 py-3  font-sans text-sm">
        <p className="text-blue-gray-900 block text-center text-xs leading-normal antialiased">
          {frequency}x per {frequencyHorizon.toLowerCase()}
        </p>
      </td>
      <td className="border-blue-gray-50 px-1 py-3 text-center font-sans text-sm">
        <p className="text-blue-gray-900 block text-xs leading-normal antialiased">
          {completions}
        </p>
      </td>
    </tr>
  );
}

function HabitsTable() {
  const query = api.habits.getHabits.useQuery({ date: startOfDay(new Date()) });

  // api.habits.getHabits.useQuery();

  if (query.isLoading) {
    return <div>Loading...</div>;
  } else if (query.error) {
    return <div>Error!</div>;
  }
  const habits = query.data;

  return (
    <div className="mx-auto max-w-screen-lg px-4 py-8 ">
      <div className="relative flex flex-col overflow-auto rounded-xl border border-blue-200 bg-white bg-clip-border text-gray-700 shadow-lg xl:col-span-2">
        <div className="relative m-0 flex items-center justify-between overflow-hidden border-b-2 bg-blue-200 p-6 text-gray-700 shadow-none">
          <div>
            <h6 className="text-blue-gray-900 mb-1 block font-sans text-base font-semibold leading-relaxed tracking-normal antialiased">
              Habits at Risk
            </h6>
          </div>
        </div>
        <div className="overflow-x-scroll scrollbar-none p-6 px-0 pb-2 pt-0">
          <table className="w-full min-w-[640px] table-auto">
            <thead>
              <tr>
                <th className="border-blue-gray-50 border-b px-6 py-3 text-left">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Habits
                  </p>
                </th>
                <th className="border-blue-gray-50 border-b px-6 py-3 text-center">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Score
                  </p>
                </th>
                <th className="border-blue-gray-50 border-b px-1 py-3 text-center">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Metrics
                  </p>
                </th>
                <th className="border-blue-gray-50 border-b px-6 py-3 text-center">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Schedule
                  </p>
                </th>
                <th className="border-blue-gray-50 border-b px-1 py-3 text-center">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Completions
                  </p>
                </th>
              </tr>
            </thead>
            <tbody>
              {habits.map((habit) => {
                if (habit.score < 0.5)
                  return (
                    <HabitTableRow key={habit.id} {...habit}></HabitTableRow>
                  );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

const HabitsPage = () => {
  return (
    <>
      <HabitsTable></HabitsTable>
    </>
  );
};

export default HabitsPage;
