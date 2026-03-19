export function required(value: string, label: string): string | null {
  if (!value || !value.trim()) return `${label} is required`;
  return null;
}

export function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

export function emailError(value: string, label = "Email"): string | null {
  const req = required(value, label);
  if (req) return req;
  if (!isEmail(value)) return `${label} is not valid`;
  return null;
}

export function numberError(
  value: string,
  label: string,
  opts: { min?: number } = {},
): string | null {
  const req = required(value, label);
  if (req) return req;
  const parsed = Number(value);
  if (Number.isNaN(parsed)) return `${label} must be a number`;
  if (opts.min != null && parsed < opts.min) return `${label} must be >= ${opts.min}`;
  return null;
}

export function isoDateError(value: string, label = "Date"): string | null {
  const req = required(value, label);
  if (req) return req;
  const trimmed = value.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return `${label} must be YYYY-MM-DD`;
  const [yearStr, monthStr, dayStr] = trimmed.split("-");
  const year = Number(yearStr);
  const month = Number(monthStr);
  const day = Number(dayStr);
  const parsed = new Date(Date.UTC(year, month - 1, day));
  const isValidDate =
    parsed.getUTCFullYear() === year &&
    parsed.getUTCMonth() === month - 1 &&
    parsed.getUTCDate() === day;
  if (!isValidDate) return `${label} is not a valid date`;
  const today = new Date();
  const todayIso = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  if (trimmed > todayIso) return `${label} cannot be in the future`;
  return null;
}

export function formatIsoDateInput(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 8);
  if (digits.length <= 4) return digits;
  if (digits.length <= 6) return `${digits.slice(0, 4)}-${digits.slice(4)}`;
  return `${digits.slice(0, 4)}-${digits.slice(4, 6)}-${digits.slice(6)}`;
}
