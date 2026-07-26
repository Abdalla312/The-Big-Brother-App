import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './auth.js';
import { showToast } from './components/toast.js';

const BASE = 'http://localhost:8080/api/v1';

async function request(method, url, body = null, isRetry = false) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getAccessToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const opts = { method, headers };
  if (body && method !== 'GET') {
    opts.body = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(`${BASE}${url}`, opts);
  } catch (err) {
    showToast('Network error. Please check your connection.', 'error');
    throw new Error('Network error');
  }

  if (res.status === 401 && !isRetry && getRefreshToken()) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return request(method, url, body, true);
    }
    clearTokens();
    window.location.hash = '#/login';
    showToast('Session expired. Please log in again.', 'error');
    throw new Error('Session expired');
  }

  let data;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }

  if (!res.ok) {
    const message = data?.message || data?.error || `Request failed (${res.status})`;
    const errors = data?.errors || [];
    const detail = errors.length ? errors.join(', ') : message;
    if (res.status !== 401) {
      showToast(detail, 'error');
    }
    const err = new Error(detail);
    err.status = res.status;
    err.data = data;
    throw err;
  }

  return data;
}

async function tryRefresh() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const json = await res.json();
    const d = json.data;
    if (d?.accessToken && d?.refreshToken) {
      setTokens(d.accessToken, d.refreshToken);
      return true;
    }
    return false;
  } catch {
    return false;
  }
}

export const api = {
  get: (url) => request('GET', url),
  post: (url, body) => request('POST', url, body),
  patch: (url, body) => request('PATCH', url, body),
  delete: (url, body) => request('DELETE', url, body),
};
