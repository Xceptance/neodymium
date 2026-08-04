package com.xceptance.neodymium.aura.manager.ui;

import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Playbook-based UI test for Aura Manager workspace files management.
 *
 * @author AI-generated: Gemini 3.6 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@DataFile("ai-test-pages/aura-manager-workspace-test.yaml")
public class AuraManagerWorkspacePlaybookUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerWorkspacePlaybookUiTest()
    {
        super(18130);
    }

    @NeodymiumTest
    @DataSet(id = "Create_Test")
    public void testCreateTest() throws Throwable
    {
        Neodymium.ai().execute();
    }

    @NeodymiumTest
    @DataSet(id = "Delete_Test")
    public void testDeleteTest() throws Throwable
    {
        Neodymium.ai().execute();
    }
}
