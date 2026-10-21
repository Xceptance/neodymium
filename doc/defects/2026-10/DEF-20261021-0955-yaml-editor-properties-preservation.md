# [DEF-20261021-0955] Visual Playbook Editor Overwrites Raw YAML and Erases Unmodeled Blocks

- **Status:** `Resolved`
- **Opened:** 2026-10-21 09:55
- **Closed:** 2026-10-21 11:00
- **Component:** `aura-manager` & `neodymium-core`
- **Scope:** `Framework & Doc/Spec`
- **Symptom:** Opening any YAML test playbook containing `_properties`, `properties`, `_meta`, `promptAddon`, `description`, lifecycle blocks (`beforeAll`, `afterAll`, `_onSuccess`, `_onFailure`), or header/section comments results in those sections disappearing completely, even when switching to the Raw YAML Editor. Saving from the editor permanently deletes those sections from the file on disk.
- **Root Cause:**
  1. `AuraFileService.parsePlaybookSections` parsed only `before`, `steps`, `after`, `variables`, and `data`, completely ignoring root-level `_properties`, `properties`, `_meta`, `promptAddon`, `description`, lifecycle hooks, and top-level variables.
  2. On initialization and on mode switch (`setEditorMode('raw')`), `dashboard-editor.js` called `compilePlaybookToYaml()` and unconditionally overwrote `#rawYamlTextarea.value` with the visually compiled YAML, destroying the original raw text delivered by the server before the user ever saw it.
  3. `compilePlaybookToYaml()` only serialized `before: |`, `steps: |`, `after: |`, and `data:` / `variables:`, permanently omitting all unmodeled sections upon save.
  4. Switching from Raw mode to Visual mode did not re-parse the raw YAML, leaving stale visual DOM.
- **Detection Gap ("What did we miss?"):** Existing UI and controller tests in `AuraManagerEditorUiTest` and `AuraTestEditorControllerTest` tested roundtrips exclusively with files that only contained `steps: |` and `data:`, never testing playbooks with `_properties`, `_meta`, or top-level comments.
- **Resolution:**
  1. Updated `AuraFileService.parsePlaybookSections()` to parse and extract `_properties` / `properties` as `propertiesYaml` and `properties`, `_meta` / `meta` as `metaYaml` and `meta`, `promptAddon` (`promptAddonYaml`), `description`, and `rootVariables` (any top-level keys not in reserved keyword list).
  2. Added support for lifecycle block aliases: `beforeAll`, `_beforeAll`, `beforeEach`, `_beforeEach`, `afterAll`, `_afterAll`, `afterEach`, `_afterEach`, `_steps`, and `_data`.
  3. Updated `AuraFileService.validateFileContent()` to consider `rootVariables` so declared top-level variables aren't falsely flagged as undefined.
  4. Updated `AuraTestDashboardController` and `AuraTestEditorController` to expose `propertiesYaml`, `metaYaml`, `description`, `promptAddonYaml`, and `rootVariables` to the frontend model. Also sanitized `file` and `relativePath` query parameters to prevent comma-delimited duplication caused by redundant parameters.
  5. Added UI components in `editor.html`: `#playbookPropertiesPanel` (editable textarea for YAML properties) and `#addPropertiesBtnContainer` (with "Add '_properties:' Block" button for playbooks lacking one).
  6. Updated `dashboard-editor.js`:
     - Serializes `_properties:` block at the top of the YAML output in `compilePlaybookToYaml()` when present.
     - Preserves `#rawYamlTextarea.value` on initial load without premature overwrite.
     - Re-syncs visual editor from raw YAML via POST `/api/editor` when switching from Raw to Visual mode if modifications were made.
     - Tracks `propertiesYaml` and `hasProperties` in undo/redo snapshot stack (`getEditorSnapshot()` / `restoreEditorSnapshot()`).
     - Added `addPropertiesBlock()` and `removePropertiesBlock()` controls.
- **Safety Net Added:**
  - `AuraFileServiceTest#testParsePlaybookSectionsWithPropertiesAndMeta`: Unit test validating extraction of `_properties`, `_meta`, `description`, and `promptAddon`.
  - `AuraFileServiceTest#testParsePlaybookSectionsWithRootVariablesAndLifecycleBlocks`: Unit test validating extraction of `beforeAll`/`beforeEach`, `afterAll`/`afterEach`, and root-level variables.
  - `AuraFileServiceTest#testValidateFileContentRecognizesRootVariablesAndProperties`: Validates that top-level root variables do not trigger false undefined-variable warnings.
  - `AuraTestEditorControllerTest#testGetEditorFragmentPopulatesPropertiesAndMetaYaml`: Verifies controller populates `propertiesYaml`, `metaYaml`, and `description` in model attributes.
  - `AuraTestEditorControllerTest#testPostEditorFragmentWithCustomContent`: Verifies POST to `/api/editor` parses and exposes updated properties without re-reading disk.
  - `AuraManagerEditorUiTest#testPlaybookPropertiesBlockPreservedAndEditableInVisualAndRawModes`: Full browser E2E test verifying that `_properties` block is displayed in visual textarea, preserved when toggling between visual and raw modes, and persisted to disk upon saving.
  - `AuraManagerEditorUiTest#testAddAndRemovePlaybookPropertiesBlockInVisualEditor`: Full browser E2E test verifying that "Add '_properties:' Block" button adds the section, custom properties are saved, and "Remove Block" removes it and clears from saved YAML.
