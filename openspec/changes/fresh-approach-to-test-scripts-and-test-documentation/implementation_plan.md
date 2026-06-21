# Implementation Plan: Fresh Approach Updates (Implicit Dynamic, Java Injection API, and TestId Removal)

This plan updates the technical specifications and design concept of the new test scripting framework to support:
1. **Implicitly Dynamic `store` Variables:** Variables captured at runtime via `store` actions are automatically marked as dynamic without requiring YAML declaration.
2. **Java Programmatic Variable Injection & Retrieval API:** An API (`Neodymium.data()`) to inject variables with their classification context (`constant`, `normal`, `sensitive`, `dynamic`) directly from JUnit setup code, bypassing the YAML.
3. **Unified Context Precedence Hierarchy:** Resolving variables via `Neodymium.data().get()` and `.exists()` by traversing programmatic scopes, active dataset row context, static playbook metadata, and configuration defaults.
4. **Removal of Dataset-level `testId`/`dataId`:** Formally dropping manual row-level identifiers in favor of automated dimension-based names.

---

## Proposed Specification Changes

### 1. Update [spec.md](specs/fresh-approach-to-test-scripts-and-test-documentation/spec.md)
*   **Java Programmatic Injection & Retrieval API Requirements:**
    *   Add formal requirements specifying the `Neodymium.data().[scope]().set(name, value)` and `.get(name)` API.
    *   Ensure injected variables do not require YAML declarations to be recognized under their respective scopes.
*   **Implicitly Dynamic `store` Variables:**
    *   Formally specify that runtime variables created by `store` steps are implicitly registered as dynamic.
*   **Test ID / Data ID Deletion:**
    *   Document that `testId` / `dataId` are no longer required in dataset mappings, as Cartesian coordinates serve as implicit identifiers.

### 2. Update [design.md](design.md)
*   Update the variable classification table to include programmatic injection and `store` action variables.
*   Document the Java API design and its internal representation.
*   Elaborate on why `testId` / `dataId` are dropped at the dataset level.

### 3. Update [comprehensive_demo.yaml](comprehensive_demo.yaml)
*   Remove the `_testId` key from `_meta` and clean up its references.
*   Remove `baseUrl` from the `_dynamic` list. Add comments illustrating how `baseUrl` is instead injected dynamically via Java.
*   Add a step demonstrating a `store` action and a subsequent verification using the stored variable.

---

## Detailed Design: Java Programmatic Injection & Retrieval API

We propose the following namespace-based API in the `Neodymium` context facade:

```java
package com.xceptance.neodymium.util;

public final class Neodymium
{
    public static NeodymiumData data()
    {
        return getContext().neodymiumData;
    }
}
```

The classifications are exposed through distinct scopes on the `NeodymiumData` and `DataScope`/`SensitiveDataScope` interfaces, which also include root-level hierarchy-aware resolution methods:

```java
package com.xceptance.neodymium.common.testdata;

public interface NeodymiumData
{
    DataScope dynamic();
    SensitiveDataScope sensitive();
    DataScope constants();
    DataScope normal();

    String get(final String name);
    <T> T get(final String name, final Class<T> clazz);
    boolean exists(final String name);
}

public interface DataScope
{
    void set(final String name, final String value);
    String get(final String name);
    <T> T get(final String name, final Class<T> clazz);
    boolean exists(final String name);
}

public interface SensitiveDataScope
{
    void set(final String name, final String value, final String mockValue);
    void set(final String name, final String value); // Uses framework-fabricated mock
    String get(final String name);
    <T> T get(final String name, final Class<T> clazz);
    boolean exists(final String name);
}
```

### Internal Behavior & Precedence Hierarchy
*   `data().dynamic().set(name, value)` registers the key in the runtime's dynamic variables list and stores the value.
*   `data().sensitive().set(name, value, mock)` registers the key in both the active sensitive registry (mapping raw to mock value) and the dynamic variables list, storing the raw value for execution.
*   `data().constants().set(name, value)` registers the key as a constant.
*   `data().normal().set(name, value)` registers the key as a standard variable.
*   `data().get(name)` and `data().exists(name)` evaluate the context hierarchy in a strict precedence order to retrieve currently valid values:
    1. Programmatic Java Scopes (`sensitive` > `dynamic` > `normal` > `constants`).
    2. Active dataset row context (combining dimension-specific lookups and dataset constants).
    3. Global playbook defaults (`_meta` defaults).
    4. External environment configuration (`_properties` or JVM/System properties).
*   These registered classifications bypass any YAML declarations, allowing developers to inject environment properties or runtime parameters cleanly from `@BeforeEach` or custom hooks.
*   Symmetric `get(name)` calls on each scope permit reading back the registered variables during the Java lifecycle. Unified `Neodymium.data().get(name)` acts as a single point of truth across splits.

---

## Detailed Design: Implicitly Dynamic `store` Variables

*   **Syntax:**
    ```yaml
    _steps:
      - Store text from "#order-number" as "orderNumber"
      - Verify order "${orderNumber}" status is pending
    ```
*   **Sanitization Pipeline:**
    When executing a `store` step, the runtime engine intercepts the defined target variable name (`orderNumber`) and records it in a `runtimeStoredVariables` registry.
    This registry is automatically appended to the active `_dynamic` lookup list before the recorder sanitizes the action JSON.
    As a result, any subsequent step referencing `${orderNumber}` is sanitized automatically without user intervention or YAML declarations.

---

## Verification Plan

### Automated Tests
- Introduce unit tests in `AiTemplateResolutionTest.java` verifying that programmatically injected variables (constant, normal, sensitive, dynamic) behave exactly as if declared in the YAML.
- Verify that a mocked execution of a `store` step registers the variable as dynamic and sanitizes recorded actions correctly.

### Manual Verification
- Review the updated `comprehensive_demo.yaml` playbook representation to ensure it is clean and free of boilerplate IDs.
