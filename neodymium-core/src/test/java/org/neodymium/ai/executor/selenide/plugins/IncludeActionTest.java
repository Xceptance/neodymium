/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.executor.selenide.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

/**
 * Unit tests verifying include action path resolution for direct, relative, and nested playbooks.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class IncludeActionTest
{
    private ExecutionContext context;
    private IncludeAction actionPlugin;
    private PlaybookStep parentStep;

    @BeforeEach
    public void setUp()
    {
        this.context = new ExecutionContext(new SessionData(Collections.emptyMap()));
        final AiSession session = AiSession.mock();
        this.context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

        this.parentStep = new PlaybookStep("Conditional step with include");
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, this.parentStep);

        this.actionPlugin = new IncludeAction(this.context);
    }

    @Test
    @DisplayName("Verify direct classpath include executes and attaches substeps to current step")
    public void testDirectClasspathInclude() throws Exception
    {
        final Action action = new Action("INCLUDE", "com/xceptance/neodymium/ai/integration/fragments/testConditionalIncludeThenBranch_sub.steps", "Include sub steps");
        this.actionPlugin.execute(action);

        assertFalse(this.parentStep.getSubSteps().isEmpty(), "Parent step should contain substeps from include");
        assertEquals(2, this.parentStep.getSubSteps().size());
        assertEquals("Click button A", this.parentStep.getSubSteps().get(0).getInstruction());
        assertEquals("Click button B", this.parentStep.getSubSteps().get(1).getInstruction());
        assertEquals(this.parentStep, this.parentStep.getSubSteps().get(0).getParent());
    }

    @Test
    @DisplayName("Verify relative include resolves correctly using active playbookFile in execution context")
    public void testRelativeIncludeResolvedViaPlaybookFile() throws Exception
    {
        // Enclosing test playbook located in com/xceptance/neodymium/ai/integration/
        this.context.getTransientData().put("playbookFile", "com/xceptance/neodymium/ai/integration/sampleTest.yaml");

        // Relative path pointing to sibling fragments/
        final Action action = new Action("INCLUDE", "fragments/testConditionalIncludeThenBranch_sub.steps", "Include relative fragment");
        this.actionPlugin.execute(action);

        assertFalse(this.parentStep.getSubSteps().isEmpty(), "Parent step should contain substeps from relative include");
        assertEquals(2, this.parentStep.getSubSteps().size());
        assertEquals("Click button A", this.parentStep.getSubSteps().get(0).getInstruction());
    }

    @Test
    @DisplayName("Verify nested relative includes resolve correctly across inclusion levels")
    public void testNestedRelativeIncludes() throws Exception
    {
        this.context.getTransientData().put("playbookFile", "com/xceptance/neodymium/ai/integration/sampleTest.yaml");

        final Action action = new Action("INCLUDE", "fragments/nestedFirst.steps", "Include first nested fragment");
        this.actionPlugin.execute(action);

        assertNotNull(this.parentStep.getSubSteps());
        assertFalse(this.parentStep.getSubSteps().isEmpty());
    }

    @Test
    @DisplayName("Verify circular inclusion is detected and prevented with an IOException")
    public void testCircularInclusionDetection()
    {
        this.context.getTransientData().put("playbookFile", "circular-a.steps");

        final Action action = new Action("INCLUDE", "circular-a.steps", "Include self");
        assertThrows(IOException.class, () ->
        {
            this.actionPlugin.execute(action);
            this.actionPlugin.execute(action);
        });
    }

    @Test
    @DisplayName("Verify empty or null include path throws IllegalArgumentException")
    public void testEmptyPathThrowsException()
    {
        final Action emptyAction = new Action("INCLUDE", "", "Empty include");
        assertThrows(IllegalArgumentException.class, () -> this.actionPlugin.execute(emptyAction));
    }
}
