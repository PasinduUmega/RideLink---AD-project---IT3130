import { getSession } from './session.js';

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function send(baseUrl, method, path, body) {
  const headers = { Accept: 'application/json' };
  const token = getSession()?.token;
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  const res = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    const message =
      (data && typeof data === 'object' && data.message) ||
      (res.status === 403 ? 'Access denied for this action' : `Request failed (${res.status})`);
    throw new ApiError(res.status, message);
  }
  return data;
}

export function createClient(baseUrl) {
  return {
    get: (path) => send(baseUrl, 'GET', path),
    post: (path, body) => send(baseUrl, 'POST', path, body),
    put: (path, body) => send(baseUrl, 'PUT', path, body),
    patch: (path, body) => send(baseUrl, 'PATCH', path, body),
    del: (path) => send(baseUrl, 'DELETE', path),
  };
}
