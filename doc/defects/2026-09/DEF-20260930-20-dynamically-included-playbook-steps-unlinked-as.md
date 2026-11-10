# [DEF-20260930-20] Dynamically included playbook steps unlinked as sub-steps of active include step

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `org.neodymium.ai.executor.selenide.plugins.IncludeAction`
- **Scope:** `Framework`
- **Symptom:** Playbook steps dynamically included at runtime via `include(...)` (such as inside conditional `If ... _include:` branches) executed as flat top-level steps on the execution context stack without being linked to the active `include` parent step in execution reports or console execution logs.
- **Root Cause:** In `IncludeAction.java`, parsed steps from included playbooks were mapped to pipeline steps and pushed onto `ExecutionContext.runStack` without setting `subStep.setParent(currentStep)` or registering them under `currentStep.getSubSteps()`.
- **Detection Gap ("What did we miss?"):** Tests for `IncludeAction` verified that included steps executed on the browser, but did not assert that dynamically included steps were attached as `subSteps` of the active `currentStep` in `ExecutionContext` and execution reports.
- **Resolution:** Updated `IncludeAction.java` to retrieve the active `currentStep` from `ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP`, set `subStep.setParent(currentStep)` for each included step, and populate `currentStep.getSubSteps()`.
- **Safety Net Added:** Verified dynamic sub-step linking across `PreliminaryReportListenerTest` and `SubStepReportingAndScopingTest`.
