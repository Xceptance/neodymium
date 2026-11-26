## 1. ContextLevel Enum & Escalation Logic (TDD)
# Tasks

- [x] Integrate full-page screenshot capability into `ScreenshotWriter.doScreenshot` with viewport boundary highlighting overlays <!-- id: 0 -->
- [x] Maintain 7-tier `ContextLevel` enum and set `isFullPageScreenshot()` to true for escalated visual levels (`VISUAL_LEAN`, `VISUAL_RICH`) <!-- id: 1 -->
- [x] Update `PlaybookStep` (`isVisualStep()`, `isFullPageVisualStep()`) and `ExecuteActionsStep.prepareInstruction` to support `(visual: full)` and `(visual:full)` tags <!-- id: 2 -->
- [x] Update `PageAnalyzer.captureScreenshot` to handle `isFullPage` parameter for explicit tags and visual escalations <!-- id: 3 -->
- [x] Add unit tests in `ContextLevelTest`, `PlaybookStepTest`, and `PageAnalyzerTest` verifying visual escalations and `(visual: full)` tag handling <!-- id: 4 -->
- [x] Update `AI-README.md` documentation for `(visual)` vs `(visual: full)` tags and 7-tier context level escalation ladder <!-- id: 5 -->
