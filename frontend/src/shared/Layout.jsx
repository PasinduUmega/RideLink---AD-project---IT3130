import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';
import Icon from './Icon.jsx';
import { Avatar } from './ui.jsx';

const ROLE_LABEL = { ADMIN: 'Administrator', DRIVER: 'Driver', RIDER: 'Rider' };

const LINKS = [
  { to: '/fares', label: 'Fares', icon: 'ticket' },
  { to: '/payments', label: 'Payments', icon: 'card' },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div>
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            <Icon name="bolt" size={18} />
          </span>
          RideLink
        </div>
        <nav aria-label="Main">
          {LINKS.map((link) => (
            <NavLink key={link.to} to={link.to}>
              <Icon name={link.icon} size={16} />
              <span>{link.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="user">
          <Avatar name={user?.fullName} />
          <div className="user-meta">
            <strong>{user?.fullName}</strong>
            <span>{ROLE_LABEL[user?.role] || user?.role}</span>
          </div>
          <button className="secondary icon-button" onClick={handleLogout} title="Log out">
            <Icon name="logout" size={16} />
            <span>Log out</span>
          </button>
        </div>
      </header>
      <main key={location.pathname}>
        <Outlet />
      </main>
    </div>
  );
}
