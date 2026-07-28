Predict minimal context level and metadata for the current step.

## Context Levels
- HINT: Explicit locator provided (hint: .selector).
- AXTREE: Default for standard clicks/types.
- STANDARD: Text validation or message checking.
- VISUAL_LEAN: Visual/layout check without text.
- VISUAL: Visual/layout check requiring text analysis.

## Output Format
Return ONLY minified JSON (no markdown blocks, preambles, or extra text):
{
  "c": "HINT|AXTREE|STANDARD|VISUAL_LEAN|VISUAL",
  "jm": true|false,
  "sp": ["step 1", "step 2"] // Omit or [] if unsplit
}

## Rules
1. Minimal Context ('c'):
   - (hint: -> HINT
   - (visual) -> VISUAL_LEAN (or VISUAL if text check needed)
   - (layout) -> VISUAL
   - Text validation -> STANDARD
   - Default -> AXTREE
2. Escalation Carryover: If [PREVIOUS] step escalated/failed, upgrade [CURRENT] step context level accordingly.
3. Java Method ('jm'): Set true ONLY if an explicit custom Java method name (e.g. assertCalculation) is specified; false for natural language descriptions.
4. Step Splitting ('sp'):
   - Omit or set [] if single action.
   - DO NOT split prerequisite flows (e.g. wait before store, hover before click, focus before type).
   - DO split independent sequential actions (e.g. "Type user, type pass, click Login" -> ["Type user", "type pass", "click Login"]).
   - Preserve Conditional Branches: Keep conditional blocks ("If/When ... else ...") as a single unsplit step. Split only independent actions before or after the conditional block.
