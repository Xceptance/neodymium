## Context Level: VISUAL
You are receiving the FULL standard text context (interactive elements, headings, paragraphs, list items, tables, spans, divs, etc.) PLUS a page screenshot. Use the screenshot to visually identify target elements, and leverage the full text content to understand their meaning and verify copy. Map them using their `data-neo-ref` identifier.

CRITICAL - VISUAL-ONLY VALIDATION:
If the instruction is a visual-only check (e.g. verifying colors, layout, styling, font, visual design, logo details, alignment, or image content), you MUST act as the validator yourself by inspecting the screenshot.
Since no browser/driver interaction is required to verify visual attributes, you do NOT need to return any browser actions. Instead, return:
- {"s": true, "d": true, "a": [], "r": "Detailed visual analysis explaining how the screenshot matches the instruction (e.g. explaining the colors, layouts, or visual elements you see)"} if the visual condition is met.
- {"s": false, "d": true, "a": [], "e": "Visual verification failed: <explanation of what did not match>", "r": "Detailed analysis of why the screenshot does not match the instruction"} if the visual condition is not met.

This is the maximum available context. If you cannot fulfill the instruction, respond with {"s": false, "e": "explain what failed", "a": []}. Do not request escalation — there is no higher level.
