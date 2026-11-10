# [DEF-20261002-02] Replay Failure on Assertion Tools Due to Missing Automation ID DOM Stamping

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`BrowserToolProvider`, `SelenideElementFinder`, `LocatorResolver`)
- **Scope:** `Framework`
- **Symptom:**
  In `CheckoutTest.replay()` for dataset `tailwind-by-claude`, replay failed at step #15 ("Verify that the order summary shows the $31.98.") with `AssertionError: Expected text/pattern "$31.98" was not found on selector "[data-ai="xcrxcvi"]" within 3000ms.` The visual RCA erroneously diagnosed that the order summary showed $29.98 instead of $31.98, whereas viewport screenshots confirmed the page rendered $31.98 correctly.
- **Root Cause:**
  1. During live recording, `AgentToolLoopStep` stamped deterministic automation IDs (`data-ai="xc..."`) onto the live DOM via `PageAnalyzer.captureSimplifiedDom(ContextLevel.STANDARD)` on every agent turn. The LLM emitted `assert_text(selector="[data-ai=\"xcrxcvi\"]", text="31.98")`.
  2. In replay mode, the test navigated from `cart.html` to `checkout.html` via `click` and `wait_for_condition(type="url_matches")`. No interaction tool (`click`, `fill`) was invoked on `checkout.html` before the assertion.
  3. Interaction tools resolve elements via `SelenideElementFinder.findElement`, which triggers on-demand DOM stamping via `tryResolveAutomationId`. However, assertion tools (`assert_text`, `assert_count`, `assert_element_state`, `assert_attribute`) directly invoked `BrowserToolProvider.findElements` and `BrowserToolProvider.resolveLazyElement`. Both methods delegated to `LocatorResolver.resolveLocator` which merely translated CSS/XPath syntax without checking if the page was stamped with `data-ai`. As a result, `[data-ai="..."]` matched 0 elements in the unstamped DOM and timed out after 3,000ms.
  4. Additionally, `assert_text`, `assert_element_state`, and `assert_attribute` did not capture and attach `domFeatureVector` upon success during live execution, leaving the recorded playbook action without a feature vector and preventing `PlaybookToolReplayer` from performing cascade healing.
  5. Furthermore, `LocatorResolver` did not transform `#xc...` into `[data-ai='...']`, leaving `#xc...` resolving as HTML element `id` instead of synthetic `data-ai`.
- **Detection Gap ("What did we miss?"):**
  Previous unit tests for `assert_text` tested plain text assertions or mocked elements where `data-ai` was already present. There were no replay integration tests asserting on newly navigated pages targeting automation IDs before any user interaction occurred.
- **Resolution:**
  1. In `SelenideElementFinder`, introduced `isAutomationIdSelector(target)` and `ensureAutomationIdsStampedIfNeeded(target)` to dynamically stamp `data-ai` via `PageAnalyzer` whenever an automation ID (`[data-ai=...]`, `#xc...`, `xc...`) is targeted on an unstamped page or after a URL change.
  2. Updated `BrowserToolProvider.findElements(selector)` and `BrowserToolProvider.resolveLazyElement(selector)` to ensure automation IDs are stamped on demand before evaluating Selenide collections or lazy element proxies.
  3. Updated `LocatorResolver` and `SelenideElementFinder.resolveLocator` to normalize `#xc...` and bare `xc...` selectors into `[data-ai='...']`.
  4. Updated `BrowserToolProvider.assert_text`, `assert_element_state`, and `assert_attribute` to extract and attach `domFeatureVector` to the tool call result so recorded playbooks preserve feature vectors for cascade healing.
- **Safety Net Added:**
  Added unit tests in `LocatorResolverTest`: `testAutomationAndTestIdAttributes` asserting `#xc...` and `xc...` resolve to `[data-ai='...']`. Added unit tests in `SelenideElementFinderTest`: `testIsAutomationIdSelector` and `testResolveLocatorTransformsHashAutomationId`. Added regression test in `BrowserToolsTest`: `testAssertTextOnUnstampedPageWithAutomationIdTriggersStamping` verifying that calling `assert_text` with an automation ID on an unstamped page dynamically triggers stamping and succeeds. Verified full end-to-end replay passes in `CheckoutTest.replay()` with 0 LLM calls and 0 tokens.
