// Phone numbers as identifiers. The same number written three ways must match
// one person, so every number is stored in one form: "+" and digits (E.164),
// or bare digits when it cannot be placed (a short code).

/** `countryCode` applies to numbers written without one: "(415) 555-0100" → "+14155550100". */
export function normalizePhone(raw: string, countryCode = "1"): string | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;
  const digits = trimmed.replace(/[^\d]/g, "");
  if (digits.length < 3) return null;
  if (trimmed.startsWith("+")) return `+${digits}`;
  if (trimmed.startsWith("00")) return `+${digits.slice(2)}`;
  if (countryCode === "1") {
    // North American numbering: ten digits, or eleven with the leading 1.
    if (digits.length === 10) return `+1${digits}`;
    if (digits.length === 11 && digits.startsWith("1")) return `+${digits}`;
  } else if (digits.length >= 8 && digits.startsWith("0")) {
    // A national number with its trunk 0: "020 7946 0018" in the UK.
    return `+${countryCode}${digits.slice(1)}`;
  } else if (digits.length >= 8) {
    return `+${countryCode}${digits}`;
  }
  return digits;
}
