package com.xceptance.neodymium.junit5.filtering;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.xceptance.neodymium.util.Neodymium;

public class FilterTestMethodCallback implements ExecutionCondition
{
    private String testExecutionRegex = Neodymium.configuration().getTestNameFilter();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        if (StringUtils.isNotEmpty(testExecutionRegex))
        {
            String fullMethodName = context.getTestClass().get() + "#" + context.getDisplayName();
            if (!Pattern.compile(testExecutionRegex)
                        .matcher(fullMethodName)
                        .find())
            {
                return ConditionEvaluationResult.disabled("not matching the test name filter " + testExecutionRegex);
            }
        }
        return ConditionEvaluationResult.enabled("");

    }
}
