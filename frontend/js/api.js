import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './auth.js';
import { showToast } from './components/toast.js';
import { API_URL } from './config.js';

const BASE = `${API_URL}/api/v1`;
const MAX_RETRIES = 2;
const INITIAL_RETRY_DELAY = 1000;

async function request(method, url, body = null, retryCount = 0) {
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

  if (res.status === 429) {
    const retryAfter = res.headers.get('Retry-After');
    const delay = retryAfter ? parseInt(retryAfter, 10) * 1000 : INITIAL_RETRY_DELAY * Math.pow(2, retryCount);
    
    if (retryCount < MAX_RETRIES) {
      showToast(`Too many requests. Retrying in ${Math.ceil(delay / 1000)}s...`, 'warning');
      await new Promise(r => setTimeout(r, delay));
      return request(method, url, body, retryCount + 1);
    }
    
    const message = 'Too many attempts. Please wait a minute before trying again.';
    showToast(message, 'error');
    const err = new Error(message);
    err.status = 429;
    throw err;
  }

  if (res.status === 401 && retryCount === 0 && getRefreshToken()) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return request(method, url, body, retryCount);
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
