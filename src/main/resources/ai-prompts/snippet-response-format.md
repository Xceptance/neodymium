## Response Format
Return your response as a valid JSON object with this EXACT structure.
CRITICAL INSTRUCTION: Do not include markdown code blocks like ```json ... ```. Output the raw JSON string starting with { and ending with } in a MINIFIED, single-line format with NO unnecessary whitespace (no newlines, no indentation, and minimal spacing) to reduce token consumption. Ensure there are no trailing commas.
CRITICAL ARRAY FORMATTING: If your response requires multiple actions, you MUST create a distinct, separate JSON object for EACH action inside the "a" array. Separate them properly with `}, {`. NEVER combine multiple actions into a single object by duplicating keys.
{
  "s": true/false,
  "a": [
    {
      "t": "ACTION_TYPE",
      "tg": "locator string (prefer id or data-neo-ref)",
      "fr": "(optional) the frameId attribute of the element, if present in the DOM representation",
      "v": "only if action requires it (omit this key entirely if not needed)",
      "desc": "what this does",
      "ed": "(mandatory) text, name, id OR a short description of the targeted element (especially important if data-neo-ref is used)",
      "c": [ "... nested actions for BRANCH type (optional) ..." ],
      "th": [ "... nested actions for BRANCH type (optional) ..." ],
      "el": [ "... nested actions for BRANCH type (optional) ..." ]
    }
  ],
  "d": true/false,
  "e": "only if s is false — describe what went wrong (omit this key entirely if s is true)",
  "r": "Brief explanation of your analysis and decisions"
}
