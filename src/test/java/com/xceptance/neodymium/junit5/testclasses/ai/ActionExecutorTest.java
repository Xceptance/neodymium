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
package com.xceptance.neodymium.junit5.testclasses.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionRegistry;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class ActionExecutorTest {

    private void resetRegistry() throws Exception {
        Field initializedField = ActionRegistry.class.getDeclaredField("initialized");
        initializedField.setAccessible(true);
        initializedField.set(null, false);

        Field pluginsField = ActionRegistry.class.getDeclaredField("plugins");
        pluginsField.setAccessible(true);
        ((Map<?, ?>) pluginsField.get(null)).clear();
    }

    @BeforeEach
    public void setup() throws Exception {
        resetRegistry();
    }

    @AfterEach
    public void teardown() throws Exception {
        resetRegistry();
    }

    public static class MockActionPlugin implements AiActionPlugin {
        public boolean executeCalled = false;
        public boolean preCheckCalled = false;

        @Override
        public String getActionName() { return "MOCK"; }

        @Override
        public List<Action> parseDirectInstruction(String instruction) { return null; }

        @Override
        public boolean requiresLlm(Action action) { return false; }

        @Override
        public String getPromptInstructions() { return "Mock prompt"; }

        @Override
        public void execute(Action action, Object testInstance, ActionExecutor executor) {
            executeCalled = true;
        }

        @Override
        public void preCheck(Action action, ActionExecutor executor) {
            preCheckCalled = true;
        }
    }

    @Test
    public void testSuccessfulExecutionDelegation() {
        ActionRegistry.init();
        MockActionPlugin mockPlugin = new MockActionPlugin();
        ActionRegistry.register(mockPlugin);

        ActionExecutor executor = new ActionExecutor(this);
        Action action = new Action();
        action.setType("MOCK");
        action.setDescription("Mock action description");
        action.setTarget("mockTarget");

        executor.execute(action);

        Assertions.assertTrue(mockPlugin.executeCalled, "Plugin execute() method should have been delegated to");
        Assertions.assertTrue(mockPlugin.preCheckCalled, "Plugin preCheck() method should have been delegated to");
    }

    @Test
    public void testHandlingUnknownActions() {
        ActionRegistry.init();
        ActionExecutor executor = new ActionExecutor(this);
        Action action = new Action();
        action.setType("UNKNOWN_ACTION");
        action.setDescription("Unknown action description");
        action.setTarget("unknownTarget");

        // Should not throw an exception, but should handle it gracefully (logs warning)
        Assertions.assertDoesNotThrow(() -> {
            executor.execute(action);
        }, "Executing an unknown action should not throw an exception, but handle gracefully");
    }

    @Test
    public void testCleanElementTextSingleQuotes()
    {
        final ActionExecutor executor = new ActionExecutor(this);
        final String input = "button 'Save this card'";
        final String expected = "Save this card";
        final String actual = executor.cleanElementText(input);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testCleanElementTextDoubleQuotes()
    {
        final ActionExecutor executor = new ActionExecutor(this);
        final String input = "link \"Log in\"";
        final String expected = "Log in";
        final String actual = executor.cleanElementText(input);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testCleanElementTextUnquoted()
    {
        final ActionExecutor executor = new ActionExecutor(this);
        final String input = "unquoted text";
        final String expected = "unquoted text";
        final String actual = executor.cleanElementText(input);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testCleanElementTextNull()
    {
        final ActionExecutor executor = new ActionExecutor(this);
        final String actual = executor.cleanElementText(null);
        Assertions.assertNull(actual);
    }
}
