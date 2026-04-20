package com.xceptance.neodymium.ai.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionType;
import com.xceptance.neodymium.ai.core.AiAgentPrompts;
import com.xceptance.neodymium.ai.core.LlmClient;
import com.xceptance.neodymium.ai.core.LlmMode;
import com.xceptance.neodymium.ai.core.PageAnalyzer;
import com.xceptance.neodymium.ai.core.TokenStats;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Entry point for explicitly generating an AI Test Prompt via exploratory
 * browser interaction.
 */
public class AiPromptGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(AiPromptGenerator.class);

    /**
     * Executes the generative AI agent and constructs a prompt.
     * 
     * @param url        The target URL to start exploration.
     * @param intent     The high-level instructional goal.
     * @param outputPath The path to output the generated YAML file.
     */
    public void generate(String url, String intent, String outputPath) {
        TokenStats tokenStats = new TokenStats();
        LlmClient llmClient = new LlmClient(Neodymium.aiConfiguration(), tokenStats);
        generate(llmClient, url, intent, outputPath);
    }

    public void generate(LlmClient llmClient, String url, String intent, String outputPath) {
        // 1. Verify property constraints using the standard Neodymium configuration
        // system.
        if (!Neodymium.aiConfiguration().aiGenerateEnabled()) {
            Assertions.fail("Neodymium AI Test Prompt Generation is disabled. "
                    + "Please set neodymium.ai.generate=true to run @NeodymiumTestGenerator exploratory logic.");
        }

        // 2. Disabling standard Playbook recording
        // While we execute, we prevent the framework from fragmenting normal records.
        // Wait, AiConfiguration relies on the properties loaded.
        // We can override the configuration property programmatically, or modify
        // Neodymium configuration.
        // Since owner Config is mutable in Neodymium (AiConfiguration extends Mutable),
        // we can set it.
        Neodymium.aiConfiguration().setProperty("neodymium.ai.playbook.record", "false");

        // 3. Print massive disclaimer
        LOG.warn("************************************************************************");
        LOG.warn("WARNING: NEODYMIUM AI EXPLORATORY GENERATOR STARTED");
        LOG.warn("This bot will dynamically navigate and attempt to fulfill your prompt.");
        LOG.warn("Exploratry AI Bots can act unpredictable and therefore may perform unexpected actions.");
        LOG.warn("Ensure you use Neodymium's 'neodymium.url.excludeList' and 'neodymium.url.includeList' to prevent");
        LOG.warn("the bot from crawling off-site to external gateways.");
        LOG.warn("Do NOT point this against production data unless fully understood.");
        LOG.warn("************************************************************************");

        // 4. Explore step-by-step
        explore(llmClient, url, intent, outputPath);
    }

    protected void openBrowser(String url) {
        com.codeborne.selenide.Selenide.open(url);
    }

    protected String captureDom(PageAnalyzer pageAnalyzer) {
        return pageAnalyzer.captureSimplifiedDom(true);
    }

    protected void executeAction(ActionExecutor actionExecutor, Action action) {
        List<Action> toExecute = new ArrayList<>();
        toExecute.add(action);
        actionExecutor.executeAll(toExecute);
    }

    public List<Action> explore(LlmClient llmClient, String url, String intent, String outputPath) {
        LOG.info("Starting step-by-step exploratory run on '{}' for intent: '{}'", url, intent);
        
        openBrowser(url);

        PageAnalyzer pageAnalyzer = new PageAnalyzer();
        ActionExecutor actionExecutor = new ActionExecutor(this);

        List<Action> successfulPath = new ArrayList<>();
        java.util.Map<String, String> knownBindings = new java.util.LinkedHashMap<>();
        
        // Globally inject the testId from the output file name so it's resolved on-the-fly during ActionExecutor phase
        String fileName = Paths.get(outputPath).getFileName().toString();
        String testId = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        knownBindings.put("testId", testId);

        List<String> iterationLogs = new ArrayList<>();

        int maxSteps = com.xceptance.neodymium.util.Neodymium.aiConfiguration().aiGenerateMaxSteps();
        int maxFailures = com.xceptance.neodymium.util.Neodymium.aiConfiguration().aiGenerateMaxFailures();
        boolean entireGoalAchieved = false;

        String currentSubgoal = null;
        List<Action> pendingActions = new ArrayList<>();
        String statusMessage = null;
        int failedAttempts = 0;

        for (int i = 0; i < maxSteps && !entireGoalAchieved; i++) {
            StringBuilder historyBuilder = new StringBuilder();
            if (successfulPath.isEmpty() && pendingActions.isEmpty()) {
                historyBuilder.append("None (Initial Step)");
            } else {
                int stepNum = 1;
                for (Action a : successfulPath) {
                    historyBuilder.append(stepNum++).append(". ").append(a.getDescription()).append("\n");
                }
                for (Action pending : pendingActions) {
                    historyBuilder.append(stepNum++).append(". ").append(pending.getDescription())
                            .append(" (PENDING VERIFICATION)\n");
                }
            }

            String dom = captureDom(pageAnalyzer);
            String prompt = AiAgentPrompts.buildExplorationPrompt(intent, currentSubgoal, historyBuilder.toString(), dom,
                    statusMessage, knownBindings);
            String responseStr = executeExplorationLlmCall(llmClient, prompt);
            
            String currentIterationLog = "Iteration " + (i + 1) + "\n\n--- AI Prompt ---\n" + prompt 
                                         + "\n\n--- AI Response ---\n" + responseStr 
                                         + "\n--------------------------------------------------";
            iterationLogs.add(currentIterationLog);

            JsonObject json = null;
            boolean validJson = false;
            int jsonRetries = 0;
            int maxJsonRetries = 3;

            while (!validJson && jsonRetries < maxJsonRetries) {
                try {
                    String cleanJson = responseStr.trim();
                    if (cleanJson.startsWith("```json")) {
                        cleanJson = cleanJson.substring(7);
                    } else if (cleanJson.startsWith("```")) {
                        cleanJson = cleanJson.substring(3);
                    }
                    if (cleanJson.endsWith("```")) {
                        cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                    }
                    cleanJson = cleanJson.trim();

                    int startIndex = cleanJson.indexOf("{");
                    int endIndex = cleanJson.lastIndexOf("}");

                    if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                        cleanJson = cleanJson.substring(startIndex, endIndex + 1);
                    } else {
                        throw new IllegalStateException("No JSON object found in response");
                    }

                    com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setStrictness(Strictness.LENIENT)
                            .create();
                    json = gson.fromJson(cleanJson, JsonObject.class);
                    
                    if (json == null) {
                        throw new IllegalStateException("Parsed JSON is null");
                    }
                    validJson = true;
                } catch (Exception e) {
                    jsonRetries++;
                    LOG.warn("Failed to parse AI response as JSON (Attempt {}/{}). Error: {}. Response snippet: {}", 
                            jsonRetries, maxJsonRetries, e.getMessage(), 
                            responseStr.length() > 50 ? responseStr.substring(0, 50) + "..." : responseStr);
                    
                    if (jsonRetries < maxJsonRetries) {
                        String retryPrompt = prompt + "\n\nCRITICAL FIX REQUIRED: Your previous response was NOT valid JSON.\nError: " 
                            + e.getMessage() + "\n\nPlease reformulate your response to be strictly valid JSON without markdown wrapping.";
                        responseStr = executeExplorationLlmCall(llmClient, retryPrompt);
                        iterationLogs.add("Iteration " + (i + 1) + " (JSON Retry " + jsonRetries + ")\n\n--- AI Response (Retry) ---\n" + responseStr + "\n--------------------------------------------------");
                    }
                }
            }

            try {
                if (!validJson || json == null) {
                    LOG.warn("Failed to format or return valid JSON after {} attempts. Instructing AI to retry next turn.", maxJsonRetries);
                    statusMessage = "FAILED: Could not parse your response as valid JSON after multiple attempts. You must return a strict JSON object.";
                    failedAttempts++;
                    continue;
                }

                if (json.has("reasoning") && !json.get("reasoning").isJsonNull()) {
                    LOG.info("AI Reasoning: {}", json.get("reasoning").getAsString());
                }

                if (json.has("currentSubgoal") && !json.get("currentSubgoal").isJsonNull()) {
                    currentSubgoal = json.get("currentSubgoal").getAsString();
                }

                if (json.has("dropLastNActions") && !json.get("dropLastNActions").isJsonNull()) {
                    int dropCount = json.get("dropLastNActions").getAsInt();
                    if (dropCount > 0) {
                        LOG.warn("LOGICAL BACKTRACKING: AI requested to drop the last {} actions from the recorded script.", dropCount);
                        int toRemoveFromList = dropCount;
                        
                        while (toRemoveFromList > 0 && !pendingActions.isEmpty()) {
                            Action dropped = pendingActions.remove(pendingActions.size() - 1);
                            LOG.info("Dropping pending action: {}", dropped.getDescription());
                            toRemoveFromList--;
                        }
                        
                        while (toRemoveFromList > 0 && !successfulPath.isEmpty()) {
                            Action dropped = successfulPath.remove(successfulPath.size() - 1);
                            LOG.info("Dropping recorded action: {}", dropped.getDescription());
                            toRemoveFromList--;
                        }
                    }
                }

                if (json.has("overallIntentAchieved") && json.get("overallIntentAchieved").getAsBoolean()) {
                    entireGoalAchieved = true;
                    for (Action pending : pendingActions) {
                        successfulPath.add(pending);
                        if (pending.getDataBindings() != null) knownBindings.putAll(pending.getDataBindings());
                    }
                    pendingActions.clear();
                    LOG.info("AI reports OVERALL INTENT ACHIEVED!");
                    break;
                }

                if (!pendingActions.isEmpty()) {
                    LOG.info("Pending actions executed smoothly. Saving to local branch.");
                    for (Action pending : pendingActions) {
                        successfulPath.add(pending);
                        if (pending.getDataBindings() != null) knownBindings.putAll(pending.getDataBindings());
                    }
                    failedAttempts = 0;
                    pendingActions.clear();
                }

                if (json.has("actions") && json.get("actions").isJsonArray() && json.getAsJsonArray("actions").size() > 0) {
                    com.google.gson.JsonArray actionsArray = json.getAsJsonArray("actions");
                    for (com.google.gson.JsonElement actionElement : actionsArray) {
                        java.util.Map<String, String> currentKnownBindings = new java.util.HashMap<>(knownBindings);
                        for (Action pending : pendingActions) {
                            if (pending.getDataBindings() != null) {
                                currentKnownBindings.putAll(pending.getDataBindings());
                            }
                        }
                        
                        JsonObject actionJson = actionElement.getAsJsonObject();
                        Action nextAction = parseAndValidateAction(actionJson, currentKnownBindings);
                        
                        if (nextAction.getType() == ActionType.ASSERT) {
                            boolean isDuplicate = false;
                            java.util.List<Action> recentActions = new java.util.ArrayList<>(successfulPath);
                            recentActions.addAll(pendingActions);
                            for (int aIdx = recentActions.size() - 1; aIdx >= 0; aIdx--) {
                                Action recent = recentActions.get(aIdx);
                                if (recent.getType() != ActionType.ASSERT) {
                                    break;
                                }
                                if (java.util.Objects.equals(recent.getDescription(), nextAction.getDescription()) &&
                                    java.util.Objects.equals(recent.getTarget(), nextAction.getTarget())) {
                                    isDuplicate = true;
                                    break;
                                }
                            }
                            if (isDuplicate) {
                                LOG.info("Skipping duplicate ASSERT action: {}", nextAction.getDescription());
                                statusMessage = "Skipped duplicate validation: " + nextAction.getDescription() + ". Please perform a different action.";
                                continue;
                            }
                        }

                        logProposedAction(nextAction, currentKnownBindings);

                        try {
                            executeAction(actionExecutor, nextAction);
                            pendingActions.add(nextAction);
                            statusMessage = nextAction.getDescription() + " (Target: " + nextAction.getTarget() + ")";
                        } catch (Exception e) {
                            if (nextAction.getType() == ActionType.ASSERT && Neodymium.aiConfiguration().aiGenerateValidations()) {
                                LOG.warn("Validation failed. Attempting reactive healing with deterministic Validator Agent...");
                                ValidationAgent validator = new ValidationAgent(new LlmClient(Neodymium.aiConfiguration(), new TokenStats(), LlmMode.AGENT));
                                String healedDescription = validator.healValidation(nextAction.getDescription(), e.getMessage(), dom);
                                if (healedDescription != null) {
                                    LOG.info("Validation successfully healed! New desc: {}", healedDescription);
                                    nextAction.setDescription(healedDescription);
                                    nextAction.setTarget(null);
                                    nextAction.setValue(null);
                                    try {
                                        executeAction(actionExecutor, nextAction);
                                        pendingActions.add(nextAction);
                                        statusMessage = nextAction.getDescription() + " (Target: healed)";
                                        continue;
                                    } catch (Exception e2) {
                                        LOG.warn("Healed validation also failed. Halting array.");
                                    }
                                }
                            }
                            LOG.warn("Action execution threw an exception, halting array. AI will evaluate next turn.");
                            statusMessage = nextAction.getDescription() + " -> FAILED with exception: " + e.getMessage();
                            break;
                        }
                    }
                    if (pendingActions.isEmpty()) {
                        failedAttempts++;
                    }
                } else if (json.has("action") && !json.get("action").isJsonNull()) {
                    JsonObject actionJson = json.getAsJsonObject("action");
                    Action nextAction = parseAndValidateAction(actionJson, knownBindings);
                    logProposedAction(nextAction, knownBindings);

                    try {
                        executeAction(actionExecutor, nextAction);
                        pendingActions.add(nextAction);
                        statusMessage = nextAction.getDescription() + " (Target: " + nextAction.getTarget() + ")";
                    } catch (Exception e) {
                        if (nextAction.getType() == ActionType.ASSERT && Neodymium.aiConfiguration().aiGenerateValidations()) {
                            LOG.warn("Validation failed. Attempting reactive healing with deterministic Validator Agent...");
                            ValidationAgent validator = new ValidationAgent(new LlmClient(Neodymium.aiConfiguration(), new TokenStats(), LlmMode.AGENT));
                            String healedDescription = validator.healValidation(nextAction.getDescription(), e.getMessage(), dom);
                            if (healedDescription != null) {
                                LOG.info("Validation successfully healed! New desc: {}", healedDescription);
                                nextAction.setDescription(healedDescription);
                                nextAction.setTarget(null);
                                nextAction.setValue(null);
                                try {
                                    executeAction(actionExecutor, nextAction);
                                    pendingActions.add(nextAction);
                                    statusMessage = nextAction.getDescription() + " (Target: healed)";
                                    // Successfully healed
                                    continue;
                                } catch (Exception e2) {
                                    LOG.warn("Healed validation also failed.");
                                }
                            }
                        }
                        LOG.warn("Action execution threw an exception, dropping it. AI will evaluate next turn.");
                        statusMessage = nextAction.getDescription() + " -> FAILED with exception: " + e.getMessage();
                    }
                } else {
                    LOG.warn("No action provided and intent not achieved. Waiting for next turn.");
                    statusMessage = "FAILED: You provided no actions. Are you stuck?";
                    failedAttempts++;
                }

            } catch (Exception e) {
                LOG.warn("Failed to parse exploration response or execute action: {}. AI will evaluate next turn.", e.getMessage());
                statusMessage = "Processing your previous response failed with error: " + e.getMessage() + ". Please fix your JSON structure or action.";
                failedAttempts++;
            }

            if (failedAttempts >= maxFailures) {
                LOG.warn("AI exceeded maximum consecutively failed attempts or errors ({}). Aborting exploration.",
                        failedAttempts);
                break;
            }
        }

        LOG.info("Exploration finished. Total confirmed steps: {}", successfulPath.size());

        if (!entireGoalAchieved) {
            Path logPath = dumpDiagnosticLog(successfulPath, iterationLogs, outputPath, "Goal was not achieved after " + iterationLogs.size() + " iterations.");
            String locationHtml = logPath != null ? logPath.toAbsolutePath().toString() : "failed to save";
            Assertions.fail(
                    "Exploration finished but the precise goal was not achieved. Diagnostic log: " + locationHtml);
        } else if (successfulPath.isEmpty()) {
            Path logPath = dumpDiagnosticLog(successfulPath, iterationLogs, outputPath, "No valid steps were successfully executed.");
            String locationHtml = logPath != null ? logPath.toAbsolutePath().toString() : "failed to save";
            Assertions.fail(
                    "Exploration finished but no steps were successful. Diagnostic log: " + locationHtml);
        } else {
            extractAndWriteYaml(successfulPath, outputPath);
        }
        
        return successfulPath;
    }

    protected Path dumpDiagnosticLog(List<Action> successfulPath, List<String> iterationLogs, String outputPath, String reason) {
        try {
            Path path = Paths.get(outputPath);
            String logFileName = path.getFileName().toString();
            if (logFileName.endsWith(".yaml") || logFileName.endsWith(".yml")) {
                logFileName = logFileName.substring(0, logFileName.lastIndexOf('.')) + "_diagnostic.log";
            } else {
                logFileName += "_diagnostic.log";
            }
            
            Path logPath;
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
                logPath = path.getParent().resolve(logFileName);
            } else {
                logPath = Paths.get(logFileName);
            }

            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("=== AI EXPLORATION DIAGNOSTIC LOG ===\n");
            logBuilder.append("Failure Reason: ").append(reason).append("\n\n");
            
            logBuilder.append("--- SUCCESSFUL STEPS ACHIEVED ---\n");
            if (successfulPath.isEmpty()) {
                logBuilder.append("None.\n");
            } else {
                for (int i = 0; i < successfulPath.size(); i++) {
                    Action a = successfulPath.get(i);
                    logBuilder.append(i + 1).append(". ").append(a.getDescription()).append("\n");
                }
            }
            logBuilder.append("\n=====================================\n\n");
            
            logBuilder.append("--- EXPLORATION ITERATION LOG ---\n");
            for (String logLine : iterationLogs) {
                logBuilder.append(logLine).append("\n");
            }
            
            Files.write(logPath, logBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            LOG.warn("Diagnostic log for failed exploration dumped to: {}", logPath);
            return logPath;
        } catch (IOException e) {
            LOG.error("Failed to write diagnostic log to path adjacent to {}", outputPath, e);
            return null;
        }
    }

    private Action parseAndValidateAction(JsonObject actionJson, java.util.Map<String, String> knownBindings) {
        ActionType type = ActionType.valueOf(actionJson.get("type").getAsString());
        String target = actionJson.has("target") && !actionJson.get("target").isJsonNull()
                ? actionJson.get("target").getAsString()
                : null;
        String value = actionJson.has("value") && !actionJson.get("value").isJsonNull()
                ? actionJson.get("value").getAsString()
                : null;
        java.util.Map<String, String> dataBindings = new java.util.HashMap<>();
        if (actionJson.has("dataBindings") && actionJson.get("dataBindings").isJsonObject()) {
            com.google.gson.JsonObject bindingsObj = actionJson.getAsJsonObject("dataBindings");
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : bindingsObj.entrySet()) {
                if (!entry.getValue().isJsonNull()) {
                    dataBindings.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        String description = actionJson.has("description") && !actionJson.get("description").isJsonNull()
                ? actionJson.get("description").getAsString()
                : "Generated action";
        String elementDetails = actionJson.has("elementDetails")
                && !actionJson.get("elementDetails").isJsonNull()
                        ? actionJson.get("elementDetails").getAsString()
                        : "";

        if (!dataBindings.isEmpty()) {
            for (java.util.Map.Entry<String, String> entry : dataBindings.entrySet()) {
                String paramName = entry.getKey();
                String paramValue = entry.getValue();
                if (!paramName.matches("^[a-zA-Z0-9]+$")) {
                    throw new IllegalArgumentException("dataBinding key '" + paramName + "' must be alphanumeric and camelCase, no spaces or special characters.");
                }
                if (paramValue == null || paramValue.trim().isEmpty()) {
                    throw new IllegalArgumentException("If dataBinding key '" + paramName + "' is provided, its value MUST be provided.");
                }
                if (knownBindings.containsKey(paramName)) {
                    if (!knownBindings.get(paramName).equals(paramValue)) {
                        throw new IllegalArgumentException("Data Binding error: Parameter " + paramName + " was already used with value '" + knownBindings.get(paramName) + "'. Do not re-assign an existing variable with a new value. Create a new variable like '" + paramName + "2' instead.");
                    }
                }
                if (!paramName.equals("testId") && !description.contains("${" + paramName + "}")) {
                    throw new IllegalArgumentException("Data Binding error: You provided dataBinding '" + paramName + "' but failed to use '${" + paramName + "}' inside your 'description'. You must embed it AND wrap it in single quotes!");
                }
            }
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}").matcher(description);
        while (matcher.find()) {
            String var = matcher.group(1);
            if (!var.startsWith("neodymium.")) {
                if (!dataBindings.containsKey(var) && !knownBindings.containsKey(var)) {
                    throw new IllegalArgumentException("Data Binding error: You referenced ${" + var + "} in your description, but '" + var + "' is not a Known Data Binding and you didn't provide it in 'dataBindings' for this step.");
                }
            }
        }

        for (java.util.Map.Entry<String, String> entry : dataBindings.entrySet()) {
            String resolved = interpolateBindings(entry.getValue(), dataBindings, knownBindings);
            if (resolved != null && resolved.matches(".*\\$\\{[^}]+\\}.*")) {
                throw new IllegalArgumentException("Data Binding error: The value for '" + entry.getKey() + "' resolves to '" + resolved + "' which contains an unresolved template. You cannot use fake placeholders like ${__TIMESTAMP__}. Only use concrete dummy data or known bindings!");
            }
        }

        if (target != null && target.contains("${")) {
            target = interpolateBindings(target, dataBindings, knownBindings);
            if (target != null && target.matches(".*\\$\\{[^}]+\\}.*")) {
                throw new IllegalArgumentException("Action target contains unresolved variables: '" + target + "'. You must provide all variables in dataBindings or use known ones.");
            }
        }
        if (value != null && value.contains("${")) {
            value = interpolateBindings(value, dataBindings, knownBindings);
            if (value != null && value.matches(".*\\$\\{[^}]+\\}.*")) {
                throw new IllegalArgumentException("Action value contains unresolved variables: '" + value + "'. You must provide all variables in dataBindings or use known ones.");
            }
        }

        Action nextAction = new Action(type, target, value, description);
        if (!dataBindings.isEmpty()) {
            nextAction.setDataBindings(dataBindings);
        }
        if (!elementDetails.isEmpty()) {
            nextAction.getElementContext().put("aiDetails", elementDetails);
        }
        return nextAction;
    }

    private void logProposedAction(Action action, java.util.Map<String, String> knownBindings) {
        String description = action.getDescription();
        java.util.Map<String, String> dataBindings = action.getDataBindings() != null ? action.getDataBindings() : new java.util.HashMap<>();
        java.util.Map<String, String> resolvedLogBindings = new java.util.LinkedHashMap<>();
        java.util.regex.Matcher logMatcher = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}").matcher(description);
        while (logMatcher.find()) {
            String var = logMatcher.group(1);
            if (dataBindings.containsKey(var)) {
                resolvedLogBindings.put(var, dataBindings.get(var));
            } else if (knownBindings.containsKey(var)) {
                resolvedLogBindings.put(var, knownBindings.get(var));
            }
        }
        
        String logMessage = description;
        if (!resolvedLogBindings.isEmpty()) {
            logMessage += " [Data: " + resolvedLogBindings.toString() + "]";
        }

        LOG.info("Executing proposed action: {}", logMessage);
    }

    protected void extractAndWriteYaml(List<Action> successfulPath, String outputPath) {
        try {
            Path path = Paths.get(outputPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            StringBuilder yamlBuilder = new StringBuilder();
            yamlBuilder.append("prompt: |\n");
            yamlBuilder.append("  Open ${neodymium.url}\n");

            java.util.Map<String, String> dataMap = new java.util.LinkedHashMap<>();
            // Auto-inject the testId from the output file name (representing the generator method)
            String fileName = path.getFileName().toString();
            String testId = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            dataMap.put("testId", testId);

            for (Action action : successfulPath) {
                // Ensure properly indented lines for YAML multiline string
                yamlBuilder.append("  ").append(action.getDescription()).append("\n");
                if (action.getDataBindings() != null) {
                    dataMap.putAll(action.getDataBindings());
                }
            }

            if (!dataMap.isEmpty()) {
                yamlBuilder.append("\ndata:\n  - ");
                boolean first = true;
                for (java.util.Map.Entry<String, String> entry : dataMap.entrySet()) {
                    if (!first) {
                        yamlBuilder.append("\n    ");
                    }
                    yamlBuilder.append(entry.getKey()).append(": \"").append(entry.getValue().replace("\"", "\\\"")).append("\"");
                    first = false;
                }
                yamlBuilder.append("\n");
            }

            Files.write(path, yamlBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            LOG.info("Successfully generated YAML test prompt to: {}", outputPath);
        } catch (IOException e) {
            LOG.error("Failed to write output YAML file to {}", outputPath, e);
        }
    }

    // Extracted for test mocking
    protected String executeExplorationLlmCall(LlmClient llmClient, String prompt) {
        return llmClient.chat(AiAgentPrompts.getSystemExplorationPrompt(Neodymium.aiConfiguration().aiGenerateValidations()), prompt);
    }

    private String interpolateBindings(String text, java.util.Map<String, String> dataBindings, java.util.Map<String, String> knownBindings) {
        if (text == null) return null;
        
        String previous = null;
        String current = text;
        int maxDepth = 5;
        
        while (current.contains("${") && !current.equals(previous) && maxDepth-- > 0) {
            previous = current;
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}").matcher(current);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String var = matcher.group(1);
                if (dataBindings != null && dataBindings.containsKey(var)) {
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(dataBindings.get(var)));
                } else if (knownBindings != null && knownBindings.containsKey(var)) {
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(knownBindings.get(var)));
                } else {
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                }
            }
            matcher.appendTail(sb);
            current = sb.toString();
        }
        return current;
    }
}
