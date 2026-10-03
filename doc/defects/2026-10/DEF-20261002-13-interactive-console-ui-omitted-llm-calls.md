# [DEF-20261002-13] Interactive Console UI Omitted LLM Calls from Memoization Signature and Viewport Details

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`interactive_console.js`, `InteractiveReasoningAndActionsUiTest.java`)
- **Scope:** `Framework`
- **Symptom:** UI tests asserting `.llm-communications-section`, `.inline-reasoning-bubble`, and `.inline-reasoning-thinking` failed with `Element not found`.
- **Root Cause:**
  1. `renderBlock` in `interactive_console.js` constructed a DOM memoization signature (`sig`) that omitted `llmCalls`, causing state updates containing LLM communication history to skip re-rendering step HTML.
  2. Headless Chrome defaulted to standard mobile viewport width (<= 900px) which activated responsive CSS media queries hiding `#bigScreenStepDetails` (`display: none !important`).
- **Detection Gap ("What did we miss?"):** UI integration tests lacked explicit browser window sizing (`browserSize = "1400x900"`), and JS memoization tests did not verify `llmCalls` cache invalidation.
- **Resolution:** Added `s.llmCalls ? s.llmCalls.length : 0` to `interactive_console.js` `renderBlock` signature calculation and set explicit desktop viewport dimensions in `InteractiveReasoningAndActionsUiTest.java`.
- **Safety Net Added:** `InteractiveReasoningAndActionsUiTest.testLlmCommunicationsRenderingInInteractiveView()`.
