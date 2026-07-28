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
 * Generates a stable, collision-resistant fingerprint string for an element.
 */
function fingerprint(el) {
    var tag   = el.tagName ? el.tagName.toLowerCase() : '';
    var id    = el.id || '';
    
    // Prioritize stable E2E testing and automation selectors over presentation attributes
    var testId = el.getAttribute('data-testid') || el.getAttribute('data-test') || el.getAttribute('data-qa') || el.getAttribute('data-cy') || '';
    
    // Normalize CSS classes to remove transient and dynamic segments
    var cls   = (typeof el.className === 'string' ? el.className : '').trim().replace(/\s+/g, ' ');
    if (cls) {
        // Strip build-specific CSS Module hashes (e.g., '__1a2b3c') to keep classes stable across builds
        cls = cls.replace(/__[a-zA-Z0-9_-]{5,8}\b/g, '');
        // Strip transient interaction/state-dependent classes to keep hashes stable across active element states
        cls = cls.replace(/\b(is-active|active|is-open|open|hovered|focused|loading|disabled|collapsed|show|expanded|checked)\b/g, '').trim().replace(/\s+/g, ' ');
    }
    var ptag  = el.parentElement && el.parentElement.tagName ? el.parentElement.tagName.toLowerCase() : '';
    var type  = el.getAttribute('type') || '';
    var name  = el.getAttribute('name') || el.getAttribute('alt') || el.getAttribute('for') || el.getAttribute('aria-label') || '';
    
    // Normalize text: extract a prefix and mask all numbers to prevent counter/price changes from breaking the hash
    var text  = (el.innerText || '').trim().substring(0, 15).toLowerCase().replace(/\d+/g, '#');
    
    // Gather framework component context if applicable
    var fwComp = getFrameworkComponent(el);
    
    // Combine all stable features into a single fingerprint string
    var raw   = [tag, id, testId, cls, ptag, type, name, text, fwComp].join('|');
    return 'xc' + djb2(raw);
}
