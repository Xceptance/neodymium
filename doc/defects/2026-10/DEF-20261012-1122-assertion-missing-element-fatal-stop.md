# [DEF-20261012-1122] Missing Element in Browser Assertion Tool Triggers Fatal Stop Instead of Retriable Tool Error

- **Status:** `Resolved`
- **Opened:** 2026-10-12 11:22
- **Closed:** 2026-10-12 13:05
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In live test executions (e.g., `CartTest_liveNormal_normal`), when an agent invoked `assert_text(selector: "#cart-btn-anchor .cart-count", expectedText: "1")`, the step failed immediately with an unhandled `AssertionError` instead of allowing the agent to inspect the page DOM, identify the correct locator (`.cart-badge`), and correct the action.
- **Root Cause:** In `BrowserToolProvider.java` (`createAssertTextTool` and `createAssertAttributeTool`), when polling timed out without a match, the tools unconditionally threw `AssertionError`. In `AgentToolLoopStep.java`, any `AssertionError` thrown from an assertion tool triggers Stop Criterion 2 ("Immediate fail on real defects!"), which immediately aborts the loop on the premise that an application state expectation was violated. However, when the specified selector matches 0 elements in the DOM, the failure is a locator mismatch rather than a verified semantic defect in the page content. Furthermore, returning a retriable `ToolResult.error` without tracking assertion state in `AgentToolLoopStep` introduced an oracle bypass risk where an agent could fail an assertion and immediately call `complete_step` to pass the step.
- **Detection Gap ("What did we miss?"):** Unit tests in `BrowserToolProviderStabilityTest` verified text mismatch assertions when elements existed, but never evaluated the scenario where an assertion tool was executed against a non-existent DOM selector to differentiate locator absence from value disagreement. Additionally, loop tests in `AgentToolLoopStepTest` only guarded against failing mutating actions on `complete_step`, not failing assertion tools.
- **Resolution:**
  1. Updated `createAssertTextTool` and `createAssertAttributeTool` in `BrowserToolProvider.java`:
     - For `assert_text`: When the wait loop times out without a match, safely check whether any element matching the selector exists in the DOM inside defensive `try / catch` blocks. If 0 elements exist, return `ToolResult.error` with a descriptive message ("Target element not found in DOM for selector '<selector>'. No element exists matching this selector. Use 'query_dom' to inspect the page structure or verify the selector."). If elements exist but text mismatches, throw `AssertionError` with element text preview to preserve Stop Criterion 2 for real content defects.
     - For `assert_attribute`: Wrapped assertion execution in `try / catch (final ElementNotFound e)` to return `ToolResult.error` when the target element is absent from the DOM, while letting `AssertionError` propagate when the attribute value mismatches on an existing element.
  2. Hardened `AgentToolLoopStep.java` with `hasUnresolvedAssertion` state tracking:
     - Tracks whether an assertion tool was executed and returned `ERROR`.
     - When `complete_step` is invoked while `hasUnresolvedAssertion == true`, rejects `complete_step` with guidance on the first attempt, and throws `ConclusiveFailureException` if repeated without successfully re-running an assertion, preventing any test oracle bypass.
     - Clears `hasUnresolvedAssertion` when an assertion tool subsequently executes with `SUCCESS`.
     - If the agent concludes without tool calls while an assertion is unresolved, aborts immediately with `ConclusiveFailureException`.
- **Safety Net Added:**
  - Added `@Test` methods `testAssertTextReturnsErrorWhenElementNotFoundInDom`, `testAssertAttributeReturnsErrorWhenElementNotFoundInDom`, and `testAssertAttributeThrowsAssertionErrorWhenValueMismatchesOnExistingElement` in [BrowserToolProviderStabilityTest.java](file:///home/rschwietzke/projects/GIT/neodymium-library/neodymium-core/src/test/java/org/neodymium/ai/tool/browser/BrowserToolProviderStabilityTest.java).
  - Added `@Test` methods `testCompleteStepRejectedWhenUnresolvedAssertionExists` and `testCompleteStepAllowedAfterUnresolvedAssertionResolved` in [AgentToolLoopStepTest.java](file:///home/rschwietzke/projects/GIT/neodymium-library/neodymium-core/src/test/java/org/neodymium/ai/pipeline/steps/AgentToolLoopStepTest.java).
