# [DEF-20261030-1615] Contributing Runs Modal Unhighlighted Badges and Missing Run Exclusion Affordance

- **Status:** `Resolved`
- **Opened:** 2026-10-30 16:15
- **Closed:** 2026-10-30 16:25
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. In the widget contributing executions modal (`widget-executions-modal.html`), run header tags (environment, test count, cost) and execution table modes are displayed as unhighlighted text or low-contrast chips without dynamic status coloring or interactive filter highlights.
  2. Users cannot exclude runs directly from the widget contributing executions modal, and `getWidgetExecutions` ignores active `excludeRuns` filter parameters from the dashboard context, leading to discrepancies between widget cards and modal details.
- **Root Cause:**
  1. `widget-executions-modal.html` utilized generic `.tag-chip` and unstyled divs rather than highlighted semantic badge classes (`run-meta-chip chip-env`, `run-meta-chip chip-browser`, `badge-status badge-pass/warning/danger`, `badge-mode-*`), and lacked `.clickable-badge` filter bindings.
  2. `AuraReportViewController.getWidgetExecutions` did not accept or extract `excludeRuns` from request headers or parameters, and `widget-run-group-header` lacked an exclusion action button and client-side sync mechanism.
- **Detection Gap ("What did we miss?"):**
  - Existing tests in `AuraReportViewControllerTest` verified model presence and view name for `getWidgetExecutions`, but did not test with active `excludeRuns` query parameters or verify the presence of run exclusion buttons and badge highlight CSS classes.
- **Resolution:**
  - Update `widget-executions-modal.html`:
    - Add `.btn-exclude-run` to run headers.
    - Highlight environment, browser, test volume, dynamic pass rate, and cost badges.
    - Make status breakdown badges clickable to filter by status with `.active-filter-badge` highlighting.
    - Add active exclusion indicator chip with restore capability in modal header.
    - Style execution mode column with `badge-mode-*`.
  - Update `AuraReportViewController.java`:
    - Accept and extract `excludeRuns` parameter / query parameter in `getWidgetExecutions`.
    - Filter excluded runs prior to metric resolution and pass `excludedRunIds` to model and downstream services.
  - Update `report-manager.js`:
    - Implement `excludeWidgetRun` and `clearExcludedRunsInModal` to synchronize background dashboard and reload modal.
    - Update `filterWidgetExecutionsByStatus` to toggle `.active-filter-badge` on matching badges.
- **Safety Net Added:**
  - `AuraReportViewControllerTest.testGetWidgetExecutions_WithExcludeRuns_OmitsExcludedRunsFromContribution`: Verifies excluded runs are excluded from contributing run groups and execution calculations.
  - `StaticResourceSyntaxTest.testWidgetExecutionsModalBadgeHighlightAndExclusionSyntax`: Verifies template tags, exclusion buttons, and badge classes.
