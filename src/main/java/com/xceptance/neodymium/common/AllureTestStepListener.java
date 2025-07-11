package com.xceptance.neodymium.common;

import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.xceptance.neodymium.common.TestStepListener.URL_CHANGED_STEP_MESSAGE;
import static io.qameta.allure.model.Status.PASSED;

public class AllureTestStepListener implements StepLifecycleListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AllureTestStepListener.class);

    private static boolean driverNullWarningLogged = false;

    @Override
    public void beforeStepStop(StepResult result)
    {
        StepLifecycleListener.super.beforeStepStop(result);

        // if the configuration is set to not take screenshots per step, we do not need to do anything
        if (!Neodymium.configuration().screenshotPerStep())
        {
            return;
        }

        // if the driver is not set, we do not need to do anything
        if (Neodymium.getDriver() == null)
        {
            if (!driverNullWarningLogged)
            {
                LOGGER.warn(
                    "Driver is not set in Neodymium and Selenide driver is used. Skipping screenshot capture for each test step. To enable screenshots per" +
                        " step, add the @Browser annotation to your test class or method.");
                driverNullWarningLogged = true;
            }
            return;
        }

        // only take additional screenshots for passing steps other status won't give additional information
        if (result.getStatus() != PASSED)
        {
            return;
        }

        String name = result.getName();

        // safety check for null name
        if (name == null)
        {
            return;
        }

        // skip screenshots for steps with names starting with $(, $$(, $x(, or $$x( or for URL changed steps
        // but don't skip steps that open a URL, to ensure that we capture the initial page load
        if (name.startsWith(URL_CHANGED_STEP_MESSAGE)
            || (name.startsWith("$(") && !name.startsWith("$(\"open\") "))
            || name.startsWith("$$(")
            || name.startsWith("$x(")
            || name.startsWith("$$x("))
        {
            return;
        }

        try
        {
            AllureAddons.attachPNG("beforeStepStop_screenshot");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
