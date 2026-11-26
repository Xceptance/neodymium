# [DEF-20261112-1410] Duplicate Area Tag With and Without @ Prefix in Run Tags List

- **Status:** `Resolved`
- **Opened:** 2026-11-12 14:10
- **Closed:** 2026-11-12 14:35
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On the dashboard "List of Runs" table, runs with an area tag displayed two tag chips for the same area: one without `@` (e.g. `Area: Browse`) and one with `@` (e.g. `Area: @Browse`).
- **Root Cause:** In `RunStorageSyncService.java`, `addTagSafe` deduplicated tags via `tagKey.toLowerCase() + "::" + tagValue.toLowerCase()`. Ingestion from root `run.json` tags produced `("Area", "Browse")`, while fallback ingestion from execution areas in `aSet` used `AuraReportDataService.normalizeArea()` which prepended `@` (`("Area", "@Browse")`). Because the composite keys differed (`"area::browse"` vs `"area::@browse"`), both entries were persisted in the database. Furthermore, `AuraReportDataService.populateRunTags` did not normalize or collapse `@` prefixes when assembling tags for display.
- **Detection Gap ("What did we miss?"):** Existing test cases in `RunStorageSyncServiceTest` validated synthetic `Suite` and `Environment` tag suppression, but never tested runs containing both structured `tags.Area` and execution `areaName` to verify cross-source `@` deduplication.
- **Resolution:**
  1. In `RunStorageSyncService.java`, updated `addTagSafe` to normalize `Area` tag values by stripping leading `@` characters so both map to the clean tag value (e.g. `Browse`), ensuring `seenTagPairs` deduplicates them. Also stripped leading `@` when iterating `aSet`.
  2. In `AuraReportDataService.java:populateRunTags`, normalized `Area` tag values on load so that even for runs already stored in the database with duplicate rows, the tags are deduplicated into a single `Area: <Value>` chip.
  3. In `AuraReportDataService.getRunTags` and `getRunTagsMap`, normalized `Area` tag values to prevent duplicate entries with and without `@`.
- **Safety Net Added:** Added unit tests `testPopulateRunTagsDeduplicatesAreaTagsWithAndWithoutAtPrefix` in `RunStorageSyncServiceTest` and `AuraReportViewControllerTest` asserting that a run with both `Area: Browse` and `Area: @Browse` produces only a single `Area: Browse` tag.
