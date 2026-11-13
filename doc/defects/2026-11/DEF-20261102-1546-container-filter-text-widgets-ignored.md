# [DEF-20261102-1546] United Widget Container Filtering Ignores Text and KPI Presentations

- **Status:** `Resolved`
- **Opened:** 2026-11-02 15:43
- **Closed:** 2026-11-02 15:47
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When activating an interactive cross-filter inside a united widget container (such as selecting `Chrome_1920x1080`), pie charts and ratio bars recalculated their metrics and breakdown numbers correctly, but text widgets (`TEXT` and `TEXT_WITH_SMALL_TREND`, such as AI Usage KPI breakdown cards or status KPI value cards) remained completely unchanged, displaying the full unfiltered totals.
- **Root Cause:** In `report-manager.js`, both `onContainerWidgetFilterClick` and `clearContainerCrossFilter` only checked `PIE_CHART`, `RATIO_BAR`, and `LIST` presentations. There was no recalculation handler implemented or invoked for `TEXT` or `TEXT_WITH_SMALL_TREND` presentations, leaving text cards unmanaged during both filter application and filter reset.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest.java` verified function presence for pie chart and ratio bar recalculation, but lacked assertions ensuring that text widget presentations were registered and wired into container filter dispatching and clearing.
- **Resolution:**
  1. Implemented `recalculateContainerText(sec, variations, isFiltered)` in `report-manager.js`:
     - **`AI_USAGE` metric:** Computes active counts for AI-Driven, Healed, Playbook, and Classic modes from `variations`. Updates primary values (`.widget-kpi-primary-val`), percentages or test counts (`.font-mono`), secondary label (`.widget-kpi-secondary-val`), and section header count. Dims zero-count segments during filtering.
     - **`STATUS` metric:** Computes filtered pass rate, passing count, fail counts, updating primary KPI (`${passPct}% PASS`), status color, secondary label, and healed/AI badges.
     - **`COSTS` metric:** Computes filtered cost and token breakdown.
     - **`TAG` metric:** Computes filtered tag item counts.
  2. Wired `recalculateContainerText(childCard, matchedVariations, true)` in `onContainerWidgetFilterClick` for `TEXT` and `TEXT_WITH_SMALL_TREND`.
  3. Wired `recalculateContainerText(childCard, allVariations, false)` in `clearContainerCrossFilter` to restore full values upon filter removal.
  4. Exported `window.recalculateContainerText = recalculateContainerText;` for global access and testability.
- **Safety Net Added:** Added automated regression test `StaticResourceSyntaxTest.testUnitedContainerTextWidgetCrossFilterDispatching()` verifying `recalculateContainerText` definition, metric handling (`AI_USAGE`, `STATUS`, `COSTS`), and wiring in both filter activation and reset code paths.
