import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './shared/AuthContext.jsx';
import Layout from './shared/Layout.jsx';
import LoginPage from './services/account/LoginPage.jsx';
import RegisterPage from './services/account/RegisterPage.jsx';
import FaresPage from './services/fare-payment/FaresPage.jsx';
import PaymentsPage from './services/fare-payment/PaymentsPage.jsx';

function RequireAuth({ children }) {
  const { session } = useAuth();
  if (!session) return <Navigate to="/login" replace />;
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
        <Route index element={<Navigate to="/fares" replace />} />
        <Route path="/fares" element={<FaresPage />} />
        <Route path="/payments" element={<PaymentsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/fares" replace />} />
    </Routes>
  );
}
