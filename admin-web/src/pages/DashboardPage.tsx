import { useEffect, useState } from 'react'
import { ApiError, api } from '../api/client'
import type { AdminDashboard } from '../api/types'
import { formatDuration } from '../lib/format'

export function DashboardPage() {
  const [data, setData] = useState<AdminDashboard | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .dashboard()
      .then(setData)
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : 'Unable to load the dashboard.'),
      )
  }, [])

  if (error) {
    return <div className="alert alert-error">{error}</div>
  }
  if (!data) {
    return <div className="spinner">Loading metrics…</div>
  }

  const feedback = data.feedbackByStatus ?? {}

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p>Live counts read straight from the database.</p>
        </div>
      </div>

      <div className="stat-grid">
        <Stat label="Total users" value={data.totalUsers} hint={`${data.newUsersLast7Days} new in 7 days`} />
        <Stat label="Active users" value={data.activeUsers} />
        <Stat label="Locked users" value={data.lockedUsers} tone={data.lockedUsers > 0 ? 'warning' : undefined} />
        <Stat
          label="Deactivated"
          value={data.deactivatedUsers}
          tone={data.deactivatedUsers > 0 ? 'warning' : undefined}
        />
        <Stat label="Transactions" value={data.totalTransactions} />
        <Stat
          label="Categories"
          value={data.systemCategories + data.personalCategories}
          hint={`${data.systemCategories} system / ${data.personalCategories} personal`}
        />
      </div>

      <div className="stat-grid">
        <Stat label="Open feedback" value={feedback.OPEN ?? 0} tone={(feedback.OPEN ?? 0) > 0 ? 'warning' : undefined} />
        <Stat label="In progress" value={feedback.IN_PROGRESS ?? 0} />
        <Stat label="Resolved" value={feedback.RESOLVED ?? 0} />
        <Stat label="Closed" value={feedback.CLOSED ?? 0} />
      </div>

      <div className="card">
        <h2 style={{ marginTop: 0, fontSize: 16 }}>AI and runtime</h2>
        <table>
          <tbody>
            <tr>
              <th style={{ width: 220 }}>Assistant engine</th>
              <td>
                <span className="badge badge-NEUTRAL">{data.ai?.engine ?? 'LOCAL'}</span>{' '}
                {data.ai?.providerAvailable
                  ? 'external provider reachable'
                  : 'built-in analyst only'}
              </td>
            </tr>
            <tr>
              <th>Questions answered</th>
              <td>{data.ai?.totalQuestions ?? 0}</td>
            </tr>
            <tr>
              <th>JVM uptime</th>
              <td>
                {formatDuration(Number(data.runtime?.uptimeSeconds ?? 0))}{' '}
                <span className="muted">
                  {data.runtime?.uptimeSinceRestart
                    ? 'since the last restart'
                    : ''}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </>
  )
}

function Stat({
  label,
  value,
  hint,
  tone,
}: {
  label: string
  value: number
  hint?: string
  tone?: 'warning'
}) {
  return (
    <div className="stat">
      <div className="label">{label}</div>
      <div className="value" style={tone ? { color: 'var(--warning)' } : undefined}>
        {value}
      </div>
      {hint && <div className="hint">{hint}</div>}
    </div>
  )
}