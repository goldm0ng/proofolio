const DAY = 24 * 60 * 60 * 1000;

function startOfDay(d: Date): number {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

/** days until deadline (negative = overdue). null when no deadline */
export function daysUntil(iso?: string | null): number | null {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  return Math.round((startOfDay(d) - startOfDay(new Date())) / DAY);
}

export function dday(iso?: string | null): { text: string; tone: "overdue" | "urgent" | "normal" } | null {
  const n = daysUntil(iso);
  if (n === null) return null;
  if (n < 0) return { text: `D+${-n}`, tone: "overdue" };
  if (n === 0) return { text: "D-Day", tone: "urgent" };
  return { text: `D-${n}`, tone: n <= 3 ? "urgent" : "normal" };
}

export function fmtDate(iso?: string | null): string {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, "0"), day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export function fmtDateTime(iso?: string | null): string {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const hh = String(d.getHours()).padStart(2, "0"), mm = String(d.getMinutes()).padStart(2, "0");
  return `${fmtDate(iso)} ${hh}:${mm}`;
}

/** ISO string -> value for <input type="datetime-local"> */
export function toLocalInput(iso?: string | null): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** datetime-local value -> ISO with local offset */
export function fromLocalInput(v: string): string | null {
  if (!v) return null;
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return null;
  const off = -d.getTimezoneOffset();
  const sign = off >= 0 ? "+" : "-";
  const pad = (n: number) => String(Math.abs(n)).padStart(2, "0");
  const local = toLocalInput(d.toISOString());
  return `${local}:00${sign}${pad(Math.floor(off / 60))}:${pad(off % 60)}`;
}

export function fmtUsd(n: number): string {
  return `$${n.toFixed(4)}`;
}
