import { createContext, useContext, useState } from 'react';
import { clearSession, getSession, setSession } from './session.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSessionState] = useState(getSession);

  function login({ token, user }) {
    const next = { token, user };
    setSession(next);
    setSessionState(next);
  }

  function logout() {
    clearSession();
    setSessionState(null);
  }

  const value = {
    session,
    user: session?.user ?? null,
    isAdmin: session?.user?.role === 'ADMIN',
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
