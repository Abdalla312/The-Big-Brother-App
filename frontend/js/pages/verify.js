import { api } from '../api.js';
import { showToast } from '../components/toast.js';

export async function renderVerify(params) {
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
        <h2 class="text-xl font-bold text-gray-900 mb-2">Invalid verification link</h2>
        <p class="text-sm text-gray-500 mb-6">The verification link is missing required parameters.</p>
        <a onclick="window.location.hash='#/login'" class="btn btn-primary cursor-pointer">Go to Login</a>
      </div>
    `;
    return;
  }

  container.innerHTML = `
    <div class="card text-center">
      <div class="flex justify-center mb-4">
        <svg class="animate-spin h-10 w-10 text-brand-600" viewBox="0 0 24 24" fill="none">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      </div>
      <h2 class="text-xl font-bold text-gray-900 mb-2">Verifying your email...</h2>
      <p class="text-sm text-gray-500">Please wait a moment.</p>
    </div>
  `;

  try {
    await api.get(`/auth/verify-email?userId=${userId}&token=${token}`);
    container.innerHTML = `
      <div class="card text-center">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-full bg-green-100 text-green-600">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
        </div>
        <h2 class="text-xl font-bold text-gray-900 mb-2">Email verified!</h2>
        <p class="text-sm text-gray-500 mb-6">Your email has been verified. You can now sign in.</p>
        <a onclick="window.location.hash='#/login'" class="btn btn-primary cursor-pointer">Go to Login</a>
      </div>
    `;
    showToast('Email verified successfully!', 'success');
  } catch (err) {
    container.innerHTML = `
      <div class="card text-center">
        <div class="flex justify-center mb-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-full bg-red-100 text-red-600">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
          </div>
        </div>
        <h2 class="text-xl font-bold text-gray-900 mb-2">Verification failed</h2>
        <p class="text-sm text-gray-500 mb-6">${err.message || 'The verification link may have expired or is invalid.'}</p>
        <a onclick="window.location.hash='#/login'" class="btn btn-primary cursor-pointer">Go to Login</a>
      </div>
    `;
  }
}
