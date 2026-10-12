import type { Metric, ScoringFormat } from "@prisma/client";
import { FrequencyHorizon } from "@prisma/client";
import { CircleDot, CircleIcon, Loader2 } from "lucide-react";
import Link from "next/link";
import { RxExternalLink, RxGear } from "react-icons/rx";
import { HabitSheet } from "../overview/habit-panel";
import { Button } from "../ui/button";
import { HabitIcon, MetricIcon } from "../ui/icons";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../ui/tooltip";
import { CompletionStatus } from "./row";

export function MetricButtonRow({
  value,
  setValue,
}: {
  id: string;
  value: number | undefined;
  setValue: (score: number) => void;
}) {
  return (
    <div className="flex flex-row flex-nowrap gap-2 p-2">
      <Button
        onClick={() => setValue(1)}
        variant={value == 1 ? "default" : "outline"}
      >
        1
      </Button>
      <Button
        onClick={() => setValue(2)}
        variant={value == 2 ? "default" : "outline"}
      >
        2
      </Button>
      <Button
        onClick={() => setValue(3)}
        variant={value == 3 ? "default" : "outline"}
      >
        3
      </Button>
      <Button
        onClick={() => setValue(4)}
        variant={value == 4 ? "default" : "outline"}
      >
        4
      </Button>
      <Button
        onClick={() => setValue(5)}
        variant={value == 5 ? "default" : "outline"}
      >
        5
      </Button>
    </div>
  );
}
interface MetricProps {
  id: string;
  prompt: string;
  value: number | undefined;
  setValue: (score: number) => void;
}

export const MetricUI = ({ id, prompt, value, setValue }: MetricProps) => {
  return (
    <div className="mb-1 py-1">
      <span className="flex flex-row items-center justify-center gap-2">
        <MetricIcon />
        <p>{prompt}</p>
      </span>
      <MetricButtonRow
        id={id}
        value={value}
        setValue={setValue}
      ></MetricButtonRow>
    </div>
  );
};

export function TypeIcon({ type }: { type: "Habit" | "Metric" }) {
  return (
    <td className="">
      <div className="flex flex-row items-center justify-center">
        {type == "Habit" ? <HabitIcon /> : <MetricIcon />}
      </div>
    </td>
  );
}

export function MetricsTooltip({ metrics }: { metrics?: Metric[] }) {
  if (metrics && metrics.length > 0)
    return (
      <>
        <TooltipProvider>
          <Tooltip delayDuration={0}>
            <TooltipTrigger>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width={16}
                height={16}
                viewBox="0 0 16 16"
                fill="none"
              >
                <path
                  d="M6.66669 9.33342C6.88394 9.55515 7.14325 9.73131 7.42944 9.85156C7.71562 9.97182 8.02293 10.0338 8.33335 10.0338C8.64378 10.0338 8.95108 9.97182 9.23727 9.85156C9.52345 9.73131 9.78277 9.55515 10 9.33342L12.6667 6.66676C13.1087 6.22473 13.357 5.62521 13.357 5.00009C13.357 4.37497 13.1087 3.77545 12.6667 3.33342C12.2247 2.89139 11.6251 2.64307 11 2.64307C10.3749 2.64307 9.77538 2.89139 9.33335 3.33342L9.00002 3.66676"
                  stroke="#3B82F6"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M9.33336 6.66665C9.11611 6.44492 8.8568 6.26876 8.57061 6.14851C8.28442 6.02825 7.97712 5.96631 7.66669 5.96631C7.35627 5.96631 7.04897 6.02825 6.76278 6.14851C6.47659 6.26876 6.21728 6.44492 6.00003 6.66665L3.33336 9.33332C2.89133 9.77534 2.64301 10.3749 2.64301 11C2.64301 11.6251 2.89133 12.2246 3.33336 12.6666C3.77539 13.1087 4.37491 13.357 5.00003 13.357C5.62515 13.357 6.22467 13.1087 6.66669 12.6666L7.00003 12.3333"
                  stroke="#3B82F6"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </TooltipTrigger>
            <TooltipContent>
              <span>{metrics.map((m) => m.prompt).join(", ")}</span>
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>
      </>
    );
  else return <></>;
}

export function TagsTooltip({ tags }: { tags: string[] }) {
  return (
    <>
      <TooltipProvider>
        <Tooltip delayDuration={0}>
          <TooltipTrigger>
            <div className="flex items-center">
              {/* only show if there's tags */}
              {tags.length > 0 && (
                <div
                  className="item"
                  data-tooltip-id="tag-tooltip"
                  data-tooltip-content={tags.join(", ")}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width={20}
                    height={20}
                    viewBox="0 0 20 20"
                    fill="none"
                  >
                    <path
                      d="M9.16667 2.5L16.6667 10C17.0911 10.4745 17.0911 11.1922 16.6667 11.6667L11.6667 16.6667C11.1922 17.0911 10.4745 17.0911 10 16.6667L2.5 9.16667V5.83333C2.5 3.99238 3.99238 2.5 5.83333 2.5H9.16667"
                      stroke="#52525B"
                      strokeWidth="1.25"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                    <circle
                      cx="7.50004"
                      cy="7.49967"
                      r="1.66667"
                      stroke="#52525B"
                      strokeWidth="1.25"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                </div>
              )}
            </div>
          </TooltipTrigger>
          <span className="bg-gray-400">
            <TooltipContent>{tags.join(", ")}</TooltipContent>
          </span>
        </Tooltip>
      </TooltipProvider>
    </>
  );
}

export function Status({
  status,
  action,
}: {
  status: CompletionStatus;
  action?: () => void;
}) {
  switch (status) {
    case CompletionStatus.LOADING:
      return (
        <Tooltip>
          <TooltipTrigger asChild>
            <Loader2 className="animate-spin stroke-gray-500"></Loader2>
          </TooltipTrigger>
          <TooltipContent>Loading</TooltipContent>
        </Tooltip>
      );
    case CompletionStatus.PARTIAL:
      return (
        <Tooltip>
          <TooltipTrigger asChild>
            <CircleDot onClick={action} className="cursor-pointer"></CircleDot>
          </TooltipTrigger>
          <TooltipContent>Partially completed.</TooltipContent>
        </Tooltip>
      );
    default:
      return (
        <Tooltip>
          <TooltipTrigger asChild>
            <CircleIcon
              onClick={action}
              className="cursor-pointer"
            ></CircleIcon>
          </TooltipTrigger>
          <TooltipContent>Click to complete this habit</TooltipContent>
        </Tooltip>
      );
  }
}

export function Schedule({
  currentCompletions,
  target,
  horizon,
}: {
  currentCompletions: number;
  target: number;
  horizon: FrequencyHorizon;
}) {
  return (
    <div className="flex flex-col items-center">
      <p className="ml-2 text-sm leading-none text-gray-600/40">
        {/* based off the frequency, if multiple times a day */}
        {currentCompletions}/{target}
      </p>
      <p className="ml-2 text-sm leading-none text-gray-400/40">
        {horizon == FrequencyHorizon.DAY ? "today" : "this week"}
      </p>
    </div>
  );
}

export function Actions({
  id,
  scoringUnit,
}: {
  id: string;
  scoringUnit: ScoringFormat;
}) {
  return (
    <div className="relative flex flex-row items-baseline gap-3 px-2 py-1">
      <HabitSheet habitId={id} scoringUnit={scoringUnit}>
        <RxGear className="cursor-pointer rounded-lg text-xl text-gray-500 hover:bg-gray-300 focus:outline-none focus:ring-1"></RxGear>
      </HabitSheet>
      <Link href={`/habit/${id}`}>
        <RxExternalLink className="cursor-pointer rounded-lg text-xl text-gray-500 hover:bg-gray-300 focus:outline-none focus:ring-1"></RxExternalLink>
      </Link>
    </div>
  );
}
