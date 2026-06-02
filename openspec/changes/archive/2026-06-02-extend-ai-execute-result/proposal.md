## Why

Currently, `Neodymium.ai().execute` returns `void`, which provides no return value or execution details to the test developer. For complex tests, debugging, and framework assertions, test developers need to inspect what happened during the AI's execution: which LLM calls were made, what prompt and context was sent, what response was received, which actions were executed, and how much token/data overhead was incurred.

## What Changes

- Modify `AiBrowser` execution methods: `execute(String)` returns `AiExecutionResult`, and the no-arg `execute()` returns a composite `AiTestRunResult` object instead of `void`. This change is backward-compatible in Java, as existing void-discarding callers are unaffected.
- Introduce `AiExecutionResult`, a queryable result object containing complete details about:
  - Total standard input/output and PESAP tokens consumed during the execution.
  - The sequence of LLM interaction details (prompts, raw LLM responses, extracted JSON/actions, image screenshot metrics, full base64 screenshot data, full HTML DOM context strings, and DOM sizes/types).
  - The final set of actions successfully parsed and executed.
  - Duration/timing and retry/escalation indicators.
  - **Dissected Steps:** A complete sequence of individual steps (`StepDetails`), containing their raw and expanded step forms, the actions executed during that step, any specific LLM calls made by that step, the step's execution duration, failure details, and **PESAP static analysis details** (such as predicted context level and semantic linter warnings for that step).
  - **Test Data Snapshot:** A copy of the active test data key-value pairs at the time of execution.
  - **Variable Lookups:** A detailed sequence of variable lookups (`LookupDetails`) that occurred during instruction template resolution, capturing the key, the raw and resolved values, whether it was localized, and the source where it was found (e.g., TestData Map, JSONPath, Neodymium Configuration, Localization File).
  - **Escalation Sequences:** A detailed list of all context escalations (`EscalationDetails`) that occurred, recording the original context level, the target context level, the trigger type (LLM-requested vs. error-triggered), and the underlying reason (LLM reasoning or execution error message).
- Introduce **AiTestRunResult**, a composite wrapper capturing the results of before-steps, main steps, and after-steps for data-driven tests.
- Support **Zero-Knowledge Action Interception** via `MockActionExecutor` subclassing, capturing all performed actions and group transitions offline without a browser session.
- Support **Zero-Knowledge Offline LLM Simulation** via `MockLlmClient` subclassing and fluent sequential `MockResponse` queues, bypasses network calls, simulates latency delays, models custom token counts, and simulates connection or HTTP communication failures.
- Support **Zero-Knowledge Offline Page Context Stand-ins** via `MockPageAnalyzer` subclassing, returning static mock DOM texts and screenshots offline without browser setup.
- Ensure **Communication/HTTP Errors** (e.g. response codes or network failures) are fully captured and preserved as part of the LLM call failure details.
- Introduce `Neodymium.getLastAiExecutionResult()` and `Neodymium.getLastAiTestRunResult()` to allow developers to query and assert on the execution result even if the test failed and threw an exception.
- Provide comprehensive helper and inspection methods on `AiExecutionResult` (e.g., querying for specific actions or LLM calls) to facilitate testing and validation.

## Capabilities

### New Capabilities
- `ai-execution-result`: Introduce a rich, queryable `AiExecutionResult` object returned from AI execution methods, enabling programmatic introspection of LLM interactions, token usage, transferred DOM/image data, and executed actions.

### Modified Capabilities
<!-- None -->

## Impact

- **APIs**: Modifies signature of `AiBrowser.execute(String)` to return `AiExecutionResult`, and `AiBrowser.execute()` to return `AiTestRunResult`. Adds context getters on `Neodymium`.
- **Classes**:
  - `com.xceptance.neodymium.ai.core.AiBrowser` (Modified)
  - `com.xceptance.neodymium.ai.core.AiAgent` (Modified)
  - `com.xceptance.neodymium.util.Neodymium` (Modified)
  - `com.xceptance.neodymium.ai.core.AiExecutionResult` (New)
  - `com.xceptance.neodymium.ai.core.AiTestRunResult` (New)
  - `com.xceptance.neodymium.ai.core.LlmCallDetails` (New)
  - `com.xceptance.neodymium.ai.core.LookupDetails` (New)
  - `com.xceptance.neodymium.ai.core.EscalationDetails` (New)
  - `com.xceptance.neodymium.ai.core.StepDetails` (New)
  - `com.xceptance.neodymium.ai.testing.AiMockResponse` (New)
  - `com.xceptance.neodymium.ai.testing.MockLlmClient` (New)
  - `com.xceptance.neodymium.ai.testing.MockPageAnalyzer` (New)
  - `com.xceptance.neodymium.ai.testing.MockActionExecutor` (New)
- **Tests**: Modifies or adds unit/integration tests to assert that `execute` correctly returns full, detailed insights.
