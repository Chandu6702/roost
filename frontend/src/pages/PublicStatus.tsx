import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { PublicStatus } from '../api/types'
import { badgeClass, fmtTime } from '../lib/format'

// This mirrors the unauthenticated /public/status endpoint. In production it is
// served at <slug>.roost.local and the tenant is resolved from the Host header;
// locally, set VITE_PUBLIC_HOST (see frontend/README) so the dev proxy forwards
// a seeded tenant's subdomain. Without it the API returns 404 and we explain why.
export default function PublicStatusPage() {
  const [status, setStatus] = useState<PublicStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [notResolved, setNotResolved] = useState(false)

  useEffect(() => {
    api
      .get<PublicStatus>('/public/status')
      .then(({ data }) => setStatus(data))
      .catch((err) => {
        if (err?.response?.status === 404) setNotResolved(true)
      })
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="muted">Loading…</p>

  if (notResolved) {
    return (
      <div className="card">
        <h1>Public status page</h1>
        <p className="muted">
          No tenant resolved from the host. In production this page lives at{' '}
          <code>&lt;slug&gt;.roost.local</code> and the tenant is read from the Host header.
        </p>
        <p className="muted">
          To preview it locally, start the dev server with <code>VITE_PUBLIC_HOST=acme.roost.local</code> (a seeded
          workspace slug) — see <code>frontend/README.md</code>.
        </p>
      </div>
    )
  }

  if (!status) return <p className="error">Could not load status.</p>

  return (
    <>
      <div className="row" style={{ marginBottom: '1rem' }}>
        <div className="grow">
          <h1>{status.workspace}</h1>
          <span className="muted">Updated {fmtTime(status.generatedAt)}</span>
        </div>
        <span className={badgeClass(status.overall)} style={{ fontSize: '0.9rem' }}>
          {status.overall}
        </span>
      </div>

      <div className="card">
        <h2>Services</h2>
        {status.monitors.length === 0 ? (
          <p className="muted">No published monitors.</p>
        ) : (
          <table>
            <tbody>
              {status.monitors.map((m) => (
                <tr key={m.name}>
                  <td>{m.name}</td>
                  <td style={{ textAlign: 'right' }}>
                    <span className={badgeClass(m.status)}>{m.status}</span>
                  </td>
                  <td className="muted" style={{ textAlign: 'right' }}>
                    {fmtTime(m.lastCheckedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {status.recentIncidents.length > 0 && (
        <div className="card">
          <h2>Recent incidents</h2>
          <table>
            <tbody>
              {status.recentIncidents.map((inc, i) => (
                <tr key={i}>
                  <td>{inc.monitor}</td>
                  <td>
                    <span className={badgeClass(inc.status)}>{inc.status}</span>
                  </td>
                  <td className="muted" style={{ textAlign: 'right' }}>
                    {fmtTime(inc.openedAt)} → {fmtTime(inc.resolvedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
