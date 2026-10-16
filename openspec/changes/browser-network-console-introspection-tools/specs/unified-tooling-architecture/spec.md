# Spec Delta

## ADDED Requirements

### Requirement: Protocol-level network traffic introspection
The system SHALL capture failed HTTP network requests (status >= 400, network aborts, and CORS errors) from active browser sessions and expose a `browser_get_network_failures` tool allowing agents to inspect recent failures with optional status filtering and buffer draining.

#### Scenario: Query HTTP failures after background API failure
- **WHEN** a background `fetch` or `XHR` request fails with HTTP 500 during page execution
- **AND** the agent calls `browser_get_network_failures`
- **THEN** the system returns a structured list containing the failed request URL, HTTP status code, method, and failure type.

#### Scenario: Ignore benign network failures matching configured patterns
- **WHEN** a network request to an ignored analytics or tracking domain fails with an HTTP error
- **AND** the URL matches `neodymium.ai.network.ignorePattern`
- **THEN** the system excludes the entry from the captured network failure buffer.

#### Scenario: Clear network failure buffer on demand
- **WHEN** the agent calls `browser_get_network_failures` with `clear_buffer=true`
- **THEN** the returned entries are drained from the buffer so subsequent calls only return new failures.

### Requirement: Client-side JavaScript console error introspection
The system SHALL capture unhandled JavaScript exceptions, runtime errors, and console warnings from the active browser session and expose a `browser_get_console_errors` tool allowing agents to inspect recent console entries filtered by severity.

#### Scenario: Retrieve unhandled JavaScript exception
- **WHEN** an unhandled client-side `TypeError` occurs during an interaction turn
- **AND** the agent calls `browser_get_console_errors` with minimum severity `SEVERE`
- **THEN** the system returns the error message, source script URL, line number, and stack trace snippet.

#### Scenario: Filter console logs by minimum severity level
- **WHEN** console warnings and informational messages exist in the log buffer
- **AND** the agent calls `browser_get_console_errors` with minimum severity `SEVERE`
- **THEN** the system returns only `SEVERE` runtime exceptions and excludes benign `WARNING` or `INFO` messages.

### Requirement: Passive diagnostic enrichment on action failure
The system SHALL buffer recent protocol-level network and console failures during agent tool execution and automatically append a concise diagnostic advisory into the conversational tool loop when actions fail repeatedly, preventing wasteful locator thrashing loops.

#### Scenario: Inject diagnostic advisory when click fails due to 500 API response
- **WHEN** an action or element assertion fails after retry attempts
- **AND** an HTTP 5xx or unhandled JS exception was captured during the interaction turn
- **THEN** the system appends a diagnostic notice containing the failing endpoint or exception details to the conversation prompt.

#### Scenario: Suppress passive alerts when no protocol failures occurred
- **WHEN** an action fails due purely to an invalid element locator with zero protocol-level network or console errors
- **THEN** the system does not append a network or console diagnostic advisory.
