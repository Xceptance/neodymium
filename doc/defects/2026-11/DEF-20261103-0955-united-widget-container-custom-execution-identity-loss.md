# [DEF-20261103-0955] United Widget Container Custom Execution Identity Loss

- **Status:** `Resolved`
- **Opened:** 2026-11-03 09:47
- **Closed:** 2026-11-03 09:58
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When uniting two dashboard widgets with custom execution identities (e.g. `@ TEST + ENVIRONMENT`), the combined container lost the custom execution identity, omitting the badge and causing variation counts to revert to global default counts (e.g., dropping from 466 tests to 118 tests).
- **Root Cause:** In `DashboardWidgetConfigDto.createContainer()`, the container DTO instantiation hardcoded `overrideExecutionIdentity = false` and `executionIdentityDimensions = Collections.emptyList()`, discarding the execution identity of the united children. Furthermore:
  1. `dashboard-widgets.html` container card header omitted the execution identity badge markup present on all other widget presentations.
  2. `AuraReportViewController.combineWidgets()` lacked execution identity compatibility validation when uniting widgets.
  3. `AuraReportViewController.getWidgetExecutions()` and `getWidgetConfigModal()` only searched top-level widgets, failing to locate nested children inside container widgets.
- **Detection Gap ("What did we miss?"):** Existing container unit tests in `DashboardWidgetConfigDtoTest` and `ReportDashboardSettingServiceTest` only instantiated containers from widgets with default/empty execution identities, leaving the custom identity inheritance and presentation paths untested.
- **Resolution:**
  1. Updated `DashboardWidgetConfigDto.createContainer()` and `addChild()` to inherit custom execution identity and run filters from children.
  2. Added execution identity compatibility validation in `AuraReportViewController.combineWidgets()` rejecting unification of widgets with mismatched execution identities.
  3. Added the execution identity badge markup to the container header in `dashboard-widgets.html`.
  4. Added `findWidgetById()` recursive lookup for modal endpoints in `AuraReportViewController`.
- **Safety Net Added:**
  - `DashboardWidgetConfigDtoTest#testCreateContainerPreservesCustomExecutionIdentity`
  - `DashboardWidgetConfigDtoTest#testAddChildPreservesCustomExecutionIdentity`
  - `ReportDashboardSettingServiceTest#testCombineWidgetsPreservesCustomExecutionIdentityAndSeparationRestoresIt`
  - `WidgetAggregationServiceTest#testAggregateWidgetsWithContainerHavingCustomExecutionIdentity`
  - `AuraReportViewControllerTest#testCombineWidgets_WhenDifferentExecutionIdentities_SetsWarningMessage`
  - `AuraReportViewControllerTest#testCombineWidgets_WhenMatchingCustomExecutionIdentities_CombinesSuccessfully`

