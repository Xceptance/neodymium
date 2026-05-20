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
}
