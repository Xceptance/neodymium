# [DEF-20261008-1405] Historical Database Runs Skip Ingestion Causing Zero Healed Counts in All Views

- **Status:** `Resolved`
- **Opened:** 2026-10-08 14:05
- **Closed:** 2026-10-08 14:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** After redesigning charts and breakdown lists for healed tests, existing runs in the database continue rendering solid clean sectors with data-pass-healed="0" and missing sparkle sub-arcs and inline badges.
- **Root Cause:** RunStorageSyncService#syncLocalRunStorage skips any run where runRepository.existsById(runId) is true. Existing runs ingested prior to the addition of passedHealedCount and succeededFixedHealedCount columns retained null values. Furthermore, LocalRunJsonStorageService#buildRunJsonContent failed to write healed counters to the run.json summary block, and AreaSummaryDto lacked SVG dash calculations for area donut charts.
- **Detection Gap ("What did we miss?"):** Unit tests in RunStorageSyncServiceTest tested brand-new run ingestion on fresh repositories, but did not test migration/sync of pre-existing database entities containing null healed counts.
- **Resolution:**
  1. Updated RunStorageSyncService#syncLocalRunStorage to re-import runs if existing entities have null healed counts.
  2. Added automatic in-flight backfilling in AuraReportDataService#getAllBatches and #getBatchOverviewData so historical runs are immediately populated from their test executions and persisted.
  3. Added passHealed, fixedHealed, knownHealed, unknownHealed to summaryNode in LocalRunJsonStorageService#buildRunJsonContent.
  4. Implemented dasharray and dashoffset calculation methods in AreaSummaryDto and dynamic SVG pattern rendering in run-report.html.
- **Safety Net Added:** Unit tests asserting that existing entities with null healed counts are correctly updated upon resync.
