Predict minimal context level and metadata for the current step.

## Context Levels
- HINT: Explicit locator provided (hint: .selector).
- MINIMAL: Ultra-compact default for standard clicks/types (interactive controls + form containers only).
- LEAN: Includes structural layout wrappers and component skeletons.
- STANDARD: Standard static text nodes and paragraph checks.
- RICH: Enhanced DOM with all data-*/aria-* attributes and deep parent context.
- VISUAL: Pure visual check/assertion without element interaction (0 DOM elements + screenshot).
- VISUAL_LEAN: Visual element interaction required (needs screenshot + LEAN element locators).
- VISUAL_RICH: Full RICH DOM + screenshot (maximum context).

## Output Format
Return ONLY minified JSON (no markdown blocks, preambles, or extra text):
{
  "c": "HINT|MINIMAL|LEAN|STANDARD|RICH|VISUAL|VISUAL_LEAN|VISUAL_RICH",
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
   - Structural container / component boundary target -> LEAN
   - Default -> MINIMAL
2. Escalation Carryover: If [PREVIOUS] step escalated/failed, upgrade [CURRENT] step context level accordingly.
3. Java Method ('jm'): Set true ONLY if an explicit custom Java method name (e.g. assertCalculation) is specified; false for natural language descriptions.
4. Step Splitting ('sp'):
   - DEFAULT: Omit 'sp' (keep unsplit) by default. Unsplit instructions are always safer than over-split instructions.
   - FORBIDDEN (Single Target Flow): NEVER split instructions that locate or target a single element or object (e.g. "Locate X and [action]", "Find Y and [action]", "Select Z and [action]"). These describe a single continuous interaction flow on one target.
   - FORBIDDEN (Referential Dependency): NEVER split an instruction if any downstream clause depends on context, nouns, or targets established in an earlier clause.
   - ALLOWED ONLY (Multiple Explicit Targets): Split ONLY when the instruction contains multiple distinct target elements with explicit, independent actions for each target (e.g. "Type user into #username, type pass into #password, click Login" -> ["Type user into #username", "type pass into #password", "click Login"]).
   - Preserve Conditional Branches: Keep conditional blocks ("If/When ... else ...") as a single unsplit step.
