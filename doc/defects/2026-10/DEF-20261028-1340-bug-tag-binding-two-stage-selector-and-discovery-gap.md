# [DEF-20261028-1340] Incomplete Tag and Environment Discovery and Lack of Two-Stage Key-Value Tag Selection in Side Panel Bug Form

- **Status:** `Resolved`
- **Opened:** 2026-10-28 13:40
- **Closed:** 2026-10-28 13:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the side-panel bug creation dialog, known tag discovery only listed `environment: STG` even when multiple environments existed across runs, batches, and variations. Furthermore, other critical tags (such as locations from `localesCsv`, browsers from `browsersCsv`, and area tags from `TestBaseVariationEntity`) were missing, and all available tags were flattened into a single confusing dropdown.
- **Root Cause:**
  1. `AuraReportDataService.getAllKnownTagsList()` only queried `batchRepository.findAll()` and non-deleted runs in `runRepository`, completely ignoring `variationRepository.findAll()`, `localesCsv`, and `browsersCsv`.
  2. Capitalization discrepancies in `TestRunTagEntity` (`"Environment"`, `"Suite"`, `"Locale"`) failed to coalesce with canonical lowercased dimensions.
  3. The UI provided only a single flat dropdown instead of a structured two-stage (Key -> Value) selector.
- **Detection Gap ("What did we miss?"):** Prior unit tests only asserted that single known tags like `environment: STG` appeared in the list, without asserting multi-environment discovery across `variationRepository`, `localesCsv`, or verifying structured key-value dimension grouping.
- **Resolution:**
  1. Implemented `getAllKnownTagMap()` in `AuraReportDataService` aggregating distinct values across `runRepository`, `batchRepository`, `variationRepository`, and `tagRepository` mapped to canonical keys (`environment`, `batch`, `browser`, `location`, `area`, and custom keys).
  2. Updated `AuraReportViewController` to expose `allKnownTagMap` across side-panel and bug fragment endpoints.
  3. Implemented a two-stage Key -> Value dropdown selector in `side-panel-step-list.html` with real-time dynamic option population and selection in `report-manager.js`.
  4. Added client-side DOM harvesting fallback in `report-manager.js` to ensure any tags displayed on current tables/filters are merged.
- **Safety Net Added:** Added `testGetAllKnownTagMapDiscoversAllDimensions` in `AuraReportTagBoundBugSeverityTest` asserting multi-environment and cross-repository dimension discovery in `getAllKnownTagMap()`.
