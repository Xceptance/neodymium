package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Playbook-based UI test for Aura Manager core theme toggling and view switching.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@DataFile("ai-test-pages/aura-manager-core-ui-test.yaml")
public class AuraManagerCoreUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerCoreUiTest()
    {
        super(18120);
    }

    @NeodymiumTest
    @DataSet(id = "Theme_Toggling")
    public void testThemeToggling() throws Throwable
    {
        Neodymium.ai().execute();
    }

    @NeodymiumTest
    @DataSet(id = "View_Switching")
    public void testViewSwitching() throws Throwable
    {
        Neodymium.ai().execute();
    }
}
