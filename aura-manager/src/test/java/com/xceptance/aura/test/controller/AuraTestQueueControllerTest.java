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
import com.xceptance.neodymium.aura.dto.DatasetDto;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import com.xceptance.neodymium.aura.dto.YamlFileDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.neodymium.ai.config.ExecutionMode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Unit tests for AuraTestQueueController default browser and execution mode behavior.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraTestQueueControllerTest
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
    public final void testDefaultExecutionModeIsAuto()
    {
        Assertions.assertEquals("AUTO", controller.getExecutionMode(),
                "AuraTestQueueController execution mode must default to AUTO");
    }

    @Test
    public final void testSetExecutionModeViaApiConfigMode()
    {
        for (final ExecutionMode mode : ExecutionMode.values())
        {
            final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
            Mockito.when(req.getParameter("mode")).thenReturn(mode.name());
            Mockito.when(req.getParameterNames()).thenReturn(Collections.enumeration(List.of("mode")));

            final Model model = new ConcurrentModel();
            final String view = controller.setConfigMode(req, model);

            Assertions.assertEquals("fragments/queue :: configPanel", view);
            Assertions.assertEquals(mode.name(), controller.getExecutionMode());
            Assertions.assertEquals(mode.name(), model.getAttribute("executionMode"));
        }
    }

    @Test
    public final void testSetInvalidExecutionModeIsRejected()
    {
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameter("mode")).thenReturn("INVALID_UNKNOWN_MODE");
        Mockito.when(req.getParameterNames()).thenReturn(Collections.enumeration(List.of("mode")));

        final Model model = new ConcurrentModel();
        controller.setConfigMode(req, model);

        Assertions.assertEquals("AUTO", controller.getExecutionMode(),
                "Invalid execution mode should be rejected, preserving existing mode");
    }

    @Test
    public final void testToggleConfigExecutionMode()
    {
        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameter("key")).thenReturn("mode");
        Mockito.when(req.getParameter("mode")).thenReturn("REPLAY_WITH_HEALING");
        Mockito.when(req.getParameterNames()).thenReturn(Collections.enumeration(List.of("key", "mode")));

        final Model model = new ConcurrentModel();
        controller.toggleConfig(req, model);

        Assertions.assertEquals("REPLAY_WITH_HEALING", controller.getExecutionMode());
    }

    @Test
    public final void testQueueTemplateContainsAllSevenExecutionModes()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final Context context = new Context();
        context.setVariable("executionMode", "AUTO");
        context.setVariable("headless", true);
        context.setVariable("video", false);
        context.setVariable("interactive", false);
        context.setVariable("queue", List.of());
        context.setVariable("totalRuns", 0);
        context.setVariable("running", false);
        context.setVariable("activeEditingFile", null);

        final String html = engine.process("fragments/queue", Collections.singleton("configPanel"), context);
        Assertions.assertNotNull(html);
        for (final ExecutionMode mode : ExecutionMode.values())
        {
            Assertions.assertTrue(html.contains("value=\"" + mode.name() + "\""),
                    "queue.html must contain option value for " + mode.name());
        }
    }

    @Test
    public final void testDefaultBrowserSelectionIsChromeAndAvailable()
    {
        final Set<String> globalProfiles = controller.getGlobalBrowserProfiles();
        Assertions.assertNotNull(globalProfiles);
        Assertions.assertFalse(globalProfiles.isEmpty(), "Global browser profiles should not be empty");

        final String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac"))
        {
            Assertions.assertFalse(globalProfiles.contains("Safari_1920x1080"),
                    "Safari should never be selected by default on non-macOS platforms");
        }

        final String defaultProfile = globalProfiles.iterator().next().toLowerCase();
        Assertions.assertTrue(defaultProfile.contains("chrome"),
                "Default selected profile should be a Chrome profile, but got: " + defaultProfile);
    }

    @Test
    public final void testSetGlobalBrowserProfilesFiltersUnavailable()
    {
        final String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac"))
        {
            controller.setGlobalBrowserProfiles(List.of("Safari_1920x1080"));
            Assertions.assertFalse(controller.getGlobalBrowserProfiles().contains("Safari_1920x1080"),
                    "Unavailable Safari profile should be rejected in setGlobalBrowserProfiles");
        }
    }

    @Test
    public final void testToggleUnavailableBrowserProfileIsRejected()
    {
        final String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("mac"))
        {
            final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
            Mockito.when(req.getParameter("profile")).thenReturn("Safari_1920x1080");
            Mockito.when(req.getParameter("action")).thenReturn("add");
            Mockito.when(req.getParameterNames()).thenReturn(Collections.enumeration(List.of("profile", "action")));

            final Model model = new ConcurrentModel();
            controller.toggleBrowserProfile(req, model);

            Assertions.assertFalse(controller.getGlobalBrowserProfiles().contains("Safari_1920x1080"),
                    "Toggle action=add on unavailable profile must be rejected");
        }
    }

    @Test
    public final void testRemoveFromQueueRemovesOnlyTargetItemAndPopulatesModel()
    {
        final HttpServletRequest addReq1 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(addReq1.getParameter("file")).thenReturn("Test1.yaml");
        Mockito.when(addReq1.getParameter("id")).thenReturn("ds1");
        Mockito.when(addReq1.getParameterNames()).thenReturn(Collections.enumeration(List.of("file", "id")));

        final HttpServletRequest addReq2 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(addReq2.getParameter("file")).thenReturn("Test2.yaml");
        Mockito.when(addReq2.getParameter("id")).thenReturn("ds2");
        Mockito.when(addReq2.getParameterNames()).thenReturn(Collections.enumeration(List.of("file", "id")));

        final Model model = new ConcurrentModel();
        controller.toggleQueue(addReq1, model);
        controller.toggleQueue(addReq2, model);

        Assertions.assertEquals(2, controller.getSelectedQueue().size());

        final HttpServletRequest removeReq = Mockito.mock(HttpServletRequest.class);
        Mockito.when(removeReq.getParameter("index")).thenReturn("0");
        Mockito.when(removeReq.getParameterNames()).thenReturn(Collections.enumeration(List.of("index")));

        final Model removeModel = new ConcurrentModel();
        final String viewName = controller.removeFromQueue(removeReq, removeModel);

        Assertions.assertEquals("fragments/queue :: queueListContainerContent", viewName);
        Assertions.assertEquals(1, controller.getSelectedQueue().size());
        Assertions.assertEquals("Test2.yaml", controller.getSelectedQueue().get(0).file);

        Assertions.assertTrue(removeModel.containsAttribute("queue"));
        Assertions.assertEquals(controller.getSelectedQueue(), removeModel.getAttribute("queue"));
    }

    @Test
    public final void testMoveQueuePopulatesModel()
    {
        final HttpServletRequest addReq1 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(addReq1.getParameter("file")).thenReturn("Test1.yaml");
        Mockito.when(addReq1.getParameter("id")).thenReturn("ds1");
        Mockito.when(addReq1.getParameterNames()).thenReturn(Collections.enumeration(List.of("file", "id")));

        final HttpServletRequest addReq2 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(addReq2.getParameter("file")).thenReturn("Test2.yaml");
        Mockito.when(addReq2.getParameter("id")).thenReturn("ds2");
        Mockito.when(addReq2.getParameterNames()).thenReturn(Collections.enumeration(List.of("file", "id")));

        final Model model = new ConcurrentModel();
        controller.toggleQueue(addReq1, model);
        controller.toggleQueue(addReq2, model);

        final HttpServletRequest moveReq = Mockito.mock(HttpServletRequest.class);
        Mockito.when(moveReq.getParameter("index")).thenReturn("0");
        Mockito.when(moveReq.getParameter("direction")).thenReturn("down");
        Mockito.when(moveReq.getParameterNames()).thenReturn(Collections.enumeration(List.of("index", "direction")));

        final Model moveModel = new ConcurrentModel();
        final String viewName = controller.moveQueue(moveReq, moveModel);

        Assertions.assertEquals("fragments/queue :: queueListContainerContent", viewName);
        Assertions.assertEquals("Test2.yaml", controller.getSelectedQueue().get(0).file);
        Assertions.assertTrue(moveModel.containsAttribute("queue"));
        Assertions.assertEquals(controller.getSelectedQueue(), moveModel.getAttribute("queue"));
    }

    @Test
    public final void testRenderPlaybookSidePanelPopulatesModel()
    {
        final Map<String, Object> details = new HashMap<>();
        details.put("file", "verla/SearchTest.yaml");
        details.put("datasetId", "normal");
        details.put("totalSteps", 3);
        details.put("steps", List.of());

        Mockito.when(this.fileService.loadPlaybookDetails("verla/SearchTest.yaml", "normal")).thenReturn(details);
        Mockito.when(this.fileService.hasPlaybook("verla/SearchTest.yaml", "normal")).thenReturn(true);

        Assertions.assertTrue(this.controller.hasPlaybook("verla/SearchTest.yaml", "normal"));

        final Model model = new ConcurrentModel();
        final String viewName = this.controller.renderPlaybookSidePanel("verla/SearchTest.yaml", "normal", model);

        Assertions.assertEquals("fragments/side-panel-playbook :: playbookSidePanel", viewName);
        Assertions.assertTrue(model.containsAttribute("playbook"));
        Assertions.assertEquals(details, model.getAttribute("playbook"));
        Assertions.assertEquals("verla/SearchTest.yaml", model.getAttribute("file"));
        Assertions.assertEquals("normal", model.getAttribute("datasetId"));
    }

    @Test
    public final void testAcceptHealingInPlaybook()
    {
        final Map<String, Object> details = new HashMap<>();
        details.put("file", "verla/SearchTest.yaml");
        details.put("datasetId", "normal");
        details.put("totalSteps", 1);
        details.put("healedSteps", 0);

        Mockito.when(this.fileService.acceptPlaybookHealing("verla/SearchTest.yaml", "normal", 0, null)).thenReturn(true);
        Mockito.when(this.fileService.loadPlaybookDetails("verla/SearchTest.yaml", "normal")).thenReturn(details);

        final Model model = new ConcurrentModel();
        final String viewName = this.controller.acceptHealing("verla/SearchTest.yaml", "normal", 0, null, model);

        Mockito.verify(this.fileService).acceptPlaybookHealing("verla/SearchTest.yaml", "normal", 0, null);
        Mockito.verify(this.fileService).loadPlaybookDetails("verla/SearchTest.yaml", "normal");

        Assertions.assertEquals("fragments/side-panel-playbook :: playbookSidePanel", viewName);
        Assertions.assertTrue(model.containsAttribute("playbook"));
        Assertions.assertEquals(details, model.getAttribute("playbook"));
    }

    @Test
    public final void testSidePanelPlaybookTemplateRendersWithoutHealedOrFailedProperties()
    {
        final SpringTemplateEngine engine = new SpringTemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);

        final Map<String, Object> step = new LinkedHashMap<>();
        step.put("instruction", "Click button");
        step.put("status", "SUCCESS");
        // deliberately omit "healed" and "failed" to simulate raw unhealed JSON map deserialization

        final Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "CLICK");
        action.put("target", "#myBtn");
        step.put("actions", List.of(action));

        final Map<String, Object> playbook = new HashMap<>();
        playbook.put("playbookPath", "path/to/pb.json");
        playbook.put("totalSteps", 1);
        playbook.put("passedSteps", 1);
        playbook.put("healedSteps", 0);
        playbook.put("failedSteps", 0);
        playbook.put("steps", List.of(step));

        final Context context = new Context();
        context.setVariable("playbook", playbook);
        context.setVariable("file", "test.yaml");
        context.setVariable("datasetId", "default");

        final String html = engine.process("fragments/side-panel-playbook", Collections.singleton("playbookSidePanel"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("Click button"));
        Assertions.assertTrue(html.contains("#myBtn"));
    }

    @Test
    public final void testToggleFolderSelectsAllDescendants()
    {
        final DatasetDto d1 = new DatasetDto("admin", "Admin", false);
        final DatasetDto d2 = new DatasetDto("user", "User", false);
        final YamlFileDto file1 = new YamlFileDto("smoke/LoginTest.yaml", List.of(d1, d2));

        final DatasetDto d3 = new DatasetDto("default", "Default", false);
        final YamlFileDto file2 = new YamlFileDto("smoke/sub/CheckTest.yaml", List.of(d3));

        final DatasetDto d4 = new DatasetDto("item1", "Item 1", false);
        final YamlFileDto otherFile = new YamlFileDto("checkout/CartTest.yaml", List.of(d4));

        Mockito.when(this.fileService.getYamlFilesList()).thenReturn(List.of(file1, file2, otherFile));

        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameter("folder")).thenReturn("smoke");
        Mockito.when(req.getParameterNames()).thenReturn(Collections.enumeration(List.of("folder")));

        final Model model = new ConcurrentModel();
        final String viewName = this.controller.toggleFolderQueue(req, model);

        Assertions.assertEquals("fragments/queue :: queueListContainerContent", viewName);
        final List<DatasetSelection> queue = this.controller.getSelectedQueue();
        Assertions.assertEquals(3, queue.size(), "All datasets in smoke folder and subfolders must be selected");
        Assertions.assertTrue(queue.stream().anyMatch(s -> "smoke/LoginTest.yaml".equals(s.file) && "admin".equals(s.id)));
        Assertions.assertTrue(queue.stream().anyMatch(s -> "smoke/LoginTest.yaml".equals(s.file) && "user".equals(s.id)));
        Assertions.assertTrue(queue.stream().anyMatch(s -> "smoke/sub/CheckTest.yaml".equals(s.file) && "default".equals(s.id)));
        Assertions.assertFalse(queue.stream().anyMatch(s -> "checkout/CartTest.yaml".equals(s.file)));
    }

    @Test
    public final void testToggleFolderDeselectsAllWhenAlreadySelected()
    {
        final DatasetDto d1 = new DatasetDto("admin", "Admin", false);
        final YamlFileDto file1 = new YamlFileDto("smoke/LoginTest.yaml", List.of(d1));

        Mockito.when(this.fileService.getYamlFilesList()).thenReturn(List.of(file1));

        final HttpServletRequest req1 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req1.getParameter("folder")).thenReturn("smoke");
        Mockito.when(req1.getParameterNames()).thenReturn(Collections.enumeration(List.of("folder")));

        final Model model1 = new ConcurrentModel();
        this.controller.toggleFolderQueue(req1, model1);
        Assertions.assertEquals(1, this.controller.getSelectedQueue().size());

        final HttpServletRequest req2 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req2.getParameter("folder")).thenReturn("smoke");
        Mockito.when(req2.getParameterNames()).thenReturn(Collections.enumeration(List.of("folder")));

        final Model model2 = new ConcurrentModel();
        this.controller.toggleFolderQueue(req2, model2);
        Assertions.assertTrue(this.controller.getSelectedQueue().isEmpty(), "Queue should be empty after toggling off");
    }

    @Test
    public final void testToggleFolderExplicitCheckedParam()
    {
        final DatasetDto d1 = new DatasetDto("admin", "Admin", false);
        final YamlFileDto file1 = new YamlFileDto("smoke/LoginTest.yaml", List.of(d1));

        Mockito.when(this.fileService.getYamlFilesList()).thenReturn(List.of(file1));

        final HttpServletRequest deselectReq = Mockito.mock(HttpServletRequest.class);
        Mockito.when(deselectReq.getParameter("folder")).thenReturn("smoke");
        Mockito.when(deselectReq.getParameter("checked")).thenReturn("false");
        Mockito.when(deselectReq.getParameterNames()).thenReturn(Collections.enumeration(List.of("folder", "checked")));

        final Model model1 = new ConcurrentModel();
        this.controller.toggleFolderQueue(deselectReq, model1);
        Assertions.assertTrue(this.controller.getSelectedQueue().isEmpty());

        final HttpServletRequest selectReq = Mockito.mock(HttpServletRequest.class);
        Mockito.when(selectReq.getParameter("folder")).thenReturn("smoke");
        Mockito.when(selectReq.getParameter("checked")).thenReturn("true");
        Mockito.when(selectReq.getParameterNames()).thenReturn(Collections.enumeration(List.of("folder", "checked")));

        final Model model2 = new ConcurrentModel();
        this.controller.toggleFolderQueue(selectReq, model2);
        Assertions.assertEquals(1, this.controller.getSelectedQueue().size());
    }

    @Test
    public final void testToggleFolderIgnoresErrorFiles()
    {
        final DatasetDto d1 = new DatasetDto("d1", "Dataset 1", false);
        final YamlFileDto validFile = new YamlFileDto("smoke/ValidTest.yaml", List.of(d1));
        final YamlFileDto errorFile = new YamlFileDto("smoke/BrokenTest.yaml", List.of(), true, "Syntax error");

        Mockito.when(this.fileService.getYamlFilesList()).thenReturn(List.of(validFile, errorFile));

        final HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getParameter("folder")).thenReturn("smoke");
        Mockito.when(req.getParameterNames()).thenReturn(Collections.enumeration(List.of("folder")));

        final Model model = new ConcurrentModel();
        this.controller.toggleFolderQueue(req, model);

        final List<DatasetSelection> queue = this.controller.getSelectedQueue();
        Assertions.assertEquals(1, queue.size());
        Assertions.assertEquals("smoke/ValidTest.yaml", queue.get(0).file);
    }
}
