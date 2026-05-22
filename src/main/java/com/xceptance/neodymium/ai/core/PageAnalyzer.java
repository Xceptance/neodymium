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
package com.xceptance.neodymium.ai.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.common.ScreenshotWriter;

/**
 * Captures page context (screenshot + simplified DOM) for the LLM. The DOM is simplified to only include interactive
 * and visible elements to keep token usage low while giving the LLM enough context. All DOM queries are batched into a
 * single JavaScript execution to minimize WebDriver round-trips for performance.
  *
 * // AI-generated: Gemini 2.0 Flash
*/
public class PageAnalyzer
{
    private static final Logger LOG = LoggerFactory.getLogger(PageAnalyzer.class);

    /**
     * JavaScript that runs in the browser and extracts all needed DOM data in a single execution. Returns a Map with
     * "elements" (list of sections) and "forms" (list of form descriptors). Visibility is checked via offsetParent
     * (null means hidden for non-fixed/non-body elements) and getComputedStyle as fallback.
     */
    private static final String CAPTURE_SCRIPT = """
        return (function(level, includesText) {
            // Configuration constants to prevent payload bloat
            var MAX_PER_SELECTOR = 150; // Safeguard against massive list rendering
            var MAX_TEXT = 200;         // Max characters captured for element text labels
            var MAX_HREF = 180;         // Max URL length for captured links
            var MAX_VALUE = 150;        // Max characters for option/input value properties
            
            // Map of assigned automation IDs (data-neo-ref) to handle unique stamping
            var usedIds = {};
            
            // Pre-populate usedIds map with existing refs to avoid duplicates during consecutive runs
            document.querySelectorAll('[data-neo-ref]').forEach(function(el) {
                usedIds[el.getAttribute('data-neo-ref')] = true;
            });

            // Fast, low-overhead djb2 hashing algorithm to compute consistent element fingerprint hashes
            function djb2(str) {
                var hash = 5381;
                for (var i = 0; i < str.length; i++) {
                    hash = ((hash << 5) + hash) + str.charCodeAt(i);
                    hash = hash & 0x7fffffff; // Keep hash as a positive 31-bit integer
                }
                return hash.toString(36); // Compact alphanumeric base-36 representation
            }

            // Generates a stable fingerprint string for an element based on tags, IDs, classes, and parents
            function fingerprint(el) {
                var tag   = el.tagName.toLowerCase();
                var id    = el.id || '';
                var cls   = (typeof el.className === 'string' ? el.className : '').trim().replace(/\\s+/g, ' ');
                var ptag  = el.parentElement ? el.parentElement.tagName.toLowerCase() : '';
                var type  = el.getAttribute('type') || '';
                var name  = el.getAttribute('name') || el.getAttribute('alt') || '';
                var raw   = [tag, id, cls, ptag, type, name].join('|');
                return 'xc_' + djb2(raw);
            }

            // Retrieves or generates and stamps a unique 'data-neo-ref' ID on the DOM element
            function assignId(el) {
                if (el.hasAttribute('data-neo-ref')) {
                    return el.getAttribute('data-neo-ref');
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
                el.setAttribute('data-neo-ref', candidate);
                return candidate;
            }

            // Evaluates element visibility accurately by checking DOM connection, HUD boundaries, display, and layout rects
            function isVisible(el) {
                if (!el.isConnected) return false;
                // Never extract elements located inside the Neodymium AI Interactive HUD itself
                if (el.closest && el.closest('.neodymium-ai-hud')) return false;

                const style = window.getComputedStyle(el);
                if (style.display === 'none' || style.visibility === 'hidden') return false;

                const rect = el.getBoundingClientRect();
                return rect.width > 0 && rect.height > 0;
            }

            // Standard truncation helper to keep token sizes predictable and clean
            function truncate(s, max) {
                if (!s) return s;
                return s.length <= max ? s : s.substring(0, max) + '…';
            }

            // Deep DOM query selector supporting crawling through Shadow DOM roots recursively
            function queryAllDeep(selector, root) {
                root = root || document;
                var results = Array.from(root.querySelectorAll(selector));
                var allEls = root.querySelectorAll('*');
                for (var i = 0; i < allEls.length; i++) {
                    if (allEls[i].shadowRoot) {
                        results = results.concat(queryAllDeep(selector, allEls[i].shadowRoot));
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
                    // Fallback escaping mechanism for older or non-standard browser contexts
                    return str.replace(/([!"#$%&'()*+,./:;<=>?@\\[\\]^`{|}~])/g, '\\\\$1');
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
                    var nameSel = tag + "[name='" + name.replace(/'/g, "\\\\'") + "']";
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
                        var classes = className.trim().split(/\\s+/).filter(Boolean);
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
                    var count = 0;
                    for (var i = 0; i < els.length && count < MAX_PER_SELECTOR; i++) {
                        var el = els[i];
                        if (!isVisible(el)) continue;

                        count++;
                        var autoId = assignId(el);
                        var text = (el.innerText || '').trim().replace(/\\s*\\n\\s*/g, ' ');
                        var options = null;
                        // Format select dropdown options neatly
                        if (el.tagName.toLowerCase() === 'select') {
                            options = Array.from(el.options).slice(0, 50).map(o => o.text.trim()).filter(t => t.length > 0).join(', ');
                            if (el.options.length > 50) options += '... (total ' + el.options.length + ')';
                        }

                        results.push({
                            label: label,
                            text: text.length <= MAX_TEXT ? text : '',
                            id: el.id || null,
                            name: el.getAttribute('name'),
                            href: truncate(el.getAttribute('href'), MAX_HREF),
                            type: el.getAttribute('type'),
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
                            value: label !== 'input' ? truncate(el.getAttribute('value'), MAX_VALUE) : null,
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
                    var count = 0;
                    for (var i = 0; i < els.length && count < MAX_PER_SELECTOR; i++) {
                        var el = els[i];
                        if (!isVisible(el)) continue;
                        if (el.closest('a')) continue; // Skip if already wrapped inside standard anchor link

                        var style = window.getComputedStyle(el);
                        // A custom element is considered clickable if it has pointer cursor, onclick attribute, or onclick handler
                        var isClickable = style.cursor === 'pointer' || el.hasAttribute('onclick') || typeof el.onclick === 'function';
                        if (!isClickable) continue;

                        count++;
                        var autoId = assignId(el);
                        var text = (el.innerText || '').trim().replace(/\\s*\\n\\s*/g, ' ');
                        var options = null;
                        if (el.tagName.toLowerCase() === 'select') {
                            options = Array.from(el.options).slice(0, 50).map(o => o.text.trim()).filter(t => t.length > 0).join(', ');
                            if (el.options.length > 50) options += '... (total ' + el.options.length + ')';
                        }

                        results.push({
                            label: label,
                            text: text.length <= MAX_TEXT ? text : '',
                            id: el.id || null,
                            name: el.getAttribute('name'),
                            href: truncate(el.getAttribute('href'), MAX_HREF),
                            type: el.getAttribute('type'),
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
                            value: label !== 'input' ? truncate(el.getAttribute('value'), MAX_VALUE) : null,
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
                                type: inp.tagName.toLowerCase() === 'select' ? 'select' : (inp.getAttribute('type') || ''),
                                name: inp.getAttribute('name') || '',
                                id: inp.id || '',
                                automationId: autoId
                            };
                            if (inp.tagName.toLowerCase() === 'select') {
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
                .concat(captureClickableElements('div, span, tr, td, th', 'clickable'));

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
                    // If multiple elements share the same label on this page, resolve a local parent text 
                    // block (e.g. card name, list item, or table cell) to disambiguate them.
                    var parentText = '';
                    var p = elObj.domElement.parentElement;
                    var depth = 0;
                    
                    // Cap parent traversal to 3 levels to guarantee context remains local to the component (e.g. card/grid cell)
                    while (p && p !== document.body && depth < 3) {
                        var tag = p.tagName.toLowerCase();
                        var role = p.getAttribute('role') || '';
                        var cls = (typeof p.className === 'string' ? p.className : '').toLowerCase();
                        var id = (p.id || '').toLowerCase();

                        // Heuristic 1: Stop climbing if we reach major HTML5 semantic landmarks
                        if (tag === 'header' || tag === 'footer' || tag === 'nav' || tag === 'aside') {
                            break;
                        }
                        // Heuristic 2: Stop climbing if we hit standard global ARIA landmark roles
                        if (role === 'banner' || role === 'navigation' || role === 'contentinfo' || role === 'complementary') {
                            break;
                        }
                        // Heuristic 3: Stop climbing at typical layout containers (generic class/ID names)
                        if (cls.includes('navbar') || cls.includes('header') || cls.includes('footer') ||
                            id.includes('navbar') || id.includes('header') || id.includes('footer')) {
                            break;
                        }

                        var pText = (p.innerText || '').trim();
                        // Only capture context if it's descriptive (longer than the label itself) but compact (< 300 chars)
                        if (pText.length > lbl.length && pText.length < 300) {
                            parentText = pText;
                        }
                        // Prevent pulling in massive generic containers
                        if (pText.length >= 300) {
                            break;
                        }
                        p = p.parentElement;
                        depth++;
                    }
                    if (parentText) {
                        elObj.parentText = truncate(parentText.replace(/\\s*\\n\\s*/g, ' | '), MAX_TEXT);
                    }
                }
                delete elObj.domElement; // Remove reference to allow browser garbage collection
            }

            // LEVEL 1 (LEAN Mode): Capture all compiled interactive elements and page structure headings
            if (level >= 1) {
                sections.push({heading: '=== Interactive Elements ===', elements: interactiveElements});

                // Capture semantic headings to help the LLM structure the page logically
                sections.push({heading: '\\n=== Page Structure ===', elements:
                    captureElements('h1', 'heading')
                    .concat(captureElements('h2', 'heading'))
                    .concat(captureElements('h3', 'heading'))
                    .concat(captureElements('h4', 'heading'))
                    .concat(captureElements('h5', 'heading'))
                });
            }
            // LEVEL 2 (STANDARD Mode): Capture visible paragraph and plain text contents for full validation
            if (includesText) {
                sections.push({heading: '\\n=== Text Content (Validation Mode) ===', elements:
                    captureElements('p, span, li, td, div', 'text')
                    .filter(function(e) { return e.text.length > 0; })
                });
            }

            return {sections: sections, forms: level >= 1 ? captureForms() : []};
        })(arguments[0], arguments[1]);
        """;

    public PageAnalyzer()
    {
    }

    /**
     * Captures a Base64-encoded screenshot of the current page.
     *
     * @return Base64 PNG string
     * @throws IOException
     */

    public String captureScreenshot(String title) throws IOException
    {
        LOG.debug("   📸 Capturing screenshot for: {}", title);
        return ScreenshotWriter.doScreenshot(title.replaceAll("[^a-zA-Z0-9-]", "_").substring(0, Math.min(title.length(), 12)),
                                             ScreenshotWriter.getFormatedReportsPath(), false, false);
    }

    /**
     * Captures a simplified representation of the current page's DOM, focusing on interactive and meaningful elements.
     * Uses a single JavaScript execution for performance.
     *
     * @return simplified DOM as a structured text
     */
    public String captureSimplifiedDom()
    {
        return captureSimplifiedDom(ContextLevel.LEAN);
    }

    /**
     * Captures a simplified representation of the current page's DOM, focusing on interactive and meaningful elements.
     * Uses a single JavaScript execution for performance.
     *
     * @param forValidation
     *            whether to include more text content for validation
     * @return simplified DOM as a structured text
     * @deprecated Use {@link #captureSimplifiedDom(ContextLevel)} instead.
     */
    @Deprecated
    public String captureSimplifiedDom(final boolean forValidation)
    {
        return captureSimplifiedDom(forValidation ? ContextLevel.STANDARD : ContextLevel.LEAN);
    }

    /**
     * Captures a simplified representation of the current page's DOM at the
     * specified context level. Uses a single JavaScript execution for performance.
     *
     * @param level
     *            the context level controlling how much DOM data to capture
     * @return simplified DOM as a structured text
     */
    @SuppressWarnings("unchecked")
    public String captureSimplifiedDom(final ContextLevel level)
    {
        final String url = com.codeborne.selenide.WebDriverRunner.url();
        final boolean isEmptyPage = "data:,".equals(url) || "about:blank".equals(url);

        if (!isEmptyPage)
        {
            LOG.debug("🔴 Capturing simplified DOM for: {} (level: {})", url, level);
        }

        final StringBuilder dom = new StringBuilder();
        dom.append("Page URL: ").append(isEmptyPage ? "<empty page>" : url).append("\n");
        dom.append("Page Title: ").append(com.codeborne.selenide.Selenide.title()).append("\n\n");

        if (level == ContextLevel.AXTREE)
        {
            try
            {
                final String axTreeContent = captureAXTreeDOM();
                if (axTreeContent != null)
                {
                    dom.append(axTreeContent);
                    final String result = dom.toString();
                    if (!isEmptyPage)
                    {
                        LOG.debug("   📄 Simplified AXTree DOM size: {} chars", result.length());
                    }
                    return result;
                }
            }
            catch (final Exception e)
            {
                LOG.warn("Failed to capture AXTree via CDP, falling back to LEAN extraction: {}", e.getMessage());
            }
        }

        try
        {
            final Map<String, Object> data = (Map<String, Object>) com.codeborne.selenide.Selenide
                                                                                                  .executeJavaScript(CAPTURE_SCRIPT, level.ordinal(), level.includesTextContent());

            // Render element sections
            final List<Map<String, Object>> sections = (List<Map<String, Object>>) data.get("sections");

            for (final Map<String, Object> section : sections)
            {
                final List<Map<String, Object>> elements = (List<Map<String, Object>>) section.get("elements");

                if (elements != null && !elements.isEmpty())
                {
                    dom.append(section.get("heading")).append("\n");

                    for (final Map<String, Object> el : elements)
                    {
                        dom.append("  ");
                        formatElement(dom, el);
                    }
                }
            }

            // Render forms
            final List<Map<String, Object>> forms = (List<Map<String, Object>>) data.get("forms");

            if (forms != null && !forms.isEmpty())
            {
                dom.append("\n=== Forms ===\n");

                for (final Map<String, Object> form : forms)
                {
                    dom.append(String.format("  [form] id='%s' action='%s' data-neo-ref='%s'\n",
                                             form.get("id"), form.get("action"), form.get("automationId")));

                    final List<Map<String, Object>> fields = (List<Map<String, Object>>) form.get("fields");

                    if (fields != null)
                    {
                        for (final Map<String, Object> field : fields)
                        {
                            dom.append(String.format(
                                                 "    [form-field] type='%s' name='%s' id='%s' data-neo-ref='%s'",
                                                 field.get("type"), field.get("name"), field.get("id"), field.get("automationId")));

                        if (field.containsKey("options"))
                        {
                            dom.append(String.format(" options='[%s]'", field.get("options")));
                        }
                        dom.append("\n");
                        }
                    }
                }
            }
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to capture DOM via JavaScript, falling back to empty DOM: {}",
                     e.getMessage());
        }

        final String result = dom.toString();
        if (!isEmptyPage)
        {
            LOG.debug("   📄 Simplified DOM size: {} chars", result.length());
        }
        return result;
    }

    /**
     * Returns a compact page context combining URL, title, and key element info.
     * Uses {@link ContextLevel#LEAN} by default.
     */
    public String getPageContext()
    {
        return getPageContext(ContextLevel.LEAN);
    }

    /**
     * Returns a compact page context combining URL, title, and key element info.
     *
     * @param forValidation
     *            whether to include more text content for validation
     * @deprecated Use {@link #getPageContext(ContextLevel)} instead.
     */
    @Deprecated
    public String getPageContext(final boolean forValidation)
    {
        return captureSimplifiedDom(forValidation ? ContextLevel.STANDARD : ContextLevel.LEAN);
    }

    /**
     * Returns a compact page context at the specified context level.
     *
     * @param level
     *            the context level controlling how much DOM data to capture
     * @return simplified DOM as a structured text
     */
    public String getPageContext(final ContextLevel level)
    {
        return captureSimplifiedDom(level);
    }

    /**
     * Formats a single element map into the output string builder. Produces the same text format as the original
     * per-element approach.
     */
    private void formatElement(final StringBuilder dom, final Map<String, Object> el)
    {
        dom.append(String.format("[%s] ", el.get("label")));

        appendIfPresent(dom, "id", el.get("id"));
        appendIfPresent(dom, "name", el.get("name"));
        appendIfPresent(dom, "type", el.get("type"));

        final String text = (String) el.get("text");
        if (text != null && !text.isEmpty())
        {
            dom.append(String.format("text='%s' ", text));
        }

        appendIfPresent(dom, "parentText", el.get("parentText"));
        appendIfPresent(dom, "href", el.get("href"));
        appendIfPresent(dom, "placeholder", el.get("placeholder"));
        appendIfPresent(dom, "aria-label", el.get("ariaLabel"));
        appendIfPresent(dom, "pattern", el.get("pattern"));
        appendIfPresent(dom, "title", el.get("title"));
        appendIfPresent(dom, "min", el.get("min"));
        appendIfPresent(dom, "max", el.get("max"));
        appendIfPresent(dom, "minlength", el.get("minlength"));
        appendIfPresent(dom, "maxlength", el.get("maxlength"));
        appendIfPresent(dom, "step", el.get("step"));
        appendIfPresent(dom, "autocomplete", el.get("autocomplete"));
        appendIfPresent(dom, "required", el.get("required"));
        appendIfPresent(dom, "readonly", el.get("readonly"));
        appendIfPresent(dom, "disabled", el.get("disabled"));
        appendIfPresent(dom, "multiple", el.get("multiple"));
        appendIfPresent(dom, "value", el.get("value"));
        appendIfPresent(dom, "options", el.get("options"));

        appendIfPresent(dom, "data-neo-ref", el.get("automationId"));

        final Object selector = el.get("selector");
        if (selector != null && !selector.toString().isEmpty())
        {
            final String selStr = selector.toString();
            final Object id = el.get("id");
            final String idStr = id != null ? id.toString() : "";

            // Check if the selector is simply the ID selector (either raw or escaped) to prevent redundant printout
            final boolean isSimpleId = !idStr.isEmpty() &&
                                        (selStr.equals("#" + idStr) ||
                                         selStr.equals("#" + escapeCssIdentifier(idStr)));

            // Omit long, wishy-washy climbing selectors that contain child/descendant combinators,
            // since data-neo-ref is 100% unique and much more stable.
            final boolean isWishyWashy = selStr.contains(" > ");

            if (!isSimpleId && !isWishyWashy)
            {
                dom.append(String.format("selector='%s' ", selStr));
            }
        }

        dom.append("\n");
    }

    /**
     * Escapes special CSS characters in an identifier to match the CSS.escape specification.
     */
    private String escapeCssIdentifier(final String str)
    {
        return str.replaceAll("([!\"#$%&'()*+,./:;<=>?@\\[\\]^`{|}~])", "\\\\$1");
    }

    private void appendIfPresent(final StringBuilder dom, final String key, final Object value)
    {
        if (value != null && !value.toString().isEmpty())
        {
            dom.append(String.format("%s='%s' ", key, value));
        }
    }

    @SuppressWarnings("unchecked")
    private String captureAXTreeDOM()
    {
        final WebDriver driver = com.codeborne.selenide.WebDriverRunner.getWebDriver();
        if (!(driver instanceof final HasCdp cdpDriver))
        {
            LOG.debug("Driver does not support CDP, AXTree is unavailable. Falling back to LEAN.");
            return null;
        }

        final Map<String, Object> axTree = cdpDriver.executeCdpCommand("Accessibility.getFullAXTree", Map.of());
        if (axTree == null || !axTree.containsKey("nodes"))
        {
            return null;
        }

        final List<Map<String, Object>> nodes = (List<Map<String, Object>>) axTree.get("nodes");
        if (nodes == null || nodes.isEmpty())
        {
            return null;
        }

        final Set<String> interactiveRoles = Set.of(
            "button", "link", "checkbox", "radio", "combobox", "listbox", "searchbox",
            "textbox", "slider", "spinbutton", "switch", "tab", "menuitem",
            "menuitemcheckbox", "menuitemradio", "input", "textarea", "select",
            "option", "treeitem", "tabpanel", "dialog", "menu"
        );

        final Set<String> landmarkRoles = Set.of(
            "heading", "form", "main", "navigation", "banner", "contentinfo", "alert", "status"
        );

        final StringBuilder dom = new StringBuilder();
        dom.append("=== Accessibility Tree (AXTree) ===\n");

        for (final Map<String, Object> node : nodes)
        {
            final boolean ignored = Boolean.TRUE.equals(node.get("ignored"));
            if (ignored)
            {
                continue;
            }

            final Object backendDOMNodeIdObj = node.get("backendDOMNodeId");
            if (backendDOMNodeIdObj == null)
            {
                continue;
            }
            final int backendDOMNodeId = ((Number) backendDOMNodeIdObj).intValue();

            // Parse role
            final Object roleObj = node.get("role");
            String role = "";
            if (roleObj instanceof final Map<?, ?> roleMap)
            {
                role = String.valueOf(roleMap.get("value"));
            }
            else if (roleObj != null)
            {
                role = String.valueOf(roleObj);
            }

            // Filter roles
            if (!interactiveRoles.contains(role) && !landmarkRoles.contains(role))
            {
                continue;
            }

            // Parse name
            final Object nameObj = node.get("name");
            String name = "";
            if (nameObj instanceof final Map<?, ?> nameMap)
            {
                name = String.valueOf(nameMap.get("value"));
            }
            else if (nameObj != null)
            {
                name = String.valueOf(nameObj);
            }
            name = cleanAccessibleText(name);

            // Parse value
            final Object valueObj = node.get("value");
            String value = "";
            if (valueObj instanceof final Map<?, ?> valueMap)
            {
                value = String.valueOf(valueMap.get("value"));
            }
            else if (valueObj != null)
            {
                value = String.valueOf(valueObj);
            }
            value = cleanAccessibleText(value);

            // Parse properties
            final List<Map<String, Object>> properties = (List<Map<String, Object>>) node.get("properties");
            boolean disabled = false;
            boolean required = false;
            boolean readonly = false;
            boolean checked = false;
            String autocomplete = "";
            String placeholder = "";

            if (properties != null)
            {
                for (final Map<String, Object> prop : properties)
                {
                    final String propName = String.valueOf(prop.get("name"));
                    final Object propValObj = prop.get("value");
                    String propValue = "";
                    if (propValObj instanceof final Map<?, ?> propValMap)
                    {
                        propValue = String.valueOf(propValMap.get("value"));
                    }
                    else if (propValObj != null)
                    {
                        propValue = String.valueOf(propValObj);
                    }

                    if ("disabled".equals(propName))
                    {
                        disabled = Boolean.parseBoolean(propValue);
                    }
                    else if ("required".equals(propName))
                    {
                        required = Boolean.parseBoolean(propValue);
                    }
                    else if ("readonly".equals(propName))
                    {
                        readonly = Boolean.parseBoolean(propValue);
                    }
                    else if ("checked".equals(propName))
                    {
                        checked = "true".equals(propValue) || "mixed".equals(propValue);
                    }
                    else if ("autocomplete".equals(propName))
                    {
                        autocomplete = propValue;
                    }
                    else if ("placeholder".equals(propName))
                    {
                        placeholder = propValue;
                    }
                }
            }

            // Stamping data-neo-ref via Runtime.callFunctionOn
            String refId = "";
            try
            {
                final Map<String, Object> resolveParams = Map.of("backendNodeId", backendDOMNodeId);
                final Map<String, Object> resolvedNode = cdpDriver.executeCdpCommand("DOM.resolveNode", resolveParams);
                if (resolvedNode != null && resolvedNode.containsKey("object"))
                {
                    final Map<String, Object> objectInfo = (Map<String, Object>) resolvedNode.get("object");
                    final String objectId = (String) objectInfo.get("objectId");
                    if (objectId != null)
                    {
                        final String functionDeclaration = """
                            function() {
                                function getFallbackLabel(el) {
                                    var iconEl = el.querySelector('[class*="bi-"], [class*="fa-"], [class*="icon-"]');
                                    var elClassStr = typeof el.className === 'string' ? el.className : (el.getAttribute && el.getAttribute('class') || '');
                                    if (!iconEl && (elClassStr.includes('bi-') || elClassStr.includes('fa-') || elClassStr.includes('icon-'))) {
                                        iconEl = el;
                                    }
                                    if (iconEl) {
                                        var iconClassStr = typeof iconEl.className === 'string' ? iconEl.className : (iconEl.getAttribute && iconEl.getAttribute('class') || '');
                                        var classes = iconClassStr.split(/\\s+/);
                                        for (var i = 0; i < classes.length; i++) {
                                            var cls = classes[i];
                                            var match = cls.match(/^(?:bi|fa|icon)-([a-z0-9-]+)$/);
                                            if (match && match[1]) {
                                                var iconName = match[1];
                                                iconName = iconName.replace(/\\d+$/, '');
                                                iconName = iconName.replace(/-(?:fill|outline|short|large|small)$/, '');
                                                iconName = iconName.replace(/-/g, ' ');
                                                return iconName.replace(/\\b\\w/g, function(l) { return l.toUpperCase(); }) + ' Icon';
                                            }
                                        }
                                    }
                                    var fallback = el.getAttribute('aria-label') || el.getAttribute('title') || el.getAttribute('placeholder') || el.getAttribute('alt') || el.getAttribute('name') || el.id || '';
                                    if (fallback) {
                                        fallback = fallback.replace(/[-_]/g, ' ').trim();
                                        return fallback.replace(/\\b\\w/g, function(l) { return l.toUpperCase(); });
                                    }
                                    return '';
                                }
                                if (this.hasAttribute('data-neo-ref')) {
                                    return {
                                        refId: this.getAttribute('data-neo-ref'),
                                        fallbackLabel: getFallbackLabel(this)
                                    };
                                }
                                var usedIds = {};
                                document.querySelectorAll('[data-neo-ref]').forEach(function(el) {
                                    usedIds[el.getAttribute('data-neo-ref')] = true;
                                });
                                
                                function djb2(str) {
                                    var hash = 5381;
                                    for (var i = 0; i < str.length; i++) {
                                        hash = ((hash << 5) + hash) + str.charCodeAt(i);
                                        hash = hash & 0x7fffffff;
                                    }
                                    return hash.toString(36);
                                }
                                
                                var tag = this.tagName.toLowerCase();
                                var id = this.id || '';
                                var cls = (typeof this.className === 'string' ? this.className : '').trim().replace(/\\s+/g, ' ');
                                var ptag = this.parentElement ? this.parentElement.tagName.toLowerCase() : '';
                                var type = this.getAttribute('type') || '';
                                var name = this.getAttribute('name') || this.getAttribute('alt') || '';
                                var raw = [tag, id, cls, ptag, type, name].join('|');
                                var base = 'xc_' + djb2(raw);
                                
                                var candidate = base;
                                var suffix = 0;
                                while (usedIds[candidate]) {
                                    suffix++;
                                    candidate = base + '_' + suffix;
                                }
                                this.setAttribute('data-neo-ref', candidate);
                                return {
                                    refId: candidate,
                                    fallbackLabel: getFallbackLabel(this)
                                };
                            }
                            """;

                        final Map<String, Object> callParams = Map.of(
                            "objectId", objectId,
                            "functionDeclaration", functionDeclaration,
                            "returnByValue", true
                        );

                        final Map<String, Object> callResult = cdpDriver.executeCdpCommand("Runtime.callFunctionOn", callParams);
                        if (callResult != null && callResult.containsKey("result"))
                        {
                            final Map<String, Object> resultVal = (Map<String, Object>) callResult.get("result");
                            if (resultVal != null && resultVal.containsKey("value"))
                            {
                                final Object stampedValueObj = resultVal.get("value");
                                if (stampedValueObj instanceof final Map<?, ?> resultMap)
                                {
                                    refId = String.valueOf(resultMap.get("refId"));
                                    final String fallbackLabel = String.valueOf(resultMap.get("fallbackLabel"));
                                    if (fallbackLabel != null && !fallbackLabel.isEmpty() && !"null".equals(fallbackLabel))
                                    {
                                        final String cleanName = name == null ? "" : name.replace('\u00a0', ' ').strip().replaceAll("\\s+", " ");
                                        if (cleanName.isEmpty())
                                        {
                                            name = fallbackLabel;
                                        }
                                        else if (cleanName.matches("^\\d+$"))
                                        {
                                            name = fallbackLabel + ": " + cleanName;
                                        }
                                    }
                                }
                                else if (stampedValueObj != null)
                                {
                                    refId = String.valueOf(stampedValueObj);
                                }
                            }
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                LOG.debug("Failed to resolve or stamp node with backendNodeId {}: {}", backendDOMNodeId, e.getMessage());
            }

            name = cleanAccessibleText(name);
            value = cleanAccessibleText(value);

            final StringBuilder nodeText = new StringBuilder();
            nodeText.append("  [").append(role).append("] ");
            if (!refId.isEmpty())
            {
                nodeText.append("data-neo-ref='").append(refId).append("' ");
            }
            if (!name.isEmpty())
            {
                nodeText.append("name='").append(name.replaceAll("'", "\\\\'")).append("' ");
            }
            if (!value.isEmpty())
            {
                nodeText.append("value='").append(value.replaceAll("'", "\\\\'")).append("' ");
            }
            if (disabled)
            {
                nodeText.append("disabled='true' ");
            }
            if (checked)
            {
                nodeText.append("checked='true' ");
            }
            if (required)
            {
                nodeText.append("required='true' ");
            }
            if (readonly)
            {
                nodeText.append("readonly='true' ");
            }
            if (!placeholder.isEmpty())
            {
                nodeText.append("placeholder='").append(placeholder.replaceAll("'", "\\\\'")).append("' ");
            }
            if (!autocomplete.isEmpty())
            {
                nodeText.append("autocomplete='").append(autocomplete).append("' ");
            }

            dom.append(nodeText.toString().trim()).append("\n");
        }

        return dom.toString();
    }

    private static String cleanAccessibleText(final String text)
    {
        if (text == null)
        {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        text.codePoints().forEach((final int cp) -> {
            final int type = Character.getType(cp);
            if (type != Character.PRIVATE_USE && type != Character.FORMAT && type != Character.CONTROL)
            {
                sb.appendCodePoint(cp);
            }
        });

        return sb.toString().replace('\u00a0', ' ').strip().replaceAll("\\s+", " ");
    }
}
