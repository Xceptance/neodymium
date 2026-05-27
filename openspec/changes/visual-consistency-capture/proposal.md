## Why

Existing solutions for visual validation are either too rigid or too labor-intensive:
- **Pixel/SSIM Regression**: Fragile, environment-dependent, and requires maintaining static baselines for every single page state.
- **Programmatic Layout Assertions** (e.g., `ui-layout-diagnostics`): Extremely precise, but require developers to write manual layout assertions (e.g., `assertDistance`, `assertAlignment`) for every element, which is tedious and unscalable for large sites.

We need a lightweight, background-running visual validator that behaves like a human **"glancing over" the screens** during functional testing. A human glance does not measure exact pixel coordinates or compare against a pixel-perfect snapshot; instead, it looks for "obvious visual inconsistencies" and "common-sense layout errors."

This tool will act as a **Visual Style & Layout Linter**—automatically detecting visual anomalies, styling drift, and layout sanity violations across the application without requiring rigid page-specific baselines or manual layout assertions.

## What Changes

- **Background Visual Glancer**: Integrate a lightweight layout/style collector that runs silently in the background of standard tests (via `NeodymiumWebDriverListener`), capturing DOM structure, positioning, and computed styling details.
- **Statistical Style Inference (Anomaly Detection)**: Analyze styling and layout properties statistically across the entire run. If the application uses a consistent style for certain elements (e.g., 95% of CTA buttons are corporate blue and right-aligned), the engine automatically flags any outlier (e.g., an orange or left-aligned CTA) as a "consistency anomaly."
- **Natural Language Design Rules**: Introduce a simple natural language rule parser allowing teams to write brand and design system guidelines in plain, readable text (e.g., "primary buttons must be corporate-blue", "modal close buttons must be top-right", "labels must be left of inputs"). The engine compiles these rules into automated style and positioning validations.
- **Common-Sense Heuristics (Visual Sanity)**: Enforce universal layout sanity checks automatically:
  - **No Element Collisions**: No visible text elements overlap or collide.
  - **No Viewport Clipping**: Elements are not clipped or cut off by container boundaries.
  - **Structural Consistency**: Repeating visual patterns (like form rows or modal dialogue hierarchies) are structurally uniform.
  - **Visual Contrast & Prominence**: Key interactive elements (CTAs, form controls) have sufficient visual prominence and color contrast.
- **On-Demand Inline Assertions**: Support triggering synchronous visual and design-system consistency assertions for a specific page or state during test execution (e.g., `NeodymiumVisualGlancer.assertVisualConsistency()`). If any layout heuristic violations or compiled natural language rules are broken on the active page state, the engine immediately throws an `AssertionError` to fail the test inline.
- **Interactive Anomaly Report**: Generate an intuitive, visual report post-execution highlighting the elements flagged during the "glance-over" scan with visual highlighting (e.g., bounding boxes) and explanation of the anomaly or heuristic violation.



## Capabilities

### New Capabilities
- `visual-consistency-glancer`: The core background capture and collection mechanism that hooks into test runs.
- `visual-anomaly-detector`: Heuristics and statistical analysis engine to flag layout sanity violations and styling/positioning anomalies across the application.

### Modified Capabilities
<!-- None currently required -->

## Impact

- **Neodymium Core**: Add rules configuration (e.g., setting severity thresholds, defining target elements or selectors) in `neodymium.properties`.
- **Runtime Performance**: Capture must be extremely lightweight (e.g., executing a single, fast JavaScript extraction payload on page load/interaction).
- **Report Portal / HTML Reports**: Custom UI plugin to display visual anomalies side-by-side with functional test outcomes.
