package com.xceptance.neodymium.junit5.filtering;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.xceptance.neodymium.common.WorkInProgress;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

public class WipTestMethodCallback implements ExecutionCondition
{
    private String testExecutionRegex = Neodymium.configuration().getTestNameFilter();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        boolean workInProgress = Neodymium.configuration().workInProgress();
        boolean wipMethod = Stream.of(context.getRequiredTestClass().getMethods())
                                  .filter(method -> method.getAnnotation(NeodymiumTest.class) != null)
                                  .anyMatch(method -> method.getAnnotation(WorkInProgress.class) != null);

        String testNameFilterMessage = testExecutionRegex != null ? "method or test matching filter: '" + testExecutionRegex + "' " : "";

        if (workInProgress && wipMethod && context.getRequiredTestMethod().getAnnotation(WorkInProgress.class) == null)
        {
            return ConditionEvaluationResult.disabled(testNameFilterMessage + "not marked as WIP");
        }

        return ConditionEvaluationResult.enabled("");
    }
}
