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
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
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
    }

    private static AiTool createClickTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS or XPath selector of the element to click");
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

                    if (reanchored != null)
                    {
                        builder.withContent("Clicked at coordinates (" + x + ", " + y + ") [re-anchored to " + reanchored.selector() + "]");
                        builder.withVariable("reanchoredSelector", reanchored.selector());
                        builder.withVariable("reanchoredFeatureVector", reanchored.domFeatureVector());
                    }
                    else
                    {
                        builder.withContent("Clicked at coordinates (" + x + ", " + y + ")");
                    }
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
                        if (reanchored != null)
                        {
                            b.withContent("Clicked Set-of-Marks badge [" + badgeNum + "] [re-anchored to " + reanchored.selector() + "]");
                            b.withVariable("reanchoredSelector", reanchored.selector());
                        }
                        else
                        {
                            b.withContent("Clicked Set-of-Marks badge [" + badgeNum + "]");
                        }
                        return b.build();
                    }
                }

                // Case 3: Standard CSS or XPath selector
                final String selector = args.hasNonNull("selector") ? args.path("selector").asText() : target;
                if (selector.isBlank())
                {
                    return ToolResult.error(call.callId(), "browser_click requires either 'selector', 'coordinates' (x, y), or 'target'");
                }

                $(selector).shouldBe(Condition.visible).click();
                return ToolResult.success(call.callId(), "Clicked element: " + selector);
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
                final String selector = call.arguments().path("selector").asText();
                final String text = call.arguments().path("text").asText();
                final boolean clearFirst = !call.arguments().has("clearFirst") || call.arguments().path("clearFirst").asBoolean(true);

                final SelenideElement el = $(selector).shouldBe(Condition.visible);
                if (clearFirst)
                {
                    el.clear();
                }
                el.sendKeys(text);
                return ToolResult.success(call.callId(), "Typed text into " + selector);
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
                return ToolResult.success(call.callId(), "Navigated to " + url);
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
                final String selector = call.arguments().path("selector").asText();
                final SelenideElement el = $(selector).shouldBe(Condition.visible);

                if (call.arguments().hasNonNull("value"))
                {
                    el.selectOptionByValue(call.arguments().path("value").asText());
                }
                else if (call.arguments().hasNonNull("text"))
                {
                    el.selectOption(call.arguments().path("text").asText());
                }
                return ToolResult.success(call.callId(), "Selected option in " + selector);
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
                final String selector = call.arguments().path("selector").asText();
                $(selector).shouldBe(Condition.visible).hover();
                return ToolResult.success(call.callId(), "Hovered over " + selector);
            }
        };
    }

    private static AiTool createAssertTextTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Selector of the element to assert text on");
        props.putObject("expectedText").put("type", "string").put("description", "Expected text content");
        props.putObject("exact").put("type", "boolean").put("description", "Whether text match must be exact (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");
        req.add("expectedText");

        final ToolDefinition def = new ToolDefinition("browser_assert_text", "Asserts that an element contains or exactly matches the expected text", schema);
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
                final String expectedText = call.arguments().path("expectedText").asText();
                final boolean exact = call.arguments().path("exact").asBoolean(false);

                if (exact)
                {
                    $(selector).shouldHave(Condition.exactText(expectedText));
                }
                else
                {
                    $(selector).shouldHave(Condition.text(expectedText));
                }
                return ToolResult.success(call.callId(), "Verified text on " + selector + " matches: " + expectedText);
            }
        };
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
                    $(sel).scrollIntoView(true);
                    return ToolResult.success(call.callId(), "Scrolled element into view: " + sel);
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
                return ToolResult.success(call.callId(), "Scrolled viewport: " + direction);
            }
        };
    }

    private static AiTool createExecuteScriptTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("script").put("type", "string").put("description", "JavaScript code to execute in browser context");
        props.putObject("args").put("type", "array").put("description", "Optional arguments passed to the script");
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
                final Object result = Selenide.executeJavaScript(script);
                return ToolResult.success(call.callId(), result != null ? result.toString() : "null");
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
                            candidates = Array.from(document.querySelectorAll('a, button, input, select, textarea, [role], h1, h2, h3, h4, p, span, div'));
                        }

                        var results = [];
                        var lowerText = (searchText || '').toLowerCase().trim();

                        for (var i = 0; i < candidates.length; i++) {
                            var el = candidates[i];
                            var elText = (el.innerText || el.textContent || '').trim();
                            if (lowerText && elText.toLowerCase().indexOf(lowerText) === -1) continue;

                            var rect = el.getBoundingClientRect();
                            var inViewport = rect.top < window.innerHeight && rect.bottom > 0 && rect.left < window.innerWidth && rect.right > 0;
                            var tag = el.tagName.toLowerCase();
                            var idStr = el.id ? '#' + el.id : '';
                            var clsStr = el.className && typeof el.className === 'string' ? '.' + el.className.trim().split(/\\s+/).slice(0, 2).join('.') : '';

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
                return ToolResult.success(call.callId(), res != null ? res.toString() : "[]");
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
                    return ToolResult.error(call.callId(), "Element not found for selector: " + selector);
                }
                return ToolResult.success(call.callId(), result.toString());
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
                    return ToolResult.error(call.callId(), "WebDriver is not running; cannot capture screenshot");
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
                final ToolResult.Builder builder = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                        .withContent("Screenshot captured successfully (" + screenshotBytes.length + " bytes)")
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
                    return ToolResult.error(call.callId(), "WebDriver not started");
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

                    return ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                            .withContent("Visual crop captured for " + selector + " (" + w + "x" + h + " px)")
                            .withArtifact("crop_" + selector, "image/png", cropBytes)
                            .withVariable("cropBase64", "data:image/png;base64," + base64)
                            .build();
                }

                return ToolResult.error(call.callId(), "Could not resolve bounding box for selector: " + selector);
            }
        };
    }
}
