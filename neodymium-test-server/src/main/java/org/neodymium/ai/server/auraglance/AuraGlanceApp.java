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
package org.neodymium.ai.server.auraglance;

import java.util.Map;

import org.neodymium.ai.server.session.SessionManager;
import org.neodymium.ai.server.session.TestSession;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;

/**
 * Mounts the Aura Glance Test Suite Hub, sandboxes, and simulation control endpoints into Javalin.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class AuraGlanceApp
{
    private final AuraGlanceController controller;
    private final SessionManager sessionManager;

    public AuraGlanceApp(final SessionManager sessionManager)
    {
        this.sessionManager = sessionManager != null ? sessionManager : new SessionManager();
        this.controller = new AuraGlanceController(this.sessionManager);
    }

    /**
     * Registers Aura Glance routes on the provided Javalin instance.
     *
     * @param app the Javalin application
     */
    public void registerRoutes(final Javalin app)
    {
        // Root index redirects to Aura Glance Starter Hub Portal
        app.get("/", ctx -> ctx.redirect("/AuraGlanceTest/index.html", HttpStatus.FOUND));
        app.get("/index.html", ctx -> ctx.redirect("/AuraGlanceTest/index.html", HttpStatus.FOUND));

        // Protocol Introspection simulation endpoints
        app.get("/AuraGlanceTest/api/faulty-backend", ctx ->
        {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .contentType(ContentType.APPLICATION_JSON)
               .result("{\"error\":\"Internal server fault\"}");
        });
        app.post("/AuraGlanceTest/api/faulty-backend", ctx ->
        {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .contentType(ContentType.APPLICATION_JSON)
               .result("{\"error\":\"Internal server fault\"}");
        });
        app.get("/AuraGlanceTest/api/missing-endpoint", ctx ->
        {
            ctx.status(HttpStatus.NOT_FOUND)
               .contentType(ContentType.APPLICATION_JSON)
               .result("{\"error\":\"Resource missing\"}");
        });
        app.post("/AuraGlanceTest/api/missing-endpoint", ctx ->
        {
            ctx.status(HttpStatus.NOT_FOUND)
               .contentType(ContentType.APPLICATION_JSON)
               .result("{\"error\":\"Resource missing\"}");
        });
        app.get("/AuraGlanceTest/api/analytics", ctx ->
        {
            ctx.status(HttpStatus.NOT_FOUND)
               .contentType(ContentType.APPLICATION_JSON)
               .result("{\"error\":\"Ignored telemetry\"}");
        });
        app.post("/AuraGlanceTest/api/analytics", ctx ->
        {
            ctx.status(HttpStatus.NOT_FOUND)
               .contentType(ContentType.APPLICATION_JSON)
               .result("{\"error\":\"Ignored telemetry\"}");
        });

        // Aura Glance hub and sandbox routes
        app.get("/AuraGlanceTest", controller::handleGet);
        app.get("/AuraGlanceTest/*", controller::handleGet);
        app.head("/AuraGlanceTest", controller::handleGet);
        app.head("/AuraGlanceTest/*", controller::handleGet);

        // Simulation Control REST Endpoints
        app.post("/api/test/sessions/{id}/auraglance/delay", ctx ->
        {
            final String id = ctx.pathParam("id");
            final TestSession session = sessionManager.getOrCreateSession(id);
            final int delayMs = ctx.queryParamAsClass("delayMs", Integer.class).getOrDefault(0);
            session.getAuraGlanceState().setArtificialDelayMs(delayMs);
            ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "delayMs", delayMs));
        });

        app.post("/api/test/sessions/{id}/auraglance/failure", ctx ->
        {
            final String id = ctx.pathParam("id");
            final TestSession session = sessionManager.getOrCreateSession(id);
            final boolean enabled = ctx.queryParamAsClass("enabled", Boolean.class).getOrDefault(true);
            session.getAuraGlanceState().setSimulateServerFailure(enabled);
            ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "simulateServerFailure", enabled));
        });

        app.post("/api/test/sessions/{id}/auraglance/reset", ctx ->
        {
            final String id = ctx.pathParam("id");
            final TestSession session = sessionManager.getOrCreateSession(id);
            session.getAuraGlanceState().reset();
            ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "status", "AuraGlanceState reset"));
        });
    }

    public AuraGlanceController getController()
    {
        return controller;
    }
}
