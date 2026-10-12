import { InfoIcon, Loader2 } from "lucide-react";
import { useState } from "react";
import { Card, CardHeader, CardTitle } from "../components/ui/card";
import { useRouter } from "next/router";

import Image from "next/image";
import { Button } from "../components/ui/button";
import { api } from "../utils/api";
import { AiOutlineCheck } from "react-icons/ai";
export const templates = [
  {
    key: "fitness",
    title: "Fitness",
    image: "/illustrations/personal-training.svg",
    description:
      "Achieve your fitness goals with a collection of fitness habits and goals.",
  },
  {
    key: "productivity",
    title: "Productivity",
    image: "/illustrations/factory.svg",
    description:
      "Become more productive with a bunch of productivity habits and goals.",
  },
  {
    key: "peace",
    title: "Peace",
    image: "/illustrations/mindfulness.svg",
    description:
      "Make your life calmer and more mindful with these habits and goals.",
  },
];

enum ModalState {
  None,
  Creating,
  Done,
}

const actionButtonLabel = (state: ModalState) => {
  switch (state) {
    case ModalState.None:
      return "Create";
    case ModalState.Creating:
      return <Loader2 className="h-6 w-6 animate-spin" />;
    case ModalState.Done:
      return <AiOutlineCheck />;
  }
};

const TemplateModal = ({
  templateKey,
  cancel,
}: {
  templateKey: string;
  cancel: () => void;
}) => {
  const [state, setState] = useState<ModalState>(ModalState.None);
  const router = useRouter();
  const context = api.useContext();
  const createFromTemplate = api.templates.createFromTemplate.useMutation({
    onSettled() {
      void context.invalidate();
    },
  });

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-gray-900 bg-opacity-75">
      <div className="rounded-lg bg-white p-5">
        <div className="text-md mb-5 font-semibold">
          You are about to use the{" "}
          <span className="bg-gray-200 font-mono">{templateKey}</span> template.{" "}
        </div>
        <div className="font-light text-gray-500">
          This will create multiple goals, habits, and metrics. This operation
          is irreversible.
        </div>
        <div className="mt-2 whitespace-nowrap align-baseline font-light text-gray-500">
          <InfoIcon className="inline"></InfoIcon> You can delete these items
          from the `All Items` page at any time.
        </div>{" "}
        <div className="mt-4 flex justify-end gap-x-2">
          <Button variant="ghost" onClick={cancel}>
            Cancel
          </Button>

          <Button
            variant="default"
            disabled={state === ModalState.Done}
            onClick={async () => {
              setState(ModalState.Creating);
              await createFromTemplate.mutateAsync({ key: templateKey });
              setState(ModalState.Done);
              await router.push({ pathname: "/overview" });
            }}
          >
            {actionButtonLabel(state)}
          </Button>
        </div>
      </div>
    </div>
  );
};

export const TemplatesList = () => {
  const [templateKey, setTemplateKey] = useState<string | undefined>(undefined); // New state variable for selected template key

  return (
    <>
      <div className="relative mt-10 flex flex-col items-center gap-y-5 rounded-xl">
        {templates.map((template) => {
          return (
            <Card
              className="h-[156px] w-2/3 cursor-pointer hover:shadow-md"
              key={template.key}
              // onClick={() => createFromTemplate.mutate({ key: template.key })}
              onClick={() => setTemplateKey(template.key)}
            >
              <div className="relative flex h-full flex-row items-center">
                <div className="flex h-[156px] w-[156px] flex-col flex-none items-center justify-center bg-gray-200 text-white">
                  <Image
                    width="120"
                    height="120"
                    src={template.image}
                    alt=""
                  ></Image>
                </div>
                <CardHeader className="space-y-3">
                  <CardTitle>{template.title}</CardTitle>
                  <p className="text-md text-gray-500">
                    {template.description}
                  </p>
                </CardHeader>
              </div>
            </Card>
          );
        })}
      </div>
      {templateKey && (
        <TemplateModal
          templateKey={templateKey}
          cancel={() => setTemplateKey(undefined)}
        ></TemplateModal>
      )}
    </>
  );
};

export const TemplatesPage = () => {
  return (
    <div className="mx-auto max-w-screen-lg px-4 py-8">
      <h1 className="my-5 text-2xl font-bold">Templates</h1>
      <p className="text-gray-600">
        Feeling overwhelmed? These templates can get you started on just about
        anything.
      </p>
      <TemplatesList />
    </div>
  );
};

export default TemplatesPage;
