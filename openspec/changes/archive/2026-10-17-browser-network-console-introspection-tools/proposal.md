# Proposal

## Why

When web applications fail asynchronously via background API errors (e.g., HTTP 4xx/5xx on `fetch`/`XHR`) or uncaught client-side JavaScript exceptions, the browser UI fails to update or display expected elements. Because Neodymium currently lacks protocol-level network and console introspection tools, the AI agent cannot distinguish between bad locators and application crashes, resulting in severe locator thrashing (10–15 turns and 40,000–80,000 wasted tokens) and misleading defect attribution (`ElementNotFoundException` instead of reporting the SUT backend/frontend bug).

## What Changes

- Add `browser_get_network_failures` tool to `BrowserToolProvider` allowing the agent to query failed HTTP requests (4xx, 5xx, aborted requests, or CORS errors) captured since the last check.
- Add `browser_get_console_errors` tool to `BrowserToolProvider` allowing the agent to inspect unhandled JavaScript exceptions, runtime errors, and console warnings.
- Implement thread-safe protocol monitors (`NetworkTrafficMonitor`, `ConsoleLogMonitor`) in `neodymium-core` backed by Chrome DevTools Protocol (CDP) events (`Network.responseReceived`, `Network.loadingFailed`, `Runtime.exceptionThrown`) with graceful fallback to W3C `driver.manage().logs().get(LogType.BROWSER)` and the Performance Resource Timing API.
- Add passive diagnostic enrichment in `AgentToolLoopStep`: when repeated action failures or locator thrashing occur, automatically query the monitoring buffer and append a compact diagnostic advisory (e.g., `"[Diagnostic Alert]: HTTP 500 on /api/cart/add"`) to break thrashing loops.
- Support URL ignore patterns via `neodymium.properties` (`neodymium.ai.network.ignorePattern`) to filter out benign telemetry, tracking pixels, or non-fatal CDN assets.
- Ensure diagnostic inquiry tools are treated as non-mutating inspections with zero replay impact in companion JSON playbooks.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `unified-tooling-architecture`: Adds protocol-level network and console introspection tools (`browser_get_network_failures`, `browser_get_console_errors`), non-fatal background monitoring buffers, and passive diagnostic enrichment on action failure.

## Impact

- **Code:**
  - `org.neodymium.ai.tool.browser.BrowserToolProvider`: register `browser_get_network_failures` and `browser_get_console_errors`.
  - `org.neodymium.ai.monitor.NetworkTrafficMonitor`: CDP/Proxy network listener and circular failure buffer.
  - `org.neodymium.ai.monitor.ConsoleLogMonitor`: CDP/W3C console error collector.
  - `org.neodymium.ai.pipeline.AgentToolLoopStep`: passive error injection on repeated action failure.
  - `org.neodymium.ai.replay.PlaybookToolReplayer`: verify non-mutating status during JSON playbook replay.
- **Dependencies:** None. Uses Selenium W3C log capabilities and Chromium DevTools Protocol (CDP) already bundled in Selenium 4.
- **Configuration:** New optional properties `neodymium.ai.network.ignorePattern` and `neodymium.ai.network.monitorEnabled`.
