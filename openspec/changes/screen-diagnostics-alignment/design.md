## Context

Neodymium's AI Playbooks allow automated tests to be declared in expressive natural language (e.g., `Verify accessibility focusing on .modal-content` or `Click "Login" button`). However, verification of visual layout constraints—such as ensuring child elements maintain a safe distance from a container's border, or verifying that a list of form labels are perfectly aligned—is currently missing.

This design introduces a new AI Action plugin, `LayoutDiagnosticsAction`, which bridges the gap between natural language layout descriptions and high-performance visual assertions in the browser. It combines LLM-native parsing of expressive spacing guidelines with robust, client-side mathematical evaluation.

To ensure visual steps have all necessary DOM data (text coordinates, sibling classes, and coordinates) without experiencing slow context escalations, all layout assertions are tagged with an explicit, case-insensitive `(layout)` or `[layout]` tag anywhere inside the instruction string.

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
  - **Uniform Alignment Check (`alignment`)**: For a list of target elements $E_i$ and a specified boundary edge (left, right, top, bottom, center):
    $$|\text{edge}(E_i) - \text{edge}(E_j)| \le \text{tolerance} \quad \forall i, j$$
- **Rationale**: Abstracting these into clean mathematical predicates keeps the code highly maintainable and allows us to reuse the same evaluation engine for both explicit and semantic assertions.

### 3. Noise-Free Viewport Health Auditing
- **Decision**: When executing a full viewport layout health audit (e.g., `Verify viewport health (layout)`), apply strict structural filters to eliminate noise and false positives:
  - **Parent-Child Collision Exclusion**: Do not check collision/overlap between any two elements where one is an ancestor of the other in the DOM tree.
  - **Leaf Node / Interactive Element Focus**: Restrict the bounding box collision checks to visible, rendered text leaf nodes and interactive controls (buttons, inputs, select boxes).
  - **Horizontal Scrollbar Check**: Perform `document.documentElement.scrollWidth > window.innerWidth` checks, and if violated, scan and identify which element is overflowing the right edge of the viewport.
- **Rationale**: A naive DOM overlap scan flags thousands of background shapes, layout wrappers, and nested layers. Smart filtering ensures the engine only flags real, user-facing bugs (like overlapping text or viewport clipping).

### 4. JavaScript-Driven Bounding Box Extraction
- **Decision**: Execute a single, highly optimized JavaScript payload in the browser to query element coordinates (`getBoundingClientRect()`) and perform layout calculations.
- **Rationale**: Fetching positions element-by-element from Java via WebDriver would trigger multiple network roundtrips, slowing down test runs dramatically. Doing it in a single JavaScript injection keeps execution fast and lightweight.

## Risks / Trade-offs

- **[Risk] Subpixel Rendering Variance** → Different browsers, viewports, or OS font-renderers can cause subpixel layout coordinates (e.g., $10.4\text{px}$ vs $10.6\text{px}$).
  - *Mitigation*: Introduce a default tolerance of $1\text{px}$ for all assertions, allowing users to customize this tolerance inside the natural language step (e.g. `tolerance: 2px`).
- **[Risk] Dynamic or Animated Layouts** → Running assertions while a dialog or element is still animating can cause false layout failures.
  - *Mitigation*: Ensure the evaluation engine waits for layout stability (i.e. element positions do not change over a brief window) before performing calculations, using standard Selenide wait conditions.
