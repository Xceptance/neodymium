# [DEF-20261023-0601] Custom Tool Root-Cause Masking and LinkageError Failure

- **Status:** `Resolved`
- **Opened:** 2026-10-23 06:01
- **Closed:** 2026-10-23 06:12
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** When a custom `@Tool` annotated Java method failed due to a `LinkageError` (e.g., `NoClassDefFoundError: MissingService`), `JavaTool` wrapped it in an opaque `RuntimeException("Error executing method " + name)` and `AgentToolLoopStep` caught and surfaced only `Tool error: Error executing method <name>`. The actual missing class name, root cause exception class, and error message were completely obscured from both the LLM agent turn loop and engineer diagnostics.
- **Root Cause:**
  1. In `JavaTool.execute()`, the reflection invocation `method.invoke()` was wrapped in `catch (final InvocationTargetException ite)`. If `ite.getCause()` was not an `AssertionError` and not an `Exception` (such as `NoClassDefFoundError` or `IncompatibleClassChangeError` which inherit from `Error`), it fell through to `throw new RuntimeException("Error executing method " + this.method.getName(), cause)`.
  2. For nested exceptions, `JavaTool` previously only took `e.getMessage()` from the outermost wrapper, discarding deeper causes.
  3. In `AgentToolLoopStep.java`, the fallback catch block caught only `Exception e` (leaving unhandled `Error`s to crash) and formatted errors using only top-level `e.getMessage()`.
- **Detection Gap ("What did we miss?"):** Existing `JavaToolFactoryTest` tests only exercised successful reflective invocations (e.g. `SampleMathTools`), never exercising reflective failure modes involving `Error`, `LinkageError`, or multi-tiered wrapped root-cause exception chains.
- **Resolution:**
  1. In `JavaTool.java`, unwrapped `InvocationTargetException` and added cycle-safe root-cause resolution via `ExceptionUtils.getRootCause()`. Ensured `VirtualMachineError` (e.g. `OutOfMemoryError`) and `AssertionError` propagate immediately, while converting `LinkageError` and nested operational exceptions into informative `ToolResult.error` detailing the error class, message, and deepest root cause.
  2. In `AgentToolLoopStep.java`, broadened the fallback handler from `catch (final Exception e)` to `catch (final Throwable e)` with immediate rethrow of fatal `VirtualMachineError` and pipeline control exceptions (`ConclusiveFailureException`, `TokenBudgetExceededException`). Enriched error messaging using `formatThrowableError()`, while ensuring assertion failures on assertion tools (both direct and nested) dispatch `ActionExecutedEvent`, record step calls/actions, and trigger Stop Criterion 2.
  3. In companion client project `neodymium-ai-testcase-hub/pom.xml`, updated the stale compiler plugin to `maven-compiler-plugin:3.14.1` with Java 21 release targeting so outdated file timestamps do not prevent compilation of new test classes.
- **Safety Net Added:**
  - Added in `JavaToolFactoryTest`:
    - `@Test testToolExecutionSurfacesLinkageAndNoClassDefFoundError`
    - `@Test testToolExecutionUnwrapsNestedRootCauseChain`
    - `@Test testToolExecutionPropagatesVirtualMachineError`
    - `@Test testToolExecutionPropagatesDirectAndNestedAssertionError`
    - `@Test testToolExecutionHandlesCyclicCausesWithoutHanging`
    - `@Test testToolExecutionHandlesAnonymousExceptionWithoutEmptyClassName`
  - Added in `AgentToolLoopStepTest`:
    - `@Test testToolExecutionPropagatesVirtualMachineError`
    - `@Test testToolExecutionPropagatesConclusiveFailureException`
    - `@Test testToolExecutionSurfacesLinkageErrorToLlm`
    - `@Test testAssertionToolUnwrapsNestedAssertionErrorAndDispatchesEvent`
