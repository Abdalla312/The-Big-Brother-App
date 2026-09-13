import { api } from '../api.js';
import { showToast } from '../components/toast.js';

export function renderForgotPassword() {
  const container = document.getElementById('auth-content');
  container.innerHTML = `
    <div class="card">
      <div class="text-center mb-8">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-600 text-white text-xl font-bold">B</div>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">Forgot password</h1>
        <p class="text-sm text-gray-500 mt-1">Enter your email and we'll send you a reset link</p>
      </div>
      <form id="forgot-form" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input type="email" id="forgot-email" class="input" placeholder="you@example.com" required autocomplete="email">
        </div>
        <div id="forgot-error" class="hidden text-sm text-red-600 bg-red-50 rounded-lg p-3"></div>
        <div id="forgot-success" class="hidden text-sm text-green-600 bg-green-50 rounded-lg p-3"></div>
        <button type="submit" id="forgot-submit" class="btn btn-primary w-full">
          Send reset link
        </button>
      </form>
      <div class="mt-6 text-center text-sm text-gray-500">
        <a onclick="window.location.hash='#/login'" class="text-brand-600 font-medium hover:text-brand-700 cursor-pointer">Back to Login</a>
      </div>
    </div>
  `;

  document.getElementById('forgot-form').addEventListener('submit', handleForgotPassword);
}

async function handleForgotPassword(e) {
  e.preventDefault();
  const btn = document.getElementById('forgot-submit');
  const errorDiv = document.getElementById('forgot-error');
  const successDiv = document.getElementById('forgot-success');
  const email = document.getElementById('forgot-email').value.trim();

  btn.disabled = true;
  btn.innerHTML = '<svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg> Sending...';
  errorDiv.classList.add('hidden');
  successDiv.classList.add('hidden');

  try {
    await api.post('/auth/forgot-password', { email });
    successDiv.textContent = 'If an account with that email exists, a password reset link has been sent.';
    successDiv.classList.remove('hidden');
    document.getElementById('forgot-email').value = '';
    showToast('Reset link sent if the email is registered', 'success');
  } catch (err) {
    errorDiv.textContent = err.message || 'Failed to send reset link.';
    errorDiv.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.innerHTML = 'Send reset link';
  }
}