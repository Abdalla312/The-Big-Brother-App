import { api } from '../api.js';
import { setTokens, setUser, clearTokens } from '../auth.js';
import { navigate } from '../router.js';
import { showToast } from '../components/toast.js';

export function renderLogin() {
  const container = document.getElementById('auth-content');
  const registered = new URLSearchParams(window.location.hash.slice(1).split('?')[1] || '').get('registered');
  
  container.innerHTML = `
    <div class="card">
      <div class="text-center mb-8">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-600 text-white text-xl font-bold">B</div>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">Welcome back</h1>
        <p class="text-sm text-gray-500 mt-1">Sign in to your account</p>
      </div>
      ${registered ? `
        <div class="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg text-green-800 text-sm">
          Account created! Please check your email to verify, then sign in.
        </div>
      ` : ''}
      <form id="login-form" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
          <input type="email" id="login-email" class="input" placeholder="you@example.com" required autocomplete="email">
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Password</label>
          <input type="password" id="login-password" class="input" placeholder="Enter your password" required autocomplete="current-password">
        </div>
        <div id="login-error" class="hidden text-sm text-red-600 bg-red-50 rounded-lg p-3"></div>
        <button type="submit" id="login-submit" class="btn btn-primary w-full">
          Sign in
        </button>
      </form>
      <div class="mt-6 text-center text-sm text-gray-500">
        Don't have an account?
        <a onclick="window.location.hash='#/register'" class="text-brand-600 font-medium hover:text-brand-700 cursor-pointer">Register</a>
      </div>
      <div class="mt-2 text-center text-sm">
        <a onclick="window.location.hash='#/resend-verification'" class="text-gray-400 hover:text-gray-600 cursor-pointer">Need to verify your email?</a>
      </div>
    </div>
  `;

  document.getElementById('login-form').addEventListener('submit', handleLogin);
}

async function handleLogin(e) {
  e.preventDefault();
  const btn = document.getElementById('login-submit');
  const errorDiv = document.getElementById('login-error');
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;

  // Clear any stale tokens from previous sessions
  clearTokens();

  btn.disabled = true;
  btn.innerHTML = '<svg class="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path></svg> Signing in...';
  errorDiv.classList.add('hidden');

  try {
    const res = await api.post('/auth/login', { email, password });
    const d = res.data;
    setTokens(d.accessToken, d.refreshToken);
    setUser({ name: d.name, email: d.email, verified: d.verified });
    showToast(`Welcome back, ${d.name}!`, 'success');
    navigate('#/dashboard');
  } catch (err) {
    errorDiv.textContent = err.message || 'Login failed';
    errorDiv.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Sign in';
  }
}
