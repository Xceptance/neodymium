# [IDEA-20260717] Token & Cost Optimization: Condensed JSON Payloads

- **Status:** `Proposed`
- **Proposed:** 2026-07-17
- **Resolved:** Pending
- **Component:** `neodymium-core (ActionExtractionPrompt, VerificationPrompt)`
- **Category:** `Performance & Cost`
- **Author:** Neodymium Core Team

---

During step execution, the LLM is queried with the SUT state (accessibility tree or DOM) and instructions to extract the necessary actions. Currently, the LLM returns a structured, verbose JSON response defining the step status and the resulting actions list. 

A typical LLM response length is around **313 characters** (~100 output tokens) due to structural formatting (whitespace/indentation) and empty placeholder attributes:

```json
{
  "status" : "SUCCESS",
  "reasoning" : "Navigating to the specified URL as requested.",
  "actions" : [ {
    "action" : "NAVIGATE",
    "locator" : "",
    "value" : "http://localhost:43377/AssertActionTest/testAssertHappyPath.html",
    "reasoning" : "Navigate to the test page."
  } ]
}
```

By condensing this response structure, we can significantly reduce completion tokens, lowering LLM API costs and execution latency.

---

### Proposals

#### 1. Minified & Non-Empty Payloads (Recommended)
Instruct the LLM to output minified JSON (no formatting whitespace) and completely omit empty or optional fields (e.g., omitting `locator: ""` when not needed, and omitting action-level `reasoning` if top-level `reasoning` covers it).

* **Proposed Format**:
  ```json
  {"status":"SUCCESS","reasoning":"Navigating to URL.","actions":[{"action":"NAVIGATE","value":"http://localhost:43377/AssertActionTest/testAssertHappyPath.html"}]}
  ```
* **Impact**:
  * **Size**: **141 characters** (a **~55% reduction** in output tokens).
  * **Implementation**: Requires updates to system prompts instructing the model to output compact JSON and omit unused properties. No parser changes are needed as Jackson naturally ignores missing fields or handles nulls gracefully.

---

#### 2. Short Key Aliases in Prompt and Parser
Add support for short, single-character key mappings within the prompt definitions and the Java parser (`ActionExtractionPrompt.java` and `VerificationPrompt.java`).

* **Proposed Mappings**:
  * `status` $\rightarrow$ `st`
  * `reasoning` $\rightarrow$ `r`
  * `targetContextLevel` $\rightarrow$ `tc`
  * `actions` $\rightarrow$ `acts`
  * `action` $\rightarrow$ `a`
  * `locator` $\rightarrow$ `l`
  * `value` $\rightarrow$ `v`

* **Proposed Format**:
  ```json
  {"st":"SUCCESS","r":"Navigating.","acts":[{"a":"NAVIGATE","v":"http://localhost:43377/AssertActionTest/testAssertHappyPath.html"}]}
  ```
* **Impact**:
  * **Size**: **117 characters** (a **~63% reduction** in output tokens).
  * **Implementation**: Requires code changes in `ActionExtractionPrompt.java` to check for these short key names when parsing the JSON response.
