# [DEF-20260921-03] Missing assert_element_state and assert_attribute Tools Causing False Pass in testAssertReadonlyFailure

- **Status:** Resolved
- **Opened:** 2026-09-21
- **Closed:** 2026-09-21
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-pipeline`, `action-model`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertReadonlyFailure`, the step `Assert that the 'readonly-input' field is editable` succeeded unexpectedly, generating an HTML report marked `PASSED` while the test failed in JUnit (`AssertionFailedError: Expected java.lang.Throwable to be thrown, but nothing was thrown`).
- **Root Cause:**
  1. `BrowserToolProvider` registered only text, count, URL, and title assertion tools (`assert_text`, `assert_count`, `assert_url`, `assert_title`), lacking native tools to assert element states (`editable`, `readonly`, `enabled`, `disabled`, `visible`, `hidden`, `checked`, `selected`, etc.) and element attributes (`placeholder`, `value`, `href`, `data-*`).
  2. In `AgentToolLoopStep`, the agent was instructed to call an assertion tool on verification instructions before `complete_step`.
  3. Lacking `assert_element_state`, the LLM inspected `#readonly-input`, observed `value="FixedData"`, and hallucinated/substituted `assert_text({"selector": "#readonly-input", "expectedText": "FixedData"})`. Because "FixedData" was present, `assert_text` succeeded, the LLM called `complete_step`, and the step was marked successful without testing the required editable state.
- **Detection Gap ("What did we miss?"):** `SelenideTargetExecutor` and `AssertAction` had full support for `ASSERT_EDITABLE`, `ASSERT_READONLY`, `ASSERT_ATTRIBUTE`, etc., in playbook replay, but `BrowserToolProvider` (which supplies tools to the live LLM agent loop) had not exposed corresponding tools. Unit tests verified tool loop completion but did not verify state assertion failures during live recording.
- **Resolution:**
  1. Implemented `assert_element_state` in `BrowserToolProvider` supporting `["visible", "hidden", "enabled", "disabled", "editable", "readonly", "checked", "unchecked", "selected", "unselected", "focused", "exists", "absent"]` and throwing `AssertionError` when conditions fail.
  2. Implemented `assert_attribute` in `BrowserToolProvider` supporting exact, substring, and regex attribute assertions and throwing `AssertionError` on mismatch.
  3. Mapped both tools bidirectionally in `Action.java` (`toToolCall()` and `fromToolCall()`).
  4. Updated `AgentToolLoopStep` prompt guidelines and tool normalizations to recognize `assert_element_state` and `assert_attribute`.
- **Safety Net Added:** Unit tests in `BrowserToolProviderStabilityTest`:
  - `testAssertElementStateToolSchema`
  - `testAssertAttributeToolSchema`
  - `testNormalizeElementStateUtility`
  - `testAssertElementStateReadonlySuccessAndEditableFailure` (verifying `readonly` passes and `editable` on a readonly element throws `AssertionError`)
  - `testAssertAttributeSuccessAndFailure` (verifying attribute substring match passes and mismatch throws `AssertionError`)
