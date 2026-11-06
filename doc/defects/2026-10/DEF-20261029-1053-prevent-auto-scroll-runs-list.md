# [DEF-20261029-1053] Prevent Automatic Scrolling to Runs and Test Lists on Area Selection and Filtering

- **Status:** `Resolved`
- **Opened:** 2026-10-29 10:53
- **Closed:** 2026-10-29 11:00
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Report Manager views (Single Run Report and Batch History / Runs Overview), clicking on test areas (such as "Open Area in All Tests" or area ratio segments) or applying filters caused the viewport to automatically scroll or jump down to the test/runs list (`#runReportSubTabAllTests` or `#batchRunsHistoryTableBody`).
- **Root Cause:**
  1. `report-manager.js#openAllTestsAndExpandArea()` unconditionally invoked `targetEl.scrollIntoView({ behavior: 'smooth', block: 'start' });`, scrolling down to the test list when an area was selected.
  2. `focusAndInspectExecution()` and `pageTestBase` initialization in `report-manager.js` unconditionally invoked `.scrollIntoView({ behavior: 'smooth', block: 'center' });` when execution query parameters were present in the URL or upon HTMX swap.
  3. During HTMX partial swaps on `#mainViewContainer` (e.g., from filter dropdowns, limit presets, as-of-run milestones), scroll position was not preserved across swaps, allowing viewport resets and shifts.
  4. In `applyBatchDirectoryFilters()`, hiding variations in `#batchTestBaseVariationsPanel` caused rapid DOM height collapse without preserving the user's scroll anchor, and missing CSS `overflow-anchor: none` permitted browser scroll anchoring to pull the viewport down to `#batchRunsHistoryTableBody`.
- **Detection Gap ("What did we miss?"):** Existing test suites (`StaticResourceSyntaxTest.java` and `AuraReportViewControllerTest.java`) validated DOM generation, syntax, and filter data structures in headless unit environments without simulating user scroll viewport persistence during DOM element collapses and tab transitions.
- **Resolution:**
  1. Removed `targetEl.scrollIntoView(...)` from `openAllTestsAndExpandArea()` in `report-manager.js`.
  2. Removed automatic `scrollIntoView(...)` in `focusAndInspectExecution()` and `pageTestBase` execution row selection.
  3. Implemented scroll position preservation in `report-manager.js` across `applyBatchDirectoryFilters()` and HTMX swaps targeting `#mainViewContainer` so the user's viewport remains undisturbed.
  4. Configured CSS `overflow-anchor: none` on `.content-body`, `#batchRunsHistoryTableBody`, `#batchTestBaseVariationsPanel`, and `.area-trend-grid` to prevent browser scroll anchoring from pulling the viewport down to the runs table upon variation filtering.
  5. Changed `.area-trend-card` cursor to default to avoid misleading interactive behavior.
- **Safety Net Added:** Added dedicated regression tests in `StaticResourceSyntaxTest.java` verifying that `openAllTestsAndExpandArea` contains zero `scrollIntoView` invocations, `overflow-anchor: none` is configured on the runs table and variations panel, and scroll preservation across filter swaps is active.
