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
package com.xceptance.neodymium.ai.action;

import java.time.Duration;
import com.xceptance.neodymium.util.Neodymium;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.util.layer.FoundElement;

import io.qameta.allure.Step;

/**
 * Translates {@link Action} objects into browser interactions via the
 * backend-agnostic {@link com.xceptance.neodymium.util.InteractionLayer}.
 * Uses smart element resolution that tries multiple strategies in order of
 * preference.
 * <p>
 * This class contains <strong>no direct Selenide or WebDriver imports</strong>.
 * All browser interactions are delegated through {@code Neodymium.interaction()}
 * and the {@link FoundElement} abstraction, making it compatible with both
 * the Selenide and Playwright backends.
 * </p>
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ActionExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ActionExecutor.class);
    private final Set<String> actionLogs = new HashSet<>();

    private final Duration getElementTimeout()
    {
        return Duration.ofMillis(Neodymium.interaction().getTimeout());
    }


    /**
     * The instance of the currently running Test Class
     */
    private Object test;

    /**
     * Context map for variables captured during playbook execution (e.g. via STORE
     * action).
     */
    private final Map<String, String> executionVariables = new HashMap<>();

    /**
     * Maps stale/recorded window handles to active window handles for robust multi-window execution.
     */
    private final Map<String, String> windowHandleMapping = new HashMap<>();

    public String cleanElementText(final String text)
    {
        if (text == null)
        {
            return null;
        }
        if (text.contains("'"))
        {
            final int firstQuote = text.indexOf("'");
            final int lastQuote = text.lastIndexOf("'");
            if (firstQuote != lastQuote && firstQuote < lastQuote)
            {
                return text.substring(firstQuote + 1, lastQuote);
            }
        }
        if (text.contains("\""))
        {
            final int firstQuote = text.indexOf("\"");
            final int lastQuote = text.lastIndexOf("\"");
            if (firstQuote != lastQuote && firstQuote < lastQuote)
            {
                return text.substring(firstQuote + 1, lastQuote);
            }
        }
        return text;
    }

    private String cleanTarget(String target)
    {
        if (target == null || target.isBlank())
        {
            throw new ActionExecutionException("Action target is null or empty");
        }

        // Clean up common AI hallucinations for Neodymium IDs
        if (target.startsWith("#xc_"))
        {
            target = target.substring(1);
            logDebug("   ⚠️ Auto-corrected AI hallucination: removed '#' from Neodymium ID [{}]", target);
        }

        // Clean up common AI hallucinations for Selenium prefixes
        if (target.toLowerCase().startsWith("xpath="))
        {
            target = target.substring(6);
            logDebug("   ⚠️ Auto-corrected AI hallucination: removed 'xpath=' prefix [{}]", target);
        }
        else if (target.toLowerCase().startsWith("css="))
        {
            target = target.substring(4);
            logDebug("   ⚠️ Auto-corrected AI hallucination: removed 'css=' prefix [{}]", target);
        }

        // Clean up AI hallucinations where the LLM wraps the ID in an attribute selector
        if (target.contains("data-neo-ref="))
        {
            final Matcher m = Pattern.compile("data-neo-ref=['\"]?(xc_[a-zA-Z0-9_]+)['\"]?").matcher(target);
            if (m.find())
            {
                target = m.group(1);
                logDebug("   ⚠️ Auto-corrected AI hallucination: extracted Neodymium ID from attribute selector [{}]", target);
            }
        }

        return target;
    }

    public ActionExecutor(Object test) {
        this.test = test;
    }

    /**
     * Returns the active test instance. Used by JIT PESAP to reflectively scan
     * the test class for custom validation methods.
     *
     * @return the test instance, or {@code null} if not set
     */
    public final Object getTestInstance()
    {
        return this.test;
    }

    public void setVariable(final String key, final String value)
    {
        if (key != null && value != null)
        {
            executionVariables.put(key, value);
            if (Neodymium.getData() != null)
            {
                Neodymium.getData().put(key, value);
            }
        }
    }

    public String getVariable(String key) {
        return executionVariables.get(key);
    }

    private String interpolate(String text) {
        if (text == null || text.isBlank() || !text.contains("${")) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : executionVariables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, entry.getValue());
            }
        }
        return result;
    }

    private void interpolateAction(final Action action)
    {
        if (action.getTarget() != null)
        {
            action.setTarget(interpolate(action.getTarget()));
        }
        if (action.getValues() != null)
        {
            final List<String> newValues = new ArrayList<>();
            for (final String val : action.getValues())
            {
                newValues.add(interpolate(val));
            }
            action.setValue(newValues);
        }
    }

    /**
     * Executes a single action.
     *
     * @param action
     *               the action to execute
     * @throws ActionExecutionException
     *                                  if the action fails
     */
    @Step("{action.description}  - {action.type} {action.replay} ")
    public void execute(final Action action) {
        actionLogs.clear();
        LOG.debug("   🤖 {}", action.getDescription());

        interpolateAction(action);
        switchFrameContext(action.getFrameId());

        preCheckAction(action);

        final AiActionPlugin plugin = ActionRegistry.getPlugin(action.getType());

        try
        {
            if (plugin != null) {
                try {
                    plugin.execute(action, test, this);

                    if (action.getElementContext() != null && !action.getElementContext().isEmpty()) {
                        LOG.debug("   ✅ Interacted with element: {}", action.getElementContext());
                    }
                } finally {
                    plugin.cleanup(action, this);
                }
            } else {
                LOG.warn("Unsupported action type: {}", action.getType());
            }
        }
        finally
        {
            try
            {
                // Always reset frame context via the backend-agnostic facade.
                Neodymium.interaction().switchToDefaultContent();
            }
            catch (final Exception ignored)
            {
            }
        }
    }

    /**
     * Executes a list of actions in sequence.
     *
     * @param actions
     *                actions to execute
     * @throws ActionExecutionException
     *                                  if any action fails
     */
    public void executeAll(final List<Action> actions) {
        for (int i = 0; i < actions.size(); i++) {
            final Action action = actions.get(i);

            LOG.debug("-----------------------------------------------------------");
            LOG.debug("▶️ [EXEC] Executing Action [{}/{}]: {}", i + 1, actions.size(), action.getType());

            execute(action);

            // Small pause between actions for page stability
            Neodymium.interaction().sleep(300);
        }
    }

    // --- Element resolution ---

    /**
     * Finds an element using multiple strategies in order of preference:
     * 0. Neodymium data-neo-ref ID
     * 1. CSS selector
     * 2. XPath
     * 3. Link text / partial link text
     * 4. Text content via XPath
     *
     * @param action the action whose target and element details are used
     * @return the first matched {@link FoundElement}
     */
    public FoundElement findElement(final Action action)
    {
        try
        {
            final List<FoundElement> elements = findElements(action);
            if (!elements.isEmpty())
            {
                return elements.get(0);
            }
            throw new ActionExecutionException(
                    String.format("Could not find element for target '%s' or text '%s'",
                            action.getTarget(), action.getElementDetails()));
        }
        catch (final ActionExecutionException e)
        {
            throw new ActionExecutionException(
                    String.format("Could not find element for target '%s' or text '%s'",
                            action.getTarget(), action.getElementDetails()));
        }
    }

    /**
     * Finds all elements using the same strategies as findElement, polling until the timeout is reached.
     *
     * @param action the action whose target and element details are used
     * @return list of matched elements (non-empty)
     */
    public List<FoundElement> findElements(final Action action)
    {
        final long start = System.currentTimeMillis();
        final long timeoutMs = getElementTimeout().toMillis();
        ActionExecutionException lastException = null;

        while (true)
        {
            try
            {
                final boolean isLastAttempt = (System.currentTimeMillis() - start + 100) >= timeoutMs;
                final List<FoundElement> elements = findElementsInternal(action, isLastAttempt);
                if (!elements.isEmpty())
                {
                    return elements;
                }
            }
            catch (final ActionExecutionException e)
            {
                lastException = e;
            }

            if (System.currentTimeMillis() - start >= timeoutMs)
            {
                break;
            }

            Neodymium.interaction().sleep(100);
        }

        if (lastException != null)
        {
            throw lastException;
        }

        throw new ActionExecutionException(
                String.format("Could not find any elements for target '%s' or text '%s'", action.getTarget(),
                        action.getElementDetails()));
    }

    /**
     * Internal element lookup using multiple fallback strategies.
     * Strategies that rely on JavaScript returning DOM element handles (Strategy 0 shadow fallback,
     * 1.5, 0.2, 5) have been removed because Playwright cannot serialize DOM elements from JS.
     * Strategies 1 (CSS), 2 (XPath), 3 (link text), and 4 (text XPath) work via the
     * {@link com.xceptance.neodymium.util.InteractionLayer} and are backend-agnostic.
     *
     * @param action      the action whose target and element details are used
     * @param logErrors   whether to emit debug log messages for each failed strategy
     * @return a non-empty list of matched elements, or throws {@link ActionExecutionException}
     */
    private List<FoundElement> findElementsInternal(final Action action, final boolean logErrors)
    {
        switchFrameContext(action.getFrameId());
        final String target = cleanTarget(action.getTarget());

        // Strategy 0: Direct Match for Neodymium Automation ID
        if (target.matches("^xc_.*"))
        {
            try
            {
                final List<FoundElement> elements = Neodymium.interaction()
                        .findAllElements(By.cssSelector("[data-neo-ref='" + target + "']"));
                if (!elements.isEmpty())
                {
                    logDebug(logErrors, "   🔍 Resolved using Strategy 0: Neodymium Automation ID [{}]", target);
                    return elements;
                }
                logDebug(logErrors, "   ❌ Strategy 0 failed: Neodymium Automation ID [[data-neo-ref='{}']]", target);
            }
            catch (final Exception e)
            {
                logDebug(logErrors, "   ❌ Strategy 0 failed: Neodymium Automation ID [[data-neo-ref='{}']] with error: {}",
                        target, e.getMessage());
            }
        }

        if (target.equals("document.title") || target.equals("pageTitle"))
        {
            logDebug(logErrors, "   🔍 Resolved using Title Strategy [{}]", target);
            return Neodymium.interaction().findAllElements(By.cssSelector("head > title"));
        }

        // Strategy 1: Try as CSS selector
        try
        {
            final List<FoundElement> elements = Neodymium.interaction().findAllElements(By.cssSelector(target));
            if (!elements.isEmpty())
            {
                logDebug(logErrors, "   🔍 Resolved using Strategy 1: CSS selector [{}]", target);
                return elements;
            }
            else
            {
                logDebug(logErrors, "   ❌ Strategy 1 failed: CSS selector [{}]", target);
            }
        }
        catch (final Exception e)
        {
            logDebug(logErrors, "   ❌ Strategy 1 failed: CSS selector [{}] with error: {}", target, e.getMessage());
        }

        // Strategy 2: Try as XPath
        if (target.startsWith("/") || target.startsWith("("))
        {
            if (isValidXPath(target))
            {
                try
                {
                    final List<FoundElement> elements = Neodymium.interaction().findAllElements(By.xpath(target));
                    if (!elements.isEmpty())
                    {
                        logDebug(logErrors, "   🔍 Resolved using Strategy 2: XPath [{}]", target);
                        return elements;
                    }
                    else
                    {
                        logDebug(logErrors, "   ❌ Strategy 2 failed: XPath [{}]", target);
                    }
                }
                catch (final Exception e)
                {
                    logDebug(logErrors, "   ❌ Strategy 2 failed: XPath [{}] with error: {}", target, e.getMessage());
                }
            }
            else
            {
                LOG.debug("Target '{}' is not a valid XPath. Skipping XPath strategy.", target);
            }
        }

        // Strategy 3: Try as link text
        try
        {
            List<FoundElement> elements = Neodymium.interaction().findAllElements(By.linkText(target));
            if (!elements.isEmpty())
            {
                logDebug(logErrors, "   🔍 Resolved using Strategy 3: Link text [{}]", target);
                return elements;
            }
            logDebug(logErrors, "   ❌ Strategy 3 failed: Link text [{}]", target);

            final String extractedTargetName = cleanElementText(target);
            if (extractedTargetName != null && !extractedTargetName.equals(target))
            {
                elements = Neodymium.interaction().findAllElements(By.linkText(extractedTargetName));
                if (!elements.isEmpty())
                {
                    logDebug(logErrors, "   🔍 Resolved using Strategy 3 (extracted target name): Link text [{}]",
                            extractedTargetName);
                    return elements;
                }
                logDebug(logErrors, "   ❌ Strategy 3 failed (extracted target name): Link text [{}]", extractedTargetName);
            }

            final String elementText = action.getElementDetails();
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target))
            {
                elements = Neodymium.interaction().findAllElements(By.linkText(elementText));
                if (!elements.isEmpty())
                {
                    logDebug(logErrors, "   🔍 Resolved using Strategy 3: Link text [{}]", elementText);
                    return elements;
                }
                logDebug(logErrors, "   ❌ Strategy 3 failed: Link text [{}]", elementText);
            }
        }
        catch (final Exception e)
        {
            logDebug(logErrors, "   ❌ Strategy 3 failed: Link text resolution with error: {}", e.getMessage());
        }

        // Strategy 4: Try finding by text content via XPath
        try
        {
            final String targetXpath = escapeXpath(target);
            String xpath = String.format(
                    "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                    targetXpath, targetXpath, targetXpath);
            List<FoundElement> elements = Neodymium.interaction().findAllElements(By.xpath(xpath));
            if (!elements.isEmpty())
            {
                logDebug(logErrors, "   🔍 Resolved using Strategy 4: Text content XPath [{}]", xpath);
                return elements;
            }
            logDebug(logErrors, "   ❌ Strategy 4 failed: Text content XPath [{}]", xpath);

            final String extractedTargetName = cleanElementText(target);
            if (extractedTargetName != null && !extractedTargetName.equals(target))
            {
                final String extractedXpath = escapeXpath(extractedTargetName);
                xpath = String.format(
                        "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                        extractedXpath, extractedXpath, extractedXpath);
                elements = Neodymium.interaction().findAllElements(By.xpath(xpath));
                if (!elements.isEmpty())
                {
                    logDebug(logErrors, "   🔍 Resolved using Strategy 4 (extracted target name): Text content XPath [{}]", xpath);
                    return elements;
                }
                logDebug(logErrors, "   ❌ Strategy 4 failed (extracted target name): Text content XPath [{}]", xpath);
            }

            final String elementText = cleanElementText(action.getElementDetails());
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target))
            {
                final String elementTextXpath = escapeXpath(elementText);
                xpath = String.format(
                        "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                        elementTextXpath, elementTextXpath, elementTextXpath);
                elements = Neodymium.interaction().findAllElements(By.xpath(xpath));
                if (!elements.isEmpty())
                {
                    logDebug(logErrors, "   🔍 Resolved using Strategy 4: Text content XPath [{}]", xpath);
                    return elements;
                }
                logDebug(logErrors, "   ❌ Strategy 4 failed: Text content XPath [{}]", xpath);
            }
        }
        catch (final Exception e)
        {
            logDebug(logErrors, "   ❌ Strategy 4 failed: Text content search with error: {}", e.getMessage());
        }

        throw new ActionExecutionException(
                String.format("Could not find any elements for target '%s' or text '%s'", action.getTarget(),
                        action.getElementDetails()));
    }
    /**
     * Resolves a locator string to a Selenium {@link By} instance.
     * Handles XPath expressions (starting with {@code /} or {@code (}), and falls back
     * to CSS selector for all other strings.
     *
     * @param target the target selector string
     * @return the resolved {@link By} locator
     */
    public By resolveLocator(final String target)
    {
        if (target.startsWith("/") || target.startsWith("("))
        {
            return By.xpath(target);
        }
        return By.cssSelector(target);
    }

    private boolean isValidXPath(final String target)
    {
        if (target == null || target.isBlank())
        {
            return false;
        }
        try
        {
            return Neodymium.interaction().executeJavaScript(
                    "try { document.createExpression(arguments[0], null); return true; } catch(e) { return false; }",
                    target);
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    private String escapeXpath(final String value)
    {
        if (!value.contains("'"))
        {
            return "'" + value + "'";
        }
        else if (!value.contains("\""))
        {
            return "\"" + value + "\"";
        }
        else
        {
            return "concat('" + value.replace("'", "', \"'\", '") + "')";
        }
    }

    /**
     * Scrolls the given element into the visible viewport using a smooth animation.
     *
     * @param element the {@link FoundElement} to scroll into view
     */
    public void scrollIntoView(final FoundElement element)
    {
        element.scrollIntoView();
        Neodymium.interaction().sleep(200);
    }

    /**
     * Pre-checks if the action target exists and is visible before any interaction
     * is attempted. Use this for safely
     * replaying instructions from a Playbook.
     */
    /**
     * Pre-checks if the action target exists and is visible before any interaction
     * is attempted. Use this for safely replaying instructions from a Playbook.
     *
     * @param action the action to pre-check
     */
    @Step("Pre-checking action: {action.type}")
    public void preCheckAction(final Action action)
    {
        final AiActionPlugin plugin = ActionRegistry.getPlugin(action.getType());
        if (plugin != null)
        {
            plugin.preCheck(action, this);
        }
    }

    /**
     * Extracts useful DOM attributes from a {@link FoundElement} to be used as action context.
     * This helps the LLM self-heal Playbooks by knowing what the element used to look like.
     *
     * @param element the element to inspect
     * @return a map of attribute names to values (nulls and blanks filtered out)
     */
    public Map<String, String> extractElementContext(final FoundElement element)
    {
        try
        {
            final Map<String, String> context = new HashMap<>();
            context.put("tagName", element.getTagName());
            context.put("text", element.getText());
            context.put("id", element.getAttribute("id"));
            context.put("classes", element.getAttribute("class"));
            context.put("href", element.getAttribute("href"));
            context.put("name", element.getAttribute("name"));
            context.put("type", element.getAttribute("type"));
            context.put("placeholder", element.getAttribute("placeholder"));

            // Filter out nulls and blank values
            context.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());

            return context;
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to extract element context: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Exception thrown when an action execution fails.
     */
    public static class ActionExecutionException extends RuntimeException
    {
        public ActionExecutionException(final String message)
        {
            super(message);
        }

        public ActionExecutionException(final String message, final Throwable cause)
        {
            super(message, cause);
        }
    }

    private void logDebug(final String format, final Object... args)
    {
        if (LOG.isDebugEnabled())
        {
            final String message = org.slf4j.helpers.MessageFormatter.arrayFormat(format, args).getMessage();
            if (actionLogs.add(message))
            {
                LOG.debug(message);
            }
        }
    }

    private void logDebug(final boolean enabled, final String format, final Object... args)
    {
        if (enabled && LOG.isDebugEnabled())
        {
            final String message = org.slf4j.helpers.MessageFormatter.arrayFormat(format, args).getMessage();
            if (actionLogs.add(message))
            {
                LOG.debug(message);
            }
        }
    }

    private void switchFrameContext(final String targetFrameId)
    {
        if (targetFrameId == null || targetFrameId.isBlank())
        {
            return;
        }
        final String[] parts = targetFrameId.split(":", 2);
        final String rawWindowHandle = parts[0];
        String windowHandle = rawWindowHandle;
        final String framePath;
        if (parts.length == 2)
        {
            framePath = parts[1];
        }
        else
        {
            framePath = "main";
        }

        try
        {
            final Set<String> activeHandles = Neodymium.interaction().getWindowHandles();
            if (windowHandle.startsWith("win_"))
            {
                final int index = Integer.parseInt(windowHandle.substring(4));
                final List<String> activeList = new ArrayList<>(activeHandles);
                if (index >= 0 && index < activeList.size())
                {
                    windowHandle = activeList.get(index);
                }
            }
            else if (!activeHandles.contains(windowHandle))
            {
                if (!windowHandleMapping.containsKey(windowHandle))
                {
                    final int nextIndex = windowHandleMapping.size();
                    final List<String> activeList = new ArrayList<>(activeHandles);
                    if (nextIndex < activeList.size())
                    {
                        windowHandleMapping.put(windowHandle, activeList.get(nextIndex));
                    }
                    else
                    {
                        windowHandleMapping.put(windowHandle, activeList.get(activeList.size() - 1));
                    }
                }
                windowHandle = windowHandleMapping.get(windowHandle);
            }
            Neodymium.interaction().switchToWindow(windowHandle);
            Neodymium.interaction().switchToDefaultContent();

            if (!"main".equals(framePath))
            {
                if (framePath.matches("^[0-9]+(\\.[0-9]+)*$"))
                {
                    final String[] indices = framePath.split("\\.");
                    for (final String indexStr : indices)
                    {
                        if (!indexStr.equals("main") && !indexStr.isBlank())
                        {
                            Neodymium.interaction().switchToFrame(Integer.parseInt(indexStr));
                        }
                    }
                }
                else
                {
                    final String[] selectors = framePath.split(" >>> ");
                    for (final String selector : selectors)
                    {
                        if (!selector.equals("main") && !selector.isBlank())
                        {
                            // Switch to frame by CSS selector using the backend-agnostic facade.
                            Neodymium.interaction().switchToFrame(selector);
                        }
                    }
                }
            }
        }
        catch (final Exception e)
        {
            if (isNoSuchWindowException(e))
            {
                logDebug("   ⚠️ Target window was closed, falling back to first available window: {}", e.getMessage());
                try
                {
                    final Set<String> activeHandlesFallback = Neodymium.interaction().getWindowHandles();
                    if (!activeHandlesFallback.isEmpty())
                    {
                        final String fallbackHandle = activeHandlesFallback.iterator().next();
                        Neodymium.interaction().switchToWindow(fallbackHandle);
                        Neodymium.interaction().switchToDefaultContent();
                        windowHandleMapping.put(rawWindowHandle, fallbackHandle);
                    }
                }
                catch (final Exception ex)
                {
                    logDebug("   ⚠️ Could not switch to fallback window: {}", ex.getMessage());
                }
            }
            else
            {
                logDebug("   ⚠️ Could not switch to frame {}: {}", targetFrameId, e.getMessage());
            }
        }
    }

    private boolean isNoSuchWindowException(final Exception e)
    {
        // Check by message since WebDriver types are not imported here.
        final String msg = e.getMessage();
        if (msg != null)
        {
            final String lower = msg.toLowerCase();
            return lower.contains("no such window") || lower.contains("window already closed")
                    || lower.contains("target window already closed");
        }
        return false;
    }
}
