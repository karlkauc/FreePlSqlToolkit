# FreePlSqlToolkit — UI/UX Polish Design

**Date:** 2026-05-14
**Scope:** `plsql-toolkit-app` (JavaFX 22 desktop workbench)
**Goal:** Lift the v0.2 workbench from "functional AtlantaFX default" to a distinctive, professional-grade PL/SQL business application with its own visual identity, refined workflows, and a complete brand mark + icon set.

---

## 1. Identity & Brand

### 1.1 Brand direction

**Oracle Crimson + Anthracite** — the app lives in the Oracle/PL/SQL universe and signals that clearly through accent color, while the chrome stays on a neutral slate so the work (code, lint findings, schema trees) reads first.

### 1.2 Color tokens

Tokens are introduced as **CSS custom properties** in `app.css`, layered on top of AtlantaFX `PrimerLight` / `PrimerDark`. AtlantaFX already provides `-color-*` semantic tokens; we add an app-namespaced layer on top.

| Token                       | Light                | Dark                 | Purpose                                  |
| --------------------------- | -------------------- | -------------------- | ---------------------------------------- |
| `-fxt-primary`              | `#C74634`            | `#E25C4A`            | Primary brand (logo, primary buttons, focus ring) |
| `-fxt-primary-hover`        | `#A53A2A`            | `#C74634`            | Hover/pressed                            |
| `-fxt-primary-soft`         | `#FBE9E6`            | `#3A1A15`            | Selected row, active tab underline bg    |
| `-fxt-surface-0`            | `#FFFFFF`            | `#0F172A`            | Editor background                        |
| `-fxt-surface-1`            | `#F8FAFC`            | `#111827`            | Window chrome / sidebar bg               |
| `-fxt-surface-2`            | `#EEF2F6`            | `#1B2433`            | Toolbar / titled-pane header             |
| `-fxt-border`               | `#E2E8F0`            | `#1F2937`            | Default 1px divider                      |
| `-fxt-fg-default`           | `#0F172A`            | `#E5E7EB`            | Primary text                             |
| `-fxt-fg-muted`             | `#64748B`            | `#94A3B8`            | Captions, section titles, gutter         |
| `-fxt-success`              | `#16A34A`            | `#22C55E`            | Lint clean, connection ok                |
| `-fxt-warn`                 | `#D97706`            | `#F59E0B`            | Lint warnings                            |
| `-fxt-danger`               | `#DC2626`            | `#EF4444`            | Lint errors, invalid objects             |
| `-fxt-info`                 | `#2563EB`            | `#60A5FA`            | Links, neutral badges                    |

**Lint severity colors are derived from these** — no separate palette. A WARN chip is `-fxt-warn` text on a 12% tinted background; an ERROR chip is `-fxt-danger`. This keeps the system small.

### 1.3 Typography

| Surface             | Font stack                                            | Size  | Weight |
| ------------------- | ----------------------------------------------------- | ----- | ------ |
| UI default          | `"Inter", "Segoe UI", "Helvetica Neue", system`       | 13px  | 400    |
| UI emphasis         | same                                                  | 13px  | 600    |
| Section titles      | same, uppercase, 0.6px tracking                       | 11px  | 600    |
| Status bar          | same                                                  | 11px  | 400    |
| Code                | `"JetBrains Mono", "Fira Code", "Cascadia Code", "Consolas"` | 13px  | 400    |

Inter is bundled as a TTF resource under `resources/fonts/Inter-*.ttf` and loaded via `Font.loadFont()` at startup. JetBrains Mono follows the same pattern. **Fallback chain matters** — never fail to render if a custom font is missing.

### 1.4 Spacing & corners

- Base spacing unit: **4px** (use multiples: 4, 8, 12, 16, 24, 32).
- Border radius: **6px** for buttons/chips, **8px** for panels/cards, **0** for the editor area.
- Focus ring: 2px solid `-fxt-primary` with a 1px transparent gap, no glow.

---

## 2. Logo & Icon Set

### 2.1 Mark concept

**"Bracket-cut cylinder + check"** — a database cylinder silhouette whose front face is cut open like a code bracket `[ ]`, with a subtle checkmark inside. Three meanings in one mark: **database (Oracle), code (bracket), linter (check)**.

```
   ╭─────────╮          The cylinder ellipse stays as-is.
  ╱           ╲         A vertical "bracket" gap is cut
 │  [    ✓  ] │   ←     out of the front, revealing the
 │            │         check mark inside.
 │            │
  ╲           ╱
   ╰─────────╯
```

### 2.2 Logo deliverables

| File                                    | Format | Size            | Use                          |
| --------------------------------------- | ------ | --------------- | ---------------------------- |
| `resources/branding/logo-mark.svg`      | SVG    | 64×64 viewBox   | App-wide mark (vector)       |
| `resources/branding/logo-wordmark.svg`  | SVG    | 240×64 viewBox  | About dialog, splash, README |
| `resources/branding/logo-monochrome.svg`| SVG    | 64×64 viewBox   | Single-color usage (b/w)     |

The mark uses **two tones only**: `-fxt-primary` for the cylinder and bracket, off-white/light surface for the check. Monochrome version flattens both to a single ink color for dark backgrounds.

### 2.3 App icons

| File                                       | Format | Size       | Use                                   |
| ------------------------------------------ | ------ | ---------- | ------------------------------------- |
| `resources/branding/icon-16.png`           | PNG    | 16×16      | Window taskbar (small)                |
| `resources/branding/icon-32.png`           | PNG    | 32×32      | Window taskbar (HiDPI)                |
| `resources/branding/icon-64.png`           | PNG    | 64×64      | Notification, alt-tab                 |
| `resources/branding/icon-128.png`          | PNG    | 128×128    | App switcher                          |
| `resources/branding/icon-256.png`          | PNG    | 256×256    | Installer, macOS Finder               |
| `resources/branding/icon-512.png`          | PNG    | 512×512    | Linux .desktop, web README hero       |
| `resources/branding/icon.icns` *(later)*   | ICNS   | bundle     | macOS .app bundle (jpackage step)     |
| `resources/branding/icon.ico` *(later)*    | ICO    | multi-res  | Windows .exe (jpackage step)          |

**Loading:** `MainApp.start()` adds `icon-16`, `icon-32`, `icon-64`, `icon-128`, `icon-256` to `stage.getIcons()`. JavaFX picks the best size per OS. `.icns`/`.ico` come later when jpackage installers ship (v0.3 backlog item already exists).

At 16×16, the cylinder simplifies to a chunky pill with a slim crimson bar — readable, recognizable.

### 2.4 Functional iconography

Existing dependency: `ikonli-feather-pack` (already in `build.gradle.kts`). Use **Feather** icons throughout — they fit the modern-business tone and have full coverage:

| Use                          | Feather glyph              |
| ---------------------------- | -------------------------- |
| Connection (online)          | `database` + status dot    |
| Connection (offline)         | `database` muted           |
| Schema browse                | `folder`                   |
| Local files                  | `hard-drive`               |
| Tools (search)               | `search`                   |
| Metrics                      | `bar-chart-2`              |
| Invalid objects              | `alert-octagon`            |
| Schema diff                  | `git-compare`              |
| Git snapshot                 | `git-commit`               |
| Theme toggle (light/dark)    | `sun` / `moon`             |
| Settings                     | `settings`                 |
| Lint OK                      | `check-circle`             |
| Lint warn                    | `alert-triangle`           |
| Lint error                   | `x-circle`                 |

Icon size in toolbars: 16px. Activity-bar: 20px. Status-bar: 14px. Always inherit `-fxt-fg-muted` unless active (then `-fxt-primary`).

---

## 3. Layout & Workflow Refinements

### 3.1 Shell anatomy (modernized)

```
┌──────────────────────────────────────────────────────────────────────────┐
│  ▮ FreePlSqlToolkit                                            ─ □ ×     │  ← OS title bar (native)
├──────────────────────────────────────────────────────────────────────────┤
│  File  Edit  View  Connections  Tools  Help                              │  ← MenuBar
├────┬─────────────────────────┬───────────────────────────────────────────┤
│ ▢  │ CONNECTIONS         ▼   │ [tab] EMP.pkb   [tab] Lint Report   + ··· │
│ 🗄 │ ● prod_ora19           │  ┌──────────────────────────────────────┐ │
│ 📂 │ ○ dev_ora21            │  │  1  CREATE OR REPLACE PACKAGE BODY  │ │
│ 🔍 │─────────────────────────│  │  2    emp AS                        │ │
│ 📊 │ SCHEMA NAVIGATOR    ▼   │  │  3      ...                         │ │
│ ⚠  │  ▸ HR                   │  │                                     │ │
│ 🔀 │  ▸ SCOTT                │  │                                     │ │
│    │─────────────────────────│  └──────────────────────────────────────┘ │
│    │ LOCAL FILES         ▶   │                                           │
├────┴─────────────────────────┴───────────────────────────────────────────┤
│  ● prod_ora19  •  3 ⚠  2 ✕  •  Ln 42, Col 18  •  UTF-8  LF  •  ☀         │  ← StatusBar
└──────────────────────────────────────────────────────────────────────────┘
```

**Three structural additions** over the current `MainView.fxml`:

1. **Activity-Bar (left, 44px wide)** — vertical strip of icon-only buttons that *focus* an existing accordion section or *open* a tool tab. Acts as a fast workflow switcher without changing the underlying Accordion model.
   - Buttons (top): Connections, Schema, Files, Search, Metrics, Invalid, Diff.
   - Buttons (bottom, pushed down with a `Region`): Settings, Theme toggle.
2. **Section title bars inside Accordion panes** — replace the bare TitledPane look with a small uppercase header + a "more" `…` button per pane (refresh, collapse all, etc. — for now refresh-only where applicable).
3. **Real StatusBar** — current bar is just a label and a separator. It becomes a 4-segment bar:
   - **Connection** — colored dot + active profile name (click → opens Connections pane). Multiple active connections show count `●3`.
   - **Lint counts** — `⚠ N  ✕ M` chips, click jumps to current tab's lint report.
   - **Caret position** — `Ln N, Col M` for the active editor tab (only visible when an editor tab is focused).
   - **File metadata** — encoding + EOL + theme toggle, right-aligned.

### 3.2 Workflow-driven decisions

The 8 features map to recognizable user **jobs-to-be-done**. The polish addresses friction points in each:

| Workflow                                       | Friction today                                                   | Fix in this design                                                                                  |
| ---------------------------------------------- | ---------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| **Connect to a DB**                            | Accordion section must be expanded; status is hidden             | Activity-bar button + status-bar dot make connection state visible at all times                     |
| **Browse a schema**                            | Tree gives no visual cue for object type                         | Feather icons per node type (package, view, trigger, table, …) — typed colors for invalid/debug     |
| **Open a local file**                          | Discovery via menu only                                          | Activity-bar `Files` button + sidebar pane title; drag-drop hint on the empty state                 |
| **Edit & live-lint PL/SQL**                    | Lint pane styling inherits AtlantaFX defaults; no severity color | Severity chips with token colors; gutter marks in editor; clear count in status bar                 |
| **Search across DB sources**                   | Plain TableView                                                  | Result rows show schema·type·name with type icon; selected row highlights with `-fxt-primary-soft`  |
| **Compute metrics**                            | Raw numbers                                                      | Headline KPIs as cards (LOC, files, avg CCN, max CCN) above the table; table inherits new chrome    |
| **Find invalid objects**                       | List with no urgency                                             | Danger-tinted row chip on `INVALID`; quick-fix "Open in editor" action                              |
| **Schema diff**                                | Plain text diff                                                  | Side-by-side with `-fxt-success`/`-fxt-danger`/`-fxt-warn` gutter strips for added/removed/changed  |
| **Git snapshot**                               | Modal dialog with no progress feedback                           | Same modal, restyled with primary button + result toast on success                                  |

### 3.3 Empty states & first-run

Every center tab and every sidebar pane gets an **empty state** when no data exists:
- Connections pane empty → icon + "No connections yet" + a primary `+ Add connection` button.
- Schema pane empty → "Connect to a database to browse its objects." + link that opens connections.
- Files pane empty → "Drop `.sql`/`.pks`/`.pkb` files here or use **File → Open**."
- Editor area with no tabs → centered logo mark at 30% opacity + welcome hints (3 common actions).

Empty states are crucial for a "professional" feel — they remove the broken/empty look that AtlantaFX defaults give us.

### 3.4 Dialog refinement

- `MasterPasswordDialog`, `ProfileEditorDialog`, `SnapshotDialog` all gain the logo mark in their header strip and the same button hierarchy (primary = crimson, secondary = ghost outline, danger = outline-red).
- `About` becomes a small custom Stage instead of a plain `Alert`: shows wordmark, version, license, links.

---

## 4. CSS & FXML Architecture

### 4.1 CSS files (in `resources/css/`)

| File              | Role                                                                 |
| ----------------- | -------------------------------------------------------------------- |
| `tokens.css`      | **New.** Defines `-fxt-*` token vars for `.root` and `.root:dark`.   |
| `app.css`         | Rewritten. Applies tokens to JavaFX selectors (`.menu-bar`, `.tab-pane`, `.button.primary`, `.status-bar`, `.activity-bar`, `.section-title`, lint chips, empty states). |
| `syntax.css`      | Existing. Updated to use `-fxt-*` instead of hardcoded hex.          |
| `components.css`  | **New.** Reusable component classes (`.kpi-card`, `.chip-warn`, `.chip-danger`, `.chip-success`, `.severity-bar`). |

All loaded in this order in `MainApp.start()`: `tokens.css` → `app.css` → `components.css` → `syntax.css`. AtlantaFX user-agent stylesheet remains the base — we override surgically, never reset wholesale.

Dark-mode tokens are scoped via a `.dark` class added to the scene root when `ThemeManager` switches. AtlantaFX already handles its own theme swap; we only need to flip our token block.

### 4.2 FXML changes

| FXML                       | Change                                                                                                  |
| -------------------------- | ------------------------------------------------------------------------------------------------------- |
| `MainView.fxml`            | Wrap `<left>` in an HBox: `[ActivityBar (44px)][Sidebar (236px, the existing Accordion)]`. Replace status-bar HBox with a multi-segment HBox with named children (`connDot`, `connLabel`, `lintWarnChip`, `lintErrChip`, `caretLabel`, `encodingLabel`, `themeButton`). |
| `ConnectionSidebar.fxml`   | Add section-title row with refresh button; restyle ListView cell.                                       |
| `SchemaNavigator.fxml`     | Add section-title row; cells get type-icon graphic.                                                     |
| `LocalFileNavigator.fxml`  | Add section-title row + empty-state placeholder.                                                        |
| `ProfileEditorDialog.fxml` | Header strip with logo mark; primary button styled `.button-primary`.                                   |

A new `ActivityBar.fxml` is introduced as a separate component for clarity, included via `<fx:include>` in `MainView.fxml`.

### 4.3 New Java classes

| Class                                                                  | Purpose                                                |
| ---------------------------------------------------------------------- | ------------------------------------------------------ |
| `org.fxt.freeplsql.app.ui.shell.ActivityBarController`                 | The vertical icon bar; bridges to the existing Accordion via the WorkspaceController. |
| `org.fxt.freeplsql.app.ui.shell.StatusBarController`                   | Computes connection dot + lint counts from `AppContext` + active editor tab. |
| `org.fxt.freeplsql.app.ui.shell.Branding`                              | Loads logo SVG/PNG resources, returns `Image` instances. |
| `org.fxt.freeplsql.app.ui.shell.FontLoader`                            | Bundles `Font.loadFont()` calls at startup with safe fallback. |
| `org.fxt.freeplsql.app.ui.shell.EmptyState`                            | Reusable factory: returns a styled centered VBox (icon + title + body + optional action button). |

### 4.4 Resources to add

```
plsql-toolkit-app/src/main/resources/
├── branding/
│   ├── logo-mark.svg
│   ├── logo-wordmark.svg
│   ├── logo-monochrome.svg
│   ├── icon-16.png ··· icon-512.png
├── fonts/
│   ├── Inter-Regular.ttf
│   ├── Inter-SemiBold.ttf
│   ├── JetBrainsMono-Regular.ttf
└── css/
    ├── tokens.css        (new)
    ├── components.css    (new)
    ├── app.css           (rewritten)
    └── syntax.css        (token-ized)
```

PNG icons are **generated from the SVG** during the implementation phase using a small script (ImageMagick or Inkscape — whichever is available on the build host). They're checked in so the build doesn't depend on either tool.

---

## 5. Out of Scope (Explicit)

- **No feature changes.** No new tabs, no new menu items, no new linter rules, no changes to `plsql-app-services`.
- **No native installers** (jpackage). `.icns`/`.ico` generation is queued for the existing v0.3 jpackage work item.
- **No splash screen.** Adds startup latency for marginal value; revisit later if launch time grows.
- **No internationalization.** Strings remain English (per project memory).
- **No accessibility audit beyond color-contrast.** WCAG-AA contrast must hold for all text/background pairs in the token table; broader a11y (screen-reader labels, keyboard-only flows) is a separate effort.
- **No re-styling of `plsql-guidelines` (MkDocs) site.** This spec is JavaFX-app-only.

---

## 6. Risks & Mitigations

| Risk                                                            | Mitigation                                                                                         |
| --------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| AtlantaFX token names change between minor versions             | All app-level styling uses `-fxt-*`; we only reference `-color-*` in `tokens.css` as a fallback. |
| Custom fonts fail to load on some Linux distros                 | `FontLoader` always returns `true`/`false` per font; CSS uses fallback chain so layout is unaffected. |
| SVG rendering for icons is uneven in JavaFX                     | Logo mark is also exported to PNG sizes; SVGs are only used in HTML/About contexts.                |
| Dark-mode contrast on crimson primary                           | Dark theme uses lifted `#E25C4A` (not the same crimson) — tested against `#0F172A` for ≥4.5:1.    |
| Existing screenshots/docs become stale                          | README hero image gets updated in the same PR; other docs scheduled in a follow-up.                |

---

## 7. Acceptance Criteria

The polish lands successfully when:

1. App launches with new logo in window title bar and dock/taskbar on Linux, macOS, Windows.
2. Light and dark themes both render the new token system; toggle is instant and persists across restarts.
3. The Activity Bar is present, all 7 buttons route to the right pane or tab.
4. Status bar shows live connection state, live lint counts, live caret position.
5. Every accordion pane and every center tab has a non-broken empty state.
6. No regressions in the existing 42 unit tests; new UI helpers carry their own tests where logic-bearing (StatusBarController binding, Branding resource loader).
7. README hero image and `About` dialog use the new wordmark.
