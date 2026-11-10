# [DEF-20261014-1425] PageAnalyzer CAPTURE_SCRIPT Omitted isDataAiEnabled Parameter

- **Status:** `Resolved`
- **Opened:** 2026-10-14 14:15
- **Closed:** 2026-10-14 14:25
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Tests asserting synthetic `[data-ai]` attribute generation (`BrowserToolsTest#testAssertTextOnUnstampedPageWithAutomationIdTriggersStamping`, `PageAnalyzerTest#testCleanDomStructureWithoutSel`, and `PageAnalyzerTest#testContainerClassPreservationWhenAutomationIdPresent`) failed because elements never had `data-ai` attributes stamped during DOM analysis, even when `neodymium.ai.dataAi.enabled` was explicitly set to `true`.
- **Root Cause:** In `PageAnalyzer.java`, the outer JavaScript evaluation wrapper invokes the embedded `CAPTURE_SCRIPT` closure. At line 700, the invocation passed only five arguments:
  ```javascript
  return (/* CAPTURE_SCRIPT */)(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
  ```
  However, `CAPTURE_SCRIPT` expects six arguments: `(root, preserveAttrs, removeInvisible, maxDepth, selectorStrategy, isDataAiEnabled)`. Omitting `arguments[5]` caused `isDataAiEnabled` to evaluate as `undefined` (falsy) inside the browser context, which bypassed synthetic AI marker assignment and stripped `[data-ai]` attributes on every capture.
- **Detection Gap ("What did we miss?"):** Commit `e95f115cf` disabled `neodymium.ai.dataAi.enabled=false` globally in `config/ai.properties` to transition toward container and visual anchors. The default falsy value masked the parameter forwarding bug in `PageAnalyzer.java`, and unit tests did not isolate parameter propagation to the injected JavaScript closure under enabled configurations.
- **Resolution:** Updated `PageAnalyzer.java` line 700 to include `arguments[5]` in the closure invocation:
  ```javascript
  return (/* CAPTURE_SCRIPT */)(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5]);
  ```
  Updated `PageAnalyzerTest` and `BrowserToolsTest` to explicitly set `neodymium.ai.dataAi.enabled` to `"true"` in a `try ... finally` block for tests that validate `[data-ai]` generation.
- **Safety Net Added:** Verified with `BrowserToolsTest#testAssertTextOnUnstampedPageWithAutomationIdTriggersStamping`, `PageAnalyzerTest#testCleanDomStructureWithoutSel`, and `PageAnalyzerTest#testContainerClassPreservationWhenAutomationIdPresent`.
