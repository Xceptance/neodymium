# [DEF-20261125-1433] ActionExecutedEvent resolvedAction Leaks Plaintext Secrets After Online Healing Telemetry Change

- **Status:** `Resolved`
- **Opened:** 2026-11-25 14:33
- **Closed:** 2026-11-25 14:41
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `PlaybookToolReplayer`, `ToolCallVariableResolver`, `SessionData`, `DefaultActionSanitizer`)
- **Scope:** `Core Engine` & `Security / Sanitization`
- **Issue:** GitLab `#174`
- **Symptom:**
  `AgentToolLoopStepTest#testSingleShotActionExecutedEventDispatchesMaskedRawAction` failed at line 5191 (`expected: <${user.password}> but was: <secretPassword123>`). Any action referencing sensitive `SessionData` variables dispatched plaintext secret values in `ActionExecutedEvent.resolvedAction`, which `PreliminaryReportListener` then serialized to JSON/HTML/Markdown reports via `actionEntry.setResolvedValue(...)`.
- **Root Cause:**
  1. PR #579 (`b7c8bfcc9`) introduced `AgentToolLoopStep.buildResolvedAction(...)` and `dispatchActionExecutedEvent(...)` to populate runtime-resolved values in `ActionExecutedEvent` telemetry for online-healed and recorded actions. It either reused the fully resolved `preResolvedCall` (`executableCall`) on the single-shot `TargetExecutor` path or invoked `ToolCallVariableResolver.resolveVariables(sourceCall, sessionData)` on the single-shot tool and interactive loop paths, expanding sensitive `SessionData` entries alongside non-sensitive variables without masking or skipping sensitive entries, regressing `DEF-20261104-1625`.
  2. `PlaybookToolReplayer.dispatchActionEvent(...)` had the same latent leak during replay because it constructed `ActionExecutedEvent` from `variableResolvedCall` and `finalCall` after `resolveVariables(rawCall, sessionData)` had already expanded sensitive credentials.
  3. `SessionData.getRawSensitiveData()` converted sensitive values via `String.valueOf(...)` rather than flattening sensitive nested `Map` structures via `flattenObject(...)`, leaving nested sensitive paths unmasked in sanitizers, and `DefaultActionSanitizer.sanitize(...)` omitted `negated`, `subStepIndex`, `subStepInstruction`, `healed`, and `expectedTarget`.
- **Detection Gap ("What did we miss?"):**
  PR #579 added online-healing report telemetry tests in `ExecuteActionsStepTest` using only non-sensitive variables (`searchPhrase = "Neodymium"`), and `AgentToolLoopStepTest` was not executed before merging. Additionally, no test covered sensitive-variable masking in the interactive tool-loop, single-shot registered-tool, literal-secret LLM output, or replay `ActionExecutedEvent` dispatch paths.
- **Resolution:**
  1. Added sensitive-preserving variable resolution (`SessionData.resolveNonSensitiveVariables(...)`, `SessionData.resolveAvailableNonSensitiveVariables(...)`, and `ToolCallVariableResolver.resolveNonSensitiveVariables(...)`) that expands non-sensitive placeholders while keeping sensitive placeholders verbatim (including nested sensitive `Map` paths and transitive non-sensitive references), and flattened sensitive `Map` entries in `SessionData.getRawSensitiveData()`.
  2. Added `DefaultActionSanitizer.sanitizeSensitiveOnly(...)` (reusing shared `sanitizeAction` and `replaceVariablesInText` helpers) to scrub raw literal secret values without reverting resolved non-sensitive variables, while preserving `isRegex`, `negated`, `subStepIndex`, `subStepInstruction`, `healed`, and `expectedTarget`.
  3. Updated `AgentToolLoopStep.buildResolvedAction(...)` and `dispatchActionExecutedEvent(...)` to stop consuming fully expanded `preResolvedCall`, resolving from `sourceCall` via `resolveNonSensitiveVariables(...)` and scrubbing with `sanitizeSensitiveOnly(...)` before applying healing metadata.
  4. Hardened `PlaybookToolReplayer.dispatchActionEvent(...)` across all replay and reactive-healing dispatch sites to emit canonical sanitized `action` alongside `resolvedAction` built via `resolveNonSensitiveVariables(...)`, `overlayHealedLocatorArgs(...)`, and `sanitizeSensitiveOnly(...)`.
- **Safety Net Added:**
  - `AgentToolLoopStepTest#testSingleShotActionExecutedEventDispatchesMaskedRawAction` (restored green)
  - `AgentToolLoopStepTest#testSingleShotToolPathRetainsSensitivePlaceholderInActionExecutedEventWhileExecutingSecret`
  - `AgentToolLoopStepTest#testInteractiveLoopRetainsSensitivePlaceholderAndExpandsNonSensitiveInResolvedAction`
  - `AgentToolLoopStepTest#testResolvedActionScrubsLiteralSecretEmittedDirectlyByLlm`
  - `PlaybookToolReplayTest#testReplayStepRetainsSensitivePlaceholderInActionExecutedEventWhileExecutingSecret`
  - `PlaybookToolReplayTest#testHealedReplayStepRetainsSensitivePlaceholderInActionExecutedEventWhileUpdatingTarget`
  - `SessionDataTest#testResolveNonSensitiveVariablesRetainsSensitivePlaceholdersAndExpandsNonSensitive`
  - `SessionDataTest#testResolveNonSensitiveVariablesRetainsTransitiveSensitiveReferences`
  - `SessionDataTest#testResolveNonSensitiveVariablesRetainsNestedSensitiveMapPlaceholderAndFlattensSensitiveData`
  - `ToolCallVariableResolverTest#testResolveNonSensitiveVariablesExpandsPublicAndPreservesSensitivePlaceholders`
  - `DefaultActionSanitizerTest#testSanitizeSensitiveOnlyMasksSecretsWhileKeepingResolvedNonSensitiveValuesAndMetadata`
