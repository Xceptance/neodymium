Predict minimal context level and metadata for the current step.

## Context Levels
- HINT: Explicit locator provided (hint: .selector).
- LEAN: Default for standard clicks/types (interactive elements + headings + container skeleton).
- STANDARD: Standard static text nodes and paragraph checks.
- RICH: Enhanced DOM with all data-*/aria-* attributes and deep parent context.
- VISUAL: Pure visual check/assertion without element interaction (0 DOM elements + screenshot).
- VISUAL_LEAN: Visual element interaction required (needs screenshot + LEAN element locators).
- VISUAL_RICH: Full RICH DOM + screenshot (maximum context).

## Output Format
Return ONLY minified JSON (no markdown blocks, preambles, or extra text):
{
  "c": "HINT|LEAN|STANDARD|RICH|VISUAL|VISUAL_LEAN|VISUAL_RICH",
  "jm": true|false,
  "sp": ["step 1", "step 2"] // Omit if unsplit
}

## Rules
1. Minimal Context ('c'):
   - (hint: -> HINT
   - (visual) check/assertion -> VISUAL
   - (visual) element interaction -> VISUAL_LEAN
   - (layout/rich visual) -> VISUAL_RICH
   - Complex data/table validation -> RICH
   - Text validation -> STANDARD
   - Default -> LEAN
2. Escalation Carryover: If [PREVIOUS] step escalated/failed, upgrade [CURRENT] step context level accordingly.
3. Java Method ('jm'): Set true ONLY if an explicit custom Java method name (e.g. assertCalculation) is specified; false for natural language descriptions.
4. Step Splitting ('sp'):
   - Omit if single action.
   - DO NOT split prerequisite flows (e.g. wait before store, hover before click, focus before type).
   - DO split independent sequential actions (e.g. "Type user, type pass, click Login" -> ["Type user", "type pass", "click Login"]).
   - Preserve Conditional Branches: Keep conditional blocks ("If/When ... else ...") as a single unsplit step. Split only independent actions before or after the conditional block.
