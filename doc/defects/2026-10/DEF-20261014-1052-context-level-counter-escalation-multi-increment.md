# [DEF-20261014-1052] Context Level Counter Multi-Increments on Escalated Steps

- **Status:** `Resolved`
- **Opened:** 2026-10-14 10:45
- **Closed:** 2026-10-14 10:53
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In execution reports, metrics summaries, and side-panel test inspector drawers, the context level counters (e.g. MINIMAL, LEAN, STANDARD, VISUAL) incremented every intermediate context level traversed during a step's escalation instead of only the step's final effective invocation level. For instance, if Step 1 ran at LEAN and Step 2 escalated from LEAN to STANDARD, the counters displayed `LEAN: 2, STANDARD: 1`, totaling 3 level invocations across only 2 executed steps.
- **Root Cause:** Both `PreliminaryReportListener.aggregateStepStats` and `AiSession.aggregateStepStats` iterated across all elements in `stats.getContextLevels()` via `for (final String lvl : levels) { contextLevelCounts.put(normalized, ... + 1); }`. Whenever a step escalated through multiple levels (such as LEAN to STANDARD), every intermediate level was incremented. In `report-manager.js`, `renderContextLevelBadges` defaulted to the backend's precalculated `metrics.contextLevelCounts` JSON, masking the front-end's fallback calculation, while `extractStepContextLevelCounts` only supported unicode `→` separators and omitted try dictionary structures.
- **Detection Gap ("What did we miss?"):** No automated unit test asserted that `contextLevelCounts` preserves a strict 1:1 mapping between executed steps and their final invocation level under escalation conditions, allowing multi-increment accumulation to go undetected.
- **Resolution:**
  1. Updated `aggregateStepStats` in `PreliminaryReportListener.java` and `AiSession.java` to determine the final non-blank level in `stats.getContextLevels()` and increment only that final level in `contextLevelCounts`, while preserving correct `totalEscalations` count.
  2. Updated `extractStepContextLevelCounts` in `report-manager.js` to split on regex `/\s*(?:→|->)\s*/`, support try objects (`{ beforeSteps, coreSteps, afterSteps }`), and recursively process `subSteps`.
  3. Updated `renderContextLevelBadges` in `report-manager.js` to prioritize extracting counts from the active row's step cards when available so historical test runs stored on disk immediately display the corrected counts.
- **Safety Net Added:** Added automated unit tests `testAggregateStepStatsIncrementsOnlyFinalContextLevelOnEscalation`, `testAggregateStepStatsWithSubSteps`, and `testPreliminaryReportListenerRecordsOnlyFinalContextLevelsInReportMetrics` in `PreliminaryReportListenerTest.java`, as well as `testAiSessionAggregateStepStatsIncrementsOnlyFinalContextLevel` in `AiSessionTest.java`. Verified JavaScript syntax using `StaticResourceSyntaxTest`.
