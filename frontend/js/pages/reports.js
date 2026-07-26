import { api } from '../api.js';
import { formatCurrency, getCurrentMonth, getFirstDayOfMonth, getLastDayOfMonth, getMonthLabel } from '../utils.js';
import { showLoading } from '../components/loading.js';

let currentMonth = getCurrentMonth();

export async function renderReports(main) {
  showLoading(main);

  const startDate = getFirstDayOfMonth(currentMonth);
  const endDate = getLastDayOfMonth(currentMonth);

  try {
    const [summaryRes, breakdownRes, trendRes, budgetCompRes, paymentRes] = await Promise.all([
      api.get(`/reports/summary?startDate=${startDate}&endDate=${endDate}`),
      api.get(`/reports/category-breakdown?startDate=${startDate}&endDate=${endDate}&type=EXPENSE`),
      api.get(`/reports/trend?startDate=${getFirstDayOfMonth(String(Number(currentMonth.split('-')[0]) - 1).padStart(4, '0') + '-01')}&endDate=${endDate}`),
      api.get(`/reports/budget-comparison?month=${currentMonth}`),
      api.get(`/reports/payment-method-breakdown?startDate=${startDate}&endDate=${endDate}&type=EXPENSE`),
    ]);

    const summary = summaryRes?.data || {};
    const breakdown = breakdownRes?.data || [];
    const trend = trendRes?.data || [];
    const budgetComp = budgetCompRes?.data || [];
    const paymentData = paymentRes?.data || [];

    main.innerHTML = `
      <div class="page-header">
        <h1 class="page-title">Reports</h1>
      </div>

      <div class="flex items-center gap-3 mb-6">
        <label class="text-sm font-medium text-gray-600">Month:</label>
        <input type="month" id="report-month" class="input" style="width: auto" value="${currentMonth}">
        <span class="text-sm text-gray-500">${getMonthLabel(currentMonth)}</span>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="card">
          <p class="text-xs font-medium text-gray-500 uppercase tracking-wide">Total Income</p>
          <p class="text-xl font-bold text-green-600 mt-1">${formatCurrency(summary.totalIncome)}</p>
        </div>
        <div class="card">
          <p class="text-xs font-medium text-gray-500 uppercase tracking-wide">Total Expenses</p>
          <p class="text-xl font-bold text-red-600 mt-1">${formatCurrency(summary.totalExpenses)}</p>
        </div>
        <div class="card">
          <p class="text-xs font-medium text-gray-500 uppercase tracking-wide">Net Balance</p>
          <p class="text-xl font-bold ${(summary.netBalance || 0) >= 0 ? 'text-green-600' : 'text-red-600'} mt-1">${formatCurrency(summary.netBalance)}</p>
        </div>
        <div class="card">
          <p class="text-xs font-medium text-gray-500 uppercase tracking-wide">Transactions</p>
          <p class="text-xl font-bold text-gray-900 mt-1">${summary.transactionCount || 0}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div class="card">
          <h3 class="text-sm font-semibold text-gray-700 mb-4">Category Breakdown</h3>
          <div style="height: 300px" class="flex items-center justify-center">
            <canvas id="breakdown-chart"></canvas>
          </div>
          ${breakdown.length ? `
            <div class="mt-4 space-y-2">
              ${breakdown.map(b => `
                <div class="flex items-center justify-between text-sm">
                  <div class="flex items-center gap-2">
                    <span class="color-dot" style="background-color: ${b.color || '#9ca3af'}"></span>
                    <span class="text-gray-700">${b.name}</span>
                  </div>
                  <div class="flex items-center gap-3">
                    <span class="text-gray-500">${b.percentage?.toFixed(1)}%</span>
                    <span class="font-medium">${formatCurrency(b.amount)}</span>
                  </div>
                </div>
              `).join('')}
            </div>
          ` : '<p class="text-sm text-gray-400 text-center mt-4">No expense data</p>'}
        </div>

        <div class="card">
          <h3 class="text-sm font-semibold text-gray-700 mb-4">Income vs Expenses Trend</h3>
          <div style="height: 300px"><canvas id="trend-chart"></canvas></div>
        </div>
      </div>

      ${budgetComp.length ? `
        <div class="card">
          <h3 class="text-sm font-semibold text-gray-700 mb-4">Budget vs Actual</h3>
          <div style="height: 350px"><canvas id="budget-chart"></canvas></div>
        </div>
      ` : ''}

      ${paymentData.length ? `
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          <div class="card">
            <h3 class="text-sm font-semibold text-gray-700 mb-4">Payment Methods</h3>
            <div style="height: 300px" class="flex items-center justify-center">
              <canvas id="payment-doughnut-chart"></canvas>
            </div>
            <div id="payment-doughnut-legend" class="mt-4 space-y-2"></div>
          </div>

          <div class="card">
            <h3 class="text-sm font-semibold text-gray-700 mb-4">Payment by Category</h3>
            <div style="height: 300px"><canvas id="payment-stacked-chart"></canvas></div>
          </div>
        </div>
      ` : ''}
    `;

    document.getElementById('report-month').addEventListener('change', (e) => {
      currentMonth = e.target.value;
      renderReports(main);
    });

    renderBreakdownChart(breakdown);
    renderTrendChart(trend);
    if (budgetComp.length) renderBudgetChart(budgetComp);
    if (paymentData.length) {
      renderPaymentDoughnut(paymentData);
      renderPaymentStackedBar(paymentData);
    }
  } catch (err) {
    main.innerHTML = `
      <div class="page-header"><h1 class="page-title">Reports</h1></div>
      <div class="card text-center py-12">
        <p class="text-gray-500 mb-4">Could not load report data.</p>
        <button class="btn btn-primary" onclick="location.reload()">Retry</button>
      </div>
    `;
  }
}

function renderBreakdownChart(data) {
  const canvas = document.getElementById('breakdown-chart');
  if (!canvas || !data.length) return;

  new Chart(canvas, {
    type: 'doughnut',
    data: {
      labels: data.map(d => d.name),
      datasets: [{
        data: data.map(d => d.amount),
        backgroundColor: data.map(d => d.color || '#9ca3af'),
        borderWidth: 2,
        borderColor: '#fff',
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '60%',
      plugins: {
        legend: { display: false },
        tooltip: { callbacks: { label: (ctx) => `${ctx.label}: ${formatCurrency(ctx.raw)}` } },
      },
    },
  });
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
        },
        {
          label: 'Expenses',
          data: trend.map(t => t.expenses),
          borderColor: '#ef4444',
          backgroundColor: 'rgba(239, 68, 68, 0.1)',
          fill: true,
          tension: 0.3,
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

function renderBudgetChart(data) {
  const canvas = document.getElementById('budget-chart');
  if (!canvas || !data.length) return;

  new Chart(canvas, {
    type: 'bar',
    data: {
      labels: data.map(d => d.name),
      datasets: [
        {
          label: 'Budgeted',
          data: data.map(d => d.budgeted),
          backgroundColor: 'rgba(99, 102, 241, 0.7)',
          borderRadius: 4,
        },
        {
          label: 'Spent',
          data: data.map(d => d.spent),
          backgroundColor: data.map(d => d.percentUsed > 100 ? 'rgba(239, 68, 68, 0.7)' : 'rgba(34, 197, 94, 0.7)'),
          borderRadius: 4,
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

const PAYMENT_COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#14b8a6'];

function renderPaymentDoughnut(data) {
  const canvas = document.getElementById('payment-doughnut-chart');
  const legendContainer = document.getElementById('payment-doughnut-legend');
  if (!canvas || !data.length) return;

  const byMethod = {};
  data.forEach(row => {
    byMethod[row.paymentMethod] = (byMethod[row.paymentMethod] || 0) + row.amount;
  });

  const entries = Object.entries(byMethod).sort((a, b) => b[1] - a[1]);
  const total = entries.reduce((sum, [, amt]) => sum + amt, 0);
  const labels = entries.map(e => e[0]);
  const values = entries.map(e => e[1]);
  const colors = labels.map((_, i) => PAYMENT_COLORS[i % PAYMENT_COLORS.length]);

  new Chart(canvas, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data: values,
        backgroundColor: colors,
        borderWidth: 2,
        borderColor: '#fff',
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '60%',
      plugins: {
        legend: { display: false },
        tooltip: { callbacks: { label: (ctx) => `${ctx.label}: ${formatCurrency(ctx.raw)}` } },
      },
    },
  });

  if (legendContainer) {
    legendContainer.innerHTML = entries.map(([method, amt], i) => {
      const pct = total > 0 ? ((amt / total) * 100).toFixed(1) : '0.0';
      return `
        <div class="flex items-center justify-between text-sm">
          <div class="flex items-center gap-2">
            <span class="color-dot" style="background-color: ${colors[i]}"></span>
            <span class="text-gray-700">${method}</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-gray-500">${pct}%</span>
            <span class="font-medium">${formatCurrency(amt)}</span>
          </div>
        </div>
      `;
    }).join('');
  }
}

function renderPaymentStackedBar(data) {
  const canvas = document.getElementById('payment-stacked-chart');
  if (!canvas || !data.length) return;

  const categories = [...new Set(data.map(d => d.categoryName))];
  const methods = [...new Set(data.map(d => d.paymentMethod))];

  const datasets = methods.map((method, i) => ({
    label: method,
    data: categories.map(cat => {
      const row = data.find(d => d.categoryName === cat && d.paymentMethod === method);
      return row ? row.amount : 0;
    }),
    backgroundColor: PAYMENT_COLORS[i % PAYMENT_COLORS.length],
    borderRadius: 4,
  }));

  new Chart(canvas, {
    type: 'bar',
    data: { labels: categories, datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'top', labels: { usePointStyle: true, padding: 20 } },
        tooltip: { callbacks: { label: (ctx) => `${ctx.dataset.label}: ${formatCurrency(ctx.raw)}` } },
      },
      scales: {
        x: { stacked: true },
        y: { stacked: true, beginAtZero: true, ticks: { callback: (v) => '$' + v.toLocaleString() } },
      },
    },
  });
}
