## Selenide/Selenium Engine Locators
- Supported locator formats for targeting elements:
  1. Standard CSS Selectors: `#id`, `.class`, `tag[attr='value']`.
  2. Neodymium Text Pseudo-Selectors (natively translated to Selenide/Selenium):
     - Exact text match: `tag:text-is("exact text")` or `tag:exact-text("exact text")` (e.g. `button:text-is("S")` or `.quick-add-dropdown.active button:text-is("S")`).
     - Substring text match: `tag:has-text("text")` or `tag:contains("text")`.
     - Text prefix: `text="exact text"`.
  3. Standard XPath: `//tag[...]` (e.g. `//button[normalize-space()='S']`).
  4. Automation Reference ID shorthand: `[data-ai='...']` (valid fallback when no standard or text-anchored selector exists).
  5. Playwright Chained Locators (`>>`):
     - Hierarchical filtering: `container >> child` (e.g. `#users-table >> tr:has-text("Alice") >> button.btn-edit`).
     - Zero-based ordinal targeting: `selector >> nth=N` (0 is 1st, 1 is 2nd; e.g. `ul >> li >> nth=2` selects 3rd list item, `.card >> nth=0` selects 1st card).
     - Negative ordinal targeting: `selector >> nth=-1` selects the last element (`nth=-2` is second to last).
     - Preference rule: Always prefer relational anchors (`tr:has-text("Alice") >> ...`) over raw positional ordinals (`nth=N`) when text/label context exists, to ensure tests remain stable across table sorting, insertions, and deletions.
     - Table/Grid Rows: When targeting the Nth row of a table or grid (e.g. "third row in table"):
       * In HTML tables: NEVER use bare `tr >> nth=N` or `tbody >> tr >> nth=N` because the header row may be inside `tbody` (especially in legacy tables lacking `<thead>`). ALWAYS target data rows containing cells: use `table >> tr:has(td) >> nth=N`, `tbody >> tr:has(td) >> nth=N`, or `[class*="row"]:not([class*="header"]) >> nth=N` so that column headers (`th`) are never counted as data rows.
       * In ARIA grids/tables: NEVER use bare `[role="row"] >> nth=N` because column header rows also use `role="row"`. ALWAYS target data rows: use `[role="table"] [role="row"]:has([role="cell"]) >> nth=N`, `[role="grid"] [role="row"]:has([role="gridcell"]) >> nth=N`, `[role="rowgroup"]:not(.header) [role="row"] >> nth=N`, or `.data-row >> nth=N` so that column header rows are excluded.
