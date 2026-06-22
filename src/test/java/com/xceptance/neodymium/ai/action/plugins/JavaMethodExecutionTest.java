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
package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;

/**
 * Runtime execution tests for the {@link JavaMethodAction} plugin.
 * Verifies reflection resolution (Stage 1 & Stage 2), parameter parsing,
 * and exception wrapping behavior.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class JavaMethodExecutionTest
{
    private boolean methodCalledNoParam = false;
    private String passedParam = null;

    /**
     * Public method to be invoked on test instance via Stage 1 reflection (no params).
     */
    @AiMethod("test method no param")
    public void myTestInstanceMethod()
    {
        this.methodCalledNoParam = true;
    }

    /**
     * Public method to be invoked on test instance via Stage 1 reflection (with String param).
     * 
     * @param value the string parameter
     */
    @AiMethod("test method with param")
    public void myTestInstanceMethodWithParam(final String value)
    {
        this.passedParam = value;
    }

    /**
     * Public method that throws a checked or unchecked exception to test wrapping.
     */
    @AiMethod("throwing method")
    public void myThrowingMethod()
    {
        throw new IllegalArgumentException("Target exception message");
    }

    @Test
    public void testExecuteTestInstanceMethodNoParam()
    {
        final JavaMethodAction javaMethodAction = new JavaMethodAction();
        final Action action = new Action("JAVA_METHOD", "myTestInstanceMethod", "Call myTestInstanceMethod");

        javaMethodAction.execute(action, this, null);

        Assertions.assertTrue(this.methodCalledNoParam);
    }

    @Test
    public void testExecuteTestInstanceMethodWithParam()
    {
        final JavaMethodAction javaMethodAction = new JavaMethodAction();
        final Action action = new Action("JAVA_METHOD", "myTestInstanceMethodWithParam", List.of("test-value"), "Call with param");

        javaMethodAction.execute(action, this, null);

        Assertions.assertEquals("test-value", this.passedParam);
    }

    @Test
    public void testExecuteUtilityMethodStage2()
    {
        final JavaMethodAction javaMethodAction = new JavaMethodAction();
        // com.xceptance.neodymium.ai.util.AiAssertions has public static void assertPriceGreaterThanZero(String)
        // We will call assertPriceGreaterThanZero with "$10.00" (should pass)
        final Action action = new Action("JAVA_METHOD", "assertPriceGreaterThanZero", List.of("$10.00"), "Call price assertion");

        javaMethodAction.execute(action, this, null);
    }

    @Test
    public void testExecuteUtilityMethodStage2Failure()
    {
        final JavaMethodAction javaMethodAction = new JavaMethodAction();
        // Calling assertPriceGreaterThanZero with "$0.00" which should fail and throw an AssertionError
        final Action action = new Action("JAVA_METHOD", "assertPriceGreaterThanZero", List.of("$0.00"), "Call price assertion failing");

        final ActionExecutionException exception = Assertions.assertThrows(ActionExecutionException.class, () ->
        {
            javaMethodAction.execute(action, this, null);
        });

        Assertions.assertTrue(exception.getMessage().contains("assertPriceGreaterThanZero"));
        Assertions.assertTrue(exception.getMessage().contains("price > 0"));
    }

    @Test
    public void testExecuteExceptionWrapping()
    {
        final JavaMethodAction javaMethodAction = new JavaMethodAction();
        final Action action = new Action("JAVA_METHOD", "myThrowingMethod", "Call myThrowingMethod");

        final ActionExecutionException exception = Assertions.assertThrows(ActionExecutionException.class, () ->
        {
            javaMethodAction.execute(action, this, null);
        });

        Assertions.assertTrue(exception.getMessage().contains("myThrowingMethod"));
        Assertions.assertTrue(exception.getMessage().contains("Target exception message"));
        Assertions.assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    public void testExecuteMethodNotFound()
    {
        final JavaMethodAction javaMethodAction = new JavaMethodAction();
        final Action action = new Action("JAVA_METHOD", "nonExistentMethod", "Call nonExistentMethod");

        final ActionExecutionException exception = Assertions.assertThrows(ActionExecutionException.class, () ->
        {
            javaMethodAction.execute(action, this, null);
        });

        Assertions.assertTrue(exception.getMessage().contains("no public method"));
        Assertions.assertTrue(exception.getMessage().contains("nonExistentMethod"));
    }

    @Test
    public void testExecuteWithBlankTarget()
    {
        final JavaMethodAction javaMethodAction = new JavaMethodAction();
        final Action action = new Action("JAVA_METHOD", "", "Blank target");

        final ActionExecutionException exception = Assertions.assertThrows(ActionExecutionException.class, () ->
        {
            javaMethodAction.execute(action, this, null);
        });

        Assertions.assertTrue(exception.getMessage().contains("requires a 'target'"));
    }

    @Test
    public void testExecuteWithInvalidDottedTarget()
    {
        final JavaMethodAction javaMethodAction = new JavaMethodAction();
        final Action action = new Action("JAVA_METHOD", "SomeClass.someMethod", "Dotted target");

        final ActionExecutionException exception = Assertions.assertThrows(ActionExecutionException.class, () ->
        {
            javaMethodAction.execute(action, this, null);
        });

        Assertions.assertTrue(exception.getMessage().contains("must be a simple method name"));
    }
}
