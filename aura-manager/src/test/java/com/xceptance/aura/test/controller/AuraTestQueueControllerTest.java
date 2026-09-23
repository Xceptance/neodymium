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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

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
    public final void testDefaultExecutionModeIsLlmRecording()
    {
        Assertions.assertEquals("LLM_RECORDING", controller.getExecutionMode(),
                "AuraTestQueueController execution mode must default to LLM_RECORDING");
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
}
