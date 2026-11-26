# [DEF-20261023-1645] Run Report Breadcrumbs and Tag/Batch Title Cleanup

- **Status:** `Resolved`
- **Opened:** 2026-10-23 16:45
- **Closed:** 2026-10-23 16:50
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On the run report overview page (`/run-report?runId=...`), the page header displayed the batch/tag name in the title (`Unknown (Run #<id>)`) and an intermediate batch link in the breadcrumbs (`/ Known Batches / Unknown / Run #<id>`), causing confusing unassigned or redundant tag/batch information to be shown for individual run reports.
- **Root Cause:**
  1. `AuraReportViewController.runReport()` prepended `effectiveBatchName` before `(Run #<id>)` when assigning the `pageTitle` model attribute.
  2. `report-manager.js` stamped `${activeBatchName}` before `(Run #${runIdLabel})` into `pageTitle.innerHTML`.
  3. `report-manager.js` injected an intermediate batch history link `<span>/</span> <a href="/batch-history?batchName=...">${activeBatchName}</a>` into `breadcrumbTrail.innerHTML`.
- **Detection Gap ("What did we miss?"):** Prior tests in `AuraReportViewControllerTest` only verified that `activeBatchName` was populated and asserted the composite page title without evaluating the single run report UX where runs should directly display their unique execution ID without unneeded batch/tag prefixes.
- **Resolution:**
  1. Updated `AuraReportViewController.runReport()` to format `pageTitle` cleanly as `"Run #" + runId`.
  2. Updated `report-manager.js` to set `headerTitle.innerHTML` directly to `<span class="material-symbols-outlined text-accent">science</span> Run #${runIdLabel}`.
  3. Updated `report-manager.js` to set `breadcrumbTrail.innerHTML` directly to `<span>/</span> <span class="text-main" style="font-weight: 600;">Run #${runIdLabel}</span>`, omitting the intermediate batch name segment.
  4. Updated `AuraReportViewControllerTest.testRunReportPreservesSubTab` to assert `pageTitle` is `"Run #RUN-100"`.
- **Safety Net Added:** Unit tests in `AuraReportViewControllerTest.testRunReportPreservesSubTab` verifying `pageTitle` is formatted as `"Run #" + runId` and static resource tests in `StaticResourceSyntaxTest`.
