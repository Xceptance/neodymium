# Architectural Proposal: Spatial DOM Augmentation & Visual Interaction Extensions

## 1. Overview & Motivation

As modern single-page applications (SPAs) increasingly rely on dynamic UI frameworks (React, Angular, Vue), shadow DOM encapsulation, custom Web Components, dynamic obfuscation, and canvas-rendered widgets, traditional selector-based automation (CSS, XPath, IDs) occasionally reaches operational limits. 

This document outlines an architectural extension to Neodymium AI to bridge the gap between traditional DOM-based automation and pure Vision-Language Model (VLM) navigation. By introducing **Set-of-Marks (SoM) visual grounding**, **spatial bounding-box DOM augmentation**, **visual input field lifecycle verification**, and **human-like cursor trajectory simulation**, Neodymium AI expands its context escalation ladder and resilient action execution capabilities.

---

## 2. Set-of-Marks (SoM) Visual Grounding & Coordinate-Based Clicking

### 2.1 Problem Statement
Pure visual navigation using Vision-Language Models (VLMs) often suffers from **coordinate hallucination** when requested to output absolute screen pixels `(x, y)`. Conversely, pure DOM navigation fails when elements lack stable selectors or reside inside closed Shadow DOM trees, dynamic Canvas elements, or dynamic iframe structures.

### 2.2 Mechanism
Set-of-Marks (SoM) visual grounding combines structural DOM metadata with visual perception:

1. **Element Extraction & Annotation:** Prior to transmitting a screenshot to the VLM, a lightweight JavaScript injection scans interactive nodes (or candidates identified via A11y/DOM parsing) and overlays high-contrast, semi-transparent numbered badges (`[1]`, `[2]`, `[3]`) over their bounding box origins.
2. **Visual Viewport Snapshot:** The viewport with rendered badge overlays is captured.
3. **VLM Selection:** The annotated screenshot is passed to the VLM with a prompt such as: *"Identify the badge number corresponding to the 'Checkout' button."*
4. **Coordinate & Handle Resolution:** The VLM returns the badge number (e.g., `#14`). Neodymium AI maps `#14` back to its exact bounding box center coordinates `(x: 412, y: 650)` and underlying DOM element handle.

```
[ Viewport HTML ] ──> [ Inject Badge Overlays ] ──> [ Capture Screenshot ]
                                                            │
                                                            ▼
[ Click (x: 412, y: 650) ] ◄── [ Map #14 -> Coords ] ◄── [ VLM Output: "#14" ]
```

---

## 3. Spatial DOM Augmentation (`SPATIAL_LEAN` & `SPATIAL_STANDARD`)

### 3.1 Concept
Standard DOM serialization (`LEAN` / `STANDARD`) strips layout information to minimize prompt token overhead. However, spatial geometry is often necessary to resolve ambiguous elements or detect overlapping UI components without requiring full image token processing.

### 3.2 Metadata Structure
The DOM extraction script is augmented to output bounding boxes, viewport offsets, and stacking contexts:

```json
{
  "ref": "node-42",
  "tag": "button",
  "text": "Place Order",
  "id": "btn-submit-104",
  "bbox": {
    "x": 450,
    "y": 620,
    "width": 120,
    "height": 40
  },
  "zIndex": 100,
  "visible": true,
  "occluded": false
}
```

### 3.3 Benefits
- **Token-Efficient Spatial Reasoning:** Enables LLMs to resolve spatial instructions (e.g., *"Click the 'Login' button at the top-right corner"*) using text tokens rather than expensive visual image tokens.
- **Occlusion Detection:** Automatically identifies when target elements are hidden behind sticky headers, floating cookie banners, or modal backdrops (`zIndex` & spatial bounds comparison).

---

## 4. Visual Input Field Interaction & Verification Lifecycle

Standard test frameworks interact with text inputs via `.sendKeys()` or DOM value property manipulation. In modern SPAs, synthetic event handling (e.g., React `onChange`), dynamic input masking (credit cards, phone numbers), custom rich text editors (Draft.js, Slate), or Canvas inputs can cause silent state desynchronization.

Neodymium AI introduces a 4-step **Visual Input Lifecycle**:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Visual Input Lifecycle                          │
├────────────────────────────────────────────────────────────────────────┤
│ 1. SPATIAL FOCUS    ──> Target input center (x, y) & trigger click    │
│ 2. FOCUS AUDIT      ──> Verify CSS :focus ring / caret blinking        │
│ 3. CHUNKED TYPING   ──> Dispatch key events (character or chunked)     │
│ 4. VISUAL VERIFY    ──> Crop field region & perform micro-OCR / VLM    │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Spatial Focus & Activation:** Move cursor to the target input bounding box center `(x, y)` and trigger click.
2. **Focus Audit:** Visually verify the presence of the visual focus ring or caret cursor (`:focus` visual audit).
3. **Chunked Key Dispatch:** Send typing events sequentially.
4. **Visual Content Verification:** Perform a targeted micro-crop of the input's bounding box and execute lightweight OCR or VLM visual inspection to verify the string actually rendered on screen.

---

## 5. Realistic Mouse Trajectories & Visual Cursor Simulation

### 5.1 Problem Statement
Instantaneous coordinate teleports (`click(x, y)`) and synthetic JavaScript clicks bypass natural browser event streams. Modern Web Application Firewalls (WAFs) and bot detection mechanisms (Cloudflare, Akamai, DataDome) flag automated sessions that lack `mousemove` vectors, feature zero movement velocity variance, or execute clicks without prior cursor hover events.

### 5.2 Humanized Trajectory Generation
Neodymium AI incorporates a trajectory engine that calculates realistic mouse paths using:
- **Cubic Bézier Curves:** Generates curved, non-linear trajectories between point $A(x_1, y_1)$ and point $B(x_2, y_2)$.
- **Fitts' Law Velocity Profiles:** Simulates human motor control (rapid acceleration at motion start, followed by smooth deceleration as the pointer approaches small targets).
- **Perlin Noise & Jitter:** Adds subtle micro-variations and small overshoots/corrections near the target.

```
       Control Point 1 (Handles)
              o . . . . . .
             .             .
            .               .
    Start  o                 o Target
     (A)                      (B)
```

### 5.3 Execution & Visual Cursor Overlay
- **CDP Protocol Execution:** Dispatch intermediate `Input.dispatchMouseEvent` calls (`type: mouseMoved`) along calculated curve points.
- **Native OS Fallback:** Drive system mouse via Java `Robot` / X11 / Win32 API during headed execution.
- **Faked Visual Cursor Overlay:** Inject a high-contrast, synthetic SVG cursor element into the DOM at high `z-index` with `pointer-events: none`. As CDP dispatches mouse movements, the SVG cursor is animated smoothly, making the AI's physical focus visible in **Aura Glance** screencasts, GIF recordings, and execution logs.

---

## 6. Integration into Neodymium AI Architecture

### 6.1 Escalating Context Ladder Alignment

The new spatial and visual capabilities integrate cleanly into the **6-Tier Escalating Context System**:

| Level | Tier Name | Data Payload & Purpose |
| :--- | :--- | :--- |
| **1** | `HINT` | Zero DOM elements. Activated via inline hints `(hint: ...)`. |
| **2** | `AXTREE` | Accessibility Tree outline (~90% smaller than raw HTML). |
| **3** | `LEAN` | Interactive DOM elements only (`<button>`, `<input>`, `<a>`, `<select>`). |
| **3.5** | `SPATIAL_LEAN` **[NEW]** | Interactive DOM + Bounding Boxes `[x, y, w, h]` & `zIndex` metadata. |
| **4** | `STANDARD` | Full interactive DOM + visible text nodes for assertions. |
| **5** | `VISUAL_LEAN` | Interactive DOM + compressed viewport screenshot. |
| **5.5** | `VISUAL_SOM` **[NEW]** | Viewport screenshot with Set-of-Marks (SoM) badge overlays. |
| **6** | `VISUAL` | Full raw DOM + uncompressed viewport screenshot. |

### 6.2 Creation & Replay Mode Lifecycle

- **Creation Mode:** When recording a new playbook step, if selector generation fails or element occlusion occurs, Neodymium AI escalates to `SPATIAL_LEAN` or `VISUAL_SOM`. The compiled action is stored in the JSON Playbook as:
  ```json
  {
    "step": 3,
    "action": "COORDINATE_CLICK",
    "target": {
      "x": 412,
      "y": 650,
      "somBadge": 14,
      "fallbackSelector": "#btn-submit"
    },
    "trajectory": "HUMAN_BEZIER"
  }
  ```
- **Replay Mode:** On CI/CD execution, Neodymium AI first attempts fast direct playback via `fallbackSelector`. If locator healing fails, it falls back to saved spatial coordinates `(x, y)` combined with humanized Bézier trajectory movement.

### 6.3 Action Execution Engine Mapping

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Neodymium AI Agent Core                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       Escalating Context Selector                      │
│ [HINT] ──> [AXTREE] ──> [LEAN] ──> [SPATIAL_LEAN] ──> [VISUAL_SOM]    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    Humanized Trajectory Generator                      │
│ - Calculates Bézier control points & Fitts' velocity profile           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     Browser Driver Protocol Layer                      │
│ - CDP Input.dispatchMouseEvent / DOM Visual Cursor SVG Animation       │
└────────────────────────────────────────────────────────────────────────┘
```
