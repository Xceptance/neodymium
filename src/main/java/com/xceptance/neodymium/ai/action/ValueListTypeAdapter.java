/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
 * // AI-generated: Gemini 2.5 Pro
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
