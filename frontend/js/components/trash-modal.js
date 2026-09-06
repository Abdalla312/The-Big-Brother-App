import { openModal, closeModal } from './modal.js';
import { showToast } from './toast.js';
import { formatDate } from '../utils.js';
import { renderTable } from './table.js';

let currentTrashPage = 0;
let currentTrashConfig = null;
let allTrashItems = [];

export function openTrashModal(config) {
  currentTrashConfig = config;
  currentTrashPage = 0;
  allTrashItems = [];

  openModal(
    config.title,
    `
      <div id="trash-modal-content">
        <div id="trash-table-container"></div>
      </div>
    `,
    `
      <div class="flex items-center justify-between w-full">
        <span id="trash-count" class="text-sm text-gray-500"></span>
        <div class="flex items-center gap-2">
          <button class="btn btn-secondary btn-sm" id="trash-restore-all" disabled>
            Restore All
          </button>
          <button class="btn btn-secondary btn-sm" id="trash-close">Close</button>
        </div>
      </div>
    `
  );

  document.getElementById('trash-close').addEventListener('click', closeModal);
  document.getElementById('trash-restore-all').addEventListener('click', handleRestoreAll);

  loadTrashPage(0);
}

async function loadTrashPage(page) {
  const container = document.getElementById('trash-table-container');
  const restoreAllBtn = document.getElementById('trash-restore-all');
  const countEl = document.getElementById('trash-count');

  container.innerHTML = '<div class="flex justify-center py-8"><div class="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-600"></div></div>';
  restoreAllBtn.disabled = true;

  try {
    const res = await currentTrashConfig.fetchTrash(page);
    const data = res?.data || {};
    const items = data.content || [];
    const pagination = {
      page: data.page,
      size: data.size,
      totalPages: data.totalPages,
      totalElements: data.totalElements,
      first: data.first,
      last: data.last,
    };

    if (items.length === 0 && page === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="mb-3 text-gray-300">
            <path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
          <p class="text-sm font-medium text-gray-500">${currentTrashConfig.emptyMessage || 'No deleted items found'}</p>
        </div>
      `;
      countEl.textContent = '0 items';
      restoreAllBtn.disabled = true;
      return;
    }

    allTrashItems = page === 0 ? items : [...allTrashItems, ...items];
    countEl.textContent = `${pagination.totalElements} item${pagination.totalElements !== 1 ? 's' : ''}`;
    restoreAllBtn.disabled = pagination.totalElements === 0;

    const columns = currentTrashConfig.columns.map(col => ({
      ...col,
      render: (row) => {
        if (col.key === 'deletedAt') {
          return `<span class="text-sm text-gray-500">${formatDate(row.deletedAt)}</span>`;
        }
        return col.render(row);
      }
    }));

    renderTable(container, {
      columns,
      rows: items,
      pagination,
      onPageChange: (p) => loadTrashPage(p),
    });

    container.querySelectorAll('.restore-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.dataset.id;
        await handleRestoreItem(id);
      });
    });

  } catch (err) {
    container.innerHTML = `<p class="text-sm text-red-500 text-center py-8">Failed to load trash.</p>`;
    restoreAllBtn.disabled = true;
  }
}

async function handleRestoreItem(id) {
  try {
    await currentTrashConfig.restoreItem(id);
    showToast('Item restored', 'success');
    loadTrashPage(currentTrashPage);
    if (currentTrashConfig.onClose) currentTrashConfig.onClose();
  } catch (err) {
    showToast('Failed to restore item', 'error');
  }
}

async function handleRestoreAll() {
  const confirm = window.confirm(`Restore all ${allTrashItems.length} deleted items?`);
  if (!confirm) return;

  const btn = document.getElementById('trash-restore-all');
  btn.disabled = true;
  btn.textContent = 'Restoring...';

  let restored = 0;
  let failed = 0;

  for (const item of allTrashItems) {
    try {
      await currentTrashConfig.restoreItem(item.id);
      restored++;
    } catch {
      failed++;
    }
  }

  btn.disabled = false;
  btn.textContent = 'Restore All';

  if (restored > 0) {
    showToast(`${restored} item${restored !== 1 ? 's' : ''} restored${failed ? `, ${failed} failed` : ''}`, 'success');
    loadTrashPage(0);
    if (currentTrashConfig.onClose) currentTrashConfig.onClose();
  } else {
    showToast('No items restored', 'error');
  }
}