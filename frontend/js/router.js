import { isLoggedIn } from './auth.js';

const routes = [];
let notFoundHandler = null;

export function addRoute(path, handler, requiresAuth = true) {
  routes.push({ path, handler, requiresAuth });
}

export function setNotFound(handler) {
  notFoundHandler = handler;
}

export function navigate(hash) {
  window.location.hash = hash;
}

export function initRouter() {
  window.addEventListener('hashchange', handleRoute);
  handleRoute();
}

async function handleRoute() {
  const hash = window.location.hash || '#/';
  const [path, query] = hash.slice(1).split('?');
  const params = new URLSearchParams(query);

  const matched = routes.find(r => r.path === path);

  if (!matched) {
    if (notFoundHandler) notFoundHandler(path);
    return;
  }

  if (matched.requiresAuth && !isLoggedIn()) {
    navigate('#/login');
    return;
  }

  if (!matched.requiresAuth && isLoggedIn() && (path === '/login' || path === '/register')) {
    navigate('#/dashboard');
    return;
  }

  await matched.handler(params);
}

export function getCurrentPath() {
  const hash = window.location.hash || '#/';
  return hash.slice(1).split('?')[0];
}
