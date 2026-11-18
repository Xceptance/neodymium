/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.neodymium.junit5.filtering;

import org.neodymium.common.browser.BrowserData;
import org.neodymium.util.Neodymium;
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
