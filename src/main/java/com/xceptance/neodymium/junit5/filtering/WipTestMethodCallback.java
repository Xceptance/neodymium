package com.xceptance.neodymium.junit5.filtering;

import java.util.List;

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
        boolean wipMethod = List.of(context.getRequiredTestClass().getMethods()).stream()
                                .filter(method -> method.getAnnotation(NeodymiumTest.class) != null)
                                .anyMatch(method -> method.getAnnotation(WorkInProgress.class) != null);
        if (workInProgress && wipMethod && context.getRequiredTestMethod().getAnnotation(WorkInProgress.class) == null)
        {
            return ConditionEvaluationResult.disabled("not marked as WIP " + testExecutionRegex);
        }
        return ConditionEvaluationResult.enabled("");
    }
}
