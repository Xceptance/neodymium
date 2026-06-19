package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.util.Neodymium;
import java.io.IOException;
import java.io.File;
import com.xceptance.neodymium.common.browser.Browser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.parallel.ResourceLock;
import com.xceptance.neodymium.ai.action.plugins.AiMethod;

@Browser("Chrome_headless")
@ResourceLock("NeodymiumAuraManager")
public abstract class BaseAuraManagerUiTest extends BaseAiTest
{
    protected HttpServer auraServer;
    protected int auraPort;
    protected final int startPort;

    protected BaseAuraManagerUiTest(final int startPort)
    {
        this.startPort = startPort;
        System.setProperty("neodymium.ai.aura.manager.shutdownDelay", "-1");
    }

    @AiMethod("Starts the Aura Manager HTTP and HTTPS servers on dynamically configured ports")
    public void startManager() throws IOException
    {
        if (auraServer != null) {
            return;
        }
        final java.io.File targetFile = new java.io.File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        final String testName = Neodymium.getTestName();
        if (testName != null)
        {
            if (testName.contains("testCreateTest") || testName.contains("Create_Test"))
            {
                if (targetFile.exists())
                {
                    java.nio.file.Files.delete(targetFile.toPath());
                }
            }
            else if (testName.contains("testDeleteTest") || testName.contains("Delete_Test"))
            {
                if (!targetFile.exists())
                {
                    final String boilerplate = "# Neodymium YAML Test Data File\n" +
                                               "steps: |\n" +
                                               "  Open browser\n" +
                                               "data:\n" +
                                               "  - testId: \"Automated Workspace Test\"\n";
                    java.nio.file.Files.writeString(targetFile.toPath(), boilerplate, java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }

        // Start manager on dynamic port
        auraServer = NeodymiumAuraManager.startServer(startPort, true);
        auraPort = auraServer.getAddress().getPort();
        Neodymium.getData().put("auraManagerUrl", "http://127.0.0.1:" + auraPort);
        System.out.println("Started AuraManager on http://127.0.0.1:" + auraPort);
        try
        {
            Thread.sleep(1500);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    @AiMethod("No-op placeholder method to prevent stopping the manager server prematurely")
    public void stopManager()
    {
        // No-op for AI Playbook. Server must stay alive across phases.
    }

    @AiMethod("Forcefully shuts down the running Aura Manager server")
    public void forceStopManager()
    {
        if (auraServer != null)
        {
            NeodymiumAuraManager.stopServer(auraServer);
            auraServer = null;
            try
            {
                Thread.sleep(500);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    @AfterEach
    public void cleanUpManager()
    {
        Selenide.closeWebDriver();
        forceStopManager();
    }

    @AiMethod("Confirms and accepts any active browser alert prompt")
    public void acceptAlert()
    {
        Selenide.confirm();
    }

    @AiMethod("Verifies that the checkbox element targeted by the selector is checked")
    public void verifyCheckboxChecked(final String selector)
    {
        Assertions.assertTrue(Selenide.$(selector).isSelected());
    }

    @AiMethod("Verifies that the checkbox element targeted by the selector is unchecked")
    public void verifyCheckboxUnchecked(final String selector)
    {
        Assertions.assertFalse(Selenide.$(selector).isSelected());
    }

    @AiMethod("Verifies that the automated-workspace-test.yaml file is present in the workspace file list")
    public void verifyAutomatedWorkspaceTestPresent()
    {
        Selenide.$("#yamlFileList").shouldHave(Condition.text("automated-workspace-test.yaml"));
    }

    @AiMethod("Verifies that the automated-workspace-test.yaml file is not present in the workspace file list")
    public void verifyAutomatedWorkspaceTestNotPresent()
    {
        Selenide.$("#yamlFileList").shouldNot(Condition.text("automated-workspace-test.yaml"));
    }

    @AiMethod("Deletes the automated-workspace-test.yaml file from the workspace directory")
    public void deleteWorkspaceTestFile() throws IOException
    {
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        if (targetFile.exists())
        {
            java.nio.file.Files.delete(targetFile.toPath());
        }
    }

    @AiMethod("Creates the automated-workspace-test.yaml file in the workspace directory with template data")
    public void createWorkspaceTestFile() throws IOException
    {
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        if (!targetFile.exists())
        {
            final String boilerplate = "# Neodymium YAML Test Data File\n" +
                                       "steps: |\n" +
                                       "  Open browser\n" +
                                       "data:\n" +
                                       "  - testId: \"Automated Workspace Test\"\n";
            java.nio.file.Files.writeString(targetFile.toPath(), boilerplate, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
