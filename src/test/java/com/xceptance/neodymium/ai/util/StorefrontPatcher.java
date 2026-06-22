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
package com.xceptance.neodymium.ai.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Programmatic patcher and subpage generator for the VÉRLA apparel sandbox.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StorefrontPatcher
{
    private static final String BASE_DIR = "/home/rschwietzke/projects/GIT/neodymium-library/src/test/resources/ai-test-pages/AuraGlanceTest/shop";

    private StorefrontPatcher()
    {
    }

    public static void main(final String[] args)
    {
        try
        {
            System.out.println("Starting storefront patching process...");
            patchPerfectPages();
            patchNormalPages();
            patchBadPages();
            System.out.println("Patching process finished successfully.");
        }
        catch (final Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getCommonCss()
    {
        return "\n        /* Dynamic Loader and Cart additions */\n" +
                "        @keyframes spin-loader {\n" +
                "            to { transform: rotate(360deg); }\n" +
                "        }\n" +
                "        #global-page-loader {\n" +
                "            position: fixed;\n" +
                "            top: 0;\n" +
                "            left: 0;\n" +
                "            right: 0;\n" +
                "            bottom: 0;\n" +
                "            background: rgba(255, 255, 255, 0.75);\n" +
                "            z-index: 10000;\n" +
                "            display: none;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "        }\n" +
                "        #global-page-loader .loader-spinner {\n" +
                "            width: 48px;\n" +
                "            height: 48px;\n" +
                "            border: 4px solid var(--color-border);\n" +
                "            border-top-color: var(--color-accent);\n" +
                "            border-radius: 50%;\n" +
                "            animation: spin-loader 0.8s linear infinite;\n" +
                "        }\n" +
                "        .cart-dropdown {\n" +
                "            display: none;\n" +
                "            position: absolute;\n" +
                "            top: 100%;\n" +
                "            right: 0;\n" +
                "            background: var(--color-bg-primary);\n" +
                "            border: 1px solid var(--color-border);\n" +
                "            box-shadow: 0 10px 30px rgba(0,0,0,0.08);\n" +
                "            width: 320px;\n" +
                "            z-index: 1000;\n" +
                "            padding: 20px;\n" +
                "            opacity: 0;\n" +
                "            transform: translateY(10px);\n" +
                "            transition: opacity var(--transition-speed), transform var(--transition-speed);\n" +
                "        }\n";
    }

    private static String injectCss(final String html, final String extraCss)
    {
        final int styleIdx = html.indexOf("</style>");
        if (styleIdx != -1)
        {
            return html.substring(0, styleIdx) + extraCss + html.substring(styleIdx);
        }
        return html;
    }

    private static void patchPerfectPages() throws IOException
    {
        final Path hpPath = Paths.get(BASE_DIR, "homepage-perfect.html");
        final Path plpPath = Paths.get(BASE_DIR, "plp-perfect.html");

        if (!Files.exists(hpPath) || !Files.exists(plpPath))
        {
            throw new IOException("Perfect files not found in base dir: " + BASE_DIR);
        }

        // 1. Update Homepage-perfect
        String hp = new String(Files.readAllBytes(hpPath), StandardCharsets.UTF_8);

        // Inject CSS
        hp = injectCss(hp, getCommonCss());

        // Update Cart Button to have Wrapper
        final String cartBtnTarget = "                <button class=\"utility-btn\" id=\"cart-btn\" aria-label=\"View Shopping Cart\">\n" +
                "                    Cart <span class=\"cart-badge\" id=\"cart-count-badge\">0</span>\n" +
                "                </button>";
        final String cartBtnReplacement = "                <div style=\"position: relative; display: inline-block;\" id=\"cart-dropdown-wrapper\">\n" +
                "                    <button class=\"utility-btn\" id=\"cart-btn\" aria-label=\"View Shopping Cart\">\n" +
                "                        Cart <span class=\"cart-badge\" id=\"cart-count-badge\">0</span>\n" +
                "                    </button>\n" +
                "                    <!-- Cart Dropdown -->\n" +
                "                    <div class=\"cart-dropdown\" id=\"cart-dropdown-panel\">\n" +
                "                        <h4 style=\"font-family: var(--font-family-serif); font-size: 16px; margin-bottom: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 8px;\">Shopping Cart</h4>\n" +
                "                        <div id=\"cart-dropdown-items\" style=\"max-height: 200px; overflow-y: auto; margin-bottom: 12px;\">\n" +
                "                            <p style=\"font-size: 13px; color: var(--color-text-secondary); text-align: center; padding: 20px 0;\" id=\"cart-empty-message\">Your cart is empty.</p>\n" +
                "                        </div>\n" +
                "                        <div id=\"cart-dropdown-footer\" style=\"display: none; border-top: 1px solid var(--color-border); padding-top: 12px;\">\n" +
                "                            <div style=\"display: flex; justify-content: space-between; font-size: 14px; font-weight: 600; margin-bottom: 12px;\">\n" +
                "                                <span>Subtotal:</span>\n" +
                "                                <span id=\"cart-dropdown-subtotal\">$0.00</span>\n" +
                "                            </div>\n" +
                "                            <button class=\"cookie-btn cookie-btn-accept\" style=\"width: 100%; justify-content: center; padding: 10px; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em;\">Checkout</button>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>";
        hp = hp.replace(cartBtnTarget, cartBtnReplacement);

        // Update Footer Links
        hp = hp.replace("href=\"#\" id=\"footer-link-shipping\"", "href=\"shipping-perfect.html\" id=\"footer-link-shipping\"");
        hp = hp.replace("href=\"#\" id=\"footer-link-orders\"", "href=\"track-orders-perfect.html\" id=\"footer-link-orders\"");
        hp = hp.replace("href=\"#\" id=\"footer-link-faq\"", "href=\"faq-perfect.html\" id=\"footer-link-faq\"");
        hp = hp.replace("href=\"#\" id=\"footer-link-contact\"", "href=\"contact-perfect.html\" id=\"footer-link-contact\"");
        hp = hp.replace("href=\"#\" id=\"footer-link-about\"", "href=\"about-perfect.html\" id=\"footer-link-about\"");
        hp = hp.replace("href=\"#\" id=\"footer-link-stores\"", "href=\"stores-perfect.html\" id=\"footer-link-stores\"");
        hp = hp.replace("href=\"#\" id=\"footer-link-careers\"", "href=\"careers-perfect.html\" id=\"footer-link-careers\"");

        // Replace country selector and script section
        final String countryModalTarget = "    <!-- Country Selector Modal Overlay -->\n" +
                "    <div class=\"modal-overlay\" id=\"country-modal-overlay\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"country-modal-title\">";
        
        // Find where the country modal starts
        final int modalStartIndex = hp.indexOf(countryModalTarget);
        if (modalStartIndex == -1)
        {
            throw new IOException("Could not locate country modal start in homepage-perfect.html");
        }

        final String restOfPage = getPerfectOverlayAndScriptsBlock("perfect");
        hp = hp.substring(0, modalStartIndex) + restOfPage;

        Files.write(hpPath, hp.getBytes(StandardCharsets.UTF_8));
        System.out.println("homepage-perfect.html patched successfully.");

        // 2. Update PLP-perfect
        String plp = new String(Files.readAllBytes(plpPath), StandardCharsets.UTF_8);
        plp = injectCss(plp, getCommonCss());
        plp = plp.replace(cartBtnTarget, cartBtnReplacement);
        plp = plp.replace("href=\"#\" id=\"footer-link-shipping\"", "href=\"shipping-perfect.html\" id=\"footer-link-shipping\"");
        plp = plp.replace("href=\"#\" id=\"footer-link-orders\"", "href=\"track-orders-perfect.html\" id=\"footer-link-orders\"");
        plp = plp.replace("href=\"#\" id=\"footer-link-faq\"", "href=\"faq-perfect.html\" id=\"footer-link-faq\"");
        plp = plp.replace("href=\"#\" id=\"footer-link-contact\"", "href=\"contact-perfect.html\" id=\"footer-link-contact\"");
        plp = plp.replace("href=\"#\" id=\"footer-link-about\"", "href=\"about-perfect.html\" id=\"footer-link-about\"");
        plp = plp.replace("href=\"#\" id=\"footer-link-stores\"", "href=\"stores-perfect.html\" id=\"footer-link-stores\"");
        plp = plp.replace("href=\"#\" id=\"footer-link-careers\"", "href=\"careers-perfect.html\" id=\"footer-link-careers\"");

        final int plpModalStartIndex = plp.indexOf(countryModalTarget);
        if (plpModalStartIndex == -1)
        {
            throw new IOException("Could not locate country modal start in plp-perfect.html");
        }
        plp = plp.substring(0, plpModalStartIndex) + restOfPage;
        Files.write(plpPath, plp.getBytes(StandardCharsets.UTF_8));
        System.out.println("plp-perfect.html patched successfully.");

        // 3. Create the 7 perfect subpages
        generateSubpages("perfect", hp);
    }

    private static void patchNormalPages() throws IOException
    {
        final Path hpPath = Paths.get(BASE_DIR, "homepage-normal.html");
        final Path plpPath = Paths.get(BASE_DIR, "plp-normal.html");

        if (!Files.exists(hpPath) || !Files.exists(plpPath))
        {
            throw new IOException("Normal files not found in base directory.");
        }

        // 1. Update Homepage-normal
        String hp = new String(Files.readAllBytes(hpPath), StandardCharsets.UTF_8);

        // Inject CSS
        hp = injectCss(hp, getCommonCss());

        // Update Cart Button
        final String cartBtnTarget = "                <button class=\"utility-btn\" id=\"btn_cart\">\n" +
                "                    Cart <span class=\"cart-badge\" id=\"badge_cart\">0</span>\n" +
                "                </button>";
        final String cartBtnReplacement = "                <div style=\"position: relative; display: inline-block;\" id=\"cart-dropdown-wrapper\">\n" +
                "                    <button class=\"utility-btn\" id=\"btn_cart\">\n" +
                "                        Cart <span class=\"cart-badge\" id=\"badge_cart\">0</span>\n" +
                "                    </button>\n" +
                "                    <!-- Cart Dropdown -->\n" +
                "                    <div class=\"cart-dropdown\" id=\"cart-dropdown-panel\">\n" +
                "                        <h4 style=\"font-family: var(--font-family-serif); font-size: 16px; margin-bottom: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 8px;\">Shopping Cart</h4>\n" +
                "                        <div id=\"cart-dropdown-items\" style=\"max-height: 200px; overflow-y: auto; margin-bottom: 12px;\">\n" +
                "                            <p style=\"font-size: 13px; color: var(--color-text-secondary); text-align: center; padding: 20px 0;\" id=\"cart-empty-message\">Your cart is empty.</p>\n" +
                "                        </div>\n" +
                "                        <div id=\"cart-dropdown-footer\" style=\"display: none; border-top: 1px solid var(--color-border); padding-top: 12px;\">\n" +
                "                            <div style=\"display: flex; justify-content: space-between; font-size: 14px; font-weight: 600; margin-bottom: 12px;\">\n" +
                "                                <span>Subtotal:</span>\n" +
                "                                <span id=\"cart-dropdown-subtotal\">$0.00</span>\n" +
                "                            </div>\n" +
                "                            <button class=\"cookie-btn cookie-btn-accept\" style=\"width: 100%; justify-content: center; padding: 10px; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em;\">Checkout</button>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>";
        hp = hp.replace(cartBtnTarget, cartBtnReplacement);

        // Footer links
        hp = hp.replace("href=\"#\">Shipping & Returns</a>", "href=\"shipping-normal.html\">Shipping & Returns</a>");
        hp = hp.replace("href=\"#\">Track Orders</a>", "href=\"track-orders-normal.html\">Track Orders</a>");
        hp = hp.replace("href=\"#\">FAQ</a>", "href=\"faq-normal.html\">FAQ</a>");
        hp = hp.replace("href=\"#\">Contact Us</a>", "href=\"contact-normal.html\">Contact Us</a>");
        hp = hp.replace("href=\"#\">About VÉRLA</a>", "href=\"about-normal.html\">About VÉRLA</a>");
        hp = hp.replace("href=\"#\">Our Stores</a>", "href=\"stores-normal.html\">Our Stores</a>");
        hp = hp.replace("href=\"#\">Careers</a>", "href=\"careers-normal.html\">Careers</a>");

        // Modal overlay
        final String countryModalTarget = "    <!-- Country Selector Modal Overlay -->\n" +
                "    <div class=\"modal-overlay\" id=\"country_modal\">";
        final int modalStartIndex = hp.indexOf(countryModalTarget);
        if (modalStartIndex == -1)
        {
            throw new IOException("Could not locate country modal start in homepage-normal.html");
        }

        final String restOfPage = getPerfectOverlayAndScriptsBlock("normal");
        hp = hp.substring(0, modalStartIndex) + restOfPage;

        Files.write(hpPath, hp.getBytes(StandardCharsets.UTF_8));
        System.out.println("homepage-normal.html patched successfully.");

        // 2. Update PLP-normal
        String plp = new String(Files.readAllBytes(plpPath), StandardCharsets.UTF_8);
        plp = injectCss(plp, getCommonCss());
        plp = plp.replace(cartBtnTarget, cartBtnReplacement);
        plp = plp.replace("href=\"#\">Shipping & Returns</a>", "href=\"shipping-normal.html\">Shipping & Returns</a>");
        plp = plp.replace("href=\"#\">Track Orders</a>", "href=\"track-orders-normal.html\">Track Orders</a>");
        plp = plp.replace("href=\"#\">FAQ</a>", "href=\"faq-normal.html\">FAQ</a>");
        plp = plp.replace("href=\"#\">Contact Us</a>", "href=\"contact-normal.html\">Contact Us</a>");
        plp = plp.replace("href=\"#\">About VÉRLA</a>", "href=\"about-normal.html\">About VÉRLA</a>");
        plp = plp.replace("href=\"#\">Our Stores</a>", "href=\"stores-normal.html\">Our Stores</a>");
        plp = plp.replace("href=\"#\">Careers</a>", "href=\"careers-normal.html\">Careers</a>");

        final int plpModalStartIndex = plp.indexOf(countryModalTarget);
        if (plpModalStartIndex == -1)
        {
            throw new IOException("Could not locate country modal start in plp-normal.html");
        }
        plp = plp.substring(0, plpModalStartIndex) + restOfPage;
        Files.write(plpPath, plp.getBytes(StandardCharsets.UTF_8));
        System.out.println("plp-normal.html patched successfully.");

        // 3. Create normal subpages
        generateSubpages("normal", hp);
    }

    private static void patchBadPages() throws IOException
    {
        final Path hpPath = Paths.get(BASE_DIR, "homepage-bad.html");
        final Path plpPath = Paths.get(BASE_DIR, "plp-bad.html");

        if (!Files.exists(hpPath) || !Files.exists(plpPath))
        {
            throw new IOException("Bad files not found in base directory.");
        }

        // 1. Update Homepage-bad
        String hp = new String(Files.readAllBytes(hpPath), StandardCharsets.UTF_8);

        // Add custom .bad-checkbox style if not already present
        final String badCheckboxStyle = "\n        /* Custom Div Checkbox Style */\n" +
                "        .bad-checkbox {\n" +
                "            width: 16px;\n" +
                "            height: 16px;\n" +
                "            border: 1px solid #767676;\n" +
                "            margin-right: 10px;\n" +
                "            display: inline-block;\n" +
                "            cursor: pointer;\n" +
                "            background-color: white;\n" +
                "            vertical-align: middle;\n" +
                "            position: relative;\n" +
                "        }\n" +
                "        .bad-checkbox.checked::after {\n" +
                "            content: \"\";\n" +
                "            position: absolute;\n" +
                "            top: 2px;\n" +
                "            left: 5px;\n" +
                "            width: 4px;\n" +
                "            height: 8px;\n" +
                "            border: solid var(--color-accent);\n" +
                "            border-width: 0 2px 2px 0;\n" +
                "            transform: rotate(45deg);\n" +
                "        }\n" +
                "        .bad-checkbox.disabled {\n" +
                "            background-color: #f0f0f0;\n" +
                "            cursor: not-allowed;\n" +
                "            opacity: 0.7;\n" +
                "        }\n";

        // Inject CSS
        hp = injectCss(hp, getCommonCss() + badCheckboxStyle);

        // Update Cart Button to have wrapper
        final String cartBtnTarget = "                <div style=\"color: var(--color-text-primary); padding: 8px; display: flex; align-items: center; font-size: 13px; text-transform: uppercase; letter-spacing: 0.05em; cursor: pointer;\" id=\"u_cart\">\n" +
                "                    Cart <div style=\"background-color: var(--color-accent); color: white; font-size: 10px; border-radius: 50%; width: 18px; height: 18px; display: inline-flex; align-items: center; justify-content: center; margin-left: 4px; font-weight: bold;\" id=\"cart_num\">0</div>\n" +
                "                </div>";
        final String cartBtnReplacement = "                <div style=\"position: relative; display: inline-block;\" id=\"cart-dropdown-wrapper\">\n" +
                "                    <div style=\"color: var(--color-text-primary); padding: 8px; display: flex; align-items: center; font-size: 13px; text-transform: uppercase; letter-spacing: 0.05em; cursor: pointer;\" id=\"u_cart\">\n" +
                "                        Cart <div style=\"background-color: var(--color-accent); color: white; font-size: 10px; border-radius: 50%; width: 18px; height: 18px; display: inline-flex; align-items: center; justify-content: center; margin-left: 4px; font-weight: bold;\" id=\"cart_num\">0</div>\n" +
                "                    </div>\n" +
                "                    <!-- Cart Dropdown -->\n" +
                "                    <div class=\"cart-dropdown\" id=\"cart-dropdown-panel\">\n" +
                "                        <h4 style=\"font-family: var(--font-family-serif); font-size: 16px; margin-bottom: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 8px;\">Shopping Cart</h4>\n" +
                "                        <div id=\"cart-dropdown-items\" style=\"max-height: 200px; overflow-y: auto; margin-bottom: 12px;\">\n" +
                "                            <p style=\"font-size: 13px; color: var(--color-text-secondary); text-align: center; padding: 20px 0;\" id=\"cart-empty-message\">Your cart is empty.</p>\n" +
                "                        </div>\n" +
                "                        <div id=\"cart-dropdown-footer\" style=\"display: none; border-top: 1px solid var(--color-border); padding-top: 12px;\">\n" +
                "                            <div style=\"display: flex; justify-content: space-between; font-size: 14px; font-weight: 600; margin-bottom: 12px;\">\n" +
                "                                <span>Subtotal:</span>\n" +
                "                                <span id=\"cart-dropdown-subtotal\">$0.00</span>\n" +
                "                            </div>\n" +
                "                            <button class=\"cookie-btn cookie-btn-accept\" style=\"width: 100%; justify-content: center; padding: 10px; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em;\">Checkout</button>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </div>";
        hp = hp.replace(cartBtnTarget, cartBtnReplacement);

        // Update Footer Links to point to subpages
        hp = hp.replace("onclick=\"location.href='#'\">Shipping & Returns</div>", "onclick=\"location.href='shipping-bad.html'\">Shipping & Returns</div>");
        hp = hp.replace("onclick=\"location.href='#'\">Track Orders</div>", "onclick=\"location.href='track-orders-bad.html'\">Track Orders</div>");
        hp = hp.replace("onclick=\"location.href='#'\">FAQ</div>", "onclick=\"location.href='faq-bad.html'\">FAQ</div>");
        hp = hp.replace("onclick=\"location.href='#'\">Contact Us</div>", "onclick=\"location.href='contact-bad.html'\">Contact Us</div>");
        hp = hp.replace("onclick=\"location.href='#'\">About VÉRLA</div>", "onclick=\"location.href='about-bad.html'\">About VÉRLA</div>");
        hp = hp.replace("onclick=\"location.href='#'\">Our Stores</div>", "onclick=\"location.href='stores-bad.html'\">Our Stores</div>");
        hp = hp.replace("onclick=\"location.href='#'\">Careers</div>", "onclick=\"location.href='careers-bad.html'\">Careers</div>");

        // Modal overlay
        final String countryModalTarget = "    <!-- Country Selector Modal Overlay -->\n" +
                "    <div style=\"position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.4); z-index: 200; display: flex; align-items: center; justify-content: center; opacity: 0; visibility: hidden; transition: all 0.3s;\" id=\"pop_m_bad\">";
        final int modalStartIndex = hp.indexOf(countryModalTarget);
        if (modalStartIndex == -1)
        {
            throw new IOException("Could not locate country modal start in homepage-bad.html");
        }

        final String restOfPage = getPerfectOverlayAndScriptsBlock("bad");
        hp = hp.substring(0, modalStartIndex) + restOfPage;

        Files.write(hpPath, hp.getBytes(StandardCharsets.UTF_8));
        System.out.println("homepage-bad.html patched successfully.");

        // 2. Update PLP-bad
        String plp = new String(Files.readAllBytes(plpPath), StandardCharsets.UTF_8);
        plp = injectCss(plp, getCommonCss() + badCheckboxStyle);
        plp = plp.replace(cartBtnTarget, cartBtnReplacement);
        plp = plp.replace("onclick=\"location.href='#'\">Shipping & Returns</div>", "onclick=\"location.href='shipping-bad.html'\">Shipping & Returns</div>");
        plp = plp.replace("onclick=\"location.href='#'\">Track Orders</div>", "onclick=\"location.href='track-orders-bad.html'\">Track Orders</div>");
        plp = plp.replace("onclick=\"location.href='#'\">FAQ</div>", "onclick=\"location.href='faq-bad.html'\">FAQ</div>");
        plp = plp.replace("onclick=\"location.href='#'\">Contact Us</div>", "onclick=\"location.href='contact-bad.html'\">Contact Us</div>");
        plp = plp.replace("onclick=\"location.href='#'\">About VÉRLA</div>", "onclick=\"location.href='about-bad.html'\">About VÉRLA</div>");
        plp = plp.replace("onclick=\"location.href='#'\">Our Stores</div>", "onclick=\"location.href='stores-bad.html'\">Our Stores</div>");
        plp = plp.replace("onclick=\"location.href='#'\">Careers</div>", "onclick=\"location.href='careers-bad.html'\">Careers</div>");

        final int plpModalStartIndex = plp.indexOf(countryModalTarget);
        if (plpModalStartIndex == -1)
        {
            throw new IOException("Could not locate country modal start in plp-bad.html");
        }
        plp = plp.substring(0, plpModalStartIndex) + restOfPage;
        Files.write(plpPath, plp.getBytes(StandardCharsets.UTF_8));
        System.out.println("plp-bad.html patched successfully.");

        // 3. Create bad subpages
        generateSubpages("bad", hp);
    }

    private static String getPerfectOverlayAndScriptsBlock(final String suffix)
    {
        final String cookieCheckboxes;
        if ("bad".equals(suffix))
        {
            cookieCheckboxes = "            <div style=\"margin-bottom: 16px; display: flex; align-items: flex-start; gap: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 12px; cursor: pointer;\" onclick=\"var c = this.querySelector('.bad-checkbox'); if (!c.classList.contains('disabled')) c.classList.toggle('checked');\">\n" +
                    "                <div class=\"bad-checkbox checked disabled\" id=\"cookie-essential\" style=\"margin-top: 4px;\"></div>\n" +
                    "                <div>\n" +
                    "                    <div style=\"font-weight: 600; font-size: 14px; color: var(--color-text-primary);\">Essential Cookies</div>\n" +
                    "                    <p style=\"font-size: 12px; color: var(--color-text-secondary);\">Required for system navigation, shopping cart, and region selection.</p>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "            <div style=\"margin-bottom: 16px; display: flex; align-items: flex-start; gap: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 12px; cursor: pointer;\" onclick=\"var c = this.querySelector('.bad-checkbox'); c.classList.toggle('checked');\">\n" +
                    "                <div class=\"bad-checkbox\" id=\"cookie-analytics\" style=\"margin-top: 4px;\"></div>\n" +
                    "                <div>\n" +
                    "                    <div style=\"font-weight: 600; font-size: 14px; color: var(--color-text-primary);\">Analytics Cookies</div>\n" +
                    "                    <p style=\"font-size: 12px; color: var(--color-text-secondary);\">Allows us to monitor site usage, traffic sources, and performance metrics.</p>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "            <div style=\"margin-bottom: 20px; display: flex; align-items: flex-start; gap: 12px; cursor: pointer;\" onclick=\"var c = this.querySelector('.bad-checkbox'); c.classList.toggle('checked');\">\n" +
                    "                <div class=\"bad-checkbox\" id=\"cookie-marketing\" style=\"margin-top: 4px;\"></div>\n" +
                    "                <div>\n" +
                    "                    <div style=\"font-weight: 600; font-size: 14px; color: var(--color-text-primary);\">Marketing Cookies</div>\n" +
                    "                    <p style=\"font-size: 12px; color: var(--color-text-secondary);\">Used to personalize advertisements and content based on your interests.</p>\n" +
                    "                </div>\n" +
                    "            </div>\n";
        }
        else
        {
            cookieCheckboxes = "            <div style=\"margin-bottom: 16px; display: flex; align-items: flex-start; gap: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 12px;\">\n" +
                    "                <input type=\"checkbox\" id=\"cookie-essential\" checked disabled style=\"margin-top: 4px;\">\n" +
                    "                <div>\n" +
                    "                    <label for=\"cookie-essential\" style=\"font-weight: 600; font-size: 14px; color: var(--color-text-primary);\">Essential Cookies</label>\n" +
                    "                    <p style=\"font-size: 12px; color: var(--color-text-secondary);\">Required for system navigation, shopping cart, and region selection.</p>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "            <div style=\"margin-bottom: 16px; display: flex; align-items: flex-start; gap: 12px; border-bottom: 1px solid var(--color-border); padding-bottom: 12px;\">\n" +
                    "                <input type=\"checkbox\" id=\"cookie-analytics\" style=\"margin-top: 4px;\">\n" +
                    "                <div>\n" +
                    "                    <label for=\"cookie-analytics\" style=\"font-weight: 600; font-size: 14px; color: var(--color-text-primary);\">Analytics Cookies</label>\n" +
                    "                    <p style=\"font-size: 12px; color: var(--color-text-secondary);\">Allows us to monitor site usage, traffic sources, and performance metrics.</p>\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "            <div style=\"margin-bottom: 20px; display: flex; align-items: flex-start; gap: 12px;\">\n" +
                    "                <input type=\"checkbox\" id=\"cookie-marketing\" style=\"margin-top: 4px;\">\n" +
                    "                <div>\n" +
                    "                    <label for=\"cookie-marketing\" style=\"font-weight: 600; font-size: 14px; color: var(--color-text-primary);\">Marketing Cookies</label>\n" +
                    "                    <p style=\"font-size: 12px; color: var(--color-text-secondary);\">Used to personalize advertisements and content based on your interests.</p>\n" +
                    "                </div>\n" +
                    "            </div>\n";
        }

        // We use slightly different wrapper IDs depending on suffix
        final String modalOverlayId = "bad".equals(suffix) ? "pop_m_bad" : ("normal".equals(suffix) ? "country_modal" : "country-modal-overlay");
        final String regionLblId = "bad".equals(suffix) ? "r_lbl_bad" : ("normal".equals(suffix) ? "region_lbl" : "current-region-label");
        final String cartBadgeId = "bad".equals(suffix) ? "cart_num" : ("normal".equals(suffix) ? "badge_cart" : "cart-count-badge");
        final String searchInputId = "bad".equals(suffix) ? "pop_srch" : "country-search-input";

        // Evaluate conditions in Java to construct correct JS
        final String badColorChange = "bad".equals(suffix) ? "dots[currentSlide].style.backgroundColor = 'var(--color-border)';" : "";
        final String badColorChangeActive = "bad".equals(suffix) ? "dots[currentSlide].style.backgroundColor = 'var(--color-accent)';" : "";

        final String cookieCheckboxesLoad;
        if ("bad".equals(suffix))
        {
            cookieCheckboxesLoad = "                        const anaBox = document.getElementById('cookie-analytics');\n" +
                    "                        const marBox = document.getElementById('cookie-marketing');\n" +
                    "                        if (localStorage.getItem('cookies-pref-analytics') === 'true') anaBox.classList.add('checked'); else anaBox.classList.remove('checked');\n" +
                    "                        if (localStorage.getItem('cookies-pref-marketing') === 'true') marBox.classList.add('checked'); else marBox.classList.remove('checked');\n";
        }
        else
        {
            cookieCheckboxesLoad = "                        if (cookieAnalyticsCb) cookieAnalyticsCb.checked = localStorage.getItem('cookies-pref-analytics') === 'true';\n" +
                    "                        if (cookieMarketingCb) cookieMarketingCb.checked = localStorage.getItem('cookies-pref-marketing') === 'true';\n";
        }

        final String cookieCheckboxesSave;
        if ("bad".equals(suffix))
        {
            cookieCheckboxesSave = "                    isAnalytics = document.getElementById('cookie-analytics').classList.contains('checked');\n" +
                    "                    isMarketing = document.getElementById('cookie-marketing').classList.contains('checked');\n";
        }
        else
        {
            cookieCheckboxesSave = "                    isAnalytics = cookieAnalyticsCb ? cookieAnalyticsCb.checked : false;\n" +
                    "                    isMarketing = cookieMarketingCb ? cookieMarketingCb.checked : false;\n";
        }

        return "    <!-- Country Selector Modal Overlay -->\n" +
                "    <div class=\"modal-overlay\" id=\"" + modalOverlayId + "\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"country-modal-title\">\n" +
                "        <div class=\"modal\" id=\"country-modal-box\">\n" +
                "            <button class=\"modal-close-btn\" id=\"country-modal-close\" aria-label=\"Close dialog\">&times;</button>\n" +
                "            <h3 class=\"modal-title\" id=\"country-modal-title\">Select Region</h3>\n" +
                "            <input type=\"text\" class=\"search-input\" id=\"" + searchInputId + "\" placeholder=\"Search country...\" aria-label=\"Search countries\">\n" +
                "            \n" +
                "            <div class=\"country-list-wrapper\" style=\"max-height: 220px; overflow-y: auto; border: 1px solid var(--color-border); margin-top: 8px;\">\n" +
                "                <div class=\"continent-group\" data-continent=\"Europe\">\n" +
                "                    <div class=\"continent-header\" style=\"font-weight: 600; font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--color-text-secondary); background: var(--color-bg-secondary); padding: 8px 16px; border-bottom: 1px solid var(--color-border); position: sticky; top: 0; z-index: 1;\">Europe</div>\n" +
                "                    <ul class=\"country-list\" style=\"list-style: none; border: none; max-height: none; overflow-y: visible;\">\n" +
                "                        <li class=\"country-item\" data-code=\"AT (€)\">Austria (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"BE (€)\">Belgium (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"DK (DKK)\">Denmark (DKK)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"FI (€)\">Finland (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"FR (€)\">France (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"DE (€)\">Germany (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"IE (€)\">Ireland (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"IT (€)\">Italy (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"NL (€)\">Netherlands (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"NO (NOK)\">Norway (NOK)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"PL (PLN)\">Poland (PLN)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"PT (€)\">Portugal (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"ES (€)\">Spain (€)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"SE (SEK)\">Sweden (SEK)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"CH (CHF)\">Switzerland (CHF)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"GB (£)\">United Kingdom (£)</li>\n" +
                "                    </ul>\n" +
                "                </div>\n" +
                "                <div class=\"continent-group\" data-continent=\"Americas\">\n" +
                "                    <div class=\"continent-header\" style=\"font-weight: 600; font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--color-text-secondary); background: var(--color-bg-secondary); padding: 8px 16px; border-bottom: 1px solid var(--color-border); position: sticky; top: 0; z-index: 1;\">Americas</div>\n" +
                "                    <ul class=\"country-list\" style=\"list-style: none; border: none; max-height: none; overflow-y: visible;\">\n" +
                "                        <li class=\"country-item\" data-code=\"AR ($)\">Argentina ($)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"BR (R$)\">Brazil (R$)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"CA ($)\">Canada ($)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"CL ($)\">Chile ($)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"CO ($)\">Colombia ($)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"MX (MXN)\">Mexico (MXN)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"PE (PEN)\">Peru (PEN)</li>\n" +
                "                        <li class=\"country-item selected\" data-code=\"US ($)\">United States ($)</li>\n" +
                "                    </ul>\n" +
                "                </div>\n" +
                "                <div class=\"continent-group\" data-continent=\"Asia/Oceania\">\n" +
                "                    <div class=\"continent-header\" style=\"font-weight: 600; font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--color-text-secondary); background: var(--color-bg-secondary); padding: 8px 16px; border-bottom: 1px solid var(--color-border); position: sticky; top: 0; z-index: 1;\">Asia / Oceania</div>\n" +
                "                    <ul class=\"country-list\" style=\"list-style: none; border: none; max-height: none; overflow-y: visible;\">\n" +
                "                        <li class=\"country-item\" data-code=\"AU ($)\">Australia ($)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"CN (¥)\">China (¥)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"HK ($)\">Hong Kong ($)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"IN (₹)\">India (₹)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"JP (¥)\">Japan (¥)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"KR (₩)\">South Korea (₩)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"NZ ($)\">New Zealand ($)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"SG ($)\">Singapore ($)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"TW (NT$)\">Taiwan (NT$)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"TH (฿)\">Thailand (฿)</li>\n" +
                "                        <li class=\"country-item\" data-code=\"VN (₫)\">Vietnam (₫)</li>\n" +
                "                    </ul>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Cookie Consent Banner -->\n" +
                "    <div class=\"cookie-banner\" id=\"cookie-consent-banner\" role=\"complementary\" aria-label=\"Cookie Consent Banner\">\n" +
                "        <p class=\"cookie-text\" id=\"cookie-banner-text\">We use cookies to optimize site design, personalize content, and analyze our traffic. By clicking Accept, you agree to our cookie policy.</p>\n" +
                "        <div class=\"cookie-actions\">\n" +
                "            <button class=\"cookie-btn cookie-btn-accept\" id=\"cookie-accept-all-btn\">Accept All</button>\n" +
                "            <button class=\"cookie-btn cookie-btn-decline\" id=\"cookie-decline-btn\">Preferences</button>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Cookie Preferences Modal Overlay -->\n" +
                "    <div class=\"modal-overlay\" id=\"cookie-preferences-modal-overlay\" role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"cookie-preferences-title\">\n" +
                "        <div class=\"modal\" id=\"cookie-preferences-modal-box\">\n" +
                "            <button class=\"modal-close-btn\" id=\"cookie-preferences-close\" aria-label=\"Close dialog\">&times;</button>\n" +
                "            <h3 class=\"modal-title\" id=\"cookie-preferences-title\">Cookie Preferences</h3>\n" +
                "            <p style=\"font-size: 13px; color: var(--color-text-secondary); margin-bottom: 20px; line-height: 1.5;\">\n" +
                "                Customize your cookie settings below. Essential cookies are required for the basic functionality of the website.\n" +
                "            </p>\n" +
                cookieCheckboxes +
                "            <button class=\"cookie-btn cookie-btn-accept\" id=\"cookie-save-preferences-btn\" style=\"width: 100%; justify-content: center; padding: 12px;\">Save Preferences</button>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Global Page Loader Overlay -->\n" +
                "    <div id=\"global-page-loader\">\n" +
                "        <div class=\"loader-spinner\"></div>\n" +
                "    </div>\n" +
                "\n" +
                "    <!-- Self-Contained JavaScript for Interactions -->\n" +
                "    <script>\n" +
                "        document.addEventListener('DOMContentLoaded', () => {\n" +
                "            // --- 1. Slideshow Carousel Logic ---\n" +
                "            const slides = document.querySelectorAll('#homepage-hero-carousel .carousel-slide, #carousel_box .carousel-slide, #sl_container .sl-shw-div');\n" +
                "            const dots = document.querySelectorAll('#homepage-hero-carousel .carousel-dot, #carousel_box .carousel-dot, #sl_container .c_dot_b');\n" +
                "            const prevBtn = document.getElementById('carousel-btn-prev') || document.getElementById('btn_prev');\n" +
                "            const nextBtn = document.getElementById('carousel-btn-next') || document.getElementById('btn_next');\n" +
                "            let currentSlide = 0;\n" +
                "            let slideInterval;\n" +
                "\n" +
                "            function showSlide(index) {\n" +
                "                if (!slides.length) return;\n" +
                "                slides[currentSlide].classList.remove('active');\n" +
                "                slides[currentSlide].setAttribute('aria-hidden', 'true');\n" +
                "                if (dots[currentSlide]) {\n" +
                "                    dots[currentSlide].classList.remove('active');\n" +
                "                    " + badColorChange + "\n" +
                "                }\n" +
                "\n" +
                "                currentSlide = (index + slides.length) % slides.length;\n" +
                "\n" +
                "                slides[currentSlide].classList.add('active');\n" +
                "                slides[currentSlide].setAttribute('aria-hidden', 'false');\n" +
                "                if (dots[currentSlide]) {\n" +
                "                    dots[currentSlide].classList.add('active');\n" +
                "                    " + badColorChangeActive + "\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            function nextSlide() {\n" +
                "                showSlide(currentSlide + 1);\n" +
                "            }\n" +
                "\n" +
                "            function prevSlide() {\n" +
                "                showSlide(currentSlide - 1);\n" +
                "            }\n" +
                "\n" +
                "            function startAutoplay() {\n" +
                "                if (slides.length) slideInterval = setInterval(nextSlide, 5000);\n" +
                "            }\n" +
                "\n" +
                "            function resetAutoplay() {\n" +
                "                clearInterval(slideInterval);\n" +
                "                startAutoplay();\n" +
                "            }\n" +
                "\n" +
                "            if (prevBtn) {\n" +
                "                prevBtn.addEventListener('click', () => {\n" +
                "                    prevSlide();\n" +
                "                    resetAutoplay();\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            if (nextBtn) {\n" +
                "                nextBtn.addEventListener('click', () => {\n" +
                "                    nextSlide();\n" +
                "                    resetAutoplay();\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            dots.forEach(dot => {\n" +
                "                dot.addEventListener('click', (e) => {\n" +
                "                    const slideIndex = parseInt(e.target.getAttribute('data-slide') || e.target.getAttribute('onclick')?.replace(/[^0-9]/g, '') || 0);\n" +
                "                    showSlide(slideIndex);\n" +
                "                    resetAutoplay();\n" +
                "                });\n" +
                "            });\n" +
                "\n" +
                "            startAutoplay();\n" +
                "\n" +
                "            // Expose carousel functions globally for inline onclick handlers across different pages\n" +
                "            window.prevSlide = () => { prevSlide(); resetAutoplay(); };\n" +
                "            window.nextSlide = () => { nextSlide(); resetAutoplay(); };\n" +
                "            window.gotoSlide = (idx) => { showSlide(idx); resetAutoplay(); };\n" +
                "            window.sh_prv = () => { prevSlide(); resetAutoplay(); };\n" +
                "            window.sh_nxt = () => { nextSlide(); resetAutoplay(); };\n" +
                "            window.sh_go = (idx) => { showSlide(idx); resetAutoplay(); };\n" +
                "\n" +
                "            // --- 2. Dynamic Cart & Dropdown Logic ---\n" +
                "            const cartBadge = document.getElementById('" + cartBadgeId + "');\n" +
                "            const quickAddButtons = document.querySelectorAll('.product-quick-add, .product-quick-add-bad, .btn-quick-add-bad');\n" +
                "            const cartWrapper = document.getElementById('cart-dropdown-wrapper');\n" +
                "            const cartBtn = document.getElementById('cart-btn') || document.getElementById('btn_cart') || document.getElementById('u_cart');\n" +
                "            const cartDropdown = document.getElementById('cart-dropdown-panel');\n" +
                "            const cartItemsContainer = document.getElementById('cart-dropdown-items');\n" +
                "            const cartSubtotalLabel = document.getElementById('cart-dropdown-subtotal');\n" +
                "            const cartFooter = document.getElementById('cart-dropdown-footer');\n" +
                "            let isCartSticky = false;\n" +
                "            let cartHoverTimeout;\n" +
                "            let cart = JSON.parse(sessionStorage.getItem('verla-cart') || '[]');\n" +
                "\n" +
                "            function showCartDropdown() {\n" +
                "                clearTimeout(cartHoverTimeout);\n" +
                "                if (cartDropdown) {\n" +
                "                    cartDropdown.style.display = 'block';\n" +
                "                    cartDropdown.offsetHeight; // force reflow\n" +
                "                    cartDropdown.style.opacity = '1';\n" +
                "                    cartDropdown.style.transform = 'translateY(0)';\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            function hideCartDropdown() {\n" +
                "                if (isCartSticky) return;\n" +
                "                if (cartDropdown) {\n" +
                "                    cartDropdown.style.opacity = '0';\n" +
                "                    cartDropdown.style.transform = 'translateY(10px)';\n" +
                "                    cartHoverTimeout = setTimeout(() => {\n" +
                "                        cartDropdown.style.display = 'none';\n" +
                "                    }, 300);\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            if (cartWrapper) {\n" +
                "                cartWrapper.addEventListener('mouseenter', showCartDropdown);\n" +
                "                cartWrapper.addEventListener('mouseleave', hideCartDropdown);\n" +
                "            }\n" +
                "\n" +
                "            if (cartBtn) {\n" +
                "                cartBtn.addEventListener('click', (e) => {\n" +
                "                    e.stopPropagation();\n" +
                "                    isCartSticky = !isCartSticky;\n" +
                "                    if (isCartSticky) {\n" +
                "                        showCartDropdown();\n" +
                "                    } else {\n" +
                "                        hideCartDropdown();\n" +
                "                    }\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            document.addEventListener('click', (e) => {\n" +
                "                if (cartWrapper && !cartWrapper.contains(e.target)) {\n" +
                "                    isCartSticky = false;\n" +
                "                    hideCartDropdown();\n" +
                "                }\n" +
                "            });\n" +
                "\n" +
                "            function updateCartUI() {\n" +
                "                if (!cartBadge) return;\n" +
                "                const totalCount = cart.reduce((sum, item) => sum + item.qty, 0);\n" +
                "                cartBadge.textContent = totalCount;\n" +
                "\n" +
                "                if (cart.length === 0) {\n" +
                "                    cartItemsContainer.innerHTML = '<p style=\"font-size: 13px; color: var(--color-text-secondary); text-align: center; padding: 20px 0;\" id=\"cart-empty-message\">Your cart is empty.</p>';\n" +
                "                    if (cartFooter) cartFooter.style.display = 'none';\n" +
                "                } else {\n" +
                "                    cartItemsContainer.innerHTML = '';\n" +
                "                    if (cartFooter) cartFooter.style.display = 'block';\n" +
                "                    let subtotal = 0;\n" +
                "\n" +
                "                    cart.forEach((item, index) => {\n" +
                "                        subtotal += item.price * item.qty;\n" +
                "                        const itemEl = document.createElement('div');\n" +
                "                        itemEl.style.display = 'flex';\n" +
                "                        itemEl.style.alignItems = 'center';\n" +
                "                        itemEl.style.gap = '12px';\n" +
                "                        itemEl.style.marginBottom = '12px';\n" +
                "                        itemEl.innerHTML = `\n" +
                "                            <div style=\"width: 40px; height: 40px; background: var(--color-bg-secondary); display: flex; align-items: center; justify-content: center; border: 1px solid var(--color-border); flex-shrink: 0;\">\n" +
                "                                ${item.svg}\n" +
                "                            </div>\n" +
                "                            <div style=\"flex: 1; min-width: 0;\">\n" +
                "                                <div style=\"font-size: 13px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--color-text-primary);\">${item.title}</div>\n" +
                "                                <div style=\"font-size: 11px; color: var(--color-text-secondary);\">${item.qty} &times; $${item.price.toFixed(2)}</div>\n" +
                "                            </div>\n" +
                "                            <button class=\"remove-cart-item\" data-index=\"${index}\" style=\"font-size: 11px; color: var(--color-accent); font-weight: 500;\">Remove</button>\n" +
                "                        `;\n" +
                "                        cartItemsContainer.appendChild(itemEl);\n" +
                "                    });\n" +
                "\n" +
                "                    if (cartSubtotalLabel) cartSubtotalLabel.textContent = `$${subtotal.toFixed(2)}`;\n" +
                "\n" +
                "                    cartItemsContainer.querySelectorAll('.remove-cart-item').forEach(btn => {\n" +
                "                        btn.addEventListener('click', (e) => {\n" +
                "                            e.stopPropagation();\n" +
                "                            const idx = parseInt(btn.getAttribute('data-index'));\n" +
                "                            cart.splice(idx, 1);\n" +
                "                            sessionStorage.setItem('verla-cart', JSON.stringify(cart));\n" +
                "                            updateCartUI();\n" +
                "                        });\n" +
                "                    });\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            // Setup Quick Add listeners\n" +
                "            document.addEventListener('click', (e) => {\n" +
                "                const btn = e.target.closest('.product-quick-add, .product-quick-add-bad, .btn-quick-add-bad');\n" +
                "                if (!btn || btn.disabled || btn.classList.contains('adding')) return;\n" +
                "\n" +
                "                // Don't add sold out items\n" +
                "                if (btn.textContent.trim().toLowerCase().includes('sold out')) return;\n" +
                "\n" +
                "                e.stopPropagation();\n" +
                "                const originalText = btn.textContent;\n" +
                "                btn.disabled = true;\n" +
                "                btn.textContent = 'Adding...';\n" +
                "                btn.classList.add('adding');\n" +
                "\n" +
                "                fetch('../index.html')\n" +
                "                    .then(() => {\n" +
                "                        const delay = Math.floor(Math.random() * 901) + 300;\n" +
                "                        setTimeout(() => {\n" +
                "                            const card = btn.closest('.product-card, .item-box, .product-card-bad');\n" +
                "                            if (card) {\n" +
                "                                const titleEl = card.querySelector('.product-title, .item-title, .product-title-bad, h3, h4');\n" +
                "                                const title = titleEl ? titleEl.textContent.trim() : 'Apparel Item';\n" +
                "                                \n" +
                "                                const priceEl = card.querySelector('.product-price, .item-price, .product-price-bad');\n" +
                "                                let priceText = '0.00';\n" +
                "                                if (priceEl) {\n" +
                "                                    const saleEl = priceEl.querySelector('.sale');\n" +
                "                                    priceText = saleEl ? saleEl.textContent : priceEl.textContent;\n" +
                "                                }\n" +
                "                                const price = parseFloat(priceText.replace(/[^0-9.]/g, '')) || 0.00;\n" +
                "                                \n" +
                "                                const svgEl = card.querySelector('svg');\n" +
                "                                const svgMarkup = svgEl ? svgEl.outerHTML : '';\n" +
                "\n" +
                "                                const existing = cart.find(item => item.title === title);\n" +
                "                                if (existing) {\n" +
                "                                    existing.qty++;\n" +
                "                                } else {\n" +
                "                                    cart.push({ title, price, svg: svgMarkup, qty: 1 });\n" +
                "                                }\n" +
                "                                sessionStorage.setItem('verla-cart', JSON.stringify(cart));\n" +
                "                                updateCartUI();\n" +
                "                            }\n" +
                "\n" +
                "                            btn.classList.remove('adding');\n" +
                "                            btn.classList.add('added');\n" +
                "                            btn.textContent = 'Added!';\n" +
                "\n" +
                "                            showCartDropdown();\n" +
                "                            setTimeout(() => {\n" +
                "                                hideCartDropdown();\n" +
                "                            }, 1500);\n" +
                "\n" +
                "                            setTimeout(() => {\n" +
                "                                btn.classList.remove('added');\n" +
                "                                btn.textContent = originalText;\n" +
                "                                btn.disabled = false;\n" +
                "                            }, 1000);\n" +
                "                        }, delay);\n" +
                "                    })\n" +
                "                    .catch(() => {\n" +
                "                        btn.classList.remove('adding');\n" +
                "                        btn.disabled = false;\n" +
                "                        btn.textContent = originalText;\n" +
                "                    });\n" +
                "            });\n" +
                "\n" +
                "            updateCartUI();\n" +
                "            window.c_add = () => {};\n" +
                "\n" +
                "            // --- 3. Country Selector Logic ---\n" +
                "            const countrySelectorBtn = document.getElementById('country-selector-btn') || document.getElementById('region_btn') || document.getElementById('u_select');\n" +
                "            const countryModalOverlay = document.getElementById('" + modalOverlayId + "');\n" +
                "            const countryModalClose = document.getElementById('country-modal-close') || countryModalOverlay?.querySelector('.modal-close-btn') || countryModalOverlay?.querySelector('div[onclick=\"close_m()\"]');\n" +
                "            const searchInput = document.getElementById('" + searchInputId + "');\n" +
                "            const countryItems = countryModalOverlay?.querySelectorAll('.country-item, .pop-li, .pop-liselected') || [];\n" +
                "            const currentRegionLabel = document.getElementById('" + regionLblId + "');\n" +
                "\n" +
                "            if (countrySelectorBtn) {\n" +
                "                countrySelectorBtn.addEventListener('click', () => {\n" +
                "                    if (countryModalOverlay) {\n" +
                "                        countryModalOverlay.classList.add('active');\n" +
                "                        countryModalOverlay.style.opacity = '1';\n" +
                "                        countryModalOverlay.style.visibility = 'visible';\n" +
                "                        if (countrySelectorBtn.setAttribute) countrySelectorBtn.setAttribute('aria-expanded', 'true');\n" +
                "                        if (searchInput) searchInput.focus();\n" +
                "                    }\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            function closeCountryModal() {\n" +
                "                if (countryModalOverlay) {\n" +
                "                    countryModalOverlay.classList.remove('active');\n" +
                "                    countryModalOverlay.style.opacity = '0';\n" +
                "                    countryModalOverlay.style.visibility = 'hidden';\n" +
                "                    if (countrySelectorBtn && countrySelectorBtn.setAttribute) countrySelectorBtn.setAttribute('aria-expanded', 'false');\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "            if (countryModalClose) {\n" +
                "                countryModalClose.addEventListener('click', (e) => {\n" +
                "                    e.stopPropagation();\n" +
                "                    closeCountryModal();\n" +
                "                });\n" +
                "            }\n" +
                "            if (countryModalOverlay) {\n" +
                "                countryModalOverlay.addEventListener('click', (e) => {\n" +
                "                    if (e.target === countryModalOverlay) closeCountryModal();\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            if (searchInput) {\n" +
                "                searchInput.addEventListener('input', (e) => {\n" +
                "                    const term = e.target.value.toLowerCase();\n" +
                "                    const groups = countryModalOverlay.querySelectorAll('.continent-group');\n" +
                "                    if (groups.length) {\n" +
                "                        groups.forEach(group => {\n" +
                "                            let visibleCount = 0;\n" +
                "                            const items = group.querySelectorAll('.country-item, .pop-li, .pop-liselected');\n" +
                "                            items.forEach(item => {\n" +
                "                                const text = item.textContent.toLowerCase();\n" +
                "                                if (text.includes(term)) {\n" +
                "                                    item.style.display = 'block';\n" +
                "                                    visibleCount++;\n" +
                "                                } else {\n" +
                "                                    item.style.display = 'none';\n" +
                "                                }\n" +
                "                            });\n" +
                "                            if (visibleCount > 0) {\n" +
                "                                group.style.display = 'block';\n" +
                "                            } else {\n" +
                "                                group.style.display = 'none';\n" +
                "                            }\n" +
                "                        });\n" +
                "                    } else {\n" +
                "                        // Simple filter if no continent groups\n" +
                "                        countryItems.forEach(item => {\n" +
                "                            const text = item.textContent.toLowerCase();\n" +
                "                            if (text.includes(term)) {\n" +
                "                                item.style.display = 'block';\n" +
                "                            } else {\n" +
                "                                item.style.display = 'none';\n" +
                "                            }\n" +
                "                        });\n" +
                "                    }\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            countryItems.forEach(item => {\n" +
                "                item.addEventListener('click', (e) => {\n" +
                "                    e.stopPropagation();\n" +
                "                    countryItems.forEach(el => {\n" +
                "                        el.classList.remove('selected');\n" +
                "                        el.classList.remove('pop-liselected');\n" +
                "                        if (el.setAttribute) el.setAttribute('aria-selected', 'false');\n" +
                "                    });\n" +
                "                    item.classList.add('selected');\n" +
                "                    if (item.classList.contains('pop-li')) item.classList.add('pop-liselected');\n" +
                "                    if (item.setAttribute) item.setAttribute('aria-selected', 'true');\n" +
                "                    \n" +
                "                    const code = item.getAttribute('data-code');\n" +
                "                    if (currentRegionLabel) currentRegionLabel.textContent = code;\n" +
                "                    closeCountryModal();\n" +
                "                });\n" +
                "            });\n" +
                "            window.open_m = () => {\n" +
                "                if (countryModalOverlay) {\n" +
                "                    countryModalOverlay.classList.add('active');\n" +
                "                    countryModalOverlay.style.opacity = '1';\n" +
                "                    countryModalOverlay.style.visibility = 'visible';\n" +
                "                    if (searchInput) searchInput.focus();\n" +
                "                }\n" +
                "            };\n" +
                "            window.close_m = () => {\n" +
                "                closeCountryModal();\n" +
                "            };\n" +
                "\n" +
                "            // --- 4. Newsletter Form Logic ---\n" +
                "            const newsletterForm = document.getElementById('newsletter-email-form') || document.getElementById('newsletter_form');\n" +
                "            const emailInput = document.getElementById('newsletter-email-input') || document.getElementById('email_input');\n" +
                "            const successMessage = document.getElementById('newsletter-success-message') || document.getElementById('success_msg');\n" +
                "\n" +
                "            if (newsletterForm && emailInput && successMessage) {\n" +
                "                newsletterForm.addEventListener('submit', (e) => {\n" +
                "                    e.preventDefault();\n" +
                "                    if (emailInput.checkValidity()) {\n" +
                "                        successMessage.style.display = 'block';\n" +
                "                        newsletterForm.style.display = 'none';\n" +
                "                    }\n" +
                "                });\n" +
                "            }\n" +
                "            window.sb_m = () => {\n" +
                "                const valEl = document.getElementById('mail_val') || document.getElementById('email_input') || document.getElementById('newsletter-email-input');\n" +
                "                const okEl = document.getElementById('ok_lbl') || document.getElementById('success_msg') || document.getElementById('newsletter-success-message');\n" +
                "                const boxEl = document.getElementById('n_box') || document.getElementById('newsletter_form') || document.getElementById('newsletter-email-form');\n" +
                "                if (valEl && valEl.value) {\n" +
                "                    if (okEl) okEl.style.display = 'block';\n" +
                "                    if (boxEl) boxEl.style.display = 'none';\n" +
                "                }\n" +
                "            };\n" +
                "\n" +
                "            // --- 5. Cookie Consent & Preferences Logic ---\n" +
                "            const cookieBanner = document.getElementById('cookie-consent-banner') || document.getElementById('cookie_banner') || document.getElementById('ck_b_bad');\n" +
                "            const acceptBtn = document.getElementById('cookie-accept-all-btn') || cookieBanner?.querySelector('.cookie-btn-accept') || cookieBanner?.querySelector('div[onclick=\"ck_ok()\"]');\n" +
                "            const declineBtn = document.getElementById('cookie-decline-btn') || cookieBanner?.querySelector('.cookie-btn-decline') || cookieBanner?.querySelector('div[onclick=\"ck_prefs()\"]') || cookieBanner?.querySelectorAll('div')[1];\n" +
                "            \n" +
                "            const cookiePrefsModal = document.getElementById('cookie-preferences-modal-overlay');\n" +
                "            const cookiePrefsClose = document.getElementById('cookie-preferences-close');\n" +
                "            const cookieSavePrefsBtn = document.getElementById('cookie-save-preferences-btn');\n" +
                "            const cookieAnalyticsCb = document.getElementById('cookie-analytics');\n" +
                "            const cookieMarketingCb = document.getElementById('cookie-marketing');\n" +
                "\n" +
                "            const consentKey = 'cookies-accepted-' + \"" + suffix + "\";\n" +
                "            if (cookieBanner && !localStorage.getItem(consentKey)) {\n" +
                "                setTimeout(() => {\n" +
                "                    cookieBanner.classList.add('active');\n" +
                "                    cookieBanner.style.transform = 'translateY(0)';\n" +
                "                }, 1000);\n" +
                "            }\n" +
                "\n" +
                "            function dismissCookieBanner() {\n" +
                "                if (cookieBanner) {\n" +
                "                    cookieBanner.classList.remove('active');\n" +
                "                    cookieBanner.style.transform = 'translateY(150%)';\n" +
                "                }\n" +
                "                localStorage.setItem(consentKey, 'true');\n" +
                "            }\n" +
                "\n" +
                "            if (acceptBtn) {\n" +
                "                acceptBtn.addEventListener('click', (e) => {\n" +
                "                    e.stopPropagation();\n" +
                "                    dismissCookieBanner();\n" +
                "                    localStorage.setItem('cookies-pref-analytics', 'true');\n" +
                "                    localStorage.setItem('cookies-pref-marketing', 'true');\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            if (declineBtn) {\n" +
                "                declineBtn.addEventListener('click', (e) => {\n" +
                "                    e.stopPropagation();\n" +
                "                    if (cookiePrefsModal) {\n" +
                cookieCheckboxesLoad +
                "                        cookiePrefsModal.classList.add('active');\n" +
                "                    }\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            function closePrefsModal() {\n" +
                "                if (cookiePrefsModal) cookiePrefsModal.classList.remove('active');\n" +
                "            }\n" +
                "\n" +
                "            if (cookiePrefsClose) cookiePrefsClose.addEventListener('click', closePrefsModal);\n" +
                "            if (cookiePrefsModal) {\n" +
                "                cookiePrefsModal.addEventListener('click', (e) => {\n" +
                "                    if (e.target === cookiePrefsModal) closePrefsModal();\n" +
                "                });\n" +
                "            }\n" +
                "\n" +
                "            if (cookieSavePrefsBtn) {\n" +
                "                cookieSavePrefsBtn.addEventListener('click', () => {\n" +
                "                    let isAnalytics = false;\n" +
                "                    let isMarketing = false;\n" +
                cookieCheckboxesSave +
                "                    localStorage.setItem('cookies-pref-analytics', isAnalytics.toString());\n" +
                "                    localStorage.setItem('cookies-pref-marketing', isMarketing.toString());\n" +
                "                    closePrefsModal();\n" +
                "                    dismissCookieBanner();\n" +
                "                });\n" +
                "            }\n" +
                "            window.ck_ok = () => {\n" +
                "                dismissCookieBanner();\n" +
                "                localStorage.setItem('cookies-pref-analytics', 'true');\n" +
                "                localStorage.setItem('cookies-pref-marketing', 'true');\n" +
                "            };\n" +
                "            window.ck_prefs = () => {\n" +
                "                if (cookiePrefsModal) {\n" +
                "                    " + cookieCheckboxesLoad + "\n" +
                "                    cookiePrefsModal.classList.add('active');\n" +
                "                }\n" +
                "            };\n" +
                "\n" +
                "            // --- 6. Global Delayed Navigation Interceptor ---\n" +
                "            document.addEventListener('click', (e) => {\n" +
                "                const anchor = e.target.closest('a') || e.target.closest('.x-nav-clickable') || (e.target.getAttribute('onclick')?.includes('location.href') ? e.target : null);\n" +
                "                if (anchor) {\n" +
                "                    let url = anchor.getAttribute('href') || anchor.getAttribute('onclick')?.match(/'([^']+)'/)?.[1];\n" +
                "                    if (url && !url.includes('#') && !anchor.getAttribute('target') && !url.startsWith('javascript:')) {\n" +
                "                        // Make sure to resolve the relative URL\n" +
                "                        e.preventDefault();\n" +
                "                        e.stopPropagation();\n" +
                "                        const loader = document.getElementById('global-page-loader');\n" +
                "                        if (loader) loader.style.display = 'flex';\n" +
                "                        const delay = Math.floor(Math.random() * (1200 - 300 + 1)) + 300;\n" +
                "                        setTimeout(() => {\n" +
                "                            window.location.href = url;\n" +
                "                        }, delay);\n" +
                "                    }\n" +
                "                }\n" +
                "            });\n" +
                "        });\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>\n";
    }

    private static void generateSubpages(final String suffix, final String skeleton) throws IOException
    {
        final String[] subpageNames = {"about", "shipping", "faq", "contact", "track-orders", "careers", "stores"};
        for (final String name : subpageNames)
        {
            final Path path = Paths.get(BASE_DIR, name + "-" + suffix + ".html");
            final String content = getSubpageContent(name, suffix);
            
            // Replace the main body block in the skeleton with this subpage's specific content
            String subHtml;
            if ("perfect".equals(suffix))
            {
                final int startMain = skeleton.indexOf("<main id=\"main-content\">");
                final int endMain = skeleton.indexOf("</main>");
                if (startMain == -1 || endMain == -1)
                {
                    throw new IOException("Could not find main tag in perfect skeleton.");
                }
                subHtml = skeleton.substring(0, startMain) + "<main id=\"main-content\">\n" + content + "\n        </main>" + skeleton.substring(endMain + 7);
            }
            else if ("normal".equals(suffix))
            {
                final int startMain = skeleton.indexOf("<div class=\"main-body\">");
                final int endMain = skeleton.indexOf("<!-- Footer Section -->");
                if (startMain == -1 || endMain == -1)
                {
                    throw new IOException("Could not find main-body div in normal skeleton.");
                }
                subHtml = skeleton.substring(0, startMain) + "<div class=\"main-body\">\n" + content + "\n    </div>\n\n    " + skeleton.substring(endMain);
            }
            else // bad
            {
                final int startMain = skeleton.indexOf("<!-- Main Content Area -->");
                final int endMain = skeleton.indexOf("<!-- Footer -->");
                if (startMain == -1 || endMain == -1)
                {
                    throw new IOException("Could not find Main Content Area comment in bad skeleton.");
                }
                // The main content area in bad version starts after the next <div> tag following the comment
                final int nextDiv = skeleton.indexOf("<div>", startMain);
                // Find matching closing div for that nextDiv
                final int endDiv = nextMainDivCloseIndex(skeleton, nextDiv);
                
                subHtml = skeleton.substring(0, nextDiv) + "<div style=\"padding: 40px 0;\">\n" + content + "\n    </div>" + skeleton.substring(endDiv);
            }

            // Adjust Page Title in <title> tag
            subHtml = subHtml.replaceAll("<title>[^<]+</title>", "<title>" + capitalize(name) + " | VÉRLA</title>");

            // Write the file
            Files.write(path, subHtml.getBytes(StandardCharsets.UTF_8));
            System.out.println("Subpage generated: " + path.getFileName());
        }
    }

    private static int nextMainDivCloseIndex(final String html, final int startDiv)
    {
        // Simple brace matcher to match nested divs
        int count = 0;
        int i = startDiv;
        while (i < html.length())
        {
            if (html.startsWith("<div", i))
            {
                count++;
                i += 4;
            }
            else if (html.startsWith("</div>", i))
            {
                count--;
                if (count == 0)
                {
                    return i + 6;
                }
                i += 6;
            }
            else
            {
                i++;
            }
        }
        return html.indexOf("<!-- Footer -->");
    }

    private static String capitalize(final String name)
    {
        if (name == null || name.isEmpty()) return "";
        if ("track-orders".equals(name)) return "Track Orders";
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String getSubpageContent(final String name, final String suffix)
    {
        // Generate content depending on name and quality suffix
        switch (name)
        {
            case "about":
                return getAboutContent(suffix);
            case "shipping":
                return getShippingContent(suffix);
            case "faq":
                return getFaqContent(suffix);
            case "contact":
                return getContactContent(suffix);
            case "track-orders":
                return getTrackOrdersContent(suffix);
            case "careers":
                return getCareersContent(suffix);
            case "stores":
                return getStoresContent(suffix);
            default:
                return "<div>Content not found.</div>";
        }
    }

    private static String getAboutContent(final String suffix)
    {
        if ("perfect".equals(suffix))
        {
            return "            <section class=\"container\" style=\"padding: 80px 0; max-width: 800px;\">\n" +
                    "                <h1 style=\"font-family: var(--font-family-serif); font-size: 36px; font-weight: 400; margin-bottom: 24px; text-align: center;\">Our Story</h1>\n" +
                    "                <p style=\"font-size: 16px; line-height: 1.8; color: var(--color-text-secondary); margin-bottom: 20px;\">\n" +
                    "                    Established in 2026, VÉRLA was founded on the philosophy of quiet luxury and absolute sustainability. We believe clothing should be a second skin—effortless, beautiful, and completely respectful of the environment.\n" +
                    "                </p>\n" +
                    "                <p style=\"font-size: 16px; line-height: 1.8; color: var(--color-text-secondary); margin-bottom: 20px;\">\n" +
                    "                    Every piece in our collection is crafted from organically certified fibers, sourced from farmers who prioritize biodiversity and fair labor. From our breathable linen shirts to our textured ribbed knitwear, each silhouette is designed to transcend seasons and trends.\n" +
                    "                </p>\n" +
                    "                <p style=\"font-size: 16px; line-height: 1.8; color: var(--color-text-secondary);\">\n" +
                    "                    We work in small, localized batches to eliminate excess stock and waste. VÉRLA isn't just about fashion; it's a movement towards mindful living, meticulous craftsmanship, and honest production.\n" +
                    "                </p>\n" +
                    "            </section>";
        }
        else if ("normal".equals(suffix))
        {
            return "        <div class=\"container\" style=\"padding: 60px 0; max-width: 800px;\">\n" +
                    "            <h2>About VÉRLA</h2>\n" +
                    "            <p style=\"margin-top: 15px; color: #555;\">\n" +
                    "                VÉRLA is a modern apparel brand that focuses on simplicity, premium fabrics, and eco-friendly manufacturing. We work directly with raw fiber growers to ensure that all textiles are organic and chemical-free.\n" +
                    "            </p>\n" +
                    "            <p style=\"margin-top: 15px; color: #555;\">\n" +
                    "                Our design studio works meticulously to engineer comfortable clothing for everyday wear. Our selection contains timeless garments built to last, supporting the slow-fashion initiative.\n" +
                    "            </p>\n" +
                    "        </div>";
        }
        else
        {
            return "        <div style=\"padding: 40px; max-width: 800px; margin: 0 auto;\">\n" +
                    "            <div style=\"font-size: 24px; font-weight: bold; margin-bottom: 20px;\">About VÉRLA Brand</div>\n" +
                    "            <div style=\"font-size: 14px; margin-bottom: 10px; color: #666;\">VÉRLA is a top luxury store launched in 2026. We sell organic wear.</div>\n" +
                    "            <div style=\"font-size: 14px; color: #666;\">We care about environment. Our linen is best in class. Shop our new shirts!</div>\n" +
                    "        </div>";
        }
    }

    private static String getShippingContent(final String suffix)
    {
        if ("perfect".equals(suffix))
        {
            return "            <section class=\"container\" style=\"padding: 80px 0; max-width: 900px;\">\n" +
                    "                <h1 style=\"font-family: var(--font-family-serif); font-size: 36px; font-weight: 400; margin-bottom: 24px;\">Shipping & Returns</h1>\n" +
                    "                <p style=\"color: var(--color-text-secondary); margin-bottom: 32px;\">We ship worldwide. All orders are processed in eco-friendly packaging from our carbon-neutral hub.</p>\n" +
                    "                \n" +
                    "                <h2 style=\"font-family: var(--font-family-serif); font-size: 20px; font-weight: 400; margin-bottom: 16px;\">Shipping Methods & Rates</h2>\n" +
                    "                <table style=\"width: 100%; border-collapse: collapse; margin-bottom: 40px; font-size: 14px;\">\n" +
                    "                    <thead>\n" +
                    "                        <tr style=\"border-bottom: 2px solid var(--color-text-primary); text-align: left;\">\n" +
                    "                            <th style=\"padding: 12px 8px; font-weight: 600;\">Method</th>\n" +
                    "                            <th style=\"padding: 12px 8px; font-weight: 600;\">Delivery Time</th>\n" +
                    "                            <th style=\"padding: 12px 8px; font-weight: 600;\">Rate (Orders < $150)</th>\n" +
                    "                            <th style=\"padding: 12px 8px; font-weight: 600;\">Rate (Orders > $150)</th>\n" +
                    "                        </tr>\n" +
                    "                    </thead>\n" +
                    "                    <tbody>\n" +
                    "                        <tr style=\"border-bottom: 1px solid var(--color-border);\">\n" +
                    "                            <td style=\"padding: 16px 8px;\">Standard Shipping</td>\n" +
                    "                            <td style=\"padding: 16px 8px;\">3-5 Business Days</td>\n" +
                    "                            <td style=\"padding: 16px 8px;\">$8.00</td>\n" +
                    "                            <td style=\"padding: 16px 8px; color: var(--color-success); font-weight: 500;\">Free</td>\n" +
                    "                        </tr>\n" +
                    "                        <tr style=\"border-bottom: 1px solid var(--color-border);\">\n" +
                    "                            <td style=\"padding: 16px 8px;\">Express Delivery</td>\n" +
                    "                            <td style=\"padding: 16px 8px;\">1-2 Business Days</td>\n" +
                    "                            <td style=\"padding: 16px 8px;\">$15.00</td>\n" +
                    "                            <td style=\"padding: 16px 8px;\">$15.00</td>\n" +
                    "                        </tr>\n" +
                    "                        <tr style=\"border-bottom: 1px solid var(--color-border);\">\n" +
                    "                            <td style=\"padding: 16px 8px;\">International Courier</td>\n" +
                    "                            <td style=\"padding: 16px 8px;\">5-10 Business Days</td>\n" +
                    "                            <td style=\"padding: 16px 8px;\">$25.00</td>\n" +
                    "                            <td style=\"padding: 16px 8px;\">$25.00</td>\n" +
                    "                        </tr>\n" +
                    "                    </tbody>\n" +
                    "                </table>\n" +
                    "                \n" +
                    "                <h2 style=\"font-family: var(--font-family-serif); font-size: 20px; font-weight: 400; margin-bottom: 16px;\">Returns Policy</h2>\n" +
                    "                <p style=\"font-size: 15px; color: var(--color-text-secondary); line-height: 1.6;\">\n" +
                    "                    We offer free returns within 30 days of receiving your package. Items must be in their original condition, unworn, unwashed, and with all product labels attached. Please use the prepaid return label included in your original package to drop off your return at any local carrier drop-box.\n" +
                    "                </p>\n" +
                    "            </section>";
        }
        else if ("normal".equals(suffix))
        {
            return "        <div class=\"container\" style=\"padding: 60px 0; max-width: 900px;\">\n" +
                    "            <h2>Shipping & Returns</h2>\n" +
                    "            <p style=\"color: #666; margin-bottom: 20px;\">We ship orders quickly with zero plastic packaging material.</p>\n" +
                    "            \n" +
                    "            <table style=\"width: 100%; border: 1px solid #ddd; border-collapse: collapse; margin-top: 20px;\">\n" +
                    "                <tr style=\"background: #f8f9fa; border-bottom: 1px solid #ddd;\">\n" +
                    "                    <th style=\"padding: 10px; text-align: left;\">Option</th>\n" +
                    "                    <th style=\"padding: 10px; text-align: left;\">Time</th>\n" +
                    "                    <th style=\"padding: 10px; text-align: left;\">Price</th>\n" +
                    "                </tr>\n" +
                    "                <tr style=\"border-bottom: 1px solid #ddd;\">\n" +
                    "                    <td style=\"padding: 10px;\">Standard</td>\n" +
                    "                    <td style=\"padding: 10px;\">3-5 days</td>\n" +
                    "                    <td style=\"padding: 10px;\">$8.00 (Free over $150)</td>\n" +
                    "                </tr>\n" +
                    "                <tr>\n" +
                    "                    <td style=\"padding: 10px;\">Express</td>\n" +
                    "                    <td style=\"padding: 10px;\">1-2 days</td>\n" +
                    "                    <td style=\"padding: 10px;\">$15.00</td>\n" +
                    "                </tr>\n" +
                    "            </table>\n" +
                    "        </div>";
        }
        else
        {
            return "        <div style=\"padding: 40px; max-width: 900px; margin: 0 auto;\">\n" +
                    "            <div style=\"font-size: 24px; font-weight: bold; margin-bottom: 20px;\">Shipping & Returns Information</div>\n" +
                    "            <div style=\"margin: 10px 0;\">Standard delivery is $8. Free shipping if cart is over $150.</div>\n" +
                    "            <div style=\"margin: 10px 0;\">Express courier runs $15. No international shipping to PO boxes.</div>\n" +
                    "            <div style=\"margin: 10px 0;\">30 day returns allowed if tags are intact. Please drop off at carrier station.</div>\n" +
                    "        </div>";
        }
    }

    private static String getFaqContent(final String suffix)
    {
        if ("perfect".equals(suffix))
        {
            return "            <section class=\"container\" style=\"padding: 80px 0; max-width: 800px;\">\n" +
                    "                <h1 style=\"font-family: var(--font-family-serif); font-size: 36px; font-weight: 400; margin-bottom: 16px; text-align: center;\">Frequently Asked Questions</h1>\n" +
                    "                <p style=\"text-align: center; color: var(--color-text-secondary); margin-bottom: 40px;\">Find quick answers about our organic fabrics, sizing, and operations.</p>\n" +
                    "                \n" +
                    "                <div style=\"display: flex; flex-direction: column; gap: 16px;\">\n" +
                    "                    <details style=\"border: 1px solid var(--color-border); padding: 20px; cursor: pointer;\" class=\"faq-item\">\n" +
                    "                        <summary style=\"font-weight: 600; font-size: 16px; outline: none; list-style: none; display: flex; justify-content: space-between; align-items: center;\">\n" +
                    "                            <span>What makes VÉRLA linen special?</span>\n" +
                    "                            <span style=\"color: var(--color-accent);\">&darr;</span>\n" +
                    "                        </summary>\n" +
                    "                        <p style=\"margin-top: 12px; font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;\">\n" +
                    "                            Our linen is organic and woven in Northern Europe. We use rain-fed flax crops that do not require artificial irrigation or heavy pesticides. The resulting fiber is durable, thermoregulating, and softens with every wash.\n" +
                    "                        </p>\n" +
                    "                    </details>\n" +
                    "                    <details style=\"border: 1px solid var(--color-border); padding: 20px; cursor: pointer;\" class=\"faq-item\">\n" +
                    "                        <summary style=\"font-weight: 600; font-size: 16px; outline: none; list-style: none; display: flex; justify-content: space-between; align-items: center;\">\n" +
                    "                            <span>How do I care for my knitwear?</span>\n" +
                    "                            <span style=\"color: var(--color-accent);\">&darr;</span>\n" +
                    "                        </summary>\n" +
                    "                        <p style=\"margin-top: 12px; font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;\">\n" +
                    "                            We recommend hand washing in cold water using a gentle, eco-friendly wool wash. Lay flat on a clean dry towel to dry. Never tumble dry or hang wet knitwear, as this stretches the delicate stitch structure.\n" +
                    "                        </p>\n" +
                    "                    </details>\n" +
                    "                    <details style=\"border: 1px solid var(--color-border); padding: 20px; cursor: pointer;\" class=\"faq-item\">\n" +
                    "                        <summary style=\"font-weight: 600; font-size: 16px; outline: none; list-style: none; display: flex; justify-content: space-between; align-items: center;\">\n" +
                    "                            <span>Can I update my order after placing it?</span>\n" +
                    "                            <span style=\"color: var(--color-accent);\">&darr;</span>\n" +
                    "                        </summary>\n" +
                    "                        <p style=\"margin-top: 12px; font-size: 14px; color: var(--color-text-secondary); line-height: 1.6;\">\n" +
                    "                            Because we pack and ship orders swiftly, order updates can only be processed within 30 minutes of placing the order. Please reach out to customer service as soon as possible.\n" +
                    "                        </p>\n" +
                    "                    </details>\n" +
                    "                </div>\n" +
                    "            </section>";
        }
        else if ("normal".equals(suffix))
        {
            return "        <div class=\"container\" style=\"padding: 60px 0; max-width: 800px;\">\n" +
                    "            <h2>FAQ</h2>\n" +
                    "            <p style=\"color: #666; margin-bottom: 20px;\">Frequently Asked Questions and helpful support topics.</p>\n" +
                    "            \n" +
                    "            <div style=\"margin-top: 20px;\">\n" +
                    "                <div class=\"faq-q\" style=\"font-weight: bold; padding: 10px; background: #eee; cursor: pointer; margin-top: 10px;\" onclick=\"this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'block' ? 'none' : 'block';\">\n" +
                    "                    What is your linen quality?\n" +
                    "                </div>\n" +
                    "                <div class=\"faq-a\" style=\"display: none; padding: 10px; border: 1px solid #eee;\">\n" +
                    "                    Our linen is premium and rain-fed, meaning it requires less chemical treatment and maintains high breathability.\n" +
                    "                </div>\n" +
                    "                \n" +
                    "                <div class=\"faq-q\" style=\"font-weight: bold; padding: 10px; background: #eee; cursor: pointer; margin-top: 10px;\" onclick=\"this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'block' ? 'none' : 'block';\">\n" +
                    "                    How to return items?\n" +
                    "                </div>\n" +
                    "                <div class=\"faq-a\" style=\"display: none; padding: 10px; border: 1px solid #eee;\">\n" +
                    "                    Use the pre-printed postal returns sticker inside your retail package and drop off at the carrier post box.\n" +
                    "                </div>\n" +
                    "            </div>\n" +
                    "        </div>";
        }
        else
        {
            return "        <div style=\"padding: 40px; max-width: 800px; margin: 0 auto;\">\n" +
                    "            <div style=\"font-size: 24px; font-weight: bold; margin-bottom: 20px;\">FAQ & Help Desk</div>\n" +
                    "            <div style=\"margin: 10px 0; border: 1px solid #ccc; padding: 10px;\">\n" +
                    "                <div style=\"font-weight: bold; cursor: pointer;\" onclick=\"var s=this.nextElementSibling.style; s.display=s.display=='block'?'none':'block';\">Where do you ship?</div>\n" +
                    "                <div style=\"display: none; margin-top: 10px; color:#666;\">We ship globally from our Germany hub.</div>\n" +
                    "            </div>\n" +
                    "            <div style=\"margin: 10px 0; border: 1px solid #ccc; padding: 10px;\">\n" +
                    "                <div style=\"font-weight: bold; cursor: pointer;\" onclick=\"var s=this.nextElementSibling.style; s.display=s.display=='block'?'none':'block';\">Are fabrics sustainable?</div>\n" +
                    "                <div style=\"display: none; margin-top: 10px; color:#666;\">Yes, our organic linen flax fibers are certified safe.</div>\n" +
                    "            </div>\n" +
                    "        </div>";
        }
    }

    private static String getContactContent(final String suffix)
    {
        if ("perfect".equals(suffix))
        {
            return "            <section class=\"container\" style=\"padding: 80px 0; max-width: 500px;\">\n" +
                    "                <h1 style=\"font-family: var(--font-family-serif); font-size: 36px; font-weight: 400; margin-bottom: 12px; text-align: center;\">Contact Us</h1>\n" +
                    "                <p style=\"text-align: center; color: var(--color-text-secondary); margin-bottom: 32px;\">Send us a message and we'll reply within 24 hours.</p>\n" +
                    "                \n" +
                    "                <form id=\"contact-form-el\" style=\"display: flex; flex-direction: column; gap: 16px;\">\n" +
                    "                    <div>\n" +
                    "                        <label for=\"c-name\" style=\"display: block; font-size: 12px; text-transform: uppercase; font-weight: 600; margin-bottom: 6px;\">Name</label>\n" +
                    "                        <input type=\"text\" id=\"c-name\" required style=\"width:100%; padding:12px; border:1px solid var(--color-border); outline:none;\">\n" +
                    "                    </div>\n" +
                    "                    <div>\n" +
                    "                        <label for=\"c-email\" style=\"display: block; font-size: 12px; text-transform: uppercase; font-weight: 600; margin-bottom: 6px;\">Email Address</label>\n" +
                    "                        <input type=\"email\" id=\"c-email\" required style=\"width:100%; padding:12px; border:1px solid var(--color-border); outline:none;\">\n" +
                    "                    </div>\n" +
                    "                    <div>\n" +
                    "                        <label for=\"c-message\" style=\"display: block; font-size: 12px; text-transform: uppercase; font-weight: 600; margin-bottom: 6px;\">Message</label>\n" +
                    "                        <textarea id=\"c-message\" required rows=\"5\" style=\"width:100%; padding:12px; border:1px solid var(--color-border); outline:none; resize:none;\"></textarea>\n" +
                    "                    </div>\n" +
                    "                    <button type=\"submit\" style=\"background:var(--color-text-primary); color:white; padding:14px; font-size:12px; text-transform:uppercase; letter-spacing:0.05em; font-weight:600; border:none; cursor:pointer;\">Send Message</button>\n" +
                    "                </form>\n" +
                    "                <div id=\"contact-success-msg\" style=\"display:none; text-align:center; padding: 24px; border: 1px solid var(--color-border); background: var(--color-bg-secondary); color: var(--color-success); font-weight:500; margin-top:20px;\">\n" +
                    "                    Message received! Thank you for reaching out to VÉRLA.\n" +
                    "                </div>\n" +
                    "            </section>\n" +
                    "            <script>\n" +
                    "                document.addEventListener('DOMContentLoaded', () => {\n" +
                    "                    const form = document.getElementById('contact-form-el');\n" +
                    "                    const success = document.getElementById('contact-success-msg');\n" +
                    "                    if (form) {\n" +
                    "                        form.addEventListener('submit', (e) => {\n" +
                    "                            e.preventDefault();\n" +
                    "                            const loader = document.getElementById('global-page-loader');\n" +
                    "                            if (loader) loader.style.display = 'flex';\n" +
                    "                            setTimeout(() => {\n" +
                    "                                if (loader) loader.style.display = 'none';\n" +
                    "                                form.style.display = 'none';\n" +
                    "                                if (success) success.style.display = 'block';\n" +
                    "                            }, 1000);\n" +
                    "                        });\n" +
                    "                    }\n" +
                    "                });\n" +
                    "            </script>";
        }
        else if ("normal".equals(suffix))
        {
            return "        <div class=\"container\" style=\"padding: 60px 0; max-width: 500px;\">\n" +
                    "            <h2>Contact Us</h2>\n" +
                    "            <p style=\"color:#666; margin-bottom:20px;\">Contact VÉRLA retail support.</p>\n" +
                    "            \n" +
                    "            <form id=\"normal-contact-form\">\n" +
                    "                <div style=\"margin-top: 12px;\">\n" +
                    "                    <label>Name:</label><br>\n" +
                    "                    <input type=\"text\" id=\"normal-c-name\" required style=\"width:100%; padding:10px;\">\n" +
                    "                </div>\n" +
                    "                <div style=\"margin-top: 12px;\">\n" +
                    "                    <label>Email:</label><br>\n" +
                    "                    <input type=\"email\" id=\"normal-c-email\" required style=\"width:100%; padding:10px;\">\n" +
                    "                </div>\n" +
                    "                <div style=\"margin-top: 12px;\">\n" +
                    "                    <label>Message:</label><br>\n" +
                    "                    <textarea id=\"normal-c-msg\" required rows=\"4\" style=\"width:100%; padding:10px;\"></textarea>\n" +
                    "                </div>\n" +
                    "                <button type=\"submit\" style=\"margin-top:16px; padding:10px 20px; background:#111; color:#fff; border:none; cursor:pointer;\">Submit Message</button>\n" +
                    "            </form>\n" +
                    "            <div id=\"normal-contact-ok\" style=\"display:none; margin-top:20px; padding:15px; background:#e2f0d9; color:#385723;\">Message successfully sent!</div>\n" +
                    "        </div>\n" +
                    "        <script>\n" +
                    "            document.addEventListener('DOMContentLoaded', () => {\n" +
                    "                const form = document.getElementById('normal-contact-form');\n" +
                    "                const success = document.getElementById('normal-contact-ok');\n" +
                    "                if (form) {\n" +
                    "                    form.addEventListener('submit', (e) => {\n" +
                    "                        e.preventDefault();\n" +
                    "                        const loader = document.getElementById('global-page-loader');\n" +
                    "                        if (loader) loader.style.display = 'flex';\n" +
                    "                        setTimeout(() => {\n" +
                    "                            if (loader) loader.style.display = 'none';\n" +
                    "                            form.style.display = 'none';\n" +
                    "                            if (success) success.style.display = 'block';\n" +
                    "                        }, 800);\n" +
                    "                    });\n" +
                    "                }\n" +
                    "            });\n" +
                    "        </script>";
        }
        else
        {
            return "        <div style=\"padding: 40px; max-width: 500px; margin: 0 auto;\">\n" +
                    "            <div style=\"font-size: 24px; font-weight: bold; margin-bottom: 20px;\">Contact Support</div>\n" +
                    "            <form id=\"bad-c-form\">\n" +
                    "                <div style=\"margin: 10px 0;\">\n" +
                    "                    Name:<br>\n" +
                    "                    <input type=\"text\" id=\"bad-name\" style=\"width:100%;\" required>\n" +
                    "                </div>\n" +
                    "                <div style=\"margin: 10px 0;\">\n" +
                    "                    Email:<br>\n" +
                    "                    <input type=\"email\" id=\"bad-email\" style=\"width:100%;\" required>\n" +
                    "                </div>\n" +
                    "                <div style=\"margin: 10px 0;\">\n" +
                    "                    Message:<br>\n" +
                    "                    <textarea id=\"bad-msg\" style=\"width:100%;\" rows=\"4\" required></textarea>\n" +
                    "                </div>\n" +
                    "                <button type=\"submit\">Send</button>\n" +
                    "            </form>\n" +
                    "            <div id=\"bad-c-success\" style=\"display:none; color: green; font-weight: bold; margin-top:20px;\">Sent!</div>\n" +
                    "        </div>\n" +
                    "        <script>\n" +
                    "            document.addEventListener('DOMContentLoaded', () => {\n" +
                    "                const form = document.getElementById('bad-c-form');\n" +
                    "                if (form) {\n" +
                    "                    form.addEventListener('submit', (e) => {\n" +
                    "                        e.preventDefault();\n" +
                    "                        const loader = document.getElementById('global-page-loader');\n" +
                    "                        if (loader) loader.style.display = 'flex';\n" +
                    "                        setTimeout(() => {\n" +
                    "                            if (loader) loader.style.display = 'none';\n" +
                    "                            form.style.display = 'none';\n" +
                    "                            document.getElementById('bad-c-success').style.display = 'block';\n" +
                    "                        }, 900);\n" +
                    "                    });\n" +
                    "                }\n" +
                    "            });\n" +
                    "        </script>";
        }
    }

    private static String getTrackOrdersContent(final String suffix)
    {
        if ("perfect".equals(suffix))
        {
            return "            <section class=\"container\" style=\"padding: 80px 0; max-width: 500px;\">\n" +
                    "                <h1 style=\"font-family: var(--font-family-serif); font-size: 36px; font-weight: 400; margin-bottom: 12px; text-align: center;\">Track Order</h1>\n" +
                    "                <p style=\"text-align: center; color: var(--color-text-secondary); margin-bottom: 32px;\">Enter your order ID to see real-time updates.</p>\n" +
                    "                \n" +
                    "                <form id=\"track-form-el\" style=\"display: flex; flex-direction: column; gap: 16px;\">\n" +
                    "                    <div>\n" +
                    "                        <label for=\"t-id\" style=\"display: block; font-size: 12px; text-transform: uppercase; font-weight: 600; margin-bottom: 6px;\">Order ID</label>\n" +
                    "                        <input type=\"text\" id=\"t-id\" placeholder=\"VERLA-xxxx-xxxx\" required style=\"width:100%; padding:12px; border:1px solid var(--color-border); outline:none;\">\n" +
                    "                    </div>\n" +
                    "                    <div>\n" +
                    "                        <label for=\"t-email\" style=\"display: block; font-size: 12px; text-transform: uppercase; font-weight: 600; margin-bottom: 6px;\">Billing Email</label>\n" +
                    "                        <input type=\"email\" id=\"t-email\" required style=\"width:100%; padding:12px; border:1px solid var(--color-border); outline:none;\">\n" +
                    "                    </div>\n" +
                    "                    <button type=\"submit\" style=\"background:var(--color-text-primary); color:white; padding:14px; font-size:12px; text-transform:uppercase; letter-spacing:0.05em; font-weight:600; border:none; cursor:pointer;\">Track Order</button>\n" +
                    "                </form>\n" +
                    "                <div id=\"track-results-msg\" style=\"display:none; border: 1px solid var(--color-border); padding: 24px; background: var(--color-bg-secondary); margin-top:20px;\">\n" +
                    "                    <h4 style=\"font-family:var(--font-family-serif); margin-bottom:12px;\">Order Status: Shipped</h4>\n" +
                    "                    <p style=\"font-size:13px; color:var(--color-text-secondary); line-height:1.6;\">\n" +
                    "                        Your package has been processed by our eco-hub and is currently in transit.\n" +
                    "                        <br><strong>Carrier:</strong> DHL Express\n" +
                    "                        <br><strong>Tracking Number:</strong> <span style=\"color:var(--color-accent); font-weight:500;\">VERLA-9928-DHL</span>\n" +
                    "                    </p>\n" +
                    "                </div>\n" +
                    "            </section>\n" +
                    "            <script>\n" +
                    "                document.addEventListener('DOMContentLoaded', () => {\n" +
                    "                    const form = document.getElementById('track-form-el');\n" +
                    "                    const results = document.getElementById('track-results-msg');\n" +
                    "                    if (form) {\n" +
                    "                        form.addEventListener('submit', (e) => {\n" +
                    "                            e.preventDefault();\n" +
                    "                            const loader = document.getElementById('global-page-loader');\n" +
                    "                            if (loader) loader.style.display = 'flex';\n" +
                    "                            setTimeout(() => {\n" +
                    "                                if (loader) loader.style.display = 'none';\n" +
                    "                                form.style.display = 'none';\n" +
                    "                                if (results) results.style.display = 'block';\n" +
                    "                            }, 1100);\n" +
                    "                        });\n" +
                    "                    }\n" +
                    "                });\n" +
                    "            </script>";
        }
        else if ("normal".equals(suffix))
        {
            return "        <div class=\"container\" style=\"padding: 60px 0; max-width: 500px;\">\n" +
                    "            <h2>Track Orders</h2>\n" +
                    "            <p style=\"color:#666; margin-bottom:20px;\">Check your order shipment details.</p>\n" +
                    "            \n" +
                    "            <form id=\"normal-track-form\">\n" +
                    "                <div style=\"margin-top: 12px;\">\n" +
                    "                    <label>Order Reference ID:</label><br>\n" +
                    "                    <input type=\"text\" id=\"normal-t-id\" required style=\"width:100%; padding:10px;\">\n" +
                    "                </div>\n" +
                    "                <div style=\"margin-top: 12px;\">\n" +
                    "                    <label>Email:</label><br>\n" +
                    "                    <input type=\"email\" id=\"normal-t-email\" required style=\"width:100%; padding:10px;\">\n" +
                    "                </div>\n" +
                    "                <button type=\"submit\" style=\"margin-top:16px; padding:10px 20px; background:#111; color:#fff; border:none; cursor:pointer;\">Locate Shipment</button>\n" +
                    "            </form>\n" +
                    "            <div id=\"normal-track-results\" style=\"display:none; margin-top:20px; padding:15px; border:1px solid #ddd; background:#f9f9f9;\">\n" +
                    "                <strong>Status:</strong> Dispatched<br>\n" +
                    "                <strong>Carrier:</strong> FedEx Priority (Ref: V-77291)\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "        <script>\n" +
                    "            document.addEventListener('DOMContentLoaded', () => {\n" +
                    "                const form = document.getElementById('normal-track-form');\n" +
                    "                const results = document.getElementById('normal-track-results');\n" +
                    "                if (form) {\n" +
                    "                    form.addEventListener('submit', (e) => {\n" +
                    "                        e.preventDefault();\n" +
                    "                        const loader = document.getElementById('global-page-loader');\n" +
                    "                        if (loader) loader.style.display = 'flex';\n" +
                    "                        setTimeout(() => {\n" +
                    "                            if (loader) loader.style.display = 'none';\n" +
                    "                            form.style.display = 'none';\n" +
                    "                            if (results) results.style.display = 'block';\n" +
                    "                        }, 900);\n" +
                    "                    });\n" +
                    "                }\n" +
                    "            });\n" +
                    "        </script>";
        }
        else
        {
            return "        <div style=\"padding: 40px; max-width: 500px; margin: 0 auto;\">\n" +
                    "            <div style=\"font-size: 24px; font-weight: bold; margin-bottom: 20px;\">Order Tracking</div>\n" +
                    "            <form id=\"bad-t-form\">\n" +
                    "                <div style=\"margin: 10px 0;\">\n" +
                    "                    Order Reference Number:<br>\n" +
                    "                    <input type=\"text\" id=\"bad-order-ref\" style=\"width:100%;\" required>\n" +
                    "                </div>\n" +
                    "                <div style=\"margin: 10px 0;\">\n" +
                    "                    Customer Email:<br>\n" +
                    "                    <input type=\"email\" id=\"bad-cust-email\" style=\"width:100%;\" required>\n" +
                    "                </div>\n" +
                    "                <button type=\"submit\">Lookup</button>\n" +
                    "            </form>\n" +
                    "            <div id=\"bad-t-results\" style=\"display:none; color: navy; margin-top:20px;\">\n" +
                    "                Package status: In Transit (DHL). Expected delivery: Tomorrow.\n" +
                    "            </div>\n" +
                    "        </div>\n" +
                    "        <script>\n" +
                    "            document.addEventListener('DOMContentLoaded', () => {\n" +
                    "                const form = document.getElementById('bad-t-form');\n" +
                    "                if (form) {\n" +
                    "                    form.addEventListener('submit', (e) => {\n" +
                    "                        e.preventDefault();\n" +
                    "                        const loader = document.getElementById('global-page-loader');\n" +
                    "                        if (loader) loader.style.display = 'flex';\n" +
                    "                        setTimeout(() => {\n" +
                    "                            if (loader) loader.style.display = 'none';\n" +
                    "                            form.style.display = 'none';\n" +
                    "                            document.getElementById('bad-t-results').style.display = 'block';\n" +
                    "                        }, 1000);\n" +
                    "                    });\n" +
                    "                }\n" +
                    "            });\n" +
                    "        </script>";
        }
    }

    private static String getCareersContent(final String suffix)
    {
        final String applyButton;
        final String modalStyles;
        if ("bad".equals(suffix))
        {
            applyButton = "                <div style=\"background-color:#111; color:white; padding:10px 16px; font-size:11px; text-transform:uppercase; display:inline-block; margin-top:12px; cursor:pointer;\" onclick=\"document.getElementById('careers-modal-overlay').classList.add('active');\">Apply Now</div>\n";
            modalStyles = "";
        }
        else
        {
            applyButton = "                <button style=\"background:var(--color-text-primary); color:white; padding:10px 20px; font-size:11px; text-transform:uppercase; letter-spacing:0.05em; font-weight:600; border:none; cursor:pointer; margin-top:12px;\" onclick=\"document.getElementById('careers-modal-overlay').classList.add('active');\">Apply Now</button>\n";
            modalStyles = "";
        }

        final String listBlock = "            <div style=\"display:flex; flex-direction:column; gap:24px; margin-bottom:40px;\">\n" +
                "                <div style=\"border:1px solid var(--color-border); padding:24px;\">\n" +
                "                    <h3 style=\"font-family:var(--font-family-serif); margin-bottom:8px;\">Store Design Associate</h3>\n" +
                "                    <p style=\"font-size:13px; color:var(--color-text-secondary); line-height:1.6;\">\n" +
                "                        Shape the physical environments of VÉRLA. You will design minimal retail experiences, focusing on organic textures and sustainable store architecture.\n" +
                "                        <br><strong>Location:</strong> Paris, FR / Hybrid\n" +
                "                    </p>\n" +
                applyButton +
                "                </div>\n" +
                "                <div style=\"border:1px solid var(--color-border); padding:24px;\">\n" +
                "                    <h3 style=\"font-family:var(--font-family-serif); margin-bottom:8px;\">Sustainability & Sourcing Specialist</h3>\n" +
                "                    <p style=\"font-size:13px; color:var(--color-text-secondary); line-height:1.6;\">\n" +
                "                        Work with our supply chain partner network to audit growing techniques, confirm fiber certification, and optimize zero-waste crop sourcing.\n" +
                "                        <br><strong>Location:</strong> Berlin, DE / Remote\n" +
                "                    </p>\n" +
                applyButton +
                "                </div>\n" +
                "            </div>\n";

        return "            <section class=\"container\" style=\"padding: 80px 0; max-width: 700px;\">\n" +
                "                <h1 style=\"font-family: var(--font-family-serif); font-size: 36px; font-weight: 400; margin-bottom: 12px; text-align: center;\">Careers</h1>\n" +
                "                <p style=\"text-align: center; color: var(--color-text-secondary); margin-bottom: 40px;\">Help us build a mindful and sustainable future.</p>\n" +
                listBlock +
                "            </section>\n" +
                "            \n" +
                "            <!-- Careers Modal -->\n" +
                "            <div class=\"modal-overlay\" id=\"careers-modal-overlay\" role=\"dialog\" aria-modal=\"true\" style=\"position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.4); z-index: 2000; display: none; align-items: center; justify-content: center;\">\n" +
                "                <div class=\"modal\" style=\"background: var(--color-bg-primary); border: 1px solid var(--color-border); width: 90%; max-width: 450px; padding: 32px; position: relative;\">\n" +
                "                    <button class=\"modal-close-btn\" onclick=\"document.getElementById('careers-modal-overlay').classList.remove('active'); document.getElementById('careers-modal-overlay').style.display='none';\" style=\"position: absolute; top: 24px; right: 24px; font-size: 24px; color: var(--color-text-secondary); background:none; border:none; cursor:pointer;\">&times;</button>\n" +
                "                    <h3 class=\"modal-title\" style=\"font-family: var(--font-family-serif); font-size: 22px; font-weight: 400; margin-bottom: 16px;\">Job Application</h3>\n" +
                "                    <form id=\"job-application-form-el\" style=\"display:flex; flex-direction:column; gap:16px;\">\n" +
                "                        <div>\n" +
                "                            <label style=\"display:block; font-size:12px; font-weight:600; margin-bottom:6px;\">Full Name</label>\n" +
                "                            <input type=\"text\" required style=\"width:100%; padding:10px; border:1px solid var(--color-border);\">\n" +
                "                        </div>\n" +
                "                        <div>\n" +
                "                            <label style=\"display:block; font-size:12px; font-weight:600; margin-bottom:6px;\">Email</label>\n" +
                "                            <input type=\"email\" required style=\"width:100%; padding:10px; border:1px solid var(--color-border);\">\n" +
                "                        </div>\n" +
                "                        <div>\n" +
                "                            <label style=\"display:block; font-size:12px; font-weight:600; margin-bottom:6px;\">LinkedIn / Portfolio URL</label>\n" +
                "                            <input type=\"url\" placeholder=\"https://\" required style=\"width:100%; padding:10px; border:1px solid var(--color-border);\">\n" +
                "                        </div>\n" +
                "                        <button type=\"submit\" style=\"background:var(--color-text-primary); color:white; padding:12px; font-size:11px; text-transform:uppercase; font-weight:600; border:none; cursor:pointer;\">Submit Application</button>\n" +
                "                    </form>\n" +
                "                    <div id=\"job-app-success\" style=\"display:none; text-align:center; padding:16px; color:var(--color-success); font-weight:600;\">Application submitted!</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            \n" +
                "            <script>\n" +
                "                document.addEventListener('DOMContentLoaded', () => {\n" +
                "                    const overlays = document.querySelectorAll('[onclick*=\"careers-modal-overlay\"]');\n" +
                "                    const modal = document.getElementById('careers-modal-overlay');\n" +
                "                    overlays.forEach(btn => {\n" +
                "                        btn.addEventListener('click', (e) => {\n" +
                "                            e.stopPropagation();\n" +
                "                            if (modal) {\n" +
                "                                modal.style.display = 'flex';\n" +
                "                                modal.classList.add('active');\n" +
                "                            }\n" +
                "                        });\n" +
                "                    });\n" +
                "\n" +
                "                    const appForm = document.getElementById('job-application-form-el');\n" +
                "                    const appSuccess = document.getElementById('job-app-success');\n" +
                "                    if (appForm) {\n" +
                "                        appForm.addEventListener('submit', (e) => {\n" +
                "                            e.preventDefault();\n" +
                "                            const loader = document.getElementById('global-page-loader');\n" +
                "                            if (loader) loader.style.display = 'flex';\n" +
                "                            setTimeout(() => {\n" +
                "                                if (loader) loader.style.display = 'none';\n" +
                "                                appForm.style.display = 'none';\n" +
                "                                if (appSuccess) appSuccess.style.display = 'block';\n" +
                "                                setTimeout(() => {\n" +
                "                                    if (modal) {\n" +
                "                                        modal.style.display = 'none';\n" +
                "                                        modal.classList.remove('active');\n" +
                "                                    }\n" +
                "                                    appForm.style.display = 'flex';\n" +
                "                                    if (appSuccess) appSuccess.style.display = 'none';\n" +
                "                                }, 1500);\n" +
                "                            }, 1000);\n" +
                "                        });\n" +
                "                    }\n" +
                "                });\n" +
                "            </script>" + modalStyles;
    }

    private static String getStoresContent(final String suffix)
    {
        final String layoutGrid;
        if ("bad".equals(suffix))
        {
            layoutGrid = "            <div style=\"display:flex; flex-direction:column; gap:20px; margin-bottom:40px;\">\n" +
                    "                <div style=\"border:1px solid var(--color-border); padding:20px;\">\n" +
                    "                    <input type=\"text\" id=\"store-search\" placeholder=\"Search city...\" style=\"width:100%; padding:10px; margin-bottom:15px; border:1px solid var(--color-border);\" oninput=\"f_stores(event)\">\n" +
                    "                    <div id=\"stores-list\">\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:12px; border-bottom:1px solid var(--color-border); cursor:pointer; font-weight:600;\" data-name=\"New York\" data-desc=\"VÉRLA Soho - 92 Mercer St\" data-pin=\"pin-nyc\">New York Soho</div>\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:12px; border-bottom:1px solid var(--color-border); cursor:pointer; font-weight:600;\" data-name=\"Paris\" data-desc=\"VÉRLA Marais - 14 Rue des Francs Bourgeois\" data-pin=\"pin-paris\">Paris Marais</div>\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:12px; border-bottom:1px solid var(--color-border); cursor:pointer; font-weight:600;\" data-name=\"Tokyo\" data-desc=\"VÉRLA Omotesando - 5-10-1 Jingumae\" data-pin=\"pin-tokyo\">Tokyo Omotesando</div>\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:12px; border-bottom:1px solid var(--color-border); cursor:pointer; font-weight:600;\" data-name=\"London\" data-desc=\"VÉRLA Mayfair - 18 Conduit St\" data-pin=\"pin-london\">London Mayfair</div>\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:12px; border-bottom:1px solid var(--color-border); cursor:pointer; font-weight:600;\" data-name=\"Berlin\" data-desc=\"VÉRLA Mitte - 44 Mulackstraße\" data-pin=\"pin-berlin\">Berlin Mitte</div>\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "                <div style=\"border:1px solid var(--color-border); padding:20px; text-align:center;\">\n" +
                    "                    <div id=\"store-details-panel\" style=\"padding:12px; background:#f9f9f9; border:1px solid var(--color-border); font-size:13px; margin-bottom:15px; font-weight:500;\">Select a store to view details.</div>\n" +
                    "                    " + getSvgMap() + "\n" +
                    "                </div>\n" +
                    "            </div>\n";
        }
        else
        {
            layoutGrid = "            <div style=\"display:grid; grid-template-columns: 1fr 1.5fr; gap:32px; margin-bottom:40px;\">\n" +
                    "                <div style=\"border:1px solid var(--color-border); padding:24px;\">\n" +
                    "                    <h3 style=\"font-family:var(--font-family-serif); margin-bottom:12px;\">Find a Store</h3>\n" +
                    "                    <input type=\"text\" id=\"store-search\" placeholder=\"Search city...\" style=\"width:100%; padding:12px; border:1px solid var(--color-border); outline:none; margin-bottom:20px;\">\n" +
                    "                    <div id=\"stores-list\" style=\"display:flex; flex-direction:column; gap:8px;\">\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:16px; border:1px solid var(--color-border); cursor:pointer; transition:all 0.3s;\" data-name=\"New York\" data-desc=\"VÉRLA Soho - 92 Mercer St\" data-pin=\"pin-nyc\">\n" +
                    "                            <strong style=\"display:block; font-size:14px;\">New York Soho</strong>\n" +
                    "                            <span style=\"font-size:12px; color:var(--color-text-secondary);\">92 Mercer St, NY</span>\n" +
                    "                        </div>\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:16px; border:1px solid var(--color-border); cursor:pointer; transition:all 0.3s;\" data-name=\"Paris\" data-desc=\"VÉRLA Marais - 14 Rue des Francs Bourgeois\" data-pin=\"pin-paris\">\n" +
                    "                            <strong style=\"display:block; font-size:14px;\">Paris Marais</strong>\n" +
                    "                            <span style=\"font-size:12px; color:var(--color-text-secondary);\">14 Rue des Francs Bourgeois</span>\n" +
                    "                        </div>\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:16px; border:1px solid var(--color-border); cursor:pointer; transition:all 0.3s;\" data-name=\"Tokyo\" data-desc=\"VÉRLA Omotesando - 5-10-1 Jingumae\" data-pin=\"pin-tokyo\">\n" +
                    "                            <strong style=\"display:block; font-size:14px;\">Tokyo Omotesando</strong>\n" +
                    "                            <span style=\"font-size:12px; color:var(--color-text-secondary);\">5-10-1 Jingumae, Shibuya</span>\n" +
                    "                        </div>\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:16px; border:1px solid var(--color-border); cursor:pointer; transition:all 0.3s;\" data-name=\"London\" data-desc=\"VÉRLA Mayfair - 18 Conduit St\" data-pin=\"pin-london\">\n" +
                    "                            <strong style=\"display:block; font-size:14px;\">London Mayfair</strong>\n" +
                    "                            <span style=\"font-size:12px; color:var(--color-text-secondary);\">18 Conduit St, London</span>\n" +
                    "                        </div>\n" +
                    "                        <div class=\"store-item-btn\" style=\"padding:16px; border:1px solid var(--color-border); cursor:pointer; transition:all 0.3s;\" data-name=\"Berlin\" data-desc=\"VÉRLA Mitte - 44 Mulackstraße\" data-pin=\"pin-berlin\">\n" +
                    "                            <strong style=\"display:block; font-size:14px;\">Berlin Mitte</strong>\n" +
                    "                            <span style=\"font-size:12px; color:var(--color-text-secondary);\">44 Mulackstraße, Berlin</span>\n" +
                    "                        </div>\n" +
                    "                    </div>\n" +
                    "                </div>\n" +
                    "                <div style=\"border:1px solid var(--color-border); padding:24px; display:flex; flex-direction:column; align-items:center; justify-content:center; background:var(--color-bg-secondary);\">\n" +
                    "                    <div id=\"store-details-panel\" style=\"padding:16px; border:1px solid var(--color-border); background:var(--color-bg-primary); font-size:13px; border-radius:4px; margin-bottom:20px; width:100%; text-align:center; font-weight:500;\">Select a store to view details.</div>\n" +
                    "                    " + getSvgMap() + "\n" +
                    "                </div>\n" +
                    "            </div>\n";
        }

        return "            <section class=\"container\" style=\"padding: 80px 0; max-width: 1000px;\">\n" +
                "                <h1 style=\"font-family: var(--font-family-serif); font-size: 36px; font-weight: 400; margin-bottom: 24px; text-align: center;\">Our Stores</h1>\n" +
                layoutGrid +
                "            </section>\n" +
                "            \n" +
                "            <script>\n" +
                "                document.addEventListener('DOMContentLoaded', () => {\n" +
                "                    const searchInput = document.getElementById('store-search');\n" +
                "                    const storeBtns = document.querySelectorAll('.store-item-btn');\n" +
                "                    const details = document.getElementById('store-details-panel');\n" +
                "                    const pins = document.querySelectorAll('.store-pin');\n" +
                "\n" +
                "                    if (searchInput) {\n" +
                "                        searchInput.addEventListener('input', (e) => {\n" +
                "                            const term = e.target.value.toLowerCase();\n" +
                "                            storeBtns.forEach(btn => {\n" +
                "                                const text = btn.textContent.toLowerCase();\n" +
                "                                btn.style.display = text.includes(term) ? 'block' : 'none';\n" +
                "                            });\n" +
                "                        });\n" +
                "                    }\n" +
                "\n" +
                "                    function selectStore(btn) {\n" +
                "                        storeBtns.forEach(b => {\n" +
                "                            b.style.borderColor = 'var(--color-border)';\n" +
                "                            b.style.background = 'none';\n" +
                "                        });\n" +
                "                        btn.style.borderColor = 'var(--color-accent)';\n" +
                "                        btn.style.background = 'var(--color-accent-light)';\n" +
                "\n" +
                "                        const name = btn.getAttribute('data-name');\n" +
                "                        const desc = btn.getAttribute('data-desc');\n" +
                "                        const pinId = btn.getAttribute('data-pin');\n" +
                "                        \n" +
                "                        if (details) {\n" +
                "                            details.innerHTML = `<strong>${name} Store</strong><br>${desc}<br><span style=\"color:var(--color-success); font-weight:600;\">Open: 10:00 AM - 7:00 PM</span>`;\n" +
                "                        }\n" +
                "\n" +
                "                        pins.forEach(pin => {\n" +
                "                            pin.querySelector('circle').setAttribute('fill', 'var(--color-text-secondary)');\n" +
                "                            pin.querySelector('circle').setAttribute('r', '5');\n" +
                "                        });\n" +
                "\n" +
                "                        const activePin = document.getElementById(pinId);\n" +
                "                        if (activePin) {\n" +
                "                            activePin.querySelector('circle').setAttribute('fill', 'var(--color-accent)');\n" +
                "                            activePin.querySelector('circle').setAttribute('r', '8');\n" +
                "                        }\n" +
                "                    }\n" +
                "\n" +
                "                    storeBtns.forEach(btn => {\n" +
                "                        btn.addEventListener('click', () => selectStore(btn));\n" +
                "                    });\n" +
                "\n" +
                "                    pins.forEach(pin => {\n" +
                "                        pin.addEventListener('click', (e) => {\n" +
                "                            e.stopPropagation();\n" +
                "                            const pinId = pin.id;\n" +
                "                            const btn = Array.from(storeBtns).find(b => b.getAttribute('data-pin') === pinId);\n" +
                "                            if (btn) selectStore(btn);\n" +
                "                        });\n" +
                "                    });\n" +
                "                });\n" +
                "                \n" +
                "                // Fallback for global event in bad version\n" +
                "                function f_stores(e) {\n" +
                "                    const term = e.target.value.toLowerCase();\n" +
                "                    document.querySelectorAll('.store-item-btn').forEach(btn => {\n" +
                "                        const text = btn.textContent.toLowerCase();\n" +
                "                        btn.style.display = text.includes(term) ? 'block' : 'none';\n" +
                "                    });\n" +
                "                }\n" +
                "            </script>";
    }

    private static String getSvgMap()
    {
        return "<svg viewBox=\"0 0 400 200\" style=\"width:100%; max-width:400px; height:auto; border:1px solid var(--color-border); background:#e6f2ff;\" aria-label=\"Offline Vector Map\">\n" +
                "                        <!-- Mock continents -->\n" +
                "                        <path d=\"M20,40 Q40,30 60,50 T100,60 T140,50 L130,120 Q100,140 60,110 Z\" fill=\"#d4edda\" stroke=\"#85c99d\" stroke-width=\"1.5\" />\n" +
                "                        <path d=\"M180,30 Q220,10 260,20 T320,50 T360,90 L340,150 Q280,180 200,120 Z\" fill=\"#d4edda\" stroke=\"#85c99d\" stroke-width=\"1.5\" />\n" +
                "                        <path d=\"M80,140 Q100,160 120,190 T160,180 L140,150 Z\" fill=\"#d4edda\" stroke=\"#85c99d\" stroke-width=\"1.5\" />\n" +
                "                        \n" +
                "                        <!-- Pins -->\n" +
                "                        <!-- NYC (Americas east) -->\n" +
                "                        <g class=\"store-pin\" id=\"pin-nyc\" style=\"cursor:pointer;\">\n" +
                "                            <circle cx=\"110\" cy=\"80\" r=\"5\" fill=\"var(--color-text-secondary)\" />\n" +
                "                            <text x=\"110\" y=\"72\" font-size=\"9\" font-weight=\"bold\" text-anchor=\"middle\" fill=\"var(--color-text-primary)\">NYC</text>\n" +
                "                        </g>\n" +
                "                        <!-- Paris (Europe West) -->\n" +
                "                        <g class=\"store-pin\" id=\"pin-paris\" style=\"cursor:pointer;\">\n" +
                "                            <circle cx=\"220\" cy=\"60\" r=\"5\" fill=\"var(--color-text-secondary)\" />\n" +
                "                            <text x=\"220\" y=\"52\" font-size=\"9\" font-weight=\"bold\" text-anchor=\"middle\" fill=\"var(--color-text-primary)\">Paris</text>\n" +
                "                        </g>\n" +
                "                        <!-- Berlin (Europe East) -->\n" +
                "                        <g class=\"store-pin\" id=\"pin-berlin\" style=\"cursor:pointer;\">\n" +
                "                            <circle cx=\"240\" cy=\"50\" r=\"5\" fill=\"var(--color-text-secondary)\" />\n" +
                "                            <text x=\"240\" y=\"42\" font-size=\"9\" font-weight=\"bold\" text-anchor=\"middle\" fill=\"var(--color-text-primary)\">Berlin</text>\n" +
                "                        </g>\n" +
                "                        <!-- London (Europe North) -->\n" +
                "                        <g class=\"store-pin\" id=\"pin-london\" style=\"cursor:pointer;\">\n" +
                "                            <circle cx=\"210\" cy=\"48\" r=\"5\" fill=\"var(--color-text-secondary)\" />\n" +
                "                            <text x=\"210\" y=\"40\" font-size=\"9\" font-weight=\"bold\" text-anchor=\"middle\" fill=\"var(--color-text-primary)\">London</text>\n" +
                "                        </g>\n" +
                "                        <!-- Tokyo (Asia East) -->\n" +
                "                        <g class=\"store-pin\" id=\"pin-tokyo\" style=\"cursor:pointer;\">\n" +
                "                            <circle cx=\"330\" cy=\"85\" r=\"5\" fill=\"var(--color-text-secondary)\" />\n" +
                "                            <text x=\"330\" y=\"77\" font-size=\"9\" font-weight=\"bold\" text-anchor=\"middle\" fill=\"var(--color-text-primary)\">Tokyo</text>\n" +
                "                        </g>\n" +
                "                    </svg>";
    }
}
