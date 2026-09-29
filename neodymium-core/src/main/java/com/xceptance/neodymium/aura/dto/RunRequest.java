/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 *_ it under the terms of the GNU Affero General Public License as published by
 *_ the Free Software Foundation, either version 3 of the License, or
 *_ (at your option) any later version.
 *_
 *_ This program is distributed in the hope that it will be useful,
 *_ but WITHOUT ANY WARRANTY; without even the implied warranty of
 *_ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *_ GNU Affero General Public License for more details.
 *_
 *_ You should have received a copy of the GNU Affero General Public License
 *_ along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.aura.dto;

import java.util.List;

/**
 * Data transfer object representing a request to run a selection of tests/datasets.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class RunRequest
{
    public List<DatasetSelection> datasets;
    public boolean headless;
    public boolean interactive;
    public boolean allure;
    public boolean video;
    public String executionMode = "LLM_RECORDING";
    public List<String> globalBrowserProfiles;

    public RunRequest()
    {
    }

    public RunRequest(final List<DatasetSelection> datasets, final boolean headless, final boolean interactive,
            final boolean allure, final boolean video, final String executionMode)
    {
        this(datasets, headless, interactive, allure, video, executionMode, null);
    }

    public RunRequest(final List<DatasetSelection> datasets, final boolean headless, final boolean interactive,
            final boolean allure, final boolean video, final String executionMode, final List<String> globalBrowserProfiles)
    {
        this.datasets = datasets;
        this.headless = headless;
        this.interactive = interactive;
        this.allure = allure;
        this.video = video;
        this.executionMode = executionMode != null ? executionMode : "LLM_RECORDING";
        this.globalBrowserProfiles = globalBrowserProfiles;
    }
}
