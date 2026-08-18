/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.aura;

import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmProviderFactory;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.aura.dto.BrowserProfileDto;
import com.xceptance.neodymium.aura.dto.ChatMessageDto;
import com.xceptance.neodymium.aura.dto.ChatRequest;
import com.xceptance.neodymium.aura.dto.ChatResponse;
import com.xceptance.neodymium.aura.dto.DatasetDto;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.util.Neodymium;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service handling Chat workflow orchestration and Gemini-powered visual or test steps reasoning.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraChatService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AuraChatService.class);

    private final AuraFileService fileService;
    private AuraManagerQueueController queueController;

    public AuraChatService(final AuraFileService fileService)
    {
        this.fileService = fileService;
    }

    public AuraChatService(final AuraFileService fileService, final AuraManagerQueueController queueController)
    {
        this.fileService = fileService;
        this.queueController = queueController;
    }

    public void setQueueController(final AuraManagerQueueController queueController)
    {
        this.queueController = queueController;
    }

    public ChatResponse runChatWorkflow(final ChatRequest req)
    {
        final StringBuilder thinkingLog = new StringBuilder();
        thinkingLog.append("[AI Intent Classification] Running Stage 1...\n");

        final LlmProvider client = LlmProviderFactory.createProvider("chat", new AiConfiguration());

        // Stage 1: Intent Classifier
        final String stage1SystemPrompt = "You are an AI router for a test automation manager called Neodymium Aura.\n"
                + "Your job is to classify the user's intent into one of the following categories:\n"
                + "- \"browser\": The user wants to configure, select, switch, or change browser profiles (e.g., \"Run on Chrome and Firefox\", \"Select mobile profiles\", \"Use Safari\", \"Configure desktop browsers\", \"Select all browsers\", \"Clear browsers\").\n"
                + "- \"select\": The user wants to select, run, filter, check, or execute one or more test cases.\n"
                + "- \"edit\": The user wants to edit, update, create, delete, add steps, or modify a test case.\n"
                + "- \"both\": The user request implies both test selection and editing.\n"
                + "- \"neither\": The user is asking a general question, greeting, or querying system statistics/status.\n\n"
                + "Respond ONLY with a valid JSON object matching this schema:\n"
                + "{\n"
                + "  \"intent\": \"browser\" | \"select\" | \"edit\" | \"both\" | \"neither\",\n"
                + "  \"reason\": \"Brief reason for classification\"\n"
                + "}";

        final LlmResponse stage1ResponseObj;
        try
        {
            stage1ResponseObj = client.chat(new LlmRequest(stage1SystemPrompt, req.prompt, null, null, 0.2, 30));
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to execute Stage 1 LLM request", e);
            return new ChatResponse("LLM provider execution error: " + e.getMessage(), thinkingLog.toString(), "error", null, null, null, null);
        }
        final String stage1Response = stage1ResponseObj != null ? stage1ResponseObj.content() : "";
        thinkingLog.append("Stage 1 Response: ").append(stage1Response).append("\n");

        String intent = "neither";
        try
        {
            final Map<?, ?> result = AuraHttpUtils.gson.fromJson(cleanJsonResponse(stage1Response), Map.class);
            if (result != null && result.containsKey("intent"))
            {
                intent = String.valueOf(result.get("intent"));
            }
        }
        catch (final Exception e)
        {
            LOGGER.error("Failed to parse Stage 1 response", e);
            thinkingLog.append("Error parsing Stage 1 response: ").append(e.getMessage()).append("\n");
        }

        thinkingLog.append("Classified intent: ").append(intent).append("\n");

        if ("browser".equalsIgnoreCase(intent))
        {
            thinkingLog.append("[AI Browser Configuration] Running Stage 2 (Browser Selection)...\n");
            final List<BrowserProfileDto> availableProfiles = queueController != null
                    ? queueController.getAvailableBrowserProfiles()
                    : new ArrayList<>();

            final StringBuilder profilesJson = new StringBuilder("[");
            for (int i = 0; i < availableProfiles.size(); i++)
            {
                final BrowserProfileDto p = availableProfiles.get(i);
                if (i > 0)
                {
                    profilesJson.append(", ");
                }
                profilesJson.append("{\"id\":\"").append(p.id)
                        .append("\",\"name\":\"").append(p.name)
                        .append("\",\"browser\":\"").append(p.browser)
                        .append("\",\"resolution\":\"").append(p.res)
                        .append("\"}");
            }
            profilesJson.append("]");

            final String browserSystemPrompt = "You are the Neodymium Aura Browser Configuration Assistant.\n"
                    + "Your goal is to select matching browser profile IDs based on the user's configuration request.\n"
                    + "Here are the available browser profiles configured in the project:\n"
                    + profilesJson.toString() + "\n\n"
                    + "Guidelines:\n"
                    + "- If the user asks for Chrome, select all relevant Chrome profiles (or specific ones if requested).\n"
                    + "- If the user asks for Firefox, select all relevant Firefox profiles.\n"
                    + "- If the user asks for Chrome + FF, select Chrome and Firefox profiles.\n"
                    + "- If the user asks for Mobile, select mobile profiles (devices/emulations).\n"
                    + "- If the user asks for Desktop, select desktop profiles (chrome, firefox, safari, edge).\n"
                    + "- If the user asks for All, select all available profiles.\n"
                    + "- If the user asks for Clear, select none (empty array).\n\n"
                    + "You MUST respond ONLY with a valid JSON object matching this schema:\n"
                    + "{\n"
                    + "  \"selectedBrowserProfiles\": [\"Profile_ID_1\", \"Profile_ID_2\", ...],\n"
                    + "  \"message\": \"Clear, friendly explanation of the applied browser profile selection.\"\n"
                    + "}";

            try
            {
                final LlmResponse resp = client.chat(new LlmRequest(browserSystemPrompt, req.prompt, null, null, 0.2, 30));
                final String respText = resp != null ? resp.content() : "";
                thinkingLog.append("Browser configuration LLM response: ").append(respText).append("\n");

                final Map<?, ?> parsed = AuraHttpUtils.gson.fromJson(cleanJsonResponse(respText), Map.class);
                final String message = parsed != null && parsed.containsKey("message")
                        ? String.valueOf(parsed.get("message"))
                        : "Updated browser configuration.";
                final List<String> selected = new ArrayList<>();
                if (parsed != null && parsed.get("selectedBrowserProfiles") instanceof List<?> list)
                {
                    for (final Object item : list)
                    {
                        if (item != null)
                        {
                            selected.add(String.valueOf(item));
                        }
                    }
                }

                return new ChatResponse(message, thinkingLog.toString(), "select_browser", null, null, null, null, selected);
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to execute browser configuration LLM request", e);
                thinkingLog.append("Error during browser configuration: ").append(e.getMessage()).append("\n");
                return new ChatResponse("Error configuring browser profiles: " + e.getMessage(), thinkingLog.toString(), "error", null, null, null, null);
            }
        }

        if ("edit".equalsIgnoreCase(intent) || "both".equalsIgnoreCase(intent))
        {
            // Run editor / creation workflow
            thinkingLog.append("[AI Test Generation] Running Stage 2 (Edit/Create)...\n");

            final StringBuilder actionInstructions = new StringBuilder();
            actionInstructions.append("Supported actions: Open URL, Type text into input fields, Click elements, Verify text content, Select dropdown options, Submit forms.");

            String activeFileContent = "";
            if (req.activeFile != null && !req.activeFile.trim().isEmpty())
            {
                try
                {
                    activeFileContent = fileService.readYamlFileContent(req.activeFile);
                }
                catch (final Exception e)
                {
                    LOGGER.error("Failed to read active file: " + req.activeFile, e);
                }
            }

            final String editorSystemPrompt = "You are the Neodymium Aura Test Editor Assistant.\n"
                    + "Your goal is to edit or create a YAML test case file based on the user's request.\n"
                    + "You MUST generate the steps using the available action plugins. Here are the instructions for the registered actions:\n"
                    + actionInstructions.toString() + "\n"
                    + "CRITICAL REQUIREMENT ON YAML FORMATTING:\n"
                    + "The YAML file content must strictly follow this format:\n"
                    + "- Do NOT include metadata fields like 'name', 'description', or any other root-level properties.\n"
                    + "- The 'steps' property MUST be a multiline YAML string (using the '|' indicator), where each line is a natural language step sentence. Example:\n"
                    + "  steps: |\n"
                    + "    Open https://posters.xceptance.io:8443/posters/\n"
                    + "    Type \"${searchTerm}\" into the search field.\n"
                    + "    Click the Search Icon.\n"
                    + "    Verify that the main heading contains \"${searchTerm}\".\n"
                    + "- The 'data' property (optional, for parameterized datasets) must be a list of parameter maps. Example:\n"
                    + "  data:\n"
                    + "    - searchTerm: Test\n"
                    + "    - searchTerm: Chair\n"
                    + "- Do NOT output steps as structured arrays/lists of action/target/value objects (e.g. '- action: NAVIGATE'). Only write them as plain, natural language statements.\n"
                    + "- Do NOT guess or hallucinate HTML element IDs, CSS selectors, class names, or XPaths (such as \"search-form-input\" or \"div.no-results\") unless they are explicitly given. Stick to high-level, simple natural language descriptions of elements (e.g., \"search field\", \"no products found message\").\n\n"
                    + "If the user wants to EDIT an existing file, the current content of the active file is:\n"
                    + activeFileContent + "\n\n"
                    + "You MUST respond in JSON format with the following schema:\n"
                    + "{\n"
                    + "  \"status\": \"COMPLETE\" | \"ERROR\",\n"
                    + "  \"reasoning\": \"Explain your step-by-step thinking process for generating the test steps.\",\n"
                    + "  \"filename\": \"name-of-the-file.yaml\",\n"
                    + "  \"content\": \"The complete new or modified YAML file content (valid YAML format, with steps and optional data sections)\",\n"
                    + "  \"message\": \"Your user-facing response message summarizing what you changed or created.\"\n"
                    + "}";

            final String editorResponseObj;
            try
            {
                final LlmResponse resp = client.chat(new LlmRequest(editorSystemPrompt, req.prompt, null, null, 0.2, 60));
                editorResponseObj = resp != null ? resp.content() : "";
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to execute Stage 2 LLM request", e);
                return new ChatResponse("Failed to generate test case: " + e.getMessage(), thinkingLog.toString(), "error", null, null, null, null);
            }
            final String editorResponse = editorResponseObj;
            thinkingLog.append("Editor Response: ").append(editorResponse).append("\n");

            try
            {
                final Map<?, ?> editResult = AuraHttpUtils.gson.fromJson(cleanJsonResponse(editorResponse), Map.class);
                final String status = String.valueOf(editResult.get("status"));
                final String message = String.valueOf(editResult.get("message"));

                if ("COMPLETE".equalsIgnoreCase(status))
                {
                    final String filename = String.valueOf(editResult.get("filename"));
                    final String content = String.valueOf(editResult.get("content"));

                    fileService.saveYamlFileContent(filename, content);
                    thinkingLog.append("Successfully saved file: ").append(filename).append("\n");
                    return new ChatResponse(message, thinkingLog.toString(), "edit_or_create_test", null, filename,
                            content, null);
                }
                else
                {
                    return new ChatResponse("Failed to generate test case: " + message, thinkingLog.toString(), "error",
                            null, null, null, null);
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to parse editor response JSON", e);
                return new ChatResponse("Failed to parse generated test response: " + e.getMessage(),
                        thinkingLog.toString(), "error", null, null, null, null);
            }
        }
        else if ("select".equalsIgnoreCase(intent))
        {
            // Run selection escalation loop
            thinkingLog.append("[AI Test Selection] Running Stage 2 (Selection Escalation)...\n");

            final List<String> allFiles = new ArrayList<>();
            final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
            fileService.scanDirStatic(resourcesDir, resourcesDir, allFiles);
            allFiles.sort(String::compareTo);

            final List<String> availableDatasets = new ArrayList<>();
            for (final String file : allFiles)
            {
                final File yamlFile = new File(resourcesDir, file);
                final Map<String, Object> details = fileService.getFileDetails(yamlFile);
                @SuppressWarnings("unchecked")
                final List<DatasetDto> datasets = (List<DatasetDto>) details.get("datasets");
                if (datasets != null)
                {
                    for (final DatasetDto d : datasets)
                    {
                        availableDatasets.add("File: " + file + ", Dataset ID: " + d.id);
                    }
                }
            }

            final List<ChatMessage> conversation = new ArrayList<>();

            final String selectionSystemPrompt = "You are the Neodymium Aura Test Selector Assistant.\n"
                    + "Your goal is to identify and select the list of datasets (specifying both their YAML file name and dataset ID/index) that match the user's request.\n"
                    + "You have access to the test cases in three escalation levels:\n"
                    + "- Level 1: Just the relative file paths and dataset IDs of all available datasets.\n"
                    + "- Level 2: Metadata for specific files (step count, step summary, dataset keys).\n"
                    + "- Level 3: The complete YAML file content for specific files.\n\n"
                    + "To minimize token usage, you must start with Level 1. If you cannot decide which datasets to select based only on file paths and dataset IDs, you must output a JSON response requesting Level 2 metadata for candidate files. If you still cannot decide, request Level 3 full content.\n"
                    + "However, before requesting metadata or full content, if you realize you need specific data parameters or steps to identify the matching test cases, you can ask the user directly or explain what you need.\n\n"
                    + "CRITICAL REQUIREMENT ON DISCARDING FILES/DATASETS:\n"
                    + "When evaluating available datasets at Level 1 based on their file names and dataset IDs:\n"
                    + "- You MUST NOT assume a dataset is irrelevant just because its file name or dataset ID does not contain keywords from the user request.\n"
                    + "- Only rule out/discard files whose names and IDs make it completely impossible or highly improbable to match.\n"
                    + "- If a filename or ID is generic or ambiguous, you MUST request Level 2 metadata (\"NEED_DETAILS\") to inspect its steps before making a decision.\n\n"
                    + "The current available datasets (Level 1) are:\n"
                    + AuraHttpUtils.gson.toJson(availableDatasets) + "\n\n"
                    + "You MUST respond in JSON format with the following schema:\n"
                    + "{\n"
                    + "  \"status\": \"NEED_DETAILS\" | \"NEED_FULL_CONTENT\" | \"COMPLETE\" | \"ASK_USER\",\n"
                    + "  \"reasoning\": \"Explain your step-by-step thinking process, which candidate files/datasets you are looking at and why.\",\n"
                    + "  \"requestedFiles\": [\"relative/path/to/file1.yaml\", ...], // Files you need details/content for (only when status is NEED_DETAILS or NEED_FULL_CONTENT)\n"
                    + "  \"selectedDatasets\": [{\"file\": \"relative/path/to/file1.yaml\", \"id\": \"posters-search\"}, ...], // The final selected datasets (only when status is COMPLETE)\n"
                    + "  \"message\": \"Your user-facing response message. If status is COMPLETE, summarize which datasets you selected. If status is ASK_USER, ask for clarification.\"\n"
                    + "}";

            conversation.add(SystemMessage.from(selectionSystemPrompt));
            if (req.history != null)
            {
                for (final ChatMessageDto msg : req.history)
                {
                    if ("user".equalsIgnoreCase(msg.role))
                    {
                        conversation.add(UserMessage.from(msg.content));
                    }
                    else if ("assistant".equalsIgnoreCase(msg.role) || "ai".equalsIgnoreCase(msg.role))
                    {
                        conversation.add(AiMessage.from(msg.content));
                    }
                }
            }
            conversation.add(UserMessage.from(req.prompt));

            int iterations = 0;
            while (iterations < 5)
            {
                iterations++;
                thinkingLog.append("Escalation Loop Iteration ").append(iterations).append("...\n");

                final String responseText;
                try
                {
                    final LlmResponse resp = client.chat(new LlmRequest("You are the Neodymium Aura Test Selector Assistant.", req.prompt, null, null, 0.2, 30));
                    responseText = resp != null ? resp.content() : "";
                }
                catch (final Exception e)
                {
                    LOGGER.error("Failed to execute LLM request in escalation loop", e);
                    return new ChatResponse("LLM selection error: " + e.getMessage(), thinkingLog.toString(), "error", null, null, null, null);
                }
                thinkingLog.append("Response: ").append(responseText).append("\n");

                try
                {
                    final Map<?, ?> selResult = AuraHttpUtils.gson.fromJson(cleanJsonResponse(responseText), Map.class);
                    final String status = String.valueOf(selResult.get("status"));
                    final String message = String.valueOf(selResult.get("message"));

                    if ("ASK_USER".equalsIgnoreCase(status))
                    {
                        return new ChatResponse(message, thinkingLog.toString(), null, null, null, null, null);
                    }
                    else if ("COMPLETE".equalsIgnoreCase(status))
                    {
                        @SuppressWarnings("unchecked")
                        final List<Map<?, ?>> rawDatasets = (List<Map<?, ?>>) selResult.get("selectedDatasets");
                        final List<DatasetSelection> selectedDatasets = new ArrayList<>();
                        if (rawDatasets != null)
                        {
                            for (final Map<?, ?> rawItem : rawDatasets)
                            {
                                final DatasetSelection sel = new DatasetSelection();
                                sel.file = String.valueOf(rawItem.get("file"));
                                sel.id = String.valueOf(rawItem.get("id"));
                                selectedDatasets.add(sel);
                            }
                        }
                        return new ChatResponse(message, thinkingLog.toString(), "select_tests", null, null, null,
                                selectedDatasets);
                    }
                    else if ("NEED_DETAILS".equalsIgnoreCase(status))
                    {
                        @SuppressWarnings("unchecked")
                        final List<String> requested = (List<String>) selResult.get("requestedFiles");
                        final List<Map<String, Object>> detailsList = new ArrayList<>();
                        for (final String reqFile : requested)
                        {
                            final File f = fileService.resolveCanonicalFile(reqFile);
                            if (f.exists() && f.isFile())
                            {
                                detailsList.add(fileService.getFileDetails(f));
                            }
                        }
                        thinkingLog.append("Retrieved Level 2 details for: ").append(requested).append("\n");

                        conversation.add(AiMessage.from(responseText));
                        conversation.add(UserMessage.from(
                                "Here is the Level 2 metadata for your requested files:\n"
                                        + AuraHttpUtils.gson.toJson(detailsList)));
                    }
                    else if ("NEED_FULL_CONTENT".equalsIgnoreCase(status))
                    {
                        @SuppressWarnings("unchecked")
                        final List<String> requested = (List<String>) selResult.get("requestedFiles");
                        final Map<String, String> contentsMap = new HashMap<>();
                        for (final String reqFile : requested)
                        {
                            final String content = fileService.readYamlFileContent(reqFile);
                            contentsMap.put(reqFile, content);
                        }
                        thinkingLog.append("Retrieved Level 3 content for: ").append(requested).append("\n");

                        conversation.add(AiMessage.from(responseText));
                        conversation.add(UserMessage.from("Here is the Level 3 full content for your requested files:\n"
                                + AuraHttpUtils.gson.toJson(contentsMap)));
                    }
                    else
                    {
                        return new ChatResponse("AI selector returned unknown status: " + status,
                                thinkingLog.toString(), "error", null, null, null, null);
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.error("Failed to parse or execute loop step", e);
                    return new ChatResponse("AI selector execution error: " + e.getMessage(), thinkingLog.toString(),
                            "error", null, null, null, null);
                }
            }
            return new ChatResponse("AI selector loop exceeded maximum iterations.", thinkingLog.toString(), "error",
                    null, null, null, null);
        }
        else
        {
            // General talk / neither
            thinkingLog.append("[AI General Chat] Running Stage 2 (Neither)...\n");
            final String fallbackSystemPrompt = "You are the Neodymium Aura AI Assistant.\n"
                    + "Respond nicely to the user's general message, greeting, or question.\n"
                    + "You can also inform them about your capabilities to select/run tests or create/edit test cases.\n\n"
                    + "You MUST respond in JSON format matching this schema:\n"
                    + "{\n"
                    + "  \"message\": \"Your markdown-formatted response message to the user.\"\n"
                    + "}";

            final List<ChatMessage> fallbackMessages = new ArrayList<>();
            fallbackMessages.add(SystemMessage.from(fallbackSystemPrompt));
            if (req.history != null)
            {
                for (final ChatMessageDto msg : req.history)
                {
                    if ("user".equalsIgnoreCase(msg.role))
                    {
                        fallbackMessages.add(UserMessage.from(msg.content));
                    }
                    else if ("assistant".equalsIgnoreCase(msg.role) || "ai".equalsIgnoreCase(msg.role))
                    {
                        fallbackMessages.add(AiMessage.from(msg.content));
                    }
                }
            }
            fallbackMessages.add(UserMessage.from(req.prompt));

            final String fallbackResponse;
            try
            {
                final LlmResponse fallbackResp = client.chat(new LlmRequest(fallbackSystemPrompt, req.prompt, null, null, 0.2, 30));
                fallbackResponse = fallbackResp != null ? fallbackResp.content() : "";
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to execute fallback LLM request", e);
                return new ChatResponse("Fallback LLM error: " + e.getMessage(), thinkingLog.toString(), "error", null, null, null, null);
            }
            String message = fallbackResponse;
            try
            {
                final Map<?, ?> fallbackResult = AuraHttpUtils.gson.fromJson(cleanJsonResponse(fallbackResponse), Map.class);
                if (fallbackResult != null && fallbackResult.containsKey("message"))
                {
                    message = String.valueOf(fallbackResult.get("message"));
                }
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to parse fallback response JSON", e);
            }
            return new ChatResponse(message, thinkingLog.toString(), null, null, null, null, null);
        }
    }

    private static String cleanJsonResponse(final String response)
    {
        if (response == null)
        {
            return "";
        }
        String cleaned = response.trim();
        if (cleaned.startsWith("```json"))
        {
            cleaned = cleaned.substring(7);
        }
        else if (cleaned.startsWith("```"))
        {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```"))
        {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
