# [DEF-20260930-10] Multi-Turn query_dom Overhead Caused by Turn 1 DOM Pruning, Ancestor Container Bubbling, and Missing ContextLevel Overrides

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `neodymium-core` (`ai-tool`, `ai-model`, `ai-pipeline`, `ai-config`)
- **Scope:** `Framework`
- **Symptom:** In test executions (such as `AddToCartTest.executeAddToCart`), verification steps targeting non-interactive elements (e.g. "Verify the estimated total is displayed") took 3 LLM turns and 2 redundant `query_dom` calls. In Turn 1, the DOM was omitted under `LEAN` mode; in Turn 2, `query_dom({"text":"total"})` suffered from container bubbling where outer layout wrappers (`div.page`, `div#maincontent`, etc.) consumed all 10 match slots and pushed the leaf match past the limit; in Turn 3, a narrower query `query_dom({"text":"Estimated Total"})` finally resolved the selector.
- **Root Cause:**
  1. Default `ContextLevel.LEAN` intentionally strips static copy and non-interactive text wrappers to minimize token consumption, leaving the agent without leaf copy targets on Turn 1 when verifying text.
  2. `PlaybookStep` lacked parsing for step-level `(contextlevel=...)` control tags, and `AiConfiguration` lacked a global `neodymium.ai.contextLevel` property to allow suites or steps to select `STANDARD` or `RICH` DOM fidelity when verifying complex copy.
  3. `BrowserToolProvider.query_dom` used a low default limit of 10 elements and traversed DOM nodes in document pre-order without container de-bubbling (suppressing matching ancestors when child elements match) or sorting matches by shortest text length, allowing large outer containers to crowd out leaf matches.
- **Detection Gap ("What did we miss?"):** Previous unit tests for `query_dom` tested mocked elements or small isolated snippets without deeply nested ancestor hierarchies, failing to reveal that pre-order DOM queries match parent containers ahead of leaf nodes and exhaust the return limit.
- **Resolution:**
  1. Implemented step-level `(contextlevel=<level>)` control tag parsing in `PlaybookStep` supporting both `=` and `:` syntax, stripping it from natural language instructions.
  2. Added `neodymium.ai.contextLevel` configuration in `AiConfiguration` with default `LEAN`.
  3. Updated `ExecuteActionsStep` to resolve `initialLevel` from `AiConfiguration` and allow step-level `(contextlevel=...)` tags to take final override precedence.
  4. Enhanced `BrowserToolProvider.query_dom` to suppress ancestor containers when matching descendant elements exist (`cand.contains(other)`), sort surviving matches by text length ascending (placing innermost leaf targets first), and increased the default element limit from 10 to 20.
  5. Updated Operating Rule 2 in `AgentToolLoopStep` to instruct agents to use distinctive multi-word phrases or specific selectors from the instruction when invoking `query_dom`.
  6. Documented `(contextlevel=<level>)` and `neodymium.ai.contextLevel` across `ai.properties`, `doc/DOCUMENTATION.md`, and `doc/AI_EXECUTION_SUMMARY.md`.
- **Safety Net Added:** Added unit test `PlaybookStepTest.testContextLevelTagParsing` for `(contextlevel=...)` variations, `BrowserToolsTest.testBrowserQueryDomToolSchema` verifying limit 20, and `BrowserToolsTest.testBrowserQueryDomAncestorSuppressionAndSorting` asserting ancestor container suppression and leaf-first sorting against nested DOM hierarchies.
