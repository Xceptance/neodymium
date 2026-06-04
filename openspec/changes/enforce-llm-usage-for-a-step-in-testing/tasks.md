## 1. Step Parsing & Model Updates

- [ ] 1.1 Update the `PlaybookStep` (or equivalent execution model) to include an `enforceLlm` boolean flag.
- [ ] 1.2 Modify the test script parser to detect the `!(llm)` prefix in step strings.
- [ ] 1.3 Update the parser to strip the `!(llm)` marker from the visible step text and set the `enforceLlm` flag to true on the step model.

## 2. Execution Engine Updates

- [ ] 2.1 Update `AiBrowser` (and related execution logic) to check the `enforceLlm` flag before executing a step.
- [ ] 2.2 If `enforceLlm` is true, bypass local deterministic fallbacks and playbook caching.
- [ ] 2.3 Route the flagged step directly to the `LlmClient` for live evaluation.

## 3. Reporting and Telemetry

- [ ] 3.1 Update logging within the execution engine to indicate when a step is forced to use the LLM.
- [ ] 3.2 Ensure the final test report clearly marks the step as having used live LLM evaluation.

## 4. Verification

- [ ] 4.1 Write a unit test verifying that the `!(llm)` prefix is correctly parsed, stripped, and flags the step.
- [ ] 4.2 Write an integration test ensuring that a flagged step bypasses cache and hits the LLM processing path.
