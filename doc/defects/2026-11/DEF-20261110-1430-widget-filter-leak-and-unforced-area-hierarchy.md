# [DEF-20261110-1430] Custom Run Filter Leak and Unforced Area Hierarchy on Untagged Tests

- **Status:** `Resolved`
- **Opened:** 2026-11-10 14:30
- **Closed:** 2026-11-10 14:45
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager's Widget Configurator and Run Filters, `batch` and `Suite` appear as duplicated dynamic custom tags (e.g. `Filter by batch: Queue` and `Filter by Suite: Queue`), and single-option fallback filters (`Locale: Default`, `Environment: Default`, `Area: @Browsing (default)`) are displayed even when no variation exists across executions. Additionally, in the Run Report and Test Base views, untagged tests are forced into an `@Browsing (default)` folder hierarchy and tab bar rather than rendering directly as plain test classes.
- **Root Cause:**
  1. `AuraReportDataService.isStandardDimensionKey(...)` did not include `batch`, `batchname`, or `suite`, causing `getCandidateCustomTags()` to categorize the run's batch name and synthesized `Suite` tag as dynamic custom tags.
  2. `AuraGlobalModelAdvice` and `AuraReportViewController` artificially injected fallback defaults (`Default`, `@Browsing (default)`) into candidate filter collections even when no distinct variants existed.
  3. `run-report.html` and `test-base.html` unconditionally wrapped test classes inside area group containers (`.area-group`, `.sub-tab-bar`, `.test-base-area-pane`) rather than checking whether real user-defined areas exist.
- **Detection Gap ("What did we miss?"):** Previous tag discovery and area grouping tests used explicit multi-area fixtures (e.g. `@Search`, `@Checkout`, `Branch`, `Release`) and did not test the pure baseline scenario where a project has zero custom tags, single fallback locales, and untagged test classes.
- **Resolution:**
  1. Updated `isStandardDimensionKey` to filter out `batch`, `batchname`, and `suite`.
  2. Added `AuraReportDataService.isRealArea(String areaName)` helper.
  3. Refined candidate filter preparation in `AuraGlobalModelAdvice` and `AuraReportViewController` to omit single-option trivial fallback dimensions.
  4. Added `hasRealAreas()` to `RunReportDto` and `TestBaseDataDto` (checking for areas other than `@Browsing (default)` and `General`).
  5. Updated `run-report.html` and `test-base.html` to conditionally skip the area breakdown panel, area group accordion headers, and area tab bar when `hasRealAreas()` is false, rendering plain test classes directly.
- **Safety Net Added:** Added tests in:
  - `AuraReportTagBoundBugSeverityTest.testCandidateCustomTagsExcludesBatchAndSuite`: asserts `getCandidateCustomTags()` excludes `batch`, `suite`, and `batchname`.
  - `RunReportDtoTest.testHasRealAreas`: tests `hasRealAreas()` on `RunReportDto` and `TestBaseDataDto`.
  - `AuraReportViewControllerTest.testCandidateFiltersOmittedWhenOnlyTrivialDefaults`: tests candidate filters suppression in model advice and controller.
