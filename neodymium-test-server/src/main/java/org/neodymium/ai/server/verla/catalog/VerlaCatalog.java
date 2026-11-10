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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.neodymium.ai.server.verla.model.CatalogConfig;
import org.neodymium.ai.server.verla.model.CategoryTemplate;
import org.neodymium.ai.server.verla.model.Country;
import org.neodymium.ai.server.verla.model.Product;
import org.neodymium.ai.server.verla.model.SubcategoryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Loads, manages, and queries the VÉRLA product catalog, country registry, and localization strings.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerlaCatalog
{
    private static final Logger LOG = LoggerFactory.getLogger(VerlaCatalog.class);

    private static final VerlaCatalog INSTANCE = new VerlaCatalog();

    private final CatalogConfig catalogConfig;
    private final List<Product> products;
    private final Map<String, Product> productsById;
    private final Map<String, Product> productsBySlug;
    private final Map<String, List<Product>> productsByCategory;
    private final Map<String, Country> countriesMap;

    public static VerlaCatalog getInstance()
    {
        return INSTANCE;
    }

    public VerlaCatalog()
    {
        this.catalogConfig = loadCatalogConfig();

        final Map<String, Country> countries = new LinkedHashMap<>();
        if (catalogConfig != null && catalogConfig.countries != null)
        {
            for (final Country c : catalogConfig.countries)
            {
                countries.put(c.code, c);
            }
        }
        this.countriesMap = Collections.unmodifiableMap(countries);

        final List<Product> prods = (catalogConfig != null) ? generateCatalogProducts(catalogConfig) : Collections.emptyList();
        this.products = Collections.unmodifiableList(prods);

        final Map<String, Product> byId = new HashMap<>();
        final Map<String, Product> bySlug = new HashMap<>();
        final Map<String, List<Product>> byCat = new HashMap<>();

        for (final Product p : prods)
        {
            byId.put(p.id, p);
            bySlug.put(p.slug, p);
            byCat.computeIfAbsent(p.category.toLowerCase(), k -> new ArrayList<>()).add(p);
        }

        this.productsById = Collections.unmodifiableMap(byId);
        this.productsBySlug = Collections.unmodifiableMap(bySlug);
        this.productsByCategory = Collections.unmodifiableMap(byCat);

        LOG.info("VÉRLA Catalog initialized with {} products and {} countries.", prods.size(), countries.size());
    }

    private static CatalogConfig loadCatalogConfig()
    {
        try
        {
            String json = null;
            final Path fsPath = Path.of("src/test/resources/ai-test-pages/verla-catalog.json");
            if (Files.exists(fsPath))
            {
                json = Files.readString(fsPath, StandardCharsets.UTF_8);
            }
            else
            {
                try (final InputStream is = VerlaCatalog.class.getClassLoader().getResourceAsStream("ai-test-pages/verla-catalog.json"))
                {
                    if (is != null)
                    {
                        json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                    }
                }
            }

            if (json == null)
            {
                LOG.error("verla-catalog.json not found on filesystem or classpath");
                return null;
            }

            return new Gson().fromJson(json, CatalogConfig.class);
        }
        catch (final Exception e)
        {
            LOG.error("Failed to parse verla-catalog.json", e);
            return null;
        }
    }

    public static List<Product> generateCatalogProducts(final CatalogConfig config)
    {
        final List<Product> products = new ArrayList<>();
        final List<CategoryTemplate> categories = config.generation.categories;
        final Map<String, List<String>> adjectives = config.generation.adjectives;
        final Map<String, List<String>> colors = config.generation.colors;
        final String defaultDesc = config.generation.descriptions.get("en");

        int idCounter = 1000;
        final Map<String, Integer> categoryCounts = new HashMap<>();
        categoryCounts.put("tops", 120);
        categoryCounts.put("bottoms", 85);
        categoryCounts.put("outerwear", 45);
        categoryCounts.put("footwear", 30);
        categoryCounts.put("accessories", 60);

        int totalIndex = 0;
        for (final CategoryTemplate cat : categories)
        {
            final int limit = categoryCounts.getOrDefault(cat.id, 100);
            for (int i = 0; i < limit; i++)
            {
                final SubcategoryTemplate sub = cat.subcategories.get(i % cat.subcategories.size());

                final String adjEn = adjectives.get("en").get((i * 3) % adjectives.get("en").size());
                final String colEn = colors.get("en").get((i * 7) % colors.get("en").size());

                final String id = "SKU-" + cat.id.toUpperCase().substring(0, 3) + "-" + idCounter++;
                final String slug = (adjEn + "-" + colEn + "-" + sub.id + "-" + i).toLowerCase().replace(" ", "-");

                final Map<String, String> names = new HashMap<>();
                final Map<String, String> descs = new HashMap<>();

                for (final String locale : config.translations.keySet())
                {
                    final List<String> adjList = adjectives.get(locale);
                    final List<String> colList = colors.get(locale);

                    final String adj = (adjList != null) ? adjList.get((i * 3) % adjList.size()) : adjEn;
                    final String col = (colList != null) ? colList.get((i * 7) % colList.size()) : colEn;

                    final String subName = sub.getSubName(locale);

                    final String name;
                    if ("ja".equals(locale))
                    {
                        name = adj + col + subName;
                    }
                    else
                    {
                        name = adj + " " + col + " " + subName;
                    }
                    names.put(locale, name);

                    final String descTemplate = config.generation.descriptions.get(locale);
                    descs.put(locale, descTemplate != null ? descTemplate : defaultDesc);
                }

                final double basePrice = 19.99 + (totalIndex % 60) * 2.00 + (totalIndex % 10) * 0.49;
                final String badge = (totalIndex % 15 == 0) ? "Sale" : ((totalIndex % 25 == 0) ? "New" : "");
                final Double salePrice = "Sale".equals(badge) ? Double.valueOf(Math.round(basePrice * 0.8 * 100.0) / 100.0) : null;
                final String hexColor = getColorHex(colEn);
                final String svgPath = getSvgPathForSubcategory(sub.id, hexColor);

                final Map<String, Integer> stockMap = new ConcurrentHashMap<>();
                final List<String> sizes = getSizesForCategory(cat.id);
                if (sizes.isEmpty())
                {
                    final int stockVal = totalIndex % 8 == 0 ? 0 : (totalIndex % 10) + 1;
                    stockMap.put("", stockVal);
                }
                else
                {
                    for (final String sz : sizes)
                    {
                        final int stockVal = (totalIndex + sz.hashCode()) % 11;
                        stockMap.put(sz, stockVal);
                    }
                }

                products.add(new Product(id, slug, names, descs, cat.id, sub.id, basePrice, badge, salePrice, svgPath, colEn, stockMap));
                totalIndex++;
            }
        }
        return products;
    }

    public static List<String> getSizesForCategory(final String category)
    {
        if ("accessories".equals(category))
        {
            return List.of();
        }
        else if ("bottoms".equals(category))
        {
            return List.of("28", "30", "32", "34", "36", "38");
        }
        else if ("footwear".equals(category) || "shoes".equals(category))
        {
            return List.of("39", "40", "41", "42", "43", "44");
        }
        else
        {
            return List.of("XS", "S", "M", "L", "XL");
        }
    }

    public static String getColorHex(final String colorName)
    {
        if (colorName == null)
        {
            return "#E6E2DA";
        }
        switch (colorName.toLowerCase())
        {
            case "off-white":
                return "#FAF9F6";
            case "charcoal":
                return "#2C2C2C";
            case "navy":
                return "#1E2A38";
            case "olive":
                return "#555D50";
            case "sage":
                return "#9CAF88";
            case "sand":
                return "#D8C7B5";
            case "taupe":
                return "#B38B6D";
            case "terracotta":
                return "#E07A5F";
            case "espresso":
                return "#4B382A";
            case "black":
                return "#1A1A1A";
            case "white":
                return "#FFFFFF";
            case "grey":
                return "#8A8D8F";
            case "slate":
                return "#708090";
            case "forest":
                return "#2D5A27";
            case "burgundy":
                return "#800020";
            case "cognac":
                return "#9A463D";
            case "caramel":
                return "#C68E17";
            default:
                return "#D3D3D3";
        }
    }

    public static String getCountryFlag(final String countryCode)
    {
        if (countryCode == null || countryCode.isBlank())
        {
            return "🏳️";
        }

        final String upperCode = countryCode.trim().toUpperCase();
        final String resolvedCode;
        if ("UK".equals(upperCode))
        {
            resolvedCode = "GB";
        }
        else if (upperCode.startsWith("CA"))
        {
            resolvedCode = "CA";
        }
        else if (upperCode.contains("_") || upperCode.contains("-"))
        {
            final int sep = upperCode.indexOf('_') != -1 ? upperCode.indexOf('_') : upperCode.indexOf('-');
            resolvedCode = upperCode.substring(0, sep);
        }
        else
        {
            resolvedCode = upperCode;
        }

        if (resolvedCode.length() != 2)
        {
            return "🏳️";
        }

        final char c1 = resolvedCode.charAt(0);
        final char c2 = resolvedCode.charAt(1);
        if (c1 < 'A' || c1 > 'Z' || c2 < 'A' || c2 > 'Z')
        {
            return "🏳️";
        }

        final int firstChar = c1 - 'A' + 0x1F1E6;
        final int secondChar = c2 - 'A' + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    public static String getSvgPathForSubcategory(final String subcat, final String hexColor)
    {
        switch (subcat)
        {
            case "shirts":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M30,35 L70,35 L75,75 L25,75 Z\" fill=\"" + hexColor + "\" stroke=\"#A8A297\" stroke-width=\"1.5\" />" +
                       "<path d=\"M40,35 L50,45 L60,35\" fill=\"none\" stroke=\"#A8A297\" stroke-width=\"1.5\" />";
            case "tshirts":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M32,32 L68,32 L70,68 L30,68 Z\" fill=\"" + hexColor + "\" stroke=\"#8E998B\" stroke-width=\"1.5\" />";
            case "sweaters":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M25,30 L75,30 L70,70 L30,70 Z\" fill=\"" + hexColor + "\" stroke=\"#8A8A8A\" stroke-width=\"1.5\" />";
            case "pants":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M30,30 L70,30 L65,80 L52,80 L50,50 L48,80 L35,80 Z\" fill=\"" + hexColor + "\" stroke=\"#9E998E\" stroke-width=\"1.5\" />";
            case "shorts":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M30,35 L70,35 L68,65 L32,65 Z\" fill=\"" + hexColor + "\" stroke=\"#8A8A8A\" stroke-width=\"1.5\" />";
            case "skirts":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M35,35 L65,35 L75,75 L25,75 Z\" fill=\"" + hexColor + "\" stroke=\"#A78E77\" stroke-width=\"1.5\" />";
            case "jackets":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M28,28 L72,28 L68,75 L32,75 Z\" fill=\"" + hexColor + "\" stroke=\"#879583\" stroke-width=\"1.5\" />";
            case "coats":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M28,25 L72,25 L68,80 L32,80 Z\" fill=\"" + hexColor + "\" stroke=\"#908775\" stroke-width=\"1.5\" />" +
                       "<path d=\"M42,25 L50,45 L58,25\" fill=\"none\" stroke=\"#908775\" stroke-width=\"1.5\" />" +
                       "<line x1=\"50\" y1=\"45\" x2=\"50\" y2=\"80\" stroke=\"#908775\" stroke-width=\"1.5\" />";
            case "sneakers":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M15,65 L45,55 L75,55 L85,70 L85,80 L15,80 Z\" fill=\"" + hexColor + "\" stroke=\"#8E998B\" stroke-width=\"1.5\" />";
            case "boots":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M25,30 L45,30 L45,60 L75,60 L80,80 L20,80 Z\" fill=\"" + hexColor + "\" stroke=\"#BAA694\" stroke-width=\"1.5\" />";
            case "bags":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<rect x=\"25\" y=\"40\" width=\"50\" height=\"40\" rx=\"5\" fill=\"" + hexColor + "\" stroke=\"#A78E77\" stroke-width=\"1.5\" />" +
                       "<path d=\"M35,40 C35,25 65,25 65,40\" stroke=\"#A78E77\" fill=\"none\" stroke-width=\"1.5\" />";
            case "hats":
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<path d=\"M15,70 C15,45 85,45 85,70 L90,70 L90,75 L10,75 L10,70 Z\" fill=\"" + hexColor + "\" stroke=\"#9E998E\" stroke-width=\"1.5\" />";
            default:
                return "<rect width=\"100\" height=\"100\" fill=\"#FAF9F6\" />" +
                       "<rect x=\"25\" y=\"25\" width=\"50\" height=\"50\" rx=\"5\" fill=\"" + hexColor + "\" stroke=\"#A8A297\" stroke-width=\"1.5\" />";
        }
    }

    public CatalogConfig getCatalogConfig()
    {
        return catalogConfig;
    }

    public List<Product> getProducts()
    {
        return products;
    }

    public Product getProductById(final String id)
    {
        if (id == null)
        {
            return null;
        }
        final String cleanId = id.contains(":") ? id.split(":")[0] : id;
        final Product p = productsById.get(cleanId);
        if (p != null)
        {
            return p;
        }
        return productsBySlug.get(cleanId);
    }

    public Product getProductBySlug(final String slug)
    {
        if (slug == null)
        {
            return null;
        }
        final String cleanSlug = slug.contains(":") ? slug.split(":")[0] : slug;
        final Product p = productsBySlug.get(cleanSlug);
        if (p != null)
        {
            return p;
        }
        return productsById.get(cleanSlug);
    }

    public List<Product> getProductsByCategory(final String category)
    {
        return productsByCategory.getOrDefault(category.toLowerCase(), Collections.emptyList());
    }

    public Map<String, Country> getCountriesMap()
    {
        return countriesMap;
    }

    public Country getCountry(final String code)
    {
        if (code == null)
        {
            return countriesMap.get("US");
        }
        final String upperCode = code.toUpperCase();
        final Country direct = countriesMap.get(upperCode);
        if (direct != null)
        {
            return direct;
        }
        if ("CA".equals(upperCode))
        {
            return countriesMap.getOrDefault("CA_EN", countriesMap.get("US"));
        }
        return countriesMap.get("US");
    }

    public List<Country> getCountries()
    {
        return catalogConfig != null && catalogConfig.countries != null ? catalogConfig.countries : Collections.emptyList();
    }

    public Product lookupProductById(final String id)
    {
        return getProductById(id);
    }

    public Product lookupProductBySlug(final String slug)
    {
        return getProductBySlug(slug);
    }

    public Map<String, String> getTranslations(final String locale)
    {
        if (catalogConfig != null && catalogConfig.translations != null)
        {
            final Map<String, String> trans = catalogConfig.translations.get(locale);
            if (trans != null)
            {
                return trans;
            }
            return catalogConfig.translations.getOrDefault("en", Collections.emptyMap());
        }
        return Collections.emptyMap();
    }
}
