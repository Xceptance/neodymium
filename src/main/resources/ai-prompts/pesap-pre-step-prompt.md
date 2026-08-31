Predict minimal context level and metadata for the given test instruction.

## Rules
1. Context Level ('c'):
   - Explicit selector hint tag `(hint: ...)` -> HINT
   - Element state, property/attribute value, metadata, or presence assertion -> MINIMAL
   - Scoped container section (modal, dialog, form, header, card) or standard form input action verb (typing text, filling fields, selecting options, submitting forms, logging in) -> LEAN
   - Page content text validation, headings, article body copy, or search results -> STANDARD
   - Complex data grid, table validation, or multi-field calculation -> RICH
   - Visual assertion `(visual)` -> VISUAL 
   - Visual interaction -> VISUAL_LEAN
   - Visual layout `(layout)` -> VISUAL_RICH
   - Default -> MINIMAL

2. Java Method ('jm'): Set true ONLY if explicit custom Java method name (e.g. assertCalculation) is specified; false for text instructions.

3. Step Splitting ('sp'):
   - Default: Omit 'sp' (keep unsplit). Unsplit instructions are always safer.
   - Split ONLY when the instruction contains one of these two cases in ANY natural language:
     1. Multiple distinct target elements with explicit independent non-conditional actions or values (e.g. "Type user in #user, type pass in #pass, click Login", "Card number is '4111...', expiry '12/29', CVV '111'", or localized equivalents).
     2. Sequential multi-action interaction chains requiring intermediate UI state changes before the next action can occur. This includes opening a menu, dropdown, modal, popup, accordion, or selector and selecting/clicking/typing into a revealed item (e.g. "Open the selector and click 'Option A'" -> ["Open the selector", "Click 'Option A'"], "Öffne die Auswahlliste und klicke auf 'Option A'" -> ["Öffne die Auswahlliste", "Klicke auf 'Option A'"], "Ouvrir le menu et cliquer sur 'Option A'" -> ["Ouvrir le menu", "Cliquer sur 'Option A'"], "Locate a product card, click 'Add to Cart', and choose an available size" -> ["Locate a product card and click 'Add to Cart'", "Choose an available size"]).
   - Language & Token Preservation: In all split steps, ALWAYS maintain the exact original language, wording, variable placeholders (e.g. ${var}), and tokens. NEVER translate, invent, inject, or wrap text in synthetic functions like translate(...) or format changes unless explicitly present in the original instruction text.
   - NEVER split instructions containing conditional logic, branch clauses, or state dependencies in ANY language (e.g. "If...", "When...", "Unless...", "In case...", "Si...", "Wenn...", "Se...", "Jeśli...", "Om...", etc.), simple single-action element targeting or locating (e.g. "Locate X and click it", "Localiser X et cliquer dessus", "Finde X und klicke darauf"), or referential dependencies (e.g. "...and hover over it"). All actions in a conditional sentence or target-and-action sentence must remain unsplit as a single step.

## Output Format
Return ONLY minified JSON:
{"c":"HINT|MINIMAL|LEAN|STANDARD|RICH|VISUAL|VISUAL_LEAN|VISUAL_RICH","jm":false,"sp":["step 1","step 2"]}
