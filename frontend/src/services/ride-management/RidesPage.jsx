import { useState } from 'react';
import { useAuth } from '../../shared/AuthContext.jsx';
import { useConfirm } from '../../shared/ConfirmDialog.jsx';
import Icon from '../../shared/Icon.jsx';
import { useToast } from '../../shared/ToastContext.jsx';
import { Badge, Card, EmptyState, ErrorBanner, PageHeader, RideProgress, TableSkeleton, formatDate, formatMoney } from '../../shared/ui.jsx';
import { useData } from '../../shared/useData.js';
import { rideApi } from './api.js';

const RIDE_STATUSES = ['REQUESTED', 'DRIVER_ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

const ACTIONS_BY_STATUS = {
  REQUESTED: [
    { label: 'Auto-assign driver', run: rideApi.autoAssign, success: 'Driver assigned automatically' },
    { label: 'Cancel', run: rideApi.cancel, variant: 'danger', success: 'Ride cancelled', ask: { title: 'Cancel this ride?', message: 'The ride request will be closed.', confirmLabel: 'Cancel ride' } },
  ],
  DRIVER_ASSIGNED: [
    { label: 'Accept', run: rideApi.accept, success: 'Ride accepted' },
    { label: 'Decline', run: rideApi.decline, variant: 'secondary', success: 'Assignment declined', ask: { title: 'Decline this assignment?', message: 'The ride goes back to the queue.', confirmLabel: 'Decline' } },
    { label: 'Cancel', run: rideApi.cancel, variant: 'danger', success: 'Ride cancelled', ask: { title: 'Cancel this ride?', message: 'The ride will be closed for everyone.', confirmLabel: 'Cancel ride' } },
  ],
  ACCEPTED: [
    { label: 'Start trip', run: rideApi.start, success: 'Trip started' },
    { label: 'Cancel', run: rideApi.cancel, variant: 'danger', success: 'Ride cancelled', ask: { title: 'Cancel this ride?', message: 'The ride will be closed for everyone.', confirmLabel: 'Cancel ride' } },
  ],
  IN_PROGRESS: [{ label: 'Complete', run: rideApi.complete, success: 'Ride completed. A fare can now be issued.' }],
};

const EMPTY_FORM = {
  pickupLocation: '',
  dropoffLocation: '',
  pickupLat: '',
  pickupLng: '',
  dropoffLat: '',
  dropoffLng: '',
  distanceKm: '',
};

export default function RidesPage() {
  const { user } = useAuth();
  const toast = useToast();
  const { confirm, dialog } = useConfirm();
  const [statusFilter, setStatusFilter] = useState('');
  const rides = useData(() => rideApi.listRides(statusFilter), [statusFilter]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [showForm, setShowForm] = useState(false);

  async function run(action, { success, reset, ask } = {}) {
    if (ask && !(await confirm(ask.message, ask))) return;
    try {
      await action();
      if (reset) {
        setForm(EMPTY_FORM);
        setShowForm(false);
      }
      if (success) toast.success(success);
      rides.reload();
    } catch (err) {
      toast.error(err.message);
    }
  }

  function handleCreate(e) {
    e.preventDefault();
    const optionalNumber = (value) => (value === '' ? undefined : Number(value));
    run(
      () =>
        rideApi.createRide({
          riderId: user.id,
          pickupLocation: form.pickupLocation,
          dropoffLocation: form.dropoffLocation,
          pickupLat: optionalNumber(form.pickupLat),
          pickupLng: optionalNumber(form.pickupLng),
          dropoffLat: optionalNumber(form.dropoffLat),
          dropoffLng: optionalNumber(form.dropoffLng),
          distanceKm: optionalNumber(form.distanceKm),
        }),
      { reset: true, success: 'Ride requested. Looking for a driver...' },
    );
  }

  const field = (name, label, props = {}) => (
    <label>
      {label}
      <input value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} {...props} />
    </label>
  );

  return (
    <>
      <PageHeader
        title="Rides"
        subtitle="Request rides and move them through each stage of the trip."
        action={
          <button onClick={() => setShowForm((v) => !v)} className="icon-button">
            <Icon name={showForm ? 'x' : 'plus'} size={16} />
            {showForm ? 'Close' : 'Request ride'}
          </button>
        }
      />
      <ErrorBanner message={rides.error} />

      {showForm && (
        <Card title="Request a ride" className="fade-in">
          <form onSubmit={handleCreate} className="form-grid">
            {field('pickupLocation', 'Pickup location', { required: true })}
            {field('dropoffLocation', 'Dropoff location', { required: true })}
            {field('pickupLat', 'Pickup latitude', { type: 'number', step: 'any' })}
            {field('pickupLng', 'Pickup longitude', { type: 'number', step: 'any' })}
            {field('dropoffLat', 'Dropoff latitude', { type: 'number', step: 'any' })}
            {field('dropoffLng', 'Dropoff longitude', { type: 'number', step: 'any' })}
            {field('distanceKm', 'Distance (km)', { type: 'number', step: 'any', min: '0' })}
            <button type="submit">Request ride</button>
          </form>
        </Card>
      )}

      <Card
        title="Ride list"
        action={
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} style={{ maxWidth: 200 }} aria-label="Filter by status">
            <option value="">All statuses</option>
            {RIDE_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        }
      >
        {rides.loading ? (
          <TableSkeleton />
        ) : !rides.data?.length ? (
          <EmptyState icon="R" title="No rides here" text={statusFilter ? 'No rides match this status.' : 'Request your first ride to get going.'} />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Route</th>
                  <th>Progress</th>
                  <th>Driver</th>
                  <th>Distance</th>
                  <th>Est. fare</th>
                  <th>Requested</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rides.data.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <strong>{r.pickupLocation}</strong>
                      <div className="muted">to {r.dropoffLocation}</div>
                    </td>
                    <td>
                      <RideProgress status={r.status} />
                      <div style={{ marginTop: 6 }}>
                        <Badge value={r.status} />
                      </div>
                    </td>
                    <td className="mono">{r.driverId || 'Unassigned'}</td>
                    <td>{r.distanceKm != null ? `${r.distanceKm} km` : '-'}</td>
                    <td className="price">{formatMoney(r.estimatedFare)}</td>
                    <td>{formatDate(r.requestedAt)}</td>
                    <td className="actions">
                      {(ACTIONS_BY_STATUS[r.status] || []).map((action) => (
                        <button
                          key={action.label}
                          className={action.variant}
                          onClick={() => run(() => action.run(r.id), { success: action.success, ask: action.ask })}
                        >
                          {action.label}
                        </button>
                      ))}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {dialog}
    </>
  );
}
