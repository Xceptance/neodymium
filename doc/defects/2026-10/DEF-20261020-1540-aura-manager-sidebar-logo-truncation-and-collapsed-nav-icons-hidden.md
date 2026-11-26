# [DEF-20261020-1540] Aura Manager Sidebar Logo Truncation and Collapsed Navigation Icons Hidden

- **Status:** `Resolved`
- **Opened:** 2026-10-20 15:40
- **Closed:** 2026-10-20 15:42
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. On the open/expanded navigation sidebar, the Neodymium Aura Manager logo title was clipped on the right as "Neodymium Aura M" with "anager" hidden from view.
  2. On the closed/collapsed navigation sidebar, the icons for "Playbooks & Queue", "Run History", and "Test Base" were completely missing/invisible, rendering empty button placeholders.
- **Root Cause:**
  1. In `aura-manager/src/main/resources/static/css/report-manager.css`, the sidebar navigation was configured with a fixed width of `260px` with `1rem` padding, `.logo-container` had `0.5rem` padding with `overflow: hidden`, and `.logo-icon` occupied `38px` plus `12px` gap. This left only `162px` width for `.logo-text`, which was styled with `font-size: 1.2rem; font-weight: 700; white-space: nowrap;`. The 22-character string "Neodymium Aura Manager" in Outfit/Inter rendered at ~200px wide, causing the container overflow to truncate "anager".
  2. In `report-manager.css`, the collapse rule `aside.collapsed nav button span, aside.collapsed .nav-item-btn span { display: none; }` unconditionally hid all `<span>` descendants inside navigation items. Because Google Material Symbols icons are rendered as `<span class="material-symbols-outlined">...</span>` within the `.nav-item-btn` container, this selector inadvertently hid the icon `<span>` elements along with the text label `<span>` elements.
- **Detection Gap ("What did we miss?"):**
  Existing test suites (`StaticResourceSyntaxTest.java` and UI tests) checked JavaScript bundle syntax and server HTTP endpoints, but lacked DOM and CSS regression tests validating that collapsed sidebar items retain visible Material Symbols icons and that brand titles fit within their allocated bounds.
- **Resolution:**
  1. Restructured `sidebar.html` and `report-manager.css` logo into a responsive two-line stacked brand layout (`.logo-brand` with uppercase letter spacing and `.logo-title` with bold gradient), matching the 38px icon height without truncation.
  2. Restricted collapsed sidebar CSS span-hiding to `span:not(.material-symbols-outlined)` and explicit `.nav-label`, ensuring `.material-symbols-outlined` remains visible, centered, and sized at `1.25rem`. Added `.nav-label` classes to text spans in `sidebar.html`.
- **Safety Net Added:**
  Added automated regression tests `testCollapsedSidebarPreservesMaterialIcons` and `testSidebarNavItemsHaveExplicitLabelsAndMaterialIcons` in `StaticResourceSyntaxTest.java` asserting that collapsed sidebar CSS preserves `.material-symbols-outlined`, that navigation items retain their semantic icons and labels, and that the brand logo contains both brand and title elements.
