# Spec Delta

## ADDED Requirements

### Requirement: Protocol-level network traffic introspection
The system SHALL capture failed HTTP network requests (status >= 400, network aborts, and CORS errors) from active browser sessions and expose a `browser_get_network_failures` tool allowing agents to inspect recent failures with optional status filtering and buffer draining.

#### Scenario: Query HTTP 500 failures after asynchronous backend API error
- **WHEN** an asynchronous `fetch` or `XHR` request fails with HTTP 500 during page execution
- **AND** the agent calls `browser_get_network_failures`
- **THEN** the system returns a structured list containing the failed request URL, HTTP status code 500, method, and failure description.

#### Scenario: Query HTTP 404 failures on missing endpoints
- **WHEN** a background request targets a non-existent endpoint returning HTTP 404
- **AND** the agent calls `browser_get_network_failures` with `status_filter="4xx"`
- **THEN** the system returns the 404 failure entries and excludes any 5xx entries.

#### Scenario: Capture network-level aborts or CORS rejections
- **WHEN** a browser network request is aborted or blocked by CORS policy without receiving an HTTP response code
- **AND** the agent calls `browser_get_network_failures` with `status_filter="blocked"`
- **THEN** the system returns the blocked entry indicating network abort or CORS failure.

#### Scenario: Ignore benign network failures matching configured ignore regex
- **WHEN** a network request to an analytics or tracking domain fails with an HTTP error
- **AND** the URL matches the pattern in `neodymium.ai.network.ignorePattern`
- **THEN** the system excludes the entry from the captured network failure buffer.

#### Scenario: Filter network failures by status category
- **WHEN** both 4xx and 5xx errors exist in the network buffer
- **AND** the agent calls `browser_get_network_failures` with `status_filter="5xx"`
- **THEN** the system returns only 5xx server failures and omits 4xx client errors.

#### Scenario: Clear network failure buffer on demand
- **WHEN** the agent calls `browser_get_network_failures` with `clear_buffer=true`
- **THEN** the returned entries are drained from the buffer so subsequent calls only return new failures.

### Requirement: Client-side JavaScript console error introspection
The system SHALL capture unhandled JavaScript exceptions, runtime errors, and console warnings from the active browser session and expose a `browser_get_console_errors` tool allowing agents to inspect recent console entries filtered by severity.

#### Scenario: Retrieve unhandled JavaScript runtime exceptions
- **WHEN** an unhandled client-side `TypeError` or `ReferenceError` occurs during an interaction turn
- **AND** the agent calls `browser_get_console_errors` with minimum severity `SEVERE`
- **THEN** the system returns the error message, source script URL, line number, and stack trace snippet.

#### Scenario: Filter console logs by minimum severity level
- **WHEN** console warnings and informational messages exist alongside severe errors
- **AND** the agent calls `browser_get_console_errors` with minimum severity `SEVERE`
- **THEN** the system returns only `SEVERE` runtime exceptions and excludes `WARNING` or `INFO` messages.

#### Scenario: Capture explicit console.error invocations
- **WHEN** page JavaScript invokes `console.error(...)` with diagnostic context
- **AND** the agent calls `browser_get_console_errors`
- **THEN** the system captures and returns the logged error message and arguments.

#### Scenario: Clear console error buffer on demand
- **WHEN** the agent calls `browser_get_console_errors` with `clear_buffer=true`
- **THEN** the captured logs are drained so subsequent calls only return new console events.

### Requirement: Passive diagnostic enrichment on action failure
The system SHALL buffer recent protocol-level network and console failures during agent tool execution and automatically append a concise diagnostic advisory into the conversational tool loop when actions fail repeatedly, preventing wasteful locator thrashing loops.

#### Scenario: Inject diagnostic advisory when click fails due to 500 API response
- **WHEN** an action or element assertion fails after retry attempts
- **AND** an HTTP 5xx was captured during the interaction turn
- **THEN** the system appends a diagnostic notice containing the failing endpoint and status to the conversation prompt.

#### Scenario: Inject diagnostic advisory when action fails due to unhandled JS exception
- **WHEN** an interaction fails to produce expected DOM changes
- **AND** an unhandled JavaScript exception was captured during the turn
- **THEN** the system appends a diagnostic alert containing the exception type and message to the conversation prompt.

#### Scenario: Suppress passive alerts when no protocol failures occurred
- **WHEN** an action fails due purely to an invalid element locator with zero protocol-level network or console errors
- **THEN** the system does not append a network or console diagnostic advisory.

#### Scenario: Stop locator thrashing loop upon passive error injection
- **WHEN** the agent receives a passive diagnostic notice regarding an HTTP 500 failure
- **THEN** the agent ceases alternate locator exploration and terminates the step with an informative backend defect verdict.

### Requirement: Test fixture and sandbox challenge endpoints for protocol introspection
The system SHALL provide dedicated test sandbox challenge pages and Javalin REST endpoints within `neodymium-test-server` to deterministically trigger, verify, and replay network failure and console error scenarios across mock and live test suites.

#### Scenario: Embedded test server serves interactive protocol failure challenge page
- **WHEN** a test navigates to `/AuraGlanceTest/shop/sandbox/protocol-introspection.html`
- **THEN** the test server serves an interactive page equipped with controls to trigger HTTP 404, HTTP 500, network aborts, unhandled JS exceptions, and background tracking requests.

#### Scenario: Embedded test server simulates configurable HTTP error endpoints on demand
- **WHEN** a client issues requests to `/AuraGlanceTest/api/faulty-backend` or `/AuraGlanceTest/api/missing-endpoint`
- **THEN** the test server returns HTTP 500 and HTTP 404 responses respectively with structured JSON error bodies.

#### Scenario: Mock sandbox test exercises diagnostic discovery tools offline
- **WHEN** `ProtocolIntrospectionSandboxMockTest` executes against the protocol introspection sandbox page
- **THEN** the mock agent discovers the failure details via `browser_get_network_failures` and `browser_get_console_errors` without connecting to external LLMs.

#### Scenario: Recorded playbooks containing introspection tools replay cleanly without network side effects
- **WHEN** a recorded playbook containing introspection tool calls is replayed in `REPLAY_STRICT` mode
- **THEN** the replayer treats introspection tools as non-mutating inspections and verifies all assertions successfully.
