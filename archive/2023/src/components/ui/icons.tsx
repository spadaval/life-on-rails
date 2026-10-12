import {
  TbSquareRoundedLetterG,
  TbSquareRoundedLetterH,
  TbSquareRoundedLetterM,
} from "react-icons/tb";
import { Tooltip, TooltipContent, TooltipTrigger } from "./tooltip";

export const GoalIcon = () => (
  <Tooltip delayDuration={200}>
    <TooltipTrigger>
      <TbSquareRoundedLetterG className="text-2xl text-yellow-500"></TbSquareRoundedLetterG>
    </TooltipTrigger>
    <TooltipContent>Goal</TooltipContent>
  </Tooltip>
);

export const HabitIcon = () => (
  <Tooltip delayDuration={200}>
    <TooltipTrigger>
      <TbSquareRoundedLetterH className="text-2xl text-blue-500"></TbSquareRoundedLetterH>
    </TooltipTrigger>
    <TooltipContent>Habit</TooltipContent>
  </Tooltip>
);

export const MetricIcon = () => (
  <Tooltip delayDuration={200}>
    <TooltipTrigger>
      <TbSquareRoundedLetterM className="text-2xl text-purple-500"></TbSquareRoundedLetterM>
    </TooltipTrigger>
    <TooltipContent>Metric</TooltipContent>
  </Tooltip>
);
