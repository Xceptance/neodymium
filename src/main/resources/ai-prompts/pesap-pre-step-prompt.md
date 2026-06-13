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
  "direction": "One-sentence execution guidance",
  "splitSteps": ["First step text", "Second step text"]
}

## Rules
1. Always predict the MINIMAL context level. Default to AXTREE unless the step clearly requires text validation (STANDARD) or visual/layout checks (VISUAL_LEAN/VISUAL).
2. Analyze ONLY the `[CURRENT]` step. `[PREVIOUS]`/`[NEXT]` are surrounding context only. ALL output fields MUST describe the `[CURRENT]` step exclusively.
3. Set "requiresJavaMethods" to true ONLY if the step requires a custom Java assertion/utility method (e.g., assertPriceGreaterThanZero, assertCalculation) rather than standard browser actions (click, type, select, hover, navigate, scroll, key press).
4. Step Splitting (`splitSteps`): If the current instruction is a compound, multi-stage action where the second action depends on page state changes that are not yet visible or present in the current DOM (e.g., "Click profile icon and then click Create Account in dropdown", or "Click cart and verify checkout page title"), split it into a sequence of simple steps in the "splitSteps" array.
5. If the current instruction is simple, or all actions can be executed immediately in the current DOM without waiting for page dates, do NOT split (omit "splitSteps" or set an empty array).


