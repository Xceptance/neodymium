# [DEF-20261105-1322] AI Usage Suite Evolution Playbook Inflation

- **Status:** `Resolved`
- **Opened:** 2026-11-05 13:22
- **Closed:** 2026-11-05 13:34
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the "AI Usage String of Pearls (Latest Approach)" widget (and other Latest Approach AI widgets), the milestone beads display 100% or the entire test suite volume (e.g., 118 out of 118 tests), flatlining across all runs and obscuring actual AI-driven or self-healing activity. Additionally, the beads were rendered in critical red (`#ef4444` / `#b91c1c`) due to score polarity re-evaluation.
- **Root Cause:** `BatchSuiteEvolutionPointDto.getTotalAiUsageCount()` lumped deterministic YAML `playbookCount` into total AI usage (`getTotalAiCount() + getTotalHealedCount() + playbookCount`). Furthermore, `WidgetAggregationService` inconsistently excluded playbooks in `BY_RUNS` and `DAILY` modes while including them in `LATEST_APPROACH`, and `resolvePearlColors()` unconditionally overwrote the designated AI purple (`#8b5cf6`) with a status pass/fail health spectrum.
- **Detection Gap ("What did we miss?"):** Previous unit tests (`WidgetAggregationServiceTest.testAiUsagePerlenketteAggregation`) verified that the DTO structure returned non-null beads and contained map keys, but used synthetic mock fixtures where `playbookCount` was small without asserting that deterministic replay tests must not inflate the AI usage total or rate.
- **Resolution:**
  1. Redefined `BatchSuiteEvolutionPointDto.getTotalAiUsageCount()` to strictly sum `getTotalAiCount() + getTotalHealedCount()`, excluding deterministic `playbookCount`.
  2. Updated `BatchSuiteEvolutionPointDto.getClassicCount()` to `Math.max(0, totalTestsKnown - getTotalAiCount() - getTotalHealedCount() - playbookCount)` so pure Java tests remain distinct from playbooks.
  3. Aligned `WidgetAggregationService.AiUsagePointBreakdown.totalAi()` to `aiDriven + healed`.
  4. Scoped `resolveContributingExecutions` for `WidgetMetric.AI_USAGE` strictly to `exec.isAiDriven() || exec.isHealed()`.
  5. Preserved designated AI and Cost themes for pearls while introducing dynamic color scaling:
     - For **Costs**: implemented the 5-tier Budget Alert Spectrum (Green $\le \$0.01$, Blue $<\$0.05$, Yellow/Amber $<\$0.15$, Orange $<\$0.50$, Red $\ge \$0.50$) to visually highlight cost spikes and budget alerts.
     - For **AI Usage**: implemented the 5-tier Thematic Purple Intensity Gradient ($\ge 50\%$ Royal dark violet, $\ge 35\%$ Deep purple, $\ge 20\%$ Vivid purple, $> 0\%$ Soft lavender, $0\%$ Pale lavender) to clearly convey AI adoption rates across milestones.
- **Safety Net Added:** Dedicated test cases in:
  - `BatchSuiteEvolutionDtoTest.testAiUsageStrictExcludesPlaybookReplays`
  - `WidgetAggregationServiceTest.testAiUsagePerlenketteAggregation`
  - `WidgetAggregationServiceTest.testPerlenketteCostsBudgetAlertSpectrum`
  - `WidgetAggregationServiceTest.testPerlenketteAiUsagePurpleIntensityGradient`

