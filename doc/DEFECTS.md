# Defect Log & Post-Mortem Registry

This document tracks diagnosed defects, regressions, and behavioral bugs encountered during development and testing across Neodymium, its test suites, and connected Systems Under Test (SUTs).

The objective is to maintain an actionable learning record: to understand **why** defects occurred, identify detection gaps (**"what did we miss?"**), and ensure safety nets (regression tests, linters, or architectural assertions) prevent recurrence.

---

## Logging Guidelines & Criteria

### When to Log a Defect (In Scope)
- **Framework Regressions & Logic Bugs**: Any semantic bug in Neodymium core, AI engine, state machine, runners, parsers, or reporters.
- **Test Harness / Fixture Failures**: Faulty assertions, broken test setups, incorrect wait conditions, or selector fragility that caused false positives or false negatives.
- **SUT Behavioral Defects**: Confirmed functional, visual, or layout defects detected in the System Under Test (e.g., Verla demo store).
- **Silent Failures or Masked Exceptions**: Situations where errors were swallowed or misclassified.

### When NOT to Log (Out of Scope)
- **Normal TDD Red Phase**: Expected test failures during active test-first development prior to implementing the feature.
- **In-Progress Compilation / Syntax Typos**: Errors resolved during the immediate editing cycle.
- **Transient External Outages**: Temporary network loss, upstream LLM provider 503/429 quota limits, or local OS process termination.

---

## Defect Entry Template

When recording a defect, add a new entry directly under the [Active Defect Records](#active-defect-records) section in **reverse chronological order** (newest entries first).

```markdown
### [DEF-YYYYMMDD-01] Concise Description of Defect
- **Date:** YYYY-MM-DD
- **Component:** `neodymium-core` / `aura-visual` / `playbook-engine` / `verla-fixture` / etc.
- **Scope:** `Framework` | `Test/Harness` | `SUT`
- **Symptom:** Observed failure, error message, or unexpected behavior.
- **Root Cause:** Technical explanation of why the defect occurred.
- **Detection Gap ("What did we miss?"):** Why existing unit tests, linters, or type systems failed to catch this earlier.
- **Resolution:** Summary of code changes made to resolve the issue.
- **Safety Net Added:** Reference to the regression test, assertion, or linter rule preventing recurrence.
```

---

## Active Defect Records

### [DEF-20261001-01] Test Run Storage Directory Contains Duplicate Dummy console-execution-1.json Files Beside Higher-Indexed Files
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`InteractiveConsoleEngine`, `AuraInteractiveService`, `AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** Run storage folders for subsequent test classes in a batch contain both `console-execution-1.json` (a dummy 0-step fallback file) and `console-execution-3.json` (the actual test log).
- **Root Cause:** `InteractiveConsoleEngine` and `AuraInteractiveService` used global counters across all test classes in a batch run to index `console-execution-*.json` files. Subsequent test classes received indexes > 1 (e.g. 3). Later, `AuraQueueService` checked for index 1 in the class folder, saw it missing, and created a dummy fallback `console-execution-1.json`. Furthermore, `markRunningOrMissingExecutionsAsSkipped` failed to mark existing completed execution snapshots as processed, triggering fallback creation.
- **Detection Gap ("What did we miss?"):** Tests verified multi-dataset indexing within a single test class, but did not assert per-class file indexing boundaries across multi-class batch execution.
- **Resolution:** Refactored `InteractiveConsoleEngine` and `AuraInteractiveService` to map execution indexes per test class folder. Updated `AuraQueueService.markRunningOrMissingExecutionsAsSkipped` to recognize existing execution files with final statuses.
- **Safety Net Added:** Added unit tests verifying per-class execution file indexing in `InteractiveConsoleEngineTest`.

### [DEF-20261001-02] Queue Execution Report Total Duration Displays Multimillion Minutes Due to Unfiltered Zero Timestamps
- **Date:** 2026-10-01
- **Component:** `aura-manager` (`RunReportDto`)
- **Scope:** `Framework`
- **Symptom:** Total execution duration for queue runs (`run_20261001_12564`) showed invalid values such as `29847542 min 38 s` instead of actual wall-clock execution time (~4-5 min).
- **Root Cause:** `RunReportDto.getTotalDurationMs()` evaluated `(maxStartMs - minStartMs) + latestExec.getDurationMs()` without filtering out uninitialized or missing start timestamps (`startMs = 0L`, Jan 1 1970). When an uninitialized execution stub or execution snapshot with `0L` timestamp was present, `minStartMs` was set to `0L`, causing the subtraction `maxStartMs - 0L` to evaluate to the current Epoch timestamp (~1.79x10^12 ms = 29,847,542 minutes).
- **Detection Gap ("What did we miss?"):** Existing unit tests for `RunReportDto` verified total duration only with valid mock timestamps or single executions, missing test coverage for queue runs where some execution snapshots have `0L` start timestamps.
- **Resolution:** Updated `RunReportDto.getTotalDurationMs()` to filter out invalid start timestamps (`startMs <= 0L`) when calculating wall-clock spans and fall back to the sum of test execution durations when valid start timestamps are missing or insufficient.
- **Safety Net Added:** Added unit test `testGetTotalDurationMs_ignoresZeroTimestampAndFallsBackToSum` in `RunReportDtoTest.java`.

### [DEF-20261001-01] Conditional Include Main Step Recorded as Substep in Console Execution Reports
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`PreliminaryReportListener` / `ExecuteActionsStep` / `InteractiveStateBuilder`)
- **Scope:** `Framework`
- **Symptom:** In `console-execution-*.json` and execution reports for steps with conditional includes (e.g. `Add product to cart:` with child `- If (condition) then _include: ...`), the main step's conditional instruction was recorded as `Substep 0` inside its own `subSteps` array alongside the actual included steps, and `subSteps` of included steps contained nested duplicates of the main step.
- **Root Cause:** When a parent step has a child step containing `_include:`, `PreliminaryReportListener` pre-populated `subSteps` with the conditional instruction as `Substep 0` prior to runtime include expansion. When `ExecuteActionsStep` and `IncludeAction` ran, `stepStats.getSubStats()` contained the container step, causing `mergeStepStats` to overwrite `Substep 0` with the conditional instruction.
- **Detection Gap ("What did we miss?"):** Existing tests for `IncludeAction` verified step execution order and execution results, but did not assert that the report's `subSteps` array excludes the conditional include step itself.
- **Resolution:** Updated `ExecuteActionsStep` to extract effective leaf sub-steps when populating sub-stats, updated `PreliminaryReportListener` to clean up intermediate container/include steps from `subSteps`, and updated `InteractiveStateBuilder` to filter out include container instructions during subStep serialization.
- **Safety Net Added:** Added unit test `testConditionalIncludeSubStepsExcludesMainStep` in `SubStepReportingAndScopingTest.java`.

### [DEF-20260930-07] Successful Executions Overwritten to Failed and SLF4J Warnings Extracted as Process Errors
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** Successful test executions with completed steps (`totalSteps > 0`) in a multi-dataset batch were overwritten to status `"failed"` with SLF4J warning messages (e.g., `WARN ... BiDiException`) listed as `failureReason`.
- **Root Cause:**
  1. `AuraQueueService.executeQueue` updated all non-failed execution JSONs to `"failed"` when `isFailedRun` was true, missing the `isZeroStep` check to distinguish unexecuted tests from completed successful ones.
  2. `extractSubprocessErrorMessage` checked `line.contains("WARN:")` with a required colon, failing to match SLF4J log lines (`[main] WARN ...`) which omit the colon, allowing lines containing `BiDiException:` to be parsed as error messages.
- **Detection Gap ("What did we miss?"):** Tests did not assert that multi-dataset execution batches containing both a failing test and a passing test retain `"passed"` status for the completed test, nor did tests cover SLF4J `WARN` log formats without trailing colons.
- **Resolution:** Re-enforced `isZeroStep` check in `AuraQueueService` when updating non-failed execution states on `isFailedRun`, and updated `extractSubprocessErrorMessage` to inspect `WARN` and `WARNING` without requiring trailing colons.
- **Safety Net Added:** Added unit tests in `AuraQueueServiceTest.java` for SLF4J `WARN` filtering and zero-step status update bounds.

### [DEF-20260930-06] Dynamically included playbook steps unlinked as sub-steps of active include step
- **Date:** 2026-09-30
- **Component:** `org.neodymium.ai.executor.selenide.plugins.IncludeAction`
- **Scope:** `Framework`
- **Symptom:** Playbook steps dynamically included at runtime via `include(...)` (such as inside conditional `If ... _include:` branches) executed as flat top-level steps on the execution context stack without being linked to the active `include` parent step in execution reports or console execution logs.
- **Root Cause:** In `IncludeAction.java`, parsed steps from included playbooks were mapped to pipeline steps and pushed onto `ExecutionContext.runStack` without setting `subStep.setParent(currentStep)` or registering them under `currentStep.getSubSteps()`.
- **Detection Gap ("What did we miss?"):** Tests for `IncludeAction` verified that included steps executed on the browser, but did not assert that dynamically included steps were attached as `subSteps` of the active `currentStep` in `ExecutionContext` and execution reports.
- **Resolution:** Updated `IncludeAction.java` to retrieve the active `currentStep` from `ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP`, set `subStep.setParent(currentStep)` for each included step, and populate `currentStep.getSubSteps()`.
- **Safety Net Added:** Verified dynamic sub-step linking across `PreliminaryReportListenerTest` and `SubStepReportingAndScopingTest`.


### [DEF-20260930-05] IllegalArgumentException on natural language step list items containing colons or hints
- **Date:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks with natural language YAML list steps containing colons within step text (such as `- Generate random email address (hint: use java method)`) fails with `java.lang.IllegalArgumentException: Invalid playbook step format in file: ... Expected string step, 'include' map, or 'instruction' map, but found map keys: [Generate random email address (hint]`.
- **Root Cause:** SnakeYAML parses list items containing `: ` (`- key: value`) into single-entry `Map` objects. `YamlPlaybookParser.parseStepBlock` rejected single-entry maps whose scalar values did not contain an `include` keyword, failing to recognize natural language step instructions containing colons (such as parenthetical hints `(hint: ...)` or formatted text `Label: text`).
- **Detection Gap ("What did we miss?"):** Unit tests only tested simple string list steps without inline colons or parenthetical hints containing `: `.
- **Resolution:** In `YamlPlaybookParser.java`, updated single-entry map scalar handling in `parseStepBlock` to reconstruct `key + ": " + value` into a full step instruction for any single-entry map in a step list, while maintaining strict `IllegalArgumentException` validation for invalid multi-key step maps.
- **Safety Net Added:** Added unit test `testParseYamlListStepWithParentheticalHintColon()` in `YamlPlaybookParserTest.java` validating natural language steps with colons in parenthetical hints or step text.

### [DEF-20260930-04] IllegalArgumentException on YAML list steps containing colons or inline includes
- **Date:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks containing list steps with colons in step text (such as `- ... and _include: fragment.steps`) fails with `java.lang.IllegalArgumentException: Invalid playbook step format in file: ... Expected string step, 'include' map, or 'instruction' map, but found map keys: [...]`.
- **Root Cause:** SnakeYAML parses list items containing `: ` into single-entry `Map` objects. In `YamlPlaybookParser.parseStepBlock`, single-entry maps were only handled if the value was a `List` or `Map` (sub-steps) or if the key was exact `_include`/`instruction`. Single-entry maps with string/primitive values (resulting from natural language step lines containing colons or inline `_include:`) threw an `IllegalArgumentException`.
- **Detection Gap ("What did we miss?"):** Unit tests in `YamlPlaybookParserTest` did not cover YAML list items containing inline colons (`:`) or inline `_include:` parameters within step text.
- **Resolution:** In `YamlPlaybookParser.java`, extended single-entry map handling in `parseStepBlock` to reconstruct `key + ": " + value` into full step instruction text when the value is scalar (`String`, primitive, or `null`), correctly creating `PlaybookStep` instances or resolving inline include directives.
- **Safety Net Added:** Added unit test `testParseYamlListStepWithInlineColonAndInclude()` in `YamlPlaybookParserTest.java` validating list steps containing inline colons and `_include:` targets.

### [DEF-20260930-03] ClassCastException when parsing playbook fragments with top-level YAML array list
- **Date:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks containing included fragment `.steps` files (or any YAML file structured as a top-level list `- ...`) fails with `java.lang.RuntimeException: Failed to parse playbook: <path> Caused by: java.lang.ClassCastException: class java.util.ArrayList cannot be cast to class java.util.Map`.
- **Root Cause:** In `YamlPlaybookParser.java`, SnakeYAML's `yaml.load(fileContent)` was directly assigned to a `Map<String, Object>` variable. When an included fragment file (such as `checkout-with-paypal.steps` or `proceed-to-payment.steps`) contains a top-level YAML list (`- step1\n- step2`), `yaml.load(fileContent)` returns a `java.util.ArrayList`, causing an unhandled `ClassCastException`.
- **Detection Gap ("What did we miss?"):** Existing `YamlPlaybookParserTest` unit tests only tested YAML files where the top-level structure was a YAML dictionary (e.g., `steps: ...`). There were no test cases for included fragment `.steps` files structured as top-level YAML array lists (`- ...`).
- **Resolution:** Updated `YamlPlaybookParser.java` to capture the output of `yaml.load(fileContent)` as `Object loadedObject`. If `loadedObject` is a `Map<?, ?>`, process standard top-level keys (`data`, `steps`, `before`, `after`). If `loadedObject` is a `List<?>` or `String`, delegate directly to `parseStepBlock`.
- **Safety Net Added:** Added unit test `testParseIncludedStepListFragment()` in `YamlPlaybookParserTest.java` validating recursive inclusion of fragment `.steps` files containing top-level YAML array lists.

### [DEF-20260930-02] Main Page Console (Playbook & Queue) Hangs During Test Execution Under High Log Volume
- **Date:** 2026-09-30
- **Component:** `aura-manager` (`dashboard-runner.js`, `dashboard-styles.css`)
- **Scope:** `Framework`
- **Symptom:** The aura-manager main UI page (playbook & queue) freezes/hangs during test execution when large amounts of stdout/console logs are printed by the test process.
- **Root Cause:** `dashboard-runner.js` invoked `localStorage.setItem('aura_previous_console_logs', terminalConsole.innerHTML)` synchronously on every single streamed log line. As log output grew to thousands of lines, writing multi-megabyte HTML strings synchronously to `localStorage` on the main JS thread dozens/hundreds of times per second blocked the browser event loop. Additionally, per-line unbatched DOM appends (`insertAdjacentHTML`) and layout queries (`innerText`) caused severe browser layout thrashing.
- **Detection Gap ("What did we miss?"):** UI tests did not run stress tests with high-frequency console output streams to measure browser event-loop latency and DOM reflow overhead.
- **Resolution:** Replaced per-line synchronous `localStorage` writes with debounced persistence (`debouncedSaveConsoleLogs`, throttled to 1 second), batched incoming log lines into single-pass DOM HTML appends (`appendLogsBatch`) per polling tick, replaced reflow-triggering `innerText` with `textContent` in filter updates, and added CSS layout containment (`contain: content`) to `#terminalConsole`—preserving 100% of all log lines without truncating output.
- **Safety Net Added:** Updated `dashboard-runner.js` and `dashboard-styles.css` with batch DOM appends, debounced persistence, and `textContent` filtering.

### [DEF-20260930-01] Aura Subprocess Playbook Parse Failures Logged as Passed in Console Execution Reports
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** When a test subprocess fails due to playbook parsing errors (e.g., `Failed to parse playbook`), `console-execution-1.json` was retained with status `"passed"` and missing failure message.
- **Root Cause:** `isFailedRun` in `AuraQueueService` did not account for `fileErrors`/`fileFailures` counters, and the execution JSON updater required `isZeroStep` (stepIndex == 0 && totalSteps == 0) to overwrite existing `"passed"` execution states.
- **Detection Gap ("What did we miss?"):** Tests did not assert that existing `console-execution-*.json` files with non-zero step metrics are overridden to `"failed"` with `failureReason` when the batch subprocess fails.
- **Resolution:** Updated `AuraQueueService` to include `fileErrors`/`fileFailures` in `isFailedRun`, relaxed `isZeroStep` restriction when `isFailedRun` is true to force-update non-failed execution states with `failureReason`, and expanded `extractSubprocessErrorMessage` trace parsing.
- **Safety Net Added:** Added unit test `testExtractSubprocessErrorMessageAndExecutionStatusUpdateOnParseError` in `AuraQueueServiceTest.java`.

### [DEF-20260929-01] Static Includes Create Synthetic Wrapper Step Nodes and Duplicate Substeps in Console Execution Reports
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`playbook-parser` / `console-reporting`)
- **Scope:** `Framework`
- **Symptom:** Unconditional static includes (`_include: file.steps`) generated synthetic container step nodes with duplicated substeps in `console-execution.json` instead of clean inlined top-level steps.
- **Root Cause:** `YamlPlaybookParser` wrapped static include steps inside a synthetic `PlaybookStep("_include: ...")` container and assigned `subStep.setParent(containerStep)`. Even after `flattenSteps()` flattened `flatSteps`, child steps retained `parent != null`, causing `PreliminaryReportListener` to reconstruct parent-child step hierarchies in `TestExecutionReport`.
- **Detection Gap ("What did we miss?"):** `YamlPlaybookParserTest` verified step counts after flattening but did not assert `parent == null` or check step hierarchy rendering for static includes.
- **Resolution:** Updated `YamlPlaybookParser` to inline static include steps directly into the playbook step list without synthetic wrapper nodes or parent links, keeping their origin `sourceFile` and `lineNumber` intact.
- **Safety Net Added:** Added unit tests in `YamlPlaybookParserTest` asserting `steps.size() == 5`, `parent == null`, `subSteps.isEmpty()`, and correct origin source file metadata for inlined static include steps.

### [DEF-20260928-07] AiSession Default Mock LLM Provider Returns Empty Tool Calls Breaking LLM-Mode Unit Tests
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`ai-session` / `mock-testing` / `agent-loop`)
- **Scope:** `Framework`
- **Symptom:** `AiSessionTest.testExecuteInlineStepsString`, `testExecutePlaybook`, and `testExecuteInlineYamlAutoSelectsFirstDataSet` fail with `InvalidAgentResponseException: Agent turn did not produce a valid tool call after warning`.
- **Root Cause:** `AiSession.createMockLlmProvider()` returned `new LlmResponse("[]", ...)` with empty tool calls. Under the tightened agent loop contract (`AgentToolLoopStep`), turns must produce a tool call (such as `complete_step`) or be rejected.
- **Detection Gap ("What did we miss?"):** When `AgentToolLoopStep` added mandatory tool call enforcement in commit 87651a09a, `createMockLlmProvider()` in `AiSession.java` was not updated to return a default `complete_step` tool call.
- **Resolution:** Updated `AiSession.createMockLlmProvider()` to return an `LlmResponse` populated with a `complete_step` `ToolCall`.
- **Safety Net Added:** `AiSessionTest` suite execution verifying all 17 unit tests pass cleanly.

### [DEF-20260928-06] Programmatic AiSession Replay Drops Step Status From Companion JSON Breaking 0-Action Step Replay
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`ai-session` / `playbook-replay` / `programmatic-execution`)
- **Scope:** `Framework`
- **Symptom:** Programmatic tests (`session.execute(...)`) containing steps recorded with 0 actions (such as verification steps completed via `complete_step` or conditional branches) passed during `FORCE_RECORDING`, but failed in `REPLAY_STRICT` with `ConclusiveFailureException: No recorded tool calls found for step '...' in REPLAY_STRICT mode`, and in `REPLAY_WITH_HEALING` by triggering unexpected live LLM fallback calls and failing replay metrics.
- **Root Cause:** In `AiSession.java`, step merging during replay copied `actions`, `toolCalls`, and hashes from `sessionSteps` to parsed inline steps, but omitted `status`, `failed`, and `failureReason`. Consequently, `parsed.getStatus()` remained `PlaybookStepStatus.PENDING`, causing `ExecuteActionsStep`'s `isRecordedCompletedStep` check to evaluate to `false`.
- **Detection Gap ("What did we miss?"):** File-based playbooks loaded their step status directly from JSON via `YamlPlaybookParser`, satisfying `isRecordedCompletedStep`. Programmatic test suites lacked unit tests verifying step status preservation across inline playbooks with 0-action steps.
- **Resolution:** Added copying of `status`, `failed`, and `failureReason` in `AiSession.java` step merging, and updated Step 9 of `ForwardIntegrationTest_testForwardSynonyms_Chrome_headless.json` with an explicit `assert_title` call.
- **Safety Net Added:** Added unit regression assertions in `AiSessionReplayTest` confirming that 0-action recorded steps retain `status` and replay successfully without triggering live LLM execution or `ConclusiveFailureException`.

### [DEF-20260928-05] Circular Self-Referencing Variable Sanitization in Literal STORE Actions Breaks Playbook Replay
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`action-sanitization` / `tool-loop` / `store-action`)
- **Scope:** `Framework`
- **Symptom:** Tests executing literal `STORE` actions passed in `FORCE_RECORDING` but failed in `REPLAY_STRICT` and `REPLAY_WITH_HEALING` with `UnresolvableVariableException: Unresolvable variable placeholder '${varName}' in template: "${varName}"` at `PlaybookToolReplayer.java:346`.
- **Root Cause:** When `store(variableName="varName", value="literalVal")` executed, it stored `"varName" -> "literalVal"` into `SessionData`. At step finalization, `DefaultActionSanitizer` and `AgentToolLoopStep.sanitizeToolCall` matched `"literalVal"` against `SessionData` and replaced it with `"${varName}"` inside the `store` tool's own arguments (`values: ["varName", "${varName}"]` and `arguments: {"variableName": "varName", "value": "${varName}"}`). On replay, `PlaybookToolReplayer` attempted to resolve `${varName}` before executing the `store` step, causing an unresolvable cyclic dependency.
- **Detection Gap ("What did we miss?"):** `DefaultActionSanitizerTest` only verified downstream consumer actions (e.g. `ASSERT_TEXT` referencing an existing variable), never testing sanitization of the `STORE` action itself or literal store tool calls.
- **Resolution:** Excluded the target variable name from candidate replacement variables during `STORE` action and `store` tool call sanitization, guarded `PlaybookToolReplayer` against resolving variable identifier keys, and repaired the recorded test playbook.
- **Safety Net Added:** Added unit regression test `testSanitizeStoreActionDoesNotSelfReferenceTargetVariable` in `DefaultActionSanitizerTest.java`.

### [DEF-20260928-04] Compile Error in StoreIntegrationTest Due to Undefined SessionData Method
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `StoreIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** `StoreIntegrationTest.java` lines 136-137 failed compilation in the IDE with `The method getDynamic(String) is undefined for the type SessionData`.
- **Root Cause:** In `testStoreMultipleVariables()`, `session.data().getDynamic(...)` was mistakenly used instead of `session.data().get(...)`. Incremental compilation in `mvn test-compile` masked the compiler error by treating the previously built `.class` file as up to date.
- **Detection Gap ("What did we miss?"):** Incremental Maven builds do not always recompile every test when target timestamps precede source edits; full clean test compilation was needed to uncover the syntax discrepancy.
- **Resolution:** Replaced `session.data().getDynamic(...)` calls with `session.data().get(...)`.
- **Safety Net Added:** Verified clean test compilation (`mvn clean test-compile`) ensuring 0 compilation errors across all test sources.

### [DEF-20260928-03] Premature Runtime Initialization Failures and Scope Deficits in Live Integration Tests (Scroll, Store, Timeout)
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `ScrollIntegrationTest`, `StoreIntegrationTest`, and `TimeoutIntegrationTest` failed immediately on invocation with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [...] was specified`, preventing execution. In addition, `RefreshIntegrationTest`, `ScrollIntegrationTest`, `SelectOptionIntegrationTest`, `StoreIntegrationTest`, and `TimeoutIntegrationTest` lacked `@AiLinter(false)`, omitted `.verifyMetrics()` chaining, lacked `@BeforeEach` lifecycle isolation, suffered from tagging inconsistencies (`@Tag("integration")`), and left major action scenarios unverified.
- **Root Cause:** Programmatic playbooks without external static YAML datasets cannot use `@AiDataSet`; legacy test classes contained orphaned dataset annotations and inline `data:` blocks. The test classes were never upgraded to the modern lifecycle, metric verification, and multi-scenario standards.
- **Detection Gap ("What did we miss?"):** The live integration test suite was not run with full test discovery across all live action classes in CI; individual test runs masked runner initialization failures.
- **Resolution:** Removed orphaned dataset annotations and dead data blocks, added `@AiLinter(false)`, isolated URL initialization into `@BeforeEach`, aligned JUnit tags to `@Tag("AuraIntegration")` and `@Tag("LiveAPI")`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with strict `.verifyMetrics()` chaining, and expanded scenario coverage across all 5 test classes.
- **Safety Net Added:** Modernized live integration test suites in `RefreshIntegrationTest.java`, `ScrollIntegrationTest.java`, `SelectOptionIntegrationTest.java`, `StoreIntegrationTest.java`, and `TimeoutIntegrationTest.java` with comprehensive multi-scenario execution and strict metric assertions.

### [DEF-20260928-02] Premature Runtime Initialization Failure and Metric Deficit in TypeIntegrationTest
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `TypeIntegrationTest` crashed immediately upon test startup with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [typeData] was specified`. In addition, execution metrics were discarded without validation, pre-conditions were unverified, and the test suite lacked coverage for multiline textareas, sequential multi-field submission, natural language semantic locators, value overwriting, and negative failure handling on disabled, readonly, and non-existent elements.
- **Root Cause:** Programmatic playbooks without external static YAML datasets cannot use `@AiDataSet`; the test harness contained an orphaned `@AiDataSet("typeData")` annotation and inline `data:` block. The test had not been modernized to adopt the `@BeforeEach` setup, `@AiLinter(false)`, `REPLAY_WITH_HEALING`, or `.verifyMetrics()` standards established across other action integration suites.
- **Detection Gap ("What did we miss?"):** Test suite sweeps did not execute `TypeIntegrationTest` individually in CI; linting and metric chaining requirements were not enforced on legacy live action tests.
- **Resolution:** Removed orphaned dataset annotations and unused imports, added `@AiLinter(false)`, isolated URL setup into `@BeforeEach`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with `.verifyMetrics()`, and expanded coverage across single-line input, multiline textareas, sequential form submission, natural language semantic locators, value overwriting, and negative failure handling for disabled, readonly, and missing inputs.
- **Safety Net Added:** Modernized live integration test suite in `TypeIntegrationTest.java` with 8 comprehensive scenarios and strict metric assertions.

### [DEF-20260928-01] Premature Runtime Initialization Failure and Metric Deficit in WaitIntegrationTest
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `WaitIntegrationTest` previously failed during test template setup with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [waitData] was specified`, discarded the execution result without metric validations (`verifyMetrics()`) allowing silent drops to go undetected, suffered from a weak assertion oracle (no pre-condition check), and lacked reach into static duration pauses (`wait` tool), dynamic DOM pop-in, content mutations, and loader disappearance.
- **Root Cause:** Programmatic playbooks without static YAML dataset definitions cannot use `@AiDataSet`; the test harness only contained a single happy-path method that triggered `assert_element_state` rather than exercising duration-based wait tools or multiple asynchronous DOM transition states provided in `testWaitHappyPath.html`.
- **Detection Gap ("What did we miss?"):** Test suite reviews did not enforce metric chaining or full fixture scenario reach for `WaitIntegrationTest`, leaving orphaned annotations, unused imports, and narrow reach undetected.
- **Resolution:** Removed orphaned dataset annotations and unused imports, added `@AiLinter(false)`, isolated URL setup into `@BeforeEach`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with `.verifyMetrics()`, and expanded coverage across pre-condition checks, hidden element reveals, dynamic pop-ins, content mutations, loader disappearance, and static duration pauses.
- **Safety Net Added:** Modernized live integration test suite in `WaitIntegrationTest.java` with comprehensive multi-scenario execution and strict metric assertions.

### [DEF-20260927-03] False Negative in Temporal Flow Anomaly Test and Non-Agnostic Linter Prompt Examples
- **Date:** 2026-09-27
- **Component:** `neodymium-core` (`ai-prompts` / `live-integration`)
- **Scope:** `Test/Harness` & `Framework`
- **Symptom:** `PrelinterRuleMatrixLiveTest.testTemporalFlowAnomaly_English_CloudIam` failed with `AssertionFailedError: Expected at least one linter finding ==> expected: <false> but was: <true>` due to zero linter findings; `playbook-linter-prompt.md` contained hardcoded language token lists (`and, und, et`, `If, Falls, Wenn`).
- **Root Cause:** In `testTemporalFlowAnomaly_English_CloudIam`, Step 2 clicked "Configure Role" to open general settings rather than opening the Revocation modal referenced in Step 1, so the linter correctly evaluated the sequence as non-inverted. Furthermore, the prompt relied on language-specific keyword lists rather than universal semantic definitions.
- **Detection Gap ("What did we miss?"):** Test fixture steps were written without verifying that the second step unambiguously targeted the opening of the dialog in the first step; prompt review failed to catch non-agnostic keyword enumerations.
- **Resolution:** Updated `testTemporalFlowAnomaly_English_CloudIam` so Step 2 explicitly triggers the confirmation modal dialog (`Click "Revoke Role" to open the confirmation modal dialog`). Refactored `playbook-linter-prompt.md` Rules 1 and 9 to excise language-specific keywords in favor of universal syntactic/semantic definitions.
- **Safety Net Added:** Deterministic unit tests in `PlaybookLinterPromptTest` and verified live isolation tests in `PrelinterRuleMatrixLiveTest`.

### [DEF-20260927-02] Broken Replay Reference and Invalid Browser Agent Dispatch in PrelinterChallengeIntegrationTest
- **Date:** 2026-09-27
- **Component:** `neodymium-core` (`ai-testing` / `live-integration`)
- **Scope:** `Test/Harness`
- **Symptom:** `PrelinterChallengeIntegrationTest` fails when executed in full: Method 3 (`testPrelinterBypassedInReplayStrict`) fails with `FileNotFoundException` during runner setup because no recording JSON file exists; Method 2 (`testPrelinterDisabledBypassesExecution`) triggers live browser LLM agent loops against intentionally flawed challenge steps.
- **Root Cause:** Method 2 configured `@AiMode(ExecutionMode.LLM_ONLY)` on a class pointing to `PrelinterChallengeTest.yaml`. Because it was not `LINTER_ONLY`, `NeodymiumAiRunner` dispatched `AgentToolLoopStep` to execute defective steps in the browser, while `LLM_ONLY` omitted recording generation (`isRecording() == false`). Method 3 attempted `REPLAY_STRICT` from Method 2's non-existent recording, causing immediate failure.
- **Detection Gap ("What did we miss?"):** Previous manual test runs and verification scripts executed only `-Dtest=PrelinterChallengeIntegrationTest#testPrelinterEnabledChallengesAllRules`, leaving Methods 2 and 3 unverified in CI.
- **Resolution:** Retired the broken `PrelinterChallengeIntegrationTest` and its orphaned fixtures (`PrelinterChallengeTest.yaml`, `PrelinterChallengeTest/index.html`).
- **Safety Net Added:** Upfront linter configuration, toggles, replay bypass, and fail-on-findings behavior remain fully guarded by deterministic unit tests in `PlaybookLinterTest`, and all 9 quality rules remain verified in `ExecutionMode.LINTER_ONLY` by `PrelinterRuleMatrixLiveTest`.

### [DEF-20260927-01] Missing Failure Status on Optional Steps During Live StateMachine Execution
- **Date:** 2026-09-27
- **Component:** `neodymium-core` (`ai-engine` / `StateMachineRunner`)
- **Scope:** `Framework`
- **Symptom:** When a step tagged with `(optional)` fails during live execution, the failure is bypassed without throwing an exception, but `metrics.getSoftFailedStepCount()` reports 0 instead of 1, and the recorded step status is incorrectly saved as `SUCCESS`.
- **Root Cause:** In `StateMachineRunner.java`, the exception handler for `playbookStep.isOptional()` logged a warning and popped try-catch scopes, but omitted setting `playbookStep.setStatus(PlaybookStepStatus.FAILED)`, `playbookStep.setFailed(true)`, and `playbookStep.setFailureReason(...)`. As a result, the subsequent `ExecuteActionsStep` post-step hook evaluated `!step.isFailed()` as true and promoted the step status to `PlaybookStepStatus.SUCCESS`.
- **Detection Gap ("What did we miss?"):** Existing optional integration tests (`mock.OptionalIntegrationTest` and legacy `live.OptionalIntegrationTest`) only verified that execution completed without throwing unhandled exceptions; neither asserted on step statuses or asserted via `.verifyMetrics().hasSoftFailedStepCount(1)`.
- **Resolution:** Updated `StateMachineRunner.java` in the `playbookStep.isOptional()` branch to explicitly mark `playbookStep` and any active sub-steps/parent steps as `FAILED`, `setFailed(true)`, and record the failure cause before continuing execution.
- **Safety Net Added:** Modernized `live.OptionalIntegrationTest` asserting `.verifyMetrics().hasStepCount(2).hasSoftFailedStepCount(1)` on bypassed failures (with `.onLive(m -> m.hasLlmCalls())`, `.onStrictReplay(m -> m.hasNoLlmCalls())`, and `.onHealing(m -> m.hasLlmCalls())`) and `.hasNoSoftFailures()` on passing optional steps across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`.

### [DEF-20260926-01] NullPointerException Unboxing Null Token Counts in GeminiLlmProvider and LLM Providers
- **Date:** 2026-09-26
- **Component:** `neodymium-core` (`ai-client`)
- **Scope:** `Framework`
- **Symptom:** In live agent execution (e.g., `IncludeIntegrationTest.testIncludeConditionalIfThenFalse`), the request fails with `ConclusiveFailureException` caused by `java.io.IOException: Failed to execute Gemini chat request: Cannot invoke "java.lang.Integer.intValue()" because the return value of "dev.langchain4j.model.output.TokenUsage.outputTokenCount()" is null`.
- **Root Cause:** In `GeminiLlmProvider.java` (and similarly in `OpenAiLlmProvider`, `VertexAiLlamaProvider`, and `MistralLlmProvider`), boxed `Integer` counts from LangChain4j's `dev.langchain4j.model.output.TokenUsage` were directly passed into primitive `int` constructor parameters of `TokenUsage(int, int, int, int)`. When LLM responses omit candidate/output token metadata (typical for tool calls or specific finish reasons), LangChain4j yields `null`, triggering an NPE during implicit auto-unboxing.
- **Detection Gap ("What did we miss?"):** Mock LLM unit tests construct `TokenUsage` with explicit non-null primitive integers (`new TokenUsage(10, 10, 20)`). Provider unit tests did not simulate LangChain4j responses with `null` token counters.
- **Resolution:** Introduced null-safe factory methods `TokenUsage.of(Integer, Integer, Integer, Integer)` and `TokenUsage.of(Integer, Integer, Integer)` in `TokenUsage.java` that coalesce `null` values to 0 and derive `totalTokenCount` if absent. Updated all four LLM providers (`GeminiLlmProvider`, `OpenAiLlmProvider`, `VertexAiLlamaProvider`, `MistralLlmProvider`) to use `TokenUsage.of(...)`.
- **Safety Net Added:** Unit tests in `TokenUsageTest` testing null and partial-null permutations for `TokenUsage.of(...)`.

### [DEF-20260925-03] Inclusion Tool Execution and Conditional Branching Fallback in AgentToolLoopStep
- **Date:** 2026-09-25
- **Component:** `neodymium-core` (`ai-agent-engine`)
- **Scope:** `Framework`
- **Symptom:** In live agent execution, conditional include playbooks (such as `testIncludeConditionalIfElseFallback`) failed because the agent could not invoke includes as native tools, instructions lost target context when hints were stripped (e.g. producing `"If is visible"` without a selector subject), and executing an include did not immediately terminate the turn loop, causing the agent to execute redundant duplicate turns.
- **Root Cause:**
  1. `BrowserToolProvider` lacked an `include` tool, forcing the LLM to either hallucinate actions or fail to invoke external playbooks directly when evaluating branches.
  2. `ExecuteActionsStep.prepareInstruction` stripped `(hint: ...)` from step instructions, resulting in grammatically incomplete instructions like `"If is visible"` when the condition relied on hint selectors.
  3. When an `include` action executes, it pushes steps onto `ExecutionContext`'s step stack to be processed by `StateMachineRunner`. Because `AgentToolLoopStep` did not treat successful `include` calls as a loop-completing goal, the turn loop continued running against an unchanged DOM.
- **Detection Gap ("What did we miss?"):** Integration tests with `MockLlmProvider` mocked pre-determined leaf actions directly on `MockLlmProvider`, bypassing the agent's interactive tool loop and dynamic prompt generation for include instructions.
- **Resolution:**
  1. Registered `include` tool in `BrowserToolProvider` accepting `path` (with aliases `file` and `target`) and executing `IncludeAction` against `ExecutionContext`.
  2. Enhanced `AgentToolLoopStep` prompt generation to detect selector hints from `rawInstruction` and repair `"If is visible"` to `"If <selector> is visible"`, appending explicit Target Selector Hint blocks.
  3. Added Rule #6 for conditional/include steps to the agent system prompt.
  4. Added immediate turn completion upon successful execution of the `include` tool, cleanly handing off execution to the pushed playbook steps on the execution stack.
- **Safety Net Added:** Unit tests in `BrowserToolsTest.testBrowserIncludeToolSchemaAndExecution()` verifying schema validation, missing path/context error handling, and stack pushing; integration tests in `mock.IncludeIntegrationTest` and `live.IncludeIntegrationTest`.

### [DEF-20260925-02] Dropped Branch Condition, Then, and Else Payloads in Action.fromToolCall
- **Date:** 2026-09-25
- **Component:** `neodymium-core` (`action-engine`)
- **Scope:** `Framework`
- **Symptom:** In `mock.IncludeIntegrationTest.testIncludeConditionalIfThen` and `mock.BranchIntegrationTest.testBranchMock`, conditional branch tool calls generated by the LLM silently dropped all `condition`, `then`, and `else` actions, causing the branch step to no-op and downstream assertions (`Cookies Accepted!`) to fail on empty DOM elements.
- **Root Cause:** `Action.fromToolCall(call)` in `Action.java` handled the `"branch"` tool name by setting `type = "BRANCH"` but never deserialized `args.path("condition")`, `args.path("then")`, or `args.path("else")` into `action.setCondition()`, `action.setThen()`, and `action.setElse()`. `BranchAction` therefore received a branch action with null branch collections. Furthermore, `Action.toToolCall()` lacked serialization of branch child collections, and `parseNestedAction` passed null `callId` into the `ToolCall` record constructor.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `Action.fromToolCall` covered only primitive leaf actions (`CLICK`, `TYPE`, `NAVIGATE`), but lacked coverage for composite tool calls with nested action arrays.
- **Resolution:** Updated `Action.fromToolCall` in `Action.java` to recursively deserialize nested action lists from `condition`, `then`, and `else` JsonNode arrays into `Action` objects (generating unique UUID callIds where missing). Added branch collection serialization to `Action.toToolCall()`.
- **Safety Net Added:** Unit tests in `ActionTest` verifying `Action.fromToolCall` and `toToolCall` round-tripping with nested `condition`, `then`, and `else` branches, and verified clean execution in `mock.IncludeIntegrationTest.testIncludeConditionalIfThen` and `mock.BranchIntegrationTest`.

### [DEF-20260925-01] Pre-Condition Assertion Timing and Inter-Test Hover State Leakage in HoverIntegrationTest
- **Date:** 2026-09-25
- **Component:** `neodymium-core` (`live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** In `HoverIntegrationTest.testHoverNonExistentElementFailure`, running the full test suite resulted in state contamination where `#categories-dropdown` and `#preview-card` pre-condition assertions failed before the test execution started. Additionally, the Visual RCA diagnostic reported misleading application-state failure ("application may not have reached expected checkout state").
- **Root Cause:** 
  1. The test asserted pre-conditions (`$("#categories-dropdown").shouldNotBe(visible)`) before `session.execute(...)` was called. Since `Open ${hover.test.url}` was inside `session.execute(...)`, the assertions ran against the dirty DOM left by preceding tests (`testCssHoverDropdown` and `testDelayedHoverActivity`).
  2. The autonomous agent probed `button#btn-categories` during its exploratory search for the missing element, altering page state before throwing `ConclusiveFailureException`.
  3. The non-existent element name was specified as `'Place Order'` on a catalog page, prompting the multimodal RCA agent to hypothesize an incomplete e-commerce checkout transition.
- **Detection Gap ("What did we miss?"):** Single-method test runs initialized a fresh browser session and did not expose the inter-method state leakage that occurs when running the full class in sequence.
- **Resolution:** Explicitly navigate to the test URL prior to checking initial pre-conditions, add an `@AfterEach` cleanup step to neutralize mouse position, and rename the negative test target to a domain-neutral action (`'Non-Existent Action'`).
- **Safety Net Added:** Full test class suite execution passing cleanly in sequence with isolated pre/post assertions and neutral RCA diagnostics.

### [DEF-20260924-05] Suite-Wide Underchecked Exception Types in Live Integration Tests
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** 28 negative test cases across 6 live integration test classes (`AssertIntegrationTest`, `CheckIntegrationTest`, `ClearIntegrationTest`, `ClickIntegrationTest`, `HoverIntegrationTest`, `TimeoutIntegrationTest`) used overly broad exception assertions (`Throwable.class`, `Exception.class`, or untyped `catch (final Exception e)` blocks).
- **Root Cause:** Historical use of generic exception catch-alls during initial test harness scaffolding. This violates test isolation and robustness principles:
  1. Catching `Throwable.class` masks fatal JVM Errors (`OutOfMemoryError`, `StackOverflowError`, `LinkageError`).
  2. Catching broad `Exception.class` allows arbitrary syntax errors, timeouts, or configuration glitches to falsely satisfy tests.
  3. Action steps (e.g. clicking/clearing/checking/hovering missing or disabled elements) have a distinct contract from verification steps (e.g. asserting text, URL, visibility, count, or attributes): action failures conclusively throw `ConclusiveFailureException`, while verification failures throw `AssertionError`.
- **Detection Gap ("What did we miss?"):** Linters and CI only checked whether tests passed green, without validating exception specificity against the framework's execution pipeline contracts.
- **Resolution:** Refactored all 28 negative test methods across the 6 live test classes to assert the exact expected exception type:
  1. `AssertIntegrationTest` (19 methods) & `TimeoutIntegrationTest` (1 method): migrated to `assertThrows(AssertionError.class, ...)`.
  2. `CheckIntegrationTest` (2 methods), `ClearIntegrationTest` (3 methods), `ClickIntegrationTest` (2 methods), and `HoverIntegrationTest` (1 method): migrated to `assertThrows(ConclusiveFailureException.class, ...)`.
- **Safety Net Added:** Exact type contracts enforced across all live suite negative tests, preventing false passes on pipeline or environmental crashes.

### [DEF-20260924-04] Agent Loop Misclassifies Plain-Text Action Failure Report as InvalidAgentResponseException
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** `HoverIntegrationTest.testHoverNonExistentElementFailure` threw `InvalidAgentResponseException: Agent turn did not produce a valid tool call after warning` instead of failing conclusively with `ConclusiveFailureException` when hovering over a missing element (`Hover over 'Place Order'`).
- **Root Cause:** Operating Rule 3 in the agent prompt directs: *"If an action fails (e.g., target element is disabled, missing, or non-interactable), DO NOT substitute uncommanded assertions and DO NOT call 'complete_step'; report the failure."* In `HoverIntegrationTest`, after `hover` failed on Turn 1 and exploratory DOM queries confirmed the element was missing, the LLM faithfully followed Rule 3 by reporting the failure in plain text without proposing tools. However, `AgentToolLoopStep` blindly treated any turn without tool calls (`proposedCalls == null || proposedCalls.isEmpty()`) as an invalid response, sent a warning scolding the model to invoke `complete_step`, and threw `InvalidAgentResponseException` on repeat. This misclassified an expected SUT action failure as an agent communication glitch and bypassed Visual RCA failure classification.
- **Detection Gap ("What did we miss?"):** Previous tests (e.g. `ClickIntegrationTest`) worked by accident because the model gave in to the warning and called `complete_step`, which was intercepted at line 719. Unit tests for `AgentToolLoopStepTest` only tested action failure rejection when the agent explicitly called `complete_step`, never when the agent adhered to Rule 3 and reported failure via plain text.
- **Resolution:** Updated `AgentToolLoopStep` so that when `hasActionToolFailed` is true for an interactive action instruction and no tool calls are proposed, the runner recognizes that the agent has concluded failure and immediately throws `ConclusiveFailureException` with the action failure details instead of issuing an invalid response warning or throwing `InvalidAgentResponseException`.
- **Safety Net Added:** Unit test in `AgentToolLoopStepTest.testActionFailureReportedInPlainTextThrowsConclusiveFailureException`.

### [DEF-20260924-03] Hover Action Tool Fallback to Body and Missing from Mutating Action Validation
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`BrowserToolProvider`, `AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Hovering over a non-existent element in `HoverIntegrationTest.testHoverNonExistentElementFailure` (`Hover over 'Add to cart'`) did not throw an exception and falsely reported success.
- **Root Cause:** Dual framework defects:
  1. `BrowserToolProvider.createHoverTool()` only declared `selector` in its schema and only queried `resolveSelector(args)` (ignoring `text`). When the LLM invoked `hover` with `{"text": "Add to cart"}`, `resolveSelector` returned `""`. `findElement("")` defaulted to `$("body")`, causing the tool to hover over the visible document `<body>` and return a false success. Additionally, empty target arguments were not validated before execution.
  2. `AgentToolLoopStep.isMutatingTool(name)` omitted `"hover"` (and `"scroll"`). Even when `hover` failed on a non-existent element, `hasActionToolFailed` was never set. When the agent subsequently called `complete_step`, interactive step failure validation was bypassed because `hover` was not tracked as a mutating action tool.
- **Detection Gap ("What did we miss?"):** Unit tests for browser tools verified tool registration but did not test hover argument parsing, text fallback, or empty target validation. `AgentToolLoopStepTest` tested action failure handling for `check` and `click`, but lacked coverage for `hover` and missing mutating tool executions on interactive instructions.
- **Resolution:**
  1. Updated `BrowserToolProvider.createHoverTool` to accept `text` alongside `selector` in the JSON schema, validate that at least one is provided (returning `ToolResult.error` otherwise), and resolve targets via text or CSS.
  2. Added `"hover"` and `"scroll"` to `AgentToolLoopStep.isMutatingTool(name)`, updated `callSelector` extraction to check `text` and `target` properties, and required that interactive action instructions execute at least one successful mutating action tool before `complete_step` is accepted.
- **Safety Net Added:** Unit tests in `BrowserToolsTest.testHoverToolSchemaAndTargetValidation` and `AgentToolLoopStepTest.testCompleteStepRejectedWhenHoverActionToolFailed` and `testCompleteStepRejectedWhenInteractiveActionInstructionHasNoMutatingToolExecuted`.

### [DEF-20260924-02] HoverIntegrationTest Catches Throwable Masking JVM Errors and Lacks State Assertion
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`HoverIntegrationTest`, `live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** `HoverIntegrationTest.testHoverNonExistentElementFailure` used `assertThrows(Throwable.class, ...)`. If a fatal JVM Error (such as `OutOfMemoryError`, `StackOverflowError`, or linkage failure) occurred during execution, the test would catch it and falsely report success. Additionally, no post-condition DOM state was asserted after failure.
- **Root Cause:** Incomplete test harness exception targeting. Catching `Throwable` violates framework test guidelines, which mandate catching `Exception.class` to prevent swallowing VM-level errors while verifying DOM stability after failure.
- **Detection Gap ("What did we miss?"):** The negative test passed during green runs because the framework properly threw an exception, but code analysis and linters did not flag `Throwable.class` usage in test assertions.
- **Resolution:** Replaced `Throwable.class` with `Exception.class` in `testHoverNonExistentElementFailure` and added post-condition oracle assertion `$(".dropdown-content").shouldNotBe(visible)`.
- **Safety Net Added:** Clean code and exception audit rule enforced across live integration test suite.

### [DEF-20260924-01] ForwardIntegrationTest Fails Initialization Due to Invalid @AiDataSet on Programmatic Playbook
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`NeodymiumAiRunner`, `live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `ForwardIntegrationTest` immediately crashed during JUnit Jupiter test template parameterization with `java.lang.IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [forwardData] was specified.`
- **Root Cause:** `ForwardIntegrationTest` was annotated with `@AiDataSet("forwardData")` while using `@AiPlaybook("programmatic")`. `NeodymiumAiRunner` attempts to resolve and filter dataset names against pre-parsed YAML playbook tables when building invocation contexts before test execution begins. Because `"programmatic"` playbooks do not define static YAML dataset tables upfront, filtering against an undeclared dataset is illegal. The inline `data:` block passed to `session.execute(...)` was only evaluated at runtime, long after test invocation discovery had already aborted.
- **Detection Gap ("What did we miss?"):** The test was tagged with `@Tag("LiveAPI")` and `@Tag("AuraIntegration")`, which are frequently excluded during routine offline CI or unit test runs without LLM credentials, masking the fact that the test method failed at JUnit discovery/parameterization time before any browser or LLM connection was initiated.
- **Resolution:** Removed the `@AiDataSet` annotation and inline `data:` block from `ForwardIntegrationTest`. Standardized fixture parameter injection in `@BeforeEach` via `session.data().putDynamic(...)`. Modernized the test suite to use real link click navigation across dedicated test fixture pages (`ForwardActionTest/page1.html`, `page2.html`, `page3.html`), and added synonym phrasing (`testForwardSynonyms`), sequential multi-step history traversals (`testMultipleForward`), and fluent metric assertions (`verifyMetrics()`).
- **Safety Net Added:** Verified test parameterization across all execution modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) via `mvn test -Dtest=ForwardIntegrationTest`.

### [DEF-20260923-06] Clear Action Silent No-Op on Checkbox Elements Leaving Checkboxes Selected
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`ClearAction`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** Executing `Clear #checkbox` on a checked `<input type="checkbox">` executed without error but left the checkbox in a checked/selected state (`isSelected() == true`). Additionally, negative tests expecting an exception failed with `AssertionFailedError: Expected java.lang.Throwable to be thrown, but nothing was thrown` because ChromeDriver permits `clear()` on any `<input>` element without error.
- **Root Cause:** Both `ClearAction` and `BrowserToolProvider#createClearTool()` delegated directly to `element.clear()`. While W3C WebDriver / ChromeDriver allows `clear()` on `<input type="checkbox">` by resetting its value string attribute, it does not toggle or uncheck the checkbox. Neither component inspected element types to uncheck checkboxes or assert editability constraints on checkboxes.
- **Detection Gap ("What did we miss?"):** No tests previously verified `ClearAction` or `BrowserToolProvider` against checkbox inputs. Tests had only focused on text inputs, textareas, and contenteditable elements.
- **Resolution:** Enhanced `ClearAction` and `createClearTool` in `BrowserToolProvider` to detect checkbox elements (`type="checkbox"` or `role="checkbox"`), assert editability (`element.shouldBe(Condition.editable)`), and if selected (`element.isSelected()`), click the element to uncheck it (preserving idempotency when already unchecked).
- **Safety Net Added:** Added unit test `testClearActionOnCheckbox` in `SelenideActionPluginsTest` and live/recorded integration test `testClearCheckbox` in `ClearIntegrationTest`.

### [DEF-20260923-05] AgentToolLoopStep Unconditional Exotic Tool Pruning Breaks Clear Cookies Live & Replay
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** `ClearCookiesIntegrationTest#testClearCookiesWhenEmpty` failed in `REPLAY_STRICT` (`ConclusiveFailureException: No recorded tool calls found for step 'Clear all cookies'`) and `REPLAY_WITH_HEALING` (`AssertionFailedError: Expected 0 LLM calls, but calls were made. ==> expected: <0> but was: <2>`). Additionally, `testClearCookiesWithActiveSession` failed during initial recording (`FORCE_RECORDING`) because browser cookies were not wiped.
- **Root Cause:** In commit `af748f3b2`, `AgentToolLoopStep#filterTools` introduced an unconditional exclusion of specialized tools (`clear_cookies`, `clear`, `upload_file`, `drag`, `drag_to`, `execute_script`) via `isExoticTool(clean)`. Because `clear_cookies` was unconditionally pruned from `availableTools`, the LLM agent never received the tool definition. In `testClearCookiesWhenEmpty`, the LLM fell back to calling `query_dom("*")` and `complete_step`, recording 0 mutating tool calls in the candidate JSON recording. During replay, `ExecuteActionsStep` threw a `ConclusiveFailureException` on empty tool calls in strict replay, and escalated to LLM healing in healing replay.
- **Detection Gap ("What did we miss?"):** Existing unit tests in `AgentToolLoopStepTest` only tested execution with custom mock tools and mock LLM calls. No unit tests validated tool catalog pruning against step instructions for specialized tools. Mock integration tests bypassed live LLM tool discovery by stubbing legacy JSON responses directly.
- **Resolution:** Enhanced `AgentToolLoopStep#filterTools` to check `isExoticToolDemanded` against instruction and context text (`effectiveInstructionText`), ensuring specialized tools (`clear_cookies`, `clear`, `upload_file`, `drag`, `drag_to`, `execute_script`) are retained whenever the instruction references them.
- **Safety Net Added:** Added unit tests in `AgentToolLoopStepTest` (`testFilterToolsPrunesExoticToolsOnStandardInstruction`, `testFilterToolsPreservesClearCookiesWhenRequested`, `testFilterToolsPreservesUploadFileWhenRequested`, `testFilterToolsPreservesDragWhenRequested`, `testFilterToolsPreservesExecuteScriptWhenRequested`), and validated live integration tests pass across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`.

### [DEF-20260923-04] LLM Action Evasion via Uncommanded Assertions and Substitution on Failed Action Steps
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** In `CheckIntegrationTest.testCheckDisabledElementFailure` and `testCheckNonExistentElementFailure`, negative tests expecting exceptions (`assertThrows`) failed because the autonomous LLM agent evaded the commanded failing action:
  1. When checking a disabled element (`#disabled-box`) or non-existent element (`#missing-checkbox-target`) failed, the agent called uncommanded assertions (e.g. `assert_element_state(state="disabled")` or `assert_element_state(state="absent")`) or checked an unrelated element (`#newsletter`), claiming the step succeeded via `complete_step`.
  2. In `BrowserToolProvider`, fallback mechanisms (such as clicking parent labels or JavaScript `arguments[0].click()`) bypassed disabled element constraints.
- **Root Cause:**
  1. `BrowserToolProvider.executeElementClick()` and `createCheckTool()` fell back to clicking parent labels or executing JavaScript clicks when standard element clicks threw an exception, bypassing the disabled state of form inputs.
  2. `AgentToolLoopStep` permitted `complete_step` if any effective tool call succeeded, without distinguishing mutating action tools from assertion or discovery tools. When a commanded action failed, successful evasion assertions satisfied the completion check. Furthermore, when challenged, the agent substituted an entirely different, uncommanded checkbox (`#newsletter`) on the page to make an action succeed.
- **Detection Gap ("What did we miss?"):** Prior tests only checked positive happy-path executions. Negative tests expecting interaction failures on disabled or absent elements had not been run with live LLM calls, missing the LLM's goal-seeking behavior to evade failures through uncommanded assertions and target substitutions.
- **Resolution:**
  1. Guarded `BrowserToolProvider.createCheckTool()` and `executeElementClick()` to strictly reject disabled elements (`el.is(Condition.disabled) || !el.is(Condition.enabled)`), throwing without falling back to parent label or JavaScript clicks.
  2. Verified in `createCheckTool()` that `el.isSelected() == targetChecked` post-interaction, returning error on failure.
  3. In `AgentToolLoopStep`, strictly enforce that for interactive action steps (`isInteractiveActionInstruction`), invoking `complete_step` while `hasActionToolFailed` is true throws `ConclusiveFailureException` immediately.
  4. Added target substitution guarding in `AgentToolLoopStep`: if a commanded target selector fails, successful actions on uncommanded elements (e.g. `#newsletter`) cannot clear `hasActionToolFailed`.
  5. Added explicit Operating Rule 3 prompt guidance prohibiting uncommanded assertion substitutions when actions fail.
- **Safety Net Added:** `BrowserToolsTest` assertions 8 and 9, `AgentToolLoopStepTest.testPrematureCompleteStepRejectedWhenActionToolFailed`, `AgentToolLoopStepTest.testCompleteStepRejectedWhenActionToolFailedAndTargetSubstitutionAttempted`, and live integration tests `CheckIntegrationTest.testCheckDisabledElementFailure` and `CheckIntegrationTest.testCheckNonExistentElementFailure`.

### [DEF-20260923-03] Non-Idempotent Check Action Toggling Checkbox State Upon Repeated Invocations
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`BrowserToolProvider`, `AgentToolLoopStep`, `Action`)
- **Scope:** `Framework`
- **Symptom:** In `CheckIntegrationTest.testCheckIdempotency`, issuing a second check step on an already checked checkbox toggled the checkbox off (unchecking it) instead of keeping it checked, causing idempotency assertions to fail.
- **Root Cause:** In the Unified Tooling Architecture, there was no dedicated `check` browser tool. Autonomous agent actions targeting checkboxes were mapped to the generic `click` tool (and `AgentToolLoopStep.normalizeToolName` collapsed `case "click", "check" -> "click"`). Because standard HTML `<input type="checkbox">` elements toggle state on click, issuing two consecutive check instructions resulted in two clicks (false -> true -> false).
- **Detection Gap ("What did we miss?"):** Prior tests only exercised single-click checking or used Selenide's `setSelected()` helper directly in harness code, without verifying multi-step idempotency inside the autonomous agent tool loop.
- **Resolution:**
  1. Implemented a dedicated, idempotent `check` browser tool in `BrowserToolProvider` that inspects `el.isSelected()` and only clicks if the current state differs from the desired target state (`checked: true/false`, defaulting to `true`), with parent `<label>` and JS fallbacks. Disallowed unchecking individual radio buttons.
  2. Added defensive delegation in `BrowserToolProvider.createSelectTool()` so that radio and checkbox inputs mistakenly targeted with `select` are safely routed to check logic.
  3. Updated `AgentToolLoopStep` system prompt (Operating Rule 3) and `normalizeToolName` to recognize and dispatch `check` / `uncheck` directly.
  4. Updated `Action.toToolCall` and `Action.fromToolCall` to round-trip `CHECK` / `check` actions.
- **Safety Net Added:** `BrowserToolsTest.testBrowserCheckToolSchema` and `CheckIntegrationTest.testCheckIdempotency`.

### [DEF-20260923-02] Duplicate @Test Annotation on @AiPlaybook Methods Triggering ParameterResolutionException
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`CheckIntegrationTest`, `ClickIntegrationTest`, `HoverIntegrationTest`, `ClearCookiesIntegrationTest`, `RefreshIntegrationTest`, `SelectOptionIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running tests failed with `ParameterResolutionException: No ParameterResolver registered for parameter [org.neodymium.ai.session.AiSession arg0] in method [public void setupProperties(org.neodymium.ai.session.AiSession)]` when `@BeforeEach` or test method expected `AiSession`.
- **Root Cause:** `@AiPlaybook` is meta-annotated with `@TestTemplate`. Adding `@Test` to the same method caused JUnit 5 Jupiter engine to discover and execute the method twice: once as standard `@Test` (where `AiInvocationExtension` is not registered and cannot resolve `AiSession`) and once as `@TestTemplate`. In addition, `SelectOptionIntegrationTest` used `@AiDataSet` without YAML datasets.
- **Detection Gap ("What did we miss?"):** Compilation succeeds because both `@Test` and `@AiPlaybook` are valid annotations; duplicate discovery errors only surface at test execution time.
- **Resolution:** Removed redundant `@Test` annotations and unused imports across affected live integration test classes; removed inapplicable `@AiDataSet` filter from `SelectOptionIntegrationTest`.
- **Safety Net Added:** Clean test execution of `CheckIntegrationTest` and companion live integration suites in both offline replay and live execution modes.

### [DEF-20260923-01] Orphaned endHook in ExecuteActionsStep Overwriting Step Status on Expected Bug with Continue-On-Error
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`ExecuteActionsStep`, `BugIntegrationTest`)
- **Scope:** `Framework`
- **Symptom:** In tests combining `(bug)` and `(continue-on-error)`, expected step failures are falsely logged as `❌ Expected bug but step succeeded` and persisted in recorded playbooks with status `SUCCESS`. In `BugIntegrationTest.testBugContinueOnError`, the test also failed due to an invalid assertion on non-existent element `#result`.
- **Root Cause:**
  1. `ExecuteActionsStep` pushed an `endHook` lambda onto `ExecutionContext.runStack` prior to `TryCatchStep`. When `StateMachineRunner` handled an expected bug failure on a step with `continue-on-error`, it discarded steps up to `EndTryStep` and continued the loop, popping the orphaned `endHook`. The hook unconditionally set `step.setStatus(SUCCESS)` and `step.setFailed(false)`.
  2. `BugIntegrationTest` asserted `$("#result").shouldHave(exactText("Click Me Triggered!"))` instead of targeting `<span id="click-status">` on `AllActionsTest/test.html`.
- **Detection Gap ("What did we miss?"):** Previous mock tests for `(bug) (continue-on-error)` only validated that the playbook run completed without uncaught exceptions, but did not assert that the failed step retained `PlaybookStepStatus.FAILED` in the recorded companion model.
- **Resolution:** Guarded status assignments and bug validation in `ExecuteActionsStep.endHook` when `step.isFailed()` is true; corrected the element locator and expected text in `BugIntegrationTest.testBugContinueOnError`.
- **Safety Net Added:** `BugIntegrationTest.testBugContinueOnError` verifying end-to-end recording and offline replay, plus assertion on recorded companion step status.

### [DEF-20260922-08] Lack of Retry Mechanism for Transient SessionNotCreatedException During WebDriver Startup
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`BrowserRunnerHelper`, `NeodymiumConfiguration`)
- **Scope:** `Framework`
- **Symptom:** Tests fail immediately during setup when ChromeDriver or the browser is temporarily unreachable (`org.openqa.selenium.SessionNotCreatedException: Could not start a new session. Response code 500. Message: session not created from chrome not reachable`).
- **Root Cause:** Transient OS process/socket collisions (e.g. DevTools port lingering in TIME_WAIT or Chrome shutdown latency from a previous test) cause ChromeDriver handshake to fail. Neodymium previously lacked a retry loop for driver session instantiation, treating all `SessionNotCreatedException` failures as fatal.
- **Detection Gap ("What did we miss?"):** No test harness resilience for transient process startup race conditions; browser creation assumed 100% determinism.
- **Resolution:** Wrapped `createWebDriverStateContainer` with a single-retry resilience loop with configurable backoff (default 2–10 seconds jitter), ensured automatic cleanup of partial resources (e.g. embedded proxies) before retry, and refreshed remote debugging port probes on retry.
- **Safety Net Added:** `BrowserRunnerHelperTest` validating single retry with backoff, abort behavior on subsequent failure, non-retry for other exceptions, and proxy leak prevention.

### [DEF-20260922-07] Generic RuntimeException Wrapping and Obsolete Schema Version Re-Persistence
- **Date:** 2026-09-22
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

### [DEF-20260922-06] Multi-Module Classpath Resource Path Resolution Divergence
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`ClasspathResourceManager`, `ClasspathResourceManagerTest`)
- **Scope:** `Framework`
- **Symptom:** `FORCE_RECORDING` mode in multi-module builds wrote newly generated JSON playbooks into `./src/test/resources/` in the top-level aggregator root directory instead of the submodule's directory (`neodymium-core/src/test/resources/`). Consequently, subsequent test runs executing `REPLAY_STRICT` or `REPLAY_WITH_HEALING` loaded obsolete cached recordings from `neodymium-core/target/test-classes/` that were never updated, leading to schema mismatches (`3.0` vs `4.0`) or missing recordings.
- **Root Cause:** `ClasspathResourceManager.getSourceResourcesRoot()` fell back to `Path.of("src/test/resources")` relative to `System.getProperty("user.dir")` instead of inspecting the active classloader root (`target/test-classes`, `target/classes`, or `bin`) to derive the actual Maven/Gradle submodule source resources directory.
- **Detection Gap ("What did we miss?"):** Unit tests ran in single-module contexts where `user.dir` coincided with the module directory, masking path divergence in multi-module reactor builds.
- **Resolution:** Updated `getSourceResourcesRoot()` to dynamically derive the source resource folder from the active classloader resource URL (`this.classLoader.getResource("")`) by substituting `target/test-classes` with `src/test/resources` before falling back to `user.dir`. Cleaned up 36 orphan playbooks from aggregator root.
- **Safety Net Added:** Added `testSourceResourcesRootResolutionInMultiModule()` in `ClasspathResourceManagerTest.java` and verified live recording in `CanvasClickSandboxMockTest`.

### [DEF-20260922-05] Chained Shorthand Tag Corruption, Missing Test-ID Variants, and Silent Blind Fallthrough in LocatorResolver
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`LocatorResolver`, `LocatorResolverTest`, `LocatorResolverBrowserTest`)
- **Scope:** `Framework`
- **Symptom:**
  1. Chaining attribute shorthands (e.g. `#modal >> data-testid=save` or `form >> id=submit`) generated invalid XPath queries searching for literal `<data-testid>` and `<id>` HTML tags (`//*[@id='modal']//data-testid` and `//id`).
  2. The `data-test-id=` attribute shorthand was unmapped, falling through to invalid raw CSS and failing at runtime.
  3. Unsupported vendor pseudo-classes and layout selectors (`:visible`, `:hidden`, `:right-of()`, `:left-of()`, `:above()`, `:below()`, `:near()`, `:nth-match()`, `:text-matches()`) silently fell through to `By.cssSelector`, causing deferred browser crashes with cryptic syntax errors and LLM thrashing.
  4. Playwright codegen internal prefixes (`internal:role=...`, `internal:text=...`, etc.) failed to resolve.
- **Root Cause:**
  1. `resolveChainedLocator` only handled `text=` and `role=`; all other segments were passed to `toXPathSegment()` which mistook shorthand keys (`data-testid`, `id`, `placeholder`) for HTML element tags.
  2. `LocatorResolver.resolveLocator` lacked pre-validation for unsupported Playwright/jQuery pseudo-classes before falling through to `By.cssSelector`, and did not recognize `data-test-id=` or `internal:` prefixes.
- **Detection Gap ("What did we miss?"):**
  Unit tests in `LocatorResolverTest` only verified isolated selectors and basic CSS/text chains. No unit or integration tests exercised attribute shorthands inside `>>` chains or checked behavior when unsupported pseudo-classes were submitted.
- **Resolution:**
  1. Expanded `resolveChainedLocator` to recursively map attribute shorthands (`id=`, `data-testid=`, `data-test=`, `data-test-id=`, `placeholder=`, `alt=`, `title=`, `label=`).
  2. Added `data-test-id=` attribute shorthand support.
  3. Added pre-validation in `resolveLocator` to detect unsupported pseudo-classes and spatial layout selectors, throwing an explicit `InvalidSelectorException` with actionable remedies.
  4. Normalized Playwright codegen `internal:*` prefixes and case flags (`[name="Save"i]`).
  5. Expanded pure unit tests in `LocatorResolverTest` and live headless Chrome browser tests in `LocatorResolverBrowserTest`.
- **Safety Net Added:**
  - Unit tests in `LocatorResolverTest`: `testChainedAttributeShorthands`, `testDataTestIdAttribute`, `testPlaywrightCodegenInternalPrefixes`, and `testUnsupportedSelectorsFailFastWithDiagnostics`.
  - Real-browser integration tests in `LocatorResolverBrowserTest`: `testNamedRoleInLiveBrowser`, `testLabelInLiveBrowser`, `testChainedShorthandsInLiveBrowser`, `testDataTestIdInLiveBrowser`, and `testUnsupportedSelectorThrowsInBrowser`.

### [DEF-20260922-04] Selector Parsing Regressions, Unreachable Fallbacks, and Incomplete Element Matching in LocatorResolver
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`LocatorResolver`, `LocatorResolverTest`)
- **Scope:** `Framework`
- **Symptom:**
  1. Standard CSS selectors starting with `text:` (such as SVG elements `text:nth-of-type(4)` or pseudo-elements `text::before`) were incorrectly parsed as Playwright text searches (`withText("nth-of-type(4)")`), rendering CSS fallback rules at Step 11 dead and unreachable.
  2. Text selectors using colon delimiters with values containing equals signs (e.g. `text:Status=OK`) were truncated to `OK` because delimiter detection checked `clean.contains("=")` globally across the entire string.
  3. `data-test=value` selectors were mapped to `[data-testid='value']`, failing to find elements with `data-test` attributes in Cypress/Playwright applications.
  4. Chaining bare ARIA roles (e.g. `#toolbar >> role=button`) produced broken XPath `//*[@id='toolbar']//role`, querying for non-existent `<role>` HTML tags.
  5. `label=value` selectors only matched `<input>` elements, ignoring labeled `<select>`, `<textarea>`, and `<button>` elements.
- **Root Cause:**
  1. Step 8 (`lower.startsWith("text:")`) greedily intercepted all `text:` selectors without checking for CSS pseudo-classes or pseudo-elements (`nth-`, `:`, `first-`, `last-`).
  2. Delimiter extraction used `indexOf(clean.contains("=") ? '=' : ':')`, which searched for `=` whenever an `=` appeared anywhere in the text content.
  3. `data-test=` was grouped with `data-testid=` and unconditionally hardcoded to `[data-testid='...']`.
  4. In `resolveChainedLocator`, bare roles returned `ByCssSelector` (`button, input[...]`), causing `roleBy instanceof ByXPath` to evaluate to false and falling into `toXPathSegment("role=button")`, which parsed `"role"` as a tag name.
  5. `label=` shorthand hardcoded `//input` in its XPath union rather than all labelable HTML form controls (`input`, `select`, `textarea`, `button`).
- **Detection Gap ("What did we miss?"):**
  Unit tests in `LocatorResolverTest` suffered from the "Accommodating Test" characterization trap: when tests failed against current behavior, assertions were altered to assert the broken implementation output (e.g. asserting `Selectors.withText("nth-of-type(4)")`) rather than enforcing specification contracts.
- **Resolution:**
  1. In `LocatorResolver`, guarded Step 8 so `text:` selectors followed by CSS pseudo syntax (`nth-`, `:`, `first-`, `last-`) fall through to CSS resolution preserving pseudo-colons (`*::before`, `*:nth-of-type(4)`).
  2. Fixed prefix delimiter detection to inspect whether the matched prefix itself ends in `:` or `=`.
  3. Updated `data-test=` to resolve to `[data-test='...'], [data-testid='...']` to match either attribute.
  4. In `resolveChainedLocator`, added `resolveBareRoleXPath` translating bare role locators into W3C XPath element predicates (`*[self::button or (self::input and (@type='button' or @type='submit')) or @role='button']`).
  5. Expanded `label=` XPath to match all standard labelable form elements (`input`, `select`, `textarea`, `button`).
- **Safety Net Added:**
  Expanded `LocatorResolverTest` with strict contract assertions verifying CSS pseudo-classes, colon-delimiter value preservation with equals signs, `data-test` matching, chained bare roles, and multi-element label resolution.

### [DEF-20260922-03] Root Container Fallback and Incomplete Playwright Selector Support Causing False Element Matching on Hidden Elements
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`LocatorResolver`, `QualityJudgeToolInterceptor`, `SelenideElementFinder`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertVisibility`, asserting that a hidden element is absent (`Assert that the hidden 'Secret Button' is absent`) failed. The selector resolved to `<body>` instead of the absent/hidden element, or failed on `assert_text(negated=true)` with `Expected text/pattern "Secret Button" was still present anywhere on the page within 3000ms.`.
- **Root Cause:**
  1. `QualityJudgeToolInterceptor` called `driver.findElements(By.cssSelector(selector))` directly, throwing `InvalidSelectorException` when Playwright selectors (e.g. `text=Secret Button`, `role=...`, or chained `>>`) were used, silently bypassing quality deliberation.
  2. `SelenideElementFinder` duplicated pseudo-resolution with an ad-hoc XPath expression `contains(normalize-space(.), '...')`. In XPath, `.` matches the string-value of all descendants, matching `<html>` and `<body>`.
  3. When the targeted button was hidden (`display: none`), `findFirstVisible` filtered it out, and Chromium's `visibleEls.find(Condition.focused)` defaulted to `<body>` (since `document.activeElement` is `document.body` when no element has focus), returning `<body>` as the matched element.
  4. In `BrowserToolProvider.matchesElementText`, `el.getAttribute("textContent")` was evaluated unconditionally on `$("body")`, dumping all text in the entire DOM (including hidden `display: none` elements). When the LLM attempted `assert_text(expectedText="Secret Button", negated=true)`, `isTextPresentOnPage` always reported the text present.
- **Detection Gap ("What did we miss?"):**
  1. `LocatorResolver` lacked browserless unit test coverage for Playwright selector extensions (`role=`, chained `>>`, attribute shorthands, test IDs).
  2. Element finding tests focused on finding present and visible elements; negative assertions on hidden/absent elements were not guarded against root container (`html`/`body`) fallback.
  3. Page-level text presence assertions lacked tests verifying that text inside `display: none` elements is correctly recognized as not present on the rendered page.
- **Resolution:**
  1. Expanded `LocatorResolver` into a comprehensive translation layer supporting all Playwright selectors (`role=`, attribute shorthands, test IDs, exact vs substring text matching, chained `>>` combinators, Shadow DOM).
  2. Excluded root containers (`html`, `body`, `head`) from wildcard text match expressions in `LocatorResolver.buildPseudoSelectorXpath`.
  3. Updated `QualityJudgeToolInterceptor` to query live DOM using `LocatorResolver.resolveLocator(selector)`.
  4. Streamlined `SelenideElementFinder.findDirect` to delegate directly to `LocatorResolver`, excised flawed `tryResolvePlaywrightPseudo`, and updated `findFirstVisible` to filter out `html` and `body` unless explicitly requested.
  5. In `BrowserToolProvider`, guarded `matchesElementText` so `textContent` is never evaluated on root containers (`body`/`html`), and filtered for visible inputs in `isTextPresentOnPage`.
- **Safety Net Added:**
  - Added unit test suite `LocatorResolverTest` (10 tests) verifying locator resolution without requiring a browser.
  - End-to-end multi-cycle regression test in `AssertIntegrationTest#testAssertVisibility` covering live recording and strict/healing replay cycles.

### [DEF-20260922-02] Missing Native Negation Support across Assertion Tools Causing Flakiness on Negative Assertions
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-executor`, `action-model`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertUrl`, step 9 ("There is no '#' in the url") exhibited non-deterministic flakiness: in some runs, the LLM invoked `assert_url({"expectedUrl": "#"})` without negation support, timing out waiting for '#' to appear and failing the step; in other runs, the LLM guessed a complex regex workaround (`^[^#]*$`), passing the step.
- **Root Cause:**
  1. None of Neodymium's browser assertion tools (`assert_url`, `assert_title`, `assert_text`, `assert_attribute`, `assert_count`, `assert_element_state`) exposed a first-class `negated` / `not` property in their schema or runtime execution loops.
  2. The `Action` domain model lacked a `negated` property and corresponding tool serialization/deserialization logic, preventing offline replays and action plugins from preserving or enforcing negative assertions.
  3. `AssertAction` lacked handling for negative assertions on URL, title, text, attribute, and count (`!=`), as well as `ASSERT_UNFOCUSED`.
- **Detection Gap ("What did we miss?"):**
  Assertion tool tests previously verified positive existence or exact matches, but lacked negative asserting suites testing that absence of characters, absent attributes, not-equal counts, and element state inverters (e.g. visible <-> hidden, focused <-> unfocused) work reliably without prompt engineering regexes.
- **Resolution:**
  1. Added `@JsonProperty("negated") private boolean negated = false;` to `Action` with full constructor overloads, with-methods, and JSON serialization/deserialization.
  2. In `Action.toToolCall()`, emit `"negated": true` if set, and map `ASSERT_UNFOCUSED`.
  3. In `Action.fromToolCall()`, parse `negated` (with aliases `not`, `invert`, `inverted`), invert element states, and map `NOT_EQUALS` count assertions to `!=`.
  4. Added `negated` schema property and inverted condition loops across `assert_url`, `assert_title`, `assert_text`, `assert_attribute`, `assert_count`, and `assert_element_state` (including `unfocused`).
  5. Updated `AssertAction` to honor `action.isNegated()` on URLs, titles, text, attributes, count (`!=`), and element focus.
- **Safety Net Added:**
  - `ActionTest#testNegatedJsonRoundTrip`, `ActionTest#testFromToolCallWithNegatedFlag`, `ActionTest#testFromToolCallAssertElementStateInversion`, `ActionTest#testFromToolCallAssertElementStateUnfocused`, `ActionTest#testFromToolCallAssertCountNotEquals`.
  - `BrowserToolsTest#testAssertToolsNegationSchemaProperties`, `BrowserToolsTest#testNormalizeElementStateUnfocused`.
  - `BrowserToolProviderStabilityTest` async polling regression coverage.
  - `AssertActionTest#testNegatedUrlAssertions`, `AssertActionTest#testNegatedTitleAssertions`, `AssertActionTest#testNegatedTextAssertions`, `AssertActionTest#testNegatedAttributeAssertions`, `AssertActionTest#testUnfocusedAndNegatedCountAssertions`.

### [DEF-20260922-01] Missing Replayed Step Count Tracking in ExecuteActionsStep Causing hasAllStepsReplayed Assertion Failure
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-replay`, `metrics`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest` and any tests asserting `hasAllStepsReplayed()` on replay, `hasAllStepsReplayed()` failed with `AssertionError: Not all steps were replayed from cache. ==> expected: <N> but was: <0>`.
- **Root Cause:**
  `AiSession.getMetrics()` retrieves `replayedStepCount` from `ExecutionContext.KEY_TOTAL_REPLAYS`. In `ExecuteActionsStep`, replaying steps via `PlaybookToolReplayer.replayStep(...)` or bypassing them via `VisualBaselineGateStep` executed the actions against the SUT but never incremented `ExecutionContext.KEY_TOTAL_REPLAYS`. Consequently, `KEY_TOTAL_REPLAYS` remained 0 across all successful replay steps.
- **Detection Gap ("What did we miss?"):**
  Unit tests for `MetricsAsserter` and `ExecutionMetrics` used manually constructed instances with mocked non-zero `replayedStepCount` values. Pipeline integration tests focused on step completion rather than asserting that the runtime metrics asserter counted real pipeline replay steps.
- **Resolution:**
  1. In `ExecuteActionsStep.java`, increment `ExecutionContext.KEY_TOTAL_REPLAYS` upon successful completion of `PlaybookToolReplayer.replayStep(...)`.
  2. In `ExecuteActionsStep.java`, increment `ExecutionContext.KEY_TOTAL_REPLAYS` when a step is visually verified and bypassed via `VisualBaselineGateStep` in replay mode.
- **Safety Net Added:**
  Regression test `ExecuteActionsStepTest#testReplayedStepCountIncrementedOnReplay` and `AssertIntegrationTest#testAssertUrl` verifying that `hasAllStepsReplayed()` passes with exact step count equality on replay runs.

### [DEF-20260921-03] Missing assert_element_state and assert_attribute Tools Causing False Pass in testAssertReadonlyFailure
- **Date:** 2026-09-21
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-pipeline`, `action-model`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertReadonlyFailure`, the step `Assert that the 'readonly-input' field is editable` succeeded unexpectedly, generating an HTML report marked `PASSED` while the test failed in JUnit (`AssertionFailedError: Expected java.lang.Throwable to be thrown, but nothing was thrown`).
- **Root Cause:**
  1. `BrowserToolProvider` registered only text, count, URL, and title assertion tools (`assert_text`, `assert_count`, `assert_url`, `assert_title`), lacking native tools to assert element states (`editable`, `readonly`, `enabled`, `disabled`, `visible`, `hidden`, `checked`, `selected`, etc.) and element attributes (`placeholder`, `value`, `href`, `data-*`).
  2. In `AgentToolLoopStep`, the agent was instructed to call an assertion tool on verification instructions before `complete_step`.
  3. Lacking `assert_element_state`, the LLM inspected `#readonly-input`, observed `value="FixedData"`, and hallucinated/substituted `assert_text({"selector": "#readonly-input", "expectedText": "FixedData"})`. Because "FixedData" was present, `assert_text` succeeded, the LLM called `complete_step`, and the step was marked successful without testing the required editable state.
- **Detection Gap ("What did we miss?"):** `SelenideTargetExecutor` and `AssertAction` had full support for `ASSERT_EDITABLE`, `ASSERT_READONLY`, `ASSERT_ATTRIBUTE`, etc., in playbook replay, but `BrowserToolProvider` (which supplies tools to the live LLM agent loop) had not exposed corresponding tools. Unit tests verified tool loop completion but did not verify state assertion failures during live recording.
- **Resolution:**
  1. Implemented `assert_element_state` in `BrowserToolProvider` supporting `["visible", "hidden", "enabled", "disabled", "editable", "readonly", "checked", "unchecked", "selected", "unselected", "focused", "exists", "absent"]` and throwing `AssertionError` when conditions fail.
  2. Implemented `assert_attribute` in `BrowserToolProvider` supporting exact, substring, and regex attribute assertions and throwing `AssertionError` on mismatch.
  3. Mapped both tools bidirectionally in `Action.java` (`toToolCall()` and `fromToolCall()`).
  4. Updated `AgentToolLoopStep` prompt guidelines and tool normalizations to recognize `assert_element_state` and `assert_attribute`.
- **Safety Net Added:** Unit tests in `BrowserToolProviderStabilityTest`:
  - `testAssertElementStateToolSchema`
  - `testAssertAttributeToolSchema`
  - `testNormalizeElementStateUtility`
  - `testAssertElementStateReadonlySuccessAndEditableFailure` (verifying `readonly` passes and `editable` on a readonly element throws `AssertionError`)
  - `testAssertAttributeSuccessAndFailure` (verifying attribute substring match passes and mismatch throws `AssertionError`)

### [DEF-20260921-02] Soft Error Bypass in assert_text Suppressing AssertionError on Mismatched Selectors
- **Date:** 2026-09-21
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `WikipediaProgrammaticTestDataTest`, Step #5 (`Verify the main heading contains '${searchPhrase}' (bug)`) failed with `ExpectedBugNotReproducedException: Expected bug but step succeeded` instead of catching the headline mismatch defect ("Neodym" vs "Neodymium"). The HTML report displayed an uncommanded second assertion on `#mw-content-subtitle`.
- **Root Cause:**
  1. In `BrowserToolProvider.createAssertTextTool`, when an element selector failed to match the expected text within `Configuration.timeout`, the tool checked `if (!isTextPresentOnPage(...))`. If the expected text existed anywhere else on the page (e.g. in a redirect subtitle, breadcrumb, or footer), it returned `ToolResult.error` instead of throwing `AssertionError`.
  2. Returning `ToolResult.error` bypassed Stop Criterion 2 (immediate termination on assertion failures) in `AgentToolLoopStep`.
  3. The agent loop fed the error back to the LLM in Turn 2, which searched for the text elsewhere in the DOM, asserted on `#mw-content-subtitle` instead of the commanded `#firstHeading`, and called `complete_step`.
  4. This false step success caused `ExecuteActionsStep` to throw `ExpectedBugNotReproducedException` on the expected defect step.
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested page-wide fallbacks and asynchronous polling, but did not assert that an element-specific `assert_text` failure throws `AssertionError` when the text is present in another element on the page.
- **Resolution:**
  1. Removed `isTextPresentOnPage` bypass from element-targeted `assert_text` in `BrowserToolProvider`, throwing `AssertionError` directly when the specified selector does not match the expected text within timeout.
  2. Maintained page-wide text assertions (`isTextPresentOnPage`) only when `selector` is null, blank, `"body"`, or `"html"`.
- **Safety Net Added:** Unit regression test in `BrowserToolProviderStabilityTest#testAssertTextThrowsAssertionErrorWhenSelectorDoesNotMatchEvenIfTextExistsElsewhereOnPage` verifying that `assert_text` on a specific selector throws `AssertionError` when the element text does not match, even if the text exists elsewhere in the document body.

### [DEF-20260920-01] Tool Loop MoveTargetOutOfBoundsException on Coordinates and Malformed Trailing Selector in Hybrid Clicks
- **Date:** 2026-09-20
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`)
- **Scope:** `Framework`
- **Symptom:** AI tool loop failed with `org.openqa.selenium.interactions.MoveTargetOutOfBoundsException: move target out of bounds: (158, 893) is out of bounds of viewport width (1280) and height (857)` when executing `click({"selector": "article[data-ai=\"xcboo7um\"] button, text:", "x": 158, "y": 893})`.
- **Root Cause:**
  1. In `BrowserToolProvider.createClickTool`, Case 1 (coordinates provided) was prioritized over Case 3 (element selector/text resolution). Even though an element selector was provided, raw coordinates routed execution directly to raw Selenium `new Actions(driver).moveToLocation(x, y).click().perform()`, completely bypassing Selenide's auto-scrolling element click logic.
  2. Selenium's `Actions.moveToLocation(x, y)` operates strictly in viewport coordinates without auto-scrolling and throws `MoveTargetOutOfBoundsException` if coordinates are outside `[0, innerWidth] x [0, innerHeight]`.
  3. LLM generated a malformed trailing selector token (`button, text:`), which caused `document.querySelector` to fail with a `DOMException: SyntaxError` and prevented element bounding rect resolution.
- **Detection Gap ("What did we miss?"):** Tests in `BrowserToolProviderStabilityTest` verified coordinate clicks within bounds and basic selector resolution separately, but did not test hybrid calls where an LLM provides both an element selector and out-of-viewport coordinates, nor did they test selector sanitization or coordinate auto-scrolling.
- **Resolution:**
  1. Added `cleanSelector` utility to sanitize hallucinated trailing markers (such as `, text:`, `, text=`, trailing commas).
  2. In `createClickTool`: Prioritized element-based clicking via Selenide (`el.shouldBe(Condition.visible).click()`) when a selector or text is provided and target is not explicitly `coord:...`. If coordinates are within the element's bounding box (`0 <= x <= width`, `0 <= y <= height`), treated them as an in-element offset via `Actions.moveToElement(el, xOffset, yOffset)`.
  3. Added auto-scrolling and viewport clamping in `performSafeCoordinateClick`: if target coordinates are outside the viewport, `window.scrollBy(...)` is called to scroll the target coordinate into the center of the viewport, coordinate offsets are adjusted, clamped to viewport boundaries, and a JavaScript `document.elementFromPoint(x, y).click()` fallback is executed if `Actions.moveToLocation` fails.
  4. Updated `clickBadgeScript` to scroll the badge into view before querying bounding rect.
  5. Updated `createHoverTool` to call `SelenideElementFinder.scrollIntoViewIfNeeded(el)` before hovering.
- **Safety Net Added:** Unit tests in `BrowserToolProviderStabilityTest` verifying `cleanSelector`, hybrid click with selector prioritizing element click without out-of-bounds error, and coordinate auto-scroll & clamping.

### [DEF-20260919-06] Replay Failure on Compound Step with Coalesced Actions and Missing 'submit' Keyword in partitionToolCallsAndActions
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-runner`)
- **Scope:** `Framework`
- **Symptom:** `CartTest.liveNormal` succeeds with an expected bug, but its recorded playbook fails during `CartTest.replayNormal` in `REPLAY_STRICT` mode with: `No recorded tool calls found for step 'Submit the promo code form.' in REPLAY_STRICT mode. Companion JSON recording file is missing or step was not recorded.`
- **Root Cause:**
  1. `matchesSubStep` in `AgentToolLoopStep` checked keywords `click`, `press`, `select`, `choose`, `add`, but omitted `submit`. The submit button click was not matched to 'Submit the promo code form.' and fell back to 'type 'FREEGIFT' into it', leaving the submit sub-step with 0 recorded tool calls.
  2. In `ExecuteActionsStep`, unrolled child sub-steps of a compound step were strictly required to have recorded tool calls in `REPLAY_STRICT` mode, failing on legitimate coalesced sub-steps where a single preceding action (e.g. `fill` clearing and typing) satisfied multiple milestones.
  3. When an assertion failed in `AgentToolLoopStep`, Stop Criterion 2 re-threw `AssertionError` before appending the in-flight tool call to `executedCalls`, preventing the failed assertion from being recorded and partitioned to its sub-step in the companion JSON.
- **Detection Gap ("What did we miss?"):** Existing compound turn group tests only tested 1:1 sub-step-to-tool-call mappings and did not verify replay unrolling of compound steps with coalesced sub-steps or expected defect assertions.
- **Resolution:**
  1. Added `submit` to `matchesSubStep` for `click` in `AgentToolLoopStep`.
  2. Recorded the in-flight assertion `ToolCall` and mapped failed `Action` in `executedCalls` when Stop Criterion 2 triggers in `AgentToolLoopStep`, ensuring failed assertions are captured in companion JSON playbooks.
  3. Updated `ExecuteActionsStep` to allow child sub-steps of a compound parent (`step.getParent() != null`) with 0 tool calls to complete as coalesced no-ops in `REPLAY_STRICT` mode instead of throwing `ConclusiveFailureException`.
- **Safety Net Added:** Unit regression tests in `AgentToolLoopStepTest` (verifying `submit` matching and failed assertion recording) and `ExecuteActionsStepTest` (verifying unrolled compound replay with coalesced sub-steps).

### [DEF-20260919-05] Expected Defect Discovery Loop Death Spiral, Unpartitioned Abortive Exceptions, and Listener Status Fabrication in Compound Steps
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-report`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest_livePerfect_perfect_20260919-222644.html`, Step #8 (`Locate the promo code input field:`) failed with `TokenBudgetExceededException: TOTAL budget breached (consumed: 100142, limit: 100000)` across 15 turns. In the HTML report, sub-steps #8.1 through #8.3 were falsely stamped as `SUCCESS` with 0 actions and 0ms duration, while sub-step #8.4 was marked `FAILED` with the parent's `Token budget exceeded` error message.
- **Root Cause:**
  1. **Discovery Loop Death Spiral on Expected Defect:** Step #8 contained 4 compound sub-steps, ending with an expected defect assertion: `Assert that a line item 'Free Bonus Gift' (bug) is added to the cart`. In Verla `perfect`, promo code `FREEGIFT` does not add the bonus line item. The agent checked DOM presence via discovery tool `query_dom({"text": "Free Bonus Gift"})` -> returned 0 matches. Because system prompt instructions strictly prohibited premature `complete_step` before verifying milestones, the agent erroneously deduced that the form submission in turn 2 had not registered, entering a 15-turn death spiral repeatedly re-submitting the promo form until breaching the 100k token limit.
  2. **Unpartitioned Abortive Exceptions:** Action partitioning and duration assignment (`AgentToolLoopStep.partitionToolCallsAndActions`) was only invoked on normal completion (`finishLoop`). When `TokenBudgetExceededException` or timeout aborted the loop, execution bypassed partitioning, leaving all compound child steps with empty actions and 0 duration in the report context.
  3. **Reporting Listener Status Guessing:** `PreliminaryReportListener` lacked handling for abortive infrastructure failures and checked `sub.isBug()`, assuming any failure in a step containing an expected bug was due to the bug, while fabricating `SUCCESS` on preceding sub-steps that had no recorded actions.
- **Detection Gap ("What did we miss?"):** Existing compound turn group tests only tested clean paths where all assertions succeeded, or single-step unrolled failures. None tested an expected bug in a compound step where an assertion milestone failed on an absent element, nor tested that abortive infrastructure exceptions (like token budget limits) properly partition executed actions to the sub-steps executed before the abort.
- **Resolution:**
  1. Updated `AgentToolLoopStep.executeLoop` to wrap the loop execution in `try-finally`, guaranteeing that `finalizeStepExecution` is always executed even on abortive exceptions (`TokenBudgetExceededException`, `StepTimeoutExceededException`), ensuring executed actions and proportional durations are partitioned to child sub-steps. Added idempotency protection (`KEY_STEP_EXECUTION_FINALIZED`) to prevent double-processing.
  2. Refined prompt directives (`systemPrompt`, `turnPrompt`) instructing the LLM that when `query_dom` finds 0 matches for an expected verification milestone, it MUST NOT retry prior form actions, but immediately invoke the commanded assertion tool (`assert_text`, `assert_count`) so expected defects and failures are cleanly asserted and recorded.
  3. Extended `matchesSubStep` in `AgentToolLoopStep` to match `clear`, `empty`, `reset` instructions to `fill`/`clear` tools.
  4. Updated `PreliminaryReportListener` to detect abortive infrastructure failures (`Token budget exceeded`, `timeout`, `Fatal environment`), preventing fabricated `SUCCESS` on unexecuted sub-steps, properly syncing executed actions and statuses from child steps, and attributing the abortive failure to the step active when the abort occurred without falsely blaming expected bugs.
- **Safety Net Added:** Added unit regression tests in `AgentToolLoopStepTest` (`testTokenBudgetExceededInCompoundStepPartitionsExecutedActionsAndSetsDurations`) asserting that when an abortive token exception occurs, executed actions and durations are still partitioned across compound sub-steps, and in `PreliminaryReportListenerTest` (`testAbortiveFailurePreservesSubStepStatusWithoutFabricatedSuccess`) asserting that abortive failures preserve accurate sub-step status and do not fabricate `SUCCESS`.

### [DEF-20260919-04] Quality Judge Container-Hijacking on Compound Steps
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`QualityJudgePrompt`, `QualityJudgeToolInterceptor`)
- **Scope:** `Framework`
- **Symptom:** `CartTest.liveNormal`, `CartTest.liveBad`, and `CartTest.liveAllDataSets` failed during live recording on the composite add-to-cart step: `liveBad` and `liveAllDataSets` failed with `Expected text/pattern "CART 1" was not found` because the cart item count remained 0; `liveNormal` timed out with `Step timeout of 60s exceeded (elapsed: 60s)`.
- **Root Cause:** In interactive deliberation mode (`QualityJudgeToolInterceptor`), the Judge prompt omitted the active tool action (`Tool: click`) and internal milestones. When evaluating a button click inside a compound step like `Locate the first product card: ... Click its 'Add to Cart' button`, the Judge compared the button locator against the parent header (`Locate the first product card:`) and erroneously refined the locator to the parent card container (`div[data-ai='xcm57t27']` or `article[data-ai='xcboo7um']`). Clicking the container either did not trigger add-to-cart or navigated away to the PDP, causing 15 LLM turns and a 60s timeout.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `QualityJudgePrompt` tested single-line instructions without compound milestones or interactive child buttons inside containers, missing container-hijacking behavior.
- **Resolution:**
  1. Enriched `compileDiscussionRequest` with `Tool: <toolName>`, `Target Locator: <selector>`, and active compound milestones.
  2. Added prompt rules in `quality-judge-discussion-prompt.md` strictly prohibiting the Judge from redirecting interactive element locators (buttons, links, inputs) to parent containers.
  3. Added programmatic safety guardrails (`isContainerHijack`) in `QualityJudgeToolInterceptor` preventing interactive candidates from being replaced by ancestor containers via syntactic CSS hierarchy checks and live DOM Level 3 containment verification.
- **Safety Net Added:** Unit regression tests in `QualityJudgePromptTest` (`testCompileDiscussionRequestWithActionAndMilestones`) and `QualityJudgeToolInterceptorTest` (`testIsContainerHijackDirectDetection`, `testDiscussionRejectsContainerHijackInConsensus`).

### [DEF-20260919-03] Asymmetric Tool Call Cloning across Compound Sub-Steps causing Replay Over-execution and Assertion Desynchronization
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `ExecuteActionsStep`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.replayAllDataSets` (`perfect` and `bad`), replay failed. In `perfect`, Step #4 added an extra item to the cart, causing `AssertionError: Expected "CART 2" but found "CART 3"`. In `bad`, Step #3 clicking Add triggered an HTMX swap of `#cart-btn-wrapper`, leaving `#cart-btn-anchor` detached during action 4 (`assert_text`), causing `ElementNotFound: Element not found {#cart-btn-anchor}`.
- **Root Cause:** When a compound turn group had fewer executed tool calls than sub-steps (e.g. in `bad` Substep 3.4 was a skipped conditional `When this string 'bad' is not equal 'bad', click size 'S'`, resulting in 4 tool calls for 5 sub-steps; in `perfect` Step 4 had 4 tool calls for 5 sub-steps because store was skipped), `AgentToolLoopStep` fallback dumped the full list of tool calls into *every* child sub-step (`child.setToolCalls(sanitizedCalls); child.setActions(actions)`). In replay mode, `ExecuteActionsStep` scheduled each child sub-step sequentially, causing all 4 actions to execute on Substep 1, and all 4 actions to execute again on Substep 2.
- **Detection Gap ("What did we miss?"):** Prior unit tests for compound turn groups only tested 1:1 matching of tool calls to sub-steps or monolithic parent steps, without testing asymmetric counts (skipped conditional sub-steps, skipped store calls) or verifying that individual child sub-steps do not receive duplicate cloned tool lists.
- **Resolution:**
  1. Replaced the cloned fallback in `AgentToolLoopStep` with `partitionToolCallsAndActions`, which sequentially correlates executed tool calls and actions to sub-steps based on instruction keywords and semantic intent, mapping skipped conditional branches to empty tool lists.
  2. Added auto-healing in `ExecuteActionsStep.mapPlaybookStepToPipelineStep` during replay to detect pre-existing playbooks with cloned tool calls (`hasCorruptedClonedCalls`) and dynamically re-partition them on the fly.
- **Safety Net Added:** Added unit regression tests in `ExecuteActionsStepTest` (`testPartitionToolCallsAndActionsAsymmetricMatching`, `testAutoHealsCorruptedClonedToolCallsInReplayMode`), and verified integration replay passes across all datasets.

### [DEF-20260919-02] Ancestor Automation ID Hijacking in Compound Selectors and Missing Sub-Step Activities/Screenshots in Replay
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`SelenideElementFinder`, `ClickAction`, `ExecuteActionsStep`, `PreliminaryReportListener`, `PlaybookToolReplayer`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.replayNormal`, Step #3 fails at substep 3.5 with `Expected text/pattern "CART 1" was not found on selector "#cart-btn-anchor" nor anywhere on the page within 3000ms`. Cart badge remains 0 because the size button in the dynamic quick-add dropdown was never clicked. Furthermore, in the HTML report, all sub-steps of the compound step displayed 0 activities and 0 screenshots.
- **Root Cause:**
  1. **Selector Hijacking:** In `SelenideElementFinder.tryResolveAutomationId`, regex matching extracted the first automation ID token in the selector (`xcboo7um`, belonging to the ancestor `<article>`). When the un-stamped dynamic size button failed to match, line 724 queried `[data-ai='xcboo7um']`, returned the visible `<article>`, and bypassed `PageAnalyzer.captureSimplifiedDom`. The replayer clicked the product card container instead of the size button.
  2. **Monolithic Replay Execution:** In `ExecuteActionsStep`, `isLegacySubStepReplay` was guarded by `!parentHasToolCalls`. Because the parent step had recorded tool calls, compound steps were executed as a single monolithic block in replay mode. Sub-steps were never scheduled in the pipeline, so no sub-step lifecycle events (`StepStartedEvent`, `StepFinishedEvent`), sub-step screenshots, or sub-step actions were recorded.
  3. **Missing Sub-Step Action Population:** `PreliminaryReportListener` failed to transfer `childStep.getActions()` into `childEntry` when populating sub-steps.
- **Detection Gap ("What did we miss?"):** Prior unit tests validated compound turn groups in live mode with static instructions, but did not test sequential sub-step unrolling during replay mode, nor did they verify that `ReportStepEntry` sub-steps received their corresponding actions and screenshots.
- **Resolution:**
  1. Updated `SelenideElementFinder.tryResolveAutomationId` to transform all `#xc...` tokens, prioritize `PageAnalyzer.captureSimplifiedDom` on missing elements, and strictly forbid bare `[data-ai='neoId']` fallbacks on compound selectors.
  2. Added `element.shouldBe(Condition.visible)` in `ClickAction` before clicking to ensure dynamic elements are ready for interaction.
  3. Updated `ExecuteActionsStep` to schedule sub-steps in sequence during replay mode (`executionMode.isReplay()`), capturing individual sub-step screenshots, statuses, and durations.
  4. Updated `PlaybookToolReplayer` to dispatch `ActionExecutedEvent` during replay.
  5. Updated `PreliminaryReportListener` to copy actions and screenshots to sub-step report entries.
- **Safety Net Added:** Added regression tests in `SelenideElementFinderTest`, `ExecuteActionsStepTest`, and `PreliminaryReportListenerTest`, and verified `CartTest.replayNormal` passes end-to-end with full sub-step activities and screenshots in the report.

### [DEF-20260919-01] Premature Strict Variable Resolution on Compound Steps with Runtime Placeholders
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`ai-pipeline`, `playbook-engine`, `ai-runner`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.liveAllDataSets` (and any playbook step containing dynamic runtime placeholders to be captured on the fly, such as `${lineItemCount}`), step execution fails immediately with `UnresolvableVariableException: Unresolvable variable placeholder '${lineItemCount}' in template: ...` before any browser actions or capture tools can execute.
- **Root Cause:** In `ExecuteActionsStep.mapPlaybookStepToPipelineStep`, compound turn groups and leaf steps invoked strict `contextState.getSessionData().resolveVariables(...)` when resolving instructions and milestones. Because dynamic variables captured during the step (e.g. via `store`) do not exist in `SessionData` at step start, strict resolution threw an `UnresolvableVariableException`. `SessionData.resolveAvailableVariables(...)` was designed specifically to leniently resolve known variables (like `${testId}`) while leaving dynamic placeholders intact, but `ExecuteActionsStep` invoked strict `resolveVariables`. In addition, `StateMachineRunner` used strict resolution in optional and bug step failure logging, risking secondary unhandled exceptions.
- **Detection Gap ("What did we miss?"):** Existing unit tests for compound turn groups in `ExecuteActionsStepTest` (`testCompoundTurnGroupMapsToSingleStepWithMilestonesInLiveMode`) tested instructions with static strings only, without dataset variables or dynamic placeholders.
- **Resolution:**
  1. Updated `ExecuteActionsStep.mapPlaybookStepToPipelineStep` to use `contextState.getSessionData().resolveAvailableVariables(...)` for both the main instruction and internal milestone sub-steps.
  2. Updated `StateMachineRunner` to use `resolveAvailableVariables(...)` when logging bug and optional step failures.
  3. Updated report and linting listeners (`PreliminaryReportListener`, `PostFlightPlaybookLinter`, `PlaybookLinterPrompt`) to use `resolveAvailableVariables(...)` so known variables are resolved cleanly without aborting on uncaptured placeholders.
- **Safety Net Added:** Added unit regression tests in `ExecuteActionsStepTest` (`testCompoundTurnGroupWithRuntimeVariablesPreservesPlaceholdersWithoutFailing`, `testLeafStepWithRuntimeVariablesPreservesPlaceholdersWithoutFailing`) asserting that available variables resolve while runtime placeholders are preserved without throwing.

### [DEF-20260918-05] Visual SSIM Threshold Step Mutation, Missing Parameterized Tag Overrides, and Config Alias Shadowing
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-model`, `ai-pipeline`, `ai-config`, `ai-junit`)
- **Scope:** `Framework`
- **Symptom:** Inability to override visual assertion SSIM thresholds per step (in YAML playbooks) or per test case via `@AiVisual`. In addition, replay gate execution unconditionally mutated `PlaybookStep.ssimMinScore` from `null` to `0.99`, polluting JSON companion recordings with unintended `ssimMinScore` fields, while static file defaults in `ai.properties` shadowed alias overrides in system properties and thread-local data.
- **Root Cause:**
  1. `VisualBaselineGateStep.execute` and `executePostActionCheck` unconditionally invoked `this.step.setSsimMinScore(minScore)` with the global config score `0.99`. This changed `ssimMinScore` from `null` to `0.99`, causing Jackson (`@JsonInclude(NON_NULL)`) to write `ssimMinScore` into JSON companion files on replay finish, breaking the contract that `(visual)` must use global configuration and not be hardcoded into JSON.
  2. `PlaybookStep` lacked support for comma-separated parameters in visual tags (e.g. `(visual: threshold=0.98)` or `(visual: full,threshold=0.98)`).
  3. `AiConfiguration.getProperty` checked the property file for the primary key (`neodymium.ai.ssim.minScore`) before evaluating fallback aliases, so the default `neodymium.ai.ssim.minScore = 0.99` in `ai.properties` masked dynamic or system property overrides on aliases like `neodymium.ai.visual.threshold`.
- **Detection Gap ("What did we miss?"):** Existing gate tests in `VisualBaselineGateStepTest` only asserted visual matching/divergence behavior, never verifying that `step.getSsimMinScore()` remained `null` for steps tagged with `(visual)`. Configuration tests verified individual properties but did not test alias override precedence against properties file defaults.
- **Resolution:**
  1. Created `@AiVisual` annotation supporting `value()` and `threshold()` attributes for test class and method level SSIM overrides in `NeodymiumAiRunner`.
  2. Updated `PlaybookStep` to support parameterized visual tags: `(visual: threshold=0.98)`, `(visual: full,threshold=0.98)`, percentage formats (`98%`), and `@JsonProperty("threshold")` / `@JsonAlias("threshold")` aliases.
  3. Updated `VisualBaselineGateStep` to evaluate `step.getSsimMinScore()` when present and avoid mutating `step.ssimMinScore` if it was initially `null`.
  4. Updated `AiConfiguration.getVisualSsimMinScore()` to enforce proper multi-tier precedence: thread-local data (`Neodymium.getData()`) -> system properties -> properties files -> default fallback `0.99`.
  5. Updated `PostFlightPlaybookLinter` and `PreliminaryReportListener` to support parameterized visual tags and report accurate thresholds.
- **Safety Net Added:** Added unit regression tests in `PlaybookStepTest` (`testVisualTagVariantsAndThresholdParsing`, `testVisualSerializationExclusionWhenNull`, `testThresholdDeserializationJsonAlias`), `VisualBaselineGateStepTest` (`testReplayWithCustomStepThreshold_passesBelowDefaultThreshold`), `AiConfigurationTest` (`testVisualSsimMinScoreDefaultsAndAliases`), and `NeodymiumAiRunnerTest` (`testAiVisualAnnotationHandling`).

### [DEF-20260918-04] Sub-Step Unrolling Amnesia, Global Scoping Leakage, and Descendant Selector Overscoring in Compound Steps
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-util`, `playbook-engine`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.liveBad`, execution timed out during `Verify that the cart item count is higher than ${lineItemCount}.` with `AssertionError: Cart line item count was not greater than 0 within 3000ms`. The LLM searched for the shopping cart badge inside the product card's DOM fragment and repeatedly evaluated irrelevant child elements.
- **Root Cause:**
  1. **Sub-Step Unrolling & Scoping Leakage:** `ExecuteActionsStep` previously unrolled indented YAML turn groups (e.g. `Locate the first product card:`) into independent pipeline steps, setting each sub-step's parent to the group header. In `AgentToolLoopStep`, this injected `### Scoping Context: Locate the first product card:` into global assertion sub-steps, misleading the agent into searching for global header elements (cart badge) within the scoped product card.
  2. **Conversational Amnesia:** Each unrolled sub-step started a brand new isolated tool loop, causing the agent to lose context of the actions it had just taken in the preceding sub-steps of the group.
  3. **Descendant Selector Overscoring:** `LocatorImprover.scoreLocator` scored any selector containing `#` as a perfect 10/10, even if it contained descendant combinators (e.g. `#prod-info div`). This bypassed Quality Judge deliberation and locked the agent into fragile descendant selectors.
- **Detection Gap ("What did we miss?"):** Existing composite step tests (`CompositeStepTest`) only validated sequential execution order and serialization for replay, but never evaluated live LLM interactions where a turn group combines contextual actions (locating, hovering, clicking) with a global assertion (verifying header cart badge count). `LocatorImproverTest` verified single ID selectors like `#submit-btn`, but lacked assertions ensuring descendant combinators were penalized.
- **Resolution:**
  1. Updated `ExecuteActionsStep.mapPlaybookStepToPipelineStep` to execute turn groups as a single compound `AgentToolLoopStep` with milestones, restricting unrolling strictly to `_include:` files and legacy sub-step replays.
  2. Tightened `LocatorImprover.scoreLocator` to require `!trimmed.contains(" ") && !trimmed.contains(">")` before awarding 10/10 to ID selectors.
  3. Refined `AgentToolLoopStep` multi-turn prompt guidance to instruct the agent to fulfill all milestone actions and commanded verifications before calling `complete_step`.
  4. Updated step completion hooks in `ExecuteActionsStep`, `AgentToolLoopStep`, and `StateMachineRunner` to distribute status, duration, actions, and tool calls to child sub-steps for report fidelity.
- **Safety Net Added:** Added unit regression tests in `ExecuteActionsStepTest` (`testCompoundTurnGroupMapsToSingleStepWithMilestonesInLiveMode`, `testIncludeStepUnrollsSubSteps`) and `LocatorImproverTest` (`testScoreLocatorDescendantCombinatorWithIdNotPerfectScore`).

### [DEF-20260918-03] Multi-Turn DOM Amnesia, Tool Thrashing, and Discovery Tool Action Pollution in Agent Tool Loop
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-tool`)
- **Scope:** `Framework`
- **Symptom:** In multi-field form steps (such as filling credit card number, expiry date, and CVV), the agent executed redundant discovery tools (`query_dom`), blind exploratory scrolling (`scroll down`), and re-querying across 7 turns. The read-only discovery tools and blind scrolls were erroneously recorded as persistent test actions in the execution report and playbook (`QUERY_DOM`, `SCROLL`).
- **Root Cause:**
  1. `pruneExpiredDomFromConversation` unconditionally wiped the DOM from Turn 1 without verifying if a replacement DOM was being provided for Turn 2 (`requireDomForNextTurn == false`), completely blinding the LLM of selectors.
  2. The continuation prompt actively nudged the model to call `query_dom`.
  3. `query_dom` used case-sensitive CSS selectors, missing camelCase attributes (`cardExpiry`, `cardCvv`), prompting an uncommanded off-screen scroll.
  4. `finishLoop` failed to filter out discovery/inspection tools (`query_dom`, `inspect`, `request_context`), polluting `PlaybookStep.actions` and `PlaybookStep.toolCalls`.
  5. Strict single-action serialization prevented the agent from proposing cohesive form field inputs in a single turn.
- **Detection Gap ("What did we miss?"):** Tests verified that `pruneExpiredDomFromConversation` pruned messages to save tokens, but did not verify that selector accessibility was maintained across multi-action turns or that discovery tools were excluded from recorded playbook actions.
- **Resolution:**
  1. Updated `pruneExpiredDomFromConversation` to preserve DOM/element selectors when no new DOM snapshot is added for the next turn.
  2. Excluded read-only discovery tools (`query_dom`, `inspect`, `request_context`, `inspect_visual`) from recorded `actions` and `toolCalls` in `finishLoop`.
  3. Enabled case-insensitive attribute fallback in `query_dom` JavaScript.
  4. Allowed cohesive form field action batching for multi-input instructions.
- **Safety Net Added:** Added unit and integration tests verifying that multi-field steps execute cleanly without amnesia, that discovery tools are not recorded as playbook actions, and that `query_dom` handles case-insensitive attribute matching.

### [DEF-20260918-02] Elimination of Fragile Natural Language Text Guessing in SemanticIntent
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-model`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** `SemanticIntent.inferFromInstruction` attempted to deduce step intent from natural language instruction text using hardcoded English substring patterns (`contains(" says now ")`, `contains(" is shown")`, `contains(" is mentioned")`), introducing fragile keyword-guessing heuristics into core Java logic and violating Neodymium's universal, language-neutral design.
- **Root Cause:** Following the removal of PESAP (which previously supplied LLM-classified intent), an ad-hoc keyword-matching method `inferFromInstruction` was introduced in commit `96b9c315f` to guess intent for unclassified steps, embedding English-specific phrasing assumptions into Java core logic.
- **Detection Gap ("What did we miss?"):** Unit tests only verified that `inferFromInstruction` matched specific predefined English test sentences, without verifying language neutrality or handling diverse phrasings.
- **Resolution:** Removed `SemanticIntent.inferFromInstruction` completely. Reverted `AgentToolLoopStep` to rely strictly on explicitly configured or recorded `SemanticIntent` (or `null` when unspecified), allowing the LLM's system prompt instructions to govern action vs. verification execution without heuristic second-guessing.
- **Safety Net Added:** Cleaned `SemanticIntentTest` to eliminate keyword inference tests; verified that `AgentToolLoopStepTest` executes cleanly without keyword guessing.

### [DEF-20260918-01] Premature Step Completion in Multi-Field Action Instructions Due to Single-Action Prompt Nudge
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `GermanCheckoutTest.live` ([GermanCheckoutTest_live_perfect_20260918-002600.html](file:///home/rschwietzke/projects/GIT/neodymium-library/target/ai-results/GermanCheckoutTest_live_perfect_20260918-002600.html)), Step 18 (`Warte bis der Text "Thank you for your purchase!" erscheint.`) timed out with `AssertionError: Expected text/pattern "Thank you for your purchase!" was not found anywhere on the page within 3000ms`.
- **Root Cause:** In Step 16 (`Kartennummer ist '4111 1111 1111 1111', Ablaufdatum '12/29' und CVV ist '111'.`), the agent filled `#cardNumber` in Turn 1. In Turn 2, the post-action turn prompt introduced in DEF-20260917-01 (*"If this was an action instruction, invoke 'complete_step' now without performing uncommanded assertions or anticipating subsequent steps"*) combined with Operating Rule 4 (*"the step goal is completely satisfied once the action executes"*) caused the LLM to call `complete_step` prematurely, leaving `#cardExpiry` and `#cardCvv` empty. Clicking purchase in Step 17 failed client-side validation, so the confirmation page never loaded.
- **Detection Gap ("What did we miss?"):** DEF-20260917-01 tested atomic single-action steps to verify that the agent did not perform uncommanded assertions, but did not test compound action steps requiring multiple sequential `fill` or `click` actions within a single instruction.
- **Resolution:**
  1. Updated `AgentToolLoopStep` post-action turn prompt to state: *"If this was an action instruction and all actions/fields requested in the instruction have been executed, invoke 'complete_step' now without performing uncommanded assertions or anticipating subsequent steps. If the instruction explicitly requested additional fields or actions that have not yet been executed, continue executing the remaining actions."*
  2. Updated Operating Rule 4 in `AgentToolLoopStep` to clarify that multi-action instructions are satisfied only when all explicitly commanded actions/fields are fulfilled.
- **Safety Net Added:** Added regression unit test `AgentToolLoopStepTest#testMultiFieldActionInstructionReceivesRefinedTurnPromptAndAllowsSequentialExecution` verifying that a multi-field fill instruction continues across turns until all requested fields are filled before invoking `complete_step`.

### [DEF-20260917-01] Premature Fast-Break in assert_text Wait Loop and Agent Over-Verification in Action Steps
- **Date:** 2026-09-17
- **Component:** `neodymium-core` (`ai-tool`, `ai-pipeline`, `ai-model`)
- **Scope:** `Framework`
- **Symptom:** During strict replay of recorded playbooks (such as `CheckoutTest.replay` on the `bad` dataset), Step 3 (`Locate the first product card and click its 'Add to Cart' or 'Add' button.`) failed unexpectedly with `Tool execution error in 'assert_text': Expected text "CART 1" was not found on element "#cart-btn-anchor"`, aborting execution before reaching Step 4 (`The mini cart quantity is now 1.`).
- **Root Cause:**
  1. Operating rules in `AgentToolLoopStep` universally commanded *"You MUST invoke an assertion tool before calling 'complete_step'"* without separating action steps from verification steps. During live recording, the LLM felt obliged to assert the cart counter after clicking, anticipating the next step and recording an uncommanded `assert_text` into Step 3's playbook.
  2. In `BrowserToolProvider.assert_text`, the polling retry loop contained a premature `if (isTextPresentOnPage(...)) break;`. When the asynchronous HTMX response (`api/cart/add`) began arriving and updated text anywhere on the page, the tool prematurely broke out of its retry loop rather than polling the target element up to `Configuration.timeout`.
- **Detection Gap ("What did we miss?"):** Existing `assert_text` unit tests verified immediate matches and page-wide fallbacks with mock drivers, but did not test asynchronous DOM mutation scenarios where text appears elsewhere on the page while the target element is still updating.
- **Resolution:**
  1. Removed the premature `isTextPresentOnPage(...) -> break` abort from `BrowserToolProvider.assert_text`, ensuring the tool polls the target element for the full `Configuration.timeout` duration.
  2. Differentiated action vs assertion execution in `AgentToolLoopStep` prompts (with keyword guessing subsequently eliminated in [DEF-20260918-02]).
  3. Restructured `AgentToolLoopStep` operating rules into distinct `Action steps` and `Verification steps` sections and updated post-action turn guidance to complete action steps immediately without uncommanded assertions.
- **Safety Net Added:**
  - `BrowserToolProviderStabilityTest#testAssertTextWaitsForAsyncTargetElementUpdateEvenIfTextIsPresentElsewhereOnPage` ensuring `assert_text` polls continuously until target element updates.

### [DEF-20260916-05] Complete Removal of PESAP Tracing & Reporting and Prompt Cleanliness
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-report`, `ai-runner`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** Reports (HTML, Markdown, JSON) continued to render "PESAP (Pre-Execution Semantic Anchor)" category rows and badge cards with zero calls. StateMachineRunner and AiSession logged empty PESAP metrics in console banners, and AgentToolLoopStep appended conversational "What is your next tool call?" trailing prompts to user turns.
- **Root Cause:**
  1. HTML, Markdown, and JSON report generators retained hardcoded category rows, badges, and JavaScript/CSS handlers for PESAP metrics.
  2. StateMachineRunner, AiSession, and InteractiveStateBuilder retained legacy PESAP token accumulators and banner debug statements.
  3. AgentToolLoopStep retained conversational scaffolding ("What is your next tool call?") originally meant for text-completion agents rather than native function-calling APIs.
- **Detection Gap ("What did we miss?"):** Report unit tests asserted the presence of the PESAP row rather than validating that decoupled/deprecated phases are omitted from user-facing accounting.
- **Resolution:**
  1. Removed "PESAP (Pre-Execution Semantic Anchor)" row and all phase badge/JS logic from HtmlReportGenerator and MarkdownReportGenerator.
  2. Removed PESAP accumulation and console tracing from StateMachineRunner, AiSession, and InteractiveStateBuilder.
  3. Removed "What is your next tool call?" prompts from AgentToolLoopStep turns and updated DOM pruning to be content-boundary-based.
  4. Updated PreliminaryReportListenerTest to assert that PESAP is absent from report tables and JSON metrics.
- **Safety Net Added:** Automated assertions in `PreliminaryReportListenerTest#testReportTokenAccountingAndCategoryBreakdown` verifying absence of "PESAP" in HTML, Markdown, and JSON reports.

### [DEF-20260916-04] Silent Test Pass on Mismatched @AiDataSet Filter
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-junit`)
- **Scope:** `Framework`
- **Symptom:** Running test classes like `SearchGermanTest` resulted in `Tests run: 0, Failures: 0, Errors: 0, Skipped: 0` and reported Maven build success without executing any actual test steps.
- **Root Cause:**
  1. `NeodymiumAiRunner.provideTestTemplateInvocationContexts` filtered datasets against method- and class-level `@AiDataSet` annotations using `shouldIncludeDataSet`.
  2. When `@AiDataSet` specified dataset IDs or includes that did not match any dataset defined in the referenced playbook YAML (e.g. `@AiDataSet("perfect")` on `SearchGermanTest` vs playbook datasets `['US', 'DE', 'FIN']`), `filteredDataSets` resulted in an empty list.
  3. The runner returned an empty stream of `TestTemplateInvocationContext`, which JUnit 5 interpreted as 0 invocations, producing a silent green build.
  4. Multiple test classes (`SearchGermanTest`, `EnglishCheckoutTest`, and `RegisterTest` in `basic` and `full`) carried stale `@AiDataSet` annotations or dead test methods from earlier template copies.
- **Detection Gap ("What did we miss?"):** JUnit 5 `@TestTemplate` test engines do not consider zero invocations as an error by default. Test runners did not validate that explicit user-provided `@AiDataSet` filters matched at least one dataset in the playbook before generating test invocations.
- **Resolution:**
  1. Corrected `@AiDataSet("DE")` in `SearchGermanTest` and `@AiDataSet("canada-fr")` in `EnglishCheckoutTest`.
  2. Pruned dead test methods in basic and full `RegisterTest` that referenced non-existent datasets.
  3. Added fail-fast validation in `NeodymiumAiRunner.provideTestTemplateInvocationContexts` that throws `IllegalArgumentException` with available dataset IDs whenever explicit `@AiDataSet` filters match zero datasets.
- **Safety Net Added:** Unit test `NeodymiumAiRunnerTest#testUnmatchedDataSetThrowsException` verifying that unmatched `@AiDataSet` triggers fail-fast `IllegalArgumentException`.

### [DEF-20260916-03] PESAP Pipeline Decoupling & Direct Agent Execution Migration
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** Upfront PESAP (Pre-Execution Step Analysis & Partitioning) introduced mandatory out-of-band LLM calls prior to action execution, created architectural tight coupling with regex/language heuristics, and constrained instruction autonomy in `AgentToolLoopStep`.
- **Root Cause:** PESAP was originally designed as a speculative pre-classifier that partitioned steps and guessed context levels before seeing actual browser execution results. This architectural separation created redundancy with the agent tool loop and violated language neutrality by encouraging intent keyword sniffing and premature DOM exclusion hacks.
- **Detection Gap ("What did we miss?"):** Early AI test designs tested PESAP in isolation via `PesapPreStepTest`, rather than measuring end-to-end latency, multilingual robustness, and tool-loop adaptability without upfront classification overhead.
- **Resolution:**
  1. Decoupled `PesapPreStep` from `ExecuteActionsStep.mapPlaybookStepToPipelineStep()`, enabling direct tool loop execution for live recording steps.
  2. Deprecated `PesapPreStep`, `PesapPrompt`, and `isPesapEnabled()`, defaulting PESAP configuration to `false`.
  3. Streamlined `AgentToolLoopStep` Turn 1 SUT capture to universal `ContextLevel.LEAN` baseline and refined completion/verification prompt guidance.
- **Safety Net Added:** Comprehensive test suite execution across `AgentToolLoopStepTest` (38 tests) and `ExecuteActionsStepTest` (9 tests) ensuring direct agent autonomous execution succeeds without upfront PESAP calls.

### [DEF-20260916-02] Pre-Action Visual Baseline Check Prematurely Aborted Replay for Visual Steps with Mutating Actions
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `SearchTest_German_testSearchDeReplayDe_DE`, step 11 (`Auf der linken Seite wird eine Box mit Kategorien, Farben, Preis und Angebot angezeigt (visual).`) failed during replay with `DivergenceException: Visual SSIM score below threshold (score: 0.5352 < 0.99)` because the page was not scrolled down.
- **Root Cause:**
  1. During live recording, the LLM executed an interactive/viewport action `scroll(direction: "down", yOffset: 300)`. `VerifyOutcomeStep` captured the post-action screenshot at `scrollY = 300` and saved its SSIM matrix as `step.screenshotHash`.
  2. During replay, `VisualBaselineGateStep.executeGate()` was invoked before replaying step actions (at `scrollY = 0`).
  3. `VisualBaselineGateStep` assumed all visual steps (`step.isVisualStep()`) were pure verification steps and evaluated live pre-action screen state against `step.screenshotHash`. When the pre-action screen did not match the post-action baseline (SSIM 0.5352 < 0.99), line 235 threw a `DivergenceException` immediately, preventing `PlaybookToolReplayer.replayStep()` from ever executing the recorded `SCROLL` action.
- **Detection Gap ("What did we miss?"):**
  - Existing unit tests for `VisualBaselineGateStep` only tested visual steps with zero actions or with `ASSERT` actions (`Action("ASSERT", ...)`); none tested steps containing mutating or viewport actions like `SCROLL` or `CLICK`.
  - The pipeline assumed `step.isVisualStep()` implied a pure verification step with no mutating actions, ignoring cases where an LLM calls non-mutating tools or scroll actions during visual assertions.
- **Resolution:**
  1. Added `VisualBaselineGateStep.isPureVerification()` to distinguish steps that only contain assertions/NO-OPs from steps that contain recorded mutating actions or tool calls (`SCROLL`, `CLICK`, etc.).
  2. Updated `VisualBaselineGateStep.executeGate()` to defer visual baseline comparison when `!isPureVerification() && coordinateTarget == null`, allowing recorded actions to execute first without throwing pre-action divergence.
  3. Added `VisualBaselineGateStep.executePostActionCheck()` and wired it into `ExecuteActionsStep`'s `standardFlow` after action replay and post-step state settling/capture to verify the post-action visual outcome against the recorded baseline.
- **Safety Net Added:**
  - Added 4 unit tests in `VisualBaselineGateStepTest`:
    - `testReplayWithMutatingAction_preActionGateDoesNotThrowDivergence()`
    - `testReplayWithMutatingAction_postActionCheckWithMatchingBaseline_succeeds()`
    - `testReplayWithMutatingAction_postActionCheckWithDivergentBaseline_throwsDivergenceException()`
    - `testReplayWithMutatingAction_postActionCheck_supportsHealing_throwsHealingRequiredException()`
  - Added 2 integration tests in `ExecuteActionsStepTest`:
    - `testReplayVisualStepWithActionExecutesActionThenVerifiesPostActionVisualBaseline()`
    - `testReplayVisualStepWithActionThrowsDivergenceWhenPostActionVisualBaselineDiffers()`

### [DEF-20260916-01] Full-Page Visual Assertion Baseline Recorded As Viewport Capture During Multi-Turn Tool Loops
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `VerlaGuestCheckout_Pl_Polish_testCheckoutReplayPerfect`, step 19 (`Na środku ekranu znajduje się zielony znacznik wyboru (visual: full).`) failed during replay with SSIM 0.6878 against the recorded baseline (required >= 0.99), causing a `DivergenceException`.
- **Root Cause:**
  1. In `AgentToolLoopStep`, for visual assertions, `activeContextLevel` is `ContextLevel.VISUAL`, where `ContextLevel.isFullPageScreenshot()` evaluates to `false`. When an agent loop required multiple turns (e.g. recovering from an invalid tool call), subsequent turns refreshed page state using `executor.captureState(activeContextLevel, activeContextLevel.isFullPageScreenshot())`. This captured a 1500x857 viewport screenshot and overwrote `KEY_LAST_STATE`.
  2. In `VerifyOutcomeStep`, for steps without mutating DOM actions (`step.getActions().isEmpty()`), the pipeline previously discarded `KEY_POST_ACTION_STATE` (which had been freshly captured as full-page by `ExecuteActionsStep`) and fell back to `KEY_LAST_STATE` (overwritten with the viewport capture). It hashed that viewport capture while marking `step.setFullPage(true)`.
  3. During replay, `VisualBaselineGateStep` checked `step.isFullPageVisualStep()`, correctly captured a 1500x1122 full-page screenshot, and compared it against the recorded 1500x857 viewport hash, causing SSIM 0.6878 < 0.99 (`DivergenceException`).
- **Detection Gap ("What did we miss?"):**
  - Existing test `testVisualStepDoesNotConsumeStalePostActionState` in `VerifyOutcomeStepTest` specifically tested that viewport visual steps without actions discard `KEY_POST_ACTION_STATE` in favor of viewport `KEY_LAST_STATE`, but did not test full-page visual steps (`(visual: full)` or `step.isFullPageVisualStep()`).
  - Unit tests for `AgentToolLoopStep` did not verify the `isFullPage` flag passed to `captureState` during intermediate turns.
- **Resolution:**
  1. Updated `AgentToolLoopStep` lines 357, 1086, and 1195 so that initial, intermediate, and visual observation state captures evaluate `isFullPage` considering `(step != null && step.isFullPageVisualStep()) || Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT")) || activeContextLevel.isFullPageScreenshot()`.
  2. Updated `VerifyOutcomeStep` lines 116-124 to preserve and consume `KEY_POST_ACTION_STATE` whenever `isFullPageReq` is true, even when `step.getActions()` is empty.
- **Safety Net Added:**
  - Added unit test `testVisualStepWithFullPagePreservesPostActionState` in `VerifyOutcomeStepTest`.
  - Added unit test `testMultiTurnFullPageVisualStepPreservesFullPageCapture` in `AgentToolLoopStepTest`.
  - Added full-page capture tracking (`capturedFullPageFlags`) in `MockTargetExecutor`.
