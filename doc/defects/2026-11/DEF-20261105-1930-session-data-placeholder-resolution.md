# [DEF-20261105-1930] AiSession.getData and SessionData.get Fail to Resolve Variable Placeholders

- **Status:** `Resolved`
- **Opened:** 2026-11-05 18:00
- **Closed:** 2026-11-05 19:30
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In programmatic AI tests, retrieving test data via `session.getData(key)` returned the raw un-substituted template string (e.g. `"user${random}@example.org"`) instead of resolving variable placeholders, forcing callers to employ the workaround `session.getData().resolveVariables("${key}")`.
- **Root Cause:** `AiSession.getData(key)` delegated to `SessionData.get(key)`, which directly returned `DataEntry.value()` without invoking variable resolution. Variable placeholder resolution was only wired into playbook step execution and action handlers.
- **Detection Gap ("What did we miss?"):** Existing `AiSessionTest` only asserted on static scalar values (e.g. `city -> "Berlin"`). `SessionDataTest` tested `resolveVariables(template)` directly with string literals rather than checking property retrieval through `SessionData.get(key)` or `AiSession.getData(key)`.
- **Resolution:**
  1. Updated `SessionData.get(key)` to resolve variable placeholders leniently via `resolveAvailableVariables(str)` when values contain `${...}`.
  2. Added overloaded `SessionData.get(key, strict)` and `SessionData.getRaw(key)`.
  3. Added overloaded `AiSession.getData(key, strict)` and `AiSession.getRawData(key)` to give callers full control over strict resolution and un-substituted raw template inspection.
  4. Updated `SessionData.resolveVariables` to resolve placeholders via `getRaw(placeholderKey)` instead of `get(placeholderKey)`, preventing circular references from resetting recursion depth and causing `StackOverflowError`.
  5. Added explicit `depth > 10` strict failure guard (`IllegalStateException`) for circular dependencies and null key protection (`if (key == null) return null;`) in `SessionData.getEntry`.
- **Safety Net Added:** Added automated regression tests in `AiSessionTest.testGetDataResolvesPlaceholders` and `SessionDataTest.testGetResolvesPlaceholdersAndPreservesRaw`, `testGetResolvesNestedAndGuardsCircularPlaceholders`, `testGetNullKeyHandling`, and `testGetNonStringObjectPreservation`.
