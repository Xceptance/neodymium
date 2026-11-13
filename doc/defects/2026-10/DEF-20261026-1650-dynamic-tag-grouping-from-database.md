# [DEF-20261026-1650] Hardcoded Batch Tag in Grouping Toolbar and Missing Dynamic Tag Grouping from Database

- **Status:** `Resolved`
- **Opened:** 2026-10-26 16:25
- **Closed:** 2026-10-26 16:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the dashboard overview (`batch-overview.html`) and history (`batch-history.html`), grouping options ("Group By:") always displayed a static "Tag (Batch)" pill even when no "Batch" tag existed in the database (`test_run_tag` table), while actual tags present in the database (such as "Release", "Team", etc.) were never discovered or offered as grouping dimensions. Furthermore, selecting non-built-in tags in the `groupBy` query parameter fell back to batch name grouping or was ignored by the dimension parser.
- **Root Cause:**
  1. In HTML templates (`batch-overview.html` and `batch-history.html`), the grouping toolbar hardcoded `<a ... th:href="@{/(groupBy='batch',...)}"><span>Tag (Batch)</span></a>` instead of iterating over dynamically discovered tag keys.
  2. `AuraReportViewController.parseGroupDimensions(...)` and `AuraReportDataService.getGroupOverviewData(...)` maintained a rigid whitelist of known dimensions (`batch`, `env`, `environment`, `locale`, `browser`, `area`, `none`, `all`). Any dynamic tag key (e.g. `Release`, `Team`) not in this static list was discarded.
  3. `AuraReportDataService.getGroupOverviewData(...)` only grouped runs by built-in fields (batch name, environment, browser, locale, area). It did not query or evaluate `getRunTagValues(run, dim)` for dynamic tag dimensions, nor did it pass dynamic tag criteria into `getBatchLatestTestExecutionsSummary(...)`.
  4. `AuraReportViewController.batchHistory(...)` and `matchesAllDimensions(...)` assumed any non-built-in dimension mapped to `batchName` and queried `runRepository.findByBatchName...`, causing single-dimension history drill-downs for dynamic tags to fail or show empty results.
- **Detection Gap ("What did we miss?"):** Existing controller and template tests in `AuraReportViewControllerTest` only tested built-in dimension keys (`batch`, `env`, `browser`, `locale`, `area`) and verified static HTML fragment renders where "Tag (Batch)" was hardcoded. No test verified dynamic discovery of distinct tag keys from `TestRunTagRepository.findDistinctTagKeys()` or end-to-end grouping by dynamic tag keys like `Release`.
- **Resolution:**
  1. Created `GroupingTagOptionDto` in `com.xceptance.aura.report.dto` holding `tagKey`, `displayName`, `toggleUrl`, and `active` state.
  2. Updated `AuraReportViewController` to query `dataService.getAllTagKeys()`, filter out built-in dimension names, and expose `groupingTags` list to the model in `batchOverview` and `batchHistory`.
  3. Updated `parseGroupDimensions`, `buildGroupLabel`, and `computeToggleUrl` to support dynamic tag keys (and `tag:<key>`) alongside built-in dimensions.
  4. Updated `AuraReportDataService.getGroupOverviewData` to dynamically accept valid tag keys, group runs by tag values using `getRunTagValues(r, dim)`, and pass `groupRunTagsByType` to `getBatchLatestTestExecutionsSummary`.
  5. Updated `AuraReportViewController.batchHistory` and `matchesAllDimensions` to filter and scope runs by dynamic tag values using `getRunTagValues(r, dim)`.
  6. Replaced hardcoded "Tag (Batch)" buttons in `batch-overview.html` and `batch-history.html` with `th:each="tagOption : ${groupingTags}"`, and updated header and card icon resolution to map tag groups to `sell`.
- **Safety Net Added:** Added 4 automated regression tests in `AuraReportViewControllerTest`:
  - `testBuildGroupingTagOptionsFromDatabase()`
  - `testBatchOverviewDynamicTagGrouping()`
  - `testBatchHistoryDynamicTagGroupingScoping()`
  - `testBatchOverviewTemplateRendersDynamicTagsAndOmitsBatchWhenAbsent()`
