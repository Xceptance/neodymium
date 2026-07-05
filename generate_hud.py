html_content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Neodymium Playback HUD</title>
    <!-- Outfit (clean headings/UI) & JetBrains Mono (tech/code details) -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <style>
        :root {
            /* Harmonious Spacious Theme (Dark) */
            --bg-page: #0b0c10;
            --bg-surface: #14161f;
            --bg-surface-hover: #1b1e2a;
            --bg-active-step: rgba(59, 130, 246, 0.08);
            --border-color: rgba(255, 255, 255, 0.08);
            
            --text-main: #f3f4f6;
            --text-secondary: #9ca3af;
            --text-muted: #4b5563;
            
            --accent-primary: #3b82f6;
            --accent-primary-hover: #2563eb;
            --accent-success: #10b981;
            --accent-warning: #f59e0b;
            --accent-danger: #ef4444;
            --accent-purple: #a855f7;
            
            --font-sans: 'Outfit', sans-serif;
            --font-mono: 'JetBrains Mono', monospace;
            --border-radius: 12px;
            --spacing-unit: 16px;
        }

        /* Light Theme Options */
        :root.force-light {
            --bg-page: #f3f4f6;
            --bg-surface: #ffffff;
            --bg-surface-hover: #f9fafb;
            --bg-active-step: rgba(37, 99, 235, 0.05);
            --border-color: rgba(0, 0, 0, 0.08);
            
            --text-main: #1f2937;
            --text-secondary: #4b5563;
            --text-muted: #9ca3af;
            
            --accent-primary: #2563eb;
            --accent-primary-hover: #1d4ed8;
            --accent-success: #059669;
            --accent-warning: #d97706;
            --accent-danger: #dc2626;
            --accent-purple: #7c3aed;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            background-color: var(--bg-page);
            color: var(--text-main);
            font-family: var(--font-sans);
            display: flex;
            flex-direction: column;
            height: 100vh;
            overflow: hidden;
            transition: background-color 0.25s, color 0.25s;
        }

        header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 16px 24px;
            background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-color);
            z-index: 10;
        }

        .header-meta {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .header-browser-icon {
            font-size: 20px;
            color: var(--accent-primary);
            display: flex;
            align-items: center;
        }

        .header-meta h1 {
            font-size: 15px;
            font-weight: 600;
            letter-spacing: -0.2px;
        }

        .header-meta span {
            font-size: 11px;
            color: var(--text-secondary);
            margin-left: 8px;
        }

        .header-controls {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .workspace {
            display: grid;
            grid-template-columns: 1.3fr 1fr;
            flex-grow: 1;
            overflow: hidden;
            height: calc(100vh - 61px);
        }

        .column {
            display: flex;
            flex-direction: column;
            overflow: hidden;
            border-right: 1px solid var(--border-color);
        }

        .column:last-child {
            border-right: none;
        }

        .panel-header {
            padding: 16px 24px;
            border-bottom: 1px solid var(--border-color);
            background-color: var(--bg-surface);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .panel-header h2 {
            font-size: 14px;
            font-weight: 600;
        }
        
        .panel-header span {
            font-size: 12px;
            color: var(--text-secondary);
        }

        .scrollable-content {
            flex-grow: 1;
            overflow-y: auto;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .block-group {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .block-header {
            font-size: 11px;
            font-weight: 700;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 1px;
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 4px;
            border-left: 3px solid var(--accent-primary);
            padding-left: 8px;
        }

        .step-card {
            background-color: var(--bg-surface);
            border: 1px solid var(--border-color);
            border-radius: var(--border-radius);
            padding: 14px 18px;
            display: flex;
            flex-direction: column;
            gap: 8px;
            cursor: pointer;
            transition: border-color 0.2s, background-color 0.2s, box-shadow 0.2s;
            position: relative;
            overflow: hidden;
        }

        .step-card.optional-step {
            border-style: dashed;
            border-color: var(--text-muted);
        }

        .step-card.optional-step:hover {
            border-color: var(--accent-warning);
        }

        .step-card.optional-step.active, .step-card.optional-step.selected-step {
            border-style: solid;
        }

        /* Visual indicator on left edge for includes */
        .step-card.include-lvl-1 { border-left: 4px solid #a855f7; }
        .step-card.include-lvl-2 { border-left: 4px solid #8b5cf6; margin-left: 12px; }
        .step-card.include-lvl-3 { border-left: 4px solid #6366f1; margin-left: 24px; }
        .step-card.include-lvl-4 { border-left: 4px solid #3b82f6; margin-left: 36px; }

        .step-card:hover {
            background-color: var(--bg-surface-hover);
            border-color: rgba(255, 255, 255, 0.15);
        }

        .step-card.active {
            background-color: var(--bg-active-step);
            border-color: var(--accent-primary);
            box-shadow: 0 0 12px rgba(59, 130, 246, 0.1);
        }

        .step-card.selected-step {
            border-color: var(--accent-purple) !important;
        }

        .step-row {
            display: flex;
            align-items: center;
            gap: 12px;
            width: 100%;
        }

        .bp-container {
            width: 22px;
            height: 22px;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }

        .bp-toggle {
            width: 14px;
            height: 14px;
            border-radius: 50%;
            border: 2px solid var(--text-muted);
            cursor: pointer;
            transition: all 0.2s;
        }

        .bp-toggle:hover {
            border-color: var(--accent-danger);
            background-color: rgba(239, 68, 68, 0.1);
        }

        .bp-toggle.bp-active {
            border-color: var(--accent-danger);
            background-color: var(--accent-danger);
            box-shadow: 0 0 8px var(--accent-danger);
        }

        .past-icon { font-size: 13px; }
        .past-icon.playbook { color: var(--accent-primary); }
        .past-icon.llm { color: var(--accent-purple); }

        .step-text {
            font-size: 14px;
            font-weight: 500;
            line-height: 1.5;
            flex-grow: 1;
        }

        .include-breadcrumb {
            font-family: var(--font-mono);
            font-size: 10px;
            font-weight: 600;
            color: var(--text-secondary);
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            padding: 2px 6px;
            border-radius: 4px;
            align-self: flex-start;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .include-breadcrumb .separator { margin: 0 4px; color: var(--text-muted); }
        .include-breadcrumb i { color: var(--accent-purple); }

        .optional-tag {
            font-family: var(--font-sans);
            font-size: 10px;
            font-weight: 700;
            color: var(--accent-warning);
            background: rgba(245, 158, 11, 0.1);
            border: 1px solid rgba(245, 158, 11, 0.2);
            padding: 2px 6px;
            border-radius: 4px;
            align-self: flex-start;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }

        .step-tags-row {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .step-accordion-details {
            display: none;
            flex-direction: column;
            gap: 8px;
            border-top: 1px solid var(--border-color);
            padding-top: 10px;
            margin-top: 4px;
            font-size: 13px;
        }

        .toolbar-footer {
            padding: 16px 24px;
            background-color: var(--bg-surface);
            border-top: 1px solid var(--border-color);
            display: flex;
            justify-content: space-between;
            align-items: center;
            z-index: 10;
        }

        .btn-group { display: flex; gap: 8px; }

        .btn {
            font-family: var(--font-sans);
            font-size: 13px;
            font-weight: 600;
            padding: 8px 16px;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            background: rgba(255, 255, 255, 0.03);
            color: var(--text-secondary);
            cursor: pointer;
            transition: all 0.2s;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }

        .btn:hover:not(:disabled) {
            background-color: rgba(255, 255, 255, 0.06);
            color: var(--text-main);
            border-color: var(--text-secondary);
        }

        .btn:disabled { opacity: 0.35; cursor: not-allowed; }

        .btn-success { color: var(--accent-success); border-color: rgba(16, 185, 129, 0.3); background: rgba(16, 185, 129, 0.05); }
        .btn-success:hover:not(:disabled) { background-color: var(--accent-success) !important; border-color: var(--accent-success) !important; color: white !important; }

        .btn-primary { color: var(--accent-primary); border-color: rgba(59, 130, 246, 0.3); background: rgba(59, 130, 246, 0.05); }
        .btn-primary:hover:not(:disabled) { background-color: var(--accent-primary) !important; border-color: var(--accent-primary) !important; color: white !important; }

        .add-step-card-btn {
            border: 1px dashed var(--border-color);
            border-radius: var(--border-radius);
            padding: 10px;
            text-align: center;
            font-size: 12.5px;
            font-weight: 600;
            color: var(--text-secondary);
            cursor: pointer;
            transition: all 0.2s;
        }

        .add-step-card-btn:hover { border-color: var(--accent-primary); color: var(--accent-primary); background: rgba(59, 130, 246, 0.02); }

        .detail-sidebar-card {
            background-color: var(--bg-surface);
            border: 1px solid var(--border-color);
            border-radius: var(--border-radius);
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .sidebar-section { display: flex; flex-direction: column; gap: 6px; }
        .sidebar-section-title { font-size: 11px; font-weight: 700; color: var(--accent-purple); text-transform: uppercase; letter-spacing: 0.8px; }
        .sidebar-section-body { font-size: 13px; line-height: 1.5; color: var(--text-secondary); }

        .webdriver-code {
            font-family: var(--font-mono);
            font-size: 11.5px;
            background: #0b0c10;
            border: 1px solid var(--border-color);
            padding: 10px;
            border-radius: 6px;
            color: var(--accent-primary);
            overflow-x: auto;
        }

        .overlay-dialog {
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(11, 12, 16, 0.8);
            z-index: 100;
            display: none;
            flex-direction: column;
            padding: 24px;
            box-sizing: border-box;
            backdrop-filter: blur(8px);
        }

        .dialog-content {
            background: var(--bg-surface);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            margin: auto;
            width: 100%;
            max-width: 600px;
            padding: 24px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.5);
            display: flex;
            flex-direction: column;
            gap: 16px;
        }

        .dialog-header { font-weight: 700; font-size: 16px; color: var(--text-main); display: flex; align-items: center; gap: 8px; }
        .dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }

        .hud-input-textarea {
            width: 100%; height: 80px;
            background: #0b0c10;
            border: 1px solid var(--border-color);
            color: var(--text-main);
            font-family: var(--font-sans);
            padding: 10px 14px;
            border-radius: 8px;
            resize: none;
            font-size: 13.5px;
        }
        .hud-input-textarea:focus { outline: none; border-color: var(--accent-primary); }

        @media (max-width: 800px) {
            .workspace { grid-template-columns: 1fr; }
            .column:last-child { display: none; }
            .step-accordion-details.mobile-visible { display: flex; }
        }
    </style>
</head>
<body>

    <header>
        <div class="header-meta">
            <span class="header-browser-icon" aria-hidden="true" title="Chrome Browser"><i class="fa-brands fa-chrome"></i></span>
            <h1 id="testNameDisplay">testCheckoutFlow</h1>
            <span id="testIdDisplay">(ID: Checkout-092)</span>
        </div>
        <div class="header-controls">
            <button class="theme-toggle-btn btn" onclick="toggleTheme()" aria-label="Toggle theme" title="Toggle Theme"><i class="fa-solid fa-circle-half-stroke"></i></button>
        </div>
    </header>

    <div class="workspace">
        <div class="column">
            <div class="panel-header">
                <h2>Execution Path</h2>
                <span id="stepProgress">Step 5 of 12</span>
            </div>

            <div class="scrollable-content" id="timelineList">
                
                <!-- BEFORE Execution Block -->
                <div class="block-group" id="beforeGroup">
                    <div class="block-header">Before Execution</div>
                    
                    <div class="step-card completed" onclick="selectStep(0, this)" tabindex="0">
                        <div class="step-row">
                            <div class="bp-container" title="Recorded Playbook"><i class="fa-solid fa-compact-disc past-icon playbook" aria-hidden="true"></i></div>
                            <span class="step-number">01</span>
                            <div class="step-text">Open base URL and wait for page layout to load completely.</div>
                            <i class="fa-solid fa-circle-check" style="color: var(--accent-success);" aria-hidden="true"></i>
                        </div>
                    </div>

                    <div class="step-card completed include-lvl-1" onclick="selectStep(1, this)" tabindex="0">
                        <div class="step-tags-row">
                            <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> setup/login.yaml</div>
                        </div>
                        <div class="step-row">
                            <div class="bp-container" title="LLM Action"><i class="fa-solid fa-robot past-icon llm" aria-hidden="true"></i></div>
                            <span class="step-number">02</span>
                            <div class="step-text">Navigate to login page.</div>
                            <i class="fa-solid fa-circle-check" style="color: var(--accent-success);" aria-hidden="true"></i>
                        </div>
                    </div>
                </div>

                <!-- CORE Execution Block -->
                <div class="block-group" id="stepsGroup">
                    <div class="block-header">Core Test Steps</div>

                    <div class="step-card completed include-lvl-1" onclick="selectStep(2, this)" tabindex="0">
                        <div class="step-tags-row">
                            <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> checkout.yaml</div>
                        </div>
                        <div class="step-row">
                            <div class="bp-container" title="Recorded Playbook"><i class="fa-solid fa-compact-disc past-icon playbook" aria-hidden="true"></i></div>
                            <span class="step-number">03</span>
                            <div class="step-text">Click on the checkout button.</div>
                            <i class="fa-solid fa-circle-check" style="color: var(--accent-success);" aria-hidden="true"></i>
                        </div>
                    </div>

                    <div class="step-card completed include-lvl-2" onclick="selectStep(3, this)" tabindex="0">
                        <div class="step-tags-row">
                            <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> checkout.yaml <span class="separator">&gt;</span> address.yaml</div>
                        </div>
                        <div class="step-row">
                            <div class="bp-container" title="LLM Action"><i class="fa-solid fa-robot past-icon llm" aria-hidden="true"></i></div>
                            <span class="step-number">04</span>
                            <div class="step-text">Locate the address form section.</div>
                            <i class="fa-solid fa-circle-check" style="color: var(--accent-success);" aria-hidden="true"></i>
                        </div>
                    </div>

                    <div class="step-card active selected-step include-lvl-3" onclick="selectStep(4, this)" tabindex="0">
                        <div class="step-tags-row">
                            <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> checkout.yaml <span class="separator">&gt;</span> address.yaml <span class="separator">&gt;</span> form-inputs.yaml</div>
                        </div>
                        <div class="step-row">
                            <div class="bp-container" title="Breakpoint (Inactive)">
                                <div class="bp-toggle" onclick="toggleBreakpoint(4, event)"></div>
                            </div>
                            <span class="step-number">05</span>
                            <div class="step-text">Enter '123 Test Street' into the Street input field.</div>
                            <i class="fa-solid fa-circle-play" style="color: var(--accent-primary);" aria-hidden="true"></i>
                        </div>
                        <!-- Accordion for Mobile Details -->
                        <div class="step-accordion-details mobile-visible" id="accordion-4">
                            <div><strong>Duration:</strong> 1.2s</div>
                            <div><strong>Reasoning:</strong> Located input mapped to "street". Sending keys.</div>
                        </div>
                    </div>

                    <div class="step-card include-lvl-4 optional-step" onclick="selectStep(5, this)" tabindex="0">
                        <div class="step-tags-row">
                            <div class="optional-tag"><i class="fa-solid fa-code-branch"></i> Optional / Condition</div>
                            <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> checkout.yaml <span class="separator">&gt;</span> address.yaml <span class="separator">&gt;</span> form-inputs.yaml <span class="separator">&gt;</span> zip-validation.yaml</div>
                        </div>
                        <div class="step-row">
                            <div class="bp-container" title="Breakpoint (Inactive)">
                                <div class="bp-toggle" onclick="toggleBreakpoint(5, event)"></div>
                            </div>
                            <span class="step-number">06</span>
                            <div class="step-text">If country is US, validate zip code is 5 digits.</div>
                            <i class="fa-regular fa-circle" style="color: var(--text-muted);" aria-hidden="true"></i>
                        </div>
                    </div>

                    <div class="step-card include-lvl-1" onclick="selectStep(6, this)" tabindex="0">
                        <div class="step-tags-row">
                            <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> checkout.yaml</div>
                        </div>
                        <div class="step-row">
                            <div class="bp-container" title="Breakpoint (Active)">
                                <div class="bp-toggle bp-active" onclick="toggleBreakpoint(6, event)"></div>
                            </div>
                            <span class="step-number">07</span>
                            <div class="step-text">Confirm order summary.</div>
                            <i class="fa-regular fa-circle" style="color: var(--text-muted);" aria-hidden="true"></i>
                        </div>
                    </div>
                    
                    <div class="step-card optional-step" onclick="selectStep(7, this)" tabindex="0">
                        <div class="step-tags-row">
                            <div class="optional-tag"><i class="fa-solid fa-code-branch"></i> Optional</div>
                        </div>
                        <div class="step-row">
                            <div class="bp-container" title="Breakpoint (Inactive)">
                                <div class="bp-toggle" onclick="toggleBreakpoint(7, event)"></div>
                            </div>
                            <span class="step-number">08</span>
                            <div class="step-text">Apply coupon code 'DISCOUNT20' if promo field is visible.</div>
                            <i class="fa-regular fa-circle" style="color: var(--text-muted);" aria-hidden="true"></i>
                        </div>
                    </div>

                    <div class="step-card" onclick="selectStep(8, this)" tabindex="0">
                        <div class="step-row">
                            <div class="bp-container" title="Breakpoint (Inactive)">
                                <div class="bp-toggle" onclick="toggleBreakpoint(8, event)"></div>
                            </div>
                            <span class="step-number">09</span>
                            <div class="step-text">Complete purchase and wait for success page. (Core un-included step)</div>
                            <i class="fa-regular fa-circle" style="color: var(--text-muted);" aria-hidden="true"></i>
                        </div>
                    </div>
                    
                    <div class="add-step-card-btn" onclick="openAddDialog()" role="button" tabindex="0">
                        <i class="fa-solid fa-plus" aria-hidden="true"></i> Add Step here
                    </div>
                </div>

                <!-- AFTER Execution Block -->
                <div class="block-group" id="afterGroup">
                    <div class="block-header">After Execution</div>

                    <div class="step-card" onclick="selectStep(9, this)" tabindex="0">
                        <div class="step-row">
                            <div class="bp-container" title="Breakpoint (Inactive)">
                                <div class="bp-toggle" onclick="toggleBreakpoint(9, event)"></div>
                            </div>
                            <span class="step-number">10</span>
                            <div class="step-text">Verify order is logged in account.</div>
                            <i class="fa-regular fa-circle" style="color: var(--text-muted);" aria-hidden="true"></i>
                        </div>
                    </div>

                    <div class="step-card include-lvl-1" onclick="selectStep(10, this)" tabindex="0">
                        <div class="step-tags-row">
                            <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> teardown/logout.yaml</div>
                        </div>
                        <div class="step-row">
                            <div class="bp-container" title="Breakpoint (Inactive)">
                                <div class="bp-toggle" onclick="toggleBreakpoint(10, event)"></div>
                            </div>
                            <span class="step-number">11</span>
                            <div class="step-text">Logout standard_user.</div>
                            <i class="fa-regular fa-circle" style="color: var(--text-muted);" aria-hidden="true"></i>
                        </div>
                    </div>
                </div>

            </div>
        </div>

        <div class="column">
            <div class="panel-header">
                <h2>Step Diagnostics</h2>
            </div>
            
            <div class="scrollable-content">
                <div class="detail-sidebar-card">
                    <div class="sidebar-section">
                        <span class="sidebar-section-title">Step Description</span>
                        <div class="sidebar-section-body" id="diagDescription" style="font-weight: 500; font-size: 14px;">
                            Enter '123 Test Street' into the Street input field.
                        </div>
                    </div>

                    <div class="sidebar-section">
                        <span class="sidebar-section-title">AI Agent Reasoning</span>
                        <div class="sidebar-section-body" id="diagReasoning">
                            Located input mapped to "street". Sending keys.
                        </div>
                    </div>

                    <div class="sidebar-section">
                        <span class="sidebar-section-title">WebDriver Actions</span>
                        <div class="webdriver-code" id="diagActions">
                            driver.findElement(By.id("address-street")).sendKeys("123 Test Street");
                        </div>
                    </div>

                    <div class="sidebar-section">
                        <span class="sidebar-section-title">Metrics</span>
                        <div class="sidebar-section-body" id="diagMetrics">
                            <strong>Duration:</strong> 1.2 seconds<br>
                            <strong>Escalation Level:</strong> Standard DOM (Level 0)<br>
                            <strong>Tokens:</strong> 310 input / 110 output
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <div class="toolbar-footer">
        <div class="btn-group">
            <button class="btn btn-primary" onclick="triggerAction('run')" title="Run Step [Alt+R]"><i class="fa-solid fa-play" aria-hidden="true"></i> Run</button>
            <button class="btn btn-success" id="btnAuto" onclick="triggerAction('auto')" title="Auto continuous execution [Alt+S]"><i class="fa-solid fa-forward-fast" aria-hidden="true"></i> Auto</button>
            <button class="btn" onclick="triggerAction('skip')" title="Skip current active step [Alt+K]"><i class="fa-solid fa-forward" aria-hidden="true"></i> Skip</button>
        </div>
        <div class="btn-group">
            <button class="btn" id="btnEditStep" onclick="openEditDialog()" title="Edit instruction text [Alt+E]"><i class="fa-solid fa-pencil" aria-hidden="true"></i> Edit Step</button>
            <button class="btn btn-success" onclick="triggerAction('save_exit')" title="Finish and Save changes"><i class="fa-solid fa-floppy-disk" aria-hidden="true"></i> Finish</button>
        </div>
    </div>

    <div class="overlay-dialog" id="addDialog">
        <div class="dialog-content">
            <div class="dialog-header"><i class="fa-solid fa-circle-plus" style="color: var(--accent-success);"></i> Add New Test Instruction</div>
            <textarea class="hud-input-textarea" id="addInputTextarea" placeholder="e.g. Click on the Basket button..."></textarea>
            <div class="dialog-footer">
                <button class="btn" onclick="closeAddDialog()">Cancel</button>
                <button class="btn btn-success" onclick="submitAdd()">Add Step</button>
            </div>
        </div>
    </div>

    <div class="overlay-dialog" id="editDialog">
        <div class="dialog-content">
            <div class="dialog-header"><i class="fa-solid fa-pen-to-square" style="color: var(--accent-purple);"></i> Edit Instruction Text</div>
            <textarea class="hud-input-textarea" id="editInputTextarea" placeholder="Enter updated description..."></textarea>
            <div class="dialog-footer">
                <button class="btn" onclick="closeEditDialog()">Cancel</button>
                <button class="btn btn-success" onclick="submitEdit()">Save Changes</button>
            </div>
        </div>
    </div>

    <script>
        const stepDetails = [
            { text: "Open base URL and wait for page layout to load completely.", reasoning: "Static base URL initialization.", actions: "driver.get(\"https://demo.xceptance.com\");", metrics: "Source: Recorded Playbook" },
            { text: "Navigate to login page.", reasoning: "Found login link in header.", actions: "driver.findElement(By.cssSelector('.login-link')).click();", metrics: "Source: Live LLM Planner" },
            { text: "Click on the checkout button.", reasoning: "Proceeding to checkout via cart page.", actions: "driver.findElement(By.id('btn-checkout')).click();", metrics: "Source: Recorded Playbook" },
            { text: "Locate the address form section.", reasoning: "Waiting for address form to be visible.", actions: "wait.until(ExpectedConditions.visibilityOfElementLocated(By.id('address-form')));", metrics: "Source: Live LLM Planner" },
            { text: "Enter '123 Test Street' into the Street input field.", reasoning: "Located input mapped to 'street'. Sending keys.", actions: "driver.findElement(By.id('address-street')).sendKeys('123 Test Street');", metrics: "Duration: 1.2 seconds<br>Tokens: 310 / 110" },
            { text: "If country is US, validate zip code is 5 digits.", reasoning: "Branching condition evaluated. Country is US, performing regex match on zip input.", actions: "String zip = driver.findElement(By.id('zip')).getAttribute('value');\\nassertTrue(zip.matches('\\\\d{5}'));", metrics: "Condition: Country == 'US'" },
            { text: "Confirm order summary.", reasoning: "Navigating next.", actions: "driver.findElement(By.id('btn-confirm')).click();", metrics: "Paused" },
            { text: "Apply coupon code 'DISCOUNT20' if promo field is visible.", reasoning: "Optional step: checking visibility of promo input before acting.", actions: "if(driver.findElements(By.id('promo-code')).size() > 0) {\\n  driver.findElement(By.id('promo-code')).sendKeys('DISCOUNT20');\\n}", metrics: "Status: Future" },
            { text: "Complete purchase and wait for success page. (Core un-included step)", reasoning: "Final checkout action.", actions: "driver.findElement(By.id('btn-buy-now')).click();", metrics: "Status: Future" },
            { text: "Verify order is logged in account.", reasoning: "Navigate to account.", actions: "driver.get('/account/orders');", metrics: "Status: Future" },
            { text: "Logout standard_user.", reasoning: "Teardown sequence.", actions: "driver.findElement(By.id('logout')).click();", metrics: "Status: Future" }
        ];

        let selectedIndex = 4;
        let activeBreakpoints = [6];

        function toggleBreakpoint(idx, event) {
            if (event) event.stopPropagation();
            const target = event.currentTarget;
            target.classList.toggle('bp-active');
            if (target.classList.contains('bp-active')) {
                if (!activeBreakpoints.includes(idx)) activeBreakpoints.push(idx);
            } else {
                activeBreakpoints = activeBreakpoints.filter(i => i !== idx);
            }
            postEventBack({ type: 'breakpoints', list: activeBreakpoints });
        }

        function selectStep(idx, card) {
            selectedIndex = idx;
            document.querySelectorAll('.step-card').forEach(c => {
                c.classList.remove('selected-step');
                const acc = c.querySelector('.step-accordion-details');
                if (acc) acc.classList.remove('mobile-visible');
            });

            card.classList.add('selected-step');
            
            const accordion = card.querySelector('.step-accordion-details');
            if (accordion) accordion.classList.add('mobile-visible');

            const details = stepDetails[idx];
            document.getElementById('diagDescription').innerText = details.text;
            document.getElementById('diagReasoning').innerText = details.reasoning;
            document.getElementById('diagActions').innerText = details.actions;
            document.getElementById('diagMetrics').innerHTML = details.metrics;

            postEventBack({ type: 'selectStep', index: idx });
        }

        function triggerAction(actionType) {
            console.log("Debugger action: " + actionType);
            postEventBack({ type: 'action', action: actionType });
            if (actionType === 'auto') {
                document.getElementById('btnAuto').classList.toggle('btn-success');
            }
        }

        const isEmbedded = window.self !== window.top;
        function postEventBack(payload) {
            payload.source = 'interactive-hud';
            if (isEmbedded) {
                window.parent.postMessage(payload, '*');
            } else if (window.opener) {
                window.opener.postMessage(payload, '*');
            }
        }

        function toggleTheme() { document.documentElement.classList.toggle('force-light'); }

        function openAddDialog() { document.getElementById('addDialog').style.display = 'flex'; document.getElementById('addInputTextarea').focus(); }
        function closeAddDialog() { document.getElementById('addDialog').style.display = 'none'; }
        function submitAdd() {
            const val = document.getElementById('addInputTextarea').value.trim();
            if (val) { postEventBack({ type: 'addStep', text: val }); closeAddDialog(); }
        }

        function openEditDialog() {
            document.getElementById('editInputTextarea').value = stepDetails[selectedIndex].text;
            document.getElementById('editDialog').style.display = 'flex';
            document.getElementById('editInputTextarea').focus();
        }
        function closeEditDialog() { document.getElementById('editDialog').style.display = 'none'; }
        function submitEdit() {
            const val = document.getElementById('editInputTextarea').value.trim();
            if (val) { postEventBack({ type: 'editStep', index: selectedIndex, text: val }); closeEditDialog(); }
        }

        document.addEventListener('keydown', function(e) {
            if (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA') return;
            const key = e.key.toLowerCase();
            if (e.key === 'Escape') { closeAddDialog(); closeEditDialog(); }
            if (e.altKey && key === 'z') { e.preventDefault(); triggerAction('back'); }
            if (e.altKey && key === 'r') { e.preventDefault(); triggerAction('run'); }
            if (e.altKey && key === 's') { e.preventDefault(); triggerAction('auto'); }
            if (e.altKey && key === 'k') { e.preventDefault(); triggerAction('skip'); }
            if (e.altKey && key === 'n') { e.preventDefault(); openAddDialog(); }
            if (e.altKey && key === 'e') { e.preventDefault(); openEditDialog(); }
        });
    </script>
</body>
</html>
"""
with open("/home/weigel/Documents/projects/automation/neodymium/src/main/resources/com/xceptance/neodymium/aura/interactive-hud.html", "w") as f:
    f.write(html_content)
print("done")
