# [IDEA-20261105-1245] Dashboard Runs List Widget Support

- **Status:** `Implemented`
- **Proposed:** 2026-11-05 12:45
- **Resolved:** 2026-11-05 13:07
- **Component:** `aura-manager`
- **Category:** `Tooling & DX`
- **Author:** AI-generated: Gemini 3.8 Flash (High) / Xceptance GmbH 2026

## 1. Problem Statement & Motivation
Aura Manager historically provided a fixed "List of All Runs for this Batch" table at the bottom of the Run History page (`/batch-history`). With the introduction of custom dashboards (`/dashboard-pages/{pageId}`), users can assemble modular widgets to track status, costs, bugs, flakiness, and test variations across custom-scoped test suites and groupings.

However, custom dashboard pages lacked a widget representation for displaying the list of test execution runs belonging to that dashboard. Users navigating custom dashboards or group sub-dashboards (e.g. filtered by browser, locale, or custom tag) could not inspect individual run summaries, status bars, timestamps, and direct report links without navigating back to the global run history.

## 2. Proposed Architecture & Design
Introduce a new widget metric and presentation format: `WidgetMetric.RUNS` and `WidgetPresentation.RUNS`:
1. **Metric & Presentation Matrix:**
   - `WidgetMetric.RUNS`: Represents the collection of test runs evaluated by the current dashboard.
   - `WidgetPresentation.RUNS`: Presents test runs in a comprehensive 10-column interactive table (`aura-table`), identical to the run history table:
     - Run ID (#RUN_ID anchor linking to `/run-report?runId=...`)
     - Tags (Suite/Batch chips and custom tags)
     - Trigger source
     - Locales & Browsers chips
     - Total Tests count
     - Multi-segment Status Breakdown bar (Clean, Healed, AI-assisted counts for Passed, Succeeded-Fixed, Failed-Known, Failed-Unknown, Ignored)
     - Pass Rate badge (or in-progress running spinner)
     - Relative/absolute timestamp
     - Inspect & Delete actions
2. **Scoping & Custom Filtering:**
   - By default, the widget automatically inherits the candidate runs scoped to the current dashboard (including sub-dashboard criteria such as `browser=Chrome` or `environment=Staging`).
   - If custom widget run filters are configured (`config.hasOwnRunFilters()`), the widget evaluates its own criteria (e.g. `limit`, date range, candidate browsers/locales/tags) independently.
3. **Widget Configuration & UX:**
   - Surfaced under Section 1 ("1. Select Information / Variation Source") of `widget-configurator-modal.html`.
   - Strictly enforces full-width card layout (`width: "full"`, 100%). Quarter width (25%) and half width (50%) are prohibited and hidden in the configurator, and any width input is normalized to `"full"`.
   - Prohibits custom execution identity (`overrideExecutionIdentity = false`, dimensions cleared) because execution identity tracks variation coordinates, which are irrelevant for a list of runs.
   - Prohibits combining `RUNS` widgets into containers, as containers divide horizontal width among children.
   - Generates default title `"List of Runs"` with counts (`Showing X of Y Runs` or `X Runs`).
   - Collapsible panel support (`toggleRunsWidgetPanel`) allowing users to collapse/expand the runs list on the dashboard.

## 3. Expected Impact & Trade-offs
- **Benefits:** Seamless visibility of test runs directly within custom dashboards and grouped sub-dashboards; unified widget configuration workflow; complete parity with run history table capabilities; guaranteed optimal readability by enforcing full width and preventing layout distortion.
- **Risks & Complexity:** Large run tables could occupy significant vertical space on dashboards. Mitigated by defaulting to full-width, providing collapsible panel toggling, and supporting widget-level run limits.

## 4. Open Questions & Alternatives Considered
- **Alternatives Considered:** Embedding a hardcoded runs table beneath every custom dashboard. Rejected in favor of a dynamic widget because users should have total control over whether, where, and with what limits/filters the runs list appears alongside their visual telemetry.

## 5. Implementation & Verification Summary
- **Backend & DTOs:**
  - `WidgetMetric.RUNS` and `WidgetPresentation.RUNS` added with aliases (`RUNS`, `RUN_HISTORY`, `RUNS_LIST`, `LIST_OF_RUNS`).
  - `DashboardWidgetConfigDto`: Added default title generation (`"List of Runs"`), strictly enforced `width = "full"`, and forced `overrideExecutionIdentity = false` with empty dimensions.
  - `DashboardWidgetViewDto`: Added runs fields, `totalRunsCount`, `showingRunsCount`, `runAreasMap`, `isRuns()`, and enforced full width in `isFullWidth()` / `getWidthClass()`.
  - `WidgetAggregationService`: Implemented `buildRunsWidget(...)` with reverse-chronological sorting and widget-level run filter scoping; hooked into `aggregateWidgets` and `resolveContributingRunIds`.
  - `ReportDashboardSettingService` & `AuraReportViewController`: Prohibited combining `RUNS` widgets into containers; sanitized `width = "full"` and `overrideExecutionIdentity = false` on save; populated `runAreasMap` in `getCustomDashboardPage`; enhanced `deleteRun` to re-render custom dashboard pages directly upon run deletion.
- **Frontend & Styling:**
  - `widget-configurator-modal.html`: Added "Runs" option in Section 1 with history icon; hid custom execution identity section when `RUNS` is active; hid/disabled quarter (25%) and half (50%) radio buttons and displayed informational note that full width is required.
  - `dashboard-widgets.html`: Added card rendering for `WidgetPresentation.RUNS` with collapsible header, count badge, filter strip, `draggable="false"` (no container drop target), and modular `th:fragment="testRunsBody(w)"` rendering the 10-column interactive runs table.
  - `dashboard-styles.css`: Added `.widget-runs-card` and `.widget-runs-body` collapse animations and styling.
  - `report-manager.js`: Implemented `toggleRunsWidgetPanel(headerEl)` and registered globally.
- **Automated Verification:**
  - `DashboardWidgetConfigDtoTest`: Validated aliases, default title, Jackson JSON serialization/deserialization, widget cloning, and strict enforcement/normalization of full width and prohibition of execution identity.
  - `WidgetAggregationServiceTest`: Validated default aggregation, limit filtering, and contributing run resolution.
  - `CustomDashboardViewControllerIntegrationTest`: Validated saving and rendering of `RUNS` widget, automatic sanitization of non-full width and execution identity overrides, and rejection of combining `RUNS` into containers over HTTP MockMvc.
  - Full suite passed with zero failures.
