export function showLoading(container) {
  container.innerHTML = `
    <div class="flex items-center justify-center py-16">
      <div class="flex items-center gap-3 text-gray-400">
        <svg class="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
        <span class="text-sm font-medium">Loading...</span>
      </div>
    </div>
  `;
}

export function showEmpty(container, message = 'No data found', icon = '') {
  const defaultIcon = `<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="mb-3 text-gray-300">
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
    <polyline points="14 2 14 8 20 8"/>
    <line x1="12" y1="18" x2="12" y2="12"/>
    <line x1="9" y1="15" x2="15" y2="15"/>
  </svg>`;

  container.innerHTML = `
    <div class="empty-state">
      ${icon || defaultIcon}
      <p class="text-sm font-medium">${message}</p>
    </div>
  `;
}

export function showPageLoading() {
  const main = document.getElementById('app-content');
  showLoading(main);
}
