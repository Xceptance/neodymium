package com.xceptance.neodymium.junit5.teststart;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.xceptance.neodymium.ai.core.AiBrowser;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;

public class NeodymiumAfterTestExecutionCallback implements AfterTestExecutionCallback
{
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception
    {
        AiBrowser aiBrowser = Neodymium.ai();
        if (aiBrowser != null)
        {
            aiBrowser.close();
        }
        
        boolean success = !context.getExecutionException().isPresent();
        Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null && playbook.isChanged() && success) {
            PlaybookManager.savePlaybook(playbook);
            AllureAddons.printToReport("Updated Playbook Saved");
        }
    }
}
