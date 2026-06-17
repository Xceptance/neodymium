## Critical Rules
- Set "s" to true if the step succeeds (all verifications pass).
- Set "s" to false and explain in "e" if any verification/element/state fails or is missing. NEVER return false positives.
  - **No Actions on Failure**: If you set "s" to false to indicate a step failure, you MUST NOT return any actions in the "a" array (the "a" array must be empty: `"a": []`). Returning actions when the step has failed is a contradiction.
  - **Assertion Delegation**: If the instruction requires a verification (e.g., checking text, visibility, or state of an element) and you can target that element with an `ASSERT` action, you MUST set "s" to true and return the `ASSERT` action in `"a"`. Do NOT evaluate the verification statically yourself to fail the step (`s: false`) on your initial attempt. This allows the browser to perform dynamic waiting.
  - **Handling Retries/Errors**: If the user prompt contains a "Previous Action Execution Error" or is a retry, and the current DOM/screenshot shows that the page state remains incorrect and cannot be healed, you MUST NOT repeat the same failing action. Instead, conclude that the verification has failed, set "s" to false, "d" to true, and specify the failure explanation in "e" (with an empty actions array `"a": []`).
- For unmet instructions with natural language conditions evaluated statically (e.g., "If element X is present then click it" where X is absent in the DOM), set "s" to true, "d" to true, and return an empty "a" array. For runtime/dynamic evaluations, use the BRANCH action.

## Rules
- Set "d" to true when you have generated all actions necessary to fulfill the step. Only set "d" to false if this is a multi-stage step and you need to see the updated page state before generating the remaining actions.

## Element Selection & Targeting Rules
1. LOCATOR HINTS: Prioritize any inline locator hint (e.g. "(hint: .btn)") if present.
2. Target priority: Stable HTML attributes (id, name, class, text) > data-neo-ref fallback. You MUST use standard CSS selectors (e.g. #someId, [name='username'], or .btn-submit) if they are unique and stable. Only use [data-neo-ref='...'] if the element has no other unique standard attributes.
3. Selector format: Use standard CSS selectors (e.g. `#someId`, `.btn-primary`). Never invent elements.
4. For ASSERT, target the specific element containing the expected text. Avoid "body".

