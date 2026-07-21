
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
        function initTheme() {
            const savedTheme = localStorage.getItem('aura_theme') || 'system';
            applyTheme(savedTheme);

        }


        function applyTheme(theme) {
            const root = document.documentElement;
            if (theme === 'dark') {
                root.classList.add('force-dark');
                root.classList.remove('force-light');
            } else if (theme === 'light') {
                root.classList.add('force-light');
                root.classList.remove('force-dark');
            } else {
                root.classList.remove('force-dark', 'force-light');
            }

            localStorage.setItem('aura_theme', theme);
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

        const editorFileName = document.getElementById('editorFileName');
        const editorContent = document.getElementById('editorContent');
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
            loadSessions();
            loadFiles();
            loadHistory();
            startPolling();
            initResizers();
            // Start history view in State 1 (full-width runs list)
            applyHistoryState(1);

            document.getElementById('chatInput').addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    sendChatMessage();
                }
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
        openModalBtn.onclick = () => {
            createTestModal.style.display = 'flex';
            newTestName.value = '';
            newTestName.focus();
        };
        closeModalBtn.onclick = () => {
            createTestModal.style.display = 'none';
        };
        newTestName.onkeydown = (event) => {
            if (event.key === 'Enter') {
                submitCreateTest();
            }
        };


        function isDatasetSelected(file, id) {
            return selectedDatasets.some(d => d.file === file && d.id === id);
        }

        function isAllDatasetsChecked(fileDto) {
            if (!fileDto.datasets || fileDto.datasets.length === 0) return false;
            return fileDto.datasets.every(d => isDatasetSelected(fileDto.file, d.id));
        }

        function toggleSelectDataset(file, id, checked) {
            if (checked) {
                if (!isDatasetSelected(file, id)) {
                    selectedDatasets.push({ file, id });
                }
            } else {
                selectedDatasets = selectedDatasets.filter(d => !(d.file === file && d.id === id));
            }
            loadFilesList(currentFilesListCached);
        }

        function toggleSelectAllDatasets(file, checked) {
            const fileDto = currentFilesListCached.find(f => f.file === file);
            if (!fileDto) return;

            fileDto.datasets.forEach(d => {
                if (checked) {
                    if (!isDatasetSelected(file, d.id)) {
                        selectedDatasets.push({ file, id: d.id });
                    }
                } else {
                    selectedDatasets = selectedDatasets.filter(ds => !(ds.file === file && ds.id === d.id));
                }
            });
            loadFilesList(currentFilesListCached);
        }

        function toggleExpandFile(file) {
            expandedFiles[file] = !expandedFiles[file];
            loadFilesList(currentFilesListCached);
        }

        async function loadFiles() {
            try {
                const res = await fetch('/api/files');
                currentFilesListCached = await res.json();
                loadFilesList(currentFilesListCached);
            } catch (e) {
                console.error("Failed to load files", e);
            }
        }

        function loadFilesList(files) {
            if (!Array.isArray(files)) {
                yamlFileList.innerHTML = `<div style="padding: 12px; color: var(--danger); font-size: 12px;">Failed to load files. Response was not an array.</div>`;
                return;
            }
            if (files.length === 0) {
                yamlFileList.innerHTML = `<div style="padding: 12px; color: var(--text-secondary); font-size: 12px; font-style: italic; text-align: center;">No YAML test files found in src/test/resources.</div>`;
                return;
            }
            yamlFileList.innerHTML = files.map(item => {
                const isExpanded = !!expandedFiles[item.file];
                const allChecked = isAllDatasetsChecked(item);

                return `
                    <div class="file-container" style="display: flex; flex-direction: column; margin-bottom: 6px;">
                        <div class="list-item" style="position: relative; padding: 6px 10px; border-radius: 6px; display: flex; align-items: center; justify-content: space-between; cursor: pointer;" tabindex="0" onclick="toggleExpandFile('${item.file}')">
                            <div class="item-main" style="min-width: 0; overflow: hidden; display: flex; align-items: center; flex-grow: 1;">
                                <i class="fa-solid ${isExpanded ? 'fa-chevron-down' : 'fa-chevron-right'}" 
                                   style="margin-right: 8px; color: var(--text-secondary); font-size: 10px; width: 12px; text-align: center;"></i>
                                <input type="checkbox" class="file-select-cb" data-file="${item.file}" 
                                       style="margin-right: 8px; cursor: pointer;" 
                                       ${allChecked ? 'checked' : ''} 
                                       onclick="event.stopPropagation(); toggleSelectAllDatasets('${item.file}', this.checked)">
                                <span style="font-size: 13px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${item.file}</span>
                            </div>
                            <button class="edit-icon-btn" onclick="event.stopPropagation(); openYamlEditor('${item.file}')" 
                                    style="padding: 4px 8px; color: var(--text-secondary);" 
                                    title="Edit YAML Steps" alt="Edit YAML Steps for ${item.file}" aria-label="Edit YAML Steps for ${item.file}">
                                <i class="fa-solid fa-pen" style="font-size: 11px;" aria-hidden="true"></i>
                            </button>
                        </div>
                        
                        <div class="dataset-list" id="datasets-${item.file}" style="display: ${isExpanded ? 'flex' : 'none'}; flex-direction: column; padding-left: 24px; margin-top: 4px; gap: 4px;">
                            ${item.datasets.map(dataset => {
                    const isChecked = isDatasetSelected(item.file, dataset.id);
                    return `
                                    <div class="dataset-item" style="display: flex; align-items: center; padding: 4px 8px; border-radius: 4px; font-size: 12px; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); justify-content: space-between; cursor: pointer;" onclick="const cb = this.querySelector('.dataset-select-cb'); cb.checked = !cb.checked; toggleSelectDataset('${item.file}', '${dataset.id}', cb.checked);">
                                        <div style="display: flex; align-items: center; min-width: 0; overflow: hidden;">
                                            <input type="checkbox" class="dataset-select-cb" data-file="${item.file}" data-id="${dataset.id}" 
                                                   style="margin-right: 8px; cursor: pointer;" 
                                                   ${isChecked ? 'checked' : ''} 
                                                   onclick="event.stopPropagation(); toggleSelectDataset('${item.file}', '${dataset.id}', this.checked)">
                                            <i class="fa-solid fa-database" style="margin-right: 6px; color: var(--accent-primary); opacity: 0.7; flex-shrink: 0;"></i>
                                            <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-left: 4px;">${dataset.label}</span>
                                        </div>
                                    </div>
                                `;
                }).join('')}
                        </div>
                    </div>
                `;
            }).join('');
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
                    colRuns.style.flex = '1 1 0';
                    // colTests and colReport remain display:none
                    break;

                case 2:
                    // ── State 2: Runs | Tests — equal 50/50 split ───────────────
                    colRuns.style.display = 'flex';
                    colRuns.style.flex = '1 1 0';
                    colTests.style.display = 'flex';
                    colTests.style.flex = '1 1 0';
                    // colReport remains display:none
                    if (r1) r1.style.display = 'block';
                    break;

                case 3:
                    // ── State 3: Runs | Tests | Details — equal three-way split ─
                    colRuns.style.display = 'flex';
                    colRuns.style.flex = '1 1 0';
                    colTests.style.display = 'flex';
                    colTests.style.flex = '1 1 0';
                    colReport.style.display = 'flex';
                    colReport.style.flex = '1 1 0';
                    if (r1) r1.style.display = 'block';
                    if (r2) r2.style.display = 'block';
                    break;

                case 4:
                    // ── State 4: Mini-runs (48px) | Tests (flex:1) | Details (flex:2)
                    // colRuns full panel is hidden; colRunsMini strip takes its place.
                    // Tests keeps its normal proportional size; details gets more room.
                    colTests.style.display = 'flex';
                    colTests.style.flex = '1 1 0';
                    colReport.style.display = 'flex';
                    colReport.style.flex = '2 1 0';
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
        function onMiniRunChipClick(runId) {
            if (runId === currentReportId) {
                // Same run: restore to State 3 (full runs panel back)
                applyHistoryState(3);
            } else {
                // Different run: go to State 2 and load its tests
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
                const res = await fetch('/api/reporting/history');
                historyCached = await res.json();
                renderHistoryTable();
            } catch (e) {
                console.error("Failed to load reporting history", e);
            }
        }

        let currentReportId = null;

        /** Converts a duration in milliseconds to a human-readable string like "3m 12s" or "45s". */
        function formatDuration(ms) {
            if (!ms || ms <= 0) return '';
            const totalSec = Math.round(ms / 1000);
            if (totalSec < 60) return totalSec + 's';
            const min = Math.floor(totalSec / 60);
            const sec = totalSec % 60;
            return sec > 0 ? min + 'm ' + sec + 's' : min + 'm';
        }

        function renderHistoryTable() {
            let rowsHtml = '';

            if (activeRunStats.running) {
                const statusBadge = `<span style="background: rgba(59,130,246,0.1); color: var(--accent); padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: 500;"><i class="fa-solid fa-circle-notch spinner" style="margin-right: 4px;"></i> Running</span>`;
                const formattedTime = activeRunStats.startTime ? activeRunStats.startTime.toLocaleString() : new Date().toLocaleString();

                rowsHtml += `
                    <tr class="history-row" style="cursor: pointer; border-left: 3px solid var(--accent);" onclick="openCurrentRunView()">
                        <td style="padding: 10px 12px;">
                            <div class="run-row-header">
                                <span class="run-number">#—</span>
                                <span class="run-timestamp">${formattedTime}</span>
                                ${statusBadge}
                            </div>
                        </td>
                    </tr>
                `;
            }

            if (historyCached.length === 0 && !activeRunStats.running) {
                rowsHtml += `<tr><td style="text-align: center; color: var(--text-secondary); font-style: italic; padding: 20px;">No historical reports.</td></tr>`;
            } else {
                rowsHtml += historyCached.map(item => {
                    let statusBadge = `<span class="badge-failed">${item.status || 'Failed'}</span>`;
                    if (item.status === 'Passed') {
                        statusBadge = `<span class="badge-success">Passed</span>`;
                    } else if (item.status === 'Aborted') {
                        statusBadge = `<span class="badge-aborted">Aborted</span>`;
                    }

                    const formattedTime = formatHistoryTimestamp(item.timestamp || item.id.substring(0, 15));
                    const durationLabel = formatDuration(item.durationMs);
                    const runNum = item.runNumber ? '#' + item.runNumber : '';

                    const isSelected = item.id === currentReportId;
                    const selectedStyle = isSelected ? 'background-color: var(--bg-hover); border-left: 3px solid var(--accent);' : '';

                    // Inline action buttons: Allure Report (disabled when not available), View Log, Delete
                    const hasReport = item.hasReport === 'true' || item.hasReport === true;
                    const allureEnabled = item.allureEnabled === true || item.allureEnabled === 'true';
                    const allureTitle = (!allureEnabled)
                        ? 'title="Allure was disabled for this run"'
                        : (!hasReport ? 'title="Allure report not available"' : '');
                    const allureDisabled = (!hasReport || !allureEnabled) ? 'disabled' : '';
                    const allureAction = hasReport
                        ? `onclick="event.stopPropagation(); window.open('/api/reporting/report/${item.id}/allure-report/index.html', '_blank')"`
                        : '';

                    const allureNote = (!allureEnabled)
                        ? `<div class="run-allure-note"><i class="fa-solid fa-circle-info"></i> Allure was disabled for this run</div>`
                        : '';

                    // Passed/failed counts as clear text
                    const passedCount = parseInt(item.passed) || 0;
                    const failedCount = parseInt(item.failed) || 0;
                    const skippedCount = parseInt(item.skipped) || 0;

                    return `
                        <tr class="history-row" style="cursor: pointer; ${selectedStyle}" onclick="selectHistoryRun('${item.id}', ${hasReport})">
                            <td style="padding: 10px 12px;">
                                <div class="run-row-header">
                                    <span class="run-number">${runNum}</span>
                                    <span class="run-timestamp">${formattedTime}</span>
                                    ${statusBadge}
                                    ${durationLabel ? `<span class="run-duration-chip"><i class="fa-regular fa-clock"></i> ${durationLabel}</span>` : ''}
                                </div>
                                <div class="run-stats-row">
                                    <span class="run-stat-passed"><i class="fa-solid fa-check" aria-hidden="true"></i> ${passedCount} passed</span>
                                    <span class="run-stat-failed"><i class="fa-solid fa-xmark" aria-hidden="true"></i> ${failedCount} failed</span>
                                    ${skippedCount > 0 ? `<span class="run-stat-skipped" style="color:var(--text-muted); font-weight:600;"><i class="fa-solid fa-forward-step" aria-hidden="true"></i> ${skippedCount} skipped</span>` : ''}
                                </div>
                                <div class="run-actions-row">
                                    <button class="run-action-btn" ${allureDisabled} ${allureTitle} ${allureAction}>
                                        <i class="fa-solid fa-chart-bar"></i> Allure Report
                                    </button>
                                    ${item.runConfig && !isRunning ? `<button class="run-action-btn" onclick="event.stopPropagation(); rerunFullRun('${JSON.stringify(item.runConfig).replace(/"/g, '&quot;')}')" title="Re-run this entire suite with the same configuration"><i class="fa-solid fa-rotate-right"></i> Rerun</button>` : ''}
                                </div>
                                <div style="display: flex; justify-content: flex-end; margin-top: 4px;">
                                    <button class="run-action-btn danger" style="font-size: 10px; padding: 2px 6px; opacity: 0.65;" onclick="event.stopPropagation(); deleteReport('${item.id}')" title="Delete this run">
                                        <i class="fa-solid fa-trash-can"></i> Delete
                                    </button>
                                </div>
                                ${allureNote}
                            </td>
                        </tr>
                    `;
                }).join('');
            }
            reportingHistoryList.innerHTML = rowsHtml;
        }

        let currentTestFile = null;

        function selectHistoryRun(reportId, hasReport) {
            currentReportId = reportId;
            currentTestFile = null;
            renderHistoryTable(); // Update selection styling

            const run = historyCached.find(r => r.id === reportId);

            // Update column headers with selected run context
            const runNum = run && run.runNumber ? '#' + run.runNumber : '';
            const runLabel = runNum ? ` - Run ${runNum}` : '';
            const testsHeader = document.getElementById('historyTestsHeader');
            if (testsHeader) {
                testsHeader.textContent = `Test Cases${runLabel}`;
            }
            const detailsHeader = document.getElementById('historyDetailsHeader');
            if (detailsHeader) {
                detailsHeader.textContent = 'Test Case Details';
            }

            // Transition to State 2: runs (fixed) | tests (flex-grow)
            applyHistoryState(2);

            // Show the "select a test" placeholder in the details panel and blank the iframe
            document.getElementById('historyPlaceholder').style.display = 'flex';
            document.getElementById('historyPlaceholder').innerHTML = `
                <i class="fa-solid fa-arrow-left" style="font-size: 32px; margin-bottom: 16px; opacity: 0.5;"></i>
                <p>Select a test case from the list.</p>
            `;
            document.getElementById('historyConsoleIframe').src = 'about:blank';

            // Render Tests (same as before)
            const testsList = document.getElementById('historyTestsList');
            if (!run || !run.tests || run.tests.length === 0) {
                testsList.innerHTML = `<div style="text-align: center; color: var(--text-secondary); font-style: italic; padding: 24px;">No tests available.</div>`;
            } else {
                testsList.innerHTML = run.tests.map(t => {
                    let cardBadgeClass = 'badge-failed';
                    let cardBadgeText = 'Failed';
                    let dotClass = 'failed';
                    if (t.status === 'Passed') {
                        cardBadgeClass = 'badge-success';
                        cardBadgeText = 'Passed';
                        dotClass = 'passed';
                    } else if (t.status === 'Aborted') {
                        cardBadgeClass = 'badge-aborted';
                        cardBadgeText = 'Aborted';
                        dotClass = 'aborted';
                    }

                    // Browser icon: BrowserDefault / Default → Chrome; unknown → generic globe
                    let browserIcon = 'fa-solid fa-globe';
                    let browserLabel = t.browser || '';
                    if (t.browser) {
                        const b = t.browser.toLowerCase();
                        if (b.includes('chrome') || b === 'browserdefault' || b === 'default') {
                            browserIcon = 'fa-brands fa-chrome';
                            if (b === 'browserdefault' || b === 'default') browserLabel = 'Chrome';
                        } else if (b.includes('firefox')) {
                            browserIcon = 'fa-brands fa-firefox-browser';
                        } else if (b.includes('edge')) {
                            browserIcon = 'fa-brands fa-edge';
                        } else if (b.includes('safari')) {
                            browserIcon = 'fa-brands fa-safari';
                        }
                    }

                    // Test label: yamlLabel (YAML filename without .yaml) + dataset ID
                    const label = t.yamlLabel || t.name || 'Test';
                    const datasetPart = t.testId ? ` · ${t.testId}` : '';

                    // Duration
                    const durText = t.durationMs ? (parseFloat(t.durationMs) / 1000).toFixed(1) + 's' : '';

                    // Playbook mode badge
                    const mode = t.playbookMode || 'llm';
                    const pbLabel = mode === 'playbook' ? 'Playbook'
                        : mode === 'healing' ? 'Healing'
                            : 'AI';
                    const pbIcon = mode === 'playbook' ? 'fa-solid fa-rotate'
                        : mode === 'healing' ? 'fa-solid fa-wrench'
                            : 'fa-solid fa-wand-magic-sparkles';
                    const pbBadge = `<span class="pb-badge ${mode}"><i class="${pbIcon}"></i> ${pbLabel}</span>`;

                    // Per-test log button
                    const logBtn = (t.hasLog === 'true' || t.hasLog === true)
                        ? `<button class="run-action-btn" style="font-size:10px; padding:2px 7px;" onclick="event.stopPropagation(); openTestLog('${reportId}', '${t.name}')" title="View test log"><i class="fa-solid fa-file-lines"></i> Log</button>`
                        : '';

                    const run = historyCached.find(r => r.id === reportId);
                    // Build the rerun payload for a single-dataset re-run:
                    // find the dataset entry from the run's stored runConfig that matches
                    // this test's yamlSource file path, then build a single-item payload
                    // reusing all the original run flags (headless, interactive, etc.).
                    let rerunBtn = '';
                    if (run && run.runConfig && !isRunning) {
                        const rc = run.runConfig;
                        // Derive the dataset file from yamlSource stored in the console-execution JSON
                        // We match the run's datasets by file path against the test's yamlSource
                        let matchedDataset = null;
                        if (t.yamlLabel && rc.datasets) {
                            matchedDataset = rc.datasets.find(d => {
                                const fname = d.file ? d.file.replace(/\\/g, '/').split('/').pop() : '';
                                return fname === t.yamlLabel + '.yaml' || fname === t.yamlLabel;
                            });
                        }
                        if (!matchedDataset && rc.datasets && rc.datasets.length > 0) {
                            // Fallback: use the first dataset if only one test file was in this run
                            matchedDataset = rc.datasets[0];
                        }
                        if (matchedDataset) {
                            let finalId = matchedDataset.id || null;
                            if (!finalId && t.testId) {
                                finalId = t.testId;
                                if (finalId.startsWith('Dataset ')) {
                                    finalId = finalId.substring(8);
                                }
                            }
                            // Build single-dataset payload: same flags but only this one dataset + testId
                            const singlePayload = {
                                datasets: [{ file: matchedDataset.file, id: finalId }],
                                headless: rc.headless,
                                interactive: rc.interactive,
                                allure: rc.allure,
                                video: rc.video,
                                keepOpen: rc.keepOpen
                            };
                            const payloadStr = JSON.stringify(singlePayload).replace(/"/g, '&quot;');
                            rerunBtn = `<button class="run-action-btn" style="font-size:10px; padding:2px 7px;" onclick="event.stopPropagation(); rerunSingleTest('${payloadStr}')" title="Re-run this test case with the same configuration"><i class="fa-solid fa-rotate-right"></i> Rerun</button>`;
                        }
                    }

                    return `<div class="test-card" data-test-name="${t.name}" onclick="loadInteractiveTest('${reportId}', '${t.file}', '${t.name}', this)">
                        <div class="test-card-title-row">
                            <span class="${cardBadgeClass}">${cardBadgeText}</span>
                            <span class="test-card-label">${label}${datasetPart}</span>
                        </div>
                        <div class="test-card-meta">
                            <span title="${t.browser || 'Browser'}"><i class="${browserIcon}"></i> ${browserLabel}</span>
                            ${durText ? `<span><i class="fa-regular fa-clock"></i> ${durText}</span>` : ''}
                            ${pbBadge}
                        </div>
                        ${(logBtn || rerunBtn) ? `<div class="test-card-actions">${logBtn}${rerunBtn}</div>` : ''}
                    </div>`;
                }).join('');
            }
        }

        /**
         * Loads the interactive replay for a specific test case in the details iframe.
         * Also populates the mini history strip with status bubbles for this test
         * across all archived runs.
         *
         * @param {string} reportId   - the run/report directory ID
         * @param {string} testFile   - the console-execution JSON filename
         * @param {string} testName   - human-readable test name (used for mini-widget matching)
         * @param {Element} rowElement - the clicked table row for highlight
         */
        function loadInteractiveTest(reportId, testFile, testName, rowElement) {
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

            document.getElementById('historyPlaceholder').style.display = 'none';
            const iframe = document.getElementById('historyConsoleIframe');
            const dataUrl = '/api/reporting/report/' + reportId + '/' + testFile;
            iframe.src = '/interactive_console.html?dataUrl=' + encodeURIComponent(dataUrl);

            // Transition to State 4: mini-runs bar, tests list, and wide details
            applyHistoryState(4);

            // Populate the mini history strip with bubbles for this test across all runs
            renderMiniHistoryStrip(testName, reportId);
        }

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

        function updateQueueList() {
            const statsQueueCount = document.getElementById('statsQueueCount');
            if (statsQueueCount) {
                statsQueueCount.innerText = `${selectedDatasets.length} Dataset${selectedDatasets.length === 1 ? '' : 's'}`;
            }

            if (selectedDatasets.length === 0) {
                queueListContainer.innerHTML = `
                    <div style="font-size: 13px; color: var(--text-secondary); font-style: italic; padding: 15px; border: 1px dashed var(--border-hover); border-radius: 8px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 8px;">
                        <i class="fa-solid fa-arrow-left" style="font-size: 16px; color: var(--accent-primary);"></i>
                        <span>No tests selected. Please check one or more YAML test cases in the sidebar explorer.</span>
                    </div>
                `;
                runQueueBtn.disabled = true;
            } else {
                queueListContainer.innerHTML = selectedDatasets.map((ds, index) => `
                    <div class="queue-item" style="display: flex; align-items: center; justify-content: space-between; padding: 6px 8px; margin-bottom: 4px; background: rgba(255,255,255,0.03); border: 1px solid var(--border-glass); border-radius: 6px;">
                        <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 0; padding-right: 5px; font-size: 12.5px;">
                            ${index + 1}. <strong>${ds.file}</strong> (Dataset: ${ds.id})
                        </span>
                        <div style="display: flex; gap: 4px; align-items: center; flex-shrink: 0;">
                            <button onclick="moveQueueItem(${index}, -1)" class="editor-btn" style="padding: 2px 6px; font-size: 10px; border-radius: 4px;" title="Move Up" ${index === 0 ? 'disabled' : ''}>
                                <i class="fa-solid fa-arrow-up"></i>
                            </button>
                            <button onclick="moveQueueItem(${index}, 1)" class="editor-btn" style="padding: 2px 6px; font-size: 10px; border-radius: 4px;" title="Move Down" ${index === selectedDatasets.length - 1 ? 'disabled' : ''}>
                                <i class="fa-solid fa-arrow-down"></i>
                            </button>
                        </div>
                    </div>
                `).join('');
                if (!isRunning) {
                    runQueueBtn.disabled = false;
                }
            }
            updateRunButtons();
        }

        function moveQueueItem(index, direction) {
            const newIndex = index + direction;
            if (newIndex >= 0 && newIndex < selectedDatasets.length) {
                const temp = selectedDatasets[index];
                selectedDatasets[index] = selectedDatasets[newIndex];
                selectedDatasets[newIndex] = temp;
                updateQueueList();
            }
        }

        function loadSessions() {
            try {
                const stored = localStorage.getItem('aura_chat_sessions');
                if (stored) {
                    chatSessions = JSON.parse(stored);
                }
            } catch (e) {
                console.error("Failed to parse chat sessions", e);
            }

            if (!chatSessions || chatSessions.length === 0) {
                const defaultSession = {
                    id: 's-' + Math.random().toString(36).substring(2) + '-' + Date.now().toString(36),
                    name: 'Default Chat',
                    history: []
                };
                chatSessions = [defaultSession];
                currentSessionId = defaultSession.id;
                saveSessions();
            } else {
                currentSessionId = localStorage.getItem('aura_active_session_id');
                if (!currentSessionId || !chatSessions.some(s => s.id === currentSessionId)) {
                    currentSessionId = chatSessions[0].id;
                }
            }

            updateSessionDropdown();
            loadCurrentSessionHistory();
        }

        function saveSessions() {
            localStorage.setItem('aura_chat_sessions', JSON.stringify(chatSessions));
            localStorage.setItem('aura_active_session_id', currentSessionId);
        }

        function updateSessionDropdown() {
            const select = document.getElementById('chatSessionSelect');
            if (select) {
                select.innerHTML = chatSessions.map(session => `
                    <option value="${session.id}" ${session.id === currentSessionId ? 'selected' : ''}>${escapeHtml(session.name)}</option>
                `).join('');
            }
        }

        function escapeHtml(str) {
            return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
        }

        function loadCurrentSessionHistory() {
            const session = chatSessions.find(s => s.id === currentSessionId);
            conversationHistory = session ? session.history : [];

            const chatMessages = document.getElementById('chatMessages');
            if (chatMessages) {
                chatMessages.innerHTML = '';

                if (conversationHistory.length === 0) {
                    appendGreeting();
                } else {
                    conversationHistory.forEach(msg => {
                        const bubble = appendChatMessage(msg.role, msg.content);
                        if (msg.thinking) {
                            const details = document.createElement('details');
                            details.className = 'thinking-details';

                            const summary = document.createElement('summary');
                            summary.innerHTML = '<i class="fa-solid fa-brain" aria-hidden="true"></i> AI Reasoning & Escalations';
                            details.appendChild(summary);

                            const content = document.createElement('pre');
                            content.className = 'thinking-content';
                            content.innerText = msg.thinking;
                            details.appendChild(content);

                            bubble.appendChild(details);
                        }
                    });
                }
            }
        }

        function appendGreeting() {
            const chatMessages = document.getElementById('chatMessages');
            if (chatMessages) {
                chatMessages.innerHTML = `
                    <div class="chat-message message-ai">
                        <span>Hello! I am Aura, your test automation assistant. How can I help you today? You can ask me to select/run test suites or create/modify tests!</span>
                    </div>
                `;
            }
        }

        function switchChatSession() {
            const select = document.getElementById('chatSessionSelect');
            if (select) {
                currentSessionId = select.value;
                saveSessions();
                loadCurrentSessionHistory();
            }
        }

        function createNewChatSession(name = 'New Chat') {
            const newSession = {
                id: 's-' + Math.random().toString(36).substring(2) + '-' + Date.now().toString(36),
                name: name,
                history: []
            };
            chatSessions.push(newSession);
            currentSessionId = newSession.id;
            saveSessions();
            updateSessionDropdown();
            loadCurrentSessionHistory();
        }

        function renameCurrentChatSession() {
            const session = chatSessions.find(s => s.id === currentSessionId);
            if (!session) return;
            const newName = prompt('Rename Chat Session:', session.name);
            if (newName && newName.trim().length > 0) {
                session.name = newName.trim();
                saveSessions();
                updateSessionDropdown();
            }
        }

        function deleteCurrentChatSession() {
            if (chatSessions.length <= 1) {
                showToast("Cannot delete the only chat session.", "error");
                return;
            }
            const session = chatSessions.find(s => s.id === currentSessionId);
            if (confirm(`Are you sure you want to delete session "${session.name}"?`)) {
                chatSessions = chatSessions.filter(s => s.id !== currentSessionId);
                currentSessionId = chatSessions[0].id;
                saveSessions();
                updateSessionDropdown();
                loadCurrentSessionHistory();
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
                    openYamlEditor(data.file);
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
            updateRunButtons();
        }

        function updateRunButtons() {
            const runQueueBtn = document.getElementById('runQueueBtn');
            const runCurrentTestBtn = document.getElementById('runCurrentTestBtn');
            const stopQueueBtn = document.getElementById('stopQueueBtn');
            if (!runQueueBtn || !runCurrentTestBtn || !stopQueueBtn) return;

            const queueCount = selectedDatasets ? selectedDatasets.length : 0;
            runQueueBtn.innerHTML = `<i class="fa-solid fa-play"></i> Run Queue (${queueCount})`;

            if (isRunning) {
                runQueueBtn.style.display = 'none';
                runCurrentTestBtn.style.display = 'none';
                stopQueueBtn.style.display = 'flex';
                return;
            }

            stopQueueBtn.style.display = 'none';
            runQueueBtn.style.display = 'flex';

            if (activeEditingFile) {
                runCurrentTestBtn.style.display = 'flex';
                runCurrentTestBtn.disabled = false;
                runQueueBtn.style.filter = 'saturate(50%) opacity(0.7)';
                runQueueBtn.disabled = true;
            } else {
                runCurrentTestBtn.style.display = 'none';
                runQueueBtn.style.filter = 'none';
                runQueueBtn.disabled = queueCount === 0;
            }
        }

        async function sendChatMessage() {
            const input = document.getElementById('chatInput');
            const prompt = input.value.trim();
            if (!prompt) return;

            input.value = '';
            appendChatMessage('user', prompt);

            const thinkingBubble = appendChatMessage('ai', 'Thinking...', true);
            const apiKeyBanner = document.getElementById('apiKeyBanner');
            apiKeyBanner.style.display = 'none';

            // Clone history before pushing user message
            const payloadHistory = [...conversationHistory];
            conversationHistory.push({ role: 'user', content: prompt });

            // Auto-rename session on first message
            const session = chatSessions.find(s => s.id === currentSessionId);
            if (session && (session.name === 'New Chat' || session.name === 'Default Chat' || session.history.length === 1)) {
                session.name = prompt.substring(0, 25) + (prompt.length > 25 ? '...' : '');
                updateSessionDropdown();
            }
            saveSessions();

            try {
                const response = await fetch('/api/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        prompt: prompt,
                        activeFile: activeEditingFile,
                        history: payloadHistory
                    })
                });

                const data = await response.json();
                thinkingBubble.remove();

                if (data.error) {
                    if (data.error.includes("API Key") || data.error.includes("LLM Client Error")) {
                        const apiKeyBannerText = document.getElementById('apiKeyBannerText');
                        apiKeyBannerText.innerHTML = data.error;
                        apiKeyBanner.style.display = 'flex';
                    }
                    appendChatMessage('ai', "Error: " + data.error);
                    return;
                }

                const aiBubble = appendChatMessage('ai', data.message);

                const msgObj = { role: 'assistant', content: data.message };
                if (data.thinking) {
                    msgObj.thinking = data.thinking;
                }
                conversationHistory.push(msgObj);
                saveSessions();

                if (data.thinking) {
                    const details = document.createElement('details');
                    details.className = 'thinking-details';

                    const summary = document.createElement('summary');
                    summary.innerHTML = '<i class="fa-solid fa-brain" aria-hidden="true"></i> AI Reasoning & Escalations';
                    details.appendChild(summary);

                    const content = document.createElement('pre');
                    content.className = 'thinking-content';
                    content.innerText = data.thinking;
                    details.appendChild(content);

                    aiBubble.appendChild(details);
                }

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
                    editorFileName.innerText = data.filename;
                    editorContent.value = data.content;
                    updateCenterLayout();
                    editorContent.focus();
                }

            } catch (e) {
                thinkingBubble.remove();
                appendChatMessage('ai', "Error connecting to AI Server: " + e.message);
            }
        }

        function appendChatMessage(sender, text, isThinking = false) {
            const chatMessages = document.getElementById('chatMessages');
            const bubble = document.createElement('div');
            bubble.className = `chat-message message-${sender}`;

            const contentSpan = document.createElement('span');
            contentSpan.innerText = text;
            bubble.appendChild(contentSpan);

            if (isThinking) {
                const indicator = document.createElement('div');
                indicator.className = 'typing-indicator';
                indicator.innerHTML = '<span></span><span></span><span></span>';
                bubble.appendChild(indicator);
            }

            chatMessages.appendChild(bubble);
            chatMessages.scrollTop = chatMessages.scrollHeight;
            return bubble;
        }

        function closeConsole() {
            consoleOpened = false;
            updateCenterLayout();
        }

        async function openYamlEditor(filename) {
            activeEditingFile = filename;
            editorFileName.innerText = filename;
            try {
                const res = await fetch(`/api/read?file=${encodeURIComponent(filename)}`);
                const data = await res.json();
                editorContent.value = data.content || '';
                updateCenterLayout();
            } catch (e) {
                showToast("Failed to load file content: " + e.message, "error");
            }
        }

        async function saveYamlFile() {
            if (!activeEditingFile) return;
            try {
                const res = await fetch('/api/save', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ file: activeEditingFile, content: editorContent.value })
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

        async function deleteYamlFile() {
            if (!activeEditingFile) return;
            document.getElementById('deleteFileNameDisplay').textContent = activeEditingFile;
            document.getElementById('deleteTestModal').style.display = 'flex';
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
                    loadFiles();
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

        async function runTestByFile(testFile) {
            const fileDatasets = [{ file: testFile, id: null }];

            const headless = document.getElementById('optHeadless').checked;
            const video = document.getElementById('optVideo').checked;
            const keepOpen = document.getElementById('optKeepOpen').checked;
            const interactive = document.getElementById('optInteractive').checked;
            const allure = document.getElementById('optReporting').checked;

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
            activeRunStats.tests = fileDatasets;
            // Reset per-file completion tracking for the fresh run.
            liveCompletedFiles.clear();
            liveLastActiveFile = null;
            renderHistoryTable();

            runSpinner.style.display = 'inline-block';
            updateCenterLayout();
            runSpinner.style.display = 'inline-block';
            terminalConsole.innerHTML = 'Connecting to run stream...\n';
            hasShownStartMessage = false;

            try {
                const res = await fetch('/api/run', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ datasets: fileDatasets, headless, video, keepOpen, interactive, allure })
                });
                const data = await res.json();
                if (data.error) {
                    showToast("Execution failed to start: " + data.error, "error");
                    isRunning = false;
                    activeRunStats.running = false;
                    renderHistoryTable();
                    consoleOpened = false;
                    updateCenterLayout();
                }
            } catch (e) {
                showToast("Failed to start run: " + e.message, "error");
                isRunning = false;
                activeRunStats.running = false;
                renderHistoryTable();
                consoleOpened = false;
                updateCenterLayout();
            }
        }

        async function runCurrentFile() {
            if (!activeEditingFile) return;

            await saveYamlFile();

            const fileDatasets = [{ file: activeEditingFile, id: null }];

            const headless = document.getElementById('optHeadless').checked;
            const video = document.getElementById('optVideo').checked;
            const keepOpen = document.getElementById('optKeepOpen').checked;
            const interactive = document.getElementById('optInteractive').checked;
            const allure = document.getElementById('optReporting').checked;

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
            activeRunStats.tests = fileDatasets;
            // Reset per-file completion tracking for the fresh run.
            liveCompletedFiles.clear();
            liveLastActiveFile = null;
            renderHistoryTable();

            runSpinner.style.display = 'inline-block';
            updateCenterLayout();
            runSpinner.style.display = 'inline-block';
            terminalConsole.innerHTML = 'Connecting to run stream...\n';
            hasShownStartMessage = false;

            try {
                const res = await fetch('/api/run', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ datasets: fileDatasets, headless, video, keepOpen, interactive, allure })
                });
                const data = await res.json();
                if (data.error) {
                    showToast("Execution failed to start: " + data.error, "error");
                    isRunning = false;
                    activeRunStats.running = false;
                    renderHistoryTable();
                    consoleOpened = false;
                    updateCenterLayout();
                }
            } catch (e) {
                showToast("Failed to start run for current file: " + e.message, "error");
                isRunning = false;
                activeRunStats.running = false;
                renderHistoryTable();
                consoleOpened = false;
                updateCenterLayout();
            }
        }

        async function runQueue() {
            if (selectedDatasets.length === 0) return;

            const headless = document.getElementById('optHeadless').checked;
            const video = document.getElementById('optVideo').checked;
            const keepOpen = document.getElementById('optKeepOpen').checked;
            const interactive = document.getElementById('optInteractive').checked;
            const allure = document.getElementById('optReporting').checked;

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
            activeRunStats.tests = selectedDatasets.map(d => ({ file: d.file, id: d.id }));
            // Reset per-file completion tracking for the fresh run.
            liveCompletedFiles.clear();
            liveLastActiveFile = null;
            renderHistoryTable();

            runSpinner.style.display = 'inline-block';
            updateCenterLayout();
            runSpinner.style.display = 'inline-block';
            terminalConsole.innerHTML = 'Connecting to run stream...\n';
            hasShownStartMessage = false;

            try {
                const res = await fetch('/api/run', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ datasets: selectedDatasets, headless, video, keepOpen, interactive, allure })
                });
                const data = await res.json();
                if (data.error) {
                    showToast("Execution failed to start: " + data.error, "error");
                    isRunning = false;
                    activeRunStats.running = false;
                    renderHistoryTable();
                    consoleOpened = false;
                    updateCenterLayout();
                }
            } catch (e) {
                showToast("Failed to start run queue: " + e.message, "error");
                isRunning = false;
                activeRunStats.running = false;
                renderHistoryTable();
                consoleOpened = false;
                updateCenterLayout();
            }
        }

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
            activeRunStats.tests = (payload.datasets || []).map(d => ({ file: d.file, id: d.id }));
            // Reset per-file completion tracking for the fresh re-run.
            liveCompletedFiles.clear();
            liveLastActiveFile = null;
            renderHistoryTable();

            runSpinner.style.display = 'inline-block';
            updateCenterLayout();
            terminalConsole.innerHTML = 'Connecting to run stream...\n';
            hasShownStartMessage = false;

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

        async function stopQueue() {
            try {
                await fetch('/api/stop', { method: 'POST' });
            } catch (e) {
                console.error("Failed to stop queue", e);
            }
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

                        document.getElementById('statsTotalExecution').innerText = `${statusData.total} Test${statusData.total === 1 ? '' : 's'}`;
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
                                    sidebarBadge.innerHTML = `<i class="fa-solid fa-circle-notch spinner"></i> ${statusData.passed + statusData.failed + (statusData.skipped || 0) + 1}/${statusData.total}`;
                                }
                                updateRunButtons();
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

        // Ensure Enter works on chat input since we decoupled the original listener
        document.getElementById('chatInput').addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                sendChatMessage();
            }
        });
    