export function ErrorBanner({ message }) {
  if (!message) return null;
  return (
    <div className="alert error" role="alert">
      <span aria-hidden="true">!</span>
      <span>{message}</span>
    </div>
  );
}

export function PageHeader({ title, subtitle, action }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function Card({ title, action, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || action) && (
        <div className="card-head">
          {title && <h2>{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

export function StatCard({ icon, label, value, tone = 'indigo' }) {
  return (
    <div className={`stat tone-${tone}`}>
      <div className="stat-icon" aria-hidden="true">
        {icon}
      </div>
      <div>
        <div className="label">{label}</div>
        <div className="value">{value}</div>
      </div>
    </div>
  );
}

export function EmptyState({ icon = '○', title, text }) {
  return (
    <div className="empty">
      <div className="empty-icon" aria-hidden="true">
        {icon}
      </div>
      <strong>{title}</strong>
      {text && <span>{text}</span>}
    </div>
  );
}

export function TableSkeleton({ rows = 4 }) {
  return (
    <div aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="skeleton" style={{ width: `${100 - i * 8}%` }} />
      ))}
    </div>
  );
}

export function Badge({ value }) {
  if (!value) return <span className="muted">-</span>;
  return <span className={`badge badge-${String(value).toLowerCase()}`}>{String(value).replace(/_/g, ' ')}</span>;
}

const RIDE_STEPS = ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED'];

export function RideProgress({ status }) {
  if (status === 'CANCELLED') return <Badge value="CANCELLED" />;
  const index = status === 'DRIVER_ASSIGNED' ? 0 : RIDE_STEPS.indexOf(status);
  return (
    <div className="ride-progress" aria-label={`Ride status ${status}`}>
      {RIDE_STEPS.map((step, i) => (
        <span key={step} className={`ride-step ${i <= index ? 'done' : ''}`} title={step.replace(/_/g, ' ')} />
      ))}
    </div>
  );
}

export function Avatar({ name = '' }) {
  const initials = name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
  return <div className="avatar" aria-hidden="true">{initials || '?'}</div>;
}

export function formatDate(value) {
  return value ? new Date(value).toLocaleString() : '-';
}

export function formatMoney(value) {
  return value == null ? '-' : Number(value).toFixed(2);
}
