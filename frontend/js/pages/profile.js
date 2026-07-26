import { api } from '../api.js';
import { getUser, setUser, clearTokens, logout } from '../auth.js';
import { formatDateTime } from '../utils.js';
import { showLoading } from '../components/loading.js';
import { openModal, closeModal, confirmDialog } from '../components/modal.js';
import { showToast } from '../components/toast.js';
import { navigate } from '../router.js';

export async function renderProfile(main) {
  showLoading(main);

  try {
    const res = await api.get('/users/me');
    const user = res?.data;
    if (user) setUser(user);

    main.innerHTML = `
      <div class="page-header">
        <h1 class="page-title">Profile</h1>
      </div>

      <div class="max-w-2xl space-y-6">
        <div class="card">
          <h3 class="text-sm font-semibold text-gray-700 mb-4">Account Information</h3>
          <div class="space-y-3">
            <div class="flex items-center gap-4">
              <div class="flex h-16 w-16 items-center justify-center rounded-full bg-brand-100 text-brand-700 text-2xl font-bold">
                ${user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
              </div>
              <div>
                <p class="text-lg font-semibold text-gray-900">${user?.name || '-'}</p>
                <p class="text-sm text-gray-500">${user?.email || '-'}</p>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-gray-100">
              <div>
                <p class="text-xs font-medium text-gray-500 uppercase">Role</p>
                <p class="text-sm font-medium text-gray-900 mt-1">${user?.role || 'USER'}</p>
              </div>
              <div>
                <p class="text-xs font-medium text-gray-500 uppercase">Member Since</p>
                <p class="text-sm font-medium text-gray-900 mt-1">${formatDateTime(user?.createdAt)}</p>
              </div>
            </div>
          </div>
          <div class="mt-4 pt-4 border-t border-gray-100">
            <button class="btn btn-secondary" id="edit-profile-btn">Edit Profile</button>
          </div>
        </div>

        <div class="card">
          <h3 class="text-sm font-semibold text-gray-700 mb-4">Change Password</h3>
          <form id="password-form" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Current Password</label>
              <input type="password" id="pw-current" class="input" required>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">New Password</label>
              <input type="password" id="pw-new" class="input" required minlength="8">
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
              <input type="password" id="pw-confirm" class="input" required minlength="8">
            </div>
            <button type="submit" class="btn btn-primary" id="pw-submit">Change Password</button>
          </form>
        </div>

        <div class="card border-red-200">
          <h3 class="text-sm font-semibold text-red-700 mb-2">Danger Zone</h3>
          <p class="text-sm text-gray-500 mb-4">Permanently delete your account and all associated data. This action cannot be undone.</p>
          <button class="btn btn-danger" id="delete-account-btn">Delete Account</button>
        </div>
      </div>
    `;

    document.getElementById('edit-profile-btn').addEventListener('click', () => openEditProfileModal(user));
    document.getElementById('password-form').addEventListener('submit', handlePasswordChange);
    document.getElementById('delete-account-btn').addEventListener('click', handleDeleteAccount);
  } catch {
    main.innerHTML = `<div class="card text-center py-12"><p class="text-gray-500">Failed to load profile.</p></div>`;
  }
}

function openEditProfileModal(user) {
  openModal(
    'Edit Profile',
    `<form id="edit-profile-form" class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Name</label>
        <input type="text" id="profile-name" class="input" maxlength="100" value="${user?.name || ''}">
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
        <input type="email" id="profile-email" class="input" value="${user?.email || ''}">
      </div>
    </form>`,
    `<button class="btn btn-secondary" id="profile-cancel">Cancel</button>
     <button class="btn btn-primary" id="profile-save">Save Changes</button>`
  );

  document.getElementById('profile-cancel').addEventListener('click', closeModal);
  document.getElementById('profile-save').addEventListener('click', async () => {
    const body = {};
    const name = document.getElementById('profile-name').value.trim();
    const email = document.getElementById('profile-email').value.trim();
    if (name) body.name = name;
    if (email) body.email = email;

    if (!body.name && !body.email) {
      showToast('Please enter a value to update', 'error');
      return;
    }

    try {
      const res = await api.patch('/users/me', body);
      if (res?.data) setUser(res.data);
      showToast('Profile updated', 'success');
      closeModal();
      const main = document.getElementById('app-content');
      await renderProfile(main);
    } catch {}
  });
}

async function handlePasswordChange(e) {
  e.preventDefault();
  const current = document.getElementById('pw-current').value;
  const newPw = document.getElementById('pw-new').value;
  const confirm = document.getElementById('pw-confirm').value;

  if (newPw !== confirm) {
    showToast('New passwords do not match', 'error');
    return;
  }
  if (newPw.length < 8) {
    showToast('New password must be at least 8 characters', 'error');
    return;
  }

  try {
    await api.patch('/users/me/password', { currentPassword: current, newPassword: newPw });
    showToast('Password changed. Please log in again.', 'success');
    clearTokens();
    logout();
  } catch {}
}

function handleDeleteAccount() {
  openModal(
    'Delete Account',
    `<p class="text-sm text-gray-600 mb-4">This will permanently delete your account and all data. Type your password to confirm.</p>
     <div>
       <label class="block text-sm font-medium text-gray-700 mb-1">Password</label>
       <input type="password" id="delete-password" class="input" required>
     </div>`,
    `<button class="btn btn-secondary" id="delete-cancel">Cancel</button>
     <button class="btn btn-danger" id="delete-confirm">Delete Account</button>`
  );

  document.getElementById('delete-cancel').addEventListener('click', closeModal);
  document.getElementById('delete-confirm').addEventListener('click', async () => {
    const password = document.getElementById('delete-password').value;
    if (!password) { showToast('Password is required', 'error'); return; }

    try {
      await api.delete('/users/me', { password });
      showToast('Account deleted', 'success');
      clearTokens();
      navigate('#/login');
    } catch {}
  });
}
