# [DEF-20260930-09] Reasoning Effort Hardcoded in Agent Tool Loop and Missing Property Resolution

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-config`)
- **Scope:** `Framework`
- **Symptom:** Setting `neodymium.ai.reasoningEffort` or expecting a configurable thinking level has no effect on execution; agent execution turns always run with hardcoded `ReasoningEffort.LOW`.
- **Root Cause:** In `AgentToolLoopStep.java:463`, `ReasoningEffort.LOW` was hardcoded during `LlmRequest` creation. Furthermore, `AiConfiguration` only read `neodymium.ai.reasoningEffort` as a fallback in `getLinterReasoningEffort()`, lacking general and role-based resolution or support for the intuitive `thinkingLevel` property name.
- **Detection Gap ("What did we miss?"):** Unit tests in `AgentToolLoopStepTest` mocked LLM callers without verifying whether configured or dynamic reasoning effort properties were propagated to outgoing `LlmRequest` instances.
- **Resolution:**
  1. Implemented `getReasoningEffort(String role, ReasoningEffort defaultEffort)` and `getReasoningEffort()` in `AiConfiguration`, supporting role overrides (`neodymium.ai.<role>.reasoningEffort` / `thinkingLevel`), global properties, and alias parsing (`MINIMAL`, `NONE` -> `OFF`).
  2. Updated `AgentToolLoopStep` to dynamically resolve `reasoningEffort` from `AiConfiguration` and session data instead of hardcoding `LOW`.
  3. Updated `PlaybookLinter` to utilize the shared `AiConfiguration.parseReasoningEffort` helper.
  4. Documented the property options in `config/ai.properties`.
- **Safety Net Added:** Added `AiConfigurationTest.testReasoningEffortResolutionAndAliases` testing property hierarchies, role overrides, and alias parsing, and `AgentToolLoopStepTest.testConfiguredReasoningEffortPassedToLlmRequest` verifying effort propagation to `LlmRequest`.
