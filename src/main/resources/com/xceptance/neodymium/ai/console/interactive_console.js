// State variables
let currentState = null;
let currentPauseId = null;
let currentRunId = null;
let isAutoMode = false;
let eventSource = null;
let helpOverlayClicked = false;
let selectedStepIndexForDetails = null;
let autoStartStepIndex = null;
// Flat ordered list of all step indices across all blocks (before + steps + after).
// Used by the ↑/↓ reorder buttons to know if a step is first/last.
let allStepIndices = [];

// -------------------------------------------------------------------------
// SSE Connection
// -------------------------------------------------------------------------
function connectSSE() {
    if (eventSource) eventSource.close();
    eventSource = new EventSource('/api/console/events');

    eventSource.addEventListener('state', (e) => {
        const data = JSON.parse(e.data);
        applyState(data);
    });

    eventSource.addEventListener('pause', (e) => {
        const data = JSON.parse(e.data);
        currentPauseId = data.pauseId;
        // Mirror into currentState so local re-renders (toggleBp, handleStepClick, …)
        // that call applyState(currentState) pick up the token via the 'pauseId' in state guard.
        if (currentState) {
            currentState.pauseId = data.pauseId;
        }
        if (currentRunId && data.runId !== currentRunId) {
            showStaleBanner(data.runId);
        } else {
            currentRunId = data.runId;
            setButtonsEnabled(true);
            checkActionApprovals();
            if (currentPauseId && currentPauseId.startsWith('pause-final-')) {
                const finalSaveOverlay = document.getElementById('finalSaveOverlay');
                const finalSaveText = document.getElementById('finalSaveText');
                const finalSaveButtons = document.getElementById('finalSaveButtons');
                if (finalSaveOverlay) {
                    const editsMade = currentState && currentState.hudPromptChanged === true;
                    const saveScopeContainer = document.getElementById('saveScopeContainer');
                    if (saveScopeContainer) {
                        saveScopeContainer.style.display = editsMade ? 'block' : 'none';
                        const saveScopeSelect = document.getElementById('saveScopeSelect');
                        if (saveScopeSelect && currentState && currentState.yamlScope) {
                            saveScopeSelect.value = currentState.yamlScope;
                        }
                    }
                    if (finalSaveText) {
                        if (editsMade) {
                            let files = [];
                            if (currentState.yamlSource) {
                                files.push(currentState.yamlSource);
                            }
                            if (currentState.playbookFile) {
                                files.push(currentState.playbookFile);
                            }
                            let filesHtml = files.length > 0 ? "<div style='margin: 10px 0; padding: 10px; background: rgba(0,0,0,0.2); border-radius: 4px;'><ul style='margin: 0; padding-left: 20px; text-align: left;'>" + files.map(f => "<li style='word-break: break-all;'><code>" + f + "</code></li>").join("") + "</ul></div>" : "";
                            finalSaveText.innerHTML = "You have made changes to the test steps during execution. The following files will be updated:" + filesHtml + "Would you like to save these changes?";
                        } else {
                            finalSaveText.innerHTML = "Test execution finished successfully!";
                        }
                    }
                    if (finalSaveButtons) {
                        finalSaveButtons.innerHTML = editsMade
                            ? `<button class="btn btn-primary" onclick="sendAction('SAVE_EXIT', { saveScope: document.getElementById('saveScopeSelect')?.value || 'local' })">Save Changes</button>
                               <button class="btn btn-danger" onclick="sendAction('DISCARD')">Discard</button>`
                            : `<button class="btn btn-primary" onclick="sendAction('DISCARD')">Close Console</button>`;
                    }
                    finalSaveOverlay.classList.add('active');
                }
            } else if (isAutoMode) {
                triggerAutoRunCheck();
            }
        }
    });

    eventSource.addEventListener('dumpReady', (e) => {
        const data = JSON.parse(e.data);
        showDumpPopup(data);
    });

    eventSource.onopen = () => setConnectionState('connected');
    eventSource.onerror = () => {
        setConnectionState('error');
        setTimeout(connectSSE, 3000); // Retry in 3 seconds
    };
}

// -------------------------------------------------------------------------
// Static JSON Fallback Load
// -------------------------------------------------------------------------
function tryStaticLoad() {
    const params = new URLSearchParams(window.location.search);
    const dataUrl = params.get('dataUrl') || params.get('data') || '/run_data.json';

    // Extract history run ID if loaded from history
    if (dataUrl.includes('/api/reporting/report/')) {
        const parts = dataUrl.split('/');
        // /api/reporting/report/<runId>/...
        if (parts.length > 4) {
            window.historyRunId = parts[4];
        }
    }

    fetch(dataUrl)
        .then(r => r.ok ? r.json() : Promise.reject(r.status))
        .then(data => {
            applyState(data);
            setConnectionState('connected');
            setButtonsEnabled(false);
            document.getElementById('reasoningText').textContent = '(Offline view — no live updates)';
        })
        .catch(() => {
            // Try direct run_example.json fallback if the server custom path failed
            fetch('run_example.json')
                .then(r => r.ok ? r.json() : Promise.reject(r.status))
                .then(data => {
                    applyState(data);
                    setConnectionState('connected');
                    setButtonsEnabled(false);
                    document.getElementById('reasoningText').textContent = '(Offline view — no live updates)';
                })
                .catch(() => {
                    setConnectionState('error');
                    document.getElementById('reasoningText').textContent = 'Could not load run data. Pass ?data=path/to/run.json';
                });
        });
}

// Detect connection method on load
window.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);

    // ── Touch capability / Mobile detection ──────────────────────────────────
    const isTouchDevice = 'ontouchstart' in window || navigator.maxTouchPoints > 0 || navigator.msMaxTouchPoints > 0;
    if (isTouchDevice) {
        document.body.classList.add('has-touch');
    } else {
        document.body.classList.add('no-touch');
    }

    // ── Mode detection ───────────────────────────────────────────────────────
    // Two mutually-exclusive display modes alter what the console shows:
    //
    //  mode-results   → loaded from history (/api/reporting/report/…); read-only.
    //                   Hides: add-step buttons, bottom action bar, progress
    //                   pill/bar, shortcuts & info icon.
    //  mode-embedded  → running inside an iframe (window.parent !== window).
    //                   Hides: status dot, settings button, theme toggle,
    //                   mobile-QR button (all owned by the parent dashboard).
    //
    const dataUrl = params.get('dataUrl') || params.get('data') || '';
    if (dataUrl.includes('/api/reporting/report/')) {
        document.body.classList.add('mode-results');
    }
    if (window.parent !== window) {
        document.body.classList.add('mode-embedded');
    }
    // ────────────────────────────────────────────────────────────────────────

    if (params.has('dataUrl') || params.has('data')) {
        tryStaticLoad();
    } else {
        fetch('/api/console/events', { method: 'HEAD' })
            .then(r => { if (r.ok || r.status === 200) connectSSE(); else tryStaticLoad(); })
            .catch(() => tryStaticLoad());
    }

    // Initialize help tooltip hover
    const btnHelp = document.getElementById('btnHelp');
    const helpOverlay = document.getElementById('helpOverlay');
    if (btnHelp && helpOverlay) {
        const container = btnHelp.closest('.help-hover-container');
        if (container) {
            container.addEventListener('mouseenter', () => {
                helpOverlay.style.display = 'flex';
            });
            container.addEventListener('mouseleave', () => {
                if (!helpOverlayClicked) {
                    helpOverlay.style.display = 'none';
                }
            });
        }
    }

    // Load settings from localStorage
    const savedTheme = localStorage.getItem('neodymium.hud.theme') || 'system';
    const savedZoom = localStorage.getItem('neodymium.hud.zoom') || '100';

    const themeSelect = document.getElementById('themeSelect');
    if (themeSelect) themeSelect.value = savedTheme;
    applyTheme(savedTheme);

    const zoomInput = document.getElementById('zoomInput');
    if (zoomInput) zoomInput.value = savedZoom;
    const zoom = parseInt(savedZoom, 10);
    if (!isNaN(zoom)) {
        const factor = zoom / 100;
        document.body.style.zoom = factor;
        document.body.style.height = (100 / factor) + 'dvh';
    }

    // Listen for system theme media query changes
    window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', () => {
        const themeSelect = document.getElementById('themeSelect');
        if (themeSelect && themeSelect.value === 'system') {
            applyTheme('system');
        }
    });

    // Viewport drag/zoom event handlers
    const viewport = document.getElementById('screenshotViewport');
    if (viewport) {
        viewport.addEventListener('mousedown', (e) => {
            isPanning = true;
            viewport.style.cursor = 'grabbing';
            startX = e.clientX - panX;
            startY = e.clientY - panY;
            e.preventDefault();
        });

        window.addEventListener('mousemove', (e) => {
            if (!isPanning) return;
            panX = e.clientX - startX;
            panY = e.clientY - startY;
            updateScreenshotTransform();
        });

        window.addEventListener('mouseup', () => {
            isPanning = false;
            if (viewport) viewport.style.cursor = 'grab';
        });

        viewport.addEventListener('wheel', (e) => {
            e.preventDefault();
            const factor = e.deltaY < 0 ? 1.15 : 0.85;
            zoomScreenshot(factor);
        }, { passive: false });

        // Mobile touch support
        viewport.addEventListener('touchstart', (e) => {
            if (e.touches.length === 1) {
                isPanning = true;
                startX = e.touches[0].clientX - panX;
                startY = e.touches[0].clientY - panY;
            }
        });
        viewport.addEventListener('touchmove', (e) => {
            if (!isPanning || e.touches.length !== 1) return;
            panX = e.touches[0].clientX - startX;
            panY = e.touches[0].clientY - startY;
            updateScreenshotTransform();
        });
        viewport.addEventListener('touchend', () => {
            isPanning = false;
        });
    }
});

// -------------------------------------------------------------------------
// State application / Dynamic rendering
// -------------------------------------------------------------------------

window.testErrors = [];
const oldError = console.error;
console.error = function(...args) {
    window.testErrors.push(args.join(' '));
    oldError.apply(console, args);
};

const activeBreakpoints = new Set();

function applyState(state) {
    if (!state || Object.keys(state).length === 0) return;

    // Capture editing state to prevent mid-flight re-renders from wiping out user input
    let activeEditStepIdx = null;
    let activeEditText = null;
    const editingCard = document.querySelector('.step-card.editing');
    if (editingCard) {
        activeEditStepIdx = parseInt(editingCard.getAttribute('data-step-idx'), 10);
        const ta = editingCard.querySelector('.inline-edit-textarea');
        if (ta) activeEditText = ta.value;
    }
    window.currentlyEditingStepIndex = activeEditStepIdx;
    window.currentEditText = activeEditText;

    const isFirstLoad = currentState === null;

    if (isFirstLoad) {
        const overlay = document.getElementById('initialLoadingOverlay');
        if (overlay) {
            overlay.style.opacity = '0';
            setTimeout(() => {
                overlay.style.visibility = 'hidden';
                overlay.style.display = 'none';
            }, 300);
        }
    }

    // Only update currentPauseId when the incoming state explicitly carries a
    // pauseId field. The server never embeds pauseId in pushState() payloads —
    // it is sent exclusively via the separate 'pause' SSE event. Local re-renders
    // (toggleBp, handleStepClick, openAddStepOverlay …) pass currentState back
    // into applyState; if we blindly overwrote currentPauseId here those calls
    // would clear the token that was set by the pause event, causing sendAction()
    // to silently drop the next Run/Skip click.
    if ('pauseId' in state) {
        currentPauseId = state.pauseId;
        window.currentPauseId = currentPauseId; // Expose to Selenium
    }

    // On a new test run, clear the selected step to fall back to default behavior
    if (currentState && currentState.runId !== state.runId) {
        selectedStepIndexForDetails = null;
    }

    // Preserve local temporary steps (isTempAdd) when applying a new state from the server
    if (currentState && currentState.blocks && state && state.blocks) {
        for (const blockName of ['before', 'steps', 'after']) {
            const oldList = currentState.blocks[blockName] || [];
            const newList = state.blocks[blockName] || [];
            const tempSteps = oldList.filter(s => s.isTempAdd);
            for (const tempStep of tempSteps) {
                if (!newList.some(s => s.index === tempStep.index)) {
                    newList.push(tempStep);
                }
            }
            state.blocks[blockName] = newList;
        }
    }

    currentState = state;
    window.currentState = currentState; // Expose to Selenium
    currentRunId = state.runId;

    // Header info
    setTextIfChanged('testNameDisplay', state.testName || '—');
    setTextIfChanged('testIdDisplay', state.testId ? `(ID: ${state.testId})` : '');

    // Set Browser Icon
    const browserLower = (state.browser || '').toLowerCase();
    const browserIcon = document.getElementById('browserIcon');
    if (browserIcon) {
        if (browserLower.includes('firefox')) {
            browserIcon.innerHTML = '<i class="fa-brands fa-firefox-browser"></i>';
        } else if (browserLower.includes('safari') && !browserLower.includes('chrome')) {
            browserIcon.innerHTML = '<i class="fa-brands fa-safari"></i>';
        } else if (browserLower.includes('edge')) {
            browserIcon.innerHTML = '<i class="fa-brands fa-edge"></i>';
        } else {
            browserIcon.innerHTML = '<i class="fa-brands fa-chrome"></i>';
        }
    }

    // Reasoning panel
    const panel = document.getElementById('reasoningPanel');
    const icon = document.getElementById('reasoningIcon');
    const text = document.getElementById('reasoningText');
    const timer = document.getElementById('reasoningTimer');
    const newReasoning = state.reasoning || '';
    if (text && text.textContent !== newReasoning) {
        text.textContent = newReasoning;
        const isFailed = !!state.reasoningFailed;
        const isThinking = !newReasoning && !isFailed;
        panel.className = 'current-reasoning-panel' +
            (isFailed ? ' failed' : '') +
            (isThinking ? ' idle' : '');
        icon.className = (isFailed
            ? 'fa-solid fa-triangle-exclamation'
            : 'fa-solid fa-brain') + ' reasoning-icon';
        // Show timer only while AI is actively thinking (has reasoning, not failed, not idle)
        if (timer) timer.style.display = (!isThinking && !isFailed) ? '' : 'none';
    }

    const beforeSteps = state.blocks?.before || [];
    const mainSteps = state.blocks?.steps || state.steps || [];
    const afterSteps = state.blocks?.after || [];

    // Update progress pill (desktop) and mobile bar.
    // currentStep = doneSteps + 1 when a step is actively running; otherwise = doneSteps.
    const allStepsForProgress = [
        ...beforeSteps,
        ...mainSteps,
        ...afterSteps
    ];
    const totalSteps = allStepsForProgress.length;
    const doneSteps = allStepsForProgress.filter(s => s.status === 'passed' || s.status === 'skipped' || s.status === 'failed' || s.status === 'aborted').length;
    const hasRunning = allStepsForProgress.some(s => s.status === 'running');
    const currentStep = doneSteps + (hasRunning ? 1 : 0);
    const pill = document.getElementById('progressPill');
    const pLabel = document.getElementById('progressLabel');
    const pFill = document.getElementById('progressBarFill');
    if (pill && totalSteps > 0) {
        pill.style.display = '';
        pLabel.textContent = `Step ${currentStep} / ${totalSteps}`;
        pFill.style.width = Math.round((currentStep / totalSteps) * 100) + '%';
    }
    // Mobile progress
    const mLabel = document.getElementById('progressLabelMobile');
    const mFill = document.getElementById('progressBarMobileFill');
    if (mLabel && totalSteps > 0) {
        mLabel.textContent = `Step ${currentStep} of ${totalSteps}`;
        mFill.style.width = Math.round((currentStep / totalSteps) * 100) + '%';
    }
    // Rebuild the flat step index list for the ↑/↓ reorder buttons
    allStepIndices = [
        ...beforeSteps,
        ...mainSteps,
        ...afterSteps
    ].map(s => s.index);

    // Test source files (Java + YAML + Playbook)
    const topTestSource = document.getElementById('topTestSource');
    if (topTestSource) {
        const files = [];
        if (state.testFile) files.push({ label: 'Java Test', value: state.testFile, icon: 'fa-file-code' });
        if (state.yamlSource) files.push({ label: 'YAML Source', value: state.yamlSource, icon: 'fa-file-lines' });
        if (state.playbookFile) files.push({ label: 'Playbook', value: state.playbookFile, icon: 'fa-file-contract' });

        if (files.length > 0) {
            const formatPath = (p) => {
                if (!p) return '';
                const parts = p.split(/[/\\]/);
                if (parts.length > 2) return '&hellip;/' + parts.slice(-2).join('/');
                return p;
            };
            topTestSource.innerHTML = files.map(f => `
                        <div style="margin-bottom:8px;" title="${escAttr(f.value)}">
                            <span style="color:var(--text-muted);font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.4px;display:block;margin-bottom:2px;">
                                <i class="fa-solid ${f.icon}" style="margin-right:4px;"></i>${f.label}
                            </span>
                            <div style="font-size:11px; color:var(--text-secondary); font-family:var(--font-mono); word-break:break-all;">${formatPath(f.value)}</div>
                        </div>
                    `).join('');
            topTestSource.style.display = '';
        } else {
            topTestSource.innerHTML = '';
            topTestSource.style.display = 'none';
        }
    }
    // Stored Variables: only show the column when state.storedVariables has entries
    const topLiveVarsCol = document.getElementById('topLiveVarsCol');
    const topLiveVarsBody = document.getElementById('topLiveVarsBody');
    const storedVars = state.storedVariables || {};
    const storedKeys = Object.keys(storedVars);
    if (topLiveVarsCol) topLiveVarsCol.style.display = (storedKeys.length > 0) ? 'block' : 'none';
    if (topLiveVarsBody) {
        if (storedKeys.length > 0) {
            topLiveVarsBody.innerHTML = storedKeys
                .map(k => `<tr style="border-bottom:1px solid rgba(255,255,255,0.03);"><td style="padding:5px 8px;font-family:var(--font-mono);font-weight:600;color:var(--text-secondary);">${escHtml(k)}</td><td style="padding:5px 8px;font-family:var(--font-mono);color:var(--text-main);">${escHtml(storedVars[k])}</td></tr>`)
                .join('');
        } else {
            topLiveVarsBody.innerHTML = '';
        }
    }

    if (isFirstLoad) {
        isAutoMode = state.autoRun || false;
    }
    syncAutoButton();

    // Render block groups
    renderBlock('before', beforeSteps);
    renderBlock('steps', mainSteps);
    renderBlock('after', afterSteps);

    // Render Data Bindings Table
    const tbody = document.getElementById('dataBindingsTableBody');
    if (tbody) {
        const bindings = state.dataBindings || {};
        const keys = Object.keys(bindings);
        if (keys.length > 0) {
            tbody.innerHTML = keys.map(k => `
                        <tr style="border-bottom: 1px solid rgba(255,255,255,0.03);">
                            <td class="binding-key" onclick="insertVarInActiveTextarea('${escAttr(k)}')" style="padding: 6px 8px; font-family: var(--font-mono); font-weight: 600; color: var(--text-secondary);">${escHtml(k)}</td>
                            <td style="padding: 6px 8px; font-family: var(--font-mono); color: var(--text-main);">${escHtml(bindings[k])}</td>
                        </tr>
                    `).join('');
        } else {
            tbody.innerHTML = `<tr><td colspan="2" style="padding: 12px; text-align: center; color: var(--text-muted);">No active variables.</td></tr>`;
        }
    }

    // JUnit tags & Test Data key-value table
    const topTags = document.getElementById('topJunitTags');
    if (topTags && state.junitTags) {
        topTags.innerHTML = state.junitTags.map(t => `<span style="background:rgba(59,130,246,0.15); color:var(--accent-primary); border:1px solid rgba(59,130,246,0.2); padding:2px 6px; border-radius:4px; font-size:10px; font-weight:700;">${escHtml(t)}</span>`).join('');
    }
    const topTestDataBody = document.getElementById('topTestDataBody');
    if (topTestDataBody && state.dataBindings) {
        const excludedKeys = ['steps', 'neodymium.stepLineNumbers', 'before', 'after', 'neodymium.classpathResourcePath', 'raw_steps', 'neodymium.testdata.index'];
        const keys = Object.keys(state.dataBindings).filter(k => !excludedKeys.includes(k));
        if (keys.length > 0) {
            topTestDataBody.innerHTML = keys.map(k => `
                        <tr style="border-bottom: 1px solid rgba(255,255,255,0.03);">
                            <td style="padding: 5px 8px; font-family: var(--font-mono); font-weight: 600; color: var(--text-secondary);">${escHtml(k)}</td>
                            <td style="padding: 5px 8px; font-family: var(--font-mono); color: var(--text-main);">${escHtml(state.dataBindings[k])}</td>
                        </tr>
                    `).join('');
        } else {
            topTestDataBody.innerHTML = `<tr><td colspan="2" style="padding: 12px; text-align: center; color: var(--text-muted);">No initial test data.</td></tr>`;
        }
    }

    restoreFocus();

    // Handle default/latest screenshot display
    let activeScreenshot = null;
    const allSteps = [
        ...beforeSteps,
        ...mainSteps,
        ...afterSteps
    ];
    // Find active/failed/running step screenshot, or last executed one
    const activeStep = allSteps.find(s => s.status === 'running' || s.status === 'failed');
    if (activeStep && activeStep.screenshot) {
        activeScreenshot = activeStep.screenshot;
    } else {
        const passedSteps = allSteps.filter(s => s.status === 'passed' && s.screenshot);
        if (passedSteps.length > 0) {
            activeScreenshot = passedSteps[passedSteps.length - 1].screenshot;
        }
    }
    if (activeScreenshot) {
        showScreenshot(activeScreenshot);
    }

    // Auto-advance: always track the currently relevant step.
    // Priority: running/failed step first, then next pending after the last passed one.
    if (state && allSteps.length > 0) {
        const steps = allSteps;
        const activeStep = steps.find(s => s.status === 'running' || s.status === 'failed');
        if (activeStep) {
            // A step is currently executing or errored – select it unconditionally
            selectedStepIndexForDetails = activeStep.index;
        } else {
            // No running step: advance to the next pending step after the last completed one
            const lastPassedIdx = steps.findLastIndex(s => s.status === 'passed');
            if (lastPassedIdx !== -1 && lastPassedIdx < steps.length - 1) {
                // Next step after the last passed one becomes the selection
                selectedStepIndexForDetails = steps[lastPassedIdx + 1].index;
            } else if (selectedStepIndexForDetails === null) {
                // Nothing passed yet – default to the first step
                selectedStepIndexForDetails = steps[0].index;
            }
            // If the user manually selected a step that is now passed,
            // jump forward to the next one automatically
            if (selectedStepIndexForDetails !== null) {
                const selStep = steps.find(s => s.index === selectedStepIndexForDetails);
                if (selStep && selStep.status === 'passed' && lastPassedIdx < steps.length - 1) {
                    selectedStepIndexForDetails = steps[lastPassedIdx + 1].index;
                }
            }
        }
    }

    if (!currentPauseId) {
        setButtonsEnabled(false);
    } else {
        setButtonsEnabled(true);
        checkActionApprovals();
    }

    // Update details block for big screens
    updateBigScreenDetails();

    // On the very first state load: open the active step's accordion on mobile
    // (on desktop the right panel handles it; on mobile the accordion is the only detail view)
    if (isFirstLoad) {
        requestAnimationFrame(() => {
            const activeCard = document.querySelector('.step-card.active');
            if (activeCard) {
                const acc = activeCard.querySelector('.step-accordion-details');
                if (acc) acc.classList.add('open');
                activeCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        });
    }
}

function checkActionApprovals() {
    if (!currentPauseId) return;
    const activeCard = document.querySelector('.step-card.active');
    if (!activeCard) return;
    const idx = parseInt(activeCard.getAttribute('data-step-idx'), 10);
    const allSteps = [
        ...(currentState?.blocks?.before || []),
        ...(currentState?.blocks?.steps || []),
        ...(currentState?.blocks?.after || [])
    ];
    const s = allSteps.find(st => st.index === idx);
    const isThinking = s && s.status === 'running' && !s.reasoning;

    const runBtn = document.getElementById('btnRun');
    if (runBtn) {
        if (isThinking || isAutoMode) {
            runBtn.disabled = true;
            runBtn.setAttribute('disabled', 'true');
        } else {
            runBtn.disabled = false;
            runBtn.removeAttribute('disabled');
        }
    }

    // Handle toolbar Back button disabled if index is 0 or auto is on
    const isFirstStep = s && s.index === 0;
    const backBtn = document.getElementById('btnBack');
    if (backBtn) {
        if (isFirstStep || isAutoMode) {
            backBtn.disabled = true;
            backBtn.setAttribute('disabled', 'true');
        } else {
            backBtn.disabled = false;
            backBtn.removeAttribute('disabled');
        }
    }
}

function renderBlock(blockName, steps) {
    const container = document.getElementById(blockName + 'Steps');
    const group = document.getElementById(blockName + 'Group');
    if (!container || !group) return;

    // Enhanced signature to include selection, breakpoints, and other UI state
    const sig = JSON.stringify({
        steps: steps.map(s => `${s.index}|${s.status}|${s.instruction}|${s.source || ''}|${s.reasoning ? s.reasoning.length : 0}|${s.actions ? s.actions.length : 0}|${s.errorMessage ? s.errorMessage.length : 0}`),
        selected: selectedStepIndexForDetails,
        breakpoints: Array.from(activeBreakpoints),
        paused: currentPauseId !== null,
        bindings: currentState?.dataBindings
    });
    if (container.dataset.sig === sig) return;
    container.dataset.sig = sig;

    if (steps.length === 0) {
        // In results/read-only mode: hide the block entirely \u2014 there are no steps
        // to display and no drag target is needed.
        // In interactive mode: show a drop zone so steps can be dragged into it.
        if (document.body.classList.contains('mode-results')) {
            group.style.display = 'none';
            return;
        }
        // Interactive mode \u2014 show empty drop zone
        container.innerHTML = `<div class="drop-target-area empty-block-zone" data-block="${blockName}" ondragover="handleDragOver(event)" ondragleave="handleDragLeave(event)" ondrop="handleDrop(event, -1, 'after', '${blockName}')">
                    <span style="opacity: 0.5; font-size: 12px;">Drop steps here</span>
                </div>`;
        group.style.display = 'flex';
        return;
    }

    group.style.display = 'flex';
    container.innerHTML = steps.map(renderStepCard).join('');

    // Only add a final zone to the 'after' block to avoid redundant targets,
    // as every card already has a zone 'before' it.
    if (blockName === 'after') {
        container.innerHTML += `<div class="drop-target-area final-drop-zone" data-block="${blockName}" ondragover="handleDragOver(event)" ondragleave="handleDragLeave(event)" ondrop="handleDrop(event, -1, 'after', '${blockName}')"></div>`;
    }
}

function buildStepDetailsHtml(step, isActiveStep) {
    const isSkipped = step.status === 'skipped';
    const isFailed = step.status === 'failed';
    const isPassed = step.status === 'passed';

    // ---- Actions: compact one-line chips ----
    const actionsList = Array.isArray(step.actions) ? step.actions : [];
    const actionsHtml = actionsList.length > 0
        ? actionsList.map((a) => {
            const parts = [];
            if (a.target) parts.push(`<span style="color:var(--text-main);font-size:12px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:260px;">${escHtml(a.target)}</span>`);
            if (a.value) parts.push(`<span style="color:var(--text-secondary);font-size:11px;">= <code style="background:rgba(255,255,255,0.06);padding:0 4px;border-radius:3px;">${escHtml(a.value)}</code></span>`);
            if (a.durationMs) parts.push(`<span style="color:var(--text-muted);font-size:10px;margin-left:auto;white-space:nowrap;"><i class="fa-solid fa-bolt" style="color:var(--accent-warning);"></i> ${(a.durationMs / 1000).toFixed(2)}s</span>`);
            return `<div class="acc-action-row">
                        <span class="acc-action-type-badge" data-type="${escAttr(a.type || 'unknown')}">${escHtml(a.type || 'unknown')}</span>
                        ${parts.join('')}
                    </div>`;
        }).join('')
        : '<div style="color:var(--text-muted); font-size:12px; padding:8px 0;">No actions recorded yet.</div>';

    const actionsLabel = isActiveStep
        ? `<div style="font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.7px;color:var(--text-secondary);margin-bottom:6px;"><i class="fa-solid fa-list-check" style="color:var(--accent-primary);"></i> Actions to perform</div>`
        : `<div style="font-size:10px;font-weight:800;text-transform:uppercase;letter-spacing:.7px;color:var(--text-secondary);margin-bottom:6px;"><i class="fa-solid fa-list-check"></i> Actions performed</div>`;

    const actionListBlock = isSkipped ? '' : `<div class="llm-action-list">${actionsLabel}${actionsHtml}</div>`;

    const errorBox = step.errorMessage ? `
                <div class="error-message-box" style="margin-bottom:12px;">
                    <span id="err-text-detail-${step.index}">${escHtml(step.errorMessage)}</span>
                    <button class="copy-btn" onclick="copyError('err-text-detail-${step.index}')" title="Copy to clipboard"><i class="fa-regular fa-copy" aria-hidden="true"></i></button>
                </div>` : '';

    // When a step fails, remind the user that individual sub-steps inside an
    // include block cannot be skipped — the entire include block must be managed
    // as a unit. Displayed for any failed step to guide the user on recovery.
    const substepWarning = isFailed
        ? `<div class="substep-warning-info" style="font-size:11px;padding:6px 10px;border-radius:6px;background:rgba(245,158,11,0.10);border:1px solid rgba(245,158,11,0.25);color:var(--accent-warning);"><i class="fa-solid fa-triangle-exclamation" aria-hidden="true"></i> Sub-steps cannot be skipped individually. Skip the parent include to skip this step.</div>`
        : '';

    // Right-column panel compact reasoning (not shown for active steps; the card has the bubble)
    const isThinking = step.status === 'running' && !step.reasoning && currentPauseId === null;
    const reasoningText = !isActiveStep && (isThinking || step.reasoning)
        ? (isThinking
            ? `<div class="acc-reasoning" style="margin-bottom:12px;"><i class="fa-solid fa-circle-notch fa-spin" style="color:var(--accent-purple)" aria-hidden="true"></i> AI is thinking...</div>`
            : `<div class="acc-reasoning" style="margin-bottom:12px; ${isFailed ? 'color:var(--accent-danger)' : ''}">
                        <i class="fa-solid ${isFailed ? 'fa-triangle-exclamation' : 'fa-brain'}" style="margin-right:8px;" aria-hidden="true"></i>${escHtml(step.reasoning)}</div>`)
        : '';

    // Timing meta — only show execution time for steps that have finished
    const thinkingTimeBlock = (step.thinkingTimeMs !== undefined && step.thinkingTimeMs > 0)
        ? `<span class="acc-timing-detail" title="Thinking duration"><i class="fa-solid fa-hourglass-half" style="color:var(--accent-purple);" aria-hidden="true"></i> Thinking: ${(step.thinkingTimeMs / 1000).toFixed(1)}s</span>`
        : '';
    const durationBlock = (isPassed || isFailed || isSkipped) && step.durationMs
        ? `<span title="Execution duration"><i class="fa-solid fa-bolt" style="color:var(--accent-warning);" aria-hidden="true"></i> ${(step.durationMs / 1000).toFixed(1)}s</span>`
        : '';
    const tokenBlock = (step.inputTokens || step.outputTokens)
        ? `<span title="Tokens (Input / Output)"><i class="fa-solid fa-calculator" style="color:var(--text-muted);" aria-hidden="true"></i> Tokens: ${step.inputTokens || 0} in / ${step.outputTokens || 0} out</span>`
        : '';
    const timingRow = (thinkingTimeBlock || durationBlock || tokenBlock)
        ? `<div style="display:flex;gap:12px;align-items:center;font-size:11px;color:var(--text-secondary);flex-wrap:wrap;justify-content:flex-end;">${thinkingTimeBlock}${durationBlock}${tokenBlock}</div>`
        : '';

    const domBlock = step.simplifiedDom
        ? `<div class="acc-dom" style="font-size:12px;"><i class="fa-solid fa-code" aria-hidden="true"></i> DOM: <a href="#" onclick="openDomOverlay(event, \`${escAttr(step.simplifiedDom)}\`)" style="color:var(--accent-primary);text-decoration:underline;">View Content</a></div>`
        : '';
    let screenshotUrl = step.screenshot;
    const effRunId = window.historyRunId || currentRunId;
    if (screenshotUrl && screenshotUrl.includes('/api/console/screenshot') && effRunId) {
        screenshotUrl += (screenshotUrl.includes('?') ? '&' : '?') + 'runId=' + encodeURIComponent(effRunId);
    }
    const screenshotBlock = screenshotUrl
        ? `<div class="acc-screenshot-block" style="margin-top:12px;">
                       <div class="screenshot-header" style="font-size:12px;"><i class="fa-solid fa-image" aria-hidden="true"></i> Screenshot: <a href="#" class="screenshot-overlay-link" onclick="openScreenshotOverlay(event,'${escAttr(screenshotUrl)}')" style="color:var(--accent-primary);text-decoration:underline;">View Fullscreen</a></div>
                       <img class="acc-screenshot" src="${escAttr(screenshotUrl)}" alt="Step screenshot"
                            onerror="handleScreenshotError(this)"
                            onclick="openScreenshotOverlay(event,'${escAttr(screenshotUrl)}')" style="cursor:pointer;margin-top:8px;max-height:200px;width:100%;object-fit:cover;border-radius:8px;">
                   </div>`
        : '';
    // Breadcrumb block (include file / chain)
    const includeChain = Array.isArray(step.includeChain) ? step.includeChain : [];
    const breadcrumbBlock = step.includeFile || (includeChain.length > 0)
        ? (() => {
            if (includeChain.length > 0) {
                const ct = includeChain.join(' > ');
                return `<div class="include-breadcrumb" title="${escAttr(ct)}"><i class="fa-solid fa-folder-open" aria-hidden="true"></i> ${escHtml(ct)}</div>`;
            }
            return `<div class="include-breadcrumb"><i class="fa-solid fa-folder-open" aria-hidden="true"></i> ${escHtml(step.includeFile)}</div>`;
        })()
        : '';

    const hasSecondary = timingRow || domBlock || screenshotBlock || breadcrumbBlock;

    if (isActiveStep) {
        // Active step: primary (actions) always open;
        // secondary (timing/DOM/screenshot/breadcrumb) behind a More details toggle.
        const secondaryHtml = hasSecondary ? `
                    <button class="step-more-details-btn" onclick="toggleStepSecondary(event, this)" aria-expanded="false">
                        <i class="fa-solid fa-chevron-right" style="font-size:9px;transition:transform 0.2s;"></i>
                        <span class="toggle-label"> More details</span>
                    </button>
                    <div class="step-secondary-details">
                        ${breadcrumbBlock}
                        ${timingRow}
                        ${domBlock}
                        ${screenshotBlock}
                    </div>` : '';

        return `<div class="step-details-wrapper" style="display:flex;flex-direction:column;gap:10px;">
                    ${actionListBlock}
                    ${secondaryHtml}
                </div>`;
    }

    // Non-active (passed / failed / skipped): full layout
    // Top header row: breadcrumb left, timings right
    const headerRow = (breadcrumbBlock || timingRow)
        ? `<div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px;flex-wrap:wrap;margin-bottom:4px;">
                       <div>${breadcrumbBlock}</div>
                       <div style="margin-left:auto;">${timingRow}</div>
                   </div>`
        : '';
    // Indent content below breadcrumb header
    const bodyIndent = headerRow ? 'padding-left:12px;border-left:2px solid rgba(255,255,255,0.07);' : '';

    return `<div class="step-details-wrapper" style="display:flex;flex-direction:column;gap:10px;">
                ${headerRow}
                <div style="${bodyIndent}display:flex;flex-direction:column;gap:10px;">
                    ${actionListBlock}
                    ${reasoningText}
                    ${errorBox}
                    ${substepWarning}
                    <div style="display:flex;gap:12px;flex-wrap:wrap;">${domBlock}</div>
                    ${screenshotBlock}
                </div>
            </div>`;
}

function generateEditableBindingsTable(bindings, textareaIdOrClass) {
    const excludedKeys = ['steps', 'neodymium.stepLineNumbers', 'before', 'after', 'neodymium.classpathResourcePath', 'raw_steps','neodymium.testdata.index'];
    const keys = Object.keys(bindings || {}).filter(k => !excludedKeys.includes(k));
    if (keys.length === 0) {
        return '<div style="font-size: 12px; color: var(--text-muted); text-align: center; padding: 8px;">No variables available.</div>';
    }

    return `
                <div style="font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--text-secondary); margin-bottom: 6px;">Insert/Edit Variables:</div>
                <table style="width: 100%; border-collapse: collapse; font-size: 12px; border: 1px solid var(--border-color); border-radius: 6px; overflow: hidden; background: rgba(0,0,0,0.15);">
                    <thead>
                        <tr style="border-bottom: 1px solid var(--border-color); background: rgba(255,255,255,0.02); text-align: left;">
                            <th style="padding: 6px 8px; color: var(--text-secondary); font-weight: 600;">Variable Name (click to insert)</th>
                            <th style="padding: 6px 8px; color: var(--text-secondary); font-weight: 600; width: 60%;">Value</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${keys.map(k => `
                            <tr style="border-bottom: 1px solid rgba(255,255,255,0.03);">
                                <td class="editable-binding-key binding-badge" onclick="insertVarAtTarget('${escAttr(k)}', '${escAttr(textareaIdOrClass)}')" style="cursor: pointer; padding: 6px 8px; font-family: var(--font-mono); font-weight: 600; color: var(--accent-primary);" title="Click to insert \${${escHtml(k)}}">${escHtml(k)}</td>
                                <td style="padding: 4px 8px;">
                                    <input type="text" value="${escAttr(bindings[k] || '')}" oninput="updateDataBinding('${escAttr(k)}', this.value)" style="width: 100%; background: rgba(0,0,0,0.25); border: 1px solid var(--border-color); color: var(--text-main); padding: 4px 8px; border-radius: 4px; font-family: var(--font-mono); font-size: 12px;" aria-label="Value for variable ${escHtml(k)}" />
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
}

function updateDataBinding(key, val) {
    if (currentState) {
        if (!currentState.dataBindings) {
            currentState.dataBindings = {};
        }
        currentState.dataBindings[key] = val;

        // Also update top test data body if visible
        const topTestDataBody = document.getElementById('topTestDataBody');
        if (topTestDataBody) {
            const excludedKeys = ['steps', 'neodymium.stepLineNumbers', 'before', 'after', 'neodymium.classpathResourcePath', 'raw_steps', 'neodymium.testdata.index'];
            const keys = Object.keys(currentState.dataBindings).filter(k => !excludedKeys.includes(k));
            topTestDataBody.innerHTML = keys.map(k => `
                        <tr style="border-bottom: 1px solid rgba(255,255,255,0.03);">
                            <td style="padding: 6px 8px; font-family: var(--font-mono); font-weight: 600; color: var(--text-secondary);">${escHtml(k)}</td>
                            <td style="padding: 6px 8px; font-family: var(--font-mono); color: var(--text-main);">${escHtml(currentState.dataBindings[k])}</td>
                        </tr>
                    `).join('');
        }
    }
}

function insertVarAtTarget(varName, targetSelector) {
    let textarea = null;
    if (targetSelector.startsWith('#')) {
        textarea = document.getElementById(targetSelector.substring(1));
    } else {
        const activeEditCard = document.querySelector('.step-card.editing');
        if (activeEditCard) {
            textarea = activeEditCard.querySelector(targetSelector);
        }
    }
    if (textarea) {
        insertAtCursor(textarea, `\${${varName}}`);
    }
}

function resolveVariables(text, bindings) {
    if (!text || !bindings) return text || '';
    let result = text;
    for (const key in bindings) {
        const placeholder = `\${${key}}`;
        result = result.replaceAll(placeholder, bindings[key]);
    }
    return result;
}

function renderStepCard(step) {
    if (!step) return '';
    const status = step.status || 'pending';
    const isSkipped = status === 'skipped';
    const isPassed = status === 'passed';
    const isActive = status === 'running' || status === 'failed';
    const isFailed = status === 'failed';
    const isEditing = window.currentlyEditingStepIndex === step.index;

    const includeLevel = typeof step.includeLevel === 'number' ? step.includeLevel : 0;
    const lvlClass = includeLevel > 0 ? ` include-lvl-${Math.min(includeLevel, 4)}` : '';
    const skippedClass = isSkipped ? ' skipped' : '';
    const passedClass = isPassed ? ' passed-step' : '';
    const editingClass = isEditing ? ' editing' : '';
    const isAborted = status === 'aborted';
    const abortedClass = isAborted ? ' aborted' : '';

    // Determine whether this step has any displayable information.
    // Used both to conditionally render the accordion and to guard against
    // selecting future steps that have nothing to show yet.
    const actionsList = Array.isArray(step.actions) ? step.actions : [];
    const hasDetails = (actionsList.length > 0)
        || step.errorMessage
        || step.durationMs
        || step.screenshot
        || step.thinkingTimeMs
        || step.simplifiedDom
        || step.reasoning;
    const isPendingNoDetails = (status === 'pending') && !hasDetails;
    const noDetailsPendingClass = isPendingNoDetails ? ' no-details-pending' : '';

    const isSelected = step.index == selectedStepIndexForDetails;
    const selectedClass = isSelected ? ' selected-details' : '';
    const activeClass = isActive ? (isFailed ? ' active failed' : ' active') : '';

    const bpActive = activeBreakpoints.has(step.index);
    let statusIconMarkup = '';

    if (step.status === 'passed') {
        statusIconMarkup = `<i class="fa-solid fa-circle-check" style="color:var(--accent-success)" aria-label="Passed"></i>`;
    } else if (step.status === 'failed') {
        statusIconMarkup = `<i class="fa-solid fa-circle-xmark" style="color:var(--accent-danger)" aria-label="Failed"></i>`;
    } else if (step.status === 'skipped') {
        statusIconMarkup = `<i class="fa-solid fa-circle-minus" style="color:var(--text-secondary)" aria-label="Skipped"></i>`;
    } else if (step.status === 'aborted') {
        statusIconMarkup = `<i class="fa-solid fa-circle-stop" style="color:var(--text-muted)" aria-label="Aborted"></i>`;
    } else if (step.status === 'running' && currentPauseId === null) {
        statusIconMarkup = `<i class="fa-solid fa-circle-notch fa-spin" style="color:var(--accent-primary)" aria-label="Running"></i>`;
    } else if (step.status === 'running') {
        statusIconMarkup = `<i class="fa-solid fa-circle" style="color:var(--accent-primary)" aria-label="Paused"></i>`;
    } else {
        // Pending, or Running but Paused/waiting for input
        if (bpActive) {
            const bpColor = 'var(--accent-danger)';
            statusIconMarkup = `
                        <svg class="bp-svg active" viewBox="0 0 100 100" style="fill: ${bpColor};" aria-label="Breakpoint active" title="Breakpoint active (Click to remove)">
                            <polygon points="30,5 70,5 95,30 95,70 70,95 30,95 5,70 5,30" />
                        </svg>
                    `;
        } else {
            statusIconMarkup = `
                        <i class="fa-regular fa-circle pending-placeholder-icon" style="color:var(--text-muted)" aria-label="Step pending" title="Step Pending"></i>
                        <svg class="bp-svg hover-only" viewBox="0 0 100 100" aria-label="Toggle breakpoint" title="Toggle Breakpoint">
                            <polygon points="30,5 70,5 95,30 95,70 70,95 30,95 5,70 5,30" />
                        </svg>
                    `;
        }
    }

    // Playbook steps: no type-tag in the card (shown as a note in reasoning area instead)
    const isPlaybook = step.source === 'playbook';
    const sourceTag = isPlaybook
        ? ''
        : step.source === 'healed'
            ? `<div class="type-tag type-healed"><i class="fa-solid fa-heart-pulse" aria-hidden="true"></i> Healed</div>`
            : `<div class="type-tag type-llm"><i class="fa-solid fa-robot" aria-hidden="true"></i> LLM Agent</div>`;

    // Thinking-time badge — always visible in the step card header so that the
    // CSS rule hiding accordions on wide screens does not obscure it.
    const thinkingBadge = (step.thinkingTimeMs !== undefined && step.thinkingTimeMs > 0)
        ? `<span class="acc-thinking-time" title="AI thinking time" style="font-size:10px;padding:1px 6px;border-radius:4px;background:rgba(139,92,246,0.12);color:var(--accent-purple);margin-left:4px;"><i class="fa-solid fa-hourglass-half" aria-hidden="true"></i> Thinking: ${(step.thinkingTimeMs / 1000).toFixed(1)}s</span>`
        : '';

    // Build breadcrumb string but do NOT put it in the tag-row;
    // it will appear in the secondary details section.
    let includeBreadcrumb = '';
    const includeChain = Array.isArray(step.includeChain) ? step.includeChain : [];
    if (includeChain.length > 0) {
        const chainText = includeChain.join(' > ');
        includeBreadcrumb = `<div class="include-breadcrumb" title="${escAttr(chainText)}"><i class="fa-solid fa-folder-open" aria-hidden="true"></i> ${escHtml(chainText)}</div>`;
    } else if (step.includeFile) {
        includeBreadcrumb = `<div class="include-breadcrumb"><i class="fa-solid fa-folder-open" aria-hidden="true"></i> ${escHtml(step.includeFile)}</div>`;
    }

    const isPausedRunning = step.status === 'running' && currentPauseId !== null;
    const isPendingOrPausedRunning = !step.status || step.status === 'pending' || isPausedRunning;
    // hasDetails is declared above (needed for noDetailsPendingClass) — do not redeclare here.

    const showEdit = step.status !== 'passed' && step.status !== 'skipped';
    const editBtnHtml = showEdit
        ? `<button class="step-edit-btn" onclick="enableEdit(this)" title="Edit Step (Alt+E)" aria-label="Edit step instruction (Alt+E)"><i class="fa-solid fa-pencil" aria-hidden="true"></i></button>`
        : '';

    // Rewind only makes sense for steps that have already been executed (passed/failed/skipped).
    // Showing it on future/active steps is confusing and not actionable.
    const rewindBtnHtml = (isPassed || isFailed || isSkipped) && !showEdit
        ? `<button class="step-rewind-btn" onclick="triggerRewind(event, ${step.index})" title="Rewind execution back here" aria-label="Rewind execution back here"><i class="fa-solid fa-rotate-left" aria-hidden="true"></i></button>`
        : '';

    // Reorder system overhaul: Drag handle only in controls
    const showReorder = step.status !== 'passed' && step.status !== 'skipped';
    let reorderHandleHtml = '';
    if (showReorder) {
        reorderHandleHtml = `
                    <div class="step-reorder-controls">
                        <div class="step-reorder-handle" draggable="true"
                             ondragstart="handleDragStart(event, ${step.index})"
                             ondragend="handleDragEnd(event)"
                             title="Drag handle to reorder step" aria-label="Drag handle to reorder step">
                             <i class="fa-solid fa-grip"></i>
                        </div>
                    </div>`;
    }

    const statusIconContainerClass = isPendingOrPausedRunning ? 'step-status-icon pending-step' : 'step-status-icon';
    const statusClickAttr = isPendingOrPausedRunning ? `onclick="toggleBp(event, ${step.index})"` : '';

    const detailsHtml = hasDetails ? buildStepDetailsHtml(step, isActive) : '';
    const accordionOpen = isActive ? ' open' : '';

    const selectedArrowMarkup = isSelected ? `<div class="selected-arrow big-screen-only" style="margin-left: 8px; color: var(--accent-purple); display: flex; align-items: center;"><i class="fa-solid fa-chevron-right"></i></div>` : '';

    const resolvedInstruction = resolveVariables(step.instruction, currentState?.dataBindings);

    // Build inline reasoning / playbook note for active step
    let inlineReasoningHtml = '';
    if (isActive) {
        if (isPlaybook) {
            // Playbook steps: show a note instead of AI reasoning
            inlineReasoningHtml = `<div class="inline-reasoning-bubble" style="background:rgba(99,102,241,0.07);border-left-color:rgba(99,102,241,0.5);">
                        <div class="inline-reasoning-label" style="color:#818cf8;"><i class="fa-solid fa-compact-disc" aria-hidden="true"></i> From Playbook</div>
                        <div class="reasoning-body">
                            <i class="fa-solid fa-book-open" style="color:#818cf8;" aria-hidden="true"></i>
                            <span style="color:var(--text-secondary);">This step is defined in a playbook and will be executed as recorded.</span>
                        </div>
                    </div>`;
        } else {
            const isThinking = step.status === 'running' && !step.reasoning && currentPauseId === null;
            if (isThinking) {
                inlineReasoningHtml = `<div class="inline-reasoning-bubble inline-reasoning-thinking">
                            <div class="inline-reasoning-label"><i class="fa-solid fa-brain" aria-hidden="true"></i> AI Thinking</div>
                            <div class="reasoning-body">
                                <i class="fa-solid fa-circle-notch fa-spin" aria-hidden="true"></i>
                                <span>The AI is analyzing the current page state, reading visible elements, and deciding what action to take next. This may take a few seconds depending on page complexity and model response time.</span>
                            </div>
                        </div>`;
            } else if (step.reasoning) {
                const reasoningId = `reasoning-${step.index}`;
                const escaped = escHtml(step.reasoning);
                const isLong = step.reasoning.length > 220;
                const preview = isLong ? escHtml(step.reasoning.substring(0, 220)) + '…' : escaped;
                inlineReasoningHtml = `<div class="inline-reasoning-bubble${isFailed ? ' failed' : ''}">
                            <div class="inline-reasoning-label"><i class="fa-solid ${isFailed ? 'fa-triangle-exclamation' : 'fa-brain'}" aria-hidden="true"></i> ${isFailed ? 'Failure Reason' : 'AI Reasoning'}</div>
                            <div class="reasoning-body">
                                <i class="fa-solid ${isFailed ? 'fa-triangle-exclamation' : 'fa-quote-left'}" aria-hidden="true"></i>
                                <span>
                                    <span id="${reasoningId}-preview">${preview}</span>
                                    ${isLong ? `<span id="${reasoningId}-full" style="display:none;">${escaped}</span>
                                    <button onclick="toggleTextExpand(event,'${reasoningId}')" class="step-more-details-btn" style="margin-top:4px;">
                                        <i class="fa-solid fa-chevron-right" style="font-size:9px;"></i> Show more
                                    </button>` : ''}
                                </span>
                            </div>
                        </div>`;
            }
        }
    }

    // "▶ CURRENT" / "✕ FAILED" badge for the active step
    const activeBadge = isActive
        ? `<div class="active-step-badge">${isFailed ? '<i class="fa-solid fa-xmark"></i> FAILED' : '<i class="fa-solid fa-play"></i> CURRENT'}</div>`
        : '';

    // Truncate long step instructions in the card; user can expand.
    const STEP_TRUNCATE = 140;
    const instrFull = escHtml(resolvedInstruction);
    const isInstrLong = resolvedInstruction.length > STEP_TRUNCATE;
    const instrPreview = isInstrLong ? escHtml(resolvedInstruction.substring(0, STEP_TRUNCATE)) + '…' : instrFull;
    const instrId = `instr-${step.index}`;
    const stepTextContent = isInstrLong
        ? `<span id="${instrId}-preview">${instrPreview}</span><span id="${instrId}-full" style="display:none;">${instrFull}</span>
                   <button onclick="toggleTextExpand(event,'${instrId}')" class="step-more-details-btn" style="margin-top:4px;padding:2px 7px;font-size:10px;">
                       <i class="fa-solid fa-chevron-right" style="font-size:8px;"></i> Show more
                   </button>`
        : instrFull;

    // Standalone variable for step number badge to avoid nesting template literals deeply
    let stepNumBadgeHtml = '';
    if (!step.isTempAdd) {
        const badgeNum = allStepIndices.indexOf(step.index) + 1;
        stepNumBadgeHtml = `
                    <div class="step-num-badge">
                        ${isSelected ? `
                            <input type="number" value="${badgeNum}"
                                   min="1" max="${allStepIndices.length}"
                                   onkeydown="handleStepInputKeyDown(event, ${step.index})"
                                   onblur="reorderStepDirect(event, ${step.index})"
                                   aria-label="Step position. Type a new number to move this step. Use Ctrl + Up/Down arrows to move.">
                        ` : `#${badgeNum}`}
                    </div>`;
    }

    return `
                <div class="drop-target-area" ondragover="handleDragOver(event)" ondragleave="handleDragLeave(event)" ondrop="handleDrop(event, ${step.index}, 'before')"></div>
                <div class="step-card${activeClass}${selectedClass}${lvlClass}${skippedClass}${passedClass}${noDetailsPendingClass}${editingClass}${abortedClass}" data-step-idx="${step.index}" role="listitem" tabindex="0" aria-label="${step.isTempAdd ? 'New Step' : 'Step ' + (allStepIndices.indexOf(step.index) + 1)}: ${escHtml(resolvedInstruction)}"
                     onclick="handleStepClick(event, ${step.index})" ondragend="handleDragEnd(event)">
                ${activeBadge}
                <div class="step-row">
                    ${stepNumBadgeHtml}
                    <div class="${statusIconContainerClass}" ${statusClickAttr}>${statusIconMarkup}</div>
                    <div class="step-text-container">
                        <div class="tag-row">${sourceTag}${thinkingBadge}</div>
                        <div class="step-text">${stepTextContent}</div>
                        <div class="step-edit-form">
                            <textarea class="inline-edit-textarea" aria-label="Edit step instruction text">${isEditing && window.currentEditText != null ? escHtml(window.currentEditText) : escHtml(step.rawInstruction || step.instruction)}</textarea>
                            <div class="inline-edit-actions">
                                <button class="btn" style="padding: 4px 10px; font-size: 11px;" onclick="cancelEdit(this)" aria-label="Cancel editing step">Cancel</button>
                                <button class="btn btn-primary step-save-btn" style="padding: 4px 10px; font-size: 11px;" onclick="saveEdit(this)" aria-label="Save step instruction">Save</button>
                            </div>
                        </div>
                        <div class="inline-edit-bindings" style="${isEditing ? 'display:flex; flex-direction:column; gap:6px; margin:8px 0;' : 'display:none;'}">${generateEditableBindingsTable(currentState?.dataBindings, '.inline-edit-textarea')}</div>
                    </div>
                    ${rewindBtnHtml}
                    ${reorderHandleHtml}
                    ${editBtnHtml}
                    ${selectedArrowMarkup}
                </div>
                ${inlineReasoningHtml ? `<div style="padding-top:4px;">${inlineReasoningHtml}</div>` : ''}
                ${hasDetails ? `<div class="step-accordion-details${accordionOpen}">${detailsHtml}</div>` : ''}
            </div>`;
}

// -------------------------------------------------------------------------
// Interactive Actions
// -------------------------------------------------------------------------
function toggleStepSecondary(event, btn) {
    event.stopPropagation();
    const secondary = btn.nextElementSibling;
    if (!secondary) return;
    const isOpen = secondary.classList.toggle('open');
    btn.classList.toggle('open', isOpen);
    btn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
    const chevron = btn.querySelector('i');
    if (chevron) chevron.style.transform = isOpen ? 'rotate(90deg)' : '';
    // Use the named span for safe text toggle (avoids whitespace text node issues)
    const label = btn.querySelector('.toggle-label');
    if (label) label.textContent = isOpen ? ' Less details' : ' More details';
}

function toggleTextExpand(event, id) {
    event.stopPropagation();
    const preview = document.getElementById(id + '-preview');
    const full = document.getElementById(id + '-full');
    const btn = event.currentTarget;
    if (!preview || !full) return;
    const expanding = full.style.display === 'none';
    preview.style.display = expanding ? 'none' : '';
    full.style.display = expanding ? '' : 'none';
    const chevron = btn.querySelector('i');
    if (chevron) chevron.style.transform = expanding ? 'rotate(90deg)' : '';
    const textNode = [...btn.childNodes].find(n => n.nodeType === Node.TEXT_NODE);
    if (textNode) textNode.textContent = expanding ? ' Show less' : ' Show more';
}

function sendAction(action, extra) {
    if (!currentPauseId || !currentRunId) return;
    const finalSaveOverlay = document.getElementById('finalSaveOverlay');
    if (finalSaveOverlay) finalSaveOverlay.classList.remove('active');
    const payload = { runId: currentRunId, pauseId: currentPauseId, action, ...extra };
    setButtonsEnabled(false);
    fetch('/api/console/action', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    }).then(r => r.json()).then(res => {
        if (res.error === 'stale-tab') {
            showStaleBanner(res.activeRunId);
        }
    }).catch(() => {
        setButtonsEnabled(true);
    });
    currentPauseId = null;
    window.currentPauseId = null;
    // Keep currentState in sync so local re-renders see no active pause token.
    if (currentState) {
        currentState.pauseId = null;
    }
}

/**
 * Triggers a debug dump of the current AI context.
 *
 * This sends the DUMP action through the normal action channel. The backend
 * handler catches DUMP, performs the dump (screenshot + DOM + step history),
 * then recursively re-pauses – so the test stays paused and the user can
 * still click Run or Skip after the dump completes.
 */
function triggerDump() {
    closeKebabMenu();
    if (!currentPauseId || !currentRunId) return;
    // DUMP goes through sendAction; the backend re-pauses immediately after.
    sendAction('DUMP');
    // Briefly restore the buttons since the backend will re-pause and send a
    // new pauseId via SSE, which will re-enable them.
    setTimeout(() => setButtonsEnabled(true), 800);
}

/**
 * Toggles the kebab debug dropdown open or closed.
 * Closes automatically when the user clicks anywhere outside.
 */
function toggleKebabMenu(event) {
    event.stopPropagation();
    const dropdown = document.getElementById('kebabDropdown');
    const btn = document.getElementById('btnKebab');
    if (!dropdown) return;
    const isOpen = dropdown.classList.contains('open');
    if (isOpen) {
        closeKebabMenu();
    } else {
        dropdown.classList.add('open');
        if (btn) btn.setAttribute('aria-expanded', 'true');
        // Close on outside click
        document.addEventListener('click', closeKebabMenu, { once: true });
    }
}

function closeKebabMenu() {
    const dropdown = document.getElementById('kebabDropdown');
    const btn = document.getElementById('btnKebab');
    if (dropdown) dropdown.classList.remove('open');
    if (btn) btn.setAttribute('aria-expanded', 'false');
}

/**
 * Formats a byte count into a human-readable string (e.g. "12.4 KB").
 *
 * @param {number} bytes - the byte count
 * @returns {string} formatted string
 */
function formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

/**
 * Populates and shows the dumpReady popup overlay.
 *
 * Called automatically when the backend fires the {@code dumpReady} SSE event
 * after {@link AiAgent#performDebugDump()} completes. The popup lists both
 * captured files with their absolute paths and byte sizes.
 *
 * @param {{ txtFile: string, htmlFile: string, txtSize: number, htmlSize: number }} data
 */
function showDumpPopup(data) {
    const overlay = document.getElementById('dumpReadyOverlay');
    const container = document.getElementById('dumpReadyFiles');
    if (!overlay || !container) return;

    container.innerHTML = `
        <div class="dump-file-entry">
            <div class="dump-file-label">
                <span class="dump-file-type-badge txt">TXT</span>
                <span class="dump-file-desc">AI context &amp; step history — what the LLM sees</span>
            </div>
            <div class="dump-file-path">
                <span>${escapeHtml(data.txtFile)}</span>
                <span class="dump-file-size">${formatBytes(data.txtSize)}</span>
            </div>
        </div>
        <div class="dump-file-entry">
            <div class="dump-file-label">
                <span class="dump-file-type-badge html">HTML</span>
                <span class="dump-file-desc">Raw DOM snapshot — main page &amp; all iframes</span>
            </div>
            <div class="dump-file-path">
                <span>${escapeHtml(data.htmlFile)}</span>
                <span class="dump-file-size">${formatBytes(data.htmlSize)}</span>
            </div>
        </div>
    `;

    overlay.classList.add('active');
    // Dismiss on Escape
    const onKey = (ev) => {
        if (ev.key === 'Escape') {
            closeDumpPopup();
            document.removeEventListener('keydown', onKey);
        }
    };
    document.addEventListener('keydown', onKey);
}

function closeDumpPopup() {
    const overlay = document.getElementById('dumpReadyOverlay');
    if (overlay) overlay.classList.remove('active');
}

/** Escapes special HTML characters to prevent XSS in file paths. */
function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function toggleAuto() {
    if (isAutoMode) {
        isAutoMode = false;
        syncAutoButton();
        autoStartStepIndex = null;
        setButtonsEnabled(true);
    } else {
        // Arm auto-mode immediately even if the pause token hasn't arrived yet.
        // triggerAutoRunCheck guards on currentPauseId internally; if the pause
        // event arrives later it will call triggerAutoRunCheck() and fire then.
        isAutoMode = true;
        syncAutoButton();
        setButtonsEnabled(true);
        const allSteps = [
            ...(currentState?.blocks?.before || []),
            ...(currentState?.blocks?.steps || []),
            ...(currentState?.blocks?.after || [])
        ];
        const activeRunningStep = allSteps.find(s => s.status === 'running');
        autoStartStepIndex = activeRunningStep ? activeRunningStep.index : null;
        triggerAutoRunCheck();
    }
}

function triggerAutoRunCheck() {
    if (!isAutoMode || !currentPauseId) return;
    const allSteps = [
        ...(currentState?.blocks?.before || []),
        ...(currentState?.blocks?.steps || []),
        ...(currentState?.blocks?.after || [])
    ];

    const hasFailedStep = allSteps.some(s => s.status === 'failed' || s.errorMessage);
    const activeRunningStep = allSteps.find(s => s.status === 'running');

    const hitBreakpoint = activeRunningStep &&
        activeBreakpoints.has(activeRunningStep.index) &&
        activeRunningStep.index !== autoStartStepIndex;

    if (hasFailedStep || hitBreakpoint) {
        isAutoMode = false;
        autoStartStepIndex = null;
        syncAutoButton();
        setButtonsEnabled(true);
        return;
    }

    setTimeout(() => {
        if (isAutoMode && currentPauseId) {
            sendAction('RUN');
        }
    }, 500);
}

// -------------------------------------------------------------------------
// Event Handlers & Local UI helpers
// -------------------------------------------------------------------------
function handleStepClick(event, index) {
    // Prevent toggling if clicking interactive controls, inputs, variable bindings, or the breakpoint status icon
    if (event.target.closest('button') || event.target.closest('textarea') || event.target.closest('input') || event.target.closest('.step-status-icon') || event.target.closest('.inline-edit-bindings')) return;

    // Allow selecting any step, including future ones.
    // Selection is now the primary trigger for reorder editing.
    if (currentState) {
        const allSteps = [
            ...(currentState.blocks?.before || []),
            ...(currentState.blocks?.steps || []),
            ...(currentState.blocks?.after || [])
        ];
        const clickedStep = allSteps.find(s => s.index === index);
        // No more blocking of pending steps selection
    }

    // Single click always selects – no toggle-off; a step stays selected once clicked
    selectedStepIndexForDetails = index;
    lastFocusedStepIndex = index; // Store for restoreFocus()

    if (currentState) {
        renderBlock('before', currentState.blocks?.before || []);
        renderBlock('steps', currentState.blocks?.steps || []);
        renderBlock('after', currentState.blocks?.after || []);
    }
    updateBigScreenDetails();

    if (window.parent && window.parent !== window) {
        window.parent.postMessage({ action: 'stepSelected' }, '*');
    }

    // Focus the input field (if rendered for the selected step)
    restoreFocus();

    // On mobile: open the accordion immediately on the first click
    if (window.innerWidth <= 900) {
        const card = document.querySelector(`.step-card[data-step-idx='${index}']`);
        if (card) {
            const acc = card.querySelector('.step-accordion-details');
            if (acc && !acc.classList.contains('open')) acc.classList.add('open');
        }
    }
}

function updateBigScreenDetails() {
    const container = document.getElementById('bigScreenStepDetails');
    const content = document.getElementById('bigScreenDetailsContent');
    if (!container || !content) return;

    if (currentState) {
        const allSteps = [
            ...(currentState.blocks?.before || []),
            ...(currentState.blocks?.steps || []),
            ...(currentState.blocks?.after || [])
        ];
        const activeStep = allSteps.find(s => s.status === 'running' || s.status === 'failed');
        if (selectedStepIndexForDetails === null && activeStep) {
            selectedStepIndexForDetails = activeStep.index;
        }
        // Don't auto-switch if user has manually selected a step unless the selected step is gone
        if (selectedStepIndexForDetails !== null) {
            const selStepExists = allSteps.some(s => s.index === selectedStepIndexForDetails);
            if (!selStepExists && activeStep) {
                selectedStepIndexForDetails = activeStep.index;
            }
        }
    }

    if (selectedStepIndexForDetails === null || !currentState) {
        container.style.display = 'none';
        return;
    }

    const allSteps = [
        ...(currentState.blocks?.before || []),
        ...(currentState.blocks?.steps || []),
        ...(currentState.blocks?.after || [])
    ];
    const step = allSteps.find(s => s.index === selectedStepIndexForDetails);
    if (!step) {
        container.style.display = 'none';
        return;
    }

    const sourceTag = step.source === 'playbook'
        ? `<div class="type-tag type-playbook"><i class="fa-solid fa-compact-disc" aria-hidden="true"></i> Playbook</div>`
        : step.source === 'healed'
            ? `<div class="type-tag type-healed"><i class="fa-solid fa-heart-pulse" aria-hidden="true"></i> Healed</div>`
            : `<div class="type-tag type-llm"><i class="fa-solid fa-robot" aria-hidden="true"></i> LLM Agent</div>`;

    let includeBreadcrumb = '';
    if (step.includeChain && step.includeChain.length > 0) {
        const chainText = step.includeChain.join(' > ');
        includeBreadcrumb = `<div class="include-breadcrumb" title="${escAttr(chainText)}"><i class="fa-solid fa-folder-open" aria-hidden="true"></i> ${escHtml(chainText)}</div>`;
    } else if (step.includeFile) {
        includeBreadcrumb = `<div class="include-breadcrumb"><i class="fa-solid fa-folder-open" aria-hidden="true"></i> ${escHtml(step.includeFile)}</div>`;
    }

    const detailsHtml = buildStepDetailsHtml(step);

    content.innerHTML = `
                <div style="font-weight: 600; font-size:14px; margin-bottom: 8px;">Step #${step.index}: ${escHtml(step.instruction)}</div>
                <div style="margin-bottom:12px; display:flex; gap:6px; flex-wrap:wrap;">${sourceTag}${includeBreadcrumb}</div>
                ${detailsHtml}
            `;
    container.style.display = 'flex';
}

function triggerRewind(event, index) {
    if (event) event.stopPropagation();
    if (!currentPauseId) return;
    selectedStepIndexForDetails = index;
    sendAction('REWIND', { targetIndex: index });
}

function pasteBinding(el, key) {
    const card = el.closest('.step-card');
    const textarea = card ? card.querySelector('.inline-edit-textarea') : document.getElementById('newStepInstruction');
    if (!textarea) return;
    const val = `\${${key}}`;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    textarea.value = textarea.value.substring(0, start) + val + textarea.value.substring(end);
    textarea.focus();
    textarea.setSelectionRange(start + val.length, start + val.length);
}

function enableEdit(btnElement) {
    const card = btnElement.closest('.step-card');
    card.classList.add('editing');

    // Explicitly show the bindings panel (which is a sibling of .step-edit-form,
    // not inside it). This makes .binding-badge elements visible to WebDriver
    // getText() in headless mode, independent of any CSS class cascade.
    const bindingsDiv = card.querySelector('.inline-edit-bindings');
    if (bindingsDiv) {
        bindingsDiv.style.display = 'flex';
        bindingsDiv.style.flexDirection = 'column';
        bindingsDiv.style.gap = '6px';
        bindingsDiv.style.margin = '8px 0';
        if (currentState?.dataBindings) {
            // Re-populate in case bindings changed since the card was last rendered.
            bindingsDiv.innerHTML = generateEditableBindingsTable(
                currentState.dataBindings,
                '.inline-edit-textarea'
            );
        }
    }

    const textarea = card.querySelector('.inline-edit-textarea');
    const idx = parseInt(card.getAttribute('data-step-idx'), 10);

    let rawInstruction = '';
    if (currentState && currentState.blocks) {
        for (const blockName of ['before', 'steps', 'after']) {
            const stepObj = currentState.blocks[blockName]?.find(s => s.index === idx);
            if (stepObj) {
                rawInstruction = stepObj.rawInstruction || stepObj.instruction;
                break;
            }
        }
    }
    textarea.value = rawInstruction;
    textarea.focus();

    const acc = card.querySelector('.step-accordion-details');
    if (acc && !acc.classList.contains('open')) acc.classList.add('open');
    setButtonsEnabled(true);
}

function cancelEdit(btnElement) {
    const card = btnElement.closest('.step-card');
    const idx = parseInt(card.getAttribute('data-step-idx'), 10);
    let removed = false;
    if (currentState && currentState.blocks) {
        for (const blockName of ['before', 'steps', 'after']) {
            const list = currentState.blocks[blockName];
            if (list) {
                const indexInList = list.findIndex(s => s.index === idx && s.isTempAdd);
                if (indexInList !== -1) {
                    list.splice(indexInList, 1);
                    removed = true;
                    break;
                }
            }
        }
    }
    if (removed) {
        applyState(currentState);
    } else {
        card.classList.remove('editing');
        // Hide the bindings panel that was shown by enableEdit.
        const bindingsDiv = card.querySelector('.inline-edit-bindings');
        if (bindingsDiv) bindingsDiv.style.display = 'none';
        setButtonsEnabled(true);
    }
}

function saveEdit(btnElement) {
    console.error('saveEdit called');
    const card = btnElement.closest('.step-card');
    const textarea = card.querySelector('.inline-edit-textarea');
    const newVal = textarea.value.trim();

    const idx = parseInt(card.getAttribute('data-step-idx'), 10);
    let isTemp = false;
    let oldVal = '';
    let blockName = 'steps';

    if (currentState && currentState.blocks) {
        for (const bKey of ['before', 'steps', 'after']) {
            const list = currentState.blocks[bKey];
            if (list) {
                const found = list.find(s => s.index === idx);
                if (found) {
                    isTemp = !!found.isTempAdd;
                    oldVal = found.instruction;
                    blockName = bKey;
                    break;
                }
            }
        }
    }

    if (isTemp) {
        if (newVal && currentPauseId) {
            sendAction('ADD', { instruction: newVal, block: blockName });
        } else {
            cancelEdit(btnElement);
        }
        card.classList.remove('editing');
    } else {
        console.error('Found step in state blocks:', blockName, idx, 'oldVal:', oldVal, 'newVal:', newVal, 'currentPauseId:', currentPauseId);
        // We always want to send an EDIT action if the user clicks Save, even if the text didn't change, 
        // because the user might have changed dataBindings.
        if (currentPauseId) {
            console.error('Sending EDIT action');
            sendAction('EDIT', { index: idx, instruction: newVal, bindings: currentState?.dataBindings });
        }
        card.classList.remove('editing');
        setButtonsEnabled(true);
    }
}

function toggleBp(event, index) {
    event.stopPropagation();
    if (activeBreakpoints.has(index)) {
        activeBreakpoints.delete(index);
    } else {
        activeBreakpoints.add(index);
    }
    applyState(currentState);
}

function copyError(elementId) {
    const text = document.getElementById(elementId).innerText;
    navigator.clipboard.writeText(text).then(() => {
        alert("Error message copied to clipboard!");
    });
}

function showScreenshot(src) {
    // No-op fallback since DOM Snapshot card is removed. Lightbox handles clicks.
}

function setButtonsEnabled(enabled) {
    const runBtn = document.getElementById('btnRun');
    const skipBtn = document.getElementById('btnSkip');
    const backBtn = document.getElementById('btnBack');
    const autoBtn = document.getElementById('btnAuto');
    const btnCancel = document.getElementById('btnCancel');
    const isEditing = document.querySelector('.step-card.editing') !== null;

    // Disable Drag/Drop if editing
    const blocks = document.querySelectorAll('.steps-block-content');
    blocks.forEach(block => {
        if (isEditing) {
            block.classList.add('drag-disabled');
        } else {
            block.classList.remove('drag-disabled');
        }
    });

    // Control visibility of Add Step buttons
    const addStepBtns = document.querySelectorAll('.add-step-card-btn');
    addStepBtns.forEach(btn => {
        btn.style.display = enabled ? '' : 'none';
    });


    if (!enabled && currentState && currentState.status !== 'running') {
        if (runBtn) runBtn.style.display = 'none';
        if (skipBtn) skipBtn.style.display = 'none';
        if (backBtn) backBtn.style.display = 'none';
        if (autoBtn) autoBtn.style.display = 'none';
        if (btnCancel) btnCancel.style.display = 'none';
    } else {
        if (runBtn) runBtn.style.display = '';
        if (skipBtn) skipBtn.style.display = '';
        if (backBtn) backBtn.style.display = '';
        if (autoBtn) autoBtn.style.display = '';
        if (btnCancel) btnCancel.style.display = '';

        if (isAutoMode) {
            if (runBtn) { runBtn.disabled = true; runBtn.setAttribute('disabled', 'true'); }
            if (skipBtn) { skipBtn.disabled = true; skipBtn.setAttribute('disabled', 'true'); }
            if (backBtn) { backBtn.disabled = true; backBtn.setAttribute('disabled', 'true'); }
            if (autoBtn) { autoBtn.disabled = isEditing; if (isEditing) autoBtn.setAttribute('disabled', 'true'); else autoBtn.removeAttribute('disabled'); }
            if (btnCancel) { btnCancel.disabled = isEditing; if (isEditing) btnCancel.setAttribute('disabled', 'true'); else btnCancel.removeAttribute('disabled'); }
        } else {
            if (runBtn) { runBtn.disabled = !enabled || isEditing; if (!enabled || isEditing) runBtn.setAttribute('disabled', 'true'); else runBtn.removeAttribute('disabled'); }
            if (skipBtn) { skipBtn.disabled = !enabled || isEditing; if (!enabled || isEditing) skipBtn.setAttribute('disabled', 'true'); else skipBtn.removeAttribute('disabled'); }
            if (backBtn) { backBtn.disabled = !enabled || isEditing; if (!enabled || isEditing) backBtn.setAttribute('disabled', 'true'); else backBtn.removeAttribute('disabled'); }
            if (autoBtn) { autoBtn.disabled = !enabled || isEditing; if (!enabled || isEditing) autoBtn.setAttribute('disabled', 'true'); else autoBtn.removeAttribute('disabled'); }
            if (btnCancel) { btnCancel.disabled = isEditing; if (isEditing) btnCancel.setAttribute('disabled', 'true'); else btnCancel.removeAttribute('disabled'); }
            const kebabBtn = document.getElementById('btnKebab');
            if (kebabBtn) { kebabBtn.disabled = !enabled || isEditing; if (!enabled || isEditing) kebabBtn.setAttribute('disabled', 'true'); else kebabBtn.removeAttribute('disabled'); }
        }
    }
}


function syncAutoButton() {
    const btn = document.getElementById('btnAuto');
    if (!btn) return;
    if (isAutoMode) {
        btn.classList.remove('btn-auto-blue');
        btn.classList.add('btn-pause-orange');
        btn.innerHTML = '<i class="fa-solid fa-pause" aria-hidden="true"></i> Pause';
        btn.title = 'Pause Auto-run (Alt+S)';
        btn.setAttribute('aria-label', 'Pause Auto-run (Alt+S)');
    } else {
        btn.classList.remove('btn-pause-orange');
        btn.classList.add('btn-auto-blue');
        btn.innerHTML = '<i class="fa-solid fa-forward-fast" aria-hidden="true"></i> Auto';
        btn.title = 'Auto-run (Alt+S)';
        btn.setAttribute('aria-label', 'Toggle Auto-run (Alt+S)');
    }
}

function setConnectionState(state) {
    const dot = document.getElementById('connectionDot');
    if (dot) {
        dot.className = '';
        dot.classList.add(state);
        dot.title = { connected: 'Loading', paused: 'Waiting for input', error: 'Disconnected' }[state] || state;
    }

    if (state === 'error' && currentState === null) {
        const overlay = document.getElementById('initialLoadingOverlay');
        if (overlay) {
            const h2 = overlay.querySelector('h2');
            const p = overlay.querySelector('p');
            if (h2) h2.textContent = 'Connection Error';
            if (p) p.textContent = 'Retrying connection...';
            const icon = overlay.querySelector('i');
            if (icon) {
                icon.classList.remove('fa-circle-notch', 'fa-spin');
                icon.classList.add('fa-triangle-exclamation');
                icon.style.color = 'var(--accent-danger)';
            }
        }
    } else if (state === 'connected' && currentState === null) {
        const overlay = document.getElementById('initialLoadingOverlay');
        if (overlay) {
            const h2 = overlay.querySelector('h2');
            const p = overlay.querySelector('p');
            if (h2) h2.textContent = 'Loading';
            if (p) p.textContent = 'Waiting for test data...';
            const icon = overlay.querySelector('i');
            if (icon) {
                icon.classList.remove('fa-triangle-exclamation');
                icon.classList.add('fa-circle-notch', 'fa-spin');
                icon.style.color = 'var(--accent-primary)';
            }
        }
    }
}

function showStaleBanner(activeRunId) {
    const banner = document.getElementById('staleBanner');
    const msg = document.getElementById('staleBannerMsg');
    if (banner && msg) {
        msg.textContent = `This tab is connected to run "${currentRunId || '?'}" but the active run is "${activeRunId}".`;
        banner.style.display = 'flex';
    }
    setButtonsEnabled(false);
}

function refreshToCurrentRun() {
    window.location.reload();
}

function showCancelWarning() {
    document.getElementById('btnCancel').style.display = 'none';
    document.getElementById('cancelWarning').style.display = 'flex';
}

document.getElementById('btnCancel').style.display = 'inline-flex';

function hideCancelWarning() {
    document.getElementById('cancelWarning').style.display = 'none';
    document.getElementById('btnCancel').style.display = 'inline-flex';
}

function triggerSaveExit() {
    sendAction('SAVE_EXIT', { bindings: currentState?.dataBindings });
}

function triggerStop() {
    hideCancelWarning();
    if (currentPauseId) {
        sendAction('ABORT');
    } else {
        setButtonsEnabled(false);
        fetch('/api/stop', {
            method: 'POST'
        }).catch(err => {
            console.error('Stop request failed', err);
        });
    }
}

function toggleTheme() {
    const themeSelect = document.getElementById('themeSelect');
    if (themeSelect) {
        if (themeSelect.value === 'dark') {
            themeSelect.value = 'light';
        } else if (themeSelect.value === 'light') {
            themeSelect.value = 'system';
        } else {
            themeSelect.value = 'dark';
        }
        saveSettings();
    }
}

function applyTheme(theme) {
    if (theme === 'light') {
        document.documentElement.classList.add('force-light');
    } else if (theme === 'dark') {
        document.documentElement.classList.remove('force-light');
    } else if (theme === 'system') {
        const systemIsLight = window.matchMedia('(prefers-color-scheme: light)').matches;
        if (systemIsLight) {
            document.documentElement.classList.add('force-light');
        } else {
            document.documentElement.classList.remove('force-light');
        }
    }
}

function setTextIfChanged(id, val) {
    const el = document.getElementById(id);
    if (el && el.textContent !== val) el.textContent = val;
}

function escHtml(str) {
    if (!str) return '';

    if (Array.isArray(str)) {
        return str.map(escHtml);
    }

    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
function escAttr(str) {
    if (!str) return '';

    if (Array.isArray(str)) {
        return str.map(escAttr);
    }

    return str.replace(/'/g, '&#39;').replace(/"/g, '&quot;');
}

// -------------------------------------------------------------------------
// Overlays & autocompletes
// -------------------------------------------------------------------------
function openOverlay(id) {
    document.getElementById(id).style.display = 'flex';
}
function closeOverlay(id) {
    document.getElementById(id).style.display = 'none';
    if (id === 'helpOverlay') {
        helpOverlayClicked = false;
    }
}
function toggleHelpOverlay() {
    const overlay = document.getElementById('helpOverlay');
    if (helpOverlayClicked) {
        overlay.style.display = 'none';
        helpOverlayClicked = false;
    } else {
        overlay.style.display = 'flex';
        helpOverlayClicked = true;
    }
}
function openDomOverlay(event, domContent) {
    if (event) event.preventDefault();
    document.getElementById('domPre').textContent = domContent;
    openOverlay('domOverlay');
}
let zoomScale = 1.0;
let panX = 0;
let panY = 0;
let isPanning = false;
let startX = 0;
let startY = 0;

function handleScreenshotError(imgElement) {
    if (imgElement) {
        imgElement.style.display = 'none';
        const container = imgElement.closest('.acc-screenshot-block');
        if (container) {
            const header = container.querySelector('.screenshot-header');
            if (header) {
                header.innerHTML = `<i class="fa-solid fa-image-slash" aria-hidden="true"></i> No screenshot taken`;
                header.style.color = 'var(--text-muted)';
            }
        }
    }
}

function openScreenshotOverlay(event, src) {
    if (event) event.preventDefault();
    const img = document.getElementById('screenshotImg');
    img.src = src;
    resetScreenshotZoom();
    openOverlay('screenshotOverlay');
}

function zoomScreenshot(factor) {
    zoomScale = Math.max(0.5, Math.min(zoomScale * factor, 5.0));
    updateScreenshotTransform();
}

function resetScreenshotZoom() {
    zoomScale = 1.0;
    panX = 0;
    panY = 0;
    updateScreenshotTransform();
}

function updateScreenshotTransform() {
    const img = document.getElementById('screenshotImg');
    if (img) {
        img.style.transform = `translate(${panX}px, ${panY}px) scale(${zoomScale})`;
    }
}
function saveSettings() {
    const theme = document.getElementById('themeSelect').value;
    const zoom = parseInt(document.getElementById('zoomInput').value, 10);
    localStorage.setItem('neodymium.hud.theme', theme);
    localStorage.setItem('neodymium.hud.zoom', zoom);
    applyTheme(theme);
    if (!isNaN(zoom)) {
        const factor = zoom / 100;
        document.body.style.zoom = factor;
        document.body.style.height = (100 / factor) + 'dvh';
    }
    closeOverlay('settingsOverlay');
}
function toggleTopDetails() {
    const panel = document.getElementById('topDetailsPanel');
    const chevron = document.getElementById('detailsChevron');
    if (!panel) return;
    const isVisible = panel.classList.contains('visible-details');
    if (!isVisible) {
        panel.classList.remove('hidden-details');
        panel.classList.add('visible-details');
        if (chevron) chevron.style.transform = 'rotate(180deg)';
    } else {
        panel.classList.remove('visible-details');
        panel.classList.add('hidden-details');
        if (chevron) chevron.style.transform = 'rotate(0deg)';
    }
}
function openAddStepOverlay(blockName) {
    const finalBlock = blockName || currentState?.activeBlock || 'steps';
    if (!currentState) {
        currentState = { activeBlock: 'steps', blocks: { before: [], steps: [], after: [] } };
    }
    if (!currentState.blocks) currentState.blocks = {};
    if (!currentState.blocks[finalBlock]) currentState.blocks[finalBlock] = [];

    const nextIdx = 1000 + Math.floor(Math.random() * 10000);
    const tempStep = {
        index: nextIdx,
        instruction: '',
        status: 'pending',
        source: 'llm',
        includeFile: null,
        includeLevel: 0,
        isTempAdd: true
    };

    currentState.blocks[finalBlock].push(tempStep);
    applyState(currentState);

    const card = document.querySelector(`.step-card[data-step-idx='${nextIdx}']`);
    if (card) {
        enableEdit(card.querySelector('.step-edit-btn') || card);
        card.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

let draggedStepIndex = null;

// -------------------------------------------------------------------------
// Reorder System Overhaul Logic
// -------------------------------------------------------------------------
let lastFocusedStepIndex = null;
let lastFocusedElementClass = null;

function announceMove(message) {
    const region = document.getElementById('ariaLiveRegion');
    if (region) region.textContent = message;
}

function flashMovedRow(stepIndex) {
    setTimeout(() => {
        const card = document.querySelector(`.step-card[data-step-idx="${stepIndex}"]`);
        if (card) {
            card.classList.add('moved-row-flash');
            setTimeout(() => card.classList.remove('moved-row-flash'), 2000);
        }
    }, 50);
}

function restoreFocus() {
    if (lastFocusedStepIndex !== null) {
        const card = document.querySelector(`.step-card[data-step-idx="${lastFocusedStepIndex}"]`);
        if (card) {
            const input = card.querySelector('.step-num-badge input');
            if (input) input.focus();
        }
    }
}

function resolveBlock(targetFlatIdx) {
    if (!currentState || !currentState.blocks) return null;
    let currentFlat = 0;
    for (const bKey of ['before', 'steps', 'after']) {
        const arr = currentState.blocks[bKey] || [];
        if (targetFlatIdx < currentFlat + arr.length) {
            return { block: bKey, relIdx: targetFlatIdx - currentFlat };
        }
        currentFlat += arr.length;
    }
    // If it's the very end
    return { block: 'after', relIdx: (currentState.blocks.after || []).length };
}

function reorderStepDirect(event, fromStepIndex) {
    const input = event.target;
    const newPos = parseInt(input.value, 10);
    if (isNaN(newPos) || !currentState) return;

    const totalCount = allStepIndices.length;
    if (newPos < 1 || newPos > totalCount) {
        input.value = allStepIndices.indexOf(fromStepIndex) + 1;
        return;
    }

    const currentPos = allStepIndices.indexOf(fromStepIndex) + 1;
    if (newPos === currentPos) return;

    // Find source block and relative index
    let fromBlock = null;
    let fromRelIdx = -1;
    for (const bKey of ['before', 'steps', 'after']) {
        const arr = currentState.blocks[bKey];
        if (arr) {
            const idx = arr.findIndex(s => s.index === fromStepIndex);
            if (idx !== -1) { fromBlock = bKey; fromRelIdx = idx; break; }
        }
    }
    if (!fromBlock || fromRelIdx === -1) return;

    // Find target block and relative index
    const targetFlatIdx = newPos - 1;
    const target = resolveBlock(targetFlatIdx);
    if (!target) return;

    // Store focus intent and update selection to the new position
    lastFocusedStepIndex = targetFlatIdx;
    selectedStepIndexForDetails = targetFlatIdx;

    announceMove(`Moved step to position ${newPos} of ${totalCount}`);
    flashMovedRow(fromStepIndex);

    sendAction('REORDER', {
        fromBlock: fromBlock,
        fromIndex: fromRelIdx,
        toBlock: target.block,
        toIndex: target.relIdx
    });
}

function handleStepInputKeyDown(event, stepIndex) {
    if (event.key === 'Enter') {
        event.preventDefault();
        reorderStepDirect(event, stepIndex);
        return;
    }

    if (event.ctrlKey && (event.key === 'ArrowUp' || event.key === 'ArrowDown')) {
        event.preventDefault();
        const direction = event.key === 'ArrowUp' ? 'up' : 'down';
        const currentPos = allStepIndices.indexOf(stepIndex) + 1;
        const newPos = direction === 'up' ? currentPos - 1 : currentPos + 1;

        if (newPos >= 1 && newPos <= allStepIndices.length) {
            // Update input value and trigger move
            event.target.value = newPos;
            reorderStepDirect(event, stepIndex);
        }
    }
}

function reorderStep(event, stepIndex, direction) {
    const currentPos = allStepIndices.indexOf(stepIndex) + 1;
    const newPos = direction === 'up' ? currentPos - 1 : currentPos + 1;
    if (newPos >= 1 && newPos <= allStepIndices.length) {
        // Try to find the actual input element to keep the UI in sync
        const card = document.querySelector(`.step-card[data-step-idx="${stepIndex}"]`);
        const input = card ? card.querySelector('.step-num-badge input') : null;
        if (input) {
            input.value = newPos;
            reorderStepDirect({ target: input }, stepIndex);
        } else {
            // Fallback to mock target if input is not found
            reorderStepDirect({ target: { value: newPos } }, stepIndex);
        }
    }
}

function handleDragStart(event, index) {
    if (document.querySelector('.step-card.editing')) {
        event.preventDefault();
        return;
    }
    draggedStepIndex = index;
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', String(index));
    // Find the card from the handle
    const card = event.currentTarget.closest('.step-card');
    if (card) card.classList.add('dragging');
    const sc = document.querySelector('.scrollable-content');
    if (sc) sc.classList.add('dragging-active');
}

function handleDragEnd(event) {
    if (event && event.stopPropagation) event.stopPropagation();
    document.querySelectorAll('.step-card.dragging').forEach(c => c.classList.remove('dragging'));
    const sc = document.querySelector('.scrollable-content');
    if (sc) sc.classList.remove('dragging-active');
    document.querySelectorAll('.drop-target-area').forEach(t => t.classList.remove('active'));
    draggedStepIndex = null;
    stopAutoScroll();
}

function handleDragOver(event) {
    event.preventDefault();
    // Allow bubbling for container auto-scroll
    event.dataTransfer.dropEffect = 'move';
    const targetArea = event.currentTarget;
    targetArea.classList.add('active');

    if (draggedStepIndex !== null) {
        const tempIndices = [...allStepIndices];
        const oldIdx = tempIndices.indexOf(draggedStepIndex);
        if (oldIdx === -1) return;

        const dropZones = [...document.querySelectorAll('.drop-target-area')];
        const zoneIdx = dropZones.indexOf(targetArea);
        if (zoneIdx === -1) return;

        // Move item in temp list to calculate new indices for preview
        tempIndices.splice(oldIdx, 1);
        let targetIdx = zoneIdx;
        if (targetIdx > oldIdx) targetIdx--;
        tempIndices.splice(targetIdx, 0, draggedStepIndex);

        // Update all input fields to show what the new indices WOULD be
        tempIndices.forEach((stepIdx, i) => {
            const card = document.querySelector(`.step-card[data-step-idx="${stepIdx}"]`);
            if (card) {
                const input = card.querySelector('.step-num-badge input');
                if (input) input.value = i + 1;
            }
        });
    }
}

function handleDragLeave(event) {
    event.stopPropagation();
    event.currentTarget.classList.remove('active');
}

let autoScrollInterval = null;
function handleContainerDragOver(event) {
    event.preventDefault();
    const sc = event.currentTarget;
    const rect = sc.getBoundingClientRect();
    const threshold = 60; // Distance from top/bottom to start scrolling
    const maxSpeed = 20;

    let speed = 0;
    if (event.clientY < rect.top + threshold) {
        speed = -maxSpeed * (1 - (event.clientY - rect.top) / threshold);
    } else if (event.clientY > rect.bottom - threshold) {
        speed = maxSpeed * (1 - (rect.bottom - event.clientY) / threshold);
    }

    if (speed !== 0) {
        if (!autoScrollInterval) {
            autoScrollInterval = setInterval(() => {
                sc.scrollTop += speed;
            }, 20);
        }
    } else {
        stopAutoScroll();
    }
}

function stopAutoScroll() {
    if (autoScrollInterval) {
        clearInterval(autoScrollInterval);
        autoScrollInterval = null;
    }
}

function handleDrop(event, targetStepIndex, position, targetBlockName) {
    event.preventDefault();

    if (draggedStepIndex === null) {
        handleDragEnd();
        return;
    }

    // Find source block and relative index
    let fromBlock = null;
    let fromRelIdx = -1;
    for (const bKey of ['before', 'steps', 'after']) {
        const arr = currentState.blocks[bKey];
        if (arr) {
            const idx = arr.findIndex(s => s.index === draggedStepIndex);
            if (idx !== -1) { fromBlock = bKey; fromRelIdx = idx; break; }
        }
    }

    if (!fromBlock || fromRelIdx === -1) {
        handleDragEnd();
        return;
    }

    // Find target block and relative index
    let targetBlock = null;
    let targetRelIdx = -1;

    if (targetStepIndex === -1 && targetBlockName) {
        // Final drop zone of a specific block
        targetBlock = targetBlockName;
        targetRelIdx = (currentState.blocks[targetBlock] || []).length;
    } else {
        // Drop on or before a specific step
        for (const bKey of ['before', 'steps', 'after']) {
            const arr = currentState.blocks[bKey];
            if (arr) {
                const tIdx = arr.findIndex(s => s.index === targetStepIndex);
                if (tIdx !== -1) {
                    targetBlock = bKey;
                    targetRelIdx = tIdx;
                    if (position === 'after') targetRelIdx++;
                    break;
                }
            }
        }
    }

    if (!targetBlock || targetRelIdx === -1) {
        handleDragEnd();
        return;
    }

    // Send to server
    if (currentPauseId) {
        sendAction('REORDER', {
            fromBlock: fromBlock,
            fromIndex: fromRelIdx,
            toBlock: targetBlock,
            toIndex: targetRelIdx
        });
    } else {
        // Local reorder if not paused
        const item = currentState.blocks[fromBlock].splice(fromRelIdx, 1)[0];
        let adjustedToIdx = targetRelIdx;
        if (fromBlock === targetBlock && adjustedToIdx > fromRelIdx) adjustedToIdx--;
        currentState.blocks[targetBlock].splice(adjustedToIdx, 0, item);
        applyState(currentState);
    }

    handleDragEnd();
}

function moveStep(event, index, direction) {
    // Deprecated in favor of Drag & Drop, but kept for internal logic if needed
    if (event) event.stopPropagation();
    if (!currentState || !currentState.blocks) return;

    let blockName = null;
    let list = null;
    let relativeIndex = -1;

    for (const bKey of ['before', 'steps', 'after']) {
        const arr = currentState.blocks[bKey];
        if (arr) {
            const idx = arr.findIndex(s => s.index === index);
            if (idx !== -1) {
                blockName = bKey;
                list = arr;
                relativeIndex = idx;
                break;
            }
        }
    }

    if (!list || relativeIndex === -1) return;

    const targetRelativeIndex = relativeIndex + direction;
    if (targetRelativeIndex < 0 || targetRelativeIndex >= list.length) return;

    if (currentPauseId) {
        sendAction('REORDER', { block: blockName, fromIndex: relativeIndex, toIndex: targetRelativeIndex });
    } else {
        const temp = list[relativeIndex];
        list[relativeIndex] = list[targetRelativeIndex];
        list[targetRelativeIndex] = temp;
        applyState(currentState);
    }
}
function insertVarInActiveTextarea(varName) {
    const activeEditCard = document.querySelector('.step-card.editing');
    if (activeEditCard) {
        const textarea = activeEditCard.querySelector('.inline-edit-textarea');
        if (textarea) {
            insertAtCursor(textarea, `\${${varName}}`);
        }
    }
}
function insertAtCursor(inputElement, textToInsert) {
    const start = inputElement.selectionStart;
    const end = inputElement.selectionEnd;
    const value = inputElement.value;
    inputElement.value = value.substring(0, start) + textToInsert + value.substring(end);
    inputElement.selectionStart = inputElement.selectionEnd = start + textToInsert.length;
    inputElement.focus();
}
function submitAddStep() {
    // Handled inline in saveEdit now
}

// Shortcuts keyboard listener
document.addEventListener('keydown', function (e) {
    if (e.repeat) return;
    const key = e.key ? e.key.toLowerCase() : '';

    if (e.key === 'Escape') {
        ['settingsOverlay', 'helpOverlay', 'domOverlay', 'screenshotOverlay'].forEach(closeOverlay);
        e.preventDefault();
        return;
    }
    if (e.altKey && key === 'c') {
        ['settingsOverlay', 'helpOverlay', 'domOverlay', 'screenshotOverlay'].forEach(closeOverlay);
        e.preventDefault();
        return;
    }
    if (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA') {
        return;
    }

    if (e.altKey && key === 'r' || (e.ctrlKey && e.key === 'Enter')) {
        e.preventDefault();
        const runBtn = document.getElementById('btnRun');
        if (runBtn && !runBtn.disabled) runBtn.click();
    } else if (e.altKey && key === 'k') {
        e.preventDefault();
        const skipBtn = document.getElementById('btnSkip');
        if (skipBtn && !skipBtn.disabled) skipBtn.click();
    } else if (e.altKey && key === 's') {
        e.preventDefault();
        toggleAuto();
    } else if (e.altKey && key === 'o') {
        e.preventDefault();
        const overlay = document.getElementById('settingsOverlay');
        if (overlay.style.display === 'flex') {
            closeOverlay('settingsOverlay');
        } else {
            openOverlay('settingsOverlay');
        }
    } else if (e.altKey && key === 'i') {
        e.preventDefault();
        toggleHelpOverlay();
    } else if (e.altKey && (key === 'z' || key === 'y')) {
        e.preventDefault();
        const backBtn = document.getElementById('btnBack');
        if (backBtn && !backBtn.disabled) backBtn.click();
    } else if (e.altKey && key === 'n') {
        e.preventDefault();
        const runBtn = document.getElementById('btnRun');
        if (runBtn && !runBtn.disabled) {
            openAddStepOverlay();
        }
    } else if (e.altKey && key === 'e') {
        e.preventDefault();
        const activeCard = document.querySelector('.step-card.active');
        if (activeCard) {
            const editBtn = activeCard.querySelector('.step-edit-btn');
            if (editBtn) editBtn.click();
        }
    } else if (e.ctrlKey && (key === 'arrowup' || key === 'arrowdown')) {
        // Global reorder shortcut when a step is selected but input is not focused
        if (selectedStepIndexForDetails !== null) {
            e.preventDefault();
            reorderStep(e, selectedStepIndexForDetails, key === 'arrowup' ? 'up' : 'down');
        }
    }
});

// -------------------------------------------------------------------------
// -------------------------------------------------------------------------
// Mobile QR Code Overlay — uses qrcode-generator (loaded lazily from CDN)
// -------------------------------------------------------------------------
let _qrLoaded = false;

function loadQrLib() {
    if (_qrLoaded && window.qrcode) return Promise.resolve();
    return new Promise((resolve, reject) => {
        const s = document.createElement('script');
        // qrcode-generator: tiny, stable, no bundler needed (plain UMD file, not npm)
        s.src = 'https://cdn.jsdelivr.net/npm/qrcode-generator@1.4.4/qrcode.js';
        s.onload = () => { _qrLoaded = true; resolve(); };
        s.onerror = () => reject(new Error('cdn_fail'));
        document.head.appendChild(s);
    });
}

/**
 * Use WebRTC ICE candidate discovery to find the machine's LAN IP address.
 * This is the only browser-side way to get the actual network interface IP
 * without a server-side API call. Works on all modern browsers.
 * Returns null if no LAN IP can be found within 1.5 s.
 */
function getLanIp() {
    return new Promise((resolve) => {
        const found = new Set();
        let pc;
        try {
            pc = new RTCPeerConnection({ iceServers: [] });
            pc.createDataChannel('');
            pc.createOffer()
                .then(o => pc.setLocalDescription(o))
                .catch(() => resolve(null));
            pc.onicecandidate = (e) => {
                if (!e || !e.candidate) {
                    // ICE gathering complete
                    pc.close();
                    resolve(found.size ? [...found][0] : null);
                    return;
                }
                // Extract IPv4 addresses from the SDP candidate line
                const m = /(\d{1,3}(?:\.\d{1,3}){3})/.exec(e.candidate.candidate);
                if (m && !m[1].startsWith('127.') && !m[1].startsWith('169.254.')) {
                    found.add(m[1]);
                }
            };
        } catch (err) { resolve(null); return; }
        // Timeout after 1.5 s in case ICE gathering hangs
        setTimeout(() => { try { pc.close(); } catch (e) { } resolve(found.size ? [...found][0] : null); }, 1500);
    });
}

async function openMobileQr() {
    openOverlay('mobileQrOverlay');
    const urlEl = document.getElementById('mobileQrUrl');
    const canvas = document.getElementById('mobileQrCanvas');
    if (!canvas || !urlEl) return;

    const hostname = window.location.hostname;
    const isLocal = hostname === 'localhost' || hostname === '127.0.0.1' || hostname.startsWith('127.');

    let scanUrl = window.location.href;
    if (window.parent !== window) {
        // We are inside the manager (iframe) — ensure the path points directly to /interactive_console.html
        scanUrl = window.location.origin + '/interactive_console.html';
    }

    // Show "Detecting LAN IP…" while we probe
    urlEl.textContent = isLocal ? 'Detecting local network IP…' : scanUrl;
    if (isLocal) {
        // Prefer the LAN IP detected by the Java server if available (injected into currentState)
        let lanIp = (currentState && currentState.lanIp && currentState.lanIp !== 'localhost') ? currentState.lanIp : null;

        // Fallback to WebRTC probe if server-side detection failed
        if (!lanIp) {
            lanIp = await getLanIp();
        }

        if (lanIp && lanIp !== 'localhost' && !lanIp.startsWith('127.')) {
            // Replace localhost/127.x.x.x with the discovered LAN IP
            scanUrl = window.location.href.replace(hostname, lanIp);
            urlEl.textContent = scanUrl;
        } else {
            // Could not detect — show raw URL with a note
            scanUrl = window.location.href;
            urlEl.textContent = scanUrl +
                '\n\n⚠ Replace "' + hostname + '" with your machine\'s LAN IP before scanning.';
        }
    }

    // Draw QR
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    try {
        await loadQrLib();
        // qrcode-generator API: qrcode(typeNumber, errorCorrectionLevel)
        const qr = window.qrcode(0, 'M');
        qr.addData(scanUrl);
        qr.make();

        const count = qr.getModuleCount();
        const cellSize = Math.floor(canvas.width / (count + 2));
        const offset = Math.floor((canvas.width - cellSize * count) / 2);

        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        ctx.fillStyle = '#000000';
        for (let row = 0; row < count; row++) {
            for (let col = 0; col < count; col++) {
                if (qr.isDark(row, col)) {
                    ctx.fillRect(offset + col * cellSize, offset + row * cellSize, cellSize, cellSize);
                }
            }
        }
    } catch (e) {
        ctx.fillStyle = '#fff';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#333';
        ctx.font = '12px monospace';
        ctx.fillText('QR unavailable', 10, 110);
        urlEl.textContent += '\n(QR library could not be loaded — use the URL above directly)';
    }
}