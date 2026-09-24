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
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * Unit tests for AuraTestEditorController.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class AuraTestEditorControllerTest
{
    private AuraFileService fileService;
    private AuraTestEditorController controller;

    @BeforeEach
    public void setUp()
    {
        fileService = Mockito.mock(AuraFileService.class);
        controller = new AuraTestEditorController(fileService);
    }

    @Test
    public void testGetEditorFragmentWithFile() throws Exception
    {
        final Model model = new ConcurrentModel();
        final String sampleYaml = "steps:\n  - Open ${neodymium.url}";
        Mockito.when(fileService.readYamlFileContent("test.yaml")).thenReturn(sampleYaml);
        Mockito.when(fileService.parsePlaybookSections(sampleYaml)).thenReturn(Map.of(
            "beforeSteps", List.of(),
            "mainSteps", List.of("Open ${neodymium.url}"),
            "afterSteps", List.of(),
            "dataMatrix", List.of(),
            "varKeys", List.of()
        ));
        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of());

        final String view = controller.getEditorFragment("test.yaml", model);

        Assertions.assertEquals("fragments/editor :: editorPanelContent", view);
        Assertions.assertEquals("test.yaml", model.getAttribute("activeEditingFile"));
        Assertions.assertEquals(sampleYaml, model.getAttribute("editingFileContent"));
        Assertions.assertEquals(false, model.getAttribute("hasParseError"));
        Assertions.assertEquals("visual", model.getAttribute("editorMode"));
        Assertions.assertEquals(false, model.getAttribute("isFragment"));
    }

    @Test
    public void testGetEditorFragmentWithStepsFile() throws Exception
    {
        final Model model = new ConcurrentModel();
        final String sampleSteps = "steps:\n  - Open ${neodymium.url}\nvariables:\n  neodymium.url: defined";
        Mockito.when(fileService.readYamlFileContent("fragments/login.steps")).thenReturn(sampleSteps);
        Mockito.when(fileService.parsePlaybookSections(sampleSteps)).thenReturn(Map.of(
            "beforeSteps", List.of(),
            "mainSteps", List.of("Open ${neodymium.url}"),
            "afterSteps", List.of(),
            "dataMatrix", List.of(),
            "varKeys", List.of(),
            "fragmentVarScopes", Map.of("neodymium.url", "defined")
        ));
        Mockito.when(fileService.getStepsFilesList()).thenReturn(List.of("fragments/login.steps"));

        final String view = controller.getEditorFragment("fragments/login.steps", model);

        Assertions.assertEquals("fragments/editor :: editorPanelContent", view);
        Assertions.assertEquals("fragments/login.steps", model.getAttribute("activeEditingFile"));
        Assertions.assertEquals(sampleSteps, model.getAttribute("editingFileContent"));
        Assertions.assertEquals(true, model.getAttribute("isFragment"));
        Assertions.assertEquals(Map.of("neodymium.url", "defined"), model.getAttribute("fragmentVarScopes"));
    }

    @Test
    public void testGetEditorFragmentWithDefectiveYamlFile() throws Exception
    {
        final Model model = new ConcurrentModel();
        final String brokenYaml = "steps: |\n    Open url\n  Close banner";
        final String parseError = "expected <block end>, but found '<scalar>'";
        Mockito.when(fileService.readYamlFileContent("broken.yaml")).thenReturn(brokenYaml);
        Mockito.when(fileService.parsePlaybookSections(brokenYaml)).thenReturn(Map.of(
            "hasError", true,
            "error", parseError,
            "beforeSteps", List.of(),
            "mainSteps", List.of(),
            "afterSteps", List.of(),
            "dataMatrix", List.of(),
            "varKeys", List.of()
        ));
        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of());

        final String view = controller.getEditorFragment("broken.yaml", model);

        Assertions.assertEquals("fragments/editor :: editorPanelContent", view);
        Assertions.assertEquals("broken.yaml", model.getAttribute("activeEditingFile"));
        Assertions.assertEquals(brokenYaml, model.getAttribute("fileContent"));
        Assertions.assertEquals(true, model.getAttribute("hasParseError"));
        Assertions.assertEquals(parseError, model.getAttribute("parseErrorMessage"));
        Assertions.assertEquals("raw", model.getAttribute("editorMode"));
    }

    @Test
    public void testGetEditorFragmentWithoutFile()
    {
        final Model model = new ConcurrentModel();
        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of());

        final String view = controller.getEditorFragment("", model);

        Assertions.assertEquals("fragments/editor :: editorPanelContent", view);
        Assertions.assertEquals("", model.getAttribute("activeEditingFile"));
        Assertions.assertEquals("", model.getAttribute("editingFileContent"));
    }

    @Test
    public void testCreateFileSuccess()
    {
        final ResponseEntity<Map<String, Object>> response = controller.createFile("MyNewTest");

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("MyNewTest.yaml", response.getBody().get("file"));
    }

    @Test
    public void testCreateStepsFileSuccess()
    {
        final ResponseEntity<Map<String, Object>> response = controller.createFile("fragments/login.steps");

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("fragments/login.steps", response.getBody().get("file"));
    }

    @Test
    public void testGetStepsFilesSuccess()
    {
        Mockito.when(fileService.getStepsFilesList()).thenReturn(List.of("fragments/login.steps"));

        final ResponseEntity<List<String>> response = controller.getStepsFiles(null);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(List.of("fragments/login.steps"), response.getBody());
    }

    @Test
    public void testGetStepsFilesWithQuerySuccess()
    {
        Mockito.when(fileService.getFilteredStepsFilesList("login")).thenReturn(List.of("fragments/login.steps"));

        final ResponseEntity<List<String>> response = controller.getStepsFiles("login");

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(List.of("fragments/login.steps"), response.getBody());
    }

    @Test
    public void testCreateFileEmptyNameReturnsBadRequest()
    {
        final ResponseEntity<Map<String, Object>> response = controller.createFile("   ");

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody().containsKey("error"));
    }

    @Test
    public void testDeleteFileWithParamSuccess() throws Exception
    {
        Mockito.when(fileService.deleteYamlFile("test.yaml")).thenReturn(true);

        final ResponseEntity<Map<String, Object>> response = controller.deleteFile("test.yaml", null);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("SUCCESS", response.getBody().get("status"));
        Assertions.assertEquals("test.yaml", response.getBody().get("file"));
    }

    @Test
    public void testDeleteFileWithRequestParameterFallback() throws Exception
    {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("file")).thenReturn("fallback.yaml");
        Mockito.when(fileService.deleteYamlFile("fallback.yaml")).thenReturn(true);

        final ResponseEntity<Map<String, Object>> response = controller.deleteFile(null, request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("SUCCESS", response.getBody().get("status"));
        Assertions.assertEquals("fallback.yaml", response.getBody().get("file"));
    }

    @Test
    public void testDeleteFileEmpty()
    {
        final ResponseEntity<Map<String, Object>> response = controller.deleteFile(null, null);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("ERROR", response.getBody().get("status"));
    }

    @Test
    public void testGetIncludeTreeFragmentSuccess() throws Exception
    {
        final Model model = new ConcurrentModel();
        final String nestedContent = "- Nested step 1\n- _include: fragments/child.steps";
        Mockito.when(fileService.readYamlFileContent("fragments/nested.steps")).thenReturn(nestedContent);
        Mockito.when(fileService.parsePlaybookSections(nestedContent)).thenReturn(Map.of(
            "mainSteps", List.of("Nested step 1", "_include: fragments/child.steps")
        ));

        final String view = controller.getIncludeTreeFragment("fragments/nested.steps", "123", model);

        Assertions.assertEquals("fragments/editor :: includeTreeCardFragment", view);
        Assertions.assertEquals("123", model.getAttribute("cardId"));
        Assertions.assertEquals("fragments/nested.steps", model.getAttribute("includeFile"));
        Assertions.assertEquals(true, model.getAttribute("fileExists"));
        Assertions.assertEquals(List.of("Nested step 1", "_include: fragments/child.steps"), model.getAttribute("includeSteps"));
    }
}
