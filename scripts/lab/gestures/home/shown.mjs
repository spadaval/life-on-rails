// What the home scripts check by. Home keeps the cells next to the one on screen composed, and their
// anchors keep off-screen bounds, so a tile counts as shown only when it is on the screen.
export const shown = (a, name, s) => {
  const r = a[name];
  return !!r && r[2] - r[0] > 1 && r[3] - r[1] > 1 && r[0] < s.width && r[2] > 0 && r[1] < s.height && r[3] > 0;
};

/** Whether any anchor on the screen has a name starting with [prefix]. */
export const anyShown = (a, prefix, s) => Object.keys(a).some((n) => n.startsWith(prefix) && shown(a, n, s));
