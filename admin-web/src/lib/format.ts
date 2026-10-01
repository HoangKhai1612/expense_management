/** Presentation helpers shared by every screen. */

const VIETNAMESE = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 })

export function formatMoney(value: number): string {
  return `${VIETNAMESE.format(value)} ₫`
}

/**
 * The API omits null fields rather than sending them, so every helper that takes a
 * timestamp accepts `undefined` as well as `null` and renders a placeholder.
 */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const parsed = new Date(iso)
  if (Number.isNaN(parsed.getTime())) return '—'
  return parsed.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const parsed = new Date(iso)
  if (Number.isNaN(parsed.getTime())) return '—'
  return parsed.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
  })
}

/** Renders an uptime or duration in seconds as a compact human string. */
export function formatDuration(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return '—'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const parts: string[] = []
  if (days) parts.push(`${days}d`)
  if (hours) parts.push(`${hours}h`)
  if (minutes || parts.length === 0) parts.push(`${minutes}m`)
  return parts.join(' ')
}

/** ADMIN_CREATE_CATEGORY -> "Create category". */
export function humaniseAction(action: string): string {
  const withoutPrefix = action.replace(/^ADMIN_/, '').toLowerCase().replace(/_/g, ' ')
  return withoutPrefix.charAt(0).toUpperCase() + withoutPrefix.slice(1)
}