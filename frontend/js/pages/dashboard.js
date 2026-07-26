import { api } from '../api.js';
import { formatCurrency, getCurrentMonth, getMonthLabel, getFirstDayOfMonth, getLastDayOfMonth, formatDate } from '../utils.js';
import { showLoading } from '../components/loading.js';

export async function renderDashboard(main) {
  showLoading(main);
  const month = getCurrentMonth();
  const startDate = getFirstDayOfMonth(month);
  const endDate = getLastDayOfMonth(month);

  try {
    const [summaryRes, trendRes, transRes] = await Promise.all([
      api.get(`/reports/summary?startDate=${startDate}&endDate=${endDate}`),
      api.get(`/reports/trend?startDate=${getFirstDayOfMonth(String(Number(month.split('-')[0]) - 1).padStart(4, '0') + '-01')}&endDate=${endDate}`),
      api.get(`/transactions?page=0&size=5&month=${month}`),
    ]);

    const summary = summaryRes?.data || {};
    const trend = trendRes?.data || [];
    const transactions = transRes?.data?.content || [];

    main.innerHTML = `
      <div class="page-header">
        <div>
          <h1 class="page-title">Dashboard</h1>
          <p class="text-sm text-gray-500 mt-1">${getMonthLabel(month)}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="card">
          <div class="flex items-center gap-3 mb-2">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-green-50 text-green-600">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
            </div>
            <span class="text-sm font-medium text-gray-500">Income</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">${formatCurrency(summary.totalIncome)}</p>
        </div>
        <div class="card">
          <div class="flex items-center gap-3 mb-2">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-red-50 text-red-600">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 18 13.5 8.5 8.5 13.5 1 6"/><polyline points="17 18 23 18 23 12"/></svg>
            </div>
            <span class="text-sm font-medium text-gray-500">Expenses</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">${formatCurrency(summary.totalExpenses)}</p>
        </div>
        <div class="card">
          <div class="flex items-center gap-3 mb-2">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
            </div>
            <span class="text-sm font-medium text-gray-500">Net Balance</span>
          </div>
          <p class="text-2xl font-bold ${(summary.netBalance || 0) >= 0 ? 'text-green-600' : 'text-red-600'}">${formatCurrency(summary.netBalance)}</p>
        </div>
        <div class="card">
          <div class="flex items-center gap-3 mb-2">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-amber-50 text-amber-600">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            </div>
            <span class="text-sm font-medium text-gray-500">Transactions</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">${summary.transactionCount || 0}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div class="card">
          <h3 class="text-sm font-semibold text-gray-700 mb-4">Monthly Trend</h3>
          <div style="height: 250px"><canvas id="trend-chart"></canvas></div>
        </div>
        <div class="card">
          <h3 class="text-sm font-semibold text-gray-700 mb-4">Recent Transactions</h3>
          <div id="recent-transactions"></div>
        </div>
      </div>
    `;

    renderTrendChart(trend);
    renderRecentTransactions(transactions);
  } catch (err) {
    main.innerHTML = `
      <div class="page-header">
        <h1 class="page-title">Dashboard</h1>
      </div>
      <div class="card text-center py-12">
        <p class="text-gray-500 mb-4">Could not load dashboard data.</p>
        <button class="btn btn-primary" onclick="window.location.reload()">Retry</button>
      </div>
    `;
  }
}

function renderTrendChart(trend) {
  const canvas = document.getElementById('trend-chart');
  if (!canvas || !trend.length) return;

  const labels = trend.map(t => {
    const [y, m] = t.month.split('-');
    return new Date(Number(y), Number(m) - 1).toLocaleDateString('en-US', { month: 'short', year: '2-digit' });
  });

  new Chart(canvas, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: 'Income',
          data: trend.map(t => t.income),
          borderColor: '#22c55e',
          backgroundColor: 'rgba(34, 197, 94, 0.1)',
          fill: true,
          tension: 0.3,
          pointRadius: 4,
          pointBackgroundColor: '#22c55e',
        },
        {
          label: 'Expenses',
          data: trend.map(t => t.expenses),
          borderColor: '#ef4444',
          backgroundColor: 'rgba(239, 68, 68, 0.1)',
          fill: true,
          tension: 0.3,
          pointRadius: 4,
          pointBackgroundColor: '#ef4444',
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'top', labels: { usePointStyle: true, padding: 20 } },
        tooltip: { callbacks: { label: (ctx) => `${ctx.dataset.label}: ${formatCurrency(ctx.raw)}` } },
      },
      scales: {
        y: { beginAtZero: true, ticks: { callback: (v) => '$' + v.toLocaleString() } },
      },
    },
  });
}

function renderRecentTransactions(transactions) {
  const container = document.getElementById('recent-transactions');
  if (!transactions.length) {
    container.innerHTML = `<p class="text-sm text-gray-400 text-center py-8">No transactions this month</p>`;
    return;
  }

  container.innerHTML = `
    <div class="space-y-3">
      ${transactions.map(t => `
        <div class="flex items-center justify-between py-2 border-b border-gray-50 last:border-0">
          <div class="flex items-center gap-3">
            <div class="color-dot" style="background-color: ${t.category?.color || '#9ca3af'}"></div>
            <div>
              <p class="text-sm font-medium text-gray-900">${t.category?.name || 'Uncategorized'}</p>
              <p class="text-xs text-gray-400">${t.note || ''}</p>
            </div>
          </div>
          <div class="text-right">
            <p class="text-sm font-semibold ${t.type === 'INCOME' ? 'text-green-600' : 'text-red-600'}">
              ${t.type === 'INCOME' ? '+' : '-'}${formatCurrency(t.amount)}
            </p>
            <p class="text-xs text-gray-400">${formatDate(t.transactionDate)}</p>
          </div>
        </div>
      `).join('')}
    </div>
    <div class="mt-4 text-center">
      <a onclick="window.location.hash='#/transactions'" class="text-sm text-brand-600 font-medium hover:text-brand-700 cursor-pointer">View all transactions</a>
    </div>
  `;
}
