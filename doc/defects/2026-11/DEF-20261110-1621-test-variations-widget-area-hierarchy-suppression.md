# [DEF-20261110-1621] Test Base Variations Dashboard Widget Displays Artificial Area Hierarchy

- **Status:** `Resolved`
- **Opened:** 2026-11-10 16:10
- **Closed:** 2026-11-10 16:31
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** The "Known Test Base Variations" widget displays `Area: @Browsing (default)` and groups tests under an area accordion even when tests have no user-defined area tags.
- **Root Cause:** `dashboard-widgets.html` unconditionally iterated `${w.testAreas}` in `testVariationsBody(w)` without checking if legitimate user-defined areas exist (`w.hasRealAreas()`). In addition, `DashboardWidgetViewDto` lacked a `hasRealAreas()` evaluation method, and the fallback to plain test classes (`w.testClasses`) required `w.testAreas` to be empty, which was never true because untagged variations defaulted to the synthetic `@Browsing (default)` area.
- **Detection Gap ("What did we miss?"):** Previous area hierarchy suppression work focused exclusively on `/run-report` and `/test-base` pages, leaving the dashboard widget fragment (`dashboard-widgets.html`) unverified. Furthermore, earlier integration tests for dashboard widgets did not assert the absence of `Area:` headers when variations lack explicit area tags.
- **Resolution:** Added `hasRealAreas()` to `DashboardWidgetViewDto`, guarded `${w.testAreas}` in `dashboard-widgets.html` with `th:if="${w.hasRealAreas()}"`, rendered `${w.testClasses}` directly when `!w.hasRealAreas()`, and updated empty-state handling.
- **Safety Net Added:** Added `@Test` `AuraReportViewControllerSpringContextTest.testTestVariationsWidgetSuppressesAreaHierarchyWhenNoRealAreasExist` and `testTestVariationsWidgetRendersAreaHierarchyWhenRealAreasExist` verifying that untagged test variations render flat class rows without the `Area:` accordion, while tagged variations preserve legitimate area grouping.
