import { useState } from 'react';
import { useAuth } from '../../shared/AuthContext.jsx';
import { useConfirm } from '../../shared/ConfirmDialog.jsx';
import { useToast } from '../../shared/ToastContext.jsx';
import { Badge, Card, EmptyState, ErrorBanner, PageHeader, TableSkeleton, formatDate, formatMoney } from '../../shared/ui.jsx';
import { useData } from '../../shared/useData.js';
import { farePaymentApi } from './api.js';

const EMPTY_FORM = { fareId: '', method: 'CARD' };
const METHODS = ['CARD'];

export default function PaymentsPage() {
  const { user } = useAuth();
  const payments = useData(() => farePaymentApi.listPayments());
  const fares = useData(() => farePaymentApi.listFares());
  const toast = useToast();
  const { confirm, dialog } = useConfirm();
  const [form, setForm] = useState(EMPTY_FORM);

  async function run(action, { success, reset, ask } = {}) {
    if (ask && !(await confirm(ask.message, ask))) return;
    try {
      await action();
      if (reset) setForm(EMPTY_FORM);
      if (success) toast.success(success);
      payments.reload();
      fares.reload();
    } catch (err) {
      toast.error(err.message);
    }
  }

  function handlePay(e) {
    e.preventDefault();
    run(() => farePaymentApi.pay({ fareId: form.fareId, riderId: user.id, method: form.method }), {
      reset: true,
      success: 'Payment successful. Thank you!',
    });
  }

  const payableFares = (fares.data || []).filter((f) => f.status !== 'VOIDED');
  const completed = (payments.data || []).filter((p) => p.status === 'COMPLETED');
  const collected = completed.reduce((sum, p) => sum + Number(p.amount || 0), 0);

  return (
    <>
      <PageHeader title="Payments" subtitle="Pay fares, follow payment status and handle refunds." />
      <ErrorBanner message={payments.error || fares.error} />

      <div className="stat-row">
        <div className="stat tone-indigo">
          <div className="stat-icon" aria-hidden="true">#</div>
          <div>
            <div className="label">Payments</div>
            <div className="value">{payments.data?.length ?? '-'}</div>
          </div>
        </div>
        <div className="stat tone-emerald">
          <div className="stat-icon" aria-hidden="true">✓</div>
          <div>
            <div className="label">Completed</div>
            <div className="value">{completed.length}</div>
          </div>
        </div>
        <div className="stat tone-amber">
          <div className="stat-icon" aria-hidden="true">$</div>
          <div>
            <div className="label">Collected</div>
            <div className="value">{formatMoney(collected)}</div>
          </div>
        </div>
      </div>

      <Card title="Pay a fare">
        <form onSubmit={handlePay} className="form-grid">
          <label>
            Fare
            <select required value={form.fareId} onChange={(e) => setForm({ ...form, fareId: e.target.value })}>
              <option value="">Select a fare</option>
              {payableFares.map((f) => (
                <option key={f.id} value={f.id}>
                  {formatMoney(f.totalFare)} for ride {f.rideId}
                </option>
              ))}
            </select>
          </label>
          <label>
            Method
            <select value={form.method} onChange={(e) => setForm({ ...form, method: e.target.value })}>
              {METHODS.map((m) => (
                <option key={m} value={m}>
                  {m.charAt(0) + m.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" disabled={!form.fareId}>
            Pay now
          </button>
        </form>
      </Card>

      <Card title={`Payment history${payments.data ? ` (${payments.data.length})` : ''}`}>
        {payments.loading ? (
          <TableSkeleton />
        ) : !payments.data?.length ? (
          <EmptyState icon="P" title="No payments yet" text="Payments you make will show up here." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Payment</th>
                  <th>Amount</th>
                  <th>Method</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {payments.data.map((p) => (
                  <tr key={p.id}>
                    <td className="mono">{p.id}</td>
                    <td className="price">{formatMoney(p.amount)}</td>
                    <td>{p.method}</td>
                    <td>
                      <Badge value={p.status} />
                    </td>
                    <td>{formatDate(p.createdAt)}</td>
                    <td className="actions">
                      {p.status === 'COMPLETED' && (
                        <button
                          className="secondary"
                          onClick={() =>
                            run(() => farePaymentApi.refund(p.id), {
                              success: 'Refund issued',
                              ask: { title: 'Refund this payment?', message: 'The amount will be returned to the rider.', confirmLabel: 'Refund' },
                            })
                          }
                        >
                          Refund
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
