export default function Pagination({ page, total, perPage, onChange }) {
  const totalPages = Math.ceil(total / perPage);
  if (totalPages <= 1) return null;

  function getPages() {
    const pages = [];
    const delta = 2;
    const left = Math.max(1, page - delta);
    const right = Math.min(totalPages, page + delta);

    if (left > 1) {
      pages.push(1);
      if (left > 2) pages.push('...');
    }
    for (let i = left; i <= right; i++) pages.push(i);
    if (right < totalPages) {
      if (right < totalPages - 1) pages.push('...');
      pages.push(totalPages);
    }
    return pages;
  }

  return (
    <div className="pagination">
      <button
        className="pagination-btn"
        disabled={page === 1}
        onClick={() => onChange(page - 1)}
      >
        ‹
      </button>

      {getPages().map((p, i) =>
        p === '...' ? (
          <span key={`ellipsis-${i}`} className="pagination-ellipsis">…</span>
        ) : (
          <button
            key={p}
            className={`pagination-btn${page === p ? ' active' : ''}`}
            onClick={() => onChange(p)}
          >
            {p}
          </button>
        )
      )}

      <button
        className="pagination-btn"
        disabled={page === totalPages}
        onClick={() => onChange(page + 1)}
      >
        ›
      </button>
    </div>
  );
}
