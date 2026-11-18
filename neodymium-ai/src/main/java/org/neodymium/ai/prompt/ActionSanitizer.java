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
package org.neodymium.ai.prompt;

import org.neodymium.ai.action.Action;
import org.neodymium.ai.model.SessionData;

/**
 * Interface representing a sanitizer that parameterizes executed actions on the fly.
 * Replaces hardcoded credentials with variable reference placeholders (e.g. "${password}")
 * before they are written to execution recordings.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public interface ActionSanitizer
{
    /**
     * Sanitizes an executed action on the fly, replacing actual sensitive values
     * with their variable references.
     *
     * @param rawAction the raw executed action
     * @param data the active session variables map
     * @return the sanitized and parameterized Action instance
     */
    Action sanitize(final Action rawAction, final SessionData data);
}
