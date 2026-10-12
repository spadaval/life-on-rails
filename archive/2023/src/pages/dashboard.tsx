import { Loader } from "../components/ui/loader";
import type {
  ExpandedHabit,
  GoalsReturnType,
  ExpandedMetric,
} from "../server/queries";
import { ScoringFormat } from "@prisma/client";
import { api } from "../utils/api";
import { PieChart, pieChartDefaultProps } from "react-minimal-pie-chart";

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js";
import { Line } from "react-chartjs-2";

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

export const options = {
  responsive: true,
  plugins: {
    legend: {
      position: "bottom" as const,
    },
    title: {
      display: true,
      text: "Number of Habit Completions (last 7 days)",
    },
  },
};

const labels = [-7, -6, -5, -4, -3, -2, -1, 0];

const data = (data: number[]) => ({
  labels,
  datasets: [
    {
      label: "Habit Completions",
      data: data,
      borderColor: "rgb(255, 99, 132)",
      backgroundColor: "rgba(255, 99, 132, 0.5)",
    },
    // {
    //   label: 'Metric Completions',
    //   data: [3, 1, 5, 1, 5, 3, 2],
    //   borderColor: 'rgb(53, 162, 235)',
    //   backgroundColor: 'rgba(53, 162, 235, 0.5)',
    // },
  ],
});

// function App() {
//   return <Line options={options} data={data} />;
// }

function min(a: number, b: number) {
  return a < b ? a : b;
}

function StatsCardRow({
  goals,
  habits,
  metrics,
}: {
  goals: GoalsReturnType[];
  habits: ExpandedHabit[];
  metrics: ExpandedMetric[];
}) {
  // const goalsQuery = api.goals.getAllGoals.useQuery();
  // const metricsQuery = api.goals.getAllMetrics.useQuery();
  // if (goalsQuery.isLoading || metricsQuery.isLoading)
  //   return <p>Loading dashboard...</p>;
  // if (goalsQuery.isError || metricsQuery.isError) return <p>Query error</p>;
  // const goalData = goalsQuery.data;
  // const metricData = metricsQuery.data;

  let redGoal = 0;
  let yellowGoal = 0;
  let greenGoal = 0;

  for (let i = 0; i < goals.length; i++) {
    const g = goals[i];
    //console.log(`goal ${g?.goal.name} - ${g?.goal.score}`)
    if ((g?.goal?.score ?? 0) < 0.4 || isNaN(g?.goal?.score ?? 0)) {
      redGoal += 1;
    } else if ((g?.goal?.score ?? 0) < 0.7) {
      yellowGoal += 1;
    } else {
      greenGoal += 1;
    }
  }

  let redHabit = 0;
  let yellowHabit = 0;
  let greenHabit = 0;

  for (let i = 0; i < goals.length; i++) {
    //habit that links to a goal
    for (let j = 0; j < (goals[i]?.habits?.length ?? 0); j++) {
      const h = goals[i]?.habits[j];
      if ((h?.score ?? 0) < 0.4) {
        redHabit += 1;
      } else if ((h?.score ?? 0) < 0.7) {
        yellowHabit += 1;
      } else {
        greenHabit += 1;
      }
    }
  }
  for (let i = 0; i < habits.length; i++) {
    //standalone habit
    const h = habits[i];
    if ((h?.score ?? 0) < 0.4) {
      redHabit += 1;
    } else if ((h?.score ?? 0) < 0.7) {
      yellowHabit += 1;
    } else {
      greenHabit += 1;
    }
  }

  let redMetric = 0;
  let yellowMetric = 0;
  let greenMetric = 0;

  for (let i = 0; i < metrics.length; i++) {
    //all metrics
    if ((metrics[i]?.score ?? 0) < 0.4) {
      redMetric += 1;
    } else if ((metrics[i]?.score ?? 0) < 0.7) {
      yellowMetric += 1;
    } else {
      greenMetric += 1;
    }
  }

  const goalData = [
    { title: "Good", value: greenGoal, color: "#21C55D" },
    { title: "OK", value: yellowGoal, color: "#EAB305" },
    { title: "Bad", value: redGoal, color: "#EF4444" },
  ];

  const habitData = [
    { title: "Good", value: greenHabit, color: "#21C55D" },
    { title: "OK", value: yellowHabit, color: "#EAB305" },
    { title: "Bad", value: redHabit, color: "#EF4444" },
  ];

  const metricsData = [
    { title: "Good", value: greenMetric, color: "#21C55D" },
    { title: "OK", value: yellowMetric, color: "#EAB305" },
    { title: "Bad", value: redMetric, color: "#EF4444" },
  ];

  const aquery = api.habits.getCompletionsSubDays.useQuery();

  return (
    <div className="mx-auto max-w-screen-lg px-4 py-2 ">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-x-6 gap-y-4">
        <div className=" relative flex flex-1 flex-col  rounded-xl  text-gray-700 shadow-md">
          <div className="rounded-t-xl bg-indigo-800 p-4 text-center text-2xl text-white">
            Goals
          </div>

          <div className="flex w-full flex-row content-stretch justify-stretch rounded-b-xl border-2  border-indigo-800 p-1 text-center">
            <PieChart
              style={{
                fontSize: "8px",
              }}
              data={goalData}
              radius={pieChartDefaultProps.radius - 6}
              lineWidth={60}
              // segmentsStyle={{ transition: 'stroke .3s', cursor: 'pointer' }}
              animate
              label={({ dataEntry }) =>
                dataEntry.value === 0 ? "" : Math.round(dataEntry.value)
              }
              labelPosition={100 - 60 / 2}
              labelStyle={{
                fill: "#fff",
                opacity: 1,
                pointerEvents: "none",
              }}
            />
            {/* <span className="m-1 flex-1 rounded-sm bg-green-500 p-4 font-semibold text-white">
              {greenHabit}
            </span>
            <span className="m-1 flex-1 rounded-sm bg-yellow-500 p-4 font-semibold text-white">
              {yellowHabit}
            </span>
            <span className="m-1 flex-1 rounded-sm bg-red-500 p-4 font-semibold text-white">
              {redHabit}
            </span> */}
          </div>
        </div>

        <div className=" relative flex flex-1 flex-col  rounded-xl  text-gray-700 shadow-md">
          <div className="rounded-t-xl bg-indigo-800 p-4 text-center text-2xl text-white">
            Habits
          </div>
          <div className="flex w-full flex-row content-stretch justify-stretch rounded-b-xl border-2  border-indigo-800 p-1 text-center">
            <PieChart
              style={{
                fontSize: "8px",
              }}
              data={habitData}
              radius={pieChartDefaultProps.radius - 6}
              lineWidth={60}
              // segmentsStyle={{ transition: 'stroke .3s', cursor: 'pointer' }}
              animate
              label={({ dataEntry }) =>
                dataEntry.value === 0 ? "" : Math.round(dataEntry.value)
              }
              labelPosition={100 - 60 / 2}
              labelStyle={{
                fill: "#fff",
                opacity: 1,
                pointerEvents: "none",
              }}
            />
            {/* <span className="m-1 flex-1 rounded-sm bg-green-500 p-4 font-semibold text-white">
              {greenHabit}
            </span>
            <span className="m-1 flex-1 rounded-sm bg-yellow-500 p-4 font-semibold text-white">
              {yellowHabit}
            </span>
            <span className="m-1 flex-1 rounded-sm bg-red-500 p-4 font-semibold text-white">
              {redHabit}
            </span> */}
          </div>
        </div>
        <div className=" relative flex flex-1 flex-col  rounded-xl  text-gray-700 shadow-md">
          <div className="rounded-t-xl bg-indigo-800 p-4 text-center text-2xl text-white">
            Metrics
          </div>
          {/* <div className="flex w-full flex-row content-stretch justify-stretch p-1 text-center">
            <span className="m-1 flex-1 rounded-sm bg-green-500 p-4 font-semibold text-white">
              {greenMetric}
            </span>
            <span className="m-1 flex-1 rounded-sm bg-yellow-500 p-4 font-semibold text-white">
              {yellowMetric}
            </span>
            <span className="m-1 flex-1 rounded-sm bg-red-500 p-4 font-semibold text-white">
              {redMetric}
            </span>
          </div> */}
          <div className="flex w-full flex-row content-stretch justify-stretch rounded-b-xl border-2 border-indigo-800 p-1 text-center">
            <PieChart
              style={{
                fontSize: "8px",
              }}
              data={metricsData}
              radius={pieChartDefaultProps.radius - 6}
              lineWidth={60}
              // segmentsStyle={{ transition: 'stroke .3s', cursor: 'pointer' }}
              animate
              label={({ dataEntry }) =>
                dataEntry.value === 0 ? "" : Math.round(dataEntry.value)
              }
              labelPosition={100 - 60 / 2}
              labelStyle={{
                fill: "#fff",
                opacity: 1,
                pointerEvents: "none",
              }}
            />
          </div>
        </div>
        {/* https://react-chartjs-2.js.org/examples/line-chart/ */}
        <Line options={options} data={data(aquery.data ?? [])} />
      </div>
    </div>
  );
}

function GoalTableRow({
  goal,
  habits,
  metrics,
  scoringUnit,
}: GoalsReturnType & { scoringUnit: ScoringFormat }) {
  return (
    <tr>
      <td className="border-blue-gray-50  px-5 py-3">
        <div className="flex items-center gap-4">
          <p className="text-blue-gray-900 block text-sm leading-normal antialiased">
            {goal.name}
          </p>
        </div>
      </td>
      <td className="border-blue-gray-50 px-5 py-3">
        <div className="">
          <p className="text-blue-gray-600  mb-1 block text-center font-sans text-xs font-medium antialiased">
            {scoringUnit == ScoringFormat.Normalized
              ? min(1, goal.score).toFixed(2)
              : (min(1, goal.score) * 100).toFixed(2) + "%"}
          </p>
          <div className="flex-start bg-blue-gray-50 flex h-1 w-full overflow-hidden rounded-sm bg-gray-200 font-sans text-xs font-medium">
            <div
              className="flex h-full items-baseline justify-center overflow-hidden break-all bg-gradient-to-tr from-red-600 to-red-400 text-white"
              style={{
                width: `${goal.score * 100}%`,
              }}
            />
          </div>
        </div>
      </td>
      <td className="border-blue-gray-50 px-1 py-3 text-center font-sans text-sm">
        <p className="text-blue-gray-900 block text-xs leading-normal antialiased">
          {habits.length}
        </p>
      </td>
      <td className="border-blue-gray-50 px-1 py-3 text-center font-sans text-sm">
        <p className="text-blue-gray-900 block text-xs leading-normal antialiased">
          {metrics.length}
        </p>
      </td>
    </tr>
  );
}

function GoalsTable({
  goals,
  scoringUnit,
}: {
  goals: GoalsReturnType[];
  scoringUnit: ScoringFormat;
}) {
  // const query = api.goals.getAllGoals.useQuery();

  // if (query.isLoading) {
  //   return <Loader></Loader>;
  // } else if (query.error) {
  //   return <div>Error!</div>;
  // }
  // const goalsData = query.data;
  // const goals = goalsData.goals;

  return (
    <div className="mx-auto max-w-screen-lg px-4 py-2 ">
      <div className="relative flex flex-col overflow-auto rounded-xl border border-indigo-800 bg-white bg-clip-border text-gray-700 shadow-lg xl:col-span-2">
        <div className="relative m-0 flex items-center justify-between overflow-hidden border-b-2 bg-indigo-800 p-6 text-gray-700 shadow-none">
          <div>
            <h6 className="mb-1 block font-sans text-xl font-semibold leading-relaxed tracking-normal text-white antialiased">
              Goals at Risk
            </h6>
          </div>
        </div>
        <div className="overflow-x-scroll p-6 px-0 pb-2 pt-0">
          <table className="w-full min-w-[640px] table-auto">
            <thead>
              <tr>
                <th className="border-blue-gray-50 border-b px-6 py-3 text-left">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Goals
                  </p>
                </th>
                <th className="border-blue-gray-50 border-b px-6 py-3 text-center">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Score
                  </p>
                </th>
                <th className="border-blue-gray-50 border-b px-1 py-3 text-center">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Habits
                  </p>
                </th>
                <th className="border-blue-gray-50 border-b px-6 py-3 text-center">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Metrics
                  </p>
                </th>
              </tr>
            </thead>
            <tbody>
              {goals
                .sort((a, b) => a.goal.score - b.goal.score)
                .map((each) => {
                  if (each.goal.score < 0.4)
                    return (
                      <GoalTableRow
                        key={each.goal.id}
                        {...each}
                        scoringUnit={scoringUnit}
                      ></GoalTableRow>
                    );
                })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function HabitTableRow({
  description,
  frequency,
  frequencyHorizon,
  score,
  completions,
  metrics,
  scoringUnit,
}: ExpandedHabit & { scoringUnit: ScoringFormat }) {
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
        <div className="">
          <p className="text-blue-gray-600  mb-1 block text-center font-sans text-xs font-medium antialiased">
            {scoringUnit == ScoringFormat.Normalized
              ? min(1, score).toFixed(2)
              : (min(1, score) * 100).toFixed(2) + "%"}
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

function HabitsTable({
  habits,
  scoringUnit,
}: {
  habits: ExpandedHabit[];
  scoringUnit: ScoringFormat;
}) {
  // const query = api.habits.getHabits.useQuery({ date: date });

  // // api.habits.getHabits.useQuery();

  // if (query.isLoading) {
  //   return <Loader></Loader>;
  // } else if (query.error) {
  //   return <div>Error!</div>;
  // }
  // const habits = query.data;

  return (
    <div className="mx-auto max-w-screen-lg px-4 py-2 ">
      <div className="relative flex flex-col overflow-auto rounded-xl border border-indigo-800 bg-white bg-clip-border text-gray-700 shadow-lg xl:col-span-2">
        <div className="relative m-0 flex items-center justify-between overflow-hidden border-b-2 bg-indigo-800 p-6 text-gray-700 shadow-none">
          <div>
            <h6 className="mb-1 block font-sans text-xl font-semibold leading-relaxed tracking-normal text-white antialiased">
              Habits at Risk
            </h6>
          </div>
        </div>
        <div className="overflow-x-scroll p-6 px-0 pb-2 pt-0">
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
              {habits
                .sort((a, b) => a.score - b.score)
                .map((habit) => {
                  if (habit.score < 0.4)
                    return (
                      <HabitTableRow
                        key={habit.id}
                        {...habit}
                        scoringUnit={scoringUnit}
                      ></HabitTableRow>
                    );
                })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function MetricTableRow({
  prompt,
  score,
  scoringUnit,
}: ExpandedMetric & { scoringUnit: ScoringFormat }) {
  return (
    <tr>
      <td className="border-blue-gray-50  px-5 py-3">
        <div className="flex items-center gap-4">
          <p className="text-blue-gray-900 block text-sm leading-normal antialiased">
            {prompt}
          </p>
        </div>
      </td>
      <td className="border-blue-gray-50 px-5 py-3">
        <div className="">
          <p className="text-blue-gray-600  mb-1 block text-center font-sans text-xs font-medium antialiased">
            {scoringUnit == ScoringFormat.Normalized
              ? min(1, score).toFixed(2)
              : (min(1, score) * 100).toFixed(2) + "%"}
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
    </tr>
  );
}

function MetricsTable({
  metrics,
  scoringUnit,
}: {
  metrics: ExpandedMetric[];
  scoringUnit: ScoringFormat;
}) {
  // const query = api.goals.getAllMetrics.useQuery();

  // if (query.isLoading) {
  //   return <Loader></Loader>;
  // } else if (query.error) {
  //   return <div>Error!</div>;
  // }
  // const metricData = query.data;
  // const metrics = metricData.metrics;

  return (
    <div className="mx-auto max-w-screen-lg px-4 py-2 ">
      <div className="relative flex flex-col overflow-auto rounded-xl border border-indigo-800 bg-white bg-clip-border text-gray-700 shadow-lg xl:col-span-2">
        <div className="relative m-0 flex items-center justify-between overflow-hidden border-b-2 bg-indigo-800 p-6 text-gray-700 shadow-none">
          <div>
            <h6 className="mb-1 block font-sans text-xl font-semibold leading-relaxed tracking-normal text-white antialiased">
              Metrics at Risk
            </h6>
          </div>
        </div>
        <div className="overflow-x-scroll p-6 px-0 pb-2 pt-0">
          <table className="w-full min-w-[640px] table-auto">
            <thead>
              <tr>
                <th className="border-blue-gray-50 border-b px-6 py-3 text-left">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Metrics
                  </p>
                </th>
                <th className="border-blue-gray-50 border-b px-6 py-3 text-center">
                  <p className="text-blue-gray-400 block font-sans text-[11px] font-medium uppercase antialiased">
                    Score
                  </p>
                </th>
              </tr>
            </thead>
            <tbody>
              {metrics
                .sort((a, b) => a.score - b.score)
                .map((metric) => {
                  if (metric.score < 0.4)
                    return (
                      <MetricTableRow
                        key={metric.id}
                        {...metric}
                        scoringUnit={scoringUnit}
                      ></MetricTableRow>
                    );
                })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

const today = new Date();
export function Home() {
  const goalsQuery = api.goals.getAllGoals.useQuery();
  const habitsQuery = api.habits.getHabits.useQuery({ date: today });
  const metricsQuery = api.goals.getAllMetrics.useQuery();
  const profileQuery = api.profile.getProfile.useQuery();

  if (
    goalsQuery.isLoading ||
    metricsQuery.isLoading ||
    habitsQuery.isLoading ||
    profileQuery.isLoading
  )
    return <Loader></Loader>;
  if (
    goalsQuery.isError ||
    metricsQuery.isError ||
    habitsQuery.isError ||
    profileQuery.isError
  )
    return <p>Query error</p>;

  return (
    <div className="">
      <div className="m-auto h-full max-w-3xl pt-2">
        <StatsCardRow
          goals={goalsQuery.data.goals}
          habits={goalsQuery.data.habits}
          metrics={metricsQuery.data.metrics}
        ></StatsCardRow>

        <GoalsTable
          goals={goalsQuery.data.goals}
          scoringUnit={profileQuery.data.scoringUnit}
        ></GoalsTable>

        <HabitsTable
          habits={habitsQuery.data}
          scoringUnit={profileQuery.data.scoringUnit}
        ></HabitsTable>

        <MetricsTable
          metrics={metricsQuery.data.metrics}
          scoringUnit={profileQuery.data.scoringUnit}
        ></MetricsTable>

        {/* <div className="mb-6 mt-5 grid grid-cols-1 gap-x-6 gap-y-12 md:grid-cols-2 xl:grid-cols-3">
          {statisticsChartsData.map((props) => (
            <StatisticsChart
              key={props.title}
              {...props}
              footer={
                <p className="text-blue-gray-600 flex items-center font-sans text-sm font-normal leading-normal antialiased">
                  <ClockIcon strokeWidth={2} className="h-4 w-4 text-inherit" />
                  &nbsp;{props.footer}
                </p>
              }
            />
          ))}
        </div> */}
      </div>
    </div>
  );
}

export default Home;
