# [DEF-20260928-05] Circular Self-Referencing Variable Sanitization in Literal STORE Actions Breaks Playbook Replay

- **Status:** Resolved
- **Opened:** 2026-09-28
- **Closed:** 2026-09-28
- **Component:** `neodymium-core` (`action-sanitization` / `tool-loop` / `store-action`)
- **Scope:** `Framework`
- **Symptom:** Tests executing literal `STORE` actions passed in `FORCE_RECORDING` but failed in `REPLAY_STRICT` and `REPLAY_WITH_HEALING` with `UnresolvableVariableException: Unresolvable variable placeholder '${varName}' in template: "${varName}"` at `PlaybookToolReplayer.java:346`.
- **Root Cause:** When `store(variableName="varName", value="literalVal")` executed, it stored `"varName" -> "literalVal"` into `SessionData`. At step finalization, `DefaultActionSanitizer` and `AgentToolLoopStep.sanitizeToolCall` matched `"literalVal"` against `SessionData` and replaced it with `"${varName}"` inside the `store` tool's own arguments (`values: ["varName", "${varName}"]` and `arguments: {"variableName": "varName", "value": "${varName}"}`). On replay, `PlaybookToolReplayer` attempted to resolve `${varName}` before executing the `store` step, causing an unresolvable cyclic dependency.
- **Detection Gap ("What did we miss?"):** `DefaultActionSanitizerTest` only verified downstream consumer actions (e.g. `ASSERT_TEXT` referencing an existing variable), never testing sanitization of the `STORE` action itself or literal store tool calls.
- **Resolution:** Excluded the target variable name from candidate replacement variables during `STORE` action and `store` tool call sanitization, guarded `PlaybookToolReplayer` against resolving variable identifier keys, and repaired the recorded test playbook.
- **Safety Net Added:** Added unit regression test `testSanitizeStoreActionDoesNotSelfReferenceTargetVariable` in `DefaultActionSanitizerTest.java`.
