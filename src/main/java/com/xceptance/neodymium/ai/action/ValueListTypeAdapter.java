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
package com.xceptance.neodymium.ai.action;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A custom GSON TypeAdapter that transparently deserializes either a single JSON string/primitive
 * or a JSON array of strings into a List of Strings. This ensures backward compatibility
 * with playbook configurations where a single value was saved as a raw string.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ValueListTypeAdapter extends TypeAdapter<List<String>>
{
    /**
     * Writes the list of values to JSON as a standard JSON array.
     *
     * @param out the JSON writer
     * @param value the list of string values to write
     * @throws IOException if a writing error occurs
     */
    @Override
    public void write(final JsonWriter out, final List<String> value) throws IOException
    {
        if (value == null)
        {
            out.nullValue();
            return;
        }
        out.beginArray();
        for (final String s : value)
        {
            out.value(s);
        }
        out.endArray();
    }

    /**
     * Reads a JSON token and deserializes it into a list of strings, supporting both array and single string forms.
     *
     * @param in the JSON reader
     * @return a list containing the deserialized string values, or null
     * @throws IOException if a reading error occurs
     */
    @Override
    public List<String> read(final JsonReader in) throws IOException
    {
        final JsonToken token = in.peek();
        if (token == JsonToken.NULL)
        {
            in.nextNull();
            return null;
        }
        final List<String> list = new ArrayList<>();
        if (token == JsonToken.BEGIN_ARRAY)
        {
            in.beginArray();
            while (in.hasNext())
            {
                list.add(in.nextString());
            }
            in.endArray();
        }
        else
        {
            list.add(in.nextString());
        }
        return list;
    }
}
