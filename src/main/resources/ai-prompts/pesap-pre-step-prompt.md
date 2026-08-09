Predict minimal context level and metadata for the current step.

## Rules
1. Context Level ('c'):
   - Explicit locator `(hint: .selector)` -> HINT
   - Visual assertion `(visual)` -> VISUAL | Visual interaction -> VISUAL_LEAN | Visual layout -> VISUAL_RICH
   - Data/table validation -> RICH | Text check -> STANDARD | Component boundary -> LEAN | Default -> MINIMAL
   - Escalation Carryover: If [PREVIOUS] step escalated/failed, upgrade [CURRENT] level accordingly.

2. Java Method ('jm'): Set true ONLY if explicit custom Java method name (e.g. assertCalculation) is specified; false for text instructions.

3. Step Splitting ('sp'):
   - Default: Omit 'sp' (keep unsplit). Unsplit instructions are always safer.
   - Split ONLY if instruction contains multiple distinct target elements with explicit independent non-conditional actions (e.g. "Type user in #user, type pass in #pass, click Login").
   - NEVER split instructions containing conditional logic, branch clauses, or state dependencies in ANY language (e.g. "If...", "When...", "Unless...", "In case...", "Si...", "Wenn...", etc.), single-target flows ("Locate X and [action]"), or referential dependencies ("...and hover over it"). All actions in a conditional sentence must remain unsplit as a single step.

## Output Format
Return ONLY minified JSON:
{"c":"HINT|MINIMAL|LEAN|STANDARD|RICH|VISUAL|VISUAL_LEAN|VISUAL_RICH","jm":false,"sp":["step 1","step 2"]}
