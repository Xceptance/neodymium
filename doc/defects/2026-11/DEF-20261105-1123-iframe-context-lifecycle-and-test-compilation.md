# [DEF-20261105-1123] Iframe Context Lifecycle Leaks, Coordinate Calculation Flaws, and Test Compilation Error in PR #509

- **Status:** `Resolved`
- **Opened:** 2026-11-05 11:23
- **Closed:** 2026-11-05 11:53
- **Component:** `neodymium-core`, `neodymium-e2e-tests`
- **Scope:** `Framework & Test/Harness`
- **Symptom:**
  1. `NestedIframesSandboxMockTest` failed to compile with `cannot find symbol: variable tools` at line 165.
  2. Autonomous workflow test `testNestedIframesAutonomousWorkflow` failed during replay modes with `IllegalArgumentException: Playbook cannot be empty: ... parsed to 0 executable steps` due to empty companion recording files.
  3. When browser instances were reused across test executions on the same worker thread, subsequent tests were trapped inside the active iframe of the previous test because the physical `WebDriver` was never returned to `defaultContent()`.
  4. In `ClickAction.java`, coordinate clicks on framed elements failed or clicked the wrong location, and threw cross-origin `DOMException` errors on cross-domain iframes, because JavaScript accumulated parent frame offsets while W3C WebDriver evaluates coordinates relative to the framed context's viewport.
  5. In `FrameContextManager.java`, frame path parsing was fragile against unspaced delimiters (e.g. `#a>>>#b`), window prefixes were not normalized before combining paths, resulting in duplicated paths (e.g. `win_0:main >>> #pact1 >>> #pact1`), and regex patterns were recompiled on every frame lookup.
- **Root Cause:**
  1. In `NestedIframesSandboxMockTest.java:165`, the code attempted to access `tools.get("assert_count")` using an undeclared `tools` map instead of `registry.getTool("assert_count")`.
  2. Programmatic playbooks without static playbook companions fail in `REPLAY_STRICT` and `REPLAY_WITH_HEALING` modes until PR #492 (`fix/neo-gh-492-programmatic-empty-recording-files`) is merged to develop.
  3. `NeodymiumAiRunner.afterEach` invoked `FrameContextManager.resetActiveFramePath()` to clear the thread-local state, but did not instruct the physical `WebDriver` to switch back to `defaultContent()`, leaving subsequent tests in the same browser session trapped inside the iframe.
  4. `ClickAction.java` traversed `win.frameElement` up to `window.top` accumulating bounding rectangle offsets in JavaScript. In W3C WebDriver, coordinates supplied to `Actions.moveToLocation(x, y)` in framed browsing contexts are evaluated relative to the active iframe's viewport, making parent offset accumulation redundant and breaking cross-origin iframes.
  5. `FrameContextManager` parsed frame hierarchies with fixed `>>>` strings without whitespace flexibility, and `combine()` failed to normalize paths before checking prefixes, resulting in duplicated hierarchy chains when locators included window specifiers.
- **Detection Gap ("What did we miss?"):**
  `NestedIframesSandboxMockTest` was added directly in PR #509 without running a maven test-compile before pushing, masking the syntax error. Unit tests for `FrameContextManager` tested pre-normalized inputs and did not exercise unspaced delimiters, coordinate expressions (`coord:`, `@`), or cross-session WebDriver context resets.
- **Resolution:**
  1. In `NestedIframesSandboxMockTest.java`, replaced `tools.get("assert_count")` with `registry.getTool("assert_count").orElseThrow()` and set `@AiMode(ExecutionMode.FORCE_RECORDING)` on `testNestedIframesAutonomousWorkflow`.
  2. In `NeodymiumAiRunner.java`, added `if (WebDriverRunner.hasWebDriverStarted()) { FrameContextManager.switchToDefaultContent(WebDriverRunner.getWebDriver()); }` in the teardown `finally` block before resetting thread-local active frame path.
  3. In `ClickAction.java`, reverted JavaScript to simple viewport-relative element bounds `arguments[0].getBoundingClientRect()`, matching `TypeAction.java` and W3C WebDriver framed context expectations.
  4. In `FrameContextManager.java`:
     - Extracted precompiled static `Pattern VOLATILE_ID_DETECTOR`.
     - Updated `normalizeFramePath`, `parseFrameHierarchy`, and `splitFrameTarget` with flexible whitespace regex `\s*>>>\s*`.
     - Implemented segment-level sliding-window suffix-prefix overlap merging in `combine()` to eliminate duplication across window prefixes, sub-hierarchies, and partial overlaps.
     - Added unique `src` filename resolution to `describeFrame()` to align runtime auto-discovery with `PageAnalyzer` DOM tree generation.
  5. In `BrowserToolProvider.java`:
     - Updated `wrapWithFrameSupport` to strip and combine frame prefixes from `source`, `target`, and `selector` arguments (supporting `drag_to`).
  6. In `Action.java`:
     - Expanded `FRAME_PREFIX_EXEMPT_TYPES` to protect tab management (`SWITCH_TAB`, `CLOSE_TAB`), alerts (`HANDLE_ALERT`), and REST actions (`GET`, `POST`, `PUT`, `DELETE`).
  7. In `FrameContextManagerTest.java`, `FrameContextManagerScopeTest.java`, and `BrowserToolProviderFrameWrapperTest.java`:
     - Added tests covering unspaced delimiters, window prefix normalization, coordinate expression exemptions, sliding-window overlap deduplication, `src` attribute frame description, and multi-endpoint tool argument stripping.
- **Safety Net Added:**
  - `FrameContextManagerTest.testParseFrameHierarchyFlexibleWhitespace`
  - `FrameContextManagerTest.testSplitFrameTargetFlexibleWhitespace`
  - `FrameContextManagerTest.testNormalizeFramePathWindowPrefixAndWhitespace`
  - `FrameContextManagerTest.testCombineWithWindowPrefixAndDeduplication`
  - `FrameContextManagerScopeTest.testDescribeFrameUsesUniqueSrcFilename`
  - `BrowserToolProviderFrameWrapperTest.testSourceAndTargetPrefixesAreBothStrippedAndMerged`
  - `NestedIframesSandboxMockTest.testNestedIframesAutonomousWorkflow`
