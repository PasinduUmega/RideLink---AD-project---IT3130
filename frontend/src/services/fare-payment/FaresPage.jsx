import { useState } from 'react';
import { useAuth } from '../../shared/AuthContext.jsx';
import { useConfirm } from '../../shared/ConfirmDialog.jsx';
import { useToast } from '../../shared/ToastContext.jsx';
import { Badge, Card, EmptyState, ErrorBanner, PageHeader, TableSkeleton, formatMoney } from '../../shared/ui.jsx';
import { useData } from '../../shared/useData.js';
import { farePaymentApi } from './api.js';

const EMPTY_CREATE = { rideId: '', distanceKm: '' };
const EMPTY_ESTIMATE = { distanceKm: '', durationMinutes: '' };

export default function FaresPage() {
  const { user } = useAuth();
  const fares = useData(() => farePaymentApi.listFares());
  const toast = useToast();
  const { confirm, dialog } = useConfirm();
  const [createForm, setCreateForm] = useState(EMPTY_CREATE);
  const [estimateForm, setEstimateForm] = useState(EMPTY_ESTIMATE);
  const [estimate, setEstimate] = useState(null);
  const [estimating, setEstimating] = useState(false);

  async function run(action, { success, ask } = {}) {
    if (ask && !(await confirm(ask.message, ask))) return;
    try {
      await action();
      if (success) toast.success(success);
      fares.reload();
    } catch (err) {
      toast.error(err.message);
    }
  }

  async function handleEstimate(e) {
    e.preventDefault();
    setEstimating(true);
    try {
      setEstimate(
        await farePaymentApi.estimate({
          distanceKm: Number(estimateForm.distanceKm),
          durationMinutes: estimateForm.durationMinutes === '' ? undefined : Number(estimateForm.durationMinutes),
        }),
      );
    } catch (err) {
      toast.error(err.message);
    } finally {
      setEstimating(false);
    }
  }

  function handleCreate(e) {
    e.preventDefault();
    run(
      async () => {
        await farePaymentApi.createFare({
          rideId: createForm.rideId,
          riderId: user.id,
          distanceKm: Number(createForm.distanceKm),
        });
        setCreateForm(EMPTY_CREATE);
      },
      { success: 'Fare issued for the ride' },
    );
  }

  return (
    <>
      <PageHeader title="Fares" subtitle="Get price estimates and issue fares for completed rides." />
      <ErrorBanner message={fares.error} />

      <div className="stat-row">
        <div className="stat tone-indigo">
          <div className="stat-icon" aria-hidden="true">#</div>
          <div>
            <div className="label">Fares issued</div>
            <div className="value">{fares.data?.length ?? '-'}</div>
          </div>
        </div>
        <div className="stat tone-sky">
          <div className="stat-icon" aria-hidden="true">~</div>
          <div>
            <div className="label">Last estimate</div>
            <div className="value">{estimate ? formatMoney(estimate.totalFare) : '-'}</div>
          </div>
        </div>
      </div>

      <div className="grid-2">
        <Card title="Fare estimate">
          <form onSubmit={handleEstimate} className="form-grid">
            <label>
              Distance (km)
              <input type="number" step="any" min="0" required value={estimateForm.distanceKm} onChange={(e) => setEstimateForm({ ...estimateForm, distanceKm: e.target.value })} />
            </label>
            <label>
              Duration (min, optional)
              <input type="number" min="0" value={estimateForm.durationMinutes} onChange={(e) => setEstimateForm({ ...estimateForm, durationMinutes: e.target.value })} />
            </label>
            <button type="submit" disabled={estimating}>
              {estimating ? 'Calculating...' : 'Get estimate'}
            </button>
          </form>
          {estimate && (
            <div className="estimate-result fade-in">
              <span className="muted">Estimated total</span>
              <strong>
                {formatMoney(estimate.totalFare)} {estimate.currency || ''}
              </strong>
            </div>
          )}
        </Card>

        <Card title="Issue a fare">
          <form onSubmit={handleCreate} className="form-grid">
            <label>
              Ride ID
              <input required value={createForm.rideId} onChange={(e) => setCreateForm({ ...createForm, rideId: e.target.value })} />
            </label>
            <label>
              Distance (km)
              <input type="number" step="any" min="0" required value={createForm.distanceKm} onChange={(e) => setCreateForm({ ...createForm, distanceKm: e.target.value })} />
            </label>
            <button type="submit">Create fare</button>
          </form>
        </Card>
      </div>

      <Card title={`All fares${fares.data ? ` (${fares.data.length})` : ''}`}>
        {fares.loading ? (
          <TableSkeleton />
        ) : !fares.data?.length ? (
          <EmptyState icon="F" title="No fares yet" text="Create a fare for a ride or get an estimate above." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Fare ID</th>
                  <th>Ride ID</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {fares.data.map((f) => (
                  <tr key={f.id}>
                    <td className="mono">{f.id}</td>
                    <td className="mono">{f.rideId}</td>
                    <td className="price">{formatMoney(f.totalFare)}</td>
                    <td>
                      <Badge value={f.status} />
                    </td>
                    <td className="actions">
                      {f.status !== 'VOIDED' && (
                        <button
                          className="danger"
                          onClick={() =>
                            run(() => farePaymentApi.voidFare(f.id), {
                              success: 'Fare voided',
                              ask: { title: 'Void this fare?', message: 'It can no longer be paid.', confirmLabel: 'Void fare' },
                            })
                          }
                        >
                          Void
                        </button>
                      )}
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
