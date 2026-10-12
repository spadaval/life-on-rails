import type { Metric, FrequencyHorizon } from "@prisma/client";
import { ScoringFormat } from "@prisma/client";
import classNames from "classnames";
import { AiFillCaretDown, AiFillCaretRight } from "react-icons/ai";
import type { ExpandedHabit, ExpandedMetric } from "../../server/queries";
import { textcolor } from "../overview/lib";
import {
  Actions,
  MetricButtonRow,
  MetricsTooltip,
  Schedule,
  Status,
  TagsTooltip,
  TypeIcon,
} from "./elements";
import { Memo, HabitPanel } from "./panel";
import { useCallback, useState } from "react";
import { api } from "../../utils/api";

export enum CompletionStatus {
  LOADING,
  INCOMPLETE,
  PARTIAL,
}

export interface Completion {
  status: CompletionStatus;
  schedule?: {
    current: number;
    frequency: number;
    frequencyHorizon: FrequencyHorizon;
  };
  action?: () => void;
}

interface RowProps {
  description: string;
  date: Date;
  completion?: Completion;
  metrics?: Metric[];
  score: number;
  tags: string[];
  type: "Habit" | "Metric";
  actions?: React.ReactNode;
  panel?: {
    open: boolean;
    togglePanel: (open: boolean) => void;
  };
  scoringUnit: ScoringFormat;
}

export const Row = ({
  type,
  description,
  score,
  completion,
  metrics,
  tags,
  actions,
  panel,
  scoringUnit,
}: RowProps): JSX.Element => {
  return (
    <tr
      tabIndex={0}
      className="h-12 rounded border border-gray-100 focus:outline-none"
    >
      <td className="cursor-pointer text-center opacity-50">
        {panel && (
          <button>
            {panel.open ? (
              <AiFillCaretDown
                onClick={() => panel.togglePanel(false)}
              ></AiFillCaretDown>
            ) : (
              <AiFillCaretRight
                onClick={() => panel.togglePanel(true)}
              ></AiFillCaretRight>
            )}
          </button>
        )}
      </td>
      <TypeIcon type={type}></TypeIcon>
      <td>
        {completion && (
          <Status
            status={completion.status}
            action={completion.action}
          ></Status>
        )}
      </td>
      <td>
        {completion?.schedule && (
          <Schedule
            currentCompletions={completion.schedule.current}
            target={completion.schedule.frequency}
            horizon={completion.schedule.frequencyHorizon}
          ></Schedule>
        )}
      </td>
      <td className="pl-2">
        <div
          className={classNames("w-full text-center text-sm", textcolor(score))}
        >
          {scoringUnit == ScoringFormat.Normalized
            ? min(1, score).toFixed(2)
            : (min(1, score) * 100).toFixed(2) + "%"}
        </div>
      </td>
      <td className="">
        <div className="flex items-center pl-2">
          <p
            className={classNames(
              "mr-2 text-base font-medium leading-none text-gray-700"
            )}
          >
            {description}
          </p>
          {type === "Habit" && (
            <MetricsTooltip metrics={metrics}></MetricsTooltip>
          )}
          <TagsTooltip tags={tags}></TagsTooltip>
        </div>
      </td>
      <td>{actions}</td>
    </tr>
  );
};

function min(a: number, b: number) {
  return a < b ? a : b;
}

export function MetricRows({
  metric,
  date,
  scoringUnit,
}: {
  metric: ExpandedMetric;
  date: Date;
  scoringUnit: ScoringFormat;
}) {
  const [memo, setMemo] = useState<string>("");
  const context = api.useContext();
  const setScore = api.metrics.setScore.useMutation({
    onSettled() {
      void context.metrics.invalidate();
    },
  });

  return (
    <>
      <Row
        type="Metric"
        description={metric.prompt}
        key={metric.id}
        tags={metric.tags.map((tag) => tag.name)}
        date={date}
        score={metric.score}
        scoringUnit={scoringUnit}
      ></Row>
      {
        <tr>
          <td
            colSpan={4}
            className="rounded border border-gray-100 bg-gray-100/70"
          ></td>
          <td colSpan={6} className="rounded border border-gray-100">
            <div className="px-4 pb-7 pt-3 md:px-10 md:pb-4">
              <div className="flex items-center">
                <MetricButtonRow
                  id={metric.id}
                  value={metric.value}
                  setValue={(value) =>
                    setScore.mutate({
                      metricId: metric.id,
                      memo: memo,
                      date: new Date(),
                      value: value,
                      score: value / 5,
                    })
                  }
                ></MetricButtonRow>
              </div>
              <div className="">
                <Memo content={memo} update={setMemo}></Memo>
              </div>
            </div>
          </td>
        </tr>
      }
    </>
  );
}

export function HabitRows({
  habit,
  date,
  scoringUnit,
}: {
  habit: ExpandedHabit;
  date: Date;
  scoringUnit: ScoringFormat;
}) {
  if (habit.metrics.length > 0) {
    return (
      <HabitWithLinkedMetrics
        habit={habit}
        date={date}
        scoringUnit={scoringUnit}
      />
    );
  } else {
    return <SimpleHabit habit={habit} date={date} scoringUnit={scoringUnit} />;
  }
}

function SimpleHabit({
  habit,
  date,
  scoringUnit,
}: {
  habit: ExpandedHabit;
  date: Date;
  scoringUnit: ScoringFormat;
}) {
  const context = api.useContext();
  const createCompletion = api.journal.complete.useMutation({
    onSuccess() {
      void context.habits.invalidate();
    },
  });

  const [loading, setLoading] = useState<boolean>(false);
  const [panelOpen, setPanelOpen] = useState<boolean>(false);
  const [memo, setMemo] = useState<string>("");

  const reset = useCallback(() => {
    setPanelOpen(false);
  }, []);

  habit.metrics;

  let status: CompletionStatus;

  if (loading) {
    status = CompletionStatus.LOADING;
  } else {
    status = CompletionStatus.INCOMPLETE;
  }

  return (
    <>
      <Row
        type="Habit"
        description={habit.description}
        key={habit.id}
        tags={habit.tags}
        date={date}
        metrics={habit.metrics}
        score={habit.score}
        actions={<Actions id={habit.id} scoringUnit={scoringUnit}></Actions>}
        panel={{
          open: panelOpen,
          togglePanel: setPanelOpen,
        }}
        completion={{
          status,
          schedule: {
            current: habit.completions,
            frequency: habit.frequency,
            frequencyHorizon: habit.frequencyHorizon,
          },
          action: () => {
            setLoading(true);
            createCompletion
              .mutateAsync({ date: date, habitId: habit.id, memo: memo })
              .then((_) => setLoading(false))
              .catch((_) => reset());
          },
        }}
        scoringUnit={scoringUnit}
      ></Row>
      {panelOpen && (
        <HabitPanel
          id={habit.id}
          metrics={habit.metrics}
          reset={reset}
          setLoading={setLoading}
          memo={{ content: memo, update: setMemo }}
        ></HabitPanel>
      )}
    </>
  );
}

function HabitWithLinkedMetrics({
  habit,
  date,
  scoringUnit,
}: {
  habit: ExpandedHabit;
  date: Date;
  scoringUnit: ScoringFormat;
}) {
  const [loading, setLoading] = useState<boolean>(false);
  const [partial, setPartial] = useState<boolean>(false);
  const [panelOpen, setPanelOpen] = useState<boolean>(false);
  const [memo, setMemo] = useState<string>("");

  const reset = useCallback(() => {
    setPartial(false);
    setPanelOpen(false);
  }, []);

  let status: CompletionStatus;

  if (loading) {
    status = CompletionStatus.LOADING;
  } else if (partial) {
    status = CompletionStatus.PARTIAL;
  } else {
    status = CompletionStatus.INCOMPLETE;
  }

  habit.metrics;
  return (
    <>
      <Row
        type="Habit"
        description={habit.description}
        key={habit.id}
        tags={habit.tags}
        date={date}
        metrics={habit.metrics}
        score={habit.score}
        actions={<Actions id={habit.id} scoringUnit={scoringUnit}></Actions>}
        panel={
          partial
            ? {
                open: panelOpen,
                togglePanel: setPanelOpen,
              }
            : undefined
        }
        completion={{
          status,
          schedule: {
            current: habit.completions,
            frequency: habit.frequency,
            frequencyHorizon: habit.frequencyHorizon,
          },
          action: () => {
            setPanelOpen(true);
            setPartial(true);
          },
        }}
        scoringUnit={scoringUnit}
      ></Row>
      {partial && panelOpen && (
        <HabitPanel
          id={habit.id}
          metrics={habit.metrics}
          reset={reset}
          setLoading={setLoading}
          memo={{ content: memo, update: setMemo }}
        ></HabitPanel>
      )}
    </>
  );
}
