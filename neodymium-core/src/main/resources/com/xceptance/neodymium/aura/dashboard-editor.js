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
            if (window.initializedAlready) {
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

    const globalBadge = document.getElementById('globalProfileCountBadge');
    let globalProfileCount = 1;
    if (globalBadge) {
        const parsed = parseInt(globalBadge.textContent);
        if (!isNaN(parsed) && parsed > 0) {
            globalProfileCount = parsed;
        }
    } else {
        const checkedGlobal = document.querySelectorAll('.profile-checkbox-row.checked');
        if (checkedGlobal.length > 0) {
            globalProfileCount = checkedGlobal.length;
        }
    }

    let calculatedTotalRuns = 0;
    items.forEach(item => {
        const file = item.getAttribute('data-file');
        const id = item.getAttribute('data-id');
        const customProfilesStr = item.getAttribute('data-custom-profiles');
        if (file && id) {
            selectedDatasets.push({ file, id });
            if (customProfilesStr && customProfilesStr.trim().length > 0) {
                const profiles = customProfilesStr.split(',').filter(p => p.trim().length > 0);
                calculatedTotalRuns += Math.max(1, profiles.length);
            } else {
                calculatedTotalRuns += Math.max(1, globalProfileCount);
            }
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
            countSpan.textContent = calculatedTotalRuns;
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

// ============================================================================
// Visual Playbook Editor Interactivity Module
// ============================================================================

let currentLineCount = 10;
let activeLineNum = 1;
let lastCaretOffset = 0;

window.currentLineCount = currentLineCount;
window.activeLineNum = activeLineNum;
window.lastCaretOffset = lastCaretOffset;

function findInlineCommentIndex(text) {
    if (!text || typeof text !== 'string') return -1;
    let inSingle = false;
    let inDouble = false;
    for (let i = 0; i < text.length; i++) {
        const ch = text[i];
        const prev = i > 0 ? text[i - 1] : '';
        if (ch === '"' && prev !== '\\' && !inSingle) {
            inDouble = !inDouble;
        } else if (ch === "'" && prev !== '\\' && !inDouble) {
            inSingle = !inSingle;
        } else if (ch === '#' && !inSingle && !inDouble) {
            if (i > 0 && (/\s/.test(prev) || prev === '"' || prev === "'")) {
                return i;
            }
        }
    }
    return -1;
}
window.findInlineCommentIndex = findInlineCommentIndex;

function getStepRawText(content) {
    if (!content) return '';
    const rawAttr = content.getAttribute('data-raw');
    const hasPills = content.querySelector('.var-pill, .unified-include-pill, .missing-include-pill, .invalid-include-pill, .hint-badge, .substep-folded-badge, .step-inline-comment, .material-symbols-outlined');
    
    // If no pills or icon tokens are present, rawAttr or innerText is clean plain text
    if (!hasPills) {
        return (rawAttr !== null && rawAttr !== undefined && rawAttr.length > 0) ? rawAttr : (content.innerText || '');
    }
    
    // If not focused, data-raw is the authoritative source of truth
    const isFocused = (document.activeElement === content || content.contains(document.activeElement));
    if (!isFocused && rawAttr !== null && rawAttr !== undefined && rawAttr.trim().length > 0) {
        return rawAttr;
    }
    
    // When focused or if data-raw is missing, reconstruct cleanly from DOM without icon ligatures
    const clone = content.cloneNode(true);
    clone.querySelectorAll('.material-symbols-outlined').forEach(el => el.remove());
    clone.querySelectorAll('.var-pill').forEach(el => {
        const v = el.getAttribute('data-var') || el.textContent.trim();
        el.replaceWith(document.createTextNode(`\${${v}}`));
    });
    clone.querySelectorAll('.unified-include-pill, .invalid-include-pill, .missing-include-pill').forEach(el => {
        const r = el.getAttribute('data-raw');
        if (r) {
            el.replaceWith(document.createTextNode(r));
        } else {
            const txt = el.textContent.trim();
            el.replaceWith(document.createTextNode(txt));
        }
    });
    clone.querySelectorAll('.substep-folded-badge').forEach(el => {
        el.replaceWith(document.createTextNode('>-'));
    });
    return clone.textContent || '';
}
window.getStepRawText = getStepRawText;

function checkAndUpdateCommentRow(stepContentOrRow) {
    if (!stepContentOrRow) return;
    const row = stepContentOrRow.classList && stepContentOrRow.classList.contains('step-row') 
        ? stepContentOrRow 
        : stepContentOrRow.closest('.step-row');
    if (!row) return;

    const content = row.querySelector('.step-content');
    if (!content) return;

    const text = getStepRawText(content).replace(/\r?\n+/g, ' ').trim();
    const isComment = text.startsWith('#');
    const wasComment = row.classList.contains('step-row-comment');

    if (isComment && !wasComment) {
        row.classList.add('step-row-comment');
        row.classList.remove('step-row-substep', 'substep-last');
        content.style.color = 'var(--text-secondary, #64748b)';
        content.style.fontStyle = 'italic';
        updateAllLineNumbers();
        refreshAllSubstepToggleBars();
    } else if (isComment && wasComment) {
        content.style.color = 'var(--text-secondary, #64748b)';
        content.style.fontStyle = 'italic';
        // If followed by substeps, update the group title without full rebuilding
        const next = row.nextElementSibling;
        if (next && next.classList.contains('substeps-toggle-bar')) {
            const titleEl = next.querySelector('.substeps-title');
            if (titleEl) {
                const raw = text.replace(/^#\s*/, '').trim();
                titleEl.textContent = raw || 'Sub-steps';
            }
        }
    } else if (!isComment && wasComment) {
        row.classList.remove('step-row-comment');
        content.style.color = '';
        content.style.fontStyle = '';
        updateAllLineNumbers();
        refreshAllSubstepToggleBars();
    }

    if (!isComment) {
        const inlineCommentIdx = findInlineCommentIndex(text);
        const existingInlineSpan = content.querySelector('.step-inline-comment');
        if (inlineCommentIdx >= 0) {
            const expectedComment = text.substring(inlineCommentIdx);
            if (!existingInlineSpan || existingInlineSpan.textContent !== expectedComment) {
                const caret = getCaretOffset(content);
                const instruction = text.substring(0, inlineCommentIdx);
                const safeInstruction = instruction.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                const safeComment = expectedComment.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                content.innerHTML = safeInstruction + `<span class="step-inline-comment">${safeComment}</span>`;
                setCaretOffset(content, caret);
            }
        } else if (existingInlineSpan) {
            const caret = getCaretOffset(content);
            content.innerText = text;
            setCaretOffset(content, caret);
        }

        const isSubstepPrefix = text.startsWith('>-') || text.startsWith('- >-') || text.startsWith('->-') || text.startsWith('  - ');
        const isCurrentlySubstep = row.classList.contains('step-row-substep');

        if (isSubstepPrefix && !isCurrentlySubstep) {
            row.classList.add('step-row-substep');
            row.setAttribute('data-manual-substep', 'true');

            // Strip the trigger prefix so it is not displayed in the sub-step line
            let cleanText = text;
            if (cleanText.startsWith('- >-')) cleanText = cleanText.substring(4).trim();
            else if (cleanText.startsWith('->-')) cleanText = cleanText.substring(3).trim();
            else if (cleanText.startsWith('>-')) cleanText = cleanText.substring(2).trim();
            else if (cleanText.startsWith('  - ')) cleanText = cleanText.substring(4).trim();

            const caret = getCaretOffset(content);
            const prefixLen = text.length - cleanText.length;
            const newCaret = Math.max(0, caret - prefixLen);

            content.innerText = cleanText;
            content.setAttribute('data-raw', cleanText);
            setCaretOffset(content, newCaret);

            updateAllLineNumbers();
            refreshAllSubstepToggleBars();
            compilePlaybookToYaml();
        }
    }
}
window.checkAndUpdateCommentRow = checkAndUpdateCommentRow;

document.addEventListener('input', function(evt) {
    const stepContent = evt.target && evt.target.closest ? evt.target.closest('.step-content') : null;
    if (stepContent) {
        checkAndUpdateCommentRow(stepContent);
    }
});

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
                checkAndUpdateCommentRow(stepContent);
                const hasPills = stepContent.querySelector('.var-pill, .unified-include-pill, .missing-include-pill, .hint-badge');
                if (!hasPills) {
                    stepContent.setAttribute('data-raw', stepContent.innerText);
                } else {
                    stepContent.setAttribute('data-raw', getStepRawText(stepContent));
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

    function serializeStepRows(rows) {
        let block = "";
        let inSubstepsGroup = false;
        rows.forEach(row => {
            const content = row.querySelector('.step-content');
            if (content) {
                const text = getStepRawText(content).replace(/\r?\n+/g, ' ').trim();
                if (!text) return;
                if (row.classList.contains('step-row-comment') || text.startsWith('#')) {
                    inSubstepsGroup = false;
                    const comment = text.startsWith('#') ? text : `# ${text}`;
                    block += `  ${comment}\n`;
                } else if (row.classList.contains('step-row-substep') || text.startsWith('>-') || text.startsWith('- >-') || text.startsWith('->-') || text.startsWith('  - ')) {
                    if (!inSubstepsGroup) {
                        block += `  - >-\n`;
                        inSubstepsGroup = true;
                    }
                    let subText = text;
                    if (subText.startsWith('- >-')) subText = subText.substring(4).trim();
                    else if (subText.startsWith('->-')) subText = subText.substring(3).trim();
                    else if (subText.startsWith('>-')) subText = subText.substring(2).trim();
                    else if (subText.startsWith('  - ')) subText = subText.substring(4).trim();
                    else if (subText.startsWith('- ') || subText.startsWith('* ')) subText = subText.substring(2).trim();
                    if (subText) {
                        block += `    ${subText}\n`;
                    }
                } else {
                    inSubstepsGroup = false;
                    const cleanText = (text.startsWith('- ') || text.startsWith('* ')) ? text.substring(2).trim() : text;
                    block += `  ${cleanText}\n`;
                }
            }
        });
        return block;
    }

    // before block
    const beforeContainer = document.getElementById('beforeStepsList');
    const beforePanel = document.getElementById('beforeCodePanel');
    if (beforePanel && beforePanel.style.display !== 'none' && beforeContainer) {
        const rows = beforeContainer.querySelectorAll(':scope > .step-row');
        if (rows.length > 0) {
            yaml += "before: |\n" + serializeStepRows(rows) + "\n";
        }
    }

    // main steps block
    const stepsContainer = document.getElementById('stepsList');
    if (stepsContainer) {
        const rows = stepsContainer.querySelectorAll(':scope > .step-row');
        yaml += "steps: |\n" + serializeStepRows(rows) + "\n";
    }

    // after block
    const afterContainer = document.getElementById('afterStepsList');
    const afterPanel = document.getElementById('afterCodePanel');
    if (afterPanel && afterPanel.style.display !== 'none' && afterContainer) {
        const rows = afterContainer.querySelectorAll(':scope > .step-row');
        if (rows.length > 0) {
            yaml += "after: |\n" + serializeStepRows(rows) + "\n";
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
    const rawTextarea = document.getElementById('rawYamlTextarea');
    if (rawTextarea && (typeof currentEditorMode === 'undefined' || currentEditorMode !== 'raw')) {
        rawTextarea.value = yaml;
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

        let lastMainStepLine = 1;
        let substepIndex = 0;
        const rootRows = container.querySelectorAll(':scope > .step-row');
        rootRows.forEach(row => {
            const rootLineNum = row.getAttribute('data-line');
            const numSpan = row.querySelector('.step-number');
            if (row.classList.contains('step-row-comment')) {
                if (numSpan) numSpan.innerHTML = '<span class="comment-hash-badge">#</span>';
                return;
            }
            if (row.classList.contains('step-row-substep')) {
                substepIndex++;
                if (numSpan) numSpan.innerText = `${lastMainStepLine}.${substepIndex}`;
                return;
            }
            lastMainStepLine = currentLine;
            substepIndex = 0;
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
        const inlineCommentIdx = findInlineCommentIndex(raw);
        if (inlineCommentIdx >= 0 && !row.classList.contains('step-row-comment')) {
            const instruction = raw.substring(0, inlineCommentIdx);
            const comment = raw.substring(inlineCommentIdx);
            const safeInstruction = instruction.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            const safeComment = comment.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            content.innerHTML = safeInstruction + `<span class="step-inline-comment">${safeComment}</span>`;
        } else {
            content.innerText = raw;
        }
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
    } else {
        content.setAttribute('data-raw', getStepRawText(content));
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
    const currentRow = content.closest ? content.closest('.step-row') : document.querySelector(`.step-row[data-line="${lineNum}"]`);
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

        if (undoDebounceTimer) {
            clearTimeout(undoDebounceTimer);
            undoDebounceTimer = null;
            pushEditorSnapshot(false);
        }

        // If Enter is pressed in an empty sub-step line (e.g. second Enter in sub-steps area), exit the sub-steps area
        if (currentRow && currentRow.classList.contains('step-row-substep') && raw.trim() === '') {
            currentRow.classList.remove('step-row-substep', 'substep-last');
            currentRow.removeAttribute('data-manual-substep');
            updateAllLineNumbers();
            refreshAllSubstepToggleBars();
            compilePlaybookToYaml();
            if (typeof pushEditorSnapshot === 'function') pushEditorSnapshot(false);
            return;
        }

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
        if (typeof pushEditorSnapshot === 'function') pushEditorSnapshot(false);
    } else if (event.key === 'Backspace' && currentOffset === 0) {
        if (currentRow.classList.contains('step-row-substep')) {
            event.preventDefault();
            if (undoDebounceTimer) {
                clearTimeout(undoDebounceTimer);
                undoDebounceTimer = null;
                pushEditorSnapshot(false);
            }
            currentRow.classList.remove('step-row-substep', 'substep-last');
            currentRow.removeAttribute('data-manual-substep');
            updateAllLineNumbers();
            refreshAllSubstepToggleBars();
            compilePlaybookToYaml();
            if (typeof pushEditorSnapshot === 'function') pushEditorSnapshot(false);
            return;
        }
        const prevRow = document.querySelector(`.step-row[data-line="${lineNum - 1}"]`);
        if (prevRow) {
            event.preventDefault();
            if (undoDebounceTimer) {
                clearTimeout(undoDebounceTimer);
                undoDebounceTimer = null;
                pushEditorSnapshot(false);
            }
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
                if (typeof pushEditorSnapshot === 'function') pushEditorSnapshot(false);
            }
        }
    } else if (event.key === 'Delete' && currentOffset >= raw.length) {
        const nextRow = document.querySelector(`.step-row[data-line="${lineNum + 1}"]`);
        if (nextRow) {
            event.preventDefault();
            if (undoDebounceTimer) {
                clearTimeout(undoDebounceTimer);
                undoDebounceTimer = null;
                pushEditorSnapshot(false);
            }
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
                if (typeof pushEditorSnapshot === 'function') pushEditorSnapshot(false);
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

    if (targetRow && targetRow.classList.contains('step-row-substep')) {
        newRow.classList.add('step-row-substep');
        newRow.setAttribute('data-manual-substep', 'true');
    }

    const safeInitial = (initialText || '').replace(/"/g, '&quot;');

    newRow.innerHTML = `
        <span class="step-number">${currentLineCount}</span>
        <div class="step-content" contenteditable="true" spellcheck="false" data-raw="${safeInitial}" onkeydown="handleKeyDown(event, ${currentLineCount})" onfocus="handleStepFocus(${currentLineCount})" onblur="handleStepBlur(${currentLineCount})" oninput="if(typeof checkAndUpdateCommentRow==='function') checkAndUpdateCommentRow(this);">${safeInitial}</div>
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
                content.setAttribute('oninput', `if(typeof checkAndUpdateCommentRow==='function') checkAndUpdateCommentRow(this);`);
            }
            const delBtn = row.querySelector('.step-actions .icon-btn');
            if (delBtn) {
                delBtn.setAttribute('onclick', `deleteStep(${lNum})`);
            }
        });
    });
    updateAllLineNumbers();
    refreshAllSubstepToggleBars();
    compilePlaybookToYaml();
}
window.reindexSteps = reindexSteps;

function toggleSubsteps(toggleBar) {
    if (!toggleBar) return;
    toggleBar.classList.toggle('collapsed');
    const isCollapsed = toggleBar.classList.contains('collapsed');
    let next = toggleBar.nextElementSibling;
    while (next && next.classList.contains('step-row-substep')) {
        next.style.display = isCollapsed ? 'none' : 'flex';
        next = next.nextElementSibling;
    }
}
window.toggleSubsteps = toggleSubsteps;

function handleSubstepTitleClick(event, el) {
    if (event) {
        event.stopPropagation();
    }
}
window.handleSubstepTitleClick = handleSubstepTitleClick;

function handleSubstepTitleKeyDown(event, el) {
    if (!el) return;
    if (event.key === 'Enter') {
        event.preventDefault();
        event.stopPropagation();
        el.blur();
        return;
    }
    if (event.key === 'Escape') {
        event.preventDefault();
        event.stopPropagation();
        el.blur();
        return;
    }
    event.stopPropagation();
}
window.handleSubstepTitleKeyDown = handleSubstepTitleKeyDown;

function handleSubstepTitleFocus(el) {
    if (!el) return;
    if (el.innerText.trim() === 'Sub-steps') {
        const range = document.createRange();
        range.selectNodeContents(el);
        const sel = window.getSelection();
        if (sel) {
            sel.removeAllRanges();
            sel.addRange(range);
        }
    }
}
window.handleSubstepTitleFocus = handleSubstepTitleFocus;

function handleSubstepTitleInput(el) {
    if (!el) return;
    const toggleBar = el.closest('.substeps-toggle-bar');
    if (!toggleBar) return;
    const container = toggleBar.parentElement;
    if (!container) return;

    const raw = (el.innerText || '').trim();
    const prev = toggleBar.previousElementSibling;
    if (prev && prev.classList.contains('step-row-comment')) {
        const commentContent = prev.querySelector('.step-content');
        if (commentContent) {
            const commentText = raw && raw !== 'Sub-steps' ? `# ${raw}` : '#';
            commentContent.innerText = commentText;
            commentContent.setAttribute('data-raw', commentText);
        }
    } else if (raw && raw !== 'Sub-steps') {
        currentLineCount++;
        const lineId = currentLineCount;
        const newRow = document.createElement('div');
        newRow.className = 'step-row step-row-comment';
        newRow.setAttribute('data-line', lineId);
        const commentText = `# ${raw}`;
        newRow.innerHTML = `
            <span class="step-number"><span class="comment-hash-badge">#</span></span>
            <div class="step-content" contenteditable="true" spellcheck="false" data-raw="${commentText.replace(/"/g, '&quot;')}"
                 style="color: var(--text-secondary, #64748b); font-style: italic;"
                 onkeydown="handleKeyDown(event, ${lineId})"
                 onfocus="handleStepFocus(${lineId})"
                 onblur="handleStepBlur(${lineId})"
                 oninput="if(typeof checkAndUpdateCommentRow==='function') checkAndUpdateCommentRow(this);">${commentText}</div>
            <div class="step-actions">
                <button type="button" class="icon-btn btn-move-up" title="Move Step Up" onclick="moveStep(${lineId}, -1)"><span class="material-symbols-outlined">keyboard_arrow_up</span></button>
                <button type="button" class="icon-btn btn-move-down" title="Move Step Down" onclick="moveStep(${lineId}, 1)"><span class="material-symbols-outlined">keyboard_arrow_down</span></button>
                <button type="button" class="icon-btn danger btn-delete-step" title="Delete Step" onclick="deleteStep(${lineId})"><span class="material-symbols-outlined">delete</span></button>
            </div>
        `;
        container.insertBefore(newRow, toggleBar);
        updateAllLineNumbers();
    }
    compilePlaybookToYaml();
    checkEditorDirtyStatus();
}
window.handleSubstepTitleInput = handleSubstepTitleInput;

function handleSubstepTitleBlur(el) {
    if (!el) return;
    const toggleBar = el.closest('.substeps-toggle-bar');
    if (!toggleBar) return;
    const container = toggleBar.parentElement;
    if (!container) return;

    const raw = (el.innerText || '').trim();
    const prev = toggleBar.previousElementSibling;

    if (!raw || raw === 'Sub-steps') {
        el.innerText = 'Sub-steps';
        if (prev && prev.classList.contains('step-row-comment')) {
            const commentContent = prev.querySelector('.step-content');
            const commentText = (commentContent ? (commentContent.innerText || '') : '').trim();
            if (commentText === '#' || commentText === '' || commentText === '# Sub-steps') {
                prev.remove();
                updateAllLineNumbers();
            }
        }
    } else {
        if (prev && prev.classList.contains('step-row-comment')) {
            const commentContent = prev.querySelector('.step-content');
            if (commentContent) {
                const commentText = `# ${raw}`;
                commentContent.innerText = commentText;
                commentContent.setAttribute('data-raw', commentText);
            }
        } else {
            currentLineCount++;
            const lineId = currentLineCount;
            const newRow = document.createElement('div');
            newRow.className = 'step-row step-row-comment';
            newRow.setAttribute('data-line', lineId);
            const commentText = `# ${raw}`;
            newRow.innerHTML = `
                <span class="step-number"><span class="comment-hash-badge">#</span></span>
                <div class="step-content" contenteditable="true" spellcheck="false" data-raw="${commentText.replace(/"/g, '&quot;')}"
                     style="color: var(--text-secondary, #64748b); font-style: italic;"
                     onkeydown="handleKeyDown(event, ${lineId})"
                     onfocus="handleStepFocus(${lineId})"
                     onblur="handleStepBlur(${lineId})"
                     oninput="if(typeof checkAndUpdateCommentRow==='function') checkAndUpdateCommentRow(this);">${commentText}</div>
                <div class="step-actions">
                    <button type="button" class="icon-btn btn-move-up" title="Move Step Up" onclick="moveStep(${lineId}, -1)"><span class="material-symbols-outlined">keyboard_arrow_up</span></button>
                    <button type="button" class="icon-btn btn-move-down" title="Move Step Down" onclick="moveStep(${lineId}, 1)"><span class="material-symbols-outlined">keyboard_arrow_down</span></button>
                    <button type="button" class="icon-btn danger btn-delete-step" title="Delete Step" onclick="deleteStep(${lineId})"><span class="material-symbols-outlined">delete</span></button>
                </div>
            `;
            container.insertBefore(newRow, toggleBar);
            updateAllLineNumbers();
        }
    }
    compilePlaybookToYaml();
    checkEditorDirtyStatus();
    if (typeof debouncedPushSnapshot === 'function') {
        debouncedPushSnapshot();
    }
}
window.handleSubstepTitleBlur = handleSubstepTitleBlur;

function refreshSubstepToggleBars(container) {
    if (!container) return;

    container.querySelectorAll('.substeps-toggle-bar').forEach(bar => bar.remove());
    container.querySelectorAll('.step-row-substep').forEach(row => {
        row.classList.remove('substep-last');
    });

    const children = Array.from(container.children);
    let i = 0;
    while (i < children.length) {
        if (children[i].classList.contains('step-row-substep')) {
            const startRow = children[i];
            let count = 0;
            let lastSubstep = startRow;

            // Extract group title from preceding comment or step
            let groupTitle = 'Sub-steps';
            const prev = startRow.previousElementSibling;
            if (prev && prev.classList.contains('step-row-comment')) {
                const commentContent = prev.querySelector('.step-content');
                if (commentContent) {
                    const raw = commentContent.innerText.replace(/^#\s*/, '').trim();
                    if (raw) {
                        groupTitle = raw;
                    }
                }
            }

            while (i < children.length && children[i].classList.contains('step-row-substep')) {
                lastSubstep = children[i];
                count++;
                i++;
            }
            if (lastSubstep) {
                lastSubstep.classList.add('substep-last');
            }
            if (count > 0) {
                const toggleBar = document.createElement('div');
                toggleBar.className = 'substeps-toggle-bar substeps-group-header';
                toggleBar.setAttribute('onclick', 'toggleSubsteps(this)');

                const headerLeft = document.createElement('div');
                headerLeft.className = 'substeps-header-left';

                const chevron = document.createElement('span');
                chevron.className = 'material-symbols-outlined toggle-chevron';
                chevron.textContent = 'expand_more';

                const titleSpan = document.createElement('span');
                titleSpan.className = 'substeps-title';
                titleSpan.contentEditable = 'true';
                titleSpan.spellcheck = false;
                titleSpan.title = 'Click to rename sub-steps block';
                titleSpan.textContent = groupTitle;
                titleSpan.setAttribute('onclick', 'handleSubstepTitleClick(event, this)');
                titleSpan.setAttribute('onkeydown', 'handleSubstepTitleKeyDown(event, this)');
                titleSpan.setAttribute('onfocus', 'handleSubstepTitleFocus(this)');
                titleSpan.setAttribute('oninput', 'handleSubstepTitleInput(this)');
                titleSpan.setAttribute('onblur', 'handleSubstepTitleBlur(this)');

                headerLeft.appendChild(chevron);
                headerLeft.appendChild(titleSpan);

                const badge = document.createElement('span');
                badge.className = 'substeps-count-badge';
                badge.textContent = `${count} sub-step${count > 1 ? 's' : ''}`;

                toggleBar.appendChild(headerLeft);
                toggleBar.appendChild(badge);

                container.insertBefore(toggleBar, startRow);
            }
        } else {
            i++;
        }
    }
}
window.refreshSubstepToggleBars = refreshSubstepToggleBars;

function refreshAllSubstepToggleBars() {
    ['#beforeStepsList', '#stepsList', '#afterStepsList'].forEach(sel => {
        const container = document.querySelector(sel);
        if (container) {
            refreshSubstepToggleBars(container);
        }
    });
}
window.refreshAllSubstepToggleBars = refreshAllSubstepToggleBars;

/* Token Rendering Parser */
function formatStepToTokens(lineNum) {
    const row = document.querySelector(`.step-row[data-line="${lineNum}"]`);
    if (!row) return;
    const content = row.querySelector('.step-content');
    if (!content) return;

    const rawText = getStepRawText(content);
    if (!rawText) return;
    content.setAttribute('data-raw', rawText);

    checkAndUpdateCommentRow(row);

    if (row.classList.contains('step-row-comment')) {
        content.innerText = rawText;
        return;
    }

    const inlineCommentIdx = findInlineCommentIndex(rawText);
    const instructionPart = inlineCommentIdx >= 0 ? rawText.substring(0, inlineCommentIdx) : rawText;
    const commentPart = inlineCommentIdx >= 0 ? rawText.substring(inlineCommentIdx) : '';

    let safeText = instructionPart.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    if (row.classList.contains('step-row-substep')) {
        safeText = safeText.replace(/^(?:-\s*)?&gt;-\s*/, '');
    } else {
        safeText = safeText.replace(/(^|\s)(?:-\s*)?&gt;-(?=\s|$)/g, '$1<span class="substep-folded-badge">&gt;-</span>');
    }

    safeText = safeText.replace(/(#[a-zA-Z0-9_\-]+)/g, '<span class="hint-badge">$1</span>');

    safeText = safeText.replace(/_include:\s*([a-zA-Z0-9_\/\.\-]*missing[a-zA-Z0-9_\/\.\-]*)/g, function(match, path) {
        return `<span class="missing-include-pill" data-raw="${match.replace(/"/g, '&quot;')}" contenteditable="false"><span class="material-symbols-outlined pill-icon">error</span>Missing Include: ${path}</span>`;
    });

    safeText = safeText.replace(/_include:\s*([a-zA-Z0-9_\/\.\-]+)/g, function(match, path) {
        const filename = path.split('/').pop();
        return `<span class="unified-include-pill" data-raw="${match.replace(/"/g, '&quot;')}" contenteditable="false"><span class="material-symbols-outlined pill-icon">extension</span><span>include: ${filename}</span><span class="material-symbols-outlined hover-arrow" onmousedown="handleArrowMouseDown(event, '${lineNum}', '${path}')" title="Toggle steps for ${filename}">expand_more</span></span>`;
    });

    safeText = safeText.replace(/\$\{([a-zA-Z0-9_\.]+)\}/g, function(match, name) {
        return `<span class="var-pill" data-var="${name}" contenteditable="false"><span class="material-symbols-outlined pill-icon">data_object</span>${name}</span>`;
    });

    if (inlineCommentIdx >= 0) {
        const safeComment = commentPart.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        content.innerHTML = safeText + `<span class="step-inline-comment">${safeComment}</span>`;
    } else {
        content.innerHTML = safeText;
    }
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
                list.push({
                    text: content.getAttribute('data-raw') || content.innerText.trim(),
                    isSubstep: row.classList.contains('step-row-substep'),
                    isManualSubstep: row.getAttribute('data-manual-substep') === 'true',
                    isComment: row.classList.contains('step-row-comment')
                });
            }
        });
        return list;
    };

    let activeLine = null;
    let activeCaret = 0;
    const activeEl = document.activeElement;
    if (activeEl && activeEl.classList.contains('step-content')) {
        const row = activeEl.closest('.step-row');
        if (row) {
            activeLine = row.getAttribute('data-line');
            if (typeof getCaretOffset === 'function') {
                activeCaret = getCaretOffset(activeEl);
            }
        }
    }

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
        matrixData,
        activeLine,
        activeCaret
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
        (stepArray || []).forEach((stepItem, idx) => {
            const lineNum = idx + 1;
            const stepText = (typeof stepItem === 'object' && stepItem !== null) ? (stepItem.text || '') : (stepItem || '');
            const isSubstep = (typeof stepItem === 'object' && stepItem !== null) ? Boolean(stepItem.isSubstep) : false;
            const isManualSubstep = (typeof stepItem === 'object' && stepItem !== null) ? Boolean(stepItem.isManualSubstep) : false;
            const isComment = (typeof stepItem === 'object' && stepItem !== null) ? Boolean(stepItem.isComment) : false;

            const row = document.createElement('div');
            row.className = 'step-row' + (isSubstep ? ' step-row-substep' : '') + (isComment ? ' step-row-comment' : '');
            if (isManualSubstep) {
                row.setAttribute('data-manual-substep', 'true');
            }
            row.setAttribute('data-line', lineNum);
            row.innerHTML = `
                <span class="step-number">${lineNum}</span>
                <div class="step-content" contenteditable="true" spellcheck="false"
                     data-raw="${stepText.replace(/"/g, '&quot;')}"
                     onkeydown="handleKeyDown(event, ${lineNum})"
                     onfocus="handleStepFocus(${lineNum})"
                     onblur="handleStepBlur(${lineNum})"
                     oninput="if(typeof checkAndUpdateCommentRow==='function') checkAndUpdateCommentRow(this);"></div>
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

    if (snap.activeLine) {
        const row = document.querySelector(`.step-row[data-line="${snap.activeLine}"]`);
        if (row) {
            const content = row.querySelector('.step-content');
            if (content) {
                content.focus();
                if (typeof setCaretOffset === 'function' && typeof snap.activeCaret === 'number') {
                    setCaretOffset(content, snap.activeCaret);
                }
            }
        }
    }
}
window.restoreEditorSnapshot = restoreEditorSnapshot;

function editorUndo() {
    clearTimeout(undoDebounceTimer);
    undoDebounceTimer = null;

    const currentSnap = getEditorSnapshot();
    const lastSnap = undoStack.length > 0 ? undoStack[undoStack.length - 1] : null;

    if (lastSnap && JSON.stringify(lastSnap) !== JSON.stringify(currentSnap)) {
        redoStack.push(currentSnap);
        restoreEditorSnapshot(lastSnap);
        return;
    }

    if (undoStack.length <= 1) return;
    const currentPopped = undoStack.pop();
    redoStack.push(currentPopped);
    const prevSnap = undoStack[undoStack.length - 1];
    restoreEditorSnapshot(prevSnap);
}
window.editorUndo = editorUndo;

function editorRedo() {
    clearTimeout(undoDebounceTimer);
    undoDebounceTimer = null;
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

    // Do not intercept paste in the Raw YAML code editor panel (preserve original formatting and newlines)
    const isRaw = (typeof currentEditorMode !== 'undefined' && currentEditorMode === 'raw')
        || (document.getElementById('rawEditorPanel') && document.getElementById('rawEditorPanel').style.display !== 'none');
    if (isRaw || (evt.target && (evt.target.id === 'rawYamlTextarea' || (evt.target.closest && evt.target.closest('#rawEditorPanel'))))) {
        return;
    }

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
    } else if (evt.target.getAttribute && (evt.target.getAttribute('contenteditable') === 'true' || evt.target.tagName === 'INPUT')) {
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

/* Aura AI Step Review Integration */
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
                            <span><b>Aura AI Review:</b> ${sugg.message}</span>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px;">
                            ${sugg.hintText ? `<button class="btn-apply-suggestion" onclick="applySuggestionHint(${sugg.targetLine}, '${sugg.hintText.replace(/'/g, "\\'")}')">Apply Hint</button>` : ''}
                            <button class="icon-btn" onclick="dismissSuggestionCard(this)" title="Dismiss"><span class="material-symbols-outlined">close</span></button>
                        </div>
                    `;
                    targetRow.after(card);
                }
            });
            showToast(`✨ Aura AI Review complete: ${data.suggestions.length} suggestion(s) found`, "info");
        } else {
            showToast("✅ Aura AI Review complete: All steps look great! No suggestions needed.", "success");
        }
    })
    .catch(err => {
        console.error('Failed to run Aura AI review:', err);
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



