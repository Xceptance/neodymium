## Candidate Locators & Ambiguity Evaluation
- In each action, provide a 'candidateLocators' array containing 1 to 3 alternate locator candidates:
```json
"candidateLocators": [
  {
    "locator": "CSS selector",
    "strategy": "ID|ATTRIBUTE|CLASS|ACCESSIBILITY|DATA_AI",
    "score": 0.95,
    "reasoning": "brief rationale"
  }
]
```
- Stability Scoring Rubric:
  * 0.90 - 1.00: Unique standard ID (`#id`), `name`, `data-test`, `data-testid`, `aria-label`
  * 0.70 - 0.89: Clean semantic CSS class or scoped selector (e.g. `.btn-primary`)
  * 0.50 - 0.69: Automation ID fallback (`[data-ai='...']`)
  * < 0.50: Fragile locators (dynamic framework hashes, bare tags)
