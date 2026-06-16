/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.ai.core.AiAgent;
import com.xceptance.neodymium.common.testdata.util.YamlFileReader;

/**
 * Executes steps from an external include file as part of the AI execution loop.
 */
public class IncludeAction implements AiActionPlugin
{
    public static final String ACTION_NAME = "INCLUDE";

    @Override
    public final String getActionName()
    {
        return ACTION_NAME;
    }

    @Override
    public final List<Action> parseDirectInstruction(final String instruction)
    {
        return null;
    }

    @Override
    public final boolean requiresLlm(final Action action)
    {
        return false;
    }

    @Override
    public final String getPromptInstructions()
    {
        return "INCLUDE: Used to execute steps from an external file. Requires target ('tg') set to the path of the file (e.g. 'fragments/login.steps'). Use this action type whenever the instruction asks to '_include' a file path.";
    }

    @Override
    public final void execute(final Action action, final Object testInstance, final ActionExecutor executor) throws ActionExecutionException
    {
        final String path = action.getTarget();
        if (path == null || path.isBlank())
        {
            throw new ActionExecutionException("INCLUDE action target path is null or empty");
        }

        final List<YamlFileReader.Step> includedSteps = YamlFileReader.loadInclude(path);

        final AiAgent agent = AiAgent.getActiveAgent();
        if (agent == null)
        {
            throw new ActionExecutionException("No active AI Agent found to execute INCLUDE action: " + path);
        }

        try
        {
            agent.executeIncludeSteps(includedSteps);
        }
        catch (final Exception e)
        {
            throw new ActionExecutionException("Failed executing included steps from path: " + path, e);
        }
    }
}
