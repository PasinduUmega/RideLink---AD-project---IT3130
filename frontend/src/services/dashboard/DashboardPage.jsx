import { Link } from 'react-router-dom';
import { accountApi } from '../account/api.js';
import { driverVehicleApi } from '../driver-vehicle/api.js';
import { farePaymentApi } from '../fare-payment/api.js';
import { rideApi } from '../ride-management/api.js';
import { useAuth } from '../../shared/AuthContext.jsx';
import Icon from '../../shared/Icon.jsx';
import { Badge, Card, EmptyState, PageHeader, RideProgress, StatCard, TableSkeleton, formatDate, formatMoney } from '../../shared/ui.jsx';
import { useData } from '../../shared/useData.js';

const QUICK_ACTIONS = [
  { to: '/rides', label: 'Request a ride', hint: 'Book a trip from A to B', icon: 'route', tone: 'indigo' },
  { to: '/fares', label: 'Estimate a fare', hint: 'See the price before you go', icon: 'ticket', tone: 'sky' },
  { to: '/payments', label: 'Pay a fare', hint: 'Settle a completed ride', icon: 'card', tone: 'emerald' },
  { to: '/vehicles', label: 'Register a vehicle', hint: 'Add a car to a driver', icon: 'car', tone: 'amber' },
  { to: '/drivers', label: 'Driver pool', hint: 'Approve and manage drivers', icon: 'driver', tone: 'rose' },
];

const settled = (result) => (result.status === 'fulfilled' ? result.value : null);

export default function DashboardPage() {
  const { user, isAdmin } = useAuth();
  const snapshot = useData(async () => {
    const [users, drivers, vehicles, rides, payments] = await Promise.allSettled([
      accountApi.listUsers(),
      driverVehicleApi.listDrivers(),
      driverVehicleApi.listVehicles(),
      rideApi.listRides(),
      farePaymentApi.listPayments(),
    ]);
    return {
      users: settled(users),
      drivers: settled(drivers),
      vehicles: settled(vehicles),
      rides: settled(rides) || [],
      payments: settled(payments) || [],
    };
  }, []);

  const firstName = user?.fullName?.split(' ')[0] || 'there';
  const greeting = new Date().getHours() < 12 ? 'Good morning' : new Date().getHours() < 18 ? 'Good afternoon' : 'Good evening';

  if (snapshot.loading) {
    return (
      <>
        <PageHeader title="Home" />
        <TableSkeleton rows={6} />
      </>
    );
  }

  const d = snapshot.data;
  const rides = d.rides;
  const payments = d.payments;
  const count = (list) => (list ? list.length : '-');
  const inProgress = rides.filter((r) => r.status === 'IN_PROGRESS' || r.status === 'ACCEPTED').length;
  const completed = rides.filter((r) => r.status === 'COMPLETED').length;
  const collected = payments.filter((p) => p.status === 'COMPLETED').reduce((sum, p) => sum + Number(p.amount || 0), 0);
  const pendingDrivers = (d.drivers || []).filter((x) => x.status === 'PENDING').length;
  const recent = [...rides].sort((a, b) => new Date(b.requestedAt) - new Date(a.requestedAt)).slice(0, 5);

  return (
    <>
      <section className="hero">
        <div>
          <p className="hero-kicker">{greeting}</p>
          <h1>Welcome back, {firstName}</h1>
          <p>
            {isAdmin
              ? `${pendingDrivers} driver application${pendingDrivers === 1 ? '' : 's'} waiting for review.`
              : 'Here is what is happening across RideLink today.'}
          </p>
        </div>
        <Link to="/rides" className="hero-cta">
          <Icon name="plus" size={16} /> Request a ride
        </Link>
      </section>

      <div className="stat-row">
        {isAdmin && <StatCard icon={<Icon name="users" />} label="Users" value={count(d.users)} tone="indigo" />}
        <StatCard icon={<Icon name="driver" />} label="Drivers" value={count(d.drivers)} tone="rose" />
        <StatCard icon={<Icon name="car" />} label="Vehicles" value={count(d.vehicles)} tone="amber" />
        <StatCard icon={<Icon name="route" />} label="Rides active" value={inProgress} tone="sky" />
        <StatCard icon={<Icon name="check" />} label="Rides completed" value={completed} tone="emerald" />
        <StatCard icon={<Icon name="card" />} label="Collected" value={formatMoney(collected)} tone="emerald" />
      </div>

      <h2 className="section-title">Quick actions</h2>
      <div className="quick-grid">
        {QUICK_ACTIONS.map((a) => (
          <Link key={a.to} to={a.to} className={`quick-tile tone-${a.tone}`}>
            <span className="quick-icon">
              <Icon name={a.icon} />
            </span>
            <span>
              <strong>{a.label}</strong>
              <span className="muted">{a.hint}</span>
            </span>
            <span className="quick-arrow" aria-hidden="true">
              <Icon name="arrow" size={16} />
            </span>
          </Link>
        ))}
      </div>

      <Card title="Recent rides" action={<Link to="/rides">View all</Link>}>
        {recent.length === 0 ? (
          <EmptyState icon="R" title="No rides yet" text="Request your first ride and it will show up here." />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Route</th>
                  <th>Progress</th>
                  <th>Status</th>
                  <th>Requested</th>
                </tr>
              </thead>
              <tbody>
                {recent.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <strong>{r.pickupLocation}</strong>
                      <div className="muted">to {r.dropoffLocation}</div>
                    </td>
                    <td>
                      <RideProgress status={r.status} />
                    </td>
                    <td>
                      <Badge value={r.status} />
                    </td>
                    <td>{formatDate(r.requestedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
