# [DEF-20261027-1317] Batch History Uniqueness Definition Misplacement & Non-Sticky Run Filters

- **Status:** `Resolved`
- **Opened:** 2026-10-27 13:17
- **Closed:** 2026-10-27 13:21
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Batch History Overview (`/batch-history`), the test execution uniqueness definition toolbar (`.uniqueness-definition-bar`) was nested inside `.last-run-header` within `.batch-latest-status-card`. This misled users into thinking uniqueness was scoped solely to the Latest Test Status pie chart, while also cluttering the chart header. Additionally, scrolling down to inspect trend graphs, the runs table, or test base variations caused both run filters and uniqueness controls to scroll out of view.
- **Root Cause:** In `batch-history.html`, `.uniqueness-definition-bar` was placed directly inside `.last-run-header` in `.batch-latest-status-card`. Neither `#batchRunScopeToolbar` nor the uniqueness controls were configured with `position: sticky`, leaving them unpinned within the main scroll container (`.content-body`).
- **Detection Gap ("What did we miss?"):** Previous visual unit tests in `AuraReportViewControllerTest` only asserted that `.uniqueness-definition-bar` was present in the rendered HTML string without verifying that it resided outside `.batch-latest-status-card` and within the primary batch toolbar container.
- **Resolution:**
  1. Extracted `.uniqueness-definition-bar` out of `.last-run-header` in `batch-history.html`, streamlining `.batch-latest-status-card` to display only the title, count badge, and timestamp.
  2. Integrated `.uniqueness-definition-bar` into `#batchRunScopeToolbar` as Row 2, demarcated with a clean dashed border line (`border-top: 1px dashed var(--border)`).
  3. Created `.batch-sticky-filters-container` with `position: sticky; top: 0; z-index: 1020;` wrapping `#batchRunScopeToolbar` and `.active-run-filters-bar` in `report-manager.css` and `batch-history.html`.
  4. Bumped `report-manager.css` asset version query parameter in `index.html` to `?v=1.1.13` for client browser cache busting.
- **Safety Net Added:** Added `testBatchStickyFiltersContainerAndUniquenessPlacement` in `StaticResourceSyntaxTest.java` and enhanced `testUniqueTagsNoDuplicateInBatchHistory` in `AuraReportViewControllerTest.java` asserting container stickiness, DOM ordering, and isolation from `batch-latest-status-card`.
