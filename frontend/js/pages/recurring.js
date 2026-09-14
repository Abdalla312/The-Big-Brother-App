import { api } from '../api.js';
import { formatCurrency, formatDate, getToday } from '../utils.js';
import { showLoading } from '../components/loading.js';
import { renderTable } from '../components/table.js';
import { openModal, closeModal, confirmDialog } from '../components/modal.js';
import { showToast } from '../components/toast.js';
import { openTrashModal } from '../components/trash-modal.js';

const PAGE_SIZE = 15;
const FREQUENCY_LABELS = { DAILY: 'Daily', WEEKLY: 'Weekly', MONTHLY: 'Monthly', YEARLY: 'Yearly' };

let currentPage = 0;

export async function renderRecurring(main) {
  showLoading(main);
  try {
    const catRes = await api.get('/categories?page=0&size=100&sort=name');
    const categories = catRes?.data?.content || [];
    await renderPage(main, categories);
  } catch {
    main.innerHTML = `<div class="card text-center py-12"><p class="text-gray-500">Failed to load categories.</p></div>`;
  }
}

async function renderPage(main, categories) {
  main.innerHTML = `
    <div class="page-header">
      <h1 class="page-title">Recurring Transactions</h1>
      <div class="flex items-center gap-2">
        <button class="btn btn-secondary" id="trash-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          Trash
        </button>
        <button class="btn btn-primary" id="add-recurring-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Add Recurring
        </button>
      </div>
    </div>

    <div class="card">
      <div id="recurring-table"></div>
    </div>
  `;

  document.getElementById('add-recurring-btn').addEventListener('click', () => openRecurringModal(categories));
  document.getElementById('trash-btn').addEventListener('click', () => openTrashRecurring(categories));

  await loadRecurring(categories);
}

async function loadRecurring(categories) {
  const tableContainer = document.getElementById('recurring-table');
  showLoading(tableContainer);

  try {
    const res = await api.get(`/recurring-transactions?page=${currentPage}&size=${PAGE_SIZE}&sort=nextExecutionDate`);
    const data = res?.data || {};
    const recurring = data.content || [];

    renderTable(tableContainer, {
      columns: [
        { header: 'Frequency', render: (r) => `<span class="text-sm">${FREQUENCY_LABELS[r.frequency] || r.frequency}</span>` },
        { header: 'Type', render: (r) => `<span class="badge ${r.type === 'INCOME' ? 'badge-income' : 'badge-expense'}">${r.type}</span>` },
        { header: 'Category', render: (r) => `
          <div class="flex items-center gap-2">
            <span class="color-dot" style="background-color: ${r.category?.color || '#9ca3af'}"></span>
            <span class="text-sm">${r.category?.name || '-'}</span>
          </div>` },
        { header: 'Amount', render: (r) => `<span class="text-sm font-semibold">${formatCurrency(r.amount)}</span>` },
        { header: 'Next Execution', render: (r) => `<span class="text-sm">${formatDate(r.nextExecutionDate)}</span>` },
        { header: 'Status', render: (r) => `<span class="badge ${r.isActive ? 'badge-active' : 'badge-paused'}">${r.isActive ? 'Active' : 'Paused'}</span>` },
        { header: 'Note', render: (r) => `<span class="text-sm text-gray-500 max-w-[150px] truncate inline-block" title="${r.note || ''}">${r.note || '-'}</span>` },
        { header: '', render: (r) => `
          <div class="flex items-center gap-1">
            <button class="btn btn-ghost btn-sm p-1 toggle-recurring" data-id="${r.id}" data-active="${r.isActive}" title="${r.isActive ? 'Pause' : 'Resume'}">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">${r.isActive ? '<rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/>' : '<polygon points="5 3 19 12 5 21 5 3"/>'}</svg>
            </button>
            <button class="btn btn-ghost btn-sm p-1 edit-recurring" data-id="${r.id}" title="Edit">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
            <button class="btn btn-ghost btn-sm p-1 delete-recurring text-red-500 hover:text-red-700" data-id="${r.id}" title="Delete">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>` },
      ],
      rows: recurring,
      pagination: { page: data.page, size: data.size, totalPages: data.totalPages, totalElements: data.totalElements, first: data.first, last: data.last },
      onPageChange: (p) => { currentPage = p; loadRecurring(categories); },
    });

    tableContainer.querySelectorAll('.edit-recurring').forEach(btn => {
      btn.addEventListener('click', () => {
        const item = recurring.find(t => t.id === btn.dataset.id);
        if (item) openRecurringModal(categories, item);
      });
    });

    tableContainer.querySelectorAll('.toggle-recurring').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          await api.patch(`/recurring-transactions/${btn.dataset.id}/toggle`);
          showToast(btn.dataset.active === 'true' ? 'Recurring transaction paused' : 'Recurring transaction resumed', 'success');
          loadRecurring(categories);
        } catch {}
      });
    });

    tableContainer.querySelectorAll('.delete-recurring').forEach(btn => {
      btn.addEventListener('click', () => {
        confirmDialog('Are you sure you want to delete this recurring transaction?', async () => {
          try {
            await api.delete(`/recurring-transactions/${btn.dataset.id}`);
            showToast('Recurring transaction deleted', 'success');
            loadRecurring(categories);
          } catch {}
        }, true);
      });
    });
  } catch {
    tableContainer.innerHTML = `<p class="text-sm text-red-500 text-center py-8">Failed to load recurring transactions.</p>`;
  }
}

function openRecurringModal(categories, existing = null) {
  const isEdit = !!existing;
  const expenseCategories = categories.filter(c => c.type === 'EXPENSE');
  const incomeCategories = categories.filter(c => c.type === 'INCOME');
  const selectedType = existing?.type || 'EXPENSE';
  const selectedPayment = existing?.paymentMethod || '';

  openModal(
    isEdit ? 'Edit Recurring Transaction' : 'New Recurring Transaction',
    `<form id="recurring-form" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Type</label>
        <select id="recurring-type" class="input" required>
          <option value="EXPENSE" ${selectedType === 'EXPENSE' ? 'selected' : ''}>Expense</option>
          <option value="INCOME" ${selectedType === 'INCOME' ? 'selected' : ''}>Income</option>
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Category</label>
        <select id="recurring-category" class="input" required>
          ${renderCategoryOptions(selectedType === 'INCOME' ? incomeCategories : expenseCategories, existing?.category?.id)}
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Amount</label>
        <input type="number" id="recurring-amount" class="input" step="0.01" min="0.01" required value="${existing?.amount || ''}">
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Frequency</label>
        <select id="recurring-frequency" class="input" required>
          ${Object.entries(FREQUENCY_LABELS).map(([value, label]) => `<option value="${value}" ${existing?.frequency === value ? 'selected' : ''}>${label}</option>`).join('')}
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">${isEdit ? 'Next Execution Date' : 'Start Date'}</label>
        <input type="date" id="recurring-date" class="input" required value="${isEdit ? existing?.nextExecutionDate : getToday()}">
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Payment Method</label>
        <input type="text" id="recurring-payment" class="input" list="payment-options"
          placeholder="e.g. Credit Card, Cash" value="${selectedPayment}">
        <datalist id="payment-options">
          <option value="Credit Card">
          <option value="Debit Card">
          <option value="Cash">
          <option value="Bank Transfer">
          <option value="PayPal">
          <option value="Apple Pay">
          <option value="Google Pay">
        </datalist>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Note</label>
        <textarea id="recurring-note" class="input" rows="2" placeholder="Optional note">${existing?.note || ''}</textarea>
      </div>
    </form>`,
    `<button class="btn btn-secondary" id="recurring-cancel">Cancel</button>
     <button class="btn btn-primary" id="recurring-save">${isEdit ? 'Save Changes' : 'Create'}</button>`
  );

  document.getElementById('recurring-cancel').addEventListener('click', closeModal);

  document.getElementById('recurring-type').addEventListener('change', (e) => {
    const cats = e.target.value === 'INCOME' ? incomeCategories : expenseCategories;
    document.getElementById('recurring-category').innerHTML = renderCategoryOptions(cats, null);
  });

  document.getElementById('recurring-save').addEventListener('click', async () => {
    const amount = parseFloat(document.getElementById('recurring-amount').value);
    const categoryId = document.getElementById('recurring-category').value;
    const frequency = document.getElementById('recurring-frequency').value;
    const date = document.getElementById('recurring-date').value;

    if (isEdit) {
      const body = {};
      if (!isNaN(amount)) body.amount = amount;
      if (categoryId) body.categoryId = categoryId;
      body.frequency = frequency;
      if (date) body.nextExecutionDate = date;
      body.paymentMethod = document.getElementById('recurring-payment').value || null;
      body.note = document.getElementById('recurring-note').value || null;

      if (!body.frequency) {
        showToast('Please fill in all required fields', 'error');
        return;
      }

      try {
        await api.patch(`/recurring-transactions/${existing.id}`, body);
        showToast('Recurring transaction updated', 'success');
        closeModal();
        loadRecurring(categories);
      } catch {}
      return;
    }

    const body = {
      type: document.getElementById('recurring-type').value,
      categoryId,
      amount,
      frequency,
      startDate: date,
      paymentMethod: document.getElementById('recurring-payment').value || null,
      note: document.getElementById('recurring-note').value || null,
    };

    if (!body.categoryId || !body.amount || !body.frequency || !body.startDate) {
      showToast('Please fill in all required fields', 'error');
      return;
    }

    try {
      await api.post('/recurring-transactions', body);
      showToast('Recurring transaction created', 'success');
      closeModal();
      loadRecurring(categories);
    } catch {}
  });
}

function openTrashRecurring(categories) {
  openTrashModal({
    title: 'Deleted Recurring Transactions',
    emptyMessage: 'No deleted recurring transactions',
    fetchTrash: (page) => api.get(`/recurring-transactions/trash?page=${page}&size=20&sort=deletedAt,desc`),
    restoreItem: (id) => api.put(`/recurring-transactions/${id}/restore`),
    onClose: () => loadRecurring(categories),
    columns: [
      { header: 'Frequency', key: 'frequency', render: (r) => `<span class="text-sm">${FREQUENCY_LABELS[r.frequency] || r.frequency}</span>` },
      { header: 'Type', key: 'type', render: (r) => `<span class="badge ${r.type === 'INCOME' ? 'badge-income' : 'badge-expense'}">${r.type}</span>` },
      { header: 'Category', key: 'category', render: (r) => `
          <div class="flex items-center gap-2">
            <span class="color-dot" style="background-color: ${r.category?.color || '#9ca3af'}"></span>
            <span class="text-sm">${r.category?.name || '-'}</span>
          </div>` },
      { header: 'Amount', key: 'amount', render: (r) => `<span class="text-sm font-semibold">${formatCurrency(r.amount)}</span>` },
      { header: 'Next Execution', key: 'nextExecutionDate', render: (r) => `<span class="text-sm">${formatDate(r.nextExecutionDate)}</span>` },
      { header: 'Status', key: 'isActive', render: (r) => `<span class="badge ${r.isActive ? 'badge-active' : 'badge-paused'}">${r.isActive ? 'Active' : 'Paused'}</span>` },
      { header: 'Deleted At', key: 'deletedAt', render: (r) => `<span class="text-sm text-gray-500">${formatDate(r.deletedAt)}</span>` },
      { header: '', key: 'actions', render: (r) => `
          <button class="btn btn-ghost btn-sm p-1 restore-btn text-green-600 hover:text-green-800" data-id="${r.id}" title="Restore">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 9l-7 7-7-7"/><path d="M5 18v-2a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v2"/></svg>
          </button>` },
    ],
  });
}

function renderCategoryOptions(categories, selectedId) {
  return categories.map(c => `<option value="${c.id}" ${c.id === selectedId ? 'selected' : ''}>${c.name}</option>`).join('');
}