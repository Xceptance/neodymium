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
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.ai.executor.selenide.PageAnalyzer;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.neodymium.ai.model.ContextLevel;
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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
        registry.register(createAssertTextTool());
        registry.register(createScrollTool());
        registry.register(createExecuteScriptTool());
        registry.register(createQueryDomTool());
        registry.register(createInspectTool());
        registry.register(createTakeScreenshotTool());
        registry.register(createInspectVisualTool());
        registry.register(createPressKeyTool());
        registry.register(createRequestContextTool());
    }

    private static AiTool createClickTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS or XPath selector of the element to click");
        props.putObject("text").put("type", "string").put("description", "Visible text of the element to click (used if selector is omitted)");
        props.putObject("target").put("type", "string").put("description", "Target expression, such as 'badge:N' or 'coord: x,y'");
        props.putObject("x").put("type", "integer").put("description", "Viewport X coordinate for pixel/visual click");
        props.putObject("y").put("type", "integer").put("description", "Viewport Y coordinate for pixel/visual click");

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

                // Case 1: Viewport coordinates provided
                if (args.hasNonNull("x") && args.hasNonNull("y") && driver != null)
                {
                    final int x = args.path("x").asInt();
                    final int y = args.path("y").asInt();

                    new Actions(driver).moveToLocation(x, y).click().perform();

                    final ReanchoringBridge.ReanchoredElement reanchored = ReanchoringBridge.resolveElementAtPoint(driver, x, y);
                    final ToolResult.Builder builder = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS);
                    final ObjectNode res = successNode("click");
                    res.put("x", x);
                    res.put("y", y);

                    if (reanchored != null)
                    {
                        res.put("selector", reanchored.selector());
                        builder.withVariable("reanchoredSelector", reanchored.selector());
                        builder.withVariable("reanchoredFeatureVector", reanchored.domFeatureVector());
                    }
                    if (driver != null)
                    {
                        res.put("url", driver.getCurrentUrl());
                        res.put("title", driver.getTitle());
                    }
                    builder.withContent(res.toString());
                    return builder.build();
                }

                // Case 2: Target is badge:N
                final String target = args.hasNonNull("target") ? args.path("target").asText() : "";
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
                            res.put("url", driver.getCurrentUrl());
                            res.put("title", driver.getTitle());
                        }
                        b.withContent(res.toString());
                        return b.build();
                    }
                }

                // Case 3: Element resolution via selector and text fallback
                final String text = args.hasNonNull("text") ? args.path("text").asText().trim() : "";
                final String selector = args.hasNonNull("selector") ? args.path("selector").asText() : target;

                if (!selector.isBlank())
                {
                    try
                    {
                        final SelenideElement el = findElement(selector);
                        if (el.is(Condition.visible))
                        {
                            el.click();
                            final ObjectNode res = successNode("click");
                            res.put("target", selector);
                            if (driver != null)
                            {
                                res.put("url", driver.getCurrentUrl());
                                res.put("title", driver.getTitle());
                            }
                            return ToolResult.success(call.callId(), res.toString());
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                if (!text.isBlank())
                {
                    try
                    {
                        final SelenideElement el = $(Selectors.byText(text)).is(Condition.visible)
                                ? $(Selectors.byText(text))
                                : $(Selectors.withText(text));
                        if (el.is(Condition.visible))
                        {
                            el.click();
                            final ObjectNode res = successNode("click");
                            res.put("text", text);
                            if (driver != null)
                            {
                                res.put("url", driver.getCurrentUrl());
                                res.put("title", driver.getTitle());
                            }
                            return ToolResult.success(call.callId(), res.toString());
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }

                final SelenideElement el;
                if (selector.startsWith("text="))
                {
                    final String rawText = selector.substring("text=".length()).trim();
                    el = $(Selectors.byText(rawText)).is(Condition.visible)
                            ? $(Selectors.byText(rawText))
                            : $(Selectors.withText(rawText));
                }
                else
                {
                    if (selector.isBlank() && text.isBlank())
                    {
                        return ToolResult.error(call.callId(), errorNode("browser_click requires either 'selector', 'text', 'coordinates' (x, y), or 'target'").toString());
                    }
                    el = findElement(selector);
                }

                if (!el.is(Condition.visible))
                {
                    try
                    {
                        el.scrollIntoView(true);
                    }
                    catch (final Exception ignored)
                    {
                    }
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
                }

                el.shouldBe(Condition.visible).click();
                final ObjectNode res = successNode("click");
                res.put("target", selector);
                if (driver != null)
                {
                    res.put("url", driver.getCurrentUrl());
                    res.put("title", driver.getTitle());
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
        props.putObject("pressEnter").put("type", "boolean").put("description", "Whether to press Enter key after typing (default: false)");

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
                final String selector = resolveSelector(call.arguments());
                final String text = call.arguments().path("text").asText();
                final boolean clearFirst = !call.arguments().has("clearFirst") || call.arguments().path("clearFirst").asBoolean(true);
                final boolean pressEnter = call.arguments().path("pressEnter").asBoolean(false);

                final SelenideElement el = findElement(selector).shouldBe(Condition.visible);
                if (clearFirst)
                {
                    el.clear();
                }
                el.sendKeys(text);
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
                final String url = call.arguments().path("url").asText();
                Selenide.open(url);
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                final ObjectNode res = successNode("navigate");
                res.put("url", driver != null ? driver.getCurrentUrl() : url);
                res.put("title", driver != null ? driver.getTitle() : "");
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
                final String selector = resolveSelector(call.arguments());
                final SelenideElement el = $(selector).shouldBe(Condition.visible);

                final ObjectNode res = successNode("select");
                res.put("target", selector);
                if (call.arguments().hasNonNull("value"))
                {
                    final String val = call.arguments().path("value").asText();
                    el.selectOptionByValue(val);
                    res.put("value", val);
                }
                else if (call.arguments().hasNonNull("text"))
                {
                    final String txt = call.arguments().path("text").asText();
                    el.selectOption(txt);
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

    private static AiTool createAssertTextTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Selector of the element to assert text on");
        props.putObject("expectedText").put("type", "string").put("description", "Expected text content (plain substring) or regex pattern if regex is true");
        props.putObject("exact").put("type", "boolean").put("description", "Whether text match must be exact (default: false)");
        props.putObject("regex").put("type", "boolean").put("description", "Whether expectedText is a regular expression pattern (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");
        req.add("expectedText");

        final ToolDefinition def = new ToolDefinition("browser_assert_text", "Asserts that an element contains or exactly matches the expected text or pattern", schema);
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
                final String expectedText = regex ? rawExpectedText : unescapeLiteralText(rawExpectedText);

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
                    if (regex)
                    {
                        if (!Pattern.compile(expectedText, Pattern.DOTALL).matcher(pageTitle != null ? pageTitle : "").find())
                        {
                            throw new AssertionError("Page title \"" + pageTitle + "\" does not match regex pattern \"" + expectedText + "\"");
                        }
                    }
                    else if (exact)
                    {
                        if (!expectedText.equals(pageTitle))
                        {
                            throw new AssertionError("Page title \"" + pageTitle + "\" does not exactly match \"" + expectedText + "\"");
                        }
                    }
                    else
                    {
                        if (pageTitle == null || !pageTitle.contains(expectedText))
                        {
                            throw new AssertionError("Page title \"" + pageTitle + "\" does not contain expected text \"" + expectedText + "\"");
                        }
                    }
                }
                else
                {
                    final SelenideElement el = (selector == null || selector.isBlank()) ? $("body") : findElement(selector);
                    final String tagName = el.getTagName().toLowerCase();
                    final boolean isInputOrTextarea = "input".equals(tagName) || "textarea".equals(tagName);

                    if (regex)
                    {
                        if (isInputOrTextarea)
                        {
                            el.shouldHave(Condition.or("Text, value, or placeholder matching pattern",
                                    Condition.matchText(expectedText),
                                    Condition.attributeMatching("value", expectedText),
                                    Condition.attributeMatching("placeholder", expectedText)));
                        }
                        else
                        {
                            el.shouldHave(Condition.matchText(expectedText));
                        }
                    }
                    else if (exact)
                    {
                        if (isInputOrTextarea)
                        {
                            el.shouldHave(Condition.or("Exact text, value, or placeholder",
                                    Condition.exactText(expectedText),
                                    Condition.exactValue(expectedText),
                                    Condition.attribute("placeholder", expectedText)));
                        }
                        else
                        {
                            el.shouldHave(Condition.exactText(expectedText));
                        }
                    }
                    else
                    {
                        if (isInputOrTextarea)
                        {
                            el.shouldHave(Condition.or("Text, value, or placeholder containing string",
                                    Condition.text(expectedText),
                                    Condition.value(expectedText),
                                    Condition.attribute("placeholder", expectedText)));
                        }
                        else
                        {
                            el.shouldHave(Condition.text(expectedText));
                        }
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

    private static AiTool createScrollTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("direction").put("type", "string").put("description", "Direction to scroll ('down', 'up', 'top', 'bottom')");
        props.putObject("selector").put("type", "string").put("description", "Optional element selector to scroll into view");
        props.putObject("yOffset").put("type", "integer").put("description", "Optional pixel distance to scroll vertically");

        final ToolDefinition def = new ToolDefinition("browser_scroll", "Scrolls the page viewport or scrolls a specific element into view", schema);
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
                if (args.hasNonNull("selector"))
                {
                    final String sel = args.path("selector").asText();
                    findElement(sel).scrollIntoView(true);
                    final ObjectNode res = successNode("scroll");
                    res.put("target", sel);
                    return ToolResult.success(call.callId(), res.toString());
                }

                final String direction = args.path("direction").asText("down").toLowerCase();
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
                    Selenide.executeJavaScript("window.scrollBy(0, -Math.round(window.innerHeight * 0.8));");
                }
                else
                {
                    final int yOffset = args.hasNonNull("yOffset") ? args.path("yOffset").asInt() : 0;
                    if (yOffset != 0)
                    {
                        Selenide.executeJavaScript("window.scrollBy(0, arguments[0]);", yOffset);
                    }
                    else
                    {
                        Selenide.executeJavaScript("window.scrollBy(0, Math.round(window.innerHeight * 0.8));");
                    }
                }
                final ObjectNode res = successNode("scroll");
                res.put("direction", direction);
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
                try
                {
                    rootNode.set("matches", MAPPER.readTree(resStr));
                }
                catch (final Exception e)
                {
                    rootNode.putArray("matches");
                }
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

                    return ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                            .withContent(res.toString())
                            .withArtifact("crop_" + selector, "image/png", cropBytes)
                            .withVariable("cropBase64", "data:image/png;base64," + base64)
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
                .put("description", "Depth and scope of the context to capture ('LEAN', 'STANDARD', 'RICH', 'VISUAL', 'VISUAL_RICH')")
                .putArray("enum");
        levelEnum.add("LEAN");
        levelEnum.add("STANDARD");
        levelEnum.add("RICH");
        levelEnum.add("VISUAL");
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
}
