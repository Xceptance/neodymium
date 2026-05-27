## Why

Existing solutions for visual validation are either too rigid or too labor-intensive:
- **Pixel/SSIM Regression**: Fragile, environment-dependent, and requires maintaining static baselines for every single page state.
- **Programmatic Layout Assertions** (e.g. `(layout)`): Extremely precise, but require developers to write manual layout assertions (e.g., `left-of`, `above`, or exact distance bounds) for every element, which is tedious and unscalable for general visual linting.

We need a lightweight, background-running visual validator that behaves like a human **"glancing over" the screens** during functional testing. A human glance does not measure exact pixel coordinates or compare against a pixel-perfect snapshot; instead, it looks for "obvious visual inconsistencies" and "common-sense layout/usability errors."

Under the **Neodymium Aura AI** suite, **Aura Glance** provides this observational visual linter. It captures screenshots and layouts automatically, and utilizes Gemini's multimodal visual reasoning to check pages against general styling, usability, and visual sanity heuristics with zero configuration.

## What Changes

- **Background Aura Glance Capture**: Integrate a lightweight layout/style collector that runs silently in the background of Neo AI runs (via `AuraCaptureListener`), capturing viewport screenshots and a lightweight Aura AST representation of visible elements.
- **Multimodal AI Visual Auditing**: During critical checkpoints or at the end of runs, the system sends screenshots and page layout profiles to Gemini. The LLM acts as an expert visual auditor, observing the page for overlapping text blocks, clipped elements, alignment drift, contrast anomalies, or general layout breakage.
- **Playbook (glance) Tag Integration**: Support a case-insensitive `(glance)` tag in natural language playbook steps (e.g. `Observe page visual consistency (glance)`). Steps with this tag route directly to `ContextLevel.VISUAL` to ensure the agent has full visual context on the very first turn.
- **Non-Blocking Soft Warning Gates**: Support case-insensitive `(soft)` tags (e.g., `Observe page visual consistency (soft) (glance)`). By default, critical visual anomalies detected by Gemini throw an `AssertionError` to fail the build. When tagged with `(soft)`, any anomalies are instead logged as non-blocking `WARNING` steps in the Aura execution report, allowing the suite to continue uninterrupted.
- **Visual Playbook Cache (dHash)**: Implement local dHash visual baseline matching. If the live viewport screenshot has a perceptual hash matching the cached baseline, Neodymium bypasses all LLM visual queries entirely and succeeds instantly and offline.
- **Interactive Aura Glance Report**: Display captured visual anomalies with bounding boxes layered directly over screenshots in the Aura Server Trace Viewer dashboard, complete with Gemini's text-based design reasoning and manual baseline approval.

## Capabilities

### New Capabilities
- `aura-glance-listener`: Background capture mechanism that captures screenshots and Aura AST metadata.
- `aura-glance-auditor`: Multimodal AI-driven linter that analyzes screens, checks guidelines, and raises visual anomalies.

### Modified Capabilities
- `ai-core-routing`: Recognize the new `(glance)` tag and route it directly to `ContextLevel.VISUAL`.
- `ai-agent-decoupling`: Decouple `(optional)` (silent skip) from `(soft)` (non-blocking warning logging) execution behaviors.

## Impact

- **Neodymium Core**: Add rules configuration (e.g., setting severity thresholds, defining target elements or selectors) in `neodymium.properties`.
- **Runtime Performance**: Capture is extremely lightweight (e.g., executing a single, fast JavaScript extraction payload on page load/interaction).
- **Report Portal / Aura Dashboard**: Display visual anomalies side-by-side with functional test outcomes, complete with dynamic HTML5 canvas bounding highlights.
