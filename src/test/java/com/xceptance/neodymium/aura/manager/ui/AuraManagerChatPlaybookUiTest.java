package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Playbook-based UI test for Aura Manager chat.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@DataFile("ai-test-pages/aura-manager-chat-test.yaml")
public class AuraManagerChatPlaybookUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerChatPlaybookUiTest()
    {
        super(18100);
    }

    @NeodymiumTest
    @DataSet(id = "Chat_Message")
    public void testChatMessage() throws Throwable
    {
        Neodymium.ai().execute();
    }
}
