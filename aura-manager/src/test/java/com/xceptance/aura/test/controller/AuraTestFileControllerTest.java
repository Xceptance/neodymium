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
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * Unit tests for AuraTestFileController verifying search and list persistence.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class AuraTestFileControllerTest
{
    private AuraFileService fileService;
    private AuraTestQueueController queueController;
    private AuraTestFileController fileController;
    private HttpServletResponse response;

    @BeforeEach
    public void setUp()
    {
        fileService = Mockito.mock(AuraFileService.class);
        queueController = Mockito.mock(AuraTestQueueController.class);
        response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of());
        Mockito.when(fileService.getStepsFilesList()).thenReturn(List.of());
        Mockito.when(fileService.getExpandedFiles()).thenReturn(Set.of());

        fileController = new AuraTestFileController(fileService, queueController);
    }

    @Test
    public void testGetFilesListFragmentWithoutQueryReturnsFullList()
    {
        final Model model = new ConcurrentModel();
        final String view = fileController.getFilesListFragment("", response, model);

        Assertions.assertEquals("fragments/test-selection :: yamlFileListContent", view);
        Mockito.verify(fileService).getYamlFilesList();
        Mockito.verify(fileService, Mockito.never()).getFilteredYamlFilesList(Mockito.anyString());
    }

    @Test
    public void testGetFilesListFragmentWithQueryReturnsFilteredList()
    {
        final Model model = new ConcurrentModel();
        final YamlFileDto fileDto = new YamlFileDto("CheckoutTest.yaml", List.of());
        Mockito.when(fileService.getFilteredYamlFilesList("checkout")).thenReturn(List.of(fileDto));
        Mockito.when(fileService.getFilteredStepsFilesList("checkout")).thenReturn(List.of("checkout.steps"));

        final String view = fileController.getFilesListFragment("checkout", response, model);

        Assertions.assertEquals("fragments/test-selection :: yamlFileListContent", view);
        Mockito.verify(fileService).getFilteredYamlFilesList("checkout");
        Mockito.verify(fileService).getFilteredStepsFilesList("checkout");
        Assertions.assertEquals(List.of(fileDto), model.getAttribute("files"));
        Assertions.assertEquals(List.of("checkout.steps"), model.getAttribute("stepsFiles"));
    }

    @Test
    public void testFilesListFragmentWithCookieFallback()
    {
        final Model model = new ConcurrentModel();
        final YamlFileDto fileDto = new YamlFileDto("CheckoutTest.yaml", List.of());
        Mockito.when(fileService.getFilteredYamlFilesList("checkout")).thenReturn(List.of(fileDto));
        Mockito.when(fileService.getFilteredStepsFilesList("checkout")).thenReturn(List.of("checkout.steps"));

        final String view = fileController.getFilesListFragment(null, "checkout", response, model);

        Assertions.assertEquals("fragments/test-selection :: yamlFileListContent", view);
        Mockito.verify(fileService).getFilteredYamlFilesList("checkout");
        Mockito.verify(fileService).getFilteredStepsFilesList("checkout");
        Assertions.assertEquals(List.of(fileDto), model.getAttribute("files"));
        Assertions.assertEquals(List.of("checkout.steps"), model.getAttribute("stepsFiles"));
    }

    @Test
    public void testFilesListFragmentWithEncodedCookieFallback()
    {
        final Model model = new ConcurrentModel();
        final YamlFileDto fileDto = new YamlFileDto("CheckoutTest.yaml", List.of());
        Mockito.when(fileService.getFilteredYamlFilesList("checkout flow")).thenReturn(List.of(fileDto));
        Mockito.when(fileService.getFilteredStepsFilesList("checkout flow")).thenReturn(List.of("checkout.steps"));

        final String view = fileController.getFilesListFragment(null, "checkout%20flow", response, model);

        Assertions.assertEquals("fragments/test-selection :: yamlFileListContent", view);
        Mockito.verify(fileService).getFilteredYamlFilesList("checkout flow");
        Mockito.verify(fileService).getFilteredStepsFilesList("checkout flow");
        Assertions.assertEquals(List.of(fileDto), model.getAttribute("files"));
        Assertions.assertEquals(List.of("checkout.steps"), model.getAttribute("stepsFiles"));
    }

    @Test
    public void testSearchFilesReturnsFilteredList()
    {
        final Model model = new ConcurrentModel();
        Mockito.when(fileService.getFilteredYamlFilesList("cart")).thenReturn(List.of());
        Mockito.when(fileService.getFilteredStepsFilesList("cart")).thenReturn(List.of());

        final String view = fileController.searchFiles("cart", response, model);

        Assertions.assertEquals("fragments/test-selection :: yamlFileListContent", view);
        Mockito.verify(fileService).getFilteredYamlFilesList("cart");
        Mockito.verify(fileService).getFilteredStepsFilesList("cart");
    }

    @Test
    public void testGetFilesPanelReturnsTestSelectionContent()
    {
        final Model model = new ConcurrentModel();
        final String view = fileController.getFilesPanel(response, model);

        Assertions.assertEquals("fragments/test-selection :: testSelectionContent", view);
        Mockito.verify(fileService).getYamlFilesList();
    }
}
