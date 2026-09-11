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
    const queueEl = document.getElementById('queueListContainer');
    if (!queueEl) return;
    htmx.ajax('POST', '/api/queue/toggle?file=' + encodeURIComponent(file) + '&id=' + encodeURIComponent(id), { target: '#queueListContainer', swap: 'innerHTML' });
}
window.toggleSelectDataset = toggleSelectDataset;

function toggleSelectAllDatasets(file, checked) {
    const queueEl = document.getElementById('queueListContainer');
    if (!queueEl) return;
    htmx.ajax('POST', '/api/queue/toggleAll?file=' + encodeURIComponent(file) + '&checked=' + checked, { target: '#queueListContainer', swap: 'innerHTML' });
}
window.toggleSelectAllDatasets = toggleSelectAllDatasets;

function toggleFileCheckboxesInstant(fileCb) {
    if (!fileCb) return;
    fileCb.indeterminate = false;
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

function toggleExpandFile(file, targetEl) {
    if (!file) return;
    const escapedFile = String(file).replace(/\\/g, '\\\\').replace(/"/g, '\\"');
    const list = document.getElementById('datasets-' + file) || document.querySelector(`.dataset-list[data-file="${escapedFile}"]`);
    const listItem = targetEl ? targetEl.closest('.list-item') : document.querySelector(`.list-item[data-file="${escapedFile}"]`);
    
    if (list) {
        const isHidden = window.getComputedStyle(list).display === 'none' || list.style.display === 'none';
        list.style.display = isHidden ? 'flex' : 'none';
        
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
            const matIcon = listItem.querySelector('.item-main .material-symbols-outlined');
            if (matIcon) {
                matIcon.textContent = isHidden ? 'keyboard_arrow_down' : 'keyboard_arrow_right';
            }
        }
    }
    fetch('/api/files/toggle?file=' + encodeURIComponent(file), { method: 'POST' }).catch(() => {});
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
            const fileListEl = document.getElementById('yamlFileList');
            if (fileListEl && window.initializedAlready) {
                const listener = function(evt) {
                    if (evt.detail.target && evt.detail.target.id === 'yamlFileList') {
                        document.removeEventListener('htmx:afterSwap', listener);
                        done();
                    }
                };
                document.addEventListener('htmx:afterSwap', listener);
                htmx.ajax('GET', '/api/files/list', { target: '#yamlFileList', swap: 'innerHTML' });
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
                let anyChecked = false;
                datasetCbs.forEach(cb => {
                    const id = cb.getAttribute('data-id');
                    const selected = isDatasetSelected(file, id);
                    cb.checked = selected;
                    if (selected) anyChecked = true;
                    else allChecked = false;
                });
                fileCb.checked = allChecked;
                fileCb.indeterminate = (!allChecked && anyChecked);
            } else {
                const selectedForFile = selectedDatasets.filter(d => String(d.file) === String(file));
                const cachedFile = (window.currentFilesListCached || []).find(f => String(f.file) === String(file));
                const totalDatasets = (cachedFile && cachedFile.datasets) ? cachedFile.datasets.length : 0;

                if (totalDatasets > 0) {
                    const isAll = (selectedForFile.length === totalDatasets);
                    const isSome = (selectedForFile.length > 0 && !isAll);
                    fileCb.checked = isAll;
                    fileCb.indeterminate = isSome;
                } else {
                    const isIndeterminateAttr = fileCb.getAttribute('data-indeterminate') === 'true';
                    if (isIndeterminateAttr) {
                        fileCb.checked = false;
                        fileCb.indeterminate = true;
                    } else {
                        const isFileInQueue = selectedForFile.length > 0;
                        fileCb.indeterminate = false;
                        fileCb.checked = isFileInQueue;
                    }
                }
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
            const isIndeterminateAttr = cb.getAttribute('data-indeterminate') === 'true';
            if (isIndeterminateAttr) {
                cb.checked = false;
                cb.indeterminate = true;
            } else {
                cb.indeterminate = false;
                cb.checked = selectedDatasets.some(d => String(d.file) === String(file));
            }
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
        if (icon) icon.textContent = 'keyboard_arrow_down';
        if (btn) btn.innerHTML = '<span class="material-symbols-outlined" id="consoleSizeIcon" aria-hidden="true">keyboard_arrow_down</span> Shrink';
    } else {
        if (icon) icon.textContent = 'keyboard_arrow_up';
        if (btn) btn.innerHTML = '<span class="material-symbols-outlined" id="consoleSizeIcon" aria-hidden="true">keyboard_arrow_up</span> Expand';
    }
    updateCenterLayout();
}
window.toggleConsoleSize = toggleConsoleSize;

function updateCenterLayout() {
    const container = document.getElementById('auraTestManagerWorkspace') || document.getElementById('dashboardView');
    if (!container) return;

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
updateCenterLayout();
document.addEventListener('DOMContentLoaded', updateCenterLayout);

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

/* HTMX save response listener to reset unsaved status */
document.addEventListener('htmx:afterResponse', function(evt) {
    if (evt.detail && evt.detail.elt && evt.detail.elt.id === 'saveYamlBtn') {
        if (evt.detail.successful) {
            initialEditorContent = compilePlaybookToYaml();
            checkEditorDirtyStatus();
            showToast("💾 Saved playbook file successfully", "success");
        }
    }
});

async function saveYamlFile() {
    if (!activeEditingFile) return;
    compilePlaybookToYaml();
    initialEditorContent = compilePlaybookToYaml();
    checkEditorDirtyStatus();

    const saveBtn = document.getElementById('saveYamlBtn');
    if (saveBtn && typeof htmx !== 'undefined') {
        htmx.trigger(saveBtn, 'click');
    } else {
        try {
            const res = await fetch('/api/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `file=${encodeURIComponent(activeEditingFile)}&content=${encodeURIComponent(initialEditorContent)}`
            });
            const data = await res.json();
            if (data.success) {
                initialEditorContent = compilePlaybookToYaml();
                checkEditorDirtyStatus();
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

function closeEditor(force = false) {
    if (!force && typeof isEditorDirty === 'function' && isEditorDirty()) {
        const confirmClose = confirm(`The file '${activeEditingFile || 'playbook'}' has unsaved changes. Are you sure you want to close without saving?`);
        if (!confirmClose) {
            return;
        }
    }
    activeEditingFile = null;
    window.activeEditingFile = null;
    if (typeof initialEditorContent !== 'undefined') initialEditorContent = '';
    if (typeof undoStack !== 'undefined') undoStack = [];
    if (typeof redoStack !== 'undefined') redoStack = [];
    if (typeof updateUndoRedoUI === 'function') updateUndoRedoUI();
    if (typeof checkEditorDirtyStatus === 'function') checkEditorDirtyStatus();
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
    const queueEl = document.getElementById('queueListContainer');
    if (!queueEl) return;
    htmx.ajax('POST', '/api/queue/move?index=' + index + '&direction=' + (direction === -1 ? 'up' : 'down'), { target: '#queueListContainer', swap: 'innerHTML' });
}
window.moveQueueItem = moveQueueItem;

// ============================================================================
// Visual Playbook Editor Interactivity Module
// ============================================================================

let currentLineCount = 10;
let activeLineNum = 1;
let lastCaretOffset = 0;

window.currentLineCount = currentLineCount;
window.activeLineNum = activeLineNum;
window.lastCaretOffset = lastCaretOffset;

function initVisualPlaybookEditor() {
    const stepsList = document.getElementById('stepsList');
    if (!stepsList) return;

    const rows = document.querySelectorAll('.step-row');
    currentLineCount = rows.length || 1;
    window.currentLineCount = currentLineCount;

    rows.forEach(row => {
        const lineId = parseInt(row.getAttribute('data-line') || '1', 10);
        formatStepToTokens(lineId);
    });

    updateAllLineNumbers();
    compilePlaybookToYaml();

    // Live typing & editing delegation for full Undo/Redo tracking
    const mainEditor = document.getElementById('visualEditorMain');
    if (mainEditor && !mainEditor.dataset.undoBound) {
        mainEditor.dataset.undoBound = 'true';
        mainEditor.addEventListener('input', function(evt) {
            const stepContent = evt.target.closest('.step-content');
            if (stepContent) {
                const hasPills = stepContent.querySelector('.var-pill, .unified-include-pill, .missing-include-pill, .hint-badge');
                if (!hasPills) {
                    stepContent.setAttribute('data-raw', stepContent.innerText);
                }
                compilePlaybookToYaml();
                debouncedPushSnapshot();
            } else if (evt.target.classList.contains('var-key-input') || evt.target.classList.contains('cell-val')) {
                compilePlaybookToYaml();
                debouncedPushSnapshot();
            }
        });
    }

    // Record initial baseline content and initialize undo/redo snapshot stack
    const currentYaml = compilePlaybookToYaml();
    if (!initialEditorContent || undoStack.length === 0) {
        initialEditorContent = currentYaml;
        if (typeof pushEditorSnapshot === 'function') {
            pushEditorSnapshot(true);
        }
    }
}
window.initVisualPlaybookEditor = initVisualPlaybookEditor;

// Automatically init editor when editor fragment is swapped in
document.addEventListener('htmx:afterSwap', function(evt) {
    if (evt.detail.target && evt.detail.target.id === 'editorPanel') {
        initVisualPlaybookEditor();
    }
});

function compilePlaybookToYaml() {
    let yaml = "# Neodymium YAML Test Data File\n\n";

    // before block
    const beforeContainer = document.getElementById('beforeStepsList');
    const beforePanel = document.getElementById('beforeCodePanel');
    if (beforePanel && beforePanel.style.display !== 'none' && beforeContainer) {
        const rows = beforeContainer.querySelectorAll(':scope > .step-row');
        if (rows.length > 0) {
            yaml += "before: |\n";
            rows.forEach(row => {
                const content = row.querySelector('.step-content');
                if (content) {
                    const raw = content.getAttribute('data-raw') || content.innerText.trim();
                    if (raw) yaml += `  ${raw}\n`;
                }
            });
            yaml += "\n";
        }
    }

    // main steps block
    const stepsContainer = document.getElementById('stepsList');
    if (stepsContainer) {
        const rows = stepsContainer.querySelectorAll(':scope > .step-row');
        yaml += "steps: |\n";
        rows.forEach(row => {
            const content = row.querySelector('.step-content');
            if (content) {
                const raw = content.getAttribute('data-raw') || content.innerText.trim();
                if (raw) yaml += `  ${raw}\n`;
            }
        });
        yaml += "\n";
    }

    // after block
    const afterContainer = document.getElementById('afterStepsList');
    const afterPanel = document.getElementById('afterCodePanel');
    if (afterPanel && afterPanel.style.display !== 'none' && afterContainer) {
        const rows = afterContainer.querySelectorAll(':scope > .step-row');
        if (rows.length > 0) {
            yaml += "after: |\n";
            rows.forEach(row => {
                const content = row.querySelector('.step-content');
                if (content) {
                    const raw = content.getAttribute('data-raw') || content.innerText.trim();
                    if (raw) yaml += `  ${raw}\n`;
                }
            });
            yaml += "\n";
        }
    }

    // data matrix
    const matrixTable = document.querySelector('#transposedGrid');
    if (matrixTable) {
        const headerCells = matrixTable.querySelectorAll('thead th');
        const iterCount = headerCells.length - 2;
        if (iterCount > 0) {
            yaml += "data:\n";
            for (let col = 0; col < iterCount; col++) {
                yaml += "  -\n";
                const rows = matrixTable.querySelectorAll('tbody tr');
                rows.forEach(row => {
                    const keyInput = row.querySelector('.var-key-input');
                    const key = keyInput ? keyInput.value.trim() : '';
                    if (!key) return;
                    const cellValInput = row.cells[col + 1]?.querySelector('.cell-val');
                    const val = cellValInput ? cellValInput.value.trim() : '';
                    yaml += `    ${key}: "${val}"\n`;
                });
            }
        }
    }

    const hiddenInput = document.getElementById('editorContent');
    if (hiddenInput) {
        hiddenInput.value = yaml;
    }
    return yaml;
}
window.compilePlaybookToYaml = compilePlaybookToYaml;

/* Line Numbering across all section containers */
function updateAllLineNumbers() {
    let currentLine = 1;
    
    const containers = ['#beforeStepsList', '#stepsList', '#afterStepsList'];
    containers.forEach(containerSelector => {
        const container = document.querySelector(containerSelector);
        if (!container) return;
        
        const parentPanel = container.closest('.editor-code-panel');
        if (parentPanel && parentPanel.style.display === 'none') return;

        const rootRows = container.querySelectorAll(':scope > .step-row');
        rootRows.forEach(row => {
            const rootLineNum = row.getAttribute('data-line');
            const numSpan = row.querySelector('.step-number');
            if (numSpan) numSpan.innerText = currentLine;
            currentLine++;

            const treeCard = document.getElementById(`includeTreeCard_${rootLineNum}`) || document.getElementById(`includeTreeCard${rootLineNum}`);
            if (treeCard && treeCard.style.display !== 'none') {
                currentLine = assignIncludeTreeLineNumbers(treeCard, currentLine);
            }
        });
    });

    const statusTag = document.querySelector('.status-left span:last-child');
    if (statusTag) {
        statusTag.innerText = `${currentLine - 1} Total Steps`;
    }
}
window.updateAllLineNumbers = updateAllLineNumbers;

function assignIncludeTreeLineNumbers(treeCard, startLine) {
    let line = startLine;
    if (!treeCard) return line;

    const innerContainer = treeCard.querySelector('[id^="includeInnerSteps_"]') || treeCard;
    const items = Array.from(innerContainer.children);

    for (let item of items) {
        if (item.classList.contains('nested-editable-step')) {
            const numSpan = item.querySelector('.sub-line-num');
            if (numSpan) numSpan.innerText = line;
            line++;
            const nestedCard = item.querySelector('.include-tree-card');
            if (nestedCard && nestedCard.style.display !== 'none' && getComputedStyle(nestedCard).display !== 'none') {
                line = assignIncludeTreeLineNumbers(nestedCard, line);
            }
        } else if (item.classList.contains('include-tree-card')) {
            if (item.style.display !== 'none' && getComputedStyle(item).display !== 'none') {
                line = assignIncludeTreeLineNumbers(item, line);
            }
        } else if (item.classList.contains('nested-level-1')) {
            const innerStep = item.querySelector('.nested-editable-step');
            if (innerStep) {
                const numSpan = innerStep.querySelector('.sub-line-num');
                if (numSpan) numSpan.innerText = line;
                line++;
            }
            const nestedCard = item.querySelector('.include-tree-card');
            if (nestedCard && nestedCard.style.display !== 'none' && getComputedStyle(nestedCard).display !== 'none') {
                line = assignIncludeTreeLineNumbers(nestedCard, line);
            }
        }
    }
    return line;
}
window.assignIncludeTreeLineNumbers = assignIncludeTreeLineNumbers;

/* Automatically format tokens and update line numbers on HTMX swap */
document.addEventListener('htmx:afterSwap', function(evt) {
    const swapped = evt.detail ? evt.detail.target : null;
    if (swapped) {
        const cards = swapped.querySelectorAll ? swapped.querySelectorAll('.include-tree-card') : [];
        cards.forEach(card => formatIncludeTreeCardTokens(card));
        if (swapped.classList && swapped.classList.contains('include-tree-card')) {
            formatIncludeTreeCardTokens(swapped);
        }
    }
    updateAllLineNumbers();
});

/* Optional Block Management */
function addBeforeBlock() {
    const panel = document.getElementById('beforeCodePanel');
    const btnContainer = document.getElementById('addBeforeBtnContainer');
    const container = document.getElementById('beforeStepsList');
    if (!panel || !container) return;

    panel.style.display = 'flex';
    if (btnContainer) btnContainer.style.display = 'none';

    if (container.children.length === 0) {
        currentLineCount++;
        const lineId = currentLineCount;
        const newRow = document.createElement('div');
        newRow.className = 'step-row';
        newRow.setAttribute('data-line', lineId);
        newRow.innerHTML = `
            <span class="step-number">1</span>
            <div class="step-content" contenteditable="true" spellcheck="false" data-raw="" onkeydown="handleKeyDown(event, ${lineId})" onfocus="handleStepFocus(${lineId})" onblur="handleStepBlur(${lineId})"></div>
            <div class="step-actions">
                <button class="icon-btn danger" onclick="deleteStep(${lineId})"><span class="material-symbols-outlined">delete</span></button>
            </div>
        `;
        container.appendChild(newRow);
        formatStepToTokens(lineId);
        
        const newContent = newRow.querySelector('.step-content');
        if (newContent) newContent.focus();
    }
    updateAllLineNumbers();
}
window.addBeforeBlock = addBeforeBlock;

function removeBeforeBlock() {
    const panel = document.getElementById('beforeCodePanel');
    const btnContainer = document.getElementById('addBeforeBtnContainer');
    const container = document.getElementById('beforeStepsList');
    if (panel) panel.style.display = 'none';
    if (btnContainer) btnContainer.style.display = 'block';
    if (container) container.innerHTML = '';
    updateAllLineNumbers();
}
window.removeBeforeBlock = removeBeforeBlock;

function addAfterBlock() {
    const panel = document.getElementById('afterCodePanel');
    const btnContainer = document.getElementById('addAfterBtnContainer');
    const container = document.getElementById('afterStepsList');
    if (!panel || !container) return;

    panel.style.display = 'flex';
    if (btnContainer) btnContainer.style.display = 'none';

    if (container.children.length === 0) {
        currentLineCount++;
        const lineId = currentLineCount;
        const newRow = document.createElement('div');
        newRow.className = 'step-row';
        newRow.setAttribute('data-line', lineId);
        newRow.innerHTML = `
            <span class="step-number">1</span>
            <div class="step-content" contenteditable="true" spellcheck="false" data-raw="" onkeydown="handleKeyDown(event, ${lineId})" onfocus="handleStepFocus(${lineId})" onblur="handleStepBlur(${lineId})"></div>
            <div class="step-actions">
                <button class="icon-btn danger" onclick="deleteStep(${lineId})"><span class="material-symbols-outlined">delete</span></button>
            </div>
        `;
        container.appendChild(newRow);
        formatStepToTokens(lineId);

        const newContent = newRow.querySelector('.step-content');
        if (newContent) newContent.focus();
    }
    updateAllLineNumbers();
}
window.addAfterBlock = addAfterBlock;

function removeAfterBlock() {
    const panel = document.getElementById('afterCodePanel');
    const btnContainer = document.getElementById('addAfterBtnContainer');
    const container = document.getElementById('afterStepsList');
    if (panel) panel.style.display = 'none';
    if (btnContainer) btnContainer.style.display = 'block';
    if (container) container.innerHTML = '';
    updateAllLineNumbers();
}
window.removeAfterBlock = removeAfterBlock;

/* Caret Offset Measurement */
function getCaretOffset(element) {
    let caretOffset = 0;
    const doc = element.ownerDocument || document;
    const win = doc.defaultView || window;
    const sel = win.getSelection();
    if (sel && sel.rangeCount > 0) {
        const range = sel.getRangeAt(0);
        const preCaretRange = range.cloneRange();
        preCaretRange.selectNodeContents(element);
        preCaretRange.setEnd(range.endContainer, range.endOffset);
        caretOffset = preCaretRange.toString().length;
    }
    return caretOffset;
}
window.getCaretOffset = getCaretOffset;

function setCaretOffset(element, offset) {
    const doc = element.ownerDocument || document;
    const win = doc.defaultView || window;
    const sel = win.getSelection();
    if (!sel) return;

    const range = doc.createRange();
    let currentLen = 0;
    let setSuccess = false;

    function walkNodes(node) {
        if (node.nodeType === 3) {
            const len = node.nodeValue.length;
            if (currentLen + len >= offset) {
                range.setStart(node, offset - currentLen);
                range.collapse(true);
                setSuccess = true;
                return true;
            }
            currentLen += len;
        } else {
            for (let child of node.childNodes) {
                if (walkNodes(child)) return true;
            }
        }
        return false;
    }

    walkNodes(element);

    if (!setSuccess) {
        range.selectNodeContents(element);
        range.collapse(false);
    }

    sel.removeAllRanges();
    sel.addRange(range);
}
window.setCaretOffset = setCaretOffset;

/* Step Focus & Blur Event Handlers */
function handleStepFocus(lineNum) {
    if (activeLineNum && activeLineNum !== lineNum) {
        const prevRow = document.querySelector(`.step-row[data-line="${activeLineNum}"]`);
        if (prevRow) prevRow.classList.remove('active-line');
        formatStepToTokens(activeLineNum);
    }

    activeLineNum = lineNum;
    window.activeLineNum = activeLineNum;
    const row = document.querySelector(`.step-row[data-line="${lineNum}"]`);
    if (!row) return;

    row.classList.add('active-line');
    const content = row.querySelector('.step-content');
    if (!content) return;

    const raw = content.getAttribute('data-raw');
    if (raw !== null && raw !== undefined) {
        content.innerText = raw;
    }

    const statLine = document.getElementById('statLine');
    if (statLine) statLine.innerText = lineNum;
}
window.handleStepFocus = handleStepFocus;

function handleStepBlur(lineNum) {
    const row = document.querySelector(`.step-row[data-line="${lineNum}"]`);
    if (!row) return;

    const content = row.querySelector('.step-content');
    if (!content) return;

    const hasPills = content.querySelector('.var-pill, .unified-include-pill, .missing-include-pill, .hint-badge');
    if (!hasPills) {
        const currentText = content.innerText;
        if (currentText !== undefined && currentText !== null) {
            content.setAttribute('data-raw', currentText);
        }
    }

    // Auto-collapse check for empty optional sections
    const parentContainer = content.closest('#beforeStepsList, #afterStepsList');
    if (parentContainer) {
        setTimeout(() => {
            const activeEl = document.activeElement;
            if (!parentContainer.contains(activeEl)) {
                let allEmpty = true;
                const rows = parentContainer.querySelectorAll('.step-row');
                rows.forEach(r => {
                    const c = r.querySelector('.step-content');
                    if (c && (c.getAttribute('data-raw') || c.innerText).trim().length > 0) {
                        allEmpty = false;
                    }
                });
                if (allEmpty) {
                    if (parentContainer.id === 'beforeStepsList') removeBeforeBlock();
                    if (parentContainer.id === 'afterStepsList') removeAfterBlock();
                }
            }
        }, 200);
    }

    setTimeout(() => {
        const activeEl = document.activeElement;
        if (!activeEl || !row.contains(activeEl)) {
            formatStepToTokens(lineNum);
        }
    }, 120);

    compilePlaybookToYaml();
}
window.handleStepBlur = handleStepBlur;

/* Keyboard Navigation */
/* Keyboard Navigation & Caret Operations */
function handleKeyDown(event, lineNum) {
    const content = event.target;
    const currentOffset = getCaretOffset(content);
    const raw = content.getAttribute('data-raw') || content.innerText || '';

    if (event.key === 'ArrowUp') {
        if (lineNum > 1) {
            event.preventDefault();
            lastCaretOffset = currentOffset;
            focusLine(lineNum - 1, currentOffset);
        }
    } else if (event.key === 'ArrowDown') {
        const nextRow = document.querySelector(`.step-row[data-line="${lineNum + 1}"]`);
        if (nextRow) {
            event.preventDefault();
            lastCaretOffset = currentOffset;
            focusLine(lineNum + 1, currentOffset);
        }
    } else if (event.key === 'Enter') {
        event.preventDefault();
        const contentEl = event.target;
        const currentOffset = getCaretOffset(contentEl);
        const raw = contentEl.getAttribute('data-raw') || contentEl.innerText || '';

        const headText = raw.substring(0, currentOffset).replace(/[\r\n]+/g, ' ').trim();
        const tailText = raw.substring(currentOffset).replace(/[\r\n]+/g, ' ').trim();

        contentEl.setAttribute('data-raw', headText);
        contentEl.innerText = headText;
        formatStepToTokens(lineNum);

        const newRow = insertStepBelow(lineNum, tailText);
        if (newRow) {
            const reindexedLine = parseInt(newRow.getAttribute('data-line'), 10) || (lineNum + 1);
            formatStepToTokens(reindexedLine);
            const newContent = newRow.querySelector('.step-content');
            if (newContent) {
                newContent.focus();
                setCaretOffset(newContent, 0);
            }
        }
        compilePlaybookToYaml();
        if (typeof debouncedPushSnapshot === 'function') debouncedPushSnapshot();
    } else if (event.key === 'Backspace' && currentOffset === 0) {
        const prevRow = document.querySelector(`.step-row[data-line="${lineNum - 1}"]`);
        if (prevRow) {
            event.preventDefault();
            const prevContent = prevRow.querySelector('.step-content');
            if (prevContent) {
                const prevRaw = prevContent.getAttribute('data-raw') || prevContent.innerText || '';
                const joinOffset = prevRaw.length;
                const combined = prevRaw + (raw ? (prevRaw ? ' ' : '') + raw : '');
                prevContent.setAttribute('data-raw', combined);
                prevContent.innerText = combined;
                formatStepToTokens(lineNum - 1);
                deleteStep(lineNum);
                focusLine(lineNum - 1, joinOffset);
                compilePlaybookToYaml();
                if (typeof debouncedPushSnapshot === 'function') debouncedPushSnapshot();
            }
        }
    } else if (event.key === 'Delete' && currentOffset >= raw.length) {
        const nextRow = document.querySelector(`.step-row[data-line="${lineNum + 1}"]`);
        if (nextRow) {
            event.preventDefault();
            const nextContent = nextRow.querySelector('.step-content');
            if (nextContent) {
                const nextRaw = nextContent.getAttribute('data-raw') || nextContent.innerText || '';
                const combined = raw + (nextRaw ? (raw ? ' ' : '') + nextRaw : '');
                content.setAttribute('data-raw', combined);
                content.innerText = combined;
                formatStepToTokens(lineNum);
                deleteStep(lineNum + 1);
                focusLine(lineNum, currentOffset);
                compilePlaybookToYaml();
                if (typeof debouncedPushSnapshot === 'function') debouncedPushSnapshot();
            }
        }
    }
}
window.handleKeyDown = handleKeyDown;

function focusLine(lineNum, targetOffset = -1) {
    const row = document.querySelector(`.step-row[data-line="${lineNum}"]`);
    if (!row) return;

    const content = row.querySelector('.step-content');
    if (!content) return;

    content.focus();
    handleStepFocus(lineNum);

    if (targetOffset >= 0) {
        const raw = content.getAttribute('data-raw') || content.innerText;
        const clampedOffset = Math.min(targetOffset, raw.length);
        setCaretOffset(content, clampedOffset);
        lastCaretOffset = clampedOffset;
    }
}
window.focusLine = focusLine;

function insertStepBelow(lineNum, initialText = '') {
    const targetRow = document.querySelector(`.step-row[data-line="${lineNum}"]`);
    if (!targetRow) return null;

    currentLineCount++;
    const newRow = document.createElement('div');
    newRow.className = 'step-row';
    newRow.setAttribute('data-line', currentLineCount);

    const safeInitial = (initialText || '').replace(/"/g, '&quot;');

    newRow.innerHTML = `
        <span class="step-number">${currentLineCount}</span>
        <div class="step-content" contenteditable="true" spellcheck="false" data-raw="${safeInitial}" onkeydown="handleKeyDown(event, ${currentLineCount})" onfocus="handleStepFocus(${currentLineCount})" onblur="handleStepBlur(${currentLineCount})">${safeInitial}</div>
        <div class="step-actions">
            <button class="icon-btn danger" onclick="deleteStep(${currentLineCount})"><span class="material-symbols-outlined">delete</span></button>
        </div>
    `;

    targetRow.after(newRow);
    reindexSteps();

    const newContent = newRow.querySelector('.step-content');
    if (newContent) newContent.focus();
    return newRow;
}
window.insertStepBelow = insertStepBelow;

function deleteStep(lineNum) {
    const row = document.querySelector(`.step-row[data-line="${lineNum}"]`);
    if (row) {
        row.remove();
        reindexSteps();
    }
}
window.deleteStep = deleteStep;

function reindexSteps() {
    const containers = ['#beforeStepsList', '#stepsList', '#afterStepsList'];
    containers.forEach(containerSelector => {
        const container = document.querySelector(containerSelector);
        if (!container) return;
        const rows = container.querySelectorAll(':scope > .step-row');
        rows.forEach((row) => {
            const lNum = row.getAttribute('data-line');
            const content = row.querySelector('.step-content');
            if (content) {
                content.setAttribute('onkeydown', `handleKeyDown(event, ${lNum})`);
                content.setAttribute('onfocus', `handleStepFocus(${lNum})`);
                content.setAttribute('onblur', `handleStepBlur(${lNum})`);
            }
            const delBtn = row.querySelector('.step-actions .icon-btn');
            if (delBtn) {
                delBtn.setAttribute('onclick', `deleteStep(${lNum})`);
            }
        });
    });
    updateAllLineNumbers();
    compilePlaybookToYaml();
}
window.reindexSteps = reindexSteps;

/* Token Rendering Parser */
function formatStepToTokens(lineNum) {
    const row = document.querySelector(`.step-row[data-line="${lineNum}"]`);
    if (!row) return;
    const content = row.querySelector('.step-content');
    if (!content) return;

    const rawText = content.getAttribute('data-raw') || content.innerText;
    if (!rawText) return;

    let safeText = rawText.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    safeText = safeText.replace(/(#[a-zA-Z0-9_\-]+)/g, '<span class="hint-badge">$1</span>');

    safeText = safeText.replace(/_include:\s*([a-zA-Z0-9_\/\.\-]*missing[a-zA-Z0-9_\/\.\-]*)/g, function(match, path) {
        return `<span class="missing-include-pill" contenteditable="false"><span class="material-symbols-outlined pill-icon">error</span>Missing Include: ${path}</span>`;
    });

    safeText = safeText.replace(/_include:\s*([a-zA-Z0-9_\/\.\-]+)/g, function(match, path) {
        const filename = path.split('/').pop();
        return `<span class="unified-include-pill" contenteditable="false"><span class="material-symbols-outlined pill-icon">extension</span><span>include: ${filename}</span><span class="material-symbols-outlined hover-arrow" onmousedown="handleArrowMouseDown(event, '${lineNum}', '${path}')" title="Toggle steps for ${filename}">expand_more</span></span>`;
    });

    safeText = safeText.replace(/\$\{([a-zA-Z0-9_\.]+)\}/g, function(match, name) {
        return `<span class="var-pill" contenteditable="false"><span class="material-symbols-outlined pill-icon">data_object</span>${name}</span>`;
    });

    content.innerHTML = safeText;
}
window.formatStepToTokens = formatStepToTokens;

function handleArrowMouseDown(event, cardId, filePath) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    
    let tree = document.getElementById(`includeTreeCard_${cardId}`) || document.getElementById(`includeTreeCard${cardId}`);

    if (tree && tree.getAttribute('data-include-file') !== filePath) {
        tree.remove();
        tree = null;
    }

    if (!tree) {
        // HTMX fetch or dynamic card fallback
        htmx.ajax('GET', `/api/editor/include-tree?file=${encodeURIComponent(filePath)}&cardId=${encodeURIComponent(cardId)}`, {
            target: `.step-row[data-line="${cardId}"]`,
            swap: 'afterend'
        });
    } else {
        tree.style.display = (tree.style.display === 'none') ? 'flex' : 'none';
        updateAllLineNumbers();
    }
}
window.handleArrowMouseDown = handleArrowMouseDown;

function formatIncludeTreeCardTokens(treeCard) {
    if (!treeCard) return;
    const steps = treeCard.querySelectorAll('.nested-editable-step');
    steps.forEach(stepEl => {
        if (!stepEl.querySelector('.unified-include-pill') && !stepEl.querySelector('.var-pill')) {
            let text = stepEl.getAttribute('data-original') || stepEl.innerText;
            if (text && text.startsWith('-')) text = text.replace(/^-\s*/, '');
            if (text && (text.includes('_include:') || text.includes('${'))) {
                const subLineSpan = stepEl.querySelector('.sub-line-num');
                const lineNumText = subLineSpan ? subLineSpan.innerText : '-';
                
                let safeText = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                safeText = safeText.replace(/_include:\s*([a-zA-Z0-9_\/\.\-]+)/g, function(match, path) {
                    const filename = path.split('/').pop();
                    const cardId = (treeCard.id ? treeCard.id.replace('includeTreeCard_', '') : 'sub') + '_' + Math.floor(Math.random()*1000);
                    return `<span class="unified-include-pill" contenteditable="false"><span class="material-symbols-outlined pill-icon">extension</span><span>include: ${filename}</span><span class="material-symbols-outlined hover-arrow" onmousedown="handleArrowMouseDown(event, '${cardId}', '${path}')" title="Toggle steps for ${filename}">expand_more</span></span>`;
                });
                safeText = safeText.replace(/\$\{([a-zA-Z0-9_\.]+)\}/g, function(match, name) {
                    return `<span class="var-pill" contenteditable="false"><span class="material-symbols-outlined pill-icon">data_object</span>${name}</span>`;
                });
                stepEl.innerHTML = `<span class="sub-line-num">${lineNumText}</span><span>${safeText}</span>`;
            }
        }
    });
}
window.formatIncludeTreeCardTokens = formatIncludeTreeCardTokens;

let activeNestedStep = null;

function handleNestedFocus(el) {
    if (!el) return;
    activeNestedStep = el;
    el.setAttribute('contenteditable', 'true');
    const text = el.getAttribute('data-original') || el.innerText;
    const lineNum = el.querySelector('.sub-line-num') ? el.querySelector('.sub-line-num').innerText : '-';
    el.innerHTML = `<span class="sub-line-num">${lineNum}</span><span class="raw-nested-text" style="outline: none;">${text}</span>`;
}
window.handleNestedFocus = handleNestedFocus;

function handleNestedBlur(el, cardId) {
    if (!el) return;
    const effectiveCardId = cardId || el.getAttribute('data-card-id');
    const rawSpan = el.querySelector('.raw-nested-text') || el;
    const newText = rawSpan.innerText.trim();
    el.setAttribute('data-original', newText);
    el.setAttribute('contenteditable', 'false');
    const treeCard = el.closest('.include-tree-card');
    if (treeCard) {
        formatIncludeTreeCardTokens(treeCard);
    }
    if (effectiveCardId) {
        markIncludeUnsaved(effectiveCardId);
    }
    updateAllLineNumbers();
}
window.handleNestedBlur = handleNestedBlur;

function enableIncludeEdit(cardId) {
    const treeCard = document.getElementById(`includeTreeCard_${cardId}`) || document.getElementById(`includeTreeCard${cardId}`);
    if (!treeCard) return;

    const btnEdit = document.getElementById(`btnEditInclude_${cardId}`);
    const btnSave = document.getElementById(`btnSaveInclude_${cardId}`);
    const btnDiscard = document.getElementById(`btnDiscardInclude_${cardId}`);

    const steps = treeCard.querySelectorAll('.nested-editable-step');
    steps.forEach(step => {
        step.setAttribute('contenteditable', 'true');
    });

    if (btnEdit) btnEdit.style.display = 'none';
    if (btnSave) btnSave.style.display = 'inline-flex';
    if (btnDiscard) btnDiscard.style.display = 'inline-flex';
}
window.enableIncludeEdit = enableIncludeEdit;

function markIncludeUnsaved(cardId) {
    const treeCard = document.getElementById(`includeTreeCard_${cardId}`) || document.getElementById(`includeTreeCard${cardId}`);
    const badge = document.getElementById(`unsavedBadge_${cardId}`);
    if (treeCard) treeCard.classList.add('has-unsaved-changes');
    if (badge) badge.style.display = 'inline-flex';
}
window.markIncludeUnsaved = markIncludeUnsaved;

function saveIncludeInline(cardId) {
    const treeCard = document.getElementById(`includeTreeCard_${cardId}`) || document.getElementById(`includeTreeCard${cardId}`);
    if (!treeCard) return;

    const filePath = treeCard.getAttribute('data-include-file');
    const steps = treeCard.querySelectorAll('.nested-editable-step');
    let content = '';
    steps.forEach(step => {
        const textSpan = step.querySelector('span:last-child');
        const lineText = textSpan ? textSpan.innerText : step.innerText;
        content += lineText + '\n';
    });

    if (filePath) {
        fetch('/api/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `file=${encodeURIComponent(filePath)}&content=${encodeURIComponent(content)}`
        }).then(() => {
            showToast(`💾 Saved included file ${filePath}`, "success");
        }).catch(err => {
            showToast(`Error saving include file: ${err.message}`, "error");
        });
    }

    const badge = document.getElementById(`unsavedBadge_${cardId}`);
    const btnEdit = document.getElementById(`btnEditInclude_${cardId}`);
    const btnSave = document.getElementById(`btnSaveInclude_${cardId}`);
    const btnDiscard = document.getElementById(`btnDiscardInclude_${cardId}`);

    steps.forEach(step => {
        step.setAttribute('contenteditable', 'false');
        step.setAttribute('data-original', step.innerText);
    });

    treeCard.classList.remove('has-unsaved-changes');
    if (badge) badge.style.display = 'none';
    if (btnEdit) btnEdit.style.display = 'inline-flex';
    if (btnSave) btnSave.style.display = 'none';
    if (btnDiscard) btnDiscard.style.display = 'none';
}
window.saveIncludeInline = saveIncludeInline;

function discardIncludeInline(cardId) {
    const treeCard = document.getElementById(`includeTreeCard_${cardId}`) || document.getElementById(`includeTreeCard${cardId}`);
    if (!treeCard) return;

    const badge = document.getElementById(`unsavedBadge_${cardId}`);
    const btnEdit = document.getElementById(`btnEditInclude_${cardId}`);
    const btnSave = document.getElementById(`btnSaveInclude_${cardId}`);
    const btnDiscard = document.getElementById(`btnDiscardInclude_${cardId}`);

    const steps = treeCard.querySelectorAll('.nested-editable-step');
    steps.forEach(step => {
        step.setAttribute('contenteditable', 'false');
        const orig = step.getAttribute('data-original');
        if (orig) {
            const textSpan = step.querySelector('span:last-child');
            if (textSpan) textSpan.innerText = orig;
            else step.innerText = orig;
        }
    });

    treeCard.classList.remove('has-unsaved-changes');
    if (badge) badge.style.display = 'none';
    if (btnEdit) btnEdit.style.display = 'inline-flex';
    if (btnSave) btnSave.style.display = 'none';
    if (btnDiscard) btnDiscard.style.display = 'none';
}
window.discardIncludeInline = discardIncludeInline;

// Track Caret Offset Position
document.addEventListener('selectionchange', () => {
    const activeEl = document.activeElement;
    if (activeEl && activeEl.classList.contains('step-content')) {
        lastCaretOffset = getCaretOffset(activeEl);
        window.lastCaretOffset = lastCaretOffset;
        const statLine = document.getElementById('statLine');
        const statCol = document.getElementById('statCol');
        if (statLine) statLine.innerText = activeLineNum;
        if (statCol) statCol.innerText = lastCaretOffset + 1;
    }
});

function insertVariableFromInput(varName) {
    if (!varName) return;
    const activeRow = document.querySelector(`.step-row[data-line="${activeLineNum}"]`);
    if (activeRow) {
        const content = activeRow.querySelector('.step-content');
        if (content) {
            let raw = content.getAttribute('data-raw') || content.innerText;
            const insertText = `\${${varName}}`;
            if (lastCaretOffset >= 0 && lastCaretOffset <= raw.length) {
                raw = raw.slice(0, lastCaretOffset) + insertText + raw.slice(lastCaretOffset);
                lastCaretOffset += insertText.length;
            } else {
                raw += ` ${insertText}`;
                lastCaretOffset = raw.length;
            }
            content.setAttribute('data-raw', raw);
            formatStepToTokens(activeLineNum);
            content.focus();
            setCaretOffset(content, lastCaretOffset);
            if (typeof pushEditorSnapshot === 'function') pushEditorSnapshot(false);
        }
    }
}
window.insertVariableFromInput = insertVariableFromInput;

function insertSnippetWithCaret(templateText, caretOffset = -1) {
    const activeEl = document.activeElement;
    const nestedStep = (activeEl && activeEl.closest('.nested-editable-step')) ? activeEl.closest('.nested-editable-step') : activeNestedStep;

    if (nestedStep && document.contains(nestedStep)) {
        const rawSpan = nestedStep.querySelector('.raw-nested-text') || nestedStep.querySelector('span:last-child') || nestedStep;
        let text = rawSpan.innerText || '';
        text += (text.length > 0 ? ' ' : '') + templateText;
        if (rawSpan !== nestedStep) rawSpan.innerText = text;
        else nestedStep.innerText = text;
        nestedStep.setAttribute('data-original', text);
        const card = nestedStep.closest('.include-tree-card');
        if (card) {
            formatIncludeTreeCardTokens(card);
            const cardId = card.getAttribute('data-card-id') || card.id.replace('includeTreeCard_', '').replace('includeTreeCard', '');
            markIncludeUnsaved(cardId);
        }
        return;
    }

    let activeRow = document.querySelector(`.step-row[data-line="${activeLineNum}"]`);
    if (!activeRow) {
        activeRow = document.querySelector(`.step-row[data-line="1"]`);
    }
    if (!activeRow) return;

    const content = activeRow.querySelector('.step-content');
    if (!content) return;

    let raw = content.getAttribute('data-raw');
    if (raw === null || raw === undefined) raw = content.innerText || '';

    if (lastCaretOffset >= 0 && lastCaretOffset <= raw.length) {
        raw = raw.slice(0, lastCaretOffset) + templateText + raw.slice(lastCaretOffset);
        if (caretOffset >= 0) {
            lastCaretOffset += caretOffset;
        } else {
            lastCaretOffset += templateText.length;
        }
    } else {
        raw += (raw.length > 0 ? ' ' : '') + templateText;
        lastCaretOffset = raw.length;
    }

    content.setAttribute('data-raw', raw);
    formatStepToTokens(activeLineNum);
    content.focus();
    setCaretOffset(content, lastCaretOffset);
    if (typeof pushEditorSnapshot === 'function') pushEditorSnapshot(false);
}
window.insertSnippetWithCaret = insertSnippetWithCaret;

function insertIncludePill(filePath) {
    const activeEl = document.activeElement;
    const nestedStep = (activeEl && activeEl.closest('.nested-editable-step')) ? activeEl.closest('.nested-editable-step') : activeNestedStep;

    if (nestedStep && document.contains(nestedStep)) {
        const treeCard = nestedStep.closest('.include-tree-card');
        const rawSpan = nestedStep.querySelector('.raw-nested-text') || nestedStep.querySelector('span:last-child') || nestedStep;
        let text = rawSpan.innerText ? rawSpan.innerText.trim() : '';

        if (!text) {
            text = `_include: ${filePath}`;
        } else {
            text += ` _include: ${filePath}`;
        }
        if (rawSpan !== nestedStep) rawSpan.innerText = text;
        else nestedStep.innerText = text;
        nestedStep.setAttribute('data-original', text);

        if (treeCard) {
            formatIncludeTreeCardTokens(treeCard);
            const cardId = treeCard.getAttribute('data-card-id') || treeCard.id.replace('includeTreeCard_', '').replace('includeTreeCard', '');
            markIncludeUnsaved(cardId);
        }
        return;
    }

    let targetLine = activeLineNum || 1;
    let targetRow = document.querySelector(`.step-row[data-line="${targetLine}"]`);

    if (!targetRow) {
        targetRow = document.querySelector(`.step-row[data-line="1"]`);
        targetLine = 1;
    }

    if (targetRow) {
        const content = targetRow.querySelector('.step-content');
        const currentRaw = content ? (content.getAttribute('data-raw') || content.innerText || '').trim() : '';

        if (!currentRaw) {
            if (content) {
                content.setAttribute('data-raw', `_include: ${filePath}`);
                formatStepToTokens(targetLine);
                content.focus();
            }
        } else {
            const newRow = insertStepBelow(targetLine, `_include: ${filePath}`);
            if (newRow) {
                const reindexedLine = parseInt(newRow.getAttribute('data-line'), 10) || (targetLine + 1);
                formatStepToTokens(reindexedLine);
                const newContent = newRow.querySelector('.step-content');
                if (newContent) newContent.focus();
            }
        }
        compilePlaybookToYaml();
        if (typeof pushEditorSnapshot === 'function') {
            pushEditorSnapshot(false);
        }
    }
}
window.insertIncludePill = insertIncludePill;

function addMatrixRowInlineEmpty() {
    const tbody = document.querySelector('#transposedGrid tbody');
    if (!tbody) return;
    const colCount = document.querySelectorAll('#transposedGrid thead th').length - 2;
    const newIndex = tbody.rows.length + 1;
    const newVarName = `newVariable_${newIndex}`;
    
    const tr = document.createElement('tr');
    let html = `
        <td>
            <div class="var-key-cell">
                <input type="text" class="var-key-input" value="${newVarName}">
                <button class="btn-insert-var-chip" onclick="insertVariableFromInput(this.previousElementSibling.value)">+ Insert</button>
            </div>
        </td>
    `;
    for (let i = 0; i < colCount; i++) {
        html += `<td><input type="text" class="cell-val" value="" placeholder="empty" oninput="compilePlaybookToYaml()"></td>`;
    }
    html += `<td></td>`;
    tr.innerHTML = html;
    tbody.appendChild(tr);
    compilePlaybookToYaml();
}
window.addMatrixRowInlineEmpty = addMatrixRowInlineEmpty;

function addMatrixColumnInline() {
    const headerRow = document.getElementById('matrixHeaderRow');
    if (!headerRow) return;
    const colCount = headerRow.cells.length - 1;
    
    const th = document.createElement('th');
    th.id = `iterHeader_${colCount}`;
    th.innerHTML = `<span>Iteration ${colCount}</span>`;
    
    headerRow.insertBefore(th, headerRow.cells[headerRow.cells.length - 1]);

    const tbodyRows = document.querySelectorAll('#transposedGrid tbody tr');
    tbodyRows.forEach((tr, rowIdx) => {
        const td = document.createElement('td');
        if (rowIdx === 0) {
            td.innerHTML = `<input type="text" class="cell-val" value="custom_${colCount}" oninput="compilePlaybookToYaml()">`;
        } else {
            td.innerHTML = '<input type="text" class="cell-val" value="" placeholder="empty" oninput="compilePlaybookToYaml()">';
        }
        tr.insertBefore(td, tr.cells[tr.cells.length - 1]);
    });
    compilePlaybookToYaml();
}
window.addMatrixColumnInline = addMatrixColumnInline;

/* Undo/Redo & Unsaved State Management */
let undoStack = [];
let redoStack = [];
let initialEditorContent = '';
let undoDebounceTimer = null;

function isEditorDirty() {
    if (!activeEditingFile) return false;
    const currentYaml = compilePlaybookToYaml();
    if (initialEditorContent && currentYaml !== initialEditorContent) {
        return true;
    }
    const unsavedIncludes = document.querySelectorAll('.include-tree-card.has-unsaved-changes');
    return unsavedIncludes.length > 0;
}
window.isEditorDirty = isEditorDirty;

function checkEditorDirtyStatus() {
    const dirty = isEditorDirty();
    const badge = document.getElementById('editorUnsavedBadge');
    if (badge) {
        badge.style.display = dirty ? 'inline-flex' : 'none';
    }
}
window.checkEditorDirtyStatus = checkEditorDirtyStatus;

function updateUndoRedoUI() {
    const undoBtn = document.getElementById('undoBtn');
    const redoBtn = document.getElementById('redoBtn');
    if (undoBtn) undoBtn.disabled = (undoStack.length <= 1);
    if (redoBtn) redoBtn.disabled = (redoStack.length === 0);
}
window.updateUndoRedoUI = updateUndoRedoUI;

function getEditorSnapshot() {
    const captureBlock = (containerId) => {
        const container = document.getElementById(containerId);
        if (!container) return [];
        const rows = container.querySelectorAll(':scope > .step-row');
        const list = [];
        rows.forEach(row => {
            const content = row.querySelector('.step-content');
            if (content) {
                list.push(content.getAttribute('data-raw') || content.innerText.trim());
            }
        });
        return list;
    };

    const beforePanel = document.getElementById('beforeCodePanel');
    const hasBefore = beforePanel && beforePanel.style.display !== 'none';
    const afterPanel = document.getElementById('afterCodePanel');
    const hasAfter = afterPanel && afterPanel.style.display !== 'none';

    const matrixTable = document.querySelector('#transposedGrid');
    const matrixData = [];
    if (matrixTable) {
        const rows = matrixTable.querySelectorAll('tbody tr');
        rows.forEach(row => {
            const keyInput = row.querySelector('.var-key-input');
            const key = keyInput ? keyInput.value : '';
            const cellValInputs = row.querySelectorAll('.cell-val');
            const vals = Array.from(cellValInputs).map(i => i.value);
            matrixData.push({ key, vals });
        });
    }

    return {
        hasBefore,
        hasAfter,
        beforeSteps: captureBlock('beforeStepsList'),
        steps: captureBlock('stepsList'),
        afterSteps: captureBlock('afterStepsList'),
        matrixData
    };
}
window.getEditorSnapshot = getEditorSnapshot;

function pushEditorSnapshot(isInitial = false) {
    const currentSnap = getEditorSnapshot();
    if (!isInitial && undoStack.length > 0) {
        const lastSnap = undoStack[undoStack.length - 1];
        if (JSON.stringify(lastSnap) === JSON.stringify(currentSnap)) {
            return;
        }
    }
    if (isInitial) {
        undoStack = [currentSnap];
        redoStack = [];
    } else {
        undoStack.push(currentSnap);
        redoStack = [];
        if (undoStack.length > 50) undoStack.shift();
    }
    updateUndoRedoUI();
    checkEditorDirtyStatus();
}
window.pushEditorSnapshot = pushEditorSnapshot;

function debouncedPushSnapshot() {
    clearTimeout(undoDebounceTimer);
    undoDebounceTimer = setTimeout(() => {
        pushEditorSnapshot(false);
    }, 350);
}
window.debouncedPushSnapshot = debouncedPushSnapshot;

function restoreEditorSnapshot(snap) {
    if (!snap) return;

    const beforePanel = document.getElementById('beforeCodePanel');
    const addBeforeBtn = document.getElementById('addBeforeBtnContainer');
    if (beforePanel && addBeforeBtn) {
        beforePanel.style.display = snap.hasBefore ? 'flex' : 'none';
        addBeforeBtn.style.display = snap.hasBefore ? 'none' : 'block';
    }

    const afterPanel = document.getElementById('afterCodePanel');
    const addAfterBtn = document.getElementById('addAfterBtnContainer');
    if (afterPanel && addAfterBtn) {
        afterPanel.style.display = snap.hasAfter ? 'flex' : 'none';
        addAfterBtn.style.display = snap.hasAfter ? 'none' : 'block';
    }

    const rebuildBlock = (containerId, stepArray) => {
        const container = document.getElementById(containerId);
        if (!container) return;
        container.innerHTML = '';
        (stepArray || []).forEach((stepText, idx) => {
            const lineNum = idx + 1;
            const row = document.createElement('div');
            row.className = 'step-row';
            row.setAttribute('data-line', lineNum);
            row.innerHTML = `
                <span class="step-number">${lineNum}</span>
                <div class="step-content" contenteditable="true" spellcheck="false"
                     data-raw="${stepText.replace(/"/g, '&quot;')}"
                     onkeydown="handleKeyDown(event, ${lineNum})"
                     onfocus="handleStepFocus(${lineNum})"
                     onblur="handleStepBlur(${lineNum})"></div>
                <div class="step-actions">
                    <button class="icon-btn danger" onclick="deleteStep(${lineNum})"><span class="material-symbols-outlined">delete</span></button>
                </div>
            `;
            container.appendChild(row);
            formatStepToTokens(lineNum);
        });
    };

    rebuildBlock('beforeStepsList', snap.beforeSteps);
    rebuildBlock('stepsList', snap.steps);
    rebuildBlock('afterStepsList', snap.afterSteps);

    const matrixTbody = document.querySelector('#transposedGrid tbody');
    if (matrixTbody && snap.matrixData) {
        matrixTbody.innerHTML = '';
        snap.matrixData.forEach(row => {
            const tr = document.createElement('tr');
            let html = `
                <td>
                    <div class="var-key-cell">
                        <input type="text" class="var-key-input" value="${row.key}" oninput="compilePlaybookToYaml(); debouncedPushSnapshot();">
                        <button class="btn-insert-var-chip" onclick="insertVariableFromInput(this.previousElementSibling.value)">+ Insert</button>
                    </div>
                </td>
            `;
            (row.vals || []).forEach(val => {
                html += `<td><input type="text" class="cell-val" value="${val}" placeholder="empty" oninput="compilePlaybookToYaml(); debouncedPushSnapshot();"></td>`;
            });
            html += `<td></td>`;
            tr.innerHTML = html;
            matrixTbody.appendChild(tr);
        });
    }

    reindexSteps();
    compilePlaybookToYaml();
    checkEditorDirtyStatus();
    updateUndoRedoUI();
}
window.restoreEditorSnapshot = restoreEditorSnapshot;

function editorUndo() {
    if (undoStack.length <= 1) return;
    const currentSnap = undoStack.pop();
    redoStack.push(currentSnap);
    const prevSnap = undoStack[undoStack.length - 1];
    restoreEditorSnapshot(prevSnap);
}
window.editorUndo = editorUndo;

function editorRedo() {
    if (redoStack.length === 0) return;
    const nextSnap = redoStack.pop();
    undoStack.push(nextSnap);
    restoreEditorSnapshot(nextSnap);
}
window.editorRedo = editorRedo;

// Global Keyboard Shortcuts
document.addEventListener('keydown', function(evt) {
    if (!window.activeEditingFile || !document.getElementById('visualEditorMain')) return;

    const isMac = /Mac|iPod|iPhone|iPad/.test(navigator.platform);
    const modKey = isMac ? evt.metaKey : evt.ctrlKey;

    // Save: Ctrl+S / Cmd+S
    if (modKey && evt.key.toLowerCase() === 's') {
        evt.preventDefault();
        saveYamlFile();
        return;
    }

    // Close: Escape
    if (evt.key === 'Escape') {
        evt.preventDefault();
        closeEditor();
        return;
    }

    // Undo: Ctrl+Z / Cmd+Z (without Shift)
    if (modKey && evt.key.toLowerCase() === 'z' && !evt.shiftKey) {
        evt.preventDefault();
        editorUndo();
        return;
    }

    // Redo: Ctrl+Y / Cmd+Y or Cmd+Shift+Z
    if ((modKey && evt.key.toLowerCase() === 'y') || (modKey && evt.shiftKey && evt.key.toLowerCase() === 'z')) {
        evt.preventDefault();
        editorRedo();
        return;
    }
});

// Plain Text & Multi-Line Step Paste Handler
document.addEventListener('paste', function(evt) {
    if (!window.activeEditingFile || !document.getElementById('visualEditorMain')) return;

    const stepContent = evt.target.closest ? evt.target.closest('.step-content') : null;
    if (stepContent) {
        evt.preventDefault();

        const rawPastedText = (evt.clipboardData || window.clipboardData).getData('text/plain') || '';
        if (!rawPastedText) return;

        const normalizedText = rawPastedText.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
        const lines = normalizedText.split('\n');

        const stepRow = stepContent.closest('.step-row');
        if (!stepRow) return;
        const lineNum = parseInt(stepRow.getAttribute('data-line'), 10);

        const currentOffset = getCaretOffset(stepContent);
        const existingRaw = stepContent.getAttribute('data-raw') || stepContent.innerText || '';

        const headText = existingRaw.substring(0, currentOffset);
        const tailText = existingRaw.substring(currentOffset);

        if (lines.length === 1) {
            const combined = headText + lines[0] + tailText;
            stepContent.setAttribute('data-raw', combined);
            stepContent.innerText = combined;

            formatStepToTokens(lineNum);

            const newCaretOffset = headText.length + lines[0].length;
            setCaretOffset(stepContent, newCaretOffset);
            lastCaretOffset = newCaretOffset;
        } else {
            const firstLineText = headText + lines[0];
            stepContent.setAttribute('data-raw', firstLineText);
            stepContent.innerText = firstLineText;
            formatStepToTokens(lineNum);

            let currentRefLine = lineNum;
            let lastInsertedContent = null;

            for (let i = 1; i < lines.length; i++) {
                let lineText = lines[i];
                if (i === lines.length - 1) {
                    lineText = lineText + tailText;
                }

                const insertedRow = insertStepBelow(currentRefLine, lineText);
                if (insertedRow) {
                    currentRefLine = parseInt(insertedRow.getAttribute('data-line'), 10) || (currentRefLine + 1);
                    formatStepToTokens(currentRefLine);
                    lastInsertedContent = insertedRow.querySelector('.step-content');
                }
            }

            if (lastInsertedContent) {
                lastInsertedContent.focus();
                const targetCaret = lines[lines.length - 1].length;
                setCaretOffset(lastInsertedContent, targetCaret);
                lastCaretOffset = targetCaret;
            }
        }

        compilePlaybookToYaml();
        if (typeof debouncedPushSnapshot === 'function') {
            debouncedPushSnapshot();
        }
    } else if (evt.target.getAttribute && (evt.target.getAttribute('contenteditable') === 'true' || evt.target.tagName === 'INPUT' || evt.target.tagName === 'TEXTAREA')) {
        evt.preventDefault();
        const plainText = (evt.clipboardData || window.clipboardData).getData('text/plain') || '';
        const singleLineText = plainText.replace(/[\r\n]+/g, ' ');

        if (document.queryCommandSupported && document.queryCommandSupported('insertText')) {
            document.execCommand('insertText', false, singleLineText);
        } else {
            const sel = window.getSelection();
            if (sel && sel.rangeCount) {
                const range = sel.getRangeAt(0);
                range.deleteContents();
                range.insertNode(document.createTextNode(singleLineText));
            }
        }

        compilePlaybookToYaml();
        if (typeof debouncedPushSnapshot === 'function') {
            debouncedPushSnapshot();
        }
    }
});

/* PESAP AI Step Review Integration */
function runReviewSteps() {
    const reviewBtn = document.getElementById('reviewStepsBtn');
    if (reviewBtn) {
        reviewBtn.innerHTML = '<span class="material-symbols-outlined spinner">sync</span> Reviewing...';
        reviewBtn.disabled = true;
    }

    const steps = [];
    document.querySelectorAll('#beforeStepsList .step-row, #stepsList .step-row, #afterStepsList .step-row').forEach(row => {
        const lineNum = row.getAttribute('data-line');
        const contentEl = row.querySelector('.step-content');
        const text = contentEl ? (contentEl.getAttribute('data-raw') || contentEl.innerText) : '';
        if (lineNum && text) {
            steps.push({ line: lineNum, text: text });
        }
    });

    fetch('/api/editor/review-steps', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ file: window.activeEditingFile, steps: steps })
    })
    .then(res => res.json())
    .then(data => {
        if (reviewBtn) {
            reviewBtn.innerHTML = '<span class="material-symbols-outlined">auto_awesome</span> Review Steps';
            reviewBtn.disabled = false;
        }

        document.querySelectorAll('.review-suggestion-card').forEach(card => card.remove());

        if (data.suggestions && data.suggestions.length > 0) {
            data.suggestions.forEach(sugg => {
                const targetRow = document.querySelector(`#stepsList .step-row[data-line="${sugg.targetLine}"]`) ||
                                  document.querySelector(`.step-row[data-line="${sugg.targetLine}"]`);
                if (targetRow) {
                    const card = document.createElement('div');
                    card.className = `review-suggestion-card ${sugg.type || 'info'}`;
                    card.innerHTML = `
                        <div class="suggestion-left">
                            <span class="material-symbols-outlined" style="font-size: 16px;">${sugg.icon || 'auto_awesome'}</span>
                            <span><b>PESAP AI Review:</b> ${sugg.message}</span>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            ${sugg.hintText ? `<button class="btn-apply-suggestion" onclick="applySuggestionHint(${sugg.targetLine}, '${sugg.hintText.replace(/'/g, "\\'")}')">Apply Hint</button>` : ''}
                            <button class="icon-btn" onclick="dismissSuggestionCard(this)" title="Dismiss"><span class="material-symbols-outlined">close</span></button>
                        </div>
                    `;
                    targetRow.after(card);
                }
            });
            showToast(`✨ PESAP Review complete: ${data.suggestions.length} suggestion(s) found`, "info");
        } else {
            showToast("✅ PESAP Review complete: All steps look great! No suggestions needed.", "success");
        }
    })
    .catch(err => {
        console.error('Failed to run PESAP review:', err);
        if (reviewBtn) {
            reviewBtn.innerHTML = '<span class="material-symbols-outlined">auto_awesome</span> Review Steps';
            reviewBtn.disabled = false;
        }
    });
}
window.runReviewSteps = runReviewSteps;

function applySuggestionHint(targetLineNum, hintText) {
    const row = document.querySelector(`.step-row[data-line="${targetLineNum}"]`);
    if (row) {
        const contentEl = row.querySelector('.step-content');
        if (contentEl) {
            let current = contentEl.getAttribute('data-raw') || contentEl.innerText;
            if (!current.includes(hintText)) {
                current = current.trim() + ' ' + hintText;
                contentEl.setAttribute('data-raw', current);
                contentEl.innerText = current;
                formatStepToTokens(targetLineNum);
                compilePlaybookToYaml();
                debouncedPushSnapshot();
            }
        }
        const card = row.nextElementSibling;
        if (card && card.classList.contains('review-suggestion-card')) {
            card.remove();
        }
    }
}
window.applySuggestionHint = applySuggestionHint;

function dismissSuggestionCard(btn) {
    const card = btn.closest('.review-suggestion-card');
    if (card) {
        card.remove();
    }
}
window.dismissSuggestionCard = dismissSuggestionCard;



