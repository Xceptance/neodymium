# [DEF-20261104-1310] Single-Pass Execution Matching Hijacks Multi-Dataset Variations via Loose Folder Prefix

- **Status:** `Resolved`
- **Opened:** 2026-11-04 13:10
- **Closed:** 2026-11-04 13:12
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager run reports containing test classes with multiple parameterized datasets or methods in the same class folder, inspecting any secondary dataset displays the steps and screenshots of the first dataset in that folder.
- **Root Cause:** In `AuraReportDataService.getExecutionDetails()`, the search loop combined exact criteria (`execKey`, `fullKey`) and fuzzy prefix matching (`(!parentFolder.isEmpty() && executionId.startsWith(parentFolder + "#"))`) in a single pass with `||`. When a class folder contained multiple execution files (e.g. `console-execution-1.json` for Dataset A and `console-execution-2.json` for Dataset B), querying Dataset B evaluated `startsWith(parentFolder + "#")` to true on File 1 before File 2 was ever reached, prematurely returning Dataset A. Furthermore, because file traversal order varies across filesystems (ext4 inode order vs NTFS alphabetical), this priority inversion introduced platform-dependent non-determinism.
- **Detection Gap ("What did we miss?"):** Previous unit tests for execution resolution in `RunStorageSyncServiceTest` used test fixtures containing only a single execution file per folder, masking the greedy multi-file collision behavior.
- **Resolution:**
  1. Refactored `AuraReportDataService.getExecutionDetails()` into a Two-Pass search architecture:
     - **Pass 1:** Scans all files exclusively for exact matches, checking both FQCN and `simpleClass` keys (`simpleFullKey`, `simpleExecKey`, `simpleExecKeyWithId`, `simpleClassMethodBrowser`, `id`, `parentWithId`, `parentWithFileName`). Returns immediately upon finding an exact match.
     - **Pass 2:** Executed only if Pass 1 found zero exact matches across the entire run. Allows folder-prefix fallback (`executionId.startsWith(parentFolder + "#")`) only if the parent folder contains a single execution file (`classFileCount == 1`), refusing ambiguous prefix matching on multi-file folders.
  2. Extracted `readJsonNodeWithRetry` helper method to eliminate duplicate retry logic.
- **Safety Net Added:** Added `@Test` `RunStorageSyncServiceTest.testMultiDatasetExecutionResolutionInSharedFolderDoesNotCrossTalk` creating two distinct datasets in a shared test folder, explicitly verifying that simple-name queries for Dataset B resolve Dataset B without hijacking, and asserting that ambiguous prefix queries on multi-file folders return null instead of guessing.
