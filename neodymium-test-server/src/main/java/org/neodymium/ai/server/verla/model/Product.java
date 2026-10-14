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
 * Represents a localized product in the VÉRLA e-commerce storefront.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class Product
{
    public final String id;
    public final String slug;
    public final Map<String, String> names;
    public final Map<String, String> descriptions;
    public final String category;
    public final String subcategory;
    public final double basePrice;
    public final String badge;
    public final Double salePrice;
    public final String svgPath;
    public final String color;
    public final Map<String, Integer> initialStock;

    public Product(final String id, final String slug, final Map<String, String> names, final Map<String, String> descriptions,
                   final String category, final String subcategory, final double basePrice, final String badge, final Double salePrice,
                   final String svgPath, final String color, final Map<String, Integer> initialStock)
    {
        this.id = id;
        this.slug = slug;
        this.names = names;
        this.descriptions = descriptions;
        this.category = category;
        this.subcategory = subcategory;
        this.basePrice = basePrice;
        this.badge = badge;
        this.salePrice = salePrice;
        this.svgPath = svgPath;
        this.color = color;
        this.initialStock = initialStock;
    }
}
