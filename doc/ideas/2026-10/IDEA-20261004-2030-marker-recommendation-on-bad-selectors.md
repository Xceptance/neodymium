# [IDEA-20261004-2030] Autonomous Visual Marker Recommendation on Fragile Selectors

- **Status:** `Implemented`
- **Proposed:** 2026-10-04 20:30
- **Resolved:** 2026-10-04 20:45
- **Component:** `neodymium-core` (`QualityJudgeToolInterceptor`, `InterceptionVerdict`, `LocatorImprover`, `AgentToolLoopStep`)
- **Category:** `AI & VLM`
- **Author:** AI-generated: Gemini 3.8 Flash & Xceptance GmbH 2026

## 1. Problem Statement & Motivation
In live LLM exploratory testing, when encountering modern web applications with textless icon buttons, anonymous container hierarchies, or obfuscated CSS-in-JS classes (e.g. `.css-1a2b3c`, `.emotion-rt8oam`), models struggle to find deterministic DOM locators. Instead of using visual markers, the model defaults to guessing brittle DOM selectors, complex nth-child paths, or thrashing with repeated failing `query_dom` calls.

The visual element marker feature (`mark_elements` / Set-of-Marks) solves this visual grounding problem, but frontier LLMs rarely discover and invoke `mark_elements` autonomously without explicit instruction-level prompting (`(marker)` hint). Currently, when an agent proposes a fragile or volatile locator that has no alternative DOM attributes in the DOM, `QualityJudgeToolInterceptor` and `LocatorImprover` silently allow the bad selector through because `candidates.isEmpty()`. If the guard were to reject it, it would trigger a fatal `AssertionError`, aborting the test.

## 2. Proposed Architecture & Design
1. **Non-Fatal Interception Feedback (`InterceptionVerdict.Decision.RETRY_WITH_FEEDBACK`):**
   Extend `InterceptionVerdict` with a `RETRY_WITH_FEEDBACK` decision. Unlike `REJECT` (which signifies fatal policy violations and aborts execution), `RETRY_WITH_FEEDBACK` short-circuits tool execution safely and returns an informative error result to the agent conversation loop with a diagnostic critique and a recommended recovery tool.

2. **Deterministic Quality Gate in `QualityJudgeToolInterceptor`:**
   Inspect interactive element calls. If a proposed selector has poor locator quality (`score < 4`, volatile ID, or deep position paths), and `LocatorImprover.generateCandidates()` cannot synthesize any stable alternative from DOM attributes (`candidates.isEmpty()` or candidates all score `< 0.4`), intercept the call with `RETRY_WITH_FEEDBACK`:
   - Critiques the fragile selector.
   - Recommends invoking `mark_elements` to visually ground the element via marker index.

3. **System Prompt Nudge in `AgentToolLoopStep`:**
   Under grounding rules, advise the model that when interactive elements lack distinctive text, IDs, or stable classes, it should invoke `mark_elements` to inspect and target elements visually via marker indices (`marker:N`).

## 3. Expected Impact & Trade-offs
- **Benefits:** Guides the LLM to autonomously "pull the marker card" without human test authors needing to annotate steps with `(marker)`. Runs in 0ms using deterministic Java heuristics in `LocatorImprover` without requiring the expensive LLM Judge.
- **Risks & Complexity:** Must avoid false-positive rejections of working CSS selectors (e.g., standard classes that happen to lack IDs). Setting the threshold strictly at `score < 4` with 0 synthesizable candidates ensures only genuinely ungroundable/fragile elements trigger the advisory.

## 4. Open Questions & Alternatives Considered
- *Alternative 1: LLM-as-a-Judge prompting.* Evaluated and rejected as primary mechanism because `judge.enabled` is `false` by default to avoid doubling token cost and adding 1–3s latency per action.
- *Alternative 2: Fatal guard rejection.* Evaluated and rejected because `AssertionError` aborts the test run rather than enabling model recovery.

## 5. Implementation Details & Verification
- **`InterceptionVerdict`:** Added `Decision.RETRY_WITH_FEEDBACK` and `retryWithFeedback(callId, reason, recommendation)` helper.
- **`AiConfiguration`:** Added `isJudgeRecommendMarkerEnabled()` (property: `neodymium.ai.judge.recommendMarker`, default: `true`).
- **`QualityJudgeToolInterceptor`:**
  - Exempts `mark_elements` and `unmark_elements` tools as well as `marker:*` and `coord:*` locators from CSS syntax judging.
  - Returns `RETRY_WITH_FEEDBACK` advising `mark_elements` when `score < 4` with 0 DOM matches.
  - In candidate evaluation (`evaluateCandidateScoring` and `deliberateCandidatesHeuristically`), returns `RETRY_WITH_FEEDBACK` if top candidate and all alternatives score `< 0.40`.
  - When LLM Judge rejects a proposed locator with no viable alternative, recommends `mark_elements`.
- **`AgentToolLoopStep`:**
  - Updated prompt Rule 2 to explicitly instruct the agent on when to use `mark_elements`.
  - In single-shot fast path, falls back to multi-turn loop when `RETRY_WITH_FEEDBACK` is returned.
  - In interactive tool loop, feeds `fbResult` to `conversation` as a soft error tool result without throwing `AssertionError`.
  - Updated `matchesTarget()` to allow `marker:*` and `coord:*` locators to satisfy previous action targets.
- **Test Verification:**
  - `QualityJudgeToolInterceptorTest`: 19 tests passing (added tests for fragile selectors, multiple low candidates, good alternative fallback, config toggle, marker/coord exemptions, mark_elements exemptions).
  - `AgentToolLoopStepTest`: `testRetryWithFeedbackInterceptorFeedsErrorResultWithoutThrowingAssertionError` verifying full 3-turn autonomous recovery (Turn 1 intercepted -> Turn 2 `mark_elements` -> Turn 3 `marker:1` click & complete).
  - `VisualMarkersSandboxMockTest`: End-to-end sandbox test `testVisualMarkersQualityJudgeInterceptionAndRecovery` against live embedded server and headless Chrome. Full suite (7 tests) passing.
