package org.neodymium.junit5.filtering;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.neodymium.common.WorkInProgress;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumAnnotationUtils;

public class WipTestMethodCallback implements ExecutionCondition
{
    private String testExecutionRegex = Neodymium.configuration().getTestNameFilter();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        boolean workInProgress = Neodymium.configuration().workInProgress();
        boolean wipMethod = Stream.of(context.getRequiredTestClass().getMethods())
                                  .filter(method -> NeodymiumAnnotationUtils.isAnnotationPresent(method, NeodymiumTest.class) || NeodymiumAnnotationUtils.isAnnotationPresent(method, org.neodymium.junit5.NeodymiumTestGenerator.class))
                                  .anyMatch(method -> NeodymiumAnnotationUtils.isAnnotationPresent(method, WorkInProgress.class));

        String testNameFilterMessage = testExecutionRegex != null ? "method or test matching filter: '" + testExecutionRegex + "' " : "";

        if (workInProgress && wipMethod && !NeodymiumAnnotationUtils.isAnnotationPresent(context.getRequiredTestMethod(), WorkInProgress.class))
        {
            return ConditionEvaluationResult.disabled(testNameFilterMessage + "not marked as WIP");
        }

        return ConditionEvaluationResult.enabled("");
    }
}
