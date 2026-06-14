# Neodymium AI Direct Commands Reference Guide

Neodymium Aura AI supports **Direct Commands**, which bypass LLM reasoning and PESAP semantic analysis entirely. Direct commands execute 100% locally on the browser, offering extreme execution speed, cost-effectiveness (0 token usage), and complete determinism.

---

## 1. Routing & Casing Rules

The Neodymium AI engine routes steps based on the **case-sensitivity of the first word** of the instruction:

*   **Direct Command Route (All-Uppercase First Word)**
    *   **Rule**: The first word of the instruction contains only uppercase letters, digits, and underscores, and must contain at least one letter (matches regex `^[A-Z0-9_]+$` containing `.*[A-Z].*`).
    *   **Behavior**: Bypasses JIT PESAP analysis, bypasses the LLM, resolves the instruction locally using action plugins, and executes the actions directly.
    *   **Fail-Fast**: If the syntax is malformed or an element is missing, it fails immediately without self-healing.
    *   **Example**: `OPEN http://localhost:8080`, `CLICK #btn-submit`, `BACK`

*   **Natural Language Route (Mixed/Lowercase First Word)**
    *   **Rule**: The first word contains lowercase characters or does not meet the uppercase criteria.
    *   **Behavior**: Evaluates JIT PESAP classification, calls the LLM to understand and map the step, and uses semantic fallback/self-healing.
    *   **Example**: `Open http://localhost:8080`, `Click the submit button`, `back`

---

## 2. Syntax & Normalization

To ensure robust parsing, the direct command engine applies the following parser standards:
1.  **Whitespace Tolerance**: Leading and trailing whitespaces are stripped. Multiple spaces between words are collapsed to a single space.
2.  **Case-Insensitive Separators**: Words like `into` (used in `TYPE`), `in` (used in `SELECT`), and authorization descriptors (`with basic auth user`, `password`) are checked case-insensitively.
3.  **Selector Parsing (`SelectorParser`)**:
    *   **Default CSS**: If no prefix is provided, selectors are parsed as CSS (e.g., `#username`, `div.active`).
    *   **Prefixing**: Explicit CSS or XPath prefixes are supported case-insensitively (e.g., `css=#username`, `xpath=//button`).
    *   **Auto-Wrapping XPath**: XPath expressions not beginning with `/` or `(` are automatically wrapped in parentheses, e.g., `xpath=a[text()='Login']` becomes `(a[text()='Login'])`.

---

## 3. Direct Command Catalog

Neodymium provides 14 direct commands covering all standard browser automation interactions:

| Direct Command | Syntax & Description | Examples |
| :--- | :--- | :--- |
| **`OPEN`** / **`NAVIGATE`** | `OPEN <url>` / `NAVIGATE <url>`<br>Opens a URL. Supports optional basic HTTP authentication using the format `with basic auth user "<user>" password "<pass>"`. | `OPEN http://localhost:8080`<br>`NAVIGATE http://localhost:8080 with basic auth user "admin" password "secret"` |
| **`CLICK`** | `CLICK <selector>`<br>Clicks the element matched by the selector. | `CLICK #submit-btn`<br>`CLICK xpath=//button[@type='submit']` |
| **`TYPE`** | `TYPE "<value>" into <selector>` (or unquoted `<value>`)<br>Enters text value into the target element. | `TYPE "john@example.com" into #email`<br>`TYPE secret into css=#pwd` |
| **`SELECT`** | `SELECT "<value>" in <selector>` (or unquoted `<value>`)<br>Selects an option from a drop-down select element. | `SELECT "United States" in #country`<br>`SELECT red in #color` |
| **`CLEAR`** | `CLEAR <selector>`<br>Clears the text input or textarea element. | `CLEAR #search-input` |
| **`HOVER`** | `HOVER <selector>`<br>Moves the mouse pointer over the target element. | `HOVER #menu-dropdown` |
| **`WAIT`** | `WAIT <duration>`<br>Waits for a specified duration in milliseconds or seconds (marked with `s`). | `WAIT 500`<br>`WAIT 2.5s` |
| **`SCROLL`** | `SCROLL <selector>` / `SCROLL <direction>`<br>Scrolls the browser to an element, or scroll in a direction (`up` / `down`). | `SCROLL #footer`<br>`SCROLL down` |
| **`KEYPRESS`** | `KEYPRESS <key>` / `KEYPRESS <key1>, <key2>, ...`<br>Simulates pressing a key, combination (e.g., `Ctrl+A`), or sequence of keys. | `KEYPRESS Enter`<br>`KEYPRESS Ctrl+A`<br>`KEYPRESS Tab, Tab, Enter` |
| **`BACK`** | `BACK`<br>Navigates back one step in the browser history. | `BACK` |
| **`FORWARD`** | `FORWARD`<br>Navigates forward one step in the browser history. | `FORWARD` |
| **`REFRESH`** | `REFRESH`<br>Reloads the current page. | `REFRESH` |
| **`CLEAR_COOKIES`** | `CLEAR_COOKIES`<br>Deletes all cookies in the active browser session. | `CLEAR_COOKIES` |
| **`java:<method>`** | `java: <methodName>(<args>)` / `java: <methodName>`<br>Executes a Java method reflectively on the test instance class or utility classes. | `java: assertPriceGreaterThanZero("14.96 €")`<br>`java: verifyLessOrEqual("[\"10\", \"15\"]")` |

---

## 4. Fail-Fast & Self-Healing Policies

Direct commands prioritize immediate feedback over self-healing:

| Policy | Direct Commands (`CLICK`, `OPEN`, etc.) | Natural Language (`Click`, `Open`, etc.) |
| :--- | :--- | :--- |
| **LLM Call Required?** | **No** (unless offline is false and api key is absent) | **Yes** (calls remote LLM for reasoning) |
| **PESAP Run?** | **No** (completely bypassed) | **Yes** (classifies step and predicts tags/splitting) |
| **On Parser Failure** | Throws immediate `AssertionError` with `(direct shortcut):` | Falls back to LLM to interpret instruction |
| **On Execution Failure** | Throws original error immediately (e.g., `ElementNotFound`) | Escalates context level, heals DOM selectors, retries |

---

## 5. Offline & In-Memory Playbook Testing

Direct commands allow writing unit and integration tests completely offline without requiring playbooks stored on the filesystem.

You can initialize an in-memory playbook programmatically:
```java
// Setup a mock/in-memory playbook that records/replays execution locally
Neodymium.initializePlaybook(true); 
Playbook playbook = Neodymium.getAiPlaybook();
playbook.setRecording(false); // Enable playback mode
```
This enables full testing of direct command execution pipelines without disk I/O.
