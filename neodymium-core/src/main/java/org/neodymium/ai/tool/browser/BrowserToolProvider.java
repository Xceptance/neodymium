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
import org.neodymium.ai.executor.selenide.plugins.ClickAction;
import org.neodymium.ai.model.DomFeatureVector;
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
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
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
        registry.register(createFillTool());
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
        registry.register(createAssertUrlTool());
        registry.register(createAssertTitleTool());
        registry.register(createAssertElementStateTool());
        registry.register(createAssertAttributeTool());
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
        registry.register(createDragTool());
        registry.register(createDragToTool());
    }

    private static int[] performSafeCoordinateClick(final WebDriver driver, final int targetX, final int targetY)
    {
        int x = targetX;
        int y = targetY;

        int vpWidth = 1280;
        int vpHeight = 800;

        try
        {
            final String vpScript = """
                return {
                    width: window.innerWidth || document.documentElement.clientWidth || 0,
                    height: window.innerHeight || document.documentElement.clientHeight || 0
                };
                """;
            final Object vpObj = ((JavascriptExecutor) driver).executeScript(vpScript);
            if (vpObj instanceof Map<?, ?> vpMap)
            {
                final int w = ((Number) vpMap.get("width")).intValue();
                final int h = ((Number) vpMap.get("height")).intValue();
                if (w > 0)
                {
                    vpWidth = w;
                }
                if (h > 0)
                {
                    vpHeight = h;
                }
            }
        }
        catch (final Exception ignored)
        {
            try
            {
                final Dimension winSize = driver.manage().window().getSize();
                vpWidth = winSize.getWidth();
                vpHeight = winSize.getHeight();
            }
            catch (final Exception ignoredWin)
            {
            }
        }

        int deltaX = 0;
        if (x < 0 || x >= vpWidth)
        {
            deltaX = x - (vpWidth / 2);
        }
        int deltaY = 0;
        if (y < 0 || y >= vpHeight)
        {
            deltaY = y - (vpHeight / 2);
        }

        if (deltaX != 0 || deltaY != 0)
        {
            try
            {
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(arguments[0], arguments[1]);", deltaX, deltaY);
                x -= deltaX;
                y -= deltaY;
            }
            catch (final Exception ignored)
            {
            }
        }

        x = Math.max(0, Math.min(x, Math.max(0, vpWidth - 1)));
        y = Math.max(0, Math.min(y, Math.max(0, vpHeight - 1)));

        try
        {
            new Actions(driver).moveToLocation(x, y).click().perform();
        }
        catch (final Exception e)
        {
            LOGGER.warn("Actions.moveToLocation({}, {}) failed: {}. Falling back to elementFromPoint JS click.", x, y, e.getMessage());
            final String jsClickScript = """
                var el = document.elementFromPoint(arguments[0], arguments[1]);
                if (el) {
                    el.click();
                    return true;
                }
                return false;
                """;
            try
            {
                ((JavascriptExecutor) driver).executeScript(jsClickScript, x, y);
            }
            catch (final Exception jsEx)
            {
                LOGGER.error("Fallback document.elementFromPoint click failed at ({}, {})", x, y, jsEx);
                throw e;
            }
        }

        return new int[]{x, y};
    }

    private static ToolResult executeElementClick(final ToolCall call, final WebDriver driver, final String selector,
            final String text, final int parsedX, final int parsedY)
    {
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

        DomFeatureVector featureVector = null;
        if (driver != null)
        {
            try
            {
                featureVector = new PageAnalyzer(driver).extractFeatureVector(el);
            }
            catch (final Throwable ignored)
            {
            }
        }

        SelenideElement clickedEl = el;
        String actualTarget = targetDesc;

        boolean clickedViaOffset = false;
        if (driver != null && parsedX != Integer.MIN_VALUE && parsedY != Integer.MIN_VALUE && parsedX >= 0 && parsedY >= 0)
        {
            try
            {
                if (el.is(Condition.visible))
                {
                    final int elWidth = el.getSize().getWidth();
                    final int elHeight = el.getSize().getHeight();
                    if (parsedX <= elWidth && parsedY <= elHeight)
                    {
                        final int xOffset = parsedX - (elWidth / 2);
                        final int yOffset = parsedY - (elHeight / 2);
                        new Actions(driver).moveToElement(el.toWebElement(), xOffset, yOffset).click().perform();
                        clickedViaOffset = true;
                    }
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        if (!clickedViaOffset)
        {
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
                if (fallbackTargetEl != null && driver != null)
                {
                    try
                    {
                        featureVector = new PageAnalyzer(driver).extractFeatureVector(fallbackTargetEl);
                    }
                    catch (final Throwable ignored)
                    {
                    }
                }
            }
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
        if (featureVector != null)
        {
            res.set("domFeatureVector", MAPPER.valueToTree(featureVector));
        }
        if (driver != null)
        {
            res.put("url", getSafeUrl(driver));
            res.put("title", getSafeTitle(driver));
        }
        return ToolResult.success(call.callId(), res.toString());
    }

    private static ToolResult executeCoordinateClick(final ToolCall call, final WebDriver driver, final String selector,
            final int parsedX, final int parsedY)
    {
        final int rawX = parsedX;
        final int rawY = parsedY;

        int x = rawX;
        int y = rawY;

        if (!selector.isBlank())
        {
            final String rectScript = """
                var el = document.querySelector(arguments[0]);
                if (!el) return null;
                var r = el.getBoundingClientRect();
                return { x: Math.round(r.left), y: Math.round(r.top) };
                """;
            try
            {
                final Object rectObj = ((JavascriptExecutor) driver).executeScript(rectScript, selector);
                if (rectObj instanceof Map<?, ?> map)
                {
                    x = ((Number) map.get("x")).intValue() + rawX;
                    y = ((Number) map.get("y")).intValue() + rawY;
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        final int[] clicked = performSafeCoordinateClick(driver, x, y);
        final int finalX = clicked[0];
        final int finalY = clicked[1];

        final ReanchoringBridge.ReanchoredElement reanchored = ReanchoringBridge.resolveElementAtPoint(driver, finalX, finalY);
        final ToolResult.Builder builder = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS);
        final ObjectNode res = successNode("click");
        res.put("x", finalX);
        res.put("y", finalY);
        if (!selector.isBlank())
        {
            res.put("elementSelector", selector);
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

        final ToolDefinition def = new ToolDefinition("click", "Clicks an interactive element or viewport coordinate in the browser", schema);
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
                String sel = args.hasNonNull("selector") ? cleanSelector(args.path("selector").asText().trim()) : "";
                final String text = args.hasNonNull("text") ? args.path("text").asText().trim() : "";

                if ((parsedX == Integer.MIN_VALUE || parsedY == Integer.MIN_VALUE) && (target.startsWith("coord:") || target.contains("@")))
                {
                    final ClickAction.CoordinateTarget coord = ClickAction.parseCoordinateTarget(target);
                    if (coord != null)
                    {
                        parsedX = coord.x();
                        parsedY = coord.y();
                        if (sel.isBlank() && coord.anchorSelector() != null && !coord.anchorSelector().isBlank())
                        {
                            sel = cleanSelector(coord.anchorSelector());
                        }
                    }
                    else if (target.startsWith("coord:"))
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
                }

                if (sel.isBlank() && !target.isBlank() && !target.startsWith("coord:") && !target.startsWith("badge:"))
                {
                    sel = cleanSelector(target);
                }

                // Case 1: Target is badge:N
                if (target.startsWith("badge:") && driver != null)
                {
                    final String badgeNum = target.substring("badge:".length()).trim();
                    final String clickBadgeScript = """
                        var badges = document.querySelectorAll('#__neo_som_badges__ > div');
                        for (var i = 0; i < badges.length; i++) {
                            if (badges[i].innerText.trim() === arguments[0]) {
                                badges[i].scrollIntoView({ block: 'center', inline: 'center' });
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
                        final int[] clicked = performSafeCoordinateClick(driver, bx, by);
                        final int finalX = clicked[0];
                        final int finalY = clicked[1];

                        final ReanchoringBridge.ReanchoredElement reanchored = ReanchoringBridge.resolveElementAtPoint(driver, finalX, finalY);
                        final ToolResult.Builder b = ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS);
                        final ObjectNode res = successNode("click");
                        res.put("target", "badge:" + badgeNum);
                        res.put("x", finalX);
                        res.put("y", finalY);
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

                // Case 2: Element resolution via selector and text fallback (prioritized over raw coordinates unless explicit coord: target)
                final boolean hasElementTarget = (!sel.isBlank() || !text.isBlank()) && !target.startsWith("coord:");
                if (hasElementTarget)
                {
                    try
                    {
                        return executeElementClick(call, driver, sel, text, parsedX, parsedY);
                    }
                    catch (final Exception | AssertionError e)
                    {
                        if (parsedX == Integer.MIN_VALUE || parsedY == Integer.MIN_VALUE || driver == null)
                        {
                            throw e;
                        }
                        LOGGER.warn("Element click failed for '{}', falling back to coordinate click ({}, {}): {}",
                                !sel.isBlank() ? sel : text, parsedX, parsedY, e.getMessage());
                    }
                }

                // Case 3: Coordinate click (pure coordinates or explicit coord: target)
                if (parsedX != Integer.MIN_VALUE && parsedY != Integer.MIN_VALUE && driver != null)
                {
                    return executeCoordinateClick(call, driver, sel, parsedX, parsedY);
                }

                return ToolResult.error(call.callId(), errorNode("click requires either 'selector', 'text', 'coordinates' (x, y), or 'target'").toString());
            }
        };
    }

    private static AiTool createFillTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Selector of the input element");
        props.putObject("text").put("type", "string").put("description", "Text to enter into the element");
        props.putObject("pressEnter").put("type", "boolean").put("description", "Whether to press Enter key after typing (default: false). MUST remain false unless the test instruction explicitly asks to press Enter, hit Enter, or submit the form.");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");
        req.add("text");

        final ToolDefinition def = new ToolDefinition("fill", "Clears existing text and enters new text into an input or textarea element. Default tool for entering, typing, or setting form field values.", schema);
        return createBaseInputTool(def, true);
    }

    private static AiTool createTypeTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "Selector of the input element");
        props.putObject("text").put("type", "string").put("description", "Text to type into the element without clearing existing text (appends text)");
        props.putObject("pressEnter").put("type", "boolean").put("description", "Whether to press Enter key after typing (default: false). MUST remain false unless the test instruction explicitly asks to press Enter, hit Enter, or submit the form.");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");
        req.add("text");

        final ToolDefinition def = new ToolDefinition("type", "Types text into an input or textarea element without clearing existing text (appends text). Use only when intentionally appending to existing content.", schema);
        return createBaseInputTool(def, false);
    }

    private static AiTool createBaseInputTool(final ToolDefinition def, final boolean defaultClearFirst)
    {
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
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                final String selector = resolveSelector(call.arguments());
                final String text = call.arguments().path("text").asText();
                final boolean clearFirst = call.arguments().has("clearFirst")
                        ? call.arguments().path("clearFirst").asBoolean(defaultClearFirst)
                        : defaultClearFirst;
                final boolean pressEnter = call.arguments().path("pressEnter").asBoolean(false);

                final SelenideElement el = findElement(selector);
                SelenideElementFinder.scrollIntoViewIfNeeded(el);
                DomFeatureVector featureVector = null;
                if (driver != null)
                {
                    try
                    {
                        featureVector = new PageAnalyzer(driver).extractFeatureVector(el);
                    }
                    catch (final Throwable ignored)
                    {
                    }
                }
                final boolean isContentEditable = Boolean.TRUE.equals(Selenide.executeJavaScript(
                    "return !!(arguments[0] && (arguments[0].isContentEditable === true || arguments[0].getAttribute('contenteditable') === 'true' || arguments[0].hasAttribute('contenteditable')));",
                    el));

                if (isContentEditable)
                {
                    el.shouldBe(Condition.visible);
                    Selenide.executeJavaScript(
                        "var el = arguments[0];"
                        + "var clear = arguments[1];"
                        + "el.focus();"
                        + "if (clear) { el.innerHTML = ''; }"
                        + "var range = document.createRange();"
                        + "range.selectNodeContents(el);"
                        + "range.collapse(false);"
                        + "var sel = window.getSelection();"
                        + "sel.removeAllRanges();"
                        + "sel.addRange(range);",
                        el, clearFirst);
                    if (text != null && !text.isEmpty())
                    {
                        el.sendKeys(text);
                    }
                    Selenide.executeJavaScript(
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                        el);
                    if (pressEnter)
                    {
                        el.sendKeys(Keys.ENTER);
                    }
                }
                else
                {
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
                }

                final ObjectNode res = successNode(def.name());
                res.put("target", selector);
                res.put("value", text);
                res.put("pressedEnter", pressEnter);
                if (featureVector != null)
                {
                    res.set("domFeatureVector", MAPPER.valueToTree(featureVector));
                }
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    /**
     * Resolves a key name or string to a Selenium {@link Keys} enum value or literal character sequence,
     * supporting standard W3C DOM key names (e.g. "ArrowDown", "Backspace", "Escape").
     *
     * @param rawKey the raw key string passed by the caller
     * @return the resolved {@link CharSequence}
     */
    public static CharSequence resolveKey(final String rawKey)
    {
        if (rawKey == null || rawKey.isBlank())
        {
            return Keys.ENTER;
        }
        final String trimmed = rawKey.trim();
        final String normalized = trimmed.toUpperCase().replace("-", "_");
        return switch (normalized)
        {
            case "ARROWDOWN", "ARROW_DOWN", "DOWN" -> Keys.ARROW_DOWN;
            case "ARROWUP", "ARROW_UP", "UP" -> Keys.ARROW_UP;
            case "ARROWLEFT", "ARROW_LEFT", "LEFT" -> Keys.ARROW_LEFT;
            case "ARROWRIGHT", "ARROW_RIGHT", "RIGHT" -> Keys.ARROW_RIGHT;
            case "BACKSPACE", "BACK_SPACE" -> Keys.BACK_SPACE;
            case "PAGEUP", "PAGE_UP" -> Keys.PAGE_UP;
            case "PAGEDOWN", "PAGE_DOWN" -> Keys.PAGE_DOWN;
            case "ESC", "ESCAPE" -> Keys.ESCAPE;
            case "ENTER", "RETURN" -> Keys.ENTER;
            case "TAB" -> Keys.TAB;
            case "SPACE", "SPACEBAR" -> Keys.SPACE;
            case "DELETE", "DEL" -> Keys.DELETE;
            case "HOME" -> Keys.HOME;
            case "END" -> Keys.END;
            case "INSERT", "INS" -> Keys.INSERT;
            default -> {
                try
                {
                    yield Keys.valueOf(normalized);
                }
                catch (final IllegalArgumentException e)
                {
                    yield trimmed;
                }
            }
        };
    }

    private static AiTool createPressKeyTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("key").put("type", "string").put("description", "Key to press, e.g. 'Enter', 'Escape', 'Tab', 'Backspace', 'ArrowDown', 'ArrowUp'");
        props.putObject("selector").put("type", "string").put("description", "Optional selector of the element to send the key to");
        schema.putArray("required").add("key");

        final ToolDefinition def = new ToolDefinition("press_key", "Presses a keyboard key on the active element or specified element", schema);
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
                final String rawKey = call.arguments().path("key").asText("Enter");
                final String selector = resolveSelector(call.arguments());
                final CharSequence key = resolveKey(rawKey);

                if (!selector.isBlank())
                {
                    $(selector).sendKeys(key);
                }
                else
                {
                    try
                    {
                        final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                        if (driver != null)
                        {
                            driver.switchTo().activeElement().sendKeys(key);
                        }
                        else
                        {
                            Selenide.actions().sendKeys(key).perform();
                        }
                    }
                    catch (final Exception e)
                    {
                        Selenide.actions().sendKeys(key).perform();
                    }
                }
                DomQuiescenceWatcher.waitForDomQuiet();
                final ObjectNode res = successNode("press_key");
                res.put("key", rawKey);
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

        final ToolDefinition def = new ToolDefinition("navigate", "Navigates the browser to the specified URL", schema);
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

        final ToolDefinition def = new ToolDefinition("select", "Selects an option from a dropdown element by value or text", schema);
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
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                DomFeatureVector featureVector = null;
                if (driver != null)
                {
                    try
                    {
                        featureVector = new PageAnalyzer(driver).extractFeatureVector(el);
                    }
                    catch (final Throwable ignored)
                    {
                    }
                }

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
                if (featureVector != null)
                {
                    res.set("domFeatureVector", MAPPER.valueToTree(featureVector));
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

        final ToolDefinition def = new ToolDefinition("hover", "Hovers the mouse cursor over an element", schema);
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
                final SelenideElement el = findElement(selector);
                SelenideElementFinder.scrollIntoViewIfNeeded(el);
                el.shouldBe(Condition.visible).hover();
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

        final ToolDefinition def = new ToolDefinition("clear", "Clears text in an input or textarea element", schema);
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
                final SelenideElement el = findElement(selector).shouldBe(Condition.visible);
                final boolean isContentEditable = Boolean.TRUE.equals(Selenide.executeJavaScript(
                    "return !!(arguments[0] && (arguments[0].isContentEditable === true || arguments[0].getAttribute('contenteditable') === 'true' || arguments[0].hasAttribute('contenteditable')));",
                    el));
                if (isContentEditable)
                {
                    Selenide.executeJavaScript(
                        "var el = arguments[0];"
                        + "el.focus();"
                        + "el.innerHTML = '';"
                        + "var range = document.createRange();"
                        + "range.selectNodeContents(el);"
                        + "range.collapse(false);"
                        + "var sel = window.getSelection();"
                        + "sel.removeAllRanges();"
                        + "sel.addRange(range);"
                        + "el.dispatchEvent(new Event('input', { bubbles: true }));",
                        el);
                }
                else
                {
                    el.clear();
                }
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
        final ToolDefinition def = new ToolDefinition("clear_cookies", "Clears all browser cookies", schema);
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
        final ToolDefinition def = new ToolDefinition("back", "Navigates back in browser history", schema);
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
        final ToolDefinition def = new ToolDefinition("forward", "Navigates forward in browser history", schema);
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
        final ToolDefinition def = new ToolDefinition("refresh", "Refreshes the current browser page", schema);
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

        final ToolDefinition def = new ToolDefinition("wait", "Pauses execution for a specified duration", schema);
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
            final String innerText = el.getAttribute("innerText");
            if (innerText != null && !innerText.isBlank() && !candidates.contains(innerText))
            {
                candidates.add(innerText);
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
                if (pattern.matcher(candidate).find()
                        || pattern.matcher(candidate.replaceAll("\\s+", " ")).find())
                {
                    return true;
                }
            }
            return false;
        }

        final String unescaped = unescapeLiteralText(expectedText);
        final String normExpected = unescaped.replaceAll("\\s+", " ").trim();
        for (final String candidate : candidates)
        {
            if (exact)
            {
                if (candidate.trim().equalsIgnoreCase(unescaped.trim())
                        || candidate.replaceAll("\\s+", " ").trim().equalsIgnoreCase(normExpected))
                {
                    return true;
                }
            }
            else
            {
                if (candidate.toLowerCase().contains(unescaped.toLowerCase())
                        || candidate.replaceAll("\\s+", " ").toLowerCase().contains(normExpected.toLowerCase()))
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

        try
        {
            final String title = Selenide.title();
            if (title != null && !title.isBlank())
            {
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
                    if (pattern.matcher(title).find())
                    {
                        return true;
                    }
                }
                else
                {
                    final String unescaped = unescapeLiteralText(expectedText);
                    if (exact)
                    {
                        if (title.trim().equalsIgnoreCase(unescaped.trim()))
                        {
                            return true;
                        }
                    }
                    else
                    {
                        if (title.toLowerCase(Locale.ROOT).contains(unescaped.toLowerCase(Locale.ROOT)))
                        {
                            return true;
                        }
                    }
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
        props.putObject("negated").put("type", "boolean").put("description", "Whether to assert absence/non-matching of the text (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("expectedText");

        final ToolDefinition def = new ToolDefinition("assert_text", "Asserts that an element contains or matches the expected text or pattern", schema);
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
                final boolean negated = call.arguments().path("negated").asBoolean(false)
                        || call.arguments().path("not").asBoolean(false)
                        || call.arguments().path("invert").asBoolean(false);
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
                        final boolean matches = pattern.matcher(actualTitle).find();
                        if (negated ? matches : !matches)
                        {
                            throw new AssertionError("Page title \"" + actualTitle + "\" " + (negated ? "matches" : "does not match") + " regex pattern \"" + expectedText + "\"");
                        }
                    }
                    else if (exact)
                    {
                        final boolean matches = actualTitle.trim().equalsIgnoreCase(expectedText.trim());
                        if (negated ? matches : !matches)
                        {
                            throw new AssertionError("Page title \"" + actualTitle + "\" " + (negated ? "matches" : "does not exactly match") + " \"" + expectedText + "\"");
                        }
                    }
                    else
                    {
                        final boolean matches = actualTitle.toLowerCase(Locale.ROOT).contains(expectedText.toLowerCase(Locale.ROOT));
                        if (negated ? matches : !matches)
                        {
                            throw new AssertionError("Page title \"" + actualTitle + "\" " + (negated ? "contains" : "does not contain") + " expected text \"" + expectedText + "\"");
                        }
                    }
                }
                else if (selector == null || selector.isBlank() || "body".equalsIgnoreCase(selector.trim()) || "html".equalsIgnoreCase(selector.trim()))
                {
                    if (negated)
                    {
                        boolean present = true;
                        final long start = System.currentTimeMillis();
                        final long timeout = Configuration.timeout;
                        while (present && (System.currentTimeMillis() - start) < timeout)
                        {
                            present = isTextPresentOnPage(expectedText, regex, exact);
                            if (present)
                            {
                                Selenide.sleep(100);
                            }
                        }
                        if (present)
                        {
                            throw new AssertionError("Expected text/pattern \"" + expectedText + "\" was still present anywhere on the page within " + timeout + "ms.");
                        }
                    }
                    else
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
                }
                else
                {
                    boolean matched = negated;
                    final long start = System.currentTimeMillis();
                    final long timeout = Configuration.timeout;

                    while ((negated ? matched : !matched) && (System.currentTimeMillis() - start) < timeout)
                    {
                        matched = false;
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

                        if (!matched && !negated)
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

                        if (negated ? matched : !matched)
                        {
                            Selenide.sleep(100);
                        }
                    }

                    if (negated ? matched : !matched)
                    {
                        if (negated)
                        {
                            throw new AssertionError("Expected text/pattern \"" + expectedText + "\" was found on selector \"" + selector + "\" within " + timeout + "ms, but expected not to match.");
                        }
                        else
                        {
                            throw new AssertionError("Expected text/pattern \"" + expectedText + "\" was not found on selector \"" + selector + "\" within " + timeout + "ms.");
                        }
                    }
                }

                final String targetDesc = (selector == null || selector.isBlank()) ? "page" : selector;
                final ObjectNode res = successNode("assert_text");
                res.put("target", targetDesc);
                res.put("expected", expectedText);
                res.put("regex", regex);
                res.put("negated", negated);
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
        props.putObject("count").put("type", "integer").put("description", "Expected count of matching elements");
        final ArrayNode operatorEnum = props.putObject("operator")
                .put("type", "string")
                .put("description", "Comparison operator ('EXACT', 'MIN', 'MAX', default: 'EXACT')")
                .putArray("enum");
        operatorEnum.add("EXACT");
        operatorEnum.add("MIN");
        operatorEnum.add("MAX");
        operatorEnum.add("NOT_EQUALS");
        props.putObject("visibleOnly").put("type", "boolean").put("description", "Whether to count only visible elements (default: true)");
        props.putObject("negated").put("type", "boolean").put("description", "Whether to assert that count does not equal the expected value (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");
        req.add("count");

        final ToolDefinition def = new ToolDefinition("assert_count", "Asserts that the count of elements matching a selector satisfies expected criteria (exact, min, max, or not equals)", schema);
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
                    return ToolResult.error(call.callId(), errorNode("A valid 'selector' is required for assert_count").toString());
                }

                final JsonNode args = call.arguments();
                final boolean hasCount = args.hasNonNull("count");
                final boolean hasExpected = hasCount || args.hasNonNull("expectedCount");
                final boolean hasMin = args.hasNonNull("minCount");
                final boolean hasMax = args.hasNonNull("maxCount");

                if (!hasExpected && !hasMin && !hasMax)
                {
                    return ToolResult.error(call.callId(), errorNode("A count constraint ('count', 'expectedCount', 'minCount', or 'maxCount') must be specified for assert_count.").toString());
                }

                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    throw new AssertionError("No active browser window found to assert element count for '" + selector + "'");
                }

                final boolean visibleOnly = !call.arguments().has("visibleOnly") || call.arguments().path("visibleOnly").asBoolean(true);
                final boolean negated = call.arguments().path("negated").asBoolean(false)
                        || call.arguments().path("not").asBoolean(false);

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

                final String operator = args.path("operator").asText("EXACT").toUpperCase(Locale.ROOT);
                final boolean isNotEquals = "NOT_EQUALS".equals(operator) || "NOT_EQUAL".equals(operator) || "NEQ".equals(operator) || negated;

                if (isNotEquals)
                {
                    final int targetCount = hasCount ? args.path("count").asInt() : (hasExpected ? args.path("expectedCount").asInt() : 0);
                    if (actualCount == targetCount)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected count to not equal %d, but found %d (visible: %d, total in DOM: %d)",
                                selector, targetCount, actualCount, actualCount, totalElements));
                    }
                }
                else if (hasCount && "MIN".equals(operator))
                {
                    final int min = args.path("count").asInt();
                    if (actualCount < min)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected at least %d elements, but found %d (visible: %d, total in DOM: %d)",
                                selector, min, actualCount, actualCount, totalElements));
                    }
                }
                else if (hasCount && "MAX".equals(operator))
                {
                    final int max = args.path("count").asInt();
                    if (actualCount > max)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected at most %d elements, but found %d (visible: %d, total in DOM: %d)",
                                selector, max, actualCount, actualCount, totalElements));
                    }
                }
                else if (hasCount)
                {
                    final int expected = args.path("count").asInt();
                    if (actualCount != expected)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected exactly %d elements, but found %d (visible: %d, total in DOM: %d)",
                                selector, expected, actualCount, actualCount, totalElements));
                    }
                }
                else if (hasExpected)
                {
                    final int expected = args.path("expectedCount").asInt();
                    if (actualCount != expected)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected exactly %d elements, but found %d (visible: %d, total in DOM: %d)",
                                selector, expected, actualCount, actualCount, totalElements));
                    }
                }
                if (!hasCount && hasMin)
                {
                    final int min = args.path("minCount").asInt();
                    if (actualCount < min)
                    {
                        throw new AssertionError(String.format(
                                "Element count assertion failed for '%s': expected at least %d elements, but found %d (visible: %d, total in DOM: %d)",
                                selector, min, actualCount, actualCount, totalElements));
                    }
                }
                if (!hasCount && hasMax)
                {
                    final int max = args.path("maxCount").asInt();
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
                res.put("negated", negated);
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

    /**
     * Sanitizes selector strings that may contain trailing hallucinated parameter keys or labels
     * from LLMs (e.g. "button, text:", "div.card, text=...", or trailing commas).
     *
     * @param selector the raw selector string
     * @return the cleaned selector string
     */
    static String cleanSelector(final String selector)
    {
        if (selector == null || selector.isBlank())
        {
            return "";
        }
        String cleaned = selector.trim();
        // Strip trailing parameter labels hallucinated by LLMs like ", text:", ", text=", ", text"
        cleaned = cleaned.replaceAll("(?i),\\s*(?:text|action|target|locator)\\s*[:=]?.*$", "").trim();
        // Strip dangling commas
        cleaned = cleaned.replaceAll(",\\s*$", "").trim();
        return cleaned;
    }

    private static String resolveSelector(final JsonNode args)
    {
        if (args != null)
        {
            if (args.hasNonNull("selector") && !args.path("selector").asText().isBlank())
            {
                return cleanSelector(args.path("selector").asText());
            }
            if (args.hasNonNull("target") && !args.path("target").asText().isBlank())
            {
                return cleanSelector(args.path("target").asText());
            }
            if (args.hasNonNull("locator") && !args.path("locator").asText().isBlank())
            {
                return cleanSelector(args.path("locator").asText());
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

    private static AiTool createAssertUrlTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("expectedUrl").put("type", "string").put("description", "Expected URL substring, full URL, or regex pattern");
        props.putObject("exact").put("type", "boolean").put("description", "Whether URL match must be exact (default: false)");
        props.putObject("regex").put("type", "boolean").put("description", "Whether expectedUrl is a regular expression pattern (default: false)");
        props.putObject("negated").put("type", "boolean").put("description", "Whether to assert that current URL does not match or contain expectedUrl (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("expectedUrl");

        final ToolDefinition def = new ToolDefinition("assert_url", "Asserts that the current browser page URL contains or matches the expected URL or pattern", schema);
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
                final String rawExpectedUrl = call.arguments().hasNonNull("expectedUrl")
                        ? call.arguments().path("expectedUrl").asText()
                        : (call.arguments().hasNonNull("url")
                                ? call.arguments().path("url").asText()
                                : (call.arguments().hasNonNull("value")
                                        ? call.arguments().path("value").asText()
                                        : (call.arguments().hasNonNull("target")
                                                ? call.arguments().path("target").asText()
                                                : null)));
                if (rawExpectedUrl == null || rawExpectedUrl.isBlank())
                {
                    throw new AssertionError("assert_url requires an 'expectedUrl' argument");
                }

                final boolean exact = call.arguments().path("exact").asBoolean(false);
                final boolean regex = call.arguments().path("regex").asBoolean(false);
                final boolean negated = call.arguments().path("negated").asBoolean(false)
                        || call.arguments().path("not").asBoolean(false)
                        || call.arguments().path("invert").asBoolean(false);
                final String expectedUrl = regex ? cleanRegexPattern(rawExpectedUrl) : unescapeLiteralText(rawExpectedUrl);

                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    throw new AssertionError("No active browser window found to assert page URL");
                }

                try
                {
                    if (regex)
                    {
                        Pattern compiledPattern;
                        try
                        {
                            compiledPattern = Pattern.compile(expectedUrl, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                        }
                        catch (final PatternSyntaxException e)
                        {
                            compiledPattern = Pattern.compile(Pattern.quote(expectedUrl), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                        }
                        final Pattern finalPattern = compiledPattern;
                        if (negated)
                        {
                            Selenide.Wait().until(d -> d.getCurrentUrl() == null || !finalPattern.matcher(d.getCurrentUrl()).find());
                        }
                        else
                        {
                            Selenide.Wait().until(d -> d.getCurrentUrl() != null && finalPattern.matcher(d.getCurrentUrl()).find());
                        }
                    }
                    else if (exact)
                    {
                        if (negated)
                        {
                            Selenide.Wait().until(d -> d.getCurrentUrl() == null || !d.getCurrentUrl().trim().equalsIgnoreCase(expectedUrl.trim()));
                        }
                        else
                        {
                            Selenide.Wait().until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().trim().equalsIgnoreCase(expectedUrl.trim()));
                        }
                    }
                    else
                    {
                        if (negated)
                        {
                            Selenide.Wait().until(d -> d.getCurrentUrl() == null || !d.getCurrentUrl().toLowerCase(Locale.ROOT).contains(expectedUrl.toLowerCase(Locale.ROOT)));
                        }
                        else
                        {
                            Selenide.Wait().until(d -> d.getCurrentUrl() != null && d.getCurrentUrl().toLowerCase(Locale.ROOT).contains(expectedUrl.toLowerCase(Locale.ROOT)));
                        }
                    }
                }
                catch (final TimeoutException e)
                {
                    final String actualUrl = WebDriverRunner.url();
                    if (regex)
                    {
                        throw new AssertionError("Assertion failed: Expected URL to " + (negated ? "not match" : "match") + " regex \"" + expectedUrl + "\" within " + Configuration.timeout + "ms, but was \"" + actualUrl + "\"");
                    }
                    else if (exact)
                    {
                        throw new AssertionError("Assertion failed: Expected URL to " + (negated ? "not exactly match" : "exactly match") + " \"" + expectedUrl + "\" within " + Configuration.timeout + "ms, but was \"" + actualUrl + "\"");
                    }
                    else
                    {
                        throw new AssertionError("Assertion failed: Expected URL to " + (negated ? "not contain" : "contain") + " \"" + expectedUrl + "\" within " + Configuration.timeout + "ms, but was \"" + actualUrl + "\"");
                    }
                }

                final String currentUrl = WebDriverRunner.url();
                return ToolResult.success(call.callId(), (negated ? "Browser URL does not match: " : "Browser URL matched: ") + currentUrl);
            }
        };
    }

    private static AiTool createAssertTitleTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("expectedTitle").put("type", "string").put("description", "Expected page title substring, full title, or regex pattern");
        props.putObject("exact").put("type", "boolean").put("description", "Whether title match must be exact (default: false)");
        props.putObject("regex").put("type", "boolean").put("description", "Whether expectedTitle is a regular expression pattern (default: false)");
        props.putObject("negated").put("type", "boolean").put("description", "Whether to assert that current title does not match or contain expectedTitle (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("expectedTitle");

        final ToolDefinition def = new ToolDefinition("assert_title", "Asserts that the current browser page title contains or matches the expected title or pattern", schema);
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
                final String rawExpectedTitle = call.arguments().hasNonNull("expectedTitle")
                        ? call.arguments().path("expectedTitle").asText()
                        : (call.arguments().hasNonNull("title")
                                ? call.arguments().path("title").asText()
                                : (call.arguments().hasNonNull("value")
                                        ? call.arguments().path("value").asText()
                                        : (call.arguments().hasNonNull("target")
                                                ? call.arguments().path("target").asText()
                                                : null)));
                if (rawExpectedTitle == null || rawExpectedTitle.isBlank())
                {
                    throw new AssertionError("assert_title requires an 'expectedTitle' argument");
                }

                final boolean exact = call.arguments().path("exact").asBoolean(false);
                final boolean regex = call.arguments().path("regex").asBoolean(false);
                final boolean negated = call.arguments().path("negated").asBoolean(false)
                        || call.arguments().path("not").asBoolean(false)
                        || call.arguments().path("invert").asBoolean(false);
                final String expectedTitle = regex ? cleanRegexPattern(rawExpectedTitle) : unescapeLiteralText(rawExpectedTitle);

                if (!WebDriverRunner.hasWebDriverStarted())
                {
                    throw new AssertionError("No active browser window found to assert page title");
                }

                try
                {
                    if (regex)
                    {
                        Pattern compiledPattern;
                        try
                        {
                            compiledPattern = Pattern.compile(expectedTitle, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                        }
                        catch (final PatternSyntaxException e)
                        {
                            compiledPattern = Pattern.compile(Pattern.quote(expectedTitle), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                        }
                        final Pattern finalPattern = compiledPattern;
                        if (negated)
                        {
                            Selenide.Wait().until(d -> d.getTitle() == null || !finalPattern.matcher(d.getTitle()).find());
                        }
                        else
                        {
                            Selenide.Wait().until(d -> d.getTitle() != null && finalPattern.matcher(d.getTitle()).find());
                        }
                    }
                    else if (exact)
                    {
                        if (negated)
                        {
                            Selenide.Wait().until(d -> d.getTitle() == null || !d.getTitle().trim().equalsIgnoreCase(expectedTitle.trim()));
                        }
                        else
                        {
                            Selenide.Wait().until(d -> d.getTitle() != null && d.getTitle().trim().equalsIgnoreCase(expectedTitle.trim()));
                        }
                    }
                    else
                    {
                        if (negated)
                        {
                            Selenide.Wait().until(d -> d.getTitle() == null || !d.getTitle().toLowerCase(Locale.ROOT).contains(expectedTitle.toLowerCase(Locale.ROOT)));
                        }
                        else
                        {
                            Selenide.Wait().until(d -> d.getTitle() != null && d.getTitle().toLowerCase(Locale.ROOT).contains(expectedTitle.toLowerCase(Locale.ROOT)));
                        }
                    }
                }
                catch (final TimeoutException e)
                {
                    final String actualTitle = Selenide.title();
                    if (regex)
                    {
                        throw new AssertionError("Assertion failed: Expected page title to " + (negated ? "not match" : "match") + " regex \"" + expectedTitle + "\" within " + Configuration.timeout + "ms, but was \"" + actualTitle + "\"");
                    }
                    else if (exact)
                    {
                        throw new AssertionError("Assertion failed: Expected page title to " + (negated ? "not exactly match" : "exactly match") + " \"" + expectedTitle + "\" within " + Configuration.timeout + "ms, but was \"" + actualTitle + "\"");
                    }
                    else
                    {
                        throw new AssertionError("Assertion failed: Expected page title to " + (negated ? "not contain" : "contain") + " \"" + expectedTitle + "\" within " + Configuration.timeout + "ms, but was \"" + actualTitle + "\"");
                    }
                }

                final String currentTitle = Selenide.title();
                return ToolResult.success(call.callId(), (negated ? "Browser page title does not match: " : "Browser page title matched: ") + currentTitle);
            }
        };
    }

    private static AiTool createAssertElementStateTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS or XPath selector targeting the element to assert state on");
        final ArrayNode stateEnum = props.putObject("state")
                .put("type", "string")
                .put("description", "Expected state of the element ('visible', 'hidden', 'enabled', 'disabled', 'editable', 'readonly', 'checked', 'unchecked', 'selected', 'unselected', 'focused', 'exists', 'absent')")
                .putArray("enum");
        stateEnum.add("visible");
        stateEnum.add("hidden");
        stateEnum.add("enabled");
        stateEnum.add("disabled");
        stateEnum.add("editable");
        stateEnum.add("readonly");
        stateEnum.add("checked");
        stateEnum.add("unchecked");
        stateEnum.add("selected");
        stateEnum.add("unselected");
        stateEnum.add("focused");
        stateEnum.add("unfocused");
        stateEnum.add("not_focused");
        stateEnum.add("exists");
        stateEnum.add("absent");
        props.putObject("negated").put("type", "boolean").put("description", "Whether to invert the state assertion (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");
        req.add("state");

        final ToolDefinition def = new ToolDefinition("assert_element_state", "Asserts that an element satisfies a specific state (e.g. editable, readonly, enabled, disabled, visible, hidden, checked, unchecked, selected, unselected, focused, unfocused, exists, absent)", schema);
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
                    throw new AssertionError("assert_element_state requires a 'selector' argument");
                }

                final String rawState;
                if (call.arguments().hasNonNull("state") && !call.arguments().path("state").asText().isBlank())
                {
                    rawState = call.arguments().path("state").asText();
                }
                else if (call.arguments().hasNonNull("expectedState") && !call.arguments().path("expectedState").asText().isBlank())
                {
                    rawState = call.arguments().path("expectedState").asText();
                }
                else if (call.arguments().hasNonNull("value") && !call.arguments().path("value").asText().isBlank())
                {
                    rawState = call.arguments().path("value").asText();
                }
                else
                {
                    throw new AssertionError("assert_element_state requires a 'state' argument");
                }

                final boolean negated = call.arguments().path("negated").asBoolean(false)
                        || call.arguments().path("not").asBoolean(false);
                final String normalized = normalizeElementState(rawState);
                final String state = negated ? switch (normalized)
                {
                    case "visible" -> "hidden";
                    case "hidden" -> "visible";
                    case "enabled" -> "disabled";
                    case "disabled" -> "enabled";
                    case "checked" -> "unchecked";
                    case "unchecked" -> "checked";
                    case "selected" -> "unselected";
                    case "unselected" -> "selected";
                    case "focused" -> "unfocused";
                    case "unfocused" -> "focused";
                    case "exists" -> "absent";
                    case "absent" -> "exists";
                    default -> normalized;
                } : normalized;

                final SelenideElement el = findElement(selector);

                switch (state)
                {
                    case "visible" -> el.shouldBe(Condition.visible);
                    case "hidden" -> el.shouldBe(Condition.hidden);
                    case "enabled" -> el.shouldBe(Condition.enabled);
                    case "disabled" -> el.shouldBe(Condition.disabled);
                    case "editable" -> el.shouldBe(Condition.editable);
                    case "readonly" -> el.shouldBe(Condition.readonly);
                    case "checked" -> el.shouldBe(Condition.checked);
                    case "unchecked" -> el.shouldNotBe(Condition.checked);
                    case "selected" ->
                    {
                        if ("SELECT".equalsIgnoreCase(el.getTagName()))
                        {
                            el.getSelectedOption().shouldBe(Condition.exist);
                        }
                        else
                        {
                            el.shouldBe(Condition.selected);
                        }
                    }
                    case "unselected" ->
                    {
                        if ("SELECT".equalsIgnoreCase(el.getTagName()))
                        {
                            el.getSelectedOption().shouldNotBe(Condition.exist);
                        }
                        else
                        {
                            el.shouldNotBe(Condition.selected);
                        }
                    }
                    case "focused" ->
                    {
                        final Boolean isFocused = Selenide.executeJavaScript(
                                "return document.activeElement === arguments[0] || (arguments[0].matches && arguments[0].matches(':focus'));",
                                el);
                        if (!Boolean.TRUE.equals(isFocused))
                        {
                            el.shouldBe(Condition.focused);
                        }
                    }
                    case "unfocused" ->
                    {
                        final Boolean isFocused = Selenide.executeJavaScript(
                                "return document.activeElement === arguments[0] || (arguments[0].matches && arguments[0].matches(':focus'));",
                                el);
                        if (Boolean.TRUE.equals(isFocused))
                        {
                            el.shouldNotBe(Condition.focused);
                        }
                    }
                    case "exists" -> el.should(Condition.exist);
                    case "absent" -> el.should(Condition.or("Element is hidden or non-existent", Condition.hidden, Condition.not(Condition.exist)));
                    default -> throw new AssertionError("Unsupported element state assertion: '" + rawState + "'. Allowed states: visible, hidden, enabled, disabled, editable, readonly, checked, unchecked, selected, unselected, focused, unfocused, exists, absent.");
                }

                final ObjectNode res = successNode("assert_element_state");
                res.put("target", selector);
                res.put("state", state);
                res.put("negated", negated);
                res.put("matched", true);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    static String normalizeElementState(final String rawState)
    {
        if (rawState == null || rawState.isBlank())
        {
            return "";
        }
        String s = rawState.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("[") && s.endsWith("]"))
        {
            s = s.substring(1, s.length() - 1).trim();
        }
        return switch (s)
        {
            case "read-only", "read_only" -> "readonly";
            case "not_checked", "un-checked", "unchecked" -> "unchecked";
            case "not_selected", "un-selected", "unselected" -> "unselected";
            case "not_exist", "not_exists", "non-existent", "absent" -> "absent";
            case "unfocused", "not_focused" -> "unfocused";
            case "present", "exist", "exists" -> "exists";
            case "invisible", "hidden" -> "hidden";
            default -> s;
        };
    }

    private static AiTool createAssertAttributeTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS or XPath selector targeting the element to assert attribute on");
        props.putObject("attribute").put("type", "string").put("description", "Name of the HTML attribute (e.g. placeholder, value, href, disabled, readonly, type, data-*)");
        props.putObject("expectedValue").put("type", "string").put("description", "Expected attribute value. If omitted, asserts that the attribute exists.");
        props.putObject("exact").put("type", "boolean").put("description", "Whether attribute value match must be exact (default: true)");
        props.putObject("regex").put("type", "boolean").put("description", "Whether expectedValue is a regular expression pattern (default: false)");
        props.putObject("negated").put("type", "boolean").put("description", "Whether to assert attribute is absent or does not match expected value (default: false)");

        final ArrayNode req = schema.putArray("required");
        req.add("selector");
        req.add("attribute");

        final ToolDefinition def = new ToolDefinition("assert_attribute", "Asserts that an element has a specified attribute and optionally verifies its value (exact, substring, or regex)", schema);
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
                    throw new AssertionError("assert_attribute requires a 'selector' argument");
                }

                final String attrName;
                if (call.arguments().hasNonNull("attribute") && !call.arguments().path("attribute").asText().isBlank())
                {
                    attrName = call.arguments().path("attribute").asText().trim();
                }
                else if (call.arguments().hasNonNull("attr") && !call.arguments().path("attr").asText().isBlank())
                {
                    attrName = call.arguments().path("attr").asText().trim();
                }
                else if (call.arguments().hasNonNull("name") && !call.arguments().path("name").asText().isBlank())
                {
                    attrName = call.arguments().path("name").asText().trim();
                }
                else
                {
                    throw new AssertionError("assert_attribute requires an 'attribute' argument");
                }

                final String rawExpectedValue;
                if (call.arguments().hasNonNull("expectedValue"))
                {
                    rawExpectedValue = call.arguments().path("expectedValue").asText();
                }
                else if (call.arguments().hasNonNull("value"))
                {
                    rawExpectedValue = call.arguments().path("value").asText();
                }
                else if (call.arguments().hasNonNull("expected"))
                {
                    rawExpectedValue = call.arguments().path("expected").asText();
                }
                else
                {
                    rawExpectedValue = null;
                }

                final boolean exact = call.arguments().path("exact").asBoolean(true);
                final boolean regex = call.arguments().path("regex").asBoolean(false);
                final boolean negated = call.arguments().path("negated").asBoolean(false)
                        || call.arguments().path("not").asBoolean(false)
                        || call.arguments().path("invert").asBoolean(false);

                final SelenideElement el = findElement(selector);

                if (rawExpectedValue != null)
                {
                    final String expectedValue = regex ? cleanRegexPattern(rawExpectedValue) : unescapeLiteralText(rawExpectedValue);
                    if (negated)
                    {
                        if (regex)
                        {
                            el.shouldNotHave(Condition.attributeMatching(attrName, expectedValue));
                        }
                        else if (exact)
                        {
                            el.shouldNotHave(Condition.attribute(attrName, expectedValue));
                        }
                        else
                        {
                            el.shouldNotHave(Condition.attributeMatching(attrName, "(?s)(?i).*" + Pattern.quote(expectedValue) + ".*"));
                        }
                    }
                    else
                    {
                        if (regex)
                        {
                            el.shouldHave(Condition.attributeMatching(attrName, expectedValue));
                        }
                        else if (exact)
                        {
                            el.shouldHave(Condition.attribute(attrName, expectedValue));
                        }
                        else
                        {
                            el.shouldHave(Condition.attributeMatching(attrName, "(?s)(?i).*" + Pattern.quote(expectedValue) + ".*"));
                        }
                    }
                }
                else
                {
                    if (negated)
                    {
                        el.shouldNotHave(Condition.attribute(attrName));
                    }
                    else
                    {
                        el.shouldHave(Condition.attribute(attrName));
                    }
                }

                final ObjectNode res = successNode("assert_attribute");
                res.put("target", selector);
                res.put("attribute", attrName);
                if (rawExpectedValue != null)
                {
                    res.put("expectedValue", rawExpectedValue);
                    res.put("exact", exact);
                    res.put("regex", regex);
                }
                res.put("negated", negated);
                res.put("matched", true);
                return ToolResult.success(call.callId(), res.toString());
            }
        };
    }

    private static AiTool createScrollTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("direction").put("type", "string").put("description", "Direction to scroll ('down', 'up', 'top', 'bottom', 'left', 'right')");
        props.putObject("selector").put("type", "string").put("description", "Element selector to scroll into view");
        props.putObject("container").put("type", "string").put("description", "Optional selector for the scrollable container element (defaults to window/document)");
        props.putObject("yOffset").put("type", "integer").put("description", "Optional pixel distance to scroll vertically");
        props.putObject("xOffset").put("type", "integer").put("description", "Optional pixel distance to scroll horizontally");

        final ToolDefinition def = new ToolDefinition("scroll", "Scrolls the viewport, an element, or inside a container. Note: browser actions automatically scroll elements into view; use scroll only for lazy loading or visual repositioning.", schema);
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

        final ToolDefinition def = new ToolDefinition("execute_script", "Executes JavaScript in the browser context and returns result", schema);
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

        final ToolDefinition def = new ToolDefinition("query_dom", "Searches the live DOM for elements matching a selector or text, returning clean subtrees with attributes and visibility", schema);
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
                            if (candidates.length === 0) {
                                try {
                                    var caseInsensitiveSel = sel.replace(/\\[([a-zA-Z0-9_:-]+[~|^$*]?=(?:"[^"]*"|'[^']*'|[^\\]\\s]+))\\]/g, '[$1 i]');
                                    if (caseInsensitiveSel !== sel) {
                                        candidates = Array.from(document.querySelectorAll(caseInsensitiveSel));
                                    }
                                } catch(e) {}
                            }
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

    /**
     * Converts CSS attribute selectors (e.g. {@code [name*="exp"]}) into case-insensitive
     * selectors (e.g. {@code [name*="exp" i]}) per CSS Selectors Level 4.
     *
     * @param selector the input CSS selector
     * @return the case-insensitive selector if attribute selectors are present, or original selector
     */
    public static String toCaseInsensitiveAttributeSelector(final String selector)
    {
        if (selector == null || selector.isBlank())
        {
            return selector;
        }
        return selector.replaceAll("\\[([a-zA-Z0-9_:-]+[~|^$*]?=(?:\"[^\"]*\"|'[^']*'|[^\\]\\s]+))\\]", "[$1 i]");
    }

    private static AiTool createInspectTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("selector").put("type", "string").put("description", "Selector of the element to inspect");
        schema.putArray("required").add("selector");

        final ToolDefinition def = new ToolDefinition("inspect", "Inspects a specific DOM element, returning its full outerHTML, attributes, and visibility", schema);
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

        final ToolDefinition def = new ToolDefinition("screenshot", "Captures a screenshot of the browser viewport with optional Set-of-Marks visual badges", schema);
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
                final ObjectNode res = successNode("screenshot");
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

        final ToolDefinition def = new ToolDefinition("inspect_visual", "Captures a targeted visual crop of a specific element or container for multimodal inspection", schema);
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
                "request_context",
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

        final ToolDefinition def = new ToolDefinition("store", "Captures text from a DOM element or stores a specified value into an execution session variable for later use", schema);
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
                    return ToolResult.error(call.callId(), errorNode("store requires a non-empty 'variableName'").toString());
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
                        return ToolResult.error(call.callId(), errorNode("store requires either a 'selector' to capture text from or a literal 'value'").toString());
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

        final ToolDefinition def = new ToolDefinition("list_tabs", "Lists all open browser tabs/windows with their handle ID, index, title, URL, and whether it is currently active.", schema);
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

        final ToolDefinition def = new ToolDefinition("switch_tab", "Switches the active browser focus to another tab or window by handle, index, or title/URL substring. If target is omitted, switches to the newest window.", schema);
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

        final ToolDefinition def = new ToolDefinition("close_tab", "Closes the current active browser tab or window, and automatically switches focus back to the parent/primary tab.", schema);
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

        final ToolDefinition def = new ToolDefinition("upload_file", "Uploads a local file to the targeted file input element or styled dropzone container without opening native OS dialogs. Automatically discovers nested or associated <input type='file'> elements.", schema);
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

        final ToolDefinition def = new ToolDefinition("handle_alert", "Interacts with and resolves native browser modal dialogs (window.alert, window.confirm, window.prompt)", schema);
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

    private static AiTool createDragTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("selector").put("type", "string").put("description", "CSS or XPath selector of the element to drag");
        props.putObject("xOffset").put("type", "integer").put("description", "Horizontal pixel offset to drag (positive for right, negative for left)");
        props.putObject("yOffset").put("type", "integer").put("description", "Vertical pixel offset to drag (positive for down, negative for up)");
        schema.putArray("required").add("selector");

        final ToolDefinition def = new ToolDefinition("drag", "Drags an interactive element by horizontal and vertical pixel offsets", schema);
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
                final String selector = resolveSelector(args);
                if (selector.isBlank())
                {
                    return ToolResult.error(call.callId(), errorNode("Element selector is required for browser_drag.").toString());
                }

                final int xOffset = args.hasNonNull("xOffset") ? args.path("xOffset").asInt(0)
                        : args.hasNonNull("offsetX") ? args.path("offsetX").asInt(0)
                        : args.hasNonNull("deltaX") ? args.path("deltaX").asInt(0)
                        : args.hasNonNull("x") ? args.path("x").asInt(0) : 0;
                final int yOffset = args.hasNonNull("yOffset") ? args.path("yOffset").asInt(0)
                        : args.hasNonNull("offsetY") ? args.path("offsetY").asInt(0)
                        : args.hasNonNull("deltaY") ? args.path("deltaY").asInt(0)
                        : args.hasNonNull("y") ? args.path("y").asInt(0) : 0;

                try
                {
                    final SelenideElement el = findElement(selector).shouldBe(Condition.visible);
                    el.scrollIntoView("{behavior: \"instant\", block: \"center\"}");

                    final WebDriver driver = WebDriverRunner.getWebDriver();
                    new Actions(driver)
                            .moveToElement(el.getWrappedElement())
                            .clickAndHold()
                            .moveByOffset(xOffset, yOffset)
                            .pause(Duration.ofMillis(50))
                            .release()
                            .perform();

                    final ObjectNode res = successNode("drag");
                    res.put("selector", selector);
                    res.put("xOffset", xOffset);
                    res.put("yOffset", yOffset);
                    return ToolResult.success(call.callId(), res.toString());
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Failed executing browser_drag on '{}': {}", selector, e.getMessage());
                    return ToolResult.error(call.callId(), errorNode("Failed dragging element: " + e.getMessage()).toString());
                }
            }
        };
    }

    private static AiTool createDragToTool()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final ObjectNode props = schema.putObject("properties");
        props.putObject("source").put("type", "string").put("description", "CSS or XPath selector of the source element to drag");
        props.putObject("target").put("type", "string").put("description", "CSS or XPath selector of the target element to drop onto");
        schema.putArray("required").add("source").add("target");

        final ToolDefinition def = new ToolDefinition("drag_to", "Drags a source element and drops it onto a target element", schema);
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
                final String source = args.hasNonNull("source") ? args.path("source").asText().trim()
                        : args.hasNonNull("sourceSelector") ? args.path("sourceSelector").asText().trim()
                        : args.hasNonNull("from") ? args.path("from").asText().trim()
                        : resolveSelector(args);

                final String target = args.hasNonNull("target") ? args.path("target").asText().trim()
                        : args.hasNonNull("targetSelector") ? args.path("targetSelector").asText().trim()
                        : args.hasNonNull("to") ? args.path("to").asText().trim()
                        : args.hasNonNull("destination") ? args.path("destination").asText().trim()
                        : "";

                if (source.isBlank() || target.isBlank())
                {
                    return ToolResult.error(call.callId(), errorNode("Both 'source' and 'target' selectors are required for browser_drag_to.").toString());
                }

                try
                {
                    final SelenideElement sourceEl = findElement(source).shouldBe(Condition.visible);
                    final SelenideElement targetEl = findElement(target).shouldBe(Condition.visible);

                    sourceEl.scrollIntoView("{behavior: \"instant\", block: \"center\"}");

                    final WebDriver driver = WebDriverRunner.getWebDriver();
                    new Actions(driver)
                            .moveToElement(sourceEl.getWrappedElement())
                            .clickAndHold()
                            .moveToElement(targetEl.getWrappedElement())
                            .pause(Duration.ofMillis(50))
                            .release()
                            .perform();

                    final String isDraggable = sourceEl.getAttribute("draggable");
                    if ("true".equalsIgnoreCase(isDraggable))
                    {
                        simulateHtml5DragAndDrop(driver, sourceEl.getWrappedElement(), targetEl.getWrappedElement());
                    }

                    final ObjectNode res = successNode("drag_to");
                    res.put("source", source);
                    res.put("target", target);
                    return ToolResult.success(call.callId(), res.toString());
                }
                catch (final Exception e)
                {
                    LOGGER.warn("Failed executing browser_drag_to from '{}' to '{}': {}", source, target, e.getMessage());
                    return ToolResult.error(call.callId(), errorNode("Failed dragging element to target: " + e.getMessage()).toString());
                }
            }
        };
    }

    /**
     * Simulates HTML5 Drag and Drop events (dragstart, dragover, drop, dragend) using a synthetic DataTransfer object.
     *
     * @param driver active WebDriver instance
     * @param source source draggable WebElement
     * @param target target dropzone WebElement
     */
    public static void simulateHtml5DragAndDrop(final WebDriver driver, final WebElement source, final WebElement target)
    {
        if (driver instanceof JavascriptExecutor js)
        {
            final String script = """
                const source = arguments[0];
                const target = arguments[1];
                if (!source || !target) return;
                
                let dataTransfer;
                try {
                    dataTransfer = new DataTransfer();
                } catch (e) {
                    dataTransfer = {
                        data: {},
                        setData: function(k, v) { this.data[k] = v; },
                        getData: function(k) { return this.data[k] || ''; },
                        clearData: function() { this.data = {}; },
                        types: ['text/plain'],
                        files: [],
                        items: [],
                        effectAllowed: 'all',
                        dropEffect: 'none'
                    };
                }
                
                function createEvent(type) {
                    let ev;
                    try {
                        ev = new DragEvent(type, {
                            bubbles: true,
                            cancelable: true,
                            dataTransfer: dataTransfer
                        });
                    } catch (e) {
                        ev = document.createEvent('CustomEvent');
                        ev.initCustomEvent(type, true, true, null);
                        ev.dataTransfer = dataTransfer;
                    }
                    return ev;
                }

                source.dispatchEvent(createEvent('dragstart'));
                target.dispatchEvent(createEvent('dragenter'));
                target.dispatchEvent(createEvent('dragover'));
                target.dispatchEvent(createEvent('drop'));
                source.dispatchEvent(createEvent('dragend'));

                function createMouseEvent(type, targetEl) {
                    const rect = targetEl.getBoundingClientRect();
                    const clientX = rect.left + rect.width / 2;
                    const clientY = rect.top + rect.height / 2;
                    const ev = new MouseEvent(type, {
                        bubbles: true,
                        cancelable: true,
                        view: window,
                        clientX: clientX,
                        clientY: clientY
                    });
                    targetEl.dispatchEvent(ev);
                }
                createMouseEvent('mousedown', source);
                createMouseEvent('mousemove', target);
                createMouseEvent('mouseup', target);
            """;
            try
            {
                js.executeScript(script, source, target);
            }
            catch (final Exception ignored)
            {
            }
        }
    }
}
