## Why

Relying exclusively on the Accessibility Object Model (AOM) or pure DOM extraction for interactive elements leads to brittleness in real-world applications (Div-Soup, Canvas, flawed ARIA, WebGL). By introducing a Unified Perception Model (UPM), a 5-Tier Locator Cascade with Local Feature Proximity Healing, and retaining the Quality Judge as an automated quality gatekeeper, we can drastically increase the resilience of `org.neodymium.ai` during replay, allowing the engine to deterministically self-heal locally in $<1\text{ ms}$ without expensive LLM API calls, while natively supporting complex visual boundaries and ensuring execution determinism via framework locking.

## What Changes

- Refactors DOM extraction to use a Unified Perception Model (UPM) containing AOM (accessible name/role), DOM features (test-ids, classes, attributes, text), and Geometric coordinates (normalized bounding boxes).
- Replaces the brittle `data-ai` hash string with a structured DOM Feature Vector and a Feature Proximity Fallback search powered by Jaccard set similarity and Levenshtein fuzzy string distance.
- Introduces a 5-Tier Locator Cascade (Engineering Hooks -> Semantic/AOM -> Text -> Feature Proximity -> Visual Coordinates).
- Stores dual representation in companion `.json` recordings: concrete selectors for Tiers 1–3 and the DOM Feature Vector for Tier 4.
- Implements Semantic Tag Equivalence Bucketing so element type changes (e.g. `<button>` to `<a>` or `<div role="button">`) with matching text and attributes resolve smoothly via Tier 4 local healing.
- Elevates and standardizes the Quality Judge (`QualityJudgePrompt`) to evaluate the full multi-tier candidate bundle during recording and implement the unified `AiPrompt<QualityJudgeResult>` interface.
- **BREAKING**: Re-orders execution escalation paths for interactive actions (`STANDARD` -> `RICH` -> `VISUAL_LEAN` -> `VISUAL_RICH`), avoiding the 0-DOM `VISUAL` tier for click/type steps.
- Implements Visual Input support by decoupling coordinate-based focus clicks from raw keyboard event dispatch.
- **BREAKING**: Modifies the Companion JSON format to store multi-tier locator schemas and requires a `targetFramework` metadata attribute for strict cross-framework replay enforcement.

## Capabilities

### New Capabilities
- `unified-perception-model`: Establishes the UPM metadata extraction and structured DOM Feature Vectors for elements.
- `locator-cascade`: Introduces the 5-Tier fallback chain, dual selector/feature storage, and resilient local healing via Jaccard/Levenshtein similarity.
- `framework-enforcement`: Strict `targetFramework` lock during companion replay to prevent cross-engine traversal failures.
- `visual-form-input`: Formalizes input handling for `<canvas>` and purely visual elements via coordinate focus + raw key dispatch.
- `quality-judge-elevation`: Standardizes the Quality Judge under `AiPrompt<T>` and expands its rubric to grade the entire 5-tier candidate set during recording.

### Modified Capabilities
- 

## Impact

- `org.neodymium.ai.executor.selenide.PageAnalyzer`: Rewritten JavaScript extraction logic for UPM generation.
- `org.neodymium.ai.pipeline.steps.ExecuteActionsStep`: Decomposed into separate lifecycle stages (PESAP, Visual Baseline, Execution).
- `org.neodymium.ai.executor.selenide.ContextLevel`: Package relocation and redefined escalation paths.
- `org.neodymium.ai.model.Action`: JSON structure migrated from a single selector string to a candidate array list and feature vector.
- `org.neodymium.ai.prompt.QualityJudgePrompt`: Standardized under `AiPrompt<QualityJudgeResult>`.
- `org.neodymium.ai.runner.StateMachineRunner`: Modified initialization logic to assert the framework lock constraint.
