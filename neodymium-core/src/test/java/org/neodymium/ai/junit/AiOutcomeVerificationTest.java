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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * Unit tests for {@link AiOutcomeVerification} annotation parsing, reflection handling,
 * and {@link NeodymiumAiRunner} matrix invocation context integration.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AiOutcomeVerificationTest
{
    @AiOutcomeVerification
    private static class ClassWithDefaultOutcome
    {
    }

    @AiOutcomeVerification({false, true})
    private static class ClassWithOutcomeMatrix
    {
        @AiOutcomeVerification({true})
        public void methodOverride()
        {
        }

        public void inheritClass()
        {
        }
    }

    /**
     * Sample test class providing test methods for runner matrix verification.
     */
    public static class RunnerMatrixTestClass
    {
        @AiInlinePlaybook("name: outcome-default\nsteps:\n  - instruction: Step 1\n")
        @AiOutcomeVerification
        public void testDefaultOutcome()
        {
        }

        @AiInlinePlaybook("name: outcome-single\nsteps:\n  - instruction: Step 1\n")
        @AiOutcomeVerification(false)
        public void testSingleOutcomeFalse()
        {
        }

        @AiInlinePlaybook("name: outcome-matrix\nsteps:\n  - instruction: Step 1\n")
        @AiOutcomeVerification({false, true})
        public void testMatrixOutcome()
        {
        }

        @AiInlinePlaybook("name: outcome-none\nsteps:\n  - instruction: Step 1\n")
        public void testNoAnnotationOutcome()
        {
        }
    }

    @AiOutcomeVerification(failOnError = false)
    public static class ClassLevelFailOnErrorOnlyTestClass
    {
        @AiInlinePlaybook("name: outcome-none\nsteps:\n  - instruction: Step 1\n")
        public void testNoMethodAnnotation()
        {
        }
    }

    @Test
    @DisplayName("AiOutcomeVerification default value is empty array and failOnError is false")
    public void testAiOutcomeVerificationDefaultValue()
    {
        final AiOutcomeVerification annot = ClassWithDefaultOutcome.class.getAnnotation(AiOutcomeVerification.class);
        Assertions.assertNotNull(annot);
        Assertions.assertArrayEquals(new boolean[]{}, annot.value());
        Assertions.assertFalse(annot.failOnError());
        Assertions.assertFalse(annot.onError());
    }

    @Test
    @DisplayName("AiOutcomeVerification supports multi-boolean matrix array")
    public void testAiOutcomeVerificationMatrix()
    {
        final AiOutcomeVerification annot = ClassWithOutcomeMatrix.class.getAnnotation(AiOutcomeVerification.class);
        Assertions.assertNotNull(annot);
        Assertions.assertArrayEquals(new boolean[]{false, true}, annot.value());
    }

    @Test
    @DisplayName("AiOutcomeVerification method-level annotation overrides class-level annotation")
    public void testAiOutcomeVerificationMethodOverride() throws NoSuchMethodException
    {
        final Method overrideMethod = ClassWithOutcomeMatrix.class.getMethod("methodOverride");
        final AiOutcomeVerification annot = overrideMethod.getAnnotation(AiOutcomeVerification.class);
        Assertions.assertNotNull(annot);
        Assertions.assertArrayEquals(new boolean[]{true}, annot.value());

        final Method inheritMethod = ClassWithOutcomeMatrix.class.getMethod("inheritClass");
        Assertions.assertNull(inheritMethod.getAnnotation(AiOutcomeVerification.class));
    }

    @Test
    @DisplayName("NeodymiumAiRunner generates single [Outcome: ON] context when annotated with bare @AiOutcomeVerification")
    public void testRunnerGeneratesSingleOutcomeOnForBareAnnotation() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = RunnerMatrixTestClass.class.getMethod("testDefaultOutcome");
        final ExtensionContext context = createMockExtensionContext(RunnerMatrixTestClass.class, method);

        final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertEquals(1, contexts.size());
        Assertions.assertTrue(contexts.get(0).getDisplayName(1).contains("[Outcome: ON]"));
    }

    @Test
    @DisplayName("NeodymiumAiRunner generates single [Outcome: OFF] context when annotated with @AiOutcomeVerification(false)")
    public void testRunnerGeneratesSingleOutcomeOffForFalseAnnotation() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = RunnerMatrixTestClass.class.getMethod("testSingleOutcomeFalse");
        final ExtensionContext context = createMockExtensionContext(RunnerMatrixTestClass.class, method);

        final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertEquals(1, contexts.size());
        Assertions.assertTrue(contexts.get(0).getDisplayName(1).contains("[Outcome: OFF]"));
    }

    @Test
    @DisplayName("NeodymiumAiRunner generates two matrix contexts when annotated with @AiOutcomeVerification({false, true})")
    public void testRunnerGeneratesMatrixContextsForArrayAnnotation() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = RunnerMatrixTestClass.class.getMethod("testMatrixOutcome");
        final ExtensionContext context = createMockExtensionContext(RunnerMatrixTestClass.class, method);

        final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertEquals(2, contexts.size());
        Assertions.assertTrue(contexts.get(0).getDisplayName(1).contains("[Outcome: OFF]"));
        Assertions.assertTrue(contexts.get(1).getDisplayName(2).contains("[Outcome: ON]"));
    }

    @Test
    @DisplayName("NeodymiumAiRunner does not append [Outcome: ...] label when no annotation is present")
    public void testRunnerNoAnnotationOmitsOutcomeLabel() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = RunnerMatrixTestClass.class.getMethod("testNoAnnotationOutcome");
        final ExtensionContext context = createMockExtensionContext(RunnerMatrixTestClass.class, method);

        final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertEquals(1, contexts.size());
        Assertions.assertFalse(contexts.get(0).getDisplayName(1).contains("[Outcome:"));
    }

    @Test
    @DisplayName("NeodymiumAiRunner does not activate outcome verification when class only defines failOnError")
    public void testRunnerClassLevelFailOnErrorDoesNotActivateOutcome() throws Exception
    {
        final NeodymiumAiRunner runner = new NeodymiumAiRunner();
        final Method method = ClassLevelFailOnErrorOnlyTestClass.class.getMethod("testNoMethodAnnotation");
        final ExtensionContext context = createMockExtensionContext(ClassLevelFailOnErrorOnlyTestClass.class, method);

        final List<TestTemplateInvocationContext> contexts = runner.provideTestTemplateInvocationContexts(context).toList();
        Assertions.assertEquals(1, contexts.size());
        Assertions.assertFalse(contexts.get(0).getDisplayName(1).contains("[Outcome:"));
    }

    private static ExtensionContext createMockExtensionContext(final Class<?> testClass, final Method method)
    {
        final Map<Object, Object> storeMap = new HashMap<>();
        final ExtensionContext.Store mockStore = (ExtensionContext.Store) Proxy.newProxyInstance(
            ExtensionContext.Store.class.getClassLoader(),
            new Class<?>[]{ExtensionContext.Store.class},
            (sp, sm, sargs) ->
            {
                if ("put".equals(sm.getName()))
                {
                    storeMap.put(sargs[0], sargs[1]);
                    return null;
                }
                if ("get".equals(sm.getName()))
                {
                    return storeMap.get(sargs[0]);
                }
                return null;
            }
        );

        return (ExtensionContext) Proxy.newProxyInstance(
            ExtensionContext.class.getClassLoader(),
            new Class<?>[]{ExtensionContext.class},
            (proxy, m, args) ->
            {
                if ("getTestClass".equals(m.getName()))
                {
                    return Optional.ofNullable(testClass);
                }
                if ("getRequiredTestClass".equals(m.getName()))
                {
                    return testClass;
                }
                if ("getTestMethod".equals(m.getName()))
                {
                    return Optional.ofNullable(method);
                }
                if ("getRequiredTestMethod".equals(m.getName()))
                {
                    return method;
                }
                if ("getRequiredTestInstance".equals(m.getName()))
                {
                    return testClass.getDeclaredConstructor().newInstance();
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
