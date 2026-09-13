import http from 'k6/http';
import { check, group, sleep, fail } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'https://staging.big-bro.app';
const TOKENS_ENV = (__ENV.TOKENS || '').trim();

const NUM_USERS = intOrDefault('NUM_USERS', 5);
const USER_EMAIL_PREFIX = __ENV.USER_EMAIL_PREFIX || 'loadtest';
const USER_EMAIL_DOMAIN = __ENV.USER_EMAIL_DOMAIN || 'bigbrother.test';
const USER_NAME = __ENV.USER_NAME || 'Load Test User';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'K6-Str0ng!Pass';
const REGISTER = (__ENV.REGISTER || '1').trim() === '1';

const VERIFY_TIMEOUT_SECONDS = parseSeconds(__ENV.VERIFY_TIMEOUT || '15m');
const VERIFY_ROUND_SLEEP_SECONDS = intOrDefault('VERIFY_ROUND_SLEEP', 30);
const REGISTER_SPACING_MS = intOrDefault('REGISTER_SPACING_MS', 2100);
const PROBE_SPACING_MS = intOrDefault('PROBE_SPACING_MS', 1200);
const LOGIN_SPACING_MS = intOrDefault('LOGIN_SPACING_MS', 3200);

const START_VUS = intOrDefault('START_VUS', 5);
const TARGET_VUS = intOrDefault('VUS', 30);
const RAMP_UP = __ENV.RAMP_UP || '30s';
const RAMP_DOWN = __ENV.RAMP_DOWN || '30s';
const DURATION = __ENV.DURATION || '3m';
const P95_LIMIT = __ENV.P95_LIMIT || 500;
const P99_LIMIT = __ENV.P99_LIMIT || 1000;
const THINK_MIN = floatOrDefault('THINK_MIN', 0.2);
const THINK_MAX = floatOrDefault('THINK_MAX', 0.8);

const transactionCreateDuration = new Trend('transaction_create_duration', true);
const writeDuration = new Trend('write_duration', true);
const rateLimited = new Rate('auth_rate_limited');
const authStatus = new Counter('auth_requests');

const headers = (token) => ({
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json',
});

const jsonHeaders = { 'Content-Type': 'application/json' };
const json = (body) => JSON.stringify(body);

const noFailedResponses = { setResponseCallback: () => false };

const today = new Date();
const pad = (n) => String(n).padStart(2, '0');
const currentMonth = `${today.getFullYear()}-${pad(today.getMonth() + 1)}`;
const monthStart = `${currentMonth}-01`;
const todayIso = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 1);
const nextMonthStart = `${nextMonth.getFullYear()}-${pad(nextMonth.getMonth() + 1)}-01`;

const monthShifted = (offsetMonths) => {
  const d = new Date(today.getFullYear(), today.getMonth() + offsetMonths, 1);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
};

const budgetMonth = () => monthShifted(state().budgetMonthOffset);

function findBudgetIdForCategory(token, month, categoryId) {
  const res = http.get(
    `${BASE_URL}/api/v1/budget?month=${month}`,
    { headers: headers(token), tags: { name: 'budget' } },
  );
  const content = res.json()?.data?.content || [];
  return content.find((b) => b.categoryId === categoryId)?.id || null;
}

const categoriesUrl = `${BASE_URL}/api/v1/categories?type=EXPENSE&page=0&size=20`;

const stateByVu = {};
const tokenByVu = {};

const vuId = () => exec.vu.idInTest;
const globIteration = () => exec.scenario.iterationInTest;
const state = () => {
  if (!stateByVu[vuId()]) {
    stateByVu[vuId()] = {
      categoryId: null,
      transactionId: null,
      budgetId: null,
      recurringId: null,
      customCategoryId: null,
      budgetMonthOffset: vuId() - 1,
    };
  }
  return stateByVu[vuId()];
};

export const options = {
  setupTimeout: '30m',
  scenarios: {
    authenticated: {
      executor: 'ramping-vus',
      exec: 'authenticatedFlow',
      startVUs: START_VUS,
      stages: [
        { duration: RAMP_UP, target: TARGET_VUS },
        { duration: DURATION, target: TARGET_VUS },
        { duration: RAMP_DOWN, target: 0 },
      ],
      gracefulRampDown: '30s',
      env: { SCENARIO: 'authenticated' },
    },
    auth_smoke: {
      executor: 'shared-iterations',
      exec: 'authFlow',
      vus: 1,
      iterations: intOrDefault('AUTH_ITERATIONS', 5),
      maxDuration: '2m',
      startTime: '5s',
      env: { SCENARIO: 'auth' },
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: [
      `p(95)<${P95_LIMIT}`,
      `p(99)<${P99_LIMIT}`,
    ],
    transaction_create_duration: [`p(95)<${P95_LIMIT}`],
    write_duration: [`p(95)<${P95_LIMIT}`],
    auth_rate_limited: ['rate<0.20'],
  },
};

export function setup() {
  const api = `${BASE_URL}/api/v1/auth`;

  const users = Array.from({ length: NUM_USERS }, (_, i) => ({
    email: `${USER_EMAIL_PREFIX}${i + 1}@${USER_EMAIL_DOMAIN}`,
    password: USER_PASSWORD,
  }));

  if (TOKENS_ENV) {
    console.log('TOKENS provided — skipping register/verify. Using', TOKENS_ENV.split(',').length, 'token(s).');
    return { tokens: TOKENS_ENV.split(',').map((t) => t.trim()).filter(Boolean), users };
  }

  const verified = {};

  if (REGISTER) {
    for (const u of users) {
      const res = http.post(
        `${api}/register`,
        json({ name: USER_NAME, email: u.email, password: u.password }),
        { headers: jsonHeaders, tags: { name: 'register' }, ...noFailedResponses },
      );
      if (res.status === 201) {
        console.log(`Created ${u.email}`);
      } else if (res.status === 409) {
        console.log(`Already exists (409): ${u.email}`);
      } else if (res.status === 429) {
        console.warn(`Rate limited on register, sleeping 31s for ${u.email}`);
        sleep(31);
      } else {
        console.warn(`Unexpected register status ${res.status} for ${u.email}`);
      }
      sleep(REGISTER_SPACING_MS / 1000);
    }
  } else {
    console.log('REGISTER=0 — assuming users already exist.');
  }

  console.log('============================================================');
  console.log('MANUAL STEP REQUIRED — register/confirm these users:');
  users.forEach((u) => console.log(`  ${u.email}`));
  console.log('Verify them (click emailed link) or in psql:');
  console.log(`  UPDATE users SET user_verified = TRUE WHERE email LIKE '${USER_EMAIL_PREFIX}%@${USER_EMAIL_DOMAIN}';`);
  console.log(`Waiting until login succeeds for all (timeout ${VERIFY_TIMEOUT_SECONDS}s)...`);
  console.log('Tip: auth is rate-limited per IP (dev capacity 30/min). 429s are handled with backoff.');
  console.log('============================================================');

  const deadline = Date.now() + VERIFY_TIMEOUT_SECONDS * 1000;

  for (;;) {
    let pending = 0;
    for (const u of users) {
      if (verified[u.email]) continue;
      const result = probeLogin(u);
      if (result === 'ok') {
        verified[u.email] = true;
        console.log(`Verified: ${u.email}`);
      } else if (result === 'rate') {
        sleep(31);
      } else {
        pending += 1;
      }
      sleep(PROBE_SPACING_MS / 1000);
    }

    if (pending === 0) break;

    if (Date.now() > deadline) {
      const unverified = users.filter((u) => !verified[u.email]).map((u) => u.email).join(', ');
      fail(`Timed out after ${VERIFY_TIMEOUT_SECONDS}s waiting for verification of: ${unverified}`);
    }
    console.log(`Waiting for ${pending} more user(s) to be verified...`);
    sleep(VERIFY_ROUND_SLEEP_SECONDS);
  }

  console.log(`All ${users.length} users verified. Logging in to issue tokens...`);

  const tokens = [];
  for (const u of users) {
    for (let attempt = 0; ; attempt++) {
      const res = http.post(
        `${api}/login`,
        json({ email: u.email, password: u.password }),
        { headers: jsonHeaders, tags: { name: 'login' } },
      );
      if (res.status === 200) {
        tokens.push(res.json().data.accessToken);
        break;
      }
      if (res.status === 429 && attempt < 5) {
        sleep(31);
        continue;
      }
      throw new Error(`Login failed for ${u.email} (${res.status})`);
    }
    sleep(LOGIN_SPACING_MS / 1000);
  }

  console.log(`Tokens issued: ${tokens.length}/${users.length}. Starting load test.`);
  return { tokens, users, verified: true };
}

function probeLogin(user) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    json({ email: user.email, password: user.password }),
    { headers: jsonHeaders, tags: { name: 'login probe' }, ...noFailedResponses },
  );
  if (res.status === 200) return 'ok';
  if (res.status === 429) return 'rate';
  return 'unverified';
}

function verifyToken(data) {
  const tokens = (data && data.tokens) || [];

  if (tokens.length > 0) {
    return tokens[(vuId() - 1) % tokens.length];
  }

  const fallbackUsers = (data && data.users) || [];
  if (tokenByVu[vuId()]) {
    return tokenByVu[vuId()];
  }
  if (fallbackUsers.length === 0) {
    return null;
  }
  const user = fallbackUsers[(vuId() - 1) % fallbackUsers.length];
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    json({ email: user.email, password: user.password }),
    { headers: jsonHeaders, tags: { name: 'login' } },
  );
  check(res, {
    'fallback login is 200 (user must be email-verified)': (r) => r.status === 200,
  });
  const token = res.status === 200 ? res.json().data?.accessToken : null;
  tokenByVu[vuId()] = token;
  return token;
}

function getCategoryId(token) {
  if (state().categoryId) {
    return state().categoryId;
  }
  const res = http.get(
    categoriesUrl,
    { headers: headers(token), tags: { name: 'categories' } },
  );
  check(res, { 'categories list is 200': (r) => r.status === 200 });
  const first = res.json()?.data?.content?.[0];
  state().categoryId = first?.id || null;
  return state().categoryId;
}

export function authenticatedFlow(data) {
  const token = verifyToken(data);
  if (!token) {
    check(null, {
      'tokens ready — let setup finish (register + verify) or pass -e TOKENS=...': (v) => v !== null,
    });
    return;
  }

  group('overview', () => {
    const health = http.get(
      `${BASE_URL}/api/v1/health`,
      { tags: { name: 'health' } },
    );
    check(health, { 'health is 200': (r) => r.status === 200 });

    const me = http.get(
      `${BASE_URL}/api/v1/users/me`,
      { headers: headers(token), tags: { name: 'users/me' } },
    );
    check(me, {
      'users/me is 200': (r) => r.status === 200,
      'users/me has data': (r) => r.json().data !== undefined,
    });
  });

  getCategoryId(token);
  const op = iterationPointer();

  switch (op) {
    case 'transactions_list':
      http.get(
        `${BASE_URL}/api/v1/transactions?month=${currentMonth}&page=0&size=20`,
        { headers: headers(token), tags: { name: 'transactions' } },
      );
      break;

    case 'create_transaction':
      createTransaction(token);
      break;

    case 'update_transaction':
      if (state().transactionId) {
        write(token, 'update transaction', 'PATCH', `/api/v1/transactions/${state().transactionId}`, {
          amount: (Math.random() * 1000 + 1).toFixed(2),
          note: 'k6 load test transaction (updated)',
          paymentMethod: 'Cash',
        });
      }
      break;

    case 'delete_transaction':
      if (state().transactionId) {
        write(token, 'delete transaction', 'DELETE', `/api/v1/transactions/${state().transactionId}`, null);
        state().transactionId = null;
      }
      break;

    case 'create_budget':
      if (state().budgetId) {
        http.get(
          `${BASE_URL}/api/v1/budget?month=${budgetMonth()}`,
          { headers: headers(token), tags: { name: 'budget' } },
        );
      } else if (state().categoryId) {
        const month = budgetMonth();
        const res = http.post(
          `${BASE_URL}/api/v1/budget`,
          json({
            categoryId: state().categoryId,
            month,
            limitAmount: (Math.random() * 2000 + 100).toFixed(2),
          }),
          {
            headers: headers(token),
            tags: { name: 'budget create' },
            setResponseCallback: (r) => r.status !== 201 && r.status !== 409,
          },
        );
        writeDuration.add(res.timings.duration);
        if (res.status === 201) {
          state().budgetId = res.json().data?.id || null;
          check(res, { 'create budget is 201': (r) => r.status === 201 });
        } else if (res.status === 409) {
          check(res, { 'create budget 409 — reusing existing': (r) => r.status === 409 });
          state().budgetId = findBudgetIdForCategory(token, month, state().categoryId);
        }
      }
      break;

    case 'update_budget':
      if (state().budgetId) {
        write(token, 'update budget', 'PATCH', `/api/v1/budget/${state().budgetId}`, {
          limitAmount: (Math.random() * 2000 + 100).toFixed(2),
        });
      }
      break;

    case 'delete_budget':
      if (state().budgetId) {
        write(token, 'delete budget', 'DELETE', `/api/v1/budget/${state().budgetId}`, null);
        state().budgetId = null;
        state().budgetMonthOffset += 48;
      }
      break;

    case 'create_category':
      const catRes = write(token, 'create category', 'POST', '/api/v1/categories', {
        name: `k6-cat-${vuId()}-${globIteration()}-${Date.now()}`,
        type: 'EXPENSE',
        color: '#dc2626',
        icon: 'circle',
      });
      if (catRes.status === 201) state().customCategoryId = catRes.json().data?.id || null;
      break;

    case 'update_category':
      if (state().customCategoryId) {
        write(token, 'update category', 'PATCH', `/api/v1/categories/${state().customCategoryId}`, {
          color: '#16a34a',
          icon: 'tag',
        });
      }
      break;

    case 'create_recurring':
      if (state().categoryId) {
        const res = write(token, 'create recurring', 'POST', '/api/v1/recurring-transactions', {
          type: 'EXPENSE',
          amount: (Math.random() * 500 + 10).toFixed(2),
          categoryId: state().categoryId,
          frequency: 'MONTHLY',
          startDate: nextMonthStart,
          paymentMethod: 'Credit Card',
          note: 'k6 load test recurring',
        });
        if (res.status === 201) state().recurringId = res.json().data?.id || null;
      }
      break;

    case 'toggle_recurring':
      if (state().recurringId) {
        write(token, 'toggle recurring', 'PATCH', `/api/v1/recurring-transactions/${state().recurringId}/toggle`, null);
      }
      break;

    case 'update_profile':
      write(token, 'update profile', 'PATCH', '/api/v1/users/me', {
        name: USER_NAME,
      });
      break;

    case 'reports_summary':
      reportFlow(token, '/reports/summary', `?startDate=${monthStart}&endDate=${todayIso}`);
      break;

    case 'reports_category_breakdown':
      reportFlow(token, '/reports/category-breakdown', `?startDate=${monthStart}&endDate=${todayIso}&type=EXPENSE`);
      break;

    case 'budget_list':
      http.get(
        `${BASE_URL}/api/v1/budget?month=${currentMonth}`,
        { headers: headers(token), tags: { name: 'budget' } },
      );
      break;
  }

  sleep(THINK_MIN + Math.random() * (THINK_MAX - THINK_MIN));
}

function createTransaction(token) {
  if (!state().categoryId) return;
  const amount = (Math.random() * 1000 + 1).toFixed(2);
  const res = http.post(
    `${BASE_URL}/api/v1/transactions`,
    json({
      type: 'EXPENSE',
      amount,
      transactionDate: todayIso,
      note: 'k6 load test transaction',
      paymentMethod: 'Credit Card',
      categoryId: state().categoryId,
    }),
    { headers: headers(token), tags: { name: 'transaction create' } },
  );
  transactionCreateDuration.add(res.timings.duration);
  if (res.status === 201) {
    state().transactionId = res.json().data?.id || null;
  }
  check(res, {
    'create transaction is 201': (r) => r.status === 201,
    'create transaction has id': (r) => r.json().data?.id !== undefined,
  });
}

function write(token, label, method, path, body) {
  const params = { headers: headers(token), tags: { name: label } };
  let res;
  switch (method) {
    case 'POST':
      res = http.post(`${BASE_URL}${path}`, json(body), params);
      break;
    case 'PATCH':
      res = body ? http.patch(`${BASE_URL}${path}`, json(body), params) : http.patch(`${BASE_URL}${path}`, null, params);
      break;
    case 'DELETE':
      res = http.del(`${BASE_URL}${path}`, null, params);
      break;
    default:
      throw new Error(`unsupported method ${method}`);
  }
  writeDuration.add(res.timings.duration);
  check(res, { [`${label} succeeds (${method} ${path})`]: (r) => r.status >= 200 && r.status < 300 });
  return res;
}

function reportFlow(token, path, query) {
  const res = http.get(
    `${BASE_URL}/api/v1${path}${query}`,
    { headers: headers(token), tags: { name: 'reports' } },
  );
  check(res, {
    [`${path} is 200`]: (r) => r.status === 200,
    [`${path} has data`]: (r) => r.json().data !== undefined,
  });
}

const OPS = [
  'transactions_list',
  'transactions_list',
  'transactions_list',
  'create_transaction',
  'update_transaction',
  'delete_transaction',
  'create_budget',
  'update_budget',
  'delete_budget',
  'create_category',
  'update_category',
  'create_recurring',
  'toggle_recurring',
  'update_profile',
  'reports_summary',
  'reports_category_breakdown',
  'budget_list',
];

function iterationPointer() {
  return OPS[(vuId() * 7919 + globIteration() * 104729) % OPS.length];
}

export function authFlow(data) {
  const users = (data && data.users) || [];

  const verifiedUser = users[globIteration() % users.length];
  if (!verifiedUser) {
    console.warn('auth_smoke: no users available (setup skipped registration). ' +
      'Skipping login/refresh/logout flow.');
    return;
  }

  authStatus.add(1);

  const login = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    json({ email: verifiedUser.email, password: verifiedUser.password }),
    { headers: jsonHeaders, tags: { name: 'login' } },
  );
  if (login.status === 429) {
    rateLimited.add(true);
  } else {
    rateLimited.add(false);
    check(login, { 'login is 200': (r) => r.status === 200 });
  }
  const refreshToken = login.json()?.data?.refreshToken;

  if (refreshToken) {
    const refresh = http.post(
      `${BASE_URL}/api/v1/auth/refresh`,
      json({ refreshToken }),
      { headers: jsonHeaders, tags: { name: 'refresh' } },
    );
    if (refresh.status === 429) {
      rateLimited.add(true);
    } else {
      rateLimited.add(false);
      check(refresh, {
        'refresh is 200': (r) => r.status === 200,
        'refresh returns new tokens': (r) => r.json().data?.accessToken !== undefined,
      });
    }

    const logout = http.post(
      `${BASE_URL}/api/v1/auth/logout`,
      json({ refreshToken }),
      { headers: jsonHeaders, tags: { name: 'logout' } },
    );
    if (logout.status === 429) {
      rateLimited.add(true);
    } else {
      rateLimited.add(false);
      check(logout, { 'logout is 200': (r) => r.status === 200 });
    }
  }

  sleep(2);
}

function intOrDefault(name, fallback) {
  const raw = __ENV[name];
  const parsed = raw !== undefined ? parseInt(raw, 10) : NaN;
  return Number.isNaN(parsed) ? fallback : parsed;
}

function floatOrDefault(name, fallback) {
  const raw = __ENV[name];
  const parsed = raw !== undefined ? parseFloat(raw) : NaN;
  return Number.isNaN(parsed) ? fallback : parsed;
}

function parseSeconds(value) {
  const m = /^(\d+)(s|m|h|d)?$/.exec(String(value || '').trim());
  if (!m) return 900;
  const multipliers = { s: 1, m: 60, h: 3600, d: 86400 };
  return parseInt(m[1], 10) * (multipliers[m[2] || 's'] || 1);
}