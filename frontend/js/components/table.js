export function renderTable(container, { columns, rows, pagination, onPageChange }) {
  if (!rows || rows.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="mb-3 text-gray-300">
          <rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/>
          <line x1="9" y1="21" x2="9" y2="9"/>
        </svg>
        <p class="text-sm font-medium">No data found</p>
      </div>
    `;
    return;
  }

  let html = `
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            ${columns.map(col => `<th>${col.header}</th>`).join('')}
          </tr>
        </thead>
        <tbody>
          ${rows.map(row => `
            <tr>
              ${columns.map(col => `<td>${col.render(row)}</td>`).join('')}
            </tr>
          `).join('')}
        </tbody>
      </table>
    </div>
  `;

  if (pagination && pagination.totalPages > 1) {
    html += `
      <div class="flex items-center justify-between mt-4 px-1">
        <p class="text-sm text-gray-500">
          Showing ${pagination.page * pagination.size + 1} to ${Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of ${pagination.totalElements}
        </p>
        <div class="flex items-center gap-1">
          <button class="btn btn-secondary btn-sm" ${pagination.first ? 'disabled' : ''} data-page="${pagination.page - 1}">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          </button>
          ${generatePageButtons(pagination)}
          <button class="btn btn-secondary btn-sm" ${pagination.last ? 'disabled' : ''} data-page="${pagination.page + 1}">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
        </div>
      </div>
    `;
  }

  container.innerHTML = html;

  if (onPageChange) {
    container.querySelectorAll('[data-page]').forEach(btn => {
      btn.addEventListener('click', () => {
        const p = parseInt(btn.dataset.page);
        if (!isNaN(p)) onPageChange(p);
      });
    });
  }
}

function generatePageButtons(pagination) {
  const { page, totalPages } = pagination;
  const pages = [];
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages, start + 5);

  for (let i = start; i < end; i++) {
    pages.push(`
      <button class="btn btn-sm ${i === page ? 'bg-brand-600 text-white' : 'btn-secondary'}" data-page="${i}">
        ${i + 1}
      </button>
    `);
  }
  return pages.join('');
}
