# [DEF-20261015-1825] Chained :has-text Selector Resolves First Item in Container Instead of Target Row

- **Status:** `Resolved`
- **Opened:** 2026-10-15 18:25
- **Closed:** 2026-10-15 18:35
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In `VirtualizedListSandboxLiveTest.testVirtualizedListLive`, executing `click(selector: 'div:has-text("Product #42") >> button:has-text("Select")')` clicked `<button id="select-product-38">` instead of `<button id="select-product-42">`, selecting Product #38 and causing subsequent verification of `#selected-product-status` to fail.
- **Root Cause:** In `playwright-selector-evaluator.js`, `queryCssSingleRoot` did not prune ancestor elements matching `:has-text(...)`. Because parent containers (e.g. `<div id="virtual-list-container">` and `<div id="virtual-content">`) precede child elements in document order and contain the target text via descendant inheritance, the outer container was matched first and passed as the primary node in `currentNodes`. Evaluating the second clause `button:has-text("Select")` against the outer container resolved to the first button in the entire container (Product #38) rather than the button inside Product #42's row.
- **Detection Gap ("What did we miss?"):** Existing chained selector tests evaluated distinct tag names across hierarchy levels (`table >> tr:has-text(...) >> button`), but lacked tests where an outer container shares the same tag (`div >> div >> button`) with multiple sibling rows containing identical child buttons.
- **Resolution:**
  1. Updated the chained `>>` execution loop in `playwright-selector-evaluator.js` to perform productive-node ancestor filtering between adjacent clauses (`clauses[i] >> clauses[i+1]`). For candidate nodes that match `clauses[i]`, the evaluator tests which nodes yield non-empty results for `clauses[i+1]`. Among these productive containers, any ancestor container enclosing another productive container is pruned using `composedContains`.
  2. Preserved intermediate container matching in `queryCssSingleRoot` so that multi-level DOM subtrees (where an item container wraps a text label child) do not prematurely discard the item container before child element queries (`>> button`, `>> input`) are evaluated.
- **Safety Net Added:** Added `PlaywrightSelectorEvaluatorBrowserTest.testChainedHasTextNestedContainersExcludeParent()` verifying that `div:has-text("Product #42") >> button:has-text("Select")` strictly resolves only Product #42's button (excluding ancestor viewport buttons like `#select-product-38`), and that captured selectors `*div:has-text(...) >> button` capture the item container `#item-42`.
