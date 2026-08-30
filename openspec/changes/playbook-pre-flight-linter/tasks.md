## 1. Capabilities & Configuration

- [x] 1.1 Add `LINTER` to `LlmCapability` and register in provider capability sets (`GeminiLlmProvider`, `MistralLlmProvider`, `VertexAiLlamaProvider`, `CachingLlmProvider`, `MockLlmProvider`)
- [x] 1.2 Add `LINTER` to `ResponseSchema` enum and token resolution
- [x] 1.3 Add linter configuration methods to `AiConfiguration` (`isLinterEnabled`, `getLinterProvider`, `getLinterModel`) and `@AiLinter` JUnit annotation
- [x] 1.4 Add `ExecutionMode.LINTER_ONLY` for static pre-flight linting test execution without browser step dispatch

## 2. Linter Domain Model & Prompting

- [x] 2.1 Create `PlaybookLinterFinding` (with `stepIndex`, `lineNumber`, `sourceFile`, `rawInstruction`, `resolvedInstruction`), `LinterCategory` (`STEP_SPLITTING_CANDIDATE`, `MISSING_VISUAL_TAG`, `AMBIGUOUS_AFFORDANCE`, `VAGUE_TARGET`, `VAGUE_VERIFICATION`, `DANGLING_ANAPHORA`, `TEMPORAL_FLOW_ANOMALY`, `HARDCODED_VOLATILE_DATA`, `INCOMPLETE_BRANCH_CLAUSE`), and `LinterSeverity` in `org.neodymium.ai.playbook.linter`
- [x] 2.2 Create `playbook-linter-prompt.md` system prompt with multilingual rules and few-shot examples (English, German, Japanese), covering all 9 domain-neutral categories and strict same-language rewrite suggestions
- [x] 2.3 Implement `PlaybookLinterPrompt` and JSON response parser in `org.neodymium.ai.prompt` with support for optional explicit `description:` context
- [x] 2.4 Implement `PlaybookLinter` service with graceful error handling, token accounting (`KEY_LINTER_TOKEN_USAGE`, `KEY_LINTER_CALL_COUNT`), event emission (`LlmResponseReceivedEvent`, `LlmRequestSentEvent`), replay mode bypass, and provider routing in `org.neodymium.ai.playbook.linter`

## 3. Session & Execution Integration

- [x] 3.1 Integrate pre-flight linting invocation in `StateMachineRunner` session initialization before the step execution loop
- [x] 3.2 Extract optional scenario description from playbook YAML `description:` header or test `@Description` annotation
- [x] 3.3 Store findings in `ExecutionContext` transient data (`KEY_PLAYBOOK_LINTER_FINDINGS`) for report listeners
- [x] 3.4 Handle `ExecutionMode.LINTER_ONLY` in `StateMachineRunner` to complete tests immediately after pre-flight linting

## 4. Test Reporting & Output

- [x] 4.1 Add `linter` category token usage and `linterFindings` collection to `TestExecutionReport.ReportMetrics` and `TestExecutionReport`
- [x] 4.2 Update `PreliminaryReportListener` to aggregate linter token metrics in `recalculateMetrics()` and populate findings from `ExecutionContext`
- [x] 4.3 Update `MarkdownReportGenerator` to render the `Playbook Quality & Pre-Flight Findings` advisory table and `Linter (Pre-Flight)` in the LLM Responsibility Breakdown
- [x] 4.4 Update `HtmlReportGenerator` to display pre-flight findings, high-contrast original step box, suggested rewrites, and collapsible accordion card

## 5. Verification & Tests

- [x] 5.1 Add multilingual unit tests for `PlaybookLinterPrompt` (English, German, Japanese, compound step splits, visual scope tagging, explicit description context, incomplete branches)
- [x] 5.2 Add unit tests for `PlaybookLinter` (enabled/disabled toggles, replay bypass, error recovery, mock LLM provider integration, token accounting)
- [x] 5.3 Add unit tests for `MarkdownReportGenerator` and `HtmlReportGenerator` linter findings rendering
- [x] 5.4 Add live challenge integration test suite (`PrelinterChallengeIntegrationTest.java`) exercising all 9 quality rules in `ExecutionMode.LINTER_ONLY`
- [x] 5.5 Execute full test suite and clean code audit
