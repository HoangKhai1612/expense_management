import { useCallback, useEffect, useState } from 'react'
import { ApiError, api } from '../api/client'
import type { AdminFeedbackView, FeedbackStatus, PageResponse } from '../api/types'
import { formatDateTime } from '../lib/format'

const STATUSES: FeedbackStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

export function FeedbackPage() {
  const [page, setPage] = useState<PageResponse<AdminFeedbackView> | null>(null)
  const [status, setStatus] = useState<FeedbackStatus | ''>('OPEN')
  const [pageIndex, setPageIndex] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [replies, setReplies] = useState<Record<number, string>>({})

  const load = useCallback(() => {
    setError(null)
    api
      .feedback({ status, page: pageIndex, size: 20 })
      .then(setPage)
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : 'Unable to load feedback.'),
      )
  }, [status, pageIndex])

  useEffect(load, [load])

  async function triage(ticket: AdminFeedbackView, next: FeedbackStatus) {
    setError(null)
    setNotice(null)
    try {
      await api.updateFeedback(ticket.id, next, replies[ticket.id] ?? ticket.adminReply ?? null)
      setNotice(`Ticket #${ticket.id} moved to ${next}.`)
      load()
    } catch (err) {
      setError(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed.')
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Feedback</h1>
          <p>Triage tickets and reply to users. Replies are visible to the submitter.</p>
        </div>
      </div>

      <div className="toolbar">
        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as FeedbackStatus | '')
            setPageIndex(0)
          }}
        >
          <option value="">All statuses</option>
          {STATUSES.map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>
        <button className="secondary" onClick={load}>
          Refresh
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}

      {!page ? (
        <div className="spinner">Loading feedback…</div>
      ) : page.content.length === 0 ? (
        <div className="empty">No tickets in this state.</div>
      ) : (
        page.content.map((ticket) => (
          <div className="card" key={ticket.id} style={{ marginBottom: 14 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
              <div>
                <strong>{ticket.title}</strong>
                <div className="muted" style={{ fontSize: 12 }}>
                  #{ticket.id} · {ticket.category} · from {ticket.userEmail} ·{' '}
                  {formatDateTime(ticket.createdAt)}
                </div>
              </div>
              <span className={`badge badge-${ticket.status}`}>{ticket.status}</span>
            </div>

            <p style={{ margin: '10px 0' }}>{ticket.content}</p>

            {ticket.adminReply && (
              <p className="muted" style={{ fontSize: 13 }}>
                <strong>Reply sent:</strong> {ticket.adminReply}
              </p>
            )}

            <textarea
              placeholder="Reply to the user (optional)"
              value={replies[ticket.id] ?? ''}
              onChange={(e) => setReplies((current) => ({ ...current, [ticket.id]: e.target.value }))}
            />

            <div className="toolbar" style={{ marginTop: 10, marginBottom: 0 }}>
              {STATUSES.filter((value) => value !== ticket.status).map((value) => (
                <button
                  key={value}
                  className={value === 'RESOLVED' || value === 'CLOSED' ? 'small' : 'small secondary'}
                  onClick={() => triage(ticket, value)}
                >
                  Mark {value}
                </button>
              ))}
            </div>
          </div>
        ))
      )}

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
            Page {page.page + 1} of {page.totalPages}
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