## Selenide/Selenium Engine Locators
- Supported locator formats for targeting elements:
  1. Standard CSS & Playwright Extended CSS:
     - Standard: `#id`, `.class`, `tag[attr='value']`.
     - Transparent Shadow DOM: All CSS, text, role, and attribute locators automatically and transparently pierce open Shadow DOM boundaries in document order. You do NOT need special shadow piercing syntax like `::shadow` (legacy `::shadow` remains supported for compatibility).
  2. Playwright Text Matchers & Pseudo-Classes:
     - Exact text match: `tag:text-is("exact text")`, `tag:exact-text("exact text")`, or `text="exact text"`.
     - Substring text match: `tag:has-text("text")`, `tag:contains("text")`, or `text*="text"`.
     - Text engine prefix / shorthands: `text="exact text"`, `"exact text"`, `'exact text'`, or regex `/pattern/flags`.
     - Element visibility: `tag:visible` or `visible=true` (matches elements that are displayed with non-zero dimensions).
     - Subtree matching: `tag:has(subSelector)`.
     - Positional matching: `:nth-match(subSelector, index)` (1-based index).
     - Layout matching: `:left-of(selector[, distance])`, `:right-of(selector[, distance])`, `:above(selector[, distance])`, `:below(selector[, distance])`, `:near(selector[, distance])`.
  3. Playwright Semantic & Accessible Role Selectors:
     - Role and accessible name: `role=button[name="Submit Order"]`, `role=link[name="Checkout"]`, `role=textbox[name="Email"]`.
     - Supported role options: `name="text"`, `checked=true|false`, `disabled=true|false`, `level=N`, `include-hidden=true`.
     - Label association: `label="Form Field Label"` (targets the associated `<input>`, `<select>`, or `<textarea>`).
     - Test ID & Attributes: `id=foo`, `placeholder="Enter email"`, `alt="Logo"`, `title="Tooltip"`, `data-testid="user-input"`.
  4. Standard XPath:
     - `//tag[...]` or `xpath=//tag[...]` (e.g. `//button[normalize-space()='S']`).
     - Note: XPath strictly adheres to W3C DOM and does NOT pierce shadow roots. Use CSS/Playwright locators when targeting elements inside shadow DOM.
  5. Automation Reference ID shorthand:
     - `#xchexid`, `xchexid`, or `[data-ai='xchexid']` (valid fallback when stamped on page).
  6. Playwright Chained Locators (`>>`):
     - Hierarchical filtering: `container >> child` (e.g. `#users-table >> tr:has-text("Alice") >> button.btn-edit`).
     - Sub-locator capturing: Prefix a clause with `*` to capture that step in the chain instead of the terminal element (e.g. `*li:has(button.delete) >> button.delete`).
     - Zero-based ordinal targeting: `selector >> nth=N` (0 is 1st, 1 is 2nd; e.g. `ul >> li >> nth=2` selects 3rd list item, `.card >> nth=0` selects 1st card).
     - Negative ordinal targeting: `selector >> nth=-1` selects the last element (`nth=-2` is second to last).
     - Preference rule: Always prefer relational anchors (`tr:has-text("Alice") >> ...`) over raw positional ordinals (`nth=N`) when text/label context exists, to ensure tests remain stable across table sorting, insertions, and deletions.
     - Table/Grid Rows: When targeting the Nth row of a table or grid (e.g. "third row in table"):
       * In HTML tables: NEVER use bare `tr >> nth=N` or `tbody >> tr >> nth=N` because the header row may be inside `tbody` (especially in legacy tables lacking `<thead>`). ALWAYS target data rows containing cells: use `table >> tr:has(td) >> nth=N`, `tbody >> tr:has(td) >> nth=N`, or `[class*="row"]:not([class*="header"]) >> nth=N` so that column headers (`th`) are never counted as data rows.
       * In ARIA grids/tables: NEVER use bare `[role="row"] >> nth=N` because column header rows also use `role="row"`. ALWAYS target data rows: use `[role="table"] [role="row"]:has([role="cell"]) >> nth=N`, `[role="grid"] [role="row"]:has([role="gridcell"]) >> nth=N`, `[role="rowgroup"]:not(.header) [role="row"] >> nth=N`, or `.data-row >> nth=N` so that column header rows are excluded.
