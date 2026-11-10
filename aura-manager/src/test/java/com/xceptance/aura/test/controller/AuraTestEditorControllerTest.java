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
import com.xceptance.neodymium.aura.AuraFileService.IssueSeverity;
import com.xceptance.neodymium.aura.AuraFileService.ValidationIssue;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neodymium.ai.playbook.linter.LinterCategory;
import org.neodymium.ai.playbook.linter.LinterSeverity;
import org.neodymium.ai.playbook.linter.PlaybookLinter;
import org.neodymium.ai.playbook.linter.PlaybookLinterFinding;
import org.neodymium.ai.playbook.linter.PlaybookLinterResult;
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
            "mainSteps", List.of("Nested step 1", "_include: fragments/child.steps"),
            "fragmentVarScopes", Map.of()
        ));

        final String view = controller.getIncludeTreeFragment("fragments/nested.steps", "123", model);

        Assertions.assertEquals("fragments/editor :: includeTreeCardFragment", view);
        Assertions.assertEquals("123", model.getAttribute("cardId"));
        Assertions.assertEquals("fragments/nested.steps", model.getAttribute("includeFile"));
        Assertions.assertEquals(true, model.getAttribute("fileExists"));
        Assertions.assertEquals(List.of("Nested step 1", "_include: fragments/child.steps"), model.getAttribute("includeSteps"));
        Assertions.assertEquals(List.of(), model.getAttribute("includeVars"));
        Assertions.assertEquals(Map.of(), model.getAttribute("includeVarScopes"));
    }

    @Test
    public void testGetIncludeTreeFragmentWithVariablesSuccess() throws Exception
    {
        final Model model = new ConcurrentModel();
        final String nestedContent = "steps:\n  - Open ${neodymium.url}\n  - Type ${username}\nvariables:\n  neodymium.url: defined\n  username: required";
        Mockito.when(fileService.readYamlFileContent("fragments/login.steps")).thenReturn(nestedContent);
        Mockito.when(fileService.parsePlaybookSections(nestedContent)).thenReturn(Map.of(
            "mainSteps", List.of("Open ${neodymium.url}", "Type ${username}"),
            "fragmentVarScopes", Map.of("neodymium.url", "defined", "username", "required")
        ));

        final String view = controller.getIncludeTreeFragment("fragments/login.steps", "card_99", model);

        Assertions.assertEquals("fragments/editor :: includeTreeCardFragment", view);
        Assertions.assertEquals("card_99", model.getAttribute("cardId"));
        Assertions.assertEquals("fragments/login.steps", model.getAttribute("includeFile"));
        Assertions.assertEquals(true, model.getAttribute("fileExists"));
        Assertions.assertEquals(List.of("neodymium.url", "username"), model.getAttribute("includeVars"));
        Assertions.assertEquals(Map.of("neodymium.url", "defined", "username", "required"), model.getAttribute("includeVarScopes"));
    }

    @Test
    public void testRenameFileSuccess() throws Exception
    {
        Mockito.when(fileService.renameOrMoveFile("old.yaml", "new.yaml")).thenReturn(true);
        Mockito.when(fileService.getActiveEditingFile()).thenReturn("new.yaml");

        final ResponseEntity<Map<String, Object>> response = controller.renameFile("old.yaml", "new.yaml", null);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("SUCCESS", response.getBody().get("status"));
        Assertions.assertEquals("old.yaml", response.getBody().get("oldFile"));
        Assertions.assertEquals("new.yaml", response.getBody().get("newFile"));
    }

    @Test
    public void testRenameFileError() throws Exception
    {
        Mockito.when(fileService.renameOrMoveFile("old.yaml", "exists.yaml")).thenThrow(new IllegalArgumentException("Destination file already exists: exists.yaml"));

        final ResponseEntity<Map<String, Object>> response = controller.renameFile("old.yaml", "exists.yaml", null);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("ERROR", response.getBody().get("status"));
        Assertions.assertEquals("Destination file already exists: exists.yaml", response.getBody().get("error"));
    }

    @Test
    public final void testGetEditorFragmentPopulatesPropertiesAndMetaYaml() throws Exception
    {
        final Model model = new ConcurrentModel();
        final String sampleYaml = "_properties:\n  timeout: 5000\nsteps:\n  - Open https://example.com";
        Mockito.when(fileService.readYamlFileContent("props.yaml")).thenReturn(sampleYaml);
        Mockito.when(fileService.parsePlaybookSections(sampleYaml)).thenReturn(Map.of(
            "beforeSteps", List.of(),
            "mainSteps", List.of("Open https://example.com"),
            "afterSteps", List.of(),
            "dataMatrix", List.of(),
            "varKeys", List.of(),
            "propertiesYaml", "timeout: 5000",
            "propertiesList", List.of(Map.of("key", "timeout", "value", "5000")),
            "metaYaml", "author: Antigravity",
            "description", "My test description"
        ));
        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of());

        final String view = controller.getEditorFragment("props.yaml", null, model);

        Assertions.assertEquals("fragments/editor :: editorPanelContent", view);
        Assertions.assertEquals("props.yaml", model.getAttribute("activeEditingFile"));
        Assertions.assertEquals("timeout: 5000", model.getAttribute("propertiesYaml"));
        Assertions.assertEquals(List.of(Map.of("key", "timeout", "value", "5000")), model.getAttribute("propertiesList"));
        Assertions.assertEquals("author: Antigravity", model.getAttribute("metaYaml"));
        Assertions.assertEquals("My test description", model.getAttribute("description"));
    }

    @Test
    public final void testPostEditorFragmentWithCustomContent() throws Exception
    {
        final Model model = new ConcurrentModel();
        final String customYaml = "_properties:\n  retries: 3\nsteps:\n  - Click button";
        Mockito.when(fileService.parsePlaybookSections(customYaml)).thenReturn(Map.of(
            "beforeSteps", List.of(),
            "mainSteps", List.of("Click button"),
            "afterSteps", List.of(),
            "dataMatrix", List.of(),
            "varKeys", List.of(),
            "propertiesYaml", "retries: 3",
            "propertiesList", List.of(Map.of("key", "retries", "value", "3"))
        ));
        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of());

        final String view = controller.getEditorFragment("custom.yaml", customYaml, model);

        Assertions.assertEquals("fragments/editor :: editorPanelContent", view);
        Assertions.assertEquals("custom.yaml", model.getAttribute("activeEditingFile"));
        Assertions.assertEquals(customYaml, model.getAttribute("editingFileContent"));
        Assertions.assertEquals("retries: 3", model.getAttribute("propertiesYaml"));
        Assertions.assertEquals(List.of(Map.of("key", "retries", "value", "3")), model.getAttribute("propertiesList"));
        Mockito.verify(fileService, Mockito.never()).readYamlFileContent(Mockito.anyString());
    }

    @Test
    public final void testAiReviewHaltsOnSyntaxError() throws Exception
    {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("file")).thenReturn("broken.yaml");
        Mockito.when(request.getParameter("content")).thenReturn("steps:\n  - Invalid: [syntax error");

        final PlaybookLinter mockLinter = Mockito.mock(PlaybookLinter.class);
        final AuraTestEditorController customController = new AuraTestEditorController(fileService, mockLinter);

        Mockito.when(fileService.validateFileContent("broken.yaml", "steps:\n  - Invalid: [syntax error"))
                .thenReturn(List.of(new ValidationIssue(IssueSeverity.ERROR, "Mapping values are not allowed here", "broken.yaml")));

        final ResponseEntity<Map<String, Object>> response = customController.aiReview(request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("SYNTAX_ERROR", response.getBody().get("status"));
        Assertions.assertTrue(response.getBody().containsKey("issues"));
        Mockito.verify(mockLinter, Mockito.never()).lintWithResult(Mockito.anyList(), Mockito.any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public final void testAiReviewSuccessfulWithFindings() throws Exception
    {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final String validYaml = "steps:\n  - Click button";
        Mockito.when(request.getParameter("file")).thenReturn("valid.yaml");
        Mockito.when(request.getParameter("content")).thenReturn(validYaml);

        final PlaybookLinter mockLinter = Mockito.mock(PlaybookLinter.class);
        final AuraTestEditorController customController = new AuraTestEditorController(fileService, mockLinter);

        Mockito.when(fileService.validateFileContent("valid.yaml", validYaml)).thenReturn(List.of());

        final PlaybookLinterFinding finding = new PlaybookLinterFinding(
                1,
                2,
                "valid.yaml",
                "Click button",
                "Click button",
                LinterCategory.VAGUE_TARGET,
                LinterSeverity.WARNING,
                "Button target is ambiguous",
                "Click the primary 'Submit' button",
                null
        );

        Mockito.when(mockLinter.lintWithResult(Mockito.anyList(), Mockito.any())).thenReturn(PlaybookLinterResult.success(List.of(finding)));

        final ResponseEntity<Map<String, Object>> response = customController.aiReview(request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("OK", response.getBody().get("status"));
        final List<Map<String, Object>> findings = (List<Map<String, Object>>) response.getBody().get("findings");
        Assertions.assertNotNull(findings);
        Assertions.assertEquals(1, findings.size());
        final Map<String, Object> f0 = findings.get(0);
        Assertions.assertEquals(1, f0.get("stepIndex"));
        Assertions.assertEquals("VAGUE_TARGET", f0.get("category"));
        Assertions.assertEquals("WARNING", f0.get("severity"));
        Assertions.assertEquals("Button target is ambiguous", f0.get("message"));
        Assertions.assertEquals("Click the primary 'Submit' button", f0.get("suggestedRewrite"));
        Assertions.assertEquals("Click button", f0.get("rawInstruction"));
    }

    @Test
    public final void testAiReviewWithEmptyContent() throws Exception
    {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getParameter("file")).thenReturn("empty.yaml");
        Mockito.when(request.getParameter("content")).thenReturn("");

        final PlaybookLinter mockLinter = Mockito.mock(PlaybookLinter.class);
        final AuraTestEditorController customController = new AuraTestEditorController(fileService, mockLinter);

        Mockito.when(fileService.validateFileContent("empty.yaml", "")).thenReturn(List.of());

        final ResponseEntity<Map<String, Object>> response = customController.aiReview(request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("OK", response.getBody().get("status"));
        Assertions.assertEquals(List.of(), response.getBody().get("findings"));
        Mockito.verify(mockLinter, Mockito.never()).lintWithResult(Mockito.anyList(), Mockito.any());
    }

    @Test
    public final void testAiReviewWhenLinterReturnsFailure() throws Exception
    {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final String validYaml = "steps:\n  - Click button";
        Mockito.when(request.getParameter("file")).thenReturn("valid.yaml");
        Mockito.when(request.getParameter("content")).thenReturn(validYaml);

        final PlaybookLinter mockLinter = Mockito.mock(PlaybookLinter.class);
        final AuraTestEditorController customController = new AuraTestEditorController(fileService, mockLinter);

        Mockito.when(fileService.validateFileContent("valid.yaml", validYaml)).thenReturn(List.of());
        Mockito.when(mockLinter.lintWithResult(Mockito.anyList(), Mockito.any())).thenReturn(
                PlaybookLinterResult.failure("No LLM provider registered for capability LINTER"));

        final ResponseEntity<Map<String, Object>> response = customController.aiReview(request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("ERROR", response.getBody().get("status"));
        Assertions.assertTrue(String.valueOf(response.getBody().get("error")).contains("No LLM provider registered"));
    }

    @Test
    public final void testAiReviewHandlesExceptionGracefully() throws Exception
    {
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final String validYaml = "steps:\n  - Click button";
        Mockito.when(request.getParameter("file")).thenReturn("valid.yaml");
        Mockito.when(request.getParameter("content")).thenReturn(validYaml);

        final PlaybookLinter mockLinter = Mockito.mock(PlaybookLinter.class);
        final AuraTestEditorController customController = new AuraTestEditorController(fileService, mockLinter);

        Mockito.when(fileService.validateFileContent("valid.yaml", validYaml)).thenReturn(List.of());
        Mockito.when(mockLinter.lintWithResult(Mockito.anyList(), Mockito.any())).thenThrow(new RuntimeException("LLM Quota Exceeded"));

        final ResponseEntity<Map<String, Object>> response = customController.aiReview(request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("ERROR", response.getBody().get("status"));
        Assertions.assertTrue(String.valueOf(response.getBody().get("error")).contains("LLM Quota Exceeded"));
    }
}
