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
package org.neodymium.ai.server;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

import io.javalin.json.JsonMapper;

/**
 * Adapter implementing Javalin's {@link JsonMapper} using Google Gson.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class JavalinGsonMapper implements JsonMapper
{
    private final Gson gson;

    public JavalinGsonMapper()
    {
        this(new Gson());
    }

    public JavalinGsonMapper(final Gson gson)
    {
        this.gson = gson != null ? gson : new Gson();
    }

    @Override
    public String toJsonString(final Object obj, final Type type)
    {
        return gson.toJson(obj, type);
    }

    @Override
    public <T> T fromJsonString(final String json, final Type targetType)
    {
        return gson.fromJson(json, targetType);
    }

    @Override
    public <T> T fromJsonStream(final InputStream json, final Type targetType)
    {
        return gson.fromJson(new InputStreamReader(json, StandardCharsets.UTF_8), targetType);
    }
}
