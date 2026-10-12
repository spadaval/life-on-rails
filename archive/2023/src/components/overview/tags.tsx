import { MinusCircleIcon } from "@heroicons/react/24/outline";
import { Plus } from "lucide-react";
import { useState } from "react";
import { api } from "../../utils/api";
import { Button } from "../ui/button";
import {
  Command,
  CommandGroup,
  CommandInput,
  CommandItem
} from "../ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "../ui/popover";

export function CreateTag({ commit }: { commit: (name: string) => void }) {
  const [open, setOpen] = useState(false);
  const [value, setValue] = useState("");
  const tagsQuery = api.tags.getTags.useQuery();
  const context = api.useContext();
  const tags = tagsQuery.data ?? [];

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className="px-4 py-2"
        >
          <Plus className="h-4 w-4"></Plus>
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-[200px] p-0">
        <Command>
          <CommandInput
            placeholder="Pick a tag..."
            value={value}
            onValueChange={setValue}
          />
          <CommandGroup>
            {tags.map((tag) => (
              <CommandItem
                key={tag.name}
                onSelect={() => {
                  commit(tag.name);
                  void context.tags.getTags.invalidate();
                  setOpen(false);
                }}
              >
                {tag.name}
              </CommandItem>
            ))}
          </CommandGroup>
          {value != "" && (
            <CommandGroup>
              <CommandItem
                onSelect={() => {
                  commit(value);
                  void context.tags.getTags.invalidate();
                  setOpen(false);
                }}
              >
                {value} (create)
              </CommandItem>
            </CommandGroup>
          )}
        </Command>
      </PopoverContent>
    </Popover>
  );
}

export function TagList(props: {
  tags: string[];
  unlink: (tagName: string) => void;
  link: (tagName: string) => void;
}) {
  return (
    <div className="flex items-center space-x-2 pt-2">
      {props.tags.map((tag) => (
        <div
          key={tag}
          className="hover:bg-slate:300 flex flex-row flex-nowrap items-center gap-3 bg-slate-200 px-4 py-2"
        >
          <span className="h-4 text-sm">{tag}</span>
          <button
            className="h-6 w-6 hover:stroke-red-300"
            onClick={() => props.unlink(tag)}
          >
            <MinusCircleIcon></MinusCircleIcon>
          </button>
        </div>
      ))}
      <CreateTag commit={(name: string) => props.link(name)}></CreateTag>
    </div>
  );
}

export function GoalTagList({ goalId }: { goalId: string }) {
  const context = api.useContext();

  const tagsQuery = api.goals.getTags.useQuery({ goalId });

  const linkGoal = api.tags.linkGoal.useMutation({
    onSettled: () => {
      void context.goals.getAllGoals.invalidate();
      void context.goals.getGoal.invalidate();
    },
  });
  const unlinkGoal = api.tags.linkGoal.useMutation({
    onSettled: () => {
      void context.goals.getAllGoals.invalidate();
      void context.goals.getGoal.invalidate();
    },
  });

  return (
    <TagList
      tags={tagsQuery.data?.map((it) => it.name) ?? []}
      link={(tag) => linkGoal.mutate({ goalId, tagName: tag })}
      unlink={(tag) => unlinkGoal.mutate({ goalId, tagName: tag })}
    ></TagList>
  );
}
