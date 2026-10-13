# [DEF-20261012-1650] Visual Editor Token Pills Leak Icon Ligatures Corrupting Saved Playbook YAML

- **Status:** `Resolved`
- **Opened:** 2026-10-12 16:50
- **Closed:** 2026-10-12 16:54
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When adding a variable (`${var}`) or include step (`include: fragment.steps`) in the visual editor, the step renders correctly as formatted pills. However, upon saving and refreshing the page, all variables are replaced by raw text like `data_object <original name>` and include elements are replaced by raw text strings like `extension` and `expand_more`.
- **Root Cause:** During visual token formatting in `formatStepToTokens()`, variable placeholders and includes are converted into HTML containing Google Material Symbols icon ligatures (`data_object`, `extension`, `expand_more`). When saving the playbook, `compilePlaybookToYaml()` extracted step content via `(content.innerText || '')`. In browser DOM engines, `.innerText` evaluates the visible text of all descendant elements, thereby capturing icon ligatures as text while stripping `${` and `}` delimiters. This corrupted string was then written directly to disk via `/api/save`.
- **Detection Gap ("What did we miss?"):** Prior editor UI tests verified visual pill rendering (`.var-pill`) and tested folded scalar multiline text saving, but did not assert disk persistence and reload behavior for steps containing visual token pills.
- **Resolution:** Introduced a robust `getStepRawText(content)` helper that reads the raw unformatted string from the `data-raw` attribute, with an authoritative fallback that clones the DOM and removes all `.material-symbols-outlined` icon elements before extracting text. Updated `compilePlaybookToYaml()`, `checkAndUpdateCommentRow()`, `handleStepBlur()`, and the live `input` listener to consistently use `getStepRawText()` across both `aura-manager` and `neodymium-core`. Attached `data-var` and `data-raw` attributes to generated pills in `formatStepToTokens()`.
- **Safety Net Added:** Added automated Selenide UI test `AuraManagerEditorUiTest.testVariableAndIncludePersistenceAcrossSaveAndReload` verifying that playbooks containing variables and include pills save without icon ligatures and reload cleanly.
