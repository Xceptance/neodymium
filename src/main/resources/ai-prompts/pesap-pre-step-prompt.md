Predict the minimal DOM context level and semantic metadata for the current web test automation step.

## Context Levels
- "AXTREE": Interactive elements only. Default for interaction steps (click, hover, select, type, navigate, clear).
- "STANDARD": Interactive elements + all visible text. Use for validation steps (checking text/messages/values).
- "VISUAL_LEAN": Interactive elements + screenshot. Use for visual/layout checks (e.g., colors, backgrounds, element appearance/positions) that do not require verifying or reading specific text content.
- "VISUAL": Interactive elements + all visible text + screenshot. Use for visual/layout checks that also require full text analysis (such as reading and asserting specific text within an element).

## Output Format
Return ONLY a raw minified JSON object (no markdown, no explanations):
{
  "contextLevel": "AXTREE|STANDARD|VISUAL_LEAN|VISUAL",
  "stepType": "INTERACTION|ASSERTION|STORAGE|NAVIGATION|OTHER",
  "expectedTargetTagName": "button|input|a|div|none",
  "pageNavigation": true|false,
  "requiresJavaMethods": true|false,
  "direction": "One-sentence execution guidance",
  "splitSteps": ["First step text", "Second step text"]
}

## Rules
1. Always predict the MINIMAL context level. Default to AXTREE unless the step clearly requires text validation (STANDARD) or visual/layout checks (VISUAL_LEAN/VISUAL).
2. Analyze ONLY the `[CURRENT]` step. `[PREVIOUS]`/`[NEXT]` are surrounding context only. ALL output fields MUST describe the `[CURRENT]` step exclusively.
3. Set "requiresJavaMethods" to true ONLY if a specific custom Java method name or method pattern (e.g., assertPriceGreaterThanZero, assertCalculation) is explicitly named or identified in the instruction. If no specific method name is specified, "requiresJavaMethods" MUST be false.
4. Step Splitting (`splitSteps`): If the current instruction is a compound, multi-stage action where the second action depends on page state changes that are not yet visible or present in the current DOM (e.g., "Click profile icon and then click Create Account in dropdown", or "Click cart and verify checkout page title"), split it into a sequence of simple steps in the "splitSteps" array.
5. If the current instruction is simple, or all actions can be executed immediately in the current DOM without waiting for page dates, do NOT split (omit "splitSteps").
6. If the current instruction contains the explicit tag "(visual)", predict "VISUAL_LEAN" unless the step also requires reading and asserting specific text content (which requires "VISUAL").
7. If the current instruction contains the explicit tag "(layout)", predict "VISUAL".



