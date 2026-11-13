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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Unit tests validating test file list rendering, cookie fallback, folder totals, and tag-based filtering in {@link AuraTestFileController}.
 *
 * @author AI-generated: Antigravity
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public class AuraTestFileControllerTest
{
    private AuraFileService fileService;
    private AuraTestQueueController queueController;
    private AuraTestFileController controller;
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

        controller = new AuraTestFileController(fileService, queueController);
        fileController = controller;
    }

    @Test
    public void testGetFilesPanelPopulatesAvailableTags()
    {
        final YamlFileDto file1 = new YamlFileDto("checkout.yaml", List.of(), false, null, false, Map.of(
            "feature", List.of("checkout"),
            "smoke", List.of("true")
        ));
        final YamlFileDto file2 = new YamlFileDto("login.yaml", List.of(), false, null, false, Map.of(
            "feature", List.of("login")
        ));

        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of(file1, file2));
        Mockito.when(fileService.getStepsFilesList()).thenReturn(Collections.emptyList());
        Mockito.when(fileService.getFragmentRequiredVariablesMap()).thenReturn(Collections.emptyMap());
        Mockito.when(fileService.getExpandedFiles()).thenReturn(Collections.emptySet());
        Mockito.when(queueController.getSelectedQueueKeys()).thenReturn(Collections.emptySet());
        Mockito.when(queueController.getFullySelectedFileKeys(Mockito.anyList())).thenReturn(Collections.emptySet());
        Mockito.when(queueController.getPartiallySelectedFileKeys(Mockito.anyList())).thenReturn(Collections.emptySet());

        final Model model = new ConcurrentModel();
        final String view = controller.getFilesPanel(response, model);

        Assertions.assertEquals("fragments/test-selection :: testSelectionContent", view);
        Assertions.assertEquals(List.of(file1, file2), model.getAttribute("files"));

        @SuppressWarnings("unchecked")
        final List<String> availableTags = (List<String>) model.getAttribute("availableTags");
        Assertions.assertNotNull(availableTags);
        Assertions.assertEquals(List.of("feature:checkout", "feature:login", "smoke:true"), availableTags);
    }
    @Test
    public void testSearchFilesWithTagFilter()
    {
        final YamlFileDto file1 = new YamlFileDto("checkout.yaml", List.of(), false, null, false, Map.of(
            "feature", List.of("checkout"),
            "smoke", List.of("true")
        ));

        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of(file1));
        Mockito.when(fileService.getFilteredYamlFilesList("feature:checkout")).thenReturn(List.of(file1));
        Mockito.when(fileService.getFilteredStepsFilesList("feature:checkout")).thenReturn(Collections.emptyList());
        Mockito.when(fileService.getStepsFilesList()).thenReturn(Collections.emptyList());
        Mockito.when(fileService.getFragmentRequiredVariablesMap()).thenReturn(Collections.emptyMap());
        Mockito.when(fileService.getExpandedFiles()).thenReturn(Set.of());

        final Model model = new ConcurrentModel();
        final String view = controller.searchFiles("feature:checkout", response, model);

        Assertions.assertEquals("fragments/test-selection :: yamlFileListContent", view);
        Assertions.assertEquals(List.of(file1), model.getAttribute("files"));

        @SuppressWarnings("unchecked")
        final List<String> availableTags = (List<String>) model.getAttribute("availableTags");
        Assertions.assertNotNull(availableTags);
        Assertions.assertEquals(List.of("feature:checkout", "smoke:true"), availableTags);
    }
    @Test
    public void testSearchFilesWithMultipleTagFilters()
    {
        final YamlFileDto file1 = new YamlFileDto("checkout.yaml", List.of(), false, null, false, Map.of(
            "feature", List.of("checkout"),
            "smoke", List.of("true")
        ));

        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of(file1));
        Mockito.when(fileService.getFilteredYamlFilesList("feature:checkout, smoke:true")).thenReturn(List.of(file1));
        Mockito.when(fileService.getFilteredStepsFilesList("feature:checkout, smoke:true")).thenReturn(Collections.emptyList());
        Mockito.when(fileService.getStepsFilesList()).thenReturn(Collections.emptyList());
        Mockito.when(fileService.getFragmentRequiredVariablesMap()).thenReturn(Collections.emptyMap());
        Mockito.when(fileService.getExpandedFiles()).thenReturn(Set.of());

        final Model model = new ConcurrentModel();
        final String view = controller.searchFiles("feature:checkout, smoke:true", response, model);

        Assertions.assertEquals("fragments/test-selection :: yamlFileListContent", view);
        Assertions.assertEquals(List.of(file1), model.getAttribute("files"));

        @SuppressWarnings("unchecked")
        final List<String> availableTags = (List<String>) model.getAttribute("availableTags");
        Assertions.assertNotNull(availableTags);
        Assertions.assertEquals(List.of("feature:checkout", "smoke:true"), availableTags);
    }
    @Test
    public void testRenderTestSelectionTemplateWithAvailableTags()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final StandardLinkBuilder linkBuilder = new StandardLinkBuilder()
        {
            @Override
            protected String computeContextPath(final IExpressionContext context, final String base, final Map<String, Object> parameters)
            {
                return "";
            }
        };
        engine.setLinkBuilder(linkBuilder);

        final YamlFileDto file1 = new YamlFileDto("checkout.yaml", List.of(), false, null, false, Map.of(
            "feature", List.of("checkout"),
            "smoke", List.of("true")
        ));

        final Context context = new Context();
        context.setVariable("files", List.of(file1));
        context.setVariable("availableTags", List.of("feature:checkout", "smoke:true"));
        context.setVariable("stepsFiles", Collections.emptyList());
        context.setVariable("expandedFiles", Collections.emptySet());
        context.setVariable("selectedKeys", Collections.emptySet());
        context.setVariable("selectedFileKeys", Collections.emptySet());
        context.setVariable("partiallySelectedFileKeys", Collections.emptySet());

        final String html = engine.process("fragments/test-selection", Collections.singleton("testSelectionContent"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"quickTagFiltersBar\""), "Must contain quick tag filters bar");
        Assertions.assertTrue(html.contains("data-tag=\"feature:checkout\""), "Must contain feature:checkout quick tag button");
        Assertions.assertTrue(html.contains("data-tag=\"smoke:true\""), "Must contain smoke:true quick tag button");
        Assertions.assertTrue(html.contains("filterTestsByTag(this.getAttribute('data-tag'))"), "Must use data-tag in event handler without Thymeleaf string restriction error");
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
    @Test
    public void testPopulateFileModelIncludesFolderTotals()
    {
        Mockito.when(queueController.getFolderTotalDatasetsJson()).thenReturn("{\"tests\":10}");
        final Model model = new ConcurrentModel();
        fileController.getFilesPanel(response, model);

        Assertions.assertEquals("{\"tests\":10}", model.getAttribute("folderTotalDatasetsJson"));
    }
}
