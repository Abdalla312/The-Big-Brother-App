const THEME_KEY = 'bb_theme';

export function getStoredTheme() {
  try {
    return localStorage.getItem(THEME_KEY);
  } catch {
    return null;
  }
}

export function getSystemTheme() {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

function isDefaultDark() {
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

export function getResolvedTheme() {
  const stored = getStoredTheme();
  if (stored === 'dark' || stored === 'light') return stored;
  return isDefaultDark() ? 'dark' : 'light';
}

export function applyTheme(theme) {
  const resolved =
    theme === 'dark' || theme === 'light' ? theme : getResolvedTheme();
  document.documentElement.setAttribute('data-theme', resolved);
}

export function setTheme(theme) {
  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch {}
  applyTheme(theme);
}

export function toggleTheme() {
  const current = getResolvedTheme();
  setTheme(current === 'dark' ? 'light' : 'dark');
  return current === 'dark' ? 'light' : 'dark';
}

export function initTheme() {
  applyTheme(getStoredTheme());
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
    if (!getStoredTheme()) applyTheme(e.matches ? 'dark' : 'light');
  });
}