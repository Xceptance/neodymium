(function(hudHtml, planned, performed, autoSkip, hudPromptChanged, isFinished, canEdit, currentUnresolvedStep, dataBindings, configMap, reasoning, isReplay, lastFullPromptOpen, lastBreakpointsStr, lastHelpShown, settingsJson) {
    if (!window.neoPollingMonitorActive) {
        window.neoPollingMonitorActive = true;
        setInterval(function() {
            var hudContainer = document.getElementById('neodymium-ai-hud-container');
            var hudEl = document.getElementById('neo-ai-hud');
            var stateStr = "not existing";
            if (hudContainer) {
                var containerHidden = hudContainer.style.display === 'none';
                var hudHidden = hudEl ? (hudEl.style.display === 'none') : true;
                if (containerHidden || hudHidden) {
                    stateStr = "exists -> not visible";
                } else {
                    stateStr = "exists -> visible";
                }
            }
            console.log("[Neodymium HUD Poller State] " + stateStr);
        }, 50);
    }

    var existingHud = document.getElementById('neodymium-ai-hud-container');
    console.log("[Neodymium HUD JS Debug] hud.js execution started. existingHud=" + (existingHud !== null) + ", Injected=" + window.neodymiumPromptGenerationHudInjected + ", isFinished=" + isFinished + ", currentUnresolvedStep=" + currentUnresolvedStep + ", reasoning=" + reasoning);

    window.neoCurrentUnresolvedStep = currentUnresolvedStep;
    window.neoDataBindings = dataBindings;
    window.neoConfigMap = configMap;

    function getSessionStorage(key) {
        try {
            return sessionStorage.getItem(key);
        } catch(e) {
            return null;
        }
    }

    function setSessionStorage(key, value) {
        try {
            sessionStorage.setItem(key, value);
        } catch(e) {}
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
        console.log("[Neodymium HUD JS Debug] Removing outdated HUD container version " + existingHud.getAttribute('data-hud-version'));
        existingHud.remove();
        window.neodymiumPromptGenerationHudInjected = false;
        existingHud = null;
    }

    if (!window.neodymiumPromptGenerationHudInjected || !existingHud) {
        console.log("[Neodymium HUD JS Debug] Creating new HUD element: injected=" + window.neodymiumPromptGenerationHudInjected + ", existingHud=" + (existingHud !== null));
        var hud = document.createElement('div');
        hud.innerHTML = hudHtml;
        document.body.appendChild(hud.firstChild);

        window.neodymiumPromptGenerationHudInjected = true;
        window.neoHudAction = null;
        window.neoSubmitAction = function(actionObj) {
            console.log("[Neodymium HUD JS Debug] window.neoSubmitAction called: " + JSON.stringify(actionObj));
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
        
        window.updateAutoSkipBtn = function() {
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
            autoSkipBtn.addEventListener('click', function(e) {
                console.log("[Neodymium HUD JS Click] Auto-Skip button clicked. Current window.neoHudAutoSkip: " + window.neoHudAutoSkip);
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
        hudElement.addEventListener('click', function(e) { e.stopPropagation(); });
        hudElement.addEventListener('mousedown', function(e) { e.stopPropagation(); });
        hudElement.addEventListener('mouseup', function(e) { e.stopPropagation(); });

        window.neoMinimizeHud = function() {
            console.log("[Neodymium HUD JS Call] neoMinimizeHud called");
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

        window.neoMaximizeHud = function() {
            console.log("[Neodymium HUD JS Call] neoMaximizeHud called");
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
            
            setTimeout(function() {
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
            minCircle.addEventListener('click', function(e) {
                console.log("[Neodymium HUD JS Click] Minimized circle clicked. neoIsDraggingHud: " + window.neoIsDraggingHud);
                if (window.neoIsDraggingHud) return;
                window.neoMaximizeHud();
            });
        }

        var minBtn = document.getElementById('neo-min-btn');
        if (minBtn) {
            minBtn.addEventListener('click', function(e) {
                console.log("[Neodymium HUD JS Click] Minimize button clicked.");
                e.stopPropagation();
                window.neoMinimizeHud();
            });
        }
        
        function makeDraggable(dragHandle, moveTargets) {
            if (!dragHandle || !moveTargets || moveTargets.length === 0) return;
            var isDown = false;
            var startX, startY, startRight, startBottom;

            dragHandle.addEventListener('mousedown', function(e) {
                if (e.target.tagName === 'BUTTON' || e.target.closest('button')) return;
                isDown = true;
                window.neoIsDraggingHud = false;
                startX = e.clientX;
                startY = e.clientY;
                
                startRight = parseFloat(moveTargets[0].style.right) || 20;
                startBottom = parseFloat(moveTargets[0].style.bottom) || 20;
                e.preventDefault();
            });

            document.addEventListener('mousemove', function(e) {
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
                
                moveTargets.forEach(function(target) {
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

            window.addEventListener('mouseup', function() {
                if (isDown) {
                    isDown = false;
                    setTimeout(function(){ window.neoIsDraggingHud = false; }, 50);
                }
            }, true);
        }

        makeDraggable(document.getElementById('neo-hud-header'), [document.getElementById('neo-ai-hud'), document.getElementById('neo-min-circle')]);
        makeDraggable(document.getElementById('neo-min-circle'), [document.getElementById('neo-ai-hud'), document.getElementById('neo-min-circle')]);

        var helpBtn = document.getElementById('neo-info-btn');
        var helpOverlay = document.getElementById('neo-help-overlay');

        if (helpBtn && helpOverlay) {
            var isHovering = false;
            helpBtn.addEventListener('mouseenter', function() {
                isHovering = true;
                helpOverlay.style.display = 'block';
            });
            helpBtn.addEventListener('mouseleave', function() {
                isHovering = false;
                setTimeout(function() {
                    if (!isHovering) {
                        helpOverlay.style.display = 'none';
                    }
                }, 100);
            });
            helpOverlay.addEventListener('mouseenter', function() {
                isHovering = true;
            });
            helpOverlay.addEventListener('mouseleave', function() {
                isHovering = false;
                setTimeout(function() {
                    if (!isHovering) {
                        helpOverlay.style.display = 'none';
                    }
                }, 100);
            });
            
            if (!window.neoHelpShown) {
                helpOverlay.style.display = 'block';
                window.neoHelpShown = true;
                setSessionStorage('neoHelpShown', 'true');
                setTimeout(function() {
                    if (!isHovering) {
                        helpOverlay.style.display = 'none';
                    }
                }, 4000);
            }
        }

        document.getElementById('neo-approve-btn').addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Approve/Run button clicked. isFinished: " + this.dataset.isFinished);
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

        document.getElementById('neo-skip-btn').addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Skip button clicked.");
            if (!this.disabled) {
                window.neoSubmitAction({ action: "SKIP" });
                this.disabled = true;
                this.style.opacity = '0.5';
            }
        });

        var editOverlay = document.getElementById('neo-edit-overlay');
        var editInput = document.getElementById('neo-edit-input');
        var editError = document.getElementById('neo-edit-error');
        var submitEditBtn = document.getElementById('neo-edit-submit-btn');

        function validateEditInput() {
            var newInstr = editInput.value;
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
                editError.innerText = "Warning: Possibly unbound variables found -> " + missingVars.join(", ") + " (Save allowed)";
                editError.style.display = 'block';
                editError.style.color = '#FFA500';
                submitEditBtn.disabled = false;
                submitEditBtn.style.opacity = '1';
            } else {
                editError.style.display = 'none';
                submitEditBtn.disabled = false;
                submitEditBtn.style.opacity = '1';
            }
        }

        editInput.addEventListener('input', validateEditInput);
        editInput.addEventListener('keyup', validateEditInput);
        editInput.addEventListener('change', validateEditInput);

        window.neoStartEditingStep = function(idx) {
            console.log("[Neodymium HUD JS Call] neoStartEditingStep called for index: " + idx);
            window.neoEditingStepIndex = idx;
            var stepText = (window.neoCurrentRenderedSteps && window.neoCurrentRenderedSteps[idx] !== undefined)
                ? window.neoCurrentRenderedSteps[idx]
                : (window.neoCurrentUnresolvedStep || document.getElementById('neo-next-action').innerText);
            
            editInput.value = stepText;
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
                    current = current.replace(/\$\{([^}]+)\}/g, function(match, key) {
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
                
                var newInstr = editInput.value;
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
            if (idx === (performed && performed.length > 0 ? performed.length : 0)) {
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
            if (stepItemEl && editOverlay) {
                stepItemEl.appendChild(editOverlay);
            }
            
            document.getElementById('neo-bindings-container').style.maxHeight = 'none';
            document.getElementById('bindingsDrawer').style.display = 'flex';
            document.getElementById('neo-toolbar-controls').style.display = 'none';
            document.getElementById('neo-edit-toolbar').style.display = 'flex';
            
            editOverlay.style.display = 'flex';
            editInput.focus();
        };

        document.getElementById('neo-edit-btn').addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Edit button clicked.");
            if (!this.disabled) {
                var currentStepIdx = (performed && performed.length > 0) ? performed.length : 0;
                window.neoStartEditingStep(currentStepIdx);
            }
        });

        var editCardIcon = document.getElementById('neo-edit-card-icon');
        if (editCardIcon) {
            editCardIcon.addEventListener('click', function() {
                console.log("[Neodymium HUD JS Click] Edit Card icon clicked.");
                var currentStepIdx = (performed && performed.length > 0) ? performed.length : 0;
                window.neoStartEditingStep(currentStepIdx);
            });
        }

        document.getElementById('neo-edit-cancel-btn').addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Edit Cancel button clicked.");
            document.getElementById('bindingsDrawer').style.display = 'none';
            document.getElementById('neo-toolbar-controls').style.display = 'flex';
            document.getElementById('neo-edit-toolbar').style.display = 'none';
            
            document.getElementById('neo-bindings-container').style.maxHeight = '100px';
            editOverlay.style.display = 'none';
        });

        submitEditBtn.addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Edit Submit button clicked. New Instruction: " + editInput.value);
            if (this.disabled) return;
            var newInstr = editInput.value;
            if (newInstr && newInstr.trim() !== '') {
                var updatedBindings = {};
                var inputs = document.querySelectorAll('.neo-binding-input');
                for (var i = 0; i < inputs.length; i++) {
                    updatedBindings[inputs[i].dataset.key] = inputs[i].value;
                }
                var editIdx = (window.neoEditingStepIndex !== undefined) ? window.neoEditingStepIndex : ((performed && performed.length > 0) ? performed.length : 0);
                window.neoSubmitAction({ action: "EDIT", instruction: newInstr.trim(), index: editIdx, bindings: updatedBindings });
                
                document.getElementById('bindingsDrawer').style.display = 'none';
                document.getElementById('neo-toolbar-controls').style.display = 'flex';
                document.getElementById('neo-edit-toolbar').style.display = 'none';
                document.getElementById('neo-bindings-container').style.maxHeight = '100px';
                editOverlay.style.display = 'none';
            } else {
                document.getElementById('bindingsDrawer').style.display = 'none';
                document.getElementById('neo-toolbar-controls').style.display = 'flex';
                document.getElementById('neo-edit-toolbar').style.display = 'none';
                document.getElementById('neo-bindings-container').style.maxHeight = '100px';
                editOverlay.style.display = 'none';
            }
        });
        document.getElementById('neo-rewind-btn').addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Rewind button clicked.");
            if (!this.disabled) {
                var histList = document.getElementById('neo-history-list');
                var count = histList ? histList.children.length : 0;
                if (count > 0) {
                    window.neoSubmitAction({ action: "REWIND", index: count - 1 });
                }
            }
        });

        var addOverlay = document.getElementById('neo-add-overlay');
        document.getElementById('neo-add-overlay-btn').addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Add Step Overlay button clicked.");
            if (!this.disabled) {
                addOverlay.style.display = 'flex';
                document.getElementById('neo-add-input').focus();
            }
        });

        document.getElementById('neo-add-cancel-btn').addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Add Step Cancel button clicked.");
            addOverlay.style.display = 'none';
            document.getElementById('neo-add-input').value = '';
        });

        document.getElementById('neo-add-submit-btn').addEventListener('click', function() {
            console.log("[Neodymium HUD JS Click] Add Step Submit button clicked. Input: " + document.getElementById('neo-add-input').value);
            var input = document.getElementById('neo-add-input').value.trim();
            if (input !== '') {
                window.neoSubmitAction({ action: "ADD", instruction: input });
                addOverlay.style.display = 'none';
                document.getElementById('neo-add-input').value = '';
            }
        });

        document.getElementById('neo-add-input').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                document.getElementById('neo-add-submit-btn').click();
            }
        });

        var dumpBtn = document.getElementById('neo-dump-btn');
        if (dumpBtn) {
            dumpBtn.addEventListener('click', function() {
                console.log("[Neodymium HUD JS Click] Dump button clicked.");
                if (!this.disabled) {
                    window.neoHudAction = JSON.stringify({ action: "DUMP" });
                    this.disabled = true;
                    this.style.opacity = '0.5';
                    this.innerText = '⏳ Dumping...';
                    setTimeout(function() {
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
            settingsBtn.addEventListener('click', function() {
                console.log("[Neodymium HUD JS Click] Settings button clicked.");
                settingsOverlay.style.display = 'flex';
            });
        }
        
        var settingsCancelBtn = document.getElementById('neo-settings-cancel-btn');
        if (settingsCancelBtn && settingsOverlay) {
            settingsCancelBtn.addEventListener('click', function() {
                console.log("[Neodymium HUD JS Click] Settings Cancel button clicked.");
                settingsOverlay.style.display = 'none';
            });
        }
        
        var settingsSubmitBtn = document.getElementById('neo-settings-submit-btn');
        if (settingsSubmitBtn && settingsOverlay) {
            settingsSubmitBtn.addEventListener('click', function() {
                console.log("[Neodymium HUD JS Click] Settings Submit button clicked.");
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
        
        window.neoApplySettings = function(settingsObj) {
            console.log("[Neodymium HUD JS Call] neoApplySettings called. settings: " + JSON.stringify(settingsObj));
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
        document.addEventListener('keydown', function(e) {
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
                    var toggleBtn = document.getElementById('neo-toggle-history');
                    if (toggleBtn) toggleBtn.click();
                }
            }
        }, true);

        // Also intercept keyup to prevent the application from reacting to the release of the shortcut keys
        document.addEventListener('keyup', function(e) {
            if (e.altKey && e.key) {
                var key = e.key.toLowerCase();
                if (key === 'a' || key === 's' || key === 'h') {
                    e.preventDefault();
                    e.stopPropagation();
                    e.stopImmediatePropagation();
                }
            }
        }, true);
        window.neoDragStart = function(e, idx) {
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', idx);
            e.target.style.opacity = '0.4';
            window.neoDraggedItem = e.target;
        };
        window.neoDragOver = function(e) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            return false;
        };
        window.neoDragEnter = function(e) {
            e.preventDefault();
            var target = e.target.closest('.neo-step-item');
            if (target && target !== window.neoDraggedItem) target.classList.add('drag-over');
        };
        window.neoDragLeave = function(e) {
            var target = e.target.closest('.neo-step-item');
            if (target) target.classList.remove('drag-over');
        };
        window.neoDrop = function(e, targetIdx) {
            e.stopPropagation();
            var target = e.target.closest('.neo-step-item');
            if (target) target.classList.remove('drag-over');
            var sourceIdx = parseInt(e.dataTransfer.getData('text/plain'), 10);
            if (sourceIdx !== targetIdx && !isNaN(sourceIdx)) {
                window.neoSubmitAction({ action: "REORDER", from: sourceIdx, to: targetIdx });
            }
            return false;
        };
        window.neoDragEnd = function(e) {
            e.target.style.opacity = '1';
            var items = document.querySelectorAll('.neo-step-item');
            for(var i = 0; i < items.length; i++) items[i].classList.remove('drag-over');
        };

        window.neoRenderFullPrompt = function() {
            console.log("[Neodymium HUD JS Call] neoRenderFullPrompt called");
            var historyContainer = document.getElementById('neo-history-table');
            var futureContainer = document.getElementById('neo-future-table');
            if (!historyContainer || !futureContainer) return;
            
            window.neoCurrentRenderedSteps = [];
            
            if (!window.neoBpListenerAttached) {
                var handler = function(e) {
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
                historyContainer.addEventListener('click', handler);
                futureContainer.addEventListener('click', handler);
                var plannedContainer = document.getElementById('neo-planned-actions');
                if (plannedContainer) plannedContainer.addEventListener('click', handler);
                window.neoBpListenerAttached = true;
            }

            if (!window.neoEditStepListenerAttached) {
                var editHandler = function(e) {
                    var target = e.target.closest('.neo-edit-step-icon');
                    if (target) {
                        e.stopPropagation();
                        var idx = parseInt(target.getAttribute('data-idx'));
                        window.neoStartEditingStep(idx);
                    }
                };
                historyContainer.addEventListener('click', editHandler);
                futureContainer.addEventListener('click', editHandler);
                window.neoEditStepListenerAttached = true;
            }

            var pList = window.neoPerformedList || [];
            var aList = window.neoPlannedList || [];

            function escapeHtml(unsafe) {
                return (unsafe || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
            }

            function getCleanStepText(text) {
                return text ? text.replace(/^⚠️\s*/, '') : '';
            }

            function genItem(stepText, statusStr) {
                var cleanText = getCleanStepText(stepText);
                var stepIdx = window.neoCurrentRenderedSteps.length;
                window.neoCurrentRenderedSteps.push(cleanText);
                
                var isBp = window.neoBreakpoints && window.neoBreakpoints.indexOf(stepIdx) !== -1;
                var bpDisplay = isBp ? '🛑' : '⚪';
                var bpOpacity = isBp ? '1' : '0.15';
                
                var classNames = 'neo-step-item';
                if (statusStr === 'done') {
                    classNames += ' completed';
                }
                if (stepText.indexOf('// [SKIPPED]') !== -1) {
                    classNames += ' skipped';
                    cleanText = cleanText.replace('// [SKIPPED] ', '');
                    stepText = stepText.replace('// [SKIPPED] ', '');
                }
                
                var editIconHtml = '';
                if (canEdit) {
                    editIconHtml = '<span class="neo-edit-step-icon" data-idx="' + stepIdx + '" style="margin-left: auto; opacity: 0.5; cursor: pointer; padding: 2px 6px; display: inline-flex; align-items: center; justify-content: center;" title="Edit this step"><i class="fa-solid fa-pen"></i></span>';
                }
                
                return '<div class="' + classNames + '" style="margin-bottom: 8px;" draggable="true" ondragstart="window.neoDragStart(event, ' + stepIdx + ')" ondragover="window.neoDragOver(event)" ondragenter="window.neoDragEnter(event)" ondragleave="window.neoDragLeave(event)" ondrop="window.neoDrop(event, ' + stepIdx + ')" ondragend="window.neoDragEnd(event)">' +
                       '<div style="display: flex; align-items: center; width: 100%;">' +
                       '<div class="neo-step-drag-handle" style="cursor: grab; margin-right: 8px; opacity: 0.3;" title="Drag to reorder">☰</div>' +
                       '<span class="neo-bp-marker" data-idx="' + stepIdx + '" style="opacity: ' + bpOpacity + '; cursor: pointer; font-size: 13px; font-weight: bold; margin-right: 8px; user-select: none;" title="Toggle breakpoint">' + bpDisplay + '</span>' +
                       '<div class="neo-step-indicator">' + (stepIdx + 1) + '</div>' +
                       '<div class="neo-step-content" style="flex-grow: 1;">' + escapeHtml(stepText) + '</div>' +
                       editIconHtml +
                       '</div></div>';
            }

            var histHtml = '';
            if (pList && pList.length > 0) {
                for (var i = 0; i < pList.length; i++) {
                    histHtml += genItem(pList[i], 'done');
                }
            }
            historyContainer.innerHTML = histHtml;

            var futHtml = '';
            if (aList && aList.length > 0) {
                var activeIdx = window.neoCurrentRenderedSteps.length;
                window.neoCurrentRenderedSteps.push(getCleanStepText(aList[0])); // increment index for the active step so future steps have correct numbers
                
                var activeElem = document.querySelector('.neo-step-item.active');
                if (activeElem) {
                    activeElem.setAttribute('draggable', 'true');
                    activeElem.setAttribute('ondragstart', 'window.neoDragStart(event, ' + activeIdx + ')');
                    activeElem.setAttribute('ondragover', 'window.neoDragOver(event)');
                    activeElem.setAttribute('ondragenter', 'window.neoDragEnter(event)');
                    activeElem.setAttribute('ondragleave', 'window.neoDragLeave(event)');
                    activeElem.setAttribute('ondrop', 'window.neoDrop(event, ' + activeIdx + ')');
                    activeElem.setAttribute('ondragend', 'window.neoDragEnd(event)');
                    
                    var activeBp = activeElem.querySelector('.neo-bp-col') || activeElem.querySelector('.neo-bp-marker');
                    if (activeBp) {
                        activeBp.className = 'neo-bp-marker';
                        activeBp.setAttribute('data-idx', activeIdx);
                        var isBp = window.neoBreakpoints && window.neoBreakpoints.indexOf(activeIdx) !== -1;
                        activeBp.innerText = isBp ? '🛑' : '⚪';
                        activeBp.style.opacity = isBp ? '1' : '0.15';
                    }

                    if (!activeElem.querySelector('.neo-step-drag-handle')) {
                        var normalView = document.getElementById('neo-active-step-normal-view');
                        if (normalView) {
                            var dragHandle = document.createElement('div');
                            dragHandle.className = 'neo-step-drag-handle';
                            dragHandle.style.cssText = 'cursor: grab; margin-right: 8px; opacity: 0.3;';
                            dragHandle.title = 'Drag to reorder';
                            dragHandle.innerText = '☰';
                            normalView.insertBefore(dragHandle, normalView.firstChild);
                        }
                    }
                }
                
                if (aList.length > 1) {
                    for (var j = 1; j < aList.length; j++) {
                        futHtml += genItem(aList[j], 'pending');
                    }
                }
            }
            futureContainer.innerHTML = futHtml;
        };

        window.neoToggleFullPrompt = function() {
            console.log("[Neodymium HUD JS Call] neoToggleFullPrompt called");
            var historyOverlay = document.getElementById('neo-history-overlay');
            var futureOverlay = document.getElementById('neo-future-overlay');
            var btn = document.getElementById('neo-full-prompt-btn');
            var plannedContainer = document.getElementById('neo-planned-actions');
            
            if (window.neoFullPromptOpen) {
                historyOverlay.style.display = 'none';
                futureOverlay.style.display = 'none';
                if (plannedContainer) plannedContainer.style.display = 'block';
                btn.classList.remove('neo-btn-primary');
                document.getElementById('neo-ai-hud').classList.remove('expanded');
                btn.innerHTML = '▲ Show Full Prompt ▲';
                window.neoFullPromptOpen = false;
                setSessionStorage('neoFullPromptOpen', 'false');
            } else {
                window.neoRenderFullPrompt();
                historyOverlay.style.display = 'flex';
                futureOverlay.style.display = 'flex';
                // we leave plannedContainer visible so it's sandwiched between history and future
                btn.classList.add('neo-btn-primary');
                document.getElementById('neo-ai-hud').classList.add('expanded');
                btn.innerHTML = '▼ Hide Full Prompt ▼';
                window.neoFullPromptOpen = true;
                setSessionStorage('neoFullPromptOpen', 'true');
            }
            setTimeout(function() {
                if (window.neoClampHudViewport) window.neoClampHudViewport();
            }, 10);
        };

        window.neoClampHudViewport = function() {
            console.log("[Neodymium HUD JS Call] neoClampHudViewport called");
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
        if ((lastNextAction !== null && lastNextAction !== currentNextAction) ||
            (lastPerfLen !== null && parseInt(lastPerfLen) !== currentPerfLen) ||
            (lastAutoSkip === 'true' && !currentAutoSkip) ||
            currentNextAction.startsWith('⚠️') ||
            wasLoading ||
            isFinished) {
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

    if (window.neoFullPromptOpen && window.neoRenderFullPrompt) {
        window.neoRenderFullPrompt();
        var historyOverlay = document.getElementById('neo-history-overlay');
        var futureOverlay = document.getElementById('neo-future-overlay');
        var btn = document.getElementById('neo-full-prompt-btn');
        var rHud = document.getElementById('neo-ai-hud');
        if (historyOverlay) historyOverlay.style.display = 'flex';
        if (futureOverlay) futureOverlay.style.display = 'flex';
        if (rHud) rHud.classList.add('expanded');
        if (btn) {
            btn.classList.add('neo-btn-primary');
            btn.innerHTML = '▼ Hide Full Prompt ▼';
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
        nextActionDiv.innerText = planned[0];
        plannedContainer.style.display = 'block';

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
            approveBtn.innerHTML = '<i class="fa-solid fa-save"></i> Save & Exit';
            approveBtn.title = 'Save & Exit';
            approveBtn.classList.add('neo-btn-primary');
            approveBtn.style.background = ''; // remove any inline
            approveBtn.dataset.isFinished = 'true';
        } else {
            approveBtn.innerHTML = '<i class="fa-solid fa-play"></i> Run';
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
        plannedContainer.style.display = 'none';
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

    // Update history
    var historyContainer = document.getElementById('neo-history-container');
    if (performed && performed.length > 0) {
        if (historyContainer) historyContainer.style.display = 'block';
        var historyList = document.getElementById('neo-history-list');
        if (historyList) {
            historyList.innerHTML = performed.map(function(a, index) {
                return '<li style="margin-bottom:3px; padding:2px; background:#444; cursor:pointer;" onclick="if(confirm(\'Rewind execution back to this step?\')) window.neoSubmitAction({ action: \'REWIND\', index: ' + index + ' });" title="Click to rewind">[' + index + '] ' + a + '</li>';
            }).join('');
        }
    }

    // Hide HUD container permanently if auto-skip is enabled
    var hudContainer = document.getElementById('neodymium-ai-hud-container');
    if (hudContainer) {
        if (autoSkip) {
            hudContainer.style.display = 'none';
        } else {
            hudContainer.style.display = '';
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
                overlayHeader.innerHTML = '<i class="fa-solid fa-floppy-disk" style="color: var(--accent-success);"></i> Neodymium Save & Exit';
                warningBlock.style.display = 'flex';
                finishedBlock.style.display = 'none';
                modificationsBlock.style.display = 'none'; 
                
                buttonsContainer.innerHTML = `
                    <button class="neo-btn" id="neo-exit-close-btn" style="flex: 1; color: #ef4444; border-color: rgba(239, 68, 68, 0.25); background: rgba(239, 68, 68, 0.03);"><i class="fa-solid fa-xmark"></i> Cancel & Close</button>
                    <button class="neo-btn" id="neo-exit-save-btn" style="flex: 1.5; color: #ffffff; background: var(--accent-success); border-color: var(--accent-success);"><i class="fa-solid fa-check"></i> Save & Overwrite</button>
                `;
                
                document.getElementById('neo-exit-close-btn').addEventListener('click', function() {
                    window.neoSubmitAction({ action: "APPROVE" });
                });
                
                document.getElementById('neo-exit-save-btn').addEventListener('click', function() {
                    window.neoSubmitAction({ action: "SAVE_EXIT" });
                });
            } else {
                overlayHeader.innerHTML = '<i class="fa-solid fa-circle-check" style="color: var(--accent-success);"></i> Neodymium Finished';
                warningBlock.style.display = 'none';
                finishedBlock.style.display = 'flex';
                modificationsBlock.style.display = 'none';
                
                buttonsContainer.innerHTML = `
                    <button class="neo-btn" id="neo-overlay-close-btn" style="width: 100%; color: #ffffff; background: var(--accent-primary); border-color: var(--accent-primary);"><i class="fa-solid fa-check"></i> Close Test</button>
                `;
                
                document.getElementById('neo-overlay-close-btn').addEventListener('click', function() {
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
        } catch(e) {}
    }

})(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14], arguments[15]);
