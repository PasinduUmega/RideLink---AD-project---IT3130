import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/AuthContext.jsx';
import { ErrorBanner } from '../../shared/ui.jsx';
import { accountApi } from './api.js';

const EMPTY_FORM = { fullName: '', email: '', password: '', phone: '', role: 'RIDER' };

export default function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  function update(field) {
    return (e) => setForm({ ...form, [field]: e.target.value });
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      login(await accountApi.register(form));
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
        <h2>Join as a rider or a driver.</h2>
        <p>Create your account in under a minute and start moving today.</p>
        <ul>
          <li>Free to sign up</li>
          <li>Drivers earn on their own schedule</li>
          <li>Riders get fair, upfront pricing</li>
        </ul>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <h1>Create your account</h1>
          <p className="muted">It only takes a moment.</p>
          <ErrorBanner message={error} />
          <form className="auth-form" onSubmit={handleSubmit}>
            <label>
              Full name
              <input required autoComplete="name" value={form.fullName} onChange={update('fullName')} />
            </label>
            <label>
              Email
              <input type="email" required autoComplete="email" value={form.email} onChange={update('email')} />
            </label>
            <label>
              Password (min 8 characters)
              <input type="password" minLength={8} required autoComplete="new-password" value={form.password} onChange={update('password')} />
            </label>
            <label>
              Phone
              <input type="tel" autoComplete="tel" value={form.phone} onChange={update('phone')} />
            </label>
            <label>
              I want to
              <select value={form.role} onChange={update('role')}>
                <option value="RIDER">Ride with RideLink</option>
                <option value="DRIVER">Drive with RideLink</option>
              </select>
            </label>
            <button type="submit" disabled={busy}>
              {busy && <span className="spinner" aria-hidden="true" />}
              {busy ? 'Creating account...' : 'Create account'}
            </button>
          </form>
          <p className="auth-footer">
            Already have an account? <Link to="/login">Sign in</Link>
          </p>
        </div>
      </section>
    </div>
  );
}
