/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.aura;

import java.io.File;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.util.Neodymium;
import org.openqa.selenium.os.ExecutableFinder;

/**
 * Validates the local availability of browser binaries, drivers, and configurations for Neodymium browser profiles.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class BrowserAvailabilityChecker
{
    private static final ExecutableFinder EXECUTABLE_FINDER = new ExecutableFinder();

    private BrowserAvailabilityChecker()
    {
    }

    /**
     * Holds the check outcome for a browser profile.
     */
    public static final class CheckResult
    {
        private final boolean available;
        private final String reason;

        public CheckResult(final boolean available, final String reason)
        {
            this.available = available;
            this.reason = reason;
        }

        public boolean isAvailable()
        {
            return this.available;
        }

        public String getReason()
        {
            return this.reason;
        }

        public static CheckResult available()
        {
            return new CheckResult(true, null);
        }

        public static CheckResult unavailable(final String reason)
        {
            return new CheckResult(false, reason);
        }
    }

    /**
     * Evaluates whether a given browser configuration is valid and runnable in the current runtime environment.
     *
     * @param tag the profile configuration key
     * @param config the parsed BrowserConfiguration
     * @param browserCategory the classified browser category (chrome, firefox, edge, safari, mobile, other)
     * @return CheckResult indicating availability and reason if unavailable
     */
    public static CheckResult check(final String tag, final BrowserConfiguration config, final String browserCategory)
    {
        if (config == null)
        {
            return CheckResult.unavailable("Browser profile configuration is missing.");
        }

        final String rawBrowser = config.getCapabilities() != null && config.getCapabilities().getBrowserName() != null
                ? config.getCapabilities().getBrowserName().toLowerCase()
                : "";

        // Check if configuration is missing browser specification
        if (StringUtils.isBlank(rawBrowser) && !"mobile".equalsIgnoreCase(browserCategory))
        {
            return CheckResult.unavailable("Missing or invalid 'browser' property in configuration.");
        }

        final String osName = System.getProperty("os.name", "").toLowerCase();

        // 1. Safari check (macOS only)
        if ("safari".equalsIgnoreCase(browserCategory) || rawBrowser.contains("safari"))
        {
            if (!osName.contains("mac"))
            {
                return CheckResult.unavailable("Apple Safari is only available on macOS systems (current OS: " + System.getProperty("os.name") + ").");
            }
            if (!new File("/Applications/Safari.app").exists() && EXECUTABLE_FINDER.find("safaridriver") == null && !new File("/usr/bin/safaridriver").exists())
            {
                return CheckResult.unavailable("Apple Safari browser or safaridriver was not found on this macOS system.");
            }
            return CheckResult.available();
        }

        // 2. Internet Explorer check (Windows only)
        if (rawBrowser.contains("internetexplorer") || rawBrowser.contains("ie"))
        {
            if (!osName.contains("win"))
            {
                return CheckResult.unavailable("Internet Explorer is only available on Windows systems.");
            }
        }

        // 3. Mobile / Tablet Emulation check (Emulation requires Chrome)
        if ("mobile".equalsIgnoreCase(browserCategory))
        {
            final CheckResult chromeCheck = checkChromeInstalled();
            if (!chromeCheck.isAvailable())
            {
                return CheckResult.unavailable("Google Chrome is required for mobile device emulation, but was not found.");
            }
            return CheckResult.available();
        }

        // 4. Chrome check
        if ("chrome".equalsIgnoreCase(browserCategory) || rawBrowser.contains("chrome"))
        {
            return checkChromeInstalled();
        }

        // 5. Firefox check
        if ("firefox".equalsIgnoreCase(browserCategory) || rawBrowser.contains("firefox"))
        {
            return checkFirefoxInstalled();
        }

        // 6. Edge check
        if ("edge".equalsIgnoreCase(browserCategory) || rawBrowser.contains("edge"))
        {
            return checkEdgeInstalled();
        }

        return CheckResult.available();
    }

    private static CheckResult checkChromeInstalled()
    {
        // Custom configured binary
        final String customPath = Neodymium.configuration().getChromeBrowserPath();
        if (StringUtils.isNotBlank(customPath))
        {
            if (!new File(customPath).exists())
            {
                return CheckResult.unavailable("Configured Google Chrome binary not found: " + customPath);
            }
            return checkCustomDriver(Neodymium.configuration().getChromeDriverPath(), "chromedriver");
        }

        // PATH check
        if (findInPathOrLocations(List.of("google-chrome", "chrome", "chromium", "chromium-browser", "google-chrome-stable"),
                List.of("/usr/bin/google-chrome", "/usr/bin/chromium", "/usr/bin/chromium-browser",
                        "/usr/bin/google-chrome-stable", "/snap/bin/chromium",
                        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
                        "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                        "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe")))
        {
            return checkCustomDriver(Neodymium.configuration().getChromeDriverPath(), "chromedriver");
        }

        return CheckResult.unavailable("Google Chrome is not installed or not found in system PATH.");
    }

    private static CheckResult checkFirefoxInstalled()
    {
        // Custom configured binary
        final String customPath = Neodymium.configuration().getFirefoxBrowserPath();
        if (StringUtils.isNotBlank(customPath))
        {
            if (!new File(customPath).exists())
            {
                return CheckResult.unavailable("Configured Mozilla Firefox binary not found: " + customPath);
            }
            return checkCustomDriver(Neodymium.configuration().getFirefoxDriverPath(), "geckodriver");
        }

        // PATH check
        if (findInPathOrLocations(List.of("firefox"),
                List.of("/usr/bin/firefox", "/snap/bin/firefox", "/usr/lib/firefox/firefox",
                        "/Applications/Firefox.app/Contents/MacOS/firefox",
                        "C:\\Program Files\\Mozilla Firefox\\firefox.exe",
                        "C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe")))
        {
            return checkCustomDriver(Neodymium.configuration().getFirefoxDriverPath(), "geckodriver");
        }

        return CheckResult.unavailable("Mozilla Firefox is not installed or not found in system PATH.");
    }

    private static CheckResult checkEdgeInstalled()
    {
        if (findInPathOrLocations(List.of("msedge", "microsoft-edge", "microsoft-edge-stable"),
                List.of("/usr/bin/microsoft-edge", "/usr/bin/microsoft-edge-stable",
                        "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
                        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                        "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe")))
        {
            return checkCustomDriver(Neodymium.configuration().getEdgeDriverPath(), "msedgedriver");
        }

        return CheckResult.unavailable("Microsoft Edge is not installed or not found in system PATH.");
    }

    private static CheckResult checkCustomDriver(final String driverPath, final String driverName)
    {
        if (StringUtils.isNotBlank(driverPath))
        {
            final File f = new File(driverPath);
            if (!f.exists())
            {
                return CheckResult.unavailable("Configured " + driverName + " executable not found: " + driverPath);
            }
        }
        return CheckResult.available();
    }

    private static boolean findInPathOrLocations(final List<String> binaryNames, final List<String> commonPaths)
    {
        for (final String name : binaryNames)
        {
            if (EXECUTABLE_FINDER.find(name) != null)
            {
                return true;
            }
        }
        for (final String path : commonPaths)
        {
            if (new File(path).exists())
            {
                return true;
            }
        }
        return false;
    }
}
