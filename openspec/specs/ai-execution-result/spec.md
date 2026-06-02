# AI Execution Result

Purpose: Return execution insights via a new `AiExecutionResult` (and composite `AiTestRunResult` for data-driven runs) from `AiBrowser.execute` methods instead of `void`.

## Requirements

### Requirement: Return Execution Result from AI Browser
The `AiBrowser.execute(String)` method SHALL return an instance of `AiExecutionResult` containing detailed insights into the AI's execution steps, LLM calls, actions, and token/data usage. The no-arg `AiBrowser.execute()` method SHALL return a composite `AiTestRunResult` object containing `beforeResult` (`AiExecutionResult`), `stepsResult` (`AiExecutionResult`), and `afterResults` (`List<AiExecutionResult>`).

#### Scenario: Execute returns non-null result object
- **WHEN** the AI agent executes a single instruction set successfully
- **THEN** the execute method returns a non-null `AiExecutionResult` containing the execution details

#### Scenario: Data-driven execute returns non-null test run result
- **WHEN** the data-driven AI execution method is called
- **THEN** it returns a composite `AiTestRunResult` combining execution details of all stages (before, steps, after)

### Requirement: Inspect Cumulative Token Usage in Result
The `AiExecutionResult` SHALL expose the cumulative token statistics specific to that execution invocation, distinguishing between Standard AGENT calls and PESAP calls.

#### Scenario: Verify token stats on execute result
- **WHEN** the execution result is inspected for token usage
- **THEN** the result returns the correct input, output, cached input, and total tokens for both Standard and PESAP modes

### Requirement: Inspect Individual LLM Calls in Result
The `AiExecutionResult` SHALL expose a list of `LlmCallDetails` representing each individual LLM request and response during the execution. Each call detail SHALL capture the system prompt, user prompt, base64 screenshot presence, full base64 screenshot data, full HTML DOM context strings, DOM context size/level, raw text response, parsed JSON actions, and individual token counts.

#### Scenario: Inspect LLM call sequence and Vision details
- **WHEN** a test executes a step using a visual context or screenshot
- **THEN** the returned `AiExecutionResult` contains `LlmCallDetails` reflecting the exact base64 screenshot data, the full HTML DOM context string, the prompts, and the LLM's response

### Requirement: Query Executed Actions and Metrics
The `AiExecutionResult` SHALL provide query methods to retrieve all executed `Action` instances, verify if a specific action type or direct parse was used, and check execution metrics such as retry count, escalation count, replay count, and duration.

#### Scenario: Query for performed actions
- **WHEN** instructions are executed and the result object is queried for actions
- **THEN** it returns the exact list of Selenium/WebDriver actions performed during the steps

### Requirement: Inspect Dissected Steps and Test Data Snapshot
The `AiExecutionResult` SHALL capture and expose a copy of the active test data variables snapshot and the complete sequence of dissected instruction steps (`StepDetails`) in both their raw (unresolved) format and expanded (resolved) format, along with individual step performance durations and failure reasons.

#### Scenario: Verify dissected steps and test data variables
- **WHEN** instructions containing test data placeholders (e.g. `${userEmail}`) are executed and the result object is inspected
- **THEN** it returns a snapshot of the active test data and the sequence of step details showing raw steps, expanded steps, step actions, step LLM calls, and step durations

### Requirement: Access Execution Result on Failure
If an AI instruction execution fails and throws an exception, the test developer MUST be able to retrieve the exact `AiExecutionResult` or `AiTestRunResult` of the failed execution.

#### Scenario: Retrieve failed execution result
- **WHEN** an AI instruction fails and throws an exception
- **THEN** calling `Neodymium.getLastAiExecutionResult()` or `Neodymium.getLastAiTestRunResult()` returns the non-null result containing all the logs, steps, and LLM call details captured up to the point of failure

### Requirement: Track Detailed Variable Lookups
The `AiExecutionResult` SHALL capture and expose a list of `LookupDetails` for all variables resolved during template processing. Each `LookupDetails` entry SHALL contain:
- `key`: the name of the matched variable placeholder (e.g., `"userEmail"`)
- `resolvedValue`: the actual value returned by the lookup mechanism (e.g., `"user@test.com"`)
- `localized`: boolean indicating whether the value was translated via localized translation assets
- `source`: the matched source string, which SHALL be one of:
  - `"TestData Map"` (matched in the active dataset map)
  - `"JSONPath Query"` (matched via JSONPath dot or bracket notation)
  - `"Neodymium Configuration"` (matched in owner-based configuration settings)
  - `"Localization File"` (matched in the neodymium translation locale assets)
  - `"Not Found"` (if lookup failed and returned null)

#### Scenario: Verify lookup source and values
- **WHEN** instructions containing placeholders like `${userEmail}` are resolved
- **THEN** `AiExecutionResult` returns lookup details showing `"userEmail"` matched from `"TestData Map"` with resolved value `"user@test.com"` and localization flags

### Requirement: Capture Context Escalation Details
The `AiExecutionResult` SHALL capture and expose a list of `EscalationDetails` for each context level escalation that occurs. Each `EscalationDetails` entry SHALL capture:
- `fromLevel`: the original `ContextLevel` (e.g. `AXTREE`)
- `toLevel`: the escalated `ContextLevel` (e.g. `VISUAL_LEAN` or `STANDARD`)
- `llmRequested`: boolean indicating whether it was an LLM-directed escalation (via `"status": "ESCALATE"` response JSON)
- `reason`: a string capturing either the LLM's detailed reasoning text or the underlying Selenide/WebDriver exception/assertion failure message

#### Scenario: Verify escalation reasoning
- **WHEN** a context escalation is triggered due to an execution error
- **THEN** the returned execution result captures an `EscalationDetails` entry where `llmRequested` is false, and `reason` contains the precise exception message of the action execution error

### Requirement: Inspect PESAP Static Analysis Details
The `AiExecutionResult` SHALL capture and expose detailed pre-execution static analysis (PESAP) outputs inside each step's `StepDetails` when PESAP is enabled. Specifically, each step's `StepDetails` SHALL contain:
- `pesapPredictedContextLevel`: the predicted `ContextLevel` (e.g. `AXTREE`, `VISUAL_LEAN`, etc.) resolved during the classification phase.
- `pesapWarnings`: the list of semantic linter warnings returned by PESAP for that specific step.

#### Scenario: Verify PESAP classification and linter results
- **WHEN** an instruction sequence is processed by the agent under active PESAP classification and semantic linter rules
- **THEN** the returned execution result's `StepDetails` sequence contains predicted context levels matching the classified outcomes and linter warnings reflecting any syntax or semantic violations flagged during the scan

### Requirement: Instrument and Mock Action Execution
The system SHALL support programmatically providing a subclass-based `MockActionExecutor` to intercept, wrap, log, or fully mock action executions. The core `ActionExecutor` and production plugins MUST be completely unaware of this mock.

#### Scenario: Mock action execution diagnostic
- **WHEN** a `MockActionExecutor` is constructed and injected into `AiBrowser`
- **THEN** actions are intercepted, the executed action lists are captured, and Selenium execution is bypassed successfully

### Requirement: Simulate LLM Offline programmatically
The system SHALL support programmatically injecting a `MockLlmClient` subclass to simulate LLM responses completely offline, bypassing LangChain4j network calls. The base `LlmClient` and the AI agent MUST be completely unaware of this mock.
To easily mock complex, repeatable execution flows, the developer can declare a sequence of `AiMockResponse` behaviors using a fluent Java builder API. Each `AiMockResponse` behavior SHALL support specifying:
- Simulated JSON success response or raw text.
- Timing latency delay in milliseconds.
- Simulated HTTP status codes or exceptions to mock communication failures.
- Optional customized token footprint metadata.

#### Scenario: Simulate complex response sequences offline
- **WHEN** a `MockLlmClient` is registered with a sequence of `AiMockResponse` behaviors (timing, HTTP status codes, actions)
- **THEN** for each sequential LLM request, the system pops the next `AiMockResponse`, sleeps for any specified delay, throws any specified connection/HTTP error, or returns the specified action JSON and populates the synthetic token statistics offline. It throws `IllegalStateException` on queue exhaustion.

### Requirement: Offline Configuration Prerequisites
To run mock execution successfully offline, the system configuration SHALL allow setting required prerequisites via system properties.
- The `neodymium.ai.apiKey` property SHALL be set to a non-blank mock string to satisfy production validation checks.
- The `neodymium.ai.pesap.enabled` property SHOULD be set to `false` to prevent unwanted LLM network requests or queue exhaustion in mock environments where PESAP is not mocked.

#### Scenario: Satisfy API key validation and disable PESAP
- **WHEN** a mock test starts and `MockLlmClient.configureForOffline()` is called
- **THEN** the system properties are configured, the API key validation passes successfully offline, and PESAP does not make extra LLM calls

### Requirement: Support Browserless Page Context Stand-ins
The system SHALL support injecting a subclass-based `MockPageAnalyzer` to supply canned DOM content strings and capture requested `ContextLevel`s and screenshots offline without a browser session. The core `PageAnalyzer` and AI agent MUST be completely unaware of this mock.

#### Scenario: Browserless execution testing
- **WHEN** a `MockPageAnalyzer` is constructed with mock DOM text and screenshots and injected into `AiBrowser`
- **THEN** the AI agent successfully extracts canned DOM context and mock screenshots without launching a browser, and logs every requested context level and screenshot title for verification

### Requirement: Capture Network and Communication Errors in Result
When an LLM call fails due to a communication, network, or HTTP server issue, the `AiExecutionResult` and the thrown exception SHALL capture and fully preserve the error details, including any HTTP response code and API response messages if available.

#### Scenario: Capture HTTP status code on network failure
- **WHEN** an LLM chat call fails with an HTTP 403 or network timeout
- **THEN** the execution result captures an `LlmCallDetails` entry with the error exception details, and the thrown exception preserves the response code and communication failure message
