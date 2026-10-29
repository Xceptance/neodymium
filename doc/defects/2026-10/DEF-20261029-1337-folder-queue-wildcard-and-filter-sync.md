# [DEF-20261029-1337] Folder Queue Wildcard Selection Inconsistency and Filter Leak in Manager

- **Status:** `Resolved`
- **Opened:** 2026-10-29 13:37
- **Closed:** 2026-10-29 13:42
- **Component:** `aura-manager` & `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  1. Toggling a folder containing files already queued as whole-file wildcards (`id == ""` or `null`) appended duplicate dataset entries instead of toggling off or recognizing the selection.
  2. Toggling a folder while filtered by a specific dataset label queued all datasets in the file, including non-matching datasets that were hidden in the client UI.
- **Root Cause:**
  1. In `AuraTestQueueController.toggleFolderQueue()`, dataset matching evaluated `d.id.equals(item.id)` without checking for whole-file wildcard selections (`item.id == null || item.id.isBlank()`).
  2. `toggleFolderQueue()` iterated over all datasets in matching files unconditionally without checking whether each dataset matched the active query when the file path itself did not match.
  3. `AuraTestQueueController.toggleFolderQueue()` called single-argument `fileService.getFilteredYamlFilesList(query)` instead of `getFilteredYamlFilesList(allFiles, query)`.
  4. `AuraFileService.updateFragmentReferences()` rewrote YAML files on disk without clearing `yamlFilesCache`.
- **Detection Gap ("What did we miss?"):** Existing tests in `AuraTestQueueControllerTest` tested `toggleFolderQueue()` with single files or clean queues, but omitted pre-existing whole-file wildcard selections in the queue, and did not test folder toggles with dataset-specific search queries.
- **Resolution:**
  1. Updated `AuraTestQueueController.toggleFolderQueue()` to check for whole-file wildcard selections in both selection counting and dataset deduplication.
  2. Enforced dataset-level query matching in `toggleFolderQueue()` when the file path does not contain the active query.
  3. Switched `toggleFolderQueue()` to use `fileService.getFilteredYamlFilesList(allFiles, query)`.
  4. Added `clearYamlFilesCache()` to `AuraFileService.updateFragmentReferences()` when files are updated, and converted `CachedYamlEntry` to a JDK 21 record.
- **Safety Net Added:**
  - Added `@Test` `testToggleFolderQueueWithExistingWholeFileSelection` in `AuraTestQueueControllerTest`.
  - Added `@Test` `testToggleFolderQueueWithDatasetSpecificFilter` in `AuraTestQueueControllerTest`.
  - Added `@Test` `testUpdateFragmentReferencesInvalidatesCache` in `AuraFileServiceTest`.
