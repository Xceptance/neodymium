# [DEF-20261117-1345] API Key Warning Server State Contamination & Global Model Metadata Gap

- **Status:** `Resolved`
- **Opened:** 2026-11-17 13:45
- **Closed:** 2026-11-17 13:54
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Starting a test run from the queue in a newly generated archetype project executed tests immediately without displaying the missing AI API key warning dialog, causing all test cases to fail due to missing credentials.
- **Root Cause:** 
  1. A verification curl command against the live archetype server invoked `POST /api/settings/ignore-api-key-warning`, setting `apiKeyWarningIgnored = true` on the singleton `AuraSettingsService` Spring bean in memory. Subsequent UI sessions loaded with `data-api-key-warning-ignored="true"`, causing `isApiKeyWarningIgnored()` to evaluate to `true` and bypass the pre-flight check.
  2. `AuraGlobalModelAdvice` did not inject `AuraSettingsService`, meaning views loaded via `AuraReportViewController` (such as root `/`) lacked `projectId` and API key status attributes (`data-project-id=""`).
- **Detection Gap ("What did we miss?"):** Tests verified controller endpoints independently with fresh isolated application contexts, but did not guard against test-harness verification calls leaking persistent in-memory state on live demo instances, nor did they assert model attributes on root `/` rendering.
- **Resolution:** 
  1. Injected `AuraSettingsService` into `AuraGlobalModelAdvice` so `hasApiKey`, `apiKeyWarningIgnored`, and `projectId` are globally present on all Aura views.
  2. Added an `htmx:confirm` event listener in `dashboard-runner.js` providing dual-layer request interception.
  3. Cleanly reset the archetype demo server and verified the end-to-end flow with browser automation.
- **Safety Net Added:** Added unit and integration test `testAddGlobalAttributesPopulatesApiKeyAndProjectMetadata` in `AuraGlobalModelAdviceTest` and verified `StaticResourceSyntaxTest` and `AuraTestSettingsControllerTest`.
