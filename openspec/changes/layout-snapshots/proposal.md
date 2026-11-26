## Why

Automated visual testing (`(visual)`) currently enforces pixel-level SSIM matching (>= 0.99) against recorded screenshots. On dynamic websites (such as real-world e-commerce catalogs with rotating products, changing prices, and promotional banners), this causes brittle replay failures, forcing test authors to write dozens of tedious element-level micro-assertions instead. We need a fuzzy structural snapshot mechanism (`(layout)`) that captures the page layout and brand color scheme during recording and verifies during replay that the live page remains "almost like that one" (>= 0.92 color SSIM), absorbing dynamic content changes while catching structural and design regressions at machine speed with zero token overhead.

## What Changes

- **Color Wireframe Engine**: Injects a lightweight browser-side transform during layout capture that neutralizes dynamic noise (`<img>`, `<video>`, variable text strings) into uniform colored skeleton bars while preserving container geometries, borders, and brand design system colors (`background-color`, button colors, accent themes).
- **3-Channel Color SSIM**: Extends `ScreenshotHasher` with RGB progressive downsampling (`128x128x3` = 49 KB) and composite color SSIM calculation (`(SSIM_R + SSIM_G + SSIM_B) / 3.0`).
- **Dedicated `(layout)` Directive**: Separates `(layout)` from `(visual)` in `PlaybookStep`, supporting parameterized thresholds (e.g. `(layout: threshold=0.90)`) and full-page wireframes (`(layout: full)`), defaulting to threshold `0.92`.
- **Automated Replay Gating**: Updates `VisualBaselineGateStep` and `VerifyOutcomeStep` to capture and evaluate color wireframe matrices during recording and replay, bypassing LLM invocations when layout similarity meets the threshold.

## Capabilities

### New Capabilities
- `layout-snapshots`: Introduces fuzzy layout snapshot recording and deterministic replay verification using color wireframe transformation and 3-channel color SSIM.

### Modified Capabilities
<!-- None -->

## Impact

- `neodymium-core`:
  - `org.neodymium.ai.util.ScreenshotHasher`: Added RGB progressive downsampling and 3-channel color SSIM calculation.
  - `org.neodymium.ai.model.PlaybookStep`: Dedicated `isLayoutStep()`, `layoutStep` flag, and layout threshold resolution.
  - `org.neodymium.ai.pipeline.steps.VerifyOutcomeStep`: Injects color wireframe prior to screenshot capture on recording layout steps.
  - `org.neodymium.ai.pipeline.steps.VisualBaselineGateStep`: Evaluates live color wireframe against recorded baseline with `minScore` default 0.92.
  - `src/main/resources/ai-scripts/neodymium-color-wireframe.js`: Browser helper for transient color wireframe application and removal.
- Test suites: Enables single-line layout assertions (e.g. `Verify cart layout (layout)`) to replace dozens of DOM presence checks.
