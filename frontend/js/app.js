import { addRoute, initRouter, navigate } from './router.js';
import { isLoggedIn, logout, getUser, setUser } from './auth.js';
import { renderSidebar, updateActiveLink } from './components/sidebar.js';
import { api } from './api.js';
import { showToast } from './components/toast.js';

function showAppUI() {
  document.getElementById('app').classList.remove('hidden');
  document.getElementById('auth-container').classList.add('hidden');
  renderSidebar();
}

function showAuthUI() {
  document.getElementById('app').classList.add('hidden');
  document.getElementById('auth-container').classList.remove('hidden');
}

function authLayout(renderFn) {
  return async (params) => {
    showAuthUI();
    await renderFn(params);
  };
}

function appLayout(renderFn) {
  return async (params) => {
    showAppUI();
    updateActiveLink();
    const main = document.getElementById('app-content');
    await renderFn(main, params);
  };
}

async function loadDashboard(main) {
  const { renderDashboard } = await import('./pages/dashboard.js');
  await renderDashboard(main);
}

async function loadTransactions(main) {
  const { renderTransactions } = await import('./pages/transactions.js');
  await renderTransactions(main);
}

async function loadRecurring(main) {
  const { renderRecurring } = await import('./pages/recurring.js');
  await renderRecurring(main);
}

async function loadCategories(main) {
  const { renderCategories } = await import('./pages/categories.js');
  await renderCategories(main);
}

async function loadBudgets(main) {
  const { renderBudgets } = await import('./pages/budgets.js');
  await renderBudgets(main);
}

async function loadReports(main) {
  const { renderReports } = await import('./pages/reports.js');
  await renderReports(main);
}

async function loadProfile(main) {
  const { renderProfile } = await import('./pages/profile.js');
  await renderProfile(main);
}

addRoute('/login', authLayout(async (params) => {
  const { renderLogin } = await import('./pages/login.js');
  renderLogin();
}), false);

addRoute('/register', authLayout(async (params) => {
  const { renderRegister } = await import('./pages/register.js');
  renderRegister();
}), false);

addRoute('/verify', authLayout(async (params) => {
  const { renderVerify } = await import('./pages/verify.js');
  await renderVerify(params);
}), false);

addRoute('/resend-verification', authLayout(async (params) => {
  const { renderResendVerification } = await import('./pages/resend-verification.js');
  renderResendVerification();
}), false);

addRoute('/dashboard', appLayout(loadDashboard));
addRoute('/', appLayout(loadDashboard));
addRoute('/transactions', appLayout(loadTransactions));
addRoute('/recurring', appLayout(loadRecurring));
addRoute('/categories', appLayout(loadCategories));
addRoute('/budgets', appLayout(loadBudgets));
addRoute('/reports', appLayout(loadReports));
addRoute('/profile', appLayout(loadProfile));

if (isLoggedIn()) {
  api.get('/users/me').then(res => {
    if (res?.data) setUser(res.data);
  }).catch(() => {});
}

initRouter();
