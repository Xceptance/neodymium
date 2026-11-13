# [DEF-20261104-1025] Missing getTrendPadBottom Getter in DashboardWidgetViewDto Causes Thymeleaf SpEL Crash on Index Rendering

- **Status:** `Resolved`
- **Opened:** 2026-11-04 10:25
- **Closed:** 2026-11-04 10:28
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Visiting the dashboard root `/` fails with HTTP 500: `org.thymeleaf.exceptions.TemplateInputException: An error happened during template parsing` caused by `SpelEvaluationException: EL1008E: Property or field 'trendPadBottom' cannot be found on object of type 'com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto'`.
- **Root Cause:** In `dashboard-widgets.html` (line 1002), `data-pad-bottom=${w.trendPadBottom}` was added to the trend `<svg>` element attributes to provide layout telemetry to client-side JavaScript. However, `DashboardWidgetViewDto.java` lacked a corresponding `public double getTrendPadBottom()` getter, causing SpEL property resolution to fail during template evaluation.
- **Detection Gap ("What did we miss?"):** Previous tests in `WidgetAggregationServiceTest` validated DTO aggregation logic and `StaticResourceSyntaxTest` checked JavaScript assets and basic regex matching, but neither rendered `fragments/dashboard-widgets :: trendGraphBody` with an active `DashboardWidgetViewDto` through the Thymeleaf Spring template engine.
- **Resolution:**
  1. Added `public double getTrendPadBottom()` to `DashboardWidgetViewDto.java` returning `isQuarterWidth() ? 30.0 : 35.0` (matching `padBottom` in `WidgetAggregationService.java`).
  2. Added an automated Spring MVC / Thymeleaf template integration test in `AuraReportViewControllerSpringContextTest.java` verifying that `fragments/dashboard-widgets :: trendGraphBody` renders cleanly with all SVG data attributes intact.
- **Safety Net Added:** Added `@Test public void testTrendGraphBodyFragmentRendersWithoutSpelErrors()` in `AuraReportViewControllerSpringContextTest.java`.
