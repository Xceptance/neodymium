html_content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Neodymium Playback HUD</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <style>
        :root {
            --bg-page: #0b0c10;
            --bg-surface: #14161f;
            --bg-surface-hover: #1b1e2a;
            --bg-active-step: rgba(59, 130, 246, 0.08);
            --border-color: rgba(255, 255, 255, 0.08);
            
            --text-main: #f3f4f6;
            --text-secondary: #9ca3af;
            --text-muted: #4b5563;
            
            --accent-primary: #3b82f6;
            --accent-success: #10b981;
            --accent-warning: #f59e0b;
            --accent-danger: #ef4444;
            --accent-purple: #a855f7;
            --accent-healed: #14b8a6;
            
            --font-sans: 'Outfit', sans-serif;
            --font-mono: 'JetBrains Mono', monospace;
            --border-radius: 12px;
        }

        :root.force-light {
            --bg-page: #f3f4f6;
            --bg-surface: #ffffff;
            --bg-surface-hover: #f9fafb;
            --bg-active-step: rgba(37, 99, 235, 0.05);
            --border-color: rgba(0, 0, 0, 0.08);
            
            --text-main: #1f2937;
            --text-secondary: #4b5563;
            --text-muted: #9ca3af;
        }

        * { box-sizing: border-box; margin: 0; padding: 0; }

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

        .header-meta { display: flex; align-items: center; gap: 12px; }
        .header-browser-icon { font-size: 20px; color: var(--accent-primary); }
        .header-meta h1 { font-size: 15px; font-weight: 600; }
        .header-meta span { font-size: 11px; color: var(--text-secondary); margin-left: 8px; }

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
        .column:last-child { border-right: none; }

        .scrollable-content {
            flex-grow: 1;
            overflow-y: auto;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 20px;
        }

        .block-group { display: flex; flex-direction: column; gap: 12px; }
        .block-header {
            font-size: 11px; font-weight: 700; color: var(--text-secondary);
            text-transform: uppercase; letter-spacing: 1px;
            display: flex; align-items: center; gap: 8px;
            margin-bottom: 4px; border-left: 3px solid var(--accent-primary); padding-left: 8px;
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
            transition: border-color 0.2s, background-color 0.2s;
            position: relative;
        }

        .step-card:hover {
            background-color: var(--bg-surface-hover);
            border-color: rgba(255, 255, 255, 0.15);
        }

        .step-card.active {
            background-color: var(--bg-active-step);
            border-color: var(--accent-primary);
            box-shadow: 0 0 12px rgba(59, 130, 246, 0.1);
        }

        .step-card.optional-step { border-style: dashed; border-color: var(--text-muted); }
        .step-card.optional-step.active { border-style: solid; }

        .step-card.include-lvl-1 { border-left: 4px solid #a855f7; }
        .step-card.include-lvl-2 { border-left: 4px solid #8b5cf6; margin-left: 12px; }
        .step-card.include-lvl-3 { border-left: 4px solid #6366f1; margin-left: 24px; }
        .step-card.include-lvl-4 { border-left: 4px solid #3b82f6; margin-left: 36px; }

        .step-row {
            display: flex;
            align-items: flex-start;
            gap: 12px;
            width: 100%;
        }

        .step-status-icon {
            width: 22px; height: 22px;
            display: flex; align-items: center; justify-content: center;
            flex-shrink: 0; font-size: 14px;
        }

        .step-text-container {
            flex-grow: 1;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .step-text { font-size: 14px; font-weight: 500; line-height: 1.4; }

        .include-breadcrumb {
            font-family: var(--font-mono); font-size: 10.5px; font-weight: 600;
            color: var(--text-secondary); background: rgba(255,255,255,0.05);
            padding: 2px 6px; border-radius: 4px; display: inline-flex; align-items: center; gap: 4px;
        }
        .include-breadcrumb i { color: var(--accent-purple); }

        .tag-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-bottom: 4px; }
        
        .type-tag {
            font-size: 10px; font-weight: 700; padding: 2px 6px; border-radius: 4px; display: inline-flex; align-items: center; gap: 4px;
        }
        .type-playbook { background: rgba(59, 130, 246, 0.1); color: var(--accent-primary); border: 1px solid rgba(59, 130, 246, 0.2); }
        .type-llm { background: rgba(168, 85, 247, 0.1); color: var(--accent-purple); border: 1px solid rgba(168, 85, 247, 0.2); }
        .type-healed { background: rgba(20, 184, 166, 0.1); color: var(--accent-healed); border: 1px solid rgba(20, 184, 166, 0.2); }
        .type-branch { background: rgba(245, 158, 11, 0.1); color: var(--accent-warning); border: 1px solid rgba(245, 158, 11, 0.2); }

        .step-edit-btn {
            opacity: 0;
            background: rgba(255,255,255,0.1);
            border: none; color: var(--text-main);
            width: 24px; height: 24px; border-radius: 4px;
            cursor: pointer; transition: opacity 0.2s, background 0.2s;
            display: flex; align-items: center; justify-content: center;
        }
        .step-card:hover .step-edit-btn { opacity: 1; }
        .step-edit-btn:hover { background: var(--accent-primary); }
        @media (hover: none) { .step-edit-btn { opacity: 1; background: rgba(255,255,255,0.05); } }

        .bp-container {
            width: 24px; height: 24px;
            display: flex; align-items: center; justify-content: center;
        }
        .bp-icon {
            color: var(--accent-danger);
            font-size: 16px;
            cursor: pointer;
            opacity: 0;
            transition: all 0.2s;
        }
        .step-card:hover .bp-icon:not(.bp-active) { opacity: 0.3; }
        .bp-icon:hover { opacity: 0.8 !important; }
        .bp-icon.bp-active { opacity: 1; text-shadow: 0 0 8px rgba(239, 68, 68, 0.6); }

        .step-accordion-details {
            display: none;
            flex-direction: column;
            gap: 12px;
            border-top: 1px solid var(--border-color);
            padding-top: 12px;
            margin-top: 4px;
        }
        .step-accordion-details.open { display: flex; }

        .acc-header {
            display: flex; justify-content: space-between; align-items: flex-start;
        }
        .acc-reasoning {
            font-size: 13.5px; color: var(--text-main); line-height: 1.5;
            display: flex; gap: 8px; align-items: flex-start;
        }
        .acc-reasoning i { color: var(--accent-purple); font-size: 16px; margin-top: 2px; }
        
        .acc-duration {
            font-size: 11px; font-weight: 600; color: var(--text-secondary);
            display: flex; flex-direction: column; gap: 4px; align-items: flex-end;
            background: rgba(255,255,255,0.03); padding: 6px 10px; border-radius: 6px; border: 1px solid var(--border-color);
        }
        .acc-duration div { display: flex; align-items: center; gap: 6px; }

        .acc-screenshot {
            width: 100%; border-radius: 8px; border: 1px solid var(--border-color);
            height: 160px; object-fit: cover; object-position: top; opacity: 0.8;
        }
        .webdriver-code {
            font-family: var(--font-mono); font-size: 11.5px;
            background: #0b0c10; border: 1px solid var(--border-color); padding: 10px; border-radius: 6px;
            color: var(--accent-primary); overflow-x: auto;
        }

        .bottom-area {
            background-color: var(--bg-surface);
            border-top: 1px solid var(--border-color);
            display: flex; flex-direction: column;
            z-index: 10;
        }

        .current-reasoning-panel {
            padding: 12px 24px;
            background: rgba(168, 85, 247, 0.05);
            border-bottom: 1px solid var(--border-color);
            display: flex; align-items: center; gap: 12px;
            font-size: 14px; font-weight: 500; color: var(--text-main);
        }
        .current-reasoning-panel i { color: var(--accent-purple); font-size: 18px; }

        .toolbar-footer {
            padding: 16px 24px;
            display: flex; justify-content: space-between; align-items: center;
        }

        .btn-group { display: flex; gap: 8px; align-items: center; }

        .btn {
            font-family: var(--font-sans); font-size: 13px; font-weight: 600;
            padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-color);
            background: rgba(255, 255, 255, 0.03); color: var(--text-secondary);
            cursor: pointer; transition: all 0.2s; display: inline-flex; align-items: center; gap: 6px;
        }
        .btn:hover:not(:disabled) { background-color: rgba(255, 255, 255, 0.06); color: var(--text-main); border-color: var(--text-secondary); }
        .btn-primary { color: var(--accent-primary); border-color: rgba(59, 130, 246, 0.3); background: rgba(59, 130, 246, 0.05); }
        .btn-primary:hover:not(:disabled) { background-color: var(--accent-primary) !important; color: white !important; }
        .btn-danger { color: var(--accent-danger); border-color: rgba(239, 68, 68, 0.3); background: rgba(239, 68, 68, 0.05); }
        .btn-danger:hover:not(:disabled) { background-color: var(--accent-danger) !important; color: white !important; border-color: var(--accent-danger) !important; }

        .cancel-warning {
            display: none; align-items: center; gap: 8px; font-size: 13px; color: var(--accent-danger); font-weight: 600;
            background: rgba(239, 68, 68, 0.1); padding: 6px 12px; border-radius: 6px; border: 1px solid rgba(239, 68, 68, 0.2);
        }

        .panel-header { padding: 16px 24px; border-bottom: 1px solid var(--border-color); background: var(--bg-surface); }
        .panel-header h2 { font-size: 14px; font-weight: 600; }

        @media (max-width: 900px) {
            .workspace { grid-template-columns: 1fr; }
            .column:last-child { display: none; }
        }
    </style>
</head>
<body>

    <header>
        <div class="header-meta">
            <span class="header-browser-icon" title="Chrome Browser"><i class="fa-brands fa-chrome"></i></span>
            <h1 id="testNameDisplay">testCheckoutFlow</h1>
            <span id="testIdDisplay">(ID: Checkout-092)</span>
        </div>
        <div class="header-controls">
            <button class="btn" onclick="toggleTheme()" title="Toggle Theme"><i class="fa-solid fa-circle-half-stroke"></i></button>
        </div>
    </header>

    <div class="workspace">
        <div class="column">
            <div class="scrollable-content" id="timelineList">
                
                <div class="block-group" id="beforeGroup">
                    <div class="block-header">Before Execution</div>
                    
                    <div class="step-card" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-solid fa-circle-check" style="color: var(--accent-success);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-playbook"><i class="fa-solid fa-compact-disc"></i> Playbook</div>
                                </div>
                                <div class="step-text">Open base URL and wait for page layout to load completely.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 0)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                        <div class="step-accordion-details">
                            <div class="acc-header">
                                <div class="acc-reasoning"><i class="fa-solid fa-brain"></i> Static base URL initialization.</div>
                                <div class="acc-duration">
                                    <div><i class="fa-solid fa-bolt" style="color: var(--accent-warning);"></i> Exec: 2.4s</div>
                                </div>
                            </div>
                            <img src="https://picsum.photos/800/300?random=1" class="acc-screenshot" alt="Screenshot">
                        </div>
                    </div>

                    <div class="step-card include-lvl-1" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-solid fa-circle-check" style="color: var(--accent-success);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-llm"><i class="fa-solid fa-robot"></i> LLM Agent</div>
                                    <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> login.yaml</div>
                                </div>
                                <div class="step-text">Navigate to login page.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 1)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                        <div class="step-accordion-details">
                            <div class="acc-header">
                                <div class="acc-reasoning"><i class="fa-solid fa-brain"></i> Identified login button in the header bar. Proceeding to click.</div>
                                <div class="acc-duration">
                                    <div><i class="fa-solid fa-brain" style="color: var(--accent-purple);"></i> AI: 1.1s</div>
                                    <div><i class="fa-solid fa-bolt" style="color: var(--accent-warning);"></i> Exec: 0.6s</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="block-group" id="stepsGroup">
                    <div class="block-header">Core Test Steps</div>

                    <div class="step-card include-lvl-1" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-solid fa-circle-check" style="color: var(--accent-success);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-playbook"><i class="fa-solid fa-compact-disc"></i> Playbook</div>
                                    <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> checkout.yaml</div>
                                </div>
                                <div class="step-text">Click on the checkout button.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 2)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                    </div>

                    <div class="step-card include-lvl-2" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-solid fa-circle-check" style="color: var(--accent-success);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-llm"><i class="fa-solid fa-robot"></i> LLM Agent</div>
                                    <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> address.yaml</div>
                                </div>
                                <div class="step-text">Locate the address form section.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 3)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                    </div>

                    <!-- ACTIVE Running Step -->
                    <div class="step-card active include-lvl-3" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-solid fa-circle-play" style="color: var(--accent-primary);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-llm"><i class="fa-solid fa-robot"></i> LLM Agent</div>
                                    <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> form-inputs.yaml</div>
                                </div>
                                <div class="step-text">Enter '123 Test Street' into the Street input field.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 4)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                        <div class="step-accordion-details open">
                            <div class="acc-header">
                                <div class="acc-reasoning"><i class="fa-solid fa-brain"></i> Located input mapped to "street". Sending keys.</div>
                                <!-- Duration hidden for current step -->
                            </div>
                            <img src="https://picsum.photos/800/300?random=2" class="acc-screenshot" alt="Screenshot">
                            <div class="webdriver-code">driver.findElement(By.id("address-street")).sendKeys("123 Test Street");</div>
                        </div>
                    </div>

                    <div class="step-card include-lvl-4 optional-step" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-regular fa-circle" style="color: var(--text-muted);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-branch"><i class="fa-solid fa-code-branch"></i> IF Condition</div>
                                    <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> zip-validation.yaml</div>
                                </div>
                                <div class="step-text">If country is US, validate zip code is 5 digits.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 5)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                    </div>

                    <div class="step-card include-lvl-1" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-regular fa-circle" style="color: var(--text-muted);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-playbook"><i class="fa-solid fa-compact-disc"></i> Playbook</div>
                                    <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> checkout.yaml</div>
                                </div>
                                <div class="step-text">Confirm order summary.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 6)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon bp-active" onclick="toggleBp(event, this)"></i></div>
                        </div>
                    </div>

                    <div class="step-card optional-step" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-regular fa-circle" style="color: var(--text-muted);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-branch"><i class="fa-solid fa-code-branch"></i> Optional</div>
                                </div>
                                <div class="step-text">Apply coupon code 'DISCOUNT20' if promo field is visible.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 7)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                    </div>

                    <div class="step-card" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-regular fa-circle" style="color: var(--text-muted);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-playbook"><i class="fa-solid fa-compact-disc"></i> Playbook</div>
                                </div>
                                <div class="step-text">Complete purchase and wait for success page. (Core un-included step)</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 8)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                    </div>
                </div>

                <div class="block-group" id="afterGroup">
                    <div class="block-header">After Execution</div>
                    
                    <div class="step-card" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-regular fa-circle" style="color: var(--text-muted);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-healed"><i class="fa-solid fa-wrench"></i> Healed Playbook</div>
                                </div>
                                <div class="step-text">Verify order is logged in account.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 9)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                    </div>

                    <div class="step-card include-lvl-1" onclick="toggleAccordion(this)">
                        <div class="step-row">
                            <div class="step-status-icon"><i class="fa-regular fa-circle" style="color: var(--text-muted);"></i></div>
                            <div class="step-text-container">
                                <div class="tag-row">
                                    <div class="type-tag type-playbook"><i class="fa-solid fa-compact-disc"></i> Playbook</div>
                                    <div class="include-breadcrumb"><i class="fa-solid fa-folder-open"></i> logout.yaml</div>
                                </div>
                                <div class="step-text">Logout standard_user.</div>
                            </div>
                            <button class="step-edit-btn" onclick="openEdit(event, 10)"><i class="fa-solid fa-pencil"></i></button>
                            <div class="bp-container"><i class="fa-solid fa-octagon bp-icon" onclick="toggleBp(event, this)"></i></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="column">
            <div class="panel-header"><h2>Visual State</h2></div>
            <div class="scrollable-content">
                <div style="background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 12px; padding: 16px; display: flex; flex-direction: column; gap: 12px;">
                    <h3 style="font-size: 13px; font-weight: 600; color: var(--text-secondary);">Current DOM Snapshot</h3>
                    <img src="https://picsum.photos/600/800?random=3" style="width: 100%; border-radius: 8px; border: 1px solid var(--border-color);" alt="Full page view">
                </div>
            </div>
        </div>
    </div>

    <!-- Bottom Action Area -->
    <div class="bottom-area">
        <!-- Current Step Reasoning Bar -->
        <div class="current-reasoning-panel">
            <i class="fa-solid fa-brain fa-fade"></i>
            <span>Agent is thinking: Analyzing form structure to map '123 Test Street' to the correct input field...</span>
        </div>
        
        <!-- Toolbar -->
        <div class="toolbar-footer">
            <div class="btn-group">
                <button class="btn" onclick="triggerAction('back')" title="Step Back"><i class="fa-solid fa-backward-step"></i> Back</button>
                <button class="btn btn-primary" onclick="triggerAction('run')" title="Run Step"><i class="fa-solid fa-play"></i> Run</button>
                <button class="btn" onclick="triggerAction('skip')" title="Skip Step"><i class="fa-solid fa-forward-step"></i> Skip</button>
                <button class="btn" id="btnAuto" onclick="triggerAction('auto')" title="Auto-run"><i class="fa-solid fa-forward-fast"></i> Auto</button>
            </div>
            
            <div class="btn-group">
                <div class="cancel-warning" id="cancelWarning">
                    Are you sure? 
                    <button class="btn" style="padding: 4px 8px; font-size: 11px;" onclick="confirmCancel()">Yes, Kill</button>
                    <button class="btn" style="padding: 4px 8px; font-size: 11px;" onclick="hideCancelWarning()">No</button>
                </div>
                <button class="btn btn-danger" id="btnCancel" onclick="showCancelWarning()"><i class="fa-solid fa-skull"></i> Cancel test</button>
            </div>
        </div>
    </div>

    <script>
        function toggleAccordion(cardElement) {
            const acc = cardElement.querySelector('.step-accordion-details');
            if (acc) acc.classList.toggle('open');
        }

        function toggleBp(event, iconElement) {
            event.stopPropagation();
            iconElement.classList.toggle('bp-active');
        }

        function openEdit(event, index) {
            event.stopPropagation();
            alert("Edit dialog opened for step " + index);
        }

        function triggerAction(actionType) {
            console.log("Debugger action: " + actionType);
            if (actionType === 'auto') {
                document.getElementById('btnAuto').classList.toggle('btn-primary');
            }
        }

        function showCancelWarning() {
            document.getElementById('btnCancel').style.display = 'none';
            document.getElementById('cancelWarning').style.display = 'flex';
        }

        function hideCancelWarning() {
            document.getElementById('cancelWarning').style.display = 'none';
            document.getElementById('btnCancel').style.display = 'inline-flex';
        }

        function confirmCancel() {
            console.log("Test killed.");
            hideCancelWarning();
        }

        function toggleTheme() { document.documentElement.classList.toggle('force-light'); }
    </script>
</body>
</html>
"""
with open("/home/weigel/Documents/projects/automation/neodymium/src/main/resources/com/xceptance/neodymium/aura/interactive-hud.html", "w") as f:
    f.write(html_content)
print("done")
