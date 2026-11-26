# [IDEA-20260913] Protocol-Level Assertions & Natural Language Tags

- **Status:** `Proposed`
- **Proposed:** 2026-09-13
- **Resolved:** Pending
- **Component:** `neodymium-core (SelenideTargetExecutor, PlaybookStep)`
- **Category:** `Protocol & Monitoring`
- **Author:** Neodymium Core Team

---

Test authors need the ability to enforce strict operational health checks directly within natural language steps without writing custom WebDriver glue code.

---

### Proposal: Built-In Protocol Control Tags and Actions

#### 1. Natural Language Playbook Control Tags
Allow test authors to append protocol assertion modifiers to any step:
* `Verify checkout modal opens (no-console-errors)`: Asserts that no JavaScript runtime exceptions occurred during modal rendering.
* `Click 'Submit Order' (no-network-errors)`: Asserts that all HTTP requests triggered by the action returned HTTP status $< 400$.
* `Click 'Apply Coupon' (expect-network-call: /api/coupons)`: Asserts that the specified network endpoint was called during the step.

#### 2. Built-In Executable Target Actions in `SelenideTargetExecutor`
* `ASSERT_NO_CONSOLE_ERRORS`: Queries `BrowserProtocolMonitor` and asserts that the step's console error buffer is empty.
* `ASSERT_NO_NETWORK_FAILURES`: Asserts that no HTTP requests completed with status $\ge 400$ or network-level errors during the step.
* `ASSERT_NETWORK_RESPONSE`: Asserts that a specified URL pattern was requested and returned an expected HTTP status (e.g. `target: "/api/cart"`, `value: "200"`).
