# [DEF-20261112-1405] Suite Run Filter Suppression and Unknown Tag Leak in Execution Identity and Dashboard Grouping

- **Status:** `Resolved`
- **Opened:** 2026-11-12 14:05
- **Closed:** 2026-11-12 14:25
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. The known run tag `Suite` (e.g. `Suite: Aura`, `Suite: Console`) is displayed in the runs table but is missing as a filter option under Custom Run Filters in the Widget Configurator, Page Creation, and Convert to Group modals.
  2. Unknown / unconfigured tags and dimensions for the active project (such as `Locale`, `Environment`, `Release`, `Branch`, and `batch`) are presented as selectable options in Execution Identity definition and Dashboard Grouping. Additionally, tag keys with different casing (e.g. `Project` and `project`) appear duplicated.
  3. Browser filtering options in Custom Run Filters include un-run browsers (`Chrome`, `Chrome_headless`, `Firefox`, `Firefox_headless`, `Safari`, `Edge`) that do not exist in any test execution run.
- **Root Cause:**
  1. `AuraReportDataService.isStandardDimensionKey(...)` classified `"suite"` as a standard dimension key, causing `getCandidateCustomTags()` to skip it. Because `Suite` had no dedicated standard filter block, it was omitted entirely from custom run filters. In addition, `getAllKnownTagMap()` and `ReportDashboardSettingService.normalizeDimensionName()` mapped `"suite"` to `"batch"`.
  2. In `widget-configurator-modal.html` and `modals.html`, `Locale` and `Environment` were unconditionally rendered as checkboxes and grouping buttons regardless of whether `candidateLocales` or `candidateEnvs` contained real values.
  3. `AuraReportViewController.resolveAvailableWidgetTagKeys()` hardcoded `"Branch"` and `"Release"`, and `populateCandidateFilterAndIdentityAttributes()` dumped those keys plus `"batch"` directly into `availableIdentityTags`.
  4. Tag key deduplication in `availableIdentityTags` used case-sensitive checking (`!contains()`), producing duplicate pills for case variations (`Project` vs `project`).
  5. `AuraReportDataService.getAllKnownTagMap()` hardcoded `List.of("Chrome", "Chrome_headless", "Firefox", "Firefox_headless", "Safari", "Edge")`, contaminating `candidateBrowsers` with un-run browser names.
  6. `TestRunTagRepository` queries did not filter out tags from soft-deleted runs (`isDeleted = false`), and `deleteRun()` did not delete tags in `tagRepository`.
- **Detection Gap ("What did we miss?"):** Previous tag discovery and filter tests used pre-populated test fixtures containing `Branch` and `Release`, and asserted that `suite` was excluded from custom tags under the assumption it was represented elsewhere, without verifying end-to-end filter candidate rendering when legitimate `Suite` tags exist on untagged or single-dimension projects.
- **Resolution:**
  1. Removed `"suite"` from `AuraReportDataService.isStandardDimensionKey` so legitimate `Suite` tags are included in `getCandidateCustomTags()`.
  2. Separated `"suite"` from `"batch"` in `getAllKnownTagMap()` and `ReportDashboardSettingService.normalizeDimensionName()`.
  3. Removed hardcoded browser loop (`List.of("Chrome", ...)`) from `getAllKnownTagMap()`, sourcing candidate browsers dynamically from executed runs/variations only.
  4. Removed hardcoded `"Branch"` and `"Release"` from `resolveAvailableWidgetTagKeys()`.
  5. Disconnected `availableTags` from `availableIdentityTags`, resolving `availableIdentityTags` purely from active project tags with case-insensitive deduplication and excluding internal dimensions (`batch`, `browser`, `locale`, `environment`).
  6. Guarded `Locale`, `Environment`, and `Area` pills/buttons in `widget-configurator-modal.html` and `modals.html` with `candidateLocales`, `candidateEnvs`, and `candidateAreas` empty checks.
  7. Scoped `TestRunTagRepository.findDistinctTagKeys()` and `findDistinctTagValuesByTagKey()` to active runs (`r.isDeleted = false`), and deleted tag records in `deleteRun()`.
- **Safety Net Added:**
  - Automated tests asserting `Suite` tag discovery and candidate custom tag generation for runs with `Suite` tags in `AuraReportTagBoundBugSeverityTest` and `AuraReportViewControllerTest`.
  - Automated tests asserting `candidateBrowsers` only contains browsers present in executed runs without injecting un-run browsers in `AuraReportDataServiceTest` and `AuraReportViewControllerTest`.
  - Assertions verifying `candidateLocales` and `candidateEnvs` suppression when empty in both execution identity and grouping fragments in `AuraReportViewControllerTest`.
  - Assertions verifying no duplicate casing in `availableIdentityTags` and exclusion of hardcoded `Branch`, `Release`, and `batch`.
