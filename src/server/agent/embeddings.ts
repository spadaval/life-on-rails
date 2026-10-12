// Local embeddings (PR 8). Default Xenova/all-MiniLM-L6-v2 (~80 MB) via
// @huggingface/transformers (deferred dep — installed in PR 8, not here).
// v1 attribution NEVER uses embeddings for merge (identical app+title only).
// Overnight backfill embeds NEW (app,title) pairs only.
export const EMBEDDING_MODEL = "Xenova/all-MiniLM-L6-v2";
export const EMBEDDING_DIMS = 384;

export async function embedTitles(_titles: { app: string | null; title: string }[]): Promise<number[][]> {
  throw new Error(
    `embeddings land in PR 8 (npm i @huggingface/transformers, model ${EMBEDDING_MODEL}). ` +
      "v1 rollup uses identical app+title; do not block the wedge on this.",
  );
}
