package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataSet;

/**
 * Playbook-based UI test for Aura Manager workspace files management.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@NeodymiumAiTest
public class AuraManagerWorkspacePlaybookUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerWorkspacePlaybookUiTest()
    {
        super(18130);
    }

    @AiPlaybook("ai-test-pages/aura-manager-workspace-create-test.yaml")
    @DataSet(id = "Create_Test")
    public void testCreateTest() throws Throwable
    {
    }

    @AiPlaybook("ai-test-pages/aura-manager-workspace-delete-test.yaml")
    @DataSet(id = "Delete_Test")
    public void testDeleteTest() throws Throwable
    {
    }
}
