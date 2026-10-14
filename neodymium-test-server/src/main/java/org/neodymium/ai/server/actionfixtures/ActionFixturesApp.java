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
package org.neodymium.ai.server.actionfixtures;

import java.util.List;

import io.javalin.Javalin;

/**
 * Mounts the isolated action test fixture routes into Javalin.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class ActionFixturesApp
{
    public static final List<String> ACTION_SUITES = List.of(
        "ActionEdgeCasesTest",
        "AllActionsTest",
        "AssertActionTest",
        "BranchActionTest",
        "CheckActionTest",
        "ClickActionTest",
        "DownloadTest",
        "FailureLineTracingTest",
        "ForwardActionTest",
        "HoverActionTest",
        "IncludeTest",
        "InteractiveHudSelenideTest",
        "NestedFrameTest",
        "OrdinalChallengesTest",
        "PageAnalyzerCheckableTest",
        "PageAnalyzerTest",
        "PositionalChallengesTest",
        "ScrollActionTest",
        "SelectActionTest",
        "SelenideActionPluginsTest",
        "SelenideAddonsTest",
        "SplitMultiActionTest",
        "SplittableBranchTest",
        "StoreActionTest",
        "SwitchWindowActionTest",
        "TypeActionTest",
        "WaitActionTest"
    );

    private final ActionFixturesController controller;

    public ActionFixturesApp()
    {
        this.controller = new ActionFixturesController();
    }

    /**
     * Registers action fixture routes on the provided Javalin application.
     *
     * @param app the Javalin application
     */
    public void registerRoutes(final Javalin app)
    {
        for (final String suite : ACTION_SUITES)
        {
            app.get("/" + suite + "/{page}", controller::handleActionPage);
            app.head("/" + suite + "/{page}", controller::handleActionPage);
        }

        app.get("/action-test-pages.html", controller::handleActionIndex);
        app.head("/action-test-pages.html", controller::handleActionIndex);
    }

    public ActionFixturesController getController()
    {
        return controller;
    }
}
