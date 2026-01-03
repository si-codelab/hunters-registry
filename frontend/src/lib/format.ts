export function clamp01(n: number) {
  return Math.max(0, Math.min(1, n))
}

export function percent(n: number) {
  return Math.round(clamp01(n) * 100)
}

export function formatClock(hour: number, minute: number) {
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`
}
