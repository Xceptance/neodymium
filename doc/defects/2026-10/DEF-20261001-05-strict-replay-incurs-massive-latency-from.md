# [DEF-20261001-05] Strict Replay Incurs Massive Latency from Pre-Execution Feature Vector Scans and Dropdown Timeout Traps

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`PlaybookToolReplayer`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** Strict replay (`REPLAY_STRICT`) of small 13-step test cases required ~92–94s despite 0 LLM calls ($0.00 cost).
- **Root Cause:**
  1. `PlaybookToolReplayer.attemptHealing` was executed unconditionally during replay, even in `REPLAY_STRICT`. When `isDirectlyPresent` was false or delayed (e.g. async dropdowns, Select2 hidden selects, un-stamped data-ai), it invoked `new PageAnalyzer().extractFeatureVectors(driver)` which crawled the full DOM and forced CSS layout reflows across hundreds of elements.
  2. `BrowserToolProvider`'s `select` tool attempted `selectOptionByValue` before `selectOptionContainingText`. When the recorded parameter was display text (e.g. "15 Miles") instead of an option value, Selenide blocked for its full 3,000ms timeout before falling back to text.
  3. `assert_count` with `visibleOnly=true` performed sequential `el.isDisplayed()` calls over WebDriver wire for every matching element (25+ elements = ~1.5s IPC wire overhead).
- **Detection Gap ("What did we miss?"):** Existing replay unit tests ran against minimal in-memory mock HTML fixtures (<10 elements) where full-DOM feature extraction took <10ms and select options had matching values and text.
- **Resolution:**
  1. Updated `PlaybookToolReplayer` to completely bypass `attemptHealing` when `mode == ExecutionMode.REPLAY_STRICT` or when healing is disabled.
  2. Updated `BrowserToolProvider` `select` tool with `selectDropdownOption` inspecting options via JavaScript in <1ms to avoid blocking on Selenide's full timeout when distinguishing between option value and visible text.
  3. Replaced sequential `el.isDisplayed()` wire calls in `assert_count` with a single batch JavaScript visibility count.
- **Safety Net Added:** Unit tests in `PlaybookToolReplayTest` (`testReplayStrictBypassesHealingEvenWithLiveCandidatesAndFeatureVectors`) verifying `REPLAY_STRICT` bypasses `attemptHealing`, and `BrowserToolProviderStabilityTest` (`testAssertCountBatchVisibilityUsesJavaScriptExecutorWhenAvailable`) verifying batch visibility script execution.
