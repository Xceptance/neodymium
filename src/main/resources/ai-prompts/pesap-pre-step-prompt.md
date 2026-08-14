Predict minimal context level and metadata for the current step.

## Rules
1. Context Level ('c'):
   - Explicit selector hint tag `(hint: ...)` -> HINT
   - Element state or presence assertion (focused, checked, unchecked, disabled, enabled, selected, readonly, editable, present, absent, exists, visible) -> MINIMAL
   - Scoped container section (modal, dialog, form, header, card) or standard form input action verb (typing text, filling fields, selecting options, submitting forms, logging in) -> LEAN
   - General text validation, heading check, or pattern string assertion -> STANDARD
   - Complex data grid, table validation, or multi-field calculation -> RICH
   - Visual assertion `(visual)` -> VISUAL 
   - Visual interaction -> VISUAL_LEAN
   - Visual layout `(layout)` -> VISUAL_RICH
   - Default -> MINIMAL
   - Escalation Carryover: If [PREVIOUS] step escalated/failed, upgrade [CURRENT] level accordingly.

2. Java Method ('jm'): Set true ONLY if explicit custom Java method name (e.g. assertCalculation) is specified; false for text instructions.

3. Step Splitting ('sp'):
   - Default: Omit 'sp' (keep unsplit). Unsplit instructions are always safer.
   - Split ONLY in these two cases:
     1. Multiple distinct target elements with explicit independent non-conditional actions or values (e.g. "Type user in #user, type pass in #pass, click Login" or "Card number is '4111...', expiry '12/29', CVV '111'").
     2. Sequential multi-action interaction chains requiring intermediate UI state changes before the next action can occur (e.g. "Locate a product card, click translate('Add to Cart'), and choose an available size" -> ["Locate a product card and click translate('Add to Cart')", "Choose an available size"]).
   - NEVER split instructions containing conditional logic, branch clauses, or state dependencies in ANY language (e.g. "If...", "When...", "Unless...", "In case...", "Si...", "Wenn...", etc.), simple single-action element targeting or locating ("Locate X and click it", "Localiser X et cliquer sur Y", "Finde X und klicke Y"), or referential dependencies ("...and hover over it"). All actions in a conditional sentence or target-and-action sentence must remain unsplit as a single step.

## Output Format
Return ONLY minified JSON:
{"c":"HINT|MINIMAL|LEAN|STANDARD|RICH|VISUAL|VISUAL_LEAN|VISUAL_RICH","jm":false,"sp":["step 1","step 2"]}
