let activeModal = null;

export function openModal(title, bodyHtml, footerHtml = '') {
  closeModal();
  const root = document.getElementById('modal-root');

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) closeModal();
  });

  overlay.innerHTML = `
    <div class="modal-panel">
      <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
        <h3 class="text-lg font-semibold text-gray-900">${title}</h3>
        <button id="modal-close-btn" class="btn btn-ghost btn-sm p-1 rounded-lg">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <div class="px-6 py-4">${bodyHtml}</div>
      ${footerHtml ? `<div class="flex items-center justify-end gap-3 border-t border-gray-200 px-6 py-4">${footerHtml}</div>` : ''}
    </div>
  `;

  root.appendChild(overlay);
  activeModal = overlay;

  overlay.querySelector('#modal-close-btn').addEventListener('click', closeModal);

  const firstInput = overlay.querySelector('input, select, textarea');
  if (firstInput) setTimeout(() => firstInput.focus(), 100);

  document.addEventListener('keydown', handleEsc);
}

export function closeModal() {
  if (activeModal) {
    activeModal.remove();
    activeModal = null;
    document.removeEventListener('keydown', handleEsc);
  }
}

function handleEsc(e) {
  if (e.key === 'Escape') closeModal();
}

export function confirmDialog(message, onConfirm, danger = false) {
  openModal(
    'Confirm',
    `<p class="text-sm text-gray-600">${message}</p>`,
    `
      <button class="btn btn-secondary" id="confirm-cancel">Cancel</button>
      <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" id="confirm-ok">${danger ? 'Delete' : 'Confirm'}</button>
    `
  );

  document.getElementById('confirm-cancel').addEventListener('click', closeModal);
  document.getElementById('confirm-ok').addEventListener('click', () => {
    closeModal();
    onConfirm();
  });
}
