# [IDEA-20261105-1215] Copy Widgets Configuration Across Custom Dashboards

- **Status:** `Implemented`
- **Proposed:** 2026-11-05 12:15
- **Resolved:** 2026-11-05 12:45
- **Component:** `aura-manager`
- **Category:** `Tooling & DX`
- **Author:** AI-generated: Gemini 3.8 Flash (High) / Xceptance GmbH 2026

## 1. Problem Statement & Motivation
Aura Manager allows users to create standard and hierarchical group custom dashboards (`/dashboard-pages/{pageId}`), configure custom telemetry widgets (metrics, presentations, scopes, identities, and filters), and unite or separate widgets into containers.

However, when a user creates a new custom dashboard or needs to replicate an existing dashboard layout:
1. Every newly created dashboard page begins completely blank (`widgets = Collections.emptyList()`).
2. There is no mechanism to duplicate or transfer configured widgets from an existing custom dashboard (or default dashboard) into another dashboard.
3. Users who want to establish similar dashboards (e.g. for different teams, releases, browsers, or environment scopes) must manually recreate and reconfigure every widget from scratch.

## 2. Proposed Architecture & Design
Introduce an end-to-end widget copying mechanism supporting both existing custom dashboards and new dashboard creation:

1. **Deep Widget Cloning (`DashboardWidgetConfigDto.cloneWithNewId`)**:
   - Generates a new unique UUID for the widget (`widget-xxxxxxxx` or `container-xxxxxxxx` if container).
   - Recursively clones all child widgets with new UUIDs if the widget is a container.
   - Preserves all visual and telemetry parameters: `title`, `metric`, `tagKey`, `presentation`, `trendMode`, `valueFormat`, `pointScope`, `width`, `sortOrder`, `showHealedAi`, `overrideExecutionIdentity`, `executionIdentityDimensions`, `overrideRunFilters`, `runFilters`, `colorPolarity`, `showDots`, and `filled`.

2. **Setting Service Operation (`ReportDashboardSettingService.copyWidgets`)**:
   - Validates that `sourcePageId` and `targetPageId` are distinct and valid.
   - Resolves widgets from `sourcePageId` (supporting both custom pages and `"default"` dashboard widgets).
   - Deep-clones each widget with new unique IDs and updates sequential `sortOrder` indices.
   - Supports two modes:
     - `Replace`: Replaces all existing widgets on the target page.
     - `Append`: Retains existing widgets on the target page and appends the cloned widgets.
   - Persists changes via H2 database and JSON sync storage.

3. **Page Creation Extension (`ReportDashboardSettingService.createCustomPage`)**:
   - Accepts an optional `copyFromPageId` parameter allowing a new dashboard to immediately inherit widgets from an existing dashboard template upon creation.

4. **Controller Endpoint (`POST /dashboard-pages/copy-widgets`)**:
   - Parameters: `targetPageId`, `sourcePageId`, `mode` (`replace` or `append`).
   - Supports both JSON responses (`Accept: application/json`) and HTMX/browser navigation (`HX-Redirect` / `redirect:/dashboard-pages/{targetPageId}`).
   - Comprehensive validation rejecting self-copy, missing sources, or empty widget sets with clean error messaging.

5. **User Interface Integration**:
   - Header Toolbar: Adds `"Copy Widgets"` button with icon `content_copy` to custom dashboards.
   - Empty State: Surfaces `"Copy Widgets"` prominently on empty dashboard heroes alongside `"Add Widget"`.
   - Modal Dialog (`copyWidgetsModal`): Dropdown listing other dashboards with widget counts, mode selection (Replace vs Append), and error alerts.
   - Create Page Modal (`createPageModal`): Dropdown permitting one-click creation from an existing template dashboard.

## 3. Expected Impact & Trade-offs
- **Benefits:** Dramatically improves dashboard setup velocity; eliminates redundant widget configuration; enables templating workflows across teams.
- **Risks & Complexity:** Potential ID collision if widgets were copied shallowly without generating new IDs. Mitigated by recursive deep-cloning with newly generated UUIDs for both container widgets and child widgets.

## 4. Open Questions & Alternatives Considered
- **Alternatives Considered:** Client-side clipboard/JSON export-import. Rejected in favor of direct server-side dashboard-to-dashboard copying for a frictionless single-click user experience within Aura Manager.
