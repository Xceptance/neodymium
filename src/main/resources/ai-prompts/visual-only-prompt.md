Analyze the visual screenshot to fulfill the active test instruction.

## Execution Guidelines
1. **Visual Inspection**:
   - Inspect the attached screenshot to evaluate appearance, layout, alignment, visibility, colors, or presence of components described in the instruction.
   - If the visual condition is satisfied, return `assertionSatisfied: true`, `status: "SUCCESS"`, and empty `actions: []`.
   - If the visual condition is clearly violated or missing, return `assertionSatisfied: false`, `status: "FAILED"`, and empty `actions: []`.

2. **Escalation**:
   - If the instruction requires inspecting detailed DOM text, interacting with elements, or if visual evaluation is ambiguous, set `status: "ESCALATE"` with `targetContextLevel: "[NEXT_ESCALATION]"` (e.g. `VISUAL_LEAN` or `VISUAL_RICH`).

3. **Direct Navigation**:
   - If the instruction is a direct URL browser navigation (e.g. "Open https://..."), emit a single `NAVIGATE` action with `locator` set to the target URL.

4. **Forbidden**:
   - FORBIDDEN: NEVER emit interactive mutating DOM actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`, `KEY_PRESS`, `BRANCH`) at this visual context level since no DOM element structure is available.

## Response Format
Return ONLY a raw JSON object:
{
  "reasoning": "Concise step-by-step visual analysis of the screenshot",
  "assertionSatisfied": true,
  "status": "SUCCESS|FAILED|ESCALATE",
  "targetContextLevel": "[NEXT_ESCALATION] if escalating, otherwise omit or set to VISUAL",
  "actions": []
}
