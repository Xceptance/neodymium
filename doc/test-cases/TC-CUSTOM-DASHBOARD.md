# Test Cases: Aura Manager Custom Dashboard & Telemetry Widgets

- **Target Component:** `aura-manager`
- **Specification Reference:** [doc/ideas/2026-10/IDEA-20261029-1145-dashboard-widget-configurator.md](../ideas/2026-10/IDEA-20261029-1145-dashboard-widget-configurator.md)
- **Authors:** AI-generated: Gemini 3.8 Flash (High) / Xceptance GmbH 2026

---

## 1. Overview & Scope

This test catalog defines manual and automated test procedures for the Aura Manager Custom Dashboard and Telemetry Widget Engine. Testing covers:
1. **Dashboard Page Lifecycle & Hierarchy**: Standard dashboards, Group dashboards (by dimension), child sub-dashboards, conversion, prohibited dimension guardrails, and cascading deletion.
2. **5-Dimensional Widget Configuration**: Metric dimensions (`STATUS`, `COSTS`, `BUGS`, `TAG`, `AI_USAGE`, `TEST_VARIATIONS`, `FLAKINESS`), presentational formats (`PIE_CHART`, `RATIO_BAR`, `PERLENKETTE`, `TREND_GRAPH`, `TEXT`, `TEXT_WITH_SMALL_TREND`, `LIST`), time scopes, value formatting, and trend modes.
3. **Widget Containerization & Cross-Filtering**: Drag & drop combining, execution identity / contributing runs equivalence checks, separation, inline renaming, and client-side cross-filtering.
4. **Contributing Executions Telemetry**: Modal statistics, text filtering, status pill filters, per-run accordions, and interactive run exclusions.
5. **Persistence & Resilience**: H2 database and `storage/report-settings.json` bidirectional synchronization.

---

## 2. Manual Test Cases Catalog

### TC-MAN-01: Create Standard Custom Dashboard Page
- **Priority:** High
- **Preconditions:** Aura Manager running with test execution history.
- **Steps:**
  1. Open the Aura Manager Overview (`/`).
  2. Locate the "Custom Dashboards" section in the main sidebar.
  3. Click the `+` button ([`openCreatePageModal`](file:///home/olha/git/neodymium/aura-manager/src/main/resources/templates/fragments/sidebar.html#L54)).
  4. Ensure "Standard Dashboard" tab is selected.
  5. Enter Name: `Release Readiness 2026`.
  6. Leave Default Execution Identity as `Browser`.
  7. Set Run Limit to `10`.
  8. Click "Create Dashboard".
- **Expected Results:**
  - Modal closes without screen flicker.
  - Browser navigates to `/dashboard-pages/{pageId}`.
  - Page header displays `Release Readiness 2026`, an "Active" badge (`0 Active`), an "Identity: browser" chip, and a "Runs: Last 10" chip.
  - Empty page hero container displays: "This page has no widgets configured yet. Add your first widget to begin tracking telemetry on this page." with an "Add Widget" primary button.
  - Sidebar updates out-of-band to display `Release Readiness 2026` with active tab highlight.

---

### TC-MAN-02: Create Group Dashboard ("Group of Tests")
- **Priority:** High
- **Preconditions:** Test run history contains executions across multiple browsers (e.g. `Chrome`, `Firefox`, `Safari`).
- **Steps:**
  1. Click `+` next to "Custom Dashboards" in the sidebar.
  2. Select the "Group of Tests" tab.
  3. Select Grouping Dimension pill: `Browser`.
  4. Select Group Cards Status Evaluation: `Latest Test Status`.
  5. Enter Name: `Browser Matrix Overview`.
  6. Click "Create Dashboard".
- **Expected Results:**
  - Navigates to `/dashboard-pages/{pageId}`.
  - Page header shows `Group: browser` and `Scope: Latest Test Status` chips.
  - "Groupings: BROWSER" section renders with interactive cards for each browser present in history.
  - Each card displays a donut chart with Clean, Healed, and AI arcs, pass rate percentage, breakdown count list, and metadata tags.
  - Clicking any group card (e.g. `Chrome`) navigates to `/dashboard-pages/{subPageId}`.
  - The sub-page displays a "Back to Browser Matrix Overview" button and a chip `browser = Chrome`.
  - Sidebar renders a collapsible group parent with a chevron icon and indented sub-pages.

---

### TC-MAN-03: Add Telemetry Widgets across Presentational Types
- **Priority:** High
- **Preconditions:** Custom dashboard page open.
- **Steps:**
  1. Click "Add Widget" in the toolbar.
  2. **Widget 1 (Pie Chart):**
     - Metric: `STATUS`
     - Check: "Add information about healed / AI to the chart"
     - Presentation: `Pie Chart`
     - Time Scope: `Latest Test Status`
     - Width: `Half Width`
     - Click "Save Widget".
  3. Click "Add Widget" again:
  4. **Widget 2 (Trend Graph):**
     - Metric: `COSTS`
     - Presentation: `Trend Graph`
     - Trend Mode: `Daily`
     - Fill Style: `Filled (Stacked Area / Gradient)`
     - Check: "Show dots on chart"
     - Width: `Half Width`
     - Click "Save Widget".
  5. Click "Add Widget" again:
  6. **Widget 3 (String of Pearls / Perlenkette):**
     - Metric: `BUGS`
     - Presentation: `String of Pearls`
     - Polarity: `Higher is Red (Critical)`
     - Trend Mode: `By Runs`
     - Width: `Full Width`
     - Click "Save Widget".
- **Expected Results:**
  - All 3 widgets appear in `#dashboardWidgetsGrid`.
  - Empty page hero is replaced by the populated widgets section.
  - Widget 1 renders an SVG donut chart with sparkles for healed tests, center pass rate, and breakdown items.
  - Widget 2 renders an SVG area chart with green/blue gradient fills, chronological date labels on X-axis, dollar amounts on Y-axis, and interactive dots. Hovering dots displays popovers.
  - Widget 3 renders sequential milestone beads connected by a midline track. Beads display red/orange/green colors based on active bug counts.

---

### TC-MAN-04: Widget Custom Run Filters & Identity Override
- **Priority:** Medium
- **Preconditions:** Multiple test runs across environments and browsers.
- **Steps:**
  1. Open widget configurator modal.
  2. Select Metric `STATUS`, Presentation `RATIO_BAR`.
  3. Expand "Custom Execution Identity" -> select `Browser` and `Locale`.
  4. Expand "Custom Run Filters":
     - Run Limit: `5 runs`
     - Filter by Browser: Check `Chrome`
  5. Click "Save Widget".
- **Expected Results:**
  - Widget header displays "Identity: browser, locale" badge and "Filtered: Chrome, Last 5" chip strip.
  - Telemetry is calculated strictly from Chrome executions across the last 5 runs, unaffected by global dashboard filters.

---

### TC-MAN-05: Drag & Drop Combine into Container
- **Priority:** High
- **Preconditions:** Two widgets on the dashboard that evaluate the same contributing runs and share the same execution identity.
- **Steps:**
  1. Click and hold the drag handle (`drag_indicator`) on Widget A.
  2. Drag Widget A over Widget B.
  3. Observe the drop overlay ("Combine into Container - Must share contributing runs").
  4. Drop Widget A onto Widget B.
- **Expected Results:**
  - Page updates via HTMX without full refresh.
  - A unified `CONTAINER` card replaces the two individual cards.
  - Container header displays `2 Linked Widgets`, drag handle, inline rename button, executions modal button, separate button, and delete button.
  - Both widgets are displayed side-by-side inside `widget-container-unified-row`.

---

### TC-MAN-06: Combine Incompatibility Warning Alert
- **Priority:** Medium
- **Preconditions:** Two widgets with differing run filter scopes (e.g. Widget A has "Last 5 runs" and Widget B has "All runs").
- **Steps:**
  1. Drag Widget A onto Widget B.
  2. Release drag.
- **Expected Results:**
  - Combination is rejected.
  - Yellow warning alert appears at the top of `#dashboardWidgetsContainer`:
    `"Cannot unite widgets: they are based on different contributing runs."`
  - Neither widget is altered or corrupted.
  - Clicking `✕` dismisses the alert.

---

### TC-MAN-07: Container Interactive Cross-Filtering
- **Priority:** High
- **Preconditions:** Combined container containing a Status Pie Chart and a Browser Ratio Bar.
- **Steps:**
  1. On the Pie Chart, click the "Passed" slice.
  2. Observe the sibling Browser Ratio Bar inside the container.
  3. Observe the container header filter strip.
  4. Click the "Clear all" button in the container header.
- **Expected Results:**
  - Sibling Browser Ratio Bar dynamically recalibrates client-side to reflect only variations whose status is "Passed".
  - Active filter chip (`Status: Passed`) appears in the container header next to a "Clear all" button.
  - Clicking "Clear all" removes filter chips and instantly restores both widgets to their unfiltered container state.

---

### TC-MAN-08: Container Renaming & Separation
- **Priority:** Medium
- **Preconditions:** Combined container on page.
- **Steps:**
  1. Click the edit pencil next to the container title.
  2. In the rename modal, enter `Core Stability Container` -> submit.
  3. Click the edit pencil next to Child Widget 1 -> enter `Defect Distribution` -> submit.
  4. Click the "Separate into individual widgets" button (`call_split`) in the container header.
- **Expected Results:**
  - New titles are reflected in the UI immediately.
  - Clicking separate unpacks the container back into two standalone widget cards in their original grid order.
  - Both cards retain their updated titles and settings.

---

### TC-MAN-09: Contributing Test Executions Modal & Run Exclusion
- **Priority:** High
- **Preconditions:** Populated widget card on page.
- **Steps:**
  1. In the widget card header, click the list icon (`format_list_bulleted`).
  2. In the modal, verify overall pass rate, total executions count, contributing runs count, and LLM spending.
  3. In the search box, type a test method name (e.g. `testCheckout`).
  4. Click the `Flaky` status filter button.
  5. In the run accordion list, locate the latest run and click the exclude run button (`block` icon).
- **Expected Results:**
  - Modal opens cleanly (`#widgetExecutionsModal`).
  - Search input filters executions in real time.
  - Status pill isolates only flaky tests.
  - Excluding the run recalculates the modal pass rate, total count, and token spending without the excluded run.
  - An exclusion badge appears: `1 run excluded` with a `×` restore button.
  - Clicking `×` restores the run to the calculation.

---

### TC-MAN-10: Convert Standard Dashboard to Group Dashboard
- **Priority:** High
- **Preconditions:** Standard dashboard with 2 active widgets.
- **Steps:**
  1. Click the "Convert to Group" button in the page toolbar.
  2. In the convert modal, select Grouping Dimension: `Environment`.
  3. Leave Card Scope as `Latest Test Status`.
  4. Click "Convert to Group".
- **Expected Results:**
  - Page refreshes to a Group Dashboard.
  - Group Overview cards appear for each environment (`Default`, `Production`, etc.).
  - Existing widgets are preserved at the bottom under "Telemetry Widgets".
  - Sidebar navigation updates the item from a single page into a collapsible group with nested sub-dashboards.

---

### TC-MAN-11: Prohibited Group Dimension Guardrail
- **Priority:** Medium
- **Preconditions:** Sub-dashboard of a group dashboard already grouped by `Browser`.
- **Steps:**
  1. Navigate to the `Chrome` sub-dashboard.
  2. Click "Convert to Group".
  3. Examine the grouping pills in the modal.
  4. Attempt to submit with `groupBy=browser` via form or API.
- **Expected Results:**
  - In the UI, the `Browser` pill is disabled and grayed out.
  - If submitted programmatically, the server rejects the request with HTTP 400 (or redirect with `?error=prohibited_dimension`):
    `"Cannot group by 'browser': this dimension was already used in a previous grouping."`

---

### TC-MAN-12: Cascading Deletion & Storage Persistence
- **Priority:** High
- **Preconditions:** Group dashboard with multiple child sub-dashboards and widgets.
- **Steps:**
  1. On the group dashboard, click "Delete Page" (or click `✕` on the item in the sidebar).
  2. In the browser confirmation dialog, click OK.
  3. Inspect the sidebar and reload the home page (`/`).
  4. Inspect `storage/report-settings.json` on disk.
- **Expected Results:**
  - Browser redirects to `/`.
  - Parent dashboard and all child sub-dashboards are removed from the sidebar.
  - In `storage/report-settings.json`, entries under `custom_dashboard_pages` and associated widget IDs under `dashboard_widgets_config` are removed.
  - Main dashboard widgets and unrelated custom pages remain untouched.

---

## 3. Automated Test Traceability Matrix

| Feature Area | Manual Test ID | Automated Test Method | Test Class |
| :--- | :--- | :--- | :--- |
| Standard Page Creation & Empty State | TC-MAN-01 | `testCreateStandardDashboardPageViaHttp`, `testGetCustomDashboardPageEmptyStateRendering` | `CustomDashboardViewControllerIntegrationTest` |
| Group Dashboard & Sub-Dashboards | TC-MAN-02 | `testCreateGroupDashboardPageViaHttp`, `testGetGroupDashboardCardsRendering` | `CustomDashboardViewControllerIntegrationTest` |
| Widget Presentational Types | TC-MAN-03 | `testPerlenketteBugsDetailsBugCountInBuckets`, `testStatusTrendGraphMultiColorStackedLayers` | `WidgetAggregationServiceTest` |
| Widget Identity & Run Filters | TC-MAN-04 | `testWidgetAggregationWithCustomExecutionIdentityOverride`, `testWidgetAggregationWithWidgetRunFilters` | `WidgetAggregationServiceTest` |
| Drag & Drop Combine / Equivalence | TC-MAN-05, TC-MAN-06 | `testCombineWidgetsContributingRunsEquivalence`, `testCombineWidgets_WhenDifferentContributingRuns_SetsWarningMessage` | `WidgetAggregationServiceTest`, `AuraReportViewControllerTest` |
| Container Cross-Filtering & Unpack | TC-MAN-07, TC-MAN-08 | `testWidgetCombineAndSeparateEndpoints`, `testSeparateContainerRestoresWidgetsInPlace` | `CustomDashboardViewControllerIntegrationTest`, `ReportDashboardSettingServiceTest` |
| Executions Modal & Run Exclusion | TC-MAN-09 | `testWidgetExecutionsModalEndpointWithRunExclusion` | `CustomDashboardViewControllerIntegrationTest` |
| Convert to Group & Prohibited Dims | TC-MAN-10, TC-MAN-11 | `testConvertToGroupDashboardRejectsProhibitedDimension` | `CustomDashboardViewControllerIntegrationTest` |
| Cascading Deletion & Persistence | TC-MAN-12 | `testDeleteCustomDashboardPageCascades`, `testDeleteCustomPageCascadesToSubPages` | `CustomDashboardViewControllerIntegrationTest`, `ReportDashboardSettingServiceTest` |
| Browser E2E Lifecycle Journey | TC-MAN-01, 03, 12 | `testCreateCustomDashboardAddWidgetAndDeletePage` | `AuraCustomDashboardUiTest` |
