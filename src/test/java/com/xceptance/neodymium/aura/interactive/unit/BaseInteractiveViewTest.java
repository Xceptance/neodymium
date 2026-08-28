/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.aura.interactive.unit;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;
import org.neodymium.util.Neodymium;

/**
 * Base test class for Interactive View unit/integration tests.
 * Establishes standalone server instances and Selenide view ports for headless validation.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("interactive-view")
public abstract class BaseInteractiveViewTest
{
    protected InteractiveConsoleEngine engine;
    protected InteractiveConsoleServer server;
    private long originalTimeout;

    @BeforeEach
    public final void setupServer() throws IOException
    {
        Neodymium.clearThreadContext();

        // Bind mock properties
        System.setProperty("neodymium.ai.interactive", "true");
        System.setProperty("neodymium.ai.interactive.allowHeadlessHUD", "true");
        org.neodymium.ai.config.AiConfiguration.resetInstance();

        // Initialize engine and server on a random free port
        this.engine = new InteractiveConsoleEngine("test-run-id");
        this.server = new InteractiveConsoleServer(this.engine);

        // Configure Selenide — use a generous 30 s timeout to cover multi-step
        // auto-run sequences, rewind flows, and drag-and-drop assertions.
        this.originalTimeout = Configuration.timeout;
        Configuration.timeout = 30000;
        Configuration.headless = true;
    }

    @AfterEach
    public final void tearDownServer()
    {
        Configuration.timeout = this.originalTimeout;

        if (this.server != null)
        {
            this.server.stop();
        }

        try
        {
            Selenide.closeWebDriver();
        }
        catch (final Exception ignored)
        {
        }

        System.clearProperty("neodymium.ai.interactive");
        System.clearProperty("neodymium.ai.interactive.allowHeadlessHUD");
        org.neodymium.ai.config.AiConfiguration.resetInstance();
        Neodymium.clearThreadContext();
    }

    /**
     * Navigates Selenide to the root URL of the active console server.
     */
    protected final void openConsoleUrl()
    {
        Selenide.open(this.server.getLocalUrl());
    }
}
