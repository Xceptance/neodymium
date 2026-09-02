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
let activeBatchName = 'Unknown';

const executionCommentsMap = {};
const executionBugMap = {};
const executionStepsMap = {};
const batchRunStatsRegistry = {};

const activeClassFilters = {};
const activeAreaFilters = {};
let activeWholeExecutionFilter = null;

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
    if (pageTitle) pageTitle.innerHTML = '<span class="material-symbols-outlined text-accent">fact_check</span> Overview of Runs & Known Batches';
    
    const breadcrumb = document.getElementById('headerBreadcrumb');
    if (breadcrumb) breadcrumb.style.display = 'none';
}

function openBatchHistoryOverview(batchName, latestRunId) {
    activeBatchName = batchName || 'Unknown';
    
    // Show Batch History Overview view state inside batch-overview fragment
    const pageRunsList = document.getElementById('pageRunsList');
    const pageBatchHistory = document.getElementById('pageBatchHistoryOverview');
    
    if (pageRunsList && pageBatchHistory) {
        pageRunsList.classList.remove('active');
        pageBatchHistory.classList.add('active');
    } else {
        const container = document.getElementById('mainViewContainer');
        if (container) {
            fetch('/fragments/batch-overview', { headers: { 'HX-Request': 'true' } })
                .then(res => res.text())
                .then(html => {
                    container.innerHTML = html;
                    if (window.htmx) htmx.process(container);
                    document.getElementById('pageRunsList')?.classList.remove('active');
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
        const rowBatchName = row.querySelector('.tag-batch')?.innerText.trim() || '';
        if (rowBatchName.includes(batchName)) {
            let clonedRow = row.cloneNode(true);
            html += `<tr class="clickable-row" hx-get="${row.getAttribute('hx-get')}" hx-target="#mainViewContainer" hx-swap="innerHTML">${clonedRow.innerHTML}</tr>`;
        }
    });

    tbody.innerHTML = html;
    htmx.process(tbody);
}

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

    const runTitle = document.getElementById('ttRunTitle') || document.getElementById('trendTooltipTitle');
    if (runTitle) runTitle.innerText = stat.title;
    const passRate = document.getElementById('ttPassRate');
    if (passRate) passRate.innerText = `${stat.rate} Pass`;
    const totalVal = document.getElementById('ttTotalVal');
    if (totalVal) totalVal.innerText = stat.total;
    const passVal = document.getElementById('ttPassVal') || document.getElementById('trendTooltipPass');
    if (passVal) passVal.innerText = stat.pass;
    const fixedVal = document.getElementById('ttFixedVal') || document.getElementById('trendTooltipFixed');
    if (fixedVal) fixedVal.innerText = stat.fixed;
    const knownVal = document.getElementById('ttKnownVal') || document.getElementById('trendTooltipKnown');
    if (knownVal) knownVal.innerText = stat.known;
    const unknownVal = document.getElementById('ttUnknownVal') || document.getElementById('trendTooltipUnknown');
    if (unknownVal) unknownVal.innerText = stat.unknown;
    const ignoredVal = document.getElementById('ttIgnoredVal') || document.getElementById('trendTooltipIgnored');
    if (ignoredVal) ignoredVal.innerText = stat.ignored;

    let ttLeft = (stat.x / 800) * svgWidth + 15;
    if (ttLeft + 220 > svgWidth) {
        ttLeft = (stat.x / 800) * svgWidth - 225;
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

function renderDynamicTrendChart(targetBatchName) {
    const batchName = targetBatchName || activeBatchName || 'Unknown';
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

    // In tables, rows are typically displayed newest to oldest. For trend charts, sort oldest to newest (left-to-right).
    const runsData = [];
    rowElements.slice().reverse().forEach(row => {
        const tagBatchEl = row.querySelector('.tag-batch span:last-child') || row.querySelector('.tag-batch');
        const rowBatch = tagBatchEl ? tagBatchEl.innerText.trim() : (row.getAttribute('data-batch') || '');
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
        const total = parseInt(row.getAttribute('data-total') || row.querySelector('td.text-mono:nth-of-type(4)')?.innerText.trim() || '0', 10) || (pass + fixed + known + unknown + ignored) || 1;
        const rate = row.getAttribute('data-rate') || (row.querySelector('.badge-status')?.innerText.trim() || '0%');
        const time = row.getAttribute('data-time') || row.querySelector('td.text-muted')?.innerText.trim() || 'Recently';

        runsData.push({ runId, pass, fixed, known, unknown, ignored, total, rate, time });
    });

    if (runsData.length === 0) {
        batchRunStatsRegistry[batchName] = { runIds: [], maxTests: 0, stats: {} };
        ['trendLayerIgnored', 'trendLayerUnknown', 'trendLayerKnown', 'trendLayerFixed', 'trendLayerPass', 'trendTopStroke'].forEach(id => {
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
            title: `Run #${r.runId} (${r.time})`,
            pass: r.pass,
            fixed: r.fixed,
            known: r.known,
            unknown: r.unknown,
            ignored: r.ignored,
            total: `${r.total} Tests`,
            rate: r.rate
        };

        const y0 = 190;
        const y1 = y0 - (r.ignored * baseScale);
        const y2 = y1 - (r.unknown * baseScale);
        const y3 = y2 - (r.known * baseScale);
        const y4 = y3 - (r.fixed * baseScale);
        const y5 = y4 - (r.pass * baseScale);
        return { x, y0, y1, y2, y3, y4, y5, id: r.runId };
    });

    batchRunStatsRegistry[batchName] = { runIds, maxTests, stats };

    if (coords.length === 1) {
        const c = coords[0];
        const halfWidth = 30;
        const leftX = Math.max(50, c.x - halfWidth);
        const rightX = Math.min(800, c.x + halfWidth);

        const pathL1 = `M ${leftX} ${c.y1} L ${rightX} ${c.y1} L ${rightX} 190 L ${leftX} 190 Z`;
        const pathL2 = `M ${leftX} ${c.y2} L ${rightX} ${c.y2} L ${rightX} ${c.y1} L ${leftX} ${c.y1} Z`;
        const pathL3 = `M ${leftX} ${c.y3} L ${rightX} ${c.y3} L ${rightX} ${c.y2} L ${leftX} ${c.y2} Z`;
        const pathL4 = `M ${leftX} ${c.y4} L ${rightX} ${c.y4} L ${rightX} ${c.y3} L ${leftX} ${c.y3} Z`;
        const pathL5 = `M ${leftX} ${c.y5} L ${rightX} ${c.y5} L ${rightX} ${c.y4} L ${leftX} ${c.y4} Z`;

        document.getElementById('trendLayerIgnored')?.setAttribute('d', pathL1);
        document.getElementById('trendLayerUnknown')?.setAttribute('d', pathL2);
        document.getElementById('trendLayerKnown')?.setAttribute('d', pathL3);
        document.getElementById('trendLayerFixed')?.setAttribute('d', pathL4);
        document.getElementById('trendLayerPass')?.setAttribute('d', pathL5);
        document.getElementById('trendTopStroke')?.setAttribute('d', `M ${leftX} ${c.y5} L ${rightX} ${c.y5}`);

        const dotsGroup = document.getElementById('trendSvgDots');
        if (dotsGroup) {
            dotsGroup.innerHTML = `
                <g class="trend-click-col" id="trendPoint-${c.id}" 
                   hx-get="/run-report?runId=${encodeURIComponent(c.id)}" hx-target="#mainViewContainer" hx-swap="innerHTML" hx-push-url="true" style="cursor: pointer;">
                    <circle cx="${c.x}" cy="${c.y5}" r="6" fill="#2563eb" stroke="#ffffff" stroke-width="2" class="trend-node-circle"/>
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
    const pathL2 = `${buildBezierTopPath('y2')} ${buildBezierReversePath('y1')} Z`;
    const pathL3 = `${buildBezierTopPath('y3')} ${buildBezierReversePath('y2')} Z`;
    const pathL4 = `${buildBezierTopPath('y4')} ${buildBezierReversePath('y3')} Z`;
    const pathL5 = `${buildBezierTopPath('y5')} ${buildBezierReversePath('y4')} Z`;

    document.getElementById('trendLayerIgnored')?.setAttribute('d', pathL1);
    document.getElementById('trendLayerUnknown')?.setAttribute('d', pathL2);
    document.getElementById('trendLayerKnown')?.setAttribute('d', pathL3);
    document.getElementById('trendLayerFixed')?.setAttribute('d', pathL4);
    document.getElementById('trendLayerPass')?.setAttribute('d', pathL5);
    document.getElementById('trendTopStroke')?.setAttribute('d', buildBezierTopPath('y5'));

    const dotsGroup = document.getElementById('trendSvgDots');
    if (dotsGroup) {
        dotsGroup.innerHTML = coords.map(c => `
            <g class="trend-click-col" id="trendPoint-${c.id}" 
               hx-get="/run-report?runId=${encodeURIComponent(c.id)}" hx-target="#mainViewContainer" hx-swap="innerHTML" hx-push-url="true" style="cursor: pointer;">
                <circle cx="${c.x}" cy="${c.y5}" r="6" fill="#2563eb" stroke="#ffffff" stroke-width="2" class="trend-node-circle"/>
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

        if (!points || points.length === 0) return;

        const maxTests = Math.max(1, ...points.map(p => p.totalCount || (p.passCount + p.fixedCount + p.knownCount + p.unknownCount + p.ignoredCount)));
        const scale = 60 / maxTests;
        const count = points.length;

        const coords = points.map((p, idx) => {
            const x = count === 1 ? 150 : 20 + (idx / (count - 1)) * 260;
            const y0 = 80;
            const y1 = y0 - ((p.ignoredCount || 0) * scale);
            const y2 = y1 - ((p.unknownCount || 0) * scale);
            const y3 = y2 - ((p.knownCount || 0) * scale);
            const y4 = y3 - ((p.fixedCount || 0) * scale);
            const y5 = y4 - ((p.passCount || 0) * scale);
            return { x, y0, y1, y2, y3, y4, y5 };
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

        let pathL1, pathL2, pathL3, pathL4, pathL5, topStroke;

        if (coords.length === 1) {
            const c = coords[0];
            const halfW = 20;
            const leftX = Math.max(20, c.x - halfW);
            const rightX = Math.min(280, c.x + halfW);

            pathL1 = `M ${leftX} ${c.y1} L ${rightX} ${c.y1} L ${rightX} 80 L ${leftX} 80 Z`;
            pathL2 = `M ${leftX} ${c.y2} L ${rightX} ${c.y2} L ${rightX} ${c.y1} L ${leftX} ${c.y1} Z`;
            pathL3 = `M ${leftX} ${c.y3} L ${rightX} ${c.y3} L ${rightX} ${c.y2} L ${leftX} ${c.y2} Z`;
            pathL4 = `M ${leftX} ${c.y4} L ${rightX} ${c.y4} L ${rightX} ${c.y3} L ${leftX} ${c.y3} Z`;
            pathL5 = `M ${leftX} ${c.y5} L ${rightX} ${c.y5} L ${rightX} ${c.y4} L ${leftX} ${c.y4} Z`;
            topStroke = `M ${leftX} ${c.y5} L ${rightX} ${c.y5}`;
        } else {
            const lastX = coords[coords.length - 1].x;
            const firstX = coords[0].x;

            pathL1 = `${buildBezierTopPath('y1')} L ${lastX} 80 L ${firstX} 80 Z`;
            pathL2 = `${buildBezierTopPath('y2')} ${buildBezierReversePath('y1')} Z`;
            pathL3 = `${buildBezierTopPath('y3')} ${buildBezierReversePath('y2')} Z`;
            pathL4 = `${buildBezierTopPath('y4')} ${buildBezierReversePath('y3')} Z`;
            pathL5 = `${buildBezierTopPath('y5')} ${buildBezierReversePath('y4')} Z`;
            topStroke = buildBezierTopPath('y5');
        }

        svg.querySelector('.area-layer-ignored')?.setAttribute('d', pathL1);
        svg.querySelector('.area-layer-unknown')?.setAttribute('d', pathL2);
        svg.querySelector('.area-layer-known')?.setAttribute('d', pathL3);
        svg.querySelector('.area-layer-fixed')?.setAttribute('d', pathL4);
        svg.querySelector('.area-layer-pass')?.setAttribute('d', pathL5);
        svg.querySelector('.area-layer-top-stroke')?.setAttribute('d', topStroke);
    });
}

function switchRunReportSubTab(subTabId, btn) {
    document.querySelectorAll('#runReportSubTabOverview, #runReportSubTabAllTests, #runReportSubTabTimeline').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.sub-tab-btn').forEach(el => el.classList.remove('active'));

    const target = document.getElementById(subTabId);
    if (target) target.classList.add('active');
    if (btn) btn.classList.add('active');

    closeSidePanelInspector();
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

    document.querySelectorAll('tr.clickable-row').forEach(r => {
        r.classList.remove('selected', 'active-selected-row');
        const marker = r.querySelector('.active-row-marker');
        if (marker) marker.remove();
    });
}

function filterAllTestsByStatus(statusKey) {
    switchRunReportSubTab('runReportSubTabAllTests', document.getElementById('runReportTabBtnAllTests'));

    if (activeWholeExecutionFilter === statusKey) {
        activeWholeExecutionFilter = null;
        statusKey = null;
    } else {
        activeWholeExecutionFilter = statusKey;
    }

    document.querySelectorAll('#runReportSubTabOverview .metric-card, #runReportSubTabOverview .kpi-card').forEach(card => card.classList.remove('active-filter'));
    document.querySelectorAll('.js-filter-badge').forEach(badge => badge.classList.remove('active-filter'));

    if (statusKey) {
        const cardMap = {
            'PASSED': 'overviewKpiPassed',
            'SUCCEEDED_FIXED': 'overviewKpiFixed',
            'FAILED_KNOWN': 'overviewKpiKnown',
            'FAILED_UNKNOWN': 'overviewKpiUnknown',
            'SKIPPED': 'overviewKpiIgnored',
            'IGNORED': 'overviewKpiIgnored',
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
        document.querySelectorAll(`.js-filter-badge[data-status-key="${statusKey}"]`).forEach(b => b.classList.add('active-filter'));
    }

    applyRunReportFilters();
}

function applyRunReportFilters() {
    const locMenu = document.getElementById('locationMultiselectMenu');
    const browserMenu = document.getElementById('browserMultiselectMenu');
    const bugMenu = document.getElementById('bugMultiselectMenu');
    const failureMenu = document.getElementById('failureMultiselectMenu');

    const selectedLocs = locMenu ? Array.from(locMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.trim().toUpperCase()) : [];
    const selectedBrowsers = browserMenu ? Array.from(browserMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.trim().toUpperCase()) : [];
    const selectedBugs = bugMenu ? Array.from(bugMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value) : [];
    const selectedFailures = failureMenu ? Array.from(failureMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value) : [];

    document.querySelectorAll('#runReportSubTabAllTests .test-row').forEach(row => {
        let loc = (row.getAttribute('data-location') || '').trim().toUpperCase();
        if (!loc) loc = 'UNKNOWN';

        let browser = (row.getAttribute('data-browser') || '').trim().toUpperCase();
        if (!browser) browser = 'UNKNOWN';

        const bugs = (row.getAttribute('data-bugs') || '').split(',');
        const failure = row.getAttribute('data-failure') || 'NONE';

        const matchLoc = selectedLocs.length === 0 || selectedLocs.includes(loc);
        const matchBrowser = selectedBrowsers.length === 0 || selectedBrowsers.includes(browser);

        let matchBug = selectedBugs.length === 0;
        if (!matchBug) {
            if (selectedBugs.includes('NONE') && (row.getAttribute('data-bugs') === 'NONE' || !row.getAttribute('data-bugs') || bugs.length === 0)) {
                matchBug = true;
            } else if (bugs.some(b => selectedBugs.includes(b))) {
                matchBug = true;
            }
        }

        let matchFailure = selectedFailures.length === 0;
        if (!matchFailure) {
            if (selectedFailures.includes('NONE') && (failure === 'NONE' || !failure)) {
                matchFailure = true;
            } else if (selectedFailures.some(f => failure.includes(f))) {
                matchFailure = true;
            }
        }

        let matchStatus = true;
        if (activeWholeExecutionFilter) {
            if (activeWholeExecutionFilter === 'SKIPPED' || activeWholeExecutionFilter === 'IGNORED') {
                const rowSt = row.getAttribute('data-status') || '';
                const rowStRaw = row.getAttribute('data-status-raw') || '';
                matchStatus = rowSt === 'SKIPPED' || rowStRaw === 'ignored' || rowStRaw === 'skipped';
            } else {
                matchStatus = row.getAttribute('data-status') === activeWholeExecutionFilter;
            }
        }

        row.style.display = (matchLoc && matchBrowser && matchBug && matchFailure && matchStatus) ? '' : 'none';
    });

    document.querySelectorAll('#runReportSubTabAllTests .test-class-container').forEach(classBox => {
        const visibleRows = classBox.querySelectorAll('.test-row:not([style*="display: none"])');
        classBox.style.display = visibleRows.length > 0 ? '' : 'none';
    });

    document.querySelectorAll('#runReportSubTabAllTests .area-group').forEach(areaBox => {
        const visibleRows = areaBox.querySelectorAll('.test-row:not([style*="display: none"])');
        areaBox.style.display = visibleRows.length > 0 ? '' : 'none';
    });

    // Filter status badges on both area group headers AND test class headers
    document.querySelectorAll('#runReportSubTabAllTests .area-group-header .badge-status, #runReportSubTabAllTests .test-class-header .badge-status').forEach(badge => {
        const badgeType = badge.getAttribute('data-status-type') || badge.getAttribute('data-status-key');
        if (activeWholeExecutionFilter) {
            if (activeWholeExecutionFilter === 'SKIPPED' || activeWholeExecutionFilter === 'IGNORED') {
                badge.style.display = (badgeType === 'SKIPPED' || badgeType === 'IGNORED') ? '' : 'none';
            } else {
                badge.style.display = (badgeType === activeWholeExecutionFilter) ? '' : 'none';
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
    document.querySelectorAll('#runReportSubTabOverview .metric-card').forEach(card => card.classList.remove('active-filter'));
    document.querySelectorAll('.js-filter-badge').forEach(b => b.classList.remove('active-filter'));
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
    if (targetEl) targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
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

function saveBugTicketLink() {
    const inputVal = document.getElementById('bugTicketInput')?.value || '';
    const parsedBugs = inputVal.split(/[, ]+/).map(s => s.trim()).filter(s => s.length > 0);
    const activeRunId = document.querySelector('.run-id-label')?.innerText.trim() || '#RUN_ID';

    if (currentActiveRowId) {
        if (parsedBugs.length > 0) {
            executionBugMap[currentActiveRowId] = parsedBugs;
            parsedBugs.forEach(bugTicket => {
                const params = new URLSearchParams({ runId: activeRunId, rowId: currentActiveRowId, bugTicket: bugTicket });
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
    toggleBugTicketFormBox();
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

    const activeRunId = document.querySelector('.run-id-label')?.innerText.trim() || '#RUN_ID';
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
            statusTd.innerHTML = `<span class="badge-status badge-healed"><span class="material-symbols-outlined" style="font-size: 0.85rem; vertical-align: middle;">healing</span> SUCCEEDED-FIXED</span>`;
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

function redirectToTestBaseVariationHistory() {
    closeTestSidePagePanel();
    const params = new URLSearchParams();
    if (currentTestNameStr) params.set('testName', currentTestNameStr);
    if (currentDataSetRaw) params.set('dataSet', currentDataSetRaw);
    if (currentLocationStr) params.set('location', currentLocationStr);
    if (currentBrowserStr) params.set('browser', currentBrowserStr);

    const queryString = params.toString() ? `?${params.toString()}` : '';
    window.location.href = `/test-base${queryString}`;
}

function getRowStatusCategory(r) {
    if (!r) return 'FAILED_UNKNOWN';
    const st = (r.getAttribute('data-status') || '').toUpperCase();
    const stRaw = (r.getAttribute('data-status-raw') || '').toLowerCase();
    const bugs = (r.getAttribute('data-bugs') || '').trim();
    const hasBugs = bugs !== '' && bugs.toUpperCase() !== 'NONE';

    if (st === 'SUCCEEDED_FIXED' || st === 'HEALED' || stRaw === 'succeeded-fixed' || stRaw === 'fixed' || stRaw === 'healed') {
        return hasBugs ? 'SUCCEEDED_FIXED' : 'PASSED';
    }
    if (st === 'PASSED' || stRaw === 'passed-clean' || stRaw === 'passed' || stRaw === 'succeeded') {
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

    let pass = 0, fixed = 0, known = 0, unknown = 0, ignored = 0;
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

    const totalExecutions = pass + fixed + known + unknown + ignored;
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
    const elOvLlmCost = document.getElementById('overviewKpiTotalLlmCost');
    const elOvLlmSub = document.getElementById('overviewKpiTotalLlmSub');

    if (elOvTotal) elOvTotal.innerText = totalExecutions;
    if (elOvPass) elOvPass.innerText = pass;
    if (elOvFixed) elOvFixed.innerText = fixed;
    if (elOvKnown) elOvKnown.innerText = known;
    if (elOvUnknown) elOvUnknown.innerText = unknown;
    if (elOvIgnored) elOvIgnored.innerText = ignored;

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

        wholeBadges.innerHTML = `
            <span class="badge-status badge-pass clickable-badge js-filter-badge${isPassActive}" onclick="filterByGlobalStatusBadge('PASSED', this)" data-status-key="PASSED" title="Filter Passed">Passed: ${pass}</span>
            <span class="badge-status badge-healed clickable-badge js-filter-badge${isFixedActive}" onclick="filterByGlobalStatusBadge('SUCCEEDED_FIXED', this)" data-status-key="SUCCEEDED_FIXED" title="Filter Succeeded-Fixed">Succeeded-Fixed: ${fixed}</span>
            <span class="badge-status badge-known-fail clickable-badge js-filter-badge${isKnownActive}" onclick="filterByGlobalStatusBadge('FAILED_KNOWN', this)" data-status-key="FAILED_KNOWN" title="Filter Known Fail">Known Fail: ${known}</span>
            <span class="badge-status badge-unknown-fail clickable-badge js-filter-badge${isUnknownActive}" onclick="filterByGlobalStatusBadge('FAILED_UNKNOWN', this)" data-status-key="FAILED_UNKNOWN" title="Filter Unknown Fail">Unknown Fail: ${unknown}</span>
            <span class="badge-status badge-ignored clickable-badge js-filter-badge${isSkippedActive}" onclick="filterByGlobalStatusBadge('SKIPPED', this)" data-status-key="SKIPPED" title="Filter Ignored/Skipped">Ignored: ${ignored}</span>
        `;
    }

    // Recalculate Area Breakdown and Pie Charts on Overview tab & Area Group Headers on All Tests tab
    document.querySelectorAll('#runReportSubTabAllTests .area-group, .area-group').forEach(areaGroup => {
        const areaName = areaGroup.getAttribute('data-area') || '';
        const areaRows = areaGroup.querySelectorAll('.test-row, tr.execution-row');

        let aPass = 0, aFixed = 0, aKnown = 0, aUnknown = 0, aIgnored = 0;
        areaRows.forEach(r => {
            const cat = getRowStatusCategory(r);
            if (cat === 'PASSED') aPass++;
            else if (cat === 'SUCCEEDED_FIXED') aFixed++;
            else if (cat === 'FAILED_KNOWN') aKnown++;
            else if (cat === 'FAILED_UNKNOWN') aUnknown++;
            else if (cat === 'SKIPPED') aIgnored++;
        });

        const totalArea = aPass + aFixed + aKnown + aUnknown + aIgnored;

        const areaExecChip = areaGroup.querySelector('.area-exec-count-chip');
        if (areaExecChip) {
            areaExecChip.innerText = `${totalArea} Executions`;
        }

        const areaBadgesContainer = areaGroup.querySelector('.area-summary-badges') || areaGroup.querySelector('.area-group-header .flex-gap-2');
        if (areaBadgesContainer) {
            const activeFilter = areaGroup.getAttribute('data-active-status-filter') || 'ALL';
            let badgesHtml = '';
            if (aPass > 0) badgesHtml += `<span class="badge-status badge-pass clickable-badge${activeFilter === 'PASSED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'PASSED');" title="Filter Passed in Area">Passed: ${aPass}</span>`;
            if (aFixed > 0) badgesHtml += `<span class="badge-status badge-healed clickable-badge${activeFilter === 'SUCCEEDED_FIXED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'SUCCEEDED_FIXED');" title="Filter Succeeded-Fixed in Area">Succeeded-Fixed: ${aFixed}</span>`;
            if (aKnown > 0) badgesHtml += `<span class="badge-status badge-known-fail clickable-badge${activeFilter === 'FAILED_KNOWN' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'FAILED_KNOWN');" title="Filter Known Fail in Area">Known: ${aKnown}</span>`;
            if (aUnknown > 0) badgesHtml += `<span class="badge-status badge-unknown-fail clickable-badge${activeFilter === 'FAILED_UNKNOWN' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'FAILED_UNKNOWN');" title="Filter Unknown Fail in Area">Unknown: ${aUnknown}</span>`;
            if (aIgnored > 0) badgesHtml += `<span class="badge-status badge-ignored clickable-badge${activeFilter === 'SKIPPED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterAreaByStatusBadge(this, 'SKIPPED');" title="Filter Ignored in Area">Ignored: ${aIgnored}</span>`;
            areaBadgesContainer.innerHTML = badgesHtml;
        }

        const pieCard = document.querySelector(`.area-pie-card[data-area="${CSS.escape(areaName)}"]`);
        if (pieCard) {
            const valPass = pieCard.querySelector('.val-pass');
            const valFixed = pieCard.querySelector('.val-fixed');
            const valKnown = pieCard.querySelector('.val-known');
            const valUnknown = pieCard.querySelector('.val-unknown');
            const valIgnored = pieCard.querySelector('.val-ignored');

            if (valPass) valPass.innerText = aPass;
            if (valFixed) valFixed.innerText = aFixed;
            if (valKnown) valKnown.innerText = aKnown;
            if (valUnknown) valUnknown.innerText = aUnknown;
            if (valIgnored) valIgnored.innerText = aIgnored;

            const svgWrapper = pieCard.querySelector('.pie-chart-wrapper');
            if (svgWrapper && totalArea > 0) {
                const pPass = (aPass / totalArea) * 100;
                const pFixed = (aFixed / totalArea) * 100;
                const pKnown = (aKnown / totalArea) * 100;
                const pUnknown = (aUnknown / totalArea) * 100;
                const pIgnored = (aIgnored / totalArea) * 100;

                const cPass = `${pPass} ${100 - pPass}`;
                const cFixed = `${pFixed} ${100 - pFixed}`;
                const cKnown = `${pKnown} ${100 - pKnown}`;
                const cUnknown = `${pUnknown} ${100 - pUnknown}`;
                const cIgnored = `${pIgnored} ${100 - pIgnored}`;

                svgWrapper.innerHTML = `
                    <svg width="100%" height="100%" viewBox="0 0 36 36">
                        <circle cx="18" cy="18" r="15.915" fill="none" stroke="#e2e8f0" stroke-width="4"/>
                        <circle cx="18" cy="18" r="15.915" fill="none" stroke="#059669" stroke-width="5" stroke-dasharray="${cPass}" stroke-dashoffset="25"/>
                        <circle cx="18" cy="18" r="15.915" fill="none" stroke="#2563eb" stroke-width="5" stroke-dasharray="${cFixed}" stroke-dashoffset="${25 - pPass}"/>
                        <circle cx="18" cy="18" r="15.915" fill="none" stroke="#ea580c" stroke-width="5" stroke-dasharray="${cKnown}" stroke-dashoffset="${25 - pPass - pFixed}"/>
                        <circle cx="18" cy="18" r="15.915" fill="none" stroke="#dc2626" stroke-width="5" stroke-dasharray="${cUnknown}" stroke-dashoffset="${25 - pPass - pFixed - pKnown}"/>
                        <circle cx="18" cy="18" r="15.915" fill="none" stroke="#64748b" stroke-width="5" stroke-dasharray="${cIgnored}" stroke-dashoffset="${25 - pPass - pFixed - pKnown - pUnknown}"/>
                    </svg>
                `;
            }
        }
    });

    // Recalculate Test Class Summary Badges
    document.querySelectorAll('#runReportSubTabAllTests .test-class-container, .test-class-container').forEach(classBox => {
        const classRows = classBox.querySelectorAll('.test-row, tr.execution-row');
        let cPass = 0, cFixed = 0, cKnown = 0, cUnknown = 0, cIgnored = 0;
        classRows.forEach(r => {
            const cat = getRowStatusCategory(r);
            if (cat === 'PASSED') cPass++;
            else if (cat === 'SUCCEEDED_FIXED') cFixed++;
            else if (cat === 'FAILED_KNOWN') cKnown++;
            else if (cat === 'FAILED_UNKNOWN') cUnknown++;
            else if (cat === 'SKIPPED') cIgnored++;
        });

        const classBadgeContainer = classBox.querySelector('.test-class-header .flex-gap-2');
        if (classBadgeContainer) {
            const activeFilter = classBox.getAttribute('data-active-status-filter') || 'ALL';
            let html = '';
            if (cPass > 0) html += `<span class="badge-status badge-pass clickable-badge${activeFilter === 'PASSED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'PASSED');" title="Filter Passed in Class">Passed: ${cPass}</span>`;
            if (cFixed > 0) html += `<span class="badge-status badge-healed clickable-badge${activeFilter === 'SUCCEEDED_FIXED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'SUCCEEDED_FIXED');" title="Filter Succeeded-Fixed in Class">Succeeded-Fixed: ${cFixed}</span>`;
            if (cKnown > 0) html += `<span class="badge-status badge-known-fail clickable-badge${activeFilter === 'FAILED_KNOWN' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'FAILED_KNOWN');" title="Filter Known Fail in Class">Known: ${cKnown}</span>`;
            if (cUnknown > 0) html += `<span class="badge-status badge-unknown-fail clickable-badge${activeFilter === 'FAILED_UNKNOWN' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'FAILED_UNKNOWN');" title="Filter Unknown Fail in Class">Unknown: ${cUnknown}</span>`;
            if (cIgnored > 0) html += `<span class="badge-status badge-ignored clickable-badge${activeFilter === 'SKIPPED' ? ' active-filter-badge' : ''}" onclick="event.stopPropagation(); filterClassByStatusBadge(this, 'SKIPPED');" title="Filter Ignored in Class">Ignored: ${cIgnored}</span>`;
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

    const activeRunId = runId || activeRow?.getAttribute('data-run-id') || document.querySelector('.run-id-label')?.innerText.trim() || '#RUN_ID';
    const activeRowId = rowId || activeRow?.getAttribute('data-row-id') || activeRow?.id || '';
    const isTestBaseRow = activeRow ? (activeRow.closest('#pageTestBase') !== null) : (document.getElementById('pageTestBase') !== null);

    // Synchronize form inputs for bug ticket entry
    const bugRunInput = document.querySelector('#bugTicketFormBox input[name="runId"]');
    const bugRowInput = document.querySelector('#bugTicketFormBox input[name="rowId"]');
    if (bugRunInput) bugRunInput.value = activeRunId;
    if (bugRowInput) bugRowInput.value = activeRowId;

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

    const singleRunControls = document.getElementById('sidePageSingleRunControls');
    const testBaseControls = document.getElementById('sidePageTestBaseVariationControls');

    if (isTestBaseRow && testBaseControls) {
        if (singleRunControls) singleRunControls.style.display = 'none';
        testBaseControls.style.display = 'flex';

        const loc = activeRow?.getAttribute('data-location') || 'Unknown';
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
        const savedWidth = localStorage.getItem('aura_side_panel_width');
        if (savedWidth) {
            const parsedWidth = parseInt(savedWidth, 10);
            panel.style.width = (isTestBaseRow ? Math.max(920, parsedWidth) : parsedWidth) + 'px';
        } else {
            panel.style.width = '920px';
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

function renderStepsForExecution(activeRow) {
    const stepListEl = document.getElementById('sidePageStepList');
    if (!stepListEl) return;

    if (!activeRow) {
        stepListEl.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem; padding: 0.5rem;">Select a test execution row to view steps.</div>';
        return;
    }

    const blocksAttr = activeRow.getAttribute('data-blocks');
    const stepsAttr = activeRow.getAttribute('data-steps');

    if ((!blocksAttr || blocksAttr === 'null' || blocksAttr === '{}') && (!stepsAttr || stepsAttr === 'null' || stepsAttr === '{}')) {
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
                        if (execDto.blocksJson) activeRow.setAttribute('data-blocks', execDto.blocksJson);
                        if (execDto.stepsJson) activeRow.setAttribute('data-steps', execDto.stepsJson);
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
        if (failureText && failureText !== 'NONE' && failureText.trim().length > 0) {
            errorTextEl.innerText = failureText.trim();
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
                    <td class="param-val">${activeRow.getAttribute('data-location') || 'Unknown'}</td>
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
    const statusVal = activeRow.getAttribute('data-status') || 'PASSED';

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
    if (sideStatusEl) sideStatusEl.innerHTML = `Status: <strong class="${statusVal === 'PASSED' ? 'status-pass' : (statusVal === 'HEALED' ? 'status-fixed' : 'status-fail')}">${statusVal}</strong>`;
    if (sideStepsEl) sideStepsEl.innerText = stepsTotal;
    if (sideStepHealthEl) sideStepHealthEl.innerText = `✨ Healed: ${healedCount} | ❌ Failed: ${failedCount}`;
    if (sideLlmCallsEl) sideLlmCallsEl.innerText = llmCalls;
    if (sideTokensEl) sideTokensEl.innerText = `Tokens: ${Number(totalTokens).toLocaleString()} (Cached: ${Number(cachedTokens).toLocaleString()})`;
    if (sideCostEl) sideCostEl.innerText = estCost.startsWith('$') ? estCost : `$${Number(estCost).toFixed(4)}`;
    if (sideTokenInOutEl) sideTokenInOutEl.innerText = `In: ${Number(inputTokens).toLocaleString()} | Out: ${Number(outputTokens).toLocaleString()}`;

    // Populate AI LLM Responsibility & Token Accounting Table
    const llmTbody = document.getElementById('sidePageLlmAccountingTbody');
    if (llmTbody) {
        const costNum = aiMetrics ? aiMetrics.totalCost : Number(String(estCost).replace('$', '')) || 0;
        llmTbody.innerHTML = `
            <tr style="font-weight: 700; background: #ffffff;">
                <td style="padding: 0.35rem 0.6rem;">Total</td>
                <td style="text-align: center; padding: 0.35rem 0.6rem;">${llmCalls}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(totalTokens).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(inputTokens).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(outputTokens).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(cachedTokens).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;" class="text-accent">${sideCostEl ? sideCostEl.innerText : '$0.0000'}</td>
            </tr>
            <tr>
                <td style="padding: 0.35rem 0.6rem;">Action (Standard Generation)</td>
                <td style="text-align: center; padding: 0.35rem 0.6rem;">${Math.ceil(Number(llmCalls) * 0.5)}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(Math.floor(Number(totalTokens) * 0.92)).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(Math.floor(Number(inputTokens) * 0.92)).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(Math.floor(Number(outputTokens) * 0.92)).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(cachedTokens).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;" class="text-accent">$${(costNum * 0.92).toFixed(4)}</td>
            </tr>
            <tr>
                <td style="padding: 0.35rem 0.6rem;">PESAP (Pre-Execution Semantic Anchor)</td>
                <td style="text-align: center; padding: 0.35rem 0.6rem;">${Math.floor(Number(llmCalls) * 0.5)}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(Math.floor(Number(totalTokens) * 0.08)).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(Math.floor(Number(inputTokens) * 0.08)).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">${Number(Math.floor(Number(outputTokens) * 0.08)).toLocaleString()}</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;">0</td>
                <td style="text-align: right; padding: 0.35rem 0.6rem;" class="text-accent">$${(costNum * 0.08).toFixed(4)}</td>
            </tr>
        `;
    }

    const ctxBadgesEl = document.getElementById('sidePageContextLevelBadges');
    if (ctxBadgesEl) {
        ctxBadgesEl.innerHTML = `
            <span class="context-badge level-minimal">MINIMAL: <strong>${Math.max(1, Math.floor(Number(stepsTotal) * 0.6))}</strong></span>
            <span class="context-badge level-lean">LEAN: <strong>${Math.max(0, Math.floor(Number(stepsTotal) * 0.2))}</strong></span>
            <span class="context-badge level-standard">STANDARD: <strong>${Math.max(0, Math.floor(Number(stepsTotal) * 0.1))}</strong></span>
            <span class="context-badge level-visual">VISUAL: <strong>${Math.max(0, Math.floor(Number(stepsTotal) * 0.1))}</strong></span>
        `;
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

    if (!blocksObj) {
        stepListEl.innerHTML = '<div style="color: var(--text-muted); font-size: 0.85rem; padding: 0.5rem;">No step execution data recorded for this run.</div>';
        return;
    }

    const beforeSteps = blocksObj.before || [];
    const coreSteps = blocksObj.steps || [];
    const afterSteps = blocksObj.after || [];

    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    const failureReason = activeRow.getAttribute('data-failure-reason') || blocksObj?.failureReason || '';
    const failureStackTrace = activeRow.getAttribute('data-failure-stack-trace') || blocksObj?.failureStackTrace || '';
    const visualRcaExplanation = activeRow.getAttribute('data-visual-rca-explanation') || blocksObj?.visualRcaExplanation || '';

    if (errorCard && errorTextEl) {
        const errorParts = [];
        if (failureText && failureText !== 'NONE' && failureText.trim().length > 0) {
            errorParts.push(failureText.trim());
        }
        if (failureReason && failureReason.trim().length > 0) {
            errorParts.push(`Failure Reason:\n${failureReason.trim()}`);
        }
        if (visualRcaExplanation && visualRcaExplanation.trim().length > 0) {
            errorParts.push(`Visual Root Cause Analysis (RCA):\n${visualRcaExplanation.trim()}`);
        }

        const allStackTraces = [];
        if (failureStackTrace && failureStackTrace.trim().length > 0) {
            allStackTraces.push(failureStackTrace.trim());
        }

        if (errorParts.length > 0 || allStackTraces.length > 0) {
            let htmlContent = '';
            if (errorParts.length > 0) {
                htmlContent += `<div style="white-space: pre-wrap; word-break: break-word;">${escapeHtml(errorParts.join('\n\n'))}</div>`;
            }
            if (allStackTraces.length > 0) {
                const combinedTrace = allStackTraces.join('\n\n');
                htmlContent += `
                    <details class="error-stacktrace-collapsible">
                        <summary class="error-stacktrace-summary">
                            <span class="material-symbols-outlined" style="font-size: 0.95rem;">terminal</span>
                            Failure Stack Trace
                        </summary>
                        <pre class="error-stacktrace-pre">${escapeHtml(combinedTrace)}</pre>
                    </details>
                `;
            }
            errorTextEl.innerHTML = htmlContent;
            errorCard.style.display = 'flex';
        } else {
            errorCard.style.display = 'none';
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

        stepsArray.forEach((s, idx) => {
            const stepNum = s.index || s.stepNum || (idx + 1);
            const title = s.instruction || s.name || s.title || `Step ${stepNum}`;
            const status = s.status || (s.passed === false ? 'failed' : 'passed');
            const isPassed = status === 'passed' || status === 'passed-clean' || s.passed === true;
            const isFailed = status === 'failed' || status === 'failed-unknown' || status === 'failed-known' || s.passed === false;
            
            const stepClass = isPassed ? 'step-passed' : (isFailed ? 'step-failed' : 'step-ignored');
            const stepId = `stepCard_${sectionClass}_${idx}`;
			console.log(s.stats);
            const contextLevel = s.stats.contextLevels || (idx % 2 === 0 ? 'MINIMAL' : 'LEAN');

            // Format Timing
            const rawStart = s.startTimestamp || s.startTime || s.startTimeMs;
            let formattedStart = '';
            if (rawStart) {
                if (typeof rawStart === 'number') {
                    formattedStart = new Date(rawStart).toLocaleTimeString();
                } else if (typeof rawStart === 'string') {
                    formattedStart = rawStart;
                }
            }

            let rawDuration = s.duration !== undefined && s.duration !== null ? s.duration : (s.durationMs !== undefined ? s.durationMs : null);
            let formattedDuration = '0 min 0 s';
            if (rawDuration !== null && rawDuration !== undefined) {
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
                    const actTarget = act.target || '';
                    const actVal = act.value ? ` ➔ ${act.value}` : '';
                    const actDesc = act.description ? ` (${act.description})` : '';
                    const hasSuccess = typeof act.success === 'boolean';
                    const successModifier = hasSuccess ? (act.success ? ' action-success' : ' action-failed') : '';
                    const successBadge = hasSuccess
                        ? `<span class="action-status-badge ${act.success ? 'action-status-ok' : 'action-status-fail'}" title="${act.success ? 'Action succeeded' : 'Action failed'}"><span class="material-symbols-outlined">${act.success ? 'check_circle' : 'cancel'}</span></span>`
                        : '';
                    const actReasoning = act.reasoning
                        ? `<div class="action-reasoning"><span class="material-symbols-outlined">psychology</span><span class="action-reasoning-text">${act.reasoning}</span></div>`
                        : '';

                    actionsHtml += `
                        <div class="action-item-pill-row${successModifier}">
                            ${successBadge}
                            <span class="action-type-pill">${actName}</span>
                            <span class="action-code-text">${actTarget}${actVal}${actDesc}</span>
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
                    subStepsHtml += `
                        <div class="sub-step-card">
                            <span class="sub-step-number">${subNum}</span>
                            <span class="sub-step-instruction">${subTitle}</span>
                            ${subDur ? `<span style="font-size: 0.72rem; color: var(--text-muted); font-family: var(--font-mono);">${subDur}</span>` : ''}
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
                    <div class="stats-summary-grid" style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 0.75rem; margin-bottom: 0.75rem; background: #f8fafc; padding: 0.75rem 1rem; border-radius: 6px; border: 1px solid var(--border);">
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
                        <div class="llm-call-card expanded" style="border: 1px solid #cbd5e1; border-radius: 8px; margin-top: 0.6rem; overflow: hidden; background: #ffffff;">
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
                    <div class="llm-call-card expanded" style="border: 1px solid #cbd5e1; border-radius: 8px; margin-top: 0.6rem; overflow: hidden; background: #ffffff;">
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
            const stepActionsCount = Array.isArray(s.actions) ? s.actions.length : 0;
            const stepScreenshotsCount = screenshotSources.length;
            const stepLlmCallsCount = Array.isArray(calls) ? calls.length : 0;
            const countsSummaryHtml = `
                <div class="step-exec-stats" style="display: flex; align-items: center; gap: 0.45rem; flex-wrap: wrap; margin-top: 0.25rem;">
                    <span class="step-stat-chip" style="display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; color: var(--text-muted); background: #f8fafc; border: 1px solid var(--border); border-radius: 10px; padding: 0.15rem 0.5rem;"><span class="material-symbols-outlined" style="font-size: 0.8rem; color: #7e22ce;">terminal</span> ${stepActionsCount} action(s)</span>
                    <span class="step-stat-chip" style="display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; color: var(--text-muted); background: #f8fafc; border: 1px solid var(--border); border-radius: 10px; padding: 0.15rem 0.5rem;"><span class="material-symbols-outlined" style="font-size: 0.8rem; color: #2563eb;">image</span> ${stepScreenshotsCount} screenshot(s)</span>
                    <span class="step-stat-chip" style="display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.72rem; color: var(--text-muted); background: #f8fafc; border: 1px solid var(--border); border-radius: 10px; padding: 0.15rem 0.5rem;"><span class="material-symbols-outlined" style="font-size: 0.8rem; color: #a855f7;">smart_toy</span> ${stepLlmCallsCount} LLM call(s)</span>
                </div>
            `;

            secHtml += `
                <div class="step-item ${stepClass}" id="${stepId}" onclick="toggleStepActionInspector(this)">
                    <div class="step-header">
                        <div class="step-title-line" style="display: flex; align-items: center; gap: 0.4rem; flex-wrap: wrap;">
                            <span class="step-num-box">#${stepNum}</span>
                            ${isPassed ? '<span class="badge-status badge-pass" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;">SUCCESS</span>' :
                              (isFailed ? '<span class="badge-status badge-unknown-fail" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;">FAILED</span>' :
                              '<span class="badge-status badge-ignored" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;">SKIPPED</span>')}
                            <span class="context-badge level-${contextLevel.toLowerCase()}">${contextLevel}</span>
                            <span style="color: var(--text-main); font-weight: 600; margin-left: 0.2rem;">${title}</span>
                        </div>
                        ${countsSummaryHtml}
                        <div class="flex-gap-2">
                            <span style="font-size: 0.75rem; font-family: var(--font-mono); color: var(--text-muted);">${formattedDuration}</span>
                            <span class="material-symbols-outlined step-expand-chevron">chevron_right</span>
                        </div>
                    </div>

                    <div style="padding: 0 0.85rem 0.5rem;">
                        <div style="font-size: 0.75rem; color: var(--text-muted); font-family: var(--font-mono); margin-top: 0.2rem;">📄 ${s.sourceMeta || s.file +':L'+ s.line || 'Playbook.yaml:L' + stepNum}</div>
                    </div>

                    <div class="step-action-inspector" onclick="event.stopPropagation()">
                        <div class="inspector-card">
                            <div class="inspector-header">
                                <div style="display: flex; justify-content: space-between; align-items: center;">
                                    <span style="font-family: var(--font-mono); font-weight: 800; color: var(--status-fixed); font-size: 0.9rem;">#${stepNum}</span>
                                    <span class="badge-status ${isPassed ? 'badge-pass' : 'badge-unknown-fail'}" style="font-size: 0.7rem;">${isPassed ? 'PASSED' : 'FAILED'}</span>
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
                                    <div style="display: flex; flex-direction: column; gap: 0.4rem; font-size: 0.8rem; background: #f8fafc; padding: 0.5rem; border-radius: 6px; border: 1px solid var(--border); margin-bottom: 0.5rem;">
                                        <div><strong>Instruction:</strong> ${title}</div>
                                        <div><strong>Duration:</strong> ${formattedDuration}</div>
                                        <div><strong>Start Timestamp:</strong> ${formattedStart || 'N/A'}</div>
                                    </div>
                                    ${reasoningHtml}
                                    ${errorBoxHtml}
                                    ${screenshotHtml}
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
}

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

// Multiselect Dropdown Toggles & Click Outside
document.addEventListener('click', (e) => {
    const toggleBtn = e.target.closest('.js-multiselect-toggle');
    if (toggleBtn) {
        e.stopPropagation();
        const menuId = toggleBtn.getAttribute('data-menu-id');
        const menu = document.getElementById(menuId);
        if (menu) {
            const isOpen = menu.classList.contains('show') || menu.classList.contains('active');
            document.querySelectorAll('.multiselect-menu').forEach(m => m.classList.remove('show', 'active'));
            if (!isOpen) {
                menu.classList.add('show', 'active');
            }
        }
        return;
    }

    if (!e.target.closest('.multiselect-dropdown')) {
        document.querySelectorAll('.multiselect-menu').forEach(m => m.classList.remove('show', 'active'));
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
    const envMenu = document.getElementById('batchEnvMultiselectMenu');
    const locMenu = document.getElementById('batchLocalesMultiselectMenu');
    const browserMenu = document.getElementById('batchBrowsersMultiselectMenu');

    if (!envMenu) return;

    const selectedEnvs = Array.from(envMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value);
    const selectedLocs = locMenu ? Array.from(locMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value) : [];
    const selectedBrowsers = browserMenu ? Array.from(browserMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value) : [];

    document.querySelectorAll('#pageRunsList .batch-card').forEach(card => {
        const cardEnv = card.getAttribute('data-env') || '';
        const cardLocs = (card.getAttribute('data-locales') || '').split(',');
        const cardBrowsers = (card.getAttribute('data-browsers') || '').split(',');

        const matchEnv = selectedEnvs.length === 0 || selectedEnvs.some(e => cardEnv.includes(e));
        const matchLoc = selectedLocs.length === 0 || selectedLocs.some(l => cardLocs.includes(l));
        const matchBrowser = selectedBrowsers.length === 0 || selectedBrowsers.some(b => cardBrowsers.includes(b));

        card.style.display = (matchEnv && matchLoc && matchBrowser) ? '' : 'none';
    });

    document.querySelectorAll('#overviewRunsTableBody .run-directory-row').forEach(row => {
        const rowEnv = row.getAttribute('data-env') || '';
        const rowLocs = (row.getAttribute('data-locales') || '').split(',');
        const rowBrowsers = (row.getAttribute('data-browsers') || '').split(',');

        const matchEnv = selectedEnvs.length === 0 || selectedEnvs.some(e => rowEnv.includes(e));
        const matchLoc = selectedLocs.length === 0 || selectedLocs.some(l => rowLocs.includes(l));
        const matchBrowser = selectedBrowsers.length === 0 || selectedBrowsers.some(b => rowBrowsers.includes(b));

        row.style.display = (matchEnv && matchLoc && matchBrowser) ? '' : 'none';
    });
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

    const selectedLocs = locMenu ? Array.from(locMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.trim().toUpperCase()) : [];
    const selectedBrowsers = browserMenu ? Array.from(browserMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.trim().toUpperCase()) : [];

    document.querySelectorAll('#pageTestBase .tb-entry-row').forEach(row => {
        let loc = (row.getAttribute('data-location') || '').trim().toUpperCase();
        if (!loc) loc = 'UNKNOWN';

        let browser = (row.getAttribute('data-browser') || '').trim().toUpperCase();
        if (!browser) browser = 'UNKNOWN';

        const matchLoc = selectedLocs.length === 0 || selectedLocs.includes(loc);
        const matchBrowser = selectedBrowsers.length === 0 || selectedBrowsers.includes(browser);

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

document.body.addEventListener('htmx:afterSwap', (evt) => {
    const target = evt.detail && evt.detail.target;
    if (target && target.id === 'mainViewContainer') {
        closeTestSidePagePanel();
        lastHandledTargetExecutionId = null;
    }

    bindGlobalListeners();

    if (target && (target.id === 'sidePageStatusBadge' || target.id === 'runReportSubTabAllTests')) {
        recalculateRunReportMetrics();
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
    const isTestBase = document.getElementById('pageTestBase') !== null;
    const navRuns = document.getElementById('navRunsList');
    const navTest = document.getElementById('navTestBase');
    if (navRuns && navTest) {
        if (isTestBase) {
            navRuns.classList.remove('active');
            navTest.classList.add('active');
        } else {
            navRuns.classList.add('active');
            navTest.classList.remove('active');
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
        
        const headerTitle = document.getElementById('pageTitle');
        if (headerTitle) headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">trending_up</span> Batch History Overview`;
        
        const breadcrumb = document.getElementById('headerBreadcrumb');
        if (breadcrumb) breadcrumb.style.display = 'flex';
        
        const trail = document.getElementById('breadcrumbTrail');
        if (trail) trail.innerHTML = `<span>/</span> <span class="text-main" style="font-weight: 600;">${activeBatchName}</span>`;
    }

    // Automatically update header title & breadcrumb if Single Run Report view is loaded
    if (document.getElementById('runReportSubTabOverview')) {
        const runIdLabel = document.querySelector('.run-id-label')?.innerText || '#RUN_ID';
        const headerTitle = document.getElementById('pageTitle');
        if (headerTitle) headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">science</span> ${activeBatchName} <span style="font-size: 0.875rem; font-weight: 500; color: var(--text-muted); margin-left: 0.5rem;">(Run #${runIdLabel})</span>`;

        const breadcrumb = document.getElementById('headerBreadcrumb');
        if (breadcrumb) breadcrumb.style.display = 'flex';

        const trail = document.getElementById('breadcrumbTrail');
        if (trail) {
            trail.innerHTML = `<span>/</span> <a href="/batch-history?batchName=${encodeURIComponent(activeBatchName)}" style="cursor: pointer; text-decoration: none; font-weight: 600;">${activeBatchName}</a> <span>/</span> <span class="text-main" style="font-weight: 600;">Run #${runIdLabel}</span>`;
        }

        recalculateRunReportMetrics();

        const targetExecId = document.querySelector('[data-target-execution-id]')?.getAttribute('data-target-execution-id')
            || new URLSearchParams(window.location.search).get('executionId')
            || new URLSearchParams(window.location.search).get('testExecutionId');

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
                    targetRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    targetRow.click();
                }
            }, 100);
        }
    }

    // Automatically update header title if Overview of Runs directory is loaded
    if (document.getElementById('pageRunsList')) {
        const headerTitle = document.getElementById('pageTitle');
        if (headerTitle) headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">fact_check</span> Overview of Runs & Known Batches`;

        const breadcrumb = document.getElementById('headerBreadcrumb');
        if (breadcrumb) breadcrumb.style.display = 'none';
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

        rowEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        rowEl.classList.add('highlight-execution-row');
        setTimeout(() => {
            rowEl.classList.remove('highlight-execution-row');
        }, 3500);

        const testName = rowEl.getAttribute('data-test-name') || '';
        const dataSet = rowEl.getAttribute('data-dataset') || '';
        const statusKey = rowEl.getAttribute('data-status') || 'passed-clean';
        const issueTag = rowEl.getAttribute('data-bugs') || '';
        const rowId = rowEl.getAttribute('data-row-id') || rowEl.id;
        const runId = rowEl.getAttribute('data-run-id') || document.querySelector('.run-id-label')?.innerText.trim() || '#RUN_ID';

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
    const runId = row.getAttribute('data-run-id') || document.querySelector('.run-id-label')?.innerText.trim() || '#RUN_ID';

    const isTestBasePage = document.getElementById('pageTestBase') !== null || row.closest('#pageTestBase') !== null;

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
window.toggleTestClassGroup = toggleTestClassGroup;
window.toggleStepActionInspector = toggleStepActionInspector;
window.toggleSubStepInspector = toggleSubStepInspector;
window.closeSidePanelInspector = closeSidePanelInspector;
window.closeTestSidePagePanel = closeTestSidePagePanel;
window.openTestSidePagePanel = openTestSidePagePanel;
window.focusAndInspectExecution = focusAndInspectExecution;
window.redirectToTestBaseVariationHistory = redirectToTestBaseVariationHistory;
window.recalculateRunReportMetrics = recalculateRunReportMetrics;

// Client-side Execution Index Filtering and Sorting matching index.html
let currentExecutionStatusFilter = 'ALL';
let currentExecutionModeFilter = 'ALL';
let currentGlobalStatusBadgeFilter = 'ALL';

function filterByGlobalStatusBadge(statusKey, btnEl) {
    if (currentGlobalStatusBadgeFilter === statusKey) {
        currentGlobalStatusBadgeFilter = 'ALL';
    } else {
        currentGlobalStatusBadgeFilter = statusKey;
    }

    document.querySelectorAll('#wholeExecutionSummaryBadges .badge-status').forEach(b => b.classList.remove('active-filter-badge'));

    if (currentGlobalStatusBadgeFilter !== 'ALL') {
        document.querySelectorAll(`#wholeExecutionSummaryBadges .badge-status[data-status-key="${statusKey}"]`).forEach(b => b.classList.add('active-filter-badge'));
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
    applyFilter();
}

function populateBrowserAndLocaleFilters() {
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

        const classContainer = row.closest('.test-class-container');
        const classStatusFilter = classContainer ? (classContainer.getAttribute('data-active-status-filter') || 'ALL') : 'ALL';

        function matchesStatusKey(filterKey) {
            if (!filterKey || filterKey === 'ALL') return true;
            const key = filterKey.toUpperCase();
            if (key === 'PASSED') return rowStatus === 'PASSED' || rowStatusRaw === 'passed-clean' || rowStatusRaw === 'passed' || rowStatusRaw === 'succeeded';
            if (key === 'HEALED' || key === 'SUCCEEDED_FIXED') return rowStatus === 'HEALED' || rowStatus === 'SUCCEEDED_FIXED' || rowStatusRaw === 'succeeded-fixed' || rowStatusRaw === 'fixed' || rowStatusRaw === 'healed';
            if (key === 'FAILED_KNOWN' || key === 'KNOWN') return rowStatus === 'FAILED_KNOWN' || rowStatusRaw === 'failed-known' || rowStatusRaw === 'known';
            if (key === 'FAILED_UNKNOWN' || key === 'UNKNOWN') return rowStatus === 'FAILED_UNKNOWN' || rowStatusRaw === 'failed-unknown' || rowStatusRaw === 'failed' || rowStatusRaw === 'error';
            if (key === 'SKIPPED' || key === 'IGNORED') return rowStatus === 'SKIPPED' || rowStatusRaw === 'ignored' || rowStatusRaw === 'skipped';
            if (key === 'FAILED') return rowStatus.includes('FAIL') || rowStatusRaw.includes('failed');
            return rowStatus.includes(key);
        }

        const matchesGlobalBadge = matchesStatusKey(currentGlobalStatusBadgeFilter);
        const matchesAreaBadge = matchesStatusKey(areaStatusFilter);
        const matchesClassBadge = matchesStatusKey(classStatusFilter);
        const matchesLegacyStatus = matchesStatusKey(currentExecutionStatusFilter);

        const matchesMode = (currentExecutionModeFilter === 'ALL' || rowMode === currentExecutionModeFilter.toUpperCase() || rowMode.includes(currentExecutionModeFilter.toUpperCase()));
        const matchesBrowser = (selBrowser === 'ALL' || rowBrowser.toLowerCase() === selBrowser.toLowerCase());
        const matchesLocale = (selLocale === 'ALL' || rowLocale.toLowerCase() === selLocale.toLowerCase());
        const matchesSearch = (!search || rowSearch.indexOf(search) !== -1);

        if (matchesGlobalBadge && matchesAreaBadge && matchesClassBadge && matchesLegacyStatus && matchesMode && matchesBrowser && matchesLocale && matchesSearch) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });

    document.querySelectorAll('.test-class-container').forEach(classBox => {
        const visibleRows = classBox.querySelectorAll('tr.execution-row:not([style*="display: none"]), tr.test-row:not([style*="display: none"])');
        classBox.style.display = visibleRows.length > 0 ? '' : 'none';
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
        const maxWidth = Math.max(minWidth, window.innerWidth - 100);
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

window.loadTestBaseVariationHistory = loadTestBaseVariationHistory;
window.populateTbSideBatchMultiselect = populateTbSideBatchMultiselect;
window.filterTbVariationHistoryTable = filterTbVariationHistoryTable;
window.initPanelResizer = initPanelResizer;



