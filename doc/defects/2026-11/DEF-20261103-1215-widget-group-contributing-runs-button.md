# [DEF-20261103-1215] Widget Group Contributing Runs Redundant Child Triggers and Missing Container Header Action

- **Status:** `Resolved`
- **Opened:** 2026-11-03 12:15
- **Closed:** 2026-11-03 12:30
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Each single widget within a united container group displays an independent button to view contributing test executions, causing header clutter and redundant triggers for widgets that share the exact same underlying runs.
  2. The parent container card header lacks a general group-level button to view contributing runs.
  3. Opening the contributing runs modal for a container widget ID fails to scope test executions to the container's contributing runs.
- **Root Cause:**
  1. `dashboard-widgets.html` placed `openWidgetExecutionsModal` inside `.widget-container-section` instead of `.widget-container-header`.
  2. `WidgetAggregationService.resolveContributingExecutions` omitted `WidgetPresentation.CONTAINER` from `isLatestVariationScope` and did not resolve scoped target runs using `resolveContributingRunIds`.
- **Detection Gap ("What did we miss?"):**
  - Existing tests verified `openWidgetExecutionsModal` on individual trend and pie widgets, but did not assert action button layout or contributing execution resolution on united container widgets.
- **Resolution:**
  - Remove `openWidgetExecutionsModal` button from `.widget-container-section` child headers in `dashboard-widgets.html`.
  - Add general `openWidgetExecutionsModal` button to `.widget-container-header` in `dashboard-widgets.html`.
  - Update `WidgetAggregationService.resolveContributingExecutions` to recognize `CONTAINER` presentation, scope target runs using `resolveContributingRunIds`, and include all latest test variations across the group.
  - Display the `layers` icon in `widget-executions-modal.html` when inspecting a container group.
- **Safety Net Added:**
  - `StaticResourceSyntaxTest.testWidgetContainerContributingRunsGeneralButtonAndNoSingleChildButton`: Verifies container header has `openWidgetExecutionsModal` and child sections do not.
  - `AuraReportViewControllerSpringContextTest.testRenderDashboardWidgetsWithContainerContributingRunsGeneralButton`: Verifies HTML rendering of group contributing runs button on container card header and omission on child sections.
  - `AuraReportViewControllerTest.testGetWidgetExecutions_ContainerWidget_ResolvesContributingExecutionsForGroup`: Verifies container widget ID returns contributing executions for the group.
  - `WidgetAggregationServiceTest.testResolveContributingExecutions_ForContainerWidget`: Verifies scoping and variation aggregation for container configs.
