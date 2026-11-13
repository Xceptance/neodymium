# [DEF-20261105-1055] Playbook YAML Tags Area Mapped to Browsing Default Category

- **Status:** `Resolved`
- **Opened:** 2026-11-05 10:55
- **Closed:** 2026-11-05 11:15
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:** YAML playbooks with explicit `tags.Area` (e.g. `Area: [Search, Browsing]`) are displayed under `@Browsing (default)` in Aura Manager reports and dashboards.
- **Root Cause:** 
  1. `InteractiveStateBuilder` serialized `variationTags` into `state.tags` but did not set `areaName`.
  2. `LocalRunJsonStorageService` only resolved `location` from `tags.locale` and did not inspect `tags.Area` or `tags.area`, falling back to `"Browsing (default)"` when no area subfolder was present on disk.
  3. `AuraFileService.extractAreaTag` only recognized single-line `#@Area` comments and ignored structured YAML `tags.Area` blocks.
- **Detection Gap ("What did we miss?"):** Existing test cases tested area tags either through directory layout (`runId/@Area/TestClass`) or legacy `#@Area` comment prefixes, without verifying structured YAML `tags.Area` tag propagation into flat runner execution storage.
- **Resolution:** 
  1. Updated `InteractiveStateBuilder` to extract the primary area from `variationTags` and populate `areaName`.
  2. Updated `LocalRunJsonStorageService` to inspect `tags.Area` / `tags.area` before falling back to `"Browsing (default)"`.
  3. Updated `AuraFileService.extractAreaTag` to support structured `tags.Area` blocks.
- **Safety Net Added:** 
  - `InteractiveConsoleEngineTest.testAreaNameExtractedFromVariationTags`
  - `AuraFileServiceTest.testExtractAreaTagFromStructuredYamlTags`
  - `RunStorageSyncServiceTest.testExecutionWithAreaTagsResolvesAreaWithoutFolder`
