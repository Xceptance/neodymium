## Context

Currently, `AiBrowser` execution methods are void and do not provide programmatic feedback about the execution details. To facilitate debugging and comprehensive framework/integration testing, a detailed result object is needed. This design addresses how to capture detailed LLM calls, DOM sizes, screenshots, actions, and token counts, and return them via `AiBrowser.execute` in a backward-compatible manner.

## Goals / Non-Goals

**Goals:**
- Return `AiExecutionResult` from `AiBrowser.execute(String)` and `AiBrowser.execute()` methods.
- Capture individual `LlmCallDetails` (including prompt content, vision usage, context level, DOM size, raw LLM response, parsed JSON, token metrics, and call mode).
- Capture execution metrics (retry count, escalation count, replay count, direct parse count, duration) specifically scoped to a single `execute` invocation.
- Ensure the changes are completely backward-compatible (void-callers continue working seamlessly).
- Capture dissected instruction steps in both their original raw format and resolved expanded format.
- Capture a copy snapshot of the active test data variables at the time of execution.

**Non-Goals:**
- Storing or tracking browser history or page transition HTML (unless sent to LLM as part of the page context).
- Changing Selenide/WebDriver execution flow or changing the langchain4j integration framework.

## Decisions

### 1. Zero-Knowledge Framework via Constructor Dependency Injection
- **Decision:** The core AI components (`AiAgent`, `LlmClient`, `PageAnalyzer`, `ActionExecutor`) will be 100% unaware of any testing or mocking mode. We achieve this by providing constructors and factories on `AiBrowser` and `AiAgent` that support standard constructor dependency injection.
- **Rationale:** By passing `LlmClient`, `PageAnalyzer`, and `ActionExecutor` to `AiBrowser` and `AiAgent` constructors, the production runtime can instantiate standard implementations, while tests can pass custom/mock subclasses. This keeps the core classes 100% clean of `if (testMode)` or `if (mocking)` branching.
- **Alternative considered:** Thread-local execution result aggregator maps or global mocking flags. This alternative was rejected because it pollutes production code with testing/mocking awareness and makes nested calls or cleanup highly complex and fragile.

### 2. Standard API Evolution (void to returned object)
- **Decision:** Change `execute(...)` signatures to return `AiExecutionResult` (for `execute(String)`) and `AiTestRunResult` (for data-driven no-arg `execute()`) instead of `void`.
- **Rationale:** In Java, changing a method return type from `void` to any non-void type is source and binary compatible for existing consumers who discard the returned object. Returning a composite `AiTestRunResult` for the no-arg method ensures that `before`, `steps`, and `after` stages are all queryable.
- **Alternative considered:** Adding new methods like `executeAndGetResult(...)`. While clean, it creates duplicate APIs. Retaining the single `execute` method is much simpler.

### 3. Execution Metrics & Token Snapshotted Delta
- **Decision:** Capture metric counts and compute invocation token usage directly in `AiAgent` and `AiBrowser` via local reference updates, rather than subscribing `AiStats` to thread-locals. Token count delta is calculated by snapshotting the `AiStats` values at the start and end of the execution block.
- **Rationale:** This avoids passing `AiStats` state updates via thread-local boundaries. It simplifies the aggregation and guarantees that double-counting of tokens across subsequent steps or nested runs is mathematically impossible.

### 4. Dissected Steps and Test Data Snapshotting
- **Decision:** Capture a copy of active test data keys/values (`Map<String, Object>`) and record both raw and expanded instructions for each step into `AiExecutionResult`.
- **Rationale:** Tests need to assert precisely how variables were resolved (e.g. asserting that `${userEmail}` was correctly expanded to `user@test.com`). Capturing a snapshot Map copy at the start of `execute()` prevents later mutation from contaminating the logged test data. Storing raw and resolved steps together enables easy comparison in assertion logic.

### 5. Failure Preservation and Complete Data Visibility
- **Decision:** Capture and store the full DOM context HTML string and full base64 screenshot data in `LlmCallDetails` rather than just metadata or sizes. Additionally, cache the last returned result object in an internal instance field on the thread's `AiBrowser` session.
- **Rationale:** Full visibility is crucial for assertions in framework tests (e.g., verifying whether elements or visual layouts were present in the exact data transferred to the LLM). When an execution fails and throws an exception, the return value is unreachable. By caching the active result in the context-scoped `AiBrowser` instance, it remains programmatically queryable via a thread-safe static getter `Neodymium.getLastAiExecutionResult()` and `Neodymium.getLastAiTestRunResult()`.

### 6. Localized Escalation and Functional Resolution Passing
- **Decision:** Capture a structured log of `EscalationDetails` (original context level, escalated context level, trigger type, and reason/exception details) and `LookupDetails` (key, value, localized status, and resolution source) on `AiExecutionResult`.
- **Rationale:** To record resolving placeholders without thread-locals, we update the signature of `resolveTestDataToPrompt(...)` to accept an optional `List<LookupDetails> lookupsCollector` argument. When doing variable lookup, we check if `Neodymium.tryLocalizedText` returns a translated value and attribute it as `localized = true` with raw and finalized strings logged.

### 7. PESAP Static Analysis Capturing
- **Decision:** Store predicted context levels and linter warnings returned by PESAP during `runPesap` directly inside the corresponding step's `StepDetails` instance within `AiExecutionResult`.
- **Rationale:** Pre-populating the `StepDetails` list at execution start allows both the pre-execution PESAP phase and the runtime execution loop to enrich the same step objects sequentially. Keeping static analysis predictions and warnings under the same step identifier simplifies assertions (e.g. verifying that a linter warning was successfully raised on a specific step).

### 8. Programmatic Offline Mocking via OOP Subclassing
- **Decision:** Introduce mock/stand-in subclasses (`MockLlmClient`, `MockPageAnalyzer`, `MockActionExecutor`) under `com.xceptance.neodymium.ai.testing` that extend the core classes and override their methods to run completely offline without browser dependencies.
- **Rationale:** By providing `MockLlmClient`, `MockPageAnalyzer`, and `MockActionExecutor`, we can mock LLM calls sequentially using a fluent `AiMockResponse` queue, serve canned DOM context/screenshots without opening a physical browser, and intercept executed Selenide/WebDriver actions for programmatic assertion checks. The core AI code has zero awareness of these subclasses—it simply interacts with the base class APIs. A mock test run can be initialized cleanly by registering a configured `AiBrowser` constructed with these mocks onto `Neodymium.setAiBrowser(...)`. When the test completes, closing `AiBrowser` releases all references, completely avoiding thread-local pollution or residual state leaks.

### 9. Offline Configuration via System Properties
- **Decision:** Provide a convenience method `MockLlmClient.configureForOffline()` (or through direct system property configuration) that programmatically sets `neodymium.ai.apiKey = mock-offline-key` and `neodymium.ai.pesap.enabled = false` before mock execution.
- **Rationale:** Production `AiAgent.execute()` reloads the configuration from system properties and validates that the API key is not blank, failing immediately if it is. Furthermore, PESAP is enabled by default, which would execute additional LLM requests and exhaust the `MockLlmClient`'s response queue. Setting these system properties at the start of mock tests ensures the production configuration guards are satisfied and PESAP's extra LLM calls are disabled, without modifying the production logic to be test-aware.

## Risks / Trade-offs

- **[Risk]** Memory leaks if references are held after a test completes.
  - **Mitigation:** Since `AiBrowser` is managed as part of the thread context which is completely re-created or wiped by the JUnit runners between tests, all mock references and cached results are garbage collected automatically.
- **[Risk]** High memory usage from storing full DOM contexts and screenshot strings inside `LlmCallDetails`.
  - **Mitigation:** While DOM strings and base64 screenshots can be large, they are scoped to a single `execute` invocation and a single thread, causing negligible footprint for standard JUnit execution runs.
- **[Risk]** Multithreading safety if multiple threads access `AiExecutionResult`.
  - **Mitigation:** Use thread-safe `Collections.synchronizedList` for internal lists in `AiExecutionResult`.
