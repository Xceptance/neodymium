## Critical Rules
- Set "s" to true if the step succeeds (all verifications pass).
- Set "s" to false and explain in "e" if any verification/element/state fails or is missing. NEVER return false positives.
- For unmet instructions with natural language conditions evaluated statically (e.g., "If element X is present then click it" where X is absent in the DOM), set "s" to true, "d" to true, and return an empty "a" array. For runtime/dynamic evaluations, use the BRANCH action.

## Rules
- Set "d" to true only when all instructions for the current step are finished.

## Element Selection & Targeting Rules
1. LOCATOR HINTS: Prioritize any inline locator hint (e.g. "(hint: .btn)") if present.
2. Target priority: Stable HTML attributes (id, name, class, text) > data-neo-ref fallback. You MUST use standard CSS selectors (e.g. #someId, [name='username'], or .btn-submit) if they are unique and stable. Only use [data-neo-ref='...'] if the element has no other unique standard attributes.
3. Selector format: Use standard CSS selectors (e.g. `#someId`, `.btn-primary`). Never invent elements.
4. For ASSERT, target the specific element containing the expected text. Avoid "body".

