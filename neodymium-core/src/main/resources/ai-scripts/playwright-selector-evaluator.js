/**
 * Playwright Selector Evaluator for Neodymium
 * Executes Playwright-style selectors, CSS extensions, text, role, and layout matchers
 * while transparently searching across open Shadow DOM boundaries in document order.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
return (function(contextRoot, rawSelector) {
    'use strict';

    if (!rawSelector || typeof rawSelector !== 'string') {
        return [];
    }

    var root = contextRoot && contextRoot.nodeType ? contextRoot : document;

    // Helper: Normalize whitespace and non-breaking spaces (NBSP -> space)
    function normalizeText(str) {
        if (!str) return '';
        return str.replace(/[\u00a0\s]+/g, ' ').trim();
    }

    // Helper: Collect all open shadow roots recursively starting from a root node
    function collectAllRoots(start) {
        var roots = [start];
        function walk(node) {
            if (!node || !node.querySelectorAll) return;
            var els = node.querySelectorAll('*');
            for (var i = 0; i < els.length; i++) {
                var el = els[i];
                if (el.shadowRoot) {
                    roots.push(el.shadowRoot);
                    walk(el.shadowRoot);
                }
            }
        }
        walk(start);
        return roots;
    }

    // Helper: Determine if element is visible
    function isElementVisible(el) {
        if (!el || !el.isConnected) return false;
        var tag = (el.tagName || '').toLowerCase();
        if (tag === 'body' || tag === 'html') return true;
        try {
            var style = window.getComputedStyle(el);
            if (style.display === 'none' || style.visibility === 'hidden') return false;
            var rect = el.getBoundingClientRect();
            if (rect.width > 0 && rect.height > 0) return true;
            if (['input', 'button', 'a', 'select', 'textarea'].indexOf(tag) !== -1) return true;
        } catch (e) {}
        return false;
    }

    // Helper: Split selector clauses by >> outside quotes, brackets, parens, and regex
    function splitClauses(selector) {
        var clauses = [];
        var start = 0;
        var inQuote = null;
        var bracketDepth = 0;
        var parenDepth = 0;
        var inRegex = false;
        var len = selector.length;

        for (var i = 0; i < len; i++) {
            var c = selector.charAt(i);
            if (c === '\\' && i + 1 < len) {
                i++;
                continue;
            }
            if (inQuote) {
                if (c === inQuote) inQuote = null;
                continue;
            }
            if (c === '"' || c === "'") {
                inQuote = c;
                continue;
            }
            if (inRegex) {
                if (c === '/') {
                    while (i + 1 < len && /[a-zA-Z]/.test(selector.charAt(i + 1))) i++;
                    inRegex = false;
                }
                continue;
            }
            if (c === '/' && bracketDepth === 0 && parenDepth === 0) {
                var prev = selector.substring(0, i).trim();
                var lastChar = prev.charAt(prev.length - 1);
                if (prev.length === 0 || lastChar === '=' || lastChar === '(' || lastChar === ',') {
                    inRegex = true;
                    continue;
                }
            }
            if (c === '[') { bracketDepth++; continue; }
            if (c === ']') { if (bracketDepth > 0) bracketDepth--; continue; }
            if (c === '(') { parenDepth++; continue; }
            if (c === ')') { if (parenDepth > 0) parenDepth--; continue; }

            if (bracketDepth === 0 && parenDepth === 0 && c === '>' && i + 1 < len && selector.charAt(i + 1) === '>') {
                var clause = selector.substring(start, i).trim();
                if (clause.length > 0) clauses.push(clause);
                i++;
                start = i + 1;
            }
        }
        var last = selector.substring(start).trim();
        if (last.length > 0) clauses.push(last);
        return clauses;
    }

    // Helper: Unquote a string literal
    function unquote(s) {
        if (!s) return '';
        var t = s.trim();
        if ((t.startsWith('"') && t.endsWith('"')) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length - 1).replace(/\\(["'])/g, '$1');
        }
        return t;
    }

    // Helper: Parse text matcher into a matcher function
    function createTextMatcher(rawVal, forceSubstring) {
        var v = rawVal.trim();
        if (v.startsWith('/') && v.lastIndexOf('/') > 0) {
            var lastSlash = v.lastIndexOf('/');
            var pattern = v.substring(1, lastSlash);
            var flags = v.substring(lastSlash + 1);
            var re = new RegExp(pattern, flags);
            return function(text) { return re.test(text); };
        }
        var isQuoted = (v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"));
        var content = unquote(v);
        if (isQuoted && !forceSubstring) {
            return function(text) { return normalizeText(text) === content; };
        }
        var lower = content.toLowerCase();
        return function(text) { return normalizeText(text).toLowerCase().indexOf(lower) !== -1; };
    }

    // Helper: Get element text including submit inputs (excluding non-renderable tags like script/style)
    function getElementText(el) {
        if (!el || el.nodeType !== 1) return '';
        var tag = (el.tagName || '').toLowerCase();
        if (tag === 'script' || tag === 'style' || tag === 'noscript') return '';
        if (tag === 'input' && (el.type === 'submit' || el.type === 'button' || el.type === 'reset')) {
            return el.value || '';
        }
        return el.textContent || '';
    }

    // Helper: Compute W3C Accessible Name and Role
    function computeAriaRole(el) {
        if (!el || el.nodeType !== 1) return '';
        var explicit = el.getAttribute('role');
        if (explicit) return explicit.trim().toLowerCase();
        var tag = (el.tagName || '').toLowerCase();
        if (tag === 'button') return 'button';
        if (tag === 'a' && el.hasAttribute('href')) return 'link';
        if (tag === 'input') {
            var type = (el.type || 'text').toLowerCase();
            if (type === 'button' || type === 'submit' || type === 'reset') return 'button';
            if (type === 'checkbox') return 'checkbox';
            if (type === 'radio') return 'radio';
            if (type === 'number') return 'spinbutton';
            return 'textbox';
        }
        if (tag === 'textarea') return 'textbox';
        if (tag === 'select') return 'combobox';
        if (tag === 'img') return 'img';
        if (/^h[1-6]$/.test(tag)) return 'heading';
        if (tag === 'form') return 'form';
        if (tag === 'table') return 'table';
        if (tag === 'dialog') return 'dialog';
        return '';
    }

    function computeAccessibleName(el) {
        if (!el || el.nodeType !== 1) return '';
        var ariaLabel = el.getAttribute('aria-label');
        if (ariaLabel) return normalizeText(ariaLabel);
        var ariaLabelledBy = el.getAttribute('aria-labelledby');
        if (ariaLabelledBy) {
            var ids = ariaLabelledBy.split(/\s+/);
            var texts = [];
            for (var i = 0; i < ids.length; i++) {
                var ref = document.getElementById(ids[i]);
                if (ref) texts.push(ref.textContent || '');
            }
            if (texts.length > 0) return normalizeText(texts.join(' '));
        }
        var tag = (el.tagName || '').toLowerCase();
        if (tag === 'input') {
            if (el.type === 'submit' || el.type === 'button' || el.type === 'reset') {
                return normalizeText(el.value || '');
            }
            if (el.placeholder) return normalizeText(el.placeholder);
        }
        if (tag === 'img' && el.alt) return normalizeText(el.alt);
        if (['button', 'a', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'].indexOf(tag) !== -1) {
            return normalizeText(el.textContent || '');
        }
        if (el.title) return normalizeText(el.title);
        return normalizeText(el.textContent || '');
    }

    // Helper: Query CSS inside roots with Playwright pseudo-classes support
    function queryCssSingleRoot(rootNode, cssClause) {
        // Handle Neodymium automation IDs: #xc... -> [data-ai="xc..."]
        var transformedCss = cssClause.replace(/#xc([a-zA-Z0-9_\-]+)/g, '[data-ai="xc$1"]');

        // Extract Playwright custom pseudos if present: :has-text, :text, :contains, :visible, :has, etc.
        var hasTextMatch = transformedCss.match(/:has-text\((["']?)(.*?)\1\)/i) || transformedCss.match(/:contains\((["']?)(.*?)\1\)/i);
        var exactTextMatch = transformedCss.match(/:text-is\((["']?)(.*?)\1\)/i) || transformedCss.match(/:exact-text\((["']?)(.*?)\1\)/i) || transformedCss.match(/:has-text-is\((["']?)(.*?)\1\)/i);
        var textMatch = transformedCss.match(/:text\((["']?)(.*?)\1\)/i);
        var visibleMatch = /:visible\b/i.test(transformedCss);

        // Extract layout and complex pseudos
        var hasMatch = transformedCss.match(/:has\((.*?)\)/i);
        var nthMatch = transformedCss.match(/:nth-match\((.*?),\s*(\d+)\)/i);
        var layoutMatch = transformedCss.match(/:(left-of|right-of|above|below|near)\((.*?)\)/i);

        // Strip known Playwright pseudos for the native querySelectorAll call
        var nativeCss = transformedCss
            .replace(/:has-text-is\((["']?).*?\1\)/gi, '')
            .replace(/:has-text\((["']?).*?\1\)/gi, '')
            .replace(/:contains\((["']?).*?\1\)/gi, '')
            .replace(/:text-is\((["']?).*?\1\)/gi, '')
            .replace(/:exact-text\((["']?).*?\1\)/gi, '')
            .replace(/:text\((["']?).*?\1\)/gi, '')
            .replace(/:visible\b/gi, '')
            .replace(/:has\((.*?)\)/gi, '')
            .replace(/:nth-match\((.*?)\)/gi, '')
            .replace(/:(?:left-of|right-of|above|below|near)\((.*?)\)/gi, '')
            .trim();

        if (!nativeCss || nativeCss === '') nativeCss = '*';

        var elements = [];
        try {
            var rawList = rootNode.querySelectorAll(nativeCss);
            for (var i = 0; i < rawList.length; i++) {
                elements.push(rawList[i]);
            }
        } catch (e) {
            return [];
        }

        // Filter by :has-text / :contains (case-insensitive substring match)
        if (hasTextMatch) {
            var hasTextArg = hasTextMatch[1] ? (hasTextMatch[1] + hasTextMatch[2] + hasTextMatch[1]) : hasTextMatch[2];
            var matcher = createTextMatcher(hasTextArg, true);
            elements = elements.filter(function(el) {
                return matcher(getElementText(el));
            });
        }

        // Filter by :text-is / :exact-text / :has-text-is (strict exact text match)
        if (exactTextMatch) {
            var exactTarget = unquote(exactTextMatch[2]);
            var exactMatcher = function(text) {
                return normalizeText(text) === exactTarget;
            };
            elements = elements.filter(function(el) {
                if (!exactMatcher(getElementText(el))) return false;
                // If selector is scoped to specific elements (not bare '*'), keep matched element
                if (nativeCss !== '*') return true;
                // For bare '*' selectors, ensure innermost smallest element is selected
                var children = el.querySelectorAll('*');
                for (var ci = 0; ci < children.length; ci++) {
                    if (exactMatcher(getElementText(children[ci]))) return false;
                }
                return true;
            });
        }

        // Filter by :text (quoted = exact, unquoted = substring)
        if (textMatch) {
            var rawTextArg = textMatch[1] ? (textMatch[1] + textMatch[2] + textMatch[1]) : textMatch[2];
            var generalMatcher = createTextMatcher(rawTextArg);
            elements = elements.filter(function(el) {
                if (!generalMatcher(getElementText(el))) return false;
                if (nativeCss !== '*') return true;
                var children = el.querySelectorAll('*');
                for (var ci = 0; ci < children.length; ci++) {
                    if (generalMatcher(getElementText(children[ci]))) return false;
                }
                return true;
            });
        }

        // Filter by :has(...)
        if (hasMatch) {
            var subSel = hasMatch[1].trim();
            elements = elements.filter(function(el) {
                return queryCssSingleRoot(el, subSel).length > 0;
            });
        }

        // Filter by :nth-match(subSelector, n)
        if (nthMatch) {
            var targetIdx = parseInt(nthMatch[2], 10) - 1;
            elements = targetIdx >= 0 && targetIdx < elements.length ? [elements[targetIdx]] : [];
        }

        // Filter by :visible
        if (visibleMatch) {
            elements = elements.filter(isElementVisible);
        }

        // Filter by layout pseudos: :left-of, :right-of, :above, :below, :near
        if (layoutMatch) {
            elements = evaluateLayoutPseudo(elements, layoutMatch[1].toLowerCase(), layoutMatch[2].trim());
        }

        return elements;
    }

    // Helper for layout matching
    function evaluateLayoutPseudo(elements, pseudoName, argStr) {
        if (!argStr) return elements;
        var commaIdx = argStr.lastIndexOf(',');
        var innerSel = argStr;
        var maxDist = pseudoName === 'near' ? 50 : Infinity;

        if (commaIdx !== -1) {
            var potentialNum = argStr.substring(commaIdx + 1).trim();
            var parsed = parseFloat(potentialNum);
            if (!isNaN(parsed)) {
                maxDist = parsed;
                innerSel = argStr.substring(0, commaIdx).trim();
            }
        }

        var anchors = queryCssSingleRoot(document, innerSel);
        if (anchors.length === 0) return [];

        var scored = [];
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            if (anchors.indexOf(el) !== -1) continue;
            var elRect = el.getBoundingClientRect();
            if (elRect.width === 0 && elRect.height === 0) continue;

            var minAnchorDist = Infinity;
            var valid = false;

            for (var a = 0; a < anchors.length; a++) {
                var aRect = anchors[a].getBoundingClientRect();
                if (aRect.width === 0 && aRect.height === 0) continue;

                var d = Infinity;
                if (pseudoName === 'left-of') {
                    d = aRect.left - elRect.right;
                    if (d >= 0 && d <= maxDist) valid = true;
                } else if (pseudoName === 'right-of') {
                    d = elRect.left - aRect.right;
                    if (d >= 0 && d <= maxDist) valid = true;
                } else if (pseudoName === 'above') {
                    d = aRect.top - elRect.bottom;
                    if (d >= 0 && d <= maxDist) valid = true;
                } else if (pseudoName === 'below') {
                    d = elRect.top - aRect.bottom;
                    if (d >= 0 && d <= maxDist) valid = true;
                } else if (pseudoName === 'near') {
                    var dx = Math.max(0, elRect.left - aRect.right, aRect.left - elRect.right);
                    var dy = Math.max(0, elRect.top - aRect.bottom, aRect.top - elRect.bottom);
                    d = Math.sqrt(dx * dx + dy * dy);
                    if (d <= maxDist) valid = true;
                }
                if (valid && d < minAnchorDist) {
                    minAnchorDist = d;
                }
            }

            if (valid) {
                scored.push({ element: el, dist: minAnchorDist });
            }
        }

        scored.sort(function(x, y) { return x.dist - y.dist; });
        return scored.map(function(s) { return s.element; });
    }

    // Engine: Query CSS across root and all open shadow roots
    function evaluateCss(startNodes, cssClause) {
        var results = [];
        var seen = new Set();

        for (var n = 0; n < startNodes.length; n++) {
            var roots = collectAllRoots(startNodes[n]);
            for (var r = 0; r < roots.length; r++) {
                var matched = queryCssSingleRoot(roots[r], cssClause);
                for (var m = 0; m < matched.length; m++) {
                    if (!seen.has(matched[m])) {
                        seen.add(matched[m]);
                        results.push(matched[m]);
                    }
                }
            }
        }
        return results;
    }

    // Helper: Checks if parent contains child across light and shadow DOM boundaries
    function composedContains(parent, child) {
        if (!parent || !child || parent === child) return false;
        var curr = child;
        while (curr) {
            if (curr.parentNode) {
                curr = curr.parentNode;
            } else if (curr.host) {
                curr = curr.host;
            } else {
                break;
            }
            if (curr === parent) return true;
        }
        return false;
    }

    // Engine: Query text= across open shadow roots
    function evaluateText(startNodes, textVal, forceSubstring) {
        var matcher = createTextMatcher(textVal, forceSubstring);
        var candidates = [];
        var seen = new Set();

        for (var n = 0; n < startNodes.length; n++) {
            var roots = collectAllRoots(startNodes[n]);
            for (var r = 0; r < roots.length; r++) {
                var allEls = roots[r].querySelectorAll('*');
                for (var i = 0; i < allEls.length; i++) {
                    var el = allEls[i];
                    var tag = (el.tagName || '').toLowerCase();
                    if (tag === 'script' || tag === 'style' || tag === 'noscript') continue;
                    if (matcher(getElementText(el))) {
                        if (!seen.has(el)) {
                            seen.add(el);
                            candidates.push(el);
                        }
                    }
                }
            }
        }

        // Return only innermost elements (elements that do not contain any other matching candidate)
        return candidates.filter(function(a) {
            return !candidates.some(function(b) {
                return composedContains(a, b);
            });
        });
    }

    // Engine: Query role= with attributes across open shadow roots
    function evaluateRole(startNodes, roleClause) {
        var m = roleClause.match(/^([a-zA-Z0-9_\-]+)(?:\[(.*)\])?$/);
        if (!m) return [];
        var expectedRole = m[1].toLowerCase();
        var rawAttrs = m[2] || '';

        var expectedName = null;
        var exactName = false;
        var expectedChecked = null;
        var expectedDisabled = null;
        var expectedLevel = null;
        var includeHidden = false;

        if (rawAttrs) {
            var nameMatch = rawAttrs.match(/name=(["'])(.*?)\1(s)?/i);
            if (nameMatch) {
                expectedName = nameMatch[2];
                exactName = !!nameMatch[3];
            } else {
                var unquotedNameMatch = rawAttrs.match(/name=([^,\]\s]+)/i);
                if (unquotedNameMatch) {
                    expectedName = unquotedNameMatch[1];
                }
            }
            if (/include-hidden/i.test(rawAttrs)) includeHidden = true;
            if (/checked=true/i.test(rawAttrs)) expectedChecked = true;
            if (/checked=false/i.test(rawAttrs)) expectedChecked = false;
            if (/disabled=true/i.test(rawAttrs)) expectedDisabled = true;
            if (/disabled=false/i.test(rawAttrs)) expectedDisabled = false;
            var levelMatch = rawAttrs.match(/level=(\d+)/i);
            if (levelMatch) expectedLevel = parseInt(levelMatch[1], 10);
        }

        var results = [];
        var seen = new Set();

        for (var n = 0; n < startNodes.length; n++) {
            var roots = collectAllRoots(startNodes[n]);
            for (var r = 0; r < roots.length; r++) {
                var els = roots[r].querySelectorAll('*');
                for (var i = 0; i < els.length; i++) {
                    var el = els[i];
                    if (seen.has(el)) continue;

                    var role = computeAriaRole(el);
                    if (role !== expectedRole) continue;

                    if (!includeHidden && !isElementVisible(el)) continue;

                    if (expectedName !== null) {
                        var accName = computeAccessibleName(el);
                        if (exactName) {
                            if (accName !== expectedName) continue;
                        } else {
                            if (accName.toLowerCase().indexOf(expectedName.toLowerCase()) === -1) continue;
                        }
                    }

                    if (expectedLevel !== null) {
                        var tag = (el.tagName || '').toLowerCase();
                        var tagLevel = tag.startsWith('h') ? parseInt(tag.substring(1), 10) : null;
                        var ariaLevel = el.getAttribute('aria-level') ? parseInt(el.getAttribute('aria-level'), 10) : tagLevel;
                        if (ariaLevel !== expectedLevel) continue;
                    }

                    if (expectedChecked !== null) {
                        var isChecked = el.checked === true || el.getAttribute('aria-checked') === 'true';
                        if (isChecked !== expectedChecked) continue;
                    }

                    if (expectedDisabled !== null) {
                        var isDisabled = el.disabled === true || el.getAttribute('aria-disabled') === 'true';
                        if (isDisabled !== expectedDisabled) continue;
                    }

                    seen.add(el);
                    results.push(el);
                }
            }
        }
        return results;
    }

    // Engine: Query label= (for attribute or wrapping label)
    function evaluateLabel(startNodes, labelText) {
        var matcher = createTextMatcher(labelText);
        var inputs = [];
        var seen = new Set();

        for (var n = 0; n < startNodes.length; n++) {
            var roots = collectAllRoots(startNodes[n]);
            for (var r = 0; r < roots.length; r++) {
                var labels = roots[r].querySelectorAll('label');
                for (var i = 0; i < labels.length; i++) {
                    var lbl = labels[i];
                    if (matcher(lbl.textContent || '')) {
                        var forId = lbl.getAttribute('for');
                        if (forId) {
                            var target = document.getElementById(forId);
                            if (target && !seen.has(target)) {
                                seen.add(target);
                                inputs.push(target);
                            }
                        } else {
                            var innerInput = lbl.querySelector('input, select, textarea');
                            if (innerInput && !seen.has(innerInput)) {
                                seen.add(innerInput);
                                inputs.push(innerInput);
                            }
                        }
                    }
                }
            }
        }
        return inputs;
    }

    // Execute a single step on current set of elements
    function executeStep(currentNodes, stepClause) {
        var s = stepClause.trim();
        var captured = false;
        if (s.startsWith('*')) {
            captured = true;
            s = s.substring(1).trim();
        }

        // Engine prefix parsing
        var engine = 'css';
        var body = s;
        var eqIdx = s.indexOf('=');
        if (eqIdx !== -1) {
            var prefix = s.substring(0, eqIdx).trim().toLowerCase();
            var knownEngines = [
                'css', 'xpath', 'text', 'text*', 'has-text', 'has-text*',
                'role', 'id', 'data-testid', 'testid', 'data-test-id', 'data-test',
                'data-ai', 'nth', 'visible', 'label', 'placeholder', 'alt', 'title'
            ];
            if (knownEngines.indexOf(prefix) !== -1 || prefix.startsWith('internal:')) {
                engine = prefix;
                body = s.substring(eqIdx + 1);
            }
        }

        // Shorthands
        if (engine === 'css') {
            var lower = s.toLowerCase();
            if (lower.startsWith('text:') || lower.startsWith('text*:') || lower.startsWith('has-text:') || lower.startsWith('has-text*:')) {
                var colonIdx = s.indexOf(':');
                engine = s.substring(0, colonIdx).trim().toLowerCase();
                body = s.substring(colonIdx + 1);
            } else if (s.startsWith('//') || s.startsWith('..') || s.startsWith('(')) {
                engine = 'xpath';
                body = s;
            } else if (s.startsWith('"') || s.startsWith("'")) {
                engine = 'text';
                body = s;
            } else if (/^xc[a-zA-Z0-9_\-]+$/.test(s)) {
                engine = 'data-ai';
                body = s;
            }
        }

        var matched = [];
        if (engine === 'css') {
            matched = evaluateCss(currentNodes, body);
        } else if (engine === 'text' || engine === 'text*' || engine === 'has-text' || engine === 'has-text*' || engine === 'internal:text' || engine === 'internal:has-text') {
            var forceSubstring = engine.indexOf('*') !== -1;
            matched = evaluateText(currentNodes, body, forceSubstring);
        } else if (engine === 'role' || engine === 'internal:role') {
            matched = evaluateRole(currentNodes, body);
        } else if (engine === 'id') {
            matched = evaluateCss(currentNodes, '[id="' + unquote(body) + '"]');
        } else if (engine === 'data-testid' || engine === 'data-test-id' || engine === 'data-test' || engine === 'testid' || engine === 'internal:testid') {
            matched = evaluateCss(currentNodes, '[data-testid="' + unquote(body) + '"], [data-test-id="' + unquote(body) + '"], [data-test="' + unquote(body) + '"]');
        } else if (engine === 'data-ai') {
            matched = evaluateCss(currentNodes, '[data-ai="' + unquote(body) + '"]');
        } else if (engine === 'label' || engine === 'internal:label') {
            matched = evaluateLabel(currentNodes, body);
        } else if (engine === 'placeholder') {
            matched = evaluateCss(currentNodes, '[placeholder="' + unquote(body) + '"]');
        } else if (engine === 'alt') {
            matched = evaluateCss(currentNodes, '[alt="' + unquote(body) + '"]');
        } else if (engine === 'title') {
            matched = evaluateCss(currentNodes, '[title="' + unquote(body) + '"]');
        } else if (engine === 'visible') {
            var wantVisible = body.trim().toLowerCase() === 'true';
            matched = currentNodes.filter(function(el) {
                return isElementVisible(el) === wantVisible;
            });
        } else if (engine === 'nth') {
            var n = parseInt(body.trim(), 10);
            if (!isNaN(n)) {
                if (n < 0) n = currentNodes.length + n;
                if (n >= 0 && n < currentNodes.length) {
                    matched = [currentNodes[n]];
                }
            }
        } else if (engine === 'xpath') {
            // XPath strictly does NOT pierce shadow DOM
            var xRes = document.evaluate(body, root, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
            for (var xi = 0; xi < xRes.snapshotLength; xi++) {
                matched.push(xRes.snapshotItem(xi));
            }
        }

        return {
            elements: matched,
            captured: captured
        };
    }

    // Main evaluation loop over >> chain
    var clauses = splitClauses(rawSelector);
    if (clauses.length === 0) return [];

    var currentNodes = [root];
    var capturedElements = null;

    for (var i = 0; i < clauses.length; i++) {
        var stepResult = executeStep(currentNodes, clauses[i]);
        if (stepResult.captured) {
            capturedElements = stepResult.elements;
        }
        currentNodes = stepResult.elements;
        if (currentNodes.length === 0) break;
    }

    return capturedElements !== null ? capturedElements : currentNodes;
})(arguments[0], arguments[1]);
