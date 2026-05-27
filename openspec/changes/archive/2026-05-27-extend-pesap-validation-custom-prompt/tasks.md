## 1. Configuration Property Definition

- [x] 1.1 Add `neodymium.ai.pesap.custom.file` string property to `AiConfiguration.java` with `@Key` annotation
- [x] 1.2 Document the property's role and expected resolution behavior in `AiConfiguration.java`
- [x] 1.3 Add a commented placeholder setting for `neodymium.ai.pesap.custom.file` in the default `ai.properties` file

## 2. Custom Rules Resolution Logic

- [x] 2.1 Implement `CustomRulesLoader` utility class with a method to load file contents from classpath or filesystem
- [x] 2.2 Implement classpath lookup checking for resource existence using `ClassLoader.getResourceAsStream`
- [x] 2.3 Implement filesystem lookup checking for file existence using `java.nio.file.Files` or `java.io.File`
- [x] 2.4 Implement strict validation: if `neodymium.ai.pesap.custom.file` is configured but does not exist in classpath or filesystem, throw a configuration exception
- [x] 2.5 Implement the default fallback checks in order: `config/pesap-custom-rules.txt` (filesystem), then `ai-prompts/pesap-custom-rules.txt` (classpath)
- [x] 2.6 Implement thread-local programmatic override lookup using `Neodymium.getData().asString("neodymium.ai.pesap.custom.file")`
- [x] 2.7 Write comprehensive unit tests for `CustomRulesLoader` (covering classpath load, filesystem load, default fallbacks, programmatic overrides, and missing file exception)

## 3. Linter Prompt Injection and Logging

- [x] 3.1 Implement `getPesapLinterPrompt(final String customRules)` in `AiAgentPrompts.java` to append custom rules to `PESAP_LINTER_PROMPT`
- [x] 3.2 Update `AiAgent.java` (`runPesap` method) to resolve the custom rules file using `CustomRulesLoader`
- [x] 3.3 Pass the resolved rules to `AiAgentPrompts.getPesapLinterPrompt` and feed the combined prompt to the LLM during static analysis
- [x] 3.4 Add diagnostic logging in `AiAgent.java` showing where the custom rules file was loaded from and its contents (at debug level)

## 4. System Prompt Files Refactoring

- [x] 4.1 Use `git mv` to rename all prompt template/system files under `src/main/resources/ai-prompts/` from `.txt` to `.md`
- [x] 4.2 Update all static string references loading these prompts in `AiAgentPrompts.java` to use the `.md` file paths
- [x] 4.3 Update the standard base prompt loading mechanism `loadPrompt` in `AiAgentPrompts.java` to look for `.md` if applicable (or just load exactly the passed name)
- [x] 4.4 Verify all other code and test references to prompt `.txt` files are updated to `.md`
- [x] 4.5 Expand base semantic rules inside the new `pesap-linter-prompt.md` to check for repeating grids, class selectors/technical jargon, dynamic IDs, non-observable actions, and playbook assertion coverage

## 5. End-to-End Verification

- [x] 5.1 Write integration tests executing a full PESAP run with a configured custom rules file
- [x] 5.2 Verify that the custom rules are loaded dynamically, appended to the linter prompt, and successfully evaluated by the LLM
- [x] 5.3 Run all existing AI core tests to verify that prompt loading functions perfectly with the new `.md` files
