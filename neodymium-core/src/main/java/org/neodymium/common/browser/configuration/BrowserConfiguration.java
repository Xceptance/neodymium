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
package org.neodymium.common.browser.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.MutableCapabilities;

/**
 * POJO class to hold browser configurations
 * 
 * @author m.kaufmann
 */
public class BrowserConfiguration
{
    private String browserTag;

    private String name;

    private MutableCapabilities capabilities;

    private HashMap<String, Object> gridProperties;

    private String testEnvironment;

    private int browserWidth;

    private int browserHeight;

    private boolean headless;

    private List<String> arguments;

    private List<String> driverArguments;

    private Map<String, Object> preferences;

    private String downloadDirectory;
    
    private boolean suppressPasswordLeakageWarning;

    /**
     * get config tag
     * 
     * @return config tag
     */
    public String getConfigTag()
    {
        return browserTag;
    }

    /**
     * set config tag for browser
     * 
     * @param configTag
     */
    protected void setConfigTag(String configTag)
    {
        this.browserTag = configTag;
    }

    /**
     * get browser name
     * 
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * set browser name
     * 
     * @param name
     */
    protected void setName(String name)
    {
        this.name = name;
    }

    /**
     * get browser capabilities
     * 
     * @return browser capabilities
     */
    public MutableCapabilities getCapabilities()
    {
        return capabilities;
    }

    /**
     * set browser capabilities
     * 
     * @param capabilities
     */
    protected void setCapabilities(MutableCapabilities capabilities)
    {
        this.capabilities = capabilities;
    }

    /**
     * get browser tag
     * 
     * @return browser tag
     */
    public String getBrowserTag()
    {
        return browserTag;
    }

    /**
     * get grid properties
     * 
     * @return grid properties
     */
    public HashMap<String, Object> getGridProperties()
    {
        return gridProperties;
    }

    /**
     * set grid properties
     * 
     * @param gridProperties
     */
    public void setGridProperties(HashMap<String, Object> gridProperties)
    {
        this.gridProperties = gridProperties;
    }

    /**
     * get test environment (grid)
     * 
     * @return
     */
    public String getTestEnvironment()
    {
        return testEnvironment;
    }

    /**
     * set test environment (grid)
     * 
     * @param testEnvironment
     */
    protected void setTestEnvironment(String testEnvironment)
    {
        this.testEnvironment = testEnvironment;
    }

    /**
     * get browser width
     * 
     * @return browser width
     */
    public int getBrowserWidth()
    {
        return browserWidth;
    }

    /**
     * set browser width
     * 
     * @param browserWidth
     */
    protected void setBrowserWidth(int browserWidth)
    {
        this.browserWidth = browserWidth;
    }

    /**
     * get browser height
     * 
     * @return browser height
     */
    public int getBrowserHeight()
    {
        return browserHeight;
    }

    /**
     * set browser height
     * 
     * @param browserHeight
     */
    protected void setBrowserHeight(int browserHeight)
    {
        this.browserHeight = browserHeight;
    }

    /**
     * should browser be headless
     * 
     * @return
     */
    public boolean isHeadless()
    {
        return headless;
    }

    /**
     * make browser headless/non-headless
     * 
     * @param headless
     */
    public void setHeadless(boolean headless)
    {
        this.headless = headless;
    }

    /**
     * get browser arguments
     * 
     * @return list of browser arguments
     */
    public List<String> getArguments()
    {
        return arguments;
    }

    /**
     * set browser arguments
     * 
     * @param arguments
     */
    public void setArguments(List<String> arguments)
    {
        this.arguments = arguments;
    }

    public List<String> getDriverArguments()
    {
        return driverArguments;
    }

    public void setDriverArguments(List<String> driverArguments)
    {
        this.driverArguments = driverArguments;
    }

    public Map<String, Object> getPreferences()
    {
        return preferences;
    }

    public void addPreference(String key, Object val)
    {
        if (this.preferences == null)
        {
            this.preferences = new HashMap<String, Object>();
        }
        this.preferences.put(key, val);
    }

    public String getDownloadDirectory()
    {
        return downloadDirectory;
    }

    public void setDownloadDirectory(String downloadDirectory)
    {
        this.downloadDirectory = downloadDirectory;
    }

    public boolean isSuppressPasswordLeakageWarning()
    {
        return suppressPasswordLeakageWarning;
    }

    public void setSuppressPasswordLeakageWarning(boolean suppressPasswordLeakageWarning)
    {
        this.suppressPasswordLeakageWarning = suppressPasswordLeakageWarning;
    }
}
