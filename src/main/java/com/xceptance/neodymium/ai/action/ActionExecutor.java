package com.xceptance.neodymium.ai.action;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.ex.ElementShould;

import io.qameta.allure.Step;

/**
 * Translates {@link Action} objects into Selenium WebDriver calls. Uses smart
 * element resolution that tries multiple
 * strategies.
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
        LOG.debug("▶ {} — {}", action.getType(), action.getDescription());

        preCheckAction(action);

        switch (action.getType()) {
            case NAVIGATE:
                executeNavigate(action);
                break;
            case CLICK:
                executeClick(action);
                break;
            case TYPE:
                executeType(action);
                break;
            case CLEAR:
                executeClear(action);
                break;
            case SELECT:
                executeSelect(action);
                break;
            case ASSERT:
                executeAssert(action);
                break;
            case WAIT:
                executeWait(action);
                break;
            case SCROLL:
                executeScroll(action);
                break;
            case HOVER:
                executeHover(action);
                break;
            case KEY_PRESS:
                executeKeyPress(action);
                break;
            case BACK:
                executeBack(action);
                break;
            case FORWARD:
                executeForward(action);
                break;
            case REFRESH:
                executeRefresh(action);
                break;
            case CLEAR_COOKIES:
                executeClearCookies(action);
                break;
            case JAVA_METHOD:
                executeJavaMethod(action);
                break;
            default:
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
            LOG.debug("Action [{}/{}]", i + 1, actions.size());

            execute(action);

            // Small pause between actions for page stability
            Selenide.sleep(300);
        }
    }

    // --- Action implementations ---

    private void executeNavigate(final Action action) {
        final String url = action.getValue();
        if (url == null || url.isBlank()) {
            throw new ActionExecutionException("NAVIGATE action requires a 'value' (URL)");
        }
        LOG.debug("Navigating to: {}", url);
        Selenide.open(url);

        // Wait for page load
        Selenide.Wait().until(
                d -> Selenide.executeJavaScript("return document.readyState").equals("complete"));
    }

    private void executeClick(final Action action) {
        final SelenideElement element = findElement(action);
        action.setElementContext(extractElementContext(element));
        scrollIntoView(element);
        try {
            element.click();
        } catch (final ElementClickInterceptedException e) {
            throw new ActionExecutionException(String.format(
                    "Click intercepted on target '%s' (element: '%s'): another element is covering it. "
                            + "Please choose a different, more specific target — ideally the innermost visible "
                            + "child element or a sibling that is not obscured.",
                    action.getTarget(), action.getElementDetails()), e);
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutionException(String.format(
                    "Element not interactable or not enabled for target '%s' (element: '%s'). "
                            + "The chosen element cannot be clicked in its current state. "
                            + "Please select a different, directly clickable element.",
                    action.getTarget(), action.getElementDetails()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutionException(String.format(
                    "Element target '%s' (element: '%s') became stale. "
                            + "The page may have updated. Re-analyzing and retrying...",
                    action.getTarget(), action.getElementDetails()), e);
        } catch (ElementShould t) {
            if (t.getMessage().contains("Element should be clickable: interactable and enabled")) {
                throw new ActionExecutionException(String.format(
                        "Element not interactable or not enabled for target '%s' (element: '%s'). "
                                + "The chosen element cannot be clicked in its current state. "
                                + "Please select a different, directly clickable element.",
                        action.getTarget(), action.getElementDetails()), t);

            }
        }
    }

    private void executeType(final Action action) {
        final SelenideElement element = findElement(action);
        action.setElementContext(extractElementContext(element));
        scrollIntoView(element);
        try {
            element.clear();
            element.sendKeys(action.getValue());
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutionException(String.format(
                    "Element not interactable or not enabled for target '%s' (element: '%s'). "
                            + "The chosen element cannot accept text in its current state. "
                            + "Please select a different, input-ready element.",
                    action.getTarget(), action.getElementDetails()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutionException(String.format(
                    "Element target '%s' (element: '%s') became stale. "
                            + "The page may have updated. Re-analyzing and retrying...",
                    action.getTarget(), action.getElementDetails()), e);
        }
    }

    private void executeClear(final Action action) {
        final SelenideElement element = findElement(action);
        action.setElementContext(extractElementContext(element));
        try {
            element.clear();
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutionException(String.format(
                    "Element not interactable or not enabled for target '%s' (element: '%s'). "
                            + "The chosen element cannot be cleared in its current state.",
                    action.getTarget(), action.getElementDetails()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutionException(String.format(
                    "Element target '%s' (element: '%s') became stale. "
                            + "The page may have updated. Re-analyzing and retrying...",
                    action.getTarget(), action.getElementDetails()), e);
        }
    }

    private void executeSelect(final Action action) {
        final SelenideElement element = findElement(action);
        action.setElementContext(extractElementContext(element));
        scrollIntoView(element);
        try {
            element.selectOption(action.getValue());
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutionException(String.format(
                    "Element not interactable or not enabled for target '%s' (element: '%s'). "
                            + "The chosen element cannot be selected in its current state.",
                    action.getTarget(), action.getElementDetails()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutionException(String.format(
                    "Element target '%s' (element: '%s') became stale. "
                            + "The page may have updated. Re-analyzing and retrying...",
                    action.getTarget(), action.getElementDetails()), e);
        }
    }

    private void executeAssert(final Action action) {
        final String expected = action.getValue();

        final SelenideElement element = findElement(action);

        if (expected == null) {
            element.should(Condition.exist);
            LOG.debug("✓ Element exists: {}", action);
            return;
        }

        try {
            if ("visible".equals(expected)) {
                element.shouldBe(Condition.visible);
            } else {
                // Expand the "OR" condition to cover common web patterns
                element.should(Condition.or("Assertion for " + expected,
                        Condition.exactText(expected),
                        Condition.partialText(expected),
                        Condition.value(expected),
                        new PartialTextContent(expected), // This is to
                                                          // get the text
                                                          // content for
                                                          // the page
                                                          // title, which
                                                          // is not
                                                          // listening on
                                                          // partialText
                        Condition.attribute("href", expected),
                        Condition.attribute("alt", expected),
                        Condition.attribute("src", expected),
                        Condition.attribute("title", expected),
                        Condition.attribute("placeholder", expected),
                        // This covers custom data attributes like data-id or data-test-id
                        new DataAttributeMatches("data-.*", ".*" + Pattern.quote(expected) + ".*")));

            }
            LOG.debug("✓ Assertion passed for: '{}'", expected);
        } catch (Throwable e) {
            // Log the actual attributes to help debug why the AI failed
            String actualDetails = String.format("Text: '%s', Value: '%s', Alt: '%s'",
                    element.getText(), element.getValue(), element.getAttribute("alt"));

            throw new ActionExecutionException(
                    String.format("Assertion failed: '%s' not found in common attributes. Found: [%s]",
                            expected, actualDetails),
                    e);
        }
    }

    private void executeWait(final Action action) {
        try {
            final int ms = action.getValue() != null ? Integer.parseInt(action.getValue()) : 1000;
            LOG.debug("Waiting {} ms", ms);
            Selenide.sleep(ms);
        } catch (final NumberFormatException e) {
            // If value isn't a number, treat it as waiting for an element
            if (action != null) {
                $(resolveLocator(action.getTarget())).shouldBe(Condition.visible, ELEMENT_TIMEOUT);
            }
        }
    }

    private void executeHover(final Action action) {
        final SelenideElement element = findElement(action);
        action.setElementContext(extractElementContext(element));
        scrollIntoView(element);
        try {
            element.hover();
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutionException(String.format(
                    "Element not interactable or not enabled for target '%s' (element: '%s'). "
                            + "The chosen element cannot be hovered in its current state.",
                    action.getTarget(), action.getElementDetails()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutionException(String.format(
                    "Element target '%s' (element: '%s') became stale. "
                            + "The page may have updated. Re-analyzing and retrying...",
                    action.getTarget(), action.getElementDetails()), e);
        }
    }

    private void executeScroll(final Action action) {
        if (action != null) {
            final SelenideElement element = findElement(action);
            scrollIntoView(element);
        } else {
            // Scroll down by viewport height
            Selenide.executeJavaScript("window.scrollBy(0, window.innerHeight)");
        }
    }

    private void executeKeyPress(final Action action) {
        final String key = action.getValue();
        if (key == null) {
            throw new ActionExecutionException("KEY_PRESS action requires a 'value' (key name)");
        }

        final Keys seleniumKey = mapKey(key);
        try {
            if (action.getTarget() != null) {
                findElement(action).sendKeys(seleniumKey);
            } else {
                // Send to the active element
                Selenide.actions().sendKeys(seleniumKey).perform();
            }
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutionException(String.format(
                    "Target element not interactable or not enabled for target '%s' (element: '%s'). "
                            + "The chosen element cannot receive key presses.",
                    action.getTarget(), action.getElementDetails()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutionException(String.format(
                    "Element target '%s' (element: '%s') became stale. "
                            + "The page may have updated. Re-analyzing and retrying...",
                    action.getTarget(), action.getElementDetails()), e);
        }
    }

    private void executeBack(final Action action) {
        LOG.debug("Navigating back");
        Selenide.back();
    }

    private void executeForward(final Action action) {
        LOG.debug("Navigating forward");
        Selenide.forward();
    }

    private void executeRefresh(final Action action) {
        LOG.debug("Refreshing page");
        Selenide.refresh();
    }

    private void executeClearCookies(final Action action) {
        LOG.debug("Clearing cookies");
        Selenide.clearBrowserCookies();
        // Also clearing local storage as it is often expected when "clearing cookies"
        // in modern web
        Selenide.clearBrowserLocalStorage();
    }

    /**
     * Invokes a Java method via reflection.
     * <p>
     * Expected action fields:
     * <ul>
     * <li>{@code target} – simple method name in from the current test class.</li>
     * <li>{@code value} – the single {@link String} argument passed to the method
     * (may be {@code null} if the method
     * accepts no arguments or accepts a nullable parameter).</li>
     * </ul>
     * <p>
     * Resolution order:
     * <ol>
     * <li>Try calling the method as a <em>static</em> method. Signature:
     * {@code public static void/Object myMethod(String)}</li>
     * <li>If no static method is found, call the method as an instance method.</li>
     * </ol>
     */
    private void executeJavaMethod(final Action action) {
        final String target = action.getTarget();
        if (target == null || target.isBlank()) {
            throw new ActionExecutionException("JAVA_METHOD action requires a 'target' containing only the class name");
        }

        final int lastDot = target.lastIndexOf('.');
        if (lastDot >= 0) {
            throw new ActionExecutionException(
                    "JAVA_METHOD target can not be fully qualified. Only use simple method name, got: " + target);
        }

        final String methodName = target;
        final String param = action.getValue(); // may be null

        LOG.debug("JAVA_METHOD: method='{}', param='{}'", methodName, param);

        try {
            Method method = null;
            boolean isStatic = false;
            boolean hasParam = false;
            Class<? extends Object> clazz = test.getClass();
            try {
                method = clazz.getMethod(methodName, String.class);
                isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());
                hasParam = true;
            } catch (final NoSuchMethodException e) {
                try {
                    method = clazz.getMethod(methodName);
                    isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());
                    hasParam = false;
                } catch (final NoSuchMethodException e1) {
                    throw new ActionExecutionException(
                            String.format("JAVA_METHOD: no public method '%s(String)' on class '%s'", methodName,
                                    test.getClass().getSimpleName()),
                            e);
                }
            }

            // run static
            if (isStatic) {
                if (hasParam) {
                    LOG.debug("Invoking static method {}(\"{}\")", methodName, param);
                    method.invoke(null, param);
                } else {
                    LOG.debug("Invoking static method {}()", methodName);
                    method.invoke(null);
                }
                return;
            }

            if (hasParam) {
                LOG.debug("Invoking method {}(\"{}\")", methodName, param);
                method.invoke(test, param);
            } else {
                LOG.debug("Invoking method {}()", methodName);
                method.invoke(test);
            }

        } catch (final java.lang.reflect.InvocationTargetException e) {
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new ActionExecutionException(String.format("JAVA_METHOD: '%s.%s' threw an exception: %s",
                    test.getClass().getSimpleName(), methodName,
                    cause.getMessage()), cause);
        } catch (final Exception e) {
            throw new ActionExecutionException(String.format("JAVA_METHOD: failed to invoke '%s.%s': %s",
                    test.getClass().getSimpleName(), methodName,
                    e.getMessage()), e);
        }
    }

    // --- Element resolution ---

    /**
     * Finds an element using multiple strategies in order of preference: 0.
     * data-neodymium-automation-id 1. CSS
     * selector 2. XPath 3. Link text / partial link text 4. Text content via XPath
     */
    SelenideElement findElement(final Action action) {
        String target = action.getTarget();
        if (target == null || target.isBlank()) {
            throw new ActionExecutionException("Action target is null or empty");
        }

        // Strategy 0: Direct Match for Neodymium Automation ID
        if (target.matches("^xc_.*")) {
            try {
                SelenideElement element = $(By.cssSelector(String.format("[data-neodymium-automation-id=\"%s\"]", target.replace("\"", "\\\""))));
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

        // Strategy 2: Try as XPath
        if (target.startsWith("/") || target.startsWith("(")) {
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

    private By resolveLocator(final String target) {
        if (target.startsWith("/") || target.startsWith("(")) {
            return By.xpath(target);
        }
        return By.cssSelector(target);
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

    private void scrollIntoView(final SelenideElement element) {
        Selenide.executeJavaScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
        Selenide.sleep(200);
    }

    private Keys mapKey(final String keyName) {
        return switch (keyName.toUpperCase()) {
            case "ENTER", "RETURN" -> Keys.ENTER;
            case "TAB" -> Keys.TAB;
            case "ESCAPE", "ESC" -> Keys.ESCAPE;
            case "BACKSPACE" -> Keys.BACK_SPACE;
            case "DELETE" -> Keys.DELETE;
            case "SPACE" -> Keys.SPACE;
            case "ARROW_UP", "UP" -> Keys.ARROW_UP;
            case "ARROW_DOWN", "DOWN" -> Keys.ARROW_DOWN;
            case "ARROW_LEFT", "LEFT" -> Keys.ARROW_LEFT;
            case "ARROW_RIGHT", "RIGHT" -> Keys.ARROW_RIGHT;
            default -> throw new ActionExecutionException("Unknown key: " + keyName);
        };
    }

    /**
     * Pre-checks if the action target exists and is visible before any interaction
     * is attempted. Use this for safely
     * replaying instructions from a Playbook.
     */
    @Step("Pre-checking action: {action.type}")
    public void preCheckAction(Action action) {
        switch (action.getType()) {
            case CLICK:
            case TYPE:
            case CLEAR:
            case SELECT:
            case HOVER:
                SelenideElement element = findElement(action);
                if (!element.exists() || !element.isDisplayed()) {
                    throw new ActionExecutionException(String.format(
                            "Pre-check failed: Target '%s' is not visible or doesn't exist.", action.getTarget()));
                }
                break;
            default:
                // No precheck for NAVIGATE, BACK, FORWARD, WAIT, ASSERT, JAVA_METHOD etc.
                break;
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
