# [DEF-20260924-03] Hover Action Tool Fallback to Body and Missing from Mutating Action Validation

- **Status:** Resolved
- **Opened:** 2026-09-24
- **Closed:** 2026-09-24
- **Component:** `neodymium-core` (`BrowserToolProvider`, `AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Hovering over a non-existent element in `HoverIntegrationTest.testHoverNonExistentElementFailure` (`Hover over 'Add to cart'`) did not throw an exception and falsely reported success.
- **Root Cause:** Dual framework defects:
  1. `BrowserToolProvider.createHoverTool()` only declared `selector` in its schema and only queried `resolveSelector(args)` (ignoring `text`). When the LLM invoked `hover` with `{"text": "Add to cart"}`, `resolveSelector` returned `""`. `findElement("")` defaulted to `$("body")`, causing the tool to hover over the visible document `<body>` and return a false success. Additionally, empty target arguments were not validated before execution.
  2. `AgentToolLoopStep.isMutatingTool(name)` omitted `"hover"` (and `"scroll"`). Even when `hover` failed on a non-existent element, `hasActionToolFailed` was never set. When the agent subsequently called `complete_step`, interactive step failure validation was bypassed because `hover` was not tracked as a mutating action tool.
- **Detection Gap ("What did we miss?"):** Unit tests for browser tools verified tool registration but did not test hover argument parsing, text fallback, or empty target validation. `AgentToolLoopStepTest` tested action failure handling for `check` and `click`, but lacked coverage for `hover` and missing mutating tool executions on interactive instructions.
- **Resolution:**
  1. Updated `BrowserToolProvider.createHoverTool` to accept `text` alongside `selector` in the JSON schema, validate that at least one is provided (returning `ToolResult.error` otherwise), and resolve targets via text or CSS.
  2. Added `"hover"` and `"scroll"` to `AgentToolLoopStep.isMutatingTool(name)`, updated `callSelector` extraction to check `text` and `target` properties, and required that interactive action instructions execute at least one successful mutating action tool before `complete_step` is accepted.
- **Safety Net Added:** Unit tests in `BrowserToolsTest.testHoverToolSchemaAndTargetValidation` and `AgentToolLoopStepTest.testCompleteStepRejectedWhenHoverActionToolFailed` and `testCompleteStepRejectedWhenInteractiveActionInstructionHasNoMutatingToolExecuted`.
