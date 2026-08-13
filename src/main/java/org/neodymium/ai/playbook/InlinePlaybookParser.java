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
import java.util.Collections;
import java.util.List;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.resources.InMemoryResourceManager;
import org.neodymium.ai.resources.PlaybookResourceManager;

/**
 * Parser implementation that parses inline multi-line playbook strings into a {@link Playbook}.
 * Supports both plain line-by-line step instructions and structured YAML string content.
 *
 * @author AI-generated: Gemini 3.6 Flash
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
     * Delegates to YamlPlaybookParser if structured YAML content is detected,
     * otherwise splits the content by line feeds.
     *
     * @param identifier the identifier (ignored for inline parsing)
     * @param manager the resource manager (ignored for inline parsing)
     * @return the parsed Playbook instance
     * @throws IOException if parsing content fails
     */
    @Override
    public Playbook parse(final String identifier, final PlaybookResourceManager manager) throws IOException
    {
        if (this.content == null || this.content.trim().isEmpty())
        {
            return new Playbook(Collections.emptyList(), Collections.emptyList());
        }

        final String trimmed = this.content.trim();
        if (trimmed.startsWith("steps:") || trimmed.startsWith("inline:") || trimmed.startsWith("---") || trimmed.startsWith("_include:") || trimmed.startsWith("include:") || trimmed.contains("\nsteps:") || trimmed.contains("\ndata:") || trimmed.contains("\n_include:") || trimmed.contains("\ninclude:"))
        {
            final InMemoryResourceManager stringManager = (manager != null)
                ? new InMemoryResourceManager(manager)
                : new InMemoryResourceManager();
            stringManager.write("inline.yaml", content);
            return new YamlPlaybookParser().parse("inline.yaml", stringManager);
        }

        final List<PlaybookStep> steps = new ArrayList<>();
        final String[] lines = this.content.split("\\r?\\n");
        for (final String line : lines)
        {
            final String lineTrimmed = line.trim();
            if (!lineTrimmed.isEmpty() && !lineTrimmed.startsWith("#"))
            {
                steps.add(new PlaybookStep(lineTrimmed));
            }
        }

        return new Playbook(steps, Collections.emptyList());
    }
}
