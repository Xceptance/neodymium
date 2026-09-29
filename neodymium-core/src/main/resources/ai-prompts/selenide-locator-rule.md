## Selenide/Selenium Engine Locators
- Supported locator formats for targeting elements:
  1. Standard CSS Selectors: `#id`, `.class`, `tag[attr='value']`.
  2. Neodymium Text Pseudo-Selectors (natively translated to Selenide/Selenium):
     - Exact text match: `tag:text-is("exact text")` or `tag:exact-text("exact text")` (e.g. `button:text-is("S")` or `.quick-add-dropdown.active button:text-is("S")`).
     - Substring text match: `tag:has-text("text")` or `tag:contains("text")`.
     - Text prefix: `text="exact text"`.
  3. Standard XPath: `//tag[...]` (e.g. `//button[normalize-space()='S']`).
  4. Automation Reference ID shorthand: `[data-ai='...']` (valid fallback when no standard or text-anchored selector exists).
