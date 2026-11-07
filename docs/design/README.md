# Visual design

Chosen 2026-10-14 after four sketch rounds. Each folder holds the Today and Graph sketches (HTML and screenshot) at 1440×900; they show the look, not the final layout.

## Elite: Mostly Harmless (built)

`mostly-harmless/`. A star atlas worn in Elite: Dangerous's menu language, turned down. Dark only.

- **Ground and panels.** Space black with a faint cool undertone (`#0c0d12`), panels `#111218`, dim orange-brown frames (`#4a2e14`) with small 6px chamfers on the outer panels only.
- **Colour has jobs.** Amber-orange (`#f08a2c`) marks now, the running timer, tracked time and selection. Solid orange fills are rare: the active nav item and the one primary action on a screen. Elsewhere selection is a low-alpha orange tint with a 1px orange edge. Cyan (`#6cc6ec`) is the calendar (planned time) and the agent badge. Under target is dim, never red.
- **Type.** Saira Condensed caps for the page title, nav, section headers and big readouts; Saira in sentence case for everything else, buttons included. Tabular numerals.
- **Signature move.** Today's waking day is an arc from rising (wake start) to setting (wake end), drawn as a route line with hour ticks: tracked time in bright orange, the rest dashed, a target reticle at now, calendar events as cyan diamonds, and the remaining time as the big readout under the zenith. The Graph is a star map: goals are four-point stars sized by hours against target, linked to their projects and habits by fine lines, with the selected goal in a reticle and a System Info panel beside it.
- **Restraint.** A faint engraved grid behind the arc and the map only. Glow only as the highlight (the selected star), never to make the screen sparkle; no fake HUD numbers, no scanlines.
- **Depth (2026-11-07).** `DESIGN.md` had grown this into a flat rule: no blur, no shadows, no glow at all. Sumeet found that went further than the look chosen here, since Elite's own panels are blurred glass with bloom, and replaced it: blur as a backdrop behind what opens over other content, soft black shadows under what floats, and glow kept to its job as a highlight, marking the selected, focused or current thing (Sumeet: a valuable UI element, used carefully and functionally, never for sparkle; and one tool among many, not something every focused element must have). Nothing pulses, and emphasis still comes from colour and fill first.

## Astral (reference, not built)

`astral/`. The close second, kept for later: the same arc and star map, calm and deliberate. Blue-black night sky (`#0f1324`, panels `#161c30`, hairlines `#2a3352`), star-ivory text (`#ece6d6`), brass-gold accent (`#d4ad62`), star-blue for planned time (`#86aee6`), coral only for agent flags (`#e07a5f`). Alegreya Sans with Alegreya Sans SC small caps, a faint engraved coordinate grid, a double-ruled frame on the arc, and goals as four-point stars each tinted its own pale colour.
