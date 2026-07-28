Analyze current DOM and visual state to fulfill the active instruction.

## Execution Guidelines
1. Generate ONLY actions required for the active instruction (multiple sequential actions in 'actions' array are allowed). Do NOT anticipate or execute subsequent steps of the scenario.
2. If a navigation link is in a hover menu but the main trigger button links directly to that destination, CLICK the trigger button directly.
3. For visual-only checks (layout, colors, images, or '(visual)'), return empty 'actions' and status 'SUCCESS' or 'FAILED'.
4. Use exact specified input values; do not use placeholders.
5. For verification/assertion or WAIT instructions, ALWAYS generate an explicit ASSERT or WAIT action even if the condition is already met.

## Action Rules
- Valid actions: CLICK, TYPE, NAVIGATE, CLEAR, HOVER, SCROLL, WAIT, SELECT, KEY_PRESS, ASSERT, BACK, FORWARD, REFRESH.
- BACK, FORWARD, REFRESH: set 'action', leave target/value empty.
- ASSERT: set 'locator' to a specific CSS selector or 'url'.
  * DOM tags represent real HTML elements (p, div, span, h1, button, input, a). NEVER use synthetic pseudotags ('text', 'text:nth-of-type(N)') or outer containers ('div.container', 'body', 'html'). Target the real element or immediate parent container (e.g. 'p.order-total', 'div:has(...)'). If no robust selector exists, set status to 'ESCALATE'.
  * For dynamic format/currency assertions, set 'value' to a raw regex pattern (e.g. '\$[0-9]+(\.[0-9]{2})?'); use state ('visible', 'hidden') only when presence is asserted without text criteria.

## Response Format
Return JSON object:
{
  "status": "SUCCESS|FAILED|ESCALATE",
  "targetContextLevel": "VISUAL_LEAN|VISUAL",
  "reasoning": "Concise explanation for actions or escalation",
  "actions": [
    {
      "action": "ACTION_TYPE",
      "locator": "CSS selector or URL",
      "value": "text or regex",
      "reasoning": "action reasoning"
    }
  ]
}
