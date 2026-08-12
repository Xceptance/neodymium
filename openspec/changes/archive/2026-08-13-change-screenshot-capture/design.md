## Context

See `proposal.md` for motivation.

Neodymium AI defines context levels in `ContextLevel` (`HINT`, `MINIMAL`, `LEAN`, `STANDARD`, `RICH`, `VISUAL`, `VISUAL_LEAN`, `VISUAL_RICH`).

When elements are below the fold, visual steps need full-page screenshots. The strategy starts with standard viewport screenshots on initial `VISUAL` steps (tagged `(visual)`), automatically escalates to full-page screenshots upon visual context escalation (`VISUAL_LEAN`, `VISUAL_RICH`), and supports an explicit `(visual: full)` or `(visual:full)` tag for immediate full-page capture.

## Goals / Non-Goals

**Goals:**
- Implement `(visual: full)` / `(visual:full)` tag detection for immediate full-page screenshot capture while keeping `(visual` prefix compatible with PESAP visual detection.
- Automatically switch from viewport screenshot to full-page screenshot when visual context escalates (`VISUAL_LEAN`, `VISUAL_RICH`).
- Retain viewport boundary highlighting on full-page screenshots to clearly display active scroll state.

**Non-Goals:**
- Creating a separate context level enum constant (retains standard 7-tier `ContextLevel` enum).
- Changing non-visual context levels (`MINIMAL`, `LEAN`, `STANDARD`, `RICH`).
- Altering video/screencasting thread logic.

## Decisions

### 1. `ContextLevel` Enum and Escalation Rules
- **Decision:** Maintain 7-tier `ContextLevel` enum (`HINT` → `MINIMAL` → `LEAN` → `STANDARD` → `RICH` → `VISUAL` → `VISUAL_LEAN` → `VISUAL_RICH`).
- **Screenshot Scope Decision:**
  - `VISUAL` (initial, un-escalated): Viewport screenshot (`Capture.VIEWPORT`).
  - `VISUAL_LEAN`, `VISUAL_RICH` (escalated): Full-page screenshot (`Capture.FULL`).

### 2. Tag Detection in `PlaybookStep` & Instruction Preparation
- **Decision:**
  - `isVisualStep()` matches any instruction containing `(visual` or `(layout)`. This ensures PESAP recognizes both standard `(visual)` and explicit `(visual: full)` steps as visual mode.
  - `isFullPageVisualStep()` matches `(visual: full)`, `(visual:full)`, `(visual-full)`, and `(visual_full)`.
  - `ExecuteActionsStep.prepareInstruction` strips `(visual)` and `(visual: full)` / `(visual:full)` from the prompt before sending to LLM.

### 3. `PageAnalyzer` and `ScreenshotWriter` Integration
- **Decision:** `PageAnalyzer.captureScreenshot(title, level, isFullPage)` receives `isFullPage` flag.
- If `isFullPage == true` or `level` is an escalated visual level (`VISUAL_LEAN`, `VISUAL_RICH`), pass `forceFullPage = true` to `ScreenshotWriter.doScreenshot(...)`.

### 4. Viewport Highlighting
- **Decision:** Draw a distinct boundary overlay on full-page screenshots outlining the scroll offset and viewport dimensions so the AI model knows where the viewport was positioned.

## Risks / Trade-offs

- **[Full-page screenshot payload size]** → Full page images are larger than viewport images. *Mitigation:* Only trigger full-page capture upon visual escalation or explicit `(visual: full)` tag requests.
