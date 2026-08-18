// ============================================================================
// Neodymium Aura Dashboard - Settings Modal & Property Management
// ============================================================================

function openSettingsModal() {
    const modal = document.getElementById('settingsModal');
    if (modal) {
        modal.style.display = 'flex';
        htmx.ajax('GET', '/api/settings', { target: '#settingsModal', swap: 'outerHTML' }).then(() => {
            const reloadedModal = document.getElementById('settingsModal');
            if (reloadedModal) reloadedModal.style.display = 'flex';
        });
    }
}
window.openSettingsModal = openSettingsModal;

function closeSettingsModal() {
    const modal = document.getElementById('settingsModal');
    if (modal) modal.style.display = 'none';
}
window.closeSettingsModal = closeSettingsModal;

function toggleSettingsGroup(headerElem) {
    const card = headerElem.closest('.settings-group-card');
    if (!card) return;
    const content = card.querySelector('.settings-group-content');
    const arrow = card.querySelector('.accordion-arrow');
    if (!content) return;

    if (content.style.display === 'none' || content.style.display === '') {
        content.style.display = 'block';
        if (arrow) arrow.classList.add('open');
    } else {
        content.style.display = 'none';
        if (arrow) arrow.classList.remove('open');
    }
}
window.toggleSettingsGroup = toggleSettingsGroup;

function expandAllSettingsGroups() {
    document.querySelectorAll('.settings-group-content').forEach(el => el.style.display = 'block');
    document.querySelectorAll('.accordion-arrow').forEach(el => el.classList.add('open'));
}
window.expandAllSettingsGroups = expandAllSettingsGroups;

function collapseAllSettingsGroups() {
    document.querySelectorAll('.settings-group-content').forEach(el => el.style.display = 'none');
    document.querySelectorAll('.accordion-arrow').forEach(el => el.classList.remove('open'));
}
window.collapseAllSettingsGroups = collapseAllSettingsGroups;

function filterSettingsProperties(query) {
    const q = (query || '').toLowerCase().trim();

    document.querySelectorAll('.settings-entry-row').forEach(row => {
        const key = (row.getAttribute('data-key') || '').toLowerCase();
        const desc = (row.getAttribute('data-desc') || '').toLowerCase();
        if (q === '' || key.includes(q) || desc.includes(q)) {
            row.style.display = 'flex';
        } else {
            row.style.display = 'none';
        }
    });

    document.querySelectorAll('.settings-section').forEach(section => {
        const rows = section.querySelectorAll('.settings-entry-row');
        if (rows.length === 0) {
            section.style.display = (q === '') ? 'block' : 'none';
            return;
        }
        const visibleRows = Array.from(rows).filter(r => r.style.display !== 'none');
        if (q === '' || visibleRows.length > 0) {
            section.style.display = 'block';
        } else {
            section.style.display = 'none';
        }
    });

    document.querySelectorAll('.settings-group-card').forEach(card => {
        if (card.id === 'addBrowserProfileFormCard') return;

        const sections = card.querySelectorAll('.settings-section');
        const devNeoRows = card.querySelectorAll('.dev-neo-row');

        if (q === '') {
            card.style.display = 'block';
            return;
        }

        const visibleSections = Array.from(sections).filter(s => s.style.display !== 'none');
        const visibleDevNeo = Array.from(devNeoRows).filter(r => {
            const key = (r.querySelector('.dev-neo-key-input')?.value || '').toLowerCase();
            const val = (r.querySelector('.dev-neo-val-input')?.value || '').toLowerCase();
            return key.includes(q) || val.includes(q);
        });

        if (visibleSections.length > 0 || visibleDevNeo.length > 0) {
            card.style.display = 'block';
            const content = card.querySelector('.settings-group-content');
            if (content) content.style.display = 'block';
            const arrow = card.querySelector('.accordion-arrow');
            if (arrow) arrow.classList.add('open');
        } else {
            card.style.display = 'none';
        }
    });
}
window.filterSettingsProperties = filterSettingsProperties;

function handleDevNeoRowInput(inputElem) {
    const container = document.getElementById('devNeoRowsContainer');
    if (!container) return;
    const rows = container.querySelectorAll('.dev-neo-row');
    const lastRow = rows[rows.length - 1];

    if (inputElem.closest('.dev-neo-row') === lastRow) {
        const keyVal = lastRow.querySelector('.dev-neo-key-input').value.trim();
        const valVal = lastRow.querySelector('.dev-neo-val-input').value.trim();
        if (keyVal !== '' || valVal !== '') {
            const newRow = document.createElement('div');
            newRow.className = 'dev-neo-row';
            newRow.innerHTML = `
                <input type="text" name="devNeoKey" class="modal-input dev-neo-key-input" list="autocompleteKeysList" value="" placeholder="Add new property key..." oninput="handleDevNeoRowInput(this)"/>
                <input type="text" name="devNeoValue" class="modal-input dev-neo-val-input" value="" placeholder="Value..." oninput="handleDevNeoRowInput(this)"/>
                <button type="button" class="btn-icon-danger" onclick="removeDevNeoRow(this)" title="Delete property override"><span class="material-symbols-outlined">delete</span></button>
            `;
            container.appendChild(newRow);
        }
    }
}
window.handleDevNeoRowInput = handleDevNeoRowInput;

function removeDevNeoRow(btnElem) {
    const row = btnElem.closest('.dev-neo-row');
    if (!row) return;
    const container = document.getElementById('devNeoRowsContainer');
    if (!container) return;

    if (container.querySelectorAll('.dev-neo-row').length > 1) {
        row.remove();
    } else {
        const keyInput = row.querySelector('.dev-neo-key-input');
        const valInput = row.querySelector('.dev-neo-val-input');
        if (keyInput) keyInput.value = '';
        if (valInput) valInput.value = '';
    }
}
window.removeDevNeoRow = removeDevNeoRow;

function switchSettingsTab(tabName) {
    const btnGeneral = document.getElementById('tabBtnGeneral');
    const btnBrowser = document.getElementById('tabBtnBrowser');
    const contentGeneral = document.getElementById('tabContentGeneral');
    const contentBrowser = document.getElementById('tabContentBrowser');
    const activeTabInput = document.getElementById('activeTabInput');

    if (activeTabInput) activeTabInput.value = tabName;

    if (tabName === 'browser') {
        if (btnGeneral) btnGeneral.classList.remove('active');
        if (btnBrowser) btnBrowser.classList.add('active');
        if (contentGeneral) {
            contentGeneral.classList.remove('active');
            contentGeneral.style.display = 'none';
        }
        if (contentBrowser) {
            contentBrowser.classList.add('active');
            contentBrowser.style.display = 'block';
        }
    } else {
        if (btnGeneral) btnGeneral.classList.add('active');
        if (btnBrowser) btnBrowser.classList.remove('active');
        if (contentGeneral) {
            contentGeneral.classList.add('active');
            contentGeneral.style.display = 'block';
        }
        if (contentBrowser) {
            contentBrowser.classList.remove('active');
            contentBrowser.style.display = 'none';
        }
    }
}
window.switchSettingsTab = switchSettingsTab;

function toggleAddBrowserProfileForm() {
    const card = document.getElementById('addBrowserProfileFormCard');
    if (!card) return;
    if (card.style.display === 'none' || !card.style.display) {
        card.style.display = 'block';
    } else {
        card.style.display = 'none';
    }
}
window.toggleAddBrowserProfileForm = toggleAddBrowserProfileForm;

function toggleBrowserHelpGuide(headerElem) {
    const card = headerElem.closest('.browser-help-card');
    if (!card) return;
    const content = card.querySelector('.browser-help-content');
    const arrow = card.querySelector('.help-guide-arrow');
    if (!content) return;

    if (content.style.display === 'none' || !content.style.display) {
        content.style.display = 'block';
        if (arrow) arrow.style.transform = 'rotate(180deg)';
    } else {
        content.style.display = 'none';
        if (arrow) arrow.style.transform = 'rotate(0deg)';
    }
}
window.toggleBrowserHelpGuide = toggleBrowserHelpGuide;
