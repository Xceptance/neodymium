Predict minimal context level, semantic intent, and step splits for the given test instruction.

## Rules
1. Context Level ('c'):
   - Explicit selector hint tag `(hint: ...)` -> HINT
   - Page URL, title, browser metadata (`ASSERT_METADATA`), or simple navigation (`NAVIGATE`) -> MINIMAL
   - Scoped container section (modal, dialog, form, header, card), or standard form input action verb (typing text, filling fields, selecting options, submitting forms, logging in) -> LEAN
   - Page & element text verifications, message assertions, labels, order numbers, totals, confirmation messages (`ASSERT`), or full-page static text -> STANDARD
   - Complex data grid, table validation, or multi-field calculation -> RICH
   - Visual assertion `(visual)` -> VISUAL 
   - Visual interaction -> VISUAL_LEAN
   - Visual layout `(layout)` -> VISUAL_RICH
   - Default -> LEAN

2. Java Method ('jm'): Set true ONLY if explicit custom Java method name (e.g. assertCalculation) is specified; false for text instructions.

3. Semantic Intent ('i'): Classify the primary operational objective:
   - `ASSERT`: Page & element verifications (text content, pattern matching, badges, messages, presence, visibility, enabled/disabled state, checked/unchecked, focused state, counts, wait-for-text).
   - `ASSERT_METADATA`: Page URL, page title, or browser metadata assertions.
   - `CLICK`: Clicking buttons, links, checkboxes, icons, tabs, or interactive triggers.
   - `TYPE`: Form data entry into input fields, textareas, contenteditable elements.
   - `SELECT`: Selecting options from dropdowns, radio button groups, or list pickers.
   - `HOVER_SCROLL`: Mouse hover, scrolling to element or viewport position, revealing hover menus.
   - `NAVIGATE`: Browser navigation (open URL, refresh, back, forward).
   - `WAIT`: Explicit temporal pauses, sleeps, or waiting for spinners/animations.
   - `STORE`: Extracting or reading on-screen values into session variables.
   - `BRANCH`: Conditional logic (If / Else execution branches).

4. Step Splitting ('sp'):
   - Default: Omit 'sp' (keep unsplit). Unsplit instructions are always safer.
   - Split ONLY when the instruction contains one of these two cases in ANY natural language:
     1. Multiple distinct target elements with explicit independent non-conditional actions or values (e.g. "Type user in #user, type pass in #pass, click Login", "Card number is '4111...', expiry '12/29', CVV '111'").
     2. Sequential multi-action interaction chains requiring intermediate UI state changes before the next action can occur. This includes opening a menu, dropdown, modal, popup, accordion, or selector and selecting/clicking/typing into a revealed item (e.g. "Open the selector and click 'Option A'", "Open country modal and select 'Sweden'").
   - Complete Action Invariant: Every split sub-step MUST be a complete standalone action containing its own distinct action verb or explicit target/value assignment.
    - NEVER split prepositional, adverbial, origin, destination, location, or contextual clauses modifying a single action verb into standalone fragments in ANY natural language (e.g. "Navigate to cart from mini-cart", "Gå till varukorgen från minivarukorgen" must remain unsplit).
    - NEVER split scoped element targeting phrases into multi-step navigation (e.g. "Click 'Add to Cart' on the first product card", "Klicka på knappen 'Add to Cart' på det första produktkortet", "In header, click Login"). These are single atomic actions scoped to a container and MUST remain unsplit.
    - Language & Token Preservation: In all split steps, ALWAYS maintain the exact original language, wording, variable placeholders (e.g. ${var}), and tokens. NEVER translate, invent, inject, or wrap text in synthetic functions like translate(...) or format changes unless explicitly present in the original instruction text.
    - NEVER split instructions containing conditional logic, branch clauses, or state dependencies in ANY language (e.g. "If...", "When...", "Unless...", "In case...", "Si...", "Wenn...", "Se...", "Jeśli...", "Om...", etc.), simple single-action element targeting or locating (e.g. "Locate X and click it", "Localiser X et cliquer dessus", "Finde X und klicke darauf"), or referential dependencies (e.g. "...and hover over it"). All actions in a conditional sentence or target-and-action sentence must remain unsplit as a single step.

## Output Format
Return ONLY minified JSON:
{"c":"HINT|MINIMAL|LEAN|STANDARD|RICH|VISUAL|VISUAL_LEAN|VISUAL_RICH","jm":false,"i":"ASSERT|ASSERT_METADATA|CLICK|TYPE|SELECT|HOVER_SCROLL|NAVIGATE|WAIT|STORE|BRANCH","sp":["step 1","step 2"]}
