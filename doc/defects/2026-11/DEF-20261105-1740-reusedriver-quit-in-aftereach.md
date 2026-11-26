# [DEF-20261105-1740] Cached WebDriver Terminated by AiInvocationExtension During AfterEach Execution

- **Status:** `Resolved`
- **Opened:** 2026-11-05 15:50
- **Closed:** 2026-11-05 17:48
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** AI tests alternate between passing and failing with `org.openqa.selenium.NoSuchSessionException: Session ID is null. Using WebDriver after calling quit()?` when combining `neodymium.webDriver.reuseDriver = true`, `neodymium.webDriver.startNewBrowserForCleanUp = true`, and an `@AfterEach` method in a `@NeodymiumAiTest` test class (GitHub issue #479).
- **Root Cause:** When `neodymium.webDriver.startNewBrowserForCleanUp = true` and an `@AfterEach` method is present, `BrowserExecutionCallback.interceptAfterEachMethod()` executes `browserRunner.teardown(testFailed)` prior to the invocation of `@AfterEach`. When `neodymium.webDriver.reuseDriver = true`, this teardown caches the active driver in `WebDriverCache.instance` and detaches the thread-local state (`Neodymium.setWebDriverStateContainer(null)`). In the subsequent `AiInvocationExtension.afterEach()` callback, the cleanup logic evaluated `!Neodymium.hasDriver() && WebDriverRunner.hasWebDriverStarted()`. Because Neodymium's state container was detached, this condition evaluated to `true`, prompting `WebDriverRunner.closeWebDriver()` to be called. Selenide proceeded to call `quit()` on the driver instance that was resting in `WebDriverCache`. When the subsequent test retrieved this cached driver instance, it crashed immediately with `NoSuchSessionException: Session ID is null`.
- **Detection Gap ("What did we miss?"):** Existing `NeodymiumAiRunnerTest` unit tests verified that `AiInvocationExtension` did not prematurely wipe `WebDriverStateContainer` or the browser profile name, but did not simulate the exact JUnit 5 callback sequencing where `BrowserExecutionCallback` caches the driver and detaches `WebDriverStateContainer` before `AiInvocationExtension.afterEach()` runs with Selenide's `WebDriverRunner` holding the cached reference.
- **Resolution:**
  1. Added `public boolean containsWebDriver(final WebDriver driver)` to `WebDriverCache` using recursive, bounded (`depth < 10`), symmetric unwrapping for raw, wrapped (`WrapsDriver`), and decorated WebDriver instances.
  2. In `NeodymiumAiRunner.AiInvocationExtension.afterEach()`, differentiated between cached drivers, managed test drivers, and unmanaged standalone drivers when `!Neodymium.hasDriver() && WebDriverRunner.hasWebDriverStarted()`:
     - If the driver is present in `WebDriverCache` (`isCached`), invoked `WebDriverRunner.resetWebDriver()` to safely detach Selenide's ThreadLocal reference without quitting the driver.
     - If the test was unmanaged (`this.browser == null`), invoked `WebDriverRunner.closeWebDriver()` to prevent standalone browser leaks.
     - If the test was managed (`this.browser != null`) and already handled by `BrowserRunner` (kept open or quit), invoked `WebDriverRunner.resetWebDriver()` to clear Selenide's ThreadLocal without redundant re-quitting.
- **Safety Net Added:**
  - `org.neodymium.ai.junit.NeodymiumAiRunnerTest#testAiInvocationExtensionPreservesCachedWebDriverAcrossAfterEach`: Verifies that `AiInvocationExtension.afterEach` does not quit or remove a driver residing in `WebDriverCache`.
  - `org.neodymium.ai.junit.NeodymiumAiRunnerTest#testAiInvocationExtensionCleansUpWhenNotCached`: Verifies that an unmanaged standalone driver is properly closed by `afterEach`.
  - `org.neodymium.ai.junit.NeodymiumAiRunnerTest#testWebDriverCacheContainsWebDriver`: Validates `WebDriverCache.containsWebDriver` with raw, wrapped, and proxy instances.
