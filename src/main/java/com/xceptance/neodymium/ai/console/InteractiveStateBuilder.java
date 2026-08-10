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

import java.util.List;
import java.util.Map;

import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
            final String datasetLabel = (String) context.getTransientData().get(ExecutionContext.KEY_ACTIVE_DATASET_LABEL);
            final String yamlSource = (String) context.getTransientData().get("yamlSource");
            final String playbookFile = (String) context.getTransientData().get("playbookFile");
            final String simpleClass = testClass != null && testClass.contains(".") ? testClass.substring(testClass.lastIndexOf('.') + 1) : testClass;

            String testName = (String) context.getTransientData().get("testName");
            if (testName == null || testName.isEmpty())
            {
                if (yamlSource != null && !yamlSource.isEmpty())
                {
                    testName = yamlSource + (datasetLabel != null && !datasetLabel.isEmpty() ? " · " + datasetLabel : "");
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

            if (yamlSource != null)
            {
                state.addProperty("yamlSource", yamlSource);
            }
            if (playbookFile != null)
            {
                state.addProperty("playbookFile", playbookFile);
            }
            if (testClass != null)
            {
                state.addProperty("testFile", testClass + (testMethod != null ? "#" + testMethod : ""));
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

        // Data bindings from SessionData
        if (context != null && context.getSessionData() != null)
        {
            final JsonObject bindings = new JsonObject();
            for (final Map.Entry<String, String> entry : context.getSessionData().getAllVariables().entrySet())
            {
                bindings.addProperty(entry.getKey(), entry.getValue());
            }
            state.add("dataBindings", bindings);
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
                    final JsonObject stepObj = serializeStep(step, i, activeStepIndex, context, "before", beforeActive, beforePassed);
                    beforeArray.add(stepObj);
                }
            }

            if (flatSteps != null)
            {
                for (int i = 0; i < flatSteps.size(); i++)
                {
                    final PlaybookStep step = flatSteps.get(i);
                    final JsonObject stepObj = serializeStep(step, i, activeStepIndex, context, "playbook", stepsActive, stepsPassed);
                    stepsArray.add(stepObj);
                }
            }

            if (afterSteps != null)
            {
                for (int i = 0; i < afterSteps.size(); i++)
                {
                    final PlaybookStep step = afterSteps.get(i);
                    final JsonObject stepObj = serializeStep(step, i, activeStepIndex, context, "after", afterActive, false);
                    afterArray.add(stepObj);
                }
            }
        }

        blocks.add("before", beforeArray);
        blocks.add("steps", stepsArray);
        blocks.add("after", afterArray);
        state.add("blocks", blocks);

        return GSON.toJson(state);
    }

    private static JsonObject serializeStep(
        final PlaybookStep step,
        final int stepIndex,
        final int activeStepIndex,
        final ExecutionContext context,
        final String source,
        final boolean isCurrentSection,
        final boolean isPastSection
    )
    {
        final JsonObject obj = new JsonObject();
        obj.addProperty("id", source + "_" + stepIndex);
        obj.addProperty("index", stepIndex + 1);
        obj.addProperty("instruction", step.getInstruction() != null ? step.getInstruction() : "");
        obj.addProperty("line", step.getLineNumber());
        obj.addProperty("file", step.getSourceFile() != null ? step.getSourceFile() : "");
        obj.addProperty("source", source);

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

        if (isCurrentSection && stepIndex == activeStepIndex && context != null)
        {
            final String currentScreenshot = (String) context.getTransientData().get("currentScreenshot");
            if (currentScreenshot != null && !currentScreenshot.isEmpty())
            {
                obj.addProperty("screenshot", currentScreenshot);
            }
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
                    actionsArray.add(actObj);
                }
            }
        }
        obj.add("actions", actionsArray);

        return obj;
    }
}
