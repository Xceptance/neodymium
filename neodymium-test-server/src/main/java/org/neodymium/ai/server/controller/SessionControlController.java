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
package org.neodymium.ai.server.controller;

import java.util.Map;

import org.neodymium.ai.server.session.SessionManager;
import org.neodymium.ai.server.session.TestSession;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Controller handling REST management endpoints for test session lifecycle,
 * state resets, and health inspections.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class SessionControlController
{
    private final SessionManager sessionManager;

    public SessionControlController(final SessionManager sessionManager)
    {
        this.sessionManager = sessionManager != null ? sessionManager : new SessionManager();
    }

    public void handleFullReset(final Context ctx)
    {
        final String id = ctx.pathParam("id");
        final TestSession session = sessionManager.getOrCreateSession(id);
        session.resetAll();
        ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "status", "RESET_COMPLETE"));
    }

    public void handleInventoryReset(final Context ctx)
    {
        final String id = ctx.pathParam("id");
        final TestSession session = sessionManager.getOrCreateSession(id);
        session.resetInventory();
        ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "status", "INVENTORY_RESET"));
    }

    public void handleUsersReset(final Context ctx)
    {
        final String id = ctx.pathParam("id");
        final TestSession session = sessionManager.getOrCreateSession(id);
        session.resetUsers();
        ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "status", "USERS_RESET"));
    }

    public void handleCartsReset(final Context ctx)
    {
        final String id = ctx.pathParam("id");
        final TestSession session = sessionManager.getOrCreateSession(id);
        session.resetCarts();
        ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "status", "CARTS_RESET"));
    }

    public void handleOrdersReset(final Context ctx)
    {
        final String id = ctx.pathParam("id");
        final TestSession session = sessionManager.getOrCreateSession(id);
        session.resetOrders();
        ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "status", "ORDERS_RESET"));
    }

    public void handleGetStatus(final Context ctx)
    {
        final String id = ctx.pathParam("id");
        final TestSession session = sessionManager.getSession(id);
        if (session == null)
        {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Session not found", "sessionId", id));
            return;
        }

        ctx.status(HttpStatus.OK).json(Map.of(
            "sessionId", session.getSessionId(),
            "createdAt", session.getCreatedAt(),
            "lastAccessedAt", session.getLastAccessedAt(),
            "userCount", session.getUserCount(),
            "cartCount", session.getCartCount(),
            "orderCount", session.getOrderCount(),
            "sessionCount", session.getSessionCount(),
            "auraGlanceRequests", session.getAuraGlanceState().getRequestCount()
        ));
    }

    public void handleDeleteSession(final Context ctx)
    {
        final String id = ctx.pathParam("id");
        final boolean removed = sessionManager.removeSession(id);
        if (removed)
        {
            ctx.status(HttpStatus.OK).json(Map.of("sessionId", id, "status", "DELETED"));
        }
        else
        {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("sessionId", id, "status", "NOT_FOUND_OR_DEFAULT"));
        }
    }

    public void handleGlobalReset(final Context ctx)
    {
        sessionManager.resetAllSessions();
        ctx.status(HttpStatus.OK).json(Map.of(
            "status", "GLOBAL_RESET_COMPLETE",
            "activeSessions", sessionManager.getActiveSessionCount()
        ));
    }

    public void handleHealth(final Context ctx)
    {
        ctx.status(HttpStatus.OK).json(Map.of("status", "UP"));
    }
}
