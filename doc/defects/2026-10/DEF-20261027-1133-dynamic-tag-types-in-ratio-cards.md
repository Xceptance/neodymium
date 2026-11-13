# [DEF-20261027-1133] Dynamic Tag Types Missing From Ratio Bar Filters

- **Status:** `Resolved`
- **Opened:** 2026-10-27 11:33
- **Closed:** 2026-10-27 11:45
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Ratio card filters on `/batch-history` only rendered a single static "Tag: Batch Ratio" card based on `lastBatch`. Custom tag types present in the database (e.g. `Release`, `Team`, `Owner` stored in `test_run_tag`) did not generate dedicated ratio cards, leaving users unable to inspect distribution ratios or filter test variations and runs by dynamic tag dimensions.
- **Root Cause:** In `batch-history.html`, the tag ratio card was statically defined with `id="batchClearTagFilterBtn"` and `data-ratio-type="tag"`, populating only `tagRatios` (which mapped `variationBatch`). `AuraReportViewController` and `AuraReportDataService` did not query distinct tag keys from the database or construct ratio distributions for non-built-in tag keys. In addition, `report-manager.js` hardcoded `batchActiveRatioFilters.tag` without dynamic tag ratio bar event handling or data attribute resolution for dynamic tag dimensions.
- **Detection Gap ("What did we miss?"):** Previous ratio bar tests (`StaticResourceSyntaxTest`) only verified the presence of hardcoded ratio cards (`browser`, `locale`, `env`, `area`, and `tag`) and did not validate dynamic generation of tag ratio cards for arbitrary tag types stored in the database.
- **Resolution:**
  1. Created `TagRatioCardDto` encapsulating dynamic tag ratio card properties (`tagKey`, `displayName`, `ratioType`, `cleanKey`, `clearBtnId`, `ratios`, `emptyMessage`).
  2. Enhanced `BatchLatestVariationDto` with a `tags` map, constructors, and `getTagsJson()` method.
  3. Added `getTagsJson()` to `TestRunEntity` and `TestBaseVariationEntity` to serialize all associated tag metadata into `data-tags` on DOM rows (`.batch-latest-var-entry`, `.run-history-row`, and `.tb-entry-row`).
  4. Extended `BatchLatestExecutionsDto` and `AuraReportDataService.getBatchLatestTestExecutionsSummary(...)` to discover all non-built-in tag keys from `getAllTagKeys()` and runs, compute distribution ratios for each tag type in `tagRatiosByType`, and populate `varTags`.
  5. Updated `AuraReportViewController.batchHistory(...)` to construct and expose `tagRatioCards` in the model for all discovered tag keys.
  6. Replaced the static card in `batch-history.html` with a dynamic `th:each="card : ${tagRatioCards}"` loop and a legacy fallback card for backward compatibility. Restored `<colgroup>` on the variations table.
  7. Generalized `report-manager.js`: dynamic `Set` tracking in `batchActiveRatioFilters` for `tag_*`, `getElementTags(el)`, `getElementTagValue(el, key)`, `matchTagFilters(el)`, `matchEntryFilters(entry, excludeType)`, cross-filtering synchronization in `syncBatchRatioBarsVisualState()`, and dynamic tag support in `filterFromRatioBar` and `resetBatchRatioFilter`.
- **Safety Net Added:** Added unit and integration tests in `AuraReportViewControllerTest.testDynamicTagRatioCardsGeneration` and `StaticResourceSyntaxTest.testDynamicTagRatioBarFilteringSyntax`.
