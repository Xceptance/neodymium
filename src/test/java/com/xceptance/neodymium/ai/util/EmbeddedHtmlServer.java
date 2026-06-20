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
package com.xceptance.neodymium.ai.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

/**
 * A lightweight, embedded HTTP server for testing AI actions locally without external dependencies.
 * It serves static files from the 'src/test/resources/ai-test-pages/' classpath directory
 * and handles the dynamic, HTMX-enabled VÉRLA e-commerce storefront.
 * 
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class EmbeddedHtmlServer
{
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedHtmlServer.class);
    private final HttpServer server;
    private final HttpsServer httpsServer;
    private final int port;
    private final int httpsPort;

    // --- VÉRLA E-Commerce Domain Models ---
    
    public static final class Country
    {
        public String code;
        public String name;
        public String locale;
        public String currency;
        public String symbol;
        public double rate;
        public String format;
    }

    public static final class CategoryTemplate
    {
        public String id;
        public Map<String, String> names;
        public List<SubcategoryTemplate> subcategories;
    }

    public static final class SubcategoryTemplate
    {
        public String id;
        public Map<String, String> names;
    }

    public static final class GenerationConfig
    {
        public List<CategoryTemplate> categories;
        public Map<String, List<String>> adjectives;
        public Map<String, List<String>> colors;
        public Map<String, String> descriptions;
    }

    public static final class CatalogConfig
    {
        public List<Country> countries;
        public Map<String, Map<String, String>> translations;
        public GenerationConfig generation;
    }

    public static final class Product
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
                       final String category, final String subcategory, final double basePrice, final String badge, final Double salePrice, final String svgPath, final String color,
                       final Map<String, Integer> initialStock)
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

    public static final class User
    {
        public final String email;
        public String password;
        public final List<Address> addresses = new ArrayList<>();
        public final List<Card> cards = new ArrayList<>();
        public final List<Order> orders = new ArrayList<>();

        public User(final String email, final String password)
        {
            this.email = email;
            this.password = password;
        }
    }

    public static final class Address
    {
        public final String id;
        public final String street;
        public final String city;
        public final String state;
        public final String postcode;
        public final String country;

        public Address(final String id, final String street, final String city, final String state, final String postcode, final String country)
        {
            this.id = id;
            this.street = street;
            this.city = city;
            this.state = state;
            this.postcode = postcode;
            this.country = country;
        }
    }

    public static final class Card
    {
        public final String id;
        public final String number;
        public final String type;
        public final String expiry;
        public final String cvv;

        public Card(final String id, final String number, final String type, final String expiry, final String cvv)
        {
            this.id = id;
            this.number = number;
            this.type = type;
            this.expiry = expiry;
            this.cvv = cvv;
        }
    }

    public static final class Order
    {
        public final String orderNumber;
        public final String email;
        public final List<OrderItem> items;
        public final double subtotal;
        public final double shipping;
        public final double tax;
        public final double total;
        public final String status;
        public final Address shippingAddress;
        public final Card paymentCard;
        public final String discountApplied;
        public final boolean freeGiftAdded;

        public Order(final String orderNumber, final String email, final List<OrderItem> items, final double subtotal,
                     final double shipping, final double tax, final double total, final String status,
                     final Address shippingAddress, final Card paymentCard, final String discountApplied, final boolean freeGiftAdded)
        {
            this.orderNumber = orderNumber;
            this.email = email;
            this.items = items;
            this.subtotal = subtotal;
            this.shipping = shipping;
            this.tax = tax;
            this.total = total;
            this.status = status;
            this.shippingAddress = shippingAddress;
            this.paymentCard = paymentCard;
            this.discountApplied = discountApplied;
            this.freeGiftAdded = freeGiftAdded;
        }
    }

    public static final class OrderItem
    {
        public final String productId;
        public final String productName;
        public final int quantity;
        public final double price;

        public OrderItem(final String productId, final String productName, final int quantity, final double price)
        {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
    }

    public static final class Cart
    {
        public final String id;
        public final Map<String, Integer> items = new ConcurrentHashMap<>();
        public String coupon = null;

        public Cart(final String id)
        {
            this.id = id;
        }
    }

    // --- VÉRLA Store In-Memory Databases ---

    private static final Map<String, User> usersDb = new ConcurrentHashMap<>();
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private static final Map<String, Cart> activeCarts = new ConcurrentHashMap<>();
    private static final Map<String, Order> ordersDb = new ConcurrentHashMap<>();

    private static CatalogConfig catalogConfig;
    private static final List<Product> catalogProducts = new ArrayList<>();
    private static final Map<String, Map<String, Integer>> productInventory = new ConcurrentHashMap<>();
    private static final Map<String, Country> countriesMap = new HashMap<>();

    static
    {
        // Pre-load default test user
        final User defaultUser = new User("johndoe@example.com", "topsecret");
        defaultUser.addresses.add(new Address("addr-default", "123 Main St", "Boston", "MA", "02108", "US"));
        defaultUser.cards.add(new Card("card-default", "1111222233334100", "Visa", "12/29", "123"));
        usersDb.put(defaultUser.email, defaultUser);
        
        loadCatalogData();
    }

    /**
     * Load the JSON catalog configuration file and load or generate catalog products.
     */
    private static void loadCatalogData()
    {
        try (final InputStream is = EmbeddedHtmlServer.class.getClassLoader().getResourceAsStream("ai-test-pages/verla-catalog.json"))
        {
            if (is == null)
            {
                LOG.error("verla-catalog.json not found in classpath resources");
                return;
            }
            final String json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

            final Gson gson = new Gson();
            catalogConfig = gson.fromJson(json, CatalogConfig.class);

            for (final Country c : catalogConfig.countries)
            {
                countriesMap.put(c.code, c);
            }

            try (final InputStream prodIs = EmbeddedHtmlServer.class.getClassLoader().getResourceAsStream("ai-test-pages/verla-products.json"))
            {
                if (prodIs != null)
                {
                    final String prodJson = new BufferedReader(new InputStreamReader(prodIs, StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining("\n"));
                    final Type listType = new TypeToken<List<Product>>(){}.getType();
                    final List<Product> loadedProducts = gson.fromJson(prodJson, listType);
                    catalogProducts.clear();
                    catalogProducts.addAll(loadedProducts);

                    productInventory.clear();
                    for (final Product p : catalogProducts)
                    {
                        productInventory.put(p.id, new ConcurrentHashMap<>(p.initialStock));
                    }
                    LOG.info("VÉRLA Static catalog loaded. Loaded {} products from verla-products.json.", catalogProducts.size());
                }
                else
                {
                    LOG.warn("verla-products.json not found. Falling back to dynamic programmatic catalog generation.");
                    final List<Product> generated = generateCatalogProducts(catalogConfig);
                    catalogProducts.clear();
                    catalogProducts.addAll(generated);

                    productInventory.clear();
                    for (final Product p : catalogProducts)
                    {
                        productInventory.put(p.id, new ConcurrentHashMap<>(p.initialStock));
                    }
                    LOG.info("VÉRLA Dynamic catalog loaded. Generated {} products.", catalogProducts.size());
                }
            }
        }
        catch (final Exception e)
        {
            LOG.error("Failed to parse and generate catalog data", e);
        }
    }

    /**
     * Programmatically generates varying products across category templates based on catalog config.
     * Useful for regenerating the verla-products.json file.
     */
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
                    
                    final String subName = (sub.names != null && sub.names.containsKey(locale)) ? sub.names.get(locale) : sub.id;

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

                // Initialize inventory: 1-10 random stock, sometimes 0 (out of stock)
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

    private static List<String> getSizesForCategory(final String category)
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

    private static String getColorHex(final String colorName)
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
                return "#363636";
            case "olive":
                return "#606E50";
            case "sand":
                return "#D5C7B4";
            case "navy":
                return "#2B3E50";
            case "taupe":
                return "#8E8279";
            case "sage":
                return "#8EA38C";
            case "terracotta":
                return "#C87A53";
            default:
                return "#E6E2DA";
        }
    }

    private static String getProductSizesSelectHtml(final String productId, final String category)
    {
        if ("accessories".equals(category))
        {
            return "";
        }

        final Map<String, Integer> stockMap = productInventory.getOrDefault(productId, Map.of());
        final List<String> sizes = getSizesForCategory(category);

        final StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"display: flex; gap: 12px; align-items: center;\">");
        sb.append("<label for=\"size\" style=\"font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em;\">Size:</label>");
        sb.append("<select name=\"size\" id=\"size\" style=\"padding: 10px 16px; border: 1px solid var(--color-border); border-radius: var(--border-radius); background: var(--color-bg-primary); outline: none; cursor: pointer;\" required>");

        String defaultSel = "";
        if ("bottoms".equals(category))
        {
            defaultSel = "32";
        }
        else if ("footwear".equals(category) || "shoes".equals(category))
        {
            defaultSel = "41";
        }
        else
        {
            defaultSel = "M";
        }

        String sizeToSelect = "";
        for (final String sz : sizes)
        {
            if (stockMap.getOrDefault(sz, 0) > 0)
            {
                if (sz.equals(defaultSel))
                {
                    sizeToSelect = sz;
                    break;
                }
                if (sizeToSelect.isEmpty())
                {
                    sizeToSelect = sz;
                }
            }
        }
        if (sizeToSelect.isEmpty() && !sizes.isEmpty())
        {
            sizeToSelect = defaultSel;
        }

        for (final String sz : sizes)
        {
            final int stock = stockMap.getOrDefault(sz, 0);
            final String label;
            final String disabledAttr;
            if (stock <= 0)
            {
                label = sz + " (Out of stock)";
                disabledAttr = " disabled";
            }
            else if (stock <= 5)
            {
                label = sz + " (" + stock + " left)";
                disabledAttr = "";
            }
            else
            {
                label = sz;
                disabledAttr = "";
            }

            final String sel = sz.equals(sizeToSelect) ? " selected" : "";
            sb.append("<option value=\"").append(sz).append("\"").append(sel).append(disabledAttr).append(">")
              .append(label).append("</option>");
        }

        sb.append("</select>");
        sb.append("</div>");
        return sb.toString();
    }

    private static String getCountryFlag(final String countryCode)
    {
        if (countryCode == null)
        {
            return "🌍";
        }

        final String upperCode = countryCode.toUpperCase();
        final String resolvedCode;
        if ("UK".equals(upperCode))
        {
            resolvedCode = "GB";
        }
        else if (upperCode.startsWith("CA"))
        {
            resolvedCode = "CA";
        }
        else
        {
            resolvedCode = upperCode;
        }

        if (resolvedCode.length() != 2)
        {
            return "🌍";
        }

        final int firstChar = resolvedCode.charAt(0) - 'A' + 0x1F1E6;
        final int secondChar = resolvedCode.charAt(1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    private static String getSvgPathForSubcategory(final String subcat, final String hexColor)
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

    /**
     * Creates a new embedded HTTP and HTTPS server bound to random free ports.
     * 
     * @throws IOException if the server cannot be bound or created
     */
    public EmbeddedHtmlServer() throws IOException
    {
        final int httpPort = Integer.getInteger("neodymium.ai.http.port", 43377);
        this.server = HttpServer.create(new InetSocketAddress(httpPort), 0);
        this.port = this.server.getAddress().getPort();
        
        final ResourceHandler resourceHandler = new ResourceHandler();
        final VerlaHandler verlaHandler = new VerlaHandler();

        this.server.createContext("/", new LoggingHandler(resourceHandler));
        this.server.createContext("/verla-perfect/", new LoggingHandler(verlaHandler));
        this.server.createContext("/verla-normal/", new LoggingHandler(verlaHandler));
        this.server.createContext("/verla-bad/", new LoggingHandler(verlaHandler));
        this.server.setExecutor(Executors.newCachedThreadPool());

        final int httpsPort = Integer.getInteger("neodymium.ai.https.port", 39389);
        this.httpsServer = HttpsServer.create(new InetSocketAddress(httpsPort), 0);
        this.httpsPort = this.httpsServer.getAddress().getPort();

        try
        {
            final KeyStore ks = KeyStore.getInstance("PKCS12");
            try (final InputStream ksf = EmbeddedHtmlServer.class.getClassLoader().getResourceAsStream("keystore.p12"))
            {
                if (ksf == null)
                {
                    throw new IOException("keystore.p12 resource not found on classpath.");
                }
                ks.load(ksf, "changeit".toCharArray());
            }

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, "changeit".toCharArray());

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            this.httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            this.httpsServer.createContext("/", new LoggingHandler(resourceHandler));
            this.httpsServer.createContext("/verla-perfect/", new LoggingHandler(verlaHandler));
            this.httpsServer.createContext("/verla-normal/", new LoggingHandler(verlaHandler));
            this.httpsServer.createContext("/verla-bad/", new LoggingHandler(verlaHandler));
            this.httpsServer.setExecutor(Executors.newCachedThreadPool());
        }
        catch (final Exception e)
        {
            throw new IOException("Failed to initialize secure HttpsServer with self-signed certificate", e);
        }
    }

    public void start()
    {
        LOG.info("Starting embedded HTML HTTP server on port {}", port);
        server.start();
        LOG.info("Starting embedded HTML HTTPS server on port {}", httpsPort);
        httpsServer.start();
    }

    public void stop()
    {
        LOG.info("Stopping embedded HTML HTTP server on port {}", port);
        server.stop(0);
        LOG.info("Stopping embedded HTML HTTPS server on port {}", httpsPort);
        httpsServer.stop(0);
    }

    public int getPort()
    {
        return port;
    }

    public int getHttpsPort()
    {
        return httpsPort;
    }

    private static final class ResourceHandler implements HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            String path = exchange.getRequestURI().getPath();
            
            if (path.equals("/"))
            {
                path = "index.html";
            }
            else if (path.startsWith("/"))
            {
                path = path.substring(1);
            }
            
            final String resourcePath = "ai-test-pages/" + path;
            
            try (final InputStream is = EmbeddedHtmlServer.class.getClassLoader().getResourceAsStream(resourcePath))
            {
                if (is == null)
                {
                    LOG.warn("Resource not found: {}", resourcePath);
                    final String response = "404 Not Found";
                    exchange.sendResponseHeaders(404, response.length());
                    try (final OutputStream os = exchange.getResponseBody())
                    {
                        os.write(response.getBytes());
                    }
                    return;
                }
                
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("ResourceHandler: serving static resource '{}' mapped from path '{}'", resourcePath, path);
                }
                else
                {
                    LOG.info("Serving static resource: {}", path);
                }
                
                final byte[] bytes = is.readAllBytes();
                String contentType = "text/plain";
                
                if (path.endsWith(".html"))
                {
                    contentType = "text/html; charset=UTF-8";
                }
                else if (path.endsWith(".css"))
                {
                    contentType = "text/css";
                }
                else if (path.endsWith(".js"))
                {
                    contentType = "application/javascript";
                }
                else if (path.endsWith(".png"))
                {
                    contentType = "image/png";
                }
                else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
                {
                    contentType = "image/jpeg";
                }
                else if (path.endsWith(".gif"))
                {
                    contentType = "image/gif";
                }
                else if (path.endsWith(".svg"))
                {
                    contentType = "image/svg+xml";
                }
                else if (path.endsWith(".pdf"))
                {
                    contentType = "application/pdf";
                    final String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                }
                else if (path.endsWith(".woff2"))
                {
                    contentType = "font/woff2";
                }
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                
                try (final OutputStream os = exchange.getResponseBody())
                {
                    os.write(bytes);
                }
            }
            catch (final Exception e)
            {
                LOG.error("Error serving resource: {}", resourcePath, e);
                final String response = "500 Internal Server Error";
                exchange.sendResponseHeaders(500, response.length());
                try (final OutputStream os = exchange.getResponseBody())
                {
                    os.write(response.getBytes());
                }
            }
        }
    }

    private static final class LoggingHandler implements HttpHandler
    {
        private final HttpHandler delegate;

        public LoggingHandler(final HttpHandler delegate)
        {
            this.delegate = delegate;
        }

        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            final long startTime = System.currentTimeMillis();
            final String method = exchange.getRequestMethod();
            final String uri = exchange.getRequestURI().toString();

            if (LOG.isDebugEnabled())
            {
                LOG.debug("--> Incoming Request: [{}] {}", method, uri);
                LOG.debug("Headers: {}", exchange.getRequestHeaders());
                final String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
                if (cookieHeader != null)
                {
                    LOG.debug("Cookies: {}", cookieHeader);
                }
            }

            try
            {
                delegate.handle(exchange);
            }
            catch (final Exception e)
            {
                LOG.error("Request failed: [{}] {}", method, uri, e);
                throw e;
            }
            finally
            {
                final int responseCode = exchange.getResponseCode();
                final long duration = System.currentTimeMillis() - startTime;
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("<-- Response Status: {} ({}ms)", responseCode, duration);
                    LOG.debug("Response Headers: {}", exchange.getResponseHeaders());
                }
                else
                {
                    LOG.info("[{}] {} -> {} ({}ms)", method, uri, responseCode, duration);
                }
            }
        }
    }

    /**
     * VÉRLA Handler: Handles e-commerce path routing, dynamic template rendering,
     * cookies, localization, HTMX infinite scroll page loading, auth, cart, checkout actions.
     */
    private static final class VerlaHandler implements HttpHandler
    {
        @Override
        public void handle(final HttpExchange exchange) throws IOException
        {
            final String requestMethod = exchange.getRequestMethod();
            final String fullPath = exchange.getRequestURI().getPath();
            
            if (LOG.isDebugEnabled())
            {
                LOG.debug("VerlaHandler: incoming SUT requestMethod='{}', fullPath='{}'", requestMethod, fullPath);
            }
            else
            {
                LOG.info("VerlaHandler request: {} {}", requestMethod, fullPath);
            }
            
            // Extract the SUT Quality Suffix (perfect, normal, bad)
            final String qualitySuffix;
            if (fullPath.startsWith("/verla-perfect/"))
            {
                qualitySuffix = "perfect";
            }
            else if (fullPath.startsWith("/verla-normal/"))
            {
                qualitySuffix = "normal";
            }
            else if (fullPath.startsWith("/verla-bad/"))
            {
                qualitySuffix = "bad";
            }
            else
            {
                sendResponse(exchange, 400, "text/plain", "Bad Request: Invalid SUT suffix");
                return;
            }

            final String contextPrefix = "/verla-" + qualitySuffix + "/";
            final String pagePath = fullPath.substring(contextPrefix.length());

            if (LOG.isDebugEnabled())
            {
                LOG.debug("VerlaHandler: SUT suffix='{}', resolved pagePath='{}'", qualitySuffix, pagePath);
            }

            // Extract cookies
            final Map<String, String> cookies = parseCookies(exchange.getRequestHeaders().getFirst("Cookie"));
            
            // Session cookie tracking
            String sessionId = cookies.get("verla_session_id");
            User currentUser = null;
            if (sessionId != null)
            {
                final String email = activeSessions.get(sessionId);
                if (email != null)
                {
                    currentUser = usersDb.get(email);
                }
            }

            // Cart cookie tracking
            String cartId = cookies.get("verla_cart_id");
            if (cartId == null)
            {
                cartId = UUID.randomUUID().toString();
                exchange.getResponseHeaders().add("Set-Cookie", "verla_cart_id=" + cartId + "; Path=/; HttpOnly");
            }
            final Cart cart = activeCarts.computeIfAbsent(cartId, k -> new Cart(k));

            // Selected Country tracking
            String countryCode = cookies.get("verla_country");
            if (countryCode == null)
            {
                countryCode = "US";
            }
            final Country activeCountry = countriesMap.getOrDefault(countryCode, countriesMap.get("US"));
            final String locale = activeCountry.locale;
            final Map<String, String> trans = catalogConfig.translations.getOrDefault(locale, catalogConfig.translations.get("en"));

            // Parse request body parameters
            final Map<String, String> params = parseRequestBody(exchange);

            // HTMX request header
            final boolean isHtmx = "true".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("HX-Request"));

            if (LOG.isDebugEnabled())
            {
                LOG.debug("Parsed Parameters: {}", params);
                LOG.debug("Is HTMX Request: {}", isHtmx);
            }

            // --- API Endpoint Routing ---
            if (pagePath.startsWith("api/"))
            {
                final String apiMethod = pagePath.substring(4);
                
                if ("country/select".equals(apiMethod))
                {
                    final String selectCode = params.getOrDefault("code", "US");
                    exchange.getResponseHeaders().add("Set-Cookie", "verla_country=" + selectCode + "; Path=/");
                    
                    // HX-Refresh reloads the current page so country switch is applied immediately
                    exchange.getResponseHeaders().add("HX-Refresh", "true");
                    sendResponse(exchange, 200, "text/html", "");
                    return;
                }
                else if ("countries".equals(apiMethod))
                {
                    final String q = exchange.getRequestURI().getQuery() != null ? getQueryParam(exchange.getRequestURI().getQuery(), "q").toLowerCase() : "";
                    final StringBuilder sb = new StringBuilder();
                    for (final Country c : catalogConfig.countries)
                    {
                        if (q.isEmpty() || c.name.toLowerCase().contains(q) || c.code.toLowerCase().contains(q))
                        {
                            final String selectedClass = c.code.equals(activeCountry.code) ? "selected" : "";
                            final String flag = getCountryFlag(c.code);
                            // Perfect vs Normal/Bad styling
                            if ("bad".equals(qualitySuffix))
                            {
                                sb.append("<div class=\"country-item ").append(selectedClass).append("\" style=\"padding:10px;cursor:pointer;\" onclick=\"document.cookie='verla_country=").append(c.code).append(";path=/';location.reload();\">")
                                  .append(c.name).append(" ").append(flag).append("</div>");
                            }
                            else
                            {
                                sb.append("<li class=\"country-item ").append(selectedClass).append("\" hx-get=\"api/country/select?code=").append(c.code).append("\">")
                                  .append(c.name).append(" ").append(flag).append("</li>");
                            }
                        }
                    }
                    sendResponse(exchange, 200, "text/html", sb.toString());
                    return;
                }
                else if ("address-form".equals(apiMethod))
                {
                    final String country = exchange.getRequestURI().getQuery() != null ? getQueryParam(exchange.getRequestURI().getQuery(), "country") : activeCountry.code;
                    final String prefix = params.getOrDefault("prefix", "shipping-");
                    sendResponse(exchange, 200, "text/html", getAddressFieldsHtml(country, locale, trans, prefix));
                    return;
                }
                else if ("cart/add".equals(apiMethod))
                {
                    String productId = params.get("productId");
                    final String size = params.get("size");
                    if (productId != null)
                    {
                        if (size != null && !size.isEmpty() && !productId.contains(":"))
                        {
                            productId = productId + ":" + size;
                        }
                        final int qty = Integer.parseInt(params.getOrDefault("quantity", "1"));
                        cart.items.merge(productId, qty, Integer::sum);
                    }
                    // Return cart badge count wrapper
                    sendResponse(exchange, 200, "text/html", getCartBadgeWrapperHtml(cart, trans, activeCountry, qualitySuffix, true));
                    return;
                }
                else if ("cart/update".equals(apiMethod))
                {
                    final String productId = params.get("productId");
                    final int qty = Integer.parseInt(params.getOrDefault("quantity", "0"));
                    if (productId != null)
                    {
                        if (qty <= 0)
                        {
                            cart.items.remove(productId);
                        }
                        else
                        {
                            cart.items.put(productId, qty);
                        }
                    }
                    sendResponse(exchange, 200, "text/html", getCartContentHtml(cart, activeCountry, trans, qualitySuffix));
                    return;
                }
                else if ("cart/remove".equals(apiMethod))
                {
                    final String productId = params.get("productId");
                    if (productId != null)
                    {
                        cart.items.remove(productId);
                    }
                    final String hxTarget = exchange.getRequestHeaders().getFirst("HX-Target");
                    if ("cart-btn-wrapper".equals(hxTarget))
                    {
                        sendResponse(exchange, 200, "text/html", getCartBadgeWrapperHtml(cart, trans, activeCountry, qualitySuffix));
                    }
                    else
                    {
                        sendResponse(exchange, 200, "text/html", getCartContentHtml(cart, activeCountry, trans, qualitySuffix));
                    }
                    return;
                }
                else if ("cart/coupon".equals(apiMethod))
                {
                    final String coupon = params.getOrDefault("couponCode", "").trim();
                    if (coupon.isEmpty())
                    {
                        cart.coupon = null;
                        sendResponse(exchange, 200, "text/html", getCartContentHtml(cart, activeCountry, trans, qualitySuffix));
                        return;
                    }
                    
                    final String lower = coupon.toLowerCase();
                    if ("10p-off".equals(lower) || "freegift".equals(lower) || "freeship".equals(lower) || "bogo".equals(lower))
                    {
                        cart.coupon = lower;
                        sendResponse(exchange, 200, "text/html", getCartContentHtml(cart, activeCountry, trans, qualitySuffix));
                    }
                    else
                    {
                        final String errorMsg = trans.getOrDefault("error", "Error") + ": This promo code is expired or invalid.";
                        sendResponse(exchange, 200, "text/html", getCartContentHtml(cart, activeCountry, trans, qualitySuffix, errorMsg));
                    }
                    return;
                }
                else if ("search/suggest".equals(apiMethod))
                {
                    final String query = params.getOrDefault("q", "").trim().toLowerCase();
                    if (query.isEmpty())
                    {
                        sendResponse(exchange, 200, "text/html", "");
                        return;
                    }

                    final List<Product> matches = new ArrayList<>();
                    for (final Product p : catalogProducts)
                    {
                        final String enName = p.names.getOrDefault("en", "").toLowerCase();
                        final String localName = p.names.getOrDefault(locale, "").toLowerCase();
                        if (enName.contains(query) || localName.contains(query) || p.id.toLowerCase().contains(query))
                        {
                            matches.add(p);
                        }
                    }

                    final StringBuilder sb = new StringBuilder();
                    final int limit = Math.min(6, matches.size());
                    for (int i = 0; i < limit; i++)
                    {
                        final Product p = matches.get(i);
                        final double price = p.salePrice != null ? p.salePrice : p.basePrice;
                        final String name = p.names.getOrDefault(locale, p.names.get("en"));
                        sb.append("<a href=\"p/").append(p.slug).append(".html\" class=\"search-suggestion-item\">")
                          .append("  <div class=\"search-suggestion-thumb\">")
                          .append("    <svg viewBox=\"0 0 100 100\">").append(p.svgPath).append("</svg>")
                          .append("  </div>")
                          .append("  <div class=\"search-suggestion-info\">")
                          .append("    <span class=\"search-suggestion-name\">").append(escapeHtml(name)).append("</span>")
                          .append("    <span class=\"search-suggestion-price\">").append(formatPrice(price, activeCountry)).append("</span>")
                          .append("  </div>")
                          .append("</a>");
                    }

                    if (matches.size() > 6)
                    {
                        final int extra = matches.size() - 6;
                        final String viewAllUrl = "plp.html?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                        sb.append("<a href=\"").append(viewAllUrl).append("\" class=\"search-more-link\">")
                          .append("+ ").append(extra).append(" more products. View all.")
                          .append("</a>");
                    }

                    sendResponse(exchange, 200, "text/html", sb.toString());
                    return;
                }
                else if ("auth/login".equals(apiMethod))
                {
                    final String email = params.getOrDefault("email", "").trim();
                    final String password = params.getOrDefault("password", "");
                    
                    final User user = usersDb.get(email);
                    if (user != null && user.password.equals(password))
                    {
                        final String newSessionId = UUID.randomUUID().toString();
                        activeSessions.put(newSessionId, email);
                        exchange.getResponseHeaders().add("Set-Cookie", "verla_session_id=" + newSessionId + "; Path=/; HttpOnly");
                        
                        exchange.getResponseHeaders().add("HX-Redirect", "/verla-" + qualitySuffix + "/account.html");
                        sendResponse(exchange, 200, "text/html", "");
                    }
                    else
                    {
                        final Map<String, String> model = new HashMap<>();
                        model.put("auth_error", "Invalid email or password.");
                        model.put("input_email", escapeHtml(email));
                        sendResponse(exchange, 400, "text/html", renderTemplate(qualitySuffix, "login.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                    }
                    return;
                }
                else if ("auth/register".equals(apiMethod))
                {
                    if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod()))
                    {
                        sendResponse(exchange, 405, "text/plain", "Method Not Allowed: Registration requires PUT");
                        return;
                    }
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("Processing registration: email={}", params.get("email"));
                    }
                    final String email = params.getOrDefault("email", "").trim();
                    final String password = params.getOrDefault("password", "");
                    final String confirmPassword = params.getOrDefault("confirmPassword", "");
                    
                    final Map<String, String> model = new HashMap<>();
                    model.put("input_email", escapeHtml(email));
                    
                    if (email.isEmpty() || !email.contains("@"))
                    {
                        model.put("err_email", "Please enter a valid email address.");
                        sendResponse(exchange, 400, "text/html", renderTemplate(qualitySuffix, "register.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                        return;
                    }
                    if (password.length() < 6)
                    {
                        model.put("err_password", "Password must be at least 6 characters.");
                        sendResponse(exchange, 400, "text/html", renderTemplate(qualitySuffix, "register.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                        return;
                    }
                    if (!password.equals(confirmPassword))
                    {
                        model.put("err_confirmPassword", "Passwords do not match.");
                        sendResponse(exchange, 400, "text/html", renderTemplate(qualitySuffix, "register.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                        return;
                    }
                    if (usersDb.containsKey(email))
                    {
                        model.put("auth_error", "An account with this email already exists.");
                        sendResponse(exchange, 400, "text/html", renderTemplate(qualitySuffix, "register.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                        return;
                    }
                    
                    final User newUser = new User(email, password);
                    usersDb.put(email, newUser);
                    
                    final String newSessionId = UUID.randomUUID().toString();
                    activeSessions.put(newSessionId, email);
                    exchange.getResponseHeaders().add("Set-Cookie", "verla_session_id=" + newSessionId + "; Path=/; HttpOnly");
                    
                    exchange.getResponseHeaders().add("HX-Redirect", "/verla-" + qualitySuffix + "/account.html");
                    sendResponse(exchange, 200, "text/html", "");
                    return;
                }
                else if ("auth/change-password".equals(apiMethod))
                {
                    final String current = params.get("currentPassword");
                    final String newPass = params.get("newPassword");
                    final String confirm = params.get("confirmNewPassword");
                    
                    final Map<String, String> model = new HashMap<>();
                    if (currentUser == null)
                    {
                        exchange.getResponseHeaders().add("HX-Redirect", "/verla-" + qualitySuffix + "/login.html");
                        sendResponse(exchange, 200, "text/html", "");
                        return;
                    }
                    if (!currentUser.password.equals(current))
                    {
                        model.put("password_error", "Current password is incorrect.");
                    }
                    else if (newPass.length() < 6)
                    {
                        model.put("password_error", "New password must be at least 6 characters.");
                    }
                    else if (!newPass.equals(confirm))
                    {
                        model.put("password_error", "Passwords do not match.");
                    }
                    else
                    {
                        currentUser.password = newPass;
                        model.put("password_status", "Password updated successfully!");
                    }
                    sendResponse(exchange, 200, "text/html", renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                    return;
                }
                else if ("auth/address/add".equals(apiMethod))
                {
                    final Map<String, String> model = new HashMap<>();
                    if (currentUser != null)
                    {
                        final String street = params.get("street");
                        final String city = params.get("city");
                        final String state = params.getOrDefault("state", "");
                        final String postcode = params.get("postcode");
                        final String country = params.get("country");
                        final String id = "addr-" + UUID.randomUUID().toString().substring(0, 8);
                        
                        currentUser.addresses.add(new Address(id, street, city, state, postcode, country));
                    }
                    sendResponse(exchange, 200, "text/html", renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                    return;
                }
                else if ("auth/address/delete".equals(apiMethod))
                {
                    final Map<String, String> model = new HashMap<>();
                    final String id = params.get("id");
                    if (currentUser != null && id != null)
                    {
                        currentUser.addresses.removeIf(a -> a.id.equals(id));
                    }
                    sendResponse(exchange, 200, "text/html", renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                    return;
                }
                else if ("auth/card/add".equals(apiMethod))
                {
                    final Map<String, String> model = new HashMap<>();
                    if (currentUser != null)
                    {
                        final String number = params.get("cardNumber");
                        final String type = params.get("cardType");
                        final String expiry = params.get("cardExpiry");
                        final String cvv = params.get("cardCvv");
                        final String id = "card-" + UUID.randomUUID().toString().substring(0, 8);
                        
                        currentUser.cards.add(new Card(id, number, type, expiry, cvv));
                    }
                    sendResponse(exchange, 200, "text/html", renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                    return;
                }
                else if ("auth/card/delete".equals(apiMethod))
                {
                    final Map<String, String> model = new HashMap<>();
                    final String id = params.get("id");
                    if (currentUser != null && id != null)
                    {
                        currentUser.cards.removeIf(c -> c.id.equals(id));
                    }
                    sendResponse(exchange, 200, "text/html", renderTemplate(qualitySuffix, "account.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                    return;
                }
                else if ("checkout/purchase".equals(apiMethod))
                {
                    // Simulate server response time of 1 to 4 seconds
                    try
                    {
                        final long delay = 1000 + (long) (Math.random() * 3000);
                        Thread.sleep(delay);
                    }
                    catch (final InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }

                    // Checkout processing logic
                    final String firstName = params.getOrDefault("firstName", "").trim();
                    final String lastName = params.getOrDefault("lastName", "").trim();
                    final String email = params.getOrDefault("email", "").trim();
                    final String cardType = params.getOrDefault("cardType", "");
                    final String cardNumber = params.getOrDefault("cardNumber", "").trim().replace(" ", "");
                    final String cardExpiry = params.getOrDefault("cardExpiry", "");
                    final String cardCvv = params.getOrDefault("cardCvv", "");
                    
                    final String street = params.get("street");
                    final String city = params.get("city");
                    final String postcode = params.get("postcode");
                    final String country = params.get("country");
                    final String state = params.getOrDefault("state", "");

                    final Map<String, String> model = new HashMap<>();
                    model.put("input_firstName", escapeHtml(firstName));
                    model.put("input_lastName", escapeHtml(lastName));
                    model.put("input_email", escapeHtml(email));
                    model.put("input_cardNumber", escapeHtml(cardNumber));
                    model.put("input_cardExpiry", escapeHtml(cardExpiry));
                    model.put("input_cardCvv", escapeHtml(cardCvv));

                    boolean hasErrors = false;
                    if (firstName.isEmpty()) { model.put("err_firstName", "First name is required."); hasErrors = true; }
                    if (lastName.isEmpty()) { model.put("err_lastName", "Last name is required."); hasErrors = true; }
                    if (email.isEmpty() || !email.contains("@")) { model.put("err_email", "Valid email is required."); hasErrors = true; }
                    
                    // Card payment validation
                    if (cardNumber.isEmpty())
                    {
                        model.put("err_cardNumber", "Card number is required.");
                        hasErrors = true;
                    }
                    else if (cardNumber.endsWith("200"))
                    {
                        model.put("err_cardNumber", trans.getOrDefault("error", "Error") + ": Card declined by provider.");
                        hasErrors = true;
                    }
                    else if (!cardNumber.matches("\\d+"))
                    {
                        model.put("err_cardNumber", "Card number must contain digits only.");
                        hasErrors = true;
                    }

                    if (cardExpiry.isEmpty()) { model.put("err_cardExpiry", "Expiry date is required."); hasErrors = true; }
                    if (cardCvv.isEmpty() || !cardCvv.matches("\\d{3,4}")) { model.put("err_cardCvv", "CVV is invalid."); hasErrors = true; }

                    if (hasErrors)
                    {
                        sendResponse(exchange, 400, "text/html", renderTemplate(qualitySuffix, "checkout.html", model, activeCountry, trans, currentUser, cart, "", isHtmx));
                        return;
                    }

                    // Success!
                    final double subtotal = calculateSubtotal(cart);
                    final double discount = calculateDiscount(cart, subtotal);
                    final double shipping = calculateShipping(cart, subtotal);
                    final double tax = Math.round((subtotal - discount) * 0.1 * 100.0) / 100.0;
                    final double total = subtotal - discount + shipping + tax;
                    
                    final String orderNum = "V-" + (int)(Math.random()*900000 + 100000) + "-" + activeCountry.code;
                    final Address shippingAddr = new Address("addr-order", street, city, state, postcode, country);
                    final Card paymentCard = new Card("card-order", cardNumber, cardType, cardExpiry, cardCvv);
                    final boolean giftAdded = "freegift".equals(cart.coupon);

                    final List<OrderItem> items = new ArrayList<>();
                    for (final Map.Entry<String, Integer> entry : cart.items.entrySet())
                    {
                        final String cartKey = entry.getKey();
                        final String prodId;
                        final String size;
                        if (cartKey.contains(":"))
                        {
                            final String[] parts = cartKey.split(":");
                            prodId = parts[0];
                            size = parts[1];
                        }
                        else
                        {
                            prodId = cartKey;
                            size = "";
                        }

                        final Product prod = lookupProductById(prodId);
                        if (prod != null)
                        {
                            final double itemPrice = prod.salePrice != null ? prod.salePrice : prod.basePrice;
                            items.add(new OrderItem(prod.id, prod.names.getOrDefault(locale, prod.names.get("en")), entry.getValue(), itemPrice));

                            final Map<String, Integer> stockMap = productInventory.get(prodId);
                            if (stockMap != null)
                            {
                                final int currentStock = stockMap.getOrDefault(size, 0);
                                final int qtyBought = entry.getValue();
                                final int newStock = Math.max(0, currentStock - qtyBought);
                                stockMap.put(size, newStock);
                            }
                        }
                    }

                    final Order order = new Order(orderNum, email, items, subtotal, shipping, tax, total, "Processing", shippingAddr, paymentCard, cart.coupon, giftAdded);
                    ordersDb.put(orderNum, order);

                    // Add to user orders list if logged in
                    if (currentUser != null)
                    {
                        currentUser.orders.add(order);
                    }

                    // Clear shopping cart
                    cart.items.clear();
                    cart.coupon = null;

                    // Display confirmation fragment
                    final String successHtml = "<div style=\"text-align:center; padding: 40px 20px;\">" +
                                               "  <div style=\"font-size: 64px; color: var(--color-success); margin-bottom: 20px;\">✓</div>" +
                                               "  <h2 style=\"font-family: var(--font-family-serif); font-size: 28px; margin-bottom: 12px;\">Thank you for your purchase!</h2>" +
                                               "  <p style=\"color: var(--color-text-secondary); margin-bottom: 24px;\">Your order has been placed successfully.</p>" +
                                               "  <div style=\"background-color: var(--color-bg-secondary); border: 1px solid var(--color-border); padding: 24px; border-radius: var(--border-radius); text-align: left; max-width: 480px; margin: 0 auto 30px auto;\">" +
                                               "    <div style=\"margin-bottom:10px;\"><strong>Order Number:</strong> <span id=\"order-number-value\">" + orderNum + "</span></div>" +
                                               "    <div style=\"margin-bottom:10px;\"><strong>Shipping Zip Code:</strong> <span id=\"zip-code-value\">" + postcode + "</span></div>" +
                                               "    <div style=\"margin-bottom:10px;\"><strong>Total Paid:</strong> " + formatPrice(total, activeCountry) + "</div>" +
                                               "    <div style=\"font-size: 12px; color: var(--color-text-secondary); margin-top: 16px;\">Use the Order Number and Shipping Zip Code to track your package on the <a href=\"track-orders.html\" style=\"color: var(--color-accent); font-weight: 600;\">Track Orders</a> page.</div>" +
                                               "  </div>" +
                                               "  <a href=\"index.html\" class=\"btn-primary\" style=\"display:inline-block;\">Continue Shopping</a>" +
                                               "</div>";
                    sendResponse(exchange, 200, "text/html", successHtml);
                    return;
                }
                else if ("order/lookup".equals(apiMethod))
                {
                    final String orderNumber = params.getOrDefault("orderNumber", "").trim();
                    final String zipCode = params.getOrDefault("zipCode", "").trim();
                    
                    final Order order = ordersDb.get(orderNumber);
                    if (order != null && order.shippingAddress.postcode.equalsIgnoreCase(zipCode))
                    {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("<div style=\"background-color: var(--color-bg-primary); border: 1px solid var(--color-border); padding: 24px; border-radius: var(--border-radius); margin-top: 20px;\">")
                          .append("  <h4 style=\"font-family: var(--font-family-serif); font-size: 18px; margin-bottom: 16px;\">Order ").append(order.orderNumber).append("</h4>")
                          .append("  <div style=\"margin-bottom: 12px;\"><strong>Status:</strong> <span style=\"color: var(--color-success); font-weight: 600;\">").append(order.status).append("</span></div>")
                          .append("  <div style=\"margin-bottom: 12px;\"><strong>Recipient:</strong> ").append(order.shippingAddress.street).append(", ").append(order.shippingAddress.city).append("</div>")
                          .append("  <h5 style=\"font-size: 13px; font-weight: 600; text-transform: uppercase; margin-bottom: 10px;\">Items</h5>")
                          .append("  <ul style=\"list-style: none; display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px;\">");
                        
                        for (final OrderItem item : order.items)
                        {
                            sb.append("    <li style=\"display:flex; justify-content: space-between; font-size: 13px;\">")
                              .append("      <span>").append(item.productName).append(" &times; ").append(item.quantity).append("</span>")
                              .append("      <span>").append(formatPrice(item.price * item.quantity, activeCountry)).append("</span>")
                              .append("    </li>");
                        }
                        if (order.freeGiftAdded)
                        {
                            sb.append("    <li style=\"display:flex; justify-content: space-between; font-size: 13px; color: var(--color-success); font-weight: 500;\">")
                              .append("      <span>VÉRLA Signature Tote Bag &times; 1</span>")
                              .append("      <span>Free Gift</span>")
                              .append("    </li>");
                        }
                        sb.append("  </ul>")
                          .append("  <div style=\"display: flex; justify-content: space-between; font-weight: 600; border-top: 1px solid var(--color-border); padding-top: 12px;\">")
                          .append("    <span>Total</span>")
                          .append("    <span>").append(formatPrice(order.total, activeCountry)).append("</span>")
                          .append("  </div>")
                          .append("</div>");
                        sendResponse(exchange, 200, "text/html", sb.toString());
                    }
                    else
                    {
                        sendResponse(exchange, 200, "text/html", "<div class=\"error-message\" style=\"margin-top: 20px; text-align: center;\">Order not found or invalid Zip Code.</div>");
                    }
                    return;
                }
                else if ("auth/logout".equals(apiMethod))
                {
                    if (sessionId != null)
                    {
                        activeSessions.remove(sessionId);
                    }
                    exchange.getResponseHeaders().add("Set-Cookie", "verla_session_id=; Path=/; Max-Age=0");
                    exchange.getResponseHeaders().add("Location", "/verla-" + qualitySuffix + "/index.html");
                    sendResponse(exchange, 302, "text/plain", "");
                    return;
                }
            }

            // --- storefront dynamic pages (full or fragments) ---

            final String pageResource;
            if (pagePath.isEmpty() || "index.html".equals(pagePath))
            {
                pageResource = "index.html";
            }
            else if (pagePath.startsWith("p/"))
            {
                // PDP: /p/slug.html
                pageResource = "pdp.html";
            }
            else if (pagePath.startsWith("c/"))
            {
                // PLP: /c/category.html
                pageResource = "plp.html";
            }
            else
            {
                pageResource = pagePath;
            }

            final String templateHtml = renderTemplate(qualitySuffix, pageResource, new HashMap<>(), activeCountry, trans, currentUser, cart, exchange.getRequestURI().toString(), isHtmx);
            if (templateHtml == null)
            {
                // Try serving it as a standard static resource from SUT folders
                new ResourceHandler().handle(exchange);
                return;
            }

            sendResponse(exchange, 200, "text/html; charset=UTF-8", templateHtml);
        }
    }

    private static String renderTemplate(final String quality, final String pageName, final Map<String, String> customModel,
                                         final Country country, final Map<String, String> trans, final User user,
                                         final Cart cart, final String requestUri, final boolean isHtmx)
    {
        try
        {
            // Resolve fullPath (path portion only) and keep it for template matching
            final String fullPath;
            final int qIdx = requestUri.indexOf("?");
            if (qIdx != -1)
            {
                fullPath = requestUri.substring(0, qIdx);
            }
            else
            {
                fullPath = requestUri;
            }

            // Load specific page template
            final String pageResPath = "ai-test-pages/verla-" + quality + "/" + pageName;
            
            // Template loading log removed as per request
            
            final String rawBody = readResource(pageResPath);
            if (rawBody == null)
            {
                return null;
            }

            // Setup variables model mapping
            final Map<String, String> model = new HashMap<>(customModel);
            model.put("quality", quality);

            // Generic translations mappings
            for (final Map.Entry<String, String> entry : trans.entrySet())
            {
                final String val = entry.getValue();
                final String resolvedVal = val != null ? val.replace("${country_symbol}", country.symbol) : "";
                model.put("lang_" + entry.getKey(), resolvedVal);
            }

            model.put("country_name", country.name);
            model.put("country_symbol", country.symbol);
            model.put("country_flag", getCountryFlag(country.code));
            model.put("cart_count", String.valueOf(cart.items.values().stream().mapToInt(Integer::intValue).sum()));
            model.put("cart_badge_html", getCartBadgeWrapperHtml(cart, trans, country, quality));

            // User navigation status HTML snippet
            if (user != null)
            {
                // Perfect vs normal/bad style
                if ("bad".equals(quality))
                {
                    model.put("user_nav_status", "<div onclick=\"location.href='/verla-bad/account.html'\" style=\"cursor:pointer;\"><svg class=\"icon-svg\" viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2\"></path><circle cx=\"12\" cy=\"7\" r=\"4\"></circle></svg> " + user.email.split("@")[0] + "</div>");
                }
                else
                {
                    model.put("user_nav_status", "<a href=\"/verla-" + quality + "/account.html\" class=\"utility-btn\"><svg class=\"icon-svg\" viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2\"></path><circle cx=\"12\" cy=\"7\" r=\"4\"></circle></svg> Account</a>");
                }
            }
            else
            {
                if ("bad".equals(quality))
                {
                    model.put("user_nav_status", "<div onclick=\"location.href='/verla-bad/login.html'\" style=\"cursor:pointer;\"><svg class=\"icon-svg\" viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2\"></path><circle cx=\"12\" cy=\"7\" r=\"4\"></circle></svg> Login</div>");
                }
                else
                {
                    model.put("user_nav_status", "<a href=\"/verla-" + quality + "/login.html\" class=\"utility-btn\"><svg class=\"icon-svg\" viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2\"></path><circle cx=\"12\" cy=\"7\" r=\"4\"></circle></svg> Login</a>");
                }
            }

            // Fill page-specific model variables
            if ("index.html".equals(pageName))
            {
                final StringBuilder sb = new StringBuilder();
                // Select first 8 products as featured list
                for (int i = 0; i < 8 && i < catalogProducts.size(); i++)
                {
                    sb.append(renderProductCard(catalogProducts.get(i), country, trans, quality));
                }
                model.put("featured_products", sb.toString());
                model.put("search_query", "");
            }
            else if ("plp.html".equals(pageName))
            {
                final String q = getQueryParam(requestUri, "q").toLowerCase();
                final String pageStr = getQueryParam(requestUri, "page");
                final int page = pageStr.isEmpty() ? 1 : Integer.parseInt(pageStr);

                final String category;
                if (fullPath.contains("/c/"))
                {
                    final String catPart = fullPath.substring(fullPath.lastIndexOf("/c/") + 3);
                    category = catPart.contains(".html") ? catPart.substring(0, catPart.indexOf(".html")) : catPart;
                }
                else
                {
                    category = getQueryParam(requestUri, "category");
                }

                final String subcat = getQueryParam(requestUri, "subcategory");
                final String sort = getQueryParam(requestUri, "sort");

                final List<String> activeColors = getQueryParamValues(requestUri, "color");
                final List<String> activePrices = getQueryParamValues(requestUri, "price");
                final List<String> activeSales = getQueryParamValues(requestUri, "sale");

                // Filter & Sort products list
                List<Product> filtered = catalogProducts.stream().filter(p -> {
                    if (!category.isEmpty() && !p.category.equalsIgnoreCase(category)) return false;
                    if (!subcat.isEmpty() && !p.subcategory.equalsIgnoreCase(subcat)) return false;
                    if (!q.isEmpty() && !p.names.get("en").toLowerCase().contains(q) &&
                        !p.descriptions.get("en").toLowerCase().contains(q)) return false;

                    // Color filter
                    if (!activeColors.isEmpty())
                    {
                        boolean colorMatch = false;
                        for (final String col : activeColors)
                        {
                            if (p.color != null && p.color.equalsIgnoreCase(col))
                            {
                                colorMatch = true;
                                break;
                            }
                        }
                        if (!colorMatch) return false;
                    }

                    // Price filter
                    if (!activePrices.isEmpty())
                    {
                        final double price = p.salePrice != null ? p.salePrice : p.basePrice;
                        boolean priceMatch = false;
                        for (final String pr : activePrices)
                        {
                            if ("0-50".equals(pr) && price < 50.0) priceMatch = true;
                            else if ("50-100".equals(pr) && price >= 50.0 && price <= 100.0) priceMatch = true;
                            else if ("100-200".equals(pr) && price > 100.0) priceMatch = true;
                        }
                        if (!priceMatch) return false;
                    }

                    // Sale filter
                    if (!activeSales.isEmpty() && activeSales.contains("true"))
                    {
                        if (p.salePrice == null) return false;
                    }

                    return true;
                }).collect(Collectors.toList());

                if ("price-asc".equals(sort))
                {
                    filtered.sort((p1, p2) -> Double.compare(p1.salePrice != null ? p1.salePrice : p1.basePrice, p2.salePrice != null ? p2.salePrice : p2.basePrice));
                }
                else if ("price-desc".equals(sort))
                {
                    filtered.sort((p1, p2) -> Double.compare(p2.salePrice != null ? p2.salePrice : p2.basePrice, p1.salePrice != null ? p1.salePrice : p1.basePrice));
                }

                model.put("total_products_count", String.valueOf(filtered.size()));
                model.put("plp_heading", category.isEmpty() ? (q.isEmpty() ? trans.getOrDefault("all", "All Products") : trans.getOrDefault("search", "Search") + ": \"" + q + "\"") : trans.getOrDefault(category, category));
                model.put("search_query", escapeHtml(q));

                // Bind checked checkbox states
                model.put("color_off_white_checked", activeColors.contains("off-white") ? "checked" : "");
                model.put("color_charcoal_checked", activeColors.contains("charcoal") ? "checked" : "");
                model.put("color_olive_checked", activeColors.contains("olive") ? "checked" : "");
                model.put("color_sand_checked", activeColors.contains("sand") ? "checked" : "");
                model.put("color_navy_checked", activeColors.contains("navy") ? "checked" : "");
                model.put("color_taupe_checked", activeColors.contains("taupe") ? "checked" : "");
                model.put("color_sage_checked", activeColors.contains("sage") ? "checked" : "");
                model.put("color_terracotta_checked", activeColors.contains("terracotta") ? "checked" : "");

                model.put("price_0_50_checked", activePrices.contains("0-50") ? "checked" : "");
                model.put("price_50_100_checked", activePrices.contains("50-100") ? "checked" : "");
                model.put("price_100_200_checked", activePrices.contains("100-200") ? "checked" : "");

                model.put("sale_checked", activeSales.contains("true") ? "checked" : "");

                // Nav sidebar highlight logic
                model.put("all_active_weight", category.isEmpty() ? "600" : "400");
                model.put("all_active_color", category.isEmpty() ? "var(--color-accent)" : "var(--color-text-secondary)");
                model.put("tops_active_weight", "tops".equals(category) ? "600" : "400");
                model.put("tops_active_color", "tops".equals(category) ? "var(--color-accent)" : "var(--color-text-secondary)");
                model.put("bottoms_active_weight", "bottoms".equals(category) ? "600" : "400");
                model.put("bottoms_active_color", "bottoms".equals(category) ? "var(--color-accent)" : "var(--color-text-secondary)");
                model.put("outerwear_active_weight", "outerwear".equals(category) ? "600" : "400");
                model.put("outerwear_active_color", "outerwear".equals(category) ? "var(--color-accent)" : "var(--color-text-secondary)");
                model.put("footwear_active_weight", "footwear".equals(category) ? "600" : "400");
                model.put("footwear_active_color", "footwear".equals(category) ? "var(--color-accent)" : "var(--color-text-secondary)");
                model.put("accessories_active_weight", "accessories".equals(category) ? "600" : "400");
                model.put("accessories_active_color", "accessories".equals(category) ? "var(--color-accent)" : "var(--color-text-secondary)");

                model.put("sort_default_selected", sort.isEmpty() || "default".equals(sort) ? "selected" : "");
                model.put("sort_asc_selected", "price-asc".equals(sort) ? "selected" : "");
                model.put("sort_desc_selected", "price-desc".equals(sort) ? "selected" : "");

                final String filterUrl = fullPath.contains("?") ? fullPath.substring(0, fullPath.indexOf("?")) : fullPath;
                model.put("filter_url", filterUrl);

                // Paginate products: size 12
                final int pageSize = 12;
                final int fromIndex = (page - 1) * pageSize;
                final int toIndex = Math.min(fromIndex + pageSize, filtered.size());

                final StringBuilder sb = new StringBuilder();
                if (fromIndex < filtered.size())
                {
                    final List<Product> paginated = filtered.subList(fromIndex, toIndex);
                    for (int j = 0; j < paginated.size(); j++)
                    {
                        final Product p = paginated.get(j);
                        final boolean isLast = (j == paginated.size() - 1) && (toIndex < filtered.size());
                        if (isLast)
                        {
                            // Build nextUrl with active refinements appended
                            final StringBuilder filterParams = new StringBuilder();
                            for (final String col : activeColors)
                            {
                                try
                                {
                                    filterParams.append("&color=").append(URLEncoder.encode(col, StandardCharsets.UTF_8.name()));
                                }
                                catch (final Exception e)
                                {
                                    filterParams.append("&color=").append(col);
                                }
                            }
                            for (final String pr : activePrices)
                            {
                                try
                                {
                                    filterParams.append("&price=").append(URLEncoder.encode(pr, StandardCharsets.UTF_8.name()));
                                }
                                catch (final Exception e)
                                {
                                    filterParams.append("&price=").append(pr);
                                }
                            }
                            for (final String sl : activeSales)
                            {
                                try
                                {
                                    filterParams.append("&sale=").append(URLEncoder.encode(sl, StandardCharsets.UTF_8.name()));
                                }
                                catch (final Exception e)
                                {
                                    filterParams.append("&sale=").append(sl);
                                }
                            }

                            final String nextUrl = filterUrl + "?page=" + (page + 1) + 
                                                   (category.isEmpty() ? "" : "&category=" + category) +
                                                   (subcat.isEmpty() ? "" : "&subcategory=" + subcat) +
                                                   (sort.isEmpty() ? "" : "&sort=" + sort) +
                                                   (q.isEmpty() ? "" : "&q=" + q) +
                                                   filterParams.toString();
                            sb.append(renderProductCardInfinite(p, country, trans, quality, nextUrl));
                        }
                        else
                        {
                            sb.append(renderProductCard(p, country, trans, quality));
                        }
                    }
                }
                model.put("plp_products", sb.toString());
            }
            else if ("pdp.html".equals(pageName))
            {
                // Dynamic SKU product detail mapping
                final String slug = fullPath.substring(fullPath.lastIndexOf("/") + 1).replace(".html", "");
                final Product prod = lookupProductBySlug(slug);
                if (prod != null)
                {
                    model.put("product_name", prod.names.getOrDefault(country.locale, prod.names.get("en")));
                    model.put("product_id", prod.id);
                    model.put("product_category", prod.category);
                    model.put("product_category_name", trans.getOrDefault(prod.category, prod.category));
                    model.put("product_subcategory_name", prod.subcategory.toUpperCase());
                    
                    final double price = prod.salePrice != null ? prod.salePrice : prod.basePrice;
                    model.put("product_price_html", formatPrice(price, country));
                    model.put("product_desc", prod.descriptions.getOrDefault(country.locale, prod.descriptions.get("en")));
                    model.put("product_svg_content", prod.svgPath);
                    model.put("product_badge_html", prod.badge.isEmpty() ? "" : "<span class=\"product-badge\">" + prod.badge.toUpperCase() + "</span>");
                    model.put("product_sizes_html", getProductSizesSelectHtml(prod.id, prod.category));
                }
                else
                {
                    return "404 Product Not Found";
                }
            }
            else if ("cart.html".equals(pageName))
            {
                model.put("cart_content_html", getCartContentHtml(cart, country, trans, quality));
            }
            else if ("checkout.html".equals(pageName))
            {
                model.put("address_fields_html", getAddressFieldsHtml(country.code, country.locale, trans, "shipping-"));
                
                final StringBuilder options = new StringBuilder();
                for (final Country c : catalogConfig.countries)
                {
                    final String sel = c.code.equals(country.code) ? "selected" : "";
                    options.append("<option value=\"").append(c.code).append("\" ").append(sel).append(">")
                           .append(c.name).append(" (").append(c.symbol).append(")</option>");
                }
                model.put("country_options_html", options.toString());

                // Order subtotal calculation
                final double subtotal = calculateSubtotal(cart);
                final double discount = calculateDiscount(cart, subtotal);
                final double shipping = calculateShipping(cart, subtotal);
                final double tax = Math.round((subtotal - discount) * 0.1 * 100.0) / 100.0;
                final double total = subtotal - discount + shipping + tax;

                model.put("checkout_subtotal", formatPrice(subtotal, country));
                model.put("checkout_shipping", formatPrice(shipping, country));
                model.put("checkout_tax", formatPrice(tax, country));
                model.put("checkout_total", formatPrice(total, country));

                if (discount > 0)
                {
                    model.put("checkout_discount_row", "<div style=\"display:flex;justify-content:space-between;color:var(--color-success);\">" +
                              "  <span>Discount (" + cart.coupon.toUpperCase() + ")</span>" +
                              "  <span>-" + formatPrice(discount, country) + "</span>" +
                              "</div>");
                }
                else
                {
                    model.put("checkout_discount_row", "");
                }

                final StringBuilder itemsSum = new StringBuilder();
                for (final Map.Entry<String, Integer> entry : cart.items.entrySet())
                {
                    final Product p = lookupProductById(entry.getKey());
                    if (p != null)
                    {
                        final String[] parts = entry.getKey().split(":");
                        final String size = parts.length > 1 ? " (" + parts[1] + ")" : "";
                        final double price = p.salePrice != null ? p.salePrice : p.basePrice;
                        itemsSum.append("<div style=\"display:flex;justify-content:space-between;font-size:13px;margin-bottom:8px;\">")
                                .append("  <span>").append(p.names.getOrDefault(country.locale, p.names.get("en"))).append(size).append(" &times; ").append(entry.getValue()).append("</span>")
                                .append("  <span>").append(formatPrice(price * entry.getValue(), country)).append("</span>")
                                .append("</div>");
                    }
                }
                if ("freegift".equals(cart.coupon))
                {
                    itemsSum.append("<div style=\"display:flex;justify-content:space-between;font-size:13px;margin-bottom:8px;color:var(--color-success);\">")
                            .append("  <span>VÉRLA Signature Tote Bag &times; 1</span>")
                            .append("  <span>Free Gift</span>")
                            .append("</div>");
                }
                model.put("checkout_items_summary", itemsSum.toString());

                // Autofill forms values from logged in user if empty
                if (user != null)
                {
                    model.putIfAbsent("input_firstName", "");
                    model.putIfAbsent("input_lastName", "");
                    model.putIfAbsent("input_email", user.email);
                }

                model.putIfAbsent("input_firstName", "");
                model.putIfAbsent("input_lastName", "");
                model.putIfAbsent("input_email", "");
                model.putIfAbsent("input_cardNumber", "");
                model.putIfAbsent("input_cardExpiry", "");
                model.putIfAbsent("input_cardCvv", "");

                model.putIfAbsent("err_firstName", "");
                model.putIfAbsent("err_lastName", "");
                model.putIfAbsent("err_email", "");
                model.putIfAbsent("err_cardNumber", "");
                model.putIfAbsent("err_cardExpiry", "");
                model.putIfAbsent("err_cardCvv", "");

                model.put("card_visa_selected", "");
                model.put("card_mastercard_selected", "");
                model.put("card_amex_selected", "");
            }
            else if ("account.html".equals(pageName))
            {
                if (user == null)
                {
                    return "<script>window.location='login.html'</script>";
                }
                
                // Account Orders History list
                final StringBuilder ordersList = new StringBuilder();
                if (user.orders.isEmpty())
                {
                    ordersList.append("<p style=\"color:var(--color-text-secondary);\">").append(trans.getOrDefault("noOrders", "No orders found.")).append("</p>");
                }
                else
                {
                    for (final Order order : user.orders)
                    {
                        ordersList.append("<div style=\"border:1px solid var(--color-border);padding:16px;border-radius:var(--border-radius);background:var(--color-bg-secondary);\">")
                                  .append("  <div style=\"display:flex;justify-content:space-between;font-weight:600;margin-bottom:10px;\">")
                                  .append("    <span>").append(order.orderNumber).append("</span>")
                                  .append("    <span>").append(formatPrice(order.total, country)).append("</span>")
                                  .append("  </div>")
                                  .append("  <div style=\"font-size:12px;color:var(--color-text-secondary);\">")
                                  .append("    Status: <span style=\"color:var(--color-success);font-weight:500;\">").append(order.status).append("</span> &middot; ")
                                  .append("    Shipped to: ").append(order.shippingAddress.street).append(", ").append(order.shippingAddress.city)
                                  .append("  </div>")
                                  .append("</div>");
                    }
                }
                model.put("account_orders_html", ordersList.toString());

                // Saved Addresses book
                final StringBuilder addressList = new StringBuilder();
                if (user.addresses.isEmpty())
                {
                    addressList.append("<p style=\"color:var(--color-text-secondary);\">").append(trans.getOrDefault("noAddresses", "No addresses found.")).append("</p>");
                }
                else
                {
                    for (final Address addr : user.addresses)
                    {
                        addressList.append("<div style=\"border:1px solid var(--color-border);padding:16px;border-radius:var(--border-radius);\">")
                                  .append("  <div style=\"font-weight:600;margin-bottom:4px;\">").append(addr.street).append("</div>")
                                  .append("  <div style=\"font-size:13px;color:var(--color-text-secondary);margin-bottom:12px;\">")
                                  .append(addr.city).append(", ").append(addr.state).append(" ").append(addr.postcode).append(" &middot; ").append(addr.country)
                                  .append("  </div>")
                                  .append("  <button hx-post=\"api/auth/address/delete?id=").append(addr.id).append("\" hx-target=\"#account-page-container\" hx-swap=\"outerHTML\" style=\"color:var(--color-error);font-size:12px;font-weight:500;\">").append(trans.getOrDefault("delete", "Delete")).append("</button>")
                                  .append("</div>");
                    }
                }
                model.put("account_addresses_html", addressList.toString());

                // Saved Betalkort cards
                final StringBuilder cardList = new StringBuilder();
                if (user.cards.isEmpty())
                {
                    cardList.append("<p style=\"color:var(--color-text-secondary);\">").append(trans.getOrDefault("noCards", "No cards found.")).append("</p>");
                }
                else
                {
                    for (final Card card : user.cards)
                    {
                        final String masked = "**** **** **** " + card.number.substring(Math.max(0, card.number.length() - 4));
                        cardList.append("<div style=\"border:1px solid var(--color-border);padding:16px;border-radius:var(--border-radius);\">")
                                .append("  <div style=\"font-weight:600;margin-bottom:4px;\">").append(card.type).append("</div>")
                                .append("  <div style=\"font-size:13px;color:var(--color-text-secondary);margin-bottom:12px;\">")
                                .append(masked).append(" (Exp: ").append(card.expiry).append(")")
                                .append("  </div>")
                                .append("  <button hx-post=\"api/auth/card/delete?id=").append(card.id).append("\" hx-target=\"#account-page-container\" hx-swap=\"outerHTML\" style=\"color:var(--color-error);font-size:12px;font-weight:500;\">").append(trans.getOrDefault("delete", "Delete")).append("</button>")
                                .append("</div>");
                    }
                }
                model.put("account_cards_html", cardList.toString());

                model.put("address_fields_html", getAddressFieldsHtml(country.code, country.locale, trans, "account-"));

                final StringBuilder options = new StringBuilder();
                for (final Country c : catalogConfig.countries)
                {
                    final String sel = c.code.equals(country.code) ? "selected" : "";
                    options.append("<option value=\"").append(c.code).append("\" ").append(sel).append(">")
                           .append(c.name).append(" (").append(c.symbol).append(")</option>");
                }
                model.put("country_options_html", options.toString());

                model.putIfAbsent("password_status", "");
                model.putIfAbsent("password_error", "");
            }

            model.putIfAbsent("auth_error", "");
            model.putIfAbsent("input_email", "");
            model.putIfAbsent("err_email", "");
            model.putIfAbsent("err_password", "");
            model.putIfAbsent("err_confirmPassword", "");

            if (LOG.isDebugEnabled())
            {
                LOG.debug("renderTemplate SUT page variables resolved: {}", model);
            }

            // Build full HTML or return only body fragment depending on HTMX request
            String bodyHtml = rawBody;
            for (final Map.Entry<String, String> entry : model.entrySet())
            {
                bodyHtml = bodyHtml.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }

            // Clean up unresolved variables
            bodyHtml = bodyHtml.replaceAll("\\$\\{[a-zA-Z_0-9-]+\\}", "");

            if (isHtmx)
            {
                if ("plp.html".equals(pageName))
                {
                    return model.getOrDefault("plp_products", "");
                }
                // HTMX requests only require the body fragment
                return bodyHtml;
            }

            // Serve full layout wrapped
            final String rawLayout = readResource("ai-test-pages/verla-" + quality + "/layout.html");
            final Map<String, String> layoutModel = new HashMap<>(model);
            layoutModel.put("body", bodyHtml);

            // Setup country selector list inside layout modal
            final StringBuilder countriesList = new StringBuilder();
            for (final Country c : catalogConfig.countries)
            {
                final String selectedClass = c.code.equals(country.code) ? "selected" : "";
                if ("bad".equals(quality))
                {
                    countriesList.append("<div class=\"country-item ").append(selectedClass).append("\" style=\"padding:10px;cursor:pointer;\" onclick=\"document.cookie='verla_country=").append(c.code).append(";path=/';location.reload();\">")
                                 .append(c.name).append(" (").append(c.symbol).append(")</div>");
                }
                else
                {
                    countriesList.append("<li class=\"country-item ").append(selectedClass).append("\" hx-get=\"api/country/select?code=").append(c.code).append("\">")
                                 .append(c.name).append(" (").append(c.symbol).append(")</li>");
                }
            }
            layoutModel.put("country_items_list", countriesList.toString());

            String layoutHtml = rawLayout;
            for (final Map.Entry<String, String> entry : layoutModel.entrySet())
            {
                layoutHtml = layoutHtml.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
            return layoutHtml.replaceAll("\\$\\{[a-zA-Z_0-9-]+\\}", "");
        }
        catch (final Exception e)
        {
            LOG.error("Failed to render template page: {}", pageName, e);
            return "500 Internal Template Error";
        }
    }

    private static String renderProductCard(final Product p, final Country country, final Map<String, String> trans, final String quality)
    {
        final double basePrice = p.basePrice;
        final Double salePrice = p.salePrice;
        
        final String badgeHtml;
        if (!p.badge.isEmpty())
        {
            badgeHtml = "<span class=\"product-badge " + ("sold-out".equalsIgnoreCase(p.badge) ? "sold-out" : "") + "\">" + p.badge.toUpperCase() + "</span>";
        }
        else
        {
            badgeHtml = "";
        }

        final String priceHtml;
        if (salePrice != null)
        {
            priceHtml = "<span class=\"original\">" + formatPrice(basePrice, country) + "</span>" +
                        "<span class=\"sale\">" + formatPrice(salePrice, country) + "</span>";
        }
        else
        {
            priceHtml = "<span>" + formatPrice(basePrice, country) + "</span>";
        }

        final String localeName = p.names.getOrDefault(country.locale, p.names.get("en"));
        final String pdpLink = "/verla-" + quality + "/p/" + p.slug + ".html";

        // Generate card layout based on perfect/normal/bad rules
        if ("bad".equals(quality))
        {
            return "<div class=\"product-card-bad\" style=\"border:1px solid #ccc;padding:10px;\" id=\"prod-info\">" +
                   "  <div onclick=\"location.href='" + pdpLink + "'\" style=\"cursor:pointer;background:#eee;height:180px;display:flex;align-items:center;justify-content:center;\">" +
                   "    <svg viewBox=\"0 0 100 100\" style=\"width:60%;height:60%;\">" + p.svgPath + "</svg>" +
                   "  </div>" +
                   "  <div style=\"padding-top:10px;\">" +
                   "    <div class=\"product-title-bad\" style=\"font-weight:bold;\">" + localeName + "</div>" +
                   "    <div class=\"product-price-bad\">" + priceHtml + "</div>" +
                   "    <div style=\"background:#C87A53;color:white;text-align:center;padding:8px;margin-top:8px;cursor:pointer;\" " +
                   "         hx-post=\"api/cart/add?productId=" + p.id + "\" hx-target=\"#cart-btn-wrapper\" hx-swap=\"outerHTML\">Add</div>" +
                   "  </div>" +
                   "</div>";
        }
        else
        {
            // Perfect and Normal layout structure
            final String ariaLabel = "perfect".equals(quality) ? " aria-label=\"View details of " + localeName + "\"" : "";
            final String accessibilityBtn = "perfect".equals(quality) ? " aria-label=\"Add " + localeName + " to shopping cart\"" : "";
            final String stockJson = escapeHtml(new Gson().toJson(productInventory.getOrDefault(p.id, Map.of())));

            return "<article class=\"product-card\">" +
                   "  <div class=\"product-media\">" +
                   "    " + badgeHtml +
                   "    <a href=\"" + pdpLink + "\"" + ariaLabel + " style=\"display:contents;\">" +
                   "      <svg viewBox=\"0 0 100 100\">" + p.svgPath + "</svg>" +
                   "    </a>" +
                   "    <button class=\"product-quick-add\" data-product-id=\"" + p.id + "\" data-category=\"" + p.category + "\" data-stock=\"" + stockJson + "\"" + accessibilityBtn + ">" +
                   "      " + trans.getOrDefault("addToCart", "Add to Cart") +
                   "    </button>" +
                   "  </div>" +
                   "  <div class=\"product-details\">" +
                   "    <h3 class=\"product-title\"><a href=\"" + pdpLink + "\">" + localeName + "</a></h3>" +
                   "    <div class=\"product-price\">" + priceHtml + "</div>" +
                   "  </div>" +
                   "</article>";
        }
    }

    private static String renderProductCardInfinite(final Product p, final Country country, final Map<String, String> trans, final String quality, final String nextUrl)
    {
        // Wrap normal card in hx-get revealed trigger tag
        final String normalCard = renderProductCard(p, country, trans, quality);
        
        // Wrap it so that when this card is revealed, it loads the next page in-place
        return "<div hx-get=\"" + nextUrl + "\" hx-trigger=\"revealed\" hx-swap=\"afterend\">" + normalCard + "</div>";
    }

    private static String getCartBadgeWrapperHtml(final Cart cart, final Map<String, String> trans, final Country country, final String quality)
    {
        return getCartBadgeWrapperHtml(cart, trans, country, quality, false);
    }

    private static String getCartBadgeWrapperHtml(final Cart cart, final Map<String, String> trans, final Country country, final String quality, final boolean showTemp)
    {
        final int count = cart.items.values().stream().mapToInt(Integer::intValue).sum();
        final String dropdownHtml;
        if ("bad".equals(quality))
        {
            dropdownHtml = "";
        }
        else
        {
            dropdownHtml = getCartDropdownHtml(cart, trans, country, quality, showTemp);
        }
        return "<div style=\"position: relative; display: inline-block;\" id=\"cart-btn-wrapper\">" +
               "  <a href=\"cart.html\" class=\"utility-btn\" id=\"cart-btn-anchor\">" +
               "    <svg class=\"icon-svg\" viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"3\" y=\"8\" width=\"18\" height=\"13\" rx=\"2\" ry=\"2\"></rect><path d=\"M16 8a4 4 0 0 0-8 0\"></path></svg> " + trans.getOrDefault("cart", "Cart") + " <span class=\"cart-badge\">" + count + "</span>" +
               "  </a>" +
               dropdownHtml +
               "</div>";
    }

    private static String getCartDropdownHtml(final Cart cart, final Map<String, String> trans, final Country country, final String quality)
    {
        return getCartDropdownHtml(cart, trans, country, quality, false);
    }

    private static String getCartDropdownHtml(final Cart cart, final Map<String, String> trans, final Country country, final String quality, final boolean showTemp)
    {
        final StringBuilder sb = new StringBuilder();
        if (showTemp)
        {
            sb.append("<div class=\"cart-dropdown show-temp\" id=\"cart-dropdown-panel\">");
        }
        else
        {
            sb.append("<div class=\"cart-dropdown\" id=\"cart-dropdown-panel\">");
        }
        if (cart.items.isEmpty())
        {
            sb.append("  <div class=\"cart-dropdown-empty\">")
              .append("    <p>").append(trans.getOrDefault("cartIsEmpty", "Cart is Empty.")).append("</p>")
              .append("  </div>");
        }
        else
        {
            sb.append("  <div class=\"cart-dropdown-items\">");
            double subtotal = 0;
            for (final Map.Entry<String, Integer> entry : cart.items.entrySet())
            {
                final Product p = lookupProductById(entry.getKey());
                if (p == null)
                {
                    continue;
                }
                final String[] parts = entry.getKey().split(":");
                final String size = parts.length > 1 ? " (" + parts[1] + ")" : "";
                final double price = p.salePrice != null ? p.salePrice : p.basePrice;
                final double rowTotal = price * entry.getValue();
                subtotal += rowTotal;

                sb.append("    <div class=\"cart-dropdown-item\">")
                  .append("      <div class=\"cart-dropdown-item-img\">")
                  .append("        <svg viewBox=\"0 0 100 100\">").append(p.svgPath).append("</svg>")
                  .append("      </div>")
                  .append("      <div class=\"cart-dropdown-item-details\">")
                  .append("        <div class=\"cart-dropdown-item-title\">").append(p.names.getOrDefault(country.locale, p.names.get("en"))).append(size).append("</div>")
                  .append("        <div class=\"cart-dropdown-item-price\">").append(entry.getValue()).append(" &times; ").append(formatPrice(price, country)).append("</div>")
                  .append("      </div>")
                  .append("    </div>");
            }
            sb.append("  </div>")
              .append("  <div class=\"cart-dropdown-footer\">")
              .append("    <div class=\"cart-dropdown-subtotal\">")
              .append("      <span>Subtotal</span>")
              .append("      <span>").append(formatPrice(subtotal, country)).append("</span>")
              .append("    </div>")
              .append("    <a href=\"cart.html\" class=\"cart-dropdown-checkout-btn\">View Cart & Checkout</a>")
              .append("  </div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String getCartContentHtml(final Cart cart, final Country country, final Map<String, String> trans, final String quality)
    {
        return getCartContentHtml(cart, country, trans, quality, null);
    }

    private static String getCartContentHtml(final Cart cart, final Country country, final Map<String, String> trans, final String quality, final String couponError)
    {
        if (cart.items.isEmpty())
        {
            return "<div id=\"cart-content-wrapper\">" +
                   "  <h1 style=\"font-family: var(--font-family-serif); font-size: 32px; font-weight: 600; margin-bottom: 30px;\">" + trans.getOrDefault("cart", "Cart") + "</h1>" +
                   "  <div style=\"text-align:center; padding: 40px 0;\">" +
                   "    <p style=\"color:var(--color-text-secondary); margin-bottom: 24px;\">" + trans.getOrDefault("cartIsEmpty", "Cart is Empty.") + "</p>" +
                   "    <a href=\"index.html\" class=\"btn-primary\">" + trans.getOrDefault("home", "Go Home") + "</a>" +
                   "  </div>" +
                   "</div>";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"cart-content-wrapper\">")
          .append("  <h1 style=\"font-family: var(--font-family-serif); font-size: 32px; font-weight: 600; margin-bottom: 30px;\">").append(trans.getOrDefault("cart", "Cart")).append("</h1>")
          .append("  <div class=\"cart-container\">")
          .append("    <div class=\"cart-main\">")
          .append("      <div class=\"cart-table-wrapper\">")
          .append("        <table class=\"cart-table\">")
          .append("          <thead>")
          .append("            <tr style=\"border-bottom: 1px solid var(--color-border); padding-bottom: 12px;\">")
          .append("              <th style=\"padding-bottom:12px;\">Product</th>")
          .append("              <th style=\"padding-bottom:12px;text-align:center;\">Quantity</th>")
          .append("              <th style=\"padding-bottom:12px;text-align:right;\">Total</th>")
          .append("            </tr>")
          .append("          </thead>")
          .append("          <tbody>");

        for (final Map.Entry<String, Integer> entry : cart.items.entrySet())
        {
            final String cartKey = entry.getKey();
            final String size;
            if (cartKey.contains(":"))
            {
                size = cartKey.split(":")[1];
            }
            else
            {
                size = "";
            }

            final Product p = lookupProductById(cartKey);
            if (p == null)
            {
                continue;
            }
            
            final double price = p.salePrice != null ? p.salePrice : p.basePrice;
            final double rowTotal = price * entry.getValue();
            final String displayName = p.names.getOrDefault(country.locale, p.names.get("en")) + (size.isEmpty() ? "" : " (" + size + ")");
            String tempKey;
            try
            {
                tempKey = URLEncoder.encode(cartKey, StandardCharsets.UTF_8.name());
            }
            catch (final Exception e)
            {
                tempKey = cartKey;
            }
            final String encKey = tempKey;

            sb.append("        <tr style=\"border-bottom: 1px solid var(--color-border);\">")
              .append("          <td style=\"padding: 20px 0; display: flex; align-items: center; gap: 16px;\">")
              .append("            <div style=\"width: 60px; height: 60px; background: var(--color-bg-secondary); border: 1px solid var(--color-border); display: flex; align-items: center; justify-content: center;\">")
              .append("              <svg viewBox=\"0 0 100 100\" style=\"width:70%;height:70%;color:var(--color-accent);\">").append(p.svgPath).append("</svg>")
              .append("            </div>")
              .append("            <div>")
              .append("              <h4 style=\"font-size: 15px; font-weight: 500;\">").append(displayName).append("</h4>")
              .append("              <div style=\"font-size: 13px; color: var(--color-text-secondary);\">").append(formatPrice(price, country)).append("</div>")
              .append("            </div>")
              .append("          </td>")
              .append("          <td style=\"padding: 20px 0; text-align: center;\">")
              .append("            <div style=\"display: inline-flex; align-items: center; border: 1px solid var(--color-border); border-radius: var(--border-radius);\">")
              .append("              <button style=\"padding: 6px 12px;\" hx-post=\"api/cart/update?productId=").append(encKey).append("&quantity=").append(entry.getValue() - 1).append("\" hx-target=\"#cart-content-wrapper\" hx-swap=\"outerHTML\">&minus;</button>")
              .append("              <span style=\"padding: 0 12px; font-weight: 600;\">").append(entry.getValue()).append("</span>")
              .append("              <button style=\"padding: 6px 12px;\" hx-post=\"api/cart/update?productId=").append(encKey).append("&quantity=").append(entry.getValue() + 1).append("\" hx-target=\"#cart-content-wrapper\" hx-swap=\"outerHTML\">&plus;</button>")
              .append("            </div>")
              .append("            <div style=\"margin-top: 6px;\">")
              .append("              <button hx-post=\"api/cart/remove?productId=").append(encKey).append("\" hx-target=\"#cart-content-wrapper\" hx-swap=\"outerHTML\" style=\"color: var(--color-error); font-size: 11px;\">Remove</button>")
              .append("            </div>")
              .append("          </td>")
              .append("          <td style=\"padding: 20px 0; text-align: right; font-weight: 600;\">").append(formatPrice(rowTotal, country)).append("</td>")
              .append("        </tr>");
        }

        final double subtotal = calculateSubtotal(cart);

        sb.append("        </tbody>")
          .append("      </table>")
          .append("    </div>")
          .append("  </div>")
          .append("  <div class=\"cart-sidebar\">")
          .append("    <h3 style=\"font-family: var(--font-family-serif); font-size: 18px; margin-bottom: 20px; border-bottom: 1px solid var(--color-border); padding-bottom: 12px;\">Order Summary</h3>")
          .append("    <form hx-post=\"api/cart/coupon\" hx-target=\"#cart-content-wrapper\" hx-swap=\"outerHTML\" style=\"margin-bottom: 24px;\">")
          .append("      <label for=\"couponCode\" class=\"form-label\" style=\"font-size:11px;\">").append(trans.getOrDefault("promoCode", "Promo Code")).append("</label>")
          .append("      <div style=\"display:flex; gap:8px;\">")
          .append("        <input type=\"text\" id=\"couponCode\" name=\"couponCode\" class=\"form-control\" placeholder=\"10p-off\" style=\"padding: 8px 12px;\" value=\"").append(cart.coupon != null ? cart.coupon : "").append("\">")
          .append("        <button type=\"submit\" class=\"btn-secondary\" style=\"padding: 8px 16px; font-size: 11px;\">").append(trans.getOrDefault("apply", "Apply")).append("</button>")
          .append("      </div>")
          .append("    </form>");

        if (couponError != null && !couponError.isEmpty())
        {
            sb.append("<div class=\"error-message\" style=\"color: var(--color-error); font-size: 12px; margin-top: -16px; margin-bottom: 16px;\">")
              .append(escapeHtml(couponError))
              .append("</div>");
        }

        final double discount = calculateDiscount(cart, subtotal);
        final double shipping = calculateShipping(cart, subtotal);
        final double tax = Math.round((subtotal - discount) * 0.1 * 100.0) / 100.0;
        final double total = subtotal - discount + shipping + tax;

        sb.append("    <div style=\"font-size: 14px; display: flex; flex-direction: column; gap: 10px;\">")
          .append("      <div style=\"display: flex; justify-content: space-between;\">")
          .append("        <span>").append(trans.getOrDefault("subtotal", "Subtotal")).append("</span>")
          .append("        <span>").append(formatPrice(subtotal, country)).append("</span>")
          .append("      </div>");

        if (discount > 0)
        {
            sb.append("      <div style=\"display: flex; justify-content: space-between; color: var(--color-success); font-weight: 500;\">")
              .append("        <span>Discount (").append(cart.coupon.toUpperCase()).append(")</span>")
              .append("        <span>-").append(formatPrice(discount, country)).append("</span>")
              .append("      </div>");
        }

        sb.append("      <div style=\"display: flex; justify-content: space-between;\">")
          .append("        <span>").append(trans.getOrDefault("shipping", "Shipping")).append("</span>")
          .append("        <span>").append(formatPrice(shipping, country)).append("</span>")
          .append("      </div>")
          .append("      <div style=\"display: flex; justify-content: space-between;\">")
          .append("        <span>").append(trans.getOrDefault("tax", "Tax")).append("</span>")
          .append("        <span>").append(formatPrice(tax, country)).append("</span>")
          .append("      </div>")
          .append("      <div style=\"display: flex; justify-content: space-between; font-weight: 600; font-size: 16px; border-top: 1px solid var(--color-border); padding-top: 12px; margin-top: 4px; color: var(--color-text-primary);\">")
          .append("        <span>").append(trans.getOrDefault("total", "Total")).append("</span>")
          .append("        <span>").append(formatPrice(total, country)).append("</span>")
          .append("      </div>")
          .append("    </div>")
          .append("    <a href=\"checkout.html\" class=\"btn-primary\" style=\"display: block; text-align: center; margin-top: 24px; padding: 12px; text-transform: uppercase; letter-spacing: 0.05em; font-size: 12px;\">")
          .append("      ").append(trans.getOrDefault("checkout", "Checkout"))
          .append("    </a>")
          .append("  </div>")
          .append("</div>")
          .append("</div>");

        return sb.toString();
    }

    private static double calculateSubtotal(final Cart cart)
    {
        double subtotal = 0;
        for (final Map.Entry<String, Integer> entry : cart.items.entrySet())
        {
            final Product p = lookupProductById(entry.getKey());
            if (p != null)
            {
                final double price = p.salePrice != null ? p.salePrice : p.basePrice;
                subtotal += price * entry.getValue();
            }
        }
        return Math.round(subtotal * 100.0) / 100.0;
    }

    private static double calculateDiscount(final Cart cart, final double subtotal)
    {
        if (cart.coupon == null) return 0;
        switch (cart.coupon)
        {
            case "10p-off":
                return Math.round(subtotal * 0.1 * 100.0) / 100.0;
            case "bogo":
                // Buy one, get one free on any item with quantity >= 2
                double bogoDiscount = 0;
                for (final Map.Entry<String, Integer> entry : cart.items.entrySet())
                {
                    if (entry.getValue() >= 2)
                    {
                        final Product p = lookupProductById(entry.getKey());
                        if (p != null)
                        {
                            final double price = p.salePrice != null ? p.salePrice : p.basePrice;
                            bogoDiscount += price * (entry.getValue() / 2);
                        }
                    }
                }
                return Math.round(bogoDiscount * 100.0) / 100.0;
            default:
                return 0;
        }
    }

    private static double calculateShipping(final Cart cart, final double subtotal)
    {
        if ("freeship".equals(cart.coupon) || subtotal >= 150.0)
        {
            return 0.00;
        }
        return 9.99;
    }

    private static String formatPrice(final double usdPrice, final Country country)
    {
        final double localPrice = usdPrice * country.rate;
        if ("JPY".equals(country.currency))
        {
            return String.format(Locale.US, country.format, String.format(Locale.US, "%,d", (long)Math.round(localPrice)));
        }
        return String.format(Locale.US, country.format, String.format(Locale.US, "%,.2f", localPrice));
    }

    private static String getAddressFieldsHtml(final String countryCode, final String locale, final Map<String, String> trans, final String prefix)
    {
        final String labelStreet = trans.getOrDefault("street", "Street Address");
        final String labelCity = trans.getOrDefault("city", "City");
        final String labelState = trans.getOrDefault("state", "State/Province");
        final String labelPostcode = trans.getOrDefault("postcode", "Zip/Postal Code");
        final String labelPrefecture = trans.getOrDefault("prefecture", "Prefecture");
        final String labelWard = trans.getOrDefault("ward", "City/Ward");
        final String labelSubarea = trans.getOrDefault("subarea", "Sub-area/Chome");
        final String labelBuilding = trans.getOrDefault("building", "Building/Apt No");

        if ("US".equals(countryCode) || "CA".equals(countryCode) || "CA_EN".equals(countryCode) || "CA_FR".equals(countryCode))
        {
            return "<div class=\"form-group\">" +
                   "  <label class=\"form-label\" for=\"" + prefix + "street\">" + labelStreet + "</label>" +
                   "  <input type=\"text\" id=\"" + prefix + "street\" name=\"street\" class=\"form-control\" required>" +
                   "</div>" +
                   "<div class=\"form-grid-2\">" +
                   "  <div class=\"form-group\">" +
                   "    <label class=\"form-label\" for=\"" + prefix + "city\">" + labelCity + "</label>" +
                   "    <input type=\"text\" id=\"" + prefix + "city\" name=\"city\" class=\"form-control\" required>" +
                   "  </div>" +
                   "  <div class=\"form-group\">" +
                   "    <label class=\"form-label\" for=\"" + prefix + "state\">" + labelState + "</label>" +
                   "    <input type=\"text\" id=\"" + prefix + "state\" name=\"state\" class=\"form-control\" required>" +
                   "  </div>" +
                   "</div>" +
                   "<div class=\"form-group\">" +
                   "  <label class=\"form-label\" for=\"" + prefix + "postcode\">" + labelPostcode + "</label>" +
                   "  <input type=\"text\" id=\"" + prefix + "postcode\" name=\"postcode\" class=\"form-control\" required>" +
                   "</div>";
        }
        else if ("JP".equals(countryCode))
        {
            return "<div class=\"form-group\">" +
                   "  <label class=\"form-label\" for=\"" + prefix + "postcode\">" + labelPostcode + "</label>" +
                   "  <input type=\"text\" id=\"" + prefix + "postcode\" name=\"postcode\" class=\"form-control\" placeholder=\"123-4567\" required>" +
                   "</div>" +
                   "<div class=\"form-grid-2\">" +
                   "  <div class=\"form-group\">" +
                   "    <label class=\"form-label\" for=\"" + prefix + "prefecture\">" + labelPrefecture + "</label>" +
                   "    <input type=\"text\" id=\"" + prefix + "prefecture\" name=\"prefecture\" class=\"form-control\" required>" +
                   "  </div>" +
                   "  <div class=\"form-group\">" +
                   "    <label class=\"form-label\" for=\"" + prefix + "ward\">" + labelWard + "</label>" +
                   "    <input type=\"text\" id=\"" + prefix + "ward\" name=\"ward\" class=\"form-control\" required>" +
                   "  </div>" +
                   "</div>" +
                   "<div class=\"form-group\">" +
                   "  <label class=\"form-label\" for=\"" + prefix + "subarea\">" + labelSubarea + "</label>" +
                   "  <input type=\"text\" id=\"" + prefix + "subarea\" name=\"subarea\" class=\"form-control\" required>" +
                   "</div>" +
                   "<div class=\"form-group\">" +
                   "  <label class=\"form-label\" for=\"" + prefix + "building\">" + labelBuilding + "</label>" +
                   "  <input type=\"text\" id=\"" + prefix + "building\" name=\"building\" class=\"form-control\">" +
                   "</div>";
        }
        else
        {
            return "<div class=\"form-group\">" +
                   "  <label class=\"form-label\" for=\"" + prefix + "street\">" + labelStreet + "</label>" +
                   "  <input type=\"text\" id=\"" + prefix + "street\" name=\"street\" class=\"form-control\" placeholder=\"Hauptstraße 12\" required>" +
                   "</div>" +
                   "<div class=\"form-grid-1-2\">" +
                   "  <div class=\"form-group\">" +
                   "    <label class=\"form-label\" for=\"" + prefix + "postcode\">" + labelPostcode + "</label>" +
                   "    <input type=\"text\" id=\"" + prefix + "postcode\" name=\"postcode\" class=\"form-control\" required>" +
                   "  </div>" +
                   "  <div class=\"form-group\">" +
                   "    <label class=\"form-label\" for=\"" + prefix + "city\">" + labelCity + "</label>" +
                   "    <input type=\"text\" id=\"" + prefix + "city\" name=\"city\" class=\"form-control\" required>" +
                   "  </div>" +
                   "</div>";
        }
    }

    private static Product lookupProductById(final String id)
    {
        final String cleanId = id.contains(":") ? id.split(":")[0] : id;
        for (final Product p : catalogProducts)
        {
            if (p.id.equals(cleanId)) return p;
        }
        return null;
    }

    private static Product lookupProductBySlug(final String slug)
    {
        for (final Product p : catalogProducts)
        {
            if (p.slug.equals(slug)) return p;
        }
        return null;
    }

    private static Map<String, String> parseCookies(final String cookieHeader)
    {
        final Map<String, String> cookies = new HashMap<>();
        if (cookieHeader != null)
        {
            final String[] pairs = cookieHeader.split(";");
            for (final String pair : pairs)
            {
                final String[] kv = pair.trim().split("=", 2);
                if (kv.length == 2)
                {
                    cookies.put(kv[0], kv[1]);
                }
            }
        }
        return cookies;
    }

    private static Map<String, String> parseRequestBody(final HttpExchange exchange) throws IOException
    {
        final Map<String, String> params = new HashMap<>();
        if (exchange.getRequestURI().getQuery() != null)
        {
            final String query = exchange.getRequestURI().getQuery();
            final String[] pairs = query.split("&");
            for (final String pair : pairs)
            {
                final String[] kv = pair.split("=", 2);
                if (kv.length == 2)
                {
                    params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name()),
                               URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()));
                }
                else if (kv.length == 1)
                {
                    params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name()), "");
                }
            }
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod()) || "PUT".equalsIgnoreCase(exchange.getRequestMethod()))
        {
            try (final InputStream is = exchange.getRequestBody();
                 final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
            {
                final String query = br.lines().collect(Collectors.joining("\n"));
                if (!query.isEmpty())
                {
                    final String[] pairs = query.split("&");
                    for (final String pair : pairs)
                    {
                        final String[] kv = pair.split("=", 2);
                        if (kv.length == 2)
                        {
                            params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name()),
                                       URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()));
                        }
                        else if (kv.length == 1)
                        {
                            params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name()), "");
                        }
                    }
                }
            }
        }
        return params;
    }

    private static String getQueryParam(final String queryOrPath, final String key)
    {
        final int qIdx = queryOrPath.indexOf("?");
        final String query = qIdx != -1 ? queryOrPath.substring(qIdx + 1) : queryOrPath;
        final String[] pairs = query.split("&");
        for (final String pair : pairs)
        {
            final String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equalsIgnoreCase(key))
            {
                try
                {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                }
                catch (final UnsupportedEncodingException e)
                {
                    return kv[1];
                }
            }
        }
        return "";
    }

    private static List<String> getQueryParamValues(final String queryOrPath, final String key)
    {
        final List<String> values = new ArrayList<>();
        final int qIdx = queryOrPath.indexOf("?");
        final String query = qIdx != -1 ? queryOrPath.substring(qIdx + 1) : queryOrPath;
        final String[] pairs = query.split("&");
        for (final String pair : pairs)
        {
            final String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equalsIgnoreCase(key))
            {
                try
                {
                    values.add(URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()));
                }
                catch (final Exception e)
                {
                    values.add(kv[1]);
                }
            }
        }
        return values;
    }

    private static String readResource(final String resourcePath)
    {
        try (final InputStream is = EmbeddedHtmlServer.class.getClassLoader().getResourceAsStream(resourcePath))
        {
            if (is == null) return null;
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));
        }
        catch (final Exception e)
        {
            LOG.error("Failed to read resource path: {}", resourcePath, e);
            return null;
        }
    }

    private static void sendResponse(final HttpExchange exchange, final int code, final String contentType, final String response) throws IOException
    {
        final byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);
        try (final OutputStream os = exchange.getResponseBody())
        {
            os.write(bytes);
        }
    }

    private static String escapeHtml(final String raw)
    {
        if (raw == null) return "";
        return raw.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }

    public static void main(final String[] args)
    {
        try
        {
            final EmbeddedHtmlServer server = new EmbeddedHtmlServer();
            server.start();

            System.out.println("========================================================================");
            System.out.println("  Neodymium Aura AI: Embedded HTML Server Running Successfully!");
            System.out.println("========================================================================");
            System.out.println("  Access the test applications via the following URLs:");
            System.out.println();
            System.out.println("  [HTTP Contexts]");
            System.out.println("    - Starter Hub Portal:   http://localhost:" + server.getPort() + "/AuraGlanceTest/index.html");
            System.out.println("    - Shop Home:            http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/index.html");
            System.out.println("    - Forms Demo:           http://localhost:" + server.getPort() + "/AuraGlanceTest/shop/forms.html");
            System.out.println("    - Dashboard:            http://localhost:" + server.getPort() + "/AuraGlanceTest/dashboard/index.html");
            System.out.println("    - Accessibility:        http://localhost:" + server.getPort() + "/AuraGlanceTest/a11y/index.html");
            System.out.println("    - React SPA:            http://localhost:" + server.getPort() + "/AuraGlanceTest/spa/index.html");
            System.out.println("    - VÉRLA Perfect Store:  http://localhost:" + server.getPort() + "/verla-perfect/index.html");
            System.out.println("    - VÉRLA Normal Store:   http://localhost:" + server.getPort() + "/verla-normal/index.html");
            System.out.println("    - VÉRLA Bad Store:      http://localhost:" + server.getPort() + "/verla-bad/index.html");
            System.out.println();
            System.out.println("  [HTTPS Secure Contexts]");
            System.out.println("    - Starter Hub Portal:   https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/index.html");
            System.out.println("    - Shop Home:            https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/shop/index.html");
            System.out.println("    - Forms Demo:           https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/shop/forms.html");
            System.out.println("    - Dashboard:            https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/dashboard/index.html");
            System.out.println("    - Accessibility:        https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/a11y/index.html");
            System.out.println("    - React SPA:            https://localhost:" + server.getHttpsPort() + "/AuraGlanceTest/spa/index.html");
            System.out.println("    - VÉRLA Perfect Store:  https://localhost:" + server.getHttpsPort() + "/verla-perfect/index.html");
            System.out.println("    - VÉRLA Normal Store:   https://localhost:" + server.getHttpsPort() + "/verla-normal/index.html");
            System.out.println("    - VÉRLA Bad Store:      https://localhost:" + server.getHttpsPort() + "/verla-bad/index.html");
            System.out.println();
            System.out.println("  NOTE: For HTTPS, you will get a self-signed certificate warning.");
            System.out.println("        You can safely bypass this or run with Chrome's '--ignore-certificate-errors' flag.");
            System.out.println("  Press Ctrl+C to terminate the server.");
            System.out.println("========================================================================");

            Thread.currentThread().join();
        }
        catch (final Exception e)
        {
            System.err.println("Failed to start Embedded HTML Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
