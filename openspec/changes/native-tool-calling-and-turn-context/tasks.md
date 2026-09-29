## 1. Core Client & Native Message Abstraction

- [x] 1.1 Implement `ChatMessage` record and `Role` enum (`SYSTEM`, `USER`, `ASSISTANT`, `TOOL`) in `org.neodymium.ai.client` and verify unit tests pass
- [x] 1.2 Update `LlmRequest` to support `List<ChatMessage> messages` and `List<ToolDefinition> tools`, verifying serialization tests pass
- [x] 1.3 Update `LlmResponse` to support deserialized `List<ToolCall> toolCalls()`, verifying response parsing tests pass
- [x] 1.4 Wire native function declarations and multi-turn message mapping in `GeminiLlmProvider` and `OpenAiLlmProvider`, verifying request formatting tests pass

## 2. Standardizing Local Browser Tool Outputs

- [x] 2.1 Update `BrowserToolProvider` to return structured JSON payloads (`url`, `title`, `selector`, `value`, `matches`) for all browser tools
- [x] 2.2 Update `BrowserToolsTest` to verify structured JSON output across all browser tools

## 3. Turn-Aware Dynamic Context in Agent Tool Loop

- [x] 3.1 Implement Turn-Aware Context in `AgentToolLoopStep` to provide zero DOM elements (URL and Title only) for `NAVIGATE`, `ASSERT_METADATA`, and focused `KEY_PRESS`
- [x] 3.2 Supply pierced DOM Light (LEAN) on Turn 1 for interactive steps (`CLICK`, `TYPE`, `SELECT`, `HOVER_SCROLL`, `ASSERT`, `STORE`)
- [x] 3.3 Append `ToolResult` JSON directly to `role: "TOOL"` messages on intermediate turns without re-extracting the full DOM tree
- [x] 3.4 Verify with `mvn test -pl neodymium-core -Dtest=AgentToolLoopStepTest`

## 4. Internal Loop Milestones (Replacing External Step Splitting)

- [x] 4.1 Implement milestone sub-action tracking inside `AgentToolLoopStep` to execute compound instructions sequentially within a single conversation
- [x] 4.2 Update `ExecuteActionsStep` to delegate compound instructions directly to `AgentToolLoopStep` without external pipeline splitting
- [x] 4.3 Verify with `mvn test -pl neodymium-core -Dtest=ExecuteActionsStepTest`

## 5. End-to-End Verification & Quality Audit

- [x] 5.1 Run `BlogTest#bruteforceSearch` and verify in the HTML report that Call 1 and Call 2/3 have zero 27KB DOM dumps and screenshots are intact
- [x] 5.2 Run `VerlaGuestCheckout_Us_English_Normal` and verify all 4 methods (`testCheckoutLive`, `testCheckoutReplay`, `testCheckoutLiveWithJudge`, `testCheckoutReplayWithJudge`) execute cleanly with native tool calling
- [x] 5.3 Perform clean code audit (zero unused imports, strict `final`, no inline FQCNs, zero committed secrets)

