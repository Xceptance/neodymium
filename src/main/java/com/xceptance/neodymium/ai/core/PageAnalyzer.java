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
        return (function(forValidation) {
            var MAX_PER_SELECTOR = 150;
            var MAX_TEXT = 200;
            var MAX_HREF = 180;
            var MAX_VALUE = 150;
            var usedIds = {};
            // Pre-populate registry from IDs already stamped on this page
            // (handles repeated script injections on the same page)
            document.querySelectorAll('[data-neodymium-automation-id]').forEach(function(el) {
                usedIds[el.getAttribute('data-neodymium-automation-id')] = true;
            });

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
                var tag   = el.tagName.toLowerCase();
                var id    = el.id || '';
                var cls   = (typeof el.className === 'string' ? el.className : '').trim().replace(/\\s+/g, ' ');
                var ptag  = el.parentElement ? el.parentElement.tagName.toLowerCase() : '';
                var type  = el.getAttribute('type') || '';
                var name  = el.getAttribute('name') || el.getAttribute('alt') || '';
                var text  = (el.innerText || el.value || '').trim().substring(0, 40);
                var raw   = [tag, id, cls, ptag, type, name, text].join('|');
                return 'xc_' + djb2(raw);
            }

            function assignId(el) {
                if (el.hasAttribute('data-neodymium-automation-id')) {
                    return el.getAttribute('data-neodymium-automation-id');
                }
                var base = fingerprint(el);
                var candidate = base;
                var suffix = 0;
                while (usedIds[candidate]) {
                    suffix++;
                    candidate = base + '_' + suffix;
                }
                usedIds[candidate] = true;
                el.setAttribute('data-neodymium-automation-id', candidate);
                return candidate;
            }

            function isVisible(el) {
                if (!el.isConnected) return false;

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

            function generateSelector(el) {
                var id = el.id;
                if (id) return '#' + id;
                var name = el.getAttribute('name');
                var tag = el.tagName.toLowerCase();
                if (name) return tag + "[name='" + name + "']";
                var cls = el.className;
                if (typeof cls === 'string' && cls.trim()) {
                    var parts = cls.trim().split(/\\s+/);
                    if (parts.length <= 3) return tag + '.' + parts.join('.');
                }
                return tag;
            }

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
                        var text = (el.innerText || '').trim();
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
                            selector: generateSelector(el),
                            automationId: autoId
                        });
                    }
                } catch(e) { /* skip selector errors */ }
                return results;
            }

            function captureClickableElements(cssSelector, label) {
                var results = [];
                try {
                    var els = queryAllDeep(cssSelector);
                    var count = 0;
                    for (var i = 0; i < els.length && count < MAX_PER_SELECTOR; i++) {
                        var el = els[i];
                        if (!isVisible(el)) continue;
                        if (el.closest('a')) continue;

                        var style = window.getComputedStyle(el);
                        var isClickable = style.cursor === 'pointer' || el.hasAttribute('onclick') || typeof el.onclick === 'function';
                        if (!isClickable) continue;

                        count++;
                        var autoId = assignId(el);
                        var text = (el.innerText || '').trim();
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
                            selector: generateSelector(el),
                            automationId: autoId
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
                        var formId = assignId(form);

                        var fields = [];
                        var inputs = queryAllDeep('input, select, textarea', form);
                        for (var j = 0; j < inputs.length; j++) {
                            var inp = inputs[j];
                            if (!isVisible(inp)) continue;
                            var autoId = assignId(inp);

                            fields.push({
                                type: inp.getAttribute('type') || '',
                                name: inp.getAttribute('name') || '',
                                id: inp.id || '',
                                automationId: autoId
                            });
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
            sections.push({heading: '=== Interactive Elements ===', elements:
                captureElements('a', 'link')
                .concat(captureElements('button', 'button'))
                .concat(captureElements('input', 'input'))
                .concat(captureElements('select', 'select'))
                .concat(captureElements('option', 'option'))
                .concat(captureElements('textarea', 'textarea'))
                .concat(captureClickableElements('div, span, tr, td, th', 'clickable'))
            });

            // Page structure
            sections.push({heading: '\\n=== Page Structure ===', elements:
                captureElements('h1', 'heading')
                .concat(captureElements('h2', 'heading'))
                .concat(captureElements('h3', 'heading'))
                .concat(captureElements('h4', 'heading'))
                .concat(captureElements('h5', 'heading'))
            });

            if (forValidation) {
                sections.push({heading: '\\n=== Text Content (Validation Mode) ===', elements:
                    captureElements('p, span, li, td, div', 'text')
                    .filter(function(e) { return e.text.length > 0; })
                });
            }

            return {sections: sections, forms: captureForms()};
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
        LOG.debug("Capturing screenshot for: {}", title);
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
        return captureSimplifiedDom(false);
    }

    /**
     * Captures a simplified representation of the current page's DOM, focusing on interactive and meaningful elements.
     * Uses a single JavaScript execution for performance.
     *
     * @param forValidation
     *            whether to include more text content for validation
     * @return simplified DOM as a structured text
     */
    @SuppressWarnings("unchecked")
    public String captureSimplifiedDom(boolean forValidation)
    {
        LOG.debug("Capturing simplified DOM for: {} (validation: {})", com.codeborne.selenide.WebDriverRunner.url(),
                  forValidation);

        final StringBuilder dom = new StringBuilder();
        dom.append("Page URL: ").append(com.codeborne.selenide.WebDriverRunner.url()).append("\n");
        dom.append("Page Title: ").append(com.codeborne.selenide.Selenide.title()).append("\n\n");

        try
        {
            final Map<String, Object> data = (Map<String, Object>) com.codeborne.selenide.Selenide
                                                                                                  .executeJavaScript(CAPTURE_SCRIPT, forValidation);

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
                    dom.append(String.format("[form] id='%s' action='%s' data-neodymium-automation-id='%s'\n",
                                             form.get("id"), form.get("action"), form.get("automationId")));

                    final List<Map<String, Object>> fields = (List<Map<String, Object>>) form.get("fields");

                    if (fields != null)
                    {
                        for (final Map<String, Object> field : fields)
                        {
                            dom.append(String.format(
                                                     "  [form-field] type='%s' name='%s' id='%s' data-neodymium-automation-id='%s' \n",
                                                     field.get("type"), field.get("name"), field.get("id"), field.get("automationId")));
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
        LOG.debug("Simplified DOM size: {} chars", result.length());
        return result;
    }

    /**
     * Returns a compact page context combining URL, title, and key element info.
     */
    public String getPageContext()
    {
        return getPageContext(false);
    }

    /**
     * Returns a compact page context combining URL, title, and key element info.
     * 
     * @param forValidation
     *            whether to include more text content for validation
     */
    public String getPageContext(boolean forValidation)
    {
        return captureSimplifiedDom(forValidation);
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

        appendIfPresent(dom, "data-neodymium-automation-id", el.get("automationId"));
        dom.append(String.format("selector='%s' ", el.get("selector")));
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
