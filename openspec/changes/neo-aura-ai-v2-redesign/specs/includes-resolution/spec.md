## MODIFIED Requirements

### Requirement: Classpath Resolution Bypass
When discovering YAML test scripts in the classpath, the YAML parser SHALL delegate relative inclusion path resolution and resource stream loading directly to the configured `PlaybookResourceManager` (e.g. `ClasspathResourceManager` or `LocalFileResourceManager`), and the parser itself MUST NOT perform protocol checks (`file:` or `jar:file:`) or copy resources to temporary directories.

#### Scenario: Local Maven/IDE execution bypass
- **WHEN** relative includes are encountered during classpath playbook parsing
- **THEN** the parser delegates resolution to the `ClasspathResourceManager` using the active loader stream, resolving paths relative to the package namespace without file copying.
