# [DEF-20261111-1106] Area Fallback in Ratio Charts and Report Page Defaults to Default Instead of @Browsing (default)

- **Status:** `Resolved`
- **Opened:** 2026-11-11 11:06
- **Closed:** 2026-11-11 11:49
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When test cases have no known/user-defined area tags (e.g. `null`, blank, `@Browsing (default)`, or `@General`), dashboard ratio bars, pie charts, breakdown lists, and trend graphs display `@Browsing (default)` (e.g. `● @Browsing (default) 2 (100%)`). On the Run Report page (`/run-report`), test executions in runs with other tagged tests are grouped under `📁 Area: @Browsing (default)` instead of `📁 Area: Default`.
- **Root Cause:** In `AuraReportDataService.normalizeArea()`, `TestExecutionDto`, `TestBaseVariationEntity`, and `LocalRunJsonStorageService`, untagged executions were hardcoded to default to `"Browsing (default)"` and `"@Browsing (default)"`. In `WidgetAggregationService`, tag extraction for area dimension unconditionally normalized via `normalizeArea` without returning `"Default"`. In Thymeleaf templates, area names were prefixed unconditionally with `@` rather than rendering `"Default"` cleanly without `@`.
- **Detection Gap ("What did we miss?"):** Previous area hierarchy suppression work focused on hiding the accordion entirely when 0 real areas were detected across an entire run, but did not address the fallback label when charts aggregate untagged variations or when runs have mixed (tagged and untagged) tests.
- **Resolution:**
  1. Updated `AuraReportDataService.normalizeArea` and `isRealArea` to treat `"Default"` and `"@Default"` as non-real area fallbacks, normalizing untagged tests to `"Default"`.
  2. Updated `AuraReportDataService.buildAreaSummaries` and `getRunAreas` to group untagged test classes under `"Default"`.
  3. Updated `TestExecutionDto`, `TestBaseVariationEntity`, and `LocalRunJsonStorageService` to default untagged area names and area tags to `"Default"` instead of `"Browsing (default)"` / `"@Browsing (default)"`.
  4. Updated `WidgetAggregationService.extractVariationTagValue`, `extractExecutionTagValue`, and `resolveRunTagCounts` to return `"Default"` whenever an area is not a real user-defined area tag.
  5. Updated `run-report.html`, `dashboard-widgets.html`, and `test-base.html` so that `"Default"` displays without a leading `@` as `Area: Default`.
  6. Updated `report-manager.js` (`normalizeAreaFilterVal` and `resolveVariationDimensionValue`) to resolve untagged tests to `'Default'` and support clicking the `'Default'` legend chip/ratio segment.
- **Safety Net Added:**
  - Added `WidgetAggregationServiceTest.testAreaRatioBarWidgetDefaultsToDefaultWhenNoAreasKnown` and `WidgetAggregationServiceTest.testAreaPieChartWidgetDefaultsToDefaultWhenNoAreasKnown`.
  - Added `AuraReportViewControllerSpringContextTest.testRunReportRendersDefaultInsteadOfBrowsingDefaultForUntaggedTestsWhenRealAreasExist`.
  - Updated `StaticResourceSyntaxTest.testUnitedContainerAreaAndLocaleRatioBarNormalizationSupport` and verified with `mvn test -pl aura-manager -Dtest=StaticResourceSyntaxTest,WidgetAggregationServiceTest,AuraReportViewControllerSpringContextTest`.
