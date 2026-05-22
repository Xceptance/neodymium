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
import java.util.List;
import java.util.Map;

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
        return (function(level) {
            var MAX_TEXT = 200;
            var MAX_HREF = 180;
            var MAX_VALUE = 150;
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
                allRoots[i].querySelectorAll('[data-neo-ref]').forEach(function(el) {
                    var ref = el.getAttribute('data-neo-ref');
                    if (seenRefs[ref]) {
                        console.log("REMOVED DUPLICATE NEO REF for ");
                        console.log(el);
                        el.removeAttribute('data-neo-ref');
                    } else {
                        seenRefs[ref] = true;
                        usedIds[ref] = true;
                    }
                });
            }

            // djb2 hash – very low computation cost, good distribution
            function djb2(str) {
                var hash = 5381;
                for (var i = 0; i < str.length; i++) {
                    hash = ((hash << 5) + hash) + str.charCodeAt(i);
                    hash = hash & 0x7fffffff; // keep positive 31-bit int
                }
                return hash.toString(36); // compact alphanumeric
            }

            function fingerprint(el) {
                var tag   = el.tagName ? el.tagName.toLowerCase() : '';
                var id    = el.id || '';
                var cls   = (typeof el.className === 'string' ? el.className : '').trim().replace(/\\s+/g, ' ');
                var ptag  = el.parentElement && el.parentElement.tagName ? el.parentElement.tagName.toLowerCase() : '';
                var type  = el.getAttribute('type') || '';
                var name  = el.getAttribute('name') || el.getAttribute('alt') || '';
                var text  = (el.innerText || el.value || '').trim().substring(0, 40);
                var raw   = [tag, id, cls, ptag, type, name, text].join('|');
                return 'xc_' + djb2(raw);
            }

            function assignId(el) {
                if (el.hasAttribute('data-neo-ref')) {
                    return el.getAttribute('data-neo-ref');
                }
                var base = fingerprint(el);
                var candidate = base;
                var suffix = 0;
                while (usedIds[candidate]) {
                    suffix++;
                    candidate = base + '_' + suffix;
                }
                usedIds[candidate] = true;
                el.setAttribute('data-neo-ref', candidate);
                return candidate;
            }

            function isVisible(el) {
                if (!el.isConnected) return false;
                if (el.closest && el.closest('.neodymium-ai-hud')) return false;

                const style = window.getComputedStyle(el);
                if (style.display === 'none' || style.visibility === 'hidden') return false;

                const rect = el.getBoundingClientRect();
                return rect.width > 0 && rect.height > 0;
            }

            function truncate(s, max) {
                if (!s) return s;
                return s.length <= max ? s : s.substring(0, max) + '…';
            }

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

            function generateSelector(el) {
                var id = el.id;
                if (id) return '#' + id;
                var name = el.getAttribute('name');
                var tag = el.tagName ? el.tagName.toLowerCase() : '';
                if (name) return tag + "[name='" + name + "']";
                var cls = el.className;
                if (typeof cls === 'string' && cls.trim()) {
                    var parts = cls.trim().split(/\s+/);
                    if (parts.length <= 3) return tag + '.' + parts.join('.');
                }
                return '';
            }

            function captureElements(cssSelector, label) {
                var results = [];
                try {
                    var els = queryAllDeep(cssSelector);
                    for (var i = 0; i < els.length; i++) {
                        var el = els[i];
                        if (!isVisible(el)) continue;

                        var autoId = assignId(el);
                        var text = (el.innerText || '').trim().replace(/\\s*\\n\\s*/g, ' ');
                        var options = null;
                        if (el.tagName && el.tagName.toLowerCase() === 'select') {
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

            function captureClickableElements(cssSelector, label) {
                var results = [];
                try {
                    var els = queryAllDeep(cssSelector);
                    for (var i = 0; i < els.length; i++) {
                        var el = els[i];
                        if (!isVisible(el)) continue;
                        if (el.closest && el.closest('a')) continue;

                        var style = window.getComputedStyle(el);
                        var isClickable = style.cursor === 'pointer' || el.hasAttribute('onclick') || typeof el.onclick === 'function';
                        if (!isClickable) continue;

                        var autoId = assignId(el);
                        var text = (el.innerText || '').trim().replace(/\\s*\\n\\s*/g, ' ');
                        var options = null;
                        if (el.tagName && el.tagName.toLowerCase() === 'select') {
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

            // Interactive elements
            var interactiveElements = captureElements('a', 'link')
                .concat(captureElements('button', 'button'))
                .concat(captureElements('input', 'input'))
                .concat(captureElements('select', 'select'))
                .concat(captureElements('option', 'option'))
                .concat(captureElements('textarea', 'textarea'))
                .concat(captureClickableElements('div, span, tr, td, th, li, label, dialog, svg, img', 'clickable'));

            var getDisplayLabel = function(elObj) {
                return elObj.text || elObj.placeholder || elObj.value || elObj.ariaLabel || elObj.title || elObj.name || '';
            };

            var textCounts = {};
            for (var i = 0; i < interactiveElements.length; i++) {
                var lbl = getDisplayLabel(interactiveElements[i]);
                if (lbl) {
                    textCounts[lbl] = (textCounts[lbl] || 0) + 1;
                }
            }

            for (var i = 0; i < interactiveElements.length; i++) {
                var elObj = interactiveElements[i];
                var lbl = getDisplayLabel(elObj);
                if (lbl && textCounts[lbl] > 1 && elObj.domElement) {
                    var parentText = '';
                    var p = elObj.domElement.parentElement;
                    while (p && p !== document.body) {
                        var pText = (p.innerText || '').trim();
                        if (pText.length > lbl.length && pText.length < 300) {
                            parentText = pText;
                        }
                        if (pText.length >= 300) {
                            break;
                        }
                        p = p.parentElement;
                    }
                    if (parentText) {
                        elObj.parentText = truncate(parentText.replace(/\\s*\\n\\s*/g, ' | '), MAX_TEXT);
                    }
                }
                delete elObj.domElement;
            }

            if (level >= 1) {
                sections.push({heading: '=== Interactive Elements ===', elements: interactiveElements});

                // Page structure
                sections.push({heading: '\\n=== Page Structure ===', elements:
                    captureElements('h1', 'heading')
                    .concat(captureElements('h2', 'heading'))
                    .concat(captureElements('h3', 'heading'))
                    .concat(captureElements('h4', 'heading'))
                    .concat(captureElements('h5', 'heading'))
                });
            }

            if (level >= 2) {
                sections.push({heading: '\\n=== Text Content (Validation Mode) ===', elements:
                    captureElements('p, span, li, td, div', 'text')
                    .filter(function(e) { return e.text.length > 0; })
                });
            }

            return {sections: sections, forms: level >= 1 ? captureForms() : []};
        })(arguments[0]);
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

        try
        {
            final Map<String, Object> data = (Map<String, Object>) com.codeborne.selenide.Selenide
                                                                                                  .executeJavaScript(CAPTURE_SCRIPT, level.ordinal());

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
        appendIfPresent(dom, "selector", el.get("selector"));
        dom.append("\n");
    }

    private void appendIfPresent(final StringBuilder dom, final String key, final Object value)
    {
        if (value != null && !value.toString().isEmpty())
        {
            dom.append(String.format("%s='%s' ", key, value));
        }
    }
}
