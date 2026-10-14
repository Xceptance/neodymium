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
package org.neodymium.ai.server.verla.model;

import java.util.Map;

/**
 * Subcategory template metadata for VÉRLA catalog products.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class SubcategoryTemplate
{
    public String id;
    public Map<String, String> names;
    public String en;
    public String fr;
    public String de;
    public String pl;
    public String se;
    public String fi;
    public String ja;

    public String getSubName(final String locale)
    {
        if (names != null && names.containsKey(locale))
        {
            return names.get(locale);
        }
        if ("de".equals(locale) && de != null)
        {
            return de;
        }
        if ("fr".equals(locale) && fr != null)
        {
            return fr;
        }
        if ("pl".equals(locale) && pl != null)
        {
            return pl;
        }
        if ("se".equals(locale) && se != null)
        {
            return se;
        }
        if ("fi".equals(locale) && fi != null)
        {
            return fi;
        }
        if ("ja".equals(locale) && ja != null)
        {
            return ja;
        }
        if (en != null)
        {
            return en;
        }
        return id;
    }
}
