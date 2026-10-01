import { useCallback, useEffect, useState } from 'react'
import { ApiError, api } from '../api/client'
import type { AuditView, PageResponse } from '../api/types'
import { formatDateTime, humaniseAction } from '../lib/format'

export function AuditPage() {
  const [page, setPage] = useState<PageResponse<AuditView> | null>(null)
  const [pageIndex, setPageIndex] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(() => {
    setError(null)
    api
      .auditLogs({ page: pageIndex, size: 50 })
      .then(setPage)
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : 'Unable to load the audit trail.'),
      )
  }, [pageIndex])

  useEffect(load, [load])

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Audit trail</h1>
          <p>Every privileged action, newest first, including refused attempts.</p>
        </div>
        <button className="secondary" onClick={load}>
          Refresh
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card table-wrap">
        {!page ? (
          <div className="spinner">Loading audit entries…</div>
        ) : page.content.length === 0 ? (
          <div className="empty">No administrative actions recorded yet.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>When</th>
                <th>Administrator</th>
                <th>Action</th>
                <th>Target</th>
                <th>Result</th>
                <th>Detail</th>
                <th>Source IP</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((entry) => (
                <tr key={entry.id}>
                  <td className="muted" style={{ whiteSpace: 'nowrap' }}>
                    {formatDateTime(entry.createdAt)}
                  </td>
                  <td>{entry.adminName ?? '—'}</td>
                  <td>{humaniseAction(entry.action)}</td>
                  <td className="mono">
                    {entry.targetType ?? '—'}
                    {entry.targetId != null ? ` #${entry.targetId}` : ''}
                  </td>
                  <td>
                    <span className={`badge badge-${entry.result === 'SUCCESS' ? 'SUCCESS' : 'LOCKED'}`}>
                      {entry.result}
                    </span>
                  </td>
                  <td className="muted">{entry.detail ?? '—'}</td>
                  <td className="mono muted">{entry.ipAddress ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {page && page.totalPages > 1 && (
        <div className="pagination">
          <button
            className="secondary small"
            disabled={pageIndex === 0}
            onClick={() => setPageIndex((index) => index - 1)}
          >
            Previous
          </button>
          <span>
            Page {page.page + 1} of {page.totalPages} ({page.totalElements} entries)
          </span>
          <button
            className="secondary small"
            disabled={pageIndex >= page.totalPages - 1}
            onClick={() => setPageIndex((index) => index + 1)}
          >
            Next
          </button>
        </div>
      )}
    </>
  )
}