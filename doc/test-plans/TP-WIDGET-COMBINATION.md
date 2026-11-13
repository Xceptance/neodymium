# Test Plan: Dashboard Widget Combination & Containerization

**Document ID:** `TP-WIDGET-COMBINATION`  
**Target Subsystem:** Neodymium Aura Manager (`aura-manager`)  
**Feature:** Custom Dashboard Telemetry Widget Combination, Container Cards, and Client-Side Cross-Filtering  
**Status:** Approved / Active  
**Author:** AI-generated: Gemini 3.8 Flash (High)  
**Co-Author:** Xceptance GmbH 2026  
**License:** GNU Affero General Public License (AGPLv3)

---

## 1. Executive Summary & Objective

In Neodymium Aura Manager, telemetry widgets on any standard or group dashboard page can be united into a single **Container Widget Card** (`.widget-container-card`). Containerization enables engineering, QA, and executive stakeholders to correlate heterogeneous dimensions (such as test execution status, AI token spending, bug severity distribution, and browser environment matrix) side-by-side inside a single composite card.

This Test Plan defines the complete end-to-end testing matrix, validation criteria, equivalence boundaries, and regression safety nets for:
1. **Pre-requisite & Equivalence Validation:** Ensuring widgets only combine when sharing identical execution identity dimensions and contributing runs.
2. **Combination Topologies & Lifecycle Transitions:** Merging standalone widgets (1 + 1), appending to existing containers (N + 1), partial child separation (N > 2), container dissolution (N = 2), full container dissolution, and cascading deletion.
3. **Multi-Span & Heterogeneous Presentation Layouts:** Unified flex-row grid rendering, mixed metric cards, and auto full-span (`100%`) adaptation for trend graphs and Perlenkette bead chains.
4. **Client-Side In-Memory Cross-Filtering:** High-performance, zero-latency facet filtering across sibling child sections with facet exclusion rules and conjunction/disjunction logic.
5. **Traceability & Executions Inspection:** Aggregate contributing executions dialogs, status filtering, and run exclusions.
6. **Dual-Tier Persistence & Rehydration:** Consistent state synchronization across the H2 database (`report_setting` table) and `storage/report-settings.json`.

---

## 2. System Architecture & Lifecycle

```
                       ┌─────────────────────────────────────────────────────────┐
                       │               WIDGET COMBINATION LIFECYCLE              │
                       └─────────────────────────────────────────────────────────┘

   Standalone Widget A                        Standalone Widget B
   [Status: Ratio Bar]                        [Costs: Pie Chart]
   (Runs: R1..R10, Scope: Latest)             (Runs: R1..R10, Scope: Latest)
            │                                          │
            └────────────────────┬─────────────────────┘
                                 │
                                 ▼ Drag & Drop / POST /widgets/combine
                     ┌───────────────────────┐
                     │ Equivalence Validation│
                     │  - Same Page?         │
                     │  - Same Identities?   │
                     │  - Identical Runs?    │
                     └───────────┬───────────┘
                                 │
                 ┌───────────────┴───────────────┐
                 │ VALID                         │ INVALID
                 ▼                               ▼
     ┌───────────────────────────┐   ┌───────────────────────────┐
     │ .widget-container-card    │   │ Combine Warning Toast     │
     │ - Embedded JSON Dataset   │   │ "Cannot unite widgets:   │
     │ - Filter Strip + Clear All│   │  differing runs/identity" │
     │ - .widget-container-row   │   └───────────────────────────┘
     │   ├─ Section A (Status)   │
     │   └─ Section B (Costs)    │
     └─────────────┬─────────────┘
                   │
      ┌────────────┴────────────┬────────────────────────┐
      ▼ (Click Slice)           ▼ (Separate Child)       ▼ (Separate All)
 ┌──────────────────────┐  ┌───────────────────────┐  ┌───────────────────────┐
 │ In-Memory Cross-     │  │ Dissolve / Extract    │  │ Unpack All Children   │
 │ Filter Recalculation │  │ N > 2: Keeps Container│  │ Delete Container Card │
 │ Sibling Cards Filter │  │ N = 2: Dissolves Both │  │ Restore Standalone    │
 └──────────────────────┘  └───────────────────────┘  └───────────────────────┘
```

---

## 3. Test Matrix & Detailed Test Cases

### Suite 1: Equivalence & Pre-Requisite Guardrails

#### TP-COMB-01: Valid Combination of Widgets with Identical Contributing Runs
* **Classification:** Functional / Core Workflow
* **Level:** Service, Integration & UI
* **Pre-conditions:** A dashboard page exists with two standalone widgets: Widget A (`STATUS`, `PIE_CHART`) and Widget B (`COSTS`, `RATIO_BAR`). Both use the default execution identity (`browser`) and default run scope (`LATEST_TEST_STATUS`) with no run filter overrides.
* **Test Steps:**
  1. Trigger combine action via drag-and-drop or `POST /widgets/combine?targetWidgetId={A}&sourceWidgetId={B}`.
  2. Inspect response HTTP status and rendered DOM.
* **Expected Result:**
  - HTTP 200 OK returned.
  - A new `.widget-container-card` is rendered with ID `container-{uuid}` and title `"Status & Costs"`.
  - Both widgets render as `.widget-container-section` inside `.widget-container-unified-row`.
  - Sibling standalone cards A and B are removed from the root grid.

#### TP-COMB-02: Rejection on Different Point Scope (`LATEST_TEST_STATUS` vs `LAST_RUN`)
* **Classification:** Guardrail / Data Integrity
* **Level:** Service & Integration
* **Pre-conditions:** Widget A has point scope `LATEST_TEST_STATUS`. Widget B has point scope `LAST_RUN`.
* **Test Steps:**
  1. Send `POST /widgets/combine?targetWidgetId={A}&sourceWidgetId={B}`.
  2. Inspect response and model attributes.
* **Expected Result:**
  - Combination is rejected.
  - Server returns warning message: `"Cannot unite widgets: they are based on different contributing runs."`
  - Neither widget is altered or containerized; both remain standalone.

#### TP-COMB-03: Rejection on Custom Run Filter Limit Mismatch
* **Classification:** Guardrail / Data Integrity
* **Level:** Service & Integration
* **Pre-conditions:** Widget A has default filter (evaluates all available test runs). Widget B has a custom run filter override with `filterLimit = 5`.
* **Test Steps:**
  1. Attempt to combine Widget A and Widget B.
* **Expected Result:**
  - `WidgetAggregationService.areBasedOnSameContributingRuns()` returns `false`.
  - UI displays `.widget-combine-warning-toast`.
  - No container is created.

#### TP-COMB-04: Rejection on Execution Identity Dimension Mismatch
* **Classification:** Guardrail / Data Integrity
* **Level:** Service, Integration & UI
* **Pre-conditions:** Widget A has custom execution identity `[browser]`. Widget B has custom execution identity `[browser, locale]`.
* **Test Steps:**
  1. Attempt to combine Widget A and Widget B.
* **Expected Result:**
  - Server detects `sameIdentity == false`.
  - Warning returned: `"Cannot unite widgets: they have different execution identities."`
  - No container is created.

#### TP-COMB-05: Self-Combination Guardrail
* **Classification:** Boundary / Negative
* **Level:** Service & Integration
* **Pre-conditions:** Standalone Widget A exists on the dashboard.
* **Test Steps:**
  1. Send `POST /widgets/combine?targetWidgetId={A}&sourceWidgetId={A}`.
* **Expected Result:**
  - Combination is rejected as a no-op (`targetWidgetId.equals(sourceWidgetId)`).
  - No container is created; Widget A remains standalone.

#### TP-COMB-06: Cross-Dashboard Page Isolation Guardrail
* **Classification:** Security / Boundary
* **Level:** Integration
* **Pre-conditions:** Widget A exists on Page 1 (`/dashboard-pages/page-1`). Widget B exists on Page 2 (`/dashboard-pages/page-2`).
* **Test Steps:**
  1. Submit `POST /widgets/combine?targetWidgetId={A}&sourceWidgetId={B}` with header `HX-Current-URL: /dashboard-pages/page-1`.
* **Expected Result:**
  - Server resolves widgets for Page 1; cannot locate Widget B on Page 1.
  - Operation rejected without cross-page data pollution.

---

### Suite 2: Combination Topologies & Lifecycle Transitions

#### TP-COMB-07: Binary Merge (1 + 1 $\rightarrow$ Container 2)
* **Classification:** Functional / Core Workflow
* **Level:** Integration & UI
* **Pre-conditions:** Standalone Widget A (sortOrder 0) and Widget B (sortOrder 1) on dashboard.
* **Test Steps:**
  1. Drop Widget B onto Widget A.
* **Expected Result:**
  - A container is created at sortOrder 0.
  - Container header displays `"2 Linked Widgets"` badge.
  - Body contains 2 `.widget-container-section` items.

#### TP-COMB-08: N-ary Append (Container 2 + 1 $\rightarrow$ Container 3)
* **Classification:** Functional / Core Workflow
* **Level:** Integration & UI
* **Pre-conditions:** Container C (containing A and B) and standalone Widget D with matching run scope exist on dashboard.
* **Test Steps:**
  1. Drop Widget D onto Container C.
* **Expected Result:**
  - Container C retains its ID and sort order.
  - Child D is appended to Container C's children list.
  - Linked badge updates to `"3 Linked Widgets"`.

#### TP-COMB-09: Partial Separation ($N > 2$)
* **Classification:** Functional / State Machine
* **Level:** Integration & UI
* **Pre-conditions:** Container C contains 3 widgets (A, B, and D).
* **Test Steps:**
  1. Click `Separate from group` (`hx-post="/widgets/separate-child?containerId={C}&widgetId={B}"`) on Child B.
* **Expected Result:**
  - Child B is extracted and rendered as an independent top-level widget on the dashboard.
  - Container C remains intact with 2 children (A and D).
  - Linked badge on Container C updates to `"2 Linked Widgets"`.

#### TP-COMB-10: Clean Container Dissolution on 2-Item Child Separation ($N = 2$)
* **Classification:** Functional / State Machine
* **Level:** Integration & UI
* **Pre-conditions:** Container C contains exactly 2 widgets (A and D).
* **Test Steps:**
  1. Click `Separate from group` on Child A.
* **Expected Result:**
  - Container C is cleanly dissolved and removed.
  - Both Widget A and Widget D become standalone top-level widgets.
  - Zero `.widget-container-card` elements remain.

#### TP-COMB-11: Full Container Separation
* **Classification:** Functional / Core Workflow
* **Level:** Integration & UI
* **Pre-conditions:** Container C contains $N$ widgets ($N \ge 2$).
* **Test Steps:**
  1. Click `Separate into individual widgets` (`hx-post="/widgets/separate?containerId={C}"`) in container header.
* **Expected Result:**
  - Container C is dissolved immediately.
  - All $N$ child widgets are unpacked sequentially into the dashboard grid at Container C's position.
  - Each unpacked widget regains its standalone action toolbar.

#### TP-COMB-12: Cascading Container Deletion
* **Classification:** Functional / Data Deletion
* **Level:** Integration & UI
* **Pre-conditions:** Container C contains widgets A and B.
* **Test Steps:**
  1. Click `Remove container` (`hx-post="/widgets/delete?widgetId={C}"`) in container header.
* **Expected Result:**
  - Container C is deleted from H2 database and `storage/report-settings.json`.
  - All nested child widget configurations are permanently deleted.
  - Neither Container C nor its children appear on the dashboard.

---

### Suite 3: Layout, Presentation & Dimension Combinations

#### TP-COMB-13: Mixed Presentation Layout (Pie + Ratio + List)
* **Classification:** Visual / Layout
* **Level:** UI
* **Pre-conditions:** Container contains Widget A (`STATUS`, `PIE_CHART`), Widget B (`COSTS`, `RATIO_BAR`), and Widget C (`BUGS`, `LIST`).
* **Test Steps:**
  1. Render container on dashboard.
  2. Inspect flex layout classes and element dimensions.
* **Expected Result:**
  - Pie section receives `.widget-container-pie-section`.
  - Ratio bar section receives `.widget-container-ratios-section`.
  - List section receives `.widget-container-generic-section`.
  - All 3 sections render side-by-side with balanced heights.

#### TP-COMB-14: Automatic Full-Span Layout for Continuous Graphs
* **Classification:** Visual / Responsive Layout
* **Level:** UI
* **Pre-conditions:** Container contains Widget A (`STATUS`, `PIE_CHART`) and Widget B (`STATUS`, `TREND_GRAPH` or `PERLENKETTE`).
* **Test Steps:**
  1. Inspect CSS classes applied to Widget B's container section.
* **Expected Result:**
  - Widget B automatically receives `.widget-container-full-span` (spanning 100% width of the container row).
  - Graph axes and timeline beads render across the full width without horizontal clipping.

#### TP-COMB-15: Interactive Section Width Toggle (`btn-widget-section-span`)
* **Classification:** Interactive UI
* **Level:** UI
* **Pre-conditions:** Container contains half-width child sections.
* **Test Steps:**
  1. Click `btn-widget-section-span` ("Expand to full width") on Child A.
  2. Click `btn-widget-section-span` ("Contract to half width") again.
* **Expected Result:**
  - Step 1: Child A expands to 100% width (`.widget-container-full-span` added); icon toggles to `close_fullscreen`.
  - Step 2: Child A contracts back to standard width; icon toggles to `open_in_full`.

#### TP-COMB-16: Decoupled Inline Renaming
* **Classification:** Functional / Content Management
* **Level:** UI & Integration
* **Pre-conditions:** Container C has title `"Status & Costs"`. Child A has title `"Pass Rate"`.
* **Test Steps:**
  1. Open rename modal for Container C (`isContainer = true`) and set title to `"Executive Overview"`.
  2. Open rename modal for Child A (`isContainer = false`) and set title to `"Production Pass Rate"`.
* **Expected Result:**
  - Container header displays `"Executive Overview"`.
  - Child A section header displays `"Production Pass Rate"`.
  - Child B's title remains unchanged.
  - Persisted titles match in database.

---

### Suite 4: Client-Side Interactive Cross-Filtering

#### TP-COMB-17: Single Dimension Facet Filtering
* **Classification:** Interactive Telemetry Engine
* **Level:** UI
* **Pre-conditions:** Container contains Status Pie and Browser List.
* **Test Steps:**
  1. Click the "Failed" slice in Status Pie.
* **Expected Result:**
  - `.container-active-filter-strip` appears with a "Failed" filter chip.
  - Sibling Browser List recalculates immediately in-memory to show only tests with status "Failed".
  - Origin Status Pie retains its full breakdown and highlights the "Failed" slice.
  - All sibling cards receive `.has-active-cross-filter`.

#### TP-COMB-18: Multi-Dimension Conjunction (AND Filtering)
* **Classification:** Interactive Telemetry Engine
* **Level:** UI
* **Pre-conditions:** Container contains Status Pie, Browser List, and Locale List.
* **Test Steps:**
  1. Click "Failed" in Status Pie.
  2. Click "Chrome" in Browser List.
* **Expected Result:**
  - Active filter strip displays two chips: `[status: Failed]` and `[browser: Chrome]`.
  - Sibling Locale List recalculates to show only tests matching $(Status = Failed \land Browser = Chrome)$.
  - Filter strip displays `Clear all` button (`btn-clear-all-container-filters`).

#### TP-COMB-19: Disjunction on Same Dimension (OR Filtering)
* **Classification:** Interactive Telemetry Engine
* **Level:** UI
* **Pre-conditions:** Container contains Browser Tag List and Status Ratio Bar.
* **Test Steps:**
  1. Click "Chrome" pill in Browser List.
  2. Click "Firefox" pill in Browser List.
* **Expected Result:**
  - Browser dimension evaluates $(Browser = Chrome \lor Browser = Firefox)$.
  - Sibling Status Ratio Bar recalculates to aggregate tests run on either Chrome or Firefox.

#### TP-COMB-20: Filter Removal & "Clear all" Reset
* **Classification:** Interactive Telemetry Engine
* **Level:** UI
* **Pre-conditions:** Container has multiple active cross-filters.
* **Test Steps:**
  1. Click `Clear all` (`btn-clear-all-container-filters`) in the active filter strip.
* **Expected Result:**
  - All active filters are purged (`delete container._activeFilters`).
  - Active filter strip is hidden.
  - `.has-active-cross-filter` is removed from all child sections.
  - All child visualizations recalculate back to their original 100% baseline.

---

### Suite 5: Contributing Executions Modal & Run Exclusion

#### TP-COMB-21: Unified Contributing Executions Modal
* **Classification:** Traceability / Modal
* **Level:** Integration & UI
* **Pre-conditions:** Container C contains Status Ratio Bar and Costs Pie Chart.
* **Test Steps:**
  1. Click `View contributing test executions` in container header actions.
* **Expected Result:**
  - Modal opens (`/widgets/executions?widgetId={C}`).
  - Execution list displays the unified set of all executions contributing across all child widgets in Container C.
  - Status filter pills (`All`, `Passed`, `Fixed`, `Failed`, `Muted`) correctly filter the modal table.

#### TP-COMB-22: Run Exclusion Propagation on Container
* **Classification:** Traceability / Dynamic Metric Override
* **Level:** Integration & UI
* **Pre-conditions:** Executions modal is open for Container C.
* **Test Steps:**
  1. Click `Exclude run` on run `run_20260916_155945`.
* **Expected Result:**
  - Run ID is appended to `excludeRuns`.
  - Badge updates to `"1 run excluded"`.
  - All child widgets in Container C re-render excluding metrics from the excluded run.

---

### Suite 6: Dual-Tier Storage & Persistence Synchronization

#### TP-COMB-23: Dual-Tier Persistence Consistency
* **Classification:** Storage / Persistence
* **Level:** Unit & Integration
* **Pre-conditions:** Container C is created on a dashboard page.
* **Test Steps:**
  1. Query H2 database table `report_setting` for key `custom_pages`.
  2. Read filesystem file `storage/report-settings.json`.
* **Expected Result:**
  - Container C exists in both DB and JSON file with identical child arrays, IDs, and sort orders.
  - No serialization errors or property truncations occur.

#### TP-COMB-24: Cold Restart & Browser Rehydration
* **Classification:** Reliability / Rehydration
* **Level:** Integration & UI
* **Pre-conditions:** Container C is saved on dashboard.
* **Test Steps:**
  1. Restart Spring Boot application.
  2. Navigate directly to `/dashboard-pages/{pageId}` in a fresh browser session.
* **Expected Result:**
  - Container C rehydrates completely from storage.
  - Children render in exact sort order with identical presentation formats and titles.
  - Cross-filtering dataset (`.widget-container-dataset`) initializes cleanly without Javascript errors.

---

## 4. Test Data Requirements & Profiles

To ensure reliable, deterministic execution across all tests without SUT or locale hardcoding:
1. **Test Batches:** Fixture runs with synthetic variations across generic tags (`browser=Chrome,Firefox,Safari`, `locale=en_US,de_DE`, `environment=Default,Staging`).
2. **Failure Scenarios:** Run fixtures containing distinct defect types (fixed, known defect, unknown defect, ignored, AI healed).
3. **Execution Identities:** Multi-coordinate execution identities combining `(testClass, testMethod, dataSet)` with optional dimensions (`browser`, `locale`, `environment`).

---

## 5. Traceability & Implementation Matrix

| Test ID | Unit Test (`WidgetAggregationServiceTest`) | Integration Test (`CustomDashboardViewControllerIntegrationTest`) | UI Test (`AuraCustomDashboardUiTest`) |
| :--- | :---: | :---: | :---: |
| TP-COMB-01..06 | `testCombineWidgetsContributingRunsEquivalence` | `testCombineWidgetsEndpointEquivalenceRejection` | `testCombineWidgetsIntoContainerAndCrossFilter` |
| TP-COMB-07..12 | `testSeparateContainerLogic` | `testSeparateContainerEndpoint`, `testSeparateChildEndpoint` | `testContainerDissolutionLifecycle` |
| TP-COMB-13..16 | `testContainerMixedPresentationsDto` | `testContainerHtmlFragmentRendering` | `testContainerFullSpanAndSectionToggle` |
| TP-COMB-17..20 | - | - | `testContainerCrossFilterMultiDimensionConjunction` |
| TP-COMB-21..22 | `testContainerExecutionsAggregation` | `testWidgetExecutionsModalEndpointWithRunExclusion` | `testContainerExecutionsModalDialog` |
| TP-COMB-23..24 | `testContainerDualTierStorageSync` | `testContainerPersistenceAcrossSessions` | `testContainerRehydrationOnReload` |
