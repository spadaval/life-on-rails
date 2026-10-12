import { useState } from "react";
import { api } from "../utils/api";

interface GoalSelectorProps {
  goals: {
    id: string;
    name: string;
  }[];
  selectedGoal: string | undefined;
  onSelect: (goalId: string) => void;
}

const GoalSelector = ({ goals, selectedGoal, onSelect }: GoalSelectorProps) => {
  if (goals == undefined) {
    return (
      <div>
        <span>No goals available</span>
      </div>
    );
  }
  return (
    <div>
      <select
        id="goal"
        name="goal"
        className="focus:shadow-outline w-full appearance-none rounded border px-3 py-2 leading-tight text-gray-700 shadow-sm focus:outline-none"
        value={selectedGoal}
        onChange={(event) => onSelect(event.target.value)}
      >
        <option value="">Select a goal</option>
        {goals.map((goal) => (
          <option key={goal.id} value={goal.id}>
            {goal.name}
          </option>
        ))}
      </select>
    </div>
  );
};

interface SubjectiveFormProps {
  goals: {
    id: string;
    name: string;
  }[];
  onClose: () => void;
  createSubjective: (
    subjectiveName: string,
    selectedGoal: string | undefined
  ) => void;
}

function SubjectiveForm({
  goals,
  onClose,
  createSubjective,
}: SubjectiveFormProps) {
  const [subjectiveName, setsubjectiveName] = useState("");
  const [selectedGoal, setSelectedGoal] = useState<string | undefined>(
    undefined
  );

  return (
    <div className="fixed inset-0 z-10 bg-black bg-opacity-40">
      <div className="flex min-h-screen items-center justify-center">
        <div className="mx-auto max-w-xl bg-white px-4 py-6 sm:px-6 lg:px-8">
          <div className="mb-8 ">
            <h2 className="mb-2 text-center text-2xl font-semibold">
              Create Subjective
            </h2>
            <form
              className="flex flex-col space-y-4"
              onSubmit={(e) => {
                e.preventDefault();
                createSubjective(subjectiveName, selectedGoal);
                onClose();
              }}
            >
              <div className="mx-2 my-3">
                <label
                  className="mb-2 block text-sm  text-gray-700"
                  htmlFor="title"
                >
                  {" "}
                  Prompt{" "}
                </label>
                <input
                  className="focus:shadow-outline w-full appearance-none rounded border px-3 py-2 text-sm leading-tight text-gray-700 shadow-sm focus:outline-none"
                  id="subjectiveName"
                  type="text"
                  placeholder="Enter subjective prompt"
                  onChange={(e) => setsubjectiveName(e.target.value)}
                  required
                />
              </div>

              <div className="mx-2 my-3">
                <label
                  className="mb-2 block text-sm  text-gray-700"
                  htmlFor="description"
                >
                  {" "}
                  Link to a Goal (Optional)
                </label>
                <GoalSelector
                  goals={goals}
                  selectedGoal={selectedGoal}
                  onSelect={setSelectedGoal}
                ></GoalSelector>
              </div>

              <div className="flex justify-end">
                <button
                  type="button"
                  className="mr-2 inline-flex items-center rounded-md border border-transparent bg-gray-600 px-4 py-2 text-sm font-semibold text-white focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 hover:bg-gray-700"
                  onClick={onClose}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="inline-flex items-center rounded-md border border-transparent bg-purple-600 px-4 py-2 text-sm font-semibold text-white focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 hover:bg-purple-700"
                >
                  Create Subjective
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

const SubjectiveFormModal = ({ onClose }: { onClose: () => void }) => {
  const createSubjective = api.metrics.createMetric.useMutation();

  const query = api.goals.getGoalOnly.useQuery();
  if (query.isLoading) return <p>loading...</p>;
  else if (query.isError) return <p>error</p>;
  const goals = query.data.goalData;

  const createSubjectiveFn = (
    subjectiveName: string,
    goalId: string | undefined
  ) => {
    if (goalId == undefined) {
      console.log(`Creating standalone subjective: ${subjectiveName}`);
      createSubjective.mutate({ prompt: subjectiveName });
    } else {
      console.log(
        `Creating habit with description ${subjectiveName} (linked to ${goalId})`
      );
      createSubjective.mutate({ prompt: subjectiveName, goalId: goalId });
    }
  };
  return (
    <SubjectiveForm
      goals={goals}
      onClose={onClose}
      createSubjective={createSubjectiveFn}
    ></SubjectiveForm>
  );
};

export default SubjectiveFormModal;
