
        // Toast Notification System
        function showToast(message, type = 'info') {
            const container = document.getElementById('toastContainer');
            if (!container) return;
            const toast = document.createElement('div');
            toast.className = `toast ${type}`;
            let iconClass = 'fa-info-circle';
            if (type === 'success') iconClass = 'fa-check-circle';
            else if (type === 'error') iconClass = 'fa-exclamation-circle';
            toast.innerHTML = `<i class="fa-solid ${iconClass}"></i> <span>${message}</span>`;
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

        // Layout Resizers
        let isResizingH = false;
        let isResizingV = false;
        let isResizingH1 = false;
        let isResizingH2 = false;

        document.getElementById('sidebarResizer').addEventListener('mousedown', function (e) {
            isResizingH = true;
            document.body.style.cursor = 'ew-resize';
            e.preventDefault();
        });

        document.getElementById('consoleResizer').addEventListener('mousedown', function (e) {
            isResizingV = true;
            document.body.style.cursor = 'ns-resize';
            e.preventDefault();
        });

        // historyResizer1 and historyResizer2 are now handled exclusively by
        // initResizers() / setupResizer(). No duplicate mousedown handlers here.


        document.addEventListener('mousemove', function (e) {
            if (isResizingH) {
                const workspace = document.querySelector('.workspace-layout');
                const workspaceRect = workspace.getBoundingClientRect();
                // calculate right area width
                const newWidth = workspaceRect.right - e.clientX - 16;
                if (newWidth > 200 && newWidth < workspaceRect.width * 0.6) {
                    document.getElementById('rightArea').style.width = newWidth + 'px';
                }
            }
            if (isResizingV) {
                const consolePanel = document.getElementById('consolePanel');
                if (consolePanel.style.display !== 'none') {
                    const workspace = document.querySelector('.workspace-layout');
                    const workspaceRect = workspace.getBoundingClientRect();
                    const newHeight = workspaceRect.bottom - e.clientY - 16;
                    if (newHeight > 100 && newHeight < workspaceRect.height * 0.8) {
                        consolePanel.style.height = newHeight + 'px';
                    }
                }
            }
            // isResizingH1 / isResizingH2 removed — handled by setupResizer()
        });

        document.addEventListener('mouseup', function (e) {
            if (isResizingH || isResizingV) {
                isResizingH = false;
                isResizingV = false;
                document.body.style.cursor = 'default';
            }
        });


        function changeTheme(theme) {
            localStorage.setItem('aura_theme', theme);
            applyTheme(theme);
            fetch('/api/theme?theme=' + encodeURIComponent(theme), { method: 'POST' }).catch(() => {});
        }

        function switchState(stateName) {
            document.querySelectorAll('.view-state').forEach(el => el.classList.remove('active'));
            document.getElementById('state-' + stateName).classList.add('active');
        }

        function showView(viewId) {
            document.getElementById('dashboardView').style.display = 'none';
            document.getElementById('reportViewContainer').style.display = 'none';
            document.getElementById('reportView').style.display = 'none';
            const icView = document.getElementById('interactiveConsoleView');
            if (icView) icView.style.display = 'none';

            document.getElementById(viewId).style.display = 'flex';

            // Re-apply the current history nav state whenever the history tab becomes
            // visible. This is necessary because applyHistoryState() is called at
            // init() time when the container is still display:none, meaning the
            // browser has not computed any layout — re-applying after the container
            // is shown guarantees correct column widths.
            if (viewId === 'reportViewContainer') {
                if (historyNavState === 4) {
                    historyNavState = 1;
                }
                requestAnimationFrame(() => applyHistoryState(historyNavState));
            }

            if (viewId !== 'reportView' && viewId !== 'interactiveConsoleView') {
                lastActiveView = viewId;
            }

            const navWorkspace = document.getElementById('navWorkspace');
            const navReports = document.getElementById('navReports');

            if (viewId === 'dashboardView') {
                if (navWorkspace) navWorkspace.classList.add('active');
                if (navReports) navReports.classList.remove('active');
            } else if (viewId === 'reportViewContainer') {
                if (navWorkspace) navWorkspace.classList.remove('active');
                if (navReports) navReports.classList.add('active');
            } else if (viewId === 'reportView' || viewId === 'interactiveConsoleView') {
                if (lastActiveView === 'reportViewContainer') {
                    if (navWorkspace) navWorkspace.classList.remove('active');
                    if (navReports) navReports.classList.add('active');
                } else {
                    if (navWorkspace) navWorkspace.classList.add('active');
                    if (navReports) navReports.classList.remove('active');
                }
            }
        }

        // DOM Elements
        const dashboardView = document.getElementById('dashboardView');
        const reportView = document.getElementById('reportView');

        const getEditorFileName = () => document.getElementById('editorFileName');
        const getEditorContent = () => document.getElementById('editorContent');
        const reportDisplayName = document.getElementById('reportDisplayName');
        const reportIframe = document.getElementById('reportIframe');

        const createTestModal = document.getElementById('createTestModal');
        const openModalBtn = document.getElementById('openModalBtn');
        const closeModalBtn = document.getElementById('closeModalBtn');
        const newTestName = document.getElementById('newTestName');

        const runQueueBtn = document.getElementById('runQueueBtn');
        const stopQueueBtn = document.getElementById('stopQueueBtn');
        const queueListContainer = document.getElementById('queueListContainer');
        let isRunning = false;
        let logFilterErrorsOnly = false;
        let logFilterAiOnly = true;
        let hasShownStartMessage = false;
        const terminalConsole = document.getElementById('terminalConsole');
        const runSpinner = document.getElementById('runSpinner');
        const yamlFileList = document.getElementById('yamlFileList');
        const reportingHistoryList = document.getElementById('reportingHistoryList');

        // State variables
        const clientId = 'c-' + Math.random().toString(36).substring(2) + '-' + Date.now().toString(36);
        let disconnected = false;
        let selectedDatasets = []; // [{ file, id }]
        let expandedFiles = {};
        let currentFilesListCached = [];
        let chatSessions = [];
        let currentSessionId = null;
        let activeEditingFile = null;
        let consoleOpened = false;
        let conversationHistory = [];
        let lastActiveView = 'dashboardView';

        function init() {
            if (window.location.search.includes('test=true')) {
                window.confirm = () => true;
                window.prompt = (msg, defaultText) => defaultText || "Mocked Input";
            }
            scrollToBottom();
            loadFiles();
            loadHistory();
            startPolling();
            initResizers();
            // Start history view in State 1 (full-width runs list)
            applyHistoryState(1);

            function onEditorPanelSwapped() {
                const fileSpan = document.getElementById('editorFileName');
                if (fileSpan && fileSpan.textContent && fileSpan.textContent.trim() !== 'test.yaml' && fileSpan.textContent.trim() !== '') {
                    activeEditingFile = fileSpan.textContent.trim();
                } else {
                    activeEditingFile = null;
                }
                updateCenterLayout();
            }

            document.addEventListener('htmx:oobAfterSwap', function(evt) {
                if (evt.detail && evt.detail.target) {
                    htmx.process(evt.detail.target);
                    if (evt.detail.target.id === 'editorPanel') {
                        onEditorPanelSwapped();
                    }
                }
            });

            document.addEventListener('htmx:afterSwap', function(evt) {
                if (evt.detail.target.id === 'yamlFileList') {
                    syncCheckboxesFromState();
                } else if (evt.detail.target.id === 'editorPanel') {
                    onEditorPanelSwapped();
                } else if (evt.detail.target.id === 'queueListContainer') {
                    syncStateFromQueueContainer();
                } else if (evt.detail.target.id === 'colTests') {
                    applyHistoryState(2);
                } else if (evt.detail.target.id === 'colReport') {
                    applyHistoryState(3);
                }
            });

            document.addEventListener('htmx:afterRequest', function(evt) {
                const path = evt.detail.pathInfo ? evt.detail.pathInfo.requestPath : '';
                if (path === '/api/reporting/delete' || path === '/api/reporting/history') {
                    loadHistory();
                    updateCenterLayout();
                }
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
                
                const banner = document.getElementById('warningBanner');
                const bannerText = document.getElementById('warningBannerText');
                if (banner && bannerText) {
                    bannerText.innerHTML = errorMsg;
                    banner.style.display = 'block';
                    setTimeout(() => {
                        banner.style.display = 'none';
                    }, 6000);
                }
            });

            syncStateFromQueueContainer();

            // Listen for AI Action triggers sent from server-side htmx responses
            document.body.addEventListener('aiAction', async function(evt) {
                const data = evt.detail;
                if (!data) return;

                if (data.action === 'select_tests') {
                    if (data.selectedDatasets) {
                        selectedDatasets = data.selectedDatasets;
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
                    }
                    await loadFiles();
                    updateQueueList();
                } else if (data.action === 'edit_or_create_test' && data.filename && data.content) {
                    await loadFiles();
                    activeEditingFile = data.filename;
                    getEditorFileName().innerText = data.filename;
                    getEditorContent().value = data.content;
                    updateCenterLayout();
                    getEditorContent().focus();
                }
            });

            // Listen for fileSaved trigger sent from server-side HTMX responses
            document.body.addEventListener('fileSaved', function(evt) {
                const detail = evt.detail;
                if (detail && detail.file) {
                    showToast(`💾 ${detail.file} successfully saved to disk!`, "success");
                }
            });

            // Auto-scroll chat to bottom after swap
            document.body.addEventListener('htmx:afterSwap', function(evt) {
                if (evt.detail.target.id === 'chatMessages' || evt.detail.target.id === 'chatContainer') {
                    scrollToBottom();
                }
            });

            // Clean up temporary thinking bubble if request fails
            document.body.addEventListener('htmx:responseError', function(evt) {
                const thinkingBubbles = document.querySelectorAll('.thinking-bubble');
                thinkingBubbles.forEach(el => el.remove());
            });

            // Enforce mutual exclusivity: Headless Mode vs Interactive HUD
            const optHeadless = document.getElementById('optHeadless');
            const optInteractive = document.getElementById('optInteractive');

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
        }

        // Modal Controls
        document.addEventListener('click', function(e) {
            const btn = e.target.closest('#openModalBtn');
            if (btn) {
                const modal = document.getElementById('createTestModal');
                const nameInput = document.getElementById('newTestName');
                if (modal) modal.style.display = 'flex';
                if (nameInput) {
                    nameInput.value = '';
                    nameInput.focus();
                }
            }
        });
        if (closeModalBtn) {
            closeModalBtn.onclick = () => {
                createTestModal.style.display = 'none';
            };
        }
        if (newTestName) {
            newTestName.onkeydown = (event) => {
                if (event.key === 'Enter') {
                    submitCreateTest();
                }
            };
        }


        function isDatasetSelected(file, id) {
            return selectedDatasets.some(d => d.file === file && d.id === id);
        }

        function isAllDatasetsChecked(fileDto) {
            if (!fileDto.datasets || fileDto.datasets.length === 0) return false;
            return fileDto.datasets.every(d => isDatasetSelected(fileDto.file, d.id));
        }

        function toggleSelectDataset(file, id, checked) {
            htmx.ajax('POST', '/api/queue/toggle?file=' + encodeURIComponent(file) + '&id=' + encodeURIComponent(id), { target: '#queueListContainer', swap: 'outerHTML' });
        }

        function toggleSelectAllDatasets(file, checked) {
            htmx.ajax('POST', '/api/queue/toggleAll?file=' + encodeURIComponent(file) + '&checked=' + checked, { target: '#queueListContainer', swap: 'outerHTML' });
        }

        function toggleExpandFile(file) {
            const list = document.getElementById('datasets-' + file);
            if (list) {
                const isHidden = list.style.display === 'none';
                list.style.display = isHidden ? 'flex' : 'none';
                
                // Find list item to update chevron icon
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
            // Sync expansion state on the server in the background without re-rendering HTML
            fetch('/api/files/toggle?file=' + encodeURIComponent(file), { method: 'POST' });
        }

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

        function syncCheckboxesFromState() {
            const fileCbs = document.querySelectorAll('.file-select-cb');
            fileCbs.forEach(cb => {
                const file = cb.getAttribute('data-file');
                const fileDto = currentFilesListCached.find(f => f.file === file);
                if (fileDto) {
                    cb.checked = isAllDatasetsChecked(fileDto);
                }
            });

            const datasetCbs = document.querySelectorAll('.dataset-select-cb');
            datasetCbs.forEach(cb => {
                const file = cb.getAttribute('data-file');
                const id = cb.getAttribute('data-id');
                cb.checked = isDatasetSelected(file, id);
            });
        }

        function loadFilesList(files) {
            syncCheckboxesFromState();
            updateQueueList();
        }

        let historyCached = [];
        let activeRunStats = { total: 0, passed: 0, failed: 0, skipped: 0, running: false, startTime: null, activeTestId: null };
        /**
         * Tracks which test files have already completed in the current live run.
         * Keyed by the full file path string. Updated whenever the backend's
         * activeFile changes to a new entry, marking the previous file as done.
         */
        let liveCompletedFiles = new Set();
        /** The last activeFile we saw from the poll — used to detect transitions. */
        let liveLastActiveFile = null;

        // ── History navigation state machine ─────────────────────────────────
        // State 1: full-width runs list only
        // State 2: runs (fixed) | tests (flex-grow)
        // State 3: runs (fixed) | tests (fixed) | details (flex-grow)
        // State 4: mini-runs bar (48px) | tests (fixed) | details (flex-grow, wide)
        let historyNavState = 1;
        let savedRunsWidth = 280;   // remembered when collapsing to mini-bar
        let savedTestsWidth = 240;   // remembered across state transitions

        /**
         * Applies one of the four layout states to the history view columns.
         * All column show/hide and width management is centralised here to
         * avoid scattered inline style mutations across multiple functions.
         *
         * @param {number} n - target state (1–4)
         */
        function applyHistoryState(n) {
            historyNavState = n;
            const colRuns = document.getElementById('colRuns');
            const colTests = document.getElementById('colTests');
            const colReport = document.getElementById('colReport');
            const colRunsMini = document.getElementById('colRunsMini');
            const colTestsMini = document.getElementById('colTestsMini');
            const r1 = document.getElementById('historyResizer1');
            const r2 = document.getElementById('historyResizer2');
            const r3 = document.getElementById('historyResizer3');
            if (!colRuns || !colTests || !colReport) return;

            // ── Reset: start from a fully clean slate ────────────────────────────
            // Set display:none on ALL columns first so CSS class rules (.left-area
            // { display:flex }) cannot accidentally make columns visible. Then clear
            // every flex/sizing inline style so the incoming state's values are the
            // sole source of truth.
            [colRuns, colTests, colReport].forEach(el => {
                el.style.display = 'none';
                el.style.flex = '';
                el.style.flexGrow = '';
                el.style.flexShrink = '';
                el.style.flexBasis = '';
                el.style.width = '';
                el.style.minWidth = '';
                // Remove any col-minimized class that a prior code path may have added;
                // it carries !important width rules that survive inline-style resets.
                el.classList.remove('col-minimized');
            });
            if (colRunsMini) colRunsMini.style.display = 'none';
            if (colTestsMini) colTestsMini.style.display = 'none';
            if (r1) r1.style.display = 'none';
            if (r2) r2.style.display = 'none';
            if (r3) r3.style.display = 'none';

            switch (n) {
                case 1:
                    // ── State 1: full-width runs list only ──────────────────────
                    colRuns.style.display = 'flex';
                    colRuns.style.flex = '1 1 0%';
                    // colTests and colReport remain display:none
                    break;

                case 2:
                    // ── State 2: Runs | Tests — equal 50/50 split ───────────────
                    colRuns.style.display = 'flex';
                    colRuns.style.flex = '1 1 0%';
                    colTests.style.display = 'flex';
                    colTests.style.flex = '1 1 0%';
                    // colReport remains display:none
                    if (r1) r1.style.display = 'block';
                    break;

                case 3:
                    // ── State 3: Runs | Tests | Details — equal three-way split ─
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
                    // ── State 4: Mini-runs (48px) | Tests (flex:1) | Details (flex:2)
                    // colRuns full panel is hidden; colRunsMini strip takes its place.
                    // Tests keeps its normal proportional size; details gets more room.
                    colTests.style.display = 'flex';
                    colTests.style.flex = '1 1 0%';
                    colReport.style.display = 'flex';
                    colReport.style.flex = '2 1 0%';
                    if (colRunsMini) colRunsMini.style.display = 'flex';
                    if (r2) r2.style.display = 'block';
                    refreshMiniRunsBar();
                    break;
            }
        }

        /**
         * Populates the mini-runs vertical chip bar with one chip per archived run.
         * The currently loaded run is highlighted with an accent border.
         */
        function refreshMiniRunsBar() {
            const bar = document.getElementById('colRunsMini');
            if (!bar) return;
            const header = bar.querySelector('.col-runs-mini-header');
            bar.innerHTML = '';
            if (header) bar.appendChild(header);

            if (activeRunStats.running) {
                const chip = document.createElement('div');
                chip.className = 'mini-run-chip running selected';
                chip.title = 'Running \u00b7 Live Execution';
                chip.innerHTML = `<span class="chip-number">#—</span><span class="chip-dot"></span>`;
                chip.addEventListener('click', () => openCurrentRunView());
                bar.appendChild(chip);
            }

            historyCached.forEach(run => {
                let statusClass = 'failed';
                if (run.status === 'Passed') {
                    statusClass = 'passed';
                } else if (run.status === 'Aborted') {
                    statusClass = 'aborted';
                }
                const isCurrent = run.id === currentReportId;
                const chip = document.createElement('div');
                chip.className = 'mini-run-chip ' + statusClass + (isCurrent ? ' selected' : '');
                chip.title = '#' + (run.runNumber || '?') + ' \u00b7 ' + (run.status || '') + (run.durationMs ? ' \u00b7 ' + (run.durationMs / 1000).toFixed(1) + 's' : '');
                chip.innerHTML = `<span class="chip-number">#${run.runNumber || '?'}</span><span class="chip-dot"></span>`;
                chip.addEventListener('click', () => onMiniRunChipClick(run.id));
                bar.appendChild(chip);
            });
        }

        /**
         * Populates the mini-tests vertical chip bar (State 4) with one chip per test
         * in the currently loaded run. Clicking a chip re-opens that test in State 3.
         */
        function refreshMiniTestsBar() {
            const bar = document.getElementById('colTestsMini');
            if (!bar) return;
            const header = bar.querySelector('.col-runs-mini-header');
            bar.innerHTML = '';
            if (header) bar.appendChild(header);
            const run = historyCached.find(r => r.id === currentReportId);
            const tests = (run && run.tests) ? run.tests : [];
            tests.forEach(t => {
                let statusClass = 'failed';
                if (t.status === 'Passed') {
                    statusClass = 'passed';
                } else if (t.status === 'Aborted') {
                    statusClass = 'aborted';
                }
                const isCurrent = t.file === currentTestFile;
                const chip = document.createElement('div');
                chip.className = 'mini-run-chip ' + statusClass + (isCurrent ? ' selected' : '');
                // Short label: use yamlLabel or testId if available, otherwise first 6 chars of name
                const shortLabel = t.yamlLabel || t.testId || (t.name ? t.name.substring(0, 6) : '?');
                chip.title = (t.yamlLabel || t.name || t.testId || '') + ' \u00b7 ' + (t.status || '');
                chip.innerHTML = `<span class="chip-number" style="font-size:8px;word-break:break-all;line-height:1.1;">${shortLabel}</span><span class="chip-dot"></span>`;
                chip.addEventListener('click', () => {
                    if (isCurrent) {
                        // Same test: expand back to State 3
                        applyHistoryState(3);
                    } else {
                        // Different test: load it and go to State 3
                        loadInteractiveTest(currentReportId, t.file, t.name, null);
                    }
                });
                bar.appendChild(chip);
            });
        }

        /**
         * Handles a click on a mini-run chip in the State-4 sidebar.
         * - Same run as currently loaded → expand back to State 3.
         * - Different run → switch to State 2 for that run (user can then select a test).
         *
         * @param {string} runId - the ID of the clicked run
         */
        function selectHistoryRun(reportId) {
            currentReportId = reportId;
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

        /**
         * Initialises drag-to-resize behaviour for both history column resizers.
         * Dragging a resizer below its auto-collapse threshold triggers a state
         * transition instead of rendering an unusably narrow column.
         *
         * Thresholds:
         *   historyResizer1 (colRuns)  < 160px → State 4 (from State 3) / State 1 (from State 2)
         *   historyResizer2 (colTests) < 140px → State 4 mini-tests bar (from State 3)
         *   historyResizer3            free drag only (details pane, no collapse)
         */
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
                // In State 2: tests column is flex-grow so dragging left will shrink it naturally
            }, null);

            // Resizer3 is to the RIGHT of the mini-bars and the LEFT edge of colReport.
            // Dragging it LEFT enlarges colReport; pass rightSide=true to invert delta.
            // No auto-collapse: the details pane must always remain visible.
            setupResizer('historyResizer3', 'colReport', 300, true, null, null);
        }

        /**
         * Sets up mousedown drag behaviour on a resizer element, resizing the
         * target column. Triggers onCollapse when width drops below minWidth.
         *
         * @param {string}   resizerId  - ID of the resizer div
         * @param {string}   colId      - ID of the column being resized
         * @param {number}   minWidth   - auto-collapse threshold in pixels
         * @param {boolean}  rightSide  - true if the column is to the RIGHT of the resizer
         *                               (inverts the delta direction)
         * @param {Function} onCollapse - called when width drops below minWidth (or null)
         * @param {Function} onResize   - called on every valid resize with new width (or null)
         */
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
                startW = col.offsetWidth;
                collapsed = false;
                resizer.classList.add('dragging');
                document.body.style.cursor = 'col-resize';
                document.body.style.userSelect = 'none';
                document.addEventListener('mousemove', onMouseMove);
                document.addEventListener('mouseup', onMouseUp);
            });

            function onMouseMove(e) {
                // rightSide=true: column is right of resizer — drag right shrinks it, drag left grows it
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

        // ── postMessage listener ─────────────────────────────────────────────
        // interactive_console.js already fires postMessage({ action: 'stepSelected' })
        // whenever the user clicks a step card (line 1092). We intercept it here
        // to trigger the State-3 → State-4 transition (widen details pane).
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

        async function loadHistory() {
            try {
                const res = await fetch('/api/reporting/history-json');
                historyCached = await res.json();
            } catch (e) {
                console.error("Failed to load reporting history", e);
            }
        }

        let currentReportId = null;

        function renderHistoryTable() {
            htmx.ajax('GET', '/api/reporting/history', { target: '#allureHistoryList', swap: 'outerHTML' });
        }

        let currentTestFile = null;

        window.loadInteractiveTest = function(reportId, testFile, testName, rowElement) {
            currentReportId = reportId;
            currentTestFile = testFile;

            // Highlight selection in the test list using CSS class
            const testsList = document.getElementById('historyTestsList');
            if (testsList) {
                testsList.querySelectorAll('.test-card').forEach(card => card.classList.remove('selected'));
            }
            if (rowElement) {
                rowElement.classList.add('selected');
            }

            // Update column header with selected run context
            const run = historyCached.find(r => r.id === reportId);
            const runNum = run && run.runNumber ? '#' + run.runNumber : '';
            const runLabel = runNum ? ` - Run ${runNum}` : '';
            const detailsHeader = document.getElementById('historyDetailsHeader');
            if (detailsHeader) {
                detailsHeader.textContent = `Test Case Details${runLabel}`;
            }

            const placeholder = document.getElementById('historyPlaceholder');
            if (placeholder) placeholder.style.display = 'none';
            const iframe = document.getElementById('historyConsoleIframe');
            if (iframe) {
                const dataUrl = '/api/reporting/report/' + reportId + '/' + testFile;
                iframe.src = '/interactive_console.html?dataUrl=' + encodeURIComponent(dataUrl);
            }

            applyHistoryState(3);

            // Populate the mini history strip with bubbles for this test across all runs
            renderMiniHistoryStrip(testName, reportId);
        };

        /**
         * Renders the mini history strip inside the Test Case Details panel.
         * Shows a bubble per run where the given testName appeared, newest first.
         * Clicking a bubble loads that run's replay.
         *
         * @param {string} testName      - the test name to match across runs
         * @param {string} activeRunId   - the currently displayed run (highlighted bubble)
         */
        function renderMiniHistoryStrip(testName, activeRunId) {
            const strip = document.getElementById('miniHistoryStrip');
            const bubblesContainer = document.getElementById('miniHistoryBubbles');
            if (!strip || !bubblesContainer) return;

            // Collect all runs that contain this test name, newest first (historyCached is already desc)
            const matchingRuns = [];
            for (const run of historyCached) {
                if (!run.tests) continue;
                const match = run.tests.find(t => t.name === testName);
                if (match) {
                    matchingRuns.push({ run, test: match });
                }
            }

            if (matchingRuns.length === 0) {
                strip.style.display = 'none';
                return;
            }

            strip.style.display = 'flex';

            bubblesContainer.innerHTML = matchingRuns.map(({ run, test }) => {
                let statusClass = 'failed';
                let bubbleBadgeClass = 'badge-failed';
                let bubbleBadgeLabel = 'Failed';
                if (test.status === 'Passed') {
                    statusClass = 'passed';
                    bubbleBadgeClass = 'badge-success';
                    bubbleBadgeLabel = 'Passed';
                } else if (test.status === 'Aborted') {
                    statusClass = 'aborted';
                    bubbleBadgeClass = 'badge-aborted';
                    bubbleBadgeLabel = 'Aborted';
                }
                const runNum = run.runNumber ? '#' + run.runNumber : '';
                const dur = test.durationMs ? (test.durationMs / 1000).toFixed(1) + 's' : '';
                const isActive = run.id === activeRunId;
                const selectedClass = isActive ? ' selected' : '';
                // Log button shown inline in bubble if a per-test log exists
                const logBtnHtml = (test.hasLog === 'true' || test.hasLog === true)
                    ? `<button class="mini-log-btn" onclick="event.stopPropagation(); openTestLog('${run.id}', '${testName}')" title="View test log"><i class="fa-solid fa-file-lines"></i></button>`
                    : '';

                return `<span class="mini-run-bubble ${statusClass}${selectedClass}"
                            onclick="loadInteractiveTest('${run.id}', '${test.file}', '${testName}', null)"
                            title="Run ${runNum} \u00b7 ${test.status}${dur ? ' \u00b7 ' + dur : ''}">
                            <span class="run-number" style="font-size:10px;">${runNum}</span>
                            <span class="${bubbleBadgeClass}" style="font-size:9px; padding:1px 4px;">${bubbleBadgeLabel}</span>
                            ${dur ? `<span style="font-size:10px; opacity:0.75;">${dur}</span>` : ''}${logBtnHtml}
                        </span>`;
            }).join('');
        }

        /**
         * Opens the per-test-case log file in the log modal.
         * The file is fetched from /api/allure/report/{runId}/{sanitizedTestName}.log
         *
         * @param {string} runId    - the report directory ID
         * @param {string} testName - the human-readable test name
         */
        async function openTestLog(runId, testName) {
            const safeLogName = testName.replace(/[^a-zA-Z0-9_\-]/g, '_') + '.log';
            const modal = document.getElementById('logModal');
            const content = document.getElementById('logModalContent');
            const downloadBtn = document.getElementById('downloadLogBtn');
            content.innerText = 'Loading...';
            modal.style.display = 'flex';
            try {
                const res = await fetch(`/api/reporting/report/${runId}/${safeLogName}`);
                if (res.ok) {
                    const text = await res.text();
                    content.innerText = text || '(empty log)';
                    downloadBtn.onclick = () => {
                        const blob = new Blob([text], { type: 'text/plain' });
                        const url = URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = safeLogName;
                        a.click();
                        URL.revokeObjectURL(url);
                    };
                } else {
                    content.innerText = 'Per-test log not available for this run.';
                    downloadBtn.onclick = null;
                }
            } catch (e) {
                content.innerText = 'Error loading test log: ' + e.message;
                downloadBtn.onclick = null;
            }
        }

        async function deleteReport(reportId) {
            if (confirm(`Are you sure you want to delete report "${reportId}"?`)) {
                try {
                    const res = await fetch('/api/reporting/delete', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ id: reportId })
                    });
                    const data = await res.json();
                    if (data.success) {
                        loadHistory();
                        updateCenterLayout();
                    } else {
                        showToast("Error deleting report: " + data.error, "error");
                    }
                } catch (e) {
                    showToast("Error deleting report: " + e.message, "error");
                }
            }
        }

        async function openLogModal(reportId) {
            const modal = document.getElementById('logModal');
            const content = document.getElementById('logModalContent');
            const downloadBtn = document.getElementById('downloadLogBtn');
            content.innerText = 'Loading...';
            modal.style.display = 'flex';
            try {
                const res = await fetch(`/api/reporting/report/${reportId}/execution.log`);
                if (res.ok) {
                    const text = await res.text();
                    content.innerText = text;
                    downloadBtn.onclick = () => {
                        const blob = new Blob([text], { type: 'text/plain' });
                        const url = URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `${reportId}_execution.log`;
                        a.click();
                        URL.revokeObjectURL(url);
                    };
                    downloadBtn.style.display = 'flex';
                } else {
                    content.innerText = 'Log file not found or empty.';
                    downloadBtn.style.display = 'none';
                }
            } catch (e) {
                content.innerText = 'Failed to load log.';
                downloadBtn.style.display = 'none';
            }
        }

        function closeLogModal() {
            document.getElementById('logModal').style.display = 'none';
        }

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
                    runQueueBtn.style.opacity = '1';
                    runQueueBtn.style.cursor = 'pointer';
                    runQueueBtn.style.filter = 'none';
                } else {
                    runQueueBtn.setAttribute('disabled', 'disabled');
                    runQueueBtn.style.opacity = '0.5';
                    runQueueBtn.style.cursor = 'not-allowed';
                    runQueueBtn.style.filter = 'grayscale(1)';
                }
            }
            updateRunButtons();

            syncCheckboxesFromState();
        }

        function updateQueueList() {
            syncStateFromQueueContainer();
        }

        function moveQueueItem(index, direction) {
            htmx.ajax('POST', '/api/queue/move?index=' + index + '&direction=' + (direction === -1 ? 'up' : 'down'), { target: '#queueListContainer', swap: 'innerHTML' });
        }


        function updateHiddenSessionId(val) {
            const hidden = document.getElementById('hiddenSessionId');
            if (hidden) {
                hidden.value = val;
            }
        }

        function setChatPrompt(promptText) {
            const input = document.getElementById('chatInput');
            if (input) {
                input.value = promptText;
                const form = input.closest('form');
                if (form) {
                    if (typeof htmx !== 'undefined') {
                        htmx.trigger(form, 'submit');
                    } else {
                        form.submit();
                    }
                }
            }
        }

        function clearChatInput() {
            const input = document.getElementById('chatInput');
            const prompt = input.value.trim();
            if (!prompt) return;

            const chatMessages = document.getElementById('chatMessages');
            if (chatMessages) {
                const userBubble = document.createElement('div');
                userBubble.className = 'chat-message user message-user';
                userBubble.innerHTML = `<div class="chat-message-header"><div class="avatar user-avatar"><i class="fa-solid fa-user"></i></div><span class="sender">You</span></div><div class="text">${escapeHtml(prompt)}</div>`;
                chatMessages.appendChild(userBubble);

                const thinkingBubble = document.createElement('div');
                thinkingBubble.className = 'chat-message ai message-ai thinking-bubble';
                thinkingBubble.innerHTML = `
                    <div class="chat-message-header"><div class="avatar ai-avatar"><i class="fa-solid fa-robot"></i></div><span class="sender">Aura Assistant</span></div>
                    <div class="typing-indicator"><span></span><span></span><span></span></div>
                `;
                chatMessages.appendChild(thinkingBubble);
                scrollToBottom();
            }

            setTimeout(() => {
                input.value = '';
            }, 50);
        }

        function scrollToBottom() {
            const chatMessages = document.getElementById('chatMessages');
            if (chatMessages) {
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }
        }

        function escapeHtml(str) {
            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
        }

        function renameCurrentChatSession() {
            const select = document.getElementById('chatSessionSelect');
            const currentId = select.value;
            const currentName = select.options[select.selectedIndex].text;
            const newName = prompt("Enter new name for this chat session:", currentName);
            if (newName && newName.trim() !== "") {
                htmx.ajax('POST', '/api/chat/rename?id=' + currentId + '&name=' + encodeURIComponent(newName.trim()), {
                    target: '#chatContainer',
                    swap: 'outerHTML'
                });
            }
        }

        function deleteCurrentChatSession() {
            const select = document.getElementById('chatSessionSelect');
            const currentId = select.value;
            if (confirm("Are you sure you want to delete this chat session?")) {
                htmx.ajax('POST', '/api/chat/delete?id=' + currentId, {
                    target: '#chatContainer',
                    swap: 'outerHTML'
                });
            }
        }

        async function submitCreateTest() {
            const name = newTestName.value.trim();
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
                    createTestModal.style.display = 'none';
                    await loadFiles();
                    await openYamlEditor(data.file);
                }
            } catch (e) {
                console.error("Failed to create test", e);
            }
        }



        let consoleExpanded = false;

        function toggleConsoleSize() {
            consoleExpanded = !consoleExpanded;
            const icon = document.getElementById('consoleSizeIcon');
            const btn = document.getElementById('toggleConsoleSizeBtn');
            if (consoleExpanded) {
                icon.className = 'fa-solid fa-chevron-down';
                btn.innerHTML = '<i class="fa-solid fa-chevron-down" id="consoleSizeIcon" aria-hidden="true"></i> Shrink';
            } else {
                icon.className = 'fa-solid fa-chevron-up';
                btn.innerHTML = '<i class="fa-solid fa-chevron-up" id="consoleSizeIcon" aria-hidden="true"></i> Expand';
            }
            updateCenterLayout();
        }

        function updateCenterLayout() {
            const hasEdit = (activeEditingFile !== null);
            const hasConsole = consoleOpened || isRunning;

            const chatPanel = document.getElementById('chatPanel');
            const editorPanel = document.getElementById('editorPanel');
            const consolePanel = document.getElementById('consolePanel');
            const consoleResizer = document.getElementById('consoleResizer');
            const closeConsoleBtn = document.getElementById('closeConsoleBtn');

            if (closeConsoleBtn) {
                closeConsoleBtn.style.display = isRunning ? 'none' : 'inline-block';
            }

            if (hasEdit) {
                document.body.classList.add('workspace-split-mode');
                switchState('editor');

                if (hasConsole) {
                    consoleResizer.style.display = 'block';
                    consolePanel.style.display = 'flex';
                    consolePanel.style.flexGrow = '0';
                    consolePanel.style.height = consoleExpanded ? '400px' : '200px';
                } else {
                    consoleResizer.style.display = 'none';
                    consolePanel.style.height = '0px'; setTimeout(() => { if (!consoleOpened && !isRunning) consolePanel.style.display = 'none'; }, 300);
                }
            } else {
                document.body.classList.remove('workspace-split-mode');
                switchState('selection');

                if (hasConsole) {
                    consoleResizer.style.display = 'block';
                    consolePanel.style.display = 'flex';
                    consolePanel.style.flexGrow = '0';
                    consolePanel.style.height = consoleExpanded ? '550px' : '280px';
                } else {
                    consoleResizer.style.display = 'none';
                    consolePanel.style.height = '0px'; setTimeout(() => { if (!consoleOpened && !isRunning) consolePanel.style.display = 'none'; }, 300);
                }
            }
        }



        function closeConsole() {
            consoleOpened = false;
            updateCenterLayout();
        }

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

        async function deleteYamlFile() {
            if (!activeEditingFile) return;
            const nameDisplay = document.getElementById('deleteFileNameDisplay');
            if (nameDisplay) nameDisplay.textContent = activeEditingFile;
            const input = document.getElementById('deleteFileNameInput');
            if (input) input.value = activeEditingFile;
            const modal = document.getElementById('deleteTestModal');
            if (modal) modal.style.display = 'flex';
        }

        async function submitDeleteTest() {
            if (!activeEditingFile) return;
            document.getElementById('deleteTestModal').style.display = 'none';
            try {
                const res = await fetch('/api/delete', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ file: activeEditingFile })
                });
                const data = await res.json();
                if (data.success) {
                    selectedDatasets = selectedDatasets.filter(d => d.file !== activeEditingFile);
                    closeEditor();
                    await loadFiles();
                } else {
                    showToast("Error deleting: " + data.error, "error");
                }
            } catch (e) {
                showToast("Error deleting file: " + e.message, "error");
            }
        }

        function closeEditor() {
            activeEditingFile = null;
            updateCenterLayout();
        }

        function openReportView(reportId) {
            reportDisplayName.innerText = reportId;
            reportIframe.src = `/api/reporting/report/${reportId}/allure-report/index.html`;

            document.body.classList.add('report-active');
            showView('reportView');
        }

        function closeReportView() {
            document.getElementById('reportIframe').src = 'about:blank';
            showView('reportViewContainer');
        }

        function openInteractiveConsoleViewLive(url, skipTestListRender = false) {
            document.getElementById('historyPlaceholder').style.display = 'none';
            // Hide mini-history strip since no specific test is selected in live view
            const miniStrip = document.getElementById('miniHistoryStrip');
            if (miniStrip) miniStrip.style.display = 'none';

            // Append dynamic cache-buster query parameter to force iframe reload/reconnect
            let targetUrl = url;
            if (url.includes('interactive_console.html') && !url.includes('dataUrl=')) {
                const buster = activeRunStats.activeTestId || activeRunStats.activeFile || Date.now();
                targetUrl = url + (url.includes('?') ? '&' : '?') + 't=' + encodeURIComponent(buster);
            }
            document.getElementById('historyConsoleIframe').src = targetUrl;
            currentReportId = null;
            // Mark that we are now watching a live run — the poll not-running branch
            // uses this flag to decide whether to redirect the user away afterwards.
            wasInLiveRunView = true;
            renderHistoryTable(); // Clear selection styling
            showView('reportViewContainer');
            // Ensure all three columns are visible so the iframe (inside colReport)
            // is actually rendered. Without this the live console loads into a
            // display:none column and the HUD is never shown to the user.
            requestAnimationFrame(() => {
                applyHistoryState(4);
                // Populate the tests column immediately — previously it was left empty
                // until the next poll cycle detected an activeFile change.
                if (!skipTestListRender) {
                    renderLiveTestList();
                }
            });
        }

        /**
         * Renders the live-run test list inside the "Tests" column.
         *
         * <p>Each entry falls into one of three visual states based on its position
         * relative to the currently executing file:
         * <ul>
         *   <li><b>Done</b> (before current, or in liveCompletedFiles): faint check icon,
         *       still clickable — navigates to the live console (best available view
         *       during an active run).</li>
         *   <li><b>Current</b> (activeFile match): accent border + spinner, clickable.</li>
         *   <li><b>Pending</b> (after current, not yet started): greyed out,
         *       cursor:default, not clickable.</li>
         * </ul>
         *
         * <p>This function is safe to call at any time; it reads all state from the
         * module-level variables {@code activeRunStats} and {@code liveCompletedFiles}.
         */
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

            // Determine the index of the currently active test so we can classify
            // all entries before it as "done" and all after it as "pending".
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

                // -- Visual state variables --
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
                    // pending
                    cardStyle = 'opacity: 0.40; pointer-events: none;';
                    iconHtml = `<i class="fa-regular fa-clock" style="color: var(--text-secondary); margin-left: 6px; flex-shrink: 0; font-size: 12px;"></i>`;
                    cursor = 'default';
                }

                // Strip long leading paths — show only the last path segment for readability
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

            // Scroll the active test card into view smoothly
            requestAnimationFrame(() => {
                const activeCard = testsList.querySelector('.test-card.active');
                if (activeCard) {
                    activeCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                }
            });
        }

        function openCurrentRunView() {
            // Mark that the user is now watching live execution output so that the
            // poll not-running branch can redirect them back to the dashboard when done.
            wasInLiveRunView = true;
            // Show Tests column, remove minified class from runs
            const colRuns = document.getElementById('colRuns');
            colRuns.classList.remove('col-minimized');
            colRuns.style.width = '320px';

            const colTests = document.getElementById('colTests');
            const historyResizer2 = document.getElementById('historyResizer2');
            colTests.classList.remove('col-minimized');
            colTests.style.display = 'flex';
            colTests.style.width = '280px';
            historyResizer2.style.display = 'block';

            // Render the test list, then open the live console iframe.
            // renderLiveTestList() is kept separate so the poll can refresh
            // the list on every activeFile transition without reopening the iframe.
            renderLiveTestList();
            openInteractiveConsoleViewLive('/interactive_console.html');
        }

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

        window.stopQueue = async function() {
            try {
                await fetch('/api/stop', { method: 'POST' });
            } catch (e) {
                console.error('Failed to stop queue', e);
            }
            isRunning = false;
            updateRunButtons();
        };

        window.prepareClientForExecution = function() {
            isRunning = true;
            consoleOpened = true;
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
            renderHistoryTable();

            if (runSpinner) runSpinner.style.display = 'inline-block';
            updateCenterLayout();
            updateRunButtons();
            if (terminalConsole) terminalConsole.innerHTML = 'Connecting to run stream...\n';
            hasShownStartMessage = false;
        };

        /**
         * Re-runs a complete historical run using its stored run configuration.
         * The runConfig is the exact RunRequest object serialized into metadata.json
         * at archive time, so it faithfully reproduces all flags and datasets.
         *
         * @param {string} runConfigJson - JSON string of the stored RunRequest
         */
        async function rerunFullRun(runConfigJson) {
            if (isRunning) return;
            let payload;
            try {
                payload = JSON.parse(runConfigJson);
            } catch (e) {
                showToast('Invalid run configuration — cannot rerun.', 'error');
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
                    showToast('Re-run failed to start: ' + data.error, 'error');
                    isRunning = false;
                    activeRunStats.running = false;
                    renderHistoryTable();
                    consoleOpened = false;
                    updateCenterLayout();
                }
            } catch (e) {
                showToast('Failed to start re-run: ' + e.message, 'error');
                isRunning = false;
                activeRunStats.running = false;
                renderHistoryTable();
                consoleOpened = false;
                updateCenterLayout();
            }
        }

        /**
         * Re-runs a single test case using a pre-built single-dataset RunRequest.
         * Called from the test card Rerun button in the history view.
         *
         * @param {string} payloadJson - JSON string of the single-dataset RunRequest
         */
        async function rerunSingleTest(payloadJson) {
            await rerunFullRun(payloadJson);
        }

        function isAiLogLine(line) {
            if (!line) return false;

            const lower = line.toLowerCase();

            // Filter out Maven execution boilerplate
            const isMavenLog = lower.includes('[info] command: mvn') ||
                lower.includes('[info] --<') ||
                lower.includes('[info] building ') ||
                lower.includes('[info] --- maven-') ||
                lower.includes('[info] skip non existing resource') ||
                lower.includes('[info] deleting ');
            if (isMavenLog) return false;

            // Check if the line contains standard AI emojis or box drawing chars
            const emojiPattern = /[🤖🧠👣🔮⚙️✨🔍║│├└─═▶✅❌\?]/u;
            if (emojiPattern.test(line)) {
                return true;
            }

            // Check if the line contains AI keywords
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

        function toggleAiOnly(checked) {
            logFilterAiOnly = checked;
            refreshLogFiltering();
        }

        function toggleErrorsOnly(checked) {
            logFilterErrorsOnly = checked;
            refreshLogFiltering();
        }

        function refreshLogFiltering() {
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

        function appendLog(line) {
            if (terminalConsole.innerHTML.includes('Console idle.') || terminalConsole.innerHTML.includes('Connecting to run stream...')) {
                terminalConsole.innerHTML = '';
                if (logFilterAiOnly && !hasShownStartMessage) {
                    hasShownStartMessage = true;
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

        let pollingIntervalId = null;
        let lastLogIndex = 0;
        let lastEventIndex = 0;
        let currentPollSession = 0;
        let serverSessionId = null;
        // Tracks whether the user was navigated into a live-run view (interactive console
        // or the live report view) during an active run. Only when this is true should the
        // poll's not-running branch redirect the user back to the dashboard view.
        let wasInLiveRunView = false;
        // Tracks the running state from the previous poll cycle so we can detect the
        // running→stopped transition edge and avoid redundant loadHistory() calls.
        let lastKnownRunning = false;

        function startPolling() {
            if (pollingIntervalId) {
                clearTimeout(pollingIntervalId);
            }
            pollStatus();
        }

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
                                openReportView(event.reportId);
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

                        document.getElementById('statsTotalExecution').innerText = statusData.total;
                        document.getElementById('statsPassed').innerText = statusData.passed;
                        document.getElementById('statsFailed').innerText = statusData.failed;
                        document.getElementById('statsSkipped').innerText = statusData.skipped || 0;

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
                            renderHistoryTable();
                            window._lastStatsStr = currentStatsStr;
                        }

                        try {
                            isRunning = statusData.running;
                            updateRunButtons();
                            if (isRunning) {
                                lastKnownRunning = true;
                                if (activeRunStats.activeFile !== statusData.activeFile || activeRunStats.activeTestId !== statusData.activeTestId) {
                                    // When the backend moves to a new file, the previous one
                                    // has completed — record it so the test list can grey
                                    // pending entries and mark done entries with a check.
                                    if (activeRunStats.activeFile && activeRunStats.activeFile !== statusData.activeFile) {
                                        liveCompletedFiles.add(activeRunStats.activeFile);
                                    }
                                    activeRunStats.activeFile = statusData.activeFile;
                                    activeRunStats.activeTestId = statusData.activeTestId;
                                    // Always refresh the live test list so state icons update.
                                    // Only reopen the iframe via openCurrentRunView if the
                                    // user is already watching the live view.
                                    if (document.getElementById('colTests').style.display === 'flex' && currentReportId === null) {
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

                                // Only redirect the user back to the dashboard view when they were
                                // explicitly watching a live run (interactive console or live report
                                // view). Browsing the history tab without a run in progress must
                                // never trigger this redirect.
                                if (wasInLiveRunView) {
                                    const isInteractiveViewOpen = document.getElementById('interactiveConsoleView').style.display === 'flex';
                                    const isLiveReportViewOpen = document.getElementById('reportViewContainer').style.display === 'flex' && currentReportId === null;
                                    if (isInteractiveViewOpen || isLiveReportViewOpen) {
                                        showView('dashboardView');
                                    }
                                    wasInLiveRunView = false;
                                }

                                // Only reload history and update layout on the running→stopped
                                // transition edge to avoid redundant /api/reporting/history fetches
                                // and unnecessary DOM updates on every idle poll cycle.
                                if (lastKnownRunning) {
                                    loadHistory();
                                    updateCenterLayout();
                                }
                            }
                            // Update the edge-tracking flag after all branch processing,
                            // so the next poll cycle can correctly detect transitions.
                            lastKnownRunning = statusData.running;
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



        // Tab lifecycle warning and disconnect notification
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
                navigator.sendBeacon('/api/disconnect?clientId=' + encodeURIComponent(clientId));
            }
        }
        window.addEventListener('message', (event) => {
            if (event.data && event.data.action === 'stepSelected') {
                // A step was clicked inside the details iframe → transition to State 4
                // (mini-runs bar | tests | wide details). Use the state machine exclusively;
                // never manipulate col classes or inline styles here to avoid bypassing the reset.
                // Guard: only transition if currently in State 3 — a delayed postMessage
                // must not yank the layout back to 4 after the user has already navigated
                // away (e.g. clicked a mini-run chip to go back to State 3 or 2).
                if (historyNavState === 3) {
                    applyHistoryState(4);
                }
            } else if (event.data && event.data.action === 'rerunTest') {
                if (event.data.testId) {
                    const testFile = event.data.testId.split('.')[0] + '.yaml'; // Fallback mapping, though we really need exact mapping.
                    runTestByFile(testFile);
                    showToast("Triggered rerun for " + testFile, "info");
                }
            }
        });

        window.addEventListener('pagehide', sendDisconnect);
        window.addEventListener('unload', sendDisconnect);

        // Keyboard Shortcuts (capturing phase to guarantee execution and prevent browser defaults)
        window.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                if (createTestModal.style.display === 'flex') {
                    event.preventDefault();
                    createTestModal.style.display = 'none';
                } else if (activeEditingFile) {
                    event.preventDefault();
                    closeEditor();
                }
            }
            if ((event.ctrlKey || event.metaKey) && (event.key === 's' || event.key === 'S')) {
                if (activeEditingFile) {
                    event.preventDefault();
                    saveYamlFile();
                }
            }
        }, true);

        // Keyboard activation of focused list items
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                const focused = document.activeElement;
                if (focused && focused.classList.contains('list-item')) {
                    event.preventDefault();
                    focused.click();
                }
            }
        });

        window.onload = () => {
            init();
            updateCenterLayout();
        };

        // Initialize Theme
        initTheme();


    