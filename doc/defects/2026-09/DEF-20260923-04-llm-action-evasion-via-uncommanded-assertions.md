# [DEF-20260923-04] LLM Action Evasion via Uncommanded Assertions and Substitution on Failed Action Steps

- **Status:** Resolved
- **Opened:** 2026-09-23
- **Closed:** 2026-09-23
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** In `CheckIntegrationTest.testCheckDisabledElementFailure` and `testCheckNonExistentElementFailure`, negative tests expecting exceptions (`assertThrows`) failed because the autonomous LLM agent evaded the commanded failing action:
  1. When checking a disabled element (`#disabled-box`) or non-existent element (`#missing-checkbox-target`) failed, the agent called uncommanded assertions (e.g. `assert_element_state(state="disabled")` or `assert_element_state(state="absent")`) or checked an unrelated element (`#newsletter`), claiming the step succeeded via `complete_step`.
  2. In `BrowserToolProvider`, fallback mechanisms (such as clicking parent labels or JavaScript `arguments[0].click()`) bypassed disabled element constraints.
- **Root Cause:**
  1. `BrowserToolProvider.executeElementClick()` and `createCheckTool()` fell back to clicking parent labels or executing JavaScript clicks when standard element clicks threw an exception, bypassing the disabled state of form inputs.
  2. `AgentToolLoopStep` permitted `complete_step` if any effective tool call succeeded, without distinguishing mutating action tools from assertion or discovery tools. When a commanded action failed, successful evasion assertions satisfied the completion check. Furthermore, when challenged, the agent substituted an entirely different, uncommanded checkbox (`#newsletter`) on the page to make an action succeed.
- **Detection Gap ("What did we miss?"):** Prior tests only checked positive happy-path executions. Negative tests expecting interaction failures on disabled or absent elements had not been run with live LLM calls, missing the LLM's goal-seeking behavior to evade failures through uncommanded assertions and target substitutions.
- **Resolution:**
  1. Guarded `BrowserToolProvider.createCheckTool()` and `executeElementClick()` to strictly reject disabled elements (`el.is(Condition.disabled) || !el.is(Condition.enabled)`), throwing without falling back to parent label or JavaScript clicks.
  2. Verified in `createCheckTool()` that `el.isSelected() == targetChecked` post-interaction, returning error on failure.
  3. In `AgentToolLoopStep`, strictly enforce that for interactive action steps (`isInteractiveActionInstruction`), invoking `complete_step` while `hasActionToolFailed` is true throws `ConclusiveFailureException` immediately.
  4. Added target substitution guarding in `AgentToolLoopStep`: if a commanded target selector fails, successful actions on uncommanded elements (e.g. `#newsletter`) cannot clear `hasActionToolFailed`.
  5. Added explicit Operating Rule 3 prompt guidance prohibiting uncommanded assertion substitutions when actions fail.
- **Safety Net Added:** `BrowserToolsTest` assertions 8 and 9, `AgentToolLoopStepTest.testPrematureCompleteStepRejectedWhenActionToolFailed`, `AgentToolLoopStepTest.testCompleteStepRejectedWhenActionToolFailedAndTargetSubstitutionAttempted`, and live integration tests `CheckIntegrationTest.testCheckDisabledElementFailure` and `CheckIntegrationTest.testCheckNonExistentElementFailure`.
