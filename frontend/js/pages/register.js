import { api } from '../api.js';
import { setTokens, setUser } from '../auth.js';
import { navigate } from '../router.js';
import { showToast } from '../components/toast.js';

export function renderRegister() {
  const container = document.getElementById('auth-content');
  container.innerHTML = `
    <div class="card">
      <div class="text-center mb-8">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-600 text-white text-xl font-bold">B</div>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">Create account</h1>
        <p class="text-sm text-gray-500 mt-1">Start tracking your expenses</p>
      </div>
      <form id="register-form" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Name</label>
          <input type="text" id="reg-name" class="input" placeholder="John Doe" required>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input type="email" id="reg-email" class="input" placeholder="you@example.com" required autocomplete="email">
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Password</label>
          <input type="password" id="reg-password" class="input" placeholder="At least 8 characters" required minlength="8" autocomplete="new-password">
        </div>
        <div id="register-error" class="hidden text-sm text-red-600 bg-red-50 rounded-lg p-3"></div>
        <button type="submit" id="register-submit" class="btn btn-primary w-full">
          Create account
        </button>
      </form>
      <div class="mt-6 text-center text-sm text-gray-500">
        Already have an account?
        <a onclick="window.location.hash='#/login'" class="text-brand-600 font-medium hover:text-brand-700 cursor-pointer">Sign in</a>
      </div>
    </div>
  `;

  document.getElementById('register-form').addEventListener('submit', handleRegister);
}

async function handleRegister(e) {
  e.preventDefault();
  const btn = document.getElementById('register-submit');
  const errorDiv = document.getElementById('register-error');
  const name = document.getElementById('reg-name').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;

  btn.disabled = true;
  btn.innerHTML = '<svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg> Creating account...';
  errorDiv.classList.add('hidden');

  try {
    const res = await api.post('/auth/register', { name, email, password });
    const d = res.data;
    setTokens(d.accessToken, d.refreshToken);
    setUser({ name: d.name, email: d.email, verified: d.verified });
    showToast('Account created! Please check your email to verify.', 'success');
    navigate('#/dashboard');
  } catch (err) {
    errorDiv.textContent = err.message || 'Registration failed';
    errorDiv.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Create account';
  }
}
