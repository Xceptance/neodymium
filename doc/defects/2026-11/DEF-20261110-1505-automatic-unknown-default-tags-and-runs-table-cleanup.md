# [DEF-20261110-1505] Automatic "Unknown"/"Default" Tag Synthesis and Runs Table Column Overcrowding

- **Status:** `Resolved`
- **Opened:** 2026-11-10 15:05
- **Closed:** 2026-11-10 15:25
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager's Widget Configurator, an environment filter ("Filter by Environment: Unknown" or "Default") is rendered even when runs have no configured environment. The runs list in batch history and the RUNS widget is overcrowded with redundant columns ("Trigger", "Locales", "Browsers") that display fallback values ("Unknown", "Default", "Chrome"), squeezing the "Tags" column and status breakdown bar.
- **Root Cause:**
  1. `AuraQueueService.java` passed a hardcoded `"Unknown"` string as the environment parameter to `queueRunProgressListener.onRunStarted`.
  2. `RunStorageSyncService.java` and `LocalRunJsonStorageService.java` synthesized fallback tags ("Environment: Unknown" and "Suite: Queue") because "Unknown" and "Queue" were not guarded.
  3. `AuraReportDataService.populateRunTags` unconditionally attached synthetic "Suite" and "Environment" tags for every run entity.
  4. `AuraReportViewController` and `AuraGlobalModelAdvice` exposed "Unknown" as a candidate environment filter.
  5. `batch-history.html` and `dashboard-widgets.html` allocated 3 separate table columns to "Trigger", "Locales", and "Browsers" instead of consolidating dimensional metadata under "Tags".
- **Detection Gap ("What did we miss?"):** Previous tag tests only verified explicit multi-attribute runs (e.g. `STG`, `PROD`, `Branch`, `Release`) and did not assert that untagged/queue runs remain completely free of synthetic "Unknown" and "Default" tags.
- **Resolution:**
  1. Added `public String environment;` to `RunRequest.java` and replaced hardcoded `"Unknown"` in `AuraQueueService.java` with dynamic resolution from `req.environment` or system properties, passing `null` when unspecified.
  2. Added `isRealEnvironment(String raw)`, `isRealLocale(String raw)`, and `isLegitimateTag(TestRunTagEntity tag)` helpers in `AuraReportDataService.java`.
  3. Guarded `RunStorageSyncService`, `LocalRunJsonStorageService`, and `populateRunTags` against synthesizing or retaining tags with values "Default", "Unknown", or "Queue". Updated `addTagSafe` and `populateRunTags` to reject and filter out tags whose value is "Default" or "Unknown".
  4. In `AuraReportViewController` and `AuraGlobalModelAdvice`, filtered `candidateEnvs` and `candidateLocales` using `isRealEnvironment` and `isRealLocale` so trivial placeholders are never exposed as candidate filters.
  5. Removed `Trigger`, `Locales`, and `Browsers` columns from `batch-history.html` and `dashboard-widgets.html`, updating `colspan` to 7 and dedicating the reclaimed horizontal space to `Tags`.
- **Safety Net Added:**
  - Automated tests in `RunStorageSyncServiceTest` and `AuraReportViewControllerTest` verifying no synthetic "Unknown", "Default", or "Queue" tags are created, and `candidateEnvs` is empty on untagged runs.
  - Template tests in `StaticResourceSyntaxTest` and `AuraReportViewControllerTest` verifying the 7-column layout.
