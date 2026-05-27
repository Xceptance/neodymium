## Why

Automated functional testing using selectors verifies if elements exist and have correct values, but completely misses layout issues such as overlapping elements, incorrect element spacing, misalignment, and viewport overflows. We need a way within Neodymium's AI Playbooks for users to declare layout constraints and visual assertions in natural language with structured hints (e.g. specifying targets, directions, or distances). This reduces reliance on fragile pixel-by-pixel visual regression testing and allows non-technical stakeholders to write robust layout verification directly in their natural language test scripts.

## What Changes

- **AI Playbook Layout Action**: Introduce a pluggable AI Playbook action (`LayoutDiagnosticsAction`) that parses visual natural language layout assertions from AI test scripts.
- **Visual Assertions Layout Tag**: Introduce a case-insensitive `(layout)` tag permitted anywhere within the natural language step (e.g. `Verify that '.logo' is left of '.menu-bar' (layout)` or `(layout) all labels are left-aligned identically`). This tag triggers immediate routing to the layout engine, bypasses standard action handlers, and instructs Neodymium to load the maximum visual context level.
- **Natural Language Parsing with Spacing Hints**: Support declarative natural language statements with parenthetical or inline spacing hints specifying elements, directions, distances, tolerances, and container boundaries (e.g. `Verify that '.logo' is left of '.menu-bar' (distance: 20px, tolerance: 5px) (layout)`, `Verify alignment of '.card-title' (top) (layout)`, or `Verify container '.container' has a minimum inner distance of 20px on the left (layout)`).
- **Layout Diagnostics Engine**: A lightweight engine running in the browser to compute element bounding boxes, calculate distances (horizontal, vertical, overlap), check container inner-distance boundaries, and verify alignments based on the parsed assertions.

## Capabilities

### New Capabilities
- `visual-layout-diagnostics`: Introduces natural language layout action parsing (routed via flexible `(layout)` tags), bounding box calculation, and distance/alignment/inner-boundary validation within AI Playbooks.

### Modified Capabilities
- `ai-core-routing`: Modifies the core routing engine in `AiAgent` to recognize the new `(layout)` tag and immediately escalate the context level to `ContextLevel.VISUAL`.

## Impact

- **AI Core Routing**: Update Neodymium's `AiAgent.java` to support the custom `(layout)` tag, routing it directly to `ContextLevel.VISUAL` to ensure the LLM receives full DOM details on the first turn and avoids expensive context escalations.
- **AI Action Plugins**: Register `LayoutDiagnosticsAction` in Neodymium's AI Action Registry to parse and execute layout assertion steps.
- **Diagnostic Engine**: JavaScript execution helper to fetch bounding boxes and compute coordinates efficiently on the browser side.
- **Test Reporting**: Integration with Neodymium reporting/logging to highlight diagnostic layout failures with exact pixel dimensions and element highlights in Allure.

