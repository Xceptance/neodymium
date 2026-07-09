/*
 * Copyright (c) 2017-2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.JavaScriptUtils;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.PropertiesUtil;

/**
 * Backend-agnostic hook handling element/page navigation updates.
 * Checks URL inclusion/exclusion rules, logs URL changes to Allure,
 * and handles popup blocker injection.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class BrowserNavigationHook
{
    private static final Map<Thread, String> LAST_URL = Collections.synchronizedMap(new WeakHashMap<>());

    public static final String URL_CHANGED_STEP_MESSAGE = "URL changed";

    private final List<String> includeList;

    private final List<String> excludeList;

    private final Map<String, String> popupMap;

    /**
     * Initializes the navigation hook from the current Neodymium configuration.
     */
    public BrowserNavigationHook()
    {
        final List<String> inc = new LinkedList<>();
        if (!Neodymium.configuration().getIncludeList().isEmpty())
        {
            inc.addAll(Arrays.asList(Neodymium.configuration().getIncludeList().split("\\s+")));
        }
        this.includeList = Collections.unmodifiableList(inc);

        final List<String> exc = new ArrayList<>();
        if (!Neodymium.configuration().getExcludeList().isEmpty())
        {
            exc.addAll(Arrays.asList(Neodymium.configuration().getExcludeList().split("\\s+")));
            exc.removeAll(inc);
        }
        this.excludeList = Collections.unmodifiableList(exc);

        this.popupMap = PropertiesUtil.getPropertiesMapForCustomIdentifier("neodymium.popup");
    }

    /**
     * Clears the cached last url for the current thread.
     */
    public static void clearLastUrl()
    {
        LAST_URL.remove(Thread.currentThread());
    }

    /**
     * Handles navigation events by executing page inclusion/exclusion checks,
     * injecting popup blockers, and reporting URL changes.
     *
     * @param currentUrl the new browser URL
     */
    public void onNavigation(final String currentUrl)
    {
        if (currentUrl == null || "data:,".equals(currentUrl))
        {
            return;
        }

        final String lastUrl = LAST_URL.get(Thread.currentThread());
        if (currentUrl.equals(lastUrl))
        {
            return;
        }

        if (Neodymium.configuration().enableStepLinks())
        {
            AllureAddons.addLinkToReport(URL_CHANGED_STEP_MESSAGE, currentUrl);
        }

        if (popupMap != null && !popupMap.isEmpty())
        {
            for (final String popup : popupMap.values())
            {
                JavaScriptUtils.injectJavascriptPopupBlocker(popup);
            }
        }

        if (includeList != null && !includeList.isEmpty())
        {
            assertTrue(includeList.stream().anyMatch(s -> Pattern.compile(s).matcher(currentUrl).find()),
                       "Opened Link was outside permitted URLs: " + currentUrl + " did not match any of the include list: " + includeList);
        }

        if (excludeList != null && !excludeList.isEmpty())
        {
            assertTrue(excludeList.stream().noneMatch(s -> Pattern.compile(s).matcher(currentUrl).find()),
                       "Opened Link was to forbidden site: " + currentUrl);
        }

        LAST_URL.put(Thread.currentThread(), currentUrl);
    }
}
