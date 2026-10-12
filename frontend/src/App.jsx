import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './shared/AuthContext.jsx';
import Layout from './shared/Layout.jsx';
import LoginPage from './services/account/LoginPage.jsx';
import RegisterPage from './services/account/RegisterPage.jsx';
import UsersPage from './services/account/UsersPage.jsx';
import DashboardPage from './services/dashboard/DashboardPage.jsx';
import DriversPage from './services/driver-vehicle/DriversPage.jsx';
import VehiclesPage from './services/driver-vehicle/VehiclesPage.jsx';
import RidesPage from './services/ride-management/RidesPage.jsx';
import FaresPage from './services/fare-payment/FaresPage.jsx';
import PaymentsPage from './services/fare-payment/PaymentsPage.jsx';

function RequireAuth({ adminOnly = false, children }) {
  const { session, isAdmin } = useAuth();
  if (!session) return <Navigate to="/login" replace />;
  if (adminOnly && !isAdmin) return <p className="alert error">Admin access required.</p>;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="/users" element={<RequireAuth adminOnly><UsersPage /></RequireAuth>} />
        <Route path="/drivers" element={<DriversPage />} />
        <Route path="/vehicles" element={<VehiclesPage />} />
        <Route path="/rides" element={<RidesPage />} />
        <Route path="/fares" element={<FaresPage />} />
        <Route path="/payments" element={<PaymentsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
