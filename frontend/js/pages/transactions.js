import { api } from '../api.js';
import { formatCurrency, formatDate, getCurrentMonth, getMonthLabel } from '../utils.js';
import { showLoading } from '../components/loading.js';
import { renderTable } from '../components/table.js';
import { openModal, closeModal, confirmDialog } from '../components/modal.js';
import { showToast } from '../components/toast.js';
import { getAccessToken } from '../auth.js';
import { API_URL } from '../config.js';

let currentMonth = getCurrentMonth();
let currentPage = 0;
let currentType = '';
let currentCategoryId = '';
const PAGE_SIZE = 15;

export async function renderTransactions(main) {
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
      <h1 class="page-title">Transactions</h1>
      <div class="flex items-center gap-2">
        <button class="btn btn-secondary" id="export-csv-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          Export CSV
        </button>
        <button class="btn btn-primary" id="add-transaction-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Add Transaction
        </button>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <div>
          <label class="block text-xs font-medium text-gray-500 mb-1">Month</label>
          <input type="month" id="filter-month" class="input" value="${currentMonth}">
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-500 mb-1">Type</label>
          <select id="filter-type" class="input">
            <option value="">All Types</option>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-500 mb-1">Category</label>
          <select id="filter-category" class="input">
            <option value="">All Categories</option>
            ${categories.map(c => `<option value="${c.id}">${c.name}</option>`).join('')}
          </select>
        </div>
      </div>
      <div id="transactions-table"></div>
    </div>
  `;

  document.getElementById('add-transaction-btn').addEventListener('click', () => openTransactionModal(categories));
  document.getElementById('export-csv-btn').addEventListener('click', () => downloadCsv());
  document.getElementById('filter-month').addEventListener('change', (e) => { currentMonth = e.target.value; currentPage = 0; loadTransactions(categories); });
  document.getElementById('filter-type').addEventListener('change', (e) => { currentType = e.target.value; currentPage = 0; loadTransactions(categories); });
  document.getElementById('filter-category').addEventListener('change', (e) => { currentCategoryId = e.target.value; currentPage = 0; loadTransactions(categories); });

  if (currentType) document.getElementById('filter-type').value = currentType;
  if (currentCategoryId) document.getElementById('filter-category').value = currentCategoryId;

  await loadTransactions(categories);
}

async function loadTransactions(categories) {
  const tableContainer = document.getElementById('transactions-table');
  showLoading(tableContainer);

  const params = new URLSearchParams({ page: currentPage, size: PAGE_SIZE });
  if (currentMonth) params.set('month', currentMonth);
  if (currentType) params.set('type', currentType);
  if (currentCategoryId) params.set('categoryId', currentCategoryId);

  try {
    const res = await api.get(`/transactions?${params}`);
    const data = res?.data || {};
    const transactions = data.content || [];

    renderTable(tableContainer, {
      columns: [
        { header: 'Date', render: (r) => `<span class="text-sm">${formatDate(r.transactionDate)}</span>` },
        { header: 'Type', render: (r) => `<span class="badge ${r.type === 'INCOME' ? 'badge-income' : 'badge-expense'}">${r.type}</span>` },
        { header: 'Category', render: (r) => `
          <div class="flex items-center gap-2">
            <span class="color-dot" style="background-color: ${r.category?.color || '#9ca3af'}"></span>
            <span class="text-sm">${r.category?.name || '-'}</span>
          </div>` },
        { header: 'Amount', render: (r) => `<span class="text-sm font-semibold">${formatCurrency(r.amount)}</span>` },
        { header: 'Payment', render: (r) => `<span class="text-sm text-gray-500">${r.paymentMethod || '-'}</span>` },
        { header: 'Note', render: (r) => `<span class="text-sm text-gray-500 max-w-[150px] truncate inline-block" title="${r.note || ''}">${r.note || '-'}</span>` },
        { header: '', render: (r) => `
          <div class="flex items-center gap-1">
            <button class="btn btn-ghost btn-sm p-1 edit-txn" data-id="${r.id}" title="Edit">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            </button>
            <button class="btn btn-ghost btn-sm p-1 delete-txn text-red-500 hover:text-red-700" data-id="${r.id}" title="Delete">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>` },
      ],
      rows: transactions,
      pagination: { page: data.page, size: data.size, totalPages: data.totalPages, totalElements: data.totalElements, first: data.first, last: data.last },
      onPageChange: (p) => { currentPage = p; loadTransactions(categories); },
    });

    tableContainer.querySelectorAll('.edit-txn').forEach(btn => {
      btn.addEventListener('click', () => {
        const txn = transactions.find(t => t.id === btn.dataset.id);
        if (txn) openTransactionModal(categories, txn);
      });
    });

    tableContainer.querySelectorAll('.delete-txn').forEach(btn => {
      btn.addEventListener('click', () => {
        confirmDialog('Are you sure you want to delete this transaction?', async () => {
          try {
            await api.delete(`/transactions/${btn.dataset.id}`);
            showToast('Transaction deleted', 'success');
            loadTransactions(categories);
          } catch {}
        }, true);
      });
    });
  } catch {
    tableContainer.innerHTML = `<p class="text-sm text-red-500 text-center py-8">Failed to load transactions.</p>`;
  }
}

function openTransactionModal(categories, existing = null) {
  const isEdit = !!existing;
  const expenseCategories = categories.filter(c => c.type === 'EXPENSE');
  const incomeCategories = categories.filter(c => c.type === 'INCOME');
  const selectedType = existing?.type || 'EXPENSE';

  openModal(
    isEdit ? 'Edit Transaction' : 'New Transaction',
    `<form id="txn-form" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Type</label>
        <select id="txn-type" class="input" required>
          <option value="EXPENSE" ${selectedType === 'EXPENSE' ? 'selected' : ''}>Expense</option>
          <option value="INCOME" ${selectedType === 'INCOME' ? 'selected' : ''}>Income</option>
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Category</label>
        <select id="txn-category" class="input" required>
          ${renderCategoryOptions(selectedType === 'INCOME' ? incomeCategories : expenseCategories, existing?.category?.id)}
        </select>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Amount</label>
        <input type="number" id="txn-amount" class="input" step="0.01" min="0.01" required value="${existing?.amount || ''}">
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Date</label>
        <input type="date" id="txn-date" class="input" required value="${existing?.transactionDate || new Date().toISOString().split('T')[0]}">
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Payment Method</label>                                      
          <input type="text" id="txn-payment" class="input" list="payment-options"                                                
            placeholder="e.g. Credit Card, Cash" value="${existing?.paymentMethod || ''}">                                   
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
        <textarea id="txn-note" class="input" rows="2" placeholder="Optional note">${existing?.note || ''}</textarea>
      </div>
    </form>`,
    `<button class="btn btn-secondary" id="txn-cancel">Cancel</button>
     <button class="btn btn-primary" id="txn-save">${isEdit ? 'Save Changes' : 'Create'}</button>`
  );

  document.getElementById('txn-cancel').addEventListener('click', closeModal);

  document.getElementById('txn-type').addEventListener('change', (e) => {
    const cats = e.target.value === 'INCOME' ? incomeCategories : expenseCategories;
    document.getElementById('txn-category').innerHTML = renderCategoryOptions(cats, null);
  });

  document.getElementById('txn-save').addEventListener('click', async () => {
    const body = {
      type: document.getElementById('txn-type').value,
      categoryId: document.getElementById('txn-category').value,
      amount: parseFloat(document.getElementById('txn-amount').value),
      transactionDate: document.getElementById('txn-date').value,
      paymentMethod: document.getElementById('txn-payment').value || null,
      note: document.getElementById('txn-note').value || null,
    };

    if (!body.categoryId || !body.amount || !body.transactionDate) {
      showToast('Please fill in all required fields', 'error');
      return;
    }

    try {
      if (isEdit) {
        await api.patch(`/transactions/${existing.id}`, body);
        showToast('Transaction updated', 'success');
      } else {
        await api.post('/transactions', body);
        showToast('Transaction created', 'success');
      }
      closeModal();
      loadTransactions(categories);
    } catch {}
  });
}

function renderCategoryOptions(categories, selectedId) {
  return categories.map(c => `<option value="${c.id}" ${c.id === selectedId ? 'selected' : ''}>${c.name}</option>`).join('');
}

async function downloadCsv() {
  const params = new URLSearchParams();
  if (currentMonth) {
    const [year, month] = currentMonth.split('-');
    const lastDay = new Date(year, month, 0).getDate();
    params.set('from', `${currentMonth}-01`);
    params.set('to', `${currentMonth}-${String(lastDay).padStart(2, '0')}`);
  }
  if (currentType) params.set('type', currentType);
  if (currentCategoryId) params.set('categoryId', currentCategoryId);

  try {
    const token = getAccessToken();
    const res = await fetch(`${API_URL}/api/v1/transactions/export?${params}`, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {},
    });

    if (!res.ok) {
      showToast('Failed to export transactions', 'error');
      return;
    }

    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const now = new Date();
    const ts = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}-${String(now.getMinutes()).padStart(2, '0')}-${String(now.getSeconds()).padStart(2, '0')}`;
    a.download = `transactions-${ts}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast('CSV downloaded', 'success');
  } catch {
    showToast('Failed to export transactions', 'error');
  }
}
