# [DEF-20261021-1138] Manager Workspace Test Case Filter Not Persistent Across Navigation and File Operations

- **Status:** `Resolved`
- **Opened:** 2026-10-21 11:38
- **Closed:** 2026-10-21 11:50
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Aura Manager workspace, test case and step fragment search filtering (`#testSearchInput`) either disappears after navigating away and returning, or becomes decoupled from the file list (input shows the search term, but all test cases are displayed after closing the editor, creating/renaming/deleting tests, or triggering file refreshes).
- **Root Cause:** 
  1. `test-selection.html` does not persist `#testSearchInput` value in `sessionStorage`.
  2. `loadFiles()` in `dashboard-editor.js` issues `GET /api/files/list` unconditionally rather than checking if `#testSearchInput` has an active search filter.
  3. `closeEditor()` in `dashboard-editor.js` invokes `loadFiles()` and clicks `refreshFilesBtn`, swapping unfiltered HTML into `#yamlFileList` while leaving `#testSearchInput` populated.
  4. `#yamlFileList` in `test-selection.html` listens to `refreshFiles from:body` via `hx-get="/api/files/list"` without `hx-include="#testSearchInput"`.
  5. There is no clear filter button to quickly reset the filter and its persistence.
- **Detection Gap ("What did we miss?"):** `AuraTestFileController` had no unit tests verifying query parameter handling on `/api/files/list`, and no tests verified client-side search query retention across DOM file list swap events and navigation cycles.
- **Resolution:** 
  1. Added session persistence (`sessionStorage.getItem('aura_test_search_query')`) and `oninput` handler (`onTestSearchInput`) for `#testSearchInput`.
  2. Added `#clearTestSearchBtn` (`clearTestSearch()`) to easily reset search input and session state.
  3. Updated `loadFiles()` to check for an active search filter and call `/api/files/search?q=...` instead of unconditionally wiping the list with `/api/files/list`.
  4. Added `hx-include="#testSearchInput"` to `refreshFilesBtn` and `#yamlFileList` (`refreshFiles from:body`) so file operations preserve active search queries.
  5. Updated `AuraTestFileController.getFilesListFragment` to accept optional `q` query parameter, filtering test and step files when non-blank.
  6. Updated `AuraTestDashboardController.renderDashboard` to support `q` query parameter for direct URL deep-linking with active filters.
  7. Added `initTestSearchFilter()` and `pageshow` listener to restore search filter on page load and back/forward browser navigation (BFcache).
- **Safety Net Added:** 
  1. `AuraTestFileControllerTest`: `testGetFilesListFragmentWithoutQueryReturnsFullList`, `testGetFilesListFragmentWithQueryReturnsFilteredList`, `testSearchFilesReturnsFilteredList`.
  2. `AuraTestDashboardControllerTest`: `testRenderDashboardWithSearchQuery`.
  3. `StaticResourceSyntaxTest`: `testManagerWorkspaceFilterPersistenceContracts`. 
