# [DEF-20261009-1455] Omission of Known Tests Pattern Swatches in History Trend Legend and Run Breakdown Bar

- **Status:** `Resolved`
- **Opened:** 2026-10-09 14:55
- **Closed:** 2026-10-09 15:00
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the History Trend view (`batch-history.html`), the Allure-style legend displayed patterned swatches (sparkle pattern for healed, diagonal pinstripes for AI-driven) for passed and failed-unknown tests, but omitted legend swatches for known bug categories ("Succeeded with Known Bug" / fixed and "Failed due to Known Bug" / known). Furthermore, the batch details run executions table (`batch-details.html`) rendered monolithic status bars rather than granular clean, healed, and AI-driven segments.
- **Root Cause:** When the SVG stacked area layers and DTO computations were enhanced with full 13-layer granularity (including `trendLayerKnownAi`, `trendLayerKnownHealed`, `trendLayerFixedAi`, and `trendLayerFixedHealed`), the corresponding `.legend-item` elements in `batch-history.html` were not populated for fixed and known healed/AI variations. Additionally, `batch-details.html` was not aligned with `batch-overview.html` and `batch-history.html` to display the granular sub-segment divs.
- **Detection Gap ("What did we miss?"):** Previous syntax tests verified JavaScript validity and that CSS pattern classes (`.legend-box.seg-ai-print`, `.legend-box.seg-healed-print`) were defined, but did not assert template-level completeness across all four execution status domains in `batch-history.html` and `batch-details.html`.
- **Resolution:**
  1. Updated `batch-history.html` legend with a full, symmetric 13-item breakdown covering Clean, Healed (sparkle), and AI-Driven (striped) for Passed, Succeeded Fixed, Failed Known, and Failed Unknown, plus Ignored.
  2. Updated `batch-details.html` status stacked bars to render granular sub-segments (`passedClean`, `passedHealed`, `passedAi`, `fixedClean`, `fixedHealed`, `fixedAi`, `knownClean`, `knownHealed`, `knownAi`, `unknownClean`, `unknownHealed`, `unknownAi`, `ignored`) with numeric counts and tooltips.
- **Safety Net Added:** Added `@Test` `StaticResourceSyntaxTest.testSymmetricPatternLegendAndStatusBreakdownTemplates` asserting presence of all symmetric legend entries in `batch-history.html` and granular pattern sub-segments in `batch-details.html`.
