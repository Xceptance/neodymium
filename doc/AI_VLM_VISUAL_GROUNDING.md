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

## 4. Resolution, Retina & DPI Invariance

To guarantee exact coordinate alignment across different developer machines, headless CI environments, and high-DPI displays:
- **1:1 CSS Pixel Parity**: When screenshots are captured on Retina displays (macOS) or HiDPI screens where `devicePixelRatio > 1.0`, images are normalized to exact CSS viewport dimensions (`window.innerWidth`, `window.innerHeight`).
- **Token Optimization**: Normalizing 2x Retina screenshots ($2048 \times 1536 \rightarrow 1024 \times 768$) reduces image payload size by up to $75\%$, cutting VLM latency and token billing.
- **Responsive Layout Stability**: Because coordinates are anchored relative to their container (`#canvas-stage@300,75`), responsive shifts or vertical scrolling maintain precise click positioning.

---

## 5. Supported Visual Interaction Types

| Action Type | Description | Replay Implementation |
| :--- | :--- | :--- |
| **Visual Click** | Click a visual hotspot/button in canvas or SVG | `moveToElement(anchor, dx, dy).click().perform()` |
| **Visual Hover** | Hover over chart nodes or visual items to reveal tooltips | `moveToElement(anchor, dx, dy).perform()` |
| **Visual Drag & Drop** | Move sliders, draw signatures, drag diagram workflow nodes | `moveToElement(anchor, x1, y1).clickAndHold().moveToElement(anchor, x2, y2).release().perform()` |
| **Visual Double Click** | Activate canvas nodes in visual workflow builders | `moveToElement(anchor, dx, dy).doubleClick().perform()` |
| **Visual Assertion** | Verify colors, icons, layout, or checkmarks | SSIM tile hash comparison / Visual RCA check |

---

## 6. Verification & Sandbox Hub

The VLM Visual Grounding engine is verified in the Neodymium Aura Test Suite:
- **Sandbox Challenge Page**: `src/test/resources/ai-test-pages/AuraGlanceTest/shop/sandbox/canvas-click.html`
- **Live Test**: [`CanvasClickSandboxLiveTest.java`](file:///home/rschwietzke/projects/GIT/neodymium-library/src/test/java/org/neodymium/ai/integration/sandbox/live/CanvasClickSandboxLiveTest.java)
- **Mock Replay Test Suite**: [`CanvasClickSandboxMockTest.java`](file:///home/rschwietzke/projects/GIT/neodymium-library/src/test/java/org/neodymium/ai/integration/sandbox/mock/CanvasClickSandboxMockTest.java) (verifies 100% pass rate across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`).
