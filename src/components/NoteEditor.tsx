// The Notes editor: CodeMirror 6 over the page's markdown, which stays exactly
// as written (the agent edits the same text with find/replace, and History
// diffs it). Live preview after Obsidian: away from the cursor, markdown
// symbols are hidden and [[links]] read as their page's title; on the cursor's
// line everything is shown as typed.
import { useEffect, useRef } from "react";
import { autocompletion, closeBrackets, closeBracketsKeymap, type Completion, type CompletionContext, completionKeymap } from "@codemirror/autocomplete";
import { defaultKeymap, history, historyKeymap, indentWithTab } from "@codemirror/commands";
import { markdown, markdownLanguage } from "@codemirror/lang-markdown";
import { HighlightStyle, syntaxHighlighting, syntaxTree } from "@codemirror/language";
import { searchKeymap } from "@codemirror/search";
import { Annotation, EditorState, type Range, StateEffect } from "@codemirror/state";
import { Decoration, type DecorationSet, EditorView, ViewPlugin, type ViewUpdate, WidgetType, keymap } from "@codemirror/view";
import { tags } from "@lezer/highlight";
import type { MarkdownConfig } from "@lezer/markdown";
import { linkChoices, slugFromText, titleFromSlug } from "../shared/wiki.js";

export interface LinkTarget {
  slug: string;
  title: string;
}

/** [[slug]] and [[slug|words]] as their own syntax nodes, ahead of ordinary links. */
const WikiLinks: MarkdownConfig = {
  defineNodes: [{ name: "WikiLink" }, { name: "WikiLinkMark" }, { name: "FootnoteRef" }],
  parseInline: [
    {
      name: "WikiLink",
      before: "Link",
      parse(cx, next, pos) {
        if (next !== 91 || cx.char(pos + 1) !== 91) return -1;
        const m = /^\[\[[^\]\n]+\]\]/.exec(cx.slice(pos, Math.min(cx.end, pos + 300)));
        if (!m) return -1;
        const end = pos + m[0].length;
        return cx.addElement(cx.elt("WikiLink", pos, end, [cx.elt("WikiLinkMark", pos, pos + 2), cx.elt("WikiLinkMark", end - 2, end)]));
      },
    },
    {
      // A citation marker, [^key]. Definitions are lines, handled below.
      name: "FootnoteRef",
      before: "Link",
      parse(cx, next, pos) {
        if (next !== 91 || cx.char(pos + 1) !== 94) return -1;
        const m = /^\[\^[^\]\s]+\](?!:)/.exec(cx.slice(pos, Math.min(cx.end, pos + 80)));
        return m ? cx.addElement(cx.elt("FootnoteRef", pos, pos + m[0].length)) : -1;
      },
    },
  ],
};

class LinkWidget extends WidgetType {
  constructor(readonly slug: string, readonly text: string, readonly missing: boolean) {
    super();
  }
  eq(o: LinkWidget) {
    return o.slug === this.slug && o.text === this.text && o.missing === this.missing;
  }
  toDOM() {
    const a = document.createElement("span");
    a.className = `cm-wikilink${this.missing ? " cm-wikilink-missing" : ""}`;
    a.dataset.slug = this.slug;
    a.title = this.missing ? `${this.slug}: not written yet. Click to start it.` : this.slug;
    a.textContent = this.text;
    return a;
  }
  ignoreEvent() {
    return false;
  }
}

class TextWidget extends WidgetType {
  constructor(readonly text: string, readonly cls: string) {
    super();
  }
  eq(o: TextWidget) {
    return o.text === this.text && o.cls === this.cls;
  }
  toDOM() {
    const s = document.createElement("span");
    s.className = this.cls;
    s.textContent = this.text;
    return s;
  }
}

/** Where a citation's source opens. */
function sourceUrl(kind: string, id: string): string | null {
  if (kind === "mail") return `https://mail.google.com/mail/u/0/#all/${encodeURIComponent(id)}`;
  if (kind === "url") return /^https?:\/\//.test(id) ? id : null;
  if (kind === "thread") return `t3-thread://v1/${id}`;
  if (kind === "obs") return `/?obs=${encodeURIComponent(id)}`;
  return null;
}

const hide = Decoration.replace({});
/** A change that came from the server, not the keyboard: not a reason to save. */
const fromOutside = Annotation.define<boolean>();
const refresh = StateEffect.define<null>();

/** The lines a cursor or selection touches: shown as typed. Nothing is, when the editor is not focused. */
function activeLines(view: EditorView): Set<number> {
  const out = new Set<number>();
  if (!view.hasFocus) return out;
  for (const r of view.state.selection.ranges) {
    const a = view.state.doc.lineAt(r.from).number;
    const b = view.state.doc.lineAt(r.to).number;
    for (let n = a; n <= b; n++) out.add(n);
  }
  return out;
}

function livePreview(pages: () => Map<string, string>) {
  return ViewPlugin.fromClass(
    class {
      decorations: DecorationSet;
      constructor(view: EditorView) {
        this.decorations = this.build(view);
      }
      update(u: ViewUpdate) {
        if (u.docChanged || u.viewportChanged || u.selectionSet || u.focusChanged || u.transactions.some((t) => t.effects.some((e) => e.is(refresh)))) {
          this.decorations = this.build(u.view);
        }
      }
      build(view: EditorView): DecorationSet {
        const doc = view.state.doc;
        const active = activeLines(view);
        const isActive = (pos: number) => active.has(doc.lineAt(pos).number);
        const known = pages();
        const out: Range<Decoration>[] = [];
        const line = (pos: number, cls: string) => out.push(Decoration.line({ class: cls }).range(doc.lineAt(pos).from));

        for (const { from, to } of view.visibleRanges) {
          // Citation sources: `[^key]: mail:…`, quiet lines at the foot of the page.
          for (let pos = from; pos <= to; ) {
            const l = doc.lineAt(pos);
            if (/^\[\^[^\]\s]+\]:/.test(l.text)) {
              line(l.from, "cm-fndef");
              const src = /\b(mail|url|thread|obs):(\S+)/.exec(l.text);
              const url = src ? sourceUrl(src[1]!, src[2]!) : null;
              if (src && url) {
                const at = l.from + src.index;
                out.push(Decoration.mark({ class: `cm-link${isActive(l.from) ? "" : " cm-live"}`, attributes: { "data-url": url } }).range(at, at + src[0].length));
              }
            }
            pos = l.to + 1;
          }
          syntaxTree(view.state).iterate({
            from,
            to,
            enter: (node) => {
              const name = node.name;
              const live = !isActive(node.from);
              const heading = /^ATXHeading(\d)$/.exec(name);
              if (heading) {
                line(node.from, `cm-h cm-h${heading[1]}`);
                return;
              }
              switch (name) {
                case "HeaderMark": {
                  // The marks and the space after them.
                  const end = doc.sliceString(node.to, node.to + 1) === " " ? node.to + 1 : node.to;
                  if (live && node.from === doc.lineAt(node.from).from) out.push(hide.range(node.from, end));
                  return;
                }
                case "EmphasisMark":
                case "CodeMark":
                case "StrikethroughMark":
                  if (live && node.from !== node.to) {
                    // A fenced block's ``` lines stay: hiding them would leave the code floating.
                    const parent = node.node.parent?.name;
                    if (parent !== "FencedCode") out.push(hide.range(node.from, node.to));
                  }
                  return;
                case "Emphasis":
                  out.push(Decoration.mark({ class: "cm-em" }).range(node.from, node.to));
                  return;
                case "StrongEmphasis":
                  out.push(Decoration.mark({ class: "cm-strong" }).range(node.from, node.to));
                  return;
                case "Strikethrough":
                  out.push(Decoration.mark({ class: "cm-strike" }).range(node.from, node.to));
                  return;
                case "InlineCode":
                  out.push(Decoration.mark({ class: "cm-icode" }).range(node.from, node.to));
                  return;
                case "FencedCode":
                case "CodeBlock":
                  for (let p = node.from; p <= node.to; ) {
                    const l = doc.lineAt(p);
                    line(l.from, "cm-codeblock");
                    p = l.to + 1;
                  }
                  return false;
                case "Blockquote":
                  for (let p = node.from; p <= node.to; ) {
                    const l = doc.lineAt(p);
                    line(l.from, "cm-quote");
                    p = l.to + 1;
                  }
                  return;
                case "QuoteMark":
                  if (live) out.push(hide.range(node.from, Math.min(node.to + 1, doc.lineAt(node.from).to)));
                  return;
                case "HorizontalRule":
                  line(node.from, "cm-hr");
                  return;
                case "ListMark": {
                  const mark = doc.sliceString(node.from, node.to);
                  if (live && /^[-*+]$/.test(mark)) out.push(Decoration.replace({ widget: new TextWidget("•", "cm-bullet") }).range(node.from, node.to));
                  return;
                }
                case "Link": {
                  const text = doc.sliceString(node.from, node.to);
                  const m = /^\[([^\]]*)\]\(([^)\s]+)[^)]*\)$/.exec(text);
                  if (!m) return;
                  const labelFrom = node.from + 1;
                  const labelTo = labelFrom + m[1]!.length;
                  out.push(Decoration.mark({ class: `cm-link${live ? " cm-live" : ""}`, attributes: { "data-url": m[2]! } }).range(labelFrom, labelTo));
                  if (live) {
                    out.push(hide.range(node.from, labelFrom));
                    out.push(hide.range(labelTo, node.to));
                  }
                  return false;
                }
                case "WikiLink": {
                  const inner = doc.sliceString(node.from + 2, node.to - 2);
                  const bar = inner.indexOf("|");
                  const slug = (bar >= 0 ? inner.slice(0, bar) : inner).trim();
                  const label = bar >= 0 ? inner.slice(bar + 1).trim() : null;
                  const title = known.get(slug);
                  if (live) {
                    out.push(Decoration.replace({ widget: new LinkWidget(slug, label ?? title ?? slug, title === undefined) }).range(node.from, node.to));
                  } else {
                    out.push(Decoration.mark({ class: `cm-wikilink-raw${title === undefined ? " cm-wikilink-missing" : ""}`, attributes: { "data-slug": slug } }).range(node.from, node.to));
                  }
                  return false;
                }
                case "FootnoteRef": {
                  const key = doc.sliceString(node.from + 2, node.to - 1);
                  if (live) out.push(Decoration.replace({ widget: new TextWidget(key, "cm-fnref") }).range(node.from, node.to));
                  else out.push(Decoration.mark({ class: "cm-fnref-raw" }).range(node.from, node.to));
                  return false;
                }
              }
            },
          });
        }
        return Decoration.set(out, true);
      }
    },
    { decorations: (v) => v.decorations },
  );
}

const highlight = HighlightStyle.define([
  { tag: tags.processingInstruction, color: "var(--color-dim)" },
  { tag: tags.meta, color: "var(--color-dim)" },
  { tag: tags.url, color: "var(--color-dim)" },
  { tag: tags.monospace, fontFamily: "ui-monospace, monospace", fontSize: "0.9em" },
  { tag: tags.quote, color: "var(--color-mute)" },
]);

const theme = EditorView.theme(
  {
    "&": { color: "var(--color-txt)", backgroundColor: "transparent", fontSize: "15.5px" },
    "&.cm-focused": { outline: "none" },
    ".cm-scroller": { fontFamily: "var(--font-sans)", lineHeight: "1.65" },
    ".cm-content": { maxWidth: "72ch", padding: "0 0 6em", caretColor: "var(--color-or)" },
    ".cm-line": { padding: "1px 0" },
    ".cm-cursor": { borderLeftColor: "var(--color-or)", borderLeftWidth: "2px" },
    "&.cm-focused .cm-selectionBackground, .cm-selectionBackground, ::selection": { backgroundColor: "rgb(240 138 44 / 0.3) !important" },
    ".cm-h": { fontFamily: "var(--font-cond)", fontWeight: "600", letterSpacing: "0.02em" },
    ".cm-h1": { fontSize: "26px", paddingBottom: "4px" },
    ".cm-h2": { fontSize: "19px", paddingTop: "14px" },
    ".cm-h3": { fontSize: "16.5px", paddingTop: "8px", fontFamily: "var(--font-sans)" },
    ".cm-strong": { fontWeight: "600" },
    ".cm-em": { fontStyle: "italic" },
    ".cm-strike": { textDecoration: "line-through", color: "var(--color-mute)" },
    ".cm-icode": { backgroundColor: "var(--color-raised)", fontFamily: "ui-monospace, monospace", fontSize: "0.9em", padding: "0 2px" },
    ".cm-codeblock": { backgroundColor: "var(--color-well)", fontFamily: "ui-monospace, monospace", fontSize: "13.5px" },
    ".cm-quote": { borderLeft: "1px solid rgb(240 138 44 / 0.6)", paddingLeft: "12px", color: "var(--color-mute)" },
    ".cm-hr": { color: "var(--color-dim)" },
    ".cm-bullet": { color: "var(--color-or)", padding: "0 2px" },
    ".cm-link, .cm-wikilink": { color: "var(--color-or)", textDecoration: "underline", textDecorationColor: "rgb(240 138 44 / 0.4)", textUnderlineOffset: "3px", cursor: "pointer" },
    ".cm-wikilink-raw": { color: "var(--color-or)" },
    ".cm-wikilink-missing": { color: "var(--color-mute)", textDecorationStyle: "dashed", textDecorationColor: "var(--color-dim)" },
    ".cm-fnref": { color: "var(--color-or)", fontSize: "0.68em", verticalAlign: "super", padding: "0 1px" },
    ".cm-fnref-raw": { color: "var(--color-dim)" },
    ".cm-fndef": { color: "var(--color-dim)", fontSize: "13px" },
    ".cm-tooltip.cm-tooltip-autocomplete": { border: "1px solid var(--color-edge)", backgroundColor: "var(--color-raised)" },
    ".cm-tooltip.cm-tooltip-autocomplete > ul": { fontFamily: "var(--font-sans)", maxHeight: "18em" },
    ".cm-tooltip-autocomplete > ul > li": { padding: "3px 10px !important", fontSize: "14px", color: "var(--color-mute)" },
    ".cm-tooltip-autocomplete > ul > li[aria-selected]": { backgroundColor: "rgb(240 138 44 / 0.1)", color: "var(--color-txt)" },
    ".cm-completionDetail": { marginLeft: "12px", fontStyle: "normal", color: "var(--color-dim)", fontSize: "12.5px" },
    ".cm-panels": { backgroundColor: "var(--color-raised)", color: "var(--color-txt)" },
    ".cm-searchMatch": { backgroundColor: "rgb(240 138 44 / 0.25)" },
  },
  { dark: true },
);

export function NoteEditor({ initial, external, pages, onChange, onFollow, onSave }: {
  /** The text the editor opens with. */
  initial: string;
  /** Text to replace everything with (the agent's newer version); applied when `n` changes. */
  external: { text: string; n: number };
  pages: LinkTarget[];
  onChange: (text: string) => void;
  /** A link was followed: an existing page, or one to start. `label` is what the link said. */
  onFollow: (slug: string, label: string | null) => void;
  /** Ctrl/Cmd-S. */
  onSave: () => void;
}) {
  const host = useRef<HTMLDivElement>(null);
  const view = useRef<EditorView | null>(null);
  const pagesRef = useRef(new Map<string, string>());
  const listRef = useRef<LinkTarget[]>([]);
  const cb = useRef({ onChange, onFollow, onSave });
  cb.current = { onChange, onFollow, onSave };

  useEffect(() => {
    const complete = (ctx: CompletionContext) => {
      const m = ctx.matchBefore(/\[\[[^\]\n|]*/);
      if (!m) return null;
      const q = m.text.slice(2);
      const after = ctx.state.sliceDoc(ctx.pos, ctx.pos + 2);
      const insert = (slug: string): Completion["apply"] => (v, _c, from, to) => {
        const text = `${slug}${after === "]]" ? "" : "]]"}`;
        v.dispatch({ changes: { from, to, insert: text }, selection: { anchor: from + slug.length + 2 } });
      };
      const options: Completion[] = linkChoices(q, listRef.current).map((p) => ({ label: p.title, detail: p.slug, apply: insert(p.slug) }));
      const slug = slugFromText(q);
      if (slug && !pagesRef.current.has(slug) && !listRef.current.some((p) => p.title.toLowerCase() === q.trim().toLowerCase())) {
        // The words typed stay as the link's text, and become the new page's title.
        const words = q.trim();
        options.push({ label: `New page “${words}”`, detail: slug, apply: insert(words === slug ? slug : `${slug}|${words}`) });
      }
      return { from: m.from + 2, options, filter: false };
    };

    const v = new EditorView({
      parent: host.current!,
      state: EditorState.create({
        doc: initial,
        extensions: [
          history(),
          closeBrackets(),
          EditorView.lineWrapping,
          markdown({ base: markdownLanguage, extensions: [WikiLinks] }),
          syntaxHighlighting(highlight),
          livePreview(() => pagesRef.current),
          autocompletion({ override: [complete], icons: false }),
          keymap.of([
            { key: "Mod-s", preventDefault: true, run: () => (cb.current.onSave(), true) },
            ...closeBracketsKeymap,
            ...completionKeymap,
            ...defaultKeymap,
            ...historyKeymap,
            ...searchKeymap,
            indentWithTab,
          ]),
          theme,
          EditorView.contentAttributes.of({ "aria-label": "Page body", spellcheck: "true" }),
          EditorView.updateListener.of((u) => {
            if (u.docChanged && !u.transactions.some((t) => t.annotation(fromOutside))) cb.current.onChange(u.state.doc.toString());
          }),
          EditorView.domEventHandlers({
            mousedown(e, view) {
              const el = (e.target as HTMLElement).closest<HTMLElement>("[data-slug], [data-url]");
              if (!el) return false;
              // A rendered link follows on a click; while it is being typed, Ctrl or Cmd follows it.
              const rendered = el.classList.contains("cm-wikilink") || el.classList.contains("cm-live");
              if (!rendered && !e.ctrlKey && !e.metaKey) return false;
              e.preventDefault();
              if (el.dataset.url) {
                const url = el.dataset.url;
                if (/^https?:/.test(url)) window.open(url, "_blank", "noopener");
                else window.location.assign(url);
                return true;
              }
              const slug = el.dataset.slug!;
              const pos = view.posAtDOM(el);
              const raw = view.state.doc.sliceString(pos, Math.min(view.state.doc.length, pos + 300));
              const label = /^\[\[[^\]|]*\|([^\]]+)\]\]/.exec(raw)?.[1] ?? null;
              cb.current.onFollow(slug, label);
              return true;
            },
          }),
        ],
      }),
    });
    view.current = v;
    return () => v.destroy();
  }, []);

  useEffect(() => {
    pagesRef.current = new Map(pages.map((p) => [p.slug, p.title]));
    listRef.current = pages;
    view.current?.dispatch({ effects: refresh.of(null) });
  }, [pages]);

  const seen = useRef(external.n);
  useEffect(() => {
    const v = view.current;
    if (!v || external.n === seen.current) return;
    seen.current = external.n;
    if (v.state.doc.toString() === external.text) return;
    const head = Math.min(v.state.selection.main.head, external.text.length);
    v.dispatch({ changes: { from: 0, to: v.state.doc.length, insert: external.text }, selection: { anchor: head }, annotations: fromOutside.of(true) });
  }, [external]);

  return <div ref={host} className="min-h-[50vh]" />;
}
