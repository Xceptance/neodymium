# [DEF-20261103-0945] Bug List Filter Leak Via Shared Bug Ticket Across Variations

- **Status:** `Resolved`
- **Opened:** 2026-11-03 09:45
- **Closed:** 2026-11-03 10:25
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When filtering a united container by "Passed", a bug list widget displays failed test variations (`failed-known`) alongside succeeded variations (`succeeded-fixed`) if they share the same bug ticket ID.
- **Root Cause:** In `report-manager.js` (`recalculateContainerList`), bug sub-items were matched via `subLabel && matchedBugTickets.has(subLabel)`. Since a single bug ticket can be attached to multiple variations (e.g., across locales or environments where one passes and another fails), matching on ticket string bypassed variation-level status isolation.
- **Detection Gap ("What did we miss?"):** Previous tests asserted that `recalculateContainerList` matched bug tickets against `matchedBugTickets`, but did not test multi-variation suites where one variation passes (`succeeded-fixed`) and another fails (`failed-known`) with the identical bug ticket.
- **Resolution:** Removed `matchedBugTickets.has(subLabel)` in `recalculateContainerList` and enforced strict variation ID matching (`subVarId && matchedVids.has(subVarId)`), keeping key fallback only when variation IDs are absent.
- **Safety Net Added:** Updated `StaticResourceSyntaxTest.testUnitedContainerCrossFilterListRecalculation` verifying strict variation ID matching and asserting absence of loose ticket-name cross-variation matching (`matchedBugTickets.has(subLabel)`).
