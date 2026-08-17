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
package org.neodymium.ai.executor.selenide;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.HasCdp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neodymium.common.ScreenshotWriter;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.LocatorCascadeResolver;

/**
 * Captures page context (screenshot + simplified DOM) for the LLM. The DOM is
 * simplified to only include interactive
 * and visible elements to keep token usage low while giving the LLM enough
 * context. All DOM queries are batched into a
 * single JavaScript execution to minimize WebDriver round-trips for
 * performance.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public class PageAnalyzer
{
    private static final Logger LOG = LoggerFactory.getLogger(PageAnalyzer.class);

    private static final String FINGERPRINT_JS_FUNCTIONS = loadResource("ai-scripts/neodymium-dom-helpers.js");

    private final WebDriver providedDriver;

    /**
     * Default constructor for PageAnalyzer.
     * Requires a WebDriver to be passed explicitly when calling analysis methods.
     */
    public PageAnalyzer()
    {
        this(null);
    }

    /**
     * Constructs a PageAnalyzer bound to an explicit WebDriver instance.
     *
     * @param driver the WebDriver instance to analyze
     */
    public PageAnalyzer(final WebDriver driver)
    {
        this.providedDriver = driver;
    }

    /**
     * Resolves the target WebDriver instance using the following priority:
     * 1. Explicit argument passed at invocation time
     * 2. Instance provided at constructor initialization
     *
     * @param explicitDriver driver override passed to method, or null
     * @return the resolved active WebDriver instance, or null if uninitialized
     */
    private WebDriver resolveDriver(final WebDriver explicitDriver)
    {
        if (explicitDriver != null)
        {
            return explicitDriver;
        }
        if (this.providedDriver != null)
        {
            return this.providedDriver;
        }
        LOG.warn("No WebDriver provided to PageAnalyzer instance.");
        return null;
    }

    /**
     * JavaScript that runs in the browser and extracts all needed DOM data in a
     * single execution. Returns a Map with
     * "elements" (list of sections) and "forms" (list of form descriptors).
     * Visibility is checked via offsetParent
     * (null means hidden for non-fixed/non-body elements) and getComputedStyle as
     * fallback.
     */
    static final String CAPTURE_SCRIPT = """
            return (function(level, includesText, includesRich, isMinimal, volatilePatterns) {
                // Configuration constants to prevent payload bloat
                var MAX_PER_SELECTOR = 150; // Safeguard against massive list rendering
                var MAX_TEXT = 200;         // Max characters captured for element text labels
                var MAX_HREF = 180;         // Max URL length for captured links
                var MAX_VALUE = 150;        // Max characters for option/input value properties

                // Preserve current active element focus across script execution
                var activeElementBeforeAnalysis = document.activeElement;

                // Map of assigned automation IDs (data-ai) to handle unique stamping
                var usedIds = {};

                // Collect all shadow roots once to avoid O(N^2) DOM traversal
                var allRoots = [document];
                function findShadowRoots(root) {
                    var els = root.querySelectorAll('*');
                    for (var i = 0; i < els.length; i++) {
                        if (els[i].shadowRoot) {
                            allRoots.push(els[i].shadowRoot);
                            findShadowRoots(els[i].shadowRoot);
                        }
                    }
                }
                findShadowRoots(document);

                var seenRefs = {};

                // Pre-populate registry from IDs already stamped on this page
                // (handles repeated script injections on the same page)
                // Also detects duplicates caused by JS cloning and removes the attribute so a unique one is generated.
                for (var i = 0; i < allRoots.length; i++) {
                    allRoots[i].querySelectorAll('[data-ai]').forEach(function(el) {
                        var ref = el.getAttribute('data-ai');
                        if (seenRefs[ref]) {
                            el.removeAttribute('data-ai');
                        } else {
                            seenRefs[ref] = true;
                            usedIds[ref] = true;
                        }
                    });
                }
            """
            + FINGERPRINT_JS_FUNCTIONS
            + """
                // Retrieves or generates and stamps a unique 'data-ai' ID on the DOM element
                function assignId(el) {
                    if (el.hasAttribute('data-ai')) {
                        return el.getAttribute('data-ai');
                    }
                    var base = fingerprint(el);
                    var candidate = base;
                    var suffix = 0;
                    // Collision resolution in case elements compute the identical fingerprint
                    while (usedIds[candidate]) {
                        suffix++;
                        candidate = base + '_' + suffix;
                    }
                    usedIds[candidate] = true;
                    el.setAttribute('data-ai', candidate);
                    return candidate;
                }

                // Evaluates element visibility accurately by checking DOM connection, HUD boundaries, display, and layout rects
                function isVisible(el) {
                    if (!el.isConnected) return false;
                    // Never extract elements located inside the Neodymium AI Interactive HUD itself
                    if (el.closest && el.closest('.neodymium-ai-hud')) return false;

                    var tagName = el.tagName ? el.tagName.toLowerCase() : '';
                    if (tagName === 'option' || tagName === 'optgroup') {
                        return el.parentElement ? isVisible(el.parentElement) : true;
                    }

                    var type = el.getAttribute ? el.getAttribute('type') : null;
                    var isCheckableInput = tagName === 'input' && (type === 'radio' || type === 'checkbox');

                    var style = window.getComputedStyle(el);
                    if (style.display === 'none' || style.visibility === 'hidden') {
                        if (isCheckableInput) {
                            var label = (el.labels && el.labels.length > 0) ? el.labels[0] : (el.closest ? el.closest('label') : null);
                            if (label && isVisible(label)) return true;
                        }
                        return false;
                    }

                    var rect = el.getBoundingClientRect();
                    if (rect.width > 0 && rect.height > 0) return true;

                    if (isCheckableInput) {
                        var label = (el.labels && el.labels.length > 0) ? el.labels[0] : (el.closest ? el.closest('label') : null);
                        if (label && isVisible(label)) return true;
                    }

                    // Fallback for interactive elements or elements with text content that are not explicitly hidden by CSS
                    var isInteractiveTag = ['a', 'button', 'input', 'select', 'textarea'].indexOf(tagName) !== -1 || el.hasAttribute('href');
                    var hasText = (el.innerText || '').trim().length > 0;
                    if ((isInteractiveTag || hasText) && style.display !== 'none' && style.visibility !== 'hidden') {
                        return true;
                    }

                    return false;
                }

                // Standard truncation helper to keep token sizes predictable and clean
                function truncate(s, max) {
                    if (!s) return s;
                    return s.length <= max ? s : s.substring(0, max) + '…';
                }

                // Helper to check if an element is checked/selected (including custom attributes)
                function isChecked(el) {
                    var classChecked = false;
                    if (el.classList && typeof el.classList.contains === 'function') {
                        classChecked = el.classList.contains('checked') || el.classList.contains('active');
                    }
                    return !!(
                        el.checked || 
                        el.hasAttribute('checked') || 
                        el.getAttribute('aria-checked') === 'true' || 
                        el.hasAttribute('data-checked') || 
                        classChecked
                    );
                }

                // Helper to extract input values, masking sensitive password fields while exposing standard input values for assertions
                function getElementValue(el, label) {
                    var tag = el.tagName ? el.tagName.toLowerCase() : '';
                    var type = (el.getAttribute('type') || '').toLowerCase();
                    if (tag === 'input' && type === 'password') {
                        return null;
                    }
                    return truncate(el.value || el.getAttribute('value'), MAX_VALUE);
                }

                // Deep DOM query selector supporting crawling through Shadow DOM roots recursively
                function queryAllDeep(selector, root) {
                    if (root) {
                        var results = Array.from(root.querySelectorAll(selector));
                        var allEls = root.querySelectorAll('*');
                        for (var i = 0; i < allEls.length; i++) {
                            if (allEls[i].shadowRoot) {
                                results = results.concat(queryAllDeep(selector, allEls[i].shadowRoot));
                            }
                        }
                        return results;
                    }

                    var results = [];
                    for (var i = 0; i < allRoots.length; i++) {
                        var els = allRoots[i].querySelectorAll(selector);
                        for (var j = 0; j < els.length; j++) {
                            results.push(els[j]);
                        }
                    }
                    return results;
                }

                // Captures structured information for matched DOM elements (inputs, links, buttons, etc.)
                function captureElements(cssSelector, label) {
                    var results = [];
                    try {
                        var els = queryAllDeep(cssSelector);
                        for (var i = 0; i < els.length; i++) {
                            var el = els[i];
                            if (!isVisible(el)) continue;

                            var autoId = assignId(el);
                            var text = (el.innerText || '').trim().replace(new RegExp('\\s*\\n\\s*', 'g'), ' ');
                            var options = null;
                            // Format select dropdown options neatly
                            if (el.tagName.toLowerCase() === 'select') {
                                options = Array.from(el.options).slice(0, 50).map(o => o.text.trim()).filter(t => t.length > 0).join(', ');
                                if (el.options.length > 50) options += '... (total ' + el.options.length + ')';
                            }

                            results.push({
                                label: label,
                                tagName: el.tagName ? el.tagName.toLowerCase() : label,
                                className: (typeof el.className === 'string' && el.className.trim().length > 0) ? el.className.trim() : null,
                                text: text.length <= MAX_TEXT ? text : '',
                                id: el.id || null,
                                name: el.getAttribute('name'),
                                href: truncate(el.getAttribute('href'), MAX_HREF),
                                type: el.getAttribute('type'),
                                role: el.getAttribute('role'),
                                checked: isChecked(el) ? 'true' : null,
                                focused: (document.activeElement === el) ? 'true' : null,
                                placeholder: el.getAttribute('placeholder'),
                                ariaLabel: el.getAttribute('aria-label'),
                                pattern: el.getAttribute('pattern'),
                                title: el.getAttribute('title'),
                                min: el.getAttribute('min'),
                                max: el.getAttribute('max'),
                                minlength: el.getAttribute('minlength'),
                                maxlength: el.getAttribute('maxlength'),
                                step: el.getAttribute('step'),
                                autocomplete: el.getAttribute('autocomplete'),
                                required: el.hasAttribute('required') ? 'true' : null,
                                readonly: el.hasAttribute('readonly') ? 'true' : null,
                                disabled: el.hasAttribute('disabled') ? 'true' : null,
                                multiple: el.hasAttribute('multiple') ? 'true' : null,
                                value: getElementValue(el, label),
                                options: options,
                                automationId: autoId,
                                domElement: el
                            });
                        }
                    } catch(e) { /* skip selector errors */ }
                    return results;
                }

                // Identifies and extracts custom elements acting as clickable targets (e.g. styled divs/spans)
                function captureClickableElements(cssSelector, label) {
                    var results = [];
                    try {
                        var els = queryAllDeep(cssSelector);
                        for (var i = 0; i < els.length; i++) {
                            var el = els[i];
                            if (!isVisible(el)) continue;
                            if (el.closest('a')) continue; // Skip if already wrapped inside standard anchor link

                            var style = window.getComputedStyle(el);
                            // A custom element is considered clickable if it has pointer cursor, onclick attribute, or onclick handler
                            var isClickable = style.cursor === 'pointer' || el.hasAttribute('onclick') || typeof el.onclick === 'function';
                            if (!isClickable) continue;                            var autoId = assignId(el);
                            var text = (el.innerText || '').trim().replace(new RegExp('\\s*\\n\\s*', 'g'), ' ');
                            var options = null;
                            if (el.tagName && el.tagName.toLowerCase() === 'select') {
                                options = Array.from(el.options).slice(0, 50).map(o => o.text.trim()).filter(t => t.length > 0).join(', ');
                                if (el.options.length > 50) options += '... (total ' + el.options.length + ')';
                            }

                            results.push({
                                label: label,
                                tagName: el.tagName ? el.tagName.toLowerCase() : label,
                                className: (typeof el.className === 'string' && el.className.trim().length > 0) ? el.className.trim() : null,
                                text: text.length <= MAX_TEXT ? text : '',
                                id: el.id || null,
                                name: el.getAttribute('name'),
                                href: truncate(el.getAttribute('href'), MAX_HREF),
                                type: el.getAttribute('type'),
                                role: el.getAttribute('role'),
                                checked: isChecked(el) ? 'true' : null,
                                placeholder: el.getAttribute('placeholder'),
                                ariaLabel: el.getAttribute('aria-label'),
                                pattern: el.getAttribute('pattern'),
                                title: el.getAttribute('title'),
                                min: el.getAttribute('min'),
                                max: el.getAttribute('max'),
                                minlength: el.getAttribute('minlength'),
                                maxlength: el.getAttribute('maxlength'),
                                step: el.getAttribute('step'),
                                autocomplete: el.getAttribute('autocomplete'),
                                required: el.hasAttribute('required') ? 'true' : null,
                                readonly: el.hasAttribute('readonly') ? 'true' : null,
                                disabled: el.hasAttribute('disabled') ? 'true' : null,
                                multiple: el.hasAttribute('multiple') ? 'true' : null,
                                value: getElementValue(el, label),
                                options: options,
                                automationId: autoId,
                                domElement: el
                            });
                        }
                    } catch(e) { /* skip selector errors */ }
                    return results;
                }

                // Extracts forms along with their respective interactive fields to build standard logical input scopes
                function captureForms() {
                    var results = [];
                    try {
                        var forms = queryAllDeep('form');
                        for (var f = 0; f < forms.length; f++) {
                            var form = forms[f];
                            if (form.closest && form.closest('.neodymium-ai-hud')) continue;
                            var formId = assignId(form);

                            var fields = [];
                            var inputs = queryAllDeep('input, select, textarea', form);
                            for (var j = 0; j < inputs.length; j++) {
                                var inp = inputs[j];
                                if (!isVisible(inp)) continue;
                                var autoId = assignId(inp);

                                var field = {
                                    type: (inp.tagName && inp.tagName.toLowerCase() === 'select') ? 'select' : (inp.getAttribute('type') || ''),
                                    name: inp.getAttribute('name') || '',
                                    id: inp.id || '',
                                    automationId: autoId
                                };
                                if (inp.tagName && inp.tagName.toLowerCase() === 'select') {
                                    field.options = Array.from(inp.options).slice(0, 50).map(o => o.text.trim()).filter(t => t.length > 0).join(', ');
                                    if (inp.options.length > 50) field.options += '... (total ' + inp.options.length + ')';
                                }
                                fields.push(field);
                            }

                            results.push({
                                id: form.id || '',
                                action: form.getAttribute('action') || '',
                                fields: fields,
                                automationId: formId
                            });
                        }
                    } catch(e) { /* skip errors */ }
                    return results;
                }

                var sections = [];

                // Fast pre-pass: Stamp data-parent-text on elements sharing identical text labels to aid LLM disambiguation
                var candidateEls = queryAllDeep('a, button, input, select, textarea, [role="button"], [role="link"]');
                var textCounts = {};
                var elLabels = [];
                for (var i = 0; i < candidateEls.length; i++) {
                    var candidateEl = candidateEls[i];
                    if (!isVisible(candidateEl)) continue;
                    var txt = (candidateEl.innerText || candidateEl.placeholder || candidateEl.value || candidateEl.getAttribute('aria-label') || candidateEl.title || candidateEl.name || '').trim().replace(new RegExp('\\s*\\n\\s*', 'g'), ' ');
                    if (txt) {
                        textCounts[txt] = (textCounts[txt] || 0) + 1;
                        elLabels.push({ el: candidateEl, txt: txt });
                    }
                }
                for (var i = 0; i < elLabels.length; i++) {
                    var item = elLabels[i];
                    if (textCounts[item.txt] > 1) {
                        var parentText = '';
                        var p = item.el.parentElement;
                        var depth = 0;
                        while (p && p !== document.body && depth < 3) {
                            var tag = p.tagName ? p.tagName.toLowerCase() : '';
                            var role = p.getAttribute ? (p.getAttribute('role') || '') : '';
                            var cls = (typeof p.className === 'string' ? p.className : '').toLowerCase();
                            var id = (p.id || '').toLowerCase();
                            if (tag === 'header' || tag === 'footer' || tag === 'nav' || tag === 'aside') break;
                            if (role === 'banner' || role === 'navigation' || role === 'contentinfo' || role === 'complementary') break;
                            if (cls.includes('navbar') || cls.includes('header') || cls.includes('footer') ||
                                id.includes('navbar') || id.includes('header') || id.includes('footer')) break;
                            var pText = (p.innerText || '').trim();
                            if (pText.length > item.txt.length && pText.length < 300) {
                                parentText = pText;
                                break;
                            }
                            if (pText.length >= 300) break;
                            p = p.parentElement;
                            depth++;
                        }
                        if (parentText && typeof item.el.setAttribute === 'function') {
                            item.el.setAttribute('data-parent-text', truncate(parentText.replace(new RegExp('\\s*\\n\\s*', 'g'), ' | '), MAX_TEXT));
                        }
                    }
                }


                // Helper to test if element is interactive
                function isInteractive(el) {
                    if (!el || !el.tagName) return false;
                    var tag = el.tagName.toLowerCase();
                    if (['a', 'button', 'input', 'select', 'textarea', 'option', 'label'].indexOf(tag) !== -1) return true;
                    if (el.hasAttribute('onclick') || typeof el.onclick === 'function') return true;
                    if (el.hasAttribute('tabindex') || el.hasAttribute('contenteditable')) return true;
                    var role = el.getAttribute('role') || '';
                    if (['button', 'link', 'checkbox', 'radio', 'tab', 'menuitem', 'option', 'switch', 'combobox'].indexOf(role) !== -1) return true;
                    var id = el.id ? el.id.toLowerCase() : '';
                    var cls = (typeof el.className === 'string' ? el.className : '').toLowerCase();
                    if (id.includes('btn') || id.includes('button') || id.includes('click') || id.includes('nav') || id.includes('cart') || id.includes('trigger') ||
                        cls.includes('btn') || cls.includes('button') || cls.includes('click') || cls.includes('nav') || cls.includes('cart') || cls.includes('trigger')) return true;
                    var style = window.getComputedStyle(el);
                    if (style.cursor === 'pointer' && !el.closest('a')) return true;
                    return false;
                }

                // Recursive Structural DOM Tree Traversal
                function buildNodeTree(el) {
                    if (!el) return null;
                    var tag = el.tagName ? el.tagName.toLowerCase() : '';
                    if (['script', 'style', 'svg', 'noscript', 'meta', 'link', 'head'].indexOf(tag) !== -1) return null;
                    if (el.closest && el.closest('.neodymium-ai-hud')) return null;

                    var vis = isVisible(el);
                    var inForm = !!(el.closest && el.closest('form'));
                    var isFormContainer = inForm && ['fieldset','div','span','p','table','tbody','tr','td'].indexOf(tag) !== -1;
                    var isFormInput = inForm && ['input','select','textarea','button'].indexOf(tag) !== -1;
                    if (!vis && !isFormInput && !isFormContainer) return null;

                    var isInter = isInteractive(el);
                    var isHead = ['h1','h2','h3','h4','h5','h6'].indexOf(tag) !== -1;
                    var isStandardLeaf = ['a', 'button', 'input', 'textarea', 'option', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'].indexOf(tag) !== -1;
                    var isCustomLeaf = (isInter || isHead) && el.children.length === 0;

                    // 1. Leaf interactive or heading element (Atomic)
                    if (isStandardLeaf || isCustomLeaf) {
                        var autoId = assignId(el);
                        var text = (el.innerText || '').trim().replace(new RegExp('\\s*\\n\\s*', 'g'), ' ');
                        var options = null;
                        if (tag === 'select') {
                            options = Array.from(el.options).slice(0, 50).map(o => (o.id ? '#' + o.id + '=' : '') + o.text.trim()).filter(t => t.length > 0).join(', ');
                            if (el.options.length > 50) options += '... (total ' + el.options.length + ')';
                        }
                        return {
                            nodeType: 'leaf',
                            tagName: tag,
                            className: (typeof el.className === 'string' && el.className.trim().length > 0) ? el.className.trim() : null,
                            text: text.length <= MAX_TEXT ? text : '',
                            id: el.id || null,
                            name: el.getAttribute('name'),
                            href: truncate(el.getAttribute('href'), MAX_HREF),
                            type: el.getAttribute('type'),
                            role: el.getAttribute('role'),
                            checked: isChecked(el) ? 'true' : null,
                            selected: (tag === 'option' ? (el.selected || el.hasAttribute('selected') ? 'true' : null) : null),
                            disabled: (el.disabled || el.hasAttribute('disabled')) ? 'true' : null,
                            hidden: (!vis ? 'true' : null),
                            focused: (document.activeElement === el) ? 'true' : null,
                            placeholder: el.getAttribute('placeholder'),
                            ariaLabel: el.getAttribute('aria-label'),
                            value: getElementValue(el, tag),
                            options: options,
                            automationId: autoId,
                            parentText: el.getAttribute('data-parent-text') || null
                        };
                    }

                    var isContainerTag = ['header','nav','main','section','article','aside','form','footer','fieldset','details','ul','ol','select','optgroup'].indexOf(tag) !== -1;
                    var hasClassOrId = (el.id || (typeof el.className === 'string' && el.className.trim().length > 0));
                    var isDivContainer = (tag === 'div' || tag === 'li') && hasClassOrId;

                    var children = [];
                    var childNodes = (tag === 'select' || tag === 'optgroup') ? Array.from(el.children).slice(0, 50) : Array.from(el.children);
                    for (var i = 0; i < childNodes.length; i++) {
                        var childRes = buildNodeTree(childNodes[i]);
                        if (childRes) {
                            if (Array.isArray(childRes)) {
                                children = children.concat(childRes);
                            } else {
                                children.push(childRes);
                            }
                        }
                    }

                    // 2. Container node with extracted children
                    var isFormContainer = tag === 'form' || tag === 'fieldset' || tag === 'select' || tag === 'optgroup';
                    var allowContainer = !isMinimal || isFormContainer;
                    if (allowContainer && (isContainerTag || isDivContainer) && children.length > 0) {
                        // Flatten single-child anonymous layout wrappers
                        var isAnonymousWrapper = !el.id && !el.getAttribute('role') && !el.getAttribute('aria-label') && (!el.className || typeof el.className !== 'string' || el.className.trim().length === 0);
                        if (isAnonymousWrapper && children.length === 1 && !Array.isArray(children[0])) {
                            return children[0];
                        }
                        var autoIdContainer = assignId(el);
                        return {
                            nodeType: 'container',
                            tagName: tag,
                            id: el.id || null,
                            className: (typeof el.className === 'string' && el.className.trim().length > 0) ? el.className.trim() : null,
                            name: el.getAttribute('name'),
                            role: el.getAttribute('role'),
                            ariaLabel: el.getAttribute('aria-label'),
                            parentText: el.getAttribute('data-parent-text') || null,
                            automationId: autoIdContainer,
                            children: children
                        };
                    }

                    // 3. Text leaf element (paragraphs, spans, table cells, or divs with text)
                    var textContent = (el.innerText || '').trim().replace(new RegExp('\\s*\\n\\s*', 'g'), ' ');
                    var isInlineWrapper = children.length > 0 && children.every(function(c) {
                        return c && (c.nodeType === 'leaf' || Array.isArray(c)) && ['span','b','strong','i','em','small','code','a'].indexOf(c.tagName || (c[0] && c[0].tagName)) !== -1;
                    });
                    if (!isMinimal && textContent.length > 0 && textContent.length <= MAX_TEXT && (children.length === 0 || isInlineWrapper)) {
                        // In LEAN mode (!includesText), exclude non-interactive static text nodes (copy text, list items, code blocks, spans)
                        if (!includesText) {
                            if (!isInter && !isHead) {
                                return null;
                            }
                        }
                        var autoId = assignId(el);
                        var leafNode = {
                            nodeType: 'leaf',
                            tagName: tag,
                            className: (typeof el.className === 'string' && el.className.trim().length > 0) ? el.className.trim() : null,
                            text: textContent,
                            id: el.id || null,
                            automationId: autoId
                        };
                        if (includesRich) {
                            var titleAttr = el.getAttribute('title');
                            if (titleAttr) leafNode.title = titleAttr;
                            var ariaDesc = el.getAttribute('aria-describedby');
                            if (ariaDesc) leafNode.ariaDescribedBy = ariaDesc;
                        }
                        return leafNode;
                    }

                    // 4. Collapse transparent wrapper divs/spans having children
                    if (children.length > 0) {
                        return children;
                    }

                    return null;
                }

                var tree = buildNodeTree(document.body);
                var rootNodes = [];
                if (tree) {
                    if (Array.isArray(tree)) {
                        rootNodes = tree;
                    } else {
                        rootNodes = [tree];
                    }
                }

                if (activeElementBeforeAnalysis && activeElementBeforeAnalysis !== document.body && typeof activeElementBeforeAnalysis.focus === 'function') {
                    try { activeElementBeforeAnalysis.focus(); } catch (e) {}
                }

                return {tree: rootNodes, forms: level >= 1 ? captureForms() : []};
            })(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
            """;

    private boolean hasActiveWebDriver(final WebDriver explicitDriver) {
        final WebDriver driver = resolveDriver(explicitDriver);
        if (driver == null) {
            return false;
        }
        try {
            driver.getCurrentUrl();
            return true;
        } catch (final Exception e) {
            LOG.debug("Active WebDriver check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Captures a Base64-encoded screenshot of the current page.
     *
     * @return Base64 PNG string
     * @throws IOException
     */
    public String captureScreenshot(final String title) throws IOException
    {
        return captureScreenshot(title, null, false, null);
    }

    public String captureScreenshot(final String title, final ContextLevel level) throws IOException
    {
        return captureScreenshot(title, level, false, null);
    }

    public String captureScreenshot(final String title, final boolean isFullPage) throws IOException
    {
        return captureScreenshot(title, null, isFullPage, null);
    }

    public String captureScreenshot(final String title, final WebDriver explicitDriver) throws IOException
    {
        return captureScreenshot(title, null, false, explicitDriver);
    }

    public String captureScreenshot(final String title, final ContextLevel level, final boolean isFullPage, final WebDriver explicitDriver) throws IOException
    {
        final long startNanos = System.nanoTime();
        final WebDriver driver = resolveDriver(explicitDriver);
        if (!hasActiveWebDriver(driver))
        {
            return null;
        }
        LOG.debug("   📸 Capturing screenshot for: {} (level: {}, fullPage: {})", title, level, isFullPage);
        try
        {
            final String result = captureScreenshotInternal(title, level, isFullPage, driver);
            final long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            final String dimsStr = formatImageDimensions(result);
            final int sizeKb = result != null ? (result.length() * 3 / 4 / 1024) : 0;
            LOG.debug("   📸 Screenshot captured for '{}' in {} ms | Dimensions: {} | Size: {} chars (~{} KB)", title, elapsedMs, dimsStr, result != null ? result.length() : 0, sizeKb);
            return result;
        }
        catch (final Exception e)
        {
            if (isNoSuchWindowException(e))
            {
                LOG.warn("   ⚠️ Target window was closed during screenshot capture, switching to first available window");
                try
                {
                    final Set<String> activeHandles = driver.getWindowHandles();
                    if (!activeHandles.isEmpty())
                    {
                        final String fallback = activeHandles.iterator().next();
                        driver.switchTo().window(fallback);
                        driver.switchTo().defaultContent();
                        final String result = captureScreenshotInternal(title, level, isFullPage, driver);
                        final long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                        final String dimsStr = formatImageDimensions(result);
                        final int sizeKb = result != null ? (result.length() * 3 / 4 / 1024) : 0;
                        LOG.debug("   📸 Screenshot captured for '{}' (fallback) in {} ms | Dimensions: {} | Size: {} chars (~{} KB)", title, elapsedMs, dimsStr, result != null ? result.length() : 0, sizeKb);
                        return result;
                    }
                }
                catch (final Exception ex)
                {
                    LOG.warn("   ⚠️ Could not switch to fallback window or capture screenshot: {}", ex.getMessage());
                }
            }
            else
            {
                LOG.warn("   ⚠️ Failed to capture screenshot: {}", e.getMessage());
            }
        }
        return null;
    }

    private String captureScreenshotInternal(final String title, final ContextLevel level, final boolean isFullPage, final WebDriver driver) throws Exception
    {
        boolean hidden = false;
        if (driver instanceof final JavascriptExecutor js)
        {
            try
            {
                final Object hudExists = js.executeScript(
                        "var hud = document.getElementById('neodymium-ai-hud-container'); " +
                        "if (hud && hud.style.display !== 'none') { hud.style.display = 'none'; return true; } " +
                        "return false;");
                hidden = Boolean.TRUE.equals(hudExists);
            }
            catch (final Exception ignored)
            {
            }
        }

        try
        {
            final boolean forceFullPage = isFullPage || (level != null && level.isFullPageScreenshot());
            return ScreenshotWriter.doScreenshot(
                    title.replaceAll("[^a-zA-Z0-9-]", "_").substring(0, Math.min(title.length(), 12)),
                    ScreenshotWriter.getFormatedReportsPath(), false, false, forceFullPage);
        }
        finally
        {
            if (hidden && driver instanceof final JavascriptExecutor js)
            {
                try
                {
                    js.executeScript(
                            "var hud = document.getElementById('neodymium-ai-hud-container'); " +
                                    "if (hud) { hud.style.display = ''; }");
                }
                catch (final Exception ignored)
                {
                }
            }
        }
    }

    private boolean isNoSuchWindowException(final Exception e)
    {
        if (e instanceof NoSuchWindowException)
        {
            return true;
        }
        final String msg = e.getMessage();
        if (msg != null)
        {
            final String lower = msg.toLowerCase();
            return lower.contains("no such window") || lower.contains("window already closed") || lower.contains("target window already closed");
        }
        return false;
    }

    private static String formatImageDimensions(final String base64Data)
    {
        if (base64Data == null || base64Data.isEmpty())
        {
            return "unknown";
        }
        try
        {
            final byte[] bytes = java.util.Base64.getDecoder().decode(base64Data);
            final java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (img != null)
            {
                return img.getWidth() + "x" + img.getHeight() + " px";
            }
        }
        catch (final Exception ignored)
        {
        }
        return "unknown";
    }

    /**
     * Captures a simplified representation of the current page's DOM, focusing on
     * interactive and meaningful elements.
     * Uses a single JavaScript execution for performance.
     *
     * @return simplified DOM as a structured text
     */
    public String captureSimplifiedDom() {
        return captureSimplifiedDom(ContextLevel.MINIMAL);
    }

    /**
     * Captures a simplified representation of the current page's DOM, focusing on
     * interactive and meaningful elements.
     * Uses a single JavaScript execution for performance.
     *
     * @param forValidation
     *                      whether to include more text content for validation
     * @return simplified DOM as a structured text
     * @deprecated Use {@link #captureSimplifiedDom(ContextLevel)} instead.
     */
    @Deprecated
    public String captureSimplifiedDom(final boolean forValidation) {
        return captureSimplifiedDom(forValidation ? ContextLevel.STANDARD : ContextLevel.MINIMAL);
    }

    /**
     * Captures a simplified representation of the current page's DOM at the
     * specified context level. Uses a single JavaScript execution for performance.
     *
     * @param level
     *              the context level controlling how much DOM data to capture
     * @return simplified DOM as a structured text
     */
    /**
     * Captures a simplified representation of the current page's DOM at the
     * specified context level using dynamically resolved WebDriver.
     *
     * @param level the context level controlling how much DOM data to capture
     * @return simplified DOM as structured text
     */
    public String captureSimplifiedDom(final ContextLevel level) {
        return captureSimplifiedDom(level, null);
    }

    /**
     * Captures a simplified representation of the current page's DOM at the
     * specified context level using an explicitly provided WebDriver.
     *
     * @param level          the context level controlling how much DOM data to capture
     * @param explicitDriver explicit WebDriver instance override, or null for dynamic resolution
     * @return simplified DOM as structured text formatted for LLM consumption
     */
    @SuppressWarnings("unchecked")
    public String captureSimplifiedDom(final ContextLevel level, final WebDriver explicitDriver) {
        final long startNanos = System.nanoTime();
        final WebDriver driver = resolveDriver(explicitDriver);
        if (!hasActiveWebDriver(driver)) {
            return "Page URL: <empty page>\nPage Title: \n\n";
        }

        final long stage1Start = System.nanoTime();
        String url;
        String title;
        try {
            url = driver.getCurrentUrl();
            title = driver.getTitle();
        } catch (final Exception e) {
            return "Page URL: <empty page>\nPage Title: \n\n";
        }
        boolean isEmptyPage = "data:,".equals(url) || "about:blank".equals(url);
        if (isEmptyPage) {
            try {
                final Set<String> handles = driver.getWindowHandles();
                if (handles != null && handles.size() > 1) {
                    for (final String handle : handles) {
                        try {
                            driver.switchTo().window(handle);
                            final String candidateUrl = driver.getCurrentUrl();
                            if (candidateUrl != null && !"data:,".equals(candidateUrl) && !"about:blank".equals(candidateUrl)) {
                                url = candidateUrl;
                                title = driver.getTitle();
                                isEmptyPage = false;
                                break;
                            }
                        } catch (final Exception ignored) {
                        }
                    }
                }
            } catch (final Exception ignored) {
            }
        }
        final long stage1Ms = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stage1Start);

        if (!isEmptyPage) {
            LOG.debug("🔴 [DOM Capture: {}] URL: {} (Stage 1 Window Resolution: {} ms)", level, url, stage1Ms);
        }

        final StringBuilder dom = new StringBuilder();
        dom.append("Page URL: ").append(isEmptyPage ? "<empty page>" : url).append("\n");
        dom.append("Page Title: ").append(title != null ? title : "").append("\n\n");

        if (isEmptyPage || level == ContextLevel.VISUAL) {
            final String result = dom.toString();
            final long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            if (!isEmptyPage) {
                LOG.debug("   📄 DOM Capture Completed in {} ms | Mode: VISUAL | Size: {} chars", elapsedMs, result.length());
            }
            return result;
        }

        final String currentWindow = driver.getWindowHandle();
        int totalElements = 0;
        int windowCount = 0;

        boolean showFrameId = true;
        try {
            final java.util.Set<String> windowHandles = driver.getWindowHandles();
            if (windowHandles.size() == 1 && driver.findElements(By.cssSelector("iframe, frame")).isEmpty()) {
                showFrameId = false;
            }
        } catch (final Exception e) {
            // fallback
        }

        try {
            final Set<String> windowHandles = driver.getWindowHandles();
            final List<String> windowList = new ArrayList<>(windowHandles);
            windowCount = windowList.size();
            for (int i = 0; i < windowList.size(); i++) {
                final String windowHandle = windowList.get(i);
                final String logicalWindowName = "win_" + i;
                driver.switchTo().window(windowHandle);

                if (showFrameId || windowList.size() > 1) {
                    dom.append("=== Window: ").append(logicalWindowName).append(" ===\n");
                    dom.append("URL: ").append(driver.getCurrentUrl()).append("\n");
                    dom.append("Title: ").append(driver.getTitle()).append("\n\n");
                }
                totalElements += captureFrameTree(dom, level, logicalWindowName, "main", showFrameId, driver);
            }
        } catch (final Exception e) {
            LOG.warn("Error capturing full frame tree: {}", e.getMessage());
        } finally {
            try {
                driver.switchTo().window(currentWindow);
                driver.switchTo().defaultContent();
            } catch (final Exception e) {
            }
        }

        if (totalElements == 0 && driver instanceof final JavascriptExecutor js) {
            try {
                final Object readyState = js.executeScript("return document.readyState");
                final Object childCount = js.executeScript("return document.body ? document.body.children.length : 0");
                LOG.warn("   ⚠️ [DOM Extraction Failure Diagnostic] Captured 0 elements! URL: '{}' | Title: '{}' | ReadyState: '{}' | Body Children: {}",
                        url, title, readyState, childCount);
            } catch (final Exception ignored) {
            }
        }

        final String result = dom.toString();
        final long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        if (!isEmptyPage) {
            LOG.debug("   📄 DOM Capture Completed in {} ms | Level: {} | Size: {} chars | Elements: {} | Windows: {}",
                    elapsedMs, level, result.length(), totalElements, windowCount);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private int captureFrameTree(final StringBuilder dom, final ContextLevel level, final String windowHandle,
            final String framePath, final boolean showFrameId, final WebDriver driver) {
        final String frameId = windowHandle + ":" + framePath;
        if (!(driver instanceof final JavascriptExecutor js)) {
            return 0;
        }
        int elementCount = 0;
        try {
            final long scriptStart = System.nanoTime();
            final Map<String, Object> data = (Map<String, Object>) js
                    .executeScript(CAPTURE_SCRIPT, level.ordinal(), level.includesTextContent(), level.includesRichMetadata(),
                            level == ContextLevel.MINIMAL,
                            this.volatileIdDetector.getPatterns().stream().map(java.util.regex.Pattern::pattern).toList());
            final long scriptMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - scriptStart);

            // Render element tree
            final List<Map<String, Object>> tree = (List<Map<String, Object>>) data.get("tree");
            if (tree != null && !tree.isEmpty()) {
                if (showFrameId) {
                    dom.append("=== Structural DOM Tree (Frame: ").append(frameId).append(") ===\n");
                } else {
                    dom.append("=== Structural DOM Tree ===\n");
                }
                for (final Map<String, Object> node : tree) {
                    elementCount += formatElementNode(dom, node, 0, showFrameId, frameId);
                }
            }
            LOG.debug("   ⚡ [Stage 2: JS Execution] Frame '{}' extracted {} nodes in {} ms", frameId, elementCount, scriptMs);

            // Now recursively process iframes in this frame
            final long frameStart = System.nanoTime();
            final List<WebElement> frames = driver.findElements(By.cssSelector("iframe, frame"));
            int skippedFrames = 0;
            for (int i = 0; i < frames.size(); i++) {
                try {
                    // Generate a stable selector for this frame in the parent context
                    final String selector = (String) js.executeScript("""
                            var el = arguments[0];
                            if (!el || !el.isConnected) return null;
                            var style = window.getComputedStyle(el);
                            if (style.display === 'none' || style.visibility === 'hidden') return null;
                            var rect = el.getBoundingClientRect();
                            if (rect.width <= 0 || rect.height <= 0) return null;
                            var src = (el.getAttribute('src') || '').toLowerCase();
                            if (src.indexOf('googlesyndication') !== -1 || src.indexOf('doubleclick') !== -1 ||
                                src.indexOf('google-analytics') !== -1 || src.indexOf('facebook.com') !== -1 ||
                                src.indexOf('adnxs') !== -1 || src.indexOf('adform') !== -1 || src.indexOf('amazon-adsystem') !== -1 ||
                                src.indexOf('youtube') !== -1 || src.indexOf('vimeo') !== -1 || src.indexOf('soundcloud') !== -1 ||
                                src.indexOf('spotify') !== -1 || src.indexOf('twitter') !== -1 || src.indexOf('instagram') !== -1 ||
                                src.indexOf('maps.google') !== -1 || src.indexOf('codepen') !== -1) {
                                return null;
                            }
                            try {
                                if (el.contentWindow && el.contentWindow.location) {
                                    var testLoc = el.contentWindow.location.href;
                                }
                            } catch (e) {
                                return null;
                            }
                            function escapeId(str) {
                              if (typeof CSS !== 'undefined' && CSS.escape) { return CSS.escape(str); }
                              return str;
                            }
                            function escapeAttr(str) {
                              return (str || '').replaceAll("'", "\\\\'");
                            }
                            if (el.id) { return '#' + escapeId(el.id); }
                            if (el.name) { return el.tagName.toLowerCase() + "[name='" + escapeAttr(el.name) + "']"; }
                            var path = [];
                            while (el && el.nodeType === 1) {
                              if (el.id) {
                                path.unshift('#' + escapeId(el.id));
                                break;
                              }
                              var tag = el.tagName.toLowerCase();
                              var parent = el.parentNode;
                              if (parent) {
                                var siblings = Array.from(parent.children).filter(function(s) { return s.tagName === el.tagName; });
                                if (siblings.length > 1) {
                                  tag += ':nth-of-type(' + (siblings.indexOf(el) + 1) + ')';
                                }
                              }
                              path.unshift(tag);
                              el = el.parentNode;
                            }
                            return path.join(' > ');
                            """,
                            frames.get(i));
                    if (selector == null) {
                        skippedFrames++;
                        continue;
                    }
                    driver.switchTo().frame(frames.get(i));
                    elementCount += captureFrameTree(dom, level, windowHandle, framePath + " >>> " + selector, showFrameId, driver);
                    driver.switchTo().parentFrame();
                } catch (final Exception e) {
                    LOG.debug("Could not switch to or process frame: {}", e.getMessage());
                    try {
                        driver.switchTo().parentFrame();
                    } catch (final Exception ignored) {
                        try {
                            driver.switchTo().defaultContent();
                        } catch (final Exception ignored2) {
                        }
                    }
                }
            }
            if (!frames.isEmpty()) {
                final long frameMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - frameStart);
                LOG.debug("   🖼️ [Stage 3: Frame Traversal] Inspected {} frame candidates (skipped {} ad/hidden frames) in {} ms",
                        frames.size(), skippedFrames, frameMs);
            }
        } catch (final Exception e) {
            LOG.warn("Failed to capture DOM for frame {}: {}", frameId, e.getMessage());
        }
        return elementCount;
    }

    /**
     * Returns a compact page context combining URL, title, and key element info.
     * Uses {@link ContextLevel#MINIMAL} by default.
     */
    public String getPageContext() {
        return getPageContext(ContextLevel.MINIMAL);
    }

    /**
     * Returns a compact page context combining URL, title, and key element info.
     *
     * @param forValidation
     *                      whether to include more text content for validation
     * @deprecated Use {@link #getPageContext(ContextLevel)} instead.
     */
    @Deprecated
    public String getPageContext(final boolean forValidation) {
        return captureSimplifiedDom(forValidation ? ContextLevel.STANDARD : ContextLevel.MINIMAL);
    }

    /**
     * Returns a compact page context at the specified context level.
     *
     * @param level
     *              the context level controlling how much DOM data to capture
     * @return simplified DOM as a structured text
     */
    public String getPageContext(final ContextLevel level) {
        return captureSimplifiedDom(level);
    }

    private final VolatileIdDetector volatileIdDetector = new VolatileIdDetector();

    /**
     * Formats a single node in the compact structural tree into the output string builder,
     * maintaining 2-space indentation depth and container tags (<header>, <main>, <article>, etc.).
     */
    @SuppressWarnings("unchecked")
    private int formatElementNode(final StringBuilder dom, final Map<String, Object> node, final int depth, final boolean showFrameId, final String frameId)
    {
        if (node == null)
        {
            return 0;
        }

        final String indent = "  ".repeat(depth);
        final String nodeType = (String) node.get("nodeType");
        int count = 0;

        if ("container".equals(nodeType))
        {
            count = 1;
            final String tag = (String) node.get("tagName");
            dom.append(indent).append("<").append(tag);
            final Object rawId = node.get("id");
            if (rawId != null && !this.volatileIdDetector.isVolatile(rawId.toString()))
            {
                appendAttribute(dom, "id", rawId);
            }
            appendAttribute(dom, "class", node.get("className"));
            appendAttribute(dom, "name", node.get("name"));
            appendAttribute(dom, "role", node.get("role"));
            appendAttribute(dom, "aria-label", node.get("ariaLabel"));
            appendAttribute(dom, "data-ai", node.get("automationId"));
            dom.append(">\n");

            final List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            if (children != null)
            {
                for (final Map<String, Object> child : children)
                {
                    count += formatElementNode(dom, child, depth + 1, showFrameId, frameId);
                }
            }

            dom.append(indent).append("</").append(tag).append(">\n");
        }
        else if ("leaf".equals(nodeType))
        {
            count = 1;
            if (showFrameId)
            {
                node.put("frameId", frameId);
            }
            dom.append(indent);
            formatElement(dom, node);
        }
        return count;
    }

    /**
     * Formats a single element map into the output string builder. Produces the
     * same text format as the original
     * per-element approach.
     */
    private void formatElement(final StringBuilder dom, final Map<String, Object> el) {
        final Object tagObj = el.get("tagName");
        final String label = (tagObj != null && !tagObj.toString().isEmpty()) ? tagObj.toString() : (el.get("label") != null ? el.get("label").toString() : "element");
        dom.append("<").append(label);

        final Object rawId = el.get("id");
        if (rawId != null && !this.volatileIdDetector.isVolatile(rawId.toString()))
        {
            appendAttribute(dom, "id", rawId);
        }
        appendAttribute(dom, "class", el.get("className"));
        appendAttribute(dom, "name", el.get("name"));
        appendAttribute(dom, "type", el.get("type"));
        appendAttribute(dom, "role", el.get("role"));
        appendAttribute(dom, "checked", el.get("checked"));
        appendAttribute(dom, "selected", el.get("selected"));

        final String text = (String) el.get("text");

        appendAttribute(dom, "href", el.get("href"));
        appendAttribute(dom, "placeholder", el.get("placeholder"));
        appendAttribute(dom, "aria-label", el.get("ariaLabel"));
        appendAttribute(dom, "pattern", el.get("pattern"));
        appendAttribute(dom, "title", el.get("title"));
        appendAttribute(dom, "min", el.get("min"));
        appendAttribute(dom, "max", el.get("max"));
        appendAttribute(dom, "minlength", el.get("minlength"));
        appendAttribute(dom, "maxlength", el.get("maxlength"));
        appendAttribute(dom, "step", el.get("step"));
        appendAttribute(dom, "autocomplete", el.get("autocomplete"));
        appendAttribute(dom, "required", el.get("required"));
        appendAttribute(dom, "readonly", el.get("readonly"));
        appendAttribute(dom, "disabled", el.get("disabled"));
        appendAttribute(dom, "hidden", el.get("hidden"));
        appendAttribute(dom, "multiple", el.get("multiple"));
        appendAttribute(dom, "value", el.get("value"));
        appendAttribute(dom, "options", el.get("options"));

        appendAttribute(dom, "data-ai", el.get("automationId"));

        appendAttribute(dom, "frameId", el.get("frameId"));

        if (text != null && !text.isEmpty()) {
            dom.append(">").append(escapeHtmlText(text)).append("</").append(label).append(">\n");
        } else {
            dom.append("/>\n");
        }
    }

    /**
     * Appends an attribute name and its escaped double-quoted value to the dom
     * builder if present.
     */
    private void appendAttribute(final StringBuilder dom, final String key, final Object value) {
        if (value != null && !value.toString().isEmpty()) {
            dom.append(" ").append(key).append("=\"").append(escapeAttributeValue(value.toString())).append("\"");
        }
    }

    /**
     * Escapes special XML/HTML entity characters in attribute values.
     */
    private String escapeAttributeValue(final String val) {
        if (val == null) {
            return "";
        }
        return val.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Escapes standard HTML entity characters in text content.
     */
    private String escapeHtmlText(final String val) {
        if (val == null) {
            return "";
        }
        return val.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Loads a classpath resource file as a UTF-8 string.
     *
     * @param resourceName the path to the resource file relative to the classpath root
     * @return the string content of the resource file
     */
    private static String loadResource(final String resourceName)
    {
        try (final InputStream is = PageAnalyzer.class.getClassLoader().getResourceAsStream(resourceName))
        {
            if (is == null)
            {
                throw new IllegalStateException("Resource not found: " + resourceName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Failed to load resource: " + resourceName, e);
        }
    }

    /**
     * Extracts all visible interactive elements across windows, frames, and shadow DOM roots
     * as structured {@link DomFeatureVector} objects for sub-millisecond local similarity matching.
     *
     * @return list of extracted DOM feature vectors
     */
    public List<DomFeatureVector> extractFeatureVectors()
    {
        return extractFeatureVectors(resolveDriver(null));
    }

    /**
     * Extracts all visible interactive elements as structured {@link DomFeatureVector} objects
     * using the specified explicit WebDriver instance.
     *
     * @param explicitDriver the WebDriver instance to evaluate against
     * @return list of extracted DOM feature vectors
     */
    public List<DomFeatureVector> extractFeatureVectors(final WebDriver explicitDriver)
    {
        final WebDriver driver = resolveDriver(explicitDriver);
        if (driver == null)
        {
            return Collections.emptyList();
        }

        final String script = """
            return (function() {
                var roots = [document];
                function collectRoots(root) {
                    try {
                        var all = root.querySelectorAll('*');
                        for (var i = 0; i < all.length; i++) {
                            if (all[i].shadowRoot) {
                                roots.push(all[i].shadowRoot);
                                collectRoots(all[i].shadowRoot);
                            }
                        }
                    } catch(e) {}
                }
                collectRoots(document);

                var results = [];
                for (var r = 0; r < roots.length; r++) {
                    try {
                        var els = roots[r].querySelectorAll('a, button, input, select, textarea, [role], [onclick], [data-testid], [data-test], [data-qa]');
                        for (var i = 0; i < els.length; i++) {
                            var el = els[i];
                            if (el.closest && el.closest('.neodymium-ai-hud')) continue;
                            var tag = el.tagName ? el.tagName.toLowerCase() : '';
                            var text = (el.innerText || el.value || el.placeholder || '').trim().replace(/\\s*\\n\\s*/g, ' ');
                            var classes = Array.from(el.classList || []);
                            var attrMap = {};
                            if (el.attributes) {
                                for (var a = 0; a < el.attributes.length; a++) {
                                    var attr = el.attributes[a];
                                    if (attr.name !== 'class' && attr.name !== 'style' && !attr.name.startsWith('data-ai')) {
                                        attrMap[attr.name] = attr.value;
                                    }
                                }
                            }
                            var role = el.getAttribute('role') || '';
                            var accessibleName = el.getAttribute('aria-label') || el.getAttribute('title') || el.getAttribute('placeholder') || (el.labels && el.labels[0] ? el.labels[0].innerText : '') || text;
                            var parentTag = el.parentElement ? (el.parentElement.tagName ? el.parentElement.tagName.toLowerCase() : '') : '';
                            var siblingIndex = el.parentElement ? Array.prototype.indexOf.call(el.parentElement.children, el) : 0;
                            var rect = el.getBoundingClientRect ? el.getBoundingClientRect() : {left: 0, top: 0, width: 0, height: 0};
                            results.push({
                                tag: tag,
                                text: text,
                                classes: classes,
                                attributes: attrMap,
                                role: role,
                                accessibleName: accessibleName ? accessibleName.trim() : '',
                                parentTag: parentTag,
                                siblingIndex: siblingIndex >= 0 ? siblingIndex : 0,
                                x: Math.round(rect.left),
                                y: Math.round(rect.top),
                                width: Math.round(rect.width),
                                height: Math.round(rect.height)
                            });
                        }
                    } catch(e) {}
                }
                return JSON.stringify(results);
            })();
            """;

        try
        {
            final Object response = ((JavascriptExecutor) driver).executeScript(script);
            if (response instanceof String jsonStr && !jsonStr.isBlank())
            {
                final ObjectMapper mapper = new ObjectMapper();
                final TypeReference<List<DomFeatureVector>> typeRef = new TypeReference<>() {};
                return mapper.readValue(jsonStr, typeRef);
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to extract DOM Feature Vectors: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * Extracts a structured {@link DomFeatureVector} for a specific target {@link WebElement}.
     *
     * @param element the WebElement to extract features from
     * @return the extracted DomFeatureVector, or null if element is invalid
     */
    public DomFeatureVector extractFeatureVector(final WebElement element)
    {
        return extractFeatureVector(resolveDriver(null), element);
    }

    /**
     * Extracts a structured {@link DomFeatureVector} for a specific target {@link WebElement}
     * using the specified explicit WebDriver instance.
     *
     * @param explicitDriver the WebDriver instance to evaluate against
     * @param element        the WebElement to extract features from
     * @return the extracted DomFeatureVector, or null if element is invalid
     */
    public DomFeatureVector extractFeatureVector(final WebDriver explicitDriver, final WebElement element)
    {
        if (element == null)
        {
            return null;
        }

        final WebDriver driver = resolveDriver(explicitDriver);
        if (driver == null)
        {
            return null;
        }

        final String script = """
            return (function(el) {
                if (!el) return null;
                var tag = el.tagName ? el.tagName.toLowerCase() : '';
                var text = (el.innerText || el.value || el.placeholder || '').trim().replace(/\\s*\\n\\s*/g, ' ');
                var classes = Array.from(el.classList || []);
                var attrMap = {};
                if (el.attributes) {
                    for (var a = 0; a < el.attributes.length; a++) {
                        var attr = el.attributes[a];
                        if (attr.name !== 'class' && attr.name !== 'style' && !attr.name.startsWith('data-ai')) {
                            attrMap[attr.name] = attr.value;
                        }
                    }
                }
                var role = el.getAttribute('role') || '';
                var accessibleName = el.getAttribute('aria-label') || el.getAttribute('title') || el.getAttribute('placeholder') || (el.labels && el.labels[0] ? el.labels[0].innerText : '') || text;
                var parentTag = el.parentElement ? (el.parentElement.tagName ? el.parentElement.tagName.toLowerCase() : '') : '';
                var siblingIndex = el.parentElement ? Array.prototype.indexOf.call(el.parentElement.children, el) : 0;
                var rect = el.getBoundingClientRect ? el.getBoundingClientRect() : {left: 0, top: 0, width: 0, height: 0};
                return JSON.stringify({
                    tag: tag,
                    text: text,
                    classes: classes,
                    attributes: attrMap,
                    role: role,
                    accessibleName: accessibleName ? accessibleName.trim() : '',
                    parentTag: parentTag,
                    siblingIndex: siblingIndex >= 0 ? siblingIndex : 0,
                    x: Math.round(rect.left),
                    y: Math.round(rect.top),
                    width: Math.round(rect.width),
                    height: Math.round(rect.height)
                });
            })(arguments[0]);
            """;

        try
        {
            final Object response = ((JavascriptExecutor) driver).executeScript(script, element);
            if (response instanceof String jsonStr && !jsonStr.isBlank())
            {
                final ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(jsonStr, DomFeatureVector.class);
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to extract DOM Feature Vector for element: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Finds the live interactive WebElement matching a recorded {@link DomFeatureVector}
     * with similarity score exceeding the given minimum threshold.
     *
     * @param explicitDriver the WebDriver instance
     * @param recordedVector the target recorded DomFeatureVector
     * @param minScore       the minimum required similarity score (e.g. 0.80)
     * @return the matched WebElement, or null if no candidate met the threshold
     */
    public WebElement findLiveElementByFeatureVector(final WebDriver explicitDriver, final DomFeatureVector recordedVector, final double minScore)
    {
        if (recordedVector == null)
        {
            return null;
        }

        final WebDriver driver = resolveDriver(explicitDriver);
        if (driver == null)
        {
            return null;
        }

        final String script = """
            return (function() {
                var roots = [document];
                function collectRoots(root) {
                    try {
                        var all = root.querySelectorAll('*');
                        for (var i = 0; i < all.length; i++) {
                            if (all[i].shadowRoot) {
                                roots.push(all[i].shadowRoot);
                                collectRoots(all[i].shadowRoot);
                            }
                        }
                    } catch(e) {}
                }
                collectRoots(document);

                var elements = [];
                var vectorData = [];
                for (var r = 0; r < roots.length; r++) {
                    try {
                        var els = roots[r].querySelectorAll('a, button, input, select, textarea, [role], [onclick], [data-testid], [data-test], [data-qa]');
                        for (var i = 0; i < els.length; i++) {
                            var el = els[i];
                            if (el.closest && el.closest('.neodymium-ai-hud')) continue;
                            var tag = el.tagName ? el.tagName.toLowerCase() : '';
                            var text = (el.innerText || el.value || el.placeholder || '').trim().replace(/\\s*\\n\\s*/g, ' ');
                            var classes = Array.from(el.classList || []);
                            var attrMap = {};
                            if (el.attributes) {
                                for (var a = 0; a < el.attributes.length; a++) {
                                    var attr = el.attributes[a];
                                    if (attr.name !== 'class' && attr.name !== 'style' && !attr.name.startsWith('data-ai')) {
                                        attrMap[attr.name] = attr.value;
                                    }
                                }
                            }
                            var role = el.getAttribute('role') || '';
                            var accessibleName = el.getAttribute('aria-label') || el.getAttribute('title') || el.getAttribute('placeholder') || (el.labels && el.labels[0] ? el.labels[0].innerText : '') || text;
                            var parentTag = el.parentElement ? (el.parentElement.tagName ? el.parentElement.tagName.toLowerCase() : '') : '';
                            var siblingIndex = el.parentElement ? Array.prototype.indexOf.call(el.parentElement.children, el) : 0;
                            var rect = el.getBoundingClientRect ? el.getBoundingClientRect() : {left: 0, top: 0, width: 0, height: 0};
                            elements.push(el);
                            vectorData.push({
                                tag: tag,
                                text: text,
                                classes: classes,
                                attributes: attrMap,
                                role: role,
                                accessibleName: accessibleName ? accessibleName.trim() : '',
                                parentTag: parentTag,
                                siblingIndex: siblingIndex >= 0 ? siblingIndex : 0,
                                x: Math.round(rect.left),
                                y: Math.round(rect.top),
                                width: Math.round(rect.width),
                                height: Math.round(rect.height)
                            });
                        }
                    } catch(e) {}
                }
                window.__neo_candidate_elements = elements;
                return JSON.stringify(vectorData);
            })();
            """;

        try
        {
            final Object response = ((JavascriptExecutor) driver).executeScript(script);
            if (response instanceof String jsonStr && !jsonStr.isBlank())
            {
                final ObjectMapper mapper = new ObjectMapper();
                final TypeReference<List<DomFeatureVector>> typeRef = new TypeReference<>() {};
                final List<DomFeatureVector> candidates = mapper.readValue(jsonStr, typeRef);
                if (LOG.isTraceEnabled())
                {
                    LOG.trace("🧬 Evaluating {} candidate elements against target vector: {}", candidates.size(), recordedVector.toSummaryString());
                }

                int bestIndex = -1;
                double bestScore = -1.0;
                for (int i = 0; i < candidates.size(); i++)
                {
                    final DomFeatureVector candidate = candidates.get(i);
                    final double baseScore = LocatorCascadeResolver.computeSimilarity(recordedVector, candidate);
                    double tieBreaker = 0.0;
                    if (recordedVector.getParentTag() != null && candidate.getParentTag() != null
                        && recordedVector.getParentTag().equalsIgnoreCase(candidate.getParentTag()))
                    {
                        tieBreaker += 0.005;
                    }
                    final int indexDiff = Math.abs(recordedVector.getSiblingIndex() - candidate.getSiblingIndex());
                    tieBreaker += Math.max(0.0, 0.005 * (1.0 - (indexDiff / 10.0)));

                    final double totalScore = baseScore + tieBreaker;
                    final boolean isMatch = baseScore >= (minScore - 1e-5);
                    if (isMatch && totalScore > bestScore)
                    {
                        bestScore = totalScore;
                        bestIndex = i;
                    }

                    if (LOG.isTraceEnabled())
                    {
                        final String matchBadge = isMatch ? " ★ CANDIDATE" : "";
                        LOG.trace("   ├─ Candidate #{} [Score: {} / Min: {}] (base: {}, tie: +{}){}",
                            i + 1, String.format("%.4f", totalScore), minScore, String.format("%.4f", baseScore), String.format("%.4f", tieBreaker), matchBadge);
                        for (final String line : candidate.toFormattedLines("   │  ", "   │  "))
                        {
                            LOG.trace(line);
                        }
                    }
                }

                if (bestIndex >= 0)
                {
                    LOG.trace("   └─ ✅ Matched Candidate #{} with score {} (>= minScore {})", bestIndex + 1, String.format("%.4f", bestScore), minScore);
                    final Object elResponse = ((JavascriptExecutor) driver).executeScript(
                        "var el = (window.__neo_candidate_elements && window.__neo_candidate_elements[" + bestIndex + "]) ? window.__neo_candidate_elements[" + bestIndex + "] : null; "
                        + "delete window.__neo_candidate_elements; return el;");
                    if (elResponse instanceof WebElement webElement)
                    {
                        return webElement;
                    }
                }
                else
                {
                    LOG.trace("   └─ ❌ No candidate met minimum score threshold {}", minScore);
                    ((JavascriptExecutor) driver).executeScript("delete window.__neo_candidate_elements;");
                }
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to find live element by DOM feature vector: {}", e.getMessage());
        }

        return null;
    }

    private static boolean isImplicitRole(final String tag, final String role)
    {
        if (tag == null || role == null)
        {
            return false;
        }
        final String t = tag.toLowerCase();
        final String r = role.toLowerCase();
        return (t.equals("a") && r.equals("link"))
            || (t.equals("button") && r.equals("button"))
            || (t.equals("header") && r.equals("banner"))
            || (t.equals("nav") && r.equals("navigation"))
            || (t.equals("main") && r.equals("main"))
            || (t.equals("footer") && r.equals("contentinfo"))
            || (t.matches("^h[1-6]$") && r.equals("heading"))
            || (t.equals("textarea") && r.equals("textbox"))
            || (t.equals("form") && r.equals("form"))
            || (t.equals("select") && (r.equals("combobox") || r.equals("select")))
            || (t.equals("option") && r.equals("option"));
    }
}
