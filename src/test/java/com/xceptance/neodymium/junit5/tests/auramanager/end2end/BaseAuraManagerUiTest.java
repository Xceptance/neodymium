package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.util.Neodymium;
import java.io.IOException;
import java.io.File;
import com.xceptance.neodymium.common.browser.Browser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceLock;

@Browser("Chrome_headless")
@ResourceLock("NeodymiumAuraManager")
public abstract class BaseAuraManagerUiTest extends BaseAiTest
{
    protected final int startPort;

    protected BaseAuraManagerUiTest(final int startPort)
    {
        this.startPort = startPort;
        System.setProperty("neodymium.ai.aura.manager.shutdownDelay", "-1");
    }

    @BeforeEach
    public void setupHelper()
    {
        AuraManagerTestHelper.setStartPort(this.startPort);
        Neodymium.ai().registerMethodClass(AuraManagerTestHelper.class);
    }

    @AfterEach
    public void cleanUpManager()
    {
        Selenide.closeWebDriver();
        AuraManagerTestHelper.forceStopManager();
    }
}
