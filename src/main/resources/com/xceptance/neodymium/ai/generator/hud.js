(function(hudHtml, planned, performed, autoSkip, hudPromptChanged, isFinished, canEdit, currentUnresolvedStep, dataBindings, configMap, reasoning, isReplay, lastFullPromptOpen) {
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

    var existingHud = document.getElementById('neo-ai-hud');
    if (existingHud && existingHud.getAttribute('data-hud-version') !== '3') {
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
            var h = document.getElementById('neo-ai-hud');
            if (h) h.style.display = 'none';
        };
        window.neoHudAutoSkip = autoSkip;
        document.getElementById('neo-autoskip-cb').checked = autoSkip;

        var hudElement = document.getElementById('neo-ai-hud');
        hudElement.style.opacity = '1';

        // Prevent mouse interactions inside the HUD from bubbling up and closing page overlays
        hudElement.addEventListener('click', function(e) { e.stopPropagation(); });
        hudElement.addEventListener('mousedown', function(e) { e.stopPropagation(); });
        hudElement.addEventListener('mouseup', function(e) { e.stopPropagation(); });

        document.getElementById('neo-min-btn').addEventListener('click', function(e) {
            e.stopPropagation();
            var content = document.getElementById('neo-hud-content');
            var hud = document.getElementById('neo-ai-hud');
            var title = document.getElementById('neo-header-title');
            if (this.innerHTML === '−') {
                content.style.display = 'none';
                hud.style.width = 'auto';
                this.innerHTML = '+';
                if (title) title.style.display = 'none';
                setSessionStorage('neoHudMinimized', 'true');
            } else {
                content.style.display = 'flex';
                hud.style.width = '350px';
                this.innerHTML = '−';
                if (title) title.style.display = 'flex';
                setSessionStorage('neoHudMinimized', 'false');
            }
        });

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

        document.getElementById('neo-autoskip-cb').addEventListener('change', function(e) {
            window.neoHudAutoSkip = e.target.checked;
            if (e.target.checked && !document.getElementById('neo-approve-btn').disabled && document.getElementById('neo-planned-actions').style.display !== 'none') {
                window.neoSubmitAction({ action: "APPROVE" });
            }
        });

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
                    var cb = document.getElementById('neo-autoskip-cb');
                    cb.checked = !cb.checked;
                    window.neoHudAutoSkip = cb.checked;
                    if (cb.checked && !document.getElementById('neo-approve-btn').disabled && document.getElementById('neo-planned-actions').style.display !== 'none') {
                        window.neoSubmitAction({ action: "APPROVE" });
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
            var overlay = document.getElementById('neo-full-prompt-overlay');
            var html = '';
            var pList = window.neoPerformedList || [];
            var aList = window.neoPlannedList || [];

            if (pList && pList.length > 0) {
                pList.forEach(function(step, i) {
                    html += '<div style="padding:4px; margin-bottom:2px; color:#888; border-left:2px solid #555;">' + 
                            '<span style="font-size:10px; margin-right:5px;">[' + (i + 1) + ']</span> ✓ ' + step + 
                            '</div>';
                });
            }
            
            if (aList && aList.length > 0) {
                html += '<div style="padding:6px; margin-bottom:2px; background:#333; color:#fff; border-left:3px solid #2196F3; font-weight:bold;">' + 
                        '<span style="font-size:10px; margin-right:5px; color:#2196F3;">[' + ((pList ? pList.length : 0) + 1) + ']</span> ➔ ' + aList[0] + 
                        '</div>';
                        
                if (aList.length > 1) {
                    aList.slice(1).forEach(function(step, i) {
                        var idx = (pList ? pList.length : 0) + 2 + i;
                        html += '<div style="padding:4px; margin-bottom:2px; color:#ccc; border-left:2px solid #444;">' + 
                                '<span style="font-size:10px; margin-right:5px;">[' + idx + ']</span> &nbsp; ' + step + 
                                '</div>';
                    });
                }
            }
            
            overlay.innerHTML = html;
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
        };

        document.getElementById('neo-full-prompt-btn').addEventListener('click', window.neoToggleFullPrompt);
        
        if (getSessionStorage('neoHudMinimized') === 'true') {
            document.getElementById('neo-min-btn').click();
        }
    }

    window.neoPerformedList = performed;
    window.neoPlannedList = planned;

    var currentHud = document.getElementById('neo-ai-hud');
    if (currentHud) {
        currentHud.style.display = 'flex';
    }

    if (window.neoFullPromptOpen && window.neoRenderFullPrompt) {
        window.neoRenderFullPrompt();
        var overlay = document.getElementById('neo-full-prompt-overlay');
        var btn = document.getElementById('neo-full-prompt-btn');
        if (overlay) overlay.style.display = 'flex';
        if (btn) btn.innerHTML = '▼ Hide Full Prompt ▼';
    }

    // Sync auto skip state
    document.getElementById('neo-autoskip-cb').checked = window.neoHudAutoSkip;

    // Update content
    var statusDiv = document.getElementById('neo-status-indicator');
    var plannedContainer = document.getElementById('neo-planned-actions');
    var nextActionDiv = document.getElementById('neo-next-action');
    var approveBtn = document.getElementById('neo-approve-btn');
    var skipBtn = document.getElementById('neo-skip-btn');
    var editBtn = document.getElementById('neo-edit-btn');
    var addBtn = document.getElementById('neo-add-overlay-btn');
    var rewindBtn = document.getElementById('neo-rewind-btn');

    if (planned && planned.length > 0) {
        if (planned[0] && planned[0].startsWith && planned[0].startsWith('⚠️')) {
            statusDiv.innerText = 'Debug - Error';
            statusDiv.style.background = '#f44336';
            statusDiv.style.color = '#fff';
        } else {
            statusDiv.innerText = 'Pending';
            statusDiv.style.background = '#FF9800';
            statusDiv.style.color = '#000';
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
            window.neoSubmitAction({ action: "APPROVE" });
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
    if (performed) {
        var historyList = document.getElementById('neo-history-list');
        if (historyList) {
            historyList.innerHTML = performed.map(function(a, index) {
                return '<li style="margin-bottom:3px; padding:2px; background:#444; cursor:pointer;" onclick="if(confirm(\'Rewind execution back to this step?\')) window.neoSubmitAction({ action: \'REWIND\', index: ' + index + ' });" title="Click to rewind">[' + index + '] ' + a + '</li>';
            }).join('');
        }
    }

})(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9], arguments[10], arguments[11], arguments[12]);
