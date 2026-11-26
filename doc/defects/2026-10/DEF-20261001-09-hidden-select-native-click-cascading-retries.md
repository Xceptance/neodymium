# [DEF-20261001-09] Hidden Select Native Click Cascading Retries and Rapid Re-stamping Bottleneck Replay Latency

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`BrowserToolProvider`, `SelenideElementFinder`)
- **Scope:** `Framework`
- **Symptom:** In test cases with styled/hidden `<select>` elements (e.g., Select2 dropdowns in `SpaLocatorTest`), dropdown selection in replay mode incurred ~9.2 seconds of delay per select action. Concurrently, asynchronous modal mounting caused `tryResolveAutomationId` to execute heavy full-DOM serialization (`captureSimplifiedDom`) on every 100ms poll tick, adding ~10-13 seconds of latency.
- **Root Cause:**
  1. `BrowserToolProvider.selectDropdownOption` attempted standard Selenide click-based option selection (`selectOption`, `selectOptionByValue`, `selectOptionContainingText`) even when the target `<select>` element was visually hidden by CSS. When ChromeDriver failed with `ElementNotInteractableException`, Selenide waited up to `Configuration.timeout` (4,000ms) per method before cascading into two additional 4,000ms retry fallbacks.
  2. `SelenideElementFinder.tryResolveAutomationId` unconditionally re-analyzed and stamped the entire DOM via `PageAnalyzer.captureSimplifiedDom(ContextLevel.STANDARD)` whenever an automation ID was not immediately matched, invoking heavy DOM serialization up to 10 times per second during async modal appearance.
  3. `SelenideElementFinder` contained an SUT-specific class check (`select2-hidden-accessible`) violating framework neutrality.
- **Detection Gap ("What did we miss?"):** Existing stability tests in `BrowserToolProviderStabilityTest` used mock drivers that simulated synchronous option click completion without modeling ChromeDriver's `ElementNotInteractableException` timeout retry loops on hidden elements. Furthermore, no polling benchmark measured the CPU and serialization overhead of unthrottled `captureSimplifiedDom` calls during element polling.
- **Resolution:**
  1. Enhanced `BrowserToolProvider.selectDropdownOption` to inspect element visibility in JavaScript. For visually hidden or non-interactable selects, it immediately updates `selectedIndex`, `selected`, `value`, and dispatches standard W3C `input` and `change` events in ~1ms without blocking.
  2. Added non-blocking DOM event fallback (`applyDomSelectFallback`) if native selection throws an exception, eliminating cascading 4-second timeout retries.
  3. Throttled `captureSimplifiedDom` in `SelenideElementFinder.tryResolveAutomationId` to a minimum interval of 1200ms per URL, preventing high-frequency DOM re-stamping while keeping 100ms lightweight CSS polling.
  4. Removed the SUT-specific `select2-hidden-accessible` class check from `SelenideElementFinder`, universally supporting any `<select>` element.
- **Safety Net Added:**
  - `BrowserToolProviderStabilityTest.testSelectToolFastPathForHiddenSelectElement`: Verifies that hidden select elements immediately execute W3C DOM dispatch via `domSelected` without timeout delay.
  - `SelenideElementFinderTest.testResetDomStampCacheForTesting`: Verifies DOM stamping cache reset capability.
