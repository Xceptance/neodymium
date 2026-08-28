## 1. Domain Model & Escalation Ladder Refactoring

- [x] 1.1 Move `ContextLevel` from `org.neodymium.ai.executor.selenide` to `org.neodymium.ai.model`.
- [x] 1.2 Redefine the escalation path in `ContextLevel.escalate()` to separate visual checks (`VISUAL`) from interactive fallbacks (`STANDARD` -> `RICH` -> `VISUAL_LEAN` -> `VISUAL_RICH`).
- [x] 1.3 Update `TargetExecutor`, `AiSession`, and `ExecutionContext` imports to use the new `ContextLevel` package location.
- [x] 1.4 **[Review & Audit]** Verify zero compile breakage, inspect `git diff`, and confirm non-AI runner classes remain 100% untouched.

## 2. Feature Proximity Math & LocatorCascadeResolver Engine (TDD)

- [x] 2.1 Write isolated unit tests (`LocatorCascadeResolverTest`) covering Jaccard similarity, Levenshtein distance, and Semantic Tag Equivalence Bucketing.
- [x] 2.2 Implement `LocatorCascadeResolver` and the DOM Feature Vector scoring engine in pure Java.
- [x] 2.3 **[Review & Audit]** Bluntly analyze similarity edge cases (duplicate buttons, empty text, tie-breakers) and benchmark CPU execution time (< 1ms).

## 3. Unified Perception Model (UPM) in PageAnalyzer

- [x] 3.1 Refactor `PageAnalyzer.CAPTURE_SCRIPT` to extract computed Accessible Name and Role alongside structural tags.
- [x] 3.2 Add DOM Feature Vector fields (tag, text, classes, attributes, sibling index) to the JS extraction output.
- [x] 3.3 Calculate anchor-relative bounding boxes for Canvas and SVG elements inside the extraction script.
- [x] 3.4 **[Review & Audit]** Verify shadow DOM and iframe traversal still function seamlessly without regressions.

## 4. Companion JSON, Backward Compatibility & Framework Lock

- [x] 4.1 Update the `Action` data model to store a 5-tier candidate locator list alongside the structured DOM Feature Vector.
- [x] 4.2 Modify `YamlPlaybookParser` and `PlaybookRecorder` to support both the new dual locator schema AND legacy single-target `.json` files (backward compatibility).
- [x] 4.3 Add `targetFramework` injection into companion JSON metadata during `FORCE_RECORDING`.
- [x] 4.4 Implement `IncompatibleFrameworkException` validation check in `StateMachineRunner` during initialization.
- [x] 4.5 **[Review & Audit]** Test loading both legacy and new companion files to ensure no existing tests are broken.

## 5. Visual Form Input Handling & SSIM Gating

- [x] 5.1 Implement coordinate-based focus clicks for visual/canvas elements prior to typing.
- [x] 5.2 Update `TypeAction` to dispatch raw keyboard events via WebDriver `Actions` when interacting with visual-only form fields.
- [x] 5.3 Integrate the existing 64x64 SSIM tile micro-crop engine as a safety gate for coordinate clicks ($\ge 0.95$ threshold).
- [x] 5.4 **[Review & Audit]** Verify coordinate safety against layout shifts and ensure standard native input fields continue using native `sendKeys()`.

## 6. Prompt Standardization & Quality Judge Elevation

- [x] 6.1 Refactor `QualityJudgePrompt` to implement `AiPrompt<QualityJudgeResult>` and load system instructions from classpath markdown.
- [x] 6.2 Expand the Quality Judge evaluation rubric to grade the entire 5-tier candidate bundle during live recording.
- [x] 6.3 Externalize hardcoded Java strings in `VerificationPrompt` and `VisualRcaPrompt` into markdown files under `src/main/resources/ai-prompts/`.
- [x] 6.4 **[Review & Audit]** Verify prompt template compilation and token consumption across all prompts.

## 7. Pipeline De-coupling & End-to-End Validation

- [x] 7.1 Extract PESAP pre-step execution from `ExecuteActionsStep` into a standalone `PesapPreStep` class.
- [x] 7.2 Extract visual baseline SSIM logic into `VisualBaselineGateStep`.
- [x] 7.3 Wire the full cascade into the pipeline and execute `VerlaGuestCheckout_Us_English` across all 8 variants.
- [x] 7.4 **[Final Audit & Regression Check]** Run complete test suite and inspect final `git diff` for zero collateral damage.
