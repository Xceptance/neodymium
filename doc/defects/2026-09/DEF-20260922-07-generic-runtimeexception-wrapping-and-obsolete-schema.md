# [DEF-20260922-07] Generic RuntimeException Wrapping and Obsolete Schema Version Re-Persistence

- **Status:** Resolved
- **Opened:** 2026-09-22
- **Closed:** 2026-09-22
- **Component:** `neodymium-core` (`IncompatiblePlaybookSchemaException`, `PlaybookToolReplayer`, `ExecuteActionsStep`, `StateMachineRunner`, `AiSession`)
- **Scope:** `Framework`
- **Symptom:** When replaying a playbook with an obsolete or incompatible schema version (e.g., version `3.0`), the runner failed with generic `java.lang.RuntimeException: org.neodymium.ai.pipeline.ConclusiveFailureException` instead of a specialized typed exception with structured version metadata. Additionally, during self-healing runs, `AiSession` copied obsolete schema versions from loaded recorded steps onto active steps, perpetuating outdated schemas.
- **Root Cause:**
  1. Schema version mismatches in `PlaybookToolReplayer` threw `ConclusiveFailureException` directly with a formatted string; `ExecuteActionsStep` wrapped it in `RuntimeException`; and `StateMachineRunner` re-threw the outer `RuntimeException` without unwrapping `PipelineException` causes.
  2. `AiSession.java` unconditionally copied `recorded.getSchemaVersion()` onto the active `PlaybookStep`, overwriting `CURRENT_SCHEMA_VERSION`.
- **Detection Gap ("What did we miss?"):** Absence of negative unit tests asserting the exact exception type and structured fields when loading legacy playbooks.
- **Resolution:**
  1. Introduced `IncompatiblePlaybookSchemaException` extending `PipelineException` with `getRecordedVersion()` and `getExpectedVersion()`.
  2. Updated `PlaybookToolReplayer` to throw `IncompatiblePlaybookSchemaException`.
  3. Updated `ExecuteActionsStep` to throw `IncompatiblePlaybookSchemaException` directly without wrapping in `RuntimeException` and update `schemaVersion` to `CURRENT_SCHEMA_VERSION` on self-healing.
  4. Updated `StateMachineRunner` to unwrap `t.getCause() instanceof PipelineException` to ensure typed exceptions bubble up cleanly.
- **Safety Net Added:** Created `IncompatiblePlaybookSchemaExceptionTest.java` verifying exception hierarchy, version accessors, and `PlaybookToolReplayer` validation.
