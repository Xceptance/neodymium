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

    private static final Duration ELEMENT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * The instance of the currently running Test Class
     */
    private Object test;

    public ActionExecutor(Object test) {
        this.test = test;
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
        LOG.debug("   🤖 {}", action.getDescription());

        preCheckAction(action);

        AiActionPlugin plugin = ActionRegistry.getPlugin(action.getType());

        if (plugin != null) {
            try {
                plugin.execute(action, test, this);
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
            LOG.debug("   --------------------------------------------------------");
            LOG.debug("▶️ [EXEC] Executing Action [{}/{}]: {}", i + 1, actions.size(), action.getType());

            execute(action);

            // Small pause between actions for page stability
            Selenide.sleep(300);
        }
    }

    // --- Element resolution ---

    /**
     * Finds an element using multiple strategies in order of preference: 0.
     * data-neodymium-automation-id 1. CSS
     * selector 2. XPath 3. Link text / partial link text 4. Text content via XPath
     */
    public SelenideElement findElement(final Action action) {
        String target = action.getTarget();
        if (target == null || target.isBlank()) {
            throw new ActionExecutionException("Action target is null or empty");
        }

        // Strategy 0: Direct Match for Neodymium Automation ID
        if (target.matches("^xc_.*")) {
            try {
                SelenideElement element = $(By.cssSelector("[data-neodymium-automation-id='" + target + "']"));
                if (element.exists()) {
                    return element.should(Condition.exist, ELEMENT_TIMEOUT);
                } else {
                    LOG.debug("Automation ID match failed for target '{}' and text '{}'", target,
                            action.getElementDetails());
                }
            } catch (final Exception e) {
                LOG.debug("Automation ID match failed for target '{}' and text '{}' with message: {}", target,
                        action.getElementDetails(), e.getMessage());
            }
        }

        // Strategy 0.5: Try explicit Shadow DOM selector
        if (target.contains("::shadow")) {
            String[] parts = target.split("::shadow");
            String shadowTarget = parts[parts.length - 1].trim();
            String[] shadowHosts = new String[parts.length - 1];
            for (int i = 0; i < parts.length - 1; i++) {
                shadowHosts[i] = parts[i].trim();
            }
            try {
                SelenideElement element = $(Selectors.shadowCss(shadowTarget, shadowHosts));
                if (element.exists()) {
                    return element.should(Condition.exist, ELEMENT_TIMEOUT);
                } else {
                    LOG.debug("Explicit Shadow DOM selector failed for target '{}' and text '{}'", target,
                            action.getElementDetails());
                }
            } catch (final Exception e) {
                LOG.debug("Explicit Shadow DOM selector failed for target '{}' and text '{}' with message: {}", target,
                        action.getElementDetails(), e.getMessage());
            }
        }

        // In some cases the ai tries this, so just give it what it wants
        if (action.getTarget().equals("document.title") || action.getTarget().equals("pageTitle")) {
            return $("head > title");
        }

        // Strategy 1: Try as CSS selector
        try {
            SelenideElement element = $(By.cssSelector(target));
            if (element.exists()) {
                return element.should(Condition.exist, ELEMENT_TIMEOUT);
            } else {
                LOG.debug("CSS selector failed for target '{}' and text '{}'", target, action.getElementDetails());
            }
        } catch (final Exception e) {
            LOG.debug("CSS selector failed for target '{}' and text '{}' with message: {}", target,
                    action.getElementDetails(), e.getMessage());
        }

        // Strategy 1.5: Try deep Shadow DOM CSS selector
        if (!target.startsWith("/") && !target.startsWith("(")) {
            try {
                SelenideElement element = $(Selectors.shadowDeepCss(target));
                if (element.exists()) {
                    return element.should(Condition.exist, ELEMENT_TIMEOUT);
                } else {
                    LOG.debug("Deep Shadow DOM selector failed for target '{}' and text '{}'", target,
                            action.getElementDetails());
                }
            } catch (final Exception e) {
                LOG.debug("Deep Shadow DOM selector failed for target '{}' and text '{}' with message: {}", target,
                        action.getElementDetails(), e.getMessage());
            }
        }

        // Strategy 2: Try as XPath
        if (target.startsWith("/") || target.startsWith("(")) {
            try {
                SelenideElement element = $(By.cssSelector(target));
                if (element.exists()) {
                    return element.should(Condition.exist, ELEMENT_TIMEOUT);
                } else {
                    LOG.debug("CSS selector failed for target '{}' and text '{}'", target, action.getElementDetails());
                }
            } catch (final Exception e) {
                LOG.debug("CSS selector failed for target '{}' and text '{}' with message: {}", target,
                        action.getElementDetails(), e.getMessage());
            }
        } else {
            LOG.debug("Target '{}' is not a valid CSS selector. Skipping CSS strategy.", target);
        }

        // Strategy 2: Try as XPath
        if (target.startsWith("/") || target.startsWith("(")) {
            if (isValidXPath(target)) {
                try {
                    SelenideElement element = $x(target);
                    if (element.exists()) {
                        return element.should(Condition.exist, ELEMENT_TIMEOUT);
                    } else {
                        LOG.debug("XPath failed for target '{}' and text '{}'", target, action.getElementDetails());
                    }
                } catch (final Exception e) {
                    LOG.debug("XPath failed for target '{}' and text '{}' with message: {}", target,
                            action.getElementDetails(), e.getMessage());
                }
            } else {
                LOG.debug("Target '{}' is not a valid XPath. Skipping XPath strategy.", target);
            }
        }

        // Strategy 3: Try as link text (first target, then element text)
        try {
            SelenideElement element = $(By.linkText(target));
            if (element.exists()) {
                return element.should(Condition.exist, ELEMENT_TIMEOUT);
            }
            LOG.debug("Link text failed for target '{}'", target);

            final String elementText = action.getElementDetails();
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target)) {
                element = $(By.linkText(elementText));
                if (element.exists()) {
                    return element.should(Condition.exist, ELEMENT_TIMEOUT);
                }
                LOG.debug("Link text failed for text '{}'", elementText);
            }
        } catch (final Exception e) {
            LOG.debug("Link text resolution failed with message: {}", e.getMessage());
        }

        // Strategy 4: Try finding by text content via XPath (first target, then element
        // text)
        try {
            // First try with target
            String targetXpath = escapeXpath(target);
            String xpath = String.format(
                    "//*[contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s)]",
                    targetXpath, targetXpath, targetXpath);
            SelenideElement element = $x(xpath);
            if (element.exists()) {
                return element.should(Condition.exist, ELEMENT_TIMEOUT);
            }
            LOG.debug("Text content search failed for target '{}'", target);

            // Then try with element text if available and different
            final String elementText = action.getElementDetails();
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target)) {
                String elementTextXpath = escapeXpath(elementText);
                xpath = String.format(
                        "//*[contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s)]",
                        elementTextXpath, elementTextXpath, elementTextXpath);
                element = $x(xpath);
                if (element.exists()) {
                    return element.should(Condition.exist, ELEMENT_TIMEOUT);
                }
                LOG.debug("Text content search failed for text '{}'", elementText);
            }

        } catch (final Exception e) {
            LOG.debug("Text content search failed with message: {}", e.getMessage());
        }

        throw new ActionExecutionException(String.format("Could not find element for target '%s' or text '%s'", target,
                action.getElementDetails()));
    }

    /**
     * Finds all elements using the same strategies as findElement.
     */
    public com.codeborne.selenide.ElementsCollection findElements(final Action action) {
        String target = action.getTarget();
        if (target == null || target.isBlank()) {
            throw new ActionExecutionException("Action target is null or empty");
        }

        // Strategy 0: Direct Match for Neodymium Automation ID
        if (target.matches("^xc_.*")) {
            try {
                com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide.$$(By.cssSelector("[data-neodymium-automation-id='" + target + "']"));
                if (!elements.isEmpty()) return elements;
            } catch (Exception e) {}
        }

        // Strategy 0.5: Try explicit Shadow DOM selector
        if (target.contains("::shadow")) {
            try {
                com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide.$$(resolveLocator(target));
                if (!elements.isEmpty()) return elements;
            } catch (Exception e) {}
        }

        if (target.equals("document.title") || target.equals("pageTitle")) {
            return com.codeborne.selenide.Selenide.$$("head > title");
        }

        // Strategy 1: Try as CSS selector
        try {
            com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide.$$(By.cssSelector(target));
            if (!elements.isEmpty()) return elements;
        } catch (Exception e) {}

        // Strategy 2: Try as XPath
        if (target.startsWith("/") || target.startsWith("(")) {
            if (isValidXPath(target)) {
                try {
                    com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide.$$x(target);
                    if (!elements.isEmpty()) return elements;
                } catch (Exception e) {}
            }
        }

        // Strategy 3: Try as link text
        try {
            com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide.$$(By.linkText(target));
            if (!elements.isEmpty()) return elements;

            final String elementText = action.getElementDetails();
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target)) {
                elements = com.codeborne.selenide.Selenide.$$(By.linkText(elementText));
                if (!elements.isEmpty()) return elements;
            }
        } catch (Exception e) {}

        // Strategy 4: Try finding by text content via XPath
        try {
            String targetXpath = escapeXpath(target);
            String xpath = String.format(
                    "//*[contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s)]",
                    targetXpath, targetXpath, targetXpath);
            com.codeborne.selenide.ElementsCollection elements = com.codeborne.selenide.Selenide.$$x(xpath);
            if (!elements.isEmpty()) return elements;

            final String elementText = action.getElementDetails();
            if (elementText != null && !elementText.isBlank() && !elementText.equals(target)) {
                String elementTextXpath = escapeXpath(elementText);
                xpath = String.format(
                        "//*[contains(normalize-space(text()), %s) or contains(@value, %s) or contains(@aria-label, %s)]",
                        elementTextXpath, elementTextXpath, elementTextXpath);
                elements = com.codeborne.selenide.Selenide.$$x(xpath);
                if (!elements.isEmpty()) return elements;
            }
        } catch (Exception e) {}

        throw new ActionExecutionException(String.format("Could not find any elements for target '%s' or text '%s'", target,
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
}
