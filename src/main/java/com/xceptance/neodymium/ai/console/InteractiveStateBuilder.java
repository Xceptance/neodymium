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
package com.xceptance.neodymium.ai.console;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neodymium.ai.action.Action;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.report.TestExecutionReport;
import org.neodymium.ai.report.TestExecutionReport.CategoryTokenUsage;
import org.neodymium.ai.report.TestExecutionReport.ReportActionEntry;
import org.neodymium.ai.report.TestExecutionReport.ReportLlmCallEntry;
import org.neodymium.ai.report.TestExecutionReport.ReportMetrics;
import org.neodymium.ai.report.TestExecutionReport.ReportScreenshotEntry;
import org.neodymium.ai.report.TestExecutionReport.ReportStepEntry;
import org.neodymium.ai.session.AiSession;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Utility serializing active {@link AiSession} and {@link ExecutionContext} state into
 * the JSON format expected by the Interactive Console UI ({@code interactive_console.js}).
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class InteractiveStateBuilder
{
    private static final Gson GSON = new Gson();

    private static final Set<String> KNOWN_LOCALE_CODES = new HashSet<>();

    static
    {
        for (final String country : Locale.getISOCountries())
        {
            KNOWN_LOCALE_CODES.add(country.toUpperCase());
            KNOWN_LOCALE_CODES.add(country.toLowerCase());
        }
        for (final String lang : Locale.getISOLanguages())
        {
            KNOWN_LOCALE_CODES.add(lang.toUpperCase());
            KNOWN_LOCALE_CODES.add(lang.toLowerCase());
        }
        final String[] extraCodes = {
            "UK", "uk", "GBR", "gbr", "USA", "usa", "DEU", "deu", "GER", "ger",
            "ENG", "eng", "FRA", "fra", "FRE", "fre", "SPA", "spa", "ITA", "ita",
            "JPN", "jpn", "ZHO", "zho", "CHI", "chi", "NLD", "nld", "DUT", "dut",
            "POL", "pol", "SWE", "swe", "NOR", "nor", "DNK", "dnk", "FIN", "fin",
            "RUS", "rus", "POR", "por", "AUS", "aus", "CAN", "can", "IND", "ind",
            "KOR", "kor", "BRA", "bra", "MEX", "mex"
        };
        for (final String code : extraCodes)
        {
            KNOWN_LOCALE_CODES.add(code);
        }
    }

    /**
     * Guesses a locale or location indicator from a dataset ID / label.
     *
     * @param datasetId the dataset ID or dataset label (e.g. "homepage test DE", "guest checkout deu")
     * @return guessed locale string (e.g. "DE", "deu", "en_US"), or null if none detected
     */
    public static String extractLocaleFromDatasetId(final String datasetId)
    {
        if (datasetId == null || datasetId.isBlank())
        {
            return null;
        }

        final String trimmed = datasetId.trim();

        // 1. Check for compound locale pattern like "en_US", "de_DE", "fr-FR", "en_GB", "jp_JP"
        final Pattern localePattern = Pattern.compile("(?i)(?:^|[^a-zA-Z0-9])([a-zA-Z]{2,3}[_-][a-zA-Z]{2,3})(?:$|[^a-zA-Z0-9])");
        final Matcher matcher = localePattern.matcher(trimmed);
        if (matcher.find())
        {
            final String match = matcher.group(1);
            final String[] parts = match.split("[_-]");
            if (parts.length == 2 && (KNOWN_LOCALE_CODES.contains(parts[0]) || KNOWN_LOCALE_CODES.contains(parts[1])))
            {
                return match;
            }
        }

        // 2. Tokenize by non-alphanumeric characters and inspect tokens from right to left
        final String[] tokens = trimmed.split("[^a-zA-Z0-9]+");
        for (int i = tokens.length - 1; i >= 0; i--)
        {
            final String token = tokens[i];
            if (token.isEmpty())
            {
                continue;
            }
            if (KNOWN_LOCALE_CODES.contains(token))
            {
                return token;
            }
        }

        return null;
    }

    private InteractiveStateBuilder()
    {
    }

    /**
     * Builds a minified state JSON string from the given session, execution context, and current step index.
     *
     * @param session the active AI session
     * @param context the thread-isolated execution context
     * @param runId the active test run identifier
     * @param activeStepIndex index of the currently active step
     * @param runnerStatus overall run status string ("running", "paused", "passed", "failed")
     * @return state JSON string
     */
    public static String buildStateJson(
        final AiSession session,
        final ExecutionContext context,
        final String runId,
        final int activeStepIndex,
        final String runnerStatus
    )
    {
        return buildStateJson(session, context, runId, activeStepIndex, runnerStatus, null);
    }

    public static String buildStateJson(
        final AiSession session,
        final ExecutionContext context,
        final String runId,
        final int activeStepIndex,
        final String runnerStatus,
        final String pauseId
    )
    {
        final TestExecutionReport report = context != null
            ? (TestExecutionReport) context.getTransientData().get("testExecutionReport")
            : null;
        return buildStateJson(session, context, runId, activeStepIndex, runnerStatus, pauseId, report);
    }

    public static String buildStateJson(
        final AiSession session,
        final ExecutionContext context,
        final String runId,
        final int activeStepIndex,
        final String runnerStatus,
        final String pauseId,
        final TestExecutionReport report
    )
    {
        final TestExecutionReport execReport = report != null
            ? report
            : (context != null ? (TestExecutionReport) context.getTransientData().get("testExecutionReport") : null);

        final JsonObject state = new JsonObject();
        state.addProperty("runId", runId != null ? runId : "live-run");
        state.addProperty("status", runnerStatus != null ? runnerStatus : "running");
        state.addProperty("currentStepIndex", activeStepIndex);
        if (pauseId != null && !pauseId.isEmpty())
        {
            state.addProperty("pauseId", pauseId);
        }

        // Test Info metadata
        final JsonArray tags = new JsonArray();
        if (context != null)
        {
            final String testClass = (String) context.getTransientData().get("testClass");
            final String testMethod = (String) context.getTransientData().get("testMethod");
            String datasetLabel = (String) context.getTransientData().get(ExecutionContext.KEY_ACTIVE_DATASET_LABEL);
            if (datasetLabel == null || datasetLabel.isEmpty())
            {
                datasetLabel = (String) context.getTransientData().get("datasetLabel");
            }
            if (datasetLabel == null || datasetLabel.isEmpty())
            {
                datasetLabel = (String) context.getTransientData().get("datasetId");
            }
            final String yamlSource = (String) context.getTransientData().get("yamlSource");
            final String playbookFileRaw = (String) context.getTransientData().get("playbookFile");
            final String playbookRecordingFile = (String) context.getTransientData().get("playbookRecordingFile");
            final String playbookFile = playbookFileRaw != null ? playbookFileRaw : yamlSource;
            final String simpleClass = testClass != null && testClass.contains(".") ? testClass.substring(testClass.lastIndexOf('.') + 1) : testClass;

            String testName = (String) context.getTransientData().get("testName");
            if (testName == null || testName.isEmpty())
            {
                if (playbookFile != null && !playbookFile.isEmpty())
                {
                    testName = playbookFile + (datasetLabel != null && !datasetLabel.isEmpty() ? " · " + datasetLabel : "");
                }
                else if (simpleClass != null)
                {
                    testName = simpleClass + (datasetLabel != null && !datasetLabel.isEmpty() ? " · " + datasetLabel : "");
                }
                else
                {
                    testName = "Live Test Run";
                }
            }
            state.addProperty("testName", testName);

            if (playbookFile != null)
            {
                state.addProperty("playbookFile", playbookFile);
            }
            if (playbookRecordingFile != null)
            {
                state.addProperty("playbookRecordingFile", playbookRecordingFile);
            }
            if (testClass != null)
            {
                state.addProperty("testFile", testClass + (testMethod != null ? "#" + testMethod : ""));
            }

            if (datasetLabel != null && !datasetLabel.isEmpty())
            {
                state.addProperty("testId", datasetLabel);
                state.addProperty("datasetId", datasetLabel);
            }

            String locale = context != null ? (String) context.getTransientData().get("locale") : null;
            if ((locale == null || locale.isEmpty()) && datasetLabel != null && !datasetLabel.isEmpty())
            {
                locale = extractLocaleFromDatasetId(datasetLabel);
            }
            if (locale != null && !locale.isEmpty())
            {
                state.addProperty("locale", locale);
            }

            if (testClass != null)
            {
                tags.add(simpleClass);
            }
            if (testMethod != null)
            {
                tags.add(testMethod);
            }
            if (datasetLabel != null && !datasetLabel.isEmpty())
            {
                tags.add("Dataset: " + datasetLabel);
            }
        }
        state.add("junitTags", tags);

        // Browser information
        String browser = context != null ? (String) context.getTransientData().get("browser") : null;
        if (browser == null || browser.isEmpty())
        {
            try
            {
                browser = com.xceptance.neodymium.util.Neodymium.getBrowserProfileName();
            }
            catch (final Throwable ignored)
            {
            }
        }
        if (browser == null || browser.isEmpty())
        {
            try
            {
                browser = com.xceptance.neodymium.util.Neodymium.getBrowserName();
            }
            catch (final Throwable ignored)
            {
            }
        }
        if (browser == null || browser.isEmpty())
        {
            if (com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted())
            {
                try
                {
                    final org.openqa.selenium.WebDriver webDriver = com.codeborne.selenide.WebDriverRunner.getWebDriver();
                    if (webDriver instanceof org.openqa.selenium.HasCapabilities)
                    {
                        browser = ((org.openqa.selenium.HasCapabilities) webDriver).getCapabilities().getBrowserName();
                    }
                }
                catch (final Throwable ignored)
                {
                }
            }
        }

        if (browser != null && !browser.isEmpty())
        {
            state.addProperty("browser", browser);
        }

        // Execution mode
        Object modeObj = context != null ? context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE) : null;
        if (modeObj == null && context != null)
        {
            modeObj = context.getTransientData().get("executionMode");
        }
        if (modeObj == null)
        {
            try
            {
                modeObj = org.neodymium.ai.config.AiConfiguration.getInstance().getExecutionMode();
            }
            catch (final Throwable ignored)
            {
            }
        }
        if (modeObj != null)
        {
            final String modeStr = modeObj.toString();
            state.addProperty("executionMode", modeStr);
        }

        // Test execution start time
        final long testStartTimeMs = context != null ? context.getStartTimeMs() : System.currentTimeMillis();
        state.addProperty("startTime", java.time.Instant.ofEpochMilli(testStartTimeMs).toString());

        // Data bindings from SessionData
        if (context != null && context.getSessionData() != null)
        {
            final JsonObject bindings = new JsonObject();
            for (final Map.Entry<String, String> entry : context.getSessionData().getAllVariables().entrySet())
            {
                bindings.addProperty(entry.getKey(), entry.getValue());
            }
            state.add("dataBindings", bindings);

            final JsonObject localBindings = new JsonObject();
            for (final Map.Entry<String, String> entry : context.getSessionData().getStaticDataVariables().entrySet())
            {
                localBindings.addProperty(entry.getKey(), entry.getValue());
            }
            state.add("localDataBindings", localBindings);
        }

        // Playbook steps block
        final JsonObject blocks = new JsonObject();
        final JsonArray beforeArray = new JsonArray();
        final JsonArray stepsArray = new JsonArray();
        final JsonArray afterArray = new JsonArray();

        if (context != null)
        {
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> beforeSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> flatSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> afterSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.afterSteps");

            boolean beforeActive = false;
            boolean stepsActive = false;
            boolean afterActive = false;

            if (beforeSteps != null && beforeSteps.stream().anyMatch(s -> s.getStatus() == PlaybookStepStatus.RUNNING))
            {
                beforeActive = true;
            }
            else if (afterSteps != null && afterSteps.stream().anyMatch(s -> s.getStatus() == PlaybookStepStatus.RUNNING))
            {
                afterActive = true;
            }
            else
            {
                stepsActive = true;
            }

            final boolean beforePassed = !beforeActive && (stepsActive || afterActive);
            final boolean stepsPassed = !beforeActive && !stepsActive && afterActive;

            if (beforeSteps != null)
            {
                for (int i = 0; i < beforeSteps.size(); i++)
                {
                    final PlaybookStep step = beforeSteps.get(i);
                    final JsonObject stepObj = serializeStep(step, i, activeStepIndex, context, "before", beforeActive, beforePassed, execReport);
                    beforeArray.add(stepObj);
                }
            }

            if (flatSteps != null)
            {
                for (int i = 0; i < flatSteps.size(); i++)
                {
                    final PlaybookStep step = flatSteps.get(i);
                    final JsonObject stepObj = serializeStep(step, i, activeStepIndex, context, "playbook", stepsActive, stepsPassed, execReport);
                    stepsArray.add(stepObj);
                }
            }

            if (afterSteps != null)
            {
                for (int i = 0; i < afterSteps.size(); i++)
                {
                    final PlaybookStep step = afterSteps.get(i);
                    final JsonObject stepObj = serializeStep(step, i, activeStepIndex, context, "after", afterActive, false, execReport);
                    afterArray.add(stepObj);
                }
            }
        }

        blocks.add("before", beforeArray);
        blocks.add("steps", stepsArray);
        blocks.add("after", afterArray);
        state.add("blocks", blocks);

        // Enrich with TestExecutionReport data if available
        if (execReport != null)
        {
            state.add("metrics", serializeMetrics(execReport.getMetrics()));
            state.add("llmCalls", serializeLlmCalls(execReport.getLlmCalls()));
            state.add("screenshots", serializeScreenshots(execReport.getScreenshots()));
            state.add("warnings", serializeWarnings(execReport.getWarnings()));

            if (execReport.getFailureReason() != null && !execReport.getFailureReason().isBlank())
            {
                state.addProperty("error", execReport.getFailureReason());
                state.addProperty("failureReason", execReport.getFailureReason());
            }
            if (execReport.getFailureStackTrace() != null && !execReport.getFailureStackTrace().isBlank())
            {
                state.addProperty("failureStackTrace", execReport.getFailureStackTrace());
            }
            if (execReport.getVisualRcaExplanation() != null && !execReport.getVisualRcaExplanation().isBlank())
            {
                state.addProperty("visualRcaExplanation", execReport.getVisualRcaExplanation());
            }
            if (execReport.getDurationMs() > 0)
            {
                state.addProperty("duration", execReport.getDurationMs());
            }
            if (execReport.getEndTimeMs() > 0)
            {
                state.addProperty("endTime", java.time.Instant.ofEpochMilli(execReport.getEndTimeMs()).toString());
            }
        }

        // Top-level reasoning property for the active step
        String topReasoning = null;
        if (context != null)
        {
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> flatSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
            if (flatSteps != null && activeStepIndex >= 0 && activeStepIndex < flatSteps.size())
            {
                final PlaybookStep activeStep = flatSteps.get(activeStepIndex);
                if (activeStep != null && activeStep.getReasoning() != null && !activeStep.getReasoning().isBlank())
                {
                    topReasoning = activeStep.getReasoning();
                }
            }
        }
        if (topReasoning == null)
        {
            if ("running".equalsIgnoreCase(runnerStatus))
            {
                topReasoning = "AI is thinking...";
            }
            else if ("paused".equalsIgnoreCase(runnerStatus))
            {
                topReasoning = "AI planned action(s). Please review and approve.";
            }
        }
        if (topReasoning != null)
        {
            state.addProperty("reasoning", topReasoning);
        }

        // Top-level failure error message fallback if not set by execReport
        if ("failed".equalsIgnoreCase(runnerStatus) && !state.has("error"))
        {
            String failureMessage = null;
            if (context != null)
            {
                @SuppressWarnings("unchecked")
                final List<PlaybookStep> flatSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.flatSteps");
                if (flatSteps != null)
                {
                    for (final PlaybookStep s : flatSteps)
                    {
                        if (s != null && s.getFailureReason() != null && !s.getFailureReason().isBlank())
                        {
                            failureMessage = s.getFailureReason();
                            break;
                        }
                    }
                }
            }
            if (failureMessage != null)
            {
                state.addProperty("error", failureMessage);
                state.addProperty("failureReason", failureMessage);
            }
        }

        return GSON.toJson(state);
    }

    private static JsonObject serializeStep(
        final PlaybookStep step,
        final int stepIndex,
        final int activeStepIndex,
        final ExecutionContext context,
        final String source,
        final boolean isCurrentSection,
        final boolean isPastSection,
        final TestExecutionReport report
    )
    {
        final JsonObject obj = new JsonObject();
        obj.addProperty("id", source + "_" + stepIndex);
        obj.addProperty("index", stepIndex + 1);
        obj.addProperty("instruction", step.getInstruction() != null ?ExecutionContext.getActiveContext().getSessionData().resolveVariables(step.getInstruction()) : "");
        obj.addProperty("line", step.getLineNumber());
        obj.addProperty("file", step.getSourceFile() != null ? step.getSourceFile() : "");

        final ExecutionMode mode = context != null
            ? (ExecutionMode) context.getTransientData().get(ExecutionContext.KEY_EXECUTION_MODE)
            : null;

        String stepEngine = "llm";
        String origin = "LLM";

        if (step.getStatus() == PlaybookStepStatus.HEALED)
        {
            stepEngine = "healed";
            origin = "HEALED";
        }
        else if (step.getSourceFile() != null && step.getSourceFile().endsWith(".java"))
        {
            stepEngine = "java";
            origin = "JAVA";
        }
        else if ((mode != null && mode.isReplay())
            || (context != null && Boolean.TRUE.equals(context.getTransientData().get("isReplayRun")))
            || (step.getSourceFile() != null && step.getSourceFile().endsWith(".json")))
        {
            stepEngine = "recording";
            origin = "PLAYBOOK";
        }
        obj.addProperty("source", stepEngine);
        obj.addProperty("origin", origin);

        PlaybookStepStatus status = step.getStatus();
        if (status == null || status == PlaybookStepStatus.PENDING)
        {
            if (isCurrentSection)
            {
                if (stepIndex < activeStepIndex)
                {
                    status = PlaybookStepStatus.SUCCESS;
                }
                else if (stepIndex == activeStepIndex)
                {
                    status = PlaybookStepStatus.RUNNING;
                }
                else
                {
                    status = PlaybookStepStatus.PENDING;
                }
            }
            else if (isPastSection)
            {
                status = PlaybookStepStatus.SUCCESS;
            }
            else
            {
                status = PlaybookStepStatus.PENDING;
            }
        }

        final String statusStr = switch (status)
        {
            case SUCCESS, HEALED -> "passed";
            case RUNNING -> "running";
            case FAILED -> "failed";
            case SKIPPED -> "skipped";
            case PENDING, SPLITTED -> "pending";
        };
        obj.addProperty("status", statusStr);

        Long stepStart = step.getStartTimeMs();
        if (stepStart == null && isCurrentSection && stepIndex == activeStepIndex && context != null)
        {
            stepStart = (Long) context.getTransientData().get("KEY_STEP_START_TIME");
        }
        if (stepStart != null && stepStart > 0)
        {
            obj.addProperty("startTimestamp", java.time.Instant.ofEpochMilli(stepStart).toString());
        }

        Long stepDuration = step.getDurationMs();
        if ((stepDuration == null || stepDuration == 0) && stepStart != null && stepStart > 0 && "running".equals(statusStr))
        {
            stepDuration = Math.max(0L, System.currentTimeMillis() - stepStart);
        }
        if (stepDuration != null)
        {
            obj.addProperty("duration", stepDuration);
        }

        if (step.getFailureReason() != null && !step.getFailureReason().isBlank())
        {
            obj.addProperty("error", step.getFailureReason());
            obj.addProperty("failureReason", step.getFailureReason());
        }

        if (isCurrentSection && stepIndex == activeStepIndex && context != null)
        {
            final String currentScreenshot = (String) context.getTransientData().get("currentScreenshot");
            if (currentScreenshot != null && !currentScreenshot.isEmpty())
            {
                obj.addProperty("screenshot", currentScreenshot);
            }
        }

        if (step.getSsimScore() != null)
        {
            obj.addProperty("ssimScore", step.getSsimScore());
        }
        if (step.getSsimMinScore() != null)
        {
            obj.addProperty("ssimMinScore", step.getSsimMinScore());
        }
        if (step.getBaselineMatrixPng() != null)
        {
            obj.addProperty("baselineMatrixPng", step.getBaselineMatrixPng());
        }
        if (step.getReplayMatrixPng() != null)
        {
            obj.addProperty("replayMatrixPng", step.getReplayMatrixPng());
        }
        if (step.getScreenshotHashDim() != null)
        {
            obj.addProperty("screenshotHashDim", step.getScreenshotHashDim());
        }

        final JsonArray actionsArray = new JsonArray();
        if (step.getActions() != null)
        {
            for (final Action action : step.getActions())
            {
                if (action != null)
                {
                    final JsonObject actObj = new JsonObject();
                    actObj.addProperty("type", action.getType() != null ? action.getType() : "");
                    actObj.addProperty("target", action.getTarget() != null ? action.getTarget() : "");
                    actObj.addProperty("value", action.getValue() != null ? action.getValue() : "");
                    actObj.addProperty("description", action.getDescription() != null ? action.getDescription() : "");
                    actObj.addProperty("reasoning", action.getReasoning() != null ? action.getReasoning() : "");
                    actionsArray.add(actObj);
                }
            }
        }

        // Attach details from matching ReportStepEntry if report is available
        ReportStepEntry reportStep = null;
        if (report != null && report.getSteps() != null)
        {
            for (final ReportStepEntry entry : report.getSteps())
            {
                if (entry != null && entry.getStepIndex() == stepIndex)
                {
                    reportStep = entry;
                    break;
                }
            }
        }

        if (reportStep != null)
        {
            if (reportStep.getRawInstruction() != null)
            {
                obj.addProperty("rawInstruction", reportStep.getRawInstruction());
            }
            obj.addProperty("bug", reportStep.isBug());
            if (reportStep.getBugDetails() != null)
            {
                obj.addProperty("bugDetails", reportStep.getBugDetails());
            }
            obj.addProperty("optional", reportStep.isOptional());
            obj.addProperty("continueOnError", reportStep.isContinueOnError());
            obj.addProperty("noHealing", reportStep.isNoHealing());
            obj.addProperty("visual", reportStep.isVisual());

            if (!reportStep.getActions().isEmpty())
            {
                obj.add("actions", serializeReportActions(reportStep.getActions()));
            }
            else
            {
                obj.add("actions", actionsArray);
            }

            if (!reportStep.getScreenshots().isEmpty())
            {
                obj.add("screenshots", serializeScreenshots(reportStep.getScreenshots()));
            }
            if (!reportStep.getLlmCalls().isEmpty())
            {
                obj.add("llmCalls", serializeLlmCalls(reportStep.getLlmCalls()));
            }
            obj.add("stats", serializeStepStats(reportStep));
            if (!reportStep.getSubSteps().isEmpty())
            {
                obj.add("subSteps", serializeSubSteps(reportStep.getSubSteps()));
            }
        }
        else
        {
            obj.add("actions", actionsArray);
        }

        if (step.getReasoning() != null && !step.getReasoning().isBlank())
        {
            obj.addProperty("reasoning", step.getReasoning());
        }
        else if (reportStep != null && reportStep.getReasoning() != null && !reportStep.getReasoning().isBlank())
        {
            obj.addProperty("reasoning", reportStep.getReasoning());
        }
        else if (step.getActions() != null && !step.getActions().isEmpty())
        {
            final StringBuilder sb = new StringBuilder();
            for (final Action action : step.getActions())
            {
                if (action != null && action.getReasoning() != null && !action.getReasoning().isBlank())
                {
                    if (!sb.isEmpty())
                    {
                        sb.append(" ");
                    }
                    sb.append(action.getReasoning().trim());
                }
            }
            if (!sb.isEmpty())
            {
                obj.addProperty("reasoning", sb.toString());
            }
        }

        return obj;
    }

    private static JsonObject serializeMetrics(final ReportMetrics metrics)
    {
        final JsonObject obj = new JsonObject();
        if (metrics == null)
        {
            return obj;
        }
        obj.addProperty("totalSteps", metrics.getTotalSteps());
        obj.addProperty("healedSteps", metrics.getHealedSteps());
        obj.addProperty("failedSteps", metrics.getFailedSteps());
        obj.addProperty("skippedSteps", metrics.getSkippedSteps());
        obj.addProperty("totalLlmCalls", metrics.getTotalLlmCalls());
        obj.addProperty("tokenUsageInput", metrics.getTokenUsageInput());
        obj.addProperty("tokenUsageOutput", metrics.getTokenUsageOutput());
        obj.addProperty("tokenUsageCached", metrics.getTokenUsageCached());
        obj.addProperty("totalTokens", metrics.getTotalTokens());
        obj.addProperty("estimatedCostUsd", metrics.getEstimatedCostUsd());
        obj.addProperty("totalReplays", metrics.getTotalReplays());
        obj.addProperty("internalCacheHits", metrics.getInternalCacheHits());
        obj.addProperty("totalEscalations", metrics.getTotalEscalations());

        if (metrics.getContextLevelCounts() != null && !metrics.getContextLevelCounts().isEmpty())
        {
            final JsonObject ctxLevels = new JsonObject();
            for (final Map.Entry<String, Integer> entry : metrics.getContextLevelCounts().entrySet())
            {
                ctxLevels.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add("contextLevelCounts", ctxLevels);
        }

        final JsonObject categories = new JsonObject();
        if (metrics.getAction() != null)
        {
            categories.add("action", serializeCategoryUsage(metrics.getAction()));
        }
        if (metrics.getPesap() != null)
        {
            categories.add("pesap", serializeCategoryUsage(metrics.getPesap()));
        }
        if (metrics.getJudge() != null)
        {
            categories.add("judge", serializeCategoryUsage(metrics.getJudge()));
        }
        if (metrics.getVerification() != null)
        {
            categories.add("verification", serializeCategoryUsage(metrics.getVerification()));
        }
        if (metrics.getVisualRca() != null)
        {
            categories.add("visualRca", serializeCategoryUsage(metrics.getVisualRca()));
        }
        if (metrics.getTotal() != null)
        {
            categories.add("total", serializeCategoryUsage(metrics.getTotal()));
        }
        obj.add("categories", categories);

        return obj;
    }

    private static JsonObject serializeCategoryUsage(final CategoryTokenUsage usage)
    {
        final JsonObject obj = new JsonObject();
        if (usage != null)
        {
            obj.addProperty("calls", usage.getCalls());
            obj.addProperty("inputTokens", usage.getInputTokens());
            obj.addProperty("outputTokens", usage.getOutputTokens());
            obj.addProperty("cachedTokens", usage.getCachedTokens());
            obj.addProperty("estimatedCostUsd", usage.getEstimatedCostUsd());
        }
        return obj;
    }

    private static JsonArray serializeLlmCalls(final List<ReportLlmCallEntry> calls)
    {
        final JsonArray arr = new JsonArray();
        if (calls != null)
        {
            for (final ReportLlmCallEntry call : calls)
            {
                if (call != null)
                {
                    final JsonObject callObj = new JsonObject();
                    callObj.addProperty("stepIndex", call.getStepIndex());
                    callObj.addProperty("capability", call.getCapability());
                    callObj.addProperty("modelName", call.getModelName());
                    callObj.addProperty("durationMs", call.getDurationMs());
                    callObj.addProperty("inputTokens", call.getInputTokens());
                    callObj.addProperty("outputTokens", call.getOutputTokens());
                    callObj.addProperty("cachedTokens", call.getCachedTokens());
                    callObj.addProperty("totalTokens", call.getTotalTokens());
                    callObj.addProperty("estimatedCostUsd", call.getEstimatedCostUsd());
                    callObj.addProperty("systemPrompt", call.getSystemPrompt());
                    callObj.addProperty("userPrompt", call.getUserPrompt());
                    callObj.addProperty("responseContent", call.getResponseContent());
                    arr.add(callObj);
                }
            }
        }
        return arr;
    }

    private static JsonArray serializeScreenshots(final List<ReportScreenshotEntry> screenshots)
    {
        final JsonArray arr = new JsonArray();
        if (screenshots != null)
        {
            for (final ReportScreenshotEntry screenshot : screenshots)
            {
                if (screenshot != null)
                {
                    final JsonObject scObj = new JsonObject();
                    scObj.addProperty("label", screenshot.getName());
                    scObj.addProperty("name", screenshot.getName());
                    scObj.addProperty("stepIndex", screenshot.getStepIndex());
                    scObj.addProperty("mediaType", screenshot.getMediaType());
                    scObj.addProperty("base64Data", screenshot.getBase64Data());
                    scObj.addProperty("timestamp", screenshot.getTimestamp());
                    arr.add(scObj);
                }
            }
        }
        return arr;
    }

    private static JsonArray serializeWarnings(final List<String> warnings)
    {
        final JsonArray arr = new JsonArray();
        if (warnings != null)
        {
            for (final String warning : warnings)
            {
                if (warning != null)
                {
                    arr.add(warning);
                }
            }
        }
        return arr;
    }

    private static JsonArray serializeReportActions(final List<ReportActionEntry> actions)
    {
        final JsonArray arr = new JsonArray();
        if (actions != null)
        {
            for (final ReportActionEntry action : actions)
            {
                if (action != null)
                {
                    final JsonObject actObj = new JsonObject();
                    actObj.addProperty("type", action.getType() != null ? action.getType() : "");
                    actObj.addProperty("target", action.getTarget() != null ? action.getTarget() : "");
                    actObj.addProperty("value", action.getValue() != null ? action.getValue() : "");
                    actObj.addProperty("description", action.getDescription() != null ? action.getDescription() : "");
                    actObj.addProperty("reasoning", action.getReasoning() != null ? action.getReasoning() : "");
                    actObj.addProperty("success", action.isSuccess());
                    arr.add(actObj);
                }
            }
        }
        return arr;
    }

    private static JsonObject serializeStepStats(final ReportStepEntry step)
    {
        final JsonObject obj = new JsonObject();
        if (step != null)
        {
            obj.addProperty("escalations", step.getEscalations());
            obj.addProperty("contextLevels", step.getContextLevels());
            obj.addProperty("pesapCalls", step.getPesapCalls());
            obj.addProperty("pesapInputTokens", step.getPesapInputTokens());
            obj.addProperty("pesapOutputTokens", step.getPesapOutputTokens());
            obj.addProperty("pesapCachedTokens", step.getPesapCachedTokens());
            obj.addProperty("standardCalls", step.getStandardCalls());
            obj.addProperty("standardInputTokens", step.getStandardInputTokens());
            obj.addProperty("standardOutputTokens", step.getStandardOutputTokens());
            obj.addProperty("standardCachedTokens", step.getStandardCachedTokens());
        }
        return obj;
    }

    private static JsonArray serializeSubSteps(final List<ReportStepEntry> subSteps)
    {
        final JsonArray arr = new JsonArray();
        if (subSteps != null)
        {
            for (final ReportStepEntry sub : subSteps)
            {
                if (sub != null)
                {
                    final JsonObject subObj = new JsonObject();
                    subObj.addProperty("stepIndex", sub.getStepIndex());
                    subObj.addProperty("instruction", sub.getInstruction() != null ? sub.getInstruction() : "");
                    subObj.addProperty("rawInstruction", sub.getRawInstruction());
                    subObj.addProperty("status", sub.getStatus());
                    subObj.addProperty("startTimeMs", sub.getStartTimeMs());
                    subObj.addProperty("durationMs", sub.getDurationMs());
                    subObj.addProperty("reasoning", sub.getReasoning());
                    subObj.addProperty("failureReason", sub.getFailureReason());
                    if (!sub.getActions().isEmpty())
                    {
                        subObj.add("actions", serializeReportActions(sub.getActions()));
                    }
                    if (!sub.getScreenshots().isEmpty())
                    {
                        subObj.add("screenshots", serializeScreenshots(sub.getScreenshots()));
                    }
                    if (!sub.getLlmCalls().isEmpty())
                    {
                        subObj.add("llmCalls", serializeLlmCalls(sub.getLlmCalls()));
                    }
                    if (!sub.getSubSteps().isEmpty())
                    {
                        subObj.add("subSteps", serializeSubSteps(sub.getSubSteps()));
                    }
                    arr.add(subObj);
                }
            }
        }
        return arr;
    }
}
