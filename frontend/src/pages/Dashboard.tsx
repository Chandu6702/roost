import { useEffect, useState, type FormEvent } from 'react'
import { api, errorMessage } from '../api/client'
import type { Monitor, MonitorType, Page, Workspace } from '../api/types'
import { fmtTime } from '../lib/format'

export default function Dashboard() {
  const [workspace, setWorkspace] = useState<Workspace | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .get<Workspace[]>('/workspaces/mine')
      .then(({ data }) => setWorkspace(data[0] ?? null))
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="muted">Loading…</p>
  if (error) return <p className="error">{error}</p>
  if (!workspace) return <CreateWorkspace onCreated={setWorkspace} />
  return <Monitors workspace={workspace} />
}

function CreateWorkspace({ onCreated }: { onCreated: (w: Workspace) => void }) {
  const [name, setName] = useState('')
  const [slug, setSlug] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const { data } = await api.post<Workspace>('/workspaces', { name, slug })
      onCreated(data)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card" style={{ maxWidth: 440 }}>
      <h1>Create your workspace</h1>
      <p className="muted">One workspace per account. Its slug becomes your public status subdomain.</p>
      <form onSubmit={onSubmit} className="stack">
        <div>
          <label htmlFor="name">Name</label>
          <input id="name" value={name} onChange={(e) => setName(e.target.value)} placeholder="Acme Inc" required />
        </div>
        <div>
          <label htmlFor="slug">Slug</label>
          <input
            id="slug"
            value={slug}
            onChange={(e) => setSlug(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''))}
            placeholder="acme"
            required
          />
          <span className="muted">→ acme.roost.local</span>
        </div>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={busy}>
          {busy ? 'Creating…' : 'Create workspace'}
        </button>
      </form>
    </div>
  )
}

function Monitors({ workspace }: { workspace: Workspace }) {
  const [monitors, setMonitors] = useState<Monitor[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function load() {
    setError('')
    try {
      const { data } = await api.get<Page<Monitor>>('/monitors', {
        params: { workspaceId: workspace.id, size: 100 },
      })
      setMonitors(data.content)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspace.id])

  async function remove(id: number) {
    if (!confirm('Delete this monitor?')) return
    try {
      await api.delete(`/monitors/${id}`)
      setMonitors((m) => m.filter((x) => x.id !== id))
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function setPublished(id: number, value: boolean) {
    try {
      await api.put(`/monitors/${id}/published`, null, { params: { value } })
      setNotice(value ? 'Added to the public status page.' : 'Removed from the public status page.')
      setTimeout(() => setNotice(''), 2500)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <>
      <div className="row" style={{ marginBottom: '1rem' }}>
        <div className="grow">
          <h1>{workspace.name}</h1>
          <span className="muted">Monitors in this workspace</span>
        </div>
      </div>

      <CreateMonitor workspaceId={workspace.id} onCreated={(m) => setMonitors((cur) => [m, ...cur])} />

      {notice && <p className="muted">{notice}</p>}
      {error && <p className="error">{error}</p>}

      <div className="card">
        {loading ? (
          <p className="muted">Loading monitors…</p>
        ) : monitors.length === 0 ? (
          <p className="muted">No monitors yet. Add one above to start probing.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>URL</th>
                <th>Type</th>
                <th>Interval</th>
                <th>Created</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {monitors.map((m) => (
                <tr key={m.id}>
                  <td>{m.name}</td>
                  <td className="muted" style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {m.url}
                  </td>
                  <td>{m.type}</td>
                  <td>{m.interval_seconds}s</td>
                  <td className="muted">{fmtTime(m.createdAt)}</td>
                  <td>
                    <div className="row" style={{ gap: '0.35rem', justifyContent: 'flex-end' }}>
                      <button className="ghost" onClick={() => setPublished(m.id, true)}>
                        Publish
                      </button>
                      <button className="ghost" onClick={() => setPublished(m.id, false)}>
                        Unpublish
                      </button>
                      <button className="danger" onClick={() => remove(m.id)}>
                        Delete
                      </button>
                    </div>
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

function CreateMonitor({ workspaceId, onCreated }: { workspaceId: number; onCreated: (m: Monitor) => void }) {
  const [name, setName] = useState('')
  const [url, setUrl] = useState('')
  const [type, setType] = useState<MonitorType>('HTTP')
  const [interval, setInterval] = useState(60)
  const [timeout, setTimeout] = useState(5000)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const { data } = await api.post<Monitor>('/monitors', {
        workspaceId,
        name,
        url,
        type,
        interval_seconds: interval,
        timeout_millis: timeout,
      })
      onCreated(data)
      setName('')
      setUrl('')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card">
      <h2>Add a monitor</h2>
      <form onSubmit={onSubmit}>
        <div className="row">
          <div className="grow">
            <label htmlFor="mname">Name</label>
            <input id="mname" value={name} onChange={(e) => setName(e.target.value)} placeholder="Prod API" required />
          </div>
          <div className="grow">
            <label htmlFor="murl">URL</label>
            <input id="murl" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://example.com/health" required />
          </div>
        </div>
        <div className="row">
          <div>
            <label htmlFor="mtype">Type</label>
            <select id="mtype" value={type} onChange={(e) => setType(e.target.value as MonitorType)}>
              <option value="HTTP">HTTP</option>
              <option value="TCP">TCP</option>
            </select>
          </div>
          <div>
            <label htmlFor="mint">Interval (s)</label>
            <input id="mint" type="number" min={30} max={3600} value={interval} onChange={(e) => setInterval(Number(e.target.value))} />
          </div>
          <div>
            <label htmlFor="mto">Timeout (ms)</label>
            <input id="mto" type="number" min={100} max={60000} value={timeout} onChange={(e) => setTimeout(Number(e.target.value))} />
          </div>
          <div style={{ alignSelf: 'flex-end' }}>
            <button type="submit" disabled={busy}>
              {busy ? 'Adding…' : 'Add monitor'}
            </button>
          </div>
        </div>
        {error && <p className="error">{error}</p>}
      </form>
    </div>
  )
}
