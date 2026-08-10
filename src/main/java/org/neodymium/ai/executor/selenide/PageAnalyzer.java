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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chromium.HasCdp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neodymium.common.ScreenshotWriter;
import org.neodymium.ai.executor.selenide.ContextLevel;

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

                // Builds a highly unique, compact CSS selector for the element to serve as alternative locator
                function generateSelector(el) {
                    // Helper to check if a selector matches exactly one element in the current DOM scope
                    function isUnique(sel) {
                        try {
                            return document.querySelectorAll(sel).length === 1;
                        } catch (e) {
                            return false;
                        }
                    }

                    // Helper to safely escape special CSS characters (such as dots or colons in IDs or class names)
                    function escapeIdentifier(str) {
                        if (typeof CSS !== 'undefined' && CSS.escape) {
                            return CSS.escape(str);
                        }
                        return str;
                    }

                    // Step 1: Check for unique immediate attributes (ID or Name) to keep selectors minimal
                    if (el.id) {
                        var idSel = '#' + escapeIdentifier(el.id);
                        if (isUnique(idSel)) {
                            return idSel;
                        }
                    }

                    var tag = el.tagName.toLowerCase();
                    var name = el.getAttribute('name');
                    if (name) {
                        var nameSel = tag + "[name='" + name.replaceAll("'", "\\\\'") + "']";
                        if (isUnique(nameSel)) {
                            return nameSel;
                        }
                    }

                    // Step 2: Climb the DOM hierarchy to construct a highly specific unique path
                    var path = [];
                    var current = el;

                    // Walk upwards until we hit the root/body element or null
                    while (current && current.nodeType === 1) { // 1 represents Node.ELEMENT_NODE
                        var currentTag = current.tagName.toLowerCase();

                        // If we reach body or html, append and terminate the path climbing
                        if (currentTag === 'body' || currentTag === 'html') {
                            path.unshift(currentTag);
                            break;
                        }

                        // Terminate early if the ancestor has a globally unique ID
                        if (current.id) {
                            var idSel = '#' + escapeIdentifier(current.id);
                            if (isUnique(idSel)) {
                                path.unshift(idSel);
                                break;
                            }
                        }

                        // Construct current path segment starting with tag name
                        var segment = currentTag;

                        // Append class names to segment to increase specificity
                        var className = current.className;
                        if (typeof className === 'string' && className.trim()) {
                            // Split by whitespace to extract individual class names
                            var classes = className.trim().split(new RegExp('\\s+')).filter(Boolean);
                            if (classes.length > 0) {
                                segment += '.' + classes.map(escapeIdentifier).join('.');
                            }
                        }

                        // Disambiguate among siblings sharing the same tag using :nth-of-type(index)
                        if (current.parentNode) {
                            var siblings = Array.from(current.parentNode.children);
                            var sameTagSiblings = siblings.filter(function(s) {
                                return s.tagName === current.tagName;
                            });
                            if (sameTagSiblings.length > 1) {
                                var index = sameTagSiblings.indexOf(current) + 1;
                                segment += ':nth-of-type(' + index + ')';
                            }
                        }

                        // Insert the computed segment at the beginning of the path
                        path.unshift(segment);

                        // Check if the current accumulated path is already globally unique
                        var currentPath = path.join(' > ');
                        if (isUnique(currentPath)) {
                            return currentPath;
                        }

                        // Walk up to parent node
                        current = current.parentNode;
                    }

                    // Return final constructed path
                    return path.join(' > ');
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
                                selector: generateSelector(el),
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
                                selector: generateSelector(el),
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

                // Compile all standard target elements that represent interactive page actions
                var interactiveElements = captureElements('a', 'link')
                    .concat(captureElements('button', 'button'))
                    .concat(captureElements('input', 'input'))
                    .concat(captureElements('select', 'select'))
                    .concat(captureElements('option', 'option'))
                    .concat(captureElements('textarea', 'textarea'))
                    .concat(captureClickableElements('div, span, tr, td, th, li, label, dialog, svg, img, canvas', 'clickable'));

                // Helper to retrieve the most meaningful text label representation of an element
                var getDisplayLabel = function(elObj) {
                    return elObj.text || elObj.placeholder || elObj.value || elObj.ariaLabel || elObj.title || elObj.name || '';
                };

                // Compute element label frequency map to identify duplicate labels requiring parent text context
                var textCounts = {};
                for (var i = 0; i < interactiveElements.length; i++) {
                    var lbl = getDisplayLabel(interactiveElements[i]);
                    if (lbl) {
                        textCounts[lbl] = (textCounts[lbl] || 0) + 1;
                    }
                }

                // Perform parent walk to disambiguate elements sharing the identical display label
                for (var i = 0; i < interactiveElements.length; i++) {
                    var elObj = interactiveElements[i];
                    var lbl = getDisplayLabel(elObj);
                    if (lbl && textCounts[lbl] > 1 && elObj.domElement) {
                        var parentText = '';
                        var p = elObj.domElement.parentElement;
                        var depth = 0;
                        while (p && p !== document.body && depth < 3) {
                            var tag = p.tagName.toLowerCase();
                            var role = p.getAttribute('role') || '';
                            var cls = (typeof p.className === 'string' ? p.className : '').toLowerCase();
                            var id = (p.id || '').toLowerCase();
                            if (tag === 'header' || tag === 'footer' || tag === 'nav' || tag === 'aside') break;
                            if (role === 'banner' || role === 'navigation' || role === 'contentinfo' || role === 'complementary') break;
                            if (cls.includes('navbar') || cls.includes('header') || cls.includes('footer') ||
                                id.includes('navbar') || id.includes('header') || id.includes('footer')) break;
                            var pText = (p.innerText || '').trim();
                            if (pText.length > lbl.length && pText.length < 300) {
                                parentText = pText;
                                break;
                            }
                            if (pText.length >= 300) break;
                            p = p.parentElement;
                            depth++;
                        }
                        if (parentText) {
                            elObj.parentText = truncate(parentText.replace(new RegExp('\\s*\\n\\s*', 'g'), ' | '), MAX_TEXT);
                            if (elObj.domElement && typeof elObj.domElement.setAttribute === 'function') {
                                elObj.domElement.setAttribute('data-parent-text', elObj.parentText);
                            }
                        }
                    }
                    delete elObj.domElement;
                }

                // Pre-stamp headings and text content in exact original sequence
                captureElements('h1', 'heading')
                    .concat(captureElements('h2', 'heading'))
                    .concat(captureElements('h3', 'heading'))
                    .concat(captureElements('h4', 'heading'))
                    .concat(captureElements('h5', 'heading'));


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
                    if (!el || !isVisible(el)) return null;
                    var tag = el.tagName ? el.tagName.toLowerCase() : '';
                    if (['script', 'style', 'svg', 'noscript', 'meta', 'link', 'head'].indexOf(tag) !== -1) return null;
                    if (el.closest && el.closest('.neodymium-ai-hud')) return null;

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
                            focused: (document.activeElement === el) ? 'true' : null,
                            placeholder: el.getAttribute('placeholder'),
                            ariaLabel: el.getAttribute('aria-label'),
                            value: getElementValue(el, tag),
                            options: options,
                            selector: generateSelector(el),
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
                        // In LEAN mode (!includesText), filter out massive paragraph copy (> 120 chars)
                        if (!includesText) {
                            if ((tag === 'p' || tag === 'blockquote') && textContent.length > 120) {
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
                            selector: generateSelector(el),
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
        return captureScreenshot(title, null);
    }

    public String captureScreenshot(final String title, final WebDriver explicitDriver) throws IOException
    {
        final WebDriver driver = resolveDriver(explicitDriver);
        if (!hasActiveWebDriver(driver))
        {
            return null;
        }
        LOG.debug("   📸 Capturing screenshot for: {}", title);
        try
        {
            return captureScreenshotInternal(title, driver);
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
                        return captureScreenshotInternal(title, driver);
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

    private String captureScreenshotInternal(final String title, final WebDriver driver) throws Exception
    {
        boolean hidden = false;
        if (driver instanceof final JavascriptExecutor js)
        {
            try
            {
                final String script = """
                        var callback = arguments[arguments.length - 1];
                        if (!document.querySelector('link[href*="font-awesome"]')) {
                            var fa = document.createElement('link');
                            fa.rel = 'stylesheet';
                            fa.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css';
                            document.head.appendChild(fa);
                        }
                        if (document.getElementById('neo-screenshot-flash-overlay')) {
                            var hud0 = document.getElementById('neodymium-ai-hud-container');
                            if (hud0 && hud0.style.display !== 'none') { hud0.style.display = 'none'; callback(true); } else { callback(false); }
                            return;
                        }
                        var hud = document.getElementById('neodymium-ai-hud-container');
                        var hasHud = !!(hud && hud.style.display !== 'none');
                        var activeEl = document.activeElement;
                        var overlay = document.createElement('div');
                        overlay.id = 'neo-screenshot-flash-overlay';
                        overlay.style.cssText = 'position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background-color: rgba(255,255,255,0); z-index: 2147483646; display: flex; align-items: center; justify-content: center; pointer-events: none; transition: background-color 0.12s ease-out;';
                        var badge = document.createElement('div');
                        badge.style.cssText = 'background: rgba(30,30,46,0.85); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.12); border-radius: 50%; width: 110px; height: 110px; display: flex; align-items: center; justify-content: center; box-shadow: 0 12px 36px rgba(0,0,0,0.55); opacity: 0; transform: scale(0.75); transition: opacity 0.18s ease, transform 0.18s cubic-bezier(0.175,0.885,0.32,1.275);';
                        var icon = document.createElement('i');
                        icon.className = 'fa-solid fa-camera';
                        icon.style.cssText = 'font-size: 46px; color: #fff; text-shadow: 0 2px 8px rgba(0,0,0,0.4);';
                        badge.appendChild(icon);
                        overlay.appendChild(badge);
                        document.body.appendChild(overlay);
                        void overlay.offsetWidth;
                        overlay.style.backgroundColor = 'rgba(255,255,255,0.38)';
                        setTimeout(function() {
                            overlay.style.backgroundColor = 'rgba(255,255,255,0)';
                            badge.style.opacity = '1';
                            badge.style.transform = 'scale(1)';
                        }, 55);
                        setTimeout(function() {
                            badge.style.opacity = '0';
                            badge.style.transform = 'scale(1.08)';
                            setTimeout(function() {
                                if (document.body.contains(overlay)) { document.body.removeChild(overlay); }
                                if (hasHud) { hud.style.display = 'none'; }
                                if (activeEl && activeEl !== document.body && typeof activeEl.focus === 'function') { try { activeEl.focus(); } catch (e) {} }
                                callback(hasHud);
                            }, 180);
                        }, 420);
                        """;
                final Object hudExists = js.executeAsyncScript(script);
                hidden = Boolean.TRUE.equals(hudExists);
            }
            catch (final Exception e)
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
        }

        try
        {
            return ScreenshotWriter.doScreenshot(
                    title.replaceAll("[^a-zA-Z0-9-]", "_").substring(0, Math.min(title.length(), 12)),
                    ScreenshotWriter.getFormatedReportsPath(), false, false);
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
        final WebDriver driver = resolveDriver(explicitDriver);
        if (!hasActiveWebDriver(driver)) {
            return "Page URL: <empty page>\nPage Title: \n\n";
        }
        final String url;
        final String title;
        try {
            url = driver.getCurrentUrl();
            title = driver.getTitle();
        } catch (final Exception e) {
            return "Page URL: <empty page>\nPage Title: \n\n";
        }
        final boolean isEmptyPage = "data:,".equals(url) || "about:blank".equals(url);

        if (!isEmptyPage) {
            LOG.debug("🔴 [DOM Capture: {}] URL: {}", level, url);
        }

        final StringBuilder dom = new StringBuilder();
        dom.append("Page URL: ").append(isEmptyPage ? "<empty page>" : url).append("\n");
        dom.append("Page Title: ").append(title != null ? title : "").append("\n\n");

        if (isEmptyPage || level == ContextLevel.VISUAL) {
            final String result = dom.toString();
            if (!isEmptyPage) {
                LOG.debug("   📄 Simplified DOM size: {} chars (VISUAL mode)", result.length());
            }
            return result;
        }

        final String currentWindow = driver.getWindowHandle();

        boolean showFrameId = true;
        try {
            final java.util.Set<String> windowHandles = driver.getWindowHandles();
            if (windowHandles.size() == 1 && com.codeborne.selenide.Selenide.$$("iframe, frame").isEmpty()) {
                showFrameId = false;
            }
        } catch (final Exception e) {
            // fallback
        }

        try {
            final Set<String> windowHandles = driver.getWindowHandles();
            final List<String> windowList = new ArrayList<>(windowHandles);
            for (int i = 0; i < windowList.size(); i++) {
                final String windowHandle = windowList.get(i);
                final String logicalWindowName = "win_" + i;
                driver.switchTo().window(windowHandle);

                // Omit window context header and URL/Title info if there is only a single
                // window
                // and no frames/iframes to avoid redundant output matching the top-level Page
                // URL and Title.
                if (showFrameId || windowList.size() > 1) {
                    dom.append("=== Window: ").append(logicalWindowName).append(" ===\n");
                    dom.append("URL: ").append(driver.getCurrentUrl()).append("\n");
                    dom.append("Title: ").append(driver.getTitle()).append("\n\n");
                }
                captureFrameTree(dom, level, logicalWindowName, "main", showFrameId, driver);
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

        final String result = dom.toString();
        if (!isEmptyPage) {
            LOG.debug("   📄 Simplified DOM size: {} chars", result.length());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void captureFrameTree(final StringBuilder dom, final ContextLevel level, final String windowHandle,
            final String framePath, final boolean showFrameId, final WebDriver driver) {
        final String frameId = windowHandle + ":" + framePath;
        if (!(driver instanceof final JavascriptExecutor js)) {
            return;
        }
        try {
            final Map<String, Object> data = (Map<String, Object>) js
                    .executeScript(CAPTURE_SCRIPT, level.ordinal(), level.includesTextContent(), level.includesRichMetadata(),
                            level == ContextLevel.MINIMAL,
                            this.volatileIdDetector.getPatterns().stream().map(java.util.regex.Pattern::pattern).toList());
            // Render element tree
            final List<Map<String, Object>> tree = (List<Map<String, Object>>) data.get("tree");
            if (tree != null && !tree.isEmpty()) {
                if (showFrameId) {
                    dom.append("=== Structural DOM Tree (Frame: ").append(frameId).append(") ===\n");
                } else {
                    dom.append("=== Structural DOM Tree ===\n");
                }
                for (final Map<String, Object> node : tree) {
                    formatElementNode(dom, node, 0, showFrameId, frameId);
                }
            }



            // Now recursively process iframes in this frame
            final List<WebElement> frames = driver.findElements(By.cssSelector("iframe, frame"));
            for (int i = 0; i < frames.size(); i++) {
                try {
                    // Generate a stable selector for this frame in the parent context
                    final String selector = (String) js.executeScript("""
                            var el = arguments[0];
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
                    driver.switchTo().frame(frames.get(i));
                    captureFrameTree(dom, level, windowHandle, framePath + " >>> " + selector, showFrameId, driver);
                    driver.switchTo().parentFrame();
                } catch (final Exception e) {
                    LOG.debug("Could not switch to frame: {}", e.getMessage());
                    try {
                        driver.switchTo().defaultContent();
                        // Recover path
                        if (!"main".equals(framePath)) {
                            if (framePath.contains(" >>> ")) {
                                final String[] selectors = framePath.split(" >>> ");
                                for (final String sel : selectors) {
                                    if (!sel.equals("main") && !sel.isBlank()) {
                                        final WebElement iframeElement = driver.findElement(org.openqa.selenium.By.cssSelector(sel));
                                        driver.switchTo().frame(iframeElement);
                                    }
                                }
                            } else {
                                final String[] indices = framePath.substring(5).split("\\."); // remove "main."
                                for (final String indexStr : indices) {
                                    if (!indexStr.equals("main") && !indexStr.isBlank()) {
                                        driver.switchTo().frame(Integer.parseInt(indexStr));
                                    }
                                }
                            }
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Failed to recover frame path context: {}", ex.getMessage());
                    }
                }
            }
        } catch (final Exception e) {
            LOG.warn("Failed to capture DOM for frame {}: {}", frameId, e.getMessage());
        }
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
    private void formatElementNode(final StringBuilder dom, final Map<String, Object> node, final int depth, final boolean showFrameId, final String frameId)
    {
        if (node == null)
        {
            return;
        }

        final String indent = "  ".repeat(depth);
        final String nodeType = (String) node.get("nodeType");

        if ("container".equals(nodeType))
        {
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
                    formatElementNode(dom, child, depth + 1, showFrameId, frameId);
                }
            }

            dom.append(indent).append("</").append(tag).append(">\n");
        }
        else if ("leaf".equals(nodeType))
        {
            if (showFrameId)
            {
                node.put("frameId", frameId);
            }
            dom.append(indent);
            formatElement(dom, node);
        }
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
     * Escapes special CSS characters in an identifier to match the CSS.escape
     * specification.
     */
    private String escapeCssIdentifier(final String str) {
        return str.replaceAll("([!\"#$%&'()*+,./:;<=>?@\\[\\]^`{|}~])", "\\\\$1");
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
