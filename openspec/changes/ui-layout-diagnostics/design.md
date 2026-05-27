## Context

Neodymium's AI Playbooks allow automated tests to be declared in expressive natural language (e.g., `Verify accessibility focusing on .modal-content` or `Click "Login" button`). However, verification of visual layout constraints—such as ensuring child elements maintain a safe distance from a container's border, or verifying that a list of form labels are perfectly aligned—is currently missing.

This design introduces a new AI Action plugin, `LayoutDiagnosticsAction`, which bridges the gap between natural language layout descriptions and high-performance visual assertions in the browser. It combines LLM-native parsing of expressive spacing guidelines with robust, client-side mathematical evaluation.

To ensure visual steps have all necessary DOM data (text coordinates, sibling classes, and coordinates) without experiencing slow context escalations, all layout assertions are tagged with an explicit, case-insensitive `(layout)` tag anywhere inside the instruction string.

## Goals / Non-Goals

**Goals:**
- Provide a natural language layout assertion mechanism within Neodymium's AI Playbooks, using a new `(layout)` tag that routes directly to the maximum context level (`ContextLevel.VISUAL`).
- Support mathematical evaluation of complex layout relationships (e.g., container inner-distance boundaries and identical element alignment).
- Minimize false positives in viewport audits by excluding parent-child nested collisions and focusing on leaf text/interactive nodes.
- Execute visual checks efficiently without incurring massive WebDriver network overhead.
- Present descriptive layout assertion failures in the test report showing exact pixel deviations.

**Non-Goals:**
- Creating a general-purpose, full-page pixel-by-pixel visual regression engine (e.g., SSIM comparisons).
- Automatic correction/fixing of layout issues in the browser.
- Supporting interactive drag-and-drop design builders.

## Decisions

### 1. Direct VISUAL Context Routing via `(layout)` Tag
- **Decision**: Update Neodymium's core `AiAgent.java` class inside the `getInitialContextLevel` method to recognize the case-insensitive `(layout)` tag anywhere in the string:
  ```java
  if (lower.contains("(layout)"))
  {
      return ContextLevel.VISUAL; // Starts directly at maximum context level!
  }
  ```
- **Rationale**: 
  - The standard `(visual)` tag starts the agent at `ContextLevel.VISUAL_LEAN`, which does **not** include text content or full DOM attributes.
  - Layout verification (resolving "all labels" or "the dialog box" to actual CSS selectors) absolutely requires the full DOM detail. Starting at `VISUAL_LEAN` would result in a mapping failure, triggering a slow context escalation to `VISUAL`.
  - Directly mapping the `(layout)` tag to `ContextLevel.VISUAL` gives the LLM full semantic context on the very first turn, completely avoiding slow escalation overhead.

### 2. Spacing and Alignment Evaluation Predicates
- **Decision**: Define layout validation rules as strict mathematical checks executed on the element bounding boxes:
  - **Inner Boundary Check (`inner-distance`)**: For a container $C$ and child elements $E_i$:
    $$\text{left}(E_i) - \text{left}(C) \ge \text{expected} - \text{tolerance}$$
    $$\text{right}(C) - \text{right}(E_i) \ge \text{expected} - \text{tolerance}$$
    $$\text{top}(E_i) - \text{top}(C) \ge \text{expected} - \text{tolerance}$$
    $$\text{bottom}(C) - \text{bottom}(E_i) \ge \text{expected} - \text{tolerance}$$
  - **Uniform Alignment Check (`alignment`)**: For a list of target elements $E_i$ and a specified boundary edge:
    $$|\text{edge}(E_i) - \text{edge}(E_j)| \le \text{tolerance} \quad \forall i, j$$
    Where $\text{edge}(E)$ is defined as:
    *   `top`: $\text{top}(E)$
    *   `bottom`: $\text{bottom}(E)$
    *   `left`: $\text{left}(E)$
    *   `right`: $\text{right}(E)$
    *   `vertical-center`: $\text{top}(E) + \frac{\text{height}(E)}{2}$
    *   `horizontal-center`: $\text{left}(E) + \frac{\text{width}(E)}{2}$
  - **Overlap Check (`overlap`)**: Two elements $A$ and $B$ overlap if their bounding boxes intersect on both axes:
    $$\text{left}(A) < \text{right}(B) \quad \land \quad \text{right}(A) > \text{left}(B) \quad \land \quad \text{top}(A) < \text{bottom}(B) \quad \land \quad \text{bottom}(A) > \text{top}(B)$$
- **Rationale**: Abstracting these into clean mathematical predicates keeps the code highly maintainable and allows us to reuse the same evaluation engine for both explicit and semantic assertions.

### 3. Visual Failure Overlays via Temporary Canvas
- **Decision**: When any layout assertion fails, the system SHALL temporarily inject a single, absolute-positioned HTML5 `<canvas>` layered directly over the viewport prior to capturing the screenshot:
  - The canvas element SHALL have CSS styling: `position: absolute; top: 0; left: 0; pointer-events: none; z-index: 2147483647; width: 100%; height: 100%;`.
  - Draw highlight indicators, distance lines, bounding boxes, and error callouts using the canvas 2D drawing context (`CanvasRenderingContext2D`).
  - To guarantee high contrast against any page background color (e.g. avoiding red indicators from becoming invisible on a red background), all outline strokes SHALL employ a dynamic high-contrast rendering technique, such as a **double-stroke** (drawing a wider 3px solid white outer stroke as a boundary outline, layered underneath a narrower 1px solid red/colored foreground stroke) or utilizing standard canvas difference composite blend modes (`globalCompositeOperation = 'difference'`).
  - This single-element canvas strategy avoids mutating reactive virtual DOM trees (React, Vue, Angular) and guarantees 100% safety.
  - Immediately remove the canvas element from the DOM during the `cleanup()` phase.
- **Rationale**: Direct DOM node injection is prone to interfering with the application's client-side framework reconciliation. A high-index, absolute-positioned canvas overlay provides rich failure diagnostics without reactive side-effects.

### 4. JavaScript-Driven Bounding Box Extraction
- **Decision**: Execute a single, highly optimized JavaScript payload in the browser to query element coordinates (`getBoundingClientRect()`) and perform layout calculations.
- **Rationale**: Fetching positions element-by-element from Java via WebDriver would trigger multiple network roundtrips, slowing down test runs dramatically. Doing it in a single JavaScript injection keeps execution fast and lightweight.

### 5. LLM Prompt Contract & Action JSON Schema
- **Decision**: Define a standardized natural language layout instruction contract for `LayoutDiagnosticsAction.getPromptInstructions()` mapping to standard `Action` attributes as follows:
  - `type`: `"LAYOUT_DIAGNOSTICS"`
  - `target`: The CSS selector, data-neo-ref reference, or XPath locator of the primary element/elements to verify.
  - `value`: A serialized layout expression specifying the spatial relation, alignment constraint, overlap constraint, or container inner-distance boundaries. Supported patterns are:
    - **Spatial Relation**: `"<relation> <selectorB> [distance: <dist>px] [tolerance: <tol>px]"` (where `<relation>` is: `left-of`, `right-of`, `above`, `below`).
      *Example: target: ".logo", value: "left-of .menu-bar distance: 20px tolerance: 5px"*
    - **Element Overlap**: `"does not overlap <selectorB>"` or `"overlaps <selectorB>"`.
      *Example: target: "#header", value: "does not overlap #main-content"*
    - **Uniform Alignment**: `"alignment <edge> [tolerance: <tol>px]"` (where `<edge>` is: `top`, `bottom`, `left`, `right`, `vertical-center`, `horizontal-center`).
      *Example: target: ".card", value: "alignment top tolerance: 2px"*
    - **Container Inner-Distance**: `"inner-distance <edge> distance: <dist>px [tolerance: <tol>px]"` (where `<edge>` is: `top`, `bottom`, `left`, `right`).
      *Example: target: ".container", value: "inner-distance left distance: 15px tolerance: 3px"*
- **Rationale**: Defining a clean, readable layout string format inside the `value` field keeps the LLM's action payload concise, standardizes representation, and ensures easy parsing by both the AI action plugin and manual direct-instruction parser.

### 6. Configurable Properties & Step-Level Overrides
- **Decision**: Introduce new configuration properties inside `NeodymiumConfiguration` with support for local, step-level overrides parsed from the natural language instructions:
  - **Tolerance**:
    - Global property: `@Key("neodymium.ai.layout.defaultTolerance")` (default: `1` pixel).
    - Local override inside a step: `(tolerance: 5px)`.
  - **Layout Stability Timeout**:
    - Global property: `@Key("neodymium.ai.layout.stabilityTimeout")` (default: `2000` milliseconds).
    - Local override inside a step: `(timeout: 5000ms)` or `(stability: 1000ms)` or `(wait: 3000ms)`.
- **Rationale**: Providing a global setting allows operations in environment-specific runner clusters (like headless Docker boxes in CI) to easily increase rendering tolerance globally via system properties (e.g. `-Dneodymium.ai.layout.defaultTolerance=2`), while still preserving exact fine-tuning control inside specific test cases.

## Risks / Trade-offs

- **[Risk] Subpixel Rendering Variance** → Different browsers, viewports, or OS font-renderers can cause subpixel layout coordinates (e.g., $10.4\text{px}$ vs $10.6\text{px}$).
  - *Mitigation*: Fallback to the configured global tolerance (`neodymium.ai.layout.defaultTolerance`), permitting local overrides inside the step instructions (e.g. `(tolerance: 2px)`).
- **[Risk] Dynamic or Animated Layouts** → Running assertions while a dialog or element is still animating can cause false layout failures.
  - *Mitigation*: Implement a hybrid stability polling loop where Java polls coordinates every 100ms using a lightweight JavaScript coordinate extraction snippet. Performing wait intervals in Java avoids locking the browser's single-threaded event loop, allowing animations to complete naturally. Java compares bounding box dimensions between consecutive polls and declares stability once they are identical. The loop respects the global timeout `neodymium.ai.layout.stabilityTimeout` (default: 2000ms) and accepts step-level overrides (e.g., `(timeout: 5000ms)`).
- **[Risk] Merge Conflict with usability-checks** → The `usability-checks` change also modifies `AiAgent.getInitialContextLevel` to route custom `(usability)` tags.
  - *Mitigation*: Order the check cleanly in `getInitialContextLevel`. Since the two tags route to different levels (e.g., `ContextLevel.STANDARD` or `ContextLevel.VISUAL`), a standard independent if-chain will seamlessly support both.


