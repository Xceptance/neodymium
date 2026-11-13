# [DEF-20261103-1115] Custom Dashboard Widgets Filter & Execution Identity Inheritance Failure and Redundant UI Controls

- **Status:** `Resolved`
- **Opened:** 2026-11-03 11:15
- **Closed:** 2026-11-03 11:28
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Widgets on custom dashboard pages did not reflect the page's execution identity dimensions (e.g. browser + environment) or run filters; instead, test variations fell back to global browser-only identity. Additionally, two "Add Widget" buttons appeared simultaneously, along with two non-functional "Manage" buttons.
- **Root Cause:** 
  1. `AuraReportViewController.getCustomDashboardPage` and `getWidgetExecutions` invoked `getBatchLatestTestExecutionsSummary` with null uniqueness dimensions and empty criteria sets instead of passing the page's effective execution identity dimensions and run filters.
  2. Both `custom-dashboard-page.html` and `dashboard-widgets.html` contained "Add Widget" and "Manage" buttons, where "Manage" targeted an undefined JavaScript function `openWidgetManagerModal()`.
- **Detection Gap ("What did we miss?"):** Tests verified that `customPage` attributes were placed in the model, but did not assert that `dataService.getBatchLatestTestExecutionsSummary` was invoked with the page's specific execution identity dimensions and run criteria.
- **Resolution:** 
  1. Forward page-level execution identity dimensions and run filters into `dataService.getBatchLatestTestExecutionsSummary` during custom page rendering and execution drill-down.
  2. In `dashboard-widgets.html`, suppress `#btnAddWidgetDashboard` when `customPage != null`, preserving a single "Add Widget" button in the custom dashboard page toolbar.
  3. Remove the obsolete and non-functional "Manage" buttons from both templates.
  4. Pass `pageId` from `#pageCustomDashboard` when opening the widget configurator modal.
- **Safety Net Added:** Unit tests in `AuraReportViewControllerTest` verifying multi-dimension execution identity (browser + environment) and runs filter inheritance on custom pages, plus template syntax tests in `StaticResourceSyntaxTest`.
