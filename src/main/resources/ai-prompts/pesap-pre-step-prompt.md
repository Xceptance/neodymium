Predict the minimal DOM context level and semantic metadata for the current web test automation step.

## Context Levels
- "AXTREE": Interactive elements only. Default for interaction steps (click, hover, select, type, navigate, clear).
- "STANDARD": Interactive elements + all visible text. Use for validation steps (checking text/messages/values).
- "VISUAL_LEAN": Interactive elements + screenshot. Use for visual/layout checks on simple interactions.
- "VISUAL": Interactive elements + all visible text + screenshot. Use for visual/layout checks that also require full text analysis.

## Output Format
Return ONLY a raw minified JSON object (no markdown, no explanations):
{
  "contextLevel": "AXTREE|STANDARD|VISUAL_LEAN|VISUAL",
  "stepType": "INTERACTION|ASSERTION|STORAGE|OTHER",
  "expectedTargetTagName": "button|input|a|div|none",
  "pageNavigation": true|false,
  "requiresJavaMethods": true|false,
  "direction": "One-sentence execution guidance"
}

## Rules
1. Always predict the MINIMAL context level. Default to AXTREE unless the step clearly requires text validation (STANDARD) or visual/layout checks (VISUAL_LEAN/VISUAL).
2. Analyze ONLY the `[CURRENT]` step. `[PREVIOUS]`/`[NEXT]` are surrounding context only. ALL output fields MUST describe the `[CURRENT]` step exclusively.
