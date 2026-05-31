## 1. Dependency and POM Configuration

- [ ] 1.1 Add the `langchain4j-vertex-ai` dependency to Neodymium POM.
- [ ] 1.2 Add the `langchain4j-mistral-ai` dependency to Neodymium POM.
- [ ] 1.3 Add the `langchain4j-ollama` dependency to Neodymium POM.

## 2. Factory and Integration Implementation

- [ ] 2.1 Create the type-safe `ChatModelFactory` class in `com.xceptance.neodymium.ai.core`.
- [ ] 2.2 Implement instantiation logic for `GoogleAiGeminiChatModel`.
- [ ] 2.3 Implement instantiation logic for `VertexAiChatModel`.
- [ ] 2.4 Implement instantiation logic for `MistralAiChatModel`.
- [ ] 2.5 Implement instantiation logic for `OllamaChatModel`.
- [ ] 2.6 Refactor `LlmClient` to acquire its model instance dynamically from `ChatModelFactory`.

## 3. Testing and Validation

- [ ] 3.1 Write unit tests for `ChatModelFactory` verifying proper property mapping and instantiation for all four providers.
- [ ] 3.2 Ensure validation failures occur if critical properties are omitted.
- [ ] 3.3 Verify vision (multimodal) payloads can be constructed successfully for the new models.

## 4. Documentation

- [ ] 4.1 Update `AI-README.md` to document properties and setup instructions for Google AI, Vertex AI, Mistral AI, and Ollama.
