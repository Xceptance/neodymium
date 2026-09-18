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

import com.xceptance.neodymium.aura.AuraChatSessionService;
import com.xceptance.neodymium.aura.AuraFileService;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

/**
 * Unit tests for AuraTestDashboardController.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public class AuraTestDashboardControllerTest
{
    private AuraInteractiveService interactiveService;
    private AuraFileService fileService;
    private AuraChatSessionService sessionService;
    private AuraTestQueueController queueController;
    private AuraTestDashboardController controller;

    @BeforeEach
    public void setUp()
    {
        interactiveService = Mockito.mock(AuraInteractiveService.class);
        fileService = Mockito.mock(AuraFileService.class);
        sessionService = Mockito.mock(AuraChatSessionService.class);
        queueController = Mockito.mock(AuraTestQueueController.class);

        Mockito.when(fileService.getYamlFilesList()).thenReturn(List.of());
        Mockito.when(interactiveService.getActiveTheme()).thenReturn("dark");

        controller = new AuraTestDashboardController(
            interactiveService,
            fileService,
            sessionService,
            queueController
        );
    }

    @Test
    public void testRenderDashboardFullPage()
    {
        final Model model = new ConcurrentModel();
        final String view = controller.renderDashboard("false", null, model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("AuraTestManager", model.getAttribute("activeTab"));
        Assertions.assertEquals("Neodymium Aura Manager Dashboard", model.getAttribute("pageTitle"));
        Assertions.assertEquals("fragments/aura-test-manager :: auraTestManager", model.getAttribute("viewFragment"));
    }

    @Test
    public void testRenderDashboardHtmx()
    {
        final Model model = new ConcurrentModel();
        final String view = controller.renderDashboard("true", null, model);

        Assertions.assertEquals("fragments/aura-test-manager :: auraTestManager", view);
        Assertions.assertEquals("AuraTestManager", model.getAttribute("activeTab"));
        Assertions.assertEquals("Neodymium Aura Manager Dashboard", model.getAttribute("pageTitle"));
    }

    @Test
    public void testChangeThemeReturnsHeaderFragment()
    {
        final Model model = new ConcurrentModel();
        final String view = controller.changeTheme("light", model);

        Assertions.assertEquals("fragments/header :: themeContainer", view);
        Assertions.assertEquals("light", model.getAttribute("theme"));
        Mockito.verify(interactiveService).setActiveTheme("light");
    }

    @Test
    public void testRenderDashboardPopulatesInteractiveAndRunningState()
    {
        final Model model = new ConcurrentModel();
        Mockito.when(queueController.isInteractive()).thenReturn(true);
        Mockito.when(queueController.isRunning()).thenReturn(false);

        final String view = controller.renderDashboard("true", null, model);

        Assertions.assertEquals("fragments/aura-test-manager :: auraTestManager", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("interactive"));
        Assertions.assertEquals(Boolean.FALSE, model.getAttribute("running"));
    }

    @Test
    public void testRenderDashboardPopulatesInteractiveAndRunningStateTrue()
    {
        final Model model = new ConcurrentModel();
        Mockito.when(queueController.isInteractive()).thenReturn(true);
        Mockito.when(queueController.isRunning()).thenReturn(true);

        final String view = controller.renderDashboard("false", null, model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("interactive"));
        Assertions.assertEquals(Boolean.TRUE, model.getAttribute("running"));
    }

    @Test
    public void testRenderDashboardWithFileParameter() throws Exception
    {
        final Model model = new ConcurrentModel();
        Mockito.when(fileService.readYamlFileContent("Freiwohl.yaml")).thenReturn("steps:\n  - Open url");
        Mockito.when(fileService.parsePlaybookSections("steps:\n  - Open url")).thenReturn(Map.of("mainSteps", List.of("Open url")));

        final String view = controller.renderDashboard("false", "Freiwohl.yaml", model);

        Assertions.assertEquals("index", view);
        Assertions.assertEquals("Freiwohl.yaml", model.getAttribute("activeEditingFile"));
        Assertions.assertEquals("Freiwohl.yaml", model.getAttribute("currentTestFile"));
        Assertions.assertEquals("steps:\n  - Open url", model.getAttribute("editingFileContent"));
        Mockito.verify(fileService).setActiveEditingFile("Freiwohl.yaml");
    }
}
