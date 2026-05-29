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
import com.xceptance.neodymium.ai.action.plugins.AssertAction;
import com.xceptance.neodymium.ai.action.plugins.ClickAction;
import com.xceptance.neodymium.util.Neodymium;

public class ActionRegistryTest {

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
        Neodymium.aiConfiguration().setProperty("neodymium.ai.plugins", "");
    }

    @Test
    public void testCorePluginRegistration() {
        ActionRegistry.init();
        
        Assertions.assertNotNull(ActionRegistry.getPlugin(AssertAction.ACTION_NAME), "ASSERT plugin should be registered by default");
        Assertions.assertNotNull(ActionRegistry.getPlugin("CLICK"), "CLICK plugin should be registered by default");
    }

    @Test
    public void testCaseInsensitiveRetrieval() {
        ActionRegistry.init();
        
        AiActionPlugin plugin1 = ActionRegistry.getPlugin("click");
        AiActionPlugin plugin2 = ActionRegistry.getPlugin("CLICK");
        AiActionPlugin plugin3 = ActionRegistry.getPlugin("cLiCk");

        Assertions.assertNotNull(plugin1);
        Assertions.assertSame(plugin1, plugin2);
        Assertions.assertSame(plugin2, plugin3);
    }

    public static class DummyActionPlugin implements AiActionPlugin {
        @Override
        public String getActionName() { return "DUMMY"; }

        @Override
        public List<Action> parseDirectInstruction(String instruction) { return null; }

        @Override
        public boolean requiresLlm(Action action) { return false; }

        @Override
        public String getPromptInstructions() { return "Dummy prompt"; }

        @Override
        public void execute(Action action, Object testInstance, ActionExecutor executor) {}
    }

    @Test
    public void testManualRegistration() {
        ActionRegistry.init();
        
        Assertions.assertNull(ActionRegistry.getPlugin("DUMMY"));
        
        ActionRegistry.register(new DummyActionPlugin());
        
        Assertions.assertNotNull(ActionRegistry.getPlugin("DUMMY"));
        Assertions.assertTrue(ActionRegistry.getPlugin("DUMMY") instanceof DummyActionPlugin);
    }

    @Test
    public void testCustomConfigurationLoadingAndExecution() {
        // Set the property before init
        Neodymium.aiConfiguration().setProperty("neodymium.ai.plugins", DummyActionPlugin.class.getName());
        
        ActionRegistry.init();
        
        AiActionPlugin plugin = ActionRegistry.getPlugin("DUMMY");
        Assertions.assertNotNull(plugin, "Custom plugin should be loaded from configuration");
        Assertions.assertTrue(plugin instanceof DummyActionPlugin);
    }

    @Test
    public void testFaultToleranceHardFailInvalidClass() {
        Neodymium.aiConfiguration().setProperty("neodymium.ai.plugins", "com.xceptance.neodymium.NonExistentClass");
        
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            ActionRegistry.init();
        });
        
        Assertions.assertTrue(exception.getMessage().contains("Failed to load AI Action plugin: com.xceptance.neodymium.NonExistentClass"));
    }

    @Test
    public void testFaultToleranceHardFailDoesNotImplementInterface() {
        // We use String class as it exists but does not implement AiActionPlugin
        Neodymium.aiConfiguration().setProperty("neodymium.ai.plugins", "java.lang.String");
        
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            ActionRegistry.init();
        });
        
        Assertions.assertTrue(exception.getMessage().contains("does not implement AiActionPlugin"));
    }
}
