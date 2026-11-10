# [DEF-20261001-07] Ephemeral Verification Modals/Dropdowns Leak Across Steps Blocking Subsequent Actions

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** During live execution of CheckoutTest (e.g. `CheckoutTest_live_tailwind_20261001-220223.html`), verification steps like Step 2 ("Validate that United States as country is selected.") opened ephemeral modals/dropdowns to assert the selected list item and immediately called `complete_step` without closing them. In subsequent steps (such as Step 6: "Go to the cart using the mini cart"), the lingering modal and its backdrop blocked interaction with navigation elements, forcing the agent to spend 7 turns, 26 seconds, and over 60,000 tokens recovering.
- **Root Cause:**
  1. System Prompt Rule 4 did not instruct the agent to inspect active trigger/header elements first before expanding menus, nor did it mandate restoring initial page state (closing ephemeral modals/dropdowns/overlays) before invoking `complete_step`.
  2. Turn prompts for verification steps did not remind the agent to close opened dialogs/overlays prior to step completion.
- **Detection Gap ("What did we miss?"):** Existing tests in `AgentToolLoopStepTest` verified that assertion tools were called before `complete_step`, but never validated instructions preventing state leakage of ephemeral overlays into subsequent steps.
- **Resolution:**
  1. Hardened System Prompt Rule 4 in `AgentToolLoopStep`: Added explicit instructions to first check active trigger/header elements directly without opening menus, and mandated restoring initial page state (closing modals, dialogs, overlays, or dropdowns) before calling `complete_step`. Strictly forbade invoking `complete_step` while ephemeral dialogs or backdrops remain open.
  2. Enhanced Turn prompts in `AgentToolLoopStep` (both full DOM and visual screenshot branches) to explicitly remind the agent: *"ensure any opened modal or dropdown is closed to restore initial page state before completing"*.
- **Safety Net Added:** Regression unit test `AgentToolLoopStepTest.testVerificationStepSystemPromptInstructsModalClosureAndStateRestoration` verifying that both System Prompt Rule 4 and turn prompts explicitly enforce modal closure and state restoration before step completion.
