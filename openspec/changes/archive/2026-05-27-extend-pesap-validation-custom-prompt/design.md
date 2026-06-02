## Context

Currently, the Pre-Execution Static Analysis Phase (PESAP) semantic linter uses a fixed set of system rules (defined in `pesap-linter-prompt.txt`) to check natural language test steps for anti-patterns before executing tests. However, different teams, environments, and test projects accumulate specific mistakes, learnings, and custom anti-patterns over time. Adding a way to inject a custom prompt extension allows projects to customize the PESAP rules dynamically based on their specific domain requirements and historical learnings.

## Goals / Non-Goals

**Goals:**
- Provide a configuration property `neodymium.ai.pesap.custom.file` to specify a custom rules file that is loaded from the classpath or filesystem.
- Automatically load custom rules from default fallback paths (`config/pesap-custom-rules.txt` on the filesystem or `ai-prompts/pesap-custom-rules.txt` on the classpath) when no explicit configuration is provided.
- Dynamically inject the custom rules block into the PESAP linter system prompt during the static analysis phase.
- Log clear messages indicating whether custom rules are loaded, where they were loaded from, and their contents at debug level.

**Non-Goals:**
- Dynamically updating or overriding the standard system exploration prompts during browser runtime execution (out of scope, this change is strictly for the Pre-Execution Static Analysis Phase).
- Allowing raw natural language rules text to be provided directly inline in the configuration property (it must always reference a valid file).

## Decisions

### Decision 1: Custom Rules Loading Precedence and Fallbacks
To provide maximum flexibility and seamless integration, the custom rules resolver will evaluate sources in the following precedence order:
1. **Explicit Property File Path:** Look up `neodymium.ai.pesap.custom.file` from the active configurations.
   - If the property is set, it MUST point to a valid file.
   - Check if the value matches an existing classpath resource (e.g. `ai-prompts/my-rules.txt`), and if so, read it from the classpath.
   - Otherwise, check if the value matches an existing file path (e.g. `config/pesap-custom-rules.txt`), and if so, read it from the filesystem.
   - If configured but the file does not exist in either classpath or filesystem, throw an initialization exception.
2. **Default Filesystem Fallback:** If the configuration property is empty, check if `config/pesap-custom-rules.txt` exists on the filesystem and load it.
3. **Default Classpath Fallback:** Check if `ai-prompts/pesap-custom-rules.txt` exists on the classpath and load it.
4. **Disabled State:** If none of the above are defined or exist, proceed with the default standard linter rules only.

*Alternative Considered:* Allowing inline plain text in the property. However, this is messy and hard to read for multi-line rules. Restricting the property strictly to a file path (classpath or filesystem) guarantees structured rules management.

### Decision 2: Prompt Extension via Dynamic Appending
Instead of making `AiAgentPrompts.PESAP_LINTER_PROMPT` mutable or complex, we will keep it as a `static final` base template and introduce a helper method `AiAgentPrompts.getPesapLinterPrompt(final String customRules)`:
- When custom rules are absent, return `PESAP_LINTER_PROMPT`.
- When present, return the original prompt appended with:
  ```markdown
  
  ### Custom Semantic Linting Rules
  Additional custom linting rules defined for this project/environment:
  <customRules>
  ```
This structure ensures clean separation of concerns and guarantees the LLM treats custom rules as high-priority constraints.

### Decision 3: Programmatic Dynamic Overrides
Support thread-local dynamic overrides using Neodymium's standard programmatic mechanism (e.g. `Neodymium.getData().put("neodymium.ai.pesap.custom.file", "config/my-custom-rules.txt")`). This enables target-specific rule variations during dynamic or parallel test runs.

### Decision 4: Rename System Prompt Files to Markdown (.md)
To improve IDE readability, syntax highlighting, and formatting of AI instructions, all prompt template and system prompt files located under `src/main/resources/ai-prompts/` will be renamed from `.txt` to `.md`. 
Correspondingly, all static string references loading these prompts in `AiAgentPrompts.java` will be updated to point to the `.md` extensions.

### Decision 5: Expand Core PESAP Linter Prompt Rules
The default base semantic linter rules defined inside the new `pesap-linter-prompt.md` will be expanded to detect the following critical test-automation issues:
1. **Ambiguous List/Grid Interactions**: Flags steps asking to act on generic elements in rows/lists without row anchors or index identifiers (preventing flakiness).
2. **Hardcoded Dynamic IDs**: Detects fragile dynamic elements like database IDs or dynamic tokens that change between seeds/runs.
3. **Technical DOM Jargon/Class Selectors**: Detects mixing CSS/XPath selectors directly into step descriptions, encouraging descriptive text and moving code selectors to Neodymium's standard `(hint: ...)` tag.
4. **Non-Observable Actions**: Detects mental actions (e.g. "Understand", "Decide") that have no observable browser counterpart.
5. **Absence of Playbook Assertions**: Checks if the entire playbook has zero validations/assertions, warning the user that the test case fails to assert page correctness.

## Risks / Trade-offs

- **[Risk]** Large custom rules files could consume significant tokens or cause LLM context limit issues.
  - *Mitigation:* Document optimal custom rules size limits in `ai.properties`. PESAP models generally have very large context windows (e.g. Gemini 2.5/3.5 models), so standard rules lists will consume negligible tokens.
- **[Risk]** Inconsistent file paths on different execution environments (CI vs local).
  - *Mitigation:* The hybrid classpath and filesystem resolution ensures rules packaged in the test JAR (classpath) or placed in the execution workspace (filesystem) load reliably regardless of execution mode.
