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
  "sp": ["First step text", "Second step text"] // Omit this field or set to [] if the step does not require splitting
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
4. Step Splitting: Only split a step into "sp" if it contains multiple mutually independent interaction flows.
   - If the step is not split, omit the "sp" field entirely or set it to an empty array `[]`.
   - DO NOT split when one part of the step is a prerequisite or direct preparation for the next part of the same action flow (e.g., waiting for an element, hovering to reveal, focusing an input before entering text, or storing/asserting on a target element). These must remain as a single step.
     * Examples of UNSPLIT:
       + "Wait for success and store the text" (Wait is preparation for Store)
       + "Hover over menu and click Profile" (Hover is preparation for Click)
       + "Focus input and type admin" (Focus is preparation for Type)
   - DO split if the step contains a sequence of state-changing interactions or unrelated operations.
     * Examples of SPLIT:
       + "Type admin in username, type secret in password, and click Login" -> ["Type admin in username", "type secret in password", "click Login"]
       + "Click Submit and check that the table has 5 rows" -> ["Click Submit", "check that the table has 5 rows"]
   - CRITICAL: Do NOT split the conditional branch itself (e.g., the block from "If" / "When" to the end of the conditional logic). The conditional branch block must remain as a single, unsplit step to preserve its logical structure for the BRANCH action processor. Any sequential actions *outside* the conditional branch block (before or after it) *must* be split off into their own separate steps.
     * Example of splitting a sequence containing a branch:
       Input: "Click the Menu button, and then if the Accept Cookies button is visible, click it, else click the Main Action button, and then verify the success message"
       Split: ["Click the Menu button", "if the Accept Cookies button is visible, click it, else click the Main Action button", "verify the success message"]
