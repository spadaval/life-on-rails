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
  headline:
    fontFamily: "Saira Condensed, Saira, ui-sans-serif, sans-serif"
    fontSize: "22px"
    fontWeight: 600
    lineHeight: 1.5
    letterSpacing: "0.08em"
  title:
    fontFamily: "Saira Condensed, Saira, ui-sans-serif, sans-serif"
    fontSize: "15px"
    fontWeight: 600
    lineHeight: 1.5
    letterSpacing: "0.07em"
  body:
    fontFamily: "Saira, ui-sans-serif, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.5
    fontFeature: "tnum"
  body-small:
    fontFamily: "Saira, ui-sans-serif, system-ui, sans-serif"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Saira Condensed, Saira, ui-sans-serif, sans-serif"
    fontSize: "12.5px"
    fontWeight: 500
    lineHeight: 1.5
    letterSpacing: "0.07em"
  button:
    fontFamily: "Saira, ui-sans-serif, system-ui, sans-serif"
    fontSize: "13.5px"
    fontWeight: 500
    lineHeight: 1.5
rounded:
  none: "0px"
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
  button:
    backgroundColor: "transparent"
    textColor: "{colors.txt}"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: "4px 12px"
  button-primary:
    backgroundColor: "{colors.or}"
    textColor: "#1a0d02"
    typography: "{typography.button}"
    rounded: "{rounded.none}"
    padding: "4px 12px"
  button-primary-hover:
    backgroundColor: "#ff9c45"
    textColor: "#1a0d02"
  button-sm:
    backgroundColor: "transparent"
    textColor: "{colors.txt}"
    rounded: "{rounded.none}"
    padding: "0 8px"
    height: "24px"
  button-danger:
    backgroundColor: "transparent"
    textColor: "{colors.bad}"
    rounded: "{rounded.none}"
    padding: "4px 12px"
  input:
    backgroundColor: "{colors.well}"
    textColor: "{colors.txt}"
    rounded: "{rounded.none}"
    padding: "6px 10px"
  chip:
    backgroundColor: "transparent"
    textColor: "{colors.mute}"
    rounded: "{rounded.none}"
    padding: "0 6px"
    height: "20px"
  panel:
    backgroundColor: "{colors.panel}"
    textColor: "{colors.txt}"
    rounded: "{rounded.none}"
    padding: "16px 20px"
  nav-item:
    backgroundColor: "transparent"
    textColor: "{colors.mute}"
    typography: "{typography.title}"
    padding: "0 22px"
    height: "34px"
  nav-item-active:
    backgroundColor: "{colors.or}"
    textColor: "#1a0d02"
    typography: "{typography.title}"
    padding: "0 22px"
    height: "34px"
  tag-agent:
    backgroundColor: "transparent"
    textColor: "{colors.cy}"
---

# Design System: Life on Rails

## Overview

**Creative North Star: "Elite: Mostly Harmless"**

A star atlas worn in the menu language of Elite: Dangerous, with the volume turned down. The waking day is a route you travel along; goals are stars you steer by. Space-black ground, panels held in thin orange-brown frames with clipped corners, condensed capitals for the instrument labels and plain sentence-case Saira for everything a person reads. It is a cockpit that never raises its voice: no alarms, nothing pulsing for attention, no fake telemetry, nothing red for falling behind. Like Elite's own panels it has depth: translucent glass over what lies behind, soft shadows under what floats, and a glow marking what is selected or now.

Density is operational, not airy: a fixed 208px left rail, a 62px page head, then framed panels in a 22px-gutter grid. One amber-orange carries every meaning of "now, running, tracked, selected"; one cyan carries "planned" and "the agent". Everything else is warm grey on blue-black. The two signature drawings, the day arc on Today and the star map of the wiki in Notes, are the only places with the engraved grid and the only places with reticles.

It refuses the stretched single phone column and the grey-card planner. Dark only; there is no light theme and none is to be built.

**Key Characteristics:**
- Space black with a cool undertone, panels one step lighter, wells one step darker.
- Orange-brown 1px frames with 6px chamfers on outer panels; everything else square.
- Amber-orange means now / running / tracked / selected; cyan means calendar and agent.
- Saira Condensed caps for titles, nav, section heads, field labels, big readouts; Saira sentence case for the rest, buttons included.
- Four-point stars and corner-bracket reticles as the only ornament.
- Under target is dim, never red.

## Colors

A near-monochrome blue-black instrument panel with warm grey type, lit by one amber accent and one cyan secondary, each with a fixed job.

### Primary
- **Drive Amber** (`or`): now, the running timer, tracked time on the arc, selection, the active nav bar, the one primary button, checkbox accent, list markers in pages, the focus outline, the brand wordmark. As a fill it appears only on the active nav item and the one primary action per screen; elsewhere it is a line, a glyph, text, or a low-alpha tint.
- **Banked Amber** (`or-deep`): confirmed (committed) blocks in the Actual lane, where a solid but quieter orange is needed so committed time does not shout like the running block.

### Secondary
- **Nav-Beacon Cyan** (`cy`): planned time (calendar events as cyan diamonds on the arc, cyan-edged blocks in the Planned lane, event chips) and the agent (the `agent` tag, agent-sourced timeline rows). Never used for selection or for "now".

### Tertiary
- **Hull-Breach Coral** (`bad`): failures only: a job that is failing, a dead database, the danger button, the health dot when a sense is down. Never for overdue, under target, or anything the owner did or didn't do.

### Neutral
- **Deep Space** (`bg`): page ground, and the fill inside cyan diamonds so the arc reads through them.
- **Instrument Well** (`well`): recessed surfaces: inputs, the star map, timeline tracks, code blocks.
- **Panel Face** (`panel`): the face of framed panels and inline editors.
- **Raised Plate** (`raised`): row hover, table header cells, inline code.
- **Engraved Line** (`line`): dividers between rows, page-head rule, meter track, the arc baseline.
- **Bulkhead Edge** (`edge`): borders of buttons, inputs, chips, segmented controls; scrollbar thumb.
- **Frame Bronze** (`frame`): the outer frame of panels, the rail and mobile-bar rules, the dashed untracked part of the arc, unlit star-map links.
- **Starlight** (`txt`): primary text.
- **Dust** (`mute`): secondary text, panel asides, unlit labels on the map, inactive nav.
- **Far Dust** (`dim`): tertiary text: empty states, footnotes, field labels, hour ticks on the arc, done tasks.

### Named Rules
**The Colour Has Jobs Rule.** Amber is now, running, tracked and selected. Cyan is planned and agent. Coral is broken. No colour is used for decoration or for categories.

**The Two Solid Fills Rule.** As a filled control, solid amber appears at most twice per screen: the active nav item and the one primary action. (Data marks, such as tracked time on the arc and the phone bar, are drawn in amber and do not count.) Every other selection is a quiet amber tint (8% alpha) with a 1px amber inset edge (55% alpha).

**The Never Red Rule.** Under target, behind, overdue and unstarted are shown dim (`mute` / `dim`), never coral. Coral is reserved for machinery that has failed.

### Over the wallpaper (the phone's home)
The launcher on the phone draws some things straight on Sumeet's wallpaper: app and folder labels, the date header and its next-event line, the page dots, and the folder tile's preview fill. These are the one exception, by Sumeet's decision (2026-11-06), to "dark only", because the wallpaper is his and may be light:
- **Light ink** (the default): Starlight text and Dust asides with a still text shadow (1dp down, 3dp blur, black at 60%). The shadow is for legibility over a photo, never for emphasis.
- **Dark ink**, when Android says the wallpaper supports dark text (`WallpaperColors.HINT_SUPPORTS_DARK_TEXT`): Deep Space for text and asides alike, no shadow. The darkest wallpaper with that hint is about `#858585`, where Deep Space reads at about 5.3:1 and a grey aside wouldn't.
- The hint describes the wallpaper as a whole, so a light patch on a dark wallpaper (or a dark one on a light wallpaper) is not guaranteed to read; the shadow carries light ink there.
- The system bar icons follow the same choice on home's pages: dark over a light wallpaper, light on the agent page, the drawer and an open folder.
- Everything LoR draws as a surface (the drawer, the dock's panel, folders, cards, menus, settings) stays dark on any wallpaper.
- The agent page sits on a veil of Deep Space at 92% over the wallpaper, enough for Far Dust to read at 4.5:1 even over white, so it doesn't follow the wallpaper.

## Typography

**Display Font:** Saira Condensed (500, 600), falling back to Saira, then ui-sans-serif
**Body Font:** Saira (400, 500, 600), falling back to ui-sans-serif, system-ui
**Label/Mono Font:** Saira Condensed caps for labels; no monospace face

**Character:** One family in two widths. The condensed caps are the ship's instrument lettering; the regular width is the logbook. Tabular numerals are on globally so every readout and hour column aligns.

### Hierarchy
- **Display** (Saira Condensed 600, 56px rising to 62px at md, line-height 1, 0.03em, uppercase): the big readout under the arc's zenith (time remaining or recorded). Its unit word ("remaining") sits beside it in 20px condensed amber.
- **Headline** (Saira Condensed 600, 22px, 0.08em, uppercase): the page title in the page head. Rendered pages use condensed 26px for their h1 and 19px for h2.
- **Title** (Saira Condensed 600, 15px, 0.07em, uppercase): panel section heads, main nav items. The brand wordmark is the same face at 19px, 0.1em, in amber.
- **Body** (Saira 400, 15px, line-height 1.5, tabular numerals): all reading text. Rendered pages run at 15.5px, relaxed leading, capped at 72ch.
- **Body small** (Saira 400, 13-14px): panel asides, metadata under task titles, legends, notes.
- **Label** (Saira Condensed 500, 12.5-13px, 0.07em, uppercase, `dim` or `mute`): field labels in definition lists, table headers, subgroup heads, memory sub-nav.
- **Button** (Saira 500, 13.5px, sentence case; 12.5px in the small size): every button. The primary button is 600.

### Named Rules
**The Condensed Caps Are Instruments Rule.** Condensed caps are for page titles, nav, section heads, field labels and big readouts. Nothing else. Buttons, sentences, task and goal names in lists stay sentence case Saira. The one exception is goal names on the star map, which are labelled like stars.

**The Tabular Rule.** Numerals are always tabular; time and hours never jitter.

## Layout

Desktop first. A two-column shell at md and up: a sticky full-height left rail (208px) with the amber wordmark, a live date-time stamp, the nav (34px rows: Today, Tasks, Goals, People, Notes), and system health pinned to its foot; then the main column with 30px side padding. Below md the rail becomes a horizontally scrolling top bar of condensed nav links, the active one in amber text instead of a fill, and main padding drops to 16px.

Every page opens with the page head: min 62px tall, a `line` rule beneath, title then a muted sub, and the global actions (search input, 300px, from lg; the Start timer primary or the running-timer pill) pushed right. Page sections stack at 20-22px. Panel grids use a 22px gutter: Today's three panels at 1.22fr / 1fr / 1.08fr from lg; Tasks, Goals and the Notes graph pair a fluid main column with a 360-440px sticky side panel from xl; Notes pairs a 300px index with the reading column from lg.

The day arc is a 1200x300 drawing capped at 1240px wide; on phones it is replaced by a straight 12px bar with Rising and Setting under it. Planned/Actual lanes use a 66px label column and 26px tracks with a faint 24-hour rhythm. The star map fills its framed panel at 62vh, or the viewport height minus the page chrome on xl.

Spacing rhythm: 6 / 8 / 12 / 20 / 22 / 30px. Panels pad 16px top and bottom, 20px sides. List rows are separated by `line` rules with 8-10px vertical padding.

## Elevation & Depth

Depth is tonal and by framing first: `well` below the page, `bg` as the ground, `panel` as a face held inside a 1px `frame` border, `raised` for hover. On top of that, as Elite's panels do (Sumeet, 2026-11-07, replacing the earlier flat rule, which went further than the look chosen in `docs/design/README.md`):
- **Blur as a backdrop.** Something that opens over other content (a sheet, a menu, a dialog, the drawer, an open folder, a translucent strip like the star map's legend) may blur what is behind it, so it reads as glass in front rather than a hole cut through. The panel's face stays dark enough that its text keeps 4.5:1 over the lightest unblurred content that can sit behind it; where blur isn't available (an old browser, a phone without cross-window blur, battery saver) the same face over a dim must look right on its own.
- **Shadows under what floats.** A soft, dark shadow under something lifted above the page (a menu, a dialog, a sheet, an icon being dragged) shows what sits on what. Shadows are black, low and wide, never coloured; things that lie in the page (panels, rows, cards) cast none. Light text's shadow over the phone's wallpaper is for legibility, not depth (see Over the wallpaper).
- **Glow is the highlight.** Glow is a functional element, not a finish: it marks where you are and what is live, so the eye finds it at a glance. It goes on the selected or focused element, the running timer and now on the arc, in the colour that already means it (amber for selected and now, cyan for the agent and planned). Because its job is to single something out, a screen has only as many glows as it has things that matter right now, usually one or two. Never on body text, never as decoration or to make a screen look rich, never animated.
- **Values.** Float shadow `0 8px 24px rgb(0 0 0 / 0.5)` (on the phone, 8dp elevation's ambient shadow in black). Backdrop blur 24px (24dp), behind a `panel` face at 72%, raised to whatever the 4.5:1 above needs. Glow `0 0 12px` in `or` or `cy` at 40%.

### Named Rules
**The Functional-Glow Rule.** Every glow answers "which one?": the selection, the focus, what is running, now. Colour and fill say what a thing is; glow says this is the one that matters. A glow that answers nothing is decoration and comes out. Nothing pulses, shimmers or flashes to get attention (see Motion).

**The Engraved-Grid Rule.** The faint 60px engraved grid (white at 2.5-3% alpha) sits behind the day arc and the star map only. Not behind panels, pages or lists.

## Shapes

Square everywhere. Buttons, inputs, chips, lanes, blocks and panels have no radius. The one silhouette is the chamfer: outer framed panels clip their top-left and bottom-right corners at 6px, drawn as a 1px `frame` outer shell with the `panel` face clipped to the same polygon inside it. Inner elements (buttons, inputs, inline editors, notices) are never chamfered.

Round shapes are reserved for small status dots (6px: the timer dot, the health dot) and for project and task bodies on the star map. The four-point star (a 20-unit path with a narrow waist) marks goals, flagged tasks and now; habits are small hollow squares. The reticle is four L-shaped corner brackets in 1.5px amber around the selected body or around now on the arc. Calendar events are 10px squares rotated 45 degrees.

Lines carry state: solid is confirmed, dashed is not yet confirmed or not yet travelled, a fainter dashed bronze is unlabelled phone or place time.

## Motion

Calm. Motion is one-shot and follows the finger: a thing moves because a hand moved it or a tap asked for it, and it stops. Nothing loops, pulses, shimmers or spins, and nothing moves on its own (a reread, an undo or a cold start puts things in place at once).

- **Feedback and fades:** 150ms (the colour transition above).
- **A thing taking its new place** (a tile, a folder from its tile, a settling icon): about 220ms, standard ease (fast out, slow in).
- **Whatever a finger lets go of** (the phone's drawer): it follows the finger 1:1, then settles with a spring that has no bounce, starting at the finger's speed.
- **Remove animations** (Android's setting, the animator duration scale at 0): every motion ends at once, and nothing is left part-way.

## Components

### Buttons
Quiet, rectangular, sentence case. They outline until touched.
- **Shape:** square (0px), 1px `edge` border.
- **Default:** transparent, `txt`, Saira 500 13.5px, 4px 12px padding, 6px icon gap.
- **Hover / Focus:** border to amber at 70%, a 10% amber tint, 150ms colour transition. Keyboard focus is a 1px amber outline at 2px offset.
- **Primary:** solid amber fill and border, near-black brown text (#1a0d02), weight 600; hover lightens to #ff9c45. One per screen (Start timer, or the screen's main commit).
- **Amber outline variant:** an ordinary button with amber border and amber text, used for a secondary commit like "Accept all".
- **Small:** 24px tall, 0 8px, 12.5px; for row actions (Done, Seen, Yes, No, map zoom).
- **Danger:** coral text with a 60% coral border; hover full coral border and 10% coral tint.
- **Disabled:** 45% opacity, no hover.

### Chips
- **Style:** 20px tall, 0 6px padding, 1px `edge` border, `mute` 12px text, square.
- **State:** calendar chips take a cyan border at 50% and cyan text. Free-time gap chips add a `dim` "fits ..." suffix.

### Cards / Containers
- **Corner Style:** 6px chamfers on two opposite corners (top-left, bottom-right); otherwise square.
- **Background:** `panel` face inside a 1px `frame` shell.
- **Shadow Strategy:** none; panels lie in the page. Floating surfaces take the shadow in Elevation & Depth.
- **Border:** the 1px bronze frame is the border.
- **Internal Padding:** 16px vertical, 20px horizontal. A header row holds the condensed section title left and a 13px `mute` aside right (a count, a link, a unit like "hours / target").

### Inputs / Fields
- **Style:** `well` background, 1px `edge` border, square, 6px 10px padding, 14px `txt`, `dim` placeholder. Selects reserve 28px on the right.
- **Focus:** border to amber at 70%, no outline ring.
- **Native controls:** checkboxes take the amber accent; date and time picker glyphs are filtered to the bronze-orange of the frame.

### Navigation
- **Rail:** condensed 15px 600 caps in `mute`, 34px rows with 22px side padding, hover to `txt`. Active is a full-width solid amber bar with near-black text. The Tasks item carries its open count right-aligned in sentence-case Saira at 80% opacity.
- **Health foot:** a 6px dot (amber when all senses report, coral otherwise) and a 13px `mute` sentence.
- **Mobile:** a top bar of condensed links, active in amber text, rule in `frame` below.
- **Segmented controls** (filters, Pages/Graph): adjacent text buttons divided by 1px `edge` rules; the selected one takes the quiet amber selection treatment, not a fill.

### Selection row
The system's selection everywhere a list or block is chosen: 8% amber background with a 1px inset amber edge at 55%. Also used as the Notice strip (a status line with a "dismiss" link) and as the selected filter segment. Hovered unselected rows go to `raised`.

### Agent tag
Lowercase "agent" in cyan at 85%, 12px, with an underlined "undo" in `txt` when the row can be reverted. Never a pill or badge box.

### Meter
A 3px hairline track in `line` with an amber fill at 85% to the hours-against-target ratio, capped at full. Under target simply shows less amber; the label beside it goes `mute` when nothing is logged.

### Day Arc (signature)
The waking day as a half-ellipse route from Rising (left) to Setting (right) over a `line` baseline and the engraved grid. The untravelled route is a 1.5px dashed `frame` line; tracked blocks overlay it in amber: the running block 4.5px solid, confirmed 3.5px, unconfirmed 2.5px at 85%, bare phone/place time at 70%. Hour ticks cross the route in dark bronze (#6b4420), every second hour labelled in 13px condensed `dim`. Calendar events are cyan-outlined diamonds just outside the route with 14px cyan labels that skip any that would collide. Now is an amber reticle with a small four-point star and a 14px amber "HH:MM now" label. The display readout, its subline, and the running-timer card sit centred under the zenith. Beneath it, Planned and Actual lanes repeat the day as 26px tracks with a 1px amber now line.

### Star Map (signature)
The intent graph as a pannable, zoomable sky on `well` with the engraved grid, inside a chamfered panel. Goals are amber four-point stars sized 8-16px by this week's hours against target (unfed goals in a muddy bronze, #8a6a4a), named below in 15px condensed caps with an "h / target" line in `mute`. Projects are 7px `mute` dots, habits 7px hollow squares, tasks 4px `txt` dots, flagged tasks small amber stars; all linked to their parent by 1px `frame` lines. Selecting a body draws a reticle, lights its constellation's links in amber, dims every other goal, and frames the constellation; labels hold their screen size at any zoom. A legend sits bottom-left on a translucent well strip, square zoom buttons bottom-right. Beside it on xl, a System Info panel shows the selection's fields as a condensed-label definition list.

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
- **Do** frame outer panels with the 1px `frame` shell and the 6px two-corner chamfer, and keep everything inside them square.
- **Do** reserve solid amber for the active nav item and the one primary action on a screen; mark every other selection with the 8% amber tint and 1px 55% amber inset edge.
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
- **Don't** round corners on buttons, inputs, chips or panels; the only round shapes are status dots and map bodies.
- **Don't** spread the engraved grid beyond the day arc and the star map.
- **Don't** stretch a single phone-width column across the desktop or fall back to grey rounded cards.
