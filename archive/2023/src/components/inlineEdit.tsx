import type { FrequencyHorizon } from "@prisma/client";
import type { ChangeEvent, KeyboardEvent } from "react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { cn } from "../lib/utils";

export const useInlineEdit = ({
  initialText,
  commit,
}: {
  initialText: string;
  commit: (text: string) => void;
}) => {
  const [isActive, setActive] = useState<boolean>(false);
  const [text, setText] = useState<string>(initialText);

  const triggerProps = {
    onClick: () => setActive(true),
  };
  const editProps = {
    value: text,
    onChange: (e: ChangeEvent<HTMLInputElement>) => setText(e.target.value),
    onFocus: () => {
      //setText(initialText);
    },
    onBlur: () => {
      console.log("blur");
      setActive(false);
    },
    onKeyDown: (event: KeyboardEvent<HTMLInputElement>) => {
      if (event.key == "Enter") {
        commit(text);
        console.log(`Commit with text ${text}`);
        setActive(false);
      } else if (event.key == "Escape") {
        setActive(false);
      }
    },
  };

  return {
    isActive,
    triggerProps,
    editProps,
  };
};

export const useInlineNumberEdit = ({
  initial,
  commit,
}: {
  initial: number;
  commit: (text: number) => void;
}) => {
  const [isActive, setActive] = useState<boolean>(false);
  const [number, setNumber] = useState(initial);

  const triggerProps = {
    onClick: () => setActive(true),
  };
  const editProps = {
    value: number,
    onChange: (e: ChangeEvent<HTMLInputElement>) =>
      setNumber(parseInt(e.target.value)),
    onKeyDown: (event: KeyboardEvent<HTMLInputElement>) => {
      if (event.key == "Enter") {
        commit(number);
        console.log(`Commit with text ${number}`);
        setActive(false);
      } else if (event.key == "Escape") {
        setActive(false);
      }
    },
  };

  return {
    isActive,
    triggerProps,
    editProps,
  };
};

export const EditableField = ({
  initialText,
  commit,
  className,
}: {
  initialText: string;
  commit: (text: string) => void;
  className?: string;
}) => {
  const { isActive, triggerProps, editProps } = useInlineEdit({
    initialText,
    commit,
  });

  return (
    <div
      className={cn("flex flex-row flex-nowrap gap-1 whitespace-nowrap rounded-lg px-2 py-2 hover:bg-sky-50 group", {"flex-grow": isActive})}
      {...triggerProps}
    >
      {isActive ? (
          <Input {...editProps} autoFocus />
      ) : (
        <>
          <div className={className}>{initialText}</div>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="hidden h-6 w-6 group-hover:inline-block"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10"
            />
          </svg>
        </>
      )}
    </div>
  );
};

type DropDownProps = {
  frequencyHorizon: FrequencyHorizon;
  commit: (freq: FrequencyHorizon) => void;
  className?: string;
};

export const DropDown = ({
  frequencyHorizon,
  commit,
  className,
}: DropDownProps) => {
  return (
    <div className="inline-block hover:bg-gray-100">
      <select
        defaultValue={frequencyHorizon}
        onChange={(event) => {
          commit(event.target.value as FrequencyHorizon);
        }}
        onKeyDown={(event: KeyboardEvent<HTMLSelectElement>) => {
          if (event.key == "Enter") {
            commit(event.currentTarget.value as FrequencyHorizon);
          } else if (event.key == "Escape") {
          }
        }}
        className={className}
      >
        <option value="DAY">Day</option>
        <option value="WEEK">Week</option>
      </select>
    </div>
  );
};

export const EditableNumberField = ({
  initial,
  commit,
  className,
}: {
  initial: number;
  commit: (text: number) => void;
  className: string;
}) => {
  const { isActive, triggerProps, editProps } = useInlineNumberEdit({
    initial,
    commit: (number) => commit(number),
  });

  return (
    <>
      {isActive ? (
        <input type="number" {...editProps} autoFocus className="w-7 " />
      ) : (
        <div
          className="group inline-flex flex-row flex-nowrap gap-1 whitespace-nowrap hover:bg-gray-100"
          {...triggerProps}
        >
          <span className={className}>{initial}</span>

          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="hidden h-6 w-6 group-hover:inline-block"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10"
            />
          </svg>
        </div>
      )}
    </>
  );
};

export type CreateLinkedMetric = {
  prompt: string;
  type: "FIVE_POINT" | "number";
};

export function CreateLinkedMetricInline({
  createMetric,
  closeEdit,
}: {
  createMetric: (prompt: string) => void;
  closeEdit: () => void;
}) {
  const { register, handleSubmit } = useForm<CreateLinkedMetric>();

  const onSubmit = (formData: CreateLinkedMetric) => {
    console.log(formData);
    createMetric(formData.prompt);
    closeEdit();
  };

  return (
    <div className="mt-2 w-full rounded-lg p-4">
      <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
        <div className="flex flex-row flex-nowrap gap-2">
          <div className="flex-grow">
            <input
              type="text"
              id="prompt"
              className="block w-full rounded-lg border border-gray-300 bg-gray-50 p-2.5 text-sm text-gray-900 focus:border-blue-500 focus:ring-blue-500"
              placeholder="Prompt"
              required
              {...register("prompt")}
            />
          </div>
          <div className="space-x-2 text-right">
            <Button onClick={closeEdit} variant="outline" type="button">
              Cancel
            </Button>
            <Button type="submit">Create</Button>
          </div>
        </div>
      </form>
    </div>
  );
}

export type CreateLinkedHabit = {
  description: string;
};

export function CreateLinkedHabitInline({
  createHabit,
  closeEdit,
}: {
  createHabit: (description: string) => void;
  closeEdit: () => void;
}) {
  const { register, handleSubmit } = useForm<CreateLinkedHabit>();

  const onSubmit = (formData: CreateLinkedHabit) => {
    createHabit(formData.description);
    closeEdit();
  };

  return (
    <div className="mt-2 w-full rounded-lg p-4">
      <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
        <div className="flex flex-row flex-nowrap gap-2">
          <div className="flex-grow">
            <input
              type="text"
              id="description"
              className="block w-full rounded-lg border border-gray-300 bg-gray-50 p-2.5 text-sm text-gray-900 focus:border-blue-500 focus:ring-blue-500"
              placeholder="Description"
              required
              {...register("description")}
            />
          </div>
          <div className="space-x-2 text-right">
            <Button onClick={closeEdit} variant="outline" type="button">
              Cancel
            </Button>
            <Button type="submit">Create</Button>
          </div>
        </div>
      </form>
    </div>
  );
}
