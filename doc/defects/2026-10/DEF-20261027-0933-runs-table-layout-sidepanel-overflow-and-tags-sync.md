# [DEF-20261027-0933] Runs Table Layout Overflow, Side Panel Collision, and Tags Column Desync

- **Status:** `Resolved`
- **Opened:** 2026-10-27 09:33
- **Closed:** 2026-10-27 09:40
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When opening the test side panel, the runs table overflows horizontally and does not fit into the viewport; long tag chips (e.g. branch names) spill out of the Tags cell and overlap the Trigger column; the Locales column is compressed to ~50px width causing badges to stack across 7 vertical lines; the Action column delete button is pushed off-screen and not displayed. In addition, batch history and batch-specific execution tables remained in the legacy format without the "Tags" column, and the "Pass Rate" column header was squished into multiple lines (`P \n R`) due to horizontal width starvation.
- **Root Cause:**
  1. `.table-cell-chips .tag-chip` declared `white-space: nowrap` without `max-width: 100%`, `overflow: hidden`, or `text-overflow: ellipsis`, allowing long branch tags to physically overflow the 250px cell width and overlap adjacent columns.
  2. `.cell-locales` lacked `min-width`, allowing browser table layout algorithms to squeeze it to the width of a single 2-letter badge (~26px) under horizontal pressure.
  3. Action column was not sticky and table padding was not compacted when the test side panel was active, causing the rightmost delete button to be clipped when the viewport width narrowed.
  4. `batch-history.html` and `batchSpecificRunsTableBody` templates omitted `<th>Tags</th>`, and `AuraReportViewController.batchHistory` did not batch-populate `run.tags`.
  5. A redundant standalone `Environment` column consumed ~85px of horizontal table budget despite the environment already being represented inside the `Tags` column, squeezing `Pass Rate` into wrapped text.
- **Detection Gap ("What did we miss?"):** Previous tests only validated `batch-overview.html` for `<th>Tags</th>` under full-width viewports, without asserting layout bounds, side panel active state, `batch-history.html`, or pass rate header text wrapping.
- **Resolution:**
  1. Added ellipsis truncation, `max-width: 100%`, and hover tooltips for long tag chips in `report-manager.css`.
  2. Enforced `min-width: 130px` on `.cell-locales` and `min-width: 140px` on `.cell-browsers`.
  3. Made `table.aura-table th.col-action` and `td.col-action` sticky (`right: 0`) and compacted table padding and status breakdown width under `body:has(#testSidePagePanel.active)`.
  4. Added the `Tags` column to `batch-history.html` and `batchSpecificRunsTableBody`.
  5. Populated `r.setTags(...)` in `AuraReportViewController.batchHistory(...)` using `tagRepository.findByRunIdIn(...)`, with automated fallback injection of `Environment: <val>` when not explicitly tagged.
  6. Removed the redundant standalone `Environment` column across all runs tables (`batch-overview.html` and `batch-history.html`), reallocating the horizontal space to `Pass Rate`.
  7. Added `.col-pass-rate` class enforcing `white-space: nowrap; min-width: 90px; text-align: center;` (and `min-width: 80px` when the side panel is active).
  8. Updated `report-manager.js` to preserve the unified 10-column layout in cloned batch-specific rows.
- **Safety Net Added:** Added template assertions in `AuraReportViewControllerTest` verifying `Tags`, `col-pass-rate`, and the absence of standalone `Environment` column; added unit test `testPopulateRunTagsInjectsEnvironmentTag`; and added CSS verification in `StaticResourceSyntaxTest`.
