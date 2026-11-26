# [DEF-20261004-1405] Tooling Path Drift and Hardcoded Machine Paths

- **Status:** `Resolved`
- **Opened:** 2026-10-04 14:05
- **Closed:** 2026-10-04 14:05
- **Component:** `neodymium-test-server`, root tooling
- **Scope:** `Infra/Build`
- **Symptom:** `StorefrontPatcher` failed on all environments other than the original developer's workstation due to a hardcoded absolute file system path (`/home/rschwietzke/...`). `EmbeddedHtmlServer.main("generate")` wrote product catalog updates to a single-module path `src/test/resources/ai-test-pages/verla-products.json` that was removed in the multi-module restructuring. Additionally, `run-neo-test.sh` hardcoded `-pl neodymium-core`, preventing tests located in `neodymium-e2e-tests` from running.
- **Root Cause:** Post-refactoring path drift following the migration to Maven multi-module architecture:
  1. `StorefrontPatcher.BASE_DIR` held an uncommitted absolute workstation path string.
  2. `EmbeddedHtmlServer.main("generate")` was not updated to write to `neodymium-test-server/src/main/resources/ai-test-pages/verla-products.json`.
  3. `run-neo-test.sh` lacked automatic module detection and did not respect paths containing `/neodymium-e2e-tests/`.
- **Detection Gap ("What did we miss?"):** Tooling scripts and helper utilities were executed ad-hoc during local development and lacked automated regression checks in standard CI pipelines.
- **Resolution:**
  1. Updated `StorefrontPatcher` to dynamically resolve `BASE_DIR` via `resolveBaseDir()` targeting `neodymium-test-server/src/main/resources/ai-test-pages/AuraGlanceTest/shop` with module-relative fallback.
  2. Updated `EmbeddedHtmlServer.main("generate")` to output to `neodymium-test-server/src/main/resources/ai-test-pages/verla-products.json` (with fallback).
  3. Enhanced `run-neo-test.sh` with automated module detection for `neodymium-e2e-tests` and customizable `$NEO_MODULE` override.
- **Safety Net Added:** Added permanent automated unit test `org.neodymium.ai.util.EmbeddedHtmlServerTest.testStorefrontPatcherResolveBaseDir` verifying that `StorefrontPatcher.resolveBaseDir()` dynamically resolves an existing directory on any machine.
