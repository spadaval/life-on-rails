// Stand-ins for src/server/functions/memory.ts (People and search) over the in-memory store.
import { search } from "../../../../data/derive.js";
import * as store from "../../../../data/store.js";
import { getState, today, TZ } from "../../../../data/store.js";

type Data<T> = { data: T };

export function getPeopleFn({ data }: Data<{ filter?: string; q?: string }>) {
  const s = getState();
  const filter = data.filter ?? "important";
  const ids = s.peopleLists[filter] ?? s.peopleLists.all ?? [];
  const q = data.q?.trim().toLowerCase() ?? "";
  const people = ids
    .map((id) => s.people.find((p) => p.id === id))
    .filter((p): p is NonNullable<typeof p> => !!p)
    .filter((p) => !q || `${p.name} ${p.identifiers.map((i) => i.value).join(" ")}`.toLowerCase().includes(q));
  return { people, total: people.length, shown: people.length };
}

export function getPersonFn({ data }: Data<{ id: string }>) {
  const s = getState();
  const hub = s.hubs[data.id];
  if (!hub) throw new Error("Nobody by that id in the demo set.");
  const waiting = s.nodes
    .filter((n) => n.waiting_on_entity_id === data.id && n.status !== "done" && n.status !== "archived")
    .sort((a, b) => (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.created_at.localeCompare(b.created_at))
    .map((n) => ({ id: n.id, title: n.title, status: n.status as string, due: n.due_local }));
  return {
    entity: hub.entity,
    person: s.people.find((p) => p.id === data.id) ?? null,
    page: hub.page,
    waiting,
    today: today(),
    timezone: TZ,
  };
}

export function setPersonFn({ data }: Data<{ id: string; priority?: boolean; keepInTouchDays?: number | null }>) {
  try {
    store.setPerson(data.id, { priority: data.priority, keepInTouchDays: data.keepInTouchDays });
    return Promise.resolve();
  } catch (err) {
    return Promise.reject(err);
  }
}

/** The real search reads Postgres; this one reads the snapshot (src/data/derive.ts). */
export function searchFn({ data }: Data<{ q: string }>) {
  return { results: search(getState(), data.q) };
}
