# [DEF-20261105-1333] splitFrameTarget Rejects Framed XPath Selectors and FrameScope Fails Default Content Transitions

- **Status:** `Resolved`
- **Opened:** 2026-11-05 13:15
- **Closed:** 2026-11-05 13:33
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  1. Compound locators with XPath element targets containing `@` (e.g. `#frame >>> //input[@id='val']`), iframe XPath selectors (`//iframe[@id='frame1'] >>> #btn`), or CSS attribute selectors containing `@` (e.g. `[data-email='user@domain.com']`) failed to split into frame and element targets. `splitFrameTarget` returned `frame = null` and left the compound selector intact, causing element lookups in the top-level browsing context to fail.
  2. Opening an AutoCloseable `FrameScope` with `null` or `"main"` while currently inside a child frame did not switch the underlying WebDriver back to `defaultContent()`, leaving operations trapped in the active iframe.
- **Root Cause:**
  1. `FrameContextManager.splitFrameTarget` evaluated `rawTarget.contains("@")` as a heuristic for coordinate clicks, inadvertently matching all XPath `@` attribute expressions and CSS selectors containing at-signs.
  2. `FrameContextManager.splitFrameTarget` used naive `lastIndexOf(">>>")` which could prematurely split on literal `>>>` sequences located inside single or double-quoted selector arguments (e.g. `//button[text()='Next >>>']`).
  3. `FrameContextManager.enter` treated any `isDefaultContent(framePath)` as a no-op return without checking if `previous != null`.
- **Detection Gap ("What did we miss?"):**
  Unit tests for `splitFrameTarget` tested synthetic coordinate strings (`coord: 50, 100@#frame >>> #btn`) without verifying standard XPath locators with `@` attributes, email attributes, or framed default-content scope transitions.
- **Resolution:**
  1. Removed `rawTarget.contains("@")` from `FrameContextManager.splitFrameTarget`.
  2. Added `findLastUnquotedDelimiter` in `FrameContextManager` to skip `>>>` occurrences enclosed in single- or double-quoted string literals.
  3. Updated `FrameContextManager.enter` to call `restoreContext(driver, null)` when `isDefaultContent(framePath)` and `previous != null`, ensuring the driver returns to `defaultContent()` and restores `previous` upon scope closure.
- **Safety Net Added:**
  - `FrameContextManagerTest.testSplitFrameTargetSupportsXPathWithAtSign`
  - `FrameContextManagerTest.testSplitFrameTargetSupportsCssAttributeWithAtSign`
  - `FrameContextManagerTest.testSplitFrameTargetProtectsQuotedDelimiterInElementSelector`
  - `FrameContextManagerTest.testSplitFrameTargetFramedAnchoredCoordinates`
  - `FrameContextManagerScopeTest.testEnterDefaultContentFromInsideFrameSwitchesAndRestores`
