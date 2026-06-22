(function (hudHtml, planned, performed, autoSkip, hudPromptChanged, isFinished, canEdit, currentUnresolvedStep, dataBindings, configMap, reasoning, isReplay, lastFullPromptOpen, lastBreakpointsStr, lastHelpShown, settingsJson, stateSignature, beforeSteps, stepsSteps, afterSteps, currentBlock, currentBlockIndex) {
    var existingHud = document.getElementById('neodymium-ai-hud-container');
    window.neoCurrentUnresolvedStep = currentUnresolvedStep;
    window.neoDataBindings = dataBindings;
    window.neoConfigMap = configMap;
    window.neoCurrentBlock = currentBlock;
    window.neoBeforeSteps = beforeSteps;
    window.neoStepsSteps = stepsSteps;
    window.neoAfterSteps = afterSteps;
    window.neoCanEdit = canEdit;

    function getSessionStorage(key) {
        try {
            return sessionStorage.getItem(key);
        } catch (e) {
            return null;
        }
    }

    function setSessionStorage(key, value) {
        try {
            sessionStorage.setItem(key, value);
        } catch (e) { }
    }

    if (window.neoFullPromptOpen === undefined) {
        window.neoFullPromptOpen = (lastFullPromptOpen === true) || (getSessionStorage('neoFullPromptOpen') === 'true');
    }

    if (window.neoBreakpoints === undefined) {
        if (lastBreakpointsStr) {
            window.neoBreakpoints = JSON.parse(lastBreakpointsStr);
        } else {
            var bpStr = getSessionStorage('neoBreakpoints');
            window.neoBreakpoints = bpStr ? JSON.parse(bpStr) : [];
        }
    }

    if (window.neoHelpShown === undefined) {
        window.neoHelpShown = (lastHelpShown === true) || (getSessionStorage('neoHelpShown') === 'true');
    }

    if (existingHud && existingHud.getAttribute('data-hud-version') !== '4') {
        existingHud.remove();
        window.neodymiumPromptGenerationHudInjected = false;
        existingHud = null;
    }

    if (!window.neodymiumPromptGenerationHudInjected || !existingHud) {
        var hud = document.createElement('div');
        hud.innerHTML = hudHtml;
        document.body.appendChild(hud.firstChild);

        window.neodymiumPromptGenerationHudInjected = true;
        window.neoHudAction = null;
        window.neoSubmitAction = function (actionObj) {
            window.neoHudAction = JSON.stringify(actionObj);
            if (window.neoMinimizeHud) {
                var shouldMinimize = false;
                if (actionObj.action === 'APPROVE' || actionObj.action === 'SAVE_EXIT') shouldMinimize = true;
                if (shouldMinimize) window.neoMinimizeHud();
            }
        };
        window.neoHudAutoSkip = autoSkip;

        var autoSkipBtn = document.getElementById('neo-autoskip-btn');
        var autoSkipIcon = document.getElementById('neo-autoskip-icon');
        var autoSkipText = document.getElementById('neo-autoskip-text');

        window.updateAutoSkipBtn = function () {
            if (window.neoHudAutoSkip) {
                if (autoSkipIcon) autoSkipIcon.innerText = '⏸️';
                if (autoSkipText) autoSkipText.innerText = 'Pause';
                if (autoSkipBtn) {
                    autoSkipBtn.classList.add('neo-btn-warning');
                    autoSkipBtn.style.background = '';
                }
            } else {
                if (autoSkipIcon) autoSkipIcon.innerText = '⏩';
                if (autoSkipText) autoSkipText.innerText = 'Fast-Forward';
                if (autoSkipBtn) {
                    autoSkipBtn.classList.remove('neo-btn-warning');
                    autoSkipBtn.style.background = '';
                }
            }
        };
        window.updateAutoSkipBtn();

        if (autoSkipBtn) {
            autoSkipBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                window.neoHudAutoSkip = !window.neoHudAutoSkip;
                window.updateAutoSkipBtn();

                setSessionStorage('neoLastAutoSkip', window.neoHudAutoSkip ? 'true' : 'false');

                if (window.neoHudAutoSkip) {
                    // Java will automatically detect the autoSkip state, handle breakpoints, and break out of its wait loop.
                    if (window.neoMinimizeHud) window.neoMinimizeHud();
                }
            });
        }

        var hudElement = document.getElementById('neo-ai-hud');
        hudElement.style.opacity = '1';

        // Prevent mouse interactions inside the HUD from bubbling up and closing page overlays
        hudElement.addEventListener('click', function (e) { e.stopPropagation(); });
        hudElement.addEventListener('mousedown', function (e) { e.stopPropagation(); });
        hudElement.addEventListener('mouseup', function (e) { e.stopPropagation(); });

        window.neoMinimizeHud = function () {
            var mainHud = document.getElementById('neo-ai-hud');
            var minCircle = document.getElementById('neo-min-circle');
            var minIcon = document.getElementById('neo-min-icon');
            if (mainHud) mainHud.style.display = 'none';
            if (minCircle) {
                minCircle.style.display = 'flex';
                if (window.neoHudAutoSkip) {
                    minIcon.innerText = '⏸️';
                    minCircle.style.background = '#FF9800';
                } else {
                    minIcon.innerText = '+';
                    minCircle.style.background = 'rgba(30,30,30,0.95)';
                }
            }
            setSessionStorage('neoHudMinimized', 'true');
        };

        window.neoMaximizeHud = function () {
            var mainHud = document.getElementById('neo-ai-hud');
            var minCircle = document.getElementById('neo-min-circle');
            var minIcon = document.getElementById('neo-min-icon');
            if (mainHud) mainHud.style.display = 'flex';
            if (minCircle) {
                minCircle.style.display = 'none';
                if (minIcon && minIcon.innerText === '⏸️') {
                    window.neoHudAutoSkip = false;
                    if (window.updateAutoSkipBtn) window.updateAutoSkipBtn();
                }
            }
            setSessionStorage('neoHudMinimized', 'false');

            setTimeout(function () {
                if (!mainHud) return;
                var rect = mainHud.getBoundingClientRect();
                var currentBottom = parseFloat(mainHud.style.bottom) || 20;
                if (currentBottom + rect.height > window.innerHeight) {
                    var newBottom = window.innerHeight - rect.height;
                    if (newBottom < 0) newBottom = 0;
                    mainHud.style.bottom = newBottom + 'px';
                    setSessionStorage('neoHudPosBottom', newBottom + 'px');
                    if (minCircle) minCircle.style.bottom = newBottom + 'px';
                }

                var currentRight = parseFloat(mainHud.style.right) || 20;
                if (currentRight + rect.width > window.innerWidth) {
                    var newRight = window.innerWidth - rect.width;
                    if (newRight < 0) newRight = 0;
                    mainHud.style.right = newRight + 'px';
                    setSessionStorage('neoHudPosRight', newRight + 'px');
                    if (minCircle) minCircle.style.right = newRight + 'px';
                }
            }, 10);
        };

        var minCircle = document.getElementById('neo-min-circle');
        if (minCircle) {
            minCircle.addEventListener('click', function (e) {
                if (window.neoIsDraggingHud) return;
                window.neoMaximizeHud();
            });
        }

        var minBtn = document.getElementById('neo-min-btn');
        if (minBtn) {
            minBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                window.neoMinimizeHud();
            });
        }

        function makeDraggable(dragHandle, moveTargets) {
            if (!dragHandle || !moveTargets || moveTargets.length === 0) return;
            var isDown = false;
            var startX, startY, startRight, startBottom;

            dragHandle.addEventListener('mousedown', function (e) {
                if (e.target.tagName === 'BUTTON' || e.target.closest('button')) return;
                isDown = true;
                window.neoIsDraggingHud = false;
                startX = e.clientX;
                startY = e.clientY;

                startRight = parseFloat(moveTargets[0].style.right) || 20;
                startBottom = parseFloat(moveTargets[0].style.bottom) || 20;
                e.preventDefault();
            });

            document.addEventListener('mousemove', function (e) {
                if (!isDown) return;
                var dx = e.clientX - startX;
                var dy = e.clientY - startY;
                if (Math.abs(dx) > 3 || Math.abs(dy) > 3) window.neoIsDraggingHud = true;

                var newRight = startRight - dx;
                var newBottom = startBottom - dy;

                var w = moveTargets[0].offsetWidth || 40;
                var h = moveTargets[0].offsetHeight || 40;
                if (newRight < 20) newRight = 20;
                if (newBottom < 20) newBottom = 20;
                if (newRight + w > window.innerWidth - 20) newRight = window.innerWidth - w - 20;
                if (newBottom + h > window.innerHeight - 20) newBottom = window.innerHeight - h - 20;

                moveTargets.forEach(function (target) {
                    target.style.right = newRight + 'px';
                    target.style.bottom = newBottom + 'px';
                    target.style.left = 'auto';
                    target.style.top = 'auto';
                });

                var helpOverlay = document.getElementById('neo-help-overlay');
                if (helpOverlay) {
                    helpOverlay.style.right = (newRight + 405) + 'px';
                    helpOverlay.style.bottom = newBottom + 'px';
                }
                var bindingsDrawer = document.getElementById('bindingsDrawer');
                if (bindingsDrawer) {
                    bindingsDrawer.style.right = (newRight + 405) + 'px';
                    bindingsDrawer.style.bottom = newBottom + 'px';
                }

                setSessionStorage('neoHudPosRight', newRight + 'px');
                setSessionStorage('neoHudPosBottom', newBottom + 'px');
            });

            window.addEventListener('mouseup', function () {
                if (isDown) {
                    isDown = false;
                    setTimeout(function () { window.neoIsDraggingHud = false; }, 50);
                }
            }, true);
        }

        makeDraggable(document.getElementById('neo-hud-header'), [document.getElementById('neo-ai-hud'), document.getElementById('neo-min-circle')]);
        makeDraggable(document.getElementById('neo-min-circle'), [document.getElementById('neo-ai-hud'), document.getElementById('neo-min-circle')]);

        var helpBtn = document.getElementById('neo-info-btn');
        var helpOverlay = document.getElementById('neo-help-overlay');

        if (helpBtn && helpOverlay) {
            var isHovering = false;
            helpBtn.addEventListener('mouseenter', function () {
                isHovering = true;
                helpOverlay.style.display = 'block';
            });
            helpBtn.addEventListener('mouseleave', function () {
                isHovering = false;
                setTimeout(function () {
                    if (!isHovering) {
                        helpOverlay.style.display = 'none';
                    }
                }, 100);
            });
            helpOverlay.addEventListener('mouseenter', function () {
                isHovering = true;
            });
            helpOverlay.addEventListener('mouseleave', function () {
                isHovering = false;
                setTimeout(function () {
                    if (!isHovering) {
                        helpOverlay.style.display = 'none';
                    }
                }, 100);
            });

            if (!window.neoHelpShown) {
                helpOverlay.style.display = 'block';
                window.neoHelpShown = true;
                setSessionStorage('neoHelpShown', 'true');
                setTimeout(function () {
                    if (!isHovering) {
                        helpOverlay.style.display = 'none';
                    }
                }, 4000);
            }
        }

        document.getElementById('neo-approve-btn').addEventListener('click', function () {
            if (!this.disabled) {
                if (this.dataset.isFinished === 'true') {
                    window.neoSubmitAction({ action: "SAVE_EXIT" });
                } else {
                    window.neoSubmitAction({ action: "APPROVE" });
                }
                this.disabled = true;
                this.style.opacity = '0.5';
            }
        });

        document.getElementById('neo-skip-btn').addEventListener('click', function () {
            if (!this.disabled) {
                window.neoSubmitAction({ action: "SKIP" });
                this.disabled = true;
                this.style.opacity = '0.5';
            }
        });

        function validateEditInput() {
            var elInput = document.getElementById('neo-edit-input');
            var elError = document.getElementById('neo-edit-error');
            var elSubmitBtn = document.getElementById('neo-edit-submit-btn');
            if (!elInput || !elError || !elSubmitBtn) return;
            var newInstr = elInput.value;
            var regex = /\$\{([^}]+)\}/g;
            var match;
            var missingVars = [];
            while ((match = regex.exec(newInstr)) !== null) {
                var varName = match[1];
                // Check if it exists in dataBindings (ignore nested/JSON path for now, just keys)
                var hasKey = false;
                if (window.neoDataBindings) {
                    for (var key in window.neoDataBindings) {
                        if (key.toLowerCase() === varName.toLowerCase()) {
                            hasKey = true; break;
                        }
                    }
                }
                if (!hasKey && window.neoConfigMap) {
                    for (var key in window.neoConfigMap) {
                        if (key.toLowerCase() === varName.toLowerCase()) {
                            hasKey = true; break;
                        }
                    }
                }
                if (!hasKey) {
                    missingVars.push(varName);
                }
            }
            if (missingVars.length > 0) {
                elError.innerText = "Warning: Possibly unbound variables found -> " + missingVars.join(", ") + " (Save allowed)";
                elError.style.display = 'block';
                elError.style.color = '#FFA500';
                elSubmitBtn.disabled = false;
                elSubmitBtn.style.opacity = '1';
            } else {
                elError.style.display = 'none';
                elSubmitBtn.disabled = false;
                elSubmitBtn.style.opacity = '1';
            }
        }

        var initialEditInput = document.getElementById('neo-edit-input');
        if (initialEditInput) {
            initialEditInput.addEventListener('input', validateEditInput);
            initialEditInput.addEventListener('keyup', validateEditInput);
            initialEditInput.addEventListener('change', validateEditInput);
        }

        window.neoStartEditingStep = function (idx) {
            window.neoEditingStepIndex = idx;
            var elInput = document.getElementById('neo-edit-input');
            var stepText = (window.neoCurrentRenderedSteps && window.neoCurrentRenderedSteps[idx] !== undefined)
                ? window.neoCurrentRenderedSteps[idx]
                : (window.neoCurrentUnresolvedStep || (document.getElementById('neo-next-action') ? document.getElementById('neo-next-action').innerText : ''));

            if (elInput) {
                elInput.value = stepText;
            }
            validateEditInput();

            var bindingsContainer = document.getElementById('neo-bindings-container');
            var bindingsTbody = document.getElementById('neo-bindings-tbody');

            function resolveValue(val) {
                if (!val || typeof val !== 'string') return val;
                var maxDepth = 10;
                var current = val;
                for (var i = 0; i < maxDepth; i++) {
                    if (current.indexOf('${') === -1) break;
                    var prev = current;
                    current = current.replace(/\$\{([^}]+)\}/g, function (match, key) {
                        if (window.neoDataBindings && window.neoDataBindings[key] !== undefined) return window.neoDataBindings[key];
                        if (window.neoConfigMap && window.neoConfigMap[key] !== undefined) return window.neoConfigMap[key];
                        return match;
                    });
                    if (current === prev) break;
                }
                return current;
            }

            function renderTable() {
                bindingsTbody.innerHTML = '';
                var hasRows = false;
                if (window.neoDataBindings && Object.keys(window.neoDataBindings).length > 0) {
                    for (var key in window.neoDataBindings) {
                        var tr = document.createElement('tr');
                        var valHtml;
                        if (key.indexOf('neodymium.') === 0) {
                            valHtml = '<span style="color:#888;">' + resolveValue(window.neoDataBindings[key]) + '</span>';
                        } else {
                            var safeVal = window.neoDataBindings[key] ? window.neoDataBindings[key].replace(/"/g, '&quot;') : '';
                            valHtml = '<input type="text" class="neo-binding-input neo-input" data-key="' + key + '" value="' + safeVal + '" style="width:100%; box-sizing:border-box; background:var(--input-bg); color:var(--text-primary); border:1px solid var(--border-color); padding:4px 6px; border-radius:4px;">';
                        }
                        tr.innerHTML = '<td style="padding:4px 5px; font-weight:bold; width:30%;">' + key + '</td><td style="padding:4px 5px; width:70%;">' + valHtml + '</td>';
                        bindingsTbody.appendChild(tr);
                        hasRows = true;
                    }
                }

                var newInstr = elInput ? elInput.value : '';
                var regex = /\$\{([^}]+)\}/g;
                var match;
                while ((match = regex.exec(newInstr)) !== null) {
                    var varName = match[1];
                    if (window.neoDataBindings && window.neoDataBindings.hasOwnProperty(varName)) continue;

                    if (window.neoConfigMap && window.neoConfigMap.hasOwnProperty(varName)) {
                        var tr = document.createElement('tr');
                        var valHtml = '<span style="color:#888;">' + resolveValue(window.neoConfigMap[varName]) + '</span>';
                        tr.innerHTML = '<td style="padding:4px 5px; font-weight:bold; width:30%; color:#888;">' + varName + '</td><td style="padding:4px 5px; width:70%;">' + valHtml + '</td>';
                        bindingsTbody.appendChild(tr);
                        hasRows = true;
                    }
                }

                if (hasRows) {
                    bindingsContainer.style.display = 'flex';
                    bindingsContainer.style.flexDirection = 'column';
                } else {
                    bindingsContainer.style.display = 'none';
                }
            }

            renderTable();

            // Move edit overlay to the step item container being edited
            var stepItemEl = null;
            var perfCount = (window.neoPerformedList || []).length;
            if (idx === perfCount) {
                // Active step
                stepItemEl = document.querySelector('.neo-step-item.active');
            } else {
                var editIconEl = document.querySelector('.neo-edit-step-icon[data-idx="' + idx + '"]');
                if (editIconEl) {
                    stepItemEl = editIconEl.closest('.neo-step-item');
                }
            }
            if (!stepItemEl) {
                // Fallback to active step card
                stepItemEl = document.querySelector('.neo-step-item.active');
            }
            var elOverlay = document.getElementById('neo-edit-overlay');
            if (stepItemEl && elOverlay) {
                stepItemEl.appendChild(elOverlay);
                elOverlay.style.display = 'flex';
            }

            document.getElementById('neo-bindings-container').style.maxHeight = 'none';
            document.getElementById('bindingsDrawer').style.display = 'flex';
            document.getElementById('neo-toolbar-controls').style.display = 'none';
            document.getElementById('neo-edit-toolbar').style.display = 'flex';

            if (elInput) {
                elInput.focus();
            }
        };

        document.getElementById('neo-edit-btn').addEventListener('click', function () {
            if (!this.disabled) {
                var currentStepIdx = (window.neoPerformedList || []).length;
                window.neoStartEditingStep(currentStepIdx);
            }
        });

        var editCardIcon = document.getElementById('neo-edit-card-icon');
        if (editCardIcon) {
            editCardIcon.addEventListener('click', function () {
                var currentStepIdx = (window.neoPerformedList || []).length;
                window.neoStartEditingStep(currentStepIdx);
            });
        }

        document.getElementById('neo-edit-cancel-btn').addEventListener('click', function () {
            document.getElementById('bindingsDrawer').style.display = 'none';
            document.getElementById('neo-toolbar-controls').style.display = 'flex';
            document.getElementById('neo-edit-toolbar').style.display = 'none';

            document.getElementById('neo-bindings-container').style.maxHeight = '100px';
            var elOverlay = document.getElementById('neo-edit-overlay');
            if (elOverlay) {
                var rHud = document.getElementById('neo-ai-hud');
                if (rHud) {
                    rHud.appendChild(elOverlay);
                }
                elOverlay.style.display = 'none';
            }
        });

        var bindingsCloseBtn = document.getElementById('neo-bindings-close-btn');
        if (bindingsCloseBtn) {
            bindingsCloseBtn.addEventListener('click', function () {
                document.getElementById('bindingsDrawer').style.display = 'none';
            });
        }

        var elSubmitEditBtn = document.getElementById('neo-edit-submit-btn');
        if (elSubmitEditBtn) {
            elSubmitEditBtn.addEventListener('click', function () {
                if (this.disabled) return;
                var elInput = document.getElementById('neo-edit-input');
                var elOverlay = document.getElementById('neo-edit-overlay');
                var newInstr = elInput ? elInput.value : '';
                if (newInstr && newInstr.trim() !== '') {
                    var updatedBindings = {};
                    var inputs = document.querySelectorAll('.neo-binding-input');
                    for (var i = 0; i < inputs.length; i++) {
                        updatedBindings[inputs[i].dataset.key] = inputs[i].value;
                    }
                    var editIdx = (window.neoEditingStepIndex !== undefined) ? window.neoEditingStepIndex : (window.neoPerformedList || []).length;
                    window.neoSubmitAction({ action: "EDIT", instruction: newInstr.trim(), index: editIdx, bindings: updatedBindings });

                    document.getElementById('bindingsDrawer').style.display = 'none';
                    document.getElementById('neo-toolbar-controls').style.display = 'flex';
                    document.getElementById('neo-edit-toolbar').style.display = 'none';
                    document.getElementById('neo-bindings-container').style.maxHeight = '100px';
                    if (elOverlay) {
                        var rHud = document.getElementById('neo-ai-hud');
                        if (rHud) {
                            rHud.appendChild(elOverlay);
                        }
                        elOverlay.style.display = 'none';
                    }
                } else {
                    document.getElementById('bindingsDrawer').style.display = 'none';
                    document.getElementById('neo-toolbar-controls').style.display = 'flex';
                    document.getElementById('neo-edit-toolbar').style.display = 'none';
                    document.getElementById('neo-bindings-container').style.maxHeight = '100px';
                    if (elOverlay) {
                        var rHud = document.getElementById('neo-ai-hud');
                        if (rHud) {
                            rHud.appendChild(elOverlay);
                        }
                        elOverlay.style.display = 'none';
                    }
                }
            });
        }
        document.getElementById('neo-rewind-btn').addEventListener('click', function () {
            if (!this.disabled) {
                var count = (window.neoPerformedList || []).length;
                if (count > 0) {
                    window.neoSubmitAction({ action: "REWIND", index: count - 1 });
                }
            }
        });

        var confirmOverlay = document.getElementById('neo-confirm-overlay');
        if (confirmOverlay) {
            document.getElementById('neo-confirm-cancel-btn').addEventListener('click', function () {
                confirmOverlay.style.display = 'none';
                window.neoConfirmCallback = null;
            });

            document.getElementById('neo-confirm-submit-btn').addEventListener('click', function () {
                if (typeof window.neoConfirmCallback === 'function') {
                    window.neoConfirmCallback();
                }
                confirmOverlay.style.display = 'none';
                window.neoConfirmCallback = null;
            });
        }

        var addOverlay = document.getElementById('neo-add-overlay');
        document.getElementById('neo-add-overlay-btn').addEventListener('click', function () {
            if (!this.disabled) {
                addOverlay.style.display = 'flex';
                document.getElementById('neo-add-input').focus();
            }
        });

        document.getElementById('neo-add-cancel-btn').addEventListener('click', function () {
            addOverlay.style.display = 'none';
            document.getElementById('neo-add-input').value = '';
        });

        document.getElementById('neo-add-submit-btn').addEventListener('click', function () {
            var input = document.getElementById('neo-add-input').value.trim();
            if (input !== '') {
                window.neoSubmitAction({ action: "ADD", instruction: input });
                addOverlay.style.display = 'none';
                document.getElementById('neo-add-input').value = '';
            }
        });

        document.getElementById('neo-add-input').addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                document.getElementById('neo-add-submit-btn').click();
            }
        });

        var dumpBtn = document.getElementById('neo-dump-btn');
        if (dumpBtn) {
            dumpBtn.addEventListener('click', function () {
                if (!this.disabled) {
                    window.neoHudAction = JSON.stringify({ action: "DUMP" });
                    this.disabled = true;
                    this.style.opacity = '0.5';
                    this.innerText = '⏳ Dumping...';
                    setTimeout(function () {
                        dumpBtn.disabled = false;
                        dumpBtn.style.opacity = '1';
                        dumpBtn.innerText = '📥 Dump Debug Context';
                    }, 2000);
                }
            });
        }

        var settingsOverlay = document.getElementById('neo-settings-overlay');
        var settingsBtn = document.getElementById('neo-settings-btn');
        if (settingsBtn && settingsOverlay) {
            settingsBtn.addEventListener('click', function () {
                settingsOverlay.style.display = 'flex';
            });
        }

        var settingsCancelBtn = document.getElementById('neo-settings-cancel-btn');
        if (settingsCancelBtn && settingsOverlay) {
            settingsCancelBtn.addEventListener('click', function () {
                settingsOverlay.style.display = 'none';
            });
        }

        var settingsSubmitBtn = document.getElementById('neo-settings-submit-btn');
        if (settingsSubmitBtn && settingsOverlay) {
            settingsSubmitBtn.addEventListener('click', function () {
                var theme = document.getElementById('neo-theme-select').value;
                var zoom = parseInt(document.getElementById('neo-zoom-input').value, 10);
                if (isNaN(zoom)) zoom = 100;

                var settingsPayload = {
                    theme: theme,
                    zoomFactor: zoom
                };

                window.neoApplySettings(settingsPayload);

                window.neoSubmitAction({ action: 'SETTINGS', payload: JSON.stringify(settingsPayload) });
                settingsOverlay.style.display = 'none';
            });
        }

        window.neoApplySettings = function (settingsObj) {
            if (!settingsObj) return;
            var container = document.getElementById('neodymium-ai-hud-container');
            if (!container) return;

            if (settingsObj.zoomFactor) {
                var zoomDec = settingsObj.zoomFactor / 100.0;
                container.style.zoom = zoomDec;
            }
            if (settingsObj.theme) {
                if (settingsObj.theme === 'light') {
                    container.classList.add('light-theme');
                } else {
                    container.classList.remove('light-theme');
                }
            }
        };



        // Keyboard shortcuts logic
        document.addEventListener('keydown', function (e) {
            if (e.repeat) return; // Prevent holding the key from triggering multiple times
            // If typing in the input field, don't trigger shortcuts
            if (document.activeElement === document.getElementById('neo-add-input') ||
                document.activeElement === document.getElementById('neo-edit-input')) {
                if (e.key === 'Enter') {
                    if (document.activeElement === document.getElementById('neo-add-input')) {
                        document.getElementById('neo-add-submit-btn').click();
                    } else if (document.activeElement === document.getElementById('neo-edit-input')) {
                        document.getElementById('neo-edit-submit-btn').click();
                    }
                }
                return;
            }

            var key = e.key ? e.key.toLowerCase() : '';
            var isAltA = e.altKey && key === 'a';
            var isCtrlEnter = e.ctrlKey && e.code === 'Enter';

            if (isAltA || isCtrlEnter || (e.altKey && (key === 's' || key === 'h'))) {
                e.preventDefault();
                e.stopPropagation();
                e.stopImmediatePropagation();

                if (isAltA || isCtrlEnter) {
                    var btn = document.getElementById('neo-approve-btn');
                    if (!btn.disabled) {
                        if (btn.dataset.isFinished === 'true') {
                            window.neoSubmitAction({ action: "SAVE_EXIT" });
                        } else {
                            window.neoSubmitAction({ action: "APPROVE" });
                        }
                        btn.disabled = true;
                        btn.style.opacity = '0.5';
                    }
                } else if (key === 's') {
                    var toggleBtn = document.getElementById('neo-autoskip-btn');
                    if (toggleBtn) {
                        toggleBtn.click();
                    }
                } else if (key === 'h') {
                    var toggleBtn = document.getElementById('neo-full-prompt-btn');
                    if (toggleBtn) toggleBtn.click();
                }
            }
        }, true);

        // Also intercept keyup to prevent the application from reacting to the release of the shortcut keys
        document.addEventListener('keyup', function (e) {
            if (e.altKey && e.key) {
                var key = e.key.toLowerCase();
                if (key === 'a' || key === 's' || key === 'h') {
                    e.preventDefault();
                    e.stopPropagation();
                    e.stopImmediatePropagation();
                }
            }
        }, true);
        window.neoDragStart = function (e, idx) {
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', idx);
            e.target.style.opacity = '0.4';
            window.neoDraggedItem = e.target;
        };
        window.neoDragOver = function (e) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            return false;
        };
        window.neoDragEnter = function (e) {
            e.preventDefault();
            var target = e.target.closest('.neo-step-item');
            if (target && target !== window.neoDraggedItem) target.classList.add('drag-over');
        };
        window.neoDragLeave = function (e) {
            var target = e.target.closest('.neo-step-item');
            if (target) target.classList.remove('drag-over');
        };
        window.neoDrop = function (e, targetIdx) {
            e.stopPropagation();
            var target = e.target.closest('.neo-step-item');
            if (target) target.classList.remove('drag-over');
            var sourceIdx = parseInt(e.dataTransfer.getData('text/plain'), 10);
            if (sourceIdx !== targetIdx && !isNaN(sourceIdx)) {
                window.neoSubmitAction({ action: "REORDER", from: sourceIdx, to: targetIdx });
            }
        };
        window.neoDragEnd = function (e) {
            e.target.style.opacity = '1';
            var items = document.querySelectorAll('.neo-step-item');
            for (var i = 0; i < items.length; i++) items[i].classList.remove('drag-over');
        };

        window.neoRenderFullPrompt = function () {
            // Safety: if the editing overlay is currently nested inside the container, append it back to #neo-ai-hud so it doesn't get destroyed.
            var elOverlay = document.getElementById('neo-edit-overlay');
            if (elOverlay && elOverlay.parentNode && elOverlay.parentNode !== document.getElementById('neo-ai-hud')) {
                var rHud = document.getElementById('neo-ai-hud');
                if (rHud) {
                    rHud.appendChild(elOverlay);
                }
            }

            var container = document.getElementById('neo-steps-container');
            if (!container) return;

            var currentBlock = window.neoCurrentBlock;
            var beforeSteps = window.neoBeforeSteps;
            var stepsSteps = window.neoStepsSteps;
            var afterSteps = window.neoAfterSteps;
            var canEdit = window.neoCanEdit;

            window.neoCurrentRenderedSteps = [];

            if (!window.neoBpListenerAttached) {
                var handler = function (e) {
                    var marker = e.target.closest('.neo-bp-marker');
                    if (marker) {
                        var idxInList = parseInt(marker.getAttribute('data-idx'));
                        if (!isNaN(idxInList)) {
                            if (!window.neoBreakpoints) window.neoBreakpoints = [];
                            var bpIdx = window.neoBreakpoints.indexOf(idxInList);
                            if (bpIdx === -1) {
                                window.neoBreakpoints.push(idxInList);
                            } else {
                                window.neoBreakpoints.splice(bpIdx, 1);
                            }
                            setSessionStorage('neoBreakpoints', JSON.stringify(window.neoBreakpoints));
                            window.neoRenderFullPrompt();
                        }
                    }
                };
                container.addEventListener('click', handler);
                window.neoBpListenerAttached = true;
            }

            if (!window.neoEditStepListenerAttached) {
                var editHandler = function (e) {
                    var target = e.target.closest('.neo-edit-step-icon');
                    if (target) {
                        e.stopPropagation();
                        var idx = parseInt(target.getAttribute('data-idx'));
                        window.neoStartEditingStep(idx);
                    }
                };
                container.addEventListener('click', editHandler);
                window.neoEditStepListenerAttached = true;
            }

            if (!window.neoRewindStepListenerAttached) {
                var rewindHandler = function (e) {
                    var target = e.target.closest('.rewind-hover-indicator');
                    if (target) {
                        e.stopPropagation();
                        var idx = parseInt(target.getAttribute('data-idx'));
                        if (!isNaN(idx)) {
                            window.neoConfirmCallback = function () {
                                window.neoSubmitAction({ action: 'REWIND', index: idx });
                            };
                            var confirmOverlay = document.getElementById('neo-confirm-overlay');
                            if (confirmOverlay) {
                                confirmOverlay.style.display = 'flex';
                            }
                        }
                    }
                };
                container.addEventListener('click', rewindHandler);
                window.neoRewindStepListenerAttached = true;
            }

            var pList = window.neoPerformedList || [];
            var aList = window.neoPlannedList || [];
            var hasMultiBlocks = (beforeSteps && beforeSteps.length > 0) || (afterSteps && afterSteps.length > 0);

            function escapeHtml(unsafe) {
                return (unsafe || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
            }

            function getCleanStepText(text) {
                return text ? text.replace(/^⚠️\s*/, '') : '';
            }

            function genHeadline(title, blockType) {
                var colorVar = 'var(--accent-primary)';
                if (blockType === 'steps') {
                    colorVar = 'var(--accent-purple)';
                } else if (blockType === 'after') {
                    colorVar = 'var(--accent-warning)';
                }
                return '<div class="neo-block-headline" style="color: ' + colorVar + '; border-left-color: ' + colorVar + '; margin-top: 14px; margin-bottom: 8px;">' + title + '</div>';
            }

            function genItem(stepText, statusStr, isInteractiveStep, localIndex) {
                var cleanText = getCleanStepText(stepText);
                var stepIdx = isInteractiveStep ? localIndex : -1;
                
                if (isInteractiveStep) {
                    window.neoCurrentRenderedSteps[localIndex] = cleanText;
                }

                var isBp = isInteractiveStep && window.neoBreakpoints && window.neoBreakpoints.indexOf(localIndex) !== -1;
                var bpDisplay = isBp ? '🛑' : '⚪';
                var bpOpacity = isBp ? '1' : '0.15';

                var classNames = 'neo-step-item';
                var statusHtml = '';
                
                if (statusStr === 'active') {
                    classNames += ' active';
                } else if (statusStr === 'done') {
                    classNames += ' completed';
                    if (isInteractiveStep) {
                        statusHtml = '<span class="checkmark" style="color: var(--accent-success); display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; box-sizing: border-box; font-size: 14px;">✔️</span>' +
                                     '<span class="rewind-hover-indicator" data-idx="' + localIndex + '" title="Rewind back to this step" style="cursor: pointer; color: var(--text-secondary); display: none; align-items: center; justify-content: center; width: 24px; height: 24px; box-sizing: border-box; font-size: 14px;"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"></path><path d="M3 3v5h5"></path></svg></span>';
                    } else {
                        statusHtml = '<span class="checkmark" style="color: var(--accent-success); display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; box-sizing: border-box; font-size: 14px;">✔️</span>';
                    }
                }

                if (stepText.indexOf('// [SKIPPED]') !== -1) {
                    classNames += ' skipped';
                    cleanText = cleanText.replace('// [SKIPPED] ', '');
                    stepText = stepText.replace('// [SKIPPED] ', '');
                }

                var editIconHtml = '';
                if (canEdit && (isInteractiveStep || statusStr === 'active')) {
                    editIconHtml = '<span class="neo-edit-step-icon" data-idx="' + localIndex + '" style="opacity: 0.5; cursor: pointer; padding: 2px 6px; display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; box-sizing: border-box; font-size: 14px;" title="Edit this step"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg></span>';
                }

                var rightActionsHtml = '';
                if (statusHtml || editIconHtml) {
                    rightActionsHtml = '<div style="margin-left: auto; display: inline-flex; align-items: center; gap: 8px; flex-shrink: 0; height: 24px;">' +
                        statusHtml +
                        editIconHtml +
                        '</div>';
                }

                var dragHandleHtml = (isInteractiveStep && statusStr !== 'active') ? '<div class="neo-step-drag-handle" style="cursor: grab; margin-right: 8px; opacity: 0.3;" title="Drag to reorder">☰</div>' : '';
                
                var bpMarkerHtml = '';
                if (isInteractiveStep || statusStr === 'active') {
                    bpMarkerHtml = '<span class="neo-bp-marker" data-idx="' + localIndex + '" style="opacity: ' + bpOpacity + '; cursor: pointer; font-size: 13px; font-weight: bold; margin-right: 8px; user-select: none;" title="Toggle breakpoint">' + bpDisplay + '</span>';
                } else if (statusStr !== 'done') {
                    bpMarkerHtml = '<span style="font-size: 12px; margin-right: 8px; display: inline-flex; align-items: center; justify-content: center; width: 20px; height: 20px;"><svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="var(--text-secondary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity:0.5;"><circle cx="12" cy="12" r="10"></circle></svg></span>';
                }

                var dragAttributes = (isInteractiveStep && statusStr !== 'active') ? ' draggable="true" ondragstart="window.neoDragStart(event, ' + localIndex + ')" ondragover="window.neoDragOver(event)" ondragenter="window.neoDragEnter(event)" ondragleave="window.neoDragLeave(event)" ondrop="window.neoDrop(event, ' + localIndex + ')" ondragend="window.neoDragEnd(event)"' : '';

                var stepNumHtml = '';
                if (statusStr === 'active') {
                    stepNumHtml = '<div class="neo-step-indicator" style="color: var(--accent-primary); width:30px;">➔</div>';
                } else {
                    stepNumHtml = '<div class="neo-step-indicator">' + (localIndex + 1) + '</div>';
                }

                var badgeHtml = '';
                if (statusStr === 'active' && hasMultiBlocks && currentBlock) {
                    var badgeColor = 'var(--accent-primary)';
                    if (currentBlock === 'steps') badgeColor = 'var(--accent-purple)';
                    else if (currentBlock === 'after') badgeColor = 'var(--accent-warning)';
                    badgeHtml = '<div id="neo-active-step-block-badge" style="font-size: 10px; font-weight: 700; text-transform: uppercase; margin-bottom: 6px; letter-spacing: 0.5px; color: ' + badgeColor + ';">' + currentBlock.toUpperCase() + '</div>';
                }

                var itemStyle = 'margin-bottom: 8px;';
                if (!isInteractiveStep && statusStr !== 'active') {
                    itemStyle += ' opacity: 0.75;';
                }

                return '<div class="' + classNames + '" style="' + itemStyle + '"' + dragAttributes + '>' +
                    badgeHtml +
                    '<div style="display: flex; align-items: center; width: 100%;">' +
                    dragHandleHtml +
                    bpMarkerHtml +
                    stepNumHtml +
                    '<div class="neo-step-content"' + (statusStr === 'active' ? ' id="neo-next-action"' : '') + ' style="flex-grow: 1;' + (statusStr === 'active' ? ' font-weight: 600;' : '') + '">' + escapeHtml(stepText) + '</div>' +
                    rightActionsHtml +
                    '</div></div>';
            }

            if (!window.neoFullPromptOpen) {
                if (aList && aList.length > 0) {
                    var activeIdx = pList.length;
                    container.innerHTML = '<div id="neo-planned-actions" class="active-step-group">' + genItem(aList[0], 'active', true, activeIdx) + '</div>';
                } else {
                    container.innerHTML = '<div style="text-align: center; color: var(--text-secondary); padding: 20px;">No active step</div>';
                }
                var addBtn = document.getElementById('neo-add-overlay-btn');
                if (addBtn) addBtn.style.display = 'none';
            } else {
                var fullHtml = '';

                if (!hasMultiBlocks) {
                    var histHtml = '';
                    for (var i = 0; i < pList.length; i++) {
                        histHtml += genItem(pList[i], 'done', true, i);
                    }
                    if (histHtml !== '') {
                        fullHtml += '<div id="neo-history-table" style="display:flex; flex-direction:column; width:100%;">' + histHtml + '</div>';
                    }

                    if (aList && aList.length > 0) {
                        var activeIdx = pList.length;
                        fullHtml += '<div id="neo-planned-actions" class="active-step-group">' + genItem(aList[0], 'active', true, activeIdx) + '</div>';
                        
                        var futHtml = '';
                        for (var j = 1; j < aList.length; j++) {
                            futHtml += genItem(aList[j], 'pending', true, activeIdx + j);
                        }
                        if (futHtml !== '') {
                            fullHtml += '<div id="neo-future-table" style="display:flex; flex-direction:column; width:100%;">' + futHtml + '</div>';
                        }
                    }
                } else {
                    var bSteps = beforeSteps || [];
                    var sSteps = stepsSteps || [];
                    var aSteps = afterSteps || [];

                    function getBlockPartition(blockName, rawList) {
                        if (currentBlock === blockName) {
                            return {
                                completed: pList,
                                active: (aList && aList.length > 0) ? aList[0] : null,
                                pending: (aList && aList.length > 1) ? aList.slice(1) : [],
                                isInteractive: true
                            };
                        } else {
                            var isPast = false;
                            if (currentBlock === 'steps' && blockName === 'before') {
                                isPast = true;
                            } else if (currentBlock === 'after' && (blockName === 'before' || blockName === 'steps')) {
                                isPast = true;
                            }
                            return {
                                completed: isPast ? rawList : [],
                                active: null,
                                pending: isPast ? [] : rawList,
                                isInteractive: false
                            };
                        }
                    }

                    var blocks = [
                        { name: 'before', title: 'Before', raw: bSteps },
                        { name: 'steps', title: 'Steps', raw: sSteps },
                        { name: 'after', title: 'After', raw: aSteps }
                    ];

                    for (var b = 0; b < blocks.length; b++) {
                        var blk = blocks[b];
                        if (blk.raw.length > 0 || currentBlock === blk.name) {
                            var partition = getBlockPartition(blk.name, blk.raw);
                            
                            var blockHtml = genHeadline(blk.title, blk.name);
                            var contentHtml = '';

                            if (blk.name === currentBlock) {
                                var blockHist = '';
                                for (var i = 0; i < partition.completed.length; i++) {
                                    blockHist += genItem(partition.completed[i], 'done', partition.isInteractive, i);
                                }
                                if (blockHist !== '') {
                                    contentHtml += '<div id="neo-history-table" style="display:flex; flex-direction:column; width:100%;">' + blockHist + '</div>';
                                }

                                if (partition.active) {
                                    var activeIdx = partition.completed.length;
                                    contentHtml += '<div id="neo-planned-actions" class="active-step-group">' + genItem(partition.active, 'active', true, activeIdx) + '</div>';
                                }

                                var blockFut = '';
                                var activeOffset = partition.active ? 1 : 0;
                                for (var i = 0; i < partition.pending.length; i++) {
                                    blockFut += genItem(partition.pending[i], 'pending', partition.isInteractive, partition.completed.length + activeOffset + i);
                                }
                                if (blockFut !== '') {
                                    contentHtml += '<div id="neo-future-table" style="display:flex; flex-direction:column; width:100%;">' + blockFut + '</div>';
                                }
                            } else {
                                var isPast = false;
                                if (currentBlock === 'steps' && blk.name === 'before') {
                                    isPast = true;
                                } else if (currentBlock === 'after' && (blk.name === 'before' || blk.name === 'steps')) {
                                    isPast = true;
                                }

                                var blockStepsHtml = '';
                                for (var i = 0; i < partition.completed.length; i++) {
                                    blockStepsHtml += genItem(partition.completed[i], 'done', partition.isInteractive, i);
                                }
                                for (var i = 0; i < partition.pending.length; i++) {
                                    blockStepsHtml += genItem(partition.pending[i], 'pending', partition.isInteractive, partition.completed.length + i);
                                }

                                if (blockStepsHtml !== '') {
                                    if (isPast) {
                                        contentHtml += '<div id="neo-history-table" style="display:flex; flex-direction:column; width:100%;">' + blockStepsHtml + '</div>';
                                    } else {
                                        contentHtml += '<div id="neo-future-table" style="display:flex; flex-direction:column; width:100%;">' + blockStepsHtml + '</div>';
                                    }
                                }
                            }

                            if (contentHtml !== '') {
                                fullHtml += blockHtml + contentHtml;
                            }
                        }
                    }
                }

                container.innerHTML = fullHtml;
                var addBtn = document.getElementById('neo-add-overlay-btn');
                if (addBtn) addBtn.style.display = 'block';
            }
        };

        window.neoToggleFullPrompt = function () {
            var btn = document.getElementById('neo-full-prompt-btn');

            if (window.neoFullPromptOpen) {
                window.neoFullPromptOpen = false;
                setSessionStorage('neoFullPromptOpen', 'false');
                btn.classList.remove('neo-btn-primary');
                document.getElementById('neo-ai-hud').classList.remove('expanded');
                btn.innerHTML = '▲ Show Full Prompt ▲';
            } else {
                window.neoFullPromptOpen = true;
                setSessionStorage('neoFullPromptOpen', 'true');
                btn.classList.add('neo-btn-primary');
                document.getElementById('neo-ai-hud').classList.add('expanded');
                btn.innerHTML = '▼ Hide Full Prompt ▼';
            }
            window.neoRenderFullPrompt();
            setTimeout(function () {
                if (window.neoClampHudViewport) window.neoClampHudViewport();
            }, 10);
        };

        window.neoClampHudViewport = function () {
            var hud = document.getElementById('neo-ai-hud');
            if (hud && hud.style.display !== 'none') {
                var rect = hud.getBoundingClientRect();
                var currentRight = parseFloat(hud.style.right) || 20;
                var currentBottom = parseFloat(hud.style.bottom) || 20;

                var maxRight = window.innerWidth - rect.width - 20;
                var maxBottom = window.innerHeight - rect.height - 20;

                var newRight = currentRight;
                var newBottom = currentBottom;

                if (newRight > maxRight) newRight = maxRight;
                if (newRight < 20) newRight = 20;
                if (newBottom > maxBottom) newBottom = maxBottom;
                if (newBottom < 20) newBottom = 20;

                if (newRight !== currentRight || newBottom !== currentBottom) {
                    hud.style.right = newRight + 'px';
                    hud.style.bottom = newBottom + 'px';
                    setSessionStorage('neoHudPosRight', newRight + 'px');
                    setSessionStorage('neoHudPosBottom', newBottom + 'px');

                    var minCircle = document.getElementById('neo-min-circle');
                    if (minCircle) {
                        minCircle.style.right = newRight + 'px';
                        minCircle.style.bottom = newBottom + 'px';
                    }
                    var helpOverlay = document.getElementById('neo-help-overlay');
                    if (helpOverlay) {
                        helpOverlay.style.right = (newRight + 405) + 'px';
                        helpOverlay.style.bottom = newBottom + 'px';
                    }
                    var bindingsDrawer = document.getElementById('bindingsDrawer');
                    if (bindingsDrawer) {
                        bindingsDrawer.style.right = (newRight + 405) + 'px';
                        bindingsDrawer.style.bottom = newBottom + 'px';
                    }
                }
            }
        };

        document.getElementById('neo-full-prompt-btn').addEventListener('click', window.neoToggleFullPrompt);

        window.addEventListener('resize', window.neoClampHudViewport);
    }

    window.neoPerformedList = performed;
    window.neoPlannedList = planned;
    window.neoHudAutoSkip = autoSkip;
    window.neoHudAction = window.neoHudAction || null;
    if (window.updateAutoSkipBtn) window.updateAutoSkipBtn();

    if (window.neoHudAutoSkip && window.neoPlannedList && window.neoPlannedList.length > 0) {
        var currentStepIdx = (window.neoPerformedList || []).length;
        if (window.neoBreakpoints && window.neoBreakpoints.indexOf(currentStepIdx) !== -1) {
            window.neoHudAutoSkip = false;
            if (window.updateAutoSkipBtn) window.updateAutoSkipBtn();
        }
    }

    var currentHud = document.getElementById('neo-ai-hud');
    var minCircle = document.getElementById('neo-min-circle');

    if (getSessionStorage('neoHudPosRight')) {
        var r = getSessionStorage('neoHudPosRight');
        var b = getSessionStorage('neoHudPosBottom');
        if (currentHud) { currentHud.style.right = r; currentHud.style.bottom = b; currentHud.style.left = 'auto'; currentHud.style.top = 'auto'; }
        if (minCircle) { minCircle.style.right = r; minCircle.style.bottom = b; minCircle.style.left = 'auto'; minCircle.style.top = 'auto'; }
        var hOverlay = document.getElementById('neo-help-overlay');
        var bDrawer = document.getElementById('bindingsDrawer');
        if (hOverlay) { hOverlay.style.right = (parseFloat(r) + 405) + 'px'; hOverlay.style.bottom = b; }
        if (bDrawer) { bDrawer.style.right = (parseFloat(r) + 385) + 'px'; bDrawer.style.bottom = b; }
    }

    var currentNextAction = (window.neoPlannedList && window.neoPlannedList.length > 0) ? window.neoPlannedList[0] : '';
    var currentPerfLen = (window.neoPerformedList || []).length;
    var currentAutoSkip = window.neoHudAutoSkip === true;
    var lastNextAction = getSessionStorage('neoLastNextAction');
    var lastPerfLen = getSessionStorage('neoLastPerfLen');
    var lastAutoSkip = getSessionStorage('neoLastAutoSkip');
    var lastReasoning = getSessionStorage('neoLastReasoning');

    var isNowLoading = reasoning === "Loading reasoning...";
    var wasLoading = lastReasoning === "Loading reasoning...";

    if (!isNowLoading) {
        // Conditions that always trigger a maximize, regardless of auto-skip state:
        // – The user manually paused (was in auto-skip, now is not)
        // – An error step is active
        // – Execution finished
        var shouldMaximize =
            (lastAutoSkip === 'true' && !currentAutoSkip) ||
            currentNextAction.startsWith('\u26a0\ufe0f') ||
            isFinished;

        // While auto-skip is active, suppress step-change/load-complete maximize triggers.
        // The HUD should stay minimized until the user explicitly pauses or an error occurs.
        if (!window.neoHudAutoSkip) {
            shouldMaximize = shouldMaximize ||
                (lastNextAction !== null && lastNextAction !== currentNextAction) ||
                (lastPerfLen !== null && parseInt(lastPerfLen) !== currentPerfLen) ||
                wasLoading;
        }

        if (shouldMaximize) {
            setSessionStorage('neoHudMinimized', 'false');
        }
    }
    setSessionStorage('neoLastNextAction', currentNextAction);
    setSessionStorage('neoLastPerfLen', currentPerfLen.toString());
    setSessionStorage('neoLastAutoSkip', currentAutoSkip ? 'true' : 'false');
    setSessionStorage('neoLastReasoning', reasoning || '');

    if (window.neoHudAutoSkip === true) {
        if (window.neoMinimizeHud) window.neoMinimizeHud();
    } else if (getSessionStorage('neoHudMinimized') === 'true') {
        if (window.neoMinimizeHud) window.neoMinimizeHud();
    } else {
        if (currentHud) currentHud.style.display = 'flex';
        if (minCircle) minCircle.style.display = 'none';
    }

    if (window.neoRenderFullPrompt) {
        window.neoRenderFullPrompt();
        var btn = document.getElementById('neo-full-prompt-btn');
        var rHud = document.getElementById('neo-ai-hud');
        if (window.neoFullPromptOpen) {
            if (rHud) rHud.classList.add('expanded');
            if (btn) {
                btn.classList.add('neo-btn-primary');
                btn.innerHTML = '▼ Hide Full Prompt ▼';
            }
        } else {
            if (rHud) rHud.classList.remove('expanded');
            if (btn) {
                btn.classList.remove('neo-btn-primary');
                btn.innerHTML = '▲ Show Full Prompt ▲';
            }
        }
    }



    // Update content
    var statusDiv = document.getElementById('neo-status-indicator');
    var plannedContainer = document.getElementById('neo-planned-actions');
    var nextActionDiv = document.getElementById('neo-next-action');
    var approveBtn = document.getElementById('neo-approve-btn');
    var skipBtn = document.getElementById('neo-skip-btn');
    var editBtn = document.getElementById('neo-edit-btn');
    var addBtn = document.getElementById('neo-add-overlay-btn');
    var rewindBtn = document.getElementById('neo-rewind-btn');

    var dumpContainer = document.getElementById('neo-dump-container');

    if (planned && planned.length > 0) {
        if (planned[0] && planned[0].startsWith && planned[0].startsWith('⚠️')) {
            statusDiv.innerText = 'Debug - Error';
            statusDiv.style.background = '#f44336';
            statusDiv.style.color = '#fff';
            if (dumpContainer) dumpContainer.style.display = 'block';
        } else {
            statusDiv.innerText = 'Pending';
            statusDiv.style.background = '#FF9800';
            statusDiv.style.color = '#000';
            if (dumpContainer) dumpContainer.style.display = 'none';
        }
        if (nextActionDiv) {
            nextActionDiv.innerText = planned[0];
        }
        if (plannedContainer) {
            plannedContainer.style.display = 'block';
        }

        // Update active step block badge
        var blockBadge = document.getElementById('neo-active-step-block-badge');
        if (blockBadge) {
            if (currentBlock && (currentBlock === 'before' || currentBlock === 'steps' || currentBlock === 'after')) {
                blockBadge.innerText = currentBlock.toUpperCase();
                blockBadge.style.display = 'inline-block';
                if (currentBlock === 'before') {
                    blockBadge.style.color = 'var(--accent-primary)';
                } else if (currentBlock === 'steps') {
                    blockBadge.style.color = 'var(--accent-purple)';
                } else if (currentBlock === 'after') {
                    blockBadge.style.color = 'var(--accent-warning)';
                }
            } else {
                blockBadge.style.display = 'none';
            }
        }

        var rContainer = document.getElementById('neo-reasoning-container');
        var pContainer = document.getElementById('neo-playbook-container');
        var rText = document.getElementById('neo-reasoning-text');
        var pReasoning = document.getElementById('neo-playbook-reasoning');

        if (rContainer) rContainer.style.display = 'none';
        if (pContainer) pContainer.style.display = 'none';

        if (isReplay) {
            if (pContainer) {
                pContainer.style.display = 'block';
                if (reasoning) {
                    pReasoning.innerText = "Original reasoning: " + reasoning;
                } else {
                    pReasoning.innerText = "";
                }
            }
        } else if (reasoning && reasoning !== "No reasoning") {
            if (rContainer) {
                rContainer.style.display = 'block';
                if (reasoning === "Loading reasoning...") {
                    rText.innerHTML = '<span style="color:#aaa; font-style:italic;">⏳ Loading reasoning from AI...</span>';
                } else {
                    rText.innerText = reasoning;
                }
            }
        }

        if (isFinished) {
            approveBtn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 6px;"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg> Save & Exit';
            approveBtn.title = 'Save & Exit';
            approveBtn.classList.add('neo-btn-primary');
            approveBtn.style.background = ''; // remove any inline
            approveBtn.dataset.isFinished = 'true';
        } else {
            approveBtn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 6px;"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg> Run';
            approveBtn.title = 'Run current active step only and pause';
            approveBtn.classList.remove('neo-btn-primary');
            approveBtn.style.background = ''; // use CSS styling
            approveBtn.dataset.isFinished = 'false';
        }

        approveBtn.disabled = false;
        approveBtn.style.opacity = '1';

        if (!canEdit) {
            var disabledMsg = "Editing is disabled: Data source is not a YAML file (e.g. CSV or hardcoded).";
            skipBtn.disabled = true; skipBtn.style.opacity = '0.3'; skipBtn.title = disabledMsg;
            editBtn.disabled = true; editBtn.style.opacity = '0.3'; editBtn.title = disabledMsg;
            addBtn.disabled = true; addBtn.style.opacity = '0.3'; addBtn.title = disabledMsg;
        } else {
            skipBtn.disabled = false;
            skipBtn.style.opacity = '1';

            editBtn.disabled = false;
            editBtn.style.opacity = '1';

            addBtn.disabled = false;
            addBtn.style.opacity = '1';
        }

        if (performed && performed.length > 0) {
            rewindBtn.disabled = false;
            rewindBtn.style.opacity = '1';
        } else {
            rewindBtn.disabled = true;
            rewindBtn.style.opacity = '0.5';
        }

        if (window.neoHudAutoSkip) {
            var hitBreakpoint = false;
            if (planned && planned.length > 0) {
                var currentStepIdx = (performed || []).length;
                if (window.neoBreakpoints && window.neoBreakpoints.indexOf(currentStepIdx) !== -1) {
                    hitBreakpoint = true;
                }
            }
            if (hitBreakpoint) {
                window.neoHudAutoSkip = false;
                if (window.updateAutoSkipBtn) window.updateAutoSkipBtn();
                if (window.neoMaximizeHud) window.neoMaximizeHud();
            } else if (reasoning && (reasoning.indexOf("Action Failed:") !== -1 || reasoning.indexOf("Assertion Failed:") !== -1 || reasoning.indexOf("Unexpected Error:") !== -1 || reasoning.indexOf("Max retries reached:") !== -1)) {
                window.neoHudAutoSkip = false;
                if (window.updateAutoSkipBtn) window.updateAutoSkipBtn();
                if (window.neoMaximizeHud) window.neoMaximizeHud();
            } else {
                window.neoSubmitAction({ action: "APPROVE" });
            }
        }
    } else {
        statusDiv.innerText = 'Waiting...';
        statusDiv.style.background = 'transparent';
        statusDiv.style.color = '#2196F3';
        if (plannedContainer) {
            plannedContainer.style.display = 'none';
        }
        var blockBadge = document.getElementById('neo-active-step-block-badge');
        if (blockBadge) blockBadge.style.display = 'none';
        var rContainer = document.getElementById('neo-reasoning-container');
        var pContainer = document.getElementById('neo-playbook-container');
        if (rContainer) rContainer.style.display = 'none';
        if (pContainer) pContainer.style.display = 'none';

        approveBtn.disabled = true;
        approveBtn.style.opacity = '0.5';
        skipBtn.disabled = true;
        skipBtn.style.opacity = '0.5';
        editBtn.style.opacity = '0.5';
        addBtn.disabled = true;
        addBtn.style.opacity = '0.5';
        rewindBtn.disabled = true;
        rewindBtn.style.opacity = '0.5';
    }


    // The outer container must always remain visible so the mini-circle pause button
    // is reachable during auto-skip. Visibility of the main HUD panel vs. the
    // mini-circle is controlled individually by neoMinimizeHud / neoMaximizeHud.
    var hudContainer = document.getElementById('neodymium-ai-hud-container');
    if (hudContainer) {
        hudContainer.setAttribute('data-hud-state-sig', stateSignature);
        hudContainer.style.display = '';
        // Ensure the HUD container is always the last child of document.body
        // to render on top of any dynamically added overlays/modals.
        if (document.body && document.body.lastChild !== hudContainer) {
            document.body.appendChild(hudContainer);
        }
    }

    if (isFinished && window.neoMaximizeHud) {
        window.neoMaximizeHud();
    }

    if (isFinished) {
        var finishOverlay = document.getElementById('neo-save-exit-overlay');
        if (finishOverlay) {
            var tbControls = document.getElementById('neo-toolbar-controls');
            var tbEdit = document.getElementById('neo-edit-toolbar');
            var bDrawer = document.getElementById('bindingsDrawer');
            if (tbControls) tbControls.style.display = 'none';
            if (tbEdit) tbEdit.style.display = 'none';
            if (bDrawer) bDrawer.style.display = 'none';

            finishOverlay.style.display = 'flex';
            var overlayHeader = document.getElementById('neo-overlay-header');
            var warningBlock = document.getElementById('neo-overlay-warning-block');
            var finishedBlock = document.getElementById('neo-overlay-finished-block');
            var modificationsBlock = document.getElementById('neo-overlay-modifications-block');
            var buttonsContainer = document.getElementById('neo-overlay-buttons-container');

            if (hudPromptChanged) {
                overlayHeader.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent-success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: inline-block; vertical-align: middle; margin-right: 8px;"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg> Neodymium Save & Exit';
                warningBlock.style.display = 'flex';
                finishedBlock.style.display = 'none';
                modificationsBlock.style.display = 'none';

                buttonsContainer.innerHTML = `
                    <button class="neo-btn" id="neo-exit-close-btn" style="flex: 1; color: #ef4444; border-color: rgba(239, 68, 68, 0.25); background: rgba(239, 68, 68, 0.03);"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: inline-block; vertical-align: middle; margin-right: 6px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg> Cancel & Close</button>
                    <button class="neo-btn" id="neo-exit-save-btn" style="flex: 1.5; color: #ffffff; background: var(--accent-success); border-color: var(--accent-success);"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: inline-block; vertical-align: middle; margin-right: 6px;"><polyline points="20 6 9 17 4 12"></polyline></svg> Save & Overwrite</button>
                `;

                document.getElementById('neo-exit-close-btn').addEventListener('click', function () {
                    window.neoSubmitAction({ action: "APPROVE" });
                });

                document.getElementById('neo-exit-save-btn').addEventListener('click', function () {
                    window.neoSubmitAction({ action: "SAVE_EXIT" });
                });
            } else {
                overlayHeader.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent-success)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: inline-block; vertical-align: middle; margin-right: 8px;"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg> Neodymium Finished';
                warningBlock.style.display = 'none';
                finishedBlock.style.display = 'flex';
                modificationsBlock.style.display = 'none';

                buttonsContainer.innerHTML = `
                    <button class="neo-btn" id="neo-overlay-close-btn" style="width: 100%; color: #ffffff; background: var(--accent-primary); border-color: var(--accent-primary);"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display: inline-block; vertical-align: middle; margin-right: 6px;"><polyline points="20 6 9 17 4 12"></polyline></svg> Close Test</button>
                `;

                document.getElementById('neo-overlay-close-btn').addEventListener('click', function () {
                    window.neoSubmitAction({ action: "APPROVE" });
                });
            }
        }
    }

    if (settingsJson) {
        try {
            var loadedSettings = JSON.parse(settingsJson);
            if (window.neoApplySettings) {
                window.neoApplySettings(loadedSettings);
            }
            var zInput = document.getElementById('neo-zoom-input');
            var tSelect = document.getElementById('neo-theme-select');
            if (zInput && loadedSettings.zoomFactor) zInput.value = loadedSettings.zoomFactor;
            if (tSelect && loadedSettings.theme) tSelect.value = loadedSettings.theme;
        } catch (e) { }
    }

})(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15], arguments[16], arguments[17], arguments[18], arguments[19], arguments[20], arguments[21]);
