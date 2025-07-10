package com.xceptance.neodymium.common;

import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;
import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.StepResult;

import java.io.IOException;

import static com.xceptance.neodymium.common.TestStepListener.URL_CHANGED_STEP_MESSAGE;
import static io.qameta.allure.model.Status.PASSED;

public class AllureTestStepListener implements StepLifecycleListener
{
    @Override
    public void beforeStepStop(StepResult result)
    {
        StepLifecycleListener.super.beforeStepStop(result);

        // if the configuration is set to not take screenshots per step, we do not need to do anything
        if (!Neodymium.configuration().screenshotPerStep())
        {
            return;
        }

        if (Neodymium.getDriver() == null)
        {
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
        if (name.startsWith(URL_CHANGED_STEP_MESSAGE) || name.startsWith("$(") || name.startsWith("$$(") || name.startsWith("$x(") || name.startsWith("$$x("))
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
