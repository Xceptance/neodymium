/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package org.neodymium.ai.executor.selenide;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.ActionDefinition;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.executor.selenide.plugins.BackAction;
import org.neodymium.ai.executor.selenide.plugins.BrowserActionPlugin;
import org.neodymium.ai.executor.selenide.plugins.ClearAction;
import org.neodymium.ai.executor.selenide.plugins.ClearCookiesAction;
import org.neodymium.ai.executor.selenide.plugins.ClickAction;
import org.neodymium.ai.executor.selenide.plugins.ForwardAction;
import org.neodymium.ai.executor.selenide.plugins.HoverAction;
import org.neodymium.ai.executor.selenide.plugins.KeyPressAction;
import org.neodymium.ai.executor.selenide.plugins.NavigateAction;
import org.neodymium.ai.executor.selenide.plugins.RefreshAction;
import org.neodymium.ai.executor.selenide.plugins.ScrollAction;
import org.neodymium.ai.executor.selenide.plugins.SelectAction;
import org.neodymium.ai.executor.selenide.plugins.SwitchWindowAction;
import org.neodymium.ai.executor.selenide.plugins.TypeAction;
import org.neodymium.ai.executor.selenide.plugins.WaitAction;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.HasAuthentication;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.WebDriver;

/**
 * Concrete target executor driving a web browser via Selenide and WebDriver.
 * Implements basic auth interception and screenshot capturing.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SelenideTargetExecutor implements TargetExecutor
{
    /**
     * Map storing registered browser action plugins.
     */
    private final Map<String, BrowserActionPlugin> plugins = new ConcurrentHashMap<>();

    /**
     * Constructs a SelenideTargetExecutor and registers default action plugins.
     */
    public SelenideTargetExecutor()
    {
        this.plugins.put("NAVIGATE", new NavigateAction());
        this.plugins.put("CLICK", new ClickAction());
        this.plugins.put("TYPE", new TypeAction());
        this.plugins.put("CLEAR", new ClearAction());
        this.plugins.put("HOVER", new HoverAction());
        this.plugins.put("BACK", new BackAction());
        this.plugins.put("FORWARD", new ForwardAction());
        this.plugins.put("REFRESH", new RefreshAction());
        this.plugins.put("CLEAR_COOKIES", new ClearCookiesAction());
        this.plugins.put("SCROLL", new ScrollAction());
        this.plugins.put("SELECT", new SelectAction());
        this.plugins.put("WAIT", new WaitAction());
        this.plugins.put("KEY_PRESS", new KeyPressAction());
        this.plugins.put("SWITCH_WINDOW", new SwitchWindowAction());
    }

    /**
     * Captures browser DOM HTML page source, captures a screenshot, and calculates
     * a layout hash of the page source.
     *
     * @return the captured BrowserSutState
     * @throws IOException if state capture fails
     */
    @Override
    public SutState captureState() throws IOException
    {
        if (!WebDriverRunner.hasWebDriverStarted())
        {
            return new BrowserSutState("<html><body>Not Started</body></html>", Collections.emptyList(), "not-started");
        }

        final String html = WebDriverRunner.getWebDriver().getPageSource();
        final String hash = calculateHtmlHash(html);

        final List<SutAttachment> attachments = new ArrayList<>();
        final String screenshotFile = Selenide.screenshot("capture_" + System.currentTimeMillis());
        if (screenshotFile != null)
        {
            attachments.add(new SutAttachment("image/png", screenshotFile, null));
        }

        return new BrowserSutState(html, attachments, hash);
    }

    /**
     * Executes common web browser actions (NAVIGATE, CLICK, TYPE, CLEAR, HOVER)
     * using Selenide element finders.
     *
     * @param action the executable action instance
     * @throws IOException if selector matches no elements or execution fails
     */
    @Override
    public void execute(final Action action) throws IOException
    {
        if (action == null)
        {
            return;
        }

        final String type = action.getType();
        final BrowserActionPlugin plugin = this.plugins.get(type.toUpperCase());
        if (plugin == null)
        {
            throw new IOException("Unsupported browser action type: " + type);
        }

        try
        {
            plugin.execute(action);
        }
        catch (final Exception e)
        {
            throw new IOException("Failed executing action: " + action.getDescription(), e);
        }
    }

    /**
     * Registers username/password credentials for basic auth challenge interception
     * using Selenium 4's HasAuthentication context.
     *
     * @param username the authentication username
     * @param password the authentication password
     */
    public void registerBasicAuth(final String username, final String password)
    {
        if (WebDriverRunner.hasWebDriverStarted() && username != null && password != null)
        {
            final WebDriver driver = WebDriverRunner.getWebDriver();
            if (driver instanceof HasAuthentication hasAuth)
            {
                hasAuth.register(UsernameAndPassword.of(username, password));
            }
        }
    }

    /**
     * Retrieves the set of supported browser actions signatures.
     *
     * @return the set of supported ActionDefinitions
     */
    @Override
    public Set<ActionDefinition> getSupportedActions()
    {
        return Set.of(
            new ActionDefinition("NAVIGATE", "Navigate to target URL", Collections.emptyMap()),
            new ActionDefinition("CLICK", "Click element matching selector", Collections.emptyMap()),
            new ActionDefinition("TYPE", "Type text into element matching selector", Collections.emptyMap()),
            new ActionDefinition("CLEAR", "Clear text from element matching selector", Collections.emptyMap()),
            new ActionDefinition("HOVER", "Hover mouse over element matching selector", Collections.emptyMap()),
            new ActionDefinition("BACK", "Navigate back in history", Collections.emptyMap()),
            new ActionDefinition("FORWARD", "Navigate forward in history", Collections.emptyMap()),
            new ActionDefinition("REFRESH", "Refresh page", Collections.emptyMap()),
            new ActionDefinition("CLEAR_COOKIES", "Clear all browser cookies", Collections.emptyMap()),
            new ActionDefinition("SCROLL", "Scroll element or page", Collections.emptyMap()),
            new ActionDefinition("SELECT", "Select option in dropdown", Collections.emptyMap()),
            new ActionDefinition("WAIT", "Wait for element state or pause", Collections.emptyMap()),
            new ActionDefinition("KEY_PRESS", "Send key press events", Collections.emptyMap()),
            new ActionDefinition("SWITCH_WINDOW", "Switch WebDriver focus to another window or tab", Collections.emptyMap())
        );
    }

    /**
     * Helper to compute layout hash of page DOM HTML.
     */
    private String calculateHtmlHash(final String html)
    {
        try
        {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            final byte[] hash = digest.digest(html.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();
            for (final byte b : hash)
            {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (final NoSuchAlgorithmException e)
        {
            return String.valueOf(html.hashCode());
        }
    }
}
