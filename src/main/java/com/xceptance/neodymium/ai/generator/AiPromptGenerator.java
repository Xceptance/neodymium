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
import com.xceptance.neodymium.ai.playbook.Playbook;
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
        if (Neodymium.aiConfiguration().aiGenerateV2()) {
            generateV2(llmClient, url, intent, outputPath);
            return;
        }

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

        // Globally inject the testId from the output file name so it's resolved
        // on-the-fly during ActionExecutor phase
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
            String prompt = AiAgentPrompts.buildExplorationPrompt(intent, currentSubgoal, historyBuilder.toString(),
                    dom,
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
                        String retryPrompt = prompt
                                + "\n\nCRITICAL FIX REQUIRED: Your previous response was NOT valid JSON.\nError: "
                                + e.getMessage()
                                + "\n\nPlease reformulate your response to be strictly valid JSON without markdown wrapping.";
                        responseStr = executeExplorationLlmCall(llmClient, retryPrompt);
                        iterationLogs.add("Iteration " + (i + 1) + " (JSON Retry " + jsonRetries
                                + ")\n\n--- AI Response (Retry) ---\n" + responseStr
                                + "\n--------------------------------------------------");
                    }
                }
            }

            try {
                if (!validJson || json == null) {
                    LOG.warn(
                            "Failed to format or return valid JSON after {} attempts. Instructing AI to retry next turn.",
                            maxJsonRetries);
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
                        LOG.warn(
                                "LOGICAL BACKTRACKING: AI requested to drop the last {} actions from the recorded script.",
                                dropCount);
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
                        if (pending.getDataBindings() != null)
                            knownBindings.putAll(pending.getDataBindings());
                    }
                    pendingActions.clear();
                    LOG.info("AI reports OVERALL INTENT ACHIEVED!");
                    break;
                }

                if (!pendingActions.isEmpty()) {
                    LOG.info("Pending actions executed smoothly. Saving to local branch.");
                    for (Action pending : pendingActions) {
                        successfulPath.add(pending);
                        if (pending.getDataBindings() != null)
                            knownBindings.putAll(pending.getDataBindings());
                    }
                    failedAttempts = 0;
                    pendingActions.clear();
                }

                if (json.has("actions") && json.get("actions").isJsonArray()
                        && json.getAsJsonArray("actions").size() > 0) {
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
                                statusMessage = "Skipped duplicate validation: " + nextAction.getDescription()
                                        + ". Please perform a different action.";
                                continue;
                            }
                        }

                        logProposedAction(nextAction, currentKnownBindings);

                        try {
                            executeAction(actionExecutor, nextAction);
                            pendingActions.add(nextAction);
                            statusMessage = nextAction.getDescription() + " (Target: " + nextAction.getTarget() + ")";
                        } catch (Exception e) {
                            if (nextAction.getType() == ActionType.ASSERT
                                    && Neodymium.aiConfiguration().aiGenerateValidations()) {
                                LOG.warn(
                                        "Validation failed. Attempting reactive healing with deterministic Validator Agent...");
                                ValidationAgent validator = new ValidationAgent(
                                        new LlmClient(Neodymium.aiConfiguration(), new TokenStats(), LlmMode.AGENT));
                                String healedDescription = validator.healValidation(nextAction.getDescription(),
                                        e.getMessage(), dom);
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
                            statusMessage = nextAction.getDescription() + " -> FAILED with exception: "
                                    + e.getMessage();
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
                        if (nextAction.getType() == ActionType.ASSERT
                                && Neodymium.aiConfiguration().aiGenerateValidations()) {
                            LOG.warn(
                                    "Validation failed. Attempting reactive healing with deterministic Validator Agent...");
                            ValidationAgent validator = new ValidationAgent(
                                    new LlmClient(Neodymium.aiConfiguration(), new TokenStats(), LlmMode.AGENT));
                            String healedDescription = validator.healValidation(nextAction.getDescription(),
                                    e.getMessage(), dom);
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
                LOG.warn("Failed to parse exploration response or execute action: {}. AI will evaluate next turn.",
                        e.getMessage());
                statusMessage = "Processing your previous response failed with error: " + e.getMessage()
                        + ". Please fix your JSON structure or action.";
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
            Path logPath = dumpDiagnosticLog(successfulPath, iterationLogs, outputPath,
                    "Goal was not achieved after " + iterationLogs.size() + " iterations.");
            String locationHtml = logPath != null ? logPath.toAbsolutePath().toString() : "failed to save";
            Assertions.fail(
                    "Exploration finished but the precise goal was not achieved. Diagnostic log: " + locationHtml);
        } else if (successfulPath.isEmpty()) {
            Path logPath = dumpDiagnosticLog(successfulPath, iterationLogs, outputPath,
                    "No valid steps were successfully executed.");
            String locationHtml = logPath != null ? logPath.toAbsolutePath().toString() : "failed to save";
            Assertions.fail(
                    "Exploration finished but no steps were successful. Diagnostic log: " + locationHtml);
        } else {
            extractAndWriteYaml(successfulPath, outputPath);
        }

        return successfulPath;
    }

    protected void generateV2(LlmClient llmClient, String url, String intent, String outputPath) {
        LOG.warn("************************************************************************");
        LOG.warn("WARNING: NEODYMIUM AI EXPLORATORY GENERATOR STARTED");
        LOG.warn("This bot will dynamically navigate and attempt to fulfill your prompt.");
        LOG.warn("Exploratry AI Bots can act unpredictable and therefore may perform unexpected actions.");
        LOG.warn("Ensure you use Neodymium's 'neodymium.url.excludeList' and 'neodymium.url.includeList' to prevent");
        LOG.warn("the bot from crawling off-site to external gateways.");
        LOG.warn("Do NOT point this against production data unless fully understood.");
        LOG.warn("************************************************************************");

        // 1. Explore step-by-step
        com.xceptance.neodymium.ai.playbook.Playbook playbook = exploreV2(llmClient, url, intent, outputPath);

        // 2. Extract
        List<Action> cleanPath = extractV2(llmClient, playbook, intent, outputPath);

        // 3. Verify
        verifyV2(cleanPath, outputPath, url);

        // 4. Dump to yaml
        extractAndWriteYaml(cleanPath, outputPath);
    }

    protected com.xceptance.neodymium.ai.playbook.Playbook exploreV2(LlmClient llmClient, String url, String intent,
            String outputPath) {
        LOG.info("\n========================================================================");
        LOG.info("\uD83E\uDDED PHASE 1: EXPLORATION (V2)");
        LOG.info("Starting step-by-step exploratory run on '{}' for intent: '{}'", url, intent);
        LOG.info("========================================================================");
        openBrowser(url);

        PageAnalyzer pageAnalyzer = new PageAnalyzer();
        ActionExecutor actionExecutor = new ActionExecutor(this);
        
        Neodymium.initializePlaybook();
        
        java.util.Map<String, String> knownBindings = new java.util.LinkedHashMap<>();
        String fileName = Paths.get(outputPath).getFileName().toString();
        String testId = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        knownBindings.put("testId", testId);

        int maxSteps = Neodymium.aiConfiguration().aiGenerateMaxSteps();
        int maxFailures = Neodymium.aiConfiguration().aiGenerateMaxFailures();
        boolean entireGoalAchieved = false;
        String currentSubgoal = null;
        String statusMessage = null;
        int failedAttempts = 0;

        List<Action> actionsForLogging = new ArrayList<>();
        List<String> iterationLogs = new ArrayList<>();

        Playbook playbook = Neodymium.getAiPlaybook();
        for (int i = 0; i < maxSteps && !entireGoalAchieved; i++) {
            LOG.info("\n\uD83D\uDC63 --- EXPLORATION STEP {} \u2192 Analyzing DOM and asking AI... ---", i + 1);
            StringBuilder historyBuilder = new StringBuilder();
            if (playbook.getSteps().isEmpty()) {
                historyBuilder.append("None (Initial Step)");
            } else {
                for (int stepIdx = 0; stepIdx < playbook.getSteps().size(); stepIdx++) {
                    com.xceptance.neodymium.ai.playbook.PlaybookStep step = playbook.getSteps().get(stepIdx);
                    historyBuilder.append(stepIdx).append(". ").append(step.getPromptLine()).append("\n");
                }
            }

            String dom = captureDom(pageAnalyzer);
            String prompt = AiAgentPrompts.buildExplorationPrompt(intent, currentSubgoal, historyBuilder.toString(),
                    dom, statusMessage, knownBindings);
            String responseStr = executeV2ExplorationLlmCall(llmClient, prompt);
            
            String currentIterationLog = "Iteration " + (i + 1) + "\n\n--- AI Prompt ---\n" + prompt
                    + "\n\n--- AI Response ---\n" + responseStr
                    + "\n--------------------------------------------------";
            iterationLogs.add(currentIterationLog);

            JsonObject json = null;
            try {
                String cleanJson = responseStr.trim();
                if (cleanJson.startsWith("```json"))
                    cleanJson = cleanJson.substring(7);
                else if (cleanJson.startsWith("```"))
                    cleanJson = cleanJson.substring(3);
                if (cleanJson.endsWith("```"))
                    cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                cleanJson = cleanJson.trim();
                int startIndex = cleanJson.indexOf("{");
                int endIndex = cleanJson.lastIndexOf("}");
                if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                    cleanJson = cleanJson.substring(startIndex, endIndex + 1);
                }
                json = new com.google.gson.GsonBuilder().setStrictness(Strictness.LENIENT).create().fromJson(cleanJson,
                        JsonObject.class);
            } catch (Exception e) {
            }

            if (json == null) {
                statusMessage = "FAILED: Could not parse response as valid JSON.";
                failedAttempts++;
                continue;
            }

            String reasoning = json.has("reasoning") && !json.get("reasoning").isJsonNull()
                    ? json.get("reasoning").getAsString()
                    : "No reasoning";
            if (json.has("currentSubgoal") && !json.get("currentSubgoal").isJsonNull()) {
                currentSubgoal = json.get("currentSubgoal").getAsString();
            }

            if (json.has("overallIntentAchieved") && json.get("overallIntentAchieved").getAsBoolean()) {
                entireGoalAchieved = true;
                LOG.info("AI reports OVERALL INTENT ACHIEVED!");
                break;
            }

            com.google.gson.JsonArray actionsArray = null;
            if (json.has("actions") && json.get("actions").isJsonArray()) {
                actionsArray = json.getAsJsonArray("actions");
            } else if (json.has("action") && !json.get("action").isJsonNull()) {
                actionsArray = new com.google.gson.JsonArray();
                actionsArray.add(json.getAsJsonObject("action"));
            }

            if (actionsArray != null && actionsArray.size() > 0) {
                for (com.google.gson.JsonElement actionElement : actionsArray) {
                    Action nextAction = null;
                    try {
                        nextAction = parseAndValidateAction(actionElement.getAsJsonObject(), knownBindings);
                        logProposedAction(nextAction, knownBindings);
                        executeAction(actionExecutor, nextAction);

                        com.xceptance.neodymium.ai.playbook.PlaybookStep pbStep = new com.xceptance.neodymium.ai.playbook.PlaybookStep();
                        pbStep.setPromptLine(nextAction.getDescription());
                        pbStep.setReasoning(reasoning);
                        pbStep.setActions(java.util.Collections.singletonList(nextAction));
                        playbook.addStep(pbStep);
                        actionsForLogging.add(nextAction);

                        if (nextAction.getDataBindings() != null)
                            knownBindings.putAll(nextAction.getDataBindings());
                        statusMessage = nextAction.getDescription() + " (Target: " + nextAction.getTarget() + ")";
                        failedAttempts = 0;
                    } catch (Exception e) {
                        LOG.warn("Action failed: {}", e.getMessage());
                        statusMessage = (nextAction != null ? nextAction.getDescription() : "Action") + " -> FAILED: "
                                + e.getMessage();
                        failedAttempts++;
                        break;
                    }
                }
            } else {
                LOG.warn("No actions provided.");
                statusMessage = "FAILED: You provided no actions.";
                failedAttempts++;
            }

            if (failedAttempts >= maxFailures) {
                LOG.warn("Exceeded max failures.");
                break;
            }
        }

        if (!entireGoalAchieved || playbook.getSteps().isEmpty()) {
            String reason = !entireGoalAchieved ? "Goal was not achieved after " + iterationLogs.size() + " iterations." : "Playbook successfully generated but produced an empty list of steps.";
            Path logPath = dumpDiagnosticLog(actionsForLogging, iterationLogs, outputPath, "V2 Failed: " + reason);
            String location = logPath != null ? logPath.toAbsolutePath().toString() : "failed to save";
            Assertions.fail("Exploration V2 failed. Reason: " + reason + " Diagnostic log: " + location);
        }

        if (Neodymium.aiConfiguration().aiGenerateV2DiagnosticLogs()) {
            dumpDiagnosticLog(actionsForLogging, iterationLogs, outputPath, "V2 Playbook RAW (Diagnostic)", "_phase1_diagnostic");
        }
        return playbook;
    }

    protected List<Action> extractV2(LlmClient llmClient, com.xceptance.neodymium.ai.playbook.Playbook playbook,
            String intent, String outputPath) {
        LOG.info("\n========================================================================");
        LOG.info("\uD83E\uDDE0 PHASE 2: EXTRACTION (V2)");
        LOG.info("\u2702\uFE0F Sending recorded Playbook to AI to extract successful minimal path...");
        LOG.info("========================================================================");
        int maxRetries = 3;
        String errorMessage = null;
        List<String> iterationLogs = new ArrayList<>();

        for (int i = 0; i < maxRetries; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("Overall Goal: ").append(intent).append("\n\nPlaybook:\n");
            for (int stepIdx = 0; stepIdx < playbook.getSteps().size(); stepIdx++) {
                com.xceptance.neodymium.ai.playbook.PlaybookStep step = playbook.getSteps().get(stepIdx);
                sb.append(stepIdx).append(": ")
                        .append(step.getPromptLine())
                        .append(" [Reasoning: ").append(step.getReasoning()).append("]\n");
            }

            String sysPrompt = AiAgentPrompts.V2_EXTRACTION_PROMPT;
            if (errorMessage != null) {
                sysPrompt += "\n\n" + AiAgentPrompts.V2_EXTRACTION_RETRY_PROMPT.replace("{error}", errorMessage);
            }

            String responseStr = llmClient.chat(sysPrompt, sb.toString());
            String currentIterationLog = "Extraction Attempt " + (i + 1) + "\n\n--- AI Prompt ---\n" + sysPrompt + "\n\n" + sb.toString()
                    + "\n\n--- AI Response ---\n" + responseStr
                    + "\n--------------------------------------------------";
            iterationLogs.add(currentIterationLog);

            try {
                String cleanJson = responseStr.trim();
                if (cleanJson.startsWith("```json"))
                    cleanJson = cleanJson.substring(7);
                else if (cleanJson.startsWith("```"))
                    cleanJson = cleanJson.substring(3);
                if (cleanJson.endsWith("```"))
                    cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                cleanJson = cleanJson.trim();

                int startIndex = cleanJson.indexOf("[");
                int endIndex = cleanJson.lastIndexOf("]");
                if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                    cleanJson = cleanJson.substring(startIndex, endIndex + 1);
                } else
                    throw new IllegalStateException("No JSON array returned.");

                com.google.gson.JsonArray jsonArray = new com.google.gson.Gson().fromJson(cleanJson,
                        com.google.gson.JsonArray.class);

                List<Integer> extractedIndices = new ArrayList<>();
                int prev = -1;
                for (com.google.gson.JsonElement el : jsonArray) {
                    int val = el.getAsInt();
                    if (val < 0 || val >= playbook.getSteps().size())
                        throw new IllegalArgumentException("Index out of bounds: " + val);
                    if (val <= prev)
                        throw new IllegalArgumentException(
                                "Indices must be strictly increasing. Found " + val + " after " + prev);
                    extractedIndices.add(val);
                    prev = val;
                }

                List<Action> cleanActions = new ArrayList<>();
                for (int idx : extractedIndices) {
                    cleanActions.addAll(playbook.getSteps().get(idx).getActions());
                }

                LOG.info("\uD83D\uDE80 Extraction successful! Picked {} out of {} steps.", extractedIndices.size(),
                        playbook.getSteps().size());
                if (Neodymium.aiConfiguration().aiGenerateV2DiagnosticLogs()) {
                    dumpDiagnosticLog(cleanActions, iterationLogs, outputPath, "V2 Extraction Finished", "_phase2_diagnostic");
                }
                return cleanActions;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                LOG.warn("Extraction failed validation: {}. Retrying...", errorMessage);
            }
        }

        Assertions.fail("Extraction phase failed after " + maxRetries + " retries.");
        return new ArrayList<>();
    }

    protected void verifyV2(List<Action> cleanPath, String outputPath, String url) {
        LOG.info("\n========================================================================");
        LOG.info("\u2705 PHASE 3: VERIFICATION (V2)");
        LOG.info("\uD83D\uDD04 Restarting browser natively and verifying extracted path...");
        LOG.info("========================================================================");

        com.xceptance.neodymium.util.WebDriverUtils.preventReuseAndTearDown();
        String currentProfile = Neodymium.getBrowserProfileName();
        if (currentProfile != null) {
            com.xceptance.neodymium.common.browser.BrowserMethodData browserData = new com.xceptance.neodymium.common.browser.BrowserMethodData(
                    currentProfile, false, false, false, false, new ArrayList<>());
            com.xceptance.neodymium.util.WebDriverUtils.setUp(browserData, "ai-verification");
        } else {
            com.codeborne.selenide.WebDriverRunner.closeWebDriver();
        }

        openBrowser(url);

        PageAnalyzer pageAnalyzer = new PageAnalyzer();

        ActionExecutor actionExecutor = new ActionExecutor(this);
        for (int i = 0; i < cleanPath.size(); i++) {
            Action nextAction = cleanPath.get(i);

            captureDom(pageAnalyzer);

            LOG.info("\uD83E\uDDEA --- Verification Step {}: {} ---", i + 1, nextAction.getDescription());
            try {
                actionExecutor.preCheckAction(nextAction);
                executeAction(actionExecutor, nextAction);
            } catch (Exception e) {
                LOG.error("Verification failed at step {}: {}", i, nextAction.getDescription(), e);
                Assertions.fail("Verification of extracted path failed at step " + i + " -> "
                        + nextAction.getDescription() + ". Error: " + e.getMessage());
            }
        }

        LOG.info("\uD83C\uDF89 Verification Complete! Clean path is robust.");
        if (Neodymium.aiConfiguration().aiGenerateV2DiagnosticLogs()) {
            dumpDiagnosticLog(cleanPath, new ArrayList<>(), outputPath, "V2 Verification Finished", "_phase3_diagnostic");
        }
    }

    protected String executeV2ExplorationLlmCall(LlmClient llmClient, String prompt) {
        return llmClient.chat(
                AiAgentPrompts.getV2SystemExplorationPrompt(Neodymium.aiConfiguration().aiGenerateValidations()),
                prompt);
    }

    protected Path dumpDiagnosticLog(List<Action> successfulPath, List<String> iterationLogs, String outputPath,
            String reason) {
        return dumpDiagnosticLog(successfulPath, iterationLogs, outputPath, reason, "_diagnostic");
    }

    protected Path dumpDiagnosticLog(List<Action> successfulPath, List<String> iterationLogs, String outputPath,
            String reason, String fileSuffix) {
        try {
            Path path = Paths.get(outputPath);
            String logFileName = path.getFileName().toString();
            if (logFileName.contains(".")) {
                logFileName = logFileName.substring(0, logFileName.lastIndexOf('.')) + fileSuffix + ".log";
            } else {
                logFileName += fileSuffix + ".log";
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
                    throw new IllegalArgumentException("dataBinding key '" + paramName
                            + "' must be alphanumeric and camelCase, no spaces or special characters.");
                }
                if (paramValue == null || paramValue.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "If dataBinding key '" + paramName + "' is provided, its value MUST be provided.");
                }
                if (knownBindings.containsKey(paramName)) {
                    if (!knownBindings.get(paramName).equals(paramValue)) {
                        if (Neodymium.aiConfiguration().aiGenerateV2()) {
                            LOG.info("V2 Mode: Allowing re-assignment of data binding '{}' from '{}' to '{}'.", paramName, knownBindings.get(paramName), paramValue);
                        } else {
                            throw new IllegalArgumentException("Data Binding error: Parameter " + paramName
                                    + " was already used with value '" + knownBindings.get(paramName)
                                    + "'. Do not re-assign an existing variable with a new value. Create a new variable like '"
                                    + paramName + "2' instead.");
                        }
                    }
                }
                if (!paramName.equals("testId") && !description.contains("${" + paramName + "}")) {
                    throw new IllegalArgumentException("Data Binding error: You provided dataBinding '" + paramName
                            + "' but failed to use '${" + paramName
                            + "}' inside your 'description'. You must embed it AND wrap it in single quotes!");
                }
            }
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}").matcher(description);
        while (matcher.find()) {
            String var = matcher.group(1);
            if (!var.startsWith("neodymium.")) {
                if (!dataBindings.containsKey(var) && !knownBindings.containsKey(var)) {
                    throw new IllegalArgumentException("Data Binding error: You referenced ${" + var
                            + "} in your description, but '" + var
                            + "' is not a Known Data Binding and you didn't provide it in 'dataBindings' for this step.");
                }
            }
        }

        for (java.util.Map.Entry<String, String> entry : dataBindings.entrySet()) {
            String resolved = interpolateBindings(entry.getValue(), dataBindings, knownBindings);
            if (resolved != null && resolved.matches(".*\\$\\{[^}]+\\}.*")) {
                throw new IllegalArgumentException("Data Binding error: The value for '" + entry.getKey()
                        + "' resolves to '" + resolved
                        + "' which contains an unresolved template. You cannot use fake placeholders like ${__TIMESTAMP__}. Only use concrete dummy data or known bindings!");
            }
        }

        if (target != null && target.contains("${")) {
            target = interpolateBindings(target, dataBindings, knownBindings);
            if (target != null && target.matches(".*\\$\\{[^}]+\\}.*")) {
                throw new IllegalArgumentException("Action target contains unresolved variables: '" + target
                        + "'. You must provide all variables in dataBindings or use known ones.");
            }
        }
        if (value != null && value.contains("${")) {
            value = interpolateBindings(value, dataBindings, knownBindings);
            if (value != null && value.matches(".*\\$\\{[^}]+\\}.*")) {
                throw new IllegalArgumentException("Action value contains unresolved variables: '" + value
                        + "'. You must provide all variables in dataBindings or use known ones.");
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
        java.util.Map<String, String> dataBindings = action.getDataBindings() != null ? action.getDataBindings()
                : new java.util.HashMap<>();
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
            // Auto-inject the testId from the output file name (representing the generator
            // method)
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
                    yamlBuilder.append(entry.getKey()).append(": \"").append(entry.getValue().replace("\"", "\\\""))
                            .append("\"");
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
        return llmClient.chat(
                AiAgentPrompts.getSystemExplorationPrompt(Neodymium.aiConfiguration().aiGenerateValidations()), prompt);
    }

    private String interpolateBindings(String text, java.util.Map<String, String> dataBindings,
            java.util.Map<String, String> knownBindings) {
        if (text == null)
            return null;

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
