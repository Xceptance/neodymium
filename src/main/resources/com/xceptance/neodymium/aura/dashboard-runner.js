// ============================================================================
// Neodymium Aura Dashboard - Test Execution & Status Polling Engine
// ============================================================================

var wasInLiveRunView = false;
var lastKnownRunning = false;
var pollingIntervalId = null;
var lastLogIndex = 0;
var lastEventIndex = 0;
var currentPollSession = 0;
var serverSessionId = null;

window.wasInLiveRunView = wasInLiveRunView;
window.lastKnownRunning = lastKnownRunning;

function openInteractiveConsoleViewLive(url, skipTestListRender = false) {
    const placeholder = document.getElementById('historyPlaceholder');
    if (placeholder) placeholder.style.display = 'none';

    const miniStrip = document.getElementById('miniHistoryStrip');
    if (miniStrip) miniStrip.style.display = 'none';

    let targetUrl = url;
    if (url.includes('interactive_console.html') && !url.includes('dataUrl=')) {
        const buster = activeRunStats.activeTestId || activeRunStats.activeFile || Date.now();
        targetUrl = url + (url.includes('?') ? '&' : '?') + 't=' + encodeURIComponent(buster);
    }
    const iframe = document.getElementById('historyConsoleIframe');
    if (iframe) iframe.src = targetUrl;

    currentReportId = null;
    window.currentReportId = null;
    wasInLiveRunView = true;
    window.wasInLiveRunView = true;
    if (typeof renderHistoryTable === 'function') renderHistoryTable();
    if (typeof showView === 'function') showView('reportViewContainer');

    requestAnimationFrame(() => {
        if (typeof applyHistoryState === 'function') applyHistoryState(4);
        if (!skipTestListRender) {
            renderLiveTestList();
        }
    });
}
window.openInteractiveConsoleViewLive = openInteractiveConsoleViewLive;

function renderLiveTestList() {
    const testsList = document.getElementById('historyTestsList');
    if (!testsList) return;

    let liveTests = activeRunStats.tests || [];
    if (liveTests.length === 0 && activeRunStats.activeFile) {
        liveTests = [{ file: activeRunStats.activeFile, id: null }];
    }

    if (liveTests.length === 0) {
        testsList.innerHTML = `<div style="text-align: center; color: var(--text-secondary); font-style: italic; padding: 20px;">No tests available.</div>`;
        return;
    }

    const activeFile = activeRunStats.activeFile || '';
    const activeTestId = activeRunStats.activeTestId || '';

    let activeIdx = -1;
    if (activeFile) {
        if (activeTestId) {
            const normalizeId = (id) => {
                if (!id) return '';
                let s = String(id).trim().toLowerCase();
                if (s.startsWith('dataset ')) {
                    s = s.substring(8).trim();
                }
                return s;
            };
            const normActive = normalizeId(activeTestId);
            activeIdx = liveTests.findIndex(t => t.file === activeFile && t.id && normalizeId(t.id) === normActive);
        }
        if (activeIdx < 0) {
            activeIdx = liveTests.findIndex(t => t.file === activeFile);
        }
    }

    testsList.innerHTML = liveTests.map((t, idx) => {
        const isCurrent = (activeIdx >= 0) ? (idx === activeIdx) : (t.file === activeFile);
        const isDone = liveCompletedFiles.has(t.file) || (activeIdx >= 0 && idx < activeIdx);
        const isPending = !isDone && !isCurrent;

        let cardStyle = '';
        let iconHtml = '';
        let onclick = '';
        let cursor = 'pointer';

        if (isCurrent) {
            cardStyle = 'background-color: var(--bg-hover); border-left: 3px solid var(--accent);';
            iconHtml = `<i class="fa-solid fa-circle-notch fa-spin" style="color: var(--accent); margin-left: 6px; flex-shrink: 0;"></i>`;
            onclick = `onclick="openInteractiveConsoleViewLive('/interactive_console.html')"`;
        } else if (isDone) {
            cardStyle = 'opacity: 0.80;';
            iconHtml = `<i class="fa-solid fa-circle-check" style="color: var(--success, #22c55e); margin-left: 6px; flex-shrink: 0; font-size: 12px;"></i>`;
            onclick = `onclick="openInteractiveConsoleViewLive('/interactive_console.html')"`;
        } else {
            cardStyle = 'opacity: 0.40; pointer-events: none;';
            iconHtml = `<i class="fa-regular fa-clock" style="color: var(--text-secondary); margin-left: 6px; flex-shrink: 0; font-size: 12px;"></i>`;
            cursor = 'default';
        }

        const label = t.file ? t.file.replace(/\\/g, '/').split('/').pop() : t.file;
        
        let datasetPart = '';
        if (t.id) {
            const isNumeric = /^\d+$/.test(String(t.id).trim());
            datasetPart = isNumeric ? ` · Dataset ${t.id}` : ` · ${t.id}`;
        }

        const cardClass = isCurrent ? 'test-card active' : 'test-card';
        return `<div class="${cardClass}" style="cursor: ${cursor}; ${cardStyle}" ${onclick}>
            <div class="test-card-title-row" style="display: flex; justify-content: space-between; align-items: center;">
                <span class="test-card-label" style="word-break: break-all; flex: 1;" title="${t.file}">${label}${datasetPart}</span>
                <div style="display: flex; align-items: center; gap: 4px; flex-shrink: 0;">
                    ${iconHtml}
                </div>
            </div>
        </div>`;
    }).join('');

    requestAnimationFrame(() => {
        const activeCard = testsList.querySelector('.test-card.active');
        if (activeCard) {
            activeCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    });
}
window.renderLiveTestList = renderLiveTestList;

function openCurrentRunView() {
    wasInLiveRunView = true;
    window.wasInLiveRunView = true;
    const colRuns = document.getElementById('colRuns');
    if (colRuns) {
        colRuns.classList.remove('col-minimized');
        colRuns.style.width = '320px';
    }

    const colTests = document.getElementById('colTests');
    const historyResizer2 = document.getElementById('historyResizer2');
    if (colTests) {
        colTests.classList.remove('col-minimized');
        colTests.style.display = 'flex';
        colTests.style.width = '280px';
    }
    if (historyResizer2) historyResizer2.style.display = 'block';

    renderLiveTestList();
    openInteractiveConsoleViewLive('/interactive_console.html');
}
window.openCurrentRunView = openCurrentRunView;

function updateRunButtons() {
    const runQueueBtn = document.getElementById('runQueueBtn');
    const runCurrentTestBtn = document.getElementById('runCurrentTestBtn');
    const stopQueueBtn = document.getElementById('stopQueueBtn');
    if (!runQueueBtn || !stopQueueBtn) return;

    if (isRunning) {
        runQueueBtn.style.display = 'none';
        if (runCurrentTestBtn) runCurrentTestBtn.style.display = 'none';
        stopQueueBtn.style.display = 'flex';
    } else {
        stopQueueBtn.style.display = 'none';
        runQueueBtn.style.display = 'flex';
        if (activeEditingFile && runCurrentTestBtn) {
            runCurrentTestBtn.style.display = 'flex';
        } else if (runCurrentTestBtn) {
            runCurrentTestBtn.style.display = 'none';
        }
    }
}
window.updateRunButtons = updateRunButtons;

function stopQueue() {
    fetch('/api/stop', { method: 'POST' }).catch(e => console.error('Failed to stop queue', e));
    isRunning = false;
    window.isRunning = false;
    updateRunButtons();
}
window.stopQueue = stopQueue;

function prepareClientForExecution() {
    isRunning = true;
    consoleOpened = true;
    window.isRunning = true;
    window.consoleOpened = true;
    currentPollSession++;
    lastLogIndex = 0;
    lastEventIndex = 0;

    activeRunStats.running = true;
    activeRunStats.startTime = new Date();
    activeRunStats.total = 0;
    activeRunStats.passed = 0;
    activeRunStats.failed = 0;
    activeRunStats.skipped = 0;
    activeRunStats.activeFile = null;
    activeRunStats.activeTestId = null;
    activeRunStats.tests = [];
    liveCompletedFiles.clear();
    liveLastActiveFile = null;
    if (typeof renderHistoryTable === 'function') renderHistoryTable();

    const runSpinner = document.getElementById('runSpinner');
    const terminalConsole = document.getElementById('terminalConsole');
    if (runSpinner) runSpinner.style.display = 'inline-block';
    if (typeof updateCenterLayout === 'function') updateCenterLayout();
    updateRunButtons();
    if (terminalConsole) terminalConsole.innerHTML = 'Connecting to run stream...\n';
    hasShownStartMessage = false;
}
window.prepareClientForExecution = prepareClientForExecution;

async function rerunFullRun(runConfigJson) {
    if (isRunning) return;
    let payload;
    try {
        payload = JSON.parse(runConfigJson);
    } catch (e) {
        if (typeof showToast === 'function') showToast('Invalid run configuration — cannot rerun.', 'error');
        return;
    }

    prepareClientForExecution();
    activeRunStats.tests = (payload.datasets || []).map(d => ({ file: d.file, id: d.id }));

    try {
        const res = await fetch('/api/run', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        if (data.error) {
            if (typeof showToast === 'function') showToast('Re-run failed to start: ' + data.error, 'error');
            isRunning = false;
            window.isRunning = false;
            activeRunStats.running = false;
            if (typeof renderHistoryTable === 'function') renderHistoryTable();
            consoleOpened = false;
            window.consoleOpened = false;
            if (typeof updateCenterLayout === 'function') updateCenterLayout();
        }
    } catch (e) {
        if (typeof showToast === 'function') showToast('Failed to start re-run: ' + e.message, 'error');
        isRunning = false;
        window.isRunning = false;
        activeRunStats.running = false;
        if (typeof renderHistoryTable === 'function') renderHistoryTable();
        consoleOpened = false;
        window.consoleOpened = false;
        if (typeof updateCenterLayout === 'function') updateCenterLayout();
    }
}
window.rerunFullRun = rerunFullRun;

async function rerunSingleTest(payloadJson) {
    await rerunFullRun(payloadJson);
}
window.rerunSingleTest = rerunSingleTest;

function isAiLogLine(line) {
    if (!line) return false;

    const lower = line.toLowerCase();

    const isMavenLog = lower.includes('[info] command: mvn') ||
        lower.includes('[info] --<') ||
        lower.includes('[info] building ') ||
        lower.includes('[info] --- maven-') ||
        lower.includes('[info] skip non existing resource') ||
        lower.includes('[info] deleting ');
    if (isMavenLog) return false;

    const emojiPattern = /[🤖🧠👣🔮⚙️✨🔍║│├└─═▶✅❌\?]/u;
    if (emojiPattern.test(line)) {
        return true;
    }

    const aiKeywords = [
        'aura',
        'pesap',
        'playbook',
        'llmclient',
        'aiagent',
        'actionexecutor',
        'aibrowser',
        'aiassertion',
        'reasoning',
        'token usage',
        'execution statistics',
        'llm calls',
        'input tokens',
        'output tokens',
        'total tokens',
        'context levels',
        'escalations',
        'direct parses',
        'step',
        'open http',
        'navigating to',
        'capturing',
        'sending prompt',
        'sending multimodal',
        'sending chat message',
        'tokens:',
        'llm response',
        'llm reasoning',
        'parsed',
        'llm proposed actions',
        'action{type=',
        '[exec]',
        'executing action',
        'resolved using strategy',
        'assertion passed',
        'all steps completed successfully',
        'playbook saved',
        'navigating to:',
        'simplified dom size'
    ];

    if (aiKeywords.some(keyword => lower.includes(keyword))) {
        return true;
    }

    return false;
}
window.isAiLogLine = isAiLogLine;

function toggleAiOnly(checked) {
    logFilterAiOnly = checked;
    window.logFilterAiOnly = logFilterAiOnly;
    refreshLogFiltering();
}
window.toggleAiOnly = toggleAiOnly;

function toggleErrorsOnly(checked) {
    logFilterErrorsOnly = checked;
    window.logFilterErrorsOnly = logFilterErrorsOnly;
    refreshLogFiltering();
}
window.toggleErrorsOnly = toggleErrorsOnly;

function refreshLogFiltering() {
    const terminalConsole = document.getElementById('terminalConsole');
    if (!terminalConsole) return;
    const logLines = terminalConsole.querySelectorAll('.log-line');
    logLines.forEach(line => {
        const text = line.innerText;
        const isErrorOrWarn = text.includes('[ERROR]') || text.includes('[WARN]') || text.includes('[FATAL]');

        const matchesErrorFilter = !logFilterErrorsOnly || isErrorOrWarn;
        const matchesAiFilter = !logFilterAiOnly || isAiLogLine(text);

        if (matchesErrorFilter && matchesAiFilter) {
            line.style.display = 'block';
        } else {
            line.style.display = 'none';
        }
    });
}
window.refreshLogFiltering = refreshLogFiltering;

function appendLog(line) {
    const terminalConsole = document.getElementById('terminalConsole');
    if (!terminalConsole) return;
    if (terminalConsole.innerHTML.includes('Console idle.') || terminalConsole.innerHTML.includes('Connecting to run stream...')) {
        terminalConsole.innerHTML = '';
        if (logFilterAiOnly && !hasShownStartMessage) {
            hasShownStartMessage = true;
            window.hasShownStartMessage = true;
            const startLine = '🤖 Test runner started. Compiling and initializing AI tests (this may take a few seconds)...';
            terminalConsole.insertAdjacentHTML('beforeend', `<div class="log-line log-info" style="display: block; margin: 0; padding: 0;">${startLine}</div>`);
        }
    }
    const isErrorOrWarn = line.includes('[ERROR]') || line.includes('[WARN]') || line.includes('[FATAL]');

    const matchesErrorFilter = !logFilterErrorsOnly || isErrorOrWarn;
    const matchesAiFilter = !logFilterAiOnly || isAiLogLine(line);

    const displayStyle = (matchesErrorFilter && matchesAiFilter) ? 'block' : 'none';
    const escapedLine = line.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    const cssClass = isErrorOrWarn ? 'log-error' : 'log-info';

    terminalConsole.insertAdjacentHTML('beforeend', `<div class="log-line ${cssClass}" style="display: ${displayStyle}; margin: 0; padding: 0;">${escapedLine}</div>`);
    terminalConsole.scrollTop = terminalConsole.scrollHeight;
}
window.appendLog = appendLog;

function copyTerminalOutput() {
    const terminalConsole = document.getElementById('terminalConsole');
    if (!terminalConsole) return;

    const logLines = terminalConsole.querySelectorAll('.log-line');
    let textToCopy = '';

    if (logLines.length > 0) {
        const visibleLines = [];
        logLines.forEach(line => {
            if (line.style.display !== 'none' && window.getComputedStyle(line).display !== 'none') {
                visibleLines.push(line.innerText || line.textContent);
            }
        });
        textToCopy = visibleLines.join('\n');
    } else {
        textToCopy = terminalConsole.innerText || terminalConsole.textContent;
    }

    const copyIcon = document.getElementById('copyTerminalIcon');
    const copyText = document.getElementById('copyTerminalText');

    const showFeedback = () => {
        if (copyIcon && copyText) {
            copyIcon.className = 'fa-solid fa-check';
            copyText.textContent = 'Copied!';
            setTimeout(() => {
                copyIcon.className = 'fa-regular fa-copy';
                copyText.textContent = 'Copy';
            }, 2000);
        }
    };

    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(textToCopy).then(showFeedback).catch(() => {
            fallbackCopyText(textToCopy, showFeedback);
        });
    } else {
        fallbackCopyText(textToCopy, showFeedback);
    }
}
window.copyTerminalOutput = copyTerminalOutput;

function fallbackCopyText(text, callback) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
        document.execCommand('copy');
        if (callback) callback();
    } catch (err) {
        console.error('Fallback copy failed', err);
    }
    document.body.removeChild(textArea);
}

function startPolling() {
    if (pollingIntervalId) {
        clearTimeout(pollingIntervalId);
    }
    pollStatus();
}
window.startPolling = startPolling;

async function pollStatus() {
    let thisSession = currentPollSession;
    try {
        const response = await fetch('/api/status?clientId=' + encodeURIComponent(clientId) + '&lastIndex=' + lastLogIndex + '&lastEventIndex=' + lastEventIndex);
        if (thisSession !== currentPollSession) return;

        if (response.ok) {
            const data = await response.json();

            if (data.logs && data.logs.length > 0) {
                data.logs.forEach(log => appendLog(log));
            }
            if (data.newIndex !== undefined) {
                lastLogIndex = data.newIndex;
            }

            if (data.events && data.events.length > 0) {
                data.events.forEach(event => {
                    if (event.type === 'reportReady') {
                        if (typeof openReportView === 'function') openReportView(event.reportId);
                    }
                    if (event.type === 'interactiveConsoleReady') {
                        openInteractiveConsoleViewLive(event.url);
                    }
                });
            }
            if (data.newEventIndex !== undefined) {
                lastEventIndex = data.newEventIndex;
            }

            if (data.status) {
                const statusData = data.status;

                if (statusData.tests) {
                    activeRunStats.tests = statusData.tests;
                }
                if (statusData.completedFiles) {
                    liveCompletedFiles = new Set(statusData.completedFiles);
                    window.liveCompletedFiles = liveCompletedFiles;
                }

                if (serverSessionId === null) {
                    serverSessionId = statusData.sessionId;
                } else if (serverSessionId !== statusData.sessionId) {
                    window.location.reload();
                    return;
                }

                const statsPanel = document.getElementById('statsPanel');
                if (statsPanel) {
                    statsPanel.style.display = (statusData.running || statusData.total > 0) ? 'block' : 'none';
                }

                const elTotal = document.getElementById('statsTotalExecution');
                const elPassed = document.getElementById('statsPassed');
                const elFailed = document.getElementById('statsFailed');
                const elSkipped = document.getElementById('statsSkipped');
                if (elTotal) elTotal.innerText = statusData.total;
                if (elPassed) elPassed.innerText = statusData.passed;
                if (elFailed) elFailed.innerText = statusData.failed;
                if (elSkipped) elSkipped.innerText = statusData.skipped || 0;

                activeRunStats.running = statusData.running;
                activeRunStats.total = statusData.total;
                activeRunStats.passed = statusData.passed;
                activeRunStats.failed = statusData.failed;
                activeRunStats.skipped = statusData.skipped || 0;
                if (statusData.running && !activeRunStats.startTime) {
                    activeRunStats.startTime = new Date();
                }
                const currentStatsStr = `${activeRunStats.running}:${activeRunStats.total}:${activeRunStats.passed}:${activeRunStats.failed}:${activeRunStats.skipped}:${historyCached.length}`;
                if (window._lastStatsStr !== currentStatsStr) {
                    if (typeof renderHistoryTable === 'function') renderHistoryTable();
                    window._lastStatsStr = currentStatsStr;
                }

                try {
                    isRunning = statusData.running;
                    window.isRunning = isRunning;
                    updateRunButtons();
                    const runSpinner = document.getElementById('runSpinner');
                    if (isRunning) {
                        lastKnownRunning = true;
                        window.lastKnownRunning = true;
                        if (activeRunStats.activeFile !== statusData.activeFile || activeRunStats.activeTestId !== statusData.activeTestId) {
                            if (activeRunStats.activeFile && activeRunStats.activeFile !== statusData.activeFile) {
                                liveCompletedFiles.add(activeRunStats.activeFile);
                            }
                            activeRunStats.activeFile = statusData.activeFile;
                            activeRunStats.activeTestId = statusData.activeTestId;
                            const colTests = document.getElementById('colTests');
                            if (colTests && colTests.style.display === 'flex' && currentReportId === null) {
                                renderLiveTestList();
                                openInteractiveConsoleViewLive('/interactive_console.html', true);
                            }
                        }

                        if (runSpinner) runSpinner.style.display = 'inline-block';
                        const sidebarBadge = document.getElementById('sidebarRunBadge');
                        if (sidebarBadge) {
                            sidebarBadge.style.display = 'flex';
                            sidebarBadge.innerHTML = `<i class="fa-solid fa-circle-notch spinner"></i> ${statusData.passed + statusData.failed + (statusData.skipped || 0) + 1}/${statusData.tests.length}`;
                        }
                    } else {
                        if (runSpinner) runSpinner.style.display = 'none';
                        const sidebarBadge = document.getElementById('sidebarRunBadge');
                        if (sidebarBadge) sidebarBadge.style.display = 'none';

                        activeRunStats.running = false;
                        activeRunStats.startTime = null;

                        if (wasInLiveRunView) {
                            const icView = document.getElementById('interactiveConsoleView');
                            const rContainer = document.getElementById('reportViewContainer');
                            const isInteractiveViewOpen = icView && icView.style.display === 'flex';
                            const isLiveReportViewOpen = rContainer && rContainer.style.display === 'flex' && currentReportId === null;
                            if (isInteractiveViewOpen || isLiveReportViewOpen) {
                                if (typeof showView === 'function') showView('dashboardView');
                            }
                            wasInLiveRunView = false;
                            window.wasInLiveRunView = false;
                        }

                        if (lastKnownRunning) {
                            if (typeof loadHistory === 'function') loadHistory();
                            if (typeof updateCenterLayout === 'function') updateCenterLayout();
                        }
                    }
                    lastKnownRunning = statusData.running;
                    window.lastKnownRunning = lastKnownRunning;
                } catch (err) {
                    console.error('Error updating UI state from status:', err);
                }
            }
            const apiKeyBanner = document.getElementById('apiKeyBanner');
            if (apiKeyBanner && apiKeyBanner.style.display === 'block' && document.getElementById('apiKeyBannerText').innerHTML.includes('offline')) {
                apiKeyBanner.style.display = 'none';
            }
        } else {
            console.error("Polling response error:", response.status);
            const apiKeyBanner = document.getElementById('apiKeyBanner');
            const apiKeyBannerText = document.getElementById('apiKeyBannerText');
            if (apiKeyBanner && apiKeyBannerText) {
                apiKeyBanner.style.display = 'block';
                apiKeyBannerText.innerHTML = "Server is offline or unreachable.";
            }
        }
    } catch (err) {
        console.error("Polling fetch error:", err);
        const apiKeyBanner = document.getElementById('apiKeyBanner');
        const apiKeyBannerText = document.getElementById('apiKeyBannerText');
        if (apiKeyBanner && apiKeyBannerText) {
            apiKeyBanner.style.display = 'block';
            apiKeyBannerText.innerHTML = "Server is offline or unreachable.";
        }
    } finally {
        if (thisSession !== currentPollSession) return;
        if (!disconnected) {
            pollingIntervalId = setTimeout(pollStatus, 2000);
        }
    }
}
window.pollStatus = pollStatus;

window.addEventListener('beforeunload', (event) => {
    if (isRunning) {
        event.preventDefault();
        event.returnValue = 'A test queue is currently running. If you close this tab, the server will shut down and the test execution will be terminated.';
        return event.returnValue;
    }
});

function sendDisconnect() {
    if (!disconnected) {
        disconnected = true;
        window.disconnected = true;
        navigator.sendBeacon('/api/disconnect?clientId=' + encodeURIComponent(clientId));
    }
}
window.sendDisconnect = sendDisconnect;

window.addEventListener('message', (event) => {
    if (event.data && event.data.action === 'stepSelected') {
        if (historyNavState === 3) {
            if (typeof applyHistoryState === 'function') applyHistoryState(4);
        }
    } else if (event.data && event.data.action === 'rerunTest') {
        if (event.data.testId) {
            const testFile = event.data.testId.split('.')[0] + '.yaml';
            if (typeof runTestByFile === 'function') runTestByFile(testFile);
            if (typeof showToast === 'function') showToast("Triggered rerun for " + testFile, "info");
        }
    }
});

window.addEventListener('pagehide', sendDisconnect);
window.addEventListener('unload', sendDisconnect);
