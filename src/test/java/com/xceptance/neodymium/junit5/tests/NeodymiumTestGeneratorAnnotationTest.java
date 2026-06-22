package com.xceptance.neodymium.junit5.tests;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.ai.generator.AiPromptGeneratorAnnotation;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class NeodymiumTestGeneratorAnnotationTest extends AbstractNeodymiumTest
{
    @Test
    public void testGeneratorAnnotationExecution()
    {
        // Executes the test class annotated with @NeodymiumTestGenerator and ensures it completes successfully.
        // It validates that the NeodymiumRunner is properly hooked and execution runs successfully 
        // without bypassing the context.
        NeodymiumTestExecutionSummary summary = run(AiPromptGeneratorAnnotation.class);
        checkPass(summary, 1, 0);
    }
}
