# [DEF-20261124-0548] Mock WebDriver Proxy NPE, Stale Splash Text, and Locale-Hardcoded Progress Assertions in Test Suites

- **Status:** `Resolved`
- **Opened:** 2026-11-24 05:28
- **Closed:** 2026-11-24 05:49
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Test Harness & Fixtures`
- **Symptom:**
  1. `BrowserToolProviderStabilityTest` (14 errors) and `NeodymiumAiRunnerTest.testAiInvocationExtensionPreservesWebDriver` (1 error) threw `NullPointerException: Cannot invoke "java.lang.Boolean.booleanValue()" because the return value of "java.lang.reflect.InvocationHandler.invoke(Object, java.lang.reflect.Method, Object[])" is null` during `WebDriverRunner.closeWebDriver()`.
  2. `AiLauncherTest.testStartSplashServerAllocatesEphemeralPortAndServesLoadingHtml` and `NeodymiumManagerLauncherTest.testStartSplashServerAllocatesEphemeralPortAndServesLoadingHtml` failed asserting `body.contains("Starting AI Manager")`.
  3. `StaticResourceSyntaxTest.testSidebarRunnerProgressLogicEdgeCases` failed on `'Formatted count'` when run under a non-US OS locale.
- **Root Cause:**
  1. `Proxy.newProxyInstance` mock `WebDriver` handlers returned `null` for `Object.equals(Object)`, causing Selenide's `DisposablesRegistry.unregister()` (`Objects.equals(...)`) to unbox `null` to `boolean`.
  2. `loading.html` was rebranded to `"Starting Neodymium AI Manager..."`, leaving stale `"Starting AI Manager"` assertions in both launcher tests.
  3. `dashboard-core.js` uses `new Intl.NumberFormat().format(n)` (OS-locale-dependent, e.g. `4.500` in `de_DE`), whereas `StaticResourceSyntaxTest` hardcoded `'4,500'` / `'4500'`.
- **Detection Gap ("What did we miss?"):**
  Release test suites were not previously executed end-to-end under non-US locales or after the `loading.html` rebrand and Selenide registry cleanup changes.
- **Resolution:**
  1. Added `createProxy(...)` in `BrowserToolProviderStabilityTest` and updated `NeodymiumAiRunnerTest` to implement `equals`, `hashCode`, and `toString` on mock `WebDriver` proxies.
  2. Updated `AiLauncherTest` and `NeodymiumManagerLauncherTest` to assert `"Starting Neodymium AI Manager"`.
  3. Updated `StaticResourceSyntaxTest.testSidebarRunnerProgressLogicEdgeCases` to compute expected formatted numbers via `new Intl.NumberFormat().format(...)`.
- **Safety Net Added:**
  - Verified with targeted Maven runs of `BrowserToolProviderStabilityTest`, `NeodymiumAiRunnerTest`, `AiLauncherTest`, `NeodymiumManagerLauncherTest`, and `StaticResourceSyntaxTest#testSidebarRunnerProgressLogicEdgeCases`.
