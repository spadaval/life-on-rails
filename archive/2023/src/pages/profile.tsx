import type { ScoringFormat } from "@prisma/client";
import { format } from "date-fns";
import range from "lodash/range";
import { Loader2 } from "lucide-react";
import { useState } from "react";
import { AiOutlineCheck } from "react-icons/ai";
import { RxRocket } from "react-icons/rx";
import { Button } from "../components/ui/button";
import { Dialog, DialogContent, DialogTrigger } from "../components/ui/dialog";
import { HelpIcon } from "../components/ui/help-icon";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { api } from "../utils/api";
import { UserProfile } from "@clerk/nextjs";

const ProfilePage = () => {
  const context = api.useContext();
  const updateScoringWeeks = api.profile.updateScoringWeeks.useMutation({
    onSuccess() {
      void context.profile.invalidate();
    },
  });
  const updateScoringUnit = api.profile.updateScoringUnit.useMutation({
    onSuccess() {
      void context.profile.invalidate();
    },
  });
  const query = api.profile.getProfile.useQuery();

  if (query.isLoading)
    return (
      <div className="flex h-screen items-center justify-center">
        <RxRocket className="animate-spin text-2xl"></RxRocket>
      </div>
    );
  if (query.isError) return <p>Error</p>;

  const profile = query.data;
  const joinDate = format(profile.createdAt, "MM/dd/yyyy hh:mm a");

  // joinDate.toString();
  return (
    <>
      <UserProfile path="/profile" routing="path" />

      <div className="flex justify-center">
        <div className="w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
          <div className="mt-8 overflow-hidden rounded-sm md:col-span-2">
            <div className="flex items-center justify-between px-4 py-4 sm:px-6 ">
              <h3 className="text-lg font-medium leading-3 text-gray-700">
                User Preferences
              </h3>
            </div>

            <div className="px-4 pt-3 sm:px-6">
              <dl className="grid grid-cols-1 gap-x-4 gap-y-8 sm:grid-cols-2">
                <div className="sm:col-span-1">
                  <div className="mb-4 flex flex-row items-center gap-2 text-sm font-medium text-gray-500">
                    <span>Scoring Format</span>
                    <HelpIcon>How scores should be formatted.</HelpIcon>
                  </div>
                  <Select
                    onValueChange={(value) => {
                      updateScoringUnit.mutate({
                        scoringUnit: value as ScoringFormat,
                      });
                    }}
                    defaultValue={profile.scoringUnit}
                  >
                    <SelectTrigger className="h-fit w-fit outline-none">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Percentage">
                        <div className="px-4 py-2">
                          <span className="text-xs uppercase text-gray-700">
                            Percentage
                          </span>
                          <br></br>
                          <span className="text-md text-black">
                            Represent scores as percentages (0-100%)
                          </span>
                        </div>
                      </SelectItem>
                      <SelectItem value="Normalized">
                        <div className="px-4 py-2">
                          <span className="text-xs uppercase text-gray-700">
                            Normalized
                          </span>
                          <br></br>
                          <span className="text-md text-black">
                            Normalize scores to a range of 0-1.
                          </span>
                        </div>
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="sm:col-span-1">
                  <div className="mb-4 flex flex-row items-center gap-2 text-sm font-medium text-gray-500">
                    <span>Scoring Time Horizon</span>
                    <HelpIcon>
                      The number of weeks used to calculate scores. <br /> A
                      smaller number of weeks will bias the scores towards more
                      recent data,
                      <br /> while longer numbers will better reflect older
                      trends.
                    </HelpIcon>
                  </div>
                  <Select
                    onValueChange={(value) => {
                      updateScoringWeeks.mutate({
                        scoringWeeks: parseInt(value),
                      });
                    }}
                    defaultValue={profile.scoringWeeks.toString()}
                  >
                    <SelectTrigger className="w-[180px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {range(2, 13).map((value) => (
                        <SelectItem key={value} value={value.toString()}>
                          <span className="font-semibold">{value}</span> weeks
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="sm:col-span-1">
                  <div className="mb-4 flex flex-row items-center gap-2 text-sm font-medium text-gray-500">
                    <Dialog>
                      <DialogTrigger>
                        <Button variant="destructive">Reset Account</Button>
                      </DialogTrigger>
                      <DialogContent>
                        <ResetAccountModal />
                      </DialogContent>
                    </Dialog>
                    <Dialog>
                      <DialogTrigger>
                        <Button variant="secondary">
                          Generate demo data
                        </Button>
                      </DialogTrigger>
                      <DialogContent>
                        <GenerateDataModal/>
                      </DialogContent>
                    </Dialog>
                  </div>
                </div>
              </dl>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

enum ModalState {
  None,
  Creating,
  Done,
}

const actionButtonLabel = (text: string, state: ModalState) => {
  switch (state) {
    case ModalState.None:
      return text;
    case ModalState.Creating:
      return <Loader2 className="h-6 w-6 animate-spin" />;
    case ModalState.Done:
      return <AiOutlineCheck />;
  }
};

const ResetAccountModal = () => {
  const context = api.useContext();
  const resetAccount = api.profile.resetAccount.useMutation({
    onSettled() {
      void context.invalidate();
    },
  });
  const [state, setState] = useState<ModalState>(ModalState.None);

  return (
    <div className="rounded-lg bg-white p-5">
      <div className="text-md mb-5 font-semibold">
        You are about to reset all data on your account.
      </div>
      <div className="font-light text-gray-500">
        This will delete all of your goals, habits, and metrics, and all
        associated completions. This operation is irreversible.
      </div>
      <div className="mt-4 flex justify-end gap-x-2">
        <Button
          variant="default"
          onClick={async () => {
            setState(ModalState.Creating);
            await resetAccount.mutateAsync();
            setState(ModalState.Done);
          }}
        >
          {actionButtonLabel("Reset account", state)}
        </Button>
      </div>
    </div>
  );
};

const GenerateDataModal = () => {
  const context = api.useContext();
  const generateData = api.profile.generateTestData.useMutation({
    onSettled() {
      void context.invalidate();
    },
  });
  const [state, setState] = useState<ModalState>(ModalState.None);

  return (
    <div className="rounded-lg bg-white p-5">
      <div className="text-md mb-5 font-semibold">
        You are about to generate fake habit completions.
      </div>
      <div className="font-light text-gray-500">
        This will fill your instance with fake habit and metric completions
        (which make not be entirely sound).
      </div>
      <div className="mt-4 flex justify-end gap-x-2">
        <Button
          variant="default"
          onClick={async () => {
            setState(ModalState.Creating);
            await generateData.mutateAsync();
            setState(ModalState.Done);
          }}
        >
          {actionButtonLabel("Generate data", state)}
        </Button>
      </div>
    </div>
  );
};

export default ProfilePage;
