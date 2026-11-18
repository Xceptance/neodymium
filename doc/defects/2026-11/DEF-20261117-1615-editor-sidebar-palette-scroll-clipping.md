# [DEF-20261117-1615] Visual Editor Sidebar Palette Scroll Clipping and Fragment Disappearance

- **Status:** `Resolved`
- **Opened:** 2026-11-17 16:15
- **Closed:** 2026-11-17 17:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the visual playbook editor, the right sidebar tools palette (`.sidebar-palette-right`) could not be scrolled down to view the step fragments list and search bar. The scroll container stopped at Category 4 header ("AVAILABLE STEP FRAGMENTS (.STEPS)") with the scrollbar's down arrow and track clipped off. Additionally, when fragment folders were collapsed, Category 4 disappeared from the visible viewport.
- **Root Cause:** Two interacting CSS height defects:
  1. *Container overflow:* In `dashboard-styles.css`, `.visual-editor-container` specified `height: 100%` alongside `flex: 1 1 0%; min-height: 0;` inside `.editor-container`, causing `#visualEditorContainer` to overflow `.editor-container` by the height of `.panel-header` (~50px–80px), clipped by `overflow: hidden`.
  2. *Bare tag selector leakage:* In `report-manager.css`, a generic `aside { width: 260px; height: 100vh; max-height: 100vh; ... }` rule intended for `#mainSidebar` leaked globally onto `<aside class="sidebar-palette-right">`. This forced `.sidebar-palette-right` to `100vh` (1080px in 1080p display) within an 815px panel, causing 265px of its scrollable bottom (where `#editorFragmentSearchInput` and `#fragmentCardsContainer` reside) to extend beyond `#editorPanel` and be clipped by `overflow: hidden`.
- **Detection Gap ("What did we miss?"):** UI tests in `AuraManagerEditorUiTest` searched for elements using Selenide's DOM matching without checking whether `.sidebar-palette-right` itself was overflowing `#editorPanel` or whether elements at the bottom of the palette are within the visible bounding client rect of `.sidebar-palette-right` when scrolled to the bottom.
- **Resolution:**
  1. Removed conflicting `height: 100%` from `.visual-editor-container`, `.editor-main-area`, and `.editor-raw-panel` in `dashboard-styles.css`.
  2. Scoped bare `aside` and `aside.collapsed` selectors in `report-manager.css` strictly to `aside#mainSidebar`.
  3. Explicitly defined `height: 100%; max-height: 100%; min-height: 0; padding: 16px 16px 36px 16px;` on `.sidebar-palette-right` in `dashboard-styles.css`.
  4. Added `id="editorSidebarPalette"` to `<aside class="sidebar-palette-right">` and `#editorFragmentCategory` to Category 4 in `editor.html`.
- **Safety Net Added:** Added and extended UI regression test `AuraManagerEditorUiTest.testEditorSidebarPaletteScrollableAndFragmentsVisibleWhenCollapsed` verifying:
  - `#visualEditorContainer` does not overflow `#editorPanel` bottom (`diff <= 2.0`).
  - `.sidebar-palette-right` does not overflow `#visualEditorContainer` bottom (`diff <= 2.0`).
  - Scrolling palette to bottom brings `#editorFragmentSearchInput` or fragment cards inside the visible palette viewport (`search.bottom <= palette.bottom + 2.0`).
  - Category 4 remains visible within the palette viewport when folder groups are collapsed.
