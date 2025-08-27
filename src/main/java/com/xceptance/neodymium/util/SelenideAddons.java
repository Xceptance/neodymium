package com.xceptance.neodymium.util;

import com.codeborne.selenide.AssertionMode;
import com.codeborne.selenide.CheckResult;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.WebElementCondition;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.impl.Html;
import com.codeborne.selenide.impl.WebElementsCollectionWrapper;
import com.codeborne.selenide.logevents.SelenideLogger;

import io.qameta.allure.Allure;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException;

import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Additional helpers for limits chained lookup in Selenide. Contribute that later back to Selenide if it proves to
 * work, so it can become API or better fully integrated so we don't need that workaround concept.
 */
public class SelenideAddons
{
    private static final String SERE = StaleElementReferenceException.class.getSimpleName();

    /**
     * Returns an supplier that will return exactly one result if any. It will return an element that is found by
     * parentSelector and has a result for subElementSelector. It does NOT return the subelements, it is meant to be a
     * workaround for poor CSS where the parent is only recognizable by looking at its children, but we don't need the
     * children. Important, this is meant to be lazy so don't call get() when you setup a field or so, only when you
     * really need the element. It reselects all the time!
     *
     * @param parentSelector
     *            The selector that is used to find subElementSelector
     * @param subElementSelector
     *            The selector that is shall have the element selected by parentSelector
     * @return an supplier that will return the result later
     */
    public static Supplier<SelenideElement> parentBySubElement(final By parentSelector, final By subElementSelector)
    {
        return new Supplier<SelenideElement>()
        {
            @Override
            public SelenideElement get()
            {
                return $$(parentSelector).asDynamicIterable().stream().filter(e -> {
                    return e.$(subElementSelector).exists();
                }).findFirst().get();
            };
        };
    }

    /**
     * Returns an supplier that will return all results if any. It will return elements that are found by parentSelector
     * and have a result for subElementSelector. It does NOT return the subelements, it is meant to be a workaround for
     * poor CSS where the parent is only recognizable by looking at its children, but we don't need the children.
     * Important, this is meant to be lazy so don't call get() when you setup a field or so, only when you really need
     * the element. It reselects all the time!
     *
     * @param parentSelector
     *            The selector that is used to find subElementSelector
     * @param subElementSelector
     *            The selector that is shall have the element selected by parentSelector
     * @return an supplier that will return the result later
     */
    public static Supplier<ElementsCollection> parentsBySubElement(final By parentSelector, final By subElementSelector)
    {
        return new Supplier<ElementsCollection>()
        {
            @Override
            public ElementsCollection get()
            {
                List<SelenideElement> list = $$(parentSelector).asDynamicIterable().stream().filter(e -> {
                    return e.$(subElementSelector).exists();
                }).collect(Collectors.toList());
                return new ElementsCollection(new WebElementsCollectionWrapper(WebDriverRunner.driver(), list));
            };
        };
    }

    /**
     * Returns an supplier that will return all results if any. It will return elements that are found by parentSelector
     * and have a no result for subElementSelector. It does NOT return the subelements, it is meant to be a workaround
     * for poor CSS where the parent is only recognizable by looking at its children, but we don't need the children.
     * Important, this is meant to be lazy so don't call get() when you setup a field or so, only when you really need
     * the element. It reselects all the time!
     *
     * @param parentSelector
     *            The selector that is used to find subElementSelector
     * @param subElementSelector
     *            The selector that shall not be contained by the parentSelector
     * @return an supplier that will return the result later
     */
    public static Supplier<ElementsCollection> parentsWithoutSubElement(final By parentSelector, final By subElementSelector)
    {
        return new Supplier<ElementsCollection>()
        {
            @Override
            public ElementsCollection get()
            {
                List<SelenideElement> list = $$(parentSelector).asDynamicIterable().stream().filter(e -> {
                    return !e.$(subElementSelector).exists();
                }).collect(Collectors.toList());
                return new ElementsCollection(new WebElementsCollectionWrapper(WebDriverRunner.driver(), list));
            };
        };
    }

    /**
     * Executes the given code at least once but potentially multiple times as long as a
     * {@link StaleElementReferenceException} occurs.
     * <p>
     * Attention: Since the SelenideElement class implements the InvocationHandler interface you have to make sure that
     * the element is retrieved in order to provoke a StaleElementReferenceException. You can do this by calling a
     * should function that uses a condition.
     * </p>
     * <p>
     * The following settings can be configured within the Neodymium configuration to tune the retry behavior:
     * </p>
     * <ul>
     * <li>neodymium.selenideAddons.staleElement.retry.count (default 3 retries)</li>
     * <li>neodymium.selenideAddons.staleElement.retry.timeout (default 500ms pause between retries)</li>
     * </ul>
     * <p>
     * <b>Example:</b>
     * </p>
     * 
     * <pre>
     * SelenideAddons.$safe(() -&gt; {
     *     return $("selector").should(exist);
     * });
     * </pre>
     *
     * @param code
     *            the code to run
     * @return the element of the execution or any exception that might bubble up
     */
    public static SelenideElement $safe(final Supplier<SelenideElement> code)
    {
        final int maxRetryCount = Neodymium.configuration().staleElementRetryCount();
        int retryCounter = 0;

        while (retryCounter <= maxRetryCount)
        {
            try
            {
                return code.get();
            }
            catch (final Throwable t)
            {
                if (t instanceof StaleElementReferenceException || isThrowableCausedBy(t, StaleElementReferenceException.class, SERE))
                {
                    retryCounter++;
                    if (retryCounter > maxRetryCount)
                    {
                        // fail
                        throw t;
                    }
                    else
                    {
                        AllureAddons.addToReport(SERE + " catched times: \"" + retryCounter + "\".", retryCounter);
                        Selenide.sleep(Neodymium.configuration().staleElementRetryTimeout());
                    }
                }
                else
                {
                    // not the kind of error we are looking for
                    throw t;
                }
            }
        }

        // never get here
        return null;
    }

    /**
     * Executes the given code at least once but potentially multiple times as long as a
     * {@link StaleElementReferenceException} occurs.
     * <p>
     * The following settings can be configured within the Neodymium configuration to tune the retry behavior:
     * </p>
     * <ul>
     * <li>neodymium.selenideAddons.staleElement.retry.count (default 3 retries)</li>
     * <li>neodymium.selenideAddons.staleElement.retry.timeout (default 500ms pause between retries)</li>
     * </ul>
     * <p>
     * <b>Example:</b>
     * </p>
     * 
     * <pre>
     * SelenideAddons.$safe(() -&gt; {
     *     $("selectorOne").find("selectorTwo").shouldBe(visible);
     * });
     * </pre>
     * 
     * @param code
     *            the code to run
     */
    public static void $safe(final Runnable code)
    {
        $safe(() -> {
            code.run();
            return null;
        });
    }

    /**
     * Recursively checks if the throwable is caused by exception of the specific class or contains one of the messages
     * listed in the {@code phrasesHintingErrorToCatch}
     * 
     * @param throwable
     *            throwable to check
     * @param clazz
     *            class of the expected throwable cause
     * @param phrasesHintingErrorToCatch
     *            optional parameters, error messages of expected throwable or its causes
     * @return result of the check as boolean value
     */
    public static boolean isThrowableCausedBy(final Throwable throwable, Class<? extends Throwable> clazz, String... phrasesHintingErrorToCatch)
    {
        Throwable t = throwable;
        while (t != null)
        {
            boolean containsMessage = false;
            for (String message : phrasesHintingErrorToCatch)
            {
                String messageText = t.getMessage();
                if (messageText != null && messageText.contains(message))
                {
                    containsMessage = true;
                    break;
                }
            }
            if (clazz.isInstance(t) || containsMessage)
            {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * The missing regular expression condition for value attributes.<br>
     * <br>
     * <p>
     * Sample: <code>$("input").waitWhile(matchesValue("foo"), 12000)</code>
     * </p>
     * 
     * @param text
     *            The text that should be contained within the value attribute
     * @return a Selenide {@link Condition}
     * @see #matchValue(String)
     */
    public static WebElementCondition matchesValue(String text)
    {
        return matchValue(".*" + text + ".*");
    }

    /**
     * The missing regular expression condition for value attributes.<br>
     * <br>
     * <p>
     * Sample: Assert that given element's value attribute matches given regular expression
     * <code>$("input").should(matchValue("Hello\s*John"))</code>
     * </p>
     *
     * @param regex
     *            e.g. Kicked.*Chuck Norris - in this case ".*" can contain any characters including spaces, tabs, CR
     *            etc.
     * @return a Selenide {@link Condition}
     */
    public static WebElementCondition matchValue(final String regex)
    {
        return Condition.attributeMatching("value", regex);
    }

    /**
     * The missing regular expression condition for attributes.<br>
     * <br>
     * <p>
     * Sample: <code>$("input").waitWhile(matchesValue("foo"), 12000)</code>
     * </p>
     * 
     * @deprecated Not needed anymore since it's supported by Selenide. Will be removed with the next major version. Use
     *             {@linkplain com.codeborne.selenide.Condition#attributeMatching} instead.
     * @param attributeName
     *            The name of the attribute that should contain the text
     * @param text
     *            The text that should be contained within the attribute
     * @return a Selenide {@link Condition}
     * @see #matchAttribute(String, String)
     */
    @Deprecated
    public static WebElementCondition matchesAttribute(String attributeName, String text)
    {
        return matchAttribute(attributeName, text);
    }

    /**
     * The missing regular expression condition for attributes.<br>
     * <br>
     * <p>
     * Sample: Assert that given element's value attribute matches given regular expression
     * <code>$("input").should(matchValue("Hello\s*John"))</code>
     * </p>
     * 
     * @deprecated Not needed anymore since it's supported by Selenide. Will be removed with the next major version. Use
     *             {@linkplain com.codeborne.selenide.Condition#attributeMatching} instead.
     * @param attributeName
     *            The name of the attribute that should be matched with the regex
     * @param regex
     *            e.g. Kicked.*Chuck Norris - in this case ".*" can contain any characters including spaces, tabs, CR
     *            etc.
     * @return a Selenide {@link Condition}
     */
    @Deprecated
    public static WebElementCondition matchAttribute(final String attributeName, final String regex)
    {
        return new WebElementCondition("match " + attributeName)
        {
            @Override
            public String toString()
            {
                return this.getName() + " '" + regex + '\'';
            }

            @Override
            public CheckResult check(Driver driver, WebElement element)
            {
                return new CheckResult(Html.text.matches(getAttributeValue(element, attributeName), regex), element.getAttribute(attributeName));
            }
        };
    }

    private static String getAttributeValue(WebElement element, String attributeName)
    {
        String attr = element.getAttribute(attributeName);
        return attr == null ? "" : attr;
    }

    /**
     * The missing wrapper to generate screenshots and save the html source code if a jUnit assertion fails.<br>
     * <br>
     * <p>
     * Sample: Assert that page title is correct and dump the page source and a screenshot in case of a mismatch
     * <code> wrapAssertionError(()-&gt;{Assert.assertEquals("MyPageTitle", Selenide.title());});</code>
     * </p>
     * 
     * @param runnable
     *            The lambda containing an assertion
     */
    public static void wrapAssertionError(final Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (AssertionError e)
        {
            Driver driver = WebDriverRunner.driver();
            String message = "No error message provided by the Assertion.";
            if (StringUtils.isNotBlank(e.getMessage()))
            {
                message = e.getMessage();
            }
            else
            {
                AssertionError wrapper = new AssertionError(message, e.getCause());
                wrapper.setStackTrace(e.getStackTrace());
                e = wrapper;
            }
            if (!driver.config().assertionMode().equals(AssertionMode.SOFT))
            {
                AssertionError e1 = e;
                SelenideLogger.commitStep(SelenideLogger.step("Assertion error", () -> {
                    if (driver.getSessionId() != null)
                    {
                        throw UIAssertionError.wrap(driver, e1, 0);
                    }
                    else
                    {
                        throw e1;
                    }
                }), e);
            }
            else
            {
                SelenideLogger.commitStep(SelenideLogger.beginStep("Assertion error", message), e);
            }
        }
    }

    /**
     * Drag and drop an element to a given position. The position will be set by the user. It drags the element and
     * moves it to a specific position of the respective element.
     * 
     * @param elementToMove
     *            The selector of the slider to drag and drop
     * @param horizontalMovement
     *            The offset for the horizontal movement
     * @param verticalMovement
     *            The offset for the vertical movement
     */
    public static void dragAndDrop(SelenideElement elementToMove, int horizontalMovement, int verticalMovement)
    {
        assertTrue(Neodymium.hasDriver(),
                   "Neodymium has no driver, so we can not perform a drag and drop operation. Please check your setup and add the @Browser annotation to your test.");

        try
        {
            // perform drag and drop via the standard Selenium way
            new Actions(Neodymium.getDriver()).dragAndDropBy(elementToMove.getWrappedElement(), horizontalMovement, verticalMovement)
                                              .build().perform();
        }
        catch (MoveTargetOutOfBoundsException targetOutOfBound)
        {
            String message = "Performing drag and drop with an element moved the element out of the viewport. Try to scroll the element completely into the view port or to decrease the absolute values of your movements.";
            SelenideLogger.commitStep(SelenideLogger.beginStep("slider", message), targetOutOfBound);
            throw UIAssertionError.wrap(WebDriverRunner.driver(), new AssertionError(message, targetOutOfBound), 0);
        }
    }

    /**
     * Drag and drop an element to a given position. The position will be set by the user. It drags the element and
     * moves it to a specific position of the respective slider.
     * 
     * @param elementToMove
     *            The selector of the slider to drag and drop
     * @param elementToCheck
     *            The locator of the slider value
     * @param horizontalMovement
     *            The offset for the horizontal movement
     * @param verticalMovement
     *            The offset for the vertical movement
     * @param pauseBetweenMovements
     *            Time to pass after the slider do the next movement step
     * @param retryMovements
     *            Amount of retries the slider will be moved
     * @param condition
     *            The condition for the slider to verify the movement
     */
    public static void dragAndDropUntilCondition(SelenideElement elementToMove, SelenideElement elementToCheck, int horizontalMovement,
                                                 int verticalMovement, int pauseBetweenMovements, int retryMovements, WebElementCondition condition)
    {
        int counter = 0;
        while (!elementToCheck.has(condition))
        {
            if (counter > retryMovements)
            {
                SelenideAddons.wrapAssertionError(() -> {
                    Assert.assertTrue("CircutBreaker: Was not able to move the element and to reach the condition. Tried: " + retryMovements
                                      + " times to move the element.", false);
                });
            }
            dragAndDrop(elementToMove, horizontalMovement, verticalMovement);
            sleep(pauseBetweenMovements);
            counter++;
        }
    }

    /**
     * Open the supplied HTML content with the current web driver.
     * 
     * @param htmlContent
     *            a String containing the HTML that should be opened in the current web driver
     */
    public static void openHtmlContentWithCurrentWebDriver(String htmlContent)
    {
        String encodedStuff = Base64.getEncoder().encodeToString(htmlContent.getBytes());
        open("data:text/html;charset=utf-8;base64," + encodedStuff);
    }

    /**
     * Waits until an optional element matches a condition. This function will return false if the element does not
     * match the given condition or can not be found in the given timeframe. This method will use the default optional
     * retry timeout.
     * <p>
     * The following settings can be configured within the Neodymium configuration to tune the retry behavior:
     * </p>
     * <ul>
     * <li>*
     * <li>neodymium.selenideAddons.optional.retry.timeout (default 2000ms pause between retries)</li>
     * </ul>
     * 
     * @param element
     *            the element to match
     * @param condition
     *            the condition for the element
     * @return if the element did match the condition within the given retries
     */
    public static boolean optionalWaitUntilCondition(SelenideElement element, WebElementCondition condition)
    {
        return optionalWaitUntilCondition(element, condition, null, null);
    }

    /**
     * Waits until an optional element matches a condition. This function will return false if the element does not
     * match the given condition or can not be found in the given timeframe.
     * <p>
     * The following settings can be configured within the Neodymium configuration to tune the retry behavior:
     * </p>
     * <ul>
     * <li>neodymium.selenideAddons.optional.retry.count (default 5 retries)</li>
     * </ul>
     *
     * @param element
     *            the element to match
     * @param condition
     *            the condition for the element
     * @param maxWaitingTime
     *            the maximum amount of time to wait
     * @return if the element did match the condition within the given retries
     */
    public static boolean optionalWaitUntilCondition(SelenideElement element, WebElementCondition condition, long maxWaitingTime)
    {
        return optionalWaitUntilCondition(element, condition, maxWaitingTime, null);
    }

    /**
     * Waits until an optional element matches a condition. This function will return false if the element does not
     * match the given condition or can not be found in the given timeframe.
     * <p>
     * The following settings can be configured within the Neodymium configuration to tune the retry behavior:
     * </p>
     * <ul>
     * <li>neodymium.selenideAddons.optional.retry.count (default 5 retries)</li>
     * </ul>
     *
     * @param element
     *            the element to match
     * @param condition
     *            the condition for the element
     * @param maxWaitingTime
     *            the maximum amount of time to wait
     * @param pollingInterval
     *            the amount of time to wait in between retries
     * @return if the element did match the condition within the given retries
     */
    public static boolean optionalWaitUntilCondition(SelenideElement element, WebElementCondition condition, Long maxWaitingTime, Long pollingInterval)
    {
        if (maxWaitingTime == null)
        {
            maxWaitingTime = Neodymium.configuration().optionalElementRetryTimeout();
        }
        if (pollingInterval == null)
        {
            pollingInterval = Neodymium.configuration().optionalElementRetryPollingIntervall();
        }

        boolean result = false;
        final long start = System.currentTimeMillis();
        while (!result && ((System.currentTimeMillis() - start) < maxWaitingTime))
        {
            if (element.has(condition))
            {
                result = true;
                break;
            }
            Selenide.sleep(pollingInterval);
        }
        return result;
    }

    /**
     * Waits while an optional element matches a condition. This function will return false if the element does match
     * the given condition or can not be found after the given timeframe. This method will use the default optional
     * retry timeout.
     * <p>
     * The following settings can be configured within the Neodymium configuration to tune the retry behavior:
     * </p>
     * <ul>
     * <li>*
     * <li>neodymium.selenideAddons.optional.retry.timeout (default 2000ms pause between retries)</li>
     * </ul>
     * 
     * @param element
     *            the element to match
     * @param condition
     *            the condition for the element
     * @return if the element did stop matching the condition within the given retries
     */
    public static boolean optionalWaitWhileCondition(SelenideElement element, WebElementCondition condition)
    {
        return optionalWaitUntilCondition(element, not(condition), null, null);
    }

    /**
     * Waits while an optional element matches a condition. This function will return false if the element does match
     * the given condition or can not be found after the given timeframe.
     * <p>
     * The following settings can be configured within the Neodymium configuration to tune the retry behavior:
     * </p>
     * <ul>
     * <li>neodymium.selenideAddons.optional.retry.count (default 5 retries)</li>
     * </ul>
     *
     * @param element
     *            the element to match
     * @param condition
     *            the condition for the element
     * @param maxWaitingTime
     *            the maximum amount of time to wait
     * @return if the element did stop matching the condition within the given retries
     */
    public static boolean optionalWaitWhileCondition(SelenideElement element, WebElementCondition condition, long maxWaitingTime)
    {
        return optionalWaitUntilCondition(element, not(condition), maxWaitingTime, null);
    }

    /**
     * Waits while an optional element matches a condition. This function will return false if the element does match
     * the given condition or can not be found after the given timeframe.
     * <p>
     * The following settings can be configured within the Neodymium configuration to tune the retry behavior:
     * </p>
     * <ul>
     * <li>neodymium.selenideAddons.optional.retry.count (default 5 retries)</li>
     * </ul>
     *
     * @param element
     *            the element to match
     * @param condition
     *            the condition for the element
     * @param maxWaitingTime
     *            the maximum amount of time to wait
     * @param pollingInterval
     *            the amount of time to wait in between retries
     * @return if the element did stop matching the condition within the given retries
     */
    public static boolean optionalWaitWhileCondition(SelenideElement element, WebElementCondition condition, long maxWaitingTime, long pollingInterval)
    {
        return optionalWaitUntilCondition(element, not(condition), maxWaitingTime, pollingInterval);
    }
}
