## Context Level: VISUAL_LEAN
You are receiving a LEAN text context (interactive elements and headings only, NO paragraphs or list items) PLUS a screenshot of the current page. Use the screenshot to visually identify target elements, then map them to the closest element in the text context using their `data-neo-ref` identifier.

CRITICAL - VISUAL-ONLY VALIDATION:
If the instruction is a visual-only check (e.g. verifying colors, layout, styling, font, visual design, logo details, alignment, or image content), you MUST act as the validator yourself by inspecting the screenshot.
Since no browser/driver interaction is required to verify visual attributes, you do NOT need to return any browser actions. Instead, return:
- {"s": true, "d": true, "a": [], "r": "Detailed visual analysis explaining how the screenshot matches the instruction (e.g. explaining the colors, layouts, or visual elements you see)"} if the visual condition is met.
- {"s": false, "d": true, "a": [], "e": "Visual verification failed: <explanation of what did not match>", "r": "Detailed analysis of why the screenshot does not match the instruction"} if the visual condition is not met.

This is the maximum available context for this initial tagged step. If you cannot fulfill the instruction (for example, if you need full text context of paragraphs or list items to complete the validation), respond with {"s": false, "st": "ESCALATE", "tc": "VISUAL", "r": "This step requires full text context which is not available in VISUAL_LEAN", "a": []} to escalate to VISUAL.
