## Why

Currently, there is no explicit way to enforce that a test step uses an LLM. As we rely on caching, playbooks, or heuristic fallbacks to optimize execution speed, we sometimes need a way to bypass these mechanisms and enforce live LLM evaluation for specific steps. This is critical for:
- Testing the LLM integration itself or debugging the AI's behavior directly.
- Ensuring non-deterministic processing is applied to complex steps.
- Handling highly dynamic scenarios where offline playback/replay is impossible. For example: image identification/OCR steps, data submissions that dynamically alter the response, or extremely dynamic website states where a previously recorded interaction becomes void. Instead of waiting for a local fallback failure, we need a way to preemptively enforce a normal LLM call.

## What Changes

- Add a mechanism (e.g., a flag, annotation, or configuration parameter) to enforce live LLM evaluation for a given test step.
- Update the playbook step and execution engine (`AiBrowser`) to honor this configuration, bypassing any local caching, determinism, or pre-recorded playbook logic.
- Ensure telemetry and reporting reflect when a step was forced to use the LLM.

## Capabilities

### New Capabilities
- `enforce-llm-step-execution`: Ability to strictly enforce live LLM processing for a given test step in the test automation workflow.

### Modified Capabilities
- `fresh-approach-to-test-scripts-and-test-documentation`: Will need updates to reflect the new syntax/mechanism for marking a step as enforcing LLM usage.

## Impact

- `com.xceptance.neodymium.ai.playbook.PlaybookStep` and related playbook execution logic.
- `com.xceptance.neodymium.ai.core.AiBrowser` and step execution pipelines.
- Test script parsing and YAML syntax to accommodate the new flag.
