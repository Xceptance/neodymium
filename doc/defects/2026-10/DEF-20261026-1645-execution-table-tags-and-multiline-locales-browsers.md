# [DEF-20261026-1645] Execution Table Layout Breakage and Missing Tags

- **Status:** `Resolved`
- **Opened:** 2026-10-26 16:45
- **Closed:** 2026-10-26 16:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the "Recent Run Execution Directory" and batch runs execution tables, runs with multiple locales or browsers cause horizontal table blowout, pushing Pass Rate, Timestamp, and Action columns off-screen. Furthermore, the second column is titled "Batch Suite" and only shows the single batch name rather than all run tags.
- **Root Cause:**
  1. Locales (`localesCsv`) and browsers (`browsersCsv`) were rendered as raw unspaced comma-separated strings inside `<td>` without wrapping containers or width limits, preventing the browser layout engine from wrapping tokens across multiple lines.
  2. `AuraReportDataService.getGroupOverviewData` did not batch-fetch or attach `TestRunTagEntity` instances to `TestRunEntity`, and the overview template lacked a tags flex container to display key-value tag chips.
- **Detection Gap ("What did we miss?"):** Controller and template unit tests evaluated mock runs with 1 locale and 1 browser, never verifying rendering behavior or table layout bounds under multi-locale, multi-browser runs with composite tag sets.
- **Resolution:**
  1. Added `@Transient private List<TestRunTagEntity> tags` to `TestRunEntity` and batch-populated it via `tagRepository.findByRunIdIn(...)` in `AuraReportDataService.getGroupOverviewData(...)`.
  2. Renamed `<th>Batch Suite</th>` to `<th>Tags</th>` in `batch-overview.html` and rendered all run tags (with `Suite`/`batch` tags linking to `/batch-history`).
  3. Wrapped locale and browser tokens into `.table-cell-chips` containers with flex-wrap so long entries gracefully wrap into multiple lines without blowing out table width.
  4. Applied the multi-line chips layout to `batch-history.html` for consistency.
  5. Updated `report-manager.js` to ensure robust reading of `data-batch` from table rows.
- **Safety Net Added:** Added unit tests in `AuraReportViewControllerTest` verifying `<th>Tags</th>`, multi-tag rendering, and multi-line locale and browser chip wrapping.
