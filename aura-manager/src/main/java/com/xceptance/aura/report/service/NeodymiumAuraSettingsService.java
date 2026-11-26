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
package com.xceptance.aura.report.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Service managing runner configuration settings, environment definitions, and browser profiles.
 *
 * @author Xceptance GmbH 2026
 */
@Service
public class NeodymiumAuraSettingsService
{
    private String selectedEnvironment = "Default";
    private String selectedBrowser = "Chrome";
    private boolean headless = true;
    private int threadCount = 4;

    public synchronized Map<String, Object> getSettings()
    {
        final Map<String, Object> settings = new HashMap<>();
        settings.put("selectedEnvironment", selectedEnvironment);
        settings.put("selectedBrowser", selectedBrowser);
        settings.put("headless", headless);
        settings.put("threadCount", threadCount);
        settings.put("availableEnvironments", List.of(
            "Default",
            "DEV - Local Sandbox",
            "STG - Staging",
            "PROD - Production"
        ));
        settings.put("availableBrowsers", List.of("Chrome", "Firefox", "Edge", "Safari", "HeadlessChrome"));
        return settings;
    }

    public synchronized void updateSettings(final String env, final String browser, final Boolean isHeadless, final Integer threads)
    {
        if (env != null && !env.isEmpty())
        {
            this.selectedEnvironment = env;
        }
        if (browser != null && !browser.isEmpty())
        {
            this.selectedBrowser = browser;
        }
        if (isHeadless != null)
        {
            this.headless = isHeadless;
        }
        if (threads != null && threads > 0)
        {
            this.threadCount = threads;
        }
    }
}
