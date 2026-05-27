/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.ai.action;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;

import io.qameta.allure.Step;

/**
 * Translates {@link Action} objects into Selenium WebDriver calls. Uses smart
 * element resolution that tries multiple
 * strategies.
 *
 * // AI-generated: Gemini 2.0 Flash
 */
public class ActionExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ActionExecutor.class);
    private final java.util.Set<String> actionLogs = new java.util.HashSet<>();

    private final Duration getElementTimeout()
    {
        return Duration.ofMillis(Configuration.timeout);
    }

    private static final String SHADOW_DOM_FINDER_PREFIX = "var allRoots = [document];\n" +
            "function findShadowRoots(root) {\n" +
            "  var els = root.querySelectorAll('*');\n" +
            "  for (var i = 0; i < els.length; i++) {\n" +
            "    if (els[i].shadowRoot) {\n" +
            "      allRoots.push(els[i].shadowRoot);\n" +
            "      findShadowRoots(els[i].shadowRoot);\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "findShadowRoots(document);\n";

    private static final String SHADOW_DOM_CSS_SELECTOR_ALL = SHADOW_DOM_FINDER_PREFIX +
            "var results = [];\n" +
            "for (var i = 0; i < allRoots.length; i++) {\n" +
            "  try {\n" +
            "    var els = allRoots[i].querySelectorAll(arguments[0]);\n" +
            "    for (var j = 0; j < els.length; j++) { results.push(els[j]); }\n" +
            "  } catch(e) {}\n" +
            "}\n" +
            "return results;";

    private static final String SHADOW_DOM_TEXT_SELECTOR_ALL = SHADOW_DOM_FINDER_PREFIX +
            "var targetText = arguments[0].toLowerCase().trim();\n" +
            "var exactMatches = [];\n" +
            "var partialMatches = [];\n" +
            "for (var i = 0; i < allRoots.length; i++) {\n" +
            "  var els = allRoots[i].querySelectorAll('*');\n" +
            "  for (var j = 0; j < els.length; j++) {\n" +
            "    var el = els[j];\n" +
            "    if (el.closest && el.closest('#neo-ai-hud')) continue;\n" +
            "    var text = el.children.length === 0 && el.textContent ? el.textContent.toLowerCase().trim() : '';\n" +
            "    var val = el.value ? el.value.toLowerCase().trim() : '';\n" +
            "    var aria = el.getAttribute('aria-label') ? el.getAttribute('aria-label').toLowerCase().trim() : '';\n" +
            "    if (text === targetText || val === targetText || aria === targetText) {\n" +
            "      exactMatches.push(el);\n" +
            "    } else if (text.includes(targetText) || val.includes(targetText) || aria.includes(targetText)) {\n" +
            "      var score = text.length || val.length || aria.length;\n" +
            "      partialMatches.push({el: el, score: score});\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "if (exactMatches.length > 0) return exactMatches;\n" +
            "partialMatches.sort(function(a, b) { return a.score - b.score; });\n" +
            "var results = []; for(var k=0; k<partialMatches.length; k++) results.push(partialMatches[k].el);\n" +
            "return results;";

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

    private String cleanTarget(String target) {
        if (target == null || target.isBlank()) {
            throw new ActionExecutionException("Action target is null or empty");
        }

        // Clean up common AI hallucinations for Neodymium IDs
        if (target.startsWith("#xc_")) {
            target = target.substring(1);
            logDebug("   ⚠️ Auto-corrected AI hallucination: removed '#' from Neodymium ID [{}]", target);
        }

        // Clean up common AI hallucinations for Selenium prefixes
        if (target.toLowerCase().startsWith("xpath=")) {
            target = target.substring(6);
            logDebug("   ⚠️ Auto-corrected AI hallucination: removed 'xpath=' prefix [{}]", target);
        } else if (target.toLowerCase().startsWith("css=")) {
            target = target.substring(4);
            logDebug("   ⚠️ Auto-corrected AI hallucination: removed 'css=' prefix [{}]", target);
        }

        // Clean up AI hallucinations where the LLM wraps the ID in an attribute
        // selector
        if (target.contains("data-neo-ref=")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("data-neo-ref=['\"]?(xc_[a-zA-Z0-9_]+)['\"]?")
                    .matcher(target);
            if (m.find()) {
                target = m.group(1);
                logDebug("   ⚠️ Auto-corrected AI hallucination: extracted Neodymium ID from attribute selector [{}]",
                        target);
            }
        }

        return target;
    }

    public ActionExecutor(Object test) {
        this.test = test;
    }

    public void setVariable(String key, String value) {
        if (key != null && value != null) {
            executionVariables.put(key, value);
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

    private void interpolateAction(Action action) {
        if (action.getTarget() != null) {
            action.setTarget(interpolate(action.getTarget()));
        }
        if (action.getValues() != null) {
            List<String> newValues = new java.util.ArrayList<>();
            for (String val : action.getValues()) {
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

        AiActionPlugin plugin = ActionRegistry.getPlugin(action.getType());

        if (plugin != null) {
            try {
                plugin.execute(action, test, this);

                if (action.getElementContext() != null && !action.getElementContext().isEmpty()) {
                    LOG.debug("   ✅ Interacted with element: {}", action.getElementContext());
                }
            } finally {
                if (plugin != null) {
                    plugin.cleanup(action, this);
                }
            }
        } else {
            LOG.warn("Unsupported action type: {}", action.getType());
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
            Selenide.sleep(300);
        }
    }

    // --- Element resolution ---

    /**
     * Finds an element using multiple strategies in order of preference: 0.
     * data-neo-ref 1. CSS
     * selector 2. XPath 3. Link text / partial link text 4. Text content via XPath
     */
    public SelenideElement findElement(final Action action) {
        try {
            return findElements(action).first().should(Condition.exist, getElementTimeout());
        } catch (ActionExecutionException e) {
            // Rethrow using singular error message to maintain backward compatibility
            throw new ActionExecutionException(String.format("Could not find element for target '%s' or text '%s'", action.getTarget(),
                    action.getElementDetails()));
        }
    }

    /**
     * Finds all elements using the same strategies as findElement.
     */
    public com.codeborne.selenide.ElementsCollection findElements(final Action action) {
        switchFrameContext(action.getFrameId());
        String target = cleanTarget(action.getTarget());

        // Strategy 0: Direct Match for Neodymium Automation ID
        if (target.matches("^xc_.*")) {
            try {
                com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide
                        .$$(By.cssSelector("[data-neo-ref='" + target + "']"));
                if (!elements.isEmpty()) {
                    logDebug("   🔍 Resolved using Strategy 0: Neodymium Automation ID [{}]", target);
                    return elements;
                } else {
                    // Fallback to Shadow DOM deep selector via Javascript
                    java.util.List<WebElement> shadowWebEls = Selenide.executeJavaScript(SHADOW_DOM_CSS_SELECTOR_ALL,
                            "[data-neo-ref='" + target + "']");
                    if (shadowWebEls != null && !shadowWebEls.isEmpty()) {
                        logDebug("   🔍 Resolved using Strategy 0 (Shadow DOM JS): Neodymium Automation ID [{}]", target);
                        return com.codeborne.selenide.Selenide.$$(shadowWebEls);
                    }
                    logDebug("   ❌ Strategy 0 failed: Neodymium Automation ID [[data-neo-ref='{}']]", target);
                }
            } catch (Exception e) {
                logDebug("   ❌ Strategy 0 failed: Neodymium Automation ID [[data-neo-ref='{}']] with error: {}", target, e.getMessage());
            }
        }

        // Strategy 0.5: Try explicit Shadow DOM selector
        if (target.contains("::shadow")) {
            try {
                com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide
                        .$$(resolveLocator(target));
                if (!elements.isEmpty()) {
                    logDebug("   🔍 Resolved using Strategy 0.5: Shadow DOM selector [{}]", target);
                    return elements;
                } else {
                    logDebug("   ❌ Strategy 0.5 failed: Shadow DOM selector [{}]", target);
                }
            } catch (Exception e) {
                logDebug("   ❌ Strategy 0.5 failed: Shadow DOM selector [{}] with error: {}", target, e.getMessage());
            }
        }

        if (target.equals("document.title") || target.equals("pageTitle")) {
            logDebug("   🔍 Resolved using Title Strategy [{}]", target);
            return com.codeborne.selenide.Selenide.$$("head > title");
        }

        // Strategy 1: Try as CSS selector
        try {
            com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide
                    .$$(By.cssSelector(target));
            if (!elements.isEmpty()) {
                logDebug("   🔍 Resolved using Strategy 1: CSS selector [{}]", target);
                return elements;
            } else {
                logDebug("   ❌ Strategy 1 failed: CSS selector [{}]", target);
            }
        } catch (Exception e) {
            logDebug("   ❌ Strategy 1 failed: CSS selector [{}] with error: {}", target, e.getMessage());
        }

        // Strategy 1.5: Try deep Shadow DOM CSS selector via Javascript
        if (!target.startsWith("/") && !target.startsWith("(")) {
            try {
                java.util.List<WebElement> shadowWebEls = Selenide.executeJavaScript(SHADOW_DOM_CSS_SELECTOR_ALL,
                        target);
                if (shadowWebEls != null && !shadowWebEls.isEmpty()) {
                    logDebug("   🔍 Resolved using Strategy 1.5: Deep Shadow DOM CSS selector [{}]", target);
                    return com.codeborne.selenide.Selenide.$$(shadowWebEls);
                } else {
                    logDebug("   ❌ Strategy 1.5 failed: Deep Shadow DOM selector [{}]", target);
                }
            } catch (Exception e) {
                logDebug("   ❌ Strategy 1.5 failed: Deep Shadow DOM selector [{}] with error: {}", target, e.getMessage());
            }
        }

        // Strategy 2: Try as XPath
        if (target.startsWith("/") || target.startsWith("(")) {
            if (isValidXPath(target)) {
                try {
                    com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide.$$x(target);
                    if (!elements.isEmpty()) {
                        logDebug("   🔍 Resolved using Strategy 2: XPath [{}]", target);
                        return elements;
                    } else {
                        logDebug("   ❌ Strategy 2 failed: XPath [{}]", target);
                    }
                } catch (Exception e) {
                    logDebug("   ❌ Strategy 2 failed: XPath [{}] with error: {}", target, e.getMessage());
                }
            } else {
                LOG.debug("Target '{}' is not a valid XPath. Skipping XPath strategy.", target);
            }
        }

        // Strategy 3: Try as link text
        try
        {
            com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide
                    .$$(By.linkText(target));
            if (!elements.isEmpty())
            {
                logDebug("   🔍 Resolved using Strategy 3: Link text [{}]", target);
                return elements;
            }
            logDebug("   ❌ Strategy 3 failed: Link text [{}]", target);

            final String extractedTargetName = cleanElementText(target);
            if (extractedTargetName != null && !extractedTargetName.equals(target))
            {
                elements = com.codeborne.selenide.Selenide.$$(By.linkText(extractedTargetName));
                if (!elements.isEmpty())
                {
                    logDebug("   🔍 Resolved using Strategy 3 (extracted target name): Link text [{}]", extractedTargetName);
                    return elements;
                }
                logDebug("   ❌ Strategy 3 failed (extracted target name): Link text [{}]", extractedTargetName);
            }

            final String elementText = action.getElementDetails();
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target))
            {
                elements = com.codeborne.selenide.Selenide.$$(By.linkText(elementText));
                if (!elements.isEmpty())
                {
                    logDebug("   🔍 Resolved using Strategy 3: Link text [{}]", elementText);
                    return elements;
                }
                logDebug("   ❌ Strategy 3 failed: Link text [{}]", elementText);
            }
        }
        catch (final Exception e)
        {
            logDebug("   ❌ Strategy 3 failed: Link text resolution with error: {}", e.getMessage());
        }

        // Strategy 4: Try finding by text content via XPath
        try
        {
            final String targetXpath = escapeXpath(target);
            String xpath = String.format(
                    "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                    targetXpath, targetXpath, targetXpath);
            com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide.$$x(xpath);
            if (!elements.isEmpty())
            {
                logDebug("   🔍 Resolved using Strategy 4: Text content XPath [{}]", xpath);
                return elements;
            }
            logDebug("   ❌ Strategy 4 failed: Text content XPath [{}]", xpath);

            final String extractedTargetName = cleanElementText(target);
            if (extractedTargetName != null && !extractedTargetName.equals(target))
            {
                final String extractedXpath = escapeXpath(extractedTargetName);
                xpath = String.format(
                        "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                        extractedXpath, extractedXpath, extractedXpath);
                elements = com.codeborne.selenide.Selenide.$$x(xpath);
                if (!elements.isEmpty())
                {
                    logDebug("   🔍 Resolved using Strategy 4 (extracted target name): Text content XPath [{}]", xpath);
                    return elements;
                }
                logDebug("   ❌ Strategy 4 failed (extracted target name): Text content XPath [{}]", xpath);
            }

            final String elementText = cleanElementText(action.getElementDetails());
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target))
            {
                final String elementTextXpath = escapeXpath(elementText);
                xpath = String.format(
                        "//*[not(ancestor-or-self::*[@id='neo-ai-hud']) and (contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s))]",
                        elementTextXpath, elementTextXpath, elementTextXpath);
                elements = com.codeborne.selenide.Selenide.$$x(xpath);
                if (!elements.isEmpty())
                {
                    logDebug("   🔍 Resolved using Strategy 4: Text content XPath [{}]", xpath);
                    return elements;
                }
                logDebug("   ❌ Strategy 4 failed: Text content XPath [{}]", xpath);
            }
        }
        catch (final Exception e)
        {
            logDebug("   ❌ Strategy 4 failed: Text content search with error: {}", e.getMessage());
        }

        // Strategy 5: Try deep Shadow DOM text search via Javascript (covers what XPath can't)
        try
        {
            java.util.List<WebElement> shadowWebEls = Selenide.executeJavaScript(SHADOW_DOM_TEXT_SELECTOR_ALL, target);
            if (shadowWebEls != null && !shadowWebEls.isEmpty())
            {
                logDebug("   🔍 Resolved using Strategy 5: Deep Shadow DOM text search [{}]", target);
                return com.codeborne.selenide.Selenide.$$(shadowWebEls);
            }
            logDebug("   ❌ Strategy 5 failed: Deep Shadow DOM text search [{}]", target);

            final String extractedTargetName = cleanElementText(target);
            if (extractedTargetName != null && !extractedTargetName.equals(target))
            {
                shadowWebEls = Selenide.executeJavaScript(SHADOW_DOM_TEXT_SELECTOR_ALL, extractedTargetName);
                if (shadowWebEls != null && !shadowWebEls.isEmpty())
                {
                    logDebug("   🔍 Resolved using Strategy 5 (extracted target name): Deep Shadow DOM text search [{}]", extractedTargetName);
                    return com.codeborne.selenide.Selenide.$$(shadowWebEls);
                }
                logDebug("   ❌ Strategy 5 failed (extracted target name): Deep Shadow DOM text search [{}]", extractedTargetName);
            }

            final String elementText = cleanElementText(action.getElementDetails());
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target))
            {
                shadowWebEls = Selenide.executeJavaScript(SHADOW_DOM_TEXT_SELECTOR_ALL, elementText);
                if (shadowWebEls != null && !shadowWebEls.isEmpty())
                {
                    logDebug("   🔍 Resolved using Strategy 5: Deep Shadow DOM text search [{}]", elementText);
                    return com.codeborne.selenide.Selenide.$$(shadowWebEls);
                }
                logDebug("   ❌ Strategy 5 failed: Deep Shadow DOM text search [{}]", elementText);
            }
        }
        catch (final Exception e)
        {
            logDebug("   ❌ Strategy 5 failed: Deep Shadow DOM text search with error: {}", e.getMessage());
        }

        throw new ActionExecutionException(
                String.format("Could not find any elements for target '%s' or text '%s'", action.getTarget(),
                        action.getElementDetails()));
    }

    public By resolveLocator(final String target) {
        if (target.startsWith("/") || target.startsWith("(")) {
            return By.xpath(target);
        }
        if (target.contains("::shadow")) {
            String[] parts = target.split("::shadow");
            String shadowTarget = parts[parts.length - 1].trim();
            String[] shadowHosts = new String[parts.length - 1];
            for (int i = 0; i < parts.length - 1; i++) {
                shadowHosts[i] = parts[i].trim();
            }
            return Selectors.shadowCss(shadowTarget, shadowHosts);
        }
        return By.cssSelector(target);
    }

    private boolean isValidCssSelector(String target) {
        if (target == null || target.isBlank())
            return false;
        try {
            return Selenide.executeJavaScript(
                    "try { document.querySelector(arguments[0]); return true; } catch(e) { return false; }",
                    target);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidXPath(String target) {
        if (target == null || target.isBlank())
            return false;
        try {
            return Selenide.executeJavaScript(
                    "try { document.createExpression(arguments[0], null); return true; } catch(e) { return false; }",
                    target);
        } catch (Exception e) {
            return false;
        }
    }

    private String escapeXpath(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        } else if (!value.contains("\"")) {
            return "\"" + value + "\"";
        } else {
            return "concat('" + value.replace("'", "', \"'\", '") + "')";
        }
    }

    public void scrollIntoView(final SelenideElement element) {
        Selenide.executeJavaScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
        Selenide.sleep(200);
    }

    /**
     * Pre-checks if the action target exists and is visible before any interaction
     * is attempted. Use this for safely
     * replaying instructions from a Playbook.
     */
    @Step("Pre-checking action: {action.type}")
    public void preCheckAction(Action action) {
        AiActionPlugin plugin = ActionRegistry.getPlugin(action.getType());
        if (plugin != null) {
            plugin.preCheck(action, this);
        }
    }

    /**
     * Extracts useful DOM attributes from a SelenideElement to be used as context.
     * This helps the LLM self-heal
     * Playbooks by knowing what the element used to look like.
     */
    public Map<String, String> extractElementContext(SelenideElement element) {
        try {
            Map<String, String> context = new HashMap<>();
            context.put("tagName", element.getTagName());
            context.put("text", element.getText());
            context.put("id", element.getAttribute("id"));
            context.put("classes", element.getAttribute("class"));
            context.put("href", element.getAttribute("href"));
            context.put("name", element.getAttribute("name"));
            context.put("type", element.getAttribute("type"));
            context.put("placeholder", element.getAttribute("placeholder"));

            // Filter out nulls
            context.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());

            return context;
        } catch (Exception e) {
            LOG.warn("Failed to extract element context: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Exception thrown when an action execution fails.
     */
    public static class ActionExecutionException extends RuntimeException {
        public ActionExecutionException(final String message) {
            super(message);
        }

        public ActionExecutionException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class DataAttributeMatches extends WebElementCondition {

        private final Pattern namePattern;

        private final Pattern valuePattern;

        public DataAttributeMatches(String nameRegex, String valueRegex) {
            super("dataAttributeMatches");
            this.namePattern = Pattern.compile(nameRegex);
            this.valuePattern = Pattern.compile(valueRegex);
        }

        @Override
        public CheckResult check(Driver driver, WebElement element) {
            Map<String, String> attributes = driver.executeJavaScript(
                    "var items = {}; " +
                            "for (var i = 0, attrs = arguments[0].attributes; i < attrs.length; i++) { " +
                            "  items[attrs[i].name] = attrs[i].value; " +
                            "} "
                            + "return items;",
                    element);
            boolean found = false;

            for (var entry : attributes.entrySet()) {
                String name = entry.getKey();

                if (!namePattern.matcher(name).matches())
                    continue;

                String value = entry.getValue();

                if (value != null && valuePattern.matcher(value).matches()) {
                    found = true;
                }
            }

            return new CheckResult(found, attributes);
        }
    }

    public static class PartialTextContent extends WebElementCondition {

        private final String value;

        public PartialTextContent(String value) {
            super("PartialTextContent");
            this.value = value;
        }

        @Override
        public CheckResult check(Driver driver, WebElement element) {
            @Nullable
            String attribute = element.getAttribute("textContent");
            boolean found = attribute.contains(value);

            return new CheckResult(found, attribute);
        }
    }

    private void logDebug(String format, Object... args) {
        if (LOG.isDebugEnabled()) {
            String message = org.slf4j.helpers.MessageFormatter.arrayFormat(format, args).getMessage();
            if (actionLogs.add(message)) {
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
        final String[] parts = targetFrameId.split(":");
        if (parts.length != 2)
        {
            return;
        }
        
        String windowHandle = parts[0];
        final String framePath = parts[1];
        
        final org.openqa.selenium.WebDriver driver = com.codeborne.selenide.WebDriverRunner.getWebDriver();
        try
        {
            final java.util.Set<String> activeHandles = driver.getWindowHandles();
            if (!activeHandles.contains(windowHandle))
            {
                if (!windowHandleMapping.containsKey(windowHandle))
                {
                    final int nextIndex = windowHandleMapping.size();
                    final java.util.List<String> activeList = new java.util.ArrayList<>(activeHandles);
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
            driver.switchTo().window(windowHandle);
            driver.switchTo().defaultContent();
            
            if (!"main".equals(framePath))
            {
                final String[] indices = framePath.split("\\.");
                for (final String indexStr : indices)
                {
                    if (!indexStr.equals("main") && !indexStr.isBlank())
                    {
                        final int index = Integer.parseInt(indexStr);
                        driver.switchTo().frame(index);
                    }
                }
            }
        }
        catch (final Exception e)
        {
            logDebug("   ⚠️ Could not switch to frame {}: {}", targetFrameId, e.getMessage());
        }
    }
}
