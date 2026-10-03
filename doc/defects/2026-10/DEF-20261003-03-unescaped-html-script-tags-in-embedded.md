# [DEF-20261003-03] Unescaped HTML/Script Tags in Embedded Step Data Payload Truncating Report JSON and Breaking Client JavaScript

- **Status:** Resolved
- **Opened:** 2026-10-03
- **Closed:** 2026-10-03
- **Component:** `neodymium-core` (`HtmlReportGenerator`)
- **Scope:** `Framework`
- **Symptom:**
  In HTML test execution reports containing DOM snapshots or scripts in step actions (e.g. `outerHtml` of `<script src="/shared/htmx.js"></script>`), step data and interactive details in the Step Inspector are completely missing or stuck on "Select a step". In the browser console, dozens of JavaScript syntax errors appear (`Uncaught SyntaxError: Invalid or unexpected token`), along with `Uncaught SyntaxError: Unterminated string in JSON` when parsing `#stepDataPayload`.
- **Root Cause:**
  `HtmlReportGenerator.appendClientScript` serialized `report.getSteps()` into raw JSON and appended it directly inside `<script id="stepDataPayload" type="application/json">` without escaping `<` characters. Under the HTML parser specification (HTML Standard § 13.2.5.4.10 "Script data state"), encountering `</script>` anywhere inside a `<script>` element immediately terminates the element, regardless of JSON string context. The remaining ~468 KB of JSON was ejected into the DOM as raw HTML markup, where embedded `<script>` tags were parsed as invalid executable JavaScript, and the truncated payload failed `JSON.parse`.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests for `HtmlReportGenerator` only tested synthetic steps with plain text instructions, without DOM actions containing `<script>` tags or HTML markup in `outerHtml` or action targets.
- **Resolution:**
  Sanitized embedded JSON in `HtmlReportGenerator.escapeJsonScriptPayload` by replacing all `<` characters with standard JSON Unicode escapes `\u003c` (`replace("<", "\\u003c")`), ensuring HTML parsers never see `</script>` or HTML tags/comments while `JSON.parse` preserves the original `<` characters identically. In addition, sanitized newline characters in `escapeAttr` to prevent inline JS attribute syntax breakage.
- **Safety Net Added:**
  Added `HtmlReportGeneratorTest.testEscapeJsonScriptPayloadWithHtmlAndScriptTags` and `testReportWithEmbeddedScriptTagsInSteps` validating that embedded `<script>` and `<!--` tags in steps are cleanly escaped, parse without errors in JSON/JS, and produce exactly 2 script tags in generated reports.
