package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataSet;

/**
 * Playbook-based UI test for Aura Manager execution history.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@NeodymiumAiTest
public class AuraManagerHistoryPlaybookUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerHistoryPlaybookUiTest()
    {
        super(18160);
    }

    @AiPlaybook("ai-test-pages/aura-manager-history-test.yaml")
    @DataSet(id = "History_List")
    public void testHistoryList() throws Throwable
    {
    }
}
