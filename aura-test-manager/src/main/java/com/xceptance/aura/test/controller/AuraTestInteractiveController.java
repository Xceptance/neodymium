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

import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.aura.AuraInteractiveService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for interactive console engine management.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@RestController
@RequestMapping("/api/interactive")
public class AuraTestInteractiveController
{
    private final AuraInteractiveService interactiveService;

    public AuraTestInteractiveController(final AuraInteractiveService interactiveService)
    {
        this.interactiveService = interactiveService;
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveEngines()
    {
        final Map<String, Object> resp = new HashMap<>();
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        resp.put("activeCount", engine != null ? 1 : 0);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopEngine(@RequestParam(value = "sessionId", required = false) final String sessionId)
    {
        final InteractiveConsoleEngine engine = interactiveService.getCurrentConsoleEngine();
        if (engine != null)
        {
            engine.abort();
        }
        final Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        return ResponseEntity.ok(resp);
    }
}
