# [DEF-20261125-1507] WidgetAggregationServiceTest AI Usage Trend Fixture Violates TestRunEntity.passedCount Invariant

- **Status:** `Resolved`
- **Opened:** 2026-11-25 15:07
- **Closed:** 2026-11-25 15:07
- **Component:** `aura-manager` (`WidgetAggregationServiceTest`)
- **Scope:** `Test/Harness`
- **Issue:** GitLab #175
- **Symptom:**
  `WidgetAggregationServiceTest#testAiUsageTrendGraphDecouplesCategoryCurves` failed on line 4084 with `AI-Driven curve must remain completely flat despite 50-test drop in Classic test volume ==> expected: <142.5> but was: <157.5>`.
- **Root Cause:**
  In `TestRunEntity`, `passedCount` represents the total passed count (`passedClean + passedHealed + passedAi`), not the Classic/clean-only count. The fixture in `WidgetAggregationServiceTest#testAiUsageTrendGraphDecouplesCategoryCurves` set `run1.setPassedCount(70)` and `run2.setPassedCount(20)` alongside `passedHealedCount(15)` and `passedAiCount(15)`, leaving `run2` with `15 + 15 = 30 > 20`. After PR #579 (`9b7fc3eca`) introduced `TestRunEntity.clampAiCount()` to guard against legacy double-counted rows where `healed + ai > categoryTotal`, `run2.getPassedAiCountSafe()` clamped `15` down to `5`, shifting `yPoint2` from `142.5` to `157.5`.
- **Detection Gap ("What did we miss?"):**
  PR #579 (`9b7fc3eca`) ran targeted `TestRunEntityTest` and `TestExecutionDtoTest` checks rather than `WidgetAggregationServiceTest`, leaving the invalid `TestRunEntity` test fixture undetected until a full suite run.
- **Resolution:**
  Corrected `run1.setPassedCount(100)` (`70 Classic clean + 15 Healed + 15 AI-Driven`) and `run2.setPassedCount(50)` (`20 Classic clean + 15 Healed + 15 AI-Driven`) in `WidgetAggregationServiceTest#testAiUsageTrendGraphDecouplesCategoryCurves` so the fixture satisfies the `TestRunEntity` invariant.
- **Safety Net Added:**
  - Added explicit `rawValuesCsv` assertions for the `Classic` (`"70,20"`), `Healed` (`"15,15"`), and `AI-Driven` (`"15,15"`) trend layers in `WidgetAggregationServiceTest#testAiUsageTrendGraphDecouplesCategoryCurves` to catch unexpected count drift or clamping immediately.
