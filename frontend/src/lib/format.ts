export function badgeClass(status: string): string {
  switch (status?.toUpperCase()) {
    case 'UP':
    case 'RESOLVED':
      return 'badge up'
    case 'DOWN':
    case 'OPEN':
      return 'badge down'
    case 'ACK':
    case 'UNKNOWN':
      return 'badge warn'
    default:
      return 'badge muted'
  }
}

export function fmtTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString()
}
