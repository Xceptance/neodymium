# [DEF-20261112-1530] Strip Leading @ Prefix From Areas in Widgets, Test Base, and Run Report

- **Status:** `Resolved`
- **Opened:** 2026-11-12 15:30
- **Closed:** 2026-11-12 15:45
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager, Area and Sub-Area names displayed with a leading `@` prefix in dashboard widgets (such as area charts and breakdown lists), on the Test Base (Variation Base) tabs and tables, and throughout the Run Report page (area pie card titles, grouped test execution headers, and sub-area summary badges).
- **Root Cause:** Historically, Neodymium tags and areas were prefixed with `@` (e.g. `@Browse`, `@Checkout`). Multiple layers across `aura-manager` actively enforced or prepended this prefix:
  1. Thymeleaf templates (`dashboard-widgets.html`, `test-base.html`, `run-report.html`) contained ternary expressions such as `${area.areaName.startsWith('@') ? area.areaName : '@' + area.areaName}` which forcibly prepended `@` even if the underlying model provided clean values.
  2. `AuraReportDataService.normalizeArea()` explicitly prepended `@` to all non-null area names (`return cleanVal.startsWith("@") ? cleanVal : "@" + cleanVal`).
  3. `TestBaseVariationEntity` and `TestExecutionDto` stored and exposed area and sub-area fields containing leading `@`.
  4. `LocalRunJsonStorageService` formatted sub-areas and areas with `@` on disk export and ingestion.
  5. `WidgetAggregationService` explicitly formatted areas with `@` when aggregating tag and area distribution metrics.
- **Detection Gap ("What did we miss?"):** Previous unit and integration tests asserted against the old `@` prefix, treating `@Area` as canonical rather than domain-clean `Area` values. No end-to-end template test verified that area titles, widget labels, and test base tabs rendered without the `@` prefix.
- **Resolution:**
  1. Updated Thymeleaf fragments (`dashboard-widgets.html`, `test-base.html`, `run-report.html`) to display `area.areaName` without prepending `@`.
  2. In `AuraReportDataService.java`, updated `normalizeArea(String)` to strip leading `@` and return canonical clean area names (e.g. `Browse`), and stripped leading `@` from sub-area groupings across all DTO and summary builders.
  3. In `TestBaseVariationEntity.java`, added `cleanAreaValue()` and `cleanSubAreaValue()` methods to strip leading `@` characters from `area` and `subArea` fields.
  4. In `TestExecutionDto.java`, stripped leading `@` from `resolvedAreaName` and `resolvedSubArea`.
  5. In `WidgetAggregationService.java`, removed hardcoded `@` prefixes and delegated to `AuraReportDataService.normalizeArea(area)`.
  6. In `LocalRunJsonStorageService.java`, added `stripLeadingAt()` helper and ensured all extracted and serialized area and sub-area strings are processed cleanly without `@`.
- **Safety Net Added:** Verified regression tests across `RunStorageSyncServiceTest`, `AuraReportViewControllerTest`, and `WidgetAggregationServiceTest` in `aura-manager`, asserting that area names and sub-areas are processed, aggregated, and displayed cleanly without leading `@`.
