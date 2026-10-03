# [DEF-20260918-03] Multi-Turn DOM Amnesia, Tool Thrashing, and Discovery Tool Action Pollution in Agent Tool Loop

- **Status:** Resolved
- **Opened:** 2026-09-18
- **Closed:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-tool`)
- **Scope:** `Framework`
- **Symptom:** In multi-field form steps (such as filling credit card number, expiry date, and CVV), the agent executed redundant discovery tools (`query_dom`), blind exploratory scrolling (`scroll down`), and re-querying across 7 turns. The read-only discovery tools and blind scrolls were erroneously recorded as persistent test actions in the execution report and playbook (`QUERY_DOM`, `SCROLL`).
- **Root Cause:**
  1. `pruneExpiredDomFromConversation` unconditionally wiped the DOM from Turn 1 without verifying if a replacement DOM was being provided for Turn 2 (`requireDomForNextTurn == false`), completely blinding the LLM of selectors.
  2. The continuation prompt actively nudged the model to call `query_dom`.
  3. `query_dom` used case-sensitive CSS selectors, missing camelCase attributes (`cardExpiry`, `cardCvv`), prompting an uncommanded off-screen scroll.
  4. `finishLoop` failed to filter out discovery/inspection tools (`query_dom`, `inspect`, `request_context`), polluting `PlaybookStep.actions` and `PlaybookStep.toolCalls`.
  5. Strict single-action serialization prevented the agent from proposing cohesive form field inputs in a single turn.
- **Detection Gap ("What did we miss?"):** Tests verified that `pruneExpiredDomFromConversation` pruned messages to save tokens, but did not verify that selector accessibility was maintained across multi-action turns or that discovery tools were excluded from recorded playbook actions.
- **Resolution:**
  1. Updated `pruneExpiredDomFromConversation` to preserve DOM/element selectors when no new DOM snapshot is added for the next turn.
  2. Excluded read-only discovery tools (`query_dom`, `inspect`, `request_context`, `inspect_visual`) from recorded `actions` and `toolCalls` in `finishLoop`.
  3. Enabled case-insensitive attribute fallback in `query_dom` JavaScript.
  4. Allowed cohesive form field action batching for multi-input instructions.
- **Safety Net Added:** Added unit and integration tests verifying that multi-field steps execute cleanly without amnesia, that discovery tools are not recorded as playbook actions, and that `query_dom` handles case-insensitive attribute matching.
