import type { ScoringFormat } from "@prisma/client";
import type { ExpandedHabit, ExpandedMetric } from "../../server/queries";
import { HabitRows, MetricRows } from "./row";

export function JournalTable({
  habits,
  date,
  metrics,
  scoringUnit,
}: {
  habits: ExpandedHabit[];
  date: Date;
  metrics: ExpandedMetric[];
  scoringUnit: ScoringFormat;
}) {
  return (
    <>
      <table className="w-full whitespace-nowrap">
        <thead>
          <tr className="font-semisbold justify-between text-center text-xs">
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
            <td></td>
          </tr>
        </thead>
        <tbody>
          {habits.map((habit) => (
            <HabitRows
              habit={habit}
              date={date}
              key={habit.id}
              scoringUnit={scoringUnit}
            ></HabitRows>
          ))}
          {metrics.map((metric) => (
            <MetricRows
              metric={metric}
              date={date}
              key={metric.id}
              scoringUnit={scoringUnit}
            ></MetricRows>
          ))}
        </tbody>
      </table>
    </>
  );
}
