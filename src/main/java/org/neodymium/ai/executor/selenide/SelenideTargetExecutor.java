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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.executor.ActionDefinition;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.executor.selenide.plugins.BackAction;
import org.neodymium.ai.executor.selenide.plugins.AssertAction;
import org.neodymium.ai.executor.selenide.plugins.BrowserActionPlugin;
import org.neodymium.ai.executor.selenide.plugins.JavaMethodAction;
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
import org.neodymium.ai.executor.selenide.plugins.CheckAction;
import org.neodymium.ai.executor.selenide.plugins.StoreAction;
import org.neodymium.ai.executor.selenide.plugins.BranchAction;
import org.neodymium.ai.executor.selenide.plugins.IncludeAction;
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
        this.plugins.put("ASSERT", new AssertAction());
        this.plugins.put("CHECK", new CheckAction());
        this.plugins.put("SPLIT", action -> {});
        this.plugins.put("NONE", action -> {});
    }

    private org.neodymium.ai.pipeline.ExecutionContext context;

    /**
     * Sets the execution context and registers context-dependent action plugins.
     *
     * @param context the execution context
     */
    public void setExecutionContext(final org.neodymium.ai.pipeline.ExecutionContext context)
    {
        this.context = context;
        this.plugins.put("JAVA_METHOD", new JavaMethodAction(context));
        this.plugins.put("STORE", new StoreAction(context));
        this.plugins.put("BRANCH", new BranchAction(context));
        this.plugins.put("INCLUDE", new IncludeAction(context));
    }

    /**
     * Captures browser DOM HTML page source, captures a screenshot, and calculates
     * a layout hash of the page source.
     *
     * @return the captured BrowserSutState
     * @throws IOException if state capture fails
     */
    @Override
    public SutState captureState(final ContextLevel level) throws IOException
    {
        return captureState(level, level != null && level.isFullPageScreenshot());
    }

    @Override
    public SutState captureState(final ContextLevel level, final boolean isFullPage) throws IOException
    {
        if (!WebDriverRunner.hasWebDriverStarted())
        {
            org.slf4j.LoggerFactory.getLogger(SelenideTargetExecutor.class).warn("⚠️ Browser/WebDriver has not started yet. No active page loaded.");
            return new BrowserSutState("⚠️ Browser/WebDriver is not started yet. No active DOM available.", Collections.emptyList(), "not-started");
        }

        final ContextLevel activeLevel = level != null ? level : ContextLevel.STANDARD;
        final String html = new PageAnalyzer(WebDriverRunner.getWebDriver()).captureSimplifiedDom(activeLevel);
        final String hash = calculateHtmlHash(html);

        final List<SutAttachment> attachments = new ArrayList<>();
        if (activeLevel.includesScreenshot())
        {
            String base64Data = null;
            try
            {
                final boolean forceFullPage = isFullPage;
                base64Data = new PageAnalyzer(WebDriverRunner.getWebDriver())
                        .captureScreenshot("capture_" + System.currentTimeMillis(), activeLevel, forceFullPage, null);
            }
            catch (final Exception e)
            {
                org.slf4j.LoggerFactory.getLogger(SelenideTargetExecutor.class)
                        .debug("PageAnalyzer captureScreenshot failed, falling back to Selenide: {}", e.getMessage());
            }

            if (base64Data != null)
            {
                attachments.add(new SutAttachment("image/png", "screenshot", base64Data));
            }
            else
            {
                final String screenshotFile = Selenide.screenshot("capture_" + System.currentTimeMillis());
                if (screenshotFile != null)
                {
                    String mediaType = "image/png";
                    try
                    {
                        final java.nio.file.Path path;
                        if (screenshotFile.startsWith("file:"))
                        {
                            path = java.nio.file.Path.of(new java.net.URI(screenshotFile));
                        }
                        else
                        {
                            path = java.nio.file.Path.of(screenshotFile);
                        }
                        final byte[] bytes = java.nio.file.Files.readAllBytes(path);
                    
                        byte[] compressedBytes = bytes;
                        try
                        {
                            final BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                            if (img != null)
                            {
                                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                                final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                                if (writers.hasNext())
                                {
                                    final ImageWriter writer = writers.next();
                                    final ImageWriteParam param = writer.getDefaultWriteParam();
                                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                                    param.setCompressionQuality(0.85f);
                                    try (final ImageOutputStream ios = ImageIO.createImageOutputStream(os))
                                    {
                                        writer.setOutput(ios);
                                        writer.write(null, new IIOImage(img, null, null), param);
                                    }
                                    writer.dispose();
                                    compressedBytes = os.toByteArray();
                                    mediaType = "image/jpeg";
                                }
                            }
                        }
                        catch (final Exception compressEx)
                        {
                            // ignore and fallback to uncompressed png
                        }
                    
                        base64Data = java.util.Base64.getEncoder().encodeToString(compressedBytes);
                    }
                    catch (final Exception e)
                    {
                        org.slf4j.LoggerFactory.getLogger(SelenideTargetExecutor.class).error("Failed to read screenshot file: " + screenshotFile, e);
                    }
                    attachments.add(new SutAttachment(mediaType, screenshotFile, base64Data));
                }
            }
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

        final String beforeUrl = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver().getCurrentUrl() : null;

        try
        {
            final Action executableAction = resolveVariables(action);
            plugin.execute(executableAction);

            if (WebDriverRunner.hasWebDriverStarted())
            {
                final String afterUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
                if (beforeUrl == null || !beforeUrl.equals(afterUrl))
                {
                    // URL changed or browser just started, wait for document ready and a stabilization delay
                    Selenide.Wait().until(d -> Selenide.executeJavaScript("return document.readyState").equals("complete"));
                    try
                    {
                        Thread.sleep(500);
                    }
                    catch (final InterruptedException ignored)
                    {
                    }
                }
            }
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
            new ActionDefinition("SCROLL", "Scroll element into view or scroll page (value: 'UP'|'TOP'|'DOWN'|'BOTTOM')", Collections.emptyMap()),
            new ActionDefinition("SELECT", "Select option in dropdown", Collections.emptyMap()),
            new ActionDefinition("WAIT", "Wait for element state or pause", Collections.emptyMap()),
            new ActionDefinition("KEY_PRESS", "Send key press events", Collections.emptyMap()),
            new ActionDefinition("SWITCH_WINDOW", "Switch WebDriver focus to another window or tab", Collections.emptyMap()),
            new ActionDefinition("ASSERT", "Assert state or value", Collections.emptyMap()),
            new ActionDefinition("CHECK", "Check or select elements", Collections.emptyMap()),
            new ActionDefinition("STORE", "Store variable values", Collections.emptyMap()),
            new ActionDefinition("BRANCH", "Conditional branch logic", Collections.emptyMap()),
            new ActionDefinition("INCLUDE", "Include external playbook", Collections.emptyMap()),
            new ActionDefinition("SPLIT", "Split step into child instructions", Collections.emptyMap()),
            new ActionDefinition("JAVA_METHOD", "Invoke Java reflection method", Collections.emptyMap())
        );
    }

    @Override
    public boolean supportsLocatorImprovement()
    {
        return true;
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

    private Action resolveVariables(final Action action)
    {
        if (action == null)
        {
            return null;
        }
        final String origTarget = action.getTarget();
        final String origValue = action.getValue();

        String resolvedTarget = interpolateString(origTarget);
        final String resolvedValue = interpolateString(origValue);

        if (resolvedTarget != null && resolvedTarget.matches("http://(localhost|127\\.0\\.0\\.1):\\d+.*"))
        {
            final String activeUrl = getActiveServerUrl();
            if (activeUrl != null && activeUrl.matches("http://(localhost|127\\.0\\.0\\.1):\\d+.*"))
            {
                final String targetPort = resolvedTarget.replaceAll("http://(localhost|127\\.0\\.0\\.1):(\\d+).*", "$2");
                final String activePort = activeUrl.replaceAll("http://(localhost|127\\.0\\.0\\.1):(\\d+).*", "$2");
                if (!targetPort.equals(activePort))
                {
                    resolvedTarget = resolvedTarget.replace(":" + targetPort, ":" + activePort);
                }
            }
        }

        if (Objects.equals(origTarget, resolvedTarget) && Objects.equals(origValue, resolvedValue))
        {
            return action;
        }

        Action resolved = action;
        if (!Objects.equals(origTarget, resolvedTarget))
        {
            resolved = resolved.withTarget(resolvedTarget);
        }
        if (!Objects.equals(origValue, resolvedValue))
        {
            resolved = resolved.withValue(resolvedValue);
        }
        return resolved;
    }

    private String interpolateString(final String text)
    {
        if (text == null || !text.contains("${"))
        {
            return text;
        }
        String result = text;
        int start;
        while ((start = result.indexOf("${")) != -1)
        {
            final int end = result.indexOf("}", start);
            if (end == -1)
            {
                break;
            }
            final String varName = result.substring(start + 2, end);
            String val = null;
            if (this.context != null && this.context.getSessionData() != null)
            {
                final Object valObj = this.context.getSessionData().get(varName);
                if (valObj != null)
                {
                    val = String.valueOf(valObj);
                }
            }
            if (val == null && org.neodymium.util.Neodymium.getData() != null && org.neodymium.util.Neodymium.getData().exists(varName))
            {
                val = org.neodymium.util.Neodymium.getData().asString(varName);
            }
            if (val == null)
            {
                val = System.getProperty(varName);
            }
            if (val != null)
            {
                result = result.substring(0, start) + val + result.substring(end + 1);
            }
            else
            {
                break;
            }
        }
        return result;
    }

    private String getActiveServerUrl()
    {
        try
        {
            if (this.context != null && this.context.getSessionData() != null)
            {
                for (final Map.Entry<String, Object> entry : this.context.getSessionData().getAllRawDataMap().entrySet())
                {
                    if (entry.getValue() != null)
                    {
                        final String strVal = String.valueOf(entry.getValue());
                        if (strVal.matches("http://(localhost|127\\.0\\.0\\.1):\\d+.*"))
                        {
                            return strVal;
                        }
                    }
                }
            }
            if (org.neodymium.util.Neodymium.getData() != null)
            {
                for (final Map.Entry<String, String> entry : org.neodymium.util.Neodymium.getData().entrySet())
                {
                    if (entry.getValue() != null && entry.getValue().matches("http://(localhost|127\\.0\\.0\\.1):\\d+.*"))
                    {
                        return entry.getValue();
                    }
                }
            }
        }
        catch (final Throwable t)
        {
            // ignore
        }
        return null;
    }

    @Override
    public void close() throws Exception
    {
        if (!org.neodymium.util.Neodymium.hasDriver() && com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted())
        {
            final boolean keepOpen = org.neodymium.util.Neodymium.configuration().keepBrowserOpen();
            if (!keepOpen)
            {
                try
                {
                    com.codeborne.selenide.WebDriverRunner.closeWebDriver();
                }
                catch (final Exception e)
                {
                    org.slf4j.LoggerFactory.getLogger(SelenideTargetExecutor.class).debug("Failed to close unmanaged Selenide WebDriver", e);
                }
            }
        }
    }
}
