/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.util.layer.desktop;

import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.util.layer.AiPromptFacade;

/**
 * Desktop-specific implementation of {@link AiPromptFacade}.
 * Exposes coordinate-based desktop interaction prompts to the AI agent.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class DesktopAiPromptFacade implements AiPromptFacade
{
    @Override
    public String getAiRolePrompt()
    {
        return "You are an AI desktop automation agent. Your job is to translate natural language instructions into concrete desktop actions using OS-level commands and java.awt.Robot. You have full capability to execute OS-level commands and control native desktop applications.";
    }

    @Override
    public String getAiTargetingRules()
    {
        return "## Desktop Coordinates Rule\n"
               + "You are controlling a desktop application visually. "
               + "For any action requiring a target element (like CLICK, TYPE, HOVER), "
               + "you MUST provide the exact [x, y] coordinates of the element on the screen "
               + "as the target string (e.g., `\"tg\": \"[500, 300]\"`). Do NOT use descriptions, CSS, or XPaths.\n\n";
    }

    @Override
    public String getClickPromptInstructions()
    {
        return "CLICK: Click on a target desktop coordinate (requires 'tg' to be the coordinates as '[x, y]').";
    }

    @Override
    public String getTypePromptInstructions()
    {
        return "TYPE: Type text into the active desktop window (requires 'v' to be the text value). You can optionally focus a target coordinate (using 'tg' as '[x, y]') before typing.";
    }

    @Override
    public String getHoverPromptInstructions()
    {
        return "HOVER: Hover the mouse pointer over a target desktop coordinate (requires 'tg' to be the coordinates as '[x, y]').";
    }

    @Override
    public String getNavigatePromptInstructions()
    {
        return "NAVIGATE: Launch or open a desktop application by running an OS command. Set 'v' to the command to execute (e.g., \"kate /tmp/file.txt\").";
    }

    @Override
    public String getAssertPromptInstructions()
    {
        return "ASSERT: Verify file existence and content on disk (set 'tg' to the absolute file path, and 'v' to the expected text content, e.g. \"Hello World\").";
    }

    @Override
    public String getPageSourceRepresentation(final ContextLevel level)
    {
        return "Desktop Session State: Active\n"
               + "Display Resolution: 1920x1080\n"
               + "Active Window: Kate text editor\n"
               + "Interactive Screen Coordinates: [x, y]\n\n"
               + "Coordinates Guide:\n"
               + "- [960, 540]: Center of the screen (typically inside the main editor area of maximized Kate)\n"
               + "- [10, 10]: Top-left of the screen\n\n";
    }
}
