import { PlusIcon, XCircle } from "lucide-react";
import { useState } from "react";
import type { ExpandedMetric } from "../../server/queries";
import { Button } from "../ui/button";
import { MetricUI } from "./elements";
import { api } from "../../utils/api";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "../ui/accordion";
import { Loader } from "../ui/loader";
import { SimpleTooltip } from "../ui/tooltip";
import type { HabitCompletion } from "@prisma/client";

export function Memo({
  content,
  update,
}: {
  content: string;
  update: (content: string) => void;
}) {
  const [active, setActive] = useState<boolean>(false);

  return !active ? (
    <span
      className="mt-5 flex cursor-pointer flex-row items-center gap-1 text-gray-400"
      onClick={() => setActive(true)}
    >
      <PlusIcon className="h-4 w-4"></PlusIcon> Add memo
    </span>
  ) : (
    <textarea
      placeholder="Memo"
      className="h-14 w-full resize-none rounded border border-gray-200 py-3 pl-3 focus:h-20 focus:overflow-auto focus:outline-none"
      value={content}
      onChange={(e) => update(e.target.value)}
      onKeyDown={(key) => {
        if (key.key === "Escape") {
          setActive(false);
        }
        if (key.key === "Enter") {
          setActive(false);
        }
      }}
    />
  );
}

function RecentCompletions({ id }: { id: string }) {
  const completions = api.habits.getCompletions.useQuery({
    habitId: id,
    timeHorizon: 2,
  });

  return (
    <div className="mx-auto w-3/4 p-2 shadow-sm">
      <Accordion type="single" collapsible>
        <AccordionItem value="item-1">
          <AccordionTrigger>Recent completions</AccordionTrigger>
          <AccordionContent>
            {completions.isLoading || completions.isError ? (
              <Loader></Loader>
            ) : (
              <CompletionsList completions={completions.data}></CompletionsList>
            )}
          </AccordionContent>
        </AccordionItem>
      </Accordion>
    </div>
  );
}

function CompletionsList({ completions }: { completions: HabitCompletion[] }) {
  const context = api.useContext();
  const deleteCompletion = api.habits.deleteCompletion.useMutation({
    onSuccess() {
      void context.habits.invalidate();
    },
  });

  if (completions.length === 0) {
    return <div className="text-center text-gray-400">No completions</div>;
  }

  return (
    <div className="pt-10">
      <ol className="pt-1 dark:border-gray-700">
        {completions.map((it) => (
          <li className="mb-2" key={it.id}>
            <div className="flex items-center justify-between gap-10 rounded-lg border border-gray-300 bg-white p-2 shadow-sm">
              <div className="mb-1 text-xs font-normal text-gray-400">
                {it.date.toLocaleString()}
              </div>
              <SimpleTooltip content="Delete this completion">
                <XCircle
                  className="cursor-pointer opacity-50 hover:bg-red-100"
                  onClick={() =>
                    deleteCompletion.mutate({ completionId: it.id })
                  }
                ></XCircle>
              </SimpleTooltip>
            </div>
            {it.memo && (
              <div className="mx-2 rounded-b-lg border border-gray-200 bg-gray-50 p-3 text-left text-xs font-normal italic text-gray-500 dark:border-gray-500 dark:bg-gray-600 dark:text-gray-300">
                {it.memo}
              </div>
            )}
          </li>
        ))}
      </ol>
    </div>
  );
}

export function HabitPanel({
  id,
  metrics,
  reset,
  setLoading,
  memo,
}: {
  id: string;
  metrics: ExpandedMetric[];
  reset: () => void;
  setLoading: (loading: boolean) => void;
  memo?: { content: string; update: (content: string) => void };
}) {
  const context = api.useContext();
  const setScore = api.metrics.setScore.useMutation();
  const createCompletion = api.journal.complete.useMutation({
    onSuccess() {
      void context.habits.invalidate();
    },
  });

  const initValue = [...Array(metrics.length).keys()].map((_) => undefined);
  const [metricValues, setMetricScores] =
    useState<(number | undefined)[]>(initValue);
  console.log(metricValues);
  return (
    <tr>
      <td colSpan={1} className="bg-gray-100/70"></td>
      <td colSpan={10} className="rounded border border-gray-100">
        <RecentCompletions id={id} />
        <div className="px-4 pb-7 pt-3 md:px-10 md:pb-4">
          <div className="flex items-center">
            {metrics.map((metric, i) => (
              <MetricUI
                key={metric.id}
                id={metric.id}
                prompt={metric.prompt}
                value={metricValues[i]}
                setValue={(value) => {
                  metricValues[i] = value;
                  setMetricScores([...metricValues]);
                }}
              ></MetricUI>
            ))}
          </div>
          <div className="">
            {memo && <Memo content={memo.content} update={memo.update}></Memo>}
          </div>
          {metrics.length > 0 && (
            <div className="items-right mt-2 flex justify-end gap-2">
              <Button variant="link" onClick={reset}>
                Cancel
              </Button>
              <Button
                variant="outline"
                disabled={metricValues.some((it) => it === undefined)}
                onClick={() => {
                  setLoading(true);
                  const now = new Date();
                  metrics.forEach((metric, i) => {
                    setScore.mutate({
                      metricId: metric.id,
                      date: now,
                      score: metricValues[i]! / 5,
                      value: metricValues[i]!,
                    });
                  });
                  createCompletion.mutate({
                    habitId: id,
                    date: now,
                    memo: memo?.content,
                  });
                  reset();
                  setLoading(false);
                }}
              >
                Done
              </Button>
            </div>
          )}
        </div>
      </td>
    </tr>
  );
}
