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
package org.neodymium.ai.playbook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.resources.PlaybookResourceManager;

/**
 * Concrete implementation of {@link PlaybookParser} that parses raw multi-line
 * strings directly into playbook steps (without using external resource managers).
 * Useful for inline playbook specifications inside test annotations.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class InlinePlaybookParser implements PlaybookParser
{
    /**
     * The raw multi-line playbook string content.
     */
    private final String content;

    /**
     * Constructs an InlinePlaybookParser with raw multi-line string content.
     *
     * @param content the raw playbook string content
     */
    public InlinePlaybookParser(final String content)
    {
        this.content = content;
    }

    /**
     * Parses the inline content directly into a Playbook.
     * Splitting the content by line feeds and skipping empty lines.
     *
     * @param identifier the identifier (ignored for inline parsing)
     * @param manager the resource manager (ignored for inline parsing)
     * @return the parsed Playbook instance
     * @throws IOException if parsing content fails
     */
    @Override
    public Playbook parse(final String identifier, final PlaybookResourceManager manager) throws IOException
    {
        final List<PlaybookStep> steps = new ArrayList<>();
        
        if (this.content != null)
        {
            final String[] lines = this.content.split("\\r?\\n");
            for (final String line : lines)
            {
                final String trimmed = line.trim();
                if (!trimmed.isEmpty())
                {
                    steps.add(new PlaybookStep(trimmed));
                }
            }
        }
        
        return new Playbook(steps, new ArrayList<>());
    }
}
