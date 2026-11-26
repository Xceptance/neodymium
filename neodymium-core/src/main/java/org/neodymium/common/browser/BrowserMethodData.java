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
package org.neodymium.common.browser;

import java.lang.reflect.Method;
import java.util.List;

public class BrowserMethodData
{
    private String browserTag;

    private boolean keepBrowserOpen;

    private boolean keepBrowserOpenOnFailure;

    private boolean startBrowserOnSetUp;

    private boolean startBrowserOnCleanUp;

    private List<Method> afterMethodsWithTestBrowser;

    public BrowserMethodData(String browserTag, boolean keepBrowserOpen, boolean keepBrowserOpenOnFailure, boolean startBrowserOnSetUp,
        boolean startBrowserOnCleanUp, List<Method> afterMethodsWithTestBrowser)
    {
        super();
        this.browserTag = browserTag;
        this.keepBrowserOpen = keepBrowserOpen;
        this.keepBrowserOpenOnFailure = keepBrowserOpenOnFailure;
        this.startBrowserOnSetUp = startBrowserOnSetUp;
        this.startBrowserOnCleanUp = startBrowserOnCleanUp;
        this.afterMethodsWithTestBrowser = afterMethodsWithTestBrowser;
    }

    public String getBrowserTag()
    {
        return browserTag;
    }

    public boolean isKeepBrowserOpen()
    {
        return keepBrowserOpen;
    }

    public boolean isKeepBrowserOpenOnFailure()
    {
        return keepBrowserOpenOnFailure;
    }

    public boolean isStartBrowserOnSetUp()
    {
        return startBrowserOnSetUp;
    }

    public boolean isStartBrowserOnCleanUp()
    {
        return startBrowserOnCleanUp;
    }

    public List<Method> getAfterMethodsWithTestBrowser()
    {
        return afterMethodsWithTestBrowser;
    }

    public void setStartBrowserOnSetUp(boolean startBrowserOnSetUp)
    {
        this.startBrowserOnSetUp = startBrowserOnSetUp;
    }

    public void setStartBrowserOnCleanUp(boolean startBrowserOnCleanUp)
    {
        this.startBrowserOnCleanUp = startBrowserOnCleanUp;
    }

    public void setAfterMethodsWithTestBrowser(List<Method> afterMethodsWithTestBrowser)
    {
        this.afterMethodsWithTestBrowser = afterMethodsWithTestBrowser;
    }
}
