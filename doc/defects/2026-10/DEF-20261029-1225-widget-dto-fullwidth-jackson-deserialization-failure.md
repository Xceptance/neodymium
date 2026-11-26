# [DEF-20261029-1225] Widget DTO FullWidth Jackson Deserialization Failure

- **Status:** `Resolved`
- **Opened:** 2026-10-29 12:25
- **Closed:** 2026-10-29 12:31
- **Component:** `aura-manager`
- **Scope:** `Framework & SUT`
- **Symptom:** On the Run History page, attempting to add or delete a dashboard widget fails silently in the UI, and the console reports: `Failed to parse dashboard widgets JSON: Unrecognized field "fullWidth" (class com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto), not marked as ignorable`.
- **Root Cause:** In `DashboardWidgetConfigDto`, the helper method `isFullWidth()` lacked `@JsonIgnore`. Jackson serialized `"fullWidth"` into the JSON payload during `setDashboardWidgets()`. During subsequent reads, Jackson failed to deserialize because `fullWidth` was not an explicit `@JsonCreator` property and `FAIL_ON_UNKNOWN_PROPERTIES` was active.
- **Detection Gap ("What did we miss?"):** Previous unit tests constructed DTOs directly via constructor or tested `WidgetAggregationService` in-memory rather than performing round-trip serialization and deserialization through `ReportDashboardSettingService`.
- **Resolution:**
  1. Annotated `isFullWidth()` with `@JsonIgnore` in `DashboardWidgetConfigDto.java`.
  2. Added `@JsonIgnoreProperties(ignoreUnknown = true)` to `DashboardWidgetConfigDto.java`.
  3. Configured `ObjectMapper` in `ReportDashboardSettingService.java` with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false`.
- **Safety Net Added:** Added test `testDashboardWidgetsJson_IgnoresUnknownPropertiesAndFullWidth` in `ReportDashboardSettingServiceTest.java`.
