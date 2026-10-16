# Tasks

## 1. Protocol Monitors and Buffers

- [ ] 1.1 Implement thread-safe `NetworkTrafficMonitor` with bounded circular ring buffer, status code filtering (4xx, 5xx, blocked), configurable URL ignore regex (`neodymium.ai.network.ignorePattern`), and CDP event listener integration, verified by `NetworkTrafficMonitorTest`.
- [ ] 1.2 Implement `ConsoleLogMonitor` capturing unhandled JavaScript errors and console warnings via CDP (`Runtime.exceptionThrown`, `Runtime.consoleAPICalled`) with W3C `driver.manage().logs().get(LogType.BROWSER)` fallback, verified by `ConsoleLogMonitorTest`.

## 2. Browser Tools in BrowserToolProvider

- [ ] 2.1 Implement `createGetNetworkFailuresTool()` in `BrowserToolProvider` exposing `browser_get_network_failures` with `status_filter`, `limit`, and `clear_buffer` parameters, verified by `BrowserToolProviderTest`.
- [ ] 2.2 Implement `createGetConsoleErrorsTool()` in `BrowserToolProvider` exposing `browser_get_console_errors` with `min_level`, `limit`, and `clear_buffer` parameters, verified by `BrowserToolProviderTest`.

## 3. Passive Diagnostic Enrichment & Playbook Replay

- [ ] 3.1 Update `AgentToolLoopStep` to inspect `NetworkTrafficMonitor` and `ConsoleLogMonitor` when interaction actions fail or locator thrashing occurs, automatically appending a concise diagnostic alert to the turn prompt, verified by `AgentToolLoopStepTest`.
- [ ] 3.2 Verify that introspection tool calls are marked as non-mutating query operations and produce zero state mutation during replay in `PlaybookToolReplayer`, verified by `PlaybookToolReplayerTest`.

## 4. End-to-End Verification & Documentation

- [ ] 4.1 Create end-to-end sandbox live test verifying that the agent retrieves network 500 errors and console exceptions on failing AJAX interactions without getting trapped in locator retry loops, verified by `NetworkIntrospectionSandboxLiveTest`.
- [ ] 4.2 Document configuration properties (`neodymium.ai.network.ignorePattern`, `neodymium.ai.network.monitorEnabled`) and update tool documentation in `doc/DOCUMENTATION.md`.

## Workflow follow-up

- Archive the change after implementation and test suite verification via `/opsx-archive`.
