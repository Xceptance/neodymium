# [DEF-20261104-1205] Jackson Serializes blocks and steps JsonNode as POJO Metadata Causing "No step execution data recorded"

- **Status:** `Resolved`
- **Opened:** 2026-11-04 12:05
- **Closed:** 2026-11-04 12:07
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager run reports and variation side panels, completed tests display "No step execution data recorded for this run." in grey despite `console-execution-*.json` on disk containing all executed steps (e.g. 24 steps in Stokke PayPal test).
- **Root Cause:** In `TestExecutionDto.java`, `getBlocks()` and `getSteps()` returned `JsonNode` without `@JsonIgnore` or `@JsonRawValue`. When `@JsonIgnore` was added to `getBlocksJson()` in commit `0cf9daaea` to eliminate duplicate serialized payloads, Spring Boot's Jackson message converter fell back to serializing the JavaBean getter methods on `com.fasterxml.jackson.databind.node.ObjectNode` (`isArray()`, `isContainerNode()`, `getNodeType()`), yielding `{ "array": false, "containerNode": true, "nodeType": "OBJECT" }` instead of the JSON step tree. Consequently, the frontend found no `steps`, `before`, or `after` arrays and displayed the empty execution message.
- **Detection Gap ("What did we miss?"):** `TestExecutionDtoTest` tested `tree.has("blocks")` without asserting the contents and structure of the serialized node, which evaluated to `true` even when Jackson wrote the POJO metadata object.
- **Resolution:**
  1. In `TestExecutionDto.java`, annotated `getBlocks()` and `getSteps()` with `@JsonIgnore`.
  2. Provided `@JsonProperty("blocks") @JsonRawValue public String getBlocksRaw()` and `@JsonProperty("steps") @JsonRawValue public String getStepsRaw()` to stream the raw JSON tree directly.
  3. Delegated `getBlocksJson()` and `getStepsJson()` to `getBlocksRaw()` / `getStepsRaw()` while retaining `@JsonIgnore`.
  4. In `report-manager.js`, added defensive validation in `extractBlocksJson` and `extractStepsJson` to detect and reject Jackson POJO metadata leaks.
- **Safety Net Added:** Added assertions in `TestExecutionDtoTest.testBlocksAndStepsJsonAreIgnoredDuringSerialization` verifying that `objectMapper.writeValueAsString(dto)` serializes `blocks` as an unescaped JSON object containing the `steps` array, rejects POJO metadata flags (`array`, `containerNode`, `nodeType`), and verifies complete round-trip deserialization back to `TestExecutionDto`.
