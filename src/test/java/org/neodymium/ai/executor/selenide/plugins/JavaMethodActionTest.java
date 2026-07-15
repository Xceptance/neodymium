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
