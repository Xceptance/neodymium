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
package com.xceptance.neodymium.aura;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Main HTTP Handler registry for Neodymium Aura Manager server routing.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class AuraManagerMainHandler implements HttpHandler
{
    private final AuraManagerRouter router;
    private final AuraQueueService queueService;
    private final AuraInteractiveService interactiveService;

    public AuraManagerMainHandler(final NeodymiumAuraManager manager)
    {
        this.router = new AuraManagerRouter();

        // Instantiate singletons of domain services
        final AuraFileService fileService = new AuraFileService();
        final AuraSettingsService settingsService = new AuraSettingsService();
        final AuraChatService chatService = new AuraChatService(fileService);
        final AuraChatSessionService sessionService = new AuraChatSessionService();
        final AuraReportingService reportingService = new AuraReportingService();
        this.interactiveService = new AuraInteractiveService();
        reportingService.setInteractiveService(this.interactiveService);
        this.queueService = new AuraQueueService(reportingService, interactiveService);

        // Instantiate standalone public controller classes injecting stateless/stateful singletons and manager
        final AuraManagerQueueController queueController = new AuraManagerQueueController(queueService, fileService, interactiveService, manager);
        final AuraManagerReportingController reportingController = new AuraManagerReportingController(reportingService, queueService, manager);
        final AuraManagerDashboardController dashboardController = new AuraManagerDashboardController(interactiveService, fileService, queueController, reportingController, sessionService, manager);
        final AuraManagerFileController fileController = new AuraManagerFileController(fileService, queueController, manager);
        final AuraManagerEditorController editorController = new AuraManagerEditorController(fileService, queueController, manager);
        final AuraManagerInteractiveController interactiveController = new AuraManagerInteractiveController(interactiveService, reportingService, queueService, manager);
        final AuraManagerChatController chatController = new AuraManagerChatController(chatService, sessionService, manager);
        final AuraManagerSettingsController settingsController = new AuraManagerSettingsController(settingsService, manager);

        // Register GET & POST mappings declaratively
        router.GET("/", dashboardController::handleDashboard);
        router.POST("/api/theme", dashboardController::handleApiTheme);

        router.GET("/api/settings", settingsController::handleGetSettingsModal);
        router.POST("/api/settings/save", settingsController::handleSaveSettings);
        router.POST("/api/settings/add-browser-profile", settingsController::handleAddBrowserProfile);
        router.POST("/api/settings/add-browser-property", settingsController::handleAddBrowserProperty);


        router.GET("/api/files", fileController::handleListFiles);
        router.GET("/api/files/list", fileController::handleListFilesPanel);
        router.GET("/api/files/search", fileController::handleSearchFiles);
        router.GET("/api/files/expand", fileController::handleToggleFileExpansion);
        router.POST("/api/files/toggle", fileController::handleToggleFileExpansion);

        router.GET("/api/editor", editorController::handleEditorPanel);
        router.GET("/api/editor/include-tree", editorController::handleGetIncludeTree);
        router.GET("/api/read", editorController::handleReadFile);
        router.POST("/api/save", editorController::handleSaveFile);
        router.POST("/api/delete", editorController::handleDeleteFile);
        router.POST("/api/create", editorController::handleCreateFile);
        router.GET("/api/modals/create", editorController::handleGetCreateModal);
        router.GET("/api/modals/delete", editorController::handleGetDeleteModal);
        router.POST("/api/editor/close", editorController::handleCloseEditor);
        router.POST("/api/editor/review-steps", editorController::handleReviewSteps);

        router.POST("/api/queue/toggle", queueController::handleToggleQueue);
        router.POST("/api/queue/toggleAll", queueController::handleToggleAllQueue);
        router.POST("/api/queue/move", queueController::handleMoveQueue);
        router.POST("/api/queue/remove", queueController::handleRemoveQueue);
        router.POST("/api/queue/clear", queueController::handleClearQueue);
        router.POST("/api/config/toggle", queueController::handleToggleConfig);

        router.POST("/api/run", queueController::handleRunQueue);
        router.GET("/api/status", queueController::handleStatusStream);
        router.GET("/api/status/panel", queueController::handleStatusPanel);
        router.POST("/api/stop", queueController::handleStopProcess);

        // Serve Static Assets & Views
        router.prefix("GET", "/dashboard-", dashboardController::handleDashboardAssets);
        router.GET("/material-symbols.css", dashboardController::handleDashboardAssets);
        router.GET("/material-symbols-outlined.woff2", dashboardController::handleDashboardAssets);
        router.GET("/interactive_console.html", interactiveController::handleHtml);
        router.GET("/interactive_console.css", interactiveController::handleCss);
        router.GET("/interactive_console.js", interactiveController::handleJs);

        // Reporting Mappings (Renamed from Allure to Reporting as requested)
        router.GET("/api/reporting/history", reportingController::handleReportingHistory);
        router.GET("/api/reporting/history-json", reportingController::handleReportingHistoryJson);
        router.GET("/api/reporting/run", reportingController::handleReportingRun);
        router.prefix("GET", "/api/reporting/report/", reportingController::handleServeReportFile);
        router.POST("/api/reporting/generate", reportingController::handleGenerateReporting);
        router.POST("/api/reporting/delete", reportingController::handleDeleteReport);

        // Chat & Interactive AI Console Actions (Kept /api/console routes for compatibility as requested)
        router.GET("/api/chat/messages", chatController::handleGetMessages);
        router.POST("/api/chat/create", chatController::handleCreateSession);
        router.POST("/api/chat/delete", chatController::handleDeleteSession);
        router.POST("/api/chat/rename", chatController::handleRenameSession);
        router.POST("/api/chat", chatController::handleChat);
        router.POST("/api/disconnect", interactiveController::handleDisconnect);
        router.prefix(null, "/api/console/events", interactiveController::handleConsoleEvents);
        router.POST("/api/console/action", interactiveController::handleConsoleAction);
        router.GET("/api/console/screenshot", interactiveController::handleConsoleScreenshot);
        router.POST("/api/console/internal/pushState", interactiveController::handlePushState);
        router.POST("/api/console/internal/broadcast", interactiveController::handleBroadcast);
        router.GET("/api/console/internal/waitForAction", interactiveController::handleWaitForAction);
    }

    public AuraQueueService getQueueService()
    {
        return queueService;
    }

    public AuraInteractiveService getInteractiveService()
    {
        return interactiveService;
    }

    @Override
    public void handle(final HttpExchange exchange) throws IOException
    {
        router.handle(exchange);
    }
}
