package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import org.neodymium.common.testdata.DataSet;

/**
 * Playbook-based UI test for Aura Manager chat.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@NeodymiumAiTest
public class AuraManagerChatPlaybookUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerChatPlaybookUiTest()
    {
        super(18100);
    }

    @AiPlaybook("ai-test-pages/aura-manager-chat-test.yaml")
    @DataSet(id = "Chat_Message")
    public void testChatMessage() throws Throwable
    {
    }
}
