import { api } from '../api.js';
import { showToast } from '../components/toast.js';

export function renderResendVerification() {
  const container = document.getElementById('auth-content');
  container.innerHTML = `
    <div class="card">
      <div class="text-center mb-8">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-600 text-white text-xl font-bold">B</div>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">Resend verification</h1>
        <p class="text-sm text-gray-500 mt-1">Enter your email to receive a new verification link</p>
      </div>
      <form id="resend-form" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input type="email" id="resend-email" class="input" placeholder="you@example.com" required autocomplete="email">
        </div>
        <div id="resend-error" class="hidden text-sm text-red-600 bg-red-50 rounded-lg p-3"></div>
        <div id="resend-success" class="hidden text-sm text-green-600 bg-green-50 rounded-lg p-3"></div>
        <button type="submit" id="resend-submit" class="btn btn-primary w-full">
          Send verification email
        </button>
      </form>
      <div class="mt-6 text-center text-sm text-gray-500">
        <a onclick="window.location.hash='#/login'" class="text-brand-600 font-medium hover:text-brand-700 cursor-pointer">Back to Login</a>
      </div>
    </div>
  `;

  document.getElementById('resend-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('resend-submit');
    const errorDiv = document.getElementById('resend-error');
    const successDiv = document.getElementById('resend-success');
    const email = document.getElementById('resend-email').value.trim();

    btn.disabled = true;
    errorDiv.classList.add('hidden');
    successDiv.classList.add('hidden');

    try {
      await api.post('/auth/resend-verification', { email });
      successDiv.textContent = 'If an account with that email exists, a verification link has been sent.';
      successDiv.classList.remove('hidden');
    } catch (err) {
      errorDiv.textContent = err.message || 'Failed to send verification email.';
      errorDiv.classList.remove('hidden');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Send verification email';
    }
  });
}
