## Why

Currently, screenshots captured during automated test execution and AI visual context gathering default to the visible browser viewport size. This prevents visual discovery of web elements located below the fold without scrolling, leaving the AI model or tester without context on where or how far to scroll. 

We need a flexible mechanism where initial visual steps take standard viewport screenshots, but switch to full-page screenshots upon visual context escalation, while also providing an explicit `(visual: full)` or `(visual:full)` tag to immediately capture full-page screenshots.

## What Changes

- Introduce full-page screenshot capture integration into `ScreenshotWriter` and AI `PageAnalyzer`.
- Support escalation-based screenshot mode: standard `VISUAL` level captures standard viewport screenshots, but upon visual context escalation (to `VISUAL_LEAN` or `VISUAL_RICH`), screenshot capture always switches to full-page mode.
- Introduce explicit `(visual: full)` / `(visual:full)` tag support (starting with `(visual` so PESAP recognizes visual mode) that immediately captures full-page screenshots on the initial step attempt without requiring prior escalation.
- Support optional visual viewport highlighting overlay on full-page screenshots to clearly indicate which portion of the page is currently scrolled into view.

## Capabilities

### New Capabilities
- `full-page-screenshot-capture`: Enables configurable and level-driven full-page screenshot capturing (including escalation triggers and explicit `(visual: full)` tag support) with optional viewport highlighting for visual AI analysis and report attachments.

### Modified Capabilities
*(None)*

## Impact

- `org.neodymium.ai.executor.selenide.ContextLevel`: Maintained 7-tier enum ladder and set `isFullPageScreenshot()` to true for escalated visual levels (`VISUAL_LEAN`, `VISUAL_RICH`).
- `org.neodymium.ai.model.PlaybookStep`: `isVisualStep()` matches `(visual`, `isFullPageVisualStep()` matches `(visual: full)` and `(visual:full)`.
- `org.neodymium.common.ScreenshotWriter`: Support full-page vs viewport selection based on context level, escalation state, and explicit step tags.
- `org.neodymium.ai.executor.selenide.PageAnalyzer`: Enhancement to pass context level, escalation status, and explicit `isFullPage` flag to screenshot capture routines.
- `org.neodymium.util.NeodymiumConfiguration` & `config/neodymium.properties`: Property configurations controlling default screenshot capture scope and viewport highlight overlays.
