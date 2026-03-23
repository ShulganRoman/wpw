import { useState, useRef, useEffect } from 'react';
import { validateImport, executeImport, validatePhotos, importPhotos, getUsers, createUser, updateUser, deleteUser, getRoles } from '../api/api';
import { useToast } from '../components/ToastContext';
import AdminCatalogTree from '../components/AdminCatalogTree';

function parseMarkdown(text) {
  // Minimal markdown rendering: headings, bold, code, tables, lists
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  // Tables
  html = html.replace(/^(\|.+\|)\n(\|[-:| ]+\|)\n((?:\|.+\|\n?)*)/gm, (_, header, sep, rows) => {
    const ths = header.split('|').filter(Boolean).map(c => `<th>${c.trim()}</th>`).join('');
    const trs = rows.trim().split('\n').map(row => {
      const tds = row.split('|').filter(Boolean).map(c => `<td>${c.trim()}</td>`).join('');
      return `<tr>${tds}</tr>`;
    }).join('');
    return `<table><thead><tr>${ths}</tr></thead><tbody>${trs}</tbody></table>`;
  });

  // Headings
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');

  // Bold
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

  // Inline code
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

  // Unordered list items
  html = html.replace(/^[*-] (.+)$/gm, '<li>$1</li>');

  // Ordered list items
  html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>');

  // Wrap consecutive <li> in <ul>
  html = html.replace(/(<li>.*<\/li>\n?)+/g, match => `<ul>${match}</ul>`);

  // Paragraphs (lines not already wrapped)
  html = html.replace(/^([^<\n].+)$/gm, '<p>$1</p>');

  // Clean up empty <p></p>
  html = html.replace(/<p><\/p>/g, '');

  return html;
}

function MarkdownReport({ text }) {
  const html = parseMarkdown(text);
  return (
    <div
      className="markdown-report"
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}

function ValidationReport({ report }) {
  if (!report) return null;

  const { valid, errors, warnings, summary } = report;

  return (
    <div className="import-report">
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
        <span style={{ fontSize: 20 }}>{valid ? '✅' : '❌'}</span>
        <strong style={{ fontSize: 14, color: valid ? '#2e7d32' : '#c62828' }}>
          {valid ? 'Validation Passed' : 'Validation Failed'}
        </strong>
      </div>

      {summary && (
        <div style={{ marginBottom: 12, fontSize: 13, color: 'var(--wpw-gray)' }}>
          {typeof summary === 'string' ? summary : JSON.stringify(summary)}
        </div>
      )}

      {errors && errors.length > 0 && (
        <div style={{ marginBottom: 10 }}>
          <strong style={{ fontSize: 12, color: '#c62828', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Errors ({errors.length})
          </strong>
          <ul style={{ marginTop: 6, paddingLeft: 20, listStyle: 'disc', display: 'flex', flexDirection: 'column', gap: 4 }}>
            {errors.slice(0, 50).map((e, i) => (
              <li key={i} style={{ fontSize: 12, color: '#c62828' }}>
                {typeof e === 'string' ? e : e.message || JSON.stringify(e)}
              </li>
            ))}
            {errors.length > 50 && (
              <li style={{ fontSize: 12, color: 'var(--wpw-mid-gray)' }}>…and {errors.length - 50} more errors</li>
            )}
          </ul>
        </div>
      )}

      {warnings && warnings.length > 0 && (
        <div>
          <strong style={{ fontSize: 12, color: '#e65100', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Warnings ({warnings.length})
          </strong>
          <ul style={{ marginTop: 6, paddingLeft: 20, listStyle: 'disc', display: 'flex', flexDirection: 'column', gap: 4 }}>
            {warnings.slice(0, 30).map((w, i) => (
              <li key={i} style={{ fontSize: 12, color: '#e65100' }}>
                {typeof w === 'string' ? w : w.message || JSON.stringify(w)}
              </li>
            ))}
            {warnings.length > 30 && (
              <li style={{ fontSize: 12, color: 'var(--wpw-mid-gray)' }}>…and {warnings.length - 30} more warnings</li>
            )}
          </ul>
        </div>
      )}

      {!errors && !warnings && !summary && (
        <pre style={{ fontSize: 12, fontFamily: 'var(--wpw-font-mono)', whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
          {JSON.stringify(report, null, 2)}
        </pre>
      )}
    </div>
  );
}

function ExcelImportTab() {
  const toast = useToast();
  const fileInputRef = useRef(null);
  const dropzoneRef = useRef(null);

  const [file, setFile] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const [validating, setValidating] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [validationReport, setValidationReport] = useState(null);
  const [executeReport, setExecuteReport] = useState(null);
  const [validationValid, setValidationValid] = useState(null);

  function handleFile(f) {
    if (!f) return;
    const ext = f.name.split('.').pop().toLowerCase();
    if (!['xlsx', 'xls'].includes(ext)) {
      toast('Please select an Excel file (.xlsx or .xls)', 'error');
      return;
    }
    setFile(f);
    setValidationReport(null);
    setExecuteReport(null);
    setValidationValid(null);
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragOver(false);
    const f = e.dataTransfer.files[0];
    handleFile(f);
  }

  function handleDragOver(e) {
    e.preventDefault();
    setDragOver(true);
  }

  function handleDragLeave() {
    setDragOver(false);
  }

  function handleClickDropzone() {
    fileInputRef.current?.click();
  }

  async function handleValidate() {
    if (!file) return;
    setValidating(true);
    setValidationReport(null);
    setExecuteReport(null);
    try {
      const report = await validateImport(file);
      setValidationReport(report);
      const isValid = report.valid !== false;
      setValidationValid(isValid);
      if (isValid) {
        toast('Validation passed — ready to import.', 'success');
      } else {
        toast('Validation failed. Please review the errors.', 'warning');
      }
    } catch (err) {
      toast(`Validation error: ${err.message}`, 'error');
    } finally {
      setValidating(false);
    }
  }

  async function handleExecute() {
    if (!file) return;
    setExecuting(true);
    setExecuteReport(null);
    try {
      const report = await executeImport(file);
      setExecuteReport(report);
      toast('Import completed successfully!', 'success');
    } catch (err) {
      toast(`Import failed: ${err.message}`, 'error');
    } finally {
      setExecuting(false);
    }
  }

  function handleReset() {
    setFile(null);
    setValidationReport(null);
    setExecuteReport(null);
    setValidationValid(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  return (
    <div className="admin-section">
      {/* Dropzone */}
      <div
        ref={dropzoneRef}
        className={`dropzone${dragOver ? ' drag-over' : ''}`}
        onClick={handleClickDropzone}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        role="button"
        tabIndex={0}
        onKeyDown={e => e.key === 'Enter' && handleClickDropzone()}
      >
        <div className="dropzone-icon">📂</div>
        <div className="dropzone-text">
          {file ? 'Click or drop to replace file' : 'Click to select or drag & drop an Excel file here'}
        </div>
        <div className="dropzone-hint">Supported formats: .xlsx, .xls</div>
        {file && <div className="dropzone-filename">📎 {file.name}</div>}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept=".xlsx,.xls"
        style={{ display: 'none' }}
        onChange={e => handleFile(e.target.files[0])}
      />

      {/* Actions */}
      <div className="import-actions">
        <button
          className="btn btn-secondary"
          onClick={handleValidate}
          disabled={!file || validating || executing}
        >
          {validating ? (
            <>
              <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
              Validating…
            </>
          ) : '✓ Validate'}
        </button>

        <button
          className="btn btn-primary"
          onClick={handleExecute}
          disabled={!file || executing || validating || validationValid === false}
          title={validationValid === false ? 'Fix validation errors before importing' : ''}
        >
          {executing ? (
            <>
              <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
              Importing…
            </>
          ) : '⬆ Execute Import'}
        </button>

        {file && (
          <button className="btn btn-secondary" onClick={handleReset}>
            ✕ Reset
          </button>
        )}
      </div>

      {validationValid === false && (
        <p style={{ marginTop: 8, fontSize: 12, color: '#c62828' }}>
          Validation must pass before executing the import.
        </p>
      )}

      {/* Validation Report */}
      {validationReport && (
        <div style={{ marginTop: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--wpw-navy)', marginBottom: 8 }}>
            Validation Report
          </div>
          <ValidationReport report={validationReport} />
        </div>
      )}

      {/* Execute Report */}
      {executeReport && (
        <div style={{ marginTop: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--wpw-navy)', marginBottom: 8 }}>
            Import Report
          </div>
          <div className="import-report">
            <MarkdownReport text={executeReport} />
          </div>
        </div>
      )}

      {/* Instructions */}
      <div className="card" style={{ marginTop: 24 }}>
        <div className="card-title">Import Instructions</div>
        <ol style={{ paddingLeft: 20, listStyle: 'decimal', display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13, color: 'var(--wpw-gray)' }}>
          <li>Prepare an Excel file (.xlsx) with product data following the required column structure.</li>
          <li>Upload the file using the drop zone above.</li>
          <li>Click <strong>Validate</strong> to check for errors before importing.</li>
          <li>If validation passes, click <strong>Execute Import</strong> to apply the changes.</li>
          <li>Review the import report for details on what was added or updated.</li>
        </ol>
      </div>
    </div>
  );
}

function PhotoImportTab() {
  const toast = useToast();
  const [files, setFiles] = useState([]);
  const [dragging, setDragging] = useState(false);
  const [validating, setValidating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [validation, setValidation] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const inputRef = useRef(null);

  function handleFiles(fileList) {
    const images = Array.from(fileList).filter(f =>
      /\.(jpe?g|png|webp)$/i.test(f.name)
    );
    if (images.length === 0) {
      toast('No image files selected (jpg, png, webp only)', 'warning');
      return;
    }
    setFiles(images);
    setValidation(null);
    setImportResult(null);
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragging(false);
    handleFiles(e.dataTransfer.files);
  }

  async function handleValidate() {
    if (files.length === 0) return;
    setValidating(true);
    setImportResult(null);
    try {
      const result = await validatePhotos(files);
      setValidation(result);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setValidating(false);
    }
  }

  async function handleImport() {
    if (files.length === 0) return;
    setImporting(true);
    try {
      const result = await importPhotos(files);
      setImportResult(result);
      toast(`Imported ${result.converted} photos for ${result.matchedProducts} products`, 'success');
      setFiles([]);
      setValidation(null);
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setImporting(false);
    }
  }

  return (
    <div>
      <div
        className={`dropzone ${dragging ? 'dragover' : ''}`}
        onDrop={handleDrop}
        onDragOver={e => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onClick={() => inputRef.current?.click()}
      >
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          multiple
          style={{ display: 'none' }}
          onChange={e => handleFiles(e.target.files)}
        />
        {files.length > 0 ? (
          <div>
            <div className="dropzone-icon">🖼️</div>
            <div className="dropzone-text">{files.length} images selected</div>
            <div className="dropzone-hint">Click or drop to replace</div>
          </div>
        ) : (
          <div>
            <div className="dropzone-icon">📸</div>
            <div className="dropzone-text">Drop product photos here</div>
            <div className="dropzone-hint">JPG, PNG, or WebP · Name files as TOOLNO.jpg or TOOLNO_2.jpg</div>
          </div>
        )}
      </div>

      {files.length > 0 && (
        <div className="import-actions">
          <button className="btn btn-primary" onClick={handleValidate} disabled={validating}>
            {validating ? 'Validating…' : 'Validate Photos'}
          </button>
          {validation && validation.matched > 0 && (
            <button className="btn btn-primary" onClick={handleImport} disabled={importing}
              style={{ marginLeft: 8 }}>
              {importing ? 'Importing…' : `Import ${validation.matched} Matched Photos`}
            </button>
          )}
        </div>
      )}

      {validation && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-body">
            <h3 style={{ marginBottom: 12 }}>Validation Result</h3>
            <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
              <div className="stat-badge" style={{ background: '#e8f5e9', color: '#2e7d32', padding: '8px 16px', borderRadius: 8 }}>
                ✓ {validation.matched} matched
              </div>
              <div className="stat-badge" style={{ background: '#fbe9e7', color: '#c62828', padding: '8px 16px', borderRadius: 8 }}>
                ✗ {validation.unmatched} unmatched
              </div>
              <div style={{ padding: '8px 16px', color: '#666' }}>
                {validation.totalFiles} total files
              </div>
            </div>

            {validation.matchedFiles && validation.matchedFiles.length > 0 && (
              <details open>
                <summary style={{ cursor: 'pointer', fontWeight: 600, marginBottom: 8, color: '#2e7d32' }}>
                  Matched files ({validation.matchedFiles.length})
                </summary>
                <div style={{ maxHeight: 200, overflow: 'auto', fontSize: 13 }}>
                  {validation.matchedFiles.map((f, i) => (
                    <div key={i} style={{ padding: '4px 0', borderBottom: '1px solid #eee' }}>
                      <span style={{ color: '#2e7d32' }}>✓</span> {f.filename} → <strong>{f.toolNo}</strong>
                    </div>
                  ))}
                </div>
              </details>
            )}

            {validation.unmatchedFiles && validation.unmatchedFiles.length > 0 && (
              <details style={{ marginTop: 12 }}>
                <summary style={{ cursor: 'pointer', fontWeight: 600, marginBottom: 8, color: '#c62828' }}>
                  Unmatched files ({validation.unmatchedFiles.length})
                </summary>
                <div style={{ maxHeight: 200, overflow: 'auto', fontSize: 13 }}>
                  {validation.unmatchedFiles.map((f, i) => (
                    <div key={i} style={{ padding: '4px 0', borderBottom: '1px solid #eee' }}>
                      <span style={{ color: '#c62828' }}>✗</span> {f.filename} → <span style={{ color: '#999' }}>{f.toolNo} (not found)</span>
                    </div>
                  ))}
                </div>
              </details>
            )}
          </div>
        </div>
      )}

      {importResult && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-body">
            <h3 style={{ marginBottom: 12, color: '#2e7d32' }}>Import Complete</h3>
            <div style={{ fontSize: 14, lineHeight: 1.8 }}>
              <div>Products matched: <strong>{importResult.matchedProducts}</strong></div>
              <div>Photos converted to WebP: <strong>{importResult.converted}</strong></div>
              {importResult.skipped > 0 && <div>Skipped: {importResult.skipped}</div>}
              {importResult.errors > 0 && <div style={{ color: '#c62828' }}>Errors: {importResult.errors}</div>}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const PRIVILEGE_LABELS = {
  BULK_IMPORT: 'Excel Import',
  BULK_EXPORT: 'Export',
  MODIFY_PRODUCTS: 'Edit Products',
  MANAGE_CATALOG: 'Catalog',
  CREATE_ROLES: 'Create Roles',
  MODIFY_ROLES: 'Edit Roles',
  DELETE_ROLES: 'Delete Roles',
};

function PrivilegeBadge({ name }) {
  return (
    <span style={{
      display: 'inline-block',
      padding: '2px 7px',
      borderRadius: 4,
      fontSize: 11,
      fontWeight: 500,
      background: '#e8edf5',
      color: '#1a2e50',
      margin: '2px 2px',
    }}>
      {PRIVILEGE_LABELS[name] || name}
    </span>
  );
}

function UserModal({ user, roles, onSave, onClose }) {
  const [username, setUsername] = useState(user?.username || '');
  const [password, setPassword] = useState('');
  const [roleId, setRoleId] = useState(user?.roleId || (roles[0]?.id ?? ''));
  const [enabled, setEnabled] = useState(user?.enabled ?? true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const toast = useToast();

  const selectedRole = roles.find(r => r.id === Number(roleId));

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    if (!username.trim()) { setError('Username is required'); return; }
    if (!user && !password) { setError('Password is required'); return; }
    if (!roleId) { setError('Role is required'); return; }

    const payload = { username: username.trim(), roleId: Number(roleId), enabled };
    if (password) payload.password = password;

    setSaving(true);
    try {
      await onSave(payload);
    } catch (err) {
      let msg = err.message;
      try { msg = JSON.parse(err.message)?.message || msg; } catch {}
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', zIndex: 1000,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }} onClick={e => e.target === e.currentTarget && onClose()}>
      <div style={{
        background: '#fff', borderRadius: 10, padding: 28, width: 420, maxWidth: '95vw',
        boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
      }}>
        <h3 style={{ margin: '0 0 20px', fontSize: 16, color: 'var(--wpw-navy)' }}>
          {user ? 'Edit User' : 'Create User'}
        </h3>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div>
            <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--wpw-gray)', display: 'block', marginBottom: 4 }}>
              Username
            </label>
            <input
              className="input"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="username"
              style={{ width: '100%', boxSizing: 'border-box' }}
              autoFocus
            />
          </div>

          <div>
            <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--wpw-gray)', display: 'block', marginBottom: 4 }}>
              Password
            </label>
            <input
              className="input"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder={user ? 'Leave blank to keep current' : 'Password (min 4 chars)'}
              style={{ width: '100%', boxSizing: 'border-box' }}
            />
          </div>

          <div>
            <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--wpw-gray)', display: 'block', marginBottom: 4 }}>
              Role
            </label>
            <select
              className="input"
              value={roleId}
              onChange={e => setRoleId(e.target.value)}
              style={{ width: '100%', boxSizing: 'border-box' }}
            >
              {roles.map(r => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
          </div>

          {selectedRole && selectedRole.privileges && selectedRole.privileges.length > 0 && (
            <div style={{ background: '#f5f7fa', borderRadius: 6, padding: '10px 12px' }}>
              <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--wpw-gray)', marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                Privileges
              </div>
              <div>
                {selectedRole.privileges.map(p => <PrivilegeBadge key={p} name={p} />)}
              </div>
            </div>
          )}

          {selectedRole && (!selectedRole.privileges || selectedRole.privileges.length === 0) && (
            <div style={{ background: '#f5f7fa', borderRadius: 6, padding: '10px 12px', fontSize: 12, color: '#999' }}>
              This role has no privileges assigned.
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <input
              type="checkbox"
              id="enabled-checkbox"
              checked={enabled}
              onChange={e => setEnabled(e.target.checked)}
            />
            <label htmlFor="enabled-checkbox" style={{ fontSize: 13, color: 'var(--wpw-gray)', cursor: 'pointer' }}>
              Account enabled
            </label>
          </div>

          {error && (
            <div style={{ fontSize: 12, color: '#c62828', background: '#fdecea', borderRadius: 6, padding: '8px 12px' }}>
              {error}
            </div>
          )}

          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 4 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function UsersTab() {
  const toast = useToast();
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null); // null | { user: null } | { user: UserResponse }
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  useEffect(() => {
    Promise.all([getUsers(), getRoles()])
      .then(([u, r]) => { setUsers(u); setRoles(r); })
      .catch(err => toast(`Failed to load: ${err.message}`, 'error'))
      .finally(() => setLoading(false));
  }, []);

  async function handleSave(payload) {
    if (modal.user) {
      const updated = await updateUser(modal.user.id, payload);
      setUsers(prev => prev.map(u => u.id === updated.id ? updated : u));
      toast('User updated', 'success');
    } else {
      const created = await createUser(payload);
      setUsers(prev => [...prev, created]);
      toast('User created', 'success');
    }
    setModal(null);
  }

  async function handleDelete(id) {
    try {
      await deleteUser(id);
      setUsers(prev => prev.filter(u => u.id !== id));
      toast('User deleted', 'success');
    } catch (err) {
      toast(`Delete failed: ${err.message}`, 'error');
    } finally {
      setConfirmDeleteId(null);
    }
  }

  if (loading) {
    return <div style={{ padding: 32, textAlign: 'center', color: 'var(--wpw-gray)' }}>Loading…</div>;
  }

  return (
    <div className="admin-section">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div style={{ fontSize: 13, color: 'var(--wpw-gray)' }}>
          {users.length} user{users.length !== 1 ? 's' : ''}
        </div>
        <button className="btn btn-primary" onClick={() => setModal({ user: null })}>
          + Create User
        </button>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e8edf5' }}>
              {['Username', 'Role', 'Privileges', 'Status', 'Created', ''].map(h => (
                <th key={h} style={{ padding: '8px 12px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--wpw-gray)', textTransform: 'uppercase', letterSpacing: '0.04em', whiteSpace: 'nowrap' }}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {users.map(u => {
              const role = roles.find(r => r.id === u.roleId);
              const privs = role?.privileges || [];
              return (
                <tr key={u.id} style={{ borderBottom: '1px solid #f0f2f5' }}>
                  <td style={{ padding: '10px 12px', fontWeight: 600, color: 'var(--wpw-navy)' }}>
                    {u.username}
                  </td>
                  <td style={{ padding: '10px 12px', color: 'var(--wpw-gray)' }}>
                    {u.roleName}
                  </td>
                  <td style={{ padding: '10px 12px', maxWidth: 280 }}>
                    {privs.length > 0
                      ? privs.map(p => <PrivilegeBadge key={p} name={p} />)
                      : <span style={{ color: '#bbb', fontSize: 12 }}>—</span>
                    }
                  </td>
                  <td style={{ padding: '10px 12px' }}>
                    <span style={{
                      display: 'inline-block', padding: '3px 9px', borderRadius: 12, fontSize: 11, fontWeight: 600,
                      background: u.enabled ? '#e8f5e9' : '#f0f0f0',
                      color: u.enabled ? '#2e7d32' : '#999',
                    }}>
                      {u.enabled ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td style={{ padding: '10px 12px', color: 'var(--wpw-gray)', whiteSpace: 'nowrap', fontSize: 12 }}>
                    {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '—'}
                  </td>
                  <td style={{ padding: '10px 12px', whiteSpace: 'nowrap' }}>
                    {confirmDeleteId === u.id ? (
                      <span style={{ fontSize: 12, display: 'flex', gap: 6, alignItems: 'center' }}>
                        <span style={{ color: '#c62828' }}>Delete?</span>
                        <button className="btn btn-primary" style={{ padding: '3px 10px', fontSize: 12, background: '#c62828', borderColor: '#c62828' }} onClick={() => handleDelete(u.id)}>Yes</button>
                        <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => setConfirmDeleteId(null)}>No</button>
                      </span>
                    ) : (
                      <span style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => setModal({ user: u })}>
                          Edit
                        </button>
                        <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12, color: '#c62828' }} onClick={() => setConfirmDeleteId(u.id)}>
                          Delete
                        </button>
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
            {users.length === 0 && (
              <tr>
                <td colSpan={6} style={{ padding: '32px 12px', textAlign: 'center', color: 'var(--wpw-gray)', fontSize: 13 }}>
                  No users found
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {modal && (
        <UserModal
          user={modal.user}
          roles={roles}
          onSave={handleSave}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  );
}

export default function AdminPage() {
  const [tab, setTab] = useState('excel');
  const userRole = localStorage.getItem('userRole');

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Admin</h1>
        <p className="page-subtitle">Import product data and photos</p>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        <button className={`btn ${tab === 'excel' ? 'btn-primary' : ''}`} onClick={() => setTab('excel')}>Excel Import</button>
        <button className={`btn ${tab === 'photos' ? 'btn-primary' : ''}`} onClick={() => setTab('photos')}>Photo Import</button>
        <button className={`btn ${tab === 'catalog' ? 'btn-primary' : ''}`} onClick={() => setTab('catalog')}>Catalog Tree</button>
        {userRole === 'admin' && (
          <button className={`btn ${tab === 'users' ? 'btn-primary' : ''}`} onClick={() => setTab('users')}>Users</button>
        )}
      </div>

      {tab === 'excel' && <ExcelImportTab />}
      {tab === 'photos' && <PhotoImportTab />}
      {tab === 'catalog' && <AdminCatalogTree />}
      {tab === 'users' && <UsersTab />}
    </div>
  );
}
