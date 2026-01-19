package com.xceptance.neodymium.junit5.filtering;

import com.xceptance.neodymium.common.browser.BrowserData;
import com.xceptance.neodymium.util.Neodymium;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class FilterTestMethodCallback implements ExecutionCondition
{
    private String testExecutionRegex = Neodymium.configuration().getTestNameFilter();

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context)
    {
        String fullMethodName = context.getTestClass().get() + "#" + context.getDisplayName();
        final List<String> browserFilter = new ArrayList<String>();
        String browserDefinitionsProperty = System.getProperty(BrowserData.SYSTEM_PROPERTY_BROWSERDEFINITION, "");
        browserDefinitionsProperty = browserDefinitionsProperty.replaceAll("\\s", "");
        if (!StringUtils.isEmpty(browserDefinitionsProperty))
        {
            browserFilter.addAll(Arrays.asList(browserDefinitionsProperty.split(",")));
        }
        else if (!StringUtils.isEmpty(Neodymium.configuration().getBrowserFilter()))
        {
            browserFilter.addAll(Arrays.asList(Neodymium.configuration().getBrowserFilter().replaceAll("\\s", "").split(",")));
        }
        testExecutionRegex = browserFilter.isEmpty() ? Neodymium.configuration().getTestNameFilter() : ".*";

        // filter testname
        if (StringUtils.isNotEmpty(testExecutionRegex) || !browserFilter.isEmpty())
        {
            if (!Pattern.compile(testExecutionRegex)
                        .matcher(fullMethodName)
                        .find()
                || (!browserFilter.isEmpty() && browserFilter.stream().noneMatch(fullMethodName::contains)))
            {
                return ConditionEvaluationResult.disabled("not matching the test name filter " + testExecutionRegex);
            }
        }

        return ConditionEvaluationResult.enabled("");
    }
}
