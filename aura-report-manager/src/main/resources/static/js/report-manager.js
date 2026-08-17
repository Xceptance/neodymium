/**
 * Aura Report Manager - Interactive Client Controller
 * Aligned with static design reference implementation
 */

// Global State Maps
let currentActiveRowId = null;
let currentExecutionIsFailed = true;
let activeTryMap = {}; 
let currentTestNameStr = 'CheckoutProcessTest';
let currentDataSetStr = '[Data Set 1: US CreditCard]';
let currentActiveRunId = '1049';
let activeBatchName = 'US Nightly Regression';

const executionCommentsMap = {
    'row-ds-1': 'Investigated by QA: Known ZIP verification API bug under tracking.'
};

const executionBugMap = {
    'row-ds-1': ['#BUG-4821', '#BUG-5102'],
    'row-ds-2': ['#BUG-4109'],
    'row-auth-2': ['#BUG-3012']
};

const executionStepsMap = {
    'row-ds-1': {
        retried: true,
        totalTries: 2,
        tries: {
            1: {
                engineOrigin: 'Java',
                error: "org.openqa.selenium.NoSuchElementException: Element #btn-checkout-v1 not found within 2000ms timeout on Try #1.\n  at com.xceptance.neodymium.tests.CheckoutProcessTest.clickCheckout(CheckoutProcessTest.java:112)",
                beforeSteps: [
                    { num: 1, title: 'Initialize Web Driver & Open Store Homepage', engine: 'Java', duration: '1.0s', timestamp: '14:22:08.010', passed: true, actions: [{ name: 'NAVIGATE', target: 'url="https://demo.xceptance.com/shop"' }] }
                ],
                coreSteps: [
                    { num: 2, title: 'Click Legacy Checkout Button #btn-checkout-v1', engine: 'Java', duration: '2.0s', timestamp: '14:22:09.010', passed: false, reasoning: 'Element #btn-checkout-v1 was absent in build #1049 DOM snapshot.', error: "NoSuchElementException: #btn-checkout-v1 not found.", actions: [{ name: 'CLICK', target: '#btn-checkout-v1' }] }
                ],
                afterSteps: []
            },
            2: {
                engineOrigin: 'Healed',
                error: "com.xceptance.neodymium.exceptions.ZipCodeInvalidException: Provided ZIP '90210' failed validation for US_WEST region.\n  at com.xceptance.neodymium.tests.CheckoutProcessTest.validateZip(CheckoutProcessTest.java:142)\n  at com.xceptance.neodymium.tests.CheckoutProcessTest.run(CheckoutProcessTest.java:88)",
                beforeSteps: [
                    { num: 1, title: 'Initialize Web Driver & Open Store Homepage', engine: 'Java', duration: '1.2s', timestamp: '14:22:10.102', passed: true, actions: [{ name: 'CLEAR_COOKIES', target: 'ClearCookiesAction.java' }, { name: 'NAVIGATE', target: 'url="https://demo.xceptance.com/shop"' }] },
                    { num: 2, title: 'Accept Cookie Consent Banner', engine: 'Java', duration: '450ms', timestamp: '14:22:11.302', passed: true, actions: [{ name: 'CLICK', target: '#btn-accept-all-cookies' }] }
                ],
                coreSteps: [
                    { 
                        num: 3, 
                        title: 'Type \'Neodymium\' in the Search input field', 
                        engine: 'AI', 
                        duration: '2.8s', 
                        tokens: '5204 in / 130 out', 
                        timestamp: '14:22:11.752', 
                        passed: true, 
                        reasoning: 'The instruction is to type \'Neodymium\' into the search input field. I identified the search input field using its unique and stable \'id\' attribute, \'searchInput\', and generated a TYPE action.', 
                        subSteps: [
                            { num: '3.1', title: 'Locate search input element #searchInput', engine: 'AI', duration: '1.1s', timestamp: '14:22:11.752', passed: true, reasoning: 'Scanned DOM tree using selector #searchInput. Found matching visible input field.', actions: [{ name: 'LOCATE', target: '#searchInput' }] },
                            { num: '3.2', title: 'Type "Neodymium" into input field & submit', engine: 'AI', duration: '1.7s', timestamp: '14:22:12.852', passed: true, reasoning: 'Dispatched keyboard TYPE sequence "Neodymium" followed by Key.ENTER trigger.', actions: [{ name: 'TYPE', target: '#searchInput = Neodymium' }, { name: 'KEYPRESS', target: 'Key.ENTER' }] }
                        ]
                    },
                    { 
                        num: 4, 
                        title: 'Proceed to Checkout & Enter Shipping Info', 
                        engine: 'Healed', 
                        duration: '3.4s', 
                        tokens: '4120 in / 95 out', 
                        timestamp: '14:22:14.552', 
                        passed: true, 
                        reasoning: 'Primary CSS selector #btn-checkout-v1 failed due to DOM refactoring in build #1049. Aura dHash & visual tree matcher identified target element #submit-checkout-btn with 98.6% visual confidence and automatically re-anchored locator.', 
                        subSteps: [
                            { num: '4.1', title: 'Self-heal checkout button locator', engine: 'Healed', duration: '1.8s', timestamp: '14:22:14.552', passed: true, reasoning: 'Re-anchored broken locator #btn-checkout-v1 to #submit-checkout-btn via visual dHash graph.', actions: [{ name: 'HEAL_LOCATOR', target: '#submit-checkout-btn' }] },
                            { num: '4.2', title: 'Fill first name & last name fields', engine: 'Java', duration: '1.6s', timestamp: '14:22:16.352', passed: true, reasoning: 'Entered parameter value "John" into input[name="firstName"].', actions: [{ name: 'TYPE', target: 'input[name="firstName"] = John' }] }
                        ]
                    },
                    { 
                        num: 5, 
                        title: 'Validate ZIP Code & Submit Order', 
                        engine: 'Playbook', 
                        duration: '400ms', 
                        timestamp: '14:22:17.952', 
                        passed: false, 
                        reasoning: 'Executed Playbook block US_WEST_Zip_Validation_Flow. Assertion failed when API rejected ZIP parameter.', 
                        error: "com.xceptance.neodymium.exceptions.ZipCodeInvalidException: Provided ZIP '90210' failed validation for US_WEST region.\n  at com.xceptance.neodymium.tests.CheckoutProcessTest.validateZip(CheckoutProcessTest.java:142)\n  at com.xceptance.neodymium.tests.CheckoutProcessTest.run(CheckoutProcessTest.java:88)", 
                        actions: [{ name: 'TYPE', target: 'input[name="zip"] = 90210' }, { name: 'ASSERT', target: 'TextEquals US_WEST' }] 
                    }
                ],
                afterSteps: [
                    { num: 6, title: 'Capture Attachments & Teardown Driver', engine: 'Java', duration: '300ms', timestamp: '14:22:18.352', passed: true, actions: [{ name: 'STORE', target: 'failure_screenshot_step3.png' }] }
                ]
            }
        }
    },
    'row-ds-2': {
        engineOrigin: 'AI Driven',
        beforeSteps: [
            { num: 1, title: 'Initialize Web Driver & Open Store Homepage', engine: 'Java', duration: '1.1s', timestamp: '11:15:00.050', passed: true, actions: [{ name: 'CLEAR_COOKIES', target: 'ClearCookiesAction.java' }, { name: 'NAVIGATE', target: 'url="https://demo.xceptance.com/shop"' }] },
            { num: 2, title: 'Accept Cookie Consent Banner', engine: 'Java', duration: '380ms', timestamp: '11:15:01.150', passed: true, actions: [{ name: 'CLICK', target: '#btn-accept-all-cookies' }] }
        ],
        coreSteps: [
            { num: 3, title: 'Select PayPal Express Payment Gateway', engine: 'AI', duration: '2.4s', tokens: '3810 in / 112 out', timestamp: '11:15:01.530', passed: true, reasoning: 'AI Agent identified PayPal visual emblem and interactive iframe button dynamically across multi-tenant locale variations.', actions: [{ name: 'CLICK', target: '#paypal-express-btn' }] },
            { num: 4, title: 'Authenticate PayPal Sandbox & Approve Transaction', engine: 'Playbook', duration: '1.9s', timestamp: '11:15:03.930', passed: true, reasoning: 'Playbook rule PayPal_Sandbox_Auth executed successfully.', actions: [{ name: 'TYPE', target: 'input[name="paypalUser"] = buyer@xceptance.com' }, { name: 'CLICK', target: '#paypal-submit-btn' }] }
        ],
        afterSteps: [
            { num: 5, title: 'Close Session & Teardown Driver', engine: 'Java', duration: '250ms', timestamp: '11:15:05.830', passed: true, actions: [{ name: 'CLEAR_COOKIES', target: 'ClearCookiesAction.java' }] }
        ]
    },
    'row-ds-3': {
        engineOrigin: 'Playbook',
        beforeSteps: [
            { num: 1, title: 'Initialize Web Driver & Open Store Homepage', engine: 'Java', duration: '1.0s', timestamp: '18:30:00.100', passed: true, actions: [{ name: 'NAVIGATE', target: 'url="https://demo.xceptance.com/shop"' }] }
        ],
        coreSteps: [
            { num: 2, title: 'Execute Express Checkout Fast Path', engine: 'Playbook', duration: '1.5s', timestamp: '18:30:01.100', passed: true, reasoning: 'Playbook block Express_Checkout_FastPath executed without errors.', actions: [{ name: 'CLICK', target: '#express-btn' }] },
            { num: 3, title: 'Verify Order Confirmation ID', engine: 'Playbook', duration: '350ms', timestamp: '18:30:02.600', passed: false, reasoning: 'Playbook assertion for Order Confirmation ID failed due to NullPointer in response payload.', error: "java.lang.NullPointerException: Cannot read field 'orderId' because 'response' is null\n  at com.xceptance.neodymium.tests.CheckoutProcessTest.verifyOrder(CheckoutProcessTest.java:198)", actions: [{ name: 'ASSERT', target: 'NotNull orderId' }] }
        ],
        afterSteps: [
            { num: 4, title: 'Capture Failure Screenshot & Teardown', engine: 'Java', duration: '280ms', timestamp: '18:30:02.950', passed: true, actions: [{ name: 'STORE', target: 'failure_screenshot.png' }] }
        ]
    }
};

const batchRunStatsRegistry = {
    'US Nightly Regression': {
        runIds: ['1045', '1046', '1047', '1048', '1049'],
        maxTests: 8,
        stats: {
            '1045': { x: 50, title: 'Run #1045 (3 days ago)', pass: 1, fixed: 1, known: 2, unknown: 1, ignored: 1, total: '6 Tests', rate: '16.7%' },
            '1046': { x: 230, title: 'Run #1046 (2 days ago)', pass: 3, fixed: 1, known: 1, unknown: 1, ignored: 1, total: '7 Tests', rate: '42.9%' },
            '1047': { x: 420, title: 'Run #1047 (Yesterday)', pass: 2, fixed: 2, known: 1, unknown: 1, ignored: 1, total: '7 Tests', rate: '28.6%' },
            '1048': { x: 610, title: 'Run #1048 (Today 11:15)', pass: 4, fixed: 2, known: 1, unknown: 0, ignored: 1, total: '8 Tests', rate: '50.0%' },
            '1049': { x: 800, title: 'Run #1049 (Latest)', pass: 3, fixed: 2, known: 1, unknown: 1, ignored: 1, total: '8 Tests', rate: '37.5%' }
        }
    },
    'EU Smoke Suite': {
        runIds: ['1042', '1043', '1044'],
        maxTests: 12,
        stats: {
            '1042': { x: 50, title: 'Run #1042 (2 days ago)', pass: 8, fixed: 1, known: 1, unknown: 0, ignored: 0, total: '10 Tests', rate: '80.0%' },
            '1043': { x: 420, title: 'Run #1043 (Yesterday)', pass: 9, fixed: 1, known: 1, unknown: 0, ignored: 0, total: '11 Tests', rate: '81.8%' },
            '1044': { x: 800, title: 'Run #1044 (Latest)', pass: 10, fixed: 1, known: 1, unknown: 0, ignored: 0, total: '12 Tests', rate: '83.3%' }
        }
    }
};

const activeClassFilters = {};
const activeAreaFilters = {};
let activeWholeExecutionFilter = null;

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
    activeBatchName = batchName || 'US Nightly Regression';
    
    // Show Batch History Overview view state inside batch-overview fragment
    const pageRunsList = document.getElementById('pageRunsList');
    const pageBatchHistory = document.getElementById('pageBatchHistoryOverview');
    
    if (pageRunsList && pageBatchHistory) {
        pageRunsList.classList.remove('active');
        pageBatchHistory.classList.add('active');
    } else {
        // If not currently loaded in DOM, fetch via HTMX
        htmx.ajax('GET', '/fragments/batch-overview', { target: '#mainViewContainer', swap: 'innerHTML' }).then(() => {
            document.getElementById('pageRunsList')?.classList.remove('active');
            document.getElementById('pageBatchHistoryOverview')?.classList.add('active');
        });
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

    const registry = batchRunStatsRegistry[activeBatchName] || batchRunStatsRegistry['US Nightly Regression'];
    const rect = svg.getBoundingClientRect();
    const mouseX = event.clientX - rect.left;
    const svgWidth = rect.width;
    
    const viewBoxX = (mouseX / svgWidth) * 800;

    const runIds = registry.runIds;
    let closestRun = runIds[runIds.length - 1];
    let minDist = 99999;

    runIds.forEach(id => {
        const dist = Math.abs(viewBoxX - registry.stats[id].x);
        if (dist < minDist) {
            minDist = dist;
            closestRun = id;
        }
    });

    const stat = registry.stats[closestRun];

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

    const runTitle = document.getElementById('ttRunTitle');
    if (runTitle) runTitle.innerText = stat.title;
    const passRate = document.getElementById('ttPassRate');
    if (passRate) passRate.innerText = `${stat.rate} Pass`;
    const totalVal = document.getElementById('ttTotalVal');
    if (totalVal) totalVal.innerText = stat.total;
    const passVal = document.getElementById('ttPassVal');
    if (passVal) passVal.innerText = stat.pass;
    const fixedVal = document.getElementById('ttFixedVal');
    if (fixedVal) fixedVal.innerText = stat.fixed;
    const knownVal = document.getElementById('ttKnownVal');
    if (knownVal) knownVal.innerText = stat.known;
    const unknownVal = document.getElementById('ttUnknownVal');
    if (unknownVal) unknownVal.innerText = stat.unknown;
    const ignoredVal = document.getElementById('ttIgnoredVal');
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
    const registry = batchRunStatsRegistry[activeBatchName] || batchRunStatsRegistry['US Nightly Regression'];
    const latestId = registry.runIds[registry.runIds.length - 1];
    const lastCircle = document.querySelector(`#trendPoint-${latestId} circle`);
    if (lastCircle) lastCircle.setAttribute('r', '7');
}

function renderDynamicTrendChart(targetBatchName) {
    const batchName = targetBatchName || activeBatchName || 'US Nightly Regression';
    const registry = batchRunStatsRegistry[batchName] || batchRunStatsRegistry['US Nightly Regression'];
    
    const svg = document.getElementById('mainTrendSvg');
    if (!svg) return;

    const runIds = registry.runIds;
    const maxTests = registry.maxTests;
    const svgHeight = 160;
    const baseScale = svgHeight / maxTests;

    const coords = runIds.map(id => {
        const s = registry.stats[id];
        const y0 = 190;
        const y1 = y0 - (s.ignored * baseScale);
        const y2 = y1 - (s.unknown * baseScale);
        const y3 = y2 - (s.known * baseScale);
        const y4 = y3 - (s.fixed * baseScale);
        const y5 = y4 - (s.pass * baseScale);
        return { x: s.x, y0, y1, y2, y3, y4, y5, id };
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

    coords.forEach(c => {
        const circle = document.querySelector(`#trendPoint-${c.id} circle`);
        if (circle) {
            circle.setAttribute('cx', c.x);
            circle.setAttribute('cy', c.y5);
        }
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

    document.querySelectorAll('#runReportSubTabOverview .metric-card').forEach(card => card.classList.remove('active-filter'));
    document.querySelectorAll('.js-filter-badge').forEach(badge => badge.classList.remove('active-filter'));

    if (statusKey) {
        const cardMap = {
            'passed-clean': 'metricValPassed',
            'succeeded-fixed': 'metricValSucceeded',
            'failed-known': 'metricValKnownFail',
            'failed-unknown': 'metricValUnknownFail',
            'ignored': 'metricValIgnored'
        };
        const cardValId = cardMap[statusKey];
        if (cardValId) {
            document.getElementById(cardValId)?.closest('.metric-card')?.classList.add('active-filter');
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

    const selectedLocs = locMenu ? Array.from(locMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.toUpperCase()) : [];
    const selectedBrowsers = browserMenu ? Array.from(browserMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value) : [];
    const selectedBugs = bugMenu ? Array.from(bugMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value) : [];
    const selectedFailures = failureMenu ? Array.from(failureMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value) : [];

    document.querySelectorAll('#runReportSubTabAllTests .test-row').forEach(row => {
        const loc = (row.getAttribute('data-location') || '').toUpperCase();
        const browser = row.getAttribute('data-browser') || '';
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
            matchStatus = row.getAttribute('data-status') === activeWholeExecutionFilter;
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
            badge.style.display = (badgeType === activeWholeExecutionFilter) ? '' : 'none';
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
    const inputVal = document.getElementById('bugTicketInput')?.value || '#BUG-4821';
    const parsedBugs = inputVal.split(/[, ]+/).map(s => s.trim()).filter(s => s.length > 0);
    
    if (currentActiveRowId) {
        if (parsedBugs.length > 0) {
            executionBugMap[currentActiveRowId] = parsedBugs;
        } else {
            delete executionBugMap[currentActiveRowId];
        }
        updateRowStatusAndMetrics(currentActiveRowId);
    }
    renderSidePanelStatusBugs();
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
    updateRowStatusAndMetrics(currentActiveRowId);
    renderSidePanelStatusBugs();
}

function updateRowStatusAndMetrics(rowId) {
    const row = document.getElementById(rowId);
    if (!row) return;

    const activeBugs = executionBugMap[rowId] || [];
    let currentStatus = row.getAttribute('data-status');

    let newStatus = currentStatus;
    if (currentStatus === 'failed-unknown' || currentStatus === 'failed-known') {
        newStatus = activeBugs.length > 0 ? 'failed-known' : 'failed-unknown';
        currentExecutionIsFailed = true;
    } else if (currentStatus === 'passed-clean' || currentStatus === 'succeeded-fixed') {
        newStatus = activeBugs.length > 0 ? 'succeeded-fixed' : 'passed-clean';
        currentExecutionIsFailed = false;
    }

    row.setAttribute('data-status', newStatus);
    row.setAttribute('data-bugs', activeBugs.length > 0 ? activeBugs.join(',') : 'NONE');

    // Update Status Cell (.col-status-badge)
    const statusTd = row.querySelector('.col-status-badge');
    if (statusTd) {
        if (newStatus === 'failed-known') {
            statusTd.innerHTML = `<span class="badge-status badge-known-fail" style="font-family: var(--font-family);"><span class="material-symbols-outlined">bug_report</span> Failed (Known Issue)</span>`;
        } else if (newStatus === 'failed-unknown') {
            statusTd.innerHTML = `<span class="badge-status badge-unknown-fail" style="font-family: var(--font-family);"><span class="material-symbols-outlined">warning</span> Failed (Without Issue Info)</span>`;
        } else if (newStatus === 'succeeded-fixed') {
            statusTd.innerHTML = `<span class="badge-status badge-fixed" style="font-family: var(--font-family);"><span class="material-symbols-outlined">bug_report</span> Succeeded (Known Bug)</span>`;
        } else if (newStatus === 'passed-clean') {
            statusTd.innerHTML = `<span class="badge-status badge-pass" style="font-family: var(--font-family);"><span class="material-symbols-outlined">check</span> Passed Clean</span>`;
        }
    }

    // Update Bug Info Cell (.col-bug-info)
    const bugTd = row.querySelector('.col-bug-info');
    if (bugTd) {
        if (activeBugs.length > 0) {
            const badgeStyle = currentExecutionIsFailed ? 'badge-known-fail' : 'badge-fixed';
            bugTd.innerHTML = activeBugs.map(b => `<span class="badge-status ${badgeStyle}"><span class="material-symbols-outlined">bug_report</span> ${b}</span>`).join(' ');
        } else if (newStatus === 'failed-unknown') {
            const failure = row.getAttribute('data-failure') || 'Exception';
            bugTd.innerHTML = `<span class="badge-status badge-unknown-fail"><span class="material-symbols-outlined">error</span> ${failure}</span>`;
        } else {
            bugTd.innerHTML = `<span style="color: var(--text-muted); font-size: 0.75rem;">None</span>`;
        }
    }

    recalculateRunReportMetrics();
}

function redirectToTestBaseVariationHistory() {
    closeTestSidePagePanel();
    if (window.htmx) {
        htmx.ajax('GET', '/test-base', { target: '#mainViewContainer', swap: 'innerHTML' }).then(() => {
            if (window.history && window.history.pushState) {
                window.history.pushState({}, '', '/test-base');
            }
            setTimeout(() => {
                const targetRow = (currentTestNameStr ? document.querySelector(`#pageTestBase .tb-entry-row[data-test-name="${currentTestNameStr}"]`) : null) || document.querySelector('#pageTestBase .tb-entry-row');
                if (targetRow) {
                    targetRow.click();
                }
            }, 100);
        });
    } else {
        window.location.href = '/test-base';
    }
}

function recalculateRunReportMetrics() {
    const rows = document.querySelectorAll('#runReportSubTabAllTests .test-row');
    if (rows.length === 0) return;

    let pass = 0, fixed = 0, known = 0, unknown = 0, ignored = 0;
    rows.forEach(r => {
        const st = r.getAttribute('data-status');
        if (st === 'passed-clean') pass++;
        else if (st === 'succeeded-fixed') fixed++;
        else if (st === 'failed-known') known++;
        else if (st === 'failed-unknown') unknown++;
        else if (st === 'ignored') ignored++;
    });

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

    const wholeBadges = document.getElementById('wholeExecutionSummaryBadges');
    if (wholeBadges) {
        wholeBadges.innerHTML = `
            <span class="badge-status badge-pass clickable-badge js-filter-badge" onclick="filterAllTestsByStatus('passed-clean')" data-status-key="passed-clean" title="Filter whole execution by Passed Clean (${pass})">${pass}</span>
            <span class="badge-status badge-fixed clickable-badge js-filter-badge" onclick="filterAllTestsByStatus('succeeded-fixed')" data-status-key="succeeded-fixed" title="Filter whole execution by Succeeded - Fixed Bug (${fixed})">${fixed}</span>
            <span class="badge-status badge-known-fail clickable-badge js-filter-badge" onclick="filterAllTestsByStatus('failed-known')" data-status-key="failed-known" title="Filter whole execution by Failed - Known Bug (${known})">${known}</span>
            <span class="badge-status badge-unknown-fail clickable-badge js-filter-badge" onclick="filterAllTestsByStatus('failed-unknown')" data-status-key="failed-unknown" title="Filter whole execution by Failed - Unknown Bug (${unknown})">${unknown}</span>
            <span class="badge-status badge-ignored clickable-badge js-filter-badge" onclick="filterAllTestsByStatus('ignored')" data-status-key="ignored" title="Filter whole execution by Ignored / Skipped (${ignored})">${ignored}</span>
        `;
    }

    // Recalculate Area Breakdown and Pie Charts on Overview tab
    document.querySelectorAll('#runReportSubTabAllTests .area-group').forEach(areaGroup => {
        let areaName = areaGroup.getAttribute('data-area') || '';
        if (areaName === 'Authentication') areaName = 'Auth';
        const areaRows = areaGroup.querySelectorAll('.test-row');

        let aPass = 0, aFixed = 0, aKnown = 0, aUnknown = 0, aIgnored = 0;
        areaRows.forEach(r => {
            const st = r.getAttribute('data-status');
            if (st === 'passed-clean') aPass++;
            else if (st === 'succeeded-fixed') aFixed++;
            else if (st === 'failed-known') aKnown++;
            else if (st === 'failed-unknown') aUnknown++;
            else if (st === 'ignored') aIgnored++;
        });

        const areaSummaryContainer = document.getElementById(`area${areaName}SummaryBadges`);
        if (areaSummaryContainer) {
            areaSummaryContainer.innerHTML = `
                <span class="badge-status badge-pass clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('passed-clean');" data-status-type="passed-clean" data-status-key="passed-clean">${aPass}</span>
                <span class="badge-status badge-fixed clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('succeeded-fixed');" data-status-type="succeeded-fixed" data-status-key="succeeded-fixed">${aFixed}</span>
                <span class="badge-status badge-known-fail clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('failed-known');" data-status-type="failed-known" data-status-key="failed-known">${aKnown}</span>
                <span class="badge-status badge-unknown-fail clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('failed-unknown');" data-status-type="failed-unknown" data-status-key="failed-unknown">${aUnknown}</span>
                <span class="badge-status badge-ignored clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('ignored');" data-status-type="ignored" data-status-key="ignored">${aIgnored}</span>
            `;
        }

        const piePass = document.getElementById(`pieValPassed${areaName}`);
        const pieFixed = document.getElementById(`pieValSucceeded${areaName}`);
        const pieKnown = document.getElementById(`pieValKnownFail${areaName}`);
        const pieUnknown = document.getElementById(`pieValUnknownFail${areaName}`);
        const pieIgnored = document.getElementById(`pieValIgnored${areaName}`);

        if (piePass) piePass.innerText = aPass;
        if (pieFixed) pieFixed.innerText = aFixed;
        if (pieKnown) pieKnown.innerText = aKnown;
        if (pieUnknown) pieUnknown.innerText = aUnknown;
        if (pieIgnored) pieIgnored.innerText = aIgnored;

        const total = aPass + aFixed + aKnown + aUnknown + aIgnored;
        const svgWrapper = document.getElementById(`pieChartSvgWrapper${areaName}`);
        if (svgWrapper && total > 0) {
            const pPass = (aPass / total) * 100;
            const pFixed = (aFixed / total) * 100;
            const pKnown = (aKnown / total) * 100;
            const pUnknown = (aUnknown / total) * 100;
            const pIgnored = (aIgnored / total) * 100;

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
    });

    // Recalculate Test Class Summary Badges
    document.querySelectorAll('#runReportSubTabAllTests .test-class-container').forEach(classBox => {
        const classRows = classBox.querySelectorAll('.test-row');
        let cPass = 0, cFixed = 0, cKnown = 0, cUnknown = 0, cIgnored = 0;
        classRows.forEach(r => {
            const st = r.getAttribute('data-status');
            if (st === 'passed-clean') cPass++;
            else if (st === 'succeeded-fixed') cFixed++;
            else if (st === 'failed-known') cKnown++;
            else if (st === 'failed-unknown') cUnknown++;
            else if (st === 'ignored') cIgnored++;
        });

        const classBadgeContainer = classBox.querySelector('.test-class-header .flex-gap-2');
        if (classBadgeContainer) {
            let html = '';
            if (cPass > 0) html += `<span class="badge-status badge-pass clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('passed-clean');" data-status-type="passed-clean" data-status-key="passed-clean">${cPass}</span>`;
            if (cFixed > 0) html += `<span class="badge-status badge-fixed clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('succeeded-fixed');" data-status-type="succeeded-fixed" data-status-key="succeeded-fixed">${cFixed}</span>`;
            if (cKnown > 0) html += `<span class="badge-status badge-known-fail clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('failed-known');" data-status-type="failed-known" data-status-key="failed-known">${cKnown}</span>`;
            if (cUnknown > 0) html += `<span class="badge-status badge-unknown-fail clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('failed-unknown');" data-status-type="failed-unknown" data-status-key="failed-unknown">${cUnknown}</span>`;
            if (cIgnored > 0) html += `<span class="badge-status badge-ignored clickable-badge" onclick="event.stopPropagation(); filterAllTestsByStatus('ignored');" data-status-type="ignored" data-status-key="ignored">${cIgnored}</span>`;
            classBadgeContainer.innerHTML = html;
        }
    });

    applyRunReportFilters();
}

function renderSidePanelStatusBugs() {
    const statusContainer = document.getElementById('sidePageStatusBadge');
    if (!statusContainer) return;

    const activeBugs = currentActiveRowId ? (executionBugMap[currentActiveRowId] || []) : [];

    if (activeBugs.length > 0) {
        const badgeStyle = currentExecutionIsFailed ? 'badge-known-fail' : 'badge-fixed';
        statusContainer.innerHTML = activeBugs.map(b => `
            <span class="badge-status ${badgeStyle}" style="font-family: var(--font-family);">
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

function openTestSidePagePanel(testName, dataSet, statusKey, issueTag, rowId, runId) {
    currentActiveRowId = rowId || null;
    currentTestNameStr = testName;
    currentDataSetStr = dataSet ? (dataSet.startsWith('[') ? dataSet : `[Data Set: ${dataSet}]`) : '';

    document.querySelectorAll('tr.clickable-row').forEach(r => {
        r.classList.remove('selected', 'active-selected-row');
        const marker = r.querySelector('.active-row-marker');
        if (marker) marker.remove();
    });

    const activeRow = rowId ? document.getElementById(rowId) : null;
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

    const activeRunId = runId || activeRow?.getAttribute('data-run-id') || document.querySelector('.run-id-label')?.innerText.trim() || '1049';
    const activeRowId = rowId || activeRow?.getAttribute('data-row-id') || activeRow?.id || 'row-ds-1';

    // Synchronize form inputs for bug ticket entry
    const bugRunInput = document.querySelector('#bugTicketFormBox input[name="runId"]');
    const bugRowInput = document.querySelector('#bugTicketFormBox input[name="rowId"]');
    if (bugRunInput) bugRunInput.value = activeRunId;
    if (bugRowInput) bugRowInput.value = activeRowId;

    // Dynamically load live bug badge section for the selected execution
    if (typeof htmx !== 'undefined' && document.getElementById('sidePageStatusBadge')) {
        htmx.ajax('GET', `/fragments/test-side-panel/bugs?runId=${encodeURIComponent(activeRunId)}&rowId=${encodeURIComponent(activeRowId)}`, {
            target: '#sidePageStatusBadge',
            swap: 'outerHTML'
        });
    }

    const nameEl = document.getElementById('sidePageTestName');
    const dsEl = document.getElementById('sidePageTestDataSet');
    if (nameEl) nameEl.innerText = testName;
    if (dsEl) dsEl.innerText = currentDataSetStr;

    const isTestBaseRow = activeRow ? (activeRow.closest('#pageTestBase') !== null) : (document.getElementById('pageTestBase') !== null);
    const singleRunControls = document.getElementById('sidePageSingleRunControls');
    const testBaseControls = document.getElementById('sidePageTestBaseVariationControls');

    if (isTestBaseRow && testBaseControls) {
        if (singleRunControls) singleRunControls.style.display = 'none';
        testBaseControls.style.display = 'flex';

        const loc = activeRow?.getAttribute('data-location') || 'US';
        const browser = activeRow?.getAttribute('data-browser') || 'Chrome';

        const locBadge = document.getElementById('tbSideLocaleBadge');
        const browserBadge = document.getElementById('tbSideBrowserBadge');
        if (locBadge) locBadge.innerHTML = `<span class="material-symbols-outlined">location_on</span> ${loc}`;
        if (browserBadge) browserBadge.innerHTML = `<span class="material-symbols-outlined text-accent">language</span> ${browser}`;
    } else {
        if (testBaseControls) testBaseControls.style.display = 'none';
        if (singleRunControls) singleRunControls.style.display = 'flex';

        currentExecutionIsFailed = statusKey ? statusKey.includes('fail') : true;
        renderSidePanelStatusBugs();
        renderStepsForExecution(activeRow);
    }

    const panel = document.getElementById('testSidePagePanel');
    const resizer = document.getElementById('allurePanelResizer');
    if (panel) panel.classList.add('active');
    if (resizer) resizer.classList.add('active');
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
    const localBindingsAttr = activeRow.getAttribute('data-local-bindings');
    const playbookFile = activeRow.getAttribute('data-playbook-file') || '';
    const failureText = activeRow.getAttribute('data-failure') || '';

    // Update Top Error Card
    const errorCard = document.getElementById('sidePageErrorDisplayCard');
    const errorTextEl = document.getElementById('sidePageErrorText');
    if (errorCard && errorTextEl) {
        if (failureText && failureText !== 'NONE' && failureText.trim().length > 0) {
            errorTextEl.innerText = failureText;
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
                    <td class="param-val">${activeRow.getAttribute('data-location') || 'US'}</td>
                </tr>
                <tr>
                    <td class="param-key">browser</td>
                    <td class="param-val">${activeRow.getAttribute('data-browser') || 'Chrome'}</td>
                </tr>
            `;
        }
        paramsTbody.innerHTML = paramsHtml;
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

            let actionsHtml = '';
            const actions = s.actions || [];
            if (actions.length > 0) {
                actionsHtml = `
                    <div class="actions-performed-box">
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

                    actionsHtml += `
                        <div class="action-item-pill-row">
                            <span class="action-type-pill">${actName}</span>
                            <span class="action-code-text">${actTarget}${actVal}${actDesc}</span>
                        </div>
                    `;
                });
                actionsHtml += `</div></div>`;
            }

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

            let reasoningHtml = '';
            if (s.reasoning) {
                reasoningHtml = `
                    <div class="reasoning-brain-row">
                        <div class="brain-icon-wrapper"><span class="material-symbols-outlined">psychology</span></div>
                        <div class="reasoning-text">${s.reasoning}</div>
                    </div>
                `;
            }

            let screenshotHtml = '';
            if (s.screenshot) {
                const src = s.screenshot.startsWith('data:') ? s.screenshot : `/storage/${s.screenshot}`;
                screenshotHtml = `
                    <div class="attachment-preview-box" style="margin-top: 0.4rem;">
                        <div class="flex-between">
                            <span style="font-size: 0.78rem; font-weight: 600;"><span class="material-symbols-outlined text-accent">image</span> Step Screenshot</span>
                            <span class="tag-chip">step_${stepNum}.png</span>
                        </div>
                        <div style="width: 100%; max-height: 240px; overflow: hidden; border-radius: 6px; border: 1px solid var(--border);">
                            <img src="${src}" alt="Step ${stepNum} Screenshot" style="width: 100%; object-fit: contain;">
                        </div>
                    </div>
                `;
            }

            secHtml += `
                <div class="step-item ${stepClass}" id="${stepId}" onclick="toggleStepActionInspector(this)">
                    <div class="step-header">
                        <div class="step-title-line">
                            <span class="step-num-box">#${stepNum}</span>
                            <span style="color: var(--text-main); font-weight: 600;">${title}</span>
                        </div>
                        <div class="flex-gap-2">
                            <span class="tag-chip" style="font-size: 0.68rem;">${s.source || s.engine || 'Java'}</span>
                            ${isPassed ? '<span class="badge-status badge-pass" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;"><span class="material-symbols-outlined" style="font-size: 0.75rem;">check</span> PASSED</span>' :
                              (isFailed ? '<span class="badge-status badge-unknown-fail" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;"><span class="material-symbols-outlined" style="font-size: 0.75rem;">close</span> FAILED</span>' :
                              '<span class="badge-status badge-ignored" style="padding: 0.15rem 0.4rem; font-size: 0.7rem;">IGNORED</span>')}
                            <span class="material-symbols-outlined step-expand-chevron">chevron_right</span>
                        </div>
                    </div>

                    <div class="step-action-inspector">
                        <div class="step-inspector-title">
                            <span class="material-symbols-outlined text-accent">info</span> STEP INSTRUCTION & DETAILS
                        </div>
                        <div class="step-inspector-headline">${title}</div>
                        ${actionsHtml}
                        ${errorBoxHtml}
                        ${reasoningHtml}
                        ${screenshotHtml}
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

    html += renderSection('BEFORE STEPS / SETUP', beforeSteps, 'before', 'play_arrow', 'var(--status-fixed)');
    html += renderSection('CORE EXECUTION STEPS', coreSteps, 'core', 'checklist', 'var(--status-pass)');
    html += renderSection('AFTER STEPS / TEARDOWN', afterSteps, 'after', 'stop', 'var(--text-muted)');

    stepListEl.innerHTML = html;
}

function toggleStepActionInspector(stepCardEl) {
    if (stepCardEl) {
        stepCardEl.classList.toggle('inspector-active');
    }
}

function closeTestSidePagePanel() {
    const panel = document.getElementById('testSidePagePanel');
    const resizer = document.getElementById('allurePanelResizer');
    if (panel) panel.classList.remove('active');
    if (resizer) resizer.classList.remove('active');

    document.querySelectorAll('tr.clickable-row').forEach(r => {
        r.classList.remove('selected', 'active-selected-row');
        const marker = r.querySelector('.active-row-marker');
        if (marker) marker.remove();
    });
}

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

    const selectedLocs = locMenu ? Array.from(locMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value.toUpperCase()) : [];
    const selectedBrowsers = browserMenu ? Array.from(browserMenu.querySelectorAll('.js-chk-option:checked')).map(o => o.value) : [];

    document.querySelectorAll('#pageTestBase .tb-entry-row').forEach(row => {
        const loc = (row.getAttribute('data-location') || '').toUpperCase();
        const browser = row.getAttribute('data-browser') || '';

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
document.addEventListener('DOMContentLoaded', () => {
    bindGlobalListeners();
});

document.body.addEventListener('htmx:afterSwap', (evt) => {
    const target = evt.detail && evt.detail.target;
    if (target && target.id === 'mainViewContainer') {
        closeTestSidePagePanel();
    }

    bindGlobalListeners();

    if (target && (target.id === 'sidePageStatusBadge' || target.id === 'runReportSubTabAllTests')) {
        recalculateRunReportMetrics();
    }
});

function bindGlobalListeners() {
    // Row click listeners for side panel inspection
    document.querySelectorAll('.js-open-side-panel').forEach(row => {
        row.removeEventListener('click', handleRowClick);
        row.addEventListener('click', handleRowClick);
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
        
        const headerTitle = document.getElementById('pageTitle');
        if (headerTitle) headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">trending_up</span> Batch History Overview`;
        
        const breadcrumb = document.getElementById('headerBreadcrumb');
        if (breadcrumb) breadcrumb.style.display = 'flex';
        
        const trail = document.getElementById('breadcrumbTrail');
        if (trail) trail.innerHTML = `<span>/</span> <span class="text-main" style="font-weight: 600;">${activeBatchName}</span>`;
    }

    // Automatically update header title & breadcrumb if Single Run Report view is loaded
    if (document.getElementById('runReportSubTabOverview')) {
        const runIdLabel = document.querySelector('.run-id-label')?.innerText || '1049';
        const headerTitle = document.getElementById('pageTitle');
        if (headerTitle) headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">science</span> ${activeBatchName} <span style="font-size: 0.875rem; font-weight: 500; color: var(--text-muted); margin-left: 0.5rem;">(Run #${runIdLabel})</span>`;

        const breadcrumb = document.getElementById('headerBreadcrumb');
        if (breadcrumb) breadcrumb.style.display = 'flex';

        const trail = document.getElementById('breadcrumbTrail');
        if (trail) {
            trail.innerHTML = `<span>/</span> <a hx-get="/batch-history?batchName=${encodeURIComponent(activeBatchName)}" hx-target="#mainViewContainer" hx-swap="innerHTML" hx-push-url="true" style="cursor: pointer; text-decoration: underline;">${activeBatchName}</a> <span>/</span> <span class="text-main" style="font-weight: 600;">Run #${runIdLabel}</span>`;
            htmx.process(trail);
        }

        recalculateRunReportMetrics();
    }

    // Automatically update header title if Overview of Runs directory is loaded
    if (document.getElementById('pageRunsList')) {
        const headerTitle = document.getElementById('pageTitle');
        if (headerTitle) headerTitle.innerHTML = `<span class="material-symbols-outlined text-accent">fact_check</span> Overview of Runs & Known Batches`;

        const breadcrumb = document.getElementById('headerBreadcrumb');
        if (breadcrumb) breadcrumb.style.display = 'none';
    }
}

function handleRowClick(e) {
    const row = e.currentTarget;
    const testName = row.getAttribute('data-test-name') || 'TestClass';
    const dataSet = row.getAttribute('data-dataset') || 'Data Set';
    const statusKey = row.getAttribute('data-status') || 'passed-clean';
    const issueTag = row.getAttribute('data-bugs') || '';
    const rowId = row.getAttribute('data-row-id') || row.id;
    const runId = row.getAttribute('data-run-id') || document.querySelector('.run-id-label')?.innerText.trim() || '1049';

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
window.redirectToTestBaseVariationHistory = redirectToTestBaseVariationHistory;
window.recalculateRunReportMetrics = recalculateRunReportMetrics;
