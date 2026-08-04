package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Playbook-based UI test for Aura Manager execution history.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@DataFile("ai-test-pages/aura-manager-history-test.yaml")
public class AuraManagerHistoryPlaybookUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerHistoryPlaybookUiTest()
    {
        super(18160);
    }

    @NeodymiumTest
    @DataSet(id = "History_List")
    public void testHistoryList() throws Throwable
    {
        Neodymium.ai().execute();
    }
}
