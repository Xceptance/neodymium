/**
 * Neodymium Color Wireframe Helper.
 * Injects a transient stylesheet that neutralizes dynamic media and text into skeleton bars
 * while preserving container geometry, layout, borders, and brand design system colors.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
(function() {
    var STYLE_ID = 'neodymium-color-wireframe-style';

    window.__neodymiumApplyColorWireframe = function() {
        if (document.getElementById(STYLE_ID)) {
            return;
        }
        var style = document.createElement('style');
        style.id = STYLE_ID;
        style.textContent = ''
            + 'img, video, canvas, picture { object-position: -99999px !important; background-color: #E5E7EB !important; }\n'
            + 'svg:not([data-icon]) { opacity: 0.15 !important; background-color: #E5E7EB !important; }\n'
            + '[style*="background-image"], .hero-banner, .banner, .c-hero-banner { background-image: none !important; }\n'
            + 'p, h1, h2, h3, h4, h5, h6, li, a, button, label, strong, em, b, i, th, td, caption, span:not([class*="icon"]):not([class*="material-symbols"]) {\n'
            + '    -webkit-text-fill-color: transparent !important;\n'
            + '    background-color: currentColor !important;\n'
            + '    border-radius: 3px !important;\n'
            + '    box-decoration-break: clone;\n'
            + '    -webkit-box-decoration-break: clone;\n'
            + '}\n'
            + 'input, textarea { -webkit-text-fill-color: transparent !important; }\n';
        (document.head || document.documentElement).appendChild(style);
    };

    window.__neodymiumRemoveColorWireframe = function() {
        var existing = document.getElementById(STYLE_ID);
        if (existing && existing.parentNode) {
            existing.parentNode.removeChild(existing);
        }
    };
})();
