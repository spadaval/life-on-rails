import { useMemo } from "react";
import MarkdownIt from "markdown-it";

// Raw HTML stays off: pages can carry text that came from email or the web.
const md = new MarkdownIt({ html: false, linkify: true, breaks: false });

const enc = encodeURIComponent;

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
      .replace(/^mail:([A-Za-z0-9]+)/, (_m, id: string) => `[mail:${id}](https://mail.google.com/mail/u/0/#all/${id})`)
      .replace(/^obs:([0-9a-f-]{36})/, (_m, id: string) => `[obs:${id.slice(0, 8)}](/memory?tab=timeline&obs=${id})`)
      .replace(/^thread:(\S+)/, (_m, id: string) => `[thread:${id}](t3-thread://v1/${id})`)
      .replace(/^url:(\S+)/, (_m, url: string) => `[${url}](${url})`);
    return `- **\\[^${key}]** ${target}`;
  });
  return lines
    .join("\n")
    .replace(/\[\[([a-z0-9][a-z0-9/-]*)(?:\|([^\]]+))?\]\]/g, (_m, slug: string, label?: string) => `[${label ?? slug}](/memory?slug=${enc(slug)})`);
}

export function Markdown({ source }: { source: string }) {
  const html = useMemo(() => md.render(linkify(source)), [source]);
  return <div className="prose-lor" dangerouslySetInnerHTML={{ __html: html }} />;
}
