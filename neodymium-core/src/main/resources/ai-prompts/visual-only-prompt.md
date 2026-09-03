Analyze the visual screenshot to fulfill the active test instruction.

## Execution Guidelines
1. **Visual Evaluation**:
   - Inspect the screenshot to verify appearance, layout, alignment, visibility, colors, or presence of components against the instruction (including localized text).
   - If satisfied: return `status: "SUCCESS"`, `assertionSatisfied: true`, and `actions: []`.
   - If violated or missing: return `status: "FAILED"`, `assertionSatisfied: false`, and `actions: []`.

2. **Escalation**:
   - If ambiguous, or if the instruction requires element interaction (clicking, typing) or detailed DOM inspection, return `status: "ESCALATE"` with `targetContextLevel: "[NEXT_ESCALATION]"` (e.g. `VISUAL_LEAN` or `VISUAL_RICH`).

3. **Direct Navigation**:
   - If the instruction is a direct URL navigation (e.g. "Open https://..."), emit a single `NAVIGATE` action with `locator` set to the target URL.
   - All other interactive DOM actions are forbidden at this visual level.

## Response Format
Return ONLY a raw JSON object:
{
  "reasoning": "Concise step-by-step visual analysis of the screenshot",
  "assertionSatisfied": true,
  "status": "SUCCESS|FAILED|ESCALATE",
  "targetContextLevel": "[NEXT_ESCALATION] if escalating, otherwise VISUAL",
  "actions": []
}

