# [IDEA-20261013-1308] Browser Network & Console Introspection Tools for Failure Diagnostics and SUT Defect Attribution

- **Status:** `Proposed`
- **Proposed:** 2026-10-13 13:08
- **Resolved:** `Pending`
- **Component:** `neodymium-core` (`BrowserToolProvider`, `AgentToolLoopStep`, `NetworkTrafficMonitor`, `ConsoleLogMonitor`)
- **Category:** `Protocol & Monitoring`
- **Author:** AI-generated: Gemini 3.8 Flash & Xceptance GmbH 2026

## 1. Problem Statement & Motivation

During autonomous test execution and playbook generation, modern web applications (SPAs, headless e-commerce, React/Vue/Angular storefronts) perform critical operations via asynchronous background requests (`fetch`/`XHR`). When an interaction (e.g. clicking "Add to Cart", submitting a form, or applying a discount code) fails due to an application crash:

1. **Silent Failure & Locator Thrashing:** The SUT fails to render the expected confirmation modal, error banner, or cart counter badge. Because the agent only perceives DOM and visual snapshots, it cannot determine whether the click missed, the selector was wrong, or the backend failed.
2. **Catastrophic Token & Turn Wastage:** The agent repeatedly retries alternative locators, coordinate clicks, arbitrary scrolls, and DOM queries across 10 to 15 turns—burning 40,000–80,000 tokens—before terminating with a misleading `StepTurnLimitExceededException` or `ElementNotFoundException`.
3. **Flawed Root-Cause Attribution:** Test reports indicate an automation locator failure rather than the actual defect (e.g. `HTTP 500 Internal Server Error` on `/api/cart/items` or an uncaught `TypeError: Cannot read properties of undefined` in bundle JS).

Current Neodymium `BrowserToolProvider` exposes 39 browser interaction and assertion tools, but zero protocol-level introspection tools for inspecting network status codes or client-side JavaScript console errors.

## 2. Proposed Architecture & Design

### Introspection Flow:
```text
+---------------------------------------------------------------------------------------+
|                               Agent ReAct Loop Turn                                   |
+---------------------------------------------------------------------------------------+
|  1. Primary Action: click("button.add-to-cart")                                       |
|     --> Dispatches DOM click event via Selenide                                       |
|                                                                                       |
|  2. SUT Background Activity:                                                          |
|     --> Fetch POST /api/cart/add returns HTTP 500 (Out of stock exception)            |
|     --> Uncaught TypeError logged to window.console                                   |
|                                                                                       |
|  3. Assertion / Verification Turn:                                                    |
|     --> Expected mini-cart drawer does not appear in DOM                              |
|                                                                                       |
|  4. Diagnostic Introspection:                                                        |
|     [Option A - Explicit]: Agent calls browser_get_network_failures()                 |
|     [Option B - Passive Guard]: AgentToolLoopStep inspects buffer on action failure    |
|                                                                                       |
|  5. Conclusive Fast-Fail:                                                             |
|     --> Immediate attribution: "Backend HTTP 500 on /api/cart/add: Out of stock"      |
|     --> Halts test immediately or branches test logic (Zero locator thrashing)        |
+---------------------------------------------------------------------------------------+
```

### Proposed Tools in `BrowserToolProvider`:

1. **`browser_get_console_errors`**:
   - **Signature:**
     ```json
     {
       "name": "browser_get_console_errors",
       "description": "Retrieves unhandled JavaScript exceptions, runtime errors, and console warnings logged by the browser since the last check.",
       "parameters": {
         "type": "object",
         "properties": {
           "min_level": {
             "type": "string",
             "enum": ["SEVERE", "WARNING", "INFO"],
             "description": "Minimum severity level to retrieve. Defaults to SEVERE."
           },
           "limit": {
             "type": "integer",
             "description": "Maximum number of recent entries to return (default: 20, max: 100)."
           },
           "clear_buffer": {
             "type": "boolean",
             "description": "Whether to drain the log buffer so subsequent calls only return new entries (default: true)."
           }
         }
       }
     }
     ```
   - **Engine Implementation:** Backed by Selenium W3C `driver.manage().logs().get(LogType.BROWSER)` and Chromium DevTools Protocol (`Runtime.consoleAPICalled`, `Runtime.exceptionThrown`).

2. **`browser_get_network_failures`**:
   - **Signature:**
     ```json
     {
       "name": "browser_get_network_failures",
       "description": "Retrieves failed HTTP network requests (status codes 4xx, 5xx, or network aborts/CORS errors) captured during page execution.",
       "parameters": {
         "type": "object",
         "properties": {
           "status_filter": {
             "type": "string",
             "enum": ["all_failures", "4xx", "5xx", "blocked"],
             "description": "Filter by failure classification. Defaults to all_failures."
           },
           "limit": {
             "type": "integer",
             "description": "Maximum number of failed requests to return (default: 20, max: 100)."
           },
           "clear_buffer": {
             "type": "boolean",
             "description": "Whether to drain the captured failure buffer (default: true)."
           }
         }
       }
     }
     ```
   - **Engine Implementation:**
     - In Chrome/Edge: Backed by CDP (`Network.enable`, listening to `Network.responseReceived` with `response.status >= 400`, and `Network.loadingFailed`).
     - In Proxied Environments: Backed by `BrowserUpProxy` / Selenide proxy request capture.
     - Fallback / Light: JavaScript Performance Resource Timing API (`window.performance.getEntriesByType('resource')`).

3. **Passive Diagnostic Enrichment (`AgentToolLoopStep`):**
   - When an action or assertion fails after multiple retries, `AgentToolLoopStep` automatically queries the buffered network and console diagnostics.
   - If an unhandled HTTP 5xx or fatal JS exception occurred within the turn window, the loop automatically appends a targeted advisory message into the conversation:
     `"[Diagnostic Alert]: Detected 1 HTTP 500 on endpoint '/api/cart/add' and 1 unhandled JS error: 'TypeError: Cannot read properties of undefined'. Avoid retrying selectors if the SUT state did not update due to application failure."`

4. **Companion JSON Playback Book Compatibility:**
   - **Zero Playback Impact:** Diagnostic inquiry tools do not emit mutating actions into the persistent no-LLM companion JSON playbook.
   - When authors use explicit assertions (e.g. `Verify no console errors occurred`), `PlaybookToolReplayer` maps this to a deterministic, pure-Java Selenide assertion (`AssertNoConsoleErrorsAction`) during CI replay.

## 3. Expected Impact & Trade-offs

- **Benefits:**
  - **Eliminates 10-15 Turn Thrashing Loops:** Transforms ambiguous locator retries into decisive root-cause identification on the first failed turn.
  - **Massive Token Reductions:** Prevents 40,000–80,000 tokens of wasteful screenshot and DOM re-ingestion when backend APIs fail.
  - **Flawless Defect Attribution:** Directly identifies whether a failure belongs in the framework locator layer or the SUT application backend.
  - **Zero Replay Fragility:** Unlike ref-based DOM targeting, protocol monitoring does not alter element locators or playback books.
- **Risks & Complexity:**
  - **Browser Compatibility:** CDP network capture is native in Chromium-based browsers; Firefox and Safari require WebDriver BiDi or proxy-based capture.
  - **Noise Filtering:** Web analytics, tracking pixels, and non-fatal CDN font 404s must be filtered out to prevent false alarms.

## 4. Open Questions & Alternatives Considered

- *Alternative 1: Rely solely on BrowserUpProxy.* Evaluated and rejected as mandatory because requiring a local proxy slows down test initialization and breaks on external upstream corporate proxies. Proxy capture should be a fallback, with CDP as the primary driver.
- *Alternative 2: Inject window error listeners via JavaScript.* Evaluated, but synthetic JS listeners cannot intercept network requests initiated by Web Workers or low-level CORS preflight rejections.
- *Open Question:* Should non-fatal tracking endpoint failures (e.g. Google Analytics / Segment 404s) be excluded by default via a configurable URL regex mask in `neodymium.properties` (`neodymium.ai.network.ignorePattern`)?
