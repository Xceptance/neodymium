# [DEF-20261001-06] Inflexible Context Level Syntax in PlaybookStep Causes Silent Fallback to Full DOM

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`PlaybookStep`, `ContextLevel`, `AgentToolLoopStep`, `ExecuteActionsStep`)
- **Scope:** `Framework`
- **Symptom:** Step annotations using concise syntax such as `(context: lean)` or `(context: none)` were silently ignored by `PlaybookStep` because `CONTEXT_LEVEL_PATTERN` only matched the longer `contextlevel` prefix. The step silently fell back to the project default `STANDARD`, incurring unwanted ~25k token full-DOM dumps.
- **Root Cause:**
  1. `PlaybookStep.CONTEXT_LEVEL_PATTERN` only matched `contextlevel[:=]` and did not accept `context[:=]`.
  2. `ContextLevel.fromString` did not recognize `NONE` or `ZERO` as synonyms for zero-DOM `HINT`.
  3. `AgentToolLoopStep` did not check `step.getContextLevel()` directly as the primary authority over transient state.
- **Detection Gap ("What did we miss?"):** `PlaybookStepTest` only tested `(contextlevel=...)`, missing the shorter and more ergonomic `(context: ...)` syntax.
- **Resolution:**
  1. Updated `PlaybookStep.CONTEXT_LEVEL_PATTERN` to support both `contextlevel` and `context` with `:` or `=`.
  2. Added `NONE`, `ZERO`, `NODOM`, and `NO_DOM` mappings to `ContextLevel.HINT` in `ContextLevel.fromString`.
  3. Enforced explicit `step.getContextLevel()` precedence in `AgentToolLoopStep` and `ExecuteActionsStep`.
- **Safety Net Added:** Unit tests in `PlaybookStepTest` (`testContextLevelTagParsing`), `ContextLevelTest` (`testFromStringParsing`), and `AgentToolLoopStepTest` (`testExplicitStepContextLevelOverridesConfiguredDefaultAndTransientState`, `testExplicitStepContextNoneResolvesToHintZeroDom`).
