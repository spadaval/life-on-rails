// Draws the frame in the system Chrome and snaps it to the panel's four grays.
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { FRAME_H, FRAME_SCALE, FRAME_W } from "./frame.js";
import { grayPng2 } from "./png.js";

let fontCss: string | null = null;

/** Saira and Saira Condensed inlined, so the page needs nothing from outside. */
export function frameFonts(): string {
  if (fontCss) return fontCss;
  const require = createRequire(import.meta.url);
  const face = (family: string, pkg: string, weight: number) => {
    const file = require.resolve(`@fontsource/${pkg}/files/${pkg}-latin-${weight}-normal.woff2`);
    return `@font-face{font-family:"${family}";font-weight:${weight};src:url(data:font/woff2;base64,${readFileSync(file).toString("base64")}) format("woff2")}`;
  };
  fontCss = [face("Saira", "saira", 400), face("Saira", "saira", 500), face("Saira", "saira", 600), face("Saira Condensed", "saira-condensed", 600)].join("\n");
  return fontCss;
}

export async function renderFrame(html: string): Promise<Buffer> {
  return (await renderFrames([html]))[0]!;
}

/** Several frames, one browser. */
export async function renderFrames(htmls: string[]): Promise<Buffer[]> {
  const { chromium } = await import("playwright-core");
  const browser = await chromium.launch({ channel: "chrome", headless: true });
  try {
    const out: Buffer[] = [];
    for (const html of htmls) out.push(await renderOne(browser, html));
    return out;
  } finally {
    await browser.close();
  }
}

async function renderOne(browser: import("playwright-core").Browser, html: string): Promise<Buffer> {
  const page = await browser.newPage({ viewport: { width: FRAME_W, height: FRAME_H }, deviceScaleFactor: FRAME_SCALE });
  await page.setContent(html, { waitUntil: "load" });
  await page.evaluate(() => document.fonts.ready);
  const shot = await page.screenshot({ type: "png" });
  // Nearest of black, #555, #aaa, white for every pixel, no dithering: text and lines stay crisp.
  const [width, height, b64] = await page.evaluate(async (src: string) => {
    const img = new Image();
    img.src = `data:image/png;base64,${src}`;
    await img.decode();
    const canvas = document.createElement("canvas");
    canvas.width = img.width;
    canvas.height = img.height;
    const ctx = canvas.getContext("2d")!;
    ctx.drawImage(img, 0, 0);
    const px = ctx.getImageData(0, 0, img.width, img.height).data;
    const levels = new Uint8Array(img.width * img.height);
    for (let i = 0; i < levels.length; i++) {
      const g = 0.299 * px[i * 4]! + 0.587 * px[i * 4 + 1]! + 0.114 * px[i * 4 + 2]!;
      levels[i] = Math.round(g / 85);
    }
    let bin = "";
    for (let i = 0; i < levels.length; i += 0x8000) bin += String.fromCharCode(...levels.subarray(i, i + 0x8000));
    return [img.width, img.height, btoa(bin)] as const;
  }, shot.toString("base64"));
  await page.close();
  return grayPng2(width, height, new Uint8Array(Buffer.from(b64, "base64")));
}
