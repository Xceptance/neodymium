document.addEventListener('DOMContentLoaded', () => {
    // Determine if the current SUT page needs the controls drawer
    const urlParams = new URLSearchParams(window.location.search);
    const forceControls = urlParams.get('controls') === 'true' || urlParams.get('anomaly') === 'true';
    const path = window.location.pathname;
    
    // Explicit pages/paths that require anomaly control capabilities
    const isAnomalyPage = path.includes('/a11y/') || path.includes('/dashboard/') || path.includes('/shop/forms.html');

    if (!isAnomalyPage && !forceControls) {
        return;
    }

    // 1. Build and Inject floating control panel HTML dynamically
    const drawerHtml = `
    <!-- Trigger Button -->
    <div class="aura-controls-trigger" id="aura-trigger" title="Aura Defect Controls">
        <svg viewBox="0 0 24 24">
            <path d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33C5.02,5.25,4.77,5.33,4.65,5.55L2.73,8.87 C2.62,9.08,2.67,9.34,2.85,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.04,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.43-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"/>
        </svg>
    </div>

    <!-- Sliding Drawer -->
    <div class="aura-drawer" id="aura-drawer">
        <div class="drawer-header">
            <div class="drawer-title">Aura Chaos Controls</div>
            <button class="drawer-close" id="aura-close">&times;</button>
        </div>
        
        <div class="drawer-section">
            <div class="drawer-section-title">Visual & Layout Defects</div>
            
            <!-- Overlap Defect -->
            <div class="control-item">
                <label class="control-label" for="toggle-overlap">
                    <span class="control-name">Inject Element Overlap</span>
                    <span class="control-desc">Positions key controls directly over headings.</span>
                </label>
                <label class="switch">
                    <input type="checkbox" id="toggle-overlap">
                    <span class="slider"></span>
                </label>
            </div>
            
            <!-- Contrast Defect -->
            <div class="control-item">
                <label class="control-label" for="toggle-contrast">
                    <span class="control-name">Inject Low Contrast</span>
                    <span class="control-desc">Forces poor visibility on text colors.</span>
                </label>
                <label class="switch">
                    <input type="checkbox" id="toggle-contrast">
                    <span class="slider"></span>
                </label>
            </div>
            
            <!-- Clipped Text Defect -->
            <div class="control-item">
                <label class="control-label" for="toggle-clipped">
                    <span class="control-name">Inject Clipped Text</span>
                    <span class="control-desc">Clamps cards height with overflow hidden.</span>
                </label>
                <label class="switch">
                    <input type="checkbox" id="toggle-clipped">
                    <span class="slider"></span>
                </label>
            </div>
            
            <!-- Responsive Defect -->
            <div class="control-item">
                <label class="control-label" for="toggle-grid">
                    <span class="control-name">Inject Mobile Overlap</span>
                    <span class="control-desc">Squeezes responsive columns without wrapping.</span>
                </label>
                <label class="switch">
                    <input type="checkbox" id="toggle-grid">
                    <span class="slider"></span>
                </label>
            </div>
            
            <!-- Layout Shift -->
            <div class="control-item">
                <label class="control-label" for="toggle-shift">
                    <span class="control-name">Inject Layout Misalignment</span>
                    <span class="control-desc">Applies asymmetrical shifts to broken grids.</span>
                </label>
                <label class="switch">
                    <input type="checkbox" id="toggle-shift">
                    <span class="slider"></span>
                </label>
            </div>
        </div>

        <div class="drawer-section">
            <div class="drawer-section-title">Conditional Steps</div>
            
            <!-- Promo Banner -->
            <div class="control-item">
                <label class="control-label" for="toggle-banner">
                    <span class="control-name">Show Promo Banner</span>
                    <span class="control-desc">Toggles optional marketing header.</span>
                </label>
                <label class="switch">
                    <input type="checkbox" id="toggle-banner" checked>
                    <span class="slider"></span>
                </label>
            </div>
        </div>
    </div>
    `;

    const container = document.createElement('div');
    container.innerHTML = drawerHtml;
    document.body.appendChild(container);

    // 2. Add Open/Close slide interactions
    const trigger = document.getElementById('aura-trigger');
    const drawer = document.getElementById('aura-drawer');
    const closeBtn = document.getElementById('aura-close');

    trigger.addEventListener('click', () => {
        drawer.classList.toggle('open');
    });

    closeBtn.addEventListener('click', () => {
        drawer.classList.remove('open');
    });

    // 3. Register anomaly toggle handlers
    const toggleOverlap = document.getElementById('toggle-overlap');
    const toggleContrast = document.getElementById('toggle-contrast');
    const toggleClipped = document.getElementById('toggle-clipped');
    const toggleGrid = document.getElementById('toggle-grid');
    const toggleShift = document.getElementById('toggle-shift');
    const toggleBanner = document.getElementById('toggle-banner');

    // Overlap Defect
    toggleOverlap.addEventListener('change', (e) => {
        const overlapTarget = document.querySelector('.overlap-target-btn');
        if (overlapTarget) {
            if (e.target.checked) {
                overlapTarget.classList.add('overlap-defect-element');
            } else {
                overlapTarget.classList.remove('overlap-defect-element');
            }
        }
    });

    // Contrast Defect
    toggleContrast.addEventListener('change', (e) => {
        if (e.target.checked) {
            document.body.classList.add('has-contrast-defect');
        } else {
            document.body.classList.remove('has-contrast-defect');
        }
    });

    // Clipped Text
    toggleClipped.addEventListener('change', (e) => {
        const clippedContainers = document.querySelectorAll('.clipped-container');
        clippedContainers.forEach(container => {
            if (e.target.checked) {
                container.classList.add('has-clipped-defect');
            } else {
                container.classList.remove('has-clipped-defect');
            }
        });
    });

    // Responsive Flex Defect
    toggleGrid.addEventListener('change', (e) => {
        const grids = document.querySelectorAll('.columns-grid');
        grids.forEach(grid => {
            if (e.target.checked) {
                grid.classList.add('has-responsive-defect');
            } else {
                grid.classList.remove('has-responsive-defect');
            }
        });
    });

    // Layout Misalignment Shift
    toggleShift.addEventListener('change', (e) => {
        const misaligned = document.querySelectorAll('.misaligned-element');
        misaligned.forEach(el => {
            if (e.target.checked) {
                el.classList.add('has-layout-shift');
            } else {
                el.classList.remove('has-layout-shift');
            }
        });
    });

    // Optional Promo Banner Toggle
    toggleBanner.addEventListener('change', (e) => {
        const banner = document.getElementById('promo-banner-container');
        if (banner) {
            if (e.target.checked) {
                banner.style.display = 'flex';
            } else {
                banner.style.display = 'none';
            }
        }
    });
});
