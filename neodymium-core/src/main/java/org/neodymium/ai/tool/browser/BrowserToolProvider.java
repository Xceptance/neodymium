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
package org.neodymium.ai.tool.browser;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.ai.executor.selenide.PageAnalyzer;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.util.AiAssertions;
import org.neodymium.ai.util.DomQuiescenceWatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.codeborne.selenide.Selenide.$;

/**
 * Provider that instantiates and registers standard browser automation tools
 * and active discovery tools into a {@link ToolRegistry}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class BrowserToolProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserToolProvider.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BrowserToolProvider()
    {
        // Static provider
    }

    private static ObjectNode successNode(final String action)
    {
        final ObjectNode node = MAPPER.createObjectNode();
        node.put("status", "SUCCESS");
        node.put("action", action);
        return node;
    }

    private static ObjectNode errorNode(final String message)
    {
        final ObjectNode node = MAPPER.createObjectNode();
        final String effectiveMessage = message != null ? message : "Unknown error";
        node.put("status", "ERROR");
        node.put("message", effectiveMessage);
        node.put("error", effectiveMessage);
        return node;
    }

    /**
     * Registers all standard browser tools and active discovery tools into the given registry.
     *
     * @param registry tool registry to populate
     */
    public static void registerBrowserTools(final ToolRegistry registry)
    {
        if (registry == null)
        {
            throw new IllegalArgumentException("Registry must not be null.");
        }

        registry.register(createClickTool());
        registry.register(createTypeTool());
        registry.register(createNavigateTool());
        registry.register(createSelectTool());
        registry.register(createHoverTool());
        registry.register(createClearTool());
        registry.register(createClearCookiesTool());
        registry.register(createBackTool());
        registry.register(createForwardTool());
        registry.register(createRefreshTool());
        registry.register(createWaitTool());
        registry.register(createAssertTextTool());
        registry.register(createAssertCountTool());
        registry.register(createScrollTool());
        registry.register(createExecuteScriptTool());
        registry.register(createQueryDomTool());
        registry.register(createInspectTool());
        registry.register(createTakeScreenshotTool());
        registry.register(createInspectVisualTool());
        registry.register(createPressKeyTool());
        registry.register(createRequestContextTool());
        registry.register(createStoreTool());
        registry.register(createListTabsTool());
        registry.register(createSwitchTabTool());
        registry.register(createCloseTabTool());
        registry.register(createUploadFileTool());
        registry.register(createHandleAlertTool());
    }

    private static AiTool createClickTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS or XPath selector of the element to click");
        props.putObject("text").put("type", "string").put("description", "Visible text of the element to click (used if selector is omitted)");
        props.putObject("target").put("type", "string").put("description", "Target expression, such as 'badge:N' or 'coord: x,y'");
        props.putObject("x").put("type", "integer").put("description", "X coordinate for pixel/visual click (relative to selector if selector is provided, or viewport X if omitted)");
        props.putObject("y").put("type", "integer").put("description", "Y coordinate for pixel/visual click (relative to selector if selector is provided, or viewport Y if omitted)");

        final ToolDefinition def = new ToolDefinition("browser_click", "Clicks an interactive element or viewport coordinate in the browser", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context) throws Exception
            {
                final JsonNode args = call.arguments();
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;

                DomQuiescenceWatcher.installTracker();

                final String target = args.hasNonNull("target") ? args.path("target").asText().trim() : "";
                int parsedX = args.hasNonNull("x") ? args.path("x").asInt() : Integer.MIN_VALUE;
                int parsedY = args.hasNonNull("y") ? args.path("y").asInt() : Integer.MIN_VALUE;

                if ((parsedX == Integer.MIN_VALUE || parsedY == Integer.MIN_VALUE) && target.startsWith("coord:"))
                {
                    final String[] parts = target.substring("coord:".length()).split(",");
                    if (parts.length == 2)
                    {
                        try
                        {
                            parsedX = Integer.parseInt(parts[0].trim());
                            parsedY = Integer.parseInt(parts[1].trim());
                        }
                        catch (final NumberFormatException ignored)
                        {
                        }
                    }
                }

                // Case 1: Coordinates provided
                if (parsedX != Integer.MIN_VALUE && parsedY != Integer.MIN_VALUE && driver != null)
                {
                    final int rawX = parsedX;
                    final int rawY = parsedY;
                    final String sel = args.hasNonNull("selector") ? args.path("selector").asText().trim() : "";

                    int x = rawX;
                    int y = rawY;
                    if (!sel.isBlank())
                    {
                        final String rectScript = """
                            var el = document.querySelector(arguments[0]);
                            if (!el) return null;
                            var r = el.getBoundingClientRect();
                            return { x: Math.round(r.left), y: Math.round(r.top) };
                            """;
                        final Object rectObj = ((JavascriptExecutor) driver).executeScript(rectScript, sel);
                        if (rectObj instanceof Map<?, ?> map)
                        {
                            x = ((Number) map.get("x")).intValue() + rawX;
                            y = ((Number) map.get("y")).intValue() + rawY;
                        }
                    }

                    new Actions(driver).moveToLocation(x, y).click().perform();

                    final ReanchoringBridge.ReanchoredElement reanchored = ReanchoringBridge.resolveElementAtPoint(driver, x, y);
                    final ToolResult.Builder builder = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS);
                    final ObjectNode res = successNode("click");
                    res.put("x", x);
                    res.put("y", y);
                    if (!sel.isBlank())
                    {
                        res.put("elementSelector", sel);
                        res.put("offsetX", rawX);
                        res.put("offsetY", rawY);
                    }

                    if (reanchored != null)
                    {
                        res.put("selector", reanchored.selector());
                        builder.withVariable("reanchoredSelector", reanchored.selector());
                        builder.withVariable("reanchoredFeatureVector", reanchored.domFeatureVector());
                    }
                    if (driver != null)
                    {
                        res.put("url", getSafeUrl(driver));
                        res.put("title", getSafeTitle(driver));
                    }
                    builder.withContent(res.toString());
                    return builder.build();
                }

                // Case 2: Target is badge:N
                if (target.startsWith("badge:") && driver != null)
                {
                    final String badgeNum = target.substring("badge:".length()).trim();
                    final String clickBadgeScript = """
                        var badges = document.querySelectorAll('#__neo_som_badges__ > div');
                        for (var i = 0; i < badges.length; i++) {
                            if (badges[i].innerText.trim() === arguments[0]) {
                                var rect = badges[i].getBoundingClientRect();
                                return { x: Math.round(rect.left + rect.width / 2), y: Math.round(rect.top + rect.height / 2) };
                            }
                        }
                        return null;
                        """;
                    final Object coordsObj = ((JavascriptExecutor) driver).executeScript(clickBadgeScript, badgeNum);
                    if (coordsObj instanceof Map<?, ?> coordsMap)
                    {
                        final int bx = ((Number) coordsMap.get("x")).intValue();
                        final int by = ((Number) coordsMap.get("y")).intValue();
                        new Actions(driver).moveToLocation(bx, by).click().perform();

                        final ReanchoringBridge.ReanchoredElement reanchored = ReanchoringBridge.resolveElementAtPoint(driver, bx, by);
                        final ToolResult.Builder b = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS);
                        final ObjectNode res = successNode("click");
                        res.put("target", "badge:" + badgeNum);
                        if (reanchored != null)
                        {
                            res.put("selector", reanchored.selector());
                            b.withVariable("reanchoredSelector", reanchored.selector());
                        }
                        if (driver != null)
                        {
                            res.put("url", getSafeUrl(driver));
                            res.put("title", getSafeTitle(driver));
                        }
                        b.withContent(res.toString());
                        return b.build();
                    }
                }

                // Case 3: Element resolution via selector and text fallback
                final String text = args.hasNonNull("text") ? args.path("text").asText().trim() : "";
                final String selector = args.hasNonNull("selector") ? args.path("selector").asText() : target;

                if (selector.isBlank() && text.isBlank())
                {
                    return ToolResult.error(call.callId(), errorNode("browser_click requires either 'selector', 'text', 'coordinates' (x, y), or 'target'").toString());
                }

                final String targetDesc = !selector.isBlank() ? selector : text;
                final SelenideElement el;

                if (!selector.isBlank())
                {
                    if (selector.startsWith("text="))
                    {
                        final String rawText = selector.substring("text=".length()).trim();
                        el = $(Selectors.byText(rawText)).is(Condition.visible)
                                ? $(Selectors.byText(rawText))
                                : $(Selectors.withText(rawText));
                    }
                    else
                    {
                        el = findElement(selector);
                    }
                }
                else
                {
                    el = $(Selectors.byText(text)).is(Condition.visible)
                            ? $(Selectors.byText(text))
                            : $(Selectors.withText(text));
                }

                SelenideElementFinder.scrollIntoViewIfNeeded(el);

                if (!el.is(Condition.visible))
                {
                    try
                    {
                        final SelenideElement hoverParent = el.closest("#cart-btn-wrapper, .dropdown, [class*='dropdown'], [id*='dropdown']");
                        if (hoverParent.exists() && hoverParent.is(Condition.visible))
                        {
                            hoverParent.hover();
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                SelenideElement clickedEl = el;
                String actualTarget = targetDesc;
                try
                {
                    el.shouldBe(Condition.visible).shouldBe(Condition.interactable).click();
                }
                catch (final Exception | AssertionError e)
                {
                    SelenideElement fallbackTargetEl = null;
                    if (!text.isBlank() && !selector.isBlank())
                    {
                        try
                        {
                            final SelenideElement textEl = $(Selectors.byText(text)).is(Condition.visible)
                                    ? $(Selectors.byText(text))
                                    : $(Selectors.withText(text));
                            SelenideElementFinder.scrollIntoViewIfNeeded(textEl);
                            textEl.shouldBe(Condition.visible).shouldBe(Condition.interactable).click();
                            fallbackTargetEl = textEl;
                        }
                        catch (final Exception | AssertionError ignored)
                        {
                        }
                    }

                    if (fallbackTargetEl == null)
                    {
                        try
                        {
                            SelenideElementFinder.scrollIntoViewIfNeeded(el);
                            Selenide.executeJavaScript("arguments[0].click();", el);
                        }
                        catch (final Throwable ignored)
                        {
                            throw e;
                        }
                    }
                    clickedEl = (fallbackTargetEl != null) ? fallbackTargetEl : el;
                    actualTarget = (fallbackTargetEl != null) ? text : targetDesc;
                }

                if (!isAlertPresent(driver))
                {
                    try
                    {
                        final String tagName = clickedEl.getTagName();
                        if ("input".equalsIgnoreCase(tagName) || "textarea".equalsIgnoreCase(tagName) || "select".equalsIgnoreCase(tagName))
                        {
                            Selenide.executeJavaScript("arguments[0].focus();", clickedEl);
                        }
                    }
                    catch (final Throwable ignored)
                    {
                    }
                }

                final ObjectNode res = successNode("click");
                res.put("target", actualTarget);
                if (driver != null)
                {
                    res.put("url", getSafeUrl(driver));
                    res.put("title", getSafeTitle(driver));
                }
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createTypeTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Selector of the input element");
        props.putObject("text").put("type", "string").put("description", "Text to type into the element");
        props.putObject("clearFirst").put("type", "boolean").put("description", "Whether to clear existing text first (default: true)");
        props.putObject("pressEnter").put("type", "boolean").put("description", "Whether to press Enter key after typing (default: false). MUST remain false unless the test instruction explicitly asks to press Enter, hit Enter, or submit the form.");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");
        req.add("text");

        final ToolDefinition def = new ToolDefinition("browser_type", "Types text into an input or textarea element", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                DomQuiescenceWatcher.installTracker();
                final String selector = resolveSelector(call.arguments());
                final String text = call.arguments().path("text").asText();
                final boolean clearFirst = !call.arguments().has("clearFirst") || call.arguments().path("clearFirst").asBoolean(true);
                final boolean pressEnter = call.arguments().path("pressEnter").asBoolean(false);

                final SelenideElement el = findElement(selector);
                SelenideElementFinder.scrollIntoViewIfNeeded(el);
                el.shouldBe(Condition.visible).shouldBe(Condition.editable);
                if (clearFirst)
                {
                    try
                    {
                        el.val(text);
                    }
                    catch (final Exception e)
                    {
                        el.clear();
                        el.sendKeys(text);
                    }
                }
                else
                {
                    el.sendKeys(text);
                }
                if (pressEnter)
                {
                    el.pressEnter();
                }
                final ObjectNode res = successNode("type");
                res.put("target", selector);
                res.put("value", text);
                res.put("pressedEnter", pressEnter);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createPressKeyTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("key").put("type", "string").put("description", "Key to press, e.g. 'Enter', 'Escape', 'Tab', 'Backspace'");
        props.putObject("selector").put("type", "string").put("description", "Optional selector of the element to send the key to");
        schema.putArray("required").add("key");

        final ToolDefinition def = new ToolDefinition("browser_press_key", "Presses a keyboard key on the active element or specified element", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                DomQuiescenceWatcher.installTracker();
                final String keyName = call.arguments().path("key").asText("ENTER").toUpperCase();
                final String selector = resolveSelector(call.arguments());
                CharSequence resolvedKey;
                try
                {
                    resolvedKey = Keys.valueOf(keyName);
                }
                catch (final IllegalArgumentException e)
                {
                    resolvedKey = Keys.ENTER;
                }
                final CharSequence key = resolvedKey;

                if (!selector.isBlank())
                {
                    $(selector).sendKeys(key);
                }
                else
                {
                    Selenide.actions().sendKeys(key).perform();
                }
                final ObjectNode res = successNode("press_key");
                res.put("key", keyName);
                if (!selector.isBlank())
                {
                    res.put("target", selector);
                }
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createNavigateTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("url").put("type", "string").put("description", "Target URL to navigate to");
        schema.putArray("required").add("url");

        final ToolDefinition def = new ToolDefinition("browser_navigate", "Navigates the browser to the specified URL", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final String url = resolveUrl(call.arguments());
                Selenide.open(url);
                DomQuiescenceWatcher.installTracker();
                DomQuiescenceWatcher.waitForDomQuiet();
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                final ObjectNode res = successNode("navigate");
                res.put("url", driver != null ? getSafeUrl(driver) : url);
                res.put("title", driver != null ? getSafeTitle(driver) : "");
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createSelectTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Selector of the select dropdown");
        props.putObject("value").put("type", "string").put("description", "Option value attribute to select");
        props.putObject("text").put("type", "string").put("description", "Option visible text to select");
        schema.putArray("required").add("selector");

        final ToolDefinition def = new ToolDefinition("browser_select", "Selects an option from a dropdown element by value or text", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                DomQuiescenceWatcher.installTracker();
                final String selector = resolveSelector(call.arguments());
                final SelenideElement el = findElement(selector).shouldBe(Condition.visible).shouldBe(Condition.enabled);

                final ObjectNode res = successNode("select");
                res.put("target", selector);
                if (call.arguments().hasNonNull("value"))
                {
                    final String val = call.arguments().path("value").asText();
                    try
                    {
                        el.selectOptionByValue(val);
                    }
                    catch (final Exception e)
                    {
                        try
                        {
                            el.selectOptionContainingText(val);
                        }
                        catch (final Exception ex)
                        {
                            el.selectOption(val);
                        }
                    }
                    res.put("value", val);
                }
                else if (call.arguments().hasNonNull("text"))
                {
                    final String txt = call.arguments().path("text").asText();
                    try
                    {
                        el.selectOption(txt);
                    }
                    catch (final Exception e)
                    {
                        try
                        {
                            el.selectOptionContainingText(txt);
                        }
                        catch (final Exception ex)
                        {
                            el.selectOptionByValue(txt);
                        }
                    }
                    res.put("text", txt);
                }
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createHoverTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("selector").put("type", "string").put("description", "Selector of the element to hover over");
        schema.putArray("required").add("selector");

        final ToolDefinition def = new ToolDefinition("browser_hover", "Hovers the mouse cursor over an element", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final String selector = resolveSelector(call.arguments());
                findElement(selector).shouldBe(Condition.visible).hover();
                final ObjectNode res = successNode("hover");
                res.put("target", selector);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createClearTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Selector of the input element to clear");
        schema.putArray("required").add("selector");

        final ToolDefinition def = new ToolDefinition("browser_clear", "Clears text in an input or textarea element", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final String selector = resolveSelector(call.arguments());
                findElement(selector).shouldBe(Condition.visible).clear();
                final ObjectNode res = successNode("clear");
                res.put("target", selector);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createClearCookiesTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ToolDefinition def = new ToolDefinition("browser_clear_cookies", "Clears all browser cookies", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                if (WebDriverRunner.hasWebDriverStarted())
                {
                    WebDriverRunner.getWebDriver().manage().deleteAllCookies();
                }
                return ToolResult.success(call.callId(), successNode("clear_cookies").toString());
            }
        };
    }

    private static AiTool createBackTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ToolDefinition def = new ToolDefinition("browser_back", "Navigates back in browser history", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                Selenide.back();
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                final ObjectNode res = successNode("back");
                if (driver != null)
                {
                    res.put("url", getSafeUrl(driver));
                    res.put("title", getSafeTitle(driver));
                }
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createForwardTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ToolDefinition def = new ToolDefinition("browser_forward", "Navigates forward in browser history", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                Selenide.forward();
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                final ObjectNode res = successNode("forward");
                if (driver != null)
                {
                    res.put("url", getSafeUrl(driver));
                    res.put("title", getSafeTitle(driver));
                }
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createRefreshTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ToolDefinition def = new ToolDefinition("browser_refresh", "Refreshes the current browser page", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                Selenide.refresh();
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                final ObjectNode res = successNode("refresh");
                if (driver != null)
                {
                    res.put("url", getSafeUrl(driver));
                    res.put("title", getSafeTitle(driver));
                }
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createWaitTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("durationMs").put("type", "integer").put("description", "Duration to wait in milliseconds");
        props.putObject("time").put("type", "string").put("description", "Duration to wait (e.g. '1000' or '1s')");

        final ToolDefinition def = new ToolDefinition("browser_wait", "Pauses execution for a specified duration", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                long waitMs = 1000;
                final JsonNode args = call.arguments();
                if (args.hasNonNull("durationMs"))
                {
                    waitMs = args.path("durationMs").asLong(1000);
                }
                else if (args.hasNonNull("time"))
                {
                    final String timeStr = args.path("time").asText().trim().toLowerCase();
                    try
                    {
                        if (timeStr.endsWith("ms"))
                        {
                            waitMs = Long.parseLong(timeStr.substring(0, timeStr.length() - 2).trim());
                        }
                        else if (timeStr.endsWith("s"))
                        {
                            waitMs = (long) (Double.parseDouble(timeStr.substring(0, timeStr.length() - 1).trim()) * 1000);
                        }
                        else
                        {
                            waitMs = Long.parseLong(timeStr);
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                else if (args.hasNonNull("value"))
                {
                    final String valStr = args.path("value").asText().trim().toLowerCase();
                    try
                    {
                        if (valStr.endsWith("ms"))
                        {
                            waitMs = Long.parseLong(valStr.substring(0, valStr.length() - 2).trim());
                        }
                        else if (valStr.endsWith("s"))
                        {
                            waitMs = (long) (Double.parseDouble(valStr.substring(0, valStr.length() - 1).trim()) * 1000);
                        }
                        else
                        {
                            waitMs = Long.parseLong(valStr);
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                Selenide.sleep(waitMs);
                final ObjectNode res = successNode("wait");
                res.put("durationMs", waitMs);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    /**
     * Unescapes escaped literal characters (such as "\$", "\(", etc.) that LLMs often emit
     * in literal strings.
     *
     * @param text the text to unescape
     * @return unescaped literal string
     */
    public static String unescapeLiteralText(final String text)
    {
        if (text == null || !text.contains("\\"))
        {
            return text;
        }
        return text.replaceAll("\\\\([$()\\[\\]{}.*+?^|\\\\])", "$1");
    }

    /**
     * Cleans up regular expression patterns emitted by LLMs (e.g. leading/trailing slashes,
     * over-escaped characters, etc.).
     *
     * @param regex the raw regex string
     * @return cleaned regex pattern string
     */
    public static String cleanRegexPattern(final String regex)
    {
        if (regex == null)
        {
            return "";
        }
        String pattern = regex.trim();
        if (pattern.startsWith("/") && pattern.endsWith("/") && pattern.length() >= 2)
        {
            pattern = pattern.substring(1, pattern.length() - 1);
        }
        if (pattern.contains("\\\\"))
        {
            pattern = pattern.replace("\\\\", "\\");
        }
        return pattern;
    }

    static boolean matchesElementText(final SelenideElement el, final String expectedText, final boolean regex, final boolean exact)
    {
        if (el == null || !el.exists())
        {
            return false;
        }

        final List<String> candidates = new ArrayList<>();
        try
        {
            final String text = el.getText();
            if (text != null && !text.isBlank())
            {
                candidates.add(text);
            }
        }
        catch (final Exception ignored)
        {
        }

        try
        {
            final String val = el.getValue();
            if (val != null && !val.isBlank())
            {
                candidates.add(val);
            }
        }
        catch (final Exception ignored)
        {
        }

        try
        {
            final String placeholder = el.getAttribute("placeholder");
            if (placeholder != null && !placeholder.isBlank())
            {
                candidates.add(placeholder);
            }
        }
        catch (final Exception ignored)
        {
        }

        try
        {
            final String ariaLabel = el.getAttribute("aria-label");
            if (ariaLabel != null && !ariaLabel.isBlank())
            {
                candidates.add(ariaLabel);
            }
        }
        catch (final Exception ignored)
        {
        }

        try
        {
            final String parentText = el.getAttribute("data-parent-text");
            if (parentText != null && !parentText.isBlank())
            {
                candidates.add(parentText);
            }
        }
        catch (final Exception ignored)
        {
        }

        try
        {
            final String textContent = el.getAttribute("textContent");
            if (textContent != null && !textContent.isBlank() && !candidates.contains(textContent))
            {
                candidates.add(textContent);
            }
        }
        catch (final Exception ignored)
        {
        }

        if (candidates.isEmpty())
        {
            return false;
        }

        if (regex)
        {
            final String cleanPattern = cleanRegexPattern(expectedText);
            Pattern pattern;
            try
            {
                pattern = Pattern.compile(cleanPattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            }
            catch (final PatternSyntaxException e)
            {
                pattern = Pattern.compile(Pattern.quote(cleanPattern), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            }
            for (final String candidate : candidates)
            {
                if (pattern.matcher(candidate).find())
                {
                    return true;
                }
            }
            return false;
        }

        final String unescaped = unescapeLiteralText(expectedText);
        for (final String candidate : candidates)
        {
            if (exact)
            {
                if (candidate.trim().equalsIgnoreCase(unescaped.trim()))
                {
                    return true;
                }
            }
            else
            {
                if (candidate.toLowerCase().contains(unescaped.toLowerCase()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean matchesElementOrAssociatedLabel(final SelenideElement el, final String expectedText, final boolean regex, final boolean exact)
    {
        if (matchesElementText(el, expectedText, regex, exact))
        {
            return true;
        }
        try
        {
            final String id = el.getAttribute("id");
            if (id != null && !id.isBlank())
            {
                final ElementsCollection forLabels = Selenide.$$("label[for='" + id + "']");
                for (final SelenideElement label : forLabels)
                {
                    if (matchesElementText(label, expectedText, regex, exact))
                    {
                        return true;
                    }
                }
            }
            final SelenideElement parentLabel = el.closest("label");
            if (parentLabel.exists() && matchesElementText(parentLabel, expectedText, regex, exact))
            {
                return true;
            }
        }
        catch (final Exception ignored)
        {
        }
        return false;
    }

    static boolean isTextPresentOnPage(final String expectedText, final boolean regex, final boolean exact)
    {
        try
        {
            if (matchesElementText($("body"), expectedText, regex, exact))
            {
                return true;
            }
        }
        catch (final Exception ignored)
        {
        }

        try
        {
            final ElementsCollection inputs = Selenide.$$("input, textarea, select");
            for (final SelenideElement input : inputs)
            {
                if (matchesElementText(input, expectedText, regex, exact))
                {
                    return true;
                }
            }
        }
        catch (final Exception ignored)
        {
        }

        return false;
    }

    private static AiTool createAssertTextTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Optional selector of the element to assert text on. If omitted, verifies presence anywhere on page.");
        props.putObject("expectedText").put("type", "string").put("description", "Expected text content (plain substring) or regex pattern if regex is true");
        props.putObject("exact").put("type", "boolean").put("description", "Whether text match must be exact (default: false)");
        props.putObject("regex").put("type", "boolean").put("description", "Whether expectedText is a regular expression pattern (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("expectedText");

        final ToolDefinition def = new ToolDefinition("browser_assert_text", "Asserts that an element contains or matches the expected text or pattern", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final String selector = resolveSelector(call.arguments());
                final String rawExpectedText = call.arguments().hasNonNull("expectedText")
                        ? call.arguments().path("expectedText").asText()
                        : call.arguments().path("text").asText();
                final boolean exact = call.arguments().path("exact").asBoolean(false);
                final boolean regex = call.arguments().path("regex").asBoolean(false);
                final String expectedText = regex ? cleanRegexPattern(rawExpectedText) : unescapeLiteralText(rawExpectedText);

                final boolean isTitle = selector != null && ("title".equalsIgnoreCase(selector.trim())
                        || "head > title".equalsIgnoreCase(selector.trim())
                        || "head title".equalsIgnoreCase(selector.trim()));

                if (isTitle)
                {
                    if (!WebDriverRunner.hasWebDriverStarted())
                    {
                        throw new AssertionError("No active browser window found to assert page title");
                    }
                    final String pageTitle = WebDriverRunner.getWebDriver().getTitle();
                    final String actualTitle = pageTitle != null ? pageTitle : "";
                    if (regex)
                    {
                        Pattern pattern;
                        try
                        {
                            pattern = Pattern.compile(expectedText, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                        }
                        catch (final PatternSyntaxException e)
                        {
                            pattern = Pattern.compile(Pattern.quote(expectedText), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                        }
                        if (!pattern.matcher(actualTitle).find())
                        {
                            throw new AssertionError("Page title \"" + actualTitle + "\" does not match regex pattern \"" + expectedText + "\"");
                        }
                    }
                    else if (exact)
                    {
                        if (!actualTitle.trim().equalsIgnoreCase(expectedText.trim()))
                        {
                            throw new AssertionError("Page title \"" + actualTitle + "\" does not exactly match \"" + expectedText + "\"");
                        }
                    }
                    else
                    {
                        if (!actualTitle.toLowerCase().contains(expectedText.toLowerCase()))
                        {
                            throw new AssertionError("Page title \"" + actualTitle + "\" does not contain expected text \"" + expectedText + "\"");
                        }
                    }
                }
                else if (selector == null || selector.isBlank() || "body".equalsIgnoreCase(selector.trim()) || "html".equalsIgnoreCase(selector.trim()))
                {
                    boolean found = false;
                    final long start = System.currentTimeMillis();
                    final long timeout = Configuration.timeout;
                    while (!found && (System.currentTimeMillis() - start) < timeout)
                    {
                        found = isTextPresentOnPage(expectedText, regex, exact);
                        if (!found)
                        {
                            Selenide.sleep(100);
                        }
                    }
                    if (!found)
                    {
                        throw new AssertionError("Expected text/pattern \"" + expectedText + "\" was not found anywhere on the page within " + timeout + "ms.");
                    }
                }
                else
                {
                    boolean matched = false;
                    final long start = System.currentTimeMillis();
                    final long timeout = Configuration.timeout;

                    while (!matched && (System.currentTimeMillis() - start) < timeout)
                    {
                        final ElementsCollection elements = findElements(selector);
                        if (!elements.isEmpty())
                        {
                            for (final SelenideElement el : elements)
                            {
                                if (matchesElementOrAssociatedLabel(el, expectedText, regex, exact))
                                {
                                    matched = true;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            try
                            {
                                final SelenideElement singleEl = findElement(selector);
                                if (singleEl.exists() && matchesElementOrAssociatedLabel(singleEl, expectedText, regex, exact))
                                {
                                    matched = true;
                                }
                            }
                            catch (final Exception ignored)
                            {
                            }
                        }

                        if (!matched)
                        {
                            try
                            {
                                final ElementsCollection currentElements = findElements(selector);
                                final SelenideElement primary = currentElements.isEmpty() ? findElement(selector) : currentElements.first();
                                if (primary.exists())
                                {
                                    final SelenideElement container = primary.closest(".form-group, .form-floating, .form-row, .field, .input-group, form, [class*='checkout'], [class*='order'], [class*='summary'], [class*='card'], [class*='table']");
                                    if (container.exists() && matchesElementText(container, expectedText, regex, exact))
                                    {
                                        matched = true;
                                    }
                                    else if (primary.parent().exists() && matchesElementText(primary.parent(), expectedText, regex, exact))
                                    {
                                        matched = true;
                                    }
                                }
                            }
                            catch (final Exception ignored)
                            {
                            }
                        }

                        if (!matched)
                        {
                            if (isTextPresentOnPage(expectedText, regex, exact))
                            {
                                break;
                            }
                            Selenide.sleep(100);
                        }
                    }

                    if (!matched)
                    {
                        if (!isTextPresentOnPage(expectedText, regex, exact))
                        {
                            throw new AssertionError("Expected text/pattern \"" + expectedText + "\" was not found on selector \"" + selector + "\" nor anywhere on the page within " + timeout + "ms.");
                        }

                        return ToolResult.error(call.callId(), errorNode("Expected text \"" + expectedText + "\" was not found on element \"" + selector + "\".").toString());
                    }
                }

                final String targetDesc = (selector == null || selector.isBlank()) ? "page" : selector;
                final ObjectNode res = successNode("assert_text");
                res.put("target", targetDesc);
                res.put("expected", expectedText);
                res.put("regex", regex);
                res.put("matched", true);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createAssertCountTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS or XPath selector targeting the elements to count");
        props.putObject("expectedCount").put("type", "integer").put("description", "Exact expected count of matching elements");
        props.putObject("minCount").put("type", "integer").put("description", "Minimum expected count of matching elements (inclusive, count >= minCount)");
        props.putObject("maxCount").put("type", "integer").put("description", "Maximum expected count of matching elements (inclusive, count <= maxCount)");
        props.putObject("visibleOnly").put("type", "boolean").put("description", "Whether to count only visible elements (default true)");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");

        final ToolDefinition def = new ToolDefinition("browser_assert_count", "Asserts that the count of elements matching a selector satisfies expected criteria (exact, min, or max)", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final String selector = resolveSelector(call.arguments());
                if (selector == null || selector.isBlank())
                {
                    return ToolResult.error(call.callId(), errorNode("A valid 'selector' is required for browser_assert_count").toString());
                }

                final boolean hasExpected = call.arguments().hasNonNull("expectedCount");
                final boolean hasMin = call.arguments().hasNonNull("minCount");
                final boolean hasMax = call.arguments().hasNonNull("maxCount");

                if (!hasExpected && !hasMin && !hasMax)
                {
                    return ToolResult.error(call.callId(), errorNode("At least one count constraint ('expectedCount', 'minCount', or 'maxCount') must be specified for browser_assert_count.").toString());
                }

                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    throw new AssertionError("No active browser window found to assert element count for '" + selector + "'");
                }

                final boolean visibleOnly = !call.arguments().has("visibleOnly") || call.arguments().path("visibleOnly").asBoolean(true);

                final ElementsCollection allElements = findElements(selector);
                int actualCount = 0;
                int totalElements = 0;

                try
                {
                    totalElements = allElements.size();
                    if (visibleOnly)
                    {
                        for (final SelenideElement el : allElements)
                        {
                            try
                            {
                                if (el.isDisplayed())
                                {
                                    actualCount++;
                                }
                            }
                            catch (final Exception ignored)
                            {
                            }
                        }
                    }
                    else
                    {
                        actualCount = totalElements;
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Failed retrieving element collection for selector '{}': {}", selector, e.getMessage());
                }

                if (hasExpected)
                {
                    final int expected = call.arguments().path("expectedCount").asInt();
                    if (actualCount != expected)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected exactly %d elements, but found %d (visible: %d, total in DOM: %d)",
                                selector, expected, actualCount, actualCount, totalElements));
                    }
                }
                if (hasMin)
                {
                    final int min = call.arguments().path("minCount").asInt();
                    if (actualCount < min)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected at least %d elements, but found %d (visible: %d, total in DOM: %d)",
                                selector, min, actualCount, actualCount, totalElements));
                    }
                }
                if (hasMax)
                {
                    final int max = call.arguments().path("maxCount").asInt();
                    if (actualCount > max)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected at most %d elements, but found %d (visible: %d, total in DOM: %d)",
                                selector, max, actualCount, actualCount, totalElements));
                    }
                }

                final ObjectNode res = successNode("assert_count");
                res.put("target", selector);
                res.put("actualCount", actualCount);
                res.put("totalInDom", totalElements);
                res.put("visibleOnly", visibleOnly);
                if (hasExpected)
                {
                    res.put("expectedCount", call.arguments().path("expectedCount").asInt());
                }
                if (hasMin)
                {
                    res.put("minCount", call.arguments().path("minCount").asInt());
                }
                if (hasMax)
                {
                    res.put("maxCount", call.arguments().path("maxCount").asInt());
                }
                res.put("matched", true);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static String resolveSelector(final JsonNode args)
    {
        if (args != null)
        {
            if (args.hasNonNull("selector") && !args.path("selector").asText().isBlank())
            {
                return args.path("selector").asText();
            }
            if (args.hasNonNull("target") && !args.path("target").asText().isBlank())
            {
                return args.path("target").asText();
            }
            if (args.hasNonNull("locator") && !args.path("locator").asText().isBlank())
            {
                return args.path("locator").asText();
            }
        }
        return "";
    }

    private static String resolveUrl(final JsonNode args)
    {
        if (args != null)
        {
            if (args.hasNonNull("url") && !args.path("url").asText().isBlank())
            {
                return args.path("url").asText();
            }
            if (args.hasNonNull("target") && !args.path("target").asText().isBlank())
            {
                return args.path("target").asText();
            }
            if (args.hasNonNull("locator") && !args.path("locator").asText().isBlank())
            {
                return args.path("locator").asText();
            }
            if (args.hasNonNull("value") && !args.path("value").asText().isBlank())
            {
                return args.path("value").asText();
            }
        }
        return "";
    }

    private static SelenideElement findElement(final String selector)
    {
        if (selector == null || selector.isBlank())
        {
            return $("body");
        }
        final SelenideElement found = SelenideElementFinder.findElement(selector);
        return found != null ? found : $(selector);
    }

    private static ElementsCollection findElements(final String selector)
    {
        if (selector == null || selector.isBlank())
        {
            return Selenide.$$("body");
        }
        try
        {
            return Selenide.$$(SelenideElementFinder.resolveLocator(selector));
        }
        catch (final Exception ignored)
        {
            try
            {
                return Selenide.$$(selector);
            }
            catch (final Exception ex)
            {
                return Selenide.$$("body");
            }
        }
    }

    private static int windowInnerHeight()
    {
        try
        {
            final Object val = Selenide.executeJavaScript("return window.innerHeight;");
            if (val instanceof final Number n)
            {
                return n.intValue();
            }
        }
        catch (final Exception ignored)
        {
        }
        return 600;
    }

    private static AiTool createScrollTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("direction").put("type", "string").put("description", "Direction to scroll ('down', 'up', 'top', 'bottom', 'left', 'right')");
        props.putObject("selector").put("type", "string").put("description", "Optional element selector to scroll into view, or target container element");
        props.putObject("container").put("type", "string").put("description", "Optional selector for the scrollable container element (defaults to window/document)");
        props.putObject("yOffset").put("type", "integer").put("description", "Optional pixel distance to scroll vertically");
        props.putObject("xOffset").put("type", "integer").put("description", "Optional pixel distance to scroll horizontally");

        final ToolDefinition def = new ToolDefinition("browser_scroll", "Scrolls the page viewport, scrolls a specific element into view, or scrolls inside a container element", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final JsonNode args = call.arguments();
                final String direction = args.path("direction").asText("down").toLowerCase();
                final int yOffset;
                if (args.hasNonNull("yOffset"))
                {
                    yOffset = args.path("yOffset").asInt();
                }
                else if (args.hasNonNull("value") && args.path("value").asText().trim().matches("^-?\\d+$"))
                {
                    yOffset = Integer.parseInt(args.path("value").asText().trim());
                }
                else
                {
                    yOffset = 0;
                }
                final int xOffset = args.hasNonNull("xOffset") ? args.path("xOffset").asInt() : 0;
                final String containerSelector = args.hasNonNull("container") ? args.path("container").asText().trim() : null;

                // 1. Explicit or detected container scroll
                final String targetContainer = (containerSelector != null && !containerSelector.isBlank())
                        ? containerSelector
                        : ((args.hasNonNull("selector") && (args.hasNonNull("direction") || yOffset != 0 || xOffset != 0))
                                ? args.path("selector").asText().trim()
                                : null);

                if (targetContainer != null && !targetContainer.isBlank()
                        && !"body".equalsIgnoreCase(targetContainer)
                        && !"html".equalsIgnoreCase(targetContainer)
                        && !"window".equalsIgnoreCase(targetContainer)
                        && !"document".equalsIgnoreCase(targetContainer))
                {
                    final Object result = Selenide.executeJavaScript(
                            "const target = arguments[0];"
                            + "const dir = arguments[1];"
                            + "const yOff = arguments[2];"
                            + "const xOff = arguments[3];"
                            + "const el = document.querySelector(target);"
                            + "if (!el) { return false; }"
                            + "const distY = (yOff !== 0) ? yOff : Math.round(el.clientHeight ? el.clientHeight * 0.8 : 400);"
                            + "const distX = (xOff !== 0) ? xOff : Math.round(el.clientWidth ? el.clientWidth * 0.8 : 400);"
                            + "if (dir === 'top') { el.scrollTop = 0; }"
                            + "else if (dir === 'bottom') { el.scrollTop = el.scrollHeight; }"
                            + "else if (dir === 'up') { el.scrollTop -= distY; }"
                            + "else if (dir === 'left') { el.scrollLeft -= distX; }"
                            + "else if (dir === 'right') { el.scrollLeft += distX; }"
                            + "else { el.scrollTop += distY; }"
                            + "el.dispatchEvent(new Event('scroll', { bubbles: true }));"
                            + "return true;",
                            targetContainer, direction, yOffset, xOffset);

                    if (Boolean.TRUE.equals(result))
                    {
                        final ObjectNode res = successNode("scroll");
                        res.put("container", targetContainer);
                        res.put("direction", direction);
                        if (yOffset != 0)
                        {
                            res.put("yOffset", yOffset);
                        }
                        return ToolResult.success(call.callId(), res.toString());
                    }
                }

                // 2. Element scroll into view
                if (args.hasNonNull("selector") && !args.path("selector").asText().isBlank())
                {
                    final String sel = args.path("selector").asText().trim();
                    final SelenideElement el = findElement(sel);
                    if (el.exists())
                    {
                        Selenide.executeJavaScript(
                            "if (arguments[0] && typeof arguments[0].scrollIntoView === 'function') {"
                            + "  arguments[0].scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' });"
                            + "}", el);
                        final ObjectNode res = successNode("scroll");
                        res.put("target", sel);
                        return ToolResult.success(call.callId(), res.toString());
                    }

                    // Element not in DOM yet (e.g. unmounted in a virtual list) -> scroll nearest scrollable container or window
                    Selenide.executeJavaScript(
                        "const scrollable = document.querySelector('.virtual-scroll-viewport, [data-virtual-scroll], [role=\"feed\"], [overflow=\"auto\"]') || "
                        + "  Array.from(document.querySelectorAll('div, main, section')).find(e => { const s = window.getComputedStyle(e); return (s.overflowY === 'auto' || s.overflowY === 'scroll') && e.scrollHeight > e.clientHeight; }); "
                        + "if (scrollable) { scrollable.scrollTop += Math.round(scrollable.clientHeight * 0.8); scrollable.dispatchEvent(new Event('scroll', { bubbles: true })); } "
                        + "else { window.scrollBy(0, Math.round(window.innerHeight * 0.8)); }"
                    );
                    final ObjectNode res = successNode("scroll");
                    res.put("target", sel);
                    res.put("note", "Element '" + sel + "' was not found in the current DOM (possibly unmounted in a virtual list). Scrolled container downward to reveal more items.");
                    return ToolResult.success(call.callId(), res.toString());
                }

                // 3. Window scroll
                if ("top".equals(direction))
                {
                    Selenide.executeJavaScript("window.scrollTo(0, 0);");
                }
                else if ("bottom".equals(direction))
                {
                    Selenide.executeJavaScript("window.scrollTo(0, document.body.scrollHeight);");
                }
                else if ("up".equals(direction))
                {
                    final int dist = (yOffset != 0) ? -Math.abs(yOffset) : -Math.round((float) (windowInnerHeight() * 0.8));
                    Selenide.executeJavaScript("window.scrollBy(0, arguments[0]);", dist);
                }
                else if ("left".equals(direction))
                {
                    final int dist = (xOffset != 0) ? -Math.abs(xOffset) : -500;
                    Selenide.executeJavaScript("window.scrollBy(arguments[0], 0);", dist);
                }
                else if ("right".equals(direction))
                {
                    final int dist = (xOffset != 0) ? Math.abs(xOffset) : 500;
                    Selenide.executeJavaScript("window.scrollBy(arguments[0], 0);", dist);
                }
                else
                {
                    final int dist = (yOffset != 0) ? yOffset : Math.round((float) (windowInnerHeight() * 0.8));
                    Selenide.executeJavaScript("window.scrollBy(0, arguments[0]);", dist);
                }

                final ObjectNode res = successNode("scroll");
                res.put("direction", direction);
                if (yOffset != 0)
                {
                    res.put("yOffset", yOffset);
                }
                if (xOffset != 0)
                {
                    res.put("xOffset", xOffset);
                }
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createExecuteScriptTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("script").put("type", "string").put("description", "JavaScript code to execute in browser context");
        props.putObject("args").put("type", "array").put("description", "Optional arguments passed to the script").putObject("items").put("type", "string");
        schema.putArray("required").add("script");

        final ToolDefinition def = new ToolDefinition("browser_execute_script", "Executes JavaScript in the browser context and returns result", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final String script = call.arguments().path("script").asText();
                try
                {
                    final Object result = Selenide.executeJavaScript(script);
                    final ObjectNode res = successNode("execute_script");
                    if (result != null)
                    {
                        res.put("result", result.toString());
                    }
                    else
                    {
                        res.putNull("result");
                    }
                    return ToolResult.success(call.callId(), res.toString());
                }
                catch (final Exception e)
                {
                    final ObjectNode res = errorNode("JavaScript execution failed: " + e.getMessage());
                    res.put("action", "execute_script");
                    return ToolResult.error(call.callId(), res.toString());
                }
            }
        };
    }

    private static AiTool createQueryDomTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS selector to search for");
        props.putObject("text").put("type", "string").put("description", "Case-insensitive text substring to search for");
        props.putObject("includeAncestors").put("type", "integer").put("description", "Number of ancestor levels to include in returned subtree (default: 1)");
        props.putObject("limit").put("type", "integer").put("description", "Maximum number of elements to return (default: 10)");

        final ToolDefinition def = new ToolDefinition("browser_query_dom", "Searches the live DOM for elements matching a selector or text, returning clean subtrees with attributes and visibility", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final JsonNode args = call.arguments();
                final String selector = args.hasNonNull("selector") ? args.path("selector").asText() : "";
                final String text = args.hasNonNull("text") ? args.path("text").asText() : "";
                final int limit = args.hasNonNull("limit") ? args.path("limit").asInt(10) : 10;

                final String queryScript = """
                    return (function(sel, searchText, maxCount) {
                        var candidates = [];
                        if (sel && sel.trim()) {
                            try {
                                candidates = Array.from(document.querySelectorAll(sel));
                            } catch(e) {}
                        } else {
                            candidates = Array.from(document.querySelectorAll('button, a, input, select, textarea, [role="button"], [role="link"], [role="checkbox"], [role="radio"], [role="tab"], [role="option"], label, h1, h2, h3, h4, p, span, div'));
                        }

                        var lowerText = (searchText || '').toLowerCase().trim();
                        var exactMatches = [];
                        var wordMatches = [];
                        var partialMatches = [];

                        for (var i = 0; i < candidates.length; i++) {
                            var el = candidates[i];
                            var elText = (el.innerText || el.textContent || '').trim();
                            if (lowerText) {
                                var elLower = elText.toLowerCase();
                                if (elLower === lowerText) {
                                    exactMatches.push(el);
                                } else if (new RegExp('(?:^|\\\\s)' + lowerText.replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&') + '(?:$|\\\\s)').test(elLower)) {
                                    wordMatches.push(el);
                                } else if (elLower.indexOf(lowerText) !== -1) {
                                    if (el.children.length <= 2) {
                                        partialMatches.push(el);
                                    }
                                }
                            } else {
                                partialMatches.push(el);
                            }
                        }

                        var filtered = exactMatches.concat(wordMatches).concat(partialMatches);
                        var results = [];
                        var seen = new Set();

                        for (var j = 0; j < filtered.length; j++) {
                            var el = filtered[j];
                            if (seen.has(el)) continue;
                            seen.add(el);

                            var rect = el.getBoundingClientRect();
                            var inViewport = rect.top < window.innerHeight && rect.bottom > 0 && rect.left < window.innerWidth && rect.right > 0;
                            var tag = el.tagName.toLowerCase();
                            var idStr = el.id ? '#' + el.id : '';
                            var clsStr = el.className && typeof el.className === 'string' ? '.' + el.className.trim().split(/\\s+/).slice(0, 2).join('.') : '';
                            var elText = (el.innerText || el.textContent || '').trim();

                            results.push({
                                tag: tag,
                                selector: tag + idStr + clsStr,
                                text: elText.length > 80 ? elText.substring(0, 80) + '…' : elText,
                                inViewport: inViewport,
                                y: Math.round(rect.top + window.scrollY),
                                outerHtml: el.outerHTML.length > 200 ? el.outerHTML.substring(0, 200) + '…' : el.outerHTML
                            });
                            if (results.length >= maxCount) break;
                        }
                        return JSON.stringify(results);
                    })(arguments[0], arguments[1], arguments[2]);
                    """;

                final Object res = Selenide.executeJavaScript(queryScript, selector, text, limit);
                final String resStr = res != null ? res.toString() : "[]";
                final ObjectNode rootNode = successNode("query_dom");
                JsonNode matchesNode;
                try
                {
                    matchesNode = MAPPER.readTree(resStr);
                }
                catch (final Exception e)
                {
                    matchesNode = MAPPER.createArrayNode();
                }
                rootNode.set("matches", matchesNode);
                rootNode.put("matchCount", matchesNode.size());
                if ("[]".equals(resStr.trim()) && !text.isBlank())
                {
                    rootNode.put("note", "No elements found matching text: '" + text + "'. Verify if the element is inside a closed menu, dropdown, modal, or iframe");
                }
                return ToolResult.success(call.callId(), rootNode.toString());
            }
        };
    }

    private static AiTool createInspectTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("selector").put("type", "string").put("description", "Selector of the element to inspect");
        schema.putArray("required").add("selector");

        final ToolDefinition def = new ToolDefinition("browser_inspect", "Inspects a specific DOM element, returning its full outerHTML, attributes, and visibility", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final String selector = call.arguments().path("selector").asText();
                final String inspectScript = """
                    return (function(sel) {
                        var el = document.querySelector(sel);
                        if (!el) return null;
                        var rect = el.getBoundingClientRect();
                        var style = window.getComputedStyle(el);
                        return JSON.stringify({
                            selector: sel,
                            tagName: el.tagName.toLowerCase(),
                            outerHTML: el.outerHTML,
                            text: (el.innerText || '').trim(),
                            visible: style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0,
                            boundingBox: { x: Math.round(rect.left), y: Math.round(rect.top), width: Math.round(rect.width), height: Math.round(rect.height) }
                        });
                    })(arguments[0]);
                    """;

                final Object result = Selenide.executeJavaScript(inspectScript, selector);
                if (result == null)
                {
                    return ToolResult.error(call.callId(), errorNode("Element not found for selector: " + selector).toString());
                }
                try
                {
                    final ObjectNode node = (ObjectNode) MAPPER.readTree(result.toString());
                    node.put("status", "SUCCESS");
                    node.put("action", "inspect");
                    return ToolResult.success(call.callId(), node.toString());
                }
                catch (final Exception e)
                {
                    return ToolResult.success(call.callId(), result.toString());
                }
            }
        };
    }

    private static AiTool createTakeScreenshotTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("fullPage").put("type", "boolean").put("description", "Whether to capture full scrollable page or viewport only (default: false)");
        props.putObject("markInteractive").put("type", "boolean").put("description", "Whether to inject temporary Set-of-Marks numeric visual badges (default: false)");

        final ToolDefinition def = new ToolDefinition("browser_take_screenshot", "Captures a screenshot of the browser viewport with optional Set-of-Marks visual badges", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final boolean markInteractive = call.arguments().path("markInteractive").asBoolean(false);
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;

                if (driver == null)
                {
                    return ToolResult.error(call.callId(), errorNode("WebDriver is not running; cannot capture screenshot").toString());
                }

                List<Map<String, Object>> badges = null;
                if (markInteractive)
                {
                    badges = VisualBadgeInjector.injectBadges(driver);
                }

                final byte[] screenshotBytes;
                try
                {
                    screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                }
                finally
                {
                    if (markInteractive)
                    {
                        VisualBadgeInjector.removeBadges(driver);
                    }
                }

                final String base64 = Base64.getEncoder().encodeToString(screenshotBytes);
                final ObjectNode res = successNode("take_screenshot");
                res.put("bytes", screenshotBytes.length);
                if (badges != null && !badges.isEmpty())
                {
                    res.put("badgeCount", badges.size());
                }

                final ToolResult.Builder builder = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                        .withContent(res.toString())
                        .withArtifact("screenshot", "image/png", screenshotBytes)
                        .withVariable("screenshotBase64", "data:image/png;base64," + base64);

                if (badges != null && !badges.isEmpty())
                {
                    builder.withVariable("visualBadges", badges);
                }

                return builder.build();
            }
        };
    }

    private static AiTool createInspectVisualTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Selector of the element or container to visually inspect");
        props.putObject("prompt").put("type", "string").put("description", "Visual prompt or question about the element");
        schema.putArray("required").add("selector");

        final ToolDefinition def = new ToolDefinition("browser_inspect_visual", "Captures a targeted visual crop of a specific element or container for multimodal inspection", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context) throws Exception
            {
                final String selector = call.arguments().path("selector").asText();
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;

                if (driver == null)
                {
                    return ToolResult.error(call.callId(), errorNode("WebDriver not started").toString());
                }

                final byte[] fullScreenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                final BufferedImage fullImg = ImageIO.read(new ByteArrayInputStream(fullScreenshot));

                final String rectScript = """
                    var el = document.querySelector(arguments[0]);
                    if (!el) return null;
                    var r = el.getBoundingClientRect();
                    return { x: Math.max(0, Math.round(r.left)), y: Math.max(0, Math.round(r.top)), w: Math.round(r.width), h: Math.round(r.height) };
                    """;
                final Object rectObj = ((JavascriptExecutor) driver).executeScript(rectScript, selector);

                if (rectObj instanceof Map<?, ?> map && fullImg != null)
                {
                    final int x = Math.min(fullImg.getWidth() - 1, ((Number) map.get("x")).intValue());
                    final int y = Math.min(fullImg.getHeight() - 1, ((Number) map.get("y")).intValue());
                    final int w = Math.min(fullImg.getWidth() - x, Math.max(1, ((Number) map.get("w")).intValue()));
                    final int h = Math.min(fullImg.getHeight() - y, Math.max(1, ((Number) map.get("h")).intValue()));

                    final BufferedImage crop = fullImg.getSubimage(x, y, w, h);
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(crop, "png", baos);
                    final byte[] cropBytes = baos.toByteArray();
                    final String base64 = Base64.getEncoder().encodeToString(cropBytes);

                    final ObjectNode res = successNode("inspect_visual");
                    res.put("target", selector);
                    res.put("width", w);
                    res.put("height", h);

                    final String prompt = call.arguments().path("prompt").asText("");
                    if (!prompt.isBlank())
                    {
                        res.put("prompt", prompt);
                    }

                    return ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                            .withContent(res.toString())
                            .withArtifact("crop_" + selector, "image/png", cropBytes)
                            .withVariable("cropBase64", "data:image/png;base64," + base64)
                            .withVariable("cropSelector", selector)
                            .withVariable("cropWidth", w)
                            .withVariable("cropHeight", h)
                            .build();
                }

                return ToolResult.error(call.callId(), errorNode("Could not resolve bounding box for selector: " + selector).toString());
            }
        };
    }

    private static AiTool createRequestContextTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        final ArrayNode levelEnum = props.putObject("level")
                .put("type", "string")
                .put("description", "Depth and scope of the context to capture ('LEAN', 'STANDARD', 'RICH', 'VISUAL', 'VISUAL_LEAN', 'VISUAL_RICH')")
                .putArray("enum");
        levelEnum.add("LEAN");
        levelEnum.add("STANDARD");
        levelEnum.add("RICH");
        levelEnum.add("VISUAL");
        levelEnum.add("VISUAL_LEAN");
        levelEnum.add("VISUAL_RICH");
        props.putObject("fullPage")
                .put("type", "boolean")
                .put("description", "Whether screenshot capture should be full scrollable page (default: false)");

        final ToolDefinition def = new ToolDefinition(
                "browser_request_context",
                "Requests a fresh page DOM snapshot at a specified depth and scope (e.g. 'STANDARD' to include static text leaves, 'RICH' for all attributes and deep hierarchy, or 'VISUAL' for screenshots).",
                schema);

        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final JsonNode args = call.arguments();
                final String levelStr = args.hasNonNull("level") ? args.path("level").asText() : "STANDARD";
                final boolean fullPage = args.path("fullPage").asBoolean(false);
                final ContextLevel targetLevel = ContextLevel.fromString(levelStr, ContextLevel.STANDARD);

                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                if (driver == null)
                {
                    final ObjectNode res = successNode("request_context");
                    res.put("level", targetLevel.name());
                    res.put("message", "Requested context level updated to " + targetLevel.name());

                    if (context != null)
                    {
                        context.setVariable("KEY_REQUESTED_CONTEXT_LEVEL", targetLevel);
                    }

                    return ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                            .withContent(res.toString())
                            .withVariable("requestedContextLevel", targetLevel.name())
                            .build();
                }

                final PageAnalyzer analyzer = new PageAnalyzer(driver);
                final String dom = analyzer.captureSimplifiedDom(targetLevel);

                final ObjectNode res = successNode("request_context");
                res.put("level", targetLevel.name());
                res.put("dom", dom);

                final ToolResult.Builder builder = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                        .withContent(res.toString())
                        .withVariable("requestedContextLevel", targetLevel.name());

                if (targetLevel.includesScreenshot())
                {
                    try
                    {
                        final String base64 = analyzer.captureScreenshot("context_" + System.currentTimeMillis(), targetLevel, fullPage, null);
                        if (base64 != null)
                        {
                            builder.withVariable("screenshotBase64", "data:image/png;base64," + base64);
                            builder.withArtifact("screenshot", "image/png", Base64.getDecoder().decode(base64));
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                if (context != null)
                {
                    context.setVariable("KEY_REQUESTED_CONTEXT_LEVEL", targetLevel);
                }

                return builder.build();
            }
        };
    }

    private static AiTool createStoreTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("variableName").put("type", "string").put("description", "Name of the variable to store the value in session data");
        props.putObject("selector").put("type", "string").put("description", "CSS, XPath, or text locator of the element to capture text from");
        props.putObject("value").put("type", "string").put("description", "Literal value to store directly instead of reading element text");
        props.putObject("adjust").put("type", "boolean").put("description", "Whether to normalize price/currency/numeric formatting (default: false)");
        final ArrayNode req = schema.putArray("required");
        req.add("variableName");

        final ToolDefinition def = new ToolDefinition("browser_store", "Captures text from a DOM element or stores a specified value into an execution session variable for later use", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                final JsonNode args = call.arguments();
                final String variableName;
                if (args.hasNonNull("variableName") && !args.path("variableName").asText().isBlank())
                {
                    variableName = args.path("variableName").asText().trim();
                }
                else if (args.hasNonNull("variable") && !args.path("variable").asText().isBlank())
                {
                    variableName = args.path("variable").asText().trim();
                }
                else if (args.hasNonNull("name") && !args.path("name").asText().isBlank())
                {
                    variableName = args.path("name").asText().trim();
                }
                else if (args.hasNonNull("key") && !args.path("key").asText().isBlank())
                {
                    variableName = args.path("key").asText().trim();
                }
                else
                {
                    variableName = "";
                }

                if (variableName.isEmpty())
                {
                    return ToolResult.error(call.callId(), errorNode("browser_store requires a non-empty 'variableName'").toString());
                }

                final boolean adjust = args.path("adjust").asBoolean(false);
                final String valueToStore;

                if (args.hasNonNull("value") && !args.path("value").asText().isBlank())
                {
                    final String literalVal = args.path("value").asText();
                    valueToStore = adjust ? AiAssertions.normalizeNumericOrPrice(literalVal) : literalVal;
                }
                else
                {
                    final String selector = resolveSelector(args);
                    if (selector == null || selector.isBlank())
                    {
                        return ToolResult.error(call.callId(), errorNode("browser_store requires either a 'selector' to capture text from or a literal 'value'").toString());
                    }

                    try
                    {
                        final SelenideElement el = findElement(selector);
                        final String text = el.getText();
                        final String actualText = (text != null && !text.isBlank()) ? text : el.getValue();
                        final String trimmed = actualText != null ? actualText.trim() : "";
                        valueToStore = adjust ? AiAssertions.normalizeNumericOrPrice(trimmed) : trimmed;
                    }
                    catch (final Exception e)
                    {
                        return ToolResult.error(call.callId(), errorNode("Failed to capture text from element '" + selector + "': " + e.getMessage()).toString());
                    }
                }

                ExecutionContext execCtx = ExecutionContext.getActiveContext();
                if (execCtx == null && context != null)
                {
                    execCtx = context.getVariable("neodymium.executionContext", ExecutionContext.class).orElse(null);
                }

                if (execCtx != null && execCtx.getSessionData() != null)
                {
                    final boolean isSensitive = variableName.toLowerCase().contains("password") || variableName.toLowerCase().contains("secret");
                    execCtx.getSessionData().putDynamic(variableName, valueToStore, isSensitive);
                }
                else
                {
                    return ToolResult.error(call.callId(), errorNode("No active ExecutionContext or SessionData found to store variable '" + variableName + "'").toString());
                }

                final ObjectNode resultNode = MAPPER.createObjectNode();
                resultNode.put("status", "success");
                resultNode.put("variableName", variableName);
                resultNode.put("storedValue", valueToStore);
                resultNode.put("message", "Stored variable '" + variableName + "' = \"" + valueToStore + "\"");
                return ToolResult.success(call.callId(), resultNode.toString());
            }
        };
    }

    /**
     * Switches the active WebDriver window/tab focus to a target handle, index, or title/URL match.
     *
     * @param driver active WebDriver instance
     * @param param target handle, index, or title/URL pattern (null/blank switches to newest window)
     * @return the handle of the switched window
     * @throws IllegalArgumentException if no matching window is found or index is out of bounds
     */
    public static String switchWindow(final WebDriver driver, final String param)
    {
        if (driver == null)
        {
            throw new IllegalArgumentException("WebDriver must not be null.");
        }
        final Set<String> handles = driver.getWindowHandles();
        final List<String> handleList = new ArrayList<>(handles);
        if (handleList.isEmpty())
        {
            throw new IllegalStateException("No open browser windows found.");
        }

        String currentHandle = null;
        try
        {
            currentHandle = driver.getWindowHandle();
        }
        catch (final Exception ignored)
        {
        }

        final String cleanParam = (param != null) ? param.trim() : "";

        if (cleanParam.isEmpty())
        {
            // Switch to the newest window that is not the current active window
            for (int i = handleList.size() - 1; i >= 0; i--)
            {
                final String handle = handleList.get(i);
                if (!handle.equals(currentHandle))
                {
                    driver.switchTo().window(handle);
                    return handle;
                }
            }
            // If only one window exists, ensure driver is focused on it
            driver.switchTo().window(handleList.get(0));
            return handleList.get(0);
        }

        // Direct handle match
        if (handles.contains(cleanParam))
        {
            driver.switchTo().window(cleanParam);
            return cleanParam;
        }

        // Window index match (win_1, 1, win_0, 0)
        Integer index = null;
        if (cleanParam.startsWith("win_"))
        {
            try
            {
                index = Integer.parseInt(cleanParam.substring(4));
            }
            catch (final NumberFormatException ignored)
            {
            }
        }
        if (index == null)
        {
            try
            {
                index = Integer.parseInt(cleanParam);
            }
            catch (final NumberFormatException ignored)
            {
            }
        }

        if (index != null)
        {
            if (index >= 0 && index < handleList.size())
            {
                final String targetHandle = handleList.get(index);
                driver.switchTo().window(targetHandle);
                return targetHandle;
            }
            else
            {
                throw new IllegalArgumentException("Window index out of bounds: " + index);
            }
        }

        // Treat parameter as window title or URL substring/regex
        final String regexParam = cleanRegexPattern(cleanParam);
        Pattern pattern = null;
        try
        {
            pattern = Pattern.compile(regexParam, Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        }
        catch (final PatternSyntaxException e)
        {
            pattern = Pattern.compile(Pattern.quote(regexParam), Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        }

        for (final String handle : handleList)
        {
            driver.switchTo().window(handle);
            final String title = driver.getTitle();
            final String url = driver.getCurrentUrl();
            if ((title != null && (pattern.matcher(title).find() || title.contains(cleanParam) || title.contains(regexParam))) ||
                (url != null && (pattern.matcher(url).find() || url.contains(cleanParam) || url.contains(regexParam))))
            {
                return handle;
            }
        }

        // Fallback back to original window handle if still valid
        if (currentHandle != null && handles.contains(currentHandle))
        {
            driver.switchTo().window(currentHandle);
        }
        throw new IllegalArgumentException("No window found with title or URL matching: " + cleanParam);
    }

    private static AiTool createListTabsTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");

        final ToolDefinition def = new ToolDefinition("browser_list_tabs", "Lists all open browser tabs/windows with their handle ID, index, title, URL, and whether it is currently active.", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    return ToolResult.error(call.callId(), errorNode("Browser/WebDriver is not started yet.").toString());
                }
                final WebDriver driver = WebDriverRunner.getWebDriver();
                final Set<String> handles = driver.getWindowHandles();
                String currentHandle = null;
                try
                {
                    currentHandle = driver.getWindowHandle();
                }
                catch (final Exception e)
                {
                    if (!handles.isEmpty())
                    {
                        currentHandle = handles.iterator().next();
                        driver.switchTo().window(currentHandle);
                    }
                }

                final ArrayNode tabsArray = MAPPER.createArrayNode();
                int index = 0;
                for (final String handle : handles)
                {
                    final ObjectNode tabNode = MAPPER.createObjectNode();
                    tabNode.put("index", index);
                    tabNode.put("handle", handle);
                    tabNode.put("active", handle.equals(currentHandle));
                    try
                    {
                        driver.switchTo().window(handle);
                        tabNode.put("title", driver.getTitle() != null ? driver.getTitle() : "");
                        tabNode.put("url", driver.getCurrentUrl() != null ? driver.getCurrentUrl() : "");
                    }
                    catch (final Exception e)
                    {
                        tabNode.put("title", "");
                        tabNode.put("url", "");
                    }
                    tabsArray.add(tabNode);
                    index++;
                }

                if (currentHandle != null && handles.contains(currentHandle))
                {
                    try
                    {
                        driver.switchTo().window(currentHandle);
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                final ObjectNode res = successNode("list_tabs");
                res.set("tabs", tabsArray);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createSwitchTabTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("target").put("type", "string").put("description", "Target window handle, index (e.g. '0', '1', 'win_1'), or title/URL substring. If omitted or empty, switches to the newest window.");

        final ToolDefinition def = new ToolDefinition("browser_switch_tab", "Switches the active browser focus to another tab or window by handle, index, or title/URL substring. If target is omitted, switches to the newest window.", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    return ToolResult.error(call.callId(), errorNode("Browser/WebDriver is not started yet.").toString());
                }
                final WebDriver driver = WebDriverRunner.getWebDriver();
                final String target = call.arguments().hasNonNull("target") ? call.arguments().path("target").asText().trim() : null;
                try
                {
                    final String switchedHandle = switchWindow(driver, target);
                    final ObjectNode res = successNode("switch_tab");
                    res.put("target", target != null ? target : "");
                    res.put("activeHandle", switchedHandle);
                    res.put("title", getSafeTitle(driver));
                    res.put("url", getSafeUrl(driver));
                    return ToolResult.success(call.callId(), res.toString());
                }
                catch (final Exception e)
                {
                    return ToolResult.error(call.callId(), errorNode("Failed to switch tab: " + e.getMessage()).toString());
                }
            }
        };
    }

    private static AiTool createCloseTabTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");

        final ToolDefinition def = new ToolDefinition("browser_close_tab", "Closes the current active browser tab or window, and automatically switches focus back to the parent/primary tab.", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    return ToolResult.error(call.callId(), errorNode("Browser/WebDriver is not started yet.").toString());
                }
                final WebDriver driver = WebDriverRunner.getWebDriver();
                final Set<String> handles = driver.getWindowHandles();
                final List<String> handleList = new ArrayList<>(handles);
                if (handleList.isEmpty())
                {
                    return ToolResult.error(call.callId(), errorNode("No open browser windows found.").toString());
                }

                String currentHandle = null;
                try
                {
                    currentHandle = driver.getWindowHandle();
                }
                catch (final Exception ignored)
                {
                }

                if (handleList.size() == 1)
                {
                    driver.close();
                    final ObjectNode res = successNode("close_tab");
                    res.put("message", "Closed the only open window; browser session ended.");
                    return ToolResult.success(call.callId(), res.toString());
                }

                String targetHandle = null;
                for (final String h : handleList)
                {
                    if (!h.equals(currentHandle))
                    {
                        targetHandle = h;
                        break;
                    }
                }

                driver.close();
                if (targetHandle != null)
                {
                    driver.switchTo().window(targetHandle);
                }

                final ObjectNode res = successNode("close_tab");
                res.put("activeHandle", targetHandle != null ? targetHandle : "");
                res.put("title", getSafeTitle(driver));
                res.put("url", getSafeUrl(driver));
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    /**
     * Checks if a native modal alert or dialog is currently present, and returns its message text.
     *
     * @param driver the active WebDriver instance
     * @return the alert message text, or null if no alert is open
     */
    public static String getActiveAlertText(final WebDriver driver)
    {
        if (driver == null)
        {
            return null;
        }
        try
        {
            return driver.switchTo().alert().getText();
        }
        catch (final Exception ignored)
        {
            return null;
        }
    }

    /**
     * Returns true if a native modal browser alert/confirm/prompt is currently open.
     *
     * @param driver the active WebDriver instance
     * @return true if an alert is active, false otherwise
     */
    public static boolean isAlertPresent(final WebDriver driver)
    {
        return getActiveAlertText(driver) != null;
    }

    /**
     * Verifies that the WebDriver instance is currently focused on an active, open window.
     * If the active window was closed (e.g. via {@code window.close()} or a button click),
     * automatically refocuses to the first available window.
     *
     * @param driver the active WebDriver instance
     */
    public static void ensureValidWindowFocus(final WebDriver driver)
    {
        if (driver == null || isAlertPresent(driver))
        {
            return;
        }
        try
        {
            driver.getWindowHandle();
        }
        catch (final Exception e)
        {
            try
            {
                final Set<String> handles = driver.getWindowHandles();
                if (handles != null && !handles.isEmpty())
                {
                    final String targetHandle = handles.iterator().next();
                    driver.switchTo().window(targetHandle);
                    LOGGER.debug("Active window was closed. Refocused WebDriver to window: {}", targetHandle);
                }
            }
            catch (final Exception ignored)
            {
            }
        }
    }

    /**
     * Safely retrieves the current URL from WebDriver, automatically refocusing if the active window was closed.
     *
     * @param driver the active WebDriver instance
     * @return the current URL or empty string on failure
     */
    public static String getSafeUrl(final WebDriver driver)
    {
        if (driver == null || isAlertPresent(driver))
        {
            return "";
        }
        ensureValidWindowFocus(driver);
        try
        {
            final String url = driver.getCurrentUrl();
            return url != null ? url : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    /**
     * Safely retrieves the current page title from WebDriver, automatically refocusing if the active window was closed.
     *
     * @param driver the active WebDriver instance
     * @return the current title or empty string on failure
     */
    public static String getSafeTitle(final WebDriver driver)
    {
        if (driver == null || isAlertPresent(driver))
        {
            return "";
        }
        ensureValidWindowFocus(driver);
        try
        {
            final String title = driver.getTitle();
            return title != null ? title : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    private static AiTool createUploadFileTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS selector, XPath, or container selector (such as a dropzone div). If omitted, targets the first file input on the page.");
        props.putObject("filePath").put("type", "string").put("description", "Absolute file path, relative file path, classpath resource path, or filename of the file to upload.");
        final ArrayNode required = schema.putArray("required");
        required.add("filePath");

        final ToolDefinition def = new ToolDefinition("browser_upload_file", "Uploads a local file to the targeted file input element or styled dropzone container without opening native OS dialogs. Automatically discovers nested or associated <input type='file'> elements.", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    return ToolResult.error(call.callId(), errorNode("Browser/WebDriver is not started yet.").toString());
                }

                final String selector = call.arguments().hasNonNull("selector") ? call.arguments().path("selector").asText().trim() : null;
                final String rawFilePath = call.arguments().hasNonNull("filePath") ? call.arguments().path("filePath").asText().trim()
                        : call.arguments().hasNonNull("file") ? call.arguments().path("file").asText().trim()
                        : call.arguments().hasNonNull("path") ? call.arguments().path("path").asText().trim()
                        : call.arguments().hasNonNull("value") ? call.arguments().path("value").asText().trim() : null;

                if (rawFilePath == null || rawFilePath.isBlank())
                {
                    return ToolResult.error(call.callId(), errorNode("Missing required parameter 'filePath'.").toString());
                }

                try
                {
                    final File resolvedFile = resolveUploadFile(rawFilePath);
                    final SelenideElement targetInput = resolveFileInput(selector);

                    if (targetInput == null || !targetInput.exists())
                    {
                        return ToolResult.error(call.callId(), errorNode("No <input type='file'> element found matching or within selector: " + selector).toString());
                    }

                    targetInput.uploadFile(resolvedFile);

                    // Ensure change and input events fire even if browser upload didn't trigger them automatically
                    try
                    {
                        Selenide.executeJavaScript("arguments[0].dispatchEvent(new Event('input', { bubbles: true }));"
                                + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", targetInput);
                    }
                    catch (final Exception ignored)
                    {
                    }

                    final WebDriver driver = WebDriverRunner.getWebDriver();
                    final ObjectNode res = successNode("upload_file");
                    res.put("selector", selector != null ? selector : "input[type='file']");
                    res.put("fileName", resolvedFile.getName());
                    res.put("fileSize", resolvedFile.length());
                    res.put("url", getSafeUrl(driver));
                    res.put("title", getSafeTitle(driver));
                    return ToolResult.success(call.callId(), res.toString());
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Failed executing browser_upload_file: {}", e.getMessage());
                    return ToolResult.error(call.callId(), errorNode("Failed uploading file: " + e.getMessage()).toString());
                }
            }
        };
    }

    private static AiTool createHandleAlertTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("action").put("type", "string").put("description", "Action to perform on the dialog: 'accept' (OK/Confirm) or 'dismiss' (Cancel). Defaults to 'accept'.");
        props.putObject("promptText").put("type", "string").put("description", "Optional text string to enter into a prompt() dialog before accepting.");

        final ToolDefinition def = new ToolDefinition("browser_handle_alert", "Interacts with and resolves native browser modal dialogs (window.alert, window.confirm, window.prompt)", schema);
        return new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    return ToolResult.error(call.callId(), errorNode("Browser/WebDriver is not started yet.").toString());
                }

                final WebDriver driver = WebDriverRunner.getWebDriver();
                final Alert alert;
                try
                {
                    alert = driver.switchTo().alert();
                }
                catch (final NoAlertPresentException e)
                {
                    return ToolResult.error(call.callId(), errorNode("No active browser alert or dialog found.").toString());
                }
                catch (final Exception e)
                {
                    return ToolResult.error(call.callId(), errorNode("Failed to inspect alert: " + e.getMessage()).toString());
                }

                try
                {
                    final String alertText = alert.getText();
                    final JsonNode args = call.arguments();
                    final String rawAction = args.hasNonNull("action") ? args.path("action").asText().trim()
                            : (args.hasNonNull("value") && args.path("value").asText().trim().matches("(?i)^(accept|dismiss|ok|cancel)$"))
                                    ? args.path("value").asText().trim()
                                    : (args.hasNonNull("text") && args.path("text").asText().trim().matches("(?i)^(accept|dismiss|ok|cancel)$"))
                                            ? args.path("text").asText().trim()
                                            : "accept";
                    final String action = ("dismiss".equalsIgnoreCase(rawAction) || "cancel".equalsIgnoreCase(rawAction)) ? "dismiss" : "accept";

                    final String candidatePrompt = args.hasNonNull("promptText") ? args.path("promptText").asText()
                            : (args.hasNonNull("prompt") ? args.path("prompt").asText()
                                    : (args.hasNonNull("target") && !args.path("target").asText().trim().matches("(?i)^(accept|dismiss|ok|cancel|alert|confirm|prompt)$"))
                                            ? args.path("target").asText()
                                            : (args.hasNonNull("locator") && !args.path("locator").asText().trim().matches("(?i)^(accept|dismiss|ok|cancel|alert|confirm|prompt)$"))
                                                    ? args.path("locator").asText()
                                                    : (args.hasNonNull("text") && !args.path("text").asText().trim().matches("(?i)^(accept|dismiss|ok|cancel)$"))
                                                            ? args.path("text").asText()
                                                            : (args.hasNonNull("value") && !args.path("value").asText().trim().matches("(?i)^(accept|dismiss|ok|cancel)$"))
                                                                    ? args.path("value").asText()
                                                                    : null);
                    final String promptText = (candidatePrompt != null && !candidatePrompt.isBlank()) ? candidatePrompt : null;

                    if (promptText != null)
                    {
                        alert.sendKeys(promptText);
                    }

                    if ("dismiss".equals(action))
                    {
                        alert.dismiss();
                    }
                    else
                    {
                        alert.accept();
                    }

                    final ObjectNode res = successNode("handle_alert");
                    res.put("alertText", alertText);
                    res.put("action", action);
                    if (promptText != null)
                    {
                        res.put("promptText", promptText);
                    }
                    return ToolResult.success(call.callId(), res.toString());
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Failed resolving native browser alert: {}", e.getMessage());
                    return ToolResult.error(call.callId(), errorNode("Failed resolving browser dialog: " + e.getMessage()).toString());
                }
            }
        };
    }

    /**
     * Resolves the target {@code <input type="file">} element given a selector which may point directly to
     * a file input, a styled dropzone container, a form, or an associated label.
     *
     * @param selector the CSS selector, XPath, or null/empty to target the first file input
     * @return the resolved SelenideElement representing the file input
     */
    public static SelenideElement resolveFileInput(final String selector)
    {
        if (selector == null || selector.isBlank())
        {
            return $("input[type='file']");
        }

        final SelenideElement element = $(selector);
        if (!element.exists())
        {
            // Try SelenideElementFinder for badge or fuzzy selector
            try
            {
                final SelenideElement found = SelenideElementFinder.findElement(selector);
                if (found != null && found.exists())
                {
                    return resolveFromCandidate(found);
                }
            }
            catch (final Exception ignored)
            {
            }
            return $("input[type='file']");
        }

        return resolveFromCandidate(element);
    }

    private static SelenideElement resolveFromCandidate(final SelenideElement element)
    {
        final String tagName = element.getTagName();
        if ("input".equalsIgnoreCase(tagName) && "file".equalsIgnoreCase(element.getAttribute("type")))
        {
            return element;
        }

        // Check if element contains an input[type='file'] descendant
        final SelenideElement descendant = element.find("input[type='file']");
        if (descendant.exists())
        {
            return descendant;
        }

        // Check label 'for' attribute
        final String forAttr = element.getAttribute("for");
        if (forAttr != null && !forAttr.isBlank())
        {
            final SelenideElement forElement = $("#" + forAttr);
            if (forElement.exists() && "input".equalsIgnoreCase(forElement.getTagName()) && "file".equalsIgnoreCase(forElement.getAttribute("type")))
            {
                return forElement;
            }
        }

        // Check closest container / parent for a file input
        try
        {
            final SelenideElement parentInput = element.closest("form, div, section, fieldset").find("input[type='file']");
            if (parentInput.exists())
            {
                return parentInput;
            }
        }
        catch (final Exception ignored)
        {
        }

        // Fallback to first file input on the page
        final SelenideElement pageInput = $("input[type='file']");
        return pageInput.exists() ? pageInput : element;
    }

    /**
     * Resolves a file path string to an existing File on disk.
     * Supports absolute paths, workspace-relative paths, classpath resources,
     * and synthetic temporary test files if the file does not already exist.
     *
     * @param rawFilePath the input file path or filename
     * @return the resolved File instance guaranteed to exist
     * @throws IOException if temporary file creation fails
     */
    public static File resolveUploadFile(final String rawFilePath) throws IOException
    {
        if (rawFilePath == null || rawFilePath.isBlank())
        {
            throw new IllegalArgumentException("File path must not be null or empty.");
        }

        final String cleanPath = rawFilePath.trim();

        // 1. Direct absolute or relative file on disk
        final File directFile = new File(cleanPath);
        if (directFile.exists() && directFile.isFile())
        {
            return directFile;
        }

        // 2. Relative to user working directory
        final File userDirFile = new File(System.getProperty("user.dir"), cleanPath);
        if (userDirFile.exists() && userDirFile.isFile())
        {
            return userDirFile;
        }

        // 3. Classpath resource
        final URL resource = BrowserToolProvider.class.getClassLoader().getResource(cleanPath);
        if (resource != null)
        {
            try
            {
                final File resFile = new File(resource.toURI());
                if (resFile.exists() && resFile.isFile())
                {
                    return resFile;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        // 4. Synthetic temporary mock file fallback (isolated directory preserving exact filename)
        final String baseName = Paths.get(cleanPath).getFileName().toString();
        final Path tempDir = Files.createTempDirectory("neo_upload_");
        final Path targetPath = tempDir.resolve(baseName);
        final File tempFile = targetPath.toFile();
        tempFile.deleteOnExit();
        tempDir.toFile().deleteOnExit();
        Files.writeString(targetPath, "Synthetic upload payload for " + baseName + "\nGenerated by Neodymium Aura AI Test Engine.");
        LOGGER.info("Created synthetic upload file for '{}' at {}", baseName, tempFile.getAbsolutePath());
        return tempFile;
    }
}
