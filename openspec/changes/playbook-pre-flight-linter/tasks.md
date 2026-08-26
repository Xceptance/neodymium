## 1. Capabilities & Configuration

- [ ] 1.1 Add `LINTER` to `LlmCapability` and register in provider capability sets (`GeminiLlmProvider`, `MistralLlmProvider`, `VertexAiLlamaProvider`, `CachingLlmProvider`, `MockLlmProvider`)
- [ ] 1.2 Add linter configuration methods to `AiConfiguration` (`isLinterEnabled`, `getLinterProvider`, `getLinterModel`)

## 2. Linter Domain Model & Prompting

- [ ] 2.1 Create `PlaybookLinterFinding`, `LinterCategory`, and `LinterSeverity` domain models in `org.neodymium.ai.playbook.linter`
- [ ] 2.2 Create `playbook-linter-prompt.md` system prompt covering compound steps, missing visual tags, ambiguous affordances, and vague target anchors
- [ ] 2.3 Implement `PlaybookLinterPrompt` and JSON response parser in `org.neodymium.ai.prompt`
- [ ] 2.4 Implement `PlaybookLinter` service with graceful error handling and provider routing in `org.neodymium.ai.playbook.linter`

## 3. Session & Execution Integration

- [ ] 3.1 Integrate pre-flight linting invocation in `StateMachineRunner` session initialization before the step execution loop
- [ ] 3.2 Store findings in `ExecutionContext` transient data for report listeners

## 4. Test Reporting & Output

- [ ] 4.1 Add linter findings collection and mapping to `TestExecutionReport` and `PreliminaryReportListener`
- [ ] 4.2 Update `MarkdownReportGenerator` to render the `Playbook Quality & Pre-Flight Findings` advisory table

## 5. Verification & Tests

- [ ] 5.1 Add unit tests for `PlaybookLinterPrompt` (JSON parsing, compound step detection)
- [ ] 5.2 Add unit tests for `PlaybookLinter` (enabled/disabled toggles, error recovery)
- [ ] 5.3 Add unit tests for `MarkdownReportGenerator` linter table rendering
- [ ] 5.4 Execute integration test on a multi-step scenario with compound instructions and verify report output
