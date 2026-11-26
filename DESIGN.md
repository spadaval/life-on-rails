---
name: Life on Rails
description: "Elite: Mostly Harmless. A star atlas worn in Elite: Dangerous's menu language, turned down. Dark only."
colors:
  bg: "#0c0d12"
  well: "#0f1016"
  panel: "#111218"
  raised: "#181920"
  line: "#1e2029"
  edge: "#3a3634"
  frame: "#4a2e14"
  txt: "#ece6dc"
  mute: "#b1a596"
  dim: "#928777"
  or: "#f08a2c"
  or-deep: "#9a5c22"
  cy: "#6cc6ec"
  bad: "#e0735a"
typography:
  display:
    fontFamily: "Saira Condensed, Saira, ui-sans-serif, sans-serif"
    fontSize: "56px"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "0.03em"
  title:
    fontFamily: "Saira Condensed, Saira, ui-sans-serif, sans-serif"
    fontSize: "22px"
    fontWeight: 600
    lineHeight: 1.5
    letterSpacing: "0.08em"
  lead:
    fontFamily: "Saira, ui-sans-serif, system-ui, sans-serif"
    fontSize: "18px"
    fontWeight: 500
    lineHeight: 1.375
  body:
    fontFamily: "Saira, ui-sans-serif, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.5
    fontFeature: "tnum"
  section:
    fontFamily: "Saira Condensed, Saira, ui-sans-serif, sans-serif"
    fontSize: "15px"
    fontWeight: 600
    lineHeight: 1.5
    letterSpacing: "0.07em"
  small:
    fontFamily: "Saira, ui-sans-serif, system-ui, sans-serif"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Saira Condensed, Saira, ui-sans-serif, sans-serif"
    fontSize: "13px"
    fontWeight: 500
    lineHeight: 1.5
    letterSpacing: "0.07em"
  button:
    fontFamily: "Saira, ui-sans-serif, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 500
    lineHeight: 1.6
rounded:
  none: "0px"
  sm: "4px"
  check: "2px"
  dot: "9999px"
spacing:
  hair: "1px"
  xs: "6px"
  sm: "8px"
  md: "12px"
  lg: "20px"
  gutter: "22px"
  page: "30px"
  rail: "208px"
components:
  button-primary:
    backgroundColor: "{colors.or}"
    textColor: "#1a0d02"
    typography: "{typography.button}"
    rounded: "{rounded.sm}"
    padding: "4px 12px"
  button-primary-hover:
    backgroundColor: "#ff9c45"
    textColor: "#1a0d02"
  button-primary-sm:
    backgroundColor: "transparent"
    textColor: "{colors.or}"
    rounded: "{rounded.sm}"
    padding: "0 8px"
    height: "24px"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.txt}"
    typography: "{typography.button}"
    rounded: "{rounded.sm}"
    padding: "4px 12px"
  button-quiet:
    backgroundColor: "transparent"
    textColor: "{colors.mute}"
    typography: "{typography.button}"
    rounded: "{rounded.sm}"
    padding: "4px 12px"
  button-sm:
    rounded: "{rounded.sm}"
    padding: "0 8px"
    height: "24px"
  button-danger:
    backgroundColor: "transparent"
    textColor: "{colors.bad}"
    rounded: "{rounded.sm}"
    padding: "4px 12px"
  input:
    backgroundColor: "{colors.well}"
    textColor: "{colors.txt}"
    rounded: "{rounded.sm}"
    padding: "4px 10px"
    height: "34px"
  checkbox:
    backgroundColor: "{colors.well}"
    rounded: "{rounded.check}"
    size: "15px"
  chip:
    backgroundColor: "transparent"
    textColor: "{colors.mute}"
    rounded: "{rounded.sm}"
    padding: "0 6px"
    height: "20px"
  panel:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.txt}"
    rounded: "{rounded.sm}"
    padding: "16px 20px"
  panel-framed:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.txt}"
    rounded: "{rounded.none}"
    padding: "16px 20px"
  nav-item:
    backgroundColor: "transparent"
    textColor: "{colors.mute}"
    typography: "{typography.section}"
    padding: "0 22px"
    height: "34px"
  nav-item-active:
    backgroundColor: "{colors.or}"
    textColor: "#1a0d02"
    typography: "{typography.section}"
    padding: "0 22px"
    height: "34px"
  tag-agent:
    backgroundColor: "transparent"
    textColor: "{colors.cy}"
---

# Design System: Life on Rails

## Overview

**Creative North Star: "Elite: Mostly Harmless"**

A star atlas worn in the menu language of Elite: Dangerous, with the volume turned down. The waking day is a route you travel along; goals are stars you steer by. Space-black ground, panels a tone lighter than it, the thin orange-brown frame with clipped corners kept for what is open or live, condensed capitals for the instrument labels and plain sentence-case Saira for everything a person reads. It is a cockpit that never raises its voice: no alarms, nothing pulsing for attention, no fake telemetry, nothing red for falling behind. Like Elite's own panels it has depth: translucent glass over what lies behind, soft shadows under what floats, and, where it reads best, a glow on what is selected or now.

Density is operational, not airy: a fixed 208px left rail, a 62px page head, then panels in a 22px-gutter grid. One amber-orange carries every meaning of "now, running, tracked, selected"; one cyan carries "planned" and "the agent". Everything else is warm grey on blue-black. The two signature drawings, the day arc on Today and the star map of the wiki in Notes, are the only places with the engraved grid and the only places with reticles.

It refuses the stretched single phone column and the grey-card planner. Dark only; there is no light theme and none is to be built.

**Key Characteristics:**
- Space black with a cool undertone, panels one step lighter, wells one step darker. Panels are told apart by that tone, not by a border.
- The orange-brown 1px frame with 6px chamfers marks what is open or live (an open editor, the selected block, a dialog), at most two on a screen; everything else takes a small 4px radius.
- Amber-orange means now / running / tracked / selected; cyan means calendar and agent.
- Saira Condensed caps for titles, nav, section heads, field labels, big readouts; Saira sentence case for the rest, buttons included.
- Four-point stars and corner-bracket reticles as the only ornament.
- Under target is dim, never red.

## Colors

A near-monochrome blue-black instrument panel with warm grey type, lit by one amber accent and one cyan secondary, each with a fixed job.

### Primary
- **Drive Amber** (`or`): now, the running timer, tracked time on the arc, selection, the active nav bar, primary buttons, checked checkboxes, checkbox accent, list markers in pages, the focus outline, the brand wordmark. As a fill it appears only on the active nav item, the primary button of a panel or toolbar, and a checked checkbox; elsewhere it is a line, a glyph, text, or a low-alpha tint.
- **Banked Amber** (`or-deep`): confirmed (committed) blocks in the Actual lane, where a solid but quieter orange is needed so committed time does not shout like the running block.

### Secondary
- **Nav-Beacon Cyan** (`cy`): planned time (calendar events as cyan diamonds on the arc, cyan-edged blocks in the Planned lane, event chips) and the agent (the `agent` tag, agent-sourced timeline rows). Never used for selection or for "now".

### Tertiary
- **Hull-Breach Coral** (`bad`): failures only: a job that is failing, a dead database, the danger button, the health dot when a sense is down. Never for overdue, under target, or anything the owner did or didn't do.

### Neutral
- **Deep Space** (`bg`): page ground, and the fill inside cyan diamonds so the arc reads through them.
- **Instrument Well** (`well`): recessed surfaces: inputs, the star map, timeline tracks, code blocks.
- **Panel Face** (`panel`): the tone of every panel, framed or not, and of inline forms.
- **Raised Plate** (`raised`): row hover, table header cells, inline code.
- **Engraved Line** (`line`): dividers between rows, page-head rule, meter track, the arc baseline.
- **Bulkhead Edge** (`edge`): borders of secondary buttons, inputs, checkboxes, chips, segmented controls; scrollbar thumb.
- **Frame Bronze** (`frame`): the outer frame of framed panels, the rail and mobile-bar rules, the dashed untracked part of the arc, unlit star-map links.
- **Starlight** (`txt`): primary text.
- **Dust** (`mute`): secondary text, panel asides, unlit labels on the map, inactive nav.
- **Far Dust** (`dim`): tertiary text: empty states, footnotes, field labels, hour ticks on the arc, done tasks.

### Named Rules
**The Colour Has Jobs Rule.** Amber is now, running, tracked and selected. Cyan is planned and agent. Coral is broken. No colour is used for decoration or for categories.

**The Solid Fills Rule.** As a filled control, solid amber appears on the active nav item and on at most one primary button per panel or toolbar (Start timer in the page head, Save in an open editor). A small primary in a row (Done, Did it, Yes) is an amber outline instead, so a list does not fill with amber. (Data marks, such as tracked time on the arc and the phone bar, and checked checkboxes do not count.) Every other selection is a quiet amber tint (8% alpha) with a 1px amber inset edge (55% alpha).

**The Never Red Rule.** Under target, behind, overdue and unstarted are shown dim (`mute` / `dim`), never coral. Coral is reserved for machinery that has failed.

### Over the wallpaper (the phone's home)
The launcher on the phone draws some things straight on Sumeet's wallpaper: app and folder labels, the date header and its next-event line, and the page dots (a folder's tile is a bare 2×2 of its apps' icons, with nothing drawn in ink but its label). These are the one exception, by Sumeet's decision (2026-11-06), to "dark only", because the wallpaper is his and may be light:
- **Light ink** (the default): Starlight text and Dust asides with a still text shadow (1dp down, 3dp blur, black at 60%). The shadow is for legibility over a photo, never for emphasis.
- **Dark ink**, when Android says the wallpaper supports dark text (`WallpaperColors.HINT_SUPPORTS_DARK_TEXT`): Deep Space for text and asides alike, no shadow. The darkest wallpaper with that hint is about `#858585`, where Deep Space reads at about 5.3:1 and a grey aside wouldn't.
- The hint describes the wallpaper as a whole, so a light patch on a dark wallpaper (or a dark one on a light wallpaper) is not guaranteed to read; the shadow carries light ink there.
- The system bar icons follow the same choice on home's pages: dark over a light wallpaper, light on the agent page, the drawer and an open folder.
- Everything LoR draws as a surface (the drawer, folders, cards, menus, settings) stays dark on any wallpaper.
- The agent page sits on a veil of Deep Space at 92% over the wallpaper, enough for Far Dust to read at 4.5:1 even over white, so it doesn't follow the wallpaper.

## Typography

**Display Font:** Saira Condensed (500, 600), falling back to Saira, then ui-sans-serif
**Body Font:** Saira (400, 500, 600), falling back to ui-sans-serif, system-ui
**Label/Mono Font:** Saira Condensed caps for labels; no monospace face

**Character:** One family in two widths. The condensed caps are the ship's instrument lettering; the regular width is the logbook. Tabular numerals are on globally so every readout and hour column aligns.

### Hierarchy
Text takes four sizes, each with one job. They are tokens in `src/styles.css` (`text-small`, `text-body`, `text-lead`, `text-title`), and `tests/look.test.ts` fails on a literal pixel size in `src/routes` or `src/components`.
- **Small, 13px** (`text-small`): small print. Metadata under a title, panel asides, counts, field labels (in condensed caps, `dim`), table heads, chips, lane labels, keyboard hints, footnotes, and small row buttons.
- **Body, 15px** (`text-body`, the page default): everything read. List rows, notes and descriptions, page head subs, inputs and selects, buttons, rendered pages (capped at 72ch, relaxed leading), and the condensed caps section heads and nav.
- **Lead, 18px** (`text-lead`): emphasis. The open task's title in its editor, the brand wordmark, a rendered page's h2.
- **Title, 22px** (`text-title`): the page title in the page head, and a rendered page's h1.

The arc's readout is the one place with sizes of its own: the display figure (Saira Condensed 600, 56px rising to 62px at md, line-height 1, 0.03em, uppercase), its unit word ("remaining") in 20px condensed amber, and the running timer's 21px figure. Labels drawn inside the arc's SVG and the star map stay in its own units.

### Named Rules
**The Condensed Caps Are Instruments Rule.** Condensed caps are for page titles, nav, section heads, field labels and big readouts. Nothing else. Buttons, sentences, task and goal names in lists stay sentence case Saira. The one exception is goal names on the star map, which are labelled like stars.

**The Tabular Rule.** Numerals are always tabular; time and hours never jitter.

## Layout

Desktop first. A two-column shell at md and up: a sticky full-height left rail (208px) with the amber wordmark, a live date-time stamp, the nav (34px rows: Today, Tasks, Goals, People, Notes), and system health pinned to its foot; then the main column with 30px side padding. Below md the rail becomes a horizontally scrolling top bar of condensed nav links, the active one in amber text instead of a fill, and main padding drops to 16px.

Every page opens with the page head: min 62px tall, a `line` rule beneath, the 22px title then a 15px muted sub, and the global actions (search input, 300px, from lg; the Start timer primary or the running-timer pill) pushed right. Page sections stack at 20-22px. Panel grids use a 22px gutter: Today's three panels at 1.22fr / 1fr / 1.08fr from lg; Tasks, Goals and the Notes graph pair a fluid main column with a 360-440px sticky side panel from xl; Notes pairs a 300px index with the reading column from lg.

The day arc is a 1200x300 drawing capped at 1240px wide; on phones it is replaced by a straight 12px bar with Rising and Setting under it. Planned/Actual lanes use a 66px label column and 26px tracks with a faint 24-hour rhythm. The star map fills its panel at 62vh, or the viewport height minus the page chrome on xl.

Spacing rhythm: 6 / 8 / 12 / 20 / 22 / 30px. Panels pad 16px top and bottom, 20px sides. List rows are separated by `line` rules with 8-10px vertical padding.

## Elevation & Depth

Depth is tonal first: `well` below the page, `bg` as the ground, `panel` a step above it for every panel, `raised` for hover. The 1px `frame` border with chamfers is not depth but emphasis: it marks the one or two panels that are open or live (see Cards / Containers). On top of that, as Elite's panels do (Sumeet, 2026-11-07, replacing the earlier flat rule, which went further than the look chosen in `docs/design/README.md`):
- **Blur as a backdrop.** Something that opens over other content (a sheet, a menu, a dialog, the drawer, an open folder, a translucent strip like the star map's legend) may blur what is behind it, so it reads as glass in front rather than a hole cut through. The panel's face stays dark enough that its text keeps 4.5:1 over the lightest unblurred content that can sit behind it; where blur isn't available (an old browser, a phone without cross-window blur, battery saver) the same face over a dim must look right on its own.
- **Shadows under what floats.** A soft, dark shadow under something lifted above the page (a menu, a dialog, a sheet, an icon being dragged) shows what sits on what. Shadows are black, low and wide, never coloured; things that lie in the page (panels, rows, cards) cast none. Light text's shadow over the phone's wallpaper is for legibility, not depth (see Over the wallpaper).
- **Glow is one way to highlight.** Glow is a functional element, not a finish: where it is used, it marks where you are or what is live, so the eye finds it at a glance. It is one choice among several, not the default for every highlight: the amber tint with its 1px edge, a solid fill, the reticle, colour and weight all single things out too, and most selections and focus states use those. Reach for glow where it reads best (a star on the map, now on the arc, the running timer), in the colour that already means it (amber for selected and now, cyan for the agent and planned). A screen rarely needs more than one or two. Never on body text, never as decoration or to make a screen look rich, never animated.
- **Values.** Float shadow `0 8px 24px rgb(0 0 0 / 0.5)` (on the phone, 8dp elevation's ambient shadow in black). Backdrop blur 24px (24dp), behind a `panel` face at 72%, raised to whatever the 4.5:1 above needs. Glow `0 0 12px` in `or` or `cy` at 40%.

### Named Rules
**The Functional-Glow Rule.** Every glow answers "which one?": the selection, the focus, what is running, now. Not every answer needs a glow; choose it where it reads better than a tint, an edge, a fill or the reticle. A glow that answers nothing is decoration and comes out. Nothing pulses, shimmers or flashes to get attention (see Motion).

**The Engraved-Grid Rule.** The faint 60px engraved grid (white at 2.5-3% alpha) sits behind the day arc and the star map only. Not behind panels, pages or lists.

## Shapes

Small radius, with the chamfer kept for emphasis (Sumeet asked for rounded corners on 2026-11-17, `docs/design/lab/brief.md`).
- **4px radius** (`rounded-sm`): plain panels, buttons, inputs, selects, textareas, chips, segmented controls, notices, inline forms, keyboard keys, the running-timer pill.
- **2px radius:** checkboxes.
- **The chamfer:** framed panels clip their top-left and bottom-right corners at 6px, drawn as a 1px `frame` outer shell with the `panel` face clipped to the same polygon inside it. Only framed panels and floating menus are chamfered; nothing inside them is.
- **Square:** data marks. Blocks on the Planned and Actual lanes, the meter, the arc's ticks and the selection's inset edge on a row.

Fully round shapes are reserved for small status dots (6px: the timer dot, the health dot) and for project and task bodies on the star map. The four-point star (a 20-unit path with a narrow waist) marks goals, flagged tasks and now; habits are small hollow squares. The reticle is four L-shaped corner brackets in 1.5px amber around the selected body or around now on the arc. Calendar events are 10px squares rotated 45 degrees.

Lines carry state: solid is confirmed, dashed is not yet confirmed or not yet travelled, a fainter dashed bronze is unlabelled phone or place time.

## Motion

Calm. Motion is one-shot and follows the finger: a thing moves because a hand moved it or a tap asked for it, and it stops. Nothing loops, pulses, shimmers or spins, and nothing moves on its own (a reread, an undo or a cold start puts things in place at once).

- **Feedback and fades:** 150ms (the colour transition above).
- **A thing taking its new place** (a tile, a folder from its tile, a settling icon): about 220ms, standard ease (fast out, slow in).
- **Whatever a finger lets go of** (the phone's drawer): it follows the finger 1:1, then settles with a spring that has no bounce, starting at the finger's speed.
- **Remove animations** (Android's setting, the animator duration scale at 0): every motion ends at once, and nothing is left part-way.

## Components

### Buttons
Three levels, sentence case, a 4px radius. Every button takes `.btn` and one level; `tests/look.test.ts` fails on a `.btn` without one.
- **Primary** (`.btn-primary`): solid amber fill and border, near-black brown text (#1a0d02), weight 600; hover lightens to #ff9c45. At most one per panel or toolbar: Start timer, Save in an editor, Create, Done in the bulk bar. In the small size it is an amber outline with amber text instead of a fill, for the main action of a row: Done on Needs you, Did it on a habit, "Yes, <label>" on a suggested block, Read on the graph.
- **Secondary** (`.btn-secondary`): transparent with a 1px `edge` border and `txt` text. Every other action that commits something: Add task, Send, Save times, Split, Reopen, Edit, the intent choices. Hover takes the border to amber at 70% and a 10% amber tint.
- **Quiet** (`.btn-quiet`): text only in `mute`, no border; hover goes to `txt` on `raised`. For what closes, dismisses or steps aside: Close, Seen, Pause, Cancel, Clear, dismiss, take back, Reset visibility.
- **Danger** (`.btn-secondary .btn-danger`): a coral secondary, not a fourth level. Coral text and a 60% coral border; hover full coral border and 10% coral tint. Delete only.
- **Size:** 15px Saira 500, 4px 12px, 34px tall, matching an input. Small (`.btn-sm`): 24px tall, 0 8px, 13px; for row actions and panel asides.
- **Pressed toggle:** a secondary with `aria-pressed="true"` (the Needs you filter, a clash's choice) takes an amber border and text.
- **Disabled:** 45% opacity, no hover.
- **Not buttons:** a row's title, a lane block, the timer's label and a goal's name are content that opens something; they are drawn as text. Tabs and segmented controls are navigation (see Navigation).

### Chips
- **Style:** 20px tall, 0 6px padding, 1px `edge` border, `mute` 13px text, 4px radius.
- **State:** calendar chips take a cyan border at 50% and cyan text. Free-time gap chips add a `dim` "fits ..." suffix.

### Cards / Containers
Panels are separated by tone, not by frame.
- **Plain panel** (`.panel`, `<Panel>`): the `panel` face on the ground, a 4px radius, no border, no shadow. The default for every panel.
- **Framed panel** (`.panel .panel-framed`, `<Panel framed>`): the same face inside a 1px `frame` shell with the 6px two-corner chamfer. Its job is to say "this is open, or live": the selected time block on Today, the open task in Tasks, the selected goal, project or habit in Goals, a page open for editing in Notes, the selected page beside the star map, the quick-add dialog. At most two on a screen; Today, Tasks, Goals and a person's page have one at most.
- **Inline forms** (new goal, new page, the timer's picker): the `panel` tone with a 4px radius, no border.
- **Internal Padding:** 16px vertical, 20px horizontal. A header row holds the condensed section title left and a 13px `mute` aside right (a count, a link, a unit like "hours / target").

### Floating menus
A menu opened by a press (on the phone, every long-press menu on home) is a chamfered panel that points at what opened it.
- **Shape:** the outer panel's 6px two-corner chamfer, with an arrow 22px wide and 10px tall out of the edge facing the source, drawn in the same 1px outline as the body.
- **Face:** `raised` inside a 1px `or-deep` outline, brighter than a panel's `frame` because the menu floats (the approved mock's colour); the float shadow of Elevation & Depth under it.
- **Place:** centred on the source's centre (the icon, the finger on empty space, the finger on a widget), kept 8px inside the screen. Below a source in the top half of the screen, above one in the bottom half, which is always the roomier side; a menu taller than that room is cut to it (never below about two rows) and scrolls. When the body is pushed in from an edge, the arrow's base stays on the body's straight edge and the arrow leans so its tip is still on the source.
- **Around it:** a light dim over the page (near-black at 35%), and a 1.5px amber ring round the pressed icon or widget. A tap anywhere outside, back, or a fold or rotation closes it.
- **Motion:** it opens with a 150ms fade and a slight scale out of the arrow's tip.
- **Content:** 15px `txt` rows (an app's shortcuts with their icons first); App info, Remove, Hide and Uninstall as a row of line glyphs over 11px `mute` labels; a 1px `line` rule between groups.

### Inputs / Fields
Every text field, select, date and time picker and textarea takes `.input`; checkboxes are styled wherever they appear.
- **Style:** `well` background, 1px `edge` border, 4px radius, 4px 10px padding, 15px `txt`, 34px tall like a button, `dim` placeholder.
- **Selects:** the browser's arrow is replaced by a small `dim` triangle 10px from the right, with 28px reserved for it; options sit on `raised`.
- **Date and time:** the picker glyphs are filtered to the bronze-orange of the frame; fields are wide enough for the whole value (136px for a time, 150px for a date).
- **Checkboxes:** a 15px `well` square with an `edge` border and a 2px radius; checked is a solid amber fill with a near-black tick; hover takes the border to amber.
- **Focus:** border to amber at 70%, no outline ring.

### Navigation
- **Rail:** condensed 15px 600 caps in `mute`, 34px rows with 22px side padding, hover to `txt`. Active is a full-width solid amber bar with near-black text. The Tasks item carries its open count right-aligned in sentence-case Saira at 80% opacity.
- **Health foot:** a 6px dot (amber when all senses report, coral otherwise) and a 13px `mute` sentence.
- **Mobile:** a top bar of condensed links, active in amber text, rule in `frame` below.
- **Segmented controls** (`.seg`: People's filters, Pages/Graph, a task's Mine/The agent's): adjacent segments in one 4px-radius `edge` outline, divided by 1px `edge` rules; the chosen one (`.active` or `aria-pressed`) takes the quiet amber selection treatment, not a fill.
- **Tabs** (Tasks' Open, Waiting, ...): text with a 2px amber underline on the chosen one.

### Selection row
The system's selection everywhere a list or block is chosen: 8% amber background with a 1px inset amber edge at 55%. Also used as the Notice strip (a status line with a "dismiss" link) and as the selected filter segment. Hovered unselected rows go to `raised`.

### Agent tag
Lowercase "agent" in cyan at 85%, 12px, with an underlined "undo" in `txt` when the row can be reverted. Never a pill or badge box.

### Meter
A 3px hairline track in `line` with an amber fill at 85% to the hours-against-target ratio, capped at full. Under target simply shows less amber; the label beside it goes `mute` when nothing is logged.

### Day Arc (signature)
The waking day as a half-ellipse route from Rising (left) to Setting (right) over a `line` baseline and the engraved grid. The untravelled route is a 1.5px dashed `frame` line; tracked blocks overlay it in amber: the running block 4.5px solid, confirmed 3.5px, unconfirmed 2.5px at 85%, bare phone/place time at 70%. Hour ticks cross the route in dark bronze (#6b4420), every second hour labelled in 13px condensed `dim`. Calendar events are cyan-outlined diamonds just outside the route with 14px cyan labels that skip any that would collide. Now is an amber reticle with a small four-point star and a 14px amber "HH:MM now" label. The display readout, its subline, and the running-timer card sit centred under the zenith. Beneath it, Planned and Actual lanes repeat the day as 26px tracks with a 1px amber now line.

### Star Map (signature)
The intent graph as a pannable, zoomable sky on `well` with the engraved grid, inside a plain panel. Goals are amber four-point stars sized 8-16px by this week's hours against target (unfed goals in a muddy bronze, #8a6a4a), named below in 15px condensed caps with an "h / target" line in `mute`. Projects are 7px `mute` dots, habits 7px hollow squares, tasks 4px `txt` dots, flagged tasks small amber stars; all linked to their parent by 1px `frame` lines. Selecting a body draws a reticle, lights its constellation's links in amber, dims every other goal, and frames the constellation; labels hold their screen size at any zoom. A legend sits bottom-left on a translucent well strip, secondary zoom buttons bottom-right. Beside it on xl, a System Info panel shows the selection's fields as a condensed-label definition list.

## Paper (the wall display)

The one place LoR is not dark: the TRMNL X on the fridge, e-paper read from across a kitchen. It is the same atlas printed rather than lit. Four grays only (black, `#555`, `#aaa`, white), no dithering, laid out at 1040x780 and rendered at 1.8x.

- **Today is the arc and one thing.** The waking day is the half-ellipse route filling the page: an imprecise clock (the reticle is now, the time it was drawn printed beside it) and the day's log. Left of now, tracked time in its goal's ink, a stretch of 40 minutes or more named outside the route. Right of now, events as hollow bands and deadlines as a black square across the route, each with its time. Inside the arc, the one thing to do, set large with a small kicker saying why it is there (pinned, running, an event, the brief, a flag) and a grey line under it. No countdown, no lists, no "Updated".
- **Goals have inks.** The four goals with the largest weekly targets each own one: black, `#555`, black dashed, `#aaa`. Any other goal is a thin black line; time under no goal is a dotted grey. The same ink marks a goal everywhere on paper: its stretches on the arc, its strip and day bars on Goals, the strip beside its tasks.
- **Goals** is a ledger, one row a goal: its ink, its name (the short title), its next task with how late it is, the last seven days as bars, hours against the weekly target with a meter. Goals without a target come last with only their next task.
- **Tasks** is one list by when they are due: Late (bold), Today, This week, Later, then the undated folded into two small grey columns. A flagged task has a star; a waiting one says who it waits on.
- **Colour jobs become ink jobs.** Coral (broken) is a reversed black chip in the head ("2 jobs need a look", "Battery 12%"); a late task is bold, never a chip. Dim is `#555`.
- **For distance:** nothing under 20px; the one thing is 32-44px.
- **Three screens, one at a time.** Today is home. Right and left on the touch bar flip through Today, Goals and Tasks as the device stored them; a middle tap redraws the one showing, fresh. A stepped-to screen gives way to Today at the next wake. Three small squares over the touch bar say which is up.
- Chart crosses stand in for the engraved grid, kept off the one thing.

## Do's and Don'ts

### Do:
- **Do** separate panels by tone, and keep the 1px `frame` shell and 6px chamfer for the one or two panels that are open or live.
- **Do** give every button one of the three levels, and size text with the four sizes.
- **Do** reserve solid amber for the active nav item and one primary button per panel or toolbar; mark every other selection with the 8% amber tint and 1px 55% amber inset edge.
- **Do** use cyan only for calendar (planned) time and for the agent.
- **Do** set page titles, nav, section heads, field labels and big readouts in Saira Condensed caps (0.07-0.08em tracking), and everything else, buttons included, in sentence-case Saira.
- **Do** show under-target and unstarted as dim text and less amber, never as an alert.
- **Do** use the four-point star and the corner-bracket reticle as the world's only glyphs for goals, flags, now and selection.
- **Do** keep numerals tabular.

### Don't:
- **Don't** add a light theme. The paper frame on the wall display is not one; it is a separate drawing. Dark text over a light wallpaper on the phone's home is not one either (see Over the wallpaper).
- **Don't** use scanlines, pulsing, coloured shadows, or glow that marks nothing (decoration, sparkle, a screen made glowy to look rich; see Elevation & Depth); the selected star's reticle is the strongest emphasis the world has.
- **Don't** put fake HUD numbers, decorative telemetry or invented coordinates on screen; every figure is real data.
- **Don't** use coral for overdue, behind, or under target; coral means a machine has failed.
- **Don't** round past 4px, or chamfer anything but a framed panel or a floating menu; the only fully round shapes are status dots and map bodies.
- **Don't** spread the engraved grid beyond the day arc and the star map.
- **Don't** stretch a single phone-width column across the desktop or fall back to grey rounded cards.
