import { useEffect, useState } from 'react'
import { api, errorMessage } from '../api/client'
import type { Incident } from '../api/types'
import { badgeClass, fmtTime } from '../lib/format'

export default function Incidents() {
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function load() {
    setError('')
    try {
      const { data } = await api.get<Incident[]>('/incidents', { params: { size: 100 } })
      setIncidents(data)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function acknowledge(inc: Incident) {
    try {
      await api.post(`/monitors/${inc.monitorId}/incidents/${inc.id}/acknowledge`)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <>
      <h1>Incidents</h1>
      <p className="muted">Opened automatically when a monitor goes down, auto-resolved on recovery.</p>
      {error && <p className="error">{error}</p>}
      <div className="card">
        {loading ? (
          <p className="muted">Loading…</p>
        ) : incidents.length === 0 ? (
          <p className="muted">No incidents. All monitors healthy. 🎉</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Monitor</th>
                <th>Status</th>
                <th>Opened</th>
                <th>Acknowledged</th>
                <th>Resolved</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {incidents.map((inc) => (
                <tr key={inc.id}>
                  <td>{inc.monitorName}</td>
                  <td>
                    <span className={badgeClass(inc.status)}>{inc.status}</span>
                  </td>
                  <td className="muted">{fmtTime(inc.openedAt)}</td>
                  <td className="muted">{fmtTime(inc.acknowledgedAt)}</td>
                  <td className="muted">{fmtTime(inc.resolvedAt)}</td>
                  <td style={{ textAlign: 'right' }}>
                    {inc.status === 'OPEN' && (
                      <button className="ghost" onClick={() => acknowledge(inc)}>
                        Acknowledge
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  )
}
