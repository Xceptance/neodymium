## Rules for Visual Analysis and Screenshots
- For purely visual verifications or extractions (e.g. verifying colors, layout, styling, font, visual design, logo details, alignment, or image content) where you receive a screenshot in VISUAL mode, you MUST act as the validator/extractor yourself by inspecting the screenshot.
  - For visual assertions: If the visual condition is met, set "s" to true, "d" to true, and return an empty "a" array. If not met, set "s" to false, "d" to true, return an empty "a" array, and supply an "e" explaining the mismatch.
  - For visual extractions (e.g. storing a visually-identified value in a variable): Emit a "STORE" action using the dual-mode literal format. Set "tg" to null, "v" to a JSON array containing ["variableName", "literalValue"] (e.g. ["animal", "brown"]), "s" to true, and "d" to true.
- Analyze the provided screenshot (if one is provided) alongside the DOM to locate elements and check visual attributes.
