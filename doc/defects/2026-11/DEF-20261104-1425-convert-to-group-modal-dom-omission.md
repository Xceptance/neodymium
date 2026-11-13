# [DEF-20261104-1425] Convert to Group Dashboard Modal Omission and Configuration Gap

- **Status:** `Resolved`
- **Opened:** 2026-11-04 14:25
- **Closed:** 2026-11-04 14:34
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking the "#btnConvertToGroupDashboard" ("Convert to Group") button on a sub grouping dashboard had no effect. No dialog was displayed and no console errors occurred because `document.getElementById('convertToGroupModal')` returned `null`.
- **Root Cause:** In `templates/fragments/modals.html`, the modal element was defined with an inline ID but omitted the Thymeleaf fragment identifier `th:fragment="convertToGroupModal"`, and `templates/index.html` originally lacked the `<div th:replace="~{fragments/modals :: convertToGroupModal}"></div>` injection. Consequently, the modal container was never rendered into the DOM. Furthermore, unlike the primary dashboard creation dialog, the convert modal lacked parity for configuring execution identity dimensions and default run filters (limit, dates, browsers, locales, environments, areas).
- **Detection Gap ("What did we miss?"):** Previous integration tests in `AuraReportViewControllerSpringContextTest` only verified `index.html` rendering for general navigation tabs and widget modals, without asserting the existence of `#convertToGroupModal` or testing template inclusion of the convert-to-group fragment.
- **Resolution:**
  1. Updated `templates/index.html` to inject `<div th:replace="~{fragments/modals :: convertToGroupModal}"></div>`.
  2. Redesigned `#convertToGroupModal` in `templates/fragments/modals.html` using the standard modal overlay structure, adding `th:fragment="convertToGroupModal"`, grouping dimension selector pills, group cards status evaluation scope radios, execution identity dimensions, and full runs filter inputs (limit, dates, browsers, locales, environments, areas).
  3. Added `updateConvertToGroupIdentityDimensionsInput()`, `updateConvertToGroupFilterInputs()`, `setConvertToGroupFilterLimit()`, and `clearConvertToGroupFilterDates()` in `static/js/report-manager.js`, ensuring all identity and filter options are captured and submitted via POST.
  4. Updated `ReportDashboardSettingService.convertToGroupDashboard(...)` to accept execution identity dimensions and run filters, applying them to the converted group parent and inheriting them down into generated child sub-dashboards.
  5. Updated `AuraReportViewController.convertCustomDashboardPageToGroup(...)` to accept and parse identity dimensions and run filter query parameters.
- **Safety Net Added:**
  - `AuraReportViewControllerSpringContextTest.testIndexPageContainsConvertToGroupModal`
  - `AuraReportViewControllerSpringContextTest.testConvertToGroupModalFragmentRendersCleanly`
  - `ReportDashboardSettingServiceTest.testConvertToGroupDashboardWithCustomIdentityAndFilters`
