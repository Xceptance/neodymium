# [IDEA-20260807] Enterprise Zero-Trust / Local VLM Support

- **Status:** `Proposed`
- **Proposed:** 2026-08-07
- **Resolved:** Pending
- **Component:** `neodymium-core (OllamaLlmProvider, LocalVlmAdapter)`
- **Category:** `Architecture & Core`
- **Author:** Neodymium Core Team

---

Enterprise companies are often restricted from sending SUT screenshots, DOM states, or proprietary data to external cloud APIs (like Google Gemini or OpenAI).
* **Concept:** Expand Neodymium's LLM engine to support local, on-premise execution using lightweight, open-source Vision-Language Models (VLMs) like **UI-TARS**, **Llama-3-Vision**, or **Qwen2-VL**.
* **Integration:**
  * Support running local models via **Ollama**, **vLLM**, or local **LangChain4j** providers.
  * Define optimized low-bit quantized model setups (e.g., 4-bit UI-TARS) that can execute directly on internal development machines or local Kubernetes QA clusters.
