# Reusable Test Blocks & Conditional Includes

Neodymium AI allows modularizing your test playbooks to avoid duplication of common action sequences. This guide explains how to use basic includes and conditional (LLM-delegated) includes, their underlying Abstract Syntax Tree (AST) compilation, variable resolution, and debugging practices.

---

## 1. Basic Includes

Basic includes are static references to external files containing a set of natural language steps. They are specified using the `_include` syntax in a playbook's steps list.

### Usage Example

**Main Playbook File (`posters_tests/purchaseFlowTest.yml`):**
```yaml
steps: |
  Navigate to https://example.com/store.
  _include: fragments/login-user.steps
  Search for "Product A" and add it to cart.
  _include: fragments/checkout.steps
```

**Included File (`posters_tests/fragments/login-user.steps`):**
```text
Click on the Login link.
Type 'john.doe@example.com' into the Email address input.
Type 'password123' into the Password input.
Click the login submit button.
```

When compiled, all the steps from `login-user.steps` and `checkout.steps` are inline-expanded directly into the main execution list.

---

## 2. Conditional / Branching Includes

Conditional includes allow the execution path to branch dynamically based on runtime conditions (e.g., dismissing optional cookie banners, handling different product page layouts, selecting payment methods).

### Syntax Example

In your natural language playbook steps, express your branching conditions using an `If... then... else...` format:

```yaml
steps: |
  Navigate to the PDP page.
  If the page is an XPDP layout then _include: fragments/configure-xpdp.steps, else _include: fragments/configure-simple-product.steps
  Click add to cart.
```

---

## 3. Under the Hood: AST Compilation & AST Formats

During the initial generation mode, the LLM analyzes the conditional statement and compiles it into an Abstract Syntax Tree (AST). 

Instead of simple string matchers, the compiler maps the statement to two distinct native action types:
* **`BRANCH` Action**: Evaluates the condition (e.g., checks if a specific element or text exists).
* **`INCLUDE` Action**: Loads steps from the referenced file and executes them if the branch is taken.

### Compiled JSON Playbook Structure
The playbook caches the structure of the AST so it can be replayed 100% offline in CI/CD. Here is what the compiled JSON looks like:

```json
{
  "steps": [
    {
      "action": "NAVIGATE",
      "value": "https://dev.stokke.com/AUT/de-at/"
    },
    {
      "action": "BRANCH",
      "condition": "the product is xpdp",
      "then": [
        {
          "action": "INCLUDE",
          "path": "fragments/configure-xpdp-product-steps.steps"
        }
      ],
      "else": [
        {
          "action": "INCLUDE",
          "path": "fragments/add-simple-product-to-cart.steps"
        }
      ]
    },
    {
      "action": "CLICK",
      "value": "Go to cart"
    }
  ]
}
```

---

## 4. Path Resolution Flow

When an `INCLUDE` action is executed at runtime:
1. The engine checks for a localized `neodymium.classpathResourcePath` in the current thread-local dataset.
2. If absent, the engine falls back to `Neodymium.getTestdataSourceFile()`.
3. Using this context, the parent directory of the original playbook (e.g. `posters_tests/`) is calculated.
4. The target path (e.g., `fragments/add-simple-product-to-cart.steps`) is resolved relative to that directory structure (e.g. `posters_tests/fragments/add-simple-product-to-cart.steps`).
5. The resource is read either directly from the classpath or from the local filesystem directories.

### Nested Include Paths

For nested includes (when an included file contains another `_include` directive), **the path must always be specified relative to the file containing the include**:
* If `posters_tests/main.yml` includes `fragments/add-simple-product-to-cart.steps` (using path `fragments/add-simple-product-to-cart.steps`), and `add-simple-product-to-cart.steps` in turn includes `save-product-info.steps` (both located under `posters_tests/fragments/`), the path in `add-simple-product-to-cart.steps` must be:
  ```text
  _include: save-product-info.steps
  ```
  Specifying `fragments/save-product-info.steps` is incorrect because it is resolved relative to the folder of the including file (`fragments/`), which would look for `fragments/fragments/save-product-info.steps` and fail.

---

## 5. Variable Resolution & State Inheritance inside Includes

Steps inside included files execute in the exact same context as the main playbook flow. They have complete access to the test case's state:

### 1. Static Test Data
Variables injected via the YAML dataset or Java properties (e.g. `${username}`) are resolved dynamically.
**Example fragment (`fragments/login.steps`):**
```text
Type '${username}' into the Email address input.
Type '${password}' into the Password input.
```

### 2. Runtime Stored Variables
Any runtime variable captured during the test flow via the `STORE` action (e.g., saving an order number or product name) is fully inherited by the included steps.
For example, if you capture a variable in the main flow:
```text
Capture the order confirmation number. Save it as variable 'generatedOrderNo'.
_include: fragments/verify-order.steps
```
You can reference `${generatedOrderNo}` directly inside `verify-order.steps`:
```text
Navigate to the order tracking page.
Type '${generatedOrderNo}' into the Order Number field.
Click Search.
```

---

## 6. Mixing Includes & Circular Dependency Checks

### Mixing Static & Dynamic Inclusions
You can mix and nest inclusions as needed:
* A dynamically compiled `INCLUDE` action (within a conditional branch) can load a file containing static `_include` directives.
* Statically included files can contain dynamic conditional `If/then/else` branching logic targeting other includes.

### Circular Loop Prevention
To prevent infinite recursion, the compiler and execution runner track the call stack of active inclusions. If an include file references itself (either directly or transitively via a cycle), the system terminates immediately with a `MalformedPlaybookException` detailing the include sequence:
`Circular inclusion detected: fragments/a.steps -> fragments/b.steps -> fragments/a.steps`

To ensure robust detection, all path arguments are canonicalized (for filesystem-based playbooks) or normalized (for classpath-based playbooks). This prevents bypassing the guard when loop references utilize differing relative path formats (e.g. referencing `fragments/B.steps` and `fragments/./B.steps` or utilizing `..` directories).

---

## 7. Debugging & Tracing

When an error occurs during execution of an included step, it is critical to know exactly which file and line caused the failure.

Neodymium automatically generates a traceback breadcrumb for every executed step:
```text
Unexpected error executing step: Click add to cart
        at fragments/configure-xpdp.steps:5 -> posters_tests/purchaseFlowTest.yml:2
```

This stack trace lets you instantly pinpoint the origin of any failed assertion or selector inside nested folders of reusable steps.
