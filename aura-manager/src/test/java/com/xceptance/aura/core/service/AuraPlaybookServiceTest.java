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
package com.xceptance.aura.core.service;

import com.xceptance.aura.core.dto.AuraOperationResult;
import com.xceptance.aura.core.dto.PlaybookSummaryDto;
import com.xceptance.aura.core.service.impl.AuraPlaybookServiceImpl;
import com.xceptance.aura.mcp.security.HostAllowlistValidator;
import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

/**
 * Unit tests for AuraPlaybookServiceImpl.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class AuraPlaybookServiceTest
{
    private AuraFileService fileService;
    private HostAllowlistValidator allowlistValidator;
    private AuraPlaybookService playbookService;

    @BeforeEach
    public void setUp()
    {
        fileService = Mockito.mock(AuraFileService.class);
        allowlistValidator = new HostAllowlistValidator();
        playbookService = new AuraPlaybookServiceImpl(fileService, allowlistValidator);
    }

    @Test
    public void testListPlaybooksFilter()
    {
        final YamlFileDto f1 = new YamlFileDto("tests/Login.yaml", List.of());
        final YamlFileDto f2 = new YamlFileDto("tests/Cart.yaml", List.of());
        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of(f1, f2));

        final List<PlaybookSummaryDto> all = playbookService.listPlaybooks(null);
        Assertions.assertEquals(2, all.size());

        final List<PlaybookSummaryDto> filtered = playbookService.listPlaybooks("cart");
        Assertions.assertEquals(1, filtered.size());
        Assertions.assertEquals("tests/Cart.yaml", filtered.get(0).getFile());
    }

    @Test
    public void testReadPlaybookPathTraversalBlocked()
    {
        final AuraOperationResult<String> res = playbookService.readPlaybook("../../etc/passwd");
        Assertions.assertEquals(AuraOperationResult.Status.ERROR, res.getStatus());
        Assertions.assertTrue(res.getMessage().contains("Invalid or unsafe test path"));
    }

    @Test
    public void testReadPlaybookSuccess() throws IOException
    {
        Mockito.when(fileService.readYamlFileContent("tests/Login.yaml")).thenReturn("steps: []");
        final AuraOperationResult<String> res = playbookService.readPlaybook("tests/Login.yaml");
        Assertions.assertTrue(res.isSuccess());
        Assertions.assertEquals("steps: []", res.getData());
    }

    @Test
    public void testSavePlaybookEmptyContentBlocked()
    {
        final AuraOperationResult<Void> res = playbookService.savePlaybook("tests/Login.yaml", "   ");
        Assertions.assertEquals(AuraOperationResult.Status.ERROR, res.getStatus());
        Assertions.assertTrue(res.getMessage().contains("cannot be empty"));
    }

    @Test
    public void testDeletePlaybookNotFound() throws IOException
    {
        Mockito.when(fileService.deleteYamlFile("tests/Unknown.yaml")).thenReturn(false);
        final AuraOperationResult<Boolean> res = playbookService.deletePlaybook("tests/Unknown.yaml");
        Assertions.assertEquals(AuraOperationResult.Status.NOT_FOUND, res.getStatus());
    }
}
