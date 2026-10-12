import { useState } from 'react';
import { useConfirm } from '../../shared/ConfirmDialog.jsx';
import Icon from '../../shared/Icon.jsx';
import { useToast } from '../../shared/ToastContext.jsx';
import { Badge, Card, EmptyState, ErrorBanner, PageHeader, TableSkeleton } from '../../shared/ui.jsx';
import { useData } from '../../shared/useData.js';
import { driverVehicleApi } from './api.js';

const DRIVER_STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'];
const RISKY_STATUSES = ['REJECTED', 'SUSPENDED'];
const EMPTY_FORM = { userId: '', licenseNumber: '', licenseExpiry: '' };

export default function DriversPage() {
  const drivers = useData(() => driverVehicleApi.listDrivers());
  const toast = useToast();
  const { confirm, dialog } = useConfirm();
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
      drivers.reload();
    } catch (err) {
      toast.error(err.message);
    }
  }

  function handleCreate(e) {
    e.preventDefault();
    run(
      () =>
        driverVehicleApi.createDriver({
          userId: form.userId,
          licenseNumber: form.licenseNumber,
          licenseExpiry: form.licenseExpiry || undefined,
        }),
      { reset: true, success: 'Driver profile created and pending review' },
    );
  }

  return (
    <>
      <PageHeader
        title="Drivers"
        subtitle="Review driver applications and keep the driver pool healthy."
        action={
          <button onClick={() => setShowForm((v) => !v)} className="icon-button">
            <Icon name={showForm ? 'x' : 'plus'} size={16} />
            {showForm ? 'Close' : 'Add driver'}
          </button>
        }
      />
      <ErrorBanner message={drivers.error} />

      {showForm && (
        <Card title="Register driver profile" className="fade-in">
          <form onSubmit={handleCreate} className="form-grid">
            <label>
              Account user ID
              <input required value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })} />
            </label>
            <label>
              License number
              <input required value={form.licenseNumber} onChange={(e) => setForm({ ...form, licenseNumber: e.target.value })} />
            </label>
            <label>
              License expiry
              <input type="date" value={form.licenseExpiry} onChange={(e) => setForm({ ...form, licenseExpiry: e.target.value })} />
            </label>
            <button type="submit">Create driver</button>
          </form>
        </Card>
      )}

      <Card title={`All drivers${drivers.data ? ` (${drivers.data.length})` : ''}`}>
        {drivers.loading ? (
          <TableSkeleton />
        ) : !drivers.data?.length ? (
          <EmptyState icon="D" title="No drivers yet" text="Register a driver profile to get started." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>License</th>
                  <th>User ID</th>
                  <th>Status</th>
                  <th>Rating</th>
                  <th>Vehicles</th>
                  <th>Change status</th>
                </tr>
              </thead>
              <tbody>
                {drivers.data.map((d) => (
                  <tr key={d.id}>
                    <td>
                      <strong>{d.licenseNumber}</strong>
                    </td>
                    <td className="mono">{d.userId}</td>
                    <td>
                      <Badge value={d.status} />
                    </td>
                    <td>{d.rating != null ? `★ ${d.rating}` : '-'}</td>
                    <td>{d.vehicles?.length ?? 0}</td>
                    <td className="actions">
                      {DRIVER_STATUSES.filter((s) => s !== d.status).map((s) => (
                        <button
                          key={s}
                          className="secondary"
                          onClick={() =>
                            run(() => driverVehicleApi.setDriverStatus(d.id, s), {
                              success: `${d.licenseNumber} is now ${s.toLowerCase()}`,
                              ask: RISKY_STATUSES.includes(s)
                                ? { message: `${d.licenseNumber} will be marked ${s.toLowerCase()}.`, title: `Mark as ${s.toLowerCase()}?`, confirmLabel: 'Confirm' }
                                : undefined,
                            })
                          }
                        >
                          {s.charAt(0) + s.slice(1).toLowerCase()}
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
