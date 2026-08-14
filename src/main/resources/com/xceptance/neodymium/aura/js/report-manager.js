/**
 * Neodymium Report Manager - Unobtrusive JavaScript Controller
 * Handles HTMX events, side panel resizers, chart rendering, and interactive UI logic.
 *
 * @author AI-generated: Gemini Advanced
 * @author Xceptance GmbH 2026
 */

(function() {
    'use strict';

    // Global Data Registries
    window.NeodymiumState = {
        activeBatchName: 'NA Integration STG',
        currentActiveRunId: '1049',
        currentActiveRowId: null,
        activeWholeExecutionFilter: null,
        runReportsRegistry: {},
        batchRunStatsRegistry: {},
        batchSpecificRunsRegistry: {},
        runsFolderRegistry: [],
        cachedTestBaseData: null
    };

    // Unobtrusive Event Delegation System
    document.addEventListener('DOMContentLoaded', function() {
        initEventDelegation();
        initSidePanelResizer();
        initHtmxHooks();
        initDataLoader();
    });

    function initEventDelegation() {
        document.addEventListener('click', function(e) {
            // Open Side Panel
            const openPanelBtn = e.target.closest('.js-open-side-panel');
            if (openPanelBtn) {
                const rowId = openPanelBtn.dataset.rowId;
                const testName = openPanelBtn.dataset.testName;
                const dataSet = openPanelBtn.dataset.dataset;
                const statusKey = openPanelBtn.dataset.status;
                const bug = openPanelBtn.dataset.bug;
                openSidePanelInspector(testName, dataSet, statusKey, bug, rowId);
                return;
            }

            // Close Side Panel
            const closePanelBtn = e.target.closest('.js-close-side-panel');
            if (closePanelBtn) {
                closeSidePanelInspector();
                return;
            }

            // Toggle Multiselect Dropdown
            const multiselectBtn = e.target.closest('.js-multiselect-toggle');
            if (multiselectBtn) {
                e.stopPropagation();
                const targetMenuId = multiselectBtn.dataset.menuId;
                toggleMultiselectMenu(targetMenuId);
                return;
            }

            // Close open multiselect menus on outside click
            if (!e.target.closest('.multiselect-menu') && !e.target.closest('.js-multiselect-toggle')) {
                document.querySelectorAll('.multiselect-menu.show').forEach(m => m.classList.remove('show'));
            }

            // Global Filter Badges
            const filterBadge = e.target.closest('.js-filter-badge');
            if (filterBadge) {
                const statusKey = filterBadge.dataset.statusKey;
                toggleGlobalStatusFilter(statusKey, filterBadge);
                return;
            }
        });

        document.addEventListener('input', function(e) {
            const filterInput = e.target.closest('.js-multiselect-search');
            if (filterInput) {
                const targetMenuId = filterInput.dataset.menuId;
                filterMultiselectOptions(targetMenuId, filterInput.value);
            }
        });

        document.addEventListener('change', function(e) {
            const chkAll = e.target.closest('.js-chk-all');
            if (chkAll) {
                const menuId = chkAll.dataset.menuId;
                handleMultiselectAll(menuId, chkAll.checked);
                return;
            }

            const chkOption = e.target.closest('.js-chk-option');
            if (chkOption) {
                const menuId = chkOption.dataset.menuId;
                const allChkId = chkOption.dataset.allChkId;
                handleMultiselectOption(menuId, allChkId);
                return;
            }
        });
    }

    function toggleMultiselectMenu(menuId) {
        const menu = document.getElementById(menuId);
        if (!menu) return;
        const isShown = menu.classList.contains('show');
        document.querySelectorAll('.multiselect-menu.show').forEach(m => m.classList.remove('show'));
        if (!isShown) menu.classList.add('show');
    }

    function handleMultiselectAll(menuId, isChecked) {
        const menu = document.getElementById(menuId);
        if (!menu) return;
        menu.querySelectorAll('input[type="checkbox"]:not(.js-chk-all)').forEach(chk => {
            chk.checked = isChecked;
        });
        updateMultiselectButtonText(menuId);
        triggerFilterUpdate(menuId);
    }

    function handleMultiselectOption(menuId, allChkId) {
        const menu = document.getElementById(menuId);
        if (!menu) return;
        const allChk = document.getElementById(allChkId);
        const options = menu.querySelectorAll('input[type="checkbox"]:not(.js-chk-all)');
        const checkedCount = Array.from(options).filter(c => c.checked).length;

        if (allChk) {
            allChk.checked = (checkedCount === options.length);
        }
        updateMultiselectButtonText(menuId);
        triggerFilterUpdate(menuId);
    }

    function updateMultiselectButtonText(menuId) {
        const menu = document.getElementById(menuId);
        if (!menu) return;
        const btn = document.querySelector(`.js-multiselect-toggle[data-menu-id="${menuId}"] .js-selected-text`);
        if (!btn) return;

        const options = menu.querySelectorAll('input[type="checkbox"]:not(.js-chk-all)');
        const checked = Array.from(options).filter(c => c.checked);

        if (checked.length === options.length || checked.length === 0) {
            btn.innerText = 'All Selected';
        } else if (checked.length === 1) {
            btn.innerText = checked[0].value;
        } else {
            btn.innerText = `${checked.length} Selected`;
        }
    }

    function filterMultiselectOptions(menuId, query) {
        const menu = document.getElementById(menuId);
        if (!menu) return;
        const q = (query || '').toLowerCase();
        menu.querySelectorAll('.multiselect-option').forEach(opt => {
            const text = (opt.dataset.searchText || opt.innerText).toLowerCase();
            opt.style.display = text.includes(q) ? 'flex' : 'none';
        });
    }

    function triggerFilterUpdate(menuId) {
        if (menuId.includes('tb')) {
            applyTestBaseFilters();
        } else if (menuId.includes('batch')) {
            applyBatchCardFilters();
        } else {
            applyGlobalTestFilters();
        }
    }

    function toggleGlobalStatusFilter(statusKey, clickedBadgeEl) {
        const state = window.NeodymiumState;
        if (state.activeWholeExecutionFilter === statusKey) {
            state.activeWholeExecutionFilter = null;
        } else {
            state.activeWholeExecutionFilter = statusKey;
        }

        document.querySelectorAll('.js-filter-badge').forEach(b => b.classList.remove('active-filter-badge'));
        if (state.activeWholeExecutionFilter && clickedBadgeEl) {
            clickedBadgeEl.classList.add('active-filter-badge');
        }

        applyGlobalTestFilters();
    }

    function applyGlobalTestFilters() {
        const state = window.NeodymiumState;
        const getCheckedValues = (menuId) => {
            const menu = document.getElementById(menuId);
            if (!menu) return [];
            return Array.from(menu.querySelectorAll('input[type="checkbox"]:not(.js-chk-all)'))
                .filter(c => c.checked)
                .map(c => c.value);
        };

        const selectedLocs = getCheckedValues('locationMultiselectMenu');
        const selectedBrowsers = getCheckedValues('browserMultiselectMenu');
        const selectedBugs = getCheckedValues('bugMultiselectMenu');
        const selectedFailures = getCheckedValues('failureMultiselectMenu');
        const statusVal = state.activeWholeExecutionFilter;

        document.querySelectorAll('#runReportSubTabAllTests .test-row').forEach(row => {
            const rowLoc = row.dataset.location;
            const rowBrowser = row.dataset.browser;
            const rowStatus = row.dataset.status;
            const rowBugs = (row.dataset.bugs || 'NONE').split(',');
            const rowFailure = row.dataset.failure || 'NONE';

            const matchLoc = selectedLocs.length === 0 || selectedLocs.includes(rowLoc);
            const matchBrowser = selectedBrowsers.length === 0 || selectedBrowsers.includes(rowBrowser);
            const matchStatus = (statusVal === null || rowStatus === statusVal);
            const matchBug = selectedBugs.length === 0 || rowBugs.some(b => selectedBugs.includes(b));
            const matchFailure = selectedFailures.length === 0 || selectedFailures.includes(rowFailure);

            if (matchLoc && matchBrowser && matchStatus && matchBug && matchFailure) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    }

    function applyTestBaseFilters() {
        const getCheckedValues = (menuId) => {
            const menu = document.getElementById(menuId);
            if (!menu) return [];
            return Array.from(menu.querySelectorAll('input[type="checkbox"]:not(.js-chk-all)'))
                .filter(c => c.checked)
                .map(c => c.value);
        };

        const selectedBatches = getCheckedValues('tbBatchMultiselectMenu');
        const selectedLocs = getCheckedValues('tbLocationsMultiselectMenu');
        const selectedBrowsers = getCheckedValues('tbBrowsersMultiselectMenu');

        document.querySelectorAll('.tb-entry-row').forEach(row => {
            const rowBatches = (row.dataset.batches || '').split(',').map(b => b.trim());
            const rowLoc = row.dataset.location;
            const rowBrowser = row.dataset.browser;

            const matchBatch = selectedBatches.length === 0 || rowBatches.some(b => selectedBatches.includes(b));
            const matchLoc = selectedLocs.length === 0 || selectedLocs.includes(rowLoc);
            const matchBrowser = selectedBrowsers.length === 0 || selectedBrowsers.includes(rowBrowser);

            row.style.display = (matchBatch && matchLoc && matchBrowser) ? '' : 'none';
        });
    }

    function applyBatchCardFilters() {
        const getCheckedValues = (menuId) => {
            const menu = document.getElementById(menuId);
            if (!menu) return [];
            return Array.from(menu.querySelectorAll('input[type="checkbox"]:not(.js-chk-all)'))
                .filter(c => c.checked)
                .map(c => c.value);
        };

        const selectedEnvs = getCheckedValues('batchEnvMultiselectMenu');
        const selectedLocs = getCheckedValues('batchLocalesMultiselectMenu');
        const selectedBrowsers = getCheckedValues('batchBrowsersMultiselectMenu');

        document.querySelectorAll('.batch-card').forEach(card => {
            const env = card.dataset.env;
            const locales = (card.dataset.locales || '').split(',');
            const browsers = (card.dataset.browsers || '').split(',');

            const matchEnv = selectedEnvs.length === 0 || selectedEnvs.includes(env);
            const matchLoc = selectedLocs.length === 0 || locales.some(l => selectedLocs.includes(l));
            const matchBrowser = selectedBrowsers.length === 0 || browsers.some(b => selectedBrowsers.includes(b));

            card.style.display = (matchEnv && matchLoc && matchBrowser) ? '' : 'none';
        });
    }

    function openSidePanelInspector(testName, dataSet, statusKey, bug, rowId) {
        const drawer = document.getElementById('sidePanelDrawer');
        if (!drawer) return;

        window.NeodymiumState.currentActiveRowId = rowId;

        const titleEl = document.getElementById('sidePanelTestTitle');
        const subtitleEl = document.getElementById('sidePanelDataSetSubtitle');
        if (titleEl) titleEl.innerText = testName || 'Test Inspector';
        if (subtitleEl) subtitleEl.innerText = dataSet || '';

        drawer.classList.add('active');
    }

    function closeSidePanelInspector() {
        const drawer = document.getElementById('sidePanelDrawer');
        if (drawer) drawer.classList.remove('active');
    }

    function initSidePanelResizer() {
        const resizer = document.getElementById('sidePanelResizer');
        const drawer = document.getElementById('sidePanelDrawer');
        if (!resizer || !drawer) return;

        let isResizing = false;
        resizer.addEventListener('mousedown', function(e) {
            isResizing = true;
            resizer.classList.add('is-resizing');
            document.body.style.cursor = 'col-resize';
            document.body.style.userSelect = 'none';
        });

        window.addEventListener('mousemove', function(e) {
            if (!isResizing) return;
            const newWidth = window.innerWidth - e.clientX;
            if (newWidth >= 380 && newWidth <= window.innerWidth - 280) {
                drawer.style.width = newWidth + 'px';
            }
        });

        window.addEventListener('mouseup', function() {
            if (isResizing) {
                isResizing = false;
                resizer.classList.remove('is-resizing');
                document.body.style.cursor = '';
                document.body.style.userSelect = '';
            }
        });
    }

    function initHtmxHooks() {
        if (typeof htmx !== 'undefined') {
            htmx.on('htmx:afterSwap', function(evt) {
                // Re-bind dynamic behaviors on swapped content
                console.log('[Neodymium HTMX] Content swapped:', evt.detail.target);
            });
        }
    }

    async function initDataLoader() {
        try {
            console.log('[Neodymium Init] Loading initial report data...');
        } catch (e) {
            console.error('[Neodymium Init Error]', e);
        }
    }
})();
