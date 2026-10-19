# Design

## Context

See [proposal.md](proposal.md) for motivation and defect history (specifically [IDEA-20261013-1308](../../doc/ideas/2026-10/IDEA-20261013-1308-browser-network-console-introspection-tools.md)).

Currently, `BrowserToolProvider` exposes 38 interactive and discovery tools (click, fill, type, inspect, query_dom, etc.), but zero protocol-level introspection tools. When a backend API returns HTTP 500 or client-side JavaScript crashes with an uncaught `TypeError`, the SUT UI fails to update. The AI agent, lacking network/console visibility, assumes its locator was inaccurate and thrashes through 10–15 alternate selector combinations across multiple turns, burning tens of thousands of tokens before terminating with an unhelpful `ElementNotFoundException`.

## Goals / Non-Goals

**Goals:**
- Provide explicit agent tools (`browser_get_network_failures`, `browser_get_console_errors`) in `BrowserToolProvider`.
- Maintain lightweight, non-blocking in-memory circular buffers for captured network failures and console exceptions.
- Leverage Selenium 4 Chrome DevTools Protocol (CDP) for Chromium browsers with seamless fallbacks to W3C `LogType.BROWSER` and JavaScript Performance APIs on non-CDP drivers.
- Implement passive diagnostic enrichment in `AgentToolLoopStep` to inject concise failure notices when actions fail repeatedly, cutting locator thrashing immediately.
- Filter out non-fatal background noise (tracking pixels, analytics, fonts) via regex configurable in `neodymium.properties`.
- Guarantee zero replay overhead or mutation in recorded JSON playbooks.
- Deliver an automated, multi-tiered test suite (Unit, Mock Sandbox E2E, Live Sandbox E2E) backed by a self-contained challenge fixture in `neodymium-test-server`.

**Non-Goals:**
- Full network body capture / payload recording (only metadata, URLs, headers, and status codes are stored to conserve memory).
- Mandatory proxy server deployment (e.g. BrowserUpProxy); CDP and native browser APIs must work out of the box without network port bindings.
- Replacing existing assertions or test failure handling.

## Decisions

### Decision 1: Hybrid Protocol Monitoring (CDP with W3C / Performance Fallback)
- **Choice**: Use Selenium 4 CDP (`DevTools` / `HasDevTools`) as the primary capture mechanism on Chromium-based drivers, with automatic fallback to W3C `driver.manage().logs().get(LogType.BROWSER)` and the `window.performance` API.
- **Rationale**:
  - CDP captures exact HTTP response status codes, aborts, CORS rejections, and console errors asynchronously at the protocol level without modifying page JavaScript.
  - Selenium 4 bundles CDP support natively without requiring external proxies or dependencies.
  - Fallbacks ensure non-Chromium or remote grid environments without CDP do not throw exceptions.
- **Alternatives Considered**:
  - *Mandatory Local Proxy (e.g. BrowserUpProxy)*: Rejected; requires extra open ports, certificate injection, slows startup, and often clashes with corporate VPNs/proxies.
  - *Monkey-patching window.fetch / XMLHttpRequest in JavaScript*: Rejected; cannot capture Web Worker requests, fails on early pre-script requests, and fails on hard CORS network aborts.

### Decision 2: Thread-Safe Ring Buffering
- **Choice**: Store captured network failures and console errors in bounded circular FIFO buffers (default capacity: 100 entries per driver session).
- **Rationale**:
  - Eliminates memory leak risks during long-running tests or large test suites.
  - Provides thread isolation aligned with Neodymium's thread-isolated `AiSession` and WebDriver instances.
  - Supports atomic draining (`clear_buffer: true`) so agents only see new events since their last check.

### Decision 3: Explicit Tool Signatures in `BrowserToolProvider`
- **Choice**:
  - `browser_get_network_failures`: filters by `status_filter` (`"all_failures"`, `"4xx"`, `"5xx"`, `"blocked"`), `limit` (default 20), `clear_buffer` (default true).
  - `browser_get_console_errors`: filters by `min_level` (`"SEVERE"`, `"WARNING"`, `"INFO"`), `limit` (default 20), `clear_buffer` (default true).
- **Rationale**: Conforms to standard OpenAPI/JSON Schema contracts used across Neodymium tools, allowing the LLM to call them naturally during troubleshooting turns.

### Decision 4: Passive Diagnostic Enrichment in `AgentToolLoopStep`
- **Choice**: If a tool action fails after retry attempts or when locator thrashing is flagged by `ThrashingGuard`, `AgentToolLoopStep` checks the buffer. If recent HTTP 5xx or `SEVERE` JS errors exist within the last turn window, it injects a one-line alert into the assistant conversation:
  `"[Diagnostic Alert]: Detected 1 HTTP 500 on endpoint '/api/cart/add'. Avoid retrying selectors if the SUT state did not update due to application failure."`
- **Rationale**: Solves the failure mode where the LLM does not spontaneously think to call the diagnostic tools, stopping wasteful exploration without requiring prompt tuning for every turn.

### Decision 5: Non-Mutating Playbook Lifecycle
- **Choice**: Tool calls to `browser_get_network_failures` and `browser_get_console_errors` are tagged as non-mutating query operations.
- **Rationale**: Avoids polluting recorded companion JSON playbooks with transient troubleshooting steps that have no bearing on happy-path test execution during replay.

### Decision 6: Testing Strategy & Test Sandbox Fixture Architecture
- **Choice**: Build a dedicated challenge page (`AuraGlanceTest/shop/sandbox/protocol-introspection.html`) and endpoints in `neodymium-test-server` paired with full unit, offline mock, and live test suites.
- **Test Matrix**:
  1. **Sandbox Fixture (`neodymium-test-server`)**:
     - Endpoints in `JavalinTestServer`:
       - `/AuraGlanceTest/api/faulty-backend`: returns HTTP 500 `{"error": "Internal server fault"}`.
       - `/AuraGlanceTest/api/missing-endpoint`: returns HTTP 404 `{"error": "Resource missing"}`.
       - `/AuraGlanceTest/api/analytics`: returns HTTP 404 `{"error": "Ignored telemetry"}`.
     - Interactive challenge page `protocol-introspection.html` with explicit triggers for 404, 500, unhandled JS exceptions, console errors, and ignored analytics.
  2. **Core Unit Tests (`neodymium-core`)**:
     - `NetworkTrafficMonitorTest`: circular buffer bounds, thread safety, status filtering, ignore pattern regex evaluation, and draining.
     - `ConsoleLogMonitorTest`: severity thresholds, exception stack formatting, buffer eviction, and draining.
     - `BrowserToolProviderTest`: parameter parsing, schema validity, and tool output formatting.
     - `AgentToolLoopStepTest`: passive alert generation on action failure, prompt formatting, and thrashing circuit breakers.
     - `PlaybookToolReplayerTest`: verification that introspection calls execute as transparent no-ops during replay.
  3. **Mock Sandbox Integration Tests (`neodymium-e2e-tests`)**:
     - `ProtocolIntrospectionSandboxMockTest`: offline validation using `MockLlmProvider` and embedded Javalin server testing 500 detection, JS exception capture, ignore pattern filtering, passive alert stopping thrashing, and strict replay.
  4. **Live Sandbox Integration Tests (`neodymium-e2e-tests`)**:
     - `ProtocolIntrospectionSandboxLiveTest`: real LLM agent execution verifying that live agents recognize protocol failures and report SUT bugs immediately without locator thrashing loops.

## Risks / Trade-offs

- **[Risk]** High volume of benign 404s (e.g., third-party tracking pixels, analytics, or missing favicon/font assets) flooding the failure buffer.
  - *Mitigation*: Support `neodymium.ai.network.ignorePattern` (regex) in `neodymium.properties` with sensible default patterns (e.g., `.*(google-analytics|segment|favicon\.ico).*`).
- **[Risk]** CDP session initialization overhead or concurrency issues.
  - *Mitigation*: Lazily attach CDP listeners upon first navigation or test initialization, catching any `DevToolsException` gracefully.
