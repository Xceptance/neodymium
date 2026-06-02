## Why

To expand the capabilities of Neodymium AI while keeping downstream integration as simple and robust as possible, we want to natively support a curated set of premium and local model providers (Google AI Gemini, GCP Vertex AI, Mistral AI, and local Ollama execution). Downstream consumers should have a seamless "batteries-included" experience, so Neodymium will package these core integrations directly in its POM.

## What Changes

- Explicitly add standard, vendor-specific LangChain4j dependencies to Neodymium's `pom.xml`:
  - `langchain4j-vertex-ai` (GCP Vertex AI, supporting Vertex Gemini/Mistral/Gemma)
  - `langchain4j-mistral-ai` (Direct Mistral AI API)
  - `langchain4j-ollama` (Local Ollama execution)
- Implement a 100% type-safe `ChatModelFactory` class that directly imports and instantiates these models using direct builder API calls based on a standard `neodymium.ai.provider` configuration switch:
  - `gemini` -> Google AI Gemini (`GoogleAiGeminiChatModel`)
  - `vertex` -> GCP Vertex AI (`VertexAiChatModel`)
  - `mistral` -> Mistral AI (`MistralAiChatModel`)
  - `ollama` -> Ollama (`OllamaChatModel`)
- Avoid all reflection or runtime class loading mechanisms, ensuring complete type safety and IDE navigability.

## Capabilities

### New Capabilities
- `common-llm-support`: Direct, pre-packaged support for Google AI Gemini, GCP Vertex AI, Mistral AI, and local Ollama providers.

### Modified Capabilities
- (None)

## Impact

- **Configuration**: Properties will select the active provider (`gemini`, `vertex`, `mistral`, or `ollama`) and specify their required parameters in `neodymium.properties`.
- **Code**: `LlmClient` will acquire its `ChatModel` via a fully compiled switch-case factory class.
- **Dependencies**: Neodymium core's compile-time POM will now package the required LangChain4j integration modules (`langchain4j-vertex-ai`, `langchain4j-mistral-ai`, `langchain4j-ollama`).
