# [DEF-20260923-03] Non-Idempotent Check Action Toggling Checkbox State Upon Repeated Invocations

- **Status:** Resolved
- **Opened:** 2026-09-23
- **Closed:** 2026-09-23
- **Component:** `neodymium-core` (`BrowserToolProvider`, `AgentToolLoopStep`, `Action`)
- **Scope:** `Framework`
- **Symptom:** In `CheckIntegrationTest.testCheckIdempotency`, issuing a second check step on an already checked checkbox toggled the checkbox off (unchecking it) instead of keeping it checked, causing idempotency assertions to fail.
- **Root Cause:** In the Unified Tooling Architecture, there was no dedicated `check` browser tool. Autonomous agent actions targeting checkboxes were mapped to the generic `click` tool (and `AgentToolLoopStep.normalizeToolName` collapsed `case "click", "check" -> "click"`). Because standard HTML `<input type="checkbox">` elements toggle state on click, issuing two consecutive check instructions resulted in two clicks (false -> true -> false).
- **Detection Gap ("What did we miss?"):** Prior tests only exercised single-click checking or used Selenide's `setSelected()` helper directly in harness code, without verifying multi-step idempotency inside the autonomous agent tool loop.
- **Resolution:**
  1. Implemented a dedicated, idempotent `check` browser tool in `BrowserToolProvider` that inspects `el.isSelected()` and only clicks if the current state differs from the desired target state (`checked: true/false`, defaulting to `true`), with parent `<label>` and JS fallbacks. Disallowed unchecking individual radio buttons.
  2. Added defensive delegation in `BrowserToolProvider.createSelectTool()` so that radio and checkbox inputs mistakenly targeted with `select` are safely routed to check logic.
  3. Updated `AgentToolLoopStep` system prompt (Operating Rule 3) and `normalizeToolName` to recognize and dispatch `check` / `uncheck` directly.
  4. Updated `Action.toToolCall` and `Action.fromToolCall` to round-trip `CHECK` / `check` actions.
- **Safety Net Added:** `BrowserToolsTest.testBrowserCheckToolSchema` and `CheckIntegrationTest.testCheckIdempotency`.
