# [DEF-20261029-2326] Neodymium.getDriver() returns null in AI tests with @Browser

- **Status:** `Resolved`
- **Opened:** 2026-10-29 15:10
- **Closed:** 2026-10-29 23:26
- **Component:** `neodymium-core` (`NeodymiumAiRunner.java`)
- **Scope:** `Framework`
- **Symptom:** When running an AI test annotated with `@Browser` and `@NeodymiumAiTest`, `Neodymium.getDriver()` returned `null`, causing `NullPointerException` on direct WebDriver operations (e.g. cookie manipulation, window resizing, or raw Selenium interactions), even though Selenide's separate thread-local driver was active.
- **Root Cause:** In JUnit 5, `BeforeEachCallback` extensions execute in the order they are registered in `TestTemplateInvocationContext.getAdditionalExtensions()`. `NeodymiumAiRunner` registered `BrowserExecutionCallback` followed by `AiInvocationExtension`. `BrowserExecutionCallback.beforeEach` ran first and registered the WebDriver into Neodymium's thread context via `Neodymium.setWebDriverStateContainer(...)`. Immediately after, `AiInvocationExtension.executeBeforeEach` ran and invoked `Neodymium.clearThreadContext()` at line 861, wiping the thread-local context and discarding the driver reference. Additionally, `AiInvocationExtension.afterEach` invoked `Neodymium.clearThreadContext()` prior to `BrowserRunner.teardown()`, preventing teardown logic from accessing `Neodymium.getBrowserProfileName()`.
- **Detection Gap ("What did we miss?"):** Existing runner tests (`NeodymiumAiRunnerTest`) only validated metadata extraction and context counting in `provideTestTemplateInvocationContexts`, without executing the full `BeforeEachCallback` lifecycle chain with an active driver container. Furthermore, E2E tests previously interacted with the page solely through Selenide's `$(...)` selectors (which held their own reference via `WebDriverRunner.setWebDriver`), masking the fact that `Neodymium.getDriver()` was `null`.
- **Resolution:**
  1. Relocated `Neodymium.clearThreadContext()` to the beginning of `TestTemplateInvocationContext.getAdditionalExtensions()` in `NeodymiumAiRunner`, matching standard Neodymium's `TemplateInvocationContext` pattern so each invocation starts with a clean thread-local state before any callbacks run.
  2. Added `Neodymium.clearThreadContext()` to `NeodymiumAiRunner.provideTestTemplateInvocationContexts()` matching `NeodymiumRunner`.
  3. Removed `Neodymium.clearThreadContext()` from `AiInvocationExtension.executeBeforeEach()`, preserving the driver registered by `BrowserExecutionCallback.beforeEach()`.
  4. Removed `Neodymium.clearThreadContext()` from `AiInvocationExtension.afterEach()`, preserving `Neodymium.getBrowserProfileName()` for `BrowserRunner.teardown()`.
- **Safety Net Added:**
  - Added unit test `NeodymiumAiRunnerTest.testAiInvocationExtensionPreservesWebDriver` asserting `Neodymium.hasDriver() == true` and `Neodymium.getDriver() != null` after `AiInvocationExtension.beforeEach` executes, with clean Selenide and Neodymium thread-local driver disposal.
  - Added unit test `NeodymiumAiRunnerTest.testGetAdditionalExtensionsClearsThreadContext` verifying both `provideTestTemplateInvocationContexts` and `getAdditionalExtensions` independently wipe thread context cleanly.
  - Added unit test `NeodymiumAiRunnerTest.testAiInvocationExtensionAfterEachPreservesBrowserProfileName` verifying browser profile name is preserved across `afterEach`.
  - Added driver assertions in live `@Browser("Chrome_headless")` E2E test `ProgrammaticTestDataYamlTest.test`.
