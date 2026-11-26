# [DEF-20261103-1135] United Container Cross-Filtering Broken by Const Variable Reassignment TypeError

- **Status:** `Resolved`
- **Opened:** 2026-11-03 11:35
- **Closed:** 2026-11-03 11:38
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking any interactive filter item or ratio bar chip (e.g. `Chrome_1920x1080`) rendered the filter chip in the container filter strip, but failed to update or recalculate any card visual states, leaving all pie charts, ratio bars, and list items in their unfiltered state ("filtering stoped working at all").
- **Root Cause:** In `matchesVariationFilter` in `report-manager.js`, variable `val` was declared with `const val = rawVal.toLowerCase();` and subsequently reassigned via `val = val.replace(/\s*\((healed|ai|clean)\)/i, '').trim();`. This threw an uncaught `TypeError: Assignment to constant variable.` on the first variation evaluated inside `updateContainerCardsVisualState`, aborting visual recalculation across all container cards.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest.java` checked syntax via `node -c` and string containment. Assignment to an identifier is syntactically valid in ECMAScript AST grammar and is only raised as a `TypeError` at runtime.
- **Resolution:** Changed `const val = rawVal.toLowerCase();` to `let val = rawVal.toLowerCase();` in `matchesVariationFilter` in `report-manager.js`.
- **Safety Net Added:** Added Node.js runtime evaluation test `@Test testMatchesVariationFilterRuntimeExecutionWithoutConstTypeError()` in `StaticResourceSyntaxTest.java` executing `matchesVariationFilter` against sample variations in a Node.js VM to prevent runtime `TypeError` regressions.
