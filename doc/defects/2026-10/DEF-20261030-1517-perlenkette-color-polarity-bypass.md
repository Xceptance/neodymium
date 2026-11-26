# [DEF-20261030-1517] String of Pearls Color Polarity Bypassed by Status Class Overrides

- **Status:** `Resolved`
- **Opened:** 2026-10-30 15:17
- **Closed:** 2026-10-30 15:23
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Toggling the Color Spectrum Polarity radio buttons ("Higher is Green (Good)" vs "Higher is Red (Critical)") in the String of Pearls widget configurator modal had no visible effect on chart pearls; pearls with high pass rates remained red in both modes.
- **Root Cause:** In `WidgetAggregationService.resolvePearlColors()`, when `higherIsRed == false`, legacy status-class checks (`statusClass.equals("bead-unknown")`) short-circuited before the 5-tier health spectrum was evaluated. Real-world test runs with failed tests (`failedUnknownCount > 0`) were forced to Red under `HIGHER_IS_GREEN`, and were also mapped to Red under `HIGHER_IS_RED` (since `passRate >= 90.0`), rendering beads identically across both polarity settings.
- **Detection Gap ("What did we miss?"):** Unit tests in `WidgetAggregationServiceTest` instantiated synthetic `TestRunEntity` objects with only `passRate` set, omitting `failedUnknownCount`. This defaulted `statusClass` to `"bead-pass"`, failing to reproduce real-world runs where failures exist alongside high pass rates.
- **Resolution:** Removed the short-circuiting status class overrides from `resolvePearlColors()` so String of Pearls faithfully adheres to the 5-tier health spectrum based on the selected polarity. Also normalized score calculations for non-percent metrics (BUGS and COSTS).
- **Safety Net Added:** Added `@Test` `testStringOfPearlsPolarityWithUnknownFailures` in `WidgetAggregationServiceTest` asserting that test runs containing active unknown failures (`failedUnknownCount > 0`) evaluate to Green under `HIGHER_IS_GREEN` (for pass rate >= 90%) and flip to Red under `HIGHER_IS_RED`.
