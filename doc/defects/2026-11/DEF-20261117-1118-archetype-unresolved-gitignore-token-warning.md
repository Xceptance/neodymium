# [DEF-20261117-1118] Maven Archetype Unresolved Gitignore Token Warning

- **Status:** `Resolved`
- **Opened:** 2026-11-17 11:18
- **Closed:** 2026-11-17 11:21
- **Component:** `neodymium-archetype`
- **Scope:** `Infra/Build`
- **Symptom:** Running `mvn archetype:generate -DarchetypeGroupId=com.xceptance -DarchetypeArtifactId=neodymium-archetype` output the warning `[WARNING] Property 'gitignore' was not specified, so the token in '//__gitignore__' is not being replaced.` during project creation.
- **Root Cause:** Maven Archetype Plugin automatically scans file and directory paths for tokens wrapped in double underscores (`__<propertyName>__`) and attempts property substitution. When the `gitignore` property was removed from `<requiredProperties>` in `archetype-metadata.xml` to avoid prompting users during interactive generation, the template resource retained the name `__gitignore__`. As a result, the plugin detected an unresolved token named `gitignore`, logged a warning, and left the file named `__gitignore__` until `archetype-post-generate.groovy` renamed it.
- **Detection Gap ("What did we miss?"):** Archetype integration tests ran in batch mode where the generator succeeded (because Groovy completed the rename to `.gitignore`), but test assertions did not verify that temporary token files were absent or that property warning tokens were completely eliminated from resource filenames.
- **Resolution:**
  - Renamed `src/main/resources/archetype-resources/__gitignore__` to `src/main/resources/archetype-resources/gitignore`, removing the `__...__` token delimiter.
  - Updated `src/main/resources/META-INF/maven/archetype-metadata.xml` to include `gitignore`.
  - Updated `src/main/resources/META-INF/archetype-post-generate.groovy` to rename `gitignore` to `.gitignore`.
- **Safety Net Added:** Added `neodymium-archetype/src/test/resources/projects/basic-test/verify.groovy` to assert that `.gitignore` exists, `gitignore` was cleanly renamed, `__gitignore__` does not exist, and `run-aura.sh` is executable.
