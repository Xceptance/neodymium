# Tasks: Interactive Quality Judge with Live SUT Probing

## Phase 1: Probing Abstractions & DTOs
- [ ] Create `org.neodymium.ai.executor.probe.LocatorProbeResult` DTO <!-- id: 1.1 -->
- [ ] Create `org.neodymium.ai.executor.probe.ProbeElementSummary` DTO (normalized for Selenide, Playwright, Appium, Mock) <!-- id: 1.2 -->
- [ ] Create `org.neodymium.ai.executor.probe.ProbeBoundingRect` DTO (`x`, `y`, `width`, `height`) <!-- id: 1.3 -->
- [ ] Add default `supportsLocatorProbing()` and `probeLocators(...)` methods to `TargetExecutor` <!-- id: 1.4 -->
- [ ] Add canned probe result support to `MockTargetExecutor` for headless unit testing <!-- id: 1.5 -->
- [ ] Write unit tests for probing abstractions in `MockTargetExecutorProbeTest` <!-- id: 1.6 -->

## Phase 2: Driver Probing Implementations (Selenide & Playwright Architecture)
- [ ] Implement `SelenideLocatorProber` to query live WebDriver/Selenide elements safely without throwing unhandled exceptions <!-- id: 2.1 -->
- [ ] Map driver-specific geometry (`WebElement.getRect()` vs Playwright `Locator.boundingBox()`) cleanly to `ProbeBoundingRect` <!-- id: 2.2 -->
- [ ] Implement `probeLocators` in `SelenideTargetExecutor` <!-- id: 2.3 -->
- [ ] Implement locator composite quality scoring logic in `SelenideLocatorProber` <!-- id: 2.4 -->
- [ ] Write unit and integration tests in `SelenideLocatorProberTest` <!-- id: 2.5 -->

## Phase 3: Cumulative Discussion Prompt & Protocol
- [ ] Create discussion prompt template `src/main/resources/ai-prompts/quality-judge-discussion-prompt.md` <!-- id: 3.1 -->
- [ ] Update `QualityJudgePrompt` to support compiling cumulative multi-turn discussion prompts with probe telemetry <!-- id: 3.2 -->
- [ ] Update `QualityJudgePrompt` response parser to handle `status` (`APPROVED`, `REFINED`, `NEED_REFINEMENT`) and `refinedProposal` <!-- id: 3.3 -->
- [ ] Write unit tests in `QualityJudgePromptTest` verifying prompt compilation and response parsing <!-- id: 3.4 -->

## Phase 4: Deliberative Pipeline Step & Observability
- [ ] Implement multi-turn deliberation loop in `QualityJudgeStep` <!-- id: 4.1 -->
- [ ] Support all judge execution modes: `DISCUSSION` (default when enabled), `ON_AMBIGUITY`, `ON_FAIL`, `ALWAYS` <!-- id: 4.2 -->
- [ ] Implement fast-path evaluation bypass for score 10 unique matches <!-- id: 4.3 -->
- [ ] Add configuration properties to `AiConfiguration` (`neodymium.ai.judge.mode=DISCUSSION`, `neodymium.ai.judge.discussion.maxTurns`, etc.) <!-- id: 4.4 -->
- [ ] Implement structured turn-by-turn console logging and EventBus dispatching <!-- id: 4.5 -->
- [ ] Track accumulated token metrics in `ExecutionContext.KEY_JUDGE_TOKEN_USAGE` and verify formatting in `StateMachineRunner` summary box <!-- id: 4.6 -->
- [ ] Write unit and integration tests in `QualityJudgeStepDiscussionTest` <!-- id: 4.7 -->

## Phase 5: Verification & End-to-End Validation
- [ ] Run full unit and integration test suite (`mvn test`) <!-- id: 5.1 -->
- [ ] Implement `VerlaJudgeModesIntegrationTest` to validate all 4 judge modes against:
  - **Best Storefront (`/verla-perfect/`)**: Verifies Fast-Path auto-approval on clean unique IDs (0 extra LLM calls). <!-- id: 5.2 -->
  - **Worst / Stress Storefronts (`/verla-pwa-chaos/` and `/verla-bad/`)**: Verifies multi-turn deliberation, ambiguous class refinement, and volatile ID rejection. <!-- id: 5.3 -->
- [ ] Validate live store execution on `VerlaGuestCheckout_CaFr_French` and `VerlaAutoTranslateCheckoutIntegrationTest` <!-- id: 5.4 -->
