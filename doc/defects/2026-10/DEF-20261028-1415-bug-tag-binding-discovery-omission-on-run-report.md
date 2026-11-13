# [DEF-20261028-1415] Bug Tag Binding Two-Stage Key and Value Discovery Omission on Run Report

- **Status:** `Resolved`
- **Opened:** 2026-10-28 14:15
- **Closed:** 2026-10-28 14:24
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On the run report page (`/run-report`), the tag binding Key dropdown only shows 5 basic dimensions (`environment`, `batch`, `browser`, `location`, `area`) and misses database keys like `branch` or `release`. Furthermore, the Value dropdown for `environment` only displays `STG`, omitting other database environments (`DEV`, `SANDBOX`, `TEST`).
- **Root Cause:** In `AuraReportViewController`, `allKnownTagMap` was only supplied to partial fragment routes (`/fragments/test-side-panel`) and omitted from full-page routes (`/run-report`, `/batch-history`, `/test-base`). Because `index.html` includes `side-panel-step-list.html` on initial render, `window.allKnownTagMap` was empty on page load, forcing a client-side fallback that harvested only attributes from the active page's table rows (which were limited to `STG`). Additionally, custom tag keys in `AuraReportDataService.getAllKnownTagMap()` retained capitalized database casing, causing presentation discrepancies, and custom run-level tags from `TEST_RUN_TAG` were not merged into execution-level tag criteria evaluation in `matchesBugTags`.
- **Detection Gap ("What did we miss?"):** Integration test `AuraReportTagBoundBugSeverityTest` directly invoked `dataService.getAllKnownTagMap()` in isolation rather than testing Spring MVC controller endpoints with full page requests (`/run-report`, `/batch-history`), allowing the missing model attribute on full page loads to go undetected.
- **Resolution:** 
  1. Added `@ModelAttribute("allKnownTagMap")` and `@ModelAttribute("allKnownTags")` to `AuraReportViewController` to ensure all endpoints automatically supply full tag dictionaries to the model.
  2. Canonicalized custom tag keys to lowercase in `AuraReportDataService.getAllKnownTagMap()` while preserving exact value casing.
  3. Added overloaded `matchesBugTags` and `applyBugsToExecution` in `AuraReportDataService` that accepts and merges run-level tags from `tagRepository` (or `runEntity.getTags()`) with execution tags before evaluation by `TagUtils.matchesTagCriteria`.
  4. Added `data-run-tags` attribute to `runReport` root container in `run-report.html` and updated `report-manager.js` to parse it into quick-pick chips and client tag discovery.
  5. Enhanced `updateBugTagKeySelectOptions` and `onBugTagKeyChanged` in `report-manager.js` with case-insensitive fallback lookups.
- **Safety Net Added:** 
  1. Updated `AuraReportTagBoundBugSeverityTest.testGetAllKnownTagMapDiscoversAllDimensions` asserting lowercase canonical keys (`branch`, `release`) and verifying that executions match run-level bound tags (`branch: develop`, `release: 2026.11`).
  2. Added `AuraReportViewControllerSpringContextTest.testModelAttributeAllKnownTagMapAndTagsPopulated` verifying `allKnownTagMap` and `allKnownTags` are populated with all required dimensions by the controller.
