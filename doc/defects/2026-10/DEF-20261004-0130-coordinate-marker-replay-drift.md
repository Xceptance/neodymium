# [DEF-20261004-0130] Visual marker and coordinate clicks replay blindly without DOM identity verification

- **Status:** `Resolved`
- **Opened:** 2026-10-04 01:30
- **Closed:** 2026-10-04 01:31
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** On playbook replay, coordinate clicks resulting from visual marker clicks (`marker:N` / `target: "coord: x,y"`) blindly clicked viewport coordinates. If the layout shifted or elements moved between versions or responsive viewports, the click landed on empty space or the wrong element without triggering locator self-healing or failing in `REPLAY_STRICT`.
- **Root Cause:** 
  1. `BrowserToolProvider` Case 1 (visual markers) did not invoke `ReanchoringBridge.resolveElementAtPoint` to capture element identity before clicking.
  2. `BrowserToolProvider` Case 3 stored the feature vector only in `builder.withVariable(...)`, omitting it from the result content JSON node, so `AgentToolLoopStep` never attached `domFeatureVector` to the recorded tool call arguments.
  3. `ReanchoringBridge` serialized `"tagName"` instead of `"tag"`, causing Jackson to deserialize null tags into `DomFeatureVector`, which reduced similarity scores below the 0.70 healing threshold.
  4. `PlaybookToolReplayer.attemptHealing` bypassed healing for any target starting with `coord:`, and strict replay lacked an element identity check before sending coordinate clicks to WebDriver.
- **Detection Gap ("What did we miss?"):** Existing marker sandbox tests executed against static pages where coordinates never shifted between record and replay, leaving layout drift unexercised.
- **Resolution:**
  1. Updated `ReanchoringBridge.RESOLVE_DOM_ELEMENT_SCRIPT` to capture `tag`, `classes`, and `attributes` (omitting ephemeral `data-m`) and populate `tag`, `classes`, and `attributes` in `domFeatureVector`.
  2. Captured DOM identity in `BrowserToolProvider` Case 1 via `ReanchoringBridge.resolveElementAtPoint(driver, bx, by)` before clicking, and attached `domFeatureVector` to the result content JSON in both Case 1 and Case 3.
  3. Hardened `PlaybookToolReplayer.attemptHealing` to evaluate recorded feature vectors on coordinate targets: if the element drifted, it heals to the relocated candidate's selector (or relocated coordinates if no selector is derivable, preventing fallback to bare generic tags like `div`).
  4. Added `verifyStrictReplayGuards` in `PlaybookToolReplayer` to verify live element identity at `(x, y)` against `recordedVector` when replaying in `REPLAY_STRICT`.
- **Safety Net Added:** Added `CoordinateReplayGuardTest` in `neodymium-core` verifying DOM vector extraction, coordinate identity matching, healing to selectors, and coordinate relocation.
