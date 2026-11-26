# [DEF-20260930-17] ClassCastException when parsing playbook fragments with top-level YAML array list

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks containing included fragment `.steps` files (or any YAML file structured as a top-level list `- ...`) fails with `java.lang.RuntimeException: Failed to parse playbook: <path> Caused by: java.lang.ClassCastException: class java.util.ArrayList cannot be cast to class java.util.Map`.
- **Root Cause:** In `YamlPlaybookParser.java`, SnakeYAML's `yaml.load(fileContent)` was directly assigned to a `Map<String, Object>` variable. When an included fragment file (such as `checkout-with-paypal.steps` or `proceed-to-payment.steps`) contains a top-level YAML list (`- step1\n- step2`), `yaml.load(fileContent)` returns a `java.util.ArrayList`, causing an unhandled `ClassCastException`.
- **Detection Gap ("What did we miss?"):** Existing `YamlPlaybookParserTest` unit tests only tested YAML files where the top-level structure was a YAML dictionary (e.g., `steps: ...`). There were no test cases for included fragment `.steps` files structured as top-level YAML array lists (`- ...`).
- **Resolution:** Updated `YamlPlaybookParser.java` to capture the output of `yaml.load(fileContent)` as `Object loadedObject`. If `loadedObject` is a `Map<?, ?>`, process standard top-level keys (`data`, `steps`, `before`, `after`). If `loadedObject` is a `List<?>` or `String`, delegate directly to `parseStepBlock`.
- **Safety Net Added:** Added unit test `testParseIncludedStepListFragment()` in `YamlPlaybookParserTest.java` validating recursive inclusion of fragment `.steps` files containing top-level YAML array lists.
