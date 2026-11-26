# [DEF-20261020-1205] Interactive Console Suggest Fix Targets Wrong Step Across Multi-Block Playbooks and Suffers Low-Contrast Modal Styling

- **Status:** `Resolved`
- **Opened:** 2026-10-20 12:05
- **Closed:** 2026-10-20 12:12
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** When clicking "Suggest fix" on a failed step in interactive mode, the AI suggestion modal displays an instruction fix for a subsequent step in the `after` block instead of the failed step in the `steps` block. Additionally, the modal text is barely legible in light mode due to dark translucent backgrounds (`rgba(0,0,0,0.3)` and `rgba(0,0,0,0.4)`) paired with low-contrast muted grey/dark text, and LLM output returns conversational markdown noise (`"Here is the clearest, most atomic phrasing: > **...**"`).
- **Root Cause:**
  1. **1-Based Global Index vs. 0-Based Multi-Block Indexing:** In `InteractiveStateBuilder.java:775`, steps are exposed to the UI with 1-based global indices (`step.index = globalIndex + 1`). When the user clicked "Suggest fix" on Step 1 (the single step in `steps`), the client dispatched `SUGGEST_FIX` with `{ index: 1 }` without specifying `block`, `blockIndex`, `flatIndex`, or `instruction`. In `InteractiveConsoleListener.java`, `findStepByIndex(context, 1, null)` resolved against `flatSteps.get(1)`, which in a 0-based list pointed to the first step of the `after` block rather than the failed step in `steps`.
  2. **Theme-Blind Inline Styling in Modal:** `#suggestionOriginalText` and `#suggestionTextarea` had hardcoded inline styles (`background: rgba(0,0,0,0.3); color: var(--text-muted);` and `background: rgba(0,0,0,0.4); color: var(--text-main);`). In light theme, `rgba(0,0,0,0.3)` overlaid on a white modal creates a dark box while `var(--text-muted)` evaluates to `#9ca3af` (light grey text), causing severe contrast degradation and WCAG failure.
  3. **Unsanitized LLM Suggestion Output:** The prompt lacked strict output framing constraints ("Output ONLY the raw revised step instruction"), causing the LLM to return conversational markdown preambles, blockquotes, and bold formatting.
- **Detection Gap ("What did we miss?"):** Prior unit test `testPauseOnStepFailureSuggestFixResolvesStepByIndexAndStoresState` only tested a single-block playbook where `playbook.steps` and `flatSteps` were identical and did not test multi-block playbooks (having `steps` and `after` blocks concurrently). Furthermore, UI modal markup and theme contrast were not validated against theme CSS variables.
- **Resolution:**
  1. Updated `interactive_console.js` `requestFixSuggestion()` to locate the step card within `currentState.blocks` and send `block`, `blockIndex`, `flatIndex`, `rawIndex`, and the exact `instruction`.
  2. Updated `InteractiveConsoleListener.java` `handleSuggestFix()` to resolve the target step using a multi-level fallback: authoritative instruction match against `currentStep`, block & block index lookup, flat index lookup, raw index with block name lookup, 1-based index fallback, and fallback search across `flatSteps`.
  3. Constrained the LLM system and user prompts to demand only the raw atomic instruction without markdown formatting or conversational intro text, and introduced `cleanInstructionSuggestion()` to strip residual code fences, blockquotes, bold/italic markers, quotes, and conversational preambles (`Here is...:`).
  4. Replaced inline styles in `interactive_console.html` with `.suggestion-original-box` and `.suggestion-textarea` classes in `interactive_console.css` using theme variables (`--bg-surface-hover`, `--border-color`, `--text-main`) for crisp contrast in both dark and light modes.
- **Safety Net Added:**
  - `InteractiveConsoleListenerTest.testCleanInstructionSuggestion`: Verifies markdown code fence, blockquote, bold marker, and conversational preamble removal.
  - `InteractiveConsoleListenerTest.testSuggestFixMultiBlockPlaybookResolvesFailedStepInMainBlock`: Simulates a multi-block playbook (`steps` and `after`) and verifies `SUGGEST_FIX` resolves the failed step in `steps` instead of drifting to `after`.
  - `InteractiveConsoleListenerTest.testSuggestFixResolvesStepByExplicitInstruction`: Verifies fallback resolution by explicit instruction name.
  - `InteractiveConsoleServerTest.testSuggestFixModalMarkupAndCssStyling`: Verifies HTML modal markup, CSS class contrast definitions, and JS payload fields.
