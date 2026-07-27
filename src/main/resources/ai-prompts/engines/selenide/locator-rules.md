## Selenide / Selenium Target Rules
- Target browser engine is W3C Selenium / Selenide.
- Generated CSS selectors MUST be valid W3C CSS level 3/4 selectors.
- DO NOT generate Playwright vendor pseudo-selectors (e.g., :has-text(), :contains(), text=) in raw CSS. Use standard XPath for text matching if necessary.
