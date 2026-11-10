# [DEF-20261002-19] Ephemeral Visual Badges Prematurely Stripped & Fragile DOM Selector Synthesis in Visual Mode

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`VisualBadgeInjector`, `BrowserToolProvider`, `AgentToolLoopStep`, `PlaybookToolReplayer`, `PlaybookStep`)
- **Scope:** `Framework`
- **Symptom:**
  1. AI agent attempting to click or interact with marked elements (`badge:N` or `[data-m="N"]`) after a visual marking screenshot encountered `NoSuchElementException` or failed selector resolution due to premature DOM badge cleanup.
  2. In CI replay, steps executed in visual mode failed with `NoSuchElementException` when searching for `[data-m="N"]` (or clicked the wrong element when attempting heuristic CSS selector synthesis from generic class soup like `button._btn_ghost`).
- **Root Cause:**
  1. `screenshot(markInteractive=true)` destroyed the badge overlay DOM nodes in a `finally` block before returning the image to the LLM turn loop. The visual badges visible in the screenshot no longer existed in the DOM when the subsequent action tool was invoked.
  2. Visual markers (`marker:N`) were erroneously normalized into CSS selectors (`[data-m="N"]`) and recorded as DOM queries. Because visual mode exists specifically to bypass inaccessible or chaotic DOM structures, forcing visual targets into the CSS selector domain caused replay failures when markers were absent.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests verified screenshot generation with mocks rather than testing multi-turn interaction loops where an agent acts upon previously marked visual targets and replaying those recorded playbooks across `REPLAY_STRICT` and `REPLAY_WITH_HEALING` modes against messy DOM fixtures.
- **Resolution:**
  1. Decoupled visual marking into an explicit, stateful `mark_elements` tool and proactive `(marker)` step hint that maintains both screen overlays and compact `data-m="N"` attributes in the DOM.
  2. Pure Visual Coordinate Grounding: In visual mode (`(marker)` / `marker:N`), resolved markers directly to screen coordinates `(x, y)` and executed native coordinate clicks (`performSafeCoordinateClick`).
  3. Recorded visual interactions in the playbook as pure coordinate actions (`target: "coord: x,y"`, `x`, `y`), completely eliminating synthetic CSS selectors, DOM querying, and live DOM stamping during CI replay.
  4. Dispatched native `StateCapturedEvent` to embed the marked screenshot into Neodymium's native HTML/Markdown execution reports (zero Allure dependency).
  5. Auto-purged markers after action execution to keep the SUT pristine.
- **Safety Net Added:**
  Regression tests in `BrowserToolsTest.java`, `VisualBadgeInjectorTest.java`, and Aura AI sandbox end-to-end suite `VisualMarkersSandboxMockTest.java` verifying marker retention across turns, pure coordinate execution, zero CSS selector generation, report screenshot emission, post-action cleanup, and flawless replay across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`.
