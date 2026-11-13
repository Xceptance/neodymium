# [DEF-20261028-1009] Batch History Dynamic Tag Ratio Clobbers Variation Tags with Run-Level First Tag

- **Status:** `Resolved`
- **Opened:** 2026-10-28 10:09
- **Closed:** 2026-10-28 10:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Batch History, the "Tag: Locale Ratio" card reports 2 variations with "ENG" (and 0 for "RUS") when a single run contains one execution with `tags.locale: ENG` and another with `tags.locale: RUS`. Both variation DOM entries receive `locale: ENG` in their `data-tags`.
- **Root Cause:** `AuraReportDataService.getBatchLatestExecutions()` discarded execution-level tags (`exec.getTags()`) when building variation maps and resolved tag values by querying the parent run entity via `getRunTagValues(varRun, tagKey)`. Because both variations belong to the same multi-locale run, the list of run tags `["ENG", "RUS"]` was resolved by taking `vals.get(0)`, clobbering all variations in that run with the first tag value (`"ENG"`).
- **Detection Gap ("What did we miss?"):** Existing ratio tests in `AuraReportViewControllerTest` used runs where all executions within the run shared the same tag values, never testing multi-dataset runs where executions within a single run carry distinct tag values.
- **Resolution:** Preserved variation-level tags in `AuraReportDataService.getBatchLatestExecutions()` via `variationTags` map and checked variation-level tags before falling back to parent run tags for both `tagRatiosByType` and `latestVariations`. Additionally enhanced `LocalRunJsonStorageService` and `RunStorageSyncService` to inspect execution `tags.locale` when extracting locales and locations.
- **Safety Net Added:** Added regression tests in [TestBaseVariationHistoryTest.java](file:///home/olha/git/neodymium/aura-manager/src/test/java/com/xceptance/aura/report/service/TestBaseVariationHistoryTest.java) (`testBatchLatestExecutionsPreservesVariationSpecificTagsInRatioCardAndDto`) and [RunStorageSyncServiceTest.java](file:///home/olha/git/neodymium/aura-manager/src/test/java/com/xceptance/aura/report/service/RunStorageSyncServiceTest.java) (`testExtractLocationFromNode` and `testImportOrUpdateRunReportExtractsLocalesFromTags`).
