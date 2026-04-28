package com.xceptance.neodymium.ai.generator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PromptGenerationHudHelper {

    private static final Logger LOG = LoggerFactory.getLogger(PromptGenerationHudHelper.class);

    private static final String HUD_SCRIPT = """
            (function(planned, performed, autoSkip, hudPromptChanged, isFinished) {
                if (!window.neodymiumPromptGenerationHudInjected || !document.getElementById('neo-ai-hud')) {
                    var hudHtml = `<div id="neo-ai-hud" style="position:fixed; bottom:20px; right:20px; width:350px; background:rgba(30,30,30,0.95); border:1px solid #555; border-radius:8px; z-index:999999; font-family:sans-serif; color:white; box-shadow:0 4px 15px rgba(0,0,0,0.5); overflow:hidden; display:flex; flex-direction:column; transition:opacity 0.3s; opacity:0;">

                        <div id="neo-add-overlay" style="display:none; position:absolute; top:0; left:0; width:100%; height:100%; background:rgba(30,30,30,0.98); z-index:10000; flex-direction:column; padding:15px; box-sizing:border-box; justify-content:center;">
                            <div style="color:#2196F3; font-weight:bold; margin-bottom:10px; font-size:14px;">➕ Insert Action Before Current</div>
                            <div style="font-size:11px; color:#aaa; margin-bottom:15px; line-height:1.4;">This action will be inserted and executed immediately before the currently pending step, pushing the rest down.</div>
                            <input type="text" id="neo-add-input" placeholder="e.g. Type 'foo' into search" style="width:100%; padding:10px; margin-bottom:15px; box-sizing:border-box; background:#222; color:white; border:1px solid #555; border-radius:4px; font-size:13px;">
                            <div style="display:flex; justify-content:flex-end; gap:10px;">
                                <button id="neo-add-cancel-btn" style="background:#555; color:white; border:none; padding:8px 15px; border-radius:4px; cursor:pointer; font-weight:bold;">Cancel</button>
                                <button id="neo-add-submit-btn" style="background:#2196F3; color:white; border:none; padding:8px 15px; border-radius:4px; cursor:pointer; font-weight:bold;">Insert</button>
                            </div>
                        </div>

                        <div id="neo-edit-overlay" style="display:none; position:absolute; top:0; left:0; width:100%; height:100%; background:rgba(30,30,30,0.98); z-index:10000; flex-direction:column; padding:15px; box-sizing:border-box; justify-content:center;">
                            <div style="color:#FF9800; font-weight:bold; margin-bottom:10px; font-size:14px;">✎ Edit Current Action</div>
                            <div style="font-size:11px; color:#aaa; margin-bottom:15px; line-height:1.4;">Modify the instruction. It will be replaced and re-evaluated.</div>
                            <input type="text" id="neo-edit-input" placeholder="" style="width:100%; padding:10px; margin-bottom:15px; box-sizing:border-box; background:#222; color:white; border:1px solid #555; border-radius:4px; font-size:13px;">
                            <div style="display:flex; justify-content:flex-end; gap:10px;">
                                <button id="neo-edit-cancel-btn" style="background:#555; color:white; border:none; padding:8px 15px; border-radius:4px; cursor:pointer; font-weight:bold;">Cancel</button>
                                <button id="neo-edit-submit-btn" style="background:#FF9800; color:white; border:none; padding:8px 15px; border-radius:4px; cursor:pointer; font-weight:bold;">Save Edit</button>
                            </div>
                        </div>

                        <div id="neo-hud-header" style="background:#2196F3; padding:8px 12px; font-weight:bold; font-size:14px; display:flex; justify-content:space-between; align-items:center; cursor:move;">
                            <span style="display:flex; align-items:center; gap:5px;">🤖 Neodymium AI <span id="neo-status-indicator" style="font-size:10px; background:#FF9800; padding:2px 6px; border-radius:10px;">Pending</span></span>
                            <div style="display:flex; gap:8px;">
                                <button id="neo-min-btn" style="background:transparent; color:white; border:none; cursor:pointer; font-size:16px; padding:0;">−</button>
                            </div>
                        </div>

                        <div id="neo-hud-content" style="padding:12px; flex-grow:1; display:flex; flex-direction:column; gap:10px; max-height:400px; overflow-y:auto;">
                            <div id="neo-planned-actions" style="display:none; background:#222; border:1px solid #444; padding:10px; border-radius:4px;">
                                <div style="font-size:11px; color:#888; text-transform:uppercase; margin-bottom:5px;">Next Action</div>
                                <div style="display:flex; align-items:flex-start; margin-bottom:5px;">
                                    <div style="flex-grow:1; position:relative; background:#333; padding:8px 8px 8px 24px; border-radius:4px; border:1px solid #555;">
                                        <button id="neo-edit-btn" disabled title="Edit" style="position:absolute; top:4px; left:4px; background:transparent; color:#aaa; border:none; cursor:pointer; font-size:12px; opacity:0.5; padding:0;">✎</button>
                                        <div id="neo-next-action" style="font-size:13px; color:#fff; word-break:break-word;"></div>
                                    </div>
                                    <div style="display:flex; flex-direction:column; gap:5px; margin-left:10px; margin-top:2px;">
                                        <button id="neo-approve-btn" disabled title="Approve [Alt+A]" style="background:#4CAF50; color:white; border:none; cursor:pointer; font-size:14px; opacity:0.5; width:24px; height:24px; border-radius:3px; display:flex; align-items:center; justify-content:center; font-weight:bold;">✓</button>
                                        <button id="neo-skip-btn" disabled title="Skip/Reject" style="background:#f44336; color:white; border:none; cursor:pointer; font-size:14px; opacity:0.5; width:24px; height:24px; border-radius:3px; display:flex; align-items:center; justify-content:center; font-weight:bold;">✕</button>
                                    </div>
                                </div>
                                <div style="display:flex; gap:5px; margin-bottom:10px;">
                                    <button id="neo-rewind-btn" disabled title="Rewind to previous step" style="flex:1; padding:6px; background:#444; border:1px dashed #666; color:#bbb; border-radius:4px; cursor:pointer; font-size:11px; opacity:0.5; text-align:center;">⏪ Back</button>
                                    <button id="neo-add-overlay-btn" disabled style="flex:1; padding:6px; background:#444; border:1px dashed #666; color:#bbb; border-radius:4px; cursor:pointer; font-size:11px; opacity:0.5; text-align:center;">Insert new step</button>
                                </div>
                                <div id="neo-all-planned"></div>
                            </div>

                            <label style="cursor:pointer; font-size:12px; display:flex; align-items:center; user-select:none; justify-content:flex-end;">
                                <input type="checkbox" id="neo-autoskip-cb" style="margin-right:5px;"> Auto-Skip [Alt+S]
                            </label>

                            <div id="neo-history-container" style="display:none; margin-top:5px;">
                                <div style="font-size:11px; color:#888; text-transform:uppercase; margin-bottom:5px;">History (Click to Rewind)</div>
                                <ol id="neo-history-list" style="margin:0; padding-left:20px; font-size:12px; color:#aaa; max-height:100px; overflow-y:auto;"></ol>
                            </div>
                        </div>
                    </div>
                    `;
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
                    document.getElementById('neo-edit-btn').addEventListener('click', function() {
                        if (!this.disabled) {
                            document.getElementById('neo-edit-input').value = document.getElementById('neo-next-action').innerText;
                            editOverlay.style.display = 'flex';
                            document.getElementById('neo-edit-input').focus();
                        }
                    });

                    document.getElementById('neo-edit-cancel-btn').addEventListener('click', function() {
                        editOverlay.style.display = 'none';
                    });

                    document.getElementById('neo-edit-submit-btn').addEventListener('click', function() {
                        var newInstr = document.getElementById('neo-edit-input').value;
                        if (newInstr && newInstr.trim() !== '' && newInstr !== document.getElementById('neo-next-action').innerText) {
                            window.neoHudAction = JSON.stringify({ action: "EDIT", instruction: newInstr.trim() });
                            editOverlay.style.display = 'none';
                        } else {
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
                        if (document.activeElement === document.getElementById('neo-add-input')) {
                            if (e.key === 'Enter') {
                                document.getElementById('neo-add-submit-btn').click();
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

                    skipBtn.disabled = false;
                    skipBtn.style.opacity = '1';

                    editBtn.disabled = false;
                    editBtn.style.opacity = '1';

                    addBtn.disabled = false;
                    addBtn.style.opacity = '1';

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
                            return '<li style="margin-bottom:3px; padding:2px; background:#444; cursor:pointer;" onclick="if(confirm(\\'Rewind execution back to this step?\\')) window.neoHudAction = JSON.stringify({ action: \\'REWIND\\', index: ' + index + ' });" title="Click to rewind">[' + index + '] ' + a + '</li>';
                        }).join('');
                    }
                }

            })(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
            """;

    public static void injectOrUpdateHud(List<String> planned, List<String> performed, boolean autoSkip,
            boolean hudPromptChanged, boolean isFinished) {
        try {
            com.codeborne.selenide.Selenide.executeJavaScript(HUD_SCRIPT, planned, performed, autoSkip,
                    hudPromptChanged, isFinished);
        } catch (Exception e) {
            LOG.warn("Failed to inject AI Generation HUD: {}", e.getMessage());
        }
    }

    public static String checkHudAction() {
        try {
            Object status = com.codeborne.selenide.Selenide.executeJavaScript("return window.neoHudAction;");
            if (status != null) {
                return String.valueOf(status);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean checkAutoSkipStatus() {
        try {
            Object status = com.codeborne.selenide.Selenide.executeJavaScript("return window.neoHudAutoSkip;");
            if (status == null)
                return null;
            return (Boolean) status;
        } catch (Exception e) {
            return null;
        }
    }

    public static void resetHudAction() {
        try {
            com.codeborne.selenide.Selenide.executeJavaScript("window.neoHudAction = null;");
        } catch (Exception e) {
            // ignore
        }
    }
}
