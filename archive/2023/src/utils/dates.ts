export function toMysqlDate(date: Date) {
  return date.toISOString().slice(0, 19).replace('T', ' ');
}