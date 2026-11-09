# [DEF-20261106-1402] Custom Run Filters Dynamic Tag Discovery and Presentation Gap

- **Status:** `Resolved`
- **Opened:** 2026-11-06 14:02
- **Closed:** 2026-11-06 14:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager's widget configurator, create page, and convert-to-group modals ("Custom Run Filters" / "Custom Page Run Filters"), users could only filter by hardcoded standard dimensions (Browsers, Locales, Environments, Test Areas). Any custom tag dimensions present in test execution runs (e.g. `Branch`, `Release`, `Team`, `Suite`, `Commit`) or variations in standard tag keys were omitted from the modal checkboxes and could not be discovered or configured.
- **Root Cause:** While the backend request-binding layer (`/widgets/save`, `/dashboard-pages/create`, etc.) already parsed arbitrary dynamic `filterTag_<tagKey>` query parameters into `WidgetFilterConfigDto.tagsByType`, and `WidgetAggregationService` honored them during filtering, the model preparation layers (`AuraReportViewController.populateCandidateFilterAndIdentityAttributes` and `AuraGlobalModelAdvice.addFilterDataToModel`) only extracted and published standard candidate collections (`candidateBrowsers`, `candidateLocales`, `candidateEnvs`, `candidateAreas`). Furthermore, the Thymeleaf templates (`widget-configurator-modal.html`, `modals.html`) and client-side JavaScript (`report-manager.js`) only had hardcoded HTML blocks for those four standard dimensions and did not dynamically iterate over discovered custom tags.
- **Detection Gap ("What did we miss?"):** Existing controller and widget tests tested standard dimension filtering (e.g. `browser`, `locale`) with pre-populated dummy values, but never verified that non-standard tag dimensions in `test_run_tag` were dynamically extracted into candidate model attributes, rendered as selectable options in the modal dialogs, or serialized back into `WidgetFilterConfigDto.tagsByType`.
- **Resolution:**
  1. Extended `WidgetFilterConfigDto` with helper methods `hasTagValue(final String tagKey, final String tagVal)` and `getTagValuesCsv(final String tagKey)` for case-insensitive Thymeleaf inspection and CSV generation.
  2. Implemented `AuraReportDataService.getCandidateCustomTags()` and helper `isStandardDimensionKey(...)` returning a sorted map of discovered non-standard tag keys and their distinct values, preserving database casing for display labels while deduplicating case-insensitively.
  3. Enriched candidate standard dimensions (`candidateBrowsers`, `candidateLocales`, `candidateEnvs`, `candidateAreas`) in `AuraReportViewController` and `AuraGlobalModelAdvice` from any matching keys in `allKnownTagMap`, and injected `candidateCustomTags` into the model.
  4. Updated `widget-configurator-modal.html` and `modals.html` to dynamically render custom tag filter sections with checkboxes and synchronized hidden inputs for each discovered custom tag key.
  5. Updated `report-manager.js` (`updateWidgetRunFiltersInputs`, `updateCreatePageFilterInputs`, `updateConvertToGroupFilterInputs`) to dynamically harvest checked custom tag checkboxes into matching hidden input fields (`filterTag_<key>`).
- **Safety Net Added:** Added tests in:
  - `ReportDashboardSettingServiceTest`: asserts `hasTagValue` and `getTagValuesCsv` behavior on `WidgetFilterConfigDto`.
  - `AuraReportTagBoundBugSeverityTest`: asserts `dataService.getCandidateCustomTags()` excludes standard dimensions and retains custom tags like `Branch` and `Release`.
  - `AuraReportViewControllerTest`:
    - `testGetWidgetConfigModal_NewWidget`: asserts `candidateCustomTags` presence in the model.
    - `testSaveWidget_WithDynamicCustomTagFilters_PersistsInTagsByType`: tests saving dynamic `filterTag_Branch` and `filterTag_Release` parameters and verifies persistence in `tagsByType`.
    - `testWidgetConfiguratorModal_RendersDynamicCustomTagsAndPreselectsExistingValues`: renders `widget-configurator-modal.html` with dynamic tags and verifies HTML checkboxes and `checked` state for configured tags.
