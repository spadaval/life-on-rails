// What Queue's scripts check by: an anchor on the screen now, or any whose name starts with a prefix.
export { shown, anyShown } from "../home/shown.mjs";

/** The first anchor on screen whose name starts with [prefix], or undefined. */
export const first = (a, prefix, s) => Object.keys(a).filter((n) => n.startsWith(prefix)).find((n) => {
  const r = a[n];
  return r[2] - r[0] > 1 && r[0] < s.width && r[2] > 0 && r[1] < s.height && r[3] > 0;
});
