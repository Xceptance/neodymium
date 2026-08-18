// ============================================================================
// Neodymium Aura Dashboard - History & Reporting Layout State Machine
// ============================================================================

function applyHistoryState(n) {
    historyNavState = n;
    window.historyNavState = n;
    const colRuns = document.getElementById('colRuns');
    const colTests = document.getElementById('colTests');
    const colReport = document.getElementById('colReport');
    const colRunsMini = document.getElementById('colRunsMini');
    const colTestsMini = document.getElementById('colTestsMini');
    const r1 = document.getElementById('historyResizer1');
    const r2 = document.getElementById('historyResizer2');
    const r3 = document.getElementById('historyResizer3');
    if (!colRuns || !colTests || !colReport) return;

    [colRuns, colTests, colReport].forEach(el => {
        el.style.display = 'none';
        el.style.flex = '';
        el.style.flexGrow = '';
        el.style.flexShrink = '';
        el.style.flexBasis = '';
        el.style.width = '';
        el.style.minWidth = '';
        el.classList.remove('col-minimized');
    });
    if (colRunsMini) colRunsMini.style.display = 'none';
    if (colTestsMini) colTestsMini.style.display = 'none';
    if (r1) r1.style.display = 'none';
    if (r2) r2.style.display = 'none';
    if (r3) r3.style.display = 'none';

    switch (n) {
        case 1:
            colRuns.style.display = 'flex';
            colRuns.style.flex = '1 1 0%';
            break;

        case 2:
            colRuns.style.display = 'flex';
            colRuns.style.flex = '1 1 0%';
            colTests.style.display = 'flex';
            colTests.style.flex = '1 1 0%';
            if (r1) r1.style.display = 'block';
            break;

        case 3:
            colRuns.style.display = 'flex';
            colRuns.style.flex = '1 1 0%';
            colTests.style.display = 'flex';
            colTests.style.flex = '1 1 0%';
            colReport.style.display = 'flex';
            colReport.style.flex = '1 1 0%';
            if (r1) r1.style.display = 'block';
            if (r2) r2.style.display = 'block';
            break;

        case 4:
            colTests.style.display = 'flex';
            colTests.style.flex = '1 1 0%';
            colReport.style.display = 'flex';
            colReport.style.flex = '2 1 0%';
            if (colRunsMini) colRunsMini.style.display = 'flex';
            if (r2) r2.style.display = 'block';
            break;
    }

    refreshMiniRunsBar();
    refreshMiniTestsBar();
}
window.applyHistoryState = applyHistoryState;

function refreshMiniRunsBar() {
    const bar = document.getElementById('colRunsMini');
    if (!bar) return;

    let html = '<div class="col-runs-mini-header">Runs</div>';

    if (activeRunStats && activeRunStats.running) {
        html += `<div class="mini-run-chip running selected" title="Running · Live Execution" onclick="if(typeof openCurrentRunView==='function')openCurrentRunView()"><span class="chip-number">#—</span><span class="chip-dot"></span></div>`;
    }

    if (historyCached && historyCached.length > 0) {
        historyCached.forEach(run => {
            let statusClass = 'failed';
            if (run.status === 'Passed') {
                statusClass = 'passed';
            } else if (run.status === 'Aborted') {
                statusClass = 'aborted';
            }
            const isCurrent = run.id === currentReportId;
            const runNum = run.runNumber || '?';
            const durationStr = run.durationMs ? ` · ${(run.durationMs / 1000).toFixed(1)}s` : '';
            const title = `#${runNum} · ${run.status || ''}${durationStr}`;
            
            html += `<div class="mini-run-chip ${statusClass}${isCurrent ? ' selected' : ''}" title="${title}" onclick="onMiniRunChipClick('${run.id}')"><span class="chip-number">#${runNum}</span><span class="chip-dot"></span></div>`;
        });
    }

    bar.innerHTML = html;
}
window.refreshMiniRunsBar = refreshMiniRunsBar;

function refreshMiniTestsBar() {
    const bar = document.getElementById('colTestsMini');
    if (!bar) return;

    let html = '<div class="col-runs-mini-header">Tests</div>';
    const run = historyCached.find(r => r.id === currentReportId);
    const tests = (run && run.tests) ? run.tests : [];

    if (tests.length > 0) {
        tests.forEach(t => {
            let statusClass = 'failed';
            if (t.status === 'Passed') {
                statusClass = 'passed';
            } else if (t.status === 'Aborted') {
                statusClass = 'aborted';
            }
            const isCurrent = t.file === currentTestFile;
            const shortLabel = t.yamlLabel || t.testId || (t.name ? t.name.substring(0, 6) : '?');
            const title = `${t.yamlLabel || t.name || t.testId || ''} · ${t.status || ''}`;
            
            html += `<div class="mini-run-chip ${statusClass}${isCurrent ? ' selected' : ''}" title="${title}" onclick="if('${t.file}'==='${currentTestFile}'){applyHistoryState(3);}else{loadInteractiveTest('${currentReportId}','${t.file}','${t.name}',null);}"><span class="chip-number" style="font-size:8px;word-break:break-all;line-height:1.1;">${shortLabel}</span><span class="chip-dot"></span></div>`;
        });
    }

    bar.innerHTML = html;
}
window.refreshMiniTestsBar = refreshMiniTestsBar;

function selectHistoryRun(reportId) {
    currentReportId = reportId;
    window.currentReportId = currentReportId;
    htmx.ajax('GET', '/api/reporting/run?id=' + encodeURIComponent(reportId), {
        target: '#colTests',
        swap: 'outerHTML'
    }).then(() => {
        applyHistoryState(2);
    });
}
window.selectHistoryRun = selectHistoryRun;

function onMiniRunChipClick(runId) {
    if (runId === currentReportId) {
        applyHistoryState(3);
    } else {
        selectHistoryRun(runId);
    }
}
window.onMiniRunChipClick = onMiniRunChipClick;

function initResizers() {
    setupResizer('historyResizer1', 'colRuns', 160, false, () => {
        if (historyNavState === 3) {
            applyHistoryState(4);
        } else if (historyNavState === 2) {
            applyHistoryState(1);
        }
    }, null);

    setupResizer('historyResizer2', 'colTests', 140, false, () => {
        if (historyNavState === 3) {
            applyHistoryState(4);
        }
    }, null);

    setupResizer('historyResizer3', 'colReport', 300, true, null, null);
}
window.initResizers = initResizers;

function setupResizer(resizerId, colId, minWidth, rightSide, onCollapse, onResize) {
    const resizer = document.getElementById(resizerId);
    const col = document.getElementById(colId);
    if (!resizer || !col) return;

    let startX = 0;
    let startW = 0;
    let collapsed = false;

    resizer.addEventListener('mousedown', (e) => {
        e.preventDefault();
        startX = e.clientX;
        startW = col.getBoundingClientRect().width;
        collapsed = false;

        resizer.classList.add('dragging');
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';

        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    });

    function onMouseMove(e) {
        const delta = rightSide ? -(e.clientX - startX) : (e.clientX - startX);
        const newW = startW + delta;
        if (onCollapse && newW < minWidth) {
            if (!collapsed) {
                collapsed = true;
                cleanup();
                onCollapse(newW);
            }
        } else if (newW >= (onCollapse ? minWidth : 0)) {
            col.style.flex = 'none';
            col.style.width = newW + 'px';
            if (onResize) onResize(newW);
        }
    }

    function onMouseUp() { cleanup(); }

    function cleanup() {
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
        resizer.classList.remove('dragging');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
    }
}
window.setupResizer = setupResizer;

window.addEventListener('message', (e) => {
    if (e.data && e.data.action === 'stepSelected' && historyNavState === 3) {
        applyHistoryState(4);
    }
});

function formatHistoryTimestamp(ts) {
    if (!ts || ts.length < 15) return ts;
    try {
        const year = parseInt(ts.substring(0, 4));
        const month = parseInt(ts.substring(4, 6)) - 1;
        const day = parseInt(ts.substring(6, 8));
        const hour = parseInt(ts.substring(9, 11));
        const minute = parseInt(ts.substring(11, 13));
        const second = parseInt(ts.substring(13, 15));

        const date = new Date(year, month, day, hour, minute, second);
        if (!isNaN(date.getTime())) {
            return date.toLocaleString();
        }
    } catch (e) {
        // fallback
    }
    return ts;
}
window.formatHistoryTimestamp = formatHistoryTimestamp;

async function loadHistory() {
    try {
        const res = await fetch('/api/reporting/history-json');
        historyCached = await res.json();
        window.historyCached = historyCached;
        refreshMiniRunsBar();
        refreshMiniTestsBar();
    } catch (e) {
        console.error("Failed to load reporting history", e);
    }
}
window.loadHistory = loadHistory;

function renderHistoryTable() {
    if (window.location.pathname !== '/history') return;
    htmx.ajax('GET', '/api/reporting/history', { target: '#allureHistoryList', swap: 'outerHTML' }).then(() => {
        loadHistory();
    });
}
window.renderHistoryTable = renderHistoryTable;

function loadInteractiveTest(reportId, testFile, testName, rowElement) {
    currentReportId = reportId;
    currentTestFile = testFile;
    window.currentReportId = currentReportId;
    window.currentTestFile = currentTestFile;

    const testsList = document.getElementById('historyTestsList');
    if (testsList) {
        testsList.querySelectorAll('.test-card').forEach(card => card.classList.remove('selected'));
    }
    if (rowElement) {
        rowElement.classList.add('selected');
    }

    const placeholder = document.getElementById('historyPlaceholder');
    if (placeholder) placeholder.style.display = 'none';

    const reportIframe = document.getElementById('historyReportIframe');
    const consoleIframe = document.getElementById('historyConsoleIframe');
    const miniStrip = document.getElementById('miniHistoryStrip');

    if (miniStrip) miniStrip.style.display = 'none';

    const buster = reportId || Date.now();
    const targetFile = testFile || currentTestFile || 'console-execution-1.json';
    let reportUrl = `/api/reporting/report/${reportId}/allure-report/index.html?t=${encodeURIComponent(buster)}`;
    let consoleUrl = `/interactive_console.html?dataUrl=${encodeURIComponent(`/api/reporting/report/${reportId}/${targetFile}`)}&t=${encodeURIComponent(buster)}`;

    if (reportIframe) {
        reportIframe.style.display = 'block';
        reportIframe.src = reportUrl;
    }
    if (consoleIframe) {
        consoleIframe.src = consoleUrl;
    }

    applyHistoryState(3);
}
window.loadInteractiveTest = loadInteractiveTest;

function openLogModal(title, content) {
    const titleEl = document.getElementById('logModalTitle');
    const contentEl = document.getElementById('logModalContent');
    const modal = document.getElementById('logModal');
    if (titleEl) titleEl.innerText = title;
    if (contentEl) contentEl.innerText = content;
    if (modal) modal.style.display = 'flex';
}
window.openLogModal = openLogModal;

function closeLogModal() {
    const modal = document.getElementById('logModal');
    if (modal) modal.style.display = 'none';
}
window.closeLogModal = closeLogModal;

function openRawLogModal(reportId) {
    fetch(`/api/reporting/report/${reportId}/execution.log`)
        .then(r => r.text())
        .then(text => openLogModal(`Execution Log (${reportId})`, text))
        .catch(e => showToast("Failed to fetch log", "error"));
}
window.openRawLogModal = openRawLogModal;

function openAiLogModal(reportId) {
    fetch(`/api/reporting/report/${reportId}/execution.log`)
        .then(r => r.text())
        .then(text => {
            const lines = text.split('\n');
            const filtered = lines.filter(l => typeof isAiLogLine === 'function' && isAiLogLine(l)).join('\n');
            openLogModal(`AI Execution Log (${reportId})`, filtered || "No AI logs found.");
        })
        .catch(e => showToast("Failed to fetch log", "error"));
}
window.openAiLogModal = openAiLogModal;

function deleteHistoryReport(reportId, btn) {
    if (!confirm(`Are you sure you want to delete report ${reportId}?`)) return;
    htmx.ajax('POST', `/api/reporting/delete?id=${encodeURIComponent(reportId)}`, { target: '#allureHistoryList', swap: 'outerHTML' })
        .then(() => {
            showToast(`Report ${reportId} deleted`, 'info');
            if (currentReportId === reportId) {
                applyHistoryState(1);
            }
            loadHistory();
        });
}
window.deleteHistoryReport = deleteHistoryReport;
