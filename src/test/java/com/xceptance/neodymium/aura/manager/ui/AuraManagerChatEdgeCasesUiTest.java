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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI edge cases test for the Neodymium Aura Manager AI Chat Assistant
 * session auto-regeneration and blank message validation.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
public final class AuraManagerChatEdgeCasesUiTest
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

        this.chatHistoryDir = new File(System.getProperty("user.dir"), "chat-history");
        cleanupChatHistory();
    }

    @AfterEach
    public final void teardown()
    {
        if (this.server != null)
        {
            NeodymiumAuraManager.stopServer(this.server);
        }
        cleanupChatHistory();
    }

    private final void cleanupChatHistory()
    {
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
    public final void testDeleteLastChatSessionAutoRegeneratesDefault()
    {
        Selenide.open("http://localhost:" + this.port + "/?test=true");
        $("#auraChatLauncher").shouldBe(Condition.visible).click();

        $(".chat-container").shouldBe(Condition.visible);

        final var select = $("#chatSessionSelect");
        select.shouldBe(Condition.visible);
        select.$$("option").shouldHave(CollectionCondition.size(1));

        // Mock confirmation modal and delete the single default session
        Selenide.executeJavaScript("window.confirm = function() { return true; };");
        $(".chat-container .btn-edit[title='Delete Chat']").shouldBe(Condition.visible).click();

        // The session dropdown automatically regenerates a fresh Default Session
        select.$$("option").shouldHave(CollectionCondition.size(1));
        select.getSelectedOption().shouldHave(Condition.text("Default Session"));
    }

    @NeodymiumTest
    public final void testSendBlankMessageDisabled()
    {
        Selenide.open("http://localhost:" + this.port + "/?test=true");
        $("#auraChatLauncher").shouldBe(Condition.visible).click();

        final var chatInput = $("#chatInput");
        chatInput.shouldBe(Condition.visible).setValue("   ").pressEnter();

        // No user chat message bubble is created for whitespace
        $$("#chatMessages .chat-message.user").shouldHave(CollectionCondition.size(0));
    }
}
