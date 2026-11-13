# [DEF-20261023-1449] Group By Toolbar Part Displayed When Viewing Grouped Test Overview

- **Status:** `Resolved`
- **Opened:** 2026-10-23 14:49
- **Closed:** 2026-10-23 14:51
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When tests are grouped (e.g. by Tag/Batch, Environment, Locale, or Browser) and a user clicks on one of the group cards to drill down into the overview for those grouped tests, the toolbar continues to render the "Group By:" label and filter pills alongside the "Back to Group Overview" button.
- **Root Cause:** In `aura-manager/src/main/resources/templates/fragments/batch-history.html`, the left child container `<div class="d-flex align-items-center gap-2 flex-wrap">` containing the `Group By:` label and `.btn-group-pills` was rendered unconditionally without checking whether the view represents a specific grouped subset (`backUrl != null and !backUrl.isEmpty()`).
- **Detection Gap ("What did we miss?"):** `AuraReportViewControllerTest.testBatchHistoryFragmentRendering()` only tested template rendering with `backUrl` unpopulated (the default/all-runs state), verifying that `Group By:` was present, but lacked test cases verifying template output when `backUrl` is populated (grouped overview drill-down state).
- **Resolution:** Added `th:if="${backUrl == null or backUrl.isEmpty()}"` to the `Group By:` wrapper `div` in `batch-history.html`. When tests are grouped (`backUrl != null and !backUrl.isEmpty()`), the "Group By:" pills are omitted from the DOM, leaving only the "Back to Group Overview" button.
- **Safety Net Added:** Added `testBatchHistoryGroupingToolbarWhenGrouped()` and updated `testBatchHistoryFragmentRendering()` in `com.xceptance.aura.report.controller.AuraReportViewControllerTest` to explicitly assert that `Group By:` is omitted and `Back to Group Overview` is present when `backUrl` is populated, and vice-versa when `backUrl` is null.
