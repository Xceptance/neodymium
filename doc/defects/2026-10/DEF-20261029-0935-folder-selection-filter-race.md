# [DEF-20261029-0935] Folder Selection Visual Inconsistency and Filter Race Condition in Manager

- **Status:** `Resolved`
- **Opened:** 2026-10-29 09:35
- **Closed:** 2026-10-29 10:11
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager, selecting a folder after applying a search filter caused the topmost hierarchy level (`tests`) and subfolders to flicker between checked (`✓`) and indeterminate (`-`), or intermittently display the topmost level as fully selected.
- **Root Cause:**
  1. Server Search DOM Pruning vs Visibility: When searching/filtering (e.g. `q=ifolor`), `/api/files/search` returns HTML containing only matching files. Non-matching files across the project (`tests/cart`, etc.) are completely absent from the DOM, rather than merely hidden with `display: none`.
  2. In `updateParentFolderCheckboxes` and `toggleFolderCheckboxesInstant`, client-side logic evaluated checkbox states solely across DOM elements (`querySelectorAll('.file-container')`). Because all files present in the DOM were checked, the client optimistically set ancestor folders (such as `tests`) to `checked = true` (`✓`), only to have the server's asynchronous response (`folderStatesUpdated`) downgrade them to `indeterminate = true` (`-`).
  3. In `toggleFolderCheckboxesInstant`, folders clicked while filtered blindly evaluated `hasHiddenFiles` across DOM elements, which returned `false` when filtered-out files were not in the DOM at all.
  4. Debounced `/api/files/search` HTMX swaps returning after `/api/queue/toggleFolder` re-injected stale `data-folder-states` from the pre-click queue state.
  5. In `AuraTestQueueController.getFolderSelectionStates()`, whole-file wildcard selections (`id == ""` or `null`) were not detected for files with datasets, undercounting selected datasets in folder aggregates.
  6. Initial Page Load Model Deficiency: `AuraTestDashboardController.renderDashboard()` did not inject `folderTotalDatasetsJson` into the model on initial dashboard load (it was only present on `AuraTestFileController.populateFileModel`), leaving `th:data-folder-totals` empty on initial render. This caused `window.currentFolderTotals` to remain empty until a file search occurred, falling back to local DOM counts and falsely calculating ancestor folders as fully selected.
  7. Redundant Full-Disk Scans and YAML Re-Parsing Latency: `AuraFileService.getYamlFilesList()` performed full disk scans and SnakeYAML re-parsing of every YAML file on every invocation with zero caching. Furthermore, `AuraTestQueueController.toggleFolderQueue()` invoked `getYamlFilesList()` three times in a single request: (1) filtering matching files, (2) `getFolderSelectionStates()`, and (3) `getFolderTotalDatasets()`, causing response times of ~1.5s for a single folder toggle.
- **Detection Gap ("What did we miss?"):** Existing controller tests verified `getFolderSelectionStates()` using individual dataset selections, but omitted whole-file wildcard selections, lacked assertions for `folderTotalDatasetsJson` on `AuraTestDashboardController`, and did not benchmark multi-pass YAML disk re-parsing overhead under folder toggling.
- **Resolution:**
  1. Exposed `getFolderTotalDatasets()` and `getFolderTotalDatasetsJson()` on `AuraTestQueueController` returning project-wide dataset counts per folder (e.g. `{"tests": 50, "tests/ifolor": 2}`).
  2. Injected `folderTotalDatasetsJson` via `AuraTestFileController.populateFileModel` and `AuraTestDashboardController.renderDashboard()` and rendered it on `#sidebarTestsContainer` via `th:data-folder-totals`.
  3. Dispatched `folderTotalsUpdated` event in `HX-Trigger` alongside `folderStatesUpdated`.
  4. Implemented `countFolderSelectedDatasets`, `countFolderTotalDatasets`, and `applyFolderCheckboxState` in `dashboard-editor.js` to compute folder states dynamically:
     - `selectedCount === 0`: `checked = false, indeterminate = false`
     - `selectedCount >= totalCount`: `checked = true, indeterminate = false`
     - `else`: `checked = false, indeterminate = true`
     Where `selectedCount` accurately sums checked checkboxes in the DOM plus queued datasets for project files filtered out of the DOM.
  5. Added defensive guard in `dashboard-editor.js` (`updateFolderCheckboxState`): if a filter is active and `knownTotal === 0` while `selectedCount > 0`, default ancestor checkboxes to `indeterminate` rather than falsely assuming `checked = true`.
  6. Refactored `toggleFolderCheckboxesInstant`, `updateParentFolderCheckboxes`, and `updateAllFolderCheckboxes` to use `updateFolderCheckboxState`.
  7. Added client-side state caching in `window.currentFolderSelectionStates` and `window.currentFolderTotals` in `dashboard-core.js` and `dashboard-editor.js`.
  8. Updated `AuraTestQueueController.java` (`getFolderSelectionStates` and `getPartiallySelectedFileKeys`) to evaluate `hasWholeFileSelection`.
  9. Implemented in-memory YAML file DTO caching (`yamlFilesCache` keyed by path, length, and `lastModified`) in `AuraFileService.getYamlFilesList()` with automated cache invalidation on file mutation operations (`saveYamlFileContent`, `createYamlFile`, `deleteYamlFile`, `renameOrMoveFile`), dropping scan latency from ~500ms to < 2ms.
  10. Overloaded `AuraFileService.getFilteredYamlFilesList(List<YamlFileDto>, String)` to allow in-memory filtering.
  11. Overloaded `getFolderSelectionStates(List<YamlFileDto>)`, `getFolderTotalDatasets(List<YamlFileDto>)`, and `setFolderStatesTrigger(response, List<YamlFileDto>)` in `AuraTestQueueController`, consolidating folder toggles to a single scan.
- **Safety Net Added:**
  - Added `@Test` `testGetFolderTotalDatasets` in `AuraTestQueueControllerTest.java`.
  - Added `@Test` `testGetFolderSelectionStatesWithWholeFileSelection` in `AuraTestQueueControllerTest.java`.
  - Added `@Test` `testPopulateFileModelIncludesFolderTotals` in `AuraTestFileControllerTest.java`.
  - Added `@Test` `testRenderDashboardFullPage` assertion for `folderTotalDatasetsJson` in `AuraTestDashboardControllerTest.java`.
  - Added `@Test` `testFilteredYamlFilesListWithListArg` and `testYamlFilesListCachingAndInvalidation` in `AuraFileServiceTest.java`.
  - Verified static resource parsing via `StaticResourceSyntaxTest.java`.
