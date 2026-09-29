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

import com.xceptance.neodymium.aura.AuraQueueService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller advice providing global model attributes for active run context across all Aura Manager views.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@ControllerAdvice(basePackages = "com.xceptance.aura")
public class AuraGlobalModelAdvice
{
    private final AuraQueueService queueService;

    public AuraGlobalModelAdvice(final AuraQueueService queueService)
    {
        this.queueService = queueService;
    }

    @ModelAttribute
    public void addGlobalAttributes(final Model model)
    {
        if (queueService != null && model != null)
        {
            final String activeRunId = queueService.getCurrentRunId();
            final boolean running = queueService.isRunningQueue();
            if (activeRunId != null && !activeRunId.isEmpty())
            {
                model.addAttribute("activeRunId", activeRunId);
                model.addAttribute("activeRunStatus", running ? "running" : "finished");
                model.addAttribute("activeRunReportUrl", "/run-report?runId=" + activeRunId);
                model.addAttribute("hasRunContext", true);
            }
            else
            {
                model.addAttribute("activeRunId", "");
                model.addAttribute("activeRunStatus", "");
                model.addAttribute("activeRunReportUrl", "");
                model.addAttribute("hasRunContext", false);
            }
            model.addAttribute("running", running);
            model.addAttribute("hasParseError", false);
            model.addAttribute("parseErrorMessage", "");
            model.addAttribute("editorMode", "visual");
        }
    }
}
