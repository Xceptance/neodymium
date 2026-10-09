# [DEF-20261009-1225] Jackson Serializes linterFindings JsonNode as POJO Metadata Instead of Array Elements

- **Status:** `Resolved`
- **Opened:** 2026-10-09 12:25
- **Closed:** 2026-10-09 12:27
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager run reports, Pre-Flight and Post-Flight findings recorded in `console-execution-*.json` do not display in the test variation side panel drawer.
- **Root Cause:** In `TestExecutionDto.java`, `linterFindings` and `postFlightFindings` getters returned `JsonNode` without `@JsonRawValue`. Spring Boot's Jackson message converter serialized the `JsonNode` bean getters (`isArray()`, `getNodeType()`), yielding an object `{ "array": true, "nodeType": "ARRAY" }` instead of the JSON array elements. As a result, the frontend check `Array.isArray(findings)` evaluated to `false` and hid the findings drawer cards.
- **Detection Gap ("What did we miss?"):** `TestExecutionDtoTest` tested Jackson deserialization (`readValue`), but did not assert HTTP/JSON serialization output (`writeValueAsString`).
- **Resolution:**
  1. Annotated `getLinterFindings()` and `getPostFlightFindings()` with `@JsonIgnore` and provided `@JsonProperty("linterFindings") @JsonRawValue public String getLinterFindingsRaw()`.
  2. Added stringified getters `getLinterFindingsJson()` and `getPostFlightFindingsJson()` in `TestExecutionDto.java` matching the pattern used by `getBlocksJson()` and `getLlmResponsibilityJson()`.
  3. In `report-manager.js`, implemented `extractFindingsArray(raw)` to seamlessly parse both raw arrays and JSON-encoded strings.
- **Safety Net Added:** Added assertions in `TestExecutionDtoTest.java` verifying that `objectMapper.writeValueAsString(dto)` serializes `linterFindings` and `postFlightFindings` as true JSON arrays with all finding fields preserved.
