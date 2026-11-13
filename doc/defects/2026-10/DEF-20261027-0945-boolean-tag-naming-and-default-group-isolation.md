# [DEF-20261027-0945] Boolean Tag Grouping Value Leakage and Untagged Run Exclusion in Default Groups

- **Status:** `Resolved`
- **Opened:** 2026-10-27 09:45
- **Closed:** 2026-10-27 09:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When grouping runs by a boolean tag (such as `Custom` with value `true`):
  1. The dashboard overview cards and history scope titles displayed raw literal boolean values (`true`) and fell back to generic `Default` instead of domain-friendly tag naming (`Custom` and `Not Custom`).
  2. The grouping representing untagged runs (`Default`) contained 0 tests ("No runs"), discarding all test executions, variations, pass/fail counts, and history trends.
- **Root Cause:**
  1. `AuraReportDataService.getGroupOverviewData(...)` evaluated dynamic tags by directly extracting raw string values from `getRunTagValues(r, dim)`. For runs with `Custom: true`, this produced the group key `"true"`, and for runs lacking the tag, it fell back to `"Default"` rather than evaluating boolean semantics (`Custom` vs `Not Custom`).
  2. In `AuraReportDataService.matchesRunCriteria(...)` and `filterScopedRun(...)`, tag filter evaluation strictly checked `runTagVals.stream().anyMatch(val -> selectedVals.stream().anyMatch(sv -> sv.equalsIgnoreCase(val)))`. Because untagged runs possess no tag records in `test_run_tag`, `getRunTagValues` returned an empty list. When filtering by `Default` (or `Not Custom`), `matchesRunCriteria` rejected all untagged runs, wiping out eligible runs and returning an empty summary.
  3. `AuraReportViewController.matchesAllDimensions(...)` and `batchHistory(...)` lacked boolean tag awareness, failing to map `Custom` to boolean true or `Not Custom` to untagged/false runs.
- **Detection Gap ("What did we miss?"):** Existing dynamic tag grouping tests (`testBatchOverviewDynamicTagGrouping` and `testBatchHistoryDynamicTagGroupingScoping`) only tested categorical string tags (`Release` with `v1.0` and `v2.0`) where all mock runs were explicitly tagged. No test verified boolean tags (`true`/`false`) or checked that untagged runs in `Default` retained their test executions and variation summaries.
- **Resolution:**
  1. Added `isBooleanTag(tagKey, runs)` in `AuraReportDataService` to dynamically detect boolean tags based on distinct tag values across the database and run entities.
  2. Added `matchesTagCriteria(run, tagKey, targetValues)` and `isExcludedByTagCriteria(...)` in `AuraReportDataService` to consistently match tagged and untagged runs across boolean tags (`<Tag>` vs `Not <Tag>`) and non-boolean default groups (`Default`).
  3. Updated `AuraReportDataService.getGroupOverviewData(...)` to group runs into `<Tag>` and `Not <Tag>` when grouping by a boolean tag.
  4. Updated `AuraReportDataService.matchesRunCriteria(...)` and `filterScopedRun(...)` to use `matchesTagCriteria`, preserving untagged runs within `Default` or `Not <Tag>` groups.
  5. Updated `AuraReportViewController.matchesAllDimensions(...)` and `batchHistory(...)` to resolve boolean tag groups (`Custom`, `Not Custom`) and untagged fallback runs.
  6. Updated `AuraReportViewController` candidate tag filter generation to expose `<Tag>` and `Not <Tag>` in the batch history multi-select dropdown for boolean tags.
- **Safety Net Added:** Added automated regression tests in `AuraReportViewControllerTest` and `AuraReportDataServiceTest`:
  - `testGroupOverviewBooleanTagNamingAndUntaggedRunInclusion`
  - `testBatchHistoryBooleanTagScoping`
  - `testGroupOverviewNonBooleanTagUntaggedRunInclusion`
