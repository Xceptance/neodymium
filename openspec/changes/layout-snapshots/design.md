## Context

See proposal.md - Why. Currently, `isVisualStep()` in `PlaybookStep` conflates `(layout)` and `(visual)`, routing both to grayscale 128x128 luminance SSIM (`ScreenshotHasher.computeSsimMatrix`) with a strict >= 0.99 threshold. This works in static mock stores like Verla, but fails on dynamic real-world sites like Eminence Organics where catalog items, prices, and banners rotate.

## Goals / Non-Goals

**Goals:**
- Provide a fuzzy, content-neutral layout snapshot mechanism via `(layout)` that permits dynamic text and product photo variations.
- Preserve brand corporate identity colors (backgrounds, buttons, text colors) while neutralizing dynamic media.
- Maintain deterministic machine-speed replay (0 tokens, < 15ms execution) using 3-channel RGB color SSIM.
- Default to a similarity threshold of 0.92, with support for step-level overrides (e.g. `(layout: threshold=0.90)`).

**Non-Goals:**
- Replacing pixel-perfect visual regression (`(visual)` remains dedicated to strict >= 0.99 appearance checks).
- Ingesting arbitrary external PNGs from other viewports/browsers without running in the target browser arena.
- Extracting complex DOM tree graphs for NLP layout reasoning.

## Decisions

### 1. Browser-Side Color Wireframe via Transient CSS Injection
- **Decision:** Execute `window.__neodymiumApplyColorWireframe()` right before capturing the screenshot in both recording and replay modes, and immediately call `window.__neodymiumRemoveColorWireframe()` after capture.
- **Rationale:** By converting `img`, `video`, and `canvas` elements to solid `#E5E7EB` boxes and rendering text as `currentColor` bars in the browser, all dynamic content noise is neutralized before pixel capture occurs.
- **Alternatives Considered:**
  - *DOM Bounding Box Graph Diffing*: Complex tree-diffing algorithms that are sensitive to wrapper div changes and ignore colors entirely.
  - *Server-side Computer Vision Masking*: Expensive, requiring OpenCV or GPU models, adding latency to replay.

### 2. 3-Channel Color SSIM (128x128x3) in `ScreenshotHasher`
- **Decision:** Downsample to 128x128 in `BufferedImage.TYPE_INT_RGB` and compute composite color SSIM:
  `SSIM_color = (SSIM_R + SSIM_G + SSIM_B) / 3.0`.
- **Rationale:** Grayscale luminance has metamerism blindspots (e.g., brand olive and warm brown having identical luminance). 3-channel SSIM catches palette regressions while keeping payload size tiny (49 KB raw, ~65 KB Base64).
- **Alternatives Considered:**
  - *Grayscale SSIM*: Misses button color or background theme regressions.
  - *CIELAB Delta E*: More computationally expensive without significant layout advantage over 3-channel SSIM.

### 3. Dedicated `isLayoutStep()` and Parameterized Thresholds
- **Decision:** Separate `isLayoutStep()` from `isVisualStep()` in `PlaybookStep.java`. Default layout threshold to `0.92` (compared to `0.99` for visual steps). Support `(layout: threshold=0.90)` and `(layout: full)`.
- **Rationale:** Allows authors to set custom tolerances when a section has more volatile content, while providing a tested default that absorbs 1-2 lines of text wrap.

## Risks / Trade-offs

- **[Vertical ripple from multi-line text expansion]** → If text adds 2+ lines and pushes the entire page down, full-page SSIM drops.
  *Mitigation:* Use viewport-scoped `(layout)` for above-the-fold checks, or set `(layout: threshold=0.88)` for highly volatile dynamic pages.
- **[Seasonal marketing background shifts]** → If marketing temporarily changes the entire site background color (e.g. holiday theme), color SSIM will fail.
  *Mitigation:* Expected behavior; the test author runs `FORCE_RECORDING` to accept the new seasonal baseline.
