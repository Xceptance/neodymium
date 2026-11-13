# [DEF-20261113-0955] Multi-Dataset Playbook Attachment Isolation, Redundant Step Disk I/O, and Attachment Path NPE Guards

- **Status:** `Resolved`
- **Opened:** 2026-11-13 09:00
- **Closed:** 2026-11-13 09:55
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Parameterized dataset 2 executions referenced `playbook.yaml` (dataset 1's playbook) instead of their isolated `playbook-2.yaml` in console execution attachments.
  2. Every test step triggered redundant disk overwrites of static playbooks, logs, and video recording files.
  3. Attachments missing `path` in JSON threw `NullPointerException` during report data loading in Aura Manager, and attachment resolution did not ensure confinement within candidate directories.

- **Root Cause:**
  1. `InteractiveConsoleEngine.saveLocalExecutionLog` hardcoded `new File(structuredDir, "playbook.yaml")` when populating `attachmentsArray` rather than resolving `playbook-{index}.yaml`.
  2. `InteractiveConsoleEngine.saveLocalExecutionLog` used unconditional `Files.writeString` and `Files.copy` on every step execution rather than checking file content or timestamp/size modifications.
  3. `AuraReportDataService.resolveAttachmentsForDto` dereferenced `att.getPath()` without null checks, and resolved paths without checking `fPath.startsWith(cDir)`.

- **Detection Gap ("What did we miss?"):**
  1. Multi-dataset tests asserted `execution-2.log` isolation but did not inspect `playbook.yaml` attachment mapping in `console-execution-2.json`.
  2. Unit tests previously invoked `saveLocalExecutionLog` once at completion, bypassing multi-step disk I/O frequency evaluation.
  3. Synthetic test fixtures always included non-null `path` properties on all mock attachment JSON definitions.

- **Resolution:**
  1. Added dataset-indexed playbook resolution (`playbook-{index}.yaml` with fallback to `playbook.yaml`) in `InteractiveConsoleEngine`.
  2. Introduced `writeStringIfChanged` and applied `copyIfChanged` across all log, playbook, and video writes in `InteractiveConsoleEngine`.
  3. Added null guards, `att.getName()` fallback, and `fPath.startsWith(cDir)` boundary enforcement in `AuraReportDataService`.

- **Safety Net Added:**
  - `InteractiveConsoleEngineTest.testMultiDatasetPlaybookIndexingIsolated`: Verifies dataset 2 isolates `playbook-2.yaml` and maps `console-execution-2.json` to its dataset-specific playbook.
  - `AuraReportViewControllerTest`: Verifies report controller and data resolution handling.
