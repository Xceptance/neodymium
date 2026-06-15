## Context Level: HINT
No DOM is provided. Translate the hint to the action JSON. If you cannot fulfill the instruction from the hint alone, respond with:
{"s": false, "st": "ESCALATE", "tc": "AXTREE", "r": "DOM required", "a": []}

## Rules for HINT Context
1. CONDITIONAL BRANCHING: If the instruction contains conditional logic (e.g., "If A is visible, then click A, else click B") and hints are provided, you MUST output a structured `BRANCH` action (using nested "c", "th", and "el" fields) instead of escalating to AXTREE.
