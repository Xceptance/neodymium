(function(hudHtml, planned, performed, autoSkip, hudPromptChanged, isFinished, canEdit, currentUnresolvedStep, dataBindings, configMap) {
    window.neoCurrentUnresolvedStep = currentUnresolvedStep;
    window.neoDataBindings = dataBindings;
    window.neoConfigMap = configMap;

    if (!window.neodymiumPromptGenerationHudInjected || !document.getElementById('neo-ai-hud')) {
        var hud = document.createElement('div');
        hud.innerHTML = hudHtml;
        document.body.appendChild(hud.firstChild);

        window.neodymiumPromptGenerationHudInjected = true;
        window.neoHudAction = null;
        window.neoHudAutoSkip = autoSkip;
        document.getElementById('neo-autoskip-cb').checked = autoSkip;

        var hudElement = document.getElementById('neo-ai-hud');
        hudElement.style.opacity = '1';

        document.getElementById('neo-approve-btn').addEventListener('click', function() {
            if (!this.disabled) {
                if (this.dataset.isFinished === 'true') {
                    window.neoHudAction = JSON.stringify({ action: "SAVE_EXIT" });
                } else {
                    window.neoHudAction = JSON.stringify({ action: "APPROVE" });
                }
                this.disabled = true;
                this.style.opacity = '0.5';
            }
        });

        document.getElementById('neo-skip-btn').addEventListener('click', function() {
            if (!this.disabled) {
                window.neoHudAction = JSON.stringify({ action: "SKIP" });
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
                document.getElementById('neo-ai-hud').style.top = '20px';
                document.getElementById('neo-ai-hud').style.bottom = '20px';
                document.getElementById('neo-bindings-container').style.maxHeight = 'none';
                document.getElementById('neo-ai-hud').style.maxHeight = 'none';
                document.getElementById('neo-hud-content').style.maxHeight = 'none';
                
                editOverlay.style.display = 'flex';
                editInput.focus();
            }
        });

        document.getElementById('neo-edit-cancel-btn').addEventListener('click', function() {
            document.getElementById('neo-ai-hud').style.width = '350px';
            document.getElementById('neo-ai-hud').style.top = 'auto';
            document.getElementById('neo-ai-hud').style.bottom = '20px';
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
                window.neoHudAction = JSON.stringify({ action: "EDIT", instruction: newInstr.trim(), index: pIdx, bindings: updatedBindings });
                
                document.getElementById('neo-ai-hud').style.width = '350px';
                document.getElementById('neo-ai-hud').style.top = 'auto';
                document.getElementById('neo-ai-hud').style.bottom = '20px';
                document.getElementById('neo-bindings-container').style.maxHeight = '100px';
                document.getElementById('neo-hud-content').style.maxHeight = '400px';
                editOverlay.style.display = 'none';
            } else {
                document.getElementById('neo-ai-hud').style.width = '350px';
                document.getElementById('neo-ai-hud').style.top = 'auto';
                document.getElementById('neo-ai-hud').style.bottom = '20px';
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
                    window.neoHudAction = JSON.stringify({ action: "REWIND", index: count - 1 });
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
                window.neoHudAction = JSON.stringify({ action: "ADD", instruction: input });
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
                window.neoHudAction = JSON.stringify({ action: "APPROVE" });
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

            if (e.altKey && e.key.toLowerCase() === 'a') {
                e.preventDefault();
                var btn = document.getElementById('neo-approve-btn');
                if (!btn.disabled) btn.click();
            } else if (e.altKey && e.key.toLowerCase() === 's') {
                e.preventDefault();
                var cb = document.getElementById('neo-autoskip-cb');
                cb.checked = !cb.checked;
                cb.dispatchEvent(new Event('change'));
            } else if (e.altKey && e.key.toLowerCase() === 'h') {
                e.preventDefault();
                var toggleBtn = document.getElementById('neo-toggle-history');
                if (toggleBtn) toggleBtn.click();
            }
        });
    }

    // Sync auto skip state
    document.getElementById('neo-autoskip-cb').checked = window.neoHudAutoSkip;

    // Update content
    var statusDiv = document.getElementById('neo-status-indicator');
    var plannedContainer = document.getElementById('neo-planned-actions');
    var nextActionDiv = document.getElementById('neo-next-action');
    var allPlannedDiv = document.getElementById('neo-all-planned');
    var approveBtn = document.getElementById('neo-approve-btn');
    var skipBtn = document.getElementById('neo-skip-btn');
    var editBtn = document.getElementById('neo-edit-btn');
    var addBtn = document.getElementById('neo-add-overlay-btn');
    var rewindBtn = document.getElementById('neo-rewind-btn');

    if (planned && planned.length > 0) {
        statusDiv.innerText = 'Pending';
        statusDiv.style.color = '#FF9800';
        nextActionDiv.innerText = planned[0];
        if (planned.length > 1) {
            allPlannedDiv.innerHTML = '<strong>Subsequent Actions:</strong><ol style="margin:5px 0 0 0; padding-left:20px; font-size:11px; color:#ccc;">' + planned.slice(1).map(function(a) { return '<li>' + a + '</li>'; }).join('') + '</ol>';
        } else {
            allPlannedDiv.innerHTML = '';
        }
        plannedContainer.style.display = 'block';

        if (isFinished) {
            approveBtn.innerHTML = '💾';
            approveBtn.title = 'Save & Exit';
            approveBtn.style.background = '#2196F3';
            approveBtn.dataset.isFinished = 'true';
        } else {
            approveBtn.innerHTML = '✓';
            approveBtn.title = 'Approve [Alt+A]';
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
            window.neoHudAction = JSON.stringify({ action: "APPROVE" });
        }
    } else {
        statusDiv.innerText = 'Waiting...';
        statusDiv.style.color = '#2196F3';
        plannedContainer.style.display = 'none';
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
                return '<li style="margin-bottom:3px; padding:2px; background:#444; cursor:pointer;" onclick="if(confirm(\'Rewind execution back to this step?\')) window.neoHudAction = JSON.stringify({ action: \'REWIND\', index: ' + index + ' });" title="Click to rewind">[' + index + '] ' + a + '</li>';
            }).join('');
        }
    }

})(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7], arguments[8], arguments[9]);
