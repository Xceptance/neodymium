# [DEF-20261009-2315] Replay of `CartTest.replayBad` fails with timeout on `.cart-badge` due to CDP spin-lock event-loop starvation during HTMX swap and cart state carryover

- **Status:** `Resolved`
- **Opened:** 2026-10-09 23:15
- **Closed:** 2026-10-09 23:30
- **Component:** `neodymium-core` / `neodymium-e2e-tests`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** During strict replay of `CartTest.replayBad` on dataset `bad` (e.g., `target/ai-results/CartTest_replayBad_bad_20261009-225956.html`), Step #3 (`Locate the first product card`) failed with:
  `Assertion failed during replay: Expected text/pattern "1" was not found on selector ".cart-badge" within 3000ms.`
  However, in the failure screenshot (`failure_225956.png`) captured immediately following the step failure, the header cart badge was clearly visible displaying `"1"` (`CART 1`), and the mini-cart dropdown panel was rendered open with the newly added item.
- **Root Cause:** Two interacting issues caused this timing failure:
  1. **CDP Flood & Event Loop Starvation in `assert_text` (`neodymium-core`):**
     In `BrowserToolProvider.assert_text`, when polling for expected text on a target selector, any `StaleElementReferenceException` thrown while inspecting detached DOM elements (e.g. during an HTMX `outerHTML` replacement of `#cart-btn-wrapper`) was caught and immediately followed by `continue;` with zero sleep.
     In headless Chrome, JavaScript execution, DOM layout/mutations, and Chrome DevTools Protocol (CDP) command dispatch share the browser's single-threaded event loop. When ChromeDriver flooded CDP with continuous, unthrottled DOM element lookups without yielding, Chrome's main thread was starved, preventing HTMX's asynchronous event callback from completing the DOM insertion until the 3,000 ms `Configuration.timeout` expired in Java. Once Java stopped polling and threw `AssertionError`, the event loop yielded, HTMX rendered the swap, and the failure screenshot was taken of the updated state.
  2. **Shopping Cart State Carryover across Test Methods (`neodymium-e2e-tests`):**
     In `CartTest.java` (across `basic`, `judge`, and `full` packages), `@BeforeEach public void setup()` only invoked `server.resetInventory()`, omitting `server.resetCarts()`, `server.resetSessions()`, and `server.resetOrders()`. If cookies (`verla_cart_id`) persisted across sequential test executions, cart items from preceding runs remained active in `EmbeddedHtmlServer.activeCarts`, causing initial line item counts and badge assertions to fall out of sync with clean-slate playbook expectations.
- **Detection Gap ("What did we miss?"):**
  1. Previous fix DEF-20261009-1605 introduced fast-retry on `isStaleException` without adding a minimal backoff sleep, overlooking that unthrottled CDP command loops starve the browser main thread in headless Chrome.
  2. Test harness setup previously assumed resetting product inventory was sufficient for isolation, ignoring in-memory server state for active carts, orders, and sessions.
- **Resolution:**
  1. Updated `BrowserToolProvider.assert_text` to sleep for 25ms (`Selenide.sleep(25)`) when catching `isStaleException` before retrying, yielding execution cycles to Chrome's event loop so asynchronous HTMX DOM insertions complete without starvation.
  2. Updated `@BeforeEach setup()` in `org.neodymium.ai.integration.verla.cart.basic.CartTest`, `judge.CartTest`, and `full.CartTest` to call `server.resetAll()` (`resetInventory()`, `resetCarts()`, `resetOrders()`, `resetSessions()`, `resetUsers()`) to ensure complete test isolation.
- **Safety Net Added:**
  - Automated unit test suite `BrowserToolProviderStabilityTest` verifying polling stability across dynamic and stale element transitions.
  - End-to-end execution of `CartTest#replayBad` verifying deterministic replay without assertion timeouts.
