import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearTokens,
} from './auth.js';

import { showToast } from './components/toast.js';
import { API_URL } from './config.js';

const BASE = `${API_URL}/api/v1`;

const MAX_RETRIES = 2;
const INITIAL_RETRY_DELAY = 1000;
const MAX_RETRY_DELAY = 10_000;

// Only automatically retry methods that are safe/idempotent.
const RETRYABLE_METHODS = new Set([
  'GET',
  'HEAD',
  'OPTIONS',
]);

// Prevent multiple requests from refreshing the token simultaneously.
let refreshPromise = null;

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = tryRefresh().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}

async function request(method, url, body = null) {
  method = method.toUpperCase();

  let retryCount = 0;
  let didRefresh = false;

  while (true) {
    const headers = {};

    // Don't set Content-Type for FormData.
    // The browser will set the correct multipart boundary.
    if (!(body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    const token = getAccessToken();

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const opts = {
      method,
      headers,
    };

    if (body !== null && body !== undefined && method !== 'GET') {
      opts.body =
        body instanceof FormData
          ? body
          : JSON.stringify(body);
    }

    let res;

    try {
      res = await fetch(`${BASE}${url}`, opts);
    } catch (error) {
      showToast(
        'Network error. Please check your connection.',
        'error'
      );

      const networkError = new Error('Network error', {
        cause: error,
      });

      networkError.status = 0;

      throw networkError;
    }

    /*
     * Access token expired
     */
    if (
      res.status === 401 &&
      !didRefresh &&
      getRefreshToken()
    ) {
      didRefresh = true;

      const refreshed = await refreshAccessToken();

      if (refreshed) {
        // getAccessToken() will now return the new token
        // and the original request will be sent again.
        continue;
      }

      clearTokens();

      window.location.hash = '#/login';

      const error = new Error('Session expired');
      error.status = 401;

      showToast(
        'Session expired. Please log in again.',
        'error'
      );

      throw error;
    }

    /*
     * Rate limited
     */
    if (
      res.status === 429 &&
      RETRYABLE_METHODS.has(method) &&
      retryCount < MAX_RETRIES
    ) {
      const retryAfter = res.headers.get('Retry-After');

      let delay;

      if (retryAfter && /^\d+$/.test(retryAfter)) {
        delay = Number(retryAfter) * 1000;
      } else {
        delay =
          INITIAL_RETRY_DELAY *
          Math.pow(2, retryCount);

        // Small random jitter to avoid synchronized retries.
        delay += Math.random() * 500;
      }

      delay = Math.min(delay, MAX_RETRY_DELAY);

      showToast(
        `Too many requests. Retrying in ${Math.ceil(delay / 1000)}s...`,
        'warning'
      );

      await new Promise(resolve =>
        setTimeout(resolve, delay)
      );

      retryCount++;
      continue;
    }

    /*
     * Parse response
     */
    let data = null;

    const text = await res.text();

    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = text;
      }
    }

    /*
     * HTTP error
     */
    if (!res.ok) {
      const message =
        data?.message ||
        data?.error ||
        `Request failed (${res.status})`;

      const errors = Array.isArray(data?.errors)
        ? data.errors
        : [];

      const detail =
        errors.length > 0
          ? errors.join(', ')
          : message;

      // Don't show another toast for 401 because
      // authentication handling already dealt with it.
      if (res.status !== 401) {
        showToast(detail, 'error');
      }

      const error = new Error(detail);

      error.status = res.status;
      error.data = data;

      throw error;
    }

    return data;
  }
}

/*
 * Refresh the access token.
 *
 * This intentionally uses fetch directly instead of request()
 * so that a failed refresh does not recursively trigger another
 * refresh attempt.
 */
async function tryRefresh() {
  const refreshToken = getRefreshToken();

  if (!refreshToken) {
    return false;
  }

  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        refreshToken,
      }),
    });

    if (!res.ok) {
      return false;
    }

    const json = await res.json();
    const data = json?.data;

    if (
      data?.accessToken &&
      data?.refreshToken
    ) {
      setTokens(
        data.accessToken,
        data.refreshToken
      );

      return true;
    }

    return false;
  } catch {
    return false;
  }
}

export const api = {
  get: url =>
    request('GET', url),

  post: (url, body) =>
    request('POST', url, body),

  patch: (url, body) =>
    request('PATCH', url, body),

  put: (url, body) =>
    request('PUT', url, body),

  delete: (url, body) =>
    request('DELETE', url, body),
};