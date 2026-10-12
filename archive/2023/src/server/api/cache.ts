import NodeCache from "node-cache";

export const cache = new NodeCache({ stdTTL: 60 * 60 });

export async function getOrCompute<T>(key: string, fn: () => Promise<T>) {
  if (!cache.has(key)) {
    console.log(`pref cache miss for key '${key}'`)
    cache.set<T>(key, await fn());
  }
  return cache.get<T>(key)!;
}
