#!/usr/bin/env node
// Writes index.html, the page Sumeet picks a direction from (lor-glev), from content.mjs and
// the critics' notes in ../critiques/:
//
//   node docs/design/lab/compare/build.mjs [--check]
//
// Every quote in content.mjs must appear word for word in that critic's notes, or the build
// fails: the page picks which of their words to show, never what they say. --check builds in
// memory and fails if index.html differs. Run it where markdown-it resolves (the main
// checkout's node_modules, or LOR_MODULES=<a package.json beside them>).
import { readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { CONCEPTS, CRITICS, BASELINE, COUNT, PAGE } from "./content.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(process.env.LOR_MODULES ?? path.join(here, "../../../../package.json"));
const MarkdownIt = require("markdown-it");
const md = new MarkdownIt({ html: false, linkify: false, typographer: false });

const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const flat = (s) => s.replace(/[*_`]/g, "").replace(/\s+/g, " ").trim();
const problems = [];

// One critic's notes on one file: the text between its marker and the next.
// A critic whose seat is open (CRITICS[key].open says why) has none, and the page says so.
function notes(file, critic) {
  const text = readFileSync(path.join(here, "../critiques", file), "utf8");
  const parts = text.split(/^<!-- critic: ([a-z0-9.-]+) -->$/m);
  for (let i = 1; i < parts.length; i += 2) if (parts[i] === critic) return parts[i + 1].trim();
  if (!CRITICS[critic]?.open) problems.push(`${file}: no notes from ${critic}`);
  return "";
}
const seated = Object.entries(CRITICS).filter(([, c]) => !c.open);
const open = Object.entries(CRITICS).filter(([, c]) => c.open);
const openSeats = () =>
  open.map(([, c]) => `<p class="seat-open"><strong>${esc(c.name)}'s seat is open.</strong> ${md.renderInline(c.open)}</p>`).join("\n");

// A quote, checked against the notes it came from, with its critic named.
function quote(file, q) {
  const critic = CRITICS[q.by] ?? problems.push(`unknown critic ${q.by}`);
  const source = notes(file, q.by);
  if (!flat(source).includes(flat(q.text))) problems.push(`${file}: not word for word from ${q.by}: "${q.text.slice(0, 60)}…"`);
  return `<li><q>${md.renderInline(q.text)}</q><cite>${esc(critic.name)}${q.on ? `, on the ${esc(q.on)}` : ""}</cite></li>`;
}

const shots = (id, name) => `
  <div class="shots" role="list" aria-label="${esc(name)}: four screens">
    ${[
      ["desk", "Dashboard Today, 1500 wide", "The afternoon: a timer running, two flags. First screen only."],
      ["narrow", "Dashboard Today, 412 wide", "The same moment at phone width."],
      ["home", "Phone home", "The cover screen, the demo day at 15:00."],
      ["agent", "Phone agent page", "One approval, one question, a failed turn, two flags."],
    ]
      .map(
        ([k, title, alt]) => `<figure role="listitem" class="shot shot-${k}">
      <a href="shots/${id}-${k}.webp"><img src="shots/${id}-${k}.webp" alt="${esc(`${name}: ${title}. ${alt}`)}" loading="lazy" decoding="async"></a>
      <figcaption>${esc(title)}</figcaption>
    </figure>`,
      )
      .join("\n    ")}
  </div>`;

const viewers = (id) => `
  <nav class="viewers" aria-label="Open in the viewers">
    <a class="btn" href="../dashboard/#/${id}/">Dashboard</a>
    <a class="btn" href="../phone/#${id}/demo">Phone</a>
    <a class="btn" href="../phone/#${id}/demo/inner">Phone, unfolded</a>
  </nav>`;

const fullNotes = (file) => `
  <div class="notes">
    ${seated
      .map(
        ([key, c]) => `<details>
      <summary>${esc(c.name)}'s notes in full</summary>
      <div class="prose">${md.render(notes(file, key))}</div>
    </details>`,
      )
      .join("\n    ")}
  </div>`;

const concept = (c) => `
<section class="concept" id="${c.id}" aria-labelledby="${c.id}-h">
  <header class="concept-head">
    <h2 id="${c.id}-h">${esc(c.name)}</h2>
    <p class="sentence">${md.renderInline(c.sentence)}</p>
    ${viewers(c.id)}
  </header>
  ${shots(c.id, c.name)}
  <dl class="facts">
    ${c.facts.map(([k, v]) => `<div><dt>${esc(k)}</dt><dd>${md.renderInline(v)}</dd></div>`).join("\n    ")}
  </dl>
  <div class="sides">
    <div class="side side-for">
      <h3>For</h3>
      <ul class="quotes">${c.for.map((q) => quote(`${c.id}.md`, q)).join("")}</ul>
    </div>
    <div class="side side-against">
      <h3>Against</h3>
      <ul class="quotes">${c.against.map((q) => quote(`${c.id}.md`, q)).join("")}</ul>
    </div>
  </div>
  <div class="steal">
    <h3>Worth stealing from it</h3>
    <ul class="quotes">${c.steal.map((q) => quote(`${c.id}.md`, q)).join("")}</ul>
  </div>
  ${fullNotes(`${c.id}.md`)}
  ${openSeats()}
</section>`;

// No line ends in whitespace: the indented helpers above start with a newline, and the
// repository's `git diff --check` refuses trailing spaces.
const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="color-scheme" content="dark">
<title>Pick a direction · LoR lab</title>
<style>
@font-face { font-family: "Hanken Grotesk"; src: url(fonts/hanken-grotesk-latin-400-normal.woff2) format("woff2"); font-weight: 400; font-display: swap; }
@font-face { font-family: "Hanken Grotesk"; src: url(fonts/hanken-grotesk-latin-500-normal.woff2) format("woff2"); font-weight: 500; font-display: swap; }
@font-face { font-family: "Hanken Grotesk"; src: url(fonts/hanken-grotesk-latin-600-normal.woff2) format("woff2"); font-weight: 600; font-display: swap; }
:root {
  /* Ground, three surfaces, three text tones, one accent. */
  --ground: #0d0e12; --s1: #15171d; --s2: #1c1f27; --s3: #262a34; --line: #2c303b;
  --text: #eceae4; --muted: #b0aea6; --faint: #84827b;
  --amber: #f0a43a; --amber-soft: #f0a43a22;
  --for: #8fcfa0; --against: #ef9a8a;
  --r-s: 8px; --r-m: 14px; --r-l: 20px;
  --t-xs: 13px; --t-s: 15px; --t-m: 17px; --t-l: 22px; --t-xl: 32px;
  --gutter: 16px;
}
* { box-sizing: border-box; }
html { -webkit-text-size-adjust: 100%; scroll-padding-top: 64px; }
body { margin: 0; background: var(--ground); color: var(--text); font: 400 var(--t-m)/1.55 "Hanken Grotesk", system-ui, sans-serif; }
::selection { background: var(--amber); color: #1a1205; }
a { color: var(--amber); text-underline-offset: 3px; text-decoration-thickness: 1px; }
a:focus-visible, summary:focus-visible { outline: 2px solid var(--amber); outline-offset: 3px; border-radius: 4px; }
h1, h2, h3 { font-weight: 600; line-height: 1.2; margin: 0; text-wrap: balance; }
p { margin: 0; }
.wrap { max-width: 1120px; margin: 0 auto; padding: 0 var(--gutter); }

/* The top: what the page is, and the way to each concept. */
.intro { padding-top: 40px; padding-bottom: 24px; }
.intro h1 { font-size: var(--t-xl); letter-spacing: -0.01em; }
.intro p { margin-top: 12px; max-width: 64ch; color: var(--muted); }
.intro p strong { color: var(--text); font-weight: 600; }
.jump { position: sticky; top: 0; z-index: 2; background: color-mix(in srgb, var(--ground) 82%, transparent); backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px); border-bottom: 1px solid var(--line); }
.jump ul { display: flex; gap: 8px; list-style: none; margin: 0 auto; padding: 10px var(--gutter); max-width: 1120px; overflow-x: auto; scrollbar-width: none; }
.jump a { display: block; padding: 6px 14px; border-radius: 999px; background: var(--s2); color: var(--text); text-decoration: none; font-size: var(--t-s); font-weight: 500; white-space: nowrap; }
.jump a:hover { background: var(--s3); }

/* A concept. */
.concept { padding: 48px 0 8px; border-top: 1px solid var(--line); margin-top: 40px; }
.concept:first-child { border-top: 0; margin-top: 0; padding-top: 32px; }
.concept-head h2 { font-size: var(--t-xl); letter-spacing: -0.01em; }
.sentence { margin-top: 8px; font-size: var(--t-l); line-height: 1.4; color: var(--muted); max-width: 40ch; }
.viewers { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 20px; }
.btn { display: inline-flex; align-items: center; min-height: 44px; padding: 0 16px; border-radius: var(--r-s); background: var(--s2); color: var(--text); text-decoration: none; font-size: var(--t-s); font-weight: 500; border: 1px solid var(--line); }
.btn::after { content: "→"; margin-left: 8px; color: var(--faint); }
.btn:hover { background: var(--s3); }

/* Four screens: one wide, three tall. A strip that scrolls sideways on a phone. */
.shots { display: grid; grid-template-columns: 1.15fr repeat(3, minmax(0, 0.42fr)); gap: 16px; margin-top: 28px; align-items: start; }
.shot { margin: 0; }
.shot a { display: block; border-radius: var(--r-m); overflow: hidden; background: var(--s1); box-shadow: 0 1px 2px #0008, 0 8px 24px #0006; }
.shot img { display: block; width: 100%; height: auto; }
.shot figcaption { margin-top: 8px; font-size: var(--t-xs); color: var(--faint); }

/* The facts a pick turns on. */
.facts { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1px; margin: 28px 0 0; background: var(--line); border-radius: var(--r-m); overflow: hidden; }
.facts div { background: var(--s1); padding: 14px 16px; }
.facts dt { font-size: var(--t-xs); color: var(--faint); font-weight: 500; }
.facts dd { margin: 4px 0 0; font-size: var(--t-s); }
.facts dd code { font-size: 0.92em; }

/* For and against, side by side. */
.sides { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 28px; }
.side, .steal { background: var(--s1); border-radius: var(--r-l); padding: 20px; }
.side h3, .steal h3 { font-size: var(--t-s); font-weight: 600; display: flex; align-items: center; gap: 8px; }
.side h3::before { content: ""; width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
.side-for h3 { color: var(--for); }
.side-against h3 { color: var(--against); }
.steal { margin-top: 16px; }
.steal h3 { color: var(--amber); }
.quotes { list-style: none; margin: 12px 0 0; padding: 0; display: grid; gap: 16px; }
.quotes q { display: block; quotes: none; font-size: var(--t-s); line-height: 1.55; }
.quotes cite { display: block; margin-top: 4px; font-style: normal; font-size: var(--t-xs); color: var(--faint); }

/* The critics' full notes, folded. */
.seat-open { margin-top: 16px; padding: 14px 16px; border-radius: var(--r-m); border: 1px dashed var(--line); color: var(--muted); font-size: var(--t-s); }
.seat-open strong { color: var(--text); font-weight: 600; }
.notes { margin-top: 16px; display: grid; gap: 8px; }
details { background: var(--s1); border-radius: var(--r-m); }
summary { cursor: pointer; padding: 14px 16px; font-size: var(--t-s); font-weight: 500; color: var(--muted); list-style: none; display: flex; justify-content: space-between; min-height: 44px; align-items: center; }
summary::-webkit-details-marker { display: none; }
summary::after { content: "+"; color: var(--faint); font-size: var(--t-l); line-height: 1; }
details[open] summary::after { content: "−"; }
details[open] summary { color: var(--text); }
.prose { padding: 0 16px 20px; font-size: var(--t-s); max-width: 72ch; }
.prose :is(h1, h2, h3, h4) { font-size: var(--t-m); margin: 20px 0 6px; }
.prose p, .prose ul, .prose ol { margin: 8px 0; }
.prose ul, .prose ol { padding-left: 20px; }
.prose li + li { margin-top: 4px; }
.prose code { font-size: 0.9em; background: var(--s2); padding: 1px 5px; border-radius: 4px; }
.prose hr { border: 0; border-top: 1px solid var(--line); margin: 16px 0; }

/* The count question, the defects, the baseline. */
.plain { padding: 48px 0 8px; border-top: 1px solid var(--line); margin-top: 40px; }
.plain h2 { font-size: var(--t-l); }
.plain > p { margin-top: 10px; max-width: 64ch; color: var(--muted); }
.plain .quotes { margin-top: 20px; max-width: 72ch; }
footer { padding: 48px 0 64px; color: var(--faint); font-size: var(--t-xs); }

@media (max-width: 860px) {
  .shots { display: flex; overflow-x: auto; scroll-snap-type: x mandatory; scroll-padding-inline: var(--gutter); gap: 12px; margin-left: calc(-1 * var(--gutter)); margin-right: calc(-1 * var(--gutter)); padding: 0 var(--gutter) 8px; scrollbar-width: none; }
  .shot { flex: 0 0 auto; scroll-snap-align: start; }
  .shot-desk { width: 86vw; }
  .shot-narrow, .shot-home, .shot-agent { width: 52vw; }
  .sides { grid-template-columns: 1fr; }
  .intro { padding-top: 28px; }
  .concept { padding-top: 36px; }
  :root { --t-xl: 28px; --t-l: 20px; }
}
@media (prefers-reduced-motion: no-preference) { html { scroll-behavior: smooth; } }
</style>
</head>
<body>
<div class="wrap intro">
  <h1>${esc(PAGE.title)}</h1>
  ${PAGE.intro.map((p) => `<p>${md.renderInline(p)}</p>`).join("\n  ")}
</div>
<nav class="jump" aria-label="Concepts">
  <ul>
    ${CONCEPTS.map((c) => `<li><a href="#${c.id}">${esc(c.name)}</a></li>`).join("")}<li><a href="#count">11 or 4?</a></li><li><a href="#current">Current</a></li><li><a href="#defects">Defects</a></li>
  </ul>
</nav>
<main class="wrap">
${CONCEPTS.map(concept).join("\n")}
<section class="plain" id="count" aria-labelledby="count-h">
  <h2 id="count-h">${esc(COUNT.title)}</h2>
  <p>${md.renderInline(COUNT.question)}</p>
  <ul class="quotes">${COUNT.answers.map((q) => quote("home-count-and-defects.md", q)).join("")}</ul>
  ${openSeats()}
</section>
<section class="plain" id="current" aria-labelledby="current-h">
  <h2 id="current-h">${esc(BASELINE.title)}</h2>
  <p>${md.renderInline(BASELINE.text)}</p>
  ${viewers("current")}
  ${shots("current", "Current")}
</section>
<section class="plain" id="defects" aria-labelledby="defects-h">
  <h2 id="defects-h">Defects the critics hit</h2>
  ${PAGE.defects.map((p) => `<p>${md.renderInline(p)}</p>`).join("\n  ")}
  ${fullNotes("home-count-and-defects.md")}
</section>
</main>
<footer class="wrap">${md.renderInline(PAGE.footer)}</footer>
</body>
</html>
`;

const page = html.replace(/[ \t]+$/gm, "");
if (problems.length) {
  console.error(problems.map((p) => `build: ${p}`).join("\n"));
  process.exit(1);
}
const out = path.join(here, "index.html");
if (process.argv.includes("--check")) {
  if (readFileSync(out, "utf8") !== page) {
    console.error("build: index.html is not what content.mjs and the notes make; run build.mjs");
    process.exit(1);
  }
  console.log("build: index.html is current");
} else {
  writeFileSync(out, page);
  console.log(`build: wrote ${out} (${page.length} bytes)`);
}
