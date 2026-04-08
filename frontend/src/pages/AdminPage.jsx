import { useState, useRef, useEffect } from 'react';
import { validateImport, executeImport, validateWpwCatalogImport, executeWpwCatalogImport, validatePhotos, importPhotos, validateArchive, importArchive, getUsers, createUser, updateUser, deleteUser, getRoles, getOperations, createApplicationTag, updateApplicationTag, deleteApplicationTag } from '../api/api';
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

function ImportPanel({ onValidate, onExecute, instructions }) {
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
    handleFile(e.dataTransfer.files[0]);
  }

  function handleReset() {
    setFile(null);
    setValidationReport(null);
    setExecuteReport(null);
    setValidationValid(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  async function handleValidate() {
    if (!file) return;
    setValidating(true);
    setValidationReport(null);
    setExecuteReport(null);
    try {
      const report = await onValidate(file);
      setValidationReport(report);
      const isValid = report.canProceed !== false;
      setValidationValid(isValid);
      toast(isValid ? 'Validation passed — ready to import.' : 'Validation failed. Please review the errors.', isValid ? 'success' : 'warning');
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
      const report = await onExecute(file);
      setExecuteReport(report);
      toast('Import completed successfully!', 'success');
    } catch (err) {
      toast(`Import failed: ${err.message}`, 'error');
    } finally {
      setExecuting(false);
    }
  }

  const hasReport = validationReport || executeReport;

  return (
    <div className="admin-import-layout">
      <div className="admin-import-left">
        <div
          ref={dropzoneRef}
          className={`dropzone${dragOver ? ' drag-over' : ''}`}
          onClick={() => fileInputRef.current?.click()}
          onDrop={handleDrop}
          onDragOver={e => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          role="button"
          tabIndex={0}
          onKeyDown={e => e.key === 'Enter' && fileInputRef.current?.click()}
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

        <div className="import-actions">
          <button className="btn btn-secondary" onClick={handleValidate} disabled={!file || validating || executing}>
            {validating ? <><div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />Validating…</> : '✓ Validate'}
          </button>
          <button
            className="btn btn-primary"
            onClick={handleExecute}
            disabled={!file || executing || validating || validationValid === false}
            title={validationValid === false ? 'Fix validation errors before importing' : ''}
          >
            {executing ? <><div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />Importing…</> : '⬆ Execute Import'}
          </button>
          {file && <button className="btn btn-secondary" onClick={handleReset}>✕ Reset</button>}
        </div>

        {validationValid === false && (
          <p style={{ marginTop: 8, fontSize: 12, color: '#c62828' }}>
            Validation must pass before executing the import.
          </p>
        )}

        <div className="card" style={{ marginTop: 24 }}>
          <div className="card-title">Import Instructions</div>
          {instructions}
        </div>
      </div>

      {hasReport && (
        <div className="admin-import-right">
          <div className="admin-report-panel">
            {validationReport && (
              <div>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--wpw-navy)', marginBottom: 8 }}>Validation Report</div>
                <ValidationReport report={validationReport} />
              </div>
            )}
            {executeReport && (
              <div style={{ marginTop: validationReport ? 20 : 0 }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--wpw-navy)', marginBottom: 8 }}>Import Report</div>
                <div className="import-report"><MarkdownReport text={executeReport} /></div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function ExcelImportTab() {
  const [mode, setMode] = useState('standard');

  return (
    <div>
      <div style={{ display: 'inline-flex', border: '1px solid var(--wpw-border)', borderRadius: 'var(--wpw-radius-sm)', overflow: 'hidden', marginBottom: 20 }} role="group">
        {[
          { value: 'standard',    label: 'Standard Format' },
          { value: 'wpw-catalog', label: 'WPW Catalog v3' },
        ].map(({ value, label }) => (
          <button
            key={value}
            onClick={() => setMode(value)}
            style={{
              padding: '7px 18px', fontSize: 13, fontWeight: 500, border: 'none',
              borderRight: value === 'standard' ? '1px solid var(--wpw-border)' : 'none',
              cursor: 'pointer',
              background: mode === value ? 'var(--wpw-blue)' : '#fff',
              color: mode === value ? '#fff' : 'var(--wpw-gray)',
              transition: 'background 0.15s, color 0.15s',
            }}
            aria-pressed={mode === value}
          >
            {label}
          </button>
        ))}
      </div>

      {mode === 'standard' && (
        <ImportPanel
          onValidate={validateImport}
          onExecute={executeImport}
          instructions={
            <ol style={{ paddingLeft: 20, listStyle: 'decimal', display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13, color: 'var(--wpw-gray)' }}>
              <li>Prepare an Excel file (.xlsx) with two sheets: <strong>Products</strong> and <strong>Product Groups</strong>.</li>
              <li>Upload the file using the drop zone above.</li>
              <li>Click <strong>Validate</strong> to check for errors before importing.</li>
              <li>If validation passes, click <strong>Execute Import</strong> to apply the changes.</li>
              <li>Review the import report for details on what was added or updated.</li>
            </ol>
          }
        />
      )}
      {mode === 'wpw-catalog' && (
        <ImportPanel
          onValidate={validateWpwCatalogImport}
          onExecute={executeWpwCatalogImport}
          instructions={
            <ol style={{ paddingLeft: 20, listStyle: 'decimal', display: 'flex', flexDirection: 'column', gap: 8, fontSize: 13, color: 'var(--wpw-gray)' }}>
              <li>Use the <strong>WPW_Catalog_v3.xlsx</strong> template (single sheet <code>Sheet1</code>).</li>
              <li>Required columns: <code>SKU</code>, <code>Category</code>, <code>Group</code>.</li>
              <li>SEO columns are ignored: <code>SEO_Title_EN</code>, <code>SEO_Description_EN</code>, <code>Keywords_EN</code>, <code>URL_Slug</code>.</li>
              <li>Upload the file, click <strong>Validate</strong>, then <strong>Execute Import</strong>.</li>
            </ol>
          }
        />
      )}
    </div>
  );
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

const ARCHIVE_ACCEPT = '.zip,.7z,.tar,.tar.gz,.tgz';
const ARCHIVE_ACCEPT_PATTERN = /\.(zip|7z|tar|tar\.gz|tgz)$/i;

function ArchiveIcon() {
  return (
    <svg
      width="40"
      height="40"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ opacity: 0.45, color: 'var(--wpw-mid-gray)' }}
      aria-hidden="true"
    >
      <path d="M21 8v13H3V8" />
      <path d="M1 3h22v5H1z" />
      <path d="M10 12h4" />
    </svg>
  );
}

function IndividualPhotosTab() {
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
        role="button"
        tabIndex={0}
        onKeyDown={e => e.key === 'Enter' && inputRef.current?.click()}
        aria-label="Drop product photos here or click to browse"
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
            <div className="dropzone-icon">🖼</div>
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
            {validating ? (
              <>
                <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                Validating…
              </>
            ) : 'Validate Photos'}
          </button>
          {validation && validation.matched > 0 && (
            <button className="btn btn-primary" onClick={handleImport} disabled={importing}>
              {importing ? (
                <>
                  <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                  Importing…
                </>
              ) : `Import ${validation.matched} Matched Photos`}
            </button>
          )}
        </div>
      )}

      {validation && (
        <div className="card" style={{ marginTop: 16 }}>
          <div className="card-body">
            <h3 style={{ marginBottom: 12 }}>Validation Result</h3>
            <div style={{ display: 'flex', gap: 16, marginBottom: 16, flexWrap: 'wrap' }}>
              <div style={{ background: '#e8f5e9', color: '#2e7d32', padding: '8px 16px', borderRadius: 8, fontSize: 13, fontWeight: 500 }}>
                {validation.matched} matched
              </div>
              <div style={{ background: '#fbe9e7', color: '#c62828', padding: '8px 16px', borderRadius: 8, fontSize: 13, fontWeight: 500 }}>
                {validation.unmatched} unmatched
              </div>
              <div style={{ padding: '8px 16px', color: '#666', fontSize: 13 }}>
                {validation.totalFiles} total files
              </div>
            </div>

            {validation.matchedFiles && validation.matchedFiles.length > 0 && (
              <details open>
                <summary style={{ cursor: 'pointer', fontWeight: 600, marginBottom: 8, color: '#2e7d32', fontSize: 13 }}>
                  Matched files ({validation.matchedFiles.length})
                </summary>
                <div style={{ maxHeight: 200, overflow: 'auto', fontSize: 13 }}>
                  {validation.matchedFiles.map((f, i) => (
                    <div key={i} style={{ padding: '4px 0', borderBottom: '1px solid #eee' }}>
                      <span style={{ color: '#2e7d32', marginRight: 6 }}>+</span>
                      {f.filename} → <strong>{f.toolNo}</strong>
                    </div>
                  ))}
                </div>
              </details>
            )}

            {validation.unmatchedFiles && validation.unmatchedFiles.length > 0 && (
              <details style={{ marginTop: 12 }}>
                <summary style={{ cursor: 'pointer', fontWeight: 600, marginBottom: 8, color: '#c62828', fontSize: 13 }}>
                  Unmatched files ({validation.unmatchedFiles.length})
                </summary>
                <div style={{ maxHeight: 200, overflow: 'auto', fontSize: 13 }}>
                  {validation.unmatchedFiles.map((f, i) => (
                    <div key={i} style={{ padding: '4px 0', borderBottom: '1px solid #eee' }}>
                      <span style={{ color: '#c62828', marginRight: 6 }}>-</span>
                      {f.filename} → <span style={{ color: '#999' }}>{f.toolNo} (not found)</span>
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

function ArchiveImportTab() {
  const toast = useToast();
  const [archiveFile, setArchiveFile] = useState(null);
  const [dragging, setDragging] = useState(false);
  const [validating, setValidating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [validation, setValidation] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [unmatchedOpen, setUnmatchedOpen] = useState(false);
  const inputRef = useRef(null);

  function handleFile(f) {
    if (!f) return;
    if (!ARCHIVE_ACCEPT_PATTERN.test(f.name)) {
      toast('Unsupported file type. Please select a ZIP, 7Z, TAR, TAR.GZ, or TGZ archive.', 'error');
      return;
    }
    setArchiveFile(f);
    setValidation(null);
    setImportResult(null);
    setUnmatchedOpen(false);
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragging(false);
    const f = e.dataTransfer.files[0];
    handleFile(f);
  }

  function handleRemove() {
    setArchiveFile(null);
    setValidation(null);
    setImportResult(null);
    setUnmatchedOpen(false);
    if (inputRef.current) inputRef.current.value = '';
  }

  async function handleValidate() {
    if (!archiveFile) return;
    setValidating(true);
    setValidation(null);
    setImportResult(null);
    try {
      const result = await validateArchive(archiveFile);
      setValidation(result);
      if (result.matched > 0) {
        toast(`Found ${result.imageFiles} images, matched ${result.matched} products`, 'success');
      } else {
        toast('Validation complete — no matched products found', 'warning');
      }
    } catch (err) {
      toast(`Validation failed: ${err.message}`, 'error');
    } finally {
      setValidating(false);
    }
  }

  async function handleImport() {
    if (!archiveFile || !validation || validation.matched === 0) return;
    setImporting(true);
    try {
      const result = await importArchive(archiveFile);
      setImportResult(result);
      toast(`Archive import complete — ${result.converted} photos imported for ${result.productsUpdated} products`, 'success');
      setArchiveFile(null);
      setValidation(null);
      if (inputRef.current) inputRef.current.value = '';
    } catch (err) {
      toast(`Import failed: ${err.message}`, 'error');
    } finally {
      setImporting(false);
    }
  }

  const canImport = validation !== null && validation.matched > 0 && !importing && !validating;

  return (
    <div>
      {/* Dropzone — hidden after file is selected */}
      {!archiveFile && (
        <div
          className={`dropzone${dragging ? ' drag-over' : ''}`}
          onDrop={handleDrop}
          onDragOver={e => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onClick={() => inputRef.current?.click()}
          role="button"
          tabIndex={0}
          onKeyDown={e => e.key === 'Enter' && inputRef.current?.click()}
          aria-label="Drop archive file here or click to browse"
        >
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 12 }}>
            <ArchiveIcon />
          </div>
          <div className="dropzone-text">Drag and drop archive file here or click to browse</div>
          <div className="dropzone-hint" style={{ marginTop: 8 }}>
            Accepted formats: .zip, .7z, .tar, .tar.gz, .tgz
          </div>
          <div className="dropzone-hint" style={{ marginTop: 4 }}>
            Up to 500 MB
          </div>
        </div>
      )}

      <input
        ref={inputRef}
        type="file"
        accept={ARCHIVE_ACCEPT}
        style={{ display: 'none' }}
        onChange={e => handleFile(e.target.files[0])}
      />

      {/* Selected file row */}
      {archiveFile && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          padding: '12px 16px',
          background: 'var(--wpw-off-white)',
          border: '1px solid var(--wpw-border)',
          borderRadius: 'var(--wpw-radius)',
          marginBottom: 16,
        }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <ArchiveIcon />
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{
              fontFamily: 'var(--wpw-font-mono)',
              fontSize: 13,
              color: 'var(--wpw-navy)',
              fontWeight: 500,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}>
              {archiveFile.name}
            </div>
            <div style={{ fontSize: 12, color: 'var(--wpw-mid-gray)', marginTop: 2 }}>
              {formatFileSize(archiveFile.size)}
            </div>
          </div>
          <button
            className="btn btn-secondary"
            onClick={handleRemove}
            disabled={validating || importing}
            style={{ flexShrink: 0 }}
          >
            Remove
          </button>
        </div>
      )}

      {/* Actions */}
      {archiveFile && (
        <div className="import-actions">
          <button
            className="btn btn-secondary"
            onClick={handleValidate}
            disabled={validating || importing}
          >
            {validating ? (
              <>
                <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                Validating…
              </>
            ) : 'Validate'}
          </button>

          <button
            className="btn btn-primary"
            onClick={handleImport}
            disabled={!canImport}
            title={!validation ? 'Run validation first' : validation.matched === 0 ? 'No matched products to import' : ''}
          >
            {importing ? (
              <>
                <div className="spinner" style={{ width: 14, height: 14, borderWidth: 2 }} />
                Importing…
              </>
            ) : validation && validation.matched > 0
              ? `Import ${validation.matched} Photos`
              : 'Import'}
          </button>
        </div>
      )}

      {/* Importing long-running notice */}
      {importing && (
        <p style={{ marginTop: 10, fontSize: 12, color: 'var(--wpw-mid-gray)' }}>
          Importing... This may take a few minutes for large archives.
        </p>
      )}

      {/* Validation results */}
      {validation && (
        <div className="import-report" style={{ marginTop: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--wpw-navy)', marginBottom: 12 }}>
            Validation Result
          </div>

          {/* Summary banner */}
          <div style={{
            background: validation.matched > 0 ? '#e8f5e9' : '#fff8e1',
            border: `1px solid ${validation.matched > 0 ? '#c8e6c9' : '#ffe082'}`,
            borderRadius: 'var(--wpw-radius-sm)',
            padding: '10px 14px',
            fontSize: 13,
            fontWeight: 500,
            color: validation.matched > 0 ? '#2e7d32' : '#e65100',
            marginBottom: 14,
          }}>
            {validation.matched > 0
              ? `Found ${validation.imageFiles} images, matched ${validation.matched} products`
              : `Found ${validation.imageFiles ?? 0} images — no products matched`
            }
          </div>

          {/* Stats grid */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
            gap: 10,
            marginBottom: 14,
          }}>
            {[
              { label: 'Total files in archive', value: validation.totalFiles ?? '—' },
              { label: 'Image files found', value: validation.imageFiles ?? '—' },
              { label: 'Non-image files skipped', value: validation.nonImageFiles ?? '—' },
              { label: 'Products matched', value: validation.matched ?? '—' },
              { label: 'Products not found', value: validation.unmatched ?? '—' },
            ].map(({ label, value }) => (
              <div key={label} style={{
                background: '#fff',
                border: '1px solid var(--wpw-border)',
                borderRadius: 'var(--wpw-radius-sm)',
                padding: '10px 12px',
              }}>
                <div style={{ fontSize: 11, color: 'var(--wpw-mid-gray)', marginBottom: 4, lineHeight: 1.3 }}>
                  {label}
                </div>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--wpw-navy)' }}>
                  {value}
                </div>
              </div>
            ))}
          </div>

          {/* Unmatched filenames */}
          {validation.unmatchedFiles && validation.unmatchedFiles.length > 0 && (
            <div>
              <button
                style={{
                  background: 'none',
                  border: 'none',
                  padding: 0,
                  fontSize: 13,
                  fontWeight: 600,
                  color: '#c62828',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                }}
                onClick={() => setUnmatchedOpen(v => !v)}
                aria-expanded={unmatchedOpen}
              >
                <span style={{
                  display: 'inline-block',
                  transform: unmatchedOpen ? 'rotate(90deg)' : 'rotate(0deg)',
                  transition: 'transform 0.2s',
                  fontSize: 11,
                }}>
                  &#9654;
                </span>
                Not found ({validation.unmatchedFiles.length} filenames)
              </button>

              {unmatchedOpen && (
                <div style={{
                  marginTop: 8,
                  maxHeight: 200,
                  overflow: 'auto',
                  border: '1px solid var(--wpw-border)',
                  borderRadius: 'var(--wpw-radius-sm)',
                  background: '#fff',
                }}>
                  {validation.unmatchedFiles.map((name, i) => (
                    <div key={i} style={{
                      padding: '5px 12px',
                      borderBottom: i < validation.unmatchedFiles.length - 1 ? '1px solid var(--wpw-border)' : 'none',
                      fontSize: 12,
                      fontFamily: 'var(--wpw-font-mono)',
                      color: 'var(--wpw-gray)',
                    }}>
                      {typeof name === 'string' ? name : name.filename || JSON.stringify(name)}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Import result */}
      {importResult && (
        <div className="import-report" style={{ marginTop: 16 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: '#2e7d32', marginBottom: 12 }}>
            Import Complete
          </div>
          <div style={{ fontSize: 13, lineHeight: 2 }}>
            <div>
              Photos converted:{' '}
              <strong style={{ color: 'var(--wpw-navy)' }}>{importResult.converted ?? '—'}</strong>
            </div>
            <div>
              Products updated:{' '}
              <strong style={{ color: 'var(--wpw-navy)' }}>{importResult.productsUpdated ?? importResult.matchedProducts ?? '—'}</strong>
            </div>
            {(importResult.skipped ?? 0) > 0 && (
              <div>
                Skipped:{' '}
                <strong style={{ color: 'var(--wpw-mid-gray)' }}>{importResult.skipped}</strong>
              </div>
            )}
            {(importResult.errors ?? 0) > 0 && (
              <div style={{ color: '#c62828' }}>
                Errors: <strong>{importResult.errors}</strong>
                {importResult.errorDetails && importResult.errorDetails.length > 0 && (
                  <ul style={{ marginTop: 6, paddingLeft: 20, listStyle: 'disc', display: 'flex', flexDirection: 'column', gap: 2 }}>
                    {importResult.errorDetails.slice(0, 20).map((e, i) => (
                      <li key={i} style={{ fontSize: 12 }}>
                        {typeof e === 'string' ? e : e.message || JSON.stringify(e)}
                      </li>
                    ))}
                    {importResult.errorDetails.length > 20 && (
                      <li style={{ fontSize: 12, color: 'var(--wpw-mid-gray)' }}>
                        …and {importResult.errorDetails.length - 20} more
                      </li>
                    )}
                  </ul>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function PhotoImportTab() {
  const [mode, setMode] = useState('individual');

  return (
    <div>
      {/* Mode switcher */}
      <div style={{
        display: 'inline-flex',
        border: '1px solid var(--wpw-border)',
        borderRadius: 'var(--wpw-radius-sm)',
        overflow: 'hidden',
        marginBottom: 20,
      }}
        role="group"
        aria-label="Photo import mode"
      >
        {[
          { value: 'individual', label: 'Individual Photos' },
          { value: 'archive', label: 'Archive Import' },
        ].map(({ value, label }) => (
          <button
            key={value}
            onClick={() => setMode(value)}
            style={{
              padding: '7px 18px',
              fontSize: 13,
              fontWeight: 500,
              border: 'none',
              borderRight: value === 'individual' ? '1px solid var(--wpw-border)' : 'none',
              cursor: 'pointer',
              background: mode === value ? 'var(--wpw-blue)' : '#fff',
              color: mode === value ? '#fff' : 'var(--wpw-gray)',
              transition: 'background 0.15s, color 0.15s',
            }}
            aria-pressed={mode === value}
          >
            {label}
          </button>
        ))}
      </div>

      {mode === 'individual' && <IndividualPhotosTab />}
      {mode === 'archive' && <ArchiveImportTab />}
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

function ApplicationTagsTab() {
  const toast = useToast();
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null); // code being edited, or 'new'
  const [formName, setFormName] = useState('');
  const [formOrder, setFormOrder] = useState('');
  const [saving, setSaving] = useState(false);
  const [confirmDeleteCode, setConfirmDeleteCode] = useState(null);

  useEffect(() => {
    getOperations()
      .then(data => setTags(Array.isArray(data) ? data : []))
      .catch(err => toast(err.message, 'error'))
      .finally(() => setLoading(false));
  }, []);

  function openNew() {
    setEditingId('new');
    setFormName('');
    setFormOrder(String(tags.length + 1));
  }

  function openEdit(tag) {
    setEditingId(tag.code);
    setFormName(tag.name);
    setFormOrder(String(tag.sortOrder));
  }

  function cancelEdit() {
    setEditingId(null);
    setFormName('');
    setFormOrder('');
  }

  async function handleSave() {
    if (!formName.trim()) { toast('Name is required', 'error'); return; }
    setSaving(true);
    try {
      const payload = { name: formName.trim(), sortOrder: formOrder ? Number(formOrder) : undefined };
      if (editingId === 'new') {
        const created = await createApplicationTag(payload);
        setTags(prev => [...prev, created].sort((a, b) => a.sortOrder - b.sortOrder));
        toast('Tag created', 'success');
      } else {
        const updated = await updateApplicationTag(editingId, payload);
        setTags(prev => prev.map(t => t.code === editingId ? updated : t).sort((a, b) => a.sortOrder - b.sortOrder));
        toast('Tag updated', 'success');
      }
      cancelEdit();
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(code) {
    try {
      await deleteApplicationTag(code);
      setTags(prev => prev.filter(t => t.code !== code));
      toast('Tag deleted', 'success');
    } catch (err) {
      toast(err.message, 'error');
    } finally {
      setConfirmDeleteCode(null);
    }
  }

  if (loading) return <div style={{ padding: 32, textAlign: 'center', color: 'var(--wpw-gray)' }}>Loading…</div>;

  return (
    <div className="admin-section">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div style={{ fontSize: 13, color: 'var(--wpw-gray)' }}>
          {tags.length} tag{tags.length !== 1 ? 's' : ''}
        </div>
        <button className="btn btn-primary" onClick={openNew} disabled={editingId !== null}>
          + Create Tag
        </button>
      </div>

      {editingId === 'new' && (
        <div style={{
          background: '#f5f7fa', border: '1px solid var(--wpw-border)', borderRadius: 'var(--wpw-radius)',
          padding: '14px 16px', marginBottom: 16, display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap',
        }}>
          <input
            className="input"
            style={{ flex: '1 1 200px' }}
            placeholder="Tag name"
            value={formName}
            onChange={e => setFormName(e.target.value)}
            autoFocus
            onKeyDown={e => e.key === 'Enter' && handleSave()}
          />
          <input
            className="input"
            style={{ width: 80 }}
            type="number"
            placeholder="Order"
            value={formOrder}
            onChange={e => setFormOrder(e.target.value)}
          />
          <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </button>
          <button className="btn btn-secondary" onClick={cancelEdit} disabled={saving}>Cancel</button>
        </div>
      )}

      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e8edf5' }}>
              {['#', 'Name', 'Code', ''].map(h => (
                <th key={h} style={{ padding: '8px 12px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--wpw-gray)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {tags.map(tag => (
              <tr key={tag.code} style={{ borderBottom: '1px solid #f0f2f5' }}>
                <td style={{ padding: '10px 12px', color: 'var(--wpw-mid-gray)', width: 40 }}>{tag.sortOrder}</td>
                <td style={{ padding: '10px 12px', fontWeight: 600, color: 'var(--wpw-navy)' }}>
                  {editingId === tag.code ? (
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <input
                        className="input"
                        style={{ flex: 1, minWidth: 120 }}
                        value={formName}
                        onChange={e => setFormName(e.target.value)}
                        autoFocus
                        onKeyDown={e => e.key === 'Enter' && handleSave()}
                      />
                      <input
                        className="input"
                        style={{ width: 70 }}
                        type="number"
                        value={formOrder}
                        onChange={e => setFormOrder(e.target.value)}
                      />
                      <button className="btn btn-primary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={handleSave} disabled={saving}>
                        {saving ? '…' : 'Save'}
                      </button>
                      <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={cancelEdit}>Cancel</button>
                    </div>
                  ) : tag.name}
                </td>
                <td style={{ padding: '10px 12px', fontFamily: 'var(--wpw-font-mono)', fontSize: 12, color: 'var(--wpw-mid-gray)' }}>
                  {tag.code}
                </td>
                <td style={{ padding: '10px 12px', whiteSpace: 'nowrap' }}>
                  {editingId !== tag.code && (
                    confirmDeleteCode === tag.code ? (
                      <span style={{ fontSize: 12, display: 'flex', gap: 6, alignItems: 'center' }}>
                        <span style={{ color: '#c62828' }}>Delete?</span>
                        <button className="btn btn-primary" style={{ padding: '3px 10px', fontSize: 12, background: '#c62828', borderColor: '#c62828' }} onClick={() => handleDelete(tag.code)}>Yes</button>
                        <button className="btn btn-secondary" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => setConfirmDeleteCode(null)}>No</button>
                      </span>
                    ) : (
                      <span style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => openEdit(tag)} disabled={editingId !== null}>
                          Edit
                        </button>
                        <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: 12, color: '#c62828' }} onClick={() => setConfirmDeleteCode(tag.code)} disabled={editingId !== null}>
                          Delete
                        </button>
                      </span>
                    )
                  )}
                </td>
              </tr>
            ))}
            {tags.length === 0 && (
              <tr>
                <td colSpan={4} style={{ padding: '32px 12px', textAlign: 'center', color: 'var(--wpw-gray)', fontSize: 13 }}>
                  No application tags yet
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
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

      <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
        <button className={`btn ${tab === 'excel' ? 'btn-primary' : ''}`} onClick={() => setTab('excel')}>Excel Import</button>
        <button className={`btn ${tab === 'photos' ? 'btn-primary' : ''}`} onClick={() => setTab('photos')}>Photo Import</button>
        <button className={`btn ${tab === 'catalog' ? 'btn-primary' : ''}`} onClick={() => setTab('catalog')}>Catalog Tree</button>
        <button className={`btn ${tab === 'tags' ? 'btn-primary' : ''}`} onClick={() => setTab('tags')}>Application Tags</button>
        {userRole === 'admin' && (
          <button className={`btn ${tab === 'users' ? 'btn-primary' : ''}`} onClick={() => setTab('users')}>Users</button>
        )}
      </div>

      {tab === 'excel' && <ExcelImportTab />}
      {tab === 'photos' && <PhotoImportTab />}
      {tab === 'catalog' && <AdminCatalogTree />}
      {tab === 'tags' && <ApplicationTagsTab />}
      {tab === 'users' && <UsersTab />}
    </div>
  );
}
