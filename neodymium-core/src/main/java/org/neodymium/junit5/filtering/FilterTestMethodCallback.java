/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
