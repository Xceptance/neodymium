# [DEF-20261014-1615] Interactive Mode Confirmation Loop on Run Due to Unconditional Bindings Edited Flag

- **Status:** `Resolved`
- **Opened:** 2026-10-14 16:15
- **Closed:** 2026-10-14 16:18
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive mode, clicking "Run" to confirm a step's planned actions displays a spinner for a short time, fails to execute the proposed actions, and asks for confirmation again in an infinite loop.
- **Root Cause:** `interactive_console.js` defaulted `bindings: currentState?.dataBindings` into all outgoing payloads in `sendAction()`. `InteractiveConsoleListener.handleUserAction()` invoked `applyDataBindings()`, which unconditionally set `KEY_BINDINGS_EDITED = true`. In `AgentToolLoopStep`, `bindingsEdited == true` caused the engine to treat the `"RUN"` action as a step instruction edit, looping back to re-query the LLM (`turn--; continue turnLoop;`) and pausing again before action execution without executing any tools.
- **Detection Gap ("What did we miss?"):** Previous tests validated `"EDIT"` payloads with bindings, but did not assert that `"RUN"` payloads with bindings preserve normal execution without setting `KEY_BINDINGS_EDITED` or triggering prompt re-evaluation loops.
- **Resolution:**
  1. In `interactive_console.js`, removed `bindings: currentState?.dataBindings` from the default payload in `sendAction()`. Only actions explicitly specifying bindings (such as `saveEdit` or `applySuggestionOnly`) transmit bindings.
  2. In `InteractiveConsoleListener.java`, added an `isEditAction` guard so that `KEY_BINDINGS_EDITED` is only set in transient context data when the action is an explicit edit (`"EDIT"`, `"UPDATE_STEP"`, `"SAVE_STEP"`).
  3. In `AgentToolLoopStep.java`, guarded the prompt re-evaluation branch with `!isExplicitRun` so that explicit run actions (`"RUN"`, `"AUTO"`, `"CONTINUE"`, `"RUN_ALL"`) proceed to tool execution rather than re-querying the LLM.
- **Safety Net Added:**
  - `org.neodymium.ai.event.InteractiveConsoleListenerTest#testRunActionDoesNotSetKeyBindingsEdited`: Verifies that `"RUN"` actions containing bindings do not set `KEY_BINDINGS_EDITED`, update data bindings, and return `"RUN"` cleanly.
