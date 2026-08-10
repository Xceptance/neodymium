package com.xceptance.neodymium.aura.manager.ui.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.neodymium.ai.config.AiConfiguration;

import com.codeborne.selenide.Selenide;
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
public abstract class BaseAuraManagerUiTest
{
    protected final int startPort;

    protected BaseAuraManagerUiTest(final int startPort)
    {
        this.startPort = startPort;
        System.setProperty("neodymium.ai.aura.manager.shutdownDelay", "-1");
    }

    @BeforeEach
    public void setupHelper() throws Exception
    {
        final boolean isInteractive = AiConfiguration.getInstance().isInteractive();
        Assertions.assertFalse(isInteractive,
            "Interactive mode is currently active ('neodymium.ai.interactive'=true or configured in properties). Automated batch UI tests must run with interactive mode disabled (isInteractive=false) to prevent halting and waiting for manual UI console input.");
        AuraManagerTestHelper.setStartPort(this.startPort);
        AuraManagerTestHelper.startManager();
    }

    @AfterEach
    public void cleanUpManager()
    {
        Selenide.closeWebDriver();
        AuraManagerTestHelper.forceStopManager();
    }
}
