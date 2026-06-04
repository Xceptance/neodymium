## 1. Configuration

- [ ] 1.1 Add `neodymium.ai.playbook.replay` property to `AiConfiguration.java`
- [ ] 1.2 Document the property in `AiConfiguration.java` as a standard boolean configuration with a default value of `true`

## 2. Core Implementation

- [ ] 2.1 Update `initializePlaybook` method in `Neodymium.java` to check the global config property `playbookReplayEnabled()`
- [ ] 2.2 Incorporate both legacy `skipReplay` and modern `neodymium.ai.skipReplay` dataset properties in the bypass determination
- [ ] 2.3 Ensure global replay disable takes precedence over any dataset/local settings

## 3. Testing and Verification

- [ ] 3.1 Create a new JUnit test class to verify global replay bypass functionality
- [ ] 3.2 Create tests to verify dataset-specific replay bypass functionality
- [ ] 3.3 Create tests to verify that global disable overrides dataset-specific settings (precedence rule)
- [ ] 3.4 Run the test suite and ensure all tests pass cleanly
- [ ] 3.5 Update AI-README.md to document the new `neodymium.ai.playbook.replay` global property

