## 1. Color Wireframe JavaScript Engine

- [x] 1.1 Create `neodymium-color-wireframe.js` in `src/main/resources/ai-scripts/` with `applyColorWireframe()` and `removeColorWireframe()`, verifying syntax and style rules
- [x] 1.2 Add helper methods to `PageAnalyzer` or browser executor to inject and remove the wireframe stylesheet, verifying execution on live browser pages

## 2. 3-Channel Color SSIM in `ScreenshotHasher`

- [x] 2.1 Implement `downsampleProgressiveColor` in `ScreenshotHasher.java` preserving `BufferedImage.TYPE_INT_RGB`
- [x] 2.2 Implement `computeColorSsimMatrix` and `calculateColorSsim` in `ScreenshotHasher.java`, verifying with unit tests in `ScreenshotHasherTest.java` that identical wireframes score >= 0.98, color shifts score < 0.85, and structural breaks score < 0.80

## 3. Step Parsing & Layout Directive in `PlaybookStep`

- [x] 3.1 Update `PlaybookStep.java` to separate `isLayoutStep()` from `isVisualStep()`, parse parameterized thresholds (e.g. `(layout: threshold=0.90)`, `(layout: 90%)`), and default layout threshold to 0.92
- [x] 3.2 Add unit tests in `PlaybookStepTest.java` verifying `isLayoutStep()`, threshold parsing, and full-page layout detection

## 4. Pipeline Recording & Replay Integration

- [x] 4.1 Update `VerifyOutcomeStep.java` to apply color wireframe before capture on recording layout steps, computing and saving the color SSIM matrix
- [x] 4.2 Update `VisualBaselineGateStep.java` to apply color wireframe during replay, evaluate live color SSIM against baseline with default threshold 0.92, and bypass LLM calls on pass
- [x] 4.3 Add unit tests in `VisualBaselineGateStepTest.java` verifying replay pass on content changes and replay failure on structural collapse

## 5. Verification & Clean Code Audit

- [x] 5.1 Run full suite of visual and step tests (`ScreenshotHasherTest`, `PlaybookStepTest`, `VisualBaselineGateStepTest`) to verify zero regressions
- [x] 5.2 Perform clean code audit: zero unused imports, zero unused variables, pragmatic `final` modifiers, and no inline FQCNs
