# [DEF-20261028-1445] Tag-Bound Bug Scoping Trap and Silent Non-Matching Feedback

- **Status:** `Resolved`
- **Opened:** 2026-10-28 14:45
- **Closed:** 2026-10-28 14:50
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When a user links a bug with tag criteria that do not match the currently open test execution (e.g., binding `environment: DEV` while viewing an `STG` run report), the save modal closes silently, the side panel continues displaying "Clean / No Linked Bugs", the execution row in the table remains unchanged with 0 bugs, and future runs on the target environment fail to discover the bug.
- **Root Cause:**
  1. `AuraReportDataService.addBugToExecution` persisted the current run's `environment` (e.g. `STG`) and `batchName` into `TestBaseBugEntity` rather than `ALL`, causing subsequent report queries on the target environment (`DEV`) to SQL-filter out the candidate bug (`findByBatchNameInAndEnvironmentIn`).
  2. When `matchesBugTags` evaluated to false for the active execution, `appliesToTarget` was false, omitting the ticket from the execution's bugs list in `run.json`. The UI replaced the badge with "Clean / No Linked Bugs" without feedback indicating the bug was successfully linked to Test Base for matching runs.
- **Detection Gap ("What did we miss?"):** Prior tests only verified tag-bound bug matching where the linked test run's environment matched the tag criteria, never testing cross-environment linkage (e.g. linking a DEV bug while viewing an STG execution).
- **Resolution:**
  1. In `AuraReportDataService.addBugToExecution`, when `cleanBoundTags` is present, stored `targetBatch = "ALL"` and `targetEnv = "ALL"` in `TestBaseBugEntity` so candidate discovery queries across all environments/batches pick up the bug and delegate evaluation to dynamic tag matching (`TagUtils.matchesTagCriteria`).
  2. Added `@PostConstruct` startup migration `normalizeTagBoundBugsScope()` in `AuraReportDataService` to normalize any existing tag-bound bugs in the database to `batchName = "ALL"` and `environment = "ALL"`.
  3. Extended `TestExecutionDto` with `inactiveBugDetails` and updated `applyBugsToExecution` to collect non-matching candidate bugs for the variation into `inactiveBugDetails`.
  4. Updated `side-panel-step-list.html` (`#sidePageStatusBadge`) to display an informative notice and pill list for inactive bugs bound to other tags with unlink capabilities (`data-inactive-bug-ticket`).
- **Safety Net Added:** `AuraReportTagBoundBugSeverityTest.testCrossEnvironmentBugLinkageAndInactiveBugDiscovery`
