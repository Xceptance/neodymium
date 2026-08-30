## 1. Capabilities & Configuration

- [ ] 1.1 Add `LINTER` to `LlmCapability` and register in provider capability sets (`GeminiLlmProvider`, `MistralLlmProvider`, `VertexAiLlamaProvider`, `CachingLlmProvider`, `MockLlmProvider`)
- [ ] 1.2 Add linter configuration methods to `AiConfiguration` (`isLinterEnabled`, `getLinterProvider`, `getLinterModel`)

## 2. Linter Domain Model & Prompting

- [ ] 2.1 Create `PlaybookLinterFinding` (with `stepIndex`, `lineNumber`, `sourceFile`, `rawInstruction`, `resolvedInstruction`), `LinterCategory` (`STEP_SPLITTING_CANDIDATE`, `MISSING_VISUAL_TAG`, `AMBIGUOUS_AFFORDANCE`, `VAGUE_TARGET`, `VAGUE_VERIFICATION`, `DANGLING_ANAPHORA`, `TEMPORAL_FLOW_ANOMALY`, `HARDCODED_VOLATILE_DATA`), and `LinterSeverity` in `org.neodymium.ai.playbook.linter`
- [ ] 2.2 Create `playbook-linter-prompt.md` system prompt with multilingual rules and few-shot examples (English, German, Japanese), covering all 8 domain-neutral categories and strict same-language rewrite suggestions
- [ ] 2.3 Implement `PlaybookLinterPrompt` and JSON response parser in `org.neodymium.ai.prompt` with support for optional explicit `description:` context
- [ ] 2.4 Implement `PlaybookLinter` service with graceful error handling, token accounting, and provider routing in `org.neodymium.ai.playbook.linter`

## 3. Session & Execution Integration

- [ ] 3.1 Integrate pre-flight linting invocation in `StateMachineRunner` session initialization before the step execution loop
- [ ] 3.2 Extract optional scenario description from playbook YAML `description:` header or test `@Description` annotation
- [ ] 3.3 Store findings in `ExecutionContext` transient data for report listeners

## 4. Test Reporting & Output

- [ ] 4.1 Add linter findings collection and mapping to `TestExecutionReport` and `PreliminaryReportListener`
- [ ] 4.2 Update `MarkdownReportGenerator` to render the `Playbook Quality & Pre-Flight Findings` advisory table with line numbers, dual raw/resolved step views, categories, severity, and suggested rewrites

## 5. Verification & Tests

- [ ] 5.1 Add multilingual unit tests for `PlaybookLinterPrompt` (English, German, Japanese, compound step splits, visual scope tagging, explicit description context)
- [ ] 5.2 Add unit tests for `PlaybookLinter` (enabled/disabled toggles, error recovery, mock LLM provider integration)
- [ ] 5.3 Add unit tests for `MarkdownReportGenerator` linter table rendering
- [ ] 5.4 Execute integration test on a multi-step scenario and verify report output
