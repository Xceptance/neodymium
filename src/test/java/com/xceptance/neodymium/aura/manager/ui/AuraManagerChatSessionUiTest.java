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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.aura.manager.ui.base.BaseAuraManagerUiTest;
import com.xceptance.neodymium.aura.manager.ui.base.AuraManagerTestHelper;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

/**
 * Selenide-based UI tests covering chat session handling in Aura Manager:
 * <ul>
 *   <li>Creating new chat sessions twice in succession without DOM targets vanishing.</li>
 *   <li>Renaming chat sessions twice in succession.</li>
 *   <li>Deleting chat sessions twice in succession without triggering htmx:targetError.</li>
 * </ul>
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuraManagerChatSessionUiTest extends BaseAuraManagerUiTest
{
    public AuraManagerChatSessionUiTest()
    {
        super(18180);
    }

    @NeodymiumTest
    @Order(1)
    public void testCreateChatSessionTwice() throws Throwable
    {
        AuraManagerTestHelper.startManager();
        final String url = Neodymium.getData().get("auraManagerUrl");
        Selenide.open(url);

        $("#auraChatLauncher").shouldBe(Condition.visible).click();
        $("#chatContainer").shouldBe(Condition.visible);

        final int initialSessionCount = $$("#chatSessionSelect option").size();

        // First Create
        $("button[title='New Chat']").shouldBe(Condition.visible).click();
        Selenide.sleep(500);

        $("#chatContainer").shouldBe(Condition.visible);
        Assertions.assertTrue($$("#chatSessionSelect option").size() > initialSessionCount,
                "Expected session count to increase after first create");

        final int countAfterFirst = $$("#chatSessionSelect option").size();

        // Second Create (testing feature used twice in succession)
        $("button[title='New Chat']").shouldBe(Condition.visible).click();
        Selenide.sleep(500);

        $("#chatContainer").shouldBe(Condition.visible);
        Assertions.assertTrue($$("#chatSessionSelect option").size() > countAfterFirst,
                "Expected session count to increase after second create");
    }

    @NeodymiumTest
    @Order(2)
    public void testRenameChatSessionTwice() throws Throwable
    {
        AuraManagerTestHelper.startManager();
        final String url = Neodymium.getData().get("auraManagerUrl");
        Selenide.open(url);

        $("#auraChatLauncher").shouldBe(Condition.visible).click();
        $("#chatContainer").shouldBe(Condition.visible);

        // First Rename
        $("button[title='Rename Chat']").shouldBe(Condition.visible).click();
        Selenide.switchTo().alert().sendKeys("Session Alpha");
        Selenide.switchTo().alert().accept();
        Selenide.sleep(500);

        $("#chatContainer").shouldBe(Condition.visible);
        $("#chatSessionSelect option:checked").shouldHave(Condition.text("Session Alpha"));

        // Second Rename (testing feature used twice in succession)
        $("button[title='Rename Chat']").shouldBe(Condition.visible).click();
        Selenide.switchTo().alert().sendKeys("Session Beta");
        Selenide.switchTo().alert().accept();
        Selenide.sleep(500);

        $("#chatContainer").shouldBe(Condition.visible);
        $("#chatSessionSelect option:checked").shouldHave(Condition.text("Session Beta"));
    }

    @NeodymiumTest
    @Order(3)
    public void testDeleteChatSessionTwice() throws Throwable
    {
        AuraManagerTestHelper.startManager();
        final String url = Neodymium.getData().get("auraManagerUrl");
        Selenide.open(url);

        $("#auraChatLauncher").shouldBe(Condition.visible).click();
        $("#chatContainer").shouldBe(Condition.visible);

        // Provision extra sessions first so we can delete twice
        $("button[title='New Chat']").click();
        Selenide.sleep(300);
        $("button[title='New Chat']").click();
        Selenide.sleep(300);

        final int startCount = $$("#chatSessionSelect option").size();

        // First Delete
        $("button[title='Delete Chat']").shouldBe(Condition.visible).click();
        Selenide.switchTo().alert().accept();
        Selenide.sleep(500);

        $("#chatContainer").shouldBe(Condition.visible);
        Assertions.assertEquals(startCount - 1, $$("#chatSessionSelect option").size());

        // Second Delete (verifies no htmx:targetError occurs on repeated delete)
        $("button[title='Delete Chat']").shouldBe(Condition.visible).click();
        Selenide.switchTo().alert().accept();
        Selenide.sleep(500);

        $("#chatContainer").shouldBe(Condition.visible);
        Assertions.assertEquals(startCount - 2, $$("#chatSessionSelect option").size());
    }
}
