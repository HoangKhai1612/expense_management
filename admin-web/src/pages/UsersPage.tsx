import { useCallback, useEffect, useState } from 'react'
import { ApiError, api } from '../api/client'
import type { AdminUserView, PageResponse, UserStatus } from '../api/types'
import { formatDateTime } from '../lib/format'

const STATUSES: UserStatus[] = ['ACTIVE', 'LOCKED', 'DEACTIVATED']

export function UsersPage() {
  const [page, setPage] = useState<PageResponse<AdminUserView> | null>(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<UserStatus | ''>('')
  const [pageIndex, setPageIndex] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  const load = useCallback(() => {
    setError(null)
    api
      .users({ search, status, page: pageIndex, size: 20 })
      .then(setPage)
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : 'Unable to load users.'),
      )
  }, [search, status, pageIndex])

  useEffect(load, [load])

  async function changeStatus(user: AdminUserView, next: UserStatus) {
    setBusyId(user.id)
    setError(null)
    setNotice(null)
    try {
      await api.updateUserStatus(
        user.id,
        next,
        next === 'LOCKED' ? 'Locked from the admin console' : 'Reactivated from the admin console',
      )
      setNotice(`${user.email} is now ${next}.`)
      load()
    } catch (err) {
      // CANNOT_MODIFY_SELF is a deliberate guard, not a crash: show the reason.
      setError(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Users</h1>
          <p>Search accounts, lock or deactivate them, and see how active each one is.</p>
        </div>
      </div>

      <div className="toolbar">
        <input
          placeholder="Search email, username or name"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
            setPageIndex(0)
          }}
        />
        <select
          value={status}
          onChange={(e) => {
            setStatus(e.target.value as UserStatus | '')
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

      <div className="card table-wrap">
        {!page ? (
          <div className="spinner">Loading users…</div>
        ) : page.content.length === 0 ? (
          <div className="empty">No accounts match this filter.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Username</th>
                <th>Role</th>
                <th>Status</th>
                <th>Transactions</th>
                <th>Last login</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((user) => (
                <tr key={user.id}>
                  <td>{user.email}</td>
                  <td className="mono">{user.username}</td>
                  <td>
                    <span className="badge badge-NEUTRAL">{user.role}</span>
                  </td>
                  <td>
                    <span className={`badge badge-${user.status}`}>{user.status}</span>
                  </td>
                  <td>{user.transactionCount}</td>
                  <td className="muted">{formatDateTime(user.lastLoginAt)}</td>
                  <td>
                    {user.status !== 'LOCKED' && (
                      <button
                        className="small danger"
                        disabled={busyId === user.id}
                        onClick={() => changeStatus(user, 'LOCKED')}
                      >
                        Lock
                      </button>
                    )}
                    {user.status === 'LOCKED' && (
                      <button
                        className="small"
                        disabled={busyId === user.id}
                        onClick={() => changeStatus(user, 'ACTIVE')}
                      >
                        Unlock
                      </button>
                    )}
                    {user.status !== 'DEACTIVATED' && (
                      <button
                        className="small secondary"
                        style={{ marginLeft: 6 }}
                        disabled={busyId === user.id}
                        onClick={() => changeStatus(user, 'DEACTIVATED')}
                      >
                        Deactivate
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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
              Page {page.page + 1} of {page.totalPages} ({page.totalElements} accounts)
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
      </div>
    </>
  )
}