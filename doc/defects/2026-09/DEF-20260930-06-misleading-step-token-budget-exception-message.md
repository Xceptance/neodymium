# [DEF-20260930-06] Misleading Step Token Budget Exception Message and Missing Step Budget Aliases

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-config`)
- **Scope:** `Framework`
- **Symptom:** When a composite or multi-turn playbook step exceeded the per-step token limit (`neodymium.ai.step.maxTokens`), the exception message reported `Token budget exceeded for test run: Total tokens consumed (...) exceeded configured step token budget (100000). Test run aborted.`. This led users to believe the test-level token budget had failed to pick up custom YAML `_properties:` (or was restricted to 100k despite total stats showing 600k+ tokens consumed). Furthermore, `AiConfiguration.getStepTokenBudget()` lacked intuitive aliases (`neodymium.ai.tokenBudget.step`, `tokenBudget.step`).
- **Root Cause:**
  1. `TokenBudgetExceededException.formatMessage` used a static prefix `"Token budget exceeded for test run:"` regardless of whether the exceeded budget was a test-level budget (`BudgetType.INPUT` / `BudgetType.OUTPUT`) or a per-step budget (`BudgetType.TOTAL`).
  2. `AiConfiguration.getStepTokenBudget()` only resolved the canonical key `neodymium.ai.step.maxTokens` and lacked fallback aliases matching the token budget naming pattern.
- **Detection Gap ("What did we miss?"):** Unit tests in `TokenBudgetGuardTest` and `AgentToolLoopStepTest` only checked that the exception was thrown and that it contained the numeric token limit, without asserting that the message clearly differentiated step-level failures from whole-test aborts.
- **Resolution:**
  1. Differentiated message formatting in `TokenBudgetExceededException.formatMessage`: `BudgetType.TOTAL` now explicitly states `"Token budget exceeded for step: Total tokens consumed (%d) exceeded configured step token budget (%d). Step aborted."`.
  2. Enhanced `AiConfiguration.getStepTokenBudget()` to support fallback aliases: `neodymium.ai.step.maxTokens`, `neodymium.ai.tokenBudget.step`, `neodymium.ai.step.tokenBudget`, and `tokenBudget.step`.
- **Safety Net Added:** Added unit tests in `PropertyPrecedenceOrderTest` (`testStepTokenBudgetAliasesAndOverrides` and `testTokenBudgetExceededExceptionStepMessageFormatting`) validating all aliases, precedence order, and message differentiation.
