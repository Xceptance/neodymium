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
package com.xceptance.aura.test.controller;

import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import com.xceptance.neodymium.aura.AuraQueueService;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

/**
 * Unit test verifying run ID, report URL, and status stream outputs for active run context binding.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraRunContextRedirectTest
{
    private AuraQueueService queueService;
    private AuraFileService fileService;
    private AuraInteractiveService interactiveService;
    private AuraTestQueueController controller;

    @BeforeAll
    public static void beforeAll()
    {
        System.setProperty("neodymium.aura.test", "true");
    }

    @BeforeEach
    public void setUp()
    {
        this.interactiveService = new AuraInteractiveService();
        this.fileService = Mockito.mock(AuraFileService.class);
        this.queueService = new AuraQueueService(null, this.interactiveService);
        this.controller = new AuraTestQueueController(this.queueService, this.fileService, this.interactiveService);
    }

    @Test
    public final void testStatusStreamContainsRunIdAndReportUrl()
    {
        final ResponseEntity<Map<String, Object>> response = controller.getStatusStream("client-123", 0, 0);
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());

        final Map<String, Object> body = response.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.containsKey("status"));

        @SuppressWarnings("unchecked")
        final Map<String, Object> statusObj = (Map<String, Object>) body.get("status");
        Assertions.assertNotNull(statusObj);
        Assertions.assertTrue(statusObj.containsKey("runId"));
        Assertions.assertTrue(statusObj.containsKey("runReportUrl"));
        Assertions.assertNotNull(statusObj.get("runId"));
        Assertions.assertNotNull(statusObj.get("runReportUrl"));
    }
}
