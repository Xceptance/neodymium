# [DEF-20261116-1240] Unbracketed Session Variable Placeholders ($var) in Tool Call Arguments Bypass Variable Resolution

- **Status:** `Resolved`
- **Opened:** 2026-11-16 12:25
- **Closed:** 2026-11-16 12:40
- **Component:** `neodymium-core` (`ToolCallVariableResolver`, `AgentToolLoopStep`, `SelenideTargetExecutor`, `SessionData`)
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** During live AI recording or execution, when input text references session or sensitive variables, LLMs occasionally emit shell-style unbracketed references (e.g. `"$password"` or `"$registeredEmail"`). The browser tools type the literal string `"$password"` into web elements instead of the actual credential value, causing authentication failures, unexpected redirects, and downstream step timeouts.
- **Root Cause:**
  1. `ToolCallVariableResolver.java` and `SelenideTargetExecutor.java` used a strict `!argsString.contains("${")` check, completely skipping variable resolution for unbracketed `$varName` placeholders even when `varName` matched an active key in `SessionData`.
  2. The system prompt in `AgentToolLoopStep.java` listed `### Active Session Variables` without explicitly instructing the LLM on the required `${variableName}` placeholder syntax.
  3. `SessionData.resolveNestedPath` handled keys with trailing dots (e.g. `password.`) by treating the empty trailing path segment as the root scalar value, which could swallow trailing punctuation.
- **Detection Gap ("What did we miss?"):** Previous fix DEF-20261102-1044 established `ToolCallVariableResolver` but only tested bracketed `${...}` syntax in `ToolCallVariableResolverTest`, missing shell-style `$var` syntax produced by LLM providers.
- **Resolution:**
  1. Updated `ToolCallVariableResolver.java` to support unbracketed `$varName` placeholders:
     - Introduced `normalizeUnbracketedVariables(text, sessionData)` with regex `(?<![\w\$])\$([a-zA-Z_](?:[a-zA-Z0-9_.]*[a-zA-Z0-9_])?)` and longest-match fallback against `sessionData.getEntry(key)`.
     - Preserves currency and price amounts (e.g. `$100`, `$19.99`), jQuery selectors (`$('#btn')`), and non-existent variables untouched.
     - Added `normalizePlaceholders(call, sessionData)` to normalize unbracketed variables in tool call arguments before execution and recording.
     - Updated `resolveVariables(call, sessionData)` fast-path to check for `$` rather than `${`, and normalizes unbracketed variables before resolving.
  2. In `AgentToolLoopStep.java`, normalized proposed tool calls via `ToolCallVariableResolver.normalizePlaceholders` in both single-shot and interactive loops so recorded actions retain canonical `${varName}` placeholders while execution receives resolved secrets.
  3. In `AgentToolLoopStep.java`, hardened the prompt under `### Active Session Variables:` with: `When referencing session variables in tool calls, use ${variableName} syntax (e.g. ${variableName}).`
  4. In `SelenideTargetExecutor.java`, updated `interpolateString` to normalize unbracketed variables via `ToolCallVariableResolver.normalizeUnbracketedVariables`.
  5. In `SessionData.java`, guarded `resolveNestedPath` against keys ending with a dot.
- **Safety Net Added:**
  - `ToolCallVariableResolverTest#testResolveVariablesNormalizesAndResolvesUnbracketedVariables` asserting unbracketed `$password` and `$user.email` resolve cleanly.
  - `ToolCallVariableResolverTest#testResolveVariablesPreservesMonetaryAndCurrencyAmounts` verifying `$100`, `$19.99`, and `$25OFF` are untouched.
  - `ToolCallVariableResolverTest#testResolveVariablesPreservesUnknownDollarIdentifiers` verifying `$unknownVar` and `$('#btn')` are untouched.
  - `ToolCallVariableResolverTest#testResolveVariablesResolvesUnbracketedWithTrailingPunctuation` verifying `"Your password is $password."` resolves to `"Your password is SecretP@ss123!."`.
  - `ToolCallVariableResolverTest#testNormalizePlaceholdersNormalizesUnbracketedVariables` verifying normalization to `${varName}` in recorded structures.
  - `ToolCallVariableResolverTest#testNormalizePlaceholdersHandlesPrefixCollisionCleanly` verifying prefix collision resolution (`$user.name` vs `$user.other`).
  - `ToolCallVariableResolverTest#testResolveVariablesNormalizesAndResolvesInFillFormStructure` verifying nested composite tool calls (e.g. `fill_form`).
  - `ToolCallVariableResolverTest#testNormalizeAndResolvePreservesJavaScriptVariablesInScriptTool` verifying that JavaScript identifiers in `execute_script` (e.g. `var $btn = ...`) are protected from unbracketed variable collision.
  - `AgentToolLoopStepTest#testSensitiveVariableUnbracketedPlaceholderNormalizedAndResolvedInToolExecutionWhilePreservingCanonicalPlaceholderInRecording` verifying end-to-end unbracketed resolution in agent tool execution, canonical bracketed recording, and conversation sanitization.
