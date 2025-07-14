package com.xceptance.neodymium.util;

import static com.codeborne.selenide.Selenide.$;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;

import org.openqa.selenium.StaleElementReferenceException;

import com.codeborne.selenide.Selenide;

/**
 * Source: http://www.swtestacademy.com/selenium-wait-javascript-angular-ajax/ With a lot of modifications
 *
 * @author rschwietzke
 */
public class JavaScriptUtils
{
    // Wait Until JQuery and JS Ready
    public static void waitForReady()
    {
        List<BooleanSupplier> conditionsToWaitFor = new LinkedList<BooleanSupplier>();

        // Wait for jQuery to load
        if (Neodymium.configuration().javascriptLoadingJQueryIsRequired())
        {
            conditionsToWaitFor.add(() -> {
                return Selenide.executeJavaScript("return !!window.jQuery && window.jQuery.active == 0");
            });
        }

        // dom ready
        conditionsToWaitFor.add(() -> {
            return Selenide.executeJavaScript("return document.readyState == 'complete'");
        });

        if (Neodymium.configuration().javascriptLoadingAnimationSelector() != null)
        {
            // no loading animation
            conditionsToWaitFor.add(() -> {
                return !$(Neodymium.configuration().javascriptLoadingAnimationSelector()).exists();
            });
        }

        until(conditionsToWaitFor);
    }

    /**
     * Wait until all conditions are true. One wait statement, so the timeouts don't sum up
     *
     * @param conditions
     *            a list of conditions to verify
     */
    public static void until(final List<BooleanSupplier> conditions)
    {
        final long timeout = Neodymium.configuration().javaScriptTimeout();
        final long start = System.currentTimeMillis();

        // loop if still is time
        for (final BooleanSupplier condition : conditions)
        {
            boolean endEarly = false;

            while (!endEarly && System.currentTimeMillis() - start < timeout)
            {
                try
                {
                    final boolean result = condition.getAsBoolean();
                    if (result)
                    {
                        endEarly = true;
                        continue;
                    }
                }
                catch (final StaleElementReferenceException | NoSuchElementException e)
                {
                    // we might have to limit the exception range
                }

                sleep(Neodymium.configuration().javaScriptPollingInterval());

                // time is up?
                if (System.currentTimeMillis() - start >= timeout)
                {
                    return;
                }
            }
        }
    }

    private static void sleep(final long msec)
    {
        // wait
        try
        {
            Thread.sleep(msec);
        }
        catch (final InterruptedException e1)
        {
            return; // leave immediately
        }
    }

    /**
     * Closes a on popup container by clicking the element identified by the selector
     * 
     * @param popupSelector
     *            selector for the popup
     */
    public static void injectJavascriptPopupBlocker(String popupSelector)
    {
        String popupSelectorQuoted = popupSelector.replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\\"");

        String popupBlocker = "(function() {"
                              + "console.log('Neodymium Popupblocker: Injected');"

                              // initialize popup list
                              + "  if (typeof window.xcPopupsToBlock === 'undefined') "
                              + "  {"
                              + "    window.xcPopupsToBlock = []; "
                              + "    console.log('Neodymium Popupblocker: xcPopupsToBlock global variable initialized.');"
                              + "  }"
                              + "  "
                              // add pattern to list if not existing
                              + "  let newPattern = '" + popupSelectorQuoted + "';"
                              + "  if (window.xcPopupsToBlock.includes(newPattern)) {"
                              + "    console.log(`Neodymium Popupblocker: Pattern \"${newPattern}\" already exists in xcPopupsToBlock. Skipping addition.`);"
                              + "  } "
                              + "  else"
                              + "  {"
                              + "    window.xcPopupsToBlock.push(newPattern);"
                              + "  }"
                              + "  "
                              + "  "
                              // create function if it's not already there
                              + "  if (typeof window.xcPopupBlocker !== 'function') "
                              + "  {"
                              + "     console.log('init xcPopupBlocker function');"
                              + "     window.xcPopupBlocker = function() "
                              + "     {"
                              + "       console.log('run xcPopupBlocker function');"
                              + "       for (let i = 0; i < window.xcPopupsToBlock.length; i++) {"
                              + "         const pattern = window.xcPopupsToBlock[i];"
                              + "         var popupElement = document.querySelector(pattern);"
                              + "         console.log('Neodymium Popupblocker: Check for Popup ' + pattern);"
                              + "         if(popupElement != null)"
                              + "         {"
                              + "             try {"
                              + "               popupElement.click();"
                              + "               console.log('Neodymium Popupblocker: Popup '+pattern+' clicked')"
                              + "             } catch(error) {}"
                              + "         }"
                              + "         if(popupElement != null)"
                              + "         {"
                              + "           try {"
                              + "             popupElement.dispatchEvent(new Event('click'));"
                              + "             console.log('Neodymium Popupblocker: Popup '+pattern+' event triggered')"
                              + "           } catch(error) {}"
                              + "         }"
                              + "       }"
                              + "     };"
                              + "     setInterval(window.xcPopupBlocker," + Neodymium.configuration().getPopupBlockerInterval() + ");"
                              + "  }"
                              + "}"
                              + ")();";

        Selenide.executeJavaScript(popupBlocker, "");
    }
}
