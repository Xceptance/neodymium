# [DEF-20261023-1226] Batch History Directory Filter and Ratio Desync

- **Status:** `Resolved`
- **Opened:** 2026-10-23 12:26
- **Closed:** 2026-10-23 12:46
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On the Batch History page (`/batch-history?batchName=Wikipedia`), when a test run contains 3 test executions (1 with locale `en`, 2 without explicit locale falling back to `Default`, all using browser `Chrome_1920x1080`), the Locale ratio bar accurately showed 3 tests (`1 EN + 2 Default`), but the Browser ratio bar, Environment ratio bar, and the Latest Test Status component (donut chart, test badge, and contributing runs) only showed 1 test (`1 Test across 1 Run`).
- **Root Cause:**
  1. `RunStorageSyncService.java` line 243 previously had `&& !"default".equalsIgnoreCase(loc)`. Test executions with no locale or default location were filtered out from `locSet`, and if at least one test execution specified a locale (e.g., `en`), `localesCsv` was stored as `"en"` instead of `"EN,Default"`. Furthermore, when `root.path("locales")` or `root.path("browsers")` existed in `run.json`, execution-level scanning was bypassed, preventing detection of executions with `Default` locale or specific browser variants (e.g. `Chrome_1920x1080` vs fallback `"Chrome"`).
  2. `AuraReportViewController.java` populated `filterLocales` and `filterBrowsers` solely by splitting `allRuns`' `localesCsv` and `browsersCsv`, missing `Default` and specific variations from `latestVariations` and `batchTestClasses`.
  3. In `report-manager.js`, the filtering checks `matchLoc = selectedLocs.length === 0 || selectedLocs.includes(loc)` and `matchBrowser = selectedBrowsers.length === 0 || selectedBrowsers.includes(browser)` inverted the default unconstrained state: when "All Selected" was active, `selectedLocs.length > 0`, so it treated the dropdown as an active restrictive filter that rejected the other 2 tests with location `DEFAULT`.
- **Detection Gap ("What did we miss?"):** Existing test fixtures used single-locale runs (e.g., all `US` or all `Default`), never exercising runs with mixed explicit and default locales in the same execution set alongside client-side directory filter recalculations.
- **Resolution:**
  1. `RunStorageSyncService.java`: Always aggregates locations and browsers from both top-level metadata and all individual executions in `execList`, normalizing locations via `AuraReportDataService.normalizeLocation` (which maps empty, unknown, and default to `"Default"`) and browsers via `normalizeBrowser`, removing generic `"Chrome"` fallback when specific variants (such as `Chrome_1920x1080`) exist.
  2. `LocalRunJsonStorageService.java`: Ensured `locales` and `browsers` arrays on `compactRootNode` are always persisted and normalized, and restored in-place enriched execution persistence.
  3. `AuraReportViewController.java`: Computed `filterEnvs`, `filterLocales`, and `filterBrowsers` as case-insensitive `TreeSet`s combining values from all runs, `latestVariations`, and `batchTestClasses`.
  4. `report-manager.js`: Updated `applyBatchDirectoryFilters()`, `recalculateBatchLatestStatusPie()`, `syncBatchRatioBarsVisualState()`, and `applyTestBaseFilters()` to check `!isLocFiltered`, `!isBrowserFiltered`, and `!isEnvFiltered` (`checkedCount > 0 && checkedCount < optionCount`), matching all variations when unconstrained.
- **Safety Net Added:**
  - `RunStorageSyncServiceTest.testMixedLocalesAndBrowsersIncludesDefaultAndSpecificInLocalesCsvAndBrowsersCsv`
  - `AuraReportViewControllerTest.testBatchHistoryDirectoryFiltersIncludeDefaultAndNormalizedLocalesAndBrowsers`
