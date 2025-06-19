package com.xceptance.neodymium.junit5;

import com.codeborne.selenide.logevents.SelenideLogger;
import com.google.common.collect.ImmutableMap;
import com.xceptance.neodymium.common.TestStepListener;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.AllureAddons.EnvironmentInfoMode;
import com.xceptance.neodymium.util.Neodymium;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.stream.Stream;

public class NeodymiumRunner implements TestTemplateInvocationContextProvider
{

    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumRunner.class);

    public static final String LISTENER_NAME = "allure-selenide-java";

    private static boolean neoVersionLogged = false;

    private NeodymiumData neoData;

    public NeodymiumRunner()
    {
        SelenideLogger.addListener(LISTENER_NAME, new AllureSelenide());

        SelenideLogger.addListener(TestStepListener.LISTENER_NAME, new TestStepListener());

        if (!neoVersionLogged && Neodymium.configuration().logNeoVersion())
        {
            if (!AllureAddons.envFileExists())
            {
                LOGGER.info("This test uses Neodymium Library (version: " + Neodymium.getNeodymiumVersion()
                            + "), MIT License, more details on https://github.com/Xceptance/neodymium-library");
                neoVersionLogged = true;
                AllureAddons.addEnvironmentInformation(ImmutableMap.<String, String> builder()
                                                                   .put("Testing Framework", "Neodymium " + Neodymium.getNeodymiumVersion())
                                                                   .build(),
                                                       EnvironmentInfoMode.IGNORE);
            }
        }
        AllureAddons.initializeEnvironmentInformation();

    }

    public enum DescriptionMode
    {
        flat,
        tree,
    };

    @Override
    public boolean supportsTestTemplate(ExtensionContext context)
    {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context)
    {
        // clear the context before next child run
        Neodymium.clearThreadContext();

        Class<?> testClass = context.getRequiredTestClass();
        Method templateMethod = context.getRequiredTestMethod();
        if (neoData == null)
        {
            neoData = new NeodymiumData(testClass);
        }
        return neoData.computeTestMethods(templateMethod);
    }
}
