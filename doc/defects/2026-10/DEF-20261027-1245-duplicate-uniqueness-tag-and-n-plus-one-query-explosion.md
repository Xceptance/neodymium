# [DEF-20261027-1245] Duplicate Uniqueness Tag Button and N+1 Database Query Explosion During Batch History Computation

- **Status:** `Resolved`
- **Opened:** 2026-10-27 12:45
- **Closed:** 2026-10-27 12:53
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Selecting the dynamic "+ Tag: Environment" uniqueness button causes a second duplicate button "+ Tag: environment" to appear in the toolbar. Furthermore, page rendering latency degrades significantly when the environment uniqueness dimension is active.
- **Root Cause:**
  1. `AuraReportViewController.availableUniquenessTags` used a case-sensitive `List.contains(activeDim)` check when reconciling active dimensions from settings (`"environment"`) against database tags (`"Environment"`). Because `"Environment".equals("environment")` was false, both entries were added to the list and both were highlighted active by Thymeleaf.
  2. `AuraReportDataService.getBatchLatestTestExecutionsSummary` invoked `isBooleanTag(tagKey, eligibleRuns)` and `getRunTagValues(run, tagKey)` inside nested loops over variations and tag keys (4,640+ invocations when environment was active). `isBooleanTag` iterated over all 130 eligible runs querying `findByRunId` on every run, and `getRunTagValues` executed `tagRepository.findByRunId` on every invocation instead of reading pre-populated `run.getTags()` in memory. Over 600,000 redundant database queries were executed during a single request.
  3. `AuraReportDataService.getBatchTestBaseClasses` invoked `getAllTagKeys()` (executing `findDistinctTagKeys()`) inside the per-variation loop rather than once upfront.
  4. `AuraReportDataService.populateRunTags` was discarding in-memory tags on runs if the database tag lookup returned an empty list.
- **Detection Gap ("What did we miss?"):** Previous tests asserted correctness of tag ratio values with small datasets and did not benchmark query counts or test case-insensitive reconciliation between settings dimensions and canonical database tag keys.
- **Resolution:**
  1. Updated `AuraReportViewController.java` to perform case-insensitive deduplication against `availableUniquenessTags`, preserve canonical title casing, and sort available uniqueness tags alphabetically via `String.CASE_INSENSITIVE_ORDER`.
  2. Updated `AuraReportDataService.getRunTagValues` to check `run.getTags()` first in memory, returning immediately with zero database queries. If `run.getTags()` is empty, the repository lookup result is cached onto the run entity.
  3. Updated `AuraReportDataService.isBooleanTag` to short-circuit upon encountering any non-boolean values from the database and avoid redundant run iterations.
  4. Precomputed `isBooleanTag` once into `Map<String, Boolean> isBoolByTag` in `getBatchLatestTestExecutionsSummary` before entering the variation and tag loops.
  5. Hoisted `getAllTagKeys()` outside the run and execution loops in `getBatchTestBaseClasses`.
  6. Updated `populateRunTags` to merge and preserve existing in-memory tags on `TestRunEntity`.
- **Safety Net Added:**
  - Automated assertions in `AuraReportViewControllerTest.testBatchHistoryExposesUniquenessModelAttributesAndRendersTemplate` verifying canonical `Environment` toggle button and ensuring zero duplicate lowercase buttons exist.
  - Multi-tag uniqueness preservation verified in `TestBaseVariationHistoryTest.testBatchLatestWithTagUniquenessPreservesReleases`.
  - Regression verified across full test suite (275/275 tests passed).
