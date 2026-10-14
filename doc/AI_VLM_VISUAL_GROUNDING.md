# Direct VLM Visual Coordinate Grounding & Spatial Anchor Pinning

## 1. Executive Summary & Core Philosophy

Traditional web test automation tools (Selenium, Cypress, Playwright) operate under the assumption that all interactive interface elements exist as distinct, queryable nodes in the Document Object Model (DOM).

In modern web development, this assumption frequently fails:
- **Canvas 2D / WebGL / WebGPU**: Complex data visualizations, interactive charts (Chart.js, ECharts, D3), mapping engines, PDF viewers, digital signature pads, document markup tools, and gaming interfaces render graphics directly into a single `<canvas>` element with **zero interactive child DOM nodes**.
- **Flattened / Icon SVG Graphs**: Dynamic graphics rendered as raw SVG `<path>` elements without accessible names, titles, or stable locators.
- **Visual-Only State Transitions**: Elements whose state changes are purely chromatic (e.g., green vs. red status indicators, visual badge movements) without associated DOM attribute or text mutations.
- **Shadow DOM & Closed Component Boundaries**: Web Components that encapsulate subtrees preventing external CSS/XPath traversal.

A human tester does not inspect DOM hierarchies; **a human looks at pixels and interacts with visual affordances**. Neodymium's Direct Vision-Language Model (VLM) Visual Grounding architecture gives the test framework *eyes* to perceive, locate, and interact with graphical UI components, while preserving the speed, determinism, and zero-token efficiency of headless browser replay.

---

## 2. The Hybrid Dual-Engine Architecture

Rather than relying entirely on slow, non-deterministic OS-level pixel-based agents (e.g., pure computer-use models that require live LLM inference on every single action), Neodymium employs a **Hybrid Dual-Engine**:

```
                  ┌─────────────────────────────────────────────────────────┐
                  │                 RECORDING / DISCOVERY                   │
                  │                                                         │
                  │  1. Multimodal Perception (DOM + Visual Screenshot)     │
                  │  2. VLM Visual Coordinate Grounding (Centroids/Points)   │
                  │  3. Spatial-to-DOM Anchor Pinning (elementFromPoint)    │
                  └────────────────────────────┬────────────────────────────┘
                                               │
                                               ▼ Serializes to Playbook:
                                    "coord: #canvas-stage@300,75"
                                               │
                  ┌────────────────────────────┴────────────────────────────┐
                  │                   DETERMINISTIC REPLAY                  │
                  │                                                         │
                  │  1. Zero LLM Tokens (Sub-millisecond execution)         │
                  │  2. Selenide/DOM Resolves Container (#canvas-stage)      │
                  │  3. Actions.moveToElement(anchor, dx, dy).click()       │
                  │  4. DOM Vector Self-Healing if container refactors      │
                  └─────────────────────────────────────────────────────────┘
```

---

## 3. End-to-End Pipeline & Lifecycle

### Phase 1: Visual Target Perception & Ingestion
When an instruction targets an un-inspectable graphical control (e.g., *"Click the blue confirm button in the canvas"*), Neodymium captures a viewport screenshot and queries the VLM at `ContextLevel.VISUAL` or `ContextLevel.VISUAL_RICH`.

The VLM emits coordinates in one of several standard formats:
1. **Explicit Target String**: `coord: #canvas-stage@300,75` or `coord: 300,75`
2. **Point / Coordinate Arrays**: `"coordinates": [300, 75]` or `"point": [300, 75]`
3. **Normalized Bounding Boxes (0..1000 scale)**: `"box_2d": [ymin, xmin, ymax, xmax]`
   - Neodymium calculates the centroid:
     $$\text{centroidX} = \frac{x_{\min} + x_{\max}}{2}, \quad \text{centroidY} = \frac{y_{\min} + y_{\max}}{2}$$

---

### Phase 2: Dynamic Spatial-to-DOM Anchor Pinning (Auto-Pinning)
Raw viewport coordinates (e.g., `coord: 450,210`) are fragile: if a page scrolls, fonts load, or a header banner expands, absolute coordinates will misclick.

**Auto-Pinning Protocol**:
1. At recording time, `PageAnalyzer.resolveAnchorCoordinate(driver, vx, vy)` executes in the browser:
   ```javascript
   const el = document.elementFromPoint(vx, vy);
   ```
2. The engine identifies the nearest stable enclosing container (`#canvas-stage`, `[data-ai="..."]`, `.chart-container`, or tag).
3. The absolute viewport coordinate is transformed into a **relative offset from the container's top-left corner**:
   $$\text{relX} = \text{round}(vx - \text{rect.left}), \quad \text{relY} = \text{round}(vy - \text{rect.top})$$
4. The target locator is recorded as:
   $$\mathbf{\text{coord: } \langle\text{container-selector}\rangle\text{@}\langle\text{relX}\rangle\text{,}\langle\text{relY}\rangle}$$

---

### Phase 3: Zero-Token Replay Execution
During replay mode (`REPLAY_STRICT` or `REPLAY_WITH_HEALING`):
1. **Zero LLM Inference**: No API calls are made ($0$ tokens, sub-millisecond execution).
2. **Anchor Resolution**: `ClickAction` parses the target string and uses `SelenideElementFinder` to locate the container element (applying DOM self-healing if the container's tag, ID, or CSS classes have refactored).
3. **Hardware-Level Click Dispatch**: Selenium `Actions` converts top-left offsets to center-relative offsets and dispatches native mouse events:
   ```java
   final int xOffset = coord.x() - (anchorElement.getSize().getWidth() / 2);
   final int yOffset = coord.y() - (anchorElement.getSize().getHeight() / 2);
   actions.moveToElement(anchorElement.toWebElement(), xOffset, yOffset).click().perform();
   ```

---

## 4. Set-of-Marks (SoM) Visual Markers & Dynamic Grounding

While Direct Coordinate Grounding (Section 3) operates effectively on un-inspectable graphical elements (Canvas, WebGL, raw SVG graphs), standard web applications often suffer from a different challenge: **elements exist in the DOM, but lack deterministic or human-readable locators**.

Examples include:
* **Textless Icon Buttons**: Shopping carts, user avatars, hamburger menus, notification bells, close/expand carets.
* **Obfuscated CSS-in-JS Classes**: Applications generated with styled-components, Emotion, or Tailwind minification (`.css-1a2b3c`, `.rt8oam`) where classes mutate across builds.
* **Complex Data Grids & Toolbars**: Tables with identical action buttons per row.

### Architecture: Visual Badge Injection & Spatial Pinning

To eliminate locator ambiguity, Neodymium implements an on-demand **Set-of-Marks (SoM)** visual marker engine:

```
[Viewport Elements] ──▶ VisualBadgeInjector.injectMarkers() ──▶ Superimpose Badges [1..N]
                                                                        │
                                                                        ▼
                                                         VLM inspects marked screenshot
                                                                        │
                                                                        ▼
[Zero-Token Replay] ◀── coord: <container>@x,y ◀── LLM emits: click(target: "marker:3")
```

1. **Badge Superimposition**: `VisualBadgeInjector` identifies interactive elements visible in the viewport, injecting high-contrast numbered badges (`[1..N]`) over each element with `pointer-events: none` and `data-m="N"` attributes on the marked DOM nodes.
2. **Universal Visual Marker Targeting**: The VLM can target elements using concise marker identifiers (`"marker:3"` or `"badge:3"`) across **any** interactive browser tool (`click`, `type`, `fill`, `enter`, `press_key`, `select`, `check`, `hover`, `clear`).
3. **Interactive Ancestor Normalization & Container-Anchored Resolution**:
   - Before dispatching the action, `ReanchoringBridge` normalizes leaf inner child nodes (`svg`, `path`, `use`, `img`, `i`, `span`) up to their enclosing interactive ancestor (`button`, `a`, `[role="button"]`, `[onclick]`, `[tabindex]`).
   - All synthetic `data-m` and `data-ai*` attributes are stripped from the recorded `DomFeatureVector`, while full structural context (`role`, `accessibleName`, `parentTag`, `siblingIndex`, and deterministic SVG/image `data-graphic-sig` FNV-1a fingerprints) is captured.
   - If the resolved element yields a stable CSS selector (`score >= 40`), the recorded tool call is promoted directly to that selector (`selector: "..."`).
   - Otherwise, `PageAnalyzer.resolveAnchorCoordinate` resolves the element's coordinates relative to its nearest repeating container—either via `#id`, `[data-testid]`/`[data-test]`/`[data-qa]`, a unique semantic class, or a heading-scoped repeating container (`coord:article:has-text("Starter Plan")@relX,relY`)—preventing layout-shift misclicks when sibling cards are inserted or reordered.
4. **SUT Hygiene (Pre-Click Auto-Purging & Clean Visual Baseline Capture)**:
   - Immediately after resolving `marker:N` and **before** capturing visual signatures or dispatching the click, `VisualBadgeInjector.clearMarkers(driver)` purges all marker badges and outlines from the live DOM.
   - `BrowserToolProvider` then captures a clean pre-click screenshot and populates the recorded `DomFeatureVector` with a clean 64-bit element `visualHash` (dHash) and 64x64 `tileSsim` matrix, while storing `KEY_PRE_CLICK_COORD_TILE_MATRIX` so `step.screenshotHash` records the **pre-click** target tile rather than post-click mutated state.
5. **Offline Replay Gating & Self-Healing (`REPLAY_WITH_HEALING`)**:
   - **Pre-Click Visual Tile Verification**: `VisualBaselineGateStep` and `PlaybookToolReplayer` verify the live pre-click 64x64 tile SSIM against the recorded baseline before dispatching coordinate clicks, halting immediately if the visual target has shifted or disappeared.
   - **Multi-Modal Identity & Relocation**: For textless icon buttons (`button`, `svg`, `canvas`), `PlaybookToolReplayer` verifies identity and relocates shifted elements (`score >= 0.55`) using structural ancestry (`parentTag`, `siblingIndex`), SVG/image graphic fingerprints (`data-graphic-sig`), and live cropped visual dHash/tile SSIM, while preserving any container anchor (`coord:<anchor>@newRelX,newRelY`).

### Activation Pathways

Visual markers can be engaged through three complementary pathways:

#### Pathway A: Proactive Author Directive (`(marker)`)
Test authors annotate steps requiring visual grounding directly in YAML playbooks:
```yaml
- step: Click the shopping cart icon (marker)
- step: Click user avatar profile button (MARKER)
```
On Turn 1 of execution, the framework:
- Automatically elevates the context level to at least `ContextLevel.VISUAL_LEAN` to provide the screenshot.
- Injects markers immediately before capturing the initial screenshot.
- Attaches the marked screenshot and instructs the model: *"This instruction is marked with (marker). Visual element markers [1..N] have been activated on screen. Target the intended element using its marker index (e.g. click(target: "marker:3"))."*
- Displays the `🎯 MARKER` badge in HTML and Markdown test reports.

#### Pathway B: Autonomous Quality Judge Escalation
When `neodymium.ai.judge.recommendMarker=true` (default: `true`), test authors do not need to annotate steps manually:
1. If the agent proposes a fragile or volatile locator (e.g., deep structural `:nth-child` or unstable obfuscated class with `score < 4`), and `LocatorImprover` cannot find resilient DOM attributes (IDs, ARIA labels, semantic text).
2. `QualityJudgeToolInterceptor` intercepts the proposed tool call and returns a soft retry error (`RETRY_WITH_FEEDBACK`):
   > *"The proposed locator is fragile and has low quality score. No resilient alternative could be generated from the DOM. Please invoke the tool 'mark_elements' to visually inspect and ground the target element via visual marker index (marker:N)."*
3. The agent autonomously calls `mark_elements`, receives the numbered screenshot, and targets `marker:N` cleanly.

#### Pathway C: Dynamic Agent Tool Invocation
The LLM can autonomously invoke the visual marker tools whenever it encounters layout ambiguity:
* **`mark_elements`**: Injects badges into the live page and captures a marked screenshot.
* **`unmark_elements`**: Clears badges from the live DOM.

---

## 5. Resolution, Retina & DPI Invariance

To guarantee exact coordinate alignment across different developer machines, headless CI environments, and high-DPI displays:
- **1:1 CSS Pixel Parity**: When screenshots are captured on Retina displays (macOS) or HiDPI screens where `devicePixelRatio > 1.0`, images are normalized to exact CSS viewport dimensions (`window.innerWidth`, `window.innerHeight`).
- **Token Optimization**: Normalizing 2x Retina screenshots ($2048 \times 1536 \rightarrow 1024 \times 768$) reduces image payload size by up to $75\%$, cutting VLM latency and token billing.
- **Responsive Layout Stability**: Because coordinates are anchored relative to their container (`#canvas-stage@300,75` or `article:has-text("Starter Plan")@140,220`), responsive shifts, grid reflows, or vertical scrolling maintain precise click positioning.

---

## 6. Supported Visual Interaction Types

| Action Type | Description | Replay Implementation |
| :--- | :--- | :--- |
| **Visual Marker Click / Action** | Interact with a marked element badge (`marker:N` / `badge:N`) across `click`, `type`, `select`, `check`, `hover`, `press_key`, `clear` | Promotes to semantic CSS selector (`score >= 40`) or container-anchored coordinates (`coord:<container>@relX,relY`) with pre-click visual tile verification and multi-modal self-healing |
| **Visual Direct Click** | Click a visual hotspot/button in canvas or SVG | `moveToElement(anchor, dx, dy).click().perform()` |
| **Visual Hover** | Hover over chart nodes or visual items to reveal tooltips | `moveToElement(anchor, dx, dy).perform()` |
| **Visual Drag & Drop** | Move sliders, draw signatures, drag diagram workflow nodes | `moveToElement(anchor, x1, y1).clickAndHold().moveToElement(anchor, x2, y2).release().perform()` |
| **Visual Double Click** | Activate canvas nodes in visual workflow builders | `moveToElement(anchor, dx, dy).doubleClick().perform()` |
| **Visual Assertion** | Verify colors, icons, layout, or checkmarks | SSIM tile hash comparison / Visual RCA check |

---

## 7. Verification & Sandbox Hub

The VLM Visual Grounding and Visual Marker engines are verified in the Neodymium Aura Test Suite:
- **Direct Canvas Grounding Sandbox**:
  - Page: `src/test/resources/ai-test-pages/AuraGlanceTest/shop/sandbox/canvas-click.html`
  - Live Suite: [`CanvasClickSandboxLiveTest.java`](file:///home/rschwietzke/projects/GIT/neodymium-library/src/test/java/org/neodymium/ai/integration/sandbox/live/CanvasClickSandboxLiveTest.java)
  - Mock Replay Suite: [`CanvasClickSandboxMockTest.java`](file:///home/rschwietzke/projects/GIT/neodymium-library/src/test/java/org/neodymium/ai/integration/sandbox/mock/CanvasClickSandboxMockTest.java)
- **Set-of-Marks Visual Markers Sandbox**:
  - Page: `src/test/resources/ai-test-pages/AuraGlanceTest/shop/sandbox/visual-markers.html`
  - Live Suite: [`VisualMarkersSandboxLiveTest.java`](file:///home/rschwietzke/projects/GIT/neodymium-library/neodymium-e2e-tests/src/test/java/org/neodymium/ai/integration/sandbox/live/VisualMarkersSandboxLiveTest.java)
  - Mock Replay Suite: [`VisualMarkersSandboxMockTest.java`](file:///home/rschwietzke/projects/GIT/neodymium-library/neodymium-e2e-tests/src/test/java/org/neodymium/ai/integration/sandbox/mock/VisualMarkersSandboxMockTest.java)
