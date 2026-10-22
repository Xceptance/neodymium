# [DEF-20261021-1635] AgentToolLoopStep Ignores Per-Step Playbook Timeout Tag

- **Status:** `Resolved`
- **Opened:** 2026-10-21 16:35
- **Closed:** 2026-10-21 16:40
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** Adding an explicit step timeout tag (e.g. `(timeout: 120s)` or `(timeout: 90000ms)`) to a playbook instruction is parsed into `PlaybookStep.timeoutMs` and applied to Selenide's element search (`Configuration.timeout`), but `AgentToolLoopStep` still fails with `Step timeout of 60s exceeded (elapsed: 60s)`.
- **Root Cause:** In `AgentToolLoopStep.execute`, `effectiveTimeoutSeconds` was computed solely from `this.timeoutSeconds` (the global configuration fallback from `AiConfiguration.getInstance().getStepTimeoutSeconds()`) and ignored `step.getTimeoutMs()` from the current `PlaybookStep`.
- **Detection Gap ("What did we miss?"):** Unit tests in `AgentToolLoopStepTest` only tested custom timeouts by passing constructor arguments (`new AgentToolLoopStep(..., timeoutSeconds)`), never asserting that `PlaybookStep.setTimeoutMs(...)` in `ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP` overrides the default loop timeout.
- **Resolution:**
  1. In `AgentToolLoopStep.java`, check `step.getTimeoutMs()` on the current `PlaybookStep`. When defined and positive, use it as `baseTimeoutSeconds` (converted to seconds with ceiling division via `(step.getTimeoutMs() + 999L) / 1000L`) before applying compound sub-step scaling.
  2. Raised default step wall-clock timeout fallback from 60s to 120s across `AiConfiguration.java`, `config/ai.properties`, and `doc/DOCUMENTATION.md` to comfortably accommodate multi-turn reasoning steps.
  3. Made per-turn LLM call timeout configurable via `neodymium.ai.execution.timeoutSeconds` (defaulting to 30s) in `AgentToolLoopStep`, disambiguating it from the 180s one-shot batch task timeout (`neodymium.ai.timeoutSeconds`).
- **Safety Net Added:** Unit tests `AgentToolLoopStepTest.testStopCriterion5PlaybookStepCustomTimeoutOverridesDefaultTimeout` and `AgentToolLoopStepTest.testStopCriterion5PlaybookStepCustomTimeoutExceededScalesSubStepsAndCeilDiv`.
