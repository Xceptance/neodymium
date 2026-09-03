/*
 * Apache License 2.0
 *
 * Copyright (c) 2026 Xceptance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neodymium.ai.executor.selenide.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.playbook.PlaybookParser;
import org.neodymium.ai.playbook.YamlPlaybookParser;
import org.neodymium.ai.resources.ClasspathResourceManager;
import org.neodymium.ai.resources.PlaybookResourceManager;
import org.neodymium.ai.session.AiSession;

/**
 * Action plugin that implements runtime execution of external playbooks.
 * Resolves recursive inclusions and pushes included steps onto the execution stack.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class IncludeAction implements BrowserActionPlugin
{
    private static final ThreadLocal<List<String>> RUNTIME_INCLUDE_STACK = ThreadLocal.withInitial(ArrayList::new);

    private final ExecutionContext context;

    /**
     * Constructs an IncludeAction with the given execution context.
     *
     * @param context the execution context
     */
    public IncludeAction(final ExecutionContext context)
    {
        this.context = context;
    }

    /**
     * Loads the target playbook, parses its steps, and pushes them onto the execution context.
     *
     * @param action the include action
     * @throws Exception if execution fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null || this.context == null)
        {
            return;
        }

        final String pathTemp = action.getTarget();
        final String path = (pathTemp == null || pathTemp.isBlank()) ? action.getValue() : pathTemp;
        if (path == null || path.isBlank())
        {
            throw new IllegalArgumentException("INCLUDE action target path is null or empty");
        }

        final List<String> stack = RUNTIME_INCLUDE_STACK.get();
        if (stack.contains(path))
        {
            final StringBuilder sb = new StringBuilder();
            for (final String s : stack)
            {
                sb.append(s).append(" -> ");
            }
            sb.append(path);
            throw new IOException("Circular dynamic inclusion detected: " + sb.toString());
        }

        stack.add(path);
        try
        {
            final AiSession session = (AiSession) this.context.getTransientData().get(ExecutionContext.KEY_SESSION);
            if (session == null)
            {
                throw new IllegalStateException("IncludeAction requires an AiSession in the execution context transient data");
            }

            final PlaybookParser parser = new YamlPlaybookParser();
            final PlaybookResourceManager manager = new ClasspathResourceManager();
            final Playbook playbook = parser.parse(path, manager);
            final List<PlaybookStep> playbookSteps = playbook.getSteps();

            // Push steps onto the stack in reverse order so they execute in the correct order (LIFO)
            for (int i = playbookSteps.size() - 1; i >= 0; i--)
            {
                final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(playbookSteps.get(i), session, this.context);
                this.context.pushStep(pipelineStep);
            }
        }
        finally
        {
            stack.remove(stack.size() - 1);
        }
    }
}
