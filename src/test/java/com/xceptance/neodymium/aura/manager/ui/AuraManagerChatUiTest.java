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
package com.xceptance.neodymium.aura.manager.ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI test to verify the Neodymium Aura Manager's AI Chat Assistant
 * sessions, message streams, and session management (TDD).
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class AuraManagerChatUiTest
{
    private HttpServer server;
    private int port;
    private File chatHistoryDir;

    @BeforeEach
    public final void setup() throws IOException
    {
        System.setProperty("neodymium.aura.test", "true");
        this.server = NeodymiumAuraManager.startServer(18109, false);
        this.port = this.server.getAddress().getPort();

        // Clear out chat-history to ensure clean isolated test run
        this.chatHistoryDir = new File(System.getProperty("user.dir"), "chat-history");
        if (this.chatHistoryDir.exists())
        {
            final File[] files = this.chatHistoryDir.listFiles();
            if (files != null)
            {
                for (final File file : files)
                {
                    file.delete();
                }
            }
        }
    }

    @AfterEach
    public final void teardown() throws IOException
    {
        if (this.server != null)
        {
            NeodymiumAuraManager.stopServer(this.server);
        }

        // Clean up chat-history files created during test
        if (this.chatHistoryDir.exists())
        {
            final File[] files = this.chatHistoryDir.listFiles();
            if (files != null)
            {
                for (final File file : files)
                {
                    file.delete();
                }
            }
            this.chatHistoryDir.delete();
        }
    }

    @NeodymiumTest
    @Order(1)
    public final void testDefaultChatSessionLoads()
    {
        Selenide.open("http://localhost:" + this.port + "/?test=true");
        $("#auraChatLauncher").shouldBe(Condition.visible).click();

        // Verify the Aura Assistant panel header is present
        $(".chat-container").shouldBe(Condition.visible);

        // Verify the select dropdown has at least the default session
        final var select = $("#chatSessionSelect");
        select.shouldBe(Condition.visible);
        
        // Under our new server-side scheme, it should have 1 option by default
        select.$$("option").shouldHave(CollectionCondition.size(1));
        select.getSelectedOption().shouldHave(Condition.text("Default Session"));

        // Verify the initial welcome greeting is present
        $(".chat-welcome-card").shouldBe(Condition.visible);
        $(".chat-welcome-title").shouldHave(Condition.text("Hello"));
    }

    @NeodymiumTest
    @Order(2)
    public final void testCreateNewChatSession()
    {
        Selenide.open("http://localhost:" + this.port + "/?test=true");
        $("#auraChatLauncher").shouldBe(Condition.visible).click();

        // Click the "+" button inside the chat header to create a new session
        $(".chat-container .btn-edit[title='New Chat']").shouldBe(Condition.visible).click();

        // The dropdown should now have two sessions
        final var select = $("#chatSessionSelect");
        select.$$("option").shouldHave(CollectionCondition.size(2));

        // The newly active session should be selected
        select.getSelectedOption().shouldHave(Condition.text("New Chat"));
    }

    @NeodymiumTest
    @Order(3)
    public final void testDeleteChatSession()
    {
        Selenide.open("http://localhost:" + this.port + "/?test=true");
        $("#auraChatLauncher").shouldBe(Condition.visible).click();

        // Click "+" to create a new session
        $(".chat-container .btn-edit[title='New Chat']").shouldBe(Condition.visible).click();

        final var select = $("#chatSessionSelect");
        select.$$("option").shouldHave(CollectionCondition.size(2));

        // Now delete the newly created session. We trigger delete button click
        // Mock window.confirm to return true to bypass native modal in headless browser environments
        Selenide.executeJavaScript("window.confirm = function() { return true; };");
        $(".chat-container .btn-edit[title='Delete Chat']").shouldBe(Condition.visible).click();

        try
        {
            for (final String entry : Selenide.getWebDriverLogs(org.openqa.selenium.logging.LogType.BROWSER))
            {
                System.err.println("BROWSER_LOG: " + entry);
            }
        }
        catch (Exception e)
        {
            System.err.println("Failed to fetch browser logs: " + e.getMessage());
        }

        // The deleted session should be gone and dropdown falls back to 1 option
        select.$$("option").shouldHave(CollectionCondition.size(1));
        select.getSelectedOption().shouldHave(Condition.text("Default Session"));
    }

    @NeodymiumTest
    @Order(4)
    public final void testRenameChatSession()
    {
        Selenide.open("http://localhost:" + this.port + "/?test=true");
        $("#auraChatLauncher").shouldBe(Condition.visible).click();

        // Mock window.prompt to return "Renamed Project Discussion"
        Selenide.executeJavaScript("window.prompt = function() { return 'Renamed Project Discussion'; };");

        // Click the rename button inside the chat header
        $(".chat-container .btn-edit[title='Rename Chat']").shouldBe(Condition.visible).click();

        // The selected option in dropdown should reflect the updated name
        final var select = $("#chatSessionSelect");
        select.getSelectedOption().shouldHave(Condition.text("Renamed Project Discussion"));
    }

    @NeodymiumTest
    @Order(5)
    public final void testSendChatMessage()
    {
        Selenide.open("http://localhost:" + this.port + "/?test=true");
        $("#auraChatLauncher").shouldBe(Condition.visible).click();

        // Type a prompt into chat input and submit
        $("#chatInput").shouldBe(Condition.visible).setValue("Hello Aura").pressEnter();

        // Verify that user message bubble is present and styled
        $$("#chatMessages .chat-message.user").shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1));
        $("#chatMessages .chat-message.user").shouldHave(Condition.text("Hello Aura"));

        // Verify that AI reply bubble is rendered and contains content
        $$("#chatMessages .chat-message.ai").shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1));
    }
}
