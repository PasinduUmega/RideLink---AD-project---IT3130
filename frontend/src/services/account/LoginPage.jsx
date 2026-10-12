import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/AuthContext.jsx';
import { ErrorBanner } from '../../shared/ui.jsx';
import { accountApi } from './api.js';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      login(await accountApi.login(form));
      navigate('/rides');
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <div className="auth-shell">
      <section className="auth-hero">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">R</span>
          RideLink
        </div>
        <h2>Move around the city, your way.</h2>
        <p>Book rides, track drivers and manage fares and payments, all in one place.</p>
        <ul>
          <li>Real-time driver matching</li>
          <li>Transparent fare estimates</li>
          <li>Secure payments and receipts</li>
        </ul>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h1>Welcome back</h1>
          <p className="muted">Sign in to continue to your dashboard.</p>
          <ErrorBanner message={error} />
          <form className="auth-form" onSubmit={handleSubmit}>
            <label>
              Email
              <input type="email" autoComplete="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </label>
            <label>
              Password
              <input type="password" autoComplete="current-password" required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </label>
            <button type="submit" disabled={busy}>
              {busy && <span className="spinner" aria-hidden="true" />}
              {busy ? 'Signing in...' : 'Sign in'}
            </button>
          </form>
          <p className="auth-footer">
            New to RideLink? <Link to="/register">Create an account</Link>
          </p>
        </div>
      </section>
    </div>
  );
}
