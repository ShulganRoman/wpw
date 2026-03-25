export function LoadingSpinner({ text = 'Loading…' }) {
  return (
    <div className="loading-container">
      <div className="spinner" />
      <span>{text}</span>
    </div>
  );
}

export function SkeletonCard() {
  return (
    <div className="skeleton-card">
      <div className="skeleton-img skeleton" />
      <div className="skeleton-body">
        <div className="skeleton-line skeleton" style={{ width: '55%' }} />
        <div className="skeleton-line skeleton" style={{ width: '90%' }} />
        <div className="skeleton-line skeleton" style={{ width: '70%' }} />
      </div>
    </div>
  );
}

export function SkeletonGrid({ count = 12 }) {
  return (
    <div className="product-grid">
      {Array.from({ length: count }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon">⚠️</div>
      <h3>Something went wrong</h3>
      <p>{message || 'An unexpected error occurred.'}</p>
      {onRetry && (
        <button className="btn btn-secondary" style={{ marginTop: '16px' }} onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
