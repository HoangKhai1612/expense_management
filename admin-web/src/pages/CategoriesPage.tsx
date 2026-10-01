import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError, api } from '../api/client'
import type { AdminCategoryView, CategoryType } from '../api/types'

export function CategoriesPage() {
  const [categories, setCategories] = useState<AdminCategoryView[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [type, setType] = useState<CategoryType>('EXPENSE')
  const [color, setColor] = useState('#3b82f6')
  const [icon, setIcon] = useState('wallet')
  const [saving, setSaving] = useState(false)

  const load = useCallback(() => {
    setError(null)
    api
      .categories()
      .then(setCategories)
      .catch((err: unknown) =>
        setError(err instanceof ApiError ? err.message : 'Unable to load categories.'),
      )
  }, [])

  useEffect(load, [load])

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    setNotice(null)
    try {
      await api.createCategory({ name, code, type, icon, color })
      setNotice(`Category "${name}" added to the system catalogue.`)
      setName('')
      setCode('')
      load()
    } catch (err) {
      // A duplicate code is the common case and deserves its own wording.
      setError(
        err instanceof ApiError
          ? `${err.code}: ${err.message}`
          : 'Could not create the category.',
      )
    } finally {
      setSaving(false)
    }
  }

  async function toggleActive(category: AdminCategoryView) {
    setBusyId(category.id)
    setError(null)
    try {
      await api.updateCategory(category.id, { active: !category.active })
      setNotice(
        `"${category.name}" is now ${category.active ? 'hidden from users' : 'visible again'}.`,
      )
      load()
    } catch (err) {
      setError(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed.')
    } finally {
      setBusyId(null)
    }
  }

  async function rename(category: AdminCategoryView) {
    const next = window.prompt('New name', category.name)
    if (!next || next.trim() === category.name) return
    setBusyId(category.id)
    setError(null)
    try {
      await api.updateCategory(category.id, { name: next.trim() })
      setNotice(`"${category.name}" renamed to "${next.trim()}".`)
      load()
    } catch (err) {
      setError(err instanceof ApiError ? `${err.code}: ${err.message}` : 'Update failed.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>System categories</h1>
          <p>
            Deactivating a category hides it from pickers. Existing transactions keep working, so user
            history is never rewritten.
          </p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}

      <div className="card" style={{ marginBottom: 20 }}>
        <h2 style={{ marginTop: 0, fontSize: 16 }}>Add a category</h2>
        <form onSubmit={onCreate}>
          <div className="form-grid">
            <div>
              <label htmlFor="cat-name">Name</label>
              <input id="cat-name" value={name} onChange={(e) => setName(e.target.value)} required />
            </div>
            <div>
              <label htmlFor="cat-code">Code</label>
              <input
                id="cat-code"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                placeholder="PETS"
                required
              />
            </div>
            <div>
              <label htmlFor="cat-type">Type</label>
              <select
                id="cat-type"
                value={type}
                onChange={(e) => setType(e.target.value as CategoryType)}
              >
                <option value="EXPENSE">Expense</option>
                <option value="INCOME">Income</option>
              </select>
            </div>
            <div>
              <label htmlFor="cat-icon">Icon key</label>
              <input id="cat-icon" value={icon} onChange={(e) => setIcon(e.target.value)} />
            </div>
            <div>
              <label htmlFor="cat-color">Colour</label>
              <input
                id="cat-color"
                type="color"
                value={color}
                onChange={(e) => setColor(e.target.value)}
                style={{ height: 36, padding: 3 }}
              />
            </div>
          </div>
          <button type="submit" disabled={saving || !name || !code}>
            {saving ? 'Creating…' : 'Create category'}
          </button>
        </form>
      </div>

      <div className="card table-wrap">
        {!categories ? (
          <div className="spinner">Loading categories…</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Code</th>
                <th>Type</th>
                <th>Used by</th>
                <th>State</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((category) => (
                <tr key={category.id}>
                  <td>
                    <span className="swatch" style={{ background: category.color ?? '#888' }} />
                    {category.name}
                  </td>
                  <td className="mono">{category.code}</td>
                  <td>
                    <span className="badge badge-NEUTRAL">{category.type}</span>
                  </td>
                  <td>{category.usageCount}</td>
                  <td>
                    <span className={`badge badge-${category.active ? 'ACTIVE' : 'DEACTIVATED'}`}>
                      {category.active ? 'ACTIVE' : 'HIDDEN'}
                    </span>
                  </td>
                  <td>
                    <button
                      className="small secondary"
                      disabled={busyId === category.id}
                      onClick={() => rename(category)}
                    >
                      Rename
                    </button>
                    <button
                      className="small secondary"
                      style={{ marginLeft: 6 }}
                      disabled={busyId === category.id}
                      onClick={() => toggleActive(category)}
                    >
                      {category.active ? 'Deactivate' : 'Activate'}
                    </button>
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