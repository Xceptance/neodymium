# [DEF-20261016-1020] CSS Descendant Combinators Crossing Shadow Boundaries Return Zero Matches

- **Status:** `Resolved`
- **Opened:** 2026-10-16 10:20
- **Closed:** 2026-10-16 10:31
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In `WebComponentsSandboxMockTest` and `WebComponentsSandboxLiveTest`, compound CSS selectors containing descendant or child combinators that span shadow boundaries (e.g. `aura-input-field#email-field input` or `aura-account-card .welcome-banner`) fail with `NoSuchElementException: Cannot locate an element using PlaywrightBy`.
- **Root Cause:** In `playwright-selector-evaluator.js`, `evaluateCss` collected shadow roots and ran `querySelectorAll(cssClause)` on each root independently. In the standard DOM API, `root.querySelectorAll(...)` never crosses a `#shadow-root` boundary. When an ancestor (e.g. `<aura-input-field>`) resides in an outer tree and its descendant (e.g. `<input>`) resides inside that element's encapsulated `shadowRoot`, no individual root contains both elements in the same query tree. Additionally, `collectAllRoots(start)` did not inspect `start.shadowRoot`, causing scoped evaluations rooted at shadow hosts to ignore their own shadow roots.
- **Detection Gap ("What did we miss?"):** Prior browser integration tests only verified single-tag or single-class queries within a single shadow root (`button.shadow-login-btn`) or explicit chained selectors (`>>`), lacking compound CSS selectors with combinators crossing from host elements into encapsulated shadow roots.
- **Resolution:** Updated `collectAllRoots` to walk `node.shadowRoot` directly from any starting node, and updated `evaluateCss` to parse CSS branches and combinators (` `, `>`, `+`, `~`), traversing composed DOM trees and stepping into `element.shadowRoot` for descendant (` `) and direct child (`>`) combinators, matching Playwright's native shadow-piercing CSS engine.
- **Safety Net Added:** Added unit and integration tests in `PlaywrightSelectorEvaluatorBrowserTest` (`testCssDescendantCombinatorPiercesShadowBoundary` and `testTripleNestedShadowRootsCombinator`) verifying descendant and child combinator queries crossing single-level, multi-level, and triple-nested shadow DOM boundaries, as well as end-to-end verification via `WebComponentsSandboxMockTest` across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`.
