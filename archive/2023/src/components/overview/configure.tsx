import { Label } from "@radix-ui/react-dropdown-menu";
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
} from "@radix-ui/react-popover";
import {
  Command,
  CommandInput,
  CommandEmpty,
  CommandGroup,
  CommandItem,
} from "../ui/command";
import { Loader } from "lucide-react";
import { useState } from "react";
import type { RouterOutputs } from "../../utils/api";
import { api } from "../../utils/api";
import { Button } from "../ui/button";

export function ConfigureOverview({
  filters,
  setFilters,
}: {
  filters: Filters | undefined;
  setFilters: (filters: Filters | undefined) => void;
}) {
  const tagsQuery = api.tags.getTags.useQuery();
  const [open, setOpen] = useState(false);

  if (!tagsQuery.data) {
    return (
      <div className="mx-auto w-full text-center">
        <div className="mb-4">
          <h4 className="font-semibold uppercase leading-none">Configure</h4>
        </div>
        <div className="">
          <div className="flex flex-row items-center gap-4">
            <Label className="text-sm uppercase text-gray-500">Tags</Label>
            <div>
              <Loader></Loader>
            </div>
          </div>
        </div>
      </div>
    );
  }
  return (
    <div className="mx-auto w-full text-center">
      <div className="mb-4">
        <h4 className="font-semibold uppercase leading-none">Configure</h4>
      </div>
      <div className="">
        <div className="flex flex-row items-center gap-4">
          <Label className="text-sm uppercase text-gray-500">Tags</Label>
          <div className="flex flex-row items-center gap-2">
            {filters?.tags.map((tag) => (
              <span key={tag} className="rounded-lg bg-gray-200">
                {tag}
              </span>
            ))}
            <Popover open={open} onOpenChange={setOpen}>
              <PopoverTrigger asChild>
                <Button variant="secondary">Filter by a tag</Button>
              </PopoverTrigger>
              <PopoverContent>
                <Command>
                  <CommandInput placeholder="Pick a tag..." />
                  <CommandEmpty>No tag found.</CommandEmpty>
                  <CommandGroup>
                    {tagsQuery.data.map((tag) => (
                      <CommandItem
                        key={tag.name}
                        onSelect={(currentValue) => {
                          setFilters({
                            tags: [...(filters?.tags ?? []), currentValue],
                          });
                          setOpen(false);
                        }}
                      >
                        {tag.name}
                      </CommandItem>
                    ))}
                  </CommandGroup>
                </Command>
              </PopoverContent>
            </Popover>
          </div>
        </div>
      </div>
      {filters && (
        <div className="mt-6">
          <Button
            variant="link"
            onClick={() => {
              setFilters(undefined);
              setOpen(false);
            }}
          >
            Clear all filters
          </Button>
        </div>
      )}
    </div>
  );
}

export type Filters = {
  tags: string[];
};

export function filteredData(
  data: RouterOutputs["goals"]["getAllGoals"],
  filters: Filters | undefined
) {
  if (!filters) return data;
  const selectedTags = new Set(filters.tags);

  const goals = data.goals.filter(
    (goal) =>
      goal.goal.tags.some((tag) => selectedTags.has(tag.name)) ||
      goal.habits.some((habit) =>
        habit.tags.some((tag) => selectedTags.has(tag))
      ) ||
      goal.metrics.some((metric) =>
        metric.tags.some((tag) => selectedTags.has(tag.name))
      )
  );
  const habits = data.habits.filter((it) =>
    it.tags.some((tag) => selectedTags.has(tag))
  );
  const metrics = data.metrics.filter((it) =>
    it.tags.some((tag) => selectedTags.has(tag.name))
  );
  console.log(data.habits);
  console.log(selectedTags);
  return {
    goals,
    habits,
    metrics,
  };
}
