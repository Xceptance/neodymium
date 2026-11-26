# [DEF-20261109-1040] Dashboard Widgets JSON Deserialization Failure on MATRIX Presentation

- **Status:** `Resolved`
- **Opened:** 2026-11-09 09:25
- **Closed:** 2026-11-09 10:46
- **Component:** `aura-manager` (`com.xceptance.aura.report.dto.widget` / `ReportDashboardSettingService`)
- **Scope:** `Framework`
- **Symptom:** `ReportDashboardSettingService` logged a warning `Failed to parse dashboard widgets JSON: Cannot deserialize value of type com.xceptance.aura.report.dto.widget.WidgetPresentation from String "MATRIX": not one of the values accepted for Enum class: [CONTAINER, PERLENKETTE, RUNS, PIE_CHART, TREND_GRAPH, TEST_VARIATIONS, TEXT_WITH_SMALL_TREND, RATIO_BAR, TEXT, LIST]` and discarded all user-configured dashboard widgets, reverting the dashboard view to factory defaults.
- **Root Cause:**
  1. `WidgetPresentation.fromString(String)` (and sibling widget enums `WidgetMetric`, `WidgetTrendMode`, `WidgetValueFormat`, `WidgetPointScope`, `WidgetColorPolarity`) lacked the `@JsonCreator` annotation. Jackson defaulted to strict `Enum.valueOf` matching, throwing `InvalidFormatException` whenever an alias or unknown string was encountered.
  2. The string `"MATRIX"` was not recognized as an alias for `WidgetPresentation.TEST_VARIATIONS` in `fromString`.
  3. `ReportDashboardSettingService.objectMapper` lacked `DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL`, causing unhandled enum parse errors to abort the entire `List<DashboardWidgetConfigDto>` deserialization rather than defaulting individual fields.
- **Detection Gap ("What did we miss?"):** Prior unit tests only serialized and deserialized valid, canonical enum constant names (e.g. `PIE_CHART`, `RATIO_BAR`) using Java objects rather than testing Jackson deserialization of raw JSON payloads containing aliases (such as `MATRIX`, `KNOWN_VARIATIONS`, `STRING_OF_PEARLS`) or legacy/unknown enum strings.
- **Resolution:**
  1. Annotated `fromString(final String raw)` with `@JsonCreator` across all widget enums (`WidgetPresentation`, `WidgetMetric`, `WidgetTrendMode`, `WidgetValueFormat`, `WidgetPointScope`, `WidgetColorPolarity`).
  2. Added `"MATRIX"`, `"TEST_MATRIX"`, and `"VARIATION_MATRIX"` as recognized aliases for `TEST_VARIATIONS` in `WidgetPresentation.fromString` and `WidgetMetric.fromString`.
  3. Configured `DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL = true` on `objectMapper` in `ReportDashboardSettingService`.
- **Safety Net Added:** Added unit tests in `DashboardWidgetConfigDtoTest` (`testDeserializeMatrixPresentationAsTestVariations`, `testDeserializeUnknownPresentationFallback`, `testDeserializeEnumAliasesAcrossDtoEnums`) and `ReportDashboardSettingServiceTest` (`testDashboardWidgetsJson_HandlesMatrixPresentationAndUnknownEnumValuesGracefully`) asserting successful JSON deserialization of `"MATRIX"`, case-insensitive aliases, and completely unrecognized enum tokens with safe fallback.
