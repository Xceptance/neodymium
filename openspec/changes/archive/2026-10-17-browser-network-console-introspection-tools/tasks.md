# Tasks

## 1. Test Server Fixtures & Endpoints (neodymium-test-server)

- [x] 1.1 Register simulation REST endpoints in `AuraGlanceApp` / `JavalinTestServer` (`/AuraGlanceTest/api/faulty-backend` for HTTP 500, `/AuraGlanceTest/api/missing-endpoint` for HTTP 404, and `/AuraGlanceTest/api/analytics` for ignore filter testing), verified by `JavalinTestServerTest`.
- [x] 1.2 Create interactive challenge page `AuraGlanceTest/shop/sandbox/protocol-introspection.html` equipped with triggers for asynchronous 404, 500, unhandled JS exceptions, console errors, and analytics requests, verified by test server endpoint test.

## 2. Protocol Monitors & Unit Tests (neodymium-core)

- [x] 2.1 Write unit tests in `NetworkTrafficMonitorTest` covering circular buffer capacity, multi-threaded concurrency safety, status filtering (`all_failures`, `4xx`, `5xx`, `blocked`), URL regex exclusion (`neodymium.ai.network.ignorePattern`), and buffer clearing.
- [x] 2.2 Implement `NetworkTrafficMonitor` with CDP event listener integration (`Network.responseReceived`, `Network.loadingFailed`) and in-memory bounded ring buffer, passing all tests in `NetworkTrafficMonitorTest`.
- [x] 2.3 Write unit tests in `ConsoleLogMonitorTest` covering severity thresholds (`SEVERE`, `WARNING`, `INFO`), unhandled runtime exception stack formatting, buffer eviction, and buffer draining.
- [x] 2.4 Implement `ConsoleLogMonitor` with CDP (`Runtime.exceptionThrown`, `Runtime.consoleAPICalled`) and W3C `driver.manage().logs().get(LogType.BROWSER)` fallback, passing all tests in `ConsoleLogMonitorTest`.

## 3. Browser Tool Provider Integration (neodymium-core)

- [x] 3.1 Write tool provider tests in `BrowserToolProviderIntrospectionTest` verifying tool schema definitions, parameter parsing, status filtering, and JSON output structures for `browser_get_network_failures` and `browser_get_console_errors`.
- [x] 3.2 Implement `createGetNetworkFailuresTool()` and `createGetConsoleErrorsTool()` in `BrowserToolProvider` and register them in `BrowserToolProvider.registerStandardTools`, passing all tests in `BrowserToolProviderIntrospectionTest`.

## 4. Passive Diagnostic Enrichment & Playbook Replay (neodymium-core)

- [x] 4.1 Write unit tests in `AgentToolLoopStepPassiveAlertTest` verifying that passive diagnostic alerts are injected into the turn conversation when actions fail with buffered errors and suppressed when the buffer is clean.
- [x] 4.2 Update `AgentToolLoopStep` to inspect `NetworkTrafficMonitor` and `ConsoleLogMonitor` upon repeated action failures or locator thrashing, appending diagnostic notices to the prompt.
- [x] 4.3 Write tests in `PlaybookToolReplayerTest` verifying that introspection tool calls in recorded playbooks execute as non-mutating query operations without failing during offline replay in `REPLAY_STRICT`.

## 5. End-to-End Sandbox Integration Tests (neodymium-e2e-tests)

- [x] 5.1 Implement `ProtocolIntrospectionSandboxMockTest` in `org.neodymium.ai.integration.sandbox.mock` extending `BaseAiTest` verifying offline discovery of 500 errors, JS exceptions, ignore filtering, thrashing prevention, and replay fidelity against `protocol-introspection.html`.
- [x] 5.2 Implement `ProtocolIntrospectionSandboxLiveTest` in `org.neodymium.ai.integration.sandbox.live` extending `BaseAiTest` verifying that live autonomous agents accurately identify backend/frontend errors without entering 10-15 turn locator thrashing loops.

## 6. Documentation & Configuration

- [x] 6.1 Document configuration properties (`neodymium.ai.network.ignorePattern`, `neodymium.ai.network.monitorEnabled`) and update tool catalog in `doc/DOCUMENTATION.md`.

## Workflow follow-up

- Archive the change after implementation and test suite verification via `/opsx-archive`.
