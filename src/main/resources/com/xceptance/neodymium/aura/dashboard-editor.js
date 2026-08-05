// ============================================================================
// Neodymium Aura Dashboard - Test & Dataset Editor & Queue Control
// ============================================================================

function isDatasetSelected(file, id) {
    if (!file || id == null) return false;
    const strFile = String(file);
    const strId = String(id);
    return selectedDatasets.some(d => String(d.file) === strFile && String(d.id) === strId);
}
window.isDatasetSelected = isDatasetSelected;

function isAllDatasetsChecked(fileDto) {
    if (!fileDto || !fileDto.datasets || fileDto.datasets.length === 0) return false;
    return fileDto.datasets.every(d => isDatasetSelected(fileDto.file, d.id));
}
window.isAllDatasetsChecked = isAllDatasetsChecked;

function toggleSelectDataset(file, id, checked) {
    htmx.ajax('POST', '/api/queue/toggle?file=' + encodeURIComponent(file) + '&id=' + encodeURIComponent(id), { target: '#queueListContainer', swap: 'innerHTML' });
}
window.toggleSelectDataset = toggleSelectDataset;

function toggleSelectAllDatasets(file, checked) {
    htmx.ajax('POST', '/api/queue/toggleAll?file=' + encodeURIComponent(file) + '&checked=' + checked, { target: '#queueListContainer', swap: 'innerHTML' });
}
window.toggleSelectAllDatasets = toggleSelectAllDatasets;

function toggleFileCheckboxesInstant(fileCb) {
    if (!fileCb) return;
    const container = fileCb.closest('.file-container');
    if (!container) return;
    const datasetCbs = container.querySelectorAll('.dataset-select-cb');
    datasetCbs.forEach(cb => {
        cb.checked = fileCb.checked;
    });
}
window.toggleFileCheckboxesInstant = toggleFileCheckboxesInstant;

function toggleDatasetCheckboxInstant(datasetCb) {
    if (!datasetCb) return;
    const container = datasetCb.closest('.file-container');
    if (!container) return;
    const fileCb = container.querySelector('.file-select-cb');
    if (!fileCb) return;
    const datasetCbs = container.querySelectorAll('.dataset-select-cb');
    let allChecked = true;
    let anyChecked = false;
    datasetCbs.forEach(cb => {
        if (cb.checked) anyChecked = true;
        else allChecked = false;
    });
    fileCb.checked = allChecked;
    fileCb.indeterminate = (!allChecked && anyChecked);
}
window.toggleDatasetCheckboxInstant = toggleDatasetCheckboxInstant;

function toggleExpandFile(file) {
    const list = document.getElementById('datasets-' + file);
    if (list) {
        const isHidden = list.style.display === 'none';
        list.style.display = isHidden ? 'flex' : 'none';
        
        const escapedFile = file.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
        const listItem = document.querySelector(`.list-item[data-file="${escapedFile}"]`);
        if (listItem) {
            const icon = listItem.querySelector('.item-main i');
            if (icon) {
                if (isHidden) {
                    icon.classList.remove('fa-chevron-right');
                    icon.classList.add('fa-chevron-down');
                } else {
                    icon.classList.remove('fa-chevron-down');
                    icon.classList.add('fa-chevron-right');
                }
            }
        }
    }
    fetch('/api/files/toggle?file=' + encodeURIComponent(file), { method: 'POST' });
}
window.toggleExpandFile = toggleExpandFile;

function loadFiles() {
    return new Promise(async (resolve, reject) => {
        let resolved = false;
        const done = () => {
            if (!resolved) {
                resolved = true;
                resolve();
            }
        };
        setTimeout(done, 1000);
        try {
            const res = await fetch('/api/files');
            currentFilesListCached = await res.json();
            window.currentFilesListCached = currentFilesListCached;
            if (window.initializedAlready) {
                const listener = function(evt) {
                    if (evt.detail.target && evt.detail.target.id === 'yamlFileList') {
                        document.removeEventListener('htmx:afterSwap', listener);
                        done();
                    }
                };
                document.addEventListener('htmx:afterSwap', listener);
                htmx.ajax('GET', '/api/files/list', { target: '#yamlFileList', swap: 'outerHTML' });
            } else {
                window.initializedAlready = true;
                syncCheckboxesFromState();
                done();
            }
        } catch (e) {
            console.error("Failed to load files", e);
            done();
        }
    });
}
window.loadFiles = loadFiles;

function syncCheckboxesFromState() {
    const fileContainers = document.querySelectorAll('.file-container');
    
    if (fileContainers.length > 0) {
        fileContainers.forEach(container => {
            const fileCb = container.querySelector('.file-select-cb');
            if (!fileCb) return;
            const file = fileCb.getAttribute('data-file');
            if (!file) return;

            const datasetCbs = container.querySelectorAll('.dataset-select-cb');
            if (datasetCbs.length > 0) {
                let allChecked = true;
                datasetCbs.forEach(cb => {
                    const id = cb.getAttribute('data-id');
                    const selected = isDatasetSelected(file, id);
                    cb.checked = selected;
                    if (!selected) allChecked = false;
                });
                fileCb.checked = allChecked;
            } else {
                // If dataset list is collapsed/not rendered in DOM, check if any dataset for file is in queue
                const isFileInQueue = selectedDatasets.some(d => String(d.file) === String(file));
                fileCb.checked = isFileInQueue;
            }
        });
    } else {
        // Fallback for standalone checkboxes
        document.querySelectorAll('.dataset-select-cb').forEach(cb => {
            const file = cb.getAttribute('data-file');
            const id = cb.getAttribute('data-id');
            cb.checked = isDatasetSelected(file, id);
        });
        document.querySelectorAll('.file-select-cb').forEach(cb => {
            const file = cb.getAttribute('data-file');
            cb.checked = selectedDatasets.some(d => String(d.file) === String(file));
        });
    }
}
window.syncCheckboxesFromState = syncCheckboxesFromState;

function loadFilesList(files) {
    syncCheckboxesFromState();
    updateQueueList();
}
window.loadFilesList = loadFilesList;

async function submitCreateTest() {
    const nameInput = document.getElementById('newTestName');
    if (!nameInput) return;
    const name = nameInput.value.trim();
    if (!name) return;
    try {
        const res = await fetch('/api/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        const data = await res.json();
        if (data.error) {
            showToast("Error: " + data.error, "error");
        } else {
            const modal = document.getElementById('createTestModal');
            if (modal) modal.style.display = 'none';
            await loadFiles();
            await openYamlEditor(data.file);
        }
    } catch (e) {
        console.error("Failed to create test", e);
    }
}
window.submitCreateTest = submitCreateTest;

var consoleExpanded = false;

function toggleConsoleSize() {
    consoleExpanded = !consoleExpanded;
    const icon = document.getElementById('consoleSizeIcon');
    const btn = document.getElementById('toggleConsoleSizeBtn');
    if (consoleExpanded) {
        if (icon) icon.className = 'fa-solid fa-chevron-down';
        if (btn) btn.innerHTML = '<i class="fa-solid fa-chevron-down" id="consoleSizeIcon" aria-hidden="true"></i> Shrink';
    } else {
        if (icon) icon.className = 'fa-solid fa-chevron-up';
        if (btn) btn.innerHTML = '<i class="fa-solid fa-chevron-up" id="consoleSizeIcon" aria-hidden="true"></i> Expand';
    }
    updateCenterLayout();
}
window.toggleConsoleSize = toggleConsoleSize;

function updateCenterLayout() {
    const hasEdit = (activeEditingFile !== null);
    const hasConsole = consoleOpened || isRunning;

    const runCurrentBtn = document.getElementById('runCurrentTestBtn');
    if (runCurrentBtn) {
        runCurrentBtn.style.display = hasEdit ? 'flex' : 'none';
    }

    if (hasEdit) {
        document.body.classList.add('workspace-split-mode');
        if (typeof switchState === 'function') switchState('editor');
    } else {
        document.body.classList.remove('workspace-split-mode');
        if (typeof switchState === 'function') switchState('selection');
    }

    const consolePanel = document.getElementById('consolePanel');
    const consoleResizer = document.getElementById('consoleResizer');
    const closeConsoleBtn = document.getElementById('closeConsoleBtn');

    if (closeConsoleBtn) {
        closeConsoleBtn.style.display = isRunning ? 'none' : 'inline-block';
    }

    if (!consolePanel || !consoleResizer) return;

    if (hasEdit) {
        if (hasConsole) {
            consoleResizer.style.display = 'block';
            consolePanel.style.display = 'flex';
            consolePanel.style.flexGrow = '0';
            consolePanel.style.height = consoleExpanded ? '400px' : '200px';
        } else {
            consoleResizer.style.display = 'none';
            consolePanel.style.height = '0px';
            setTimeout(() => { if (!consoleOpened && !isRunning) consolePanel.style.display = 'none'; }, 300);
        }
    } else {
        if (hasConsole) {
            consoleResizer.style.display = 'block';
            consolePanel.style.display = 'flex';
            consolePanel.style.flexGrow = '0';
            consolePanel.style.height = consoleExpanded ? '550px' : '280px';
        } else {
            consoleResizer.style.display = 'none';
            consolePanel.style.height = '0px';
            setTimeout(() => { if (!consoleOpened && !isRunning) consolePanel.style.display = 'none'; }, 300);
        }
    }
}
window.updateCenterLayout = updateCenterLayout;

function closeConsole() {
    consoleOpened = false;
    window.consoleOpened = false;
    updateCenterLayout();
}
window.closeConsole = closeConsole;

function openYamlEditor(filename) {
    return new Promise((resolve) => {
        let resolved = false;
        const done = () => {
            if (!resolved) {
                resolved = true;
                resolve();
            }
        };
        setTimeout(done, 1000);
        activeEditingFile = filename;
        window.activeEditingFile = activeEditingFile;
        updateCenterLayout();
        const listener = function(evt) {
            if (evt.detail.target && evt.detail.target.id === 'editorPanel') {
                document.removeEventListener('htmx:afterSwap', listener);
                done();
            }
        };
        document.addEventListener('htmx:afterSwap', listener);
        htmx.ajax('GET', `/api/editor?file=${encodeURIComponent(filename)}`, { target: '#editorPanel', swap: 'outerHTML' });
    });
}
window.openYamlEditor = openYamlEditor;

async function saveYamlFile() {
    if (!activeEditingFile) return;
    const saveBtn = document.getElementById('saveYamlBtn');
    if (saveBtn && typeof htmx !== 'undefined') {
        htmx.trigger(saveBtn, 'click');
    } else {
        try {
            const res = await fetch('/api/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ file: activeEditingFile, content: getEditorContent().value })
            });
            const data = await res.json();
            if (data.success) {
                showToast(`💾 ${activeEditingFile} successfully saved to disk!`, "success");
            } else {
                showToast("Error saving: " + data.error, "error");
            }
        } catch (e) {
            showToast("Error saving file: " + e.message, "error");
        }
    }
}
window.saveYamlFile = saveYamlFile;

async function deleteYamlFile() {
    if (!activeEditingFile) return;
    const nameDisplay = document.getElementById('deleteFileNameDisplay');
    if (nameDisplay) nameDisplay.textContent = activeEditingFile;
    const input = document.getElementById('deleteFileNameInput');
    if (input) input.value = activeEditingFile;
    const modal = document.getElementById('deleteTestModal');
    if (modal) modal.style.display = 'flex';
}
window.deleteYamlFile = deleteYamlFile;

async function submitDeleteTest() {
    if (!activeEditingFile) return;
    const modal = document.getElementById('deleteTestModal');
    if (modal) modal.style.display = 'none';
    try {
        const res = await fetch('/api/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ file: activeEditingFile })
        });
        const data = await res.json();
        if (data.success) {
            selectedDatasets = selectedDatasets.filter(d => d.file !== activeEditingFile);
            window.selectedDatasets = selectedDatasets;
            closeEditor();
            await loadFiles();
        } else {
            showToast("Error deleting: " + data.error, "error");
        }
    } catch (e) {
        showToast("Error deleting file: " + e.message, "error");
    }
}
window.submitDeleteTest = submitDeleteTest;

function closeEditor() {
    activeEditingFile = null;
    window.activeEditingFile = null;
    const fileSpan = document.getElementById('editorFileName');
    if (fileSpan) {
        fileSpan.textContent = '';
        fileSpan.removeAttribute('data-file');
    }
    updateCenterLayout();
    if (window.htmx) {
        htmx.ajax('POST', '/api/editor/close', { swap: 'none' });
    }
}
window.closeEditor = closeEditor;

function openReportView(reportId) {
    const reportDisplayName = document.getElementById('reportDisplayName');
    const reportIframe = document.getElementById('reportIframe');
    if (reportDisplayName) reportDisplayName.innerText = reportId;
    if (reportIframe) reportIframe.src = `/api/reporting/report/${reportId}/allure-report/index.html`;

    document.body.classList.add('report-active');
    if (typeof showView === 'function') showView('reportView');
}
window.openReportView = openReportView;

function closeReportView() {
    const iframe = document.getElementById('reportIframe');
    if (iframe) iframe.src = 'about:blank';
    if (typeof showView === 'function') showView('reportViewContainer');
}
window.closeReportView = closeReportView;

function syncStateFromQueueContainer() {
    const container = document.getElementById('queueListContainer');
    if (!container) return;
    const items = container.querySelectorAll('.queue-item');
    selectedDatasets = [];
    items.forEach(item => {
        const file = item.getAttribute('data-file');
        const id = item.getAttribute('data-id');
        if (file && id) {
            selectedDatasets.push({ file, id });
        }
    });
    window.selectedDatasets = selectedDatasets;
    
    const statsQueueCount = document.getElementById('statsQueueCount');
    if (statsQueueCount) {
        statsQueueCount.innerText = `${selectedDatasets.length} Dataset${selectedDatasets.length === 1 ? '' : 's'}`;
    }

    const runQueueBtn = document.getElementById('runQueueBtn');
    if (runQueueBtn) {
        const countSpan = runQueueBtn.querySelector('.badge-count');
        if (countSpan) {
            countSpan.textContent = selectedDatasets.length;
        }
        if (selectedDatasets.length > 0) {
            runQueueBtn.removeAttribute('disabled');
            runQueueBtn.disabled = false;
            runQueueBtn.style.opacity = '1';
            runQueueBtn.style.cursor = 'pointer';
            runQueueBtn.style.filter = 'none';
        } else {
            runQueueBtn.setAttribute('disabled', 'disabled');
            runQueueBtn.disabled = true;
            runQueueBtn.style.opacity = '0.5';
            runQueueBtn.style.cursor = 'not-allowed';
            runQueueBtn.style.filter = 'grayscale(1)';
        }
    }
    if (typeof updateRunButtons === 'function') updateRunButtons();
    syncCheckboxesFromState();
}
window.syncStateFromQueueContainer = syncStateFromQueueContainer;

function updateQueueList() {
    syncStateFromQueueContainer();
}
window.updateQueueList = updateQueueList;

function moveQueueItem(index, direction) {
    htmx.ajax('POST', '/api/queue/move?index=' + index + '&direction=' + (direction === -1 ? 'up' : 'down'), { target: '#queueListContainer', swap: 'innerHTML' });
}
window.moveQueueItem = moveQueueItem;
