import { api } from '../api.js';
import { formatCurrency, getCurrentMonth, getMonthLabel } from '../utils.js';
import { showLoading, showEmpty } from '../components/loading.js';
import { openModal, closeModal, confirmDialog } from '../components/modal.js';
import { showToast } from '../components/toast.js';

let currentMonth = getCurrentMonth();

export async function renderBudgets(main) {
  showLoading(main);

  main.innerHTML = `
    <div class="page-header">
      <h1 class="page-title">Budgets</h1>
      <button class="btn btn-primary" id="add-budget-btn">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        Add Budget
      </button>
    </div>

    <div class="flex items-center gap-3 mb-4">
      <label class="text-sm font-medium text-gray-600">Month:</label>
      <input type="month" id="budget-month" class="input" style="width: auto" value="${currentMonth}">
      <span class="text-sm text-gray-500">${getMonthLabel(currentMonth)}</span>
    </div>

    <div id="budgets-grid" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4"></div>
  `;

  document.getElementById('add-budget-btn').addEventListener('click', () => openBudgetModal(null));
  document.getElementById('budget-month').addEventListener('change', (e) => {
    currentMonth = e.target.value;
    document.querySelector('#budget-month + span').textContent = getMonthLabel(currentMonth);
    loadBudgets();
  });

  await loadBudgets();
}

async function loadBudgets() {
  const grid = document.getElementById('budgets-grid');
  showLoading(grid);

  try {
    const [budgetRes, catRes] = await Promise.all([
      api.get(`/budget?month=${currentMonth}&page=0&size=100`),
      api.get('/categories?page=0&size=100'),
    ]);

    const budgets = budgetRes?.data?.content || [];
    const categories = catRes?.data?.content || [];

    if (budgets.length === 0) {
      showEmpty(grid, `No budgets for ${getMonthLabel(currentMonth)}. Click "Add Budget" to create one.`);
      return;
    }

    grid.innerHTML = budgets.map(b => {
      const pct = b.percentUsed || 0;
      const barColor = pct >= 90 ? '#dc2626' : pct >= 75 ? '#f59e0b' : '#22c55e';

      return `
        <div class="card group hover:shadow-md transition-shadow">
          <div class="flex items-start justify-between mb-3">
            <div class="flex items-center gap-3">
              <div class="color-dot" style="background-color: ${b.category?.color || '#9ca3af'}; width: 2.5rem; height: 2.5rem; border-radius: 0.5rem;"></div>
              <div>
                <h3 class="text-sm font-semibold text-gray-900">${b.category?.name || 'Unknown'}</h3>
                <p class="text-xs text-gray-400">${getMonthLabel(b.month)}</p>
              </div>
            </div>
            <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button class="btn btn-ghost btn-sm p-1 edit-budget" data-id="${b.id}">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
              </button>
              <button class="btn btn-ghost btn-sm p-1 delete-budget text-red-500 hover:text-red-700" data-id="${b.id}">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              </button>
            </div>
          </div>
          <div class="mb-2">
            <div class="flex justify-between text-sm mb-1">
              <span class="text-gray-600">Spent: <span class="font-semibold">${formatCurrency(b.spentAmount)}</span></span>
              <span class="text-gray-500">of ${formatCurrency(b.limitAmount)}</span>
            </div>
            <div class="progress-bar">
              <div class="progress-bar-fill" style="width: ${Math.min(pct, 100)}%; background-color: ${barColor}"></div>
            </div>
          </div>
          <div class="flex justify-between text-xs text-gray-500 mt-2">
            <span>${pct.toFixed(0)}% used</span>
            <span>${formatCurrency(b.remainingAmount)} remaining</span>
          </div>
        </div>
      `;
    }).join('');

    grid.querySelectorAll('.edit-budget').forEach(btn => {
      btn.addEventListener('click', () => {
        const b = budgets.find(b => b.id === btn.dataset.id);
        if (b) openBudgetModal(b, categories);
      });
    });

    grid.querySelectorAll('.delete-budget').forEach(btn => {
      btn.addEventListener('click', () => {
        confirmDialog('Delete this budget?', async () => {
          try {
            await api.delete(`/budget/${btn.dataset.id}`);
            showToast('Budget deleted', 'success');
            loadBudgets();
          } catch {}
        }, true);
      });
    });
  } catch {
    grid.innerHTML = `<p class="text-sm text-red-500 text-center py-8 col-span-full">Failed to load budgets.</p>`;
  }
}

async function openBudgetModal(existing, preloadedCategories) {
  let categories = preloadedCategories;
  if (!categories) {
    try {
      const res = await api.get('/categories?page=0&size=100&sort=name&type=EXPENSE');
      categories = res?.data?.content || [];
    } catch { categories = []; }
  }

  const isEdit = !!existing;

  openModal(
    isEdit ? 'Edit Budget' : 'New Budget',
    `<form id="budget-form" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Category</label>
        <select id="budget-category" class="input" required ${isEdit ? 'disabled' : ''}>
          <option value="">Select category</option>
          ${categories.map(c => `<option value="${c.id}" ${existing?.category?.id === c.id ? 'selected' : ''}>${c.name}</option>`).join('')}
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Month</label>
        <input type="month" id="budget-month-input" class="input" required value="${existing?.month || currentMonth}" ${isEdit ? 'disabled' : ''}>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Limit Amount</label>
        <input type="number" id="budget-limit" class="input" step="0.01" min="0.01" required value="${existing?.limitAmount || ''}">
      </div>
    </form>`,
    `<button class="btn btn-secondary" id="budget-cancel">Cancel</button>
     <button class="btn btn-primary" id="budget-save">${isEdit ? 'Save Changes' : 'Create'}</button>`
  );

  document.getElementById('budget-cancel').addEventListener('click', closeModal);

  document.getElementById('budget-save').addEventListener('click', async () => {
    const body = {
      categoryId: document.getElementById('budget-category').value,
      month: document.getElementById('budget-month-input').value,
      limitAmount: parseFloat(document.getElementById('budget-limit').value),
    };

    if (!body.categoryId || !body.month || !body.limitAmount) {
      showToast('Please fill in all required fields', 'error');
      return;
    }

    try {
      if (isEdit) {
        await api.patch(`/budget/${existing.id}`, { limitAmount: body.limitAmount });
        showToast('Budget updated', 'success');
      } else {
        await api.post('/budget', body);
        showToast('Budget created', 'success');
      }
      closeModal();
      loadBudgets();
    } catch {}
  });
}
