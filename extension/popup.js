const MAX_HISTORY = 8;

const pathInput      = document.getElementById('folderPath');
const openBtn        = document.getElementById('openBtn');
const browseBtn      = document.getElementById('browseBtn');
const statusEl       = document.getElementById('status');
const historySection = document.getElementById('historySection');
const historyList    = document.getElementById('historyList');

function loadHistory() {
  return JSON.parse(localStorage.getItem('folderHistory') || '[]');
}

function saveHistory(path) {
  let history = loadHistory().filter(p => p !== path);
  history.unshift(path);
  history = history.slice(0, MAX_HISTORY);
  localStorage.setItem('folderHistory', JSON.stringify(history));
}

function removeFromHistory(path) {
  const history = loadHistory().filter(p => p !== path);
  localStorage.setItem('folderHistory', JSON.stringify(history));
  renderHistory();
}

function renderHistory() {
  const history = loadHistory();
  if (history.length === 0) {
    historySection.style.display = 'none';
    return;
  }
  historySection.style.display = 'block';
  historyList.innerHTML = '';
  history.forEach(path => {
    const li = document.createElement('li');
    li.title = path;

    const span = document.createElement('span');
    span.className = 'history-path';
    span.textContent = path;
    span.addEventListener('click', () => {
      pathInput.value = path;
      openFolder(path);
    });

    const remove = document.createElement('span');
    remove.className = 'history-remove';
    remove.textContent = '×';
    remove.title = 'Remove';
    remove.addEventListener('click', (e) => {
      e.stopPropagation();
      removeFromHistory(path);
    });

    li.appendChild(span);
    li.appendChild(remove);
    historyList.appendChild(li);
  });
}

function showStatus(message, type) {
  statusEl.textContent = message;
  statusEl.className = type;
  setTimeout(() => { statusEl.className = ''; }, 3000);
}

function openFolder(path) {
  if (!path.trim()) {
    showStatus('Please enter a folder path.', 'error');
    return;
  }

  openBtn.disabled = true;

  chrome.runtime.sendMessage({ action: 'openFolder', path: path.trim() }, (response) => {
    openBtn.disabled = false;
    if (chrome.runtime.lastError) {
      showStatus('Native host not connected. Run install.bat first.', 'error');
      return;
    }
    if (response && response.success) {
      showStatus('Folder opened successfully.', 'success');
      saveHistory(path.trim());
      renderHistory();
    } else {
      showStatus(response?.message || 'Failed to open folder.', 'error');
    }
  });
}

browseBtn.addEventListener('click', () => {
  browseBtn.disabled = true;
  browseBtn.textContent = '...';

  chrome.runtime.sendMessage({ action: 'browse' }, (response) => {
    browseBtn.disabled = false;
    browseBtn.textContent = 'Browse';

    if (chrome.runtime.lastError) {
      showStatus('Native host not connected. Run install.bat first.', 'error');
      return;
    }
    if (response && response.success && response.path) {
      pathInput.value = response.path;
    } else if (response && response.message && response.message !== 'cancelled') {
      showStatus(response.message, 'error');
    }
  });
});

openBtn.addEventListener('click', () => openFolder(pathInput.value));

pathInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') openFolder(pathInput.value);
});

renderHistory();
