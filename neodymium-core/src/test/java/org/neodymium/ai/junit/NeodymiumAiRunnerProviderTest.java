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
package org.neodymium.ai.junit;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.util.Neodymium;

/**
 * Regression test suite verifying declarative {@link AiProvider} annotation resolution
 * and system property propagation in {@link NeodymiumAiRunner} without package-name heuristics.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public class NeodymiumAiRunnerProviderTest
{
    @BeforeEach
    public void setUp()
    {
        Neodymium.clearThreadContext();
        System.clearProperty("neodymium.ai.global.provider");
    }

    @AfterEach
    public void tearDown()
    {
        Neodymium.clearThreadContext();
        System.clearProperty("neodymium.ai.global.provider");
    }

    @AiProvider("mock")
    @AiMode(ExecutionMode.LLM_ONLY)
    public static class ClassLevelAnnotatedTestClass
    {
        @Test
        @AiInlinePlaybook("name: test\nsteps:\n  - step: Click\n")
        public void testDefaultProvider()
        {
        }

        @Test
        @AiProvider("gemini")
        @AiInlinePlaybook("name: test\nsteps:\n  - step: Click\n")
        public void testMethodLevelOverride()
        {
        }
    }

    @AiProvider("mock")
    public static abstract class BaseAnnotatedClass
    {
    }

    @AiMode(ExecutionMode.LLM_ONLY)
    public static class SubclassInheritedTestClass extends BaseAnnotatedClass
    {
        @Test
        @AiInlinePlaybook("name: test\nsteps:\n  - step: Click\n")
        public void testInheritedProvider()
        {
        }
    }

    @AiMode(ExecutionMode.LLM_ONLY)
    public static class UnannotatedTestClass
    {
        @Test
        @AiInlinePlaybook("name: test\nsteps:\n  - step: Click\n")
        public void testNoAnnotation()
        {
        }
    }

    @Test
    public void testClassLevelAiProviderResolution() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = ClassLevelAnnotatedTestClass.class.getMethod("testDefaultProvider");
        final ExtensionContext context = createMockExtensionContext(ClassLevelAnnotatedTestClass.class, method);

        final List<TestTemplateInvocationContext> invocations = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final BeforeEachCallback beforeEach = findBeforeEachCallback(invocations.get(0));
        beforeEach.beforeEach(context);

        Assertions.assertEquals("mock", Neodymium.getData().get("neodymium.ai.global.provider"));
    }

    @Test
    public void testMethodLevelAiProviderOverride() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = ClassLevelAnnotatedTestClass.class.getMethod("testMethodLevelOverride");
        final ExtensionContext context = createMockExtensionContext(ClassLevelAnnotatedTestClass.class, method);

        final List<TestTemplateInvocationContext> invocations = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final BeforeEachCallback beforeEach = findBeforeEachCallback(invocations.get(0));
        beforeEach.beforeEach(context);

        Assertions.assertEquals("gemini", Neodymium.getData().get("neodymium.ai.global.provider"));
    }

    @Test
    public void testInheritedAiProviderResolution() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = SubclassInheritedTestClass.class.getMethod("testInheritedProvider");
        final ExtensionContext context = createMockExtensionContext(SubclassInheritedTestClass.class, method);

        final List<TestTemplateInvocationContext> invocations = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final BeforeEachCallback beforeEach = findBeforeEachCallback(invocations.get(0));
        beforeEach.beforeEach(context);

        Assertions.assertEquals("mock", Neodymium.getData().get("neodymium.ai.global.provider"));
    }

    @Test
    public void testSystemPropertyFallbackWhenUnannotated() throws Exception
    {
        System.setProperty("neodymium.ai.global.provider", "mock");

        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = UnannotatedTestClass.class.getMethod("testNoAnnotation");
        final ExtensionContext context = createMockExtensionContext(UnannotatedTestClass.class, method);

        final List<TestTemplateInvocationContext> invocations = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final BeforeEachCallback beforeEach = findBeforeEachCallback(invocations.get(0));
        beforeEach.beforeEach(context);

        Assertions.assertEquals("mock", Neodymium.getData().get("neodymium.ai.global.provider"));
    }

    @Test
    public void testNoProviderConfiguredWhenUnannotatedAndNoSysProp() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = UnannotatedTestClass.class.getMethod("testNoAnnotation");
        final ExtensionContext context = createMockExtensionContext(UnannotatedTestClass.class, method);

        final List<TestTemplateInvocationContext> invocations = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertFalse(invocations.isEmpty());

        final BeforeEachCallback beforeEach = findBeforeEachCallback(invocations.get(0));
        beforeEach.beforeEach(context);

        Assertions.assertNull(Neodymium.getData().get("neodymium.ai.global.provider"));
    }

    private BeforeEachCallback findBeforeEachCallback(final TestTemplateInvocationContext invocation)
    {
        final List<Extension> extensions = invocation.getAdditionalExtensions();
        return (BeforeEachCallback) extensions.stream()
            .filter(e -> e instanceof BeforeEachCallback)
            .findFirst()
            .orElseThrow(() -> new AssertionError("No BeforeEachCallback found"));
    }

    private ExtensionContext createMockExtensionContext(final Class<?> testClass, final Method testMethod) throws Exception
    {
        final Object instance = testClass != null && !java.lang.reflect.Modifier.isAbstract(testClass.getModifiers())
            ? testClass.getDeclaredConstructor().newInstance()
            : new Object();

        final Map<Object, Object> storeData = new HashMap<>();
        final ExtensionContext.Store mockStore = (ExtensionContext.Store) Proxy.newProxyInstance(
            ExtensionContext.Store.class.getClassLoader(),
            new Class<?>[] { ExtensionContext.Store.class },
            (proxy, m, args) -> {
                if ("get".equals(m.getName()) && args.length == 1)
                {
                    return storeData.get(args[0]);
                }
                if ("get".equals(m.getName()) && args.length == 2)
                {
                    return storeData.getOrDefault(args[0], null);
                }
                if ("put".equals(m.getName()))
                {
                    storeData.put(args[0], args[1]);
                    return null;
                }
                if ("remove".equals(m.getName()))
                {
                    return storeData.remove(args[0]);
                }
                return null;
            }
        );

        return (ExtensionContext) Proxy.newProxyInstance(
            ExtensionContext.class.getClassLoader(),
            new Class<?>[] { ExtensionContext.class },
            (proxy, m, args) -> {
                if ("getRequiredTestClass".equals(m.getName()))
                {
                    return testClass;
                }
                if ("getTestClass".equals(m.getName()))
                {
                    return Optional.ofNullable(testClass);
                }
                if ("getRequiredTestMethod".equals(m.getName()))
                {
                    return testMethod;
                }
                if ("getTestMethod".equals(m.getName()))
                {
                    return Optional.ofNullable(testMethod);
                }
                if ("getRequiredTestInstance".equals(m.getName()))
                {
                    return instance;
                }
                if ("getTestInstance".equals(m.getName()))
                {
                    return Optional.ofNullable(instance);
                }
                if ("getDisplayName".equals(m.getName()))
                {
                    return testMethod != null ? testMethod.getName() : "test";
                }
                if ("getStore".equals(m.getName()))
                {
                    return mockStore;
                }
                if (m.getReturnType().equals(Optional.class))
                {
                    return Optional.empty();
                }
                return null;
            }
        );
    }
}
