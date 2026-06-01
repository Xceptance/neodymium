(function(hudHtml, planned, performed, autoSkip, hudPromptChanged, isFinished, canEdit, currentUnresolvedStep, dataBindings, configMap, reasoning, isReplay, lastFullPromptOpen, lastBreakpointsStr, lastHelpShown) {
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

    var existingHud = document.getElementById('neodymium-ai-hud-container');
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
        window.neoSubmitAction = function(actionObj) {
            window.neoHudAction = JSON.stringify(actionObj);
            if (window.neoMinimizeHud) window.neoMinimizeHud();
        };
        window.neoHudAutoSkip = autoSkip;
        
        var autoSkipBtn = document.getElementById('neo-autoskip-btn');
        var autoSkipIcon = document.getElementById('neo-autoskip-icon');
        var autoSkipText = document.getElementById('neo-autoskip-text');
        
        window.updateAutoSkipBtn = function() {
            if (window.neoHudAutoSkip) {
                if (autoSkipIcon) autoSkipIcon.innerText = '⏸️';
                if (autoSkipText) autoSkipText.innerText = 'Pause';
                if (autoSkipBtn) autoSkipBtn.style.background = '#FF9800';
            } else {
                if (autoSkipIcon) autoSkipIcon.innerText = '⏩';
                if (autoSkipText) autoSkipText.innerText = 'Fast-Forward';
                if (autoSkipBtn) autoSkipBtn.style.background = '#4CAF50';
            }
        };
        window.updateAutoSkipBtn();

        if (autoSkipBtn) {
            autoSkipBtn.addEventListener('click', function(e) {
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
                if (window.neoIsDraggingHud) return;
                window.neoMaximizeHud();
            });
        }

        var minBtn = document.getElementById('neo-min-btn');
        if (minBtn) {
            minBtn.addEventListener('click', function(e) {
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
                if (newRight < 0) newRight = 0;
                if (newBottom < 0) newBottom = 0;
                if (newRight + w > window.innerWidth) newRight = window.innerWidth - w;
                if (newBottom + h > window.innerHeight) newBottom = window.innerHeight - h;
                
                moveTargets.forEach(function(target) {
                    target.style.right = newRight + 'px';
                    target.style.bottom = newBottom + 'px';
                    target.style.left = 'auto';
                    target.style.top = 'auto';
                });
                
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

        document.getElementById('neo-edit-btn').addEventListener('click', function() {
            if (!this.disabled) {
                editInput.value = window.neoCurrentUnresolvedStep || document.getElementById('neo-next-action').innerText;
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
                                valHtml = '<input type="text" class="neo-binding-input" data-key="' + key + '" value="' + safeVal + '" style="width:100%; box-sizing:border-box; background:#333; color:#fff; border:1px solid #555; padding:2px; border-radius:2px;">';
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
                
                // Allow table to grow up to a large height and expand HUD top dynamically
                document.getElementById('neo-ai-hud').style.width = '600px';
                document.getElementById('neo-bindings-container').style.maxHeight = 'none';
                document.getElementById('neo-hud-content').style.maxHeight = 'none';
                
                editOverlay.style.display = 'flex';
                editInput.focus();
            }
        });

        document.getElementById('neo-edit-cancel-btn').addEventListener('click', function() {
            document.getElementById('neo-ai-hud').style.width = '350px';
            document.getElementById('neo-bindings-container').style.maxHeight = '100px';
            document.getElementById('neo-hud-content').style.maxHeight = '400px';
            editOverlay.style.display = 'none';
        });

        submitEditBtn.addEventListener('click', function() {
            if (this.disabled) return;
            var newInstr = editInput.value;
            if (newInstr && newInstr.trim() !== '') {
                var updatedBindings = {};
                var inputs = document.querySelectorAll('.neo-binding-input');
                for (var i = 0; i < inputs.length; i++) {
                    updatedBindings[inputs[i].dataset.key] = inputs[i].value;
                }
                var pIdx = (performed && performed.length > 0) ? performed.length : 0;
                window.neoSubmitAction({ action: "EDIT", instruction: newInstr.trim(), index: pIdx, bindings: updatedBindings });
                
                document.getElementById('neo-ai-hud').style.width = '350px';
                document.getElementById('neo-bindings-container').style.maxHeight = '100px';
                document.getElementById('neo-hud-content').style.maxHeight = '400px';
                editOverlay.style.display = 'none';
            } else {
                document.getElementById('neo-ai-hud').style.width = '350px';
                document.getElementById('neo-bindings-container').style.maxHeight = '100px';
                document.getElementById('neo-hud-content').style.maxHeight = '400px';
                editOverlay.style.display = 'none';
            }
        });

        document.getElementById('neo-rewind-btn').addEventListener('click', function() {
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
            if (!this.disabled) {
                addOverlay.style.display = 'flex';
                document.getElementById('neo-add-input').focus();
            }
        });

        document.getElementById('neo-add-cancel-btn').addEventListener('click', function() {
            addOverlay.style.display = 'none';
            document.getElementById('neo-add-input').value = '';
        });

        document.getElementById('neo-add-submit-btn').addEventListener('click', function() {
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

        window.neoRenderFullPrompt = function() {
            var table = document.getElementById('neo-full-prompt-table');
            if (!table) return;
            
            window.neoCurrentRenderedSteps = [];
            
            if (!window.neoBpListenerAttached) {
                table.addEventListener('click', function(e) {
                    var td = e.target.closest('td.neo-bp-col');
                    if (td) {
                        var idxInList = parseInt(td.getAttribute('data-idx'));
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
                });
                window.neoBpListenerAttached = true;
            }

            var html = '';
            var pList = window.neoPerformedList || [];
            var aList = window.neoPlannedList || [];

            function getStatusIcon(statusStr) {
                if (statusStr === 'error') return '<span style="color:#f44336; font-size:14px; font-weight:bold;">✕</span>';
                if (statusStr === 'pending') return '<span style="color:#2196F3; font-size:14px; font-weight:bold;">➔</span>';
                if (statusStr === 'done') return '<span style="color:#4CAF50; font-size:14px; font-weight:bold;">✓</span>';
                return '';
            }

            function escapeHtml(unsafe) {
                return (unsafe || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
            }

            function getCleanStepText(text) {
                return text ? text.replace(/^⚠️\s*/, '') : '';
            }

            function genRow(stepText, statusStr, isCurrent) {
                var cleanText = getCleanStepText(stepText);
                var stepIdx = window.neoCurrentRenderedSteps.length;
                window.neoCurrentRenderedSteps.push(cleanText);
                
                var isBp = window.neoBreakpoints && window.neoBreakpoints.indexOf(stepIdx) !== -1;
                var bpDisplay = isBp ? '🛑' : '<span style="opacity:0.2;">⚪</span>';
                
                var rowStyle = 'border-bottom:1px solid #333;';
                if (isCurrent) {
                    rowStyle += ' background:#333; border-left:3px solid #2196F3; font-weight:bold;';
                } else if (statusStr === 'done') {
                    rowStyle += ' color:#888; border-left:2px solid #555;';
                } else {
                    rowStyle += ' color:#aaa; border-left:2px solid #444;';
                }

                return '<tr style="' + rowStyle + '">' +
                       '<td class="neo-bp-col" data-idx="' + stepIdx + '" style="width:25px; text-align:center; padding:6px; cursor:pointer;" title="Click to toggle breakpoint">' + bpDisplay + '</td>' +
                       '<td style="padding:6px; word-break:break-word;">' + escapeHtml(stepText) + '</td>' +
                       '<td style="width:25px; text-align:center; padding:6px;">' + getStatusIcon(statusStr) + '</td>' +
                       '</tr>';
            }

            if (pList && pList.length > 0) {
                pList.forEach(function(step) {
                    html += genRow(step, 'done', false);
                });
            }
            
            if (aList && aList.length > 0) {
                var currentStep = aList[0];
                var status = currentStep.startsWith('⚠️') ? 'error' : 'pending';
                html += genRow(currentStep, status, true);
                
                if (aList.length > 1) {
                    aList.slice(1).forEach(function(step) {
                        html += genRow(step, '', false);
                    });
                }
            }
            table.innerHTML = html;
        };

        window.neoToggleFullPrompt = function() {
            var overlay = document.getElementById('neo-full-prompt-overlay');
            var btn = document.getElementById('neo-full-prompt-btn');
            
            if (window.neoFullPromptOpen) {
                overlay.style.display = 'none';
                btn.innerHTML = '▲ Show Full Prompt ▲';
                window.neoFullPromptOpen = false;
                setSessionStorage('neoFullPromptOpen', 'false');
            } else {
                window.neoRenderFullPrompt();
                overlay.style.display = 'flex';
                btn.innerHTML = '▼ Hide Full Prompt ▼';
                window.neoFullPromptOpen = true;
                setSessionStorage('neoFullPromptOpen', 'true');
            }
            
            setTimeout(function() {
                var hud = document.getElementById('neo-ai-hud');
                if (!hud) return;
                var rect = hud.getBoundingClientRect();
                var currentBottom = parseFloat(hud.style.bottom) || 20;
                
                if (currentBottom + rect.height > window.innerHeight) {
                    var newBottom = window.innerHeight - rect.height;
                    if (newBottom < 0) newBottom = 0;
                    hud.style.bottom = newBottom + 'px';
                    setSessionStorage('neoHudPosBottom', newBottom + 'px');
                    var minCircle = document.getElementById('neo-min-circle');
                    if (minCircle) minCircle.style.bottom = newBottom + 'px';
                }
            }, 10);
        };

        document.getElementById('neo-full-prompt-btn').addEventListener('click', window.neoToggleFullPrompt);
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
    }

    var currentNextAction = (window.neoPlannedList && window.neoPlannedList.length > 0) ? window.neoPlannedList[0] : '';
    var currentPerfLen = (window.neoPerformedList || []).length;
    var currentAutoSkip = window.neoHudAutoSkip === true;
    var lastNextAction = getSessionStorage('neoLastNextAction');
    var lastPerfLen = getSessionStorage('neoLastPerfLen');
    var lastAutoSkip = getSessionStorage('neoLastAutoSkip');

    if ((lastNextAction !== null && lastNextAction !== currentNextAction) || 
        (lastPerfLen !== null && parseInt(lastPerfLen) !== currentPerfLen) ||
        (lastAutoSkip === 'true' && !currentAutoSkip) ||
        currentNextAction.startsWith('⚠️') ||
        isFinished) {
        setSessionStorage('neoHudMinimized', 'false');
    }
    setSessionStorage('neoLastNextAction', currentNextAction);
    setSessionStorage('neoLastPerfLen', currentPerfLen.toString());
    setSessionStorage('neoLastAutoSkip', currentAutoSkip ? 'true' : 'false');

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
        var overlay = document.getElementById('neo-full-prompt-overlay');
        var btn = document.getElementById('neo-full-prompt-btn');
        if (overlay) overlay.style.display = 'flex';
        if (btn) btn.innerHTML = '▼ Hide Full Prompt ▼';
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
            approveBtn.innerHTML = '💾';
            approveBtn.title = 'Save & Exit';
            approveBtn.style.background = '#2196F3';
            approveBtn.dataset.isFinished = 'true';
        } else {
            approveBtn.innerHTML = '✓';
            approveBtn.title = 'Approve [Ctrl+Enter or Alt+A]';
            approveBtn.style.background = '#4CAF50';
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
    } else {
        if (historyContainer) historyContainer.style.display = 'none';
    }

})(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12], arguments[13], arguments[14]);
