import { getUser, getRefreshToken, logout } from '../auth.js';
import { getCurrentPath } from '../router.js';
import { getResolvedTheme, toggleTheme } from '../theme.js';

const navItems = [
  { path: '/dashboard', label: 'Dashboard', icon: 'layout-dashboard' },
  { path: '/transactions', label: 'Transactions', icon: 'arrow-left-right' },
  { path: '/recurring', label: 'Recurring', icon: 'repeat' },
  { path: '/categories', label: 'Categories', icon: 'tag' },
  { path: '/budgets', label: 'Budgets', icon: 'wallet' },
  { path: '/reports', label: 'Reports', icon: 'bar-chart-3' },
  { path: '/profile', label: 'Profile', icon: 'user' },
];

function isCollapsed() {
  return localStorage.getItem('sidebar-collapsed') === 'true';
}

function applyState() {
  const container = document.getElementById('sidebar-container');
  const btn = document.getElementById('sidebar-toggle');
  if (!container || !btn) return;
  const collapsed = isCollapsed();
  container.classList.toggle('collapsed', collapsed);
  btn.classList.toggle('collapsed', collapsed);
}

export function renderSidebar() {
  const container = document.getElementById('sidebar-container');
  const user = getUser();

  container.innerHTML = `
    <aside class="flex w-full h-full min-w-[256px] flex-col bg-slate-900 text-white">
      <div class="flex h-16 items-center gap-3 px-5 border-b border-slate-700/50">
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-white text-sm font-bold">B</div>
        <span class="text-lg font-bold tracking-tight">Big Brother</span>
      </div>
      <nav class="flex-1 overflow-y-auto px-3 py-4 space-y-1" id="sidebar-nav">
        ${navItems.map(item => `
          <a class="sidebar-link" data-path="${item.path}" onclick="window.location.hash='#${item.path}'">
            <i data-lucide="${item.icon}" class="h-5 w-5"></i>
            <span>${item.label}</span>
          </a>
        `).join('')}
      </nav>
      <div class="border-t border-slate-700/50 p-4">
        <button id="sidebar-theme" class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-400 hover:bg-slate-800 hover:text-white transition-colors mb-1">
          <i data-lucide="moon" class="h-4 w-4"></i>
          <span class="theme-label">Dark mode</span>
        </button>
        <div class="flex items-center gap-3 mb-3">
          <div class="flex h-8 w-8 items-center justify-center rounded-full bg-brand-500/20 text-brand-300 text-sm font-semibold">
            ${user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-white truncate">${user?.name || 'User'}</p>
            <p class="text-xs text-slate-400 truncate">${user?.email || ''}</p>
          </div>
        </div>
        <button id="sidebar-logout" class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-400 hover:bg-slate-800 hover:text-white transition-colors">
          <i data-lucide="log-out" class="h-4 w-4"></i>
          <span>Log out</span>
        </button>
      </div>
    </aside>
  `;

  let toggleBtn = document.getElementById('sidebar-toggle');
  if (!toggleBtn) {
    toggleBtn = document.createElement('button');
    toggleBtn.id = 'sidebar-toggle';
    toggleBtn.innerHTML = '<i data-lucide="chevron-left" class="toggle-icon"></i>';
    document.getElementById('app').appendChild(toggleBtn);

    toggleBtn.addEventListener('click', () => {
      localStorage.setItem('sidebar-collapsed', isCollapsed() ? 'false' : 'true');
      applyState();
    });
  }

  lucide.createIcons();
  updateActiveLink();
  applyState();
  updateThemeButton();

  document.getElementById('sidebar-theme').addEventListener('click', () => {
    toggleTheme();
    updateThemeButton();
  });

  document.getElementById('sidebar-logout').addEventListener('click', async () => {
    try {
      const { api } = await import('../api.js');
      await api.post('/auth/logout', { refreshToken: getRefreshToken() });
    } catch {}
    logout();
  });
}

function updateThemeButton() {
  const dark = getResolvedTheme() === 'dark';
  const btn = document.getElementById('sidebar-theme');
  if (!btn) return;
  btn.innerHTML = `
    <i data-lucide="${dark ? 'sun' : 'moon'}" class="h-4 w-4"></i>
    <span class="theme-label">${dark ? 'Light mode' : 'Dark mode'}</span>
  `;
  lucide.createIcons();
}

export function updateActiveLink() {
  const path = getCurrentPath();
  document.querySelectorAll('.sidebar-link').forEach(link => {
    const linkPath = link.dataset.path;
    if (path === linkPath || (path === '/' && linkPath === '/dashboard')) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });
}
