import { useMemo, useState } from 'react';
import { useConfirm } from '../../shared/ConfirmDialog.jsx';
import Icon from '../../shared/Icon.jsx';
import { useToast } from '../../shared/ToastContext.jsx';
import { Avatar, Badge, Card, EmptyState, ErrorBanner, PageHeader, TableSkeleton, formatDate } from '../../shared/ui.jsx';
import { useData } from '../../shared/useData.js';
import { accountApi } from './api.js';

export default function UsersPage() {
  const users = useData(() => accountApi.listUsers());
  const toast = useToast();
  const { confirm, dialog } = useConfirm();
  const [query, setQuery] = useState('');

  async function run(action, { success, ask } = {}) {
    if (ask && !(await confirm(ask.message, ask))) return;
    try {
      await action();
      if (success) toast.success(success);
      users.reload();
    } catch (err) {
      toast.error(err.message);
    }
  }

  const rows = useMemo(() => {
    const list = users.data || [];
    const q = query.trim().toLowerCase();
    if (!q) return list;
    return list.filter((u) => `${u.fullName} ${u.email} ${u.role}`.toLowerCase().includes(q));
  }, [users.data, query]);

  return (
    <>
      <PageHeader title="Users" subtitle="Manage accounts, reactivate or deactivate access." />
      <ErrorBanner message={users.error} />

      <Card
        title={`All users${users.data ? ` (${users.data.length})` : ''}`}
        action={
          <label className="search">
            <Icon name="search" size={16} />
            <input placeholder="Search name, email or role" value={query} onChange={(e) => setQuery(e.target.value)} />
          </label>
        }
      >
        {users.loading ? (
          <TableSkeleton />
        ) : rows.length === 0 ? (
          <EmptyState icon="U" title="No users found" text={query ? 'Try a different search.' : 'Accounts will appear here once people sign up.'} />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Joined</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((u) => {
                  const status = u.status || (u.active ? 'ACTIVE' : 'DEACTIVATED');
                  return (
                    <tr key={u.id}>
                      <td>
                        <div className="person">
                          <Avatar name={u.fullName} />
                          <div>
                            <strong>{u.fullName}</strong>
                            <div className="muted">{u.email}</div>
                          </div>
                        </div>
                      </td>
                      <td>{u.role}</td>
                      <td>
                        <Badge value={status} />
                      </td>
                      <td>{formatDate(u.createdAt)}</td>
                      <td className="actions">
                        {status === 'ACTIVE' ? (
                          <button
                            className="danger"
                            onClick={() =>
                              run(() => accountApi.deactivateUser(u.id), {
                                success: `${u.fullName} has been deactivated`,
                                ask: { message: `${u.fullName} will lose access until reactivated.`, title: 'Deactivate account?', confirmLabel: 'Deactivate' },
                              })
                            }
                          >
                            Deactivate
                          </button>
                        ) : (
                          <button onClick={() => run(() => accountApi.activateUser(u.id), { success: `${u.fullName} is active again` })}>
                            Activate
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {dialog}
    </>
  );
}
