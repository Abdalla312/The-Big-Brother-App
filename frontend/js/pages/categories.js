import { api } from '../api.js';
import { showLoading, showEmpty } from '../components/loading.js';
import { openModal, closeModal, confirmDialog } from '../components/modal.js';
import { showToast } from '../components/toast.js';
import { openTrashModal } from '../components/trash-modal.js';
import { formatDate } from '../utils.js';

let activeTab = 'all';

export async function renderCategories(main) {
  showLoading(main);

  main.innerHTML = `
    <div class="page-header">
      <h1 class="page-title">Categories</h1>
      <div class="flex items-center gap-2">
        <button class="btn btn-secondary" id="trash-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          Trash
        </button>
        <button class="btn btn-primary" id="add-category-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Add Category
        </button>
      </div>
    </div>

    <div class="flex items-center gap-2 mb-4">
      <button class="btn btn-sm cat-tab ${activeTab === 'all' ? 'bg-brand-600 text-white' : 'btn-secondary'}" data-tab="all">All</button>
      <button class="btn btn-sm cat-tab ${activeTab === 'EXPENSE' ? 'bg-brand-600 text-white' : 'btn-secondary'}" data-tab="EXPENSE">Expense</button>
      <button class="btn btn-sm cat-tab ${activeTab === 'INCOME' ? 'bg-brand-600 text-white' : 'btn-secondary'}" data-tab="INCOME">Income</button>
    </div>

    <div id="categories-grid" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4"></div>
  `;

  document.getElementById('add-category-btn').addEventListener('click', () => openCategoryModal(null));
  document.getElementById('trash-btn').addEventListener('click', () => openTrashCategories());
  document.querySelectorAll('.cat-tab').forEach(tab => {
    tab.addEventListener('click', (e) => {
      activeTab = e.target.dataset.tab;
      renderCategories(main);
    });
  });

  await loadCategories();
}

async function loadCategories() {
  const grid = document.getElementById('categories-grid');
  showLoading(grid);

  const typeParam = activeTab !== 'all' ? `&type=${activeTab}` : '';

  try {
    const res = await api.get(`/categories?page=0&size=200&sort=name${typeParam}`);
    const categories = res?.data?.content || [];

    if (categories.length === 0) {
      showEmpty(grid, 'No categories found');
      return;
    }

    grid.innerHTML = categories.map(c => `
      <div class="card group hover:shadow-md transition-shadow">
        <div class="flex items-start justify-between">
          <div class="flex items-center gap-3">
            <div class="color-dot" style="background-color: ${c.color || '#9ca3af'}; width: 2rem; height: 2rem; border-radius: 0.5rem;"></div>
            <div>
              <h3 class="text-sm font-semibold text-gray-900">${c.name}</h3>
              <span class="badge ${c.type === 'INCOME' ? 'badge-income' : 'badge-expense'} mt-1">${c.type}</span>
            </div>
          </div>
          <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button class="btn btn-ghost btn-sm p-1 edit-cat" data-id="${c.id}">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
            ${!c.isDefault ? `
              <button class="btn btn-ghost btn-sm p-1 delete-cat text-red-500 hover:text-red-700" data-id="${c.id}">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            ` : ''}
          </div>
        </div>
        ${c.isDefault ? '<p class="text-xs text-gray-400 mt-2">Default category</p>' : ''}
      </div>
    `).join('');

    grid.querySelectorAll('.edit-cat').forEach(btn => {
      btn.addEventListener('click', () => {
        const cat = categories.find(c => c.id === btn.dataset.id);
        if (cat) openCategoryModal(cat);
      });
    });

    grid.querySelectorAll('.delete-cat').forEach(btn => {
      btn.addEventListener('click', () => {
        confirmDialog('Delete this category? It will fail if any transactions use it.', async () => {
          try {
            await api.delete(`/categories/${btn.dataset.id}`);
            showToast('Category deleted', 'success');
            loadCategories();
          } catch {}
        }, true);
      });
    });
  } catch {
    grid.innerHTML = `<p class="text-sm text-red-500 text-center py-8 col-span-full">Failed to load categories.</p>`;
  }
}

function openCategoryModal(existing) {
  const isEdit = !!existing;

  openModal(
    isEdit ? 'Edit Category' : 'New Category',
    `<form id="cat-form" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Name</label>
        <input type="text" id="cat-name" class="input" required maxlength="100" value="${existing?.name || ''}">
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Type</label>
        <select id="cat-type" class="input" required ${isEdit ? 'disabled' : ''}>
          <option value="EXPENSE" ${existing?.type === 'EXPENSE' ? 'selected' : ''}>Expense</option>
          <option value="INCOME" ${existing?.type === 'INCOME' ? 'selected' : ''}>Income</option>
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Color</label>
        <div class="flex items-center gap-3">
          <input type="color" id="cat-color" value="${existing?.color || '#6366f1'}" class="h-10 w-14 rounded border cursor-pointer">
          <input type="text" id="cat-color-text" class="input flex-1" value="${existing?.color || '#6366f1'}" maxlength="20">
        </div>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Icon</label>
        <input type="text" id="cat-icon" class="input" placeholder="e.g. restaurant, home, bolt" maxlength="50" value="${existing?.icon || ''}">
      </div>
    </form>`,
    `<button class="btn btn-secondary" id="cat-cancel">Cancel</button>
     <button class="btn btn-primary" id="cat-save">${isEdit ? 'Save Changes' : 'Create'}</button>`
  );

  document.getElementById('cat-cancel').addEventListener('click', closeModal);
  document.getElementById('cat-color').addEventListener('input', (e) => {
    document.getElementById('cat-color-text').value = e.target.value;
  });
  document.getElementById('cat-color-text').addEventListener('input', (e) => {
    document.getElementById('cat-color').value = e.target.value;
  });

  document.getElementById('cat-save').addEventListener('click', async () => {
    const body = {
      name: document.getElementById('cat-name').value.trim(),
      type: document.getElementById('cat-type').value,
      color: document.getElementById('cat-color-text').value || null,
      icon: document.getElementById('cat-icon').value || null,
    };

    if (!body.name) { showToast('Name is required', 'error'); return; }

    try {
      if (isEdit) {
        await api.patch(`/categories/${existing.id}`, { name: body.name, color: body.color, icon: body.icon });
        showToast('Category updated', 'success');
      } else {
        await api.post('/categories', body);
        showToast('Category created', 'success');
      }
      closeModal();
      loadCategories();
    } catch {}
  });
}

function openTrashCategories() {
  openTrashModal({
    title: 'Deleted Categories',
    emptyMessage: 'No deleted categories',
    fetchTrash: (page) => api.get(`/categories/trash?page=${page}&size=20&sort=deletedAt,desc`),
    restoreItem: (id) => api.put(`/categories/${id}/restore`),
    onClose: loadCategories,
    columns: [
      { header: 'Name', key: 'name', render: (r) => `<span class="text-sm font-medium">${r.name}</span>` },
      { header: 'Type', key: 'type', render: (r) => `<span class="badge ${r.type === 'INCOME' ? 'badge-income' : 'badge-expense'}">${r.type}</span>` },
      { header: 'Color', key: 'color', render: (r) => `
          <div class="flex items-center gap-2">
            <span class="color-dot" style="background-color: ${r.color || '#9ca3af'}; width: 1.5rem; height: 1.5rem;"></span>
            <span class="text-sm text-gray-500">${r.color || '-'}</span>
          </div>` },
      { header: 'Icon', key: 'icon', render: (r) => `<span class="text-sm text-gray-500">${r.icon || '-'}</span>` },
      { header: 'Deleted At', key: 'deletedAt', render: (r) => `<span class="text-sm text-gray-500">${formatDate(r.deletedAt)}</span>` },
      { header: '', key: 'actions', render: (r) => `
          <button class="btn btn-ghost btn-sm p-1 restore-btn text-green-600 hover:text-green-800" data-id="${r.id}" title="Restore">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 9l-7 7-7-7"/><path d="M5 18v-2a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v2"/></svg>
          </button>` },
    ],
  });
}
