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
package com.xceptance.aura.core.service;

import com.xceptance.aura.core.dto.AuraOperationResult;
import com.xceptance.aura.core.dto.PlaybookSummaryDto;

import java.util.List;

/**
 * Service managing test playbook files, validation, and content operations.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public interface AuraPlaybookService
{
    /**
     * Lists all available playbooks, optionally filtered by keyword.
     *
     * @param filter optional filter keyword
     * @return list of playbook summaries
     */
    List<PlaybookSummaryDto> listPlaybooks(String filter);

    /**
     * Reads the raw YAML content of a playbook.
     *
     * @param testPath relative path to the playbook file
     * @return operation result containing raw content or error
     */
    AuraOperationResult<String> readPlaybook(String testPath);

    /**
     * Creates or updates a playbook file.
     *
     * @param testPath relative destination path
     * @param content raw YAML content
     * @return operation result
     */
    AuraOperationResult<Void> savePlaybook(String testPath, String content);

    /**
     * Deletes a playbook file after path validation.
     *
     * @param testPath relative path to the playbook file
     * @return operation result indicating success or failure
     */
    AuraOperationResult<Boolean> deletePlaybook(String testPath);
}
