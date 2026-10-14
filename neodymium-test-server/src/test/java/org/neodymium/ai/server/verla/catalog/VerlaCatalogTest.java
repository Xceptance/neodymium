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
package org.neodymium.ai.server.verla.catalog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.server.verla.model.Product;

/**
 * Unit tests for {@link VerlaCatalog} verifying product lookups by ID, slug,
 * composite keys with size suffixes, and bidirectional ID/slug fallbacks.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class VerlaCatalogTest
{
    private static VerlaCatalog catalog;

    @BeforeAll
    public static void setUp()
    {
        catalog = VerlaCatalog.getInstance();
    }

    @Test
    public void testLookupProductByExactId()
    {
        final Product first = catalog.getProducts().getFirst();
        final Product product = catalog.getProductById(first.id);
        Assertions.assertNotNull(product, "Product should be found by exact ID");
        Assertions.assertEquals(first.id, product.id);
    }

    @Test
    public void testLookupProductByIdWithCompositeSizeKey()
    {
        final Product first = catalog.getProducts().getFirst();
        final Product product = catalog.getProductById(first.id + ":32");
        Assertions.assertNotNull(product, "Product should be resolved when key contains size suffix");
        Assertions.assertEquals(first.id, product.id);

        final Product lookupProduct = catalog.lookupProductById(first.id + ":34");
        Assertions.assertNotNull(lookupProduct, "lookupProductById should resolve composite key");
        Assertions.assertEquals(first.id, lookupProduct.id);
    }

    @Test
    public void testLookupProductByIdFallbackToSlug()
    {
        final Product first = catalog.getProducts().getFirst();
        final Product product = catalog.getProductById(first.slug);
        Assertions.assertNotNull(product, "Product should resolve by slug fallback when ID lookup fails");
        Assertions.assertEquals(first.id, product.id);

        final Product compositeSlug = catalog.getProductById(first.slug + ":32");
        Assertions.assertNotNull(compositeSlug, "Composite slug:size should resolve via ID fallback");
        Assertions.assertEquals(first.id, compositeSlug.id);
    }

    @Test
    public void testLookupProductByExactSlug()
    {
        final Product first = catalog.getProducts().getFirst();
        final Product product = catalog.getProductBySlug(first.slug);
        Assertions.assertNotNull(product, "Product should be found by exact slug");
        Assertions.assertEquals(first.slug, product.slug);
    }

    @Test
    public void testLookupProductBySlugWithCompositeSizeKey()
    {
        final Product first = catalog.getProducts().getFirst();
        final Product product = catalog.getProductBySlug(first.slug + ":32");
        Assertions.assertNotNull(product, "Product should be resolved when slug key contains size suffix");
        Assertions.assertEquals(first.slug, product.slug);

        final Product lookupProduct = catalog.lookupProductBySlug(first.slug + ":36");
        Assertions.assertNotNull(lookupProduct, "lookupProductBySlug should resolve composite size key");
        Assertions.assertEquals(first.slug, lookupProduct.slug);
    }

    @Test
    public void testLookupProductBySlugFallbackToId()
    {
        final Product first = catalog.getProducts().getFirst();
        final Product product = catalog.getProductBySlug(first.id);
        Assertions.assertNotNull(product, "Product should resolve by ID fallback when slug lookup fails");
        Assertions.assertEquals(first.slug, product.slug);

        final Product compositeId = catalog.getProductBySlug(first.id + ":32");
        Assertions.assertNotNull(compositeId, "Composite id:size should resolve via slug fallback");
        Assertions.assertEquals(first.slug, compositeId.slug);
    }

    @Test
    public void testLookupProductWithNullOrUnknownReturnsNull()
    {
        Assertions.assertNull(catalog.getProductById(null), "Null ID should return null");
        Assertions.assertNull(catalog.getProductBySlug(null), "Null slug should return null");
        Assertions.assertNull(catalog.getProductById("UNKNOWN-SKU"), "Unknown ID should return null");
        Assertions.assertNull(catalog.getProductBySlug("unknown-slug"), "Unknown slug should return null");
    }
}
