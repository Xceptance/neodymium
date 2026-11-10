# [DEF-20260925-01] Pre-Condition Assertion Timing and Inter-Test Hover State Leakage in HoverIntegrationTest

- **Status:** Resolved
- **Opened:** 2026-09-25
- **Closed:** 2026-09-25
- **Component:** `neodymium-core` (`live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** In `HoverIntegrationTest.testHoverNonExistentElementFailure`, running the full test suite resulted in state contamination where `#categories-dropdown` and `#preview-card` pre-condition assertions failed before the test execution started. Additionally, the Visual RCA diagnostic reported misleading application-state failure ("application may not have reached expected checkout state").
- **Root Cause:** 
  1. The test asserted pre-conditions (`$("#categories-dropdown").shouldNotBe(visible)`) before `session.execute(...)` was called. Since `Open ${hover.test.url}` was inside `session.execute(...)`, the assertions ran against the dirty DOM left by preceding tests (`testCssHoverDropdown` and `testDelayedHoverActivity`).
  2. The autonomous agent probed `button#btn-categories` during its exploratory search for the missing element, altering page state before throwing `ConclusiveFailureException`.
  3. The non-existent element name was specified as `'Place Order'` on a catalog page, prompting the multimodal RCA agent to hypothesize an incomplete e-commerce checkout transition.
- **Detection Gap ("What did we miss?"):** Single-method test runs initialized a fresh browser session and did not expose the inter-method state leakage that occurs when running the full class in sequence.
- **Resolution:** Explicitly navigate to the test URL prior to checking initial pre-conditions, add an `@AfterEach` cleanup step to neutralize mouse position, and rename the negative test target to a domain-neutral action (`'Non-Existent Action'`).
- **Safety Net Added:** Full test class suite execution passing cleanly in sequence with isolated pre/post assertions and neutral RCA diagnostics.
