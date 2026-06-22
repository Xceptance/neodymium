## ADDED Requirements

### Requirement: Configuration Property Resolution
The system SHALL support the configuration property `neodymium.ai.pesap.custom.file` to specify a custom rules file. It SHALL resolve the property value according to the following precedence order:
1. If the value matches an existing classpath resource, the system SHALL load the custom rules from that classpath resource.
2. If the value does not exist as a classpath resource but matches an existing file on the filesystem, the system SHALL load the custom rules from that filesystem file path.
3. If the property is set but the specified file does not exist on either the classpath or the filesystem, the system SHALL throw an initialization exception indicating the missing file.

The property value SHALL always be treated as a file path and never be interpreted directly as raw plain text rules.

#### Scenario: Configured classpath resource
- **WHEN** `neodymium.ai.pesap.custom.file` is configured as a classpath resource that exists
- **THEN** the system loads the contents of the classpath resource as the custom rules text

#### Scenario: Configured file path
- **WHEN** `neodymium.ai.pesap.custom.file` is configured as a path to a file on the filesystem that exists, and is not a classpath resource
- **THEN** the system loads the contents of the filesystem file as the custom rules text

#### Scenario: Configured file does not exist
- **WHEN** `neodymium.ai.pesap.custom.file` is configured, but does not exist on either the classpath or the filesystem
- **THEN** the system throws an initialization exception indicating that the custom rules file could not be found

### Requirement: Fallback Resolution Mechanisms
When the property `neodymium.ai.pesap.custom.file` is not explicitly set or is empty, the system SHALL check for default fallbacks in the following order:
1. If `config/pesap-custom-rules.txt` exists on the filesystem, it SHALL load the rules from this path.
2. Otherwise, if `ai-prompts/pesap-custom-rules.txt` exists on the classpath, it SHALL load the rules from this path.
3. If neither exists, the custom rules SHALL be empty (disabled state).

#### Scenario: Default filesystem fallback
- **WHEN** `neodymium.ai.pesap.custom.file` is not configured, and `config/pesap-custom-rules.txt` exists on the filesystem
- **THEN** the system loads the contents of `config/pesap-custom-rules.txt` as the custom rules text

#### Scenario: Default classpath fallback
- **WHEN** `neodymium.ai.pesap.custom.file` is not configured, `config/pesap-custom-rules.txt` does not exist, and `ai-prompts/pesap-custom-rules.txt` exists on the classpath
- **THEN** the system loads the contents of `ai-prompts/pesap-custom-rules.txt` as the custom rules text

#### Scenario: No custom rules
- **WHEN** `neodymium.ai.pesap.custom.file` is not configured, and default rules files do not exist
- **THEN** the system proceeds with no custom rules applied

### Requirement: Prompt Extension and Injection
The system SHALL dynamically inject the resolved custom rules into the Pre-Execution Static Analysis Phase (PESAP) linter system prompt during the static analysis phase. If custom rules are present, they SHALL be appended to the base linter prompt in the following format:
```markdown

### Custom Semantic Linting Rules
Additional custom linting rules defined for this project/environment:
<customRules>
```

#### Scenario: Prompt injection with custom rules
- **WHEN** custom rules are successfully resolved and present
- **THEN** the system appends the custom rules section to the base `PESAP_LINTER_PROMPT` and passes the extended prompt to the LLM during static analysis

### Requirement: Programmatic Dynamic Overrides
The system SHALL support dynamic thread-local programmatic overrides of `neodymium.ai.pesap.custom.file` using `Neodymium.getData().put(...)`.

#### Scenario: Programmatic override
- **WHEN** a test execution registers a programmatic override value for `neodymium.ai.pesap.custom.file` in `Neodymium.getData()`
- **THEN** the system resolves the rules according to the programmatic override value, bypassing the default static properties

### Requirement: Logging of Custom Rules
The system MUST log a message indicating that custom rules are loaded, detailing the resolved source location (filesystem, classpath, or default fallback) and their content at debug level.

#### Scenario: Logging source
- **WHEN** custom rules are loaded from `config/pesap-custom-rules.txt`
- **THEN** the system writes a log message specifying that custom rules were loaded from the filesystem path `config/pesap-custom-rules.txt`
