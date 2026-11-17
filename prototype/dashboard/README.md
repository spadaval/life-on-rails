# Dashboard prototype

Each design direction's dashboard on the same made-up data, switched by the Version
control in the corner. Mission lor-2803, item lor-5uxk. Nothing here talks to LoR: the data
is a frozen snapshot, every change stays in the tab, and a reload starts over.

```
pnpm proto:dev     # http://127.0.0.1:3859, hot reload
pnpm proto:build   # dist/, with .lor-stamp; scripts/lab/publish.sh serves it at /lab/dashboard/
node scripts/shots.mjs --catalogue shots/prototype.json   # screenshots, with proto:dev running
```

## Links

Everything is in the hash, so a link names a whole state: `#/<version>/<path>?<params>`.

- `#/current/tasks?id=Book%20flights%20to%20Lisbon` opens a version's page with its own parameters.
- `scene=` picks the data and clock: `afternoon` (default: 15:00, a timer 47 minutes in, two
  flags), `morning` (09:00, the brief, three flags), `quiet` (nothing flagged, running or due),
  `empty` (09:00, nothing recorded). Changing the scene starts it over.
- `still=1` stops the clock (for screenshots). `shell=0` hides the Version control.

Those three stay on every link. An unknown version opens `current`.

## Layout

- `src/data/`: shared by every direction. `snapshot.json` (do not edit; `snapshot.ts` remakes it),
  `store.ts` (the state, every action, undo, the clock, the scenes) and `derive.ts` (the lists
  Today, Tasks and search need).
- `src/shell/`: shared. The hash route (`nav.ts`: `useRoute`, `href`, `navigate`) and the
  Version control. It links only the showing direction's stylesheets.
- `src/directions/<name>/`: one direction. Its own tokens, components, screens and CSS; it
  imports nothing from another direction. `current` is the shipped dashboard, copied from
  `src/routes` and `src/components` at 48477ea with its server calls pointed at the store.

## Adding a direction

1. `cp -r src/directions/template src/directions/<name>` (the template is a one-page example
   of the whole contract and shows in the dropdown only under `proto:dev`).
2. Add `{ id, name, load }` to `src/directions/index.ts`.
3. Export `{ css: [url], App }` from its `index.ts`, with `import css from "./styles.css?url"`.
   `App` gets the route; read data with `useStore()` and the helpers in `derive.ts`, change it
   with the store's actions, and offer Undo with `useUndo()` and `undo()` where LoR can reverse
   the change.
4. Add its shots to `shots/prototype.json` under its version's name.

## Remaking the snapshot

`node_modules/.bin/tsx prototype/dashboard/snapshot.ts [--day YYYY-MM-DD]` empties the test
database, loads the demo set at that day, and reads it through LoR's own read functions at
15:00 (with the `running` variant) and 09:00. It refuses any database but `life_on_rails_test`
and leaves it on the base variant. Ids change on every run, so links name things by title.
