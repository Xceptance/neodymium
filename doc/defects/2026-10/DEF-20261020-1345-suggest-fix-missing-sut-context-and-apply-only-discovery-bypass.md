# [DEF-20261020-1345] Interactive Suggest Fix Lacks SUT Context and Apply Only Bypasses Pause for Discovery Steps

- **Status:** `Resolved`
- **Opened:** 2026-10-20 13:45
- **Closed:** 2026-10-20 13:51
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** In interactive mode, clicking "Suggest Fix" generated instructions without considering the live SUT page, resulting in blind repetitions of failed assertions. Furthermore, clicking "Apply Only" immediately executed and failed assertion steps without pausing for user confirmation.
- **Root Cause:** 
  1. `handleSuggestFix` in `InteractiveConsoleListener.java` passed only the raw instruction string to a text LLM with zero SUT context (no URL, title, DOM, or failure message).
  2. `AgentToolLoopStep.java` only evaluated `hasProposedActions` (which excludes discovery tools and step completions), causing verification/discovery turns on newly applied steps to skip `pauseBeforeActionExecution` and execute to failure immediately.
- **Detection Gap ("What did we miss?"):** Prior tests mocked user actions on action-heavy steps (`click`), where `hasProposedActions` was true, missing assertion/verification steps that rely on discovery tools (`query_dom`) and complete without action tool calls.
- **Resolution:** 
  1. Injected live SUT state (current page URL, page title, simplified DOM, failure reason) into `handleSuggestFix` prompt construction in `InteractiveConsoleListener.java`.
  2. Introduced `KEY_REQUIRE_CONFIRMATION_ONCE` on `autoConfirm: false` and ensured `AgentToolLoopStep` pauses on turn 1 before any tool or discovery execution when confirmation is required.
- **Safety Net Added:** 
  1. Added `testSuggestFixIncludesFailureReasonAndSutDetailsInLlmPrompt` and updated `testApplyOnlyPausesBeforeActionExecutionAwaitingUserConfirmation` in `InteractiveConsoleListenerTest.java`.
  2. Added `testAgentToolLoopPausesOnTurnOneWhenRequireConfirmationOnceArmedEvenForDiscoveryTools` in `AgentToolLoopStepTest.java`.
