# [DEF-20261111-1037] Default Dashboard Refresh and Widget Reorder Collapse View to Runs List and Stale Active Run Context

- **Status:** `Resolved`
- **Opened:** 2026-11-11 10:37
- **Closed:** 2026-11-11 10:46
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Clicking "Refresh" (`/runs/refresh`) or "Resync Storage" while viewing custom dashboard pages or the root dashboard URL (`/`) causes all widgets to disappear, collapsing the main container into the raw `batchHistory` runs list table until a full browser reload (F5).
  2. Dragging/moving widgets on the default dashboard (root URL `/`) causes all widgets to disappear, replacing the dashboard view with the `batchHistory` runs list table.
  3. Clicking "Refresh" fails to update the "Active Run Context" card (`#sidebarContextCard`) in the left navigation panel, leaving stale run status and progress.
  4. Clicking "Refresh" unconditionally closes any open `#testSidePagePanel` drawer, even if the inspected execution row exists in the swapped DOM.
- **Root Cause:**
  1. In `AuraReportViewController.refreshAndResyncRuns(...)` and `deleteRun(...)`, the incoming `targetUrl` header (`HX-Current-URL` / `Referer`) was only checked for `/run-report`, `/batch-history`, `/test-base`, and `/dashboard-pages/`. When on the root URL `/`, it fell through to `batchOverview(...)`, which unconditionally returned `"fragments/batch-history :: batchHistory"` rather than resolving the primary configured dashboard page.
  2. In `AuraReportViewController`'s widget manipulation endpoints (`reorderWidgets`, `reorderContainerChildren`, `combineWidgets`, `separateContainer`, `separateChildWidget`, `renameWidget`, `getWidgetConfigModal`, `getWidgetExecutions`, `createOrUpdateWidget`, `deleteWidget`, `toggleWidgetDots`, `toggleWidgetFilled`, `resetWidgets`), the target page was extracted using `extractDashboardPageId(targetUrl)`. For the root URL `/`, `extractDashboardPageId` returned `null`, causing widget reordering and mutation calls to operate on the global fallback settings instead of the root custom dashboard page, and `refreshBatchView` fell through to `batchOverview`, returning the runs list.
  3. `#sidebarContextCard` was rendered inside `#mainSidebar` outside `#mainViewContainer` and lacked `hx-swap-oob="true"` and an out-of-band fragment inclusion in HTMX responses (`custom-dashboard-page.html`, `batch-history.html`, `test-base.html`, `run-report.html`), preventing HTMX from updating the card on view refreshes.
  4. In `report-manager.js`, the `htmx:afterSwap` event listener called `closeTestSidePagePanel()` whenever `data-target-execution-id` or query parameter `executionId` was absent, ignoring `currentActiveRowId` and discarding the active drawer state.
- **Detection Gap ("What did we miss?"):**
  Existing controller tests for `refreshAndResyncRuns`, `reorderWidgets`, and `reorderContainerChildren` only asserted URLs with explicit `/batch-history` or `/dashboard-pages/{id}` paths. No test simulated an HTMX interaction initiated from the root dashboard URL (`http://localhost:8080/`), allowing the unhandled root path fallback to escape detection.
- **Resolution:**
  1. Implemented `isRootUrl(url)` and `resolveTargetPageId(url)` in `AuraReportViewController` to resolve the root path to the primary configured custom dashboard page (`dashboardSettingService.getRootCustomPages()` or `getCustomPages()`).
  2. Updated `refreshAndResyncRuns`, `deleteRun`, `refreshBatchView`, and all 13 widget action endpoints in `AuraReportViewController` to resolve target page IDs using `resolveTargetPageId(targetUrl)` and return `rootIndex(hxRequest, null, model)` when target is root URL.
  3. Added `@RequestHeader(value = "HX-Request", required = false) final String hxRequest` to `AuraGlobalModelAdvice.addGlobalAttributes(...)` to expose `isHxRequest` to Thymeleaf views, with backward-compatible `addGlobalAttributes(Model)` overload.
  4. Added `th:fragment="sidebarContextCard"` and `th:attr="hx-swap-oob=${isHxRequest == true ? 'true' : null}"` to `#sidebarContextCard` in `sidebar.html`, and embedded out-of-band sync blocks in `custom-dashboard-page.html`, `batch-history.html`, `test-base.html`, and `run-report.html`.
  5. Updated `htmx:afterSwap` in `report-manager.js` to preserve `#testSidePagePanel` and re-render steps via `renderStepsForExecution(activeRow)` when `currentActiveRowId` exists in the newly swapped DOM.
- **Safety Net Added:**
  - `AuraReportViewControllerTest.testRefreshAndResyncRuns_CustomDashboardPage`: Verifies refreshing on `/dashboard-pages/page-custom-1` returns custom dashboard fragment.
  - `AuraReportViewControllerTest.testRefreshAndResyncRuns_RootUrl_WithConfiguredDashboard`: Verifies refreshing on `/` with configured root dashboard returns custom dashboard fragment.
  - `AuraReportViewControllerTest.testRefreshAndResyncRuns_RootUrl_WithoutConfiguredDashboard`: Verifies refreshing on `/` without configured dashboard falls back to `batchHistory`.
  - `AuraReportViewControllerTest.testReorderWidgets_RootUrl_InvokesSettingServiceWithResolvedPageId`: Verifies reordering widgets on `/` resolves root dashboard page ID and calls `reorderWidgets(pageId, ids)`.
  - `AuraReportViewControllerTest.testReorderContainerChildren_RootUrl_InvokesSettingServiceWithResolvedPageId`: Verifies reordering child widgets on `/` resolves root dashboard page ID and calls `reorderContainerChildren(pageId, containerId, ids)`.
  - `AuraGlobalModelAdviceTest.testAddGlobalAttributesHxRequestFlag`: Verifies `isHxRequest` model attribute handling.
  - `StaticResourceSyntaxTest`: Validates HTML templates and JavaScript syntax across 71 static asset tests.
