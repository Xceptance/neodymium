## 1. Core Parser Changes

- [ ] 1.1 Update `JAVA_METHOD_PATTERN` in `JavaMethodAction.java` to detect and parse optional variable assignment syntax (e.g. `java: ${varName} = method(...)`).
- [ ] 1.2 Implement parameter parsing for JSON arrays and comma-separated arguments in `JavaMethodAction.parseDirectInstruction`.
- [ ] 1.3 Refactor reflective method lookup in `JavaMethodAction.execute` to resolve methods accepting multiple `String` arguments based on the parsed argument count.
- [ ] 1.4 Refactor `JavaMethodAction.execute` to capture non-void method return values and store them in the active `Neodymium.getData()` map under the assigned variable name.

## 2. Dynamic JShell Evaluation

- [ ] 2.1 Update `JavaMethodAction` to detect pure Java expression instructions (without explicit method signatures) and execute them via the safe JShell evaluator.
- [ ] 2.2 Enforce safety filters on dynamic expressions using character allowlisting.

## 3. Verification & Testing

- [ ] 3.1 Create test cases in `JavaMethodTest.java` verifying multi-parameter reflective calls, return value context variable assignment, and safe JShell evaluations.
- [ ] 3.2 Verify all unit and integration tests build and pass successfully.
