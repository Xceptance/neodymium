package com.xceptance.neodymium.common;

import static io.qameta.allure.model.Status.PASSED;

import java.io.IOException;

import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;

import io.qameta.allure.listener.StepLifecycleListener;
import io.qameta.allure.model.StepResult;

public class AllureTestStepListener implements StepLifecycleListener
{
    @Override
    public void beforeStepStop(StepResult result)
    {
        StepLifecycleListener.super.beforeStepStop(result);

        // check if there is an actual page shown and not only an empty browser
        if ("data:,".equals(Neodymium.getDriver().getCurrentUrl())){
            return;
        }

        // only take additional screenshots for passing steps other status won't give additional information
        if (result.getStatus() != PASSED) {
            return;
        }

        if (Neodymium.configuration().screenshotPerStep())
        {
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
}
