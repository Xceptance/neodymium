package com.xceptance.neodymium.aura.manager.ui.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;

/**
 * Abstract base class for Aura Manager UI tests.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
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
    }

    @AfterEach
    public void cleanUpManager()
    {
        Selenide.closeWebDriver();
        AuraManagerTestHelper.forceStopManager();
    }
}
