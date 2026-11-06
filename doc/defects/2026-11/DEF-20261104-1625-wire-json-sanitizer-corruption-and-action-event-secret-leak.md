# [DEF-20261104-1625] Multi-Turn Wire JSON Corruption and Plaintext Secret Leakage in ActionExecutedEvent

- **Status:** `Resolved`
- **Opened:** 2026-11-04 16:25
- **Closed:** 2026-11-04 16:45
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** 
  1. Multi-turn conversation messages with structured JSON tool results failed over the wire with malformed JSON syntax when secrets matched numeric or boolean literals (e.g. `1234` -> `[MASKED_VAR_pin]`), or when tool results contained hybrid JSON payloads followed by trailing diagnostic alerts (`\n\n⚠️ Passive Diagnostic Alert: ...`).
  2. Actions containing raw literal secrets dispatched `ActionExecutedEvent` with plaintext credentials in `resolvedAction`, causing `PreliminaryReportListener` to publish raw passwords into HTML test reports via `actionEntry.setResolvedValue(resolvedAction.getValue())`.
- **Root Cause:** 
  1. `LlmSanitizerHelper.toSanitizedRequest(...)` sanitized message contents via raw regex `maskingContext.mask(content)` rather than AST-aware `maskJsonOrTextContent(...)`, bypassing JSON literal protection on subsequent conversation turns. Additionally, `maskJsonOrTextContent` assumed pure JSON ending in `}` or `]`, falling back to raw regex when a trailing diagnostic alert was appended after `\n\n`.
  2. In `AgentToolLoopStep.java` (lines 1047, 1102, 1594, 3686), dispatching `new ActionExecutedEvent(canonicalAction, rawAction/mappedAction, ...)` passed unsanitized actions holding literal secrets as `resolvedAction`, allowing report listeners to expose unmasked secrets.
- **Detection Gap ("What did we miss?"):** 
  `AgentToolLoopStepTest#testSingleShotActionExecutedEventDispatchesMaskedRawAction` only asserted `aee.getAction()`, failing to inspect `aee.getResolvedAction()`. `LlmSanitizerHelperTest` validated pure JSON masking in isolation, but did not test multi-turn JSON message preservation through `toSanitizedRequest` or hybrid JSON payloads with diagnostic alerts.
- **Resolution:**
  1. Overloaded `LlmSanitizerHelper.maskJsonOrTextContent` with precompiled `MaskingContext` and integrated it into `toSanitizedRequest` for all outbound message contents. Handled hybrid JSON + alert payloads by partitioning on `\n\n`, applying AST masking to the JSON segment and regex masking to the alert text.
  2. In `AgentToolLoopStep.java` (lines 1047, 1102, 1594, 3686), dispatched `ActionExecutedEvent(canonicalAction, canonicalAction, ...)`, guaranteeing that literal secrets are replaced with variable placeholders in both parameters before being published to event buses and report listeners.
  3. Extracted `handleSingleShotUnresolvableVariable(...)` helper in `AgentToolLoopStep` to eliminate duplicated fallback handling.
- **Safety Net Added:**
  - `LlmSanitizerHelperTest#testToSanitizedRequestPreservesValidJsonInMessagesWhenSecretIsNumericOrBoolean`
  - `LlmSanitizerHelperTest#testMaskJsonOrTextContentWithHybridJsonAndDiagnosticAlert`
  - `AgentToolLoopStepTest#testSingleShotActionExecutedEventDispatchesMaskedRawAction` (expanded to assert `aee.getResolvedAction()` retains variable placeholders and leaks zero plaintext secrets)
