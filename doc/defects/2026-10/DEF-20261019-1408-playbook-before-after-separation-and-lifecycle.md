# [DEF-20261019-1408] Missing Block Separation and Lifecycle Semantics for Before and After Steps in Playbook Execution, Reports, and Interactive Mode

- **Status:** `Resolved`
- **Opened:** 2026-10-19 13:35
- **Closed:** 2026-10-19 14:10
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In the test editor, users can define steps in `before` and `after` blocks, but during execution all steps landed in the core `steps` block in both preliminary disk reports (HTML/Markdown/JSON) and the interactive console state. Furthermore, if a step in the `before` block failed, core execution was not cleanly prevented or skipped, and if a test step failed, teardown steps in the `after` block were skipped instead of being guaranteed to execute like test fixture teardowns.
- **Root Cause:**
  1. `YamlPlaybookParser` previously inlined all `before` and `after` alias blocks directly into `Playbook.steps` without setting block ownership (`step.setBlock(...)`) or retaining separate collections.
  2. `Playbook` lacked discrete `beforeSteps` and `afterSteps` storage and builder methods.
  3. `TestExecutionReport.ReportStepEntry` and `PreliminaryReportListener` did not track or serialize the `block` attribute.
  4. `InteractiveStateBuilder` dumped all steps into a single unpartitioned array rather than partitioning steps and report entries into `blocks: { before: [...], steps: [...], after: [...] }`.
  5. `NeodymiumAiRunner` and `AiSession` executed playbooks as a monolithic step stream rather than enforcing standard testing lifecycle semantics (`try { runBefore(); runCore(); } finally { runAfter(); }`) with suppressed exception chaining (`addSuppressed`) when both core and teardown fail.
  6. `StateMachineRunner.markRemainingStepsAsSkipped` indiscriminately marked all remaining steps as `SKIPPED` upon failure, including `after` steps, rather than keeping `after` steps pending for teardown execution.
  7. A `return;` statement inside the `finally` block of `StateMachineRunner.run()` executed when `SessionFinishedEvent` was deferred, which silently discarded and swallowed thrown exceptions from intermediate blocks.
- **Detection Gap ("What did we miss?"):**
  Previous tests for before/after aliases in `YamlPlaybookParserTest` only asserted the total step count against `playbook.getSteps()` and did not verify block tagging, step partitioning, lifecycle execution abort/guarantee semantics, or interactive state serialization.
- **Resolution:**
  1. Added `block` property (`"before"`, `"steps"`, `"after"`) to `PlaybookStep` and `ReportStepEntry`.
  2. Added `beforeSteps` and `afterSteps` collections, accessors, and builder methods to `Playbook`.
  3. Updated `YamlPlaybookParser` to parse and partition steps into `beforeSteps`, `steps`, and `afterSteps`, tagging `step.setBlock(...)` on top-level and inlined/substep items.
  4. Updated `PreliminaryReportListener` to propagate block ownership to `ReportStepEntry`.
  5. Updated `InteractiveStateBuilder` to structure interactive JSON state with `blocks: { before: [...], steps: [...], after: [...] }`.
  6. Updated `HtmlReportGenerator` and `MarkdownReportGenerator` to render block badges and visual sections.
  7. Updated `StateMachineRunner` to preserve `afterSteps` unskipped on test failure, properly defer `SessionFinishedEvent`, and removed the `return;` statement in `finally` that swallowed exceptions.
  8. Implemented lifecycle execution with `try { before(); core(); } finally { after(); }` and suppressed exception chaining in both `NeodymiumAiRunner` and `AiSession`.
- **Safety Net Added:**
  `org.neodymium.ai.runner.PlaybookLifecycleExecutionTest`:
  - `testBeforeStepFailureAbortsCoreStepsAndExecutesAfterSteps`: Verifies that a failure in a before step aborts core steps (marked SKIPPED) and executes after steps (marked SUCCESS).
  - `testCoreStepFailureExecutesAfterSteps`: Verifies that a failure in a core step still executes after steps.
  - `testSuppressedExceptionHandlingWhenBothCoreAndAfterFail`: Verifies that when both core and after blocks fail, after block failure is chained as a suppressed exception to the primary core failure.
  - `testReportAndInteractiveStatePreserveBlockSeparation`: Verifies that report entries and interactive console JSON state retain discrete `before`, `steps`, and `after` blocks.
  `org.neodymium.ai.playbook.YamlPlaybookParserTest`:
  - `testParsePlaybookWithBeforeAndSteps`
  - `testParsePlaybookWithBeforeStepsAndAfter`
  - `testParsePlaybookWithAllBeforeAliases`
  - `testParsePlaybookWithAllAfterAliases`
  - `testParsePlaybookWithStructuredActionsInBeforeAndAfter`
  - `testParsePlaybookWithIncludesInBeforeAndAfter`
  - `testParsePlaybookWithExplicitBlockKeys`
  - `testParseCompanionJsonRecordingWithBlockPartitioning`
