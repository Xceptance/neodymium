// Cached framework metadata property keys to avoid scanning element properties repeatedly
var reactFiberKey = null;
var vueKey = null;

// Fast, low-overhead djb2 hashing algorithm to compute consistent element fingerprint hashes
function djb2(str) {
    var hash = 5381;
    for (var i = 0; i < str.length; i++) {
        hash = ((hash << 5) + hash) + str.charCodeAt(i);
        hash = hash & 0x7fffffff; // Keep hash as a positive 31-bit integer
    }
    return hash.toString(36); // Compact alphanumeric base-36 representation
}

/**
 * Extracts the framework component name (React or Vue) for a given DOM element.
 * Navigates the internal Virtual DOM trees if present, caching key names for maximum speed.
 */
function getFrameworkComponent(el) {
    // 1. Resolve React Component Name via internal Fiber Node
    if (reactFiberKey === null) {
        reactFiberKey = Object.keys(el).find(function(k) {
            return k.indexOf('__reactFiber$') === 0;
        }) || '';
    }
    if (reactFiberKey && el[reactFiberKey]) {
        var curr = el[reactFiberKey];
        while (curr) {
            if (curr.type && typeof curr.type === 'function' && curr.type.name) {
                return curr.type.name; // E.g., "ProductCard"
            }
            curr = curr.return;
        }
    }

    // 2. Resolve Vue Component Name (supports Vue 2 and Vue 3)
    if (vueKey === null) {
        vueKey = Object.keys(el).find(function(k) {
            return k === '__vue__' || k.indexOf('__vueParentComponent') === 0;
        }) || '';
    }
    if (vueKey && el[vueKey]) {
        var vnode = el[vueKey];
        if (vnode.type && typeof vnode.type === 'object') {
            return vnode.type.name || vnode.type.__name || '';
        }
        if (vnode.$options) {
            return vnode.$options.name || vnode.$options._componentTag || '';
        }
    }
    return ''; // Default fallback for standard HTML or other unsupported pages
}

/**
 * Checks whether an element ID is auto-generated or volatile.
 */
function isVolatileId(id) {
    if (!id || typeof id !== 'string') return false;
    if (/^(v-btn|v-node|react-div|react|ember|aria)-\d+$/i.test(id)) return true;
    if (/[_-]\d{4,}$/.test(id)) return true;
    if (typeof volatilePatterns !== 'undefined' && Array.isArray(volatilePatterns)) {
        for (var i = 0; i < volatilePatterns.length; i++) {
            try {
                var pat = volatilePatterns[i];
                if (pat && typeof pat === 'string' && pat.trim().length > 0) {
                    var pStr = pat.trim();
                    if (pStr.startsWith('/') && pStr.lastIndexOf('/') > 0) {
                        var lastSlash = pStr.lastIndexOf('/');
                        var body = pStr.substring(1, lastSlash);
                        var flags = pStr.substring(lastSlash + 1);
                        if (new RegExp(body, flags).test(id)) return true;
                    } else {
                        if (new RegExp(pStr).test(id)) return true;
                    }
                }
            } catch (e) {}
        }
    }
    return false;
}

/**
 * Returns the 0-based same-tag index of an element relative to its parent's relevant element children.
 * Ignores scripts, styles, links, and HUD overlays to ensure index stability across runs.
 */
function getElementIndex(el) {
    if (!el || !el.parentElement) return 0;
    var targetTag = el.tagName ? el.tagName.toLowerCase() : '';
    if (targetTag === 'script' || targetTag === 'style' || targetTag === 'link') return 0;
    var count = 0;
    var child = el.parentElement.firstElementChild;
    while (child) {
        if (child === el) return count;
        if (child.tagName && child.tagName.toLowerCase() === targetTag) {
            if (!child.classList || !child.classList.contains('neodymium-ai-hud')) {
                count++;
            }
        }
        child = child.nextElementSibling;
    }
    return count;
}

/**
 * Constructs a concise ancestor path for an element up to 3 levels, stripping transient state classes.
 */
function getAncestorPath(el) {
    var parts = [];
    var curr = el.parentElement;
    var depth = 0;
    while (curr && curr.tagName && depth < 3) {
        var tag = curr.tagName.toLowerCase();
        if (tag === 'body' || tag === 'html') break;
        var cls = (typeof curr.className === 'string' ? curr.className : '').trim().replace(new RegExp('\\s+', 'g'), ' ');
        if (cls) {
            cls = cls.replace(new RegExp('__[a-zA-Z0-9_-]{5,8}\\b', 'g'), '');
            cls = cls.replace(new RegExp('\\b(is-active|active|is-open|open|hovered|focused|loading|disabled|collapsed|show|expanded|checked|hydrated)\\b', 'g'), '').trim().split(new RegExp('\\s+'))[0] || '';
        }
        parts.push(tag + (cls ? '.' + cls : ''));
        curr = curr.parentElement;
        depth++;
    }
    return parts.join('>');
}

/**
 * Generates a stable, collision-resistant, deterministic fingerprint string for an element.
 */
function fingerprint(el) {
    var tag = el.tagName ? el.tagName.toLowerCase() : '';
    var id = el.id || '';
    if (isVolatileId(id)) {
        id = '';
    }

    // Prioritize stable E2E testing and automation selectors over presentation attributes
    var testId = el.getAttribute('data-testid') || el.getAttribute('data-test') || el.getAttribute('data-qa') || el.getAttribute('data-cy') || '';

    // Normalize CSS classes to remove transient and dynamic segments
    var cls = (typeof el.className === 'string' ? el.className : '').trim().replace(new RegExp('\\s+', 'g'), ' ');
    if (cls) {
        // Strip build-specific CSS Module hashes (e.g., '__1a2b3c') to keep classes stable across builds
        cls = cls.replace(new RegExp('__[a-zA-Z0-9_-]{5,8}\\b', 'g'), '');
        // Strip transient interaction/state-dependent classes to keep hashes stable across active element states
        cls = cls.replace(new RegExp('\\b(is-active|active|is-open|open|hovered|focused|loading|disabled|collapsed|show|expanded|checked)\\b', 'g'), '').trim().replace(new RegExp('\\s+', 'g'), ' ');
    }
    var path = getAncestorPath(el);
    var idx = getElementIndex(el);
    var type = el.getAttribute('type') || '';
    var name = el.getAttribute('name') || el.getAttribute('alt') || el.getAttribute('for') || el.getAttribute('aria-label') || '';

    // Gather framework component context if applicable
    var fwComp = getFrameworkComponent(el);

    // Combine all stable features into a single fingerprint string
    var raw = [tag, id, testId, cls, path, idx, type, name, fwComp].join('|');
    return 'xc' + djb2(raw);
}
