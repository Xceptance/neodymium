/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit5.tests.auramanager.end2end;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.ai.action.plugins.AiMethod;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.Assertions;
import java.io.File;
import java.io.IOException;

/**
 * Static test helper class for Aura Manager UI tests.
 * Declares all helper methods as public static and annotates them with @AiMethod.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerTestHelper
{
    private static HttpServer auraServer;
    private static int auraPort;
    private static int startPort = 18150;

    private AuraManagerTestHelper()
    {
        // Prevent instantiation
    }

    public static void setStartPort(final int port)
    {
        startPort = port;
    }

    public static HttpServer getAuraServer()
    {
        return auraServer;
    }

    @AiMethod("Starts the Aura Manager HTTP and HTTPS servers on dynamically configured ports")
    public static void startManager() throws IOException
    {
        if (auraServer != null)
        {
            return;
        }
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
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
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    @AiMethod("No-op placeholder method to prevent stopping the manager server prematurely")
    public static void stopManager()
    {
        // No-op for AI Playbook. Server must stay alive across phases.
    }

    @AiMethod("Forcefully shuts down the running Aura Manager server")
    public static void forceStopManager()
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

    @AiMethod("Confirms and accepts any active browser alert prompt")
    public static void acceptAlert()
    {
        Selenide.confirm();
    }

    @AiMethod("Verifies that the checkbox element targeted by the selector is checked")
    public static void verifyCheckboxChecked(final String selector)
    {
        Assertions.assertTrue(Selenide.$(selector).isSelected());
    }

    @AiMethod("Verifies that the checkbox element targeted by the selector is unchecked")
    public static void verifyCheckboxUnchecked(final String selector)
    {
        Assertions.assertFalse(Selenide.$(selector).isSelected());
    }

    @AiMethod("Verifies that the automated-workspace-test.yaml file is present in the workspace file list")
    public static void verifyAutomatedWorkspaceTestPresent()
    {
        Selenide.$("#yamlFileList").shouldHave(Condition.text("automated-workspace-test.yaml"));
    }

    @AiMethod("Verifies that the automated-workspace-test.yaml file is not present in the workspace file list")
    public static void verifyAutomatedWorkspaceTestNotPresent()
    {
        Selenide.$("#yamlFileList").shouldNot(Condition.text("automated-workspace-test.yaml"));
    }

    @AiMethod("Deletes the automated-workspace-test.yaml file from the workspace directory")
    public static void deleteWorkspaceTestFile() throws IOException
    {
        final File targetFile = new File("src/test/resources/automated-workspace-test.yaml").getAbsoluteFile();
        if (targetFile.exists())
        {
            java.nio.file.Files.delete(targetFile.toPath());
        }
    }

    @AiMethod("Creates the automated-workspace-test.yaml file in the workspace directory with template data")
    public static void createWorkspaceTestFile() throws IOException
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
