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
package org.neodymium.ai.integration.external;

import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiInlinePlaybook;
import org.neodymium.ai.junit.AiJudge;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.common.browser.Browser;

/**
 * External integration test executing search workflows against blog.xceptance.com
 * across all execution modes (FORCE_RECORDING, REPLAY_STRICT, REPLAY_WITH_HEALING)
 * and 3 judge configuration variants (Standard, Embedded Judge, Extra Judge).
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@NeodymiumAiTest
@AiPlaybook(recordingDirectory = "target/playbooks/external")
@Browser("Chrome_1500x1000_headless")
public class BlogTest
{
    // =========================================================================
    // 1. Standard (No Quality Judge)
    // =========================================================================

    @AiJudge(false)
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiInlinePlaybook(
        """
            promptAddon: Some elements might require a click first to be revealed. 

            steps: |
                Open 'https://blog.xceptance.com/'.
                Search for 'Neodymium' by typing the searchphrase and the enter key.
                'Search Results for: neodymium' is displayed.
        """
     )
    public void bruteforceSearch(final AiSession session)
    {
    }

    @AiJudge(false)
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiInlinePlaybook(
        """
            steps: |
                Open 'https://blog.xceptance.com/'.
                Open the search input by clicking the magnifyiing glass icon.
                Type 'neodymium' into the search field.
                Press enter.
                'Search Results for: neodymium' is displayed.
        """
     )
    public void preciseSearch(final AiSession session)
    {
    }


    // =========================================================================
    // 3. Extra Judge (Separate LLM Step)
    // =========================================================================

    @AiJudge(true)
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiInlinePlaybook(
        """
            steps: |
                Open 'https://blog.xceptance.com/'.
                Search for 'Neodymium'.
                'Search Results for: neodymium' is displayed.
        """
     )
    public void bruteforceSearchExtraJudge(final AiSession session)
    {
    }

    @AiJudge(true)
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiInlinePlaybook(
        """
            steps: |
                Open 'https://blog.xceptance.com/'.
                Open the search input by clicking the magnifyiing glass icon.
                Type 'neodymium' into the search field.
                Press enter.
                'Search Results for: neodymium' is displayed.
        """
     )
    public void preciseSearchExtraJudge(final AiSession session)
    {
    }
}