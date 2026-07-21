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
package com.xceptance.neodymium.aura.manager;

import static com.codeborne.selenide.Selenide.$;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI test to verify the Neodymium Aura Manager's base dashboard layout and
 * server-side Theme Switcher functionality under Thymeleaf and HTMX.
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class AuraManagerDashboardBaseLayoutUiTest
{
    private HttpServer server;
    private int port;

    @BeforeEach
    public final void setup() throws IOException
    {
        System.setProperty("neodymium.aura.test", "true");
        this.server = NeodymiumAuraManager.startServer(18888, false);
        this.port = this.server.getAddress().getPort();
    }

    @AfterEach
    public final void teardown()
    {
        if (this.server != null)
        {
            NeodymiumAuraManager.stopServer(this.server);
        }
    }

    @NeodymiumTest
    public final void testBaseStructureRendering()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Verify key structural containers are present and visible
        $(".sidebar").shouldBe(Condition.visible);
        $("#navWorkspace").shouldBe(Condition.visible);
        $("#navReports").shouldBe(Condition.visible);
        $(".main-container").shouldBe(Condition.visible);
        $(".top-navbar").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testThemeToggle()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Verify the theme switching buttons render
        $("#themeLight").shouldBe(Condition.visible);
        $("#themeDark").shouldBe(Condition.visible);
        $("#themeSystem").shouldBe(Condition.visible);

        // Toggle Dark theme
        $("#themeDark").click();
        $("html").shouldHave(Condition.cssClass("force-dark"));

        // Toggle Light theme
        $("#themeLight").click();
        $("html").shouldHave(Condition.cssClass("force-light"));

        // Toggle System theme
        $("#themeSystem").click();
        $("html").shouldNotHave(Condition.cssClass("force-dark"));
        $("html").shouldNotHave(Condition.cssClass("force-light"));
    }
}
