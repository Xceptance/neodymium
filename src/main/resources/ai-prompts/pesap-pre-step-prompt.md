Predict minimal DOM context level and semantic metadata for the current web test automation step.

## Context Levels
- "AXTREE": Interactive elements only. Default for interaction (click, hover, select, type, navigate, clear).
- "STANDARD": Interactive elements + visible text. Use for text validation (messages/values).
- "VISUAL_LEAN": Interactive elements + screenshot. Use for visual/layout checks without text validation.
- "VISUAL": Interactive + visible text + screenshot. Use for visual/layout checks requiring text analysis.

## Output Format
Return ONLY minified JSON:
{
  "c": "AXTREE|STANDARD|VISUAL_LEAN|VISUAL",
  "jm": true|false,
  "sp": ["First step text", "Second step text"]
}

## Rules
1. Predict MINIMAL context level. Default to AXTREE unless text validation (STANDARD) or visual/layout checks (VISUAL_LEAN/VISUAL) are required.
2. Describe the `[CURRENT]` step exclusively. `[PREVIOUS]`/`[NEXT]` are for context only.
3. Set "jm" to true ONLY if a custom Java method name or pattern (e.g. assertCalculation) is explicitly identified in the step. Otherwise, false.
4. Step Splitting: If the step is compound/multi-stage and the second action depends on page state changes (e.g., "click menu then click Sign out"), split into simple steps in "sp". Otherwise, omit "sp".
5. If the step contains "(visual)", predict VISUAL_LEAN (or VISUAL if text checking is also required). If it contains "(layout)", predict VISUAL.
