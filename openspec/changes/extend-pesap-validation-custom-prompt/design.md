## Context

Currently, the Pre-Execution Static Analysis Phase (PESAP) semantic linter uses a fixed set of system rules (defined in `pesap-linter-prompt.txt`) to check natural language test steps for anti-patterns before executing tests. However, different teams, environments, and test projects accumulate specific mistakes, learnings, and custom anti-patterns over time. Adding a way to inject a custom prompt extension allows projects to customize the PESAP rules dynamically based on their specific domain requirements and historical learnings.

## Goals / Non-Goals

**Goals:**
- Provide a configuration property `neodymium.ai.pesap.customRules` to specify direct custom rules or load a custom rules file from the classpath or filesystem.
- Automatically load custom rules from default fallback paths (`config/pesap-custom-rules.txt` on the filesystem or `ai-prompts/pesap-custom-rules.txt` on the classpath) when no explicit configuration is provided.
- Dynamically inject the custom rules block into the PESAP linter system prompt during the static analysis phase.
- Log clear messages indicating whether custom rules are loaded, where they were loaded from, and their contents at debug level.

**Non-Goals:**
- Dynamically updating or overriding the standard system exploration prompts during browser runtime execution (out of scope, this change is strictly for the Pre-Execution Static Analysis Phase).
- Validating the syntax of the user-provided custom rules (treated as raw natural language instructions injected directly into the LLM system prompt).

## Decisions

### Decision 1: Custom Rules Loading Precedence and Fallbacks
To provide maximum flexibility and seamless integration, the custom rules resolver will evaluate sources in the following precedence order:
1. **Explicit Property String / File Path:** Look up `neodymium.ai.pesap.customRules` from the active configurations.
   - If the value matches an existing file path (e.g. `config/pesap-custom-rules.txt`), read it from the filesystem.
   - If the value matches an existing classpath resource (e.g. `ai-prompts/my-rules.txt`), read it from the classpath.
   - Otherwise, treat the value as direct raw natural language rules text.
2. **Default Filesystem Fallback:** If the configuration property is empty, check if `config/pesap-custom-rules.txt` exists on the filesystem and load it.
3. **Default Classpath Fallback:** Check if `ai-prompts/pesap-custom-rules.txt` exists on the classpath and load it.
4. **Disabled State:** If none of the above are defined or exist, proceed with the default standard linter rules only.

*Alternative Considered:* Only allowing file paths or only allowing direct text. Allowing both with a fallback mechanism offers the best developer experience, allowing teams to quickly write dynamic rule overrides inline in property files or maintain a clean separate rules file.

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
Support thread-local dynamic overrides using Neodymium's standard programmatic mechanism (e.g. `Neodymium.getData().put("neodymium.ai.pesap.customRules", "my-rules")`). This enables target-specific rule variations during dynamic or parallel test runs.

## Risks / Trade-offs

- **[Risk]** Large custom rules files could consume significant tokens or cause LLM context limit issues.
  - *Mitigation:* Document optimal custom rules size limits in `ai.properties`. PESAP models generally have very large context windows (e.g. Gemini 2.5/3.5 models), so standard rules lists will consume negligible tokens.
- **[Risk]** Inconsistent file paths on different execution environments (CI vs local).
  - *Mitigation:* The hybrid classpath and filesystem resolution ensures rules packaged in the test JAR (classpath) or placed in the execution workspace (filesystem) load reliably regardless of execution mode.
