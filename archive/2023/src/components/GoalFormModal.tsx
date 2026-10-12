import { useState } from "react";
import { api } from "../utils/api";

interface GoalFormProps {
  onClose: () => void;
  createGoal: (goalName: string) => void;
}

function GoalForm({ onClose, createGoal }: GoalFormProps) {
  const [goalName, setGoalName] = useState("");

  return (
    <div className="fixed inset-0 z-10 bg-black bg-opacity-40">
      <div className="flex min-h-screen items-center justify-center">
        <div className="mx-auto max-w-xl bg-white px-4 py-6 sm:px-6 lg:px-8">
          <div className="mb-8 ">
            <h2 className="mb-2 text-center text-2xl font-semibold">
              Create Goal
            </h2>
            <form
              className="flex flex-col space-y-4"
              onSubmit={(e) => {
                e.preventDefault();
                createGoal(goalName);
                onClose();
              }}
            >
              <div className="mx-2 my-3">
                <label
                  className="mb-2 block text-sm  text-gray-700"
                  htmlFor="title"
                >
                  {" "}
                  Description{" "}
                </label>
                <input
                  className="focus:shadow-outline w-full appearance-none rounded border px-3 py-2 text-sm leading-tight text-gray-700 shadow-sm focus:outline-none"
                  id="goalName"
                  type="text"
                  placeholder="Enter goal description"
                  onChange={(e) => setGoalName(e.target.value)}
                  required
                />
              </div>

              <div className="flex justify-end">
                <button
                  type="button"
                  className="mr-2 inline-flex items-center rounded-md border border-transparent bg-gray-600 px-4 py-2 text-sm font-semibold text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2"
                  onClick={onClose}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="inline-flex items-center rounded-md border border-transparent bg-green-600 px-4 py-2 text-sm font-semibold text-white hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-offset-2"
                >
                  Create Goal
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

const GoalFormModal = ({ onClose }: { onClose: () => void }) => {
  const createGoal = api.goals.createGoal.useMutation();

  const createGoalFn = (goalName: string) => {
    createGoal.mutate({ name: goalName });
  };

  return <GoalForm onClose={onClose} createGoal={createGoalFn}></GoalForm>;
};

export default GoalFormModal;
