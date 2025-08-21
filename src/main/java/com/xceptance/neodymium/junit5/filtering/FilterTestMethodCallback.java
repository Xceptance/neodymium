package com.xceptance.neodymium.junit5.filtering;

import com.xceptance.neodymium.util.Neodymium;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.List;
import java.util.regex.Pattern;

public class FilterTestMethodCallback implements ExecutionCondition
{
    private String testExecutionRegex = Neodymium.configuration().getTestNameFilter();

    private List<String> browserFilter = Neodymium.getBrowserFilter();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        String fullMethodName = context.getTestClass().get() + "#" + context.getDisplayName();

        // filter testname
        if (StringUtils.isNotEmpty(testExecutionRegex))
        {
            if (!Pattern.compile(testExecutionRegex)
                        .matcher(fullMethodName)
                        .find())
            {
                return ConditionEvaluationResult.disabled("not matching the test name filter " + testExecutionRegex);
            }
        }

        // filter browser
        if (!browserFilter.isEmpty())
        {
            if (browserFilter.stream().noneMatch(fullMethodName::contains))
            {
                return ConditionEvaluationResult.disabled("no browser contained in the browser filter");
            }
        }

        return ConditionEvaluationResult.enabled("");
    }
}
