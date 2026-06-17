## 1. Core Data & Utility Layer

- [x] 1.1 Create `MalformedPlaybookException.java`
- [x] 1.2 Modify `YamlFileReader.java` to support interleaved includes and variables resolution
- [x] 1.3 Modify `TestDataUtils.java` to bypass copy to `/tmp` and pass classpath context

## 2. AI Action & Execution Layer

- [x] 2.1 Create `IncludeAction.java`
- [x] 2.2 Register `IncludeAction` in `ActionRegistry.java`
- [x] 2.3 Modify `BranchAction.java` to track condition evaluation results
- [x] 2.4 Modify `AiAgent.java` to handle string trace paths and conditional inclusions caching
- [x] 2.5 Modify `StepLinter.java` and `MockActionExecutor.java` to support includes

## 3. Verification & Testing

- [x] 3.1 Copy unit tests and playbook test resources
- [x] 3.2 Verify all tests pass
