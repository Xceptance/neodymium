# [DEF-20261110-0615] Event-Driven Playbook Mid-Lifecycle Recording Persistence and AI Reasoning Sanitization

- **Status:** `Resolved`
- **Opened:** 2026-11-10 05:40
- **Closed:** 2026-11-10 10:35
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** 
  1. Mock integration tests in `neodymium-e2e-tests` (such as `ClickIntegrationTest` and `OptionalIntegrationTest`) failed with `AssertionFailedError: Recorded playbook file should exist on disk` because the file was asserted mid-method before `@AfterEach` lifecycle teardown.
  2. Recorded playbooks contained raw localhost URLs (e.g. `http://localhost:8542/...`) inside `reasoning`, failing `AssertionFailedError: Recorded playbook should not contain any hardcoded localhost URLs`.
  3. LLM JSON responses with `actions` but no top-level `thought` dumped the entire raw JSON string into `step.reasoning`.
- **Root Cause:**
  1. `AgentToolLoopStep.extractThought` lacked inspection for `node.has("actions")`, returning the full JSON payload as thought text.
  2. `step.reasoning` bypassed `DefaultActionSanitizer.sanitizeText(thought, sessionData)`.
  3. `NeodymiumAiRunner` deferred `SessionFinishedEvent` to `@AfterEach`, whereas mock tests asserted disk artifacts mid-method. Attempting to eagerly persist inside `AiSession.execute` violated the observer pattern, created redundant double writes, and leaked partial recordings on failure in `LLM_RECORDING`.
  4. `BaseAiTest.getTestResourceFile` checked `e2eSrc.getParentFile().exists()`, misrouting non-existent file lookups to `./src/test/resources` instead of `neodymium-e2e-tests/src/test/resources` when running from monorepo root.
- **Detection Gap ("What did we miss?"):** Tests previously ran in monolithic reactor suites with pre-existing recordings, masking the timing of programmatic recording flushes and reasoning parameterization on single-shot mock responses.
- **Resolution:**
  1. Created `PlaybookRecordingFlushEvent implements ExecutionEvent` carrying the session's `ExecutionContext` to allow decoupled, event-driven mid-test flushes.
  2. Wired `PlaybookRecorder` to handle `PlaybookRecordingFlushEvent`, made file writing idempotent (skipping identical normalized content), and retained failure cleanup strictly under `FORCE_RECORDING`.
  3. Removed eager recorder invocation from `AiSession.execute(...)`.
  4. Updated `AgentToolLoopStep` to extract reasoning from the first action when only `actions` is returned, and sanitized all thoughts via `DefaultActionSanitizer`.
  5. Added `flushRecording(session)` to `BaseAiTest` and invoked it across the 18 mock integration tests.
  6. Replaced fragile `getParentFile().exists()` check in `BaseAiTest.getTestResourceFile` with monorepo root detection (`new File("neodymium-e2e-tests").isDirectory()`).
- **Safety Net Added:** Verified all 18 mock integration tests pass across both `FORCE_RECORDING` and `REPLAY_STRICT` invocations (`mvn test -pl neodymium-e2e-tests -Dtest="org.neodymium.ai.integration.mock.*Test"`).
