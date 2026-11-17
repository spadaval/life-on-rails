// Loaded with `node --import` by homecore's wasm tests (wasmJsNodeTest). Compose's Skia ships only a
// browser build of its loader (skiko.mjs): its Node branch is compiled out, so under Node it can
// neither fetch nor read skiko.wasm ("both async and sync fetching of the wasm failed"). This hook
// rewrites the loader's one call, `loadSkikoWASM()`, to pass the file's bytes in as `wasmBinary`,
// which the loader takes before trying to fetch. Nothing else in the loader changes.
import { registerHooks } from 'node:module';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

registerHooks({
  load(url, context, nextLoad) {
    const result = nextLoad(url, context);
    if (!url.startsWith('file:') || !url.endsWith('/skiko.mjs')) return result;
    const source = String(result.source);
    const call = 'loadSkikoWASM().then(';
    if (!source.includes(call)) throw new Error(`skiko-under-node: ${url} no longer calls ${call}`);
    const wasm = JSON.stringify(fileURLToPath(new URL('skiko.wasm', url)));
    const patched = source.replace(call,
      `loadSkikoWASM({ wasmBinary: (await import('node:fs')).readFileSync(${wasm}) }).then(`);
    return { ...result, source: patched };
  },
});
