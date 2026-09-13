import { api } from '../api.js';
import { showToast } from '../components/toast.js';

export function renderResetPassword(params) {
  const container = document.getElementById('auth-content');
  const userId = params.get('userId');
  const token = params.get('token');

  if (!userId || !token) {
    container.innerHTML = `
      <div class="card text-center">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-full bg-red-100 text-red-600">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
          </div>
        </div>
        <h2 class="text-xl font-bold text-gray-900 mb-2">Invalid reset link</h2>
        <p class="text-sm text-gray-500 mb-6">The password reset link is missing required parameters.</p>
        <a onclick="window.location.hash='#/login'" class="btn btn-primary cursor-pointer">Go to Login</a>
      </div>
    `;
    return;
  }

  container.innerHTML = `
    <div class="card">
      <div class="text-center mb-8">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-600 text-white text-xl font-bold">B</div>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">Reset password</h1>
        <p class="text-sm text-gray-500 mt-1">Choose a new password for your account</p>
      </div>
      <form id="reset-form" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">New Password</label>
          <input type="password" id="reset-password" class="input" placeholder="At least 8 characters" required minlength="8" autocomplete="new-password">
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
          <input type="password" id="reset-confirm" class="input" placeholder="Confirm new password" required minlength="8" autocomplete="new-password">
        </div>
        <div id="reset-error" class="hidden text-sm text-red-600 bg-red-50 rounded-lg p-3"></div>
        <button type="submit" id="reset-submit" class="btn btn-primary w-full">
          Reset password
        </button>
      </form>
      <div class="mt-6 text-center text-sm text-gray-500">
        <a onclick="window.location.hash='#/login'" class="text-brand-600 font-medium hover:text-brand-700 cursor-pointer">Back to Login</a>
      </div>
    </div>
  `;

  document.getElementById('reset-form').addEventListener('submit', (e) => handleReset(e, userId, token));
}

async function handleReset(e, userId, token) {
  e.preventDefault();
  const btn = document.getElementById('reset-submit');
  const errorDiv = document.getElementById('reset-error');
  const password = document.getElementById('reset-password').value;
  const confirm = document.getElementById('reset-confirm').value;

  if (password !== confirm) {
    errorDiv.textContent = 'Passwords do not match';
    errorDiv.classList.remove('hidden');
    return;
  }

  btn.disabled = true;
  btn.innerHTML = '<svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg> Resetting...';
  errorDiv.classList.add('hidden');

  try {
    await api.post('/auth/reset-password', { token, userId, newPassword: password });
    const container = document.getElementById('auth-content');
    container.innerHTML = `
      <div class="card text-center">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-full bg-green-100 text-green-600">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
        </div>
        <h2 class="text-xl font-bold text-gray-900 mb-2">Password reset!</h2>
        <p class="text-sm text-gray-500 mb-6">Your password has been changed. You can now sign in with your new password.</p>
        <a onclick="window.location.hash='#/login'" class="btn btn-primary cursor-pointer">Go to Login</a>
      </div>
    `;
    showToast('Password has been reset successfully!', 'success');
  } catch (err) {
    errorDiv.textContent = err.message || 'Failed to reset password. The link may have expired.';
    errorDiv.classList.remove('hidden');
    btn.disabled = false;
    btn.innerHTML = 'Reset password';
  }
}