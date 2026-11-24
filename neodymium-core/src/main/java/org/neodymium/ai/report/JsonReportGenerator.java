/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;

/**
 * Report generator producing machine-readable structured JSON documents from {@link TestExecutionReport}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class JsonReportGenerator
{
    private final ObjectMapper mapper;

    /**
     * Constructs a JsonReportGenerator with pretty printing enabled.
     */
    public JsonReportGenerator()
    {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Serializes the test execution report to a pretty-printed JSON string.
     *
     * @param report the test execution report
     * @return pretty-printed JSON string
     * @throws IOException if serialization fails
     */
    public String generate(final TestExecutionReport report) throws IOException
    {
        if (report == null)
        {
            return "{}";
        }
        return this.mapper.writeValueAsString(report);
    }
}
