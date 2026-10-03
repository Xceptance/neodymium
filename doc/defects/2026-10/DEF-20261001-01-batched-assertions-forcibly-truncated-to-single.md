# [DEF-20261001-01] Batched Assertions Forcibly Truncated to Single Call per Turn Causing Step Token Budget Exhaustion

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Compound verification steps containing multiple assertions (e.g. verifying 10 header elements in a single step) fail with `Token budget exceeded for step: Total tokens consumed (101110) exceeded configured step token budget (100000)` after thrashing across 11 turns, despite the LLM correctly proposing all assertions in Turn 1.
- **Root Cause:** `AgentToolLoopStep` restricted cohesive batch execution exclusively to form inputs via `isCohesiveFormInputBatch`. When the LLM proposed multiple `assert_*` calls alongside `complete_step`, the engine evaluated the batch as invalid, executed only the first call (`assert_title`), and discarded the remaining 13 calls to "prevent stale DOM errors" (even though assertions are read-only and non-navigating). This forced the agent into single-call turns, triggered DOM amnesia and `query_dom` fallbacks, and exceeded the token budget.
- **Detection Gap ("What did we miss?"):** `AgentToolLoopStepTest.testCohesiveFormInputBatchExecutesAllInputsInSingleTurn` verified cohesive form input batches (`fill`), but there were no tests validating cohesive assertion batches (`assert_*`), and `testBatchedToolCallsTruncatedToSingleActionPerTurn` assumed any non-form batch should be truncated.
- **Resolution:** Replaced `isCohesiveFormInputBatch` with `isCohesiveBatch` in `AgentToolLoopStep.java`, recognizing both form inputs (`isFormInputAction`) and assertions (`isAssertionTool`) as safe batchable operations. Added synthetic `SKIPPED` handling for interrupted batches and updated System Prompt Rule 4 to guide cohesive multi-assertion generation.
- **Safety Net Added:** Added `testCohesiveAssertionBatchExecutesAllAssertionsInSingleTurn` and `testCohesiveAssertionBatchInterruptedOnAssertionFailure` in `AgentToolLoopStepTest.java`.
