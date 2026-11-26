# [DEF-20261116-1305] Element Highlighting Introduces Artificial Delay in Headless Executions Without Recording

- **Status:** `Resolved`
- **Opened:** 2026-11-16 12:47
- **Closed:** 2026-11-16 13:38
- **Component:** `neodymium-core`, `neodymium-classic`
- **Scope:** `Framework & Config/Environment`
- **Symptom:** Test suites running with `neodymium.debugUtils.highlight=true` in headless browser modes without active video or GIF recording experience massive execution delays ($600\text{ ms}$ default sleep per element lookup).
- **Root Cause:** `ElementHighlightUtils.highlightAllElements` and `DebugUtils.highlightAllElements` execute a blocking sleep cycle ($2 \times \text{duration} \times \text{blinkCount}$) on every element find before immediately resetting the temporary DOM highlight overlay via `resetAllHighlight()`. In headless environments where no video or GIF recording is active, no human eye sees the viewport and no background thread captures frames during the sleep, rendering the delay completely wasteful. Furthermore, `Neodymium.isHeadless()` was refined to avoid premature fallback to `MultibrowserConfiguration.DEFAULT_BROWSER_ID` when running outside an active runner context.
- **Detection Gap ("What did we miss?"):** Existing highlight tests ran either with mock executions or specifically checked DOM mutations in short isolation, without considering whether headless CI/CLI test suites executing thousands of element lookups without recording benefit from skipping the visual blinking loop.
- **Resolution:**
  - Added `neodymium.debugUtils.highlight.headless.withoutVideo` property (default `false`) to `NeodymiumConfiguration`.
  - Added `Neodymium.isHeadless()` and `Neodymium.isHeadless(final WebDriver driver)` detecting `WebDriverStateContainer`'s `BrowserConfiguration.isHeadless()`, explicit `@Browser` profile definitions, Selenide `Configuration.headless`, and WebDriver `--headless` command-line flags without false-positive default profile fallbacks when runner context is absent.
  - Added `FilmTestExecution.isRecording()` and `FilmTestExecution.isRecording(WebDriver driver)` to detect live background recording threads for the current driver/test across raw drivers and Selenium 4 `EventFiringDecorator` proxies.
  - Added gatekeeper `Neodymium.shouldHighlightElements()` and `Neodymium.shouldHighlightElements(WebDriver driver)` integrated into `NeodymiumWebDriverListener`, `ElementHighlightUtils`, and `DebugUtils` across both `neodymium-core` and `neodymium-classic`.
- **Safety Net Added:** Added permanent regression test suites `org.neodymium.util.HighlightHeadlessOptimizationTest` in `neodymium-core` and `com.xceptance.neodymium.util.HighlightHeadlessOptimizationTest` in `neodymium-classic` verifying all truth-table permutations across headful, headless, active recording, and explicit override configurations.
