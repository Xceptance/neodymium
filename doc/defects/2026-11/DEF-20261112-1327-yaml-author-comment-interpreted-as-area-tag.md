# [DEF-20261112-1327] YAML @author Comment Inadvertently Interpreted as Area Tag

- **Status:** `Resolved`
- **Opened:** 2026-11-12 13:27
- **Closed:** 2026-11-12 13:30
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Playbook YAML files containing standard attribution comments such as `# @author Xceptance GmbH 2026` were erroneously assigned the area tag `@author` in console status views, interactive test states, and Aura reporting metrics.
- **Root Cause:** In `AuraFileService.java`, legacy heuristic regexes (`AREA_TAG_PATTERN = Pattern.compile("^\\s*#?\\s*@([a-zA-Z0-9_-]+)")` and `AREA_TAG_SECTION_HEADER_PATTERN`) were scanned across lines prior to the first section header (`steps:`, `before:`, etc.). Because `#?` made the comment character optional, `# @author` matched the regex and extracted `@author` as an area tag before the structured `tags:` block was evaluated.
- **Detection Gap ("What did we miss?"):** Existing test cases in `AuraFileServiceTest` explicitly asserted legacy support for `#@Checkout` and `@Checkout` without asserting that standard `# @author` Javadoc attribution comments must be ignored and never extracted as area tags.
- **Resolution:** Removed legacy `@tag` / `#@tag` regex extraction (`AREA_TAG_PATTERN`, `AREA_TAG_SECTION_HEADER_PATTERN`, and `normalizeBlockScalars` tag comment rewriting) from `AuraFileService.java`. Area tags are now exclusively derived from the structured YAML `tags` / `_tags` property.
- **Safety Net Added:** Added unit tests in `AuraFileServiceTest`: `testAuthorCommentDoesNotYieldAreaTag` and updated `testExtractAreaTagFromContent`, `testExtractAreaTagFromFileSystem`, and `testParsePlaybookSectionsWithAreaTag` asserting that `# @author ...` comments and legacy `#@tag` formats return `null` and only structured `tags:` definitions yield area tags.
