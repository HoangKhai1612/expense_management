import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Layout() {
  const { user, logout } = useAuth()

  return (
    <div className="app">
      <nav className="sidebar">
        <div className="brand">
          Finance AI
          <small>Admin console</small>
        </div>

        <NavLink to="/" end className="nav-link">
          Dashboard
        </NavLink>
        <NavLink to="/users" className="nav-link">
          Users
        </NavLink>
        <NavLink to="/categories" className="nav-link">
          Categories
        </NavLink>
        <NavLink to="/feedback" className="nav-link">
          Feedback
        </NavLink>
        <NavLink to="/audit" className="nav-link">
          Audit trail
        </NavLink>

        <div style={{ marginTop: 'auto', padding: '14px 10px 0' }}>
          <div className="muted" style={{ fontSize: 12, marginBottom: 8, wordBreak: 'break-all' }}>
            Signed in as
            <br />
            <strong style={{ color: 'var(--text)' }}>{user?.email ?? 'administrator'}</strong>
          </div>
          <button className="secondary small" onClick={logout}>
            Sign out
          </button>
        </div>
      </nav>

      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}