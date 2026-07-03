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
                if (currentRunId && data.runId !== currentRunId) {
                    showStaleBanner(data.runId);
                } else {
                    currentRunId = data.runId;
                    setButtonsEnabled(true);
                    checkActionApprovals();
                    if (isAutoMode) {
                        triggerAutoRunCheck();
                    }
                }
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
            const dataUrl = params.get('data') || '/run_data.json';
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
            fetch('/api/console/events', { method: 'HEAD' })
                .then(r => { if (r.ok || r.status === 200) connectSSE(); else tryStaticLoad(); })
                .catch(() => tryStaticLoad());

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
        const activeBreakpoints = new Set();

        function applyState(state) {
            if (!state) return;
            const isFirstLoad = currentState === null;
            currentState = state;
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

            // Update progress pill (desktop) and mobile bar.
            // currentStep = doneSteps + 1 when a step is actively running; otherwise = doneSteps.
            const allStepsForProgress = [
                ...(state.blocks?.before || []),
                ...(state.blocks?.steps || []),
                ...(state.blocks?.after || [])
            ];
            const totalSteps = allStepsForProgress.length;
            const doneSteps = allStepsForProgress.filter(s => s.status === 'passed' || s.status === 'skipped' || s.status === 'failed').length;
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
                ...(state.blocks?.before || []),
                ...(state.blocks?.steps || []),
                ...(state.blocks?.after || [])
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
            renderBlock('before', state.blocks?.before || []);
            renderBlock('steps', state.blocks?.steps || []);
            renderBlock('after', state.blocks?.after || []);

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
                const keys = Object.keys(state.dataBindings);
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
                ...(state.blocks?.before || []),
                ...(state.blocks?.steps || []),
                ...(state.blocks?.after || [])
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
            if (state && (state.blocks?.steps || []).length > 0) {
                const steps = state.blocks.steps;
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

            if (isThinking || isAutoMode) {
                document.getElementById('btnRun').disabled = true;
            } else {
                document.getElementById('btnRun').disabled = false;
            }

            // Handle toolbar Back button disabled if index is 0 or auto is on
            const isFirstStep = s && s.index === 0;
            document.getElementById('btnBack').disabled = isFirstStep || isAutoMode;
        }

        function renderBlock(blockName, steps) {
            const container = document.getElementById(blockName + 'Steps');
            const group = document.getElementById(blockName + 'Group');
            if (!container || !group) return;

            // Enhanced signature to include selection, breakpoints, and other UI state
            const sig = JSON.stringify({
                steps: steps.map(s => `${s.index}|${s.status}|${s.instruction}|${s.source||''}|${s.reasoning ? s.reasoning.length : 0}|${s.actions ? s.actions.length : 0}|${s.errorMessage ? s.errorMessage.length : 0}`),
                selected: selectedStepIndexForDetails,
                breakpoints: Array.from(activeBreakpoints),
                paused: currentPauseId !== null,
                bindings: currentState?.dataBindings
            });
            if (container.dataset.sig === sig) return;
            container.dataset.sig = sig;

            if (steps.length === 0) {
                // If the block is empty, we show a drop zone so steps can be dragged into it
                container.innerHTML = `<div class="drop-target-area empty-block-zone" data-block="${blockName}" ondragover="handleDragOver(event)" ondragleave="handleDragLeave(event)" ondrop="handleDrop(event, -1, 'after', '${blockName}')">
                    <span style="opacity: 0.5; font-size: 12px;">Drop steps here</span>
                </div>`;
                // Keep the group visible if it's the main 'steps' block, even if empty, 
                // or if it's explicitly needed. For now, let's keep all groups visible if we want to drop into them.
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

            // Right-column panel compact reasoning (not shown for active steps; the card has the bubble)
            const isThinking = step.status === 'running' && !step.reasoning;
            const reasoningText = !isActiveStep && (isThinking || step.reasoning)
                ? (isThinking
                    ? `<div class="acc-reasoning" style="margin-bottom:12px;"><i class="fa-solid fa-circle-notch fa-spin" style="color:var(--accent-purple)" aria-hidden="true"></i> AI is thinking...</div>`
                    : `<div class="acc-reasoning" style="margin-bottom:12px; ${isFailed ? 'color:var(--accent-danger)' : ''}">
                        <i class="fa-solid ${isFailed ? 'fa-triangle-exclamation' : 'fa-brain'}" style="margin-right:8px;" aria-hidden="true"></i>${escHtml(step.reasoning)}</div>`)
                : '';

            // Timing meta — only show execution time for steps that have finished
            const thinkingTimeBlock = (step.thinkingTimeMs !== undefined && step.thinkingTimeMs > 0)
                ? `<span title="Thinking duration"><i class="fa-solid fa-hourglass-half" style="color:var(--accent-purple);" aria-hidden="true"></i> ${(step.thinkingTimeMs / 1000).toFixed(1)}s</span>`
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
            const screenshotBlock = step.screenshot
                ? `<div class="acc-screenshot-block" style="margin-top:12px;">
                       <div style="font-size:12px;"><i class="fa-solid fa-image" aria-hidden="true"></i> Screenshot: <a href="#" onclick="openScreenshotOverlay(event,'${escAttr(step.screenshot)}')" style="color:var(--accent-primary);text-decoration:underline;">View Fullscreen</a></div>
                       <img class="acc-screenshot" src="${escAttr(step.screenshot)}" alt="Step screenshot"
                            onclick="openScreenshotOverlay(event,'${escAttr(step.screenshot)}')" style="cursor:pointer;margin-top:8px;max-height:200px;width:100%;object-fit:cover;border-radius:8px;">
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
                    <div style="display:flex;gap:12px;flex-wrap:wrap;">${domBlock}</div>
                    ${screenshotBlock}
                </div>
            </div>`;
        }

        function generateEditableBindingsTable(bindings, textareaIdOrClass) {
            const keys = Object.keys(bindings || {});
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
                    const keys = Object.keys(currentState.dataBindings);
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
            const includeLevel = typeof step.includeLevel === 'number' ? step.includeLevel : 0;
            const lvlClass = includeLevel > 0 ? ` include-lvl-${Math.min(includeLevel, 4)}` : '';
            const skippedClass = isSkipped ? ' skipped' : '';
            const passedClass = isPassed ? ' passed-step' : '';

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
            } else if (step.status === 'running' && currentPauseId === null) {
                statusIconMarkup = `<i class="fa-solid fa-circle-notch fa-spin" style="color:var(--accent-primary)" aria-label="Running"></i>`;
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
            const rewindBtnHtml = isPassed || isFailed || isSkipped
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
                    const isThinking = step.status === 'running' && !step.reasoning;
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

            return `
                <div class="drop-target-area" ondragover="handleDragOver(event)" ondragleave="handleDragLeave(event)" ondrop="handleDrop(event, ${step.index}, 'before')"></div>
                <div class="step-card${activeClass}${selectedClass}${lvlClass}${skippedClass}${passedClass}${noDetailsPendingClass}" data-step-idx="${step.index}" role="listitem" tabindex="0" aria-label="Step ${step.index}: ${escHtml(resolvedInstruction)}"
                     onclick="handleStepClick(event, ${step.index})" ondragend="handleDragEnd(event)">
                ${activeBadge}
                <div class="step-row">
                    <div class="step-num-badge">
                        ${isSelected ? `
                            <input type="number" value="${allStepIndices.indexOf(step.index) + 1}"
                                   min="1" max="${allStepIndices.length}"
                                   onkeydown="handleStepInputKeyDown(event, ${step.index})"
                                   onblur="reorderStepDirect(event, ${step.index})"
                                   aria-label="Step position. Type a new number to move this step. Use Ctrl + Up/Down arrows to move.">
                        ` : `#${step.index + 1}`}
                    </div>
                    <div class="${statusIconContainerClass}" ${statusClickAttr}>${statusIconMarkup}</div>
                    <div class="step-text-container">
                        <div class="tag-row">${sourceTag}</div>
                        <div class="step-text">${stepTextContent}</div>
                        <div class="step-edit-form">
                            <textarea class="inline-edit-textarea" aria-label="Edit step instruction text">${escHtml(step.rawInstruction || step.instruction)}</textarea>
                            <div class="inline-edit-bindings" style="margin: 8px 0; display: flex; flex-direction: column; gap: 6px;"></div>
                            <div class="inline-edit-actions">
                                <button class="btn" style="padding: 4px 10px; font-size: 11px;" onclick="cancelEdit(this)" aria-label="Cancel editing step">Cancel</button>
                                <button class="btn btn-primary" style="padding: 4px 10px; font-size: 11px;" onclick="saveEdit(this)" aria-label="Save step instruction">Save</button>
                            </div>
                        </div>
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
        }

        function toggleAuto() {
            if (isAutoMode) {
                isAutoMode = false;
                syncAutoButton();
                autoStartStepIndex = null;
                setButtonsEnabled(true);
            } else {
                if (!currentPauseId) return;
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
            // Prevent toggling if clicking interactive controls or the breakpoint status icon
            if (event.target.closest('button') || event.target.closest('textarea') || event.target.closest('.step-status-icon')) return;

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

            const textarea = card.querySelector('.inline-edit-textarea');
            const idx = parseInt(card.getAttribute('data-step-idx'), 10);

            let rawInstruction = '';
            if (currentState && currentState.blocks) {
                for (const blockName of ['before', 'steps', 'after']) {
                    const stepObj = currentState.blocks[blockName]?.find(s => s.index === idx);
                    if (stepObj) {
                        rawInstruction = stepObj.instruction;
                        break;
                    }
                }
            }
            textarea.value = rawInstruction;
            textarea.focus();

            const bindingsDiv = card.querySelector('.inline-edit-bindings');
            if (bindingsDiv && currentState?.dataBindings) {
                bindingsDiv.innerHTML = '<div style="font-size:11px; font-weight:600; color:var(--text-secondary); margin-bottom:4px;">Available Variables (Click to paste):</div>' +
                    Object.entries(currentState.dataBindings).map(([k, v]) => `
                        <div class="binding-item" style="display:flex; justify-content:space-between; font-size:11px; padding:4px 6px; border-radius:4px; background:rgba(255,255,255,0.03); cursor:pointer; border:1px solid transparent;" onclick="pasteBinding(this, '${k}')" onmouseover="this.style.borderColor='var(--accent-primary)'" onmouseout="this.style.borderColor='transparent'">
                            <span style="color:var(--accent-primary); font-weight:600;">\${${k}}</span>
                            <span style="color:var(--text-muted); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; margin-left:8px;">${escHtml(v)}</span>
                        </div>
                    `).join('');
            }

            const acc = card.querySelector('.step-accordion-details');
            if (acc && !acc.classList.contains('open')) acc.classList.add('open');
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
            }
        }

        function saveEdit(btnElement) {
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
            } else {
                if (oldVal !== newVal && currentPauseId) {
                    sendAction('EDIT', { index: idx, instruction: newVal });
                }
                card.classList.remove('editing');
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

            if (isAutoMode) {
                if (runBtn) runBtn.disabled = true;
                if (skipBtn) skipBtn.disabled = true;
                if (backBtn) backBtn.disabled = true;
                if (autoBtn) autoBtn.disabled = false;
            } else {
                if (runBtn) runBtn.disabled = !enabled;
                if (skipBtn) skipBtn.disabled = !enabled;
                if (backBtn) backBtn.disabled = !enabled;
                if (autoBtn) autoBtn.disabled = !enabled;
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
            if (!dot) return;
            dot.className = '';
            dot.classList.add(state);
            dot.title = { connected: 'Connected', paused: 'Waiting for input', error: 'Disconnected' }[state] || state;
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
                openAddStepOverlay();
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

            // Show "Detecting LAN IP…" while we probe
            urlEl.textContent = isLocal ? 'Detecting local network IP…' : window.location.href;

            let scanUrl = window.location.href;
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