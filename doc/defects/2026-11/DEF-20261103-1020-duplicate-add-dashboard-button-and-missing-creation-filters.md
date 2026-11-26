# [DEF-20261103-1020] Duplicate Add Dashboard Button and Missing Page-Level Execution Identity & Runs Filter on Creation

- **Status:** `Resolved`
- **Opened:** 2026-11-03 10:20
- **Closed:** 2026-11-03 10:38
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** The sidebar displayed both a '+' icon button in the header and a '+ New Page' card button in the nav list to create custom dashboards. Furthermore, the dashboard creation dialog only accepted a page name without allowing users to configure the page's default execution identity dimensions and runs filter criteria.
- **Root Cause:** Redundant button elements in `sidebar.html` coupled with `DashboardPageConfigDto` and `createPageModal` lacking identity and run filter properties and inputs.
- **Detection Gap ("What did we miss?"):** Existing UI controller tests only verified page creation with a name string and did not test page-level scoping or sidebar button uniqueness.
- **Resolution:** Removed the redundant `+ New Page` button in `sidebar.html`, added `executionIdentityDimensions` and `runFilters` to `DashboardPageConfigDto`, expanded `ReportDashboardSettingService.createCustomPage`, updated `createPageModal` with identity and runs filter controls, and bound page-level criteria to widget evaluation.
- **Safety Net Added:** Unit tests in `AuraReportViewControllerTest` and `ReportDashboardSettingServiceTest` asserting page creation with custom execution identity, run limits, and browser/environment criteria, plus template inspection tests.
