// ============================================================================
// Neodymium Aura Dashboard - Core, Theme, Toast & Layout Engine
// ============================================================================

// Global State Variables
var clientId = 'client_' + Math.random().toString(36).substring(2, 9);
var lastActiveView = 'dashboardView';
var disconnected = false;

var historyCached = [];
var currentReportId = null;
var currentTestFile = null;

var activeEditingFile = null;
var selectedDatasets = [];
var currentFilesListCached = [];

var isRunning = false;
var consoleOpened = false;

var logFilterAiOnly = true;
var logFilterErrorsOnly = false;
var hasShownStartMessage = false;

var activeRunStats = {
    running: false,
    startTime: null,
    total: 0,
    passed: 0,
    failed: 0,
    skipped: 0,
    activeFile: null,
    activeTestId: null,
    tests: []
};
var liveCompletedFiles = new Set();
var liveLastActiveFile = null;

var chatSessions = [];
var currentSessionId = null;
var conversationHistory = [];

var historyNavState = 1;
var savedRunsWidth = 280;
var savedTestsWidth = 240;

// Bind to window for direct Selenium/Selenide executeScript access
window.clientId = clientId;
window.lastActiveView = lastActiveView;
window.historyCached = historyCached;
window.currentReportId = currentReportId;
window.currentTestFile = currentTestFile;
window.activeEditingFile = activeEditingFile;
window.selectedDatasets = selectedDatasets;
window.currentFilesListCached = currentFilesListCached;
window.isRunning = isRunning;
window.consoleOpened = consoleOpened;
window.logFilterAiOnly = logFilterAiOnly;
window.logFilterErrorsOnly = logFilterErrorsOnly;
window.activeRunStats = activeRunStats;
window.liveCompletedFiles = liveCompletedFiles;
window.historyNavState = historyNavState;

// Helper getters for dynamic DOM elements
function getEditorFileName() {
    return document.getElementById('editorFileName');
}
window.getEditorFileName = getEditorFileName;

function getEditorContent() {
    return document.getElementById('editorContent');
}
window.getEditorContent = getEditorContent;

// Toast Notification System
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    let iconName = 'info';
    if (type === 'success') iconName = 'check_circle';
    else if (type === 'error') iconName = 'warning';
    toast.innerHTML = `<span class="material-symbols-outlined">${iconName}</span> <span>${message}</span>`;
    toast.style.cursor = 'pointer';
    toast.onclick = () => toast.remove();
    container.appendChild(toast);
    const duration = type === 'error' ? 10000 : 3000;
    setTimeout(() => {
        if (toast.parentNode) {
            toast.remove();
        }
    }, duration);
}
window.showToast = showToast;

function showWarning(msg) {
    const banner = document.getElementById('warningBanner');
    const bannerText = document.getElementById('warningBannerText');
    if (banner && bannerText) {
        bannerText.innerHTML = msg;
        banner.style.display = 'block';
        setTimeout(() => {
            banner.style.display = 'none';
        }, 5000);
    }
}
window.showWarning = showWarning;

// Theme Logic
function syncThemeToIframes(theme) {
    const iframes = document.querySelectorAll('iframe');
    iframes.forEach(iframe => {
        try {
            if (iframe.contentWindow) {
                iframe.contentWindow.postMessage({ type: 'aura-theme-change', theme: theme }, '*');
                if (typeof iframe.contentWindow.applyTheme === 'function') {
                    iframe.contentWindow.applyTheme(theme);
                }
            }
        } catch (e) {
            // Ignore potential cross-origin restrictions
        }
    });
}
window.syncThemeToIframes = syncThemeToIframes;

function initTheme() {
    const savedTheme = localStorage.getItem('aura_theme') || 'system';
    applyTheme(savedTheme);

    window.addEventListener('load', function () {
        syncThemeToIframes(savedTheme);
    });

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('iframe').forEach(iframe => {
            iframe.addEventListener('load', function () {
                const currentTheme = localStorage.getItem('aura_theme') || 'system';
                syncThemeToIframes(currentTheme);
            });
        });
    });
}
window.initTheme = initTheme;

function applyTheme(theme) {
    const root = document.documentElement;
    const body = document.body;
    if (theme === 'dark') {
        root.classList.add('force-dark');
        root.classList.remove('force-light');
        if (body) {
            body.classList.add('force-dark');
            body.classList.remove('force-light');
        }
    } else if (theme === 'light') {
        root.classList.add('force-light');
        root.classList.remove('force-dark');
        if (body) {
            body.classList.add('force-light');
            body.classList.remove('force-dark');
        }
    } else {
        root.classList.remove('force-dark', 'force-light');
        if (body) {
            body.classList.remove('force-dark', 'force-light');
        }
    }

    localStorage.setItem('aura_theme', theme);
    syncThemeToIframes(theme);
}
window.applyTheme = applyTheme;

function changeTheme(theme) {
    localStorage.setItem('aura_theme', theme);
    applyTheme(theme);
    fetch('/api/theme?theme=' + encodeURIComponent(theme), { method: 'POST' }).catch(() => {});
}
window.changeTheme = changeTheme;

// Layout Resizers (Sidebar & Console)
let isResizingH = false;
let isResizingV = false;

document.addEventListener('DOMContentLoaded', function() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('view') === 'history' || window.location.pathname.endsWith('/history')) {
        showView('reportViewContainer');
    }

    const sidebarResizer = document.getElementById('sidebarResizer');
    if (sidebarResizer) {
        sidebarResizer.addEventListener('mousedown', function (e) {
            isResizingH = true;
            document.body.style.cursor = 'ew-resize';
            e.preventDefault();
        });
    }

    const consoleResizer = document.getElementById('consoleResizer');
    if (consoleResizer) {
        consoleResizer.addEventListener('mousedown', function (e) {
            isResizingV = true;
            document.body.style.cursor = 'ns-resize';
            e.preventDefault();
        });
    }
});

document.addEventListener('mousemove', function (e) {
    if (isResizingH) {
        const workspace = document.querySelector('.workspace-layout');
        if (workspace) {
            const workspaceRect = workspace.getBoundingClientRect();
            const newWidth = workspaceRect.right - e.clientX - 16;
            if (newWidth > 200 && newWidth < workspaceRect.width * 0.6) {
                const rightArea = document.getElementById('rightArea');
                if (rightArea) rightArea.style.width = newWidth + 'px';
            }
        }
    }
    if (isResizingV) {
        const consolePanel = document.getElementById('consolePanel');
        if (consolePanel && consolePanel.style.display !== 'none') {
            const workspace = document.querySelector('.workspace-layout');
            if (workspace) {
                const workspaceRect = workspace.getBoundingClientRect();
                const newHeight = workspaceRect.bottom - e.clientY - 16;
                if (newHeight > 100 && newHeight < workspaceRect.height * 0.8) {
                    consolePanel.style.height = newHeight + 'px';
                }
            }
        }
    }
});

document.addEventListener('mouseup', function (e) {
    if (isResizingH || isResizingV) {
        isResizingH = false;
        isResizingV = false;
        document.body.style.cursor = 'default';
    }
});

function switchState(stateName) {
    const container = document.getElementById('auraTestManagerWorkspace') || document.getElementById('dashboardView');
    if (container) {
        container.querySelectorAll('.view-state').forEach(el => el.classList.remove('active'));
    } else {
        document.querySelectorAll('#state-selection, #state-editor').forEach(el => el.classList.remove('active'));
    }
    const stateEl = document.getElementById('state-' + stateName);
    if (stateEl) stateEl.classList.add('active');
}
window.switchState = switchState;

function showView(viewId) {
    const dashView = document.getElementById('dashboardView');
    const icView = document.getElementById('interactiveConsoleView');

    if (dashView) dashView.style.display = 'none';
    if (icView) icView.style.display = 'none';

    const targetView = document.getElementById(viewId);
    if (targetView) targetView.style.display = 'flex';

    lastActiveView = viewId;
    window.lastActiveView = viewId;
}
window.showView = showView;

function openInteractiveConsoleView(url) {
    const iframe = document.getElementById('interactiveConsoleIframe');
    if (iframe) {
        iframe.src = url || '/interactive_console.html';
    }
    showView('interactiveConsoleView');
}
window.openInteractiveConsoleView = openInteractiveConsoleView;

function closeInteractiveConsoleView() {
    const iframe = document.getElementById('interactiveConsoleIframe');
    if (iframe) {
        iframe.src = 'about:blank';
    }
    showView('dashboardView');
}
window.closeInteractiveConsoleView = closeInteractiveConsoleView;

function onEditorPanelSwapped() {
    const container = document.getElementById('auraTestManagerWorkspace') || document.getElementById('dashboardView');
    if (!container) return;

    const fileSpan = document.getElementById('editorFileName');
    const dataFile = fileSpan ? fileSpan.getAttribute('data-file') : null;
    const textFile = fileSpan && fileSpan.textContent ? fileSpan.textContent.trim() : null;
    const filename = (dataFile && dataFile.trim() !== '') ? dataFile.trim() : textFile;

    if (filename && filename !== '' && filename !== 'test.yaml') {
        activeEditingFile = filename;
        window.activeEditingFile = activeEditingFile;
    } else {
        activeEditingFile = null;
        window.activeEditingFile = null;
    }
    if (typeof updateCenterLayout === 'function') updateCenterLayout();
}
window.onEditorPanelSwapped = onEditorPanelSwapped;

function init() {
    if (window.location.search.includes('test=true')) {
        window.confirm = () => true;
        window.prompt = (msg, defaultText) => defaultText || "Mocked Input";
    }

    const isTestManagerPage = document.getElementById('auraTestManagerWorkspace') !== null ||
                              document.getElementById('dashboardView') !== null;

    if (isTestManagerPage) {
        if (typeof scrollToBottom === 'function') scrollToBottom();
        if (typeof loadFiles === 'function') loadFiles();
        if (typeof startPolling === 'function') startPolling();
        if (typeof initResizers === 'function') initResizers();
    }

    let savedYamlFileListScrollTop = 0;

    document.addEventListener('htmx:beforeSwap', function(evt) {
        if (evt.detail && evt.detail.target && evt.detail.target.id === 'yamlFileList') {
            const isSearch = evt.detail.elt && evt.detail.elt.id === 'testSearchInput';
            if (isSearch) {
                savedYamlFileListScrollTop = 0;
            } else {
                savedYamlFileListScrollTop = evt.detail.target.scrollTop;
            }
        }
    });

    document.addEventListener('htmx:oobAfterSwap', function(evt) {
        onEditorPanelSwapped();
        if (typeof syncStateFromQueueContainer === 'function') syncStateFromQueueContainer();
        if (typeof syncCheckboxesFromState === 'function') syncCheckboxesFromState();
    });

    document.addEventListener('htmx:afterSwap', function(evt) {
        onEditorPanelSwapped();
        if (evt.detail && evt.detail.target) {
            if (evt.detail.target.id === 'yamlFileList') {
                const scrollPos = savedYamlFileListScrollTop;
                evt.detail.target.scrollTop = scrollPos;
                requestAnimationFrame(() => {
                    if (evt.detail.target) {
                        evt.detail.target.scrollTop = scrollPos;
                    }
                });
                if (typeof syncCheckboxesFromState === 'function') syncCheckboxesFromState();
            } else if (evt.detail.target.id === 'queueListContainer' || evt.detail.target.id === 'runControls') {
                if (typeof syncStateFromQueueContainer === 'function') syncStateFromQueueContainer();
            }
        }
    });

    document.addEventListener('htmx:afterSettle', function(evt) {
        onEditorPanelSwapped();
    });

    document.addEventListener('htmx:responseError', function(evt) {
        const xhr = evt.detail.xhr;
        let errorMsg = 'Server returned HTTP ' + xhr.status;
        try {
            const data = JSON.parse(xhr.responseText);
            if (data && data.error) {
                errorMsg = data.error;
            }
        } catch(e) {}
        
        showWarning(errorMsg);
    });

    if (typeof syncStateFromQueueContainer === 'function') syncStateFromQueueContainer();

    document.body.addEventListener('aiAction', async function(evt) {
        const data = evt.detail;
        if (!data) return;

        if (data.action === 'select_tests') {
            if (data.selectedDatasets) {
                selectedDatasets = data.selectedDatasets;
                window.selectedDatasets = selectedDatasets;
            } else if (data.files) {
                selectedDatasets = [];
                data.files.forEach(file => {
                    const fileDto = currentFilesListCached.find(f => f.file === file);
                    if (fileDto && fileDto.datasets) {
                        fileDto.datasets.forEach(d => {
                            selectedDatasets.push({ file, id: d.id });
                        });
                    }
                });
                window.selectedDatasets = selectedDatasets;
            }
            if (typeof loadFiles === 'function') await loadFiles();
            if (typeof updateQueueList === 'function') updateQueueList();
        } else if (data.action === 'edit_or_create_test' && data.filename && data.content) {
            if (typeof loadFiles === 'function') await loadFiles();
            activeEditingFile = data.filename;
            window.activeEditingFile = activeEditingFile;
            if (getEditorFileName()) getEditorFileName().innerText = data.filename;
            if (getEditorContent()) getEditorContent().value = data.content;
            if (typeof updateCenterLayout === 'function') updateCenterLayout();
            if (getEditorContent()) getEditorContent().focus();
        } else if (data.action === 'select_browser' || data.selectedBrowserProfiles) {
            if (window.htmx) {
                window.htmx.ajax('GET', '/', {
                    target: '#configPanel',
                    select: '#configPanel',
                    swap: 'outerHTML'
                });
                window.htmx.ajax('GET', '/', {
                    target: '#queueListContainer',
                    select: '#queueListContainer',
                    swap: 'innerHTML'
                });
            }
        }
    });

    document.body.addEventListener('fileSaved', function(evt) {
        const detail = evt.detail;
        if (detail && detail.file) {
            showToast(`💾 ${detail.file} successfully saved to disk!`, "success");
        }
    });

    document.body.addEventListener('htmx:afterSwap', function(evt) {
        if (evt.detail.target.id === 'chatMessages' || evt.detail.target.id === 'chatContainer') {
            if (typeof scrollToBottom === 'function') scrollToBottom();
        }
    });

    document.body.addEventListener('htmx:responseError', function(evt) {
        const thinkingBubbles = document.querySelectorAll('.thinking-bubble');
        thinkingBubbles.forEach(el => el.remove());
    });
}
window.init = init;

// Modal Controls Event Delegation
document.addEventListener('click', function(e) {
    const openBtn = e.target.closest('#openModalBtn');
    if (openBtn) {
        const modal = document.getElementById('createTestModal');
        const nameInput = document.getElementById('newTestName');
        if (modal) modal.style.display = 'flex';
        if (nameInput) {
            nameInput.value = '';
            nameInput.focus();
        }
    }
    const closeBtn = e.target.closest('#closeModalBtn');
    if (closeBtn) {
        const modal = document.getElementById('createTestModal');
        if (modal) modal.style.display = 'none';
    }
});

document.addEventListener('keydown', function(event) {
    const newTestInput = document.getElementById('newTestName');
    if (newTestInput && event.target === newTestInput && event.key === 'Enter') {
        if (typeof submitCreateTest === 'function') {
            submitCreateTest();
        }
    }
});

initTheme();
document.addEventListener('DOMContentLoaded', function() {
    init();
});
