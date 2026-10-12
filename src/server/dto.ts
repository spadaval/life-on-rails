// Server-function DTO boundary: postgres.js rows carry Date/Buffer/JSON
// values that Start's serializability validator rejects as `unknown`.
// Round-trip through JSON (Dates → ISO strings) and cast to concrete DTOs.
export type Row = Record<string, string | number | boolean | null>;

export function dto(rows: unknown): Row[] {
  return JSON.parse(JSON.stringify(rows)) as Row[];
}

export function one(rows: unknown): Row | null {
  const list = dto(rows);
  return list[0] ?? null;
}
