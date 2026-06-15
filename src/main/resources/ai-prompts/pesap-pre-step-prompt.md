Predict minimal DOM context level and semantic metadata for the current web test automation step.

## Context Levels
- "HINT": Zero elements, only prompt-defined locator hints. Use when explicit locators (e.g. (hint: .selector)) are provided.
- "AXTREE": Semantic accessibility tree. Default for standard interaction steps (click, hover, select, type, navigate, clear).
- "STANDARD": Interactive elements + visible text. Use for text validation (messages/values).
- "VISUAL_LEAN": Interactive elements + screenshot. Use for visual/layout checks without text validation.
- "VISUAL": Interactive + visible text + screenshot. Use for visual/layout checks requiring text analysis.

## Output Format
Return ONLY minified JSON:
{
  "c": "HINT|AXTREE|STANDARD|VISUAL_LEAN|VISUAL",
  "jm": true|false,
  "sp": ["First step text", "Second step text"]
}

## Rules
1. Predict the MINIMAL context level (`c`) based on tags or step content:
   - If the step contains "(hint:", predict "HINT".
   - If the step contains "(visual)", predict "VISUAL_LEAN" (or "VISUAL" if text checking is also required).
   - If the step contains "(layout)", predict "VISUAL" (since layout validation generally requires parsing label text to map elements).
   - Otherwise, default to "AXTREE" unless text validation ("STANDARD") is required.
     - Text validation examples requiring "STANDARD":
       * "Verify the message 'Order placed successfully' is displayed"
       * "Check that the total shows '$42.99'"
       * "The error text 'Invalid email' appears below the input"
2. Analyze the `[CURRENT]` step exclusively. `[PREVIOUS]`/`[NEXT]` are for context only.
3. Set "jm" to true ONLY if a custom Java method name (e.g., assertCalculation, validateShippingCost, runPriceCheck) is explicitly identified in the step. Do NOT set to true for natural language descriptions like "verify the calculation" or "check the total".
4. Step Splitting: If the step is compound/multi-stage and the second action depends on page state changes (e.g., "click menu then click Sign out"), split into simple, individual steps in "sp" (one step per page state change).
   - Do NOT split if actions target the same page state without navigation or DOM mutation between them (e.g., filling out multiple fields in the same form).
   - If no split is needed, omit "sp".

