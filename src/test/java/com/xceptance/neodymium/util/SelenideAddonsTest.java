package com.xceptance.neodymium.util;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.Range;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.StaleElementReferenceException;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.ex.ElementShould;
import com.codeborne.selenide.ex.UIAssertionError;
import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEvent.EventStatus;
import com.codeborne.selenide.logevents.LogEventListener;
import com.codeborne.selenide.logevents.SelenideLog;
import com.codeborne.selenide.logevents.SelenideLogger;
import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.browser.multibrowser.Browser;
import com.xceptance.neodymium.module.statement.browser.multibrowser.SuppressBrowsers;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class SelenideAddonsTest
{
    private void openBlogPage()
    {
        Selenide.open("https://blog.xceptance.com/");
    }

    @Test
    public void testMatchesAttributeCondition()
    {
        openBlogPage();
        $("#masthead .search-toggle").click();

        $("#search-container .search-field").should(SelenideAddons.matchesAttribute("placeholder", "Search"));
    }

    @Test
    public void testMatchAttributeCondition()
    {
        openBlogPage();
        $("#masthead .search-toggle").click();

        $("#search-container .search-field").should(SelenideAddons.matchAttribute("placeholder", "^S.a.c.\\s…"));
        $("#search-container .search-field").should(SelenideAddons.matchAttribute("placeholder", "\\D+"));
    }

    @Test
    public void testMatchAttributeConditionError()
    {
        openBlogPage();
        $("#masthead .search-toggle").click();

        Assert.assertThrows(ElementShould.class, () -> {
            $("#search-container .search-field").should(SelenideAddons.matchAttribute("placeholder", "\\d+"));
        });
    }

    @Test
    public void testMatchAttributeConditionErrorMissingAttribute()
    {
        openBlogPage();
        $("#masthead .search-toggle").click();

        Assert.assertThrows(ElementShould.class, () -> {
            $("#search-container .search-field").should(SelenideAddons.matchAttribute("foo", "bar"));
        });
    }

    @Test
    public void testMatchesValueCondition()
    {
        openBlogPage();
        $("#masthead .search-toggle").click();
        $("#search-container .search-field").val("searchphrase").submit();

        $("#content .search-field").should(SelenideAddons.matchesValue("earchphras"));
    }

    @Test
    public void testMatchValueCondition()
    {
        openBlogPage();
        $("#masthead .search-toggle").click();
        $("#search-container .search-field").val("searchphrase").submit();

        $("#content .search-field").should(SelenideAddons.matchValue("^s.a.c.p.r.s.$"));
        $("#content .search-field").should(SelenideAddons.matchValue("\\D+"));
    }

    @Test
    public void testMatchValueConditionError()
    {
        openBlogPage();
        $("#masthead .search-toggle").click();
        $("#search-container .search-field").val("searchphrase").submit();

        Assert.assertThrows(ElementShould.class, () -> {
            $("#content .search-field").should(SelenideAddons.matchValue("\\d+"));
        });
    }

    @Test()
    public void testWrapAssertion()
    {
        openBlogPage();

        SelenideAddons.wrapAssertionError(() -> {
            Assert.assertEquals("Passionate Testing | Xceptance Blog", Selenide.title());
        });
    }

    @Test
    public void testWrapAssertionError()
    {
        openBlogPage();

        Assert.assertThrows(UIAssertionError.class, () -> {
            SelenideAddons.wrapAssertionError(() -> {
                Assert.assertEquals("MyPageTitle", Selenide.title());
            });
        });
    }

    @Test
    public void testWrapSoftAssertionError()
    {
        final String errMessage = "Title doesn't match.";
        SelenideLogger.addListener("testListener", new LogEventListener()
        {
            @Override
            public void afterEvent(LogEvent currentLog)
            {
                SelenideLog log = (SelenideLog) currentLog;
                if (log.getStatus().equals(EventStatus.FAIL))
                {
                    Assert.assertEquals("Selenide log event not matching", "Assertion error", log.getElement());
                    Assert.assertTrue("Selenide log event not matching", log.getError().getMessage().startsWith(errMessage));
                }
            }

            @Override
            public void beforeEvent(LogEvent currentLog)
            {
                // ignore
            }
        });

        openBlogPage();
        Neodymium.softAssertions(true);
        try
        {

            SelenideAddons.wrapAssertionError(() -> {
                Assert.assertEquals(errMessage, "MyPageTitle", Selenide.title());
            });
        }
        finally
        {
            Neodymium.softAssertions(false);
            SelenideLogger.removeListener("testListener");
        }
    }

    @Test
    public void testWrapSoftAssertionErrorWithoutMessage()
    {
        final String errMessage = "No error message provided by the Assertion.";

        SelenideLogger.addListener("testListener", new LogEventListener()
        {
            @Override
            public void afterEvent(LogEvent currentLog)
            {
                SelenideLog log = (SelenideLog) currentLog;
                if (log.getStatus().equals(EventStatus.FAIL))
                {
                    Assert.assertEquals("Selenide log event not matching", "Assertion error", log.getElement());
                    Assert.assertEquals("Selenide log event not matching", log.getSubject(), errMessage);
                }
            }

            @Override
            public void beforeEvent(LogEvent currentLog)
            {
                // ignore
            }
        });

        openBlogPage();
        Neodymium.softAssertions(true);
        try
        {
            SelenideAddons.wrapAssertionError(() -> {
                Assert.assertTrue(Selenide.title().startsWith("MyPageTitle"));
            });
        }
        finally
        {
            Neodymium.softAssertions(false);
            SelenideLogger.removeListener("testListener");
        }
    }

    @Test
    public void testWrapAssertionErrorWithoutMessage()
    {
        final String errMessage = "AssertionError: No error message provided by the Assertion.";
        try
        {
            openBlogPage();
            SelenideAddons.wrapAssertionError(() -> {
                Assert.assertTrue(Selenide.title().startsWith("MyPageTitle"));
            });
        }
        catch (UIAssertionError e)
        {
            Assert.assertTrue(e.getMessage().startsWith(errMessage));
        }
    }

    @Test()
    @SuppressBrowsers
    public void testSafeRunnable()
    {
        // preparing the test setup as kind of generator
        AtomicInteger counter = new AtomicInteger(0);
        List<Runnable> runArray = new ArrayList<Runnable>();
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall pass!");
                     });
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall never be seen!");
                     });
        final Iterator<Runnable> iterator = runArray.iterator();

        // testing the error path after three exceptions
        long startTime = new Date().getTime();
        try
        {
            SelenideAddons.$safe(() -> {
                counter.incrementAndGet();
                if (iterator.hasNext())
                {
                    iterator.next().run();
                }
            });
        }
        catch (StaleElementReferenceException e)
        {
            Assert.assertTrue(e.getMessage().startsWith("You shall pass!"));
        }
        long endTime = new Date().getTime();
        Assert.assertTrue(endTime - startTime > Neodymium.configuration().staleElementRetryTimeout());
        Assert.assertEquals(counter.get(), Neodymium.configuration().staleElementRetryCount() + 1);

        // testing the happy path after one exception
        SelenideAddons.$safe(() -> {
            counter.incrementAndGet();
            if (iterator.hasNext())
            {
                iterator.next().run();
            }
        });
        Assert.assertEquals(counter.get(), Neodymium.configuration().staleElementRetryCount() + 3);
    }

    @Test()
    public void testSafeSupplier()
    {
        // preparing the test setup as kind of generator
        AtomicInteger counter = new AtomicInteger(0);
        List<Runnable> runArray = new ArrayList<Runnable>();
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall pass!");
                     });
        runArray.add(
                     () -> {
                         throw new StaleElementReferenceException("You shall never be seen!");
                     });
        final Iterator<Runnable> iterator = runArray.iterator();

        // testing the error path after three exceptions
        openBlogPage();
        long startTime = new Date().getTime();
        try
        {
            SelenideAddons.$safe(() -> {
                counter.incrementAndGet();
                if (iterator.hasNext())
                {
                    iterator.next().run();
                }
                return $("body").should(exist);
            });
        }
        catch (StaleElementReferenceException e)
        {
            Assert.assertTrue(e.getMessage().startsWith("You shall pass!"));
        }
        long endTime = new Date().getTime();
        Assert.assertTrue(endTime - startTime > Neodymium.configuration().staleElementRetryTimeout());
        Assert.assertEquals(counter.get(), Neodymium.configuration().staleElementRetryCount() + 1);

        // testing the happy path after one exception
        SelenideElement element = SelenideAddons.$safe(() -> {
            counter.incrementAndGet();
            if (iterator.hasNext())
            {
                iterator.next().run();
            }
            return $("body");
        });
        Assert.assertEquals(counter.get(), Neodymium.configuration().staleElementRetryCount() + 3);
        element.shouldBe(visible);
    }

    @Test()
    @SuppressBrowsers
    public void testSafeNestedException()
    {
        // preparing the test setup as kind of generator
        AtomicInteger counter = new AtomicInteger(0);
        List<Runnable> runArray = new ArrayList<Runnable>();
        runArray.add(
                     () -> {
                         throw getWrappedThrowable("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw getWrappedThrowable("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw getWrappedThrowable("You shall not pass!");
                     });
        runArray.add(
                     () -> {
                         throw getWrappedThrowable("You shall pass!");
                     });
        runArray.add(
                     () -> {
                         throw getWrappedThrowable("You shall never be seen!");
                     });
        final Iterator<Runnable> iterator = runArray.iterator();

        // testing the error path after three exceptions
        long startTime = new Date().getTime();
        try
        {
            SelenideAddons.$safe(() -> {
                counter.incrementAndGet();
                if (iterator.hasNext())
                {
                    iterator.next().run();
                }
            });
        }
        catch (RuntimeException e)
        {
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof StaleElementReferenceException);
            Assert.assertTrue(e.getCause().getMessage().startsWith("You shall pass!"));
        }
        long endTime = new Date().getTime();
        Assert.assertTrue(endTime - startTime > Neodymium.configuration().staleElementRetryTimeout());
        Assert.assertEquals(counter.get(), Neodymium.configuration().staleElementRetryCount() + 1);

        // testing the happy path after one exception
        SelenideAddons.$safe(() -> {
            counter.incrementAndGet();
            if (iterator.hasNext())
            {
                iterator.next().run();
            }
        });
        Assert.assertEquals(counter.get(), Neodymium.configuration().staleElementRetryCount() + 3);
    }

    private RuntimeException getWrappedThrowable(String message)
    {
        return new RuntimeException(new StaleElementReferenceException(message));
    }

    @Test()
    public void testRightwardDragAndDropUntilCondition()
    {
        openSliderPage();

        SelenideElement slider = $(".balSlider a[role=slider]");
        slider.shouldHave(attribute("aria-valuenow", "-10"));
        SelenideAddons.dragAndDropUntilCondition(slider, slider, 40, 0, 3000, 23, Condition.attribute("aria-valuenow", "8"));
        slider.shouldHave(attribute("aria-valuenow", "8"));
    }

    @Test()
    public void testLeftwardDragAndDropUntilCondition()
    {
        openSliderPage();

        SelenideElement slider = $(".balSlider a[role=slider]");
        slider.shouldHave(attribute("aria-valuenow", "-10"));
        SelenideAddons.dragAndDropUntilCondition(slider, slider, -40, 0, 3000, 23, Condition.attribute("aria-valuenow", "-8"));
        slider.shouldHave(attribute("aria-valuenow", "-8"));
    }

    @Test()
    public void testUpwardDragAndDropUntilCondition()
    {
        openSliderPage();

        SelenideElement slider = $("#equalizer .k-slider-vertical:first-child a");
        slider.shouldHave(attribute("aria-valuenow", "10"));
        SelenideAddons.dragAndDropUntilCondition(slider, slider, 0, -10, 3000, 23, Condition.attribute("aria-valuenow", "16"));
        slider.shouldHave(attribute("aria-valuenow", "16"));
    }

    @Test()
    public void testDownwardDragAndDropUntilCondition()
    {
        openSliderPage();

        SelenideElement slider = $("#equalizer .k-slider-vertical:first-child a");
        slider.shouldHave(attribute("aria-valuenow", "10"));
        SelenideAddons.dragAndDropUntilCondition(slider, slider, 0, 10, 3000, 23, Condition.attribute("aria-valuenow", "-6"));
        slider.shouldHave(attribute("aria-valuenow", "-6"));
    }

    @Test()
    public void testLeftwardDragAndDropUntilAttribute()
    {
        openSliderPage();

        SelenideElement slider = $(".balSlider a[role=slider]");
        slider.shouldHave(attribute("aria-valuenow", "-10"));
        leftHorizontalDragAndDropUntilAttribute(slider, slider, -40, "aria-valuenow", "-8");
        slider.shouldHave(attribute("aria-valuenow", "-8"));
    }

    @Test()
    public void testDragAndDropAssertionError()
    {
        openSliderPage();

        SelenideElement slider = $(".balSlider a[role=slider]");
        slider.shouldHave(attribute("aria-valuenow", "-10"));

        Assert.assertThrows(UIAssertionError.class, () -> {
            SelenideAddons.dragAndDropUntilCondition(slider, slider, -10, 0, 3000, -1, Condition.attribute("aria-valuenow", "-16"));
        });
    }

    @Test()
    public void testRightwardDragAndDrop()
    {
        openSliderPage();

        SelenideElement slider = $(".balSlider a[role=slider]");
        slider.shouldHave(attribute("aria-valuenow", "-10"));
        SelenideAddons.dragAndDrop(slider, 32, 0);
        slider.shouldHave(attribute("aria-valuenow", "2"));
    }

    @Test()
    public void testLeftwardDragAndDrop()
    {
        openSliderPage();

        SelenideElement slider = $(".balSlider a[role=slider]");
        slider.shouldHave(attribute("aria-valuenow", "-10"));
        SelenideAddons.dragAndDrop(slider, -32, 0);
        slider.shouldHave(attribute("aria-valuenow", "-2"));
    }

    @Test()
    public void testDownwardDragAndDrop()
    {
        openSliderPage();

        SelenideElement slider = $("#equalizer .k-slider-vertical:first-child a");
        slider.shouldHave(attribute("aria-valuenow", "10"));
        SelenideAddons.dragAndDrop(slider, 0, 12);
        slider.shouldHave(attribute("aria-valuenow", "6"));
    }

    @Test()
    public void testUpwardDragAndDrop()
    {
        openSliderPage();

        SelenideElement slider = $("#equalizer .k-slider-vertical:first-child a");
        slider.shouldHave(attribute("aria-valuenow", "10"));
        SelenideAddons.dragAndDrop(slider, 0, -12);
        slider.shouldHave(attribute("aria-valuenow", "14"));
    }

    @Test()
    public void testRightwardDragAndDropOutOfBounds()
    {
        openSliderPage();

        SelenideElement slider = $(".balSlider a[role=slider]");
        slider.shouldHave(attribute("aria-valuenow", "-10"));
        AssertionError exception = Assert.assertThrows(AssertionError.class, () -> {
            SelenideAddons.dragAndDrop(slider, 3200, 0);
        });
        String expectedMessage = "Performing drag and drop with an element moved the element out of the viewport. Try to scroll the element completely into the view port or to decrease the absolute values of your movements.";
        String actualMessage = exception.getMessage();
        Assert.assertTrue(String.format("The exception message '%s' doesn't contain the expected message '%s'", actualMessage, expectedMessage),
                          actualMessage.contains(expectedMessage));
    }

    @Test()
    public void testLeftwardDragAndDropOutOfBounds()
    {
        openSliderPage();

        SelenideElement slider = $(".balSlider a[role=slider]");
        slider.shouldHave(attribute("aria-valuenow", "-10"));
        AssertionError exception = Assert.assertThrows(AssertionError.class, () -> {
            SelenideAddons.dragAndDrop(slider, -3200, 0);
        });
        String expectedMessage = "Performing drag and drop with an element moved the element out of the viewport. Try to scroll the element completely into the view port or to decrease the absolute values of your movements.";
        String actualMessage = exception.getMessage();
        Assert.assertTrue(String.format("The exception message %s doesn't contain the expected message %s", actualMessage, expectedMessage),
                          actualMessage.contains(expectedMessage));
    }

    @Test()
    public void testUpwardDragAndDropOutOfBounds()
    {
        openSliderPage();

        SelenideElement slider = $("#equalizer .k-slider-vertical:first-child a");
        slider.shouldHave(attribute("aria-valuenow", "10"));
        AssertionError exception = Assert.assertThrows(AssertionError.class, () -> {
            SelenideAddons.dragAndDrop(slider, 0, -1200);
        });
        String expectedMessage = "Performing drag and drop with an element moved the element out of the viewport. Try to scroll the element completely into the view port or to decrease the absolute values of your movements.";
        String actualMessage = exception.getMessage();
        Assert.assertTrue(String.format("The exception message %s doesn't contain the expected message %s", actualMessage, expectedMessage),
                          actualMessage.contains(expectedMessage));
    }

    @Test()
    public void testDownwardDragAndDropOutOfBounds()
    {
        openSliderPage();

        SelenideElement slider = $("#equalizer .k-slider-vertical:first-child a");
        slider.shouldHave(attribute("aria-valuenow", "10"));
        AssertionError exception = Assert.assertThrows(AssertionError.class, () -> {
            SelenideAddons.dragAndDrop(slider, 0, 1200);
        });
        String expectedMessage = "Performing drag and drop with an element moved the element out of the viewport. Try to scroll the element completely into the view port or to decrease the absolute values of your movements.";
        String actualMessage = exception.getMessage();
        Assert.assertTrue(String.format("The exception message %s doesn't contain the expected message %s", actualMessage, expectedMessage),
                          actualMessage.contains(expectedMessage));
    }

    private void openSliderPage()
    {
        Selenide.open("https://demos.telerik.com/kendo-ui/slider/index");
        boolean overlayIsVisible = true;
        try
        {
            $("#onetrust-accept-btn-handler").shouldBe(visible);
        }
        catch (ElementNotFound e)
        {
            overlayIsVisible = false;
        }

        if (overlayIsVisible)
        {
            $("#onetrust-accept-btn-handler").click();
            $("#onetrust-consent-sdk .onetrust-pc-dark-filter").shouldBe(hidden);
            Selenide.refresh();
        }
        $(".kd-loader-wrap").shouldBe(hidden, Duration.ofMillis(6000));
    }

    private void leftHorizontalDragAndDropUntilAttribute(SelenideElement elementToMove, SelenideElement elementToCheck, int horizontalMovement,
                                                         String sliderValueAttributeName, String moveUntil)
    {
        // Example: create a special method to move until a given text
        SelenideAddons.dragAndDropUntilCondition(elementToMove, elementToCheck, horizontalMovement, 0, 3000, 10,
                                                 Condition.attribute(sliderValueAttributeName, moveUntil));
    }

    @Test
    public void testOpenHtmlContentWithCurrentWebDriver()
    {
        final String text = "Hi\n\nHow are you?)\n\nBye";
        final String textHtml = "<div dir=\"auto\">Hi<div dir=\"auto\"><br></div><div dir=\"auto\">How are you?)</div><div dir=\"auto\"><br></div><div dir=\"auto\">Bye</div></div>";

        SelenideAddons.openHtmlContentWithCurrentWebDriver(textHtml);
        Assert.assertEquals(text, $("body").getText());
    }

    private static final int CUSTOM_MAX_WAITING_TIME = 20000;

    private static final int CUSTOM_POLLING_INTERVAL = 5000;

    private static final String PRIVACY_DIALOG_SELECTOR = "#privacy-message";

    private void validateRuntimeIsExpected(Runnable runnable, long expectedTime, long pollingInterval)
    {
        final long startTime = new Date().getTime();
        runnable.run();
        final long endTime = new Date().getTime();

        final long runtime = endTime - startTime;
        // check that runtime of the wait until method was as long as expected
        Assert.assertTrue("Runtime was not within the expected range", Range.between(expectedTime, expectedTime + pollingInterval / 2)
                                                                            .contains(runtime));
    }

    @Test
    public void testOptionalWaitUntil()
    {
        openBlogPage();

        validateRuntimeIsExpected(() -> {
            // wait until the optional privacy dialog is visible
            boolean isVisible = SelenideAddons.optionalWaitUntilCondition($(PRIVACY_DIALOG_SELECTOR), visible, CUSTOM_MAX_WAITING_TIME);
            Assert.assertTrue("the privacy message dialog was not found within the timeframe", isVisible);
        },
                                  0,
                                  Neodymium.configuration().optionalElementRetryPollingIntervall());

        validateRuntimeIsExpected(() -> {
            // try wait until the optional privacy dialog is hidden, which will fail
            boolean isHidden = SelenideAddons.optionalWaitUntilCondition($(PRIVACY_DIALOG_SELECTOR), hidden, CUSTOM_MAX_WAITING_TIME);
            // check that the is false as is expected
            Assert.assertFalse("the privacy message dialog was unexpectedly hidden during the timeframe", isHidden);
        },
                                  CUSTOM_MAX_WAITING_TIME,
                                  Neodymium.configuration().optionalElementRetryPollingIntervall());
    }

    @Test
    public void testOptionalWaitWhile()
    {
        openBlogPage();

        validateRuntimeIsExpected(() -> {
            // wait until the optional privacy dialog is not visible anymore, which will fail
            boolean isHidden = SelenideAddons.optionalWaitWhileCondition($(PRIVACY_DIALOG_SELECTOR), visible, CUSTOM_MAX_WAITING_TIME);
            // check that the result is false as expected
            Assert.assertFalse("the privacy message dialog was unexpectedly hidden during the timeframe", isHidden);
        },
                                  CUSTOM_MAX_WAITING_TIME,
                                  Neodymium.configuration().optionalElementRetryPollingIntervall());

        // click the opt out button
        $(PRIVACY_DIALOG_SELECTOR).find(".btn-link").click();

        validateRuntimeIsExpected(() -> {
            // wait until the privacy dialog is hidden
            boolean isHidden = SelenideAddons.optionalWaitWhileCondition($(PRIVACY_DIALOG_SELECTOR), visible, CUSTOM_MAX_WAITING_TIME);
            Assert.assertTrue("the privacy message dialog remained visible during the timeframe", isHidden);
        },
                                  0,
                                  Neodymium.configuration().optionalElementRetryPollingIntervall());
    }

    @Test
    public void testOptionalWaitUntilWithDefaultTimeout()
    {
        openBlogPage();

        validateRuntimeIsExpected(() -> {
            // wait until the optional privacy dialog is visible
            boolean isVisible = SelenideAddons.optionalWaitUntilCondition($(PRIVACY_DIALOG_SELECTOR), visible);
            Assert.assertTrue("the privacy message dialog was not found within the timeframe", isVisible);
        },
                                  0,
                                  Neodymium.configuration().optionalElementRetryPollingIntervall());

        validateRuntimeIsExpected(() -> {
            // try wait until the optional privacy dialog is hidden, which will fail
            boolean isHidden = SelenideAddons.optionalWaitUntilCondition($(PRIVACY_DIALOG_SELECTOR), hidden);
            // check that the is false as is expected
            Assert.assertFalse("the privacy message dialog was unexpectedly hidden during the timeframe", isHidden);
        },
                                  Neodymium.configuration().optionalElementRetryTimeout(),
                                  Neodymium.configuration().optionalElementRetryPollingIntervall());
    }

    @Test
    public void testOptionalWaitWhileWithDefaultTimeout()
    {
        openBlogPage();

        validateRuntimeIsExpected(() -> {
            // wait until the optional privacy dialog is not visible anymore, which will fail
            boolean isHidden = SelenideAddons.optionalWaitWhileCondition($(PRIVACY_DIALOG_SELECTOR), visible);
            // check that the result is false as expected
            Assert.assertFalse("the privacy message dialog was unexpectedly hidden during the timeframe", isHidden);
        },
                                  Neodymium.configuration().optionalElementRetryTimeout(),
                                  Neodymium.configuration().optionalElementRetryPollingIntervall());

        // click the opt out button
        $(PRIVACY_DIALOG_SELECTOR).find(".btn-link").click();

        validateRuntimeIsExpected(() -> {
            // wait until the privacy dialog is hidden
            boolean isHidden = SelenideAddons.optionalWaitWhileCondition($(PRIVACY_DIALOG_SELECTOR), visible);
            Assert.assertTrue("the privacy message dialog remained visible during the timeframe", isHidden);
        },
                                  0,
                                  Neodymium.configuration().optionalElementRetryPollingIntervall());
    }

    @Test
    public void testOptionalWaitUntilWithTimeoutAndPollingInterval()
    {
        openBlogPage();

        validateRuntimeIsExpected(() -> {
            // wait until the optional privacy dialog is visible
            boolean isVisible = SelenideAddons.optionalWaitUntilCondition($(PRIVACY_DIALOG_SELECTOR), visible, CUSTOM_MAX_WAITING_TIME,
                                                                          CUSTOM_POLLING_INTERVAL);
            Assert.assertTrue("the privacy message dialog was not found within the timeframe", isVisible);
        },
                                  0,
                                  CUSTOM_POLLING_INTERVAL);

        validateRuntimeIsExpected(() -> {
            // try wait until the optional privacy dialog is hidden, which will fail
            boolean isHidden = SelenideAddons.optionalWaitUntilCondition($(PRIVACY_DIALOG_SELECTOR), hidden, CUSTOM_MAX_WAITING_TIME, CUSTOM_POLLING_INTERVAL);
            // check that the is false as is expected
            Assert.assertFalse("the privacy message dialog was unexpectedly hidden during the timeframe", isHidden);
        },
                                  CUSTOM_MAX_WAITING_TIME,
                                  CUSTOM_POLLING_INTERVAL);
    }

    @Test
    public void testOptionalWaitWhileWithTimeoutAndPollingInterval()
    {
        openBlogPage();

        validateRuntimeIsExpected(() -> {
            // wait until the optional privacy dialog is not visible anymore, which will fail
            boolean isHidden = SelenideAddons.optionalWaitWhileCondition($(PRIVACY_DIALOG_SELECTOR), visible, CUSTOM_MAX_WAITING_TIME, CUSTOM_POLLING_INTERVAL);
            // check that the result is false as expected
            Assert.assertFalse("the privacy message dialog was unexpectedly hidden during the timeframe", isHidden);
        },
                                  CUSTOM_MAX_WAITING_TIME,
                                  CUSTOM_POLLING_INTERVAL);

        // click the opt out button
        $(PRIVACY_DIALOG_SELECTOR).find(".btn-link").click();

        validateRuntimeIsExpected(() -> {
            // wait until the privacy dialog is hidden
            boolean isHidden = SelenideAddons.optionalWaitWhileCondition($(PRIVACY_DIALOG_SELECTOR), visible, CUSTOM_MAX_WAITING_TIME, CUSTOM_POLLING_INTERVAL);
            Assert.assertTrue("the privacy message dialog remained visible during the timeframe", isHidden);
        },
                                  0,
                                  CUSTOM_POLLING_INTERVAL);
    }
}
