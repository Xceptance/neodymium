# [DEF-20261023-1645] Header Title Desynchronization for Dashboard Root and Grouped Overview

- **Status:** `Resolved`
- **Opened:** 2026-10-23 16:45
- **Closed:** 2026-10-23 16:53
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When visiting `http://localhost:8080/?groupBy=` (or grouped overview), the header displayed legacy or long titles ("Overview of Runs & Known Batches" or "Overview of Runs - Grouped by ...") instead of "Group overview". When visiting `http://localhost:8080/`, the header displayed "Batch History Overview" or "Run History - All Runs" instead of "Runs overview".
- **Root Cause:** In `AuraReportViewController.java`, `batchOverview` passed `"Overview of Runs - Grouped by " + groupLabel` while `batchHistory` passed `"Run History - All Runs"`. Additionally, in `fragments/header.html` the fallback string was hardcoded to `"Overview of Runs & Known Batches"`, and in `report-manager.js`, `bindGlobalListeners()` unconditionally clobbered `#pageTitle` with `"Overview of Runs & Known Batches"` (for `pageRunsList`) and `"Batch History Overview"` (for `mainTrendSvg`), overriding server-rendered titles on initial loads and HTMX partial swaps.
- **Detection Gap ("What did we miss?"):** Prior controller unit tests explicitly asserted the legacy strings (`"Overview of Runs - Grouped by ..."` and `"Run History - All Runs"`), and client-side DOM mutation tests in `StaticResourceSyntaxTest` only validated JS syntax without verifying header synchronization against route semantics.
- **Resolution:**
  1. Updated `AuraReportViewController.batchOverview()` to set `pageTitle` to `"Group overview"`.
  2. Updated `AuraReportViewController.batchHistory()` to set `pageTitle` to `"Runs overview"` when viewing all runs (`effectiveGroupType.equals("all")`).
  3. Updated `fragments/header.html` fallback to `"Runs overview"` and bound the appropriate Material Symbols icons (`grid_view` for Group overview and `format_list_bulleted` for Runs overview).
  4. Updated `report-manager.js` to set `#pageTitle` to `"Group overview"` (with `grid_view` icon) when `pageRunsList` is active, and `"Runs overview"` (with `format_list_bulleted` icon) when viewing all runs.
- **Safety Net Added:** Updated assertions in `com.xceptance.aura.report.controller.AuraReportViewControllerTest` (`testBatchOverviewGroupingRoutes()`, `testBatchOverviewMultiDimensionalGrouping()`) to assert `pageTitle` equals `"Group overview"` when grouped and `"Runs overview"` when viewing all runs.
