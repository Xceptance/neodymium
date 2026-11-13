# [DEF-20261027-1215] Remove Hardcoded Environment Property in Favor of Universal Dynamic Tags

- **Status:** `Resolved`
- **Opened:** 2026-10-27 12:15
- **Closed:** 2026-10-27 12:20
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Disconnect between hardcoded `environment` domain property and the dynamic `environment` tag type. Hardcoded "Environment: Batch Ratio" card, "Environment" grouping pills, uniqueness toggles, and scope menus were rendered even when runs had no environments, while database tag types named "Environment" or "env" were suppressed from dynamic tag ratio cards and grouping options due to hardcoded exclusion keywords.
- **Root Cause:** `environment` was historically configured as a 4th hardcoded domain property alongside `browser`, `locale`, and `area`. It was listed in `BUILTIN_DIMENSIONS`, `BUILTIN_KEYS`, and `builtInKeywords`, which caused dynamic tag discovery to filter it out, requiring redundant and hardcoded UI elements for Environment across templates and controllers.
- **Detection Gap ("What did we miss?"):** Previous tests asserted static environment cards and grouping pills rather than enforcing domain neutrality where only browser, locale, and area are core built-in execution dimensions while all other dimensions are dynamically driven by database tags.
- **Resolution:**
  1. Updated `AuraReportViewController.builtInKeywords` and `AuraReportDataService.builtInKeywords` to strictly contain only `browser`, `browsers`, `locale`, `locales`, `area`, `areas`, `none`, and `all`, removing `environment` and `env`.
  2. Removed hardcoded Environment grouping pills from `batch-overview.html` and `batch-history.html`, letting `groupingTags` render `Tag (Environment)` dynamically.
  3. Removed hardcoded `scopeEnvsDropdown` from `batch-history.html`, delegating run scope filtering to `tagTypeFilters`.
  4. Removed hardcoded Environment uniqueness button from `batch-history.html`, allowing `availableUniquenessTags` to dynamically render `+ Tag: Environment`.
  5. Removed hardcoded static "Environment: Batch Ratio" card from `batch-history.html`, allowing `tagRatioCards` to dynamically render an Environment ratio card whenever present in the dataset.
  6. Updated `RunStorageSyncService` to map incoming run environments into `TestRunTagEntity(runId, "Environment", env)` ensuring 100% backward compatibility for existing records.
- **Safety Net Added:** Unit tests in `AuraReportViewControllerTest` and `StaticResourceSyntaxTest` verifying dynamic tag card generation for Environment, dynamic grouping options, absence of static cards, and client-side filter synchronization.
