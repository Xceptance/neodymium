/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.junit4;

import java.util.LinkedList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.logevents.SelenideLogger;
import org.neodymium.util.Neodymium;

import io.qameta.allure.Attachment;
import io.qameta.allure.selenide.AllureSelenide;

public class NeodymiumCucumberRunListener extends RunListener
{
    public static final String LISTENER_NAME = "allure-selenide-cucumber";

    private static final Logger LOGGER = LoggerFactory.getLogger(NeodymiumCucumberRunListener.class);

    private List<Failure> failures = new LinkedList<>();

    public NeodymiumCucumberRunListener()
    {
        SelenideLogger.addListener(LISTENER_NAME, new AllureSelenide());
    }

    @Override
    public void testStarted(Description description) throws Exception
    {
        Neodymium.clearThreadContext();
        LOGGER.debug("Test started: " + description.toString());
    }

    @Override
    public void testFinished(Description description) throws Exception
    {
        LOGGER.debug("Test finished: " + description.toString());
    }

    @Override
    public void testFailure(Failure failure) throws Exception
    {
        LOGGER.debug("Test failed: " + failure);
        failures.add(failure);
    }

    @Attachment(type = "image/png", value = "{filename}", fileExtension = ".png")
    public byte[] attachPNG(byte[] filedata, String filename)
    {
        return filedata;
    }

    @Attachment(type = "text/plain", value = "{filename}")
    public byte[] attachTxt(byte[] filedata, String filename)
    {
        return filedata;
    }

    public boolean hasFailure()
    {
        return (failures.size() > 0);
    }

    public List<Failure> getFailures()
    {
        return failures;
    }
}
