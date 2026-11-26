# [DEF-20261013-1647] Run Report Overview and All Tests Status Filter State Desynchronization

- **Status:** `Resolved`
- **Opened:** 2026-10-13 16:47
- **Closed:** 2026-10-13 16:57
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Run Report view, filtering by status directly on the "All Tests" tab works as expected. However, after filtering tests by clicking a status metric card in the "Overview" tab (e.g., clicking "Passed"), it is impossible to remove the filter or select a different status badge in the "All Tests" tab; the table remains permanently locked to the status selected from the Overview tab. Additionally, clicking "Total Executions" hides all tests.
- **Root Cause:** 
  1. `filterAllTestsByStatus(statusKey)` (triggered by Overview KPI cards) sets both `activeWholeExecutionFilter` and `currentGlobalStatusBadgeFilter`.
  2. `filterByGlobalStatusBadge(statusKey, btnEl)` (triggered by status badges on the "All Tests" tab) only updated `currentGlobalStatusBadgeFilter` and failed to clear or update `activeWholeExecutionFilter`.
  3. `applyRunReportFilters()` evaluated `activeWholeExecutionFilter || (currentGlobalStatusBadgeFilter !== 'ALL' ? currentGlobalStatusBadgeFilter : null)`. Because `activeWholeExecutionFilter` remained truthy, it short-circuited and completely shadowed any changes made by `filterByGlobalStatusBadge`.
  4. `filterAllTestsByStatus('ALL')` assigned `activeWholeExecutionFilter = 'ALL'`, which evaluated to truthy in `applyRunReportFilters()` and tested `rowSt === 'ALL'`, matching 0 rows and hiding all test executions.
  5. `filterByGlobalStatusBadge` did not synchronize or clear the Overview card `.active-filter` highlighting.
- **Detection Gap ("What did we miss?"):** Existing test suites validated DTO mappings and JS syntax via `node -c`, but did not assert cross-tab state transitions and filter reset lifecycles between Overview cards and All Tests header badges.
- **Resolution:** 
  1. Synchronized `activeWholeExecutionFilter` and `currentGlobalStatusBadgeFilter` in both `filterByGlobalStatusBadge` and `filterAllTestsByStatus`.
  2. Guarded `applyRunReportFilters()` against string value `'ALL'`.
  3. Synchronized Overview KPI card `.active-filter` classes when toggling badges from the All Tests tab.
  4. Bumped `report-manager.js` script version to `?v=1.1.2` in `index.html`.
- **Safety Net Added:** Added automated regression test in `StaticResourceSyntaxTest.java` verifying filter state transitions across Overview and All Tests tabs.
