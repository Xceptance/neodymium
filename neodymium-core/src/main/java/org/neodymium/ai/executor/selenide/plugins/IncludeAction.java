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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.neodymium.util.Neodymium;
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
        final String rawPath = (pathTemp == null || pathTemp.isBlank()) ? action.getValue() : pathTemp;
        if (rawPath == null || rawPath.isBlank())
        {
            throw new IllegalArgumentException("INCLUDE action target path is null or empty");
        }

        final PlaybookResourceManager manager = new ClasspathResourceManager();
        final List<String> stack = RUNTIME_INCLUDE_STACK.get();
        final String resolvedPath = resolveIncludePath(rawPath, manager, stack);

        if (stack.contains(resolvedPath) || stack.contains(rawPath))
        {
            final StringBuilder sb = new StringBuilder();
            for (final String s : stack)
            {
                sb.append(s).append(" -> ");
            }
            sb.append(resolvedPath);
            throw new IOException("Circular dynamic inclusion detected: " + sb.toString());
        }

        stack.add(resolvedPath);
        try
        {
            final AiSession session = (AiSession) this.context.getTransientData().get(ExecutionContext.KEY_SESSION);
            if (session == null)
            {
                throw new IllegalStateException("IncludeAction requires an AiSession in the execution context transient data");
            }

            final PlaybookParser parser = new YamlPlaybookParser();
            final Playbook playbook = parser.parse(resolvedPath, manager);
            final List<PlaybookStep> playbookSteps = playbook.getSteps();

            final PlaybookStep currentStep = (PlaybookStep) this.context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
            if (currentStep != null && playbookSteps != null)
            {
                for (final PlaybookStep subStep : playbookSteps)
                {
                    subStep.setParent(currentStep);
                    if (subStep.getSourceFile() == null || !subStep.getSourceFile().contains("/"))
                    {
                        subStep.setSourceFile(resolvedPath);
                    }
                    if (!currentStep.getSubSteps().contains(subStep))
                    {
                        currentStep.getSubSteps().add(subStep);
                    }
                }
            }

            // Push steps onto the stack in reverse order so they execute in the correct order (LIFO)
            for (int i = playbookSteps.size() - 1; i >= 0; i--)
            {
                final PlaybookStep subStep = playbookSteps.get(i);
                if (currentStep != null && subStep.getParent() == null)
                {
                    subStep.setParent(currentStep);
                }
                final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(subStep, session, this.context);
                this.context.pushStep(pipelineStep);
            }
        }
        finally
        {
            stack.remove(stack.size() - 1);
        }
    }

    private String resolveIncludePath(final String path, final PlaybookResourceManager manager, final List<String> stack)
    {
        if (resourceExists(path, manager))
        {
            return path;
        }

        final List<String> candidateParents = new ArrayList<>();

        if (!stack.isEmpty())
        {
            candidateParents.add(stack.get(stack.size() - 1));
        }

        final PlaybookStep currentStep = (PlaybookStep) this.context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        if (currentStep != null)
        {
            if (currentStep.getSourceFile() != null && !currentStep.getSourceFile().isBlank())
            {
                candidateParents.add(currentStep.getSourceFile());
            }
            if (currentStep.getParent() != null && currentStep.getParent().getSourceFile() != null && !currentStep.getParent().getSourceFile().isBlank())
            {
                candidateParents.add(currentStep.getParent().getSourceFile());
            }
        }

        final Object playbookFileObj = this.context.getTransientData().get("playbookFile");
        if (playbookFileObj instanceof String pb && !pb.isBlank())
        {
            candidateParents.add(pb);
        }

        final Object yamlSourceObj = this.context.getTransientData().get("yamlSource");
        if (yamlSourceObj instanceof String ys && !ys.isBlank())
        {
            candidateParents.add(ys);
        }

        if (Neodymium.getData() != null && Neodymium.getData().exists("neodymium.sourceFile"))
        {
            final String sf = Neodymium.getData().asString("neodymium.sourceFile");
            if (sf != null && !sf.isBlank())
            {
                candidateParents.add(sf);
            }
        }

        final String testdataSourceFile = Neodymium.getTestdataSourceFile();
        if (testdataSourceFile != null && !testdataSourceFile.isBlank())
        {
            candidateParents.add(testdataSourceFile);
        }

        for (final String parent : candidateParents)
        {
            if (parent != null && !parent.isBlank())
            {
                final String resolved = manager.resolveInclude(parent, path);
                if (resourceExists(resolved, manager))
                {
                    return resolved;
                }
            }
        }

        return path;
    }

    private boolean resourceExists(final String identifier, final PlaybookResourceManager manager)
    {
        if (identifier == null || identifier.isBlank())
        {
            return false;
        }
        try (final InputStream in = manager.read(identifier))
        {
            return in != null;
        }
        catch (final Exception ignored)
        {
            return false;
        }
    }
}
