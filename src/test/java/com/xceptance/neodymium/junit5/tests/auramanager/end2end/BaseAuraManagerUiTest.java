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

    public void stopManager()
    {
        // No-op for AI Playbook. Server must stay alive across phases.
    }

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

    public void acceptAlert()
    {
        Selenide.confirm();
    }

    public void verifyCheckboxChecked(final String selector)
    {
        Assertions.assertTrue(Selenide.$(selector).isSelected());
    }

    public void verifyCheckboxUnchecked(final String selector)
    {
        Assertions.assertFalse(Selenide.$(selector).isSelected());
    }

    public void verifyAutomatedWorkspaceTestPresent()
    {
        Selenide.$("#yamlFileList").shouldHave(Condition.text("automated-workspace-test.yaml"));
    }

    public void verifyAutomatedWorkspaceTestNotPresent()
    {
        Selenide.$("#yamlFileList").shouldNot(Condition.text("automated-workspace-test.yaml"));
    }

    public void deleteWorkspaceTestFile() throws IOException
    {
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        if (targetFile.exists())
        {
            java.nio.file.Files.delete(targetFile.toPath());
        }
    }

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
