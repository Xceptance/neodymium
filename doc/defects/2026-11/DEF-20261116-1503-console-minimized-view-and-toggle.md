# [DEF-20261116-1503] Broken Minimized Console Appearance and Inability to Toggle While Running

- **Status:** `Resolved`
- **Opened:** 2026-11-16 15:03
- **Closed:** 2026-11-16 15:21
- **Component:** `aura-manager`
- **Scope:** `Framework & SUT`
- **Symptom:** In `/playbooks`, the console displays in a cramped 38px strip where action buttons and header text are clipped by internal padding and `overflow: hidden`. Furthermore, clicking "Collapse" while a test is executing fails to minimize the console, and tests running in the background leave the console collapsed on page load.
- **Root Cause:** 
  1. `#consolePanel.console-collapsed` constrained height to 38px with `overflow: hidden` while inner container padding and margin exceeded 43px, clipping buttons and labels.
  2. `updateCenterLayout()` hardcoded `effectiveCollapsed = isRunning ? false : consoleCollapsed;`, overriding user collapse actions during test execution.
  3. Status polling updated `isRunning` on detecting an active run but omitted calling `updateCenterLayout()` or updating `consoleCollapsed`.
- **Detection Gap ("What did we miss?"):** Previous UI tests in `AuraManagerStopExecutionUiTest` and `AuraManagerEditorUiTest` focused on queue actions and editor modals but did not assert console height, element clipping, or toggle behavior during live execution.
- **Resolution:** Redesigned collapsed console into a clean, unclipped 38px dock bar hiding granular controls (`.console-expanded-controls`), decoupled toggle state from `isRunning` so `consoleCollapsed` can be toggled open/closed regardless of execution state, and added automatic expansion triggers on run start and status polling rising edge.
- **Safety Net Added:** `AuraManagerConsoleUiTest#testConsoleStartsMinimizedWithCleanAppearance`, `testConsoleToggleWhileIdle`, `testConsoleAutoOpensWhenExecutionStarts`, `testConsoleToggleWhileRunning`.
