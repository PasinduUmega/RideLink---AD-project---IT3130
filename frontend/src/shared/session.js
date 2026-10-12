const KEY = 'ridelink.session';

export function getSession() {
  try {
    return JSON.parse(localStorage.getItem(KEY));
  } catch {
    return null;
  }
}

export function setSession(session) {
  try {
    localStorage.setItem(KEY, JSON.stringify(session));
  } catch {
    // Storage unavailable: the session lasts only until the page reloads.
  }
}

export function clearSession() {
  try {
    localStorage.removeItem(KEY);
  } catch {
    // Nothing stored to clear.
  }
}
