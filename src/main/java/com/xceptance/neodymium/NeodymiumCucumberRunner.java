package com.xceptance.neodymium;

import java.io.IOException;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.util.Neodymium;

import io.cucumber.junit.NeodymiumCucumberWrapper;

public class NeodymiumCucumberRunner extends NeodymiumCucumberWrapper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumRunner.class);

    public NeodymiumCucumberRunner(Class<?> clazz) throws InitializationError, IOException
    {
        super(clazz);
        LOGGER.info("Running a test using Neodymium Library (version: " + Neodymium.getNeodymiumVersion()
                    + "), see also https://github.com/Xceptance/neodymium-library");
    }

    @Override
    public void run(RunNotifier notifier)
    {
        // we add our own run listener in order to attach screenshots taken by Selenide to the Allure report
        // this also necessary to clear the context between tests
        notifier.addListener(new NeodymiumCucumberRunListener());
        super.run(notifier);
    }
}
