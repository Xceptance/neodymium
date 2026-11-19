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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.tool.Tool;

/**
 * Unit tests verifying the functionality of the JavaMethodAction reflection invocation plugin.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class JavaMethodActionTest
{
    private ExecutionContext context;
    private JavaMethodAction actionPlugin;

    @BeforeEach
    public void setUp()
    {
        this.context = new ExecutionContext(new SessionData(Collections.emptyMap()));
        this.actionPlugin = new JavaMethodAction(this.context);
        DummyHelper.methodCalled = false;
        DummyHelper.lastParam = null;
        ToolHelper.methodCalled = false;
        ToolHelper.lastParam = null;
    }

    @Test
    public void testInvokeStaticMethodSuccess() throws Exception
    {
        System.setProperty("neodymium.ai.agent.methods.classes", DummyHelper.class.getName());
        
        final Action action = new Action("JAVA_METHOD", "dummyStaticMethod", List.of("hello"), "call method", "reason");
        this.actionPlugin.execute(action);

        assertTrue(DummyHelper.methodCalled);
        assertEquals("hello", DummyHelper.lastParam);
    }

    @Test
    public void testInvokeIntConversionSuccess() throws Exception
    {
        System.setProperty("neodymium.ai.agent.methods.classes", DummyHelper.class.getName());
        
        final Action action = new Action("JAVA_METHOD", "dummyIntMethod", List.of("42"), "call method", "reason");
        this.actionPlugin.execute(action);

        assertTrue(DummyHelper.methodCalled);
        assertEquals("42", DummyHelper.lastParam);
    }

    @Test
    public void testInvokeInstanceMethodSuccess() throws Exception
    {
        final TestInstance instance = new TestInstance();
        this.context.getTransientData().put("junit.testInstance", instance);

        final Action action = new Action("JAVA_METHOD", "instanceMethod", List.of("world"), "call method", "reason");
        this.actionPlugin.execute(action);

        assertTrue(instance.instanceCalled);
        assertEquals("world", instance.paramValue);
    }

    @Test
    public void testMethodNotFoundThrowsException()
    {
        System.setProperty("neodymium.ai.agent.methods.classes", DummyHelper.class.getName());
        
        final Action action = new Action("JAVA_METHOD", "nonExistentMethod", List.of("val"), "call method", "reason");
        assertThrows(IllegalArgumentException.class, () -> {
            this.actionPlugin.execute(action);
        });
    }

    @Test
    public void testInvokeToolStaticMethodSuccess() throws Exception
    {
        System.setProperty("neodymium.ai.agent.methods.classes", ToolHelper.class.getName());

        final Action action = new Action("JAVA_METHOD", "toolStaticMethod", List.of("tool-test"), "call tool method", "reason");
        this.actionPlugin.execute(action);

        assertTrue(ToolHelper.methodCalled);
        assertEquals("tool-test", ToolHelper.lastParam);
    }

    public static class ToolHelper
    {
        public static boolean methodCalled;
        public static String lastParam;

        @Tool(description = "A dummy tool method")
        public static void toolStaticMethod(final String param)
        {
            methodCalled = true;
            lastParam = param;
        }
    }

    @SuppressWarnings("removal")
    public static class DummyHelper
    {
        public static boolean methodCalled;
        public static String lastParam;

        @AiMethod("A dummy test method")
        public static void dummyStaticMethod(final String param)
        {
            methodCalled = true;
            lastParam = param;
        }

        @AiMethod("A dummy method with int parameter")
        public static void dummyIntMethod(final int param)
        {
            methodCalled = true;
            lastParam = String.valueOf(param);
        }
    }

    @SuppressWarnings("removal")
    public static class TestInstance
    {
        public boolean instanceCalled;
        public String paramValue;

        @AiMethod("A dummy instance method")
        public void instanceMethod(final String param)
        {
            this.instanceCalled = true;
            this.paramValue = param;
        }
    }
}
