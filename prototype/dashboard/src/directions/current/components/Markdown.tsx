import { useMemo } from "react";
import MarkdownIt from "markdown-it";
import footnote from "markdown-it-footnote";
import { href } from "../../../shell/nav.js";
import { replaceWikiLinks } from "../shared/wiki.js";

// Raw HTML stays off: pages can carry text that came from email or the web. No linkify: a bare
// address in the made-up pages must not become a link out of the prototype.
const md = new MarkdownIt({ html: false, linkify: false, breaks: false }).use(footnote as unknown as (md: InstanceType<typeof MarkdownIt>) => void);
// Only links within the prototype (#/...) stay links; any other is drawn as its text.
md.validateLink = (url: string) => url.startsWith("#");

/**
 * Pages are plain markdown with two conventions the server does not parse:
 * [[slug]] links and footnote citations. Turn both into links for reading.
 */
export function linkify(source: string): string {
  const lines = source.split("\n").map((line) => {
    const def = /^\[\^([^\]\s]+)\]:\s*(.*)$/.exec(line);
    if (!def) return line;
    const [, key, rest] = def;
    const target = rest!
      // The prototype links nowhere outside itself: sources are shown, not linked.
      .replace(/^mail:([A-Za-z0-9]+)/, (_m, id: string) => `mail:${id}`)
      .replace(/^obs:([0-9a-f-]{36})/, (_m, id: string) => `obs:${id.slice(0, 8)}`)
      .replace(/^thread:(\S+)/, (_m, id: string) => `thread:${id}`)
      .replace(/^url:(\S+)/, (_m, url: string) => url);
    return `[^${key}]: ${target}`;
  });
  return replaceWikiLinks(lines.join("\n"), (slug, label) => `[${label ?? slug}](${href("/notes", { slug })})`);
}

/** Sources fold away under the page: they are for checking, not reading. */
function foldSources(html: string): string {
  const count = (html.match(/<li id="fn/g) ?? []).length;
  return html
    .replace('<hr class="footnotes-sep">', "")
    .replace('<section class="footnotes">', `<details class="footnotes"><summary>Sources (${count})</summary>`)
    .replace(/<\/section>\s*$/, "</details>");
}

export function Markdown({ source }: { source: string }) {
  const html = useMemo(() => foldSources(md.render(linkify(source))), [source]);
  return <div className="prose-lor" dangerouslySetInnerHTML={{ __html: html }} />;
}
