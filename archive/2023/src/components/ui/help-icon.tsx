import type { ReactNode } from "react";
import { AiFillQuestionCircle } from "react-icons/ai";
import { Tooltip, TooltipContent, TooltipTrigger } from "./tooltip";

export const HelpIcon = ({ children }: { children: ReactNode }) => (
  <Tooltip>
    <TooltipTrigger>
      <AiFillQuestionCircle className="h-6 w-6 fill-gray-500 hover:fill-gray-800" />
    </TooltipTrigger>
    <TooltipContent className="w-60" side="right">{children}</TooltipContent>
  </Tooltip>
);
