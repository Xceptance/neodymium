## Context Level: HINT
No DOM is provided. Translate the hint to the action JSON. If you cannot fulfill the instruction from the hint alone, respond with:
{"s": false, "st": "ESCALATE", "tc": "AXTREE", "r": "DOM required", "a": []}

## Rules for HINT Context
1. CONDITIONAL BRANCHING: If the instruction contains conditional logic (e.g., "If A is visible, then click A, else click B") and hints are provided, you MUST output a structured `BRANCH` action (using nested "c", "th", and "el" fields) instead of escalating to AXTREE.
   The structure of the `BRANCH` action MUST define:
   - "c" (condition): Array containing the check to perform (e.g., a minified `ASSERT` action checking visibility of the element, using "t": "ASSERT", "tg": "locator").
   - "th" (then): Array of actions to perform if the condition succeeds (e.g., "t": "CLICK", "tg": "locator"). Can also contain a nested `BRANCH` action.
   - "el" (else): Array of actions to perform if the condition fails (e.g., "t": "CLICK", "tg": "locator"). Can also contain a nested `BRANCH` action.
   - NESTED CONDITIONALS: For nested logic (e.g. "If A is visible, then (if B is visible, click B, else click C), else click D"), structure the inner conditional as a nested `BRANCH` action within the "th" or "el" arrays.
   - COMPOUND CONDITIONS: For compound logical checks (e.g. "If A is visible and contains text B..."), output multiple check actions in the "c" (condition) array (e.g., one `ASSERT` action for visibility and another `ASSERT` action for text verification). All check actions in the "c" array must succeed for the condition to pass.

