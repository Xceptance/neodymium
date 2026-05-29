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
package com.xceptance.neodymium.ai;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.xceptance.neodymium.ai.util.EmbeddedHtmlServer;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;

/**
 * Base class for all AI tests. 
 * Automatically manages an embedded HTTP server and determines the test URL based on class/method name.
 */
public abstract class BaseAiTest
{
    protected static EmbeddedHtmlServer server;
    protected String currentTestUrl;

    /**
     * Starts the embedded server before any tests in the class are run.
     * 
     * @throws IOException if server fails to start
     */
    @BeforeAll
    public static void startServer() throws IOException
    {
        server = new EmbeddedHtmlServer();
        server.start();
        // Optional: you could configure Selenide here if necessary
        // Configuration.baseUrl = "http://localhost:" + server.getPort();
    }

    /**
     * Stops the embedded server after all tests in the class have finished.
     */
    @AfterAll
    public static void stopServer()
    {
        if (server != null)
        {
            server.stop();
        }
    }

    /**
     * Prepares the URL for the current test case based on class and method name.
     * 
     * @param testInfo the JUnit 5 test info injected automatically
     */
    @BeforeEach
    public void setupPageUrl(final TestInfo testInfo)
    {
        final String className = testInfo.getTestClass().get().getSimpleName();
        final String methodName = testInfo.getTestMethod().get().getName();
        
        currentTestUrl = String.format("http://localhost:%d/%s/%s.html", server.getPort(), className, methodName);
    }
}
