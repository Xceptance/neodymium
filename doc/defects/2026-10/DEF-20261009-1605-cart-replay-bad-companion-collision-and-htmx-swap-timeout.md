# [DEF-20261009-1605] Replay of `CartTest.replayBad` fails with timeout on `#cart-btn-anchor` due to companion playbook namespace collision and HTMX swap stall

- **Status:** `Resolved`
- **Opened:** 2026-10-09 16:05
- **Closed:** 2026-10-09 19:15
- **Component:** `neodymium-core` / `neodymium-e2e-tests`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** During replay of `CartTest.replayBad` on dataset `bad` (e.g., `CartTest_replayBad_bad_20261009-144129.html`), Step #3 (`Locate the first product card: Verify that the cart item count is higher than ${lineItemCount}`) failed with:
  `Assertion failed during replay: Expected text/pattern "CART 1" was not found on selector "#cart-btn-anchor" within 3000ms.`
  Additionally, companion playbook JSON files across `basic.CartTest`, `judge.CartTest`, and `full.CartTest` continually collided and overwrote one another in `target/playbooks/integration/cart/`.
- **Root Cause:** Two interacting issues caused this test failure:
  1. **Companion Playbook Namespace Collision:**
     `NeodymiumAiRunner.computeRecordingPath` generates companion filenames as:
     `recDir + "/" + testClass.getSimpleName() + "_" + recordingMethod + "_" + datasetId + "_" + browserProfile + ".json"`.
     Because all three cart test classes (`org.neodymium.ai.integration.verla.cart.basic.CartTest`, `org.neodymium.ai.integration.verla.cart.judge.CartTest`, and `org.neodymium.ai.integration.verla.cart.full.CartTest`) shared the identical simple class name `CartTest` and the identical recording directory `target/playbooks/integration/cart`, each live recording or replay in one package silently overwrote the recorded companion artifact (`CartTest_liveBad_bad_Chrome_1500x1000_headless.json`) used by the other test packages.
  2. **Asynchronous HTMX OuterHTML Swap Race Condition in `assert_text`:**
     In the Verla demo store under `verla-bad`, clicking `.add-btn.product-quick-add` triggers an asynchronous HTMX request:
     `hx-post="api/cart/add?productId=SKU-TOP-1000"` with `hx-target="#cart-btn-wrapper"` and `hx-swap="outerHTML"`.
     When HTMX swaps `#cart-btn-wrapper`, the existing `#cart-btn-anchor` element is detached from the active DOM.
     In `BrowserToolProvider.assert_text`, when polling for expected text on a selector, any `StaleElementReferenceException` thrown while inspecting detached DOM elements was swallowed into a general catch block that proceeded to sleep for 100ms. In parallel test runs with high CPU contention, this artificial 100ms sleep compounded across multiple attempts caused the 3000ms assertion timeout to expire before the re-query could inspect the newly attached DOM node.
- **Detection Gap ("What did we miss?"):** Companion recording paths previously assumed distinct simple class names within each recording directory. When packages had distinct subpackages (`basic`, `judge`, `full`) but identical class names, tests in different packages clobbered each other's recorded playbooks. Furthermore, `BrowserToolProvider.assert_text` handled DOM element polling without fast-retry semantics upon encountering stale elements during asynchronous DOM re-renders.
- **Resolution:**
  1. Updated `recordingDirectory` in `basic.CartTest`, `judge.CartTest`, and `full.CartTest` to dedicated subdirectories (`target/playbooks/integration/cart/basic`, `target/playbooks/integration/cart/judge`, and `target/playbooks/integration/cart/full`) ensuring fully isolated companion playbook artifacts.
  2. Populated dedicated directories with baseline playbooks.
  3. Implemented `isStaleException` detection in `BrowserToolProvider.java` to detect `StaleElementReferenceException` (and wrapped causes/messages) across element inspection methods (`matchesElementText`, `matchesElementOrAssociatedLabel`).
  4. Updated `BrowserToolProvider.assert_text` polling loop to catch `isStaleException` and immediately `continue;` without sleeping 100ms, enabling immediate re-query of the DOM to acquire the fresh element immediately after an HTMX DOM swap completes.
- **Safety Net Added:**
  - Automated unit regression tests in `BrowserToolProviderStabilityTest` verifying stability across element inspection and flexible selectors.
  - End-to-end regression tests across all three cart variants (`org.neodymium.ai.integration.verla.cart.basic.CartTest#replayBad`, `org.neodymium.ai.integration.verla.cart.judge.CartTest#replayBad`, and `org.neodymium.ai.integration.verla.cart.full.CartTest#replayBad`).
