# [DEF-20261113-0820] Attachment Lifecycle Leaks, Path Traversal Inconsistencies, and Streaming URL Gaps

- **Status:** `Resolved`
- **Opened:** 2026-11-13 06:10
- **Closed:** 2026-11-13 08:20
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework`
- **Symptom:** 
  1. `Neodymium.addAttachment` retained duplicate attachments if invoked multiple times with identical filenames.
  2. Test attachments leaked across test runs within thread-pooled runner threads due to incomplete thread context clearing in `Neodymium.clearThreadContext()`.
  3. Video streaming endpoints in the Aura Report UI attempted to load videos from `/runs/.../files/...` instead of the authenticated byte-range streaming endpoint `/api/runs/...`.
  4. Video player cards and attachments from previously selected tests suffered visual ghosting while fetching step details for newly clicked test runs in `report-manager.js`.
  5. Multi-dataset parameterized tests wrote all execution logs to a single shared `execution.log` overwriting prior dataset outputs.
  6. Path traversal attempts like `../../../passwd` or nested subpaths like `sub/dir/data.json` produced inconsistent sanitization filenames or leading underscores (`___passwd`).
  7. Opening attachments in separate browser windows failed when popup blockers were active, without a fallback navigation mechanism.
  8. Missing MIME content types for `.csv`, `.pdf`, and `.xml` caused Aura Manager report file serving to default to `application/octet-stream`.

- **Root Cause:**
  1. `Neodymium.addAttachment` unconditionally appended to `context.attachments` without deduplication by name or path.
  2. `Neodymium.clearThreadContext()` removed the context mapping from `CONTEXTS` but did not clear the `attachments` collection on the removed instance, leaving residual state accessible to thread-bound listeners.
  3. `report-manager.js` constructed video src URLs using a legacy non-existent path template (`/runs/{runId}/files/{testClass}/{videoUrl}`) instead of the byte-range streaming controller mapping `/api/runs/{runId}/{testClass}/{filename}`.
  4. Step fetching in `renderStepsForExecution` did not reset or hide the video player and attachment container before issuing the async fetch request for new execution details.
  5. `InteractiveConsoleEngine.saveLocalExecutionLog` wrote directly to `execution.log` rather than isolating logs per dataset execution (`execution-{index}.log`).
  6. `ExecutionAttachment.sanitizeAttachmentPath` executed path separator replacement (`/` and `\` to `_`) prior to stripping traversal dot sequences (`..`), converting `../` into `.._` which left orphan underscores upon subsequent removal.
  7. `openAttachmentWindow` invoked `window.open` without verifying whether the pop-up window handle was blocked or null.
  8. `AuraReportViewController.servePath` lacked content-type branch mappings for CSV, PDF, and XML file extensions.

- **Detection Gap ("What did we miss?"):**
  1. Existing attachment tests only asserted single attachment additions without multi-turn overwriting or thread-pool reuse tests.
  2. Video and attachment UI rendering tests previously relied on synthetic DOM mocks that did not simulate consecutive row switching or streaming URL resolution.
  3. Multi-dataset execution tests asserted final console JSON outputs but did not inspect disk file generation under `customDir/runFolder/testClass/` across sequential dataset iterations.

- **Resolution:**
  1. Updated `Neodymium.addAttachment` to remove existing entries matching name or path before appending new attachments.
  2. Updated `Neodymium.clearThreadContext()` to invoke `context.attachments.clear()`.
  3. Corrected video URL builder in `report-manager.js` to `/api/runs/{runId}/{testClass}/{filename}` and added byte-range friendly streaming handling.
  4. Added video player pause/src-clearing and attachment container hiding at the start of step loading in `report-manager.js`.
  5. Added `execution-{index}.log` dataset-indexed logging alongside fallback `execution.log` in `InteractiveConsoleEngine`.
  6. Refactored `ExecutionAttachment.sanitizeAttachmentPath` to strip leading traversal prefixes (`../`, `..\`) and collapse leading underscores while safely preserving file extensions and capping long stems.
  7. Added popup blocker detection in `openAttachmentWindow` falling back to `window.location.href = targetUrl`.
  8. Added MIME mappings for `text/csv; charset=utf-8`, `application/pdf`, and `application/xml; charset=utf-8` in `AuraReportViewController.servePath`.

- **Safety Net Added:**
  - `NeodymiumAttachmentTest.testDeduplicationOnSameName`: Verifies replacement and `openByDefault` updates on duplicate attachment names.
  - `NeodymiumAttachmentTest.testClearThreadContextPurgesAttachments`: Verifies total purge of attachments on context cleanup.
  - `NeodymiumAttachmentTest.testPathSanitization`: Asserts traversal stripping (`../../../passwd` -> `passwd`), subpath flattening (`sub/dir/data.json` -> `sub_dir_data.json`), Windows paths, dotfiles (`.env` -> `_env`), and stem capping.
  - `InteractiveConsoleEngineTest.testMultiDatasetExecutionLogIndexingIsolated`: Asserts per-dataset `execution-1.log` and `execution-2.log` isolation and JSON metadata attachment references.
  - `InteractiveConsoleEngineTest.testReconciliationWithoutStatusChangeFlushesUpdatedExecutionLog`: Asserts execution log flushing on test outcome reconciliation.
  - `StaticResourceSyntaxTest`: Validates JavaScript syntax and template correctness across all modified static resources.
