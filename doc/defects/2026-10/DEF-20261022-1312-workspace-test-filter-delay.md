# [DEF-20261022-1312] Workspace Active Test Filter Delay and Unfiltered Flash on Page Load

- **Status:** `Resolved`
- **Opened:** 2026-10-22 13:12
- **Closed:** 2026-10-22 13:20
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When opening or reloading the Aura Manager workspace view (`/`), if a test filter was active from a previous search or navigation, all test files were rendered unfiltered for hundreds of milliseconds before abruptly flashing and refreshing with the filtered subset.
- **Root Cause:** The active test filter query was persisted solely in client-side `sessionStorage` (`aura_test_search_query`). Because standard HTTP GET requests do not send `sessionStorage` to the server, `AuraTestDashboardController.renderDashboard` evaluated `query` as null and rendered `#yamlFileList` with all files. Subsequently, `loadFiles()` executed sequential asynchronous calls (`/api/files/json` followed by `htmx.ajax('/api/files/search')`) before swapping the DOM, causing a visible delay and double-render.
- **Detection Gap ("What did we miss?"):** Existing controller tests only exercised `renderDashboard` with explicit request parameters, missing the realistic user flow where page navigation occurs without URL parameters while an active filter is retained in client state.
- **Resolution:**
  1. Synchronized active test search query to cookie `aura_test_search_query` in `dashboard-editor.js`.
  2. Updated `AuraTestDashboardController` and `AuraTestFileController` to read `@CookieValue(value = "aura_test_search_query", required = false)` when the `q` request parameter is absent, pre-filtering the HTML on initial server render.
  3. Added synchronous instant DOM filtering (`applyInstantTestFilter`) in `dashboard-editor.js` and an inline DOM script in `test-selection.html` to eliminate any client-side rendering gap.
  4. Optimized `loadFiles(forceSwap)` to bypass redundant HTMX DOM swaps on initial page load.
- **Safety Net Added:**
  - `AuraTestDashboardControllerTest.testRenderDashboardWithCookieFallback`
  - `AuraTestFileControllerTest.testFilesListFragmentWithCookieFallback`
  - `StaticResourceSyntaxTest.testWorkspaceTestSearchInstantFilterAndCookiePersistence`
