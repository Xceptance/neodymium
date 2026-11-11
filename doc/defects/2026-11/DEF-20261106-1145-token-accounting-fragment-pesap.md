# [DEF-20261106-1145] Aura Manager Token Accounting Omits Fragment Sub-Steps and Fabricates Phantom PESAP

- **Status:** `Resolved`
- **Opened:** 2026-11-06 11:45
- **Closed:** 2026-11-06 12:35
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager's test execution side panel and run report, token accounting and LLM calls were severely underreported during tests with fragment sub-steps (e.g. reporting ~301k input tokens when over 509k were consumed), while the LLM responsibility table displayed a fabricated "PESAP (Pre-Execution Semantic Anchor)" category with fabricated metrics (e.g. 50% calls / 8% tokens). Additionally, when a test was aborted by `TokenBudgetGuard`, the breaching turn was dropped from disk reports, and `RunReportDto#getTotalLlmTokens` collapsed to zero if token metrics were stored only in nested `metrics`.
- **Root Cause:**
  1. `report-manager.js#syncRowAiUsage()` only traversed top-level steps in `blocksObj.before`, `steps`, and `after`, completely omitting nested `subSteps` executed by included fragments.
  2. Pre-flight linter calls (`stepIndex == -1`) stored in root `llmCalls` were omitted when aggregating step calls.
  3. `report-manager.js#parseResponsibilityBuckets()` strictly required `'pesap'` in the parsed responsibility JSON. Because Neodymium backend removed PESAP in `DEF-20260916-05`, this validation always returned `null`, forcing the UI into `renderHeuristicLlmRows()` which fabricated 50% calls and 8% tokens as PESAP.
  4. `TestExecutionDto` and `LocalRunJsonStorageService` lacked granular bindings for `inputTokensCount`, `outputTokensCount`, and `cachedTokensCount`, falling back to hardcoded 85%/15% multiplier heuristics rather than reading from `metrics.tokenUsageInput` and `metrics.tokenUsageOutput`.
  5. `AiSession.java` registered `TokenBudgetGuard` before `PreliminaryReportListener`. When a budget breach occurred, the guard threw `TokenBudgetExceededException` synchronously, halting event dispatch before the preliminary report listener could record the breaching turn and its token consumption.
  6. `TestExecutionDto#getLlmTotalTokens()` and `isAiDriven()` relied strictly on flat fields rather than delegating to fallbacks when flat fields were unpopulated.
- **Detection Gap ("What did we miss?"):** Previous unit tests in `TestExecutionDtoTest` tested basic deserialization without `metrics` token mappings, and `StaticResourceSyntaxTest` lacked tests evaluating recursive fragment `subSteps` traversal and modern 5-category responsibility JSON parsing without PESAP. Furthermore, no test asserted that passive reporting listeners precede circuit breakers in event registration.
- **Resolution:**
  1. Updated `report-manager.js#syncRowAiUsage()` to recursively aggregate LLM calls from all nested `subSteps` and combine them with root `blocksObj.llmCalls` (including pre-flight `stepIndex: -1` calls), deduplicating by unique call fingerprint.
  2. Updated `report-manager.js#parseResponsibilityBuckets()` to validate and normalize modern categories without requiring `'pesap'`, and removed the PESAP row from `renderLlmResponsibilityRows()` and `renderHeuristicLlmRows()`.
  3. Cleaned up stale `'pre-step pesap'` filter token in `dashboard-runner.js` and CSS comments in `dashboard-styles.css`.
  4. Enhanced `TestExecutionDto` and `LocalRunJsonStorageService` to deserialize and persist `inputTokensCount`, `outputTokensCount`, `cachedTokensCount`, `llmCalls`, and extract `llmResponsibilityJson` dynamically from `metrics.categories` or `metrics`.
  5. Updated `TestExecutionDto#getLlmTotalTokens()` to delegate to `getTotalTokensCount()`, `getLlmCost()` to inspect `metrics.estimatedCostUsd`, and `isAiDriven()` to check calls and tokens.
  6. Registered `PreliminaryReportListener` before `TokenBudgetGuard` in `AiSession.java` so breaching turns are captured by telemetry before execution aborts.
  7. Updated `run-report.html` to output `th:data-input-tokens`, `th:data-output-tokens`, and `th:data-cached-tokens` on execution rows.
- **Safety Net Added:**
  - Automated tests in `TestExecutionDtoTest`: `testTokenUsageAndResponsibilityDeserializationFromMetrics`, `testTokenUsageDirectFieldsAndFallbacks`, and `testCostAndTokensFallbacksFromMetricsAndLlmCalls`.
  - Node-based regression tests in `StaticResourceSyntaxTest`: `testResponsibilityTableOmitsPesapAndParsesModernBuckets` and `testSyncRowAiUsageRecursivelyTraversesSubStepsAndPreFlightCalls`.
  - Automated test in `AiSessionTest`: `testEventListenerOrderPreliminaryReportBeforeTokenBudgetGuard`.
