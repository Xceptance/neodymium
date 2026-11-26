## Context

When Neodymium parses test data and step files using `YamlFileReader`, it resolves `_include` directives. Currently, if an AI agent is invoked from within a Java method using `ai.execute("""_include: src/...""")`, the framework checks if a test data YAML file exists for the current test case. If it does, the framework incorrectly assumes the `_include` path is relative to the directory containing that test data file. This leads to invalid paths being constructed, such as `logodata/tests/aura/kita/src/test/java/...`, which fails file lookup.

## Goals / Non-Goals

**Goals:**
- Fix the include path resolution in `YamlFileReader` so that paths specified inline (like `src/test/java/...`) resolve correctly when invoked from Java test code, regardless of whether a test data YAML file exists for that test case.

**Non-Goals:**
- We are not changing how regular file-to-file relative `_include` paths are resolved within actual YAML script files.

## Decisions

- **Path Resolution Context Check:** Modify `YamlFileReader` (or the underlying parser/include resolution logic) to distinguish the base directory when evaluating includes. If the source of the `_include` is an inline string (e.g. from Java code), it should use the project root (or standard class path) as the base resolution context rather than appending to the test data file's path.
- **Determine Source:** We might need to ensure that when `ai.execute()` passes the string to be parsed, `YamlFileReader` is not incorrectly seeded with the test data file as its `file` or `classpathResourcePath` context if the content being parsed is dynamically provided from code.

## Risks / Trade-offs

- **Risk**: Breaking existing `_include` paths within test data YAML files.
  - **Mitigation**: Ensure the fix specifically targets the code path where inline strings are parsed and evaluated, leaving the standard file-based YAML parsing intact.
