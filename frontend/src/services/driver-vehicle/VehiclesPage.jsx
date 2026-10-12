import { useState } from 'react';
import { useAuth } from '../../shared/AuthContext.jsx';
import { useConfirm } from '../../shared/ConfirmDialog.jsx';
import Icon from '../../shared/Icon.jsx';
import { useToast } from '../../shared/ToastContext.jsx';
import { Badge, Card, EmptyState, ErrorBanner, PageHeader, TableSkeleton } from '../../shared/ui.jsx';
import { useData } from '../../shared/useData.js';
import { driverVehicleApi } from './api.js';

const EMPTY_FORM = { driverId: '', make: '', model: '', year: '', plateNumber: '', color: '', capacity: '4' };

export default function VehiclesPage() {
  const { isAdmin } = useAuth();
  const vehicles = useData(() => driverVehicleApi.listVehicles());
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
      vehicles.reload();
    } catch (err) {
      toast.error(err.message);
    }
  }

  function handleCreate(e) {
    e.preventDefault();
    run(
      () =>
        driverVehicleApi.createVehicle({
          driverId: form.driverId,
          make: form.make,
          model: form.model,
          year: Number(form.year),
          plateNumber: form.plateNumber,
          color: form.color,
          capacity: Number(form.capacity),
        }),
      { reset: true, success: `Vehicle ${form.plateNumber} registered` },
    );
  }

  const driverLabel = (id) => {
    const d = (drivers.data || []).find((x) => x.id === id);
    return d ? d.licenseNumber : id;
  };

  return (
    <>
      <PageHeader
        title="Vehicles"
        subtitle={isAdmin ? 'Register vehicles and control which ones are active.' : 'Vehicles registered to drivers on the platform.'}
        action={
          <button onClick={() => setShowForm((v) => !v)} className="icon-button">
            <Icon name={showForm ? 'x' : 'plus'} size={16} />
            {showForm ? 'Close' : 'Register vehicle'}
          </button>
        }
      />
      <ErrorBanner message={vehicles.error} />

      {showForm && (
        <Card title="Register vehicle" className="fade-in">
          <form onSubmit={handleCreate} className="form-grid">
            <label>
              Driver
              <select required value={form.driverId} onChange={(e) => setForm({ ...form, driverId: e.target.value })}>
                <option value="">Select driver</option>
                {(drivers.data || []).map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.licenseNumber}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Make
              <input required value={form.make} onChange={(e) => setForm({ ...form, make: e.target.value })} />
            </label>
            <label>
              Model
              <input required value={form.model} onChange={(e) => setForm({ ...form, model: e.target.value })} />
            </label>
            <label>
              Year
              <input type="number" required value={form.year} onChange={(e) => setForm({ ...form, year: e.target.value })} />
            </label>
            <label>
              Plate number
              <input required value={form.plateNumber} onChange={(e) => setForm({ ...form, plateNumber: e.target.value })} />
            </label>
            <label>
              Color
              <input value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} />
            </label>
            <label>
              Capacity
              <input type="number" min="1" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} />
            </label>
            <button type="submit">Register vehicle</button>
          </form>
        </Card>
      )}

      <Card title={`All vehicles${vehicles.data ? ` (${vehicles.data.length})` : ''}`}>
        {vehicles.loading ? (
          <TableSkeleton />
        ) : !vehicles.data?.length ? (
          <EmptyState icon="V" title="No vehicles yet" text="Register a vehicle under a driver to see it here." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Plate</th>
                  <th>Vehicle</th>
                  <th>Driver</th>
                  <th>Capacity</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {vehicles.data.map((v) => (
                  <tr key={v.id}>
                    <td>
                      <strong>{v.plateNumber}</strong>
                    </td>
                    <td>
                      {v.make} {v.model} <span className="muted">({v.year})</span>
                    </td>
                    <td>{driverLabel(v.driverId)}</td>
                    <td>{v.capacity} seats</td>
                    <td>
                      <Badge value={v.active ? 'ACTIVE' : 'INACTIVE'} />
                    </td>
                    <td className="actions">
                      {isAdmin &&
                        (v.active ? (
                          <button
                            className="danger"
                            onClick={() =>
                              run(() => driverVehicleApi.deactivateVehicle(v.id), {
                                success: `${v.plateNumber} deactivated`,
                                ask: { message: `${v.plateNumber} will stop accepting trips.`, title: 'Deactivate vehicle?', confirmLabel: 'Deactivate' },
                              })
                            }
                          >
                            Deactivate
                          </button>
                        ) : (
                          <button onClick={() => run(() => driverVehicleApi.activateVehicle(v.id), { success: `${v.plateNumber} activated` })}>
                            Activate
                          </button>
                        ))}
                      <button
                        className="secondary"
                        onClick={() =>
                          run(() => driverVehicleApi.deleteVehicle(v.id), {
                            success: `${v.plateNumber} removed`,
                            ask: { message: `This permanently removes ${v.plateNumber}.`, title: 'Delete vehicle?', confirmLabel: 'Delete' },
                          })
                        }
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {!isAdmin && <p className="muted">Only administrators can activate or deactivate vehicles.</p>}
      </Card>
      {dialog}
    </>
  );
}
