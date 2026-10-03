# [DEF-20260918-05] Visual SSIM Threshold Step Mutation, Missing Parameterized Tag Overrides, and Config Alias Shadowing

- **Status:** Resolved
- **Opened:** 2026-09-18
- **Closed:** 2026-09-18
- **Component:** `neodymium-core` (`ai-model`, `ai-pipeline`, `ai-config`, `ai-junit`)
- **Scope:** `Framework`
- **Symptom:** Inability to override visual assertion SSIM thresholds per step (in YAML playbooks) or per test case via `@AiVisual`. In addition, replay gate execution unconditionally mutated `PlaybookStep.ssimMinScore` from `null` to `0.99`, polluting JSON companion recordings with unintended `ssimMinScore` fields, while static file defaults in `ai.properties` shadowed alias overrides in system properties and thread-local data.
- **Root Cause:**
  1. `VisualBaselineGateStep.execute` and `executePostActionCheck` unconditionally invoked `this.step.setSsimMinScore(minScore)` with the global config score `0.99`. This changed `ssimMinScore` from `null` to `0.99`, causing Jackson (`@JsonInclude(NON_NULL)`) to write `ssimMinScore` into JSON companion files on replay finish, breaking the contract that `(visual)` must use global configuration and not be hardcoded into JSON.
  2. `PlaybookStep` lacked support for comma-separated parameters in visual tags (e.g. `(visual: threshold=0.98)` or `(visual: full,threshold=0.98)`).
  3. `AiConfiguration.getProperty` checked the property file for the primary key (`neodymium.ai.ssim.minScore`) before evaluating fallback aliases, so the default `neodymium.ai.ssim.minScore = 0.99` in `ai.properties` masked dynamic or system property overrides on aliases like `neodymium.ai.visual.threshold`.
- **Detection Gap ("What did we miss?"):** Existing gate tests in `VisualBaselineGateStepTest` only asserted visual matching/divergence behavior, never verifying that `step.getSsimMinScore()` remained `null` for steps tagged with `(visual)`. Configuration tests verified individual properties but did not test alias override precedence against properties file defaults.
- **Resolution:**
  1. Created `@AiVisual` annotation supporting `value()` and `threshold()` attributes for test class and method level SSIM overrides in `NeodymiumAiRunner`.
  2. Updated `PlaybookStep` to support parameterized visual tags: `(visual: threshold=0.98)`, `(visual: full,threshold=0.98)`, percentage formats (`98%`), and `@JsonProperty("threshold")` / `@JsonAlias("threshold")` aliases.
  3. Updated `VisualBaselineGateStep` to evaluate `step.getSsimMinScore()` when present and avoid mutating `step.ssimMinScore` if it was initially `null`.
  4. Updated `AiConfiguration.getVisualSsimMinScore()` to enforce proper multi-tier precedence: thread-local data (`Neodymium.getData()`) -> system properties -> properties files -> default fallback `0.99`.
  5. Updated `PostFlightPlaybookLinter` and `PreliminaryReportListener` to support parameterized visual tags and report accurate thresholds.
- **Safety Net Added:** Added unit regression tests in `PlaybookStepTest` (`testVisualTagVariantsAndThresholdParsing`, `testVisualSerializationExclusionWhenNull`, `testThresholdDeserializationJsonAlias`), `VisualBaselineGateStepTest` (`testReplayWithCustomStepThreshold_passesBelowDefaultThreshold`), `AiConfigurationTest` (`testVisualSsimMinScoreDefaultsAndAliases`), and `NeodymiumAiRunnerTest` (`testAiVisualAnnotationHandling`).
