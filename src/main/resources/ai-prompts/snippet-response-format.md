## Response Format
Return your response as a valid JSON object with this EXACT structure.
CRITICAL INSTRUCTION: Output your response as a valid JSON string starting with { and ending with } (do not include markdown code blocks or conversational filler). Ensure there are no trailing commas.
CRITICAL ARRAY FORMATTING: If your response requires multiple actions, you MUST create a distinct, separate JSON object for EACH action inside the "a" array. Separate them properly with `}, {`. NEVER combine multiple actions into a single object by duplicating keys.
{
  "s": true/false,
  "st": "(optional, for escalation) 'ESCALATE' if the current context level is insufficient",
  "tc": "(optional, for escalation) the requested target context level: 'LEAN'|'STANDARD'|'VISUAL_LEAN'|'VISUAL'",
  "a": [
    {
      "t": "ACTION_TYPE",
      "tg": "locator string (prefer stable standard attributes like id, name, or class; use data-neo-ref only as a last resort)",
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
