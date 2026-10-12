import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/chat")({
  component: Chat,
});

function Chat() {
  return (
    <main className="mx-auto max-w-2xl p-6">
      <h1 className="text-3xl font-bold">Chat</h1>
      <p className="mt-2 text-sm text-neutral-500">Agent chat + voice PTT stub land in PR 8. Push-to-talk stays disabled until chat tools work.</p>
      <button className="mt-4 rounded border px-4 py-2 text-neutral-400" disabled title="Stub until PR 8">
        Push to talk (stub)
      </button>
    </main>
  );
}
