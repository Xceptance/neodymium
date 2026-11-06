/**
 * Aura Report Manager - Interactive Client Controller
 * Aligned with static design reference implementation
 */

// Global State Maps
let currentActiveRowId = null;
let currentExecutionIsFailed = true;
let activeTryMap = {}; 
let currentTestNameStr = '';
let currentDataSetStr = '';
let currentDataSetRaw = '';
let currentLocationStr = '';
let currentBrowserStr = '';
let currentActiveRunId = '#RUN_ID';
let activeBatchName = 'Default';

const executionCommentsMap = {};
const executionBugMap = {};
const executionStepsMap = {};
const batchRunStatsRegistry = {};

const activeClassFilters = {};
const activeAreaFilters = {};
let activeWholeExecutionFilter = null;
let currentExecutionStatusFilter = 'ALL';
let currentExecutionModeFilter = 'ALL';
let currentGlobalStatusBadgeFilter = 'ALL';
let currentHealedFilter = 'ALL';

function formatDurationInMinutes(durMs) {
    if (durMs === null || durMs === undefined) return '0 min 0 s';
    const num = Number(durMs);
    if (isNaN(num) || num <= 0) return '0 min 0 s';
    const totalSec = Math.round(num / 1000);
    const minutes = Math.floor(totalSec / 60);
    const seconds = totalSec % 60;
    return `${minutes} min ${seconds} s`;
}

function formatTimestamp(raw) {
    if (!raw) return '';
    let d = null;
    if (typeof raw === 'number') {
        d = new Date(raw);
    } else if (typeof raw === 'string') {
        const num = Number(raw);
        if (!isNaN(num) && num > 0) {
            d = new Date(num);
        } else {
            d = new Date(raw.includes(' ') && !raw.includes('T') ? raw.replace(' ', 'T') : raw);
        }
    }
    if (!d || isNaN(d.getTime())) {
        return String(raw);
    }
    const pad = (n) => String(n).padStart(2, '0');
    const YYYY = d.getFullYear();
    const MM = pad(d.getMonth() + 1);
    const DD = pad(d.getDate());
    const hh = pad(d.getHours());
    const mm = pad(d.getMinutes());
    const ss = pad(d.getSeconds());
    return `${YYYY}-${MM}-${DD} ${hh}:${mm}:${ss}`;
}

function formatAllDurationsOnPage() {
    document.querySelectorAll('.js-format-duration').forEach(el => {
        const durMs = el.getAttribute('data-duration-ms');
        if (durMs !== null && durMs !== undefined && durMs !== '') {
            el.innerText = formatDurationInMinutes(durMs);
        }
    });
}

function formatAllTimestampsOnPage() {
    document.querySelectorAll('.js-format-timestamp').forEach(el => {
        const tsMs = el.getAttribute('data-timestamp-ms') || el.getAttribute('data-timestamp');
        if (tsMs !== null && tsMs !== undefined && tsMs !== '' && tsMs !== '0') {
            const formatted = formatTimestamp(tsMs);
            if (formatted) {
                el.innerText = formatted;
            }
        }
    });
}

function formatAllOnPage() {
    formatAllDurationsOnPage();
    formatAllTimestampsOnPage();
    initContributingRunsCollapseState();
    initBatchStickyFiltersObserver();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', formatAllOnPage);
} else {
    formatAllOnPage();
}

document.addEventListener('htmx:afterSwap', formatAllOnPage);

// Navigation & View Switches
function toggleSidebarCollapse() {
    const sidebar = document.getElementById('mainSidebar');
    if (sidebar) sidebar.classList.toggle('collapsed');
}

function showRunsList() {
    document.querySelectorAll('.view-state').forEach(el => el.classList.remove('active'));
    const runsPage = document.getElementById('pageRunsList');
    if (runsPage) runsPage.classList.add('active');
    
    document.querySelectorAll('#mainSidebar nav button').forEach(b => b.classList.remove('active'));
    const navBtn = document.getElementById('navRunsList');
    if (navBtn) navBtn.classList.add('active');

    const pageTitle = document.getElementById('pageTitle');
    if (pageTitle) pageTitle.innerHTML = '<span class="material-symbols-outlined text-accent">format_list_bulleted</span> Runs overview';
    
    const breadcrumb = document.getElementById('headerBreadcrumb');
    if (breadcrumb) breadcrumb.style.display = 'none';
}

function openBatchHistoryOverview(batchName, latestRunId) {
    activeBatchName = batchName || 'Default';
    
    // Show Batch History Overview view state inside batch-overview fragment
    const pageRunsList = document.getElementById('pageRunsList');
    const pageBatchHistory = document.getElementById('pageBatchHistoryOverview');
    
    if (pageRunsList && pageBatchHistory) {
        pageRunsList.classList.remove('active');
        pageBatchHistory.classList.add('active');
    } else {
        const container = document.getElementById('mainViewContainer');
        if (container) {
            fetch('/fragments/batch-history', { headers: { 'HX-Request': 'true' } })
                .then(res => res.text())
                .then(html => {
                    container.innerHTML = html;
                    if (window.htmx) htmx.process(container);
                    document.getElementById('pageBatchHistoryOverview')?.classList.add('active');
                    bindGlobalListeners();
                })
                .catch(() => {
                    window.location.href = '/';
                });
        } else {
            window.location.href = '/';
        }
    }

    const titleEl = document.getElementById('pageTitle');
    if (titleEl) titleEl.innerHTML = `<span class="material-symbols-outlined text-accent">trending_up</span> Batch History Overview`;
    
    const breadcrumbEl = document.getElementById('headerBreadcrumb');
    if (breadcrumbEl) breadcrumbEl.style.display = 'flex';
    
    const trail = document.getElementById('breadcrumbTrail');
    if (trail) trail.innerHTML = `<span>/</span> <span class="text-main" style="font-weight: 600;">${activeBatchName}</span>`;
    
    const overviewTitleEl = document.getElementById('batchOverviewTitle');
    if (overviewTitleEl) overviewTitleEl.innerText = activeBatchName;

    renderBatchSpecificRunsTable(activeBatchName);
    renderDynamicTrendChart(activeBatchName);
}

function renderBatchSpecificRunsTable(batchName) {
    const tbody = document.getElementById('batchSpecificRunsTableBody');
    if (!tbody) return;

    const overviewRows = document.querySelectorAll('#overviewRunsTableBody .run-directory-row');
    let html = '';
    overviewRows.forEach(row => {
        const rowBatchName = row.getAttribute('data-batch') || row.querySelector('.tag-batch')?.innerText.trim() || '';
        if (rowBatchName.includes(batchName)) {
            let clonedRow = row.cloneNode(true);
            html += `<tr class="clickable-row" hx-get="${row.getAttribute('hx-get')}" hx-target="#mainViewContainer" hx-swap="innerHTML">${clonedRow.innerHTML}</tr>`;
        }
    });

    tbody.innerHTML = html;
    htmx.process(tbody);
}

function applyCustomBatchLimit() {
    const input = document.getElementById('customBatchLimitInput');
    if (!input) return;
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = input.getAttribute('data-batch-name')
        || document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);
    const val = parseInt(input.value.trim(), 10);
    if (!isNaN(val) && val > 0) {
        urlParams.set('limit', val);
    } else {
        urlParams.delete('limit');
    }
    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function excludeBatchRun(runId) {
    if (!runId) return;
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    const existingExcludes = (urlParams.get('excludeRuns') || '')
        .split(',')
        .map(s => s.trim())
        .filter(Boolean);
    if (!existingExcludes.includes(runId)) {
        existingExcludes.push(runId);
    }

    urlParams.set('batchName', batchName);
    urlParams.set('excludeRuns', existingExcludes.join(','));

    const startInput = document.getElementById('batchStartDateInput');
    const startVal = startInput ? startInput.value.trim() : (urlParams.get('startDate') || urlParams.get('olderThanDate') || '');
    if (startVal) {
        urlParams.set('startDate', startVal);
    } else {
        urlParams.delete('startDate');
        urlParams.delete('olderThanDate');
    }
    const endInput = document.getElementById('batchEndDateInput');
    const endVal = endInput ? endInput.value.trim() : (urlParams.get('endDate') || '');
    if (endVal) {
        urlParams.set('endDate', endVal);
    } else {
        urlParams.delete('endDate');
    }

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function applyBatchRunsTimespan() {
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);

    const startInput = document.getElementById('batchStartDateInput');
    const startVal = startInput ? startInput.value.trim() : (urlParams.get('startDate') || '');
    if (startVal) {
        urlParams.set('startDate', startVal);
    } else {
        urlParams.delete('startDate');
        urlParams.delete('olderThanDate');
    }

    const endInput = document.getElementById('batchEndDateInput');
    const endVal = endInput ? endInput.value.trim() : (urlParams.get('endDate') || '');
    if (endVal) {
        urlParams.set('endDate', endVal);
    } else {
        urlParams.delete('endDate');
    }

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function applyBatchRunsDateCutoff(dateStr) {
    const startInput = document.getElementById('batchStartDateInput');
    if (startInput) {
        startInput.value = dateStr ? dateStr.trim() : '';
    }
    applyBatchRunsTimespan();
}

function applyBatchLimitPreset(limitVal) {
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);
    if (limitVal && limitVal > 0) {
        urlParams.set('limit', limitVal);
    } else {
        urlParams.delete('limit');
    }

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function resetBatchRunExclusions() {
    resetAllBatchRunScopeFilters();
}

function selectAllBatchRunScopeFilter(paramName, isChecked, menuId) {
    if (!paramName) return;
    const menuEl = menuId ? document.getElementById(menuId) : null;
    const applyNow = arguments.length > 3 ? arguments[3] : false;

    if (!isChecked) {
        // Unchecking "Select all" inside dropdown: deselect all options client-side and wait for user selection
        if (menuEl) {
            const allChk = menuEl.querySelector('.js-scope-chk-all');
            if (allChk) allChk.checked = false;
            menuEl.querySelectorAll('.js-scope-chk').forEach(cb => cb.checked = false);
            menuEl.dataset.pendingSelection = 'true';
        }
        return;
    }

    // Checking "Select all"
    if (menuEl) {
        delete menuEl.dataset.pendingSelection;
        menuEl.querySelectorAll('.js-scope-chk').forEach(cb => cb.checked = true);
        const allChk = menuEl.querySelector('.js-scope-chk-all');
        if (allChk) allChk.checked = true;
    }

    if (!applyNow) {
        // Do not reload until Apply button is clicked
        return;
    }

    const excludeParamName = 'exclude' + paramName.charAt(0).toUpperCase() + paramName.slice(1);
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);
    urlParams.delete(paramName);
    urlParams.delete(excludeParamName);

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function toggleBatchRunScopeFilter(paramName, value, menuId) {
    if (!paramName) return;
    const menuEl = menuId ? document.getElementById(menuId) : null;
    if (menuEl) {
        delete menuEl.dataset.pendingSelection;
        const allChk = menuEl.querySelector('.js-scope-chk-all');
        const optionChks = Array.from(menuEl.querySelectorAll('.js-scope-chk'));
        if (allChk && optionChks.length > 0) {
            allChk.checked = optionChks.every(cb => cb.checked);
        }
    }
}

function applyBatchRunScopeFilter(paramName, menuId) {
    if (!paramName) return;
    const menuEl = menuId ? document.getElementById(menuId) : null;
    const optionChks = menuEl ? Array.from(menuEl.querySelectorAll('.js-scope-chk')) : [];
    const candidateVals = optionChks.map(cb => cb.getAttribute('data-val') || cb.value).filter(Boolean);
    const checkedVals = optionChks.filter(cb => cb.checked).map(cb => cb.getAttribute('data-val') || cb.value).filter(Boolean);
    const uncheckedVals = optionChks.filter(cb => !cb.checked).map(cb => cb.getAttribute('data-val') || cb.value).filter(Boolean);

    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);

    const excludeParamName = 'exclude' + paramName.charAt(0).toUpperCase() + paramName.slice(1);

    if (candidateVals.length === 0 || checkedVals.length === candidateVals.length) {
        // All candidates selected: clear both inclusion and exclusion params (default state)
        urlParams.delete(paramName);
        urlParams.delete(excludeParamName);
    } else if (checkedVals.length === 0) {
        // None selected: explicit _none_ inclusion
        urlParams.set(paramName, '_none_');
        urlParams.delete(excludeParamName);
    } else if (checkedVals.length <= uncheckedVals.length) {
        // Fewer or equal items selected than unselected: inclusion mode
        urlParams.set(paramName, checkedVals.join(','));
        urlParams.delete(excludeParamName);
    } else {
        // Fewer items unselected: exclusion mode
        urlParams.set(excludeParamName, uncheckedVals.join(','));
        urlParams.delete(paramName);
    }

    if (menuEl) {
        delete menuEl.dataset.pendingSelection;
        optionChks.forEach(cb => cb.setAttribute('data-initial-checked', String(cb.checked)));
    }

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function removeBatchRunScopeFilter(paramName, value) {
    if (!paramName || !value) return;
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);
    const excludeParamName = 'exclude' + paramName.charAt(0).toUpperCase() + paramName.slice(1);

    if (value === '_none_') {
        urlParams.delete(paramName);
        urlParams.delete(excludeParamName);
    } else {
        const currentVals = (urlParams.get(paramName) || '')
            .split(',')
            .map(s => s.trim())
            .filter(Boolean);

        const targetNorm = value.trim().toLowerCase();
        const filtered = currentVals.filter(v => v.toLowerCase() !== targetNorm);

        if (filtered.length > 0) {
            urlParams.set(paramName, filtered.join(','));
        } else {
            urlParams.delete(paramName);
        }
    }

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function resetAllBatchRunScopeFilters() {
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);
    urlParams.delete('excludeRuns');
    urlParams.delete('olderThanDate');
    urlParams.delete('startDate');
    urlParams.delete('endDate');
    urlParams.delete('runBrowsers');
    urlParams.delete('runLocales');
    urlParams.delete('runEnvs');
    urlParams.delete('runAreas');
    urlParams.delete('runTags');
    urlParams.delete('excludeRunBrowsers');
    urlParams.delete('excludeRunLocales');
    urlParams.delete('excludeRunEnvs');
    urlParams.delete('excludeRunAreas');
    urlParams.delete('excludeRunTags');

    const keysToDelete = [];
    for (const key of urlParams.keys()) {
        if (key.startsWith('runTag_') || key.startsWith('excludeRunTag_')) {
            keysToDelete.push(key);
        }
    }
    keysToDelete.forEach(k => urlParams.delete(k));

    const startInput = document.getElementById('batchStartDateInput');
    if (startInput) startInput.value = '';
    const endInput = document.getElementById('batchEndDateInput');
    if (endInput) endInput.value = '';
    const cutoffInput = document.getElementById('batchCutoffDateInput');
    if (cutoffInput) cutoffInput.value = '';

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function clearBatchTimespan() {
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);
    urlParams.delete('startDate');
    urlParams.delete('endDate');
    urlParams.delete('olderThanDate');

    const startInput = document.getElementById('batchStartDateInput');
    if (startInput) startInput.value = '';
    const endInput = document.getElementById('batchEndDateInput');
    if (endInput) endInput.value = '';
    const cutoffInput = document.getElementById('batchCutoffDateInput');
    if (cutoffInput) cutoffInput.value = '';

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

function clearBatchCutoffDate() {
    clearBatchTimespan();
}

function clearExcludedRuns() {
    const urlParams = new URLSearchParams(window.location.search);
    const batchName = document.querySelector('#pageBatchHistoryOverview')?.getAttribute('data-batch-name')
        || document.getElementById('customBatchLimitInput')?.getAttribute('data-batch-name')
        || urlParams.get('batchName')
        || (typeof activeBatchName !== 'undefined' ? activeBatchName : 'Default');

    urlParams.set('batchName', batchName);
    urlParams.delete('excludeRuns');

    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

const batchActiveRatioFilters = {
    browser: new Set(),
    locale: new Set(),
    env: new Set(),
    area: new Set(),
    tag: new Set()
};

function resetAllBatchRatioFilters() {
    for (const key in batchActiveRatioFilters) {
        if (batchActiveRatioFilters[key] && typeof batchActiveRatioFilters[key].clear === 'function') {
            batchActiveRatioFilters[key].clear();
        }
    }
}

function getElementTags(el) {
    if (!el) return {};
    if (el._cachedTags) return el._cachedTags;
    let tags = {};
    const tagsJson = el.getAttribute('data-tags');
    if (tagsJson) {
        try {
            tags = JSON.parse(tagsJson) || {};
        } catch (e) {
            tags = {};
        }
        // Extract multi-valued duplicate keys if present in raw string
        if (typeof tagsJson === 'string' && tagsJson.includes(':')) {
            const pairRegex = /"([^"\\]*(?:\\.[^"\\]*)*)"\s*:\s*(?:"([^"\\]*(?:\\.[^"\\]*)*)"|\[([^\]]*)\])/g;
            let m;
            const seenKeys = {};
            while ((m = pairRegex.exec(tagsJson)) !== null) {
                const k = m[1].toLowerCase();
                const strVal = m[2];
                if (strVal !== undefined) {
                    const cleanVal = strVal.replace(/\\"/g, '"').replace(/\\\\/g, '\\').trim();
                    if (!seenKeys[k]) {
                        seenKeys[k] = [cleanVal];
                    } else if (!seenKeys[k].includes(cleanVal)) {
                        seenKeys[k].push(cleanVal);
                    }
                }
            }
            for (const k in seenKeys) {
                if (seenKeys[k].length > 1) {
                    tags[k] = seenKeys[k];
                }
            }
        }
    }
    const batchAttr = el.getAttribute('data-batch');
    if (batchAttr && !tags['batch']) {
        tags['batch'] = batchAttr;
    }
    el._cachedTags = tags;
    return tags;
}

function getElementTagValue(el, tagKey) {
    if (!el || !tagKey) return null;
    const tags = getElementTags(el);
    const cleanKey = tagKey.toLowerCase().replace(/^tag_/, '').replace(/[^a-z0-9]/g, '_');
    for (const k in tags) {
        const normK = k.toLowerCase().replace(/[^a-z0-9]/g, '_');
        if (normK === cleanKey || k.toLowerCase() === tagKey.toLowerCase()) {
            return tags[k];
        }
    }
    if (cleanKey === 'batch' || cleanKey === 'tag') {
        return el.getAttribute('data-batch') || null;
    }
    if (cleanKey === 'locale' || cleanKey === 'locales') {
        return el.getAttribute('data-locales') || el.getAttribute('data-location') || null;
    }
    if (cleanKey === 'browser' || cleanKey === 'browsers') {
        return el.getAttribute('data-browsers') || el.getAttribute('data-browser') || null;
    }
    if (cleanKey === 'area' || cleanKey === 'areas') {
        return el.getAttribute('data-areas') || el.getAttribute('data-area') || null;
    }
    if (cleanKey === 'env' || cleanKey === 'environment') {
        return el.getAttribute('data-env') || null;
    }
    return null;
}

function extractCandidateTagValues(rawVal) {
    const results = [];
    if (Array.isArray(rawVal)) {
        rawVal.forEach(item => {
            if (item !== null && item !== undefined) {
                String(item).split(',').forEach(sub => {
                    const s = sub.trim().toUpperCase();
                    if (s && !results.includes(s)) results.push(s);
                });
            }
        });
    } else if (rawVal !== null && rawVal !== undefined && String(rawVal).trim().length > 0) {
        String(rawVal).split(',').forEach(sub => {
            const s = sub.trim().toUpperCase();
            if (s && !results.includes(s)) results.push(s);
        });
    }
    return results.length > 0 ? results : ['DEFAULT'];
}

function matchTagFilters(el) {
    if (!el) return true;

    // 1. Legacy tag filter
    if (batchActiveRatioFilters.tag && batchActiveRatioFilters.tag.size > 0) {
        const selectedTags = Array.from(batchActiveRatioFilters.tag);
        const tags = getElementTags(el);
        const batchVal = (tags['batch'] || el.getAttribute('data-batch') || '').trim().toUpperCase();
        const matchesLegacy = selectedTags.some(t => {
            if (batchVal && (batchVal === t || batchVal.includes(t) || t.includes(batchVal))) return true;
            for (const k in tags) {
                const candidates = extractCandidateTagValues(tags[k]);
                if (candidates.some(c => c === t || c.includes(t) || t.includes(c))) return true;
            }
            return false;
        });
        if (!matchesLegacy) return false;
    }

    // 2. Dynamic tag filters
    for (const filterKey in batchActiveRatioFilters) {
        if (!filterKey.startsWith('tag_')) continue;
        const activeSet = batchActiveRatioFilters[filterKey];
        if (!activeSet || activeSet.size === 0) continue;

        const cleanKey = filterKey.substring(4);
        const rawVal = getElementTagValue(el, cleanKey);
        const candidates = extractCandidateTagValues(rawVal);

        const selectedVals = Array.from(activeSet);
        const matches = selectedVals.some(sv => {
            const normSv = sv.trim().toUpperCase();
            if (normSv.startsWith('NOT ')) {
                const baseTag = normSv.substring(4).trim();
                return candidates.some(v => v === 'DEFAULT' || v === normSv || v === 'FALSE' || (v !== baseTag && v !== 'TRUE'));
            }
            if (candidates.includes('TRUE') && normSv === cleanKey.toUpperCase()) {
                return true;
            }
            const normSvArea = normalizeAreaFilterVal(normSv);
            return candidates.some(v => v === normSv || (normSvArea && normalizeAreaFilterVal(v) === normSvArea));
        });

        if (!matches) {
            return false;
        }
    }
    return true;
}

function matchEntryFilters(entry, excludeType) {
    const isEnvFiltered = (excludeType !== 'env') && batchActiveRatioFilters.env && batchActiveRatioFilters.env.size > 0;
    if (isEnvFiltered) {
        let env = (entry.getAttribute('data-env') || '').trim().toUpperCase();
        if (!env) env = 'DEFAULT';
        const selectedEnvs = Array.from(batchActiveRatioFilters.env);
        const matchEnv = selectedEnvs.some(e => env === e || env.includes(e) || e.includes(env));
        if (!matchEnv) return false;
    }

    const isLocFiltered = (excludeType !== 'locale') && batchActiveRatioFilters.locale && batchActiveRatioFilters.locale.size > 0;
    if (isLocFiltered) {
        let loc = (entry.getAttribute('data-location') || '').trim().toUpperCase();
        if (!loc || loc === 'UNKNOWN') loc = 'DEFAULT';
        const selectedLocs = Array.from(batchActiveRatioFilters.locale);
        const matchLoc = selectedLocs.some(l => loc === l || loc.includes(l) || l.includes(loc));
        if (!matchLoc) return false;
    }

    const isBrowserFiltered = (excludeType !== 'browser') && batchActiveRatioFilters.browser && batchActiveRatioFilters.browser.size > 0;
    if (isBrowserFiltered) {
        let browser = (entry.getAttribute('data-browser') || '').trim().toUpperCase();
        if (!browser) browser = 'UNKNOWN';
        const selectedBrowsers = Array.from(batchActiveRatioFilters.browser);
        const matchBrowser = selectedBrowsers.some(b => browser === b || browser.includes(b) || b.includes(browser));
        if (!matchBrowser) return false;
    }

    const isAreaFiltered = (excludeType !== 'area') && batchActiveRatioFilters.area && batchActiveRatioFilters.area.size > 0;
    if (isAreaFiltered) {
        let area = (entry.getAttribute('data-area') || '').trim().toUpperCase();
        if (!area) area = '@BROWSING (DEFAULT)';
        const selectedAreas = Array.from(batchActiveRatioFilters.area);
        const matchArea = selectedAreas.some(a => {
            const normA = normalizeAreaFilterVal(a);
            const normArea = normalizeAreaFilterVal(area);
            return normArea && normA && normArea === normA;
        });
        if (!matchArea) return false;
    }

    // Legacy tag filter
    const isTagFiltered = (excludeType !== 'tag') && batchActiveRatioFilters.tag && batchActiveRatioFilters.tag.size > 0;
    if (isTagFiltered) {
        let batch = (entry.getAttribute('data-batch') || '').trim().toUpperCase();
        const tags = getElementTags(entry);
        const selectedTags = Array.from(batchActiveRatioFilters.tag);
        const matchTag = selectedTags.some(t => {
            if (batch && (batch === t || batch.includes(t) || t.includes(batch))) return true;
            for (const k in tags) {
                const v = String(tags[k] || '').trim().toUpperCase();
                if (v && (v === t || v.includes(t) || t.includes(v))) return true;
            }
            return false;
        });
        if (!matchTag) return false;
    }

    // Dynamic tag filters
    const normExclude = excludeType ? excludeType.toLowerCase() : null;
    for (const filterKey in batchActiveRatioFilters) {
        if (!filterKey.startsWith('tag_')) continue;
        if (normExclude && (filterKey === normExclude || filterKey.substring(4) === normExclude.replace(/^tag_/, ''))) {
            continue;
        }
        const activeSet = batchActiveRatioFilters[filterKey];
        if (!activeSet || activeSet.size === 0) continue;

        const cleanKey = filterKey.substring(4);
        const rawVal = getElementTagValue(entry, cleanKey);
        const candidates = extractCandidateTagValues(rawVal);
        const selectedVals = Array.from(activeSet);
        const matches = selectedVals.some(sv => {
            const normSv = sv.trim().toUpperCase();
            if (normSv.startsWith('NOT ')) {
                const baseTag = normSv.substring(4).trim();
                return candidates.some(v => v === 'DEFAULT' || v === normSv || v === 'FALSE' || (v !== baseTag && v !== 'TRUE'));
            }
            if (candidates.includes('TRUE') && normSv === cleanKey.toUpperCase()) {
                return true;
            }
            const normSvArea = normalizeAreaFilterVal(normSv);
            return candidates.some(v => v === normSv || (normSvArea && normalizeAreaFilterVal(v) === normSvArea));
        });
        if (!matches) return false;
    }

    return true;
}

function normalizeAreaFilterVal(str) {
    if (!str) return '';
    let s = str.trim().toUpperCase();
    if (s.startsWith('@')) s = s.substring(1).trim();
    if (s === 'GENERAL' || s === 'BROWSING (DEFAULT)' || s === 'DEFAULT') return 'BROWSING (DEFAULT)';
    return s;
}

window.applyCustomBatchLimit = applyCustomBatchLimit;
window.applyBatchLimitPreset = applyBatchLimitPreset;
window.excludeBatchRun = excludeBatchRun;
window.applyBatchRunsTimespan = applyBatchRunsTimespan;
window.clearBatchTimespan = clearBatchTimespan;
window.applyBatchRunsDateCutoff = applyBatchRunsDateCutoff;
window.resetBatchRunExclusions = resetBatchRunExclusions;
window.selectAllBatchRunScopeFilter = selectAllBatchRunScopeFilter;
window.toggleBatchRunScopeFilter = toggleBatchRunScopeFilter;
window.applyBatchRunScopeFilter = applyBatchRunScopeFilter;
window.removeBatchRunScopeFilter = removeBatchRunScopeFilter;
window.resetAllBatchRunScopeFilters = resetAllBatchRunScopeFilters;
window.clearBatchCutoffDate = clearBatchCutoffDate;
window.clearExcludedRuns = clearExcludedRuns;
window.recalculateBatchLatestStatusPie = recalculateBatchLatestStatusPie;
window.applyBatchDirectoryFilters = applyBatchDirectoryFilters;
window.resetBatchFilters = resetBatchFilters;
window.resetBatchRatioFilter = resetBatchRatioFilter;
window.syncBatchRatioBarsVisualState = syncBatchRatioBarsVisualState;
window.filterFromRatioBar = filterFromRatioBar;
window.batchActiveRatioFilters = batchActiveRatioFilters;
window.resetAllBatchRatioFilters = resetAllBatchRatioFilters;
window.normalizeAreaFilterVal = normalizeAreaFilterVal;
window.getElementTags = getElementTags;
window.getElementTagValue = getElementTagValue;
window.matchTagFilters = matchTagFilters;
window.matchEntryFilters = matchEntryFilters;
window.extractCandidateTagValues = extractCandidateTagValues;

function handleTrendChartHover(event) {
    const svg = document.getElementById('mainTrendSvg');
    const guide = document.getElementById('trendHoverGuide');
    const tooltip = document.getElementById('trendHoverTooltip');
    if (!svg || !guide || !tooltip) return;

    const registry = batchRunStatsRegistry[activeBatchName] || (Object.values(batchRunStatsRegistry)[0] || { runIds: [], stats: {} });
    if (!registry || !registry.runIds || registry.runIds.length === 0) return;

    const rect = svg.getBoundingClientRect();
    const mouseX = event.clientX - rect.left;
    const svgWidth = rect.width;
    
    const viewBoxX = (mouseX / svgWidth) * 800;

    const runIds = registry.runIds;
    let closestRun = runIds[runIds.length - 1];
    let minDist = 99999;

    runIds.forEach(id => {
        const stat = registry.stats[id];
        if (!stat) return;
        const dist = Math.abs(viewBoxX - stat.x);
        if (dist < minDist) {
            minDist = dist;
            closestRun = id;
        }
    });

    const stat = registry.stats[closestRun];
    if (!stat) return;

    guide.setAttribute('x1', stat.x);
    guide.setAttribute('x2', stat.x);
    guide.style.display = 'block';

    document.querySelectorAll('.trend-node-circle').forEach(c => {
        c.setAttribute('r', '6');
        c.style.filter = '';
    });
    const activeCircle = document.querySelector(`#trendPoint-${closestRun} circle`);
    if (activeCircle) {
        activeCircle.setAttribute('r', '9');
        activeCircle.style.filter = 'url(#glow)';
    }

    const runIdEl = document.getElementById('ttRunId');
    if (runIdEl) runIdEl.innerText = `#${stat.runId || closestRun}`;

    const headerHealed = document.getElementById('ttHeaderHealed');
    if (headerHealed) {
        if (stat.totalHealed > 0) {
            headerHealed.innerText = `✨ ${stat.totalHealed} healed`;
            headerHealed.style.display = 'inline-flex';
        } else {
            headerHealed.style.display = 'none';
        }
    }

    const headerAi = document.getElementById('ttHeaderAi');
    if (headerAi) {
        if (stat.totalAi > 0) {
            headerAi.innerText = `${stat.totalAi} AI`;
            headerAi.style.display = 'inline-flex';
        } else {
            headerAi.style.display = 'none';
        }
    }

    const timestampEl = document.getElementById('ttTimestamp');
    if (timestampEl) timestampEl.innerText = stat.time || 'Recently';

    const runTitle = document.getElementById('ttRunTitle') || document.getElementById('trendTooltipTitle');
    if (runTitle) {
        let badgesHtml = '';
        if (stat.totalHealed > 0) {
            badgesHtml += ` <span class="badge-healed-inline badge-pass-healed" style="font-size: 0.68rem; padding: 0.05rem 0.35rem; margin-left: 0.35rem;">✨ ${stat.totalHealed} healed</span>`;
        }
        if (stat.totalAi > 0) {
            badgesHtml += ` <span class="badge-ai-inline badge-pass-ai" style="font-size: 0.68rem; padding: 0.05rem 0.35rem; margin-left: 0.35rem;">${stat.totalAi} AI</span>`;
        }
        if (badgesHtml) {
            runTitle.innerHTML = `${stat.title}${badgesHtml}`;
        } else {
            runTitle.innerText = stat.title;
        }
    }

    const passRate = document.getElementById('ttPassRate') || document.getElementById('trendTooltipRate');
    if (passRate) {
        let rateText = (stat.rate || '0%').trim();
        if (!rateText.toLowerCase().includes('pass')) {
            rateText += ' PASS';
        }
        passRate.innerText = rateText.toUpperCase();
    }

    const totalVal = document.getElementById('ttTotalVal');
    if (totalVal) {
        totalVal.innerText = (stat.total && stat.total.includes('Tests')) ? stat.total : `${stat.total || 0} Tests`;
    }

    function updateTooltipRow(rowId, valId, healedId, aiId, count, healedCount, aiCount) {
        const row = document.getElementById(rowId);
        const val = document.getElementById(valId);
        const healed = healedId ? document.getElementById(healedId) : null;
        const ai = aiId ? document.getElementById(aiId) : null;
        if (val) val.innerText = count;
        if (healed) {
            if (healedCount > 0) {
                healed.innerText = `✨ ${healedCount} healed`;
                healed.style.display = 'inline-flex';
            } else {
                healed.style.display = 'none';
            }
        }
        if (ai) {
            if (aiCount > 0) {
                ai.innerText = `${aiCount} AI`;
                ai.style.display = 'inline-flex';
            } else {
                ai.style.display = 'none';
            }
        }
        if (row) {
            row.style.display = count > 0 ? 'flex' : 'none';
        }
    }

    updateTooltipRow('ttRowPass', 'ttPassVal', 'ttHealedPass', 'ttAiPass', stat.pass, stat.passHealed, stat.passAi);
    updateTooltipRow('ttRowFixed', 'ttFixedVal', 'ttHealedFixed', 'ttAiFixed', stat.fixed, stat.fixedHealed, stat.fixedAi);
    updateTooltipRow('ttRowKnown', 'ttKnownVal', 'ttHealedKnown', 'ttAiKnown', stat.known, stat.knownHealed, stat.knownAi);
    updateTooltipRow('ttRowUnknown', 'ttUnknownVal', 'ttHealedUnknown', 'ttAiUnknown', stat.unknown, stat.unknownHealed, stat.unknownAi);
    updateTooltipRow('ttRowIgnored', 'ttIgnoredVal', null, null, stat.ignored, 0, 0);

    const legacyPass = document.getElementById('trendTooltipPass');
    if (legacyPass) legacyPass.innerText = stat.pass;
    const legacyFixed = document.getElementById('trendTooltipFixed');
    if (legacyFixed) legacyFixed.innerText = stat.fixed;
    const legacyKnown = document.getElementById('trendTooltipKnown');
    if (legacyKnown) legacyKnown.innerText = stat.known;
    const legacyUnknown = document.getElementById('trendTooltipUnknown');
    if (legacyUnknown) legacyUnknown.innerText = stat.unknown;
    const legacyIgnored = document.getElementById('trendTooltipIgnored');
    if (legacyIgnored) legacyIgnored.innerText = stat.ignored;

    const tooltipWidth = 265;
    let ttLeft = (stat.x / 800) * svgWidth + 15;
    if (ttLeft + tooltipWidth + 10 > svgWidth) {
        ttLeft = Math.max(10, (stat.x / 800) * svgWidth - tooltipWidth - 15);
    }

    tooltip.style.left = ttLeft + 'px';
    tooltip.style.top = '20px';
    tooltip.style.display = 'flex';
}

function hideTrendChartHover() {
    const guide = document.getElementById('trendHoverGuide');
    const tooltip = document.getElementById('trendHoverTooltip');
    if (guide) guide.style.display = 'none';
    if (tooltip) tooltip.style.display = 'none';

    document.querySelectorAll('.trend-node-circle').forEach(c => {
        c.setAttribute('r', '6');
        c.style.filter = '';
    });
    const registry = batchRunStatsRegistry[activeBatchName] || (Object.values(batchRunStatsRegistry)[0] || { runIds: [] });
    if (!registry || !registry.runIds || registry.runIds.length === 0) return;
    const latestId = registry.runIds[registry.runIds.length - 1];
    const lastCircle = document.querySelector(`#trendPoint-${latestId} circle`);
    if (lastCircle) lastCircle.setAttribute('r', '7');
}

let selectedMainTrendKey = null;

function toggleMainTrendLegendSelect(key) {
    selectedMainTrendKey = (selectedMainTrendKey === key) ? null : key;
    renderDynamicTrendChart(activeBatchName);
}
window.toggleMainTrendLegendSelect = toggleMainTrendLegendSelect;

function renderDynamicTrendChart(targetBatchName) {
    const batchName = targetBatchName || activeBatchName || 'Default';
    const svg = document.getElementById('mainTrendSvg');
    if (!svg) return;

    // Collect runs from DOM table
    let rowElements = Array.from(document.querySelectorAll('#batchRunsHistoryTableBody tr.run-history-row'));
    if (rowElements.length === 0) {
        rowElements = Array.from(document.querySelectorAll('#batchSpecificRunsTableBody tr'));
    }
    if (rowElements.length === 0) {
        rowElements = Array.from(document.querySelectorAll('#overviewRunsTableBody .run-directory-row'));
    }
    rowElements = rowElements.filter(r => r.style.display !== 'none');

    // In tables, rows are typically displayed newest to oldest. For trend charts, sort oldest to newest (left-to-right).
    const runsData = [];
    rowElements.slice().reverse().forEach(row => {
        const tagBatchEl = row.querySelector('.tag-batch span:last-child') || row.querySelector('.tag-batch');
        const rowBatch = row.getAttribute('data-batch') || (tagBatchEl ? tagBatchEl.innerText.trim() : '');
        const isHistoryTable = row.closest('#batchRunsHistoryTableBody') !== null;
        if (!isHistoryTable && targetBatchName && rowBatch && !rowBatch.includes(targetBatchName) && !targetBatchName.includes(rowBatch)) {
            return;
        }
        const runId = row.getAttribute('data-run-id') || row.querySelector('strong.text-mono')?.innerText.replace('#', '').trim() || '#RUN_ID';
        const pass = parseInt(row.getAttribute('data-pass') || row.querySelector('.seg-pass')?.innerText.trim() || '0', 10) || 0;
        const fixed = parseInt(row.getAttribute('data-fixed') || row.querySelector('.seg-fixed')?.innerText.trim() || '0', 10) || 0;
        const known = parseInt(row.getAttribute('data-known') || row.querySelector('.seg-known')?.innerText.trim() || '0', 10) || 0;
        const unknown = parseInt(row.getAttribute('data-unknown') || row.querySelector('.seg-unknown')?.innerText.trim() || '0', 10) || 0;
        const ignored = parseInt(row.getAttribute('data-ignored') || row.querySelector('.seg-ignored')?.innerText.trim() || '0', 10) || 0;
        const passHealed = parseInt(row.getAttribute('data-pass-healed') || '0', 10) || 0;
        const fixedHealed = parseInt(row.getAttribute('data-fixed-healed') || '0', 10) || 0;
        const knownHealed = parseInt(row.getAttribute('data-known-healed') || '0', 10) || 0;
        const unknownHealed = parseInt(row.getAttribute('data-unknown-healed') || '0', 10) || 0;
        const totalHealed = passHealed + fixedHealed + knownHealed + unknownHealed;
        const passAi = parseInt(row.getAttribute('data-pass-ai') || '0', 10) || 0;
        const fixedAi = parseInt(row.getAttribute('data-fixed-ai') || '0', 10) || 0;
        const knownAi = parseInt(row.getAttribute('data-known-ai') || '0', 10) || 0;
        const unknownAi = parseInt(row.getAttribute('data-unknown-ai') || '0', 10) || 0;
        const totalAi = passAi + fixedAi + knownAi + unknownAi;
        const total = parseInt(row.getAttribute('data-total') || row.querySelector('td.text-mono:nth-of-type(4)')?.innerText.trim() || '0', 10) || (pass + fixed + known + unknown + ignored) || 1;
        const rate = row.getAttribute('data-rate') || (row.querySelector('.badge-status')?.innerText.trim() || '0%');
        const time = row.getAttribute('data-time') || row.querySelector('td.text-muted')?.innerText.trim() || 'Recently';

        runsData.push({ runId, pass, fixed, known, unknown, ignored, passHealed, fixedHealed, knownHealed, unknownHealed, totalHealed, passAi, fixedAi, knownAi, unknownAi, totalAi, total, rate, time });
    });

    if (runsData.length === 0) {
        batchRunStatsRegistry[batchName] = { runIds: [], maxTests: 0, stats: {} };
        [
            'trendLayerIgnored',
            'trendLayerUnknown', 'trendLayerUnknownHealed', 'trendLayerUnknownHealedPattern', 'trendLayerUnknownAi', 'trendLayerUnknownAiPattern',
            'trendLayerKnown', 'trendLayerKnownHealed', 'trendLayerKnownHealedPattern', 'trendLayerKnownAi', 'trendLayerKnownAiPattern',
            'trendLayerFixed', 'trendLayerFixedHealed', 'trendLayerFixedHealedPattern', 'trendLayerFixedAi', 'trendLayerFixedAiPattern',
            'trendLayerPass', 'trendLayerPassHealed', 'trendLayerPassHealedPattern', 'trendLayerPassAi', 'trendLayerPassAiPattern',
            'trendTopStroke'
        ].forEach(id => {
            document.getElementById(id)?.setAttribute('d', '');
        });
        const dotsGroup = document.getElementById('trendSvgDots');
        if (dotsGroup) dotsGroup.innerHTML = '';
        return;
    }

    const runIds = runsData.map(r => r.runId);
    const maxTests = Math.max(1, ...runsData.map(r => r.total));
    const svgHeight = 160;
    const baseScale = svgHeight / maxTests;

    const stats = {};
    const count = runsData.length;
    const coords = runsData.map((r, idx) => {
        const x = count === 1 ? 400 : 50 + (idx / (count - 1)) * 750;
        stats[r.runId] = {
            x,
            runId: r.runId,
            time: r.time,
            title: `Run #${r.runId} (${r.time})`,
            pass: r.pass,
            fixed: r.fixed,
            known: r.known,
            unknown: r.unknown,
            ignored: r.ignored,
            passHealed: r.passHealed,
            fixedHealed: r.fixedHealed,
            knownHealed: r.knownHealed,
            unknownHealed: r.unknownHealed,
            totalHealed: r.totalHealed,
            passAi: r.passAi,
            fixedAi: r.fixedAi,
            knownAi: r.knownAi,
            unknownAi: r.unknownAi,
            totalAi: r.totalAi,
            total: `${r.total} Tests`,
            rate: r.rate
        };

        const cleanUnknown = Math.max(0, r.unknown - r.unknownHealed - r.unknownAi);
        const cleanKnown = Math.max(0, r.known - r.knownHealed - r.knownAi);
        const cleanFixed = Math.max(0, r.fixed - r.fixedHealed - r.fixedAi);
        const cleanPass = Math.max(0, r.pass - r.passHealed - r.passAi);

        const y0 = 190;
        const y1 = y0 - (r.ignored * baseScale);
        const y2_clean = y1 - (cleanUnknown * baseScale);
        const y2_healed = y2_clean - (r.unknownHealed * baseScale);
        const y2 = y2_healed - (r.unknownAi * baseScale);
        const y3_clean = y2 - (cleanKnown * baseScale);
        const y3_healed = y3_clean - (r.knownHealed * baseScale);
        const y3 = y3_healed - (r.knownAi * baseScale);
        const y4_clean = y3 - (cleanFixed * baseScale);
        const y4_healed = y4_clean - (r.fixedHealed * baseScale);
        const y4 = y4_healed - (r.fixedAi * baseScale);
        const y5_clean = y4 - (cleanPass * baseScale);
        const y5_healed = y5_clean - (r.passHealed * baseScale);
        const y5 = y5_healed - (r.passAi * baseScale);

        return {
            x,
            y0,
            y1,
            y2_clean,
            y2_healed,
            y2,
            y3_clean,
            y3_healed,
            y3,
            y4_clean,
            y4_healed,
            y4,
            y5_clean,
            y5_healed,
            y5,
            id: r.runId,
            passHealed: r.passHealed,
            fixedHealed: r.fixedHealed,
            knownHealed: r.knownHealed,
            unknownHealed: r.unknownHealed,
            totalHealed: r.totalHealed,
            passAi: r.passAi,
            fixedAi: r.fixedAi,
            knownAi: r.knownAi,
            unknownAi: r.unknownAi,
            totalAi: r.totalAi
        };
    });

    batchRunStatsRegistry[batchName] = { runIds, maxTests, stats };

    const mainTrendMetricMeta = {
        passClean: { label: 'Passed Clean', color: 'var(--status-pass, #10b981)', getVal: r => Math.max(0, r.pass - r.passHealed - r.passAi) },
        passHealed: { label: 'Passed Healed', color: 'var(--status-pass-healed, #34d399)', getVal: r => r.passHealed },
        passAi: { label: 'Passed AI-Driven', color: 'var(--status-pass, #10b981)', getVal: r => r.passAi },
        fixedClean: { label: 'Succeeded with Known Bug', color: 'var(--status-fixed, #3b82f6)', getVal: r => Math.max(0, r.fixed - r.fixedHealed - r.fixedAi) },
        fixedHealed: { label: 'Succeeded Fixed Healed', color: 'var(--status-fixed-healed, #60a5fa)', getVal: r => r.fixedHealed },
        fixedAi: { label: 'Succeeded Fixed AI-Driven', color: 'var(--status-fixed, #3b82f6)', getVal: r => r.fixedAi },
        knownClean: { label: 'Failed due to Known Bug', color: 'var(--status-known-fail, #f97316)', getVal: r => Math.max(0, r.known - r.knownHealed - r.knownAi) },
        knownHealed: { label: 'Failed Known Healed', color: 'var(--status-known-healed, #fb923c)', getVal: r => r.knownHealed },
        knownAi: { label: 'Failed Known AI-Driven', color: 'var(--status-known-fail, #f97316)', getVal: r => r.knownAi },
        unknownClean: { label: 'Failed without Issue Info', color: 'var(--status-unknown-fail, #ef4444)', getVal: r => Math.max(0, r.unknown - r.unknownHealed - r.unknownAi) },
        unknownHealed: { label: 'Failed Unknown Healed', color: 'var(--status-unknown-healed, #f87171)', getVal: r => r.unknownHealed },
        unknownAi: { label: 'Failed Unknown AI-Driven', color: 'var(--status-unknown-fail, #ef4444)', getVal: r => r.unknownAi },
        ignored: { label: 'Ignored / Skipped', color: 'var(--status-ignored, #9ca3af)', getVal: r => r.ignored }
    };

    if (selectedMainTrendKey && mainTrendMetricMeta[selectedMainTrendKey]) {
        const meta = mainTrendMetricMeta[selectedMainTrendKey];
        [
            'trendLayerIgnored',
            'trendLayerUnknown', 'trendLayerUnknownHealed', 'trendLayerUnknownHealedPattern', 'trendLayerUnknownAi', 'trendLayerUnknownAiPattern',
            'trendLayerKnown', 'trendLayerKnownHealed', 'trendLayerKnownHealedPattern', 'trendLayerKnownAi', 'trendLayerKnownAiPattern',
            'trendLayerFixed', 'trendLayerFixedHealed', 'trendLayerFixedHealedPattern', 'trendLayerFixedAi', 'trendLayerFixedAiPattern',
            'trendLayerPass', 'trendLayerPassHealed', 'trendLayerPassHealedPattern', 'trendLayerPassAi', 'trendLayerPassAiPattern'
        ].forEach(id => {
            document.getElementById(id)?.setAttribute('d', '');
        });

        const metricYs = runsData.map(r => 190 - (meta.getVal(r) * baseScale));

        let linePathStr = '';
        if (coords.length === 1) {
            const halfWidth = 30;
            const leftX = Math.max(50, coords[0].x - halfWidth);
            const rightX = Math.min(800, coords[0].x + halfWidth);
            linePathStr = `M ${leftX} ${metricYs[0]} L ${rightX} ${metricYs[0]}`;
        } else {
            linePathStr = `M ${coords[0].x} ${metricYs[0]}`;
            for (let i = 0; i < coords.length - 1; i++) {
                const x1 = coords[i].x;
                const y1 = metricYs[i];
                const x2 = coords[i + 1].x;
                const y2 = metricYs[i + 1];
                const dx = (x2 - x1) / 2;
                linePathStr += ` C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`;
            }
        }

        const topStrokeEl = document.getElementById('trendTopStroke');
        if (topStrokeEl) {
            topStrokeEl.setAttribute('d', linePathStr);
            topStrokeEl.style.stroke = meta.color;
        }

        const dotsGroup = document.getElementById('trendSvgDots');
        if (dotsGroup) {
            dotsGroup.innerHTML = coords.map((c, idx) => `
                <g class="trend-click-col" id="trendPoint-${c.id}" 
                   hx-get="/run-report?runId=${encodeURIComponent(c.id)}" hx-target="#mainViewContainer" hx-swap="innerHTML" hx-push-url="true" style="cursor: pointer;">
                    <circle cx="${c.x}" cy="${metricYs[idx]}" r="6" fill="${meta.color}" stroke="#ffffff" stroke-width="2" class="trend-node-circle"/>
                </g>
            `).join('');
            if (window.htmx) htmx.process(dotsGroup);
        }

        document.querySelectorAll('.allure-legend:not(.evo-legend) .legend-item').forEach(item => {
            const k = item.getAttribute('data-trend-key');
            if (k === selectedMainTrendKey) {
                item.classList.add('is-active');
                item.classList.remove('is-dimmed');
            } else {
                item.classList.add('is-dimmed');
                item.classList.remove('is-active');
            }
        });
        return;
    }

    const topStrokeEl = document.getElementById('trendTopStroke');
    if (topStrokeEl) {
        topStrokeEl.style.stroke = '';
    }
    document.querySelectorAll('.allure-legend:not(.evo-legend) .legend-item').forEach(item => {
        item.classList.remove('is-active', 'is-dimmed');
    });

    if (coords.length === 1) {
        const c = coords[0];
        const halfWidth = 30;
        const leftX = Math.max(50, c.x - halfWidth);
        const rightX = Math.min(800, c.x + halfWidth);

        const pathL1 = `M ${leftX} ${c.y1} L ${rightX} ${c.y1} L ${rightX} 190 L ${leftX} 190 Z`;
        const pathL2 = `M ${leftX} ${c.y2_clean} L ${rightX} ${c.y2_clean} L ${rightX} ${c.y1} L ${leftX} ${c.y1} Z`;
        const pathL2H = `M ${leftX} ${c.y2_healed} L ${rightX} ${c.y2_healed} L ${rightX} ${c.y2_clean} L ${leftX} ${c.y2_clean} Z`;
        const pathL2Ai = `M ${leftX} ${c.y2} L ${rightX} ${c.y2} L ${rightX} ${c.y2_healed} L ${leftX} ${c.y2_healed} Z`;
        const pathL3 = `M ${leftX} ${c.y3_clean} L ${rightX} ${c.y3_clean} L ${rightX} ${c.y2} L ${leftX} ${c.y2} Z`;
        const pathL3H = `M ${leftX} ${c.y3_healed} L ${rightX} ${c.y3_healed} L ${rightX} ${c.y3_clean} L ${leftX} ${c.y3_clean} Z`;
        const pathL3Ai = `M ${leftX} ${c.y3} L ${rightX} ${c.y3} L ${rightX} ${c.y3_healed} L ${leftX} ${c.y3_healed} Z`;
        const pathL4 = `M ${leftX} ${c.y4_clean} L ${rightX} ${c.y4_clean} L ${rightX} ${c.y3} L ${leftX} ${c.y3} Z`;
        const pathL4H = `M ${leftX} ${c.y4_healed} L ${rightX} ${c.y4_healed} L ${rightX} ${c.y4_clean} L ${leftX} ${c.y4_clean} Z`;
        const pathL4Ai = `M ${leftX} ${c.y4} L ${rightX} ${c.y4} L ${rightX} ${c.y4_healed} L ${leftX} ${c.y4_healed} Z`;
        const pathL5 = `M ${leftX} ${c.y5_clean} L ${rightX} ${c.y5_clean} L ${rightX} ${c.y4} L ${leftX} ${c.y4} Z`;
        const pathL5H = `M ${leftX} ${c.y5_healed} L ${rightX} ${c.y5_healed} L ${rightX} ${c.y5_clean} L ${leftX} ${c.y5_clean} Z`;
        const pathL5Ai = `M ${leftX} ${c.y5} L ${rightX} ${c.y5} L ${rightX} ${c.y5_healed} L ${leftX} ${c.y5_healed} Z`;

        document.getElementById('trendLayerIgnored')?.setAttribute('d', pathL1);
        document.getElementById('trendLayerUnknown')?.setAttribute('d', pathL2);
        document.getElementById('trendLayerUnknownHealed')?.setAttribute('d', c.unknownHealed > 0 ? pathL2H : '');
        document.getElementById('trendLayerUnknownHealedPattern')?.setAttribute('d', c.unknownHealed > 0 ? pathL2H : '');
        document.getElementById('trendLayerUnknownAi')?.setAttribute('d', c.unknownAi > 0 ? pathL2Ai : '');
        document.getElementById('trendLayerUnknownAiPattern')?.setAttribute('d', c.unknownAi > 0 ? pathL2Ai : '');
        document.getElementById('trendLayerKnown')?.setAttribute('d', pathL3);
        document.getElementById('trendLayerKnownHealed')?.setAttribute('d', c.knownHealed > 0 ? pathL3H : '');
        document.getElementById('trendLayerKnownHealedPattern')?.setAttribute('d', c.knownHealed > 0 ? pathL3H : '');
        document.getElementById('trendLayerKnownAi')?.setAttribute('d', c.knownAi > 0 ? pathL3Ai : '');
        document.getElementById('trendLayerKnownAiPattern')?.setAttribute('d', c.knownAi > 0 ? pathL3Ai : '');
        document.getElementById('trendLayerFixed')?.setAttribute('d', pathL4);
        document.getElementById('trendLayerFixedHealed')?.setAttribute('d', c.fixedHealed > 0 ? pathL4H : '');
        document.getElementById('trendLayerFixedHealedPattern')?.setAttribute('d', c.fixedHealed > 0 ? pathL4H : '');
        document.getElementById('trendLayerFixedAi')?.setAttribute('d', c.fixedAi > 0 ? pathL4Ai : '');
        document.getElementById('trendLayerFixedAiPattern')?.setAttribute('d', c.fixedAi > 0 ? pathL4Ai : '');
        document.getElementById('trendLayerPass')?.setAttribute('d', pathL5);
        document.getElementById('trendLayerPassHealed')?.setAttribute('d', c.passHealed > 0 ? pathL5H : '');
        document.getElementById('trendLayerPassHealedPattern')?.setAttribute('d', c.passHealed > 0 ? pathL5H : '');
        document.getElementById('trendLayerPassAi')?.setAttribute('d', c.passAi > 0 ? pathL5Ai : '');
        document.getElementById('trendLayerPassAiPattern')?.setAttribute('d', c.passAi > 0 ? pathL5Ai : '');
        document.getElementById('trendTopStroke')?.setAttribute('d', `M ${leftX} ${c.y5} L ${rightX} ${c.y5}`);

        const dotsGroup = document.getElementById('trendSvgDots');
        if (dotsGroup) {
            dotsGroup.innerHTML = `
                <g class="trend-click-col" id="trendPoint-${c.id}" 
                   hx-get="/run-report?runId=${encodeURIComponent(c.id)}" hx-target="#mainViewContainer" hx-swap="innerHTML" hx-push-url="true" style="cursor: pointer;">
                    <circle cx="${c.x}" cy="${c.y5}" r="6" fill="${c.totalHealed > 0 ? '#34d399' : (c.totalAi > 0 ? '#10b981' : '#2563eb')}" stroke="${c.totalHealed > 0 ? '#fef08a' : (c.totalAi > 0 ? '#6ee7b7' : '#ffffff')}" stroke-width="2" class="trend-node-circle"/>
                </g>
            `;
            if (window.htmx) htmx.process(dotsGroup);
        }
        return;
    }

    function buildBezierTopPath(yProp) {
        let d = `M ${coords[0].x} ${coords[0][yProp]}`;
        for (let i = 0; i < coords.length - 1; i++) {
            const x1 = coords[i].x;
            const y1 = coords[i][yProp];
            const x2 = coords[i + 1].x;
            const y2 = coords[i + 1][yProp];
            const dx = (x2 - x1) / 2;
            d += ` C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`;
        }
        return d;
    }

    function buildBezierReversePath(yProp) {
        let d = `L ${coords[coords.length - 1].x} ${coords[coords.length - 1][yProp]}`;
        for (let i = coords.length - 1; i > 0; i--) {
            const x1 = coords[i].x;
            const y1 = coords[i][yProp];
            const x2 = coords[i - 1].x;
            const y2 = coords[i - 1][yProp];
            const dx = (x1 - x2) / 2;
            d += ` C ${x1 - dx} ${y1}, ${x2 + dx} ${y2}, ${x2} ${y2}`;
        }
        return d;
    }

    const lastX = coords[coords.length - 1].x;
    const firstX = coords[0].x;

    const pathL1 = `${buildBezierTopPath('y1')} L ${lastX} 190 L ${firstX} 190 Z`;
    const pathL2 = `${buildBezierTopPath('y2_clean')} ${buildBezierReversePath('y1')} Z`;
    const pathL2H = `${buildBezierTopPath('y2_healed')} ${buildBezierReversePath('y2_clean')} Z`;
    const pathL2Ai = `${buildBezierTopPath('y2')} ${buildBezierReversePath('y2_healed')} Z`;
    const pathL3 = `${buildBezierTopPath('y3_clean')} ${buildBezierReversePath('y2')} Z`;
    const pathL3H = `${buildBezierTopPath('y3_healed')} ${buildBezierReversePath('y3_clean')} Z`;
    const pathL3Ai = `${buildBezierTopPath('y3')} ${buildBezierReversePath('y3_healed')} Z`;
    const pathL4 = `${buildBezierTopPath('y4_clean')} ${buildBezierReversePath('y3')} Z`;
    const pathL4H = `${buildBezierTopPath('y4_healed')} ${buildBezierReversePath('y4_clean')} Z`;
    const pathL4Ai = `${buildBezierTopPath('y4')} ${buildBezierReversePath('y4_healed')} Z`;
    const pathL5 = `${buildBezierTopPath('y5_clean')} ${buildBezierReversePath('y4')} Z`;
    const pathL5H = `${buildBezierTopPath('y5_healed')} ${buildBezierReversePath('y5_clean')} Z`;
    const pathL5Ai = `${buildBezierTopPath('y5')} ${buildBezierReversePath('y5_healed')} Z`;

    const hasUnknownHealed = coords.some(c => c.unknownHealed > 0);
    const hasUnknownAi = coords.some(c => c.unknownAi > 0);
    const hasKnownHealed = coords.some(c => c.knownHealed > 0);
    const hasKnownAi = coords.some(c => c.knownAi > 0);
    const hasFixedHealed = coords.some(c => c.fixedHealed > 0);
    const hasFixedAi = coords.some(c => c.fixedAi > 0);
    const hasPassHealed = coords.some(c => c.passHealed > 0);
    const hasPassAi = coords.some(c => c.passAi > 0);

    document.getElementById('trendLayerIgnored')?.setAttribute('d', pathL1);
    document.getElementById('trendLayerUnknown')?.setAttribute('d', pathL2);
    document.getElementById('trendLayerUnknownHealed')?.setAttribute('d', hasUnknownHealed ? pathL2H : '');
    document.getElementById('trendLayerUnknownHealedPattern')?.setAttribute('d', hasUnknownHealed ? pathL2H : '');
    document.getElementById('trendLayerUnknownAi')?.setAttribute('d', hasUnknownAi ? pathL2Ai : '');
    document.getElementById('trendLayerUnknownAiPattern')?.setAttribute('d', hasUnknownAi ? pathL2Ai : '');
    document.getElementById('trendLayerKnown')?.setAttribute('d', pathL3);
    document.getElementById('trendLayerKnownHealed')?.setAttribute('d', hasKnownHealed ? pathL3H : '');
    document.getElementById('trendLayerKnownHealedPattern')?.setAttribute('d', hasKnownHealed ? pathL3H : '');
    document.getElementById('trendLayerKnownAi')?.setAttribute('d', hasKnownAi ? pathL3Ai : '');
    document.getElementById('trendLayerKnownAiPattern')?.setAttribute('d', hasKnownAi ? pathL3Ai : '');
    document.getElementById('trendLayerFixed')?.setAttribute('d', pathL4);
    document.getElementById('trendLayerFixedHealed')?.setAttribute('d', hasFixedHealed ? pathL4H : '');
    document.getElementById('trendLayerFixedHealedPattern')?.setAttribute('d', hasFixedHealed ? pathL4H : '');
    document.getElementById('trendLayerFixedAi')?.setAttribute('d', hasFixedAi ? pathL4Ai : '');
    document.getElementById('trendLayerFixedAiPattern')?.setAttribute('d', hasFixedAi ? pathL4Ai : '');
    document.getElementById('trendLayerPass')?.setAttribute('d', pathL5);
    document.getElementById('trendLayerPassHealed')?.setAttribute('d', hasPassHealed ? pathL5H : '');
    document.getElementById('trendLayerPassHealedPattern')?.setAttribute('d', hasPassHealed ? pathL5H : '');
    document.getElementById('trendLayerPassAi')?.setAttribute('d', hasPassAi ? pathL5Ai : '');
    document.getElementById('trendLayerPassAiPattern')?.setAttribute('d', hasPassAi ? pathL5Ai : '');
    document.getElementById('trendTopStroke')?.setAttribute('d', buildBezierTopPath('y5'));

    const dotsGroup = document.getElementById('trendSvgDots');
    if (dotsGroup) {
        dotsGroup.innerHTML = coords.map(c => `
            <g class="trend-click-col" id="trendPoint-${c.id}" 
               hx-get="/run-report?runId=${encodeURIComponent(c.id)}" hx-target="#mainViewContainer" hx-swap="innerHTML" hx-push-url="true" style="cursor: pointer;">
                <circle cx="${c.x}" cy="${c.y5}" r="6" fill="${c.totalHealed > 0 ? '#34d399' : (c.totalAi > 0 ? '#10b981' : '#2563eb')}" stroke="${c.totalHealed > 0 ? '#fef08a' : (c.totalAi > 0 ? '#6ee7b7' : '#ffffff')}" stroke-width="2" class="trend-node-circle"/>
            </g>
        `).join('');
        if (window.htmx) htmx.process(dotsGroup);
    }
}

function renderDynamicAreaTrendCharts() {
    const areaSvgs = document.querySelectorAll('.area-trend-svg');
    if (areaSvgs.length === 0) return;

    areaSvgs.forEach(svg => {
        const rawJson = svg.getAttribute('data-run-points');
        if (!rawJson) return;

        let points = [];
        try {
            points = JSON.parse(rawJson);
        } catch (e) {
            console.error("Failed to parse area run points", e);
            return;
        }

        const historyRows = Array.from(document.querySelectorAll('#batchRunsHistoryTableBody tr.run-history-row'));
        if (historyRows.length > 0) {
            const visibleRunIds = new Set(historyRows.filter(r => r.style.display !== 'none').map(r => r.getAttribute('data-run-id')));
            points = points.filter(p => visibleRunIds.has(p.runId));
        }

        if (!points || points.length === 0) {
            svg.querySelectorAll('path').forEach(p => p.setAttribute('d', ''));
            return;
        }

        const maxTests = Math.max(1, ...points.map(p => p.totalCount || (p.passCount + p.fixedCount + p.knownCount + p.unknownCount + p.ignoredCount)));
        const scale = 60 / maxTests;
        const count = points.length;

        const coords = points.map((p, idx) => {
            const x = count === 1 ? 150 : 20 + (idx / (count - 1)) * 260;
            const unknownCount = p.unknownCount || 0;
            const unknownHealed = p.unknownHealedCount || 0;
            const unknownAi = p.unknownAiCount || 0;
            const cleanUnknown = Math.max(0, unknownCount - unknownHealed - unknownAi);

            const knownCount = p.knownCount || 0;
            const knownHealed = p.knownHealedCount || 0;
            const knownAi = p.knownAiCount || 0;
            const cleanKnown = Math.max(0, knownCount - knownHealed - knownAi);

            const fixedCount = p.fixedCount || 0;
            const fixedHealed = p.fixedHealedCount || 0;
            const fixedAi = p.fixedAiCount || 0;
            const cleanFixed = Math.max(0, fixedCount - fixedHealed - fixedAi);

            const passCount = p.passCount || 0;
            const passHealed = p.passHealedCount || 0;
            const passAi = p.passAiCount || 0;
            const cleanPass = Math.max(0, passCount - passHealed - passAi);

            const y0 = 80;
            const y1 = y0 - ((p.ignoredCount || 0) * scale);
            const y2_clean = y1 - (cleanUnknown * scale);
            const y2_healed = y2_clean - (unknownHealed * scale);
            const y2 = y2_healed - (unknownAi * scale);
            const y3_clean = y2 - (cleanKnown * scale);
            const y3_healed = y3_clean - (knownHealed * scale);
            const y3 = y3_healed - (knownAi * scale);
            const y4_clean = y3 - (cleanFixed * scale);
            const y4_healed = y4_clean - (fixedHealed * scale);
            const y4 = y4_healed - (fixedAi * scale);
            const y5_clean = y4 - (cleanPass * scale);
            const y5_healed = y5_clean - (passHealed * scale);
            const y5 = y5_healed - (passAi * scale);

            return {
                x,
                y0,
                y1,
                y2_clean,
                y2_healed,
                y2,
                y3_clean,
                y3_healed,
                y3,
                y4_clean,
                y4_healed,
                y4,
                y5_clean,
                y5_healed,
                y5,
                unknownHealed,
                unknownAi,
                knownHealed,
                knownAi,
                fixedHealed,
                fixedAi,
                passHealed,
                passAi
            };
        });

        function buildBezierTopPath(yProp) {
            let d = `M ${coords[0].x} ${coords[0][yProp]}`;
            for (let i = 0; i < coords.length - 1; i++) {
                const x1 = coords[i].x;
                const y1 = coords[i][yProp];
                const x2 = coords[i + 1].x;
                const y2 = coords[i + 1][yProp];
                const dx = (x2 - x1) / 2;
                d += ` C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`;
            }
            return d;
        }

        function buildBezierReversePath(yProp) {
            let d = `L ${coords[coords.length - 1].x} ${coords[coords.length - 1][yProp]}`;
            for (let i = coords.length - 1; i > 0; i--) {
                const x1 = coords[i].x;
                const y1 = coords[i][yProp];
                const x2 = coords[i - 1].x;
                const y2 = coords[i - 1][yProp];
                const dx = (x1 - x2) / 2;
                d += ` C ${x1 - dx} ${y1}, ${x2 + dx} ${y2}, ${x2} ${y2}`;
            }
            return d;
        }

        let pathL1, pathL2, pathL2H, pathL2Ai, pathL3, pathL3H, pathL3Ai, pathL4, pathL4H, pathL4Ai, pathL5, pathL5H, pathL5Ai, topStroke;

        if (coords.length === 1) {
            const c = coords[0];
            const halfW = 20;
            const leftX = Math.max(20, c.x - halfW);
            const rightX = Math.min(280, c.x + halfW);

            pathL1 = `M ${leftX} ${c.y1} L ${rightX} ${c.y1} L ${rightX} 80 L ${leftX} 80 Z`;
            pathL2 = `M ${leftX} ${c.y2_clean} L ${rightX} ${c.y2_clean} L ${rightX} ${c.y1} L ${leftX} ${c.y1} Z`;
            pathL2H = `M ${leftX} ${c.y2_healed} L ${rightX} ${c.y2_healed} L ${rightX} ${c.y2_clean} L ${leftX} ${c.y2_clean} Z`;
            pathL2Ai = `M ${leftX} ${c.y2} L ${rightX} ${c.y2} L ${rightX} ${c.y2_healed} L ${leftX} ${c.y2_healed} Z`;
            pathL3 = `M ${leftX} ${c.y3_clean} L ${rightX} ${c.y3_clean} L ${rightX} ${c.y2} L ${leftX} ${c.y2} Z`;
            pathL3H = `M ${leftX} ${c.y3_healed} L ${rightX} ${c.y3_healed} L ${rightX} ${c.y3_clean} L ${leftX} ${c.y3_clean} Z`;
            pathL3Ai = `M ${leftX} ${c.y3} L ${rightX} ${c.y3} L ${rightX} ${c.y3_healed} L ${leftX} ${c.y3_healed} Z`;
            pathL4 = `M ${leftX} ${c.y4_clean} L ${rightX} ${c.y4_clean} L ${rightX} ${c.y3} L ${leftX} ${c.y3} Z`;
            pathL4H = `M ${leftX} ${c.y4_healed} L ${rightX} ${c.y4_healed} L ${rightX} ${c.y4_clean} L ${leftX} ${c.y4_clean} Z`;
            pathL4Ai = `M ${leftX} ${c.y4} L ${rightX} ${c.y4} L ${rightX} ${c.y4_healed} L ${leftX} ${c.y4_healed} Z`;
            pathL5 = `M ${leftX} ${c.y5_clean} L ${rightX} ${c.y5_clean} L ${rightX} ${c.y4} L ${leftX} ${c.y4} Z`;
            pathL5H = `M ${leftX} ${c.y5_healed} L ${rightX} ${c.y5_healed} L ${rightX} ${c.y5_clean} L ${leftX} ${c.y5_clean} Z`;
            pathL5Ai = `M ${leftX} ${c.y5} L ${rightX} ${c.y5} L ${rightX} ${c.y5_healed} L ${leftX} ${c.y5_healed} Z`;
            topStroke = `M ${leftX} ${c.y5} L ${rightX} ${c.y5}`;
        } else {
            const lastX = coords[coords.length - 1].x;
            const firstX = coords[0].x;

            pathL1 = `${buildBezierTopPath('y1')} L ${lastX} 80 L ${firstX} 80 Z`;
            pathL2 = `${buildBezierTopPath('y2_clean')} ${buildBezierReversePath('y1')} Z`;
            pathL2H = `${buildBezierTopPath('y2_healed')} ${buildBezierReversePath('y2_clean')} Z`;
            pathL2Ai = `${buildBezierTopPath('y2')} ${buildBezierReversePath('y2_healed')} Z`;
            pathL3 = `${buildBezierTopPath('y3_clean')} ${buildBezierReversePath('y2')} Z`;
            pathL3H = `${buildBezierTopPath('y3_healed')} ${buildBezierReversePath('y3_clean')} Z`;
            pathL3Ai = `${buildBezierTopPath('y3')} ${buildBezierReversePath('y3_healed')} Z`;
            pathL4 = `${buildBezierTopPath('y4_clean')} ${buildBezierReversePath('y3')} Z`;
            pathL4H = `${buildBezierTopPath('y4_healed')} ${buildBezierReversePath('y4_clean')} Z`;
            pathL4Ai = `${buildBezierTopPath('y4')} ${buildBezierReversePath('y4_healed')} Z`;
            pathL5 = `${buildBezierTopPath('y5_clean')} ${buildBezierReversePath('y4')} Z`;
            pathL5H = `${buildBezierTopPath('y5_healed')} ${buildBezierReversePath('y5_clean')} Z`;
            pathL5Ai = `${buildBezierTopPath('y5')} ${buildBezierReversePath('y5_healed')} Z`;
            topStroke = buildBezierTopPath('y5');
        }

        const hasUnknownHealed = coords.some(c => c.unknownHealed > 0);
        const hasUnknownAi = coords.some(c => c.unknownAi > 0);
        const hasKnownHealed = coords.some(c => c.knownHealed > 0);
        const hasKnownAi = coords.some(c => c.knownAi > 0);
        const hasFixedHealed = coords.some(c => c.fixedHealed > 0);
        const hasFixedAi = coords.some(c => c.fixedAi > 0);
        const hasPassHealed = coords.some(c => c.passHealed > 0);
        const hasPassAi = coords.some(c => c.passAi > 0);

        svg.querySelector('.area-layer-ignored')?.setAttribute('d', pathL1);
        svg.querySelector('.area-layer-unknown')?.setAttribute('d', pathL2);
        svg.querySelector('.area-layer-unknown-healed')?.setAttribute('d', hasUnknownHealed ? pathL2H : '');
        svg.querySelector('.area-layer-unknown-healed-pat')?.setAttribute('d', hasUnknownHealed ? pathL2H : '');
        svg.querySelector('.area-layer-unknown-ai')?.setAttribute('d', hasUnknownAi ? pathL2Ai : '');
        svg.querySelector('.area-layer-unknown-ai-pat')?.setAttribute('d', hasUnknownAi ? pathL2Ai : '');
        svg.querySelector('.area-layer-known')?.setAttribute('d', pathL3);
        svg.querySelector('.area-layer-known-healed')?.setAttribute('d', hasKnownHealed ? pathL3H : '');
        svg.querySelector('.area-layer-known-healed-pat')?.setAttribute('d', hasKnownHealed ? pathL3H : '');
        svg.querySelector('.area-layer-known-ai')?.setAttribute('d', hasKnownAi ? pathL3Ai : '');
        svg.querySelector('.area-layer-known-ai-pat')?.setAttribute('d', hasKnownAi ? pathL3Ai : '');
        svg.querySelector('.area-layer-fixed')?.setAttribute('d', pathL4);
        svg.querySelector('.area-layer-fixed-healed')?.setAttribute('d', hasFixedHealed ? pathL4H : '');
        svg.querySelector('.area-layer-fixed-healed-pat')?.setAttribute('d', hasFixedHealed ? pathL4H : '');
        svg.querySelector('.area-layer-fixed-ai')?.setAttribute('d', hasFixedAi ? pathL4Ai : '');
        svg.querySelector('.area-layer-fixed-ai-pat')?.setAttribute('d', hasFixedAi ? pathL4Ai : '');
        svg.querySelector('.area-layer-pass')?.setAttribute('d', pathL5);
        svg.querySelector('.area-layer-pass-healed')?.setAttribute('d', hasPassHealed ? pathL5H : '');
        svg.querySelector('.area-layer-pass-healed-pat')?.setAttribute('d', hasPassHealed ? pathL5H : '');
        svg.querySelector('.area-layer-pass-ai')?.setAttribute('d', hasPassAi ? pathL5Ai : '');
        svg.querySelector('.area-layer-pass-ai-pat')?.setAttribute('d', hasPassAi ? pathL5Ai : '');
        svg.querySelector('.area-layer-top-stroke')?.setAttribute('d', topStroke);
    });
}

function setBatchHistoryAsOfRun(runId) {
    const urlParams = new URLSearchParams(window.location.search);
    const container = document.querySelector('#pageBatchHistoryOverview');
    if (container) {
        const batchName = container.getAttribute('data-batch-name');
        const groupType = container.getAttribute('data-group-type');
        const groupValue = container.getAttribute('data-group-value');
        if (groupType && groupValue) {
            urlParams.set('groupType', groupType);
            urlParams.set('groupValue', groupValue);
        } else if (batchName) {
            urlParams.set('batchName', batchName);
        }
    }
    const currentAsOf = urlParams.get('asOfRunId');
    if (!runId || String(runId) === String(currentAsOf)) {
        urlParams.delete('asOfRunId');
    } else {
        urlParams.set('asOfRunId', runId);
    }
    const targetUrl = `/batch-history?${urlParams.toString()}`;
    if (window.htmx) {
        htmx.ajax('GET', targetUrl, { target: '#mainViewContainer', pushUrl: true });
    } else {
        window.location.href = targetUrl;
    }
}

let selectedSuiteEvolutionKey = null;

function toggleSuiteEvolutionLegendSelect(key) {
    selectedSuiteEvolutionKey = (selectedSuiteEvolutionKey === key) ? null : key;
    renderDynamicSuiteEvolutionChart();
}
window.toggleSuiteEvolutionLegendSelect = toggleSuiteEvolutionLegendSelect;

function renderDynamicSuiteEvolutionChart() {
    const svg = document.getElementById('batchSuiteEvolutionSvg');
    if (!svg) return;

    const rawJson = svg.getAttribute('data-evolution-points');
    if (!rawJson) return;

    let points = [];
    try {
        points = JSON.parse(rawJson);
    } catch (e) {
        console.error("Failed to parse batch suite evolution points", e);
        return;
    }

    if (!points || points.length === 0) {
        svg.querySelectorAll('path').forEach(p => p.setAttribute('d', ''));
        const dots = svg.querySelector('#evoSvgDots');
        if (dots) dots.innerHTML = '';
        const yAxis = svg.querySelector('#evoSvgYAxis');
        if (yAxis) yAxis.innerHTML = '';
        const xAxis = svg.querySelector('#evoSvgXAxis');
        if (xAxis) xAxis.innerHTML = '';
        return;
    }

    const maxTests = Math.max(1, ...points.map(p => p.totalTestsKnown || 1));
    const y0 = 150;
    const yTop = 20;
    const chartHeight = y0 - yTop; // 130px
    const scale = chartHeight / maxTests;
    const count = points.length;
    const xMin = 70;
    const xMax = 785;
    const chartWidth = xMax - xMin; // 730px

    const coords = points.map((p, idx) => {
        const x = count === 1 ? 420 : xMin + (idx / (count - 1)) * chartWidth;

        const ignoredCount = p.ignoredCount || 0;
        const unknownClean = p.unknownCleanCount || 0;
        const unknownHealed = p.unknownHealedCount || 0;
        const unknownAi = p.unknownAiCount || 0;
        const knownClean = p.knownCleanCount || 0;
        const knownHealed = p.knownHealedCount || 0;
        const knownAi = p.knownAiCount || 0;
        const fixedClean = p.fixedCleanCount || 0;
        const fixedHealed = p.fixedHealedCount || 0;
        const fixedAi = p.fixedAiCount || 0;
        const passClean = p.passCleanCount || 0;
        const passHealed = p.passHealedCount || 0;
        const passAi = p.passAiCount || 0;

        const y1 = y0 - (ignoredCount * scale);
        const y2_clean = y1 - (unknownClean * scale);
        const y2_healed = y2_clean - (unknownHealed * scale);
        const y2 = y2_healed - (unknownAi * scale);
        const y3_clean = y2 - (knownClean * scale);
        const y3_healed = y3_clean - (knownHealed * scale);
        const y3 = y3_healed - (knownAi * scale);
        const y4_clean = y3 - (fixedClean * scale);
        const y4_healed = y4_clean - (fixedHealed * scale);
        const y4 = y4_healed - (fixedAi * scale);
        const y5_clean = y4 - (passClean * scale);
        const y5_healed = y5_clean - (passHealed * scale);
        const y5 = y5_healed - (passAi * scale);

        return {
            x,
            y0,
            y1,
            y2_clean,
            y2_healed,
            y2,
            y3_clean,
            y3_healed,
            y3,
            y4_clean,
            y4_healed,
            y4,
            y5_clean,
            y5_healed,
            y5,
            unknownHealed,
            unknownAi,
            knownHealed,
            knownAi,
            fixedHealed,
            fixedAi,
            passHealed,
            passAi,
            point: p
        };
    });

    const activeRunId = svg.getAttribute('data-active-run-id');
    const activeIdx = activeRunId ? coords.findIndex(c => String(c.point.runId) === String(activeRunId)) : -1;

    // Dynamic Y-Axis Grid Lines & Labels
    const yAxisGroup = svg.querySelector('#evoSvgYAxis');
    if (yAxisGroup) {
        yAxisGroup.innerHTML = '';

        function createGridLine(yVal) {
            const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
            line.setAttribute('x1', xMin);
            line.setAttribute('y1', yVal);
            line.setAttribute('x2', xMax);
            line.setAttribute('y2', yVal);
            line.setAttribute('stroke', 'var(--border)');
            line.setAttribute('stroke-width', '1');
            line.setAttribute('stroke-dasharray', '3 3');
            line.setAttribute('opacity', '0.6');
            return line;
        }

        function createAxisText(xVal, yVal, text) {
            const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            label.setAttribute('x', xVal);
            label.setAttribute('y', yVal);
            label.setAttribute('fill', 'var(--text-muted)');
            label.setAttribute('font-size', '10');
            label.setAttribute('font-family', 'JetBrains Mono, monospace');
            label.setAttribute('text-anchor', 'end');
            label.textContent = text;
            return label;
        }

        // Baseline (0 Tests)
        const baseLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        baseLine.setAttribute('x1', xMin - 5);
        baseLine.setAttribute('y1', y0);
        baseLine.setAttribute('x2', xMax);
        baseLine.setAttribute('y2', y0);
        baseLine.setAttribute('stroke', 'var(--border)');
        baseLine.setAttribute('stroke-width', '1.2');
        yAxisGroup.appendChild(baseLine);
        yAxisGroup.appendChild(createAxisText(xMin - 8, y0 + 3, '0 Tests'));

        // Vertical axis line
        const vLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        vLine.setAttribute('x1', xMin);
        vLine.setAttribute('y1', yTop - 5);
        vLine.setAttribute('x2', xMin);
        vLine.setAttribute('y2', y0);
        vLine.setAttribute('stroke', 'var(--border)');
        vLine.setAttribute('stroke-width', '1.2');
        yAxisGroup.appendChild(vLine);

        // Top grid line (maxTests)
        const topY = y0 - (maxTests * scale);
        yAxisGroup.appendChild(createGridLine(topY));
        yAxisGroup.appendChild(createAxisText(xMin - 8, topY + 3, `${maxTests} Tests`));

        // Mid grid line if maxTests > 1
        if (maxTests > 1) {
            const midVal = Math.round(maxTests / 2);
            if (midVal > 0 && midVal < maxTests) {
                const midY = y0 - (midVal * scale);
                yAxisGroup.appendChild(createGridLine(midY));
                yAxisGroup.appendChild(createAxisText(xMin - 8, midY + 3, `${midVal} Tests`));
            }
        }
    }

    // Dynamic X-Axis Timeline Labels & Ticks
    const xAxisGroup = svg.querySelector('#evoSvgXAxis');
    if (xAxisGroup) {
        xAxisGroup.innerHTML = '';
        const isDense = coords.length > 20;

        // Collect milestone candidates for X-axis labels
        const candidates = [];
        coords.forEach((c, idx) => {
            const p = c.point;
            const isHistoricalActive = (idx === activeIdx);
            const isLast = (idx === coords.length - 1);
            const isFirst = (idx === 0);
            const isGrowth = (p.newTestsCount > 0);
            if ((!isDense) || isGrowth || isFirst || isLast || isHistoricalActive) {
                candidates.push({ coord: c, idx, isGrowth, isFirst, isLast, isHistoricalActive });
            }
        });

        // Filter candidates to avoid horizontal label overlap (minimum spacing 48px)
        const labelsToRender = [];
        candidates.forEach((cand) => {
            if (labelsToRender.length === 0) {
                labelsToRender.push(cand);
                return;
            }
            const lastRendered = labelsToRender[labelsToRender.length - 1];
            const dist = cand.coord.x - lastRendered.coord.x;

            if (cand.isLast) {
                if (dist < 45 && labelsToRender.length > 1) {
                    labelsToRender.pop();
                }
                labelsToRender.push(cand);
            } else if (dist >= 45 || cand.isGrowth || cand.isHistoricalActive) {
                if (dist < 45) {
                    if (!lastRendered.isGrowth && !lastRendered.isHistoricalActive && !lastRendered.isFirst) {
                        labelsToRender.pop();
                        labelsToRender.push(cand);
                    }
                } else {
                    labelsToRender.push(cand);
                }
            }
        });

        // Fill wide gaps (> 160px) with intermediate ticks
        const finalLabels = [];
        for (let i = 0; i < labelsToRender.length; i++) {
            finalLabels.push(labelsToRender[i]);
            if (i < labelsToRender.length - 1) {
                const cur = labelsToRender[i];
                const next = labelsToRender[i + 1];
                const gap = next.coord.x - cur.coord.x;
                if (gap > 160) {
                    const midIdx = Math.round((cur.idx + next.idx) / 2);
                    if (coords[midIdx] && midIdx !== cur.idx && midIdx !== next.idx) {
                        finalLabels.push({
                            coord: coords[midIdx],
                            idx: midIdx,
                            isGrowth: false,
                            isFirst: false,
                            isLast: false,
                            isIntermediate: true
                        });
                    }
                }
            }
        }

        // Render ticks and text labels
        finalLabels.forEach((item) => {
            const c = item.coord;
            const p = c.point;

            const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            g.setAttribute('class', 'evo-axis-label-group');
            g.setAttribute('style', 'cursor: pointer;');
            g.onclick = function(e) {
                e.stopPropagation();
                setBatchHistoryAsOfRun(p.runId);
            };

            // Tick mark
            const tick = document.createElementNS('http://www.w3.org/2000/svg', 'line');
            tick.setAttribute('x1', c.x);
            tick.setAttribute('y1', y0);
            tick.setAttribute('x2', c.x);
            tick.setAttribute('y2', y0 + (item.isGrowth ? 7 : 5));
            tick.setAttribute('stroke', item.isGrowth ? 'var(--status-pass)' : 'var(--border)');
            tick.setAttribute('stroke-width', item.isGrowth ? '1.5' : '1');
            g.appendChild(tick);

            // Run ID text
            const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
            text.setAttribute('x', c.x);
            text.setAttribute('y', y0 + 16);
            text.setAttribute('class', 'evo-axis-text');
            text.setAttribute('text-anchor', 'middle');
            text.textContent = `#${p.runId}`;
            g.appendChild(text);

            // Growth sub-badge or Latest tag
            if (item.isGrowth && p.newTestsCount > 0) {
                const badge = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                badge.setAttribute('x', c.x);
                badge.setAttribute('y', y0 + 26);
                badge.setAttribute('class', 'evo-axis-growth-badge');
                badge.setAttribute('text-anchor', 'middle');
                badge.textContent = `+${p.newTestsCount}`;
                g.appendChild(badge);
            } else if (item.isLast) {
                const latestText = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                latestText.setAttribute('x', c.x);
                latestText.setAttribute('y', y0 + 26);
                latestText.setAttribute('fill', 'var(--text-muted)');
                latestText.setAttribute('font-size', '8.5');
                latestText.setAttribute('font-family', 'JetBrains Mono, monospace');
                latestText.setAttribute('text-anchor', 'middle');
                latestText.textContent = 'Latest';
                g.appendChild(latestText);
            }

            xAxisGroup.appendChild(g);
        });
    }

    function buildBezierTopPath(yProp) {
        let d = `M ${coords[0].x} ${coords[0][yProp]}`;
        for (let i = 0; i < coords.length - 1; i++) {
            const x1 = coords[i].x;
            const y1 = coords[i][yProp];
            const x2 = coords[i + 1].x;
            const y2 = coords[i + 1][yProp];
            const dx = (x2 - x1) / 2;
            d += ` C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`;
        }
        return d;
    }

    function buildBezierReversePath(yProp) {
        let d = `L ${coords[coords.length - 1].x} ${coords[coords.length - 1][yProp]}`;
        for (let i = coords.length - 1; i > 0; i--) {
            const x1 = coords[i].x;
            const y1 = coords[i][yProp];
            const x2 = coords[i - 1].x;
            const y2 = coords[i - 1][yProp];
            const dx = (x1 - x2) / 2;
            d += ` C ${x1 - dx} ${y1}, ${x2 + dx} ${y2}, ${x2} ${y2}`;
        }
        return d;
    }

    const evoMetricMeta = {
        passClean: { label: 'Passed Clean', color: 'var(--status-pass, #10b981)', getVal: p => p.passCleanCount || 0 },
        passHealed: { label: 'Passed Healed', color: 'var(--status-pass-healed, #34d399)', getVal: p => p.passHealedCount || 0 },
        passAi: { label: 'Passed AI', color: 'var(--status-pass, #10b981)', getVal: p => p.passAiCount || 0 },
        fixed: { label: 'Succeeded Fixed', color: 'var(--status-fixed, #3b82f6)', getVal: p => p.fixedCount || ((p.fixedCleanCount || 0) + (p.fixedHealedCount || 0) + (p.fixedAiCount || 0)) },
        known: { label: 'Failed Known', color: 'var(--status-known-fail, #f97316)', getVal: p => p.knownCount || ((p.knownCleanCount || 0) + (p.knownHealedCount || 0) + (p.knownAiCount || 0)) },
        unknown: { label: 'Failed Unknown', color: 'var(--status-unknown-fail, #ef4444)', getVal: p => p.unknownCount || ((p.unknownCleanCount || 0) + (p.unknownHealedCount || 0) + (p.unknownAiCount || 0)) },
        ignored: { label: 'Ignored', color: 'var(--status-ignored, #9ca3af)', getVal: p => p.ignoredCount || 0 }
    };

    const isIsolated = Boolean(selectedSuiteEvolutionKey && evoMetricMeta[selectedSuiteEvolutionKey]);
    const metricYs = isIsolated ? coords.map(c => y0 - (evoMetricMeta[selectedSuiteEvolutionKey].getVal(c.point) * scale)) : null;

    if (isIsolated) {
        [
            'evoLayerIgnored',
            'evoLayerUnknownClean', 'evoLayerUnknownHealed', 'evoLayerUnknownHealedPat', 'evoLayerUnknownAi', 'evoLayerUnknownAiPat',
            'evoLayerKnownClean', 'evoLayerKnownHealed', 'evoLayerKnownHealedPat', 'evoLayerKnownAi', 'evoLayerKnownAiPat',
            'evoLayerFixedClean', 'evoLayerFixedHealed', 'evoLayerFixedHealedPat', 'evoLayerFixedAi', 'evoLayerFixedAiPat',
            'evoLayerPassClean', 'evoLayerPassHealed', 'evoLayerPassHealedPat', 'evoLayerPassAi', 'evoLayerPassAiPat'
        ].forEach(id => {
            svg.querySelector('#' + id)?.setAttribute('d', '');
        });

        let evoLinePathStr = '';
        if (coords.length === 1) {
            const halfW = 40;
            const leftX = Math.max(xMin, coords[0].x - halfW);
            const rightX = Math.min(xMax, coords[0].x + halfW);
            evoLinePathStr = `M ${leftX} ${metricYs[0]} L ${rightX} ${metricYs[0]}`;
        } else {
            evoLinePathStr = `M ${coords[0].x} ${metricYs[0]}`;
            for (let i = 0; i < coords.length - 1; i++) {
                const x1 = coords[i].x;
                const y1 = metricYs[i];
                const x2 = coords[i + 1].x;
                const y2 = metricYs[i + 1];
                const dx = (x2 - x1) / 2;
                evoLinePathStr += ` C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`;
            }
        }
        const evoTopStrokeEl = svg.querySelector('#evoTopStroke');
        if (evoTopStrokeEl) {
            evoTopStrokeEl.setAttribute('d', evoLinePathStr);
            evoTopStrokeEl.style.stroke = evoMetricMeta[selectedSuiteEvolutionKey].color;
        }
    } else {
        const evoTopStrokeEl = svg.querySelector('#evoTopStroke');
        if (evoTopStrokeEl) {
            evoTopStrokeEl.style.stroke = '';
        }

        let pathL1, pathL2, pathL2H, pathL2Ai, pathL3, pathL3H, pathL3Ai, pathL4, pathL4H, pathL4Ai, pathL5, pathL5H, pathL5Ai, topStroke;

        if (coords.length === 1) {
            const c = coords[0];
            const halfW = 40;
            const leftX = Math.max(xMin, c.x - halfW);
            const rightX = Math.min(xMax, c.x + halfW);

            pathL1 = `M ${leftX} ${c.y1} L ${rightX} ${c.y1} L ${rightX} ${y0} L ${leftX} ${y0} Z`;
            pathL2 = `M ${leftX} ${c.y2_clean} L ${rightX} ${c.y2_clean} L ${rightX} ${c.y1} L ${leftX} ${c.y1} Z`;
            pathL2H = `M ${leftX} ${c.y2_healed} L ${rightX} ${c.y2_healed} L ${rightX} ${c.y2_clean} L ${leftX} ${c.y2_clean} Z`;
            pathL2Ai = `M ${leftX} ${c.y2} L ${rightX} ${c.y2} L ${rightX} ${c.y2_healed} L ${leftX} ${c.y2_healed} Z`;
            pathL3 = `M ${leftX} ${c.y3_clean} L ${rightX} ${c.y3_clean} L ${rightX} ${c.y2} L ${leftX} ${c.y2} Z`;
            pathL3H = `M ${leftX} ${c.y3_healed} L ${rightX} ${c.y3_healed} L ${rightX} ${c.y3_clean} L ${leftX} ${c.y3_clean} Z`;
            pathL3Ai = `M ${leftX} ${c.y3} L ${rightX} ${c.y3} L ${rightX} ${c.y3_healed} L ${leftX} ${c.y3_healed} Z`;
            pathL4 = `M ${leftX} ${c.y4_clean} L ${rightX} ${c.y4_clean} L ${rightX} ${c.y3} L ${leftX} ${c.y3} Z`;
            pathL4H = `M ${leftX} ${c.y4_healed} L ${rightX} ${c.y4_healed} L ${rightX} ${c.y4_clean} L ${leftX} ${c.y4_clean} Z`;
            pathL4Ai = `M ${leftX} ${c.y4_healed} L ${rightX} ${c.y4_healed} L ${rightX} ${c.y4_clean} L ${leftX} ${c.y4_clean} Z`;
            pathL5 = `M ${leftX} ${c.y5_clean} L ${rightX} ${c.y5_clean} L ${rightX} ${c.y4} L ${leftX} ${c.y4} Z`;
            pathL5H = `M ${leftX} ${c.y5_healed} L ${rightX} ${c.y5_healed} L ${rightX} ${c.y5_clean} L ${leftX} ${c.y5_clean} Z`;
            pathL5Ai = `M ${leftX} ${c.y5} L ${rightX} ${c.y5} L ${rightX} ${c.y5_healed} L ${leftX} ${c.y5_healed} Z`;
            topStroke = `M ${leftX} ${c.y5} L ${rightX} ${c.y5}`;
        } else {
            const lastX = coords[coords.length - 1].x;
            const firstX = coords[0].x;

            pathL1 = `${buildBezierTopPath('y1')} L ${lastX} ${y0} L ${firstX} ${y0} Z`;
            pathL2 = `${buildBezierTopPath('y2_clean')} ${buildBezierReversePath('y1')} Z`;
            pathL2H = `${buildBezierTopPath('y2_healed')} ${buildBezierReversePath('y2_clean')} Z`;
            pathL2Ai = `${buildBezierTopPath('y2')} ${buildBezierReversePath('y2_healed')} Z`;
            pathL3 = `${buildBezierTopPath('y3_clean')} ${buildBezierReversePath('y2')} Z`;
            pathL3H = `${buildBezierTopPath('y3_healed')} ${buildBezierReversePath('y3_clean')} Z`;
            pathL3Ai = `${buildBezierTopPath('y3')} ${buildBezierReversePath('y3_healed')} Z`;
            pathL4 = `${buildBezierTopPath('y4_clean')} ${buildBezierReversePath('y3')} Z`;
            pathL4H = `${buildBezierTopPath('y4_healed')} ${buildBezierReversePath('y4_clean')} Z`;
            pathL4Ai = `${buildBezierTopPath('y4')} ${buildBezierReversePath('y4_healed')} Z`;
            pathL5 = `${buildBezierTopPath('y5_clean')} ${buildBezierReversePath('y4')} Z`;
            pathL5H = `${buildBezierTopPath('y5_healed')} ${buildBezierReversePath('y5_clean')} Z`;
            pathL5Ai = `${buildBezierTopPath('y5')} ${buildBezierReversePath('y5_healed')} Z`;
            topStroke = buildBezierTopPath('y5');
        }

        const hasUnknownHealed = coords.some(c => c.unknownHealed > 0);
        const hasUnknownAi = coords.some(c => c.unknownAi > 0);
        const hasKnownHealed = coords.some(c => c.knownHealed > 0);
        const hasKnownAi = coords.some(c => c.knownAi > 0);
        const hasFixedHealed = coords.some(c => c.fixedHealed > 0);
        const hasFixedAi = coords.some(c => c.fixedAi > 0);
        const hasPassHealed = coords.some(c => c.passHealed > 0);
        const hasPassAi = coords.some(c => c.passAi > 0);

        svg.querySelector('#evoLayerIgnored')?.setAttribute('d', pathL1);
        svg.querySelector('#evoLayerUnknownClean')?.setAttribute('d', pathL2);
        svg.querySelector('#evoLayerUnknownHealed')?.setAttribute('d', hasUnknownHealed ? pathL2H : '');
        svg.querySelector('#evoLayerUnknownHealedPat')?.setAttribute('d', hasUnknownHealed ? pathL2H : '');
        svg.querySelector('#evoLayerUnknownAi')?.setAttribute('d', hasUnknownAi ? pathL2Ai : '');
        svg.querySelector('#evoLayerUnknownAiPat')?.setAttribute('d', hasUnknownAi ? pathL2Ai : '');

        svg.querySelector('#evoLayerKnownClean')?.setAttribute('d', pathL3);
        svg.querySelector('#evoLayerKnownHealed')?.setAttribute('d', hasKnownHealed ? pathL3H : '');
        svg.querySelector('#evoLayerKnownHealedPat')?.setAttribute('d', hasKnownHealed ? pathL3H : '');
        svg.querySelector('#evoLayerKnownAi')?.setAttribute('d', hasKnownAi ? pathL3Ai : '');
        svg.querySelector('#evoLayerKnownAiPat')?.setAttribute('d', hasKnownAi ? pathL3Ai : '');

        svg.querySelector('#evoLayerFixedClean')?.setAttribute('d', pathL4);
        svg.querySelector('#evoLayerFixedHealed')?.setAttribute('d', hasFixedHealed ? pathL4H : '');
        svg.querySelector('#evoLayerFixedHealedPat')?.setAttribute('d', hasFixedHealed ? pathL4H : '');
        svg.querySelector('#evoLayerFixedAi')?.setAttribute('d', hasFixedAi ? pathL4Ai : '');
        svg.querySelector('#evoLayerFixedAiPat')?.setAttribute('d', hasFixedAi ? pathL4Ai : '');

        svg.querySelector('#evoLayerPassClean')?.setAttribute('d', pathL5);
        svg.querySelector('#evoLayerPassHealed')?.setAttribute('d', hasPassHealed ? pathL5H : '');
        svg.querySelector('#evoLayerPassHealedPat')?.setAttribute('d', hasPassHealed ? pathL5H : '');
        svg.querySelector('#evoLayerPassAi')?.setAttribute('d', hasPassAi ? pathL5Ai : '');
        svg.querySelector('#evoLayerPassAiPat')?.setAttribute('d', hasPassAi ? pathL5Ai : '');

        svg.querySelector('#evoTopStroke')?.setAttribute('d', topStroke);
    }

    document.querySelectorAll('.allure-legend.evo-legend .legend-item').forEach(item => {
        const k = item.getAttribute('data-evo-key');
        if (selectedSuiteEvolutionKey) {
            if (k === selectedSuiteEvolutionKey) {
                item.classList.add('is-active');
                item.classList.remove('is-dimmed');
            } else {
                item.classList.add('is-dimmed');
                item.classList.remove('is-active');
            }
        } else {
            item.classList.remove('is-active', 'is-dimmed');
        }
    });

    const dotsGroup = svg.querySelector('#evoSvgDots');
    const guide = svg.querySelector('#evoHoverGuide');
    const hoverDot = svg.querySelector('#evoHoverDot');
    const tooltip = document.getElementById('evoHoverTooltip');

    if (dotsGroup) {
        dotsGroup.innerHTML = '';
        const isDense = coords.length > 20;

        coords.forEach((c, idx) => {
            const p = c.point;
            const isHistoricalActive = (idx === activeIdx);
            const isLast = (idx === coords.length - 1);
            const isFirst = (idx === 0);
            const isGrowth = (p.newTestsCount > 0);
            const isMilestone = (!isDense) || isGrowth || isFirst || isLast || isHistoricalActive;

            // In dense histories, only render key evolution milestones, first/last, and active point
            if (!isMilestone) return;

            const r = isHistoricalActive ? 6.5 : (isGrowth ? 5.5 : (isLast ? 5 : 4));
            const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
            g.setAttribute('class', 'evo-point-group');
            g.setAttribute('style', 'cursor: pointer;');
            g.onclick = function(e) {
                e.stopPropagation();
                setBatchHistoryAsOfRun(p.runId);
            };

            const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
            circle.setAttribute('cx', c.x);
            circle.setAttribute('cy', isIsolated ? metricYs[idx] : c.y5);
            circle.setAttribute('r', r);
            let circleClass = 'evo-node-circle';
            if (isHistoricalActive) {
                circleClass += ' evo-node-active';
            } else if (isGrowth) {
                circleClass += ' evo-node-growth';
            }
            circle.setAttribute('class', circleClass);
            circle.setAttribute('data-index', idx);
            circle.setAttribute('data-run-id', p.runId);
            const circleColor = isIsolated
                ? (isHistoricalActive ? '#f59e0b' : evoMetricMeta[selectedSuiteEvolutionKey].color)
                : (isHistoricalActive ? '#f59e0b' : (isGrowth ? '#10b981' : '#2563eb'));
            circle.setAttribute('fill', circleColor);
            circle.setAttribute('stroke', '#ffffff');
            circle.setAttribute('stroke-width', isHistoricalActive ? '2.5' : (isGrowth ? '2' : '1.5'));

            g.appendChild(circle);
            dotsGroup.appendChild(g);
        });
    }

    if (activeIdx >= 0) {
        const activeCoord = coords[activeIdx];
        if (guide) {
            guide.setAttribute('x1', activeCoord.x);
            guide.setAttribute('x2', activeCoord.x);
            guide.style.display = 'block';
        }
    } else {
        if (guide) guide.style.display = 'none';
    }

    function showEvoHover(idx) {
        if (idx < 0 || idx >= coords.length) return;
        const c = coords[idx];
        const p = c.point;

        if (guide) {
            guide.setAttribute('x1', c.x);
            guide.setAttribute('x2', c.x);
            guide.style.display = 'block';
        }

        if (hoverDot) {
            hoverDot.setAttribute('cx', c.x);
            hoverDot.setAttribute('cy', isIsolated ? metricYs[idx] : c.y5);
            const hoverFill = isIsolated
                ? (idx === activeIdx ? '#f59e0b' : evoMetricMeta[selectedSuiteEvolutionKey].color)
                : ((idx === activeIdx) ? '#f59e0b' : ((p.newTestsCount > 0) ? '#10b981' : '#2563eb'));
            hoverDot.setAttribute('fill', hoverFill);
            hoverDot.style.display = 'block';
        }

        svg.querySelectorAll('.evo-node-circle').forEach((dot) => {
            const dotIdx = parseInt(dot.getAttribute('data-index'), 10);
            if (dotIdx === idx || dotIdx === activeIdx) {
                dot.setAttribute('r', '7');
            } else if (dot.classList.contains('evo-node-growth')) {
                dot.setAttribute('r', '5.5');
            } else {
                dot.setAttribute('r', '4');
            }
        });

        if (tooltip) {
            const ttRunId = document.getElementById('evoTtRunId');
            const ttPassRate = document.getElementById('evoTtPassRate');
            const ttTime = document.getElementById('evoTtTime');
            const ttTotal = document.getElementById('evoTtTotal');
            const ttNew = document.getElementById('evoTtNew');
            const ttBreakdown = document.getElementById('evoTtBreakdown');
            const ttRunLink = document.getElementById('evoTtRunLink');

            if (ttRunId) ttRunId.innerText = `#${p.runId}`;
            if (ttPassRate) {
                ttPassRate.innerText = `${p.passRateFormatted || (Math.round(p.passRate) + '%')} PASS`;
                ttPassRate.className = p.passRate >= 95 ? 'badge-status badge-pass' : (p.passRate >= 70 ? 'badge-status badge-fixed' : 'badge-status badge-fail');
            }
            if (ttTime) ttTime.innerText = p.timestampLabel || ('Run ' + (idx + 1));
            if (ttTotal) ttTotal.innerText = `${p.totalTestsKnown} tests`;
            if (ttNew) {
                ttNew.innerText = p.newTestsCount > 0 ? `+${p.newTestsCount}` : '0';
                ttNew.style.color = p.newTestsCount > 0 ? 'var(--status-fixed)' : 'var(--text-muted)';
            }
            if (ttRunLink) {
                const runUrl = `/run-report?runId=${encodeURIComponent(p.runId)}`;
                ttRunLink.setAttribute('href', runUrl);
                ttRunLink.setAttribute('hx-get', runUrl);
                ttRunLink.onclick = function(e) {
                    e.stopPropagation();
                };
            }
            if (ttBreakdown) {
                const passTotal = (p.passCleanCount || 0) + (p.passHealedCount || 0) + (p.passAiCount || 0);
                const fixedTotal = (p.fixedCleanCount || 0) + (p.fixedHealedCount || 0) + (p.fixedAiCount || 0);
                const knownTotal = (p.knownCleanCount || 0) + (p.knownHealedCount || 0) + (p.knownAiCount || 0);
                const unknownTotal = (p.unknownCleanCount || 0) + (p.unknownHealedCount || 0) + (p.unknownAiCount || 0);
                const ignoredTotal = p.ignoredCount || 0;

                ttBreakdown.innerHTML = `
                    <div style="display: flex; justify-content: space-between; color: var(--status-pass);">
                        <span>Passed:</span><strong>${passTotal}</strong>
                    </div>
                    ${fixedTotal > 0 ? `<div style="display: flex; justify-content: space-between; color: var(--status-fixed);"><span>Fixed:</span><strong>${fixedTotal}</strong></div>` : ''}
                    ${knownTotal > 0 ? `<div style="display: flex; justify-content: space-between; color: var(--status-known-fail);"><span>Known Fail:</span><strong>${knownTotal}</strong></div>` : ''}
                    ${unknownTotal > 0 ? `<div style="display: flex; justify-content: space-between; color: var(--status-unknown-fail);"><span>Unknown Fail:</span><strong>${unknownTotal}</strong></div>` : ''}
                    ${ignoredTotal > 0 ? `<div style="display: flex; justify-content: space-between; color: var(--status-ignored);"><span>Ignored:</span><strong>${ignoredTotal}</strong></div>` : ''}
                `;
            }

            const rect = svg.getBoundingClientRect();
            const svgWidth = rect.width || 800;
            const ttWidth = 200;
            let left = (c.x / 800) * svgWidth + 10;
            if (left + ttWidth > svgWidth) {
                left = Math.max(5, (c.x / 800) * svgWidth - ttWidth - 10);
            }
            tooltip.style.left = left + 'px';
            tooltip.style.top = '15px';
            tooltip.style.display = 'block';
        }
    }

    function hideEvoHover() {
        if (activeIdx >= 0) {
            const activeCoord = coords[activeIdx];
            if (guide) {
                guide.setAttribute('x1', activeCoord.x);
                guide.setAttribute('x2', activeCoord.x);
                guide.style.display = 'block';
            }
        } else {
            if (guide) guide.style.display = 'none';
        }
        if (hoverDot) hoverDot.style.display = 'none';
        if (tooltip) tooltip.style.display = 'none';

        svg.querySelectorAll('.evo-node-circle').forEach((dot) => {
            const dotIdx = parseInt(dot.getAttribute('data-index'), 10);
            if (dotIdx === activeIdx) {
                dot.setAttribute('r', '6.5');
            } else if (dot.classList.contains('evo-node-growth')) {
                dot.setAttribute('r', '5.5');
            } else {
                dot.setAttribute('r', '4');
            }
        });
    }

    const wrapper = svg.closest('.suite-evolution-chart-wrapper') || svg;

    wrapper.onmousemove = function(e) {
        if (e.target.closest('#evoHoverTooltip')) return;
        const rect = svg.getBoundingClientRect();
        if (rect.width <= 0) return;
        const mouseX = e.clientX - rect.left;
        const viewBoxX = (mouseX / rect.width) * 800;

        let closestIdx = 0;
        let minDist = 99999;
        coords.forEach((c, idx) => {
            const dist = Math.abs(viewBoxX - c.x);
            if (dist < minDist) {
                minDist = dist;
                closestIdx = idx;
            }
        });
        showEvoHover(closestIdx);
    };

    wrapper.onclick = function(e) {
        if (e.target.closest('#evoHoverTooltip') || e.target.closest('.evo-point-group')) return;
        const rect = svg.getBoundingClientRect();
        if (rect.width <= 0) return;
        const mouseX = e.clientX - rect.left;
        const viewBoxX = (mouseX / rect.width) * 800;

        let closestIdx = 0;
        let minDist = 99999;
        coords.forEach((c, idx) => {
            const dist = Math.abs(viewBoxX - c.x);
            if (dist < minDist) {
                minDist = dist;
                closestIdx = idx;
            }
        });
        if (coords[closestIdx]) {
            setBatchHistoryAsOfRun(coords[closestIdx].point.runId);
        }
    };

    wrapper.onmouseleave = function(e) {
        if (e.relatedTarget && e.relatedTarget.closest && e.relatedTarget.closest('#evoHoverTooltip')) return;
        hideEvoHover();
    };

    if (tooltip) {
        tooltip.onmouseleave = function(e) {
            if (e.relatedTarget && e.relatedTarget.closest && e.relatedTarget.closest('.suite-evolution-chart-wrapper')) return;
            hideEvoHover();
        };
    }

    updateEvoStepperControls(activeRunId, getSuiteEvolutionMilestones());
}

function getSuiteEvolutionMilestones() {
    const svg = document.getElementById('batchSuiteEvolutionSvg');
    if (!svg) return [];
    const rawJson = svg.getAttribute('data-evolution-points');
    if (!rawJson) return [];
    let points = [];
    try {
        points = JSON.parse(rawJson);
    } catch (e) {
        return [];
    }
    if (!points || points.length === 0) return [];
    const isDense = points.length > 20;
    return points.filter((p, idx) => (!isDense) || (p.newTestsCount > 0) || (idx === 0) || (idx === points.length - 1));
}

function updateEvoStepperControls(activeRunId, milestones) {
    if (!milestones || milestones.length === 0) return;
    const count = milestones.length;
    let currentIdx = -1;
    if (activeRunId) {
        currentIdx = milestones.findIndex(m => String(m.runId) === String(activeRunId));
    }
    const isLatestSnapshot = (currentIdx === -1 || currentIdx === count - 1);
    const displayIdx = (currentIdx >= 0) ? currentIdx : (count - 1);
    const currentMilestone = milestones[displayIdx];

    const prevBtns = [document.getElementById('btnEvoPrevMilestone'), document.getElementById('btnChartArrowPrev')];
    const nextBtns = [document.getElementById('btnEvoNextMilestone'), document.getElementById('btnChartArrowNext')];
    const indicator = document.getElementById('evoMilestoneLabel');

    prevBtns.forEach(btn => {
        if (btn) btn.disabled = (displayIdx <= 0);
    });

    nextBtns.forEach(btn => {
        if (btn) btn.disabled = (displayIdx >= count - 1 && isLatestSnapshot);
    });

    if (indicator && currentMilestone) {
        const growthSuffix = currentMilestone.newTestsCount > 0 ? ` (+${currentMilestone.newTestsCount})` : '';
        const runText = `#${currentMilestone.runId}${growthSuffix}`;
        if (displayIdx === count - 1 && !activeRunId) {
            indicator.textContent = `Latest · Dot ${count}/${count} (${runText})`;
        } else {
            indicator.textContent = `Dot ${displayIdx + 1}/${count} (${runText})`;
        }
    }
}

function stepSuiteEvolutionMilestone(direction) {
    const svg = document.getElementById('batchSuiteEvolutionSvg');
    if (!svg) return;
    const milestones = getSuiteEvolutionMilestones();
    if (!milestones || milestones.length <= 1) return;

    const activeRunId = svg.getAttribute('data-active-run-id');
    let currentIdx = -1;
    if (activeRunId) {
        currentIdx = milestones.findIndex(m => String(m.runId) === String(activeRunId));
    }
    if (currentIdx === -1) {
        currentIdx = milestones.length - 1;
    }

    const nextIdx = currentIdx + direction;
    if (nextIdx < 0 || nextIdx >= milestones.length) {
        return;
    }

    const targetMilestone = milestones[nextIdx];
    if (nextIdx === milestones.length - 1 && direction > 0) {
        setBatchHistoryAsOfRun(null);
    } else {
        setBatchHistoryAsOfRun(targetMilestone.runId);
    }
}

function switchRunReportSubTab(subTabId, btn, skipClosePanel) {
    document.querySelectorAll('#runReportSubTabOverview, #runReportSubTabAllTests, #runReportSubTabTimeline').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.sub-tab-btn').forEach(el => el.classList.remove('active'));

    const target = document.getElementById(subTabId);
    if (target) target.classList.add('active');

    if (!btn && subTabId) {
        if (subTabId === 'runReportSubTabAllTests') btn = document.getElementById('runReportTabBtnAllTests');
        else if (subTabId === 'runReportSubTabTimeline') btn = document.getElementById('runReportTabBtnTimeline');
        else if (subTabId === 'runReportSubTabOverview') btn = document.getElementById('runReportTabBtnOverview');
    }
    if (btn) btn.classList.add('active');

    try {
        sessionStorage.setItem('activeRunReportSubTab', subTabId);
    } catch (e) {}

    if (window.history && window.history.replaceState) {
        try {
            const url = new URL(window.location.href);
            url.searchParams.set('subTab', subTabId);
            window.history.replaceState({}, '', url.toString());
        } catch (e) {}
    }

    if (!skipClosePanel && subTabId !== 'runReportSubTabAllTests') {
        closeSidePanelInspector();
    }
}

function switchTestBaseAreaTab(paneId, btn) {
    document.querySelectorAll('#pageTestBase .sub-tab-bar .sub-tab-btn').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('#pageTestBase .test-base-area-pane').forEach(el => {
        el.style.display = 'none';
    });

    if (btn) {
        btn.classList.add('active');
    }

    const targetPane = document.getElementById(paneId);
    if (targetPane) {
        targetPane.style.display = 'flex';
    }

    closeSidePanelInspector();
}

function toggleStepSection(headerEl) {
    const section = headerEl.parentElement;
    if (section) section.classList.toggle('expanded');
}

function toggleAreaGroup(headerEl) {
    const container = headerEl.parentElement;
    if (container) container.classList.toggle('expanded');
}

function toggleSubAreaGroup(headerEl) {
    const container = headerEl.parentElement;
    if (container) container.classList.toggle('expanded');
}

function toggleTestClassGroup(headerEl) {
    const container = headerEl.parentElement;
    if (!container) return;
    const isExpanded = container.classList.contains('expanded');
    const folderIcon = headerEl.querySelector('.material-symbols-outlined.text-accent');
    if (isExpanded) {
        container.classList.remove('expanded');
        if (folderIcon) folderIcon.textContent = 'folder';
    } else {
        container.classList.add('expanded');
        if (folderIcon) folderIcon.textContent = 'folder_open';
    }
}

function toggleBatchVariationsPanel(headerEl) {
    const panel = headerEl ? headerEl.closest('.collapsible-panel, .widget-test-variations-card') : document.getElementById('batchTestBaseVariationsPanel');
    if (panel) {
        panel.classList.toggle('expanded');
    }
}

function toggleRunsWidgetPanel(headerEl) {
    const panel = headerEl ? headerEl.closest('.collapsible-panel, .widget-runs-card') : null;
    if (panel) {
        panel.classList.toggle('expanded');
    }
}

function toggleAllBatchClassGroups(expand, btn) {
    const panel = (btn && btn.closest) ? btn.closest('.collapsible-panel, .widget-test-variations-card') : document.getElementById('batchTestBaseVariationsPanel');
    if (!panel) return;
    if (expand) {
        panel.classList.add('expanded');
    }
    panel.querySelectorAll('.area-group').forEach(ag => {
        if (expand) {
            ag.classList.add('expanded');
        } else {
            ag.classList.remove('expanded');
        }
    });
    panel.querySelectorAll('.sub-area-group').forEach(sag => {
        if (expand) {
            sag.classList.add('expanded');
        } else {
            sag.classList.remove('expanded');
        }
    });
    panel.querySelectorAll('.test-class-container').forEach(c => {
        const folderIcon = c.querySelector('.test-class-header .material-symbols-outlined.text-accent');
        if (expand) {
            c.classList.add('expanded');
            if (folderIcon) folderIcon.textContent = 'folder_open';
        } else {
            c.classList.remove('expanded');
            if (folderIcon) folderIcon.textContent = 'folder';
        }
    });
}

function toggleStepActionInspector(stepItemEl, event) {
    if (event) event.stopPropagation();
    stepItemEl.classList.toggle('inspector-active');
}

function toggleSubStepInspector(subStepEl, event) {
    if (event) event.stopPropagation();
    subStepEl.classList.toggle('sub-step-active');
}

function closeSidePanelInspector() {
    const panel = document.getElementById('testSidePagePanel');
    const resizer = document.getElementById('panelResizer');
    if (panel) panel.classList.remove('active');
    if (resizer) resizer.classList.remove('active');

    const linterContainer = document.getElementById('sidePageLinterFindingsContainer');
    if (linterContainer) {
        linterContainer.innerHTML = '';
        linterContainer.style.display = 'none';
    }
    const postFlightContainer = document.getElementById('sidePagePostFlightFindingsContainer');
    if (postFlightContainer) {
        postFlightContainer.innerHTML = '';
        postFlightContainer.style.display = 'none';
    }

    document.querySelectorAll('tr.clickable-row').forEach(r => {
        r.classList.remove('selected', 'active-selected-row');
        const marker = r.querySelector('.active-row-marker');
        if (marker) marker.remove();
    });
}

function filterAllTestsByStatus(statusKey) {
    switchRunReportSubTab('runReportSubTabAllTests', document.getElementById('runReportTabBtnAllTests'));

    // Clicking general status number or card resets healed filter to ALL
    currentHealedFilter = 'ALL';
    document.querySelectorAll('#healedFilterToggle .segment-btn').forEach(btn => {
        if (btn.getAttribute('data-val') === 'ALL') {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    if (statusKey === 'ALL' || activeWholeExecutionFilter === statusKey || currentGlobalStatusBadgeFilter === statusKey) {
        activeWholeExecutionFilter = null;
        currentGlobalStatusBadgeFilter = 'ALL';
        statusKey = null;
    } else {
        activeWholeExecutionFilter = statusKey;
        currentGlobalStatusBadgeFilter = statusKey;
    }

    document.querySelectorAll('#runReportSubTabOverview .metric-card, #runReportSubTabOverview .kpi-card').forEach(card => card.classList.remove('active-filter'));
    document.querySelectorAll('.js-filter-badge').forEach(badge => badge.classList.remove('active-filter', 'active-filter-badge'));

    if (statusKey) {
        const cardMap = {
            'PASSED': 'overviewKpiPassed',
            'SUCCEEDED_FIXED': 'overviewKpiFixed',
            'FAILED_KNOWN': 'overviewKpiKnown',
            'FAILED_UNKNOWN': 'overviewKpiUnknown',
            'SKIPPED': 'overviewKpiIgnored',
            'IGNORED': 'overviewKpiIgnored',
            'RUNNING': 'overviewKpiRunning',
            'passed-clean': 'metricValPassed',
            'succeeded-fixed': 'metricValSucceeded',
            'failed-known': 'metricValKnownFail',
            'failed-unknown': 'metricValUnknownFail',
            'ignored': 'metricValIgnored'
        };
        const cardValId = cardMap[statusKey];
        if (cardValId) {
            document.getElementById(cardValId)?.closest('.metric-card, .kpi-card')?.classList.add('active-filter');
        }
        document.querySelectorAll(`.js-filter-badge[data-status-key="${statusKey}"]`).forEach(b => b.classList.add('active-filter', 'active-filter-badge'));
    }

    applyFilter();
    applyRunReportFilters();
}

function applyRunReportFilters() {
    const modeMenu = document.getElementById('reportModesMultiselectMenu');
    const browserMenu = document.getElementById('reportBrowsersMultiselectMenu') || document.getElementById('browserMultiselectMenu');
    const locMenu = document.getElementById('reportLocalesMultiselectMenu') || document.getElementById('locationMultiselectMenu');

    const selectedModes = modeMenu ? Array.from(modeMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.trim().toUpperCase()) : [];
    const totalModes = modeMenu ? modeMenu.querySelectorAll('.js-chk-option').length : 0;

    const selectedBrowsers = browserMenu ? Array.from(browserMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.trim().toUpperCase()) : [];
    const totalBrowsers = browserMenu ? browserMenu.querySelectorAll('.js-chk-option').length : 0;

    const selectedLocs = locMenu ? Array.from(locMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.trim().toUpperCase()) : [];
    const totalLocs = locMenu ? locMenu.querySelectorAll('.js-chk-option').length : 0;

    const searchInput = document.getElementById('searchInput');
    const search = searchInput ? (searchInput.value || '').trim().toLowerCase() : '';

    document.querySelectorAll('#runReportSubTabAllTests .test-row').forEach(row => {
        let loc = (row.getAttribute('data-location') || '').trim().toUpperCase();
        if (!loc) loc = 'UNKNOWN';

        let browser = (row.getAttribute('data-browser') || '').trim().toUpperCase();
        if (!browser) browser = 'UNKNOWN';

        let mode = (row.getAttribute('data-mode') || '').trim().toUpperCase();
        if (!mode) mode = 'UNKNOWN';

        const rowSearch = (row.getAttribute('data-search') || (row.innerText || '')).toLowerCase();

        const matchLoc = !locMenu || totalLocs === 0 ? true : selectedLocs.includes(loc);
        const matchBrowser = !browserMenu || totalBrowsers === 0 ? true : selectedBrowsers.includes(browser);
        const matchMode = !modeMenu || totalModes === 0 ? true : selectedModes.includes(mode);
        const matchSearch = (!search || rowSearch.indexOf(search) !== -1);

        let matchStatus = true;
        const statusFilter = (currentGlobalStatusBadgeFilter && currentGlobalStatusBadgeFilter !== 'ALL') ? currentGlobalStatusBadgeFilter : (activeWholeExecutionFilter && activeWholeExecutionFilter !== 'ALL' ? activeWholeExecutionFilter : null);
        if (statusFilter) {
            const rowSt = (row.getAttribute('data-status') || '').toUpperCase();
            const rowStRaw = (row.getAttribute('data-status-raw') || '').toLowerCase();
            const isFlaky = row.getAttribute('data-flaky') === 'true';
            if (statusFilter === 'FLAKY') {
                matchStatus = isFlaky;
            } else if (statusFilter === 'SKIPPED' || statusFilter === 'IGNORED') {
                matchStatus = rowSt === 'SKIPPED' || rowStRaw === 'ignored' || rowStRaw === 'skipped';
            } else if (statusFilter === 'PASSED') {
                matchStatus = rowSt === 'PASSED' || rowStRaw === 'passed-clean' || rowStRaw === 'passed' || rowStRaw === 'succeeded';
            } else if (statusFilter === 'SUCCEEDED_FIXED' || statusFilter === 'FIXED') {
                matchStatus = rowSt === 'SUCCEEDED_FIXED' || rowStRaw === 'succeeded-fixed' || rowStRaw === 'fixed';
            } else if (statusFilter === 'FAILED_KNOWN' || statusFilter === 'KNOWN') {
                matchStatus = rowSt === 'FAILED_KNOWN' || rowStRaw === 'failed-known' || rowStRaw === 'known';
            } else if (statusFilter === 'FAILED_UNKNOWN' || statusFilter === 'UNKNOWN') {
                matchStatus = rowSt === 'FAILED_UNKNOWN' || rowStRaw === 'failed-unknown' || rowStRaw === 'failed' || rowStRaw === 'error';
            } else {
                matchStatus = rowSt === statusFilter;
            }
        }

        const areaGroup = row.closest('.area-group');
        const areaStatusFilter = areaGroup ? (areaGroup.getAttribute('data-active-status-filter') || 'ALL') : 'ALL';
        const subAreaGroup = row.closest('.sub-area-group');
        const subAreaStatusFilter = subAreaGroup ? (subAreaGroup.getAttribute('data-active-status-filter') || 'ALL') : 'ALL';
        const classContainer = row.closest('.test-class-container');
        const classStatusFilter = classContainer ? (classContainer.getAttribute('data-active-status-filter') || 'ALL') : 'ALL';

        function matchesBadge(filterKey) {
            if (!filterKey || filterKey === 'ALL') return true;
            const key = filterKey.toUpperCase();
            if (key === 'FLAKY') return row.getAttribute('data-flaky') === 'true';
            const rowSt = (row.getAttribute('data-status') || '').toUpperCase();
            const rowStRaw = (row.getAttribute('data-status-raw') || '').toLowerCase();
            if (key === 'PASSED') return rowSt === 'PASSED' || rowStRaw === 'passed-clean' || rowStRaw === 'passed' || rowStRaw === 'succeeded';
            if (key === 'SUCCEEDED_FIXED' || key === 'FIXED') return rowSt === 'SUCCEEDED_FIXED' || rowStRaw === 'succeeded-fixed' || rowStRaw === 'fixed';
            if (key === 'FAILED_KNOWN' || key === 'KNOWN') return rowSt === 'FAILED_KNOWN' || rowStRaw === 'failed-known' || rowStRaw === 'known';
            if (key === 'FAILED_UNKNOWN' || key === 'UNKNOWN') return rowSt === 'FAILED_UNKNOWN' || rowStRaw === 'failed-unknown' || rowStRaw === 'failed' || rowStRaw === 'error';
            if (key === 'SKIPPED' || key === 'IGNORED') return rowSt === 'SKIPPED' || rowStRaw === 'ignored' || rowStRaw === 'skipped';
            return rowSt.includes(key);
        }

        const matchArea = matchesBadge(areaStatusFilter);
        const matchSubArea = matchesBadge(subAreaStatusFilter);
        const matchClass = matchesBadge(classStatusFilter);

        row.style.display = (matchLoc && matchBrowser && matchMode && matchStatus && matchArea && matchSubArea && matchClass && matchSearch) ? '' : 'none';
    });

    document.querySelectorAll('#runReportSubTabAllTests .test-class-container').forEach(classBox => {
        const visibleRows = Array.from(classBox.querySelectorAll('.test-row')).filter(r => r.style.display !== 'none');
        classBox.style.display = visibleRows.length > 0 ? '' : 'none';
    });

    document.querySelectorAll('#runReportSubTabAllTests .sub-area-group').forEach(subAreaBox => {
        const visibleRows = Array.from(subAreaBox.querySelectorAll('.test-row')).filter(r => r.style.display !== 'none');
        subAreaBox.style.display = visibleRows.length > 0 ? '' : 'none';
    });

    document.querySelectorAll('#runReportSubTabAllTests .area-group').forEach(areaBox => {
        const visibleRows = Array.from(areaBox.querySelectorAll('.test-row')).filter(r => r.style.display !== 'none');
        areaBox.style.display = visibleRows.length > 0 ? '' : 'none';
    });

    const totalVisible = Array.from(document.querySelectorAll('#runReportSubTabAllTests .test-row')).filter(r => r.style.display !== 'none').length;
    const emptyState = document.getElementById('runReportFilterEmptyState');
    if (emptyState) {
        emptyState.style.display = totalVisible === 0 ? 'block' : 'none';
    }

    // Filter status badges on area group headers, sub-area group headers AND test class headers
    document.querySelectorAll('#runReportSubTabAllTests .area-group-header .badge-status, #runReportSubTabAllTests .sub-area-header .badge-status, #runReportSubTabAllTests .test-class-header .badge-status').forEach(badge => {
        const badgeType = badge.getAttribute('data-status-type') || badge.getAttribute('data-status-key');
        const stFilter = (currentGlobalStatusBadgeFilter && currentGlobalStatusBadgeFilter !== 'ALL') ? currentGlobalStatusBadgeFilter : (activeWholeExecutionFilter && activeWholeExecutionFilter !== 'ALL' ? activeWholeExecutionFilter : null);
        if (stFilter) {
            if (stFilter === 'SKIPPED' || stFilter === 'IGNORED') {
                badge.style.display = (badgeType === 'SKIPPED' || badgeType === 'IGNORED') ? '' : 'none';
            } else {
                badge.style.display = (badgeType === stFilter) ? '' : 'none';
            }
        } else {
            badge.style.display = '';
        }
    });
}

function resetGlobalTestFilters() {
    document.querySelectorAll('#runReportSubTabAllTests .js-chk-all, #runReportSubTabAllTests .js-chk-option').forEach(c => c.checked = true);
    document.querySelectorAll('#runReportSubTabAllTests .js-selected-text').forEach(s => s.innerText = 'All Selected');
    activeWholeExecutionFilter = null;
    currentGlobalStatusBadgeFilter = 'ALL';
    currentExecutionStatusFilter = 'ALL';
    currentExecutionModeFilter = 'ALL';
    currentHealedFilter = 'ALL';
    document.querySelectorAll('#runReportSubTabOverview .metric-card').forEach(card => card.classList.remove('active-filter'));
    document.querySelectorAll('.js-filter-badge').forEach(b => b.classList.remove('active-filter', 'active-filter-badge'));
    document.querySelectorAll('.area-group, .sub-area-group, .test-class-container').forEach(el => el.removeAttribute('data-active-status-filter'));
    const searchInput = document.getElementById('searchInput');
    if (searchInput) searchInput.value = '';
    applyRunReportFilters();
}

function openAllTestsAndExpandArea(targetAreaGroupId) {
    switchRunReportSubTab('runReportSubTabAllTests', document.getElementById('runReportTabBtnAllTests'));

    document.querySelectorAll('#runReportSubTabAllTests .area-group').forEach(areaContainer => {
        if (areaContainer.id === targetAreaGroupId) {
            areaContainer.style.display = '';
            areaContainer.classList.add('expanded');
        } else {
            areaContainer.classList.remove('expanded');
        }
    });

    const targetEl = document.getElementById(targetAreaGroupId);
    // targetEl.scrollIntoView removed: prevent automatic scrolling to test list on area click
}

function toggleTestDataInfoBox() {
    const box = document.getElementById('testDataInfoBox');
    if (box) box.classList.toggle('active');
}

function toggleBugTicketFormBox() {
    const box = document.getElementById('bugTicketFormBox');
    if (box) box.classList.toggle('active');
}

function toggleRunCommentFormBox() {
    const box = document.getElementById('runCommentFormBox');
    if (box) box.classList.toggle('active');
}

function resolveActiveRunId(preferredRunId, rowElement) {
    let rid = preferredRunId;
    if (!rid || rid === '#RUN_ID' || rid === 'RUN_ID') {
        rid = rowElement?.getAttribute('data-run-id');
    }
    if (!rid || rid === '#RUN_ID' || rid === 'RUN_ID') {
        const rootContainer = document.querySelector('[data-run-id]');
        rid = rootContainer?.getAttribute('data-run-id');
    }
    if (!rid || rid === '#RUN_ID' || rid === 'RUN_ID') {
        try {
            const urlParams = new URLSearchParams(window.location.search);
            rid = urlParams.get('runId');
        } catch (ignored) {}
    }
    if (!rid || rid === '#RUN_ID' || rid === 'RUN_ID') {
        const labelText = document.querySelector('.run-id-label')?.innerText?.trim();
        if (labelText && labelText !== '#RUN_ID' && labelText !== 'RUN_ID') {
            rid = labelText;
        }
    }
    if (rid && typeof rid === 'string') {
        rid = rid.trim().replace(/^#+/, '');
    }
    return (rid && rid !== 'RUN_ID' && rid !== '#RUN_ID') ? rid : '#RUN_ID';
}

const selectedBugTags = new Set();

function renderSelectedBugTags() {
    const container = document.getElementById('bugSelectedTagsContainer');
    const hiddenInput = document.getElementById('bugBoundTagsInput');
    if (hiddenInput) {
        hiddenInput.value = Array.from(selectedBugTags).join(', ');
    }
    if (!container) return;
    container.innerHTML = '';

    if (selectedBugTags.size === 0) {
        const placeholder = document.createElement('span');
        placeholder.style.fontSize = '0.72rem';
        placeholder.style.color = 'var(--text-muted)';
        placeholder.style.fontStyle = 'italic';
        placeholder.innerText = 'No tags selected (applies unconditionally across runs)';
        container.appendChild(placeholder);
    } else {
        const title = document.createElement('span');
        title.style.fontSize = '0.72rem';
        title.style.fontWeight = '700';
        title.style.color = 'var(--text-main)';
        title.style.marginRight = '0.2rem';
        title.innerText = 'Bound:';
        container.appendChild(title);

        selectedBugTags.forEach(tag => {
            const chip = document.createElement('span');
            chip.className = 'selected-bound-tag';

            const txt = document.createElement('span');
            txt.innerText = tag;
            chip.appendChild(txt);

            const removeBtn = document.createElement('span');
            removeBtn.className = 'material-symbols-outlined';
            removeBtn.innerText = 'close';
            removeBtn.title = 'Remove tag';
            removeBtn.onclick = function(e) {
                e.stopPropagation();
                toggleTagSelection(tag);
            };
            chip.appendChild(removeBtn);

            container.appendChild(chip);
        });
    }

    // Synchronize quick-pick chips active state
    document.querySelectorAll('.tag-chip-quick-select').forEach(chip => {
        const tag = chip.getAttribute('data-tag');
        if (selectedBugTags.has(tag)) {
            chip.classList.add('selected');
        } else {
            chip.classList.remove('selected');
        }
    });
}

function toggleTagSelection(tag) {
    if (!tag) return;
    const cleanTag = tag.trim();
    if (!cleanTag) return;
    if (selectedBugTags.has(cleanTag)) {
        selectedBugTags.delete(cleanTag);
    } else {
        selectedBugTags.add(cleanTag);
    }
    renderSelectedBugTags();
}

window.allKnownTagMap = window.allKnownTagMap || {};

function updateBugTagKeySelectOptions() {
    const keySelect = document.getElementById('bugTagKeySelect');
    if (!keySelect) return;
    const currentVal = keySelect.value;
    const existingKeys = new Set(Array.from(keySelect.options).map(o => o.value.toLowerCase()).filter(Boolean));

    Object.keys(window.allKnownTagMap || {}).forEach(k => {
        if (!existingKeys.has(k.toLowerCase())) {
            existingKeys.add(k.toLowerCase());
            const opt = document.createElement('option');
            opt.value = k;
            opt.innerText = k;
            keySelect.appendChild(opt);
        }
    });
    if (currentVal) {
        keySelect.value = currentVal;
    }
}

function onBugTagKeyChanged(selectedKey) {
    const valSelect = document.getElementById('bugTagValueSelect');
    const addBtn = document.getElementById('btnAddTagPairBtn');
    if (!valSelect) return;

    valSelect.innerHTML = '<option value="">-- Select Value --</option>';
    if (addBtn) addBtn.disabled = true;

    if (!selectedKey || !selectedKey.trim()) {
        valSelect.disabled = true;
        return;
    }

    const key = selectedKey.trim();
    let values = (window.allKnownTagMap && window.allKnownTagMap[key]) ? window.allKnownTagMap[key] : null;
    if ((!values || !Array.isArray(values) || values.length === 0) && window.allKnownTagMap) {
        const lowerKey = key.toLowerCase();
        for (const [k, v] of Object.entries(window.allKnownTagMap)) {
            if (k.toLowerCase() === lowerKey) {
                values = v;
                break;
            }
        }
    }

    if (Array.isArray(values) && values.length > 0) {
        values.forEach(val => {
            const opt = document.createElement('option');
            opt.value = val;
            opt.innerText = val;
            valSelect.appendChild(opt);
        });
        valSelect.disabled = false;
    } else {
        valSelect.disabled = true;
    }
}

function onBugTagValueSelected(selectedValue) {
    const addBtn = document.getElementById('btnAddTagPairBtn');
    if (selectedValue && selectedValue.trim().length > 0) {
        if (addBtn) addBtn.disabled = false;
        onAddTagPairClicked();
    } else if (addBtn) {
        addBtn.disabled = true;
    }
}

function onAddTagPairClicked() {
    const keySelect = document.getElementById('bugTagKeySelect');
    const valSelect = document.getElementById('bugTagValueSelect');
    const addBtn = document.getElementById('btnAddTagPairBtn');
    const key = keySelect?.value?.trim();
    const val = valSelect?.value?.trim();

    if (key && val) {
        const pair = `${key}: ${val}`;
        toggleTagSelection(pair);

        if (keySelect) keySelect.value = '';
        if (valSelect) {
            valSelect.innerHTML = '<option value="">-- Select Value --</option>';
            valSelect.disabled = true;
        }
        if (addBtn) addBtn.disabled = true;
    }
}

function onAddOtherTagSelected(selectEl) {
    if (!selectEl) return;
    const val = selectEl.value;
    if (val && val.trim().length > 0) {
        toggleTagSelection(val.trim());
        selectEl.value = '';
    }
}

function onBugSavedSuccessfully() {
    toggleBugTicketFormBox();
    const ticketInput = document.getElementById('bugTicketInput');
    if (ticketInput) ticketInput.value = '';
    selectedBugTags.clear();
    renderSelectedBugTags();
}

window.allKnownTagMap = window.allKnownTagMap || {};
window.updateBugTagKeySelectOptions = updateBugTagKeySelectOptions;
window.onBugTagKeyChanged = onBugTagKeyChanged;
window.onBugTagValueSelected = onBugTagValueSelected;
window.onAddTagPairClicked = onAddTagPairClicked;
window.toggleTagSelection = toggleTagSelection;
window.onAddOtherTagSelected = onAddOtherTagSelected;
window.onBugSavedSuccessfully = onBugSavedSuccessfully;
window.renderSelectedBugTags = renderSelectedBugTags;

function saveBugTicketLink() {
    const inputVal = document.getElementById('bugTicketInput')?.value || '';
    const severityVal = document.getElementById('bugSeveritySelect')?.value || 'MAJOR';
    const boundTagsVal = document.getElementById('bugBoundTagsInput')?.value || '';
    const parsedBugs = inputVal.split(/[, ]+/).map(s => s.trim()).filter(s => s.length > 0);
    const activeRow = currentActiveRowId ? (document.getElementById(currentActiveRowId) || document.querySelector(`[data-row-id="${currentActiveRowId}"]`)) : null;
    const activeRunId = resolveActiveRunId(null, activeRow);

    if (currentActiveRowId) {
        if (parsedBugs.length > 0) {
            executionBugMap[currentActiveRowId] = parsedBugs;
            parsedBugs.forEach(bugTicket => {
                const params = new URLSearchParams({
                    runId: activeRunId,
                    rowId: currentActiveRowId,
                    bugTicket: bugTicket,
                    severity: severityVal,
                    boundTags: boundTagsVal
                });
                fetch('/fragments/test-side-panel/bugs', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'HX-Request': 'true' },
                    body: params.toString()
                }).then(res => {
                    const triggerHeader = res.headers.get('HX-Trigger');
                    if (triggerHeader) {
                        try {
                            const triggerData = JSON.parse(triggerHeader);
                            if (triggerData.bugUpdated) {
                                document.body.dispatchEvent(new CustomEvent('bugUpdated', { detail: triggerData.bugUpdated }));
                            }
                        } catch (e) {
                            console.error('Failed to parse HX-Trigger:', e);
                        }
                    }
                    return res.text();
                }).then(html => {
                    const section = document.getElementById('sidePageStatusBadge');
                    if (section && html) section.outerHTML = html;
                    if (window.htmx) htmx.process(document.getElementById('sidePageStatusBadge') || document.body);
                }).catch(err => console.error('Failed to link bug ticket:', err));
            });
        }
        updateRowStatusAndMetrics(currentActiveRowId);
    }
    onBugSavedSuccessfully();
}

function saveRunComment() {
    const val = document.getElementById('runCommentInput')?.value || '';
    const card = document.getElementById('activeCommentDisplayCard');
    const valEl = document.getElementById('activeCommentVal');
    
    if (currentActiveRowId && val) {
        executionCommentsMap[currentActiveRowId] = val;
    }

    if (valEl) valEl.innerText = `"${val}"`;
    if (card && val) card.style.display = 'flex';

    toggleRunCommentFormBox();
}

function removeRunComment() {
    if (currentActiveRowId) delete executionCommentsMap[currentActiveRowId];
    const card = document.getElementById('activeCommentDisplayCard');
    const input = document.getElementById('runCommentInput');
    if (card) card.style.display = 'none';
    if (input) input.value = '';
}

function removeSpecificBugTicketLink(bugToRemove) {
    if (!currentActiveRowId) return;
    let existingBugs = executionBugMap[currentActiveRowId] || [];
    existingBugs = existingBugs.filter(b => b !== bugToRemove);
    if (existingBugs.length > 0) {
        executionBugMap[currentActiveRowId] = existingBugs;
    } else {
        delete executionBugMap[currentActiveRowId];
    }

    const activeRow = currentActiveRowId ? (document.getElementById(currentActiveRowId) || document.querySelector(`[data-row-id="${currentActiveRowId}"]`)) : null;
    const activeRunId = resolveActiveRunId(null, activeRow);
    fetch(`/fragments/test-side-panel/bugs?runId=${encodeURIComponent(activeRunId)}&rowId=${encodeURIComponent(currentActiveRowId)}&bugTicket=${encodeURIComponent(bugToRemove)}`, {
        method: 'DELETE',
        headers: { 'HX-Request': 'true' }
    }).then(res => {
        const triggerHeader = res.headers.get('HX-Trigger');
        if (triggerHeader) {
            try {
                const triggerData = JSON.parse(triggerHeader);
                if (triggerData.bugUpdated) {
                    document.body.dispatchEvent(new CustomEvent('bugUpdated', { detail: triggerData.bugUpdated }));
                }
            } catch (e) {
                console.error('Failed to parse HX-Trigger:', e);
            }
        }
        return res.text();
    }).then(html => {
        const section = document.getElementById('sidePageStatusBadge');
        if (section && html) section.outerHTML = html;
        if (window.htmx) htmx.process(document.getElementById('sidePageStatusBadge') || document.body);
    }).catch(err => console.error('Failed to unlink bug ticket:', err));

    updateRowStatusAndMetrics(currentActiveRowId);
}

function updateRowStatusAndMetrics(rowId) {
    const row = document.getElementById(rowId);
    if (!row) return;

    let activeBugs;
    if (executionBugMap[rowId] !== undefined) {
        activeBugs = executionBugMap[rowId];
    } else {
        const rawBugs = row.getAttribute('data-bugs') || '';
        activeBugs = (rawBugs && rawBugs !== 'NONE') ? rawBugs.split(',').map(s => s.trim()).filter(Boolean) : [];
        executionBugMap[rowId] = activeBugs;
    }

    const rawStatus = (row.getAttribute('data-status-raw') || '').toLowerCase();
    const currentStatus = (row.getAttribute('data-status') || '').toUpperCase();

    let newStatus = currentStatus;
    let newRawStatus = rawStatus;

    if (rawStatus === 'succeeded-fixed' || rawStatus === 'fixed' || rawStatus === 'healed' ||
        rawStatus === 'passed-clean' || rawStatus === 'passed' || rawStatus === 'succeeded') {
        newStatus = activeBugs.length > 0 ? 'SUCCEEDED_FIXED' : 'PASSED';
        newRawStatus = activeBugs.length > 0 ? 'succeeded-fixed' : 'passed-clean';
        currentExecutionIsFailed = false;
    } else if (rawStatus === 'failed-known' || rawStatus === 'known' ||
               rawStatus === 'failed' || rawStatus === 'failed-unknown' || rawStatus === 'error') {
        newStatus = activeBugs.length > 0 ? 'FAILED_KNOWN' : 'FAILED_UNKNOWN';
        newRawStatus = activeBugs.length > 0 ? 'failed-known' : 'failed-unknown';
        currentExecutionIsFailed = true;
    } else if (rawStatus === 'ignored' || rawStatus === 'skipped') {
        newStatus = 'SKIPPED';
        newRawStatus = 'ignored';
        currentExecutionIsFailed = false;
    } else {
        if (currentStatus === 'FAILED_UNKNOWN' || currentStatus === 'FAILED_KNOWN') {
            newStatus = activeBugs.length > 0 ? 'FAILED_KNOWN' : 'FAILED_UNKNOWN';
            newRawStatus = activeBugs.length > 0 ? 'failed-known' : 'failed-unknown';
            currentExecutionIsFailed = true;
        } else if (currentStatus === 'PASSED' || currentStatus === 'SUCCEEDED_FIXED' || currentStatus === 'HEALED') {
            newStatus = activeBugs.length > 0 ? 'SUCCEEDED_FIXED' : 'PASSED';
            newRawStatus = activeBugs.length > 0 ? 'succeeded-fixed' : 'passed-clean';
            currentExecutionIsFailed = false;
        }
    }

    row.setAttribute('data-status', newStatus);
    row.setAttribute('data-status-raw', newRawStatus);
    row.setAttribute('data-bugs', activeBugs.length > 0 ? activeBugs.join(',') : 'NONE');

    // Update Status Cell (.col-status-badge)
    const statusTd = row.querySelector('.col-status-badge') || row.querySelector('td:nth-child(2)');
    if (statusTd) {
        if (newStatus === 'PASSED' || newStatus === 'passed-clean') {
            statusTd.innerHTML = `<span class="badge-status badge-pass"><span class="material-symbols-outlined" style="font-size: 0.85rem; vertical-align: middle;">check_circle</span> PASSED</span>`;
        } else if (newStatus === 'SUCCEEDED_FIXED' || newStatus === 'succeeded-fixed') {
            statusTd.innerHTML = `<span class="badge-status badge-fixed"><span class="material-symbols-outlined" style="font-size: 0.85rem; vertical-align: middle;">bug_report</span> SUCCEEDED-FIXED</span>`;
        } else if (newStatus === 'FAILED_KNOWN' || newStatus === 'failed-known') {
            statusTd.innerHTML = `<span class="badge-status badge-known-fail"><span class="material-symbols-outlined" style="font-size: 0.85rem; vertical-align: middle;">bug_report</span> KNOWN FAIL</span>`;
        } else if (newStatus === 'FAILED_UNKNOWN' || newStatus === 'failed-unknown') {
            statusTd.innerHTML = `<span class="badge-status badge-unknown-fail"><span class="material-symbols-outlined" style="font-size: 0.85rem; vertical-align: middle;">cancel</span> UNKNOWN FAIL</span>`;
        } else if (newStatus === 'SKIPPED' || newStatus === 'ignored' || newStatus === 'skipped') {
            statusTd.innerHTML = `<span class="badge-status badge-ignored"><span class="material-symbols-outlined" style="font-size: 0.85rem; vertical-align: middle;">skip_next</span> SKIPPED</span>`;
        }
    }

    // Update Bug Info Cell (.col-bug-info)
    const bugTd = row.querySelector('.col-bug-info');
    if (bugTd) {
        if (activeBugs.length > 0) {
            const badgeStyle = currentExecutionIsFailed ? 'badge-known-fail' : 'badge-fixed';
            bugTd.innerHTML = activeBugs.map(b => `<span class="badge-status ${badgeStyle}"><span class="material-symbols-outlined">bug_report</span> ${b}</span>`).join(' ');
        } else if (newStatus === 'FAILED_UNKNOWN' || newStatus === 'failed-unknown') {
            const failure = row.getAttribute('data-failure') || 'Exception';
            bugTd.innerHTML = `<span class="badge-status badge-unknown-fail"><span class="material-symbols-outlined">error</span> ${failure}</span>`;
        } else {
            bugTd.innerHTML = `<span style="color: var(--text-muted); font-size: 0.75rem;">None</span>`;
        }
    }

    recalculateRunReportMetrics();
}

function cleanDataSetStringJS(raw) {
    if (!raw) return '';
    let s = raw.trim();
    if (s.startsWith('[Data Set:') && s.endsWith(']')) {
        s = s.substring(10, s.length - 1).trim();
    } else if (s.startsWith('[') && s.endsWith(']')) {
        s = s.substring(1, s.length - 1).trim();
    }
    return s;
}

function redirectToTestBaseVariationHistory(e) {
    const params = new URLSearchParams();
    if (currentTestNameStr) params.set('testName', currentTestNameStr);
    if (currentDataSetRaw) params.set('dataSet', currentDataSetRaw);
    if (currentLocationStr) params.set('location', currentLocationStr);
    if (currentBrowserStr) params.set('browser', currentBrowserStr);

    const queryString = params.toString() ? `?${params.toString()}` : '';
    const targetUrl = `/test-base${queryString}`;

    if (e && (e.ctrlKey || e.metaKey || e.shiftKey || e.button === 1)) {
        return;
    }
    if (e) {
        e.preventDefault();
    }
    closeTestSidePagePanel();
    window.location.href = targetUrl;
}

function getRowStatusCategory(r) {
    if (!r) return 'FAILED_UNKNOWN';
    const st = (r.getAttribute('data-status') || '').toUpperCase();
    const stRaw = (r.getAttribute('data-status-raw') || '').toLowerCase();
    const bugs = (r.getAttribute('data-bugs') || '').trim();
    const hasBugs = bugs !== '' && bugs.toUpperCase() !== 'NONE';

    if (st === 'RUNNING' || stRaw === 'running' || stRaw === 'in_progress' || stRaw === 'executing' || stRaw === 'pending') {
        return 'RUNNING';
    }
    if (st === 'SUCCEEDED_FIXED' || stRaw === 'succeeded-fixed' || stRaw === 'fixed') {
        return hasBugs ? 'SUCCEEDED_FIXED' : 'PASSED';
    }
    if (st === 'PASSED' || st === 'HEALED' || stRaw === 'passed-clean' || stRaw === 'passed' || stRaw === 'succeeded' || stRaw === 'healed') {
        return hasBugs ? 'SUCCEEDED_FIXED' : 'PASSED';
    }
    if (st === 'FAILED_KNOWN' || stRaw === 'failed-known' || stRaw === 'known') {
        return hasBugs ? 'FAILED_KNOWN' : 'FAILED_UNKNOWN';
    }
    if (st === 'FAILED_UNKNOWN' || stRaw === 'failed-unknown' || stRaw === 'failed' || stRaw === 'error') {
        return hasBugs ? 'FAILED_KNOWN' : 'FAILED_UNKNOWN';
    }
    if (st === 'SKIPPED' || stRaw === 'ignored' || stRaw === 'skipped') {
        return 'SKIPPED';
    }
    return hasBugs ? 'FAILED_KNOWN' : 'FAILED_UNKNOWN';
}

function recalculateRunReportMetrics() {
    const rows = document.querySelectorAll('#runReportSubTabAllTests .test-row, tr.execution-row');
    if (rows.length === 0) return;

    let pass = 0, fixed = 0, known = 0, unknown = 0, ignored = 0, running = 0;
    let passHealed = 0, fixedHealed = 0, knownHealed = 0, unknownHealed = 0;
    let totalLlmCalls = 0;
    let totalLlmTokens = 0;
    let totalLlmCost = 0.0;

    rows.forEach(r => {
        const cat = getRowStatusCategory(r);
        if (cat === 'PASSED') pass++;
        else if (cat === 'SUCCEEDED_FIXED') fixed++;
        else if (cat === 'FAILED_KNOWN') known++;
        else if (cat === 'FAILED_UNKNOWN') unknown++;
        else if (cat === 'SKIPPED') ignored++;
        else if (cat === 'RUNNING') running++;

        const isHealed = r.getAttribute('data-healed') === 'true' || Number(r.getAttribute('data-healed-count') || 0) > 0 || (r.getAttribute('data-status-raw') || '').toLowerCase() === 'healed';
        if (isHealed) {
            if (cat === 'PASSED') passHealed++;
            else if (cat === 'SUCCEEDED_FIXED') fixedHealed++;
            else if (cat === 'FAILED_KNOWN') knownHealed++;
            else if (cat === 'FAILED_UNKNOWN') unknownHealed++;
        }

        const ai = syncRowAiUsage(r);
        if (ai) {
            totalLlmCalls += ai.llmCalls;
            totalLlmTokens += ai.totalTokens;
            totalLlmCost += ai.totalCost;
        } else {
            totalLlmCalls += Number(r.getAttribute('data-llm-calls') || 0);
            totalLlmTokens += Number(r.getAttribute('data-total-tokens') || 0);
            const rawCost = r.getAttribute('data-est-cost') || '0';
            totalLlmCost += Number(String(rawCost).replace('$', '')) || 0;
        }
    });

    const totalExecutions = pass + fixed + known + unknown + ignored + running;
    const wholeTotalChip = document.getElementById('wholeExecutionTotalCountChip');
    if (wholeTotalChip) {
        wholeTotalChip.innerText = `${totalExecutions} Executions`;
    }

    const elPass = document.getElementById('metricValPassed');
    const elFixed = document.getElementById('metricValSucceeded');
    const elKnown = document.getElementById('metricValKnownFail');
    const elUnknown = document.getElementById('metricValUnknownFail');
    const elIgnored = document.getElementById('metricValIgnored');

    if (elPass) elPass.innerText = pass;
    if (elFixed) elFixed.innerText = fixed;
    if (elKnown) elKnown.innerText = known;
    if (elUnknown) elUnknown.innerText = unknown;
    if (elIgnored) elIgnored.innerText = ignored;

    const elOvTotal = document.getElementById('overviewKpiTotal');
    const elOvPass = document.getElementById('overviewKpiPassed');
    const elOvFixed = document.getElementById('overviewKpiFixed');
    const elOvKnown = document.getElementById('overviewKpiKnown');
    const elOvUnknown = document.getElementById('overviewKpiUnknown');
    const elOvIgnored = document.getElementById('overviewKpiIgnored');
    const elOvRunning = document.getElementById('overviewKpiRunning');
    const elOvLlmCost = document.getElementById('overviewKpiTotalLlmCost');
    const elOvLlmSub = document.getElementById('overviewKpiTotalLlmSub');

    if (elOvTotal) elOvTotal.innerText = totalExecutions;

    if (elOvPass) {
        if (passHealed > 0) {
            elOvPass.className = 'kpi-value-dual';
            elOvPass.innerHTML = `<span class="kpi-healed-part text-pass-healed" onclick="event.stopPropagation(); filterAllTestsByHealedStatus('PASSED', 'HEALED')" title="Filter Passed Healed Tests"><span class="material-symbols-outlined kpi-sparkle-icon">auto_fix_high</span> ${passHealed} healed</span><span class="kpi-slash">/</span><span class="kpi-total-part text-pass" onclick="event.stopPropagation(); filterAllTestsByStatus('PASSED')" title="Filter All Passed Tests">${pass} passed</span>`;
        } else {
            elOvPass.className = 'kpi-value text-pass';
            elOvPass.innerText = pass;
        }
    }

    if (elOvFixed) {
        if (fixedHealed > 0) {
            elOvFixed.className = 'kpi-value-dual';
            elOvFixed.innerHTML = `<span class="kpi-healed-part text-fixed-healed" onclick="event.stopPropagation(); filterAllTestsByHealedStatus('SUCCEEDED_FIXED', 'HEALED')" title="Filter Succeeded-Fixed Healed Tests"><span class="material-symbols-outlined kpi-sparkle-icon">auto_fix_high</span> ${fixedHealed} healed</span><span class="kpi-slash">/</span><span class="kpi-total-part text-fixed" onclick="event.stopPropagation(); filterAllTestsByStatus('SUCCEEDED_FIXED')" title="Filter All Succeeded-Fixed Tests">${fixed} fixed</span>`;
        } else {
            elOvFixed.className = 'kpi-value text-fixed';
            elOvFixed.innerText = fixed;
        }
    }

    if (elOvKnown) {
        if (knownHealed > 0) {
            elOvKnown.className = 'kpi-value-dual';
            elOvKnown.innerHTML = `<span class="kpi-healed-part text-known-healed" onclick="event.stopPropagation(); filterAllTestsByHealedStatus('FAILED_KNOWN', 'HEALED')" title="Filter Known Fail Healed Tests"><span class="material-symbols-outlined kpi-sparkle-icon">auto_fix_high</span> ${knownHealed} healed</span><span class="kpi-slash">/</span><span class="kpi-total-part text-known" onclick="event.stopPropagation(); filterAllTestsByStatus('FAILED_KNOWN')" title="Filter All Known Fail Tests">${known} known fail</span>`;
        } else {
            elOvKnown.className = 'kpi-value text-known';
            elOvKnown.innerText = known;
        }
    }

    if (elOvUnknown) {
        if (unknownHealed > 0) {
            elOvUnknown.className = 'kpi-value-dual';
            elOvUnknown.innerHTML = `<span class="kpi-healed-part text-unknown-healed" onclick="event.stopPropagation(); filterAllTestsByHealedStatus('FAILED_UNKNOWN', 'HEALED')" title="Filter Unknown Fail Healed Tests"><span class="material-symbols-outlined kpi-sparkle-icon">auto_fix_high</span> ${unknownHealed} healed</span><span class="kpi-slash">/</span><span class="kpi-total-part text-fail" onclick="event.stopPropagation(); filterAllTestsByStatus('FAILED_UNKNOWN')" title="Filter All Unknown Fail Tests">${unknown} unknown fail</span>`;
        } else {
            elOvUnknown.className = 'kpi-value text-fail';
            elOvUnknown.innerText = unknown;
        }
    }

    if (elOvIgnored) elOvIgnored.innerText = ignored;
    if (elOvRunning) elOvRunning.innerText = running;

    if (elOvLlmCost) {
        elOvLlmCost.innerText = '$' + (Math.ceil(totalLlmCost * 10000) / 10000).toFixed(4);
    }
    if (elOvLlmSub) {
        elOvLlmSub.innerText = `${totalLlmCalls} calls (${totalLlmTokens.toLocaleString()} tokens)`;
    }

    populateBrowserAndLocaleFilters();

    const wholeBadges = document.getElementById('wholeExecutionSummaryBadges');
    if (wholeBadges) {
        const isPassActive = currentGlobalStatusBadgeFilter === 'PASSED' ? ' active-filter-badge' : '';
        const isFixedActive = currentGlobalStatusBadgeFilter === 'SUCCEEDED_FIXED' ? ' active-filter-badge' : '';
        const isKnownActive = currentGlobalStatusBadgeFilter === 'FAILED_KNOWN' ? ' active-filter-badge' : '';
        const isUnknownActive = currentGlobalStatusBadgeFilter === 'FAILED_UNKNOWN' ? ' active-filter-badge' : '';
        const isSkippedActive = (currentGlobalStatusBadgeFilter === 'SKIPPED' || currentGlobalStatusBadgeFilter === 'IGNORED') ? ' active-filter-badge' : '';
        const isRunningActive = currentGlobalStatusBadgeFilter === 'RUNNING' ? ' active-filter-badge' : '';

        wholeBadges.innerHTML = `
            <span class="badge-status badge-pass clickable-badge js-filter-badge${isPassActive}" onclick="filterByGlobalStatusBadge('PASSED', this)" data-status-key="PASSED" title="Filter Passed">Passed: ${pass}</span>
            <span class="badge-status badge-fixed clickable-badge js-filter-badge${isFixedActive}" onclick="filterByGlobalStatusBadge('SUCCEEDED_FIXED', this)" data-status-key="SUCCEEDED_FIXED" title="Filter Succeeded-Fixed">Succeeded-Fixed: ${fixed}</span>
            <span class="badge-status badge-known-fail clickable-badge js-filter-badge${isKnownActive}" onclick="filterByGlobalStatusBadge('FAILED_KNOWN', this)" data-status-key="FAILED_KNOWN" title="Filter Known Fail">Known Fail: ${known}</span>
            <span class="badge-status badge-unknown-fail clickable-badge js-filter-badge${isUnknownActive}" onclick="filterByGlobalStatusBadge('FAILED_UNKNOWN', this)" data-status-key="FAILED_UNKNOWN" title="Filter Unknown Fail">Unknown Fail: ${unknown}</span>
            <span class="badge-status badge-ignored clickable-badge js-filter-badge${isSkippedActive}" onclick="filterByGlobalStatusBadge('SKIPPED', this)" data-status-key="SKIPPED" title="Filter Ignored/Skipped">Ignored: ${ignored}</span>
            ${running > 0 ? `<span class="badge-status badge-running clickable-badge js-filter-badge${isRunningActive}" onclick="filterByGlobalStatusBadge('RUNNING', this)" data-status-key="RUNNING" title="Filter Running">Running: ${running}</span>` : ''}
        `;
    }

    // Recalculate Area Breakdown and Pie Charts on Overview tab & Area Group Headers on All Tests tab
    document.querySelectorAll('#runReportSubTabAllTests .area-group, .area-group').forEach(areaGroup => {
        const areaName = areaGroup.getAttribute('data-area') || '';
        const areaRows = areaGroup.querySelectorAll('.test-row, tr.execution-row');

        let aPass = 0, aFixed = 0, aKnown = 0, aUnknown = 0, aIgnored = 0, aRunning = 0;
        let aPassHealed = 0, aFixedHealed = 0, aKnownHealed = 0, aUnknownHealed = 0;
        let aPassAi = 0, aFixedAi = 0, aKnownAi = 0, aUnknownAi = 0;
        areaRows.forEach(r => {
            const cat = getRowStatusCategory(r);
            if (cat === 'PASSED') aPass++;
            else if (cat === 'SUCCEEDED_FIXED') aFixed++;
            else if (cat === 'FAILED_KNOWN') aKnown++;
            else if (cat === 'FAILED_UNKNOWN') aUnknown++;
            else if (cat === 'SKIPPED') aIgnored++;
            else if (cat === 'RUNNING') aRunning++;

            const isHealed = r.getAttribute('data-healed') === 'true' || Number(r.getAttribute('data-healed-count') || 0) > 0 || (r.getAttribute('data-status-raw') || '').toLowerCase() === 'healed';
            if (isHealed) {
                if (cat === 'PASSED') aPassHealed++;
                else if (cat === 'SUCCEEDED_FIXED') aFixedHealed++;
                else if (cat === 'FAILED_KNOWN') aKnownHealed++;
                else if (cat === 'FAILED_UNKNOWN') aUnknownHealed++;
            }

            const isAi = r.getAttribute('data-ai') === 'true' || (r.getAttribute('data-mode') || '').toUpperCase() === 'AI' || Number(r.getAttribute('data-llm-calls') || 0) > 0;
            if (isAi) {
                if (cat === 'PASSED') aPassAi++;
                else if (cat === 'SUCCEEDED_FIXED') aFixedAi++;
                else if (cat === 'FAILED_KNOWN') aKnownAi++;
                else if (cat === 'FAILED_UNKNOWN') aUnknownAi++;
            }
        });

        const totalArea = aPass + aFixed + aKnown + aUnknown + aIgnored + aRunning;

        const areaExecChip = areaGroup.querySelector('.area-exec-count-chip');
        if (areaExecChip) {
            areaExecChip.innerText = `${totalArea} Executions`;
        }

        const areaBadgesContainer = areaGroup.querySelector('.area-summary-badges') || areaGroup.querySelector('.area-group-header .flex-gap-2');
        if (areaBadgesContainer) {
            const activeFilter = areaGroup.getAttribute('data-active-status-filter') || 'ALL';
            let badgesHtml = '';
            if (aPass > 0) badgesHtml += `<span class="badge-status badge-pass clickable-badge${activeFilter === 'PASSED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'PASSED');" title="Filter Passed in Area">Passed: ${aPass}</span>`;
            if (aFixed > 0) badgesHtml += `<span class="badge-status badge-fixed clickable-badge${activeFilter === 'SUCCEEDED_FIXED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'SUCCEEDED_FIXED');" title="Filter Succeeded-Fixed in Area">Succeeded-Fixed: ${aFixed}</span>`;
            if (aKnown > 0) badgesHtml += `<span class="badge-status badge-known-fail clickable-badge${activeFilter === 'FAILED_KNOWN' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'FAILED_KNOWN');" title="Filter Known Fail in Area">Known: ${aKnown}</span>`;
            if (aUnknown > 0) badgesHtml += `<span class="badge-status badge-unknown-fail clickable-badge${activeFilter === 'FAILED_UNKNOWN' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'FAILED_UNKNOWN');" title="Filter Unknown Fail in Area">Unknown: ${aUnknown}</span>`;
            if (aIgnored > 0) badgesHtml += `<span class="badge-status badge-ignored clickable-badge${activeFilter === 'SKIPPED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'SKIPPED');" title="Filter Ignored in Area">Ignored: ${aIgnored}</span>`;
            if (aRunning > 0) badgesHtml += `<span class="badge-status badge-running clickable-badge${activeFilter === 'RUNNING' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'RUNNING');" title="Filter Running in Area">Running: ${aRunning}</span>`;
            areaBadgesContainer.innerHTML = badgesHtml;
        }

        const pieCard = document.querySelector(`.area-pie-card[data-area="${CSS.escape(areaName)}"]`);
        if (pieCard) {
            const valPass = pieCard.querySelector('.val-pass');
            const valFixed = pieCard.querySelector('.val-fixed');
            const valKnown = pieCard.querySelector('.val-known');
            const valUnknown = pieCard.querySelector('.val-unknown');
            const valIgnored = pieCard.querySelector('.val-ignored');
            const valRunning = pieCard.querySelector('.val-running');

            if (valPass) valPass.innerText = aPass;
            if (valFixed) valFixed.innerText = aFixed;
            if (valKnown) valKnown.innerText = aKnown;
            if (valUnknown) valUnknown.innerText = aUnknown;
            if (valIgnored) valIgnored.innerText = aIgnored;
            if (valRunning) valRunning.innerText = aRunning;

            const syncInlineHealedBadge = (valElement, badgeClass, count) => {
                if (!valElement) return;
                const parentGroup = valElement.closest('.val-group') || valElement.parentElement;
                if (!parentGroup) return;
                let badge = parentGroup.querySelector(`.${badgeClass}`);
                if (count > 0) {
                    if (!badge) {
                        badge = document.createElement('span');
                        badge.className = `badge-healed-inline ${badgeClass}`;
                        parentGroup.appendChild(badge);
                    }
                    badge.innerText = `✨ ${count} healed`;
                    badge.style.display = 'inline-flex';
                } else if (badge) {
                    badge.style.display = 'none';
                }
            };

            const syncInlineAiBadge = (valElement, badgeClass, count) => {
                if (!valElement) return;
                const parentGroup = valElement.closest('.val-group') || valElement.parentElement;
                if (!parentGroup) return;
                let badge = parentGroup.querySelector(`.${badgeClass}`);
                if (count > 0) {
                    if (!badge) {
                        badge = document.createElement('span');
                        badge.className = `badge-ai-inline ${badgeClass}`;
                        parentGroup.appendChild(badge);
                    }
                    badge.innerText = `${count} AI`;
                    badge.style.display = 'inline-flex';
                } else if (badge) {
                    badge.style.display = 'none';
                }
            };

            syncInlineHealedBadge(valPass, 'badge-pass-healed', aPassHealed);
            syncInlineAiBadge(valPass, 'badge-pass-ai', aPassAi);
            syncInlineHealedBadge(valFixed, 'badge-fixed-healed', aFixedHealed);
            syncInlineAiBadge(valFixed, 'badge-fixed-ai', aFixedAi);
            syncInlineHealedBadge(valKnown, 'badge-known-healed', aKnownHealed);
            syncInlineAiBadge(valKnown, 'badge-known-ai', aKnownAi);
            syncInlineHealedBadge(valUnknown, 'badge-unknown-healed', aUnknownHealed);
            syncInlineAiBadge(valUnknown, 'badge-unknown-ai', aUnknownAi);

            const svgWrapper = pieCard.querySelector('.pie-chart-wrapper');
            if (svgWrapper && totalArea > 0) {
                const patId = 'sparkle-pat-' + encodeURIComponent(areaName).replace(/[^a-zA-Z0-9_-]/g, '_');
                const aiPatId = 'ai-pat-' + encodeURIComponent(areaName).replace(/[^a-zA-Z0-9_-]/g, '_');
                let currentOffset = 25;
                let circlesHtml = `
                    <svg width="100%" height="100%" viewBox="0 0 36 36">
                        <defs>
                            <pattern id="${patId}" width="4.5" height="4.5" patternUnits="userSpaceOnUse" patternTransform="rotate(25)">
                                <path d="M 2.25, 0.7 Q 2.25, 2.25 3.8, 2.25 Q 2.25, 2.25 2.25, 3.8 Q 2.25, 2.25 0.7, 2.25 Q 2.25, 2.25 2.25, 0.7 Z" fill="#ffffff" opacity="0.9"/>
                                <circle cx="3.8" cy="0.9" r="0.4" fill="#fef08a" opacity="0.95"/>
                            </pattern>
                            <pattern id="${aiPatId}" width="4" height="4" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
                                <line x1="0" y1="0" x2="0" y2="4" stroke="rgba(255,255,255,0.75)" stroke-width="1.2" />
                            </pattern>
                        </defs>
                        <circle cx="18" cy="18" r="15.915" fill="none" stroke="#e2e8f0" stroke-width="4"/>
                `;

                const addSubSlice = (count, color, title, isHealed = false, isAi = false) => {
                    if (count <= 0) return;
                    const pct = (count / totalArea) * 100;
                    const dashArray = `${pct} ${100 - pct}`;
                    circlesHtml += `<circle cx="18" cy="18" r="15.915" fill="none" stroke="${color}" stroke-width="5" stroke-dasharray="${dashArray}" stroke-dashoffset="${currentOffset}"><title>${title}: ${count}</title></circle>`;
                    if (isHealed) {
                        circlesHtml += `<circle cx="18" cy="18" r="15.915" fill="none" stroke="url(#${patId})" stroke-width="5" stroke-dasharray="${dashArray}" stroke-dashoffset="${currentOffset}" pointer-events="none"></circle>`;
                    }
                    if (isAi) {
                        circlesHtml += `<circle cx="18" cy="18" r="15.915" fill="none" stroke="url(#${aiPatId})" stroke-width="5" stroke-dasharray="${dashArray}" stroke-dashoffset="${currentOffset}" pointer-events="none"></circle>`;
                    }
                    currentOffset -= pct;
                };

                // 1. Successful: Clean (#059669) + Healed (#34d399 + sparkle) + AI (#059669 + pinstripe)
                const cleanPass = Math.max(0, aPass - aPassHealed - aPassAi);
                addSubSlice(cleanPass, '#059669', 'Clean Successful', false, false);
                addSubSlice(aPassHealed, '#34d399', 'Healed Successful', true, false);
                addSubSlice(aPassAi, '#059669', 'AI-Driven Successful', false, true);

                // 2. Succeeded-Fixed: Clean (#2563eb) + Healed (#60a5fa + sparkle) + AI (#2563eb + pinstripe)
                const cleanFixed = Math.max(0, aFixed - aFixedHealed - aFixedAi);
                addSubSlice(cleanFixed, '#2563eb', 'Clean Succeeded-Fixed', false, false);
                addSubSlice(aFixedHealed, '#60a5fa', 'Healed Succeeded-Fixed', true, false);
                addSubSlice(aFixedAi, '#2563eb', 'AI-Driven Succeeded-Fixed', false, true);

                // 3. Known Failed: Clean (#ea580c) + Healed (#fb923c + sparkle) + AI (#ea580c + pinstripe)
                const cleanKnown = Math.max(0, aKnown - aKnownHealed - aKnownAi);
                addSubSlice(cleanKnown, '#ea580c', 'Clean Known Fail', false, false);
                addSubSlice(aKnownHealed, '#fb923c', 'Healed Known Fail', true, false);
                addSubSlice(aKnownAi, '#ea580c', 'AI-Driven Known Fail', false, true);

                // 4. Unknown Failed: Clean (#dc2626) + Healed (#f87171 + sparkle) + AI (#dc2626 + pinstripe)
                const cleanUnknown = Math.max(0, aUnknown - aUnknownHealed - aUnknownAi);
                addSubSlice(cleanUnknown, '#dc2626', 'Clean Unknown Fail', false, false);
                addSubSlice(aUnknownHealed, '#f87171', 'Healed Unknown Fail', true, false);
                addSubSlice(aUnknownAi, '#dc2626', 'AI-Driven Unknown Fail', false, true);

                // 5. Ignored: (#64748b)
                addSubSlice(aIgnored, '#64748b', 'Ignored / Skipped', false, false);

                // 6. Running: (#94a3b8)
                addSubSlice(aRunning, '#94a3b8', 'Running', false, false);

                circlesHtml += '</svg>';
                svgWrapper.innerHTML = circlesHtml;
            }
        }
    });

    // Recalculate Test Class Summary Badges
    document.querySelectorAll('#runReportSubTabAllTests .test-class-container, .test-class-container').forEach(classBox => {
        const classRows = classBox.querySelectorAll('.test-row, tr.execution-row');
        let cPass = 0, cFixed = 0, cKnown = 0, cUnknown = 0, cIgnored = 0, cRunning = 0;
        classRows.forEach(r => {
            const cat = getRowStatusCategory(r);
            if (cat === 'PASSED') cPass++;
            else if (cat === 'SUCCEEDED_FIXED') cFixed++;
            else if (cat === 'FAILED_KNOWN') cKnown++;
            else if (cat === 'FAILED_UNKNOWN') cUnknown++;
            else if (cat === 'SKIPPED') cIgnored++;
            else if (cat === 'RUNNING') cRunning++;
        });

        const classBadgeContainer = classBox.querySelector('.test-class-header .flex-gap-2');
        if (classBadgeContainer) {
            const activeFilter = classBox.getAttribute('data-active-status-filter') || 'ALL';
            let html = '';
            if (cPass > 0) html += `<span class="badge-status badge-pass clickable-badge${activeFilter === 'PASSED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'PASSED');" title="Filter Passed in Class">Passed: ${cPass}</span>`;
            if (cFixed > 0) html += `<span class="badge-status badge-fixed clickable-badge${activeFilter === 'SUCCEEDED_FIXED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'SUCCEEDED_FIXED');" title="Filter Succeeded-Fixed in Class">Succeeded-Fixed: ${cFixed}</span>`;
            if (cKnown > 0) html += `<span class="badge-status badge-known-fail clickable-badge${activeFilter === 'FAILED_KNOWN' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'FAILED_KNOWN');" title="Filter Known Fail in Class">Known: ${cKnown}</span>`;
            if (cUnknown > 0) html += `<span class="badge-status badge-unknown-fail clickable-badge${activeFilter === 'FAILED_UNKNOWN' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'FAILED_UNKNOWN');" title="Filter Unknown Fail in Class">Unknown: ${cUnknown}</span>`;
            if (cIgnored > 0) html += `<span class="badge-status badge-ignored clickable-badge${activeFilter === 'SKIPPED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'SKIPPED');" title="Filter Ignored in Class">Ignored: ${cIgnored}</span>`;
            if (cRunning > 0) html += `<span class="badge-status badge-running clickable-badge${activeFilter === 'RUNNING' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'RUNNING');" title="Filter Running in Class">Running: ${cRunning}</span>`;
            classBadgeContainer.innerHTML = html;
        }
    });

    applyFilter();
}

function onSidePageBugsUpdated() {
    const statusContainer = document.getElementById('sidePageStatusBadge');
    if (!statusContainer) return;

    const selectedRow = document.querySelector('tr.execution-row.selected') || 
                        document.querySelector('tr.execution-row.active-selected-row') ||
                        (currentActiveRowId ? document.getElementById(currentActiveRowId) : null);
    if (!selectedRow) return;

    const rowId = selectedRow.getAttribute('data-row-id') || selectedRow.id || currentActiveRowId;

    const bugElements = statusContainer.querySelectorAll('.badge-status, .badge-known-fail, .badge-fixed, [data-bug-ticket]');
    const bugs = [];
    bugElements.forEach(el => {
        let ticket = el.getAttribute('data-bug-ticket');
        if (!ticket) {
            const text = (el.innerText || '').trim();
            const match = text.match(/\bBUG-[\w-]+\b/i) || text.match(/#([\w-]+)/);
            if (match) {
                ticket = match[0];
            }
        }
        if (ticket) {
            const clean = ticket.replace(/^#/, '').trim();
            if (clean && clean.toUpperCase() !== 'NONE' && !bugs.includes(clean)) {
                bugs.push(clean);
            }
        }
    });

    if (rowId) {
        executionBugMap[rowId] = bugs;
        selectedRow.setAttribute('data-bugs', bugs.length > 0 ? bugs.join(',') : 'NONE');
        updateRowStatusAndMetrics(rowId);
    }
}

document.addEventListener('htmx:afterSwap', function (evt) {
    if (evt.detail && evt.detail.target && (evt.detail.target.id === 'sidePageStatusBadge' || evt.detail.target.closest('#sidePageStatusBadge'))) {
        onSidePageBugsUpdated();
    }
});

function renderSidePanelStatusBugs() {
    const statusContainer = document.getElementById('sidePageStatusBadge');
    if (!statusContainer) return;

    const activeBugs = currentActiveRowId ? (executionBugMap[currentActiveRowId] || []) : [];

    if (activeBugs.length > 0) {
        const badgeStyle = currentExecutionIsFailed ? 'badge-known-fail' : 'badge-fixed';
        statusContainer.innerHTML = activeBugs.map(b => `
            <span class="badge-status ${badgeStyle}" style="font-family: var(--font-family);" data-bug-ticket="${b}">
                <span class="material-symbols-outlined">bug_report</span> ${b}
                <button class="btn-danger-subtle" title="Unlink ${b}" onclick="removeSpecificBugTicketLink('${b}')"><span class="material-symbols-outlined">delete</span></button>
            </span>
        `).join(' ');
    } else if (currentExecutionIsFailed) {
        statusContainer.innerHTML = `<span class="badge-status badge-unknown-fail" style="font-family: var(--font-family);">Failed (Without Issue Info)</span>`;
    } else {
        statusContainer.innerHTML = `<span class="badge-status badge-pass" style="font-family: var(--font-family);">Passed Clean</span>`;
    }
}

function resolveTestMethod(activeRow, blocksObj) {
    if (!activeRow && !blocksObj) return '';

    let method = activeRow ? (activeRow.getAttribute('data-test-method') || '') : '';
    if (!method && blocksObj) {
        method = blocksObj.testMethod || '';
    }
    if (method && method.trim().length > 0 && method.trim() !== 'null' && method.trim() !== 'undefined') {
        return method.trim();
    }

    const testFile = (activeRow ? (activeRow.getAttribute('data-test-file') || '') : '') || (blocksObj ? (blocksObj.testFile || '') : '');
    if (testFile && testFile.includes('#')) {
        const parts = testFile.split('#');
        if (parts.length > 1 && parts[1].trim().length > 0) {
            return parts[1].trim();
        }
    }

    let rowId = activeRow ? (activeRow.getAttribute('data-row-id') || activeRow.id || '') : '';
    if (rowId) {
        if (rowId.includes('%23')) {
            try { rowId = decodeURIComponent(rowId); } catch (e) {}
        }
        if (rowId.includes('#')) {
            const parts = rowId.split('#');
            if (parts.length > 1 && parts[1].trim().length > 0) {
                return parts[1].trim();
            }
        }
    }

    const testName = activeRow ? (activeRow.getAttribute('data-test-name') || '') : '';
    if (testName) {
        if (testName.includes('::')) {
            const parts = testName.split('::');
            if (parts.length > 1 && parts[1].trim().length > 0) {
                return parts[1].trim();
            }
        } else if (testName.includes('#')) {
            const parts = testName.split('#');
            if (parts.length > 1 && parts[1].trim().length > 0) {
                return parts[1].trim();
            }
        }
    }

    return '';
}

function openTestSidePagePanel(testName, dataSet, statusKey, issueTag, rowId, runId) {
    currentActiveRowId = rowId || null;
    currentTestNameStr = testName || '';
    currentDataSetRaw = dataSet || '';
    currentDataSetStr = dataSet ? (dataSet.startsWith('[') ? dataSet : `[Data Set: ${dataSet}]`) : '';

    const activeRow = rowId ? document.getElementById(rowId) : null;
    if (activeRow) {
        if (!currentTestNameStr) currentTestNameStr = activeRow.getAttribute('data-test-name') || '';
        if (!currentDataSetRaw) currentDataSetRaw = activeRow.getAttribute('data-dataset') || '';
        currentLocationStr = activeRow.getAttribute('data-location') || '';
        currentBrowserStr = activeRow.getAttribute('data-browser') || '';
    } else {
        currentLocationStr = '';
        currentBrowserStr = '';
    }

    document.querySelectorAll('tr.clickable-row').forEach(r => {
        r.classList.remove('selected', 'active-selected-row');
        const marker = r.querySelector('.active-row-marker');
        if (marker) marker.remove();
    });

    if (activeRow) {
        activeRow.classList.add('selected', 'active-selected-row');
        const firstTd = activeRow.querySelector('td');
        if (firstTd && !firstTd.querySelector('.active-row-marker')) {
            const marker = document.createElement('span');
            marker.className = 'active-row-marker';
            marker.innerHTML = '<span class="active-indicator-badge"><span class="material-symbols-outlined" style="font-size: 0.8rem;">play_arrow</span> OPEN</span>';
            firstTd.prepend(marker);
        }
    }

    const activeRunId = resolveActiveRunId(runId, activeRow);
    const activeRowId = rowId || activeRow?.getAttribute('data-row-id') || activeRow?.id || '';
    const isTestBaseRow = activeRow ? (activeRow.closest('#pageTestBase') !== null || activeRow.closest('#batchTestBaseVariationsPanel') !== null || activeRow.classList.contains('tb-entry-row')) : (document.getElementById('pageTestBase') !== null);

    // Synchronize form inputs for bug ticket entry
    const bugRunInput = document.querySelector('#bugTicketFormBox input[name="runId"]');
    const bugRowInput = document.querySelector('#bugTicketFormBox input[name="rowId"]');
    if (bugRunInput) bugRunInput.value = activeRunId;
    if (bugRowInput) bugRowInput.value = activeRowId;

    selectedBugTags.clear();
    const availableTagsContainer = document.getElementById('bugAvailableTagsContainer');
    if (availableTagsContainer) {
        availableTagsContainer.innerHTML = '';
        const quickTags = new Set();

        // 1. Run tags: environment and batch
        const env = activeRow?.getAttribute('data-environment')
                 || activeRow?.getAttribute('data-env')
                 || document.querySelector('[data-environment]')?.getAttribute('data-environment')
                 || '';
        if (env && env !== 'Default' && env !== 'Unknown') {
            quickTags.add(`environment: ${env}`);
        }

        const batch = activeRow?.getAttribute('data-batch')
                   || activeRow?.getAttribute('data-batch-name')
                   || document.querySelector('[data-batch-name]')?.getAttribute('data-batch-name')
                   || '';
        if (batch && batch !== 'Default' && batch !== 'Unknown') {
            quickTags.add(`batch: ${batch}`);
        }

        // 2. Execution tags: location and browser
        const browser = activeRow?.getAttribute('data-browser');
        if (browser && browser !== 'Unknown') {
            quickTags.add(`browser: ${browser}`);
        }
        const location = activeRow?.getAttribute('data-location');
        if (location && location !== 'Unknown') {
            quickTags.add(`location: ${location}`);
        }

        // 3. Execution custom tags
        const rawTags = activeRow?.getAttribute('data-tags');
        if (rawTags) {
            rawTags.split(',').map(s => s.trim()).filter(Boolean).forEach(t => quickTags.add(t));
        }

        // 4. Run-level tags
        const runTagsAttr = document.querySelector('[data-run-tags]')?.getAttribute('data-run-tags');
        if (runTagsAttr) {
            runTagsAttr.split(',').map(s => s.trim()).filter(Boolean).forEach(t => quickTags.add(t));
        }

        if (quickTags.size > 0) {
            const label = document.createElement('span');
            label.style.color = 'var(--text-muted)';
            label.style.fontSize = '0.72rem';
            label.innerText = 'Quick-pick:';
            availableTagsContainer.appendChild(label);

            quickTags.forEach(tag => {
                const chip = document.createElement('span');
                chip.className = 'tag-chip-quick-select';
                chip.setAttribute('data-tag', tag);
                chip.innerText = tag;
                chip.onclick = function() {
                    toggleTagSelection(tag);
                };
                availableTagsContainer.appendChild(chip);
            });
        }
    }

    // Harvest all known tag keys and values from DOM into window.allKnownTagMap
    window.allKnownTagMap = window.allKnownTagMap || {};
    const ensureTagInMap = (key, val) => {
        if (!key || !val) return;
        const cleanK = key.trim().toLowerCase();
        const cleanV = val.trim();
        if (!cleanV || cleanV === 'Default' || cleanV === 'Unknown' || cleanV === 'ALL') return;

        let canonicalKey = cleanK;
        if (cleanK === 'env' || cleanK === 'instance') canonicalKey = 'environment';
        else if (cleanK === 'suite' || cleanK === 'batchname') canonicalKey = 'batch';
        else if (cleanK === 'locale') canonicalKey = 'location';

        window.allKnownTagMap[canonicalKey] = window.allKnownTagMap[canonicalKey] || [];
        if (!window.allKnownTagMap[canonicalKey].includes(cleanV)) {
            window.allKnownTagMap[canonicalKey].push(cleanV);
            window.allKnownTagMap[canonicalKey].sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }));
        }
    };

    document.querySelectorAll('tr[data-environment], [data-environment], [data-env]').forEach(r => {
        const e = r.getAttribute('data-environment') || r.getAttribute('data-env');
        ensureTagInMap('environment', e);
    });
    document.querySelectorAll('tr[data-batch], [data-batch], [data-batch-name]').forEach(r => {
        const b = r.getAttribute('data-batch') || r.getAttribute('data-batch-name');
        ensureTagInMap('batch', b);
    });
    document.querySelectorAll('tr[data-browser], [data-browser]').forEach(r => {
        const b = r.getAttribute('data-browser');
        ensureTagInMap('browser', b);
    });
    document.querySelectorAll('tr[data-location], [data-location], [data-locale]').forEach(r => {
        const l = r.getAttribute('data-location') || r.getAttribute('data-locale');
        ensureTagInMap('location', l);
    });
    document.querySelectorAll('tr[data-tags], [data-tags]').forEach(r => {
        const t = r.getAttribute('data-tags');
        if (t) {
            t.split(',').map(s => s.trim()).filter(Boolean).forEach(tag => {
                if (tag.includes(':')) {
                    const parts = tag.split(':');
                    ensureTagInMap(parts[0], parts.slice(1).join(':'));
                }
            });
        }
    });
    const pageRunTags = document.querySelector('[data-run-tags]')?.getAttribute('data-run-tags');
    if (pageRunTags) {
        pageRunTags.split(',').map(s => s.trim()).filter(Boolean).forEach(tag => {
            if (tag.includes(':')) {
                const parts = tag.split(':');
                ensureTagInMap(parts[0], parts.slice(1).join(':'));
            }
        });
    }

    updateBugTagKeySelectOptions();

    renderSelectedBugTags();

    // Dynamically load live bug badge section for the selected execution (only for single run reports, not Test Base)
    if (typeof htmx !== 'undefined' && document.getElementById('sidePageStatusBadge') && !isTestBaseRow && activeRunId !== '#RUN_ID') {
        htmx.ajax('GET', `/fragments/test-side-panel/bugs?runId=${encodeURIComponent(activeRunId)}&rowId=${encodeURIComponent(activeRowId)}`, {
            target: '#sidePageStatusBadge',
            swap: 'outerHTML'
        });
    }

    const nameEl = document.getElementById('sidePageTestName');
    const dsEl = document.getElementById('sidePageTestDataSet');
    const startTimeEl = document.getElementById('sidePageTestStartTimeVal');
    
    const resolvedMethod = resolveTestMethod(activeRow, null);
    if (nameEl) {
        if (resolvedMethod && currentTestNameStr && !currentTestNameStr.includes('::') && !currentTestNameStr.includes(resolvedMethod)) {
            nameEl.innerText = `${currentTestNameStr} :: ${resolvedMethod}`;
        } else {
            nameEl.innerText = currentTestNameStr || testName;
        }
    }
    if (dsEl) dsEl.innerText = currentDataSetStr;

    const btnHistory = document.getElementById('btnViewHistoryInTestBase');
    if (btnHistory) {
        const histParams = new URLSearchParams();
        if (currentTestNameStr) histParams.set('testName', currentTestNameStr);
        if (currentDataSetRaw) histParams.set('dataSet', currentDataSetRaw);
        if (currentLocationStr) histParams.set('location', currentLocationStr);
        if (currentBrowserStr) histParams.set('browser', currentBrowserStr);
        const histQs = histParams.toString() ? `?${histParams.toString()}` : '';
        btnHistory.href = `/test-base${histQs}`;
    }

    if (startTimeEl) {
        const rawTs = activeRow ? (activeRow.getAttribute('data-timestamp-ms') || activeRow.getAttribute('data-timestamp') || activeRow.getAttribute('data-start-time') || '') : '';
        let startStr = formatTimestamp(rawTs);
        if (!startStr && activeRow) {
            const dateFmt = activeRow.getAttribute('data-date-formatted') || '';
            const timeFmt = activeRow.getAttribute('data-time-formatted') || '';
            if (dateFmt || timeFmt) {
                startStr = `${dateFmt} ${timeFmt}`.trim();
            }
        }
        if (startTimeEl.parentElement) {
            if (startStr) {
                startTimeEl.innerText = `Started: ${startStr}`;
                startTimeEl.parentElement.style.display = 'flex';
            } else {
                startTimeEl.parentElement.style.display = 'none';
            }
        }
    }

    const isFlaky = activeRow ? (activeRow.getAttribute('data-flaky') === 'true') : false;
    const flakyReason = activeRow ? (activeRow.getAttribute('data-flaky-reason') || 'Flaky test: frequent unknown failures in previous runs') : '';
    const flakyWarnBox = document.getElementById('sidePageFlakyWarningBox');
    const flakyWarnText = document.getElementById('sidePageFlakyWarningText');
    const tbSideFlakyBadge = document.getElementById('tbSideFlakyBadge');
    if (flakyWarnBox) {
        if (isFlaky) {
            flakyWarnBox.style.display = 'flex';
            if (flakyWarnText) flakyWarnText.innerText = flakyReason;
        } else {
            flakyWarnBox.style.display = 'none';
        }
    }
    if (tbSideFlakyBadge) {
        tbSideFlakyBadge.style.display = isFlaky ? 'inline-flex' : 'none';
        if (isFlaky && flakyReason) tbSideFlakyBadge.title = flakyReason;
    }

    const singleRunControls = document.getElementById('sidePageSingleRunControls');
    const testBaseControls = document.getElementById('sidePageTestBaseVariationControls');

    if (isTestBaseRow && testBaseControls) {
        if (singleRunControls) singleRunControls.style.display = 'none';
        testBaseControls.style.display = 'flex';

        const loc = activeRow?.getAttribute('data-location') || 'Default';
        const browser = activeRow?.getAttribute('data-browser') || 'Chrome';
        const testClass = activeRow?.getAttribute('data-test-name') || testName || '';
        const testMethod = activeRow?.getAttribute('data-test-method') || '';
        const dataSetVal = activeRow?.getAttribute('data-dataset') || dataSet || '';

        const locBadge = document.getElementById('tbSideLocaleBadge');
        const browserBadge = document.getElementById('tbSideBrowserBadge');
        if (locBadge) locBadge.innerHTML = `<span class="material-symbols-outlined">location_on</span> ${loc}`;
        if (browserBadge) browserBadge.innerHTML = `<span class="material-symbols-outlined text-accent">language</span> ${browser}`;

        loadTestBaseVariationHistory(testClass, testMethod, dataSetVal, loc, browser);
    } else {
        if (testBaseControls) testBaseControls.style.display = 'none';
        if (singleRunControls) singleRunControls.style.display = 'flex';

        currentExecutionIsFailed = statusKey ? statusKey.includes('fail') : true;
        renderSidePanelStatusBugs();
        renderStepsForExecution(activeRow);
    }

    const panel = document.getElementById('testSidePagePanel');
    const resizer = document.getElementById('allurePanelResizer') || document.getElementById('panelResizer');
    if (panel) {
        const maxPanelWidth = Math.max(320, window.innerWidth - 420);
        const defaultWidth = Math.min(920, maxPanelWidth);
        const savedWidth = localStorage.getItem('aura_side_panel_width');
        if (savedWidth) {
            const parsedWidth = parseInt(savedWidth, 10);
            const targetWidth = isTestBaseRow ? Math.max(defaultWidth, parsedWidth) : parsedWidth;
            panel.style.width = Math.min(targetWidth, maxPanelWidth) + 'px';
        } else {
            panel.style.width = defaultWidth + 'px';
        }
        panel.classList.add('active');
    }
    if (resizer) resizer.classList.add('active');
}

function extractCallCost(c) {
    if (!c) return 0;
    const raw = c.estimatedCostUsd !== undefined ? c.estimatedCostUsd :
               (c.costUsd !== undefined ? c.costUsd :
               (c.cost_usd !== undefined ? c.cost_usd :
               (c.estimatedCost !== undefined ? c.estimatedCost :
               (c.cost !== undefined ? c.cost :
               (c.totalCost !== undefined ? c.totalCost : 0)))));
    const num = Number(raw);
    return (!isNaN(num) && num > 0) ? num : 0;
}

function extractCallTokens(c) {
    if (!c) return 0;
    const raw = c.totalTokens !== undefined ? c.totalTokens :
               (c.tokens !== undefined ? c.tokens : 0);
    const num = Number(raw);
    return (!isNaN(num) && num > 0) ? num : 0;
}

function syncRowAiUsage(row) {
    if (!row) return null;
    const blocksAttr = row.getAttribute('data-blocks');
    const stepsAttr = row.getAttribute('data-steps');
    let blocksObj = null;
    try {
        if (blocksAttr && blocksAttr !== '{}') blocksObj = JSON.parse(blocksAttr);
    } catch (e) {}

    if (!blocksObj || (!blocksObj.before && !blocksObj.steps && !blocksObj.after && !blocksObj.llmCalls)) {
        let stepsObj = null;
        try {
            if (stepsAttr && stepsAttr !== '{}') stepsObj = JSON.parse(stepsAttr);
        } catch (e) {}
        if (stepsObj && stepsObj.tries) {
            const try1 = stepsObj.tries['1'] || Object.values(stepsObj.tries)[0];
            if (try1) {
                blocksObj = {
                    before: try1.beforeSteps || [],
                    steps: try1.coreSteps || [],
                    after: try1.afterSteps || []
                };
            }
        }
    }

    let allLlmCalls = [];
    if (blocksObj) {
        if (Array.isArray(blocksObj.llmCalls) && blocksObj.llmCalls.length > 0) {
            allLlmCalls = blocksObj.llmCalls;
        } else {
            const allSteps = [...(blocksObj.before || []), ...(blocksObj.steps || []), ...(blocksObj.after || [])];
            allSteps.forEach(s => {
                if (Array.isArray(s.llmCalls)) {
                    allLlmCalls.push(...s.llmCalls);
                }
            });
        }
    }

    let calcLlmCalls = 0;
    let calcTotalTokens = 0;
    let calcInputTokens = 0;
    let calcOutputTokens = 0;
    let calcCachedTokens = 0;
    let calcTotalCost = 0.0;

    if (allLlmCalls.length > 0) {
        calcLlmCalls = allLlmCalls.length;
        allLlmCalls.forEach(c => {
            const inp = Number(c.inputTokens || 0);
            const out = Number(c.outputTokens || 0);
            const cac = Number(c.cachedTokens || 0);
            const tot = extractCallTokens(c) || (inp + out);
            const cost = extractCallCost(c);
            calcInputTokens += inp;
            calcOutputTokens += out;
            calcCachedTokens += cac;
            calcTotalTokens += tot;
            calcTotalCost += cost;
        });
    } else {
        calcLlmCalls = Number(row.getAttribute('data-llm-calls') || '0');
        calcTotalTokens = Number(row.getAttribute('data-total-tokens') || '0');
        calcInputTokens = Number(row.getAttribute('data-input-tokens') || '0');
        calcOutputTokens = Number(row.getAttribute('data-output-tokens') || '0');
        calcCachedTokens = Number(row.getAttribute('data-cached-tokens') || '0');
        const rawCostAttr = row.getAttribute('data-est-cost') || '0';
        calcTotalCost = Number(rawCostAttr.replace('$', '')) || 0;
    }

    const calcCostFormatted = '$' + (Math.ceil(calcTotalCost * 10000) / 10000).toFixed(4);

    row.setAttribute('data-llm-calls', calcLlmCalls);
    row.setAttribute('data-total-tokens', calcTotalTokens);
    row.setAttribute('data-input-tokens', calcInputTokens);
    row.setAttribute('data-output-tokens', calcOutputTokens);
    row.setAttribute('data-cached-tokens', calcCachedTokens);
    row.setAttribute('data-est-cost', calcCostFormatted);

    const aiUsageCell = row.querySelector('td:last-child');
    if (aiUsageCell) {
        if (calcLlmCalls > 0) {
            aiUsageCell.innerHTML = `
                <div>
                    <div class="llm-stat"><span class="material-symbols-outlined" style="font-size: 0.85rem; vertical-align: middle;">smart_toy</span> <span>${calcLlmCalls} call(s)</span></div>
                    <div class="llm-tokens">${calcTotalTokens.toLocaleString()} toks (${calcCostFormatted})</div>
                </div>
            `;
        } else {
            aiUsageCell.innerHTML = `<div class="llm-muted">—</div>`;
        }
    }

    return {
        llmCalls: calcLlmCalls,
        totalTokens: calcTotalTokens,
        inputTokens: calcInputTokens,
        outputTokens: calcOutputTokens,
        cachedTokens: calcCachedTokens,
        totalCost: calcTotalCost,
        costFormatted: calcCostFormatted
    };
}

function syncAllRowsAiUsage() {
    document.querySelectorAll('.execution-row').forEach(row => syncRowAiUsage(row));
}

let liveStepRefreshTimer = null;

function stopLiveStepRefreshTimer() {
    if (liveStepRefreshTimer) {
        clearInterval(liveStepRefreshTimer);
        liveStepRefreshTimer = null;
    }
}

function extractBlocksJson(execDto) {
    if (!execDto) return null;
    if (typeof execDto.blocksJson === 'string' && execDto.blocksJson !== '{}' && execDto.blocksJson !== 'null' && execDto.blocksJson.trim().length > 0) {
        return execDto.blocksJson;
    }
    if (execDto.blocks) {
        try {
            const str = typeof execDto.blocks === 'string' ? execDto.blocks : JSON.stringify(execDto.blocks);
            if (str && str !== '{}' && str !== 'null' && str.trim().length > 0) return str;
        } catch (e) {}
    }
    return null;
}

function extractStepsJson(execDto) {
    if (!execDto) return null;
    if (typeof execDto.stepsJson === 'string' && execDto.stepsJson !== '{}' && execDto.stepsJson !== 'null' && execDto.stepsJson.trim().length > 0) {
        return execDto.stepsJson;
    }
    if (execDto.steps) {
        try {
            const str = typeof execDto.steps === 'string' ? execDto.steps : JSON.stringify(execDto.steps);
            if (str && str !== '{}' && str !== 'null' && str.trim().length > 0) return str;
        } catch (e) {}
    }
    return null;
}

function extractFindingsArray(raw) {
    if (!raw) return [];
    if (Array.isArray(raw)) return raw;
    if (typeof raw === 'string' && raw.trim().length > 0) {
        try {
            const parsed = JSON.parse(raw);
            if (Array.isArray(parsed)) return parsed;
        } catch (e) {}
    }
    return [];
}

function startLiveStepRefreshTimer(activeRow) {
    stopLiveStepRefreshTimer();
    if (!activeRow) return;

    const runId = activeRow.getAttribute('data-run-id') || (typeof activeRunId !== 'undefined' ? activeRunId : '#RUN_ID');
    const rowId = activeRow.getAttribute('data-row-id') || activeRow.id;

    if (!runId || !rowId) return;

    liveStepRefreshTimer = setInterval(() => {
        let currentActiveEl = activeRow;
        if (!document.body.contains(currentActiveEl)) {
            const safeRowId = (rowId || '').replace(/\\/g, '\\\\').replace(/"/g, '\\"');
            currentActiveEl = document.querySelector(`[data-row-id="${safeRowId}"]`) ||
                              (activeRow.id ? document.getElementById(activeRow.id) : null);
        }

        const stepListEl = document.getElementById('sidePageStepList');
        if (!currentActiveEl || !stepListEl) {
            stopLiveStepRefreshTimer();
            return;
        }

        fetch(`/fragments/test-side-panel/steps?runId=${encodeURIComponent(runId)}&rowId=${encodeURIComponent(rowId)}`)
            .then(res => {
                if (!res.ok) return null;
                return res.json();
            })
            .then(execDto => {
                if (execDto) {
                    let changed = false;
                    const prevBlocks = currentActiveEl.getAttribute('data-blocks');
                    const prevSteps = currentActiveEl.getAttribute('data-steps');
                    const prevFailure = currentActiveEl.getAttribute('data-failure-reason');
                    const prevStatus = currentActiveEl.getAttribute('data-status') || '';

                    const newBlocks = extractBlocksJson(execDto);
                    const newSteps = extractStepsJson(execDto);

                    if (execDto.status && execDto.status !== prevStatus) {
                        currentActiveEl.setAttribute('data-status', execDto.status);
                        currentActiveEl.setAttribute('data-status-raw', execDto.status);
                        changed = true;
                    }
                    if (newBlocks && newBlocks !== prevBlocks) {
                        currentActiveEl.setAttribute('data-blocks', newBlocks);
                        changed = true;
                    }
                    if (newSteps && newSteps !== prevSteps) {
                        currentActiveEl.setAttribute('data-steps', newSteps);
                        changed = true;
                    }
                    if (execDto.localDataBindingsJson) currentActiveEl.setAttribute('data-local-bindings', execDto.localDataBindingsJson);
                    if (execDto.playbookFile) currentActiveEl.setAttribute('data-playbook-file', execDto.playbookFile);
                    if (execDto.testMethod) currentActiveEl.setAttribute('data-test-method', execDto.testMethod);
                    if (execDto.failure) currentActiveEl.setAttribute('data-failure', execDto.failure);
                    if (execDto.failureReason && execDto.failureReason !== prevFailure) {
                        currentActiveEl.setAttribute('data-failure-reason', execDto.failureReason);
                        changed = true;
                    }
                    if (execDto.failureStackTrace) currentActiveEl.setAttribute('data-failure-stack-trace', execDto.failureStackTrace);
                    if (execDto.visualRcaExplanation) currentActiveEl.setAttribute('data-visual-rca-explanation', execDto.visualRcaExplanation);
                    if (execDto.durationMs) currentActiveEl.setAttribute('data-duration-ms', execDto.durationMs);
                    if (execDto.durationFormatted) currentActiveEl.setAttribute('data-duration-formatted', execDto.durationFormatted);
                    if (execDto.startTime) currentActiveEl.setAttribute('data-start-time', execDto.startTime);
                    if (execDto.dateFormatted) currentActiveEl.setAttribute('data-date-formatted', execDto.dateFormatted);
                    if (execDto.timeFormatted) currentActiveEl.setAttribute('data-time-formatted', execDto.timeFormatted);
                    if (execDto.videoUrl) currentActiveEl.setAttribute('data-video-url', execDto.videoUrl);
                    else if (execDto.videoPath) currentActiveEl.setAttribute('data-video-url', execDto.videoPath);
                    if (execDto.llmResponsibilityJson) currentActiveEl.setAttribute('data-llm-responsibility', execDto.llmResponsibilityJson);
                    if (execDto.contextLevelCountsJson) currentActiveEl.setAttribute('data-context-level-counts', execDto.contextLevelCountsJson);

                    const liveLinter = extractFindingsArray(execDto.linterFindings).length > 0
                        ? extractFindingsArray(execDto.linterFindings)
                        : extractFindingsArray(execDto.linterFindingsJson);
                    const liveLinterJson = liveLinter.length > 0 ? JSON.stringify(liveLinter) : '';
                    if ((currentActiveEl.getAttribute('data-linter-findings') || '') !== liveLinterJson) {
                        if (liveLinterJson) currentActiveEl.setAttribute('data-linter-findings', liveLinterJson);
                        else currentActiveEl.removeAttribute('data-linter-findings');
                        changed = true;
                    }

                    const livePostFlight = extractFindingsArray(execDto.postFlightFindings).length > 0
                        ? extractFindingsArray(execDto.postFlightFindings)
                        : extractFindingsArray(execDto.postFlightFindingsJson);
                    const livePostFlightJson = livePostFlight.length > 0 ? JSON.stringify(livePostFlight) : '';
                    if ((currentActiveEl.getAttribute('data-post-flight-findings') || '') !== livePostFlightJson) {
                        if (livePostFlightJson) currentActiveEl.setAttribute('data-post-flight-findings', livePostFlightJson);
                        else currentActiveEl.removeAttribute('data-post-flight-findings');
                        changed = true;
                    }

                    if (changed) {
                        const savedScrollTop = stepListEl ? stepListEl.scrollTop : 0;
                        renderStepsForExecution(currentActiveEl);
                        if (stepListEl && savedScrollTop > 0) {
                            stepListEl.scrollTop = savedScrollTop;
                        }
                    }

                    const updatedSt = (currentActiveEl.getAttribute('data-status') || execDto.status || '').toUpperCase();
                    const updatedStRaw = (currentActiveEl.getAttribute('data-status-raw') || execDto.status || '').toLowerCase();
                    const isTerminal = updatedSt === 'PASSED' || updatedSt === 'FAILED' || updatedSt === 'IGNORED' || updatedSt === 'SKIPPED' || updatedStRaw === 'passed' || updatedStRaw === 'failed' || updatedStRaw === 'ignored';

                    if (isTerminal && !changed) {
                        stopLiveStepRefreshTimer();
                    }
                }
            })
            .catch(() => {});
    }, 1000);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function formatInlineMarkdown(text) {
    if (!text) return '';
    return text
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
        .replace(/\*([^*]+)\*/g, '<em>$1</em>')
        .replace(/`([^`]+)`/g, '<code class="text-mono" style="font-size: 0.85em; background: rgba(0,0,0,0.05); padding: 1px 4px; border-radius: 3px;">$1</code>');
}

function getScoreBadgeClass(score) {
    const s = String(score || '').trim().toUpperCase();
    switch (s) {
        case 'ERROR_PRESENT':
        case 'MISSING':
        case 'STUCK':
        case 'OBSTRUCTED':
        case 'FAIL':
        case 'FAILED':
            return 'pill-fail';
        case 'CLEAN':
        case 'FOUND':
        case 'PROGRESSING':
        case 'CLEAR':
        case 'PASS':
        case 'PASSED':
            return 'pill-pass';
        default:
            return 'pill-pending';
    }
}

function extractRcaFromText(rawText) {
    if (!rawText) return { cleanError: '', rcaText: null };
    const str = String(rawText);

    const marker1 = 'Visual Root Cause Analysis (RCA):';
    const marker2 = '**Root Cause Analysis:**';
    const marker3 = '### Diagnostic Rubrics';

    const idx1 = str.indexOf(marker1);
    const idx2 = str.indexOf(marker2);
    const idx3 = str.indexOf(marker3);

    const validIndices = [idx1, idx2, idx3].filter(idx => idx >= 0);
    if (validIndices.length > 0) {
        const markerIdx = Math.min(...validIndices);
        return {
            cleanError: str.substring(0, markerIdx).trim(),
            rcaText: str.substring(markerIdx).trim()
        };
    }

    return { cleanError: str.trim(), rcaText: null };
}

function parseVisualRca(rcaText) {
    if (!rcaText) return null;
    let text = String(rcaText).trim();

    text = text.replace(/^Visual Root Cause Analysis \(RCA\):\s*/i, '').trim();

    let rootCause = '';
    const rcMatch = text.match(/(?:\*\*Root Cause (?:Analysis|Diagnosis):\*\*|Root Cause Diagnosis:)\s*\n*([\s\S]*?)(?=(?:###\s*Diagnostic Rubrics|-\s*\*\*)|$)/i);
    if (rcMatch) {
        rootCause = rcMatch[1].trim();
    }

    const rubrics = [];
    const rubricDefs = [
        { key: 'targetPresence', name: '🎯 Target Presence Check', pattern: /-\s*\*\*(?:🎯\s*)?Target Presence Check:\*\*\s*`?\[([^\]]+)\]`?\s*([\s\S]*?)(?=(?:-\s*\*\*)|$)/i },
        { key: 'formValidation', name: '📝 Form & Validation Check', pattern: /-\s*\*\*(?:📝\s*)?Form & Validation Check:\*\*\s*`?\[([^\]]+)\]`?\s*([\s\S]*?)(?=(?:-\s*\*\*)|$)/i },
        { key: 'flowState', name: '🧭 Navigation & Flow State', pattern: /-\s*\*\*(?:🧭\s*)?Navigation & Flow State:\*\*\s*`?\[([^\]]+)\]`?\s*([\s\S]*?)(?=(?:-\s*\*\*)|$)/i },
        { key: 'obstruction', name: '🚫 Action Obstruction Check', pattern: /-\s*\*\*(?:🚫\s*)?Action Obstruction Check:\*\*\s*`?\[([^\]]+)\]`?\s*([\s\S]*?)(?=(?:-\s*\*\*)|$)/i }
    ];

    for (const def of rubricDefs) {
        const m = text.match(def.pattern);
        if (m) {
            rubrics.push({
                name: def.name,
                score: m[1].trim(),
                analysis: m[2].trim()
            });
        }
    }

    if (rubrics.length === 0) {
        const genericRegex = /-\s*\*\*([^*]+):\*\*\s*`?\[([^\]]+)\]`?\s*([^\n]+)/g;
        let gm;
        while ((gm = genericRegex.exec(text)) !== null) {
            rubrics.push({
                name: gm[1].trim(),
                score: gm[2].trim(),
                analysis: gm[3].trim()
            });
        }
    }

    if (!rootCause) {
        if (rubrics.length > 0) {
            const rubricHeaderIdx = text.indexOf('### Diagnostic Rubrics');
            if (rubricHeaderIdx > 0) {
                rootCause = text.substring(0, rubricHeaderIdx).replace(/\*\*Root Cause Analysis:\*\*/i, '').trim();
            } else {
                const bulletIdx = text.indexOf('- **');
                if (bulletIdx > 0) {
                    rootCause = text.substring(0, bulletIdx).replace(/\*\*Root Cause Analysis:\*\*/i, '').trim();
                }
            }
        } else {
            rootCause = text;
        }
    }

    return { rootCause, rubrics };
}

function renderVisualRcaHtml(rcaText) {
    const parsed = parseVisualRca(rcaText);
    if (!parsed) return '';

    let html = '<div class="visual-rca-box">';
    html += '<div class="rca-title"><span class="material-symbols-outlined" style="font-size: 1.05rem; vertical-align: middle;">search</span> Visual Root Cause Analysis (RCA)</div>';

    if (parsed.rootCause) {
        html += `<div class="rca-root-cause"><strong>Root Cause Diagnosis:</strong> ${formatInlineMarkdown(escapeHtml(parsed.rootCause))}</div>`;
    }

    if (parsed.rubrics && parsed.rubrics.length > 0) {
        html += '<div class="rubrics-grid">';
        for (const item of parsed.rubrics) {
            const scoreClass = getScoreBadgeClass(item.score);
            html += `
                <div class="rubric-item-card">
                    <div class="rubric-item-header">
                        <span class="rubric-name">${escapeHtml(item.name)}</span>
                        <span class="rubric-score ${scoreClass}">${escapeHtml(item.score)}</span>
                    </div>
                    <div class="rubric-analysis">${formatInlineMarkdown(escapeHtml(item.analysis))}</div>
                </div>
            `;
        }
        html += '</div>';
    } else if (!parsed.rootCause) {
        html += `<div class="rca-root-cause" style="white-space: pre-wrap;">${formatInlineMarkdown(escapeHtml(rcaText))}</div>`;
    }

    html += '</div>';
    return html;
}

function renderErrorBoxContent(failureText, failureReason, visualRcaExplanation, allStackTraces) {
    const rawFailure = (failureReason && failureReason.trim().length > 0) 
        ? failureReason.trim() 
        : (failureText && failureText !== 'NONE' && failureText.trim().length > 0 ? failureText.trim() : '');

    let cleanFailure = rawFailure;
    let effectiveRca = visualRcaExplanation && visualRcaExplanation.trim().length > 0 ? visualRcaExplanation.trim() : '';

    if (rawFailure) {
        const extracted = extractRcaFromText(rawFailure);
        cleanFailure = extracted.cleanError;
        if (!effectiveRca && extracted.rcaText) {
            effectiveRca = extracted.rcaText;
        }
    }

    if (!cleanFailure && !effectiveRca && (!allStackTraces || allStackTraces.length === 0)) {
        return '';
    }

    let html = '';
    if (cleanFailure) {
        const displayFailure = cleanFailure.replace(/^(?:Failure Reason|Error):\s*/i, '');
        html += `<div style="white-space: pre-wrap; word-break: break-word; color: #991b1b; font-family: var(--font-mono); font-size: 0.8rem; margin-bottom: 0.35rem;"><strong>Error:</strong> ${escapeHtml(displayFailure)}</div>`;
    }

    if (effectiveRca) {
        html += renderVisualRcaHtml(effectiveRca);
    }

    if (allStackTraces && allStackTraces.length > 0) {
        const combinedTrace = allStackTraces.join('\n\n');
        html += `
            <details class="error-stacktrace-collapsible">
                <summary class="error-stacktrace-summary">
                    <span class="material-symbols-outlined" style="font-size: 0.95rem;">terminal</span>
                    Failure Stack Trace
                </summary>
                <pre class="error-stacktrace-pre">${escapeHtml(combinedTrace)}</pre>
            </details>
        `;
    }

    return html;
}

function renderLinterFindingsCard(findings, isPostFlight) {
    if (!findings || !Array.isArray(findings) || findings.length === 0) {
        return '';
    }
    const count = findings.length;
    const borderLeftColor = isPostFlight ? '#8b5cf6' : '#6366f1';
    const title = isPostFlight
        ? `🔍 Empirical Playbook Findings (Post-Flight Telemetry) (${count})`
        : `📋 Playbook Quality & Pre-Flight Findings (${count})`;
    const col4Title = isPostFlight ? 'Telemetry & Suggested Rewrite' : 'Message & Suggested Rewrite';
    const rewriteBg = isPostFlight ? 'rgba(139,92,246,0.06)' : 'rgba(99,102,241,0.06)';
    const rewriteBorder = isPostFlight ? '#8b5cf6' : '#6366f1';
    const rewriteLabelColor = isPostFlight ? '#6d28d9' : '#4338ca';
    const rewriteCodeColor = isPostFlight ? '#4c1d95' : '#312e81';

    let rowsHtml = '';
    for (const f of findings) {
        const lineLabel = (f.lineNumber && f.lineNumber > 0) ? `L${f.lineNumber}` : `Step ${f.stepIndex || 0}`;
        const sev = (f.severity || 'INFO').toUpperCase();
        let sevBadge = '';
        if (sev === 'ERROR') {
            sevBadge = '<span class="badge badge-error" style="background:#ef4444;color:#fff;padding:2px 8px;border-radius:4px;font-size:0.75rem;font-weight:600;">ERROR</span>';
        } else if (sev === 'WARNING') {
            sevBadge = '<span class="badge badge-warning" style="background:#f59e0b;color:#fff;padding:2px 8px;border-radius:4px;font-size:0.75rem;font-weight:600;">WARNING</span>';
        } else {
            sevBadge = '<span class="badge badge-info" style="background:#3b82f6;color:#fff;padding:2px 8px;border-radius:4px;font-size:0.75rem;font-weight:600;">INFO</span>';
        }
        const cat = f.category || 'GENERAL';

        let stepTextHtml = '';
        if (f.rawInstruction && f.resolvedInstruction && f.rawInstruction !== f.resolvedInstruction) {
            stepTextHtml = `
                <div style="margin:6px 0;font-size:0.85rem;background:var(--bg-muted,#f1f5f9);padding:6px 10px;border-radius:4px;border-left:3px solid #94a3b8;">
                    <div style="font-size:0.72rem;font-weight:700;color:var(--text-muted,#475569);text-transform:uppercase;letter-spacing:0.5px;margin-bottom:2px;">Template Step:</div>
                    <code style="white-space:pre-wrap;font-weight:500;color:var(--text-main,#1e293b);">${escapeHtml(f.rawInstruction)}</code>
                    <div style="font-size:0.72rem;font-weight:700;color:var(--text-muted,#475569);text-transform:uppercase;letter-spacing:0.5px;margin-top:4px;margin-bottom:2px;">Resolved Step:</div>
                    <code style="white-space:pre-wrap;font-weight:500;color:var(--text-main,#1e293b);">${escapeHtml(f.resolvedInstruction)}</code>
                </div>
            `;
        } else if (f.rawInstruction && f.rawInstruction.trim()) {
            stepTextHtml = `
                <div style="margin:6px 0;font-size:0.85rem;background:var(--bg-muted,#f1f5f9);padding:6px 10px;border-radius:4px;border-left:3px solid #94a3b8;">
                    <div style="font-size:0.72rem;font-weight:700;color:var(--text-muted,#475569);text-transform:uppercase;letter-spacing:0.5px;margin-bottom:2px;">Original Step:</div>
                    <code style="white-space:pre-wrap;font-weight:500;color:var(--text-main,#1e293b);">${escapeHtml(f.rawInstruction)}</code>
                </div>
            `;
        }

        let rewriteHtml = '';
        if (f.suggestedRewrite && f.suggestedRewrite.trim()) {
            rewriteHtml = `
                <div style="margin-top:6px;font-size:0.85rem;background:${rewriteBg};padding:6px 10px;border-radius:4px;border-left:3px solid ${rewriteBorder};">
                    <div style="font-size:0.72rem;font-weight:700;color:${rewriteLabelColor};text-transform:uppercase;letter-spacing:0.5px;margin-bottom:2px;">💡 Suggested Rewrite:</div>
                    <code style="white-space:pre-wrap;font-weight:500;color:${rewriteCodeColor};">${escapeHtml(f.suggestedRewrite)}</code>
                </div>
            `;
        }

        rowsHtml += `
            <tr style="border-bottom: 1px solid var(--border, #e2e8f0);">
                <td style="vertical-align:top; padding: 8px 10px;"><code>${escapeHtml(lineLabel)}</code></td>
                <td style="vertical-align:top; padding: 8px 10px;"><span style="font-weight:600;font-size:0.85rem;">${escapeHtml(cat)}</span></td>
                <td style="vertical-align:top; padding: 8px 10px;">${sevBadge}</td>
                <td style="padding: 8px 10px;">
                    <div style="font-weight:600;margin-bottom:6px;color:var(--text-main,#1e293b);">${escapeHtml(f.message || '')}</div>
                    ${stepTextHtml}
                    ${rewriteHtml}
                </td>
            </tr>
        `;
    }

    return `
        <details open class="diagnostic-box ${isPostFlight ? 'postflight-linter-box' : 'linter-box'}" style="border-left: 4px solid ${borderLeftColor}; background: var(--bg-card, #ffffff); margin-bottom: 16px; padding: 14px 18px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); border: 1px solid var(--border, #e2e8f0); border-left-width: 4px;">
            <summary class="box-header" style="font-size: 0.95rem; font-weight: 700; color: var(--text-main, #1e293b); cursor: pointer; user-select: none; list-style: none; display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center; gap: 8px;"><span>${title}</span></div>
                <span class="linter-toggle-icon" style="font-size: 0.85rem; color: var(--text-muted); transition: transform 0.2s ease;">▼</span>
            </summary>
            <div class="linter-content" style="margin-top: 12px; overflow-x: auto;">
                <table class="data-table" style="width: 100%; border-collapse: collapse; font-size: 0.82rem;">
                    <thead>
                        <tr style="border-bottom: 2px solid var(--border, #cbd5e1); text-align: left; background: var(--bg-hover, #f8fafc);">
                            <th style="padding: 8px 10px; font-weight: 700;">Line / Step</th>
                            <th style="padding: 8px 10px; font-weight: 700;">Category</th>
                            <th style="padding: 8px 10px; font-weight: 700;">Severity</th>
                            <th style="padding: 8px 10px; font-weight: 700;">${col4Title}</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${rowsHtml}
                    </tbody>
                </table>
            </div>
        </details>
    `;
}

function renderStepsForExecution(activeRow) {
    const stepListEl = document.getElementById('sidePageStepList');
    if (!stepListEl) return;

    if (!activeRow) {
        stepListEl.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem; padding: 0.5rem;">Select a test execution row to view steps.</div>';
        const linterContainer = document.getElementById('sidePageLinterFindingsContainer');
        if (linterContainer) {
            linterContainer.innerHTML = '';
            linterContainer.style.display = 'none';
        }
        const postFlightContainer = document.getElementById('sidePagePostFlightFindingsContainer');
        if (postFlightContainer) {
            postFlightContainer.innerHTML = '';
            postFlightContainer.style.display = 'none';
        }
        return;
    }

    startLiveStepRefreshTimer(activeRow);

    const blocksAttr = activeRow.getAttribute('data-blocks');
    const stepsAttr = activeRow.getAttribute('data-steps');
    const stepsFetched = activeRow.hasAttribute('data-steps-fetched');

    if (!stepsFetched && (!blocksAttr || blocksAttr === 'null' || blocksAttr === '{}') && (!stepsAttr || stepsAttr === 'null' || stepsAttr === '{}')) {
        activeRow.setAttribute('data-steps-fetched', 'true');
        const runId = activeRow.getAttribute('data-run-id') || (typeof activeRunId !== 'undefined' ? activeRunId : '#RUN_ID');
        const rowId = activeRow.getAttribute('data-row-id') || activeRow.id;

        if (rowId) {
            stepListEl.innerHTML = '<div style="padding: 1.5rem; text-align: center; color: var(--text-muted);"><span class="material-symbols-outlined" style="font-size: 1.5rem; vertical-align: middle; animation: spin 1s linear infinite;">sync</span> Loading steps and screenshots...</div>';

            fetch(`/fragments/test-side-panel/steps?runId=${encodeURIComponent(runId)}&rowId=${encodeURIComponent(rowId)}`)
                .then(res => {
                    if (!res.ok) throw new Error('Network response was not ok');
                    return res.json();
                })
                .then(execDto => {
                    if (execDto) {
                        const newBlocks = extractBlocksJson(execDto);
                        const newSteps = extractStepsJson(execDto);
                        if (newBlocks) activeRow.setAttribute('data-blocks', newBlocks);
                        if (newSteps) activeRow.setAttribute('data-steps', newSteps);
                        if (execDto.localDataBindingsJson) activeRow.setAttribute('data-local-bindings', execDto.localDataBindingsJson);
                        if (execDto.playbookFile) activeRow.setAttribute('data-playbook-file', execDto.playbookFile);
                        if (execDto.testMethod) activeRow.setAttribute('data-test-method', execDto.testMethod);
                        if (execDto.failure) activeRow.setAttribute('data-failure', execDto.failure);
                        if (execDto.failureReason) activeRow.setAttribute('data-failure-reason', execDto.failureReason);
                        if (execDto.failureStackTrace) activeRow.setAttribute('data-failure-stack-trace', execDto.failureStackTrace);
                        if (execDto.visualRcaExplanation) activeRow.setAttribute('data-visual-rca-explanation', execDto.visualRcaExplanation);
                        if (execDto.durationMs) activeRow.setAttribute('data-duration-ms', execDto.durationMs);
                        if (execDto.durationFormatted) activeRow.setAttribute('data-duration-formatted', execDto.durationFormatted);
                        if (execDto.startTime) activeRow.setAttribute('data-start-time', execDto.startTime);
                        if (execDto.dateFormatted) activeRow.setAttribute('data-date-formatted', execDto.dateFormatted);
                        if (execDto.timeFormatted) activeRow.setAttribute('data-time-formatted', execDto.timeFormatted);
                        if (execDto.videoUrl) activeRow.setAttribute('data-video-url', execDto.videoUrl);
                        else if (execDto.videoPath) activeRow.setAttribute('data-video-url', execDto.videoPath);
                        if (execDto.llmResponsibilityJson) activeRow.setAttribute('data-llm-responsibility', execDto.llmResponsibilityJson);
                        if (execDto.contextLevelCountsJson) activeRow.setAttribute('data-context-level-counts', execDto.contextLevelCountsJson);

                        const linterArray = extractFindingsArray(execDto.linterFindings).length > 0
                            ? extractFindingsArray(execDto.linterFindings)
                            : extractFindingsArray(execDto.linterFindingsJson);
                        if (linterArray.length > 0) {
                            activeRow.setAttribute('data-linter-findings', JSON.stringify(linterArray));
                        } else {
                            activeRow.removeAttribute('data-linter-findings');
                        }

                        const postFlightArray = extractFindingsArray(execDto.postFlightFindings).length > 0
                            ? extractFindingsArray(execDto.postFlightFindings)
                            : extractFindingsArray(execDto.postFlightFindingsJson);
                        if (postFlightArray.length > 0) {
                            activeRow.setAttribute('data-post-flight-findings', JSON.stringify(postFlightArray));
                        } else {
                            activeRow.removeAttribute('data-post-flight-findings');
                        }

                        const startTimeEl = document.getElementById('sidePageTestStartTimeVal');
                        if (startTimeEl) {
                            const rawTs = execDto.timestampMs || execDto.startTime || `${execDto.dateFormatted || ''} ${execDto.timeFormatted || ''}`.trim();
                            const startStr = formatTimestamp(rawTs) || execDto.startTime || '';
                            if (startStr) {
                                startTimeEl.innerText = `Started: ${startStr}`;
                                if (startTimeEl.parentElement) startTimeEl.parentElement.style.display = 'flex';
                            }
                        }

                        renderStepsForExecution(activeRow);
                    } else {
                        renderStepsForExecution(activeRow);
                    }
                })
                .catch(err => {
                    console.error('Error fetching execution details:', err);
                    stepListEl.innerHTML = '<div style="padding: 1rem; color: #ef4444; font-size: 0.85rem;">Failed to load step details for this test execution.</div>';
                });
            return;
        }
    }

    const localBindingsAttr = activeRow.getAttribute('data-local-bindings');
    const playbookFile = activeRow.getAttribute('data-playbook-file') || '';
    const failureText = activeRow.getAttribute('data-failure') || '';

    // Update Top Error Card (Initial check from row data-failure attribute)
    const errorCard = document.getElementById('sidePageErrorDisplayCard');
    const errorTextEl = document.getElementById('sidePageErrorText');
    if (errorCard && errorTextEl) {
        const initialHtml = renderErrorBoxContent(failureText, activeRow.getAttribute('data-failure-reason') || '', activeRow.getAttribute('data-visual-rca-explanation') || '', []);
        if (initialHtml) {
            errorTextEl.innerHTML = initialHtml;
            errorCard.style.display = 'flex';
        } else {
            errorCard.style.display = 'none';
        }
    }

    // Update Dataset Parameters Table
    const paramsTbody = document.getElementById('testDataParamsTbody');
    const playbookFileLabel = document.getElementById('sidePanelPlaybookFileLabel');
    if (playbookFileLabel) playbookFileLabel.innerText = playbookFile || 'Playbook / Test Dataset';

    if (paramsTbody) {
        let paramsHtml = '';
        let localData = {};
        try {
            if (localBindingsAttr) localData = JSON.parse(localBindingsAttr);
        } catch (e) {}

        const keys = Object.keys(localData);
        if (keys.length > 0) {
            keys.forEach(k => {
                paramsHtml += `
                    <tr>
                        <td class="param-key">${k}</td>
                        <td class="param-val">${localData[k]}</td>
                    </tr>
                `;
            });
        } else {
            paramsHtml = `
                <tr>
                    <td class="param-key">location</td>
                    <td class="param-val">${activeRow.getAttribute('data-location') || 'Default'}</td>
                </tr>
                <tr>
                    <td class="param-key">browser</td>
                    <td class="param-val">${activeRow.getAttribute('data-browser') || 'Chrome'}</td>
                </tr>
            `;
        }
        paramsTbody.innerHTML = paramsHtml;
    }

    // Synchronize row AI usage & retrieve fresh metrics
    const aiMetrics = syncRowAiUsage(activeRow);

    // Populate Top Single Run Metrics Grid
    const durMs = activeRow.getAttribute('data-duration-ms');
    const stepsTotal = activeRow.getAttribute('data-steps-total') || '0';
    const healedCount = activeRow.getAttribute('data-healed-count') || '0';
    const failedCount = activeRow.getAttribute('data-failed-count') || '0';
    const llmCalls = aiMetrics ? aiMetrics.llmCalls : (activeRow.getAttribute('data-llm-calls') || '0');
    const totalTokens = aiMetrics ? aiMetrics.totalTokens : (activeRow.getAttribute('data-total-tokens') || '0');
    const inputTokens = aiMetrics ? aiMetrics.inputTokens : (activeRow.getAttribute('data-input-tokens') || '0');
    const outputTokens = aiMetrics ? aiMetrics.outputTokens : (activeRow.getAttribute('data-output-tokens') || '0');
    const cachedTokens = aiMetrics ? aiMetrics.cachedTokens : (activeRow.getAttribute('data-cached-tokens') || '0');
    const estCost = aiMetrics ? aiMetrics.costFormatted : (activeRow.getAttribute('data-est-cost') || '$0.0000');
    let statusVal = activeRow.getAttribute('data-status') || 'PASSED';
    if (statusVal === 'SUCCEEDED_FIXED' || statusVal === 'succeeded-fixed' || statusVal === 'healed' || ((statusVal === 'PASSED' || statusVal === 'passed') && Number(healedCount) > 0)) {
        statusVal = 'HEALED';
    }

    const sideDurEl = document.getElementById('sideMetricDuration');
    const sideStatusEl = document.getElementById('sideMetricStatus');
    const sideStepsEl = document.getElementById('sideMetricSteps');
    const sideStepHealthEl = document.getElementById('sideMetricStepHealth');
    const sideLlmCallsEl = document.getElementById('sideMetricLlmCalls');
    const sideTokensEl = document.getElementById('sideMetricTokens');
    const sideCostEl = document.getElementById('sideMetricCost');
    const sideTokenInOutEl = document.getElementById('sideMetricTokenInOut');

    if (sideDurEl) {
        let durFormatted = '0 min 0 s';
        if (durMs) {
            durFormatted = formatDurationInMinutes(durMs);
        } else {
            const durFormattedAttr = activeRow.getAttribute('data-duration-formatted');
            if (durFormattedAttr) {
                const num = Number(durFormattedAttr);
                durFormatted = !isNaN(num) ? formatDurationInMinutes(num) : durFormattedAttr;
            }
        }
        sideDurEl.innerHTML = `${durFormatted}`;
    }
    if (sideStatusEl) sideStatusEl.innerHTML = `Status: <strong class="${statusVal === 'PASSED' ? 'status-pass' : (statusVal === 'HEALED' || statusVal === 'SUCCEEDED_FIXED' ? 'status-fixed' : (statusVal === 'RUNNING' ? 'status-running' : 'status-fail'))}">${statusVal === 'SUCCEEDED_FIXED' ? 'HEALED' : statusVal}</strong>`;
    if (sideStepsEl) sideStepsEl.innerText = stepsTotal;
    if (sideStepHealthEl) sideStepHealthEl.innerText = `✨ Healed: ${healedCount} | ❌ Failed: ${failedCount}`;
    if (sideLlmCallsEl) sideLlmCallsEl.innerText = llmCalls;
    if (sideTokensEl) sideTokensEl.innerText = `Tokens: ${Number(totalTokens).toLocaleString()} (Cached: ${Number(cachedTokens).toLocaleString()})`;
    if (sideCostEl) sideCostEl.innerText = estCost.startsWith('$') ? estCost : `$${Number(estCost).toFixed(4)}`;
    if (sideTokenInOutEl) sideTokenInOutEl.innerText = `In: ${Number(inputTokens).toLocaleString()} | Out: ${Number(outputTokens).toLocaleString()}`;

    // Populate AI LLM Responsibility & Token Accounting Table
    const llmTbody = document.getElementById('sidePageLlmAccountingTbody');
    if (llmTbody) {
        const responsibilityAttr = activeRow.getAttribute('data-llm-responsibility');
        renderLlmResponsibilityRows(llmTbody, responsibilityAttr, {
            totalCalls: Number(llmCalls) || 0,
            totalTokens: Number(totalTokens) || 0,
            inputTokens: Number(inputTokens) || 0,
            outputTokens: Number(outputTokens) || 0,
            cachedTokens: Number(cachedTokens) || 0,
            cost: Number(String(estCost).replace('$', '')) || 0
        });

        // Open the table by default so the user sees the breakdown without an extra click.
        const llmBox = document.getElementById('sidePageLlmAccountingBox');
        if (llmBox) llmBox.classList.add('active');
        const llmContainer = document.getElementById('sidePageLlmTableContainer');
        const llmChevron = document.getElementById('sidePageLlmTableChevron');
        if (llmContainer && llmContainer.style.display === 'none') {
            llmContainer.style.display = 'block';
            if (llmChevron) llmChevron.innerText = 'expand_less';
        }
    }

    const ctxBadgesEl = document.getElementById('sidePageContextLevelBadges');
    if (ctxBadgesEl) {
        const countsAttr = activeRow.getAttribute('data-context-level-counts');
        renderContextLevelBadges(ctxBadgesEl, countsAttr, Number(stepsTotal) || 0, activeRow);
    }

    // Extract Blocks
    let blocksObj = null;
    try {
        if (blocksAttr && blocksAttr !== '{}') blocksObj = JSON.parse(blocksAttr);
    } catch (e) {}

    // Fallback to steps.tries if blocks missing
    if (!blocksObj || (!blocksObj.before && !blocksObj.steps && !blocksObj.after)) {
        let stepsObj = null;
        try {
            if (stepsAttr && stepsAttr !== '{}') stepsObj = JSON.parse(stepsAttr);
        } catch (e) {}

        if (stepsObj && stepsObj.tries) {
            const try1 = stepsObj.tries['1'] || Object.values(stepsObj.tries)[0];
            if (try1) {
                blocksObj = {
                    before: try1.beforeSteps || [],
                    steps: try1.coreSteps || [],
                    after: try1.afterSteps || []
                };
            }
        }
    }

    const beforeSteps = blocksObj ? (blocksObj.before || []) : [];
    const coreSteps = blocksObj ? (blocksObj.steps || []) : [];
    const afterSteps = blocksObj ? (blocksObj.after || []) : [];

    const stVal = (activeRow.getAttribute('data-status') || '').toUpperCase();
    const stRawVal = (activeRow.getAttribute('data-status-raw') || '').toLowerCase();
    const isRunningState = stVal === 'RUNNING' || stRawVal === 'running' || stRawVal === 'in_progress' || stRawVal === 'executing' || stRawVal === 'pending';

    const failureReason = activeRow.getAttribute('data-failure-reason') || blocksObj?.failureReason || '';
    const failureStackTrace = activeRow.getAttribute('data-failure-stack-trace') || blocksObj?.failureStackTrace || '';
    const visualRcaExplanation = activeRow.getAttribute('data-visual-rca-explanation') || blocksObj?.visualRcaExplanation || '';

    if (errorCard && errorTextEl) {
        const allStackTraces = [];
        if (failureStackTrace && failureStackTrace.trim().length > 0) {
            allStackTraces.push(failureStackTrace.trim());
        }

        const renderedHtml = renderErrorBoxContent(failureText, failureReason, visualRcaExplanation, allStackTraces);
        if (renderedHtml) {
            errorTextEl.innerHTML = renderedHtml;
            errorCard.style.display = 'flex';
        } else {
            errorCard.style.display = 'none';
        }
    }

    // Render Pre-Flight Findings Box
    const linterContainer = document.getElementById('sidePageLinterFindingsContainer');
    if (linterContainer) {
        const linterFindings = extractFindingsArray(activeRow.getAttribute('data-linter-findings'));
        if (linterFindings.length > 0) {
            linterContainer.innerHTML = renderLinterFindingsCard(linterFindings, false);
            linterContainer.style.display = 'block';
        } else {
            linterContainer.innerHTML = '';
            linterContainer.style.display = 'none';
        }
    }

    // Render Empirical Post-Flight Findings Box
    const postFlightContainer = document.getElementById('sidePagePostFlightFindingsContainer');
    if (postFlightContainer) {
        const postFlightFindings = extractFindingsArray(activeRow.getAttribute('data-post-flight-findings'));
        if (postFlightFindings.length > 0) {
            postFlightContainer.innerHTML = renderLinterFindingsCard(postFlightFindings, true);
            postFlightContainer.style.display = 'block';
        } else {
            postFlightContainer.innerHTML = '';
            postFlightContainer.style.display = 'none';
        }
    }

    if (!blocksObj || (beforeSteps.length === 0 && coreSteps.length === 0 && afterSteps.length === 0)) {
        const hasExistingRenderedSteps = stepListEl.querySelector('.step-item') !== null;
        if (hasExistingRenderedSteps) {
            return;
        }
        if (isRunningState) {
            stepListEl.innerHTML = `
                <div style="padding: 2rem 1rem; text-align: center; color: var(--text-muted); background: var(--bg-card); border-radius: var(--radius-lg); border: 1px dashed var(--border-color); margin-top: 0.5rem;">
                    <span class="material-symbols-outlined text-accent" style="font-size: 1.8rem; vertical-align: middle; animation: spin 1.2s linear infinite; margin-bottom: 0.5rem; display: block;">sync</span>
                    <strong style="color: var(--text-main);">Test execution in progress...</strong>
                    <div style="font-size: 0.8rem; margin-top: 0.35rem; color: var(--text-muted);">Waiting for initial step logs and screenshots from SUT.</div>
                </div>
            `;
            return;
        } else {
            stepListEl.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem; padding: 0.5rem;">No step execution data recorded for this run.</div>';
            return;
        }
    }

    let html = '';

    function renderSection(sectionTitle, stepsArray, sectionClass, defaultIcon, accentBorderColor) {
        if (!stepsArray || stepsArray.length === 0) return '';

        let secHtml = `
            <div class="step-section expanded">
                <div class="step-section-header" onclick="toggleStepSection(this)" style="border-left-color: ${accentBorderColor};">
                    <div class="step-section-title">
                        <span class="material-symbols-outlined text-accent">${defaultIcon}</span>
                        <span>${sectionTitle} (${stepsArray.length})</span>
                    </div>
                    <span class="material-symbols-outlined step-section-chevron">chevron_right</span>
                </div>
                <div class="step-section-body">
        `;

        let hasEncounteredFailure = false;
        stepsArray.forEach((s, idx) => {
            const stepNum = s.index || s.stepNum || (idx + 1);
            const title = s.instruction || s.name || s.title || `Step ${stepNum}`;
            let rawStatus = (s.status || (s.passed === false ? 'failed' : 'passed')).toLowerCase();
            const subsList = Array.isArray(s.subSteps) ? s.subSteps : (Array.isArray(s.sub_steps) ? s.sub_steps : []);
            if (subsList.length > 0) {
                const executedSubs = subsList.filter(sub => sub.status && String(sub.status).toUpperCase() !== 'PENDING' && String(sub.status).toUpperCase() !== 'SKIPPED');
                const hasFailedSub = subsList.some(sub => String(sub.status || '').toUpperCase() === 'FAILED' || sub.failureReason || sub.error || sub.failure);
                if (hasFailedSub) {
                    rawStatus = 'failed';
                } else if (executedSubs.length > 0) {
                    const lastSub = executedSubs[executedSubs.length - 1];
                    const lastSubStatus = String(lastSub.status || '').toUpperCase();
                    if (lastSubStatus === 'PASSED' || lastSubStatus === 'SUCCESS' || lastSubStatus === 'HEALED') {
                        rawStatus = 'passed';
                    } else if (lastSubStatus === 'FAILED') {
                        rawStatus = 'failed';
                    } else {
                        rawStatus = lastSubStatus.toLowerCase();
                    }
                }
            }
            if (hasEncounteredFailure && !isRunningState) {
                rawStatus = 'skipped';
            }
            if (rawStatus === 'failed' || s.passed === false || s.error || s.failure) {
                if (!s.optional && !s.continueOnError) {
                    hasEncounteredFailure = true;
                }
            }
            const status = rawStatus;
            const isHealed = status === 'healed' || status === 'succeeded-fixed' || status === 'fixed' || s.healed === true || (Array.isArray(s.actions) && s.actions.some(a => a.healed));
            const isPassed = !isHealed && (status === 'passed' || status === 'passed-clean' || status === 'success' || s.passed === true);
            const isFailed = status === 'failed' || status === 'failed-unknown' || status === 'failed-known' || s.passed === false;
            
            const stepClass = isHealed ? 'step-healed' : (isPassed ? 'step-passed' : (isFailed ? 'step-failed' : 'step-ignored'));
            const stepId = `stepCard_${sectionClass}_${idx}`;
            const rawContextLevel = s.contextLevel || s.contextLevels || s.stats?.contextLevels || s.stats?.contextLevel || 'PENDING';
            const isUnexecutedOrSkipped = !isPassed && !isFailed && status !== 'running';
            const contextLevel = isUnexecutedOrSkipped ? 'PENDING' : rawContextLevel;

            // Format Timing
            const rawStart = s.startTimestamp || s.startTime || s.startTimeMs;
            let formattedStart = '';
            if (rawStart && !isUnexecutedOrSkipped) {
                if (typeof rawStart === 'number') {
                    formattedStart = new Date(rawStart).toLocaleTimeString();
                } else if (typeof rawStart === 'string') {
                    formattedStart = rawStart;
                }
            }

            let rawDuration = s.duration !== undefined && s.duration !== null ? s.duration : (s.durationMs !== undefined ? s.durationMs : null);
            let formattedDuration = '0 min 0 s';
            if (isUnexecutedOrSkipped) {
                formattedDuration = '-';
            } else if (rawDuration !== null && rawDuration !== undefined) {
                formattedDuration = formatDurationInMinutes(rawDuration);
            }

            // Format Reasoning
            let reasoningHtml = '';
            if (s.reasoning) {
                reasoningHtml = `
                    <div class="reasoning-card" style="margin-top: 0.5rem;">
                        <div class="reasoning-title">💡 AI Reasoning & Decisioning</div>
                        <div class="reasoning-body">${s.reasoning}</div>
                    </div>
                `;
            }

            // Format Screenshot
            let screenshotHtml = '';
            const screenshotSources = [];

            function formatScreenshotSrc(rawSrc, mediaType) {
                if (!rawSrc) return '';
                if (rawSrc.startsWith('data:') || rawSrc.startsWith('http://') || rawSrc.startsWith('https://')) {
                    return rawSrc;
                }
                if (rawSrc.includes('/') && (rawSrc.endsWith('.png') || rawSrc.endsWith('.jpg') || rawSrc.endsWith('.jpeg') || rawSrc.endsWith('.webp'))) {
                    return rawSrc.startsWith('/') ? rawSrc : `/storage/${rawSrc}`;
                }
                const mime = mediaType || 'image/jpeg';
                return `data:${mime};base64,${rawSrc}`;
            }

            // Format Screenshot (Ignore top-level transient screenshot, rely strictly on s.screenshots array)
            if (Array.isArray(s.screenshots) && s.screenshots.length > 0) {
                s.screenshots.forEach((sc, scIdx) => {
                    let fullSrc = '';
                    if (sc.base64Data) {
                        const mime = sc.mediaType || 'image/jpeg';
                        fullSrc = sc.base64Data.startsWith('data:') ? sc.base64Data : `data:${mime};base64,${sc.base64Data}`;
                    } else {
                        const rawSrc = sc.src || sc.url || sc.path || sc.screenshot;
                        fullSrc = formatScreenshotSrc(rawSrc, sc.mediaType);
                    }

                    if (fullSrc && !screenshotSources.some(item => item.src === fullSrc)) {
                        screenshotSources.push({
                            src: fullSrc,
                            label: sc.label || sc.name || `Capture #${scIdx + 1}`
                        });
                    }
                });
            }

            if (screenshotSources.length > 0) {
                screenshotHtml = `
                    <div class="step-card-screenshots-preview" style="margin-top: 0.6rem;">
                        <div style="font-weight: 700; font-size: 0.78rem; margin-bottom: 0.3rem; color: var(--text-muted); display: flex; align-items: center; gap: 0.3rem;">
                            <span class="material-symbols-outlined" style="font-size: 0.95rem;">image</span>
                            <span>Step Screenshots (${screenshotSources.length})</span>
                        </div>
                        <div style="display: flex; gap: 0.6rem; flex-wrap: wrap;">
                `;
                screenshotSources.forEach(sc => {
                    const safeLabel = (sc.label || 'Screenshot').replace(/'/g, "\\'");
                    screenshotHtml += `
                        <div style="display: flex; flex-direction: column; align-items: center; background: var(--bg-card, #ffffff); border: 1px solid var(--border); border-radius: 6px; padding: 0.3rem; box-shadow: 0 1px 3px rgba(0,0,0,0.06);">
                            <img src="${sc.src}" class="preview-thumb" alt="${sc.label}" title="Click to enlarge" onclick="event.stopPropagation(); openImageModal(this.src, '${safeLabel}')">
                            <span style="font-size: 0.7rem; font-weight: 500; color: var(--text-muted); margin-top: 0.25rem;">${sc.label}</span>
                        </div>
                    `;
                });
                screenshotHtml += `</div></div>`;
            }

            // Format Visual SSIM Compare
            let ssimHtml = '';
            const baselineSrc = formatScreenshotSrc(s.baselineMatrixPng, 'image/png');
            const replaySrc = formatScreenshotSrc(s.replayMatrixPng, 'image/png');
            if (baselineSrc || replaySrc || s.ssimScore != null || s.ssimMinScore != null || s.screenshotHashDim) {
                const scoreVal = (typeof s.ssimScore === 'number') ? s.ssimScore : null;
                const scorePct = scoreVal != null ? (scoreVal * 100).toFixed(2) + '%' : 'N/A';
                const scoreClass = scoreVal != null && scoreVal >= 0.95 ? 'badge-pass'
                    : scoreVal != null && scoreVal >= 0.80 ? 'badge-healed'
                    : 'badge-unknown-fail';
                const minScoreVal = (typeof s.ssimMinScore === 'number') ? s.ssimMinScore : null;
                const minScorePct = minScoreVal != null ? (minScoreVal * 100).toFixed(2) + '%' : null;
                const dimLabel = s.screenshotHashDim != null ? String(s.screenshotHashDim) : null;
                const dimSuffix = dimLabel ? ` (${escapeHtml(dimLabel)}x${escapeHtml(dimLabel)})` : '';
                ssimHtml = `
                    <div class="step-ssim-card" style="margin-top: 0.5rem; border: 1px solid var(--border); border-radius: 10px; background: var(--bg-card); padding: 0.65rem 0.85rem; display: flex; flex-direction: column; gap: 0.5rem;">
                        <div style="font-size: 0.72rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.06em; color: #475569; display: flex; align-items: center; gap: 0.45rem; flex-wrap: wrap;">
                            <span class="material-symbols-outlined" style="font-size: 0.95rem;">compare</span>
                            <span>Visual SSIM Compare</span>
                            <span class="badge-status ${scoreClass}" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;">SSIM ${scorePct}</span>
                            ${minScorePct != null ? `<span class="badge-status" style="padding: 0.15rem 0.4rem; font-size: 0.7rem; background: #fef3c7; color: #92400e; border: 1px solid #fde68a;">Min ${minScorePct}</span>` : ''}
                        </div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.6rem;">
                            ${baselineSrc ? `
                                <figure style="margin: 0; display: flex; flex-direction: column; align-items: center; gap: 0.25rem;">
                                    <img src="${baselineSrc}" class="preview-thumb" alt="Baseline matrix" title="Click to enlarge" onclick="event.stopPropagation(); openImageModal(this.src, 'Baseline Matrix')" style="max-width: 100%; border-radius: 6px; border: 1px solid var(--border);">
                                    <figcaption style="font-size: 0.7rem; color: var(--text-muted); font-weight: 600;">Baseline${dimSuffix}</figcaption>
                                </figure>
                            ` : ''}
                            ${replaySrc ? `
                                <figure style="margin: 0; display: flex; flex-direction: column; align-items: center; gap: 0.25rem;">
                                    <img src="${replaySrc}" class="preview-thumb" alt="Replay matrix" title="Click to enlarge" onclick="event.stopPropagation(); openImageModal(this.src, 'Replay Matrix')" style="max-width: 100%; border-radius: 6px; border: 1px solid var(--border);">
                                    <figcaption style="font-size: 0.7rem; color: var(--text-muted); font-weight: 600;">Replay${dimSuffix}</figcaption>
                                </figure>
                            ` : ''}
                        </div>
                    </div>
                `;
            }

            // Format Actions
            let actionsHtml = '';
            const actions = s.actions || [];
            if (actions.length > 0) {
                actionsHtml = `
                    <div class="actions-performed-box" style="margin-top: 0.5rem;">
                        <div class="actions-performed-title">
                            <span class="material-symbols-outlined" style="font-size: 0.85rem; color: #7e22ce;">terminal</span>
                            <span>Actions Performed (${actions.length})</span>
                        </div>
                        <div style="display: flex; flex-direction: column; gap: 0.4rem;">
                `;
                actions.forEach(act => {
                    const actName = act.type || act.name || 'ACTION';
                    const actTarget = act.resolvedTarget || act.target || '';
                    const actVal = (act.resolvedValue || act.value) ? ` ➔ ${act.resolvedValue || act.value}` : '';
                    const actDesc = act.description ? ` (${act.description})` : '';
                    const hasSuccess = typeof act.success === 'boolean';
                    const successModifier = hasSuccess ? (act.success ? ' action-success' : ' action-failed') : '';
                    const successBadge = hasSuccess
                        ? `<span class="action-status-badge ${act.success ? 'action-status-ok' : 'action-status-fail'}" title="${act.success ? 'Action succeeded' : 'Action failed'}"><span class="material-symbols-outlined">${act.success ? 'check_circle' : 'cancel'}</span></span>`
                        : '';
                    const actReasoning = act.reasoning
                        ? `<div class="action-reasoning"><span class="material-symbols-outlined">psychology</span><span class="action-reasoning-text">${act.reasoning}</span></div>`
                        : '';
                    const origTarget = act.expectedTarget || act.originalTarget || act.healedFrom || (act.healed && act.target && act.target !== actTarget ? act.target : null);
                    let healedBadge = '';
                    if (origTarget && origTarget !== actTarget) {
                        healedBadge = `<div class="action-healed-from" style="width: 100%; display: flex; align-items: center; gap: 0.35rem; margin-top: 0.25rem; font-size: 0.78rem; color: #7e22ce;"><span>🧬 Healed from:</span> <span style="display: inline-block; padding: 0.1rem 0.4rem; background: #f3e8ff; border: 1px solid #d8b4fe; border-radius: 4px; text-decoration: line-through; color: #7e22ce; font-family: monospace;">${escapeHtml(origTarget)}</span></div>`;
                    }

                    let subStepBadge = '';
                    if (act.subStepInstruction || (act.subStepIndex !== undefined && act.subStepIndex !== null)) {
                        const subNum = (act.subStepIndex !== undefined && act.subStepIndex !== null)
                            ? `#${stepNum}.${act.subStepIndex + 1}`
                            : '';
                        const subText = act.subStepInstruction ? escapeHtml(act.subStepInstruction) : '';
                        subStepBadge = `<span class="substep-attribution-pill" style="display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; padding: 1px 6px; border-radius: 4px; background: #e0e7ff; color: #3730a3; border: 1px solid #c7d2fe; font-weight: 600;" title="${subText}">${subNum ? `${subNum} ` : ''}${subText}</span>`;
                    }

                    actionsHtml += `
                        <div class="action-item-pill-row${successModifier}">
                            ${successBadge}
                            ${subStepBadge}
                            <span class="action-type-pill">${actName}</span>
                            <span class="action-code-text">${escapeHtml(actTarget)}${escapeHtml(actVal)}${escapeHtml(actDesc)}</span>
                            ${healedBadge}
                            ${actReasoning}
                        </div>
                    `;
                });
                actionsHtml += `</div></div>`;
            }

            // Format Error Details
            let errorBoxHtml = '';
            if (s.error || s.failure) {
                errorBoxHtml = `
                    <div class="step-error-box-top" style="margin-top: 0.4rem;">
                        <div class="error-box-top-header">
                            <span><span class="material-symbols-outlined">warning</span> Step Error Details</span>
                            <span class="tag-chip" style="background: #fef2f2; color: #dc2626; border-color: #fecaca;">FAILED</span>
                        </div>
                        <pre class="error-box-top-text">${s.error || s.failure}</pre>
                    </div>
                `;
            }

            // Format Sub-steps
            let subStepsHtml = '';
            const subSteps = s.subSteps || s.sub_steps || [];
            if (subSteps.length > 0) {
                subStepsHtml = `
                    <div class="sub-steps-container" style="margin-top: 0.5rem;">
                        <div class="sub-steps-header">SUB-STEPS (${subSteps.length})</div>
                `;
                subSteps.forEach((sub, subIdx) => {
                    const subNum = `#${stepNum}.${subIdx + 1}`;
                    const subTitle = sub.instruction || sub.action || `Sub-step ${subIdx + 1}`;
                    const subDur = (sub.durationMs !== undefined && sub.durationMs !== null) ? formatDurationInMinutes(sub.durationMs) : (sub.duration ? formatDurationInMinutes(sub.duration) : '');
                    const subStatus = (sub.status || 'SUCCESS').toUpperCase();
                    const subBadgeClass = (subStatus === 'PASSED' || subStatus === 'SUCCESS') ? 'badge-pass'
                        : subStatus === 'HEALED' ? 'badge-healed'
                        : subStatus === 'FAILED' ? 'badge-unknown-fail' : 'badge-ignored';

                    // Source line info
                    const subSource = sub.sourceFile ? `📄 ${sub.sourceFile}${sub.lineNumber > 0 ? ':L' + sub.lineNumber : ''}` : '';

                    // Error Box
                    let subErrorHtml = '';
                    if (sub.failureReason || sub.error || sub.failure) {
                        subErrorHtml = `
                            <div class="step-error-box-top" style="margin-top: 0.3rem;">
                                <div class="error-box-top-header">
                                    <span><span class="material-symbols-outlined">warning</span> Sub-Step Error Details</span>
                                    <span class="tag-chip" style="background: #fef2f2; color: #dc2626; border-color: #fecaca;">FAILED</span>
                                </div>
                                <pre class="error-box-top-text">${escapeHtml(sub.failureReason || sub.error || sub.failure)}</pre>
                            </div>
                        `;
                    }

                    // Reasoning
                    let subReasoningHtml = '';
                    if (sub.reasoning || (Array.isArray(sub.reasonings) && sub.reasonings.length > 0)) {
                        const rText = sub.reasoning || sub.reasonings.join('\n\n');
                        subReasoningHtml = `
                            <div class="action-reasoning" style="margin-top: 0.3rem; padding: 0.4rem 0.6rem; background: #fdf4ff; border-left: 3px solid #a855f7; border-radius: 4px;">
                                <span class="material-symbols-outlined" style="color:#a855f7; font-size: 0.9rem;">psychology</span>
                                <span style="font-size: 0.78rem; color: var(--text-main); font-weight: 500;">${escapeHtml(rText)}</span>
                            </div>
                        `;
                    }

                    // Screenshots
                    let subScreenshotsHtml = '';
                    const subScList = Array.isArray(sub.screenshots) ? sub.screenshots : [];
                    if (subScList.length > 0) {
                        subScreenshotsHtml = `
                            <div style="margin-top: 0.4rem;">
                                <div style="font-size: 0.72rem; font-weight: 700; color: var(--text-muted); margin-bottom: 0.2rem; display: flex; align-items: center; gap: 0.25rem;">
                                    <span class="material-symbols-outlined" style="font-size: 0.85rem;">image</span>
                                    <span>Sub-Step Screenshots (${subScList.length})</span>
                                </div>
                                <div style="display: flex; gap: 0.4rem; flex-wrap: wrap;">
                        `;
                        subScList.forEach(sc => {
                            const scSrc = formatScreenshotSrc(sc.base64Data || sc.data || sc.url || sc.src, sc.mediaType || 'image/png');
                            const scName = sc.name || sc.label || 'Screenshot';
                            const safeLabel = scName.replace(/'/g, "\\'");
                            if (scSrc) {
                                subScreenshotsHtml += `
                                    <div style="display: flex; flex-direction: column; align-items: center; background: var(--bg-card); border: 1px solid var(--border); border-radius: 4px; padding: 0.2rem;">
                                        <img src="${scSrc}" class="preview-thumb" alt="${scName}" title="Click to enlarge" onclick="event.stopPropagation(); openImageModal(this.src, '${safeLabel}')" style="max-height: 80px;">
                                        <span style="font-size: 0.68rem; color: var(--text-muted); margin-top: 0.2rem;">${scName}</span>
                                    </div>
                                `;
                            }
                        });
                        subScreenshotsHtml += `</div></div>`;
                    }

                    // Actions
                    let subActionsHtml = '';
                    const subActList = Array.isArray(sub.actions) ? sub.actions : [];
                    if (subActList.length > 0) {
                        subActionsHtml = `
                            <div class="actions-performed-box" style="margin-top: 0.4rem;">
                                <div class="actions-performed-title">
                                    <span class="material-symbols-outlined" style="font-size: 0.85rem; color: #7e22ce;">terminal</span>
                                    <span>Actions (${subActList.length})</span>
                                </div>
                                <div style="display: flex; flex-direction: column; gap: 0.3rem;">
                        `;
                        subActList.forEach(act => {
                            const actName = act.type || act.name || 'ACTION';
                            const actTarget = act.resolvedTarget || act.target || '';
                            const actVal = (act.resolvedValue || act.value) ? ` ➔ ${act.resolvedValue || act.value}` : '';
                            const hasSuccess = typeof act.success === 'boolean';
                            const successModifier = hasSuccess ? (act.success ? ' action-success' : ' action-failed') : '';
                            const successBadge = hasSuccess
                                ? `<span class="action-status-badge ${act.success ? 'action-status-ok' : 'action-status-fail'}" title="${act.success ? 'Action succeeded' : 'Action failed'}"><span class="material-symbols-outlined">${act.success ? 'check_circle' : 'cancel'}</span></span>`
                                : '';
                            const origTarget = act.expectedTarget || act.originalTarget || act.healedFrom || (act.healed && act.target && act.target !== actTarget ? act.target : null);
                            let healedBadge = '';
                            if (origTarget && origTarget !== actTarget) {
                                healedBadge = `<div class="action-healed-from" style="width: 100%; display: flex; align-items: center; gap: 0.35rem; margin-top: 0.25rem; font-size: 0.78rem; color: #7e22ce;"><span>🧬 Healed from:</span> <span style="display: inline-block; padding: 0.1rem 0.4rem; background: #f3e8ff; border: 1px solid #d8b4fe; border-radius: 4px; text-decoration: line-through; color: #7e22ce; font-family: monospace;">${escapeHtml(origTarget)}</span></div>`;
                            }
                            subActionsHtml += `
                                <div class="action-item-pill-row${successModifier}">
                                    ${successBadge}
                                    <span class="action-type-pill">${actName}</span>
                                    <span class="action-code-text">${escapeHtml(actTarget)}${escapeHtml(actVal)}</span>
                                    ${healedBadge}
                                </div>
                            `;
                        });
                        subActionsHtml += `</div></div>`;
                    }

                    // LLM Calls
                    let subLlmCallsHtml = '';
                    const subLlmList = Array.isArray(sub.llmCalls) ? sub.llmCalls : [];
                    if (subLlmList.length > 0) {
                        subLlmCallsHtml = `
                            <div style="margin-top: 0.4rem;">
                                <div style="font-size: 0.72rem; font-weight: 700; color: var(--text-muted); margin-bottom: 0.2rem; display: flex; align-items: center; gap: 0.25rem;">
                                    <span class="material-symbols-outlined" style="font-size: 0.85rem;">smart_toy</span>
                                    <span>LLM Invocations (${subLlmList.length})</span>
                                </div>
                                <div style="display: flex; flex-direction: column; gap: 0.3rem;">
                        `;
                        subLlmList.forEach((call, cIdx) => {
                            const cap = call.capability || 'LLM Call';
                            const model = call.modelName || call.model || call.llmModel || 'default';
                            const tokens = extractCallTokens(call);
                            const rawCost = extractCallCost(call);
                            let formattedCost = '$0.0000';
                            if (rawCost > 0) {
                                const roundedUp = Math.ceil(rawCost * 10000) / 10000;
                                formattedCost = '$' + roundedUp.toFixed(4);
                            }
                            const dur = (call.durationMs !== undefined && call.durationMs !== null) 
                                ? (call.durationMs + ' ms') 
                                : (call.duration ? formatDurationInMinutes(call.duration) : '');
                            
                            const systemPromptText = call.systemPrompt || call.system_prompt || call.system || '';
                            const userPromptText = call.userPrompt || call.user_prompt || call.domContext || call.dom_context || call.prompt || '';
                            const rawResponseText = call.responseContent || call.response_content || call.rawResponse || call.raw_response || call.response || call.output || call.completion || '';

                            const metaParts = [];
                            if (tokens) metaParts.push(`Tokens: ${tokens}`);
                            if (rawCost > 0) metaParts.push(`Cost: ${formattedCost}`);
                            if (dur) metaParts.push(dur);
                            const callMetaStr = metaParts.join(' | ');

                            subLlmCallsHtml += `
                                <div class="llm-call-card" style="border: 1px solid var(--border); border-radius: 6px; margin-top: 0.3rem; overflow: hidden; background: var(--bg-card);">
                                    <div class="llm-call-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');" style="background: var(--bg-panel); padding: 0.35rem 0.6rem; display: flex; justify-content: space-between; align-items: center; font-size: 0.75rem; cursor: pointer; user-select: none;">
                                        <div style="display: flex; align-items: center; gap: 0.35rem; font-weight: 700; color: #1e293b;">
                                            <span class="material-symbols-outlined llm-subsection-chevron" style="font-size: 0.8rem;">chevron_right</span>
                                            <span class="material-symbols-outlined" style="font-size: 0.85rem; color: #a855f7;">smart_toy</span>
                                            <span><strong>${escapeHtml(cap)}</strong> <span style="color:var(--text-muted); font-weight: 400;">(${escapeHtml(model)})</span></span>
                                        </div>
                                        <span style="font-family: var(--font-mono); font-size: 0.7rem; color: #64748b;">${callMetaStr}</span>
                                    </div>
                                    <div class="llm-call-body" style="padding: 0.5rem; display: flex; flex-direction: column; gap: 0.4rem;">
                                        
                                        <div class="llm-subsection-card">
                                            <div class="llm-subsection-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');">
                                                <div class="llm-subsection-title">
                                                    <span class="material-symbols-outlined llm-subsection-chevron">chevron_right</span>
                                                    <span class="material-symbols-outlined" style="font-size: 0.85rem; color: #0284c7;">tune</span>
                                                    <span>System Prompt</span>
                                                </div>
                                                <span style="font-size: 0.68rem; color: #94a3b8; font-weight: 500;">Instruction & Persona</span>
                                            </div>
                                            <div class="llm-subsection-body">${systemPromptText ? escapeHtml(systemPromptText) : '<em style="color: var(--text-muted);">No explicit system prompt recorded</em>'}</div>
                                        </div>

                                        <div class="llm-subsection-card">
                                            <div class="llm-subsection-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');">
                                                <div class="llm-subsection-title">
                                                    <span class="material-symbols-outlined llm-subsection-chevron">chevron_right</span>
                                                    <span class="material-symbols-outlined" style="font-size: 0.85rem; color: #d97706;">code</span>
                                                    <span>User Prompt & DOM Context (Plain Text)</span>
                                                </div>
                                                <span style="font-size: 0.68rem; color: #94a3b8; font-weight: 500;">User Query & Page Snapshot</span>
                                            </div>
                                            <div class="llm-subsection-body">${userPromptText ? escapeHtml(userPromptText) : '<em style="color: var(--text-muted);">No user prompt/DOM context recorded</em>'}</div>
                                        </div>

                                        <div class="llm-subsection-card">
                                            <div class="llm-subsection-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');">
                                                <div class="llm-subsection-title">
                                                    <span class="material-symbols-outlined llm-subsection-chevron">chevron_right</span>
                                                    <span class="material-symbols-outlined" style="font-size: 0.85rem; color: #16a34a;">terminal</span>
                                                    <span>Raw Model Response</span>
                                                </div>
                                                <span style="font-size: 0.68rem; color: #94a3b8; font-weight: 500;">AI Completion Payload</span>
                                            </div>
                                            <div class="llm-subsection-body">${rawResponseText ? escapeHtml(rawResponseText) : '<em style="color: var(--text-muted);">No raw response recorded</em>'}</div>
                                        </div>

                                    </div>
                                </div>
                            `;
                        });
                        subLlmCallsHtml += `</div></div>`;
                    }

                    subStepsHtml += `
                        <div class="sub-step-card" onclick="toggleSubStepInspector(this, event)">
                            <div class="sub-step-card-header">
                                <div style="display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap;">
                                    <span class="sub-step-number">${subNum}</span>
                                    <span class="badge-status ${subBadgeClass}" style="font-size: 0.65rem; padding: 0.1rem 0.35rem;">${subStatus}</span>
                                    <span class="sub-step-instruction">${escapeHtml(subTitle)}</span>
                                </div>
                                <div style="display: flex; align-items: center; gap: 0.4rem;">
                                    ${subDur ? `<span style="font-size: 0.72rem; color: var(--text-muted); font-family: var(--font-mono);">${subDur}</span>` : ''}
                                    <span class="material-symbols-outlined sub-step-chevron">chevron_right</span>
                                </div>
                            </div>
                            <div class="sub-step-inspector">
                                ${subSource ? `<div style="font-size: 0.72rem; color: var(--text-muted); font-family: var(--font-mono);">${subSource}</div>` : ''}
                                ${subErrorHtml}
                                ${subReasoningHtml}
                                ${subScreenshotsHtml}
                                ${subActionsHtml}
                                ${subLlmCallsHtml}
                            </div>
                        </div>
                    `;
                });
                subStepsHtml += `</div>`;
            }

            // Build LLM Details Tab Content (calculate totals from calls array, simplified stats-summary-grid)
            const stats = s.stats || {};
            const calls = s.llmCalls || [];

            let calcTotalTokens = 0;
            let calcTotalCostNum = 0;
            
            if (calls.length > 0) {
                calls.forEach(call => {
                    calcTotalTokens += extractCallTokens(call);
                    calcTotalCostNum += extractCallCost(call);
                });
            } else if (s.stats) {
                calcTotalTokens = Number(s.stats.totalTokens || 0);
                calcTotalCostNum = extractCallCost(s.stats);
            }

            const calcTotalCostFormatted = calcTotalCostNum > 0 
                ? '$' + (Math.ceil(calcTotalCostNum * 10000) / 10000).toFixed(4)
                : '$0.0000';

            let statsHtml = '';
            if (s.stats || s.llmCalls) {
                statsHtml = `
                    <div class="stats-summary-grid" style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 0.75rem; margin-bottom: 0.75rem; background: var(--bg-card); padding: 0.75rem 1rem; border-radius: 6px; border: 1px solid var(--border);">
                        <div><span style="font-size: 0.75rem; color: var(--text-muted); font-weight: 600;">Total Tokens Usage</span><br><strong style="font-size: 1.1rem; color: var(--text-main); font-family: var(--font-mono);">${calcTotalTokens}</strong></div>
                        <div><span style="font-size: 0.75rem; color: var(--text-muted); font-weight: 600;">Estimated Cost</span><br><strong style="font-size: 1.1rem; color: #0284c7; font-family: var(--font-mono);">${calcTotalCostFormatted}</strong></div>
                    </div>
                `;
            }

            function escapeHtml(str) {
                if (!str) return '';
                return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
            }

            let llmCallsHtml = '';
            if (calls.length > 0) {
                calls.forEach((call, cIdx) => {
                    const model = call.modelName || call.model || call.llmModel || 'LLM Model';
                    const tokens = extractCallTokens(call);
                    const rawCost = extractCallCost(call);
                    let formattedCost = '$0.0000';
                    if (rawCost > 0) {
                        const roundedUp = Math.ceil(rawCost * 10000) / 10000;
                        formattedCost = '$' + roundedUp.toFixed(4);
                    }
                    const dur = (call.durationMs !== undefined && call.durationMs !== null) ? formatDurationInMinutes(call.durationMs) : (call.duration ? formatDurationInMinutes(call.duration) : '');
                    
                    const systemPromptText = call.systemPrompt || call.system_prompt || call.system || '';
                    const userPromptText = call.userPrompt || call.user_prompt || call.domContext || call.dom_context || call.prompt || '';
                    const rawResponseText = call.responseContent || call.response_content || call.rawResponse || call.raw_response || call.response || call.output || call.completion || '';

                    const metaParts = [];
                    if (tokens) metaParts.push(`Tokens: ${tokens}`);
                    metaParts.push(`Cost: ${formattedCost}`);
                    if (dur) metaParts.push(`Duration: ${dur}`);
                    const callMetaStr = metaParts.join(' | ');

                    llmCallsHtml += `
                        <div class="llm-call-card expanded" style="border: 1px solid var(--border); border-radius: 8px; margin-top: 0.6rem; overflow: hidden; background: var(--bg-card);">
                            <div class="llm-call-header" onclick="this.parentElement.classList.toggle('expanded')" style="background: #f1f5f9; padding: 0.5rem 0.75rem; display: flex; justify-content: space-between; align-items: center; font-size: 0.8rem; cursor: pointer; user-select: none;">
                                <div style="display: flex; align-items: center; gap: 0.4rem; font-weight: 700; color: #1e293b;">
                                    <span class="material-symbols-outlined llm-subsection-chevron" style="font-size: 0.85rem;">chevron_right</span>
                                    <span class="material-symbols-outlined" style="font-size: 0.95rem; color: #a855f7;">smart_toy</span>
                                    <span>Call #${cIdx + 1}: ${model}</span>
                                </div>
                                <span style="font-family: var(--font-mono); font-size: 0.72rem; color: #64748b;">${callMetaStr}</span>
                            </div>
                            <div class="llm-call-body" style="padding: 0.6rem; display: flex; flex-direction: column; gap: 0.5rem;">
                                
                                <div class="llm-subsection-card">
                                    <div class="llm-subsection-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');">
                                        <div class="llm-subsection-title">
                                            <span class="material-symbols-outlined llm-subsection-chevron">chevron_right</span>
                                            <span class="material-symbols-outlined" style="font-size: 0.9rem; color: #0284c7;">tune</span>
                                            <span>System Prompt</span>
                                        </div>
                                        <span style="font-size: 0.7rem; color: #94a3b8; font-weight: 500;">Instruction & Persona</span>
                                    </div>
                                    <div class="llm-subsection-body">${systemPromptText ? escapeHtml(systemPromptText) : '<em style="color: var(--text-muted);">No explicit system prompt recorded</em>'}</div>
                                </div>

                                <div class="llm-subsection-card">
                                    <div class="llm-subsection-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');">
                                        <div class="llm-subsection-title">
                                            <span class="material-symbols-outlined llm-subsection-chevron">chevron_right</span>
                                            <span class="material-symbols-outlined" style="font-size: 0.9rem; color: #d97706;">code</span>
                                            <span>User Prompt & DOM Context (Plain Text)</span>
                                        </div>
                                        <span style="font-size: 0.7rem; color: #94a3b8; font-weight: 500;">User Query & Page Snapshot</span>
                                    </div>
                                    <div class="llm-subsection-body">${userPromptText ? escapeHtml(userPromptText) : '<em style="color: var(--text-muted);">No user prompt/DOM context recorded</em>'}</div>
                                </div>

                                <div class="llm-subsection-card">
                                    <div class="llm-subsection-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');">
                                        <div class="llm-subsection-title">
                                            <span class="material-symbols-outlined llm-subsection-chevron">chevron_right</span>
                                            <span class="material-symbols-outlined" style="font-size: 0.9rem; color: #16a34a;">terminal</span>
                                            <span>Raw Model Response</span>
                                        </div>
                                        <span style="font-size: 0.7rem; color: #94a3b8; font-weight: 500;">AI Completion Payload</span>
                                    </div>
                                    <div class="llm-subsection-body">${rawResponseText ? escapeHtml(rawResponseText) : '<em style="color: var(--text-muted);">No raw response recorded</em>'}</div>
                                </div>

                            </div>
                        </div>
                    `;
                });
            } else if (s.prompt || s.llmPrompt || s.llmResponse) {
                const promptSnippet = s.prompt || s.llmPrompt || `[System Instruction for Step #${stepNum}]\nExecute: ${title}`;
                const responseSnippet = s.llmResponse || s.response || JSON.stringify(actions, null, 2);
                llmCallsHtml = `
                    <div class="llm-call-card expanded" style="border: 1px solid var(--border); border-radius: 8px; margin-top: 0.6rem; overflow: hidden; background: var(--bg-card);">
                        <div class="llm-call-header" onclick="this.parentElement.classList.toggle('expanded')" style="background: #f1f5f9; padding: 0.5rem 0.75rem; display: flex; justify-content: space-between; align-items: center; font-size: 0.8rem; cursor: pointer; user-select: none;">
                            <div style="display: flex; align-items: center; gap: 0.4rem; font-weight: 700; color: #1e293b;">
                                <span class="material-symbols-outlined llm-subsection-chevron" style="font-size: 0.85rem;">chevron_right</span>
                                <span class="material-symbols-outlined" style="font-size: 0.95rem; color: #a855f7;">smart_toy</span>
                                <span>LLM Prompt & Response</span>
                            </div>
                        </div>
                        <div class="llm-call-body" style="padding: 0.6rem; display: flex; flex-direction: column; gap: 0.5rem;">
                            <div class="llm-subsection-card">
                                <div class="llm-subsection-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');">
                                    <div class="llm-subsection-title">
                                        <span class="material-symbols-outlined llm-subsection-chevron">chevron_right</span>
                                        <span class="material-symbols-outlined" style="font-size: 0.9rem; color: #d97706;">code</span>
                                        <span>User Prompt & DOM Context (Plain Text)</span>
                                    </div>
                                </div>
                                <div class="llm-subsection-body">${escapeHtml(promptSnippet)}</div>
                            </div>
                            <div class="llm-subsection-card">
                                <div class="llm-subsection-header" onclick="event.stopPropagation(); this.parentElement.classList.toggle('expanded');">
                                    <div class="llm-subsection-title">
                                        <span class="material-symbols-outlined llm-subsection-chevron">chevron_right</span>
                                        <span class="material-symbols-outlined" style="font-size: 0.9rem; color: #16a34a;">terminal</span>
                                        <span>Raw Model Response</span>
                                    </div>
                                </div>
                                <div class="llm-subsection-body">${escapeHtml(responseSnippet)}</div>
                            </div>
                        </div>
                    </div>
                `;
            } else {
                llmCallsHtml = `<div style="font-size: 0.8rem; color: var(--text-muted); padding: 0.5rem;">No LLM call details recorded for this step.</div>`;
            }

            // Per-step totals (number of actions, screenshots and LLM calls) from console-log.json step data.
            const stepActionsCount = isUnexecutedOrSkipped ? 0 : (Array.isArray(s.actions) ? s.actions.length : 0);
            const stepScreenshotsCount = isUnexecutedOrSkipped ? 0 : screenshotSources.length;
            const stepLlmCallsCount = isUnexecutedOrSkipped ? 0 : (Array.isArray(calls) ? calls.length : 0);
            let stepInTokens = 0;
            let stepOutTokens = 0;
            if (Array.isArray(calls)) {
                for (const c of calls) {
                    stepInTokens += Number(c.inputTokens || c.input_tokens || 0) || 0;
                    stepOutTokens += Number(c.outputTokens || c.output_tokens || 0) || 0;
                }
            }
            if (stepInTokens === 0 && stepOutTokens === 0) {
                stepInTokens = Number(s.standardInputTokens || s.standard_input_tokens || 0) || 0;
                stepOutTokens = Number(s.standardOutputTokens || s.standard_output_tokens || 0) || 0;
            }
            const tokensSuffix = (stepInTokens > 0 || stepOutTokens > 0)
                ? `, ${stepInTokens.toLocaleString('en-US')} in/ ${stepOutTokens.toLocaleString('en-US')} out`
                : '';
            const hasSsim = s.baselineMatrixPng || s.replayMatrixPng || s.ssimScore != null || s.ssimMinScore != null || s.screenshotHashDim;
            const countsSummaryHtml = `
                <div class="step-exec-stats" style="display: flex; align-items: center; gap: 0.45rem; flex-wrap: wrap; margin-top: 0.25rem;">
                    <span class="step-stat-chip" style="display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; color: var(--text-muted); background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; padding: 0.15rem 0.5rem;"><span class="material-symbols-outlined" style="font-size: 0.8rem; color: #7e22ce;">terminal</span> ${stepActionsCount} action(s)</span>
                    <span class="step-stat-chip" style="display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; color: var(--text-muted); background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; padding: 0.15rem 0.5rem;"><span class="material-symbols-outlined" style="font-size: 0.8rem; color: #2563eb;">image</span> ${stepScreenshotsCount} screenshot(s)</span>
                    <span class="step-stat-chip" style="display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; color: var(--text-muted); background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; padding: 0.15rem 0.5rem;"><span class="material-symbols-outlined" style="font-size: 0.8rem; color: #a855f7;">smart_toy</span> ${stepLlmCallsCount} LLM call(s)${tokensSuffix}</span>
                    ${hasSsim ? `<span class="step-stat-chip" style="display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; color: #0f766e; background: #f0fdfa; border: 1px solid #99f6e4; border-radius: 10px; padding: 0.15rem 0.5rem;"><span class="material-symbols-outlined" style="font-size: 0.8rem; color: #0d9488;">compare</span> Visual SSIM</span>` : ''}
                </div>
            `;

            secHtml += `
                <div class="step-item ${stepClass}" id="${stepId}" onclick="toggleStepActionInspector(this)">
                    <div class="step-header">
                        <div class="step-title-line" style="display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap;">
                            <span class="step-num-box">#${stepNum}</span>
                            ${isHealed ? '<span class="badge-status badge-healed" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;"><span class="material-symbols-outlined" style="font-size: 0.85rem; vertical-align: middle;">healing</span> HEALED</span>' :
                              (isPassed ? '<span class="badge-status badge-pass" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;">SUCCESS</span>' :
                              (isFailed ? '<span class="badge-status badge-unknown-fail" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;">FAILED</span>' :
                              '<span class="badge-status badge-ignored" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;">SKIPPED</span>'))}
                            <span class="context-badge ${contextLevelClass(contextLevel)}">${contextLevel}</span>
                            <span style="color: var(--text-main); font-weight: 600; margin-left: 0.2rem;">${title}</span>
                        </div>
                        ${countsSummaryHtml}
                        <div class="flex-gap-2">
                            <span style="font-size: 0.75rem; font-family: var(--font-mono); color: var(--text-muted);">${formattedDuration}</span>
                            <span class="material-symbols-outlined step-expand-chevron">chevron_right</span>
                        </div>
                    </div>

                    <div style="padding: 0 0.85rem 0.5rem;">
                        <div style="font-size: 0.75rem; color: var(--text-muted); font-family: var(--font-mono); margin-top: 0.2rem;">📄 ${s.sourceMeta || s.playbookSource || s.file +':L'+ s.line || 'Playbook.yaml:L' + stepNum}</div>
                    </div>

                    <div class="step-action-inspector" onclick="event.stopPropagation()">
                        <div class="inspector-card">
                            <div class="inspector-header">
                                <div style="display: flex; justify-content: space-between; align-items: center;">
                                    <span style="font-family: var(--font-mono); font-weight: 800; color: var(--status-fixed); font-size: 0.9rem;">#${stepNum}</span>
                                    <span class="badge-status ${isHealed ? 'badge-healed' : (isPassed ? 'badge-pass' : 'badge-unknown-fail')}" style="font-size: 0.7rem;">${isHealed ? 'HEALED' : (isPassed ? 'PASSED' : 'FAILED')}</span>
                                </div>
                                <div style="font-size: 0.95rem; font-weight: 700; color: var(--text-main); margin-top: 0.2rem;">${title}</div>
                            </div>
                            <div class="inspector-tabs">
                                <button class="tab-btn active" onclick="switchInspectorTab(this, '${stepId}_tabOverview')">📋 Overview</button>
                                <button class="tab-btn" onclick="switchInspectorTab(this, '${stepId}_tabLlm')">🤖 LLM Details</button>
                            </div>
                            <div class="inspector-content">
                                <!-- TAB 1: OVERVIEW -->
                                <div class="tab-panel active" id="${stepId}_tabOverview">
                                    <div style="display: flex; flex-direction: column; gap: 0.4rem; font-size: 0.8rem; background: var(--bg-card); padding: 0.5rem; border-radius: 6px; border: 1px solid var(--border); margin-bottom: 0.5rem;">
                                        <div><strong>Instruction:</strong> ${title}</div>
                                        <div><strong>Duration:</strong> ${formattedDuration}</div>
                                        <div><strong>Start Timestamp:</strong> ${formattedStart || 'N/A'}</div>
                                    </div>
                                    ${reasoningHtml}
                                    ${errorBoxHtml}
                                    ${screenshotHtml}
                                    ${ssimHtml}
                                    ${actionsHtml}
                                    ${subStepsHtml}
                                </div>

                                <!-- TAB 2: LLM DETAILS -->
                                <div class="tab-panel" id="${stepId}_tabLlm">
                                    ${statsHtml}
                                    ${llmCallsHtml}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });

        secHtml += `
                </div>
            </div>
        `;
        return secHtml;
    }

    const testMethod = resolveTestMethod(activeRow, blocksObj);
    let coreSectionTitle = 'CORE EXECUTION STEPS';
    if (testMethod && testMethod.trim().length > 0) {
        const cleanMethod = testMethod.trim();
        const formattedMethod = cleanMethod.endsWith('()') ? cleanMethod : `${cleanMethod}()`;
        coreSectionTitle = `CORE EXECUTION STEPS &mdash; <span style="font-family: var(--font-mono); font-weight: normal; font-size: 0.85em; text-transform: none; color: var(--text-secondary);">${escapeHtml(formattedMethod)}</span>`;
    }

    html += renderSection('BEFORE STEPS / SETUP', beforeSteps, 'before', 'play_arrow', 'var(--status-fixed)');
    html += renderSection(coreSectionTitle, coreSteps, 'core', 'checklist', 'var(--status-pass)');
    html += renderSection('AFTER STEPS / TEARDOWN', afterSteps, 'after', 'stop', 'var(--text-muted)');

    stepListEl.innerHTML = html;

    const videoUrlAttr = activeRow.getAttribute('data-video-url') || activeRow.getAttribute('data-video');
    const attachmentContainer = document.getElementById('attachmentSectionContainer');
    const videoPreviewBox = document.getElementById('attachmentVideoBox') || (attachmentContainer ? attachmentContainer.querySelector('.attachment-video-mock')?.closest('.attachment-preview-box') : null);

    const hasVideo = videoUrlAttr && videoUrlAttr.trim().length > 0 && videoUrlAttr !== 'null';

    if (hasVideo && attachmentContainer) {
        if (videoPreviewBox) {
            videoPreviewBox.style.display = 'flex';
        }
        const runId = activeRow.getAttribute('data-run-id') || (typeof activeRunId !== 'undefined' ? activeRunId : '');
        const testClass = activeRow.getAttribute('data-test-class') || activeRow.getAttribute('data-test-name') || activeRow.getAttribute('data-class-name') || '';
        const src = (videoUrlAttr.startsWith('http') || videoUrlAttr.startsWith('/'))
            ? videoUrlAttr
            : `/api/runs/${encodeURIComponent(runId)}/${encodeURIComponent(testClass)}/${encodeURIComponent(videoUrlAttr)}`;

        const videoBox = videoPreviewBox ? videoPreviewBox.querySelector('.attachment-video-mock') : attachmentContainer.querySelector('.attachment-video-mock');
        if (videoBox) {
            videoBox.style.height = 'auto';
            videoBox.style.padding = '0.5rem';
            videoBox.innerHTML = `<video controls style="width:100%; max-height:280px; border-radius:6px; background:#000;" src="${src}"></video>`;
        }
        const tagChip = videoPreviewBox ? videoPreviewBox.querySelector('.tag-chip') : null;
        if (tagChip) {
            tagChip.textContent = videoUrlAttr;
        }
        const openBtn = videoPreviewBox ? videoPreviewBox.querySelector('button[onclick*="openAttachmentWindow"]') : null;
        if (openBtn) {
            openBtn.setAttribute('onclick', `openAttachmentWindow('video', '${src}')`);
        }
    } else {
        if (videoPreviewBox) {
            videoPreviewBox.style.display = 'none';
        }
    }

    if (attachmentContainer) {
        const visibleAttachments = attachmentContainer.querySelectorAll('.attachment-preview-box:not([style*="display: none"])');
        if (visibleAttachments.length === 0) {
            attachmentContainer.style.display = 'none';
        } else {
            attachmentContainer.style.display = 'block';
            const fileCountChip = attachmentContainer.querySelector('.step-section-header .tag-chip');
            if (fileCountChip) {
                fileCountChip.textContent = `${visibleAttachments.length} FILE${visibleAttachments.length > 1 ? 'S' : ''}`;
            }
        }
    }
}

function openAttachmentWindow(type, url) {
    if (!url) return;
    let targetUrl = url;
    if (!targetUrl.startsWith('http://') && !targetUrl.startsWith('https://') && !targetUrl.startsWith('/api/')) {
        const activeRow = document.querySelector('.execution-row.selected, .clickable-row.selected, tr.execution-row:hover, tr.clickable-row');
        if (activeRow) {
            const runId = activeRow.getAttribute('data-run-id') || (typeof activeRunId !== 'undefined' ? activeRunId : '');
            const testClass = activeRow.getAttribute('data-test-class') || activeRow.getAttribute('data-test-name') || activeRow.getAttribute('data-class-name') || '';
            const filename = activeRow.getAttribute('data-video-url') || url;
            if (filename.startsWith('http') || filename.startsWith('/')) {
                targetUrl = filename;
            } else {
                targetUrl = `/api/runs/${encodeURIComponent(runId)}/${encodeURIComponent(testClass)}/${encodeURIComponent(filename)}`;
            }
        }
    }
    window.open(targetUrl, '_blank', 'width=1000,height=700,resizable=yes,scrollbars=yes');
}
window.openAttachmentWindow = openAttachmentWindow;

function extractStepContextLevelCounts(activeRow) {
    if (!activeRow) return null;
    const blocksAttr = activeRow.getAttribute('data-blocks');
    const stepsAttr = activeRow.getAttribute('data-steps');
    let stepsList = [];

    try {
        if (blocksAttr && blocksAttr !== '{}') {
            const blocks = JSON.parse(blocksAttr);
            const before = blocks.before || [];
            const steps = blocks.steps || [];
            const after = blocks.after || [];
            stepsList = [...before, ...steps, ...after];
        }
    } catch (e) {}

    if (stepsList.length === 0) {
        try {
            if (stepsAttr && stepsAttr !== '{}') {
                const stepsObj = JSON.parse(stepsAttr);
                if (stepsObj && stepsObj.tries) {
                    const try1 = stepsObj.tries['1'] || Object.values(stepsObj.tries)[0];
                    if (Array.isArray(try1)) {
                        stepsList = try1;
                    } else if (try1 && typeof try1 === 'object') {
                        stepsList = [...(try1.beforeSteps || []), ...(try1.coreSteps || []), ...(try1.afterSteps || [])];
                    }
                } else if (Array.isArray(stepsObj)) {
                    stepsList = stepsObj;
                }
            }
        } catch (e) {}
    }

    if (stepsList.length === 0) return null;

    const counts = {};
    function processStep(s) {
        if (!s) return;
        const st = (s.status || '').toUpperCase();
        if (st === 'SKIPPED' || st === 'PENDING' || st === 'UNEXECUTED') return;

        const rawContextLevel = s.contextLevel || s.contextLevels || s.stats?.contextLevels;
        if (rawContextLevel) {
            let highestLevel = null;
            if (Array.isArray(rawContextLevel)) {
                const validLevels = rawContextLevel.filter(lvl => lvl && typeof lvl === 'string' && lvl.trim().toUpperCase() !== 'PENDING');
                if (validLevels.length > 0) {
                    highestLevel = validLevels[validLevels.length - 1].trim().toUpperCase();
                }
            } else if (typeof rawContextLevel === 'string') {
                const parts = rawContextLevel.split(/\s*(?:→|->)\s*/).map(p => p.trim().toUpperCase()).filter(p => p && p !== 'PENDING');
                if (parts.length > 0) {
                    highestLevel = parts[parts.length - 1];
                }
            }

            if (highestLevel) {
                counts[highestLevel] = (counts[highestLevel] || 0) + 1;
            }
        }

        if (Array.isArray(s.subSteps)) {
            s.subSteps.forEach(processStep);
        }
    }

    stepsList.forEach(processStep);

    return Object.keys(counts).length > 0 ? counts : null;
}

function renderContextLevelBadges(ctxBadgesEl, countsJson, stepsTotal, activeRow) {
    let counts = activeRow ? extractStepContextLevelCounts(activeRow) : null;
    if (!counts) {
        counts = parseContextLevelCounts(countsJson);
    }

    const defaultLevels = ['MINIMAL', 'LEAN', 'STANDARD', 'VISUAL'];
    const mergedCounts = {};

    defaultLevels.forEach(lvl => {
        mergedCounts[lvl] = 0;
    });

    if (counts) {
        Object.keys(counts).forEach(key => {
            const normalizedKey = key.toUpperCase();
            mergedCounts[normalizedKey] = Number(counts[key]) || 0;
        });
    }

    ctxBadgesEl.innerHTML = Object.keys(mergedCounts).map(key => {
        const cls = contextLevelClass(key);
        const count = mergedCounts[key];
        return `<span class="context-badge ${cls}">${key}: <strong>${count}</strong></span>`;
    }).join('');
}

function parseContextLevelCounts(json) {
    if (!json) return null;
    try {
        const obj = JSON.parse(json);
        if (!obj || typeof obj !== 'object' || Object.keys(obj).length === 0) return null;
        return obj;
    } catch (e) {
        return null;
    }
}

function contextLevelClass(key) {
    const raw = String(key || '').trim();
    const parts = raw.split(/\s*(?:→|->)\s*/);
    const k = (parts[parts.length - 1] || '').toUpperCase();
    if (k === 'MINIMAL') return 'level-minimal';
    if (k === 'LEAN') return 'level-lean';
    if (k === 'STANDARD') return 'level-standard';
    if (k.indexOf('VISUAL') === 0) return 'level-visual';
    if (k === 'PENDING') return 'level-pending';
    return 'level-standard';
}

window.renderContextLevelBadges = renderContextLevelBadges;
window.parseContextLevelCounts = parseContextLevelCounts;
window.contextLevelClass = contextLevelClass;

function renderLlmResponsibilityRows(llmTbody, responsibilityJson, fallback) {
    const labels = [
        { key: 'total',        label: 'Total',                                                       total: true  },
        { key: 'action',       label: 'Action (Standard Generation)',                                total: false },
        { key: 'pesap',        label: 'PESAP (Pre-Execution Semantic Anchor)',                        total: false },
        { key: 'judge',        label: 'Self-Judging Validation',                                      total: false },
        { key: 'verification', label: 'Semantic Outcome Verification',                                total: false },
        { key: 'visualRca',    label: 'Visual Root Cause Analysis (RCA)',                             total: false }
    ];
    const buckets = parseResponsibilityBuckets(responsibilityJson);
    if (buckets) {
        llmTbody.innerHTML = labels.map(({ key, label, total }) => {
            const b = buckets[key] || { calls: 0, inputTokens: 0, outputTokens: 0, cachedTokens: 0, totalTokens: 0, estimatedCostUsd: 0 };
            return renderResponsibilityRow(label, b, total);
        }).join('');
    } else {
        llmTbody.innerHTML = renderHeuristicLlmRows(fallback);
    }
}

function parseResponsibilityBuckets(json) {
    if (!json) return null;
    try {
        const obj = JSON.parse(json);
        if (!obj || typeof obj !== 'object') return null;
        const required = ['total', 'action', 'pesap', 'judge', 'verification', 'visualRca'];
        for (let i = 0; i < required.length; i++) {
            if (!obj[required[i]] || typeof obj[required[i]] !== 'object') return null;
        }
        const normalized = {};
        for (let i = 0; i < required.length; i++) {
            const k = required[i];
            const src = obj[k];
            normalized[k] = {
                calls:           Number(src.calls)           || 0,
                inputTokens:     Number(src.inputTokens)     || 0,
                outputTokens:    Number(src.outputTokens)    || 0,
                cachedTokens:    Number(src.cachedTokens)    || 0,
                totalTokens:     Number(src.totalTokens)     || 0,
                estimatedCostUsd:Number(src.estimatedCostUsd)|| 0
            };
        }
        return normalized;
    } catch (e) {
        return null;
    }
}

function renderResponsibilityRow(label, bucket, isTotal) {
    const rowStyle = isTotal
        ? 'font-weight: 700; background: var(--bg-hover);'
        : '';
    const costText = '$' + (Number(bucket.estimatedCostUsd) || 0).toFixed(4);
    return `
        <tr style="${rowStyle}">
            <td style="padding: 0.35rem 0.6rem;">${label}</td>
            <td style="text-align: center; padding: 0.35rem 0.6rem;">${(bucket.calls || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(bucket.totalTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(bucket.inputTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(bucket.outputTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(bucket.cachedTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;" class="text-accent">${costText}</td>
        </tr>
    `;
}

function renderHeuristicLlmRows(fallback) {
    const f = fallback || { totalCalls: 0, totalTokens: 0, inputTokens: 0, outputTokens: 0, cachedTokens: 0, cost: 0 };
    const cost = Number(f.cost) || 0;
    return `
        <tr style="font-weight: 700; background: var(--bg-hover);">
            <td style="padding: 0.35rem 0.6rem;">Total</td>
            <td style="text-align: center; padding: 0.35rem 0.6rem;">${(f.totalCalls || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(f.totalTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(f.inputTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(f.outputTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(f.cachedTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;" class="text-accent">$${cost.toFixed(4)}</td>
        </tr>
        <tr>
            <td style="padding: 0.35rem 0.6rem;">Action (Standard Generation)</td>
            <td style="text-align: center; padding: 0.35rem 0.6rem;">${Math.ceil((f.totalCalls || 0) * 0.5)}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${Math.floor((f.totalTokens || 0) * 0.92).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${Math.floor((f.inputTokens || 0) * 0.92).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${Math.floor((f.outputTokens || 0) * 0.92).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${(f.cachedTokens || 0).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;" class="text-accent">$${(cost * 0.92).toFixed(4)}</td>
        </tr>
        <tr>
            <td style="padding: 0.35rem 0.6rem;">PESAP (Pre-Execution Semantic Anchor)</td>
            <td style="text-align: center; padding: 0.35rem 0.6rem;">${Math.floor((f.totalCalls || 0) * 0.5)}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${Math.floor((f.totalTokens || 0) * 0.08).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${Math.floor((f.inputTokens || 0) * 0.08).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">${Math.floor((f.outputTokens || 0) * 0.08).toLocaleString()}</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;">0</td>
            <td style="text-align: right; padding: 0.35rem 0.6rem;" class="text-accent">$${(cost * 0.08).toFixed(4)}</td>
        </tr>
    `;
}

window.renderLlmResponsibilityRows = renderLlmResponsibilityRows;
window.parseResponsibilityBuckets = parseResponsibilityBuckets;
window.renderResponsibilityRow = renderResponsibilityRow;
window.renderHeuristicLlmRows = renderHeuristicLlmRows;

function toggleSidePanelLlmTable() {
    const container = document.getElementById('sidePageLlmTableContainer');
    const chevron = document.getElementById('sidePageLlmTableChevron');
    if (container) {
        const isHidden = container.style.display === 'none';
        container.style.display = isHidden ? 'block' : 'none';
        if (chevron) chevron.innerText = isHidden ? 'expand_less' : 'expand_more';
    }
}

function switchInspectorTab(btn, tabId) {
    const card = btn.closest('.inspector-card');
    if (!card) return;

    card.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    card.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));

    btn.classList.add('active');
    const targetPanel = document.getElementById(tabId);
    if (targetPanel) targetPanel.classList.add('active');
}

function toggleStepActionInspector(stepCardEl) {
    if (stepCardEl) {
        stepCardEl.classList.toggle('inspector-active');
    }
}

function closeTestSidePagePanel() {
    stopLiveStepRefreshTimer();
    const panel = document.getElementById('testSidePagePanel');
    const resizer = document.getElementById('allurePanelResizer') || document.getElementById('panelResizer');
    const varDrawer = document.getElementById('variationHistoryDrawer');
    const varResizer = document.getElementById('variationHistoryResizer');

    if (panel) panel.classList.remove('active');
    if (resizer) resizer.classList.remove('active');
    if (varDrawer) varDrawer.classList.remove('active');
    if (varResizer) varResizer.classList.remove('active');

    currentActiveRowId = null;

    document.querySelectorAll('tr.clickable-row').forEach(r => {
        r.classList.remove('selected', 'active-selected-row');
        const marker = r.querySelector('.active-row-marker');
        if (marker) marker.remove();
    });
}

window.toggleSidePanelLlmTable = toggleSidePanelLlmTable;
window.switchInspectorTab = switchInspectorTab;

function setActiveNavBtn(btn) {
    document.querySelectorAll('#mainSidebar .nav-item-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
}

// Multiselect Dropdown Toggles, Ratio Bar Filtering & Click Outside
document.addEventListener('click', (e) => {
    const ratioEl = e.target.closest('[data-ratio-type]');
    if (ratioEl) {
        const type = ratioEl.getAttribute('data-ratio-type');
        const val = ratioEl.getAttribute('data-filter-val');
        if (type && val) {
            e.preventDefault();
            e.stopPropagation();
            filterFromRatioBar(type, val, e.ctrlKey || e.metaKey);
            return;
        }
    }

function revertScopeMenuCheckboxes(menu) {
    if (!menu) return;
    delete menu.dataset.pendingSelection;
    const optionChks = menu.querySelectorAll('.js-scope-chk');
    if (optionChks.length === 0) return;
    optionChks.forEach(cb => {
        if (cb.hasAttribute('data-initial-checked')) {
            cb.checked = cb.getAttribute('data-initial-checked') === 'true';
        }
    });
    const allChk = menu.querySelector('.js-scope-chk-all');
    if (allChk) {
        allChk.checked = Array.from(optionChks).every(cb => cb.checked);
    }
}

document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        const activeScopeMenu = e.target.closest?.('.run-scope-menu');
        if (activeScopeMenu) {
            const applyBtn = activeScopeMenu.querySelector('.btn-apply-scope-filter');
            if (applyBtn) {
                e.preventDefault();
                applyBtn.click();
            }
        }
    }
});

    const toggleBtn = e.target.closest('.js-multiselect-toggle');
    if (toggleBtn) {
        e.stopPropagation();
        const menuId = toggleBtn.getAttribute('data-menu-id');
        const menu = document.getElementById(menuId);
        if (menu) {
            const isOpen = menu.classList.contains('show') || menu.classList.contains('active');
            document.querySelectorAll('.multiselect-menu').forEach(m => {
                if (m.classList.contains('show') || m.classList.contains('active')) {
                    revertScopeMenuCheckboxes(m);
                }
                m.classList.remove('show', 'active');
            });
            document.querySelectorAll('.multiselect-dropdown').forEach(d => d.classList.remove('active'));
            if (!isOpen) {
                menu.classList.add('show', 'active');
                toggleBtn.closest('.multiselect-dropdown')?.classList.add('active');
            }
        }
        return;
    }

    if (!e.target.closest('.multiselect-dropdown')) {
        document.querySelectorAll('.multiselect-menu').forEach(m => {
            if (m.classList.contains('show') || m.classList.contains('active')) {
                revertScopeMenuCheckboxes(m);
            }
            m.classList.remove('show', 'active');
        });
        document.querySelectorAll('.multiselect-dropdown').forEach(d => d.classList.remove('active'));
    }
});

// Multiselect Checkbox Changes
document.addEventListener('change', (e) => {
    const chk = e.target;
    if (chk.matches('.js-chk-all') || chk.matches('.js-chk-option')) {
        const menuId = chk.getAttribute('data-menu-id');
        const menu = document.getElementById(menuId);
        if (!menu) return;

        const allChk = menu.querySelector('.js-chk-all');
        const optionChks = menu.querySelectorAll('.js-chk-option');

        if (chk.matches('.js-chk-all')) {
            optionChks.forEach(o => o.checked = chk.checked);
        } else {
            if (allChk) {
                const allChecked = Array.from(optionChks).every(o => o.checked);
                allChk.checked = allChecked;
            }
        }

        const dropdown = menu.closest('.multiselect-dropdown');
        const summaryEl = dropdown?.querySelector('.js-selected-text');
        const checkedCount = Array.from(optionChks).filter(o => o.checked).length;
        const totalCount = optionChks.length;

        if (summaryEl) {
            if (checkedCount === totalCount) {
                summaryEl.innerText = 'All Selected';
            } else if (checkedCount === 0) {
                summaryEl.innerText = 'None Selected';
            } else {
                summaryEl.innerText = `${checkedCount} Selected`;
            }
        }

        applyBatchDirectoryFilters();
        applyTestBaseFilters();
        applyRunReportFilters();
        if (menuId === 'tbSideBatchMultiselectMenu') {
            filterTbVariationHistoryTable();
        }
    }
});

// Multiselect Live Search Filter Listener
document.addEventListener('input', (e) => {
    if (e.target.matches('.js-multiselect-search')) {
        const query = e.target.value.toLowerCase().trim();
        const menuId = e.target.getAttribute('data-menu-id');
        const menu = document.getElementById(menuId);
        if (!menu) return;

        menu.querySelectorAll('.multiselect-option').forEach(opt => {
            if (opt.querySelector('.js-chk-all')) return;
            const txt = (opt.getAttribute('data-search-text') || opt.innerText).toLowerCase();
            opt.style.display = txt.includes(query) ? '' : 'none';
        });
    }
});

function applyBatchDirectoryFilters() {
    const scrollContainer = document.getElementById('mainViewContainer');
    const savedScrollTop = scrollContainer ? scrollContainer.scrollTop : 0;

    const envMenu = document.getElementById('batchEnvMultiselectMenu');
    const locMenu = document.getElementById('batchLocalesMultiselectMenu');
    const browserMenu = document.getElementById('batchBrowsersMultiselectMenu');
    const areaMenu = document.getElementById('batchAreasMultiselectMenu');

    // If legacy multiselect dropdown menus are present and have specific checked options, sync into batchActiveRatioFilters
    if (envMenu) {
        const envOptionChks = Array.from(envMenu.querySelectorAll('.js-chk-option'));
        const envChecked = envOptionChks.filter(o => o.checked);
        if (envChecked.length > 0 && envChecked.length < envOptionChks.length && batchActiveRatioFilters.env.size === 0) {
            envChecked.forEach(o => batchActiveRatioFilters.env.add(o.value.trim().toUpperCase()));
        }
    }
    if (locMenu) {
        const locOptionChks = Array.from(locMenu.querySelectorAll('.js-chk-option'));
        const locChecked = locOptionChks.filter(o => o.checked);
        if (locChecked.length > 0 && locChecked.length < locOptionChks.length && batchActiveRatioFilters.locale.size === 0) {
            locChecked.forEach(o => batchActiveRatioFilters.locale.add(o.value.trim().toUpperCase()));
        }
    }
    if (browserMenu) {
        const brOptionChks = Array.from(browserMenu.querySelectorAll('.js-chk-option'));
        const brChecked = brOptionChks.filter(o => o.checked);
        if (brChecked.length > 0 && brChecked.length < brOptionChks.length && batchActiveRatioFilters.browser.size === 0) {
            brChecked.forEach(o => batchActiveRatioFilters.browser.add(o.value.trim().toUpperCase()));
        }
    }
    if (areaMenu) {
        const arOptionChks = Array.from(areaMenu.querySelectorAll('.js-chk-option'));
        const arChecked = arOptionChks.filter(o => o.checked);
        if (arChecked.length > 0 && arChecked.length < arOptionChks.length && batchActiveRatioFilters.area.size === 0) {
            arChecked.forEach(o => batchActiveRatioFilters.area.add(o.value.trim().toUpperCase()));
        }
    }

    const isEnvFiltered = batchActiveRatioFilters.env.size > 0;
    const selectedEnvs = Array.from(batchActiveRatioFilters.env);

    const isLocFiltered = batchActiveRatioFilters.locale.size > 0;
    const selectedLocs = Array.from(batchActiveRatioFilters.locale);

    const isBrowserFiltered = batchActiveRatioFilters.browser.size > 0;
    const selectedBrowsers = Array.from(batchActiveRatioFilters.browser);

    const isAreaFiltered = batchActiveRatioFilters.area.size > 0;
    const selectedAreas = Array.from(batchActiveRatioFilters.area);

    const isTagFiltered = batchActiveRatioFilters.tag.size > 0;
    const selectedTags = Array.from(batchActiveRatioFilters.tag);

    document.querySelectorAll('.batch-card').forEach(card => {
        const cardEnv = (card.getAttribute('data-env') || '').toUpperCase();
        const cardLocs = (card.getAttribute('data-locales') || '').split(',').map(s => s.trim().toUpperCase()).filter(Boolean);
        const cardBrowsers = (card.getAttribute('data-browsers') || '').split(',').map(s => s.trim().toUpperCase()).filter(Boolean);

        const matchEnv = !isEnvFiltered
            ? true
            : selectedEnvs.some(e => cardEnv.includes(e) || e.includes(cardEnv));
        const matchLoc = !isLocFiltered
            ? true
            : (cardLocs.length > 0 && selectedLocs.some(l => cardLocs.some(cl => cl.includes(l) || l.includes(cl))));
        const matchBrowser = !isBrowserFiltered
            ? true
            : (cardBrowsers.length > 0 && selectedBrowsers.some(b => cardBrowsers.some(cb => cb.includes(b) || b.includes(cb))));

        card.style.display = (matchEnv && matchLoc && matchBrowser) ? '' : 'none';
    });

    document.querySelectorAll('#overviewRunsTableBody .run-directory-row').forEach(row => {
        const rowEnv = (row.getAttribute('data-env') || '').toUpperCase();
        const rowLocs = (row.getAttribute('data-locales') || '').split(',').map(s => s.trim().toUpperCase()).filter(Boolean);
        const rowBrowsers = (row.getAttribute('data-browsers') || '').split(',').map(s => s.trim().toUpperCase()).filter(Boolean);

        const matchEnv = !isEnvFiltered
            ? true
            : selectedEnvs.some(e => rowEnv.includes(e) || e.includes(rowEnv));
        const matchLoc = !isLocFiltered
            ? true
            : (rowLocs.length > 0 && selectedLocs.some(l => rowLocs.some(cl => cl.includes(l) || l.includes(cl))));
        const matchBrowser = !isBrowserFiltered
            ? true
            : (rowBrowsers.length > 0 && selectedBrowsers.some(b => rowBrowsers.some(cb => cb.includes(b) || b.includes(cb))));

        row.style.display = (matchEnv && matchLoc && matchBrowser) ? '' : 'none';
    });

    document.querySelectorAll('#batchTestBaseVariationsPanel .tb-entry-row').forEach(row => {
        row.style.display = matchEntryFilters(row, null) ? '' : 'none';
    });

    document.querySelectorAll('#batchTestBaseVariationsPanel .test-class-container').forEach(classBox => {
        const visibleRows = Array.from(classBox.querySelectorAll('.tb-entry-row')).filter(r => r.style.display !== 'none');
        classBox.style.display = (visibleRows.length > 0) ? '' : 'none';
    });

    document.querySelectorAll('#batchTestBaseVariationsPanel .area-group').forEach(areaBox => {
        const visibleRows = Array.from(areaBox.querySelectorAll('.tb-entry-row')).filter(r => r.style.display !== 'none');
        areaBox.style.display = (visibleRows.length > 0) ? '' : 'none';
    });

    const allVariationRows = Array.from(document.querySelectorAll('#batchTestBaseVariationsPanel .tb-entry-row'));
    const visibleVariationRows = allVariationRows.filter(r => r.style.display !== 'none');
    const variationsCountBadge = document.getElementById('batchTotalVariationsCountBadge');
    if (variationsCountBadge) {
        const count = visibleVariationRows.length;
        variationsCountBadge.innerText = `${count} ${count === 1 ? 'Variation' : 'Variations'}`;
    }

    const noMatchMsg = document.getElementById('batchVariationsNoMatchMsg');
    if (noMatchMsg) {
        let hasActiveRatioFilters = isEnvFiltered || isLocFiltered || isBrowserFiltered || isAreaFiltered || isTagFiltered;
        if (!hasActiveRatioFilters) {
            for (const k in batchActiveRatioFilters) {
                if (k.startsWith('tag_') && batchActiveRatioFilters[k].size > 0) {
                    hasActiveRatioFilters = true;
                    break;
                }
            }
        }
        noMatchMsg.style.display = (hasActiveRatioFilters && allVariationRows.length > 0 && visibleVariationRows.length === 0) ? 'block' : 'none';
    }

    recalculateBatchLatestStatusPie();
    syncBatchRatioBarsVisualState();

    if (scrollContainer && savedScrollTop > 0) {
        scrollContainer.scrollTop = savedScrollTop;
        requestAnimationFrame(() => {
            if (scrollContainer) scrollContainer.scrollTop = savedScrollTop;
        });
    }
}

function formatTokenCostCurrency(amount) {
    if (amount >= 1000000.0) {
        return '$' + (amount / 1000000.0).toFixed(2) + 'M';
    }
    if (amount >= 10000.0) {
        return '$' + (amount / 1000.0).toFixed(1) + 'K';
    }
    if (amount >= 1.0) {
        return '$' + amount.toFixed(2);
    }
    if (amount > 0.0) {
        return '$' + amount.toFixed(4);
    }
    return '$0.0000';
}

function recalculateBatchLatestStatusPie() {
    const pieWrapper = document.querySelector('.pie-chart-wrapper-batch');
    if (!pieWrapper) return;

    let variationEntries = Array.from(document.querySelectorAll('#batchLatestVariationsData .batch-latest-var-entry'));
    const isUsingDedicatedData = (variationEntries.length > 0);
    if (!isUsingDedicatedData) {
        variationEntries = Array.from(document.querySelectorAll('#batchTestBaseVariationsPanel .tb-entry-row'));
    }
    if (variationEntries.length === 0) return;

    const isEnvFiltered = batchActiveRatioFilters.env.size > 0;
    const selectedEnvs = Array.from(batchActiveRatioFilters.env);

    const isLocFiltered = batchActiveRatioFilters.locale.size > 0;
    const selectedLocs = Array.from(batchActiveRatioFilters.locale);

    const isBrowserFiltered = batchActiveRatioFilters.browser.size > 0;
    const selectedBrowsers = Array.from(batchActiveRatioFilters.browser);

    const isAreaFiltered = batchActiveRatioFilters.area.size > 0;
    const selectedAreas = Array.from(batchActiveRatioFilters.area);

    const isTagFiltered = batchActiveRatioFilters.tag.size > 0;
    const selectedTags = Array.from(batchActiveRatioFilters.tag);

    let passClean = 0;
    let passHealed = 0;
    let passAi = 0;
    let fixedClean = 0;
    let fixedHealed = 0;
    let fixedAi = 0;
    let knownClean = 0;
    let knownHealed = 0;
    let knownAi = 0;
    let unknownClean = 0;
    let unknownHealed = 0;
    let unknownAi = 0;
    let ignored = 0;

    let filteredTotalCost = 0.0;
    let filteredTotalTokens = 0;

    const activeRunIds = new Set();
    const runStatsMap = {};

    variationEntries.forEach(entry => {
        if (!matchEntryFilters(entry, null)) {
            return;
        }

        const costStr = entry.getAttribute('data-llm-cost');
        if (costStr) {
            filteredTotalCost += parseFloat(costStr) || 0.0;
        }
        const toksStr = entry.getAttribute('data-llm-tokens');
        if (toksStr) {
            filteredTotalTokens += parseInt(toksStr, 10) || 0;
        }

        const runId = entry.getAttribute('data-run-id');
        if (runId) {
            activeRunIds.add(runId);
            if (!runStatsMap[runId]) {
                runStatsMap[runId] = {
                    passClean: 0, passHealed: 0, passAi: 0,
                    fixedClean: 0, fixedHealed: 0, fixedAi: 0,
                    knownClean: 0, knownHealed: 0, knownAi: 0,
                    unknownClean: 0, unknownHealed: 0, unknownAi: 0,
                    ignored: 0,
                    total: 0
                };
            }
        }
        const runStats = runId ? runStatsMap[runId] : null;

        const rawStatus = (entry.getAttribute('data-status') || '').trim().toLowerCase();
        const healed = entry.getAttribute('data-healed') === 'true' || rawStatus === 'healed';
        const mode = (entry.getAttribute('data-mode') || '').trim().toUpperCase();
        const ai = (mode === 'AI');

        if (rawStatus === 'passed-clean' || rawStatus === 'passed' || rawStatus === 'succeeded') {
            if (healed) { passHealed++; if (runStats) { runStats.passHealed++; runStats.total++; } }
            else if (ai) { passAi++; if (runStats) { runStats.passAi++; runStats.total++; } }
            else { passClean++; if (runStats) { runStats.passClean++; runStats.total++; } }
        } else if (rawStatus === 'succeeded-fixed' || rawStatus === 'fixed') {
            if (healed) { fixedHealed++; if (runStats) { runStats.fixedHealed++; runStats.total++; } }
            else if (ai) { fixedAi++; if (runStats) { runStats.fixedAi++; runStats.total++; } }
            else { fixedClean++; if (runStats) { runStats.fixedClean++; runStats.total++; } }
        } else if (rawStatus === 'failed-known' || rawStatus === 'known') {
            if (healed) { knownHealed++; if (runStats) { runStats.knownHealed++; runStats.total++; } }
            else if (ai) { knownAi++; if (runStats) { runStats.knownAi++; runStats.total++; } }
            else { knownClean++; if (runStats) { runStats.knownClean++; runStats.total++; } }
        } else if (rawStatus === 'failed-unknown' || rawStatus === 'unknown' || rawStatus === 'failed' || rawStatus === 'error') {
            if (healed) { unknownHealed++; if (runStats) { runStats.unknownHealed++; runStats.total++; } }
            else if (ai) { unknownAi++; if (runStats) { runStats.unknownAi++; runStats.total++; } }
            else { unknownClean++; if (runStats) { runStats.unknownClean++; runStats.total++; } }
        } else {
            ignored++;
            if (runStats) { runStats.ignored++; runStats.total++; }
        }
    });

    const totalPassed = passClean + passHealed + passAi;
    const totalFixed = fixedClean + fixedHealed + fixedAi;
    const totalKnown = knownClean + knownHealed + knownAi;
    const totalUnknown = unknownClean + unknownHealed + unknownAi;
    const totalTests = totalPassed + totalFixed + totalKnown + totalUnknown + ignored;

    // Center KPI
    const rateEl = document.querySelector('.pie-center-rate');
    const subEl = document.querySelector('.pie-center-sub');
    if (rateEl) {
        if (totalTests > 0) {
            const passPct = Math.round(((totalPassed + totalFixed) / totalTests) * 100);
            rateEl.innerText = `${passPct}%`;
        } else {
            rateEl.innerText = '0%';
        }
    }
    if (subEl) {
        subEl.innerText = totalTests > 0 ? 'PASS' : 'TESTS';
    }

    // Donut SVG arcs
    let currentOffset = 25.0;
    function updateArc(arcKey, patKey, count) {
        const pct = totalTests > 0 ? (count * 100.0 / totalTests) : 0;
        const el = pieWrapper.querySelector(`svg circle[data-arc="${arcKey}"]`);
        const patEl = patKey ? pieWrapper.querySelector(`svg circle[data-arc="${patKey}"]`) : null;

        if (pct > 0) {
            const dashArray = `${pct.toFixed(2)} ${(100.0 - pct).toFixed(2)}`;
            const dashOffset = currentOffset.toFixed(2);
            if (el) {
                el.setAttribute('stroke-dasharray', dashArray);
                el.setAttribute('stroke-dashoffset', dashOffset);
                el.style.display = '';
            }
            if (patEl) {
                patEl.setAttribute('stroke-dasharray', dashArray);
                patEl.setAttribute('stroke-dashoffset', dashOffset);
                patEl.style.display = '';
            }
            currentOffset -= pct;
        } else {
            if (el) el.style.display = 'none';
            if (patEl) patEl.style.display = 'none';
        }
    }

    updateArc('pass-clean', null, passClean);
    updateArc('pass-healed', 'pass-healed-pat', passHealed);
    updateArc('pass-ai', 'pass-ai-pat', passAi);
    updateArc('fixed-clean', null, fixedClean);
    updateArc('fixed-healed', 'fixed-healed-pat', fixedHealed);
    updateArc('fixed-ai', 'fixed-ai-pat', fixedAi);
    updateArc('known-clean', null, knownClean);
    updateArc('known-healed', 'known-healed-pat', knownHealed);
    updateArc('known-ai', 'known-ai-pat', knownAi);
    updateArc('unknown-clean', null, unknownClean);
    updateArc('unknown-healed', 'unknown-healed-pat', unknownHealed);
    updateArc('unknown-ai', 'unknown-ai-pat', unknownAi);
    updateArc('ignored', null, ignored);

    // Breakdown list
    function updateBreakdown(breakdownKey, totalCount, healedCount, aiCount) {
        const item = document.querySelector(`.pie-breakdown-item[data-breakdown="${breakdownKey}"]`);
        if (!item) return;
        if (totalCount > 0) {
            item.style.display = '';
            const valEl = item.querySelector('.val');
            if (valEl) valEl.innerText = totalCount;
            const healedEl = item.querySelector('.badge-healed-inline');
            if (healedEl) {
                healedEl.style.display = healedCount > 0 ? '' : 'none';
                healedEl.innerText = `✨ ${healedCount} healed`;
            }
            const aiEl = item.querySelector('.badge-ai-inline');
            if (aiEl) {
                aiEl.style.display = aiCount > 0 ? '' : 'none';
                aiEl.innerText = `${aiCount} AI`;
            }
        } else {
            item.style.display = 'none';
        }
    }

    updateBreakdown('passed', totalPassed, passHealed, passAi);
    updateBreakdown('fixed', totalFixed, fixedHealed, fixedAi);
    updateBreakdown('known', totalKnown, knownHealed, knownAi);
    updateBreakdown('unknown', totalUnknown, unknownHealed, unknownAi);
    updateBreakdown('ignored', ignored, 0, 0);

    const totalValEl = document.getElementById('batchLatestTotalVal');
    const totalTileEl = document.getElementById('batchLatestTotalTile');
    if (totalValEl) totalValEl.innerText = totalTests;
    if (totalTileEl) totalTileEl.style.display = totalTests > 0 ? '' : 'none';

    const noBreakdownMsg = document.getElementById('batchLatestNoBreakdownMsg');
    if (noBreakdownMsg) {
        noBreakdownMsg.style.display = totalTests === 0 ? '' : 'none';
    }

    const tokenCostValEl = document.getElementById('batchTokenCostValue');
    if (tokenCostValEl) {
        tokenCostValEl.textContent = formatTokenCostCurrency(filteredTotalCost);
    }
    const tokenCostBadgeEl = document.getElementById('batchTokenCostTokensBadge');
    if (tokenCostBadgeEl) {
        tokenCostBadgeEl.textContent = filteredTotalTokens.toLocaleString() + ' tokens';
    }

    // Contributing runs list synchronization
    let visibleContribRunsCount = 0;
    const contribItems = document.querySelectorAll('.contributing-runs-list .contributing-run-item');
    contribItems.forEach(item => {
        const rId = item.getAttribute('data-run-id');
        const isVisible = rId && activeRunIds.has(rId);
        item.style.display = isVisible ? '' : 'none';
        if (isVisible) {
            visibleContribRunsCount++;
            const stats = runStatsMap[rId];
            if (stats) {
                const countEl = item.querySelector('.contributing-run-count');
                if (countEl) {
                    countEl.innerText = `(${stats.total} ${stats.total === 1 ? 'test' : 'tests'})`;
                }
                const barEl = item.querySelector('.contributing-run-bar');
                if (barEl) {
                    renderContributingRunSegments(barEl, stats);
                }
            }
        }
    });

    const contribList = document.getElementById('batchContributingRunsList');
    const noContribMsg = document.getElementById('batchNoContributingRunsMsg');
    if (contribList && noContribMsg) {
        if (visibleContribRunsCount > 0) {
            contribList.style.display = '';
            noContribMsg.style.display = 'none';
        } else {
            contribList.style.display = 'none';
            noContribMsg.style.display = '';
        }
    }

    // Header badge
    const countBadge = document.getElementById('batchLatestTestCountBadge');
    if (countBadge) {
        const testLabel = totalTests === 1 ? 'Test' : 'Tests';
        const runLabel = visibleContribRunsCount === 1 ? 'Run' : 'Runs';
        countBadge.innerText = `${totalTests} ${testLabel} across ${visibleContribRunsCount} ${runLabel}`;
    }

    // Dynamic layout adaptation: switch to stacked layout when 0 or 1 run remains visible
    const latestBody = document.querySelector('.latest-status-body');
    if (latestBody) {
        latestBody.classList.toggle('layout-stacked', visibleContribRunsCount <= 1);
    }
    const collapsedLabel = document.getElementById('batchContribCollapsedLabel');
    if (collapsedLabel) {
        collapsedLabel.innerText = `Contributing Runs (${visibleContribRunsCount})`;
    }
}

function toggleContributingRunsCollapse() {
    const latestBody = document.querySelector('.latest-status-body');
    if (!latestBody) return;
    const isCollapsed = latestBody.classList.toggle('contrib-collapsed');
    try {
        localStorage.setItem('aura_batch_contrib_collapsed', isCollapsed ? 'true' : 'false');
    } catch (e) {
        // ignore localStorage errors (e.g. incognito mode)
    }
}

function initContributingRunsCollapseState() {
    try {
        const isCollapsed = localStorage.getItem('aura_batch_contrib_collapsed') === 'true';
        const latestBody = document.querySelector('.latest-status-body');
        if (latestBody && isCollapsed) {
            latestBody.classList.add('contrib-collapsed');
        }
    } catch (e) {
        // ignore
    }
}

let batchStickyFiltersObserver = null;

function updateBatchStickyFiltersHeight() {
    const filtersContainer = document.getElementById('batchStickyFiltersContainer');
    if (filtersContainer) {
        const height = filtersContainer.offsetHeight;
        if (height > 0) {
            document.documentElement.style.setProperty('--batch-sticky-filters-height', height + 'px');
        }
    }
}

function initBatchStickyFiltersObserver() {
    updateBatchStickyFiltersHeight();
    const filtersContainer = document.getElementById('batchStickyFiltersContainer');
    if (filtersContainer && window.ResizeObserver) {
        if (!batchStickyFiltersObserver) {
            batchStickyFiltersObserver = new ResizeObserver(() => {
                updateBatchStickyFiltersHeight();
            });
        }
        batchStickyFiltersObserver.disconnect();
        batchStickyFiltersObserver.observe(filtersContainer);
    }
}

if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
    window.addEventListener('resize', updateBatchStickyFiltersHeight);
}

function renderContributingRunSegments(barEl, stats) {
    if (!barEl || !stats || stats.total <= 0) return;
    const t = stats.total;

    const segDefs = [
        { count: stats.passClean, cls: 'seg-pass', label: 'Passed Clean' },
        { count: stats.passHealed, cls: 'seg-pass-healed seg-healed-print', label: 'Passed Clean Healed' },
        { count: stats.passAi, cls: 'seg-pass seg-ai-print', label: 'Passed AI-Driven' },
        { count: stats.fixedClean, cls: 'seg-fixed', label: 'Succeeded with Known Bug' },
        { count: stats.fixedHealed, cls: 'seg-fixed-healed seg-healed-print', label: 'Succeeded Fixed Healed' },
        { count: stats.fixedAi, cls: 'seg-fixed seg-ai-print', label: 'Succeeded Fixed AI-Driven' },
        { count: stats.knownClean, cls: 'seg-known', label: 'Failed due to Known Bug' },
        { count: stats.knownHealed, cls: 'seg-known-healed seg-healed-print', label: 'Failed Known Healed' },
        { count: stats.knownAi, cls: 'seg-known seg-ai-print', label: 'Failed Known AI-Driven' },
        { count: stats.unknownClean, cls: 'seg-unknown', label: 'Failed without Issue Info' },
        { count: stats.unknownHealed, cls: 'seg-unknown-healed seg-healed-print', label: 'Failed Unknown Healed' },
        { count: stats.unknownAi, cls: 'seg-unknown seg-ai-print', label: 'Failed Unknown AI-Driven' },
        { count: stats.ignored, cls: 'seg-ignored', label: 'Ignored / Skipped' }
    ];

    let html = '';
    segDefs.forEach(s => {
        if (s.count > 0) {
            const pct = (s.count / t * 100).toFixed(2);
            const title = `${s.count} ${s.label} (${pct}%)`;
            const text = (parseFloat(pct) >= 12) ? s.count : '';
            html += `<div class="status-bar-seg ${s.cls}" style="width: ${pct}%;" title="${title}">${text}</div>`;
        }
    });

    barEl.innerHTML = html;
}

function syncBatchRatioBarsVisualState() {
    let variationEntries = Array.from(document.querySelectorAll('#batchLatestVariationsData .batch-latest-var-entry'));
    if (variationEntries.length === 0) {
        variationEntries = Array.from(document.querySelectorAll('#batchTestBaseVariationsPanel .tb-entry-row'));
    }

    const envMenu = document.getElementById('batchEnvMultiselectMenu');
    const locMenu = document.getElementById('batchLocalesMultiselectMenu');
    const browserMenu = document.getElementById('batchBrowsersMultiselectMenu');
    const areaMenu = document.getElementById('batchAreasMultiselectMenu');

    const isEnvFiltered = batchActiveRatioFilters.env.size > 0;
    const selectedEnvs = Array.from(batchActiveRatioFilters.env);

    const isLocFiltered = batchActiveRatioFilters.locale.size > 0;
    const selectedLocs = Array.from(batchActiveRatioFilters.locale);

    const isBrowserFiltered = batchActiveRatioFilters.browser.size > 0;
    const selectedBrowsers = Array.from(batchActiveRatioFilters.browser);

    const isAreaFiltered = batchActiveRatioFilters.area.size > 0;
    const selectedAreas = Array.from(batchActiveRatioFilters.area);

    const isTagFiltered = batchActiveRatioFilters.tag.size > 0;
    const selectedTags = Array.from(batchActiveRatioFilters.tag);

    // 1. BROWSERS RATIO (Cross-filtered by Env + Locales + Areas + Tags)
    const browserClearBtn = document.getElementById('batchClearBrowserFilterBtn');
    if (browserClearBtn) {
        browserClearBtn.style.display = isBrowserFiltered ? 'inline-flex' : 'none';
    }

    const browserCounts = {};
    let totalBrowserVariations = 0;

    variationEntries.forEach(entry => {
        let browser = (entry.getAttribute('data-browser') || '').trim().toUpperCase();
        if (!browser) browser = 'UNKNOWN';

        if (matchEntryFilters(entry, 'browser')) {
            browserCounts[browser] = (browserCounts[browser] || 0) + 1;
            totalBrowserVariations++;
        }
    });


    document.querySelectorAll('.ratio-bar-seg[data-ratio-type="browser"], .ratio-legend-chip[data-ratio-type="browser"]').forEach(el => {
        const rawVal = el.getAttribute('data-filter-val') || '';
        const val = rawVal.trim().toUpperCase();

        let count = browserCounts[val] || 0;
        if (count === 0) {
            for (const bKey in browserCounts) {
                if (bKey.includes(val) || val.includes(bKey)) {
                    count = browserCounts[bKey];
                    break;
                }
            }
        }

        const pct = (totalBrowserVariations > 0) ? (count / totalBrowserVariations * 100) : 0;
        const formattedPct = `${Math.round(pct)}%`;

        if (el.classList.contains('ratio-bar-seg')) {
            el.style.width = `${pct.toFixed(2)}%`;
            el.innerText = (pct >= 15) ? formattedPct : '';
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        } else if (el.classList.contains('ratio-legend-chip')) {
            const valSpan = el.querySelector('.val');
            if (valSpan) {
                valSpan.innerText = `${count} (${formattedPct})`;
            }
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        }

        if (!isBrowserFiltered) {
            el.classList.remove('is-dimmed', 'is-active');
        } else if (selectedBrowsers.some(v => v === val || v.includes(val) || val.includes(v))) {
            el.classList.add('is-active');
            el.classList.remove('is-dimmed');
        } else {
            el.classList.add('is-dimmed');
            el.classList.remove('is-active');
        }
    });

    if (browserMenu) {
        const optionChks = Array.from(browserMenu.querySelectorAll('.js-chk-option'));
        optionChks.forEach(chk => {
            const optVal = chk.value.trim().toUpperCase();
            let count = browserCounts[optVal] || 0;
            if (count === 0) {
                for (const bKey in browserCounts) {
                    if (bKey.includes(optVal) || optVal.includes(bKey)) {
                        count = browserCounts[bKey];
                        break;
                    }
                }
            }
            const labelEl = chk.closest('.multiselect-option');
            if (labelEl) {
                labelEl.classList.toggle('is-empty-option', count === 0);
                const span = labelEl.querySelector('span');
                if (span) {
                    span.innerText = `${chk.value} (${count})`;
                }
            }
        });
    }

    // 2. LOCALES RATIO (Cross-filtered by Env + Browsers + Areas + Tags)
    const locClearBtn = document.getElementById('batchClearLocaleFilterBtn');
    if (locClearBtn) {
        locClearBtn.style.display = isLocFiltered ? 'inline-flex' : 'none';
    }

    const localeCounts = {};
    let totalLocaleVariations = 0;

    variationEntries.forEach(entry => {
        let loc = (entry.getAttribute('data-location') || '').trim().toUpperCase();
        if (!loc || loc === 'UNKNOWN') loc = 'DEFAULT';

        if (matchEntryFilters(entry, 'locale')) {
            localeCounts[loc] = (localeCounts[loc] || 0) + 1;
            totalLocaleVariations++;
        }
    });


    document.querySelectorAll('.ratio-bar-seg[data-ratio-type="locale"], .ratio-legend-chip[data-ratio-type="locale"]').forEach(el => {
        const rawVal = el.getAttribute('data-filter-val') || '';
        const val = rawVal.trim().toUpperCase();

        let count = localeCounts[val] || 0;
        if (count === 0) {
            for (const lKey in localeCounts) {
                if (lKey.includes(val) || val.includes(lKey)) {
                    count = localeCounts[lKey];
                    break;
                }
            }
        }

        const pct = (totalLocaleVariations > 0) ? (count / totalLocaleVariations * 100) : 0;
        const formattedPct = `${Math.round(pct)}%`;

        if (el.classList.contains('ratio-bar-seg')) {
            el.style.width = `${pct.toFixed(2)}%`;
            el.innerText = (pct >= 15) ? formattedPct : '';
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        } else if (el.classList.contains('ratio-legend-chip')) {
            const valSpan = el.querySelector('.val');
            if (valSpan) {
                valSpan.innerText = `${count} (${formattedPct})`;
            }
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        }

        if (!isLocFiltered) {
            el.classList.remove('is-dimmed', 'is-active');
        } else if (selectedLocs.some(v => v === val || v.includes(val) || val.includes(v))) {
            el.classList.add('is-active');
            el.classList.remove('is-dimmed');
        } else {
            el.classList.add('is-dimmed');
            el.classList.remove('is-active');
        }
    });

    if (locMenu) {
        const optionChks = Array.from(locMenu.querySelectorAll('.js-chk-option'));
        optionChks.forEach(chk => {
            const optVal = chk.value.trim().toUpperCase();
            let count = localeCounts[optVal] || 0;
            if (count === 0) {
                for (const lKey in localeCounts) {
                    if (lKey.includes(optVal) || optVal.includes(lKey)) {
                        count = localeCounts[lKey];
                        break;
                    }
                }
            }
            const labelEl = chk.closest('.multiselect-option');
            if (labelEl) {
                labelEl.classList.toggle('is-empty-option', count === 0);
                const span = labelEl.querySelector('span');
                if (span) {
                    span.innerText = `${chk.value} (${count})`;
                }
            }
        });
    }

    // 3. ENVIRONMENT RATIO (Cross-filtered by Locales + Browsers + Areas + Tags)
    const envClearBtn = document.getElementById('batchClearEnvFilterBtn');
    if (envClearBtn) {
        envClearBtn.style.display = isEnvFiltered ? 'inline-flex' : 'none';
    }

    const envCounts = {};
    let totalEnvVariations = 0;

    variationEntries.forEach(entry => {
        let env = (entry.getAttribute('data-env') || '').trim().toUpperCase();
        if (!env) env = 'DEFAULT';

        if (matchEntryFilters(entry, 'env')) {
            envCounts[env] = (envCounts[env] || 0) + 1;
            totalEnvVariations++;
        }
    });


    document.querySelectorAll('.ratio-bar-seg[data-ratio-type="env"], .ratio-legend-chip[data-ratio-type="env"]').forEach(el => {
        const rawVal = el.getAttribute('data-filter-val') || '';
        const val = rawVal.trim().toUpperCase();

        let count = envCounts[val] || 0;
        if (count === 0) {
            for (const eKey in envCounts) {
                if (eKey.includes(val) || val.includes(eKey)) {
                    count = envCounts[eKey];
                    break;
                }
            }
        }

        const pct = (totalEnvVariations > 0) ? (count / totalEnvVariations * 100) : 0;
        const formattedPct = `${Math.round(pct)}%`;

        if (el.classList.contains('ratio-bar-seg')) {
            el.style.width = `${pct.toFixed(2)}%`;
            el.innerText = (pct >= 15) ? formattedPct : '';
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        } else if (el.classList.contains('ratio-legend-chip')) {
            const valSpan = el.querySelector('.val');
            if (valSpan) {
                valSpan.innerText = `${count} (${formattedPct})`;
            }
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        }

        if (!isEnvFiltered) {
            el.classList.remove('is-dimmed', 'is-active');
        } else if (selectedEnvs.some(v => v === val || v.includes(val) || val.includes(v))) {
            el.classList.add('is-active');
            el.classList.remove('is-dimmed');
        } else {
            el.classList.add('is-dimmed');
            el.classList.remove('is-active');
        }
    });

    if (envMenu) {
        const optionChks = Array.from(envMenu.querySelectorAll('.js-chk-option'));
        optionChks.forEach(chk => {
            const optVal = chk.value.trim().toUpperCase();
            let count = envCounts[optVal] || 0;
            if (count === 0) {
                for (const eKey in envCounts) {
                    if (eKey.includes(optVal) || optVal.includes(eKey)) {
                        count = envCounts[eKey];
                        break;
                    }
                }
            }
            const labelEl = chk.closest('.multiselect-option');
            if (labelEl) {
                labelEl.classList.toggle('is-empty-option', count === 0);
                const span = labelEl.querySelector('span');
                if (span) {
                    span.innerText = `${chk.value} (${count})`;
                }
            }
        });
    }

    // 4. AREAS RATIO (Cross-filtered by Env + Locales + Browsers + Tags)
    const areaClearBtn = document.getElementById('batchClearAreaFilterBtn');
    if (areaClearBtn) {
        areaClearBtn.style.display = isAreaFiltered ? 'inline-flex' : 'none';
    }

    const areaCounts = {};
    let totalAreaVariations = 0;

    variationEntries.forEach(entry => {
        let area = (entry.getAttribute('data-area') || '').trim().toUpperCase();
        if (!area) area = '@BROWSING (DEFAULT)';

        if (matchEntryFilters(entry, 'area')) {
            const normArea = normalizeAreaFilterVal(area);
            areaCounts[normArea] = (areaCounts[normArea] || 0) + 1;
            totalAreaVariations++;
        }
    });


    document.querySelectorAll('.ratio-bar-seg[data-ratio-type="area"], .ratio-legend-chip[data-ratio-type="area"]').forEach(el => {
        const rawVal = el.getAttribute('data-filter-val') || '';
        const normVal = normalizeAreaFilterVal(rawVal);

        let count = areaCounts[normVal] || 0;

        const pct = (totalAreaVariations > 0) ? (count / totalAreaVariations * 100) : 0;
        const formattedPct = `${Math.round(pct)}%`;

        if (el.classList.contains('ratio-bar-seg')) {
            el.style.width = `${pct.toFixed(2)}%`;
            el.innerText = (pct >= 15) ? formattedPct : '';
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        } else if (el.classList.contains('ratio-legend-chip')) {
            const valSpan = el.querySelector('.val');
            if (valSpan) {
                valSpan.innerText = `${count} (${formattedPct})`;
            }
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        }

        if (!isAreaFiltered) {
            el.classList.remove('is-dimmed', 'is-active');
        } else if (selectedAreas.some(v => {
            const normV = normalizeAreaFilterVal(v);
            return normVal && normV && normVal === normV;
        })) {
            el.classList.add('is-active');
            el.classList.remove('is-dimmed');
        } else {
            el.classList.add('is-dimmed');
            el.classList.remove('is-active');
        }
    });

    if (areaMenu) {
        const optionChks = Array.from(areaMenu.querySelectorAll('.js-chk-option'));
        optionChks.forEach(chk => {
            const optVal = chk.value.trim().toUpperCase();
            const normOpt = normalizeAreaFilterVal(optVal);
            let count = areaCounts[normOpt] || 0;
            const labelEl = chk.closest('.multiselect-option');
            if (labelEl) {
                labelEl.classList.toggle('is-empty-option', count === 0);
                const span = labelEl.querySelector('span');
                if (span) {
                    span.innerText = `${chk.value} (${count})`;
                }
            }
        });
    }

    // 5. TAGS / BATCH RATIO (Cross-filtered by Env + Locales + Browsers + Areas)
    const tagClearBtn = document.getElementById('batchClearTagFilterBtn');
    if (tagClearBtn) {
        tagClearBtn.style.display = isTagFiltered ? 'inline-flex' : 'none';
    }

    const tagCounts = {};
    let totalTagVariations = 0;

    variationEntries.forEach(entry => {
        let batch = (entry.getAttribute('data-batch') || '').trim().toUpperCase();

        if (matchEntryFilters(entry, 'tag')) {
            tagCounts[batch] = (tagCounts[batch] || 0) + 1;
            totalTagVariations++;
        }
    });


    document.querySelectorAll('.ratio-bar-seg[data-ratio-type="tag"], .ratio-legend-chip[data-ratio-type="tag"]').forEach(el => {
        const rawVal = el.getAttribute('data-filter-val') || '';
        const val = rawVal.trim().toUpperCase();

        let count = tagCounts[val] || 0;
        if (count === 0) {
            for (const tKey in tagCounts) {
                if (tKey.includes(val) || val.includes(tKey)) {
                    count = tagCounts[tKey];
                    break;
                }
            }
        }

        const pct = (totalTagVariations > 0) ? (count / totalTagVariations * 100) : 0;
        const formattedPct = `${Math.round(pct)}%`;

        if (el.classList.contains('ratio-bar-seg')) {
            el.style.width = `${pct.toFixed(2)}%`;
            el.innerText = (pct >= 15) ? formattedPct : '';
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        } else if (el.classList.contains('ratio-legend-chip')) {
            const valSpan = el.querySelector('.val');
            if (valSpan) {
                valSpan.innerText = `${count} (${formattedPct})`;
            }
            el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
        }

        if (!isTagFiltered) {
            el.classList.remove('is-dimmed', 'is-active');
        } else if (selectedTags.some(v => v === val || v.includes(val) || val.includes(v))) {
            el.classList.add('is-active');
            el.classList.remove('is-dimmed');
        } else {
            el.classList.add('is-dimmed');
            el.classList.remove('is-active');
        }
    });

    // 6. DYNAMIC TAG RATIO CARDS
    const dynamicTagTypes = new Set();
    document.querySelectorAll('.ratio-bar-seg[data-ratio-type^="tag_"], .ratio-legend-chip[data-ratio-type^="tag_"]').forEach(el => {
        const rt = el.getAttribute('data-ratio-type');
        if (rt) dynamicTagTypes.add(rt);
    });

    dynamicTagTypes.forEach(tagRatioType => {
        const cleanKey = tagRatioType.substring(4);
        const normType = tagRatioType.toLowerCase();
        const isThisTagFiltered = batchActiveRatioFilters[normType] && batchActiveRatioFilters[normType].size > 0;

        const clearBtn = document.getElementById('batchClearTag_' + cleanKey + 'FilterBtn')
            || Array.from(document.querySelectorAll('.btn-clear-ratio')).find(b => b.id.toLowerCase() === ('batchcleartag_' + cleanKey.toLowerCase() + 'filterbtn'));
        if (clearBtn) {
            clearBtn.style.display = isThisTagFiltered ? 'inline-flex' : 'none';
        }

        const valCounts = {};
        let totalVariationsForCard = 0;

        variationEntries.forEach(entry => {
            if (matchEntryFilters(entry, normType)) {
                const rawVal = getElementTagValue(entry, cleanKey) || 'Default';
                const candidates = extractCandidateTagValues(rawVal);
                candidates.forEach(val => {
                    valCounts[val] = (valCounts[val] || 0) + 1;
                    totalVariationsForCard++;
                });
            }
        });

        document.querySelectorAll(`.ratio-bar-seg[data-ratio-type="${tagRatioType}"], .ratio-legend-chip[data-ratio-type="${tagRatioType}"]`).forEach(el => {
            const rawVal = el.getAttribute('data-filter-val') || '';
            const val = rawVal.trim().toUpperCase();

            let count = valCounts[val] || 0;
            if (count === 0) {
                for (const vKey in valCounts) {
                    if (vKey.includes(val) || val.includes(vKey)) {
                        count = valCounts[vKey];
                        break;
                    }
                }
            }

            const pct = (totalVariationsForCard > 0) ? (count / totalVariationsForCard * 100) : 0;
            const formattedPct = `${Math.round(pct)}%`;

            if (el.classList.contains('ratio-bar-seg')) {
                el.style.width = `${pct.toFixed(2)}%`;
                el.innerText = (pct >= 15) ? formattedPct : '';
                el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
            } else if (el.classList.contains('ratio-legend-chip')) {
                const valSpan = el.querySelector('.val');
                if (valSpan) {
                    valSpan.innerText = `${count} (${formattedPct})`;
                }
                el.title = `${rawVal}: ${count} variations (${formattedPct}) - Click to filter, Ctrl+Click to toggle`;
            }

            const activeSet = batchActiveRatioFilters[normType];
            if (!isThisTagFiltered || !activeSet || activeSet.size === 0) {
                el.classList.remove('is-dimmed', 'is-active');
            } else if (Array.from(activeSet).some(v => v === val || v.includes(val) || val.includes(v))) {
                el.classList.add('is-active');
                el.classList.remove('is-dimmed');
            } else {
                el.classList.add('is-dimmed');
                el.classList.remove('is-active');
            }
        });
    });
}

function filterFromRatioBar(type, filterVal, isCtrlOrMeta) {
    if (!type || !filterVal) return;
    const lowerType = type.toLowerCase();
    const normType = (lowerType === 'browsers' || lowerType === 'browser') ? 'browser'
        : (lowerType === 'locales' || lowerType === 'locale') ? 'locale'
        : (lowerType === 'envs' || lowerType === 'env' || lowerType === 'environment') ? 'env'
        : (lowerType === 'areas' || lowerType === 'area') ? 'area'
        : (lowerType === 'tags' || lowerType === 'tag' || lowerType === 'batch') ? 'tag'
        : (lowerType.startsWith('tag_')) ? lowerType : null;

    if (!normType) return;
    if (!batchActiveRatioFilters[normType]) {
        batchActiveRatioFilters[normType] = new Set();
    }

    const targetValUpper = filterVal.trim().toUpperCase();

    // Collect all known values for this dimension from the rendered ratio cards
    const knownElements = Array.from(document.querySelectorAll(
        `.ratio-legend-chip[data-ratio-type="${type}"], .ratio-bar-seg[data-ratio-type="${type}"]`
    ));
    const allKnownVals = Array.from(new Set(
        knownElements.map(el => (el.getAttribute('data-filter-val') || '').trim().toUpperCase()).filter(v => v.length > 0)
    ));

    const activeSet = batchActiveRatioFilters[normType];

    if (isCtrlOrMeta) {
        // Toggle target value
        if (activeSet.size === 0) {
            // All were active -> toggle target off (activate all except target)
            allKnownVals.forEach(v => {
                const match = (normType === 'area')
                    ? (normalizeAreaFilterVal(v) === normalizeAreaFilterVal(targetValUpper))
                    : (v === targetValUpper);
                if (!match) activeSet.add(v);
            });
        } else {
            let existingEntry = null;
            if (normType === 'area') {
                const normTarget = normalizeAreaFilterVal(targetValUpper);
                for (const item of activeSet) {
                    if (normalizeAreaFilterVal(item) === normTarget) {
                        existingEntry = item;
                        break;
                    }
                }
            } else if (activeSet.has(targetValUpper)) {
                existingEntry = targetValUpper;
            }

            if (existingEntry !== null) {
                activeSet.delete(existingEntry);
                if (activeSet.size === 0) {
                    // If nothing left selected, reset back to all active
                    activeSet.clear();
                }
            } else {
                activeSet.add(targetValUpper);
                if (allKnownVals.length > 0 && activeSet.size >= allKnownVals.length) {
                    // All are selected -> equivalent to no filter
                    activeSet.clear();
                }
            }
        }
    } else {
        // Single click: isolate or reset
        let isOnlyActive = false;
        if (activeSet.size === 1) {
            if (normType === 'area') {
                const currentVal = Array.from(activeSet)[0];
                isOnlyActive = (normalizeAreaFilterVal(currentVal) === normalizeAreaFilterVal(targetValUpper));
            } else {
                isOnlyActive = activeSet.has(targetValUpper);
            }
        }

        if (isOnlyActive) {
            // Already the only active filter -> toggle back to all
            activeSet.clear();
        } else {
            // Isolate
            activeSet.clear();
            activeSet.add(targetValUpper);
        }
    }

    // Keep legacy multiselect menus in sync if present on any other view
    const menuId = (normType === 'browser') ? 'batchBrowsersMultiselectMenu'
        : (normType === 'locale') ? 'batchLocalesMultiselectMenu'
        : (normType === 'env') ? 'batchEnvMultiselectMenu'
        : (normType === 'area') ? 'batchAreasMultiselectMenu' : null;
    if (menuId) {
        const menu = document.getElementById(menuId);
        if (menu) {
            const allChk = menu.querySelector('.js-chk-all');
            const optionChks = Array.from(menu.querySelectorAll('.js-chk-option'));
            if (activeSet.size === 0) {
                if (allChk) allChk.checked = true;
                optionChks.forEach(o => o.checked = true);
            } else {
                if (allChk) allChk.checked = false;
                optionChks.forEach(o => {
                    const oVal = o.value.trim().toUpperCase();
                    if (normType === 'area') {
                        const normOVal = normalizeAreaFilterVal(oVal);
                        o.checked = Array.from(activeSet).some(s => normalizeAreaFilterVal(s) === normOVal);
                    } else {
                        o.checked = activeSet.has(oVal);
                    }
                });
            }
            const dropdown = menu.closest('.multiselect-dropdown');
            const summaryEl = dropdown?.querySelector('.js-selected-text');
            if (summaryEl) {
                const checkedCount = optionChks.filter(o => o.checked).length;
                summaryEl.innerText = (checkedCount === optionChks.length || checkedCount === 0) ? 'All Selected'
                    : (checkedCount === 1 ? (optionChks.find(o => o.checked)?.value || '1 Selected') : `${checkedCount} Selected`);
            }
        }
    }

    applyBatchDirectoryFilters();
}

function resetBatchRatioFilter(dimension) {
    const lowerDim = dimension ? dimension.toLowerCase() : '';
    const normType = (lowerDim === 'browsers' || lowerDim === 'browser') ? 'browser'
        : (lowerDim === 'locales' || lowerDim === 'locale') ? 'locale'
        : (lowerDim === 'envs' || lowerDim === 'env' || lowerDim === 'environment') ? 'env'
        : (lowerDim === 'areas' || lowerDim === 'area') ? 'area'
        : (lowerDim === 'tags' || lowerDim === 'tag' || lowerDim === 'batch') ? 'tag'
        : (lowerDim.startsWith('tag_')) ? lowerDim : null;

    if (normType && batchActiveRatioFilters[normType]) {
        batchActiveRatioFilters[normType].clear();
    } else if (!dimension || dimension === 'all') {
        resetAllBatchRatioFilters();
    }

    // Keep legacy multiselect menu in sync if present
    const menuId = (normType === 'browser') ? 'batchBrowsersMultiselectMenu'
        : (normType === 'locale') ? 'batchLocalesMultiselectMenu'
        : (normType === 'env') ? 'batchEnvMultiselectMenu'
        : (normType === 'area') ? 'batchAreasMultiselectMenu' : null;
    if (menuId) {
        const menu = document.getElementById(menuId);
        if (menu) {
            const allChk = menu.querySelector('.js-chk-all');
            const optionChks = menu.querySelectorAll('.js-chk-option');
            if (allChk) allChk.checked = true;
            optionChks.forEach(o => o.checked = true);
            const dropdown = menu.closest('.multiselect-dropdown');
            const summaryEl = dropdown?.querySelector('.js-selected-text');
            if (summaryEl) summaryEl.innerText = 'All Selected';
        }
    }

    applyBatchDirectoryFilters();
}

function resetBatchFilters() {
    document.querySelectorAll('#batchFiltersToolbar .js-chk-all, #batchFiltersToolbar .js-chk-option').forEach(c => c.checked = true);
    document.querySelectorAll('#batchFiltersToolbar .js-selected-text').forEach(s => s.innerText = 'All Selected');
    applyBatchDirectoryFilters();
}

function applyTestBaseFilters() {
    const locMenu = document.getElementById('tbLocalesMultiselectMenu');
    const browserMenu = document.getElementById('tbBrowsersMultiselectMenu');

    if (!locMenu && !browserMenu) return;

    const locOptionChks = locMenu ? Array.from(locMenu.querySelectorAll('.js-chk-option')) : [];
    const locCheckedChks = locOptionChks.filter(o => o.checked);
    const isLocFiltered = locCheckedChks.length > 0 && locCheckedChks.length < locOptionChks.length;
    const selectedLocs = locCheckedChks.map(o => o.value.trim().toUpperCase());

    const browserOptionChks = browserMenu ? Array.from(browserMenu.querySelectorAll('.js-chk-option')) : [];
    const browserCheckedChks = browserOptionChks.filter(o => o.checked);
    const isBrowserFiltered = browserCheckedChks.length > 0 && browserCheckedChks.length < browserOptionChks.length;
    const selectedBrowsers = browserCheckedChks.map(o => o.value.trim().toUpperCase());

    document.querySelectorAll('#pageTestBase .tb-entry-row').forEach(row => {
        let loc = (row.getAttribute('data-location') || '').trim().toUpperCase();
        if (!loc || loc === 'UNKNOWN') loc = 'DEFAULT';

        let browser = (row.getAttribute('data-browser') || '').trim().toUpperCase();
        if (!browser) browser = 'UNKNOWN';

        const matchLoc = !isLocFiltered
            ? (locOptionChks.length === 0 || locCheckedChks.length > 0)
            : selectedLocs.some(l => loc === l || loc.includes(l) || l.includes(loc));

        const matchBrowser = !isBrowserFiltered
            ? (browserOptionChks.length === 0 || browserCheckedChks.length > 0)
            : selectedBrowsers.some(b => browser === b || browser.includes(b) || b.includes(browser));

        row.style.display = (matchLoc && matchBrowser) ? '' : 'none';
    });
}

function resetTestBaseFilters() {
    document.querySelectorAll('#tbFiltersToolbar .js-chk-all, #tbFiltersToolbar .js-chk-option').forEach(c => c.checked = true);
    document.querySelectorAll('#tbFiltersToolbar .js-selected-text').forEach(s => s.innerText = 'All Selected');
    applyTestBaseFilters();
}

// Unobtrusive Event Listeners & HTMX Re-Binding
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        bindGlobalListeners();
    });
} else {
    bindGlobalListeners();
}

let lastHandledTargetExecutionId = null;
let preservedMainViewScrollTop = null;

document.body.addEventListener('htmx:beforeRequest', (evt) => {
    const target = evt.detail && evt.detail.target;
    if (target && target.id === 'mainViewContainer') {
        const path = evt.detail.requestConfig?.path || '';
        if (path.includes('run-report') || path.includes('test-base') || path.includes('playbooks')) {
            preservedMainViewScrollTop = null;
        } else {
            const container = document.getElementById('mainViewContainer');
            if (container) {
                preservedMainViewScrollTop = container.scrollTop;
            }
        }
    }
});

document.body.addEventListener('htmx:afterSwap', (evt) => {
    const target = evt.detail && evt.detail.target;
    if (target && target.id === 'mainViewContainer') {
        resetAllBatchRatioFilters();
        const urlParams = new URLSearchParams(window.location.search);
        const targetExecId = document.querySelector('[data-target-execution-id]')?.getAttribute('data-target-execution-id')
            || urlParams.get('executionId')
            || urlParams.get('testExecutionId');
        if (!targetExecId) {
            closeTestSidePagePanel();
        }
        lastHandledTargetExecutionId = null;

        if (preservedMainViewScrollTop !== null && preservedMainViewScrollTop > 0) {
            const container = document.getElementById('mainViewContainer');
            if (container) {
                container.scrollTop = preservedMainViewScrollTop;
                requestAnimationFrame(() => {
                    if (container && preservedMainViewScrollTop !== null) {
                        container.scrollTop = preservedMainViewScrollTop;
                    }
                });
            }
        }
    }

    bindGlobalListeners();

    if (target && (target.id === 'sidePageStatusBadge' || target.id === 'runReportSubTabAllTests')) {
        recalculateRunReportMetrics();
    }
});

document.body.addEventListener('htmx:afterSettle', (evt) => {
    const target = evt.detail && evt.detail.target;
    if (target && target.id === 'mainViewContainer' && preservedMainViewScrollTop !== null && preservedMainViewScrollTop > 0) {
        const container = document.getElementById('mainViewContainer');
        if (container) {
            container.scrollTop = preservedMainViewScrollTop;
        }
        preservedMainViewScrollTop = null;
    }
});

function bindGlobalListeners() {
    // Initialize panel resizer drag behavior
    initPanelResizer();

    // Synchronize AI usage columns and data attributes for all execution rows
    syncAllRowsAiUsage();

    // Row click listeners for side panel inspection
    document.querySelectorAll('.js-open-side-panel').forEach(row => {
        row.removeEventListener('click', handleRowClick);
        row.addEventListener('click', handleRowClick);
    });

    // Close button listeners for side panel
    document.querySelectorAll('.js-close-side-panel, .side-page-close').forEach(btn => {
        btn.removeEventListener('click', closeTestSidePagePanel);
        btn.addEventListener('click', closeTestSidePagePanel);
    });

    // Synchronize sidebar active tab highlighting
    const isAuraManager = document.getElementById('auraTestManagerWorkspace') !== null || document.getElementById('dashboardView') !== null;
    const isTestBase = document.getElementById('pageTestBase') !== null;
    const navRuns = document.getElementById('navRunsList');
    const navTest = document.getElementById('navTestBase');
    const navAura = document.getElementById('navAuraManager');
    if (navRuns && navTest) {
        if (isAuraManager) {
            navRuns.classList.remove('active');
            navTest.classList.remove('active');
            if (navAura) navAura.classList.add('active');
        } else if (isTestBase) {
            navRuns.classList.remove('active');
            navTest.classList.add('active');
            if (navAura) navAura.classList.remove('active');
        } else {
            navRuns.classList.add('active');
            navTest.classList.remove('active');
            if (navAura) navAura.classList.remove('active');
        }
    }

    // Automatically render trend chart if Batch History view is loaded
    if (document.getElementById('mainTrendSvg')) {
        const titleEl = document.getElementById('batchOverviewTitle');
        if (titleEl && titleEl.innerText.trim()) {
            activeBatchName = titleEl.innerText.trim();
        }
        renderDynamicTrendChart(activeBatchName);
        renderDynamicAreaTrendCharts();
        renderDynamicSuiteEvolutionChart();
        recalculateBatchLatestStatusPie();
        syncBatchRatioBarsVisualState();
        
        const isAllRuns = !activeBatchName || activeBatchName.toLowerCase() === 'all' || activeBatchName.toLowerCase() === 'all runs';
        const headerTitle = document.getElementById('pageTitle');
        const breadcrumb = document.getElementById('headerBreadcrumb');
        const trail = document.getElementById('breadcrumbTrail');

        if (headerTitle) {
            if (isAllRuns) {
                headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">format_list_bulleted</span> Runs overview`;
                if (breadcrumb) breadcrumb.style.display = 'none';
            } else {
                let groupIcon = 'trending_up';
                const pageHistoryEl = document.getElementById('pageBatchHistoryOverview');
                const groupType = pageHistoryEl ? pageHistoryEl.getAttribute('data-group-type') : null;
                if (groupType === 'environment') groupIcon = 'dns';
                else if (groupType === 'locale') groupIcon = 'public';
                else if (groupType === 'browser') groupIcon = 'web';
                else if (groupType === 'batch') groupIcon = 'sell';
                else if (groupType === 'area') groupIcon = 'category';

                headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">${groupIcon}</span> ${escapeHtml(activeBatchName)}`;
                if (breadcrumb) breadcrumb.style.display = 'none';
            }
        }
    }

    if (document.getElementById('batchSuiteEvolutionSvg') && !document.getElementById('mainTrendSvg')) {
        renderDynamicSuiteEvolutionChart();
    }

    // Automatically update header title & breadcrumb if Single Run Report view is loaded
    if (document.getElementById('runReportSubTabOverview')) {
        const runReportContainer = document.querySelector('[data-batch-name]');
        if (runReportContainer && runReportContainer.getAttribute('data-batch-name')) {
            const domBatchName = runReportContainer.getAttribute('data-batch-name').trim();
            if (domBatchName) {
                activeBatchName = domBatchName;
            }
        } else {
            const urlBatch = new URLSearchParams(window.location.search).get('batch');
            if (urlBatch && urlBatch.trim()) {
                activeBatchName = urlBatch.trim();
            }
        }

        const runIdLabel = document.querySelector('.run-id-label')?.innerText || '#RUN_ID';
        const headerTitle = document.getElementById('pageTitle');
        if (headerTitle) headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">science</span> Run #${runIdLabel}`;

        const breadcrumb = document.getElementById('headerBreadcrumb');
        if (breadcrumb) breadcrumb.style.display = 'flex';

        const trail = document.getElementById('breadcrumbTrail');
        if (trail) {
            trail.innerHTML = `<span>/</span> <span class="text-main" style="font-weight: 600;">Run #${runIdLabel}</span>`;
        }

        recalculateRunReportMetrics();
        applyRunReportFilters();

        const urlParams = new URLSearchParams(window.location.search);
        let storedSubTab = urlParams.get('subTab');
        if (!storedSubTab) {
            try {
                storedSubTab = sessionStorage.getItem('activeRunReportSubTab');
            } catch (e) {}
        }
        if (storedSubTab) {
            let normalizedSubTab = storedSubTab;
            if (storedSubTab === 'AllTests' || storedSubTab === 'all-tests') normalizedSubTab = 'runReportSubTabAllTests';
            else if (storedSubTab === 'Timeline' || storedSubTab === 'timeline') normalizedSubTab = 'runReportSubTabTimeline';
            else if (storedSubTab === 'Overview') normalizedSubTab = 'runReportSubTabOverview';

            const subTabEl = document.getElementById(normalizedSubTab);
            if (subTabEl && !subTabEl.classList.contains('active')) {
                switchRunReportSubTab(normalizedSubTab, null, true);
            }
        }

        const targetExecId = document.querySelector('[data-target-execution-id]')?.getAttribute('data-target-execution-id')
            || urlParams.get('executionId')
            || urlParams.get('testExecutionId')
            || urlParams.get('variationId')
            || urlParams.get('testName');

        if (targetExecId && lastHandledTargetExecutionId !== targetExecId) {
            focusAndInspectExecution(targetExecId);
        }
    }

    // Automatically expand and select target test variation if Test Base page is loaded
    const testBaseEl = document.getElementById('pageTestBase');
    if (testBaseEl) {
        const urlParams = new URLSearchParams(window.location.search);
        const targetTestName = testBaseEl.getAttribute('data-target-test-name') || urlParams.get('testName');
        const targetDataSet = testBaseEl.getAttribute('data-target-dataset') || urlParams.get('dataSet');
        const targetLocation = testBaseEl.getAttribute('data-target-location') || urlParams.get('location');
        const targetBrowser = testBaseEl.getAttribute('data-target-browser') || urlParams.get('browser');

        if (targetTestName) {
            testBaseEl.removeAttribute('data-target-test-name');
            testBaseEl.removeAttribute('data-target-dataset');
            testBaseEl.removeAttribute('data-target-location');
            testBaseEl.removeAttribute('data-target-browser');

            setTimeout(() => {
                const rows = Array.from(document.querySelectorAll(`#pageTestBase .tb-entry-row[data-test-name="${targetTestName}"]`));
                let targetRow = null;

                if (rows.length > 0) {
                    if (targetDataSet || targetLocation || targetBrowser) {
                        const targetCleanDs = cleanDataSetStringJS(targetDataSet);
                        targetRow = rows.find(r => {
                            const rDs = r.getAttribute('data-dataset') || '';
                            const rCleanDs = cleanDataSetStringJS(rDs);
                            const rLoc = r.getAttribute('data-location') || '';
                            const rBrowser = r.getAttribute('data-browser') || '';

                            let matchDs = true;
                            if (targetDataSet) {
                                matchDs = rDs === targetDataSet || rCleanDs === targetCleanDs || (targetCleanDs !== '' && (rDs.includes(targetCleanDs) || targetDataSet.includes(rCleanDs)));
                            }
                            let matchLoc = true;
                            if (targetLocation) {
                                matchLoc = rLoc.toLowerCase() === targetLocation.toLowerCase();
                            }
                            let matchBrowser = true;
                            if (targetBrowser) {
                                matchBrowser = rBrowser.toLowerCase() === targetBrowser.toLowerCase();
                            }
                            return matchDs && matchLoc && matchBrowser;
                        }) || rows.find(r => {
                            const rDs = r.getAttribute('data-dataset') || '';
                            const rCleanDs = cleanDataSetStringJS(rDs);
                            const targetCleanDs = cleanDataSetStringJS(targetDataSet);
                            return targetDataSet && (rDs === targetDataSet || rCleanDs === targetCleanDs || (targetCleanDs !== '' && (rDs.includes(targetCleanDs) || targetDataSet.includes(rCleanDs))));
                        }) || rows[0];
                    } else {
                        targetRow = rows[0];
                    }
                } else {
                    targetRow = document.querySelector('#pageTestBase .tb-entry-row');
                }

                if (targetRow) {
                    const pane = targetRow.closest('.test-base-area-pane');
                    if (pane) {
                        const paneId = pane.id;
                        const tabBtn = document.querySelector(`[data-pane-id="${paneId}"]`);
                        if (tabBtn) switchTestBaseAreaTab(paneId, tabBtn);
                    }
                    const classGroup = targetRow.closest('.test-class-container');
                    if (classGroup) {
                        classGroup.classList.add('expanded');
                        classGroup.classList.remove('collapsed');
                        const executionsDiv = classGroup.querySelector('.test-class-executions');
                        if (executionsDiv) executionsDiv.style.display = 'block';
                    }
                    // targetRow.scrollIntoView removed: prevent automatic scrolling to list
                    targetRow.click();
                }
            }, 100);
        }
    }


}

function focusAndInspectExecution(targetExecId) {
    if (!targetExecId) return;

    lastHandledTargetExecutionId = targetExecId;
    document.querySelector('[data-target-execution-id]')?.removeAttribute('data-target-execution-id');

    const allTestsBtn = document.getElementById('runReportTabBtnAllTests');
    if (allTestsBtn) {
        switchRunReportSubTab('runReportSubTabAllTests', allTestsBtn);
    }

    setTimeout(() => {
        let rowEl = null;
        try {
            rowEl = document.getElementById(targetExecId) || document.querySelector(`[data-row-id="${CSS.escape(targetExecId)}"]`);
        } catch (e) {
            rowEl = document.getElementById(targetExecId);
        }
        if (!rowEl) {
            try {
                rowEl = document.querySelector(`[data-test-method="${CSS.escape(targetExecId)}"]`)
                    || document.querySelector(`[data-test-name="${CSS.escape(targetExecId)}"]`)
                    || document.querySelector(`[data-search*="${CSS.escape(targetExecId)}"]`);
            } catch (err) {
                // ignore
            }
        }
        if (!rowEl) return;

        let parent = rowEl.parentElement;
        while (parent) {
            if (parent.classList && parent.classList.contains('test-class-container')) {
                parent.style.display = 'block';
                parent.classList.remove('collapsed');
            }
            if (parent.classList && parent.classList.contains('area-group')) {
                parent.style.display = 'block';
                parent.classList.add('expanded');
                parent.classList.remove('collapsed');
            }
            parent = parent.parentElement;
        }

        // rowEl.scrollIntoView removed: prevent automatic scrolling to list
        rowEl.classList.add('highlight-execution-row');
        setTimeout(() => {
            rowEl.classList.remove('highlight-execution-row');
        }, 3500);

        const testName = rowEl.getAttribute('data-test-name') || '';
        const dataSet = rowEl.getAttribute('data-dataset') || '';
        const statusKey = rowEl.getAttribute('data-status') || 'passed-clean';
        const issueTag = rowEl.getAttribute('data-bugs') || '';
        const rowId = rowEl.getAttribute('data-row-id') || rowEl.id;
        const runId = resolveActiveRunId(rowEl.getAttribute('data-run-id'), rowEl);

        openTestSidePagePanel(testName, dataSet, statusKey, issueTag, rowId, runId);
    }, 120);
}

function handleRowClick(e) {
    const row = e.currentTarget;
    const testName = row.getAttribute('data-test-name') || '';
    const dataSet = row.getAttribute('data-dataset') || '';
    const statusKey = row.getAttribute('data-status') || 'passed-clean';
    const issueTag = row.getAttribute('data-bugs') || '';
    const rowId = row.getAttribute('data-row-id') || row.id;
    const runId = resolveActiveRunId(row.getAttribute('data-run-id'), row);

    const isTestBasePage = document.getElementById('pageTestBase') !== null || row.closest('#pageTestBase') !== null || row.closest('#batchTestBaseVariationsPanel') !== null || row.classList.contains('tb-entry-row');

    const panel = document.getElementById('testSidePagePanel');
    const varDrawer = document.getElementById('variationHistoryDrawer');
    const isAlreadyOpenForThisRow = (panel && panel.classList.contains('active') && (currentActiveRowId === rowId || row.classList.contains('selected'))) ||
                                   (varDrawer && varDrawer.classList.contains('active') && (currentActiveRowId === rowId || row.classList.contains('selected')));

    if (isTestBasePage && isAlreadyOpenForThisRow) {
        closeTestSidePagePanel();
        return;
    }

    if (rowId) {
        lastHandledTargetExecutionId = rowId;
    }
    document.querySelector('[data-target-execution-id]')?.removeAttribute('data-target-execution-id');

    if (!isTestBasePage && rowId && window.history && window.history.replaceState) {
        try {
            const url = new URL(window.location.href);
            url.searchParams.set('executionId', rowId);
            window.history.replaceState({}, '', url.toString());
        } catch (err) {
            // Ignore URL parsing errors if any
        }
    }

    closeTestSidePagePanel();
    openTestSidePagePanel(testName, dataSet, statusKey, issueTag, rowId, runId);
}

// Explicitly expose UI controller methods on window for HTML inline handlers
window.switchRunReportSubTab = switchRunReportSubTab;
window.switchTestBaseAreaTab = switchTestBaseAreaTab;
window.filterAllTestsByStatus = filterAllTestsByStatus;
window.applyRunReportFilters = applyRunReportFilters;
window.resetGlobalTestFilters = resetGlobalTestFilters;
window.openAllTestsAndExpandArea = openAllTestsAndExpandArea;
window.toggleStepSection = toggleStepSection;
window.toggleAreaGroup = toggleAreaGroup;
window.toggleSubAreaGroup = toggleSubAreaGroup;
window.toggleTestClassGroup = toggleTestClassGroup;
window.toggleBatchVariationsPanel = toggleBatchVariationsPanel;
window.toggleRunsWidgetPanel = toggleRunsWidgetPanel;
window.toggleAllBatchClassGroups = toggleAllBatchClassGroups;
window.toggleStepActionInspector = toggleStepActionInspector;
window.toggleSubStepInspector = toggleSubStepInspector;
window.closeSidePanelInspector = closeSidePanelInspector;
window.closeTestSidePagePanel = closeTestSidePagePanel;
window.openTestSidePagePanel = openTestSidePagePanel;
window.focusAndInspectExecution = focusAndInspectExecution;
window.redirectToTestBaseVariationHistory = redirectToTestBaseVariationHistory;
window.recalculateRunReportMetrics = recalculateRunReportMetrics;

// Client-side Execution Index Filtering and Sorting matching index.html

function filterByGlobalStatusBadge(statusKey, btnEl) {
    if (currentGlobalStatusBadgeFilter === statusKey || activeWholeExecutionFilter === statusKey) {
        currentGlobalStatusBadgeFilter = 'ALL';
        activeWholeExecutionFilter = null;
    } else {
        currentGlobalStatusBadgeFilter = statusKey;
        activeWholeExecutionFilter = statusKey;
    }

    document.querySelectorAll('#wholeExecutionSummaryBadges .badge-status').forEach(b => b.classList.remove('active-filter', 'active-filter-badge'));

    if (currentGlobalStatusBadgeFilter !== 'ALL') {
        document.querySelectorAll(`#wholeExecutionSummaryBadges .badge-status[data-status-key="${statusKey}"]`).forEach(b => b.classList.add('active-filter', 'active-filter-badge'));
    }

    document.querySelectorAll('#runReportSubTabOverview .metric-card, #runReportSubTabOverview .kpi-card').forEach(card => card.classList.remove('active-filter'));
    if (activeWholeExecutionFilter) {
        const cardMap = {
            'PASSED': 'overviewKpiPassed',
            'SUCCEEDED_FIXED': 'overviewKpiFixed',
            'FAILED_KNOWN': 'overviewKpiKnown',
            'FAILED_UNKNOWN': 'overviewKpiUnknown',
            'SKIPPED': 'overviewKpiIgnored',
            'IGNORED': 'overviewKpiIgnored',
            'RUNNING': 'overviewKpiRunning'
        };
        const cardValId = cardMap[activeWholeExecutionFilter];
        if (cardValId) {
            document.getElementById(cardValId)?.closest('.metric-card, .kpi-card')?.classList.add('active-filter');
        }
    }

    applyFilter();
}

function filterAreaByStatusBadge(badgeEl, statusKey) {
    const areaGroup = badgeEl.closest('.area-group');
    if (!areaGroup) return;

    const currentFilter = areaGroup.getAttribute('data-active-status-filter') || 'ALL';
    const newFilter = currentFilter === statusKey ? 'ALL' : statusKey;

    areaGroup.setAttribute('data-active-status-filter', newFilter);

    if (newFilter !== 'ALL') {
        areaGroup.classList.add('expanded');
    }

    areaGroup.querySelectorAll('.area-summary-badges .badge-status').forEach(b => b.classList.remove('active-filter-badge'));
    if (newFilter !== 'ALL') {
        badgeEl.classList.add('active-filter-badge');
    }

    applyFilter();
}

function filterSubAreaByStatusBadge(badgeEl, statusKey) {
    const subAreaGroup = badgeEl.closest('.sub-area-group');
    if (!subAreaGroup) return;

    const currentFilter = subAreaGroup.getAttribute('data-active-status-filter') || 'ALL';
    const newFilter = currentFilter === statusKey ? 'ALL' : statusKey;

    subAreaGroup.setAttribute('data-active-status-filter', newFilter);

    if (newFilter !== 'ALL') {
        subAreaGroup.classList.add('expanded');
        const parentArea = subAreaGroup.closest('.area-group');
        if (parentArea) parentArea.classList.add('expanded');
    }

    subAreaGroup.querySelectorAll('.sub-area-summary-badges .badge-status').forEach(b => b.classList.remove('active-filter-badge'));
    if (newFilter !== 'ALL') {
        badgeEl.classList.add('active-filter-badge');
    }

    applyFilter();
}

function filterClassByStatusBadge(badgeEl, statusKey) {
    const classContainer = badgeEl.closest('.test-class-container');
    if (!classContainer) return;

    const currentFilter = classContainer.getAttribute('data-active-status-filter') || 'ALL';
    const newFilter = currentFilter === statusKey ? 'ALL' : statusKey;

    classContainer.setAttribute('data-active-status-filter', newFilter);

    if (newFilter !== 'ALL') {
        classContainer.classList.add('expanded');
        const folderIcon = classContainer.querySelector('.test-class-header .material-symbols-outlined.text-accent');
        if (folderIcon) folderIcon.textContent = 'folder_open';
    }

    classContainer.querySelectorAll('.test-class-header .badge-status').forEach(b => b.classList.remove('active-filter-badge'));
    if (newFilter !== 'ALL') {
        badgeEl.classList.add('active-filter-badge');
    }

    applyFilter();
}

function filterByStatus(status) {
    currentExecutionStatusFilter = status || 'ALL';
    applyFilter();
}

function filterByMode(mode) {
    currentExecutionModeFilter = mode || 'ALL';
    const sel = document.getElementById('modeFilter');
    if (sel && sel.value !== currentExecutionModeFilter) {
        sel.value = currentExecutionModeFilter;
    }
    const modeMenu = document.getElementById('reportModesMultiselectMenu');
    if (modeMenu) {
        const allChk = modeMenu.querySelector('.js-chk-all');
        const optionChks = modeMenu.querySelectorAll('.js-chk-option');
        if (mode === 'ALL' || !mode) {
            if (allChk) allChk.checked = true;
            optionChks.forEach(o => o.checked = true);
        } else {
            if (allChk) allChk.checked = false;
            optionChks.forEach(o => o.checked = (o.value.toUpperCase() === mode.toUpperCase()));
        }
        const summaryEl = modeMenu.closest('.multiselect-dropdown')?.querySelector('.js-selected-text');
        if (summaryEl) {
            summaryEl.innerText = (mode === 'ALL' || !mode) ? 'All Selected' : '1 Selected';
        }
    }
    applyFilter();
}

function setHealedFilter(mode) {
    currentHealedFilter = mode || 'ALL';
    document.querySelectorAll('#healedFilterToggle .segment-btn').forEach(btn => {
        if (btn.getAttribute('data-val') === currentHealedFilter) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
    applyFilter();
}

function filterAllTestsByHealedStatus(status, healedFilter) {
    switchRunReportSubTab('runReportSubTabAllTests', document.getElementById('runReportTabBtnAllTests'));
    if (healedFilter) {
        setHealedFilter(healedFilter);
    }
    if (status) {
        currentGlobalStatusBadgeFilter = status;
        activeWholeExecutionFilter = status;
        document.querySelectorAll('#wholeExecutionSummaryBadges .badge-status').forEach(b => b.classList.remove('active-filter', 'active-filter-badge'));
        document.querySelectorAll(`#wholeExecutionSummaryBadges .badge-status[data-status-key="${status}"]`).forEach(b => b.classList.add('active-filter', 'active-filter-badge'));

        document.querySelectorAll('#runReportSubTabOverview .metric-card, #runReportSubTabOverview .kpi-card').forEach(card => card.classList.remove('active-filter'));
        const cardMap = {
            'PASSED': 'overviewKpiPassed',
            'SUCCEEDED_FIXED': 'overviewKpiFixed',
            'FAILED_KNOWN': 'overviewKpiKnown',
            'FAILED_UNKNOWN': 'overviewKpiUnknown',
            'SKIPPED': 'overviewKpiIgnored',
            'IGNORED': 'overviewKpiIgnored',
            'RUNNING': 'overviewKpiRunning'
        };
        const cardValId = cardMap[status];
        if (cardValId) {
            document.getElementById(cardValId)?.closest('.metric-card, .kpi-card')?.classList.add('active-filter');
        }

        applyFilter();
        if (typeof applyRunReportFilters === 'function') {
            applyRunReportFilters();
        }
    } else {
        applyFilter();
    }
}

function populateBrowserAndLocaleFilters() {
    const modeMenu = document.getElementById('reportModesMultiselectMenu');
    const browserMenu = document.getElementById('reportBrowsersMultiselectMenu');
    const localeMenu = document.getElementById('reportLocalesMultiselectMenu');

    if (modeMenu || browserMenu || localeMenu) {
        const rows = document.querySelectorAll('#runReportSubTabAllTests tr.execution-row, #runReportSubTabAllTests tr.test-row');
        const modes = new Set();
        const browsers = new Set();
        const locales = new Set();

        rows.forEach(r => {
            const m = r.getAttribute('data-mode');
            const b = r.getAttribute('data-browser');
            const l = r.getAttribute('data-location');
            if (m && m.trim() && m !== 'null') modes.add(m.trim());
            if (b && b.trim() && b !== 'null') browsers.add(b.trim());
            if (l && l.trim() && l !== 'null') locales.add(l.trim());
        });

        function syncMenu(menu, set, allLabel) {
            if (!menu) return;
            const existing = menu.querySelectorAll('.js-chk-option');
            if (existing.length === 0 && set.size > 0) {
                const menuId = menu.id;
                let html = `<label class="multiselect-option"><input type="checkbox" class="js-chk-all" data-menu-id="${menuId}" value="ALL" checked> <strong>${allLabel}</strong></label><div class="multiselect-divider"></div>`;
                Array.from(set).sort().forEach(val => {
                    html += `<label class="multiselect-option"><input type="checkbox" class="js-chk-option" data-menu-id="${menuId}" value="${val}" checked> <span>${val}</span></label>`;
                });
                menu.innerHTML = html;
            }
        }

        syncMenu(modeMenu, modes, 'Select All Modes');
        syncMenu(browserMenu, browsers, 'Select All Browsers');
        syncMenu(localeMenu, locales, 'Select All Locales');
        return;
    }

    const browserSelect = document.getElementById('browserFilter');
    const localeSelect = document.getElementById('localeFilter');
    if (!browserSelect && !localeSelect) return;

    const rows = document.querySelectorAll('tr.execution-row, tr.test-row');
    const browsers = new Set();
    const locales = new Set();

    rows.forEach(r => {
        const b = r.getAttribute('data-browser');
        const l = r.getAttribute('data-location');
        if (b && b.trim() && b !== 'null') browsers.add(b.trim());
        if (l && l.trim() && l !== 'null') locales.add(l.trim());
    });

    if (browserSelect && browserSelect.options.length <= 1) {
        const currentB = browserSelect.value || 'ALL';
        browserSelect.innerHTML = '<option value="ALL">All Browsers</option>';
        Array.from(browsers).sort().forEach(b => {
            const opt = document.createElement('option');
            opt.value = b;
            opt.textContent = b;
            browserSelect.appendChild(opt);
        });
        browserSelect.value = currentB;
    }

    if (localeSelect && localeSelect.options.length <= 1) {
        const currentL = localeSelect.value || 'ALL';
        localeSelect.innerHTML = '<option value="ALL">All Locales</option>';
        Array.from(locales).sort().forEach(l => {
            const opt = document.createElement('option');
            opt.value = l;
            opt.textContent = l;
            localeSelect.appendChild(opt);
        });
        localeSelect.value = currentL;
    }
}

function applyFilter() {
    if (document.getElementById('runReportSubTabAllTests')) {
        applyRunReportFilters();
        return;
    }

    const searchInput = document.getElementById('searchInput');
    const search = searchInput ? (searchInput.value || '').trim().toLowerCase() : '';
    const browserSelect = document.getElementById('browserFilter');
    const localeSelect = document.getElementById('localeFilter');
    const selBrowser = browserSelect ? browserSelect.value : 'ALL';
    const selLocale = localeSelect ? localeSelect.value : 'ALL';

    const rows = document.querySelectorAll('tr.execution-row, tr.test-row');

    rows.forEach(function(row) {
        const rowStatus = (row.getAttribute('data-status') || '').toUpperCase();
        const rowStatusRaw = (row.getAttribute('data-status-raw') || '').toLowerCase();
        const rowMode = (row.getAttribute('data-mode') || '').toUpperCase();
        const rowBrowser = (row.getAttribute('data-browser') || '').trim();
        const rowLocale = (row.getAttribute('data-location') || '').trim();
        const rowSearch = (row.getAttribute('data-search') || (row.innerText || '')).toLowerCase();

        const areaGroup = row.closest('.area-group');
        const areaStatusFilter = areaGroup ? (areaGroup.getAttribute('data-active-status-filter') || 'ALL') : 'ALL';
        const subAreaGroup = row.closest('.sub-area-group');
        const subAreaStatusFilter = subAreaGroup ? (subAreaGroup.getAttribute('data-active-status-filter') || 'ALL') : 'ALL';

        const classContainer = row.closest('.test-class-container');
        const classStatusFilter = classContainer ? (classContainer.getAttribute('data-active-status-filter') || 'ALL') : 'ALL';

        function matchesStatusKey(filterKey) {
            if (!filterKey || filterKey === 'ALL') return true;
            const key = filterKey.toUpperCase();
            if (key === 'FLAKY') return row.getAttribute('data-flaky') === 'true';
            if (key === 'PASSED') return rowStatus === 'PASSED' || rowStatusRaw === 'passed-clean' || rowStatusRaw === 'passed' || rowStatusRaw === 'succeeded';
            if (key === 'SUCCEEDED_FIXED' || key === 'FIXED') return rowStatus === 'SUCCEEDED_FIXED' || rowStatusRaw === 'succeeded-fixed' || rowStatusRaw === 'fixed';
            if (key === 'FAILED_KNOWN' || key === 'KNOWN') return rowStatus === 'FAILED_KNOWN' || rowStatusRaw === 'failed-known' || rowStatusRaw === 'known';
            if (key === 'FAILED_UNKNOWN' || key === 'UNKNOWN') return rowStatus === 'FAILED_UNKNOWN' || rowStatusRaw === 'failed-unknown' || rowStatusRaw === 'failed' || rowStatusRaw === 'error';
            if (key === 'SKIPPED' || key === 'IGNORED') return rowStatus === 'SKIPPED' || rowStatusRaw === 'ignored' || rowStatusRaw === 'skipped';
            if (key === 'FAILED') return rowStatus.includes('FAIL') || rowStatusRaw.includes('failed');
            return rowStatus.includes(key);
        }

        const matchesGlobalBadge = matchesStatusKey(currentGlobalStatusBadgeFilter);
        const matchesAreaBadge = matchesStatusKey(areaStatusFilter);
        const matchesSubAreaBadge = matchesStatusKey(subAreaStatusFilter);
        const matchesClassBadge = matchesStatusKey(classStatusFilter);
        const matchesLegacyStatus = matchesStatusKey(currentExecutionStatusFilter);

        const isHealed = row.getAttribute('data-healed') === 'true' || Number(row.getAttribute('data-healed-count') || 0) > 0 || rowStatusRaw === 'healed';
        const matchesHealed = (currentHealedFilter === 'ALL') ||
                              (currentHealedFilter === 'HEALED' && isHealed) ||
                              (currentHealedFilter === 'NOT_HEALED' && !isHealed);

        const matchesMode = (currentExecutionModeFilter === 'ALL' || rowMode === currentExecutionModeFilter.toUpperCase() || rowMode.includes(currentExecutionModeFilter.toUpperCase()));
        const matchesBrowser = (selBrowser === 'ALL' || rowBrowser.toLowerCase() === selBrowser.toLowerCase());
        const matchesLocale = (selLocale === 'ALL' || rowLocale.toLowerCase() === selLocale.toLowerCase());
        const matchesSearch = (!search || rowSearch.indexOf(search) !== -1);

        if (matchesGlobalBadge && matchesAreaBadge && matchesSubAreaBadge && matchesClassBadge && matchesLegacyStatus && matchesMode && matchesHealed && matchesBrowser && matchesLocale && matchesSearch) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });

    document.querySelectorAll('.test-class-container').forEach(classBox => {
        const visibleRows = classBox.querySelectorAll('tr.execution-row:not([style*="display: none"]), tr.test-row:not([style*="display: none"])');
        classBox.style.display = visibleRows.length > 0 ? '' : 'none';
    });

    document.querySelectorAll('.sub-area-group').forEach(subAreaBox => {
        const visibleSubAreaRows = subAreaBox.querySelectorAll('tr.execution-row:not([style*="display: none"]), tr.test-row:not([style*="display: none"])');
        subAreaBox.style.display = visibleSubAreaRows.length > 0 ? '' : 'none';
    });

    document.querySelectorAll('.area-group').forEach(areaBox => {
        const visibleAreaRows = areaBox.querySelectorAll('tr.execution-row:not([style*="display: none"]), tr.test-row:not([style*="display: none"])');
        areaBox.style.display = visibleAreaRows.length > 0 ? '' : 'none';
    });
}

let sortDirectionsMap = {};
function sortTable(colIndex) {
    const table = document.getElementById('executionsTable') || document.querySelector('.aura-table');
    if (!table) return;
    const tbody = table.querySelector('tbody');
    if (!tbody) return;
    const rows = Array.from(tbody.querySelectorAll('tr.execution-row, tr.test-row'));
    if (rows.length <= 1) return;

    const isAsc = sortDirectionsMap[colIndex] = !sortDirectionsMap[colIndex];

    rows.sort(function(a, b) {
        const aCell = a.children[colIndex];
        const bCell = b.children[colIndex];
        if (!aCell || !bCell) return 0;

        const aVal = aCell.getAttribute('data-sort') || aCell.textContent.trim();
        const bVal = bCell.getAttribute('data-sort') || bCell.textContent.trim();

        const aNum = parseFloat(aVal);
        const bNum = parseFloat(bVal);

        if (!isNaN(aNum) && !isNaN(bNum)) {
            return isAsc ? (aNum - bNum) : (bNum - aNum);
        }
        return isAsc ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
    });

    rows.forEach(function(row) {
        tbody.appendChild(row);
    });
}

window.filterByGlobalStatusBadge = filterByGlobalStatusBadge;
window.filterAreaByStatusBadge = filterAreaByStatusBadge;
window.filterSubAreaByStatusBadge = filterSubAreaByStatusBadge;
window.filterClassByStatusBadge = filterClassByStatusBadge;
window.filterByStatus = filterByStatus;
window.filterByMode = filterByMode;
window.applyFilter = applyFilter;
window.sortTable = sortTable;

// Listen for bugUpdated trigger from backend
document.body.addEventListener('bugUpdated', function(evt) {
    const data = evt.detail;
    if (!data) return;
    const rowId = data.rowId;
    if (rowId) {
        if (data.bugs) {
            if (data.bugs.length > 0) {
                executionBugMap[rowId] = data.bugs;
            } else {
                delete executionBugMap[rowId];
            }
        }
        const row = document.getElementById(rowId);
        if (row && data.status) {
            row.setAttribute('data-status', data.status);
        }
        updateRowStatusAndMetrics(rowId);
        if (typeof onBugSavedSuccessfully === 'function') {
            onBugSavedSuccessfully();
        }
    }
});

/**
 * Interactive Lightbox Image Modal
 */
function openImageModal(imgSrc, title) {
    let modalEl = document.getElementById('imageLightboxModal');
    if (!modalEl) {
        modalEl = document.createElement('div');
        modalEl.id = 'imageLightboxModal';
        modalEl.className = 'image-lightbox-modal';
        modalEl.onclick = function (e) {
            if (e.target === modalEl || e.target.classList.contains('lightbox-backdrop')) {
                closeImageModal();
            }
        };
        document.body.appendChild(modalEl);
    }

    modalEl.innerHTML = `
        <div class="lightbox-backdrop"></div>
        <div class="lightbox-content" onclick="event.stopPropagation();">
            <div class="lightbox-header">
                <span class="lightbox-title">${title || 'Screenshot Preview'}</span>
                <div class="lightbox-actions">
                    <a href="${imgSrc}" target="_blank" download="screenshot.png" class="lightbox-btn" title="Open in new tab / Download">
                        <span class="material-symbols-outlined">open_in_new</span>
                    </a>
                    <button class="lightbox-btn" onclick="closeImageModal()" title="Close (Esc)">
                        <span class="material-symbols-outlined">close</span>
                    </button>
                </div>
            </div>
            <div class="lightbox-body">
                <img src="${imgSrc}" class="lightbox-image" alt="${title || 'Screenshot'}">
            </div>
        </div>
    `;

    modalEl.classList.add('active');
    document.body.style.overflow = 'hidden';

    const onKeydown = function (e) {
        if (e.key === 'Escape') {
            closeImageModal();
            document.removeEventListener('keydown', onKeydown);
        }
    };
    document.addEventListener('keydown', onKeydown);
}

function closeImageModal() {
    const modalEl = document.getElementById('imageLightboxModal');
    if (modalEl) {
        modalEl.classList.remove('active');
    }
    document.body.style.overflow = '';
}

window.openImageModal = openImageModal;
window.closeImageModal = closeImageModal;

function escapeTextHelper(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function loadTestBaseVariationHistory(testClass, testMethod, dataSet, location, browser) {
    let methodVal = testMethod;
    let dataSetVal = dataSet;
    let locVal = location;
    let browserVal = browser;

    if (arguments.length === 4) {
        methodVal = '';
        dataSetVal = arguments[1];
        locVal = arguments[2];
        browserVal = arguments[3];
    }

    const tbody = document.getElementById('tbVariationHistoryTableBody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted p-3" style="font-size: 0.82rem;"><span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Loading execution history...</td></tr>';

    const url = `/fragments/test-base/variation-history?testClass=${encodeURIComponent(testClass || '')}&testMethod=${encodeURIComponent(methodVal || '')}&dataSet=${encodeURIComponent(dataSetVal || '')}&location=${encodeURIComponent(locVal || '')}&browser=${encodeURIComponent(browserVal || '')}`;

    fetch(url, { headers: { 'HX-Request': 'true' } })
        .then(res => res.text())
        .then(html => {
            tbody.innerHTML = html;
            if (window.htmx) htmx.process(tbody);
            populateTbSideBatchMultiselect();
        })
        .catch(() => {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger p-3" style="font-size: 0.82rem;">Failed to load variation history.</td></tr>';
        });
}

function populateTbSideBatchMultiselect() {
    const menu = document.getElementById('tbSideBatchMultiselectMenu');
    if (!menu) return;

    const rows = document.querySelectorAll('#tbVariationHistoryTableBody tr.history-var-row');
    const batches = new Set();
    rows.forEach(r => {
        const b = r.getAttribute('data-batch');
        if (b) batches.add(b);
    });

    if (batches.size > 0) {
        let html = '<label class="multiselect-option"><input type="checkbox" class="js-chk-all" data-menu-id="tbSideBatchMultiselectMenu" value="ALL" checked> <strong>Select All Batches</strong></label><div class="multiselect-divider"></div>';
        batches.forEach(b => {
            html += `<label class="multiselect-option"><input type="checkbox" class="js-chk-option" data-menu-id="tbSideBatchMultiselectMenu" value="${escapeTextHelper(b)}" checked> <span>${escapeTextHelper(b)}</span></label>`;
        });
        menu.innerHTML = html;
    }
}

function filterTbVariationHistoryTable() {
    const menu = document.getElementById('tbSideBatchMultiselectMenu');
    if (!menu) return;

    const checkedBoxes = Array.from(menu.querySelectorAll('.js-chk-option:checked')).map(cb => cb.value);
    const allChecked = menu.querySelector('.js-chk-all')?.checked || false;

    const rows = document.querySelectorAll('#tbVariationHistoryTableBody tr.history-var-row');
    rows.forEach(r => {
        const batch = r.getAttribute('data-batch') || '';
        if (allChecked || checkedBoxes.includes(batch)) {
            r.style.display = '';
        } else {
            r.style.display = 'none';
        }
    });
}

function initPanelResizer() {
    const resizer = document.getElementById('allurePanelResizer') || document.getElementById('panelResizer');
    const panel = document.getElementById('testSidePagePanel');
    if (!resizer || !panel) return;
    if (resizer.dataset.resizerBound === 'true') return;
    resizer.dataset.resizerBound = 'true';

    let isDragging = false;
    let startX = 0;
    let startWidth = 0;

    resizer.addEventListener('mousedown', (e) => {
        isDragging = true;
        startX = e.clientX;
        startWidth = panel.offsetWidth;
        resizer.classList.add('is-resizing');
        document.body.style.cursor = 'col-resize';
        document.body.style.userSelect = 'none';
        e.preventDefault();
    });

    document.addEventListener('mousemove', (e) => {
        if (!isDragging) return;
        const deltaX = startX - e.clientX;
        const minWidth = 320;
        const maxWidth = Math.max(minWidth, window.innerWidth - 420);
        const newWidth = Math.min(Math.max(startWidth + deltaX, minWidth), maxWidth);
        panel.style.width = newWidth + 'px';
    });

    document.addEventListener('mouseup', () => {
        if (!isDragging) return;
        isDragging = false;
        resizer.classList.remove('is-resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        if (panel.offsetWidth) {
            localStorage.setItem('aura_side_panel_width', panel.offsetWidth);
        }
    });
}

let variationHistorySortDirections = {};
function sortVariationHistoryTable(colIndex) {
    const tbody = document.getElementById('tbVariationHistoryTableBody');
    if (!tbody) return;
    const rows = Array.from(tbody.querySelectorAll('tr.history-var-row'));
    if (rows.length <= 1) return;

    const isAsc = variationHistorySortDirections[colIndex] = !variationHistorySortDirections[colIndex];

    const table = tbody.closest('table');
    if (table) {
        const ths = table.querySelectorAll('thead th');
        ths.forEach((th, idx) => {
            const icon = th.querySelector('.sort-icon');
            if (icon) {
                if (idx === colIndex) {
                    icon.textContent = isAsc ? 'arrow_upward' : 'arrow_downward';
                } else {
                    icon.textContent = 'unfold_more';
                }
            }
        });
    }

    rows.sort(function(a, b) {
        const aCell = a.children[colIndex];
        const bCell = b.children[colIndex];
        if (!aCell || !bCell) return 0;

        const aVal = (aCell.getAttribute('data-sort') || aCell.textContent || '').trim();
        const bVal = (bCell.getAttribute('data-sort') || bCell.textContent || '').trim();

        if (colIndex === 3) {
            const aEpoch = parseDateToEpochHelper(aVal);
            const bEpoch = parseDateToEpochHelper(bVal);
            return isAsc ? (aEpoch - bEpoch) : (bEpoch - aEpoch);
        }

        const aNum = parseFloat(aVal);
        const bNum = parseFloat(bVal);
        if (!isNaN(aNum) && !isNaN(bNum)) {
            return isAsc ? (aNum - bNum) : (bNum - aNum);
        }
        return isAsc ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
    });

    rows.forEach(function(row) {
        tbody.appendChild(row);
    });
}

function parseDateToEpochHelper(val) {
    if (!val || val === 'Recently') return Number.MAX_SAFE_INTEGER;
    const clean = val.trim();
    const d = new Date(clean.replace(' ', 'T'));
    if (!isNaN(d.getTime())) return d.getTime();
    return 0;
}

window.loadTestBaseVariationHistory = loadTestBaseVariationHistory;
window.populateTbSideBatchMultiselect = populateTbSideBatchMultiselect;
window.filterTbVariationHistoryTable = filterTbVariationHistoryTable;
window.initPanelResizer = initPanelResizer;
window.sortVariationHistoryTable = sortVariationHistoryTable;

function deleteRunFromHistory(runId, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    if (!runId) return;

    const cleanRunId = String(runId).replace(/^#+/, '');
    if (!confirm(`Are you sure you want to remove run #${cleanRunId} from history?\n\nThis will remove it from all run tables, batch history, and Test Base variation history.`)) {
        return;
    }

    const currentUrl = window.location.href;
    fetch(`/runs/${encodeURIComponent(cleanRunId)}/delete`, {
        method: 'POST',
        headers: {
            'HX-Request': 'true',
            'HX-Current-URL': currentUrl
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`Failed to remove run: HTTP ${response.status}`);
        }
        return response.text();
    })
    .then(html => {
        const container = document.getElementById('mainViewContainer');
        if (container) {
            container.innerHTML = html;
            if (window.htmx) {
                htmx.process(container);
            }
            bindGlobalListeners();
        }
        // If we are currently showing a side panel for variation history, reload it
        const varDrawer = document.getElementById('variationHistoryDrawer');
        if (varDrawer && varDrawer.classList.contains('active')) {
            const activeRow = document.querySelector('.tb-entry-row.selected');
            if (activeRow) {
                const testClass = activeRow.getAttribute('data-test-name') || '';
                const testMethod = activeRow.getAttribute('data-test-method') || '';
                const dataSet = activeRow.getAttribute('data-dataset') || '';
                const loc = activeRow.getAttribute('data-location') || '';
                const browser = activeRow.getAttribute('data-browser') || '';
                loadTestBaseVariationHistory(testClass, testMethod, dataSet, loc, browser);
            }
        }
    })
    .catch(err => {
        alert('Could not remove run from history: ' + err.message);
    });
}

window.deleteRunFromHistory = deleteRunFromHistory;

if (typeof window.runBatch !== 'function') {
    window.runBatch = async function(batchName) {
        if (!batchName) return;

        const btns = document.querySelectorAll(`[data-batch-name="${batchName}"]`);
        btns.forEach(b => {
            b.disabled = true;
            b.style.opacity = '0.7';
        });

        if (typeof showToast === 'function') {
            showToast(`Starting batch "${batchName}"...`, 'info');
        }

        try {
            if (typeof prepareClientForExecution === 'function') {
                prepareClientForExecution();
            }

            const response = await fetch(`/api/batch/run?batchName=${encodeURIComponent(batchName)}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });

            const data = await response.json();
            if (!response.ok || data.status === 'error' || data.error) {
                const msg = data.error || data.message || 'Failed to start batch execution.';
                if (typeof showToast === 'function') {
                    showToast(msg, 'error');
                } else {
                    alert(msg);
                }
            } else {
                if (typeof showToast === 'function') {
                    showToast(`Batch "${batchName}" execution started!`, 'success');
                }
                const pulse = document.getElementById('sidebarContextPulseDot');
                if (pulse) pulse.style.display = 'inline-block';
                const card = document.getElementById('sidebarContextCard');
                if (card) card.style.display = 'flex';
                const sidebarBatchNameEl = document.getElementById('sidebarActiveBatchName');
                if (sidebarBatchNameEl) sidebarBatchNameEl.textContent = batchName;
                try {
                    localStorage.setItem('aura_active_batch_name', batchName);
                } catch (e) {}
            }
        } catch (e) {
            if (typeof showToast === 'function') {
                showToast('Failed to trigger batch run: ' + e.message, 'error');
            } else {
                alert('Failed to trigger batch run: ' + e.message);
            }
        } finally {
            btns.forEach(b => {
                b.disabled = false;
                b.style.opacity = '1';
            });
        }
    };
}

/* =========================================================================
 * TOKEN COST BREAKDOWN MODAL & HISTORICAL GROUP NAVIGATION
 * ========================================================================= */
let activeTokenCostGroupIndex = -1;

function formatCostWithSameUnit(cost, templateText) {
    if (!templateText) return `$${cost.toFixed(4)}`;
    const match = templateText.match(/^([^0-9.-]*)[0-9.,]+([^0-9.-]*)$/);
    const prefix = (match && match[1]) ? match[1] : '';
    const suffix = (match && match[2]) ? match[2] : '';
    return `${prefix}${cost.toFixed(4)}${suffix}`;
}

function extractFallbackTokenCostGroupFromDom() {
    const totalCostEl = document.getElementById('batchTokenCostValue');
    const tokensBadgeEl = document.getElementById('batchTokenCostTokensBadge');
    const deltaEl = document.getElementById('batchTokenCostDelta');

    const costText = totalCostEl ? totalCostEl.textContent.trim() : '$0.0000';
    const tokensText = tokensBadgeEl ? tokensBadgeEl.textContent.trim() : '0 tokens';
    const deltaText = deltaEl ? deltaEl.textContent.trim() : '+$0.0000 vs prev';

    const numericCost = parseFloat(costText.replace(/[^0-9.-]/g, '')) || 0.0;
    const numericTokens = parseInt(tokensText.replace(/[^0-9]/g, ''), 10) || 0;

    const varEntries = document.querySelectorAll('#batchLatestVariationsData .batch-latest-var-entry');
    const runMap = new Map();

    if (varEntries && varEntries.length > 0) {
        varEntries.forEach(entry => {
            const runId = entry.getAttribute('data-run-id') || 'latest';
            const cost = parseFloat(entry.getAttribute('data-llm-cost')) || 0.0;
            const tokens = parseInt(entry.getAttribute('data-llm-tokens'), 10) || 0;
            const browser = entry.getAttribute('data-browser') || '';
            const env = entry.getAttribute('data-env') || '';
            const batch = entry.getAttribute('data-batch') || '';

            if (!runMap.has(runId)) {
                runMap.set(runId, {
                    runId: runId,
                    timestampLabel: 'Recently',
                    browser: browser,
                    environment: env,
                    batchName: batch,
                    contributedTestCount: 0,
                    contributedTokens: 0,
                    contributedCost: 0.0
                });
            }
            const r = runMap.get(runId);
            r.contributedTestCount += 1;
            r.contributedCost += cost;
            r.contributedTokens += tokens;
        });
    } else {
        const runElements = document.querySelectorAll('#batchContributingRunsList .contributing-run-item');
        runElements.forEach(item => {
            const runId = item.getAttribute('data-run-id') || item.querySelector('.contributing-run-id')?.textContent?.trim() || 'run';
            const time = item.querySelector('.contributing-run-time')?.textContent?.trim() || 'Recently';
            const countText = item.querySelector('.contributing-run-count')?.textContent?.trim() || '(0 tests)';
            const testCount = parseInt(countText.replace(/[^0-9]/g, ''), 10) || 0;
            const browser = item.querySelector('.chip-browser')?.textContent?.trim() || '';
            const env = item.querySelector('.chip-env')?.textContent?.trim() || '';
            const batch = item.querySelector('.chip-tag')?.textContent?.trim() || '';

            runMap.set(runId, {
                runId: runId,
                timestampLabel: time,
                browser: browser,
                environment: env,
                batchName: batch,
                contributedTestCount: testCount,
                contributedTokens: numericTokens,
                contributedCost: numericCost
            });
        });
    }

    const runsList = Array.from(runMap.values());
    runsList.forEach(r => {
        r.contributedCostFormatted = formatCostWithSameUnit(r.contributedCost, costText);
        r.contributedTokensFormatted = r.contributedTokens.toLocaleString() + ' tokens';
        const pct = numericCost > 0 ? (r.contributedCost / numericCost) * 100.0 : 0.0;
        r.costPercentage = pct;
        r.costPercentageFormatted = `${pct.toFixed(1)}%`;
    });

    return {
        pointIndex: 0,
        pointLabel: 'Day 1 of 1 (Latest)',
        runId: runsList.length > 0 ? runsList[0].runId : 'latest',
        timestampLabel: 'Today',
        totalCost: numericCost,
        totalCostFormatted: costText,
        totalTokens: numericTokens,
        totalTokensFormatted: tokensText,
        deltaCost: 0.0,
        deltaLabel: deltaText,
        contributingRuns: runsList
    };
}

function getTokenCostPointGroupsData() {
    const jsonEl = document.getElementById('tokenCostTrendGroupsJson');
    if (jsonEl && jsonEl.textContent.trim()) {
        let rawText = jsonEl.textContent.trim();
        if (rawText.includes('&quot;') || rawText.includes('&amp;') || rawText.includes('&#')) {
            const textarea = document.createElement('textarea');
            textarea.innerHTML = rawText;
            rawText = textarea.value;
        }
        try {
            const data = JSON.parse(rawText);
            if (Array.isArray(data) && data.length > 0) {
                return data;
            }
        } catch (e) {
            console.error('Failed to parse token cost point groups JSON:', e, rawText);
        }
    }

    const fallback = extractFallbackTokenCostGroupFromDom();
    if (fallback && fallback.contributingRuns && fallback.contributingRuns.length > 0) {
        return [fallback];
    }
    return [];
}

function openTokenCostBreakdownModal(pointIndex) {
    const modal = document.getElementById('tokenCostBreakdownModal');
    if (!modal) return;

    const groups = getTokenCostPointGroupsData();
    if (!groups || groups.length === 0) {
        modal.style.display = 'flex';
        return;
    }

    if (typeof pointIndex === 'number' && pointIndex >= 0 && pointIndex < groups.length) {
        activeTokenCostGroupIndex = pointIndex;
    } else {
        activeTokenCostGroupIndex = groups.length - 1;
    }

    modal.style.display = 'flex';
    renderTokenCostBreakdown(activeTokenCostGroupIndex);
}

function closeTokenCostBreakdownModal() {
    const modal = document.getElementById('tokenCostBreakdownModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function navigateTokenCostGroup(step) {
    const groups = getTokenCostPointGroupsData();
    if (!groups || groups.length === 0) return;

    const newIdx = activeTokenCostGroupIndex + step;
    if (newIdx >= 0 && newIdx < groups.length) {
        activeTokenCostGroupIndex = newIdx;
        renderTokenCostBreakdown(activeTokenCostGroupIndex);
    }
}

function renderTokenCostBreakdown(idx) {
    const groups = getTokenCostPointGroupsData();
    if (!groups || groups.length === 0 || idx < 0 || idx >= groups.length) return;

    activeTokenCostGroupIndex = idx;
    const group = groups[idx];

    const runsList = group.contributingRuns || [];
    let totalContributedTests = 0;
    runsList.forEach(r => { totalContributedTests += (r.contributedTestCount || 0); });

    const prevBtn = document.getElementById('btnPrevCostGroup');
    const nextBtn = document.getElementById('btnNextCostGroup');
    if (prevBtn) prevBtn.disabled = (idx <= 0);
    if (nextBtn) nextBtn.disabled = (idx >= groups.length - 1);

    const titleEl = document.getElementById('tokenCostModalPointTitle');
    const subtitleEl = document.getElementById('tokenCostModalPointSubtitle');
    if (titleEl) {
        const isLatest = (idx === groups.length - 1);
        titleEl.textContent = `Day ${idx + 1} of ${groups.length}${isLatest ? ' (Latest)' : ''}`;
    }
    if (subtitleEl) {
        subtitleEl.textContent = `${group.timestampLabel || 'Today'} (${runsList.length} run${runsList.length === 1 ? '' : 's'})`;
    }

    const totalValEl = document.getElementById('tokenCostModalTotalVal');
    const tokensBadgeEl = document.getElementById('tokenCostModalTokensBadge');
    const deltaValEl = document.getElementById('tokenCostModalDeltaVal');
    const runsCountEl = document.getElementById('tokenCostModalRunsCount');
    const testsCountEl = document.getElementById('tokenCostModalTestsCount');

    if (totalValEl) totalValEl.textContent = group.totalCostFormatted || '$0.0000';
    if (tokensBadgeEl) tokensBadgeEl.textContent = group.totalTokensFormatted || '0 tokens';
    if (deltaValEl) {
        deltaValEl.textContent = group.deltaLabel || '$0.0000 vs prev day';
        deltaValEl.classList.toggle('negative', group.deltaCost < 0);
    }

    if (runsCountEl) {
        runsCountEl.textContent = `${runsList.length} Contributing Run${runsList.length === 1 ? '' : 's'}`;
    }
    if (testsCountEl) {
        testsCountEl.textContent = `${totalContributedTests} Test${totalContributedTests === 1 ? '' : 's'}`;
    }

    const tbody = document.getElementById('tokenCostModalRunsTableBody');
    const noRunsMsg = document.getElementById('tokenCostModalNoRunsMsg');

    if (tbody) {
        tbody.innerHTML = '';
        if (runsList.length === 0) {
            if (noRunsMsg) noRunsMsg.style.display = '';
        } else {
            if (noRunsMsg) noRunsMsg.style.display = 'none';
            runsList.forEach(r => {
                const tr = document.createElement('tr');
                tr.className = 'clickable-row';

                let chipsHtml = '';
                if (r.browser && r.browser !== 'Default') {
                    chipsHtml += `<span class="run-meta-chip chip-browser">${escapeHtml(r.browser)}</span> `;
                }
                if (r.environment && r.environment !== 'Default') {
                    chipsHtml += `<span class="run-meta-chip chip-env">${escapeHtml(r.environment)}</span> `;
                }
                if (r.batchName && r.batchName !== 'Default') {
                    chipsHtml += `<span class="run-meta-chip chip-tag">${escapeHtml(r.batchName)}</span>`;
                }
                if (!chipsHtml) {
                    chipsHtml = `<span class="text-muted" style="font-size: 0.72rem;">Default</span>`;
                }

                tr.innerHTML = `
                    <td>
                        <a class="text-mono run-id-link" style="text-decoration: none; font-weight: 700; color: var(--text-main);"
                           href="/run-report?runId=${encodeURIComponent(r.runId)}"
                           hx-get="/run-report?runId=${encodeURIComponent(r.runId)}"
                           hx-target="#mainViewContainer"
                           hx-swap="innerHTML"
                           hx-push-url="true"
                           onclick="closeTokenCostBreakdownModal();"
                           title="Inspect run report #${escapeHtml(r.runId)}">
                            <span class="material-symbols-outlined" style="font-size: 0.75rem; vertical-align: middle;">launch</span>
                            #${escapeHtml(r.runId)}
                        </a>
                    </td>
                    <td class="text-muted font-mono" style="font-size: 0.75rem;">${escapeHtml(r.timestampLabel || 'Recently')}</td>
                    <td>${chipsHtml}</td>
                    <td style="text-align: right; font-family: var(--font-mono); font-weight: 600;">${r.contributedTestCount}</td>
                    <td style="text-align: right; font-family: var(--font-mono); font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(r.contributedTokensFormatted)}</td>
                    <td style="text-align: right; font-family: var(--font-mono); font-weight: 700; color: var(--text-main);">${escapeHtml(r.contributedCostFormatted)}</td>
                    <td>
                        <div style="display: flex; align-items: center; gap: 0.4rem;">
                            <div class="cost-share-track" style="flex: 1;" title="${escapeHtml(r.costPercentageFormatted)}">
                                <div class="cost-share-fill" style="width: ${Math.max(2, r.costPercentage || 0)}%;"></div>
                            </div>
                            <span style="font-size: 0.7rem; font-family: var(--font-mono); font-weight: 600; width: 42px; text-align: right;">${escapeHtml(r.costPercentageFormatted)}</span>
                        </div>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    }

    renderTokenCostModalSparkline(groups, idx);
}

function renderTokenCostModalSparkline(groups, activeIdx) {
    const svgPathEl = document.getElementById('tokenCostModalSparklinePath');
    const dotsG = document.getElementById('tokenCostModalSparklineDots');
    if (!svgPathEl || !dotsG || !groups || groups.length === 0) return;

    const width = 400.0;
    const height = 42.0;
    const padX = 14.0;
    const padY = 8.0;
    const plotW = width - (2.0 * padX);
    const plotH = height - (2.0 * padY);

    const n = groups.length;
    let min = Infinity;
    let max = -Infinity;
    groups.forEach(g => {
        const c = g.totalCost || 0.0;
        if (c < min) min = c;
        if (c > max) max = c;
    });

    const xs = [];
    const ys = [];
    for (let i = 0; i < n; i++) {
        xs[i] = padX + (n === 1 ? plotW / 2.0 : i * (plotW / (n - 1)));
        const v = groups[i].totalCost || 0.0;
        if (max > min) {
            const norm = (v - min) / (max - min);
            ys[i] = padY + ((1.0 - norm) * plotH);
        } else {
            ys[i] = height / 2.0;
        }
    }

    if (n === 1) {
        svgPathEl.setAttribute('d', `M ${padX} ${height / 2.0} L ${width - padX} ${height / 2.0}`);
    } else if (n === 2) {
        svgPathEl.setAttribute('d', `M ${xs[0]} ${ys[0]} L ${xs[1]} ${ys[1]}`);
    } else {
        let d = `M ${xs[0]} ${ys[0]}`;
        for (let i = 0; i < n - 1; i++) {
            const p0x = (i > 0) ? xs[i - 1] : xs[i];
            const p0y = (i > 0) ? ys[i - 1] : ys[i];
            const p1x = xs[i];
            const p1y = ys[i];
            const p2x = xs[i + 1];
            const p2y = ys[i + 1];
            const p3x = (i + 2 < n) ? xs[i + 2] : p2x;
            const p3y = (i + 2 < n) ? ys[i + 2] : p2y;

            const cp1x = p1x + ((p2x - p0x) / 6.0);
            const cp1y = p1y + ((p2y - p0y) / 6.0);
            const cp2x = p2x - ((p3x - p1x) / 6.0);
            const cp2y = p2y - ((p3y - p1y) / 6.0);

            d += ` C ${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${p2x.toFixed(1)} ${p2y.toFixed(1)}`;
        }
        svgPathEl.setAttribute('d', d);
    }

    dotsG.innerHTML = '';
    for (let i = 0; i < n; i++) {
        const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        circle.setAttribute('cx', xs[i].toFixed(1));
        circle.setAttribute('cy', ys[i].toFixed(1));
        circle.setAttribute('r', (i === activeIdx) ? '6' : '3.5');
        circle.setAttribute('class', `sparkline-dot-interactive ${i === activeIdx ? 'active-dot' : ''}`);
        circle.setAttribute('fill', (i === activeIdx) ? 'var(--accent, #3b82f6)' : 'var(--token-trend-stroke, #9f1239)');
        circle.setAttribute('stroke', (i === activeIdx) ? '#ffffff' : 'var(--bg-panel)');
        circle.setAttribute('stroke-width', (i === activeIdx) ? '2' : '1.5');
        circle.setAttribute('data-point-idx', i);
        circle.addEventListener('click', (e) => {
            e.stopPropagation();
            renderTokenCostBreakdown(i);
        });

        const title = document.createElementNS('http://www.w3.org/2000/svg', 'title');
        title.textContent = `Day ${i + 1}: ${groups[i].totalCostFormatted} (${groups[i].timestampLabel || 'Today'})`;
        circle.appendChild(title);

        dotsG.appendChild(circle);
    }
}

document.addEventListener('keydown', (e) => {
    const modal = document.getElementById('tokenCostBreakdownModal');
    if (!modal || modal.style.display === 'none') return;

    if (e.key === 'Escape') {
        closeTokenCostBreakdownModal();
    } else if (e.key === 'ArrowLeft') {
        navigateTokenCostGroup(-1);
    } else if (e.key === 'ArrowRight') {
        navigateTokenCostGroup(1);
    }
});

/* ==========================================================================
   WIDGET CONFIGURATOR JAVASCRIPT HANDLERS
   ========================================================================== */

function selectTagKeyPreset(val) {
    const sel = document.getElementById('widgetTagKeySelect');
    if (!sel) return;
    const lowerVal = (val || '').toLowerCase();
    let found = false;
    for (let i = 0; i < sel.options.length; i++) {
        if (sel.options[i].value.toLowerCase() === lowerVal || sel.options[i].text.toLowerCase() === lowerVal) {
            sel.selectedIndex = i;
            found = true;
            break;
        }
    }
    if (!found) {
        const opt = document.createElement('option');
        opt.value = val;
        opt.text = val;
        sel.add(opt);
        sel.value = val;
    }
}

function openWidgetConfiguratorModal(widgetId) {
    const modal = document.getElementById('widgetConfiguratorModal');
    if (modal) {
        modal.style.display = 'flex';
    }

    const pageEl = document.getElementById('pageCustomDashboard');
    const pageId = pageEl ? (pageEl.getAttribute('data-page-id') || '') : '';

    if (window.htmx && document.getElementById('widgetConfiguratorModalContainer')) {
        let url = '/widgets/config-modal';
        const params = [];
        if (widgetId) {
            params.push('widgetId=' + encodeURIComponent(widgetId));
        }
        if (pageId) {
            params.push('pageId=' + encodeURIComponent(pageId));
        }
        if (params.length > 0) {
            url += '?' + params.join('&');
        }
        window.htmx.ajax('GET', url, {
            target: '#widgetConfiguratorModalContainer',
            swap: 'innerHTML'
        }).then(() => {
            const m = document.getElementById('widgetConfiguratorModal');
            if (m) m.style.display = 'flex';
            if (typeof updateSuggestedWidgetTitle === 'function') {
                updateSuggestedWidgetTitle();
            }
        });
        return;
    }

    if (!modal) return;

    const idInput = document.getElementById('widgetConfigId');
    const titleInput = document.getElementById('widgetTitleInput');
    if (idInput) idInput.value = widgetId || '';
    if (titleInput) {
        if (!widgetId) {
            titleInput.value = '';
            titleInput.dataset.userCustom = 'false';
        } else {
            titleInput.dataset.userCustom = 'true';
        }
    }

    if (!widgetId) {
        const defaultMetric = modal.querySelector('input[name="metric"][value="STATUS"]');
        if (defaultMetric) defaultMetric.checked = true;
        const defaultPres = modal.querySelector('input[name="presentation"][value="PIE_CHART"]');
        if (defaultPres) defaultPres.checked = true;
        onWidgetMetricChanged('STATUS');
        onWidgetPresentationChanged('PIE_CHART');
    }

    modal.style.display = 'flex';
}

function closeWidgetConfiguratorModal() {
    const modal = document.getElementById('widgetConfiguratorModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function onWidgetMetricChanged(metric) {
    const statusOptionsRow = document.getElementById('widgetStatusOptionsRow');
    if (statusOptionsRow) {
        statusOptionsRow.style.display = (metric === 'STATUS') ? 'block' : 'none';
    }
    const tagSelectorRow = document.getElementById('widgetTagSelectorRow');
    if (tagSelectorRow) {
        tagSelectorRow.style.display = (metric === 'TAG') ? 'block' : 'none';
    }
    const tagSelect = document.getElementById('widgetTagKeySelect');
    if (tagSelect) {
        tagSelect.disabled = (metric !== 'TAG');
    }
    const perlenketteRadio = document.querySelector('#widgetPresentationSection input[type="radio"][name="presentation"][value="PERLENKETTE"]');
    if (perlenketteRadio) {
        perlenketteRadio.disabled = (metric === 'TAG');
        if (metric === 'TAG' && perlenketteRadio.checked) {
            const pieRadio = document.querySelector('#widgetPresentationSection input[type="radio"][name="presentation"][value="PIE_CHART"]');
            if (pieRadio) {
                pieRadio.checked = true;
                if (typeof onWidgetPresentationChanged === 'function') {
                    onWidgetPresentationChanged('PIE_CHART');
                }
            }
        }
    }
    if (typeof updateSuggestedWidgetTitle === 'function') {
        updateSuggestedWidgetTitle();
    }
}

function onWidgetPresentationChanged(presentation) {
    const valueFormatSec = document.getElementById('widgetValueFormatSection');
    const perlenketteOpts = document.getElementById('widgetPerlenketteOptionsSection');
    const trendModeSec = document.getElementById('widgetTrendModeSection');
    const trendOpts = document.getElementById('widgetTrendOptionsSection');
    const pointScopeSec = document.getElementById('widgetPointScopeSection');

    if (valueFormatSec) {
        valueFormatSec.style.display = (presentation === 'PERLENKETTE' || presentation === 'TEXT' || presentation === 'TEXT_WITH_SMALL_TREND') ? 'block' : 'none';
    }
    if (perlenketteOpts) {
        perlenketteOpts.style.display = (presentation === 'PERLENKETTE') ? 'block' : 'none';
    }
    if (trendModeSec) {
        trendModeSec.style.display = (presentation === 'PERLENKETTE' || presentation === 'TREND_GRAPH' || presentation === 'TEXT_WITH_SMALL_TREND') ? 'block' : 'none';
    }
    if (trendOpts) {
        trendOpts.style.display = (presentation === 'TREND_GRAPH') ? 'block' : 'none';
    }
    if (pointScopeSec) {
        pointScopeSec.style.display = (presentation === 'PIE_CHART' || presentation === 'RATIO_BAR' || presentation === 'TEXT' || presentation === 'LIST') ? 'block' : 'none';
    }
    if (typeof updateSuggestedWidgetTitle === 'function') {
        updateSuggestedWidgetTitle();
    }
}

function toggleTrendDots(checkbox, widgetId) {
    if (!widgetId) return;
    const card = document.getElementById('widgetCard-' + widgetId);
    if (!card) return;
    const overlay = card.querySelector('.widget-trend-points-overlay');
    if (overlay) {
        overlay.style.display = checkbox.checked ? 'block' : 'none';
    }
    if (window.htmx) {
        fetch('/widgets/toggle-dots?widgetId=' + encodeURIComponent(widgetId) + '&showDots=' + checkbox.checked, {
            method: 'POST',
            headers: { 'HX-Request': 'true' }
        }).catch(function(err) {
            console.warn('Failed to persist widget dots state:', err);
        });
    }
}
window.toggleTrendDots = toggleTrendDots;

function toggleTrendFilled(checkbox, widgetId) {
    if (!widgetId) return;
    const card = document.getElementById('widgetCard-' + widgetId);
    if (!card) return;
    const isFilled = checkbox.checked;

    // Toggle multi-layer area paths and top stroke
    const areaPaths = card.querySelectorAll('.widget-trend-layer-area, .widget-trend-layer-pattern, .widget-trend-top-stroke, .widget-trend-single-area');
    areaPaths.forEach(el => {
        el.style.display = isFilled ? '' : 'none';
    });

    // Toggle multi-layer lines
    const linePaths = card.querySelectorAll('.widget-trend-layer-line');
    linePaths.forEach(el => {
        el.style.display = isFilled ? 'none' : '';
    });

    // Deselect any active legend isolate chip when switching modes
    card.querySelectorAll('.ratio-legend-chip.is-active').forEach(c => c.classList.remove('is-active', 'is-dimmed'));

    if (window.htmx) {
        fetch('/widgets/toggle-filled?widgetId=' + encodeURIComponent(widgetId) + '&filled=' + isFilled, {
            method: 'POST',
            headers: { 'HX-Request': 'true' }
        }).catch(function(err) {
            console.warn('Failed to persist widget filled state:', err);
        });
    }
}
window.toggleTrendFilled = toggleTrendFilled;

function extractLinePathPoints(d) {
    if (!d) return [];
    const pts = [];
    const re = /[ML]\s*([-\d.]+)\s+([-\d.]+)/gi;
    let match;
    while ((match = re.exec(d)) !== null) {
        pts.push({ x: parseFloat(match[1]), y: parseFloat(match[2]) });
    }
    return pts;
}

function updateTrendYAxis(card, svg, isolatedMax, unit) {
    const yAxisGroup = card.querySelector('.widget-trend-y-axis');
    if (!yAxisGroup) return;

    if (!yAxisGroup.getAttribute('data-orig-html')) {
        yAxisGroup.setAttribute('data-orig-html', yAxisGroup.innerHTML);
    }

    const padLeft = parseFloat(svg.getAttribute('data-pad-left')) || 70;
    const padRight = parseFloat(svg.getAttribute('data-pad-right')) || 20;
    const padTop = parseFloat(svg.getAttribute('data-pad-top')) || 15;
    const trendWidth = parseFloat(svg.getAttribute('data-trend-width')) || 800;
    const trendHeight = parseFloat(svg.getAttribute('data-trend-height')) || 200;
    const baselineY = parseFloat(svg.getAttribute('data-baseline-y')) || (trendHeight - 35);
    const graphHeight = baselineY - padTop;
    const rightX = trendWidth - padRight;

    if (!unit) {
        const origTexts = Array.from(yAxisGroup.querySelectorAll('text'));
        const lastText = origTexts.length > 0 ? origTexts[origTexts.length - 1].textContent.trim() : '';
        if (lastText.includes('%')) {
            unit = '%';
        } else if (lastText.includes('$')) {
            unit = '$';
        } else if (lastText.toLowerCase().includes('flaky')) {
            unit = ' flaky';
        } else if (lastText.toLowerCase().includes('bugs')) {
            unit = ' bugs';
        } else {
            unit = ' Tests';
        }
    }

    const formatVal = function(v) {
        if (unit === '$') return '$' + v.toFixed(4);
        if (unit === '%') return v.toFixed(1) + '%';
        if (unit === ' Tests') return Math.round(v) + (Math.round(v) === 1 ? ' Test' : ' Tests');
        return Math.round(v) + unit;
    };

    let html = '';
    html += '<line x1="' + padLeft + '" y1="' + baselineY + '" x2="' + rightX + '" y2="' + baselineY + '" stroke="var(--border)" stroke-width="1.2"/>';
    html += '<text x="' + (padLeft - 8) + '" y="' + (baselineY + 3.5) + '" fill="var(--text-muted)" font-size="10" font-family="JetBrains Mono, monospace" text-anchor="end">' + formatVal(0) + '</text>';

    if (isolatedMax > 1) {
        const midVal = Math.round(isolatedMax / 2);
        if (midVal > 0 && midVal < isolatedMax) {
            const midY = baselineY - ((midVal / isolatedMax) * graphHeight);
            html += '<line x1="' + padLeft + '" y1="' + midY.toFixed(1) + '" x2="' + rightX + '" y2="' + midY.toFixed(1) + '" stroke="var(--border)" stroke-width="1" stroke-dasharray="3 3" opacity="0.6"/>';
            html += '<text x="' + (padLeft - 8) + '" y="' + (midY + 3.5).toFixed(1) + '" fill="var(--text-muted)" font-size="10" font-family="JetBrains Mono, monospace" text-anchor="end">' + formatVal(midVal) + '</text>';
        }
    }

    const topY = padTop;
    html += '<line x1="' + padLeft + '" y1="' + topY.toFixed(1) + '" x2="' + rightX + '" y2="' + topY.toFixed(1) + '" stroke="var(--border)" stroke-width="1" stroke-dasharray="3 3" opacity="0.6"/>';
    html += '<text x="' + (padLeft - 8) + '" y="' + (topY + 3.5).toFixed(1) + '" fill="var(--text-muted)" font-size="10" font-family="JetBrains Mono, monospace" text-anchor="end">' + formatVal(isolatedMax) + '</text>';

    yAxisGroup.innerHTML = html;
}

function restoreTrendYAxis(card) {
    const yAxisGroup = card.querySelector('.widget-trend-y-axis');
    if (yAxisGroup && yAxisGroup.getAttribute('data-orig-html')) {
        yAxisGroup.innerHTML = yAxisGroup.getAttribute('data-orig-html');
        yAxisGroup.removeAttribute('data-orig-html');
    }
}

function toggleWidgetTrendLayerSelect(target, layerName) {
    let widgetId = target;
    let name = layerName;
    if (typeof target === 'object' && target !== null && target.getAttribute) {
        widgetId = target.getAttribute('data-widget-id');
        name = target.getAttribute('data-layer-name');
    }
    if (!widgetId) return;
    const card = document.getElementById('widgetCard-' + widgetId);
    if (!card) return;

    const chips = Array.from(card.querySelectorAll('.ratio-legend-chip[data-layer-name]'));
    if (chips.length <= 1) return;
    const clickedChip = chips.find(c => c.getAttribute('data-layer-name') === name);
    if (!clickedChip) return;

    const isCurrentlyActive = clickedChip.classList.contains('is-active');

    const svg = card.querySelector('.widget-trend-svg');
    const areaPaths = card.querySelectorAll('.widget-trend-layer-area');
    const patternPaths = card.querySelectorAll('.widget-trend-layer-pattern');
    const linePaths = card.querySelectorAll('.widget-trend-layer-line');
    const topStroke = card.querySelector('.widget-trend-top-stroke');
    const pointWrappers = card.querySelectorAll('.widget-trend-point-wrapper');

    const fillToggle = card.querySelector('.widget-fill-toggle-input');
    const isFilled = fillToggle ? fillToggle.checked : (areaPaths.length > 0);

    if (isCurrentlyActive) {
        chips.forEach(c => {
            c.classList.remove('is-active', 'is-dimmed');
        });
        areaPaths.forEach(p => { p.style.display = isFilled ? '' : 'none'; });
        patternPaths.forEach(p => { p.style.display = isFilled ? '' : 'none'; });
        linePaths.forEach(p => {
            p.style.display = isFilled ? 'none' : '';
            const origD = p.getAttribute('data-orig-d');
            if (origD) {
                p.setAttribute('d', origD);
                p.removeAttribute('data-orig-d');
            }
        });
        if (topStroke) topStroke.style.display = isFilled ? '' : 'none';

        restoreTrendYAxis(card);

        pointWrappers.forEach(w => {
            const origTop = w.getAttribute('data-orig-top');
            if (origTop !== null) {
                w.style.top = origTop + '%';
            }
            const dot = w.querySelector('.widget-trend-point-dot');
            if (dot) dot.style.backgroundColor = '';
        });
    } else {
        chips.forEach(c => {
            if (c === clickedChip) {
                c.classList.add('is-active');
                c.classList.remove('is-dimmed');
            } else {
                c.classList.add('is-dimmed');
                c.classList.remove('is-active');
            }
        });

        areaPaths.forEach(p => { p.style.display = 'none'; });
        patternPaths.forEach(p => { p.style.display = 'none'; });
        if (topStroke) topStroke.style.display = 'none';

        let targetLine = null;
        linePaths.forEach(p => {
            if (p.getAttribute('data-layer-name') === name) {
                p.style.display = 'block';
                targetLine = p;
            } else {
                p.style.display = 'none';
            }
        });

        if (targetLine && svg) {
            const origD = targetLine.getAttribute('data-orig-d') || targetLine.getAttribute('d');
            targetLine.setAttribute('data-orig-d', origD);

            const rawCsv = targetLine.getAttribute('data-raw-values');
            const padLeft = parseFloat(svg.getAttribute('data-pad-left')) || 70;
            const padRight = parseFloat(svg.getAttribute('data-pad-right')) || 20;
            const padTop = parseFloat(svg.getAttribute('data-pad-top')) || 15;
            const trendWidth = parseFloat(svg.getAttribute('data-trend-width')) || 800;
            const trendHeight = parseFloat(svg.getAttribute('data-trend-height')) || 200;
            const baselineY = parseFloat(svg.getAttribute('data-baseline-y')) || (trendHeight - 35);
            const graphWidth = trendWidth - padLeft - padRight;
            const graphHeight = baselineY - padTop;

            let pts = [];
            let isolatedMax = 0;

            if (rawCsv) {
                const vals = rawCsv.split(',').map(Number);
                vals.forEach(v => { if (v > isolatedMax) isolatedMax = v; });
                if (isolatedMax < 1) isolatedMax = 1;
                const n = vals.length;
                for (let i = 0; i < n; i++) {
                    const x = (n === 1) ? (padLeft + graphWidth / 2) : (padLeft + (i / (n - 1)) * graphWidth);
                    const y = baselineY - (vals[i] / isolatedMax) * graphHeight;
                    pts.push({ x: x, y: y, val: vals[i] });
                }
            } else {
                const extracted = extractLinePathPoints(origD);
                let minY = baselineY;
                extracted.forEach(p => { if (p.y < minY) minY = p.y; });
                const currentHeight = baselineY - minY;
                if (currentHeight > 1 && graphHeight > 0) {
                    const factor = graphHeight / currentHeight;
                    extracted.forEach(p => {
                        const relY = baselineY - p.y;
                        pts.push({ x: p.x, y: baselineY - (relY * factor) });
                    });
                } else {
                    pts = extracted;
                }
                const maxValAttr = parseFloat(targetLine.getAttribute('data-max-val')) || 0;
                isolatedMax = maxValAttr > 0 ? maxValAttr : 1;
            }

            if (pts.length > 0) {
                let newD = '';
                if (pts.length === 1) {
                    const x0 = Math.max(padLeft, pts[0].x - 25);
                    const x1 = Math.min(trendWidth - padRight, pts[0].x + 25);
                    newD = 'M ' + x0.toFixed(1) + ' ' + pts[0].y.toFixed(1) + ' L ' + x1.toFixed(1) + ' ' + pts[0].y.toFixed(1);
                } else {
                    newD = 'M ' + pts[0].x.toFixed(1) + ' ' + pts[0].y.toFixed(1);
                    for (let i = 1; i < pts.length; i++) {
                        newD += ' L ' + pts[i].x.toFixed(1) + ' ' + pts[i].y.toFixed(1);
                    }
                }
                targetLine.setAttribute('d', newD);
            }

            updateTrendYAxis(card, svg, isolatedMax);

            const lineColor = targetLine.style.stroke || clickedChip.querySelector('.dot')?.style.backgroundColor || '';
            pointWrappers.forEach((w, idx) => {
                let ptY = null;
                if (pts.length === 1) {
                    ptY = pts[0].y;
                } else if (idx < pts.length) {
                    ptY = pts[idx].y;
                }
                if (ptY !== null && trendHeight > 0) {
                    w.style.top = ((ptY / trendHeight) * 100) + '%';
                }
                const dot = w.querySelector('.widget-trend-point-dot');
                if (dot && lineColor) {
                    dot.style.backgroundColor = lineColor;
                }
            });
        }
    }
}
window.toggleWidgetTrendLayerSelect = toggleWidgetTrendLayerSelect;
window.updateTrendYAxis = updateTrendYAxis;
window.restoreTrendYAxis = restoreTrendYAxis;

function toggleWidgetExecutionIdentitySection(show) {
    var sec = document.getElementById('widgetExecutionIdentityOptions');
    if (sec) sec.style.display = show ? 'block' : 'none';
}

function updateExecutionIdentityDimensionsInput() {
    var chks = document.querySelectorAll('input[name="identityDimCheckbox"]:checked');
    var vals = [];
    chks.forEach(function(c) { vals.push(c.value); });
    var inp = document.getElementById('widgetExecutionIdentityDimensionsInput');
    if (inp) inp.value = vals.join(',');
}

function toggleWidgetRunFiltersSection(show) {
    var sec = document.getElementById('widgetRunFiltersOptions');
    if (sec) sec.style.display = show ? 'block' : 'none';
}

function updateWidgetRunFiltersInputs() {
    function collectChecked(name, targetId) {
        var chks = document.querySelectorAll('input[name="' + name + '"]:checked');
        var vals = [];
        chks.forEach(function(c) { vals.push(c.value); });
        var inp = document.getElementById(targetId);
        if (inp) inp.value = vals.join(',');
    }
    collectChecked('filterBrowsersCheckbox', 'widgetFilterBrowsersInput');
    collectChecked('filterLocalesCheckbox', 'widgetFilterLocalesInput');
    collectChecked('filterEnvsCheckbox', 'widgetFilterEnvsInput');
    collectChecked('filterAreasCheckbox', 'widgetFilterAreasInput');
}

function moveWidgetPosition(currentIndex, direction) {
    const list = document.querySelectorAll('.active-widget-item');
    if (!list || list.length === 0) return;
    const targetIndex = currentIndex + direction;
    if (targetIndex < 0 || targetIndex >= list.length) return;

    const deleteBtns = document.querySelectorAll('.active-widget-item button[hx-post*="widgetId="]');
    const ids = [];
    deleteBtns.forEach(btn => {
        const postAttr = btn.getAttribute('hx-post') || '';
        const match = postAttr.match(/widgetId=([^&]+)/);
        if (match) {
            ids.push(match[1]);
        }
    });

    if (ids.length >= 2 && currentIndex < ids.length && targetIndex < ids.length) {
        const temp = ids[currentIndex];
        ids[currentIndex] = ids[targetIndex];
        ids[targetIndex] = temp;

        if (window.htmx) {
            window.htmx.ajax('POST', '/widgets/reorder', {
                target: '#mainViewContainer',
                swap: 'innerHTML',
                values: { widgetIds: ids.join(',') }
            });
            closeWidgetConfiguratorModal();
        }
    }
}

function openWidgetExecutionsModal(widgetId) {
    if (!widgetId) return;

    const modalContainer = document.getElementById('widgetExecutionsModalContainer');
    if (modalContainer) {
        modalContainer.innerHTML = `
            <div class="modal-overlay" id="widgetExecutionsModal" style="display: flex;" onclick="if (event.target === this) closeWidgetExecutionsModal();">
                <div class="modal-card widget-executions-card" style="max-width: 980px; width: 96%; min-height: 220px; display: flex; flex-direction: column; align-items: center; justify-content: center; background: var(--bg-card);" onclick="event.stopPropagation();">
                    <div class="d-flex flex-column align-items-center gap-3 p-4">
                        <div class="spinner-border text-primary" role="status" style="width: 2.2rem; height: 2.2rem;"></div>
                        <div class="text-muted" style="font-size: 0.85rem;">Loading contributing test executions...</div>
                    </div>
                </div>
            </div>`;
    }

    if (window.htmx && modalContainer) {
        const urlParams = new URLSearchParams(window.location.search);
        const excludeRuns = urlParams.get('excludeRuns');
        let requestUrl = '/widgets/executions?widgetId=' + encodeURIComponent(widgetId);
        if (excludeRuns) {
            requestUrl += '&excludeRuns=' + encodeURIComponent(excludeRuns);
        }
        window.htmx.ajax('GET', requestUrl, {
            target: '#widgetExecutionsModalContainer',
            swap: 'innerHTML'
        }).then(() => {
            const reloaded = document.getElementById('widgetExecutionsModal');
            if (reloaded) reloaded.style.display = 'flex';
        });
    } else {
        const modal = document.getElementById('widgetExecutionsModal');
        if (modal) {
            modal.style.display = 'flex';
        }
    }
}
window.openWidgetExecutionsModal = openWidgetExecutionsModal;

function openRenameWidgetModal(widgetId, currentTitle, isGroup) {
    const modal = document.getElementById('renameWidgetModal');
    if (!modal) return;

    const idInput = document.getElementById('renameWidgetIdInput');
    const titleInput = document.getElementById('renameWidgetTitleInput');
    const pageIdInput = document.getElementById('renameWidgetPageIdInput');
    const modalTitle = document.getElementById('renameWidgetModalTitle');
    const modalIcon = document.getElementById('renameWidgetModalIcon');
    const modalSubtitle = document.getElementById('renameWidgetModalSubtitle');

    const pageEl = document.getElementById('pageCustomDashboard');
    const pageId = pageEl ? (pageEl.getAttribute('data-page-id') || '') : '';

    if (idInput) idInput.value = widgetId || '';
    if (pageIdInput) pageIdInput.value = pageId || '';
    if (titleInput) {
        titleInput.value = currentTitle || '';
    }

    if (isGroup) {
        if (modalTitle) modalTitle.textContent = 'Rename Widget Group';
        if (modalIcon) modalIcon.textContent = 'layers';
        if (modalSubtitle) modalSubtitle.textContent = 'Give a descriptive custom title to this widget group.';
    } else {
        if (modalTitle) modalTitle.textContent = 'Rename Widget';
        if (modalIcon) modalIcon.textContent = 'edit';
        if (modalSubtitle) modalSubtitle.textContent = 'Give a descriptive custom title to this widget.';
    }

    modal.style.display = 'flex';
    if (titleInput) {
        setTimeout(() => {
            titleInput.focus();
            titleInput.select();
        }, 50);
    }
}

function closeRenameWidgetModal() {
    const modal = document.getElementById('renameWidgetModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

window.openRenameWidgetModal = openRenameWidgetModal;
window.closeRenameWidgetModal = closeRenameWidgetModal;

function excludeWidgetRun(event, runId, widgetId) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    if (!runId) return;

    const urlParams = new URLSearchParams(window.location.search);
    const existingExcludes = (urlParams.get('excludeRuns') || '')
        .split(',')
        .map(s => s.trim())
        .filter(Boolean);
    if (!existingExcludes.includes(runId)) {
        existingExcludes.push(runId);
    }
    const newExcludeRuns = existingExcludes.join(',');
    urlParams.set('excludeRuns', newExcludeRuns);

    const newSearch = urlParams.toString();
    const newRelativeUrl = window.location.pathname + (newSearch ? '?' + newSearch : '');
    window.history.replaceState(null, '', newRelativeUrl);

    // Refresh background batch history / dashboard view
    const batchTargetUrl = `/batch-history?${newSearch}`;
    if (window.htmx) {
        window.htmx.ajax('GET', batchTargetUrl, { target: '#mainViewContainer', pushUrl: false });
    }

    // Refresh widget executions modal with updated excludeRuns
    const targetWidgetId = widgetId || document.getElementById('widgetExecutionsModal')?.getAttribute('data-widget-id');
    const modalContainer = document.getElementById('widgetExecutionsModalContainer');
    if (window.htmx && modalContainer && targetWidgetId) {
        window.htmx.ajax('GET', '/widgets/executions?widgetId=' + encodeURIComponent(targetWidgetId) + '&excludeRuns=' + encodeURIComponent(newExcludeRuns), {
            target: '#widgetExecutionsModalContainer',
            swap: 'innerHTML'
        }).then(() => {
            const reloaded = document.getElementById('widgetExecutionsModal');
            if (reloaded) reloaded.style.display = 'flex';
        });
    }
}
window.excludeWidgetRun = excludeWidgetRun;

function clearExcludedRunsInModal(widgetId) {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.delete('excludeRuns');

    const newSearch = urlParams.toString();
    const newRelativeUrl = window.location.pathname + (newSearch ? '?' + newSearch : '');
    window.history.replaceState(null, '', newRelativeUrl);

    const batchTargetUrl = `/batch-history?${newSearch}`;
    if (window.htmx) {
        window.htmx.ajax('GET', batchTargetUrl, { target: '#mainViewContainer', pushUrl: false });
    }

    const targetWidgetId = widgetId || document.getElementById('widgetExecutionsModal')?.getAttribute('data-widget-id');
    const modalContainer = document.getElementById('widgetExecutionsModalContainer');
    if (window.htmx && modalContainer && targetWidgetId) {
        window.htmx.ajax('GET', '/widgets/executions?widgetId=' + encodeURIComponent(targetWidgetId), {
            target: '#widgetExecutionsModalContainer',
            swap: 'innerHTML'
        }).then(() => {
            const reloaded = document.getElementById('widgetExecutionsModal');
            if (reloaded) reloaded.style.display = 'flex';
        });
    }
}
window.clearExcludedRunsInModal = clearExcludedRunsInModal;

function closeWidgetExecutionsModal() {
    const modal = document.getElementById('widgetExecutionsModal');
    if (modal) {
        modal.style.display = 'none';
    }
}
window.closeWidgetExecutionsModal = closeWidgetExecutionsModal;

function toggleWidgetRunAccordion(headerEl) {
    const group = headerEl.closest('.widget-run-accordion-group');
    if (!group) return;
    const body = group.querySelector('.widget-run-group-body');
    const chevron = group.querySelector('.widget-run-chevron');
    if (!body) return;

    if (body.style.display === 'none') {
        body.style.display = 'block';
        if (chevron) chevron.style.transform = 'rotate(0deg)';
    } else {
        body.style.display = 'none';
        if (chevron) chevron.style.transform = 'rotate(-90deg)';
    }
}
window.toggleWidgetRunAccordion = toggleWidgetRunAccordion;

function toggleAllWidgetRunAccordions() {
    const bodies = document.querySelectorAll('.widget-run-group-body');
    if (bodies.length === 0) return;
    const anyOpen = Array.from(bodies).some(b => b.style.display !== 'none');
    bodies.forEach(b => {
        b.style.display = anyOpen ? 'none' : 'block';
        const chevron = b.closest('.widget-run-accordion-group')?.querySelector('.widget-run-chevron');
        if (chevron) chevron.style.transform = anyOpen ? 'rotate(-90deg)' : 'rotate(0deg)';
    });
}
window.toggleAllWidgetRunAccordions = toggleAllWidgetRunAccordions;

function updateWidgetParentSectionSpan(rowEl) {
    if (!rowEl) return;
    const section = rowEl.closest('.widget-container-section');
    if (section) {
        const anyExpanded = section.querySelector('.widget-list-group:not(.collapsed)');
        const spanBtn = section.querySelector('.btn-widget-section-span');
        const spanIcon = spanBtn ? spanBtn.querySelector('.material-symbols-outlined') : null;
        if (anyExpanded) {
            section.classList.add('widget-container-full-span');
            section.classList.add('is-uncollapsed-expanded');
            if (spanIcon) spanIcon.textContent = 'close_fullscreen';
            if (spanBtn) spanBtn.setAttribute('title', 'Restore column width');
        } else {
            if (section.classList.contains('is-uncollapsed-expanded')) {
                section.classList.remove('widget-container-full-span');
                section.classList.remove('is-uncollapsed-expanded');
                if (spanIcon) spanIcon.textContent = 'open_in_full';
                if (spanBtn) spanBtn.setAttribute('title', 'Expand to full width');
            }
        }
        return;
    }

    const card = rowEl.closest('.widget-card:not(.widget-container-card)');
    if (card) {
        const anyExpanded = card.querySelector('.widget-list-group:not(.collapsed)');
        if (anyExpanded) {
            if (!card.classList.contains('widget-card-full')) {
                card.classList.add('widget-card-full', 'widget-full-width', 'is-uncollapsed-expanded');
            }
        } else if (card.classList.contains('is-uncollapsed-expanded')) {
            card.classList.remove('widget-card-full', 'widget-full-width', 'is-uncollapsed-expanded');
        }
    }
}
window.updateWidgetParentSectionSpan = updateWidgetParentSectionSpan;

function toggleWidgetSectionSpan(btn, event) {
    if (event) {
        event.stopPropagation();
    }
    const section = btn.closest('.widget-container-section');
    if (!section) return;
    const isFull = section.classList.toggle('widget-container-full-span');
    section.classList.remove('is-uncollapsed-expanded');
    const icon = btn.querySelector('.material-symbols-outlined');
    if (icon) {
        icon.textContent = isFull ? 'close_fullscreen' : 'open_in_full';
    }
    btn.setAttribute('title', isFull ? 'Restore column width' : 'Expand to full width');
}
window.toggleWidgetSectionSpan = toggleWidgetSectionSpan;

function toggleWidgetListItemCollapse(rowEl, event) {
    if (event) {
        event.stopPropagation();
    }
    const group = rowEl.closest('.widget-list-group');
    if (!group) return;
    const subitems = group.querySelector('.widget-list-subitems');
    const chevron = (rowEl.classList && rowEl.classList.contains('widget-list-chevron'))
        ? rowEl
        : (rowEl.querySelector('.widget-list-chevron') || group.querySelector('.widget-list-chevron'));
    if (!subitems) return;

    const isCollapsed = group.classList.contains('collapsed') || subitems.style.display === 'none' || subitems.classList.contains('is-collapsed-hidden');
    if (isCollapsed) {
        group.classList.remove('collapsed');
        subitems.classList.remove('is-collapsed-hidden');
        subitems.style.removeProperty('display');
        subitems.style.display = 'flex';
        if (chevron) chevron.style.transform = 'rotate(0deg)';
    } else {
        group.classList.add('collapsed');
        subitems.classList.add('is-collapsed-hidden');
        subitems.style.display = 'none';
        subitems.style.setProperty('display', 'none', 'important');
        if (chevron) chevron.style.transform = 'rotate(-90deg)';
    }

    updateWidgetParentSectionSpan(rowEl);
}
window.toggleWidgetListItemCollapse = toggleWidgetListItemCollapse;

let activeExecStatusFilter = 'all';

function filterWidgetExecutionsByStatus(status, btn) {
    activeExecStatusFilter = status;
    document.querySelectorAll('.js-exec-status-filter').forEach(b => {
        if (b.getAttribute('data-status-filter') === status) {
            b.classList.add('active');
        } else {
            b.classList.remove('active');
        }
    });

    document.querySelectorAll('.js-run-status-badge').forEach(badge => {
        const badgeKey = badge.getAttribute('data-status-key');
        if (status !== 'all' && badgeKey === status) {
            badge.classList.add('active-filter-badge');
        } else {
            badge.classList.remove('active-filter-badge');
        }
    });

    const searchInput = document.getElementById('widgetExecutionsFilterInput');
    applyWidgetExecutionsFilter(searchInput ? searchInput.value : '');
}
window.filterWidgetExecutionsByStatus = filterWidgetExecutionsByStatus;

function filterWidgetExecutionsModal(query) {
    applyWidgetExecutionsFilter(query);
}
window.filterWidgetExecutionsModal = filterWidgetExecutionsModal;

function applyWidgetExecutionsFilter(query) {
    const q = (query || '').toLowerCase().trim();
    const groups = document.querySelectorAll('.widget-run-accordion-group');

    groups.forEach(group => {
        const rows = group.querySelectorAll('.widget-execution-row');
        let visibleInGroup = 0;

        rows.forEach(row => {
            const st = (row.getAttribute('data-status') || '').toLowerCase();
            const name = (row.getAttribute('data-test-name') || '').toLowerCase();
            const method = (row.getAttribute('data-method') || '').toLowerCase();
            const cls = (row.getAttribute('data-class') || '').toLowerCase();
            const br = (row.getAttribute('data-browser') || '').toLowerCase();
            const loc = (row.getAttribute('data-locale') || '').toLowerCase();

            let statusMatch = true;
            if (activeExecStatusFilter === 'pass') {
                statusMatch = st.includes('pass') || (st.includes('success') && !st.includes('fixed'));
            } else if (activeExecStatusFilter === 'fixed') {
                statusMatch = st.includes('fixed');
            } else if (activeExecStatusFilter === 'flaky') {
                statusMatch = (row.getAttribute('data-flaky') === 'true');
            } else if (activeExecStatusFilter === 'fail') {
                statusMatch = st.includes('fail') || st.includes('unknown') || st.includes('known');
            } else if (activeExecStatusFilter === 'ignored') {
                statusMatch = st.includes('ignore') || st.includes('skip');
            }

            let textMatch = true;
            if (q) {
                textMatch = name.includes(q) || method.includes(q) || cls.includes(q) || br.includes(q) || loc.includes(q);
            }

            if (statusMatch && textMatch) {
                row.style.display = '';
                visibleInGroup++;
            } else {
                row.style.display = 'none';
            }
        });

        group.style.display = (visibleInGroup > 0 || rows.length === 0) ? '' : 'none';
    });
}

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const modal = document.getElementById('widgetConfiguratorModal');
        if (modal && modal.style.display !== 'none') {
            closeWidgetConfiguratorModal();
        }
        const execModal = document.getElementById('widgetExecutionsModal');
        if (execModal && execModal.style.display !== 'none') {
            closeWidgetExecutionsModal();
        }
    }
});

// ==========================================
// DASHBOARD WIDGETS DRAG & DROP AND UNIFY
// ==========================================

let activeDraggedWidgetId = null;
let activeDraggedWidgetRuns = null;
let dragAutoScrollRafId = null;
let dragAutoScrollSpeed = 0;

function parseContributingRunsCsv(csv) {
    if (!csv) return new Set();
    return new Set(csv.split(',').map(s => s.trim()).filter(Boolean));
}

function areRunsIdentical(setA, setB) {
    if (setA.size !== setB.size) return false;
    for (const val of setA) {
        if (!setB.has(val)) return false;
    }
    return true;
}

function getDashboardScrollContainer() {
    const main = document.getElementById('mainViewContainer');
    if (main && (main.scrollHeight > main.clientHeight || window.getComputedStyle(main).overflowY === 'auto')) {
        return main;
    }
    return document.scrollingElement || document.documentElement || window;
}

function startDragAutoScrollLoop() {
    if (dragAutoScrollRafId !== null) return;

    function step() {
        if (!activeDraggedWidgetId || dragAutoScrollSpeed === 0) {
            dragAutoScrollRafId = null;
            return;
        }

        const container = getDashboardScrollContainer();
        if (container) {
            if (typeof container.scrollBy === 'function') {
                container.scrollBy({ top: dragAutoScrollSpeed, behavior: 'instant' });
            } else if (container === window) {
                window.scrollBy(0, dragAutoScrollSpeed);
            }
        }

        dragAutoScrollRafId = requestAnimationFrame(step);
    }

    dragAutoScrollRafId = requestAnimationFrame(step);
}

function stopDragAutoScroll() {
    dragAutoScrollSpeed = 0;
    if (dragAutoScrollRafId !== null) {
        cancelAnimationFrame(dragAutoScrollRafId);
        dragAutoScrollRafId = null;
    }
}

function updateDragAutoScrollOnPointer(clientY) {
    if (!activeDraggedWidgetId) {
        stopDragAutoScroll();
        return;
    }

    const container = getDashboardScrollContainer();
    if (!container) return;

    let topEdge = 0;
    let bottomEdge = window.innerHeight;

    if (container !== window && typeof container.getBoundingClientRect === 'function') {
        const rect = container.getBoundingClientRect();
        topEdge = Math.max(rect.top, 0);
        bottomEdge = Math.min(rect.bottom, window.innerHeight);
    }

    const THRESHOLD = 90;
    const MAX_SPEED = 24;

    let speed = 0;

    if (clientY < topEdge + THRESHOLD) {
        const ratio = clientY <= topEdge ? 1 : Math.max(0, (topEdge + THRESHOLD - clientY) / THRESHOLD);
        speed = -Math.round(ratio * MAX_SPEED);
        if (container !== window && container.scrollTop <= 0) {
            speed = 0;
        }
    } else if (clientY > bottomEdge - THRESHOLD) {
        const ratio = clientY >= bottomEdge ? 1 : Math.max(0, (clientY - (bottomEdge - THRESHOLD)) / THRESHOLD);
        speed = Math.round(ratio * MAX_SPEED);
        if (container !== window && (container.scrollTop + container.clientHeight >= container.scrollHeight - 1)) {
            speed = 0;
        }
    } else {
        speed = 0;
    }

    dragAutoScrollSpeed = speed;

    if (dragAutoScrollSpeed !== 0) {
        startDragAutoScrollLoop();
    } else {
        stopDragAutoScroll();
    }
}

function initDashboardWidgetsDragAndDrop() {
    const grid = document.getElementById('dashboardWidgetsGrid');
    if (!grid) return;

    grid.ondragover = (e) => {
        if (!activeDraggedWidgetId) return;
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
        updateDragAutoScrollOnPointer(e.clientY);
    };

    grid.ondrop = (e) => {
        if (e.target.closest('.widget-card')) {
            return;
        }
        e.preventDefault();
        stopDragAutoScroll();
        const sourceId = activeDraggedWidgetId || e.dataTransfer.getData('text/plain');
        if (!sourceId) return;

        const currentCards = Array.from(grid.querySelectorAll(':scope > .widget-card'));
        const ids = currentCards.map(c => c.getAttribute('data-widget-id')).filter(Boolean);
        const fromIdx = ids.indexOf(sourceId);
        if (fromIdx !== -1) {
            ids.splice(fromIdx, 1);
        }
        ids.push(sourceId);

        if (window.htmx) {
            window.htmx.ajax('POST', '/widgets/reorder', {
                target: '#mainViewContainer',
                swap: 'innerHTML',
                values: { widgetIds: ids.join(',') }
            });
        }
    };

    const cards = grid.querySelectorAll(':scope > .widget-card');
    cards.forEach(card => {
        card.setAttribute('draggable', 'true');

        card.ondragstart = (e) => {
            activeDraggedWidgetId = card.getAttribute('data-widget-id');
            activeDraggedWidgetRuns = card.getAttribute('data-contributing-runs');
            e.dataTransfer.setData('text/plain', activeDraggedWidgetId || '');
            e.dataTransfer.effectAllowed = 'move';
            card.classList.add('widget-is-dragging');
        };

        card.ondragend = (e) => {
            stopDragAutoScroll();
            activeDraggedWidgetId = null;
            activeDraggedWidgetRuns = null;
            card.classList.remove('widget-is-dragging');
            cards.forEach(c => {
                c.classList.remove('drop-reorder-before', 'drop-reorder-after', 'drop-combine-valid', 'drop-combine-invalid');
            });
        };

        card.ondragover = (e) => {
            if (!activeDraggedWidgetId || activeDraggedWidgetId === card.getAttribute('data-widget-id')) {
                return;
            }
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            updateDragAutoScrollOnPointer(e.clientY);

            const rect = card.getBoundingClientRect();
            const relY = e.clientY - rect.top;
            const height = rect.height;

            if (relY < height * 0.25) {
                card.classList.remove('drop-reorder-after', 'drop-combine-valid', 'drop-combine-invalid');
                card.classList.add('drop-reorder-before');
            } else if (relY > height * 0.75) {
                card.classList.remove('drop-reorder-before', 'drop-combine-valid', 'drop-combine-invalid');
                card.classList.add('drop-reorder-after');
            } else {
                card.classList.remove('drop-reorder-before', 'drop-reorder-after');

                const targetRuns = card.getAttribute('data-contributing-runs');
                const setSource = parseContributingRunsCsv(activeDraggedWidgetRuns);
                const setTarget = parseContributingRunsCsv(targetRuns);
                const sameRuns = areRunsIdentical(setSource, setTarget);

                if (sameRuns) {
                    card.classList.remove('drop-combine-invalid');
                    card.classList.add('drop-combine-valid');
                } else {
                    card.classList.remove('drop-combine-valid');
                    card.classList.add('drop-combine-invalid');
                }
            }
        };

        card.ondragleave = (e) => {
            if (!card.contains(e.relatedTarget)) {
                card.classList.remove('drop-reorder-before', 'drop-reorder-after', 'drop-combine-valid', 'drop-combine-invalid');
            }
        };

        card.ondrop = (e) => {
            e.preventDefault();
            stopDragAutoScroll();
            const sourceId = activeDraggedWidgetId || e.dataTransfer.getData('text/plain');
            const targetId = card.getAttribute('data-widget-id');

            const isBefore = card.classList.contains('drop-reorder-before');
            const isAfter = card.classList.contains('drop-reorder-after');
            const isCombineValid = card.classList.contains('drop-combine-valid');
            const isCombineInvalid = card.classList.contains('drop-combine-invalid');

            card.classList.remove('drop-reorder-before', 'drop-reorder-after', 'drop-combine-valid', 'drop-combine-invalid');

            if (!sourceId || !targetId || sourceId === targetId) return;

            if (isCombineValid) {
                if (window.htmx) {
                    window.htmx.ajax('POST', '/widgets/combine?targetWidgetId=' + encodeURIComponent(targetId) + '&sourceWidgetId=' + encodeURIComponent(sourceId), {
                        target: '#mainViewContainer',
                        swap: 'innerHTML'
                    });
                }
            } else if (isCombineInvalid) {
                const container = document.getElementById('dashboardWidgetsContainer');
                if (container) {
                    let warn = container.querySelector('.widget-combine-warning-toast');
                    if (!warn) {
                        warn = document.createElement('div');
                        warn.className = 'alert alert-warning widget-combine-warning-toast d-flex align-items-center justify-content-between p-2 mb-2';
                        warn.style.borderRadius = 'var(--radius-md, 6px)';
                        warn.style.background = 'rgba(239, 68, 68, 0.15)';
                        warn.style.border = '1px solid rgba(239, 68, 68, 0.4)';
                        warn.style.color = '#ef4444';
                        container.prepend(warn);
                    }
                    warn.innerHTML = `
                        <div class="d-flex align-items-center gap-2">
                            <span class="material-symbols-outlined" style="font-size: 1.15rem; color: #ef4444;">error</span>
                            <span>Cannot unite widgets: they are based on different contributing runs.</span>
                        </div>
                        <button type="button" class="btn-close" style="font-size: 0.75rem; filter: invert(1); opacity: 0.7; cursor: pointer; background: transparent; border: none; color: inherit;" onclick="this.closest('.alert').remove()">✕</button>
                    `;
                    setTimeout(() => { if (warn.parentNode) warn.remove(); }, 6000);
                }
            } else if (isBefore || isAfter) {
                const currentCards = Array.from(grid.querySelectorAll(':scope > .widget-card'));
                const ids = currentCards.map(c => c.getAttribute('data-widget-id')).filter(Boolean);
                const fromIdx = ids.indexOf(sourceId);
                if (fromIdx !== -1) {
                    ids.splice(fromIdx, 1);
                }
                const toIdx = ids.indexOf(targetId);
                if (toIdx !== -1) {
                    ids.splice(isBefore ? toIdx : toIdx + 1, 0, sourceId);
                } else {
                    ids.push(sourceId);
                }

                if (window.htmx) {
                    window.htmx.ajax('POST', '/widgets/reorder', {
                        target: '#mainViewContainer',
                        swap: 'innerHTML',
                        values: { widgetIds: ids.join(',') }
                    });
                }
            }
        };
    });
}
if (typeof window !== 'undefined') {
    window.initDashboardWidgetsDragAndDrop = initDashboardWidgetsDragAndDrop;
}

if (typeof window !== 'undefined' && typeof window.addEventListener === 'function' && !window._dashboardWidgetDragScrollListenersBound) {
    window._dashboardWidgetDragScrollListenersBound = true;

    window.addEventListener('dragover', (e) => {
        if (activeDraggedWidgetId) {
            updateDragAutoScrollOnPointer(e.clientY);
        }
    });

    window.addEventListener('dragend', () => {
        stopDragAutoScroll();
        activeDraggedWidgetId = null;
        activeDraggedWidgetRuns = null;
    });

    window.addEventListener('drop', () => {
        stopDragAutoScroll();
    });

    window.addEventListener('wheel', (e) => {
        if (!activeDraggedWidgetId) return;
        const container = getDashboardScrollContainer();
        if (container && typeof container.scrollBy === 'function') {
            e.preventDefault();
            container.scrollBy({ top: e.deltaY, behavior: 'instant' });
        }
    }, { passive: false });

    if (typeof document !== 'undefined' && typeof document.addEventListener === 'function') {
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                stopDragAutoScroll();
            }
        });
    }
}

// ==========================================
// CONTAINER INTERACTIVE CROSS-FILTERING
// ==========================================

function parseContainerDataset(container) {
    if (!container) return [];
    const dataScript = container.querySelector('.widget-container-dataset');
    if (!dataScript || !dataScript.textContent) return [];
    try {
        return JSON.parse(dataScript.textContent.trim() || '[]');
    } catch (e) {
        return [];
    }
}

function matchesVariationFilter(v, filterDim, filterVal) {
    if (!v) return false;
    const dim = (filterDim || '').toLowerCase().trim();
    const rawVal = (filterVal || '').trim();
    let val = rawVal.toLowerCase();

    // Check if filterVal specifies sub-mode qualifier: (healed), (ai), or (clean)
    const isHealedFilter = /\(healed\)/i.test(rawVal) || val === 'healed';
    const isAiFilter = /\(ai\)/i.test(rawVal) || val === 'ai';
    const isCleanFilter = /\(clean\)/i.test(rawVal) || val === 'clean';

    const rawStatus = (v.s || '').toLowerCase().trim();
    const vHealed = (v.h === true) || rawStatus === 'healed' || ((v.m || '').toUpperCase() === 'HEALED');
    const vAi = (v.m === 'AI') || (v.s && v.s.toLowerCase().includes('ai'));

    if (isHealedFilter && !vHealed) return false;
    if (isAiFilter && !vAi) return false;
    if (isCleanFilter && (vHealed || vAi)) return false;

    // Clean base val without (healed), (ai), (clean)
    val = val.replace(/\s*\((healed|ai|clean)\)/i, '').trim();
    if (!val || val === 'healed' || val === 'ai') {
        return true;
    }

    if (dim === 'costs' || dim === 'tokens' || val.includes('token') || val.includes('cost')) {
        return (v.tok && Number(v.tok) > 0) || (v.c && Number(v.c) > 0) || v.m === 'AI';
    }

    if (dim === 'browser') {
        const b = (v.b || '').toLowerCase().trim();
        return b === val;
    }
    if (dim === 'locale' || dim === 'location') {
        const l = (v.l || '').toLowerCase().trim();
        return l === val;
    }
    if (dim === 'env' || dim === 'environment') {
        const e = (v.e || '').toLowerCase().trim();
        return e === val;
    }
    if (dim === 'area') {
        const a = (v.a || '').toLowerCase().trim().replace(/^@/, '').replace(/\s*\(default\)$/i, '').trim();
        const cleanArea = val.replace(/^@/, '').replace(/\s*\(default\)$/i, '').trim();
        return a === cleanArea;
    }
    if (dim === 'mode') {
        if (val.includes('ai')) return v.m === 'AI' || (v.s && v.s.includes('ai'));
        if (val.includes('heal')) return v.h === true || v.m === 'HEALED';
        return !v.h && v.m !== 'AI';
    }
    if (dim === 'status') {
        const s = (v.s || '').toLowerCase().trim();
        if (val.includes('unknown') || (val.includes('fail') && !val.includes('known'))) {
            return s.includes('unknown') || (s.includes('fail') && !s.includes('known'));
        }
        if (val.includes('known') && !val.includes('unknown')) {
            return s.includes('known') && !s.includes('unknown');
        }
        if (val.includes('fixed')) {
            return s.includes('fixed');
        }
        if (val.includes('pass') || val.includes('succeed')) {
            return (s.includes('pass') || s.includes('succeed')) && !s.includes('fixed');
        }
        if (val.includes('ignore') || val.includes('skip')) {
            return s.includes('ignore') || s.includes('skip');
        }
        return s === val;
    }

    if (dim === 'severity' || dim === 'bugs' || dim === 'bug' || dim === 'category') {
        if (Array.isArray(v.bugs)) {
            const hasTicket = v.bugs.some(b => String(b).toLowerCase().trim() === val);
            if (hasTicket) return true;
        }
        if (Array.isArray(v.bsev)) {
            const hasSev = v.bsev.some(sev => String(sev).toLowerCase().trim() === val);
            if (hasSev) return true;
            if (v.bsev.length > 0 && !val.includes('known')) {
                return false;
            }
        }
        const s = (v.s || '').toLowerCase().trim();
        if (val.includes('known') || val.includes('major') || val.includes('critical') || val.includes('blocker') || val.includes('minor') || val.includes('trivial')) {
            if (Array.isArray(v.bsev) && v.bsev.length > 0) {
                return false;
            }
            return s.includes('known') || (Array.isArray(v.bugs) && v.bugs.length > 0);
        }
    }

    if (dim === 'flaky' || dim === 'flakiness') {
        const isFlaky = (v.f === true || v.f === 'true');
        if (val.includes('flaky')) return isFlaky;
        if (val.includes('stable')) return !isFlaky;
        return isFlaky;
    }

    // Custom tag lookup
    if (v.t && typeof v.t === 'object') {
        for (const k in v.t) {
            if (k.toLowerCase() === dim || dim === 'tag' || dim === 'generic') {
                const tagVal = (v.t[k] || '').toLowerCase().trim();
                if (tagVal === val) {
                    return true;
                }
            }
        }
    }

    // Direct fallback check on browser / locale / status / area
    const b = (v.b || '').toLowerCase().trim();
    if (b === val) return true;
    const l = (v.l || '').toLowerCase().trim();
    if (l === val) return true;
    const s = (v.s || '').toLowerCase().trim();
    if (s === val) return true;

    return false;
}

function normalizeStatusCategory(raw) {
    if (!raw) return 'Passed';
    const s = raw.toLowerCase().trim();
    if (s.includes('unknown') || (s.includes('fail') && !s.includes('known'))) return 'Unknown Fail';
    if (s.includes('known') && !s.includes('unknown')) return 'Known Fail';
    if (s.includes('fixed')) return 'Succeeded Fixed';
    if (s.includes('pass') || s.includes('succeed')) return 'Passed';
    if (s.includes('ignore') || s.includes('skip')) return 'Ignored';
    return raw;
}

function resolveSliceCount(rawVal, sliceCounts) {
    if (sliceCounts[rawVal] !== undefined) return sliceCounts[rawVal];
    const norm = normalizeStatusCategory(rawVal);
    if (sliceCounts[norm] !== undefined) return sliceCounts[norm];
    for (const k in sliceCounts) {
        if (k.toLowerCase() === rawVal.toLowerCase()) return sliceCounts[k];
    }
    return 0;
}

function resolveDimensionCount(rawVal, counts, dim) {
    if (!rawVal) return 0;
    if (counts[rawVal] !== undefined) return counts[rawVal];
    if (dim === 'status') {
        const norm = normalizeStatusCategory(rawVal);
        if (counts[norm] !== undefined) return counts[norm];
    }
    const rl = rawVal.toLowerCase().trim();
    for (const k in counts) {
        if (k.toLowerCase().trim() === rl) return counts[k];
    }
    const cleanRl = rl.replace(/^@/, '').replace(/\s*\(default\)$/i, '').trim();
    for (const k in counts) {
        const cleanK = k.toLowerCase().trim().replace(/^@/, '').replace(/\s*\(default\)$/i, '').trim();
        if (cleanK === cleanRl) return counts[k];
    }
    return 0;
}

function resolveVariationDimensionValue(v, dim) {
    if (!v) return 'Unknown';
    const d = (dim || '').toLowerCase().trim();
    if (d === 'flaky' || d === 'flakiness') return (v.f === true || v.f === 'true') ? 'Flaky Tests' : 'Stable Tests';
    if (d === 'browser') return v.b || 'Unknown';
    if (d === 'locale' || d === 'location') return v.l || 'Default';
    if (d === 'env' || d === 'environment') return v.e || 'Default';
    if (d === 'area') {
        const raw = v.a || 'Browsing';
        const clean = raw.replace(/^@/, '').replace(/\s*\(default\)$/i, '').trim();
        return clean.length > 0 ? clean : 'Browsing';
    }
    if (d === 'mode' || d === 'type') return v.m || (v.h ? 'Healed' : 'Classic');
    if (d === 'costs' || d === 'tokens') return ((v.tok && Number(v.tok) > 0) || (v.c && Number(v.c) > 0) || v.m === 'AI') ? 'AI / Tokens' : 'No AI';
    if (d === 'status') {
        const s = (v.s || '').toLowerCase();
        if (s.includes('unknown') || (s.includes('fail') && !s.includes('known'))) return 'Unknown Fail';
        if (s.includes('known') && !s.includes('unknown')) return 'Known Fail';
        if (s.includes('fixed')) return 'Succeeded Fixed';
        if (s.includes('pass') || s.includes('succeed')) return 'Passed';
        if (s.includes('ignore') || s.includes('skip')) return 'Ignored';
        return 'Passed';
    }
    if (v.t && typeof v.t === 'object') {
        for (const k in v.t) {
            if (k.toLowerCase() === d) return v.t[k] || 'Default';
        }
    }
    return v.b || 'Default';
}

function recalculateContainerPieChart(sec, variations, isFiltered) {
    if (!sec) return;
    const metric = (sec.getAttribute('data-metric') || 'STATUS').toUpperCase();
    if (metric === 'COSTS') {
        recalculateCostsPieChart(sec, variations, isFiltered);
    } else if (metric === 'TAG') {
        recalculateTagPieChart(sec, variations, isFiltered);
    } else if (metric === 'AI_USAGE') {
        recalculateAiUsagePieChart(sec, variations, isFiltered);
    } else if (metric === 'BUGS') {
        recalculateBugsPieChart(sec, variations, isFiltered);
    } else if (metric === 'FLAKINESS') {
        recalculateFlakinessPieChart(sec, variations, isFiltered);
    } else {
        recalculateStatusPieChart(sec, variations, isFiltered);
    }
}

function recalculateStatusPieChart(sec, variations, isFiltered) {
    if (!sec) return;
    const total = variations.length;
    let passClean = 0, passHealed = 0, passAi = 0;
    let fixedClean = 0, fixedHealed = 0, fixedAi = 0;
    let knownClean = 0, knownHealed = 0, knownAi = 0;
    let unknownClean = 0, unknownHealed = 0, unknownAi = 0;
    let ignored = 0;

    variations.forEach(v => {
        const rawStatus = (v.s || '').toLowerCase().trim();
        const healed = (v.h === true) || rawStatus === 'healed';
        const ai = (v.m === 'AI');

        if (rawStatus.includes('unknown') || (rawStatus.includes('fail') && !rawStatus.includes('known'))) {
            if (healed) unknownHealed++; else if (ai) unknownAi++; else unknownClean++;
        } else if (rawStatus.includes('known') && !rawStatus.includes('unknown')) {
            if (healed) knownHealed++; else if (ai) knownAi++; else knownClean++;
        } else if (rawStatus.includes('fixed')) {
            if (healed) fixedHealed++; else if (ai) fixedAi++; else fixedClean++;
        } else if (rawStatus.includes('ignore') || rawStatus.includes('skip')) {
            ignored++;
        } else {
            if (healed) passHealed++; else if (ai) passAi++; else passClean++;
        }
    });

    const totalPassed = passClean + passHealed + passAi;
    const totalFixed = fixedClean + fixedHealed + fixedAi;
    const totalKnown = knownClean + knownHealed + knownAi;
    const totalUnknown = unknownClean + unknownHealed + unknownAi;

    // Center KPI
    const rateEl = sec.querySelector('.pie-center-rate');
    const subEl = sec.querySelector('.pie-center-sub');
    if (rateEl) {
        if (total > 0) {
            const passPct = Math.round(((totalPassed + totalFixed) / total) * 100);
            rateEl.textContent = `${passPct}%`;
        } else {
            rateEl.textContent = '0%';
        }
    }
    if (subEl) {
        subEl.textContent = total > 0 ? 'PASS' : 'TESTS';
    }

    // Section header summary count
    const sumEl = sec.querySelector('.widget-section-header .font-mono');
    if (sumEl) {
        sumEl.textContent = `${total} Tests`;
    }

    // SVG donut circles
    const sliceCounts = {
        'Passed': totalPassed,
        'Succeeded Fixed': totalFixed,
        'Known Fail': totalKnown,
        'Unknown Fail': totalUnknown,
        'Ignored': ignored
    };
    const sliceHealedCounts = {
        'Passed': passHealed,
        'Succeeded Fixed': fixedHealed,
        'Known Fail': knownHealed,
        'Unknown Fail': unknownHealed,
        'Ignored': 0
    };
    const sliceAiCounts = {
        'Passed': passAi,
        'Succeeded Fixed': fixedAi,
        'Known Fail': knownAi,
        'Unknown Fail': unknownAi,
        'Ignored': 0
    };

    let curOffset = 25.0;
    const svgCircles = Array.from(sec.querySelectorAll('svg circle.widget-interactive-filter'));
    svgCircles.forEach(circle => {
        const rawVal = circle.getAttribute('data-filter-val') || '';
        const cnt = resolveSliceCount(rawVal, sliceCounts);
        const pct = total > 0 ? (cnt / total * 100) : 0;
        circle.setAttribute('stroke-dasharray', `${pct.toFixed(2)} ${(100 - pct).toFixed(2)}`);
        circle.setAttribute('stroke-dashoffset', `${curOffset.toFixed(2)}`);
        circle.style.display = (pct > 0) ? '' : 'none';
        curOffset -= pct;
    });

    // Breakdown list items
    const breakdownItems = Array.from(sec.querySelectorAll('.pie-breakdown-item'));
    breakdownItems.forEach(item => {
        const rawVal = item.getAttribute('data-filter-val') || '';
        const normKey = normalizeStatusCategory(rawVal);
        const cnt = resolveSliceCount(rawVal, sliceCounts);
        const pct = total > 0 ? (cnt / total * 100) : 0;
        const valSpan = item.querySelector('.val');
        if (valSpan) {
            valSpan.textContent = `${cnt} (${pct.toFixed(1)}%)`;
        }
        if (isFiltered && cnt === 0) {
            item.style.opacity = '0.35';
        } else {
            item.style.opacity = '1';
        }

        const healedCnt = sliceHealedCounts[normKey] || 0;
        const aiCnt = sliceAiCounts[normKey] || 0;
        const healedBadge = item.querySelector('.badge-healed-inline');
        if (healedBadge) {
            if (healedCnt > 0) {
                healedBadge.textContent = `✨ ${healedCnt} healed`;
                healedBadge.style.display = '';
                healedBadge.setAttribute('data-filter-dim', 'status');
                healedBadge.setAttribute('data-filter-val', `${rawVal || normKey} (Healed)`);
                healedBadge.title = `Filter healed ${rawVal || normKey} tests`;
                healedBadge.style.cursor = 'pointer';
                if (!healedBadge.classList.contains('widget-interactive-filter')) {
                    healedBadge.classList.add('widget-interactive-filter');
                }
            } else {
                healedBadge.style.display = 'none';
            }
        }
        const aiBadge = item.querySelector('.badge-ai-inline');
        if (aiBadge) {
            if (aiCnt > 0) {
                aiBadge.textContent = `${aiCnt} AI`;
                aiBadge.style.display = '';
                aiBadge.setAttribute('data-filter-dim', 'status');
                aiBadge.setAttribute('data-filter-val', `${rawVal || normKey} (AI)`);
                aiBadge.title = `Filter AI ${rawVal || normKey} tests`;
                aiBadge.style.cursor = 'pointer';
                if (!aiBadge.classList.contains('widget-interactive-filter')) {
                    aiBadge.classList.add('widget-interactive-filter');
                }
            } else {
                aiBadge.style.display = 'none';
            }
        }
    });
}

function recalculateCostsPieChart(sec, variations, isFiltered) {
    if (!sec) return;
    let totalCost = 0.0;
    let totalTokens = 0;
    variations.forEach(v => {
        if (v.c !== undefined && v.c !== null) totalCost += Number(v.c) || 0;
        if (v.tok !== undefined && v.tok !== null) totalTokens += Number(v.tok) || 0;
    });

    const rateEl = sec.querySelector('.pie-center-rate');
    const subEl = sec.querySelector('.pie-center-sub');
    if (rateEl) {
        rateEl.textContent = `$${totalCost.toFixed(2)}`;
    }
    if (subEl) {
        subEl.textContent = 'COST';
    }

    const sumEl = sec.querySelector('.widget-section-header .font-mono');
    if (sumEl) {
        sumEl.textContent = `${totalTokens.toLocaleString()} tokens`;
    }

    const promptTokens = Math.round(totalTokens * 0.70);
    const completionTokens = Math.max(0, totalTokens - promptTokens);

    const svgCircles = Array.from(sec.querySelectorAll('svg circle.widget-interactive-filter'));
    svgCircles.forEach(circle => {
        const rawVal = (circle.getAttribute('data-filter-val') || '').toLowerCase();
        let pct = 0;
        let offset = 25.0;
        if (rawVal.includes('prompt')) {
            pct = totalTokens > 0 ? 70.0 : 0;
            offset = 25.0;
        } else if (rawVal.includes('completion')) {
            pct = totalTokens > 0 ? 30.0 : 0;
            offset = -45.0;
        }
        circle.setAttribute('stroke-dasharray', `${pct.toFixed(2)} ${(100 - pct).toFixed(2)}`);
        circle.setAttribute('stroke-dashoffset', `${offset.toFixed(2)}`);
        circle.style.display = (pct > 0) ? '' : 'none';
    });

    const breakdownItems = Array.from(sec.querySelectorAll('.pie-breakdown-item'));
    breakdownItems.forEach(item => {
        const rawVal = (item.getAttribute('data-filter-val') || '').toLowerCase();
        const isPrompt = rawVal.includes('prompt');
        const cnt = isPrompt ? promptTokens : completionTokens;
        const pct = totalTokens > 0 ? (isPrompt ? 70.0 : 30.0) : 0;
        const valSpan = item.querySelector('.val');
        if (valSpan) {
            valSpan.textContent = `${cnt.toLocaleString()} (${pct.toFixed(1)}%)`;
        }
        if (isFiltered && cnt === 0) {
            item.style.opacity = '0.35';
        } else {
            item.style.opacity = '1';
        }
    });
}

function recalculateTagPieChart(sec, variations, isFiltered) {
    if (!sec) return;
    const dim = sec.getAttribute('data-tag-key') || 'browser';
    const counts = {};
    const total = variations.length;

    variations.forEach(v => {
        const val = resolveVariationDimensionValue(v, dim);
        counts[val] = (counts[val] || 0) + 1;
    });

    const rateEl = sec.querySelector('.pie-center-rate');
    const subEl = sec.querySelector('.pie-center-sub');
    if (rateEl) {
        rateEl.textContent = `${total}`;
    }
    if (subEl) {
        subEl.textContent = 'ITEMS';
    }

    const sumEl = sec.querySelector('.widget-section-header .font-mono');
    if (sumEl) {
        sumEl.textContent = `${total} Items`;
    }

    let curOffset = 25.0;
    const svgCircles = Array.from(sec.querySelectorAll('svg circle.widget-interactive-filter'));
    svgCircles.forEach(circle => {
        const rawVal = circle.getAttribute('data-filter-val') || '';
        const cnt = resolveDimensionCount(rawVal, counts, dim);
        const pct = total > 0 ? (cnt / total * 100) : 0;
        circle.setAttribute('stroke-dasharray', `${pct.toFixed(2)} ${(100 - pct).toFixed(2)}`);
        circle.setAttribute('stroke-dashoffset', `${curOffset.toFixed(2)}`);
        circle.style.display = (pct > 0) ? '' : 'none';
        curOffset -= pct;
    });

    const breakdownItems = Array.from(sec.querySelectorAll('.pie-breakdown-item'));
    breakdownItems.forEach(item => {
        const rawVal = item.getAttribute('data-filter-val') || '';
        const cnt = resolveDimensionCount(rawVal, counts, dim);
        const pct = total > 0 ? (cnt / total * 100) : 0;
        const valSpan = item.querySelector('.val');
        if (valSpan) {
            valSpan.textContent = `${cnt} (${pct.toFixed(1)}%)`;
        }
        if (isFiltered && cnt === 0) {
            item.style.opacity = '0.35';
        } else {
            item.style.opacity = '1';
        }
    });
}

function recalculateAiUsagePieChart(sec, variations, isFiltered) {
    if (!sec) return;
    let aiDriven = 0, healed = 0, playbook = 0, classic = 0;
    const total = variations.length;

    variations.forEach(v => {
        const isHealed = (v.h === true) || (v.m || '').toUpperCase() === 'HEALED';
        const isAi = (v.m || '').toUpperCase() === 'AI';
        const isPlaybook = (v.m || '').toUpperCase() === 'PLAYBOOK' && !isHealed;
        if (isAi) aiDriven++;
        else if (isHealed) healed++;
        else if (isPlaybook) playbook++;
        else classic++;
    });

    const totalAi = aiDriven + healed;
    const aiRate = total > 0 ? Math.round((totalAi / total) * 100) : 0;

    const rateEl = sec.querySelector('.pie-center-rate');
    const subEl = sec.querySelector('.pie-center-sub');
    if (rateEl) {
        rateEl.textContent = `${aiRate}%`;
    }
    if (subEl) {
        subEl.textContent = 'AI';
    }

    const sumEl = sec.querySelector('.widget-section-header .font-mono');
    if (sumEl) {
        sumEl.textContent = `${total} Tests`;
    }

    const counts = {
        'AI-Driven': aiDriven,
        'Healed': healed,
        'Playbook': playbook,
        'Classic': classic
    };

    let curOffset = 25.0;
    const svgCircles = Array.from(sec.querySelectorAll('svg circle.widget-interactive-filter'));
    svgCircles.forEach(circle => {
        const rawVal = circle.getAttribute('data-filter-val') || '';
        let cnt = counts[rawVal];
        if (cnt === undefined) {
            const rl = rawVal.toLowerCase();
            for (const k in counts) {
                if (k.toLowerCase() === rl) { cnt = counts[k]; break; }
            }
        }
        cnt = cnt || 0;
        const pct = total > 0 ? (cnt / total * 100) : 0;
        circle.setAttribute('stroke-dasharray', `${pct.toFixed(2)} ${(100 - pct).toFixed(2)}`);
        circle.setAttribute('stroke-dashoffset', `${curOffset.toFixed(2)}`);
        circle.style.display = (pct > 0) ? '' : 'none';
        curOffset -= pct;
    });

    const breakdownItems = Array.from(sec.querySelectorAll('.pie-breakdown-item'));
    breakdownItems.forEach(item => {
        const rawVal = item.getAttribute('data-filter-val') || '';
        let cnt = counts[rawVal];
        if (cnt === undefined) {
            const rl = rawVal.toLowerCase();
            for (const k in counts) {
                if (k.toLowerCase() === rl) { cnt = counts[k]; break; }
            }
        }
        cnt = cnt || 0;
        const pct = total > 0 ? (cnt / total * 100) : 0;
        const valSpan = item.querySelector('.val');
        if (valSpan) {
            valSpan.textContent = `${cnt} (${pct.toFixed(1)}%)`;
        }
        if (isFiltered && cnt === 0) {
            item.style.opacity = '0.35';
        } else {
            item.style.opacity = '1';
        }
    });
}

function getBugSeverityRank(sev) {
    if (!sev) return 0;
    switch (sev.toUpperCase().trim()) {
        case 'CRITICAL': return 4;
        case 'MAJOR': return 3;
        case 'MINOR': return 2;
        case 'TRIVIAL': return 1;
        default: return 0;
    }
}
window.getBugSeverityRank = getBugSeverityRank;

function extractVariationBugSeverities(variations, container) {
    const sevCounts = {
        'CRITICAL': 0,
        'MAJOR': 0,
        'MINOR': 0,
        'TRIVIAL': 0
    };
    const uniqueBugs = new Map();
    let domSeverityMap = null;

    variations.forEach(v => {
        if (Array.isArray(v.bugs) && v.bugs.length > 0) {
            v.bugs.forEach((ticket, idx) => {
                const cleanTicket = (ticket || '').trim();
                if (!cleanTicket) return;
                const ticketKey = cleanTicket.toLowerCase();

                let sev = (Array.isArray(v.bsev) && v.bsev[idx]) ? v.bsev[idx] : null;
                if (!sev && container) {
                    if (!domSeverityMap) {
                        domSeverityMap = new Map();
                        const groups = container.querySelectorAll('.widget-list-group');
                        groups.forEach(grp => {
                            const lblEl = grp.querySelector('.widget-list-label');
                            const grpSev = lblEl ? (lblEl.getAttribute('data-filter-val') || lblEl.innerText || '').toUpperCase().trim() : '';
                            if (grpSev) {
                                grp.querySelectorAll('.widget-list-subitem-row').forEach(sub => {
                                    const vid = (sub.getAttribute('data-variation-id') || sub.getAttribute('data-subitem-secondary') || '').trim();
                                    const vk = (sub.getAttribute('data-variation-key') || '').trim();
                                    if (vid) domSeverityMap.set(vid, grpSev);
                                    if (vk) domSeverityMap.set(vk, grpSev);
                                });
                            }
                        });
                    }
                    sev = domSeverityMap.get(v.vid) || domSeverityMap.get(v.k) || 'MAJOR';
                }
                const cleanSev = (sev || 'MAJOR').toUpperCase().trim();

                if (uniqueBugs.has(ticketKey)) {
                    const prevSev = uniqueBugs.get(ticketKey);
                    if (getBugSeverityRank(cleanSev) > getBugSeverityRank(prevSev)) {
                        uniqueBugs.set(ticketKey, cleanSev);
                    }
                } else {
                    uniqueBugs.set(ticketKey, cleanSev);
                }
            });
        }
    });

    uniqueBugs.forEach(sev => {
        sevCounts[sev] = (sevCounts[sev] || 0) + 1;
    });
    const totalBugs = uniqueBugs.size;

    return { totalBugs, sevCounts };
}
window.extractVariationBugSeverities = extractVariationBugSeverities;

function recalculateBugsPieChart(sec, variations, isFiltered) {
    if (!sec) return;
    const container = sec.closest('.widget-container-card');
    const { totalBugs, sevCounts } = extractVariationBugSeverities(variations, container);

    const rateEl = sec.querySelector('.pie-center-rate');
    const subEl = sec.querySelector('.pie-center-sub');
    if (rateEl) {
        rateEl.textContent = `${totalBugs}`;
    }
    if (subEl) {
        subEl.textContent = 'BUGS';
    }

    const sumEl = sec.querySelector('.widget-section-header .font-mono');
    if (sumEl) {
        sumEl.textContent = `${totalBugs} ${totalBugs === 1 ? 'Bug' : 'Bugs'}`;
    }

    let curOffset = 25.0;
    const svgCircles = Array.from(sec.querySelectorAll('svg circle.widget-interactive-filter'));
    svgCircles.forEach(circle => {
        const rawVal = (circle.getAttribute('data-filter-val') || '').toUpperCase().trim();
        const cnt = sevCounts[rawVal] || 0;
        const pct = totalBugs > 0 ? (cnt / totalBugs * 100) : 0;
        circle.setAttribute('stroke-dasharray', `${pct.toFixed(2)} ${(100 - pct).toFixed(2)}`);
        circle.setAttribute('stroke-dashoffset', `${curOffset.toFixed(2)}`);
        circle.style.display = (pct > 0) ? '' : 'none';
        curOffset -= pct;
    });

    const breakdownItems = Array.from(sec.querySelectorAll('.pie-breakdown-item'));
    breakdownItems.forEach(item => {
        const rawVal = (item.getAttribute('data-filter-val') || '').toUpperCase().trim();
        const cnt = sevCounts[rawVal] || 0;
        const pct = totalBugs > 0 ? (cnt / totalBugs * 100) : 0;
        const valSpan = item.querySelector('.val');
        if (valSpan) {
            valSpan.textContent = `${cnt} (${pct.toFixed(1)}%)`;
        }
        if (isFiltered && cnt === 0) {
            item.style.opacity = '0.35';
        } else {
            item.style.opacity = '1';
        }
    });
}
window.recalculateBugsPieChart = recalculateBugsPieChart;

function recalculateFlakinessPieChart(sec, variations, isFiltered) {
    if (!sec) return;
    const total = variations.length;
    let flakyCount = 0;
    variations.forEach(v => {
        if (v.f === true || v.f === 'true') {
            flakyCount++;
        }
    });
    const stableCount = total - flakyCount;
    const flakyRate = total > 0 ? (flakyCount / total * 100) : 0;

    const rateEl = sec.querySelector('.pie-center-rate');
    const subEl = sec.querySelector('.pie-center-sub');
    if (rateEl) {
        rateEl.textContent = `${Math.round(flakyRate)}%`;
    }
    if (subEl) {
        subEl.textContent = 'FLAKY';
    }

    const sumEl = sec.querySelector('.widget-section-header .font-mono');
    if (sumEl) {
        sumEl.textContent = `${total} ${total === 1 ? 'Test' : 'Tests'}`;
    }

    let curOffset = 25.0;
    const svgCircles = Array.from(sec.querySelectorAll('svg circle.widget-interactive-filter'));
    svgCircles.forEach(circle => {
        const rawVal = (circle.getAttribute('data-filter-val') || '').toLowerCase().trim();
        const cnt = rawVal.includes('flaky') ? flakyCount : stableCount;
        const pct = total > 0 ? (cnt / total * 100) : 0;
        circle.setAttribute('stroke-dasharray', `${pct.toFixed(2)} ${(100 - pct).toFixed(2)}`);
        circle.setAttribute('stroke-dashoffset', `${curOffset.toFixed(2)}`);
        circle.style.display = (pct > 0) ? '' : 'none';
        curOffset -= pct;
    });

    const breakdownItems = Array.from(sec.querySelectorAll('.pie-breakdown-item'));
    breakdownItems.forEach(item => {
        const rawVal = (item.getAttribute('data-filter-val') || '').toLowerCase().trim();
        const cnt = rawVal.includes('flaky') ? flakyCount : stableCount;
        const pct = total > 0 ? (cnt / total * 100) : 0;
        const valSpan = item.querySelector('.val');
        if (valSpan) {
            valSpan.textContent = `${cnt} (${Math.round(pct)}%)`;
        }
        if (isFiltered && cnt === 0) {
            item.style.opacity = '0.35';
        } else {
            item.style.opacity = '1';
        }
    });
}
window.recalculateFlakinessPieChart = recalculateFlakinessPieChart;

function recalculateContainerRatioBar(sec, variations, isFiltered) {
    if (!sec) return;
    const metric = (sec.getAttribute('data-metric') || '').toUpperCase();

    if (metric === 'BUGS') {
        const container = sec.closest('.widget-container-card');
        const { totalBugs, sevCounts } = extractVariationBugSeverities(variations, container);

        const sumEl = sec.querySelector('.widget-section-header .font-mono');
        if (sumEl) {
            sumEl.textContent = `${totalBugs} ${totalBugs === 1 ? 'Bug' : 'Bugs'}`;
        }

        const segments = Array.from(sec.querySelectorAll('.ratio-bar-seg'));
        segments.forEach(seg => {
            const rawVal = (seg.getAttribute('data-filter-val') || '').toUpperCase().trim();
            const cnt = sevCounts[rawVal] || 0;
            const pct = totalBugs > 0 ? (cnt / totalBugs * 100) : 0;
            seg.style.width = `${pct.toFixed(2)}%`;
            seg.innerText = (pct >= 15) ? `${Math.round(pct)}%` : '';
            seg.title = `${rawVal}: ${cnt} (${Math.round(pct)}%)`;
            seg.style.display = (cnt > 0) ? '' : 'none';
        });

        const legendChips = Array.from(sec.querySelectorAll('.ratio-legend-chip'));
        legendChips.forEach(chip => {
            const rawVal = (chip.getAttribute('data-filter-val') || '').toUpperCase().trim();
            const cnt = sevCounts[rawVal] || 0;
            const pct = totalBugs > 0 ? (cnt / totalBugs * 100) : 0;
            const valSpan = chip.querySelector('.val');
            if (valSpan) {
                valSpan.textContent = `${cnt} (${Math.round(pct)}%)`;
            }
            chip.title = `${rawVal}: ${cnt} (${Math.round(pct)}%)`;
            if (isFiltered && cnt === 0) {
                chip.style.opacity = '0.35';
            } else {
                chip.style.opacity = '1';
            }
        });
        return;
    }

    if (metric === 'FLAKINESS') {
        const total = variations.length;
        let flakyCount = 0;
        variations.forEach(v => {
            if (v.f === true || v.f === 'true') {
                flakyCount++;
            }
        });
        const stableCount = total - flakyCount;

        const sumEl = sec.querySelector('.widget-section-header .font-mono');
        if (sumEl) {
            sumEl.textContent = `${total} ${total === 1 ? 'Test' : 'Tests'}`;
        }

        const segments = Array.from(sec.querySelectorAll('.ratio-bar-seg'));
        segments.forEach(seg => {
            const rawVal = (seg.getAttribute('data-filter-val') || '').toLowerCase().trim();
            const cnt = rawVal.includes('flaky') ? flakyCount : stableCount;
            const pct = total > 0 ? (cnt / total * 100) : 0;
            seg.style.width = `${pct.toFixed(2)}%`;
            seg.innerText = (pct >= 15) ? `${Math.round(pct)}%` : '';
            seg.title = `${rawVal.includes('flaky') ? 'Flaky Tests' : 'Stable Tests'}: ${cnt} (${Math.round(pct)}%)`;
            seg.style.display = (cnt > 0) ? '' : 'none';
        });

        const legendChips = Array.from(sec.querySelectorAll('.ratio-legend-chip'));
        legendChips.forEach(chip => {
            const rawVal = (chip.getAttribute('data-filter-val') || '').toLowerCase().trim();
            const cnt = rawVal.includes('flaky') ? flakyCount : stableCount;
            const pct = total > 0 ? (cnt / total * 100) : 0;
            const valSpan = chip.querySelector('.val');
            if (valSpan) {
                valSpan.textContent = `${cnt} (${Math.round(pct)}%)`;
            }
            chip.title = `${rawVal.includes('flaky') ? 'Flaky Tests' : 'Stable Tests'}: ${cnt} (${Math.round(pct)}%)`;
            if (isFiltered && cnt === 0) {
                chip.style.opacity = '0.35';
            } else {
                chip.style.opacity = '1';
            }
        });
        return;
    }

    const dim = (metric === 'TAG')
        ? (sec.getAttribute('data-tag-key') || 'browser')
        : (metric === 'STATUS' ? 'status'
            : (metric === 'AI_USAGE' ? 'mode'
            : (metric === 'COSTS' ? 'costs'
            : (metric === 'FLAKINESS' ? 'flaky'
            : 'browser'))));
    const counts = {};
    const total = variations.length;

    variations.forEach(v => {
        const val = resolveVariationDimensionValue(v, dim);
        counts[val] = (counts[val] || 0) + 1;
    });

    const sumEl = sec.querySelector('.widget-section-header .font-mono');
    if (sumEl) {
        sumEl.textContent = `${total} Items`;
    }

    const segments = Array.from(sec.querySelectorAll('.ratio-bar-seg'));
    segments.forEach(seg => {
        const rawVal = seg.getAttribute('data-filter-val') || '';
        const cnt = resolveDimensionCount(rawVal, counts, dim);
        const pct = total > 0 ? (cnt / total * 100) : 0;
        seg.style.width = `${pct.toFixed(2)}%`;
        seg.innerText = (pct >= 15) ? `${Math.round(pct)}%` : '';
        seg.title = `${rawVal}: ${cnt} (${Math.round(pct)}%)`;
        seg.style.display = (cnt > 0) ? '' : 'none';
    });

    const legendChips = Array.from(sec.querySelectorAll('.ratio-legend-chip'));
    legendChips.forEach(chip => {
        const rawVal = chip.getAttribute('data-filter-val') || '';
        const cnt = resolveDimensionCount(rawVal, counts, dim);
        const pct = total > 0 ? (cnt / total * 100) : 0;
        const valSpan = chip.querySelector('.val');
        if (valSpan) {
            valSpan.textContent = `${cnt} (${Math.round(pct)}%)`;
        }
        chip.title = `${rawVal}: ${cnt} (${Math.round(pct)}%)`;
        if (isFiltered && cnt === 0) {
            chip.style.opacity = '0.35';
        } else {
            chip.style.opacity = '1';
        }
    });
}

function recalculateContainerList(sec, matchedVariations, isFiltered) {
    if (!sec) return;
    const metric = (sec.getAttribute('data-metric') || 'STATUS').toUpperCase();

    // Collect matched keys and variation IDs
    const matchedKeys = new Set(matchedVariations.map(v => (v.k || '').toLowerCase().trim()).filter(k => k.length > 0));
    const matchedVids = new Set(matchedVariations.map(v => (v.vid || '').toLowerCase().trim()).filter(k => k.length > 0));

    // Bug occurrences index: map normalized bug ticket -> array of occurrence descriptors
    const matchedBugOccurrences = new Map();
    if (metric === 'BUGS') {
        matchedVariations.forEach(v => {
            if (Array.isArray(v.bugs) && v.bugs.length > 0) {
                const vid = (v.vid || '').toLowerCase().trim();
                const k = (v.k || '').toLowerCase().trim();
                const b = (v.b || '').toLowerCase().trim();
                const e = (v.e || '').toLowerCase().trim();
                const l = (v.l || '').toLowerCase().trim();
                v.bugs.forEach((ticket, idx) => {
                    const normTicket = String(ticket).toLowerCase().trim();
                    if (!normTicket) return;
                    const sev = (Array.isArray(v.bsev) && v.bsev[idx]) ? String(v.bsev[idx]).toLowerCase().trim() : '';
                    if (!matchedBugOccurrences.has(normTicket)) {
                        matchedBugOccurrences.set(normTicket, []);
                    }
                    matchedBugOccurrences.get(normTicket).push({
                        vid: vid,
                        k: k,
                        browser: b,
                        env: e,
                        locale: l,
                        sev: sev
                    });
                });
            }
        });
    }

    const listGroups = sec.querySelectorAll('.widget-list-group');
    let totalVisibleSubitems = 0;
    let totalVisibleGroups = 0;

    listGroups.forEach(grp => {
        const headerRow = grp.querySelector('.widget-list-row');
        const countSpan = headerRow ? headerRow.querySelector('.font-mono') : null;
        if (countSpan && !countSpan.getAttribute('data-original-val')) {
            countSpan.setAttribute('data-original-val', countSpan.textContent.trim());
        }

        const subitems = grp.querySelectorAll('.widget-list-subitem-row');
        let visibleCount = 0;

        if (!isFiltered) {
            grp.style.removeProperty('display');
            grp.style.display = '';
            grp.classList.remove('is-filtered-hidden');
            if (headerRow) {
                headerRow.classList.remove('is-filter-active');
                headerRow.style.opacity = '1';
                headerRow.querySelectorAll('.widget-interactive-filter').forEach(f => {
                    f.classList.remove('is-filter-active');
                    f.style.opacity = '1';
                });
            }
            if (countSpan && countSpan.getAttribute('data-original-val')) {
                countSpan.textContent = countSpan.getAttribute('data-original-val');
            }
            if (subitems.length > 0) {
                subitems.forEach(sub => {
                    sub.style.removeProperty('display');
                    sub.style.display = '';
                    sub.classList.remove('is-filtered-hidden');
                });
            }
            totalVisibleGroups++;
            totalVisibleSubitems += (subitems.length > 0 ? subitems.length : 1);
            return;
        }

        if (subitems.length > 0) {
            subitems.forEach(sub => {
                const subVarKey = (sub.getAttribute('data-variation-key') || '').toLowerCase().trim();
                const subVarId = (sub.getAttribute('data-variation-id') || sub.getAttribute('data-subitem-secondary') || '').toLowerCase().trim();

                let matches = false;
                if (metric === 'BUGS') {
                    const normSubTicket = (sub.getAttribute('data-subitem-label') || sub.querySelector('.badge-status')?.innerText || '').toLowerCase().trim();
                    const occurrences = matchedBugOccurrences.get(normSubTicket);
                    if (occurrences && occurrences.length > 0) {
                        for (const occ of occurrences) {
                            let varMatch = false;
                            if (subVarId && occ.vid && subVarId === occ.vid) {
                                varMatch = true;
                            } else if (subVarKey && occ.k && subVarKey === occ.k) {
                                varMatch = true;
                            } else if (!subVarId && !subVarKey) {
                                const subTagsAttr = (sub.getAttribute('data-subitem-tags') || '').toLowerCase().trim();
                                if (subTagsAttr) {
                                    const subTagsList = subTagsAttr.split(',').map(t => t.trim()).filter(Boolean);
                                    const envTags = [];
                                    const browserTags = [];
                                    const localeTags = [];
                                    for (const st of subTagsList) {
                                        if (st.startsWith('env:') || st.startsWith('env=') || st.startsWith('environment:') || st.startsWith('environment=')) {
                                            envTags.push(st.split(/[:=]/)[1].trim());
                                        } else if (st.startsWith('browser:') || st.startsWith('browser=')) {
                                            browserTags.push(st.split(/[:=]/)[1].trim());
                                        } else if (st.startsWith('locale:') || st.startsWith('locale=') || st.startsWith('location:') || st.startsWith('location=')) {
                                            localeTags.push(st.split(/[:=]/)[1].trim());
                                        }
                                    }
                                    let tagMismatch = false;
                                    if (occ.env && envTags.length > 0 && !envTags.includes(occ.env)) tagMismatch = true;
                                    if (occ.browser && browserTags.length > 0 && !browserTags.includes(occ.browser)) tagMismatch = true;
                                    if (occ.locale && localeTags.length > 0 && !localeTags.includes(occ.locale)) tagMismatch = true;
                                    if (!tagMismatch) {
                                        varMatch = true;
                                    }
                                } else {
                                    varMatch = true;
                                }
                            }

                            if (varMatch) {
                                matches = true;
                                break;
                            }
                        }
                    }
                } else {
                    if (subVarKey && matchedKeys.has(subVarKey)) {
                        matches = true;
                    } else if (subVarId && matchedVids.has(subVarId)) {
                        matches = true;
                    } else if (subVarKey) {
                        for (const mk of matchedKeys) {
                            if (mk === subVarKey || mk.startsWith(subVarKey + '|') || subVarKey.startsWith(mk + '|')) {
                                matches = true;
                                break;
                            }
                        }
                    }
                }

                if (matches) {
                    sub.style.removeProperty('display');
                    sub.style.display = '';
                    sub.classList.remove('is-filtered-hidden');
                    visibleCount++;
                } else {
                    sub.style.display = 'none';
                    sub.style.setProperty('display', 'none', 'important');
                    sub.classList.add('is-filtered-hidden');
                }
            });

            if (visibleCount > 0) {
                grp.style.removeProperty('display');
                grp.style.display = '';
                grp.classList.remove('is-filtered-hidden');
                totalVisibleGroups++;
                totalVisibleSubitems += visibleCount;
                if (countSpan) {
                    if (metric === 'BUGS') {
                        const uniqueTickets = new Set();
                        subitems.forEach(sub => {
                            if (!sub.classList.contains('is-filtered-hidden')) {
                                const t = (sub.getAttribute('data-subitem-label') || sub.querySelector('.badge-status')?.innerText || sub.getAttribute('data-subitem-badge') || '').trim().toLowerCase();
                                if (t) uniqueTickets.add(t);
                            }
                        });
                        countSpan.textContent = `${uniqueTickets.size}`;
                    } else {
                        countSpan.textContent = `${visibleCount}`;
                    }
                }
            } else {
                grp.style.display = 'none';
                grp.style.setProperty('display', 'none', 'important');
                grp.classList.add('is-filtered-hidden');
            }
        } else {
            // Flat list group without nested subitems
            const labelEl = headerRow ? (headerRow.querySelector('.widget-list-label') || headerRow.querySelector('.widget-interactive-filter') || headerRow) : null;
            const rawVal = (labelEl ? (labelEl.getAttribute('data-filter-val') || labelEl.innerText) : '').trim();
            let matches = false;
            if (rawVal) {
                const rl = rawVal.toLowerCase();
                for (const v of matchedVariations) {
                    if (matchesVariationFilter(v, 'status', rl) || matchesVariationFilter(v, 'tag', rl) || matchesVariationFilter(v, 'mode', rl) || matchesVariationFilter(v, 'severity', rl) || matchesVariationFilter(v, 'flaky', rl)) {
                        matches = true;
                        break;
                    }
                }
            }
            if (matches) {
                grp.style.removeProperty('display');
                grp.style.display = '';
                grp.classList.remove('is-filtered-hidden');
                totalVisibleGroups++;
                totalVisibleSubitems++;
            } else {
                grp.style.display = 'none';
                grp.style.setProperty('display', 'none', 'important');
                grp.classList.add('is-filtered-hidden');
            }
        }
    });

    // Update section header summary count
    const sumEl = sec.querySelector('.widget-section-header .font-mono');
    if (sumEl) {
        if (!sumEl.getAttribute('data-original-summary')) {
            sumEl.setAttribute('data-original-summary', sumEl.textContent.trim());
        }
        if (isFiltered) {
            let totalCount = totalVisibleSubitems;
            if (metric === 'BUGS') {
                const uniqueVisibleTickets = new Set();
                sec.querySelectorAll('.widget-list-subitem-row').forEach(sub => {
                    if (!sub.classList.contains('is-filtered-hidden')) {
                        const t = (sub.getAttribute('data-subitem-label') || sub.querySelector('.badge-status')?.innerText || sub.getAttribute('data-subitem-badge') || '').trim().toLowerCase();
                        if (t) uniqueVisibleTickets.add(t);
                    }
                });
                totalCount = uniqueVisibleTickets.size;
            }
            const unit = (metric === 'BUGS') ? (totalCount === 1 ? 'Bug' : 'Bugs') : (totalCount === 1 ? 'Item' : 'Items');
            sumEl.textContent = `${totalCount} ${unit}`;
        } else {
            sumEl.textContent = sumEl.getAttribute('data-original-summary');
        }
    }

    // Toggle empty message when all list items are filtered out
    let emptyEl = sec.querySelector('.widget-list-empty-filter');
    if (isFiltered && totalVisibleGroups === 0) {
        if (!emptyEl) {
            emptyEl = document.createElement('div');
            emptyEl.className = 'widget-list-empty-filter text-muted text-center py-3';
            emptyEl.style.fontSize = '0.78rem';
            emptyEl.textContent = 'No items match current filters';
            const body = sec.querySelector('.widget-list-card-body') || sec;
            body.appendChild(emptyEl);
        } else {
            emptyEl.style.display = '';
        }
    } else if (emptyEl) {
        emptyEl.style.display = 'none';
    }
}
window.recalculateContainerList = recalculateContainerList;

function recalculateContainerText(sec, variations, isFiltered) {
    if (!sec) return;
    const metric = (sec.getAttribute('data-metric') || 'STATUS').toUpperCase();
    const total = variations.length;

    if (metric === 'AI_USAGE') {
        let aiDriven = 0, healed = 0, playbook = 0, classic = 0;
        variations.forEach(v => {
            const isHealed = (v.h === true) || (v.m || '').toUpperCase() === 'HEALED';
            const isAi = (v.m || '').toUpperCase() === 'AI';
            const isPlaybook = (v.m || '').toUpperCase() === 'PLAYBOOK' && !isHealed;
            if (isAi) aiDriven++;
            else if (isHealed) healed++;
            else if (isPlaybook) playbook++;
            else classic++;
        });

        const counts = {
            'AI-Driven': aiDriven,
            'Healed': healed,
            'Playbook': playbook,
            'Classic': classic
        };

        const segmentDivs = sec.querySelectorAll('.widget-interactive-filter[data-filter-dim="type"]');
        segmentDivs.forEach(div => {
            const rawVal = div.getAttribute('data-filter-val') || '';
            let cnt = counts[rawVal];
            if (cnt === undefined) {
                const rl = rawVal.toLowerCase();
                for (const k in counts) {
                    if (k.toLowerCase() === rl) { cnt = counts[k]; break; }
                }
            }
            cnt = cnt || 0;
            const pct = total > 0 ? Math.round((cnt / total) * 100) : 0;

            const primaryVal = div.querySelector('.widget-kpi-primary-val');
            const fontMono = div.querySelector('.font-mono');
            if (primaryVal) {
                const prevText = primaryVal.textContent.trim();
                if (prevText.endsWith('%')) {
                    primaryVal.textContent = `${pct}%`;
                    if (fontMono) fontMono.textContent = `${cnt} tests`;
                } else {
                    primaryVal.textContent = `${cnt}`;
                    if (fontMono) fontMono.textContent = `${pct}%`;
                }
            }
            if (isFiltered && cnt === 0) {
                div.style.opacity = '0.35';
            } else {
                div.style.opacity = '1';
            }
        });

        const secondaryVal = sec.querySelector('.widget-kpi-secondary-val');
        if (secondaryVal) {
            secondaryVal.textContent = `${total} Tests`;
        }

        const sumEl = sec.querySelector('.widget-section-header .font-mono');
        if (sumEl) {
            sumEl.textContent = `${total} Tests`;
        }
    } else if (metric === 'STATUS') {
        let totalPassed = 0;
        let passClean = 0, passHealed = 0, passAi = 0;
        variations.forEach(v => {
            const s = (v.s || '').toLowerCase().trim();
            const healed = (v.h === true) || s === 'healed';
            const ai = (v.m === 'AI');
            if (s.includes('pass') || s.includes('succeed') || s.includes('fixed')) {
                totalPassed++;
                if (healed) passHealed++; else if (ai) passAi++; else passClean++;
            }
        });

        const passPct = total > 0 ? Math.round((totalPassed / total) * 100) : 0;
        const primaryVal = sec.querySelector('.widget-kpi-primary-val');
        if (primaryVal) {
            primaryVal.textContent = `${passPct}% PASS`;
            if (passPct === 100) {
                primaryVal.style.color = 'var(--text-success, #10b981)';
            } else if (passPct > 0) {
                primaryVal.style.color = 'var(--text-warning, #f59e0b)';
            } else {
                primaryVal.style.color = 'var(--text-danger, #ef4444)';
            }
        }

        const secondaryVal = sec.querySelector('.widget-kpi-secondary-val');
        if (secondaryVal) {
            secondaryVal.textContent = `${totalPassed} of ${total} Tests Passing`;
        }

        const sumEl = sec.querySelector('.widget-section-header .font-mono');
        if (sumEl) {
            sumEl.textContent = `${total} Tests`;
        }

        const healedBadge = sec.querySelector('.badge-healed-inline');
        if (healedBadge) {
            if (passHealed > 0) {
                healedBadge.textContent = `✨ ${passHealed} healed`;
                healedBadge.setAttribute('data-filter-dim', 'status');
                healedBadge.setAttribute('data-filter-val', 'Passed (Healed)');
                healedBadge.title = 'Filter healed Passed tests';
                healedBadge.classList.add('widget-interactive-filter');
                healedBadge.style.display = '';
            } else {
                healedBadge.style.display = 'none';
            }
        }
        const aiBadge = sec.querySelector('.badge-ai-inline');
        if (aiBadge) {
            if (passAi > 0) {
                aiBadge.textContent = `${passAi} AI`;
                aiBadge.setAttribute('data-filter-dim', 'status');
                aiBadge.setAttribute('data-filter-val', 'Passed (AI)');
                aiBadge.title = 'Filter AI Passed tests';
                aiBadge.classList.add('widget-interactive-filter');
                aiBadge.style.display = '';
            } else {
                aiBadge.style.display = 'none';
            }
        }
    } else if (metric === 'COSTS') {
        let totalCost = 0.0;
        let totalTokens = 0;
        variations.forEach(v => {
            if (v.c !== undefined && v.c !== null) totalCost += Number(v.c) || 0;
            if (v.tok !== undefined && v.tok !== null) totalTokens += Number(v.tok) || 0;
        });

        const primaryVal = sec.querySelector('.widget-kpi-primary-val');
        if (primaryVal) {
            primaryVal.textContent = `$${totalCost.toFixed(2)}`;
        }

        const secondaryVal = sec.querySelector('.widget-kpi-secondary-val');
        if (secondaryVal) {
            secondaryVal.textContent = `${totalTokens.toLocaleString()} Tokens`;
        }

        const sumEl = sec.querySelector('.widget-section-header .font-mono');
        if (sumEl) {
            sumEl.textContent = `${totalTokens.toLocaleString()} tokens`;
        }
    } else if (metric === 'BUGS') {
        const container = sec.closest('.widget-container-card');
        const { totalBugs } = extractVariationBugSeverities(variations, container);

        const primaryVal = sec.querySelector('.widget-kpi-primary-val');
        if (primaryVal) {
            const prevText = primaryVal.textContent.trim();
            const isNumericOnly = /^\d+$/.test(prevText);
            if (isNumericOnly) {
                primaryVal.textContent = `${totalBugs}`;
            } else {
                primaryVal.textContent = `${totalBugs} ${totalBugs === 1 ? 'Bug' : 'Bugs'}`;
            }
            primaryVal.style.color = (totalBugs === 0) ? 'var(--status-pass, #10b981)' : 'var(--status-known-fail, #f97316)';
        }

        const secondaryVal = sec.querySelector('.widget-kpi-secondary-val');
        if (secondaryVal) {
            secondaryVal.textContent = `${totalBugs} ${totalBugs === 1 ? 'Active Bug' : 'Active Bugs'}`;
        }

        const sumEl = sec.querySelector('.widget-section-header .font-mono');
        if (sumEl) {
            sumEl.textContent = `${totalBugs} ${totalBugs === 1 ? 'Bug' : 'Bugs'}`;
        }
    } else if (metric === 'FLAKINESS') {
        let flakyCount = 0;
        variations.forEach(v => {
            if (v.f === true || v.f === 'true') {
                flakyCount++;
            }
        });
        const flakyPct = total > 0 ? Math.round((flakyCount / total) * 100) : 0;
        const primaryVal = sec.querySelector('.widget-kpi-primary-val');
        if (primaryVal) {
            primaryVal.textContent = `${flakyPct}% FLAKY`;
            if (flakyPct === 0) {
                primaryVal.style.color = 'var(--text-success, #10b981)';
            } else if (flakyPct < 15) {
                primaryVal.style.color = 'var(--text-warning, #f59e0b)';
            } else {
                primaryVal.style.color = 'var(--text-danger, #ef4444)';
            }
        }
        const secondaryVal = sec.querySelector('.widget-kpi-secondary-val');
        if (secondaryVal) {
            secondaryVal.textContent = `${flakyCount} of ${total} Tests Flaky`;
        }
        const sumEl = sec.querySelector('.widget-section-header .font-mono');
        if (sumEl) {
            sumEl.textContent = `${total} Tests`;
        }
    } else {
        const primaryVal = sec.querySelector('.widget-kpi-primary-val');
        if (primaryVal) {
            primaryVal.textContent = `${total}`;
        }
        const secondaryVal = sec.querySelector('.widget-kpi-secondary-val');
        if (secondaryVal) {
            secondaryVal.textContent = `${total} Items`;
        }
        const sumEl = sec.querySelector('.widget-section-header .font-mono');
        if (sumEl) {
            sumEl.textContent = `${total} Items`;
        }
    }
}
window.recalculateContainerText = recalculateContainerText;

function getContainerActiveFilters(container) {
    if (!container._activeFilters) {
        container._activeFilters = {};
    }
    return container._activeFilters;
}
window.getContainerActiveFilters = getContainerActiveFilters;

function getContainerActiveFiltersCount(activeFilters) {
    if (!activeFilters) return 0;
    let count = 0;
    for (const dim in activeFilters) {
        const set = activeFilters[dim];
        if (set) {
            count += (set.size !== undefined ? set.size : set.length);
        }
    }
    return count;
}
window.getContainerActiveFiltersCount = getContainerActiveFiltersCount;

function matchesContainerFilters(v, activeFilters, excludeDim) {
    if (!v) return false;
    if (!activeFilters) return true;
    const cleanExclude = (excludeDim || '').toLowerCase().trim();
    const normExclude = (cleanExclude === 'location' ? 'locale' : (cleanExclude === 'environment' ? 'env' : cleanExclude));

    for (const dim in activeFilters) {
        const cleanDim = dim.toLowerCase().trim();
        const normDim = (cleanDim === 'location' ? 'locale' : (cleanDim === 'environment' ? 'env' : cleanDim));
        if (cleanExclude && (cleanDim === cleanExclude || normDim === normExclude)) {
            continue;
        }
        const valSet = activeFilters[dim];
        if (!valSet || (valSet.size !== undefined ? valSet.size === 0 : valSet.length === 0)) {
            continue;
        }
        const vals = Array.from(valSet);
        const matchesAnyInDim = vals.some(val => matchesVariationFilter(v, dim, val));
        if (!matchesAnyInDim) {
            return false;
        }
    }
    return true;
}
window.matchesContainerFilters = matchesContainerFilters;

function renderContainerFilterStrip(container, activeFilters) {
    const strip = container.querySelector('.container-active-filter-strip');
    if (!strip) return;

    const totalFilters = getContainerActiveFiltersCount(activeFilters);
    if (totalFilters === 0) {
        strip.style.display = 'none';
        return;
    }

    strip.style.display = 'inline-flex';

    let chipsContainer = strip.querySelector('.container-filter-chips');
    if (!chipsContainer) {
        chipsContainer = document.createElement('div');
        chipsContainer.className = 'container-filter-chips d-inline-flex align-items-center gap-1 flex-wrap';
        strip.insertBefore(chipsContainer, strip.firstChild);
    }
    chipsContainer.innerHTML = '';

    for (const dim in activeFilters) {
        const valSet = activeFilters[dim];
        if (!valSet) continue;
        valSet.forEach(val => {
            const chip = document.createElement('span');
            chip.className = 'tag-container-filter d-inline-flex align-items-center gap-1';
            chip.setAttribute('data-filter-dim', dim);
            chip.setAttribute('data-filter-val', val);

            const isHealedFilter = val.toLowerCase().includes('(healed)');
            const isAiFilter = val.toLowerCase().includes('(ai)');

            const iconSpan = document.createElement('span');
            iconSpan.className = 'material-symbols-outlined';
            iconSpan.style.fontSize = '0.75rem';
            if (isHealedFilter) {
                iconSpan.textContent = 'auto_fix_high';
                iconSpan.style.color = '#34d399';
                chip.classList.add('filter-healed');
            } else if (isAiFilter) {
                iconSpan.textContent = 'psychology';
                iconSpan.style.color = '#a78bfa';
                chip.classList.add('filter-ai');
            } else {
                iconSpan.textContent = 'filter_alt';
            }

            const txtSpan = document.createElement('span');
            const strong = document.createElement('strong');
            strong.className = 'filter-val-text';
            strong.textContent = val;
            txtSpan.appendChild(strong);

            const closeBtn = document.createElement('button');
            closeBtn.type = 'button';
            closeBtn.className = 'btn-clear-container-filter';
            closeBtn.title = `Remove ${val} filter`;
            closeBtn.textContent = '✕';
            closeBtn.onclick = function(e) {
                removeContainerSingleFilter(this, e, dim, val);
            };

            chip.appendChild(iconSpan);
            chip.appendChild(txtSpan);
            chip.appendChild(closeBtn);
            chipsContainer.appendChild(chip);
        });
    }

    let clearAllBtn = strip.querySelector('.btn-clear-all-container-filters');
    if (!clearAllBtn) {
        clearAllBtn = document.createElement('button');
        clearAllBtn.type = 'button';
        clearAllBtn.className = 'btn-clear-all-container-filters btn-clear-ratio ms-1';
        clearAllBtn.title = 'Clear all filters';
        clearAllBtn.textContent = 'Clear all';
        clearAllBtn.onclick = function(e) {
            clearContainerCrossFilter(this, e);
        };
        strip.appendChild(clearAllBtn);
    }
    clearAllBtn.style.display = (totalFilters > 1) ? 'inline-block' : 'none';
}
window.renderContainerFilterStrip = renderContainerFilterStrip;

function updateContainerCardsVisualState(container, activeFilters) {
    const totalFilters = getContainerActiveFiltersCount(activeFilters);
    if (totalFilters === 0) {
        clearContainerCrossFilter(container);
        return;
    }

    const allVariations = parseContainerDataset(container);
    const childCards = container.querySelectorAll('.widget-child-card, .widget-container-section');

    childCards.forEach(childCard => {
        childCard.classList.add('has-active-cross-filter');

        const presentation = (childCard.getAttribute('data-presentation') || '').toUpperCase();
        const metric = (childCard.getAttribute('data-metric') || 'STATUS').toUpperCase();
        const cardDim = (metric === 'TAG')
            ? (childCard.getAttribute('data-tag-key') || 'browser').toLowerCase().trim()
            : (metric === 'STATUS' ? 'status'
                : (metric === 'AI_USAGE' ? 'mode'
                : (metric === 'COSTS' ? 'costs'
                : (metric === 'BUGS' ? 'severity'
                : (metric === 'FLAKINESS' ? 'flaky'
                : (metric === 'TEST_VARIATIONS' ? 'variations'
                : 'status')))))).toLowerCase().trim();

        // Variations for this card using facet exclusion (or none for LIST presentation to strictly hide non-matching items)
        const excludeDimForCard = (presentation === 'LIST') ? null : cardDim;
        const matchedVariations = (totalFilters > 0)
            ? allVariations.filter(v => matchesContainerFilters(v, activeFilters, excludeDimForCard))
            : allVariations;

        if (childCard) {
            if (presentation === 'PIE_CHART') {
                recalculateContainerPieChart(childCard, matchedVariations, true);
            } else if (presentation === 'RATIO_BAR') {
                recalculateContainerRatioBar(childCard, matchedVariations, true);
            } else if (presentation === 'LIST') {
                recalculateContainerList(childCard, matchedVariations, true);
            } else if (presentation === 'TEXT' || presentation === 'TEXT_WITH_SMALL_TREND') {
                recalculateContainerText(childCard, matchedVariations, true);
            }
        }

        // Update active and dimmed states on interactive filter items within this card
        const interactiveItems = childCard.querySelectorAll('.widget-interactive-filter');
        const normCardDim = (cardDim === 'location' ? 'locale' : (cardDim === 'environment' ? 'env' : cardDim));
        const activeValsInDim = activeFilters[cardDim] || activeFilters[normCardDim] || new Set();

        interactiveItems.forEach(item => {
            const itemDim = (item.getAttribute('data-filter-dim') || cardDim).toLowerCase().trim();
            const normItemDim = (itemDim === 'location' ? 'locale' : (itemDim === 'environment' ? 'env' : itemDim));
            const itemVal = (item.getAttribute('data-filter-val') || item.innerText || '').trim();
            const dimSet = activeFilters[itemDim] || activeFilters[normItemDim] || activeValsInDim;

            let isItemActive = false;
            let isParentOfActive = false;
            if (dimSet && dimSet.size > 0) {
                const itemValLower = itemVal.toLowerCase();
                for (const activeVal of dimSet) {
                    const activeValLower = activeVal.toLowerCase().trim();
                    if (activeValLower === itemValLower) {
                        isItemActive = true;
                        break;
                    }
                    if (activeValLower.startsWith(itemValLower + ' (') || activeValLower.startsWith(itemValLower + ' (healed') || activeValLower.startsWith(itemValLower + ' (ai')) {
                        isParentOfActive = true;
                    }
                }
            }

            if (isItemActive) {
                item.classList.add('is-filter-active');
                item.style.opacity = '1';
            } else if (isParentOfActive) {
                item.classList.remove('is-filter-active');
                item.style.opacity = '1';
            } else if (dimSet && dimSet.size > 0) {
                item.classList.remove('is-filter-active');
                item.style.opacity = '0.38';
            } else {
                item.classList.remove('is-filter-active');
                item.style.opacity = '1';
            }
        });
    });
}
window.updateContainerCardsVisualState = updateContainerCardsVisualState;

function onContainerWidgetFilterClick(el, event) {
    if (event) {
        event.stopPropagation();
    }
    const container = el.closest('.widget-container-card');
    if (!container) return;

    const filterDim = (el.getAttribute('data-filter-dim') || 'status').toLowerCase().trim();
    const filterVal = (el.getAttribute('data-filter-val') || el.innerText || '').trim();

    const activeFilters = getContainerActiveFilters(container);
    if (!activeFilters[filterDim]) {
        activeFilters[filterDim] = new Set();
    }
    const dimSet = activeFilters[filterDim];

    let existingEntry = null;
    for (const item of dimSet) {
        if (item.toLowerCase().trim() === filterVal.toLowerCase().trim()) {
            existingEntry = item;
            break;
        }
    }

    if (existingEntry !== null) {
        // Toggle off
        dimSet.delete(existingEntry);
        if (dimSet.size === 0) {
            delete activeFilters[filterDim];
        }
    } else {
        // Toggle on / add with superset/subset disambiguation
        const lowerVal = filterVal.toLowerCase().trim();
        if (lowerVal.endsWith('(healed)') || lowerVal.endsWith('(ai)') || lowerVal.endsWith('(clean)')) {
            const baseVal = filterVal.replace(/\s*\([^)]*\)\s*$/, '').trim().toLowerCase();
            for (const item of Array.from(dimSet)) {
                if (item.toLowerCase().trim() === baseVal) {
                    dimSet.delete(item);
                }
            }
        } else {
            for (const item of Array.from(dimSet)) {
                const itemBase = item.replace(/\s*\([^)]*\)\s*$/, '').trim().toLowerCase();
                if (itemBase === lowerVal && item.toLowerCase().trim() !== lowerVal) {
                    dimSet.delete(item);
                }
            }
        }
        dimSet.add(filterVal);
    }

    renderContainerFilterStrip(container, activeFilters);
    updateContainerCardsVisualState(container, activeFilters);
}
window.onContainerWidgetFilterClick = onContainerWidgetFilterClick;

function removeContainerSingleFilter(btn, event, optDim, optVal) {
    if (event) {
        event.stopPropagation();
    }
    const container = (btn && btn.classList && btn.classList.contains('widget-container-card'))
        ? btn
        : (btn && typeof btn.closest === 'function' ? btn.closest('.widget-container-card') : null);
    if (!container) return;

    const chip = (btn && typeof btn.closest === 'function') ? btn.closest('.tag-container-filter') : null;
    const dim = (optDim || (chip ? chip.getAttribute('data-filter-dim') : null) || '').toLowerCase().trim();
    const val = optVal || (chip ? chip.getAttribute('data-filter-val') : null);

    if (!dim || !val) {
        clearContainerCrossFilter(container, event);
        return;
    }

    const activeFilters = getContainerActiveFilters(container);
    const dimSet = activeFilters[dim];
    if (dimSet) {
        let existingEntry = null;
        for (const item of dimSet) {
            if (item.toLowerCase().trim() === val.toLowerCase().trim()) {
                existingEntry = item;
                break;
            }
        }
        if (existingEntry !== null) {
            dimSet.delete(existingEntry);
            if (dimSet.size === 0) {
                delete activeFilters[dim];
            }
        }
    }

    renderContainerFilterStrip(container, activeFilters);
    updateContainerCardsVisualState(container, activeFilters);
}
window.removeContainerSingleFilter = removeContainerSingleFilter;

function clearContainerCrossFilter(btn, event) {
    if (event) {
        event.stopPropagation();
    }
    const container = (btn && btn.classList && btn.classList.contains('widget-container-card'))
        ? btn
        : (btn && typeof btn.closest === 'function' ? btn.closest('.widget-container-card') : null);
    if (!container) return;

    delete container._activeFilters;

    const strip = container.querySelector('.container-active-filter-strip');
    if (strip) {
        strip.style.display = 'none';
        const chipsContainer = strip.querySelector('.container-filter-chips');
        if (chipsContainer) {
            chipsContainer.innerHTML = '';
        }
    }

    container.querySelectorAll('.widget-interactive-filter.is-filter-active').forEach(e => {
        e.classList.remove('is-filter-active');
    });

    const allVariations = parseContainerDataset(container);
    const childCards = container.querySelectorAll('.widget-child-card, .widget-container-section');

    childCards.forEach(childCard => {
        childCard.classList.remove('has-active-cross-filter');
        childCard.querySelectorAll('.is-filtered-hidden').forEach(el => {
            el.classList.remove('is-filtered-hidden');
            el.style.removeProperty('display');
        });

        const presentation = (childCard.getAttribute('data-presentation') || '').toUpperCase();
        if (presentation === 'PIE_CHART') {
            recalculateContainerPieChart(childCard, allVariations, false);
        } else if (presentation === 'RATIO_BAR') {
            recalculateContainerRatioBar(childCard, allVariations, false);
        } else if (presentation === 'LIST') {
            recalculateContainerList(childCard, allVariations, false);
        } else if (presentation === 'TEXT' || presentation === 'TEXT_WITH_SMALL_TREND') {
            recalculateContainerText(childCard, allVariations, false);
        }

        childCard.querySelectorAll('.widget-interactive-filter').forEach(item => {
            item.style.opacity = '1';
        });
    });
}
window.clearContainerCrossFilter = clearContainerCrossFilter;

function updateCreatePageIdentityDimensionsInput() {
    const checked = Array.from(document.querySelectorAll('input[name="createPageIdentityDimCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const hidden = document.getElementById('createPageIdentityDimensionsInput');
    if (hidden) {
        hidden.value = checked.length > 0 ? checked.join(',') : 'browser';
    }
}
window.updateCreatePageIdentityDimensionsInput = updateCreatePageIdentityDimensionsInput;

function updateCreatePageFilterInputs() {
    const bChecked = Array.from(document.querySelectorAll('input[name="createPageFilterBrowsersCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const bHidden = document.getElementById('createPageFilterBrowsersInput');
    if (bHidden) bHidden.value = bChecked.join(',');

    const lChecked = Array.from(document.querySelectorAll('input[name="createPageFilterLocalesCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const lHidden = document.getElementById('createPageFilterLocalesInput');
    if (lHidden) lHidden.value = lChecked.join(',');

    const eChecked = Array.from(document.querySelectorAll('input[name="createPageFilterEnvsCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const eHidden = document.getElementById('createPageFilterEnvsInput');
    if (eHidden) eHidden.value = eChecked.join(',');

    const aChecked = Array.from(document.querySelectorAll('input[name="createPageFilterAreasCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const aHidden = document.getElementById('createPageFilterAreasInput');
    if (aHidden) aHidden.value = aChecked.join(',');
}
window.updateCreatePageFilterInputs = updateCreatePageFilterInputs;

function setCreatePageFilterLimit(limit) {
    const input = document.getElementById('createPageFilterLimitInput');
    if (input) {
        input.value = (limit !== null && limit !== undefined) ? limit : '';
    }
}
window.setCreatePageFilterLimit = setCreatePageFilterLimit;

function clearCreatePageFilterDates() {
    const s = document.getElementById('createPageFilterStartDateInput');
    const e = document.getElementById('createPageFilterEndDateInput');
    if (s) s.value = '';
    if (e) e.value = '';
}
window.clearCreatePageFilterDates = clearCreatePageFilterDates;

function switchCreatePageTab(type) {
    const input = document.getElementById('createPageDashboardTypeInput');
    if (input) input.value = type;
    const tabStandard = document.getElementById('tabBtnStandardDashboard');
    const tabGroup = document.getElementById('tabBtnGroupDashboard');
    const groupSection = document.getElementById('groupDashboardConfigSection');
    const submitBtn = document.getElementById('submitCreatePageBtn');
    const nameDesc = document.getElementById('createPageNameDesc');
    const nameInput = document.getElementById('createPageNameInput');

    if (type === 'group') {
        if (tabStandard) tabStandard.classList.remove('active');
        if (tabGroup) tabGroup.classList.add('active');
        if (groupSection) groupSection.style.display = 'block';
        if (submitBtn) submitBtn.textContent = 'Create Group Dashboard';
        if (nameDesc) nameDesc.textContent = 'Creates a main group dashboard with overview cards and nested sub-dashboards.';
        if (nameInput && !nameInput.value.trim()) {
            const groupBy = document.getElementById('createPageGroupByInput')?.value || '';
            if (groupBy) {
                const formatted = groupBy.split(',').map(d => d.trim()).filter(Boolean).map(d => d.charAt(0).toUpperCase() + d.slice(1));
                nameInput.placeholder = `e.g. ${formatted.join(' & ')} Test Groups`;
            } else {
                nameInput.placeholder = 'e.g. Test Groups';
            }
        }
    } else {
        if (tabStandard) tabStandard.classList.add('active');
        if (tabGroup) tabGroup.classList.remove('active');
        if (groupSection) groupSection.style.display = 'none';
        if (submitBtn) submitBtn.textContent = 'Create Page';
        if (nameDesc) nameDesc.textContent = 'Creates a dedicated dashboard page where you can curate custom telemetry widgets.';
        if (nameInput) {
            nameInput.placeholder = 'e.g. Executive Summary or Performance Overview';
        }
    }
}
window.switchCreatePageTab = switchCreatePageTab;

function setCreatePageGroupBy(groupBy, pillBtn) {
    if (!pillBtn) {
        if (!groupBy) {
            document.querySelectorAll('#createPageGroupByPills .group-by-pill').forEach(btn => btn.classList.remove('active'));
            const input = document.getElementById('createPageGroupByInput');
            if (input) input.value = '';
            return;
        }
        const target = document.querySelector('#createPageGroupByPills .group-by-pill[data-group-by="' + groupBy + '"]');
        if (target) {
            document.querySelectorAll('#createPageGroupByPills .group-by-pill').forEach(btn => btn.classList.remove('active'));
            target.classList.add('active');
            const input = document.getElementById('createPageGroupByInput');
            if (input) input.value = groupBy;
        }
        return;
    }

    const isCurrentlyActive = pillBtn.classList.contains('active');
    if (isCurrentlyActive) {
        pillBtn.classList.remove('active');
    } else {
        pillBtn.classList.add('active');
    }

    const allPills = Array.from(document.querySelectorAll('#createPageGroupByPills .group-by-pill'));
    const nextActive = allPills.filter(p => p.classList.contains('active')).map(p => p.getAttribute('data-group-by'));
    const inputVal = nextActive.join(',');
    const input = document.getElementById('createPageGroupByInput');
    if (input) input.value = inputVal;

    const nameInput = document.getElementById('createPageNameInput');
    if (nameInput && (!nameInput.value || nameInput.value.endsWith('Test Groups'))) {
        if (nextActive.length > 0) {
            const formatted = nextActive.map(d => d.charAt(0).toUpperCase() + d.slice(1));
            nameInput.placeholder = `e.g. ${formatted.join(' & ')} Test Groups`;
        } else {
            nameInput.placeholder = 'e.g. Test Groups';
        }
    }
}
window.setCreatePageGroupBy = setCreatePageGroupBy;

function setCreatePageCardScope(scope) {
    const input = document.getElementById('createPageGroupCardScopeInput');
    if (input) input.value = scope;
    const help = document.getElementById('groupCardScopeHelpText');
    if (help) {
        if (scope === 'LAST_RUN') {
            help.textContent = 'Shows test results strictly from the single most recent execution run.';
        } else {
            help.textContent = 'Shows the latest known status of each test across execution history.';
        }
    }
}
window.setCreatePageCardScope = setCreatePageCardScope;

function openCreatePageModal(event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    const modal = document.getElementById('createPageModal');
    if (modal) {
        modal.style.display = 'flex';
        const input = document.getElementById('createPageNameInput');
        if (input) {
            input.value = '';
            setTimeout(() => input.focus(), 50);
        }
        const errorDiv = document.getElementById('createPageError');
        if (errorDiv) {
            errorDiv.style.display = 'none';
            errorDiv.textContent = '';
        }

        // Reset tabs to standard dashboard
        switchCreatePageTab('standard');
        document.querySelectorAll('#createPageGroupByPills .group-by-pill').forEach(btn => btn.classList.remove('active'));
        const groupByInput = document.getElementById('createPageGroupByInput');
        if (groupByInput) groupByInput.value = '';

        setCreatePageCardScope('LATEST_TEST_STATUS');
        const radioLatest = document.querySelector('input[name="createPageCardScopeRadio"][value="LATEST_TEST_STATUS"]');
        if (radioLatest) radioLatest.checked = true;

        // Reset identity checkboxes to default (Browser checked)
        document.querySelectorAll('input[name="createPageIdentityDimCheckbox"]').forEach(cb => {
            cb.checked = (cb.value === 'browser');
        });
        updateCreatePageIdentityDimensionsInput();

        // Reset filter inputs
        setCreatePageFilterLimit(null);
        clearCreatePageFilterDates();
        document.querySelectorAll('input[name="createPageFilterBrowsersCheckbox"], input[name="createPageFilterLocalesCheckbox"], input[name="createPageFilterEnvsCheckbox"], input[name="createPageFilterAreasCheckbox"]').forEach(cb => {
            cb.checked = false;
        });
        updateCreatePageFilterInputs();

        // Reset copy from page select
        const copyFromSelect = document.getElementById('createPageCopyFromSelect');
        if (copyFromSelect) {
            copyFromSelect.value = '';
        }
    }
}
window.openCreatePageModal = openCreatePageModal;

function closeCreatePageModal() {
    const modal = document.getElementById('createPageModal');
    if (modal) {
        modal.style.display = 'none';
        const input = document.getElementById('createPageNameInput');
        if (input) input.value = '';
        const errorDiv = document.getElementById('createPageError');
        if (errorDiv) errorDiv.style.display = 'none';
    }
}
window.closeCreatePageModal = closeCreatePageModal;

function setActiveNavBtn(btn) {
    document.querySelectorAll('#mainSidebar .nav-item-btn').forEach(b => b.classList.remove('active'));
    if (btn) {
        btn.classList.add('active');
    }
}
window.setActiveNavBtn = setActiveNavBtn;

async function submitCreatePage(event) {
    if (event) {
        event.preventDefault();
    }
    const form = document.getElementById('createPageForm');
    const input = document.getElementById('createPageNameInput');
    const name = input ? input.value.trim() : '';
    if (!name) {
        const errorDiv = document.getElementById('createPageError');
        if (errorDiv) {
            errorDiv.textContent = 'Please enter a valid page name.';
            errorDiv.style.display = 'block';
        }
        return;
    }
    const typeInput = document.getElementById('createPageDashboardTypeInput');
    if (typeInput && typeInput.value === 'group') {
        const groupByInput = document.getElementById('createPageGroupByInput');
        if (!groupByInput || !groupByInput.value || !groupByInput.value.trim()) {
            const errorDiv = document.getElementById('createPageError');
            if (errorDiv) {
                errorDiv.textContent = 'Please select at least one grouping dimension.';
                errorDiv.style.display = 'block';
            }
            return;
        }
    }
    updateCreatePageIdentityDimensionsInput();
    updateCreatePageFilterInputs();
    try {
        const formData = form ? new FormData(form) : new FormData();
        formData.set('name', name);
        const bodyParams = new URLSearchParams(formData);
        const res = await fetch('/dashboard-pages/create', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: bodyParams
        });
        if (res.ok) {
            const data = await res.json();
            closeCreatePageModal();
            window.location.href = '/dashboard-pages/' + data.id;
            return;
        }
    } catch (e) {
        console.warn('Direct page creation failed, falling back to form submit:', e);
    }
    if (form) {
        form.submit();
    }
}
window.submitCreatePage = submitCreatePage;

async function deleteCustomPage(pageId, pageName, event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    if (!pageId) return;
    const displayName = pageName || 'this page';
    if (!confirm('Are you sure you want to delete the dashboard page "' + displayName + '"?')) {
        return;
    }
    try {
        const res = await fetch('/dashboard-pages/delete', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams({ pageId: pageId })
        });
        window.location.href = '/';
        return;
    } catch (e) {
        console.warn('Direct page deletion failed, falling back to form submit:', e);
    }
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/dashboard-pages/delete';
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'pageId';
    input.value = pageId;
    form.appendChild(input);
    document.body.appendChild(form);
    form.submit();
}
window.deleteCustomPage = deleteCustomPage;

function updateConvertToGroupIdentityDimensionsInput() {
    const checked = Array.from(document.querySelectorAll('input[name="convertToGroupIdentityDimCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const hidden = document.getElementById('convertToGroupIdentityDimensionsInput');
    if (hidden) {
        hidden.value = checked.length > 0 ? checked.join(',') : 'browser';
    }
}
window.updateConvertToGroupIdentityDimensionsInput = updateConvertToGroupIdentityDimensionsInput;

function updateConvertToGroupFilterInputs() {
    const bChecked = Array.from(document.querySelectorAll('input[name="convertToGroupFilterBrowsersCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const bHidden = document.getElementById('convertToGroupFilterBrowsersInput');
    if (bHidden) bHidden.value = bChecked.join(',');

    const lChecked = Array.from(document.querySelectorAll('input[name="convertToGroupFilterLocalesCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const lHidden = document.getElementById('convertToGroupFilterLocalesInput');
    if (lHidden) lHidden.value = lChecked.join(',');

    const eChecked = Array.from(document.querySelectorAll('input[name="convertToGroupFilterEnvsCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const eHidden = document.getElementById('convertToGroupFilterEnvsInput');
    if (eHidden) eHidden.value = eChecked.join(',');

    const aChecked = Array.from(document.querySelectorAll('input[name="convertToGroupFilterAreasCheckbox"]:checked')).map(cb => cb.value.trim()).filter(Boolean);
    const aHidden = document.getElementById('convertToGroupFilterAreasInput');
    if (aHidden) aHidden.value = aChecked.join(',');
}
window.updateConvertToGroupFilterInputs = updateConvertToGroupFilterInputs;

function setConvertToGroupFilterLimit(limit) {
    const input = document.getElementById('convertToGroupFilterLimitInput');
    if (input) {
        input.value = (limit !== null && limit !== undefined) ? limit : '';
    }
}
window.setConvertToGroupFilterLimit = setConvertToGroupFilterLimit;

function clearConvertToGroupFilterDates() {
    const s = document.getElementById('convertToGroupFilterStartDateInput');
    const e = document.getElementById('convertToGroupFilterEndDateInput');
    if (s) s.value = '';
    if (e) e.value = '';
}
window.clearConvertToGroupFilterDates = clearConvertToGroupFilterDates;

function openConvertToGroupModal() {
    const pageEl = document.getElementById('pageCustomDashboard');
    const pageId = pageEl ? pageEl.getAttribute('data-page-id') : '';
    const pageName = pageEl ? pageEl.getAttribute('data-page-name') : 'Dashboard';
    const prohibitedCsv = pageEl ? (pageEl.getAttribute('data-prohibited-group-by') || '') : '';
    const prohibitedDims = prohibitedCsv.split(',').map(s => s.trim().toLowerCase()).filter(s => s.length > 0);

    const modal = document.getElementById('convertToGroupModal');
    if (!modal) return;
    const idInput = document.getElementById('convertToGroupPageIdInput');
    if (idInput) idInput.value = pageId;
    const title = document.getElementById('convertToGroupModalTitle');
    if (title) title.textContent = 'Convert "' + (pageName || 'Dashboard') + '" to Group';
    const errorDiv = document.getElementById('convertToGroupError');
    if (errorDiv) {
        errorDiv.style.display = 'none';
        errorDiv.textContent = '';
    }

    // Reset prohibited pills & active state
    const allPills = Array.from(document.querySelectorAll('#convertToGroupGroupByPills .group-by-pill'));
    let firstAllowedPill = null;

    allPills.forEach(pill => {
        const dim = (pill.getAttribute('data-group-by') || '').trim().toLowerCase();
        pill.classList.remove('active');
        if (prohibitedDims.includes(dim)) {
            pill.disabled = true;
            pill.classList.add('pill-disabled');
            pill.title = `Grouping by '${dim}' is prohibited because this page was already grouped by this dimension.`;
        } else {
            pill.disabled = false;
            pill.classList.remove('pill-disabled');
            pill.title = '';
            if (!firstAllowedPill) {
                firstAllowedPill = pill;
            }
        }
    });

    const submitBtn = document.getElementById('submitConvertToGroupBtn');
    const input = document.getElementById('convertToGroupGroupByInput');
    if (input) input.value = '';

    if (firstAllowedPill) {
        if (submitBtn) submitBtn.disabled = false;
    } else {
        if (submitBtn) submitBtn.disabled = true;
        if (errorDiv) {
            errorDiv.textContent = 'All available grouping dimensions have already been evaluated on this page.';
            errorDiv.style.display = 'block';
        }
    }

    setConvertToGroupCardScope('LATEST_TEST_STATUS');

    // Reset identity checkboxes: Browser checked, others unchecked
    document.querySelectorAll('input[name="convertToGroupIdentityDimCheckbox"]').forEach(cb => {
        cb.checked = (cb.value === 'browser');
    });
    updateConvertToGroupIdentityDimensionsInput();

    // Reset filter inputs
    setConvertToGroupFilterLimit(null);
    clearConvertToGroupFilterDates();
    document.querySelectorAll('input[name="convertToGroupFilterBrowsersCheckbox"], input[name="convertToGroupFilterLocalesCheckbox"], input[name="convertToGroupFilterEnvsCheckbox"], input[name="convertToGroupFilterAreasCheckbox"]').forEach(cb => {
        cb.checked = false;
    });
    updateConvertToGroupFilterInputs();

    modal.style.display = 'flex';
}
window.openConvertToGroupModal = openConvertToGroupModal;

function closeConvertToGroupModal() {
    const modal = document.getElementById('convertToGroupModal');
    if (modal) {
        modal.style.display = 'none';
        const errorDiv = document.getElementById('convertToGroupError');
        if (errorDiv) errorDiv.style.display = 'none';
    }
}
window.closeConvertToGroupModal = closeConvertToGroupModal;

function setConvertToGroupGroupBy(dim, btn) {
    if (!btn || btn.disabled || btn.classList.contains('pill-disabled')) {
        return;
    }

    const allAllowedPills = Array.from(document.querySelectorAll('#convertToGroupGroupByPills .group-by-pill'))
        .filter(p => !p.disabled && !p.classList.contains('pill-disabled'));
    const isCurrentlyActive = btn.classList.contains('active');

    if (isCurrentlyActive) {
        btn.classList.remove('active');
    } else {
        btn.classList.add('active');
    }

    const nextActive = allAllowedPills.filter(p => p.classList.contains('active')).map(p => p.getAttribute('data-group-by'));
    const inputVal = nextActive.join(',');
    const input = document.getElementById('convertToGroupGroupByInput');
    if (input) input.value = inputVal;
}
window.setConvertToGroupGroupBy = setConvertToGroupGroupBy;

function setConvertToGroupCardScope(scope) {
    const input = document.getElementById('convertToGroupCardScopeInput');
    if (input) input.value = scope;
    const radios = document.querySelectorAll('input[name="convertToGroupCardScopeRadio"]');
    radios.forEach(r => {
        r.checked = (r.value === scope);
    });
}
window.setConvertToGroupCardScope = setConvertToGroupCardScope;

async function submitConvertToGroup(event) {
    if (event) {
        event.preventDefault();
    }
    const form = document.getElementById('convertToGroupForm');
    if (!form) return;
    const idInput = document.getElementById('convertToGroupPageIdInput');
    const pageId = idInput ? idInput.value : '';
    if (!pageId) {
        const errorDiv = document.getElementById('convertToGroupError');
        if (errorDiv) {
            errorDiv.textContent = 'Invalid dashboard page ID.';
            errorDiv.style.display = 'block';
        }
        return;
    }
    const groupByInput = document.getElementById('convertToGroupGroupByInput');
    if (!groupByInput || !groupByInput.value || !groupByInput.value.trim()) {
        const errorDiv = document.getElementById('convertToGroupError');
        if (errorDiv) {
            errorDiv.textContent = 'Please select at least one grouping dimension.';
            errorDiv.style.display = 'block';
        }
        return;
    }
    updateConvertToGroupIdentityDimensionsInput();
    updateConvertToGroupFilterInputs();
    try {
        const formData = new FormData(form);
        const res = await fetch('/dashboard-pages/convert-to-group', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams(formData)
        });
        if (res.ok) {
            const data = await res.json();
            closeConvertToGroupModal();
            if (window.location.pathname === '/dashboard-pages/' + data.id) {
                window.location.reload();
            } else {
                window.location.href = '/dashboard-pages/' + data.id;
            }
            return;
        } else {
            const errData = await res.json().catch(() => null);
            const errorDiv = document.getElementById('convertToGroupError');
            if (errorDiv && errData && errData.error) {
                errorDiv.textContent = errData.error;
                errorDiv.style.display = 'block';
                return;
            }
        }
    } catch (e) {
        console.warn('Direct convert-to-group failed, falling back to form submit:', e);
    }
    form.submit();
}
window.submitConvertToGroup = submitConvertToGroup;

function toggleSidebarGroup(btn, event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    const container = btn ? btn.closest('.sidebar-page-group-container') : null;
    if (!container) return;
    const isCurrentlyCollapsed = container.classList.contains('collapsed');
    if (isCurrentlyCollapsed) {
        container.classList.remove('collapsed');
        btn.setAttribute('aria-expanded', 'true');
    } else {
        container.classList.add('collapsed');
        btn.setAttribute('aria-expanded', 'false');
    }
}
window.toggleSidebarGroup = toggleSidebarGroup;

function initSidebarGroupExpansion() {
    // If a dashboard page or sub-page is active, ensure its ancestor groups are expanded so the active link is visible
    const activeNavBtn = document.querySelector('#customPagesNav .nav-item-btn.active');
    if (activeNavBtn) {
        let parent = activeNavBtn.closest('.sidebar-page-group-container');
        while (parent) {
            parent.classList.remove('collapsed');
            const toggle = parent.querySelector(':scope > .sidebar-page-item .btn-sidebar-group-toggle');
            if (toggle) {
                toggle.setAttribute('aria-expanded', 'true');
            }
            parent = parent.parentElement ? parent.parentElement.closest('.sidebar-page-group-container') : null;
        }
    }
}
window.initSidebarGroupExpansion = initSidebarGroupExpansion;

function openCopyWidgetsModal(event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    const modal = document.getElementById('copyWidgetsModal');
    if (!modal) return;

    // Get current page ID from data attribute or hidden input
    const pageContainer = document.getElementById('pageCustomDashboard');
    const currentPageId = pageContainer ? pageContainer.getAttribute('data-page-id') : '';
    const targetInput = document.getElementById('copyWidgetsTargetPageIdInput');
    if (targetInput && currentPageId) {
        targetInput.value = currentPageId;
    }

    // Reset select
    const select = document.getElementById('copyWidgetsSourceSelect');
    if (select) {
        select.value = '';
        // Disable self if option exists
        Array.from(select.options).forEach(opt => {
            if (currentPageId && opt.value === currentPageId) {
                opt.disabled = true;
                opt.hidden = true;
            } else {
                opt.disabled = false;
                opt.hidden = false;
            }
        });
    }

    // Reset mode to replace
    const replaceRadio = document.querySelector('input[name="mode"][value="replace"]');
    if (replaceRadio) replaceRadio.checked = true;

    // Clear error
    const errorDiv = document.getElementById('copyWidgetsError');
    if (errorDiv) {
        errorDiv.textContent = '';
        errorDiv.style.display = 'none';
    }

    modal.style.display = 'flex';
}
window.openCopyWidgetsModal = openCopyWidgetsModal;

function closeCopyWidgetsModal() {
    const modal = document.getElementById('copyWidgetsModal');
    if (modal) {
        modal.style.display = 'none';
        const errorDiv = document.getElementById('copyWidgetsError');
        if (errorDiv) errorDiv.style.display = 'none';
    }
}
window.closeCopyWidgetsModal = closeCopyWidgetsModal;

async function submitCopyWidgets(event) {
    if (event) {
        event.preventDefault();
    }
    const form = document.getElementById('copyWidgetsForm');
    if (!form) return;

    const sourceSelect = document.getElementById('copyWidgetsSourceSelect');
    const sourcePageId = sourceSelect ? sourceSelect.value : '';
    const targetInput = document.getElementById('copyWidgetsTargetPageIdInput');
    const targetPageId = targetInput ? targetInput.value : '';
    const errorDiv = document.getElementById('copyWidgetsError');

    if (!sourcePageId) {
        if (errorDiv) {
            errorDiv.textContent = 'Please select a source dashboard to copy from.';
            errorDiv.style.display = 'block';
        }
        return;
    }

    if (!targetPageId) {
        if (errorDiv) {
            errorDiv.textContent = 'No target dashboard identified.';
            errorDiv.style.display = 'block';
        }
        return;
    }

    if (sourcePageId === targetPageId) {
        if (errorDiv) {
            errorDiv.textContent = 'Source and target dashboard cannot be the same.';
            errorDiv.style.display = 'block';
        }
        return;
    }

    const modeInput = form.querySelector('input[name="mode"]:checked');
    const mode = modeInput ? modeInput.value : 'replace';

    try {
        const formData = new FormData();
        formData.append('sourcePageId', sourcePageId);
        formData.append('targetPageId', targetPageId);
        formData.append('mode', mode);

        const res = await fetch('/dashboard-pages/copy-widgets', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams(formData)
        });

        if (res.ok) {
            closeCopyWidgetsModal();
            window.location.reload();
            return;
        } else {
            const data = await res.json().catch(() => ({}));
            if (errorDiv) {
                errorDiv.textContent = data.message || 'Failed to copy widgets.';
                errorDiv.style.display = 'block';
            }
            return;
        }
    } catch (e) {
        console.warn('Direct fetch failed, falling back to form submit:', e);
    }
    form.submit();
}
window.submitCopyWidgets = submitCopyWidgets;

document.addEventListener('DOMContentLoaded', () => {
    initDashboardWidgetsDragAndDrop();
    initSidebarGroupExpansion();
});

document.addEventListener('htmx:afterSwap', () => {
    initDashboardWidgetsDragAndDrop();
    initSidebarGroupExpansion();
});

