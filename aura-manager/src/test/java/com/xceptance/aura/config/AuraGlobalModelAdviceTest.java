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
package com.xceptance.aura.config;

import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * Unit test verifying AuraGlobalModelAdvice controller advice model attribute population.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraGlobalModelAdviceTest
{
    private AuraQueueService queueService;
    private AuraGlobalModelAdvice advice;

    @BeforeAll
    public static void beforeAll()
    {
        System.setProperty("neodymium.aura.test", "true");
    }

    @BeforeEach
    public void setUp()
    {
        final AuraInteractiveService interactiveService = new AuraInteractiveService();
        this.queueService = new AuraQueueService(null, interactiveService);
        this.advice = new AuraGlobalModelAdvice(this.queueService);
    }

    @Test
    public final void testAddGlobalAttributesWhenNoRunActive()
    {
        final Model model = new ConcurrentModel();
        advice.addGlobalAttributes(model);

        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("hasRunContext"));
        Assertions.assertEquals("", model.getAttribute("activeRunId"));
        Assertions.assertEquals("", model.getAttribute("activeRunStatus"));
        Assertions.assertEquals("", model.getAttribute("activeRunReportUrl"));
        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("running"));
    }
}
