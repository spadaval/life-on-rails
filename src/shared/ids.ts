// UUIDv7 (time-sortable) generator. No dependency; RFC 9562 §6.2 layout:
// 48-bit unix_ms | 4-bit ver (0111) | 12-bit rand | 2-bit var (10) | 62-bit rand.
export function uuidv7(nowMs = Date.now()): string {
  const rand = (n: number) => {
    const b = new Uint8Array(n);
    crypto.getRandomValues(b);
    return b;
  };
  const r1 = rand(10);
  const hex = (b: Uint8Array) => [...b].map((x) => x.toString(16).padStart(2, "0")).join("");
  const unixHex = nowMs.toString(16).padStart(12, "0");
  const randHex = hex(r1);
  // randHex is 20 hex chars: [0..12) 12-bit rand_a + ver nibble, [12..16)..., rest
  const randA = (parseInt(randHex.slice(0, 3), 16) & 0x0fff) | 0x7000;
  const randBHi = (parseInt(randHex.slice(3, 5), 16) & 0x3f) | 0x80;
  return [
    unixHex.slice(0, 8),
    unixHex.slice(8, 12),
    randA.toString(16).padStart(4, "0"),
    randBHi.toString(16).padStart(2, "0") + randHex.slice(5, 7),
    randHex.slice(7, 19),
  ].join("-");
}
