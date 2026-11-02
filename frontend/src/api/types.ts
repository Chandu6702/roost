// Wire types — mirror the Spring Boot DTOs exactly, including the snake_case
// JSON names on MonitorRequest/MonitorResponse (@JsonProperty on the backend).

export type MonitorType = 'HTTP' | 'TCP'
export type IncidentStatus = 'OPEN' | 'ACK' | 'RESOLVED'

export interface LoginResponse {
  token: string
}

export interface Workspace {
  id: number
  name: string
  slug: string
}

export interface Monitor {
  id: number
  workspaceId: number
  name: string
  url: string
  interval_seconds: number
  timeout_millis: number
  type: MonitorType
  createdAt: string
}

export interface MonitorRequest {
  workspaceId: number
  name: string
  url: string
  interval_seconds: number
  timeout_millis: number
  type: MonitorType
}

// Spring Data Page envelope (only the fields we read).
export interface Page<T> {
  content: T[]
  totalElements: number
  number: number
  size: number
}

export interface Incident {
  id: number
  monitorId: number
  monitorName: string
  status: IncidentStatus
  openedAt: string
  acknowledgedAt: string | null
  resolvedAt: string | null
  summary: string | null
}

export interface PublicMonitorStatus {
  name: string
  status: string // UP / DOWN / UNKNOWN
  lastCheckedAt: string | null
}

export interface PublicRecentIncident {
  monitor: string
  status: string
  openedAt: string
  resolvedAt: string | null
}

export interface PublicStatus {
  workspace: string
  overall: string
  generatedAt: string
  monitors: PublicMonitorStatus[]
  recentIncidents: PublicRecentIncident[]
}
