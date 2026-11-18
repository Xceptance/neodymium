# [DEF-20261118-1342] Sidebar Collapse CSS Specificity Regression and Missing Chat Launcher on Custom Dashboards

- **Status:** `Resolved`
- **Opened:** 2026-11-18 13:37
- **Closed:** 2026-11-18 13:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Clicking the left navigation collapse button (`#sidebarToggleBtn`) hides sidebar text labels and centers icons, but `#mainSidebar` remains `260px` wide instead of collapsing to `72px`.
  2. The `DASHBOARDS +` section header remains visible inside the collapsed sidebar, and the right half of `.sidebar-toggle-btn` (`right: -12px`) is covered by `#customDashboardPageToolbar`.
  3. The `Neodymium AI` chat launcher button is missing on Custom Dashboard pages (`#pageCustomDashboard`).
- **Root Cause:**
  1. Upstream commit `e645c53f3` on `origin/develop` changed the sidebar rule in `report-manager.css` from `aside { width: 260px; ... }` to `aside#mainSidebar, aside:not(.sidebar-palette-right) { width: 260px; ... }`. Because `aside#mainSidebar` has ID specificity `(1, 0, 1)`, it outranked `aside.collapsed { width: 72px; padding: 0; }` (`(0, 1, 1)`).
  2. In `sidebar.html`, the Dashboards group label uses `<div class="nav-group-label d-flex ...">`. Bootstrap's `.d-flex` (`display: flex !important;`) overrode `aside.collapsed .nav-group-label { display: none; }` in `report-manager.css`. Additionally, `aside#mainSidebar` had `z-index: 10`, placing its child `.sidebar-toggle-btn` behind `#customDashboardPageToolbar` (`z-index: 1010`).
  3. Upstream commit `8f216a36b` on `origin/develop` added `body:has(#pageCustomDashboard) #appHeader { display: none; }` and moved global header controls into `#customDashboardPageToolbar` in `custom-dashboard-page.html` without including `fragments/header :: chatLauncherBtn`.
- **Detection Gap ("What did we miss?"):** Static resource tests verified `aside.collapsed` and `header.html` independently, without asserting that collapsed sidebar selectors match `aside#mainSidebar` specificity or that `#customDashboardPageToolbar` mounts `chatLauncherBtn` when `#appHeader` is hidden.
- **Resolution:**
  - Updated `report-manager.css` to declare `aside#mainSidebar.collapsed, aside:not(.sidebar-palette-right).collapsed, aside.collapsed { width: 72px; padding: 0; }`, raised `aside#mainSidebar` `z-index` to `1020`, and added `!important` to `aside.collapsed .nav-group-label { display: none !important; }`.
  - Included `<th:block th:replace="~{fragments/header :: chatLauncherBtn}"></th:block>` inside `#customDashboardPageToolbar` in `custom-dashboard-page.html`.
  - Updated `openAuraChat()` and `closeAuraChat()` in `dashboard-runner.js` to toggle `.active` on all `.aura-chat-launcher` elements.
- **Safety Net Added:**
  - Extended `StaticResourceSyntaxTest.testAuraChatLauncherInTopHeaderAndTopRightOverlay` to assert `aside#mainSidebar.collapsed`, `aside.collapsed .nav-group-label { display: none !important; }`, and `fragments/header :: chatLauncherBtn` inside `custom-dashboard-page.html`.
